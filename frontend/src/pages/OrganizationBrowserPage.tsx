import { useMemo, useState, useCallback } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import toast from 'react-hot-toast';
import {
  Network, ChevronRight, ChevronDown, Church, Building2, Map as MapIcon,
  MapPin, Users, UsersRound, Home, Landmark, Search as SearchIcon,
  Loader2, GripVertical, Eye,
} from 'lucide-react';
import api from '@/lib/api';
import { useI18n } from '@/i18n';
import { useTenant } from '@/contexts/TenantContext';

/**
 * G5.2 — Navigateur d'organisation (Church OS niveau 1).
 * Arbre hiérarchique réel (G2.1) via /api/v1/org — glisser-déposer pour
 * re-rattacher un nœud (POST /org/nodes/{id}/move), autorisé uniquement
 * si l'utilisateur possède la permission ORG_NODE_MOVE (résolue serveur).
 * Aucune donnée mockée : tout provient du backend, scopé par tenant.
 */

interface OrgNode {
  id: string;
  tenantId: string;
  parentId?: string | null;
  type: 'ROOT_CHURCH' | 'CAMPUS' | 'SUB_CHURCH' | 'ASSEMBLY' | 'REGION' | 'DISTRICT' | 'DEPARTMENT' | 'GROUP';
  name: string;
  code: string;
  status: 'ACTIVE' | 'INACTIVE' | 'ARCHIVED';
  path: string;
  level: number;
  description?: string | null;
  icon?: string | null;
  color?: string | null;
  sortOrder?: number | null;
  responsibleId?: string | null;
  createdAt: string;
}

interface OrgTreeView {
  root: OrgNode | null;
  allNodes: OrgNode[];
  childrenByParent: Record<string, OrgNode[]>;
}

interface NodeDetails {
  node: OrgNode;
  children: OrgNode[];
  descendantCount: number;
  responsible: { id: string; fullName: string; email: string } | null;
}

const TYPE_ICONS: Record<OrgNode['type'], typeof Church> = {
  ROOT_CHURCH: Church,
  CAMPUS: Landmark,
  SUB_CHURCH: Building2,
  ASSEMBLY: Users,
  REGION: MapIcon,
  DISTRICT: MapPin,
  DEPARTMENT: Home,
  GROUP: UsersRound,
};

const TYPE_ORDER: OrgNode['type'][] = [
  'ROOT_CHURCH', 'CAMPUS', 'SUB_CHURCH', 'ASSEMBLY', 'REGION', 'DISTRICT', 'DEPARTMENT', 'GROUP',
];

function sortByOrder(nodes: OrgNode[]): OrgNode[] {
  return [...nodes].sort(
    (a, b) => (a.sortOrder ?? 0) - (b.sortOrder ?? 0) || a.name.localeCompare(b.name)
  );
}

export default function OrganizationBrowserPage() {
  const { t } = useI18n();
  const { hasPermission } = useTenant();
  const queryClient = useQueryClient();

  const [expanded, setExpanded] = useState<Set<string> | null>(null);
  const [selectedId, setSelectedId] = useState<string | null>(null);
  const [filter, setFilter] = useState('');
  const [dragId, setDragId] = useState<string | null>(null);
  const [overId, setOverId] = useState<string | null>(null);

  // Édition réservée aux porteurs de la permission serveur ORG_NODE_MOVE
  const canMove = hasPermission('ORG_NODE_MOVE');
  const canCreateRoot = hasPermission('CHURCH_CREATE');

  const treeQuery = useQuery<OrgTreeView>({
    queryKey: ['org', 'tree'],
    queryFn: async () => {
      const res = await api.get('/org/tree');
      return res.data;
    },
  });

  const statsQuery = useQuery<Record<string, number>>({
    queryKey: ['org', 'stats'],
    queryFn: async () => {
      const res = await api.get('/org/stats');
      return res.data;
    },
  });

  const detailsQuery = useQuery<NodeDetails>({
    queryKey: ['org', 'node', selectedId],
    queryFn: async () => (await api.get(`/org/nodes/${selectedId}`)).data,
    enabled: !!selectedId,
  });

  const moveMutation = useMutation({
    mutationFn: async ({ nodeId, parentId }: { nodeId: string; parentId: string }) => {
      const res = await api.post(`/org/nodes/${nodeId}/move`, { parentId });
      return res.data as OrgNode;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['org'] });
      toast.success(t('organization.moveOk'));
    },
    onError: () => {
      toast.error(t('organization.moveErr'));
    },
    onSettled: () => {
      setDragId(null);
      setOverId(null);
    },
  });

  const createRootMutation = useMutation({
    mutationFn: async (payload: { name: string; code?: string }) => {
      const res = await api.post('/org/root-church', payload);
      return res.data as OrgNode;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['org'] });
      toast.success(t('organization.createOk'));
    },
    onError: () => {
      toast.error(t('organization.createErr'));
    },
  });

  const tree = treeQuery.data;
  const childrenByParent = useMemo(() => tree?.childrenByParent ?? {}, [tree]);

  // Ensembles de descendants pré-calculés pour la garde anti-cycle du drag & drop
  // (le backend revalide de toute façon — jamais de confiance aveugle au client).
  const descendantsOf = useCallback(
    (nodeId: string): Set<string> => {
      const acc = new Set<string>();
      const stack = [nodeId];
      while (stack.length) {
        const cur = stack.pop() as string;
        for (const child of childrenByParent[cur] ?? []) {
          if (!acc.has(child.id)) {
            acc.add(child.id);
            stack.push(child.id);
          }
        }
      }
      return acc;
    },
    [childrenByParent]
  );

  const isFiltering = filter.trim().length >= 2;

  // Nœuds correspondant au filtre + leurs ancêtres (pour garder l'arbre navigable)
  const visibleIds = useMemo(() => {
    if (!isFiltering || !tree) return null;
    const q = filter.trim().toLowerCase();
    const matchIds = new Set<string>();
    const byId = new Map(tree.allNodes.map((n) => [n.id, n]));
    for (const node of tree.allNodes) {
      if (node.name.toLowerCase().includes(q) || node.code.toLowerCase().includes(q)) {
        matchIds.add(node.id);
        // remonter les ancêtres
        let cur: OrgNode | undefined = node;
        while (cur?.parentId) {
          matchIds.add(cur.parentId);
          cur = byId.get(cur.parentId);
        }
      }
    }
    return matchIds;
  }, [filter, isFiltering, tree]);

  const toggleExpanded = (id: string) => {
    setExpanded((prev) => {
      const base = prev ?? new Set(tree?.allNodes.map((n) => n.id) ?? []);
      const next = new Set(base);
      if (next.has(id)) next.delete(id);
      else next.add(id);
      return next;
    });
  };

  const handleDrop = (targetId: string) => {
    if (!dragId || dragId === targetId) return;
    const moved = tree?.allNodes.find((n) => n.id === dragId);
    if (!moved) return;
    // Anti-cycle côté client (rejeté aussi côté serveur) :
    // interdit de déposer un nœud sur lui-même ou l'un de ses descendants.
    if (descendantsOf(dragId).has(targetId)) {
      toast.error(t('organization.cycleErr'));
      setDragId(null);
      setOverId(null);
      return;
    }
    moveMutation.mutate({ nodeId: dragId, parentId: targetId });
  };

  const renderNode = (node: OrgNode, depth: number) => {
    if (visibleIds && !visibleIds.has(node.id)) return null;
    const children = sortByOrder(childrenByParent[node.id] ?? []);
    const visibleChildren = children.filter((c) => !visibleIds || visibleIds.has(c.id));
    const isOpen = visibleIds ? true : (expanded ?? new Set(tree?.allNodes.map((n) => n.id) ?? [])).has(node.id);
    const Icon = TYPE_ICONS[node.type] ?? Building2;
    const hasChildren = children.length > 0;
    const isSelected = selectedId === node.id;
    const dropForbidden = !!dragId && (dragId === node.id || descendantsOf(dragId).has(node.id));

    return (
      <div key={node.id} role="treeitem" aria-expanded={hasChildren ? isOpen : undefined} aria-selected={isSelected}>
        <div
          draggable={canMove}
          onDragStart={(e) => {
            if (!canMove) return;
            e.dataTransfer.setData('text/plain', node.id);
            e.dataTransfer.effectAllowed = 'move';
            setDragId(node.id);
          }}
          onDragEnd={() => { setDragId(null); setOverId(null); }}
          onDragOver={(e) => {
            if (!canMove || !dragId || dropForbidden) return;
            e.preventDefault();
            e.dataTransfer.dropEffect = 'move';
            setOverId(node.id);
          }}
          onDragLeave={() => setOverId((cur) => (cur === node.id ? null : cur))}
          onDrop={(e) => {
            if (!canMove || dropForbidden) return;
            e.preventDefault();
            handleDrop(node.id);
          }}
          onClick={() => setSelectedId(node.id)}
          className={`group flex items-center gap-2 rounded-xl px-2 py-1.5 cursor-pointer transition-all duration-150 ${
            isSelected
              ? 'bg-primary-500/10 ring-1 ring-primary-500/30'
              : 'hover:bg-gray-100 dark:hover:bg-gray-800/50'
          } ${overId === node.id && !dropForbidden ? 'ring-2 ring-primary-500 bg-primary-500/5' : ''} ${
            dragId === node.id ? 'opacity-40' : ''
          }`}
          style={{ paddingLeft: `${8 + depth * 20}px` }}
          title={node.description ?? node.name}
        >
          {hasChildren ? (
            <button
              type="button"
              aria-label={isOpen ? 'Collapse' : 'Expand'}
              onClick={(e) => { e.stopPropagation(); toggleExpanded(node.id); }}
              className="p-0.5 rounded text-gray-400 hover:text-gray-700 dark:hover:text-gray-200"
            >
              {isOpen ? <ChevronDown className="w-4 h-4" /> : <ChevronRight className="w-4 h-4" />}
            </button>
          ) : (
            <span className="w-5" />
          )}

          {canMove && (
            <GripVertical className="w-3.5 h-3.5 text-gray-300 dark:text-gray-600 opacity-0 group-hover:opacity-100 shrink-0 cursor-grab" />
          )}

          <span
            className="p-1.5 rounded-lg shrink-0"
            style={{ backgroundColor: `${node.color ?? '#6366f1'}1a`, color: node.color ?? '#6366f1' }}
          >
            <Icon className="w-3.5 h-3.5" />
          </span>

          <span className="text-sm font-medium text-gray-800 dark:text-gray-100 truncate">
            {node.name}
          </span>
          <span className="text-[10px] uppercase tracking-wide text-gray-400 shrink-0">
            {t(`organization.type.${node.type}`)}
          </span>
          {node.status !== 'ACTIVE' && (
            <span className="badge text-[10px] bg-gray-200 dark:bg-gray-700 text-gray-500 shrink-0">
              {node.status}
            </span>
          )}
        </div>

        {isOpen && visibleChildren.length > 0 && (
          <div role="group">{visibleChildren.map((c) => renderNode(c, depth + 1))}</div>
        )}
      </div>
    );
  };

  return (
    <div className="page-container">
      {/* Header */}
      <div className="page-header">
        <div className="animate-fade-in">
          <div className="flex items-center gap-2 mb-1">
            <Network className="w-5 h-5 text-primary-500" />
            <span className="text-sm font-medium text-primary-600 dark:text-primary-400 uppercase tracking-wider">
              Church OS
            </span>
          </div>
          <h1 className="page-title">
            {t('organization.title')}{' '}
            <span className="text-gradient font-display">Discipolat</span>
          </h1>
          <p className="page-subtitle">{t('organization.subtitle')}</p>
        </div>
        <span
          className={`inline-flex items-center gap-1.5 badge text-xs shrink-0 ${
            canMove
              ? 'bg-green-100 dark:bg-green-900/30 text-green-700 dark:text-green-400'
              : 'bg-gray-100 dark:bg-gray-800 text-gray-500 dark:text-gray-400'
          }`}
        >
          {canMove ? <GripVertical className="w-3.5 h-3.5" /> : <Eye className="w-3.5 h-3.5" />}
          {canMove ? t('organization.dragHint') : t('organization.readOnly')}
        </span>
      </div>

      {/* Stats par type (API réelle /org/stats) */}
      {statsQuery.data && Object.values(statsQuery.data).some((v) => v > 0) && (
        <div className="flex flex-wrap gap-2 mb-6 animate-slide-up">
          {TYPE_ORDER.filter((tp) => (statsQuery.data?.[tp] ?? 0) > 0).map((tp) => {
            const Icon = TYPE_ICONS[tp];
            return (
              <span
                key={tp}
                className="inline-flex items-center gap-1.5 glass-card px-3 py-1.5 text-xs font-medium text-gray-600 dark:text-gray-300"
              >
                <Icon className="w-3.5 h-3.5 text-primary-500" />
                {t(`organization.type.${tp}`)}
                <span className="stat-value !text-sm">{statsQuery.data?.[tp] ?? 0}</span>
              </span>
            );
          })}
        </div>
      )}

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Arbre */}
        <div className="lg:col-span-2 glass-card p-4 animate-slide-up">
          <div className="relative mb-4">
            <SearchIcon className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400" />
            <input
              type="search"
              value={filter}
              onChange={(e) => setFilter(e.target.value)}
              placeholder={t('organization.search')}
              aria-label={t('organization.search')}
              className="input pl-9 text-sm"
            />
          </div>

          {treeQuery.isLoading && (
            <div className="flex items-center justify-center py-16 text-gray-400">
              <Loader2 className="w-6 h-6 animate-spin" />
            </div>
          )}

          {treeQuery.isError && (
            <div className="text-center py-16">
              <p className="text-sm text-red-500">{t('organization.loadErr')}</p>
              <button
                type="button"
                onClick={() => treeQuery.refetch()}
                className="btn btn-secondary btn-sm mt-3"
              >
                {t('organization.retry')}
              </button>
            </div>
          )}

          {!treeQuery.isLoading && tree && !tree.root && (
            <RootChurchForm
              canCreate={canCreateRoot}
              creating={createRootMutation.isPending}
              onCreate={(payload) => createRootMutation.mutate(payload)}
            />
          )}

          {tree?.root && (
            <div role="tree" aria-label={t('organization.title')} className="space-y-0.5 max-h-[60vh] overflow-y-auto pr-1">
              {renderNode(tree.root, 0)}
            </div>
          )}
        </div>

        {/* Panneau détails (API réelle /org/nodes/{id}) */}
        <div className="glass-card p-5 animate-slide-up" style={{ animationDelay: '100ms' }}>
          <h3 className="text-sm font-semibold text-gray-900 dark:text-gray-100 mb-4">
            {t('organization.details')}
          </h3>
          {!selectedId && (
            <p className="text-sm text-gray-400 dark:text-gray-500">{t('organization.noSelection')}</p>
          )}
          {selectedId && detailsQuery.isLoading && (
            <div className="space-y-3">
              <div className="skeleton h-4 w-32 rounded" />
              <div className="skeleton h-4 w-48 rounded" />
              <div className="skeleton h-4 w-24 rounded" />
            </div>
          )}
          {selectedId && detailsQuery.isError && (
            <p className="text-sm text-red-500">{t('organization.loadErr')}</p>
          )}
          {detailsQuery.data && (() => {
            const d = detailsQuery.data;
            const Icon = TYPE_ICONS[d.node.type] ?? Building2;
            return (
              <div className="space-y-4 text-sm">
                <div className="flex items-center gap-3">
                  <span
                    className="p-2.5 rounded-xl shrink-0"
                    style={{ backgroundColor: `${d.node.color ?? '#6366f1'}1a`, color: d.node.color ?? '#6366f1' }}
                  >
                    <Icon className="w-5 h-5" />
                  </span>
                  <div className="min-w-0">
                    <p className="font-semibold text-gray-900 dark:text-gray-100 truncate">{d.node.name}</p>
                    <p className="text-xs text-gray-400">{t(`organization.type.${d.node.type}`)} · {d.node.code}</p>
                  </div>
                </div>

                {d.node.description && (
                  <p className="text-gray-500 dark:text-gray-400">{d.node.description}</p>
                )}

                <div>
                  <p className="text-xs uppercase tracking-wide text-gray-400 mb-1">
                    {t('organization.responsible')}
                  </p>
                  <p className="text-gray-800 dark:text-gray-200">
                    {d.responsible ? `${d.responsible.fullName}` : t('organization.none')}
                  </p>
                  {d.responsible?.email && (
                    <p className="text-xs text-gray-400">{d.responsible.email}</p>
                  )}
                </div>

                <div className="grid grid-cols-2 gap-3">
                  <div className="rounded-xl bg-gray-50 dark:bg-gray-800/50 p-3">
                    <p className="text-[10px] uppercase tracking-wide text-gray-400">
                      {t('organization.descendants')}
                    </p>
                    <p className="stat-value !text-xl">{d.descendantCount}</p>
                  </div>
                  <div className="rounded-xl bg-gray-50 dark:bg-gray-800/50 p-3">
                    <p className="text-[10px] uppercase tracking-wide text-gray-400">
                      {t('organization.children')}
                    </p>
                    <p className="stat-value !text-xl">{d.children.length}</p>
                  </div>
                </div>

                {d.children.length > 0 && (
                  <div>
                    <p className="text-xs uppercase tracking-wide text-gray-400 mb-2">
                      {t('organization.children')}
                    </p>
                    <ul className="space-y-1">
                      {sortByOrder(d.children).map((c) => (
                        <li key={c.id}>
                          <button
                            type="button"
                            onClick={() => setSelectedId(c.id)}
                            className="w-full text-left flex items-center gap-2 rounded-lg px-2 py-1.5 hover:bg-gray-100 dark:hover:bg-gray-800/50 text-gray-700 dark:text-gray-300"
                          >
                            <span
                              className="w-2 h-2 rounded-full shrink-0"
                              style={{ backgroundColor: c.color ?? '#6366f1' }}
                            />
                            <span className="truncate">{c.name}</span>
                          </button>
                        </li>
                      ))}
                    </ul>
                  </div>
                )}
              </div>
            );
          })()}
        </div>
      </div>
    </div>
  );
}

/** Formulaire de création de l'église racine — visible seulement si le tenant
 *  n'a aucun nœud et que l'utilisateur porte la permission CHURCH_CREATE. */
function RootChurchForm({
  canCreate,
  creating,
  onCreate,
}: {
  canCreate: boolean;
  creating: boolean;
  onCreate: (payload: { name: string; code?: string }) => void;
}) {
  const { t } = useI18n();
  const [name, setName] = useState('');
  const [code, setCode] = useState('');

  if (!canCreate) {
    return (
      <div className="text-center py-16">
        <Network className="w-10 h-10 text-gray-300 dark:text-gray-600 mx-auto mb-3" />
        <p className="text-sm text-gray-500">{t('organization.empty')}</p>
        <p className="text-xs text-gray-400 mt-1">{t('organization.emptyHint')}</p>
      </div>
    );
  }

  return (
    <form
      onSubmit={(e) => {
        e.preventDefault();
        if (name.trim().length < 2) return;
        onCreate({ name: name.trim(), code: code.trim() || undefined });
      }}
      className="max-w-md mx-auto py-10 space-y-4"
    >
      <div className="text-center">
        <Network className="w-10 h-10 text-gray-300 dark:text-gray-600 mx-auto mb-3" />
        <p className="text-sm text-gray-500">{t('organization.empty')}</p>
        <p className="text-xs text-gray-400 mt-1">{t('organization.emptyHint')}</p>
      </div>
      <input
        value={name}
        onChange={(e) => setName(e.target.value)}
        placeholder={t('organization.rootName')}
        aria-label={t('organization.rootName')}
        required
        minLength={2}
        className="input"
      />
      <input
        value={code}
        onChange={(e) => setCode(e.target.value)}
        placeholder={t('organization.rootCode')}
        aria-label={t('organization.rootCode')}
        className="input"
      />
      <button
        type="submit"
        disabled={creating || name.trim().length < 2}
        className="btn-glow btn-sm w-full inline-flex items-center justify-center gap-2"
      >
        {creating && <Loader2 className="w-4 h-4 animate-spin" />}
        {t('organization.createRoot')}
      </button>
    </form>
  );
}
