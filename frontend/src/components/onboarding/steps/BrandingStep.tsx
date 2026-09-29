// B1 — Étape 4 : identité visuelle. Action serveur : TenantSettingsService.updateBranding.
import { useState } from 'react';
import { useI18n, tText } from '@/i18n';
import type { BrandingData } from '@/types/onboarding';
import type { StepProps } from './StepProps';

const HEX_RE = /^#[0-9a-fA-F]{6}$/;

export default function BrandingStep({ step, disabled, onSubmit }: StepProps) {
  const { t } = useI18n();
  const prior = (step.completedData ?? {}) as Partial<BrandingData>;
  const [form, setForm] = useState<BrandingData>({
    primaryColor: prior.primaryColor ?? '#1a7f5a',
    logoUrl: prior.logoUrl ?? '',
    allowDarkMode: prior.allowDarkMode ?? true,
  });
  const [error, setError] = useState<string | null>(null);

  const submit = (e: React.FormEvent) => {
    e.preventDefault();
    if (form.primaryColor && !HEX_RE.test(form.primaryColor)) {
      setError(tText('La couleur doit être au format #RRGGBB.'));
      return;
    }
    if (form.logoUrl && !/^https?:\/\//i.test(form.logoUrl)) {
      setError(tText("L'URL du logo doit commencer par http:// ou https://"));
      return;
    }
    setError(null);
    onSubmit(form as unknown as Record<string, unknown>);
  };

  return (
    <form onSubmit={submit} className="glass-card p-6 space-y-4" noValidate>
      <div>
        <label htmlFor="primaryColor" className="block text-sm font-medium mb-1">
          {t('Couleur principale')}
        </label>
        <div className="flex gap-2">
          <input
            id="primaryColor" type="color" value={form.primaryColor}
            onChange={(e) => setForm((f) => ({ ...f, primaryColor: e.target.value }))}
            disabled={disabled}
            className="h-11 w-16 rounded border border-gray-300 dark:border-gray-700 bg-transparent"
          />
          <input
            aria-label={tText('Couleur principale (hex)')}
            value={form.primaryColor}
            onChange={(e) => setForm((f) => ({ ...f, primaryColor: e.target.value }))}
            disabled={disabled}
            className="flex-1 rounded-lg border border-gray-300 dark:border-gray-700 bg-white dark:bg-gray-900 px-3 py-2 min-h-[44px] font-mono"
          />
        </div>
      </div>
      <div>
        <label htmlFor="logoUrl" className="block text-sm font-medium mb-1">{t('URL du logo')}</label>
        <input
          id="logoUrl" type="url" value={form.logoUrl ?? ''}
          onChange={(e) => setForm((f) => ({ ...f, logoUrl: e.target.value }))}
          disabled={disabled}
          className="w-full rounded-lg border border-gray-300 dark:border-gray-700 bg-white dark:bg-gray-900 px-3 py-2 min-h-[44px]"
        />
      </div>
      <label className="flex items-center gap-2 min-h-[44px] cursor-pointer">
        <input
          id="allowDarkMode" type="checkbox" checked={form.allowDarkMode ?? true}
          onChange={(e) => setForm((f) => ({ ...f, allowDarkMode: e.target.checked }))}
          disabled={disabled}
          className="w-5 h-5"
        />
        <span className="text-sm">{t('Autoriser le mode sombre')}</span>
      </label>
      {error && <p role="alert" className="text-sm text-red-500">{error}</p>}
      <button type="submit" disabled={disabled} className="btn-primary btn-sm min-h-[44px]">
        {tText('Enregistrer')}
      </button>
    </form>
  );
}
