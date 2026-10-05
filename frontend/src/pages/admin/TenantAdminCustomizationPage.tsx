import { useMemo, useState } from 'react';
import toast from 'react-hot-toast';
import { Loader2, Plus, Trash2, Eye, EyeOff, Save, LayoutList, Languages, ToggleLeft } from 'lucide-react';
import { FeatureGate } from '@/config/useUiCustomization';
import {
  buildGroupTree,
  useCreateNavigationGroup,
  useDeleteNavigationGroup,
  useDeleteUiFeature,
  useDeleteUiLabel,
  useNavigationAdminLabels,
  useNavigationGroupsAdmin,
  useUiCustomizationAdmin,
  useUpdateNavigationGroup,
  useUpsertUiFeature,
  useUpsertUiLabel,
} from '@/hooks/useNavigationAdmin';

/**
 * LOT 2 §GR + §LB — écran « Personnalisation de l'interface ».
 *
 * <p>Répond à « je veux que l'admin puisse le définir » :
 * <ul>
 *   <li><b>Onglets groupés</b> : créer, renommer, ordonner, imbriquer et
 *       replier/replier par défaut les groupes du menu. Le libellé du groupe
 *       est ce que l'utilisateur voit — un groupe s'appelle « Zones » ou
 *       « Circonscriptions », comme l'église le souhaite ;</li>
 *   <li><b>Libellés</b> : renommer n'importe quelle chaîne déjà affichée, par
 *       langue ;</li>
 *   <li><b>Fonctionnalités</b> : retirer ou renommer un bouton sur un écran.</li>
 * </ul>
 *
 * <p>L'écran est lui-même sous le paramétrage : le bouton de suppression peut
 * être retiré par l'administration (l'app tests alors la FeatureGate).
 */
type TabId = 'groups' | 'labels' | 'features';

export default function TenantAdminCustomizationPage() {
  const labels = useNavigationAdminLabels();
  const [tab, setTab] = useState<TabId>('groups');

  return (
    <div className="space-y-5 animate-fade-in">
      <header>
        <h1 className="text-2xl font-bold text-gray-900 dark:text-gray-100">
          {labels.title || 'Personnalisation de l’interface'}
        </h1>
        <p className="text-sm text-gray-500 dark:text-gray-400 mt-1">
          {labels.subtitle ||
            'Regroupez les onglets, renommez les libellés et retirez les fonctionnalités que vous n’utilisez pas.'}
        </p>
      </header>

      <div role="tablist" aria-label="Sections" className="flex gap-1 p-1 rounded-xl bg-gray-100/60 dark:bg-gray-800/50 w-fit">
        {(
          [
            { id: 'groups', label: labels.groupsTitle || 'Groupes d’onglets', icon: LayoutList },
            { id: 'labels', label: labels.labelsTitle || 'Libellés', icon: Languages },
            { id: 'features', label: labels.featuresTitle || 'Fonctionnalités', icon: ToggleLeft },
          ] as const
        ).map(({ id, label: text, icon: Icon }) => (
          <button
            key={id}
            type="button"
            role="tab"
            aria-selected={tab === id}
            onClick={() => setTab(id)}
            className={`inline-flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-sm font-medium transition-all duration-200 ease-smooth
              ${tab === id
                ? 'bg-white dark:bg-gray-700 text-primary-700 dark:text-primary-400 shadow-sm'
                : 'text-gray-500 dark:text-gray-400 hover:text-gray-800 dark:hover:text-gray-200'}`}
          >
            <Icon className="w-4 h-4" />
            {text}
          </button>
        ))}
      </div>

      {tab === 'groups' && <GroupsSection />}
      {tab === 'labels' && <LabelsSection />}
      {tab === 'features' && <FeaturesSection />}
    </div>
  );
}

/* ================================================================== */
/* Groupes d'onglets                                                    */
/* ================================================================== */

function GroupsSection() {
  const labels = useNavigationAdminLabels();
  const query = useNavigationGroupsAdmin();
  const create = useCreateNavigationGroup();
  const update = useUpdateNavigationGroup();
  const remove = useDeleteNavigationGroup();

  const [key, setKey] = useState('');
  const [label, setLabel] = useState('');
  const [parentGroupId, setParentGroupId] = useState<string>('');

  const groups = query.data?.groups ?? [];
  const tree = useMemo(
    () => buildGroupTree(groups, query.data?.assignments ?? {}),
    [groups, query.data?.assignments],
  );

  const submit = async () => {
    if (label.trim().length < 2) return;
    try {
      await create.mutateAsync({
        key: key.trim() || label.trim().toLowerCase().replace(/\s+/g, '-'),
        label: label.trim(),
        parentGroupId: parentGroupId || null,
        displayOrder: tree.length,
        collapsedByDefault: true,
      });
      setKey('');
      setLabel('');
      setParentGroupId('');
      toast.success(labels.saved || 'Enregistré');
    } catch {
      toast.error(labels.error || 'Enregistrement impossible');
    }
  };

  const toggle = async (id: string, field: 'enabled' | 'collapsedByDefault' | 'showCount') => {
    const target = groups.find((group) => group.id === id);
    if (!target) return;
    try {
      await update.mutateAsync({ id, payload: { [field]: !target[field] } });
    } catch {
      toast.error(labels.error || 'Enregistrement impossible');
    }
  };

  const destroy = async (id: string, name: string) => {
    if (!confirm((labels.deleteConfirm || 'Supprimer « {name} » ?').replace('{name}', name))) return;
    try {
      await remove.mutateAsync(id);
      toast.success(labels.deleted || 'Supprimé');
    } catch {
      toast.error(labels.error || 'Suppression impossible');
    }
  };

  if (query.isLoading) {
    return <Loader2 className="w-5 h-5 animate-spin text-gray-400" />;
  }

  return (
    <section className="space-y-4">
      <div className="glass-card p-4 space-y-3">
        <div className="flex flex-wrap gap-3 items-end">
          <label className="flex-1 min-w-[12rem]">
            <span className="block text-xs font-semibold text-gray-500 dark:text-gray-400 mb-1">
              {labels.label || 'Libellé'}
            </span>
            <input
              className="input"
              value={label}
              onChange={(event) => setLabel(event.target.value)}
              placeholder="Zones"
            />
          </label>
          <label className="flex-1 min-w-[10rem]">
            <span className="block text-xs font-semibold text-gray-500 dark:text-gray-400 mb-1">
              {labels.key || 'Clé'} (optionnel)
            </span>
            <input
              className="input"
              value={key}
              onChange={(event) => setKey(event.target.value)}
              placeholder="zones"
            />
          </label>
          <label className="flex-1 min-w-[10rem]">
            <span className="block text-xs font-semibold text-gray-500 dark:text-gray-400 mb-1">
              {labels.parent || 'Groupe parent'}
            </span>
            <select className="input" value={parentGroupId} onChange={(event) => setParentGroupId(event.target.value)}>
              <option value="">{labels.rootGroup || 'Aucun (premier niveau)'}</option>
              {groups.map((group) => (
                <option key={group.id} value={group.id}>
                  {group.label}
                </option>
              ))}
            </select>
          </label>
          <button
            type="button"
            onClick={submit}
            disabled={create.isPending || label.trim().length < 2}
            className="btn-primary inline-flex items-center gap-2 disabled:opacity-50"
          >
            <Plus className="w-4 h-4" />
            {labels.create || 'Créer'}
          </button>
        </div>
      </div>

      {tree.length === 0 ? (
        <p className="text-sm text-gray-500 dark:text-gray-400">
          {labels.emptyGroups ||
            'Aucun groupe personnalisé. Sans configuration, le menu est déjà regroupé par section.'}
        </p>
      ) : (
        <ul className="space-y-1.5">
          {tree.map((group) => (
            <li
              key={group.id}
              className="glass-card p-3 flex flex-wrap items-center gap-3"
              style={{ marginInlineStart: `${group.depth * 1.25}rem` }}
            >
              <div className="flex-1 min-w-[10rem]">
                <p className="text-sm font-semibold text-gray-900 dark:text-gray-100">{group.label}</p>
                <p className="text-xs text-gray-500 dark:text-gray-400">
                  <code className="text-[11px]">{group.key}</code>
                  {group.hrefCount > 0 && ` · ${group.hrefCount} onglet(s)`}
                  {group.tenantId == null && ' · réglage global (lecture seule)'}
                </p>
              </div>

              <button
                type="button"
                onClick={() => toggle(group.id, 'collapsedByDefault')}
                className="btn-ghost btn-sm inline-flex items-center gap-1.5"
                title={labels.collapsed || 'Replié par défaut'}
              >
                {group.collapsedByDefault ? <Eye className="w-3.5 h-3.5" /> : <EyeOff className="w-3.5 h-3.5" />}
                {group.collapsedByDefault ? (labels.collapsed || 'Replié') : 'Ouvert'}
              </button>

              <button
                type="button"
                onClick={() => toggle(group.id, 'enabled')}
                className="btn-ghost btn-sm"
                aria-pressed={group.enabled !== false}
              >
                {group.enabled !== false ? (labels.saved || 'Actif') : 'Inactif'}
              </button>

              <FeatureGate
                pageKey="/tenant/customization"
                featureKey="delete-group"
                fallback={
                  <span
                    className="text-xs text-gray-400 dark:text-gray-600"
                    title="Suppression désactivée par l’administration"
                  >
                    —
                  </span>
                }
              >
                <button
                  type="button"
                  onClick={() => destroy(group.id, group.label)}
                  className="btn-ghost btn-sm text-red-600 dark:text-red-400"
                  aria-label={`Supprimer ${group.label}`}
                >
                  <Trash2 className="w-4 h-4" />
                </button>
              </FeatureGate>
            </li>
          ))}
        </ul>
      )}
    </section>
  );
}

/* ================================================================== */
/* Libellés                                                             */
/* ================================================================== */

function LabelsSection() {
  const labels = useNavigationAdminLabels();
  const query = useUiCustomizationAdmin();
  const upsert = useUpsertUiLabel();
  const remove = useDeleteUiLabel();

  const [labelKey, setLabelKey] = useState('');
  const [value, setValue] = useState('');
  const [locale, setLocale] = useState('*');

  // Les surcharges « à toi » : pas celles livrées par la plateforme.
  const own = useMemo(
    () => (query.data?.labels ?? []).filter((label) => label.tenantId != null),
    [query.data?.labels],
  );

  const submit = async () => {
    if (!labelKey.trim() || !value.trim()) return;
    try {
      await upsert.mutateAsync({ labelKey: labelKey.trim(), value: value.trim(), locale });
      setLabelKey('');
      setValue('');
      toast.success(labels.saved || 'Enregistré');
    } catch {
      toast.error(labels.error || 'Enregistrement impossible');
    }
  };

  if (query.isLoading) return <Loader2 className="w-5 h-5 animate-spin text-gray-400" />;

  return (
    <section className="space-y-4">
      <div className="glass-card p-4 flex flex-wrap gap-3 items-end">
        <label className="flex-[2] min-w-[14rem]">
          <span className="block text-xs font-semibold text-gray-500 dark:text-gray-400 mb-1">
            Clé du libellé
          </span>
          <input
            className="input"
            value={labelKey}
            onChange={(event) => setLabelKey(event.target.value)}
            placeholder="nav.back"
          />
        </label>
        <label className="flex-1 min-w-[9rem]">
          <span className="block text-xs font-semibold text-gray-500 dark:text-gray-400 mb-1">
            {labels.label || 'Nouveau texte'}
          </span>
          <input className="input" value={value} onChange={(event) => setValue(event.target.value)} />
        </label>
        <label className="w-32">
          <span className="block text-xs font-semibold text-gray-500 dark:text-gray-400 mb-1">
            {labels.locale || 'Langue'}
          </span>
          <select className="input" value={locale} onChange={(event) => setLocale(event.target.value)}>
            <option value="*">{labels.anyLocale || 'Toutes'}</option>
            <option value="fr">fr</option>
            <option value="en">en</option>
            <option value="pt">pt</option>
            <option value="es">es</option>
            <option value="sw">sw</option>
            <option value="ar">ar</option>
          </select>
        </label>
        <button
          type="button"
          onClick={submit}
          disabled={upsert.isPending || !labelKey.trim() || !value.trim()}
          className="btn-primary inline-flex items-center gap-2 disabled:opacity-50"
        >
          <Save className="w-4 h-4" />
          {labels.create || 'Ajouter'}
        </button>
      </div>

      {own.length === 0 ? (
        <p className="text-sm text-gray-500 dark:text-gray-400">
          Aucune surcharge pour l’instant. L’application utilise ses libellés d’origine.
        </p>
      ) : (
        <ul className="space-y-1.5">
          {own.map((label) => (
            <li key={label.id} className="glass-card p-3 flex flex-wrap items-center gap-3">
              <div className="flex-1 min-w-[12rem]">
                <p className="text-sm font-medium text-gray-900 dark:text-gray-100">{label.value}</p>
                <p className="text-xs text-gray-500 dark:text-gray-400">
                  <code className="text-[11px]">{label.labelKey}</code> · {label.locale}
                </p>
              </div>
              <button
                type="button"
                onClick={async () => {
                  try {
                    await upsert.mutateAsync({
                      labelKey: label.labelKey,
                      value: label.value,
                      locale: label.locale,
                      enabled: !label.enabled,
                    });
                  } catch {
                    toast.error(labels.error || 'Enregistrement impossible');
                  }
                }}
                className="btn-ghost btn-sm"
              >
                {label.enabled ? 'Actif' : 'Suspendu'}
              </button>
              <button
                type="button"
                onClick={async () => {
                  try {
                    await remove.mutateAsync(label.id);
                    toast.success(labels.deleted || 'Supprimé');
                  } catch {
                    toast.error(labels.error || 'Suppression impossible');
                  }
                }}
                className="btn-ghost btn-sm text-red-600 dark:text-red-400"
                aria-label={`Supprimer ${label.labelKey}`}
              >
                <Trash2 className="w-4 h-4" />
              </button>
            </li>
          ))}
        </ul>
      )}
    </section>
  );
}

/* ================================================================== */
/* Fonctionnalités / boutons                                            */
/* ================================================================== */

function FeaturesSection() {
  const labels = useNavigationAdminLabels();
  const query = useUiCustomizationAdmin();
  const upsert = useUpsertUiFeature();
  const remove = useDeleteUiFeature();

  const [pageKey, setPageKey] = useState('');
  const [featureKey, setFeatureKey] = useState('');
  const [labelOverride, setLabelOverride] = useState('');
  const [enabled, setEnabled] = useState(true);

  const own = useMemo(
    () => (query.data?.features ?? []).filter((feature) => feature.tenantId != null),
    [query.data?.features],
  );

  const submit = async () => {
    if (!pageKey.trim() || !featureKey.trim()) return;
    try {
      await upsert.mutateAsync({
        pageKey: pageKey.trim(),
        featureKey: featureKey.trim(),
        labelOverride: labelOverride.trim() || undefined,
        enabled,
      });
      setPageKey('');
      setFeatureKey('');
      setLabelOverride('');
      toast.success(labels.saved || 'Enregistré');
    } catch {
      toast.error(labels.error || 'Enregistrement impossible');
    }
  };

  if (query.isLoading) return <Loader2 className="w-5 h-5 animate-spin text-gray-400" />;

  return (
    <section className="space-y-4">
      <div className="glass-card p-4 flex flex-wrap gap-3 items-end">
        <label className="flex-1 min-w-[9rem]">
          <span className="block text-xs font-semibold text-gray-500 dark:text-gray-400 mb-1">Écran</span>
          <input
            className="input"
            value={pageKey}
            onChange={(event) => setPageKey(event.target.value)}
            placeholder="/souls"
          />
        </label>
        <label className="flex-1 min-w-[9rem]">
          <span className="block text-xs font-semibold text-gray-500 dark:text-gray-400 mb-1">
            Fonctionnalité
          </span>
          <input
            className="input"
            value={featureKey}
            onChange={(event) => setFeatureKey(event.target.value)}
            placeholder="export"
          />
        </label>
        <label className="flex-1 min-w-[9rem]">
          <span className="block text-xs font-semibold text-gray-500 dark:text-gray-400 mb-1">
            Nouveau libellé
          </span>
          <input
            className="input"
            value={labelOverride}
            onChange={(event) => setLabelOverride(event.target.value)}
          />
        </label>
        <label className="flex items-center gap-2 pb-2">
          <input type="checkbox" checked={enabled} onChange={(event) => setEnabled(event.target.checked)} />
          <span className="text-sm text-gray-600 dark:text-gray-300">Actif</span>
        </label>
        <button
          type="button"
          onClick={submit}
          disabled={upsert.isPending || !pageKey.trim() || !featureKey.trim()}
          className="btn-primary inline-flex items-center gap-2 disabled:opacity-50"
        >
          <Save className="w-4 h-4" />
          {labels.create || 'Enregistrer'}
        </button>
      </div>

      {own.length === 0 ? (
        <p className="text-sm text-gray-500 dark:text-gray-400">
          Aucun réglage. Par défaut tout est visible : ne désactivez que ce que vous ne voulez pas.
        </p>
      ) : (
        <ul className="space-y-1.5">
          {own.map((feature) => (
            <li key={feature.id} className="glass-card p-3 flex flex-wrap items-center gap-3">
              <div className="flex-1 min-w-[12rem]">
                <p className="text-sm font-medium text-gray-900 dark:text-gray-100">
                  {feature.labelOverride || feature.featureKey}
                </p>
                <p className="text-xs text-gray-500 dark:text-gray-400">
                  <code className="text-[11px]">
                    {feature.pageKey}:{feature.featureKey}
                  </code>
                </p>
              </div>
              <button
                type="button"
                onClick={async () => {
                  try {
                    await upsert.mutateAsync({
                      pageKey: feature.pageKey,
                      featureKey: feature.featureKey,
                      labelOverride: feature.labelOverride ?? undefined,
                      enabled: !feature.enabled,
                    });
                  } catch {
                    toast.error(labels.error || 'Enregistrement impossible');
                  }
                }}
                className="btn-ghost btn-sm"
              >
                {feature.enabled ? 'Visible' : 'Retirée'}
              </button>
              <button
                type="button"
                onClick={async () => {
                  try {
                    await remove.mutateAsync(feature.id);
                    toast.success(labels.deleted || 'Supprimé');
                  } catch {
                    toast.error(labels.error || 'Suppression impossible');
                  }
                }}
                className="btn-ghost btn-sm text-red-600 dark:text-red-400"
                aria-label={`Supprimer ${feature.featureKey}`}
              >
                <Trash2 className="w-4 h-4" />
              </button>
            </li>
          ))}
        </ul>
      )}
    </section>
  );
}