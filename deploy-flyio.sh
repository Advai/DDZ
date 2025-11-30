#!/bin/bash

# Deployment script for Fly.io
# This script helps deploy the Dou Dizhu game to Fly.io

set -e  # Exit on error

echo "========================================"
echo "Dou Dizhu - Fly.io Deployment"
echo "========================================"
echo ""

# Check if flyctl is installed
if ! command -v flyctl &> /dev/null; then
    echo "❌ flyctl is not installed"
    echo ""
    echo "Install it with:"
    echo "  macOS:   brew install flyctl"
    echo "  Linux:   curl -L https://fly.io/install.sh | sh"
    echo "  Windows: pwsh -Command \"iwr https://fly.io/install.ps1 -useb | iex\""
    echo ""
    exit 1
fi

echo "✅ flyctl is installed"
echo ""

# Check if logged in
if ! flyctl auth whoami &> /dev/null; then
    echo "⚠️  Not logged in to Fly.io"
    echo ""
    read -p "Do you want to login now? (y/n) " -n 1 -r
    echo ""
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        flyctl auth login
    else
        echo "❌ Please login first with: flyctl auth login"
        exit 1
    fi
fi

echo "✅ Logged in to Fly.io"
echo ""

# Check if fly.toml exists
if [ ! -f "fly.toml" ]; then
    echo "❌ fly.toml not found"
    echo "Please run this script from the project root directory"
    exit 1
fi

# Extract app name from fly.toml
APP_NAME=$(grep -E "^app = " fly.toml | cut -d'"' -f2)

echo "App name: $APP_NAME"
echo ""

# Check if app exists
if flyctl apps list | grep -q "$APP_NAME"; then
    echo "✅ App exists: $APP_NAME"
    DEPLOYING=true
else
    echo "⚠️  App does not exist: $APP_NAME"
    echo ""
    read -p "Do you want to create it now? (y/n) " -n 1 -r
    echo ""
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        flyctl apps create "$APP_NAME"
        echo "✅ App created"
        DEPLOYING=true
    else
        echo "❌ Cannot deploy without creating app first"
        exit 1
    fi
fi

echo ""
echo "========================================
"
echo "Deploying to Fly.io..."
echo "========================================"
echo ""

# Deploy
flyctl deploy

echo ""
echo "========================================"
echo "✅ Deployment Complete!"
echo "========================================"
echo ""

# Get app info
echo "App URL: https://$APP_NAME.fly.dev"
echo ""

echo "Useful commands:"
echo "  flyctl status        - Check app status"
echo "  flyctl logs          - View logs"
echo "  flyctl open          - Open app in browser"
echo "  flyctl ssh console   - SSH into container"
echo ""

# Ask if user wants to open the app
read -p "Do you want to open the app in your browser? (y/n) " -n 1 -r
echo ""
if [[ $REPLY =~ ^[Yy]$ ]]; then
    flyctl open
fi
