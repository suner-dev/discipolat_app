import { useI18n } from '@/i18n';

/**
 * SPEC_ORGANISATION_MODULABLE_V3 §6.3 / T-W14 — `<ModuleMultiSelect>`.
 * Sélection de modules par nœud, avec presets. Les modules sont INDÉPENDANTS
 * par défaut (V3-D) : rien de coché = nœud vierge, on n'hérite jamais
 * silencieusement. Les libellés restent techniques (code) pour éviter toute
 * clé i18n manquante (garde-fou : jamais de label figé en dur pour un module).
 */

// Catalogue de modules — miroir de la liste terrain mobile.
export const KNOWN_MODULES = [
  'people', 'events', 'notifications', 'dashboard', 'org', 'families', 'groups',
  'discipleship', 'academy', 'finance', 'media', 'pastoral', 'prayer', 'sermons',
  'assets', 'workflow', 'reports', 'analytics', 'messaging', 'documents',
  'calendar', 'forms', 'ai', 'chat', 'payments',
];

// Presets de confort (jamais imposés — V3-D) : cocher applique le preset.
export const MODULE_PRESETS: Record<string, string[]> = {
  starter: ['people', 'events', 'sermons', 'prayer', 'dashboard'],
  full: [...KNOWN_MODULES],
};

export default function ModuleMultiSelect({
  value,
  onChange,
  options = KNOWN_MODULES,
  showPresets = true,
}: {
  value: string[];
  onChange: (next: string[]) => void;
  options?: string[];
  showPresets?: boolean;
}) {
  const { t } = useI18n();
  const selected = new Set(value);

  const toggle = (code: string) => {
    const next = new Set(selected);
    if (next.has(code)) next.delete(code);
    else next.add(code);
    onChange([...next]);
  };

  return (
    <div className="space-y-2">
      {showPresets && (
        <div className="flex flex-wrap gap-1.5">
          {Object.entries(MODULE_PRESETS).map(([name, codes]) => (
            <button
              key={name}
              type="button"
              onClick={() => onChange(codes.filter((c) => options.includes(c)))}
              className="text-[11px] px-2 py-0.5 rounded-full border border-gray-200 dark:border-gray-700 text-gray-500 hover:border-primary-400"
            >
              {t(`orgV3.modules.preset.${name}`)}
            </button>
          ))}
        </div>
      )}
      <div className="flex flex-wrap gap-1.5">
        {options.map((code) => {
          const on = selected.has(code);
          return (
            <button
              key={code}
              type="button"
              onClick={() => toggle(code)}
              aria-pressed={on}
              className={`px-2 py-1 rounded-full text-xs font-mono ${
                on ? 'bg-primary-500 text-white' : 'bg-gray-100 dark:bg-gray-800 text-gray-500'
              }`}
            >
              {code}
            </button>
          );
        })}
      </div>
    </div>
  );
}
