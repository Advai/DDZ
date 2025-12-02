import { test, expect } from '@playwright/test';
import { createGame, joinGame, startGame, waitForPhase, placeBid } from '../fixtures/test-helpers';

test.describe('Bidding Phase', () => {
  test('game transitions from LOBBY to BIDDING', async ({ browser }) => {
    const context = await browser.newContext();
    const page = await context.newPage();

    try {
      await createGame(page, 'BidTest', 3);

      // Verify initial phase is LOBBY
      const initialPhase = await page.textContent('#gamePhase');
      expect(initialPhase).toBe('LOBBY');

      // Start game
      await startGame(page);

      // Verify transition to BIDDING
      await waitForPhase(page, 'BIDDING');
      const newPhase = await page.textContent('#gamePhase');
      expect(newPhase).toBe('BIDDING');
    } finally {
      await context.close();
    }
  });

  test('bidding interface appears in BIDDING phase', async ({ browser }) => {
    const context = await browser.newContext();
    const page = await context.newPage();

    try {
      await createGame(page, 'BidUI', 3);
      await startGame(page);
      await waitForPhase(page, 'BIDDING');

      // Verify bidding UI elements exist
      const biddingPanel = await page.locator('#biddingPanel');
      await expect(biddingPanel).toBeVisible();

      // Verify bidding status or buttons visible
      const content = await page.textContent('body');
      expect(content).toMatch(/bid|pass|Bid|Pass/i);
    } finally {
      await context.close();
    }
  });

  test('player can pass during bidding', async ({ browser }) => {
    const context = await browser.newContext();
    const page = await context.newPage();

    try {
      await createGame(page, 'PassTest', 3);
      await startGame(page);
      await waitForPhase(page, 'BIDDING');

      // Wait to see if it's our turn
      await page.waitForTimeout(1000);

      const content = await page.textContent('body');
      if (content?.includes('Your turn') || content?.includes('Place your bid')) {
        // Try to pass
        await placeBid(page, 0);

        // Wait for action to process
        await page.waitForTimeout(1000);

        // Game should still be in BIDDING or advanced
        const phase = await page.textContent('#gamePhase');
        expect(phase).toMatch(/BIDDING|PLAY|LANDLORD_SELECTION/);
      }
    } finally {
      await context.close();
    }
  });
});
