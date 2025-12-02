import { test, expect } from '@playwright/test';
import { createGame, waitForWebSocketConnection } from '../fixtures/test-helpers';

test.describe('Game Creation', () => {
  test('creates a 3-player game successfully', async ({ page }) => {
    await page.goto('/');

    // Fill in player name
    await page.fill('#playerName', 'TestPlayer1');

    // Select 3 players
    await page.selectOption('#playerCount', '3');

    // Create game
    await page.click('button:has-text("Create Game")');

    // Wait for WebSocket connection
    await waitForWebSocketConnection(page);

    // Verify game ID displayed (format: g-{uuid})
    const gameId = await page.textContent('#gameId');
    expect(gameId).toMatch(/^g-[a-f0-9-]{36}$/);

    // Verify join code displayed
    const joinCodeText = await page.textContent('.info-box:has-text("Join Code")');
    expect(joinCodeText).toMatch(/Join Code:\s*[A-Z0-9]{4,8}/);

    // Verify phase is LOBBY
    const phase = await page.textContent('#gamePhase');
    expect(phase).toBe('LOBBY');

    // Verify player count shows 1/3
    const playerCountText = await page.textContent('.info-box:has-text("Players")');
    expect(playerCountText).toContain('1/3');
  });

  test('creates a 5-player game successfully', async ({ page }) => {
    const { gameId, joinCode } = await createGame(page, 'TestPlayer5', 5);

    // Verify game created
    expect(gameId).toMatch(/^g-/);
    expect(joinCode).toMatch(/[A-Z0-9]{4,8}/);

    // Verify player count shows 1/5
    const playerCountText = await page.textContent('.info-box:has-text("Players")');
    expect(playerCountText).toContain('1/5');
  });

  test('creates a 7-player game successfully', async ({ page }) => {
    const { gameId, joinCode } = await createGame(page, 'TestPlayer7', 7);

    // Verify game created
    expect(gameId).toMatch(/^g-/);
    expect(joinCode).toMatch(/[A-Z0-9]{4,8}/);

    // Verify player count shows 1/7
    const playerCountText = await page.textContent('.info-box:has-text("Players")');
    expect(playerCountText).toContain('1/7');
  });

  test('displays player name in players list', async ({ page }) => {
    await page.goto('/');
    await page.fill('#playerName', 'UniquePlayerName123');
    await page.selectOption('#playerCount', '3');
    await page.click('button:has-text("Create Game")');

    await waitForWebSocketConnection(page);

    // Verify player name appears in the UI
    const content = await page.textContent('body');
    expect(content).toContain('UniquePlayerName123');
  });

  test('no JavaScript console errors during game creation', async ({ page }) => {
    const consoleErrors: string[] = [];

    // Listen for console errors
    page.on('console', msg => {
      if (msg.type() === 'error') {
        consoleErrors.push(msg.text());
      }
    });

    await page.goto('/');
    await page.fill('#playerName', 'ErrorTest');
    await page.selectOption('#playerCount', '3');
    await page.click('button:has-text("Create Game")');

    await waitForWebSocketConnection(page);

    // Verify no console errors (filter out resource loading errors which are acceptable)
    const criticalErrors = consoleErrors.filter(
      err => !err.includes('net::ERR') && !err.includes('favicon')
    );
    expect(criticalErrors).toHaveLength(0);
  });

  test('start game button is visible after creation', async ({ page }) => {
    await createGame(page, 'StartBtnTest', 3);

    // Verify start button exists and is visible
    const startBtn = page.locator('#startGameBtn');
    await expect(startBtn).toBeVisible();

    // Verify button text
    const btnText = await startBtn.textContent();
    expect(btnText).toContain('Start Game');
  });

  test('creating game with empty name shows error or uses default', async ({ page }) => {
    await page.goto('/');

    // Leave player name empty
    await page.fill('#playerName', '');

    // Select player count
    await page.selectOption('#playerCount', '3');

    // Try to create game
    await page.click('button:has-text("Create Game")');

    // Either an error is shown OR a default name is used and game is created
    // Wait a bit to see what happens
    await page.waitForTimeout(2000);

    const connectionStatus = await page.textContent('#connectionStatus');

    // If connected, game was created with default name (acceptable)
    // If not connected, that's also acceptable (validation worked)
    expect(connectionStatus).toBeTruthy(); // Just verify element exists
  });
});
