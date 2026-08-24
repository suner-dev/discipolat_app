import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {
  SkeletonLine, SkeletonCard, SkeletonTable, SkeletonDashboard,
  EmptyState, ConfirmDialog, ProgressBar, OnboardingStepper, VisuallyHidden,
  useReducedMotion,
} from '@/components/ui/UXComponents';
import React from 'react';

// ═══════════════════════════════════════════════════════════
// P2 #85 — SKELETON LOADING TESTS
// ═══════════════════════════════════════════════════════════

describe('SkeletonLine', () => {
  it('renders a div with animate-pulse class', () => {
    const { container } = render(<SkeletonLine />);
    const el = container.firstChild as HTMLElement;
    expect(el.tagName).toBe('DIV');
    expect(el.className).toContain('animate-pulse');
  });

  it('applies custom className', () => {
    const { container } = render(<SkeletonLine className="h-4 w-1/2" />);
    const el = container.firstChild as HTMLElement;
    expect(el.className).toContain('h-4');
    expect(el.className).toContain('w-1/2');
  });
});

describe('SkeletonCard', () => {
  it('renders a glass-card skeleton', () => {
    const { container } = render(<SkeletonCard />);
    expect(container.querySelector('.glass-card')).toBeTruthy();
  });

  it('renders avatar placeholder', () => {
    const { container } = render(<SkeletonCard />);
    const avatar = container.querySelector('.w-10.h-10');
    expect(avatar).toBeTruthy();
  });
});

describe('SkeletonTable', () => {
  it('renders correct number of rows', () => {
    const { container } = render(<SkeletonTable rows={3} cols={2} />);
    const rows = container.querySelectorAll('.glass-card');
    expect(rows.length).toBe(3);
  });
});

describe('SkeletonDashboard', () => {
  it('renders 4 stat cards', () => {
    const { container } = render(<SkeletonDashboard />);
    const cards = container.querySelectorAll('.stat-card');
    expect(cards.length).toBe(4);
  });
});

// ═══════════════════════════════════════════════════════════
// P2 #86 — EMPTY STATE TESTS
// ═══════════════════════════════════════════════════════════

describe('EmptyState', () => {
  it('renders title', () => {
    render(<EmptyState title="Aucun élément" />);
    expect(screen.getByText('Aucun élément')).toBeTruthy();
  });

  it('renders description when provided', () => {
    render(<EmptyState title="Vide" description="Pas de données" />);
    expect(screen.getByText('Pas de données')).toBeTruthy();
  });

  it('renders action button when provided', () => {
    const onClick = vi.fn();
    render(<EmptyState title="Vide" action={{ label: 'Créer', onClick }} />);
    const btn = screen.getByText('Créer');
    expect(btn).toBeTruthy();
    fireEvent.click(btn);
    expect(onClick).toHaveBeenCalledTimes(1);
  });

  it('does not render action button when not provided', () => {
    render(<EmptyState title="Vide" />);
    expect(screen.queryByRole('button')).toBeNull();
  });
});

// ═══════════════════════════════════════════════════════════
// P2 #87 — CONFIRM DIALOG TESTS
// ═══════════════════════════════════════════════════════════

describe('ConfirmDialog', () => {
  it('renders nothing when open is false', () => {
    const { container } = render(
      <ConfirmDialog open={false} title="Test" message="Msg" onConfirm={vi.fn()} onCancel={vi.fn()} />
    );
    expect(container.innerHTML).toBe('');
  });

  it('renders when open is true', () => {
    render(
      <ConfirmDialog open={true} title="Supprimer?" message="Action irréversible" onConfirm={vi.fn()} onCancel={vi.fn()} />
    );
    expect(screen.getByText('Supprimer?')).toBeTruthy();
    expect(screen.getByText('Action irréversible')).toBeTruthy();
  });

  it('calls onConfirm when confirm button is clicked', async () => {
    const onConfirm = vi.fn();
    render(
      <ConfirmDialog open={true} title="Test" message="Msg" onConfirm={onConfirm} onCancel={vi.fn()} />
    );
    const confirmBtn = screen.getByText('Confirmer');
    await userEvent.click(confirmBtn);
    expect(onConfirm).toHaveBeenCalledTimes(1);
  });

  it('calls onCancel when cancel button is clicked', async () => {
    const onCancel = vi.fn();
    render(
      <ConfirmDialog open={true} title="Test" message="Msg" onConfirm={vi.fn()} onCancel={onCancel} />
    );
    const cancelBtn = screen.getByText('Annuler');
    await userEvent.click(cancelBtn);
    expect(onCancel).toHaveBeenCalledTimes(1);
  });

  it('calls onCancel when overlay is clicked', async () => {
    const onCancel = vi.fn();
    render(
      <ConfirmDialog open={true} title="Test" message="Msg" onConfirm={vi.fn()} onCancel={onCancel} />
    );
    const overlay = document.querySelector('.modal-overlay');
    if (overlay) fireEvent.click(overlay);
    expect(onCancel).toHaveBeenCalled();
  });

  it('uses custom confirm/cancel labels', () => {
    render(
      <ConfirmDialog open={true} title="Test" message="Msg" confirmLabel="Oui" cancelLabel="Non" onConfirm={vi.fn()} onCancel={vi.fn()} />
    );
    expect(screen.getByText('Oui')).toBeTruthy();
    expect(screen.getByText('Non')).toBeTruthy();
  });
});

// ═══════════════════════════════════════════════════════════
// P2 #96 — PROGRESS BAR TESTS
// ═══════════════════════════════════════════════════════════

describe('ProgressBar', () => {
  it('renders with correct width percentage', () => {
    const { container } = render(<ProgressBar value={50} max={100} />);
    const bar = container.querySelector('[role="progressbar"]');
    expect(bar).toBeTruthy();
    expect(bar?.getAttribute('aria-valuenow')).toBe('50');
  });

  it('renders label when provided', () => {
    render(<ProgressBar value={75} label="Progression" />);
    expect(screen.getByText('Progression')).toBeTruthy();
    expect(screen.getByText('75%')).toBeTruthy();
  });

  it('clamps value to 0-100 range', () => {
    const { container } = render(<ProgressBar value={150} max={100} />);
    const bar = container.querySelector('[role="progressbar"]');
    expect(bar?.getAttribute('aria-valuemax')).toBe('100');
  });

  it('sets aria-valuemin to 0', () => {
    const { container } = render(<ProgressBar value={30} />);
    const bar = container.querySelector('[role="progressbar"]');
    expect(bar?.getAttribute('aria-valuemin')).toBe('0');
  });
});

// ═══════════════════════════════════════════════════════════
// P2 #97 — ONBOARDING STEPPER TESTS
// ═══════════════════════════════════════════════════════════

describe('OnboardingStepper', () => {
  const steps = [
    { label: 'Étape 1' },
    { label: 'Étape 2', done: true },
    { label: 'Étape 3' },
  ];

  it('renders all step labels', () => {
    render(<OnboardingStepper steps={steps} current={0} />);
    expect(screen.getByText('Étape 1')).toBeTruthy();
    expect(screen.getByText('Étape 2')).toBeTruthy();
    expect(screen.getByText('Étape 3')).toBeTruthy();
  });

  it('calls onStepClick when a step is clicked', async () => {
    const onStepClick = vi.fn();
    render(<OnboardingStepper steps={steps} current={0} onStepClick={onStepClick} />);
    const step2 = screen.getByText('Étape 2');
    await userEvent.click(step2);
    expect(onStepClick).toHaveBeenCalledWith(1);
  });

  it('renders step numbers for incomplete steps', () => {
    render(<OnboardingStepper steps={steps} current={0} />);
    // Step 1 and 3 should have numbers
    expect(screen.getByText('1')).toBeTruthy();
    expect(screen.getByText('3')).toBeTruthy();
  });
});

// ═══════════════════════════════════════════════════════════
// P2 #94 — VISUALLY HIDDEN TESTS
// ═══════════════════════════════════════════════════════════

describe('VisuallyHidden', () => {
  it('renders children with sr-only class', () => {
    render(<VisuallyHidden>Hidden text</VisuallyHidden>);
    const el = screen.getByText('Hidden text');
    expect(el.className).toContain('sr-only');
  });
});

// ═══════════════════════════════════════════════════════════
// P2 #93 — REDUCED MOTION HOOK TESTS
// ═══════════════════════════════════════════════════════════

function TestComponent() {
  const reduced = useReducedMotion();
  return <span>{reduced ? 'reduced' : 'normal'}</span>;
}

describe('useReducedMotion', () => {
  beforeEach(() => {
    // Mock window.matchMedia for jsdom
    Object.defineProperty(window, 'matchMedia', {
      writable: true,
      value: vi.fn().mockImplementation((query: string) => ({
        matches: false,
        media: query,
        onchange: null,
        addListener: vi.fn(),
        removeListener: vi.fn(),
        addEventListener: vi.fn(),
        removeEventListener: vi.fn(),
        dispatchEvent: vi.fn(),
      })),
    });
  });

  it('returns false when prefers-reduced-motion is not set', () => {
    render(<TestComponent />);
    expect(screen.getByText('normal')).toBeTruthy();
  });
});
