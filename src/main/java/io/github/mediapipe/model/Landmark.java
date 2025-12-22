package io.github.mediapipe.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents a 3D landmark point.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Landmark {
    private float x;
    private float y;
    private float z;
}
