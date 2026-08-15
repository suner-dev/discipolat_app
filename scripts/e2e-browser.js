/**
 * E2E navigateur réel — module Responsable (départements).
 *
 * Parcourt : login → sélecteur de rôle → départements → détail → Outils →
 * Rapport → Gestion/Événements → modale Présences (mark-all + export CSV) →
 * recherche globale → dossier membre (Présences) → dashboard responsable.
 *
 * Prérequis : stack locale active (API :8080, web :5173) et
 * `npm install` dans frontend/ (puppeteer-core). Chrome système requis.
 *
 * Usage : node scripts/e2e-browser.js
 *        (variables : BASE, DEPT_ID, EMAIL, PASSWORD en tête de fichier)
 */
const puppeteer = require('../frontend/node_modules/puppeteer-core');
const fs = require('fs');

const BASE = process.env.E2E_BASE || 'http://localhost:5173';
const API = process.env.E2E_API || 'http://localhost:8080';
const DEPT_ID = process.env.E2E_DEPT_ID || 'b0000000-0000-0000-0000-000000000004';
const EMAIL = process.env.E2E_EMAIL || 'responsable@discipolat.com';
const PASSWORD = process.env.E2E_PASSWORD || 'password123';
const EVENT_TITLE = process.env.E2E_EVENT || 'QA Culte du dimanche';
const MEMBER_NAME = process.env.E2E_MEMBER || 'Aya Kouassi';
const CHROME = process.env.E2E_CHROME || '/usr/bin/google-chrome-stable';
const SHOTS = '/tmp/e2e-shots';

fs.mkdirSync(SHOTS, { recursive: true });

let failures = [];
let steps = 0;
const consoleErrors = [];

function ok(name) { steps++; console.log(`  ✅ ${name}`); }
function fail(name, detail) { steps++; failures.push({ name, detail }); console.log(`  ❌ ${name} — ${detail}`); }
async function shot(page, name) { await page.screenshot({ path: `${SHOTS}/${String(steps).padStart(2, '0')}-${name}.png` }); }
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

(async () => {
  const browser = await puppeteer.launch({
    executablePath: CHROME,
    headless: 'new',
    args: ['--no-sandbox', '--disable-dev-shm-usage'],
    defaultViewport: { width: 1440, height: 900 },
  });
  const page = await browser.newPage();
  page.on('console', (m) => { if (m.type() === 'error') consoleErrors.push(m.text()); });
  page.on('pageerror', (e) => consoleErrors.push(`PAGEERROR: ${e.message}`));
  page.on('response', (r) => { if (r.status() >= 400) consoleErrors.push(`HTTP ${r.status()} ${r.url()}`); });

  try {
    console.log('\n== 1. Login ==');
    await page.goto(`${BASE}/login`, { waitUntil: 'networkidle2', timeout: 30000 });
    await page.type('#email', EMAIL);
    await page.type('#password', PASSWORD);
    await clickText(page, 'Se connecter');
    await sleep(1500);
    const roleShown = await page.evaluate(() => document.body.innerText.includes('Choisissez un rôle'));
    if (roleShown) { await clickText(page, 'Responsable'); await sleep(1500); ok('Sélecteur de rôle → Responsable'); }
    else ok('Login direct');
    await shot(page, 'dashboard');

    console.log('\n== 2. Départements ==');
    await page.goto(`${BASE}/departments`, { waitUntil: 'networkidle2', timeout: 30000 });
    await waitForText(page, 'Départements', 15000);
    const hasDept = await page.evaluate(() => document.body.innerText.includes('Audiovisuel'));
    hasDept ? ok('Liste départements (Audiovisuel présent)') : fail('Liste départements', 'Audiovisuel introuvable');

    console.log('\n== 3. Détail département ==');
    await page.goto(`${BASE}/departments/${DEPT_ID}`, { waitUntil: 'networkidle2', timeout: 30000 });
    await waitForText(page, 'Audiovisuel', 15000);
    const detailOk = await page.evaluate(() => {
      const t = document.body.innerText;
      return t.includes('Gérer') && t.includes('Outils') && t.includes('Rapport') && t.includes('Stats');
    });
    detailOk ? ok('Boutons Gérer / Outils / Rapport / Stats') : fail('Page détail', 'boutons manquants');

    console.log('\n== 4. Outils (ex-404) ==');
    await page.goto(`${BASE}/departments/${DEPT_ID}/tools`, { waitUntil: 'networkidle2', timeout: 30000 });
    await sleep(1200);
    const toolsText = await page.evaluate(() => document.body.innerText);
    if (toolsText.includes('Outils & rapports')) {
      ok('Page Outils chargée');
      const tabs = ['Rapports', 'Checklists', 'Inventaire', 'Documentation', 'Paramètres'];
      const missing = tabs.filter((t) => !toolsText.includes(t));
      missing.length === 0 ? ok('Onglets Outils : 5') : fail('Onglets Outils', missing.join(', '));
    } else fail('Page Outils', '« Outils & rapports » introuvable');
    await shot(page, 'tools');

    console.log('\n== 5. Rapport ==');
    await page.goto(`${BASE}/departments/${DEPT_ID}/report`, { waitUntil: 'networkidle2', timeout: 30000 });
    await sleep(1200);
    const reportText = await page.evaluate(() => document.body.innerText);
    (reportText.includes('Rapport') || reportText.includes('Synthèse')) ? ok('Page Rapport chargée') : fail('Page Rapport', 'contenu introuvable');

    console.log('\n== 6. Gestion — Événements & Présences ==');
    await page.goto(`${BASE}/departments/${DEPT_ID}/manage`, { waitUntil: 'networkidle2', timeout: 30000 });
    await sleep(1500);
    const tabClicked = await page.evaluate(() => {
      const btns = [...document.querySelectorAll('button')].filter((b) => b.innerText.trim() === 'Événements');
      if (!btns.length) return false;
      btns[btns.length - 1].click();
      return true;
    });
    await sleep(1200);
    tabClicked ? ok('Onglet Événements ouvert') : fail('Onglet Événements', 'bouton introuvable');
    const mgmtText = await page.evaluate(() => document.body.innerText);
    mgmtText.includes(EVENT_TITLE) ? ok('Liste des événements affichée') : fail('Liste des événements', `« ${EVENT_TITLE} » absent`);
    await shot(page, 'manage-events');

    const hasPresences = await page.evaluate(() => [...document.querySelectorAll('button')].filter((b) => b.innerText.includes('Présences')).length);
    if (hasPresences > 0) {
      await clickText(page, 'Présences');
      await sleep(1200);
      const modalText = await page.evaluate(() => document.body.innerText);
      const hasMarkAll = modalText.includes('Marquer tous présents');
      const hasExport = modalText.includes('Exporter CSV');
      (hasMarkAll && hasExport) ? ok('Modale : mark-all + export CSV') : fail('Modale', `markAll:${hasMarkAll} export:${hasExport}`);
      await clickText(page, 'Marquer tous présents');
      await sleep(1200);
      const after = await page.evaluate(() => document.body.innerText);
      /marqu|présent/i.test(after) ? ok('Marquer tous présents : feedback') : fail('Marquer tous présents', 'aucun retour');
      await shot(page, 'attendance-markall');
      await page.keyboard.press('Escape');
      await sleep(500);
    } else fail('Modale Présences', 'aucun bouton Présences');

    console.log('\n== 7. Recherche globale ==');
    const searchInput = await page.$('input[placeholder*="Recherche rapide"]');
    if (searchInput) {
      await searchInput.click({ clickCount: 3 });
      await searchInput.type('aya');
      await waitForText(page, MEMBER_NAME, 8000)
        .then(() => ok(`Recherche : ${MEMBER_NAME} trouvé`))
        .catch(() => fail('Recherche globale', `${MEMBER_NAME} introuvable`));
      await shot(page, 'global-search');
    } else fail('Recherche globale', 'champ « Recherche rapide » introuvable');

    console.log('\n== 8. Dossier membre ==');
    const token = await page.evaluate(() => localStorage.getItem('accessToken'));
    const resp = await fetch(`${API}/api/v1/departments/${DEPT_ID}/members?page=0&size=5`, { headers: { Authorization: `Bearer ${token}` } }).then((r) => r.json()).catch(() => null);
    const memberId = resp && resp.content && resp.content.length ? resp.content[0].id : null;
    if (memberId) {
      await page.goto(`${BASE}/departments/${DEPT_ID}/members/${memberId}`, { waitUntil: 'networkidle2', timeout: 30000 });
      await sleep(1200);
      const tabOk = await page.evaluate(() => {
        const btns = [...document.querySelectorAll('button')].filter((b) => b.innerText.trim().startsWith('Présences'));
        if (!btns.length) return false;
        btns[btns.length - 1].click();
        return true;
      });
      await sleep(1200);
      tabOk ? ok('Dossier : onglet Présences ouvert') : fail('Dossier membre', 'onglet Présences introuvable');
      const presencesText = await page.evaluate(() => document.body.innerText);
      presencesText.includes('Présence aux événements') ? ok('Dossier : section Présence aux événements') : fail('Dossier Présences', 'section introuvable');
      const hasActions = presencesText.includes('Marquer tous présents') && presencesText.includes('Exporter CSV');
      hasActions ? ok('Dossier : actions mark-all + export') : fail('Dossier Présences', 'actions manquantes');
      await shot(page, 'dossier-presences');
    } else fail('Dossier membre', 'aucun membre récupéré');

    console.log('\n== 9. Dashboard responsable ==');
    await page.goto(`${BASE}/dashboard/responsable`, { waitUntil: 'networkidle2', timeout: 30000 });
    await sleep(1500);
    const dashText = await page.evaluate(() => document.body.innerText);
    dashText.includes('Présence aux événements') ? ok('Dashboard : carte Présence aux événements') : fail('Dashboard', 'carte présence introuvable');
    dashText.includes('Audiovisuel') ? ok('Dashboard : département actif') : fail('Dashboard', 'département absent');

    console.log('\n================ RÉSULTAT ================');
    const realErrors = consoleErrors.filter((e) => !e.includes('favicon') && !e.includes('ERR_BLOCKED'));
    console.log(`Console/page errors: ${realErrors.length}`);
    realErrors.slice(0, 20).forEach((e) => console.log(`  ⚠️ ${e.slice(0, 200)}`));
    if (failures.length === 0) console.log(`\n🎉 ${steps} étapes — TOUT PASSE`);
    else {
      console.log(`\n💥 ${failures.length} échec(s) sur ${steps} :`);
      failures.forEach((f) => console.log(`  ❌ ${f.name}: ${f.detail}`));
    }
    console.log(`Screenshots: ${SHOTS}/`);
  } catch (err) {
    console.error('FATAL:', err.message);
    await shot(page, 'fatal');
  } finally {
    await browser.close();
  }
  process.exit(failures.length ? 1 : 0);
})();
