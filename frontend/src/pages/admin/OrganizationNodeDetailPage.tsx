import { useState } from 'react';
import { useParams, Link } from 'react-router-dom';
import toast from 'react-hot-toast';
import {
  Network, Users, Church, UserCog, RefreshCw, Loader2, ToggleLeft, ToggleRight,
  ChevronRight, Palette, IdCard, ShieldCheck, Ticket,
} from 'lucide-react';
import { useI18n } from '@/i18n';
import {
  useNodeAggregate, useNodeChildren, useNodeFeatures, useSetNodeFeatures, useNodeTheme,
  usePatchNodeTheme, useNodeTeam, useOrgTreeV3, type ChildWithAggregate,
} from '@/hooks/useOrganizationV3';
import ProgressionSparkline from '@/components/organization/ProgressionSparkline';

/**
 * SPEC_ORGANISATION_MODULABLE_V3 §6.2 — Fiche nœud.
 * Onglets : Identité · Responsable & équipe (assignments, T-W13) · Modules (D,
 * T-W14) · Thème (D, T-W14) · Agrégats/progression (E, T-W11) · Codes de
 * rejointure (lien, V2).
 *
 * <p>Le drill-down affiche des NOMBRES uniquement (recalcul serveur) : aucun
 * PII nominatif de membre n'est exposé (D7). L'équipe d'un nœud montre les
 * porteurs de rôle déclarés sur CE nœud (admin tenant uniquement). Les modules
 * sont INDÉPENDANTS par défaut : cocher ici n'impacte pas les enfants (V3-D).
 */

type Tab = 'identity' | 'team' | 'modules' | 'theme' | 'aggregate' | 'codes';

const TABS: Tab[] = ['identity', 'team', 'modules', 'theme', 'aggregate', 'codes'];

export default function OrganizationNodeDetailPage() {
  const { nodeId } = useParams<{ nodeId: string }>();
  const { t } = useI18n();
  const [tab, setTab] = useState<Tab>('identity');

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

      <div className="flex flex-wrap gap-2 mb-6">
        {TABS.map((tb) => (
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

      {tab === 'identity' && <IdentityTab nodeId={nodeId ?? null} />}
      {tab === 'team' && <TeamTab nodeId={nodeId ?? null} />}
      {tab === 'modules' && <ModulesTab nodeId={nodeId ?? null} />}
      {tab === 'theme' && <ThemeTab nodeId={nodeId ?? null} />}
      {tab === 'aggregate' && <AggregateTab nodeId={nodeId ?? null} />}
      {tab === 'codes' && <CodesTab nodeId={nodeId ?? null} />}
    </div>
  );
}

function IdentityTab({ nodeId }: { nodeId: string | null }) {
  const { t } = useI18n();
  const tree = useOrgTreeV3();
  const node = (tree.data ?? []).find((n) => n.id === nodeId);
  if (tree.isLoading) return <Loader />;
  if (!node) return <p className="text-sm text-gray-400">{t('orgV3.node.notFound')}</p>;
  const rows: Array<[string, string]> = [
    [t('orgV3.identity.name'), node.name],
    [t('orgV3.identity.level'), node.levelName ?? '—'],
    [t('orgV3.identity.type'), node.type],
    [t('orgV3.identity.responsible'), node.responsibleName ?? '—'],
  ];
  return (
    <div className="glass-card p-4 space-y-2">
      <div className="flex items-center gap-2 text-primary-500 mb-1">
        <IdCard className="w-5 h-5" />
        <span className="text-sm font-medium">{t('orgV3.identity.title')}</span>
      </div>
      <dl className="grid grid-cols-1 md:grid-cols-2 gap-x-6 gap-y-2">
        {rows.map(([k, v]) => (
          <div key={k} className="flex items-baseline gap-2">
            <dt className="text-xs uppercase tracking-wide text-gray-400 w-40 shrink-0">{k}</dt>
            <dd className="text-sm text-gray-800 dark:text-gray-100">{v}</dd>
          </div>
        ))}
      </dl>
    </div>
  );
}

function TeamTab({ nodeId }: { nodeId: string | null }) {
  const { t } = useI18n();
  const team = useNodeTeam(nodeId);
  if (team.isLoading) return <Loader />;
  const members = team.data ?? [];
  return (
    <div className="glass-card p-4 space-y-2">
      <div className="flex items-center gap-2 text-primary-500 mb-1">
        <ShieldCheck className="w-5 h-5" />
        <span className="text-sm font-medium">{t('orgV3.team.title')}</span>
      </div>
      <p className="text-xs text-gray-400">{t('orgV3.team.hint')}</p>
      {members.length === 0 ? (
        <p className="text-sm text-gray-400">{t('orgV3.team.empty')}</p>
      ) : (
        <ul className="space-y-1">
          {members.map((m) => (
            <li key={m.assignmentId} className="flex items-center gap-3 text-sm py-1">
              <span className="flex-1 truncate text-gray-800 dark:text-gray-100">{m.memberName ?? m.userId}</span>
              <span className="text-xs text-primary-600">{m.roleLabel ?? '—'}</span>
              <span className={`text-[11px] ${m.status === 'ACTIVE' ? 'text-green-500' : 'text-gray-400'}`}>
                {t(`orgV3.team.status.${m.status}`)}
              </span>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}

function AggregateTab({ nodeId }: { nodeId: string | null }) {
  const { t } = useI18n();
  const agg = useNodeAggregate(nodeId);
  const children = useNodeChildren(nodeId);
  if (agg.isLoading) return <Loader />;
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
        <ProgressionSparkline series={a?.progression ?? []} label={t('orgV3.agg.progression')} />
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
              <Link to={`/tenant/organization/nodes/${c.nodeId}`} className="flex-1 truncate hover:text-primary-600">
                {c.name}
              </Link>
              <span className="text-xs text-gray-400">{t('orgV3.agg.members')}: {c.memberCount ?? 0}</span>
            </li>
          ))}
        </ul>
      </div>
    </div>
  );
}

function CodesTab({ nodeId }: { nodeId: string | null }) {
  const { t } = useI18n();
  if (!nodeId) return null;
  return (
    <div className="glass-card p-4 space-y-2">
      <div className="flex items-center gap-2 text-primary-500 mb-1">
        <Ticket className="w-5 h-5" />
        <span className="text-sm font-medium">{t('orgV3.codes.title')}</span>
      </div>
      <p className="text-sm text-gray-500 dark:text-gray-400">{t('orgV3.codes.hint')}</p>
      <Link to="/tenant/join-management" className="btn btn-secondary btn-sm inline-flex items-center gap-1">
        {t('orgV3.codes.open')}
      </Link>
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

function Loader() {
  return <div className="flex justify-center py-16 text-gray-400"><Loader2 className="w-6 h-6 animate-spin" /></div>;
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

  if (features.isLoading) return <Loader />;

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
