import { defineConfig, devices } from '@playwright/test';

/**
 * A5 #4 — Configuration Playwright racine (« e2/ »).
 *
 * Principe : zéro dépendance au code du frontend (ni src/, ni vite, ni son
 * node_modules). La suite ne parle qu'à une URL HTTP déployée, ce qui la
 * rend valide sur n'importe quel environnement : beta, staging, preview.
 *
 * Activation : le workflow .github/workflows/e2e.yml exécute automatiquement
 * tous les fichiers `specs/**/*.spec.ts` dès qu'ils existent. Le jour où
 * Agent B livre la suite, elle tourne sans qu'aucun fichier de CI ne change.
 */
export default defineConfig({
  testDir: './specs',
  fullyParallel: true,
  forbidOnly: !!process.env.CI,
  retries: process.env.CI ? 2 : 0,
  reporter: [
    ['list'],
    ['html', { open: 'never' }],
    ['junit', { outputFile: 'results/junit.xml' }],
  ],
  use: {
    // Environnement cible : surchargeable sans toucher au dépôt.
    baseURL: process.env.E2E_BASE_URL ?? 'https://discipolat-beta.onrender.com',
    trace: 'on-first-retry',
    screenshot: 'only-on-failure',
  },
  projects: [
    { name: 'chromium', use: { ...devices['Desktop Chrome'] } },
  ],
});
