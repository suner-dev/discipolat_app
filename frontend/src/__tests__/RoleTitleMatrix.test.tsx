import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';

/**
 * SPEC_ORGANISATION_MODULABLE_V3 §9.9 — T-Q2 (matrice intitulés, §6.3 / T-W12).
 *
 * Vérifie que `<RoleTitleMatrix>` sépare bien l'AFFICHAGE (intitulé éditable
 * par nœud) de la CAPACITÉ : le composant n'expose QUE des libellés ; renommer
 * passe par `useUpsertRoleTitle` (PUT §5.2) et ne touche aucune permission.
 */
const mutateAsync = vi.fn().mockResolvedValue({});

vi.mock('@/hooks/useOrganizationV3', () => ({
  useRoleTitles: () => ({
    data: [
      { nodeId: null, label: 'Ancien', resolved: false },
      { nodeId: 'c1', label: 'Berger', resolved: false },
      // ligne « résolue » appendue par l'API → jamais éditable ici.
      { nodeId: null, label: 'Ancien (effectif)', resolved: true, effectiveLabel: 'Ancien' },
    ],
    isLoading: false,
  }),
  useOrgTreeV3: () => ({
    data: [
      { id: 'c1', parentId: 'r1', name: 'Campus Center', type: 'CAMPUS', levelName: 'Église locale' },
    ],
    isLoading: false,
  }),
  useUpsertRoleTitle: () => ({ mutateAsync, isPending: false }),
}));

import RoleTitleMatrix from '@/components/organization/RoleTitleMatrix';

beforeEach(() => {
  vi.clearAllMocks();
  mutateAsync.mockResolvedValue({});
});

describe('RoleTitleMatrix — intitulés par scope (B / T-Q2)', () => {
  it("rend le défaut tenant et une ligne par nœud (levelName affichée)", () => {
    render(<RoleTitleMatrix roleId="r-ancien" roleLabel="Ancien" />);
    // Le hint rappelle explicitement que ça ne change pas les permissions.
    expect(screen.getByText(/ne change jamais les permissions/i)).toBeInTheDocument();
    // Ligne par nœud : nom + levelName.
    expect(screen.getByText('Campus Center · Église locale')).toBeInTheDocument();
  });

  it('pré-remplit les inputs depuis les intitulés non résolus (défaut vs nœud)', () => {
    render(<RoleTitleMatrix roleId="r-ancien" roleLabel="Ancien" />);
    const tenantInput = screen.getByLabelText('Défaut (tenant)') as HTMLInputElement;
    expect(tenantInput.value).toBe('Ancien');
    const nodeInput = screen.getByLabelText('Ancien @ Campus Center') as HTMLInputElement;
    expect(nodeInput.value).toBe('Berger');
  });

  it('enregistrer un intitulé appelle upsert (PUT) sans toucher une permission', async () => {
    render(<RoleTitleMatrix roleId="r-ancien" roleLabel="Ancien" />);
    const nodeInput = screen.getByLabelText('Ancien @ Campus Center');
    fireEvent.change(nodeInput, { target: { value: 'Pasteur principal' } });
    // Bouton de sauvegarde de la ligne nœud (aria-label = t('orgV3.titles.save')).
    const saveBtns = screen.getAllByRole('button', { name: /enregistrer/i });
    fireEvent.click(saveBtns[saveBtns.length - 1]);
    await waitFor(() =>
      expect(mutateAsync).toHaveBeenCalledWith({
        roleId: 'r-ancien',
        nodeId: 'c1',
        label: 'Pasteur principal',
      }),
    );
  });
});
