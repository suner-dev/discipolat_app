import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import toast from 'react-hot-toast';
import api, { getErrorMessage } from '@/lib/api';
import {
  Users, Plus, Loader2, RefreshCw, Search, Star, Calendar, Wrench,
  BarChart3, UserPlus, CheckCircle, X, Filter, ChevronDown, Clock,
} from 'lucide-react';

interface Volunteer {
  id: string;
  userId: string;
  userName: string;
  skills: string[];
  availability: string[];
  maxHoursPerWeek: number;
  totalHours: number;
  rating: number;
  status: 'ACTIVE' | 'INACTIVE' | 'ON_LEAVE';
}

interface MatchResult {
  volunteerId: string;
  volunteerName: string;
  score: number;
  matchingSkills: string[];
}

const SKILLS = ['Musique', 'Son', 'Projection', 'Accueil', 'Enseignement', 'Jeunesse', 'Informatique', 'Transport', 'Cuisine', 'Nettoyage', 'Photographie', 'Video', 'Animation', 'Prière', 'Catéchèse'];

export default function VolunteersPage() {
  const qc = useQueryClient();
  const [search, setSearch] = useState('');
  const [showAdd, setShowAdd] = useState(false);
  const [selectedSkills, setSelectedSkills] = useState<string[]>([]);
  const [newVolunteer, setNewVolunteer] = useState({ userName: '', skills: [] as string[], maxHoursPerWeek: 10 });

  const { data: volunteers = [], isLoading, refetch } = useQuery({
    queryKey: ['volunteers'],
    queryFn: async () => { const res = await api.get('/volunteers'); return res.data as Volunteer[]; },
  });

  const { data: stats } = useQuery({
    queryKey: ['volunteers', 'stats'],
    queryFn: async () => { const res = await api.get('/volunteers/stats'); return res.data as Record<string, unknown>; },
  });

  const { data: matches = [], refetch: refetchMatches } = useQuery({
    queryKey: ['volunteers', 'match', selectedSkills],
    queryFn: async () => {
      if (selectedSkills.length === 0) return [];
      const res = await api.get('/volunteers/match', { params: { skills: selectedSkills.join(',') } });
      return res.data as MatchResult[];
    },
    enabled: selectedSkills.length > 0,
  });

  const createMutation = useMutation({
    mutationFn: async () => { await api.post('/volunteers', newVolunteer); },
    onSuccess: () => { toast.success('Bénévole ajouté'); qc.invalidateQueries({ queryKey: ['volunteers'] }); setShowAdd(false); },
    onError: (err: unknown) => toast.error(getErrorMessage(err)),
  });

  const filtered = volunteers.filter(v => !search || v.userName.toLowerCase().includes(search.toLowerCase()));

  const toggleSkill = (skill: string) => {
    setSelectedSkills(prev => prev.includes(skill) ? prev.filter(s => s !== skill) : [...prev, skill]);
  };

  const getStatusColor = (status: string) => {
    switch (status) { case 'ACTIVE': return 'badge-success'; case 'ON_LEAVE': return 'badge-warning'; default: return 'badge-error'; }
  };

  return (
    <div className="page-container max-w-6xl">
      <div className="page-header">
        <div><h1 className="page-title flex items-center gap-2"><Users className="w-5 h-5 text-primary-500" /> Bénévoles</h1>
          <p className="page-subtitle">Compétences, disponibilité et matching événements→bénévoles.</p></div>
        <div className="page-header-actions">
          <button onClick={() => refetch()} className="btn-ghost btn-sm"><RefreshCw className="w-4 h-4" /></button>
          <button className="btn-primary btn-sm" onClick={() => setShowAdd(true)}><Plus className="w-4 h-4" /> Ajouter</button>
        </div>
      </div>

      {/* Stats */}
      {stats && (
        <div className="grid grid-cols-2 lg:grid-cols-4 gap-4 mb-6">
          {[
            { label: 'Total', value: (stats['totalVolunteers'] as number) ?? 0, icon: Users, color: 'primary' },
            { label: 'Actifs', value: (stats['activeVolunteers'] as number) ?? 0, icon: CheckCircle, color: 'green' },
            { label: 'Heures totales', value: (stats['totalHours'] as number) ?? 0, icon: Calendar, color: 'blue' },
            { label: 'Note moyenne', value: ((stats['averageRating'] as number) ?? 0).toFixed(1), icon: Star, color: 'amber' },
          ].map((s, i) => (
            <div key={s.label} className="stat-card animate-slide-up" style={{ animationDelay: `${i * 60}ms` }}>
              <div className="flex items-start justify-between mb-2">
                <span className="stat-label text-[10px]">{s.label}</span>
                <s.icon className={`w-4 h-4 text-${s.color}-500`} />
              </div>
              <p className="stat-value text-2xl">{s.value}</p>
            </div>
          ))}
        </div>
      )}

      {/* Skills matching */}
      <div className="glass-card p-4 mb-6 animate-slide-up">
        <div className="flex items-center gap-2 mb-3"><Wrench className="w-4 h-4 text-primary-500" /><span className="text-sm font-semibold">Recherche par compétences</span></div>
        <div className="flex flex-wrap gap-2">
          {SKILLS.map(skill => (
            <button key={skill} onClick={() => toggleSkill(skill)}
              className={`px-3 py-1.5 rounded-lg text-xs font-medium transition-all ${selectedSkills.includes(skill) ? 'bg-primary-500 text-white' : 'bg-gray-100 dark:bg-gray-800 text-gray-600 dark:text-gray-400 hover:bg-primary-50 dark:hover:bg-primary-900/20'}`}>
              {skill}
            </button>
          ))}
        </div>
        {matches.length > 0 && (
          <div className="mt-3 p-3 rounded-xl bg-green-50 dark:bg-green-900/10 border border-green-200/50 dark:border-green-700/30">
            <p className="text-xs font-semibold text-green-700 dark:text-green-400 mb-2">🎯 {matches.length} bénévole(s) correspondant(s)</p>
            {matches.slice(0, 5).map(m => (
              <div key={m.volunteerId} className="flex items-center gap-2 text-xs text-green-600 dark:text-green-300">
                <span className="font-medium">{m.volunteerName}</span>
                <span className="text-green-500">• Score: {(m.score * 100).toFixed(0)}%</span>
                <span className="text-green-400">• {m.matchingSkills.join(', ')}</span>
              </div>
            ))}
          </div>
        )}
      </div>

      {/* Volunteers list */}
      {filtered.length === 0 ? (
        <div className="glass-card p-10 text-center"><Users className="w-10 h-10 text-gray-300 mb-3 mx-auto" /><p className="text-gray-500">Aucun bénévole.</p></div>
      ) : (
        <div className="space-y-3">
          {filtered.map((v, i) => (
            <div key={v.id} className="glass-card px-5 py-4 flex items-center gap-4 animate-slide-up" style={{ animationDelay: `${i * 40}ms` }}>
              <div className="w-11 h-11 rounded-xl bg-gradient-to-br from-primary-500 to-emerald-600 flex items-center justify-center text-white text-sm font-bold flex-shrink-0">
                {v.userName.split(' ').map(n => n[0]).join('').substring(0, 2)}
              </div>
              <div className="flex-1 min-w-0">
                <div className="flex items-center gap-2"><span className="text-sm font-bold text-gray-900 dark:text-gray-100">{v.userName}</span>
                  <span className={`badge text-[10px] ${getStatusColor(v.status)}`}>{v.status}</span></div>
                <div className="flex flex-wrap gap-1 mt-1">
                  {v.skills.map(s => <span key={s} className="px-1.5 py-0.5 rounded text-[9px] bg-primary-50 dark:bg-primary-900/20 text-primary-600 dark:text-primary-400">{s}</span>)}
                </div>
                <div className="flex items-center gap-3 mt-1 text-[10px] text-gray-400">
                  <span className="flex items-center gap-1"><Clock className="w-3 h-3" /> {v.totalHours}h total</span>
                  <span className="flex items-center gap-1"><Calendar className="w-3 h-3" /> {v.availability?.join(', ')}</span>
                  <span className="flex items-center gap-1"><Star className="w-3 h-3 text-amber-400" /> {v.rating?.toFixed(1)}</span>
                </div>
              </div>
            </div>
          ))}
        </div>
      )}

      {/* Add modal */}
      {showAdd && (
        <div className="modal-overlay" onClick={() => setShowAdd(false)}>
          <div className="modal-content max-w-md" onClick={e => e.stopPropagation()}>
            <div className="modal-header"><h3 className="text-base font-bold">Ajouter un bénévole</h3><button onClick={() => setShowAdd(false)} className="btn-icon"><X className="w-5 h-5" /></button></div>
            <div className="modal-body space-y-4">
              <div><label className="label">Nom</label><input className="input" value={newVolunteer.userName} onChange={e => setNewVolunteer({ ...newVolunteer, userName: e.target.value })} /></div>
              <div><label className="label">Heures max/semaine</label><input type="number" className="input" value={newVolunteer.maxHoursPerWeek} onChange={e => setNewVolunteer({ ...newVolunteer, maxHoursPerWeek: Number(e.target.value) })} /></div>
            </div>
            <div className="modal-footer">
              <button className="btn-ghost btn-sm" onClick={() => setShowAdd(false)}>Annuler</button>
              <button className="btn-primary btn-sm" onClick={() => createMutation.mutate()} disabled={createMutation.isPending || !newVolunteer.userName}>
                {createMutation.isPending ? <Loader2 className="w-4 h-4 animate-spin" /> : <UserPlus className="w-4 h-4" />} Ajouter
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
