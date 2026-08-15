import { useEffect, useMemo, useState } from 'react';
import { createPortal } from 'react-dom';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useParams, Link, useNavigate } from 'react-router-dom';
import api, { getErrorMessage } from '@/lib/api';
import { usePlatformConfig } from '@/contexts/PlatformContext';
import toast from 'react-hot-toast';
import {
  Building2, ArrowLeft, Network, Briefcase, Users2, ListTodo, History,
  Plus, Pencil, Archive, Trash2, UserPlus, Loader2, CheckCircle2,
  Clock, AlertTriangle, CalendarDays, ChevronRight, ChevronDown, Save,
  X,  Star, Flag, FolderTree, Activity, ListChecks, Boxes, Search, Settings, BookOpen,
  UserCheck, UserX, Download,
} from 'lucide-react';

type Team = {
  id: string; nom: string; parentId?: string | null; type: string;
  chefNom?: string; adjointNom?: string; objectif?: string; description?: string;
  dateDebut?: string; dateFin?: string; statut: string; nbMembres: number;
};
type Position = { id: string; nom: string; description?: string; competencesRequises?: string; statut: string; nbMembres: number };
type Assignment = { id: string; memberId: string; memberNom?: string; teamId?: string; teamNom?: string; positionId?: string; positionNom?: string; role: string; dateDebut?: string; dateFin?: string; actif: boolean };
type Task = { id: string; titre: string; description?: string; teamId?: string; teamNom?: string; assignedTo?: string; assigneeNom?: string; priorite: string; statut: string; echeance?: string; avancement: number; enRetard: boolean };
type ActivityItem = { id: string; action: string; details?: string; actorNom?: string; createdAt: string };

const TABS = [
  { key: 'org', label: 'Organisation', icon: Network },
  { key: 'positions', label: 'Postes', icon: Briefcase },
  { key: 'assignments', label: 'Affectations', icon: Users2 },
  { key: 'tasks', label: 'Tâches', icon: ListTodo },
  { key: 'checklists', label: 'Checklists', icon: ListChecks },
  { key: 'inventory', label: 'Inventaire', icon: Boxes },
  { key: 'events', label: 'Événements', icon: CalendarDays },
  { key: 'docs', label: 'Documentation', icon: BookOpen },
  { key: 'settings', label: 'Paramètres', icon: Settings },
  { key: 'activity', label: 'Activité', icon: History },
] as const;

const TYPE_LABELS: Record<string, string> = {
  SOUS_DEPARTEMENT: 'Sous-département',
  EQUIPE_PERMANENTE: 'Équipe permanente',
  EQUIPE_TEMPORAIRE: 'Équipe temporaire',
};
const TYPE_COLORS: Record<string, string> = {
  SOUS_DEPARTEMENT: 'badge-info',
  EQUIPE_PERMANENTE: 'badge-success',
  EQUIPE_TEMPORAIRE: 'badge-warning',
};
const PRIORITE_COLORS: Record<string, string> = {
  BASSE: 'text-gray-400',
  MOYENNE: 'text-amber-500',
  HAUTE: 'text-red-500',
};
const STATUT_TASK_BADGE: Record<string, string> = {
  A_FAIRE: 'badge-gray', EN_COURS: 'badge-info', BLOQUEE: 'badge-danger',
  TERMINEE: 'badge-success', VALIDEE: 'badge-success', ANNULEE: 'badge-inactive',
};
const ROLE_LABELS: Record<string, string> = { CHEF: 'Chef', ADJOINT: 'Adjoint', MEMBRE: 'Membre' };

export default function DepartmentManagementPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const [tab, setTab] = useState<(typeof TABS)[number]['key']>('org');
  const [search, setSearch] = useState('');
  const { moduleEnabled } = usePlatformConfig();

  // Onglets modulables : masqués quand l'administrateur désactive le sous-module
  const visibleTabs = TABS.filter((t) => {
    if (t.key === 'checklists') return moduleEnabled('DEPT_CHECKLISTS');
    if (t.key === 'inventory') return moduleEnabled('DEPT_INVENTORY');
    if (t.key === 'docs') return moduleEnabled('DEPT_DOCUMENTS');
    if (t.key === 'events') return moduleEnabled('EVENTS');
    return true;
  });
  useEffect(() => {
    if (!visibleTabs.some((t) => t.key === tab)) {
      setTab('org');
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [moduleEnabled, tab]);

  const { data: searchResults } = useQuery({
    queryKey: ['department', id, 'search', search],
    queryFn: async () => (await api.get(`/departments/${id}/search`, { params: { q: search } })).data as any,
    enabled: !!id && search.trim().length >= 2,
  });

  const { data: dept } = useQuery({
    queryKey: ['department', id],
    queryFn: async () => (await api.get(`/departments/${id}/detail`)).data as any,
    enabled: !!id,
  });

  const { data: membersPage } = useQuery({
    queryKey: ['department', id, 'members'],
    queryFn: async () => (await api.get(`/departments/${id}/members?size=200`)).data as any,
    enabled: !!id,
  });
  const members: any[] = membersPage?.content || [];

  const { data: overview, isLoading } = useQuery({
    queryKey: ['department', id, 'management'],
    queryFn: async () => (await api.get(`/departments/${id}/management`)).data as any,
    enabled: !!id,
  });

  const invalidate = () => queryClient.invalidateQueries({ queryKey: ['department', id, 'management'] });

  const teams: Team[] = overview?.teams ?? [];
  const positions: Position[] = overview?.positions ?? [];
  const assignments: Assignment[] = overview?.assignments ?? [];
  const taskStats = overview?.taskStats ?? {};
  const activity: ActivityItem[] = overview?.activity ?? [];
  const org = overview?.org ?? {};

  const rootTeams = useMemo(() => teams.filter((t) => !t.parentId), [teams]);

  if (isLoading) {
    return (
      <div className="page-container flex items-center justify-center min-h-[60vh]">
        <Loader2 className="w-8 h-8 animate-spin text-primary-500" />
      </div>
    );
  }

  return (
    <div className="page-container">
      <div className="page-header">
        <button onClick={() => navigate(`/departments/${id}`)} className="btn-ghost btn-sm mb-2">
          <ArrowLeft className="w-4 h-4" /> Retour au département
        </button>
        <div className="flex items-center gap-3">
          <div className="p-3 rounded-xl bg-gradient-to-br from-amber-500 to-orange-500 text-white shadow-lg">
            <Building2 className="w-6 h-6" />
          </div>
          <div className="flex-1">
            <h1 className="page-title">Gestion de {dept?.nom || 'département'}</h1>
            <p className="page-subtitle">Organisation · postes · affectations · tâches · événements · activité</p>
          </div>
        </div>
        <div className="relative mt-3 w-full sm:w-80 sm:mt-0">
          <input
            className="input pl-9"
            placeholder="Recherche rapide : membre, équipe, poste, tâche…"
            value={search}
            onChange={(e) => setSearch(e.target.value)}
          />
          <Search className="w-4 h-4 absolute left-3 top-1/2 -translate-y-1/2 text-gray-400" />
        </div>
      </div>

      {search.trim().length >= 2 && (
        <GlobalSearchResults results={searchResults} deptId={id || ''} onClear={() => setSearch('')} />
      )}

      {/* Org mini-KPIs */}
      <div className="grid grid-cols-2 sm:grid-cols-4 gap-3 mb-5">
        <div className="stat-card p-3 text-center">
          <span className="stat-label text-[10px]">Équipes actives</span>
          <p className="stat-value text-xl">{org.equipesActives ?? 0}</p>
        </div>
        <div className="stat-card p-3 text-center">
          <span className="stat-label text-[10px]">Postes actifs</span>
          <p className="stat-value text-xl">{org.postesActifs ?? 0}</p>
        </div>
        <div className="stat-card p-3 text-center">
          <span className="stat-label text-[10px]">Membres affectés</span>
          <p className="stat-value text-xl">{org.membresAffectes ?? 0}</p>
        </div>
        <div className="stat-card p-3 text-center">
          <span className="stat-label text-[10px]">Tâches en retard</span>
          <p className="stat-value text-xl text-red-500">{taskStats.enRetard ?? 0}</p>
        </div>
      </div>

      {/* Tabs */}
      <div className="flex gap-2 mb-6 overflow-x-auto pb-1">
        {visibleTabs.map((t) => {
          const Icon = t.icon;
          return (
            <button
              key={t.key}
              onClick={() => setTab(t.key)}
              className={`px-4 py-2 rounded-xl text-sm font-medium flex items-center gap-2 transition-all cursor-pointer whitespace-nowrap ${
                tab === t.key
                  ? 'bg-gradient-to-r from-amber-500 to-orange-500 text-white shadow-glow'
                  : 'bg-white/70 dark:bg-gray-800/70 text-gray-600 dark:text-gray-300 hover:bg-gray-100 dark:hover:bg-gray-700'
              }`}
            >
              <Icon className="w-4 h-4" /> {t.label}
            </button>
          );
        })}
      </div>

      {tab === 'org' && (
        <OrganisationTab deptId={id || ''} teams={teams} rootTeams={rootTeams} members={members} onChanged={invalidate} />
      )}
      {tab === 'positions' && <PositionsTab deptId={id || ''} positions={positions} onChanged={invalidate} />}
      {tab === 'assignments' && (
        <AssignmentsTab deptId={id || ''} assignments={assignments} teams={teams} positions={positions} members={members} onChanged={invalidate} />
      )}
      {tab === 'tasks' && (
        <TasksTab deptId={id || ''} taskStats={taskStats} teams={teams} members={members} onChanged={invalidate} />
      )}
      {tab === 'checklists' && <ChecklistsTab deptId={id || ''} onChanged={invalidate} />}
      {tab === 'inventory' && <InventoryTab deptId={id || ''} onChanged={invalidate} />}
      {tab === 'events' && <EventsTab deptId={id || ''} onChanged={invalidate} />}
      {tab === 'docs' && <DocumentsTab deptId={id || ''} onChanged={invalidate} />}
      {tab === 'settings' && <SettingsTab deptId={id || ''} />}
      {tab === 'activity' && <ActivityTab activity={activity} />}
    </div>
  );
}

// ============================================================
// ORGANISATION — arbre des équipes / sous-départements
// ============================================================

function TeamNode({ team, teams, depth = 0, deptId }: { team: Team; teams: Team[]; depth?: number; deptId: string }) {
  const children = teams.filter((t) => t.parentId === team.id);
  const [open, setOpen] = useState(depth < 1);

  return (
    <div className="animate-slide-up" style={{ animationDelay: `${Math.min(depth * 40, 200)}ms` }}>
      <div
        className={`flex items-center gap-2 p-2.5 rounded-xl mb-1 transition-all hover:bg-gray-50 dark:hover:bg-gray-800/40 ${
          depth === 0 ? 'bg-white/50 dark:bg-gray-800/40 border border-gray-200/60 dark:border-gray-700/40' : ''
        }`}
        style={{ marginLeft: depth * 22 }}
      >
        {children.length > 0 ? (
          <button onClick={() => setOpen(!open)} className="p-0.5 text-gray-400 cursor-pointer">
            {open ? <ChevronDown className="w-4 h-4" /> : <ChevronRight className="w-4 h-4" />}
          </button>
        ) : (
          <span className="w-4" />
        )}
        <div className={`p-1.5 rounded-lg ${depth === 0 ? 'bg-gradient-to-br from-amber-500 to-orange-500 text-white' : 'bg-gray-100 dark:bg-gray-700 text-gray-500 dark:text-gray-300'}`}>
          <FolderTree className="w-4 h-4" />
        </div>
        <div className="min-w-0 flex-1">
          <div className="flex items-center gap-2 flex-wrap">
            <span className="text-sm font-semibold text-gray-900 dark:text-gray-100">{team.nom}</span>
            <span className={`badge text-[9px] ${TYPE_COLORS[team.type] || 'badge-gray'}`}>{TYPE_LABELS[team.type] || team.type}</span>
            {team.statut === 'ARCHIVED' && <span className="badge text-[9px] badge-inactive">Archivée</span>}
          </div>
          {(team.objectif || team.description) && (
            <p className="text-[10px] text-gray-400 truncate max-w-md">{team.objectif || team.description}</p>
          )}
          <div className="flex items-center gap-2 text-[10px] text-gray-400 mt-0.5">
            <span className="flex items-center gap-0.5"><Users2 className="w-3 h-3" /> {team.nbMembres}</span>
            {team.chefNom && <span>· Chef : {team.chefNom}</span>}
            {team.adjointNom && <span>· Adjoint : {team.adjointNom}</span>}
            {team.dateFin && team.type === 'EQUIPE_TEMPORAIRE' && (
              <span className="flex items-center gap-0.5 text-amber-500"><CalendarDays className="w-3 h-3" /> fin {team.dateFin}</span>
            )}
          </div>
        </div>
        <div className="flex items-center gap-1 flex-shrink-0">
          <TeamActions team={team} teams={teams} deptId={deptId} onChanged={() => {}} />
        </div>
      </div>
      {open && children.map((c) => <TeamNode key={c.id} team={c} teams={teams} depth={depth + 1} deptId={deptId} />)}
    </div>
  );
}

function TeamActions({ team, teams, deptId, onChanged }: { team: Team; teams: Team[]; deptId: string; onChanged: () => void }) {
  const [editing, setEditing] = useState(false);
  const queryClient = useQueryClient();
  const invalidate = () => queryClient.invalidateQueries({ queryKey: ['department'] });

  const archiveMutation = useMutation({
    mutationFn: async () => {
      await api.delete(`/departments/${deptId}/teams/${team.id}`);
    },
    onSuccess: () => { toast.success('Équipe archivée'); invalidate(); },
    onError: (err) => toast.error(getErrorMessage(err)),
  });

  if (editing) {
    return (
      <EditTeamForm
        team={team}
        teams={teams}
        deptId={deptId}
        onDone={() => { setEditing(false); invalidate(); }}
      />
    );
  }

  return (
    <>
      <button onClick={() => setEditing(true)} title="Modifier" className="p-1.5 rounded-lg text-gray-400 hover:text-primary-600 hover:bg-primary-500/10 transition-all cursor-pointer">
        <Pencil className="w-3.5 h-3.5" />
      </button>
      {team.statut !== 'ARCHIVED' && (
        <button onClick={() => archiveMutation.mutate()} title="Archiver" className="p-1.5 rounded-lg text-gray-400 hover:text-amber-600 hover:bg-amber-500/10 transition-all cursor-pointer">
          <Archive className="w-3.5 h-3.5" />
        </button>
      )}
    </>
  );
}

function EditTeamForm({ team, teams, deptId, onDone }: { team: Team; teams: Team[]; deptId: string; onDone: () => void }) {
  const queryClient = useQueryClient();
  const [nom, setNom] = useState(team.nom);
  const [parentId, setParentId] = useState(team.parentId || '');
  const [type, setType] = useState(team.type);
  const [objectif, setObjectif] = useState(team.objectif || '');
  const [dateFin, setDateFin] = useState(team.dateFin || '');

  const updateMutation = useMutation({
    mutationFn: async () => {
      await api.put(`/departments/${deptId}/teams/${team.id}`, {
        nom, parentId: parentId || null, type, objectif: objectif || null, dateFin: dateFin || null,
      });
    },
    onSuccess: () => { toast.success('Équipe modifiée ✅'); queryClient.invalidateQueries({ queryKey: ['department'] }); onDone(); },
    onError: (err) => toast.error(getErrorMessage(err)),
  });

  return (
    <div className="flex items-center gap-2">
      <input className="input py-1 px-2 text-xs w-28" value={nom} onChange={(e) => setNom(e.target.value)} placeholder="Nom" />
      <select className="input py-1 px-2 text-xs w-32" value={type} onChange={(e) => setType(e.target.value)}>
        {Object.entries(TYPE_LABELS).map(([k, v]) => <option key={k} value={k}>{v}</option>)}
      </select>
      <button onClick={() => updateMutation.mutate()} disabled={updateMutation.isPending} className="btn-primary btn-xs cursor-pointer">
        <Save className="w-3 h-3" /> Enregistrer
      </button>
      <button onClick={onDone} className="btn-ghost btn-xs cursor-pointer"><X className="w-3 h-3" /></button>
    </div>
  );
}

function OrganisationTab({ teams, rootTeams, members, deptId, onChanged }: { teams: Team[]; rootTeams: Team[]; members: any[]; deptId: string; onChanged: () => void }) {
  const [showCreate, setShowCreate] = useState(false);
  return (
    <div className="glass-card p-5">
      <div className="flex items-center justify-between mb-4 flex-wrap gap-2">
        <div className="flex items-center gap-2">
          <Network className="w-4 h-4 text-primary-500" />
          <h3 className="text-sm font-semibold text-gray-900 dark:text-gray-100">Organigramme du département</h3>
        </div>
        <button onClick={() => setShowCreate(!showCreate)} className="btn-primary btn-sm cursor-pointer">
          <Plus className="w-4 h-4" /> {showCreate ? 'Fermer' : 'Nouvelle équipe'}
        </button>
      </div>
      {showCreate && (
        <CreateTeamForm deptId={deptId} teams={teams} members={members} onDone={() => { setShowCreate(false); onChanged(); }} />
      )}
      <div className="mt-2 space-y-1">
        {rootTeams.length === 0 && (
          <div className="text-center py-10">
            <Network className="w-10 h-10 text-gray-300 mx-auto mb-2" />
            <p className="text-sm text-gray-400">Aucune équipe — créez votre premier sous-département ou équipe</p>
          </div>
        )}
        {rootTeams.map((t) => <TeamNode key={t.id} team={t} teams={teams} deptId={deptId} />)}
      </div>
    </div>
  );
}

function CreateTeamForm({ teams, members, deptId, onDone }: { teams: Team[]; members: any[]; deptId: string; onDone: () => void }) {
  const queryClient = useQueryClient();
  const [nom, setNom] = useState('');
  const [type, setType] = useState('EQUIPE_PERMANENTE');
  const [parentId, setParentId] = useState('');
  const [objectif, setObjectif] = useState('');
  const [dateDebut, setDateDebut] = useState('');
  const [dateFin, setDateFin] = useState('');
  const [eventId, setEventId] = useState('');

  const { data: deptEvents = [] } = useQuery({
    queryKey: ['department', deptId, 'events'],
    queryFn: async () => (await api.get(`/events/department/${deptId}?size=100`)).data?.content ?? [],
    enabled: !!deptId && type === 'EQUIPE_TEMPORAIRE',
  });

  const createMutation = useMutation({
    mutationFn: async () => {
      await api.post(`/departments/${deptId}/teams`, {
        nom, type, parentId: parentId || null, objectif: objectif || null,
        dateDebut: dateDebut || null, dateFin: dateFin || null, eventId: eventId || null,
      });
    },
    onSuccess: () => { toast.success('Équipe créée ✅'); queryClient.invalidateQueries({ queryKey: ['department'] }); onDone(); },
    onError: (err) => toast.error(getErrorMessage(err)),
  });

  const selectEvent = (id: string) => {
    setEventId(id);
    const ev = deptEvents.find((e: any) => e.id === id);
    if (ev?.dateDebut && !dateDebut) setDateDebut(ev.dateDebut.slice(0, 10));
    if (ev?.dateFin && !dateFin) setDateFin(ev.dateFin.slice(0, 10));
  };

  return (
    <div className="p-4 rounded-2xl bg-gray-50 dark:bg-gray-800/40 border border-gray-200/60 dark:border-gray-700/40 mb-4">
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-3">
        <div>
          <label className="label" htmlFor="team-nom">Nom de l'équipe *</label>
          <input id="team-nom" className="input" value={nom} onChange={(e) => setNom(e.target.value)} placeholder="Ex : Son, Vidéo, Convention 2026…" />
        </div>
        <div>
          <label className="label" htmlFor="team-type">Type</label>
          <select id="team-type" className="input" value={type} onChange={(e) => setType(e.target.value)}>
            {Object.entries(TYPE_LABELS).map(([k, v]) => <option key={k} value={k}>{v}</option>)}
          </select>
        </div>
        <div>
          <label className="label" htmlFor="team-parent">Équipe parente</label>
          <select id="team-parent" className="input" value={parentId} onChange={(e) => setParentId(e.target.value)}>
            <option value="">— Aucune (racine) —</option>
            {teams.map((t) => <option key={t.id} value={t.id}>{t.nom}</option>)}
          </select>
        </div>
        {type === 'EQUIPE_TEMPORAIRE' && (
          <>
            <div>
              <label className="label" htmlFor="team-event">Événement lié (optionnel)</label>
              <select id="team-event" className="input" value={eventId} onChange={(e) => selectEvent(e.target.value)}>
                <option value="">— Aucun événement —</option>
                {deptEvents.map((ev: any) => (
                  <option key={ev.id} value={ev.id}>{ev.titre}</option>
                ))}
              </select>
            </div>
            <div className="grid grid-cols-2 gap-2">
              <div>
                <label className="label">Début</label>
                <input type="date" className="input" value={dateDebut} onChange={(e) => setDateDebut(e.target.value)} />
              </div>
              <div>
                <label className="label">Fin</label>
                <input type="date" className="input" value={dateFin} onChange={(e) => setDateFin(e.target.value)} />
              </div>
            </div>
          </>
        )}
        <div className="sm:col-span-2 lg:col-span-4">
          <label className="label">Objectif</label>
          <input className="input" value={objectif} onChange={(e) => setObjectif(e.target.value)} placeholder="Objectif de l'équipe…" />
        </div>
      </div>
      <button
        onClick={() => createMutation.mutate()}
        disabled={!nom.trim() || createMutation.isPending}
        className="btn-primary btn-sm mt-3 cursor-pointer"
      >
        {createMutation.isPending ? <Loader2 className="w-4 h-4 animate-spin" /> : <Plus className="w-4 h-4" />} Créer l'équipe
      </button>
    </div>
  );
}

// ============================================================
// POSTES
// ============================================================

function PositionsTab({ positions, deptId, onChanged }: { positions: Position[]; deptId: string; onChanged: () => void }) {
  const queryClient = useQueryClient();
  const [nom, setNom] = useState('');
  const [description, setDescription] = useState('');
  const [competences, setCompetences] = useState('');

  const createMutation = useMutation({
    mutationFn: async () => {
      await api.post(`/departments/${deptId}/positions`, { nom, description: description || null, competencesRequises: competences || null });
    },
    onSuccess: () => { toast.success('Poste créé ✅'); setNom(''); setDescription(''); setCompetences(''); queryClient.invalidateQueries({ queryKey: ['department'] }); onChanged(); },
    onError: (err) => toast.error(getErrorMessage(err)),
  });
  const archiveMutation = useMutation({
    mutationFn: async (positionId: string) => api.delete(`/departments/${deptId}/positions/${positionId}`),
    onSuccess: () => { toast.success('Poste archivé'); queryClient.invalidateQueries({ queryKey: ['department'] }); onChanged(); },
    onError: (err) => toast.error(getErrorMessage(err)),
  });

  return (
    <div className="glass-card p-5">
      <div className="flex items-center gap-2 mb-4">
        <Briefcase className="w-4 h-4 text-primary-500" />
        <h3 className="text-sm font-semibold text-gray-900 dark:text-gray-100">Postes du département</h3>
      </div>
      <div className="p-4 rounded-2xl bg-gray-50 dark:bg-gray-800/40 border border-gray-200/60 dark:border-gray-700/40 mb-4">
        <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
          <div>
            <label className="label">Nom du poste *</label>
            <input className="input" value={nom} onChange={(e) => setNom(e.target.value)} placeholder="Ex : Technicien son, Vidéaste…" />
          </div>
          <div>
            <label className="label">Description</label>
            <input className="input" value={description} onChange={(e) => setDescription(e.target.value)} placeholder="Rôle, responsabilités…" />
          </div>
          <div>
            <label className="label">Compétences requises</label>
            <input className="input" value={competences} onChange={(e) => setCompetences(e.target.value)} placeholder="Ex : mixage, éclairage…" />
          </div>
        </div>
        <button onClick={() => createMutation.mutate()} disabled={!nom.trim() || createMutation.isPending} className="btn-primary btn-sm mt-3 cursor-pointer">
          <Plus className="w-4 h-4" /> Créer le poste
        </button>
      </div>
      {positions.length === 0 ? (
        <div className="text-center py-8">
          <Briefcase className="w-10 h-10 text-gray-300 mx-auto mb-2" />
          <p className="text-sm text-gray-400">Aucun poste défini</p>
        </div>
      ) : (
        <div className="space-y-2">
          {positions.map((p) => (
            <div key={p.id} className="flex items-center gap-3 p-3 rounded-xl bg-gray-50 dark:bg-gray-800/40">
              <div className="p-2 rounded-lg bg-primary-500/10 text-primary-600 dark:text-primary-300">
                <Briefcase className="w-4 h-4" />
              </div>
              <div className="min-w-0 flex-1">
                <div className="flex items-center gap-2 flex-wrap">
                  <span className="text-sm font-semibold text-gray-900 dark:text-gray-100">{p.nom}</span>
                  <span className="badge text-[9px] badge-gray">{p.nbMembres} membre{p.nbMembres > 1 ? 's' : ''}</span>
                </div>
                {(p.description || p.competencesRequises) && (
                  <p className="text-[10px] text-gray-400 truncate max-w-xl">{p.description}{p.competencesRequises ? ` · ${p.competencesRequises}` : ''}</p>
                )}
              </div>
              {p.statut !== 'ARCHIVED' && (
                <button onClick={() => archiveMutation.mutate(p.id)} title="Archiver" className="p-1.5 rounded-lg text-gray-400 hover:text-amber-600 hover:bg-amber-500/10 transition-all cursor-pointer">
                  <Archive className="w-3.5 h-3.5" />
                </button>
              )}
            </div>
          ))}
        </div>
      )}
    </div>
  );
}

// ============================================================
// AFFECTATIONS
// ============================================================

function AssignmentsTab({ assignments, teams, positions, members, deptId, onChanged }: {
  assignments: Assignment[]; teams: Team[]; positions: Position[]; members: any[]; deptId: string; onChanged: () => void;
}) {
  const queryClient = useQueryClient();
  const [memberId, setMemberId] = useState('');
  const [teamId, setTeamId] = useState('');
  const [positionId, setPositionId] = useState('');
  const [role, setRole] = useState('MEMBRE');
  const [dateDebut, setDateDebut] = useState('');

  const createMutation = useMutation({
    mutationFn: async () => {
      await api.post(`/departments/${deptId}/assignments`, {
        memberId, teamId: teamId || null, positionId: positionId || null, role, dateDebut: dateDebut || null,
      });
    },
    onSuccess: () => { toast.success('Membre affecté ✅'); setMemberId(''); setTeamId(''); setPositionId(''); setRole('MEMBRE'); setDateDebut(''); queryClient.invalidateQueries({ queryKey: ['department'] }); onChanged(); },
    onError: (err) => toast.error(getErrorMessage(err)),
  });
  const endMutation = useMutation({
    mutationFn: async (assignmentId: string) => api.delete(`/departments/${deptId}/assignments/${assignmentId}`),
    onSuccess: () => { toast.success('Affectation terminée'); queryClient.invalidateQueries({ queryKey: ['department'] }); onChanged(); },
    onError: (err) => toast.error(getErrorMessage(err)),
  });

  const activeAssignments = assignments.filter((a) => a.actif);
  const inactiveAssignments = assignments.filter((a) => !a.actif);

  return (
    <div className="glass-card p-5">
      <div className="flex items-center gap-2 mb-4">
        <Users2 className="w-4 h-4 text-primary-500" />
        <h3 className="text-sm font-semibold text-gray-900 dark:text-gray-100">Affectations membres → équipes / postes</h3>
        <span className="badge text-[10px] badge-info">{activeAssignments.length} actives</span>
      </div>

      <div className="p-4 rounded-2xl bg-gray-50 dark:bg-gray-800/40 border border-gray-200/60 dark:border-gray-700/40 mb-4">
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-5 gap-3">
          <div>
            <label className="label">Membre *</label>
            <select className="input" value={memberId} onChange={(e) => setMemberId(e.target.value)}>
              <option value="">— Choisir —</option>
              {members.map((m) => <option key={m.id} value={m.id}>{m.nom}</option>)}
            </select>
          </div>
          <div>
            <label className="label">Équipe *</label>
            <select className="input" value={teamId} onChange={(e) => setTeamId(e.target.value)}>
              <option value="">— Choisir —</option>
              {teams.filter((t) => t.statut === 'ACTIVE').map((t) => <option key={t.id} value={t.id}>{t.nom}</option>)}
            </select>
          </div>
          <div>
            <label className="label">Poste</label>
            <select className="input" value={positionId} onChange={(e) => setPositionId(e.target.value)}>
              <option value="">— Aucun —</option>
              {positions.filter((p) => p.statut === 'ACTIVE').map((p) => <option key={p.id} value={p.id}>{p.nom}</option>)}
            </select>
          </div>
          <div>
            <label className="label">Rôle</label>
            <select className="input" value={role} onChange={(e) => setRole(e.target.value)}>
              {Object.entries(ROLE_LABELS).map(([k, v]) => <option key={k} value={k}>{v}</option>)}
            </select>
          </div>
          <div>
            <label className="label">Début</label>
            <input type="date" className="input" value={dateDebut} onChange={(e) => setDateDebut(e.target.value)} />
          </div>
        </div>
        <button onClick={() => createMutation.mutate()} disabled={!memberId || !teamId || createMutation.isPending} className="btn-primary btn-sm mt-3 cursor-pointer">
          <UserPlus className="w-4 h-4" /> Affecter
        </button>
      </div>

      {activeAssignments.length === 0 ? (
        <div className="text-center py-8">
          <Users2 className="w-10 h-10 text-gray-300 mx-auto mb-2" />
          <p className="text-sm text-gray-400">Aucune affectation active</p>
        </div>
      ) : (
        <div className="overflow-x-auto">
          <table className="data-table">
            <thead>
              <tr>
                <th>Membre</th><th>Équipe</th><th>Poste</th><th>Rôle</th><th>Début</th><th>Fin</th><th></th>
              </tr>
            </thead>
            <tbody>
              {activeAssignments.map((a) => (
                <tr key={a.id}>
                  <td className="font-medium text-gray-900 dark:text-gray-100">{a.memberNom || a.memberId.slice(0, 8)}</td>
                  <td>{a.teamNom ? <span className="badge text-[10px] badge-info">{a.teamNom}</span> : '—'}</td>
                  <td className="text-sm">{a.positionNom || '—'}</td>
                  <td><span className={`badge text-[10px] ${a.role === 'CHEF' ? 'badge-warning' : a.role === 'ADJOINT' ? 'badge-info' : 'badge-gray'}`}>{ROLE_LABELS[a.role] || a.role}</span></td>
                  <td className="text-sm">{a.dateDebut || '—'}</td>
                  <td className="text-sm">{a.dateFin || '—'}</td>
                  <td>
                    <button onClick={() => endMutation.mutate(a.id)} title="Mettre fin" className="p-1.5 rounded-lg text-gray-400 hover:text-red-600 hover:bg-red-500/10 transition-all cursor-pointer">
                      <X className="w-3.5 h-3.5" />
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {inactiveAssignments.length > 0 && (
        <div className="mt-4">
          <p className="text-xs font-medium text-gray-400 mb-2 uppercase tracking-wider">Historique ({inactiveAssignments.length})</p>
          <div className="space-y-1">
            {inactiveAssignments.map((a) => (
              <div key={a.id} className="flex items-center gap-2 text-xs text-gray-400 px-2 py-1">
                <X className="w-3 h-3" />
                {a.memberNom} → {a.teamNom || a.positionNom || '—'} · terminé le {a.dateFin || '—'}
              </div>
            ))}
          </div>
        </div>
      )}
    </div>
  );
}

// ============================================================
// TÂCHES
// ============================================================

function TasksTab({ taskStats, teams, members, deptId, onChanged }: {
  taskStats: any; teams: Team[]; members: any[]; deptId: string; onChanged: () => void;
}) {
  const queryClient = useQueryClient();
  const { data: tasks = [] } = useQuery({
    queryKey: ['department', deptId, 'tasks'],
    queryFn: async () => (await api.get(`/departments/${deptId}/tasks`)).data as Task[],
    enabled: !!deptId,
  });
  const [titre, setTitre] = useState('');
  const [teamId, setTeamId] = useState('');
  const [assignedTo, setAssignedTo] = useState('');
  const [priorite, setPriorite] = useState('MOYENNE');
  const [echeance, setEcheance] = useState('');
  const [showCreate, setShowCreate] = useState(false);

  const createMutation = useMutation({
    mutationFn: async () => {
      await api.post(`/departments/${deptId}/tasks`, {
        titre, teamId: teamId || null, assignedTo: assignedTo || null, priorite, echeance: echeance || null,
      });
    },
    onSuccess: () => { toast.success('Tâche créée ✅'); setTitre(''); setTeamId(''); setAssignedTo(''); setPriorite('MOYENNE'); setEcheance(''); setShowCreate(false); invalidateAll(); },
    onError: (err) => toast.error(getErrorMessage(err)),
  });

  const updateMutation = useMutation({
    mutationFn: async ({ taskId, data }: { taskId: string; data: any }) =>
      (await api.put(`/departments/${deptId}/tasks/${taskId}`, data)).data,
    onSuccess: () => invalidateAll(),
    onError: (err) => toast.error(getErrorMessage(err)),
  });
  const deleteMutation = useMutation({
    mutationFn: async (taskId: string) => api.delete(`/departments/${deptId}/tasks/${taskId}`),
    onSuccess: () => { toast.success('Tâche supprimée'); invalidateAll(); },
    onError: (err) => toast.error(getErrorMessage(err)),
  });

  function invalidateAll() {
    queryClient.invalidateQueries({ queryKey: ['department', deptId, 'tasks'] });
    queryClient.invalidateQueries({ queryKey: ['department'] });
    onChanged();
  }

  const setStatus = (task: Task, statut: string) =>
    updateMutation.mutate({ taskId: task.id, data: { titre: task.titre, statut, priorite: task.priorite, avancement: task.avancement } });
  const setAvancement = (task: Task, avancement: number) =>
    updateMutation.mutate({ taskId: task.id, data: { titre: task.titre, statut: task.statut, priorite: task.priorite, avancement } });

  const kpis = [
    { label: 'En cours', value: taskStats.enCours ?? 0, color: 'text-blue-500', icon: Clock },
    { label: 'À faire', value: taskStats.aFaire ?? 0, color: 'text-gray-500', icon: ListTodo },
    { label: 'En retard', value: taskStats.enRetard ?? 0, color: 'text-red-500', icon: AlertTriangle },
    { label: 'Terminées', value: (taskStats.terminees ?? 0) + (taskStats.validees ?? 0), color: 'text-emerald-500', icon: CheckCircle2 },
  ];

  return (
    <div className="space-y-5">
      <div className="grid grid-cols-2 sm:grid-cols-4 gap-3">
        {kpis.map((k) => {
          const Icon = k.icon;
          return (
            <div key={k.label} className="stat-card p-3 text-center">
              <Icon className={`w-4 h-4 mx-auto mb-1 ${k.color}`} />
              <p className={`stat-value text-xl ${k.color}`}>{k.value}</p>
              <span className="stat-label text-[10px]">{k.label}</span>
            </div>
          );
        })}
      </div>

      <div className="glass-card p-5">
        <div className="flex items-center justify-between mb-4 flex-wrap gap-2">
          <div className="flex items-center gap-2">
            <ListTodo className="w-4 h-4 text-primary-500" />
            <h3 className="text-sm font-semibold text-gray-900 dark:text-gray-100">Tâches ({tasks.length})</h3>
          </div>
          <button onClick={() => setShowCreate(!showCreate)} className="btn-primary btn-sm cursor-pointer">
            <Plus className="w-4 h-4" /> {showCreate ? 'Fermer' : 'Nouvelle tâche'}
          </button>
        </div>

        {showCreate && (
          <div className="p-4 rounded-2xl bg-gray-50 dark:bg-gray-800/40 border border-gray-200/60 dark:border-gray-700/40 mb-4">
            <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-3">
              <div>
                <label className="label">Titre *</label>
                <input className="input" value={titre} onChange={(e) => setTitre(e.target.value)} placeholder="Ex : Préparer la console son…" />
              </div>
              <div>
                <label className="label">Assignée à</label>
                <select className="input" value={assignedTo} onChange={(e) => setAssignedTo(e.target.value)}>
                  <option value="">— Non assignée —</option>
                  {members.map((m) => <option key={m.id} value={m.id}>{m.nom}</option>)}
                </select>
              </div>
              <div>
                <label className="label">Équipe</label>
                <select className="input" value={teamId} onChange={(e) => setTeamId(e.target.value)}>
                  <option value="">— Aucune —</option>
                  {teams.filter((t) => t.statut === 'ACTIVE').map((t) => <option key={t.id} value={t.id}>{t.nom}</option>)}
                </select>
              </div>
              <div className="grid grid-cols-2 gap-2">
                <div>
                  <label className="label">Priorité</label>
                  <select className="input" value={priorite} onChange={(e) => setPriorite(e.target.value)}>
                    <option value="BASSE">Basse</option>
                    <option value="MOYENNE">Moyenne</option>
                    <option value="HAUTE">Haute</option>
                  </select>
                </div>
                <div>
                  <label className="label">Échéance</label>
                  <input type="date" className="input" value={echeance} onChange={(e) => setEcheance(e.target.value)} />
                </div>
              </div>
            </div>
            <button onClick={() => createMutation.mutate()} disabled={!titre.trim() || createMutation.isPending} className="btn-primary btn-sm mt-3 cursor-pointer">
              <Plus className="w-4 h-4" /> Créer la tâche
            </button>
          </div>
        )}

        {tasks.length === 0 ? (
          <div className="text-center py-8">
            <ListTodo className="w-10 h-10 text-gray-300 mx-auto mb-2" />
            <p className="text-sm text-gray-400">Aucune tâche</p>
          </div>
        ) : (
          <div className="space-y-2">
            {tasks.map((t) => (
              <div key={t.id} className={`p-3 rounded-xl bg-gray-50 dark:bg-gray-800/40 border ${t.enRetard ? 'border-red-300/50 dark:border-red-500/30' : 'border-transparent'}`}>
                <div className="flex items-center gap-3 flex-wrap">
                  <Flag className={`w-4 h-4 shrink-0 ${PRIORITE_COLORS[t.priorite] || 'text-gray-400'}`} />
                  <div className="min-w-0 flex-1">
                    <div className="flex items-center gap-2 flex-wrap">
                      <span className={`text-sm font-semibold ${t.statut === 'ANNULEE' ? 'line-through text-gray-400' : 'text-gray-900 dark:text-gray-100'}`}>{t.titre}</span>
                      {t.enRetard && <span className="badge text-[9px] badge-danger flex items-center gap-1"><AlertTriangle className="w-2.5 h-2.5" /> En retard</span>}
                    </div>
                    <div className="flex items-center gap-2 text-[10px] text-gray-400 mt-0.5 flex-wrap">
                      {t.assigneeNom && <span>👤 {t.assigneeNom}</span>}
                      {t.teamNom && <span>· {t.teamNom}</span>}
                      {t.echeance && <span className="flex items-center gap-0.5">· <CalendarDays className="w-3 h-3" /> {t.echeance}</span>}
                    </div>
                    <div className="flex items-center gap-2 mt-2">
                      <input
                        type="range" min={0} max={100} step={5}
                        value={t.avancement}
                        onChange={(e) => setAvancement(t, Number(e.target.value))}
                        className="w-40 accent-emerald-500"
                      />
                      <span className="text-[10px] font-bold text-emerald-600">{t.avancement}%</span>
                    </div>
                  </div>
                  <select
                    value={t.statut}
                    onChange={(e) => setStatus(t, e.target.value)}
                    className="input w-32 py-1 text-xs"
                  >
                    {Object.entries(STATUT_TASK_BADGE).map(([k]) => (
                      <option key={k} value={k}>{k.replace('_', ' ').toLowerCase()}</option>
                    ))}
                  </select>
                  <button onClick={() => deleteMutation.mutate(t.id)} title="Supprimer" className="p-1.5 rounded-lg text-gray-400 hover:text-red-600 hover:bg-red-500/10 transition-all cursor-pointer">
                    <Trash2 className="w-3.5 h-3.5" />
                  </button>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
}

// ============================================================
// ACTIVITÉ
// ============================================================

const ACTION_LABELS: Record<string, string> = {
  TEAM_CREATED: 'Équipe créée',
  TEAM_UPDATED: 'Équipe modifiée',
  TEAM_ARCHIVED: 'Équipe archivée',
  POSITION_CREATED: 'Poste créé',
  POSITION_UPDATED: 'Poste modifié',
  POSITION_ARCHIVED: 'Poste archivé',
  MEMBER_ASSIGNED: 'Membre affecté',
  ASSIGNMENT_ENDED: 'Fin d\'affectation',
  TASK_CREATED: 'Tâche créée',
  TASK_UPDATED: 'Tâche mise à jour',
  TASK_DELETED: 'Tâche supprimée',
};

function ActivityTab({ activity }: { activity: ActivityItem[] }) {
  return (
    <div className="glass-card p-5">
      <div className="flex items-center gap-2 mb-4">
        <History className="w-4 h-4 text-primary-500" />
        <h3 className="text-sm font-semibold text-gray-900 dark:text-gray-100">Journal d'activité</h3>
        <span className="badge text-[10px] badge-gray">{activity.length}</span>
      </div>
      {activity.length === 0 ? (
        <div className="text-center py-8">
          <Activity className="w-10 h-10 text-gray-300 mx-auto mb-2" />
          <p className="text-sm text-gray-400">Aucune activité enregistrée pour l'instant</p>
        </div>
      ) : (
        <div className="space-y-0">
          {activity.map((a, i) => (
            <div key={a.id} className="relative flex gap-3 pb-4">
              {i < activity.length - 1 && <span className="absolute left-[9px] top-6 bottom-0 w-px bg-gray-200 dark:bg-gray-700" />}
              <div className="w-[18px] h-[18px] rounded-full bg-gradient-to-br from-amber-500 to-orange-500 shrink-0 mt-0.5 flex items-center justify-center">
                <span className="w-2 h-2 rounded-full bg-white" />
              </div>
              <div className="min-w-0 flex-1">
                <p className="text-sm text-gray-800 dark:text-gray-200">
                  <span className="font-semibold">{ACTION_LABELS[a.action] || a.action.replace(/_/g, ' ')}</span>
                  {a.details && <span className="text-gray-500 dark:text-gray-400"> — {a.details}</span>}
                </p>
                <p className="text-[10px] text-gray-400">
                  {a.actorNom ? `par ${a.actorNom} · ` : ''}{new Date(a.createdAt).toLocaleString('fr-FR')}
                </p>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}

// ============================================================
// CHECKLISTS — listes de contrôle (préparation événement, tâche…)
// ============================================================

type Checklist = {
  id: string; titre: string; cibleType: string; cibleId?: string | null;
  statut: string; progression: number; createdAt?: string;
  items: { id: string; libelle: string; fait: boolean }[];
};

export function ChecklistsTab({ deptId, onChanged }: { deptId: string; onChanged: () => void }) {
  const queryClient = useQueryClient();
  const [titre, setTitre] = useState('');
  const [cibleType, setCibleType] = useState('GENERAL');
  const [items, setItems] = useState<string[]>(['', '']);

  const { data: checklists = [], isLoading } = useQuery({
    queryKey: ['department', deptId, 'checklists'],
    queryFn: async () => (await api.get(`/departments/${deptId}/checklists`)).data as Checklist[],
    enabled: !!deptId,
  });

  const invalidate = () => {
    queryClient.invalidateQueries({ queryKey: ['department', deptId, 'checklists'] });
    onChanged();
  };

  const createMutation = useMutation({
    mutationFn: async () => {
      await api.post(`/departments/${deptId}/checklists`, {
        titre, cibleType,
        items: items.map((i) => i.trim()).filter(Boolean),
      });
    },
    onSuccess: () => {
      toast.success('Checklist créée ✅');
      setTitre(''); setItems(['', '']);
      invalidate();
    },
    onError: (err) => toast.error(getErrorMessage(err)),
  });

  const addItemMutation = useMutation({
    mutationFn: async ({ checklistId, libelle }: { checklistId: string; libelle: string }) =>
      api.post(`/departments/${deptId}/checklists/${checklistId}/items`, { libelle }),
    onSuccess: invalidate,
    onError: (err) => toast.error(getErrorMessage(err)),
  });

  const toggleItemMutation = useMutation({
    mutationFn: async ({ checklistId, itemId, fait }: { checklistId: string; itemId: string; fait: boolean }) =>
      api.put(`/departments/${deptId}/checklists/${checklistId}/items/${itemId}`, { fait }),
    onSuccess: invalidate,
    onError: (err) => toast.error(getErrorMessage(err)),
  });

  const closeMutation = useMutation({
    mutationFn: async (checklistId: string) =>
      api.put(`/departments/${deptId}/checklists/${checklistId}`, { statut: 'TERMINEE' }),
    onSuccess: () => { toast.success('Checklist terminée'); invalidate(); },
    onError: (err) => toast.error(getErrorMessage(err)),
  });

  const deleteMutation = useMutation({
    mutationFn: async (checklistId: string) =>
      api.delete(`/departments/${deptId}/checklists/${checklistId}`),
    onSuccess: () => { toast.success('Checklist supprimée'); invalidate(); },
    onError: (err) => toast.error(getErrorMessage(err)),
  });

  const deleteItemMutation = useMutation({
    mutationFn: async ({ checklistId, itemId }: { checklistId: string; itemId: string }) =>
      api.delete(`/departments/${deptId}/checklists/${checklistId}/items/${itemId}`),
    onSuccess: invalidate,
    onError: (err) => toast.error(getErrorMessage(err)),
  });

  if (isLoading) {
    return <div className="flex justify-center py-10"><Loader2 className="w-6 h-6 animate-spin text-primary-500" /></div>;
  }

  return (
    <div className="space-y-4">
      <div className="glass-card p-5">
        <div className="flex items-center gap-2 mb-4">
          <ListChecks className="w-4 h-4 text-primary-500" />
          <h3 className="text-sm font-semibold text-gray-900 dark:text-gray-100">Nouvelle checklist</h3>
        </div>
        <div className="grid grid-cols-1 sm:grid-cols-2 gap-3 mb-3">
          <div>
            <label className="label">Titre *</label>
            <input className="input" value={titre} onChange={(e) => setTitre(e.target.value)}
              placeholder="Ex : Préparation du culte de dimanche" />
          </div>
          <div>
            <label className="label">Cible</label>
            <select className="input" value={cibleType} onChange={(e) => setCibleType(e.target.value)}>
              <option value="GENERAL">Général</option>
              <option value="TACHE">Tâche</option>
              <option value="EVENEMENT">Événement</option>
              <option value="EQUIPE">Équipe</option>
              <option value="MEMBRE">Membre</option>
            </select>
          </div>
        </div>
        <div className="space-y-2 mb-3">
          {items.map((item, idx) => (
            <div key={idx} className="flex items-center gap-2">
              <input className="input flex-1" value={item}
                onChange={(e) => setItems(items.map((v, i) => (i === idx ? e.target.value : v)))}
                placeholder={`Élément ${idx + 1} — ex : Sono testée`} />
              {items.length > 1 && (
                <button onClick={() => setItems(items.filter((_, i) => i !== idx))}
                  className="p-2 rounded-lg text-gray-400 hover:text-red-500 cursor-pointer">
                  <X className="w-4 h-4" />
                </button>
              )}
            </div>
          ))}
          <button onClick={() => setItems([...items, ''])} className="text-xs text-primary-600 dark:text-primary-300 flex items-center gap-1 cursor-pointer">
            <Plus className="w-3.5 h-3.5" /> Ajouter un élément
          </button>
        </div>
        <button onClick={() => createMutation.mutate()} disabled={!titre.trim() || createMutation.isPending}
          className="btn-primary btn-sm cursor-pointer">
          <Plus className="w-4 h-4" /> Créer la checklist
        </button>
      </div>

      {checklists.length === 0 ? (
        <div className="text-center py-8">
          <ListChecks className="w-10 h-10 text-gray-300 mx-auto mb-2" />
          <p className="text-sm text-gray-400">Aucune checklist pour le moment</p>
        </div>
      ) : (
        checklists.map((c) => (
          <div key={c.id} className="glass-card p-4">
            <div className="flex items-center gap-2 mb-1 flex-wrap">
              <span className={`text-sm font-semibold ${c.statut === 'TERMINEE' ? 'text-gray-400 line-through' : 'text-gray-900 dark:text-gray-100'}`}>{c.titre}</span>
              <span className={`badge text-[9px] ${c.statut === 'TERMINEE' ? 'badge-success' : 'badge-info'}`}>
                {c.statut === 'TERMINEE' ? 'Terminée' : c.cibleType === 'GENERAL' ? 'Générale' : c.cibleType.toLowerCase()}
              </span>
            </div>
            <div className="flex items-center gap-2 mb-2">
              <div className="flex-1 h-1.5 rounded-full bg-gray-200 dark:bg-gray-700 overflow-hidden">
                <div className="h-full rounded-full bg-gradient-to-r from-amber-500 to-orange-500 transition-all"
                  style={{ width: `${c.progression}%` }} />
              </div>
              <span className="text-[10px] text-gray-500">{c.progression}%</span>
            </div>
            <div className="space-y-1.5">
              {c.items.map((item) => (
                <div key={item.id} className="flex items-center gap-2 group">
                  <input type="checkbox" checked={item.fait}
                    onChange={() => toggleItemMutation.mutate({ checklistId: c.id, itemId: item.id, fait: !item.fait })}
                    className="w-4 h-4 accent-amber-500 cursor-pointer" />
                  <span className={`flex-1 text-sm ${item.fait ? 'line-through text-gray-400' : 'text-gray-700 dark:text-gray-200'}`}>{item.libelle}</span>
                  <button onClick={() => deleteItemMutation.mutate({ checklistId: c.id, itemId: item.id })}
                    className="p-1 rounded text-gray-300 hover:text-red-500 opacity-0 group-hover:opacity-100 transition-all cursor-pointer">
                    <Trash2 className="w-3.5 h-3.5" />
                  </button>
                </div>
              ))}
            </div>
            <div className="flex items-center gap-2 mt-3">
              <form onSubmit={(e) => {
                e.preventDefault();
                const input = e.currentTarget.querySelector('input') as HTMLInputElement;
                if (input.value.trim()) addItemMutation.mutate({ checklistId: c.id, libelle: input.value.trim() });
                input.value = '';
              }} className="flex-1 flex gap-2">
                <input className="input py-1.5 text-xs flex-1" placeholder="Ajouter un élément…" />
              </form>
              {c.statut !== 'TERMINEE' && (
                <button onClick={() => closeMutation.mutate(c.id)} className="btn-ghost btn-sm text-xs cursor-pointer">
                  <CheckCircle2 className="w-3.5 h-3.5" /> Terminer
                </button>
              )}
              <button onClick={() => deleteMutation.mutate(c.id)} className="p-1.5 rounded-lg text-gray-400 hover:text-red-500 hover:bg-red-500/10 transition-all cursor-pointer">
                <Trash2 className="w-4 h-4" />
              </button>
            </div>
          </div>
        ))
      )}
    </div>
  );
}

// ============================================================
// INVENTAIRE — matériel du département
// ============================================================

type Equipment = {
  id: string; nom: string; description?: string; quantite: number;
  etat: string; responsableId?: string | null; affecteAId?: string | null;
  localisation?: string; dateAcquisition?: string;
};

const ETAT_LABELS: Record<string, string> = {
  NEUF: 'Neuf', BON: 'Bon état', USAGE: 'Usage', REPARATION: 'En réparation', HORS_SERVICE: 'Hors service',
};
const ETAT_COLORS: Record<string, string> = {
  NEUF: 'badge-success', BON: 'badge-info', USAGE: 'badge-gray', REPARATION: 'badge-warning', HORS_SERVICE: 'badge-danger',
};

export function InventoryTab({ deptId, onChanged }: { deptId: string; onChanged: () => void }) {
  const queryClient = useQueryClient();
  const [nom, setNom] = useState('');
  const [description, setDescription] = useState('');
  const [quantite, setQuantite] = useState(1);
  const [etat, setEtat] = useState('BON');
  const [localisation, setLocalisation] = useState('');
  const [editing, setEditing] = useState<Equipment | null>(null);

  const { data: equipment = [], isLoading } = useQuery({
    queryKey: ['department', deptId, 'equipment'],
    queryFn: async () => (await api.get(`/departments/${deptId}/equipment`)).data as Equipment[],
    enabled: !!deptId,
  });

  const invalidate = () => {
    queryClient.invalidateQueries({ queryKey: ['department', deptId, 'equipment'] });
    onChanged();
  };

  const saveMutation = useMutation({
    mutationFn: async () => {
      const payload = { nom, description: description || null, quantite, etat, localisation: localisation || null };
      if (editing) {
        await api.put(`/departments/${deptId}/equipment/${editing.id}`, payload);
      } else {
        await api.post(`/departments/${deptId}/equipment`, payload);
      }
    },
    onSuccess: () => {
      toast.success(editing ? 'Équipement modifié ✅' : 'Équipement ajouté ✅');
      setNom(''); setDescription(''); setQuantite(1); setEtat('BON'); setLocalisation(''); setEditing(null);
      invalidate();
    },
    onError: (err) => toast.error(getErrorMessage(err)),
  });

  const deleteMutation = useMutation({
    mutationFn: async (equipmentId: string) => api.delete(`/departments/${deptId}/equipment/${equipmentId}`),
    onSuccess: () => { toast.success('Équipement supprimé'); invalidate(); },
    onError: (err) => toast.error(getErrorMessage(err)),
  });

  if (isLoading) {
    return <div className="flex justify-center py-10"><Loader2 className="w-6 h-6 animate-spin text-primary-500" /></div>;
  }

  const totalItems = equipment.reduce((sum, e) => sum + e.quantite, 0);

  return (
    <div className="space-y-4">
      <div className="grid grid-cols-2 sm:grid-cols-4 gap-3">
        <div className="stat-card p-3 text-center">
          <span className="stat-label text-[10px]">Équipements</span>
          <p className="stat-value text-xl">{equipment.length}</p>
        </div>
        <div className="stat-card p-3 text-center">
          <span className="stat-label text-[10px]">Articles au total</span>
          <p className="stat-value text-xl">{totalItems}</p>
        </div>
        <div className="stat-card p-3 text-center">
          <span className="stat-label text-[10px]">En réparation</span>
          <p className="stat-value text-xl text-amber-500">{equipment.filter((e) => e.etat === 'REPARATION').length}</p>
        </div>
        <div className="stat-card p-3 text-center">
          <span className="stat-label text-[10px]">Hors service</span>
          <p className="stat-value text-xl text-red-500">{equipment.filter((e) => e.etat === 'HORS_SERVICE').length}</p>
        </div>
      </div>

      <div className="glass-card p-5">
        <div className="flex items-center gap-2 mb-4">
          <Boxes className="w-4 h-4 text-primary-500" />
          <h3 className="text-sm font-semibold text-gray-900 dark:text-gray-100">
            {editing ? `Modifier « ${editing.nom} »` : 'Nouvel équipement'}
          </h3>
        </div>
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-3">
          <div>
            <label className="label">Nom *</label>
            <input className="input" value={nom} onChange={(e) => setNom(e.target.value)} placeholder="Ex : Caméra Sony A7" />
          </div>
          <div>
            <label className="label">Quantité</label>
            <input type="number" min={1} className="input" value={quantite}
              onChange={(e) => setQuantite(Math.max(1, Number(e.target.value) || 1))} />
          </div>
          <div>
            <label className="label">État</label>
            <select className="input" value={etat} onChange={(e) => setEtat(e.target.value)}>
              {Object.entries(ETAT_LABELS).map(([k, v]) => <option key={k} value={k}>{v}</option>)}
            </select>
          </div>
          <div className="sm:col-span-2 lg:col-span-2">
            <label className="label">Description</label>
            <input className="input" value={description} onChange={(e) => setDescription(e.target.value)}
              placeholder="Marque, modèle, caractéristiques…" />
          </div>
          <div>
            <label className="label">Localisation</label>
            <input className="input" value={localisation} onChange={(e) => setLocalisation(e.target.value)} placeholder="Ex : Salle 3, studio…" />
          </div>
        </div>
        <div className="flex items-center gap-2 mt-4">
          <button onClick={() => saveMutation.mutate()} disabled={!nom.trim() || saveMutation.isPending}
            className="btn-primary btn-sm cursor-pointer">
            <Save className="w-4 h-4" /> {editing ? 'Enregistrer' : 'Ajouter'}
          </button>
          {editing && (
            <button onClick={() => { setEditing(null); setNom(''); setDescription(''); setQuantite(1); setEtat('BON'); setLocalisation(''); }}
              className="btn-ghost btn-sm cursor-pointer">
              <X className="w-4 h-4" /> Annuler
            </button>
          )}
        </div>
      </div>

      {equipment.length === 0 ? (
        <div className="text-center py-8">
          <Boxes className="w-10 h-10 text-gray-300 mx-auto mb-2" />
          <p className="text-sm text-gray-400">Aucun équipement enregistré</p>
        </div>
      ) : (
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-3">
          {equipment.map((e) => (
            <div key={e.id} className="glass-card p-4">
              <div className="flex items-start justify-between gap-2">
                <div className="flex items-center gap-2 min-w-0">
                  <div className="p-2 rounded-lg bg-primary-500/10 text-primary-600 dark:text-primary-300 shrink-0">
                    <Boxes className="w-4 h-4" />
                  </div>
                  <div className="min-w-0">
                    <p className="text-sm font-semibold text-gray-900 dark:text-gray-100 truncate">{e.nom}</p>
                    <span className={`badge text-[9px] ${ETAT_COLORS[e.etat] || 'badge-gray'}`}>{ETAT_LABELS[e.etat] || e.etat}</span>
                  </div>
                </div>
                <div className="flex items-center gap-1">
                  <button onClick={() => {
                    setEditing(e);
                    setNom(e.nom); setDescription(e.description || ''); setQuantite(e.quantite);
                    setEtat(e.etat); setLocalisation(e.localisation || '');
                  }} className="p-1.5 rounded-lg text-gray-400 hover:text-primary-500 hover:bg-primary-500/10 transition-all cursor-pointer" title="Modifier">
                    <Pencil className="w-3.5 h-3.5" />
                  </button>
                  <button onClick={() => deleteMutation.mutate(e.id)}
                    className="p-1.5 rounded-lg text-gray-400 hover:text-red-500 hover:bg-red-500/10 transition-all cursor-pointer" title="Supprimer">
                    <Trash2 className="w-3.5 h-3.5" />
                  </button>
                </div>
              </div>
              <div className="mt-2 flex items-center gap-2 text-[11px] text-gray-500">
                <span>×{e.quantite}</span>
                {e.localisation && <><span>·</span><span className="truncate">{e.localisation}</span></>}
              </div>
              {e.description && <p className="mt-1 text-[11px] text-gray-400 line-clamp-2">{e.description}</p>}
            </div>
          ))}
        </div>
      )}
    </div>
  );
}

// ============================================================
// RECHERCHE GLOBALE DU DÉPARTEMENT
// ============================================================

function GlobalSearchResults({ results, deptId, onClear }: { results: any; deptId: string; onClear: () => void }) {
  const navigate = useNavigate();
  if (!results) {
    return (
      <div className="glass-card p-5 mb-5 flex items-center gap-3">
        <Loader2 className="w-4 h-4 animate-spin text-primary-500" /> Recherche en cours…
      </div>
    );
  }
  const total = results.total ?? 0;
  const sections = [
    { key: 'membres', label: 'Membres', items: results.membres ?? [], render: (m: any) => (
      <button key={m.id} onClick={() => { navigate(`/departments/${deptId}/members/${m.id}`); onClear(); }}
        className="w-full text-left px-3 py-2 hover:bg-gray-100 dark:hover:bg-gray-700/60 rounded-lg flex items-center justify-between gap-2 cursor-pointer">
        <span className="flex items-center gap-2 text-sm">
          <Users2 className="w-4 h-4 text-primary-500" /> {m.nomComplet}
        </span>
        <span className="text-[11px] text-gray-400">{m.statut}</span>
      </button>
    ) },
    { key: 'equipes', label: 'Équipes', items: results.equipes ?? [], render: (t: any) => (
      <div key={t.id} className="px-3 py-2 flex items-center justify-between gap-2">
        <span className="flex items-center gap-2 text-sm"><Network className="w-4 h-4 text-amber-500" /> {t.nom}</span>
        <span className="badge badge-gray text-[10px]">{t.nbMembres} membre{t.nbMembres > 1 ? 's' : ''}</span>
      </div>
    ) },
    { key: 'postes', label: 'Postes', items: results.postes ?? [], render: (p: any) => (
      <div key={p.id} className="px-3 py-2 flex items-center gap-2 text-sm">
        <Briefcase className="w-4 h-4 text-sky-500" /> {p.nom}
      </div>
    ) },
    { key: 'taches', label: 'Tâches', items: results.taches ?? [], render: (t: any) => (
      <div key={t.id} className="px-3 py-2 flex items-center justify-between gap-2">
        <span className="flex items-center gap-2 text-sm"><ListTodo className="w-4 h-4 text-emerald-500" /> {t.titre}</span>
        <span className={`badge text-[10px] ${STATUT_TASK_BADGE[t.statut] || 'badge-gray'}`}>{t.statut}</span>
      </div>
    ) },
    { key: 'evenements', label: 'Événements', items: results.evenements ?? [], render: (e: any) => (
      <div key={e.id} className="px-3 py-2 flex items-center justify-between gap-2">
        <span className="flex items-center gap-2 text-sm"><CalendarDays className="w-4 h-4 text-violet-500" /> {e.titre}</span>
        {e.dateDebut && <span className="text-[11px] text-gray-400">{formatEventDate(e.dateDebut)}</span>}
      </div>
    ) },
  ];
  return (
    <div className="glass-card p-5 mb-5">
      <div className="flex items-center justify-between mb-3">
        <h3 className="text-sm font-semibold text-gray-900 dark:text-gray-100">
          {total > 0 ? `${total} résultat${total > 1 ? 's' : ''}` : 'Aucun résultat'} pour cette recherche
        </h3>
        <button onClick={onClear} className="text-xs text-gray-400 hover:text-gray-600 dark:hover:text-gray-200 cursor-pointer">
          <X className="w-4 h-4" />
        </button>
      </div>
      {total === 0 ? (
        <p className="text-sm text-gray-400">Essayez un nom, un poste, une équipe, une tâche ou un événement.</p>
      ) : (
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-4">
          {sections.filter((s) => s.items.length > 0).map((s) => (
            <div key={s.key}>
              <p className="text-[11px] uppercase tracking-wide text-gray-400 mb-1">{s.label} ({s.items.length})</p>
              <div className="space-y-0.5">{s.items.map(s.render)}</div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}

function formatEventDate(value: string) {
  const d = new Date(value);
  if (Number.isNaN(d.getTime())) return value;
  return d.toLocaleDateString('fr-FR', { day: 'numeric', month: 'short', year: 'numeric' }) +
    ' · ' + d.toLocaleTimeString('fr-FR', { hour: '2-digit', minute: '2-digit' });
}

// ============================================================
// ÉVÉNEMENTS DU DÉPARTEMENT
// ============================================================

type DeptEvent = {
  id: string; titre: string; typeEvenement: string; lieu?: string;
  dateDebut: string; dateFin?: string; statut: string; nbInscrits: number; description?: string;
};

const EVENT_TYPES = ['SORTIE', 'RETRAITE', 'EVANGELISATION', 'REUNION', 'VISITE', 'CONFERENCE', 'FORMATION', 'ANNIVERSAIRE', 'AUTRE'];
const EVENT_TYPE_LABELS: Record<string, string> = {
  SORTIE: 'Sortie', RETRAITE: 'Retraite', EVANGELISATION: 'Évangélisation', REUNION: 'Réunion',
  VISITE: 'Visite', CONFERENCE: 'Conférence', FORMATION: 'Formation', ANNIVERSAIRE: 'Anniversaire', AUTRE: 'Autre',
};
const EVENT_STATUT_BADGE: Record<string, string> = {
  PLANIFIE: 'badge-info', EN_COURS: 'badge-warning', TERMINE: 'badge-success', ANNULE: 'badge-inactive',
};

function EventsTab({ deptId, onChanged }: { deptId: string; onChanged: () => void }) {
  const queryClient = useQueryClient();
  const [titre, setTitre] = useState('');
  const [typeEvenement, setTypeEvenement] = useState('REUNION');
  const [lieu, setLieu] = useState('');
  const [dateDebut, setDateDebut] = useState('');
  const [description, setDescription] = useState('');

  const { data: events = [], isLoading } = useQuery({
    queryKey: ['department', deptId, 'events'],
    queryFn: async () => {
      const res = await api.get(`/events/department/${deptId}?size=100`);
      return (res.data?.content ?? []) as DeptEvent[];
    },
    enabled: !!deptId,
  });

  const [attendanceEvent, setAttendanceEvent] = useState<DeptEvent | null>(null);

  const invalidate = () => {
    queryClient.invalidateQueries({ queryKey: ['department', deptId, 'events'] });
    onChanged();
  };

  const createMutation = useMutation({
    mutationFn: async () => {
      await api.post('/events', {
        typeEvenement, titre, lieu, description,
        dateDebut: new Date(dateDebut).toISOString(),
        departmentId: deptId,
      });
    },
    onSuccess: () => {
      toast.success('Événement créé ✅');
      setTitre(''); setLieu(''); setDateDebut(''); setDescription('');
      invalidate();
    },
    onError: (err) => toast.error(getErrorMessage(err)),
  });

  const upcoming = events
    .filter((e) => e.statut === 'PLANIFIE' || e.statut === 'EN_COURS')
    .sort((a, b) => new Date(a.dateDebut).getTime() - new Date(b.dateDebut).getTime());
  const past = events
    .filter((e) => e.statut === 'TERMINE' || e.statut === 'ANNULE')
    .sort((a, b) => new Date(b.dateDebut).getTime() - new Date(a.dateDebut).getTime());

  if (isLoading) {
    return <div className="flex justify-center py-10"><Loader2 className="w-6 h-6 animate-spin text-primary-500" /></div>;
  }

  return (
    <div className="space-y-4">
      <div className="glass-card p-5">
        <div className="flex items-center gap-2 mb-4">
          <CalendarDays className="w-4 h-4 text-primary-500" />
          <h3 className="text-sm font-semibold text-gray-900 dark:text-gray-100">Nouvel événement du département</h3>
        </div>
        <div className="grid grid-cols-1 sm:grid-cols-2 gap-3 mb-3">
          <div>
            <label className="label">Titre *</label>
            <input className="input" value={titre} onChange={(e) => setTitre(e.target.value)}
              placeholder="Ex : Convention départementale 2026" />
          </div>
          <div>
            <label className="label">Type</label>
            <select className="input" value={typeEvenement} onChange={(e) => setTypeEvenement(e.target.value)}>
              {EVENT_TYPES.map((t) => <option key={t} value={t}>{EVENT_TYPE_LABELS[t]}</option>)}
            </select>
          </div>
          <div>
            <label className="label">Date et heure *</label>
            <input type="datetime-local" className="input" value={dateDebut} onChange={(e) => setDateDebut(e.target.value)} />
          </div>
          <div>
            <label className="label">Lieu</label>
            <input className="input" value={lieu} onChange={(e) => setLieu(e.target.value)} placeholder="Ex : Temple principal" />
          </div>
        </div>
        <textarea className="input mb-3 min-h-[70px]" value={description} onChange={(e) => setDescription(e.target.value)}
          placeholder="Description, participants attendus, équipes mobilisées…" />
        <button onClick={() => createMutation.mutate()} disabled={!titre.trim() || !dateDebut || createMutation.isPending}
          className="btn-primary btn-sm cursor-pointer">
          <Plus className="w-4 h-4" /> Créer l'événement
        </button>
      </div>

      <div className="glass-card p-5">
        <h3 className="text-sm font-semibold text-gray-900 dark:text-gray-100 mb-3">
          À venir ({upcoming.length})
        </h3>
        {upcoming.length === 0 ? (
          <p className="text-sm text-gray-400">Aucun événement planifié. Créez le premier événement de votre département.</p>
        ) : (
          <div className="space-y-2">
            {upcoming.map((e) => (
              <div key={e.id} className="flex items-center justify-between gap-3 p-3 rounded-xl border border-gray-100 dark:border-gray-700/60 hover:bg-gray-50 dark:hover:bg-gray-800/50">
                <div className="flex items-center gap-3 min-w-0">
                  <div className="p-2 rounded-lg bg-primary-500/10 text-primary-600 dark:text-primary-300 shrink-0">
                    <CalendarDays className="w-4 h-4" />
                  </div>
                  <div className="min-w-0">
                    <p className="text-sm font-medium text-gray-900 dark:text-gray-100 truncate">{e.titre}</p>
                    <p className="text-[11px] text-gray-400">
                      {EVENT_TYPE_LABELS[e.typeEvenement] ?? e.typeEvenement} · {formatEventDate(e.dateDebut)}
                      {e.lieu ? ` · ${e.lieu}` : ''}
                    </p>
                  </div>
                </div>
                <div className="flex items-center gap-2 shrink-0">
                  <button
                    onClick={() => setAttendanceEvent(e)}
                    className="btn-ghost btn-xs inline-flex cursor-pointer"
                    title="Pointer la présence des membres à cet événement"
                  >
                    <UserCheck className="w-3.5 h-3.5" /> Présences
                  </button>
                  <span className={`badge text-[10px] ${EVENT_STATUT_BADGE[e.statut] || 'badge-gray'}`}>{e.statut}</span>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>

      {past.length > 0 && (
        <div className="glass-card p-5">
          <h3 className="text-sm font-semibold text-gray-900 dark:text-gray-100 mb-3">Passés ({past.length})</h3>
          <div className="space-y-1.5">
            {past.map((e) => (
              <div key={e.id} className="flex items-center justify-between gap-3 px-3 py-2 text-sm">
                <span className="text-gray-700 dark:text-gray-200 truncate">{e.titre}</span>
                <div className="flex items-center gap-2 shrink-0">
                  <button
                    onClick={() => setAttendanceEvent(e)}
                    className="btn-ghost btn-xs inline-flex cursor-pointer"
                    title="Pointer la présence des membres à cet événement"
                  >
                    <UserCheck className="w-3.5 h-3.5" /> Présences
                  </button>
                  <span className="text-[11px] text-gray-400">{formatEventDate(e.dateDebut)}</span>
                </div>
              </div>
            ))}
          </div>
        </div>
      )}

      {attendanceEvent && (
        <EventAttendanceModal deptId={deptId} event={attendanceEvent} onClose={() => setAttendanceEvent(null)} />
      )}
    </div>
  );
}

// ============================================================
// PRÉSENCE DES MEMBRES À UN ÉVÉNEMENT
// ============================================================

function initials(name: string) {
  const parts = name.trim().split(/\s+/).filter(Boolean);
  if (parts.length === 0) return '?';
  if (parts.length === 1) return parts[0].slice(0, 2).toUpperCase();
  return (parts[0][0] + parts[parts.length - 1][0]).toUpperCase();
}

export function EventAttendanceModal({ deptId, event, onClose }: { deptId: string; event: DeptEvent; onClose: () => void }) {
  const queryClient = useQueryClient();
  const { data, isLoading } = useQuery({
    queryKey: ['department', deptId, 'events', event.id, 'attendance'],
    queryFn: async () => (await api.get(`/departments/${deptId}/events/${event.id}/attendance`)).data as any,
    enabled: !!deptId,
  });

  const markMutation = useMutation({
    mutationFn: async ({ soulId, present }: { soulId: string; present: boolean }) =>
      (await api.put(`/departments/${deptId}/events/${event.id}/attendance`, { soulId, present })).data,
    onSuccess: () => {
      toast.success('Présence enregistrée ✅');
      queryClient.invalidateQueries({ queryKey: ['department', deptId, 'events', event.id, 'attendance'] });
    },
    onError: (err) => toast.error(getErrorMessage(err)),
  });

  const markAllMutation = useMutation({
    mutationFn: async () =>
      (await api.post(`/departments/${deptId}/events/${event.id}/attendance/mark-all?present=true`)).data,
    onSuccess: (r) => {
      toast.success(`${r?.marques ?? 0} membres marqués présents ✅`);
      queryClient.invalidateQueries({ queryKey: ['department', deptId, 'events', event.id, 'attendance'] });
    },
    onError: (err) => toast.error(getErrorMessage(err)),
  });

  const downloadCsv = () => {
    api.get(`/departments/${deptId}/events/${event.id}/attendance/export`, { responseType: 'blob' })
      .then((res) => {
        const url = URL.createObjectURL(res.data as Blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `feuille-presence-${event.titre.toLowerCase().replace(/[^a-z0-9]+/g, '-').replace(/^-|-$/g, '').slice(0, 40) || 'evenement'}.csv`;
        a.click();
        URL.revokeObjectURL(url);
        toast.success('Feuille de présence exportée 📥');
      })
      .catch((err) => toast.error(getErrorMessage(err)));
  };

  const membres: any[] = data?.membres ?? [];

  return createPortal(
    <div className="fixed inset-0 z-[100] flex items-center justify-center bg-black/50 p-4" onClick={onClose}>
      <div className="glass-card p-5 w-full max-w-xl max-h-[85vh] overflow-y-auto" onClick={(e) => e.stopPropagation()}>
        <div className="flex items-center justify-between mb-1">
          <h3 className="text-sm font-semibold text-gray-900 dark:text-gray-100 flex items-center gap-2">
            <UserCheck className="w-4 h-4 text-primary-500" />
            Présences — {event.titre}
          </h3>
          <button onClick={onClose} className="p-1 rounded-lg text-gray-400 hover:text-gray-600 cursor-pointer">
            <X className="w-5 h-5" />
          </button>
        </div>
        <p className="text-[11px] text-gray-400 mb-4">
          Pointez chaque membre présent ou absent. Le responsable du département, le chef de famille
          ou le faiseur de l'âme peuvent marquer la présence.
        </p>

        {isLoading ? (
          <div className="flex justify-center py-10"><Loader2 className="w-6 h-6 animate-spin text-primary-500" /></div>
        ) : (
          <>
            <div className="flex flex-wrap gap-3 mb-4 text-xs text-gray-500">
              <span>Total : <strong className="text-gray-800 dark:text-gray-100">{data?.total ?? 0}</strong></span>
              <span>Présents : <strong className="text-green-600">{data?.presents ?? 0}</strong></span>
              <span>Absents : <strong className="text-red-500">{data?.absents ?? 0}</strong></span>
              <span>Non pointés : <strong className="text-amber-600">{data?.nonMarques ?? 0}</strong></span>
            </div>
            {membres.length === 0 ? (
              <p className="text-sm text-gray-400 text-center py-6">Aucun membre dans ce département</p>
            ) : (
              <div className="space-y-1.5 max-h-[50vh] overflow-y-auto pr-1">
                {membres.map((m: any) => {
                  const isPresent = m.present === true;
                  const isAbsent = m.present === false;
                  return (
                    <div key={m.soulId} className="flex items-center justify-between gap-3 p-2.5 rounded-xl bg-gray-50 dark:bg-gray-800/40">
                      <div className="flex items-center gap-3 min-w-0">
                        <div className={`w-8 h-8 rounded-full flex items-center justify-center text-xs font-bold shrink-0 ${
                          isPresent ? 'bg-emerald-100 text-emerald-700' : isAbsent ? 'bg-red-100 text-red-700' : 'bg-gray-200 text-gray-500'
                        }`}>
                          {initials(m.nom)}
                        </div>
                        <div className="min-w-0">
                          <p className="text-sm font-medium text-gray-900 dark:text-gray-100 truncate">{m.nom}</p>
                          <p className={`text-[10px] font-medium ${isPresent ? 'text-emerald-600' : isAbsent ? 'text-red-500' : 'text-gray-400'}`}>
                            {isPresent ? '✓ Présent' : isAbsent ? '✗ Absent' : 'Non pointé'}
                          </p>
                        </div>
                      </div>
                      <div className="flex gap-1 shrink-0">
                        <button
                          onClick={() => markMutation.mutate({ soulId: m.soulId, present: true })}
                          disabled={markMutation.isPending || isPresent}
                          className={`p-2 rounded-lg transition-colors cursor-pointer ${
                            isPresent
                              ? 'bg-emerald-100 text-emerald-700'
                              : 'text-gray-400 hover:bg-emerald-50 hover:text-emerald-600'
                          }`}
                          title="Marquer présent"
                        >
                          <UserCheck className="w-4 h-4" />
                        </button>
                        <button
                          onClick={() => markMutation.mutate({ soulId: m.soulId, present: false })}
                          disabled={markMutation.isPending || isAbsent}
                          className={`p-2 rounded-lg transition-colors cursor-pointer ${
                            isAbsent
                              ? 'bg-red-100 text-red-700'
                              : 'text-gray-400 hover:bg-red-50 hover:text-red-600'
                          }`}
                          title="Marquer absent"
                        >
                          <UserX className="w-4 h-4" />
                        </button>
                      </div>
                    </div>
                  );
                })}
              </div>
            )}

            <div className="flex flex-wrap items-center justify-between gap-2 mt-4 pt-3 border-t border-gray-100 dark:border-gray-700/60">
              <button
                onClick={() => markAllMutation.mutate()}
                disabled={markAllMutation.isPending || membres.length === 0}
                className="btn-secondary btn-sm cursor-pointer"
                title="Marquer tous les membres présents à cet événement"
              >
                {markAllMutation.isPending ? <Loader2 className="w-4 h-4 animate-spin" /> : <UserCheck className="w-4 h-4" />}
                Marquer tous présents
              </button>
              <button
                onClick={downloadCsv}
                className="btn-ghost btn-sm cursor-pointer"
                title="Exporter la feuille de présence en CSV"
              >
                <Download className="w-4 h-4" /> Exporter CSV
              </button>
            </div>
          </>
        )}
      </div>
    </div>,
    document.body
  );
}

// ============================================================
// DOCUMENTATION — procédures, guides, formulaires, comptes rendus
// ============================================================

const DOC_TYPES = ['PROCEDURE', 'GUIDE', 'DOCUMENT', 'FORMULAIRE', 'COMPTE_RENDU', 'RESSOURCE'] as const;
const DOC_TYPE_LABELS: Record<string, string> = {
  PROCEDURE: 'Procédure', GUIDE: 'Guide', DOCUMENT: 'Document',
  FORMULAIRE: 'Formulaire', COMPTE_RENDU: 'Compte rendu', RESSOURCE: 'Ressource',
};
const DOC_TYPE_BADGES: Record<string, string> = {
  PROCEDURE: 'badge-danger', GUIDE: 'badge-info', DOCUMENT: 'badge-gray',
  FORMULAIRE: 'badge-warning', COMPTE_RENDU: 'badge-success', RESSOURCE: 'badge-violet',
};

type DeptDocument = {
  id: string; titre: string; type: string; description?: string;
  url?: string; statut: string; createdAt?: string;
};

export function DocumentsTab({ deptId, onChanged }: { deptId: string; onChanged: () => void }) {
  const queryClient = useQueryClient();
  const [titre, setTitre] = useState('');
  const [type, setType] = useState('DOCUMENT');
  const [description, setDescription] = useState('');
  const [url, setUrl] = useState('');
  const [showCreate, setShowCreate] = useState(false);

  const { data: documents = [], isLoading } = useQuery({
    queryKey: ['department', deptId, 'documents'],
    queryFn: async () => (await api.get(`/departments/${deptId}/documents`)).data as DeptDocument[],
    enabled: !!deptId,
  });
  const { data: stats = {} } = useQuery({
    queryKey: ['department', deptId, 'documents', 'stats'],
    queryFn: async () => (await api.get(`/departments/${deptId}/documents/stats`)).data as any,
    enabled: !!deptId,
  });

  const invalidate = () => {
    queryClient.invalidateQueries({ queryKey: ['department', deptId, 'documents'] });
    onChanged();
  };

  const createMutation = useMutation({
    mutationFn: async () => {
      await api.post(`/departments/${deptId}/documents`, {
        titre: titre.trim(), type, description: description || null, url: url || null,
      });
    },
    onSuccess: () => {
      toast.success('Document ajouté ✅');
      setTitre(''); setDescription(''); setUrl(''); setType('DOCUMENT'); setShowCreate(false);
      invalidate();
    },
    onError: (err) => toast.error(getErrorMessage(err)),
  });

  const archiveMutation = useMutation({
    mutationFn: async ({ doc, statut }: { doc: DeptDocument; statut: string }) => {
      await api.put(`/departments/${deptId}/documents/${doc.id}`, {
        titre: doc.titre, type: doc.type, description: doc.description || null,
        url: doc.url || null, statut,
      });
    },
    onSuccess: () => { toast.success('Statut mis à jour'); invalidate(); },
    onError: (err) => toast.error(getErrorMessage(err)),
  });

  const deleteMutation = useMutation({
    mutationFn: async (docId: string) => api.delete(`/departments/${deptId}/documents/${docId}`),
    onSuccess: () => { toast.success('Document supprimé'); invalidate(); },
    onError: (err) => toast.error(getErrorMessage(err)),
  });

  if (isLoading) {
    return <div className="flex justify-center py-10"><Loader2 className="w-6 h-6 animate-spin text-primary-500" /></div>;
  }

  return (
    <div className="space-y-4">
      {/* KPIs par type */}
      <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-6 gap-3">
        <div className="stat-card p-3 text-center">
          <span className="stat-label text-[10px]">Total</span>
          <p className="stat-value text-xl">{stats.total ?? documents.length}</p>
        </div>
        {DOC_TYPES.map((t) => (
          <div key={t} className="stat-card p-3 text-center">
            <span className={`badge text-[9px] ${DOC_TYPE_BADGES[t]}`}>{DOC_TYPE_LABELS[t]}</span>
            <p className="stat-value text-lg mt-1">{stats[t] ?? 0}</p>
          </div>
        ))}
      </div>

      <div className="glass-card p-5">
        <div className="flex items-center justify-between mb-4 flex-wrap gap-2">
          <div className="flex items-center gap-2">
            <BookOpen className="w-4 h-4 text-primary-500" />
            <h3 className="text-sm font-semibold text-gray-900 dark:text-gray-100">Bibliothèque du département</h3>
          </div>
          <button onClick={() => setShowCreate(!showCreate)} className="btn-primary btn-sm cursor-pointer">
            <Plus className="w-4 h-4" /> {showCreate ? 'Fermer' : 'Ajouter un document'}
          </button>
        </div>

        {showCreate && (
          <div className="p-4 rounded-2xl bg-gray-50 dark:bg-gray-800/40 border border-gray-200/60 dark:border-gray-700/40 mb-4">
            <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-3">
              <div>
                <label className="label" htmlFor="doc-titre">Titre *</label>
                <input id="doc-titre" className="input" value={titre} onChange={(e) => setTitre(e.target.value)}
                  placeholder="Ex : Procédure d'accueil, Guide son…" />
              </div>
              <div>
                <label className="label" htmlFor="doc-type">Type</label>
                <select id="doc-type" className="input" value={type} onChange={(e) => setType(e.target.value)}>
                  {DOC_TYPES.map((t) => <option key={t} value={t}>{DOC_TYPE_LABELS[t]}</option>)}
                </select>
              </div>
              <div className="sm:col-span-2">
                <label className="label" htmlFor="doc-url">Lien du document (optionnel)</label>
                <input id="doc-url" className="input" value={url} onChange={(e) => setUrl(e.target.value)}
                  placeholder="https://… (Drive, PDF, formulaire…) " />
              </div>
              <div className="sm:col-span-2 lg:col-span-4">
                <label className="label" htmlFor="doc-desc">Description</label>
                <input id="doc-desc" className="input" value={description} onChange={(e) => setDescription(e.target.value)}
                  placeholder="À quoi sert ce document, quand l'utiliser…" />
              </div>
            </div>
            <button onClick={() => createMutation.mutate()} disabled={!titre.trim() || createMutation.isPending}
              className="btn-primary btn-sm mt-3 cursor-pointer">
              <Plus className="w-4 h-4" /> Ajouter
            </button>
          </div>
        )}

        {documents.length === 0 ? (
          <div className="text-center py-8">
            <BookOpen className="w-10 h-10 text-gray-300 mx-auto mb-2" />
            <p className="text-sm text-gray-400">Aucun document — centralisez vos procédures, guides et formulaires ici.</p>
          </div>
        ) : (
          <div className="space-y-2">
            {documents.map((d) => (
              <div key={d.id} className={`flex items-start gap-3 p-3 rounded-xl bg-gray-50 dark:bg-gray-800/40 ${
                d.statut === 'ARCHIVE' ? 'opacity-50' : ''
              }`}>
                <div className={`p-2 rounded-lg shrink-0 ${DOC_TYPE_BADGES[d.type] ? 'bg-gray-100 dark:bg-gray-700' : ''}`}>
                  <BookOpen className="w-4 h-4 text-gray-500" />
                </div>
                <div className="min-w-0 flex-1">
                  <div className="flex items-center gap-2 flex-wrap">
                    <span className="text-sm font-semibold text-gray-900 dark:text-gray-100">{d.titre}</span>
                    <span className={`badge text-[9px] ${DOC_TYPE_BADGES[d.type] || 'badge-gray'}`}>{DOC_TYPE_LABELS[d.type] || d.type}</span>
                    {d.statut === 'ARCHIVE' && <span className="badge text-[9px] badge-inactive">Archivé</span>}
                  </div>
                  {d.description && <p className="text-xs text-gray-500 dark:text-gray-400 mt-0.5">{d.description}</p>}
                  <div className="flex items-center gap-3 mt-1">
                    {d.url && (
                      <a href={d.url} target="_blank" rel="noreferrer"
                        className="text-[11px] font-medium text-primary-600 hover:underline inline-flex items-center gap-1">
                        Ouvrir le document ↗
                      </a>
                    )}
                    <span className="text-[10px] text-gray-400">
                      {d.createdAt ? new Date(d.createdAt).toLocaleDateString('fr-FR') : ''}
                    </span>
                  </div>
                </div>
                <div className="flex items-center gap-1 shrink-0">
                  {d.statut === 'ACTIF' ? (
                    <button onClick={() => archiveMutation.mutate({ doc: d, statut: 'ARCHIVE' })}
                      className="p-1.5 rounded-lg text-gray-400 hover:text-amber-600 hover:bg-amber-500/10 transition-all cursor-pointer" title="Archiver">
                      <Archive className="w-3.5 h-3.5" />
                    </button>
                  ) : (
                    <button onClick={() => archiveMutation.mutate({ doc: d, statut: 'ACTIF' })}
                      className="p-1.5 rounded-lg text-gray-400 hover:text-emerald-600 hover:bg-emerald-500/10 transition-all cursor-pointer" title="Restaurer">
                      <CheckCircle2 className="w-3.5 h-3.5" />
                    </button>
                  )}
                  <button onClick={() => { if (confirm(`Supprimer le document « ${d.titre} » ?`)) deleteMutation.mutate(d.id); }}
                    className="p-1.5 rounded-lg text-gray-400 hover:text-red-600 hover:bg-red-500/10 transition-all cursor-pointer" title="Supprimer">
                    <Trash2 className="w-3.5 h-3.5" />
                  </button>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
}

// ============================================================
// PARAMÈTRES — seuils configurables des alertes intelligentes
// ============================================================

export function SettingsTab({ deptId }: { deptId: string }) {
  const queryClient = useQueryClient();
  const [absenceSeuil, setAbsenceSeuil] = useState(2);
  const [absencePeriode, setAbsencePeriode] = useState(3);
  const [inactiviteMois, setInactiviteMois] = useState(3);
  const [tacheRetardAlerte, setTacheRetardAlerte] = useState(true);
  const [eventRappelJours, setEventRappelJours] = useState(1);
  const [loaded, setLoaded] = useState(false);

  const { isLoading } = useQuery({
    queryKey: ['department', deptId, 'settings'],
    queryFn: async () => {
      const res = await api.get(`/departments/${deptId}/settings`);
      const s = res.data ?? {};
      setAbsenceSeuil(s.absenceSeuil ?? 2);
      setAbsencePeriode(s.absencePeriode ?? 3);
      setInactiviteMois(s.inactiviteMois ?? 3);
      setTacheRetardAlerte(s.tacheRetardAlerte ?? true);
      setEventRappelJours(s.eventRappelJours ?? 1);
      setLoaded(true);
      return s;
    },
    enabled: !!deptId,
  });

  const saveMutation = useMutation({
    mutationFn: async () => {
      await api.put(`/departments/${deptId}/settings`, {
        absenceSeuil, absencePeriode, inactiviteMois, tacheRetardAlerte, eventRappelJours,
      });
    },
    onSuccess: () => {
      toast.success('Seuils d\'alertes enregistrés ✅');
      queryClient.invalidateQueries({ queryKey: ['department', deptId, 'settings'] });
      queryClient.invalidateQueries({ queryKey: ['department', deptId, 'alerts'] });
    },
    onError: (err) => toast.error(getErrorMessage(err)),
  });

  const num = (v: string) => {
    const n = Number(v);
    return Number.isNaN(n) ? 0 : n;
  };

  return (
    <div className="glass-card p-5">
      <div className="flex items-center gap-2 mb-1">
        <Settings className="w-4 h-4 text-primary-500" />
        <h3 className="text-sm font-semibold text-gray-900 dark:text-gray-100">Paramètres du département</h3>
      </div>
      <p className="text-xs text-gray-400 mb-4">
        Seuils des alertes intelligentes — aucun paramètre n'est codé en dur : les règles
        lisent ces valeurs. Modifiez-les puis enregistrez.
      </p>

      {isLoading && !loaded ? (
        <div className="flex justify-center py-8"><Loader2 className="w-5 h-5 animate-spin text-primary-500" /></div>
      ) : (
        <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
          <div className="p-4 rounded-2xl bg-gray-50 dark:bg-gray-800/40 border border-gray-200/60 dark:border-gray-700/40">
            <div className="flex items-center gap-2 mb-1">
              <AlertTriangle className="w-4 h-4 text-amber-500" />
              <p className="text-sm font-medium text-gray-900 dark:text-gray-100">Absences répétées</p>
            </div>
            <p className="text-[11px] text-gray-400 mb-3">
              Alerte HAUTE quand un membre est absent au moins N fois sur la période (en semaines).
            </p>
            <div className="flex gap-3">
              <div className="flex-1">
                <label className="label" htmlFor="absence-seuil">Absences requises (1–10)</label>
                <input id="absence-seuil" type="number" min={1} max={10} className="input" value={absenceSeuil}
                  onChange={(e) => setAbsenceSeuil(num(e.target.value))} />
              </div>
              <div className="flex-1">
                <label className="label" htmlFor="absence-periode">Période en semaines (1–12)</label>
                <input id="absence-periode" type="number" min={1} max={12} className="input" value={absencePeriode}
                  onChange={(e) => setAbsencePeriode(num(e.target.value))} />
              </div>
            </div>
          </div>

          <div className="p-4 rounded-2xl bg-gray-50 dark:bg-gray-800/40 border border-gray-200/60 dark:border-gray-700/40">
            <div className="flex items-center gap-2 mb-1">
              <Activity className="w-4 h-4 text-emerald-500" />
              <p className="text-sm font-medium text-gray-900 dark:text-gray-100">Inactivité</p>
            </div>
            <p className="text-[11px] text-gray-400 mb-3">
              Alerte MOYENNE quand un membre n'a aucune fiche de présence depuis N mois (0 = désactivé).
            </p>
            <div className="flex-1">
              <label className="label" htmlFor="inactivite-mois">Mois sans présence (0–24)</label>
              <input id="inactivite-mois" type="number" min={0} max={24} className="input" value={inactiviteMois}
                onChange={(e) => setInactiviteMois(num(e.target.value))} />
            </div>
          </div>

          <div className="p-4 rounded-2xl bg-gray-50 dark:bg-gray-800/40 border border-gray-200/60 dark:border-gray-700/40 sm:col-span-2">
            <label className="flex items-center gap-3 cursor-pointer">
              <input type="checkbox" checked={tacheRetardAlerte}
                onChange={(e) => setTacheRetardAlerte(e.target.checked)}
                className="w-4 h-4 accent-amber-500 cursor-pointer" />
              <span className="text-sm font-medium text-gray-900 dark:text-gray-100">
                Activer l'alerte « tâche en retard »
              </span>
            </label>
            <p className="text-[11px] text-gray-400 mt-1">
              Alerte MOYENNE quand une tâche affectée dépasse son échéance.
            </p>
          </div>

          <div className="p-4 rounded-2xl bg-gray-50 dark:bg-gray-800/40 border border-gray-200/60 dark:border-gray-700/40 sm:col-span-2">
            <div className="flex items-center gap-2 mb-1">
              <CalendarDays className="w-4 h-4 text-indigo-500" />
              <p className="text-sm font-medium text-gray-900 dark:text-gray-100">Rappel automatique des événements</p>
            </div>
            <p className="text-[11px] text-gray-400 mb-3">
              Notifie le responsable N jours avant chaque événement rattaché au département
              (0 = rappel désactivé). En plus du rappel J-1 envoyé aux inscrits.
            </p>
            <div className="flex-1">
              <label className="label" htmlFor="event-rappel-jours">Jours avant l'événement (0–30)</label>
              <input id="event-rappel-jours" type="number" min={0} max={30} className="input" value={eventRappelJours}
                onChange={(e) => setEventRappelJours(num(e.target.value))} />
            </div>
          </div>
        </div>
      )}

      <button onClick={() => saveMutation.mutate()} disabled={saveMutation.isPending}
        className="btn-primary btn-sm mt-4 cursor-pointer">
        {saveMutation.isPending ? <Loader2 className="w-4 h-4 animate-spin" /> : <Save className="w-4 h-4" />}
        Enregistrer les paramètres
      </button>
    </div>
  );
}
