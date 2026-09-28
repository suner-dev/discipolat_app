// B1 — Étape 0 : identité de l'église. Action serveur : renommage de l'église racine.
import { useState } from 'react';
import { useI18n, tText } from '@/i18n';
import type { ChurchIdentityData } from '@/types/onboarding';
import type { StepProps } from './StepProps';

export default function ChurchIdentityStep({ step, disabled, onSubmit }: StepProps) {
  const { t } = useI18n();
  const prior = (step.completedData ?? {}) as Partial<ChurchIdentityData>;
  const [form, setForm] = useState<ChurchIdentityData>({
    churchName: prior.churchName ?? '',
    businessName: prior.businessName ?? '',
    city: prior.city ?? '',
    phone: prior.phone ?? '',
    email: prior.email ?? '',
    timezone: prior.timezone ?? '',
    currency: prior.currency ?? '',
  });
  const [error, setError] = useState<string | null>(null);

  const set = (k: keyof ChurchIdentityData) => (e: { target: { value: string } }) =>
    setForm((f) => ({ ...f, [k]: e.target.value }));

  const submit = (e: React.FormEvent) => {
    e.preventDefault();
    const name = form.churchName.trim();
    if (name.length < 2 || name.length > 120) {
      setError(tText('Le nom doit contenir entre 2 et 120 caractères.'));
      return;
    }
    if (form.email && !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(form.email)) {
      setError(tText('Adresse email invalide.'));
      return;
    }
    setError(null);
    onSubmit(form as unknown as Record<string, unknown>);
  };

  const label = (k: string) => `${t(k)}:`;
  return (
    <form onSubmit={submit} className="glass-card p-6 space-y-4" noValidate>
      <div>
        <label htmlFor="churchName" className="block text-sm font-medium mb-1">
          {label('Nom de l\u2019église')} <span className="text-red-400">*</span>
        </label>
        <input
          id="churchName"
          name="churchName"
          value={form.churchName}
          onChange={set('churchName')}
          disabled={disabled}
          required
          minLength={2}
          maxLength={120}
          aria-invalid={error ? true : undefined}
          aria-describedby={error ? 'churchName-error' : undefined}
          className="w-full rounded-lg border border-gray-300 dark:border-gray-700 bg-white dark:bg-gray-900 px-3 py-2 min-h-[44px]"
        />
      </div>
      {[
        { id: 'businessName', k: 'Nom commercial' },
        { id: 'city', k: 'Ville' },
        { id: 'phone', k: 'Téléphone' },
        { id: 'email', k: 'Email', type: 'email' },
        { id: 'timezone', k: 'Fuseau horaire (IANA)' },
        { id: 'currency', k: 'Devise (ISO-4217)' },
      ].map((f) => (
        <div key={f.id}>
          <label htmlFor={f.id} className="block text-sm font-medium mb-1">{label(f.k)}</label>
          <input
            id={f.id}
            name={f.id}
            type={f.type ?? 'text'}
            value={form[f.id as keyof ChurchIdentityData] ?? ''}
            onChange={set(f.id as keyof ChurchIdentityData)}
            disabled={disabled}
            className="w-full rounded-lg border border-gray-300 dark:border-gray-700 bg-white dark:bg-gray-900 px-3 py-2 min-h-[44px]"
          />
        </div>
      ))}
      {error && (
        <p id="churchName-error" role="alert" className="text-sm text-red-500">{error}</p>
      )}
      <button type="submit" disabled={disabled} className="btn-primary btn-sm min-h-[44px]">
        {tText('Enregistrer')}
      </button>
    </form>
  );
}
