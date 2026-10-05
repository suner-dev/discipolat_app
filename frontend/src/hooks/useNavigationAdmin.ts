import { useCallback, useMemo, useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import toast from 'react-hot-toast';
import api from '@/lib/api';
import { useI18n } from '@/i18n';
import type { AssignmentMap, NavigationGroupDto } from '@/navigation/grouping';

/**
 * LOT 2 §GR / §LB — client des groupes de navigation et du paramétrage fin.
 *
 * <p>Toute écriture invalide le cache concerné : la sidebar et le fil d'Ariane
 * lisent les mêmes clés, sinon l'admin verrait « enregistré » sans que son
 * menu change avant un rechargement manuel.
 */

export interface NavigationGroupsResponse {
  groups: NavigationGroupDto[];
  assignments: AssignmentMap;
}

/* ------------------------------------------------------------------ */
/* Groupes de navigation                                              */
/* ------------------------------------------------------------------ */

export function useNavigationGroupsAdmin() {
  return useQuery<NavigationGroupsResponse>({
    queryKey: ['navigation', 'groups', 'admin'],
    queryFn: async () => {
      const res = await api.get('/tenant/navigation/groups/admin');
      return {
        groups: Array.isArray(res.data?.groups) ? res.data.groups : [],
        assignments: (res.data?.assignments ?? {}) as AssignmentMap,
      };
    },
    staleTime: 30_000,
  });
}

export interface NavigationGroupPayload {
  key: string;
  label: string;
  description?: string;
  icon?: string;
  parentGroupId?: string | null;
  displayOrder?: number;
  roles?: string[];
  moduleKey?: string;
  enabled?: boolean;
  collapsedByDefault?: boolean;
  showCount?: boolean;
}

function useInvalidateNavigation() {
  const queryClient = useQueryClient();
  return useCallback(() => {
    // Les deux caches : celui de la sidebar et celui de l'écran d'admin.
    void queryClient.invalidateQueries({ queryKey: ['navigation', 'groups'] });
  }, [queryClient]);
}

export function useCreateNavigationGroup() {
  const invalidate = useInvalidateNavigation();
  return useMutation({
    mutationFn: async (payload: NavigationGroupPayload) => {
      await api.post('/tenant/navigation/groups', payload);
    },
    onSuccess: invalidate,
  });
}

export function useUpdateNavigationGroup() {
  const invalidate = useInvalidateNavigation();
  return useMutation({
    mutationFn: async ({ id, payload }: { id: string; payload: Partial<NavigationGroupPayload> }) => {
      await api.patch(`/tenant/navigation/groups/${id}`, payload);
    },
    onSuccess: invalidate,
  });
}

export function useDeleteNavigationGroup() {
  const invalidate = useInvalidateNavigation();
  return useMutation({
    mutationFn: async (id: string) => {
      await api.delete(`/tenant/navigation/groups/${id}`);
    },
    onSuccess: invalidate,
  });
}

export interface NavItemRef {
  itemKey?: string | null;
  href: string;
  displayOrder?: number;
}

export function useSetNavigationGroupItems() {
  const invalidate = useInvalidateNavigation();
  return useMutation({
    mutationFn: async ({ groupId, items }: { groupId: string; items: NavItemRef[] }) => {
      await api.put(`/tenant/navigation/groups/${groupId}/items`, { items });
    },
    onSuccess: invalidate,
  });
}

/* ------------------------------------------------------------------ */
/* Paramétrage fin (libellés + fonctionnalités)                        */
/* ------------------------------------------------------------------ */

export interface UiLabelSetting {
  id: string;
  /** `null` = réglage global livré par la plateforme (non modifiable ici). */
  tenantId?: string | null;
  labelKey: string;
  locale: string;
  value: string;
  description?: string;
  enabled: boolean;
  nodeId?: string | null;
}

export interface UiFeatureSettingRow {
  id: string;
  /** `null` = réglage global livré par la plateforme (non modifiable ici). */
  tenantId?: string | null;
  pageKey: string;
  featureKey: string;
  labelOverride?: string;
  enabled: boolean;
  displayOrder: number;
  moduleKey?: string;
  description?: string;
  nodeId?: string | null;
}

export interface UiCustomizationAdminView {
  labels: UiLabelSetting[];
  features: UiFeatureSettingRow[];
}

export function useUiCustomizationAdmin() {
  return useQuery<UiCustomizationAdminView>({
    queryKey: ['ui-customization', 'admin'],
    queryFn: async () => {
      const res = await api.get('/tenant/customization/admin');
      return {
        labels: Array.isArray(res.data?.labels) ? res.data.labels : [],
        features: Array.isArray(res.data?.features) ? res.data.features : [],
      };
    },
    staleTime: 30_000,
  });
}

function useInvalidateCustomization() {
  const queryClient = useQueryClient();
  return useCallback(() => {
    void queryClient.invalidateQueries({ queryKey: ['ui-customization'] });
  }, [queryClient]);
}

export interface LabelPayload {
  labelKey: string;
  locale?: string;
  value: string;
  description?: string;
  enabled?: boolean;
  nodeId?: string | null;
}

export function useUpsertUiLabel() {
  const invalidate = useInvalidateCustomization();
  return useMutation({
    mutationFn: async (payload: LabelPayload) => {
      await api.post('/tenant/customization/labels', payload);
    },
    onSuccess: invalidate,
  });
}

export function useDeleteUiLabel() {
  const invalidate = useInvalidateCustomization();
  return useMutation({
    mutationFn: async (id: string) => {
      await api.delete(`/tenant/customization/labels/${id}`);
    },
    onSuccess: invalidate,
  });
}

export interface FeaturePayload {
  pageKey: string;
  featureKey: string;
  labelOverride?: string;
  enabled?: boolean;
  displayOrder?: number;
  moduleKey?: string;
  description?: string;
  nodeId?: string | null;
}

export function useUpsertUiFeature() {
  const invalidate = useInvalidateCustomization();
  return useMutation({
    mutationFn: async (payload: FeaturePayload) => {
      await api.post('/tenant/customization/features', payload);
    },
    onSuccess: invalidate,
  });
}

export function useDeleteUiFeature() {
  const invalidate = useInvalidateCustomization();
  return useMutation({
    mutationFn: async (id: string) => {
      await api.delete(`/tenant/customization/features/${id}`);
    },
    onSuccess: invalidate,
  });
}

/**
 * Arbre des groupes, avec la profondeur, pour les rendre par récursion sans
 * recalculer la hiérarchie dans chaque composant.
 */
export interface NavigationGroupTreeNode extends NavigationGroupDto {
  depth: number;
  hrefCount: number;
}

export function buildGroupTree(
  groups: NavigationGroupDto[],
  assignments: AssignmentMap,
): NavigationGroupTreeNode[] {
  const ids = new Set(groups.map((group) => group.id));
  const countByGroup = new Map<string, number>();
  for (const groupIds of Object.values(assignments)) {
    for (const groupId of groupIds) {
      countByGroup.set(groupId, (countByGroup.get(groupId) ?? 0) + 1);
    }
  }

  const childrenOf = new Map<string, NavigationGroupDto[]>();
  const roots: NavigationGroupDto[] = [];
  for (const group of groups) {
    // Un parent absent de la sélection devient racine : rien ne devient
    // invisible dans l'écran d'admin.
    if (group.parentGroupId && ids.has(group.parentGroupId)) {
      const bucket = childrenOf.get(group.parentGroupId) ?? [];
      bucket.push(group);
      childrenOf.set(group.parentGroupId, bucket);
    } else {
      roots.push(group);
    }
  }

  const byOrder = (a: NavigationGroupDto, b: NavigationGroupDto) =>
    (a.displayOrder ?? 0) - (b.displayOrder ?? 0) ||
    (a.label ?? '').localeCompare(b.label ?? '', 'fr');

  const flatten = (list: NavigationGroupDto[], depth: number): NavigationGroupTreeNode[] =>
    [...list].sort(byOrder).flatMap((group) => [
      {
        ...group,
        depth,
        hrefCount: countByGroup.get(group.id) ?? 0,
      },
      ...flatten(childrenOf.get(group.id) ?? [], depth + 1),
    ]);

  return flatten(roots, 0);
}

/** Libellés i18n de l'écran, résolus une seule fois. */
export function useNavigationAdminLabels() {
  const { t } = useI18n();
  return useMemo(
    () => ({
      title: t('navigation.admin.title'),
      subtitle: t('navigation.admin.subtitle'),
      groupsTitle: t('navigation.admin.groupsTitle'),
      labelsTitle: t('navigation.admin.labelsTitle'),
      featuresTitle: t('navigation.admin.featuresTitle'),
      key: t('navigation.admin.key'),
      label: t('navigation.admin.label'),
      parent: t('navigation.admin.parent'),
      icon: t('navigation.admin.icon'),
      collapsed: t('navigation.admin.collapsed'),
      showCount: t('navigation.admin.showCount'),
      locale: t('navigation.admin.locale'),
      anyLocale: t('navigation.admin.anyLocale'),
      create: t('navigation.admin.create'),
      saved: t('navigation.admin.saved'),
      deleted: t('navigation.admin.deleted'),
      error: t('navigation.admin.error'),
      deleteConfirm: t('navigation.admin.deleteConfirm'),
      emptyGroups: t('navigation.admin.emptyGroups'),
      rootGroup: t('navigation.admin.rootGroup'),
    }),
    [t],
  );
}