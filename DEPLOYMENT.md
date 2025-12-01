# Dou Dizhu (斗地主) - Deployment Guide

This guide explains how to deploy the Dou Dizhu card game to production so you and your friends can play online.

## Table of Contents

1. [Architecture Overview](#architecture-overview)
2. [Local Testing with Docker](#local-testing-with-docker)
3. [Fly.io Deployment (Recommended)](#flyio-deployment-recommended)
4. [Alternative Deployment Options](#alternative-deployment-options)
5. [Configuration](#configuration)
6. [Troubleshooting](#troubleshooting)

---

## Architecture Overview

The application consists of:
- **Backend**: Spring Boot (Java 21) REST API + WebSocket server
- **Frontend**: Static HTML/CSS/JavaScript single-page application
- **Deployment**: Single Docker container serving both frontend and backend

**Key Features:**
- In-memory game state (games reset on server restart)
- No database required
- WebSocket for real-time game updates
- CORS enabled for development/testing

---

## Local Testing with Docker

### Prerequisites

- Docker Desktop installed ([Download](https://www.docker.com/products/docker-desktop/))
- Docker Compose (included with Docker Desktop)

### Build and Run

```bash
# Build and start the application
docker compose up --build

# Access the game
open http://localhost:8080
```

The application will be available at `http://localhost:8080`.

### Stop the Application

```bash
# Stop and remove containers
docker compose down

# Stop, remove containers, and clear volumes
docker compose down -v
```

### View Logs

```bash
# View real-time logs
docker compose logs -f

# View logs for specific service
docker compose logs -f ddz-game
```

---

## Fly.io Deployment (Recommended)

Fly.io provides:
- ✅ Free tier (perfect for 1-2 concurrent games)
- ✅ Free subdomain (e.g., `ddz-game.fly.dev`)
- ✅ Global CDN with automatic HTTPS
- ✅ WebSocket support
- ✅ Simple one-command deployment

### Step 1: Install Fly.io CLI

**macOS:**
```bash
brew install flyctl
```

**Linux:**
```bash
curl -L https://fly.io/install.sh | sh
```

**Windows:**
```powershell
pwsh -Command "iwr https://fly.io/install.ps1 -useb | iex"
```

[More installation options](https://fly.io/docs/hands-on/install-flyctl/)

### Step 2: Sign Up / Login

```bash
# Sign up for free account
flyctl auth signup

# Or login if you already have an account
flyctl auth login
```

### Step 3: Configure Your App

Edit `fly.toml` and customize:

```toml
app = "your-game-name"  # Change this! Must be globally unique
primary_region = "ord"   # Change to region closest to you
```

**Available regions:**
- `ord` - Chicago, USA
- `iad` - Ashburn, Virginia, USA
- `lax` - Los Angeles, USA
- `ewr` - Secaucus, New Jersey, USA
- `lhr` - London, UK
- `fra` - Frankfurt, Germany
- `sin` - Singapore
- `syd` - Sydney, Australia

[Full list of regions](https://fly.io/docs/reference/regions/)

### Step 4: Deploy

```bash
# Create the app (first time only)
flyctl apps create your-game-name

# Deploy the application
flyctl deploy

# The deployment will:
# 1. Build the Docker image in the cloud
# 2. Deploy to Fly.io infrastructure
# 3. Provide you with a URL like https://your-game-name.fly.dev
```

**First deployment takes ~3-5 minutes** (building Docker image).
Subsequent deployments are faster (~1-2 minutes).

### Step 5: Open Your Game

```bash
# Open the deployed app in your browser
flyctl open

# Or manually visit: https://your-game-name.fly.dev
```

### Managing Your Deployment

```bash
# View application status
flyctl status

# View real-time logs
flyctl logs

# View app info and URL
flyctl info

# Scale to more VMs (if needed)
flyctl scale count 2

# Scale VM resources (requires paid plan)
flyctl scale vm shared-cpu-2x --memory 512

# SSH into your container (debugging)
flyctl ssh console

# Restart the app
flyctl apps restart

# Delete the app
flyctl apps destroy your-game-name
```

---

## Alternative Deployment Options

### Railway

[Railway](https://railway.app/) is another excellent free option:

1. Sign up at https://railway.app
2. Click "New Project" → "Deploy from GitHub repo"
3. Select your repository
4. Railway auto-detects Dockerfile and deploys

**Pros:**
- Free tier with good limits
- GitHub integration for auto-deploy
- Easy database addition if needed later

### Render

[Render](https://render.com/) offers free tier web services:

1. Sign up at https://render.com
2. New → Web Service
3. Connect repository
4. Build command: `docker build -t ddz .`
5. Start command: Docker image

### Self-Hosted (VPS)

If you have a VPS (DigitalOcean, Linode, etc.):

```bash
# On your VPS
git clone <your-repo>
cd DDZ
docker compose up -d

# Set up reverse proxy with nginx
# Configure domain and SSL with Let's Encrypt
```

---

## Configuration

### Backend URL Configuration

The frontend automatically detects the backend URL:
- **Local development**: Uses `http://localhost:8080`
- **Production**: Uses same origin as frontend (`window.location.origin`)

To manually configure, edit `web/config.js`:

```javascript
window.DDZ_CONFIG = {
    BACKEND_URL: 'https://your-backend.fly.dev',
    // WebSocket URL is auto-derived
};
```

### Environment Variables

Available environment variables (set in `fly.toml` or `docker-compose.yml`):

```bash
SERVER_PORT=8080                  # Server port (default: 8080)
SPRING_PROFILES_ACTIVE=production # Spring profile
```

### CORS Configuration

**Development:** CORS allows all origins (configured in `GameController.java`)

**Production:** You may want to restrict CORS to your domain.

Edit `server/src/main/java/com/yourco/ddz/server/api/GameController.java`:

```java
@CrossOrigin(origins = "https://your-domain.com")
```

---

## Updating Your Deployment

### Fly.io

After making code changes:

```bash
# Commit your changes
git add .
git commit -m "Update game logic"

# Deploy updated version
flyctl deploy
```

### Railway / Render

With GitHub integration enabled:
1. Push changes to your repository
2. Automatic deployment triggers
3. Wait ~2-5 minutes for deployment

### Docker Compose (Local/VPS)

```bash
# Rebuild and restart
docker compose up --build -d

# Or with no cache
docker compose build --no-cache
docker compose up -d
```

---

## Custom Domain Setup

### Fly.io Custom Domain

```bash
# Add your domain
flyctl certs create your-domain.com

# Follow instructions to add DNS records
# Usually: A record pointing to Fly.io IP
```

### Domain Providers

Recommended registrars for cheap domains:
- **Namecheap**: $8-12/year for .com
- **Cloudflare**: At-cost pricing (~$9/year)
- **Porkbun**: $9-11/year for .com

---

## Troubleshooting

### Build Fails

**Issue:** Docker build fails with Java errors

**Solution:**
```bash
# Clean local Gradle cache
./gradlew clean

# Rebuild without Docker cache
docker compose build --no-cache
```

### Application Won't Start

**Check logs:**
```bash
# Docker Compose
docker compose logs ddz-game

# Fly.io
flyctl logs
```

**Common issues:**
- Port already in use (change port in config)
- Insufficient memory (upgrade VM size)
- Java heap space (add JVM options)

### WebSocket Connection Fails

**Issue:** Games don't update in real-time

**Causes:**
- Proxy/load balancer not configured for WebSocket
- CORS blocking WebSocket connections
- Client using wrong WebSocket URL

**Solution:**
- Verify WebSocket URL in browser console
- Check that Fly.io/Railway supports WebSockets (they do)
- Ensure HTTPS is used (ws:// vs wss://)

### Games Disappear After Restart

This is **expected behavior**. Games are stored in-memory.

**Solutions:**
1. Keep server running (set `auto_stop_machines = false` in `fly.toml`)
2. Add persistence layer (Redis/PostgreSQL) - future enhancement

### Can't Connect from Friends' Computers

**Checklist:**
- ✅ App is deployed to public URL (not localhost)
- ✅ Friends are using HTTPS URL (not HTTP)
- ✅ Firewall allows inbound connections
- ✅ CORS is configured correctly

---

## Cost Estimates

### Free Tier (Recommended for Testing)

**Fly.io:**
- 3 shared VMs (256MB RAM each)
- 160GB outbound data transfer/month
- **Cost:** $0/month

**Railway:**
- $5 free credit/month
- ~500 hours execution time
- **Cost:** $0/month (within free tier)

### Paid Tier (For Heavy Usage)

**Fly.io (if you outgrow free tier):**
- Shared CPU VM: ~$2-5/month
- Dedicated CPU VM: ~$30/month
- Additional bandwidth: $0.02/GB

**Railway:**
- $5/month per developer
- Usage-based pricing after free tier

**VPS (Self-hosted):**
- DigitalOcean: $6/month (basic droplet)
- Linode: $5/month (Nanode)
- Hetzner: €4/month (basic VPS)

---

## Security Notes

⚠️ **Current Security Status:**

- ❌ No authentication
- ❌ No authorization
- ❌ CORS allows all origins
- ❌ No rate limiting
- ❌ No input sanitization beyond game rules

**This is intentional** for an MVP game to play with friends.

**For public deployment**, consider adding:
- User authentication (Spring Security)
- Session management
- Rate limiting
- Input validation
- HTTPS enforcement
- Restricted CORS origins

---

## Getting Help

- **Fly.io Docs**: https://fly.io/docs/
- **Docker Docs**: https://docs.docker.com/
- **Spring Boot Docs**: https://spring.io/projects/spring-boot

---

## Summary

**Quickest Path to Playing with Friends:**

```bash
# 1. Install Fly.io CLI
brew install flyctl

# 2. Sign up
flyctl auth signup

# 3. Deploy
flyctl deploy

# 4. Share URL with friends
flyctl info
```

**Your friends visit:** `https://your-app-name.fly.dev`

That's it! 🎮

---

*Last updated: 2025-11-30*
