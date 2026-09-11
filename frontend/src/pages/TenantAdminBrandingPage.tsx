import { useEffect, useState } from "react";
import { useAuth } from "@/contexts/AuthContext";
import api from "@/lib/api";

export default function TenantAdminBrandingPage() {
  const { hasRole } = useAuth();
  const [branding, setBranding] = useState<any>(null);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [message, setMessage] = useState<{type: string, text: string} | null>(null);

  useEffect(() => {
    if (!hasRole("TENANT_OWNER") && !hasRole("TENANT_ADMIN")) {
      return;
    }
    fetchBranding();
  }, [hasRole]);

  const fetchBranding = async () => {
    try {
      const res = await api.get("/admin/branding");
      setBranding(res.data);
    } catch (error) {
      console.error("Error fetching branding:", error);
    } finally {
      setLoading(false);
    }
  };

  const handleSave = async (e: React.FormEvent) => {
    e.preventDefault();
    setSaving(true);
    try {
      await api.put("/admin/branding", branding);
      setMessage({ type: "success", text: "Branding mis a jour avec succes" });
    } catch (error) {
      setMessage({ type: "error", text: "Erreur lors de la mise a jour" });
    } finally {
      setSaving(false);
    }
  };

  if (!hasRole("TENANT_OWNER") && !hasRole("TENANT_ADMIN")) {
    return <div className="p-8 text-center">Acces non autorise</div>;
  }

  if (loading) {
    return <div className="p-8 text-center">Chargement...</div>;
  }

  return (
    <div className="p-6 max-w-2xl mx-auto">
      <h1 className="text-2xl font-bold mb-6">Personnalisation (Branding)</h1>

      {message && (
        <div className={"p-4 rounded-lg mb-6 " + (message.type === "success" ? "bg-emerald-100 text-emerald-700" : "bg-red-100 text-red-700")}>
          {message.text}
        </div>
      )}

      <form onSubmit={handleSave} className="space-y-6">
        <div className="bg-white rounded-lg border p-6">
          <h2 className="text-lg font-semibold mb-4">Couleurs</h2>
          <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
            <div>
              <label className="block text-sm font-medium mb-1">Couleur principale</label>
              <input
                type="color"
                value={branding?.primaryColor || "#6366F1"}
                onChange={(e) => setBranding({ ...branding, primaryColor: e.target.value })}
                className="w-full h-10 border rounded-lg"
              />
            </div>
            <div>
              <label className="block text-sm font-medium mb-1">Couleur secondaire</label>
              <input
                type="color"
                value={branding?.secondaryColor || "#8B5CF6"}
                onChange={(e) => setBranding({ ...branding, secondaryColor: e.target.value })}
                className="w-full h-10 border rounded-lg"
              />
            </div>
            <div>
              <label className="block text-sm font-medium mb-1">Couleur d'accent</label>
              <input
                type="color"
                value={branding?.accentColor || "#EC4899"}
                onChange={(e) => setBranding({ ...branding, accentColor: e.target.value })}
                className="w-full h-10 border rounded-lg"
              />
            </div>
          </div>
        </div>

        <div className="bg-white rounded-lg border p-6">
          <h2 className="text-lg font-semibold mb-4">Logo & Identite</h2>
          <div className="space-y-4">
            <div>
              <label className="block text-sm font-medium mb-1">Nom de l'eglise / organisation</label>
              <input
                type="text"
                value={branding?.churchName || ""}
                onChange={(e) => setBranding({ ...branding, churchName: e.target.value })}
                className="w-full px-4 py-2 border rounded-lg"
              />
            </div>
            <div>
              <label className="block text-sm font-medium mb-1">Slogan</label>
              <input
                type="text"
                value={branding?.tagline || ""}
                onChange={(e) => setBranding({ ...branding, tagline: e.target.value })}
                className="w-full px-4 py-2 border rounded-lg"
              />
            </div>
            <div>
              <label className="block text-sm font-medium mb-1">URL du logo</label>
              <input
                type="url"
                value={branding?.logoUrl || ""}
                onChange={(e) => setBranding({ ...branding, logoUrl: e.target.value })}
                className="w-full px-4 py-2 border rounded-lg"
              />
            </div>
            <div>
              <label className="block text-sm font-medium mb-1">URL du favicon</label>
              <input
                type="url"
                value={branding?.faviconUrl || ""}
                onChange={(e) => setBranding({ ...branding, faviconUrl: e.target.value })}
                className="w-full px-4 py-2 border rounded-lg"
              />
            </div>
          </div>
        </div>

        <div className="bg-white rounded-lg border p-6">
          <h2 className="text-lg font-semibold mb-4">Coordonnees</h2>
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div>
              <label className="block text-sm font-medium mb-1">Adresse</label>
              <input
                type="text"
                value={branding?.address || ""}
                onChange={(e) => setBranding({ ...branding, address: e.target.value })}
                className="w-full px-4 py-2 border rounded-lg"
              />
            </div>
            <div>
              <label className="block text-sm font-medium mb-1">Telephone</label>
              <input
                type="tel"
                value={branding?.phone || ""}
                onChange={(e) => setBranding({ ...branding, phone: e.target.value })}
                className="w-full px-4 py-2 border rounded-lg"
              />
            </div>
            <div>
              <label className="block text-sm font-medium mb-1">Email</label>
              <input
                type="email"
                value={branding?.email || ""}
                onChange={(e) => setBranding({ ...branding, email: e.target.value })}
                className="w-full px-4 py-2 border rounded-lg"
              />
            </div>
            <div>
              <label className="block text-sm font-medium mb-1">Site web</label>
              <input
                type="url"
                value={branding?.website || ""}
                onChange={(e) => setBranding({ ...branding, website: e.target.value })}
                className="w-full px-4 py-2 border rounded-lg"
              />
            </div>
          </div>
        </div>

        <div className="flex gap-4">
          <button
            type="submit"
            disabled={saving}
            className="px-6 py-3 bg-indigo-600 text-white rounded-lg hover:bg-indigo-700 disabled:opacity-50"
          >
            {saving ? "Enregistrement..." : "Enregistrer le branding"}
          </button>
        </div>
      </form>
    </div>
  );
}
