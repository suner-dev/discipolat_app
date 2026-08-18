import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import toast from 'react-hot-toast';
import api from '@/lib/api';
import { Shield, Check, X, Loader2, Save, Plus, Copy, Pencil, Trash2, BadgeCheck, Eye, Pencil as PencilIcon, Trash, ChevronDown, ChevronRight } from 'lucide-react';

interface PermissionEntry {
  role: string;
  permission: string;
  enabled: boolean;
  canRead?: boolean;
  canWrite?: boolean;
  canDelete?: boolean;
  scope?: string;
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
  defaultScope?: string;
  scopeDescription?: string;
}

const SCOPE_OPTIONS = [
  { value: 'GLOBAL', label: 'Global', description: 'Accès à toutes les données' },
  { value: 'DEPARTMENT', label: 'Département', description: 'Limité aux départements assignés' },
  { value: 'FAMILY', label: 'Famille', description: 'Limité à la famille assignée' },
  { value: 'TEAM', label: 'Équipe', description: 'Limité à l\'équipe assignée' },
  { value: 'OWN', label: 'Propre', description: 'Limité à ses propres données' },
];

export default function PermissionsPage() {
  const queryClient = useQueryClient();
  const [expandedModules, setExpandedModules] = useState<Record<string, boolean>>({});
  const [expandedRole, setExpandedRole] = useState<string | null>(null);
  const [rwdMode, setRwdMode] = useState<boolean>(false); // false = ancien mode (enabled), true = mode R/W/D

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

  // --- Mutations ---
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

  const updateRwdMutation = useMutation({
    mutationFn: async ({ role, permission, canRead, canWrite, canDelete, scope }: {
      role: string; permission: string; canRead: boolean; canWrite: boolean; canDelete: boolean; scope: string;
    }) => {
      await api.put(`/permissions/${role}/${permission}/rwd`, { canRead, canWrite, canDelete, scope });
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

  const toggleModule = (module: string) => {
    setExpandedModules((prev) => ({ ...prev, [module]: !prev[module] }));
  };

  const toggleRoleExpand = (role: string) => {
    setExpandedRole(expandedRole === role ? null : role);
  };

  const getEntry = (role: string, perm: string): PermissionEntry | undefined => {
    return groupedPermissions[role]?.find((p) => p.permission === perm);
  };

  // --- R/W/D cell renderer ---
  const renderRwdCell = (role: string, perm: string) => {
    if (!rwdMode) {
      // Ancien mode : toggle enabled
      const entry = getEntry(role, perm);
      const enabled = entry?.enabled ?? true;
      return (
        <td key={role} className="text-center">
          <button
            onClick={() => updateMutation.mutate({ role, permission: perm, enabled: !enabled })}
            className={`w-9 h-9 rounded-xl flex items-center justify-center mx-auto transition-all duration-200
              ${enabled
                ? 'bg-primary-500/15 text-primary-600 dark:text-primary-400 hover:bg-primary-500/25'
                : 'bg-gray-100/50 dark:bg-gray-800/30 text-gray-300 dark:text-gray-600 hover:bg-gray-200/50 dark:hover:bg-gray-700/40'
              }`}
            title={enabled ? 'Désactiver' : 'Activer'}
          >
            {enabled ? <Check className="w-4 h-4" /> : <X className="w-4 h-4" />}
          </button>
        </td>
      );
    }

    // Mode R/W/D : 3 mini-toggle + scope
    const entry = getEntry(role, perm);
    const canRead = entry?.canRead ?? entry?.enabled ?? true;
    const canWrite = entry?.canWrite ?? entry?.enabled ?? true;
    const canDelete = entry?.canDelete ?? false;
    const scope = entry?.scope ?? 'GLOBAL';

    const handleToggle = (field: 'canRead' | 'canWrite' | 'canDelete', val: boolean) => {
      const newValues = { canRead, canWrite, canDelete, scope, [field]: val };
      updateRwdMutation.mutate({ role, permission: perm, ...newValues });
    };

    const handleScopeChange = (newScope: string) => {
      updateRwdMutation.mutate({ role, permission: perm, canRead, canWrite, canDelete, scope: newScope });
    };

    return (
      <td key={role} className="text-center px-1">
        <div className="flex flex-col items-center gap-1">
          {/* R/W/D mini-toggles */}
          <div className="flex items-center gap-0.5">
            <button
              onClick={() => handleToggle('canRead', !canRead)}
              className={`w-7 h-7 rounded-lg flex items-center justify-center transition-all text-[10px] font-bold
                ${canRead ? 'bg-blue-500/15 text-blue-600 dark:text-blue-400' : 'bg-gray-100/50 text-gray-300 dark:text-gray-600'}`}
              title={canRead ? 'Lecture activée' : 'Lecture désactivée'}
            >
              <Eye className="w-3 h-3" />
            </button>
            <button
              onClick={() => handleToggle('canWrite', !canWrite)}
              className={`w-7 h-7 rounded-lg flex items-center justify-center transition-all text-[10px] font-bold
                ${canWrite ? 'bg-amber-500/15 text-amber-600 dark:text-amber-400' : 'bg-gray-100/50 text-gray-300 dark:text-gray-600'}`}
              title={canWrite ? 'Écriture activée' : 'Écriture désactivée'}
            >
              <PencilIcon className="w-3 h-3" />
            </button>
            <button
              onClick={() => handleToggle('canDelete', !canDelete)}
              className={`w-7 h-7 rounded-lg flex items-center justify-center transition-all text-[10px] font-bold
                ${canDelete ? 'bg-red-500/15 text-red-600 dark:text-red-400' : 'bg-gray-100/50 text-gray-300 dark:text-gray-600'}`}
              title={canDelete ? 'Suppression activée' : 'Suppression désactivée'}
            >
              <Trash className="w-3 h-3" />
            </button>
          </div>
          {/* Scope selector */}
          <select
            value={scope}
            onChange={(e) => handleScopeChange(e.target.value)}
            className="text-[9px] px-1 py-0.5 rounded border border-gray-200 dark:border-gray-700 bg-white dark:bg-gray-800 text-gray-600 dark:text-gray-400 max-w-[80px] truncate"
            title={`Scope: ${SCOPE_OPTIONS.find(s => s.value === scope)?.description || scope}`}
          >
            {SCOPE_OPTIONS.map(s => (
              <option key={s.value} value={s.value}>{s.label}</option>
            ))}
          </select>
        </div>
      </td>
    );
  };

  if (permLoading || rolesLoading) {
    return <div className="min-h-[50vh] flex items-center justify-center"><div className="spinner h-8 w-8" /></div>;
  }

  return (
    <div className="page-container max-w-full">
      <div className="page-header">
        <div>
          <h1 className="page-title">Rôles & permissions</h1>
          <p className="page-subtitle">
            Gérez les rôles, permissions granulaires (lecture/écriture/suppression) et scopes d'accès.
          </p>
        </div>
        <div className="page-header-actions flex items-center gap-2">
          {/* Toggle RWD mode */}
          <button
            onClick={() => setRwdMode(!rwdMode)}
            className={`btn-sm rounded-lg px-3 py-1.5 text-xs font-medium transition-all ${
              rwdMode
                ? 'bg-primary-500 text-white shadow-md'
                : 'bg-gray-100 dark:bg-gray-800 text-gray-600 dark:text-gray-400 hover:bg-gray-200 dark:hover:bg-gray-700'
            }`}
          >
            {rwdMode ? 'Mode R/W/D ✓' : 'Mode simple'}
          </button>
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
      {Object.entries(catalogByModule).map(([module, perms]) => {
        const isExpanded = expandedModules[module] !== false; // expanded by default
        return (
          <div key={module} className="glass-card overflow-hidden mb-6">
            <button
              className="card-header w-full flex items-center gap-2 hover:bg-gray-50/50 dark:hover:bg-gray-800/20 transition-colors"
              onClick={() => toggleModule(module)}
            >
              {isExpanded ? <ChevronDown className="w-4 h-4 text-gray-400" /> : <ChevronRight className="w-4 h-4 text-gray-400" />}
              <h3 className="text-sm font-bold text-gray-900 dark:text-gray-100 uppercase tracking-wider">{module}</h3>
              <span className="text-[10px] text-gray-400 ml-2">({perms.length} permission{perms.length > 1 ? 's' : ''})</span>
            </button>
            {isExpanded && (
              <div className="overflow-x-auto">
                <table className="table">
                  <thead>
                    <tr>
                      <th className="min-w-[200px]">Permission</th>
                      {roleKeys.map((role) => (
                        <th key={role} className="text-center px-3 text-[10px]">
                          <div className="flex flex-col items-center">
                            <span>{getRoleLabel(role)}</span>
                            {rwdMode && (
                              <span className="flex items-center gap-0.5 mt-0.5 text-gray-400">
                                <Eye className="w-2 h-2" />
                                <PencilIcon className="w-2 h-2" />
                                <Trash className="w-2 h-2" />
                              </span>
                            )}
                          </div>
                        </th>
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
                            {rwdMode && perm.defaultScope && perm.defaultScope !== 'GLOBAL' && (
                              <span className="block text-[9px] text-primary-500">Scope par défaut: {perm.defaultScope}</span>
                            )}
                          </td>
                          {roleKeys.map((role) => renderRwdCell(role, permKey))}
                        </tr>
                      );
                    })}
                  </tbody>
                </table>
              </div>
            )}
          </div>
        );
      })}

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
