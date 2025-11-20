# Dou Dizhu (DDZ) Frontend Developer Guide

Complete API documentation for building frontend clients for the DDZ card game backend.

## Table of Contents

1. [Getting Started](#getting-started)
2. [Architecture Overview](#architecture-overview)
3. [REST API Endpoints](#rest-api-endpoints)
4. [WebSocket Protocol](#websocket-protocol)
5. [Data Structures (DTOs)](#data-structures-dtos)
6. [Game Flow & Phases](#game-flow--phases)
7. [Common Pitfalls](#common-pitfalls)
8. [Complete Example Code](#complete-example-code)
9. [Testing Your Frontend](#testing-your-frontend)

---

## Getting Started

### Running the Backend Locally

```bash
# From the DDZ repository root
./start-servers.sh
```

The backend will start on `http://localhost:8080`

**Health Check:**
```bash
curl http://localhost:8080/actuator/health
```

Expected response:
```json
{"status":"UP"}
```

### Technology Stack

- **Backend:** Spring Boot 3.3.3, Java 21
- **Communication:** REST API (HTTP) + WebSocket (real-time updates)
- **Data Format:** JSON
- **CORS:** Currently allows all origins (no authentication yet)

---

## Architecture Overview

### Communication Pattern

The DDZ backend uses a **hybrid REST + WebSocket architecture**:

1. **REST API** - Used for game lifecycle operations:
   - Creating games
   - Joining games
   - Starting games
   - Fetching game state snapshots

2. **WebSocket** - Used for real-time gameplay:
   - Submitting player actions (bid, play cards, pass)
   - Receiving live game state updates
   - Broadcasting changes to all players

### Typical Flow

```
1. Player creates game via REST API → receives gameId and joinCode
2. Other players join via REST API with joinCode → receive gameId
3. Each player opens WebSocket connection to /ws/game/{gameId}?playerId={uuid}
4. Host starts game via REST API → all players receive state update via WebSocket
5. Players submit actions via WebSocket → all players receive updated state
6. Game continues until TERMINATED phase
```

---

## REST API Endpoints

Base URL: `http://localhost:8080/api/games`

### 1. Create Game

**Endpoint:** `POST /api/games`

**Request Body:**
```json
{
  "playerCount": 3,
  "creatorName": "Alice"
}
```

**Validation:**
- `playerCount` must be between 3 and 12
- `creatorName` must not be blank

**Response (200 OK):**
```json
{
  "gameId": "550e8400-e29b-41d4-a716-446655440000",
  "joinCode": "ABCD",
  "players": [
    {
      "id": "7c9e6679-7425-40de-944b-e07fc1f90ae7",
      "name": "Alice",
      "cardCount": 0,
      "isLandlord": false,
      "isConnected": false,
      "score": 0
    }
  ],
  "playerCount": 1,
  "phase": "LOBBY",
  "currentPlayer": null,
  "yourPlayerId": "7c9e6679-7425-40de-944b-e07fc1f90ae7"
}
```

**Important:** Save both `gameId` and `yourPlayerId` - you'll need them for all subsequent operations.

---

### 2. Join Game

**Endpoint:** `POST /api/games/{gameId}/join`

**Request Body:**
```json
{
  "playerName": "Bob"
}
```

**Validation:**
- `playerName` must not be blank
- Game must not be full
- Game must exist

**Response (200 OK):**
```json
{
  "gameId": "550e8400-e29b-41d4-a716-446655440000",
  "joinCode": "ABCD",
  "players": [
    {
      "id": "7c9e6679-7425-40de-944b-e07fc1f90ae7",
      "name": "Alice",
      "cardCount": 0,
      "isLandlord": false,
      "isConnected": false,
      "score": 0
    },
    {
      "id": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
      "name": "Bob",
      "cardCount": 0,
      "isLandlord": false,
      "isConnected": false,
      "score": 0
    }
  ],
  "playerCount": 2,
  "phase": "LOBBY",
  "currentPlayer": null,
  "yourPlayerId": "3fa85f64-5717-4562-b3fc-2c963f66afa6"
}
```

**Error Responses:**
- `404 Not Found` - Game does not exist
- `400 Bad Request` - Game is full or invalid player name

---

### 3. Get Game by Join Code

**Endpoint:** `GET /api/games/by-code/{joinCode}`

**Example:** `GET /api/games/by-code/ABCD`

**Response (200 OK):**
```json
{
  "gameId": "550e8400-e29b-41d4-a716-446655440000",
  "joinCode": "ABCD",
  "players": [...],
  "playerCount": 2,
  "phase": "LOBBY",
  "currentPlayer": null,
  "yourPlayerId": null
}
```

**Note:** `yourPlayerId` will be null since this is just a lookup. Use this to get the `gameId` before joining.

**Use Case:**
```javascript
// User enters join code "ABCD"
const response = await fetch(`${baseUrl}/api/games/by-code/ABCD`);
const gameInfo = await response.json();

// Now join with the gameId
const joinResponse = await fetch(`${baseUrl}/api/games/${gameInfo.gameId}/join`, {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({ playerName: 'Bob' })
});
```

---

### 4. Start Game

**Endpoint:** `POST /api/games/{gameId}/start`

**No request body required.**

**Response (200 OK):**
```json
{
  "gameId": "550e8400-e29b-41d4-a716-446655440000",
  "joinCode": "ABCD",
  "players": [...],
  "playerCount": 3,
  "phase": "BIDDING",
  "currentPlayer": "7c9e6679-7425-40de-944b-e07fc1f90ae7",
  "yourPlayerId": null
}
```

**Requirements:**
- Game must have exactly `playerCount` players (the number specified when creating)
- Game must be in `LOBBY` phase

**Error Responses:**
- `404 Not Found` - Game does not exist
- `400 Bad Request` - Not enough players or game already started

---

### 5. Get Game State

**Endpoint:** `GET /api/games/{gameId}/state?playerId={playerId}`

**Example:** `GET /api/games/550e8400-e29b-41d4-a716-446655440000/state?playerId=7c9e6679-7425-40de-944b-e07fc1f90ae7`

**Response (200 OK):**
```json
{
  "gameId": "550e8400-e29b-41d4-a716-446655440000",
  "phase": "PLAY",
  "currentPlayer": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "myHand": [
    {"suit": "SPADES", "rank": "SEVEN"},
    {"suit": "HEARTS", "rank": "NINE"},
    {"suit": "DIAMONDS", "rank": "ACE"}
  ],
  "players": [
    {
      "id": "7c9e6679-7425-40de-944b-e07fc1f90ae7",
      "name": "Alice",
      "cardCount": 15,
      "isLandlord": true,
      "isConnected": true,
      "score": 0
    },
    {
      "id": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
      "name": "Bob",
      "cardCount": 17,
      "isLandlord": false,
      "isConnected": true,
      "score": 0
    }
  ],
  "currentLead": {
    "comboType": "SINGLE",
    "cards": [{"suit": "CLUBS", "rank": "KING"}]
  },
  "scores": {
    "7c9e6679-7425-40de-944b-e07fc1f90ae7": 0,
    "3fa85f64-5717-4562-b3fc-2c963f66afa6": 0
  },
  "bombsPlayed": 0,
  "rocketsPlayed": 0,
  "currentBet": 3,
  "multiplier": 3
}
```

**Important:**
- The `myHand` field contains only the requesting player's cards
- Other players' cards are hidden (you only see their `cardCount`)
- Scores are keyed by player ID (string format)

**Error Responses:**
- `404 Not Found` - Game does not exist
- `400 Bad Request` - Invalid playerId or player not in game

---

## WebSocket Protocol

### Connection

**Endpoint:** `ws://localhost:8080/ws/game/{gameId}?playerId={playerId}`

**Example:**
```javascript
const ws = new WebSocket(
  `ws://localhost:8080/ws/game/550e8400-e29b-41d4-a716-446655440000?playerId=7c9e6679-7425-40de-944b-e07fc1f90ae7`
);
```

**On Connection:**
The server immediately sends the current game state to the newly connected client:

```json
{
  "type": "GAME_UPDATE",
  "state": { /* GameStateResponse object */ },
  "message": "Connected to game 550e8400-e29b-41d4-a716-446655440000"
}
```

---

### Message Types

#### 1. GAME_UPDATE (Server → Client)

Sent whenever game state changes. All connected players receive this.

**Structure:**
```json
{
  "type": "GAME_UPDATE",
  "state": {
    "gameId": "550e8400-e29b-41d4-a716-446655440000",
    "phase": "PLAY",
    "currentPlayer": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
    "myHand": [...],
    "players": [...],
    "currentLead": {...},
    "scores": {...},
    "bombsPlayed": 0,
    "rocketsPlayed": 0,
    "currentBet": 3,
    "multiplier": 3
  },
  "message": "Action processed"
}
```

**Note:** The `state` field is a complete `GameStateResponse` object (same as REST API).

---

#### 2. ERROR (Server → Client)

Sent when an action fails validation.

**Structure:**
```json
{
  "type": "ERROR",
  "error": "Cannot pass - you are leading the trick"
}
```

---

### Sending Actions (Client → Server)

All player actions use polymorphic JSON with a `type` field.

#### Action 1: BID

Used during `BIDDING` phase to place a bid.

**Structure:**
```json
{
  "type": "BID",
  "playerId": "7c9e6679-7425-40de-944b-e07fc1f90ae7",
  "bidValue": 3
}
```

**Fields:**
- `bidValue`: Integer from 0 to 3 (0 = pass, 3 = maximum bid)

**JavaScript Example:**
```javascript
ws.send(JSON.stringify({
  type: 'BID',
  playerId: myPlayerId,
  bidValue: 3
}));
```

---

#### Action 2: PLAY

Used during `PLAY` phase to play cards.

**Structure:**
```json
{
  "type": "PLAY",
  "playerId": "7c9e6679-7425-40de-944b-e07fc1f90ae7",
  "cards": [
    {"suit": "SPADES", "rank": "SEVEN"},
    {"suit": "HEARTS", "rank": "SEVEN"},
    {"suit": "CLUBS", "rank": "SEVEN"}
  ]
}
```

**Fields:**
- `cards`: Array of `CardDto` objects representing the cards to play

**JavaScript Example:**
```javascript
// Playing three 7s (a TRIPLE)
ws.send(JSON.stringify({
  type: 'PLAY',
  playerId: myPlayerId,
  cards: [
    { suit: 'SPADES', rank: 'SEVEN' },
    { suit: 'HEARTS', rank: 'SEVEN' },
    { suit: 'CLUBS', rank: 'SEVEN' }
  ]
}));
```

---

#### Action 3: PASS

Used during `PLAY` phase to pass your turn.

**Structure:**
```json
{
  "type": "PASS",
  "playerId": "7c9e6679-7425-40de-944b-e07fc1f90ae7"
}
```

**JavaScript Example:**
```javascript
ws.send(JSON.stringify({
  type: 'PASS',
  playerId: myPlayerId
}));
```

**Important:** You cannot pass if you are leading the trick (no `currentLead`). The server will return an ERROR message.

---

#### Action 4: SELECT_LANDLORD

Used during `BIDDING` phase when multiple players bid the same amount and a tie-breaker is needed.

**Structure:**
```json
{
  "type": "SELECT_LANDLORD",
  "playerId": "7c9e6679-7425-40de-944b-e07fc1f90ae7",
  "selectedPlayerId": "3fa85f64-5717-4562-b3fc-2c963f66afa6"
}
```

**Fields:**
- `selectedPlayerId`: UUID of the player to become landlord (string format)

**When to use:** Check if `gameState.currentPlayer` equals your `playerId` and the phase is `BIDDING` but no one can bid anymore. This is rare but handled by the server.

---

## Data Structures (DTOs)

### GameStateResponse

The primary game state object sent to clients.

```typescript
interface GameStateResponse {
  gameId: string;                    // UUID of the game
  phase: Phase;                      // Current game phase
  currentPlayer: string | null;      // UUID of player whose turn it is
  myHand: CardDto[];                 // Your cards (private to you)
  players: PlayerInfo[];             // All players in the game
  currentLead: PlayedHandDto | null; // Current leading hand (null if you're leading)
  scores: Record<string, number>;    // Final scores (populated in TERMINATED phase)
  bombsPlayed: number;               // Number of bombs played
  rocketsPlayed: number;             // Number of rockets played
  currentBet: number;                // Winning bid amount
  multiplier: number;                // Score multiplier (currentBet * 2^(bombs + rockets))
}
```

**Key Notes:**
- `phase` values: `"LOBBY"`, `"BIDDING"`, `"PLAY"`, `"TERMINATED"`
- `currentLead` is `null` when no one has played yet (leader's turn)
- `scores` map is only populated when `phase === "TERMINATED"`
- `multiplier` formula: `currentBet × 2^(bombsPlayed + rocketsPlayed)`

---

### PlayerInfo

Information about a player in the game.

```typescript
interface PlayerInfo {
  id: string;              // UUID of the player
  name: string;            // Display name
  cardCount: number;       // Number of cards in hand
  isLandlord: boolean;     // True if this player is the landlord
  isConnected: boolean;    // True if player's WebSocket is connected
  score: number;           // Player's score for this game (updated at end)
}
```

**CRITICAL WARNING:**
- The field is named `id`, NOT `playerId`
- The field is named `cardCount`, NOT `handSize`
- Using the wrong field names will cause your app to break silently!

**Example Bug:**
```javascript
// WRONG - will silently fail
playerNames[player.playerId] = player.name; // player.playerId is undefined!

// CORRECT
playerNames[player.id] = player.name;
```

---

### CardDto

Represents a playing card.

```typescript
interface CardDto {
  suit: Suit;   // Card suit
  rank: Rank;   // Card rank
}

type Suit =
  | "SPADES"
  | "HEARTS"
  | "CLUBS"
  | "DIAMONDS"
  | "NONE";     // Used for jokers

type Rank =
  | "THREE" | "FOUR" | "FIVE" | "SIX" | "SEVEN" | "EIGHT" | "NINE" | "TEN"
  | "JACK" | "QUEEN" | "KING" | "ACE"
  | "TWO"           // Second highest rank (special)
  | "LITTLE_JOKER"  // Highest single card (suit = NONE)
  | "BIG_JOKER";    // Highest single card (suit = NONE)
```

**Rank Order (lowest to highest):**
```
THREE < FOUR < FIVE < SIX < SEVEN < EIGHT < NINE < TEN < JACK < QUEEN < KING < ACE < TWO < LITTLE_JOKER < BIG_JOKER
```

**Special Rules:**
- Jokers have `suit: "NONE"`
- `TWO` is special: it can appear at the end of a straight after ACE (e.g., `J-Q-K-A-2`)
- Straights with TWO must be 5+ cards total

---

### PlayedHandDto

Represents a combination of cards that was played.

```typescript
interface PlayedHandDto {
  comboType: ComboType;  // Type of combination
  cards: CardDto[];      // Cards in the combination
}

type ComboType =
  | "SINGLE"                  // One card
  | "PAIR"                    // Two cards of same rank (e.g., 7-7)
  | "TRIPLE"                  // Three cards of same rank (e.g., 7-7-7)
  | "TRIPLE_WITH_SINGLE"      // Triple + any single (e.g., 7-7-7-3)
  | "TRIPLE_WITH_PAIR"        // Triple + any pair (e.g., 7-7-7-3-3)
  | "SEQUENCE"                // 5+ consecutive cards (e.g., 3-4-5-6-7)
  | "PAIR_SEQUENCE"           // 3+ consecutive pairs (e.g., 3-3-4-4-5-5)
  | "AIRPLANE"                // 2+ consecutive triples (e.g., 7-7-7-8-8-8)
  | "AIRPLANE_WITH_SINGLES"   // Airplane + equal singles (e.g., 7-7-7-8-8-8-3-4)
  | "AIRPLANE_WITH_PAIRS"     // Airplane + equal pairs (e.g., 7-7-7-8-8-8-3-3-4-4)
  | "BOMB"                    // Four of a kind (e.g., 7-7-7-7)
  | "ROCKET";                 // Little Joker + Big Joker
```

**Combo Rules:**
- `SEQUENCE` must be 5+ cards
- `PAIR_SEQUENCE` must be 3+ pairs
- `AIRPLANE` must be 2+ consecutive triples
- `BOMB` beats everything except higher bombs and ROCKET
- `ROCKET` beats everything

**Special Straight Rule:**
- Valid: `J-Q-K-A-2` (5 cards, 2 after Ace)
- Invalid: `K-A-2` (only 3 cards, need 5+)
- Invalid: `2-3-4-5-6` (2 cannot start a sequence)

---

### GameInfo

Response from creating/joining a game.

```typescript
interface GameInfo {
  gameId: string;              // UUID of the game
  joinCode: string;            // 4-letter code for joining (e.g., "ABCD")
  players: PlayerInfo[];       // All players currently in the game
  playerCount: number;         // Current number of players
  phase: Phase;                // Current phase (usually "LOBBY")
  currentPlayer: string | null;// UUID of current player
  yourPlayerId: string | null; // Your player ID (null for lookups)
}
```

---

## Game Flow & Phases

### Phase Progression

```
LOBBY → BIDDING → PLAY → TERMINATED
```

### 1. LOBBY Phase

**What happens:**
- Players join the game
- Game cannot start until all player slots are filled
- No actions required from players

**Frontend actions:**
- Display join code prominently
- Show list of joined players
- Enable "Start Game" button when `playerCount === maxPlayers`
- Call `POST /api/games/{gameId}/start` to begin

---

### 2. BIDDING Phase

**What happens:**
- Players take turns bidding (0-3)
- Highest bidder becomes landlord
- Landlord receives 3 extra cards
- In 5-player games, 2 landlords are selected (co-landlord variant)

**Frontend actions:**
- Display bidding UI when `gameState.currentPlayer === yourPlayerId`
- Show bid options: 0 (pass), 1, 2, 3
- Send BID action via WebSocket
- Disable bidding when it's not your turn
- Display current highest bid

**Example:**
```javascript
if (gameState.phase === 'BIDDING' && gameState.currentPlayer === myPlayerId) {
  // Show bidding buttons
  showBiddingUI();
}

function placeBid(bidValue) {
  ws.send(JSON.stringify({
    type: 'BID',
    playerId: myPlayerId,
    bidValue: bidValue
  }));
}
```

---

### 3. PLAY Phase

**What happens:**
- Players take turns playing cards
- Must beat the current leading hand (or pass)
- First to empty their hand wins
- Landlord wins alone; peasants win as a team

**Frontend actions:**
- Enable card selection when `gameState.currentPlayer === yourPlayerId`
- Display `gameState.currentLead` to show what you must beat
- Allow passing unless `currentLead === null` (you're leading)
- Send PLAY or PASS actions via WebSocket
- Update UI when cards are played

**Example:**
```javascript
if (gameState.phase === 'PLAY' && gameState.currentPlayer === myPlayerId) {
  enableCardSelection();

  if (gameState.currentLead === null) {
    // You're leading - you MUST play cards
    disablePassButton();
  } else {
    // You can pass or beat the current lead
    enablePassButton();
  }
}

function playCards(selectedCards) {
  ws.send(JSON.stringify({
    type: 'PLAY',
    playerId: myPlayerId,
    cards: selectedCards
  }));
}

function pass() {
  ws.send(JSON.stringify({
    type: 'PASS',
    playerId: myPlayerId
  }));
}
```

---

### 4. TERMINATED Phase

**What happens:**
- Game has ended
- `gameState.scores` map is populated with final scores
- Scores are zero-sum: landlord(s) gain what peasants lose
- Multiplier affects score magnitude

**Frontend actions:**
- Display game over screen
- Show winners and losers
- Display scoreboard with `gameState.scores`
- Offer "Play Again" button (creates new game)

**Example:**
```javascript
if (gameState.phase === 'TERMINATED') {
  const myScore = gameState.scores[myPlayerId];

  if (myScore > 0) {
    showWinScreen(myScore);
  } else {
    showLoseScreen(myScore);
  }

  displayScoreboard(gameState.scores, gameState.players);
}
```

**Scoreboard Example:**
```javascript
function displayScoreboard(scores, players) {
  players.forEach(player => {
    const score = scores[player.id]; // Note: use player.id, not player.playerId!
    console.log(`${player.name}: ${score > 0 ? '+' : ''}${score}`);
  });
}
```

---

## Common Pitfalls

### 1. Field Name Mismatches

**Problem:** Backend DTOs use different field names than you might expect.

**Critical fields:**
```javascript
// WRONG
player.playerId  // ❌ undefined
player.handSize  // ❌ undefined

// CORRECT
player.id        // ✅ "7c9e6679-7425-40de-944b-e07fc1f90ae7"
player.cardCount // ✅ 15
```

**Why this matters:** These bugs fail silently. Your code won't crash, but features won't work.

**Example bug from production:**
```javascript
// This creates a mapping like: { undefined: "Alice" }
playerNames[player.playerId] = player.name;

// Later, looking up a player's name returns undefined
console.log(playerNames[actualPlayerId]); // undefined - shows as "Unknown"
```

**Fix:**
```javascript
// Always use player.id
playerNames[player.id] = player.name;
```

---

### 2. Phase Name: TERMINATED vs GAME_OVER

**Problem:** Frontend code often assumes the phase is called `GAME_OVER`, but it's actually `TERMINATED`.

**Wrong:**
```javascript
if (gameState.phase === 'GAME_OVER') {  // ❌ Never true!
  showGameOverScreen();
}
```

**Correct:**
```javascript
if (gameState.phase === 'TERMINATED') {  // ✅ Correct
  showGameOverScreen();
}
```

---

### 3. WebSocket Timing Issue on Join

**Problem:** When a player joins via REST API, they might connect their WebSocket after the join broadcast.

**What happens:**
1. Player calls `POST /api/games/{gameId}/join`
2. Server broadcasts game state to existing WebSocket connections
3. New player opens WebSocket connection
4. New player misses the broadcast and sees stale state

**Solution:** The server now sends full game state immediately upon WebSocket connection, so this is handled automatically. Just make sure you:

```javascript
// Open WebSocket immediately after joining
const joinResponse = await fetch(`${baseUrl}/api/games/${gameId}/join`, {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({ playerName: name })
});

const data = await joinResponse.json();
myPlayerId = data.yourPlayerId;

// Connect WebSocket ASAP
ws = new WebSocket(`${wsUrl}/ws/game/${gameId}?playerId=${myPlayerId}`);

ws.onmessage = (event) => {
  const message = JSON.parse(event.data);
  if (message.type === 'GAME_UPDATE') {
    gameState = message.state; // First message contains full state
    updateUI();
  }
};
```

---

### 4. Passing When Leading

**Problem:** Players try to pass when they're leading the trick.

**Rule:** If `gameState.currentLead === null`, you MUST play cards (you cannot pass).

**Correct Implementation:**
```javascript
function canPass(gameState, myPlayerId) {
  return gameState.phase === 'PLAY'
      && gameState.currentPlayer === myPlayerId
      && gameState.currentLead !== null;  // Can only pass if someone else is leading
}

// In your UI
if (canPass(gameState, myPlayerId)) {
  enablePassButton();
} else {
  disablePassButton();
}
```

---

### 5. Not Handling ERROR Messages

**Problem:** Server sends ERROR messages when actions fail, but frontend doesn't display them.

**Solution:**
```javascript
ws.onmessage = (event) => {
  const message = JSON.parse(event.data);

  if (message.type === 'GAME_UPDATE') {
    gameState = message.state;
    updateUI();
  } else if (message.type === 'ERROR') {
    // Display error to user
    showErrorToast(message.error);
  }
};

function showErrorToast(errorMessage) {
  // Show a temporary notification
  console.error('Game error:', errorMessage);
  alert(errorMessage); // Replace with better UI
}
```

---

### 6. Not Updating Scores in TERMINATED Phase

**Problem:** Forgetting to update cumulative scores when the game ends.

**Solution:**
```javascript
let cumulativeScores = {}; // Persistent across games in same lobby

ws.onmessage = (event) => {
  const message = JSON.parse(event.data);

  if (message.type === 'GAME_UPDATE') {
    gameState = message.state;

    if (gameState.phase === 'TERMINATED') {
      // Update cumulative scores
      Object.keys(gameState.scores).forEach(playerId => {
        const gameScore = gameState.scores[playerId];
        cumulativeScores[playerId] = (cumulativeScores[playerId] || 0) + gameScore;
      });

      showGameOverScreen(gameState.scores, cumulativeScores);
    }

    updateUI();
  }
};
```

---

## Complete Example Code

### Full Game Client (JavaScript)

```javascript
class DDZClient {
  constructor(serverUrl, wsUrl) {
    this.serverUrl = serverUrl; // e.g., "http://localhost:8080"
    this.wsUrl = wsUrl;         // e.g., "ws://localhost:8080"
    this.gameId = null;
    this.myPlayerId = null;
    this.gameState = null;
    this.ws = null;
    this.playerNames = {};
    this.cumulativeScores = {};
  }

  // Create a new game
  async createGame(playerCount, creatorName) {
    const response = await fetch(`${this.serverUrl}/api/games`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ playerCount, creatorName })
    });

    if (!response.ok) {
      throw new Error(`Failed to create game: ${response.statusText}`);
    }

    const data = await response.json();
    this.gameId = data.gameId;
    this.myPlayerId = data.yourPlayerId;

    // Initialize player names and scores
    data.players.forEach(player => {
      this.playerNames[player.id] = player.name;
      this.cumulativeScores[player.id] = 0;
    });

    await this.connectWebSocket();
    return data;
  }

  // Join an existing game by join code
  async joinGameByCode(joinCode, playerName) {
    // First, get the game ID from the join code
    const lookupResponse = await fetch(`${this.serverUrl}/api/games/by-code/${joinCode}`);

    if (!lookupResponse.ok) {
      throw new Error(`Game not found: ${joinCode}`);
    }

    const gameInfo = await lookupResponse.json();
    this.gameId = gameInfo.gameId;

    // Now join the game
    const joinResponse = await fetch(`${this.serverUrl}/api/games/${this.gameId}/join`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ playerName })
    });

    if (!joinResponse.ok) {
      throw new Error(`Failed to join game: ${joinResponse.statusText}`);
    }

    const data = await joinResponse.json();
    this.myPlayerId = data.yourPlayerId;

    // Initialize player names and scores
    data.players.forEach(player => {
      this.playerNames[player.id] = player.name;
      this.cumulativeScores[player.id] = 0;
    });

    await this.connectWebSocket();
    return data;
  }

  // Start the game (host only)
  async startGame() {
    const response = await fetch(`${this.serverUrl}/api/games/${this.gameId}/start`, {
      method: 'POST'
    });

    if (!response.ok) {
      throw new Error(`Failed to start game: ${response.statusText}`);
    }

    return await response.json();
  }

  // Connect to WebSocket
  async connectWebSocket() {
    return new Promise((resolve, reject) => {
      this.ws = new WebSocket(`${this.wsUrl}/ws/game/${this.gameId}?playerId=${this.myPlayerId}`);

      this.ws.onopen = () => {
        console.log('WebSocket connected');
        resolve();
      };

      this.ws.onmessage = (event) => {
        const message = JSON.parse(event.data);
        this.handleMessage(message);
      };

      this.ws.onerror = (error) => {
        console.error('WebSocket error:', error);
        reject(error);
      };

      this.ws.onclose = () => {
        console.log('WebSocket disconnected');
      };
    });
  }

  // Handle WebSocket messages
  handleMessage(message) {
    if (message.type === 'GAME_UPDATE') {
      this.gameState = message.state;

      // Update player names
      this.gameState.players.forEach(player => {
        this.playerNames[player.id] = player.name; // Note: player.id, not player.playerId!
        if (!(player.id in this.cumulativeScores)) {
          this.cumulativeScores[player.id] = 0;
        }
      });

      // Update cumulative scores if game ended
      if (this.gameState.phase === 'TERMINATED') {
        Object.keys(this.gameState.scores).forEach(playerId => {
          this.cumulativeScores[playerId] += this.gameState.scores[playerId];
        });
      }

      this.onGameUpdate(this.gameState);

    } else if (message.type === 'ERROR') {
      this.onError(message.error);
    }
  }

  // Send a bid
  bid(bidValue) {
    this.ws.send(JSON.stringify({
      type: 'BID',
      playerId: this.myPlayerId,
      bidValue: bidValue
    }));
  }

  // Play cards
  playCards(cards) {
    this.ws.send(JSON.stringify({
      type: 'PLAY',
      playerId: this.myPlayerId,
      cards: cards
    }));
  }

  // Pass
  pass() {
    this.ws.send(JSON.stringify({
      type: 'PASS',
      playerId: this.myPlayerId
    }));
  }

  // Check if it's my turn
  isMyTurn() {
    return this.gameState && this.gameState.currentPlayer === this.myPlayerId;
  }

  // Check if I can pass
  canPass() {
    return this.isMyTurn()
        && this.gameState.phase === 'PLAY'
        && this.gameState.currentLead !== null;
  }

  // Get my player info
  getMyPlayerInfo() {
    return this.gameState?.players.find(p => p.id === this.myPlayerId);
  }

  // Override these methods in your UI code
  onGameUpdate(gameState) {
    console.log('Game updated:', gameState);
  }

  onError(error) {
    console.error('Game error:', error);
  }

  // Disconnect
  disconnect() {
    if (this.ws) {
      this.ws.close();
    }
  }
}

// Usage example
const client = new DDZClient('http://localhost:8080', 'ws://localhost:8080');

// Override callbacks
client.onGameUpdate = (gameState) => {
  console.log(`Phase: ${gameState.phase}`);
  console.log(`Current player: ${gameState.currentPlayer}`);

  if (gameState.phase === 'BIDDING' && client.isMyTurn()) {
    // Show bidding UI
    console.log('Your turn to bid!');
  }

  if (gameState.phase === 'PLAY' && client.isMyTurn()) {
    // Show play UI
    console.log('Your turn to play!');
    console.log('Your hand:', gameState.myHand);
  }

  if (gameState.phase === 'TERMINATED') {
    // Show game over
    const myScore = gameState.scores[client.myPlayerId];
    console.log(`Game over! Your score: ${myScore}`);
  }
};

client.onError = (error) => {
  alert(`Error: ${error}`);
};

// Create a game
await client.createGame(3, 'Alice');
console.log(`Join code: ${client.gameId}`);

// Or join a game
// await client.joinGameByCode('ABCD', 'Bob');
```

---

## Testing Your Frontend

### 1. Local Backend Setup

```bash
cd /path/to/DDZ
./start-servers.sh
```

Verify backend is running:
```bash
curl http://localhost:8080/actuator/health
# Should return: {"status":"UP"}
```

---

### 2. Test with Multiple Clients

Open 3 browser tabs (or use different browsers) and test the full flow:

**Tab 1 (Host):**
```javascript
const client1 = new DDZClient('http://localhost:8080', 'ws://localhost:8080');
await client1.createGame(3, 'Alice');
console.log('Join code:', client1.gameId); // Copy this code
```

**Tab 2 (Player 2):**
```javascript
const client2 = new DDZClient('http://localhost:8080', 'ws://localhost:8080');
await client2.joinGameByCode('ABCD', 'Bob'); // Use code from Tab 1
```

**Tab 3 (Player 3):**
```javascript
const client3 = new DDZClient('http://localhost:8080', 'ws://localhost:8080');
await client3.joinGameByCode('ABCD', 'Charlie'); // Use code from Tab 1
```

**Back to Tab 1 (Start game):**
```javascript
await client1.startGame();
```

All three tabs should now receive game state updates and be in the BIDDING phase.

---

### 3. Test Bidding

In each tab:
```javascript
// When it's your turn
client.bid(3); // Bid 3 (or 0, 1, 2)
```

After all players bid, the game should transition to PLAY phase.

---

### 4. Test Playing Cards

In the tab where it's your turn:
```javascript
// Play a single card
client.playCards([
  { suit: 'SPADES', rank: 'SEVEN' }
]);

// Or pass (if not leading)
client.pass();
```

---

### 5. Test Game Completion

Play until one player runs out of cards. The game should:
1. Transition to TERMINATED phase
2. Populate `gameState.scores` with final scores
3. Display game over screen in your UI

---

### 6. Test Error Handling

Try invalid actions to see error messages:

```javascript
// Try to pass when leading (should fail)
client.pass(); // ERROR: "Cannot pass - you are leading the trick"

// Try to play invalid cards
client.playCards([
  { suit: 'SPADES', rank: 'SEVEN' },
  { suit: 'HEARTS', rank: 'EIGHT' } // Not a valid combo
]);
```

---

### 7. Browser Developer Tools

Use Chrome DevTools to inspect WebSocket traffic:

1. Open DevTools (F12)
2. Go to Network tab
3. Click on the WebSocket connection
4. View Messages tab to see all WebSocket traffic

This is invaluable for debugging state update issues.

---

## Additional Resources

### Card Ranks Reference

```javascript
const RANK_ORDER = [
  'THREE', 'FOUR', 'FIVE', 'SIX', 'SEVEN', 'EIGHT', 'NINE', 'TEN',
  'JACK', 'QUEEN', 'KING', 'ACE', 'TWO', 'LITTLE_JOKER', 'BIG_JOKER'
];

function compareRanks(rank1, rank2) {
  return RANK_ORDER.indexOf(rank1) - RANK_ORDER.indexOf(rank2);
}
```

### Combo Type Reference

```javascript
const COMBO_TYPES = {
  SINGLE: 'One card',
  PAIR: 'Two of same rank',
  TRIPLE: 'Three of same rank',
  TRIPLE_WITH_SINGLE: 'Three of same rank + any single',
  TRIPLE_WITH_PAIR: 'Three of same rank + any pair',
  SEQUENCE: '5+ consecutive cards',
  PAIR_SEQUENCE: '3+ consecutive pairs',
  AIRPLANE: '2+ consecutive triples',
  AIRPLANE_WITH_SINGLES: 'Airplane + equal singles',
  AIRPLANE_WITH_PAIRS: 'Airplane + equal pairs',
  BOMB: 'Four of same rank',
  ROCKET: 'Little Joker + Big Joker'
};
```

### Health Check Endpoint

Monitor backend status:
```javascript
async function checkBackendHealth() {
  const response = await fetch('http://localhost:8080/actuator/health');
  const health = await response.json();
  return health.status === 'UP';
}
```

---

## Summary Checklist

Before launching your frontend:

- [ ] Uses `player.id` instead of `player.playerId`
- [ ] Uses `player.cardCount` instead of `player.handSize`
- [ ] Checks for `phase === 'TERMINATED'` instead of `'GAME_OVER'`
- [ ] Opens WebSocket immediately after joining game
- [ ] Handles GAME_UPDATE messages to update UI
- [ ] Handles ERROR messages to display to user
- [ ] Disables "Pass" button when leading (`currentLead === null`)
- [ ] Updates cumulative scores in TERMINATED phase
- [ ] Maps player IDs to names for scoreboard display
- [ ] Tested with 3 simultaneous browser tabs

---

## Support

If you encounter issues:

1. Check backend health: `curl http://localhost:8080/actuator/health`
2. Inspect WebSocket messages in browser DevTools
3. Check backend logs for error messages
4. Verify you're using correct field names (`player.id`, `player.cardCount`)
5. Verify you're checking correct phase name (`TERMINATED`)

---

**Good luck building your frontend!** 🎮

This backend is production-ready and has been tested with a full 3-player game. All core mechanics work correctly, including the special straight rule allowing TWO after ACE.
