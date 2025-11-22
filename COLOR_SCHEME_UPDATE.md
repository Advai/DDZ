# UI Color Scheme Update - "Against Autumn Fields"

## New Color Palette

Applied the "Against Autumn Fields" color scheme from Canva:

- **Scarlet:** `#ab202a` - Deep red for accents, errors, and emphasis
- **Charcoal:** `#335155` - Dark teal-gray for panels and neutral elements
- **Black:** `#15141a` - Very dark background base
- **Yellow:** `#f8cf2c` - Bright yellow for highlights, buttons, and important info

## Color Mapping

### Background & Structure
| Element | Old Color | New Color |
|---------|-----------|-----------|
| Body background | Blue gradient (`#1e3c72` to `#2a5298`) | Black to Charcoal gradient (`#15141a` to `#335155`) |
| Panel background | White with opacity | Charcoal with opacity (`rgba(51, 81, 85, 0.4)`) |
| Panel borders | White with opacity | Yellow with opacity (`rgba(248, 207, 44, 0.3)`) |
| Panel headings | Gold (`#ffd700`) | Yellow (`#f8cf2c`) |

### Interactive Elements
| Element | Old Color | New Color |
|---------|-----------|-----------|
| Buttons | Gold (`#ffd700`) | Yellow (`#f8cf2c`) |
| Button text | Dark blue (`#1e3c72`) | Black (`#15141a`) |
| Button hover | Light gold (`#ffed4e`) | Bright yellow (`#ffdc4e`) |
| Selected cards | Green border (`#4caf50`) | Yellow border (`#f8cf2c`) |

### Status Indicators
| Element | Old Color | New Color |
|---------|-----------|-----------|
| Connected status | Green background | Yellow background (`rgba(248, 207, 44, 0.3)`) |
| Disconnected status | Red background | Scarlet background (`rgba(171, 32, 42, 0.3)`) |
| Log success messages | Green (`#51cf66`) | Yellow (`#f8cf2c`) |
| Log error messages | Red (`#ff6b6b`) | Scarlet (`#ab202a`) |

### Game Elements
| Element | Old Color | New Color |
|---------|-----------|-----------|
| Landlord highlight | Gold (`#ffd700`) | Yellow (`#f8cf2c`) |
| Current turn indicator | Green (`#4caf50`) | Scarlet (`#ab202a`) |
| Red suit cards (♥ ♦) | Bright red (`#e74c3c`) | Scarlet (`#ab202a`) |
| Black suit cards (♣ ♠) | Dark gray (`#2c3e50`) | Charcoal (`#335155`) |

### Special Components

#### Joker Cards
- **Background:** Scarlet to Charcoal gradient (`linear-gradient(135deg, #ab202a 0%, #335155 100%)`)
- **Text/Icon:** Yellow (`#f8cf2c`)
- **Border:** Yellow (`#f8cf2c`)
- **Hover:** Gradient reverses (Charcoal to Scarlet)

#### Bidding Panel
- **Background:** Charcoal with opacity (`rgba(51, 81, 85, 0.4)`)
- **Border:** Yellow with opacity
- **Heading:** Yellow (`#f8cf2c`)
- **Active status text:** Yellow (`#f8cf2c`)
- **Inactive status text:** Charcoal (`#335155`)

#### Bid Buttons (when enabled)
| Button | Color | Description |
|--------|-------|-------------|
| Pass (0) | Charcoal (`#335155`) | Neutral gray-green |
| Bid 1 | Yellow (`#f8cf2c`) | Bright yellow |
| Bid 2 | Yellow-to-Scarlet gradient | Transitional gradient |
| Bid 3 | Scarlet (`#ab202a`) | Bold red for highest bid |

#### Info Boxes
- **Standard:** Black with opacity (`rgba(21, 20, 26, 0.5)`)
- **Join Code:** Yellow background with yellow border and text
- **Cards container:** Black with opacity (`rgba(21, 20, 26, 0.5)`)
- **Current lead:** Scarlet background with Scarlet border

#### Game Log
- **Background:** Black with opacity (`rgba(21, 20, 26, 0.7)`)
- **Border:** Charcoal with opacity
- **Success text:** Yellow (`#f8cf2c`)
- **Error text:** Scarlet (`#ab202a`)

## Visual Hierarchy

The new color scheme creates a clear visual hierarchy:

1. **Primary Actions/Info:** Yellow (`#f8cf2c`)
   - Buttons, highlights, landlord status, join codes

2. **Active States/Urgency:** Scarlet (`#ab202a`)
   - Current turn, high bids, errors, important alerts

3. **Structure/Containers:** Charcoal (`#335155`)
   - Panels, player list items, neutral buttons, black suit cards

4. **Background/Depth:** Black (`#15141a`)
   - Main background, deep containers, shadows

## Testing the New Look

The color scheme provides:
- **Better contrast** between text and backgrounds
- **Clear focus** on important elements (yellow stands out)
- **Cohesive aesthetic** that matches the "Against Autumn Fields" theme
- **Accessible** color combinations for readability
- **Distinctive states** for different game phases and player statuses

All changes are applied to `/Users/advai/Documents/DDZ/web/index.html`. Refresh the browser to see the new color scheme!
