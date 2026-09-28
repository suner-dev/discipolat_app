// B1 — Étape 2 : familles et départements.
// Action serveur : DepartmentService.create puis FamilyService.create (avec quota).
// Si aucune liste n'est fournie, le backend vérifie l'existence préalable.
import { useState } from 'react';
import { Plus, X } from 'lucide-react';
import { useI18n, tText } from '@/i18n';
import type { StructureData } from '@/types/onboarding';
import type { StepProps } from './StepProps';

function ListEditor({
  id, legend, values, onChange, disabled, addLabel,
}: {
  id: string; legend: string; values: string[]; onChange: (v: string[]) => void;
  disabled: boolean; addLabel: string;
}) {
  return (
    <fieldset className="space-y-2" disabled={disabled}>
      <legend className="text-sm font-medium mb-1">{legend}</legend>
      {values.map((v, i) => (
        <div key={i} className="flex gap-2">
          <input
            aria-label={`${legend} ${i + 1}`}
            value={v}
            onChange={(e) => onChange(values.map((x, j) => (j === i ? e.target.value : x)))}
            className="flex-1 rounded-lg border border-gray-300 dark:border-gray-700 bg-white dark:bg-gray-900 px-3 py-2 min-h-[44px]"
          />
          <button
            type="button" aria-label={tText('Retirer')}
            onClick={() => onChange(values.filter((_, j) => j !== i))}
            className="px-3 rounded-lg border border-gray-300 dark:border-gray-700 min-h-[44px] min-w-[44px] flex items-center justify-center"
          >
            <X className="w-4 h-4" aria-hidden="true" />
          </button>
        </div>
      ))}
      <button
        type="button" id={id} onClick={() => onChange([...values, ''])}
        className="btn-sm rounded-lg border border-gray-300 dark:border-gray-700 px-3 min-h-[44px] inline-flex items-center gap-1"
      >
        <Plus className="w-4 h-4" aria-hidden="true" /> {addLabel}
      </button>
    </fieldset>
  );
}

export default function StructureStep({ step, disabled, onSubmit }: StepProps) {
  const { t } = useI18n();
  const prior = (step.completedData ?? {}) as Partial<StructureData>;
  const [departments, setDepartments] = useState<string[]>(prior.departments ?? []);
  const [families, setFamilies] = useState<string[]>(prior.families ?? []);
  const [error, setError] = useState<string | null>(null);

  const submit = (e: React.FormEvent) => {
    e.preventDefault();
    const clean = (a: string[]) => a.map((s) => s.trim()).filter(Boolean);
    const d = clean(departments);
    const f = clean(families);
    if (d.length === 0 && f.length === 0) {
      setError(tText('Ajoutez au moins un département ou une famille (ou ignorez avec un motif).'));
      return;
    }
    setError(null);
    onSubmit({ departments: d, families: f });
  };

  return (
    <form onSubmit={submit} className="glass-card p-6 space-y-5" noValidate>
      <ListEditor id="add-department" legend={t('Départements')} values={departments}
        onChange={setDepartments} disabled={disabled} addLabel={tText('Ajouter un département')} />
      <ListEditor id="add-family" legend={t('Familles')} values={families}
        onChange={setFamilies} disabled={disabled} addLabel={tText('Ajouter une famille')} />
      {error && <p role="alert" className="text-sm text-red-500">{error}</p>}
      <button type="submit" disabled={disabled} className="btn-primary btn-sm min-h-[44px]">
        {tText('Enregistrer')}
      </button>
    </form>
  );
}
