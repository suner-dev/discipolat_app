import { describe, it, expect } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import DataTable from '@/components/shared/DataTable';

/* ============================================================================
 * DataTable — tri mémoïsé, animation bornée et pagination client (opt-in).
 * ========================================================================== */

interface Row { id: string; nom: string; age: number }

const ROWS: Row[] = Array.from({ length: 25 }, (_, i) => ({
  id: `r${i}`,
  nom: `Disciple ${String(25 - i).padStart(2, '0')}`,
  age: 18 + (i % 40),
}));

const columns = [
  { header: 'Nom', accessor: 'nom' as const },
  { header: 'Âge', accessor: 'age' as const },
];

describe('DataTable — pagination client (opt-in pageSize)', () => {
  it('affiche toutes les lignes par défaut (comportement historique)', () => {
    render(<DataTable data={ROWS} columns={columns} />);
    expect(screen.getAllByRole('row')).toHaveLength(ROWS.length + 1); // + header
  });

  it('borne le DOM au pageSize et affiche la navigation', () => {
    render(<DataTable data={ROWS} columns={columns} pageSize={10} />);
    // 1 header + 10 lignes
    expect(screen.getAllByRole('row')).toHaveLength(11);
    expect(screen.getByText('1–10 sur 25')).toBeInTheDocument();
    expect(screen.getByText('1 / 3')).toBeInTheDocument();
  });

  it('navigue entre les pages et met à jour le compteur', () => {
    render(<DataTable data={ROWS} columns={columns} pageSize={10} />);
    fireEvent.click(screen.getByLabelText('Page suivante'));
    expect(screen.getByText('11–20 sur 25')).toBeInTheDocument();
    expect(screen.getByText('2 / 3')).toBeInTheDocument();
    fireEvent.click(screen.getByLabelText('Page précédente'));
    expect(screen.getByText('1–10 sur 25')).toBeInTheDocument();
  });

  it('désactive le bouton précédent sur la première page', () => {
    render(<DataTable data={ROWS} columns={columns} pageSize={10} />);
    expect(screen.getByLabelText('Page précédente')).toBeDisabled();
  });

  it('revient à la première page quand les données changent (pas de page obsolète)', () => {
    const { rerender } = render(<DataTable data={ROWS} columns={columns} pageSize={10} />);
    fireEvent.click(screen.getByLabelText('Page suivante'));
    expect(screen.getByText('2 / 3')).toBeInTheDocument();
    // Les données rétrécissent (filtre, recherche...) : la pagination disparaît
    // (5 ≤ pageSize) et aucune page obsolète ne subsiste.
    rerender(<DataTable data={ROWS.slice(0, 5)} columns={columns} pageSize={10} />);
    expect(screen.queryByText('2 / 3')).not.toBeInTheDocument();
    expect(screen.queryByText('1–10 sur 25')).not.toBeInTheDocument();
  });

  it('revient à la première page quand le tri change', () => {
    render(<DataTable data={ROWS} columns={columns} pageSize={10} sortable />);
    fireEvent.click(screen.getByLabelText('Page suivante'));
    expect(screen.getByText('2 / 3')).toBeInTheDocument();
    fireEvent.click(screen.getByText('Nom'));
    expect(screen.getByText('1 / 3')).toBeInTheDocument();
  });
});

describe('DataTable — tri', () => {
  it('trie par colonne au clic (asc puis desc)', () => {
    render(<DataTable data={ROWS} columns={columns} sortable />);
    const th = screen.getByText('Nom');
    fireEvent.click(th);
    const rows = screen.getAllByRole('row');
    expect(rows[1].textContent).toContain('Disciple 01'); // asc
    fireEvent.click(th);
    const rows2 = screen.getAllByRole('row');
    expect(rows2[1].textContent).toContain('Disciple 25'); // desc
  });
});
