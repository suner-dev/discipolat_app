import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useParams, useNavigate } from 'react-router-dom';
import toast from 'react-hot-toast';
import api, { getErrorMessage } from '@/lib/api';
import {
  Shield, Plus, Trash2, ArrowUp, ArrowDown, Loader2, X, Star,
} from 'lucide-react';
import { tText } from '@/i18n';

/** Champs pertinents de UserResponse côté serveur (record Java). */
interface UserResponse {
  id: string;
  firstName: string;
  lastName: string;
  email: string;
  role: string;
  roles: string[];
  activeRole: string | null;
  estChefDeFamille: boolean;
  familleGereeId: string | null;
}

interface FamilySummary { id: string; nom: string; }

const AVAILABLE_ROLES = [
  { key: 'ADMIN', label: 'Administrateur', color: 'from-red-500 to-rose-500' },
  { key: 'PASTEUR', label: 'Pasteur', color: 'from-blue-500 to-indigo-500' },
  { key: 'RESPONSABLE', label: 'Responsable', color: 'from-purple-500 to-violet-500' },
  { key: 'FAISEUR', label: 'Faiseur de disciples', color: 'from-green-500 to-emerald-500' },
  { key: 'CHEF_DE_FAMILLE', label: 'Chef de famille', color: 'from-amber-500 to-orange-500' },
  { key: 'MEMBRE', label: 'Membre', color: 'from-gray-400 to-gray-500' },
];

export default function UserRolesPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const qc = useQueryClient();
  const [showAddRole, setShowAddRole] = useState(false);
  const [showChefPicker, setShowChefPicker] = useState(false);

  // --- Contract repaired: GET /users/{id} returns UserResponse (the real endpoint).
  const { data: user, isLoading } = useQuery({
    queryKey: ['users', id],
    queryFn: async () => {
      const res = await api.get(`/users/${id}`);
      return res.data as UserResponse;
    },
    enabled: !!id,
  });

  const roles: string[] = user?.roles ?? [];
  const activeRole: string | null = user?.activeRole ?? null;
  const isChef = user?.estChefDeFamille ?? false;

  // Fetch families for chef-promotion picker.
  const { data: families = [] } = useQuery({
    queryKey: ['families-for-chef'],
    queryFn: async () => {
      const res = await api.get('/families', { params: { size: 100 } });
      const content = res.data?.content ?? res.data ?? [];
      return (Array.isArray(content) ? content : []).map((f: any) => ({ id: f.id, nom: f.nom || f.name || f.id })) as FamilySummary[];
    },
    enabled: showChefPicker,
  });

  // --- Mutations on the REAL server contract ---

  const addRoleMutation = useMutation({
    mutationFn: async (role: string) => {
      // POST /users/{id}/roles  body {role}
      await api.post(`/users/${id}/roles`, { role });
    },
    onSuccess: () => {
      toast.success(tText('Rôle ajouté'));
      qc.invalidateQueries({ queryKey: ['users', id] });
      setShowAddRole(false);
    },
    onError: (err) => toast.error(getErrorMessage(err)),
  });

  const removeRoleMutation = useMutation({
    mutationFn: async (role: string) => {
      // DELETE /users/{id}/roles/{role}  (correct already)
      await api.delete(`/users/${id}/roles/${role}`);
    },
    onSuccess: () => {
      toast.success(tText('Rôle retiré'));
      qc.invalidateQueries({ queryKey: ['users', id] });
    },
    onError: (err) => toast.error(getErrorMessage(err)),
  });

  const setActiveRoleMutation = useMutation({
    mutationFn: async (role: string) => {
      // PATCH /users/{id}/active-role  body {activeRole}
      await api.patch(`/users/${id}/active-role`, { activeRole: role });
    },
    onSuccess: () => {
      toast.success(tText('Rôle actif changé'));
      qc.invalidateQueries({ queryKey: ['users', id] });
    },
    onError: (err) => toast.error(getErrorMessage(err)),
  });

  const promoteChefMutation = useMutation({
    mutationFn: async (familleId: string) => {
      // PATCH /users/{id}/promote-chef  body {familleId}
      await api.patch(`/users/${id}/promote-chef`, { familleId });
    },
    onSuccess: () => {
      toast.success(tText('Promu chef de famille'));
      qc.invalidateQueries({ queryKey: ['users', id] });
      setShowChefPicker(false);
    },
    onError: (err) => toast.error(getErrorMessage(err)),
  });

  const demoteChefMutation = useMutation({
    mutationFn: async () => {
      // PATCH /users/{id}/demote-chef
      await api.patch(`/users/${id}/demote-chef`);
    },
    onSuccess: () => {
      toast.success(tText('Rétrogradé de chef de famille'));
      qc.invalidateQueries({ queryKey: ['users', id] });
    },
    onError: (err) => toast.error(getErrorMessage(err)),
  });

  const assignedRoles = roles;
  const availableToAdd = AVAILABLE_ROLES.filter((r) => !assignedRoles.includes(r.key));

  if (isLoading) return <div className="min-h-[40vh] flex items-center justify-center"><Loader2 className="w-6 h-6 animate-spin text-gray-400" /></div>;

  return (
    <div className="page-container max-w-3xl">
      <div className="page-header">
        <div className="animate-fade-in">
          <button onClick={() => navigate(-1)} className="btn-ghost btn-sm mb-2 -ml-2">
            <X className="w-4 h-4" /> {tText('Retour')}
          </button>
          <div className="flex items-center gap-2 mb-1">
            <Shield className="w-5 h-5 text-primary-500" />
            <h1 className="page-title">{tText('Gestion des rôles')}</h1>
          </div>
          <p className="page-subtitle">{tText('Attribution et gestion des rôles utilisateur')}</p>
        </div>
        <div className="page-header-actions">
          <button onClick={() => setShowAddRole(true)} className="btn-primary btn-sm">
            <Plus className="w-4 h-4" /> {tText('Ajouter un rôle')}
          </button>
        </div>
      </div>

      {/* Active role */}
      {activeRole && (
        <div className="glass-card p-4 mb-6 flex items-center gap-3 border-l-4 border-primary-500">
          <Shield className="w-5 h-5 text-primary-500 shrink-0" />
          <div>
            <p className="text-xs text-gray-400">{tText('Rôle actif')}</p>
            <p className="text-sm font-bold text-gray-900 dark:text-gray-100">
              {AVAILABLE_ROLES.find((r) => r.key === activeRole)?.label || activeRole}
            </p>
          </div>
        </div>
      )}

      {/* Chef promotion */}
      <div className="glass-card p-4 mb-6">
        <div className="flex items-center justify-between">
          <div>
            <h3 className="text-sm font-bold text-gray-900 dark:text-gray-100 flex items-center gap-2">
              <ArrowUp className="w-4 h-4 text-amber-500" />
              {tText('Chef de famille')}
            </h3>
            <p className="text-xs text-gray-400 mt-0.5">
              {isChef ? 'Cet utilisateur est chef de famille' : 'Promouvoir ou rétrograder de chef de famille'}
            </p>
          </div>
          {isChef ? (
            <button
              onClick={() => demoteChefMutation.mutate()}
              disabled={demoteChefMutation.isPending}
              className="btn-secondary btn-sm"
            >
              {demoteChefMutation.isPending ? <Loader2 className="w-4 h-4 animate-spin" /> : <ArrowDown className="w-4 h-4" />}
              Rétrograder
            </button>
          ) : (
            <button
              onClick={() => setShowChefPicker(true)}
              disabled={promoteChefMutation.isPending}
              className="btn-primary btn-sm"
            >
              {promoteChefMutation.isPending ? <Loader2 className="w-4 h-4 animate-spin" /> : <ArrowUp className="w-4 h-4" />}
              Promouvoir
            </button>
          )}
        </div>
      </div>

      {/* Roles list */}
      <h3 className="text-sm font-bold text-gray-900 dark:text-gray-100 mb-3">{tText('Rôles assignés')}</h3>
      {roles.length === 0 ? (
        <div className="glass-card p-8 text-center">
          <Shield className="w-8 h-8 text-gray-300 mb-2 mx-auto" />
          <p className="text-gray-500 text-sm">Aucun rôle assigné.</p>
        </div>
      ) : (
        <div className="space-y-2">
          {roles.map((roleKey) => {
            const roleInfo = AVAILABLE_ROLES.find((r) => r.key === roleKey);
            const isActive = roleKey === activeRole;
            return (
              <div key={roleKey} className="glass-card px-5 py-3 flex items-center justify-between">
                <div className="flex items-center gap-3">
                  <div className={`w-9 h-9 rounded-xl bg-gradient-to-br ${roleInfo?.color || 'from-gray-400 to-gray-500'} flex items-center justify-center text-white shadow-sm`}>
                    <Shield className="w-4 h-4" />
                  </div>
                  <div>
                    <p className="text-sm font-semibold text-gray-900 dark:text-gray-100">
                      {roleInfo?.label || roleKey}
                    </p>
                    {isActive && (
                      <p className="text-[10px] text-primary-500 font-medium">{tText('Actif')}</p>
                    )}
                  </div>
                </div>
                <div className="flex items-center gap-1">
                  {!isActive && (
                    <button
                      onClick={() => setActiveRoleMutation.mutate(roleKey)}
                      disabled={setActiveRoleMutation.isPending}
                      className="btn-icon text-primary-400 hover:text-primary-500 hover:bg-primary-50 dark:hover:bg-primary-900/20"
                      title={tText('Définir comme actif')}
                    >
                      <Star className="w-4 h-4" />
                    </button>
                  )}
                  <button
                    onClick={() => {
                      if (confirm(`Retirer le rôle ${roleInfo?.label || roleKey} ?`)) {
                        removeRoleMutation.mutate(roleKey);
                      }
                    }}
                    className="btn-icon text-red-400 hover:text-red-500 hover:bg-red-50 dark:hover:bg-red-900/20"
                    title={tText('Retirer le rôle')}
                  >
                    <Trash2 className="w-4 h-4" />
                  </button>
                </div>
              </div>
            );
          })}
        </div>
      )}

      {/* Add role modal */}
      {showAddRole && (
        <div className="modal-overlay" onClick={() => setShowAddRole(false)}>
          <div className="modal-content max-w-md" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <h3 className="text-base font-bold text-gray-900 dark:text-gray-100">{tText('Ajouter un rôle')}</h3>
              <button className="btn-icon" onClick={() => setShowAddRole(false)}><X className="w-5 h-5" /></button>
            </div>
            <div className="modal-body space-y-2">
              {availableToAdd.length === 0 ? (
                <p className="text-sm text-gray-400 text-center py-4">Tous les rôles sont déjà assignés.</p>
              ) : (
                availableToAdd.map((role) => (
                  <button
                    key={role.key}
                    onClick={() => addRoleMutation.mutate(role.key)}
                    disabled={addRoleMutation.isPending}
                    className="w-full text-left p-3 rounded-xl border border-gray-200 dark:border-white/10 hover:border-primary-500 hover:bg-primary-50 dark:hover:bg-primary-900/10 transition-all"
                  >
                    <div className="flex items-center gap-3">
                      <div className={`w-8 h-8 rounded-lg bg-gradient-to-br ${role.color} flex items-center justify-center text-white shadow-sm`}>
                        <Shield className="w-4 h-4" />
                      </div>
                      <span className="text-sm font-medium text-gray-900 dark:text-gray-100">{role.label}</span>
                    </div>
                  </button>
                ))
              )}
            </div>
            <div className="modal-footer">
              <button className="btn-ghost btn-sm" onClick={() => setShowAddRole(false)}>Fermer</button>
            </div>
          </div>
        </div>
      )}

      {/* Chef-picker modal (promote-chef requires familleId) */}
      {showChefPicker && (
        <div className="modal-overlay" onClick={() => setShowChefPicker(false)}>
          <div className="modal-content max-w-md" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <h3 className="text-base font-bold text-gray-900 dark:text-gray-100">{tText('Promouvoir chef de famille')}</h3>
              <button className="btn-icon" onClick={() => setShowChefPicker(false)}><X className="w-5 h-5" /></button>
            </div>
            <div className="modal-body space-y-2">
              <p className="text-xs text-gray-400 mb-2">{tText('Choisissez la famille à diriger :')}</p>
              {families.length === 0 ? (
                <p className="text-sm text-gray-400 text-center py-4">{tText('Aucune famille disponible.')}</p>
              ) : (
                families.map((f) => (
                  <button
                    key={f.id}
                    onClick={() => promoteChefMutation.mutate(f.id)}
                    disabled={promoteChefMutation.isPending}
                    className="w-full text-left p-3 rounded-xl border border-gray-200 dark:border-white/10 hover:border-amber-500 hover:bg-amber-50 dark:hover:bg-amber-900/10 transition-all"
                  >
                    <span className="text-sm font-medium text-gray-900 dark:text-gray-100">{f.nom}</span>
                  </button>
                ))
              )}
            </div>
            <div className="modal-footer">
              <button className="btn-ghost btn-sm" onClick={() => setShowChefPicker(false)}>{tText('Annuler')}</button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
