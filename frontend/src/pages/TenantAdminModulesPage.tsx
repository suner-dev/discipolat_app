import { useEffect, useMemo, useState } from "react";
import { useTenant } from "@/contexts/TenantContext";
import api from "@/lib/api";
import { useI18n } from "@/i18n";
import ModuleMultiSelect from "@/components/organization/ModuleMultiSelect";
import {
  useOrgTreeV3, useNodeFeatures, useSetNodeFeatures, type NodeFeature,
} from "@/hooks/useOrganizationV3";

interface TenantFeature {
  id: string;
  tenantId: string;
  moduleCode: string;
  enabled: boolean;
  configuration: Record<string, any>;
  limits: Record<string, any>;
  createdAt: string;
  updatedAt: string;
}

const MODULE_LABELS: Record<string, string> = {
  people: "Membres",
  events: "Événements",
  notifications: "Notifications",
  dashboard: "Tableau de bord",
  org: "Organisation",
  families: "Familles",
  groups: "Groupes",
  discipleship: "Discipleship",
  academy: "Académie",
  finance: "Finances",
  media: "Médias",
  pastoral: "Pastoral",
  prayer: "Prière",
  assets: "Matériel",
  workflow: "Workflows",
  custom_fields: "Champs personnalisés",
  dress_code: "Dress Code",
  health: "Santé / Infirmerie",
  reports: "Rapports",
  analytics: "Analytics",
  messaging: "Messagerie",
  documents: "Documents",
  calendar: "Calendrier",
  forms: "Formulaires",
  marketplace: "Marketplace",
  ai: "Intelligence Artificielle",
  chat: "Chat",
  payments: "Paiements",
  sermons: "Sermons",
};

function formatModuleLabel(key: string) {
  return MODULE_LABELS[key] || key
    .replace(/_/g, " ")
    .replace(/\b\w/g, (l) => l.toUpperCase());
}

export default function TenantAdminModulesPage() {
  const { hasPermission } = useTenant();
  const { t } = useI18n();
  const [features, setFeatures] = useState<TenantFeature[]>([]);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState<string | null>(null);
  const [message, setMessage] = useState<{ type: string; text: string } | null>(null);
  // §6.2 (D) / T-W14 — portée : modules du tenant (racine) OU d'un nœud
  // (campus/église) via `organization_node_features`. Indépendance par défaut
  // (V3-D) : un nœud n'hérite jamais silencieusement des modules du tenant.
  const [scope, setScope] = useState<"tenant" | "node">("tenant");
  const [nodeId, setNodeId] = useState<string>("");

  useEffect(() => {
    if (!hasPermission("MODULES_READ")) return;
    fetchFeatures();
  }, [hasPermission]);

  const fetchFeatures = async () => {
    try {
      const res = await api.get("/admin/tenant-features");
      const featureList: TenantFeature[] = res.data;
      setFeatures(featureList);
    } catch (error) {
      console.error("Erreur:", error);
      setMessage({ type: "error", text: "Erreur lors du chargement des modules" });
    } finally {
      setLoading(false);
    }
  };

  const toggleFeature = async (feature: TenantFeature) => {
    if (!hasPermission("MODULES_TOGGLE")) return;
    setSaving(feature.moduleCode);
    try {
      const newEnabled = !feature.enabled;
      await api.put(`/admin/tenant-features/${feature.moduleCode}`, { enabled: newEnabled });
      setMessage({ type: "success", text: newEnabled ? "Module activé" : "Module désactivé" });
      fetchFeatures();
    } catch (error) {
      setMessage({ type: "error", text: "Erreur lors de la modification" });
      console.error("Erreur:", error);
    } finally {
      setSaving(null);
    }
  };

  return (
    <div className="p-6">
      <div className="flex justify-between items-center mb-6">
        <h1 className="text-2xl font-bold">Modules et Fonctionnalités</h1>
        <button
          onClick={() => fetchFeatures()}
          className="px-4 py-2 bg-indigo-600 text-white rounded-lg hover:bg-indigo-700"
        >
          Actualiser
        </button>
      </div>

      {/* §6.2 — sélecteur de portée tenant ↔ nœud. */}
      <div className="flex flex-wrap items-center gap-2 mb-6">
        <button
          type="button"
          onClick={() => setScope("tenant")}
          className={`px-3 py-1.5 rounded-lg text-sm font-medium ${
            scope === "tenant" ? "bg-indigo-600 text-white" : "bg-gray-100 text-gray-600"
          }`}
        >
          {t("orgV3.modulesScope.tenant")}
        </button>
        <button
          type="button"
          onClick={() => setScope("node")}
          className={`px-3 py-1.5 rounded-lg text-sm font-medium ${
            scope === "node" ? "bg-indigo-600 text-white" : "bg-gray-100 text-gray-600"
          }`}
        >
          {t("orgV3.modulesScope.node")}
        </button>
        {scope === "node" && <NodePicker value={nodeId} onChange={setNodeId} />}
      </div>

      {message && (
        <div className={`p-4 rounded-lg mb-6 ${message.type === "success" ? "bg-green-100 text-green-700" : "bg-red-100 text-red-700"}`}>
          {message.text}
        </div>
      )}

      {scope === "node" ? (
        nodeId ? (
          <NodeModulesPanel nodeId={nodeId} />
        ) : (
          <p className="text-sm text-gray-400">{t("orgV3.modulesScope.pickNode")}</p>
        )
      ) : loading ? (
        <div className="p-8 text-center">Chargement...</div>
      ) : (
        <>
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
            {features.map((feature) => (
              <div
                key={feature.moduleCode}
                className={`p-4 rounded-lg border ${
                  feature.enabled ? "border-indigo-200 bg-indigo-50" : "border-gray-200 bg-gray-50"
                }`}
              >
                <div className="flex items-center justify-between">
                  <div className="flex-1">
                    <h3 className="font-medium">{formatModuleLabel(feature.moduleCode)}</h3>
                    <p className="text-sm text-gray-500 mt-1">
                      {feature.enabled ? "Activé" : "Désactivé"}
                      {feature.limits && Object.keys(feature.limits).length > 0 && (
                        <>
                          " - "
                          {Object.entries(feature.limits)
                            .map(([k, v]) => `${k}: ${v}`)
                            .join(", ")}
                        </>
                      )}
                    </p>
                    <p className="text-xs text-gray-400 mt-1">{feature.moduleCode}</p>
                  </div>
                  <button
                    onClick={() => toggleFeature(feature)}
                    disabled={saving === feature.moduleCode || !hasPermission("MODULES_TOGGLE")}
                    className={`relative w-11 h-6 rounded-full ${
                      feature.enabled ? "bg-indigo-600" : "bg-gray-300"
                    } transition-colors disabled:opacity-50`}
                  >
                    <span className={`absolute top-1 left-1 w-4 h-4 bg-white rounded-full shadow ${
                      feature.enabled ? "translate-x-5" : ""
                    }`} />
                  </button>
                </div>
              </div>
            ))}
          </div>

          {features.length === 0 && (
            <div className="text-center py-12 bg-gray-50 rounded-lg border">
              <p className="text-gray-500 mb-2">Aucun module configuré</p>
              <p className="text-sm text-gray-400">
                Les modules de base (people, events, notifications) sont activés automatiquement à la création du tenant.
              </p>
            </div>
          )}
        </>
      )}

      {saving && (
        <div className="fixed bottom-4 right-4 bg-gray-800 text-white px-4 py-2 rounded-lg">
          Enregistrement...
        </div>
      )}
    </div>
  );
}

/** Sélecteur de nœud (arbre V3) pour la portée « modules par nœud ». */
function NodePicker({ value, onChange }: { value: string; onChange: (id: string) => void }) {
  const { t } = useI18n();
  const tree = useOrgTreeV3();
  const nodes = useMemo(() => [...(tree.data ?? [])].sort((a, b) => a.name.localeCompare(b.name)), [tree.data]);
  return (
    <select
      value={value}
      onChange={(e) => onChange(e.target.value)}
      aria-label={t("orgV3.modulesScope.pickNode")}
      className="px-3 py-1.5 rounded-lg border text-sm bg-white dark:bg-gray-800"
    >
      <option value="">{tree.isLoading ? "…" : t("orgV3.modulesScope.pickNode")}</option>
      {nodes.map((n) => (
        <option key={n.id} value={n.id}>
          {n.levelName ? `${n.name} — ${n.levelName}` : n.name}
        </option>
      ))}
    </select>
  );
}

/**
 * §6.2 (D) — modules d'un NŒUD (indépendants par défaut, V3-D). Réutilise
 * `<ModuleMultiSelect>` ; la sauvegarde remplace la sélection du nœud (PUT),
 * sans jamais toucher aux modules du tenant ni des autres nœuds.
 */
function NodeModulesPanel({ nodeId }: { nodeId: string }) {
  const { t } = useI18n();
  const features = useNodeFeatures(nodeId);
  const setModules = useSetNodeFeatures(nodeId);
  const [selected, setSelected] = useState<string[]>([]);
  const [saved, setSaved] = useState<string | null>(null);

  // Synchronise la sélection depuis l'API (uniquement quand les features
  // changent et qu'on n'a pas de modification en cours non sauvegardée).
  useEffect(() => {
    const enabled = (features.data ?? []).filter((f: NodeFeature) => f.enabled).map((f) => f.moduleCode);
    setSelected(enabled);
    setSaved(null);
  }, [nodeId, features.data]);

  const dirty = useMemo(() => {
    const base = new Set((features.data ?? []).filter((f) => f.enabled).map((f) => f.moduleCode));
    if (base.size !== selected.length) return true;
    return selected.some((c) => !base.has(c));
  }, [features.data, selected]);

  const save = async () => {
    try {
      await setModules.mutateAsync(selected.map((code) => ({ code, enabled: true })));
      setSaved(t("orgV3.modules.saved"));
    } catch {
      setSaved(t("orgV3.modules.saveErr"));
    }
  };

  if (features.isLoading) return <div className="p-8 text-center">Chargement...</div>;

  return (
    <div className="p-4 rounded-lg border bg-white dark:bg-gray-900 space-y-4">
      <p className="text-sm text-gray-500">{t("orgV3.modulesScope.hint")}</p>
      <ModuleMultiSelect value={selected} onChange={setSelected} />
      <div className="flex items-center gap-3">
        <button
          type="button"
          onClick={save}
          disabled={setModules.isPending || !dirty}
          className="px-4 py-2 bg-indigo-600 text-white rounded-lg hover:bg-indigo-700 disabled:opacity-50"
        >
          {t("orgV3.modules.save")}
        </button>
        {saved && <span className="text-sm text-gray-500">{saved}</span>}
      </div>
    </div>
  );
}
