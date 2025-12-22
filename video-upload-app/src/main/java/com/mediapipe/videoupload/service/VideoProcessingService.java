package com.mediapipe.videoupload.service;

import com.mediapipe.videoupload.model.VideoProcessingResult;
import com.mediapipe.videoupload.model.VideoStats;
import io.github.mediapipe.model.FaceDetection;
import io.github.mediapipe.service.FaceDetector;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.videoio.VideoCapture;
import org.opencv.videoio.Videoio;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class VideoProcessingService {

    private final FaceDetector faceDetector;

    static {
        nu.pattern.OpenCV.loadLocally();
    }

    public VideoProcessingResult processVideo(MultipartFile file, int frameSkip) throws IOException {
        long startTime = System.currentTimeMillis();
        String videoId = UUID.randomUUID().toString();

        Path tempFile = Files.createTempFile("video-", "-" + file.getOriginalFilename());
        try {
            Files.copy(file.getInputStream(), tempFile, StandardCopyOption.REPLACE_EXISTING);

            VideoCapture capture = new VideoCapture(tempFile.toString());
            if (!capture.isOpened()) {
                throw new IllegalArgumentException("Failed to open video file");
            }

            try {
                return processVideoFrames(capture, videoId, file, frameSkip, startTime);
            } finally {
                capture.release();
            }
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }

    private VideoProcessingResult processVideoFrames(VideoCapture capture, String videoId,
                                                     MultipartFile file, int frameSkip,
                                                     long startTime) throws IOException {
        int totalFrames = (int) capture.get(Videoio.CAP_PROP_FRAME_COUNT);
        double fps = capture.get(Videoio.CAP_PROP_FPS);
        double duration = totalFrames / fps;

        Map<Integer, List<FaceDetection>> faceDetectionsByFrame = new LinkedHashMap<>();
        Mat frame = new Mat();
        int frameNumber = 0;
        int processedFrames = 0;

        log.info("Processing video: {} frames, {} fps, {} seconds", totalFrames, fps, duration);

        while (capture.read(frame)) {
            if (frameNumber % (frameSkip + 1) == 0) {
                try {
                    List<FaceDetection> faces = detectFacesInFrame(frame);
                    faceDetectionsByFrame.put(frameNumber, faces);
                    processedFrames++;

                    if (processedFrames % 10 == 0) {
                        log.info("Processed {} frames out of {}", processedFrames, totalFrames / (frameSkip + 1));
                    }
                } catch (Exception e) {
                    log.error("Error processing frame {}: {}", frameNumber, e.getMessage());
                    faceDetectionsByFrame.put(frameNumber, Collections.emptyList());
                }
            }
            frameNumber++;
        }

        VideoStats stats = calculateStats(faceDetectionsByFrame);
        long processingTime = System.currentTimeMillis() - startTime;

        return VideoProcessingResult.builder()
                .videoId(videoId)
                .fileName(file.getOriginalFilename())
                .fileSize(file.getSize())
                .totalFrames(totalFrames)
                .processedFrames(processedFrames)
                .fps(fps)
                .durationSeconds(duration)
                .faceDetectionsByFrame(faceDetectionsByFrame)
                .stats(stats)
                .status("COMPLETED")
                .processingTimeMs(processingTime)
                .build();
    }

    private List<FaceDetection> detectFacesInFrame(Mat frame) throws IOException {
        MatOfByte matOfByte = new MatOfByte();
        Imgcodecs.imencode(".jpg", frame, matOfByte);
        byte[] imageBytes = matOfByte.toArray();
        return faceDetector.detectFaces(imageBytes);
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
}
