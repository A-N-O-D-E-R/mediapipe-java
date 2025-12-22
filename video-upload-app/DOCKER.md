# Docker Deployment Guide

This guide provides detailed information about deploying the MediaPipe Video Upload Application using Docker.

## Table of Contents

- [Quick Start](#quick-start)
- [Docker Images](#docker-images)
- [Configuration](#configuration)
- [Deployment Methods](#deployment-methods)
- [Production Deployment](#production-deployment)
- [Monitoring](#monitoring)
- [Troubleshooting](#troubleshooting)

## Quick Start

The fastest way to get started is using the provided script:

```bash
./docker-run.sh
```

This will build and start the application. Access it at `http://localhost:8080`

### Quick Start Commands

```bash
# Start the application
./docker-run.sh start

# View logs
./docker-run.sh logs

# Check status
./docker-run.sh status

# Stop the application
./docker-run.sh stop

# Get help
./docker-run.sh help
```

## Docker Images

### Base Images

- **Build Stage:** `maven:3.9-eclipse-temurin-21`
  - Includes Maven and JDK 21 for building the application
  - Installs Python 3 and MediaPipe dependencies

- **Runtime Stage:** `eclipse-temurin:21-jre`
  - Minimal JRE 21 runtime
  - Python 3 runtime with MediaPipe pre-installed
  - OpenCV system libraries

### Image Size

- **Build Image:** ~2.5GB (not included in final image)
- **Final Runtime Image:** ~1.2GB

## Configuration

### Environment Variables

Configure the application using environment variables:

#### JVM Settings

```bash
JAVA_OPTS="-Xmx2g -Xms512m"
```

Recommended settings by video size:
- Small videos (<100MB): `-Xmx1g -Xms256m`
- Medium videos (100MB-500MB): `-Xmx2g -Xms512m`
- Large videos (>500MB): `-Xmx4g -Xms1g`

#### MediaPipe Settings

```bash
# Python executable path (pre-configured in Docker)
MEDIAPIPE_PYTHON_EXECUTABLE=/opt/venv/bin/python3

# Face detection settings
MEDIAPIPE_FACE_DETECTION_ENABLED=true
MEDIAPIPE_FACE_DETECTION_MIN_DETECTION_CONFIDENCE=0.5
```

#### Spring Boot Settings

```bash
# Server port
SERVER_PORT=8080

# File upload limits
SPRING_SERVLET_MULTIPART_MAX_FILE_SIZE=500MB
SPRING_SERVLET_MULTIPART_MAX_REQUEST_SIZE=500MB

# Logging
LOGGING_LEVEL_COM_MEDIAPIPE_VIDEOUPLOAD=INFO
LOGGING_LEVEL_IO_GITHUB_MEDIAPIPE=INFO
```

### Docker Compose Configuration

Edit `docker-compose.yml` to customize:

```yaml
environment:
  - JAVA_OPTS=-Xmx4g -Xms1g
  - MEDIAPIPE_FACE_DETECTION_MIN_DETECTION_CONFIDENCE=0.7

deploy:
  resources:
    limits:
      cpus: '4'
      memory: 4G
```

## Deployment Methods

### Method 1: Docker Compose (Recommended)

**Advantages:**
- Simple configuration
- Easy to manage
- Automatic restarts
- Volume management

```bash
# Start
docker-compose up -d

# View logs
docker-compose logs -f

# Stop
docker-compose down
```

### Method 2: Docker CLI

**Advantages:**
- More control
- No docker-compose dependency
- Easier integration with orchestration tools

```bash
# Build from parent directory
cd /path/to/mediapipe-java
docker build -t mediapipe-video-upload -f video-upload-app/Dockerfile .

# Run with custom settings
docker run -d \
  --name mediapipe-video-upload \
  -p 8080:8080 \
  -e JAVA_OPTS="-Xmx2g" \
  -v $(pwd)/uploads:/app/uploads \
  --restart unless-stopped \
  mediapipe-video-upload
```

### Method 3: Docker Swarm

For high availability:

```bash
# Initialize swarm
docker swarm init

# Deploy stack
docker stack deploy -c docker-compose.yml mediapipe-stack

# Scale service
docker service scale mediapipe-stack_video-upload-app=3

# Remove stack
docker stack rm mediapipe-stack
```

## Production Deployment

### Resource Requirements

**Minimum:**
- CPU: 2 cores
- Memory: 2GB
- Disk: 10GB

**Recommended:**
- CPU: 4 cores
- Memory: 4GB
- Disk: 20GB (more for video storage)

### Production-Ready docker-compose.yml

```yaml
version: '3.8'

services:
  video-upload-app:
    build:
      context: ..
      dockerfile: video-upload-app/Dockerfile
    image: mediapipe-video-upload:1.0.0
    container_name: mediapipe-video-upload-prod
    ports:
      - "8080:8080"
    environment:
      - JAVA_OPTS=-Xmx4g -Xms1g -XX:+UseG1GC
      - SPRING_PROFILES_ACTIVE=production
    volumes:
      - video-data:/app/data
      - video-uploads:/app/uploads
      - video-logs:/app/logs
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8080/api/video/health"]
      interval: 30s
      timeout: 10s
      retries: 3
      start_period: 60s
    restart: always
    deploy:
      resources:
        limits:
          cpus: '4'
          memory: 4G
        reservations:
          cpus: '2'
          memory: 2G
    logging:
      driver: "json-file"
      options:
        max-size: "10m"
        max-file: "3"

volumes:
  video-data:
  video-uploads:
  video-logs:
```

### Reverse Proxy Setup (Nginx)

```nginx
server {
    listen 80;
    server_name your-domain.com;

    client_max_body_size 500M;

    location / {
        proxy_pass http://localhost:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;

        # Timeouts for video processing
        proxy_read_timeout 600s;
        proxy_connect_timeout 600s;
        proxy_send_timeout 600s;
    }
}
```

## Monitoring

### Health Checks

Built-in health check endpoint:

```bash
curl http://localhost:8080/api/video/health
```

Docker automatically monitors this endpoint.

### View Container Stats

```bash
# Real-time stats
docker stats mediapipe-video-upload

# Container logs
docker logs -f mediapipe-video-upload

# Docker Compose logs
docker-compose logs -f
```

### Prometheus Metrics (Optional)

Add Spring Boot Actuator to expose metrics:

```yaml
# In docker-compose.yml
environment:
  - MANAGEMENT_ENDPOINTS_WEB_EXPOSURE_INCLUDE=health,info,metrics,prometheus
```

## Troubleshooting

### Container Won't Start

```bash
# Check logs
docker-compose logs

# Check container status
docker-compose ps

# Inspect container
docker inspect mediapipe-video-upload
```

### Out of Memory

```bash
# Check memory usage
docker stats mediapipe-video-upload

# Increase memory limit in docker-compose.yml
deploy:
  resources:
    limits:
      memory: 6G
```

### Python/MediaPipe Errors

```bash
# Enter container
docker exec -it mediapipe-video-upload /bin/bash

# Check Python
python3 --version

# Test MediaPipe
python3 -c "import mediapipe; print(mediapipe.__version__)"

# Check OpenCV
python3 -c "import cv2; print(cv2.__version__)"
```

### Video Upload Fails

1. **Check file size limits:**
   ```bash
   # Increase in docker-compose.yml
   environment:
     - SPRING_SERVLET_MULTIPART_MAX_FILE_SIZE=1GB
   ```

2. **Check disk space:**
   ```bash
   docker system df
   docker system prune
   ```

3. **Check logs:**
   ```bash
   docker-compose logs -f | grep -i error
   ```

### Slow Processing

1. **Increase CPU cores:**
   ```yaml
   deploy:
     resources:
       limits:
         cpus: '8'
   ```

2. **Use frame skipping:**
   ```bash
   curl -F "video=@video.mp4" -F "frameSkip=9" \
     http://localhost:8080/api/video/upload
   ```

3. **Increase JVM heap:**
   ```bash
   -e JAVA_OPTS="-Xmx6g -Xms2g"
   ```

## Cleanup

```bash
# Stop and remove containers
docker-compose down

# Remove containers and volumes
docker-compose down -v

# Remove images
docker rmi mediapipe-video-upload

# Full cleanup
docker system prune -a --volumes
```

## Security Best Practices

1. **Don't run as root** (already configured in Dockerfile)
2. **Limit resources** (configured in docker-compose.yml)
3. **Use secrets** for sensitive configuration
4. **Enable HTTPS** with reverse proxy
5. **Regular security updates:**
   ```bash
   docker-compose pull
   docker-compose up -d --build
   ```

## Backup and Recovery

### Backup Volumes

```bash
# Backup uploads volume
docker run --rm -v mediapipe-video-uploads:/data \
  -v $(pwd):/backup ubuntu \
  tar czf /backup/uploads-backup.tar.gz /data
```

### Restore Volumes

```bash
# Restore uploads volume
docker run --rm -v mediapipe-video-uploads:/data \
  -v $(pwd):/backup ubuntu \
  tar xzf /backup/uploads-backup.tar.gz -C /
```

## Advanced Configuration

### Custom Network

```yaml
services:
  video-upload-app:
    networks:
      - mediapipe-network

networks:
  mediapipe-network:
    driver: bridge
```

### Using External Database (Future)

```yaml
services:
  video-upload-app:
    depends_on:
      - postgres
    environment:
      - SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/mediapipe

  postgres:
    image: postgres:15
    environment:
      - POSTGRES_PASSWORD=secret
    volumes:
      - postgres-data:/var/lib/postgresql/data
```

## Performance Tuning

### JVM Tuning

```bash
JAVA_OPTS="-Xmx4g -Xms2g \
  -XX:+UseG1GC \
  -XX:MaxGCPauseMillis=200 \
  -XX:+UseStringDeduplication"
```

### Container Resource Limits

```yaml
deploy:
  resources:
    limits:
      cpus: '8'
      memory: 8G
      pids: 1000
```

## Support

For issues or questions:
1. Check the logs: `docker-compose logs -f`
2. Review this documentation
3. Check the main README.md
4. Open an issue on GitHub
