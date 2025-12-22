package com.mediapipe.videoupload.model;

import io.github.mediapipe.model.FaceDetection;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VideoProcessingResult {
    private String videoId;
    private String fileName;
    private long fileSize;
    private int totalFrames;
    private int processedFrames;
    private double fps;
    private double durationSeconds;
    private Map<Integer, List<FaceDetection>> faceDetectionsByFrame;
    private VideoStats stats;
    private String status;
    private Long processingTimeMs;
}
