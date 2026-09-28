// B1 — Étape 3 : inviter les responsables.
// Action serveur : InvitationService.createInvitation pour chaque entrée.
import { useState } from 'react';
import { Plus, X } from 'lucide-react';
import { tText } from '@/i18n';
import type { RoleInvitation, RolesData } from '@/types/onboarding';
import type { StepProps } from './StepProps';

const EMAIL_RE = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

export default function RolesStep({ step, disabled, onSubmit }: StepProps) {
  const prior = (step.completedData ?? {}) as Partial<RolesData>;
  const [rows, setRows] = useState<RoleInvitation[]>(prior.invitations ?? [{ email: '', role: 'PASTEUR' }]);
  const [error, setError] = useState<string | null>(null);

  const update = (i: number, patch: Partial<RoleInvitation>) =>
    setRows((r) => r.map((x, j) => (j === i ? { ...x, ...patch } : x)));

  const submit = (e: React.FormEvent) => {
    e.preventDefault();
    const clean = rows
      .map((r) => ({ email: r.email.trim().toLowerCase(), role: r.role.trim().toUpperCase() }))
      .filter((r) => r.email || r.role);
    if (clean.length === 0) {
      setError(tText('Ajoutez au moins une invitation, ou ignorez cette étape avec un motif.'));
      return;
    }
    const bad = clean.find((r) => !EMAIL_RE.test(r.email) || !r.role);
    if (bad) {
      setError(tText('Chaque ligne doit avoir un email valide et un rôle.'));
      return;
    }
    setError(null);
    onSubmit({ invitations: clean });
  };

  return (
    <form onSubmit={submit} className="glass-card p-6 space-y-4" noValidate>
      {rows.map((r, i) => (
        <div key={i} className="flex flex-col sm:flex-row gap-2">
          <input
            aria-label={tText('Email de l\u2019invité')}
            type="email" value={r.email}
            onChange={(e) => update(i, { email: e.target.value })}
            disabled={disabled}
            className="flex-1 rounded-lg border border-gray-300 dark:border-gray-700 bg-white dark:bg-gray-900 px-3 py-2 min-h-[44px]"
          />
          <input
            aria-label={tText('Rôle')}
            value={r.role}
            onChange={(e) => update(i, { role: e.target.value })}
            disabled={disabled}
            className="sm:w-48 rounded-lg border border-gray-300 dark:border-gray-700 bg-white dark:bg-gray-900 px-3 py-2 min-h-[44px]"
          />
          <button
            type="button" aria-label={tText('Retirer')}
            onClick={() => setRows((rs) => rs.filter((_, j) => j !== i))}
            className="px-3 rounded-lg border border-gray-300 dark:border-gray-700 min-h-[44px] min-w-[44px] flex items-center justify-center"
          >
            <X className="w-4 h-4" aria-hidden="true" />
          </button>
        </div>
      ))}
      <button
        type="button" onClick={() => setRows((r) => [...r, { email: '', role: '' }])}
        className="btn-sm rounded-lg border border-gray-300 dark:border-gray-700 px-3 min-h-[44px] inline-flex items-center gap-1"
      >
        <Plus className="w-4 h-4" aria-hidden="true" /> {tText('Ajouter une invitation')}
      </button>
      {error && <p role="alert" className="text-sm text-red-500">{error}</p>}
      <button type="submit" disabled={disabled} className="btn-primary btn-sm min-h-[44px]">
        {tText('Envoyer les invitations')}
      </button>
    </form>
  );
}
