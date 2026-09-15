import { useCallback, useEffect, useRef, useState } from "react";
import { useTenant } from "@/contexts/TenantContext";
import api from "@/lib/api";

const DEFAULT_BRANDING = {
  businessName: "", slogan: "",
  logoUrl: "", logoDarkUrl: "", coverUrl: "", faviconUrl: "",
  primaryColor: "#6366F1", secondaryColor: "#8B5CF6", accentColor: "#EC4899",
  surfaceColor: "#FFFFFF", backgroundColor: "#F8FAFC",
  textPrimaryColor: "#1E293B", textSecondaryColor: "#64748B",
  successColor: "#10B981", warningColor: "#F59E0B", errorColor: "#EF4444", infoColor: "#3B82F6",
  primaryFont: "Inter", secondaryFont: "Inter", headingFont: "Inter", monoFont: "JetBrains Mono",
  customCss: "", customHeadHtml: "",
};

type BrandingData = typeof DEFAULT_BRANDING & Record<string, unknown>;

function ColorInput({ label, value, onChange }: { label: string; value: string; onChange: (v: string) => void }) {
  return (
    <div className="flex items-center gap-3">
      <input type="color" value={value} onChange={(e) => onChange(e.target.value)}
        className="w-10 h-10 border-2 border-gray-200 rounded-lg cursor-pointer" />
      <div className="flex-1">
        <label className="text-xs font-medium text-gray-500">{label}</label>
        <input type="text" value={value} onChange={(e) => onChange(e.target.value)}
          className="w-full px-2 py-1 text-sm border rounded font-mono" />
      </div>
    </div>
  );
}

export default function TenantAdminBrandingPage() {
  const { tenantId, hasPermission } = useTenant();
  const [data, setData] = useState<BrandingData>(DEFAULT_BRANDING);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [msg, setMsg] = useState<{ type: "success" | "error"; text: string } | null>(null);
  const [tab, setTab] = useState<"colors" | "identity" | "fonts" | "contact" | "advanced">("colors");
  const [uploading, setUploading] = useState<string | null>(null);
  const previewRef = useRef<HTMLDivElement>(null);
  const wsRef = useRef<WebSocket | null>(null);

  useEffect(() => {
    api.get("/admin/settings").then((r) => {
      setData({ ...DEFAULT_BRANDING, ...r.data });
      setLoading(false);
    }).catch(() => setLoading(false));
  }, []);

  useEffect(() => {
    applyLivePreview();
  }, [data.primaryColor, data.secondaryColor, data.accentColor, data.surfaceColor, data.backgroundColor,
    data.textPrimaryColor, data.textSecondaryColor, data.successColor, data.warningColor, data.errorColor,
    data.infoColor, data.primaryFont, data.secondaryFont, data.headingFont, data.monoFont]);

  useEffect(() => {
    if (!tenantId) return;
    try {
      const proto = window.location.protocol === "https:" ? "wss:" : "ws:";
      const ws = new WebSocket(`${proto}//${window.location.host}/ws`);
      ws.onopen = () => ws.send(`CONNECT\naccept-version:1.1,1.0\n\n\0`);
      wsRef.current = ws;
      ws.onmessage = (evt) => {
        if (evt.data.includes("tenant-branding:changed") || evt.data.includes("tenant-settings:changed")) {
          api.get("/admin/settings").then((r) => setData({ ...DEFAULT_BRANDING, ...r.data }));
        }
      };
      return () => { ws.close(); wsRef.current = null; };
    } catch {}
  }, [tenantId]);

  const applyLivePreview = useCallback(() => {
    const root = document.documentElement;
    root.style.setProperty("--brand-primary", data.primaryColor);
    root.style.setProperty("--brand-secondary", data.secondaryColor);
    root.style.setProperty("--brand-accent", data.accentColor);
    root.style.setProperty("--brand-surface", data.surfaceColor);
    root.style.setProperty("--brand-background", data.backgroundColor);
    root.style.setProperty("--brand-text-primary", data.textPrimaryColor);
    root.style.setProperty("--brand-text-secondary", data.textSecondaryColor);
    root.style.setProperty("--brand-success", data.successColor);
    root.style.setProperty("--brand-warning", data.warningColor);
    root.style.setProperty("--brand-error", data.errorColor);
    root.style.setProperty("--brand-info", data.infoColor);
    root.style.setProperty("--brand-font-primary", `"${data.primaryFont}", sans-serif`);
    root.style.setProperty("--brand-font-heading", `"${data.headingFont}", sans-serif`);
  }, [data]);

  const update = (k: string, v: string) => setData((prev) => ({ ...prev, [k]: v }));

  const handleSave = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!hasPermission("BRANDING_UPDATE")) return;
    setSaving(true); setMsg(null);
    try {
      await api.put("/admin/settings", data);
      setMsg({ type: "success", text: "Branding mis à jour — tous les clients voient le changement en < 5s" });
    } catch { setMsg({ type: "error", text: "Erreur lors de la sauvegarde" }); }
    finally { setSaving(false); }
  };

  const handleUpload = async (file: File, assetType: string) => {
    setUploading(assetType);
    const form = new FormData();
    form.append("file", file);
    form.append("assetType", assetType);
    try {
      const r = await api.post("/admin/branding/assets", form, { headers: { "Content-Type": "multipart/form-data" } });
      update(assetType === "logo" ? "logoUrl" : assetType === "logo-dark" ? "logoDarkUrl" : assetType, r.data.url);
    } catch { setMsg({ type: "error", text: `Erreur upload ${assetType}` }); }
    finally { setUploading(null); }
  };

  if (loading) return <div className="p-8 text-center text-gray-500">Chargement des paramètres...</div>;

  const tabs = [
    { key: "colors", label: "Couleurs" }, { key: "identity", label: "Identité" },
    { key: "fonts", label: "Polices" }, { key: "contact", label: "Contact" },
    { key: "advanced", label: "Avancé" },
  ] as const;

  return (
    <div className="min-h-screen bg-gray-50">
      <div className="max-w-7xl mx-auto p-6">
        <div className="flex items-center justify-between mb-6">
          <div>
            <h1 className="text-2xl font-bold text-gray-900">Branding Dynamique</h1>
            <p className="text-sm text-gray-500 mt-1">Configuration visuelle de votre église — propagée en temps réel à tous les clients</p>
          </div>
          <div className="flex items-center gap-2">
            <span className="inline-flex items-center px-3 py-1 rounded-full text-xs font-medium bg-blue-100 text-blue-800">
              WebSocket temps réel
            </span>
            <button onClick={handleSave} disabled={saving || !hasPermission("BRANDING_UPDATE")}
              className="px-6 py-2.5 bg-indigo-600 text-white rounded-lg hover:bg-indigo-700 disabled:opacity-50 font-medium transition-colors">
              {saving ? "Enregistrement..." : "Enregistrer"}
            </button>
          </div>
        </div>

        {msg && (
          <div className={`p-4 rounded-lg mb-6 ${msg.type === "success" ? "bg-emerald-50 text-emerald-700 border border-emerald-200" : "bg-red-50 text-red-700 border border-red-200"}`}>
            {msg.text}
          </div>
        )}

        <div className="grid grid-cols-12 gap-6">
          <div className="col-span-8">
            <div className="bg-white rounded-xl border shadow-sm">
              <div className="flex border-b">
                {tabs.map((t) => (
                  <button key={t.key} onClick={() => setTab(t.key)}
                    className={`px-5 py-3 text-sm font-medium border-b-2 transition-colors ${tab === t.key ? "border-indigo-600 text-indigo-600" : "border-transparent text-gray-500 hover:text-gray-700"}`}>
                    {t.label}
                  </button>
                ))}
              </div>
              <div className="p-6">
                <form onSubmit={handleSave} className="space-y-6">
                  {tab === "colors" && (
                    <div className="space-y-6">
                      <div>
                        <h3 className="text-sm font-semibold text-gray-900 mb-3">Palette Principale</h3>
                        <div className="grid grid-cols-2 gap-4">
                          <ColorInput label="Primaire" value={data.primaryColor} onChange={(v) => update("primaryColor", v)} />
                          <ColorInput label="Secondaire" value={data.secondaryColor} onChange={(v) => update("secondaryColor", v)} />
                          <ColorInput label="Accent" value={data.accentColor} onChange={(v) => update("accentColor", v)} />
                          <ColorInput label="Surface" value={data.surfaceColor} onChange={(v) => update("surfaceColor", v)} />
                        </div>
                      </div>
                      <div>
                        <h3 className="text-sm font-semibold text-gray-900 mb-3">Arrière-plan & Texte</h3>
                        <div className="grid grid-cols-2 gap-4">
                          <ColorInput label="Background" value={data.backgroundColor} onChange={(v) => update("backgroundColor", v)} />
                          <ColorInput label="Texte principal" value={data.textPrimaryColor} onChange={(v) => update("textPrimaryColor", v)} />
                          <ColorInput label="Texte secondaire" value={data.textSecondaryColor} onChange={(v) => update("textSecondaryColor", v)} />
                        </div>
                      </div>
                      <div>
                        <h3 className="text-sm font-semibold text-gray-900 mb-3">Statuts</h3>
                        <div className="grid grid-cols-2 gap-4">
                          <ColorInput label="Succès" value={data.successColor} onChange={(v) => update("successColor", v)} />
                          <ColorInput label="Avertissement" value={data.warningColor} onChange={(v) => update("warningColor", v)} />
                          <ColorInput label="Erreur" value={data.errorColor} onChange={(v) => update("errorColor", v)} />
                          <ColorInput label="Info" value={data.infoColor} onChange={(v) => update("infoColor", v)} />
                        </div>
                      </div>
                    </div>
                  )}
                  {tab === "identity" && (
                    <div className="space-y-6">
                      <div className="grid grid-cols-2 gap-4">
                        <div>
                          <label className="block text-sm font-medium text-gray-700 mb-1">Nom commercial</label>
                          <input type="text" value={data.businessName} onChange={(e) => update("businessName", e.target.value)}
                            className="w-full px-3 py-2 border rounded-lg" placeholder="Nom affiché partout" />
                        </div>
                        <div>
                          <label className="block text-sm font-medium text-gray-700 mb-1">Slogan</label>
                          <input type="text" value={data.slogan} onChange={(e) => update("slogan", e.target.value)}
                            className="w-full px-3 py-2 border rounded-lg" placeholder="Accroche" />
                        </div>
                      </div>
                      {(["logo", "logoDark", "cover", "favicon"] as const).map((type) => (
                        <div key={type}>
                          <label className="block text-sm font-medium text-gray-700 mb-1">
                            {type === "logo" ? "Logo principal" : type === "logoDark" ? "Logo mode sombre" : type === "cover" ? "Couverture" : "Favicon"}
                          </label>
                          <div className="flex gap-3">
                            <input type="url" value={data[type] || ""} onChange={(e) => update(type, e.target.value)}
                              className="flex-1 px-3 py-2 border rounded-lg" placeholder="https://..." />
                            <label className="px-4 py-2 bg-gray-100 text-gray-700 rounded-lg cursor-pointer hover:bg-gray-200 text-sm font-medium">
                              {uploading === type ? "..." : "Upload"}
                              <input type="file" accept="image/*" className="hidden"
                                onChange={(e) => e.target.files?.[0] && handleUpload(e.target.files[0], type === "logoDark" ? "logo-dark" : type)} />
                            </label>
                          </div>
                          {(data[type] as string) && (
                            <img src={data[type] as string} alt={type} className="mt-2 h-16 object-contain rounded border" />
                          )}
                        </div>
                      ))}
                      <div>
                        <label className="block text-sm font-medium text-gray-700 mb-1">CSS personnalisé</label>
                        <textarea value={data.customCss} onChange={(e) => update("customCss", e.target.value)}
                          className="w-full px-3 py-2 border rounded-lg font-mono text-xs" rows={4}
                          placeholder=":root { --brand-radius: 12px; }" />
                      </div>
                    </div>
                  )}
                  {tab === "fonts" && (
                    <div className="space-y-4">
                      {[
                        { k: "primaryFont", l: "Police principale" }, { k: "secondaryFont", l: "Police secondaire" },
                        { k: "headingFont", l: "Police titres" }, { k: "monoFont", l: "Police monospace" },
                      ].map(({ k, l }) => (
                        <div key={k}>
                          <label className="block text-sm font-medium text-gray-700 mb-1">{l}</label>
                          <input type="text" value={data[k]} onChange={(e) => update(k, e.target.value)}
                            className="w-full px-3 py-2 border rounded-lg" />
                        </div>
                      ))}
                    </div>
                  )}
                  {tab === "contact" && (
                    <div className="grid grid-cols-2 gap-4">
                      <div><label className="block text-sm font-medium text-gray-700 mb-1">Email</label>
                        <input type="email" value={data.email || ""} onChange={(e) => update("email", e.target.value)} className="w-full px-3 py-2 border rounded-lg" /></div>
                      <div><label className="block text-sm font-medium text-gray-700 mb-1">Téléphone</label>
                        <input type="tel" value={data.phone || ""} onChange={(e) => update("phone", e.target.value)} className="w-full px-3 py-2 border rounded-lg" /></div>
                      <div><label className="block text-sm font-medium text-gray-700 mb-1">Site web</label>
                        <input type="url" value={data.website || ""} onChange={(e) => update("website", e.target.value)} className="w-full px-3 py-2 border rounded-lg" /></div>
                      <div><label className="block text-sm font-medium text-gray-700 mb-1">Devise</label>
                        <select value={data.currency || "XAF"} onChange={(e) => update("currency", e.target.value)} className="w-full px-3 py-2 border rounded-lg">
                          <option value="XAF">XAF (FCFA)</option><option value="EUR">EUR</option><option value="USD">USD</option>
                        </select></div>
                      <div className="col-span-2"><label className="block text-sm font-medium text-gray-700 mb-1">Adresse</label>
                        <textarea value={data.address || ""} onChange={(e) => update("address", e.target.value)} className="w-full px-3 py-2 border rounded-lg" rows={2} /></div>
                    </div>
                  )}
                  {tab === "advanced" && (
                    <div className="space-y-4">
                      <div><label className="block text-sm font-medium text-gray-700 mb-1">HTML dans {'<head>'}</label>
                        <textarea value={data.customHeadHtml || ""} onChange={(e) => update("customHeadHtml", e.target.value)}
                          className="w-full px-3 py-2 border rounded-lg font-mono text-xs" rows={4} /></div>
                      <div><label className="block text-sm font-medium text-gray-700 mb-1">Pied de page</label>
                        <input type="text" value={(data as Record<string, string>).footerText || ""} onChange={(e) => update("footerText" as string, e.target.value)}
                          className="w-full px-3 py-2 border rounded-lg" placeholder="© {{year}} {{tenant_name}}" /></div>
                    </div>
                  )}
                </form>
              </div>
            </div>
          </div>

          <div className="col-span-4">
            <div className="bg-white rounded-xl border shadow-sm p-4 sticky top-6">
              <h3 className="text-sm font-semibold text-gray-900 mb-3">Aperçu en direct</h3>
              <div ref={previewRef} className="rounded-lg border overflow-hidden" style={{ background: data.backgroundColor }}>
                <div className="p-4" style={{ background: data.primaryColor, color: "#FFFFFF" }}>
                  <div className="flex items-center gap-3">
                    {data.logoUrl ? <img src={data.logoUrl} alt="Logo" className="h-10" /> : (
                      <div className="w-10 h-10 rounded-lg flex items-center justify-center font-bold text-lg"
                        style={{ background: data.accentColor, color: "#FFFFFF" }}>
                        {(data.businessName || "E")[0]}
                      </div>
                    )}
                    <div>
                      <div className="font-bold text-sm" style={{ fontFamily: `"${data.headingFont}", sans-serif` }}>
                        {data.businessName || "Votre Église"}
                      </div>
                      <div className="text-xs opacity-80">{data.slogan || "Slogan ici"}</div>
                    </div>
                  </div>
                </div>
                <div className="p-4 space-y-3">
                  <div className="flex gap-2">
                    <button className="px-3 py-1.5 rounded text-xs font-medium text-white" style={{ background: data.primaryColor }}>Action</button>
                    <button className="px-3 py-1.5 rounded text-xs font-medium text-white" style={{ background: data.accentColor }}>Secondaire</button>
                    <button className="px-3 py-1.5 rounded text-xs font-medium border" style={{ borderColor: data.primaryColor, color: data.primaryColor }}>Outline</button>
                  </div>
                  <div className="text-xs" style={{ color: data.textSecondaryColor }}>
                    Texte secondaire — Police: {data.primaryFont}
                  </div>
                  <div className="text-xs font-medium" style={{ color: data.textPrimaryColor }}>
                    Texte principal — Titres: {data.headingFont}
                  </div>
                  <div className="flex gap-2">
                    <span className="px-2 py-0.5 rounded-full text-xs text-white" style={{ background: data.successColor }}>Succès</span>
                    <span className="px-2 py-0.5 rounded-full text-xs text-white" style={{ background: data.warningColor }}>Alerte</span>
                    <span className="px-2 py-0.5 rounded-full text-xs text-white" style={{ background: data.errorColor }}>Erreur</span>
                    <span className="px-2 py-0.5 rounded-full text-xs text-white" style={{ background: data.infoColor }}>Info</span>
                  </div>
                </div>
                <div className="px-4 py-2 text-xs border-t" style={{ color: data.textSecondaryColor, borderColor: `${data.primaryColor}20` }}>
                  © 2026 {data.businessName || "Votre Église"}
                </div>
              </div>
              <div className="mt-4 p-3 bg-gray-50 rounded-lg">
                <h4 className="text-xs font-semibold text-gray-700 mb-2">CSS Variables générées</h4>
                <pre className="text-xs text-gray-600 font-mono whitespace-pre-wrap max-h-40 overflow-auto">
{`--brand-primary: ${data.primaryColor};
--brand-secondary: ${data.secondaryColor};
--brand-accent: ${data.accentColor};
--brand-font: "${data.primaryFont}";
--brand-heading: "${data.headingFont}";`}
                </pre>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}