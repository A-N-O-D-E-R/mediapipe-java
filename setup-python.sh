#!/bin/bash

# Setup script for MediaPipe Python dependencies

set -e

echo "MediaPipe Java Wrapper - Python Setup"
echo "======================================"
echo ""

# Check for Python
if ! command -v python3 &> /dev/null; then
    echo "Error: python3 is not installed or not in PATH"
    echo "Please install Python 3.8 or higher"
    exit 1
fi

PYTHON_VERSION=$(python3 --version | cut -d' ' -f2)
echo "Found Python: $PYTHON_VERSION"

# Check Python version is >= 3.8
REQUIRED_VERSION="3.8"
if ! python3 -c "import sys; exit(0 if sys.version_info >= (3, 8) else 1)"; then
    echo "Error: Python 3.8 or higher is required"
    echo "Current version: $PYTHON_VERSION"
    exit 1
fi

echo ""
echo "Installing MediaPipe Python dependencies..."
echo ""

# Install dependencies
pip install -r src/main/resources/python/requirements.txt

echo ""
echo "Verifying installation..."
python3 -c "import mediapipe; print(f'MediaPipe version: {mediapipe.__version__}')"
python3 -c "import cv2; print(f'OpenCV version: {cv2.__version__}')"
python3 -c "import numpy; print(f'NumPy version: {numpy.__version__}')"

echo ""
echo "Setup complete! You can now use the MediaPipe Java wrapper."
echo ""
echo "To test, try running one of the examples in the examples/ directory."
