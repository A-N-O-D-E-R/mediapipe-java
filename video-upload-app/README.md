# Video Upload Application with MediaPipe Face Detection

A Spring Boot REST API application that accepts video uploads and performs face detection on each frame using MediaPipe.

## Features

- Video file upload via REST API
- Frame-by-frame face detection using MediaPipe
- Configurable frame skipping for performance optimization
- Detailed statistics about face detections
- Support for various video formats (MP4, AVI, MOV, etc.)

## Prerequisites

- Java 21 or higher
- Maven 3.6+
- Python 3.8+ with the following packages:
  ```bash
  pip install mediapipe>=0.10.9 opencv-python>=4.8.0 numpy>=1.24.0
  ```

## Installation

1. First, install the MediaPipe Java wrapper library:
   ```bash
   cd ..
   mvn clean install
   ```

2. Build the application:
   ```bash
   cd video-upload-app
   mvn clean package
   ```

## Running the Application

### Option 1: Using Maven

```bash
mvn spring-boot:run
```

The application will start on `http://localhost:8080`

### Option 2: Using Docker (Recommended)

Docker provides an isolated environment with all dependencies pre-configured.

#### Using Docker Compose (Easiest)

```bash
# Build and start the application
docker-compose up --build

# Or run in detached mode
docker-compose up -d --build

# View logs
docker-compose logs -f

# Stop the application
docker-compose down
```

The application will be available at `http://localhost:8080`

#### Using Docker CLI

```bash
# Build the Docker image from the parent directory
cd ..
docker build -t mediapipe-video-upload -f video-upload-app/Dockerfile .

# Run the container
docker run -d \
  --name mediapipe-video-upload \
  -p 8080:8080 \
  -e JAVA_OPTS="-Xmx2g -Xms512m" \
  mediapipe-video-upload

# View logs
docker logs -f mediapipe-video-upload

# Stop the container
docker stop mediapipe-video-upload
docker rm mediapipe-video-upload
```

#### Docker Configuration

The Docker setup includes:
- Java 21 Runtime
- Python 3 with MediaPipe, OpenCV, and NumPy pre-installed
- Optimized multi-stage build for smaller image size
- Health checks for container monitoring
- Resource limits (configurable in docker-compose.yml)

**Environment Variables:**

You can customize the application by setting environment variables:

```bash
docker run -d \
  -p 8080:8080 \
  -e JAVA_OPTS="-Xmx4g" \
  -e MEDIAPIPE_FACE_DETECTION_MIN_DETECTION_CONFIDENCE=0.7 \
  -e SPRING_SERVLET_MULTIPART_MAX_FILE_SIZE=1GB \
  mediapipe-video-upload
```

## API Endpoints

### Upload Video for Processing

**Endpoint:** `POST /api/video/upload`

**Content-Type:** `multipart/form-data`

**Parameters:**
- `video` (required): The video file to upload
- `frameSkip` (optional, default=0): Number of frames to skip between processing.
  - `0` = process every frame
  - `1` = process every other frame
  - `5` = process every 6th frame

**Example using cURL:**

```bash
# Process every frame
curl -X POST http://localhost:8080/api/video/upload \
  -F "video=@path/to/your/video.mp4"

# Process every 5th frame for faster processing
curl -X POST http://localhost:8080/api/video/upload \
  -F "video=@path/to/your/video.mp4" \
  -F "frameSkip=4"
```

**Response:**

```json
{
  "videoId": "550e8400-e29b-41d4-a716-446655440000",
  "fileName": "video.mp4",
  "fileSize": 15728640,
  "totalFrames": 300,
  "processedFrames": 300,
  "fps": 30.0,
  "durationSeconds": 10.0,
  "faceDetectionsByFrame": {
    "0": [
      {
        "confidence": 0.95,
        "boundingBox": {
          "x": 120,
          "y": 80,
          "width": 200,
          "height": 250
        },
        "keypoints": [...]
      }
    ],
    "1": [...],
    ...
  },
  "stats": {
    "totalFacesDetected": 450,
    "framesWithFaces": 295,
    "framesWithoutFaces": 5,
    "averageFacesPerFrame": 1.5,
    "maxFacesInSingleFrame": 3
  },
  "status": "COMPLETED",
  "processingTimeMs": 12450
}
```

### Health Check

**Endpoint:** `GET /api/video/health`

```bash
curl http://localhost:8080/api/video/health
```

**Response:**
```json
{
  "status": "UP",
  "service": "video-upload-service"
}
```

## Configuration

Edit `src/main/resources/application.yml` to customize settings:

```yaml
server:
  port: 8080

spring:
  servlet:
    multipart:
      max-file-size: 500MB      # Maximum file size
      max-request-size: 500MB   # Maximum request size

mediapipe:
  python-executable: python3    # Python executable path
  face-detection:
    enabled: true
    min-detection-confidence: 0.5  # 0.0 to 1.0
```

## Performance Considerations

### Frame Skipping

Processing every frame can be slow for long videos. Use the `frameSkip` parameter to improve performance:

- **frameSkip=0**: Best quality, slowest (processes all frames)
- **frameSkip=4**: Good balance (processes every 5th frame)
- **frameSkip=9**: Fast processing (processes every 10th frame)

### Video Size

For large videos, consider:
1. Increasing the `spring.servlet.multipart.max-file-size` setting
2. Using a higher frame skip value
3. Pre-processing videos to lower resolution

### Memory

Video processing is memory-intensive. Ensure your JVM has sufficient heap:

```bash
mvn spring-boot:run -Dspring-boot.run.jvmArguments="-Xmx4g"
```

## Example Usage with Python

```python
import requests

url = "http://localhost:8080/api/video/upload"
files = {'video': open('video.mp4', 'rb')}
params = {'frameSkip': 4}

response = requests.post(url, files=files, params=params)
result = response.json()

print(f"Processed {result['processedFrames']} frames")
print(f"Found {result['stats']['totalFacesDetected']} faces")
print(f"Processing time: {result['processingTimeMs']}ms")
```

## Example Usage with JavaScript (Fetch API)

```javascript
const formData = new FormData();
formData.append('video', videoFile);
formData.append('frameSkip', 4);

fetch('http://localhost:8080/api/video/upload', {
  method: 'POST',
  body: formData
})
.then(response => response.json())
.then(data => {
  console.log('Processed frames:', data.processedFrames);
  console.log('Total faces detected:', data.stats.totalFacesDetected);
})
.catch(error => console.error('Error:', error));
```

## Troubleshooting

### Python Not Found

If you get errors about Python not being found:

1. Ensure Python 3.8+ is installed: `python3 --version`
2. Update the `mediapipe.python-executable` in `application.yml` with the full path
3. Verify MediaPipe is installed: `python3 -c "import mediapipe"`

**Note:** When using Docker, Python is pre-installed and configured, so this issue shouldn't occur.

### Video File Not Supported

Ensure the video codec is supported by OpenCV. Common supported formats:
- MP4 (H.264)
- AVI
- MOV
- MKV

### Out of Memory

For large videos, increase JVM memory:

**Using Maven:**
```bash
export MAVEN_OPTS="-Xmx4g"
mvn spring-boot:run
```

**Using Docker:**
```bash
docker run -d -p 8080:8080 \
  -e JAVA_OPTS="-Xmx4g -Xms1g" \
  mediapipe-video-upload
```

**Using Docker Compose:**

Edit `docker-compose.yml` and adjust the memory limits:
```yaml
deploy:
  resources:
    limits:
      memory: 6G
```

### Docker Build Issues

If you encounter issues building the Docker image:

1. **Build from the parent directory:**
   ```bash
   cd /path/to/mediapipe-java
   docker build -t mediapipe-video-upload -f video-upload-app/Dockerfile .
   ```

2. **Clear Docker cache if needed:**
   ```bash
   docker builder prune
   docker build --no-cache -t mediapipe-video-upload -f video-upload-app/Dockerfile .
   ```

3. **Check Docker has enough resources:**
   - Docker Desktop: Increase memory in settings (recommend 4GB minimum)
   - Linux: Ensure sufficient system memory available

## License

This application is built using the MediaPipe Java Wrapper library.
