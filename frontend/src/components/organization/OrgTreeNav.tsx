import { useMemo } from 'react';
import { Loader2, ChevronRight, Users, Church, UserCog } from 'lucide-react';
import { useI18n } from '@/i18n';
import type { TreeNode } from '@/hooks/useOrganizationV3';

/**
 * SPEC_ORGANISATION_MODULABLE_V3 §6.3 / T-W11 — `<OrgTreeNav>`.
 * Arbre de drill-down (E) : remplace l'affichage brut `type` par le **nom du
 * niveau** (`levelName`), affiche le **responsable** et les **compteurs inline**
 * (§5.4 : fidèles, églises, leaders). Aucun PII nominatif de membre — seuls des
 * nombres et le nom du responsable déclaré (§3.2.5 / D7).
 */
export default function OrgTreeNav({
  nodes,
  loading,
  onOpen,
  rootId = null,
  depth = 0,
}: {
  nodes: TreeNode[];
  loading?: boolean;
  onOpen?: (node: TreeNode) => void;
  rootId?: string | null;
  depth?: number;
}) {
  const { t } = useI18n();

  // Index enfants par parent : un seul passage, la récursion dessine l'arbre.
  const childrenByParent = useMemo(() => {
    const map = new Map<string | null, TreeNode[]>();
    for (const n of nodes) {
      const key = n.parentId ?? null;
      const arr = map.get(key) ?? [];
      arr.push(n);
      map.set(key, arr);
    }
    for (const arr of map.values()) {
      arr.sort((a, b) => (a.levelName ?? a.name).localeCompare(b.levelName ?? b.name));
    }
    return map;
  }, [nodes]);

  if (loading) {
    return <div className="flex justify-center py-8 text-gray-400"><Loader2 className="w-5 h-5 animate-spin" /></div>;
  }

  const children = childrenByParent.get(rootId) ?? [];

  if (children.length === 0 && depth === 0) {
    return <p className="text-sm text-gray-400">{t('orgV3.tree.empty')}</p>;
  }

  return (
    <ul className={depth === 0 ? 'space-y-1' : 'space-y-1 ml-4 border-l border-gray-200 dark:border-gray-700 pl-3'}>
      {children.map((n) => (
        <li key={n.id}>
          <button
            type="button"
            onClick={() => onOpen?.(n)}
            className="w-full flex items-center gap-2 text-sm text-gray-700 dark:text-gray-200 hover:bg-gray-50 dark:hover:bg-gray-800/50 rounded px-1.5 py-1 text-left"
          >
            <ChevronRight className="w-4 h-4 text-gray-400 shrink-0" />
            <span className="flex-1 truncate" title={n.levelName ?? n.name}>{n.name}</span>
            {n.levelName && (
              <span className="text-[10px] uppercase tracking-wide text-primary-600/80 shrink-0">{n.levelName}</span>
            )}
            <span className="flex items-center gap-2 text-[11px] text-gray-400 shrink-0">
              <span className="inline-flex items-center gap-0.5"><Users className="w-3 h-3" />{n.memberCount ?? 0}</span>
              <span className="inline-flex items-center gap-0.5"><Church className="w-3 h-3" />{n.churchCount ?? 0}</span>
              <span className="inline-flex items-center gap-0.5"><UserCog className="w-3 h-3" />{n.leaderCount ?? 0}</span>
            </span>
            {n.responsibleName && (
              <span className="hidden md:inline text-[11px] text-gray-400 truncate max-w-[10rem]">{n.responsibleName}</span>
            )}
          </button>
          <OrgTreeNav nodes={nodes} onOpen={onOpen} rootId={n.id} depth={depth + 1} />
        </li>
      ))}
    </ul>
  );
}
