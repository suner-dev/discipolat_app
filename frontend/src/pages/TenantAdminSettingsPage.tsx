import { useEffect, useState } from "react";
import { useTenant } from "@/contexts/TenantContext";
import api from "@/lib/api";

export default function TenantAdminSettingsPage() {
  const { hasPermission } = useTenant();
  const [settings, setSettings] = useState<any>(null);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [message, setMessage] = useState<{type: string, text: string} | null>(null);

  useEffect(() => {
    if (!hasPermission("TENANT_SETTINGS_READ")) return;
    fetchSettings();
  }, [hasPermission]);

  const fetchSettings = async () => {
    try {
      const res = await api.get("/admin/settings");
      setSettings(res.data);
    } catch (error) {
      console.error("Erreur:", error);
    } finally {
      setLoading(false);
    }
  };

  const handleSave = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!hasPermission("TENANT_SETTINGS_UPDATE")) return;
    setSaving(true);
    try {
      await api.put("/admin/settings", settings);
      setMessage({ type: "success", text: "Paramètres mis à jour avec succès" });
    } catch (error: any) {
      setMessage({ type: "error", text: error.response?.data?.error || "Erreur lors de la mise à jour" });
    } finally {
      setSaving(false);
    }
  };

  if (loading) return <div className="p-8 text-center">Chargement...</div>;

  return (
    <div className="p-6 max-w-2xl mx-auto">
      <h1 className="text-2xl font-bold mb-6">Paramètres</h1>

      {message && (
        <div className={"p-4 rounded-lg mb-6 " + (message.type === "success" ? "bg-emerald-100 text-emerald-700" : "bg-red-100 text-red-700")}>
          {message.text}
        </div>
      )}

      <form onSubmit={handleSave} className="space-y-6">
        <div className="bg-white rounded-lg border p-6">
          <h2 className="text-lg font-semibold mb-4">Région & Langue</h2>
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div>
              <label className="block text-sm font-medium mb-1">Langue</label>
              <select
                value={settings?.language || "fr"}
                onChange={(e) => setSettings({ ...settings, language: e.target.value })}
                className="w-full px-4 py-2 border rounded-lg"
              >
                <option value="fr">Français</option>
                <option value="en">Anglais</option>
                <option value="pt">Portugais</option>
                <option value="es">Espagnol</option>
                <option value="ar">Arabe</option>
              </select>
            </div>
            <div>
              <label className="block text-sm font-medium mb-1">Pays</label>
              <select
                value={settings?.country || "CM"}
                onChange={(e) => setSettings({ ...settings, country: e.target.value })}
                className="w-full px-4 py-2 border rounded-lg"
              >
                <option value="CM">Cameroun</option>
                <option value="FR">France</option>
                <option value="BE">Belgique</option>
                <option value="CH">Suisse</option>
                <option value="CD">RDC</option>
              </select>
            </div>
            <div>
              <label className="block text-sm font-medium mb-1">Devise</label>
              <select
                value={settings?.currency || "XAF"}
                onChange={(e) => setSettings({ ...settings, currency: e.target.value })}
                className="w-full px-4 py-2 border rounded-lg"
              >
                <option value="XAF">FCFA</option>
                <option value="EUR">Euro</option>
                <option value="USD">Dollar</option>
              </select>
            </div>
            <div>
              <label className="block text-sm font-medium mb-1">Fuseau horaire</label>
              <select
                value={settings?.timezone || "Africa/Douala"}
                onChange={(e) => setSettings({ ...settings, timezone: e.target.value })}
                className="w-full px-4 py-2 border rounded-lg"
              >
                <option value="Africa/Douala">Africa/Douala</option>
                <option value="Africa/Lagos">Africa/Lagos</option>
                <option value="Africa/Kinshasa">Africa/Kinshasa</option>
                <option value="Europe/Paris">Europe/Paris</option>
                <option value="UTC">UTC</option>
              </select>
            </div>
            <div>
              <label className="block text-sm font-medium mb-1">Format de date</label>
              <input
                type="text"
                value={settings?.dateFormat || "dd/MM/yyyy"}
                onChange={(e) => setSettings({ ...settings, dateFormat: e.target.value })}
                className="w-full px-4 py-2 border rounded-lg"
                placeholder="dd/MM/yyyy"
              />
            </div>
            <div>
              <label className="block text-sm font-medium mb-1">Indicatif téléphone</label>
              <input
                type="text"
                value={settings?.phoneCountryCode || "+237"}
                onChange={(e) => setSettings({ ...settings, phoneCountryCode: e.target.value })}
                className="w-full px-4 py-2 border rounded-lg"
                placeholder="+237"
              />
            </div>
          </div>
        </div>

        <div className="bg-white rounded-lg border p-6">
          <h2 className="text-lg font-semibold mb-4">Coordonnées de l'organisation</h2>
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div>
              <label className="block text-sm font-medium mb-1">Email</label>
              <input
                type="email"
                value={settings?.email || ""}
                onChange={(e) => setSettings({ ...settings, email: e.target.value })}
                className="w-full px-4 py-2 border rounded-lg"
              />
            </div>
            <div>
              <label className="block text-sm font-medium mb-1">Téléphone</label>
              <input
                type="tel"
                value={settings?.phone || ""}
                onChange={(e) => setSettings({ ...settings, phone: e.target.value })}
                className="w-full px-4 py-2 border rounded-lg"
              />
            </div>
            <div>
              <label className="block text-sm font-medium mb-1">Site web</label>
              <input
                type="url"
                value={settings?.website || ""}
                onChange={(e) => setSettings({ ...settings, website: e.target.value })}
                className="w-full px-4 py-2 border rounded-lg"
              />
            </div>
          </div>
        </div>

        <div className="flex gap-4">
          <button
            type="submit"
            disabled={saving || !hasPermission("TENANT_SETTINGS_UPDATE")}
            className="px-6 py-3 bg-indigo-600 text-white rounded-lg hover:bg-indigo-700 disabled:opacity-50"
          >
            {saving ? "Enregistrement..." : "Enregistrer les paramètres"}
          </button>
        </div>
      </form>
    </div>
  );
}
