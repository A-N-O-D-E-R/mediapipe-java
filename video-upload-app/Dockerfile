# Multi-stage Dockerfile for Video Upload App with MediaPipe

# Stage 1: Build stage
FROM maven:3.9-eclipse-temurin-21 AS build

# Install Python and dependencies
RUN apt-get update && apt-get install -y \
    python3 \
    python3-pip \
    python3-venv \
    libglib2.0-0 \
    && rm -rf /var/lib/apt/lists/*

# Create a virtual environment for Python packages
RUN python3 -m venv /opt/venv
ENV PATH="/opt/venv/bin:$PATH"

# Install MediaPipe and OpenCV Python packages
RUN pip install --no-cache-dir \
    mediapipe>=0.10.9 \
    opencv-python-headless>=4.8.0 \
    numpy>=1.24.0

# Set working directory
WORKDIR /app

# Copy parent pom first and install the mediapipe-java-wrapper library
COPY ./pom.xml /app/parent/pom.xml
COPY ./src /app/parent/src
WORKDIR /app/parent
RUN mvn clean install -DskipTests

# Now build the video upload app
WORKDIR /app/video-upload
COPY video-upload-app/pom.xml .
RUN mvn dependency:go-offline

COPY video-upload-app/src ./src
RUN mvn clean package -DskipTests

# Stage 2: Runtime stage
FROM eclipse-temurin:21-jre

# Install Python runtime and required system libraries
RUN apt-get update && apt-get install -y \
    python3 \
    python3-venv \
    libglib2.0-0 \
    libgomp1 \
    && rm -rf /var/lib/apt/lists/*

# Copy Python virtual environment from build stage
COPY --from=build /opt/venv /opt/venv
ENV PATH="/opt/venv/bin:$PATH"

# Set Python executable path for MediaPipe
ENV MEDIAPIPE_PYTHON_EXECUTABLE=/opt/venv/bin/python3

# Create app directory
WORKDIR /app

# Copy the built JAR from build stage
COPY --from=build /app/video-upload/target/*.jar app.jar

# Create directory for temporary video files
RUN mkdir -p /tmp/videos && chmod 777 /tmp/videos

# Expose application port
EXPOSE 8080

# Set JVM options for container
ENV JAVA_OPTS="-Xmx2g -Xms512m"

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
    CMD curl -f http://localhost:8080/api/video/health || exit 1

# Run the application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
