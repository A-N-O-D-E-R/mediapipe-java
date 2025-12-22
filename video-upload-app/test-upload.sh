#!/bin/bash

# Test script for video upload API
# Usage: ./test-upload.sh <path-to-video> [frameSkip]

if [ "$#" -lt 1 ]; then
    echo "Usage: $0 <path-to-video> [frameSkip]"
    echo "Example: $0 sample-video.mp4 4"
    exit 1
fi

VIDEO_FILE=$1
FRAME_SKIP=${2:-0}
API_URL="http://localhost:8080/api/video/upload"

if [ ! -f "$VIDEO_FILE" ]; then
    echo "Error: Video file '$VIDEO_FILE' not found"
    exit 1
fi

echo "Uploading video: $VIDEO_FILE"
echo "Frame skip: $FRAME_SKIP"
echo "API URL: $API_URL"
echo ""

curl -X POST "$API_URL" \
  -F "video=@$VIDEO_FILE;type=video/mp4" \
  -F "frameSkip=$FRAME_SKIP" \
  -H "Accept: application/json" \
  -w "\n\nHTTP Status: %{http_code}\n" \
  | jq '.' 2>/dev/null || cat

echo ""
echo "Upload completed!"
