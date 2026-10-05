import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import OrgTreeNav from '@/components/organization/OrgTreeNav';
import type { TreeNode } from '@/hooks/useOrganizationV3';

/**
 * SPEC_ORGANISATION_MODULABLE_V3 §9.9 — T-Q2.
 *
 * `<OrgTreeNav>` (drill-down E, §6.3) doit remplacer l'affichage brut du
 * `type` par le **nom du niveau** (`levelName`) et rendre les **compteurs
 * inline** (fidèles/églises/leaders) — sans PII nominatif (D7).
 */
const NODES: TreeNode[] = [
  { id: 'r1', parentId: null, name: 'Afrique centrale', type: 'REGION', levelName: 'Région', memberCount: 120, churchCount: 7, leaderCount: 15, responsibleName: 'Pasteur Nzolo' },
  { id: 'z1', parentId: 'r1', name: 'Zone Nord', type: 'ZONE', levelName: 'Zone', memberCount: 40, churchCount: 2, leaderCount: 5 },
  { id: 'c1', parentId: 'z1', name: 'Campus Bukavu', type: 'CAMPUS', levelName: 'Campus', memberCount: 25, churchCount: 1, leaderCount: 3 },
];

describe('OrgTreeNav — drill-down par niveau custom (T-W11 / T-Q2)', () => {
  it("affiche le levelName (et non le type brut) pour chaque nœud", () => {
    render(<OrgTreeNav nodes={NODES} />);
    // Le nom du niveau s'affiche en badge ; le `type` (REGION/ZONE) ne doit JAMAIS
    // apparaître tel quel dans l'arbre agrégé.
    expect(screen.getAllByText('Région').length).toBeGreaterThan(0);
    expect(screen.getByText('Zone')).toBeInTheDocument();
    expect(screen.getByText('Campus')).toBeInTheDocument();
    expect(screen.queryByText('REGION')).not.toBeInTheDocument();
    expect(screen.queryByText('CAMPUS')).not.toBeInTheDocument();
  });

  it('rend les compteurs inline (fidèles/églises/leaders) du nœud', () => {
    render(<OrgTreeNav nodes={NODES} />);
    // Les totaux du sous-arbre (E) : le root affiche ses compteurs agrégés.
    expect(screen.getByText('120')).toBeInTheDocument();
    expect(screen.getByText('7')).toBeInTheDocument();
    expect(screen.getByText('15')).toBeInTheDocument();
  });

  it('dessine l’arbre (enfants rattachés au parent) et le responsable', () => {
    render(<OrgTreeNav nodes={NODES} />);
    // Enfant visible via la récursion (rootId=null → r1, puis z1, puis c1).
    expect(screen.getByText('Zone Nord')).toBeInTheDocument();
    expect(screen.getByText('Campus Bukavu')).toBeInTheDocument();
    // Responsable déclaré (nom, pas de liste nominative de membres → D7 ok).
    expect(screen.getByText('Pasteur Nzolo')).toBeInTheDocument();
  });

  it('renvoie l’objet cliqué à onOpen', () => {
    const onOpen = vi.fn();
    render(<OrgTreeNav nodes={NODES} onOpen={onOpen} />);
    fireEvent.click(screen.getByText('Zone Nord'));
    expect(onOpen).toHaveBeenCalledWith(expect.objectContaining({ id: 'z1', levelName: 'Zone' }));
  });

  it("affiche l'état vide (clé i18n) quand aucun nœud", () => {
    render(<OrgTreeNav nodes={[]} />);
    // orgV3.tree.empty (FR de repli) — aucun node, aucun compteur.
    expect(screen.getByText('Aucun nœud')).toBeInTheDocument();
  });
});
