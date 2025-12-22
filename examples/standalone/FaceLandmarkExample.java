import io.github.mediapipe.model.FaceLandmarks;
import io.github.mediapipe.service.FaceLandmarker;

import java.io.File;
import java.util.List;

/**
 * Example of using the FaceLandmarker without Spring Boot.
 */
public class FaceLandmarkExample {

    public static void main(String[] args) {
        // Create a face landmarker with custom confidence thresholds
        try (FaceLandmarker landmarker = new FaceLandmarker(0.5f, 0.5f)) {
            // Initialize the landmarker
            landmarker.initialize();

            // Detect face landmarks in an image
            File imageFile = new File("path/to/your/image.jpg");
            List<FaceLandmarks> faces = landmarker.detectLandmarks(imageFile);

            // Process results
            System.out.println("Detected landmarks for " + faces.size() + " faces");

            for (int i = 0; i < faces.size(); i++) {
                FaceLandmarks face = faces.get(i);
                System.out.println("\nFace " + (i + 1) + ":");
                System.out.println("  Landmarks: " + face.getLandmarks().size());
                System.out.println("  Blend Shapes: " +
                        (face.getBlendshapes() != null ? face.getBlendshapes().size() : 0));

                // Print some blend shapes (facial expressions)
                if (face.getBlendshapes() != null && !face.getBlendshapes().isEmpty()) {
                    System.out.println("  Top expressions:");
                    face.getBlendshapes().stream()
                            .filter(bs -> bs.getScore() > 0.3f)
                            .limit(5)
                            .forEach(bs -> System.out.println("    " +
                                    bs.getCategory() + ": " + bs.getScore()));
                }
            }
        }
    }
}
