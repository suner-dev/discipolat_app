import { useEffect, useState } from "react";
import { useAuth } from "@/contexts/AuthContext";
import api from "@/lib/api";

interface ModuleInfo {
  key: string;
  enabled: boolean;
  label: string;
}

export default function TenantAdminModulesPage() {
  const { hasRole } = useAuth();
  const [modules, setModules] = useState<ModuleInfo[]>([]);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [message, setMessage] = useState<{type: string, text: string} | null>(null);

  useEffect(() => {
    if (!hasRole("TENANT_OWNER") && !hasRole("TENANT_ADMIN")) return;
    fetchModules();
  }, [hasRole, message]);

  const fetchModules = async () => {
    try {
      const res = await api.get("/admin/modules");
      const moduleList = Object.entries(res.data).map(([key, enabled]: [string, boolean]) => ({
        key,
        enabled,
        label: formatModuleLabel(key)
      }));
      setModules(moduleList);
    } catch (error) {
      console.error("Erreur:", error);
    } finally {
      setLoading(false);
    }
  };

  const toggleModule = async (key: string, enabled: boolean) => {
    setSaving(true);
    try {
      await api.put(`/admin/modules/${key}`, { enabled });
      setMessage({ type: "success", text: enabled ? "Module active" : "Module desactive" });
      fetchModules();
    } catch (error) {
      setMessage({ type: "error", text: "Erreur lors de la modification" });
      console.error("Erreur:", error);
    } finally {
      setSaving(false);
    }
  };

  const formatModuleLabel = (key: string) => {
    return key
      .replace(/_/g, " ")
      .replace(/\w/g, l => l.toUpperCase());
  };

  const hasAccess = hasRole("TENANT_OWNER") || hasRole("TENANT_ADMIN");
  if (!hasAccess) {
    return <div className="p-8 text-center">Acces non autorise</div>;
  }

  if (loading) return <div className="p-8 text-center">Chargement...</div>;

  return (
    <div className="p-6">
      <div className="flex justify-between items-center mb-6">
        <h1 className="text-2xl font-bold">Modules et Fonctionnalites</h1>
        <button
          onClick={() => fetchModules() }
          className="px-4 py-2 bg-indigo-600 text-white rounded-lg hover:bg-indigo-700"
        >
          Actualiser
        </button>
      </div>

      {message && (
        <div className={"p-4 rounded-lg mb-6 " + (message.type === "success" ? "bg-green-100 text-green-700" : "bg-red-100 text-red-700")}>
          {message.text}
        </div>
      )}

      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
        {modules.map((mod) => (
          <div
            key={mod.key}
            className={"p-4 rounded-lg border " +
              (mod.enabled ? "border-indigo-200 bg-indigo-50" : "border-gray-200 bg-gray-50")
            }
          >
            <div className="flex items-center justify-between">
              <div className="flex-1">
                <h3 className="font-medium">{mod.label}</h3>
                <p className="text-sm text-gray-500 mt-1">
                  {mod.enabled ? "Active" : "Desactive"}
                </p>
              </div>
              <button
                onClick={() => toggleModule(mod.key, !mod.enabled)}
                disabled={saving}
                className={"relative w-11 h-6 rounded-full " +
                  (mod.enabled ? "bg-indigo-600" : "bg-gray-300") +
                  " transition-colors"
                }
              >
                <span className={"absolute top-1 left-1 w-4 h-4 bg-white rounded-full shadow " +
                  (mod.enabled ? "translate-x-5" : "")
                } />
              </button>
            </div>
          </div>
        ))}
      </div>

      {saving && (
        <div className="fixed bottom-4 right-4 bg-gray-800 text-white px-4 py-2 rounded-lg">
          Enregistrement...
        </div>
      )}
    </div>
  );
}
