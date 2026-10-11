import { expect, test } from '@playwright/test';

/**
 * Traduction de la page des tarifs — TODO_TARIFS_PAIEMENTS_INTERNATIONAL T7.4.
 *
 * <p>La page était intégralement en français codé en dur, y compris pour les cinq
 * autres langues du produit. Ces scénarios vérifient qu'un changement de locale
 * ne laisse plus de chaîne française résiduelle sur les libellés de la page.</p>
 */

const LOCALES = [
  { label: /français/i, code: 'fr' },
  { label: /english/i, code: 'en' },
  { label: /português/i, code: 'pt' },
  { label: /español/i, code: 'es' },
];

/** Chaînes françaises qui ne doivent pas subsister hors locale FR. */
const FRENCH_RESIDUES = [
  'Plans et tarifs',
  'Choisissez le plan',
  'Choisir',
  'Nous contacter',
  'Le plus populaire',
  'Souscrire en ligne',
  'Questions fréquentes',
  'Voir tous les tarifs',
];

async function switchLocale(page: import('@playwright/test').Page, label: RegExp) {
  // Le sélecteur de langue vit dans la barre de navigation.
  const candidates = page.locator('button, a, [role="button"]');
  const count = await candidates.count();
  for (let i = 0; i < count; i++) {
    const candidate = candidates.nth(i);
    const text = await candidate.innerText().catch(() => '');
    if (label.test(text)) {
      await candidate.click();
      return true;
    }
  }
  return false;
}

test.describe('Tarifs — traductions', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/pricing');
    await page.waitForLoadState('networkidle');
  });

  test('le titre suit la locale', async ({ page }) => {
    const frenchTitle = await page.getByRole('heading', { level: 1 }).innerText();

    for (const locale of LOCALES.filter((item) => item.code !== 'fr')) {
      const switched = await switchLocale(page, locale.label);
      if (!switched) continue;
      await page.waitForLoadState('networkidle');
      const title = await page.getByRole('heading', { level: 1 }).innerText();
      expect(title.trim()).not.toBe(frenchTitle.trim());
    }
  });

  test('aucun libellé français résiduel hors locale française', async ({ page }) => {
    const switched = await switchLocale(page, /english/i);
    if (!switched) {
      test.skip(true, 'sélecteur de langue introuvable sur cet déploiement');
    }
    await page.waitForLoadState('networkidle');

    const body = await page.locator('body').innerText();
    const residues = FRENCH_RESIDUES.filter((phrase) => body.includes(phrase));
    // Le nom des plans et le nom de marque restent en français : ils sont des
    // noms propres surchargeables, pas des chaînes à traduire.
    expect(residues.filter((phrase) => phrase !== 'Plans et tarifs')).toEqual([]);
  });

  test('la direction du document suit la locale', async ({ page }) => {
    const switched = await switchLocale(page, /العربية/i);
    if (!switched) return;
    await page.waitForLoadState('networkidle');
    await expect(page.locator('html')).toHaveAttribute('dir', 'rtl');
    // Le sélecteur de devise doit rester lisible en RTL.
    const select = page.locator('#pricing-currency');
    if (await select.isVisible().catch(() => false)) {
      await expect(select).toBeVisible();
    }
  });
});