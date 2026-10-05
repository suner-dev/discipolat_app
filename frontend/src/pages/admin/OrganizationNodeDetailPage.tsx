import { useState } from 'react';
import { useParams } from 'react-router-dom';
import toast from 'react-hot-toast';
import {
  Network, Users, Church, UserCog, RefreshCw, Loader2, ToggleLeft, ToggleRight,
  ChevronRight, Palette,
} from 'lucide-react';
import { useI18n } from '@/i18n';
import {
  useNodeAggregate, useNodeChildren, useNodeFeatures, useSetNodeFeatures, useNodeTheme,
  usePatchNodeTheme, type ChildWithAggregate,
} from '@/hooks/useOrganizationV3';

/**
 * SPEC_ORGANISATION_MODULABLE_V3 §6.2 — Fiche nœud : Agrégats/progression (E,
 * T-W11) · Modules (D, T-W14) · Thème (D, T-W14).
 *
 * <p>Le drill-down affiche des NOMBRES uniquement (recalcul serveur) : aucun
 * PII nominatif n'est exposé (D7). Les modules sont INDÉPENDANTS par défaut :
 * cocher ici n'impacte pas les enfants (V3-D).
 */

type Tab = 'aggregate' | 'modules' | 'theme';

export default function OrganizationNodeDetailPage() {
  const { nodeId } = useParams<{ nodeId: string }>();
  const { t } = useI18n();
  const [tab, setTab] = useState<Tab>('aggregate');

  const agg = useNodeAggregate(nodeId ?? null);
  const children = useNodeChildren(nodeId ?? null);

  return (
    <div className="page-container">
      <div className="page-header">
        <div className="flex items-center gap-2 mb-1">
          <Network className="w-5 h-5 text-primary-500" />
          <span className="text-sm font-medium text-primary-600 uppercase tracking-wider">
            {t('orgV3.node.kicker')}
          </span>
        </div>
        <h1 className="page-title">{t('orgV3.node.title')}</h1>
        <p className="page-subtitle font-mono text-xs">{nodeId}</p>
      </div>

      <div className="flex gap-2 mb-6">
        {(['aggregate', 'modules', 'theme'] as Tab[]).map((tb) => (
          <button
            key={tb}
            type="button"
            onClick={() => setTab(tb)}
            className={`px-3 py-1.5 rounded-lg text-sm ${
              tab === tb ? 'bg-primary-500 text-white' : 'bg-gray-100 dark:bg-gray-800 text-gray-600'
            }`}
          >
            {t(`orgV3.node.tab.${tb}`)}
          </button>
        ))}
      </div>

      {tab === 'aggregate' && <AggregateTab agg={agg} children={children} />}
      {tab === 'modules' && <ModulesTab nodeId={nodeId ?? null} />}
      {tab === 'theme' && <ThemeTab nodeId={nodeId ?? null} />}
    </div>
  );
}

function AggregateTab({ agg, children }: {
  agg: ReturnType<typeof useNodeAggregate>;
  children: ReturnType<typeof useNodeChildren>;
}) {
  const { t } = useI18n();
  if (agg.isLoading) return <div className="flex justify-center py-16 text-gray-400"><Loader2 className="w-6 h-6 animate-spin" /></div>;
  const a = agg.data;
  return (
    <div className="space-y-6">
      <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
        <Stat icon={<Users className="w-5 h-5" />} label={t('orgV3.agg.members')} value={a?.memberCount ?? 0} />
        <Stat icon={<Church className="w-5 h-5" />} label={t('orgV3.agg.churches')} value={a?.churchCount ?? 0} />
        <Stat icon={<UserCog className="w-5 h-5" />} label={t('orgV3.agg.leaders')} value={a?.leaderCount ?? 0} />
        <Stat icon={<Network className="w-5 h-5" />} label={t('orgV3.agg.sermons')} value={a?.sermonCount ?? 0} />
      </div>

      <div className="glass-card p-4">
        <p className="text-xs uppercase tracking-wide text-gray-400 mb-2">{t('orgV3.agg.progression')}</p>
        <ProgressionSparkline series={a?.progression ?? []} />
      </div>

      <div className="glass-card p-4">
        <p className="text-xs uppercase tracking-wide text-gray-400 mb-3">{t('orgV3.agg.children')}</p>
        {(children.data ?? []).length === 0 && (
          <p className="text-sm text-gray-400">{t('orgV3.agg.noChildren')}</p>
        )}
        <ul className="space-y-1">
          {(children.data ?? []).map((c: ChildWithAggregate) => (
            <li key={c.nodeId} className="flex items-center gap-2 text-sm text-gray-700 dark:text-gray-200">
              <ChevronRight className="w-4 h-4 text-gray-400" />
              <span className="flex-1 truncate">{c.name}</span>
              <span className="text-xs text-gray-400">{t('orgV3.agg.members')}: {c.memberCount ?? 0}</span>
            </li>
          ))}
        </ul>
      </div>
    </div>
  );
}

function Stat({ icon, label, value }: { icon: React.ReactNode; label: string; value: number }) {
  return (
    <div className="glass-card p-4 flex flex-col gap-1">
      <span className="flex items-center gap-2 text-primary-500">{icon}</span>
      <span className="stat-value !text-2xl">{value}</span>
      <span className="text-[11px] uppercase tracking-wide text-gray-400">{label}</span>
    </div>
  );
}

/** Mini-courbe SVG de progression (nombre de fidèles dans le temps). */
function ProgressionSparkline({ series }: { series: Array<{ snapshotAt: string; memberCount: number }> }) {
  if (series.length < 2) return <p className="text-sm text-gray-400">—</p>;
  const vals = series.map((s) => s.memberCount);
  const max = Math.max(...vals, 1);
  const w = 300, h = 60;
  const pts = vals.map((v, i) => `${(i / (vals.length - 1)) * w},${h - (v / max) * h}`).join(' ');
  return (
    <svg viewBox={`0 0 ${w} ${h}`} className="w-full h-16" role="img" aria-label="progression">
      <polyline fill="none" stroke="currentColor" strokeWidth={2} points={pts} className="text-primary-500" />
    </svg>
  );
}

function ModulesTab({ nodeId }: { nodeId: string | null }) {
  const { t } = useI18n();
  const features = useNodeFeatures(nodeId);
  const setModules = useSetNodeFeatures(nodeId);

  const toggle = async (code: string, enabled: boolean, existing: Record<string, unknown>) => {
    // Reconstruit la sélection complète (le PUT remplace la liste).
    const others = (features.data ?? []).filter((f) => f.moduleCode !== code);
    const modules = [
      ...others.map((f) => ({ code: f.moduleCode, enabled: f.enabled, configurationJson: f.configurationJson })),
      { code, enabled, configurationJson: existing },
    ].filter((m) => m.code);
    try {
      await setModules.mutateAsync(modules);
      toast.success(t('orgV3.modules.saved'));
    } catch {
      toast.error(t('orgV3.modules.saveErr'));
    }
  };

  if (features.isLoading) return <div className="flex justify-center py-16 text-gray-400"><Loader2 className="w-6 h-6 animate-spin" /></div>;

  return (
    <div className="glass-card p-4">
      <p className="text-sm text-gray-500 dark:text-gray-400 mb-4">{t('orgV3.modules.hint')}</p>
      <ul className="space-y-2">
        {(features.data ?? []).map((f) => (
          <li key={f.moduleCode} className="flex items-center gap-3 text-sm">
            <button type="button" onClick={() => toggle(f.moduleCode, !f.enabled, f.configurationJson ?? {})}
              className="text-primary-500">
              {f.enabled ? <ToggleRight className="w-6 h-6" /> : <ToggleLeft className="w-6 h-6 text-gray-400" />}
            </button>
            <span className="font-mono">{f.moduleCode}</span>
            <span className={`ml-auto text-xs ${f.enabled ? 'text-green-500' : 'text-gray-400'}`}>
              {f.enabled ? t('orgV3.modules.on') : t('orgV3.modules.off')}
            </span>
          </li>
        ))}
        {(features.data ?? []).length === 0 && (
          <p className="text-sm text-gray-400">{t('orgV3.modules.empty')}</p>
        )}
      </ul>
    </div>
  );
}

function ThemeTab({ nodeId }: { nodeId: string | null }) {
  const { t } = useI18n();
  const theme = useNodeTheme(nodeId);
  const patch = usePatchNodeTheme(nodeId);
  const [primary, setPrimary] = useState('');

  const save = async () => {
    if (!primary.trim()) return;
    try {
      await patch.mutateAsync({ colors: { primary: primary.trim() } });
      toast.success(t('orgV3.theme.saved'));
    } catch {
      toast.error(t('orgV3.theme.saveErr'));
    }
  };

  return (
    <div className="glass-card p-4 space-y-4">
      <div className="flex items-center gap-2 text-primary-500">
        <Palette className="w-5 h-5" />
        <span className="text-sm font-medium">{t('orgV3.theme.title')}</span>
        <span className="ml-auto text-[11px] uppercase text-gray-400">{theme.data?.source ?? '—'}</span>
      </div>
      <p className="text-sm text-gray-500 dark:text-gray-400">{t('orgV3.theme.hint')}</p>
      <input value={primary} onChange={(e) => setPrimary(e.target.value)}
        placeholder="#6366f1" aria-label={t('orgV3.theme.primary')} className="input" />
      <button type="button" onClick={save} disabled={patch.isPending || !primary.trim()}
        className="btn btn-primary inline-flex items-center gap-2">
        {patch.isPending ? <Loader2 className="w-4 h-4 animate-spin" /> : <RefreshCw className="w-4 h-4" />}
        {t('orgV3.theme.apply')}
      </button>
    </div>
  );
}
