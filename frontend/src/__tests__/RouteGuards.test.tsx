import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { MemoryRouter, Routes, Route } from 'react-router-dom';
import { I18nProvider } from '@/i18n';
import api from '@/lib/api';
import type { TenantContextValue } from '@/types/tenant';
import {
  RequireAuth,
  RequireTenantAccess,
  RequireScope,
  RequireRole,
  RequireFeature,
  RequirePermission,
  PermissionGate,
} from '@/components/guards/RouteGuardsG54'; // PORT Develop1 : jeu de gardes §G5.4

// -- Mocks -------------------------------------------------------------------
vi.mock('@/lib/api', () => ({
  default: { get: vi.fn(), put: vi.fn(), post: vi.fn() },
  getErrorMessage: vi.fn().mockReturnValue('Erreur'),
}));

const mockAuth = { isAuthenticated: true, isLoading: false };
vi.mock('@/contexts/AuthContext', () => ({
  useAuth: () => mockAuth,
}));

// Fake de TenantContext piloté jeu de rôles par jeu de rôles (§55-4).
const fakeTenant: Partial<TenantContextValue> = {
  isInitialized: true,
  isLoading: false,
  currentTenant: { id: 't1', name: 'Église Béréa' } as never,
  currentMembership: { id: 'm1', role: 'MEMBRE' } as never,
};
function tenantWith(perms: string[], roles: string[], features: Record<string, boolean>) {
  fakeTenant.currentMembership = { id: 'm1', role: roles[0] ?? 'MEMBRE' } as never;
  fakeTenant.hasPermission = (p: string) => perms.includes(p.toUpperCase());
  fakeTenant.canAccess = (r: string, a: string) =>
    perms.includes(`${r.toUpperCase()}_${a.toUpperCase()}`);
  fakeTenant.hasRole = (r: string) => roles.includes(r.toUpperCase());
  fakeTenant.isFeatureBlocked = (f: string) => features[f] === false || features[f.toUpperCase()] === false;
}
vi.mock('@/contexts/TenantContext', () => ({
  useTenant: () => fakeTenant,
}));

beforeEach(() => {
  vi.clearAllMocks();
  fakeTenant.isInitialized = true;
  fakeTenant.currentTenant = { id: 't1', name: 'Église Béréa' } as never;
  mockAuth.isAuthenticated = true;
  mockAuth.isLoading = false;
});

function renderWithRouter(ui: React.ReactElement) {
  return render(
    <I18nProvider>
      <MemoryRouter initialEntries={['/protected']}>
        <Routes>
          <Route path="/protected" element={ui} />
          <Route path="/login" element={<p>Page de connexion</p>} />
        </Routes>
      </MemoryRouter>
    </I18nProvider>,
  );
}

const Child = () => <p>Contenu protégé</p>;

describe('RouteGuards G5.4 — jeux de rôles (§55-4)', () => {
  it('MEMBRE sans ORG_NODE_MOVE : écran Accès refusé + Demander l\'accès notifie le serveur', async () => {
    tenantWith(['EVENT_VIEW'], ['MEMBRE'], {});
    renderWithRouter(
      <RequireScope resource="ORG_NODE" action="MOVE" resourceLabel="Arbre d'organisation">
        <Child />
      </RequireScope>,
    );
    expect(screen.queryByText('Contenu protégé')).not.toBeInTheDocument();
    expect(screen.getByText('Accès refusé')).toBeInTheDocument();
    expect(screen.getByText(/ORG_NODE_MOVE/)).toBeInTheDocument();

    (api.post as ReturnType<typeof vi.fn>).mockResolvedValue({ data: { status: 'NOTIFIED' } });
    fireEvent.click(screen.getByRole('button', { name: /Demander l'accès/ }));
    await waitFor(() => {
      expect(api.post).toHaveBeenCalledWith('/access-requests', {
        permissionKey: 'ORG_NODE_MOVE',
        resourceLabel: "Arbre d'organisation",
      });
    });
  });

  it('ADMIN avec ORG_NODE_MOVE : passe', () => {
    tenantWith(['ORG_NODE_MOVE'], ['ADMIN'], {});
    renderWithRouter(
      <RequireScope resource="ORG_NODE" action="MOVE">
        <Child />
      </RequireScope>,
    );
    expect(screen.getByText('Contenu protégé')).toBeInTheDocument();
  });

  it('RESPONSABLE : RequireRole admet les rôles résolus serveur', () => {
    tenantWith([], ['RESPONSABLE'], {});
    renderWithRouter(
      <RequireRole roles={['ADMIN', 'PASTEUR', 'RESPONSABLE']}>
        <Child />
      </RequireRole>,
    );
    expect(screen.getByText('Contenu protégé')).toBeInTheDocument();

    tenantWith([], ['MEMBRE'], {});
    renderWithRouter(
      <RequireRole roles={['ADMIN']}>
        <Child />
      </RequireRole>,
    );
    expect(screen.getByText('Accès refusé')).toBeInTheDocument();
    expect(screen.getByText(/Rôles acceptés/)).toBeInTheDocument();
  });

  it('CHEF_DE_FAMILLE avec PERMISSION VIEW : RequirePermission passe sans écran de refus', () => {
    tenantWith(['FAMILY_VIEW'], ['CHEF_DE_FAMILLE'], {});
    renderWithRouter(
      <RequirePermission permission="FAMILY_VIEW">
        <Child />
      </RequirePermission>,
    );
    expect(screen.getByText('Contenu protégé')).toBeInTheDocument();
  });

  it('RequireFeature : refus seulement sur drapeau serveur false (miroir du 403), jamais sur absence', () => {
    tenantWith([], ['MEMBRE'], { FINANCES: false });
    renderWithRouter(
      <RequireFeature feature="FINANCES">
        <Child />
      </RequireFeature>,
    );
    expect(screen.getByText('Fonctionnalité non incluse')).toBeInTheDocument();
    expect(screen.getByText(/FINANCES/)).toBeInTheDocument();

    // Absence de drapeau = module non géré → on laisse passer (backend seul maître)
    tenantWith([], ['MEMBRE'], {});
    renderWithRouter(
      <RequireFeature feature="FINANCES">
        <Child />
      </RequireFeature>,
    );
    expect(screen.getByText('Contenu protégé')).toBeInTheDocument();
  });

  it('RequireTenantAccess : pas de tenant actif → invitation à choisir une organisation', () => {
    fakeTenant.currentTenant = null;
    tenantWith([], ['MEMBRE'], {});
    renderWithRouter(
      <RequireTenantAccess>
        <Child />
      </RequireTenantAccess>,
    );
    expect(screen.getByText('Aucune organisation sélectionnée')).toBeInTheDocument();
    expect(screen.getByRole('link', { name: /Choisir une organisation/ })).toHaveAttribute(
      'href',
      '/tenant-switcher',
    );
    fakeTenant.currentTenant = { id: 't1', name: 'Église Béréa' } as never;
  });

  it('RequireAuth : session expirée → redirection /login', () => {
    mockAuth.isAuthenticated = false;
    renderWithRouter(
      <RequireAuth>
        <Child />
      </RequireAuth>,
    );
    expect(screen.getByText('Page de connexion')).toBeInTheDocument();
  });

  it('PermissionGate mode disable : action visible mais inerte avec tooltip (jamais de bouton mort silencieux)', () => {
    tenantWith(['EVENT_VIEW'], ['FAISEUR'], {});
    render(
      <I18nProvider>
        <MemoryRouter>
          <PermissionGate permission="FINANCE_EXPORT" mode="disable">
            <button type="button">Exporter</button>
          </PermissionGate>
        </MemoryRouter>
      </I18nProvider>,
    );
    const wrapper = screen.getByRole('button', { name: 'Exporter' }).parentElement!;
    expect(wrapper).toHaveAttribute('aria-disabled', 'true');
    expect(wrapper).toHaveAttribute('title', expect.stringContaining('FINANCE_EXPORT'));
  });
});
