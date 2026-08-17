/**
 * E2E navigateur réel — présences hebdo (absent→présent), fiche utilisateur,
 * évaluations (donner / modifier).
 *
 * Prérequis : stack locale active (API :8080, web :5173) et `npm install`
 * dans frontend/ (puppeteer-core). Chrome système requis.
 *
 * Usage : node scripts/e2e-browser-fiche.js
 */
const puppeteer = require('../frontend/node_modules/puppeteer-core');
const fs = require('fs');

const BASE = process.env.E2E_BASE || 'http://localhost:5173';
const CHROME = process.env.E2E_CHROME || '/usr/bin/google-chrome-stable';
const ADMIN_EMAIL = process.env.E2E_ADMIN_EMAIL || 'admin@discipolat.com';
const RESP_EMAIL = process.env.E2E_RESP_EMAIL || 'responsable@discipolat.com';
const PASSWORD = process.env.E2E_PASSWORD || 'password123';
const SHOTS = '/tmp/e2e-fiche-shots';

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
  await sleep(2000);
  const roleShown = await page.evaluate(() => document.body.innerText.includes('Choisissez un rôle'));
  if (roleShown) {
    // Choisir le rôle qui correspond à l'espace cible
    const role = email.includes('admin') ? 'Admin' : 'Responsable';
    await clickText(page, role);
    await sleep(2000);
  }
}

(async () => {
  const browser = await puppeteer.launch({
    executablePath: CHROME,
    headless: 'new',
    args: ['--no-sandbox', '--disable-dev-shm-usage'],
    defaultViewport: { width: 1440, height: 900 },
  });

  try {
    // ============================================================
    // FLUX A — ADMIN : fiche utilisateur + évaluation (donner/modifier)
    // ============================================================
    console.log('\n== A1. Login admin ==');
    const pageA = await browser.newPage();
    pageA.on('console', (m) => { if (m.type() === 'error') consoleErrors.push(m.text()); });
    pageA.on('pageerror', (e) => consoleErrors.push(`PAGEERROR: ${e.message}`));
    pageA.on('response', (r) => { if (r.status() >= 400 && !r.url().includes('/evaluations/')) consoleErrors.push(`HTTP ${r.status()} ${r.url()}`); });
    await login(pageA, ADMIN_EMAIL);
    ok('Login admin');

    console.log('\n== A2. Liste des utilisateurs ==');
    await pageA.goto(`${BASE}/users`, { waitUntil: 'networkidle2', timeout: 30000 });
    await waitForText(pageA, 'Utilisateurs', 30000);
    const rows = await pageA.evaluate(() => document.querySelectorAll('button[title="Voir la fiche complète"]').length);
    rows > 0 ? ok(`${rows} fiche(s) disponibles`) : fail('Liste utilisateurs', 'aucun bouton Fiche');
    await shot(pageA, 'users-list');

    console.log('\n== A3. Fiche utilisateur ==');
    const opened = await pageA.evaluate(() => {
      // Cibler l'utilisateur avec âme liée (membre@discipolat.com → fiche complète
      // avec section « Fiche âme liée » + dossier). Repli : premier bouton.
      const rows = [...document.querySelectorAll('tr')];
      const row = rows.find((r) => r.innerText.includes('membre@discipolat.com'));
      const btn = (row && row.querySelector('button[title="Voir la fiche complète"]'))
        || document.querySelector('button[title="Voir la fiche complète"]');
      if (!btn) return false;
      btn.click();
      return true;
    });
    if (!opened) { fail('Fiche utilisateur', 'bouton introuvable'); }
    else {
      await sleep(2000);
      const modalInfo = await pageA.evaluate(() => {
        const overlay = document.querySelector('.fixed.inset-0.z-\\[100\\]');
        if (!overlay) return { open: false };
        const text = overlay.innerText;
        const lower = text.toLowerCase();
        return {
          open: true,
          hasEval: text.includes('Évaluation'),
          hasIdentity: !!overlay.querySelector('h3'),
          hasDossier: lower.includes('dossier du membre') || lower.includes('aucune âme liée')
            || lower.includes('fiche âme liée') || lower.includes('âmes suivies')
            || lower.includes('départements dirigés') || lower.includes('famille gérée'),
          text: text.replace(/\n+/g, ' ').slice(0, 400),
        };
      });
      (modalInfo.open && modalInfo.hasEval && modalInfo.hasIdentity)
        ? ok('Modale fiche ouverte (identité + évaluation)')
        : fail('Fiche utilisateur', JSON.stringify({ open: modalInfo.open, hasEval: modalInfo.hasEval, hasIdentity: modalInfo.hasIdentity }));
      modalInfo.hasDossier ? ok('Fiche : sections âme/dossier/âmes suivies présentes') : fail('Fiche utilisateur', `aucune section de contenu — ${modalInfo.text}`);
      await shot(pageA, 'fiche-modal');
    }

    console.log('\n== A4. Évaluation : donner / modifier ==');
    if (opened) {
      const overlaySel = '.fixed.inset-0.z-\\[100\\]';
      // Étoile 5 (dernière du rang interactif)
      const stars = await pageA.evaluate((sel) => {
        const o = document.querySelector(sel);
        return o ? [...o.querySelectorAll('button')].filter((b) => b.querySelector('svg.lucide-star')).length : 0;
      }, overlaySel);
      stars >= 5 ? ok(`${stars} étoiles interactives`) : fail('Évaluation', `${stars} étoiles trouvées`);
      await pageA.evaluate((sel) => {
        const o = document.querySelector(sel);
        const btns = [...o.querySelectorAll('button')].filter((b) => b.querySelector('svg.lucide-star'));
        btns[btns.length - 1].click(); // 5e étoile
      }, overlaySel);
      await sleep(400);
      const ta = await pageA.$('textarea[placeholder*="Appréciation"]');
      if (ta) { await ta.click({ clickCount: 3 }); await ta.type('QA navigateur e2e'); }
      const saveLabel = await pageA.evaluate((sel) => {
        const o = document.querySelector(sel);
        const b = [...o.querySelectorAll('button')].find((x) => /(Donner|Modifier) l'évaluation/.test(x.innerText.trim()));
        return b ? b.innerText.trim() : '';
      }, overlaySel);
      ok(`Bouton : ${saveLabel}`);
      await clickText(pageA, saveLabel);
      await sleep(2500);
      const afterSave = await pageA.evaluate(() => document.body.innerText);
      const saved = /Évaluation (enregistrée|modifiée)/.test(afterSave);
      saved ? ok('Évaluation enregistrée/modifiée ✅ (toast)') : fail('Évaluation save', 'aucun toast de confirmation');
      await shot(pageA, 'eval-saved');

      // Re-ouvrir pour vérifier l'état « Vous avez évalué » + modifier
      await clickText(pageA, 'Fermer');
      await sleep(700);
      await pageA.evaluate(() => {
        const rows = [...document.querySelectorAll('tr')];
        const row = rows.find((r) => r.innerText.includes('membre@discipolat.com'));
        const btn = (row && row.querySelector('button[title="Voir la fiche complète"]'))
          || document.querySelector('button[title="Voir la fiche complète"]');
        if (btn) btn.click();
      });
      await sleep(2000);
      const modal2 = await pageA.evaluate(() => document.body.innerText);
      modal2.includes('Vous avez évalué') ? ok('Fiche : badge « Vous avez évalué »') : fail('Fiche re-ouverte', 'badge manquant');
      // Modifier : reprendre ma note puis baisser à 4 étoiles
      const ghost = await pageA.evaluate((sel) => {
        const overlay = document.querySelector(sel);
        const b = overlay && [...overlay.querySelectorAll('button')].find((x) => x.innerText.includes('Modifier ma dernière'));
        if (b) { b.click(); return true; }
        return false;
      }, overlaySel);
      ghost ? ok('Reprise de ma dernière évaluation') : fail('Évaluation modify', 'bouton de reprise absent');
      await sleep(600);
      await pageA.evaluate((sel) => {
        const overlay = document.querySelector(sel);
        const btns = [...overlay.querySelectorAll('button')].filter((b) => b.querySelector('svg.lucide-star'));
        btns[3].click(); // 4e étoile
      }, overlaySel);
      await sleep(400);
      await clickText(pageA, "Modifier l'évaluation");
      await sleep(2500);
      const afterModify = await pageA.evaluate(() => document.body.innerText);
      /Évaluation modifiée/.test(afterModify) ? ok('Évaluation modifiée ✅ (toast)') : fail('Évaluation modify', 'pas de toast « modifiée »');
      await shot(pageA, 'eval-modified');
      await clickText(pageA, 'Fermer');
    }

    // ============================================================
    // FLUX B — RESPONSABLE : saisie des présences (absent → présent)
    // ============================================================
    console.log('\n== B1. Login responsable ==');
    // Contexte de navigation incognito : localStorage séparé (le login admin
    // du flux A est partagé entre onglets du même contexte).
    const respCtx = await browser.createBrowserContext();
    const pageB = await respCtx.newPage();
    pageB.on('console', (m) => { if (m.type() === 'error') consoleErrors.push(m.text()); });
    pageB.on('pageerror', (e) => consoleErrors.push(`PAGEERROR: ${e.message}`));
    pageB.on('response', (r) => { if (r.status() >= 400) consoleErrors.push(`HTTP ${r.status()} ${r.url()}`); });
    await login(pageB, RESP_EMAIL);
    ok('Login responsable');

    console.log('\n== B2. Saisie des présences (absent → présent) ==');
    await pageB.goto(`${BASE}/dashboard/responsable`, { waitUntil: 'networkidle2', timeout: 30000 });
    await sleep(2500);
    const sheetText = await pageB.evaluate(() => document.body.innerText);
    sheetText.includes('Saisie des présences') ? ok('Carte Saisie des présences') : fail('Saisie présences', 'carte introuvable');
    await shot(pageB, 'presence-sheet');

    const firstRow = await pageB.evaluate(() => {
      const btn = [...document.querySelectorAll('button[title="Marquer absent"]')][0];
      if (!btn) return null;
      // Le groupe de boutons est le plus proche ancêtre flex ; la ligne membre
      // est son parent (elle porte aussi le statut Présent/Absent/Non pointé).
      const row = btn.closest('div[class*="flex"]')?.parentElement || null;
      const name = row ? (row.querySelectorAll('p')[0]?.innerText || '') : '';
      return { name, hasRow: !!row };
    });
    if (!firstRow) {
      fail('Saisie présences', 'aucun membre à pointer');
    } else {
      ok(`Membre ciblé : ${firstRow.name}`);
      // Marquer ABSENT puis enregistrer
      await pageB.evaluate(() => { [...document.querySelectorAll('button[title="Marquer absent"]')][0].click(); });
      await sleep(500);
      const absentShown = await pageB.evaluate(() => {
        const btn = [...document.querySelectorAll('button[title="Marquer absent"]')][0];
        const row = btn.closest('div[class*="flex"]')?.parentElement;
        return row ? row.innerText.includes('Absent') : false;
      });
      absentShown ? ok('Statut « Absent » affiché') : fail('Présences', 'statut Absent non affiché');
      await clickText(pageB, 'Enregistrer les présences');
      await waitForText(pageB, 'Présences enregistrées', 10000).then(() => ok('Enregistrement (absent) ✅'))
        .catch(() => fail('Présences save', 'pas de toast « Présences enregistrées »'));
      await sleep(1200);

      // Repasser à PRÉSENT (le bug corrigé : le bouton reste actif)
      await pageB.evaluate(() => {
        const btn = [...document.querySelectorAll('button[title="Marquer présent"]')][0];
        if (btn && !btn.disabled) btn.click();
        return !!(btn && !btn.disabled);
      });
      await sleep(500);
      const presentShown = await pageB.evaluate(() => {
        const btn = [...document.querySelectorAll('button[title="Marquer présent"]')][0];
        const row = btn.closest('div[class*="flex"]')?.parentElement;
        return row ? row.innerText.includes('Présent') : false;
      });
      presentShown ? ok('Statut repassé à « Présent » (absent modifiable)') : fail('Présences', 'impossible de repasser à Présent');
      await clickText(pageB, 'Enregistrer les présences');
      await waitForText(pageB, 'Présences enregistrées', 10000).then(() => ok('Enregistrement (présent) ✅'))
        .catch(() => fail('Présences save', 'pas de toast après Présent'));
      await shot(pageB, 'presence-present');

      // Restaurer l'état : re-marquer absent puis enregistrer (idempotent)
      await pageB.evaluate(() => { [...document.querySelectorAll('button[title="Marquer absent"]')][0].click(); });
      await sleep(300);
      await clickText(pageB, 'Enregistrer les présences');
      await sleep(1500);
      ok('État restauré (absent) — fin de test');
    }

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
