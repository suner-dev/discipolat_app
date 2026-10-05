import {
  createContext,
  useCallback,
  useContext,
  useMemo,
  useState,
  type ReactNode,
} from 'react';
import { navForRole, PLATFORM_NAV } from '@/workspaces';
import { filterNavByRole } from '@/lib/routeAccess';
import { usePlatformConfig, menusToSections } from '@/contexts/PlatformContext';
import { resolveIcon } from '@/lib/menuIcons';
import type { LucideIcon } from 'lucide-react';
import type { MenuEntry } from '@/types';
import {
  buildGroupedNav,
  pathToHref,
  type GroupedNavNode,
  type NavEntry,
  type NavSection,
} from './grouping';
import { useNavigationGroups } from './useNavigationGroups';

/**
 * LOT 2 §GR — source unique de la navigation groupée.
 *
 * <p>La forme du menu est calculée **une seule fois** par ce provider, puis
 * consommée par la barre latérale comme par le fil d'Ariane. Avant, chaque
 * composant refaisait le calcul de son côté : deux passes pouvait diverger et
 * afficher deux menus différents sur le même écran.
 *
 * <p>Ordre de résolution des entrées (inchangé, cf. `Sidebar`) : menus pilotés
 * par la configuration backend s'ils existent, sinon navigation statique du
 * rôle actif. Le filtre par rôle est **toujours** appliqué dans les deux cas —
 * sans quoi un compte FAISEUR voyait des menus Responsable qui rebroussaient
 * chemin vers son propre espace (boutons morts).
 */
interface NavigationContextValue {
  nodes: GroupedNavNode[];
  /** Vrai quand le backend n'a rien renvoyé : regroupement par section. */
  isDegraded: boolean;
  isPlatformAdmin: boolean;
  openState: Record<string, boolean>;
  onToggleGroup: (id: string, open: boolean) => boolean;
  /** Fil de groupes menant à une route, pour le fil d'Ariane. */
  trailFor: (pathname: string) => GroupedNavNode[];
}

const NavigationContext = createContext<NavigationContextValue | null>(null);

interface NavigationProviderProps {
  user: {
    platformSuperAdmin?: boolean;
    activeRole?: string | null;
    role?: string | null;
  } | null;
  children: ReactNode;
}

export function NavigationProvider({ user, children }: NavigationProviderProps) {
  const { menus: configMenus } = usePlatformConfig();
  const [openState, setOpenState] = useState<Record<string, boolean>>({});

  const onToggleGroup = useCallback((id: string, open: boolean) => {
    setOpenState((previous) => ({ ...previous, [id]: open }));
    return open;
  }, []);

  const activeRole = user?.activeRole || user?.role || 'FAISEUR';
  const isPlatformAdmin = user?.platformSuperAdmin === true;

  const { groups, assignments, isDegraded } = useNavigationGroups(user, !isPlatformAdmin);

  const sections = useMemo<NavSection[]>(() => {
    if (isPlatformAdmin) {
      return PLATFORM_NAV as unknown as NavSection[];
    }
    if (configMenus.length > 0) {
      const fromConfig = menusToSections(configMenus).map((section) => ({
        title: section.title,
        items: filterNavByRole(
          section.items.map((menu: MenuEntry) => ({
            name: menu.label,
            href: menu.href,
            icon: resolveIcon(menu.icon) as LucideIcon,
            subtitle: '',
          })),
          activeRole,
        ) satisfies NavEntry[],
      }));
      const nonEmpty = fromConfig.filter((section) => section.items.length > 0);
      // Si la configuration ne laisse aucun menu accessible pour ce rôle, on
      // replie sur la navigation statique — le menu ne doit jamais être vide.
      if (nonEmpty.length > 0) return nonEmpty;
    }
    return navForRole(user as never, activeRole).map((section) => ({
      title: section.title,
      items: filterNavByRole(section.items as unknown as NavEntry[], activeRole),
    }));
  }, [configMenus, isPlatformAdmin, activeRole, user]);

  const nodes = useMemo(
    () =>
      buildGroupedNav({
        sections,
        groups: isPlatformAdmin ? [] : groups,
        assignments: isPlatformAdmin ? {} : assignments,
      }).nodes,
    [sections, groups, assignments, isPlatformAdmin],
  );

  const trailFor = useCallback(
    (pathname: string) => pathToHref(nodes, pathname) ?? [],
    [nodes],
  );

  const value = useMemo<NavigationContextValue>(
    () => ({
      nodes,
      isDegraded,
      isPlatformAdmin,
      openState,
      onToggleGroup,
      trailFor,
    }),
    [nodes, isDegraded, isPlatformAdmin, openState, onToggleGroup, trailFor],
  );

  return <NavigationContext.Provider value={value}>{children}</NavigationContext.Provider>;
}

/**
 * Consomme la navigation groupée.
 *
 * <p>Hors provider — cas d'un composant rendu seul (test unitaire du
 * `Sidebar`, story, rendu isolé) — on **recalcule** une navigation cohérente à
 * partir de l'utilisateur fourni, plutôt que de renvoyer le menu d'un rôle par
 * défaut. Sans cela, un composant testé hors layout afficherait toujours le menu
 * ADMIN et les tests de rôles become faux positifs.
 */
export function useGroupedNavigation(
  user?: { activeRole?: string | null; role?: string | null; platformSuperAdmin?: boolean } | null,
): NavigationContextValue {
  const context = useContext(NavigationContext);
  const activeRole = user?.activeRole || user?.role || 'ADMIN';

  const fallbackNodes = useMemo(
    () =>
      buildGroupedNav({
        sections: navForRole(user as never, activeRole).map((section) => ({
          title: section.title,
          items: section.items as unknown as NavEntry[],
        })),
      }).nodes,
    [activeRole, user],
  );

  if (context) return context;
  return {
    nodes: fallbackNodes,
    isDegraded: true,
    isPlatformAdmin: user?.platformSuperAdmin === true,
    openState: {},
    onToggleGroup: () => false,
    trailFor: (pathname: string) => pathToHref(fallbackNodes, pathname) ?? [],
  };
}