// B4 — Formulaire de création d'invitation.
//
// Le corps envoyé ne contient QUE les clés attendues par le backend
// (`email`, `role`, `scopeType`, `organizationNodeId`) : le contrat §3 est
// figé, on n'invente pas de champ.
//
// La portée ORGANIZATION n'est proposed que si l'arborescence est chargée :
// envoyer `scopeType: ORGANIZATION` sans nœud produirait une invitation
// invalide côté serveur.

import { useState } from 'react';
import { useI18n } from '@/i18n';
import { SkeletonLine } from '@/components/ui/UXComponents';
import {
  useCreateInvitation,
  useAssignableRoles,
  FALLBACK_ROLES,
  type CreateInvitationResult,
  type ScopeType,
} from '@/hooks/useInvitations';
import { useOrgNodes } from '@/hooks/useOrgNodes';
import { getErrorMessage } from '@/lib/api';
import toast from 'react-hot-toast';

interface Props {
  onClose: () => void;
  onCreated: (result: CreateInvitationResult) => void;
}

export default function InvitationCreateDialog({ onClose, onCreated }: Props) {
  const { t } = useI18n();
  const create = useCreateInvitation();
  const nodes = useOrgNodes();
  const rolesQuery = useAssignableRoles();
  // L'API fait foi ; le repli est la liste des rôles système réellement
  // seedés (V135), pas une liste inventée par l'écran.
  const roles = rolesQuery.data ?? FALLBACK_ROLES;

  const [email, setEmail] = useState('');
  const [role, setRole] = useState('');
  const [scopeType, setScopeType] = useState<ScopeType>('TENANT');
  const [nodeId, setNodeId] = useState('');

  const submit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!email || !role) return;
    if (scopeType === 'ORGANIZATION' && !nodeId) {
      toast.error(t('invitations.orgNodeRequired'));
      return;
    }
    try {
      const result = await create.mutateAsync({
        email: email.trim(),
        role,
        scopeType,
        organizationNodeId: scopeType === 'ORGANIZATION' ? nodeId : undefined,
      });
      onCreated(result);
    } catch (error) {
      // 400 métier : le backend renvoie { error, invitationId? }.
      const message =
        (error as { response?: { data?: { error?: string } } })?.response?.data?.error ??
        getErrorMessage(error);
      toast.error(message);
    }
  };

  const inputClass =
    'w-full rounded-lg border border-gray-300 dark:border-gray-700 bg-white dark:bg-gray-900 px-3 py-2 min-h-[44px]';

  return (
    <div
      className="modal-overlay"
      role="dialog"
      aria-modal="true"
      aria-label={t('invitations.new')}
      onClick={onClose}
    >
      <div className="modal-content max-w-md" onClick={(e) => e.stopPropagation()}>
        <form onSubmit={submit} className="p-6 space-y-4" noValidate>
          <h2 className="text-base font-bold text-gray-900 dark:text-gray-100">{t('invitations.new')}</h2>

          <div>
            <label htmlFor="inv-email" className="block text-sm font-medium text-gray-700 dark:text-gray-200 mb-1">
              {t('invitations.email')} <span className="text-red-400">*</span>
            </label>
            <input
              id="inv-email"
              type="email"
              required
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              placeholder="email@exemple.com"
              className={inputClass}
            />
          </div>

          <div>
            <label htmlFor="inv-role" className="block text-sm font-medium text-gray-700 dark:text-gray-200 mb-1">
              {t('invitations.role.label')} <span className="text-red-400">*</span>
            </label>
            <select
              id="inv-role"
              required
              value={role}
              onChange={(e) => setRole(e.target.value)}
              className={inputClass}
            >
              <option value="">{t('invitations.role.placeholder')}</option>
              {roles.map((r) => (
                <option key={r.key} value={r.key}>
                  {r.label}
                </option>
              ))}
            </select>
          </div>

          <fieldset>
            <legend className="block text-sm font-medium text-gray-700 dark:text-gray-200 mb-1">
              {t('invitations.scope.label')}
            </legend>
            <div className="flex flex-col gap-2">
              <label className="flex items-center gap-2 min-h-[44px]">
                <input
                  type="radio"
                  name="scopeType"
                  value="TENANT"
                  checked={scopeType === 'TENANT'}
                  onChange={() => setScopeType('TENANT')}
                />
                <span className="text-sm text-gray-700 dark:text-gray-200">{t('invitations.scope.tenant')}</span>
              </label>
              <label className="flex items-center gap-2 min-h-[44px]">
                <input
                  type="radio"
                  name="scopeType"
                  value="ORGANIZATION"
                  checked={scopeType === 'ORGANIZATION'}
                  onChange={() => setScopeType('ORGANIZATION')}
                  disabled={nodes.isLoading || (nodes.data?.length ?? 0) === 0}
                />
                <span className="text-sm text-gray-700 dark:text-gray-200">{t('invitations.scope.organization')}</span>
              </label>
            </div>
          </fieldset>

          {scopeType === 'ORGANIZATION' && (
            <div>
              <label htmlFor="inv-node" className="block text-sm font-medium text-gray-700 dark:text-gray-200 mb-1">
                {t('invitations.orgNode')} <span className="text-red-400">*</span>
              </label>
              {nodes.isLoading ? (
                <SkeletonLine className="h-11 w-full" />
              ) : (
                <select
                  id="inv-node"
                  value={nodeId}
                  onChange={(e) => setNodeId(e.target.value)}
                  className={inputClass}
                  required
                >
                  <option value="">{t('invitations.orgNodePlaceholder')}</option>
                  {(nodes.data ?? []).map((node) => (
                    <option key={node.id} value={node.id}>
                      {node.name}
                    </option>
                  ))}
                </select>
              )}
            </div>
          )}

          <div className="modal-footer">
            <button type="button" onClick={onClose} className="btn-ghost btn-sm">
              {t('common.cancel')}
            </button>
            <button type="submit" disabled={create.isPending} className="btn-primary btn-sm">
              {t('invitations.send')}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
