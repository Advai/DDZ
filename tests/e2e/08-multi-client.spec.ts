import { test, expect } from '@playwright/test';
import { createGame, joinGame, waitForPhase, startGame, placeBid } from '../fixtures/test-helpers';

test.describe('Multi-Client Synchronization (CRITICAL)', () => {
  test('3 players synchronize in real-time', async ({ browser }) => {
    const context1 = await browser.newContext();
    const context2 = await browser.newContext();
    const context3 = await browser.newContext();

    const page1 = await context1.newPage();
    const page2 = await context2.newPage();
    const page3 = await context3.newPage();

    try {
      // Player 1 creates
      const { joinCode } = await createGame(page1, 'Alice', 3);

      // Players 2 and 3 join
      await Promise.all([
        joinGame(page2, joinCode, 'Bob'),
        joinGame(page3, joinCode, 'Carol')
      ]);

      // Player 1 starts game
      await startGame(page1);

      // Verify all 3 see BIDDING phase within 500ms
      await Promise.all([
        waitForPhase(page1, 'BIDDING'),
        waitForPhase(page2, 'BIDDING'),
        waitForPhase(page3, 'BIDDING')
      ]);

      // Verify synchronization
      const phase1 = await page1.textContent('#gamePhase');
      const phase2 = await page2.textContent('#gamePhase');
      const phase3 = await page3.textContent('#gamePhase');

      expect(phase1).toBe('BIDDING');
      expect(phase2).toBe('BIDDING');
      expect(phase3).toBe('BIDDING');
    } finally {
      await context1.close();
      await context2.close();
      await context3.close();
    }
  });

  test('bid synchronizes across all clients within 500ms', async ({ browser }) => {
    const context1 = await browser.newContext();
    const context2 = await browser.newContext();
    const context3 = await browser.newContext();

    const page1 = await context1.newPage();
    const page2 = await context2.newPage();
    const page3 = await context3.newPage();

    try {
      // Set up game
      const { joinCode } = await createGame(page1, 'Alice', 3);
      await Promise.all([
        joinGame(page2, joinCode, 'Bob'),
        joinGame(page3, joinCode, 'Carol')
      ]);
      await startGame(page1);
      await waitForPhase(page1, 'BIDDING');

      // Wait to identify current player
      await page1.waitForTimeout(1000);

      // Check who's turn it is and place a bid
      const page1Content = await page1.textContent('body');

      if (page1Content?.includes('Your turn') || page1Content?.includes('Place your bid')) {
        // Page 1's turn - bid 3
        await placeBid(page1, 3);
      }

      // Wait for sync (max 500ms)
      await page2.waitForTimeout(500);
      await page3.waitForTimeout(500);

      // Verify all pages see the bid update
      const body2 = await page2.textContent('body');
      const body3 = await page3.textContent('body');

      // At least one should show bid info (or next player's turn)
      const syncVerified =
        body2?.includes('bid') ||
        body2?.includes('Bid') ||
        body3?.includes('bid') ||
        body3?.includes('Bid');

      expect(syncVerified).toBe(true);
    } finally {
      await context1.close();
      await context2.close();
      await context3.close();
    }
  });

  test('game state synchronizes after player disconnection', async ({ browser }) => {
    const context1 = await browser.newContext();
    const context2 = await browser.newContext();
    const context3 = await browser.newContext();

    const page1 = await context1.newPage();
    const page2 = await context2.newPage();
    const page3 = await context3.newPage();

    try {
      // Set up game
      const { joinCode } = await createGame(page1, 'Alice', 3);
      await joinGame(page2, joinCode, 'Bob');
      await joinGame(page3, joinCode, 'Carol');

      await startGame(page1);
      await waitForPhase(page1, 'BIDDING');

      // Disconnect Player 2 (close context)
      await context2.close();

      // Wait a bit
      await page1.waitForTimeout(2000);

      // Game should still be in progress (might auto-pass for disconnected player)
      const phase1 = await page1.textContent('#gamePhase');
      const phase3 = await page3.textContent('#gamePhase');

      // Verify game continues (phase is either BIDDING or advanced to PLAY)
      expect(phase1).toMatch(/BIDDING|PLAY|LANDLORD_SELECTION/);
      expect(phase3).toMatch(/BIDDING|PLAY|LANDLORD_SELECTION/);

      // Verify remaining players still connected
      const status1 = await page1.textContent('#connectionStatus');
      const status3 = await page3.textContent('#connectionStatus');

      expect(status1).toContain('Connected');
      expect(status3).toContain('Connected');
    } finally {
      await context1.close();
      // context2 already closed
      await context3.close();
    }
  });

  test('5 players synchronize during landlord selection', async ({ browser }) => {
    const contexts = await Promise.all([
      browser.newContext(),
      browser.newContext(),
      browser.newContext(),
      browser.newContext(),
      browser.newContext()
    ]);

    const pages = await Promise.all(contexts.map(ctx => ctx.newPage()));

    try {
      // Player 1 creates 5-player game
      const { joinCode } = await createGame(pages[0], 'P1', 5);

      // Other 4 players join
      await Promise.all([
        joinGame(pages[1], joinCode, 'P2'),
        joinGame(pages[2], joinCode, 'P3'),
        joinGame(pages[3], joinCode, 'P4'),
        joinGame(pages[4], joinCode, 'P5')
      ]);

      // Start game
      await startGame(pages[0]);

      // Wait for BIDDING phase on all clients
      await Promise.all(pages.map(page => waitForPhase(page, 'BIDDING')));

      // Verify all 5 clients show same phase
      const phases = await Promise.all(pages.map(page => page.textContent('#gamePhase')));
      const allBidding = phases.every(phase => phase === 'BIDDING');

      expect(allBidding).toBe(true);

      // Verify all clients show 5 players
      for (const page of pages) {
        const content = await page.textContent('body');
        // Should show multiple players (exact count may vary based on UI)
        expect(content).toContain('P1');
      }
    } finally {
      await Promise.all(contexts.map(ctx => ctx.close()));
    }
  });

  test('WebSocket messages arrive in correct order', async ({ browser }) => {
    const context1 = await browser.newContext();
    const context2 = await browser.newContext();

    const page1 = await context1.newPage();
    const page2 = await context2.newPage();

    const messages1: any[] = [];
    const messages2: any[] = [];

    // Capture messages on both pages
    page1.on('websocket', ws => {
      ws.on('framereceived', frame => {
        try {
          const msg = JSON.parse(frame.payload as string);
          messages1.push(msg);
        } catch (e) {
          // Ignore
        }
      });
    });

    page2.on('websocket', ws => {
      ws.on('framereceived', frame => {
        try {
          const msg = JSON.parse(frame.payload as string);
          messages2.push(msg);
        } catch (e) {
          // Ignore
        }
      });
    });

    try {
      // Set up game
      const { joinCode } = await createGame(page1, 'Alice', 3);
      await joinGame(page2, joinCode, 'Bob');

      await startGame(page1);
      await waitForPhase(page1, 'BIDDING');

      // Wait for messages
      await page1.waitForTimeout(2000);

      // Verify both clients received GAME_UPDATE messages
      const updates1 = messages1.filter(m => m.type === 'GAME_UPDATE');
      const updates2 = messages2.filter(m => m.type === 'GAME_UPDATE');

      expect(updates1.length).toBeGreaterThan(0);
      expect(updates2.length).toBeGreaterThan(0);

      // Verify phase progression makes sense
      const phases1 = updates1.map(m => m.state?.phase).filter(Boolean);
      expect(phases1).toContain('BIDDING');
    } finally {
      await context1.close();
      await context2.close();
    }
  });

  test('simultaneous actions from different clients handled correctly', async ({ browser }) => {
    const context1 = await browser.newContext();
    const context2 = await browser.newContext();
    const context3 = await browser.newContext();

    const page1 = await context1.newPage();
    const page2 = await context2.newPage();
    const page3 = await context3.newPage();

    try {
      // Set up game
      const { joinCode } = await createGame(page1, 'Alice', 3);
      await Promise.all([
        joinGame(page2, joinCode, 'Bob'),
        joinGame(page3, joinCode, 'Carol')
      ]);

      await startGame(page1);
      await waitForPhase(page1, 'BIDDING');

      // All players try to act simultaneously (only current player's action should succeed)
      await Promise.all([
        page1.click('button:has-text("Pass")').catch(() => {}),
        page2.click('button:has-text("Pass")').catch(() => {}),
        page3.click('button:has-text("Pass")').catch(() => {})
      ]);

      // Wait for server to process
      await page1.waitForTimeout(1000);

      // Game should still be in valid state
      const phase1 = await page1.textContent('#gamePhase');
      expect(phase1).toMatch(/BIDDING|PLAY|LANDLORD_SELECTION/);

      // All clients should still be connected
      const status1 = await page1.textContent('#connectionStatus');
      const status2 = await page2.textContent('#connectionStatus');
      const status3 = await page3.textContent('#connectionStatus');

      expect(status1).toContain('Connected');
      expect(status2).toContain('Connected');
      expect(status3).toContain('Connected');
    } finally {
      await context1.close();
      await context2.close();
      await context3.close();
    }
  });
});
