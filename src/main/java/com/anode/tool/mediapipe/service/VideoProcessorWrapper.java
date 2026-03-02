package com.anode.tool.mediapipe.service;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.anode.tool.mediapipe.util.PythonBridge;
import com.fasterxml.jackson.databind.JsonNode;

public class VideoProcessorWrapper implements AutoCloseable {
    
    private final PythonBridge pythonBridge;
    private final Path videoFile;
    private final UUID id; 
    private final JsonNode videoMetadata; 

    public VideoProcessorWrapper(Path videoFile){
        this(new PythonBridge(), videoFile);
    }


    public VideoProcessorWrapper(PythonBridge pythonBridge, Path videoFile){
        this.pythonBridge=pythonBridge;
        this.videoFile=videoFile;
        this.id=UUID.randomUUID();
        this.videoMetadata=openVideo();
    }

    private JsonNode openVideo(){
        Map<String, Object> openCommand = new HashMap<>();
        openCommand.put("action", "open_video");
        openCommand.put("videoPath", this.videoFile.toAbsolutePath().toString());
        openCommand.put("videoId", this.id);
        JsonNode openResponse = pythonBridge.sendCommand(openCommand);
        if (!"success".equals(openResponse.get("status").asText())) {
            throw new IllegalArgumentException("Failed to open video file: " + openResponse.get("message").asText());
        }
        return openResponse;
    }

    @Override
    public void close() {
        Map<String, Object> closeCommand = new HashMap<>();
        closeCommand.put("action", "close_video");
        closeCommand.put("videoId", this.id);
        pythonBridge.sendCommand(closeCommand);
    }

    public UUID getVideoId(){
        return id;
    }

    public double getVideoDuration() {
        return videoMetadata.get("durationSeconds").asDouble();
    }

    public int getTotalFrames() {
        return videoMetadata.get("totalFrames").asInt();
    }

    public double getFramePerSeconds(){
        return videoMetadata.get("fps").asDouble();
    }
}
