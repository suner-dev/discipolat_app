import { useState } from 'react';
import toast from 'react-hot-toast';
import { Loader2, Save } from 'lucide-react';
import { useI18n } from '@/i18n';
import {
  useRoleTitles, useUpsertRoleTitle, useOrgTreeV3,
} from '@/hooks/useOrganizationV3';

/**
 * SPEC_ORGANISATION_MODULABLE_V3 §6.3 / T-W12 — matrice intitulés rôle × nœud (B).
 *
 * <p>Sépare visuellement la CAPACITÉ (permissions, portées par le rôle, non
 * éditables ici) de l'AFFICHAGE (intitulé). Renommer un intitulé ne change
 * jamais une permission (V3-B / §11.4). `nodeId = null` = défaut du tenant.
 */
export default function RoleTitleMatrix({ roleId, roleLabel }: { roleId: string; roleLabel: string }) {
  const { t } = useI18n();
  const titles = useRoleTitles(roleId);
  const tree = useOrgTreeV3();
  const upsert = useUpsertRoleTitle();

  const [draft, setDraft] = useState<Record<string, string>>({});

  const labelFor = (nodeId: string | null) => {
    const key = nodeId ?? '__tenant__';
    if (key in draft) return draft[key];
    const found = (titles.data ?? []).find((x) => !x.resolved && (x.nodeId ?? null) === nodeId);
    return found?.label ?? '';
  };

  const save = async (nodeId: string | null) => {
    const key = nodeId ?? '__tenant__';
    const label = (draft[key] ?? '').trim();
    if (!label) return;
    try {
      await upsert.mutateAsync({ roleId, nodeId, label });
      toast.success(t('orgV3.titles.saved'));
    } catch {
      toast.error(t('orgV3.titles.saveErr'));
    }
  };

  const nodes = (tree.data ?? []).slice(0, 40);

  return (
    <div className="space-y-3">
      <p className="text-xs text-gray-500 dark:text-gray-400">
        {t('orgV3.titles.hint')} — <span className="font-medium">{roleLabel}</span>
      </p>

      {/* Défaut du tenant */}
      <div className="flex items-center gap-2">
        <span className="w-40 text-sm text-gray-600 dark:text-gray-300">{t('orgV3.titles.tenantDefault')}</span>
        <input
          className="input flex-1 text-sm"
          value={labelFor(null)}
          onChange={(e) => setDraft({ ...draft, __tenant__: e.target.value })}
          placeholder={roleLabel}
          aria-label={t('orgV3.titles.tenantDefault')}
        />
        <button type="button" onClick={() => save(null)} disabled={upsert.isPending}
          className="btn btn-secondary btn-sm inline-flex items-center gap-1">
          {upsert.isPending ? <Loader2 className="w-3.5 h-3.5 animate-spin" /> : <Save className="w-3.5 h-3.5" />}
          {t('orgV3.titles.save')}
        </button>
      </div>

      {/* Par nœud */}
      {tree.isLoading ? (
        <div className="flex justify-center py-6 text-gray-400"><Loader2 className="w-5 h-5 animate-spin" /></div>
      ) : (
        <ul className="space-y-2 max-h-72 overflow-y-auto">
          {nodes.map((n) => (
            <li key={n.id} className="flex items-center gap-2">
              <span className="w-40 text-sm text-gray-600 dark:text-gray-300 truncate" title={n.levelName ?? n.name}>
                {n.levelName ? `${n.name} · ${n.levelName}` : n.name}
              </span>
              <input
                className="input flex-1 text-sm"
                value={labelFor(n.id)}
                onChange={(e) => setDraft({ ...draft, [n.id]: e.target.value })}
                placeholder={labelFor(null) || roleLabel}
                aria-label={`${roleLabel} @ ${n.name}`}
              />
              <button type="button" onClick={() => save(n.id)} disabled={upsert.isPending}
                className="p-1.5 text-gray-400 hover:text-primary-500" aria-label={t('orgV3.titles.save')}>
                <Save className="w-4 h-4" />
              </button>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}
