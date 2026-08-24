import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import toast from 'react-hot-toast';
import api, { getErrorMessage } from '@/lib/api';
import {
  TrendingUp, Plus, Loader2, RefreshCw, Sparkles, Target, CheckCircle,
  Clock, BarChart3, User, ChevronDown, X, ArrowRight,
} from 'lucide-react';

interface DevPlan {
  id: string;
  membreId: string;
  membreName: string;
  objectives: DevObjective[];
  status: 'ACTIVE' | 'COMPLETED' | 'ARCHIVED';
  progress: number;
  createdAt: string;
}

interface DevObjective {
  id: string;
  title: string;
  description: string;
  category: string;
  deadline: string;
  completed: boolean;
  completedAt?: string;
}

export default function DevPlanPage() {
  const qc = useQueryClient();
  const [selectedMember, setSelectedMember] = useState('');
  const [showCreate, setShowCreate] = useState(false);
  const [newObjective, setNewObjective] = useState({ title: '', description: '', category: 'COMPETENCE', deadline: '' });

  const { data: plans = [], isLoading, refetch } = useQuery({
    queryKey: ['dev-plans', selectedMember],
    queryFn: async () => {
      const url = selectedMember ? `/development-plans/by-member/${selectedMember}` : '/development-plans';
      const res = await api.get(url);
      return res.data as DevPlan[];
    },
  });

  const autoGenMutation = useMutation({
    mutationFn: async () => { await api.post('/development-plans/auto-generate'); },
    onSuccess: () => { toast.success('Plans auto-générés par IA'); qc.invalidateQueries({ queryKey: ['dev-plans'] }); },
    onError: (err: unknown) => toast.error(getErrorMessage(err)),
  });

  const getStatusColor = (status: string) => {
    switch (status) { case 'ACTIVE': return 'badge-success'; case 'COMPLETED': return 'badge-info'; default: return 'badge-warning'; }
  };

  return (
    <div className="page-container max-w-6xl">
      <div className="page-header">
        <div><h1 className="page-title flex items-center gap-2"><TrendingUp className="w-5 h-5 text-primary-500" /> Plans de développement</h1>
          <p className="page-subtitle">Objectifs individuels générés par IA basés sur les performances.</p></div>
        <div className="page-header-actions">
          <button onClick={() => refetch()} className="btn-ghost btn-sm"><RefreshCw className="w-4 h-4" /></button>
          <button onClick={() => autoGenMutation.mutate()} disabled={autoGenMutation.isPending} className="btn-ghost btn-sm">
            {autoGenMutation.isPending ? <Loader2 className="w-4 h-4 animate-spin" /> : <Sparkles className="w-4 h-4" />} Auto-générer
          </button>
          <button className="btn-primary btn-sm" onClick={() => setShowCreate(true)}><Plus className="w-4 h-4" /> Nouvel objectif</button>
        </div>
      </div>

      {plans.length === 0 ? (
        <div className="glass-card p-10 text-center animate-scale-in">
          <TrendingUp className="w-10 h-10 text-gray-300 mb-3 mx-auto" />
          <p className="text-gray-500 font-medium">Aucun plan de développement.</p>
          <p className="text-xs text-gray-400 mt-1">Utilisez "Auto-générer" pour créer des plans basés sur les performances.</p>
        </div>
      ) : (
        <div className="space-y-4">
          {plans.map((plan, i) => (
            <div key={plan.id} className="glass-card p-5 animate-slide-up" style={{ animationDelay: `${i * 60}ms` }}>
              <div className="flex items-center justify-between mb-3">
                <div className="flex items-center gap-3">
                  <div className="w-10 h-10 rounded-xl bg-gradient-to-br from-primary-500 to-primary-600 flex items-center justify-center text-white">
                    <User className="w-5 h-5" />
                  </div>
                  <div>
                    <h3 className="text-sm font-bold text-gray-900 dark:text-gray-100">{plan.membreName}</h3>
                    <span className={`badge text-[10px] ${getStatusColor(plan.status)}`}>{plan.status}</span>
                  </div>
                </div>
                <div className="text-right">
                  <p className="text-lg font-bold text-primary-500">{plan.progress}%</p>
                  <div className="w-20 h-1.5 rounded-full bg-gray-200 dark:bg-gray-700 overflow-hidden">
                    <div className="h-full rounded-full bg-primary-500" style={{ width: `${plan.progress}%` }} />
                  </div>
                </div>
              </div>
              <div className="space-y-2">
                {plan.objectives?.map((obj) => (
                  <div key={obj.id} className={`flex items-center gap-3 p-3 rounded-xl ${obj.completed ? 'bg-green-50 dark:bg-green-900/10' : 'bg-gray-50 dark:bg-gray-800/40'}`}>
                    <CheckCircle className={`w-4 h-4 ${obj.completed ? 'text-green-500' : 'text-gray-300'}`} />
                    <div className="flex-1">
                      <p className={`text-sm font-medium ${obj.completed ? 'text-green-700 dark:text-green-300 line-through' : 'text-gray-900 dark:text-gray-100'}`}>{obj.title}</p>
                      <p className="text-[10px] text-gray-400">{obj.category} • {obj.deadline ? new Date(obj.deadline).toLocaleDateString('fr-FR') : 'Pas de deadline'}</p>
                    </div>
                  </div>
                ))}
              </div>
            </div>
          ))}
        </div>
      )}

      {showCreate && (
        <div className="modal-overlay" onClick={() => setShowCreate(false)}>
          <div className="modal-content max-w-md" onClick={e => e.stopPropagation()}>
            <div className="modal-header"><h3 className="text-base font-bold">Nouvel objectif</h3><button onClick={() => setShowCreate(false)} className="btn-icon"><X className="w-5 h-5" /></button></div>
            <div className="modal-body space-y-4">
              <div><label className="label">Titre</label><input className="input" value={newObjective.title} onChange={e => setNewObjective({ ...newObjective, title: e.target.value })} placeholder="Ex: Compléter la formation leadership" /></div>
              <div><label className="label">Description</label><textarea className="input min-h-[60px]" value={newObjective.description} onChange={e => setNewObjective({ ...newObjective, description: e.target.value })} /></div>
              <div><label className="label">Catégorie</label>
                <select className="input" value={newObjective.category} onChange={e => setNewObjective({ ...newObjective, category: e.target.value })}>
                  <option value="COMPETENCE">Compétence</option><option value="FORMATION">Formation</option><option value="SERVITE">Service</option><option value="SPIRITUEL">Spirituel</option>
                </select></div>
              <div><label className="label">Date limite</label><input type="date" className="input" value={newObjective.deadline} onChange={e => setNewObjective({ ...newObjective, deadline: e.target.value })} /></div>
            </div>
            <div className="modal-footer">
              <button className="btn-ghost btn-sm" onClick={() => setShowCreate(false)}>Annuler</button>
              <button className="btn-primary btn-sm" disabled={!newObjective.title}><CheckCircle className="w-4 h-4" /> Créer</button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
