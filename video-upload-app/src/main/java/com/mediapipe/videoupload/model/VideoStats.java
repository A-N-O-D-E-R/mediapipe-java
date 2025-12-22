package com.mediapipe.videoupload.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VideoStats {
    private int totalFacesDetected;
    private int framesWithFaces;
    private int framesWithoutFaces;
    private double averageFacesPerFrame;
    private int maxFacesInSingleFrame;
}
