import { useEffect, useState } from "react";
import { useTenant } from "@/contexts/TenantContext";
import api from "@/lib/api";

interface DressCodeRule {
  id?: string;
  groupName: string;
  description: string;
  imageUrl: string;
}

interface DressCode {
  id: string;
  spaceId?: string;
  eventId?: string;
  serviceName: string;
  title: string;
  beginsAt: string;
  endsAt: string;
  status: string;
  archived: boolean;
  rules: DressCodeRule[];
}

export default function DressCodePage() {
  const { hasPermission } = useTenant();
  const [dressCodes, setDressCodes] = useState<DressCode[]>([]);
  const [loading, setLoading] = useState(true);
  const [showForm, setShowForm] = useState(false);
  const [editing, setEditing] = useState<DressCode | null>(null);
  const [form, setForm] = useState<Partial<DressCode>>({
    title: "",
    serviceName: "",
    status: "DRAFT",
    rules: [],
  });
  const [message, setMessage] = useState<{ type: string; text: string } | null>(null);

  useEffect(() => {
    fetchDressCodes();
  }, []);

  const fetchDressCodes = async () => {
    try {
      const res = await api.get("/dress-codes");
      setDressCodes(res.data);
    } catch (error) {
      console.error("Erreur:", error);
    } finally {
      setLoading(false);
    }
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      if (editing?.id) {
        await api.put(`/dress-codes/${editing.id}`, form);
        setMessage({ type: "success", text: "Dress code mis à jour" });
      } else {
        await api.post("/dress-codes", form);
        setMessage({ type: "success", text: "Dress code créé" });
      }
      setShowForm(false);
      setEditing(null);
      setForm({ title: "", serviceName: "", status: "DRAFT", rules: [] });
      fetchDressCodes();
    } catch {
      setMessage({ type: "error", text: "Erreur lors de l'enregistrement" });
    }
  };

  const handleEdit = (dc: DressCode) => {
    setEditing(dc);
    setForm(dc);
    setShowForm(true);
  };

  const handleArchive = async (id: string) => {
    try {
      await api.post(`/dress-codes/${id}/archive`);
      setMessage({ type: "success", text: "Dress code archivé" });
      fetchDressCodes();
    } catch {
      setMessage({ type: "error", text: "Erreur lors de l'archivage" });
    }
  };

  const addRule = () => {
    setForm({ ...form, rules: [...(form.rules || []), { groupName: "", description: "", imageUrl: "" }] });
  };

  const updateRule = (index: number, field: keyof DressCodeRule, value: string) => {
    const rules = [...(form.rules || [])];
    rules[index] = { ...rules[index], [field]: value };
    setForm({ ...form, rules });
  };

  const removeRule = (index: number) => {
    const rules = [...(form.rules || [])];
    rules.splice(index, 1);
    setForm({ ...form, rules });
  };

  if (loading) return <div className="p-8 text-center">Chargement...</div>;

  return (
    <div className="p-6">
      <div className="flex justify-between items-center mb-6">
        <h1 className="text-2xl font-bold">Dress Codes</h1>
        {hasPermission("DRESS_CODE_CREATE") && (
          <button
            onClick={() => { setShowForm(true); setEditing(null); setForm({ title: "", serviceName: "", status: "DRAFT", rules: [] }); }}
            className="px-4 py-2 bg-indigo-600 text-white rounded-lg hover:bg-indigo-700"
          >
            Nouveau Dress Code
          </button>
        )}
      </div>
      {message && (
        <div className={`p-4 rounded-lg mb-6 ${message.type === "success" ? "bg-emerald-100 text-emerald-700" : "bg-red-100 text-red-700"}`}>
          {message.text}
        </div>
      )}
      {showForm && (
        <div className="bg-white rounded-lg border p-6 mb-6">
          <h2 className="text-lg font-semibold mb-4">{editing ? "Modifier" : "Créer"} un dress code</h2>
          <form onSubmit={handleSubmit} className="space-y-4">
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              <div>
                <label className="block text-sm font-medium mb-1">Titre</label>
                <input type="text" value={form.title || ""} onChange={(e) => setForm({ ...form, title: e.target.value })} className="w-full px-4 py-2 border rounded-lg" required />
              </div>
              <div>
                <label className="block text-sm font-medium mb-1">Service</label>
                <input type="text" value={form.serviceName || ""} onChange={(e) => setForm({ ...form, serviceName: e.target.value })} className="w-full px-4 py-2 border rounded-lg" />
              </div>
              <div>
                <label className="block text-sm font-medium mb-1">Début</label>
                <input type="datetime-local" value={form.beginsAt || ""} onChange={(e) => setForm({ ...form, beginsAt: e.target.value })} className="w-full px-4 py-2 border rounded-lg" />
              </div>
              <div>
                <label className="block text-sm font-medium mb-1">Fin</label>
                <input type="datetime-local" value={form.endsAt || ""} onChange={(e) => setForm({ ...form, endsAt: e.target.value })} className="w-full px-4 py-2 border rounded-lg" />
              </div>
              <div>
                <label className="block text-sm font-medium mb-1">Statut</label>
                <select value={form.status || "DRAFT"} onChange={(e) => setForm({ ...form, status: e.target.value })} className="w-full px-4 py-2 border rounded-lg">
                  <option value="DRAFT">Brouillon</option>
                  <option value="PUBLISHED">Publié</option>
                  <option value="ARCHIVED">Archivé</option>
                </select>
              </div>
            </div>
            <div>
              <div className="flex justify-between items-center mb-2">
                <label className="text-sm font-medium">Règles par groupe</label>
                <button type="button" onClick={addRule} className="text-sm text-indigo-600 hover:text-indigo-700">+ Ajouter</button>
              </div>
              {(form.rules || []).map((rule, index) => (
                <div key={index} className="flex gap-2 mb-2">
                  <input type="text" placeholder="Groupe" value={rule.groupName} onChange={(e) => updateRule(index, "groupName", e.target.value)} className="flex-1 px-3 py-2 border rounded-lg text-sm" />
                  <input type="text" placeholder="Description" value={rule.description} onChange={(e) => updateRule(index, "description", e.target.value)} className="flex-2 px-3 py-2 border rounded-lg text-sm" />
                  <button type="button" onClick={() => removeRule(index)} className="px-3 py-2 text-red-600 hover:bg-red-50 rounded-lg">✕</button>
                </div>
              ))}
            </div>
            <div className="flex gap-4">
              <button type="submit" className="px-6 py-2 bg-indigo-600 text-white rounded-lg hover:bg-indigo-700">{editing ? "Mettre à jour" : "Créer"}</button>
              <button type="button" onClick={() => { setShowForm(false); setEditing(null); }} className="px-6 py-2 border rounded-lg hover:bg-gray-50">Annuler</button>
            </div>
          </form>
        </div>
      )}
      <div className="space-y-4">
        {dressCodes.length === 0 ? (
          <p className="text-gray-500 text-center py-8">Aucun dress code</p>
        ) : (
          dressCodes.map((dc) => (
            <div key={dc.id} className="bg-white rounded-lg border p-4">
              <div className="flex justify-between items-start">
                <div>
                  <h3 className="font-semibold">{dc.title}</h3>
                  <p className="text-sm text-gray-500">{dc.serviceName} • {dc.status}</p>
                  {dc.rules && dc.rules.length > 0 && (
                    <div className="mt-2 flex gap-2 flex-wrap">
                      {dc.rules.map((rule, i) => (
                        <span key={i} className="px-2 py-1 text-xs rounded-full bg-indigo-100 text-indigo-700">{rule.groupName}</span>
                      ))}
                    </div>
                  )}
                </div>
                <div className="flex gap-2">
                  {hasPermission("DRESS_CODE_UPDATE") && (
                    <button onClick={() => handleEdit(dc)} className="px-3 py-1 text-sm border rounded-lg hover:bg-gray-50">Modifier</button>
                  )}
                  {hasPermission("DRESS_CODE_ARCHIVE") && (
                    <button onClick={() => handleArchive(dc.id)} className="px-3 py-1 text-sm text-orange-600 border border-orange-200 rounded-lg hover:bg-orange-50">Archiver</button>
                  )}
                </div>
              </div>
            </div>
          ))
        )}
      </div>
    </div>
  );
}
