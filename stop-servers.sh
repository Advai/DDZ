#!/bin/bash

# Dou Dizhu - Stop All Servers

echo "Stopping Dou Dizhu servers..."

pkill -f "http.server 3000"
pkill -f "bootRun"

sleep 2

# Check if ports are free
if lsof -i :3000 > /dev/null 2>&1; then
    echo "⚠️  Warning: Port 3000 still in use"
    lsof -i :3000
else
    echo "✅ Port 3000 is free"
fi

if lsof -i :8080 > /dev/null 2>&1; then
    echo "⚠️  Warning: Port 8080 still in use"
    lsof -i :8080
else
    echo "✅ Port 8080 is free"
fi

echo ""
echo "Servers stopped."
