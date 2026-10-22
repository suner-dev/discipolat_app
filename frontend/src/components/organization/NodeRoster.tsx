import { useState } from 'react';
import { Loader2, Search, Users } from 'lucide-react';
import { useI18n } from '@/i18n';
import { useNodeMembers, type NodeMemberRow } from '@/hooks/useOrganizationV3';
import { UserDetailModal } from '@/components/users/UserDetailModal';

/**
 * Roster nominatif des membres d'une entité ecclésiastique (mission
 * « consulter les membres à chaque échelle »).
 *
 * <p>Vue en LECTURE SEULE, consommée à deux endroits — l'onglet « Membres » de
 * la fiche nœud (`/organization/:id`) et l'annuaire scopé
 * (`/users?nodeId=&scope=`) — donc écrite une seule fois. Le serveur scope
 * (tenant du contexte, 404 cross-tenant), déduplique (un membre = une ligne, au
 * point d'attachement le plus proche) et borne la pagination ; la composante
 * n'invente jamais un rattachement : sans données, repli explicite.
 *
 * <p>Visibilité alignée sur l'annuaire existant `/users` (tout membre
 * authentifié du tenant) : aucune donnée supplémentaire n'est ouverte, elle est
 * scopée sur une entité.
 */
export interface NodeRosterProps {
  nodeId: string | null;
  /** Scope initial (défaut SUBTREE = avec les sous-structures). */
  initialScope?: 'DIRECT' | 'SUBTREE';
  /** Taille de page (défaut 20, bornée serveur). */
  size?: number;
}

const SCOPES = ['DIRECT', 'SUBTREE'] as const;

export default function NodeRoster({ nodeId, initialScope = 'SUBTREE', size = 20 }: NodeRosterProps) {
  const { t } = useI18n();
  const [scope, setScope] = useState<'DIRECT' | 'SUBTREE'>(initialScope);
  const [q, setQ] = useState('');
  const [page, setPage] = useState(0);
  const [openUserId, setOpenUserId] = useState<string | null>(null);
  const roster = useNodeMembers(nodeId, { scope, q, page, size });

  // Changer de filtre ramène à la première page, sinon la requête porte sur une
  // page qui n'existe plus dans le nouvel ensemble.
  const resetPage = (fn: () => void) => {
    fn();
    setPage(0);
  };

  if (!nodeId) return null;
  const data = roster.data;
  // Spinner réservé à la PREMIÈRE lecture : changer de page ou taper dans le
  // filtre conserve la lecture précédente (`keepPreviousData` dans le hook), donc
  // le champ de recherche et les compteurs restent sous la main.
  if (!data && roster.isPending) {
    return (
      <div className="flex items-center justify-center py-8 text-gray-400">
        <Loader2 className="w-5 h-5 animate-spin" />
      </div>
    );
  }

  const rows: NodeMemberRow[] = data?.content ?? [];
  const totalPages = data?.totalPages ?? 0;

  return (
    <div className="glass-card p-4 space-y-3">
      <div className="flex items-center gap-2 text-primary-500">
        <Users className="w-5 h-5" />
        <span className="text-sm font-medium">
          {data?.nodeName ? t('orgV3.members.titleOf', { node: data.nodeName }) : t('orgV3.members.title')}
        </span>
        <span className="ml-auto text-xs text-gray-400">
          {t('orgV3.members.countDirect')}: {data?.memberCountDirect ?? 0} ·{' '}
          {t('orgV3.members.countSousArbre')}: {data?.memberCountSousArbre ?? 0}
        </span>
      </div>

      <div className="flex flex-wrap items-center gap-2">
        <div className="relative flex-1 min-w-[180px]">
          <Search className="w-4 h-4 absolute left-2 top-1/2 -translate-y-1/2 text-gray-400" />
          <input
            type="search"
            value={q}
            onChange={(e) => resetPage(() => setQ(e.target.value))}
            placeholder={t('orgV3.members.search')}
            aria-label={t('orgV3.members.search')}
            className="input-glass w-full pl-8 text-sm"
          />
        </div>
        <div className="flex rounded-lg overflow-hidden border border-gray-200 dark:border-gray-700">
          {SCOPES.map((sc) => (
            <button
              key={sc}
              type="button"
              onClick={() => resetPage(() => setScope(sc))}
              className={`px-3 py-1.5 text-xs ${
                scope === sc ? 'bg-primary-500 text-white' : 'bg-gray-100 dark:bg-gray-800 text-gray-600'
              }`}
            >
              {t(`orgV3.members.scope.${sc}`)}
            </button>
          ))}
        </div>
      </div>

      {/* Dégradation explicite : une erreur de lecture n'est pas un « vide » —
          et elle n'est pas non plus masquée par la page précédente conservée. */}
      {roster.isError ? (
        <p className="text-sm text-red-500">{t('orgV3.members.error')}</p>
      ) : rows.length === 0 ? (
        <p className="text-sm text-gray-400">{t('orgV3.members.empty')}</p>
      ) : (
        <ul className="divide-y divide-gray-100 dark:divide-gray-800">
          {rows.map((m) => (
            <li key={m.userId} className="py-2 flex items-center gap-3 text-sm">
              <div className="flex-1 min-w-0">
                <button
                  type="button"
                  onClick={() => setOpenUserId(m.userId)}
                  className="truncate hover:text-primary-600 text-gray-800 dark:text-gray-100 text-left"
                >
                  {[m.firstName, m.lastName].filter(Boolean).join(' ') || m.userId}
                </button>
                <div className="text-[11px] text-gray-400 truncate">{m.email ?? '—'}</div>
              </div>
              <div className="text-right shrink-0">
                {m.roles && m.roles.length > 0 && (
                  <span className="text-[11px] text-primary-600">{m.roles.join(', ')}</span>
                )}
                {m.direct ? (
                  <span className="block text-[11px] text-green-600">
                    {t('orgV3.members.scope.DIRECT')}
                  </span>
                ) : (
                  m.attachedNodeName && (
                    <span className="text-[11px] text-gray-400">
                      {t('orgV3.members.via')}: {m.attachedNodeName}
                    </span>
                  )
                )}
              </div>
            </li>
          ))}
        </ul>
      )}

      {totalPages > 1 && (
        <div className="flex items-center justify-center gap-3 pt-2">
          <button
            type="button"
            className="btn btn-secondary btn-sm disabled:opacity-40"
            disabled={page === 0}
            onClick={() => setPage((p) => Math.max(0, p - 1))}
          >
            {t('orgV3.members.prev')}
          </button>
          <span className="text-xs text-gray-400">
            {page + 1} / {totalPages}
          </span>
          <button
            type="button"
            className="btn btn-secondary btn-sm disabled:opacity-40"
            disabled={page + 1 >= totalPages}
            onClick={() => setPage((p) => p + 1)}
          >
            {t('orgV3.members.next')}
          </button>
        </div>
      )}

      {/* Fiche membre complète réutilisée depuis l'annuaire (pas re-créée) —
          le roster mène à la fiche, la fiche mène au fil d'Ariane. */}
      {openUserId && <UserDetailModal userId={openUserId} onClose={() => setOpenUserId(null)} />}
    </div>
  );
}
