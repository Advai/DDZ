# DDZ Testing Guide

## Overview

This project uses Playwright for E2E tests and JUnit 5 for backend tests.

## Running Tests

### E2E Tests (Playwright)

```bash
# Run all E2E tests (against staging)
npm test

# Run against local server
npm run test:local

# Run against production
npm run test:prod

# Run in headed mode (see browser)
npm run test:headed

# Debug mode
npm run test:debug

# Interactive UI mode
npm run test:ui

# View last test report
npm run test:report
```

### Backend Tests (JUnit)

```bash
# All backend tests
./gradlew test

# Engine tests only
./gradlew :engine:test

# Server tests only
./gradlew :server:test
```

## Test Categories

### E2E Tests (tests/e2e/)

1. **01-game-creation.spec.ts** - Basic game creation smoke test
2. **02-websocket-connection.spec.ts** - WebSocket connectivity (CRITICAL)
3. **03-join-game.spec.ts** - Multi-player lobby functionality
4. **04-bidding-phase.spec.ts** - Bidding mechanics
5. **08-multi-client.spec.ts** - Real-time synchronization (CRITICAL)

### Backend Integration Tests

Located in `server/src/test/java/`:
- **GameRegistryTest.java** - Game registry functionality
- Additional integration tests can be added as needed

## Writing New Tests

### E2E Test Template

```typescript
import { test, expect } from '@playwright/test';
import { createGame, joinGame } from '../fixtures/test-helpers';

test.describe('My Feature', () => {
  test('does something', async ({ page }) => {
    await page.goto('/');
    // ... test code ...
    expect(await page.textContent('#selector')).toBe('expected');
  });
});
```

### Backend Test Template

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class MyFeatureIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void testSomething() throws Exception {
        mockMvc.perform(get("/api/endpoint"))
            .andExpect(status().isOk());
    }
}
```

## Debugging Failed Tests

### Playwright

1. Check screenshots: `playwright-report/`
2. View trace: `npx playwright show-trace trace.zip`
3. Run in headed mode: `npm run test:headed`
4. Use debug mode: `npm run test:debug`

### Backend

1. Check test reports: `server/build/reports/tests/test/index.html`
2. Enable debug logging in `application.properties`
3. Use `@Disabled` to skip flaky tests temporarily

## CI/CD

Tests run automatically on every pull request:
- Backend tests: ~2-3 minutes
- E2E tests: ~4-5 minutes
- Total: ~5-7 minutes (parallel execution)

## Test Environment

- **Production**: https://ddz-game.fly.dev/
- **Staging**: https://ddz-game-staging.fly.dev/
- **Local**: http://localhost:8080

Default: Staging (override with TEST_URL env var)

## Test Helpers

Common test utilities are located in `tests/fixtures/test-helpers.ts`:

- `createGame(page, playerName, playerCount)` - Create a new game
- `joinGame(page, joinCode, playerName)` - Join existing game
- `waitForWebSocketConnection(page)` - Wait for WS to connect
- `waitForPhase(page, phase)` - Wait for specific game phase
- `startGame(page)` - Start the game
- `placeBid(page, bidValue)` - Place a bid during bidding phase
- `getCurrentPhase(page)` - Get current game phase
- `getPlayerCount(page)` - Get player count (e.g., "3/5")

## Troubleshooting

### WebSocket Connection Issues

If tests fail with WebSocket errors:
1. Verify staging is deployed: `flyctl status --app ddz-game-staging`
2. Check staging logs: `flyctl logs --app ddz-game-staging`
3. Test manually: visit https://ddz-game-staging.fly.dev/

### Test Timeouts

If tests timeout:
1. Increase timeout in `playwright.config.ts`
2. Check if staging server is slow
3. Run with `--headed` to see what's happening

### Flaky Tests

If tests are flaky:
1. Add more explicit waits (use `waitForFunction`)
2. Increase WebSocket sync time
3. Check for race conditions in multi-client tests
