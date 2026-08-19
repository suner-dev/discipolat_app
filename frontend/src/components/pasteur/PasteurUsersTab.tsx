import { useState, useCallback } from 'react';
import { Link } from 'react-router-dom';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import api from '@/lib/api';
import type { User, PageResponse, UserRole } from '@/types';
import {
  UserCog, Plus, Search, Eye, Edit3, Trash2, ArrowLeft,
  Loader2, Shield, History, Filter, Mail, Phone, Calendar,
  Key, X, CheckCircle, XCircle,
} from 'lucide-react';
import toast from 'react-hot-toast';

const ROLE_COLORS: Record<string, string> = {
  ADMIN: 'bg-red-100 text-red-700 dark:bg-red-900/30 dark:text-red-400',
  PASTEUR: 'bg-purple-100 text-purple-700 dark:bg-purple-900/30 dark:text-purple-400',
  RESPONSABLE: 'bg-blue-100 text-blue-700 dark:bg-blue-900/30 dark:text-blue-400',
  CHEF_DE_FAMILLE: 'bg-green-100 text-green-700 dark:bg-green-900/30 dark:text-green-400',
  FAISEUR: 'bg-amber-100 text-amber-700 dark:bg-amber-900/30 dark:text-amber-400',
  MEMBRE: 'bg-gray-100 text-gray-700 dark:bg-gray-700 dark:text-gray-300',
};

type ViewMode = 'liste' | 'detail' | 'edit';

export default function PasteurUsersTab() {
  const queryClient = useQueryClient();
  const [page, setPage] = useState(0);
  const [search, setSearch] = useState('');
  const [roleFilter, setRoleFilter] = useState<UserRole | ''>('');
  const [showFilters, setShowFilters] = useState(false);
  const [view, setView] = useState<ViewMode>('liste');
  const [selectedUser, setSelectedUser] = useState<User | null>(null);
  const [showDeleteConfirm, setShowDeleteConfirm] = useState<string | null>(null);
  const [form, setForm] = useState({ firstName: '', lastName: '', email: '', phone: '', role: 'MEMBRE' as UserRole, situationFamiliale: '' });

  const { data, isLoading } = useQuery({
    queryKey: ['users', 'pasteur', page, search, roleFilter],
    queryFn: async () => {
      const params = new URLSearchParams({ size: '20', page: String(page) });
      if (search) params.set('search', search);
      if (roleFilter) params.set('role', roleFilter);
      const res = await api.get(`/users?${params}`);
      return res.data as PageResponse<User>;
    },
    enabled: view === 'liste',
  });

  const { data: userDetail, isLoading: detailLoading } = useQuery({
    queryKey: ['users', selectedUser?.id],
    queryFn: async () => {
      const res = await api.get(`/users/${selectedUser!.id}`);
      return res.data as User;
    },
    enabled: !!selectedUser && (view === 'detail' || view === 'edit'),
  });

  const deleteMutation = useMutation({
    mutationFn: async (id: string) => { await api.delete(`/users/${id}`); },
    onSuccess: () => { queryClient.invalidateQueries({ queryKey: ['users'] }); toast.success('Utilisateur supprimé'); setShowDeleteConfirm(null); },
    onError: () => toast.error('Erreur lors de la suppression'),
  });

  const updateMutation = useMutation({
    mutationFn: async ({ id, data }: { id: string; data: typeof form }) => {
      await api.put(`/users/${id}`, data);
    },
    onSuccess: () => { queryClient.invalidateQueries({ queryKey: ['users'] }); toast.success('Utilisateur mis à jour'); setView('liste'); setSelectedUser(null); },
    onError: () => toast.error('Erreur lors de la mise à jour'),
  });

  const handleEdit = useCallback((user: User) => {
    setForm({
      firstName: user.firstName || '',
      lastName: user.lastName || '',
      email: user.email || '',
      phone: user.phone || '',
      role: user.role || 'MEMBRE',
      situationFamiliale: user.situationFamiliale || '',
    });
    setSelectedUser(user);
    setView('edit');
  }, []);

  const roleLabel = (r: string) => {
    const labels: Record<string, string> = {
      ADMIN: 'Administrateur', PASTEUR: 'Pasteur', RESPONSABLE: 'Responsable',
      CHEF_DE_FAMILLE: 'Chef de famille', FAISEUR: 'Faiseur', MEMBRE: 'Membre',
    };
    return labels[r] || r;
  };

  // === VUE EDIT ===
  if (view === 'edit' && selectedUser) {
    return (
      <div className="animate-slide-up">
        <button onClick={() => { setView('liste'); setSelectedUser(null); }} className="flex items-center gap-2 text-sm text-primary-600 hover:text-primary-700 mb-4">
          <ArrowLeft className="w-4 h-4" /> Retour à la liste
        </button>
        <div className="glass-card p-6">
          <div className="flex items-center gap-4 mb-6">
            <div className="w-14 h-14 rounded-2xl bg-gradient-to-br from-primary-400 to-primary-600 flex items-center justify-center text-xl font-bold text-white">
              {form.firstName?.charAt(0)}{form.lastName?.charAt(0)}
            </div>
            <div>
              <h2 className="text-lg font-bold text-gray-900 dark:text-gray-100">Modifier l'utilisateur</h2>
              <p className="text-sm text-gray-500">{selectedUser.email}</p>
            </div>
          </div>
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div>
              <label className="label">Prénom *</label>
              <input className="input" value={form.firstName} onChange={e => setForm({ ...form, firstName: e.target.value })} />
            </div>
            <div>
              <label className="label">Nom *</label>
              <input className="input" value={form.lastName} onChange={e => setForm({ ...form, lastName: e.target.value })} />
            </div>
            <div>
              <label className="label">Email *</label>
              <input className="input" type="email" value={form.email} onChange={e => setForm({ ...form, email: e.target.value })} />
            </div>
            <div>
              <label className="label">Téléphone</label>
              <input className="input" value={form.phone} onChange={e => setForm({ ...form, phone: e.target.value })} placeholder="+243..." />
            </div>
            <div>
              <label className="label">Rôle</label>
              <select className="input" value={form.role} onChange={e => setForm({ ...form, role: e.target.value as UserRole })}>
                {Object.keys(ROLE_COLORS).map(r => <option key={r} value={r}>{roleLabel(r)}</option>)}
              </select>
            </div>
            <div>
              <label className="label">Situation familiale</label>
              <select className="input" value={form.situationFamiliale} onChange={e => setForm({ ...form, situationFamiliale: e.target.value })}>
                <option value="">Non spécifié</option>
                <option value="CELIBATAIRE">Célibataire</option>
                <option value="MARIE">Marié(e)</option>
                <option value="DIVORCE">Divorcé(e)</option>
                <option value="VEUF">Veuf/VEuve</option>
              </select>
            </div>
          </div>
          <div className="flex justify-end gap-3 mt-6 pt-4 border-t border-gray-200 dark:border-gray-700">
            <button onClick={() => { setView('liste'); setSelectedUser(null); }} className="btn-secondary">Annuler</button>
            <button onClick={() => { if (!form.firstName.trim() || !form.lastName.trim() || !form.email.trim()) { toast.error('Remplissez les champs obligatoires'); return; } updateMutation.mutate({ id: selectedUser.id, data: form }); }} disabled={updateMutation.isPending} className="btn-primary">
              {updateMutation.isPending && <Loader2 className="w-4 h-4 animate-spin" />} Enregistrer
            </button>
          </div>
        </div>
      </div>
    );
  }

  // === VUE DÉTAIL ===
  if (view === 'detail' && selectedUser) {
    const u = userDetail || selectedUser;
    return (
      <div className="animate-slide-up">
        <button onClick={() => { setView('liste'); setSelectedUser(null); }} className="flex items-center gap-2 text-sm text-primary-600 hover:text-primary-700 mb-4">
          <ArrowLeft className="w-4 h-4" /> Retour à la liste
        </button>
        <div className="glass-card p-6">
          <div className="flex items-start justify-between mb-6">
            <div className="flex items-center gap-4">
              <div className="w-16 h-16 rounded-2xl bg-gradient-to-br from-primary-400 to-primary-600 flex items-center justify-center text-2xl font-bold text-white">
                {u.firstName?.charAt(0)}{u.lastName?.charAt(0)}
              </div>
              <div>
                <h2 className="text-xl font-bold text-gray-900 dark:text-gray-100">{u.firstName} {u.lastName}</h2>
                <p className="text-sm text-gray-500">{u.email}</p>
                <div className="flex gap-2 mt-2">
                  {(u.roles || [u.role]).map(r => (
                    <span key={r} className={`badge text-[10px] ${ROLE_COLORS[r] || 'bg-gray-100 text-gray-700'}`}>{roleLabel(r)}</span>
                  ))}
                </div>
              </div>
            </div>
            <div className="flex gap-2">
              <button onClick={() => handleEdit(u)} className="btn-secondary btn-sm"><Edit3 className="w-4 h-4" /> Modifier</button>
              <Link to="/users" className="btn-primary btn-sm"><Eye className="w-4 h-4" /> Gestion complète</Link>
            </div>
          </div>

          <div className="grid grid-cols-2 md:grid-cols-4 gap-4 mb-6">
            <div className="p-3 rounded-xl bg-gray-50 dark:bg-gray-800/50 text-center">
              <Phone className="w-4 h-4 mx-auto text-gray-400 mb-1" />
              <p className="text-xs text-gray-400">Téléphone</p>
              <p className="font-semibold text-sm">{u.phone || '—'}</p>
            </div>
            <div className="p-3 rounded-xl bg-gray-50 dark:bg-gray-800/50 text-center">
              <Calendar className="w-4 h-4 mx-auto text-gray-400 mb-1" />
              <p className="text-xs text-gray-400">Naissance</p>
              <p className="font-semibold text-sm">{u.dateNaissance ? new Date(u.dateNaissance).toLocaleDateString('fr-FR') : '—'}</p>
            </div>
            <div className="p-3 rounded-xl bg-gray-50 dark:bg-gray-800/50 text-center">
              <Shield className="w-4 h-4 mx-auto text-gray-400 mb-1" />
              <p className="text-xs text-gray-400">Statut</p>
              <p className={`font-semibold text-sm flex items-center justify-center gap-1 ${u.statut === 'ACTIVE' ? 'text-green-500' : 'text-red-500'}`}>
                {u.statut === 'ACTIVE' ? <CheckCircle className="w-3.5 h-3.5" /> : <XCircle className="w-3.5 h-3.5" />}
                {u.statut}
              </p>
            </div>
            <div className="p-3 rounded-xl bg-gray-50 dark:bg-gray-800/50 text-center">
              <Key className="w-4 h-4 mx-auto text-gray-400 mb-1" />
              <p className="text-xs text-gray-400">2FA</p>
              <p className={`font-semibold text-sm ${u.twoFactorEnabled ? 'text-green-500' : 'text-gray-400'}`}>{u.twoFactorEnabled ? 'Activé' : 'Désactivé'}</p>
            </div>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div className="p-4 rounded-xl bg-gray-50 dark:bg-gray-800/50">
              <h4 className="text-xs font-semibold text-gray-500 mb-2 uppercase">Informations</h4>
              <div className="space-y-2 text-sm">
                <div className="flex justify-between"><span className="text-gray-500">Email</span><span>{u.email}</span></div>
                <div className="flex justify-between"><span className="text-gray-500">Téléphone</span><span>{u.phone || '—'}</span></div>
                <div className="flex justify-between"><span className="text-gray-500">Situation</span><span>{u.situationFamiliale || '—'}</span></div>
                <div className="flex justify-between"><span className="text-gray-500">Chef de famille</span><span>{u.estChefDeFamille ? 'Oui' : 'Non'}</span></div>
              </div>
            </div>
            <div className="p-4 rounded-xl bg-gray-50 dark:bg-gray-800/50">
              <h4 className="text-xs font-semibold text-gray-500 mb-2 uppercase">Dates</h4>
              <div className="space-y-2 text-sm">
                <div className="flex justify-between"><span className="text-gray-500">Créé le</span><span>{new Date(u.createdAt).toLocaleDateString('fr-FR')}</span></div>
                <div className="flex justify-between"><span className="text-gray-500">Modifié le</span><span>{new Date(u.updatedAt).toLocaleDateString('fr-FR')}</span></div>
              </div>
            </div>
          </div>
        </div>
      </div>
    );
  }

  // === VUE LISTE ===
  return (
    <div className="animate-slide-up">
      <div className="flex items-center justify-between mb-4">
        <div className="flex items-center gap-2">
          <UserCog className="w-5 h-5 text-blue-500" />
          <h2 className="text-lg font-semibold text-gray-900 dark:text-gray-100">Utilisateurs</h2>
          {data && <span className="text-xs text-gray-400">({data.totalElements} résultats)</span>}
        </div>
        <div className="flex gap-2">
          <button onClick={() => setShowFilters(!showFilters)} className={`btn-secondary btn-sm ${showFilters ? 'bg-primary-50 dark:bg-primary-900/20 border-primary-300' : ''}`}>
            <Filter className="w-4 h-4" /> Filtres
          </button>
          <Link to="/users" className="btn-primary btn-sm"><Plus className="w-4 h-4" /> Gérer les utilisateurs</Link>
        </div>
      </div>

      <div className="glass-card p-4 mb-4">
        <div className="flex flex-col sm:flex-row gap-3">
          <div className="flex-1 relative">
            <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400" />
            <input type="text" placeholder="Rechercher par nom, email..." value={search} onChange={e => { setSearch(e.target.value); setPage(0); }} className="input pl-10" />
          </div>
        </div>
        {showFilters && (
          <div className="flex flex-wrap gap-3 mt-4 pt-4 border-t border-white/20">
            <div className="flex items-center gap-2">
              <span className="text-xs text-gray-400 font-medium">Rôle</span>
              <select value={roleFilter} onChange={e => { setRoleFilter(e.target.value as UserRole | ''); setPage(0); }} className="input w-auto text-sm">
                <option value="">Tous</option>
                <option value="ADMIN">Administrateur</option>
                <option value="PASTEUR">Pasteur</option>
                <option value="RESPONSABLE">Responsable</option>
                <option value="CHEF_DE_FAMILLE">Chef de famille</option>
                <option value="FAISEUR">Faiseur</option>
                <option value="MEMBRE">Membre</option>
              </select>
            </div>
          </div>
        )}
      </div>

      {isLoading ? (
        <div className="space-y-3">{[...Array(5)].map((_, i) => <div key={i} className="glass-card p-4"><div className="skeleton h-12 w-full rounded-xl" /></div>)}</div>
      ) : (
        <div className="glass-card overflow-hidden">
          <div className="overflow-x-auto">
            <table className="table w-full">
              <thead>
                <tr>
                  <th>Nom</th>
                  <th>Email</th>
                  <th>Rôle</th>
                  <th>Statut</th>
                  <th>2FA</th>
                  <th>Créé le</th>
                  <th className="text-right">Actions</th>
                </tr>
              </thead>
              <tbody>
                {(data?.content || []).map(u => (
                  <tr key={u.id} className="hover:bg-white/40 dark:hover:bg-gray-800/20 transition-colors">
                    <td>
                      <button onClick={() => { setSelectedUser(u); setView('detail'); }} className="flex items-center gap-2 text-primary-600 hover:text-primary-700 font-medium hover:underline">
                        <div className="w-7 h-7 rounded-lg bg-gradient-to-br from-primary-400 to-primary-600 flex items-center justify-center text-[10px] font-bold text-white">
                          {u.firstName?.charAt(0)}{u.lastName?.charAt(0)}
                        </div>
                        {u.firstName} {u.lastName}
                      </button>
                    </td>
                    <td className="text-sm text-gray-500">{u.email}</td>
                    <td><span className={`badge text-[10px] ${ROLE_COLORS[u.role] || 'bg-gray-100 text-gray-700'}`}>{roleLabel(u.role)}</span></td>
                    <td><span className={`badge text-[10px] ${u.statut === 'ACTIVE' ? 'bg-green-100 text-green-700 dark:bg-green-900/30 dark:text-green-400' : 'bg-red-100 text-red-700 dark:bg-red-900/30 dark:text-red-400'}`}>{u.statut}</span></td>
                    <td className="text-sm">{u.twoFactorEnabled ? '✅' : '—'}</td>
                    <td className="text-sm text-gray-500">{new Date(u.createdAt).toLocaleDateString('fr-FR')}</td>
                    <td className="text-right">
                      <div className="flex items-center justify-end gap-1">
                        <button onClick={() => { setSelectedUser(u); setView('detail'); }} className="p-1.5 rounded-lg hover:bg-gray-100 dark:hover:bg-gray-700" title="Voir">
                          <Eye className="w-3.5 h-3.5 text-gray-500" />
                        </button>
                        <button onClick={() => handleEdit(u)} className="p-1.5 rounded-lg hover:bg-gray-100 dark:hover:bg-gray-700" title="Modifier">
                          <Edit3 className="w-3.5 h-3.5 text-gray-500" />
                        </button>
                        <button onClick={() => setShowDeleteConfirm(u.id)} className="p-1.5 rounded-lg hover:bg-red-50 dark:hover:bg-red-900/20" title="Supprimer">
                          <Trash2 className="w-3.5 h-3.5 text-red-400" />
                        </button>
                      </div>
                    </td>
                  </tr>
                ))}
                {(data?.content || []).length === 0 && <tr><td colSpan={7} className="py-12 text-center text-gray-400">Aucun utilisateur trouvé</td></tr>}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {data && data.totalPages > 1 && (
        <div className="flex items-center justify-between mt-4">
          <p className="text-sm text-gray-500">{data.number * data.size + 1} à {Math.min((data.number + 1) * data.size, data.totalElements)} sur {data.totalElements}</p>
          <div className="flex gap-2">
            <button onClick={() => setPage(p => Math.max(0, p - 1))} disabled={data.first} className="btn-secondary btn-sm">← Précédent</button>
            <button onClick={() => setPage(p => p + 1)} disabled={data.last} className="btn-primary btn-sm">Suivant →</button>
          </div>
        </div>
      )}

      {showDeleteConfirm && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/50" onClick={() => setShowDeleteConfirm(null)}>
          <div className="bg-white dark:bg-gray-800 rounded-xl p-6 max-w-sm w-full animate-slide-up" onClick={e => e.stopPropagation()}>
            <h3 className="text-lg font-semibold mb-2">Supprimer cet utilisateur ?</h3>
            <p className="text-sm text-gray-500 mb-4">Cette action est irréversible.</p>
            <div className="flex justify-end gap-3">
              <button onClick={() => setShowDeleteConfirm(null)} className="btn-secondary">Annuler</button>
              <button onClick={() => deleteMutation.mutate(showDeleteConfirm)} className="btn-primary bg-red-600 hover:bg-red-700"><Trash2 className="w-4 h-4" /> Supprimer</button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
