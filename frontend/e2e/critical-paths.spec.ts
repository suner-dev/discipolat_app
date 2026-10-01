import { test, expect, request, type Page, type APIRequestContext } from '@playwright/test';

/**
 * §G6.4 — PARCOURS CRITIQUES E2E (web) — chaque test touche la VRAIE stack :
 * API réelle :8080 (comptes de démonstration semés au démarrage) + UI réelle.
 *
 * Prérequis : backend actif (E2E_API) ; le front est démarré par
 * playwright.config.ts (ou déjà vivant en local).
 *
 * Variables : E2E_API (défaut http://localhost:8080/api/v1),
 *             E2E_EMAIL/E2E_PASSWORD (compte admin de démo, défaut pasteur).
 */
const API = process.env.E2E_API || 'http://localhost:8080/api/v1';
const ADMIN_EMAIL = process.env.E2E_EMAIL || 'pasteur@discipolat.com';
const ADMIN_PASSWORD = process.env.E2E_PASSWORD || 'password123';

const run = Date.now().toString(36);

async function apiLogin(ctx: APIRequestContext, email: string, password: string) {
  const res = await ctx.post(`${API}/auth/login`, { data: { email, password } });
  expect(res.ok(), `login ${email} → ${res.status()}`).toBeTruthy();
  const body = await res.json();
  return body.accessToken as string;
}

async function uiLogin(page: Page, email: string, password: string) {
  await page.goto('/login');
  await page.fill('input[type="email"]', email);
  await page.fill('input[name="password"]', password);
  await Promise.all([
    page.waitForURL((u) => !u.pathname.startsWith('/login'), { timeout: 20_000 }),
    page.click('button[type="submit"]'),
  ]);
}

let adminApi: APIRequestContext;
let adminToken: string;

test.beforeAll(async () => {
  adminApi = await request.newContext({ baseURL: API });
  adminToken = await apiLogin(adminApi, ADMIN_EMAIL, ADMIN_PASSWORD);
  // §G6.4 — « warm-up » du chemin d'inscription : sur une JVM froide (CI, redémarrage
  // local), la toute première exécution de /auth/register dépasse 30 s (JIT +
  // dépendances lazy) et ferait expirer CP1 pour de fausses raisons. On chauffe
  // le chemin réel avec une adresse jetable avant les parcours.
  await adminApi.post(`${API}/auth/register`, {
    data: { email: `e2e-warmup-${run}@test.com`, password: 'Password123!', firstName: 'Warmup', lastName: 'E2E' },
  }).then((r) => r.status());
});

const auth = () => ({ Authorization: `Bearer ${adminToken}` });

test.describe.serial('Parcours critiques §G6.4', () => {
  test('CP1 — inscription libre au nom de l’église → succès affiché', async ({ page }) => {
    await page.goto('/register');
    await page.fill('input[name="firstName"]', `E2e${run}`);
    await page.fill('input[name="lastName"]', 'Playwright');
    await page.fill('input[type="email"]', `e2e-${run}@test.com`);
    await page.fill('input[name="password"]', 'Password123!');
    const confirm = page.locator('input[name="confirmPassword"]');
    if (await confirm.count()) await confirm.fill('Password123!');
    const phone = page.locator('input[name="phone"]');
    if (await phone.count()) await phone.fill('+33600000000');
    await page.click('button[type="submit"]');
    // Écran de succès : le formulaire a disparu (plus de champ email).
    await expect(page.locator('input[type="email"]')).toHaveCount(0, { timeout: 15_000 });
  });

  test('CP2 — répertoire : fiche créée, « sans espace », affectation → disparaît', async ({ page }) => {
    // Fiche créée par l’API réelle (même endpoint que le bouton « Nouvelle fiche »).
    const person = await adminApi.post(`${API}/people/register?source=MANUEL`, {
      headers: auth(),
      data: { firstName: `Orphelin${run}`, lastName: 'E2E', emailNormalized: `orphelin-${run}@test.com` },
    });
    expect(person.status()).toBe(200);
    const personId = (await person.json()).id as string;

    await uiLogin(page, ADMIN_EMAIL, ADMIN_PASSWORD);
    await page.goto('/people?withoutSpace=true');
    // Le filtre « sans espace » est actif (URL profonde → case cochée) et la fiche est visible.
    await expect(page.locator('input[type="checkbox"]').first()).toBeChecked();
    await page.check('input[type="checkbox"] >> nth=0');
    await expect(page.getByText(`Orphelin${run} E2E`).first()).toBeVisible({ timeout: 15_000 });

    // Affectation via l’UI (modale → choix d’espace → Confirmer).
    const spaces = await adminApi.get(`${API}/spaces`, { headers: auth() });
    expect(spaces.ok()).toBeTruthy();
    const spaceList = await spaces.json();
    expect(Array.isArray(spaceList) && spaceList.length, 'aucun espace de démo').toBeTruthy();

    // §G6.4 — « Affecter » scoppé sur LA ligne de la fiche créée : les runs
    // précédents laissent des orphelins en tête de liste « sans espace », et
    // un .first() général affecterait la mauvaise fiche.
    const ligne = page.locator('tr', { hasText: `Orphelin${run} E2E` }).first();
    await ligne.getByRole('button', { name: 'Affecter' }).click();
    await expect(page.getByRole('combobox')).toBeVisible();
    await page.getByRole('combobox').selectOption({ index: 1 });
    await page.getByRole('button', { name: 'Confirmer' }).click();
    // Sync point UI : la modale s'est fermée (succès du POST) avant de vérifier.
    await expect(page.getByRole('combobox')).toBeHidden({ timeout: 10_000 });
    // Le serveur a créé l’affectation (vérification API = source de vérité)…
    await expect
      .poll(async () => {
        const after = await adminApi.get(`${API}/people/${personId}/spaces`, { headers: auth() });
        return ((await after.json()) as unknown[]).length;
      }, { timeout: 10_000, intervals: [300] })
      .toBeGreaterThanOrEqual(1);
    // …et la liste « sans espace » ne contient plus la fiche après rechargement.
    await page.reload();
    await page.check('input[type="checkbox"] >> nth=0');
    await expect(page.getByText(`Orphelin${run} E2E`)).toHaveCount(0, { timeout: 15_000 });
  });

  test('CP3 — événement + dress code publiés visibles côté UI', async ({ page }) => {
    const eventRes = await adminApi.post(`${API}/church-events`, {
      headers: auth(),
      data: {
        title: `E2E Culte ${run}`, type: 'CULTE',
        startAt: new Date(Date.now() + 86_400_000).toISOString(),
        endAt: new Date(Date.now() + 90_000_000).toISOString(),
      },
    });
    expect(eventRes.status()).toBe(200);

    const dcRes = await adminApi.post(`${API}/dress-codes`, {
      headers: auth(),
      data: {
        title: `E2E Tenue ${run}`, serviceName: 'Culte', status: 'PUBLISHED',
        rules: [{ groupName: 'Hommes', description: 'Costume' }],
      },
    });
    expect(dcRes.status()).toBe(201);

    await uiLogin(page, ADMIN_EMAIL, ADMIN_PASSWORD);
    await page.goto('/admin/dress-codes');
    await expect(page.getByText(`E2E Tenue ${run}`).first()).toBeVisible({ timeout: 15_000 });
  });

  test('CP4 — matériel : inventaire API visible dans l’UI, prêt→dommage→maintenance', async ({ page }) => {
    const itemRes = await adminApi.post(`${API}/inventory`, {
      headers: auth(),
      data: { nom: `E2E Câble ${run}`, categorie: 'AUDIO', purchasePrice: 15000 },
    });
    expect([200, 201]).toContain(itemRes.status());
    const itemId = (await itemRes.json()).id as string;

    await uiLogin(page, ADMIN_EMAIL, ADMIN_PASSWORD);
    await page.goto('/inventory');
    await expect(page.getByText(`E2E Câble ${run}`).first()).toBeVisible({ timeout: 15_000 });

    // Prêt avec dommage déclaré → ticket de maintenance automatique (API réelle).
    const members = await adminApi.get(`${API}/people?size=1`, { headers: auth() });
    const memberId = (await members.json()).content?.[0]?.id;
    if (memberId) {
      const checkout = await adminApi.post(`${API}/assets/${itemId}/checkout`, {
        headers: auth(),
        data: { memberId, condition: 'GOOD', notes: 'E2E' },
      });
      expect([200, 201]).toContain(checkout.status());
      const ret = await adminApi.post(`${API}/assets/${itemId}/return`, {
        headers: auth(),
        data: { condition: 'DAMAGED', notes: `cassé e2e ${run}` },
      });
      expect(ret.status()).toBe(200);
      const maint = await adminApi.get(`${API}/assets/${itemId}/maintenance`, { headers: auth() });
      expect((await maint.json()).length).toBeGreaterThanOrEqual(1);
    }
  });

  test('CP5 — workflow : moteur vérifié côté serveur, automatisation réelle visible sur /workflow', async ({ page }) => {
    // 1. Moteur générique (colonne vertébrale des transferts US-13) : création
    //    définition + étape, vérification PERSISTÉE via l’API — ce moteur n’a
    //    pas de page UI propre (§G6.4 : ne pas assertir un lien inexistant).
    const def = await adminApi.post(`${API}/workflow-engine/definitions`, {
      headers: auth(),
      data: { entityType: 'E2E_TYPE', code: `E2E_${run}`, name: `Flux E2E ${run}`, enabled: true },
    });
    expect([200, 201]).toContain(def.status());
    const defId = (await def.json()).id as string;
    await adminApi.post(`${API}/workflow-engine/definitions/${defId}/steps`, {
      headers: auth(),
      data: { stepOrder: 1, stepType: 'APPROVAL', name: 'Validation e2e', assigneeRole: 'PASTEUR' },
    });
    const steps = await adminApi.get(`${API}/workflow-engine/definitions/${defId}/steps`, { headers: auth() });
    expect(steps.ok()).toBeTruthy();
    expect((await steps.json()).length).toBeGreaterThanOrEqual(1);

    // 2. Chemin critique réel de la page /workflow : une automatisation créée
    //    par le même endpoint que le bouton « Nouvelle automatisation » doit
    //    s’afficher dans la grille.
    const auto = await adminApi.post(`${API}/workflow/automations`, {
      headers: auth(),
      data: {
        nom: `E2E Auto ${run}`, description: 'Rappel e2e', triggerType: 'EVENEMENT_A_VENIR',
        triggerConfig: '{}', actionType: 'NOTIFIER', actionConfig: '{}',
      },
    });
    expect([200, 201]).toContain(auto.status());

    await uiLogin(page, ADMIN_EMAIL, ADMIN_PASSWORD);
    await page.goto('/workflow');
    await expect(page.getByText('Automatisations Workflow')).toBeVisible({ timeout: 15_000 });
    await expect(page.getByText(`E2E Auto ${run}`).first()).toBeVisible({ timeout: 15_000 });
  });

  test('CP6 — invitation : lien reçu → acceptation UI → compte + membership réels', async ({ page }) => {
    const email = `invite-e2e-${run}@test.com`;
    const inv = await adminApi.post(`${API}/admin/invitations`, {
      headers: auth(),
      data: { email, role: 'RESPONSABLE' },
    });
    expect(inv.status()).toBe(201);
    const token = (await inv.json()).invitationToken as string;

    const check = await adminApi.get(`${API}/admin/invitations/validate/${token}`);
    expect(check.status()).toBe(200);
    expect((await check.json()).valid).toBe(true);

    // Acceptation via la VRAIE page publique. §G6.4 — le formulaire réel
    // n’a PAS d’attributs name : Prénom/Nom via placeholders, et le champ
    // « Confirmer le mot de passe » DOIT être rempli (sinon reject client).
    await page.goto(`/accept-invitation?token=${token}`);
    await expect(page.getByPlaceholder('Jean')).toBeVisible({ timeout: 15_000 });
    await page.getByPlaceholder('Jean').fill('Invitée');
    await page.getByPlaceholder('Dupont').fill('E2E');
    const pws = page.locator('input[type="password"]');
    await pws.first().fill('Passwort123!');
    await pws.nth(1).fill('Passwort123!');
    await page.click('button[type="submit"]');
    await expect(
      page.getByText('Compte créé avec succès').first(),
      'pas de succès d’acceptation visible',
    ).toBeVisible({ timeout: 15_000 });
  });

  test('CP7 — finance : écritures + rapprochement → solde visible sur /finances', async ({ page }) => {
    // §G6.4 — /finances est un chunk lazy LOURD (recharts) : sur un serveur
    // vite froid, la compilation à la demande dépassait le budget du run
    // complet (faux négatif observé pw12, alors qu’un run seul passe en 20 s).
    test.slow();
    const today = new Date().toISOString().slice(0, 10);
    const tx = await adminApi.post(`${API}/finances/transactions`, {
      headers: auth(),
      data: { type: 'RECETTE', categorie: 'DIME', montant: 42000 + (run.length % 10), description: `E2E don ${run}`, dateTransaction: today },
    });
    expect([200, 201]).toContain(tx.status());

    await uiLogin(page, ADMIN_EMAIL, ADMIN_PASSWORD);
    await page.goto('/finances');
    // Le chunk lazy doit d’abord se matérialiser (titre de page), puis la ligne.
    await expect(page.getByRole('heading', { name: 'Finances' })).toBeVisible({ timeout: 60_000 });
    await expect(page.getByText(`E2E don ${run}`).first()).toBeVisible({ timeout: 30_000 });
  });

  test('CP8 — rôle modifié → permissions recalculées côté serveur < 5 s', async () => {
    // Membre de démo → promotion RESPONSABLE par l’admin → /me/permissions suit.
    // §G6.4 — GET /users n'a PAS de paramètre « search » (silencieusement
    // ignoré → n'importe quelle fiche en [0]). Le vrai contrat de recherche
    // est GET /users/search?q= (email + nom Prénom), d'où la cible exacte.
    const usersRes = await adminApi.get(`${API}/users/search?q=membre@discipolat.com`, { headers: auth() });
    expect(usersRes.ok()).toBeTruthy();
    const cible = (await usersRes.json()).find(
      (u: { email: string }) => u.email === 'membre@discipolat.com',
    );
    test.skip(!cible, 'compte de démonstration membre introuvable');

    const memberApi = await request.newContext({ baseURL: API });
    const memberToken = await apiLogin(memberApi, 'membre@discipolat.com', 'password123');
    const headers = () => ({ Authorization: `Bearer ${memberToken}` });

    // Précondition : sans la promotion, MEMBER_CREATE (apanage RESPONSABLE,
    // miroir catalogue DEPARTMENT_ADMIN) ne doit PAS être présente — sinon le
    // poll ne prouverait rien.
    const before = await memberApi.get(`${API}/me/permissions`, { headers: headers() });
    expect(before.ok(), `permissions initiales refusées → ${before.status()}`).toBeTruthy();
    expect(JSON.stringify(await before.json())).not.toContain('MEMBER_CREATE');

    const started = Date.now();
    // PUT /users/{id} exige la fiche complète (@Valid) : on renvoie les champs
    // réels du compte + le nouveau rôle — le serveur doit synchroniser la
    // tenant_membership et recalculer les permissions resolved (§G4.4).
    const put = await adminApi.put(`${API}/users/${cible.id}`, {
      headers: auth(),
      data: {
        email: cible.email, firstName: cible.firstName, lastName: cible.lastName,
        role: 'RESPONSABLE',
      },
    });
    expect(put.status(), `promotion refusée → ${put.status()}`).toBe(200);

    let elapsed = -1;
    await expect
      .poll(
        async () => {
          const perms = await memberApi.get(`${API}/me/permissions`, { headers: headers() });
          if (!perms.ok()) return false;
          const body = JSON.stringify(await perms.json());
          if (body.includes('MEMBER_CREATE')) {
            elapsed = Date.now() - started;
            return true;
          }
          return false;
        },
        { timeout: 5_000, intervals: [250] },
      )
      .toBe(true);
    expect(elapsed).toBeLessThan(5_000);

    // Restaure MEMBRE (run idempotente : rejouable sur la même base de démo).
    await adminApi.put(`${API}/users/${cible.id}`, {
      headers: auth(),
      data: {
        email: cible.email, firstName: cible.firstName, lastName: cible.lastName,
        role: 'MEMBRE',
      },
    });
  });
});
