# Suite E2E Playwright — `e2/`

Périmètre **orchestration A5 #4** : cette suite est volontairement hors du
frontend (`frontend/**` n'est jamais touché). Elle ne parle qu'à une URL HTTP
déployée via `baseURL` (voir `playwright.config.ts`).

## Activation automatique

Le workflow [`.github/workflows/e2e.yml`](../.github/workflows/e2e.yml) :

1. détecte les fichiers `specs/**/*.spec.ts` ;
2. **aucune spec** → le job passe en mode « attente » (notice, pas d'échec
   mensonger) ;
3. **première spec déposée** (par Agent B ou n'importe qui) → elle est
   exécutée immédiatement, sans aucune modification de CI à prévoir.

## Écrire une spec

Déposer un fichier dans `specs/`, par exemple `specs/login.spec.ts` :

```ts
import { test, expect } from '@playwright/test';

test('connexion tenant admin', async ({ page }) => {
  await page.goto('/login');
  // ...
});
```

## Exécution locale

```bash
cd e2
npm ci            # ou npm install
npx playwright install --with-deps chromium
E2E_BASE_URL=https://discipolat-beta.onrender.com npx playwright test
```

Variables d'environnement :

| Variable        | Défaut                                     | Rôle                       |
|-----------------|--------------------------------------------|----------------------------|
| `E2E_BASE_URL`  | `https://discipolat-beta.onrender.com`     | cible de la suite          |
