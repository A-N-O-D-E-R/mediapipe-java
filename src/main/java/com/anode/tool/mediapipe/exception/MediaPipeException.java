package com.anode.tool.mediapipe.exception;

/**
 * Base exception for all MediaPipe-related errors.
 */
public class MediaPipeException extends RuntimeException {

    public MediaPipeException(String message) {
        super(message);
    }

    public MediaPipeException(String message, Throwable cause) {
        super(message, cause);
    }
}
