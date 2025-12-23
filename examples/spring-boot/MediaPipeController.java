import io.github.mediapipe.model.FaceDetection;
import io.github.mediapipe.model.FaceLandmarks;
import io.github.mediapipe.service.FaceDetector;
import io.github.mediapipe.service.FaceLandmarker;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

/**
 * Example Spring Boot REST controller using MediaPipe services.
 */
@RestController
@RequestMapping("/api/mediapipe")
public class MediaPipeController {

    @Autowired
    private FaceDetector faceDetector;

    @Autowired
    private FaceLandmarker faceLandmarker;

    /**
     * Detect faces in an uploaded image.
     */
    @PostMapping(value = "/detect-faces", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<DetectionResponse> detectFaces(@RequestParam("image") MultipartFile image) {
        try {
            List<FaceDetection> faces = faceDetector.detectFaces(image.getBytes());
            return ResponseEntity.ok(new DetectionResponse(faces.size(), faces));
        } catch (IOException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Detect face landmarks in an uploaded image.
     */
    @PostMapping(value = "/detect-landmarks", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<LandmarkResponse> detectLandmarks(@RequestParam("image") MultipartFile image) {
        try {
            List<FaceLandmarks> landmarks = faceLandmarker.detectLandmarks(image.getBytes());
            return ResponseEntity.ok(new LandmarkResponse(landmarks.size(), landmarks));
        } catch (IOException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    record DetectionResponse(int count, List<FaceDetection> faces) {}
    record LandmarkResponse(int count, List<FaceLandmarks> faces) {}
}
