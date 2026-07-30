import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import api, { getErrorMessage } from '@/lib/api';
import DataTable from '@/components/shared/DataTable';
import type { User, PageResponse, Family } from '@/types';
import type { ColumnDef } from '@/types/table';
import { UserCog, Plus, Loader2, X, Sparkles, Shield, Mail, Key, User as UserIcon, ArrowUp, ArrowDown, History, Move, Trash2, RefreshCw, Users, BarChart3, Star } from 'lucide-react';
import toast from 'react-hot-toast';

const ROLE_LABELS: Record<string, string> = {
  ADMIN: 'Administrateur',
  PASTEUR: 'Pasteur',
  RESPONSABLE: 'Responsable',
  FAISEUR: 'Faiseur de disciples',
};

const ROLE_BADGES: Record<string, string> = {
  ADMIN: 'badge-info',
  PASTEUR: 'badge-info',
  RESPONSABLE: 'badge-warning',
  FAISEUR: 'badge-success',
};

export default function UsersPage() {
  const queryClient = useQueryClient();
  const [page, setPage] = useState(0);
  const [showModal, setShowModal] = useState(false);
  const [selectedUser, setSelectedUser] = useState<User | null>(null);
  const [actionModal, setActionModal] = useState<'' | 'promote' | 'demote' | 'transfer' | 'history' | 'hardDelete'>('');
  const [transferFamilleId, setTransferFamilleId] = useState('');
  const [transferAmes, setTransferAmes] = useState(false);
  const [demoteRole, setDemoteRole] = useState('RESPONSABLE');
  const [formData, setFormData] = useState({
    email: '',
    password: 'password123',
    firstName: '',
    lastName: '',
    role: 'FAISEUR',
  });

  const { data, isLoading } = useQuery({
    queryKey: ['users', page],
    queryFn: async () => {
      const res = await api.get(`/users?size=20&page=${page}`);
      return res.data as PageResponse<User>;
    },
  });

  const { data: workload } = useQuery({
    queryKey: ['users', 'workload'],
    queryFn: async () => {
      const res = await api.get('/users/faiseur-workload');
      return res.data as { faiseurId: string; faiseurName: string; familleId?: string; familleName?: string; soulCount: number }[];
    },
  });

  const userIds = data?.content.map(u => u.id).join(',');
  const { data: evalScores } = useQuery({
    queryKey: ['users', 'eval-scores', userIds],
    queryFn: async () => {
      if (!userIds) return {};
      const res = await api.get(`/users/evaluation-scores?userIds=${userIds}`);
      return res.data as Record<string, Record<string, { moyenne: number | null; total: number }>>;
    },
    enabled: !!userIds,
  });

  const { data: families } = useQuery({
    queryKey: ['families', 'all'],
    queryFn: async () => {
      const res = await api.get('/families?size=100');
      return res.data.content as Family[];
    },
  });

  const { data: userHistory } = useQuery({
    queryKey: ['users', selectedUser?.id, 'history'],
    queryFn: async () => {
      const res = await api.get(`/users/${selectedUser!.id}/faiseur-history`);
      return res.data as Record<string, any>;
    },
    enabled: !!selectedUser && actionModal === 'history',
  });

  const createMutation = useMutation({
    mutationFn: async () => {
      await api.post('/users', formData);
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['users'] });
      toast.success('Compte créé avec succès');
      setShowModal(false);
      setFormData({ email: '', password: 'password123', firstName: '', lastName: '', role: 'FAISEUR' });
    },
    onError: (err) => toast.error(getErrorMessage(err)),
  });

  const promoteMutation = useMutation({
    mutationFn: async (id: string) => {
      await api.patch(`/users/${id}/promote-faiseur`);
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['users'] });
      toast.success('Utilisateur promu Faiseur de disciples');
      setActionModal('');
      setSelectedUser(null);
    },
    onError: (err) => toast.error(getErrorMessage(err)),
  });

  const demoteMutation = useMutation({
    mutationFn: async ({ id, newRole }: { id: string; newRole: string }) => {
      await api.patch(`/users/${id}/demote`, { newRole });
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['users'] });
      toast.success('Faiseur rétrogradé');
      setActionModal('');
      setSelectedUser(null);
    },
    onError: (err) => toast.error(getErrorMessage(err)),
  });

  const transferMutation = useMutation({
    mutationFn: async ({ id, nouvelleFamilleId, transfererAmes }: { id: string; nouvelleFamilleId: string; transfererAmes: boolean }) => {
      await api.patch(`/users/${id}/transfer`, { nouvelleFamilleId, transfererAmes });
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['users'] });
      toast.success('Faiseur transféré');
      setActionModal('');
      setSelectedUser(null);
    },
    onError: (err) => toast.error(getErrorMessage(err)),
  });

  const hardDeleteMutation = useMutation({
    mutationFn: async (id: string) => {
      await api.delete(`/users/${id}/hard-delete`);
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['users'] });
      toast.success('Utilisateur supprimé définitivement');
      setActionModal('');
      setSelectedUser(null);
    },
    onError: (err) => toast.error(getErrorMessage(err)),
  });

  const restoreMutation = useMutation({
    mutationFn: async (id: string) => {
      await api.patch(`/users/${id}/restore`);
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['users'] });
      toast.success('Utilisateur restauré');
    },
    onError: (err) => toast.error(getErrorMessage(err)),
  });

  const columns: ColumnDef<User>[] = [
    {
      header: 'Utilisateur',
      cell: (user) => (
        <div className="flex items-center gap-3">
          <div className="w-8 h-8 rounded-xl bg-gradient-to-br from-primary-400 to-primary-600 flex items-center justify-center shadow-sm">
            <span className="text-xs font-bold text-white">
              {user.firstName?.[0]}{user.lastName?.[0]}
            </span>
          </div>
          <div>
            <p className="text-sm font-medium text-gray-900 dark:text-gray-100">
              {user.firstName} {user.lastName}
            </p>
            <p className="text-xs text-gray-400">{user.email}</p>
          </div>
        </div>
      ),
    },
    {
      header: 'Rôle',
      cell: (user) => (
        <div className="flex flex-wrap items-center gap-1">
          <span className={ROLE_BADGES[user.role] || 'badge-gray'}>
            {ROLE_LABELS[user.role] || user.role}
          </span>
          {user.estChefDeFamille && (
            <span className="badge text-[10px] bg-gold-100 dark:bg-gold-900/30 text-gold-700 dark:text-gold-400 border border-gold-200/50 dark:border-gold-700/30">
              Chef
            </span>
          )}
        </div>
      ),
    },
    {
      header: 'Statut',
      cell: (user) => (
        <div className="flex items-center gap-1.5">
          <span className={`w-2 h-2 rounded-full ${user.statut === 'ACTIVE' ? 'bg-green-500 shadow-[0_0_6px_rgba(34,197,94,0.5)]' : 'bg-gray-400'}`} />
          <span className={user.statut === 'ACTIVE' ? 'badge-success' : 'badge-gray'}>
            {user.statut === 'ACTIVE' ? 'Actif' : 'Inactif'}
          </span>
        </div>
      ),
    },
    {
      header: 'Évaluation',
      cell: (user) => {
        const scores = evalScores?.[user.id];
        if (!scores) return <span className="text-xs text-gray-400">—</span>;
        const allNotes = Object.values(scores);
        const totalAvg = allNotes.reduce((acc, s) => acc + (s.moyenne || 0), 0) / Math.max(allNotes.length, 1);
        const totalCount = allNotes.reduce((acc, s) => acc + s.total, 0);
        const CATEGORIE_LABELS: Record<string, string> = {
          RESPONSABLE: 'Responsable',
          CHEF_FAMILLE: 'Chef de famille',
          FAISEUR: 'Faiseur',
        };
        return (
          <div className="group relative flex items-center gap-1.5">
            <div className="flex cursor-help">
              {[1,2,3,4,5].map(i => (
                <Star key={i} className={`w-3 h-3 ${i <= Math.round(totalAvg) ? 'fill-amber-400 text-amber-400' : 'text-gray-300 dark:text-gray-600'}`} />
              ))}
            </div>
            <span className="text-xs font-medium text-gray-500">{totalAvg > 0 ? totalAvg.toFixed(1) : ''}</span>
            {totalCount > 0 && <span className="text-[10px] text-gray-400">({totalCount})</span>}

            {/* Tooltip détail par catégorie */}
            <div className="absolute bottom-full left-1/2 -translate-x-1/2 mb-2 opacity-0 invisible group-hover:opacity-100 group-hover:visible transition-all duration-200 z-50 pointer-events-none">
              <div className="bg-gray-900 dark:bg-gray-700 text-white text-xs rounded-lg shadow-xl px-3 py-2.5 min-w-[180px]">
                <p className="font-semibold text-[11px] text-gray-300 mb-1.5 border-b border-gray-700 pb-1">Détail par catégorie</p>
                <div className="space-y-1">
                  {Object.entries(scores).map(([cat, s]) => (
                    <div key={cat} className="flex items-center justify-between gap-3">
                      <span className="text-gray-300">{CATEGORIE_LABELS[cat] || cat}</span>
                      <span className="font-medium">
                        {s.moyenne !== null && s.moyenne > 0
                          ? <>{s.moyenne.toFixed(1)} <span className="text-[10px] text-gray-400">/5</span></>
                          : <span className="text-gray-500">—</span>
                        }
                        <span className="text-[10px] text-gray-400 ml-1">({s.total})</span>
                      </span>
                    </div>
                  ))}
                </div>
              </div>
              {/* Arrow */}
              <div className="absolute top-full left-1/2 -translate-x-1/2 w-2 h-2 bg-gray-900 dark:bg-gray-700 rotate-45 -mt-1" />
            </div>
          </div>
        );
      },
    },
    {
      header: 'Actions',
      cell: (user) => (
        <div className="flex items-center gap-1">
          {user.role === 'FAISEUR' && (
            <>
              <button
                onClick={() => { setSelectedUser(user); setActionModal('demote'); }}
                className="p-1.5 rounded-lg hover:bg-amber-100 dark:hover:bg-amber-900/20 text-amber-600 hover:text-amber-700 transition-colors"
                title="Rétrograder"
              >
                <ArrowDown className="w-3.5 h-3.5" />
              </button>
              <button
                onClick={() => { setSelectedUser(user); setTransferFamilleId(''); setTransferAmes(false); setActionModal('transfer'); }}
                className="p-1.5 rounded-lg hover:bg-blue-100 dark:hover:bg-blue-900/20 text-blue-600 hover:text-blue-700 transition-colors"
                title="Transférer"
              >
                <Move className="w-3.5 h-3.5" />
              </button>
              <button
                onClick={() => { setSelectedUser(user); setActionModal('history'); }}
                className="p-1.5 rounded-lg hover:bg-purple-100 dark:hover:bg-purple-900/20 text-purple-600 hover:text-purple-700 transition-colors"
                title="Historique"
              >
                <History className="w-3.5 h-3.5" />
              </button>
            </>
          )}
          {user.role !== 'FAISEUR' && user.role !== 'ADMIN' && (
            <button
              onClick={() => { setSelectedUser(user); setActionModal('promote'); }}
              className="p-1.5 rounded-lg hover:bg-green-100 dark:hover:bg-green-900/20 text-green-600 hover:text-green-700 transition-colors"
              title="Promouvoir Faiseur"
            >
              <ArrowUp className="w-3.5 h-3.5" />
            </button>
          )}
          <button
            onClick={() => { setSelectedUser(user); setActionModal('hardDelete'); }}
            className="p-1.5 rounded-lg hover:bg-red-100 dark:hover:bg-red-900/20 text-red-600 hover:text-red-700 transition-colors"
            title="Supprimer définitivement (RGPD)"
          >
            <Trash2 className="w-3.5 h-3.5" />
          </button>
        </div>
      ),
    },
  ];

  return (
    <div className="page-container">
      <div className="page-header">
        <div className="animate-fade-in">
          <div className="flex items-center gap-2 mb-1">
            <UserCog className="w-5 h-5 text-primary-500" />
            <h1 className="page-title">Utilisateurs</h1>
          </div>
          <p className="page-subtitle">Gestion des comptes et des rôles</p>
        </div>
        <button onClick={() => setShowModal(true)} className="btn-primary btn-sm animate-scale-in">
          <Plus className="w-4 h-4" />
          Nouvel utilisateur
        </button>
      </div>

      {/* Workload section */}
      {workload && workload.length > 0 && (
        <div className="glass-card p-5 mb-6">
          <div className="flex items-center gap-2 mb-4">
            <BarChart3 className="w-4 h-4 text-primary-500" />
            <h3 className="text-sm font-semibold text-gray-900 dark:text-gray-100">Charge de travail des Faiseurs</h3>
          </div>
          <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-6 gap-3">
            {workload.map((w) => (
              <div key={w.faiseurId} className="p-3 rounded-xl bg-gradient-to-br from-gray-50 to-white dark:from-gray-800/50 dark:to-gray-800/30 border border-gray-100 dark:border-gray-700/50">
                <p className="text-xs text-gray-500 dark:text-gray-400 truncate" title={w.faiseurName}>{w.faiseurName}</p>
                <p className="text-lg font-bold text-gray-900 dark:text-gray-100 mt-1">{w.soulCount}</p>
                <p className="text-[10px] text-gray-400">âmes suivies</p>
                {w.familleName && <p className="text-[10px] text-gray-400 truncate">Famille: {w.familleName}</p>}
              </div>
            ))}
          </div>
        </div>
      )}

      <DataTable<User>
        columns={columns}
        data={data?.content || []}
        isLoading={isLoading}
        emptyMessage="Aucun utilisateur"
        emptyIcon={<UserCog className="w-16 h-16 text-gray-300 dark:text-gray-600" />}
      />

      {data && data.totalPages > 1 && (
        <div className="flex items-center justify-between mt-4">
          <p className="text-sm text-gray-500">{data.number + 1} / {data.totalPages}</p>
          <div className="flex gap-2">
            <button onClick={() => setPage(p => Math.max(0, p - 1))} disabled={data.first} className="btn-secondary btn-sm">← Précédent</button>
            <button onClick={() => setPage(p => p + 1)} disabled={data.last} className="btn-primary btn-sm">Suivant →</button>
          </div>
        </div>
      )}

      {/* Create user modal */}
      {showModal && (
        <div className="modal-overlay" onClick={() => setShowModal(false)}>
          <div className="modal-content max-w-md" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <div className="flex items-center gap-3">
                <div className="p-2 rounded-xl bg-gradient-to-br from-primary-500 to-primary-600 text-white shadow-sm">
                  <UserIcon className="w-5 h-5" />
                </div>
                <div>
                  <h3 className="text-lg font-semibold text-gray-900 dark:text-gray-100">Nouvel utilisateur</h3>
                  <p className="text-sm text-gray-500 dark:text-gray-400">Créer un compte pour un nouveau membre</p>
                </div>
              </div>
              <button onClick={() => setShowModal(false)} className="p-1.5 rounded-lg hover:bg-gray-100 dark:hover:bg-gray-800 transition-colors">
                <X className="w-5 h-5 text-gray-400" />
              </button>
            </div>

            <div className="modal-body space-y-4">
              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="label">Prénom</label>
                  <div className="relative">
                    <UserIcon className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400" />
                    <input className="input pl-10" placeholder="Jean" value={formData.firstName} onChange={(e) => setFormData({ ...formData, firstName: e.target.value })} />
                  </div>
                </div>
                <div>
                  <label className="label">Nom</label>
                  <input className="input" placeholder="Dupont" value={formData.lastName} onChange={(e) => setFormData({ ...formData, lastName: e.target.value })} />
                </div>
              </div>

              <div>
                <label className="label">Email</label>
                <div className="relative">
                  <Mail className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400" />
                  <input type="email" className="input pl-10" placeholder="jean.dupont@email.com" value={formData.email} onChange={(e) => setFormData({ ...formData, email: e.target.value })} />
                </div>
              </div>

              <div>
                <label className="label">Rôle</label>
                <div className="relative">
                  <Shield className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400 z-10" />
                  <select className="input pl-10" value={formData.role} onChange={(e) => setFormData({ ...formData, role: e.target.value })}>
                    <option value="FAISEUR">Faiseur de disciples</option>
                    <option value="RESPONSABLE">Responsable de département</option>
                    <option value="PASTEUR">Pasteur</option>
                    <option value="ADMIN">Administrateur</option>
                  </select>
                </div>
              </div>

              <div className="p-3 rounded-xl bg-amber-50/70 dark:bg-amber-900/10 border border-amber-200/50 dark:border-amber-800/30">
                <div className="flex items-start gap-2">
                  <Sparkles className="w-4 h-4 text-amber-500 mt-0.5 flex-shrink-0" />
                  <p className="text-xs text-amber-700 dark:text-amber-400">
                    Un email de bienvenue sera envoyé avec les instructions de connexion. Mot de passe temporaire : <code className="font-mono text-amber-800 dark:text-amber-300 bg-amber-100/50 dark:bg-amber-900/30 px-1 rounded">password123</code>
                  </p>
                </div>
              </div>
            </div>

            <div className="modal-footer">
              <button onClick={() => setShowModal(false)} className="btn-secondary btn-sm">Annuler</button>
              <button
                onClick={() => createMutation.mutate()}
                disabled={!formData.email || !formData.firstName || createMutation.isPending}
                className="btn-primary btn-sm"
              >
                {createMutation.isPending ? (
                  <Loader2 className="w-4 h-4 animate-spin" />
                ) : (
                  <Plus className="w-4 h-4" />
                )}
                Créer le compte
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Promote modal */}
      {actionModal === 'promote' && selectedUser && (
        <div className="modal-overlay" onClick={() => { setActionModal(''); setSelectedUser(null); }}>
          <div className="modal-content max-w-sm" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <h3 className="text-lg font-semibold text-gray-900 dark:text-gray-100">Promouvoir en Faiseur</h3>
              <button onClick={() => { setActionModal(''); setSelectedUser(null); }} className="p-1.5 rounded-lg hover:bg-gray-100 dark:hover:bg-gray-800">
                <X className="w-5 h-5 text-gray-400" />
              </button>
            </div>
            <div className="modal-body">
              <p className="text-sm text-gray-600 dark:text-gray-400">
                Promouvoir <strong>{selectedUser.firstName} {selectedUser.lastName}</strong> au rôle de Faiseur de disciples ?
              </p>
            </div>
            <div className="modal-footer">
              <button onClick={() => { setActionModal(''); setSelectedUser(null); }} className="btn-secondary btn-sm">Annuler</button>
              <button onClick={() => promoteMutation.mutate(selectedUser.id)} disabled={promoteMutation.isPending} className="btn-primary btn-sm">
                {promoteMutation.isPending ? <Loader2 className="w-4 h-4 animate-spin" /> : <ArrowUp className="w-4 h-4" />}
                Promouvoir
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Demote modal */}
      {actionModal === 'demote' && selectedUser && (
        <div className="modal-overlay" onClick={() => { setActionModal(''); setSelectedUser(null); }}>
          <div className="modal-content max-w-sm" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <h3 className="text-lg font-semibold text-gray-900 dark:text-gray-100">Rétrograder</h3>
              <button onClick={() => { setActionModal(''); setSelectedUser(null); }} className="p-1.5 rounded-lg hover:bg-gray-100 dark:hover:bg-gray-800">
                <X className="w-5 h-5 text-gray-400" />
              </button>
            </div>
            <div className="modal-body space-y-4">
              <p className="text-sm text-gray-600 dark:text-gray-400">
                Rétrograder <strong>{selectedUser.firstName} {selectedUser.lastName}</strong> vers un autre rôle :
              </p>
              <select className="input" value={demoteRole} onChange={(e) => setDemoteRole(e.target.value)}>
                <option value="RESPONSABLE">Responsable de département</option>
                <option value="FAISEUR">Faiseur de disciples</option>
              </select>
            </div>
            <div className="modal-footer">
              <button onClick={() => { setActionModal(''); setSelectedUser(null); }} className="btn-secondary btn-sm">Annuler</button>
              <button onClick={() => demoteMutation.mutate({ id: selectedUser.id, newRole: demoteRole })} disabled={demoteMutation.isPending} className="btn-primary btn-sm">
                {demoteMutation.isPending ? <Loader2 className="w-4 h-4 animate-spin" /> : <ArrowDown className="w-4 h-4" />}
                Rétrograder
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Transfer modal */}
      {actionModal === 'transfer' && selectedUser && (
        <div className="modal-overlay" onClick={() => { setActionModal(''); setSelectedUser(null); }}>
          <div className="modal-content max-w-sm" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <h3 className="text-lg font-semibold text-gray-900 dark:text-gray-100">Transférer le Faiseur</h3>
              <button onClick={() => { setActionModal(''); setSelectedUser(null); }} className="p-1.5 rounded-lg hover:bg-gray-100 dark:hover:bg-gray-800">
                <X className="w-5 h-5 text-gray-400" />
              </button>
            </div>
            <div className="modal-body space-y-4">
              <p className="text-sm text-gray-600 dark:text-gray-400">
                Transférer <strong>{selectedUser.firstName} {selectedUser.lastName}</strong> vers une nouvelle famille :
              </p>
              <select className="input" value={transferFamilleId} onChange={(e) => setTransferFamilleId(e.target.value)}>
                <option value="">Sélectionner une famille...</option>
                {(families || []).map((f) => (
                  <option key={f.id} value={f.id}>{f.nom}</option>
                ))}
              </select>
              <label className="flex items-center gap-2 text-sm text-gray-600 dark:text-gray-400">
                <input type="checkbox" checked={transferAmes} onChange={(e) => setTransferAmes(e.target.checked)} className="rounded" />
                Transférer également les âmes suivies
              </label>
            </div>
            <div className="modal-footer">
              <button onClick={() => { setActionModal(''); setSelectedUser(null); }} className="btn-secondary btn-sm">Annuler</button>
              <button
                onClick={() => transferMutation.mutate({ id: selectedUser.id, nouvelleFamilleId: transferFamilleId, transfererAmes: transferAmes })}
                disabled={!transferFamilleId || transferMutation.isPending}
                className="btn-primary btn-sm"
              >
                {transferMutation.isPending ? <Loader2 className="w-4 h-4 animate-spin" /> : <Move className="w-4 h-4" />}
                Transférer
              </button>
            </div>
          </div>
        </div>
      )}

      {/* History modal */}
      {actionModal === 'history' && selectedUser && (
        <div className="modal-overlay" onClick={() => { setActionModal(''); setSelectedUser(null); }}>
          <div className="modal-content max-w-lg max-h-[80vh] overflow-y-auto" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <h3 className="text-lg font-semibold text-gray-900 dark:text-gray-100">Historique - {selectedUser.firstName} {selectedUser.lastName}</h3>
              <button onClick={() => { setActionModal(''); setSelectedUser(null); }} className="p-1.5 rounded-lg hover:bg-gray-100 dark:hover:bg-gray-800">
                <X className="w-5 h-5 text-gray-400" />
              </button>
            </div>
            <div className="modal-body">
              {userHistory ? (
                <pre className="text-xs text-gray-600 dark:text-gray-400 whitespace-pre-wrap font-mono bg-gray-50 dark:bg-gray-900/50 p-4 rounded-xl">
                  {JSON.stringify(userHistory, null, 2)}
                </pre>
              ) : (
                <div className="flex justify-center py-8">
                  <Loader2 className="w-6 h-6 animate-spin text-primary-500" />
                </div>
              )}
            </div>
            <div className="modal-footer">
              <button onClick={() => { setActionModal(''); setSelectedUser(null); }} className="btn-secondary btn-sm">Fermer</button>
            </div>
          </div>
        </div>
      )}

      {/* Hard Delete modal */}
      {actionModal === 'hardDelete' && selectedUser && (
        <div className="modal-overlay" onClick={() => { setActionModal(''); setSelectedUser(null); }}>
          <div className="modal-content max-w-sm" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <h3 className="text-lg font-semibold text-red-600">Suppression définitive</h3>
              <button onClick={() => { setActionModal(''); setSelectedUser(null); }} className="p-1.5 rounded-lg hover:bg-gray-100 dark:hover:bg-gray-800">
                <X className="w-5 h-5 text-gray-400" />
              </button>
            </div>
            <div className="modal-body">
              <p className="text-sm text-gray-600 dark:text-gray-400">
                Êtes-vous sûr de vouloir supprimer définitivement <strong>{selectedUser.firstName} {selectedUser.lastName}</strong> ?
                Cette action est irréversible (RGPD).
              </p>
            </div>
            <div className="modal-footer">
              <button onClick={() => { setActionModal(''); setSelectedUser(null); }} className="btn-secondary btn-sm">Annuler</button>
              <button onClick={() => hardDeleteMutation.mutate(selectedUser.id)} disabled={hardDeleteMutation.isPending} className="btn-danger btn-sm">
                {hardDeleteMutation.isPending ? <Loader2 className="w-4 h-4 animate-spin" /> : <Trash2 className="w-4 h-4" />}
                Supprimer
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
