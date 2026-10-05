import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';

/**
 * SPEC_ORGANISATION_MODULABLE_V3 §6.2 / T-W13 / T-Q2 — Assignations membre (C).
 *
 * `<MemberRolesPage>` materialise « nommer X ancien et lui affecter 3 campus ».
 * Ces tests verrouillent trois contrats :
 *
 * 1. **Portée découplée** — une assignation porte un rôle-capacité sur un nœud
 *    (ou sur tout le tenant quand `nodeId` est vide) ; l'appartenance n'intervient pas.
 * 2. **Retirer ≠ purger** — on ne fait qu'appeler la mutation `end` (transition
 *    ENDED côté serveur) ; la page ne supprime rien localement.
 * 3. **Seules les lignes ACTIVE sont listées** — une assignation `ENDED` reste en
 *    base mais sort de l'écran (le composant filtre sur `status === 'ACTIVE'`).
 */

const assignMutate = vi.fn().mockResolvedValue({});
const endMutate = vi.fn().mockResolvedValue({});

const ASSIGNMENTS = [
  { id: 'a-1', userId: 'u-1', roleId: 'r-ancien', status: 'ACTIVE', nodeId: 'c1' },
  { id: 'a-2', userId: 'u-1', roleId: 'r-pasteur', status: 'ACTIVE', nodeId: null },
  { id: 'a-3', userId: 'u-1', roleId: 'r-ancien', status: 'ENDED', nodeId: 'c2' },
];

vi.mock('@/hooks/useOrganizationV3', () => ({
  useMemberAssignments: () => ({ data: ASSIGNMENTS, isLoading: false }),
  useAssignMemberRole: () => ({ mutateAsync: assignMutate, isPending: false }),
  useEndMemberAssignment: () => ({ mutateAsync: endMutate, isPending: false }),
  useOrgTreeV3: () => ({
    data: [{ id: 'c1', parentId: null, name: 'Campus Bukavu', type: 'CAMPUS', levelName: 'Campus' }],
    isLoading: false,
  }),
}));

const ROLES = [
  { id: 'r-ancien', label: 'Ancien' },
  { id: 'r-pasteur', label: 'Pasteur' },
];

vi.mock('@/lib/api', () => ({
  default: { get: vi.fn() },
  getErrorMessage: () => 'err',
}));

import MemberRolesPage from '@/pages/admin/MemberRolesPage';
import api from '@/lib/api';

const mockedApiGet = vi.mocked(api.get);

function renderPage() {
  const qc = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return render(
    <QueryClientProvider client={qc}>
      <MemoryRouter initialEntries={['/admin/members/u-1/roles']}>
        <Routes>
          <Route path="/admin/members/:userId/roles" element={<MemberRolesPage />} />
        </Routes>
      </MemoryRouter>
    </QueryClientProvider>,
  );
}

beforeEach(() => {
  vi.clearAllMocks();
  // `clearAllMocks` efface les implémentations (pas seulement l'historique) :
  // sans ce ré-armement, `api.get` renverrait `undefined` et le sélecteur de
  // rôles resterait vide. Le ré-armer ici le rend explicite et robuste.
  mockedApiGet.mockResolvedValue({ data: ROLES });
  assignMutate.mockResolvedValue({});
  endMutate.mockResolvedValue({});
});

describe('MemberRolesPage — assignations membre par nœud (C / T-W13 / T-Q2)', () => {
  it('affiche le membre ciblé et les rôles résolus par leur libellé', async () => {
    renderPage();
    expect(screen.getByText('u-1')).toBeInTheDocument();
    // Libellé de rôle résolu via /tenant/roles (jamais l'id brut si connu).
    // « Ancien » existe aussi dans le <select> : on cible le <span> de la liste.
    expect(await screen.findByText('Ancien', { selector: 'span' })).toBeInTheDocument();
    expect(screen.getByText('Pasteur', { selector: 'span' })).toBeInTheDocument();
  });

  it('n\'affiche QUE les assignations ACTIVE (les ENDED sortent de l\'écran)', () => {
    renderPage();
    // a-3 est ENDED : son id ne doit apparaître nulle part. On vérifie le
    // nombre de boutons « mettre fin » = 2 (a-1, a-2), pas 3.
    expect(screen.getAllByRole('button', { name: /mettre fin/i })).toHaveLength(2);
  });

  it('montre le nœud quand il y en a un, sinon « tout le tenant »', async () => {
    renderPage();
    // a-1 est sur c1 → nom du nœud résolu via l'arbre V3. « Campus Bukavu »
    // existe aussi dans le <select> (« Campus Bukavu · Campus ») : le span de
    // la liste est le seul texte EXACT.
    expect(await screen.findByText('Campus Bukavu', { selector: 'span' })).toBeInTheDocument();
    // a-2 est sans nœud → portée tenant.
    expect(screen.getByText('Tout le tenant', { selector: 'span' })).toBeInTheDocument();
  });

  it('affecte un rôle sur un nœud (nodeId transmission)', async () => {
    renderPage();
    const roleSelect = screen.getByLabelText('Rôle');
    const nodeSelect = screen.getByLabelText('Nœud');
    // Les options arrivent via useQuery (api mocké) : changer la valeur AVANT
    // leur rendu fait retomber jsdom sur "" et le bouton reste désactivé.
    await screen.findByRole('option', { name: 'Ancien' });
    await screen.findByRole('option', { name: 'Campus Bukavu · Campus' });
    fireEvent.change(roleSelect, { target: { value: 'r-ancien' } });
    fireEvent.change(nodeSelect, { target: { value: 'c1' } });
    fireEvent.click(screen.getByRole('button', { name: /affecter/i }));
    await waitFor(() =>
      expect(assignMutate).toHaveBeenCalledWith({ roleId: 'r-ancien', nodeId: 'c1' }),
    );
  });

  it('affecte sur tout le tenant quand aucun nœud n\'est choisi (null, pas "")', async () => {
    renderPage();
    await screen.findByRole('option', { name: 'Pasteur' });
    fireEvent.change(screen.getByLabelText('Rôle'), { target: { value: 'r-pasteur' } });
    fireEvent.click(screen.getByRole('button', { name: /affecter/i }));
    await waitFor(() =>
      // Le contrat du backend distingue null (tenant) de "" : on envoie null.
      expect(assignMutate).toHaveBeenCalledWith({ roleId: 'r-pasteur', nodeId: null }),
    );
  });

  it('retirer une assignation appelle end (transition ENDED, pas de purge)', async () => {
    renderPage();
    const endButtons = await screen.findAllByRole('button', { name: /mettre fin/i });
    fireEvent.click(endButtons[0]);
    await waitFor(() => expect(endMutate).toHaveBeenCalledWith('a-1'));
  });

  it('le bouton « affecter » est désactivé sans rôle choisi', () => {
    renderPage();
    expect(screen.getByRole('button', { name: /affecter/i })).toBeDisabled();
  });
});