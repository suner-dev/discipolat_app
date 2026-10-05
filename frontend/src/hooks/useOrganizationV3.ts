// SPEC_ORGANISATION_MODULABLE_V3 §6.4 — clients d'API web (T-W10..T-W14).
//
// Regroupe les hooks de la couche « tout est modulable » :
//   - niveaux configurables (A)            → /tenant/organization/levels
//   - arbre + agrégats drill-down (E)      → /tenant/organization/tree, /nodes/{id}/aggregate, /children
//   - modules & thème par nœud (D)         → /nodes/{id}/features, /nodes/{id}/theme
//   - intitulés par scope (B)              → /roles/{id}/titles
//   - affiliations multi-nœuds (C)         → /members/{id}/assignments
//
// Les libellés de niveaux/rôles viennent de l'API (dynamiques) : on ne les
// fige jamais en dur côté client (garde-fou i18n V2/F26).

import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import api from '@/lib/api';
import { useTenant } from '@/contexts/TenantContext';

// ---------- types (miroir des payloads backend §5) ----------

export interface OrgLevel {
  id: string;
  rootTenantId: string;
  name: string;
  pluralName?: string | null;
  description?: string | null;
  depthOrder: number;
  semanticType: string;
  parentLevelId?: string | null;
  icon?: string | null;
  color?: string | null;
  branching?: boolean;
  active?: boolean;
}

export interface OrgLevelInput {
  name: string;
  pluralName?: string;
  semanticType: string;
  depthOrder: number;
  parentLevelId?: string | null;
  icon?: string;
  color?: string;
}

export interface TreeNode {
  id: string;
  parentId?: string | null;
  name: string;
  type: string;
  levelId?: string | null;
  levelName?: string | null;
  responsibleId?: string | null;
  responsibleName?: string | null;
}

export interface NodeAggregate {
  nodeId: string;
  levelName?: string | null;
  responsibleName?: string | null;
  snapshotAt?: string;
  memberCount: number;
  churchCount: number;
  leaderCount: number;
  sermonCount: number;
  prayerTopicCount: number;
  metrics?: Record<string, unknown>;
  progression?: Array<{
    snapshotAt: string;
    memberCount: number;
    churchCount: number;
    leaderCount: number;
  }>;
}

export interface ChildWithAggregate {
  nodeId: string;
  name: string;
  type: string;
  levelId?: string | null;
  responsibleId?: string | null;
  memberCount?: number;
  churchCount?: number;
  leaderCount?: number;
}

export interface NodeFeature {
  moduleCode: string;
  enabled: boolean;
  configurationJson?: Record<string, unknown>;
}

export interface NodeTheme {
  source: 'DEFAULT' | 'INHERITED' | 'OVERRIDDEN';
  theme: Record<string, unknown>;
}

export interface RoleTitleView {
  id?: string;
  nodeId?: string | null;
  label: string;
  labelPlural?: string | null;
  /** Marqueur de la ligne « résolue » appendue par l'API (fallback effectif). */
  resolved?: boolean;
  effectiveLabel?: string;
}

export interface MemberAssignment {
  id: string;
  userId: string;
  roleId: string;
  nodeId?: string | null;
  status: 'ACTIVE' | 'SUSPENDED' | 'ENDED';
  roleLabel?: string;
  nodeName?: string;
}

// ---------- A : niveaux configurables ----------

export function useOrgLevels() {
  const { currentTenant } = useTenant();
  const tenantKey = currentTenant?.id ?? 'no-tenant';
  return useQuery<OrgLevel[]>({
    queryKey: ['t', tenantKey, 'org', 'levels'],
    queryFn: async () => (await api.get('/tenant/organization/levels')).data,
  });
}

export function useCreateOrgLevel() {
  const qc = useQueryClient();
  const { currentTenant } = useTenant();
  return useMutation({
    mutationFn: async (input: OrgLevelInput) => (await api.post('/tenant/organization/levels', input)).data as OrgLevel,
    onSuccess: () => qc.invalidateQueries({ queryKey: ['t', currentTenant?.id, 'org', 'levels'] }),
  });
}

export function useUpdateOrgLevel() {
  const qc = useQueryClient();
  const { currentTenant } = useTenant();
  return useMutation({
    mutationFn: async ({ id, patch }: { id: string; patch: Partial<OrgLevelInput> }) =>
      (await api.patch(`/tenant/organization/levels/${id}`, patch)).data as OrgLevel,
    onSuccess: () => qc.invalidateQueries({ queryKey: ['t', currentTenant?.id, 'org', 'levels'] }),
  });
}

export function useDeleteOrgLevel() {
  const qc = useQueryClient();
  const { currentTenant } = useTenant();
  return useMutation({
    mutationFn: async (id: string) => api.delete(`/tenant/organization/levels/${id}`),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['t', currentTenant?.id, 'org', 'levels'] }),
  });
}

// ---------- E : arbre & drill-down ----------

export function useOrgTreeV3() {
  const { currentTenant } = useTenant();
  const tenantKey = currentTenant?.id ?? 'no-tenant';
  return useQuery<TreeNode[]>({
    queryKey: ['t', tenantKey, 'org', 'tree-v3'],
    queryFn: async () => (await api.get('/tenant/organization/tree')).data,
  });
}

export function useNodeAggregate(nodeId: string | null, refresh = false) {
  const { currentTenant } = useTenant();
  const tenantKey = currentTenant?.id ?? 'no-tenant';
  return useQuery<NodeAggregate>({
    queryKey: ['t', tenantKey, 'org', 'aggregate', nodeId, refresh],
    queryFn: async () =>
      (await api.get(`/tenant/organization/nodes/${nodeId}/aggregate`, { params: { refresh } })).data,
    enabled: !!nodeId,
  });
}

// T-W14 — assistant de création d'un nœud : niveau (A) + modules (D) à la
// création. Le backend accepte levelId + moduleCodes inline (T-B10/T-B13) via
// POST /org/nodes ; le thème s'applique ensuite (PATCH /nodes/{id}/theme).
export interface CreateNodeInput {
  name: string;
  type: string;
  parentId?: string | null;
  levelId?: string | null;
  responsibleId?: string | null;
  code?: string;
  moduleCodes?: string[];
}

export function useCreateOrgNodeV3() {
  const qc = useQueryClient();
  const { currentTenant } = useTenant();
  return useMutation({
    mutationFn: async (input: CreateNodeInput) =>
      (await api.post('/org/nodes', input)).data as TreeNode,
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['t', currentTenant?.id, 'org', 'tree-v3'] });
      qc.invalidateQueries({ queryKey: ['org'] });
    },
  });
}

export function useNodeChildren(nodeId: string | null) {
  const { currentTenant } = useTenant();
  const tenantKey = currentTenant?.id ?? 'no-tenant';
  return useQuery<ChildWithAggregate[]>({
    queryKey: ['t', tenantKey, 'org', 'children', nodeId],
    queryFn: async () => (await api.get(`/tenant/organization/nodes/${nodeId}/children`)).data,
    enabled: !!nodeId,
  });
}

// ---------- D : modules & thème par nœud ----------

export function useNodeFeatures(nodeId: string | null) {
  const { currentTenant } = useTenant();
  const tenantKey = currentTenant?.id ?? 'no-tenant';
  return useQuery<NodeFeature[]>({
    queryKey: ['t', tenantKey, 'org', 'features', nodeId],
    queryFn: async () => (await api.get(`/tenant/organization/nodes/${nodeId}/features`)).data,
    enabled: !!nodeId,
  });
}

export function useSetNodeFeatures(nodeId: string | null) {
  const qc = useQueryClient();
  const { currentTenant } = useTenant();
  return useMutation({
    mutationFn: async (modules: Array<{ code: string; enabled: boolean; configurationJson?: Record<string, unknown> }>) =>
      (await api.put(`/tenant/organization/nodes/${nodeId}/features`, { modules })).data as NodeFeature[],
    onSuccess: () => qc.invalidateQueries({ queryKey: ['t', currentTenant?.id, 'org', 'features', nodeId] }),
  });
}

export function useNodeTheme(nodeId: string | null) {
  const { currentTenant } = useTenant();
  const tenantKey = currentTenant?.id ?? 'no-tenant';
  return useQuery<NodeTheme>({
    queryKey: ['t', tenantKey, 'org', 'theme', nodeId],
    queryFn: async () => (await api.get(`/tenant/organization/nodes/${nodeId}/theme`)).data,
    enabled: !!nodeId,
  });
}

export function usePatchNodeTheme(nodeId: string | null) {
  const qc = useQueryClient();
  const { currentTenant } = useTenant();
  return useMutation({
    mutationFn: async (theme: Record<string, unknown>) =>
      (await api.patch(`/tenant/organization/nodes/${nodeId}/theme`, theme)).data as NodeTheme,
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['t', currentTenant?.id, 'org', 'theme', nodeId] });
      qc.invalidateQueries({ queryKey: ['t', currentTenant?.id, 'org', 'aggregate'] });
    },
  });
}

// ---------- B : intitulés par scope ----------

export function useRoleTitles(roleId: string | null) {
  const { currentTenant } = useTenant();
  const tenantKey = currentTenant?.id ?? 'no-tenant';
  return useQuery<RoleTitleView[]>({
    queryKey: ['t', tenantKey, 'roles', roleId, 'titles'],
    queryFn: async () => (await api.get(`/tenant/roles/${roleId}/titles`)).data,
    enabled: !!roleId,
  });
}

export function useUpsertRoleTitle() {
  const qc = useQueryClient();
  const { currentTenant } = useTenant();
  return useMutation({
    mutationFn: async ({ roleId, nodeId, label, labelPlural }: {
      roleId: string; nodeId?: string | null; label: string; labelPlural?: string;
    }) => (await api.put(`/tenant/roles/${roleId}/titles`, { nodeId, label, labelPlural })).data,
    onSuccess: () => qc.invalidateQueries({ queryKey: ['t', currentTenant?.id, 'roles'] }),
  });
}

// ---------- C : affiliations multi-nœuds ----------

export function useMemberAssignments(userId: string | null) {
  const { currentTenant } = useTenant();
  const tenantKey = currentTenant?.id ?? 'no-tenant';
  return useQuery<MemberAssignment[]>({
    queryKey: ['t', tenantKey, 'members', userId, 'assignments'],
    queryFn: async () => (await api.get(`/tenant/members/${userId}/assignments`)).data,
    enabled: !!userId,
  });
}

export function useAssignMemberRole(userId: string | null) {
  const qc = useQueryClient();
  const { currentTenant } = useTenant();
  return useMutation({
    mutationFn: async ({ roleId, nodeId }: { roleId: string; nodeId?: string | null }) =>
      (await api.post(`/tenant/members/${userId}/assignments`, { roleId, nodeId })).data,
    onSuccess: () => qc.invalidateQueries({ queryKey: ['t', currentTenant?.id, 'members', userId, 'assignments'] }),
  });
}

export function useEndMemberAssignment(userId: string | null) {
  const qc = useQueryClient();
  const { currentTenant } = useTenant();
  return useMutation({
    mutationFn: async (assignmentId: string) =>
      api.delete(`/tenant/members/${userId}/assignments/${assignmentId}`),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['t', currentTenant?.id, 'members', userId, 'assignments'] }),
  });
}
