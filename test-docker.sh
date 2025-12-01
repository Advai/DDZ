#!/bin/bash

# Local Docker testing script
# Tests the Docker build and runs the application locally

set -e  # Exit on error

echo "========================================"
echo "Dou Dizhu - Local Docker Test"
echo "========================================"
echo ""

# Check if Docker is running
if ! docker info > /dev/null 2>&1; then
    echo "❌ Docker is not running"
    echo "Please start Docker Desktop and try again"
    exit 1
fi

echo "✅ Docker is running"
echo ""

# Clean up any existing containers
echo "🧹 Cleaning up existing containers..."
docker compose down 2>/dev/null || true
echo ""

# Build and start
echo "========================================
"
echo "Building Docker image..."
echo "========================================"
echo ""

docker compose build --no-cache

echo ""
echo "========================================"
echo "Starting application..."
echo "========================================"
echo ""

docker compose up -d

echo ""
echo "========================================"
echo "✅ Application Started!"
echo "========================================"
echo ""

# Wait for health check
echo "Waiting for application to be ready..."
MAX_ATTEMPTS=30
ATTEMPT=0

while [ $ATTEMPT -lt $MAX_ATTEMPTS ]; do
    if curl -sf http://localhost:8080/actuator/health > /dev/null 2>&1; then
        echo "✅ Application is healthy!"
        break
    fi

    ATTEMPT=$((ATTEMPT + 1))
    echo -n "."
    sleep 2
done

if [ $ATTEMPT -eq $MAX_ATTEMPTS ]; then
    echo ""
    echo "❌ Application failed to start within 60 seconds"
    echo ""
    echo "View logs with: docker compose logs"
    exit 1
fi

echo ""
echo ""
echo "========================================"
echo "🎮 Game is Ready!"
echo "========================================"
echo ""
echo "Open in browser: http://localhost:8080"
echo ""
echo "Useful commands:"
echo "  docker compose logs -f     - View logs"
echo "  docker compose down        - Stop application"
echo "  docker compose restart     - Restart application"
echo ""

# Ask if user wants to open browser
read -p "Open in browser now? (y/n) " -n 1 -r
echo ""
if [[ $REPLY =~ ^[Yy]$ ]]; then
    if command -v open &> /dev/null; then
        open http://localhost:8080
    elif command -v xdg-open &> /dev/null; then
        xdg-open http://localhost:8080
    else
        echo "Please manually open: http://localhost:8080"
    fi
fi

echo ""
echo "To stop: docker compose down"
