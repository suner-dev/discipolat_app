import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import api from '@/lib/api';
import { useTenant } from '@/contexts/TenantContext';
import OrganizationBrowserPage from '@/pages/OrganizationBrowserPage';

// -- Mocks -------------------------------------------------------------------
vi.mock('@/contexts/TenantContext', async () => {
  const actual = await vi.importActual('@/contexts/TenantContext');
  return { ...actual, useTenant: vi.fn() };
});

vi.mock('@/lib/api', () => ({
  default: { get: vi.fn(), post: vi.fn() },
  getErrorMessage: vi.fn().mockReturnValue('Erreur'),
}));

// -- Fixures (forme réelle de /org/tree, /org/stats, /org/nodes/{id}) --------
const ROOT = {
  id: 'r1', tenantId: 't1', parentId: null, type: 'ROOT_CHURCH', name: 'Église Centrale',
  code: 'ROOT', status: 'ACTIVE', path: 'ROOT', level: 0, color: '#6366f1', createdAt: '2026-01-01T00:00:00Z',
};
const DEPT_TECH = {
  id: 'd1', tenantId: 't1', parentId: 'r1', type: 'DEPARTMENT', name: 'Louange',
  code: 'DEP-LOU', status: 'ACTIVE', path: 'ROOT.DEP-LOU', level: 1, color: '#f59e0b', createdAt: '2026-01-01T00:00:00Z',
};
const GROUP = {
  id: 'g1', tenantId: 't1', parentId: 'd1', type: 'GROUP', name: 'Chant',
  code: 'GRP-CHT', status: 'ACTIVE', path: 'ROOT.DEP-LOU.GRP-CHT', level: 2, createdAt: '2026-01-01T00:00:00Z',
};
interface FixtureNode {
  id: string; tenantId: string; parentId: string | null; type: string; name: string;
  code: string; status: string; path: string; level: number; color?: string; createdAt: string;
}
const TREE: { root: FixtureNode; allNodes: FixtureNode[]; childrenByParent: Record<string, FixtureNode[]> } = {
  root: ROOT,
  allNodes: [ROOT, DEPT_TECH, GROUP],
  childrenByParent: { r1: [DEPT_TECH], d1: [GROUP] },
};

function mockPermissions(perms: string[]) {
  (useTenant as ReturnType<typeof vi.fn>).mockReturnValue({
    hasPermission: (p: string) => perms.includes(p.toUpperCase()),
  });
}

function stubApi(overrides: { tree?: unknown } = {}) {
  (api.get as ReturnType<typeof vi.fn>).mockImplementation((url: string) => {
    if (url === '/org/tree') return Promise.resolve({ data: overrides.tree ?? TREE });
    if (url === '/org/stats') return Promise.resolve({ data: { ROOT_CHURCH: 1, DEPARTMENT: 1, GROUP: 1 } });
    if (url.startsWith('/org/nodes/')) {
      const id = url.split('/').pop() as string;
      const node = TREE.allNodes.find((n) => n.id === id) ?? ROOT;
      return Promise.resolve({
        data: {
          node,
          children: TREE.childrenByParent[id] ?? [],
          descendantCount: TREE.allNodes.filter((n) => n.path.startsWith(`${node.path}.`)).length,
          responsible: null,
        },
      });
    }
    return Promise.resolve({ data: null });
  });
  (api.post as ReturnType<typeof vi.fn>).mockResolvedValue({ data: {} });
}

function renderPage() {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return render(
    <QueryClientProvider client={queryClient}>
      <OrganizationBrowserPage />
    </QueryClientProvider>,
  );
}

beforeEach(() => {
  vi.clearAllMocks();
  mockPermissions(['ORG_NODE_MOVE', 'CHURCH_CREATE']);
  stubApi();
});

describe('OrganizationBrowserPage (G5.2)', () => {
  it('fetches the real org tree and renders nodes by name', async () => {
    renderPage();
    await waitFor(() => {
      expect(api.get).toHaveBeenCalledWith('/org/tree');
      expect(screen.getByText('Église Centrale')).toBeInTheDocument();
    });
    expect(screen.getByText('Louange')).toBeInTheDocument();
    expect(screen.getByText('Chant')).toBeInTheDocument();
  });

  it('enables drag on nodes when the user holds ORG_NODE_MOVE', async () => {
    renderPage();
    const row = await screen.findByText('Louange');
    const draggable = row.closest('[draggable]');
    expect(draggable).not.toBeNull();
    expect(draggable?.getAttribute('draggable')).toBe('true');
  });

  it('renders read-only (no draggable rows) without ORG_NODE_MOVE', async () => {
    mockPermissions([]);
    renderPage();
    const row = await screen.findByText('Louange');
    expect(row.closest('[draggable="true"]')).toBeNull();
    expect(screen.getByText('Lecture seule')).toBeInTheDocument();
  });

  it('loads node details from /org/nodes/{id} on click', async () => {
    renderPage();
    const row = await screen.findByText('Louange');
    fireEvent.click(row);
    await waitFor(() => expect(api.get).toHaveBeenCalledWith('/org/nodes/d1'));
    expect(await screen.findByText('Aucun')).toBeInTheDocument(); // responsable absent → état vide réel
  });

  it('moves a node via POST /org/nodes/{id}/move on drop', async () => {
    renderPage();
    const dragged = await screen.findByText('Chant');
    const target = screen.getByText('Église Centrale');
    const dataTransfer = { setData: vi.fn(), getData: vi.fn(), effectAllowed: '', dropEffect: '' };
    fireEvent.dragStart(dragged.closest('[draggable="true"]')!, { dataTransfer });
    fireEvent.dragOver(target.closest('[draggable="true"]')!, { dataTransfer });
    fireEvent.drop(target.closest('[draggable="true"]')!, { dataTransfer });
    await waitFor(() => {
      expect(api.post).toHaveBeenCalledWith('/org/nodes/g1/move', { parentId: 'r1' });
    });
  });

  it('refuses to drop a node into its own descendant (cycle guard, no API call)', async () => {
    renderPage();
    const dragged = await screen.findByText('Louange');
    const descendant = screen.getByText('Chant');
    const dataTransfer = { setData: vi.fn(), getData: vi.fn(), effectAllowed: '', dropEffect: '' };
    fireEvent.dragStart(dragged.closest('[draggable="true"]')!, { dataTransfer });
    fireEvent.drop(descendant.closest('[draggable="true"]')!, { dataTransfer });
    await new Promise((r) => setTimeout(r, 50));
    expect(api.post).not.toHaveBeenCalled();
  });

  it('offers the root-church creation form only with CHURCH_CREATE when tree is empty', async () => {
    stubApi({ tree: { root: null, allNodes: [], childrenByParent: {} } });
    renderPage();
    const input = await screen.findByLabelText("Nom de l'église");
    fireEvent.change(input, { target: { value: 'Nouvelle Église' } });
    fireEvent.click(screen.getByText("Créer l'église racine"));
    await waitFor(() => {
      expect(api.post).toHaveBeenCalledWith('/org/root-church', { name: 'Nouvelle Église' });
    });
  });
});
