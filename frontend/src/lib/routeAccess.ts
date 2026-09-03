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

  // Nouveaux modules issus de l'audit des endpoints orphelins
  '/admin-requests': ['ADMIN', 'PASTEUR'],
  '/emergency-aid': ['ADMIN', 'PASTEUR', 'RESPONSABLE'],
  '/aid-exchange': ['ADMIN', 'PASTEUR', 'RESPONSABLE'],
  '/2fa-status': ['ADMIN', 'PASTEUR'],
  '/visit-notes/verify': ['ADMIN', 'PASTEUR', 'FAISEUR'],
  '/ai-health': ['ADMIN', 'PASTEUR'],
  '/ai-predictions/risks': ['ADMIN', 'PASTEUR', 'RESPONSABLE'],
  '/team-tasks': ['ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE'],
  '/sermon-translations': ['ADMIN', 'PASTEUR'],
  '/admin/import': ['ADMIN', 'PASTEUR'],
  '/kpi-narrative': ['ADMIN', 'PASTEUR'],
  '/maker-tracking': ['ADMIN', 'PASTEUR', 'RESPONSABLE'],
  '/member-competences': ['ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE'],
  '/qr-checkin': ['ADMIN', 'PASTEUR', 'RESPONSABLE', 'FAISEUR'],
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
export function rolesForPath(path: string): AccessRole[] | null {
  const pathSegs = splitSegments(path);
  let bestMatch: string | null = null;
  let bestLen = -1;
  for (const pattern of Object.keys(ROUTE_ROLES)) {
    const patSegs = splitSegments(pattern);
    if (patSegs.length > bestLen && matchesPattern(patSegs, pathSegs)) {
      bestMatch = pattern;
      bestLen = patSegs.length;
    }
  }
  return bestMatch ? ROUTE_ROLES[bestMatch] : null;
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
