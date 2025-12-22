package com.mediapipe.videoupload.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.mediapipe.videoupload.model.VideoProcessingResult;
import com.mediapipe.videoupload.model.VideoStats;
import io.github.mediapipe.exception.DetectionException;
import io.github.mediapipe.model.BoundingBox;
import io.github.mediapipe.model.FaceDetection;
import io.github.mediapipe.model.FaceLandmarks;
import io.github.mediapipe.model.Keypoint;
import io.github.mediapipe.service.FaceDetector;
import io.github.mediapipe.service.FaceLandmarker;
import io.github.mediapipe.util.PythonBridge;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class VideoProcessingService {

    private final FaceDetector faceDetector;
    private final FaceLandmarker faceLandmarker;
    private final PythonBridge pythonBridge;


    public VideoProcessingResult processVideo(MultipartFile file, int frameSkip) throws IOException {
        long startTime = System.currentTimeMillis();

        // Ensure Python bridge is running
        if (!pythonBridge.isRunning()) {
            pythonBridge.start();
        }

        // Ensure face detector is initialized
        faceDetector.initialize();
        faceLandmarker.initialize();

        Path savedVideo = saveMultipartFile(file, "/tmp/video");
        try {
            // Open video using Python bridge
            Map<Integer, List<FaceDetection>> faceDetectionsByFrame = faceDetector.detectFaces(savedVideo, frameSkip);
            Map<Integer, List<FaceLandmarks>> landmarksByFrame = faceLandmarker.detectLandmarks(savedVideo, frameSkip);
            VideoStats stats = calculateStats(faceDetectionsByFrame);
            long processingTime = System.currentTimeMillis() - startTime;

            return VideoProcessingResult.builder()
                    .fileName(file.getOriginalFilename())
                    .fileSize(file.getSize())
                    .faceDetectionsByFrame(faceDetectionsByFrame)
                    .landmarksByFrame(landmarksByFrame)
                    .stats(stats)
                    .status("COMPLETED")
                    .processingTimeMs(processingTime)
                    .build();

        } finally {
            Files.deleteIfExists(savedVideo);
        }
    }

    private VideoStats calculateStats(Map<Integer, List<FaceDetection>> detectionsByFrame) {
        int totalFaces = 0;
        int framesWithFaces = 0;
        int framesWithoutFaces = 0;
        int maxFaces = 0;

        for (List<FaceDetection> faces : detectionsByFrame.values()) {
            int faceCount = faces.size();
            totalFaces += faceCount;
            maxFaces = Math.max(maxFaces, faceCount);

            if (faceCount > 0) {
                framesWithFaces++;
            } else {
                framesWithoutFaces++;
            }
        }

        double avgFaces = detectionsByFrame.isEmpty() ? 0 :
                (double) totalFaces / detectionsByFrame.size();

        return VideoStats.builder()
                .totalFacesDetected(totalFaces)
                .framesWithFaces(framesWithFaces)
                .framesWithoutFaces(framesWithoutFaces)
                .averageFacesPerFrame(avgFaces)
                .maxFacesInSingleFrame(maxFaces)
                .build();
    }


    public Path saveMultipartFile(MultipartFile multipartFile, String targetDir) throws IOException {
        // Ensure the target directory exists
        Path dirPath = Paths.get(targetDir);
        if (!Files.exists(dirPath)) {
            Files.createDirectories(dirPath);
        }

        // Build the target file path
        String originalFilename = multipartFile.getOriginalFilename();
        if (originalFilename == null) {
            originalFilename = "uploaded_video";
        }
        Path targetPath = dirPath.resolve(originalFilename);
        multipartFile.transferTo(targetPath.toFile());
        // Return the path so you can use it with OpenCV
        return targetPath.toAbsolutePath();
    }
}
