import { describe, it, expect, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { GroupedTabs, type GroupedTabGroup } from '@/components/ui/GroupedTabs';
import { DURATION, EASING, staggerDelay } from '@/lib/motion';

const GROUPS: GroupedTabGroup[] = [
  {
    id: 'vie',
    label: 'Vie de l’église',
    tabs: [
      { id: 'sermons', label: 'Sermons', count: 12 },
      { id: 'prieres', label: 'Prières', count: 3 },
    ],
  },
  {
    id: 'pilote',
    label: 'Pilotage',
    tabs: [
      { id: 'kpi', label: 'KPI' },
      { id: 'rapports', label: 'Rapports' },
    ],
  },
];

describe('LOT 2 §GR — groupes d’onglets', () => {
  it('affiche les groupes, pas une longue liste d’onglets', () => {
    render(<GroupedTabs groups={GROUPS} value="sermons" onChange={() => {}} />);

    expect(screen.getByRole('button', { name: /Vie de l.église/i })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /Pilotage/i })).toBeInTheDocument();
  });

  it('déplie le groupe contenant l’onglet actif au premier rendu', () => {
    render(<GroupedTabs groups={GROUPS} value="sermons" onChange={() => {}} />);

    const group = screen.getByRole('button', { name: /Vie de l.église/i });
    expect(group).toHaveAttribute('aria-expanded', 'true');
    expect(screen.getByRole('tab', { name: /Sermons/i })).toHaveAttribute('aria-selected', 'true');
  });

  it('au clic sur un groupe, ses sous-onglets s’affichent', async () => {
    const user = userEvent.setup();
    render(<GroupedTabs groups={GROUPS} value="sermons" onChange={() => {}} />);

    const pilote = screen.getByRole('button', { name: /Pilotage/i });
    expect(pilote).toHaveAttribute('aria-expanded', 'false');

    await user.click(pilote);
    expect(pilote).toHaveAttribute('aria-expanded', 'true');
    expect(screen.getByRole('tab', { name: 'KPI' })).toBeInTheDocument();
  });

  it('replie le groupe quand on reclique dessus', async () => {
    const user = userEvent.setup();
    render(<GroupedTabs groups={GROUPS} value="sermons" onChange={() => {}} />);

    const vie = screen.getByRole('button', { name: /Vie de l.église/i });
    await user.click(vie);
    expect(vie).toHaveAttribute('aria-expanded', 'false');
  });

  it('notifie le changement d’onglet', async () => {
    const user = userEvent.setup();
    const onChange = vi.fn();
    render(<GroupedTabs groups={GROUPS} value="sermons" onChange={onChange} />);

    await user.click(screen.getByRole('tab', { name: /Prières/i }));
    expect(onChange).toHaveBeenCalledWith('prieres');
  });

  it('affiche les compteurs', () => {
    render(<GroupedTabs groups={GROUPS} value="sermons" onChange={() => {}} />);
    expect(screen.getByText('12')).toBeInTheDocument();
  });

  it('navigue au clavier entre les onglets d’un groupe', async () => {
    const user = userEvent.setup();
    const onChange = vi.fn();
    render(<GroupedTabs groups={GROUPS} value="sermons" onChange={onChange} />);

    await user.click(screen.getByRole('tab', { name: /Sermons/i }));
    await user.keyboard('{ArrowRight}');
    expect(onChange).toHaveBeenCalledWith('prieres');
  });

  it('un onglet désactivé n’est pas sélectionnable', () => {
    const disabledGroups: GroupedTabGroup[] = [
      { id: 'g', label: 'Groupe', tabs: [{ id: 'off', label: 'Indisponible', disabled: true }] },
    ];
    render(<GroupedTabs groups={disabledGroups} value="off" onChange={() => {}} />);
    expect(screen.getByRole('tab', { name: 'Indisponible' })).toBeDisabled();
  });

  it('suit le groupe de l’onglet actif quand celui-ci change', () => {
    const { rerender } = render(
      <GroupedTabs groups={GROUPS} value="sermons" onChange={() => {}} />,
    );
    rerender(<GroupedTabs groups={GROUPS} value="rapports" onChange={() => {}} />);

    expect(screen.getByRole('button', { name: /Pilotage/i })).toHaveAttribute(
      'aria-expanded',
      'true',
    );
    expect(screen.getByRole('tab', { name: 'Rapports' })).toHaveAttribute('aria-selected', 'true');
  });

it('rend le contenu de l’onglet sélectionné si un rendu est fourni', () => {
    render(
      <GroupedTabs
        groups={GROUPS}
        value="sermons"
        onChange={() => {}}
        renderPanel={(id) => <p>PANNEAU:{id}</p>}
      />,
    );
    // Le texte est réparti sur deux nœuds (« PANEAU: » + l'id), d'où le
    // matcher par fonction plutôt qu'une chaîne exacte.
    expect(
      screen.getByText((_content, element) =>
        element?.tagName === 'P' && element.textContent === 'PANNEAU:sermons',
      ),
    ).toBeInTheDocument();
  });

  it('ne plante pas si aucun onglet ne correspond à la valeur', () => {
    render(<GroupedTabs groups={GROUPS} value="inconnu" onChange={() => {}} />);
    expect(screen.getByRole('tab', { name: /Sermons/i })).toBeInTheDocument();
  });
});

describe('LOT 2 §AN — jetons de mouvement', () => {
  it('borne le décalage d’enchaînement', () => {
    // Sans plafond, une liste de 40 entrées retarderait la dernière de 1,6 s :
    // l'utilisateur perçoit un gel, pas une animation.
    expect(staggerDelay(3)).toBe(120);
    expect(staggerDelay(100)).toBe(320);
    expect(staggerDelay(100, 40, 2)).toBe(80);
  });

  it('expose des durées croissantes', () => {
    expect(DURATION.fast).toBeLessThan(DURATION.base);
    expect(DURATION.base).toBeLessThan(DURATION.slow);
  });

  it('les courbes sont des courbes d’accélération CSS valides', () => {
    for (const easing of Object.values(EASING)) {
      expect(easing).toMatch(/^cubic-bezier\(-?[\d.]+, -?[\d.]+, -?[\d.]+, -?[\d.]+\)$/);
    }
  });
});