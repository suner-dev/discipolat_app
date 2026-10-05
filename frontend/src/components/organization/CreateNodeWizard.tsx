import { useState } from 'react';
import toast from 'react-hot-toast';
import { Loader2, X, ChevronLeft, ChevronRight, Plus, Check } from 'lucide-react';
import api from '@/lib/api';
import { useI18n } from '@/i18n';
import {
  useOrgLevels, useOrgTreeV3, useCreateOrgNodeV3,
  type TreeNode,
} from '@/hooks/useOrganizationV3';

/**
 * SPEC_ORGANISATION_MODULABLE_V3 §6.3 — Assistant de création d'un nœud
 * (T-W14) : identité → niveau (A) → modules (D) → thème (optionnel).
 *
 * <p>Le backend accepte `levelId` + `moduleCodes` à la création (T-B10/T-B13) ;
 * le thème s'applique dans un second temps via PATCH (le nœud doit exister).
 * Les modules sont INDÉPENDANTS par défaut : ne rien cocher = nœud vierge,
 * on n'hérite jamais automatiquement (V3-D).
 */

// Catalogue de modules proposé à la création — miroir de la liste terrain
// mobile ; les libellés restent techniques (code) pour éviter toute clé i18n
// manquante (garde-fou : jamais de label figé en dur pour un module d'admin).
const KNOWN_MODULES = [
  'people', 'events', 'notifications', 'dashboard', 'org', 'families', 'groups',
  'discipleship', 'academy', 'finance', 'media', 'pastoral', 'prayer', 'sermons',
  'assets', 'workflow', 'reports', 'analytics', 'messaging', 'documents',
  'calendar', 'forms', 'ai', 'chat', 'payments',
];

const NODE_TYPES = ['REGION', 'DISTRICT', 'ROOT_CHURCH', 'SUB_CHURCH', 'CAMPUS', 'DEPARTMENT', 'GROUP'];

type Step = 0 | 1 | 2;

export default function CreateNodeWizard({
  open,
  onClose,
  presetParentId,
}: {
  open: boolean;
  onClose: () => void;
  presetParentId?: string | null;
}) {
  const { t } = useI18n();
  const levels = useOrgLevels();
  const tree = useOrgTreeV3();
  const createNode = useCreateOrgNodeV3();

  const [step, setStep] = useState<Step>(0);
  const [name, setName] = useState('');
  const [type, setType] = useState<string>('CAMPUS');
  const [parentId, setParentId] = useState<string | null>(presetParentId ?? null);
  const [levelId, setLevelId] = useState<string | null>(null);
  const [modules, setModules] = useState<Set<string>>(new Set());
  const [primaryColor, setPrimaryColor] = useState('');

  const nodes: TreeNode[] = tree.data ?? [];

  const canNext = step === 0 ? name.trim().length >= 2 : true;

  const reset = () => {
    setStep(0);
    setName('');
    setType('CAMPUS');
    setParentId(presetParentId ?? null);
    setLevelId(null);
    setModules(new Set());
    setPrimaryColor('');
  };

  const submit = async () => {
    try {
      const created = await createNode.mutateAsync({
        name: name.trim(),
        type,
        parentId,
        levelId,
        moduleCodes: [...modules],
      });
      // Thème optionnel — appliqué après création (le nœud doit exister).
      if (primaryColor.trim() && created?.id) {
        try {
          await api.patch(`/tenant/organization/nodes/${created.id}/theme`, {
            colors: { primary: primaryColor.trim() },
          });
        } catch {
          toast.error(t('orgV3.wizard.themeErr'));
        }
      }
      toast.success(t('orgV3.wizard.created'));
      reset();
      onClose();
    } catch {
      toast.error(t('orgV3.wizard.createErr'));
    }
  };

  if (!open) return null;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40 p-4" onClick={onClose}>
      <div
        className="glass-card w-full max-w-lg p-5 space-y-4 max-h-[85vh] overflow-y-auto"
        onClick={(e) => e.stopPropagation()}
        role="dialog"
        aria-modal="true"
        aria-label={t('orgV3.wizard.title')}
      >
        <div className="flex items-center gap-2">
          <h3 className="text-base font-semibold flex-1">{t('orgV3.wizard.title')}</h3>
          <button type="button" onClick={onClose} aria-label={t('orgV3.wizard.close')}
            className="text-gray-400 hover:text-gray-600"><X className="w-5 h-5" /></button>
        </div>

        {/* Fil d'étapes */}
        <div className="flex items-center gap-1 text-xs">
          {([0, 1, 2] as Step[]).map((s) => (
            <span key={s}
              className={`px-2 py-1 rounded-full ${s === step ? 'bg-primary-500 text-white' : 'bg-gray-100 dark:bg-gray-800 text-gray-500'}`}>
              {t(`orgV3.wizard.step.${s}`)}
            </span>
          ))}
        </div>

        {step === 0 && (
          <div className="space-y-3">
            <label className="block text-sm">
              <span className="text-gray-500">{t('orgV3.wizard.name')}</span>
              <input autoFocus value={name} onChange={(e) => setName(e.target.value)}
                className="input mt-1" placeholder={t('orgV3.wizard.namePh')} />
            </label>
            <label className="block text-sm">
              <span className="text-gray-500">{t('orgV3.wizard.type')}</span>
              <select value={type} onChange={(e) => setType(e.target.value)} className="input mt-1">
                {NODE_TYPES.map((tp) => <option key={tp} value={tp}>{tp}</option>)}
              </select>
            </label>
            <label className="block text-sm">
              <span className="text-gray-500">{t('orgV3.wizard.parent')}</span>
              <select value={parentId ?? ''} onChange={(e) => setParentId(e.target.value || null)} className="input mt-1">
                <option value="">{t('orgV3.wizard.parentNone')}</option>
                {nodes.map((n) => <option key={n.id} value={n.id}>{n.name}</option>)}
              </select>
            </label>
          </div>
        )}

        {step === 1 && (
          <div className="space-y-2">
            <p className="text-sm text-gray-500">{t('orgV3.wizard.levelHint')}</p>
            <button type="button" onClick={() => setLevelId(null)}
              className={`w-full text-left rounded-lg px-3 py-2 text-sm ${!levelId ? 'ring-1 ring-primary-500 bg-primary-500/10' : 'bg-gray-50 dark:bg-gray-800/50'}`}>
              {t('orgV3.wizard.levelAuto')} <span className="text-gray-400">— {t('orgV3.wizard.levelAutoHint')}</span>
            </button>
            {(levels.data ?? []).map((lv) => (
              <button key={lv.id} type="button" onClick={() => setLevelId(lv.id)}
                className={`w-full text-left rounded-lg px-3 py-2 text-sm flex items-center gap-2 ${levelId === lv.id ? 'ring-1 ring-primary-500 bg-primary-500/10' : 'bg-gray-50 dark:bg-gray-800/50'}`}>
                <span className="flex-1">{lv.name}</span>
                <span className="text-[10px] uppercase text-gray-400">{lv.semanticType}</span>
                {levelId === lv.id && <Check className="w-4 h-4 text-primary-500" />}
              </button>
            ))}
          </div>
        )}

        {step === 2 && (
          <div className="space-y-3">
            <div>
              <p className="text-sm text-gray-500 mb-2">{t('orgV3.wizard.modulesHint')}</p>
              <div className="flex flex-wrap gap-1.5">
                {KNOWN_MODULES.map((code) => {
                  const on = modules.has(code);
                  return (
                    <button key={code} type="button"
                      onClick={() => setModules((prev) => {
                        const next = new Set(prev);
                        if (next.has(code)) next.delete(code); else next.add(code);
                        return next;
                      })}
                      className={`px-2 py-1 rounded-full text-xs font-mono ${on ? 'bg-primary-500 text-white' : 'bg-gray-100 dark:bg-gray-800 text-gray-500'}`}>
                      {code}
                    </button>
                  );
                })}
              </div>
            </div>
            <label className="block text-sm">
              <span className="text-gray-500">{t('orgV3.wizard.theme')}</span>
              <input value={primaryColor} onChange={(e) => setPrimaryColor(e.target.value)}
                className="input mt-1" placeholder="#6366f1" />
            </label>
          </div>
        )}

        <div className="flex items-center gap-2 pt-2">
          {step > 0 && (
            <button type="button" onClick={() => setStep((s) => (s - 1) as Step)}
              className="btn btn-secondary btn-sm inline-flex items-center gap-1">
              <ChevronLeft className="w-4 h-4" />{t('orgV3.wizard.back')}
            </button>
          )}
          <div className="flex-1" />
          {step < 2 ? (
            <button type="button" disabled={!canNext} onClick={() => setStep((s) => (s + 1) as Step)}
              className="btn btn-primary btn-sm inline-flex items-center gap-1">
              {t('orgV3.wizard.next')}<ChevronRight className="w-4 h-4" />
            </button>
          ) : (
            <button type="button" disabled={createNode.isPending} onClick={submit}
              className="btn btn-primary btn-sm inline-flex items-center gap-1">
              {createNode.isPending ? <Loader2 className="w-4 h-4 animate-spin" /> : <Plus className="w-4 h-4" />}
              {t('orgV3.wizard.submit')}
            </button>
          )}
        </div>
      </div>
    </div>
  );
}
