// PORT Develop1 (§G5.5-58) — éditeur de rôles/permissions scopé, qui remplace
// l'ébauche de main (liste lecture seule + bouton « Créer un rôle » sans action).
// La fiche de main reste visible à l'identique (jetons de permissions) et le
// backend a été complété pour le contrat : GET /api/v1/admin/roles (projection
// isSystem + permissions en clés) — voir RoleManagementController.listRolesForEditor.
//
// PARAMÉTRISATION PAR ÉGLISE (Phase 9) — deux branchements :
// 1. la check-list des permissions est alimentée par le catalogue CANONIQUE
//    (`GET /admin/roles/permissions`, table `permissions`) et non plus par
//    `/permissions/catalog` (table legacy `permission_catalog`) : celle-ci contient
//    des clés absentes du canon, que le backend écartait en silence — un
//    « Enregistrer » qui renvoyait 200 sans rien changer.
// 2. un rôle global se **personnalise** pour l'église courante : « Personnaliser
//    pour mon église » instancie la surcouche locale (`POST /admin/roles/{id}/local-copy`),
//    seule ligne réellement exercée par AuthorizationService ; « Réinitialiser » y
//    renonce (`DELETE /admin/roles/{id}/local-copy`) et ré-ancre les porteurs sur le
//    rôle global.
import { useEffect, useMemo, useState } from "react";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import toast from "react-hot-toast";
import { useI18n } from "@/i18n";
import { useTenant } from "@/contexts/TenantContext";
import api, { getErrorMessage } from "@/lib/api";
import { Plus, Pencil, Trash2, KeyRound, X, Save, Loader2, Search, Tags, Sparkles, RotateCcw } from "lucide-react";
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
  // Phase 9 — quatre champs ADDITIFS de la projection (un client qui les ignore
  // raisonne exactement comme avant).
  /** Ligne portée par l'église courante (surcouche ou rôle créé ici). */
  local?: boolean;
  /** false = le rôle global homonyme ne décide plus rien ici, la surcouche prime. */
  effectiveForTenant?: boolean;
  /** true = ligne globale, donc personnalisable (une surcouche peut être créée). */
  customizable?: boolean;
  /** true = ligne globale déjà doublée par une surcouche dans cette église. */
  customized?: boolean;
}

/** Entrée du catalogue canonique (entité `Permission`, table `permissions`). */
interface CatalogEntry {
  key: string;
  label?: string;
  description?: string;
  scope?: string;
  category?: string;
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

  // Phase 9 — « Personnaliser pour mon église » : idempotent côté serveur, et la
  // copie hérite de la matrice globale (aucun droit ne change à l'instant où l'on
  // clique). On ouvre directement l'éditeur sur la ligne locale renvoyée.
  const customizeMutation = useMutation({
    mutationFn: async (globalId: string) =>
      ((await api.post(`/admin/roles/${globalId}/local-copy`)).data) as Role,
    onSuccess: (local) => {
      invalidate();
      setPermTarget(local);
      toast.success(t("admin.roleLocalized"));
    },
    onError: (err: unknown) => toast.error(getErrorMessage(err)),
  });

  // Phase 9 — chemin inverse : supprimer la surcouche rétablit le catalogue global.
  const revertMutation = useMutation({
    mutationFn: async (localId: string) => api.delete(`/admin/roles/${localId}/local-copy`),
    onSuccess: () => {
      invalidate();
      toast.success(t("admin.roleReverted"));
    },
    onError: (err: unknown) => toast.error(getErrorMessage(err)),
  });

  // Index par clé pour relier une ligne globale à sa surcouche (et l'inverse) : la
  // surcouche porte la MÊME clé que son homologue global (uk_role_tenant_key).
  const localByKey = useMemo(
    () => new Map(roles.filter((r) => r.local).map((r) => [r.key, r])),
    [roles],
  );
  const globalByKey = useMemo(
    () => new Set(roles.filter((r) => !r.local).map((r) => r.key)),
    [roles],
  );

  if (isLoading) return <div className="p-8 text-center">…</div>;

  const globalRoles = roles.filter((r) => !r.local);
  const churchRoles = roles.filter((r) => r.local);

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
        <h2 className="text-lg font-semibold text-gray-900 mb-1">{t("admin.rolesGlobal")}</h2>
        <p className="text-xs text-gray-500 mb-3">{t("admin.rolesGlobalHint")}</p>
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          {globalRoles.map((role) => (
            <CardRole
              key={role.id} role={role}
              canAssign={hasPermission("PERMISSION_ASSIGN")}
              canCustomize={hasPermission("ROLE_UPDATE")}
              customizePending={customizeMutation.isPending}
              localCopy={localByKey.get(role.key)}
              onPermissions={(target) => setPermTarget(target)}
              onCustomize={() => customizeMutation.mutate(role.id)}
              t={t}
            />
          ))}
        </div>
      </section>

      <section>
        <h2 className="text-lg font-semibold text-gray-900 mb-3">{t("admin.rolesCustom")}</h2>
        {churchRoles.length === 0 ? (
          <div className="text-center py-8 bg-gray-50 rounded-lg border">
            <p className="text-sm text-gray-400">{t("admin.roleNew")}</p>
          </div>
        ) : (
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            {churchRoles.map((role) => (
              <CardRole
                key={role.id} role={role}
                canAssign={hasPermission("PERMISSION_ASSIGN")}
                canUpdate={hasPermission("ROLE_UPDATE")}
                canDelete={hasPermission("ROLE_DELETE")}
                overridesGlobal={globalByKey.has(role.key)}
                onEdit={() => { setEditTarget(role); setForm({ key: role.key, label: role.label, description: role.description || "", priority: role.priority }); }}
                onPermissions={(target) => setPermTarget(target)}
                onRevert={globalByKey.has(role.key) ? () => {
                  if (window.confirm(t("admin.roleRevertQ", { label: role.label }))) revertMutation.mutate(role.id);
                } : undefined}
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

function CardRole({ role, canUpdate = false, canAssign, canDelete = false, canCustomize = false,
  customizePending = false, localCopy, overridesGlobal = false, onEdit, onPermissions, onCustomize, onRevert, onDelete, t }: {
  role: Role; canUpdate?: boolean; canAssign: boolean; canDelete?: boolean; canCustomize?: boolean;
  customizePending?: boolean;
  /** Surcouche locale de même clé (carte globale uniquement). */
  localCopy?: Role;
  /** true = cette ligne locale double une ligne globale (carte locale uniquement). */
  overridesGlobal?: boolean;
  onEdit?: () => void; onPermissions: (target: Role) => void; onCustomize?: () => void;
  onRevert?: () => void; onDelete?: () => void;
  t: (k: string, p?: Record<string, string>) => string;
}) {
  const [showTitles, setShowTitles] = useState(false);
  // La ligne réellement exercée par le serveur : une carte globale déjà doublée
  // n'est plus qu'un modèle — l'édition doit porter sur sa surcouche.
  const editable = role.local ? role : localCopy ?? null;
  return (
    <div className="bg-white rounded-lg border p-4">
      <div className="flex items-start justify-between">
        <div>
          <div className="flex items-center gap-2 mb-1 flex-wrap">
            <h3 className="font-semibold">{role.label}</h3>
            {role.isSystem && !role.local && (
              <span className="px-2 py-0.5 text-xs bg-gray-100 text-gray-600 rounded-full">{t("admin.roleSystem")}</span>
            )}
            {role.local && overridesGlobal && (
              <span className="px-2 py-0.5 text-xs bg-amber-100 text-amber-800 rounded-full">{t("admin.roleSurcouche")}</span>
            )}
            {!role.local && role.customized && (
              <span className="px-2 py-0.5 text-xs bg-amber-100 text-amber-800 rounded-full">{t("admin.roleCustomized")}</span>
            )}
          </div>
          <p className="text-sm text-gray-500 font-mono">{role.key}</p>
          {role.description && <p className="text-sm text-gray-600 mt-1">{role.description}</p>}
          {role.effectiveForTenant === false && (
            <p className="text-xs text-gray-400 mt-1">{t("admin.roleOverriddenHint")}</p>
          )}
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
          disabled={!canAssign || !editable}
          title={!canAssign ? "PERMISSION_ASSIGN" : (!editable ? t("admin.roleGlobalOnly") : undefined)}
          onClick={() => { if (editable) onPermissions(editable); }}
        >
          <KeyRound className="w-3.5 h-3.5" /> {t("admin.permEditor")}
        </button>
        {/* Phase 9 — un rôle global ne se modifie pas ici : c'est le socle commun
            à toutes les églises. On instancie d'abord la surcouche locale de
            l'église courante, seule ligne vraiment exerçable. L'appel est
            idempotent et ne retire aucun droit. */}
        {onCustomize && !role.local && !localCopy && (
          <button
            className="flex items-center gap-1 text-sm text-amber-700 hover:text-amber-900 disabled:opacity-40 disabled:cursor-not-allowed"
            disabled={!canCustomize || customizePending}
            title={!canCustomize ? "ROLE_UPDATE" : undefined}
            onClick={onCustomize}
          >
            <Sparkles className="w-3.5 h-3.5" /> {t("admin.roleCustomize")}
          </button>
        )}
        {onRevert && (
          <button
            className="flex items-center gap-1 text-sm text-gray-600 hover:text-gray-900"
            onClick={onRevert}
          >
            <RotateCcw className="w-3.5 h-3.5" /> {t("admin.roleRevert")}
          </button>
        )}
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
    // Phase 9 — catalogue CANONIQUE (table `permissions`, les clés réellement
    // exercées par @authz.can). L'ancien `/permissions/catalog` lisait
    // `permission_catalog` (legacy) dont les clés inconnues du canon étaient
    // silencieusement écartées à l'écriture ; depuis, le backend renvoie un 400
    // explicite (UNKNOWN_PERMISSION_KEYS) — brancher ici le canon est donc aussi
    // ce qui rend l'enregistrement réellement effectif.
    queryKey: ["admin", "roles", "permissions"],
    queryFn: async () => {
      const res = await api.get("/admin/roles/permissions");
      return res.data as CatalogEntry[];
    },
  });

  const save = useMutation({
    mutationFn: async () =>
      api.put(`/admin/roles/${role.id}/permissions`, { permissionKeys: [...selected] }),
    onSuccess: () => { toast.success(t("admin.permSaved")); onSaved(); },
    onError: (err: unknown) => toast.error(getErrorMessage(err)),
  });

  const byCategory = useMemo(() => {
    const q = search.trim().toLowerCase();
    const groups: Record<string, CatalogEntry[]> = {};
    for (const entry of catalog) {
      if (q && !entry.key.toLowerCase().includes(q) && !(entry.label || "").toLowerCase().includes(q)) continue;
      const cat = entry.category || "GENERAL";
      (groups[cat] ||= []).push(entry);
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
        {Object.entries(byCategory).map(([category, entries]) => (
          <div key={category}>
            <h4 className="text-xs font-bold uppercase text-gray-500 mb-1">{category}</h4>
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
