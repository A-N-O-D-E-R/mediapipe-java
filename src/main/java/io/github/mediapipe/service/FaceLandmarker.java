package io.github.mediapipe.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.mediapipe.exception.DetectionException;
import io.github.mediapipe.model.BlendShape;
import io.github.mediapipe.model.FaceLandmarks;
import io.github.mediapipe.model.Landmark;
import io.github.mediapipe.util.PythonBridge;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;

/**
 * Face landmarker service using MediaPipe.
 * Detects facial landmarks, blend shapes, and facial transformation matrices.
 */
@Slf4j
public class FaceLandmarker implements AutoCloseable {

    private final PythonBridge pythonBridge;
    private final ObjectMapper objectMapper;
    private final float minDetectionConfidence;
    private final float minTrackingConfidence;
    private boolean initialized = false;

    /**
     * Create a face landmarker with default settings.
     */
    public FaceLandmarker() {
        this(0.5f, 0.5f);
    }

    /**
     * Create a face landmarker with custom confidence thresholds.
     *
     * @param minDetectionConfidence Minimum detection confidence (0.0 - 1.0)
     * @param minTrackingConfidence Minimum tracking confidence (0.0 - 1.0)
     */
    public FaceLandmarker(float minDetectionConfidence, float minTrackingConfidence) {
        this(new PythonBridge(), minDetectionConfidence, minTrackingConfidence);
    }

    /**
     * Create a face landmarker with custom Python bridge and confidence thresholds.
     *
     * @param pythonBridge Python bridge instance
     * @param minDetectionConfidence Minimum detection confidence
     * @param minTrackingConfidence Minimum tracking confidence
     */
    public FaceLandmarker(PythonBridge pythonBridge, float minDetectionConfidence, float minTrackingConfidence) {
        this.pythonBridge = pythonBridge;
        this.minDetectionConfidence = minDetectionConfidence;
        this.minTrackingConfidence = minTrackingConfidence;
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Initialize the face landmarker.
     * This must be called before detecting landmarks.
     */
    public void initialize() {
        if (initialized) {
            log.warn("Face landmarker already initialized");
            return;
        }

        // Start Python bridge if not running
        if (!pythonBridge.isRunning()) {
            pythonBridge.start();
        }

        // Initialize face landmarker in Python
        Map<String, Object> command = new HashMap<>();
        command.put("action", "init_face_landmarker");
        command.put("minDetectionConfidence", minDetectionConfidence);
        command.put("minTrackingConfidence", minTrackingConfidence);

        JsonNode response = pythonBridge.sendCommand(command);
        if (!"success".equals(response.get("status").asText())) {
            throw new DetectionException("Failed to initialize face landmarker");
        }

        initialized = true;
        log.info("Face landmarker initialized with detection confidence: {}, tracking confidence: {}",
                minDetectionConfidence, minTrackingConfidence);
    }

    /**
     * Detect face landmarks in an image file.
     *
     * @param imageFile Image file to process
     * @return List of face landmarks for each detected face
     */
    public List<FaceLandmarks> detectLandmarks(File imageFile) {
        if (!imageFile.exists()) {
            throw new IllegalArgumentException("Image file does not exist: " + imageFile);
        }

        try {
            byte[] imageBytes = Files.readAllBytes(imageFile.toPath());
            return detectLandmarks(imageBytes);
        } catch (IOException e) {
            throw new DetectionException("Failed to read image file", e);
        }
    }

    /**
     * Detect face landmarks in an image byte array.
     *
     * @param imageBytes Image data as bytes
     * @return List of face landmarks for each detected face
     */
    public List<FaceLandmarks> detectLandmarks(byte[] imageBytes) {
        if (!initialized) {
            initialize();
        }

        // Encode image to base64
        String base64Image = Base64.getEncoder().encodeToString(imageBytes);

        // Send detection command
        Map<String, Object> command = new HashMap<>();
        command.put("action", "detect_landmarks");
        command.put("imageData", base64Image);

        JsonNode response = pythonBridge.sendCommand(command);

        // Parse results
        return parseFaceLandmarks(response);
    }

    /**
     * Parse face landmarks from JSON response.
     */
    private List<FaceLandmarks> parseFaceLandmarks(JsonNode response) {
        try {
            List<FaceLandmarks> landmarksList = new ArrayList<>();
            JsonNode facesNode = response.get("faces");

            if (facesNode != null && facesNode.isArray()) {
                for (JsonNode faceNode : facesNode) {
                    FaceLandmarks faceLandmarks = new FaceLandmarks();

                    // Parse landmarks
                    List<Landmark> landmarks = new ArrayList<>();
                    JsonNode landmarksNode = faceNode.get("landmarks");
                    if (landmarksNode != null && landmarksNode.isArray()) {
                        for (JsonNode lmNode : landmarksNode) {
                            Landmark landmark = new Landmark(
                                    (float) lmNode.get("x").asDouble(),
                                    (float) lmNode.get("y").asDouble(),
                                    (float) lmNode.get("z").asDouble()
                            );
                            landmarks.add(landmark);
                        }
                    }
                    faceLandmarks.setLandmarks(landmarks);

                    // Parse blend shapes
                    List<BlendShape> blendShapes = new ArrayList<>();
                    JsonNode blendShapesNode = faceNode.get("blendshapes");
                    if (blendShapesNode != null && blendShapesNode.isArray()) {
                        for (JsonNode bsNode : blendShapesNode) {
                            BlendShape blendShape = new BlendShape(
                                    bsNode.get("category").asText(),
                                    (float) bsNode.get("score").asDouble()
                            );
                            blendShapes.add(blendShape);
                        }
                    }
                    faceLandmarks.setBlendshapes(blendShapes);

                    // Parse transformation matrix
                    JsonNode matrixNode = faceNode.get("transformationMatrix");
                    if (matrixNode != null && matrixNode.isArray()) {
                        double[][] matrix = objectMapper.convertValue(matrixNode, double[][].class);
                        faceLandmarks.setTransformationMatrix(matrix);
                    }

                    landmarksList.add(faceLandmarks);
                }
            }

            log.debug("Detected landmarks for {} faces", landmarksList.size());
            return landmarksList;

        } catch (Exception e) {
            throw new DetectionException("Failed to parse face landmark results", e);
        }
    }

    @Override
    public void close() {
        if (pythonBridge != null) {
            pythonBridge.close();
        }
        initialized = false;
    }
}
