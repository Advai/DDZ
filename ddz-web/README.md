# DDZ Web Frontend

Production-ready React + TypeScript frontend for the Dou Dizhu (Fight the Landlord) card game.

## 🚀 Quick Start

```bash
# Install dependencies
npm install

# Start development server
npm run dev
# → Opens at http://localhost:3000

# Build for production
npm run build
```

## 📁 Project Structure

```
src/
├── components/
│   ├── common/           # Reusable UI components
│   │   ├── Button.tsx    # Button with variants (primary, secondary, danger)
│   │   └── Modal.tsx     # Modal dialog with backdrop
│   ├── layout/
│   │   └── TopNav.tsx    # Main navigation bar
│   └── game/             # Game-specific components (Phase 3-4)
│
├── pages/
│   ├── LandingPage.tsx        # Home page with "Start a New Game"
│   ├── GamePage.tsx           # Game lobby + table (Phase 2-3)
│   └── NotImplementedPage.tsx # Placeholder for unbuilt features
│
├── hooks/                # Custom React hooks (Phase 2+)
├── services/             # API/WebSocket clients (Phase 2+)
├── context/              # Global state management (Phase 2+)
└── styles/
    └── index.css         # Global styles + Tailwind directives
```

## 🎨 Tech Stack

- **React 18** - UI framework
- **TypeScript** - Type safety
- **Vite** - Build tool (instant HMR)
- **React Router v6** - Client-side routing
- **Tailwind CSS** - Utility-first styling
- **Axios** - HTTP client (Phase 2+)

## 🛣️ Routes

| Route | Page | Status |
|-------|------|--------|
| `/` | Landing page | ✅ Phase 1 |
| `/game/:gameId` | Game lobby/table | 🚧 Phase 2-3 |
| `/rules` | Rules overlay | 🚧 Future |
| `/shop` | Shop page | 🚧 Future |
| `/login` | Login page | 🚧 Future |

## 🎯 Phase 1 Complete ✅

**Wireframe Foundation** - All routes navigable with consistent branding

- ✅ Project setup (Vite + React + TypeScript)
- ✅ Tailwind CSS configured with custom theme
- ✅ Folder structure created
- ✅ Button component (primary, secondary, danger variants)
- ✅ Modal component (with backdrop, ESC key, animations)
- ✅ TopNav component (logo, menu, login button)
- ✅ NotImplementedPage (reusable placeholder)
- ✅ LandingPage wireframe (hero section, CTA button)
- ✅ GamePage placeholder
- ✅ React Router with all routes
- ✅ Dev server running at http://localhost:3000

## 🔜 Next Steps (Phase 2)

- [ ] NicknameModal component with validation
- [ ] API service layer (`/services/api.ts`)
- [ ] `POST /api/games` integration
- [ ] Game creation flow
- [ ] Shareable link generation

## 🛠️ Development

### Backend Proxy

Vite dev server proxies requests to Spring Boot:
- `/api/*` → `http://localhost:8080/api/*`
- `/ws/*` → `ws://localhost:8080/ws/*`

Make sure your Spring Boot backend is running on port 8080.

### Code Quality Standards

All code follows:
- **TypeScript best practices** (no `any` types, explicit interfaces)
- **React best practices** (functional components, custom hooks)
- **Clean code principles** (SRP, descriptive names, small functions)
- **Tailwind conventions** (mobile-first, utility classes)

### File Naming

- Components: `PascalCase.tsx` (e.g., `Button.tsx`)
- Hooks: `camelCase.ts` (e.g., `useWebSocket.ts`)
- Utils: `camelCase.ts` (e.g., `formatCard.ts`)

## 📦 Build & Deploy

```bash
# Build for production
npm run build

# Copy to Spring Boot static resources
cp -r dist/* ../server/src/main/resources/static/

# Deploy to Fly.io
cd ../server
./gradlew bootJar
flyctl deploy
```

## 🎮 Features

### Current (Phase 1)
- ✅ Responsive dark theme with green accents
- ✅ Mobile-first design
- ✅ Smooth animations and transitions
- ✅ Keyboard navigation (ESC to close modals)
- ✅ All routes accessible

### Coming Soon
- 🔜 Game creation with nickname
- 🔜 Shareable game links
- 🔜 Lobby with player seats
- 🔜 Real-time game table
- 🔜 Card selection and play
- 🔜 Bidding interface
- 🔜 Score tracking

## 📝 License

Proprietary - All rights reserved
