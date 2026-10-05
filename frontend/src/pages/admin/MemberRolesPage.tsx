import { useMemo, useState } from 'react';
import { useParams } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import toast from 'react-hot-toast';
import { Users2, Plus, Trash2, Loader2 } from 'lucide-react';
import api from '@/lib/api';
import { useI18n } from '@/i18n';
import {
  useMemberAssignments, useAssignMemberRole, useEndMemberAssignment, useOrgTreeV3,
} from '@/hooks/useOrganizationV3';

/**
 * SPEC_ORGANISATION_MODULABLE_V3 §6.2 / T-W13 — Assignations membre (C).
 *
 * « Nommer X ancien et lui affecter 3 campus ». Le membre porte un
 * rôle-capacité sur 0..n nœuds, découplé de son appartenance. Retirer une
 * assignation = transition ENDED (jamais une purge).
 */

interface RoleRef { id: string; label: string; key?: string }

export default function MemberRolesPage() {
  const { userId } = useParams<{ userId: string }>();
  const { t } = useI18n();
  const assignments = useMemberAssignments(userId ?? null);
  const tree = useOrgTreeV3();
  const assign = useAssignMemberRole(userId ?? null);
  const end = useEndMemberAssignment(userId ?? null);

  const rolesQuery = useQuery<RoleRef[]>({
    queryKey: ['tenant', 'roles'],
    queryFn: async () => (await api.get('/tenant/roles')).data,
  });

  const [roleId, setRoleId] = useState('');
  const [nodeId, setNodeId] = useState('');

  const nodeNameById = useMemo(
    () => new Map((tree.data ?? []).map((n) => [n.id, n.name])),
    [tree.data],
  );
  const roleLabelById = useMemo(
    () => new Map((rolesQuery.data ?? []).map((r) => [r.id, r.label])),
    [rolesQuery.data],
  );

  const add = async () => {
    if (!roleId) return;
    try {
      await assign.mutateAsync({ roleId, nodeId: nodeId || null });
      toast.success(t('orgV3.assign.added'));
    } catch {
      toast.error(t('orgV3.assign.addErr'));
    }
  };

  const items = (assignments.data ?? []).filter((a) => a.status === 'ACTIVE');

  return (
    <div className="page-container">
      <div className="page-header">
        <div className="flex items-center gap-2 mb-1">
          <Users2 className="w-5 h-5 text-primary-500" />
          <span className="text-sm font-medium text-primary-600 uppercase tracking-wider">
            {t('orgV3.assign.kicker')}
          </span>
        </div>
        <h1 className="page-title">{t('orgV3.assign.title')}</h1>
        <p className="page-subtitle font-mono text-xs">{userId}</p>
      </div>

      <div className="glass-card p-4 mb-6">
        <div className="grid grid-cols-1 md:grid-cols-3 gap-3">
          <select value={roleId} onChange={(e) => setRoleId(e.target.value)}
            aria-label={t('orgV3.assign.role')} className="input">
            <option value="">{t('orgV3.assign.rolePh')}</option>
            {(rolesQuery.data ?? []).map((r) => (
              <option key={r.id} value={r.id}>{r.label}</option>
            ))}
          </select>
          <select value={nodeId} onChange={(e) => setNodeId(e.target.value)}
            aria-label={t('orgV3.assign.node')} className="input">
            <option value="">{t('orgV3.assign.nodeTenant')}</option>
            {(tree.data ?? []).map((n) => (
              <option key={n.id} value={n.id}>{n.levelName ? `${n.name} · ${n.levelName}` : n.name}</option>
            ))}
          </select>
          <button type="button" onClick={add} disabled={!roleId || assign.isPending}
            className="btn btn-primary inline-flex items-center justify-center gap-2">
            {assign.isPending ? <Loader2 className="w-4 h-4 animate-spin" /> : <Plus className="w-4 h-4" />}
            {t('orgV3.assign.add')}
          </button>
        </div>
      </div>

      <div className="glass-card p-2">
        {assignments.isLoading && (
          <div className="flex justify-center py-10 text-gray-400"><Loader2 className="w-6 h-6 animate-spin" /></div>
        )}
        {!assignments.isLoading && items.length === 0 && (
          <p className="text-sm text-gray-400 text-center py-8">{t('orgV3.assign.empty')}</p>
        )}
        <ul className="divide-y divide-gray-100 dark:divide-gray-800">
          {items.map((a) => (
            <li key={a.id} className="flex items-center gap-3 px-3 py-2.5 text-sm">
              <span className="flex-1 text-gray-800 dark:text-gray-100">
                {roleLabelById.get(a.roleId) ?? a.roleId}
              </span>
              <span className="text-xs text-gray-400">
                {a.nodeId ? (nodeNameById.get(a.nodeId) ?? a.nodeId) : t('orgV3.assign.nodeTenant')}
              </span>
              <button type="button" aria-label={t('orgV3.assign.end')}
                onClick={() => end.mutateAsync(a.id).then(
                  () => toast.success(t('orgV3.assign.ended')),
                  () => toast.error(t('orgV3.assign.endErr')))}
                className="p-1 text-red-400 hover:text-red-600">
                <Trash2 className="w-4 h-4" />
              </button>
            </li>
          ))}
        </ul>
      </div>
    </div>
  );
}
