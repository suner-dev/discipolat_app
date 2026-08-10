import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import toast from 'react-hot-toast';
import api from '@/lib/api';
import { Shield, Check, X, Loader2, Save, Plus, Copy, Pencil, Trash2, BadgeCheck } from 'lucide-react';

interface PermissionEntry {
  role: string;
  permission: string;
  enabled: boolean;
}

interface PlatformRole {
  key: string;
  label: string;
  description?: string;
  system: boolean;
  nb_permissions?: number;
}

interface PermissionCatalogEntry {
  key: string;
  label: string;
  module: string;
  description?: string;
}

export default function PermissionsPage() {
  const queryClient = useQueryClient();

  const { data: permissions, isLoading: permLoading } = useQuery({
    queryKey: ['permissions'],
    queryFn: async () => {
      const res = await api.get('/permissions');
      return res.data as PermissionEntry[];
    },
  });

  const { data: roles, isLoading: rolesLoading } = useQuery({
    queryKey: ['permissions', 'roles'],
    queryFn: async () => {
      const res = await api.get('/permissions/roles');
      return res.data as PlatformRole[];
    },
  });

  const { data: catalog } = useQuery({
    queryKey: ['permissions', 'catalog'],
    queryFn: async () => {
      const res = await api.get('/permissions/catalog');
      return res.data as PermissionCatalogEntry[];
    },
  });

  const updateMutation = useMutation({
    mutationFn: async ({ role, permission, enabled }: { role: string; permission: string; enabled: boolean }) => {
      await api.put(`/permissions/${role}/${permission}`, { enabled });
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['permissions'] });
      toast.success('Permission mise à jour');
    },
    onError: () => toast.error('Erreur lors de la mise à jour'),
  });

  const invalidateRoles = () => {
    queryClient.invalidateQueries({ queryKey: ['permissions', 'roles'] });
    queryClient.invalidateQueries({ queryKey: ['permissions'] });
  };

  const createRoleMutation = useMutation({
    mutationFn: async (body: { key: string; label: string; description?: string }) => {
      await api.post('/permissions/roles', body);
    },
    onSuccess: () => { invalidateRoles(); toast.success('Rôle créé'); },
    onError: (e: unknown) => toast.error((e as { response?: { data?: { error?: string } } }).response?.data?.error || 'Erreur'),
  });

  const renameRoleMutation = useMutation({
    mutationFn: async ({ key, label }: { key: string; label: string }) => {
      await api.put(`/permissions/roles/${key}`, { label });
    },
    onSuccess: () => { invalidateRoles(); toast.success('Rôle renommé'); },
    onError: () => toast.error('Erreur'),
  });

  const duplicateRoleMutation = useMutation({
    mutationFn: async ({ sourceKey, newKey, label }: { sourceKey: string; newKey: string; label: string }) => {
      await api.post('/permissions/roles/duplicate', { sourceKey, newKey, label });
    },
    onSuccess: () => { invalidateRoles(); toast.success('Rôle dupliqué'); },
    onError: () => toast.error('Erreur'),
  });

  const deleteRoleMutation = useMutation({
    mutationFn: async (key: string) => { await api.delete(`/permissions/roles/${key}`); },
    onSuccess: () => { invalidateRoles(); toast.success('Rôle supprimé'); },
    onError: (e: unknown) => toast.error((e as { response?: { data?: { detail?: string } } }).response?.data?.detail || 'Erreur'),
  });

  // Modal state
  const [createOpen, setCreateOpen] = useState(false);
  const [createForm, setCreateForm] = useState({ key: '', label: '', description: '' });
  const [renameRole, setRenameRole] = useState<PlatformRole | null>(null);
  const [duplicateTarget, setDuplicateTarget] = useState<PlatformRole | null>(null);
  const [dupForm, setDupForm] = useState({ newKey: '', label: '' });

  const groupedPermissions = permissions?.reduce((acc, p) => {
    (acc[p.role] = acc[p.role] || []).push(p);
    return acc;
  }, {} as Record<string, PermissionEntry[]>) || {};

  const roleKeys = Object.keys(groupedPermissions);
  const catalogByModule = catalog?.reduce((acc, p) => {
    (acc[p.module] = acc[p.module] || []).push(p);
    return acc;
  }, {} as Record<string, PermissionCatalogEntry[]>) || {};

  const getPermissionLabel = (perm: string) => {
    const entry = catalog?.find((c) => c.key === perm);
    return entry?.label || perm.replace(/_/g, ' ').toLowerCase().replace(/\b\w/g, (c) => c.toUpperCase());
  };

  const getRoleLabel = (key: string) => {
    const role = roles?.find((r) => r.key === key);
    return role?.label || key;
  };

  const isSystemRole = (key: string) => roles?.find((r) => r.key === key)?.system ?? false;

  if (permLoading || rolesLoading) {
    return <div className="min-h-[50vh] flex items-center justify-center"><div className="spinner h-8 w-8" /></div>;
  }

  return (
    <div className="page-container max-w-full">
      <div className="page-header">
        <div>
          <h1 className="page-title">Rôles & permissions</h1>
          <p className="page-subtitle">Gérez les rôles et leurs permissions. Les rôles système ne peuvent pas être supprimés.</p>
        </div>
        <div className="page-header-actions">
          <button className="btn-primary btn-sm" onClick={() => { setCreateForm({ key: '', label: '', description: '' }); setCreateOpen(true); }}>
            <Plus className="w-4 h-4" /> Nouveau rôle
          </button>
        </div>
      </div>

      {/* Rôles */}
      <div className="glass-card overflow-hidden mb-6">
        <div className="card-header"><h3 className="text-sm font-bold text-gray-900 dark:text-gray-100">Rôles</h3></div>
        <div className="divide-y divide-gray-100/50 dark:divide-gray-800/30">
          {roles?.map((role) => (
            <div key={role.key} className="flex items-center gap-4 px-5 py-3">
              {role.system ? <BadgeCheck className="w-5 h-5 text-primary-500 flex-shrink-0" /> : <Shield className="w-5 h-5 text-gray-400 flex-shrink-0" />}
              <div className="flex-1 min-w-0">
                <div className="flex items-center gap-2">
                  <span className="text-sm font-semibold text-gray-900 dark:text-gray-100">{role.label}</span>
                  <span className="font-mono text-[10px] text-gray-400">({role.key})</span>
                  {role.system && <span className="badge badge-info text-[10px]">Système</span>}
                </div>
                {role.description && <p className="text-xs text-gray-500 dark:text-gray-400 truncate">{role.description}</p>}
              </div>
              <span className="text-xs text-gray-400">{role.nb_permissions ?? '-'} permission(s)</span>
              <div className="flex items-center gap-1">
                <button className="btn-icon text-gray-400 hover:text-gray-700 dark:hover:text-gray-200 hover:bg-gray-100/70 dark:hover:bg-gray-800/40" onClick={() => { setRenameRole(role); }} aria-label={`Renommer ${role.label}`}><Pencil className="w-4 h-4" /></button>
                <button className="btn-icon text-gray-400 hover:text-primary-600 hover:bg-primary-50/50 dark:hover:bg-primary-900/20" onClick={() => { setDuplicateTarget(role); setDupForm({ newKey: role.key + '_COPY', label: role.label + ' (copie)' }); }} aria-label={`Dupliquer ${role.label}`}><Copy className="w-4 h-4" /></button>
                {!role.system && (
                  <button className="btn-icon text-gray-400 hover:text-red-500 hover:bg-red-50 dark:hover:bg-red-900/20" onClick={() => { if (confirm(`Supprimer le rôle « ${role.label} » ?`)) deleteRoleMutation.mutate(role.key); }} aria-label={`Supprimer ${role.label}`}><Trash2 className="w-4 h-4" /></button>
                )}
              </div>
            </div>
          ))}
        </div>
      </div>

      {/* Tableau des permissions par module */}
      {Object.entries(catalogByModule).map(([module, perms]) => (
        <div key={module} className="glass-card overflow-hidden mb-6">
          <div className="card-header"><h3 className="text-sm font-bold text-gray-900 dark:text-gray-100 uppercase tracking-wider">{module}</h3></div>
          <div className="overflow-x-auto">
            <table className="table">
              <thead>
                <tr>
                  <th className="min-w-[200px]">Permission</th>
                  {roleKeys.map((role) => (
                    <th key={role} className="text-center px-3 text-[10px]">{getRoleLabel(role)}</th>
                  ))}
                </tr>
              </thead>
              <tbody>
                {perms.map((perm) => {
                  const permKey = perm.key;
                  return (
                    <tr key={permKey}>
                      <td className="text-sm font-medium text-gray-700 dark:text-gray-300">
                        <span className="block">{perm.label}</span>
                        <span className="block text-[10px] text-gray-400">{permKey}</span>
                      </td>
                      {roleKeys.map((role) => {
                        const entry = groupedPermissions[role]?.find((p) => p.permission === permKey);
                        const enabled = entry?.enabled ?? true;
                        return (
                          <td key={role} className="text-center">
                            <button
                              onClick={() => updateMutation.mutate({ role, permission: permKey, enabled: !enabled })}
                              className={`w-9 h-9 rounded-xl flex items-center justify-center mx-auto transition-all duration-200
                                ${enabled
                                  ? 'bg-primary-500/15 text-primary-600 dark:text-primary-400 hover:bg-primary-500/25'
                                  : 'bg-gray-100/50 dark:bg-gray-800/30 text-gray-300 dark:text-gray-600 hover:bg-gray-200/50 dark:hover:bg-gray-700/40'
                                }`}
                              title={enabled ? 'Cliquer pour désactiver' : 'Cliquer pour activer'}
                            >
                              {enabled ? <Check className="w-4 h-4" /> : <X className="w-4 h-4" />}
                            </button>
                          </td>
                        );
                      })}
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        </div>
      ))}

      {/* Modal créer rôle */}
      {createOpen && (
        <div className="modal-overlay" onClick={() => setCreateOpen(false)}>
          <div className="modal-content" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header"><h3 className="text-base font-bold text-gray-900 dark:text-gray-100">Nouveau rôle</h3>
              <button className="btn-icon text-gray-400 hover:text-gray-600" onClick={() => setCreateOpen(false)}>×</button></div>
            <div className="modal-body space-y-4">
              <div><label className="label">Clé (unique)</label><input className="input font-mono" value={createForm.key} onChange={(e) => setCreateForm({ ...createForm, key: e.target.value.toUpperCase() })} placeholder="NOUVEAU_ROLE" /></div>
              <div><label className="label">Libellé</label><input className="input" value={createForm.label} onChange={(e) => setCreateForm({ ...createForm, label: e.target.value })} /></div>
              <div><label className="label">Description</label><input className="input" value={createForm.description} onChange={(e) => setCreateForm({ ...createForm, description: e.target.value })} /></div>
            </div>
            <div className="modal-footer">
              <button className="btn-ghost btn-sm" onClick={() => setCreateOpen(false)}>Annuler</button>
              <button className="btn-primary btn-sm" onClick={() => { createRoleMutation.mutate(createForm); setCreateOpen(false); }} disabled={createRoleMutation.isPending}>
                <Plus className="w-4 h-4" /> Créer
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Modal renommer */}
      {renameRole && (
        <div className="modal-overlay" onClick={() => setRenameRole(null)}>
          <div className="modal-content" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header"><h3 className="text-base font-bold text-gray-900 dark:text-gray-100">Renommer {renameRole.label}</h3>
              <button className="btn-icon text-gray-400 hover:text-gray-600" onClick={() => setRenameRole(null)}>×</button></div>
            <div className="modal-body">
              <div><label className="label">Nouveau libellé</label>
                <input className="input" defaultValue={renameRole.label} id="rename-label" autoFocus
                  onKeyDown={(e) => { if (e.key === 'Enter') { renameRoleMutation.mutate({ key: renameRole.key, label: (e.target as HTMLInputElement).value }); setRenameRole(null); } }} />
              </div>
            </div>
            <div className="modal-footer">
              <button className="btn-ghost btn-sm" onClick={() => setRenameRole(null)}>Annuler</button>
              <button className="btn-primary btn-sm" onClick={() => {
                const input = document.getElementById('rename-label') as HTMLInputElement;
                renameRoleMutation.mutate({ key: renameRole.key, label: input?.value || renameRole.label });
                setRenameRole(null);
              }}><Save className="w-4 h-4" /> Enregistrer</button>
            </div>
          </div>
        </div>
      )}

      {/* Modal dupliquer */}
      {duplicateTarget && (
        <div className="modal-overlay" onClick={() => setDuplicateTarget(null)}>
          <div className="modal-content" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header"><h3 className="text-base font-bold text-gray-900 dark:text-gray-100">Dupliquer {duplicateTarget.label}</h3>
              <button className="btn-icon text-gray-400 hover:text-gray-600" onClick={() => setDuplicateTarget(null)}>×</button></div>
            <div className="modal-body space-y-4">
              <div><label className="label">Clé du nouveau rôle</label><input className="input font-mono" value={dupForm.newKey} onChange={(e) => setDupForm({ ...dupForm, newKey: e.target.value.toUpperCase() })} /></div>
              <div><label className="label">Libellé</label><input className="input" value={dupForm.label} onChange={(e) => setDupForm({ ...dupForm, label: e.target.value })} /></div>
            </div>
            <div className="modal-footer">
              <button className="btn-ghost btn-sm" onClick={() => setDuplicateTarget(null)}>Annuler</button>
              <button className="btn-primary btn-sm" onClick={() => { duplicateRoleMutation.mutate({ sourceKey: duplicateTarget.key, ...dupForm }); setDuplicateTarget(null); }}><Copy className="w-4 h-4" /> Dupliquer</button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}