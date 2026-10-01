import { useEffect, useMemo, useState } from 'react';
import { useParams, Link } from 'react-router-dom';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import toast from 'react-hot-toast';
import {
  Boxes, Calendar, CheckCircle2, ChevronRight, CircleSlash, FileText,
  FolderOpen, GraduationCap, LayoutDashboard, ListTodo, Loader2, PiggyBank,
  Save, Settings, ShieldCheck, Users, Wallet, Workflow, Lock,
} from 'lucide-react';
import api from '@/lib/api';
import { useI18n } from '@/i18n';

/**
 * G5.3 — Department/Family OS (niveau 2 : expérience GÉNÉRÉE).
 *
 * L'écran /spaces/:id construit son menu, ses tuiles de modules, ses widgets
 * et ses formulaires exclusivement depuis GET /spaces/:id/bootstrap
 * (template G2.3 + modules activés G1.3/G2.2 + statuts G2.7 + champs
 * personnalisés G2.4 + membres G3.2 + permissions résolues serveur §G4.4).
 * Deux espaces = deux expériences distinctes depuis la même base de code.
 * Aucun bouton mort : les modules sans page réelle sont informatifs, les
 * widgets inconnus ne sont pas rendus, l'édition est réservée aux porteurs
 * de la permission serveur (canCustomize), ré-évaluée à chaque requête.
 */

interface ModuleBootstrap { code: string; enabled: boolean }
interface FieldBootstrap {
  key: string; label: string; type: string; required: boolean;
  options: string[]; placeholder?: string | null; defaultValue?: string | null;
}
interface StatusColumn {
  code: string; name: string; color?: string | null; icon?: string | null;
  displayOrder?: number; initial?: boolean; final?: boolean;
  allowedTransitions?: string[];
}
interface MemberBootstrap {
  personId: string; fullName: string | null; responsibility: string | null;
  membershipType: string | null; joinedAt: string | null;
}
interface SpaceBootstrap {
  spaceId: string;
  tenantId: string;
  organizationUnitId: string | null;
  spaceType: 'DEPARTMENT' | 'FAMILY' | 'SUB_TEAM';
  templateCode: string | null;
  name: string;
  code: string;
  icon: string | null;
  color: string | null;
  description: string | null;
  status: string;
  visiblePeopleScope: string;
  entityType: string;
  modules: ModuleBootstrap[];
  statuses: StatusColumn[];
  customFields: FieldBootstrap[];
  widgets: Array<Record<string, unknown>>;
  widgetsLocked: boolean;
  permissions: { canCustomize: boolean; permissionKeys: string[]; roleKeys: string[] };
  memberCount: number;
  membersPreview: MemberBootstrap[];
  uiConfig: Record<string, unknown>;
  actions: Record<string, unknown>;
  updatedAt: string | null;
}

/** Modules → vraies pages de l'app (parité garde routeAccess). Un module sans
 *  mapping reste une tuile informative, jamais un bouton mort. */
const MODULE_ROUTES: Record<string, { href: string; icon: typeof Boxes }> = {
  EVENEMENTS: { href: '/events', icon: Calendar },
  EVENTS: { href: '/events', icon: Calendar },
  TACHES: { href: '/team-tasks', icon: ListTodo },
  TASKS: { href: '/team-tasks', icon: ListTodo },
  DOCUMENTS: { href: '/documents', icon: FolderOpen },
  FINANCE: { href: '/finances', icon: Wallet },
  BUDGET: { href: '/budgets', icon: PiggyBank },
  PRIERES: { href: '/prayers', icon: GraduationCap },
  INVENTAIRE: { href: '/inventory', icon: Boxes },
  EQUIPEMENTS: { href: '/inventory', icon: Boxes },
  FORMATION: { href: '/trainings', icon: GraduationCap },
  WORKFLOW: { href: '/workflow', icon: Workflow },
};

function widgetType(w: Record<string, unknown>): string | null {
  const t = w.type ?? w.widget ?? w.kind;
  return typeof t === 'string' ? t.toUpperCase() : null;
}

export default function SpaceOsPage() {
  const { id } = useParams<{ id: string }>();
  const { t } = useI18n();
  const queryClient = useQueryClient();
  const [tab, setTab] = useState<'overview' | 'members' | 'statuses' | 'fields' | 'settings'>('overview');

  const bootstrapQuery = useQuery<SpaceBootstrap>({
    queryKey: ['space', 'bootstrap', id],
    queryFn: async () => (await api.get(`/spaces/${id}/bootstrap`)).data,
    enabled: !!id,
  });

  const boot = bootstrapQuery.data;

  // Expérience générée : les onglets dépendent de la configuration résolue.
  const tabs = useMemo(() => {
    const list: Array<{ id: typeof tab; label: string; icon: typeof Boxes }> = [
      { id: 'overview', label: t('spaceOs.overview'), icon: LayoutDashboard },
    ];
    list.push({ id: 'members', label: `${t('spaceOs.members')} (${boot?.memberCount ?? 0})`, icon: Users });
    if ((boot?.statuses.length ?? 0) > 0) list.push({ id: 'statuses', label: t('spaceOs.statuses'), icon: CircleSlash });
    if ((boot?.customFields.length ?? 0) > 0) list.push({ id: 'fields', label: t('spaceOs.fields'), icon: FileText });
    if (boot?.permissions.canCustomize) list.push({ id: 'settings', label: t('spaceOs.settings'), icon: Settings });
    return list;
  }, [boot, t]);

  useEffect(() => {
    // si l'onglet courant disparaît (changement de config propagé §G2.7), retour à overview
    if (boot && !tabs.some((x) => x.id === tab)) setTab('overview');
  }, [boot, tabs, tab]);

  if (bootstrapQuery.isLoading) {
    return (
      <div className="page-container flex items-center justify-center min-h-[50vh]">
        <Loader2 className="w-8 h-8 animate-spin text-primary-500" />
      </div>
    );
  }

  if (bootstrapQuery.isError || !boot) {
    return (
      <div className="page-container">
        <div className="glass-card p-10 text-center">
          <CircleSlash className="w-10 h-10 text-gray-300 dark:text-gray-600 mx-auto mb-3" />
          <p className="text-sm text-red-500">{t('spaceOs.loadErr')}</p>
          <button type="button" className="btn btn-secondary btn-sm mt-4" onClick={() => bootstrapQuery.refetch()}>
            {t('organization.retry')}
          </button>
        </div>
      </div>
    );
  }

  const accent = boot.color ?? '#6366f1';

  return (
    <div className="page-container">
      {/* En-tête généré (couleur/icônes §G2.7 customisation visible) */}
      <div className="page-header">
        <div className="animate-fade-in">
          <div className="flex items-center gap-2 mb-1">
            <span className="p-1.5 rounded-lg" style={{ backgroundColor: `${accent}1a`, color: accent }}>
              <Boxes className="w-4 h-4" />
            </span>
            <span
              className="text-sm font-medium uppercase tracking-wider"
              style={{ color: accent }}
            >
              {t(`spaceOs.type.${boot.spaceType}`)}
              {boot.templateCode ? ` · ${boot.templateCode}` : ''}
            </span>
          </div>
          <h1 className="page-title">
            {boot.name}{' '}
            <span className="text-gradient font-display">{boot.code}</span>
          </h1>
          <p className="page-subtitle">
            {boot.description || t('spaceOs.noDescription')}
          </p>
        </div>
        <Link to="/organization" className="btn btn-secondary btn-sm shrink-0">
          {t('spaceOs.orgBrowser')}
        </Link>
      </div>

      {/* Onglets GÉNÉRÉS depuis la config (jamais codés par espace) */}
      <div className="mb-6 -mx-4 px-4 overflow-x-auto scrollbar-hide">
        <div className="flex items-center gap-1 pb-2 min-w-max" role="tablist">
          {tabs.map((x) => {
            const Icon = x.icon;
            const active = tab === x.id;
            return (
              <button
                key={x.id}
                type="button"
                role="tab"
                aria-selected={active}
                onClick={() => setTab(x.id)}
                className={`flex items-center gap-1.5 px-3 py-2 rounded-xl text-xs font-medium transition-all duration-200 whitespace-nowrap ${
                  active
                    ? 'text-white shadow-sm'
                    : 'text-gray-500 hover:text-gray-700 dark:hover:text-gray-300 hover:bg-gray-100 dark:hover:bg-gray-800/50'
                }`}
                style={active ? { backgroundColor: accent } : undefined}
              >
                <Icon className="w-3.5 h-3.5" />
                {x.label}
              </button>
            );
          })}
        </div>
      </div>

      {tab === 'overview' && <OverviewTab boot={boot} accent={accent} />}
      {tab === 'members' && <MembersTab boot={boot} />}
      {tab === 'statuses' && <StatusesTab boot={boot} />}
      {tab === 'fields' && (
        <FieldsTab
          boot={boot}
          onSave={() => queryClient.invalidateQueries({ queryKey: ['space', 'bootstrap', id] })}
        />
      )}
      {tab === 'settings' && boot.permissions.canCustomize && (
        <SettingsTab
          boot={boot}
          onSaved={() => queryClient.invalidateQueries({ queryKey: ['space', 'bootstrap', id] })}
        />
      )}
    </div>
  );
}

/* ────────────────────────────────────────────────────────────────────────── */

function OverviewTab({ boot, accent }: { boot: SpaceBootstrap; accent: string }) {
  const { t } = useI18n();

  const stats = [
    { label: t('spaceOs.memberCount'), value: boot.memberCount },
    { label: t('spaceOs.moduleCount'), value: boot.modules.length },
    { label: t('spaceOs.statusCount'), value: boot.statuses.length },
    { label: t('spaceOs.fieldCount'), value: boot.customFields.length },
  ];

  // Widgets connus uniquement — un type inconnu n'est JAMAIS simulé.
  const knownWidgets = (boot.widgets ?? []).filter((w) =>
    ['MEMBERS', 'STATUSES', 'CUSTOM_FIELDS'].includes(widgetType(w) ?? ''));

  return (
    <div className="space-y-6">
      <div className="grid grid-cols-2 lg:grid-cols-4 gap-4 animate-slide-up">
        {stats.map((s) => (
          <div key={s.label} className="stat-card">
            <span className="stat-label">{s.label}</span>
            <span className="stat-value">{s.value}</span>
          </div>
        ))}
      </div>

      {/* Tuiles de modules activés (vraies pages quand elles existent) */}
      <div className="glass-card p-6 animate-slide-up" style={{ animationDelay: '60ms' }}>
        <div className="flex items-center justify-between mb-4">
          <h3 className="text-lg font-semibold text-gray-900 dark:text-gray-100">{t('spaceOs.modules')}</h3>
          {boot.widgetsLocked && (
            <span className="inline-flex items-center gap-1 badge text-[10px] bg-amber-100 dark:bg-amber-900/30 text-amber-700 dark:text-amber-400">
              <Lock className="w-3 h-3" /> {t('spaceOs.widgetsLocked')}
            </span>
          )}
        </div>
        {boot.modules.length === 0 ? (
          <p className="text-sm text-gray-400">{t('spaceOs.noModules')}</p>
        ) : (
          <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-3">
            {boot.modules.map((m) => {
              const route = MODULE_ROUTES[m.code.toUpperCase()];
              const Icon = route ? route.icon : Boxes;
              const inner = (
                <div
                  className={`flex items-center gap-3 rounded-xl border p-3 transition-all ${
                    m.enabled && route
                      ? 'border-gray-200 dark:border-gray-700 hover:border-primary-500/50 hover:shadow-md bg-white dark:bg-gray-900'
                      : 'border-dashed border-gray-200 dark:border-gray-700 opacity-70'
                  }`}
                >
                  <span
                    className="p-2 rounded-lg shrink-0"
                    style={{ backgroundColor: `${accent}14`, color: m.enabled ? accent : '#9ca3af' }}
                  >
                    <Icon className="w-4 h-4" />
                  </span>
                  <div className="min-w-0">
                    <p className="text-sm font-medium text-gray-800 dark:text-gray-200 truncate">{m.code}</p>
                    <p className="text-[10px] text-gray-400">
                      {m.enabled
                        ? route ? t('spaceOs.openModule') : t('spaceOs.moduleInformative')
                        : t('spaceOs.moduleDisabled')}
                    </p>
                  </div>
                  {m.enabled && route && <ChevronRight className="w-4 h-4 text-gray-300 ml-auto shrink-0" />}
                </div>
              );
              return m.enabled && route ? (
                <Link key={m.code} to={route.href}>{inner}</Link>
              ) : (
                <div key={m.code}>{inner}</div>
              );
            })}
          </div>
        )}
      </div>

      {/* Widgets paramétrés (template G2.3 ou surcharge espace) — vraies données */}
      {knownWidgets.length > 0 && (
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-6 animate-slide-up" style={{ animationDelay: '120ms' }}>
          {knownWidgets.map((w, i) => {
            const type = widgetType(w);
            if (type === 'MEMBERS') {
              return (
                <div key={i} className="glass-card p-6">
                  <h4 className="text-sm font-semibold mb-3 text-gray-900 dark:text-gray-100">{t('spaceOs.recentMembers')}</h4>
                  <ul className="space-y-2">
                    {boot.membersPreview.slice(0, 5).map((m) => (
                      <li key={m.personId} className="flex items-center gap-2 text-sm text-gray-700 dark:text-gray-300">
                        <Users className="w-3.5 h-3.5 text-gray-400" />
                        <span className="truncate">{m.fullName ?? m.personId}</span>
                        {m.responsibility && <span className="badge text-[10px] ml-auto shrink-0">{m.responsibility}</span>}
                      </li>
                    ))}
                    {boot.membersPreview.length === 0 && (
                      <li className="text-sm text-gray-400">{t('spaceOs.emptyMembers')}</li>
                    )}
                  </ul>
                </div>
              );
            }
            if (type === 'STATUSES') {
              return (
                <div key={i} className="glass-card p-6">
                  <h4 className="text-sm font-semibold mb-3 text-gray-900 dark:text-gray-100">{t('spaceOs.statusesPreview')}</h4>
                  <div className="flex flex-wrap gap-2">
                    {boot.statuses.map((s) => (
                      <span
                        key={s.code}
                        className="inline-flex items-center gap-1 rounded-full px-2.5 py-1 text-xs font-medium"
                        style={{ backgroundColor: `${s.color ?? '#6366f1'}1a`, color: s.color ?? '#6366f1' }}
                      >
                        {s.name}
                      </span>
                    ))}
                  </div>
                </div>
              );
            }
            return (
              <div key={i} className="glass-card p-6">
                <h4 className="text-sm font-semibold mb-3 text-gray-900 dark:text-gray-100">{t('spaceOs.fieldsPreview')}</h4>
                <ul className="space-y-1">
                  {boot.customFields.map((f) => (
                    <li key={f.key} className="flex items-center gap-2 text-sm text-gray-700 dark:text-gray-300">
                      <FileText className="w-3.5 h-3.5 text-gray-400" />
                      <span className="truncate">{f.label}</span>
                      {f.required && <span className="text-red-500 text-xs ml-auto shrink-0">*</span>}
                    </li>
                  ))}
                </ul>
              </div>
            );
          })}
        </div>
      )}

      <p className="text-[10px] text-gray-400 flex items-center gap-1">
        <ShieldCheck className="w-3 h-3" />
        {t('spaceOs.generatedFrom')} {boot.templateCode ?? boot.spaceType}
        {boot.updatedAt ? ` · ${new Date(boot.updatedAt).toLocaleString()}` : ''}
      </p>
    </div>
  );
}

/* ────────────────────────────────────────────────────────────────────────── */

function MembersTab({ boot }: { boot: SpaceBootstrap }) {
  const { t } = useI18n();
  if (boot.membersPreview.length === 0) {
    return (
      <div className="glass-card p-10 text-center animate-slide-up">
        <Users className="w-10 h-10 text-gray-300 dark:text-gray-600 mx-auto mb-3" />
        <p className="text-sm text-gray-500">{t('spaceOs.emptyMembers')}</p>
      </div>
    );
  }
  return (
    <div className="glass-card p-6 animate-slide-up">
      <div className="flex items-center justify-between mb-4">
        <h3 className="text-lg font-semibold text-gray-900 dark:text-gray-100">{t('spaceOs.members')}</h3>
        <span className="badge-info text-[10px]">{boot.memberCount}</span>
      </div>
      <div className="overflow-x-auto">
        <table className="w-full text-sm">
          <thead>
            <tr className="text-left text-[10px] uppercase tracking-wide text-gray-400">
              <th className="pb-2">{t('spaceOs.member')}</th>
              <th className="pb-2">{t('spaceOs.membershipType')}</th>
              <th className="pb-2">{t('spaceOs.responsibility')}</th>
              <th className="pb-2">{t('spaceOs.joined')}</th>
            </tr>
          </thead>
          <tbody>
            {boot.membersPreview.map((m) => (
              <tr key={m.personId} className="border-t border-gray-100 dark:border-gray-800">
                <td className="py-2.5">
                  <Link to={`/souls/${m.personId}`} className="text-primary-600 dark:text-primary-400 hover:underline">
                    {m.fullName ?? m.personId}
                  </Link>
                </td>
                <td className="py-2.5 text-gray-500">{m.membershipType ?? '—'}</td>
                <td className="py-2.5 text-gray-500">{m.responsibility ?? '—'}</td>
                <td className="py-2.5 text-gray-400 text-xs">
                  {m.joinedAt ? new Date(m.joinedAt).toLocaleDateString() : '—'}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
      {boot.memberCount > boot.membersPreview.length && (
        <p className="text-[11px] text-gray-400 mt-3">{t('spaceOs.membersTruncated')}</p>
      )}
    </div>
  );
}

/* ────────────────────────────────────────────────────────────────────────── */

function StatusesTab({ boot }: { boot: SpaceBootstrap }) {
  const { t } = useI18n();
  return (
    <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4 animate-slide-up">
      {boot.statuses.map((s) => (
        <div key={s.code} className="glass-card p-5 relative overflow-hidden">
          <div className="absolute top-0 left-0 right-0 h-1" style={{ backgroundColor: s.color ?? '#6366f1' }} />
          <div className="flex items-center justify-between mb-2">
            <h4 className="font-semibold text-gray-900 dark:text-gray-100">{s.name}</h4>
            <span className="text-[10px] font-mono text-gray-400">{s.code}</span>
          </div>
          <div className="flex flex-wrap gap-1.5 mb-3">
            {s.initial && <span className="badge text-[10px] bg-green-100 dark:bg-green-900/30 text-green-700 dark:text-green-400">{t('spaceOs.initial')}</span>}
            {s.final && <span className="badge text-[10px] bg-blue-100 dark:bg-blue-900/30 text-blue-700 dark:text-blue-400">{t('spaceOs.final')}</span>}
          </div>
          <p className="text-[11px] uppercase tracking-wide text-gray-400 mb-1">{t('spaceOs.transitions')}</p>
          {(s.allowedTransitions?.length ?? 0) > 0 ? (
            <div className="flex flex-wrap gap-1">
              {s.allowedTransitions!.map((tr) => (
                <span key={tr} className="inline-flex items-center gap-1 rounded-lg bg-gray-100 dark:bg-gray-800 px-2 py-0.5 text-xs text-gray-600 dark:text-gray-300">
                  <ChevronRight className="w-3 h-3" />{tr}
                </span>
              ))}
            </div>
          ) : (
            <p className="text-xs text-gray-400">{t('spaceOs.noTransitions')}</p>
          )}
        </div>
      ))}
    </div>
  );
}

/* ────────────────────────────────────────────────────────────────────────── */

function FieldsTab({ boot, onSave }: { boot: SpaceBootstrap; onSave: () => void }) {
  const { t } = useI18n();
  const [values, setValues] = useState<Record<string, string>>({});

  // Valeurs courantes RÉELLES (bundle G2.4 scopé serveur).
  const bundleQuery = useQuery<Record<string, unknown>>({
    queryKey: ['custom-fields', boot.entityType, boot.spaceId],
    queryFn: async () => (await api.get(`/custom-fields/${boot.entityType}/${boot.spaceId}`)).data ?? {},
  });

  useEffect(() => {
    if (!bundleQuery.data) return;
    const raw = bundleQuery.data;
    // tolère {code: valeur} plat ou {values: {code: valeur}}
    const valuesObj = raw.values && typeof raw.values === 'object' && !Array.isArray(raw.values)
      ? raw.values as Record<string, unknown>
      : raw;
    const next: Record<string, string> = {};
    for (const f of boot.customFields) {
      const v = valuesObj[f.key];
      next[f.key] = typeof v === 'string' ? v : v != null ? String(v) : (f.defaultValue ?? '');
    }
    setValues(next);
  }, [bundleQuery.data, boot.customFields]);

  const editable = boot.permissions.canCustomize;

  const saveMutation = useMutation({
    mutationFn: async () => {
      await api.put(`/custom-fields/${boot.entityType}/${boot.spaceId}`, { values });
    },
    onSuccess: () => {
      toast.success(t('spaceOs.valuesSaved'));
      bundleQuery.refetch();
      onSave();
    },
    onError: () => toast.error(t('spaceOs.valuesSaveErr')),
  });

  return (
    <div className="glass-card p-6 animate-slide-up max-w-2xl">
      <div className="flex items-center justify-between mb-4">
        <h3 className="text-lg font-semibold text-gray-900 dark:text-gray-100">{t('spaceOs.fields')}</h3>
        <span className="badge-info text-[10px]">{boot.customFields.length}</span>
      </div>
      <div className="space-y-4">
        {boot.customFields.map((f) => (
          <div key={f.key}>
            <label htmlFor={`cf-${f.key}`} className="block text-xs font-medium text-gray-500 dark:text-gray-400 mb-1">
              {f.label}
              {f.required && <span className="text-red-500 ml-1">*</span>}
              <span className="text-gray-300 dark:text-gray-600 ml-1.5 font-normal uppercase text-[9px]">{f.type}</span>
            </label>
            {f.type === 'PICKLIST' || f.type === 'SELECT' ? (
              <select
                id={`cf-${f.key}`}
                className="input text-sm"
                disabled={!editable}
                value={values[f.key] ?? ''}
                onChange={(e) => setValues((v) => ({ ...v, [f.key]: e.target.value }))}
              >
                <option value="">—</option>
                {f.options.map((o) => <option key={o} value={o}>{o}</option>)}
              </select>
            ) : f.type === 'BOOLEAN' ? (
              <select
                id={`cf-${f.key}`}
                className="input text-sm"
                disabled={!editable}
                value={values[f.key] ?? ''}
                onChange={(e) => setValues((v) => ({ ...v, [f.key]: e.target.value }))}
              >
                <option value="">—</option>
                <option value="true">✓</option>
                <option value="false">✗</option>
              </select>
            ) : (
              <input
                id={`cf-${f.key}`}
                className="input text-sm"
                type={f.type === 'NUMBER' ? 'number' : f.type === 'DATE' ? 'date' : 'text'}
                disabled={!editable}
                placeholder={f.placeholder ?? undefined}
                value={values[f.key] ?? ''}
                onChange={(e) => setValues((v) => ({ ...v, [f.key]: e.target.value }))}
              />
            )}
          </div>
        ))}
      </div>
      <button
        type="button"
        disabled={!editable || saveMutation.isPending || bundleQuery.isLoading}
        className="btn-glow btn-sm mt-6 inline-flex items-center gap-2 disabled:opacity-50"
        onClick={() => saveMutation.mutate()}
        title={editable ? undefined : t('spaceOs.readOnlyFields')}
      >
        {saveMutation.isPending ? <Loader2 className="w-4 h-4 animate-spin" /> : <Save className="w-4 h-4" />}
        {t('spaceOs.saveValues')}
      </button>
    </div>
  );
}

/* ────────────────────────────────────────────────────────────────────────── */

function SettingsTab({ boot, onSaved }: { boot: SpaceBootstrap; onSaved: () => void }) {
  const { t } = useI18n();
  const [name, setName] = useState(boot.name);
  const [description, setDescription] = useState(boot.description ?? '');
  const [icon, setIcon] = useState(boot.icon ?? '');
  const [color, setColor] = useState(boot.color ?? '#6366f1');

  const saveMutation = useMutation({
    // PUT partiel : le serveur n'applique que les champs non-null (§0.3 —
    // la permission canCustomize est revérifiée serveur, jamais supposée).
    mutationFn: async () => {
      await api.put(`/spaces/${boot.spaceId}`, {
        name, description, icon: icon || null, color: color || null,
      });
    },
    onSuccess: () => {
      toast.success(t('spaceOs.settingsSaved'));
      onSaved();
    },
    onError: () => toast.error(t('spaceOs.settingsSaveErr')),
  });

  return (
    <div className="glass-card p-6 animate-slide-up max-w-2xl">
      <h3 className="text-lg font-semibold text-gray-900 dark:text-gray-100 mb-1">{t('spaceOs.settings')}</h3>
      <p className="text-xs text-gray-400 mb-5">{t('spaceOs.settingsHint')}</p>
      <div className="space-y-4">
        <div>
          <label className="block text-xs font-medium text-gray-500 mb-1" htmlFor="space-name">{t('spaceOs.spaceName')}</label>
          <input id="space-name" className="input text-sm" value={name} onChange={(e) => setName(e.target.value)} minLength={2} />
        </div>
        <div>
          <label className="block text-xs font-medium text-gray-500 mb-1" htmlFor="space-desc">{t('spaceOs.description')}</label>
          <textarea id="space-desc" className="input text-sm min-h-[80px]" value={description} onChange={(e) => setDescription(e.target.value)} />
        </div>
        <div className="grid grid-cols-2 gap-4">
          <div>
            <label className="block text-xs font-medium text-gray-500 mb-1" htmlFor="space-icon">{t('spaceOs.icon')}</label>
            <input id="space-icon" className="input text-sm" value={icon} onChange={(e) => setIcon(e.target.value)} placeholder="lucide" />
          </div>
          <div>
            <label className="block text-xs font-medium text-gray-500 mb-1" htmlFor="space-color">{t('spaceOs.color')}</label>
            <div className="flex items-center gap-2">
              <input id="space-color" type="color" className="h-9 w-14 rounded cursor-pointer border-0 bg-transparent" value={/^#[0-9a-fA-F]{6}$/.test(color) ? color : '#6366f1'} onChange={(e) => setColor(e.target.value)} />
              <input className="input text-sm flex-1" value={color} onChange={(e) => setColor(e.target.value)} />
            </div>
          </div>
        </div>
      </div>
      <button
        type="button"
        disabled={saveMutation.isPending || name.trim().length < 2}
        className="btn-glow btn-sm mt-6 inline-flex items-center gap-2"
        onClick={() => saveMutation.mutate()}
      >
        {saveMutation.isPending ? <Loader2 className="w-4 h-4 animate-spin" /> : <CheckCircle2 className="w-4 h-4" />}
        {t('spaceOs.saveSettings')}
      </button>
    </div>
  );
}
