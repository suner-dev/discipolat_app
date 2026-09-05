import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import api, { getErrorMessage } from '@/lib/api';
import { useAuth } from '@/contexts/AuthContext';
import toast from 'react-hot-toast';
import {
  Shield,
  Loader2,
  CheckCircle2,
  XCircle,
  BarChart3,
  Plus,
  Trash2,
  X,
} from 'lucide-react';

type TypeDemande = 'BAPTEME' | 'DEDICACE' | 'ACCUEIL_NOUVEAU' | 'TRANSFERT' | 'MARIAGE' | 'BENEDICTION';
type Statut = 'SOUMISE' | 'EN_EXAMEN' | 'APPROUVEE' | 'REJETEE' | 'TRAITEE';

interface AdminRequest {
  id: string;
  tenantId: string;
  demandeurId: string;
  typeDemande: TypeDemande;
  motif: string;
  details?: string;
  statut: Statut;
  traitePar?: string;
  traiteLe?: string;
  commentaireTraitement?: string;
  soumiseLe: string;
}

interface AdminDemoRequest {
  id: string;
  churchName: string;
  contactName: string;
  contactEmail: string;
  status: string;
  createdAt: string;
}

interface Stats {
  total: number;
  enExamen: number;
  approuvees: number;
  rejetees: number;
}

interface CreateAdminRequest {
  typeDemande: TypeDemande;
  motif: string;
  details?: string;
}

const STATUS_STYLE: Record<string, string> = {
  SOUMISE: 'text-yellow-400 bg-yellow-500/20',
  EN_EXAMEN: 'text-blue-400 bg-blue-500/20',
  APPROUVEE: 'text-green-400 bg-green-500/20',
  REJETEE: 'text-red-400 bg-red-500/20',
  TRAITEE: 'text-purple-400 bg-purple-500/20',
};

const STATUS_LABELS: Record<Statut, string> = {
  SOUMISE: 'Soumise',
  EN_EXAMEN: 'En examen',
  APPROUVEE: 'Approuvée',
  REJETEE: 'Rejetée',
  TRAITEE: 'Traitée',
};

const TYPE_LABELS: Record<TypeDemande, string> = {
  BAPTEME: 'Baptême',
  DEDICACE: 'Dédicace',
  ACCUEIL_NOUVEAU: 'Accueil nouveau',
  TRANSFERT: 'Transfert',
  MARIAGE: 'Mariage',
  BENEDICTION: 'Bénédiction',
};

export default function AdminRequestsPage() {
  const qc = useQueryClient();
  const { user } = useAuth();
  const [tab, setTab] = useState<'requests' | 'demo'>('requests');
  const [showCreate, setShowCreate] = useState(false);
  const [form, setForm] = useState<CreateAdminRequest>({
    typeDemande: 'BAPTEME',
    motif: '',
    details: '',
  });

  const { data: requests = [], isLoading } = useQuery({
    queryKey: ['admin-requests'],
    queryFn: async () => (await api.get('/admin-requests')).data as AdminRequest[],
  });

  const { data: stats } = useQuery({
    queryKey: ['admin-requests-stats'],
    queryFn: async () => (await api.get('/admin-requests/stats')).data as Stats,
  });

  const { data: demoRequests = [], isLoading: loadingDemo } = useQuery({
    queryKey: ['admin-demo-requests'],
    queryFn: async () => (await api.get('/admin-demo-requests')).data as AdminDemoRequest[],
    enabled: tab === 'demo',
  });

  const processMutation = useMutation({
    mutationFn: async ({ id, decision }: { id: string; decision: Statut }) => {
      const res = await api.post(`/admin-requests/${id}/process`, null, {
        params: { decision, traiteurId: user?.id },
      });
      return res.data as AdminRequest;
    },
    onSuccess: () => {
      toast.success('Demande traitée');
      qc.invalidateQueries({ queryKey: ['admin-requests'] });
      qc.invalidateQueries({ queryKey: ['admin-requests-stats'] });
    },
    onError: (e) => toast.error(getErrorMessage(e)),
  });

  const createMutation = useMutation({
    mutationFn: async (payload: CreateAdminRequest) => {
      const res = await api.post('/admin-requests', payload);
      return res.data as AdminRequest;
    },
    onSuccess: () => {
      toast.success('Demande créée');
      setShowCreate(false);
      setForm({ typeDemande: 'BAPTEME', motif: '', details: '' });
      qc.invalidateQueries({ queryKey: ['admin-requests'] });
      qc.invalidateQueries({ queryKey: ['admin-requests-stats'] });
    },
    onError: (e) => toast.error(getErrorMessage(e)),
  });

  const deleteMutation = useMutation({
    mutationFn: async (id: string) => {
      await api.delete(`/admin-requests/${id}`);
    },
    onSuccess: () => {
      toast.success('Demande supprimée');
      qc.invalidateQueries({ queryKey: ['admin-requests'] });
      qc.invalidateQueries({ queryKey: ['admin-requests-stats'] });
    },
    onError: (e) => toast.error(getErrorMessage(e)),
  });

  const cardStats = stats
    ? [
        { label: 'Total', value: stats.total, icon: BarChart3 },
        { label: 'En examen', value: stats.enExamen, icon: Loader2 },
        { label: 'Approuvées', value: stats.approuvees, icon: CheckCircle2 },
        { label: 'Rejetées', value: stats.rejetees, icon: XCircle },
      ]
    : [];

  return (
    <div className="page-container">
      <div className="page-header">
        <div className="p-3 rounded-xl bg-gradient-to-br from-indigo-500 to-purple-700 text-white shadow-lg">
          <Shield className="w-6 h-6" />
        </div>
        <div>
          <h1 className="page-title">Demandes administratives</h1>
          <p className="page-subtitle">Gestion des demandes de démos et d'adhésion</p>
        </div>
        <div className="ml-auto">
          <button
            onClick={() => setShowCreate(true)}
            className="btn-primary btn-sm flex items-center gap-1.5"
          >
            <Plus className="w-4 h-4" /> Nouvelle demande
          </button>
        </div>
      </div>

      {cardStats.length > 0 && (
        <div className="grid grid-cols-2 md:grid-cols-4 gap-4 mb-6">
          {cardStats.map(({ label, value, icon: Icon }) => (
            <div key={label} className="stat-card">
              <Icon className="w-5 h-5 text-primary-500 opacity-80" />
              <p className="stat-value">{value}</p>
              <p className="stat-label">{label}</p>
            </div>
          ))}
        </div>
      )}

      <div className="flex gap-2 mb-6">
        <button onClick={() => setTab('requests')}
          className={`btn-sm px-4 py-2 rounded-lg ${tab === 'requests' ? 'bg-gradient-to-r from-indigo-500 to-purple-600 text-white shadow-md' : 'glass-card hover:shadow-md'}`}>
          Demandes ({requests.length})
        </button>
        <button onClick={() => setTab('demo')}
          className={`btn-sm px-4 py-2 rounded-lg ${tab === 'demo' ? 'bg-gradient-to-r from-indigo-500 to-purple-600 text-white shadow-md' : 'glass-card hover:shadow-md'}`}>
          Démos ({demoRequests.length})
        </button>
      </div>

      {tab === 'requests' ? (
        isLoading ? (
          <div className="flex justify-center py-12"><Loader2 className="w-8 h-8 animate-spin text-primary-500" /></div>
        ) : requests.length === 0 ? (
          <div className="glass-card p-10 text-center text-gray-500">Aucune demande administrative</div>
        ) : (
          <div className="space-y-3">
            {requests.map((r) => (
              <div key={r.id} className="glass-card p-5">
                <div className="flex items-start justify-between">
                  <div className="flex-1">
                    <div className="flex items-center gap-2 mb-1">
                      <span className={`px-2 py-0.5 rounded-full text-xs font-medium ${STATUS_STYLE[r.statut] ?? 'text-gray-400 bg-gray-500/20'}`}>{STATUS_LABELS[r.statut] ?? r.statut}</span>
                      <span className="text-xs text-gray-400">{TYPE_LABELS[r.typeDemande] ?? r.typeDemande}</span>
                    </div>
                    <p className="text-sm font-medium text-gray-800 dark:text-gray-200">{r.motif}</p>
                    {r.details && <p className="text-xs text-gray-500 mt-1">{r.details}</p>}
                    <p className="text-[11px] text-gray-500 mt-1">{new Date(r.soumiseLe).toLocaleDateString('fr-FR')}</p>
                  </div>
                  <div className="flex items-center gap-2 ml-4">
                    {r.statut === 'SOUMISE' || r.statut === 'EN_EXAMEN' ? (
                      <>
                        <button onClick={() => processMutation.mutate({ id: r.id, decision: 'APPROUVEE' })}
                          disabled={processMutation.isPending}
                          className="btn-sm px-3 py-1.5 rounded-lg bg-green-600 text-white text-xs hover:bg-green-700 flex items-center gap-1">
                          {processMutation.isPending ? <Loader2 className="w-3 h-3 animate-spin" /> : <CheckCircle2 className="w-3 h-3" />} Approuver
                        </button>
                        <button onClick={() => processMutation.mutate({ id: r.id, decision: 'REJETEE' })}
                          disabled={processMutation.isPending}
                          className="btn-sm px-3 py-1.5 rounded-lg bg-red-600 text-white text-xs hover:bg-red-700 flex items-center gap-1">
                          <XCircle className="w-3 h-3" /> Rejeter
                        </button>
                      </>
                    ) : null}
                    <button
                      onClick={() => { if (window.confirm('Supprimer cette demande ?')) deleteMutation.mutate(r.id); }}
                      disabled={deleteMutation.isPending}
                      className="p-2 rounded-lg text-gray-400 hover:text-red-600 hover:bg-red-50 dark:hover:bg-red-900/20 transition-colors"
                      title="Supprimer"
                    >
                      <Trash2 className="w-4 h-4" />
                    </button>
                  </div>
                </div>
              </div>
            ))}
          </div>
        )
      ) : (
        loadingDemo ? (
          <div className="flex justify-center py-12"><Loader2 className="w-8 h-8 animate-spin text-primary-500" /></div>
        ) : demoRequests.length === 0 ? (
          <div className="glass-card p-10 text-center text-gray-500">Aucune demande de démo</div>
        ) : (
          <div className="space-y-3">
            {demoRequests.map((d) => (
              <div key={d.id} className="glass-card p-5">
                <div className="flex items-center justify-between">
                  <div>
                    <div className="flex items-center gap-2 mb-1">
                      <span className="text-sm font-medium text-gray-800 dark:text-gray-200">{d.churchName}</span>
                    </div>
                    <p className="text-xs text-gray-500">{d.contactName} — {d.contactEmail}</p>
                    <p className="text-[11px] text-gray-500 mt-1">{new Date(d.createdAt).toLocaleDateString('fr-FR')}</p>
                  </div>
                  <span className={`px-2 py-0.5 rounded-full text-xs font-medium ${STATUS_STYLE[d.status] ?? 'text-gray-400 bg-gray-500/20'}`}>{d.status}</span>
                </div>
              </div>
            ))}
          </div>
        )
      )}

      {showCreate && (
        <div className="modal-overlay" onClick={() => setShowCreate(false)}>
          <div className="modal-content max-w-md" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <h3 className="text-base font-bold text-gray-900 dark:text-gray-100">Nouvelle demande</h3>
              <button onClick={() => setShowCreate(false)} className="p-1.5 rounded-lg hover:bg-gray-100 dark:hover:bg-gray-800">
                <X className="w-5 h-5 text-gray-400" />
              </button>
            </div>
            <div className="modal-body space-y-3">
              <div>
                <label className="block text-xs font-medium text-gray-600 dark:text-gray-400 mb-1">Type</label>
                <select
                  value={form.typeDemande}
                  onChange={(e) => setForm((f) => ({ ...f, typeDemande: e.target.value as TypeDemande }))}
                  className="input w-full"
                >
                  {(Object.keys(TYPE_LABELS) as TypeDemande[]).map((t) => (
                    <option key={t} value={t}>{TYPE_LABELS[t]}</option>
                  ))}
                </select>
              </div>
              <div>
                <label className="block text-xs font-medium text-gray-600 dark:text-gray-400 mb-1">Motif</label>
                <input
                  type="text"
                  value={form.motif}
                  onChange={(e) => setForm((f) => ({ ...f, motif: e.target.value }))}
                  className="input w-full"
                  placeholder="Motif de la demande"
                />
              </div>
              <div>
                <label className="block text-xs font-medium text-gray-600 dark:text-gray-400 mb-1">Détails</label>
                <textarea
                  value={form.details}
                  onChange={(e) => setForm((f) => ({ ...f, details: e.target.value }))}
                  className="input w-full"
                  rows={3}
                  placeholder="Détails (facultatif)"
                />
              </div>
            </div>
            <div className="modal-footer flex gap-2">
              <button className="btn-ghost btn-sm flex-1" onClick={() => setShowCreate(false)}>Annuler</button>
              <button
                className="btn-primary btn-sm flex-1"
                disabled={createMutation.isPending || !form.motif.trim()}
                onClick={() => createMutation.mutate(form)}
              >
                {createMutation.isPending ? <Loader2 className="w-4 h-4 animate-spin" /> : <Plus className="w-4 h-4" />} Créer
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
