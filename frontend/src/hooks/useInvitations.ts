// B4 — Accès aux invitations d'un tenant (constat F3).
//
// Ce que le backend livre réellement (InvitationController) :
// - POST   /admin/invitations            -> 201 { invitationId, invitationLink, emailSent,
//                                             requiresTenantSwitch, … } ou, si le compte
//                                             existe déjà dans le tenant, 201
//                                             { invitedUserId, crossTenantIdentity }
// - GET    /admin/invitations?page&size&status&q
//                                          -> PageResponse { content, page, size,
//                                             totalElements, totalPages }
//                                          -> liste simple SI `page` est absent
//                                          (rétro-compatibilité conservée par le backend)
// - POST   /admin/invitations/{id}/resend -> { invitationLink, emailSent, expiresAt }
// - DELETE /admin/invitations/{id}
//
// Deux décisions de conception, toutes deux vérifiables ici :
// 1. La réponse de liste est **validée** (zod) et normalisée en `Page`. Le
//    backend renvoie deux formes selon la présence de `page` : on ne doit pas
//    laisser une dérive casser l'écran, ni faire confiance à un `as`.
// 2. Les clés de cache sont **préfixées par le tenant** (cf. `useTenant`).
//    Sans cela, un changement d'église pourrait afficher pendant 5 min
//    (`staleTime` global) les invitations de l'église précédente.

import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { AxiosError } from 'axios';
import { z } from 'zod';
import api from '@/lib/api';
import { useTenant } from '@/contexts/TenantContext';

// ── Schémas de réponse (le contrat, pas une supposition) ──────────────────

const InvitationSchema = z.object({
  id: z.string().uuid(),
  email: z.string(),
  role: z.string(),
  status: z.string(),
  scopeType: z.string().optional(),
  scopeId: z.string().nullable().optional(),
  organizationNodeId: z.string().nullable().optional(),
  organizationNodeName: z.string().nullable().optional(),
  createdAt: z.string(),
  expiresAt: z.string(),
  acceptedAt: z.string().nullable().optional(),
  tenantName: z.string().optional(),
});
export type Invitation = z.infer<typeof InvitationSchema>;

const PageResponseSchema = z.object({
  content: z.array(InvitationSchema),
  page: z.number(),
  size: z.number(),
  totalElements: z.number(),
  totalPages: z.number(),
});

/** Deux formes possibles côté backend : PageResponse (avec `page`) ou tableau. */
const ListResponseSchema = z.union([PageResponseSchema, z.array(InvitationSchema)]);

const CreateResponseSchema = z
  .object({
    success: z.boolean(),
    // Invitation créée
    invitationId: z.string().optional(),
    invitationLink: z.string().optional(),
    emailSent: z.boolean().optional(),
    requiresTenantSwitch: z.boolean().optional(),
    // Membre ajouté directement (compte déjà connu dans ce tenant)
    invitedUserId: z.string().optional(),
    crossTenantIdentity: z.boolean().optional(),
    message: z.string().optional(),
  })
  .passthrough();

const ResendResponseSchema = z
  .object({
    success: z.boolean(),
    invitationLink: z.string(),
    emailSent: z.boolean(),
    expiresAt: z.string(),
    message: z.string().optional(),
  })
  .passthrough();

export type CreateInvitationResult = z.infer<typeof CreateResponseSchema>;
export type ResendInvitationResult = z.infer<typeof ResendResponseSchema>;

export interface InvitationPage {
  content: Invitation[];
  page: number;
  totalPages: number;
  totalElements: number;
}

export type ScopeType = 'TENANT' | 'ORGANIZATION';

export interface InvitationFilters {
  page: number;
  size: number;
  status?: string;
  q?: string;
}

// ── Clés de cache (préfixées par tenant — voir en-tête) ───────────────────

const useInvitationKeys = () => {
  const { currentTenant } = useTenant();
  const tenantKey = currentTenant?.id ?? 'no-tenant';
  return {
    all: ['t', tenantKey, 'admin', 'invitations'] as const,
    list: (filters: InvitationFilters) =>
      ['t', tenantKey, 'admin', 'invitations', 'list', filters] as const,
  };
};

/** Un 4xx n'est pas transitoire : le rejouer retarderait l'affichage de l'erreur. */
export function shouldRetry(failureCount: number, error: unknown): boolean {
  const status = (error as AxiosError | undefined)?.response?.status;
  if (status !== undefined && status >= 400 && status < 500) return false;
  return failureCount < 1;
}

// ── Liste ────────────────────────────────────────────────────────────────

export function useInvitations(filters: InvitationFilters) {
  const keys = useInvitationKeys();
  return useQuery({
    queryKey: keys.list(filters),
    queryFn: async (): Promise<InvitationPage> => {
      const params: Record<string, string | number> = { page: filters.page, size: filters.size };
      if (filters.status) params.status = filters.status;
      // Le backend ignore une recherche de moins de 2 caractères : on ne l'envoie
      // pas, pour ne pas faire croire à un filtre actif qui ne filtre pas.
      if (filters.q && filters.q.trim().length >= 2) params.q = filters.q.trim();

      const response = await api.get('/admin/invitations', { params });
      const parsed = ListResponseSchema.parse(response.data);
      if (Array.isArray(parsed)) {
        return { content: parsed, page: 0, totalPages: 1, totalElements: parsed.length };
      }
      return {
        content: parsed.content,
        page: parsed.page,
        totalPages: parsed.totalPages,
        totalElements: parsed.totalElements,
      };
    },
    placeholderData: (previous) => previous,
    retry: shouldRetry,
  });
}

// ── Création ─────────────────────────────────────────────────────────────

export interface CreateInvitationInput {
  email: string;
  role: string;
  scopeType: ScopeType;
  organizationNodeId?: string;
}

export function useCreateInvitation() {
  const qc = useQueryClient();
  const keys = useInvitationKeys();
  return useMutation({
    mutationFn: async (input: CreateInvitationInput) => {
      // Le backend n'attend QUE ces clés : `scopeId` n'est pas utilisé à la
      // création, le nœud d'organisation suffit.
      const body: Record<string, string> = {
        email: input.email,
        role: input.role,
        scopeType: input.scopeType,
      };
      if (input.scopeType === 'ORGANIZATION' && input.organizationNodeId) {
        body.organizationNodeId = input.organizationNodeId;
      }
      const response = await api.post('/admin/invitations', body);
      return CreateResponseSchema.parse(response.data);
    },
    onSuccess: () => qc.invalidateQueries({ queryKey: keys.all }),
  });
}

// ── Renvoi ───────────────────────────────────────────────────────────────

export function useResendInvitation() {
  const qc = useQueryClient();
  const keys = useInvitationKeys();
  return useMutation({
    mutationFn: async (id: string) => {
      const response = await api.post(`/admin/invitations/${id}/resend`);
      return ResendResponseSchema.parse(response.data);
    },
    onSuccess: () => qc.invalidateQueries({ queryKey: keys.all }),
  });
}

// ── Annulation ───────────────────────────────────────────────────────────

export function useCancelInvitation() {
  const qc = useQueryClient();
  const keys = useInvitationKeys();
  return useMutation({
    mutationFn: async (id: string) => {
      await api.delete(`/admin/invitations/${id}`);
      return id;
    },
    onSuccess: () => qc.invalidateQueries({ queryKey: keys.all }),
  });
}

// ── Rôles assignables (consommation de GET /admin/roles/overview) ─────────

const RoleSchema = z.object({
  id: z.string().uuid(),
  key: z.string().min(1),
  label: z.string().min(1),
  system: z.boolean().optional(),
  priority: z.number().optional(),
});
export type AssignableRole = z.infer<typeof RoleSchema>;

/**
 * Repli built from the SYSTEM roles actually seeded by the migration
 * V135__create_multi_tenant_core_tables.sql. It is deliberately the backend's
 * truth, not a list invented by the UI: an invitation whose `role` is absent
 * from the `roles` table is refused at runtime
 * (InvitationService → roleRepository.findByTenantIdAndKey … orElse 400).
 */
export const FALLBACK_ROLES: readonly AssignableRole[] = [
  { id: 'f-tenant-owner', key: 'TENANT_OWNER', label: 'Propriétaire' },
  { id: 'f-tenant-admin', key: 'TENANT_ADMIN', label: 'Administrateur' },
  { id: 'f-church-admin', key: 'CHURCH_ADMIN', label: 'Admin église' },
  { id: 'f-church-leader', key: 'CHURCH_LEADER', label: 'Leader église' },
  { id: 'f-dept-admin', key: 'DEPARTMENT_ADMIN', label: 'Admin département' },
  { id: 'f-dept-leader', key: 'DEPARTMENT_LEADER', label: 'Leader département' },
  { id: 'f-family-leader', key: 'FAMILY_LEADER', label: 'Chef de famille' },
  { id: 'f-disciple-maker', key: 'DISCIPLE_MAKER', label: 'Faiseur de disciples' },
  { id: 'f-member', key: 'MEMBER', label: 'Membre' },
  { id: 'f-guest', key: 'GUEST', label: 'Invité' },
];

/**
 * Rôles que l'on peut attribuer via une invitation, lus depuis l'API.
 * L'API est la seule autorité : un tenant peut définir ses propres rôles
 * (RoleManagementController → /admin/roles/overview = rôles système + rôles
 * custom du tenant). Une liste en dur dans l'écran ne peut pas les connaître.
 */
export function useAssignableRoles() {
  const { currentTenant } = useTenant();
  const tenantKey = currentTenant?.id ?? 'no-tenant';

  return useQuery({
    queryKey: ['t', tenantKey, 'admin', 'roles'] as const,
    queryFn: async (): Promise<AssignableRole[]> => {
      const response = await api.get('/admin/roles/overview');
      return z.array(RoleSchema).parse(response.data);
    },
    staleTime: 5 * 60 * 1000,
    retry: shouldRetry,
  });
}
