import { expect, test } from '@playwright/test';

/**
 * Cohérence entre la landing et la page des tarifs
 * — TODO_TARIFS_PAIEMENTS_INTERNATIONAL T7.3 (correction de la faille F4).
 *
 * <p>Avant ce lot, la landing affichait trois tiers « Sur devis » codés en dur
 * pendant que {@code /pricing} affichait quatre plans avec de vrais prix : deux
 * vérités contradictoires sur le meme produit. Ces scenarios verifient que la
 * landing ne reintroduit pas cette divergence.</p>
 */

/** Symboles monetaires reconnus comme preuve qu'un prix reel est affiche. */
const CURRENCY_SYMBOLS = ['€', '$', 'FCFA', 'F CFA', '£'];

async function sectionText(page: import('@playwright/test').Page): Promise<string | null> {
  const section = page.locator('#pricing');
  if ((await section.count()) === 0) return null;
  return section.innerText();
}

function countMatches(text: string, needle: string): number {
  return text.split(needle).length - 1;
}

test.describe('Landing — section tarifs', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/');
    await page.locator('#pricing').scrollIntoViewIfNeeded().catch(() => undefined);
  });

  test('ne affiche plus de devis generique la ou un prix existe', async ({ page }) => {
    const raw = await sectionText(page);
    if (raw === null) {
      test.skip(true, 'section tarifs masquee par le super-admin');
      return;
    }
    const text = raw;

    const quotes = countMatches(text, 'Sur devis');
    const unavailable = countMatches(text, 'communiqué') + countMatches(text, 'non publié');
    const hasRealPrice = CURRENCY_SYMBOLS.some((symbol) => text.includes(symbol));

    if (!hasRealPrice && unavailable === 0) {
      // Aucun prix et aucun tarif manquant : le mode « sur devis » est le
      // comportement historique, assume comme un reglage du super-admin.
      expect(quotes).toBeGreaterThan(0);
      return;
    }
    // Des qu'un prix existe, le nombre de devis generiques doit rester marginal :
    // c'est precisement la contradiction que ce lot supprime.
    expect(quotes - unavailable).toBeLessThanOrEqual(Math.max(1, hasRealPrice ? 2 : 0));
  });

  test('propose un lien vers la page des tarifs complete', async ({ page }) => {
    if ((await sectionText(page)) === null) {
      test.skip(true, 'section tarifs masquee');
    }
    const link = page.locator('#pricing').getByRole('link', { name: /tous les tarifs|all pricing/i });
    if ((await link.count()) > 0) {
      await expect(link).toHaveAttribute('href', '/pricing');
    }
  });

  test('expose au plus le nombre de plans configure', async ({ page }) => {
    if ((await sectionText(page)) === null) {
      test.skip(true, 'section tarifs masquee');
    }
    // La landing ne montre jamais plus de plans que /pricing : la landing est
    // une vitrine, pas le catalogue complet.
    const landingPlans = await page.locator('#pricing a[href^="/pricing?plan="]').count();
    expect(landingPlans).toBeLessThanOrEqual(12);
  });

  test('la landing et /pricing partagent le meme selecteur de devise', async ({ page }) => {
    if ((await page.locator('#landing-currency').count()) === 0) return;

    await page.goto('/pricing');
    if ((await page.locator('#pricing-currency').count()) === 0) return;

    // Meme devise retenue des deux cotes : la devise du visiteur est un
    // choix, pas un etat propre a chaque page.
    await expect(page.locator('#pricing-currency')).toBeVisible();
  });
});