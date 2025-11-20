#!/bin/bash

# Dou Dizhu - Start Both Servers
# This script starts both the frontend and backend servers

echo "======================================"
echo "Starting Dou Dizhu Servers"
echo "======================================"

# Kill any existing servers
pkill -f "http.server 3000" 2>/dev/null
pkill -f "bootRun" 2>/dev/null
sleep 1

# Get the directory where this script is located
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

echo ""
echo "Starting frontend web server on port 3000..."
cd "$SCRIPT_DIR/web"
python3 -m http.server 3000 > /tmp/ddz-frontend.log 2>&1 &
FRONTEND_PID=$!
echo "Frontend PID: $FRONTEND_PID"

sleep 2

echo ""
echo "Starting backend game server on port 8080..."
cd "$SCRIPT_DIR"
./gradlew :server:bootRun > /tmp/ddz-backend.log 2>&1 &
BACKEND_PID=$!
echo "Backend PID: $BACKEND_PID"

echo ""
echo "======================================"
echo "Servers are starting..."
echo "======================================"
echo ""
echo "Frontend log: tail -f /tmp/ddz-frontend.log"
echo "Backend log: tail -f /tmp/ddz-backend.log"
echo ""
echo "Waiting for backend to start (this takes ~10 seconds)..."

# Wait for backend to be ready
for i in {1..30}; do
    if curl -s http://localhost:8080/actuator/health > /dev/null 2>&1; then
        echo ""
        echo "✅ Backend is ready!"
        break
    fi
    echo -n "."
    sleep 1
done

echo ""
echo "======================================"
echo "🎮 READY TO PLAY!"
echo "======================================"
echo ""
echo "Open your browser to: http://localhost:3000"
echo ""
echo "To stop servers:"
echo "  kill $FRONTEND_PID $BACKEND_PID"
echo "Or run: ./stop-servers.sh"
echo ""
echo "Servers are running in background."
echo "Check logs with:"
echo "  tail -f /tmp/ddz-frontend.log"
echo "  tail -f /tmp/ddz-backend.log"
echo ""
