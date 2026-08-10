import { createContext, useCallback, useContext, useMemo, type ReactNode } from 'react';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import api from '@/lib/api';
import { useAuth } from '@/contexts/AuthContext';
import type { MenuEntry, PlatformModule } from '@/types';

/**
 * Configuration de la plateforme pilotée par le backend :
 * - menus visibles pour l'utilisateur (filtrés par rôle + module actif)
 * - état d'activation des modules (utilisé pour le gating des routes)
 *
 * Toute modification d'administration est reflétée après refetch.
 */

interface GatingInfo {
  href: string;
  moduleKey: string;
  moduleEnabled: boolean;
}

interface PlatformConfigValue {
  menus: MenuEntry[];
  modules: PlatformModule[];
  isLoaded: boolean;
  moduleEnabled: (key: string) => boolean;
  canAccessPath: (pathname: string) => boolean;
  refetch: () => void;
}

const PlatformContext = createContext<PlatformConfigValue | null>(null);

export function PlatformProvider({ children }: { children: ReactNode }) {
  const { user, isAuthenticated, activeRole } = useAuth();
  const queryClient = useQueryClient();

  const { data: menus = [] } = useQuery({
    queryKey: ['platform', 'menus', user?.id, activeRole],
    queryFn: async () => {
      const res = await api.get('/platform/menus');
      return res.data as MenuEntry[];
    },
    enabled: !!isAuthenticated && !!user,
    staleTime: 60_000,
  });

  const { data: gating = [] } = useQuery({
    queryKey: ['platform', 'gating', user?.id, activeRole],
    queryFn: async () => {
      const res = await api.get('/platform/gating');
      return res.data as GatingInfo[];
    },
    enabled: !!isAuthenticated && !!user,
    staleTime: 60_000,
  });

  /** Retourne true si l'utilisateur peut accéder à cette route. */
  const canAccessPath = useCallback((pathname: string) => {
    if (gating.length === 0) return true; // pas d'info de gating → permissif
    // Trouve l'entrée de gating dont le href correspond (longueur max).
    let best: GatingInfo | null = null;
    for (const g of gating) {
      if (pathname === g.href || pathname.startsWith(g.href + '/')) {
        if (!best || g.href.length > best.href.length) best = g;
      }
    }
    if (!best) return true; // route non répertoriée → accessible
    return best.moduleEnabled;
  }, [gating]);

  const { data: modules = [] } = useQuery({
    queryKey: ['platform', 'modules', user?.id],
    queryFn: async () => {
      const res = await api.get('/platform/modules');
      return res.data as PlatformModule[];
    },
    enabled: !!isAuthenticated && !!user,
    staleTime: 60_000,
  });

  const moduleEnabled = useCallback(
    (key: string) => {
      const module = modules.find((m) => m.key === key);
      return module ? module.enabled : true; // module inconnu = non restreint
    },
    [modules]
  );

  const refetch = useCallback(() => {
    queryClient.invalidateQueries({ queryKey: ['platform'] });
  }, [queryClient]);

  const value = useMemo<PlatformConfigValue>(() => ({
    menus,
    modules,
    isLoaded: isAuthenticated,
    moduleEnabled,
    canAccessPath,
    refetch,
  }), [menus, modules, isAuthenticated, moduleEnabled, canAccessPath, refetch]);

  return <PlatformContext.Provider value={value}>{children}</PlatformContext.Provider>;
}

export function usePlatformConfig(): PlatformConfigValue {
  const ctx = useContext(PlatformContext);
  if (!ctx) {
    return { menus: [], modules: [], isLoaded: false, moduleEnabled: () => true, canAccessPath: () => true, refetch: () => undefined };
  }
  return ctx;
}

/** Regroupe les menus par section, en conservant l'ordre défini par l'admin. */
export function menusToSections(menus: MenuEntry[]): { title: string; items: MenuEntry[] }[] {
  const sections = new Map<string, MenuEntry[]>();
  for (const menu of menus) {
    const list = sections.get(menu.section) ?? [];
    list.push(menu);
    sections.set(menu.section, list);
  }
  return [...sections.entries()].map(([title, items]) => ({ title, items }));
}
