import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import api from '@/lib/api';
import toast from 'react-hot-toast';
import { Shield, Check, X, Loader2, Save } from 'lucide-react';

interface Permission {
  role: string;
  permission: string;
  enabled: boolean;
}

export default function PermissionsPage() {
  const queryClient = useQueryClient();

  const { data: permissions, isLoading } = useQuery({
    queryKey: ['permissions'],
    queryFn: async () => {
      const res = await api.get('/permissions');
      return res.data as Permission[];
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

  const groupedPermissions = permissions?.reduce((acc, p) => {
    if (!acc[p.role]) acc[p.role] = [];
    acc[p.role].push(p);
    return acc;
  }, {} as Record<string, Permission[]>) || {};

  const getRoleLabel = (role: string) => {
    const labels: Record<string, string> = {
      ADMIN: 'Admin',
      PASTEUR: 'Pasteur',
      RESPONSABLE: 'Responsable',
      FAISEUR: 'Faiseur',
    };
    return labels[role] || role;
  };

  const getPermissionLabel = (perm: string) => {
    const labels: Record<string, string> = {
      USER_CREATE: 'Créer utilisateur',
      USER_READ: 'Voir utilisateurs',
      USER_UPDATE: 'Modifier utilisateur',
      USER_DELETE: 'Supprimer utilisateur',
      FAMILY_CREATE: 'Créer famille',
      FAMILY_READ: 'Voir familles',
      FAMILY_UPDATE: 'Modifier famille',
      FAMILY_DELETE: 'Supprimer famille',
      SOUL_CREATE: 'Créer âme',
      SOUL_READ: 'Voir âmes',
      SOUL_UPDATE: 'Modifier âme',
      SOUL_DELETE: 'Supprimer âme',
      REPORT_CREATE: 'Créer rapport',
      REPORT_READ: 'Voir rapports',
      REPORT_EXPORT: 'Exporter rapports',
      REPORT_CORRECT: 'Corriger rapport',
      AUDIT_READ: 'Voir audit',
      BULK_IMPORT: 'Import en masse',
      PERMISSION_MANAGE: 'Gérer permissions',
    };
    return labels[perm] || perm;
  };

  if (isLoading) {
    return (
      <div className="page-container flex items-center justify-center min-h-[60vh]">
        <Loader2 className="w-8 h-8 animate-spin text-primary-500" />
      </div>
    );
  }

  return (
    <div className="page-container">
      <div className="page-header">
        <div className="flex items-center gap-3">
          <div className="p-3 rounded-xl bg-gradient-to-br from-purple-500 to-violet-500 text-white shadow-lg">
            <Shield className="w-6 h-6" />
          </div>
          <div>
            <h1 className="page-title">Matrice des permissions</h1>
            <p className="page-subtitle">Configuration fine des accès par rôle</p>
          </div>
        </div>
      </div>

      <div className="glass-card overflow-hidden">
        <div className="overflow-x-auto">
          <table className="data-table">
            <thead>
              <tr>
                <th className="min-w-[200px]">Permission</th>
                {Object.keys(groupedPermissions).map((role) => (
                  <th key={role} className="text-center">{getRoleLabel(role)}</th>
                ))}
              </tr>
            </thead>
            <tbody>
              {Object.entries(
                (permissions || []).reduce((acc, p) => {
                  if (!acc[p.permission]) acc[p.permission] = {};
                  acc[p.permission][p.role] = p.enabled;
                  return acc;
                }, {} as Record<string, Record<string, boolean>>)
              ).map(([permission, roles]) => (
                <tr key={permission}>
                  <td className="font-medium text-gray-900 dark:text-gray-100">
                    {getPermissionLabel(permission)}
                    <span className="block text-xs text-gray-400">{permission}</span>
                  </td>
                  {Object.keys(groupedPermissions).map((role) => {
                    const enabled = roles[role] ?? false;
                    return (
                      <td key={role} className="text-center">
                        <button
                          onClick={() => updateMutation.mutate({ role, permission, enabled: !enabled })}
                          disabled={updateMutation.isPending}
                          className={`inline-flex items-center justify-center w-10 h-10 rounded-xl transition-all duration-200 ${
                            enabled
                              ? 'bg-green-50 dark:bg-green-900/20 text-green-600 hover:bg-green-100'
                              : 'bg-gray-50 dark:bg-gray-800/30 text-gray-400 hover:bg-gray-100'
                          }`}
                        >
                          {enabled ? <Check className="w-5 h-5" /> : <X className="w-5 h-5" />}
                        </button>
                      </td>
                    );
                  })}
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
}
