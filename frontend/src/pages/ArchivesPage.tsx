import { useEffect, useRef, useState, type ReactNode } from 'react';
import { keepPreviousData, useQuery } from '@tanstack/react-query';
import { Archive, CalendarDays, Users2, ClipboardList, X, Loader2, ShieldCheck, Clock3, Shirt, Package, ChevronLeft, ChevronRight } from 'lucide-react';
import type { LucideIcon } from 'lucide-react';
import api from '@/lib/api';
import { tText, getI18nLocale } from '@/i18n';
import type { PageResponse } from '@/types';

/**
 * V271 — Écran « Archives » (§G3.4 / §G6.4).
 *
 * Consultation des snapshots d'événements produits au clôturage
 * (GET /api/v1/church-events/archives — par année / mois / espace).
 * Les snapshots sont rendus en libellés français lisibles : aucune
 * arborescence JSON brute, aucune clé technique exposée.
 *
 * Additif : la page ne fait qu'exposer des données déjà archivées ;
 * rien n'y est supprimé ni modifiable.
 *
 * V272 — parcours complet, côté lecture :
 *  - la liste passe par `GET /archives/search` (paginé, résumés SANS
 *    instantané : plus aucun téléchargement intégral) ;
 *  - les filtres année/mois sont alimentés par `GET /archives/facets`
 *    (les années réellement archivées, côté serveur) ;
 *  - le détail est chargé à la demande par `GET /archives/{id}` ;
 *  - navigation entre les versions d'un même événement via `eventId`
 *    (le serveur ne crée qu'en ajout : aucune version écrasée) ;
 *  - Échap ferme la fiche, le focus reste dans la boîte de dialogue,
 *    pagination Précédent/Suivant ;
 *  - les mois et les dates suivent la locale active (`Intl.DateTimeFormat`),
 *    plus de français codé en dur ; les statuts passent par `tText`.
 */

/**
 * Sections du snapshot d'événement archivé. Le payload est un JSON
 * historique : chaque champ est optionnel et les tableaux sont gardés par
 * Array.isArray à l'usage — typé, sans `any`.
 */
interface SnapSpace { spaceName?: string; role?: string }
interface SnapTeam { name?: string; description?: string }
interface SnapTask { title?: string; status?: string; dueAt?: string }
interface SnapScheduleItem { title?: string; startAt?: string }
interface SnapAsset { quantity?: number }
interface SnapDressRule { groupName?: string; description?: string }
interface SnapDressCode { title?: string; serviceName?: string; status?: string; rules?: SnapDressRule[] }
interface EventSnapshot {
  startAt?: string;
  endAt?: string;
  type?: string;
  description?: string;
  attendanceCount?: number;
  spaces?: SnapSpace[];
  teams?: SnapTeam[];
  tasks?: SnapTask[];
  schedule?: SnapScheduleItem[];
  assets?: SnapAsset[];
  dressCodes?: SnapDressCode[];
}

/** Détail complet (GET /archives/{id}) — l'instantané n'arrive que ici. */
interface EventArchive {
  id: string;
  eventId: string;
  eventTitle: string;
  eventStartAt: string;
  eventYear: number;
  eventMonth: number;
  spaceId?: string | null;
  version: number;
  /** Colonne dénormalisée V272 §3 ; le snapshot garde la valeur historique. */
  attendanceCount?: number;
  archivedAt: string;
  snapshotJson: EventSnapshot;
}

/** Ligne de liste (GET /archives/search) — résumé sans instantané. */
interface ArchiveSummary {
  id: string;
  eventId: string;
  eventTitle: string;
  eventStartAt: string;
  eventYear: number;
  eventMonth: number;
  spaceId?: string | null;
  version: number;
  attendanceCount?: number;
  archivedAt: string;
  archivedBy?: string | null;
}

/** Facette (GET /archives/facets) — années/mois réellement archivés. */
interface ArchiveFacet {
  eventYear: number;
  months: number[];
  archiveCount: number;
}

/** Repli de libellé si `Intl` était indisponible (fr) — jamais premier choix. */
const MOIS = [
  'Janvier', 'Février', 'Mars', 'Avril', 'Mai', 'Juin',
  'Juillet', 'Août', 'Septembre', 'Octobre', 'Novembre', 'Décembre',
];

/** Libellé du mois dans la locale active (V272 — plus de français en dur). */
const monthLabel = (m: number): string => {
  try {
    return new Intl.DateTimeFormat(getI18nLocale(), { month: 'long' })
      .format(new Date(2000, m - 1, 1));
  } catch {
    return MOIS[m - 1] ?? String(m);
  }
};

/** Statuts de tâches/tenues : rendu humain traduit, jamais la constante brute. */
const STATUT_LABELS: Record<string, string> = {
  PENDING: 'En attente',
  TODO: 'À faire',
  IN_PROGRESS: 'En cours',
  DONE: 'Terminé',
  COMPLETED: 'Terminé',
  CANCELLED: 'Annulé',
  ARCHIVED: 'Archivé',
  ARCHIVE: 'Archivé',
  ACTIVE: 'Actif',
  DRAFT: 'Brouillon',
};

const humanStatut = (s?: string | null) =>
  s ? tText(STATUT_LABELS[s.toUpperCase()] || s.toLowerCase().replace(/_/g, ' ')) : '';

const fmtDate = (iso?: string | null) => {
  if (!iso) return '—';
  const d = new Date(iso);
  return Number.isNaN(d.getTime())
    ? '—'
    : d.toLocaleDateString(getI18nLocale(), { day: 'numeric', month: 'long', year: 'numeric' });
};

const fmtDateHeure = (iso?: string | null) => {
  if (!iso) return '—';
  const d = new Date(iso);
  return Number.isNaN(d.getTime())
    ? '—'
    : d.toLocaleString(getI18nLocale(), { day: 'numeric', month: 'short', year: 'numeric', hour: '2-digit', minute: '2-digit' });
};

function SnapshotSection({ icon: Icon, title, empty, children }: {
  icon: LucideIcon; title: string; empty: boolean; children: ReactNode;
}) {
  if (empty) return null;
  return (
    <div className="glass-card p-4">
      <h4 className="flex items-center gap-2 text-sm font-bold text-gray-900 dark:text-gray-100 mb-2">
        <Icon className="w-4 h-4 text-indigo-500" /> {title}
      </h4>
      {children}
    </div>
  );
}

/**
 * Détail d'une archive : snapshot rendu en libellés lisibles (V272 — la
 * page est multilingue, le français ne reste qu'en source de traduction).
 * Échap ferme ; le focus reste dans la boîte (pieuille à Tab/Shift+Tab) ;
 * navigation entre les versions du même événement.
 */
function ArchiveDetail({ archive, versions, loadingVersions, onSelectVersion, onClose }: {
  archive: EventArchive;
  versions: ArchiveSummary[];
  loadingVersions: boolean;
  onSelectVersion: (id: string) => void;
  onClose: () => void;
}) {
  const snap = archive.snapshotJson || {};
  const spaces = Array.isArray(snap.spaces) ? snap.spaces : [];
  const teams = Array.isArray(snap.teams) ? snap.teams : [];
  const tasks = Array.isArray(snap.tasks) ? snap.tasks : [];
  const schedule = Array.isArray(snap.schedule) ? snap.schedule : [];
  const assets = Array.isArray(snap.assets) ? snap.assets : [];
  const dressCodes = Array.isArray(snap.dressCodes) ? snap.dressCodes : [];

  const boxRef = useRef<HTMLDivElement>(null);
  useEffect(() => {
    const box = boxRef.current;
    if (!box) return;
    const previous = document.activeElement as HTMLElement | null;
    box.focus();
    const onKey = (e: KeyboardEvent) => {
      if (e.key === 'Escape') {
        e.stopPropagation();
        onClose();
        return;
      }
      if (e.key !== 'Tab') return;
      // Pieuille de focus : Tab/Shift+Tab restent dans la boîte de dialogue.
      const focusable = Array.from(
        box.querySelectorAll<HTMLElement>('button, [href], input, select, textarea, [tabindex]:not([tabindex="-1"])'),
      ).filter((el) => !el.hasAttribute('disabled'));
      if (focusable.length === 0) return;
      const first = focusable[0];
      const last = focusable[focusable.length - 1];
      const active = document.activeElement;
      if (e.shiftKey && (active === first || active === box)) {
        e.preventDefault();
        last.focus();
      } else if (!e.shiftKey && active === last) {
        e.preventDefault();
        first.focus();
      }
    };
    box.addEventListener('keydown', onKey);
    return () => {
      box.removeEventListener('keydown', onKey);
      previous?.focus?.();
    };
  }, [onClose]);

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4" onClick={onClose}>
      <div
        ref={boxRef}
        tabIndex={-1}
        className="glass-card w-full max-w-2xl max-h-[85vh] overflow-y-auto p-6 space-y-4 outline-none"
        onClick={(e) => e.stopPropagation()}
        role="dialog"
        aria-modal="true"
        aria-label={archive.eventTitle}
      >
        <div className="flex items-start justify-between gap-4">
          <div>
            <h3 className="text-lg font-bold text-gray-900 dark:text-gray-100">{archive.eventTitle}</h3>
            <p className="text-xs text-gray-500 dark:text-gray-400 mt-1">
              {tText('Archivé le')} {fmtDateHeure(archive.archivedAt)} · {tText('Version')} {archive.version}
            </p>
          </div>
          <button onClick={onClose} className="p-1.5 rounded-lg text-gray-400 hover:text-gray-700 dark:hover:text-gray-200 cursor-pointer" aria-label={tText('Fermer')}>
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* V272 — navigation de versions : toutes les clôtures du même
            événement, aucune n'écrase l'historique (index unique V272 §2). */}
        {(loadingVersions || versions.length > 1) && (
          <div className="flex flex-wrap items-center gap-2 text-xs">
            <span className="text-gray-500 dark:text-gray-400">{tText('Autres versions de cet événement')}</span>
            {loadingVersions && <Loader2 className="w-3.5 h-3.5 animate-spin text-indigo-500" />}
            {versions.map((v) => (
              <button
                key={v.id}
                onClick={() => onSelectVersion(v.id)}
                aria-label={`${tText('Version')} ${v.version}`}
                className={`px-2 py-1 rounded-lg border transition-colors cursor-pointer ${
                  v.id === archive.id
                    ? 'border-indigo-500 bg-indigo-500/10 text-indigo-600 dark:text-indigo-300 font-bold'
                    : 'border-white/15 text-gray-600 dark:text-gray-300 hover:border-indigo-400'
                }`}
              >
                v{v.version} · {fmtDate(v.archivedAt)}
              </button>
            ))}
          </div>
        )}

        <div className="grid grid-cols-1 sm:grid-cols-2 gap-3 text-sm">
          <div><span className="text-gray-500 dark:text-gray-400">{tText('Début')} :</span> {fmtDate(snap.startAt || archive.eventStartAt)}</div>
          <div><span className="text-gray-500 dark:text-gray-400">{tText('Fin')} :</span> {fmtDate(snap.endAt)}</div>
          {snap.type && <div><span className="text-gray-500 dark:text-gray-400">{tText('Type')} :</span> {String(snap.type).toLowerCase().replace(/_/g, ' ')}</div>}
          <div>
            <span className="text-gray-500 dark:text-gray-400">{tText('Présences')} :</span>{' '}
            {archive.attendanceCount ?? snap.attendanceCount ?? 0}
          </div>
        </div>

        {snap.description && (
          <p className="text-sm text-gray-700 dark:text-gray-300 whitespace-pre-wrap">{snap.description}</p>
        )}

        <SnapshotSection icon={Users2} title={tText('Espaces')} empty={spaces.length === 0}>
          <ul className="space-y-1 text-sm text-gray-700 dark:text-gray-300">
            {spaces.map((s, i) => (
              <li key={i}>
                {s.spaceName || tText('Espace')}
                {s.role ? <span className="text-gray-400"> — {String(s.role).toLowerCase().replace(/_/g, ' ')}</span> : null}
              </li>
            ))}
          </ul>
        </SnapshotSection>

        <SnapshotSection icon={ClipboardList} title={tText('Équipes')} empty={teams.length === 0}>
          <ul className="space-y-1 text-sm text-gray-700 dark:text-gray-300">
            {teams.map((t, i) => <li key={i}>{t.name}{t.description ? <span className="text-gray-400"> — {t.description}</span> : null}</li>)}
          </ul>
        </SnapshotSection>

        <SnapshotSection icon={ClipboardList} title={tText('Tâches')} empty={tasks.length === 0}>
          <ul className="space-y-1 text-sm text-gray-700 dark:text-gray-300">
            {tasks.map((t, i) => (
              <li key={i} className="flex items-center gap-2">
                <span className="w-1.5 h-1.5 rounded-full bg-indigo-400 shrink-0" />
                <span className="flex-1">{t.title}</span>
                {humanStatut(t.status) && <span className="badge text-[10px] badge-info">{humanStatut(t.status)}</span>}
                {t.dueAt && <span className="text-xs text-gray-400">{fmtDate(t.dueAt)}</span>}
              </li>
            ))}
          </ul>
        </SnapshotSection>

        <SnapshotSection icon={Clock3} title={tText('Programme')} empty={schedule.length === 0}>
          <ul className="space-y-1 text-sm text-gray-700 dark:text-gray-300">
            {schedule.map((s, i) => (
              <li key={i}>{s.title}{s.startAt ? <span className="text-gray-400"> — {fmtDateHeure(s.startAt)}</span> : null}</li>
            ))}
          </ul>
        </SnapshotSection>

        <SnapshotSection icon={Package} title={tText('Matériel')} empty={assets.length === 0}>
          <p className="text-sm text-gray-700 dark:text-gray-300">
            {assets.reduce((n, a) => n + (typeof a.quantity === 'number' ? a.quantity : 0), 0)} {tText('articles réservés')}
          </p>
        </SnapshotSection>

        <SnapshotSection icon={Shirt} title={tText('Tenues (dress-codes)')} empty={dressCodes.length === 0}>
          <ul className="space-y-2 text-sm text-gray-700 dark:text-gray-300">
            {dressCodes.map((d, i) => (
              <li key={i}>
                <span className="font-medium">{d.title}</span>
                {d.serviceName ? <span className="text-gray-400"> — {d.serviceName}</span> : null}
                {humanStatut(d.status) && <span className="badge text-[10px] badge-gray ml-2">{humanStatut(d.status)}</span>}
                {Array.isArray(d.rules) && d.rules.length > 0 && (
                  <ul className="ml-4 mt-1 space-y-0.5 text-xs text-gray-500 dark:text-gray-400">
                    {d.rules.map((r, j) => (
                      <li key={j}>{r.groupName}{r.description ? ` — ${r.description}` : ''}</li>
                    ))}
                  </ul>
                )}
              </li>
            ))}
          </ul>
        </SnapshotSection>

        <div className="flex items-center gap-2 text-xs text-gray-400 pt-2 border-t border-white/10">
          <ShieldCheck className="w-3.5 h-3.5" />
          {tText('Instantané immuable : les archives ne sont jamais écrasées, chaque nouvelle clôture crée une version.')}
        </div>
      </div>
    </div>
  );
}

/** Taille de page arrêtée avec le serveur (`size` borné à 50 sur /search). */
const PAGE_SIZE = 12;

export default function ArchivesPage() {
  const [year, setYear] = useState<string>('');
  const [month, setMonth] = useState<string>('');
  const [page, setPage] = useState(0);
  const [openId, setOpenId] = useState<string | null>(null);

  // Facettes serveur (§V272) : seules les années/mois réellement archivés
  // alimentent les filtres — aucun listing en dur, aucun téléchargement.
  const facetsQuery = useQuery<ArchiveFacet[]>({
    queryKey: ['church-event-archives', 'facets'],
    queryFn: async () => {
      const res = await api.get('/church-events/archives/facets');
      return res.data as ArchiveFacet[];
    },
  });
  const facets = facetsQuery.data ?? [];
  const years = facets.map((f) => f.eventYear).sort((a, b) => b - a);
  const months = facets.find((f) => String(f.eventYear) === year)?.months ?? [];

  // Liste paginée de résumés — l'instantané complet n'arrive qu'au détail.
  const searchQuery = useQuery<PageResponse<ArchiveSummary>>({
    queryKey: ['church-event-archives', 'search', year || 'all', month || 'all', page],
    queryFn: async () => {
      const params = new URLSearchParams({ page: String(page), size: String(PAGE_SIZE) });
      if (year) params.set('year', year);
      if (month) params.set('month', month);
      const res = await api.get(`/church-events/archives/search?${params.toString()}`);
      return res.data as PageResponse<ArchiveSummary>;
    },
    placeholderData: keepPreviousData,
  });
  const summaries = searchQuery.data?.content ?? [];
  const totalElements = searchQuery.data?.totalElements ?? 0;
  const totalPages = searchQuery.data?.totalPages ?? 0;

  // Détail à la demande (GET /archives/{id}) : seul ce clic charge l'instantané.
  const detailQuery = useQuery<EventArchive>({
    queryKey: ['church-event-archives', 'detail', openId],
    queryFn: async () => {
      const res = await api.get(`/church-events/archives/${openId}`);
      return res.data as EventArchive;
    },
    enabled: !!openId,
  });

  // Versions du même événement (navigation V272) — réutilise /search?eventId.
  const openedEventId = detailQuery.data?.eventId;
  const versionsQuery = useQuery<PageResponse<ArchiveSummary>>({
    queryKey: ['church-event-archives', 'search', 'versions', openedEventId],
    queryFn: async () => {
      const res = await api.get(`/church-events/archives/search?eventId=${openedEventId}&size=50`);
      return res.data as PageResponse<ArchiveSummary>;
    },
    enabled: !!openedEventId,
  });
  const versions = versionsQuery.data?.content ?? [];

  const resetPage = () => setPage(0);

  return (
    <div className="p-6 max-w-5xl mx-auto">
      <div className="flex items-center gap-3 mb-1">
        <Archive className="w-6 h-6 text-indigo-500" />
        <h1 className="text-2xl font-bold text-gray-900 dark:text-gray-100">{tText('Archives')}</h1>
      </div>
      <p className="text-sm text-gray-500 dark:text-gray-400 mb-6">
        {tText('Consultez les instantanés des événements clôturés — par année et par mois.')}
      </p>

      {/* Filtres année / mois (§G3.4) — servis par /archives/facets (V272) */}
      <div className="flex flex-wrap gap-3 mb-6">
        <select
          className="input w-auto"
          value={year}
          onChange={(e) => { setYear(e.target.value); setMonth(''); resetPage(); }}
          aria-label={tText('Année')}
        >
          <option value="">{tText('Toutes les années')}</option>
          {years.map((y) => <option key={y} value={y}>{y}</option>)}
        </select>
        <select
          className="input w-auto"
          value={month}
          onChange={(e) => { setMonth(e.target.value); resetPage(); }}
          aria-label={tText('Mois')}
          disabled={!year}
        >
          <option value="">{tText('Tous les mois')}</option>
          {months.map((m) => <option key={m} value={m}>{monthLabel(m)}</option>)}
        </select>
        {(year || month) && (
          <button className="btn-secondary btn-sm" onClick={() => { setYear(''); setMonth(''); resetPage(); }}>
            {tText('Réinitialiser')}
          </button>
        )}
      </div>

      {searchQuery.isLoading && (
        <div className="flex items-center justify-center py-16">
          <Loader2 className="w-6 h-6 animate-spin text-indigo-500" />
        </div>
      )}

      {searchQuery.error && (
        <div className="glass-card p-6 text-sm text-red-600 dark:text-red-400">
          {tText('Impossible de charger les archives.')}
        </div>
      )}

      {!searchQuery.isLoading && !searchQuery.error && summaries.length === 0 && (
        <div className="glass-card p-10 text-center">
          <CalendarDays className="w-10 h-10 text-gray-300 dark:text-gray-600 mx-auto mb-3" />
          <p className="text-sm text-gray-500 dark:text-gray-400">
            {tText('Aucune archive pour cette période. Un événement apparaît ici après sa clôture.')}
          </p>
        </div>
      )}

      {!searchQuery.isLoading && summaries.length > 0 && (
        <>
          <div className={`grid grid-cols-1 sm:grid-cols-2 gap-4 ${searchQuery.isFetching ? 'opacity-60' : ''}`}>
            {summaries.map((a) => (
              <button
                key={a.id}
                onClick={() => setOpenId(a.id)}
                className="glass-card p-4 text-left hover:ring-2 hover:ring-indigo-500/40 transition-all cursor-pointer"
              >
                <div className="flex items-start justify-between gap-3">
                  <h3 className="text-sm font-bold text-gray-900 dark:text-gray-100">{a.eventTitle}</h3>
                  <span className="badge text-[10px] badge-info shrink-0">v{a.version}</span>
                </div>
                <p className="text-xs text-gray-500 dark:text-gray-400 mt-1">
                  {fmtDate(a.eventStartAt)} · {tText('Archivé le')} {fmtDate(a.archivedAt)}
                </p>
                <p className="text-xs text-gray-400 dark:text-gray-500 mt-2 flex items-center gap-1">
                  <Users2 className="w-3.5 h-3.5" /> {a.attendanceCount ?? 0} {tText('présences')}
                </p>
              </button>
            ))}
          </div>

          {/* Pagination Précédent/Suivant (V272 — consultation bornée) */}
          <div className="flex items-center justify-between gap-3 mt-6 text-sm text-gray-500 dark:text-gray-400">
            <span>
              {totalElements} {tText('archives')} · {tText('Page')} {page + 1}/{Math.max(totalPages, 1)}
            </span>
            <div className="flex items-center gap-2">
              <button
                className="btn-secondary btn-sm flex items-center gap-1"
                onClick={() => setPage((p) => Math.max(0, p - 1))}
                disabled={page === 0 || searchQuery.isFetching}
              >
                <ChevronLeft className="w-4 h-4" /> {tText('Précédent')}
              </button>
              <button
                className="btn-secondary btn-sm flex items-center gap-1"
                onClick={() => setPage((p) => p + 1)}
                disabled={page + 1 >= totalPages || searchQuery.isFetching}
              >
                {tText('Suivant')} <ChevronRight className="w-4 h-4" />
              </button>
            </div>
          </div>
        </>
      )}

      {openId && detailQuery.isLoading && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50" onClick={() => setOpenId(null)}>
          <div className="glass-card p-8 flex items-center gap-3" onClick={(e) => e.stopPropagation()}>
            <Loader2 className="w-5 h-5 animate-spin text-indigo-500" />
            <span className="text-sm text-gray-500 dark:text-gray-400">{tText('Chargement…')}</span>
          </div>
        </div>
      )}

      {openId && detailQuery.data && (
        <ArchiveDetail
          archive={detailQuery.data}
          versions={versions}
          loadingVersions={versionsQuery.isLoading}
          onSelectVersion={(id) => setOpenId(id)}
          onClose={() => setOpenId(null)}
        />
      )}

      {openId && detailQuery.error && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50" onClick={() => setOpenId(null)}>
          <div className="glass-card p-6 max-w-md text-sm text-red-600 dark:text-red-400" onClick={(e) => e.stopPropagation()} role="alertdialog" aria-modal="true">
            {tText('Impossible de charger les archives.')}
          </div>
        </div>
      )}
    </div>
  );
}
