import { useCallback, useMemo } from 'react';
import { useTenantOptional } from '@/contexts/TenantContext';

/**
 * Pont « servi d'abord » (phase 4 résiduelle — Paramétrisation par église).
 *
 * Sémantique alignée sur la garde de routes `ProtectedRoute` (App.tsx) : les
 * permissions SERVIES par le serveur (`GET /me/permissions`, agrégées par
 * TenantContext) priment des que le contexte tenant est initialisé ; sinon on
 * retombe sur le littéral de rôles historique (fallback legacy). Une église qui personnalise sa matrice
 * rôle→permission voit donc immédiatement les boutons/sections de pages
 * suivre la matrice, sans jamais régresser pendant le chargement.
 *
 * Règle d'or (additif) : la clé passée ici doit avoir une matrice ≡ au
 * littéral legacy remplacé (inventaire .e2e-tmp/param/keys_modules.txt,
 * migrations V258-V262), ou être une clé UI_* dédiée mintée pour ce site.
 */
export function useServedAccess() {
  const tenant = useTenantOptional();
  // Sémantique de référence : « le contexte tenant a fini de charger ».
  // Identique à la garde de routes `ProtectedRoute` (`permissions && … &&
  // tenant?.isInitialized`, App.tsx) ET à `AuthState.canServed`
  // (`_servedPermissionsReady`, mobile/lib/app.dart).
  //
  // ⚠ Une version antérieure exigeait en plus `permissions.length > 0`. C'était
  // une divergence : la garde de routes, elle, refuse déjà un ensemble servi
  // vide (fail-closed), et le mobile ne l'exige pas. Le cas n'est donc
  // atteignable que sur un écran rendu hors ProtectedRoute — où l'ancien
  // comportement laissait paradoxalement les boutons visibles alors que le
  // serveur les refuse. Aligne sur les deux autres points d'entree.
  const servedActive = !!tenant?.isInitialized;

  /** granted(key, legacy) : la clé de permission sert d'abord ; le prédicat
   *  legacy (comparaison de rôles figée) ne s'applique qu'en l'absence de
   *  permissions servies. */
  const can = useCallback(
    (key: string, legacy: () => boolean): boolean => {
      if (tenant && servedActive) return tenant.hasPermission(key);
      return legacy();
    },
    [tenant, servedActive],
  );

  /** grantedOne([k1, k2…], legacy) : au moins une des clés suffit
   *  (miroir de ProtectedRoute qui admet si une clé du registre est accordée). */
  const canAny = useCallback(
    (keys: string[], legacy: () => boolean): boolean => {
      if (tenant && servedActive) return keys.some((k) => tenant.hasPermission(k));
      return legacy();
    },
    [tenant, servedActive],
  );

  return { can, canAny, servedActive };
}

/**
 * Phase 6 — vocabulaire de rôles « servi d'abord » (paramétrable par église).
 * Les sélecteurs de rôles (ciblage d'annonces, initiateurs de workflow,
 * visibilité de menus…) partaient d'une liste figée de literals ; ils sont
 * ici complétés par le catalogue de rôles SERVI du tenant
 * (`GET /admin/roles/overview`, chargé dans TenantContext), qui contient les
 * rôles custom créés par chaque église.
 *
 * Règle d'or (additif) : le résultat est toujours un SUR-ENSEMBLE de `base`
 * — aucun choix existant ne peut disparaître ; les clés servies inconnues
 * s'ajoutent. Hors contexte tenant initialisé (tests, chargement), on
 * retourne `base` à l'identique.
 */
export function useRoleVocabulary(base: readonly string[]): string[] {
  const tenant = useTenantOptional();
  const servedKeys =
    tenant?.isInitialized && tenant.roles?.length
      ? tenant.roles.map((r) => (r.key ?? '').toUpperCase()).filter((k) => k.length > 0)
      : [];
  return useMemo(() => {
    const out = [...base];
    for (const k of servedKeys) if (!out.includes(k)) out.push(k);
    return out;
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [base.join('|'), servedKeys.join('|')]);
}
