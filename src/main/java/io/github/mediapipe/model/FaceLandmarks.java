package io.github.mediapipe.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Result of face landmark detection containing landmarks, blend shapes, and transformation matrix.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FaceLandmarks {
    private List<Landmark> landmarks;
    private List<BlendShape> blendshapes;
    private double[][] transformationMatrix;
}
