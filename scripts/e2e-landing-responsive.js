/**
 * E2E navigateur réel — Landing page publique (responsive + cliquable).
 *
 * Vérifie, sur un vrai Chrome headless et le build de production :
 *   1. Aucun débordement horizontal (375 / 390 / 768 / 1440 px)
 *   2. Sections clés présentes (hero → footer)
 *   3. CTA Connexion → /login
 *   4. Menu mobile : ouverture, ancre Tarifs (section sous le header), fermeture
 *   5. Nav desktop : hamburger masqué
 *   6. Modale démo : ouverture + fermeture
 *   7. Bascule de thème
 *   8. Zéro erreur console
 *
 * Prerequis : `npm run build` dans frontend/ puis serveur statique sur 4173
 * (`npx vite preview --port 4173`). Chrome systeme requis.
 *
 * Usage : node scripts/e2e-landing-responsive.js
 */
const puppeteer = require('../frontend/node_modules/puppeteer-core');
const fs = require('fs');

const BASE = process.env.LANDING_BASE || 'http://localhost:4173';
const CHROME = process.env.E2E_CHROME || '/usr/bin/google-chrome-stable';
const SHOTS = '/tmp/e2e-landing-shots';

fs.mkdirSync(SHOTS, { recursive: true });

let failures = [];
let steps = 0;
const ok = (name) => { steps++; console.log(`  ✅ ${name}`); };
const fail = (name, detail) => { steps++; failures.push({ name, detail }); console.log(`  ❌ ${name} — ${detail}`); };
const sleep = (ms) => new Promise((r) => setTimeout(r, ms));

const VIEWPORTS = [
  { name: 'mobile-375', width: 375, height: 812, isMobile: true },
  { name: 'mobile-390', width: 390, height: 844, isMobile: true },
  { name: 'tablet-768', width: 768, height: 1024, isMobile: false },
  { name: 'desktop-1440', width: 1440, height: 900, isMobile: false },
];

async function checkNoHorizontalOverflow(page, vp) {
  const res = await page.evaluate(() => ({
    scrollW: document.documentElement.scrollWidth,
    innerW: window.innerWidth,
  }));
  if (res.scrollW <= res.innerW + 2) {
    ok(`${vp.name} — pas de scroll horizontal (${res.scrollW}px <= ${res.innerW}px)`);
  } else {
    fail(`${vp.name} — debordement horizontal`, `scrollWidth=${res.scrollW} > innerWidth=${res.innerW}`);
  }
}

(async () => {
  const browser = await puppeteer.launch({
    executablePath: CHROME,
    headless: 'new',
    args: ['--no-sandbox', '--disable-dev-shm-usage'],
  });

  try {
    for (const vp of VIEWPORTS) {
      console.log(`\n== Viewport ${vp.name} ==`);
      const page = await browser.newPage();
      const consoleErrors = [];
      const httpErrors = [];
      page.on('console', (m) => { if (m.type() === 'error') consoleErrors.push(m.text()); });
      page.on('pageerror', (e) => consoleErrors.push(`PAGEERROR: ${e.message}`));
      page.on('response', (r) => { if (r.status() >= 400) httpErrors.push(`HTTP ${r.status()} ${r.url()}`); });

      await page.setViewport({ width: vp.width, height: vp.height, isMobile: vp.isMobile, hasTouch: vp.isMobile });
      await page.goto(`${BASE}/`, { waitUntil: 'networkidle2', timeout: 60000 });
      await sleep(900);

      // 1. Debordement horizontal
      await checkNoHorizontalOverflow(page, vp);

      // 2. Sections cles
      const sections = await page.evaluate(() =>
        ['hero', 'problem', 'features', 'modules', 'roles', 'pricing', 'about'].map((id) => !!document.getElementById(id)),
      );
      sections.every(Boolean)
        ? ok(`${vp.name} — sections (hero→footer) presentes`)
        : fail(`${vp.name} — sections manquantes`, JSON.stringify(sections));

      // 3. CTA Connexion
      const loginHref = await page.evaluate(() => {
        const a = [...document.querySelectorAll('a')].find((el) => /connexion/i.test(el.textContent || ''));
        return a ? a.getAttribute('href') : null;
      });
      loginHref === '/login'
        ? ok(`${vp.name} — CTA Connexion → /login`)
        : fail(`${vp.name} — CTA Connexion`, `href=${loginHref}`);

      // 4. Menu mobile (< lg) ou nav desktop (>= lg)
      if (vp.width < 1024) {
        const opened = await page.evaluate(() => {
          const burger = document.querySelector('header button[aria-expanded]');
          if (!burger) return false;
          burger.click();
          return true;
        });
        await sleep(500);
        const menuState = await page.evaluate(() => {
          const links = [...document.querySelectorAll('header a')].filter((a) => (a.textContent || '').trim().length > 2);
          return { hasLogin: links.some((a) => (a.getAttribute('href') || '') === '/login'), count: links.length };
        });
        opened && menuState.hasLogin && menuState.count >= 2
          ? ok(`${vp.name} — menu mobile ouvert (${menuState.count} liens)`)
          : fail(`${vp.name} — menu mobile`, JSON.stringify(menuState));

        // Ancre Tarifs : la section doit remonter sous le header
        const anchor = await page.evaluate(() => {
          const btn = [...document.querySelectorAll('header button')].find((b) => /tarifs|pricing/i.test(b.textContent || ''));
          if (!btn) return false;
          btn.click();
          return true;
        });
        await sleep(1500);
        const pricingPos = await page.evaluate(() => {
          const el = document.getElementById('pricing');
          return el ? Math.round(el.getBoundingClientRect().top) : null;
        });
        anchor && pricingPos !== null && Math.abs(pricingPos) <= 120
          ? ok(`${vp.name} — ancre Tarifs → section a ${pricingPos}px du haut`)
          : fail(`${vp.name} — ancre Tarifs`, `top=${pricingPos}px`);

        const menuClosed = await page.evaluate(() => {
          const burger = document.querySelector('header button[aria-expanded]');
          return burger ? burger.getAttribute('aria-expanded') === 'false' : null;
        });
        menuClosed === true
          ? ok(`${vp.name} — menu mobile referme apres navigation`)
          : fail(`${vp.name} — fermeture du menu`, `aria-expanded=${menuClosed}`);
      } else {
        const burgerVisible = await page.evaluate(() => {
          const burger = document.querySelector('header button[aria-expanded]');
          return burger ? burger.getBoundingClientRect().width > 0 : false;
        });
        !burgerVisible
          ? ok(`${vp.name} — nav desktop (hamburger masque)`)
          : fail(`${vp.name} — hamburger visible sur desktop`, 'burger visible');
      }

      // 5. Modale demo
      const demoOpened = await page.evaluate(() => {
        const btn = [...document.querySelectorAll('button')].find((b) => /d[ée]monstration/i.test(b.textContent || ''));
        if (!btn) return false;
        btn.click();
        return true;
      });
      await sleep(600);
      const dialogShown = await page.evaluate(() => !!document.querySelector('[role="dialog"]'));
      demoOpened && dialogShown
        ? ok(`${vp.name} — modale demo ouverte`)
        : fail(`${vp.name} — modale demo`, 'non ouverte');
      if (dialogShown) {
        await page.keyboard.press('Escape');
        await sleep(400);
      }

      // 6. Bascule de theme (l'etat peut deja etre persiste entre viewports)
      const themeOk = await page.evaluate(() => {
        const btn = [...document.querySelectorAll('header button')].find((b) => /mode sombre|mode clair/i.test(b.getAttribute('aria-label') || ''));
        if (!btn) return null;
        const before = document.documentElement.classList.contains('dark');
        btn.click();
        return new Promise((resolve) => setTimeout(() => resolve({
          before,
          after: document.documentElement.classList.contains('dark'),
        }), 200));
      });
      themeOk && themeOk.after !== themeOk.before
        ? ok(`${vp.name} — bascule theme (${themeOk.before ? 'clair' : 'sombre'} → ${themeOk.after ? 'sombre' : 'clair'})`)
        : fail(`${vp.name} — bascule theme`, JSON.stringify(themeOk));

      // 7. Console / reseau : les appels /api en erreur (backend local absent)
      //    ne sont pas des defauts front ; le branding tombe sur son fallback.
      const realConsole = consoleErrors.filter((e) => !/favicon|Failed to load resource|ERR_BLOCKED|ERR_NAME|net::ERR/i.test(e));
      const realHttp = httpErrors.filter((e) => !/favicon/i.test(e) && !/\/api\//.test(e));
      realConsole.length === 0 && realHttp.length === 0
        ? ok(`${vp.name} — aucune erreur JS/reseau (hors API locale)`)
        : fail(`${vp.name} — erreurs front`, [...realConsole, ...realHttp].slice(0, 3).join(' | ').slice(0, 300));

      await page.screenshot({ path: `${SHOTS}/${vp.name}.png`, fullPage: false });
      await page.close();
    }

    console.log('\n================ RESULTAT ================');
    if (failures.length === 0) console.log(`\n🎉 ${steps} etapes — TOUT PASSE`);
    else {
      console.log(`\n💥 ${failures.length} echec(s) sur ${steps} :`);
      failures.forEach((f) => console.log(`  ❌ ${f.name}: ${f.detail}`));
    }
    console.log(`Screenshots: ${SHOTS}/`);
  } catch (err) {
    console.error('FATAL:', err.message);
  } finally {
    await browser.close();
  }
  process.exit(failures.length ? 1 : 0);
})();

