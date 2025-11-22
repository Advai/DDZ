# Game Over, Scoring, and Play Again Features

## Summary of Changes

Based on user feedback after playing a full 3-player game, I implemented:

1. **Obvious Game Over Overlay** - Big, unmissable notification when game ends
2. **Bet & Multiplier Display** - Shows stakes and how bombs/rockets affect scoring
3. **Cumulative Scoreboard** - Tracks total scores across multiple games in the same lobby
4. **Play Again Functionality** - Start a new game without leaving the lobby

## Backend Changes

### Updated `GameStateResponse` DTO
**File:** `server/src/main/java/com/yourco/ddz/server/api/dto/GameStateResponse.java`

Added two new fields:
- `Integer currentBet` - The winning bid from the bidding phase
- `Integer multiplier` - Final multiplier including bombs/rockets (bet × 2^(bombs + rockets))

```java
public record GameStateResponse(
    String gameId,
    String phase,
    String currentPlayer,
    List<CardDto> myHand,
    List<PlayerInfo> players,
    PlayedHandDto currentLead,
    Map<String, Integer> scores,
    Integer bombsPlayed,
    Integer rocketsPlayed,
    Integer currentBet,      // NEW
    Integer multiplier) {    // NEW
```

**Calculation:**
```java
int currentBet = state.getHighestBid();
int multiplier = currentBet * (int) Math.pow(2, state.getBombsPlayed() + state.getRocketsPlayed());
```

## Frontend Changes

### 1. Bet & Multiplier Panel

**Location:** Between "Start Game" buttons and "Players" section

**Display:**
- Shows during PLAY and GAME_OVER phases
- Hidden during LOBBY and BIDDING phases

**Elements:**
- Current Bet (💵) - The winning bid value
- Bombs (💣) count
- Rockets (🚀) count
- Multiplier (✖️) - Final score multiplier

**Visual Style:**
- Scarlet background (`rgba(171, 32, 42, 0.3)`)
- Scarlet border (`#ab202a`)
- Yellow highlights for bet and multiplier values

### 2. Game Over Overlay

**Trigger:** When `gameState.phase === 'GAME_OVER'`

**Visual Design:**
- Full-screen overlay with semi-transparent black background
- Centered modal with gradient background (Charcoal to Black)
- Yellow border (`#f8cf2c`)
- Large "🎉 GAME OVER 🎉" heading

**Content:**
- Winner indicated with 👑 crown
- Score changes for this game:
  - Yellow for positive scores
  - Scarlet for negative scores
  - White for zero
- Two buttons:
  - **Play Again** (Yellow) - Starts a new game
  - **Close** (Gray) - Dismisses overlay

### 3. Cumulative Scoreboard

**Location:** Between bet panel and players list

**When Visible:** After first game completes (gamesPlayed > 0)

**Features:**
- Shows "Games Played: X" counter
- Table with player rankings
- Medals for top 3:
  - 🥇 First place
  - 🥈 Second place
  - 🥉 Third place
- Color-coded scores:
  - Yellow for positive total scores
  - Scarlet for negative total scores
  - White for zero

**Sorting:** Players sorted by cumulative score (highest to lowest)

**Persistence:**
- Scores tracked in `cumulativeScores` object (client-side only)
- Resets when leaving lobby
- Accumulates across multiple games in same lobby session

### 4. Play Again Functionality

**Function:** `playAgain()`

**Behavior:**
1. Closes the game over overlay
2. Calls `/api/games/{gameId}/start` endpoint
3. Starts a new game with same players
4. WebSocket broadcasts new game state to all clients
5. Scoreboard persists and accumulates

**User Experience:**
- Click "Play Again" button on game over overlay
- Game restarts immediately in BIDDING phase
- All players dealt new hands
- Cumulative scores update after each game

### 5. Score Tracking

**Variables Added:**
```javascript
let cumulativeScores = {};  // { playerId: totalScore }
let gamesPlayed = 0;        // Number of completed games
```

**Logic:**
- When GAME_OVER detected:
  - Add `gameState.scores` (score changes) to `cumulativeScores`
  - Increment `gamesPlayed` counter
  - Update scoreboard display
  - Show game over overlay

## User Experience Flow

### First Game
1. Players create/join lobby
2. Start game → BIDDING phase
3. Bid and play
4. Game ends → **GAME_OVER phase**
5. **Big overlay appears** with results
6. **Bet panel** shows final bet and multiplier
7. **Scoreboard appears** showing cumulative scores
8. Click **"Play Again"**

### Subsequent Games
1. New game starts automatically
2. Scoreboard persists, showing running totals
3. After game ends, overlay shows:
   - This game's score changes
   - (Scoreboard separately shows cumulative totals)
4. Players can keep playing and accumulating scores

### Leaving Lobby
- Scores are lost (not persisted to database)
- This is intentional - persistence is not a priority
- Each lobby session is independent

## Technical Details

### Bet Panel Visibility
```javascript
const betPanel = document.getElementById('betPanel');
if (gameState.phase === 'PLAY' || gameState.phase === 'GAME_OVER') {
    betPanel.style.display = 'block';
    // Update values...
} else {
    betPanel.style.display = 'none';
}
```

### Score Accumulation
```javascript
if (gameState.phase === 'GAME_OVER') {
    // Add this game's scores to running totals
    Object.entries(gameState.scores).forEach(([playerId, scoreChange]) => {
        if (!cumulativeScores[playerId]) {
            cumulativeScores[playerId] = 0;
        }
        cumulativeScores[playerId] += scoreChange;
    });
    gamesPlayed++;

    showGameOverOverlay();
    updateScoreboard();
}
```

### Winner Determination
```javascript
// Find player with highest score change this game
let winner = null;
let highestScore = -Infinity;

gameState.players.forEach(player => {
    const scoreChange = gameState.scores[player.playerId] || 0;
    if (scoreChange > highestScore) {
        highestScore = scoreChange;
        winner = player;
    }
});
```

## Testing Checklist

- [ ] Play a 3-player game to completion
- [ ] Verify game over overlay appears with correct scores
- [ ] Check bet panel shows during PLAY phase
- [ ] Verify bombs/rockets correctly increase multiplier
- [ ] Click "Play Again" and confirm new game starts
- [ ] Play multiple games and verify scoreboard accumulates correctly
- [ ] Verify medals (🥇🥈🥉) appear for top 3 players
- [ ] Test with different player counts (4-player, 5-player)
- [ ] Verify scores reset when leaving and rejoining lobby

## Future Enhancements (Not Implemented)

These could be added later:
- Persist scores to database
- Player profiles with all-time stats
- Game history/replay
- Export scoreboard as CSV
- Achievements/badges
- Sound effects for game over
- Animated confetti for winner

## Files Modified

1. `server/src/main/java/com/yourco/ddz/server/api/dto/GameStateResponse.java`
2. `web/index.html`

## Color Scheme Consistency

All new UI elements follow the "Against Autumn Fields" palette:
- **Scarlet (`#ab202a`):** Bet panel, negative scores, urgency
- **Charcoal (`#335155`):** Backgrounds, neutral elements
- **Black (`#15141a`):** Deep backgrounds, overlay
- **Yellow (`#f8cf2c`):** Highlights, positive scores, important info
