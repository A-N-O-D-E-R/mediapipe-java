#!/usr/bin/env python3
"""
MediaPipe Bridge Script
Provides a JSON-based interface to MediaPipe vision tasks for Java integration.
Communicates via stdin/stdout using JSON messages.
"""

import sys
import json
import base64
import traceback
from io import BytesIO
from typing import Dict, Any, List, Optional

import cv2
import numpy as np
import mediapipe as mp
from mediapipe.tasks import python
from mediapipe.tasks.python import vision


class MediaPipeBridge:
    """Bridge between Java and MediaPipe Python API."""

    def __init__(self):
        self.face_detector: Optional[vision.FaceDetector] = None
        self.face_landmarker: Optional[vision.FaceLandmarker] = None

    def initialize_face_detector(self, model_path: Optional[str] = None,
                                 min_detection_confidence: float = 0.5) -> Dict[str, Any]:
        """Initialize the Face Detector."""
        try:
            base_options = python.BaseOptions(
                model_asset_path=model_path if model_path else None
            )
            options = vision.FaceDetectorOptions(
                base_options=base_options,
                min_detection_confidence=min_detection_confidence
            )
            self.face_detector = vision.FaceDetector.create_from_options(options)
            return {"status": "success", "message": "Face detector initialized"}
        except Exception as e:
            return {"status": "error", "message": str(e), "traceback": traceback.format_exc()}

    def initialize_face_landmarker(self, model_path: Optional[str] = None,
                                   min_detection_confidence: float = 0.5,
                                   min_tracking_confidence: float = 0.5) -> Dict[str, Any]:
        """Initialize the Face Landmarker."""
        try:
            base_options = python.BaseOptions(
                model_asset_path=model_path if model_path else None
            )
            options = vision.FaceLandmarkerOptions(
                base_options=base_options,
                min_face_detection_confidence=min_detection_confidence,
                min_tracking_confidence=min_tracking_confidence,
                output_face_blendshapes=True,
                output_facial_transformation_matrixes=True
            )
            self.face_landmarker = vision.FaceLandmarker.create_from_options(options)
            return {"status": "success", "message": "Face landmarker initialized"}
        except Exception as e:
            return {"status": "error", "message": str(e), "traceback": traceback.format_exc()}

    def detect_faces(self, image_data: str) -> Dict[str, Any]:
        """Detect faces in the provided image."""
        try:
            if self.face_detector is None:
                # Auto-initialize with defaults if not already done
                init_result = self.initialize_face_detector()
                if init_result["status"] == "error":
                    return init_result

            # Decode base64 image
            image_bytes = base64.b64decode(image_data)
            nparr = np.frombuffer(image_bytes, np.uint8)
            image = cv2.imdecode(nparr, cv2.IMREAD_COLOR)

            # Convert to RGB (MediaPipe expects RGB)
            rgb_image = cv2.cvtColor(image, cv2.COLOR_BGR2RGB)
            mp_image = mp.Image(image_format=mp.ImageFormat.SRGB, data=rgb_image)

            # Detect faces
            detection_result = self.face_detector.detect(mp_image)

            # Convert results to JSON-serializable format
            faces = []
            for detection in detection_result.detections:
                bbox = detection.bounding_box
                face_data = {
                    "boundingBox": {
                        "x": bbox.origin_x,
                        "y": bbox.origin_y,
                        "width": bbox.width,
                        "height": bbox.height
                    },
                    "keypoints": [
                        {
                            "type": kp.category_name if hasattr(kp, 'category_name') else str(i),
                            "x": kp.x,
                            "y": kp.y
                        }
                        for i, kp in enumerate(detection.keypoints)
                    ] if detection.keypoints else [],
                    "confidence": detection.categories[0].score if detection.categories else 0.0
                }
                faces.append(face_data)

            return {
                "status": "success",
                "faces": faces,
                "count": len(faces)
            }
        except Exception as e:
            return {"status": "error", "message": str(e), "traceback": traceback.format_exc()}

    def detect_face_landmarks(self, image_data: str) -> Dict[str, Any]:
        """Detect face landmarks in the provided image."""
        try:
            if self.face_landmarker is None:
                # Auto-initialize with defaults if not already done
                init_result = self.initialize_face_landmarker()
                if init_result["status"] == "error":
                    return init_result

            # Decode base64 image
            image_bytes = base64.b64decode(image_data)
            nparr = np.frombuffer(image_bytes, np.uint8)
            image = cv2.imdecode(nparr, cv2.IMREAD_COLOR)

            # Convert to RGB
            rgb_image = cv2.cvtColor(image, cv2.COLOR_BGR2RGB)
            mp_image = mp.Image(image_format=mp.ImageFormat.SRGB, data=rgb_image)

            # Detect landmarks
            detection_result = self.face_landmarker.detect(mp_image)

            # Convert results to JSON-serializable format
            faces = []
            for i, face_landmarks in enumerate(detection_result.face_landmarks):
                face_data = {
                    "landmarks": [
                        {
                            "x": landmark.x,
                            "y": landmark.y,
                            "z": landmark.z
                        }
                        for landmark in face_landmarks
                    ]
                }

                # Add blend shapes if available
                if detection_result.face_blendshapes and i < len(detection_result.face_blendshapes):
                    face_data["blendshapes"] = [
                        {
                            "category": bs.category_name,
                            "score": bs.score
                        }
                        for bs in detection_result.face_blendshapes[i]
                    ]

                # Add transformation matrix if available
                if (detection_result.facial_transformation_matrixes and
                    i < len(detection_result.facial_transformation_matrixes)):
                    matrix = detection_result.facial_transformation_matrixes[i]
                    face_data["transformationMatrix"] = matrix.tolist()

                faces.append(face_data)

            return {
                "status": "success",
                "faces": faces,
                "count": len(faces)
            }
        except Exception as e:
            return {"status": "error", "message": str(e), "traceback": traceback.format_exc()}

    def process_command(self, command: Dict[str, Any]) -> Dict[str, Any]:
        """Process a command from Java."""
        action = command.get("action")

        if action == "init_face_detector":
            return self.initialize_face_detector(
                model_path=command.get("modelPath"),
                min_detection_confidence=command.get("minDetectionConfidence", 0.5)
            )
        elif action == "init_face_landmarker":
            return self.initialize_face_landmarker(
                model_path=command.get("modelPath"),
                min_detection_confidence=command.get("minDetectionConfidence", 0.5),
                min_tracking_confidence=command.get("minTrackingConfidence", 0.5)
            )
        elif action == "detect_faces":
            return self.detect_faces(command.get("imageData"))
        elif action == "detect_landmarks":
            return self.detect_face_landmarks(command.get("imageData"))
        elif action == "ping":
            return {"status": "success", "message": "pong"}
        elif action == "shutdown":
            return {"status": "success", "message": "shutting down"}
        else:
            return {"status": "error", "message": f"Unknown action: {action}"}

    def run(self):
        """Main loop - read commands from stdin and write results to stdout."""
        sys.stderr.write("MediaPipe Bridge started\n")
        sys.stderr.flush()

        for line in sys.stdin:
            try:
                line = line.strip()
                if not line:
                    continue

                command = json.loads(line)
                result = self.process_command(command)

                # Write result as single-line JSON
                sys.stdout.write(json.dumps(result) + "\n")
                sys.stdout.flush()

                # Check for shutdown
                if command.get("action") == "shutdown":
                    break

            except json.JSONDecodeError as e:
                error_response = {
                    "status": "error",
                    "message": f"Invalid JSON: {str(e)}"
                }
                sys.stdout.write(json.dumps(error_response) + "\n")
                sys.stdout.flush()
            except Exception as e:
                error_response = {
                    "status": "error",
                    "message": str(e),
                    "traceback": traceback.format_exc()
                }
                sys.stdout.write(json.dumps(error_response) + "\n")
                sys.stdout.flush()


if __name__ == "__main__":
    bridge = MediaPipeBridge()
    bridge.run()
