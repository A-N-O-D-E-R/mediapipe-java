package com.anode.tool.mediapipe.exception;

/**
 * Exception thrown when face detection or landmark detection fails.
 */
public class DetectionException extends MediaPipeException {

    public DetectionException(String message) {
        super(message);
    }

    public DetectionException(String message, Throwable cause) {
        super(message, cause);
    }
}
