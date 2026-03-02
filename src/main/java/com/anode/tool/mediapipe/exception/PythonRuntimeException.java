package com.anode.tool.mediapipe.exception;

/**
 * Exception thrown when there are issues with the Python runtime.
 */
public class PythonRuntimeException extends MediaPipeException {

    public PythonRuntimeException(String message) {
        super(message);
    }

    public PythonRuntimeException(String message, Throwable cause) {
        super(message, cause);
    }
}
