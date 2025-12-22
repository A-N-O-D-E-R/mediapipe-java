import io.github.mediapipe.model.FaceDetection;
import io.github.mediapipe.service.FaceDetector;

import java.io.File;
import java.util.List;

/**
 * Example of using the FaceDetector without Spring Boot.
 */
public class FaceDetectionExample {

    public static void main(String[] args) {
        // Create a face detector with 50% confidence threshold
        try (FaceDetector detector = new FaceDetector(0.5f)) {
            // Initialize the detector
            detector.initialize();

            // Detect faces in an image
            File imageFile = new File("path/to/your/image.jpg");
            List<FaceDetection> faces = detector.detectFaces(imageFile);

            // Process results
            System.out.println("Detected " + faces.size() + " faces");

            for (int i = 0; i < faces.size(); i++) {
                FaceDetection face = faces.get(i);
                System.out.println("\nFace " + (i + 1) + ":");
                System.out.println("  Confidence: " + face.getConfidence());
                System.out.println("  Bounding Box: " +
                        "(" + face.getBoundingBox().getX() + ", " +
                        face.getBoundingBox().getY() + ") " +
                        face.getBoundingBox().getWidth() + "x" +
                        face.getBoundingBox().getHeight());
                System.out.println("  Keypoints: " + face.getKeypoints().size());
            }
        }
    }
}
