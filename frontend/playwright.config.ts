import { defineConfig, devices } from '@playwright/test';

/**
 * §G6.4 — E2E des parcours critiques (web). La stack locale (API :8080 +
 * comptes seedés DataInitializer) est utilisée ; en CI, le workflow provisionne
 * Postgres/Redis + backend, et Playwright démarre le front lui-même.
 *
 * Variables :
 *  - E2E_BASE (défaut http://localhost:5173)
 *  - E2E_API  (défaut http://localhost:8080/api/v1)
 */
const BASE = process.env.E2E_BASE || 'http://localhost:5173';

export default defineConfig({
  testDir: './e2e',
  timeout: 60_000,
  workers: 1, // parcours sériels : ils partagent la base de démo locale
  retries: 0,
  reporter: [['list'], ['json', { outputFile: 'e2e/results.json' }]],
  use: {
    baseURL: BASE,
    headless: true,
    trace: 'retain-on-failure',
    // §G6.4 : canal « chromium » = build complet (headless=new), contrairement au
    // headless shell séparé — évite un second téléchargement de navigateur.
    channel: 'chromium',
  },
  projects: [
    { name: 'chromium', use: { ...devices['Desktop Chrome'] } },
  ],
  // En local la stack est déjà démarrée ; en CI on laisse le workflow s'occuper
  // de l'API et Playwright du front (webServer ci-dessous, réutilisé si vivant).
  webServer: process.env.E2E_SKIP_WEBSERVER
    ? undefined
    : {
        command: 'npm run dev -- --port 5173 --strictPort',
        url: BASE,
        reuseExistingServer: true,
        timeout: 120_000,
      },
});
