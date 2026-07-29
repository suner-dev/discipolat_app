import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import api, { getErrorMessage } from '@/lib/api';
import DataTable from '@/components/shared/DataTable';
import type { User, PageResponse } from '@/types';
import type { ColumnDef } from '@/types/table';
import { UserCog, Plus, Loader2, X, Sparkles, Shield, Mail, Key, User as UserIcon } from 'lucide-react';
import toast from 'react-hot-toast';

const ROLE_LABELS: Record<string, string> = {
  PASTEUR: 'Pasteur',
  RESPONSABLE: 'Responsable',
  FAISEUR: 'Faiseur de disciples',
};

const ROLE_BADGES: Record<string, string> = {
  PASTEUR: 'badge-info',
  RESPONSABLE: 'badge-warning',
  FAISEUR: 'badge-success',
};

export default function UsersPage() {
  const queryClient = useQueryClient();
  const [page, setPage] = useState(0);
  const [showModal, setShowModal] = useState(false);
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
              ⭐ Chef
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
    </div>
  );
}
