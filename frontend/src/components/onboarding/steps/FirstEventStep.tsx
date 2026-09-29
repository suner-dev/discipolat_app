// B1 — Étape 6 : premier événement.
// Action serveur : EventService.create (titre, dateDebut, lieu, statut="PLANIFIE").
import { useState } from 'react';
import { useI18n, tText } from '@/i18n';
import type { FirstEventData } from '@/types/onboarding';
import type { StepProps } from './StepProps';

export default function FirstEventStep({ step, disabled, onSubmit }: StepProps) {
  const { t } = useI18n();
  const prior = (step.completedData ?? {}) as Partial<FirstEventData>;
  const [form, setForm] = useState<FirstEventData>({
    title: prior.title ?? '',
    startAt: prior.startAt ? prior.startAt.slice(0, 16) : '',
    location: prior.location ?? '',
  });
  const [error, setError] = useState<string | null>(null);

  const submit = (e: React.FormEvent) => {
    e.preventDefault();
    const title = form.title.trim();
    if (title.length < 2 || title.length > 160) {
      setError(tText('Le titre doit contenir entre 2 et 160 caractères.'));
      return;
    }
    if (!form.startAt) {
      setError(tText('La date de début est obligatoire.'));
      return;
    }
    // datetime-local donne une heure locale sans fuseau : on convertit en ISO UTC.
    const iso = new Date(form.startAt).toISOString();
    if (Number.isNaN(Date.parse(iso))) {
      setError(tText('Date invalide.'));
      return;
    }
    if (Date.parse(iso) <= Date.now()) {
      setError(tText('La date doit être dans le futur.'));
      return;
    }
    setError(null);
    onSubmit({ title, startAt: iso, location: form.location?.trim() || undefined });
  };

  return (
    <form onSubmit={submit} className="glass-card p-6 space-y-4" noValidate>
      <div>
        <label htmlFor="title" className="block text-sm font-medium mb-1">
          {t('Titre de l\u2019événement')} <span className="text-red-400">*</span>
        </label>
        <input
          id="title" value={form.title} onChange={(e) => setForm((f) => ({ ...f, title: e.target.value }))}
          disabled={disabled} required minLength={2} maxLength={160}
          aria-invalid={error ? true : undefined} aria-describedby={error ? 'title-error' : undefined}
          className="w-full rounded-lg border border-gray-300 dark:border-gray-700 bg-white dark:bg-gray-900 px-3 py-2 min-h-[44px]"
        />
      </div>
      <div>
        <label htmlFor="startAt" className="block text-sm font-medium mb-1">
          {t('Date et heure de début')} <span className="text-red-400">*</span>
        </label>
        <input
          id="startAt" type="datetime-local" value={form.startAt}
          onChange={(e) => setForm((f) => ({ ...f, startAt: e.target.value }))}
          disabled={disabled} required
          className="w-full rounded-lg border border-gray-300 dark:border-gray-700 bg-white dark:bg-gray-900 px-3 py-2 min-h-[44px]"
        />
      </div>
      <div>
        <label htmlFor="location" className="block text-sm font-medium mb-1">{t('Lieu')}</label>
        <input
          id="location" value={form.location ?? ''}
          onChange={(e) => setForm((f) => ({ ...f, location: e.target.value }))}
          disabled={disabled}
          className="w-full rounded-lg border border-gray-300 dark:border-gray-700 bg-white dark:bg-gray-900 px-3 py-2 min-h-[44px]"
        />
      </div>
      {error && <p id="title-error" role="alert" className="text-sm text-red-500">{error}</p>}
      <button type="submit" disabled={disabled} className="btn-primary btn-sm min-h-[44px]">
        {tText('Créer l\u2019événement')}
      </button>
    </form>
  );
}
