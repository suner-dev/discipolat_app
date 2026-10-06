import { useCallback, useEffect, useMemo, useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import api, { getErrorMessage } from '@/lib/api';
import { useI18n } from '@/i18n';
import { ChevronLeft, ChevronRight, HeartHandshake, Loader2, Plus, Search, Users, X } from 'lucide-react';
import toast from 'react-hot-toast';

/**
 * V231 — « Mon encadrement » : le membre déclare lui-même ses autorités
 * enregistrées (pasteur, supérieur, mentor, parrain…) selon le paramétrage
 * de son église (dictionnaire MEMBER_RELATION_TYPE côté serveur). Effet
 * automatique : rattachement ACTIVE + notification chez le supérieur, qui
 * retrouve ce membre dans « ses membres ». Additif — ne remplace aucune
 * section du profil existant.
 *
 * <p>Robustesse assumée :
 * <ul>
 *   <li>la liste « mes membres » est <b>paginée côté serveur</b>
 *       ({@code /relations/me/members}) : un encadrement peut dépasser le
 *       plafond de rattachements entrants, et une liste non bornée casserait
 *       la page ;</li>
 *   <li>le bouton « retirer » n'est proposé que si le serveur a déclaré la
 *       relation {@code revocable} — sinon l'app afficherait un bouton qui
 *       échouerait en 403 ;</li>
 *   <li>une panne du serveur affiche un message, jamais un vide silencieux.</li>
 * </ul>
 */

export interface RelationRow {
  id: string;
  fromUserId?: string;
  fromNom?: string;
  toUserId?: string;
  toNom?: string;
  otherUserId: string;
  otherNom: string;
  relationType: string;
  typeLabel: string;
  statut: string;
  note?: string | null;
  createdAt?: string | null;
  revocable?: boolean;
}

interface RelationSearchUser {
  id: string;
  firstName?: string;
  lastName?: string;
  email?: string;
}

const MEMBERS_PAGE_SIZE = 25;

export function MyRelationsCard({ onOpenUser }: { onOpenUser?: (userId: string) => void }) {
  const { t } = useI18n();
  const queryClient = useQueryClient();
  const [adding, setAdding] = useState(false);
  const [type, setType] = useState('');
  const [query, setQuery] = useState('');
  const [selected, setSelected] = useState<RelationSearchUser | null>(null);
  const [note, setNote] = useState('');
  const [membersPage, setMembersPage] = useState(0);

  const { data: types, isError: typesError } = useQuery({
    queryKey: ['relations', 'types'],
    queryFn: async () => (await api.get('/relations/types')).data as { code: string; label: string }[],
    staleTime: 60_000,
    retry: 1,
  });

  const { data: mine, isLoading, isError, refetch } = useQuery({
    queryKey: ['relations', 'me'],
    queryFn: async () => {
      const res = await api.get('/relations/me');
      return res.data as { sortantes: RelationRow[]; entrantes: RelationRow[] };
    },
    retry: 1,
  });

  // « Ses membres » : source paginée et bornée côté serveur. On ne se fie
  // jamais à `mine.entrantes` pour la liste affichée (non bornée).
  const { data: membersPage_, isLoading: membersLoading } = useQuery({
    queryKey: ['relations', 'me', 'members', membersPage],
    queryFn: async () => {
      const res = await api.get('/relations/me/members', {
        params: { page: membersPage, size: MEMBERS_PAGE_SIZE },
      });
      return res.data as {
        content: RelationRow[];
        totalElements: number;
        totalPages: number;
        number: number;
      };
    },
    placeholderData: (previous) => previous,
    retry: 1,
  });

  // Recherche de membres enregistrés (même endpoint que l'annuaire) —
  // debounce léger pour ne pas mitrainer /users/search.
  const [debounced, setDebounced] = useState('');
  useEffect(() => {
    const id = window.setTimeout(() => setDebounced(query.trim()), 300);
    return () => window.clearTimeout(id);
  }, [query]);
  const { data: candidates, isFetching: searching } = useQuery({
    queryKey: ['relations', 'search', debounced],
    queryFn: async () => (await api.get('/users/search', { params: { q: debounced } })).data as RelationSearchUser[],
    enabled: debounced.length >= 2,
    staleTime: 15_000,
    retry: 1,
  });

  const defaultType = useMemo(() => types?.[0]?.code ?? 'PASTEUR', [types]);

  const invalidateAll = useCallback(() => {
    queryClient.invalidateQueries({ queryKey: ['relations'] });
    // La fiche utilisateur expose aussi ces relations : elle doit suivre.
    queryClient.invalidateQueries({ queryKey: ['users'] });
  }, [queryClient]);

  const declareMutation = useMutation({
    mutationFn: async () => {
      const res = await api.post('/relations/me', {
        toUserId: selected?.id,
        relationType: type || defaultType,
        note: note.trim() || undefined,
      });
      return res.data;
    },
    onSuccess: () => {
      toast.success(t('relations.declaredToast'));
      invalidateAll();
      setAdding(false);
      setSelected(null);
      setQuery('');
      setNote('');
    },
    onError: (err) => toast.error(getErrorMessage(err)),
  });

  const revokeMutation = useMutation({
    mutationFn: async (id: string) => (await api.delete(`/relations/me/${id}`)).data,
    onSuccess: () => {
      toast.success(t('relations.removedToast'));
      invalidateAll();
    },
    onError: (err) => toast.error(getErrorMessage(err)),
  });

  const sortantes = mine?.sortantes ?? [];
  const members = membersPage_?.content ?? [];
  const totalMembers = membersPage_?.totalElements ?? 0;
  const totalPages = membersPage_?.totalPages ?? 0;
  const openUser = onOpenUser;

  return (
    <div className="bg-white dark:bg-gray-800 rounded-xl shadow-sm border border-gray-100 dark:border-gray-700/60 p-6">
      <div className="flex items-center justify-between gap-3">
        <div className="flex items-center gap-2">
          <HeartHandshake className="w-5 h-5 text-primary-500" />
          <h3 className="text-lg font-semibold text-gray-900 dark:text-gray-100">{t('relations.sectionTitle')}</h3>
        </div>
        <button
          type="button"
          onClick={() => setAdding((v) => !v)}
          aria-expanded={adding}
          className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-sm font-medium bg-primary-50 text-primary-700 hover:bg-primary-100 dark:bg-primary-900/30 dark:text-primary-300 cursor-pointer"
        >
          {adding ? <X className="w-4 h-4" /> : <Plus className="w-4 h-4" />}
          {adding ? t('relations.cancel') : t('relations.add')}
        </button>
      </div>
      <p className="text-sm text-gray-500 dark:text-gray-400 mt-1">{t('relations.hint')}</p>

      {adding && (
        <div className="mt-4 rounded-xl border border-gray-200 dark:border-gray-700 p-4 space-y-3">
          <div>
            <label htmlFor="relation-type" className="block text-xs font-medium text-gray-500 mb-1">
              {t('relations.type')}
            </label>
            {typesError ? (
              <p className="text-xs text-amber-600 dark:text-amber-400">{t('relations.typesUnavailable')}</p>
            ) : (
              <select
                id="relation-type"
                value={type || defaultType}
                onChange={(e) => setType(e.target.value)}
                className="w-full rounded-lg border-gray-300 dark:border-gray-600 dark:bg-gray-900 text-sm"
              >
                {(types ?? [{ code: 'PASTEUR', label: t('relations.typePasteur') }]).map((tp) => (
                  <option key={tp.code} value={tp.code}>{tp.label}</option>
                ))}
              </select>
            )}
          </div>
          <div>
            <label htmlFor="relation-search" className="block text-xs font-medium text-gray-500 mb-1">
              {t('relations.searchLabel')}
            </label>
            <div className="relative">
              <Search className="w-4 h-4 absolute left-3 top-1/2 -translate-y-1/2 text-gray-400" />
              <input
                id="relation-search"
                value={query}
                onChange={(e) => { setQuery(e.target.value); setSelected(null); }}
                placeholder={t('relations.searchPlaceholder')}
                className="w-full pl-9 pr-3 py-2 rounded-lg border-gray-300 dark:border-gray-600 dark:bg-gray-900 text-sm"
              />
            </div>
            {searching && (
              <p className="text-xs text-gray-400 mt-1 flex items-center gap-1">
                <Loader2 className="w-3 h-3 animate-spin" />
              </p>
            )}
            {!selected && debounced.length >= 2 && candidates && candidates.length === 0 && !searching && (
              <p className="text-xs text-gray-400 mt-1">{t('relations.noCandidate')}</p>
            )}
            {!selected && (candidates?.length ?? 0) > 0 && (
              <ul className="mt-2 max-h-40 overflow-y-auto rounded-lg border border-gray-200 dark:border-gray-700 divide-y divide-gray-100 dark:divide-gray-700/60">
                {(candidates ?? []).slice(0, 8).map((c) => (
                  <li key={c.id}>
                    <button
                      type="button"
                      onClick={() => setSelected(c)}
                      className="w-full text-left px-3 py-2 text-sm hover:bg-gray-50 dark:hover:bg-gray-700/40 cursor-pointer"
                    >
                      {`${c.firstName ?? ''} ${c.lastName ?? ''}`.trim() || c.email}
                      {c.email ? <span className="text-xs text-gray-400 ml-2">{c.email}</span> : null}
                    </button>
                  </li>
                ))}
              </ul>
            )}
            {selected && (
              <div className="mt-2 inline-flex items-center gap-2 px-3 py-1.5 rounded-full bg-emerald-50 dark:bg-emerald-900/30 text-emerald-700 dark:text-emerald-300 text-sm">
                {`${selected.firstName ?? ''} ${selected.lastName ?? ''}`.trim() || selected.email}
                <button
                  type="button"
                  onClick={() => setSelected(null)}
                  className="cursor-pointer"
                  aria-label={t('relations.clearSelection')}
                >
                  <X className="w-3.5 h-3.5" />
                </button>
              </div>
            )}
          </div>
          <div>
            <label htmlFor="relation-note" className="block text-xs font-medium text-gray-500 mb-1">
              {t('relations.noteLabel')}
            </label>
            <textarea
              id="relation-note"
              value={note}
              onChange={(e) => setNote(e.target.value)}
              rows={2}
              className="w-full rounded-lg border-gray-300 dark:border-gray-600 dark:bg-gray-900 text-sm"
              placeholder={t('relations.notePlaceholder')}
            />
          </div>
          <button
            type="button"
            disabled={!selected || declareMutation.isPending}
            onClick={() => declareMutation.mutate()}
            className="px-4 py-2 rounded-lg text-sm font-medium bg-primary-600 text-white hover:bg-primary-700 disabled:opacity-50 cursor-pointer"
          >
            {declareMutation.isPending ? (
              <Loader2 className="w-4 h-4 animate-spin inline" />
            ) : (
              t('relations.submit')
            )}
          </button>
        </div>
      )}

      {isError ? (
        <div className="mt-4 flex items-center justify-between gap-3 rounded-lg border border-red-200 dark:border-red-900/60 bg-red-50 dark:bg-red-950/30 px-3 py-2">
          <p className="text-sm text-red-700 dark:text-red-300">{t('relations.loadError')}</p>
          <button
            type="button"
            onClick={() => refetch()}
            className="text-xs px-2 py-1 rounded-lg bg-red-100 dark:bg-red-900/40 text-red-700 dark:text-red-200 cursor-pointer"
          >
            {t('relations.retry')}
          </button>
        </div>
      ) : isLoading ? (
        <div className="flex items-center gap-2 text-sm text-gray-400 mt-4">
          <Loader2 className="w-4 h-4 animate-spin" /> {t('relations.loading')}
        </div>
      ) : (
        <div className="mt-4 grid grid-cols-1 lg:grid-cols-2 gap-4">
          <div>
            <p className="text-[10px] font-semibold text-gray-400 uppercase tracking-wider mb-2 flex items-center gap-1.5">
              <HeartHandshake className="w-3.5 h-3.5" /> {t('relations.myCovering')}
            </p>
            {sortantes.length === 0 ? (
              <p className="text-sm text-gray-400">{t('relations.emptyCovering')}</p>
            ) : (
              <ul className="space-y-2">
                {sortantes.map((r) => (
                  <li
                    key={r.id}
                    className="flex items-center justify-between gap-2 rounded-lg border border-gray-100 dark:border-gray-700/60 px-3 py-2"
                  >
                    <PersonCell row={r} onOpenUser={openUser} />
                    {r.revocable !== false && (
                      <button
                        type="button"
                        onClick={() => revokeMutation.mutate(r.id)}
                        disabled={revokeMutation.isPending}
                        className="shrink-0 text-xs px-2 py-1 rounded-lg text-red-600 hover:bg-red-50 dark:hover:bg-red-900/30 disabled:opacity-50 cursor-pointer"
                      >
                        {t('relations.remove')}
                      </button>
                    )}
                  </li>
                ))}
              </ul>
            )}
          </div>

          <div>
            <p className="text-[10px] font-semibold text-gray-400 uppercase tracking-wider mb-2 flex items-center gap-1.5">
              <Users className="w-3.5 h-3.5" /> {t('relations.myMembers')}
              {totalMembers > 0 && (
                <span className="rounded-full bg-gray-100 dark:bg-gray-700 px-1.5 text-[9px] font-semibold text-gray-600 dark:text-gray-300">
                  {totalMembers}
                </span>
              )}
            </p>
            {membersLoading && members.length === 0 ? (
              <p className="text-xs text-gray-400 flex items-center gap-1.5">
                <Loader2 className="w-3 h-3 animate-spin" /> {t('relations.loading')}
              </p>
            ) : members.length === 0 ? (
              <p className="text-sm text-gray-400">{t('relations.emptyMembers')}</p>
            ) : (
              <>
                <ul className="space-y-2">
                  {members.map((r) => (
                    <li
                      key={r.id}
                      className="flex items-center justify-between gap-2 rounded-lg border border-gray-100 dark:border-gray-700/60 px-3 py-2"
                    >
                      <PersonCell row={r} onOpenUser={openUser} />
                      {r.revocable === true && (
                        <button
                          type="button"
                          onClick={() => revokeMutation.mutate(r.id)}
                          disabled={revokeMutation.isPending}
                          className="shrink-0 text-xs px-2 py-1 rounded-lg text-amber-700 hover:bg-amber-50 dark:text-amber-300 dark:hover:bg-amber-900/30 disabled:opacity-50 cursor-pointer"
                        >
                          {t('relations.detach')}
                        </button>
                      )}
                    </li>
                  ))}
                </ul>
                {totalPages > 1 && (
                  <div className="mt-3 flex items-center justify-between gap-2">
                    <button
                      type="button"
                      onClick={() => setMembersPage((p) => Math.max(0, p - 1))}
                      disabled={membersPage === 0 || membersLoading}
                      aria-label={t('relations.previousPage')}
                      className="inline-flex items-center gap-1 text-xs px-2 py-1 rounded-lg border border-gray-200 dark:border-gray-700 disabled:opacity-40 cursor-pointer"
                    >
                      <ChevronLeft className="w-3 h-3" /> {t('relations.previousPage')}
                    </button>
                    <span className="text-[11px] text-gray-400">
                      {t('relations.page')} {membersPage + 1} / {totalPages}
                    </span>
                    <button
                      type="button"
                      onClick={() => setMembersPage((p) => p + 1)}
                      disabled={membersPage + 1 >= totalPages || membersLoading}
                      aria-label={t('relations.nextPage')}
                      className="inline-flex items-center gap-1 text-xs px-2 py-1 rounded-lg border border-gray-200 dark:border-gray-700 disabled:opacity-40 cursor-pointer"
                    >
                      {t('relations.nextPage')} <ChevronRight className="w-3 h-3" />
                    </button>
                  </div>
                )}
              </>
            )}
            <p className="text-[11px] text-gray-400 mt-2">{t('relations.membersHint')}</p>
          </div>
        </div>
      )}
    </div>
  );
}

/**
 * Cellule « personne » : le nom est un bouton qui ouvre la fiche complète.
 * C'est l'exigence explicite (« on peut cliquer dans la liste des
 * utilisateurs ») ; sans `onOpenUser`, on retombe sur du texte simple.
 */
function PersonCell({ row, onOpenUser }: { row: RelationRow; onOpenUser?: (id: string) => void }) {
  const { t } = useI18n();
  const content = (
    <>
      <p className="text-sm font-medium text-gray-800 dark:text-gray-200 truncate">{row.otherNom}</p>
      <p className="text-xs text-gray-400">{row.typeLabel}</p>
    </>
  );
  if (!onOpenUser) {
    return <div className="min-w-0">{content}</div>;
  }
  return (
    <button
      type="button"
      onClick={() => onOpenUser(row.otherUserId)}
      title={t('relations.openProfile')}
      className="min-w-0 text-left hover:underline cursor-pointer"
    >
      {content}
    </button>
  );
}
