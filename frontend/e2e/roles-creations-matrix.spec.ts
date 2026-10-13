import { test, expect, request, type Page, type APIRequestContext } from '@playwright/test';

/**
 * MATRICE RÔLES × CRÉATIONS × FLOWS — « comme Playwright sur toute l'appli » :
 * chaque création possible (église, dénomination, association, organisation,
 * méga-association) × chaque rôle (ADMIN…MEMBRE) × les flows posés par-dessus
 * (hiérarchie réseau, code de rejointure, lien /j/:slug).
 *
 * VRAIE stack uniquement (aucun stub) : API :8080 profil beta seedé + UI :5173.
 * Toutes les affirmations ci-dessous ont été vérifiées en smoke sur la stack
 * locale du 2026-10-12 (comportements réels observés, pas des idéaux).
 *
 * Variables : E2E_API (défaut http://localhost:8080/api/v1),
 *             E2E_BASE (défaut http://localhost:5173).
 */
const API = process.env.E2E_API || 'http://localhost:8080/api/v1';
const BASE = process.env.E2E_BASE || 'http://localhost:5173';

const run = Date.now().toString(36);
const PASSWORD = 'E2eMatrix!2026';

/** Les 5 TenantKind réels (enum backend + contrainte SQL V222). */
const KINDS = ['CHURCH', 'DENOMINATION', 'ASSOCIATION', 'ORGANIZATION', 'MEGA_ASSOCIATION'] as const;

/** Les 6 comptes semés par DataInitializer (profil beta, password123). */
const SEED_ACCOUNTS = [
  { email: 'admin@discipolat.com', role: 'ADMIN' },
  { email: 'pasteur@discipolat.com', role: 'PASTEUR' },
  { email: 'responsable@discipolat.com', role: 'RESPONSABLE' },
  { email: 'chef@discipolat.com', role: 'CHEF_DE_FAMILLE' },
  { email: 'faiseur@discipolat.com', role: 'FAISEUR' },
  { email: 'membre@discipolat.com', role: 'MEMBRE' },
] as const;

/**
 * Garde @authz.isTenantAdmin() vérifiée en smoke (2026-10-12) :
 * ADMIN et PASTEUR passent (200) ; RESPONSABLE/FAISEUR/…/MEMBRE refusés (403)
 * sur /admin/invitations ET /tenant/organization.
 */
const TENANT_ADMIN_EMAILS = new Set(['admin@discipolat.com', 'pasteur@discipolat.com']);

const stamp = (label: string) => `${label}-${run}-${Math.random().toString(36).slice(2, 6)}`;

function decodeClaims(accessToken: string): Record<string, unknown> {
  const payload = accessToken.split('.')[1].replace(/-/g, '+').replace(/_/g, '/');
  return JSON.parse(Buffer.from(payload, 'base64').toString('utf8'));
}

/** Inscription libre « créer mon organisation » — le VRAI flux public. */
async function registerOrg(
  ctx: APIRequestContext,
  opts: { kind?: string; churchName: string; email?: string; password?: string; extra?: Record<string, unknown> },
) {
  const email = opts.email ?? `${stamp('fondateur')}@e2e.matrix.com`;
  const res = await ctx.post(`${API}/auth/register`, {
    data: {
      email,
      password: opts.password ?? PASSWORD,
      firstName: 'Fonda',
      lastName: 'Matrix',
      phone: `+336${Date.now().toString().slice(-8)}`,
      churchName: opts.churchName,
      createChurch: true,
      kind: opts.kind,
      consentCgu: true,
      consentPrivacy: true,
      consentArt9: true,
      ...opts.extra,
    },
  });
  return { email, res, body: await res.json().catch(() => ({})) };
}

/** Session fondateur → en-tête Bearer. */
function bearer(body: Record<string, any>) {
  return { Authorization: `Bearer ${body.session.accessToken as string}` };
}

let anon: APIRequestContext;

test.beforeAll(async () => {
  anon = await request.newContext({ baseURL: API });
  // Warm-up du chemin /auth/register (JIT JVM froide, pattern critical-paths CP1).
  await anon.post(`${API}/auth/register`, {
    data: { email: `${stamp('warmup')}@e2e.matrix.com`, password: PASSWORD, firstName: 'Warm', lastName: 'Up' },
  });
});

async function uiLogin(page: Page, email: string, password: string) {
  await page.goto('/login');
  await page.fill('input[type="email"]', email);
  await page.fill('input[name="password"]', password);
  await page.click('button[type="submit"]');

  // Les comptes multi-rôles (admin=ADMIN+PASTEUR, responsable, chef) tombent
  // sur l'écran « Choisissez un rôle » et RESTENT sur /login tant qu'aucun
  // rôle n'est sélectionné (LoginPage onSubmit L142-145 : navigation reportée).
  // On honore ce flux réel : soit navigation directe (mono-rôle), soit
  // sélecteur → on clique la première carte de rôle → navigation.
  const roleSelector = page.getByText('Choisissez un rôle');
  const navigated = page
    .waitForURL((u) => !u.pathname.startsWith('/login'), { timeout: 20_000 })
    .then(() => 'nav' as const)
    .catch(() => null);
  const selector = roleSelector
    .waitFor({ state: 'visible', timeout: 20_000 })
    .then(() => 'selector' as const)
    .catch(() => null);
  const outcome = await Promise.race([navigated, selector]);

  if (outcome === 'selector') {
    await page.locator('div.space-y-3.animate-slide-up button').first().click();
    await page.waitForURL((u) => !u.pathname.startsWith('/login'), { timeout: 20_000 });
  } else if (outcome !== 'nav') {
    throw new Error(`uiLogin(${email}) : ni navigation ni sélecteur de rôle détectés`);
  }
}

// ─────────────────────────────────────────────────────────────────────────────
// SPEC A — MATRICE CRÉATION : les 5 TenantKind par le vrai flux d'inscription
// ─────────────────────────────────────────────────────────────────────────────
test.describe.serial('SPEC A — matrice création × TenantKind', () => {
  for (const kind of KINDS) {
    test(`A[${kind}] — création libre → organisation du bon type, fondateur connecté, invariants réseau`, async () => {
      const name = `E2E ${kind} ${run}`;
      const { email, res, body } = await registerOrg(anon, { kind, churchName: name });
      expect(res.status(), `register ${kind} a échoué`).toBe(201);

      // 1) Session réémise au fondateur : il est connecté tout de suite (D1).
      expect(typeof body.session?.accessToken).toBe('string');
      const claims = decodeClaims(body.session.accessToken);
      expect(String(claims.email)).toBe(email.toLowerCase());
      expect(claims.tenantId).toBeTruthy();

      // 2) Code de rejointure immédiat + lien d'invitation (contrat §4.1).
      expect(body.church.joinCode, 'pas de joinCode').not.toBe('');
      expect(body.church.slug).toBeTruthy();
      // Constat 2026-10-12 (corrigé dans SelfServiceChurchService) : la
      // réponse /register renvoyait kind="CHURCH" même pour une dénomination
      // (DTO matérialisé avant l'UPDATE). L'API doit dire la vérité dès ici.
      expect(body.church.kind, 'la réponse /register doit porter le kind réel').toBe(kind);
      expect(body.grantedPlan, 'le plan accordé doit être nommé').toBeTruthy();

      // 3) Source de vérité : la vue organisation du fondateur.
      const orgRes = await anon.get(`${API}/tenant/organization`, { headers: bearer(body) });
      expect(orgRes.status()).toBe(200);
      const org = await orgRes.json();
      expect(org.kind, `kind attendu ${kind}`).toBe(kind);
      expect(org.status).toBe('ACTIVE');
      expect(org.parentTenantId, 'une création isolée ne doit pas avoir de parent').toBeNull();
      expect(org.rootTenantId, 'une création isolée est sa propre racine').toBe(org.id);
      expect(org.childCount).toBe(0);
      expect(org.invitePath).toBe(`/j/${org.slug}`);

      // 4) Le fondateur n'est pas un membre ordinaire : permissions métier > 0.
      const permsRes = await anon.get(`${API}/me/permissions`, { headers: bearer(body) });
      expect(permsRes.status()).toBe(200);
      expect((await permsRes.json()).permissions.length).toBeGreaterThan(0);
    });
  }

  test('A[fail-closed] — kind inconnu refusé explicitement (jamais de création silencieuse)', async () => {
    const { res, body } = await registerOrg(anon, { kind: 'SECTE', churchName: `E2E genre ${run}` });
    expect(res.status()).toBe(400);
    expect(body.title).toBe('ORG_KIND_INVALID');
  });

  test('A[fail-closed] — createChurch ⊕ joinCode exclusifs', async () => {
    const { res, body } = await registerOrg(anon, {
      churchName: `E2E double ${run}`,
      extra: { joinCode: 'peu-importe' },
    });
    expect(res.status()).toBe(400);
    expect(String(body.detail)).toContain('exclusif');
  });

  test('A[fail-closed] — consentements RGPD absents → 400 de validation', async () => {
    const res = await anon.post(`${API}/auth/register`, {
      data: {
        email: `${stamp('sansconsent')}@e2e.matrix.com`,
        password: PASSWORD,
        firstName: 'Sans',
        lastName: 'Consenti',
        churchName: `E2E sans consent ${run}`,
        createChurch: true,
      },
    });
    expect(res.status()).toBe(400);
    const body = await res.json();
    expect(body.title).toBe('Validation Error');
    // Les trois consentements sont requis (@AssertTrue sur le DTO).
    expect(Object.keys(body.errors ?? {})).toEqual(
      expect.arrayContaining(['consentCgu', 'consentPrivacy', 'consentArt9']),
    );
  });

  test('A[fail-closed] — nom d’organisation vide → CHURCH_NAME_REQUIRED', async () => {
    const { res, body } = await registerOrg(anon, { kind: 'ASSOCIATION', churchName: '   ' });
    expect(res.status()).toBe(400);
    expect(body.title).toBe('CHURCH_NAME_REQUIRED');
  });

  test('A[fail-closed] — email déjà pris → refus, aucune création partielle', async () => {
    const first = await registerOrg(anon, { kind: 'ORGANIZATION', churchName: `E2E doublon ${run}` });
    expect(first.res.status()).toBe(201);
    const second = await registerOrg(anon, {
      kind: 'ORGANIZATION',
      churchName: `E2E doublon bis ${run}`,
      email: first.email,
    });
    expect(second.res.status()).toBe(400);
    expect(String(second.body.detail)).toContain('already exists');
  });
});

// ─────────────────────────────────────────────────────────────────────────────
// SPEC B — MATRICE RÔLES : 6 comptes semés × login × permissions × accès
// ─────────────────────────────────────────────────────────────────────────────
test.describe.serial('SPEC B — matrice rôles (6 UserRoles)', () => {
  for (const account of SEED_ACCOUNTS) {
    test(`B[${account.role}] — login API + claims + périmètre d’accès conforme`, async () => {
      const login = await anon.post(`${API}/auth/login`, {
        data: { email: account.email, password: 'password123' },
      });
      expect(login.status(), `login ${account.email}`).toBe(200);
      const token = (await login.json()).accessToken as string;
      const headers = { Authorization: `Bearer ${token}` };

      // Claims : le rôle attendu figure bien dans la session.
      const claims = decodeClaims(token);
      expect(claims.email).toBe(account.email);
      expect(claims.roles as string[]).toContain(account.role);

      // /auth/me et /me/permissions répondent pour TOUS les rôles (scoping UI).
      const me = await anon.get(`${API}/auth/me`, { headers });
      expect(me.status()).toBe(200);
      const perms = await anon.get(`${API}/me/permissions`, { headers });
      expect(perms.status()).toBe(200);
      const permBody = await perms.json();
      expect(permBody.userId).toBeTruthy();
      expect(Array.isArray(permBody.permissions)).toBeTruthy();

      // Garde tenant-admin (smoke 2026-10-12) : ADMIN et PASTEUR passent ;
      // les autres rôles sont refusés 403 sur les deux endpoints sensibles.
      const expected = TENANT_ADMIN_EMAILS.has(account.email) ? 200 : 403;
      const invitations = await anon.get(`${API}/admin/invitations`, { headers });
      expect(invitations.status(), `invitations pour ${account.role}`).toBe(expected);
      const org = await anon.get(`${API}/tenant/organization`, { headers });
      expect(org.status(), `organization pour ${account.role}`).toBe(expected);

      // La hiérarchie de nœuds est ouverte à tout membre authentifié.
      const tree = await anon.get(`${API}/org/tree`, { headers });
      expect(tree.status()).toBe(200);
    });

    test(`B[${account.role}] — login UI : la session s’ouvre au-delà de /login`, async ({ page }) => {
      await uiLogin(page, account.email, 'password123');
      await expect(page).not.toHaveURL(/\/login/);
    });
  }

  test('B[switch-role] — multi-rôles autorisé, rôle non détenu refusé', async () => {
    // admin@ porte ADMIN + PASTEUR : la bascule est un contrat réel (G4.4).
    const login = await anon.post(`${API}/auth/login`, {
      data: { email: 'admin@discipolat.com', password: 'password123' },
    });
    const token = (await login.json()).accessToken as string;
    const headers = { Authorization: `Bearer ${token}` };
    const pasteur = await anon.post(`${API}/auth/switch-role`, { headers, data: { role: 'PASTEUR' } });
    expect(pasteur.status()).toBe(200);
    expect(decodeClaims((await pasteur.json()).accessToken).activeRole).toBe('PASTEUR');
    // Restaure ADMIN pour ne pas laisser l’état d’en-tête biaisé aux autres tests.
    const back = await anon.post(`${API}/auth/switch-role`, { headers, data: { role: 'ADMIN' } });
    expect(back.status()).toBe(200);

    // membre@ ne détient pas ADMIN : fail-closed (400, pas de session surélevée).
    const membre = await anon.post(`${API}/auth/login`, {
      data: { email: 'membre@discipolat.com', password: 'password123' },
    });
    const mHeaders = { Authorization: `Bearer ${(await membre.json()).accessToken}` };
    const elevate = await anon.post(`${API}/auth/switch-role`, { headers: mHeaders, data: { role: 'ADMIN' } });
    expect(elevate.status()).toBe(400);
  });

  test('B[negatif] — sans token : 401 sur les endpoints authentifiés', async () => {
    const org = await anon.get(`${API}/tenant/organization`);
    expect(org.status()).toBe(401);
    const perms = await anon.get(`${API}/me/permissions`);
    expect(perms.status()).toBe(401);
  });
});

// ─────────────────────────────────────────────────────────────────────────────
// SPEC C — FLOWS par-dessus la création (hiérarchie, rejointure, lien public)
// ─────────────────────────────────────────────────────────────────────────────
test.describe.serial('SPEC C — flows réseau au-dessus des créations', () => {
  let denomHeaders: Record<string, string>;
  let denomOrg: Record<string, any>;
  let joinCode: string;

  test.beforeAll(async () => {
    const { res, body } = await registerOrg(anon, {
      kind: 'DENOMINATION',
      churchName: `E2E Réseau ${run}`,
    });
    expect(res.status()).toBe(201);
    denomHeaders = bearer(body);
    joinCode = body.church.joinCode as string;
    const org = await anon.get(`${API}/tenant/organization`, { headers: denomHeaders });
    denomOrg = await org.json();
  });

  test('C1 — dénomination → église enfant autonome (vrai sous-tenant rattaché)', async () => {
    const child = await anon.post(`${API}/tenant/organization/sub-churches`, {
      headers: denomHeaders,
      data: { name: `E2E Fille ${run}`, mode: 'autonomous' },
    });
    expect(child.status()).toBe(201);
    const c = await child.json();
    expect(c.kind).toBe('CHURCH');
    expect(c.parentTenantId).toBe(denomOrg.id);
    expect(c.rootTenantId, 'l’enfant hérite de la racine du réseau').toBe(denomOrg.id);
    expect(c.invitePath).toBe(`/j/${c.slug}`);

    // La vue réseau de la dénomination contient bien l’enfant.
    const network = await anon.get(`${API}/tenant/organization/network`, { headers: denomHeaders });
    expect(network.status()).toBe(200);
    const ids = (await network.json()).map((n: any) => n.id);
    expect(ids).toContain(c.id);

    // …et la vue courante le liste dans ses enfants.
    const current = await anon.get(`${API}/tenant/organization`, { headers: denomHeaders });
    const children = (await current.json()).children.map((n: any) => n.id);
    expect(children).toContain(c.id);
  });

  test('C2 — mode léger refusé sur /sub-churches (on passe par les codes d’entrée)', async () => {
    const res = await anon.post(`${API}/tenant/organization/sub-churches`, {
      headers: denomHeaders,
      data: { name: `E2E Light ${run}`, mode: 'light' },
    });
    expect(res.status()).toBe(400);
    expect((await res.json()).code).toBe('SUB_CHURCH_MODE_NOT_SUPPORTED');
  });

  test('C3 — code de rejointure OPEN : inscription membre + garde d’activation', async () => {
    const email = `${stamp('agrege')}@e2e.matrix.com`;
    const res = await anon.post(`${API}/auth/register`, {
      data: {
        email,
        password: PASSWORD,
        firstName: 'Agre',
        lastName: 'Ge',
        phone: `+336${Date.now().toString().slice(-8)}`,
        joinCode,
        consentCgu: true,
        consentPrivacy: true,
        consentArt9: true,
      },
    });
    expect(res.status()).toBe(201);
    const body = await res.json();
    expect(body.role).toBe('MEMBRE');
    expect(body.churchName).toBe(`E2E Réseau ${run}`);

    // Fail-closed : le compte créé par code n'a pas de session avant activation
    // email (flux §G3.1) — jamais d'accès direct au tenant.
    const login = await anon.post(`${API}/auth/login`, { data: { email, password: PASSWORD } });
    expect(login.status()).toBe(401);
  });

  test('C4 — lien public /j/:slug : la page de rejointure s’affiche sans session', async ({ page }) => {
    await page.goto(`/j/${denomOrg.slug}`);
    await expect(page).toHaveURL(new RegExp(`/j/${denomOrg.slug}`));
    // JoinChurchPage rend un <h1> (étapes « rejoindre ») — aucune redirection /login.
    await expect(page.locator('h1').first()).toBeVisible({ timeout: 15_000 });
    await expect(page).not.toHaveURL(/\/login/);
  });

  test('C5 — slug unique malgré noms identiques (suffixe numérique, jamais d’écrasement)', async () => {
    const name = `E2E Homonyme ${run}`;
    const a = await registerOrg(anon, { kind: 'CHURCH', churchName: name });
    const b = await registerOrg(anon, { kind: 'CHURCH', churchName: name });
    expect(a.res.status()).toBe(201);
    expect(b.res.status()).toBe(201);
    expect(b.body.church.slug).not.toBe(a.body.church.slug);
    expect(b.body.church.slug).toMatch(/-2$/);
  });

  test('C6 — un fondateur « méga-association » gouverne son réseau (dénomination → église)', async () => {
    const mega = await registerOrg(anon, { kind: 'MEGA_ASSOCIATION', churchName: `E2E Méga ${run}` });
    expect(mega.res.status()).toBe(201);
    const h = bearer(mega.body);
    const region = await anon.post(`${API}/tenant/organization/sub-churches`, {
      headers: h,
      data: { name: `E2E Région ${run}`, mode: 'autonomous' },
    });
    expect(region.status()).toBe(201);
    const r = await region.json();
    expect(r.parentTenantId, 'la région est enfant de la méga-association').toBeTruthy();
    expect(r.rootTenantId, 'la région rattache à la racine du réseau').toBeTruthy();
    // Le réseau de la méga-association (vue du fondateur, même session que C1)
    // contient bien l'enfant — assertion d'appartenance, pas de compteur absolu.
    const network = await anon.get(`${API}/tenant/organization/network`, { headers: h });
    expect(network.status()).toBe(200);
    const ids = (await network.json()).map((n: any) => n.id);
    expect(ids).toContain(r.id);
  });
});
