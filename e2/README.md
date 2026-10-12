# Suite E2E Playwright — `e2/`

Périmètre **orchestration A5 #4** : cette suite est volontairement hors du
frontend (`frontend/**` n'est jamais touché). Elle ne parle qu'à une URL HTTP,
`baseURL` (voir `playwright.config.ts`) — déployée, ou construite et servie par
le workflow de CI.

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

Deux cibles possibles, et c'est tout :

**a) l'environnement déployé** (celui pour lequel la suite a été écrite) :

```bash
cd e2
npm ci                      # ou npm install
npx playwright install --with-deps chromium
E2E_BASE_URL=https://<environnement-déployé> npx playwright test
```

**b) le build du commit, servi en local** — c'est ce que fait `e2e.yml`. Le SPA a
besoin du contrat « tarifs », sinon `usePricing` reste sur « Chargement du
catalogue… » et trois specs deviennent impossible à satisfaire (mesuré le
2026-10-12 : 12 passed / 3 failed). Un stub fournit ce contrat, hors ligne :

```bash
cd e2
npm run stub:pricing &                          # http://127.0.0.1:4180
(cd ../frontend && VITE_API_URL=http://127.0.0.1:4180 npm run build \
  && npx vite preview --host 127.0.0.1 --port 4173)
E2E_BASE_URL=http://127.0.0.1:4173 npx playwright test
```

Variables d'environnement :

| Variable                   | Défaut                      | Rôle                                            |
|----------------------------|-----------------------------|-------------------------------------------------|
| `E2E_BASE_URL`             | `http://127.0.0.1:4173`     | cible de la suite (build local du commit)       |
| `PRICING_STUB_PORT`        | `4180`                      | port du contrat tarifs                           |
| `PRICING_STUB_FREE_PLAN`   | absent                      | `1` = réintroduit DISCOVERY à 0 FCFA (KNOWN_ISSUES E9) |

## Ce que la suite prouve, et ce qu'elle ne prouve pas

Le stub de `support/pricing-stub.mjs` reproduit le contrat publié
(`docs/API_TARIFS.md`) avec les valeurs posées par les migrations
V144/V177/V246/V247. Ce n'est **pas** le backend : la résolution réelle
(fallback de devise, canaux réellement disponibles, prix régionaux) n'est pas
exercée ici. Portée et limites tracées dans `KNOWN_ISSUES.md` (E6, E9, E10).
