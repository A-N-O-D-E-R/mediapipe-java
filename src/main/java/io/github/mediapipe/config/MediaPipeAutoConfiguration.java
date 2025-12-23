package io.github.mediapipe.config;

import io.github.mediapipe.service.FaceDetector;
import io.github.mediapipe.service.FaceLandmarker;
import io.github.mediapipe.util.PythonBridge;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.core.annotation.Order;

import jakarta.annotation.PreDestroy;

/**
 * Auto-configuration for MediaPipe services.
 */
@Slf4j
@AutoConfiguration
@ConditionalOnClass(PythonBridge.class)
@EnableConfigurationProperties(MediaPipeProperties.class)
public class MediaPipeAutoConfiguration {

    private PythonBridge sharedPythonBridge;

    /**
     * Create a shared Python bridge bean.
     */
    @Bean
    @ConditionalOnMissingBean
    @Order(1)
    public PythonBridge pythonBridge(MediaPipeProperties properties) {
        log.info("Creating shared Python bridge with executable: {}", properties.getPythonExecutable());
        sharedPythonBridge = new PythonBridge(properties.getPythonExecutable());
        sharedPythonBridge.start();
        return sharedPythonBridge;
    }

    /**
     * Create a face detector bean if enabled.
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "mediapipe.face-detection", name = "enabled", havingValue = "true", matchIfMissing = true)
    public FaceDetector faceDetector(PythonBridge pythonBridge, MediaPipeProperties properties) {
        log.info("Creating FaceDetector bean with min confidence: {}",
                properties.getFaceDetection().getMinDetectionConfidence());

        FaceDetector detector = new FaceDetector(
                pythonBridge,
                properties.getFaceDetection().getMinDetectionConfidence()
        );
        detector.initialize();
        return detector;
    }

    /**
     * Create a face landmarker bean if enabled.
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "mediapipe.face-landmarker", name = "enabled", havingValue = "true", matchIfMissing = true)
    public FaceLandmarker faceLandmarker(PythonBridge pythonBridge, MediaPipeProperties properties) {
        log.info("Creating FaceLandmarker bean with detection confidence: {}, tracking confidence: {}",
                properties.getFaceLandmarker().getMinDetectionConfidence(),
                properties.getFaceLandmarker().getMinTrackingConfidence());

        FaceLandmarker landmarker = new FaceLandmarker(
                pythonBridge,
                properties.getFaceLandmarker().getMinDetectionConfidence(),
                properties.getFaceLandmarker().getMinTrackingConfidence()
        );
        landmarker.initialize();
        return landmarker;
    }

    @PreDestroy
    public void cleanup() {
        if (sharedPythonBridge != null) {
            log.info("Shutting down shared Python bridge");
            sharedPythonBridge.close();
        }
    }
}
