import { useState } from 'react';
import toast from 'react-hot-toast';
import { Layers, Plus, Trash2, Loader2, ArrowUp, ArrowDown } from 'lucide-react';
import { useI18n } from '@/i18n';
import {
  useOrgLevels, useCreateOrgLevel, useUpdateOrgLevel, useDeleteOrgLevel,
  type OrgLevel,
} from '@/hooks/useOrganizationV3';

/**
 * SPEC_ORGANISATION_MODULABLE_V3 §6.2 / T-W10 — Éditeur de niveaux (A).
 *
 * Chaque dénomination définit SA hiérarchie (Région → Zone → Campus…) :
 * nom, ordre, type sémantique. Les libellés viennent du backend (dynamiques),
 * on ne les fige jamais ici. La création exige admin/owner ; la suppression
 * est refusée (409) si des nœuds utilisent encore le niveau.
 */

const SEMANTIC_TYPES = [
  'ROOT_CHURCH', 'CAMPUS', 'SUB_CHURCH', 'ASSEMBLY',
  'REGION', 'DISTRICT', 'DEPARTMENT', 'GROUP', 'CUSTOM',
];

export default function OrganizationLevelsPage() {
  const { t } = useI18n();
  const levelsQuery = useOrgLevels();
  const createLevel = useCreateOrgLevel();
  const updateLevel = useUpdateOrgLevel();
  const deleteLevel = useDeleteOrgLevel();

  const [form, setForm] = useState({ name: '', pluralName: '', semanticType: 'CUSTOM', color: '#6366f1' });

  const levels = [...(levelsQuery.data ?? [])].sort((a, b) => a.depthOrder - b.depthOrder);

  const submit = async () => {
    if (form.name.trim().length < 2) return;
    const depthOrder = (levels.at(-1)?.depthOrder ?? 0) + 1;
    try {
      await createLevel.mutateAsync({
        name: form.name.trim(),
        pluralName: form.pluralName.trim() || undefined,
        semanticType: form.semanticType,
        depthOrder,
        color: form.color,
      });
      setForm({ name: '', pluralName: '', semanticType: 'CUSTOM', color: '#6366f1' });
      toast.success(t('orgV3.levels.created'));
    } catch {
      toast.error(t('orgV3.levels.createErr'));
    }
  };

  const remove = async (lvl: OrgLevel) => {
    if (!confirm(t('orgV3.levels.deleteConfirm', { name: lvl.name }))) return;
    try {
      await deleteLevel.mutateAsync(lvl.id);
      toast.success(t('orgV3.levels.deleted'));
    } catch {
      toast.error(t('orgV3.levels.deleteErr'));
    }
  };

  const rename = async (lvl: OrgLevel, name: string) => {
    if (name.trim() === lvl.name || name.trim().length < 2) return;
    try {
      await updateLevel.mutateAsync({ id: lvl.id, patch: { name: name.trim() } });
    } catch {
      toast.error(t('orgV3.levels.renameErr'));
    }
  };

  return (
    <div className="page-container">
      <div className="page-header">
        <div className="flex items-center gap-2 mb-1">
          <Layers className="w-5 h-5 text-primary-500" />
          <span className="text-sm font-medium text-primary-600 dark:text-primary-400 uppercase tracking-wider">
            {t('orgV3.levels.kicker')}
          </span>
        </div>
        <h1 className="page-title">{t('orgV3.levels.title')}</h1>
        <p className="page-subtitle">{t('orgV3.levels.subtitle')}</p>
      </div>

      <div className="glass-card p-4 mb-6 animate-slide-up">
        <div className="grid grid-cols-1 md:grid-cols-4 gap-3">
          <input
            value={form.name}
            onChange={(e) => setForm({ ...form, name: e.target.value })}
            placeholder={t('orgV3.levels.namePh')}
            aria-label={t('orgV3.levels.namePh')}
            className="input"
          />
          <input
            value={form.pluralName}
            onChange={(e) => setForm({ ...form, pluralName: e.target.value })}
            placeholder={t('orgV3.levels.pluralPh')}
            aria-label={t('orgV3.levels.pluralPh')}
            className="input"
          />
          <select
            value={form.semanticType}
            onChange={(e) => setForm({ ...form, semanticType: e.target.value })}
            aria-label={t('orgV3.levels.semantic')}
            className="input"
          >
            {SEMANTIC_TYPES.map((st) => (
              <option key={st} value={st}>{t(`orgV3.semantic.${st}`)}</option>
            ))}
          </select>
          <button
            type="button"
            onClick={submit}
            disabled={createLevel.isPending || form.name.trim().length < 2}
            className="btn btn-primary inline-flex items-center justify-center gap-2"
          >
            {createLevel.isPending ? <Loader2 className="w-4 h-4 animate-spin" /> : <Plus className="w-4 h-4" />}
            {t('orgV3.levels.add')}
          </button>
        </div>
      </div>

      {levelsQuery.isLoading && (
        <div className="flex justify-center py-16 text-gray-400"><Loader2 className="w-6 h-6 animate-spin" /></div>
      )}

      <div className="glass-card p-2 animate-slide-up">
        {levels.length === 0 && !levelsQuery.isLoading && (
          <p className="text-sm text-gray-400 text-center py-10">{t('orgV3.levels.empty')}</p>
        )}
        <ul className="divide-y divide-gray-100 dark:divide-gray-800">
          {levels.map((lvl, i) => (
            <li key={lvl.id} className="flex items-center gap-3 px-3 py-2.5">
              <span className="w-6 text-center text-xs font-mono text-gray-400">{i + 1}</span>
              <span className="w-2 h-6 rounded-full" style={{ backgroundColor: lvl.color ?? '#6366f1' }} />
              <input
                defaultValue={lvl.name}
                onBlur={(e) => rename(lvl, e.target.value)}
                aria-label={lvl.name}
                className="flex-1 bg-transparent border-0 focus:ring-1 focus:ring-primary-500 rounded px-2 py-1 text-sm text-gray-800 dark:text-gray-100"
              />
              <span className="text-[10px] uppercase tracking-wide text-gray-400 shrink-0">
                {t(`orgV3.semantic.${lvl.semanticType}`)}
              </span>
              <span className="inline-flex items-center gap-0.5">
                <button
                  type="button"
                  aria-label={t('orgV3.levels.moveUp')}
                  disabled={i === 0}
                  onClick={() => updateLevel.mutate({ id: lvl.id, patch: { depthOrder: levels[i - 1].depthOrder } })}
                  className="p-1 text-gray-400 hover:text-gray-700 disabled:opacity-30"
                >
                  <ArrowUp className="w-4 h-4" />
                </button>
                <button
                  type="button"
                  aria-label={t('orgV3.levels.moveDown')}
                  disabled={i === levels.length - 1}
                  onClick={() => updateLevel.mutate({ id: lvl.id, patch: { depthOrder: levels[i + 1].depthOrder } })}
                  className="p-1 text-gray-400 hover:text-gray-700 disabled:opacity-30"
                >
                  <ArrowDown className="w-4 h-4" />
                </button>
                <button
                  type="button"
                  aria-label={t('orgV3.levels.delete')}
                  onClick={() => remove(lvl)}
                  className="p-1 text-red-400 hover:text-red-600"
                >
                  <Trash2 className="w-4 h-4" />
                </button>
              </span>
            </li>
          ))}
        </ul>
      </div>
    </div>
  );
}
