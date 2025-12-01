# Dou Dizhu (斗地主) - Fighting the Landlord

A multiplayer card game server and web client for the popular Chinese card game Dou Dizhu.

## Quick Start Guide

### Option 1: Start Both Servers (Recommended)

From the project root directory, run:

```bash
./start-servers.sh
```

This will:
- Start the frontend on port 3000
- Start the backend on port 8080
- Run both in the background
- Show you when everything is ready (~10-15 seconds)

**To stop servers:**
```bash
./stop-servers.sh
```

---

### Option 2: Start Servers Manually (Two Terminals)

**Terminal 1 - Frontend:**
```bash
cd /Users/advai/Documents/DDZ/web
python3 -m http.server 3000
```

**Terminal 2 - Backend:**
```bash
cd /Users/advai/Documents/DDZ
./gradlew :server:bootRun
```

Keep both terminals open. Press `Ctrl+C` in each to stop.

---

### Open the Web Client

Once servers are running, open your browser to:
```
http://localhost:3000
```

**Note:** Don't open localhost:8080 - that's the backend API only!

---

## Testing the Game (3 Players)

### Step 1: Create a Game (Browser Window 1)

1. Open http://localhost:3000
2. Enter your name: `Alice`
3. Select "3 Players"
4. Click **"Create Game"**
5. **Note the JOIN CODE** (displayed in gold box, e.g., "A3B7")

### Step 2: Join with Player 2 (Browser Window 2)

1. Open http://localhost:3000 in a **new browser window**
2. Enter your name: `Bob`
3. Enter the join code from Step 1
4. Click **"Join Game"**

### Step 3: Join with Player 3 (Browser Window 3)

1. Open http://localhost:3000 in another **new browser window**
2. Enter your name: `Charlie`
3. Enter the same join code
4. Click **"Join Game"**

### Step 4: Start and Play

1. In **Window 1 (Alice)**: Click **"Start Game"**
2. **Bidding Phase**: Each player bids 0-3 (or pass with 0)
3. **Play Phase**:
   - Click cards to select them (they move up)
   - Click **"Play Selected Cards"** or **"Pass"**
   - First player to empty their hand wins!

---

## Deployment to Production

Want to play with friends online? Deploy the game to the cloud!

### Quick Deploy (5 minutes)

```bash
# Test with Docker locally first
./test-docker.sh

# Deploy to Fly.io (free tier)
./deploy-flyio.sh
```

### Full Deployment Guide

See **[DEPLOYMENT.md](DEPLOYMENT.md)** for comprehensive deployment instructions including:
- ✅ Docker setup and local testing
- ✅ Fly.io deployment (recommended - free tier)
- ✅ Alternative platforms (Railway, Render, VPS)
- ✅ Custom domain setup
- ✅ Troubleshooting guide

### Deployment Options

| Platform | Cost | Setup Time | Best For |
|----------|------|------------|----------|
| **Fly.io** | Free | 5 min | Playing with friends (recommended) |
| Railway | Free | 5 min | Auto-deploy from GitHub |
| Render | Free | 10 min | Simple free hosting |
| VPS | $5/mo | 30 min | Full control |

---

## Project Structure

```
DDZ/
├── engine/              # Game logic (rules, cards, game state)
│   └── src/
│       ├── main/java/   # Core game engine
│       └── test/java/   # 77 passing tests
│
├── server/              # Spring Boot server
│   └── src/
│       ├── main/java/   # REST API + WebSocket
│       └── test/java/   # 8 passing tests
│
├── web/                 # Frontend client
│   ├── index.html       # Single-page web app
│   ├── config.js        # Backend URL configuration
│   └── README.md        # Detailed frontend docs
│
├── Dockerfile           # Docker image definition
├── docker-compose.yml   # Local Docker setup
├── fly.toml             # Fly.io deployment config
├── .dockerignore        # Docker build exclusions
│
├── start-servers.sh     # Start dev servers
├── stop-servers.sh      # Stop dev servers
├── test-docker.sh       # Test Docker build locally
├── deploy-flyio.sh      # Deploy to Fly.io
│
├── DEPLOYMENT.md        # Full deployment guide
└── README.md            # This file
```

---

## API Endpoints

### REST API (port 8080)

- `POST /api/games` - Create new game
- `POST /api/games/{gameId}/join` - Join game
- `POST /api/games/{gameId}/start` - Start game
- `GET /api/games/by-code/{joinCode}` - Get game by join code
- `GET /api/games/{gameId}/state?playerId={uuid}` - Get game state

### WebSocket

- `ws://localhost:8080/ws/game/{gameId}?playerId={uuid}`

---

## Running Tests

### All Tests
```bash
./gradlew test
```

### Engine Tests Only (77 tests)
```bash
./gradlew :engine:test
```

### Server Tests Only (8 tests)
```bash
./gradlew :server:test
```

---

## Troubleshooting

### "Address already in use" (port 8080)

Another process is using port 8080. Find and kill it:

```bash
# Find the process
lsof -i :8080

# Kill it (replace PID with the number from above)
kill -9 PID
```

### Cards not showing in browser

1. Open browser DevTools (F12)
2. Check Console tab for errors
3. Make sure server is running (check http://localhost:8080)
4. Hard refresh the page (Cmd+Shift+R or Ctrl+Shift+R)

### WebSocket won't connect

1. Verify server is running
2. Check server URL in web client (should be `http://localhost:8080`)
3. Make sure you're using a modern browser (Chrome, Firefox, Safari, Edge)

---

## Tech Stack

- **Backend**: Java 21, Spring Boot 3.3.3, WebSocket
- **Build**: Gradle 9.1.0
- **Testing**: JUnit 5
- **Frontend**: Vanilla JavaScript, HTML5, CSS3

---

## Development Commands

### Build Project
```bash
./gradlew build
```

### Clean Build
```bash
./gradlew clean build
```

### Run with Different Port
```bash
./gradlew :server:bootRun --args='--server.port=9090'
```
