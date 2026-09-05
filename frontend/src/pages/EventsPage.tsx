import { useMemo, useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { Link } from 'react-router-dom';
import api, { getErrorMessage } from '@/lib/api';
import { useAuth } from '@/contexts/AuthContext';
import { usePlatformConfig } from '@/contexts/PlatformContext';
import DataTable from '@/components/shared/DataTable';
import AttachmentPicker from '@/components/shared/AttachmentPicker';
import AttachmentLinks from '@/components/shared/AttachmentLinks';
import { useDictionaries } from '@/hooks/useDictionaries';
import { useI18n } from '@/i18n';
import type { Evenement, PageResponse, TypeEvenement, StatutEvenement, User, DictionaryEntry } from '@/types';
import type { ColumnDef } from '@/types/table';
import {
  Calendar,
  Plus,
  Search,
  Filter,
  MapPin,
  Clock,
  Users,
  Loader2,
  X,
  CheckCircle2,
  UserMinus,
  Pencil,
  Trash2,
  Eye,
  UserCheck,
  UserX,
  LayoutDashboard,
  ChevronRight,
  Sparkles,
  BellRing,
  Paperclip,
  Download,
  List,
  Grid3X3,
} from 'lucide-react';
import toast from 'react-hot-toast';

/** Repli (dictionnaires indisponibles) — les valeurs réelles viennent de la base. */
const TYPE_LABELS: Record<string, string> = {
  SORTIE: 'Sortie',
  RETRAITE: 'Retraite',
  EVANGELISATION: 'Évangélisation',
  REUNION: 'Réunion',
  VISITE: 'Visite',
  CONFERENCE: 'Conférence',
  FORMATION: 'Formation',
  ANNIVERSAIRE: 'Anniversaire',
  CULTE: 'Culte',
  ETUDE_BIBLIQUE: 'Étude biblique',
  VEILLEE: 'Veillée',
  PRIERE: 'Temps de prière',
  AUTRE: 'Autre',
};

const STATUT_LABELS: Record<string, string> = {
  PLANIFIE: 'Planifié',
  EN_COURS: 'En cours',
  TERMINE: 'Terminé',
  ANNULE: 'Annulé',
};

interface EventFormState {
  titre: string;
  description: string;
  typeEvenement: TypeEvenement;
  dateDebut: string;
  dateFin: string;
  lieu: string;
  limitePlaces: number | undefined;
  statut?: StatutEvenement;
  fichierIds: string[];
}

function eventForm(
  key: string,
  form: EventFormState,
  update: (partial: Partial<EventFormState>) => void,
  submitLabel: string,
  onSubmit: () => void,
  isPending: boolean,
  onCancel: () => void,
  typeOptions: { code: string; label: string }[],
  statusOptions: { code: string; label: string }[]
) {
  const isEdit = submitLabel !== 'Créer';
  return (
    <div key={key} className="card p-6 mb-6 animate-slide-up">
      <div className="flex items-center justify-between mb-4">
        <h3 className="text-lg font-semibold text-gray-900 dark:text-gray-100">
          {isEdit ? "Modifier l'événement" : 'Nouvel événement'}
        </h3>
        {onCancel && (
          <button onClick={onCancel} className="p-1 rounded-lg hover:bg-gray-100 dark:hover:bg-gray-800">
            <X className="w-5 h-5 text-gray-500" />
          </button>
        )}
      </div>
      <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
        <div className="sm:col-span-2">
          <label className="label">Titre *</label>
          <input className="input" value={form.titre} onChange={(e) => update({ titre: e.target.value })} placeholder="Ex: Retraite de prière" />
        </div>
        <div className="sm:col-span-2">
          <label className="label">Description</label>
          <textarea className="input" rows={2} value={form.description} onChange={(e) => update({ description: e.target.value })} />
        </div>
        <div>
          <label className="label">Type *</label>
          <select className="input" value={form.typeEvenement} onChange={(e) => update({ typeEvenement: e.target.value as TypeEvenement })}>
            {typeOptions.map((o) => (
              <option key={o.code} value={o.code}>{o.label}</option>
            ))}
          </select>
        </div>
        <div>
          <label className="label">Lieu</label>
          <input className="input" value={form.lieu} onChange={(e) => update({ lieu: e.target.value })} placeholder="Ex: Salle paroissiale" />
        </div>
        <div>
          <label className="label">Date de début *</label>
          <input type="datetime-local" className="input" value={form.dateDebut} onChange={(e) => update({ dateDebut: e.target.value })} />
        </div>
        <div>
          <label className="label">Date de fin</label>
          <input type="datetime-local" className="input" value={form.dateFin} onChange={(e) => update({ dateFin: e.target.value })} />
        </div>
        <div>
          <label className="label">Limite de places</label>
          <input type="number" min={1} className="input" value={form.limitePlaces || ''} onChange={(e) => update({ limitePlaces: e.target.value ? parseInt(e.target.value) : undefined })} placeholder="Illimité si vide" />
        </div>
        {isEdit && form.statut && (
          <div>
            <label className="label">Statut</label>
            <select className="input" value={form.statut} onChange={(e) => update({ statut: e.target.value as StatutEvenement })}>
              {statusOptions.map((o) => (
                <option key={o.code} value={o.code}>{o.label}</option>
              ))}
            </select>
          </div>
        )}
      </div>
      <div className="mt-4">
        <label className="label flex items-center gap-1.5">
          <Paperclip className="w-3.5 h-3.5 text-gray-400" /> Pièces jointes
        </label>
        <AttachmentPicker value={form.fichierIds} onChange={(ids) => update({ fichierIds: ids })} />
      </div>
      <div className="flex justify-end gap-3 mt-4">
        {onCancel && <button onClick={onCancel} className="btn-secondary btn-sm">Annuler</button>}
        <button onClick={onSubmit} disabled={!form.titre || isPending} className="btn-primary btn-sm">
          {isPending ? <Loader2 className="w-4 h-4 animate-spin" /> : isEdit ? <Pencil className="w-4 h-4" /> : <Plus className="w-4 h-4" />}
          {submitLabel}
        </button>
      </div>
    </div>
  );
}

export default function EventsPage() {
  const { user } = useAuth();
  const queryClient = useQueryClient();
  const { locale } = useI18n();
  const dictionaries = useDictionaries();
  const { moduleEnabled } = usePlatformConfig();

  /** Types configurés (dictionnaire) — sinon repli sur les valeurs par défaut. */
  const typeEntries = useMemo<{ code: string; label: string; color?: string }[]>(() => {
    const configured = dictionaries.options('EVENT_TYPE');
    if (configured.length > 0) {
      return configured.map((e: DictionaryEntry) => ({ code: e.code, label: e.label, color: e.color }));
    }
    return Object.entries(TYPE_LABELS).map(([code, label]) => ({ code, label }));
  }, [dictionaries]);

  const typeLabel = (code: string) =>
    dictionaries.label('EVENT_TYPE', code) || TYPE_LABELS[code] || code;

  const statusEntries = useMemo<{ code: string; label: string }[]>(() => {
    const configured = dictionaries.options('EVENT_STATUS');
    if (configured.length > 0) {
      return configured.map((e: DictionaryEntry) => ({ code: e.code, label: e.label }));
    }
    return Object.entries(STATUT_LABELS).map(([code, label]) => ({ code, label }));
  }, [dictionaries]);

  const statusLabel = (code: string) =>
    dictionaries.label('EVENT_STATUS', code) || STATUT_LABELS[code] || code;
  const [view, setView] = useState<'list' | 'consolidated'>('list');
  const [displayMode, setDisplayMode] = useState<'table' | 'calendar'>('table');
  const [page, setPage] = useState(0);
  const [search, setSearch] = useState('');
  const [typeFilter, setTypeFilter] = useState<TypeEvenement | ''>('');
  const [statutFilter, setStatutFilter] = useState<StatutEvenement | ''>('');
  const [showFilters, setShowFilters] = useState(false);
  const [showCreate, setShowCreate] = useState(false);
  // Espace métier : le rôle ACTIF détermine ce qui est visible (et non l'ensemble des rôles)
  const isPasteurOrAdmin = user?.activeRole === 'PASTEUR' || user?.activeRole === 'ADMIN';
  const [showEdit, setShowEdit] = useState(false);
  const [editingEvent, setEditingEvent] = useState<Evenement | null>(null);
  const [showDeleteConfirm, setShowDeleteConfirm] = useState<string | null>(null);
  const [showAttendance, setShowAttendance] = useState<Evenement | null>(null);
  const [newEvent, setNewEvent] = useState({
    titre: '',
    description: '',
    typeEvenement: 'REUNION' as TypeEvenement,
    dateDebut: new Date().toISOString().slice(0, 16),
    dateFin: '',
    lieu: '',
    limitePlaces: undefined as number | undefined,
    fichierIds: [] as string[],
  });
  const [editEvent, setEditEvent] = useState({
    titre: '',
    description: '',
    typeEvenement: 'REUNION' as TypeEvenement,
    dateDebut: '',
    dateFin: '',
    lieu: '',
    limitePlaces: undefined as number | undefined,
    statut: 'PLANIFIE' as StatutEvenement,
    fichierIds: [] as string[],
  });

  if (!moduleEnabled('EVENTS')) {
    return (
      <div className="page-container flex flex-col items-center justify-center min-h-[60vh] text-center">
        <Calendar className="w-10 h-10 text-gray-300 mb-3" />
        <h1 className="text-lg font-semibold text-gray-800 dark:text-gray-100">Module Événements désactivé</h1>
        <p className="text-sm text-gray-400 mt-1">
          L'administrateur a désactivé ce module. Réactivez-le depuis l'espace d'administration.
        </p>
        <Link to="/dashboard" className="btn-ghost btn-sm mt-4">Retour au tableau de bord</Link>
      </div>
    );
  }

  const { data, isLoading } = useQuery({
    queryKey: ['events', page, search, typeFilter, statutFilter],
    queryFn: async () => {
      const params = new URLSearchParams({ size: '20', page: String(page) });
      if (search) params.set('search', search);
      if (typeFilter) params.set('typeEvenement', typeFilter);
      if (statutFilter) params.set('statut', statutFilter);
      const res = await api.get(`/events?${params}`);
      return res.data as PageResponse<Evenement>;
    },
  });

  const createMutation = useMutation({
    mutationFn: async (evt: typeof newEvent) => {
      await api.post('/events', {
        ...evt,
        dateFin: evt.dateFin || undefined,
        lieu: evt.lieu || undefined,
        limitePlaces: evt.limitePlaces || undefined,
        fichierIds: evt.fichierIds,
      });
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['events'] });
      toast.success('Événement créé');
      setShowCreate(false);
      setNewEvent({ titre: '', description: '', typeEvenement: 'REUNION', dateDebut: new Date().toISOString().slice(0, 16), dateFin: '', lieu: '', limitePlaces: undefined, fichierIds: [] });
    },
    onError: (err) => toast.error(getErrorMessage(err)),
  });

  const registerMutation = useMutation({
    mutationFn: async (eventId: string) => {
      await api.post(`/events/${eventId}/register`);
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['events'] });
      toast.success('Inscription confirmée');
    },
    onError: (err) => toast.error(getErrorMessage(err)),
  });

  const updateEventMutation = useMutation({
    mutationFn: async ({ id, data }: { id: string; data: typeof editEvent }) => {
      await api.put(`/events/${id}`, {
        titre: data.titre,
        description: data.description || undefined,
        typeEvenement: data.typeEvenement,
        dateDebut: data.dateDebut,
        dateFin: data.dateFin || undefined,
        lieu: data.lieu || undefined,
        limitePlaces: data.limitePlaces || undefined,
        statut: data.statut,
        fichierIds: data.fichierIds,
      });
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['events'] });
      toast.success('Événement mis à jour');
      setShowEdit(false);
      setEditingEvent(null);
    },
    onError: (err) => toast.error(getErrorMessage(err)),
  });

  const deleteEventMutation = useMutation({
    mutationFn: async (id: string) => {
      await api.delete(`/events/${id}`);
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['events'] });
      toast.success('Événement supprimé');
      setShowDeleteConfirm(null);
    },
    onError: (err) => toast.error(getErrorMessage(err)),
  });

  // Consolidated upcoming events (Pasteur only)
  const { data: consolidated } = useQuery({
    queryKey: ['events', 'consolidated'],
    queryFn: async () => {
      const res = await api.get('/events/consolidated?days=14');
      return res.data as {
        id: string; titre: string; typeEvenement: string;
        dateDebut: string; dateFin?: string; lieu?: string;
        statut: string; nbInscrits: number; limitePlaces?: number;
        organisateurNom?: string; organisateurRole?: string;
        familleId?: string;
      }[];
    },
    enabled: isPasteurOrAdmin && view === 'consolidated',
  });

  const { data: consolidatedStats } = useQuery({
    queryKey: ['events', 'consolidated', 'stats'],
    queryFn: async () => {
      const res = await api.get('/events/consolidated/by-family?days=14');
      return res.data as { total: number; parType: Record<string, number>; parFamille: Record<string, number> };
    },
    enabled: isPasteurOrAdmin && view === 'consolidated',
  });

  // User lookup cache
  const { data: allUsers } = useQuery({
    queryKey: ['users', 'all'],
    queryFn: async () => {
      const res = await api.get('/users?size=100');
      return res.data.content as User[];
    },
  });

  const getUserName = (userId: string) => {
    const user = allUsers?.find((u) => u.id === userId);
    return user ? `${user.firstName} ${user.lastName}` : userId.slice(0, 8) + '...';
  };

  const { data: attendanceData } = useQuery({
    queryKey: ['events', showAttendance?.id, 'registrations'],
    queryFn: async () => {
      const res = await api.get(`/events/${showAttendance?.id}/registrations`);
      return res.data as { id?: string; utilisateurId: string; statutInscription: string; present: boolean }[];
    },
    enabled: !!showAttendance,
  });

  const markAttendanceMutation = useMutation({
    mutationFn: async ({ eventId, userId, present }: { eventId: string; userId: string; present: boolean }) => {
      await api.post(`/events/${eventId}/attendance`, { userId, present });
    },
    onSuccess: (_data, variables) => {
      queryClient.invalidateQueries({ queryKey: ['events', variables.eventId, 'registrations'] });
      queryClient.invalidateQueries({ queryKey: ['events'] });
      toast.success('Présence enregistrée');
    },
    onError: (err) => toast.error(getErrorMessage(err)),
  });

  const openEdit = (evt: Evenement) => {
    setEditingEvent(evt);
    setEditEvent({
      titre: evt.titre,
      description: evt.description || '',
      typeEvenement: evt.typeEvenement,
      dateDebut: evt.dateDebut.slice(0, 16),
      dateFin: evt.dateFin ? evt.dateFin.slice(0, 16) : '',
      lieu: evt.lieu || '',
      limitePlaces: evt.limitePlaces,
      statut: evt.statut,
      fichierIds: evt.piecesJointes?.map(a => a.fileId) ?? [],
    });
    setShowEdit(true);
  };

  const columns: ColumnDef<Evenement>[] = [
    {
      header: 'Événement',
      cell: (evt) => (
        <div>
          <p className="text-sm font-medium text-gray-900 dark:text-gray-100">{evt.titre}</p>
          {evt.description && (
            <p className="text-xs text-gray-500 dark:text-gray-400 truncate max-w-xs">{evt.description}</p>
          )}
        </div>
      ),
    },
    {
      header: 'Type',
      cell: (evt) => (
        <span
          className="inline-flex items-center px-2 py-1 rounded-full text-xs font-medium bg-gray-100 text-gray-800 dark:bg-gray-700 dark:text-gray-300"
          style={dictionaries.color('EVENT_TYPE', evt.typeEvenement)
            ? { backgroundColor: `${dictionaries.color('EVENT_TYPE', evt.typeEvenement)}22`, color: dictionaries.color('EVENT_TYPE', evt.typeEvenement) }
            : undefined}
        >
          {typeLabel(evt.typeEvenement)}
        </span>
      ),
    },
    {
      header: 'Date',
      cell: (evt) => (
        <div className="flex items-center gap-1 text-sm text-gray-600 dark:text-gray-400">
          <Clock className="w-3 h-3" />
          {new Date(evt.dateDebut).toLocaleDateString(locale, { day: 'numeric', month: 'short', hour: '2-digit', minute: '2-digit' })}
        </div>
      ),
    },
    {
      header: 'Lieu',
      cell: (evt) => evt.lieu ? (
        <div className="flex items-center gap-1 text-sm text-gray-600 dark:text-gray-400">
          <MapPin className="w-3 h-3" />
          {evt.lieu}
        </div>
      ) : '-',
    },
    {
      header: 'Places',
      cell: (evt) => (
        <div className="flex items-center gap-1 text-sm text-gray-600 dark:text-gray-400">
          <Users className="w-3 h-3" />
          {evt.limitePlaces ? `${evt.nbInscrits}/${evt.limitePlaces}` : `${evt.nbInscrits} inscrits`}
        </div>
      ),
    },
    {
      header: 'Statut',
      cell: (evt) => (
        <span className="badge badge-info">
          {statusLabel(evt.statut)}
        </span>
      ),
    },
    {
      header: 'Pièces',
      cell: (evt) => <AttachmentLinks pieces={evt.piecesJointes} />,
    },
    {
      header: 'Actions',
      cell: (evt) => {
        const isOrganizer = evt.organisateurId === user?.id;
        const canManage = isOrganizer || user?.role === 'PASTEUR' || user?.role === 'RESPONSABLE';
        return (
          <div className="flex items-center gap-1">
            {evt.statut === 'PLANIFIE' && (
              <button
                onClick={() => registerMutation.mutate(evt.id)}
                disabled={registerMutation.isPending}
                className="btn-ghost btn-sm text-primary-600"
                title="S'inscrire"
              >
                {registerMutation.isPending ? <Loader2 className="w-4 h-4 animate-spin" /> : <CheckCircle2 className="w-4 h-4" />}
              </button>
            )}
            <button
              onClick={() => setShowAttendance(evt)}
              className="btn-ghost btn-sm text-gray-600"
              title="Voir les inscriptions"
            >
              <Eye className="w-4 h-4" />
            </button>
            {canManage && (
              <>
                <button
                  onClick={() => openEdit(evt)}
                  className="btn-ghost btn-sm text-blue-600"
                  title="Modifier"
                >
                  <Pencil className="w-4 h-4" />
                </button>
                <button
                  onClick={() => setShowDeleteConfirm(evt.id)}
                  className="btn-ghost btn-sm text-red-600"
                  title="Supprimer"
                >
                  <Trash2 className="w-4 h-4" />
                </button>
              </>
            )}
          </div>
        );
      },
    },
  ];

  const allEvents = data?.content || [];
  const evtStats = useMemo(() => ({
    total: data?.totalElements ?? allEvents.length,
    planifies: allEvents.filter(e => e.statut === 'PLANIFIE').length,
    enCours: allEvents.filter(e => e.statut === 'EN_COURS').length,
    termines: allEvents.filter(e => e.statut === 'TERMINE').length,
  }), [data, allEvents]);

  const exportCsv = () => {
    const rows = [['Titre', 'Type', 'Statut', 'Date début', 'Date fin', 'Lieu', 'Capacité', 'Inscriptions']];
    allEvents.forEach((e) => {
      rows.push([
        e.titre,
        TYPE_LABELS[e.typeEvenement] || e.typeEvenement,
        STATUT_LABELS[e.statut] || e.statut,
        e.dateDebut ? new Date(e.dateDebut).toLocaleDateString(locale) : '',
        e.dateFin ? new Date(e.dateFin).toLocaleDateString(locale) : '',
        e.lieu || '',
        String(e.limitePlaces || ''),
        String(e.nbInscrits || 0),
      ]);
    });
    const csv = rows.map(r => r.map(c => `"${c}"`).join(',')).join('\n');
    const blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `evenements_${new Date().toISOString().slice(0, 10)}.csv`;
    a.click();
    URL.revokeObjectURL(url);
  };

  // Calendar grid computation
  const calendarWeeks = useMemo(() => {
    const now = new Date();
    const year = now.getFullYear();
    const month = now.getMonth();
    const firstDay = new Date(year, month, 1);
    const lastDay = new Date(year, month + 1, 0);
    const weeks: { date: Date; events: Evenement[]; isCurrentMonth: boolean; isToday: boolean }[][] = [];
    let week: typeof weeks[0] = [];
    const startOffset = firstDay.getDay();
    for (let i = 0; i < startOffset; i++) {
      const d = new Date(year, month, -(startOffset - 1 - i));
      week.push({ date: d, events: [], isCurrentMonth: false, isToday: false });
    }
    for (let d = 1; d <= lastDay.getDate(); d++) {
      const date = new Date(year, month, d);
      const dayStr = date.toISOString().slice(0, 10);
      const dayEvents = allEvents.filter(e => {
        if (!e.dateDebut) return false;
        return new Date(e.dateDebut).toISOString().slice(0, 10) === dayStr;
      });
      week.push({ date, events: dayEvents, isCurrentMonth: true, isToday: d === now.getDate() });
      if (week.length === 7) { weeks.push(week); week = []; }
    }
    while (week.length > 0 && week.length < 7) {
      const lastDate = week[week.length - 1].date;
      const nextDate = new Date(lastDate.getFullYear(), lastDate.getMonth(), lastDate.getDate() + 1);
      week.push({ date: nextDate, events: [], isCurrentMonth: false, isToday: false });
    }
    if (week.length === 7) weeks.push(week);
    return weeks;
  }, [allEvents]);

  return (
    <div className="page-container">
      <div className="page-header">
        <div>
          <h1 className="page-title">Événements</h1>
          <p className="text-gray-500 dark:text-gray-400 mt-1">Gestion des événements de famille — {evtStats.total} événement(s)</p>
        </div>
        <div className="flex gap-2">
          {isPasteurOrAdmin && (
            <div className="tabs p-1 rounded-xl bg-white/50 dark:bg-gray-800/30 mr-2">
              <button
                onClick={() => setView('list')}
                className={view === 'list' ? 'tab-active text-xs px-3 py-1.5' : 'tab text-xs px-3 py-1.5'}
              >
                <Calendar className="w-3.5 h-3.5" /> Liste
              </button>
              <button
                onClick={() => setView('consolidated')}
                className={view === 'consolidated' ? 'tab-active text-xs px-3 py-1.5' : 'tab text-xs px-3 py-1.5'}
              >
                <LayoutDashboard className="w-3.5 h-3.5" /> Vue consolidée
              </button>
            </div>
          )}
          <div className="flex items-center gap-1 p-1 rounded-xl bg-white/50 dark:bg-gray-800/30">
            <button onClick={() => setDisplayMode('table')} className={displayMode === 'table' ? 'tab-active text-xs px-2 py-1' : 'tab text-xs px-2 py-1'} title="Vue tableau">
              <List className="w-3.5 h-3.5" />
            </button>
            <button onClick={() => setDisplayMode('calendar')} className={displayMode === 'calendar' ? 'tab-active text-xs px-2 py-1' : 'tab text-xs px-2 py-1'} title="Vue calendrier">
              <Grid3X3 className="w-3.5 h-3.5" />
            </button>
          </div>
          <button onClick={exportCsv} className="btn-secondary btn-sm">
            <Download className="w-4 h-4" /> Export
          </button>
          <button onClick={() => setShowFilters(!showFilters)} className={`btn-secondary btn-sm ${showFilters ? 'bg-primary-50' : ''}`}>
            <Filter className="w-4 h-4" /> Filtres
          </button>
          <button onClick={() => setShowCreate(!showCreate)} className="btn-primary btn-sm">
            <Plus className="w-4 h-4" /> Nouvel événement
          </button>
        </div>
      </div>

      {/* Stats cards */}
      <div className="grid grid-cols-2 sm:grid-cols-4 gap-3 mb-6">
        {[
          { label: 'Total', value: evtStats.total, icon: Calendar, color: 'from-primary-500 to-primary-600', filter: '' },
          { label: 'Planifiés', value: evtStats.planifies, icon: Clock, color: 'from-sky-500 to-blue-500', filter: 'PLANIFIE' },
          { label: 'En cours', value: evtStats.enCours, icon: CheckCircle2, color: 'from-amber-500 to-orange-500', filter: 'EN_COURS' },
          { label: 'Terminés', value: evtStats.termines, icon: Users, color: 'from-emerald-500 to-green-500', filter: 'TERMINE' },
        ].map((s, i) => (
          <button
            key={s.label}
            type="button"
            onClick={() => { if (s.filter) { setStatutFilter(statutFilter === (s.filter as StatutEvenement) ? '' : (s.filter as StatutEvenement)); setPage(0); } else { setStatutFilter(''); setPage(0); } }}
            className={`stat-card animate-slide-up text-left cursor-pointer hover:shadow-lg hover:-translate-y-0.5 transition-all duration-200 ${statutFilter === s.filter && s.filter ? 'ring-2 ring-primary-500/50' : ''}`}
            style={{ animationDelay: `${i * 50}ms` }}
          >
            <div className={`absolute top-0 left-0 right-0 h-0.5 bg-gradient-to-r ${s.color} opacity-60`} />
            <div className="flex items-start justify-between mb-2">
              <span className="stat-label text-[10px]">{s.label}</span>
              <div className={`p-1.5 rounded-lg bg-gradient-to-br ${s.color} text-white shadow-sm`}>
                <s.icon className="w-3.5 h-3.5" />
              </div>
            </div>
            <p className="stat-value text-xl">{s.value}</p>
          </button>
        ))}
      </div>

      {/* Create form */}
      {showCreate && eventForm('create', newEvent, (v) => setNewEvent({ ...newEvent, ...v }), 'Créer', () => createMutation.mutate(newEvent), createMutation.isPending, () => setShowCreate(false), typeEntries, statusEntries)}

      {/* Edit form */}
      {showEdit && editingEvent && eventForm('edit', editEvent, (v) => setEditEvent({ ...editEvent, ...v }), 'Enregistrer', () => updateEventMutation.mutate({ id: editingEvent.id, data: editEvent }), updateEventMutation.isPending, () => { setShowEdit(false); setEditingEvent(null); }, typeEntries, statusEntries)}

      {/* Attendance modal */}
      {showAttendance && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50" onClick={() => setShowAttendance(null)}>
          <div className="card p-6 w-full max-w-lg mx-4 animate-slide-up" onClick={(e) => e.stopPropagation()}>
            <div className="flex items-center justify-between mb-4">
              <div>
                <h3 className="text-lg font-semibold text-gray-900 dark:text-gray-100">{showAttendance.titre}</h3>
                <p className="text-sm text-gray-500 dark:text-gray-400">
                  {showAttendance.nbInscrits} inscrit{showAttendance.nbInscrits !== 1 ? 's' : ''}
                  {showAttendance.limitePlaces ? ` / ${showAttendance.limitePlaces} places` : ''}
                </p>
              </div>
              <button onClick={() => setShowAttendance(null)} className="p-1 rounded-lg hover:bg-gray-100 dark:hover:bg-gray-800">
                <X className="w-5 h-5 text-gray-500" />
              </button>
            </div>
            {/* Stats bar */}
            {attendanceData && attendanceData.length > 0 && (
              <div className="flex gap-3 mb-4 text-xs text-gray-500">
                <span>Total: <strong>{attendanceData.length}</strong></span>
                <span>Présents: <strong className="text-green-600">{attendanceData.filter(r => r.statutInscription === 'PRESENT').length}</strong></span>
                <span>Absents: <strong className="text-red-500">{attendanceData.filter(r => r.statutInscription === 'ABSENT').length}</strong></span>
              </div>
            )}
            {attendanceData && attendanceData.length > 0 ? (
              <div className="space-y-2">
                {attendanceData.map((reg, idx) => {
                  const isAnnulee = reg.statutInscription === 'ANNULEE';
                  const isPresent = reg.statutInscription === 'PRESENT';
                  const isAbsent = reg.statutInscription === 'ABSENT';
                  const isEnAttente = reg.statutInscription === 'EN_ATTENTE';
                  return (
                    <div key={reg.utilisateurId} className="flex items-center justify-between p-3 rounded-lg bg-gray-50 dark:bg-gray-800/50">
                      <div className="flex items-center gap-3">
                        <div className={`w-8 h-8 rounded-full flex items-center justify-center text-xs font-bold ${
                          isPresent ? 'bg-green-100 text-green-700' :
                          isAbsent ? 'bg-red-100 text-red-700' :
                          isAnnulee ? 'bg-gray-100 text-gray-500' :
                          'bg-amber-100 text-amber-700'
                        }`}>
                          {reg.utilisateurId.slice(0, 2).toUpperCase()}
                        </div>
                        <div>
                          <p className="text-sm font-medium text-gray-900 dark:text-gray-100">
                            {getUserName(reg.utilisateurId)}
                          </p>
                          <p className={`text-xs font-medium ${
                            isPresent ? 'text-green-600' :
                            isAbsent ? 'text-red-500' :
                            isAnnulee ? 'text-gray-400' :
                            'text-amber-600'
                          }`}>
                            {isPresent ? '✓ Présent' :
                             isAbsent ? '✗ Absent' :
                             isAnnulee ? 'Annulé' :
                             isEnAttente ? 'En attente' :
                             'Inscrit'}
                          </p>
                        </div>
                      </div>
                      {!isAnnulee && showAttendance && (
                        <div className="flex gap-1">
                          <button
                            onClick={() => markAttendanceMutation.mutate({
                              eventId: showAttendance.id,
                              userId: reg.utilisateurId,
                              present: true,
                            })}
                            disabled={markAttendanceMutation.isPending}
                            className={`p-2 rounded-lg transition-colors ${
                              isPresent
                                ? 'bg-green-100 text-green-700 dark:bg-green-900/30 dark:text-green-300'
                                : 'hover:bg-green-50 text-gray-400 hover:text-green-600 dark:hover:bg-green-900/20'
                            }`}
                            title="Marquer présent"
                          >
                            <UserCheck className="w-4 h-4" />
                          </button>
                          <button
                            onClick={() => markAttendanceMutation.mutate({
                              eventId: showAttendance.id,
                              userId: reg.utilisateurId,
                              present: false,
                            })}
                            disabled={markAttendanceMutation.isPending}
                            className={`p-2 rounded-lg transition-colors ${
                              isAbsent
                                ? 'bg-red-100 text-red-700 dark:bg-red-900/30 dark:text-red-300'
                                : 'hover:bg-red-50 text-gray-400 hover:text-red-600 dark:hover:bg-red-900/20'
                            }`}
                            title="Marquer absent"
                          >
                            <UserX className="w-4 h-4" />
                          </button>
                        </div>
                      )}
                    </div>
                  );
                })}
              </div>
            ) : (
              <div className="text-center py-8">
                <Users className="w-8 h-8 text-gray-300 dark:text-gray-600 mx-auto mb-2" />
                <p className="text-sm text-gray-500 dark:text-gray-400">Aucune inscription pour le moment</p>
              </div>
            )}
            <div className="flex justify-end mt-4">
              <button onClick={() => setShowAttendance(null)} className="btn-secondary btn-sm">Fermer</button>
            </div>
          </div>
        </div>
      )}

      {/* Delete confirmation modal */}
      {showDeleteConfirm && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50" onClick={() => setShowDeleteConfirm(null)}>
          <div className="card p-6 w-full max-w-sm mx-4 animate-slide-up" onClick={(e) => e.stopPropagation()}>
            <div className="flex items-center gap-3 mb-4">
              <div className="w-10 h-10 rounded-full bg-red-100 dark:bg-red-900/30 flex items-center justify-center">
                <Trash2 className="w-5 h-5 text-red-600" />
              </div>
              <div>
                <h3 className="text-lg font-semibold text-gray-900 dark:text-gray-100">Confirmer la suppression</h3>
                <p className="text-sm text-gray-500 dark:text-gray-400">Cette action est irréversible.</p>
              </div>
            </div>
            <div className="flex justify-end gap-2">
              <button onClick={() => setShowDeleteConfirm(null)} className="btn-secondary btn-sm">Annuler</button>
              <button
                onClick={() => deleteEventMutation.mutate(showDeleteConfirm)}
                disabled={deleteEventMutation.isPending}
                className="btn-primary btn-sm bg-red-600 hover:bg-red-700"
              >
                {deleteEventMutation.isPending ? <Loader2 className="w-4 h-4 animate-spin" /> : <Trash2 className="w-4 h-4" />}
                Supprimer
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Consolidated view (Pasteur / Admin) */}
      {view === 'consolidated' && isPasteurOrAdmin ? (
        <div className="space-y-6 animate-fade-in">
          {/* Stats cards */}
          {consolidatedStats && (
            <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
              <div className="glass-card p-5 animate-slide-up">
                <div className="flex items-center justify-between mb-2">
                  <span className="stat-label">Total à venir</span>
                  <div className="p-2.5 rounded-xl bg-gradient-to-br from-blue-500 to-indigo-500 text-white shadow-lg">
                    <Calendar className="w-4 h-4" />
                  </div>
                </div>
                <span className="stat-value">{consolidatedStats.total}</span>
                <p className="text-[10px] text-gray-400 mt-1">dans les 14 prochains jours</p>
              </div>
              {Object.entries(consolidatedStats.parType).slice(0, 3).map(([type, count], i) => (
                <div key={type} className="glass-card p-5 animate-slide-up" style={{ animationDelay: `${i * 50}ms` }}>
                  <div className="flex items-center justify-between mb-2">
                    <span className="stat-label">{typeLabel(type)}</span>
                    <div className="p-2.5 rounded-xl bg-gradient-to-br from-amber-500 to-orange-500 text-white shadow-lg">
                      <Sparkles className="w-4 h-4" />
                    </div>
                  </div>
                  <span className="stat-value text-2xl">{count}</span>
                </div>
              ))}
            </div>
          )}

          {/* Upcoming events list */}
          <div className="glass-card p-5 animate-slide-up">
            <h3 className="text-base font-semibold text-gray-900 dark:text-gray-100 mb-4 flex items-center gap-2">
              <BellRing className="w-4 h-4 text-primary-500" />
              Événements à venir (14 jours)
            </h3>
            {consolidated && consolidated.length > 0 ? (
              <div className="space-y-2">
                {consolidated.map((evt, i) => {
                  const bgColor = evt.typeEvenement === 'CULTE' || evt.typeEvenement === 'ETUDE_BIBLIQUE'
                    ? 'bg-green-50/50 dark:bg-green-900/10 border-l-green-500'
                    : evt.typeEvenement === 'VEILLEE' || evt.typeEvenement === 'PRIERE'
                    ? 'bg-purple-50/50 dark:bg-purple-900/10 border-l-purple-500'
                    : 'bg-white/30 dark:bg-gray-800/30 border-l-gray-300 dark:border-l-gray-600';
                  return (
                    <div
                      key={evt.id}
                      className={`flex items-center justify-between p-3.5 rounded-xl ${bgColor} border-l-[3px] animate-fade-in`}
                      style={{ animationDelay: `${i * 30}ms` }}
                    >
                      <div className="flex items-center gap-3">
                        <div className="flex flex-col items-center min-w-[48px]">
                          <span className="text-lg font-bold text-gray-900 dark:text-gray-100 leading-none">
                            {new Date(evt.dateDebut).getDate()}
                          </span>
                          <span className="text-[10px] text-gray-400 uppercase">
                            {new Date(evt.dateDebut).toLocaleDateString(locale, { month: 'short' })}
                          </span>
                        </div>
                        <div>
                          <p className="text-sm font-semibold text-gray-900 dark:text-gray-100">{evt.titre}</p>
                          <div className="flex items-center gap-2 text-[11px] text-gray-500 mt-0.5">
                            <Clock className="w-3 h-3" />
                            {new Date(evt.dateDebut).toLocaleTimeString(locale, { hour: '2-digit', minute: '2-digit' })}
                            {evt.lieu && <><MapPin className="w-3 h-3 ml-1" />{evt.lieu}</>}
                          </div>
                          {evt.organisateurNom && (
                            <p className="text-[10px] text-gray-400 mt-0.5">
                              Organisé par {evt.organisateurNom}
                              {evt.organisateurRole && ` (${evt.organisateurRole})`}
                            </p>
                          )}
                        </div>
                      </div>
                      <div className="flex items-center gap-2">
                        <span
                          className="inline-flex items-center px-2 py-0.5 rounded-full text-[10px] font-medium bg-gray-100 text-gray-800 dark:bg-gray-700 dark:text-gray-300"
                          style={dictionaries.color('EVENT_TYPE', evt.typeEvenement)
                            ? { backgroundColor: `${dictionaries.color('EVENT_TYPE', evt.typeEvenement)}22`, color: dictionaries.color('EVENT_TYPE', evt.typeEvenement) }
                            : undefined}
                        >
                          {typeLabel(evt.typeEvenement)}
                        </span>
                        {evt.nbInscrits > 0 && (
                          <span className="text-xs text-gray-400 flex items-center gap-1">
                            <Users className="w-3 h-3" />{evt.nbInscrits}
                          </span>
                        )}
                      </div>
                    </div>
                  );
                })}
              </div>
            ) : (
              <div className="text-center py-10">
                <Calendar className="w-12 h-12 text-gray-300 dark:text-gray-600 mx-auto mb-3" />
                <p className="text-sm text-gray-500 dark:text-gray-400">Aucun événement à venir dans les 14 prochains jours</p>
              </div>
            )}
          </div>
        </div>
      ) : (
        <>
          {/* Search and filters */}
          <div className="card p-4 mb-6">
            <div className="flex flex-col sm:flex-row gap-3">
              <div className="flex-1 relative">
                <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400" />
                <input type="text" placeholder="Rechercher un événement..." value={search} onChange={(e) => { setSearch(e.target.value); setPage(0); }} className="input pl-10" />
              </div>
            </div>
            {showFilters && (
              <div className="flex flex-wrap gap-3 mt-3 pt-3 border-t border-gray-200 dark:border-gray-700">
                <select value={typeFilter} onChange={(e) => { setTypeFilter(e.target.value as TypeEvenement | ''); setPage(0); }} className="input w-auto">
                  <option value="">Tous les types</option>
                  {typeEntries.map((o) => (
                    <option key={o.code} value={o.code}>{o.label}</option>
                  ))}
                </select>
                <select value={statutFilter} onChange={(e) => { setStatutFilter(e.target.value as StatutEvenement | ''); setPage(0); }} className="input w-auto">
                  <option value="">Tous les statuts</option>
                  {statusEntries.map((o) => (
                    <option key={o.code} value={o.code}>{o.label}</option>
                  ))}
                </select>
              </div>
            )}
          </div>

          {/* Calendar View */}
          {displayMode === 'calendar' ? (
            <div className="card p-4 mb-6">
              <div className="flex items-center justify-between mb-4">
                <h3 className="text-sm font-semibold text-gray-900 dark:text-gray-100">
                  {new Date().toLocaleDateString(locale, { month: 'long', year: 'numeric' })}
                </h3>
                <div className="flex gap-2 text-[10px]">
                  {Object.entries(TYPE_LABELS).slice(0, 6).map(([k, v]) => (
                    <span key={k} className="px-1.5 py-0.5 rounded bg-gray-100 dark:bg-gray-700 text-gray-500">{v}</span>
                  ))}
                </div>
              </div>
              <div className="grid grid-cols-7 gap-px bg-gray-200 dark:bg-gray-700 rounded-lg overflow-hidden">
                {['Dim', 'Lun', 'Mar', 'Mer', 'Jeu', 'Ven', 'Sam'].map((d) => (
                  <div key={d} className="bg-gray-50 dark:bg-gray-800 p-2 text-center text-[10px] font-semibold text-gray-400 uppercase">{d}</div>
                ))}
                {calendarWeeks.flat().map((day, i) => (
                  <div key={i} className={`bg-white dark:bg-gray-900 p-1.5 min-h-[60px] ${!day.isCurrentMonth ? 'opacity-40' : ''} ${day.isToday ? 'bg-primary-50 dark:bg-primary-900/20 ring-1 ring-primary-400' : ''}`}>
                    <p className={`text-[10px] font-medium mb-1 ${day.isToday ? 'text-primary-600 dark:text-primary-400' : 'text-gray-500'}`}>{day.date.getDate()}</p>
                    <div className="space-y-0.5">
                      {day.events.slice(0, 2).map((ev) => (
                        <div key={ev.id} className="text-[8px] leading-tight px-1 py-0.5 rounded bg-primary-100 dark:bg-primary-900/30 text-primary-700 dark:text-primary-300 truncate cursor-pointer hover:bg-primary-200 dark:hover:bg-primary-800/40" title={ev.titre}>
                          {ev.titre.slice(0, 12)}
                        </div>
                      ))}
                      {day.events.length > 2 && (
                        <p className="text-[8px] text-gray-400 pl-1">+{day.events.length - 2}</p>
                      )}
                    </div>
                  </div>
                ))}
              </div>
              <p className="text-[10px] text-gray-400 mt-2 text-center">Calendrier du mois en cours — cliquez sur un événement pour voir les détails</p>
            </div>
          ) : (
            <>
              <DataTable<Evenement>
                columns={columns}
                data={data?.content || []}
                isLoading={isLoading}
                emptyMessage="Aucun événement trouvé"
                emptyIcon={<Calendar className="w-12 h-12 text-gray-300 dark:text-gray-600 mx-auto mb-4" />}
              />

              {data && data.totalPages > 1 && (
                <div className="flex items-center justify-between mt-4">
                  <p className="text-sm text-gray-500">Page {data.number + 1} / {data.totalPages}</p>
                  <div className="flex gap-2">
                    <button onClick={() => setPage(p => Math.max(0, p - 1))} disabled={data.first} className="btn-secondary btn-sm">Précédent</button>
                    <button onClick={() => setPage(p => p + 1)} disabled={data.last} className="btn-secondary btn-sm">Suivant</button>
                  </div>
                </div>
              )}
            </>
          )}
        </>
      )}
    </div>
  );
}
