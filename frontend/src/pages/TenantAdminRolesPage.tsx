// PORT Develop1 (§G5.5-58) — éditeur de rôles/permissions scopé, qui remplace
// l'ébauche de main (liste lecture seule + bouton « Créer un rôle » sans action).
// La fiche de main reste visible à l'identique (jetons de permissions) et le
// backend a été complété pour le contrat : GET /api/v1/admin/roles (projection
// isSystem + permissions en clés) — voir RoleManagementController.listRolesForEditor.
import { useEffect, useMemo, useState } from "react";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import toast from "react-hot-toast";
import { useI18n } from "@/i18n";
import { useTenant } from "@/contexts/TenantContext";
import api, { getErrorMessage } from "@/lib/api";
import { Plus, Pencil, Trash2, KeyRound, X, Save, Loader2, Search, Tags } from "lucide-react";
import RoleTitleMatrix from "@/components/organization/RoleTitleMatrix";

/**
 * G5.5 (§58) — Éditeur de rôles/permissions SCOPÉ au tenant.
 * Autorité réelle côté serveur (@authz.can ROLE_CREATE / ROLE_UPDATE /
 * ROLE_DELETE / PERMISSION_ASSIGN) ; l'UI désactive seulement ce qui est
 * interdit — aucun bouton mort silencieux.
 */

interface Role {
  id: string;
  key: string;
  label: string;
  description?: string;
  priority: number;
  isSystem: boolean;
  permissions?: string[];
}

interface CatalogEntry {
  key: string;
  label?: string;
  module?: string;
  description?: string;
}

interface RoleForm {
  key: string;
  label: string;
  description: string;
  priority: number;
}

const EMPTY_FORM: RoleForm = { key: "", label: "", description: "", priority: 50 };

export default function TenantAdminRolesPage() {
  const { t } = useI18n();
  const { hasPermission } = useTenant();
  const queryClient = useQueryClient();
  const [form, setForm] = useState<RoleForm | null>(null);
  const [editTarget, setEditTarget] = useState<Role | null>(null);
  const [permTarget, setPermTarget] = useState<Role | null>(null);

  const { data: roles = [], isLoading } = useQuery({
    queryKey: ["admin", "roles"],
    queryFn: async () => {
      const res = await api.get("/admin/roles");
      return res.data as Role[];
    },
  });

  const invalidate = () => queryClient.invalidateQueries({ queryKey: ["admin", "roles"] });

  const saveMutation = useMutation({
    mutationFn: async () => {
      if (editTarget) {
        await api.put(`/admin/roles/${editTarget.id}`, {
          label: form!.label,
          description: form!.description,
          priority: form!.priority,
        });
      } else {
        await api.post("/admin/roles", {
          key: form!.key.trim().toUpperCase(),
          label: form!.label,
          description: form!.description,
          priority: form!.priority,
          permissionKeys: [],
        });
      }
    },
    onSuccess: () => {
      invalidate();
      setForm(null);
      setEditTarget(null);
      toast.success(t("admin.roleSaved"));
    },
    onError: (err: unknown) => toast.error(getErrorMessage(err)),
  });

  const deleteMutation = useMutation({
    mutationFn: async (id: string) => api.delete(`/admin/roles/${id}`),
    onSuccess: () => {
      invalidate();
      toast.success(t("admin.roleDeleted"));
    },
    onError: (err: unknown) => toast.error(getErrorMessage(err)),
  });

  if (isLoading) return <div className="p-8 text-center">…</div>;

  const systemRoles = roles.filter((r) => r.isSystem);
  const customRoles = roles.filter((r) => !r.isSystem);

  return (
    <div className="p-6">
      <div className="flex justify-between items-center mb-6">
        <h1 className="text-2xl font-bold">{t("admin.rolesTitle")}</h1>
        <button
          className="flex items-center gap-2 px-4 py-2 bg-indigo-600 text-white rounded-lg hover:bg-indigo-700 disabled:opacity-50 disabled:cursor-not-allowed"
          disabled={!hasPermission("ROLE_CREATE")}
          title={!hasPermission("ROLE_CREATE") ? "ROLE_CREATE" : undefined}
          onClick={() => { setEditTarget(null); setForm(EMPTY_FORM); }}
        >
          <Plus className="w-4 h-4" /> {t("admin.roleNew")}
        </button>
      </div>

      <section className="mb-6">
        <h2 className="text-lg font-semibold text-gray-900 mb-3">{t("admin.roleSystem")}</h2>
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          {systemRoles.map((role) => (
            <CardRole
              key={role.id} role={role}
              canAssign={hasPermission("PERMISSION_ASSIGN")}
              onPermissions={() => setPermTarget(role)}
              t={t}
            />
          ))}
        </div>
      </section>

      <section>
        <h2 className="text-lg font-semibold text-gray-900 mb-3">{t("admin.rolesCustom")}</h2>
        {customRoles.length === 0 ? (
          <div className="text-center py-8 bg-gray-50 rounded-lg border">
            <p className="text-sm text-gray-400">{t("admin.roleNew")}</p>
          </div>
        ) : (
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            {customRoles.map((role) => (
              <CardRole
                key={role.id} role={role}
                canAssign={hasPermission("PERMISSION_ASSIGN")}
                canUpdate={hasPermission("ROLE_UPDATE")}
                canDelete={hasPermission("ROLE_DELETE")}
                onEdit={() => { setEditTarget(role); setForm({ key: role.key, label: role.label, description: role.description || "", priority: role.priority }); }}
                onPermissions={() => setPermTarget(role)}
                onDelete={() => {
                  if (window.confirm(t("admin.roleDeleteQ", { label: role.label }))) deleteMutation.mutate(role.id);
                }}
                t={t}
              />
            ))}
          </div>
        )}
      </section>

      {/* Modale création / édition */}
      {form && (
        <Modal onClose={() => setForm(null)}>
          <h3 className="text-lg font-bold mb-4">{editTarget ? t("admin.roleEditTitle") : t("admin.roleNew")}</h3>
          <div className="space-y-4">
            {!editTarget && (
              <Field label={t("admin.roleKeyLbl")}>
                <input
                  className="w-full px-3 py-2 border rounded-lg font-mono uppercase"
                  value={form.key}
                  onChange={(e) => setForm({ ...form, key: e.target.value.toUpperCase().replace(/[^A-Z0-9_]/g, "") })}
                  placeholder="RESPONSABLE_FINANCES"
                />
              </Field>
            )}
            <Field label={t("admin.roleNameLbl")}>
              <input className="w-full px-3 py-2 border rounded-lg" value={form.label}
                onChange={(e) => setForm({ ...form, label: e.target.value })} />
            </Field>
            <Field label={t("admin.roleDescLbl")}>
              <textarea className="w-full px-3 py-2 border rounded-lg" rows={2} value={form.description}
                onChange={(e) => setForm({ ...form, description: e.target.value })} />
            </Field>
            <Field label={t("admin.rolePriorityLbl")}>
              <input type="number" min={0} max={100} className="w-32 px-3 py-2 border rounded-lg" value={form.priority}
                onChange={(e) => setForm({ ...form, priority: Number(e.target.value) })} />
            </Field>
          </div>
          <div className="flex justify-end gap-3 mt-6">
            <button className="px-4 py-2 border rounded-lg" onClick={() => setForm(null)}>{t("admin.cancel")}</button>
            <button
              className="flex items-center gap-2 px-4 py-2 bg-indigo-600 text-white rounded-lg disabled:opacity-50"
              disabled={!form.label.trim() || (!editTarget && !form.key.trim()) || saveMutation.isPending}
              onClick={() => saveMutation.mutate()}
            >
              {saveMutation.isPending ? <Loader2 className="w-4 h-4 animate-spin" /> : <Save className="w-4 h-4" />}
              {t("admin.save")}
            </button>
          </div>
        </Modal>
      )}

      {/* Éditeur de permissions scopé */}
      {permTarget && (
        <PermissionEditor
          role={permTarget}
          onClose={() => setPermTarget(null)}
          onSaved={() => { invalidate(); setPermTarget(null); }}
          t={t}
        />
      )}
    </div>
  );
}

function CardRole({ role, canUpdate = false, canAssign, canDelete = false, onEdit, onPermissions, onDelete, t }: {
  role: Role; canUpdate?: boolean; canAssign: boolean; canDelete?: boolean;
  onEdit?: () => void; onPermissions: () => void; onDelete?: () => void;
  t: (k: string, p?: Record<string, string>) => string;
}) {
  const [showTitles, setShowTitles] = useState(false);
  return (
    <div className="bg-white rounded-lg border p-4">
      <div className="flex items-start justify-between">
        <div>
          <div className="flex items-center gap-2 mb-1">
            <h3 className="font-semibold">{role.label}</h3>
            {role.isSystem && (
              <span className="px-2 py-0.5 text-xs bg-gray-100 text-gray-600 rounded-full">{t("admin.roleSystem")}</span>
            )}
          </div>
          <p className="text-sm text-gray-500 font-mono">{role.key}</p>
          {role.description && <p className="text-sm text-gray-600 mt-1">{role.description}</p>}
        </div>
        <div className="text-right">
          <p className="text-sm text-gray-500">{t("admin.rolePriorityLbl")}: {role.priority}</p>
          <p className="text-sm text-gray-500">{role.permissions?.length || 0}</p>
        </div>
      </div>
      {/* Affichage conserve de main : les permissions en jetons, au-dela du simple
          compteur de Develop1 — la fiche doit rester lisible sans ouvrir l'editeur. */}
      {role.permissions && role.permissions.length > 0 && (
        <div className="mt-3 pt-3 border-t">
          <p className="text-xs text-gray-500 mb-1">{t("admin.permEditor")}</p>
          <div className="flex flex-wrap gap-1">
            {role.permissions.slice(0, 5).map((perm) => (
              <span key={perm} className="px-2 py-0.5 text-xs bg-indigo-50 text-indigo-700 rounded">
                {perm}
              </span>
            ))}
            {role.permissions.length > 5 && (
              <span className="text-xs text-gray-500">+{role.permissions.length - 5}</span>
            )}
          </div>
        </div>
      )}
      <div className="flex gap-3 mt-3 pt-3 border-t">
        {onEdit && (
          <button
            className="flex items-center gap-1 text-sm text-indigo-600 hover:text-indigo-900 disabled:opacity-40 disabled:cursor-not-allowed"
            disabled={!canUpdate}
            title={!canUpdate ? "ROLE_UPDATE" : undefined}
            onClick={onEdit}
          >
            <Pencil className="w-3.5 h-3.5" /> {t("admin.edit")}
          </button>
        )}
        <button
          className="flex items-center gap-1 text-sm text-emerald-700 hover:text-emerald-900 disabled:opacity-40 disabled:cursor-not-allowed"
          disabled={!canAssign}
          title={!canAssign ? "PERMISSION_ASSIGN" : undefined}
          onClick={onPermissions}
        >
          <KeyRound className="w-3.5 h-3.5" /> {t("admin.permEditor")}
        </button>
        <button
          className="flex items-center gap-1 text-sm text-purple-700 hover:text-purple-900"
          onClick={() => setShowTitles((s) => !s)}
        >
          <Tags className="w-3.5 h-3.5" /> {t("orgV3.titles.expand")}
        </button>
        {onDelete && (
          <button
            className="ml-auto flex items-center gap-1 text-sm text-red-600 hover:text-red-800 disabled:opacity-40 disabled:cursor-not-allowed"
            disabled={!canDelete}
            title={!canDelete ? "ROLE_DELETE" : undefined}
            onClick={onDelete}
          >
            <Trash2 className="w-3.5 h-3.5" /> {t("admin.delete")}
          </button>
        )}
      </div>
      {showTitles && (
        <div className="mt-3 pt-3 border-t">
          <RoleTitleMatrix roleId={role.id} roleLabel={role.label} />
        </div>
      )}
    </div>
  );
}

function PermissionEditor({ role, onClose, onSaved, t }: {
  role: Role; onClose: () => void; onSaved: () => void;
  t: (k: string, p?: Record<string, string>) => string;
}) {
  const [selected, setSelected] = useState<Set<string>>(new Set(role.permissions || []));
  const [search, setSearch] = useState("");

  const { data: catalog = [], isLoading } = useQuery({
    queryKey: ["permissions", "catalog"],
    queryFn: async () => {
      const res = await api.get("/permissions/catalog");
      return res.data as CatalogEntry[];
    },
  });

  const save = useMutation({
    mutationFn: async () =>
      api.put(`/admin/roles/${role.id}/permissions`, { permissionKeys: [...selected] }),
    onSuccess: () => { toast.success(t("admin.permSaved")); onSaved(); },
    onError: (err: unknown) => toast.error(getErrorMessage(err)),
  });

  const byModule = useMemo(() => {
    const q = search.trim().toLowerCase();
    const groups: Record<string, CatalogEntry[]> = {};
    for (const entry of catalog) {
      if (q && !entry.key.toLowerCase().includes(q) && !(entry.label || "").toLowerCase().includes(q)) continue;
      const mod = entry.module || "GENERAL";
      (groups[mod] ||= []).push(entry);
    }
    return groups;
  }, [catalog, search]);

  return (
    <Modal onClose={onClose} wide>
      <div className="flex items-center justify-between mb-2">
        <h3 className="text-lg font-bold">{t("admin.permEditor")} — {role.label}</h3>
        <button onClick={onClose}><X className="w-5 h-5 text-gray-400" /></button>
      </div>
      <div className="relative mb-3">
        <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400" />
        <input
          className="w-full pl-9 pr-3 py-2 border rounded-lg"
          placeholder={t("admin.permSearch")}
          value={search}
          onChange={(e) => setSearch(e.target.value)}
        />
      </div>
      <p className="text-xs text-gray-500 mb-2">{t("admin.permCount", { n: String(selected.size) })}</p>
      <div className="max-h-[50vh] overflow-y-auto space-y-4 pr-1">
        {isLoading && <p className="text-center text-gray-400 py-6">…</p>}
        {Object.entries(byModule).map(([mod, entries]) => (
          <div key={mod}>
            <h4 className="text-xs font-bold uppercase text-gray-500 mb-1">{mod}</h4>
            <div className="grid grid-cols-1 md:grid-cols-2 gap-1">
              {entries.map((entry) => (
                <label key={entry.key} className="flex items-center gap-2 text-sm px-2 py-1 rounded hover:bg-gray-50 cursor-pointer">
                  <input
                    type="checkbox"
                    checked={selected.has(entry.key)}
                    onChange={() => {
                      setSelected((prev) => {
                        const next = new Set(prev);
                        if (next.has(entry.key)) next.delete(entry.key); else next.add(entry.key);
                        return next;
                      });
                    }}
                  />
                  <span className="font-mono text-xs">{entry.key}</span>
                  {entry.label && <span className="text-gray-400 truncate">{entry.label}</span>}
                </label>
              ))}
            </div>
          </div>
        ))}
      </div>
      <div className="flex justify-end gap-3 mt-4">
        <button className="px-4 py-2 border rounded-lg" onClick={onClose}>{t("admin.cancel")}</button>
        <button
          className="flex items-center gap-2 px-4 py-2 bg-indigo-600 text-white rounded-lg disabled:opacity-50"
          disabled={save.isPending}
          onClick={() => save.mutate()}
        >
          {save.isPending ? <Loader2 className="w-4 h-4 animate-spin" /> : <Save className="w-4 h-4" />}
          {t("admin.save")}
        </button>
      </div>
    </Modal>
  );
}

/* ---------- UI primitives ---------- */

function Modal({ children, onClose, wide = false }: { children: React.ReactNode; onClose: () => void; wide?: boolean }) {
  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40 p-4" onClick={onClose}>
      <div className={"bg-white rounded-xl shadow-xl p-6 w-full " + (wide ? "max-w-3xl" : "max-w-md")}
        onClick={(e) => e.stopPropagation()}>
        {children}
      </div>
    </div>
  );
}

function Field({ label, children }: { label: string; children: React.ReactNode }) {
  return (
    <div>
      <label className="block text-sm font-medium mb-1">{label}</label>
      {children}
    </div>
  );
}
