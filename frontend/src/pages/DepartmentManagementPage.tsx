import { useMemo, useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useParams, Link, useNavigate } from 'react-router-dom';
import api, { getErrorMessage } from '@/lib/api';
import toast from 'react-hot-toast';
import {
  Building2, ArrowLeft, Network, Briefcase, Users2, ListTodo, History,
  Plus, Pencil, Archive, Trash2, UserPlus, Loader2, CheckCircle2,
  Clock, AlertTriangle, CalendarDays, ChevronRight, ChevronDown, Save,
  X, Star, Flag, FolderTree, Activity,
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
            <p className="page-subtitle">Organisation · postes · affectations · tâches · activité</p>
          </div>
        </div>
      </div>

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
        {TABS.map((t) => {
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
        <OrganisationTab teams={teams} rootTeams={rootTeams} members={members} onChanged={invalidate} />
      )}
      {tab === 'positions' && <PositionsTab positions={positions} onChanged={invalidate} />}
      {tab === 'assignments' && (
        <AssignmentsTab assignments={assignments} teams={teams} positions={positions} members={members} onChanged={invalidate} />
      )}
      {tab === 'tasks' && (
        <TasksTab taskStats={taskStats} teams={teams} members={members} onChanged={invalidate} />
      )}
      {tab === 'activity' && <ActivityTab activity={activity} />}
    </div>
  );
}

// ============================================================
// ORGANISATION — arbre des équipes / sous-départements
// ============================================================

function TeamNode({ team, teams, depth = 0 }: { team: Team; teams: Team[]; depth?: number }) {
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
          <TeamActions team={team} teams={teams} onChanged={() => {}} />
        </div>
      </div>
      {open && children.map((c) => <TeamNode key={c.id} team={c} teams={teams} depth={depth + 1} />)}
    </div>
  );
}

function TeamActions({ team, teams, onChanged }: { team: Team; teams: Team[]; onChanged: () => void }) {
  const [editing, setEditing] = useState(false);
  const queryClient = useQueryClient();
  const invalidate = () => queryClient.invalidateQueries({ queryKey: ['department'] });

  const archiveMutation = useMutation({
    mutationFn: async () => {
      await api.delete(`/departments/${getDeptId()}/teams/${team.id}`);
    },
    onSuccess: () => { toast.success('Équipe archivée'); invalidate(); },
    onError: (err) => toast.error(getErrorMessage(err)),
  });

  function getDeptId() {
    const m = window.location.pathname.match(/^\/departments\/([^/]+)\/manage/);
    return m ? m[1] : '';
  }

  if (editing) {
    return (
      <EditTeamForm
        team={team}
        teams={teams}
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

function EditTeamForm({ team, teams, onDone }: { team: Team; teams: Team[]; onDone: () => void }) {
  const queryClient = useQueryClient();
  const [nom, setNom] = useState(team.nom);
  const [parentId, setParentId] = useState(team.parentId || '');
  const [type, setType] = useState(team.type);
  const [objectif, setObjectif] = useState(team.objectif || '');
  const [dateFin, setDateFin] = useState(team.dateFin || '');

  const updateMutation = useMutation({
    mutationFn: async () => {
      await api.put(`/departments/${getDeptId()}/teams/${team.id}`, {
        nom, parentId: parentId || null, type, objectif: objectif || null, dateFin: dateFin || null,
      });
    },
    onSuccess: () => { toast.success('Équipe modifiée ✅'); queryClient.invalidateQueries({ queryKey: ['department'] }); onDone(); },
    onError: (err) => toast.error(getErrorMessage(err)),
  });

  function getDeptId() {
    const m = window.location.pathname.match(/^\/departments\/([^/]+)\/manage/);
    return m ? m[1] : '';
  }

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

function OrganisationTab({ teams, rootTeams, members, onChanged }: { teams: Team[]; rootTeams: Team[]; members: any[]; onChanged: () => void }) {
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
        <CreateTeamForm teams={teams} members={members} onDone={() => { setShowCreate(false); onChanged(); }} />
      )}
      <div className="mt-2 space-y-1">
        {rootTeams.length === 0 && (
          <div className="text-center py-10">
            <Network className="w-10 h-10 text-gray-300 mx-auto mb-2" />
            <p className="text-sm text-gray-400">Aucune équipe — créez votre premier sous-département ou équipe</p>
          </div>
        )}
        {rootTeams.map((t) => <TeamNode key={t.id} team={t} teams={teams} />)}
      </div>
    </div>
  );
}

function CreateTeamForm({ teams, members, onDone }: { teams: Team[]; members: any[]; onDone: () => void }) {
  const queryClient = useQueryClient();
  const [nom, setNom] = useState('');
  const [type, setType] = useState('EQUIPE_PERMANENTE');
  const [parentId, setParentId] = useState('');
  const [objectif, setObjectif] = useState('');
  const [dateDebut, setDateDebut] = useState('');
  const [dateFin, setDateFin] = useState('');

  const createMutation = useMutation({
    mutationFn: async () => {
      await api.post(`/departments/${getDeptId()}/teams`, {
        nom, type, parentId: parentId || null, objectif: objectif || null,
        dateDebut: dateDebut || null, dateFin: dateFin || null,
      });
    },
    onSuccess: () => { toast.success('Équipe créée ✅'); queryClient.invalidateQueries({ queryKey: ['department'] }); onDone(); },
    onError: (err) => toast.error(getErrorMessage(err)),
  });

  function getDeptId() {
    const m = window.location.pathname.match(/^\/departments\/([^/]+)\/manage/);
    return m ? m[1] : '';
  }

  return (
    <div className="p-4 rounded-2xl bg-gray-50 dark:bg-gray-800/40 border border-gray-200/60 dark:border-gray-700/40 mb-4">
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-3">
        <div>
          <label className="label">Nom de l'équipe *</label>
          <input className="input" value={nom} onChange={(e) => setNom(e.target.value)} placeholder="Ex : Son, Vidéo, Convention 2026…" />
        </div>
        <div>
          <label className="label">Type</label>
          <select className="input" value={type} onChange={(e) => setType(e.target.value)}>
            {Object.entries(TYPE_LABELS).map(([k, v]) => <option key={k} value={k}>{v}</option>)}
          </select>
        </div>
        <div>
          <label className="label">Équipe parente</label>
          <select className="input" value={parentId} onChange={(e) => setParentId(e.target.value)}>
            <option value="">— Aucune (racine) —</option>
            {teams.map((t) => <option key={t.id} value={t.id}>{t.nom}</option>)}
          </select>
        </div>
        {type === 'EQUIPE_TEMPORAIRE' && (
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

function PositionsTab({ positions, onChanged }: { positions: Position[]; onChanged: () => void }) {
  const queryClient = useQueryClient();
  const [nom, setNom] = useState('');
  const [description, setDescription] = useState('');
  const [competences, setCompetences] = useState('');

  const createMutation = useMutation({
    mutationFn: async () => {
      await api.post(`/departments/${getDeptId()}/positions`, { nom, description: description || null, competencesRequises: competences || null });
    },
    onSuccess: () => { toast.success('Poste créé ✅'); setNom(''); setDescription(''); setCompetences(''); queryClient.invalidateQueries({ queryKey: ['department'] }); onChanged(); },
    onError: (err) => toast.error(getErrorMessage(err)),
  });
  const archiveMutation = useMutation({
    mutationFn: async (positionId: string) => api.delete(`/departments/${getDeptId()}/positions/${positionId}`),
    onSuccess: () => { toast.success('Poste archivé'); queryClient.invalidateQueries({ queryKey: ['department'] }); onChanged(); },
    onError: (err) => toast.error(getErrorMessage(err)),
  });

  function getDeptId() {
    const m = window.location.pathname.match(/^\/departments\/([^/]+)\/manage/);
    return m ? m[1] : '';
  }

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

function AssignmentsTab({ assignments, teams, positions, members, onChanged }: {
  assignments: Assignment[]; teams: Team[]; positions: Position[]; members: any[]; onChanged: () => void;
}) {
  const queryClient = useQueryClient();
  const [memberId, setMemberId] = useState('');
  const [teamId, setTeamId] = useState('');
  const [positionId, setPositionId] = useState('');
  const [role, setRole] = useState('MEMBRE');
  const [dateDebut, setDateDebut] = useState('');

  const createMutation = useMutation({
    mutationFn: async () => {
      await api.post(`/departments/${getDeptId()}/assignments`, {
        memberId, teamId: teamId || null, positionId: positionId || null, role, dateDebut: dateDebut || null,
      });
    },
    onSuccess: () => { toast.success('Membre affecté ✅'); setMemberId(''); setTeamId(''); setPositionId(''); setRole('MEMBRE'); setDateDebut(''); queryClient.invalidateQueries({ queryKey: ['department'] }); onChanged(); },
    onError: (err) => toast.error(getErrorMessage(err)),
  });
  const endMutation = useMutation({
    mutationFn: async (assignmentId: string) => api.delete(`/departments/${getDeptId()}/assignments/${assignmentId}`),
    onSuccess: () => { toast.success('Affectation terminée'); queryClient.invalidateQueries({ queryKey: ['department'] }); onChanged(); },
    onError: (err) => toast.error(getErrorMessage(err)),
  });

  function getDeptId() {
    const m = window.location.pathname.match(/^\/departments\/([^/]+)\/manage/);
    return m ? m[1] : '';
  }

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

function TasksTab({ taskStats, teams, members, onChanged }: {
  taskStats: any; teams: Team[]; members: any[]; onChanged: () => void;
}) {
  const queryClient = useQueryClient();
  const { data: tasks = [] } = useQuery({
    queryKey: ['department', getDeptId(), 'tasks'],
    queryFn: async () => (await api.get(`/departments/${getDeptId()}/tasks`)).data as Task[],
    enabled: !!getDeptId(),
  });
  const [titre, setTitre] = useState('');
  const [teamId, setTeamId] = useState('');
  const [assignedTo, setAssignedTo] = useState('');
  const [priorite, setPriorite] = useState('MOYENNE');
  const [echeance, setEcheance] = useState('');
  const [showCreate, setShowCreate] = useState(false);

  const createMutation = useMutation({
    mutationFn: async () => {
      await api.post(`/departments/${getDeptId()}/tasks`, {
        titre, teamId: teamId || null, assignedTo: assignedTo || null, priorite, echeance: echeance || null,
      });
    },
    onSuccess: () => { toast.success('Tâche créée ✅'); setTitre(''); setTeamId(''); setAssignedTo(''); setPriorite('MOYENNE'); setEcheance(''); setShowCreate(false); invalidateAll(); },
    onError: (err) => toast.error(getErrorMessage(err)),
  });

  const updateMutation = useMutation({
    mutationFn: async ({ taskId, data }: { taskId: string; data: any }) =>
      (await api.put(`/departments/${getDeptId()}/tasks/${taskId}`, data)).data,
    onSuccess: () => invalidateAll(),
    onError: (err) => toast.error(getErrorMessage(err)),
  });
  const deleteMutation = useMutation({
    mutationFn: async (taskId: string) => api.delete(`/departments/${getDeptId()}/tasks/${taskId}`),
    onSuccess: () => { toast.success('Tâche supprimée'); invalidateAll(); },
    onError: (err) => toast.error(getErrorMessage(err)),
  });

  function getDeptId() {
    const m = window.location.pathname.match(/^\/departments\/([^/]+)\/manage/);
    return m ? m[1] : '';
  }
  function invalidateAll() {
    queryClient.invalidateQueries({ queryKey: ['department', getDeptId(), 'tasks'] });
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
