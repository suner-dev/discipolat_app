/**
 * LOT 2 §GR — construction de la navigation groupée.
 *
 * <p>Fichier **pur** : aucune dépendance React, aucun accès réseau. Toute la
 * logique de regroupement est ici pour être testée unitairement — c'est
 * aujourd'hui 34 barres d'onglets dupliquées dans l'application, et la
 * regroupement doit être prouvé, pas espéré.
 *
 * <p><b>Principe non négociable : RIEN NE DISPARAÎT.</b> Une entrée de menu
 * qui n'est affectée à aucun groupe est rendue dans un groupe « résiduel »
 * (les sections d'origine), jamais perdue. Supprimer un groupe ne retire donc
 * aucune fonctionnalité — c'est ce qui rend le regroupement sans risque.
 */

import type { LucideIcon } from 'lucide-react';

/** Entrée de menu telle que la produit la sidebar (statique ou backend). */
export interface NavEntry {
  name: string;
  href: string;
  icon: LucideIcon;
  subtitle: string;
  /** Section d'origine (`workspaces.ts` ou `menu_entries.section`). */
  section?: string;
}

/** Section d'origine, conservée pour le rendu résiduel. */
export interface NavSection {
  title: string;
  items: NavEntry[];
}

/** Groupe tel que renvoyé par `GET /tenant/navigation/groups`. */
export interface NavigationGroupDto {
  id: string;
  tenantId?: string | null;
  key: string;
  label: string;
  description?: string | null;
  icon?: string | null;
  parentGroupId?: string | null;
  displayOrder?: number;
  roles?: string[];
  moduleKey?: string | null;
  enabled?: boolean;
  collapsedByDefault?: boolean;
  showCount?: boolean;
}

/** Affectations `href -> [groupId, …]`. */
export type AssignmentMap = Record<string, string[]>;

export interface GroupedNavNode {
  /** `group` = groupe configurable ; `section` = section d'origine résiduelle. */
  kind: 'group' | 'section';
  id: string;
  key: string;
  label: string;
  icon?: string | null;
  description?: string | null;
  collapsedByDefault: boolean;
  showCount: boolean;
  /** Entrées directement portées par ce nœud. */
  items: NavEntry[];
  /** Sous-groupes (imbrication). */
  children: GroupedNavNode[];
  /** Vrai si le nœud vient d'une section d'origine et non d'un groupe configuré. */
  residual: boolean;
}

export interface BuildGroupedNavOptions {
  sections: NavSection[];
  groups?: NavigationGroupDto[];
  assignments?: AssignmentMap;
}

export interface BuildGroupedNavResult {
  nodes: GroupedNavNode[];
  /** Total d'entrées en entrée et en sortie — doit être identique. */
  totalIn: number;
  totalOut: number;
  /** Entrées qui n'ont pu être affectées à aucun groupe. */
  unassigned: NavEntry[];
}

const FALLBACK_ICON = 'CircleDot';

/** Minuscules, sans accents, espaces compactés — pour comparer libellés et clés. */
export function normalizeLabel(value?: string | null): string {
  if (!value) return '';
  return value
    .normalize('NFD')
    .replace(/\p{M}/gu, '')
    .trim()
    .toLowerCase();
}

/** « Vie de l'église » → « vie-de-l-eglise » (slug stable pour une clé). */
export function slugify(value: string): string {
  return normalizeLabel(value)
    .replace(/[^a-z0-9]+/g, '-')
    .replace(/^-+|-+$/g, '')
    .slice(0, 60);
}

/**
 * Regroupe les entrées d'un menu en groupes.
 *
 * <p>Ordre de priorité d'affectation d'une entrée :
 * <ol>
 *   <li>une <b>affectation explicite</b> (`assignments[href]`) ;</li>
 *   <li>sinon le groupe dont la clé ou le libellé correspond à la
 *       <b>section</b> d'origine de l'entrée ;</li>
 *   <li>sinon la section d'origine reste porteuse (groupe résiduel).</li>
 * </ol>
 */
export function buildGroupedNav(options: BuildGroupedNavOptions): BuildGroupedNavResult {
  const { sections, groups = [], assignments = {} } = options;

  // 1. Aplatir les sections en entrées, en mémorisant la section d'origine.
  const entries: Array<NavEntry & { sectionTitle: string }> = [];
  for (const section of sections) {
    for (const item of section.items ?? []) {
      entries.push({ ...item, sectionTitle: item.section ?? section.title });
    }
  }
  const totalIn = entries.length;

  // 2. Si aucun groupe n'est configuré, on synthétise un groupe par section :
  //    l'amélioration est immédiate, sans aucune configuration, et réversible.
  const effectiveGroups: NavigationGroupDto[] = groups.length > 0
    ? groups
    : sections.map((section, index) => ({
        id: `section:${section.title}`,
        key: slugify(section.title) || `section-${index}`,
        label: section.title,
        icon: null,
        parentGroupId: null,
        displayOrder: index,
        collapsedByDefault: true,
        showCount: false,
        residual: true,
      } as NavigationGroupDto));

  // 3. Index des groupes par id et par identifiant textuel (clé/libellé normalisés).
  const byId = new Map<string, NavigationGroupDto>();
  for (const group of effectiveGroups) byId.set(group.id, group);

  const byLabel = new Map<string, NavigationGroupDto[]>();
  for (const group of effectiveGroups) {
    for (const candidate of [group.key, group.label]) {
      const norm = normalizeLabel(candidate);
      if (!norm) continue;
      const bucket = byLabel.get(norm) ?? [];
      bucket.push(group);
      byLabel.set(norm, bucket);
    }
  }

  /** Profondeur d'un groupe dans l'arbre (pour départager les homonymes). */
  const depthOf = (group: NavigationGroupDto): number => {
    let depth = 0;
    let cursor = group.parentGroupId;
    const guard = new Set<string>();
    while (cursor && byId.has(cursor) && !guard.has(cursor)) {
      guard.add(cursor);
      depth += 1;
      cursor = byId.get(cursor)?.parentGroupId ?? null;
    }
    return depth;
  };

  /** Le groupe le plus SPÉCIFIQUE dont le libellé/clé correspond. */
  const matchGroupFor = (sectionTitle: string): NavigationGroupDto | undefined => {
    const candidates = byLabel.get(normalizeLabel(sectionTitle)) ?? [];
    if (candidates.length === 0) return undefined;
    return [...candidates].sort(
      (a, b) =>
        depthOf(b) - depthOf(a) ||
        (a.key ?? '').localeCompare(b.key ?? '', 'fr'),
    )[0];
  };

  // 4. Affecter chaque entrée. Une entrée explicitement affectée à un groupe
  //    qui n'existe plus (groupe supprimé côté serveur) RETOMBE en section
  //    résiduelle — elle ne doit jamais disparaître.
  const itemsByGroup = new Map<string, NavEntry[]>();
  const residualBySection = new Map<string, NavEntry[]>();
  const unassigned: NavEntry[] = [];

  const pushItem = (map: Map<string, NavEntry[]>, key: string, entry: NavEntry) => {
    const bucket = map.get(key) ?? [];
    bucket.push(entry);
    map.set(key, bucket);
  };

  const seenHrefs = new Set<string>();
  for (const entry of entries) {
    if (seenHrefs.has(entry.href)) continue; // garde-fou : une URL = une entrée
    seenHrefs.add(entry.href);

    const explicit = (assignments[entry.href] ?? []).find((groupId) => byId.has(groupId));
    if (explicit) {
      pushItem(itemsByGroup, explicit, entry);
      continue;
    }

    const sectionNorm = normalizeLabel(entry.sectionTitle);
    const match = sectionNorm ? matchGroupFor(entry.sectionTitle) : undefined;
    if (match) {
      pushItem(itemsByGroup, match.id, entry);
      continue;
    }

    pushItem(residualBySection, entry.sectionTitle, entry);
    unassigned.push(entry);
  }

  // 5. Construire l'arbre. Un groupe dont le parent est absent de la sélection
  //    devient racine (zéro perte, zéro orphelin invisible).
  const childrenByParent = new Map<string, NavigationGroupDto[]>();
  const roots: NavigationGroupDto[] = [];
  for (const group of effectiveGroups) {
    const parentId = group.parentGroupId;
    if (parentId && byId.has(parentId)) {
      const bucket = childrenByParent.get(parentId) ?? [];
      bucket.push(group);
      childrenByParent.set(parentId, bucket);
    } else {
      roots.push(group);
    }
  }

  const sortGroups = (list: NavigationGroupDto[]): NavigationGroupDto[] =>
    [...list].sort(
      (a, b) =>
        (a.displayOrder ?? 0) - (b.displayOrder ?? 0) ||
        (a.label ?? '').localeCompare(b.label ?? '', 'fr'),
    );

  const buildNode = (group: NavigationGroupDto, seen: Set<string>): GroupedNavNode => {
    const items = itemsByGroup.get(group.id) ?? [];
    const nodeChildren = seen.has(group.id)
      ? []
      : sortGroups(childrenByParent.get(group.id) ?? []).map((child) =>
          buildNode(child, new Set([...seen, group.id])),
        );
    return {
      kind: 'group',
      id: group.id,
      key: group.key,
      label: group.label,
      icon: group.icon ?? null,
      description: group.description ?? null,
      collapsedByDefault: group.collapsedByDefault ?? true,
      showCount: group.showCount ?? false,
      items,
      children: nodeChildren,
      residual: false,
    };
  };

  const nodes: GroupedNavNode[] = sortGroups(roots).map((group) => buildNode(group, new Set()));

  // 6. Sections résiduelles. Une section peut avoir été partiellement drainée
  //    vers un groupe configuré ; ses restes sont alors rattachés à CE groupe
  //    plutôt que d'afficher un doublon « Pilotage / Pilotage ». S'il n'existe
  //    aucun groupe correspondant, la section reste porteuse à son propre
  //    niveau, rendue après les groupes.
  const nodeById = new Map<string, GroupedNavNode>();
  const collect = (list: GroupedNavNode[]) => {
    for (const node of list) {
      nodeById.set(node.id, node);
      collect(node.children);
    }
  };
  collect(nodes);

  const residualNodes: GroupedNavNode[] = [];
  for (const section of sections) {
    const leftovers = residualBySection.get(section.title);
    if (!leftovers || leftovers.length === 0) continue;

    const match = matchGroupFor(section.title);
    const existing = match ? nodeById.get(match.id) : undefined;
    if (existing) {
      existing.items = [...existing.items, ...leftovers];
      continue;
    }

    residualNodes.push({
      kind: 'section',
      id: `section:${section.title}`,
      key: slugify(section.title),
      label: section.title,
      icon: FALLBACK_ICON,
      description: null,
      collapsedByDefault: true,
      showCount: false,
      items: leftovers,
      children: [],
      residual: true,
    });
  }

  const allNodes = [...nodes, ...residualNodes];
  const countItems = (list: GroupedNavNode[]): number =>
    list.reduce((sum, node) => sum + node.items.length + countItems(node.children), 0);

  return {
    nodes: allNodes,
    totalIn,
    totalOut: countItems(allNodes),
    unassigned,
  };
}

/**
 * Un groupe doit-il s'ouvrir tout seul parce que la route active est à
 * l'intérieur ? Sans cela l'utilisateur ne voit plus où il se trouve.
 */
export function containsHref(node: GroupedNavNode, pathname: string): boolean {
  if (node.items.some((item) => item.href === pathname)) return true;
  if (node.children.some((child) => containsHref(child, pathname))) return true;
  return node.items.some(
    (item) => pathname === item.href || pathname.startsWith(`${item.href}/`),
  );
}

/** Parcours à plat de tous les nœuds (groupes + sections résiduelles). */
export function flattenNodes(nodes: GroupedNavNode[]): GroupedNavNode[] {
  return nodes.flatMap((node) => [node, ...flattenNodes(node.children)]);
}

/** Nombre d'entités d'un groupe, sous-groupes compris. */
export function countNodes(node: GroupedNavNode): number {
  return node.items.length + node.children.reduce((sum, child) => sum + countNodes(child), 0);
}

/** Retrouve le chemin de groupes menant à une route (pour le fil d'Ariane). */
export function pathToHref(
  nodes: GroupedNavNode[],
  pathname: string,
  trail: GroupedNavNode[] = [],
): GroupedNavNode[] | null {
  for (const node of nodes) {
    const next = [...trail, node];
    if (
      node.items.some(
        (item) => pathname === item.href || pathname.startsWith(`${item.href}/`),
      )
    ) {
      return next;
    }
    const deeper = pathToHref(node.children, pathname, next);
    if (deeper) return deeper;
  }
  return null;
}