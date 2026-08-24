interface Step { label: string; completed: boolean; }

/**
 * P3 #91 — Indication de progression pour formulaires longs.
 * Affiche les étapes du formulaire avec leur état (complétée / en cours) et
 * une barre de progression globale. Annoncé aux lecteurs d'écran via role/aria.
 */
export default function FormStepper({ steps, className = '' }: { steps: Step[]; className?: string }) {
  const completedCount = steps.filter((s) => s.completed).length;
  const pct = Math.round((completedCount / Math.max(steps.length, 1)) * 100);

  return (
    <div className={`bg-white/5 dark:bg-white/5 rounded-2xl p-4 border border-gray-200 dark:border-white/10 ${className}`} role="group" aria-label="Progression du formulaire">
      <div className="flex items-center justify-between mb-2">
        <p className="text-xs font-medium text-gray-500 dark:text-gray-400">
          Progression : {completedCount}/{steps.length} sections
        </p>
        <span className="text-xs font-bold text-primary-600 dark:text-primary-400" aria-live="polite">{pct} %</span>
      </div>
      <div className="w-full h-1.5 bg-gray-200 dark:bg-black/40 rounded-full overflow-hidden mb-3" role="progressbar" aria-valuenow={pct} aria-valuemin={0} aria-valuemax={100}>
        <div className="h-full bg-gradient-to-r from-primary-500 to-primary-600 rounded-full transition-all duration-300" style={{ width: `${pct}%` }} />
      </div>
      <ol className="flex flex-wrap gap-x-4 gap-y-1">
        {steps.map((s, i) => (
          <li key={s.label} className="flex items-center gap-1.5 text-xs" aria-current={s.completed ? undefined : 'step'}>
            <span className={`w-4 h-4 rounded-full flex items-center justify-center text-[10px] font-bold ${s.completed ? 'bg-green-500 text-white' : 'bg-gray-300 dark:bg-white/10 text-gray-600 dark:text-gray-400'}`} aria-hidden="true">
              {s.completed ? '✓' : i + 1}
            </span>
            <span className={s.completed ? 'text-gray-700 dark:text-gray-300' : 'text-gray-400'}>{s.label}</span>
          </li>
        ))}
      </ol>
    </div>
  );
}
