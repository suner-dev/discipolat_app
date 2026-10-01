import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import api from '@/lib/api';
import { useAuth } from '@/contexts/AuthContext';
import PastoralMinistryPage from '@/pages/PastoralMinistryPage';

// -- Mocks -------------------------------------------------------------------
vi.mock('@/lib/api', () => ({
  default: { get: vi.fn(), post: vi.fn(), put: vi.fn(), delete: vi.fn() },
  getErrorMessage: vi.fn().mockReturnValue('Erreur'),
}));
vi.mock('@/contexts/AuthContext', () => ({ useAuth: vi.fn() }));
vi.mock('react-hot-toast', () => ({ default: { success: vi.fn(), error: vi.fn() } }));

// -- Fixures (formes réelles de /pastorate/*, /org/tree/flat, /users) --------
const APPOINTMENTS = [
  { id: 'ap1', pastorId: 'p1', organizationUnitId: 'c1', roleCode: 'PASTEUR', title: 'Pasteur de campus', startDate: '2024-03-01', endDate: null, appointmentType: 'NOMINATION', status: 'ACTIVE', reason: null },
  { id: 'ap2', pastorId: 'p2', organizationUnitId: 'c1', roleCode: 'PASTEUR', title: null, startDate: '2020-01-01', endDate: '2024-02-01', appointmentType: 'NOMINATION', status: 'ENDED', reason: 'Transfert' },
];
const TRANSFERS = [
  { id: 'tr1', pastorId: 'p2', fromOrgUnitId: 'c1', toOrgUnitId: 'c2', transferDate: '2026-09-20', reason: 'Réorganisation', status: 'PENDING' },
];
const UNITS = [
  { id: 'r1', tenantId: 't1', parentId: null, type: 'ROOT_CHURCH', name: 'Église Centrale', code: 'ROOT', status: 'ACTIVE', path: 'ROOT', level: 0, createdAt: '' },
  { id: 'c1', tenantId: 't1', parentId: 'r1', type: 'CAMPUS', name: 'Campus Nord', code: 'C-ND', status: 'ACTIVE', path: 'ROOT.C-ND', level: 1, createdAt: '' },
  { id: 'c2', tenantId: 't1', parentId: 'r1', type: 'CAMPUS', name: 'Campus Sud', code: 'C-SD', status: 'ACTIVE', path: 'ROOT.C-SD', level: 1, createdAt: '' },
];
const PASTORS = [
  { id: 'p1', firstName: 'André', lastName: 'Mbala', email: 'a@x.cd' },
  { id: 'p2', firstName: 'Grégoire', lastName: 'K.', email: 'g@x.cd' },
];

function stubApi() {
  (api.get as ReturnType<typeof vi.fn>).mockImplementation((url: string) => {
    if (url === '/pastorate/appointments') return Promise.resolve({ data: APPOINTMENTS });
    if (url === '/pastorate/transfers') return Promise.resolve({ data: TRANSFERS });
    if (url === '/org/tree/flat') return Promise.resolve({ data: UNITS });
    if (url.startsWith('/users?role=PASTEUR')) return Promise.resolve({ data: { content: PASTORS } });
    return Promise.resolve({ data: null });
  });
  (api.post as ReturnType<typeof vi.fn>).mockResolvedValue({ data: {} });
  (api.delete as ReturnType<typeof vi.fn>).mockResolvedValue({ data: {} });
}

function renderPage(activeRole: string | undefined) {
  (useAuth as ReturnType<typeof vi.fn>).mockReturnValue({
    isAuthenticated: true,
    user: { id: 'u0', activeRole },
  });
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return render(
    <QueryClientProvider client={queryClient}>
      <PastoralMinistryPage />
    </QueryClientProvider>,
  );
}

beforeEach(() => {
  vi.clearAllMocks();
  stubApi();
});

describe('PastoralMinistryPage (G4.3)', () => {
  it('renders active and past mandates from real endpoints', async () => {
    renderPage('PASTEUR');
    await waitFor(() => {
      expect(api.get).toHaveBeenCalledWith('/pastorate/appointments');
      expect(api.get).toHaveBeenCalledWith('/pastorate/transfers');
    });
    // Pastor + unit names resolved via /users and /org/tree/flat fixtures
    expect(await screen.findByText('André Mbala')).toBeInTheDocument();
    expect(screen.getAllByText(/Campus Nord/).length).toBeGreaterThan(0);
    expect(screen.getByText('Grégoire K.')).toBeInTheDocument(); // mandat passé
  });

  it('hides write actions for PASTEUR (read-only, parity with @PreAuthorize)', async () => {
    renderPage('PASTEUR');
    await screen.findByText('André Mbala');
    expect(screen.queryByTestId('pastoral-create-btn')).not.toBeInTheDocument();
    expect(screen.queryByTestId('pastoral-end-ap1')).not.toBeInTheDocument();
    expect(screen.queryByTestId('transfer-create-btn')).not.toBeInTheDocument();
  });

  it('shows write actions for ADMIN and creates an appointment via POST /pastorate/appointments', async () => {
    renderPage('ADMIN');
    await screen.findByTestId('pastoral-create-btn');
    fireEvent.click(screen.getByTestId('pastoral-create-btn'));

    fireEvent.change(await screen.findByTestId('apt-pastor'), { target: { value: 'p2' } });
    fireEvent.change(screen.getByTestId('apt-unit'), { target: { value: 'c2' } });
    fireEvent.click(screen.getByTestId('apt-submit'));
    await waitFor(() => {
      expect(api.post).toHaveBeenCalledWith('/pastorate/appointments', expect.objectContaining({
        pastorId: 'p2', organizationUnitId: 'c2', roleCode: 'PASTEUR',
      }));
    });
  });

  it('approves a pending transfer via POST /pastorate/transfers/{id}/approve', async () => {
    renderPage('PASTOR_PRINCIPAL');
    await screen.findByText('Transferts');
    fireEvent.click(screen.getByText('Transferts'));
    const approveBtn = await screen.findByTestId('transfer-approve-tr1');
    fireEvent.click(approveBtn);
    await waitFor(() => {
      expect(api.post).toHaveBeenCalledWith('/pastorate/transfers/tr1/approve');
    });
  });

  it('warns about the double-mandate rule in the appointment form (impact preview)', async () => {
    renderPage('ADMIN');
    await screen.findByTestId('pastoral-create-btn');
    fireEvent.click(screen.getByTestId('pastoral-create-btn'));
    // p1 a un mandat ACTIF sur c1 → sélection p1 + unité c2 → avertissement
    fireEvent.change(await screen.findByTestId('apt-pastor'), { target: { value: 'p1' } });
    fireEvent.change(screen.getByTestId('apt-unit'), { target: { value: 'c2' } });
    expect(await screen.findByText(/déjà un mandat actif/i)).toBeInTheDocument();
  });
});
