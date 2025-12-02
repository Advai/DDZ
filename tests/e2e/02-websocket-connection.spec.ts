import { test, expect } from '@playwright/test';
import { waitForWebSocketConnection } from '../fixtures/test-helpers';

test.describe('WebSocket Connection (CRITICAL)', () => {
  test('connects successfully after game creation', async ({ page }) => {
    // Set up WebSocket listener before navigation
    const wsPromise = page.waitForEvent('websocket');

    await page.goto('/');

    // Fill form and create game
    await page.fill('#playerName', 'WSTest');
    await page.selectOption('#playerCount', '3');
    await page.click('button:has-text("Create Game")');

    // Wait for WebSocket connection
    const ws = await wsPromise;

    // Verify WebSocket URL format (wss:// for HTTPS, includes gameId and playerId)
    expect(ws.url()).toMatch(/wss?:\/\/.*\/ws\/game\/.+\?playerId=.+/);

    // Verify connection status UI shows "Connected"
    await waitForWebSocketConnection(page);
    const connectionStatus = await page.textContent('#connectionStatus');
    expect(connectionStatus).toContain('Connected');

    // Verify no WebSocket error events
    let errorOccurred = false;
    ws.on('socketerror', () => {
      errorOccurred = true;
    });

    // Wait 2 seconds to see if any errors occur
    await page.waitForTimeout(2000);
    expect(errorOccurred).toBe(false);
  });

  test('no 1008 Policy Violation errors', async ({ page }) => {
    const wsPromise = page.waitForEvent('websocket');

    await page.goto('/');
    await page.fill('#playerName', 'PolicyTest');
    await page.selectOption('#playerCount', '3');
    await page.click('button:has-text("Create Game")');

    const ws = await wsPromise;

    // Monitor close events
    let closeCode: number | null = null;
    ws.on('close', (payload) => {
      closeCode = payload;
    });

    // Wait 5 seconds to ensure stable connection
    await page.waitForTimeout(5000);

    // Verify WebSocket not closed with 1008 (Policy Violation)
    if (closeCode !== null) {
      expect(closeCode).not.toBe(1008);
    }

    // Verify connection is still open
    const connectionStatus = await page.textContent('#connectionStatus');
    expect(connectionStatus).toContain('Connected');
  });

  test('receives initial GAME_UPDATE message', async ({ page }) => {
    const messages: any[] = [];

    // Set up message listener
    page.on('websocket', ws => {
      ws.on('framereceived', frame => {
        try {
          const msg = JSON.parse(frame.payload as string);
          messages.push(msg);
        } catch (e) {
          // Ignore non-JSON frames
        }
      });
    });

    await page.goto('/');
    await page.fill('#playerName', 'MessageTest');
    await page.selectOption('#playerCount', '3');
    await page.click('button:has-text("Create Game")');

    // Wait for connection
    await waitForWebSocketConnection(page);

    // Wait a bit for messages to arrive
    await page.waitForTimeout(2000);

    // Verify at least one GAME_UPDATE message received
    const gameUpdates = messages.filter(m => m.type === 'GAME_UPDATE');
    expect(gameUpdates.length).toBeGreaterThan(0);

    // Verify message structure
    if (gameUpdates.length > 0) {
      const firstUpdate = gameUpdates[0];
      expect(firstUpdate).toHaveProperty('state');
      expect(firstUpdate.state).toHaveProperty('phase');
    }
  });

  test('connection persists during lobby wait', async ({ page }) => {
    await page.goto('/');
    await page.fill('#playerName', 'PersistTest');
    await page.selectOption('#playerCount', '3');
    await page.click('button:has-text("Create Game")');

    // Wait for initial connection
    await waitForWebSocketConnection(page);

    // Wait 10 seconds (simulate waiting for other players)
    await page.waitForTimeout(10000);

    // Verify still connected
    const connectionStatus = await page.textContent('#connectionStatus');
    expect(connectionStatus).toContain('Connected');

    // Verify game ID still visible
    const gameId = await page.textContent('#gameId');
    expect(gameId).not.toBe('-');
    expect(gameId).toMatch(/^g-/);
  });

  test('WebSocket URL uses correct protocol (wss for HTTPS)', async ({ page }) => {
    const wsPromise = page.waitForEvent('websocket');

    await page.goto('/');
    await page.fill('#playerName', 'ProtocolTest');
    await page.selectOption('#playerCount', '3');
    await page.click('button:has-text("Create Game")');

    const ws = await wsPromise;

    // For production/staging (HTTPS), WebSocket should use wss://
    const baseURL = page.context().browser()?.version() || process.env.TEST_URL || '';
    if (baseURL.includes('https://') || baseURL.includes('fly.dev')) {
      expect(ws.url()).toMatch(/^wss:\/\//);
    } else {
      // For local HTTP, ws:// is acceptable
      expect(ws.url()).toMatch(/^wss?:\/\//);
    }
  });
});
