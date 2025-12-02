# DDZ Testing Guide

## Standalone Game Test (No Server Required)

### Running the Standalone Test

This tests the complete game engine without any server infrastructure.

```bash
# From project root
./gradlew :engine:run --args="com.yourco.ddz.engine.demo.StandaloneGameTest"
```

Or compile and run manually:
```bash
./gradlew :engine:build
java -cp engine/build/classes/java/main com.yourco.ddz.engine.demo.StandaloneGameTest
```

### What It Tests

✅ **Full Game Flow:**
1. Create game with 3-12 players
2. Deal cards
3. Bidding phase with interactive input
4. Multi-max-bid resolution (RPS simulation)
5. Interactive landlord selection (snake draft)
6. Play phase with card parsing
7. Win detection
8. Final scoring with bomb/rocket multipliers

### Example Session

```
=== Dou Dizhu Standalone Game Test ===

Enter number of players (3-12): 5
Enter name for Player 1: Alice
Enter name for Player 2: Bob
Enter name for Player 3: Carol
Enter name for Player 4: Dave
Enter name for Player 5: Eve

=== Game Configuration ===
GameConfig{players=5, decks=2, landlords=2, extraCards=3, maxBid=6, custom=false}

Starting game...

=== Hands Dealt ===
Alice: 20 cards: 3♠, 3♥, 4♦, 5♣, ...
Bob: 20 cards: 3♦, 4♠, 5♥, ...
...

=== BIDDING PHASE ===
Max bid: 6
Bid 0 to pass, 1-6 to bid

Current highest bid: 0
Alice, enter your bid (0-6): 3
Player Alice bid 3

Current highest bid: 3
Bob, enter your bid (0-6): 6
Player Bob bid max (6)!
...

=== ROCK PAPER SCISSORS ===
Max bidders: [Bob, Carol]
Random winner: Carol
===========================

=== LANDLORD TEAM SELECTION ===
Carol, select your teammate:
1. Alice
2. Dave
3. Eve
Enter choice (1-3): 2

Carol selected Dave as landlord!

=== PLAY PHASE ===
Landlords: Carol, Dave

--- Carol's Turn ---
Your hand: 23 cards: 3♠, 3♥, 4♦, ...
You lead this round!

Enter cards to play (e.g., 3H,3D for pair of 3s), or 'PASS' to pass:
> 3H,3D

...

=== GAME OVER ===
Winner: Carol
Team: LANDLORDS WIN!

Final Scores:
Carol (Landlord): +240
Dave (Landlord): +240
Alice (Farmer): -160
Bob (Farmer): -160
Eve (Farmer): -160

Bombs played: 2
Rockets played: 0
```

---

## Testing REST API (Server Mode)

### 1. Start the Server

```bash
./gradlew :server:bootRun
```

Server starts on `http://localhost:8080`

### 2. Create a Game

```bash
curl -X POST http://localhost:8080/api/games \
  -H "Content-Type: application/json" \
  -d '{"playerCount": 5, "creatorName": "Alice"}'
```

**Response:**
```json
{
  "gameId": "g-abc123...",
  "joinCode": "WXYZ",
  "players": [
    {
      "id": "uuid-alice",
      "name": "Alice",
      "cardCount": 0,
      "isLandlord": false,
      "isConnected": true,
      "score": 0
    }
  ],
  "maxPlayers": 5,
  "status": "LOBBY",
  "currentPlayer": null
}
```

### 3. Find Game by Join Code

```bash
curl http://localhost:8080/api/games/by-code/WXYZ
```

### 4. Join the Game

```bash
curl -X POST http://localhost:8080/api/games/g-abc123.../join \
  -H "Content-Type: application/json" \
  -d '{"playerName": "Bob"}'
```

**Response:**
```json
{
  "gameId": "g-abc123...",
  "joinCode": "WXYZ",
  "players": [
    {"id": "uuid-alice", "name": "Alice", ...},
    {"id": "uuid-bob", "name": "Bob", ...}
  ],
  "maxPlayers": 5,
  "status": "LOBBY",
  "currentPlayer": null
}
```

Repeat for Carol, Dave, Eve...

### 5. Start the Game

When all 5 players have joined:

```bash
curl -X POST http://localhost:8080/api/games/g-abc123.../start
```

**Response:**
```json
{
  "gameId": "g-abc123...",
  "status": "BIDDING",
  "players": [...],
  "currentPlayer": "uuid-alice"
}
```

### 6. Get Game State

```bash
curl "http://localhost:8080/api/games/g-abc123.../state?playerId=uuid-alice"
```

**Response:**
```json
{
  "gameId": "g-abc123...",
  "phase": "BIDDING",
  "currentPlayer": "uuid-alice",
  "myHand": [
    {"suit": "HEARTS", "rank": "THREE"},
    {"suit": "DIAMONDS", "rank": "FOUR"},
    ...
  ],
  "players": [...],
  "currentLead": null,
  "scores": {},
  "bombsPlayed": 0,
  "rocketsPlayed": 0
}
```

---

## Testing WebSocket (Coming Soon)

Once WebSocket is implemented, you can test with:

### Using `wscat` (WebSocket CLI tool)

```bash
# Install wscat
npm install -g wscat

# Connect
wscat -c "ws://localhost:8080/ws/game/g-abc123...?playerId=uuid-alice"

# Send bid
> {"type":"BID","playerId":"uuid-alice","bidValue":3}

# Receive state update
< {"type":"STATE_UPDATE","phase":"BIDDING",...}
```

### Using JavaScript in Browser

```javascript
const ws = new WebSocket('ws://localhost:8080/ws/game/g-abc123...?playerId=uuid-alice');

ws.onmessage = (event) => {
  const msg = JSON.parse(event.data);
  console.log('Received:', msg);
};

// Send bid
ws.send(JSON.stringify({
  type: 'BID',
  playerId: 'uuid-alice',
  bidValue: 3
}));

// Play cards
ws.send(JSON.stringify({
  type: 'PLAY',
  playerId: 'uuid-alice',
  cards: [
    {suit: 'HEARTS', rank: 'THREE'},
    {suit: 'DIAMONDS', rank: 'THREE'}
  ]
}));

// Pass
ws.send(JSON.stringify({
  type: 'PASS',
  playerId: 'uuid-alice'
}));
```

---

## Card Input Format

**Rank Codes:**
- Numbers: `2`, `3`, `4`, `5`, `6`, `7`, `8`, `9`
- `T` = 10
- `J` = Jack
- `Q` = Queen
- `K` = King
- `A` = Ace
- `LJ` = Little Joker
- `BJ` = Big Joker

**Suit Codes:**
- `S` = Spades ♠
- `H` = Hearts ♥
- `D` = Diamonds ♦
- `C` = Clubs ♣

**Examples:**
- Single: `3H` (3 of hearts)
- Pair: `3H,3D` (pair of 3s)
- Triple: `5H,5D,5S` (triple 5s)
- Sequence: `3H,4D,5S,6C,7H` (straight 3-7)
- Bomb: `KH,KD,KS,KC` (4 kings)
- Rocket: `LJ,BJ` (both jokers)

---

## Troubleshooting

### Standalone Test Issues

**Problem:** `Scanner` hangs waiting for input
**Solution:** Make sure to press Enter after each input

**Problem:** Invalid card parse error
**Solution:** Use exact format: `RankSuit` (e.g., `3H` not `3h` or `H3`)

### REST API Issues

**Problem:** Connection refused
**Solution:** Make sure server is running (`./gradlew :server:bootRun`)

**Problem:** 404 Not Found
**Solution:** Check the gameId matches the one from create response

**Problem:** 400 Bad Request "Game is full"
**Solution:** Too many players joined, create a new game

---

## Automated Testing

### E2E Tests (Playwright)

We use Playwright for end-to-end browser testing. Tests are located in `tests/e2e/`.

See [tests/README.md](tests/README.md) for full testing documentation.

**Quick Start**:
```bash
npm test
```

**Test Environments**:
- Staging: https://ddz-game-staging.fly.dev/ (default)
- Production: https://ddz-game.fly.dev/ (use `npm run test:prod`)
- Local: http://localhost:8080 (use `npm run test:local`)

**Available Commands**:
```bash
npm test              # Run all E2E tests against staging
npm run test:headed   # Run with visible browser
npm run test:debug    # Debug mode
npm run test:ui       # Interactive UI mode
npm run test:report   # View last test report
```

### Backend Integration Tests

Spring Boot integration tests are located in `server/src/test/java/`.

**Quick Start**:
```bash
./gradlew :server:test
```

**Run specific test class**:
```bash
./gradlew :server:test --tests GameRegistryTest
```

**Run all tests**:
```bash
./gradlew test
```

### CI/CD

All tests run automatically on every pull request via GitHub Actions:
- Backend tests (JUnit): ~2-3 minutes
- E2E tests (Playwright): ~4-5 minutes
- Total CI time: ~5-7 minutes (parallel execution)

**Test Coverage**:
- ✅ WebSocket connection and stability
- ✅ Multi-client synchronization
- ✅ Game creation and joining
- ✅ Bidding phase mechanics
- ✅ Backend REST API endpoints
- ✅ Game engine logic (85+ unit tests)

**View CI Results**:
Visit the "Actions" tab in GitHub to see test results for each PR.

---

## Next Steps

1. ✅ Run standalone test to verify engine works
2. ✅ Test REST API endpoints
3. ✅ WebSocket handlers implemented
4. ✅ Frontend UI built
5. ✅ Automated E2E testing with Playwright
6. ✅ CI/CD pipeline configured

---

## Debugging Tips

**View server logs:**
```bash
tail -f logs/spring.log  # If logging configured
```

**Enable debug logging:**
Add to `server/src/main/resources/application.properties`:
```properties
logging.level.com.yourco.ddz=DEBUG
```

**Check game state directly:**
The `GameRegistry` holds all active games in memory. You can add debug endpoints to inspect state.
