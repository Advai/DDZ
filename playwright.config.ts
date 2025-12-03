import { defineConfig, devices } from '@playwright/test';

export default defineConfig({
  testDir: './tests/e2e',

  timeout: 60 * 1000, // 60 seconds per test

  expect: {
    timeout: 10000 // 10 seconds for assertions
  },

  // Run tests sequentially (avoid WebSocket conflicts)
  fullyParallel: false,
  workers: 1,

  // Fail fast on CI
  forbidOnly: !!process.env.CI,

  // Retry failed tests on CI
  retries: process.env.CI ? 2 : 0,

  reporter: [
    ['html', { outputFolder: 'playwright-report' }],
    ['list'],
    ['github'] // GitHub Actions annotations
  ],

  use: {
    // Test against staging by default, allow override
    baseURL: process.env.TEST_URL || 'https://ddz-game-staging.fly.dev',

    screenshot: 'only-on-failure',
    video: 'retain-on-failure',
    trace: 'on-first-retry',

    viewport: { width: 1280, height: 720 },
  },

  projects: [
    {
      name: 'chromium',
      use: { ...devices['Desktop Chrome'] },
    },
  ],
});
