import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import api, { getErrorMessage } from '@/lib/api';
import toast from 'react-hot-toast';
import {
  Network, Plus, Pencil, Archive, Users2, FolderTree, ChevronDown,
  ChevronRight, CalendarDays, Save, X, Loader2,
} from 'lucide-react';
import type { Team } from './types';
import { TYPE_LABELS, TYPE_COLORS } from './types';

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
          <TeamActions team={team} teams={teams} deptId={deptId} members={[]} onChanged={() => {}} />
        </div>
      </div>
      {open && children.map((c) => <TeamNode key={c.id} team={c} teams={teams} depth={depth + 1} deptId={deptId} />)}
    </div>
  );
}

function TeamActions({ team, teams, deptId, members, onChanged }: { team: Team; teams: Team[]; deptId: string; members: any[]; onChanged: () => void }) {
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
        members={members}
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

function EditTeamForm({ team, teams, deptId, members, onDone }: { team: Team; teams: Team[]; deptId: string; members: any[]; onDone: () => void }) {
  const queryClient = useQueryClient();
  const [nom, setNom] = useState(team.nom);
  const [parentId, setParentId] = useState(team.parentId || '');
  const [type, setType] = useState(team.type);
  const [objectif, setObjectif] = useState(team.objectif || '');
  const [dateFin, setDateFin] = useState(team.dateFin || '');
  const [chefId, setChefId] = useState('');
  const [adjointId, setAdjointId] = useState('');

  const updateMutation = useMutation({
    mutationFn: async () => {
      await api.put(`/departments/${deptId}/teams/${team.id}`, {
        nom, parentId: parentId || null, type, objectif: objectif || null, dateFin: dateFin || null,
        chefId: chefId || null, adjointId: adjointId || null,
      });
    },
    onSuccess: () => { toast.success('Équipe modifiée ✅'); queryClient.invalidateQueries({ queryKey: ['department'] }); onDone(); },
    onError: (err) => toast.error(getErrorMessage(err)),
  });

  return (
    <div className="p-3 rounded-2xl bg-gray-50 dark:bg-gray-800/40 border border-gray-200/60 dark:border-gray-700/40">
      <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-5 gap-3">
        <div>
          <label className="label text-[10px]">Nom</label>
          <input className="input py-1.5 text-xs" value={nom} onChange={(e) => setNom(e.target.value)} placeholder="Nom de l'équipe" />
        </div>
        <div>
          <label className="label text-[10px]">Type</label>
          <select className="input py-1.5 text-xs" value={type} onChange={(e) => setType(e.target.value)}>
            {Object.entries(TYPE_LABELS).map(([k, v]) => <option key={k} value={k}>{v}</option>)}
          </select>
        </div>
        <div>
          <label className="label text-[10px]">Chef d'équipe</label>
          <select className="input py-1.5 text-xs" value={chefId} onChange={(e) => setChefId(e.target.value)}>
            <option value="">— Aucun —</option>
            {members.map((m) => <option key={m.id} value={m.id}>{m.nom}</option>)}
          </select>
        </div>
        <div>
          <label className="label text-[10px]">Adjoint(e)</label>
          <select className="input py-1.5 text-xs" value={adjointId} onChange={(e) => setAdjointId(e.target.value)}>
            <option value="">— Aucun —</option>
            {members.map((m) => <option key={m.id} value={m.id}>{m.nom}</option>)}
          </select>
        </div>
        <div>
          <label className="label text-[10px]">Objectif</label>
          <input className="input py-1.5 text-xs" value={objectif} onChange={(e) => setObjectif(e.target.value)} placeholder="Objectif" />
        </div>
      </div>
      <div className="flex items-center gap-2 mt-2">
        <button onClick={() => updateMutation.mutate()} disabled={updateMutation.isPending} className="btn-primary btn-xs cursor-pointer">
          <Save className="w-3 h-3" /> Enregistrer
        </button>
        <button onClick={onDone} className="btn-ghost btn-xs cursor-pointer"><X className="w-3 h-3" /> Annuler</button>
      </div>
    </div>
  );
}

export function OrganisationTab({ teams, rootTeams, members, deptId, onChanged }: { teams: Team[]; rootTeams: Team[]; members: any[]; deptId: string; onChanged: () => void }) {
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
  const [chefId, setChefId] = useState('');
  const [adjointId, setAdjointId] = useState('');

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
        chefId: chefId || null, adjointId: adjointId || null,
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
    <div className="p-4 rounded-2xl bg-gray-50 dark:bg-gray-800/40 border border-gray-200/60 dark:border-gray-700/40 mb-4">        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-5 gap-3">
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
        <div>
          <label className="label">Chef d'équipe</label>
          <select className="input" value={chefId} onChange={(e) => setChefId(e.target.value)}>
            <option value="">— Aucun —</option>
            {members.map((m) => <option key={m.id} value={m.id}>{m.nom}</option>)}
          </select>
        </div>
        <div>
          <label className="label">Adjoint(e)</label>
          <select className="input" value={adjointId} onChange={(e) => setAdjointId(e.target.value)}>
            <option value="">— Aucun —</option>
            {members.map((m) => <option key={m.id} value={m.id}>{m.nom}</option>)}
          </select>
        </div>
        <div className="sm:col-span-2 lg:col-span-5">
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

