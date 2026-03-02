package com.anode.tool.mediapipe.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents a 2D keypoint (e.g., facial feature).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Keypoint {
    private String type;
    private float x;
    private float y;
}
