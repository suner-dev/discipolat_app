import { useQuery } from '@tanstack/react-query';
import api from '@/lib/api';
import type { AssignmentMap, NavigationGroupDto } from './grouping';

/**
 * LOT 2 §GR — groupes d'onglets du menu.
 *
 * <p>Le backend reste la source de vérité quand il répond, mais un échec réseau
 * ne doit jamais casser la navigation : `groups` retombe alors à `[]`, ce qui
 * fait basculer {@link buildGroupedNav} sur son mode dégradé (un groupe par
 * section). L'utilisateur garde donc TOUJOURS un menu utilisable — c'est le
 * même principe que le repli `configMenus → navForRole` de la sidebar.
 *
 * <p>La requête est portée par l'identité + le rôle : un changement de rôle doit
 * refaire le calcul, sinon un membre verrait les groupes d'un pasteur.
 */
export interface NavigationGroupsResponse {
  groups: NavigationGroupDto[];
  assignments: AssignmentMap;
}

const EMPTY: NavigationGroupsResponse = { groups: [], assignments: {} };

export function useNavigationGroups(
  user: { id?: string | number | null; activeRole?: string | null; role?: string | null } | null | undefined,
  enabled = true,
) {
  const { data } = useQuery<NavigationGroupsResponse>({
    queryKey: ['navigation', 'groups', user?.id ?? null, user?.activeRole ?? user?.role ?? null],
    queryFn: async () => {
      const res = await api.get('/tenant/navigation/groups');
      return {
        groups: Array.isArray(res.data?.groups) ? res.data.groups : [],
        assignments: (res.data?.assignments ?? {}) as AssignmentMap,
      };
    },
    // La forme du menu bouge rarement ; 5 min évitent un aller-retour à chaque
    // changement de page tout en gardant un délai de rafraîchissement court.
    staleTime: 5 * 60 * 1000,
    retry: 1,
    enabled,
  });

  return {
    groups: data?.groups ?? EMPTY.groups,
    assignments: data?.assignments ?? EMPTY.assignments,
    /** Vrai quand le backend n'a rien renvoyé : on applique le mode dégradé. */
    isDegraded: data === undefined,
  };
}