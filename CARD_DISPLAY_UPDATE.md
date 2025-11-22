# Card Display Update

## Changes Made

Updated the card rendering to use numbers, symbols, and icons instead of spelled-out names.

### Display Format

#### Regular Cards
- **Numbers:** 3, 4, 5, 6, 7, 8, 9, **10** (changed from "T")
- **Face Cards:** J, Q, K, A
- **Special:** 2

#### Suits (already working, kept as-is)
- Hearts: ♥ (red)
- Diamonds: ♦ (red)
- Clubs: ♣ (black)
- Spades: ♠ (black)

#### Jokers (new special handling)
- **Little Joker:** 🃏 (playing card back joker - black & white style)
- **Big Joker:** 🤡 (clown face - colorful)

### Visual Changes

#### Joker Cards
- **Background:** Purple gradient (lighter to darker purple)
- **Border:** Gold (#ffd700)
- **Icon Size:** 48px (larger than regular cards)
- **Hover Effect:** Gradient reverses direction

#### Regular Cards
- White background
- Red text for hearts/diamonds
- Black text for clubs/spades
- Rank + suit symbol layout

### Code Changes

**File:** `web/index.html`

1. **Added `formatRank()` function:**
```javascript
function formatRank(rank) {
    const rankMap = {
        'T': '10',      // Ten
        'LJ': '🃏',     // Little Joker
        'BJ': '🤡'      // Big Joker
    };
    return rankMap[rank] || rank;
}
```

2. **Updated `createCardElement()` to handle jokers:**
- Detects joker cards (rank === 'LJ' or 'BJ')
- Applies special `.joker` CSS class
- Centers large emoji icon
- Skips suit display for jokers

3. **Added CSS styling for jokers:**
```css
.card.joker {
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
    color: white;
    border-color: #ffd700;
}

.card.joker:hover {
    background: linear-gradient(135deg, #764ba2 0%, #667eea 100%);
}
```

### Examples

Before:
```
T of HEARTS    → Now: 10 ♥
K of SPADES    → Now: K ♠
LJ (joker)     → Now: 🃏 (purple card with gold border)
BJ (joker)     → Now: 🤡 (purple card with gold border)
```

### Testing

Refresh the browser (F5) and deal cards. You should see:
1. **Number cards** display as digits: 3, 4, 5... 10
2. **Face cards** display as letters: J, Q, K, A
3. **Suit symbols** next to rank: ♥ ♦ ♣ ♠
4. **Jokers** appear as large emojis on purple gradient background with gold border

The cards are sorted in standard Dou Dizhu order:
`3, 4, 5, 6, 7, 8, 9, 10, J, Q, K, A, 2, 🃏, 🤡`
