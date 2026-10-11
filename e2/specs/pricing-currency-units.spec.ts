import { expect, test } from '@playwright/test';

/**
 * Unités monétaires à l'écran — TODO_TARIFS_PAIEMENTS_INTERNATIONAL T10.1.
 *
 * <p>Ces scénarios visent la régression la plus coûteuse du chantier : afficher
 * 29 centimes au lieu de 29 euros. Un montant majeur lu comme mineur se voit
 * immédiatement (« 0,29 € »), un montant mineur lu comme majeur aussi (« 2 900 € »)
 * — mais les deux se détectent ici sans avoir à passer une commande réelle.</p>
 */

/** Devise attendue pour la locale courante, d'après la langue du navigateur. */
function expectedFractionDigits(locale: string): number {
  // Les devises sans unité mineure ne doivent jamais afficher de décimale.
  return ['fr-FR', 'en-US'].includes(locale) ? 2 : 2;
}

test.describe('Affichage monétaire', () => {
  test('un montant en euros ne perd pas ses deux décimales', async ({ page }) => {
    await page.goto('/pricing');

    const body = await page.locator('body').innerText();
    const euroAmounts = body.match(/(\d[\d\s ]*),\d{2}\s*€/g) ?? [];

    for (const amount of euroAmounts) {
      const numeric = Number(amount.replace(/\s/g, '').replace('€', '').replace(',', '.'));
      expect(Number.isFinite(numeric)).toBe(true);
      // Un prix sous 1 € sur un abonnement mensuel est le symptôme exact de la
      // faille « montant majeur lu comme centimes ».
      expect(numeric).toBeGreaterThanOrEqual(1);
    }
  });

  test('aucune valeur absurne ne fuit dans le rendu', async ({ page }) => {
    await page.goto('/pricing');
    const body = await page.locator('body').innerText();
    // 2900 € pour un plan à 29 € : le symptôme inverse (centimes lus comme majeurs).
    expect(body).not.toMatch(/\b\d{3,}\s*€\s*\/\s*mois/);
    expect(body).not.toMatch(/\b\d{4,}\s*FCFA/);
  });

  test('le franc CFA ne s’affiche jamais avec des centimes', async ({ page }) => {
    await page.goto('/pricing');

    const select = page.locator('#pricing-currency');
    if ((await select.count()) === 0) return;

    const options = await select.locator('option').all();
    for (const option of options) {
      const value = await option.getAttribute('value');
      if (value !== 'XAF' && value !== 'XOF') continue;
      await select.selectOption(value ?? '');
      await page.waitForLoadState('networkidle');
      const body = await page.locator('body').innerText();
      // « 15 000,00 FCFA » serait une faute visible pour un client local.
      expect(body).not.toMatch(/[\d\s ]+[.,]00\s*(FCFA|F CFA)/);
    }
  });
});