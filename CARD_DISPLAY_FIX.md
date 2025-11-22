# Card Display Fix - Complete Mapping

## Problem
Cards were displaying as "SEVEN" instead of "7" because the backend sends full enum names (e.g., "SEVEN", "JACK", "QUEEN") but the frontend was only mapping a few single-character codes.

## Root Cause
The backend `CardDto` serializes cards using `.name()` on the enums:
```java
// Backend sends:
rank: "THREE", "FOUR", "FIVE", ..., "JACK", "QUEEN", "KING", "ACE", "TWO", "LITTLE_JOKER", "BIG_JOKER"
suit: "CLUBS", "DIAMONDS", "HEARTS", "SPADES", "JOKER"
```

## Fixes Applied

### 1. Complete Rank Mapping
**File:** `web/index.html` - `formatRank()` function

Updated to map ALL backend enum names to display values:

```javascript
function formatRank(rank) {
    const rankMap = {
        'THREE': '3',
        'FOUR': '4',
        'FIVE': '5',
        'SIX': '6',
        'SEVEN': '7',
        'EIGHT': '8',
        'NINE': '9',
        'TEN': '10',
        'JACK': 'J',
        'QUEEN': 'Q',
        'KING': 'K',
        'ACE': 'A',
        'TWO': '2',
        'LITTLE_JOKER': '🃏',
        'BIG_JOKER': '🤡'
    };
    return rankMap[rank] || rank;
}
```

### 2. Updated Joker Detection
**File:** `web/index.html` - `createCardElement()` function

Changed from old single-character codes to full enum names:
```javascript
// OLD: const isJoker = card.rank === 'LJ' || card.rank === 'BJ';
// NEW:
const isJoker = card.rank === 'LITTLE_JOKER' || card.rank === 'BIG_JOKER';
```

### 3. Fixed Card Sorting
**File:** `web/index.html` - `renderHand()` function

Updated sort order to use full enum names:
```javascript
const rankOrder = [
    'THREE', 'FOUR', 'FIVE', 'SIX', 'SEVEN', 'EIGHT', 'NINE', 'TEN',
    'JACK', 'QUEEN', 'KING', 'ACE', 'TWO', 'LITTLE_JOKER', 'BIG_JOKER'
];
```

## Expected Display

### Number Cards
```
THREE    → 3
FOUR     → 4
FIVE     → 5
SIX      → 6
SEVEN    → 7
EIGHT    → 8
NINE     → 9
TEN      → 10
```

### Face Cards
```
JACK     → J
QUEEN    → Q
KING     → K
ACE      → A
TWO      → 2
```

### Jokers
```
LITTLE_JOKER → 🃏 (purple card, gold border)
BIG_JOKER    → 🤡 (purple card, gold border)
```

### Suits (unchanged)
```
HEARTS    → ♥ (red)
DIAMONDS  → ♦ (red)
CLUBS     → ♣ (black)
SPADES    → ♠ (black)
```

## Testing
1. Start the servers (both frontend and backend)
2. Create a game and deal cards
3. Verify all cards show as numbers/symbols (NOT "SEVEN", "JACK", etc.)
4. Check that cards are sorted correctly: 3, 4, 5... 10, J, Q, K, A, 2, 🃏, 🤡

## Complete Card Display Examples

**3 of Hearts:**
```
┌─────┐
│ 3   │
│  ♥  │
│     │
└─────┘
(white card, red text)
```

**Jack of Spades:**
```
┌─────┐
│ J   │
│  ♠  │
│     │
└─────┘
(white card, black text)
```

**Little Joker:**
```
┌─────┐
│     │
│  🃏  │
│     │
└─────┘
(purple gradient, gold border, large icon)
```

**Big Joker:**
```
┌─────┐
│     │
│  🤡  │
│     │
└─────┘
(purple gradient, gold border, large icon)
```
