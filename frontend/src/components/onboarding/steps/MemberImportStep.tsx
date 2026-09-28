// B1 — Étape 1 : import des membres. Action serveur : audit TENANT_MEMBERS_IMPORT.
// D4 : le wizard est déclaratif mais vérifié — importedCount >= 1, ou skip motivé.
import { useState } from 'react';
import { useI18n, tText } from '@/i18n';
import type { MemberImportData } from '@/types/onboarding';
import type { StepProps } from './StepProps';

export default function MemberImportStep({ step, disabled, onSubmit }: StepProps) {
  const { t } = useI18n();
  const prior = step.completedData as Partial<MemberImportData> | null;
  const [count, setCount] = useState<number>(prior?.importedCount ?? 0);
  const [error, setError] = useState<string | null>(null);

  const submit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!Number.isInteger(count) || count < 1) {
      setError(tText('Indiquez au moins un membre importé, ou ignorez cette étape avec un motif.'));
      return;
    }
    setError(null);
    onSubmit({ importedCount: count });
  };

  return (
    <form onSubmit={submit} className="glass-card p-6 space-y-4" noValidate>
      <p className="text-sm text-gray-600 dark:text-gray-400">{step.description}</p>
      <div>
        <label htmlFor="importedCount" className="block text-sm font-medium mb-1">
          {t('Nombre de membres importés')} <span className="text-red-400">*</span>
        </label>
        <input
          id="importedCount"
          name="importedCount"
          type="number"
          min={1}
          step={1}
          value={Number.isNaN(count) ? '' : count}
          onChange={(e) => setCount(e.target.value === '' ? NaN : Number(e.target.value))}
          disabled={disabled}
          required
          aria-invalid={error ? true : undefined}
          aria-describedby={error ? 'importedCount-error' : undefined}
          className="w-full rounded-lg border border-gray-300 dark:border-gray-700 bg-white dark:bg-gray-900 px-3 py-2 min-h-[44px]"
        />
      </div>
      {error && (
        <p id="importedCount-error" role="alert" className="text-sm text-red-500">{error}</p>
      )}
      <button type="submit" disabled={disabled} className="btn-primary btn-sm min-h-[44px]">
        {tText('Enregistrer')}
      </button>
    </form>
  );
}
