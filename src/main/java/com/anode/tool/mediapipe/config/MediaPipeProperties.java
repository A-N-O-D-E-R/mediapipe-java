package com.anode.tool.mediapipe.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for MediaPipe.
 */
@Data
@ConfigurationProperties(prefix = "mediapipe")
public class MediaPipeProperties {

    /**
     * Path to Python executable. Defaults to "python3".
     */
    private String pythonExecutable = "python3";

    /**
     * Face detection configuration.
     */
    private FaceDetection faceDetection = new FaceDetection();

    /**
     * Face landmarker configuration.
     */
    private FaceLandmarker faceLandmarker = new FaceLandmarker();

    @Data
    public static class FaceDetection {
        /**
         * Whether to enable face detection.
         */
        private boolean enabled = true;

        /**
         * Minimum detection confidence (0.0 - 1.0).
         */
        private float minDetectionConfidence = 0.5f;

        /**
         * Path to custom model file (optional).
         */
        private String modelPath;
    }

    @Data
    public static class FaceLandmarker {
        /**
         * Whether to enable face landmarker.
         */
        private boolean enabled = true;

        /**
         * Minimum detection confidence (0.0 - 1.0).
         */
        private float minDetectionConfidence = 0.5f;

        /**
         * Minimum tracking confidence (0.0 - 1.0).
         */
        private float minTrackingConfidence = 0.5f;

        /**
         * Path to custom model file (optional).
         */
        private String modelPath;
    }
}
