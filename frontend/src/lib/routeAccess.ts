import type { UserRole } from '@/types';

/* ============================================================================
 * REGISTRE CENTRAL DES ACCÈS PAR RÔLE
 * --------------------------------------------------------------------------
 * Source de vérité unique partagée par :
 *  - ProtectedRoute (App.tsx) : contrôle d'accès aux routes ;
 *  - Sidebar : filtrage des menus configurables (backend) selon le rôle actif,
 *    afin qu'aucun menu visible ne puisse renvoyer vers un espace interdit.
 *
 * Une route absente de ce registre est accessible à tout utilisateur
 * authentifié (comportement de <ProtectedRoute> sans prop `roles`).
 * ========================================================================== */

export type AccessRole = UserRole | string;

/* ============================================================================
 * PARAMÉTRISATION PAR ÉGLISE (Phase 2-client) — ACCÈS PAR PERMISSIONS SERVIES
 * --------------------------------------------------------------------------
 * Tranche MEMBERS : pour les endpoints backend gardés par
 * `@authz.can('<CLÉ>')`, les routes correspondantes sont gardées côté client par
 * les CLÉS de permissions SERVIES (`GET /api/v1/me/permissions`, miroir exact du
 * serveur depuis la Phase 2-client — pont d'autorités actives inclus). Une
 * église qui redonne une clé à un rôle (surcouche tenant de la matrice V258)
 * remet l'UI à jour sans redéploiement : plus rien n'est figé.
 *
 * Les littéraux `ROUTE_ROLES` restent le REPLI de transition : route non
 * convertie, contexte tenant pas encore chargé, ou test monté hors TenantProvider.
 * Le serveur demeure seul juge : l'UI ne fait qu'aligner les boutons.
 * ========================================================================== */

/** Routes converties : motif de chemin → clés permission servant la garde (any-of). */
export const ROUTE_PERMISSIONS: Record<string, string[]> = {
  // Tranche MEMBERS (Phase 2) — clés du catalogue V258, miroir des gardes `@authz.can`.
  '/members/requests': ['MEMBERS_EXECUTE'],         // inbox · statuts · présences scopées
  '/member-competences': ['MEMBERS_EXECUTE'],       // appariement des compétences
  '/qr-checkin': ['MEMBERS_EXECUTE_QRCHECKIN'],     // pointage QR (garde la plus large de l'écran)
  '/geofencing': ['MEMBERS_EXECUTE_R4A'],           // garde de classe GeofencingController
};

/** Routes restreintes : motif de chemin (segments `:param` dynamiques) → rôles autorisés. */
export const ROUTE_ROLES: Record<string, AccessRole[]> = {
  // Tableaux de bord
  '/dashboard/membre/activities': ['MEMBRE'],
  '/dashboard/membre': ['MEMBRE'],
  '/dashboard/pasteur': ['ADMIN', 'PASTEUR'],
  '/dashboard/chef-famille': ['ADMIN', 'PASTEUR', 'CHEF_DE_FAMILLE'],
  '/dashboard/responsable': ['ADMIN', 'PASTEUR', 'RESPONSABLE'],

  // Âmes & familles
  '/souls/retractions': ['ADMIN', 'PASTEUR', 'RESPONSABLE'],
  '/souls/new': ['ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR'],
  '/souls/:id/pastoral-360': ['ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR'],
  '/souls/:id/edit': ['ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR'],
  '/souls/:id': ['ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR'],
  '/souls': ['ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR'],
  '/families/compare': ['ADMIN', 'PASTEUR'],
  '/families/new': ['ADMIN', 'PASTEUR', 'CHEF_DE_FAMILLE'],
  '/families/:id/faiseur-performance': ['ADMIN', 'PASTEUR', 'CHEF_DE_FAMILLE', 'FAISEUR'],
  '/families/:id': ['ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR'],
  '/families': ['ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR'],

  // Départements & RH
  '/departments/:id/report': ['ADMIN', 'PASTEUR', 'RESPONSABLE'],
  '/departments/:id/manage': ['ADMIN', 'PASTEUR', 'RESPONSABLE'],
  '/departments/:id/members/:memberId': ['ADMIN', 'PASTEUR', 'RESPONSABLE'],
  '/departments/:id/stats': ['ADMIN', 'PASTEUR', 'RESPONSABLE'],
  '/departments/:id/tools': ['ADMIN', 'PASTEUR', 'RESPONSABLE'],
  '/departments/:id': ['ADMIN', 'PASTEUR', 'RESPONSABLE'],
  '/organization': ['ADMIN', 'PASTEUR', 'RESPONSABLE'],
  // §G4.3 — pastorat : lecture bergers/administration, écritures PASTOR_PRINCIPAL/ADMIN.
  '/pastoral': ['ADMIN', 'PASTEUR', 'PASTOR_PRINCIPAL'],
  '/spaces/:id': ['ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR', 'MEMBRE'],
  '/departments': ['ADMIN', 'PASTEUR', 'RESPONSABLE'],
  '/users': ['ADMIN', 'PASTEUR', 'RESPONSABLE'],
  '/skills-matrix': ['ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE'],
  '/team-gantt': ['ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE'],
  '/transfers': ['ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR'],
  '/transfers/new': ['ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR'],
  '/transfers/:id': ['ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR'],
  '/surveys': ['ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR', 'MEMBRE'],
  '/tickets': ['ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR', 'MEMBRE'],
  '/quest': ['ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR', 'MEMBRE'],
  '/pastoral-visits': ['ADMIN', 'PASTEUR', 'RESPONSABLE', 'FAISEUR'],

  // Rapports & suivi
  '/reports/maker': ['ADMIN', 'PASTEUR', 'FAISEUR'],
  '/reports/family': ['ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR'],
  '/reports/urgent-aid': ['ADMIN', 'PASTEUR', 'RESPONSABLE'],
  '/discipline': ['ADMIN', 'PASTEUR', 'RESPONSABLE'],
  '/members/requests': ['ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE'],

  // Espaces métiers
  '/crm/faiseur': ['ADMIN', 'PASTEUR', 'FAISEUR'],
  '/cercle-faiseurs': ['ADMIN', 'PASTEUR', 'FAISEUR'],

  // Administration — console tenant admin (G5.5 §58, parité garde ProtectedRoute scope="tenant")
  '/admin/dashboard': ['ADMIN', 'PASTEUR'],
  '/admin/members': ['ADMIN', 'PASTEUR'],
  '/admin/roles': ['ADMIN', 'PASTEUR'],
  '/admin/invitations': ['ADMIN', 'PASTEUR'],
  '/admin/spaces': ['ADMIN', 'PASTEUR'],
  '/tenant/settings': ['ADMIN', 'PASTEUR'],
  '/tenant/modules': ['ADMIN', 'PASTEUR'],
  '/tenant/join-management': ['ADMIN', 'PASTEUR'],
  '/tenant/announcements': ['ADMIN', 'PASTEUR'],
  // T-W7 : écran Propriété & délégation (F13). La garde de ROUTE est ici
  // alignée sur ADMIN/PASTEUR ; la garde de MÉTIER (qui peut céder la
  // propriété) reste côté serveur : `@authz.isTenantOwner()`.
  '/tenant/ownership': ['ADMIN', 'PASTEUR'],
  // T-W5 : réseau d'une dénomination (§1.3) — organigramme racine + enfants.
  '/tenant/organization': ['ADMIN', 'PASTEUR'],
  // SPEC_ORGANISATION_MODULABLE_V3 §6 / T-W15 — écrans « modulable » scopés tenant.
  // Le super-admin ne doit PAS joindre /tenant/organization/* (redirection V2).
  '/tenant/organization/levels': ['ADMIN', 'PASTEUR'],
  '/tenant/organization/nodes/:nodeId': ['ADMIN', 'PASTEUR'],
  '/tenant/members/:userId/roles': ['ADMIN', 'PASTEUR'],
  // LOT 2 §GR + §LB — écran « tout est paramétrable ».
  '/tenant/customization': ['ADMIN', 'PASTEUR'],

  // Administration
  '/admin/payments': ['ADMIN', 'PASTEUR'],
  '/admin/webhook-logs': ['ADMIN', 'PASTEUR'],
  '/admin/webhooks': ['ADMIN', 'PASTEUR'],
  '/admin/whatsapp': ['ADMIN', 'PASTEUR'],
  '/admin/connectors': ['ADMIN', 'PASTEUR'],
  '/admin/transfers': ['ADMIN', 'PASTEUR'],
  '/admin/settings': ['ADMIN', 'PASTEUR'],
  '/admin/modules': ['ADMIN', 'PASTEUR'],
  '/admin/menus': ['ADMIN', 'PASTEUR'],
  '/admin/pages': ['ADMIN', 'PASTEUR'],
  '/admin/custom-fields': ['ADMIN', 'PASTEUR'],
  '/admin/feedback': ['ADMIN', 'PASTEUR'],
  '/admin/dictionaries': ['ADMIN', 'PASTEUR'],
  '/admin/tenants': ['ADMIN', 'PASTEUR'],
  '/admin/notifications': ['ADMIN', 'PASTEUR'],
  '/admin/system': ['ADMIN', 'PASTEUR'],
  '/admin/integrations': ['ADMIN', 'PASTEUR'],
  '/admin/gdpr': ['ADMIN', 'PASTEUR'],
  '/admin/compliance': ['ADMIN', 'PASTEUR'],
  '/admin': ['ADMIN', 'PASTEUR'],
  '/finances': ['ADMIN', 'PASTEUR'],
  '/health-observatory': ['ADMIN', 'PASTEUR'],
  '/digital-twin': ['ADMIN', 'PASTEUR'],
  '/sermon-assistant': ['ADMIN', 'PASTEUR'],
  '/events/program': ['ADMIN', 'PASTEUR'],
  '/programs': ['ADMIN', 'PASTEUR'],
  '/events/statistics': ['ADMIN', 'PASTEUR'],
  '/prayers/spaces': ['ADMIN', 'PASTEUR'],
  '/audit': ['ADMIN', 'PASTEUR'],
  '/permissions': ['ADMIN', 'PASTEUR'],
  // T-W6 (F6) : le membre passe d'une église à une autre SANS se réinscrire.
  // Accessible à tout membre connecté : la décision transfert/adhésion est
  // prise côté backend selon la racine (`root_tenant_id`), pas ici.
  '/transfer': ['ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR', 'MEMBRE'],
  '/api-docs': ['ADMIN', 'PASTEUR'],
  '/kingdom-map': ['ADMIN', 'PASTEUR', 'RESPONSABLE'],

  // Modules accessibles à tous les rôles opérationnels
  '/search': ['ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR'],
  '/parallel-followups': ['ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR'],
  '/alerts': ['ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR'],
  '/documents': ['ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR'],
  '/map': ['ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR'],
  '/evangelism': ['ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR'],
  '/objectives': ['ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR'],
  '/visits': ['ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR'],
  '/prophetic-journal': ['ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR'],
  '/voice-reports': ['ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR'],
  '/evaluations': ['ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR'],

  // Modules ouverts à tous les rôles (opérationnels + membre)
  '/prayers/actions-de-grace': ['ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR', 'MEMBRE'],
  '/prayers': ['ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR', 'MEMBRE'],
  '/events': ['ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR', 'MEMBRE'],
  '/communications': ['ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR', 'MEMBRE'],
  '/badges': ['ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR', 'MEMBRE'],
  '/giving': ['ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR', 'MEMBRE'],
  '/tontines': ['ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR', 'MEMBRE'],
  '/trainings': ['ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR', 'MEMBRE'],
  '/appointments': ['ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR', 'MEMBRE'],

  // Nouvelles modules P1/P3
  '/currency-settings': ['ADMIN'],
  '/streaming': ['ADMIN', 'PASTEUR', 'RESPONSABLE'],
  '/broadcast': ['ADMIN', 'PASTEUR', 'RESPONSABLE'],
  '/inventory': ['ADMIN', 'PASTEUR', 'RESPONSABLE'],
  '/department-kpis': ['ADMIN', 'PASTEUR', 'RESPONSABLE'],
  '/rewards': ['ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR', 'MEMBRE'],
  '/marketplace': ['ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR', 'MEMBRE'],
  '/community': ['ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR', 'MEMBRE'],
  '/network': ['ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR', 'MEMBRE'],
  '/passport': ['ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR', 'MEMBRE'],
  '/ai-predictions': ['ADMIN', 'PASTEUR', 'RESPONSABLE'],

  // IA, Voice, Exports, Imports - accessibles à tous les rôles
  '/ai-assistant': ['ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR', 'MEMBRE'],
  '/voice-assistant': ['ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR', 'MEMBRE'],
  '/exports': ['ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR', 'MEMBRE'],
  '/import': ['ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR', 'MEMBRE'],
  '/admin/import': ['ADMIN', 'PASTEUR'],
  '/admin/space-import-export': ['ADMIN', 'PASTEUR'],
  '/admin/legacy-migration': ['ADMIN', 'PASTEUR'],

  // Nouveaux modules issus de l'audit des endpoints orphelins
  '/admin-requests': ['ADMIN', 'PASTEUR'],
  '/admin/sync-conflicts': ['ADMIN', 'PASTEUR', 'RESPONSABLE'],
  '/emergency-aid': ['ADMIN', 'PASTEUR', 'RESPONSABLE'],
  '/aid-exchange': ['ADMIN', 'PASTEUR', 'RESPONSABLE'],
  '/2fa-status': ['ADMIN', 'PASTEUR'],
  '/visit-notes/verify': ['ADMIN', 'PASTEUR', 'FAISEUR'],
  '/ai-health': ['ADMIN', 'PASTEUR'],
  '/ai-predictions/risks': ['ADMIN', 'PASTEUR', 'RESPONSABLE'],
  '/team-tasks': ['ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE'],
  '/sermon-translations': ['ADMIN', 'PASTEUR'],
  '/kpi-narrative': ['ADMIN', 'PASTEUR'],
  '/maker-tracking': ['ADMIN', 'PASTEUR', 'RESPONSABLE'],
  '/member-competences': ['ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE'],
  '/qr-checkin': ['ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR', 'MEMBRE'],
  '/network/stats': ['ADMIN', 'PASTEUR', 'RESPONSABLE'],
  '/voices': ['ADMIN', 'PASTEUR', 'CHEF_DE_FAMILLE', 'FAISEUR'],
  '/workflow': ['ADMIN', 'PASTEUR'],
  '/skill-matching': ['ADMIN', 'PASTEUR', 'RESPONSABLE'],
  '/face-recognition': ['ADMIN', 'PASTEUR'],
  '/twin-snapshot': ['ADMIN', 'PASTEUR'],
  '/reward-certificates': ['ADMIN', 'PASTEUR'],
  '/sermons': ['ADMIN', 'PASTEUR'],
  '/development-plans': ['ADMIN', 'PASTEUR', 'CHEF_DE_FAMILLE'],
  '/surveys/:id': ['ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE'],
  '/forms/:id/responses': ['ADMIN', 'PASTEUR', 'RESPONSABLE'],
  '/calendar-ical': ['ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR', 'MEMBRE'],
  '/department-reports': ['ADMIN', 'PASTEUR', 'RESPONSABLE'],
  '/family-tree': ['ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE'],
  '/announcement-schedule': ['ADMIN', 'PASTEUR'],
  '/documents/:id': ['ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR'],
  '/tickets/:id': ['ADMIN', 'PASTEUR'],
  '/testimonies/:id': ['ADMIN', 'PASTEUR'],
  '/rewards/claims': ['ADMIN', 'PASTEUR', 'FAISEUR'],
  '/users/roles': ['ADMIN'],
  '/admin/compliance-exports': ['ADMIN'],
  '/conversations': ['ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR', 'MEMBRE'],
  '/courses': ['ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR', 'MEMBRE'],
  '/equipment': ['ADMIN', 'PASTEUR', 'RESPONSABLE'],
  '/budgets': ['ADMIN', 'PASTEUR', 'RESPONSABLE'],
  // V233-V236 — pages web des contrats services (mêmes gardes que App.tsx).
  '/discipleship': ['ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR'],
  '/tasks': ['ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR'],
  '/health': ['ADMIN', 'PASTEUR', 'HEALTH_STAFF', 'HEALTH_LEAD'],
  '/finance-modules': ['ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE'],
};

function splitSegments(path: string): string[] {
  if (!path) return [];
  return path.split('?')[0].split('#')[0].split('/').filter(Boolean);
}

function matchesPattern(patternSegments: string[], pathSegments: string[]): boolean {
  if (patternSegments.length !== pathSegments.length) return false;
  return patternSegments.every(
    (seg, i) => seg.startsWith(':') || seg === pathSegments[i]
  );
}

/**
 * Rôles autorisés pour un chemin donné.
 * Retourne `null` si la route n'est pas restreinte (accessible à tous).
 */
function bestMatch<T>(registry: Record<string, T>, path: string): T | null {
  const pathSegs = splitSegments(path);
  let bestMatchKey: string | null = null;
  let bestLen = -1;
  for (const pattern of Object.keys(registry)) {
    const patSegs = splitSegments(pattern);
    if (patSegs.length > bestLen && matchesPattern(patSegs, pathSegs)) {
      bestMatchKey = pattern;
      bestLen = patSegs.length;
    }
  }
  return bestMatchKey !== null ? registry[bestMatchKey] : null;
}

export function rolesForPath(path: string): AccessRole[] | null {
  return bestMatch(ROUTE_ROLES, path);
}

/** Clés permission servant une route convertie ; `null` si la route reste par rôle. */
export function permissionsForPath(path: string): string[] | null {
  return bestMatch(ROUTE_PERMISSIONS, path);
}

/**
 * Vérifie qu'un rôle actif peut accéder à un chemin.
 * `true` si la route n'est pas restreinte ou si le rôle est autorisé.
 * Un ADMIN dispose des accès PASTEUR (équivalence métier).
 */
export function canRoleAccessPath(path: string, role: string | null | undefined): boolean {
  if (!role) return true;
  const allowed = rolesForPath(path);
  if (!allowed) return true;
  if (allowed.includes(role)) return true;
  if (role === 'ADMIN' && allowed.includes('PASTEUR')) return true;
  return false;
}

/** Filtre une liste d'items de navigation {href} selon le rôle actif. */
export function filterNavByRole<T extends { href: string }>(items: T[], role: string | null | undefined): T[] {
  return items.filter((item) => canRoleAccessPath(item.href, role));
}

/**
 * PARAMÉTRISATION PAR ÉGLISE (Phase 2-client) — accès d'abord par CLÉ SERVIE.
 * `hasPermission` est la lecture du cache de permissions servies (TenantContext) :
 * une route convertie est accordée/refusée par ses clés, sans littéral de rôle.
 * Sans checker (contexte non chargé, test hors provider) ou sans route convertie,
 * repli strict sur le registre par rôle — comportement transition identique.
 */
export function canAccessPath(
  path: string,
  role: string | null | undefined,
  hasPermission?: ((key: string) => boolean) | undefined,
): boolean {
  const keys = permissionsForPath(path);
  if (keys && hasPermission) return keys.some((k) => hasPermission(k));
  return canRoleAccessPath(path, role);
}

/** Filtre de navigation par clé servie, avec repli par rôle (voir {@link canAccessPath}). */
export function filterNavByAccess<T extends { href: string }>(
  items: T[],
  role: string | null | undefined,
  hasPermission?: ((key: string) => boolean) | undefined,
): T[] {
  return items.filter((item) => canAccessPath(item.href, role, hasPermission));
}
