# DDZ Production Roadmap (Dec 1-31, 2024)

## Status Overview

**Last Updated:** December 1, 2024
**Target Launch Date:** December 31, 2024
**Current Phase:** Week 1 - Foundation & Critical Fixes

### Completed Tasks (Day 1)
- ✅ Repository cleanup (removed 10 temporary MD files)
- ✅ Fixed critical WebSocket connection issues in production
- ✅ Deployed fixes to https://ddz-game.fly.dev/
- ✅ Validated multi-client connections and game creation

### Next Steps (Days 2-3)
- Set up Playwright E2E test suite
- Add backend integration tests
- Begin PostgreSQL setup planning

---

## Overview
This plan addresses repository organization, production deployment fixes, testing infrastructure, and prioritization for a fully playable 5-7 player webapp with scores and profiles by December 31st.

---

## PHASE 1: Repository Cleanup & Documentation

### Task 1.1: Remove Non-Documentation MD Files
**Goal:** Clean up repository by removing temporary/generated markdown files

**Files to KEEP:**
- `README.md` - Main project readme
- `DEPLOYMENT.md` - Production deployment guide
- `TESTING.md` - Testing documentation
- `FRONTEND_DEVELOPER_GUIDE.md` - Frontend architecture guide
- `QUICKSTART.txt` - Quick start guide
- `web/README.md` - Web-specific documentation

**Files to REMOVE:**
- `BUG_FIXES.md`
- `BUG_FIXES_ROUND2.md`
- `BUG_FIXES_ROUND3.md`
- `CARD_DISPLAY_FIX.md`
- `CARD_DISPLAY_UPDATE.md`
- `COLOR_SCHEME_UPDATE.md`
- `CRITICAL_FIXES.md`
- `FIXES_APPLIED.md`
- `GAME_OVER_AND_SCORING_UPDATE.md`
- `PR_DESCRIPTION.md`

**Effort:** 5 minutes

---

## PHASE 2: CRITICAL - Fix Production WebSocket Connection Issue

### Problem Analysis
The WebSocket connection fails in production (https://ddz-game.fly.dev/) showing "Disconnected" status when users create games.

**Root Cause:** WebSocket upgrade requests fail at Fly.io's reverse proxy due to:
1. Fly.io terminates HTTPS at edge but doesn't properly forward WebSocket upgrade headers
2. Spring Boot not configured to handle proxied WebSocket connections
3. Client uses manual string replacement instead of config.js WS_URL

### Task 2.1: Fix WebSocket Configuration
**Priority:** CRITICAL (blocks all production use)

**Changes Required:**

1. **Update `server/src/main/resources/application.yml`:**
   - Add WebSocket-specific Tomcat configuration
   - Configure proxy-aware headers handling
   - Add connection timeout settings

2. **Update `fly.toml`:**
   - Configure explicit WebSocket support
   - Ensure upgrade headers are forwarded
   - Add WebSocket-specific health checks

3. **Update `web/index.html`:**
   - Use `window.DDZ_CONFIG.WS_URL` instead of manual string replacement
   - Add detailed WebSocket connection logging for debugging
   - Add exponential backoff retry logic

4. **Update `web/config.js`:**
   - Ensure WS_URL is properly exposed and used

**Validation Method:**
- Use Puppeteer MCP to navigate to https://ddz-game.fly.dev/
- Create a 3-player test game
- Verify WebSocket connection succeeds
- Test player switching functionality
- Monitor `flyctl logs` for connection attempts

**Effort:** 2-4 hours

**Critical Files:**
- `server/src/main/resources/application.yml`
- `fly.toml`
- `web/index.html` (connectWebSocket function, lines 931-962)
- `web/config.js`

---

## PHASE 3: Testing Infrastructure Setup

### Task 3.1: Enable Automated E2E Testing with Playwright
**Goal:** Prevent regressions in WebSocket connectivity and UI functionality

**Approach:**
1. Create `tests/` directory at project root
2. Install Playwright: `npm init -y && npm install -D @playwright/test`
3. Create `playwright.config.ts` for test configuration
4. Write initial E2E tests:
   - Test 1: Create game and verify WebSocket connection
   - Test 2: Join game flow
   - Test 3: Start game and verify bidding phase
   - Test 4: Complete 3-player game flow
   - Test 5: Player switching in test mode

**Test Structure:**
```
tests/
├── e2e/
│   ├── game-creation.spec.ts
│   ├── websocket-connection.spec.ts
│   ├── bidding-phase.spec.ts
│   └── full-game-flow.spec.ts
├── fixtures/
│   └── test-server.ts
└── playwright.config.ts
```

**Benefits:**
- Catches UI regressions automatically
- Validates WebSocket connectivity
- Tests against production deployment
- Can run in CI/CD pipeline

**Effort:** 8-12 hours for initial suite

### Task 3.2: Add Backend Integration Tests
**Goal:** Test REST API and WebSocket endpoints

**Approach:**
1. Add Spring WebFlux test dependency to `server/build.gradle`
2. Create integration tests for:
   - GameController REST endpoints
   - WebSocket message handling
   - Multi-client WebSocket scenarios
3. Follow existing patterns from `GameRegistryTest.java`

**Effort:** 6-8 hours

---

## PHASE 4: Production-Ready Features (December 31st Goal)

### Decision Point: Feature Prioritization

**Timeline:** 30 days (Dec 1 - Dec 31)
**Goal:** Fully playable 5-7 player webapp with scores and profiles

### Tier 1: MUST HAVE (Essential for Playability)
**Priority 1: Fix WebSocket Connection** ✓ (covered in Phase 2)
**Priority 2: Session Recovery**
- Problem: Players disconnected mid-game cannot rejoin
- Solution: Store active games in database, allow reconnection
- Effort: 16-20 hours

**Priority 3: Basic Score Persistence**
- Store game results per player
- Simple leaderboard (wins/losses/total games)
- Effort: 12-16 hours

### Tier 2: SHOULD HAVE (Better UX)
**Priority 4: User Profiles**
- Create/save player profiles (name, stats)
- Persistent player IDs (not just session-based UUIDs)
- Effort: 12-16 hours

**Priority 5: Connection Stability**
- Auto-reconnect on disconnect
- Better error handling and user feedback
- Effort: 8-12 hours

### Tier 3: NICE TO HAVE (Can defer to January)
- Authentication (OAuth, email/password)
- Rate limiting
- Input sanitization (basic validation exists)
- Game history replay
- Advanced analytics

### Recommended 30-Day Schedule

**Week 1 (Dec 1-7):**
- Day 1-2: Fix WebSocket issue + deploy + validate
- Day 3-4: Set up Playwright E2E tests
- Day 5-7: Add H2 database + Spring Data JPA setup

**Week 2 (Dec 8-14):**
- Day 8-10: Implement session recovery
- Day 11-12: Basic score persistence
- Day 13-14: Testing + bug fixes

**Week 3 (Dec 15-21):**
- Day 15-17: User profiles
- Day 18-19: Connection stability improvements
- Day 20-21: End-to-end testing

**Week 4 (Dec 22-28):**
- Day 22-24: Polish + UX improvements
- Day 25-26: Load testing
- Day 27-28: Final bug fixes

**Dec 29-31:** Buffer for issues

---

## USER DECISIONS (Confirmed)

1. ✅ **Puppeteer MCP Usage:** Yes, use Puppeteer to debug WebSocket live
2. ✅ **Testing Priority:** Playwright E2E tests AFTER fixing WebSocket
3. ✅ **Database Choice:** PostgreSQL (comparison below)
4. ✅ **Feature Scope:** Option B - Full user auth + profiles + leaderboards
5. ✅ **MVP Requirements:** Persistent user accounts, production-ready design

### Database Comparison: H2 vs PostgreSQL

| Aspect | H2 Embedded | PostgreSQL | Winner |
|--------|-------------|------------|---------|
| **Setup Complexity** | Low (single JAR dependency) | Medium (Fly.io Postgres add-on) | H2 |
| **Production Ready** | Not recommended for production | Industry standard | PostgreSQL ✓ |
| **Concurrent Users** | Limited (single file lock) | Excellent (handles 1000s) | PostgreSQL ✓ |
| **Data Safety** | File corruption risk | ACID compliant, WAL | PostgreSQL ✓ |
| **Scalability** | Cannot scale horizontally | Can add replicas/sharding | PostgreSQL ✓ |
| **Backup/Recovery** | Manual file copy | pg_dump, WAL archiving | PostgreSQL ✓ |
| **Cost on Fly.io** | Free (included) | ~$1.94/month (256MB) | H2 |
| **Learning Curve** | None (same JPA) | Same (Spring Data JPA) | Tie |
| **Local Development** | Easy (auto-creates file) | Need Docker or local PG | H2 |
| **For Production Use** | ❌ Not suitable | ✅ Recommended | PostgreSQL ✓ |

**Recommendation:** PostgreSQL is the correct choice for your use case:
- You want production-ready design for real users
- Need to handle traffic spikes reliably
- Bug-free gameplay is critical (PostgreSQL's ACID guarantees)
- Minimal cost (~$2/month on Fly.io)
- Same Spring Data JPA code works with both

**Setup on Fly.io:**
```bash
flyctl postgres create --name ddz-postgres --region ord
flyctl postgres attach ddz-postgres --app ddz-game
```

---

## IMPLEMENTATION ORDER (Finalized for Dec 31st Deadline)

### Week 1: Foundation & Critical Fixes (Dec 1-7)

**Day 1 (Today):**
1. ✅ Clean up MD files (5 min)
2. ✅ Use Puppeteer MCP to debug WebSocket connection live at https://ddz-game.fly.dev/
3. ✅ Fix WebSocket configuration (application.yml, fly.toml, web/index.html, web/config.js)
4. ✅ Deploy fix and validate with Puppeteer

**Day 2-3:**
5. Set up Playwright E2E test suite
   - Create tests/ directory structure
   - Install Playwright dependencies
   - Write critical E2E tests:
     - Test: Create game + WebSocket connection
     - Test: Join game flow
     - Test: Start game + bidding phase
     - Test: Complete 3-player game
     - Test: Player switching in test mode
6. Add backend integration tests
   - GameController REST endpoint tests
   - WebSocket integration tests

**Day 4-5:**
7. Set up PostgreSQL on Fly.io
   - Create Postgres cluster: `flyctl postgres create --name ddz-postgres --region ord`
   - Attach to app: `flyctl postgres attach ddz-postgres --app ddz-game`
8. Add Spring Data JPA + PostgreSQL dependencies to server/build.gradle
9. Configure application.yml for PostgreSQL (with profiles: dev/prod)
10. Set up local PostgreSQL via Docker for development

**Day 6-7:**
11. Design and create JPA entities:
    - UserEntity (id, username, email, passwordHash, createdAt, stats)
    - GameEntity (id, joinCode, createdAt, completedAt, status)
    - GamePlayerEntity (join table: game_id, user_id, role, finalScore)
    - LeaderboardEntity (aggregated stats view)
12. Create Spring Data repositories
13. Write database migration scripts (Flyway)

### Week 2: Core Persistence & Auth (Dec 8-14)

**Day 8-10:**
14. Implement User Authentication
    - Spring Security setup
    - Registration endpoint (/api/auth/register)
    - Login endpoint (/api/auth/login) - JWT tokens
    - Logout endpoint
    - Password hashing (BCrypt)
15. Update GameController to require authentication
16. Modify GameRegistry to persist games to database
17. Add user profile endpoints (/api/users/me, /api/users/{id}/stats)

**Day 11-12:**
18. Implement Session Recovery
    - Store active game state snapshots in database
    - Add reconnection logic to WebSocket handler
    - Update frontend to detect disconnection and auto-reconnect
    - Add "Resume Game" UI for returning users

**Day 13-14:**
19. Implement Score Persistence
    - Save game results to database on completion
    - Update GameStateResponse to include historical scores
    - Add leaderboard calculation logic
    - Create leaderboard endpoint (/api/leaderboard)
20. Write integration tests for persistence layer
21. Test database migrations and rollbacks

### Week 3: User Profiles & Frontend Integration (Dec 15-21)

**Day 15-16:**
22. Build User Profile Frontend
    - Registration page
    - Login page
    - User profile page (stats display)
    - Session management (store JWT in localStorage)
23. Update game creation flow to require login
24. Add user menu (profile, logout)

**Day 17-18:**
25. Build Leaderboard Frontend
    - Leaderboard page (/leaderboard)
    - Filter by time period (all-time, this month, this week)
    - Display: rank, username, wins, losses, win rate, total games
26. Add "My Games" history page
27. Add game replay viewer (view past game states)

**Day 19-20:**
28. Connection Stability Improvements
    - Implement exponential backoff for WebSocket reconnection
    - Add connection status indicator in UI
    - Handle stale sessions gracefully
    - Add heartbeat/ping-pong to keep connections alive
29. Add detailed error messages for user feedback
30. Improve loading states and transitions

**Day 21:**
31. End-to-End testing of full user flow:
    - Register → Login → Create Game → Play → View Scores → Leaderboard
32. Fix any bugs discovered during E2E testing

### Week 4: Polish, Testing & Deployment (Dec 22-28)

**Day 22-24:**
33. UI/UX Polish
    - Loading spinners for async operations
    - Better error messaging
    - Responsive design fixes
    - Mobile testing and fixes
34. Add input validation
    - Username requirements (3-20 chars, alphanumeric)
    - Password requirements (min 8 chars, complexity)
    - Game name validation
35. Add rate limiting (Spring Security)
    - 10 game creations per user per hour
    - 100 API requests per user per minute

**Day 25-26:**
36. Load Testing
    - Test with 50 concurrent users
    - Test with 10 simultaneous games
    - Monitor database performance
    - Optimize slow queries (add indexes)
37. Security audit
    - SQL injection prevention (JPA handles this)
    - XSS prevention (Content-Security-Policy headers)
    - CSRF token validation
    - Secure WebSocket connections

**Day 27-28:**
38. Final bug fixes based on load testing
39. Write deployment runbook
40. Update DEPLOYMENT.md with database setup
41. Create backup/restore procedures
42. Final production deployment

**Dec 29-31:** Buffer for Critical Issues

---

## DETAILED TECHNICAL SPECIFICATIONS

### Authentication Flow

**Registration:**
```
POST /api/auth/register
{
  "username": "player123",
  "email": "player@example.com",
  "password": "SecurePass123"
}

Response: 201 Created
{
  "userId": "uuid",
  "username": "player123",
  "token": "jwt-token"
}
```

**Login:**
```
POST /api/auth/login
{
  "username": "player123",
  "password": "SecurePass123"
}

Response: 200 OK
{
  "userId": "uuid",
  "username": "player123",
  "token": "jwt-token",
  "expiresIn": 86400
}
```

**Token Usage:**
```
WebSocket: ws://ddz-game.fly.dev/ws/game/{gameId}?token={jwt-token}
REST API: Authorization: Bearer {jwt-token}
```

### Database Schema

**users table:**
```sql
CREATE TABLE users (
  id UUID PRIMARY KEY,
  username VARCHAR(20) UNIQUE NOT NULL,
  email VARCHAR(255) UNIQUE NOT NULL,
  password_hash VARCHAR(255) NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT NOW(),
  last_login TIMESTAMP,
  is_active BOOLEAN DEFAULT true
);

CREATE INDEX idx_users_username ON users(username);
CREATE INDEX idx_users_email ON users(email);
```

**games table:**
```sql
CREATE TABLE games (
  id VARCHAR(50) PRIMARY KEY,
  join_code VARCHAR(6) UNIQUE NOT NULL,
  creator_id UUID REFERENCES users(id),
  player_count INT NOT NULL,
  max_bid INT NOT NULL,
  status VARCHAR(20) NOT NULL, -- LOBBY, ACTIVE, COMPLETED, ABANDONED
  created_at TIMESTAMP NOT NULL DEFAULT NOW(),
  started_at TIMESTAMP,
  completed_at TIMESTAMP,
  winner_team VARCHAR(20) -- LANDLORDS, FARMERS
);

CREATE INDEX idx_games_join_code ON games(join_code);
CREATE INDEX idx_games_status ON games(status);
CREATE INDEX idx_games_created_at ON games(created_at DESC);
```

**game_players table:**
```sql
CREATE TABLE game_players (
  game_id VARCHAR(50) REFERENCES games(id),
  user_id UUID REFERENCES users(id),
  player_index INT NOT NULL,
  is_landlord BOOLEAN NOT NULL DEFAULT false,
  final_score INT,
  bid_amount INT,
  PRIMARY KEY (game_id, user_id)
);

CREATE INDEX idx_game_players_user_id ON game_players(user_id);
```

**game_states table (for session recovery):**
```sql
CREATE TABLE game_states (
  id BIGSERIAL PRIMARY KEY,
  game_id VARCHAR(50) REFERENCES games(id),
  state_json JSONB NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_game_states_game_id ON game_states(game_id);
CREATE INDEX idx_game_states_created_at ON game_states(game_id, created_at DESC);
```

**Materialized View for Leaderboard:**
```sql
CREATE MATERIALIZED VIEW leaderboard AS
SELECT
  u.id as user_id,
  u.username,
  COUNT(*) as total_games,
  SUM(CASE WHEN gp.final_score > 0 THEN 1 ELSE 0 END) as wins,
  SUM(CASE WHEN gp.final_score < 0 THEN 1 ELSE 0 END) as losses,
  SUM(gp.final_score) as total_score,
  ROUND(100.0 * SUM(CASE WHEN gp.final_score > 0 THEN 1 ELSE 0 END) / COUNT(*), 2) as win_rate
FROM users u
JOIN game_players gp ON u.id = gp.user_id
JOIN games g ON gp.game_id = g.id
WHERE g.status = 'COMPLETED'
GROUP BY u.id, u.username
ORDER BY total_score DESC;

CREATE UNIQUE INDEX idx_leaderboard_user_id ON leaderboard(user_id);
```

### Session Recovery Flow

1. User disconnects mid-game
2. WebSocket handler saves game state snapshot to game_states table
3. User reconnects (manually or auto)
4. Frontend checks for active games: `GET /api/games/active`
5. If active game found, prompt user: "Resume game?"
6. On resume, load latest game state from database
7. Reconnect WebSocket with resumed game state

### Connection Stability Implementation

**WebSocket Reconnection Logic (web/index.html):**
```javascript
let reconnectAttempts = 0;
const maxReconnectAttempts = 5;
const baseDelay = 1000; // 1 second

function connectWebSocket() {
  // ... existing code ...

  ws.onclose = (event) => {
    if (reconnectAttempts < maxReconnectAttempts) {
      const delay = baseDelay * Math.pow(2, reconnectAttempts);
      log(`Connection lost. Reconnecting in ${delay/1000}s...`, 'error');
      setTimeout(() => {
        reconnectAttempts++;
        connectWebSocket();
      }, delay);
    } else {
      log('Failed to reconnect. Please refresh the page.', 'error');
      // Optionally: show "Resume Game" modal
    }
  };

  ws.onopen = () => {
    reconnectAttempts = 0; // Reset on successful connection
    // ... existing code ...
  };
}
```

**Heartbeat/Keep-Alive (Spring Boot):**
```java
@Scheduled(fixedRate = 30000) // Every 30 seconds
public void sendHeartbeat() {
  for (Map.Entry<String, List<WebSocketSession>> entry : gameSessions.entrySet()) {
    for (WebSocketSession session : entry.getValue()) {
      if (session.isOpen()) {
        sendMessage(session, new HeartbeatMessage());
      }
    }
  }
}
```

---

## CRITICAL FILES TO MODIFY

### Phase 1: Repository Cleanup (Day 1)
- Remove: All BUG_FIXES*.md, CARD_DISPLAY*.md, COLOR_SCHEME_UPDATE.md, etc.

### Phase 2: WebSocket Fix (Day 1)
- `server/src/main/resources/application.yml` - Add WebSocket config
- `fly.toml` - Add WebSocket upgrade support
- `web/index.html` (connectWebSocket function, lines 931-962) - Use config.js WS_URL
- `web/config.js` - Expose WS_URL properly

### Phase 3: Testing Infrastructure (Days 2-3)
**New Files:**
- `tests/playwright.config.ts`
- `tests/e2e/game-creation.spec.ts`
- `tests/e2e/websocket-connection.spec.ts`
- `tests/e2e/bidding-phase.spec.ts`
- `tests/e2e/full-game-flow.spec.ts`
- `server/src/test/java/com/yourco/ddz/server/api/GameControllerIntegrationTest.java`
- `server/src/test/java/com/yourco/ddz/server/ws/WebSocketIntegrationTest.java`

**Modified Files:**
- `server/build.gradle` - Add Spring WebFlux test dependency
- `package.json` (new) - Playwright dependencies

### Phase 4: PostgreSQL Setup (Days 4-5)
**Modified Files:**
- `server/build.gradle` - Add Spring Data JPA, PostgreSQL driver, Flyway
- `server/src/main/resources/application.yml` - Add datasource config with profiles
- `server/src/main/resources/application-dev.yml` (new) - Local PostgreSQL config
- `server/src/main/resources/application-prod.yml` (new) - Fly.io PostgreSQL config
- `docker-compose.yml` - Add local PostgreSQL service

### Phase 5: JPA Entities & Repositories (Days 6-7)
**New Files:**
- `server/src/main/java/com/yourco/ddz/server/entity/UserEntity.java`
- `server/src/main/java/com/yourco/ddz/server/entity/GameEntity.java`
- `server/src/main/java/com/yourco/ddz/server/entity/GamePlayerEntity.java`
- `server/src/main/java/com/yourco/ddz/server/repository/UserRepository.java`
- `server/src/main/java/com/yourco/ddz/server/repository/GameRepository.java`
- `server/src/main/java/com/yourco/ddz/server/repository/GamePlayerRepository.java`
- `server/src/main/resources/db/migration/V1__create_users_table.sql`
- `server/src/main/resources/db/migration/V2__create_games_table.sql`
- `server/src/main/resources/db/migration/V3__create_game_players_table.sql`
- `server/src/main/resources/db/migration/V4__create_game_states_table.sql`
- `server/src/main/resources/db/migration/V5__create_leaderboard_view.sql`

### Phase 6: Authentication (Days 8-10)
**New Files:**
- `server/src/main/java/com/yourco/ddz/server/config/SecurityConfig.java`
- `server/src/main/java/com/yourco/ddz/server/security/JwtTokenProvider.java`
- `server/src/main/java/com/yourco/ddz/server/security/JwtAuthenticationFilter.java`
- `server/src/main/java/com/yourco/ddz/server/api/AuthController.java`
- `server/src/main/java/com/yourco/ddz/server/api/UserController.java`
- `server/src/main/java/com/yourco/ddz/server/api/dto/RegisterRequest.java`
- `server/src/main/java/com/yourco/ddz/server/api/dto/LoginRequest.java`
- `server/src/main/java/com/yourco/ddz/server/api/dto/AuthResponse.java`
- `server/src/main/java/com/yourco/ddz/server/service/UserService.java`

**Modified Files:**
- `server/build.gradle` - Add Spring Security, JWT dependencies
- `server/src/main/java/com/yourco/ddz/server/api/GameController.java` - Add @PreAuthorize
- `server/src/main/java/com/yourco/ddz/server/ws/GameWebSocketHandler.java` - Extract JWT from query params

### Phase 7: Session Recovery & Score Persistence (Days 11-14)
**New Files:**
- `server/src/main/java/com/yourco/ddz/server/service/GamePersistenceService.java`
- `server/src/main/java/com/yourco/ddz/server/service/LeaderboardService.java`
- `server/src/main/java/com/yourco/ddz/server/api/LeaderboardController.java`

**Modified Files:**
- `server/src/main/java/com/yourco/ddz/server/core/GameRegistry.java` - Add database persistence
- `server/src/main/java/com/yourco/ddz/server/ws/GameWebSocketHandler.java` - Save state on disconnect, load on reconnect
- `server/src/main/java/com/yourco/ddz/server/api/GameController.java` - Add /active and /history endpoints

### Phase 8: Frontend User System (Days 15-18)
**New Files:**
- `web/login.html`
- `web/register.html`
- `web/profile.html`
- `web/leaderboard.html`
- `web/history.html`
- `web/js/auth.js` - Authentication utilities
- `web/css/auth.css` - Login/register styling

**Modified Files:**
- `web/index.html` - Add login requirement, user menu, JWT handling
- `web/config.js` - Add token storage

### Phase 9: Connection Stability (Days 19-20)
**Modified Files:**
- `web/index.html` - Add reconnection logic with exponential backoff
- `server/src/main/java/com/yourco/ddz/server/ws/GameWebSocketHandler.java` - Add heartbeat mechanism
- `server/src/main/java/com/yourco/ddz/server/config/WebSocketConfig.java` - Configure keep-alive

### Phase 10: Polish & Security (Days 22-26)
**Modified Files:**
- `web/index.html` - Add loading states, error messages, validation
- `web/css/styles.css` - Responsive design fixes
- `server/src/main/java/com/yourco/ddz/server/config/SecurityConfig.java` - Add rate limiting
- `server/src/main/resources/application.yml` - Add CSP headers

**New Files:**
- `server/src/main/java/com/yourco/ddz/server/config/RateLimitingConfig.java`

### Phase 11: Documentation Updates (Days 27-28)
**Modified Files:**
- `DEPLOYMENT.md` - Add PostgreSQL setup, authentication setup
- `README.md` - Update with authentication flow
- `TESTING.md` - Add Playwright test documentation

**New Files:**
- `DEPLOYMENT_RUNBOOK.md` - Step-by-step production deployment
- `BACKUP_PROCEDURES.md` - Database backup and restore

---

## SUMMARY

This is a comprehensive plan to transform the DDZ game from a prototype to a production-ready web application by December 31st. The plan prioritizes:

1. **Immediate fixes** - WebSocket connectivity (Day 1)
2. **Quality assurance** - E2E testing with Playwright (Days 2-3)
3. **Data persistence** - PostgreSQL with full user accounts (Days 4-14)
4. **User experience** - Authentication, profiles, leaderboards (Days 15-21)
5. **Reliability** - Connection stability, error handling (Days 19-20)
6. **Production readiness** - Security, rate limiting, polish (Days 22-28)

The plan is aggressive but achievable with focused daily work. PostgreSQL provides production-grade data reliability essential for accurate score tracking. The 3-day buffer (Dec 29-31) accounts for unexpected issues.

**Total estimated effort:** ~200 hours over 30 days (~6-7 hours/day average)
