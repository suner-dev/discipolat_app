// B4 — Nœuds d'organisation pour le sélecteur de portée d'une invitation.
//
// Source unique : GET /admin/org/tree (OrganizationManagementController), qui
// renvoie { root, nodes[], childrenByParent }. On consomme `nodes` : la page
// n'a pas besoin de reconstruire l'arbre, seulement de proposer les nœuds.
//
// Pourquoi une requête séparée et non un import du module « organisation » :
// l'invitation ne manipule pas l'arborescence, elle en choisit un nœud. Une
// requête ciblée, validée, avec repli silencieux : si l'arborescence est
// indisponible, l'invitation au niveau de l'église reste possible.

import { useQuery } from '@tanstack/react-query';
import { z } from 'zod';
import api from '@/lib/api';
import { useTenant } from '@/contexts/TenantContext';

const OrgNodeSchema = z.object({
  id: z.string().uuid(),
  name: z.string(),
  type: z.string().optional(),
  status: z.string().optional(),
  parentId: z.string().nullable().optional(),
  level: z.number().nullable().optional(),
});

const OrgTreeSchema = z.object({
  root: OrgNodeSchema.nullable().optional(),
  nodes: z.array(OrgNodeSchema),
  childrenByParent: z.record(z.string(), z.array(OrgNodeSchema)).optional(),
});

export type OrgNode = z.infer<typeof OrgNodeSchema>;

export function useOrgNodes() {
  const { currentTenant } = useTenant();
  const tenantKey = currentTenant?.id ?? 'no-tenant';

  return useQuery({
    queryKey: ['t', tenantKey, 'org', 'tree'] as const,
    queryFn: async (): Promise<OrgNode[]> => {
      const response = await api.get('/admin/org/tree');
      return OrgTreeSchema.parse(response.data).nodes;
    },
    // L'arborescence change rarement et l'invitation doit rester utilisable
    // même si cet appel échoue : on n'insiste pas.
    retry: 1,
    staleTime: 5 * 60 * 1000,
  });
}
