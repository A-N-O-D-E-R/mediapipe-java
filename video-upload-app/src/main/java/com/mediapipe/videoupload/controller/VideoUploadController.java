package com.mediapipe.videoupload.controller;

import com.mediapipe.videoupload.model.VideoProcessingResult;
import com.mediapipe.videoupload.service.VideoProcessingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/video")
@RequiredArgsConstructor
public class VideoUploadController {

    private final VideoProcessingService videoProcessingService;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadVideo(
            @RequestParam("video") MultipartFile video,
            @RequestParam(value = "frameSkip", defaultValue = "0") int frameSkip) {

        try {
            if (video.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Video file is empty"));
            }

            String contentType = video.getContentType();
            if (contentType == null || !contentType.startsWith("video/")) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "File must be a video"));
            }

            log.info("Received video upload: {} ({} bytes), frameSkip: {}",
                    video.getOriginalFilename(), video.getSize(), frameSkip);

            VideoProcessingResult result = videoProcessingService.processVideo(video, frameSkip);

            log.info("Video processing completed: {} frames processed in {} ms",
                    result.getProcessedFrames(), result.getProcessingTimeMs());

            return ResponseEntity.ok(result);

        } catch (IllegalArgumentException e) {
            log.error("Invalid video file: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Error processing video", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to process video: " + e.getMessage()));
        }
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "UP");
        response.put("service", "video-upload-service");
        return ResponseEntity.ok(response);
    }
}
