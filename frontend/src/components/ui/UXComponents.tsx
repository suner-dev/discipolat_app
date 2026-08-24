import { useState, useEffect, ReactNode } from 'react';
import { AlertTriangle, CheckCircle2, Info, X, RefreshCw, Loader2 } from 'lucide-react';

// ═══════════════════════════════════════════════════════════
// P2 #85 — SKELETON LOADING
// ═══════════════════════════════════════════════════════════

export function SkeletonLine({ className = '' }: { className?: string }) {
  return <div className={`animate-pulse bg-gray-200 dark:bg-gray-700 rounded ${className}`} />;
}

export function SkeletonCard() {
  return (
    <div className="glass-card p-4 space-y-3">
      <div className="flex items-center gap-3">
        <div className="w-10 h-10 rounded-xl animate-pulse bg-gray-200 dark:bg-gray-700" />
        <div className="flex-1 space-y-2">
          <SkeletonLine className="h-4 w-2/3" />
          <SkeletonLine className="h-3 w-1/3" />
        </div>
      </div>
      <SkeletonLine className="h-3 w-full" />
      <SkeletonLine className="h-3 w-4/5" />
    </div>
  );
}

export function SkeletonTable({ rows = 5, cols = 4 }: { rows?: number; cols?: number }) {
  return (
    <div className="space-y-3">
      {Array.from({ length: rows }).map((_, i) => (
        <div key={i} className="glass-card p-4 flex gap-4">
          {Array.from({ length: cols }).map((_, j) => (
            <SkeletonLine key={j} className={`h-4 ${j === 0 ? 'w-1/4' : j === cols - 1 ? 'w-1/6' : 'flex-1'}`} />
          ))}
        </div>
      ))}
    </div>
  );
}

export function SkeletonDashboard() {
  return (
    <div className="space-y-6">
      <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
        {Array.from({ length: 4 }).map((_, i) => (
          <div key={i} className="stat-card">
            <SkeletonLine className="h-3 w-1/2 mb-2" />
            <SkeletonLine className="h-7 w-1/3" />
          </div>
        ))}
      </div>
      <SkeletonCard />
      <SkeletonTable rows={3} cols={3} />
    </div>
  );
}

// ═══════════════════════════════════════════════════════════
// P2 #86 — EMPTY STATES
// ═══════════════════════════════════════════════════════════

interface EmptyStateProps {
  icon?: ReactNode;
  title: string;
  description?: string;
  action?: { label: string; onClick: () => void };
}

export function EmptyState({ icon, title, description, action }: EmptyStateProps) {
  return (
    <div className="glass-card p-12 text-center animate-fade-in">
      <div className="w-16 h-16 rounded-2xl bg-gray-100 dark:bg-gray-800 flex items-center justify-center mx-auto mb-4">
        {icon || <Info className="w-8 h-8 text-gray-300 dark:text-gray-600" />}
      </div>
      <h3 className="text-base font-bold text-gray-900 dark:text-gray-100 mb-1">{title}</h3>
      {description && <p className="text-sm text-gray-500 dark:text-gray-400 max-w-sm mx-auto">{description}</p>}
      {action && (
        <button onClick={action.onClick} className="btn-primary btn-sm mt-4">
          {action.label}
        </button>
      )}
    </div>
  );
}

// ═══════════════════════════════════════════════════════════
// P2 #87 — CONFIRMATION DIALOG
// ═══════════════════════════════════════════════════════════

interface ConfirmDialogProps {
  open: boolean;
  title: string;
  message: string;
  confirmLabel?: string;
  cancelLabel?: string;
  variant?: 'danger' | 'warning' | 'info';
  onConfirm: () => void;
  onCancel: () => void;
}

export function ConfirmDialog({
  open, title, message, confirmLabel = 'Confirmer', cancelLabel = 'Annuler',
  variant = 'danger', onConfirm, onCancel,
}: ConfirmDialogProps) {
  if (!open) return null;

  const colors = {
    danger: 'bg-red-500 hover:bg-red-600',
    warning: 'bg-amber-500 hover:bg-amber-600',
    info: 'bg-blue-500 hover:bg-blue-600',
  };

  return (
    <div className="modal-overlay" onClick={onCancel}>
      <div className="modal-content max-w-sm" onClick={(e) => e.stopPropagation()}>
        <div className="p-6 text-center">
          <div className={`w-12 h-12 rounded-2xl mx-auto mb-4 flex items-center justify-center ${
            variant === 'danger' ? 'bg-red-100 dark:bg-red-900/30' :
            variant === 'warning' ? 'bg-amber-100 dark:bg-amber-900/30' :
            'bg-blue-100 dark:bg-blue-900/30'
          }`}>
            {variant === 'danger' ? <AlertTriangle className="w-6 h-6 text-red-500" /> :
             variant === 'warning' ? <AlertTriangle className="w-6 h-6 text-amber-500" /> :
             <Info className="w-6 h-6 text-blue-500" />}
          </div>
          <h3 className="text-base font-bold text-gray-900 dark:text-gray-100 mb-2">{title}</h3>
          <p className="text-sm text-gray-500 dark:text-gray-400">{message}</p>
        </div>
        <div className="modal-footer">
          <button onClick={onCancel} className="btn-ghost btn-sm">{cancelLabel}</button>
          <button onClick={onConfirm} className={`btn-sm text-white ${colors[variant]}`}>{confirmLabel}</button>
        </div>
      </div>
    </div>
  );
}

// ═══════════════════════════════════════════════════════════
// P2 #88 — TOAST NOTIFICATION SYSTEM (wrapper)
// ═══════════════════════════════════════════════════════════

export { default as toast } from 'react-hot-toast';

export function ToastContainer() {
  // Handled by react-hot-toast's Toaster
  return null;
}

// ═══════════════════════════════════════════════════════════
// P2 #93 — REDUCED MOTION SUPPORT
// ═══════════════════════════════════════════════════════════

export function useReducedMotion() {
  const [prefersReduced, setPrefersReduced] = useState(false);

  useEffect(() => {
    const mq = window.matchMedia('(prefers-reduced-motion: reduce)');
    setPrefersReduced(mq.matches);
    const handler = (e: MediaQueryListEvent) => setPrefersReduced(e.matches);
    mq.addEventListener('change', handler);
    return () => mq.removeEventListener('change', handler);
  }, []);

  return prefersReduced;
}

// ═══════════════════════════════════════════════════════════
// P2 #94 — SCREEN READER SUPPORT
// ═══════════════════════════════════════════════════════════

export function VisuallyHidden({ children }: { children: ReactNode }) {
  return (
    <span className="sr-only" style={{ position: 'absolute', width: 1, height: 1, padding: 0, margin: -1, overflow: 'hidden', clip: 'rect(0,0,0,0)', border: 0 }}>
      {children}
    </span>
  );
}

// ═══════════════════════════════════════════════════════════
// P2 #96 — PROGRESS INDICATOR
// ═══════════════════════════════════════════════════════════

export function ProgressBar({ value, max = 100, label, color = 'emerald' }: {
  value: number; max?: number; label?: string; color?: string;
}) {
  const pct = Math.min(100, Math.max(0, (value / max) * 100));
  return (
    <div className="w-full">
      {label && <div className="flex justify-between text-xs text-gray-500 dark:text-gray-400 mb-1">
        <span>{label}</span>
        <span>{Math.round(pct)}%</span>
      </div>}
      <div className="w-full h-2 rounded-full bg-gray-200 dark:bg-gray-700 overflow-hidden">
        <div
          className={`h-full rounded-full bg-${color}-500 transition-all duration-500`}
          style={{ width: `${pct}%` }}
          role="progressbar"
          aria-valuenow={value}
          aria-valuemin={0}
          aria-valuemax={max}
        />
      </div>
    </div>
  );
}

// ═══════════════════════════════════════════════════════════
// P2 #97 — ONBOARDING TOOLTIP / STEPPER
// ═══════════════════════════════════════════════════════════

export function OnboardingStepper({ steps, current, onStepClick }: {
  steps: { label: string; done?: boolean }[];
  current: number;
  onStepClick?: (i: number) => void;
}) {
  return (
    <div className="flex items-center gap-1 overflow-x-auto py-2">
      {steps.map((step, i) => (
        <div key={i} className="flex items-center">
          <button
            onClick={() => onStepClick?.(i)}
            className={`flex items-center gap-1.5 px-3 py-1.5 rounded-full text-xs font-medium transition-all ${
              i === current
                ? 'bg-primary-500 text-white shadow-sm'
                : step.done
                ? 'bg-green-100 dark:bg-green-900/30 text-green-700 dark:text-green-400'
                : 'bg-gray-100 dark:bg-gray-800 text-gray-500 dark:text-gray-400'
            }`}
          >
            {step.done ? (
              <CheckCircle2 className="w-3 h-3" />
            ) : (
              <span className="w-4 h-4 rounded-full bg-current/10 flex items-center justify-center text-[10px] font-bold">
                {i + 1}
              </span>
            )}
            <span className="hidden sm:inline">{step.label}</span>
          </button>
          {i < steps.length - 1 && <div className="w-4 h-px bg-gray-300 dark:bg-gray-600 mx-1" />}
        </div>
      ))}
    </div>
  );
}
