MediaPipe Java Wrapper
=========================
---
A Java wrapper library for Google MediaPipe with Spring Boot support, enabling easy face detection and face landmark detection in Java/Spring applications.

## Features

- **Face Detection**: Detect faces in images with bounding boxes and keypoints
- **Face Landmark Detection**: Detect 468 facial landmarks with blend shapes and transformation matrices
- **Spring Boot Auto-Configuration**: Seamless integration with Spring Boot applications
- **Cross-Platform**: Works on Linux, macOS, and Windows (x86_64)
- **Easy to Use**: Clean Java API with minimal setup

## Architecture

This library uses a Python bridge approach:
- **Java API**: Clean, idiomatic Java interfaces
- **Python Backend**: MediaPipe Python (pre-built binaries available for all platforms)
- **Transparent Communication**: JSON-based IPC via stdin/stdout

This approach ensures:
- **Easy Installation**: No need to build native libraries from source
- **Cross-Platform Support**: Works on any platform with Python support
- **Up-to-Date**: Leverages the officially maintained MediaPipe Python package

## Prerequisites

### Required
- **Java 21** or higher
- **Python 3.8** or higher
- **Maven** (for building from source)

### Python Dependencies
The library will communicate with Python, which needs these packages:
```bash
pip install mediapipe>=0.10.9 opencv-python>=4.8.0 numpy>=1.24.0
```

Or use the provided requirements file:
```bash
pip install -r src/main/resources/python/requirements.txt
```

## Installation

### Maven

Add to your `pom.xml`:

```xml
<dependency>
    <groupId>com.anode.tool.mediapipe</groupId>
    <artifactId>mediapipe-java-wrapper</artifactId>
    <version>0.1.0-SNAPSHOT</version>
</dependency>
```

### Gradle

Add to your `build.gradle`:

```gradle
implementation 'com.anode.tool.mediapipe:mediapipe-java-wrapper:0.1.0-SNAPSHOT'
```

## Quick Start

### Standalone Java

```java
import com.anode.tool.mediapipe.model.FaceDetection;
import com.anode.tool.mediapipe.service.FaceDetector;

import java.io.File;
import java.util.List;

public class Example {
    public static void main(String[] args) {
        try (FaceDetector detector = new FaceDetector(0.5f)) {
            detector.initialize();

            List<FaceDetection> faces = detector.detectFaces(
                new File("image.jpg")
            );

            System.out.println("Detected " + faces.size() + " faces");
            faces.forEach(face -> {
                System.out.println("Confidence: " + face.getConfidence());
                System.out.println("Bounding Box: " + face.getBoundingBox());
            });
        }
    }
}
```

### Spring Boot

#### 1. Add dependency to `pom.xml`

```xml
<dependency>
    <groupId>com.anode.tool.mediapipe</groupId>
    <artifactId>mediapipe-java-wrapper</artifactId>
    <version>0.1.0-SNAPSHOT</version>
</dependency>
```

#### 2. Configure in `application.yml`

```yaml
mediapipe:
  python-executable: python3  # Optional, defaults to python3
  face-detection:
    enabled: true
    min-detection-confidence: 0.5
  face-landmarker:
    enabled: true
    min-detection-confidence: 0.5
    min-tracking-confidence: 0.5
```

#### 3. Use in your service/controller

```java
@RestController
public class FaceDetectionController {

    @Autowired
    private FaceDetector faceDetector;

    @PostMapping("/detect-faces")
    public ResponseEntity<?> detect(@RequestParam("image") MultipartFile image) {
        List<FaceDetection> faces = faceDetector.detectFaces(image.getBytes());
        return ResponseEntity.ok(faces);
    }
}
```

## Usage Examples

### Face Detection

```java
try (FaceDetector detector = new FaceDetector(0.5f)) {
    detector.initialize();

    // From file
    List<FaceDetection> faces = detector.detectFaces(new File("image.jpg"));

    // From bytes
    byte[] imageBytes = Files.readAllBytes(Path.of("image.jpg"));
    faces = detector.detectFaces(imageBytes);

    // Process results
    for (FaceDetection face : faces) {
        BoundingBox bbox = face.getBoundingBox();
        System.out.printf("Face at (%d, %d) size %dx%d%n",
            bbox.getX(), bbox.getY(),
            bbox.getWidth(), bbox.getHeight());

        List<Keypoint> keypoints = face.getKeypoints();
        System.out.println("Keypoints: " + keypoints.size());
    }
}
```

### Face Landmark Detection

```java
try (FaceLandmarker landmarker = new FaceLandmarker(0.5f, 0.5f)) {
    landmarker.initialize();

    List<FaceLandmarks> faces = landmarker.detectLandmarks(new File("image.jpg"));

    for (FaceLandmarks face : faces) {
        // 468 facial landmarks
        List<Landmark> landmarks = face.getLandmarks();
        System.out.println("Landmarks: " + landmarks.size());

        // Facial expressions (blend shapes)
        List<BlendShape> expressions = face.getBlendshapes();
        expressions.stream()
            .filter(bs -> bs.getScore() > 0.3f)
            .forEach(bs -> System.out.println(
                bs.getCategory() + ": " + bs.getScore()
            ));

        // Transformation matrix (for 3D face orientation)
        double[][] matrix = face.getTransformationMatrix();
    }
}
```

### Shared Python Bridge (Advanced)

If you're using multiple detectors, you can share a single Python bridge:

```java
PythonBridge bridge = new PythonBridge("python3");
bridge.start();

FaceDetector detector = new FaceDetector(bridge, 0.5f);
FaceLandmarker landmarker = new FaceLandmarker(bridge, 0.5f, 0.5f);

detector.initialize();
landmarker.initialize();

// Use both services...

bridge.close();
```

## Configuration

### Spring Boot Properties

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `mediapipe.python-executable` | String | `python3` | Path to Python executable |
| `mediapipe.face-detection.enabled` | Boolean | `true` | Enable face detection bean |
| `mediapipe.face-detection.min-detection-confidence` | Float | `0.5` | Minimum detection confidence (0.0-1.0) |
| `mediapipe.face-landmarker.enabled` | Boolean | `true` | Enable face landmarker bean |
| `mediapipe.face-landmarker.min-detection-confidence` | Float | `0.5` | Minimum detection confidence (0.0-1.0) |
| `mediapipe.face-landmarker.min-tracking-confidence` | Float | `0.5` | Minimum tracking confidence (0.0-1.0) |

## Building from Source

```bash
# Clone the repository
git clone https://github.com/yourusername/mediapipe-java-wrapper.git
cd mediapipe-java-wrapper

# Install Python dependencies
pip install -r src/main/resources/python/requirements.txt

# Build with Maven
mvn clean install

# Run tests
mvn test
```

## Troubleshooting

### Python not found

If you get a `PythonRuntimeException` about Python not being found:

1. **Check Python installation**:
   ```bash
   python3 --version
   ```

2. **Specify full path** in Spring Boot:
   ```yaml
   mediapipe:
     python-executable: /usr/bin/python3
   ```

3. **Or in standalone Java**:
   ```java
   PythonBridge bridge = new PythonBridge("/usr/bin/python3");
   ```

### MediaPipe module not found

Install MediaPipe Python package:
```bash
pip install mediapipe opencv-python numpy
```

### Permission denied on Linux

The Python script needs execute permissions:
```bash
chmod +x src/main/resources/python/mediapipe_bridge.py
```

## Performance Considerations

- **First Detection**: The first detection will be slower as Python loads MediaPipe models
- **Subsequent Detections**: Much faster as models are cached
- **Batch Processing**: Process multiple images in sequence using the same detector instance
- **Concurrent Processing**: Create multiple detector instances (each with its own Python process)

## Limitations

- **Requires Python Runtime**: Python 3.8+ must be installed on the system
- **Process Overhead**: Communication via process pipes has some overhead (typically <50ms)
- **Model Loading**: First detection takes ~1-2 seconds to load models

## Roadmap

- [ ] Add hand tracking support
- [ ] Add pose estimation support
- [ ] Add object detection support
- [ ] Improve error handling and recovery
- [ ] Add metrics and monitoring support


## External refs

- [Google MediaPipe](https://developers.google.com/mediapipe) for the excellent ML framework
- [MediaPipe Python](https://pypi.org/project/mediapipe/) for the pre-built binaries
