package io.github.mediapipe.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Result of face detection containing bounding box, keypoints, and confidence.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FaceDetection {
    private BoundingBox boundingBox;
    private List<Keypoint> keypoints;
    private float confidence;
}
