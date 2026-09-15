import { useEffect, useState } from "react";
import { useTenant } from "@/contexts/TenantContext";
import api from "@/lib/api";

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
};

export default function TenantAdminModulesPage() {
  const { hasPermission } = useTenant();
  const [features, setFeatures] = useState<TenantFeature[]>([]);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState<string | null>(null);
  const [message, setMessage] = useState<{ type: string; text: string } | null>(null);

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

  const formatModuleLabel = (key: string) => {
    return MODULE_LABELS[key] || key
      .replace(/_/g, " ")
      .replace(/\b\w/g, (l) => l.toUpperCase());
  };

  if (loading) return <div className="p-8 text-center">Chargement...</div>;

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

      {message && (
        <div className={`p-4 rounded-lg mb-6 ${message.type === "success" ? "bg-green-100 text-green-700" : "bg-red-100 text-red-700"}`}>
          {message.text}
        </div>
      )}

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

      {saving && (
        <div className="fixed bottom-4 right-4 bg-gray-800 text-white px-4 py-2 rounded-lg">
          Enregistrement...
        </div>
      )}
    </div>
  );
}