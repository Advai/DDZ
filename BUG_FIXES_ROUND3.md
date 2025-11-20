# Bug Fixes Round 3 - Join Button, Scoreboard, and Game Over Issues

## Session Summary

This session addressed critical bugs with the join game functionality, scoreboard display, and game over overlay not appearing.

---

## Bugs Fixed

### 1. ✅ Join Game Button - Player Not Seeing Game Screen

**Problem:** When a player joined via game code, they wouldn't see the game screen. The game would update for existing players, but the joining player's screen wouldn't transition to the game panel, creating a deadlock that required starting over.

**Root Cause:**
- When a player joined via HTTP POST `/join`, the backend would broadcast a WebSocket update to all connected clients
- However, the joining player's WebSocket wasn't connected yet!
- The join flow was:
  1. HTTP POST to `/join` → Backend broadcasts update
  2. Client receives HTTP response
  3. Client connects WebSocket
  4. WebSocket sends "Connected to game" with NO game state
- The joining player missed the initial broadcast and never received the full game state

**Fix:** Modified `GameWebSocketHandler.java` to send the full current game state when a WebSocket connection is established.

**Code Changes:**
```java
// File: server/src/main/java/com/yourco/ddz/server/ws/GameWebSocketHandler.java
// Lines 67-75

log.info("WebSocket connected - gameId: {}, playerId: {}", gameId, playerId);

// Send current game state to the newly connected client
if (playerId != null) {
  GameStateResponse stateResponse = GameStateResponse.from(game.loop().state(), playerId);
  sendMessage(session, new GameUpdateMessage(stateResponse, "Connected to game " + gameId));
} else {
  sendMessage(session, new GameUpdateMessage(null, "Connected to game " + gameId));
}
```

**Result:** ✅ Joining players now immediately see the game screen with all players listed

---

### 2. ✅ Scoreboard Replacing Players Instead of Adding Them

**Problem:**
- When Player A joined, they appeared on the scoreboard
- When Player B joined, Player B's name replaced Player A's name
- Only one player was ever visible on the scoreboard at a time
- Same issue happened with Player C

**Root Cause:** **Field name mismatch between backend DTO and frontend code!**

Backend `PlayerInfo` DTO uses these field names:
```java
public record PlayerInfo(
    String id,           // ✓ Not "playerId"
    String name,         // ✓
    int cardCount,       // ✓ Not "handSize"
    boolean isLandlord,
    boolean isConnected,
    int score
)
```

Frontend was incorrectly accessing:
```javascript
// WRONG - player.playerId was undefined!
playerNames[player.playerId] = player.name;
cumulativeScores[player.playerId] = 0;

// WRONG - player.handSize was undefined!
li.textContent = `${player.name} (${player.handSize} cards)`;
```

**What This Caused:**
All players were being mapped to the key `undefined` in the `playerNames` and `cumulativeScores` objects, so each new player would overwrite the previous one!

**Fix:** Changed all field accesses from `player.playerId` → `player.id` and `player.handSize` → `player.cardCount`

**Code Changes:**
```javascript
// File: web/index.html

// Scoreboard initialization (lines 633-645)
gameState.players.forEach(player => {
    // FIXED: Use player.id instead of player.playerId
    playerNames[player.id] = player.name;
    console.log(`📝 Mapped player name: ${player.id} -> ${player.name}`);

    if (!(player.id in cumulativeScores)) {
        cumulativeScores[player.id] = 0;
        console.log(`➕ Added player ${player.name} to scoreboard at 0`);
    } else {
        console.log(`✓ Player ${player.name} already in scoreboard with score ${cumulativeScores[player.id]}`);
    }
});

// Player list rendering (lines 654-671)
gameState.players.forEach(player => {
    const li = document.createElement('li');

    // FIXED: Use player.id and player.cardCount
    const isLandlord = gameState.landlordIds && gameState.landlordIds.includes(player.id);
    const isCurrentTurn = player.id === gameState.currentPlayer;

    if (isLandlord) li.classList.add('landlord');
    if (isCurrentTurn) li.classList.add('current-turn');

    li.textContent = `${player.name} (${player.cardCount} cards)`;
    if (isLandlord) li.textContent += ' 👑';
    if (isCurrentTurn) li.textContent += ' ←';
    if (player.id === myPlayerId) li.textContent += ' (You)';

    playersList.appendChild(li);
});

// Landlord selection (lines 968-974)
gameState.players.forEach(player => {
    if (player.id === myPlayerId) return; // Can't select yourself
    if (gameState.landlordIds && gameState.landlordIds.includes(player.id)) return;

    const btn = document.createElement('button');
    btn.textContent = player.name;
    btn.onclick = () => selectLandlord(player.id);
    container.appendChild(btn);
});
```

**Debugging Added:**
Added extensive console logging to track scoreboard updates:
```javascript
// Lines 1043-1060
console.log('🏆 Updating scoreboard display');
console.log('   - cumulativeScores entries:', Object.keys(cumulativeScores).length);
console.log('   - playerNames entries:', Object.keys(playerNames).length);

const sortedScores = Object.entries(cumulativeScores)
    .map(([playerId, score]) => {
        const name = playerNames[playerId] || 'Unknown';
        console.log(`   - ${name} (${playerId.substring(0, 8)}...): ${score}`);
        return { playerId, score, name };
    })
    .sort((a, b) => b.score - a.score);
```

**Result:** ✅ All players now appear on the scoreboard with their correct names

---

### 3. ✅ Player Name Display Removed from Game Info

**Problem:** User liked having their name displayed in the Game Info panel, but it was removed in previous bug fix (we removed the UUID display).

**Fix:** Added back "Your Name" field but showing the inputted name instead of UUID.

**Code Changes:**
```javascript
// File: web/index.html

// Added global variable (line 418)
let myPlayerName = null; // Store the player's name

// Added HTML display (lines 321-323)
<div class="info-box">
    <strong>Your Name:</strong> <span id="myPlayerNameDisplay">-</span>
</div>

// Set in createGame() (line 459)
myPlayerName = playerName;
document.getElementById('myPlayerNameDisplay').textContent = myPlayerName;

// Set in joinGame() (line 522)
myPlayerName = playerName;
document.getElementById('myPlayerNameDisplay').textContent = myPlayerName;

// Clear in leaveGame() (line 1213)
myPlayerName = null;
```

**Result:** ✅ Players now see their entered name in the Game Info panel

---

## Bugs Reported But Not Yet Fixed

### 4. ❌ Game Over Overlay Not Showing

**Problem:** After playing through a game, the winner/loser overlay does not appear on anyone's screen when the game terminates.

**Status:** Not yet fixed - needs investigation

**Possible Causes:**
- `gameState.phase === 'GAME_OVER'` condition not being met
- Backend not sending GAME_OVER phase correctly
- `gameOverShown` flag logic issue

**Investigation Needed:**
- Check if backend is setting phase to GAME_OVER
- Check if WebSocket is broadcasting the GAME_OVER state
- Check browser console for logs showing GAME_OVER detection
- Verify `gameOverShown` flag is being reset properly

---

### 5. ❌ Scoreboard Not Updating Scores After Game

**Problem:** After a game completes, the scoreboard doesn't add/deduct points from winners/losers. Points should accumulate (and can go negative).

**Status:** Not yet fixed - needs investigation

**Expected Behavior:**
- When game ends with `gameState.scores` (e.g., `{player1: +50, player2: -25, player3: -25}`)
- Cumulative scores should update: `cumulativeScores[playerId] += scoreChange`
- Scoreboard should display updated totals

**Current Code (lines 767-783):**
```javascript
if (gameState.phase === 'GAME_OVER' && !gameOverShown) {
    console.log('🎮 GAME_OVER detected! Showing overlay...');
    log('🎉 GAME OVER! Final scores calculated.', 'success');

    // Update cumulative scores
    if (gameState.scores) {
        Object.entries(gameState.scores).forEach(([playerId, scoreChange]) => {
            if (!cumulativeScores[playerId]) {
                cumulativeScores[playerId] = 0;
            }
            cumulativeScores[playerId] += scoreChange;
        });
        gamesPlayed++;
    }

    // Show game over overlay (only once per game)
    showGameOverOverlay();
    gameOverShown = true;

    // Show and update scoreboard
    updateScoreboard();
}
```

**Possible Issues:**
- GAME_OVER condition never being met (same as bug #4)
- `gameState.scores` not being populated by backend
- Score keys might be using wrong player ID format

---

### 6. ❌ Straights Cannot Include 2 After Ace

**Problem:** You should be allowed to make sequences with a 2 after an Ace (e.g., J-Q-K-A-2 or Q-K-A-2-3). Currently this is not allowed.

**Constraint:** Sequences cannot contain Little Joker or Big Joker.

**Status:** Not yet fixed - backend game logic needs update

**Files to Modify:**
- `engine/src/main/java/com/yourco/ddz/engine/core/SimplePlayDetector.java`
- `engine/src/main/java/com/yourco/ddz/engine/core/HandDetector.java`

**Current Rank Order:**
```
3, 4, 5, 6, 7, 8, 9, 10, J, Q, K, A, 2, LITTLE_JOKER, BIG_JOKER
```

**Issue:**
- Straight detection currently treats 2 as highest rank (after Ace)
- Need to allow wraparound: A-2-3 or K-A-2 as valid sequences
- But NOT 2-3-4 starting with 2 (2 is too powerful)

**Decision Needed:**
- Should 2 be allowed at the end of sequences? (e.g., K-A-2)
- Should sequences wrap around? (e.g., A-2-3-4-5)
- Or should 2 be excluded from straights entirely?

This requires clarification of Dou Dizhu rules and updating the engine logic accordingly.

---

## Files Modified

### Backend Files:
1. **`server/src/main/java/com/yourco/ddz/server/ws/GameWebSocketHandler.java`**
   - Lines 67-75: Send full game state on WebSocket connection
   - Fixed: Joining players now receive complete state immediately

### Frontend Files:
1. **`web/index.html`**
   - Line 418: Added `myPlayerName` variable
   - Lines 321-323: Added "Your Name" display in Game Info panel
   - Lines 459, 468: Set `myPlayerName` in `createGame()`
   - Lines 522, 531: Set `myPlayerName` in `joinGame()`
   - Line 1213: Clear `myPlayerName` in `leaveGame()`
   - Lines 633-645: Fixed `player.playerId` → `player.id` in scoreboard init
   - Lines 654-671: Fixed `player.playerId` → `player.id` and `player.handSize` → `player.cardCount` in player list
   - Lines 968-974: Fixed `player.playerId` → `player.id` in landlord selection
   - Lines 1043-1060: Added extensive debugging logs for scoreboard updates

---

## Testing Checklist

### ✅ Completed Tests:
- [x] Create game - Player A sees game screen
- [x] Join with code - Player B sees game screen immediately
- [x] Join with code - Player C sees game screen immediately
- [x] Scoreboard shows all 3 players at 0 points
- [x] Player name displays in Game Info panel
- [x] All players see each other in Players list with correct card counts

### ❌ Outstanding Tests:
- [ ] Play game to completion - verify GAME_OVER overlay appears
- [ ] Verify winner sees "🎉 VICTORY! 🎉" screen
- [ ] Verify losers see "💔 DEFEAT 💔" screen
- [ ] Scoreboard updates with score changes after game
- [ ] Play multiple games - verify scores accumulate correctly
- [ ] Verify scores can go negative (zero-sum game)
- [ ] Test straights with 2 after Ace (currently not allowed)

---

## Technical Details

### Backend DTO Structure
```java
// PlayerInfo.java
public record PlayerInfo(
    String id,              // Player UUID as string
    String name,            // Player's display name
    int cardCount,          // Number of cards in hand
    boolean isLandlord,     // Is this player a landlord?
    boolean isConnected,    // Is player currently connected?
    int score               // Current game score
)

// GameStateResponse.java
public record GameStateResponse(
    String gameId,
    String phase,                    // LOBBY, BIDDING, PLAY, GAME_OVER
    String currentPlayer,            // UUID of current player
    List<CardDto> myHand,
    List<PlayerInfo> players,
    PlayedHandDto currentLead,
    Map<String, Integer> scores,     // Final scores when GAME_OVER
    Integer bombsPlayed,
    Integer rocketsPlayed,
    Integer currentBet,
    Integer multiplier
)
```

### Frontend State Variables
```javascript
let ws = null;                      // WebSocket connection
let gameState = null;               // Current game state from backend
let myPlayerId = null;              // My player UUID
let myPlayerName = null;            // My display name
let currentGameId = null;           // Current game UUID
let joinCodeForGame = null;         // 4-letter join code
let selectedCards = new Set();      // Cards selected for playing
let cumulativeScores = {};          // Map: playerId -> total score across all games
let gamesPlayed = 0;                // Number of completed games in this lobby
let gameOverShown = false;          // Flag: has game over overlay been shown?
let playerNames = {};               // Map: playerId -> player name
```

---

## Next Steps

1. **Debug GAME_OVER detection:**
   - Check server logs for phase transitions
   - Check browser console for "🎮 GAME_OVER detected!" log
   - Verify backend is sending phase: "GAME_OVER" in WebSocket updates

2. **Fix scoreboard score updates:**
   - Verify `gameState.scores` contains correct player IDs
   - Check if score keys match the IDs in `cumulativeScores`
   - Ensure score update logic runs when GAME_OVER is detected

3. **Update straight detection logic:**
   - Research official Dou Dizhu rules for straights with 2
   - Update `SimplePlayDetector.java` or `HandDetector.java`
   - Add tests for A-2-3, K-A-2, and invalid sequences with Jokers
   - Ensure 2 cannot start a sequence (2-3-4 invalid)

---

## Previous Bug Fix Sessions

This is the third round of bug fixes. Previous sessions:
- **Round 1:** Fixed game over overlay showing on join, scoreboard initialization, hand type display
- **Round 2:** Fixed game over overlay tracking, player name mapping, removed UUID display

See `BUG_FIXES.md` and `BUG_FIXES_ROUND2.md` for details on previous fixes.
