# Dou Dizhu Web Client - MVP Testing Guide

This is a simple single-page web application for manual testing of the Dou Dizhu game backend.

## Quick Start

### 1. Start the Backend Server

```bash
cd /Users/advai/Documents/DDZ
./gradlew :server:bootRun
```

The server will start on `http://localhost:8080`

### 2. Open the Web Client

Open your browser and go to:
```
http://localhost:3000
```

Or open the file directly:
```
file:///Users/advai/Documents/DDZ/web/index.html
```

## How to Test the Game

### Creating a Game

1. Enter your name in "Your Name" field
2. Select number of players (3, 4, or 5)
3. Click "Create Game"
4. You'll see a **Join Code** (e.g., "A3B7") - share this with other players

### Joining a Game

1. Enter your name
2. Enter the 4-letter join code
3. Click "Join Game"

### Testing Solo (Multiple Browser Windows)

To test the full game flow yourself:

1. Open 3 different browser windows (or use incognito/private windows)
2. In Window 1: Create a 3-player game as "Alice"
3. Note the join code
4. In Window 2: Join the game as "Bob" with the join code
5. In Window 3: Join the game as "Charlie" with the join code
6. In Window 1: Click "Start Game"
7. Play through the game:
   - **Bidding Phase**: Each player bids (0-3)
   - **Landlord Selection** (if applicable): Choose co-landlord
   - **Play Phase**: Play cards or pass

## Game Flow

### Phase 1: Lobby
- Players join the game
- Once all players are in, click "Start Game"

### Phase 2: Bidding
- Each player bids (0 = pass, 1-3 = bid value)
- Highest bidder becomes landlord
- If multiple high bidders, one is chosen randomly

### Phase 3: Landlord Selection (4-5 players only)
- Primary landlord selects co-landlords
- Uses snake draft (primary picks, then other landlords pick in order)

### Phase 4: Play
- Landlord starts
- Play cards that beat the current lead, or pass
- Card combinations: singles, pairs, triples, bombs, straights, airplanes, etc.
- First player to empty their hand wins

## Features

- ✅ Create and join games
- ✅ Real-time WebSocket updates
- ✅ Visual card selection
- ✅ Bidding interface
- ✅ Landlord selection (multi-landlord games)
- ✅ Play cards or pass
- ✅ See current lead
- ✅ Player list with turn indicator
- ✅ Game log for debugging

## Troubleshooting

### "WebSocket not connected"
- Make sure the backend server is running on port 8080
- Check the server URL in the UI (should be `http://localhost:8080`)

### "Game not found"
- Double-check the join code
- Make sure the game hasn't been deleted

### Cards not showing
- Check browser console for errors (F12)
- Make sure WebSocket connection is established (green "Connected" status)

## Technical Details

### REST API Endpoints Used
- `POST /api/games` - Create game
- `POST /api/games/{gameId}/join` - Join game
- `POST /api/games/{gameId}/start` - Start game
- `GET /api/games/by-code/{joinCode}` - Get game by join code

### WebSocket Messages

**Client → Server:**
```json
{
  "type": "BID",
  "playerId": "uuid",
  "bidValue": 2
}

{
  "type": "PLAY",
  "playerId": "uuid",
  "cards": [{"rank": "3", "suit": "HEARTS"}]
}

{
  "type": "PASS",
  "playerId": "uuid"
}

{
  "type": "SELECT_LANDLORD",
  "playerId": "uuid",
  "selectedPlayerId": "uuid"
}
```

**Server → Client:**
```json
{
  "gameState": { /* full game state */ },
  "message": "Action processed"
}
```

## Known Limitations (MVP)

- No authentication
- No game history
- No scoring display
- No card sorting options
- No keyboard shortcuts
- No mobile optimization
- Single-page only (no routing)

This is intentionally simple for quick manual testing. Your friend will build the production frontend!
