import {defineConfig, devices} from '@playwright/test';

export default defineConfig({
  testDir: './test',
  fullyParallel: false,
  workers: 1,
  forbidOnly: Boolean(process.env.CI),
  retries: process.env.CI ? 1 : 0,
  timeout: 30_000,
  expect: {
    timeout: 5_000,
  },
  reporter: process.env.CI
    ? [['line'], ['junit', {outputFile: 'output/results.xml'}], ['html', {outputFolder: 'playwright-report', open: 'never'}]]
    : [['list'], ['html', {outputFolder: 'playwright-report', open: 'never'}]],
  use: {
    baseURL: process.env.BASE_URL ?? 'http://127.0.0.1:58080',
    ...devices['Desktop Chrome'],
    screenshot: 'only-on-failure',
    trace: 'retain-on-failure',
    video: 'retain-on-failure',
  },
  outputDir: 'output/artifacts',
});
