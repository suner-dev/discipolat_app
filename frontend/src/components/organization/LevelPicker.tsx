import { Loader2, Check } from 'lucide-react';
import { useI18n } from '@/i18n';
import { useOrgLevels } from '@/hooks/useOrganizationV3';

/**
 * SPEC_ORGANISATION_MODULABLE_V3 §6.3 / T-W10 — `<LevelPicker>`.
 * Sélection d'un niveau custom de la dénomination (A). `null` = repli sur le
 * type sémantique de l'enum (rétro-compat : le backend accepte `levelId` nul,
 * §3.2.2). Les libellés viennent de l'API, jamais figés ici.
 */
export default function LevelPicker({
  value,
  onChange,
  allowAuto = true,
}: {
  value: string | null;
  onChange: (levelId: string | null) => void;
  allowAuto?: boolean;
}) {
  const { t } = useI18n();
  const levels = useOrgLevels();
  const ordered = [...(levels.data ?? [])].sort((a, b) => a.depthOrder - b.depthOrder);

  if (levels.isLoading) {
    return <div className="flex justify-center py-6 text-gray-400"><Loader2 className="w-5 h-5 animate-spin" /></div>;
  }

  return (
    <div className="space-y-2">
      {allowAuto && (
        <button
          type="button"
          onClick={() => onChange(null)}
          className={`w-full text-left rounded-lg px-3 py-2 text-sm ${
            !value ? 'ring-1 ring-primary-500 bg-primary-500/10' : 'bg-gray-50 dark:bg-gray-800/50'
          }`}
        >
          {t('orgV3.level.auto')}{' '}
          <span className="text-gray-400">— {t('orgV3.level.autoHint')}</span>
        </button>
      )}
      {ordered.map((lv) => (
        <button
          key={lv.id}
          type="button"
          onClick={() => onChange(lv.id)}
          className={`w-full text-left rounded-lg px-3 py-2 text-sm flex items-center gap-2 ${
            value === lv.id ? 'ring-1 ring-primary-500 bg-primary-500/10' : 'bg-gray-50 dark:bg-gray-800/50'
          }`}
        >
          <span className="flex-1">{lv.name}</span>
          <span className="text-[10px] uppercase text-gray-400">{lv.semanticType}</span>
          {value === lv.id && <Check className="w-4 h-4 text-primary-500" />}
        </button>
      ))}
    </div>
  );
}
