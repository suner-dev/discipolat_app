import { useMemo, useState, type ReactNode } from 'react';
import { ChevronDown, ChevronRight, CornerDownRight, Loader2 } from 'lucide-react';
import { useI18n } from '@/i18n';

/**
 * V231 — composant d'arbre générique et réutilisable.
 *
 * <p>Pourquoi ne pas réutiliser `OrgTreeNav` : celui-ci est typé sur
 * l'interface `TreeNode` de l'org V3 et affiche en dur `memberCount`,
 * `churchCount`, `leaderCount`, `levelName` et `responsibleName`. Il ne sait
 * pas rendre un encadrement personnel, ni une chaîne d'ascendance, ni des
 * people (il n'a pas d'identifiant de personne cliquable). Le factoriser
 * ici — un seul mécanisme de pliage/expansion, réutilisable plus tard par
 * l'org V3 — évite d'avoir deux arbres divergents dans l'application.
 *
 * <p>Accessibilité : chaque nœud est un vrai `<button>` avec
 * `aria-expanded`, et la liste imbriquée porte `role="group"`.
 */

export interface TreeNodeModel {
  /** Identifiant stable (utilisé comme clé React et pour l'état de pliage). */
  id: string;
  /** Libellé principal. */
  label: string;
  /** Contenu secondaire (type, niveau, origine…). */
  meta?: string;
  /** Élément affiché à droite du libellé (badge, compteur). */
  badge?: ReactNode;
  /** Le nœud porte le focus d'un membre ; sert à le mettre en évidence. */
  highlighted?: boolean;
  /** Le nœud est une personne : rend le label cliquable. */
  personId?: string;
  onSelectPerson?: (personId: string) => void;
  /** Enfants. */
  children?: TreeNodeModel[];
}

export function HierarchyTree({
  nodes,
  defaultExpanded = true,
  emptyLabel,
  emptyIcon,
}: {
  nodes: TreeNodeModel[];
  defaultExpanded?: boolean;
  emptyLabel?: string;
  emptyIcon?: ReactNode;
}) {
  if (nodes.length === 0) {
    return emptyLabel ? (
      <p className="text-xs text-gray-400 flex items-center gap-1.5">
        {emptyIcon}
        {emptyLabel}
      </p>
    ) : null;
  }
  return (
    <ul className="space-y-0.5" role="tree">
      {nodes.map((node) => (
        <TreeBranch
          key={node.id}
          node={node}
          level={0}
          defaultExpanded={defaultExpanded}
        />
      ))}
    </ul>
  );
}

function TreeBranch({
  node,
  level,
  defaultExpanded,
}: {
  node: TreeNodeModel;
  level: number;
  defaultExpanded: boolean;
}) {
  const children = node.children ?? [];
  const collapsible = children.length > 0;
  const [expanded, setExpanded] = useState(defaultExpanded);
  const { t } = useI18n();

  // L'état de pliage est local au nœud : pas de registre global, donc pas
  // de risque d'invalidation croisée entre branches.
  const isOpen = collapsible && expanded;

  const labelNode = (
    <span
      className={[
        'flex items-center gap-1.5 rounded-lg px-2 py-1 text-[11px]',
        node.highlighted
          ? 'bg-violet-100 dark:bg-violet-900/40 ring-1 ring-violet-300 dark:ring-violet-700'
          : '',
      ].join(' ')}
    >
      {level > 0 && <CornerDownRight className="w-3 h-3 shrink-0 text-gray-300" aria-hidden />}
      {collapsible ? (
        <button
          type="button"
          onClick={() => setExpanded((v) => !v)}
          aria-expanded={isOpen}
          aria-label={`${isOpen ? t('hierarchy.collapseNode') : t('hierarchy.expandNode')} ${node.label}`}
          className="shrink-0 text-gray-400 hover:text-gray-600 dark:hover:text-gray-200 cursor-pointer"
        >
          {isOpen ? <ChevronDown className="w-3 h-3" /> : <ChevronRight className="w-3 h-3" />}
        </button>
      ) : (
        <span className="w-3 shrink-0" aria-hidden />
      )}

      {node.personId && node.onSelectPerson ? (
        <button
          type="button"
          onClick={() => node.onSelectPerson?.(node.personId as string)}
          className="font-medium text-gray-700 dark:text-gray-300 hover:text-violet-600 dark:hover:text-violet-400 hover:underline cursor-pointer truncate"
        >
          {node.label}
        </button>
      ) : (
        <span className="font-medium text-gray-700 dark:text-gray-300 truncate">{node.label}</span>
      )}

      {node.meta && <span className="text-[10px] text-gray-400 shrink-0">{node.meta}</span>}
      {node.badge}
    </span>
  );

  return (
    <li role="treeitem" aria-expanded={collapsible ? isOpen : undefined}>
      {labelNode}
      {isOpen && (
        <ul
          role="group"
          className="ml-3 pl-2 border-l border-gray-200 dark:border-gray-700/60 space-y-0.5"
        >
          {children.map((child) => (
            <TreeBranch key={child.id} node={child} level={level + 1} defaultExpanded={defaultExpanded} />
          ))}
        </ul>
      )}
    </li>
  );
}

export function LoadingTree({ label }: { label: string }) {
  return (
    <p className="text-xs text-gray-400 flex items-center gap-1.5">
      <Loader2 className="w-3 h-3 animate-spin" /> {label}
    </p>
  );
}

/**
 * Transforme l'agrégat `hierarchie.branches` du backend en nœuds
 * d'arbre : une racine « Branche n » par branche, et sous elle la chaîne
 * d'ascendance (du nœud le plus proche à la racine), chaque niveau portant
 * son responsable comme personne cliquable.
 *
 * <p>La première personne de la chaîne se place au niveau parent : dans
 * l'interface, le membre <em>est</em> le premier maillon, c'est son chef
 * direct qui est le suivant.
 */
export function useBranchTree(
  branches: HierarchyBranch[] | undefined,
  onSelectPerson: (personId: string) => void,
  t?: (key: string) => string,
): TreeNodeModel[] {
  return useMemo(() => {
    if (!branches?.length) return [];
    const localizeOrigin = (o: string) => {
      switch (o) {
        case 'ASSIGNATION_V3': return t?.('hierarchy.originAssignation') ?? o;
        case 'ADHESION_NOEUD': return t?.('hierarchy.originAdhesion') ?? o;
        case 'RESPONSABLE_NOEUD': return t?.('hierarchy.originResponsable') ?? o;
        default: return o;
      }
    };
    const localizeNodeType = (n: string) => {
      const map: Record<string, string> = {
        ROOT_CHURCH: 'hierarchy.nodeType.rootChurch',
        CAMPUS: 'hierarchy.nodeType.campus',
        SUB_CHURCH: 'hierarchy.nodeType.subChurch',
        ASSEMBLY: 'hierarchy.nodeType.assembly',
        REGION: 'hierarchy.nodeType.region',
        DISTRICT: 'hierarchy.nodeType.district',
        DEPARTMENT: 'hierarchy.nodeType.department',
        GROUP: 'hierarchy.nodeType.group',
      };
      const key = map[n];
      return key ? t?.(key) ?? n : n;
    };
    return branches.map((branch, index) => {
      const noeud: HierarchyNode = branch.noeud ?? { id: branchKey(branch, index) };
      const chaine: HierarchyStep[] = branch.chaine ?? [];
      const childNodes: TreeNodeModel[] = chaine
        .filter((step) => step.responsable?.id)
        .map((step) => ({
          id: `${branchKey(branch, index)}-${step.id}`,
          label: step.responsable!.nom ?? step.nom ?? '—',
          meta: step.nom ?? undefined,
          personId: String(step.responsable!.id),
          onSelectPerson,
        }));

      return {
        id: branchKey(branch, index),
        label: noeud.nom ?? '—',
        meta: [noeud.type ? localizeNodeType(noeud.type) : undefined, branch.origine ? localizeOrigin(branch.origine) : undefined]
          .filter(Boolean)
          .join(' · ') || undefined,
        children: childNodes,
      };
    });
  }, [branches, onSelectPerson, t]);
}

function branchKey(branch: HierarchyBranch, index: number): string {
  return `${branch.noeud?.id ?? 'branche'}-${index}`;
}

/** Formes minimales du JSON `hierarchie` réellement consommé. */
export interface HierarchyNode {
  id: string;
  nom?: string;
  type?: string;
  level?: number;
}

export interface HierarchyResponsible {
  id: string;
  nom?: string;
}

export interface HierarchyStep extends HierarchyNode {
  responsable?: HierarchyResponsible | null;
}

export interface HierarchyBranch {
  noeud?: HierarchyNode;
  origine?: string;
  niveaux?: number;
  chaine?: HierarchyStep[];
}
