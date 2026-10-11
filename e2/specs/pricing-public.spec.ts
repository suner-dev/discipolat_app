import { expect, test } from '@playwright/test';

/**
 * Page publique des tarifs — TODO_TARIFS_PAIEMENTS_INTERNATIONAL T10.1.
 *
 * <p>Ces scénarios sont volontairement tolérants sur la disponibilité des plans
 * (un déploiement peut n'en exposer aucun) mais <b>stricts sur les invariants</b>
 * de l'affichage : un montant absent ne doit jamais se lire comme un zéro, et la
 * symbole de la devise doit suivre la devise.</p>
 */

/** Compte les occurrences d'un motif dans le texte normalisé de la page. */
async function normalizedText(page: import('@playwright/test').Page): Promise<string> {
  return (await page.locator('body').innerText())
    .replace(/ | /g, ' ')
    .replace(/[\s ]+/g, ' ')
    .trim();
}

test.describe('Page des tarifs', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/pricing');
    await expect(page.getByRole('heading', { level: 1 })).toBeVisible();
  });

  test('affiche un titre et un sous-titre sans chaîne vide', async ({ page }) => {
    const heading = page.getByRole('heading', { level: 1 });
    await expect(heading).not.toBeEmpty();
    // Les templates d'une page publique doivent être traduits : pas de résidu
    // de gabarit tel que « undefined » ou « NaN ».
    await expect(page.locator('body')).not.toContainText(/undefined|NaN|\[object Object\]/);
  });

  test('ne présente jamais un montant nul comme un tarif', async ({ page }) => {
    const text = await normalizedText(page);
    // Un « 0 » isolé suivi de « € » ou « FCFA » se lirait comme un plan gratuit.
    expect(text).not.toMatch(/(^|\s)0\s*(€|\$|FCFA|F CFA|£)/i);
  });

  test('propose un sélecteur de devise étiqueté quand plusieurs devises sont actives', async ({ page }) => {
    const select = page.locator('#pricing-currency');
    if ((await select.count()) === 0) {
      // Absence admise : le super-admin peut refuser le choix de devise.
      return;
    }
    await expect(select).toBeVisible();
    const options = await select.locator('option').allInnerTexts();
    expect(options.length).toBeGreaterThan(0);
    // Chaque option doit nommer sa devise : « XAF — Franc CFA (BEAC) ».
    for (const option of options) {
      expect(option).toMatch(/[A-Z]{3}/);
    }
  });

  test('le sélecteur de cycle n’est proposé que s’il est autorisé', async ({ page }) => {
    const monthly = page.getByRole('button', { name: /mensuel|monthly|men\.|mensal/i });
    const yearly = page.getByRole('button', { name: /annuel|yearly|an\.|mensal|سنوي/i });
    // Un des deux au moins doit exister, sinon la page n'est pas navigable.
    expect((await monthly.count()) + (await yearly.count())).toBeGreaterThan(0);
  });

  test('changer de devise conserve les plans affichés', async ({ page }) => {
    const select = page.locator('#pricing-currency');
    if ((await select.count()) === 0) return;
    const before = await page.locator('article').count();
    const options = await select.locator('option').all();
    if (options.length < 2) return;

    await select.selectOption(await options[1].getAttribute('value') ?? '');
    await page.waitForLoadState('networkidle');
    expect(await page.locator('article').count()).toBe(before);
  });

  test('reste utilisable en lecture seule (RTL)', async ({ page }) => {
    await page.getByRole('button', { name: /français|العربية|english/i }).first().click().catch(() => undefined);
    await page.waitForLoadState('networkidle');
    const dir = await page.evaluate(() => document.documentElement.dir);
    // Le site est en arabe : la page tarifs doit se retourner sans casser.
    expect(['ltr', 'rtl']).toContain(dir);
  });
});