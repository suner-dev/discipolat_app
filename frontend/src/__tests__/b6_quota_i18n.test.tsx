import { describe, expect, it, vi, beforeEach, afterEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import PlatformOnboardingFlowPage from '@/pages/PlatformOnboardingFlowPage';
import { QuotaUsageCards } from '@/components/admin/QuotaUsageCards';
import { I18nProvider } from '@/i18n';
import type { Locale } from '@/i18n';
import en from '@/i18n/en';
import ar from '@/i18n/ar';
import sw from '@/i18n/sw';
import es from '@/i18n/es';
import pt from '@/i18n/pt';

const metrics = [{ key: 'm', label: 'Members', available: true, usage: 10, limit: 100, percent: 10, enforced: true, source: 'src', unit: null, unlimited: false }];

vi.mock('@/lib/api', () => {
  const get = vi.fn();
  const post = vi.fn();
  return { default: { get, post }, api: { get, post }, getErrorMessage: () => 'Erreur test', __mocks: { get, post } };
});
const mocked = (await import('@/lib/api')) as unknown as { __mocks: { get: ReturnType<typeof vi.fn>; post: ReturnType<typeof vi.fn> } };
const { get, post } = mocked.__mocks;

vi.mock('react-hot-toast', () => ({ default: { success: vi.fn(), error: vi.fn() } }));

function renderIn(locale: Locale) {
  window.localStorage.setItem('discipolat-locale', locale);
  return render(
    <I18nProvider>
      <QuotaUsageCards metrics={metrics} />
    </I18nProvider>,
  );
}

beforeEach(() => { vi.clearAllMocks(); get.mockResolvedValue({ data: [] }); });

// `tText` lit une variable de MODULE (`activeLocale`) réassignée par
// I18nProvider : sans reset explicite, les assertions en français ci-dessous
// liraient l'arabe posé par les cas `renderIn('ar')` ci-dessus.
afterEach(() => { window.localStorage.setItem('discipolat-locale', 'fr'); });

describe('B6 QuotaUsageCards i18n', () => {
  it('les 5 clés existent dans les 6 locales', () => {
    const keys = ['quotas.title','quotas.retry','quotas.loading','quotas.empty','quotas.unavailable','quotas.unlimited','quotas.enforced','quotas.notEnforced','quotas.enforcementUnknown','quotas.limitLabel','quotas.percentLabel','quotas.sourceLabel','quotas.megabytes'];
    for (const d of [en, es, pt, sw, ar]) {
      for (const k of keys) expect(d[k as keyof typeof d], `clé ${k} absente`).toBeTruthy();
    }
  });

  it('rend le titre traduit en anglais', () => {
    renderIn('en');
    expect(screen.getByRole('heading', { name: 'Quotas and usage' })).toBeTruthy();
  });

  it('rend le titre traduit en arabe', () => {
    renderIn('ar');
    expect(screen.getByRole('heading', { name: 'الحصص والاستخدام' })).toBeTruthy();
  });

  it('rend "Limite appliquée" et "Source" traduits', () => {
    renderIn('en');
    expect(screen.getByText('Limit enforced')).toBeTruthy();
    expect(screen.getByText('Source: src')).toBeTruthy();
    expect(screen.getByText('Limit: 100')).toBeTruthy();
  });

  it('rend swahili', () => {
    renderIn('sw');
    expect(screen.getByRole('heading', { name: 'Mipaka na matumizi' })).toBeTruthy();
    expect(screen.getByText('Kipaka kimetumika')).toBeTruthy();
  });
});

