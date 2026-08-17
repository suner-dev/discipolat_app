import { useState, useRef } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import api, { getErrorMessage } from '@/lib/api';
import DataTable from '@/components/shared/DataTable';
import { useAuth } from '@/contexts/AuthContext';
import type { User, PageResponse, Family, TransferRequest } from '@/types';
import type { ColumnDef } from '@/types/table';
import { UserCog, Plus, Loader2, X, Sparkles, Shield, Mail, Key, User as UserIcon, ArrowUp, ArrowDown, History, Move, Trash2, RefreshCw, Users, BarChart3, Star, ClipboardList, Heart, UserX, UserRound } from 'lucide-react';
import toast from 'react-hot-toast';
import { useCustomFieldForm } from '@/hooks/useCustomFieldForm';
import CustomFieldRenderer from '@/components/shared/CustomFieldRenderer';
import { useDictionaries } from '@/hooks/useDictionaries';
import { UserDetailModal } from '@/components/users/UserDetailModal';

/** Repli (dictionnaires indisponibles) — les valeurs réelles viennent de la base. */
const ROLE_FALLBACK: Record<string, string> = {
  ADMIN: 'Administrateur',
  PASTEUR: 'Pasteur',
  RESPONSABLE: 'Responsable',
  CHEF_DE_FAMILLE: 'Chef de famille',
  FAISEUR: 'Faiseur de disciples',
  MEMBRE: 'Membre',
};

const ROLE_BADGES: Record<string, string> = {
  ADMIN: 'badge-info',
  PASTEUR: 'badge-info',
  RESPONSABLE: 'badge-warning',
  CHEF_DE_FAMILLE: 'badge-warning',
  FAISEUR: 'badge-success',
  MEMBRE: 'badge-gray',
};

const CHARGE_FALLBACK: Record<string, string> = {
  LEGER: 'Léger',
  NORMAL: 'Normal',
  SURCHARGE: 'Surchargé',
  'SURCHARGÉ': 'Surchargé',
};

const CHARGE_STYLES: Record<string, string> = {
  LEGER: 'bg-green-100/80 dark:bg-green-900/30 text-green-700 dark:text-green-400 border-green-200/60 dark:border-green-700/40',
  NORMAL: 'bg-blue-100/80 dark:bg-blue-900/30 text-blue-700 dark:text-blue-300 border-blue-200/60 dark:border-blue-700/40',
  'SURCHARGÉ': 'bg-red-100/80 dark:bg-red-900/30 text-red-700 dark:text-red-300 border-red-200/60 dark:border-red-700/40',
};

export default function UsersPage() {
  const queryClient = useQueryClient();
  const { user } = useAuth();
  const dictionaries = useDictionaries();
  const isResponsable = user?.activeRole === 'RESPONSABLE';
  const [page, setPage] = useState(0);
  const [showModal, setShowModal] = useState(false);
  const [selectedUser, setSelectedUser] = useState<User | null>(null);
  const [actionModal, setActionModal] = useState<'' | 'detail' | 'promote' | 'demote' | 'transfer' | 'history' | 'hardDelete'>('');
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
      return res.data as { faiseurId: string; faiseurName: string; familleId?: string; familleName?: string; soulCount: number; charge?: string }[];
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

  // Champs personnalisés configurés par l'administration (type USER).
  // La requête n'est activée qu'à l'ouverture de la modale de création.
  const customFields = useCustomFieldForm('USER', { enabled: showModal });
  // Résultat de la sauvegarde des champs (non fatale si échec secondaire).
  const fieldsSavedRef = useRef(true);

  const createMutation = useMutation({
    mutationFn: async () => {
      const res = await api.post('/users', formData);
      const created = res.data as User;
      // Sauvegarde des champs personnalisés après création du compte.
      fieldsSavedRef.current = await customFields.save(created.id);
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['users'] });
      if (fieldsSavedRef.current) {
        toast.success('Compte créé avec succès');
      } else {
        toast.error("Compte créé, mais les champs personnalisés n'ont pas pu être enregistrés");
      }
      setShowModal(false);
      customFields.reset();
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
      const res = await api.patch(`/users/${id}/transfer`, { nouvelleFamilleId, transfererAmes });
      return res.data as TransferRequest;
    },
    onSuccess: (data) => {
      queryClient.invalidateQueries({ queryKey: ['users'] });
      if (data.statut === 'EXECUTE') {
        toast.success('Faiseur transféré');
      } else {
        toast.success('Demande de transfert soumise pour validation');
      }
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
            {dictionaries.label('USER_ROLE', user.role) || ROLE_FALLBACK[user.role] || user.role}
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
        const CATEGORIE_FALLBACK: Record<string, string> = {
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
                      <span className="text-gray-300">{dictionaries.label('EVALUATION_CATEGORIE', cat) || CATEGORIE_FALLBACK[cat] || cat}</span>
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
          <button
            onClick={() => { setSelectedUser(user); setActionModal('detail'); }}
            className="p-1.5 rounded-lg hover:bg-primary-100 dark:hover:bg-primary-900/20 text-primary-600 hover:text-primary-700 transition-colors"
            title="Voir la fiche complète"
          >
            <UserRound className="w-3.5 h-3.5" />
          </button>
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
        <button
          onClick={() => { customFields.reset(); setShowModal(true); }}
          className="btn-primary btn-sm animate-scale-in"
        >
          <Plus className="w-4 h-4" />
          Nouvel utilisateur
        </button>
      </div>

      {/* Workload section — scopée par rôle actif côté serveur
          (responsable : faiseurs de ses départements ; super-utilisateurs : tous les faiseurs) */}
      {workload && workload.length > 0 && (
        <div className="glass-card p-5 mb-6">
          <div className="flex items-center gap-2 mb-1">
            <BarChart3 className="w-4 h-4 text-primary-500" />
            <h3 className="text-sm font-semibold text-gray-900 dark:text-gray-100">
              {isResponsable ? 'Charge de travail de mon département' : 'Charge de travail des Faiseurs'}
            </h3>
          </div>
          <p className="text-xs text-gray-400 mb-4">
            {isResponsable
              ? 'Faiseurs des membres de vos départements'
              : 'Répartition des disciples suivis par faiseur'}
          </p>
          <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-6 gap-3">
            {workload.map((w) => (
              <div key={w.faiseurId} className="p-3 rounded-xl bg-gradient-to-br from-gray-50 to-white dark:from-gray-800/50 dark:to-gray-800/30 border border-gray-100 dark:border-gray-700/50">
                <p className="text-xs text-gray-500 dark:text-gray-400 truncate" title={w.faiseurName}>{w.faiseurName}</p>
                <p className="text-lg font-bold text-gray-900 dark:text-gray-100 mt-1">{w.soulCount}</p>
                <p className="text-[10px] text-gray-400">âmes suivies</p>
                {w.charge && (
                  <span className={`inline-flex items-center px-1.5 py-0.5 rounded-full text-[9px] font-semibold border mt-1 ${CHARGE_STYLES[w.charge] || CHARGE_STYLES.NORMAL}`}>
                    {dictionaries.label('USER_CHARGE', w.charge) || CHARGE_FALLBACK[w.charge] || w.charge}
                  </span>
                )}
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

            <div className="modal-body space-y-4 max-h-[65vh] overflow-y-auto">
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
                    {(dictionaries.options('USER_ROLE').length > 0
                      // Rôles créables uniquement (CHEF_DE_FAMILLE est un rôle dérivé,
                      // attribué via la gestion des familles — pas à la création).
                      ? dictionaries.options('USER_ROLE').filter((o) => ROLE_FALLBACK[o.code])
                      : Object.entries(ROLE_FALLBACK).map(([value, label]) => ({ code: value, label }))
                    ).map((o) => (
                      <option key={o.code} value={o.code}>{o.label}</option>
                    ))}
                  </select>
                </div>
              </div>

              {/* Champs personnalisés (configurables par l'admin) */}
              {customFields.definitions.length > 0 && (
                <div className="space-y-3 p-3.5 rounded-xl bg-white/50 dark:bg-gray-800/30 border border-gray-200/60 dark:border-gray-700/60">
                  <div className="flex items-center gap-2">
                    <ClipboardList className="w-4 h-4 text-primary-500" />
                    <p className="text-xs font-semibold text-gray-700 dark:text-gray-300">Informations complémentaires</p>
                  </div>
                  <CustomFieldRenderer
                    definitions={customFields.definitions}
                    values={customFields.values}
                    onChange={customFields.setValue}
                    readOnlyFieldIds={customFields.readOnlyFieldIds}
                  />
                  {customFields.missingRequired.length > 0 && (
                    <p className="text-[11px] text-red-500">
                      Requis : {customFields.missingRequired.join(', ')}
                    </p>
                  )}
                </div>
              )}

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
                onClick={() => {
                  if (customFields.missingRequired.length > 0) {
                    toast.error(`Champs requis : ${customFields.missingRequired.join(', ')}`);
                    return;
                  }
                  createMutation.mutate();
                }}
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

      {/* User detail modal — fiche complète + évaluations */}
      {actionModal === 'detail' && selectedUser && (
        <UserDetailModal
          userId={selectedUser.id}
          onClose={() => { setActionModal(''); setSelectedUser(null); }}
        />
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
          <div className="modal-content max-w-lg max-h-[85vh] overflow-y-auto" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <div className="flex items-center gap-3">
                <div className="w-10 h-10 rounded-xl bg-gradient-to-br from-purple-500 to-violet-600 flex items-center justify-center shadow-lg">
                  <History className="w-5 h-5 text-white" />
                </div>
                <div>
                  <h3 className="text-lg font-semibold text-gray-900 dark:text-gray-100">
                    Historique — {selectedUser.firstName} {selectedUser.lastName}
                  </h3>
                  <p className="text-xs text-gray-400">Parcours de disciple & âmes suivies</p>
                </div>
              </div>
              <button onClick={() => { setActionModal(''); setSelectedUser(null); }} className="p-1.5 rounded-lg hover:bg-gray-100 dark:hover:bg-gray-800">
                <X className="w-5 h-5 text-gray-400" />
              </button>
            </div>
            <div className="modal-body">
              {userHistory ? (
                <div className="space-y-5">
                  {/* Résumé */}
                  <div className="flex items-center justify-between p-4 rounded-2xl bg-gradient-to-br from-purple-50 to-violet-50 dark:from-purple-900/20 dark:to-violet-900/10 border border-purple-200/40 dark:border-purple-700/30">
                    <div>
                      <p className="text-xs text-gray-500 dark:text-gray-400">Rôle</p>
                      <p className="text-sm font-semibold text-gray-900 dark:text-gray-100">
                        {ROLE_FALLBACK[userHistory.role] || userHistory.role || '—'}
                      </p>
                    </div>
                    {userHistory.estChef && (
                      <span className="badge text-[10px] bg-gold-100 dark:bg-gold-900/30 text-gold-700 dark:text-gold-400">Chef de famille</span>
                    )}
                    <div className="text-right">
                      <p className="text-xs text-gray-500 dark:text-gray-400">Membre depuis</p>
                      <p className="text-sm font-semibold text-gray-900 dark:text-gray-100">
                        {userHistory.dateCreation
                          ? new Date(userHistory.dateCreation).toLocaleDateString('fr-FR', { day: 'numeric', month: 'short', year: 'numeric' })
                          : '—'}
                      </p>
                    </div>
                  </div>

                  {/* Âmes actuellement suivies */}
                  <div>
                    <div className="flex items-center justify-between mb-2">
                      <p className="text-xs font-semibold text-gray-700 dark:text-gray-300 uppercase tracking-wider flex items-center gap-1.5">
                        <Heart className="w-3.5 h-3.5 text-emerald-500" />
                        Âmes actuellement suivies
                      </p>
                      <span className="badge text-[10px] badge-success">{userHistory.nombreAmesActuelles ?? (userHistory.amesActuelles || []).length}</span>
                    </div>
                    {(userHistory.amesActuelles || []).length === 0 ? (
                      <div className="p-4 text-center rounded-xl bg-gray-50 dark:bg-gray-800/40">
                        <p className="text-xs text-gray-400">Aucune âme suivie actuellement</p>
                      </div>
                    ) : (
                      <div className="space-y-1.5">
                        {(userHistory.amesActuelles || []).map((s: any) => (
                          <div key={s.id} className="flex items-center gap-3 p-2.5 rounded-xl bg-gray-50 dark:bg-gray-800/40">
                            <div className={`w-8 h-8 rounded-full flex items-center justify-center text-[11px] font-bold text-white shrink-0 ${
                              s.statut === 'ACTIF' ? 'bg-emerald-500' : s.statut === 'EN_INTEGRATION' ? 'bg-amber-500' : s.statut === 'EN_VEILLE' ? 'bg-blue-500' : 'bg-red-500'
                            }`}>
                              {(s.nom || '?').split(' ').map((p: string) => p?.[0]).join('').slice(0, 2)}
                            </div>
                            <div className="min-w-0 flex-1">
                              <p className="text-sm font-medium text-gray-800 dark:text-gray-200 truncate">{s.nom}</p>
                              <p className="text-[10px] text-gray-400">
                                {s.statut === 'ACTIF' ? 'Actif' : s.statut === 'EN_INTEGRATION' ? 'En intégration' : s.statut === 'EN_VEILLE' ? 'En veille' : 'Décroché'}
                              </p>
                            </div>
                          </div>
                        ))}
                      </div>
                    )}
                  </div>

                  {/* Sorties passées */}
                  {(userHistory.sorties || []).length > 0 && (
                    <div>
                      <div className="flex items-center justify-between mb-2">
                        <p className="text-xs font-semibold text-gray-700 dark:text-gray-300 uppercase tracking-wider flex items-center gap-1.5">
                          <UserX className="w-3.5 h-3.5 text-red-500" />
                          Sorties de suivi
                        </p>
                        <span className="badge text-[10px] badge-gray">{(userHistory.sorties || []).length}</span>
                      </div>
                      <div className="space-y-1.5">
                        {(userHistory.sorties || []).map((ex: any, i: number) => (
                          <div key={i} className="flex items-center justify-between gap-3 p-2.5 rounded-xl bg-red-50/50 dark:bg-red-900/10 border border-red-200/30 dark:border-red-800/20">
                            <p className="text-sm text-gray-700 dark:text-gray-300 flex items-center gap-2 min-w-0">
                              <UserX className="w-3.5 h-3.5 text-red-400 shrink-0" />
                              <span className="truncate">{ex.motif || 'Sortie du suivi'}</span>
                            </p>
                            <span className="text-[10px] text-gray-400 whitespace-nowrap">
                              {ex.dateSortie ? new Date(ex.dateSortie).toLocaleDateString('fr-FR', { day: 'numeric', month: 'short', year: 'numeric' }) : '—'}
                            </span>
                          </div>
                        ))}
                      </div>
                    </div>
                  )}
                </div>
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
