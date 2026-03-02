package com.anode.tool.mediapipe.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents a facial blend shape (expression coefficient).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BlendShape {
    private String category;
    private float score;
}
