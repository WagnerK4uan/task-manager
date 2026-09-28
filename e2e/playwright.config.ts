import { defineConfig, devices } from '@playwright/test';

const porta = process.env.FRONTEND_PORT ?? '4200';

export default defineConfig({
  testDir: './tests',
  workers: 1,
  fullyParallel: false,
  retries: 0,
  timeout: 30_000,
  expect: { timeout: 10_000 },
  outputDir: 'results/artefatos',
  reporter: [
    ['list'],
    ['junit', { outputFile: 'results/TEST-e2e.xml' }],
  ],
  use: {
    baseURL: process.env.E2E_BASE_URL ?? `http://localhost:${porta}`,
    trace: 'off',
    screenshot: 'off',
    video: 'off',
  },
  projects: [{ name: 'chromium', use: { ...devices['Desktop Chrome'] } }],
});
