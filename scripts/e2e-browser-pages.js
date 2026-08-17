/**
 * E2E navigateur réel — Page Builder (V65) : liste admin, création de page,
 * publication, rendu public avec données réelles, RBAC, suppression.
 *
 * Prérequis : stack locale active (API :8080, web :5173) et `npm install`
 * dans frontend/ (puppeteer-core). Chrome système requis.
 *
 * Usage : node scripts/e2e-browser-pages.js
 */
const puppeteer = require('../frontend/node_modules/puppeteer-core');
const fs = require('fs');

const BASE = process.env.E2E_BASE || 'http://localhost:5173';
const CHROME = process.env.E2E_CHROME || '/usr/bin/google-chrome-stable';
const ADMIN_EMAIL = process.env.E2E_ADMIN_EMAIL || 'admin@discipolat.com';
const FAISEUR_EMAIL = process.env.E2E_FAISEUR_EMAIL || 'faiseur@discipolat.com';
const PASSWORD = process.env.E2E_PASSWORD || 'password123';
const SHOTS = '/tmp/e2e-pages-shots';

fs.mkdirSync(SHOTS, { recursive: true });

let failures = [];
let steps = 0;
const consoleErrors = [];

function ok(name) { steps++; console.log(`  ✅ ${name}`); }
function fail(name, detail) { steps++; failures.push({ name, detail }); console.log(`  ❌ ${name} — ${detail}`); }
async function shot(page, name) { try { await page.screenshot({ path: `${SHOTS}/${String(steps).padStart(2, '0')}-${name}.png` }); } catch (_) {} }
async function clickText(page, text) {
  return page.evaluate((t) => {
    const el = document.evaluate(`//button[contains(normalize-space(text()), "${t}")]`, document, null, XPathResult.FIRST_ORDERED_NODE_TYPE, null).singleNodeValue;
    if (!el) return false;
    el.click();
    return true;
  }, text);
}
async function waitForText(page, text, timeout = 15000) {
  await page.waitForFunction((t) => document.body && document.body.innerText.includes(t), { timeout }, text);
}
const sleep = (ms) => new Promise((r) => setTimeout(r, ms));

async function login(page, email) {
  await page.goto(`${BASE}/login`, { waitUntil: 'networkidle2', timeout: 30000 });
  await page.waitForSelector('#email', { timeout: 15000 });
  await page.type('#email', email);
  await page.type('#password', PASSWORD);
  await clickText(page, 'Se connecter');
  await sleep(2500);
  const roleShown = await page.evaluate(() => document.body.innerText.includes('Choisissez un rôle'));
  if (roleShown) {
    const role = email.includes('admin') ? 'Admin' : 'Faiseur';
    await clickText(page, role);
    await sleep(2500);
  }
}

async function getToken(page) {
  return page.evaluate(() => {
    const v = localStorage.getItem('token') || localStorage.getItem('accessToken') || localStorage.getItem('jwt');
    return v || '';
  });
}

(async () => {
  const browser = await puppeteer.launch({
    executablePath: CHROME,
    headless: 'new',
    args: ['--no-sandbox', '--disable-dev-shm-usage'],
    defaultViewport: { width: 1440, height: 900 },
  });

  const SLUG = `qa-pages-${Date.now() % 100000}`;
  const TITLE = `QA Page Builder ${Date.now() % 100000}`;

  try {
    // ============================================================
    // FLUX A — ADMIN : liste, création, publication, rendu
    // ============================================================
    console.log('\n== A1. Login admin ==');
    const pageA = await browser.newPage();
    pageA.on('console', (m) => { if (m.type() === 'error') consoleErrors.push(m.text()); });
    pageA.on('pageerror', (e) => consoleErrors.push(`PAGEERROR: ${e.message}`));
    pageA.on('response', (r) => { if (r.status() >= 400 && !r.url().includes('/revisions')) consoleErrors.push(`HTTP ${r.status()} ${r.url()}`); });
    await login(pageA, ADMIN_EMAIL);
    ok('Login admin');

    console.log('\n== A2. Liste des pages (Page Builder) ==');
    await pageA.goto(`${BASE}/admin/pages`, { waitUntil: 'networkidle2', timeout: 30000 });
    await waitForText(pageA, 'Pages personnalisées', 30000);
    const seeded = await pageA.evaluate(() => document.body.innerText.includes("Vue d'ensemble de l'église"));
    seeded ? ok('Page d’exemple « Vue d’ensemble de l’église » présente') : fail('Liste pages', 'page d’exemple absente');
    await shot(pageA, 'pages-list');

    console.log('\n== A3. Création d’une page (KPI + texte) ==');
    await clickText(pageA, 'Nouvelle page');
    await waitForText(pageA, 'Adresse (/pages/…)', 10000);
    const setInput = (label, value) => pageA.evaluate(({ label, value }) => {
      const lab = [...document.querySelectorAll('label')].find((l) => l.innerText.includes(label));
      const container = lab && lab.closest('div');
      const input = container && container.querySelector('input, select, textarea');
      if (!input) return false;
      const setter = Object.getOwnPropertyDescriptor(window.HTMLInputElement.prototype, 'value').set;
      setter.call(input, value);
      input.dispatchEvent(new Event('input', { bubbles: true }));
      return true;
    }, { label, value });
    setInput('Titre', TITLE) ? ok('Titre saisi') : fail('Création', 'champ Titre introuvable');
    setInput('Adresse (/pages/…)', SLUG);
    setInput('Clé (unique, technique)', SLUG.toUpperCase().replace(/-/g, '_'));
    await clickText(pageA, 'Ajouter un bloc');
    await sleep(300);
    // Ajouter un bloc TEXTE
    await pageA.evaluate(() => {
      const btns = [...document.querySelectorAll('button')].filter((b) => b.innerText.includes('Texte') && b.innerText.includes('Ajouter un bloc') === false);
      const add = [...document.querySelectorAll('button')].find((b) => /Ajouter un bloc/.test(b.innerText) === false && b.innerText.trim() === 'Texte');
      if (add) add.click();
    });
    await sleep(400);
    await shot(pageA, 'page-create');
    await clickText(pageA, 'Créer');
    await waitForText(pageA, 'Page créée', 15000).then(() => ok('Page créée ✅ (toast)'))
      .catch(() => fail('Création', 'pas de toast « Page créée »'));
    await sleep(1000);

    console.log('\n== A4. Publication ==');
    const pubBtn = await pageA.evaluate((title) => {
      const card = [...document.querySelectorAll('.glass-card')].find((c) => c.innerText.includes(title));
      const sw = card && card.querySelector('[role="switch"]');
      if (sw) { sw.click(); return true; }
      return false;
    }, TITLE);
    pubBtn ? ok('Toggle de publication cliqué') : fail('Publication', 'switch introuvable');
    await sleep(2500);
    const pubState = await pageA.evaluate((title) => {
      const card = [...document.querySelectorAll('.glass-card')].find((c) => c.innerText.includes(title));
      return card ? card.innerText : '';
    }, TITLE);
    pubState.includes('Publiée') ? ok('Page publiée (badge)') : fail('Publication', `badge manquant — ${pubState.slice(0, 120)}`);
    await shot(pageA, 'page-published');

    console.log('\n== A5. Rendu public (données réelles) ==');
    await pageA.goto(`${BASE}/pages/${SLUG}`, { waitUntil: 'networkidle2', timeout: 30000 });
    await waitForText(pageA, TITLE, 15000);
    const kpiRendered = await pageA.evaluate(() => {
      const txt = document.body.innerText;
      return /Âmes suivies|Indicateur/.test(txt);
    });
    kpiRendered ? ok('Page rendue avec bloc KPI') : fail('Rendu', 'aucun KPI rendu');
    await shot(pageA, 'page-render');

    console.log('\n== A6. La page d’exemple rend ses KPI réels ==');
    await pageA.goto(`${BASE}/pages/apercu-eglise`, { waitUntil: 'networkidle2', timeout: 30000 });
    await waitForText(pageA, "Vue d'ensemble de l'église", 30000);
    const realData = await pageA.evaluate(() => {
      const txt = document.body.innerText;
      const hasValue = /(Âmes suivies|Familles|Départements|Alertes ouvertes)/.test(txt);
      const hasTable = txt.includes('Dernières âmes') || txt.includes('Événements à venir');
      const hasLinks = txt.includes('Accès rapides');
      return { hasValue, hasTable, hasLinks };
    });
    realData.hasValue ? ok('KPI réels affichés') : fail('Aperçu', 'aucun KPI');
    realData.hasTable ? ok('Tableaux de données réelles affichés') : fail('Aperçu', 'aucun tableau');
    realData.hasLinks ? ok('Bloc liens rapides rendu') : fail('Aperçu', 'liens absents');
    await shot(pageA, 'apercu-render');

    // ============================================================
    // FLUX B — FAISEUR : page ouverte (scope), page restreinte refusée
    // ============================================================
    console.log('\n== B1. Login faiseur ==');
    const ctxB = await browser.createBrowserContext();
    const pageB = await ctxB.newPage();
    pageB.on('console', (m) => { if (m.type() === 'error') consoleErrors.push(m.text()); });
    pageB.on('pageerror', (e) => consoleErrors.push(`PAGEERROR: ${e.message}`));
    pageB.on('response', (r) => { if (r.status() >= 400) consoleErrors.push(`HTTP ${r.status()} ${r.url()}`); });
    await login(pageB, FAISEUR_EMAIL);
    ok('Login faiseur');

    console.log('\n== B2. La page publiée s’ouvre (rôles autorisés) ==');
    await pageB.goto(`${BASE}/pages/${SLUG}`, { waitUntil: 'networkidle2', timeout: 30000 });
    await waitForText(pageB, TITLE, 15000);
    ok('Faiseur accède à la page publiée');
    await shot(pageB, 'faiseur-page');

    console.log('\n== B3. Rendu scopé : le faiseur voit SON périmètre ==');
    // La page d'exemple est ouverte à FAISEUR : le rendu ne doit pas planter
    // et doit afficher des valeurs (éventuellement 0 pour son périmètre).
    await pageB.goto(`${BASE}/pages/apercu-eglise`, { waitUntil: 'networkidle2', timeout: 30000 });
    await waitForText(pageB, "Vue d'ensemble de l'église", 30000);
    const scopeRender = await pageB.evaluate(() => {
      const txt = document.body.innerText;
      return txt.includes('Âmes suivies') || txt.includes('Départements');
    });
    scopeRender ? ok('Rendu scopé pour le faiseur (KPI présents)') : fail('Rendu scopé', 'KPI absents');
    await shot(pageB, 'faiseur-scope');

    console.log('\n== B4. RBAC API : non-admin ne peut pas lister/écrire les pages ==');
    const tokenB = await getToken(pageB);
    const apiCheck = await pageB.evaluate(async (token) => {
      const res = await fetch('/api/v1/pages', { headers: { Authorization: `Bearer ${token}` } });
      return { status: res.status };
    }, tokenB);
    apiCheck.status === 403
      ? ok(`GET /api/v1/pages par FAISEUR → ${apiCheck.status} (403 attendu)`)
      : fail('RBAC API', `GET /pages par faiseur → ${apiCheck.status}`);
    const apiCheck2 = await pageB.evaluate(async (token) => {
      const res = await fetch('/api/v1/pages/sources', { headers: { Authorization: `Bearer ${token}` } });
      return { status: res.status };
    }, tokenB);
    apiCheck2.status === 403
      ? ok(`GET /pages/sources par FAISEUR → ${apiCheck2.status} (403 attendu)`)
      : fail('RBAC API', `GET /pages/sources par faiseur → ${apiCheck2.status}`);

    // ============================================================
    // FLUX C — ADMIN : suppression de la page de test
    // ============================================================
    console.log('\n== C1. Nettoyage : suppression de la page de test ==');
    await pageA.goto(`${BASE}/admin/pages`, { waitUntil: 'networkidle2', timeout: 30000 });
    await waitForText(pageA, 'Pages personnalisées', 30000);
    const deleted = await pageA.evaluate(async (title) => {
      window.confirm = () => true;
      const card = [...document.querySelectorAll('.glass-card')].find((c) => c.innerText.includes(title));
      if (!card) return 'card-missing';
      const del = card.querySelector('button[aria-label^="Supprimer"]');
      if (!del) return 'btn-missing';
      del.click();
      await new Promise((r) => setTimeout(r, 1500));
      return 'deleted';
    }, TITLE);
    deleted === 'deleted'
      ? ok('Page de test supprimée (nettoyage)')
      : fail('Nettoyage', `échec (${deleted})`);

    console.log('\n================ RÉSULTAT ================');
    const realErrors = consoleErrors.filter((e) => !e.includes('favicon') && !e.includes('ERR_BLOCKED') && !e.includes('net::ERR_ABORTED'));
    console.log(`Console/page errors: ${realErrors.length}`);
    realErrors.slice(0, 20).forEach((e) => console.log(`  ⚠️ ${e.slice(0, 220)}`));
    if (failures.length === 0) console.log(`\n🎉 ${steps} étapes — TOUT PASSE`);
    else {
      console.log(`\n💥 ${failures.length} échec(s) sur ${steps} :`);
      failures.forEach((f) => console.log(`  ❌ ${f.name}: ${f.detail}`));
    }
    console.log(`Screenshots: ${SHOTS}/`);
  } catch (err) {
    console.error('FATAL:', err.message);
  } finally {
    try { await browser.close(); } catch (_) {}
  }
  process.exit(failures.length ? 1 : 0);
})();
