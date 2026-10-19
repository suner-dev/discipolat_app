import { useState, type ReactNode } from 'react';
import { useQuery } from '@tanstack/react-query';
import { Archive, CalendarDays, Users2, ClipboardList, X, Loader2, ShieldCheck, Clock3, Shirt, Package } from 'lucide-react';
import type { LucideIcon } from 'lucide-react';
import api from '@/lib/api';
import { tText } from '@/i18n';

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
 */

interface EventArchive {
  id: string;
  eventId: string;
  eventTitle: string;
  eventStartAt: string;
  eventYear: number;
  eventMonth: number;
  spaceId?: string | null;
  version: number;
  archivedAt: string;
  snapshotJson: Record<string, any>;
}

const MOIS = [
  'Janvier', 'Février', 'Mars', 'Avril', 'Mai', 'Juin',
  'Juillet', 'Août', 'Septembre', 'Octobre', 'Novembre', 'Décembre',
];

/** Statuts de tâches/tenues : rendu humain, jamais la constante brute. */
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
  s ? (STATUT_LABELS[s.toUpperCase()] || s.toLowerCase().replace(/_/g, ' ')) : '';

const fmtDate = (iso?: string | null) => {
  if (!iso) return '—';
  const d = new Date(iso);
  return Number.isNaN(d.getTime())
    ? '—'
    : d.toLocaleDateString('fr-FR', { day: 'numeric', month: 'long', year: 'numeric' });
};

const fmtDateHeure = (iso?: string | null) => {
  if (!iso) return '—';
  const d = new Date(iso);
  return Number.isNaN(d.getTime())
    ? '—'
    : d.toLocaleString('fr-FR', { day: 'numeric', month: 'short', year: 'numeric', hour: '2-digit', minute: '2-digit' });
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

/** Détail d'une archive : snapshot rendu en libellés français. */
function ArchiveDetail({ archive, onClose }: { archive: EventArchive; onClose: () => void }) {
  const snap = archive.snapshotJson || {};
  const spaces: any[] = Array.isArray(snap.spaces) ? snap.spaces : [];
  const teams: any[] = Array.isArray(snap.teams) ? snap.teams : [];
  const tasks: any[] = Array.isArray(snap.tasks) ? snap.tasks : [];
  const schedule: any[] = Array.isArray(snap.schedule) ? snap.schedule : [];
  const assets: any[] = Array.isArray(snap.assets) ? snap.assets : [];
  const dressCodes: any[] = Array.isArray(snap.dressCodes) ? snap.dressCodes : [];

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4" onClick={onClose}>
      <div
        className="glass-card w-full max-w-2xl max-h-[85vh] overflow-y-auto p-6 space-y-4"
        onClick={(e) => e.stopPropagation()}
        role="dialog"
        aria-modal="true"
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

        <div className="grid grid-cols-1 sm:grid-cols-2 gap-3 text-sm">
          <div><span className="text-gray-500 dark:text-gray-400">{tText('Début')} :</span> {fmtDate(snap.startAt || archive.eventStartAt)}</div>
          <div><span className="text-gray-500 dark:text-gray-400">{tText('Fin')} :</span> {fmtDate(snap.endAt)}</div>
          {snap.type && <div><span className="text-gray-500 dark:text-gray-400">{tText('Type')} :</span> {String(snap.type).toLowerCase().replace(/_/g, ' ')}</div>}
          <div><span className="text-gray-500 dark:text-gray-400">{tText('Présences')} :</span> {snap.attendanceCount ?? 0}</div>
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
            {assets.reduce((n: number, a: any) => n + (typeof a.quantity === 'number' ? a.quantity : 0), 0)} {tText('articles réservés')}
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
                    {d.rules.map((r: any, j: number) => (
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

export default function ArchivesPage() {
  const [year, setYear] = useState<string>('');
  const [month, setMonth] = useState<string>('');
  const [openId, setOpenId] = useState<string | null>(null);

  const { data, isLoading, error } = useQuery<EventArchive[]>({
    queryKey: ['church-event-archives'],
    queryFn: async () => {
      const res = await api.get('/church-events/archives');
      return res.data;
    },
  });

  const archives = data ?? [];
  // Les filtres sont alimentés par les années réellement archivées : aucun
  // listing en dur, l'église décide de son propre historique.
  const years = [...new Set(archives.map((a) => a.eventYear))].sort((a, b) => b - a);
  const months = [...new Set(
    archives.filter((a) => !year || a.eventYear === Number(year)).map((a) => a.eventMonth),
  )].sort((a, b) => a - b);

  const filtered = archives.filter(
    (a) => (!year || a.eventYear === Number(year)) && (!month || a.eventMonth === Number(month)),
  );
  const opened = filtered.find((a) => a.id === openId) ?? archives.find((a) => a.id === openId);

  return (
    <div className="p-6 max-w-5xl mx-auto">
      <div className="flex items-center gap-3 mb-1">
        <Archive className="w-6 h-6 text-indigo-500" />
        <h1 className="text-2xl font-bold text-gray-900 dark:text-gray-100">{tText('Archives')}</h1>
      </div>
      <p className="text-sm text-gray-500 dark:text-gray-400 mb-6">
        {tText('Consultez les instantanés des événements clôturés — par année et par mois.')}
      </p>

      {/* Filtres année / mois (§G3.4) */}
      <div className="flex flex-wrap gap-3 mb-6">
        <select
          className="input w-auto"
          value={year}
          onChange={(e) => { setYear(e.target.value); setMonth(''); }}
          aria-label={tText('Année')}
        >
          <option value="">{tText('Toutes les années')}</option>
          {years.map((y) => <option key={y} value={y}>{y}</option>)}
        </select>
        <select
          className="input w-auto"
          value={month}
          onChange={(e) => setMonth(e.target.value)}
          aria-label={tText('Mois')}
          disabled={!year}
        >
          <option value="">{tText('Tous les mois')}</option>
          {months.map((m) => <option key={m} value={m}>{MOIS[m - 1]}</option>)}
        </select>
        {(year || month) && (
          <button className="btn-secondary btn-sm" onClick={() => { setYear(''); setMonth(''); }}>
            {tText('Réinitialiser')}
          </button>
        )}
      </div>

      {isLoading && (
        <div className="flex items-center justify-center py-16">
          <Loader2 className="w-6 h-6 animate-spin text-indigo-500" />
        </div>
      )}

      {error && (
        <div className="glass-card p-6 text-sm text-red-600 dark:text-red-400">
          {tText('Impossible de charger les archives.')}
        </div>
      )}

      {!isLoading && !error && filtered.length === 0 && (
        <div className="glass-card p-10 text-center">
          <CalendarDays className="w-10 h-10 text-gray-300 dark:text-gray-600 mx-auto mb-3" />
          <p className="text-sm text-gray-500 dark:text-gray-400">
            {tText('Aucune archive pour cette période. Un événement apparaît ici après sa clôture.')}
          </p>
        </div>
      )}

      <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
        {filtered.map((a) => (
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
              <Users2 className="w-3.5 h-3.5" /> {a.snapshotJson?.attendanceCount ?? 0} {tText('présences')}
            </p>
          </button>
        ))}
      </div>

      {opened && <ArchiveDetail archive={opened} onClose={() => setOpenId(null)} />}
    </div>
  );
}
