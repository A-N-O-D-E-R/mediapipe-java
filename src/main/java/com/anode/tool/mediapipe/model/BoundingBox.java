package com.anode.tool.mediapipe.model;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents a bounding box for detected objects.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BoundingBox {
    private int x;
    private int y;
    private int width;
    private int height;

    /**
     * Get the right edge of the bounding box.
     */
    @JsonIgnore
    public int getRight() {
        return x + width;
    }

    /**
     * Get the bottom edge of the bounding box.
     */
    @JsonIgnore
    public int getBottom() {
        return y + height;
    }

    /**
     * Get the center X coordinate.
     */
    @JsonIgnore
    public int getCenterX() {
        return x + width / 2;
    }

    /**
     * Get the center Y coordinate.
     */
    @JsonIgnore
    public int getCenterY() {
        return y + height / 2;
    }
}
