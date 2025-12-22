package io.github.mediapipe.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.mediapipe.exception.PythonRuntimeException;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Manages the Python bridge process for MediaPipe operations.
 * Handles process lifecycle, communication, and resource cleanup.
 */
@Slf4j
public class PythonBridge implements AutoCloseable {

    private static final String PYTHON_SCRIPT_NAME = "mediapipe_bridge.py";
    private static final long COMMAND_TIMEOUT_SECONDS = 30;

    private final ObjectMapper objectMapper;
    private final String pythonExecutable;
    private Process pythonProcess;
    private BufferedWriter processInput;
    private BufferedReader processOutput;
    private BufferedReader processError;
    private Path scriptPath;
    private volatile boolean isRunning = false;

    /**
     * Create a new Python bridge with the default Python executable.
     */
    public PythonBridge() {
        this("python3");
    }

    /**
     * Create a new Python bridge with a custom Python executable path.
     *
     * @param pythonExecutable Path to the Python executable
     */
    public PythonBridge(String pythonExecutable) {
        this.pythonExecutable = pythonExecutable;
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Initialize and start the Python bridge process.
     */
    public synchronized void start() {
        if (isRunning) {
            log.warn("Python bridge is already running");
            return;
        }

        try {
            // Extract Python script from resources to temporary file
            extractPythonScript();

            // Start Python process
            ProcessBuilder processBuilder = new ProcessBuilder(
                    pythonExecutable,
                    "-u",  // Unbuffered output
                    scriptPath.toString()
            );
            processBuilder.redirectErrorStream(false);

            log.info("Starting Python bridge with: {}", pythonExecutable);
            pythonProcess = processBuilder.start();

            processInput = new BufferedWriter(new OutputStreamWriter(pythonProcess.getOutputStream()));
            processOutput = new BufferedReader(new InputStreamReader(pythonProcess.getInputStream()));
            processError = new BufferedReader(new InputStreamReader(pythonProcess.getErrorStream()));

            // Start error stream reader thread
            startErrorStreamReader();

            isRunning = true;

            // Test the connection
            JsonNode response = sendCommand(Map.of("action", "ping"));
            if (!"success".equals(response.get("status").asText())) {
                throw new PythonRuntimeException("Failed to ping Python bridge");
            }

            log.info("Python bridge started successfully");

        } catch (IOException e) {
            cleanup();
            throw new PythonRuntimeException("Failed to start Python bridge", e);
        }
    }

    /**
     * Extract the Python script from resources to a temporary file.
     */
    private void extractPythonScript() throws IOException {
        try (InputStream scriptStream = getClass().getClassLoader()
                .getResourceAsStream("python/" + PYTHON_SCRIPT_NAME)) {

            if (scriptStream == null) {
                throw new FileNotFoundException("Python script not found in resources: python/" + PYTHON_SCRIPT_NAME);
            }

            // Create temporary file
            scriptPath = Files.createTempFile("mediapipe_bridge_", ".py");
            scriptPath.toFile().deleteOnExit();

            // Copy script to temporary file
            Files.copy(scriptStream, scriptPath, StandardCopyOption.REPLACE_EXISTING);

            log.debug("Extracted Python script to: {}", scriptPath);
        }
    }

    /**
     * Start a thread to read and log error stream output.
     */
    private void startErrorStreamReader() {
        Thread errorReader = new Thread(() -> {
            try {
                String line;
                while ((line = processError.readLine()) != null) {
                    log.debug("Python stderr: {}", line);
                }
            } catch (IOException e) {
                if (isRunning) {
                    log.warn("Error reading Python stderr", e);
                }
            }
        }, "python-bridge-error-reader");
        errorReader.setDaemon(true);
        errorReader.start();
    }

    /**
     * Send a command to the Python bridge and wait for a response.
     *
     * @param command Command data to send
     * @return Response from Python
     * @throws PythonRuntimeException if communication fails
     */
    public synchronized JsonNode sendCommand(Map<String, Object> command) {
        if (!isRunning) {
            throw new PythonRuntimeException("Python bridge is not running");
        }

        try {
            // Send command as JSON
            String commandJson = objectMapper.writeValueAsString(command);
            log.debug("Sending command: {}", commandJson);

            processInput.write(commandJson);
            processInput.newLine();
            processInput.flush();

            // Read response
            String responseLine = processOutput.readLine();
            if (responseLine == null) {
                throw new PythonRuntimeException("Python process closed unexpectedly");
            }

            log.debug("Received response: {}", responseLine);

            JsonNode response = objectMapper.readTree(responseLine);

            // Check for error status
            if ("error".equals(response.get("status").asText())) {
                String errorMessage = response.get("message").asText();
                String traceback = response.has("traceback") ?
                        response.get("traceback").asText() : "";

                log.error("Python error: {}\n{}", errorMessage, traceback);
                throw new PythonRuntimeException("Python error: " + errorMessage);
            }

            return response;

        } catch (IOException e) {
            throw new PythonRuntimeException("Failed to communicate with Python bridge", e);
        }
    }

    /**
     * Check if the Python bridge is running.
     */
    public boolean isRunning() {
        return isRunning && pythonProcess != null && pythonProcess.isAlive();
    }

    /**
     * Stop the Python bridge process.
     */
    public synchronized void stop() {
        if (!isRunning) {
            return;
        }

        try {
            // Send shutdown command
            if (pythonProcess != null && pythonProcess.isAlive()) {
                try {
                    sendCommand(Map.of("action", "shutdown"));
                } catch (Exception e) {
                    log.warn("Error sending shutdown command", e);
                }

                // Wait for graceful shutdown
                boolean exited = pythonProcess.waitFor(5, TimeUnit.SECONDS);
                if (!exited) {
                    log.warn("Python process did not exit gracefully, forcing termination");
                    pythonProcess.destroyForcibly();
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Interrupted while waiting for Python process to exit");
        } finally {
            cleanup();
        }

        log.info("Python bridge stopped");
    }

    /**
     * Clean up resources.
     */
    private void cleanup() {
        isRunning = false;

        try {
            if (processInput != null) {
                processInput.close();
            }
        } catch (IOException e) {
            log.warn("Error closing process input", e);
        }

        try {
            if (processOutput != null) {
                processOutput.close();
            }
        } catch (IOException e) {
            log.warn("Error closing process output", e);
        }

        try {
            if (processError != null) {
                processError.close();
            }
        } catch (IOException e) {
            log.warn("Error closing process error", e);
        }

        if (pythonProcess != null) {
            pythonProcess.destroy();
        }

        if (scriptPath != null) {
            try {
                Files.deleteIfExists(scriptPath);
            } catch (IOException e) {
                log.warn("Error deleting temporary script file", e);
            }
        }
    }

    @Override
    public void close() {
        stop();
    }
}
