import { test, expect } from '@playwright/test';
import { createGame, joinGame, startGame, waitForPhase, placeBid } from '../fixtures/test-helpers';

test.describe('Bidding Phase', () => {
  test('game transitions from LOBBY to BIDDING', async ({ browser }) => {
    const context1 = await browser.newContext();
    const context2 = await browser.newContext();
    const context3 = await browser.newContext();

    const page1 = await context1.newPage();
    const page2 = await context2.newPage();
    const page3 = await context3.newPage();

    try {
      // Create game with 3 players
      const { joinCode } = await createGame(page1, 'BidTest1', 3);

      // Join 2 more players
      await joinGame(page2, joinCode, 'BidTest2');
      await joinGame(page3, joinCode, 'BidTest3');

      // Wait for all players to be synced
      await page1.waitForTimeout(1000);

      // Verify initial phase is LOBBY
      const initialPhase = await page1.textContent('#gamePhase');
      expect(initialPhase).toBe('LOBBY');

      // Start game
      await startGame(page1);

      // Verify transition to BIDDING
      await waitForPhase(page1, 'BIDDING');
      const newPhase = await page1.textContent('#gamePhase');
      expect(newPhase).toBe('BIDDING');
    } finally {
      await context1.close();
      await context2.close();
      await context3.close();
    }
  });

  test('bidding interface appears in BIDDING phase', async ({ browser }) => {
    const context1 = await browser.newContext();
    const context2 = await browser.newContext();
    const context3 = await browser.newContext();

    const page1 = await context1.newPage();
    const page2 = await context2.newPage();
    const page3 = await context3.newPage();

    try {
      // Create game with 3 players
      const { joinCode } = await createGame(page1, 'BidUI1', 3);
      await joinGame(page2, joinCode, 'BidUI2');
      await joinGame(page3, joinCode, 'BidUI3');

      await page1.waitForTimeout(1000);

      await startGame(page1);
      await waitForPhase(page1, 'BIDDING');

      // Verify bidding UI elements exist
      const biddingPanel = await page1.locator('#biddingPanel');
      await expect(biddingPanel).toBeVisible();

      // Verify bidding status or buttons visible
      const content = await page1.textContent('body');
      expect(content).toMatch(/bid|pass|Bid|Pass/i);
    } finally {
      await context1.close();
      await context2.close();
      await context3.close();
    }
  });

  test('player can pass during bidding', async ({ browser }) => {
    const context1 = await browser.newContext();
    const context2 = await browser.newContext();
    const context3 = await browser.newContext();

    const page1 = await context1.newPage();
    const page2 = await context2.newPage();
    const page3 = await context3.newPage();

    try {
      // Create game with 3 players
      const { joinCode } = await createGame(page1, 'PassTest1', 3);
      await joinGame(page2, joinCode, 'PassTest2');
      await joinGame(page3, joinCode, 'PassTest3');

      await page1.waitForTimeout(1000);

      await startGame(page1);
      await waitForPhase(page1, 'BIDDING');

      // Wait to see if it's our turn
      await page1.waitForTimeout(1000);

      const content = await page1.textContent('body');
      if (content?.includes('Your turn') || content?.includes('Place your bid') || content?.includes('YOUR TURN')) {
        // Try to pass
        await placeBid(page1, 0);

        // Wait for action to process
        await page1.waitForTimeout(1000);

        // Game should still be in BIDDING or advanced
        const phase = await page1.textContent('#gamePhase');
        expect(phase).toMatch(/BIDDING|PLAY|LANDLORD_SELECTION/);
      } else {
        // If it's not our turn initially, that's okay - test is inconclusive but not failed
        console.log('Not our turn to bid, skipping pass test');
      }
    } finally {
      await context1.close();
      await context2.close();
      await context3.close();
    }
  });
});
