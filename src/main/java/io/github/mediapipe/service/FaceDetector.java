package io.github.mediapipe.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.mediapipe.exception.DetectionException;
import io.github.mediapipe.model.BoundingBox;
import io.github.mediapipe.model.FaceDetection;
import io.github.mediapipe.model.Keypoint;
import io.github.mediapipe.util.PythonBridge;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * Face detector service using MediaPipe.
 * Detects faces in images and provides bounding boxes and keypoints.
 */
@Slf4j
public class FaceDetector implements AutoCloseable {

    private final PythonBridge pythonBridge;
    private final ObjectMapper objectMapper;
    private final String modelName;
    private final float minDetectionConfidence;
    private boolean initialized = false;

    /**
     * Create a face detector with default settings.
     */
    public FaceDetector() {
        this(0.5f);
    }

    /**
     * Create a face detector with custom confidence threshold.
     *
     * @param minDetectionConfidence Minimum confidence threshold (0.0 - 1.0)
     */
    public FaceDetector(float minDetectionConfidence) {
        this(new PythonBridge(), minDetectionConfidence);
    }

    /**
     * Create a face detector with custom Python bridge and confidence threshold.
     *
     * @param pythonBridge Python bridge instance
     * @param minDetectionConfidence Minimum confidence threshold
     */
    public FaceDetector(PythonBridge pythonBridge, float minDetectionConfidence) {
        this.pythonBridge = pythonBridge;
        this.modelName="blaze_face_short_range.tflite";
        this.minDetectionConfidence = minDetectionConfidence;
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Initialize the face detector.
     * This must be called before detecting faces.
     */
    public void initialize() {
        if (initialized) {
            log.warn("Face detector already initialized");
            return;
        }

        // Start Python bridge if not running
        if (!pythonBridge.isRunning()) {
            pythonBridge.start();
        }

        // Initialize face detector in Python
        Map<String, Object> command = new HashMap<>();
        command.put("action", "init_face_detector");
        command.put("minDetectionConfidence", minDetectionConfidence);
        command.put("modelPath", Paths.get(System.getProperty("user.home"), ".mediapipe", "models","face_detection",modelName).toAbsolutePath().toString());

        JsonNode response = pythonBridge.sendCommand(command);
        if (!"success".equals(response.get("status").asText())) {
            throw new DetectionException("Failed to initialize face detector");
        }

        initialized = true;
        log.info("Face detector initialized with confidence threshold: {}", minDetectionConfidence);
    }

    /**
     * Detect faces in an image file.
     *
     * @param imageFile Image file to process
     * @return List of detected faces
     */
    public List<FaceDetection> detectFaces(File imageFile) {
        if (!imageFile.exists()) {
            throw new IllegalArgumentException("Image file does not exist: " + imageFile);
        }

        try {
            byte[] imageBytes = Files.readAllBytes(imageFile.toPath());
            return detectFaces(imageBytes);
        } catch (IOException e) {
            throw new DetectionException("Failed to read image file", e);
        }
    }

    /**
     * Detect faces in an image byte array.
     *
     * @param imageBytes Image data as bytes
     * @return List of detected faces
     */
    public List<FaceDetection> detectFaces(byte[] imageBytes) {
        if (!initialized) {
            initialize();
        }

        // Encode image to base64
        String base64Image = Base64.getEncoder().encodeToString(imageBytes);

        // Send detection command
        Map<String, Object> command = new HashMap<>();
        command.put("action", "detect_faces");
        command.put("imageData", base64Image);

        JsonNode response = pythonBridge.sendCommand(command);

        // Parse results
        return parseFaceDetections(response);
    }

    /**
     * Parse face detections from JSON response.
     */
    private List<FaceDetection> parseFaceDetections(JsonNode response) {
        try {
            List<FaceDetection> detections = new ArrayList<>();
            JsonNode facesNode = response.get("faces");

            if (facesNode != null && facesNode.isArray()) {
                for (JsonNode faceNode : facesNode) {
                    FaceDetection detection = new FaceDetection();

                    // Parse bounding box
                    JsonNode bboxNode = faceNode.get("boundingBox");
                    if (bboxNode != null) {
                        BoundingBox bbox = new BoundingBox(
                                bboxNode.get("x").asInt(),
                                bboxNode.get("y").asInt(),
                                bboxNode.get("width").asInt(),
                                bboxNode.get("height").asInt()
                        );
                        detection.setBoundingBox(bbox);
                    }

                    // Parse keypoints
                    List<Keypoint> keypoints = new ArrayList<>();
                    JsonNode keypointsNode = faceNode.get("keypoints");
                    if (keypointsNode != null && keypointsNode.isArray()) {
                        for (JsonNode kpNode : keypointsNode) {
                            Keypoint kp = new Keypoint(
                                    kpNode.get("type").asText(),
                                    (float) kpNode.get("x").asDouble(),
                                    (float) kpNode.get("y").asDouble()
                            );
                            keypoints.add(kp);
                        }
                    }
                    detection.setKeypoints(keypoints);

                    // Parse confidence
                    detection.setConfidence((float) faceNode.get("confidence").asDouble());

                    detections.add(detection);
                }
            }

            log.debug("Detected {} faces", detections.size());
            return detections;

        } catch (Exception e) {
            throw new DetectionException("Failed to parse face detection results", e);
        }
    }


    // TODO: study the feasability of the concurent processing with python bridge
    public Map<Integer, List<FaceDetection>> detectFaces(Path videoFile, int frameSkip){
        try(VideoProcessorWrapper wrapper = new VideoProcessorWrapper(pythonBridge, videoFile)){
            Map<Integer, List<FaceDetection>> faceDetectionsByFrame = new LinkedHashMap<>();
            int processedFrames = 0;

            for (int frameNumber = 0; frameNumber < wrapper.getTotalFrames(); frameNumber++) {
                if (frameNumber % (frameSkip + 1) == 0) {
                    try {
                        List<FaceDetection> faces =  detectFacesInFrame(wrapper.getVideoId(), frameNumber);
                        faceDetectionsByFrame.put(frameNumber, faces);
                        processedFrames++;

                        if (processedFrames % 10 == 0) {
                            log.info("Processed {} frames out of {}", processedFrames, wrapper.getTotalFrames() / (frameSkip + 1));
                        }
                    } catch (Exception e) {
                        log.error("Error processing frame {}: {}", frameNumber, e.getMessage());
                        faceDetectionsByFrame.put(frameNumber, Collections.emptyList());
                    }
                }
            }
            return faceDetectionsByFrame;
        }
    }

    private List<FaceDetection> detectFacesInFrame(UUID videoId, int frameNumber) {
        Map<String, Object> command = new HashMap<>();
        command.put("action", "detect_faces_in_frame");
        command.put("videoId", videoId);
        command.put("frameNumber", frameNumber);

        JsonNode response = pythonBridge.sendCommand(command);

        if (!"success".equals(response.get("status").asText())) {
            throw new DetectionException("Failed to detect faces in frame: " + response.get("message").asText());
        }

        return parseFaceDetections(response);
    }



    @Override
    public void close() {
        if (pythonBridge != null) {
            pythonBridge.close();
        }
        initialized = false;
    }
}
