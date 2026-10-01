import { useMemo, useState } from "react";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import toast from "react-hot-toast";
import { useI18n } from "@/i18n";
import api, { getErrorMessage } from "@/lib/api";
import { Plus, Pencil, Archive, Loader2, X, Save, LayoutGrid } from "lucide-react";

/**
 * G5.5 (§58) — Gestion des espaces du tenant : création/configuration/archivage
 * RÉELS via /api/v1/spaces (G2.6). La création exige une unité
 * d'organisation (serveur : organizationUnitId obligatoire + droit de
 * configurer l'unité) — le sélecteur est alimenté par /org/tree.
 */

interface Space {
  id: string;
  organizationUnitId: string;
  spaceType: "DEPARTMENT" | "FAMILY" | "SUB_TEAM";
  templateCode?: string | null;
  name: string;
  code?: string | null;
  icon?: string | null;
  color?: string | null;
  description?: string | null;
  status: string;
  visiblePeopleScope: "CAMPUS" | "CHURCH";
  configuration?: Record<string, unknown>;
}

interface OrgNode { id: string; name: string; children?: OrgNode[] }

interface SpaceForm {
  name: string;
  code: string;
  spaceType: Space["spaceType"];
  templateCode: string;
  organizationUnitId: string;
  visiblePeopleScope: Space["visiblePeopleScope"];
  color: string;
  description: string;
}

const EMPTY_FORM: SpaceForm = {
  name: "", code: "", spaceType: "DEPARTMENT", templateCode: "",
  organizationUnitId: "", visiblePeopleScope: "CHURCH", color: "#6366F1", description: "",
};

export default function TenantAdminSpacesPage() {
  const { t } = useI18n();
  const queryClient = useQueryClient();
  const [form, setForm] = useState<SpaceForm | null>(null);
  const [editTarget, setEditTarget] = useState<Space | null>(null);

  const { data: spaces = [], isLoading } = useQuery({
    queryKey: ["admin", "spaces"],
    queryFn: async () => (await api.get("/spaces")).data as Space[],
  });

  const { data: orgTree = [] } = useQuery({
    queryKey: ["admin", "org", "tree"],
    queryFn: async () => (await api.get("/org/tree")).data,
  });

  const { data: templates = [] } = useQuery({
    queryKey: ["space-templates", "list"],
    queryFn: async () => {
      const res = await api.get("/space-templates/list").catch(() => ({ data: [] }));
      return (res.data || []) as { code: string; name?: string }[];
    },
  });

  const unitOptions = useMemo(() => {
    const flat: { id: string; label: string }[] = [];
    const walk = (nodes: OrgNode[], depth: number) => {
      for (const n of nodes || []) {
        flat.push({ id: n.id, label: `${"— ".repeat(depth)}${n.name}` });
        if (n.children?.length) walk(n.children, depth + 1);
      }
    };
    walk(Array.isArray(orgTree) ? orgTree : orgTree.content ?? [], 0);
    return flat;
  }, [orgTree]);

  const save = useMutation({
    mutationFn: async () => {
      if (editTarget) {
        await api.put(`/spaces/${editTarget.id}`, {
          name: form!.name,
          description: form!.description,
          color: form!.color,
          templateCode: form!.templateCode || null,
          visiblePeopleScope: form!.visiblePeopleScope,
        });
      } else {
        await api.post("/spaces", {
          organizationUnitId: form!.organizationUnitId,
          spaceType: form!.spaceType,
          templateCode: form!.templateCode || null,
          name: form!.name,
          code: form!.code || null,
          color: form!.color,
          description: form!.description,
          visiblePeopleScope: form!.visiblePeopleScope,
        });
      }
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["admin", "spaces"] });
      setForm(null);
      setEditTarget(null);
      toast.success(t("admin.spaceSaved"));
    },
    onError: (err: unknown) => toast.error(getErrorMessage(err)),
  });

  const archive = useMutation({
    mutationFn: async (id: string) => api.delete(`/spaces/${id}`),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["admin", "spaces"] });
      toast.success(t("admin.spaceArchived"));
    },
    onError: (err: unknown) => toast.error(getErrorMessage(err)),
  });

  const openCreate = () => { setEditTarget(null); setForm(EMPTY_FORM); };
  const openEdit = (s: Space) => {
    setEditTarget(s);
    setForm({
      ...EMPTY_FORM,
      name: s.name, code: s.code || "", spaceType: s.spaceType,
      templateCode: s.templateCode || "", organizationUnitId: s.organizationUnitId,
      visiblePeopleScope: s.visiblePeopleScope, color: s.color || EMPTY_FORM.color,
      description: s.description || "",
    });
  };

  if (isLoading) {
    return <div className="p-8 text-center"><Loader2 className="w-6 h-6 animate-spin inline" /></div>;
  }

  return (
    <div className="p-6">
      <div className="flex justify-between items-center mb-6">
        <h1 className="text-2xl font-bold">{t("admin.spacesTitle")}</h1>
        <button
          className="flex items-center gap-2 px-4 py-2 bg-indigo-600 text-white rounded-lg hover:bg-indigo-700"
          onClick={openCreate}
        >
          <Plus className="w-4 h-4" /> {t("admin.spaceNew")}
        </button>
      </div>

      {spaces.length === 0 ? (
        <div className="text-center py-16 bg-gray-50 rounded-lg border">
          <LayoutGrid className="w-10 h-10 text-gray-300 mx-auto mb-3" />
          <p className="text-gray-500">{t("admin.spacesEmpty")}</p>
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
          {spaces.map((s) => (
            <div key={s.id} className="bg-white rounded-lg border p-4 flex flex-col">
              <div className="flex items-start justify-between">
                <div className="flex items-center gap-2">
                  <span className="w-3 h-3 rounded-full" style={{ background: s.color || "#6366F1" }} />
                  <h3 className="font-semibold">{s.name}</h3>
                </div>
                <span className={"px-2 py-0.5 text-[10px] rounded-full " +
                  (s.status === "ACTIVE" ? "bg-emerald-100 text-emerald-700" : "bg-gray-100 text-gray-600")}>
                  {s.status}
                </span>
              </div>
              <div className="mt-2 text-xs text-gray-500 space-y-1">
                <p>{s.spaceType}{s.code ? ` · ${s.code}` : ""}</p>
                {s.templateCode && <p>Template: {s.templateCode}</p>}
                <p>{t("admin.spaceVisibilityLbl")}: {s.visiblePeopleScope}</p>
              </div>
              <div className="flex gap-3 mt-3 pt-3 border-t">
                <button
                  className="flex items-center gap-1 text-sm text-indigo-600 hover:text-indigo-900"
                  onClick={() => openEdit(s)}
                >
                  <Pencil className="w-3.5 h-3.5" /> {t("admin.edit")}
                </button>
                {s.status !== "ARCHIVED" && (
                  <button
                    className="ml-auto flex items-center gap-1 text-sm text-red-600 hover:text-red-800 disabled:opacity-40"
                    disabled={archive.isPending}
                    onClick={() => {
                      if (window.confirm(t("admin.spaceArchiveQ", { name: s.name }))) archive.mutate(s.id);
                    }}
                  >
                    <Archive className="w-3.5 h-3.5" /> {t("admin.spaceArchive")}
                  </button>
                )}
              </div>
            </div>
          ))}
        </div>
      )}

      {form && (
        <Modal onClose={() => setForm(null)}>
          <div className="flex justify-between items-center mb-4">
            <h3 className="text-lg font-bold">{editTarget ? t("admin.edit") : t("admin.spaceNew")}</h3>
            <button onClick={() => setForm(null)}><X className="w-5 h-5 text-gray-400" /></button>
          </div>
          <div className="space-y-3">
            <div>
              <label className="block text-sm font-medium mb-1">{t("admin.spaceNameLbl")}</label>
              <input className="w-full px-3 py-2 border rounded-lg" value={form.name}
                onChange={(e) => setForm({ ...form, name: e.target.value })} />
            </div>
            {!editTarget && (
              <>
                <div className="grid grid-cols-2 gap-3">
                  <div>
                    <label className="block text-sm font-medium mb-1">{t("admin.spaceTypeLbl")}</label>
                    <select className="w-full px-3 py-2 border rounded-lg" value={form.spaceType}
                      onChange={(e) => setForm({ ...form, spaceType: e.target.value as Space["spaceType"] })}>
                      <option value="DEPARTMENT">DEPARTMENT</option>
                      <option value="FAMILY">FAMILY</option>
                      <option value="SUB_TEAM">SUB_TEAM</option>
                    </select>
                  </div>
                  <div>
                    <label className="block text-sm font-medium mb-1">Code</label>
                    <input className="w-full px-3 py-2 border rounded-lg font-mono uppercase" value={form.code}
                      onChange={(e) => setForm({ ...form, code: e.target.value.toUpperCase().replace(/[^A-Z0-9_-]/g, "") })} />
                  </div>
                </div>
                <div>
                  <label className="block text-sm font-medium mb-1">{t("admin.spaceTypeLbl")} — unité d'organisation *</label>
                  <select className="w-full px-3 py-2 border rounded-lg" value={form.organizationUnitId}
                    onChange={(e) => setForm({ ...form, organizationUnitId: e.target.value })}>
                    <option value="">—</option>
                    {unitOptions.map((u) => <option key={u.id} value={u.id}>{u.label}</option>)}
                  </select>
                </div>
              </>
            )}
            <div className="grid grid-cols-2 gap-3">
              <div>
                <label className="block text-sm font-medium mb-1">{t("admin.spaceTemplateLbl")}</label>
                <select className="w-full px-3 py-2 border rounded-lg" value={form.templateCode}
                  onChange={(e) => setForm({ ...form, templateCode: e.target.value })}>
                  <option value="">—</option>
                  {templates.map((tpl) => <option key={tpl.code} value={tpl.code}>{tpl.name || tpl.code}</option>)}
                </select>
              </div>
              <div>
                <label className="block text-sm font-medium mb-1">{t("admin.spaceVisibilityLbl")}</label>
                <select className="w-full px-3 py-2 border rounded-lg" value={form.visiblePeopleScope}
                  onChange={(e) => setForm({ ...form, visiblePeopleScope: e.target.value as Space["visiblePeopleScope"] })}>
                  <option value="CHURCH">CHURCH</option>
                  <option value="CAMPUS">CAMPUS</option>
                </select>
              </div>
            </div>
            <div className="flex items-center gap-3">
              <div>
                <label className="block text-sm font-medium mb-1">Couleur</label>
                <input type="color" className="h-9 w-16 border rounded" value={form.color}
                  onChange={(e) => setForm({ ...form, color: e.target.value })} />
              </div>
              <div className="flex-1">
                <label className="block text-sm font-medium mb-1">{t("admin.roleDescLbl")}</label>
                <input className="w-full px-3 py-2 border rounded-lg" value={form.description}
                  onChange={(e) => setForm({ ...form, description: e.target.value })} />
              </div>
            </div>
          </div>
          <div className="flex justify-end gap-3 mt-5">
            <button className="px-4 py-2 border rounded-lg" onClick={() => setForm(null)}>{t("admin.cancel")}</button>
            <button
              className="flex items-center gap-2 px-4 py-2 bg-indigo-600 text-white rounded-lg disabled:opacity-50"
              disabled={save.isPending || !form.name.trim() || (!editTarget && !form.organizationUnitId)}
              title={!editTarget && !form.organizationUnitId ? "organizationUnitId" : undefined}
              onClick={() => save.mutate()}
            >
              {save.isPending ? <Loader2 className="w-4 h-4 animate-spin" /> : <Save className="w-4 h-4" />} {t("admin.save")}
            </button>
          </div>
        </Modal>
      )}
    </div>
  );
}

function Modal({ children, onClose }: { children: React.ReactNode; onClose: () => void }) {
  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40 p-4" onClick={onClose}>
      <div className="bg-white rounded-xl shadow-xl p-6 w-full max-w-lg" onClick={(e) => e.stopPropagation()}>
        {children}
      </div>
    </div>
  );
}
