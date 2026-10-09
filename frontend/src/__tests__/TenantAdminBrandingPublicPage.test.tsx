import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { render, screen, fireEvent, waitFor, cleanup } from '@testing-library/react';
import { I18nProvider } from '@/i18n';
import TenantAdminBrandingPage from '@/pages/TenantAdminBrandingPage';

/**
 * LOT 2 §GLISE-D'ABORD (T2.5) — onglet admin « Page publique ».
 *
 * <p>Le contrat verrouillé ici n'est PAS l'aspect, ce sont les promesses du plan :</p>
 * <ul>
 *   <li><b>A1 (rien de supprimé)</b> : les cinq onglets existants restent rendus ;
 *       on n'AJOUTE qu'un sixième.</li>
 *   <li><b>A5/A7 (dark launch)</b> : l'interrupteur suit `landing_enabled` du
 *       serveur, `false` par défaut — aucune page publique ne s'active toute seule.</li>
 *   <li><b>A3 (réutilisation)</b> : le lien partageable applique la CONVENTION
 *       d'URL vanity existante, transposée sur le chemin `/e/:slug` de la landing.</li>
 *   <li><b>R2 (double consentement)</b> : l'avertissement « annuaire + landing »
 *       est présent — publier la page ne dispense pas d'être listé.</li>
 * </ul>
 *
 * <p>Rendus avec le VRAI `I18nProvider` (locale par défaut FR) : on teste les
 * chaînes réellement affichées, pas des clés brutes.</p>
 */

const { apiGet, apiPut, mockUseTenant } = vi.hoisted(() => ({
  apiGet: vi.fn(),
  apiPut: vi.fn(),
  mockUseTenant: vi.fn(),
}));

vi.mock('@/lib/api', () => ({
  default: {
    get: apiGet,
    put: apiPut,
    post: vi.fn(),
    delete: vi.fn(),
    interceptors: { request: { use: vi.fn() }, response: { use: vi.fn() } },
    defaults: { headers: { common: {} } },
  },
  getErrorMessage: vi.fn().mockReturnValue('Erreur'),
}));

vi.mock('@/contexts/TenantContext', () => ({ useTenant: mockUseTenant }));

// NodeThemePanel s'appuie sur react-query + l'API V3 : on les neutralise.
// IMPORTANT : les hooks mockés doivent renvoyer des IDENTITÉS STABLES. Sinon
// NodeThemePanel (`useEffect(…, [nodeId, theme.data])`) verrait une nouvelle
// référence à chaque rendu, re-déclencherait setColors → boucle de rendu infinie
// → épuisement mémoire du worker. Les constantes sont créées DANS la factory
// (exécutée une seule fois et mise en cache) pour éviter la TDZ du hoisting.
vi.mock('@/hooks/useOrganizationV3', () => {
  const tree = { data: [], isLoading: false };
  const theme = { data: {} };
  const patch = { mutateAsync: vi.fn(), isPending: false };
  return {
    useOrgTreeV3: () => tree,
    useNodeTheme: () => theme,
    usePatchNodeTheme: () => patch,
  };
});

class FakeWebSocket {
  onopen: (() => void) | null = null;
  onmessage: (() => void) | null = null;
  send() {}
  close() {}
}

function settings(over: Record<string, unknown> = {}) {
  return {
    businessName: 'Église Bethel', slogan: 'Une famille pour toujours',
    primaryColor: '#6366F1', secondaryColor: '#8B5CF6', accentColor: '#EC4899',
    surfaceColor: '#FFFFFF', backgroundColor: '#F8FAFC',
    textPrimaryColor: '#1E293B', textSecondaryColor: '#64748B',
    successColor: '#10B981', warningColor: '#F59E0B', errorColor: '#EF4444', infoColor: '#3B82F6',
    primaryFont: 'Inter', secondaryFont: 'Inter', headingFont: 'Inter', monoFont: 'JetBrains Mono',
    customCss: '', customHeadHtml: '', landingEnabled: false, landingSections: [],
    ...over,
  };
}

function renderPage(slug = 'bethel') {
  mockUseTenant.mockReturnValue({
    currentTenant: { id: 'tenant-1', slug },
    hasPermission: () => true,
  });
  return render(
    <I18nProvider>
      <TenantAdminBrandingPage />
    </I18nProvider>,
  );
}

async function openPublicTab() {
  fireEvent.click(await screen.findByRole('button', { name: 'Page publique' }));
}

beforeEach(() => {
  vi.stubGlobal('WebSocket', FakeWebSocket);
  apiGet.mockResolvedValue({ data: settings() });
  apiPut.mockResolvedValue({ data: settings() });
});

afterEach(() => {
  cleanup();
  vi.unstubAllGlobals();
  vi.clearAllMocks();
});

describe("LOT 2 §GLISE-D'ABORD (T2.5) — onglet admin « Page publique »", () => {
  it('A1 : ajoute un onglet sans retirer les cinq existants', async () => {
    renderPage();
    for (const label of ['Couleurs', 'Identité', 'Polices', 'Contact', 'Avancé', 'Page publique']) {
      expect(await screen.findByRole('button', { name: label })).toBeInTheDocument();
    }
  });

  it('A5/A7 : l\'interrupteur suit landing_enabled du serveur (false par défaut)', async () => {
    apiGet.mockResolvedValue({ data: settings({ landingEnabled: false }) });
    renderPage();
    await openPublicTab();
    expect(screen.getByRole('checkbox')).not.toBeChecked();
  });

  it('A5 : se rallume si le serveur reporte landing_enabled=true', async () => {
    apiGet.mockResolvedValue({ data: settings({ landingEnabled: true }) });
    renderPage();
    await openPublicTab();
    expect(screen.getByRole('checkbox')).toBeChecked();
  });

  it('A3 : le lien partageable applique la convention vanity sur /e/:slug', async () => {
    renderPage('bethel');
    await openPublicTab();
    const url = screen.getByTestId('landing-share-url') as HTMLInputElement;
    expect(url.value).toBe(`${window.location.origin}/e/bethel`);
  });

  it('A3 : le slug est encodé dans le lien', async () => {
    renderPage('bethel est');
    await openPublicTab();
    const url = screen.getByTestId('landing-share-url') as HTMLInputElement;
    expect(url.value).toBe(`${window.location.origin}/e/bethel%20est`);
  });

  it('R2 : rappelle que la publication exige AUSSI le listing annuaire', async () => {
    renderPage();
    await openPublicTab();
    expect(screen.getByText(/deux consentements distincts/)).toBeInTheDocument();
  });

  it('sans slug : pas de lien, on explique comment l\'obtenir', async () => {
    renderPage('');
    await openPublicTab();
    expect(screen.queryByTestId('landing-share-url')).not.toBeInTheDocument();
    expect(screen.getByText(/identifiant \(slug\)/)).toBeInTheDocument();
  });

  it('basculer puis enregistrer envoie landing_enabled:true (PUT /admin/settings)', async () => {
    renderPage();
    await openPublicTab();
    fireEvent.click(screen.getByRole('checkbox'));
    fireEvent.click(screen.getByRole('button', { name: 'Enregistrer' }));
    await waitFor(() => {
      expect(apiPut).toHaveBeenCalledWith('/admin/settings', expect.objectContaining({ landingEnabled: true }));
    });
  });
});
