import { Page, expect } from '@playwright/test';

/**
 * Wait for WebSocket connection to establish
 */
export async function waitForWebSocketConnection(page: Page): Promise<void> {
  // Wait for connection status to show "Connected"
  await page.waitForFunction(
    () => {
      const status = document.getElementById('connectionStatus');
      return status?.textContent?.includes('Connected');
    },
    { timeout: 15000 }
  );
}

/**
 * Create a new game via UI
 */
export async function createGame(
  page: Page,
  playerName: string,
  playerCount: number
): Promise<{ gameId: string; joinCode: string }> {
  await page.goto('/');

  // Fill in player name
  await page.fill('#playerName', playerName);

  // Select player count
  await page.selectOption('#playerCount', playerCount.toString());

  // Click create game button
  await page.click('button:has-text("Create Game")');

  // Wait for WebSocket connection
  await waitForWebSocketConnection(page);

  // Get game ID and join code
  const gameId = await page.textContent('#gameId');

  // Get join code from info box (need to find it in the UI)
  const joinCodeText = await page.textContent('.info-box:has-text("Join Code")');
  const joinCodeMatch = joinCodeText?.match(/📋 JOIN CODE:\s*([A-Z0-9]+)/);
  const joinCode = joinCodeMatch ? joinCodeMatch[1] : '';

  return { gameId: gameId || '', joinCode };
}

/**
 * Join an existing game via join code
 */
export async function joinGame(
  page: Page,
  joinCode: string,
  playerName: string
): Promise<void> {
  await page.goto('/');

  // Fill in player name
  await page.fill('#playerName', playerName);

  // Fill in join code
  await page.fill('#joinCode', joinCode);

  // Click join game button
  await page.click('button:has-text("Join Game")');

  // Wait for WebSocket connection
  await waitForWebSocketConnection(page);
}

/**
 * Wait for game to transition to specific phase
 */
export async function waitForPhase(page: Page, phase: string): Promise<void> {
  await page.waitForFunction(
    (expectedPhase) => {
      const el = document.getElementById('gamePhase');
      return el?.textContent === expectedPhase;
    },
    phase,
    { timeout: 10000 }
  );
}

/**
 * Start game (must be in LOBBY phase)
 */
export async function startGame(page: Page): Promise<void> {
  await page.click('#startGameBtn');
  // Wait for transition to bidding or landlord selection phase
  await page.waitForFunction(
    () => {
      const phase = document.getElementById('gamePhase')?.textContent;
      return phase === 'BIDDING' || phase === 'LANDLORD_SELECTION';
    },
    { timeout: 10000 }
  );
}

/**
 * Place a bid (0 = pass)
 */
export async function placeBid(page: Page, bidValue: number): Promise<void> {
  // Find and click the bid button
  await page.click(`button:has-text("${bidValue === 0 ? 'Pass' : 'Bid ' + bidValue}")`);

  // Allow WebSocket message to propagate
  await page.waitForTimeout(500);
}

/**
 * Get current game phase
 */
export async function getCurrentPhase(page: Page): Promise<string> {
  const phase = await page.textContent('#gamePhase');
  return phase || '';
}

/**
 * Wait for specific text to appear in element
 */
export async function waitForText(
  page: Page,
  selector: string,
  expectedText: string,
  timeout: number = 10000
): Promise<void> {
  await page.waitForFunction(
    ({ sel, text }) => {
      const el = document.querySelector(sel);
      return el?.textContent?.includes(text);
    },
    { sel: selector, text: expectedText },
    { timeout }
  );
}

/**
 * Check if element exists
 */
export async function elementExists(page: Page, selector: string): Promise<boolean> {
  try {
    await page.waitForSelector(selector, { timeout: 1000 });
    return true;
  } catch {
    return false;
  }
}

/**
 * Get player count from UI
 */
export async function getPlayerCount(page: Page): Promise<string> {
  const count = await page.locator('#playersList li').count();
  // Return format like "3" (just the count, since there's no total displayed)
  return count.toString();
}
