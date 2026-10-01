import { useEffect, useState } from "react";
import { useTenant } from "@/contexts/TenantContext";
import { useAuth } from "@/contexts/AuthContext";
import { useImpersonation } from "@/contexts/ImpersonationContext";
import { useI18n } from "@/i18n";
import api, { getErrorMessage } from "@/lib/api";
import toast from "react-hot-toast";
import { Eye, UserPlus, Trash2, Loader2, X } from "lucide-react";

/**
 * G5.5 (§58 / §G3.2) — Membres du tenant : affectation de rôle RÉELLE
 * (PUT /admin/members/{id}/role, isolation + dernier propriétaire gardé côté
 * serveur), invitation réelle (POST /admin/invitations) et révocation
 * (DELETE /admin/members/{id}). UI = confort, autorité = serveur.
 */

interface Member {
  membershipId: string;
  userId: string;
  email: string;
  firstName: string;
  lastName: string;
  fullName: string;
  role: string;
  status: string;
  joinedAt: string;
  photoUrl?: string;
  phone?: string;
  isActive: boolean;
}

interface RoleOption { key: string; label: string }

export default function TenantAdminMembersPage() {
  const { t } = useI18n();
  const { hasPermission, currentTenant } = useTenant();
  const { user } = useAuth();
  const { startImpersonation } = useImpersonation();
  const [members, setMembers] = useState<Member[]>([]);
  const [roles, setRoles] = useState<RoleOption[]>([]);
  const [loading, setLoading] = useState(true);
  const [page, setPage] = useState(0);
  const [search, setSearch] = useState("");
  const [roleFilter, setRoleFilter] = useState("");
  const [editTarget, setEditTarget] = useState<Member | null>(null);
  const [inviteOpen, setInviteOpen] = useState(false);
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    if (!hasPermission("USER_MANAGE")) {
      setLoading(false);
      return;
    }
    fetchMembers();
  }, [page, roleFilter]);

  useEffect(() => {
    // Rôles du tenant pour l'éditeur d'affectation + le formulaire d'invitation.
    api.get("/admin/roles")
      .then((res) => setRoles((res.data || []).map((r: RoleOption) => ({ key: r.key, label: r.label }))))
      .catch(() => setRoles([]));
  }, []);

  const fetchMembers = async () => {
    try {
      const params = new URLSearchParams();
      params.set("page", page.toString());
      params.set("size", "20");
      if (search) params.set("search", search);
      if (roleFilter) params.set("role", roleFilter);
      const res = await api.get(`/admin/members?${params}`);
      setMembers(res.data.content || []);
    } catch (error) {
      console.error("Error fetching members:", error);
    } finally {
      setLoading(false);
    }
  };

  const changeRole = async (member: Member, roleKey: string) => {
    setBusy(true);
    try {
      await api.put(`/admin/members/${member.membershipId}/role`, { roleKey });
      toast.success(t("admin.memberRoleSaved"));
      setEditTarget(null);
      fetchMembers();
    } catch (err) {
      toast.error(getErrorMessage(err));
    } finally {
      setBusy(false);
    }
  };

  const removeMember = async (member: Member) => {
    if (!window.confirm(t("admin.memberRemoveQ", { name: member.fullName || member.email }))) return;
    try {
      await api.delete(`/admin/members/${member.membershipId}`);
      toast.success(t("admin.memberRemoved"));
      fetchMembers();
    } catch (err) {
      toast.error(getErrorMessage(err));
    }
  };

  if (loading) {
    return <div className="p-8 text-center">…</div>;
  }

  return (
    <div className="p-6">
      <div className="flex justify-between items-center mb-6">
        <h1 className="text-2xl font-bold">{t("admin.tMembers")}</h1>
        <button
          className="flex items-center gap-2 px-4 py-2 bg-indigo-600 text-white rounded-lg hover:bg-indigo-700 disabled:opacity-50 disabled:cursor-not-allowed"
          disabled={!hasPermission("USER_INVITE")}
          title={!hasPermission("USER_INVITE") ? "USER_INVITE" : undefined}
          onClick={() => setInviteOpen(true)}
        >
          <UserPlus className="w-4 h-4" /> {t("admin.inviteTitle")}
        </button>
      </div>

      <div className="flex gap-4 mb-6">
        <input
          type="text"
          placeholder="Rechercher par nom ou email..."
          value={search}
          onChange={(e) => setSearch(e.target.value)}
          onKeyDown={(e) => { if (e.key === "Enter") { setPage(0); fetchMembers(); } }}
          className="flex-1 px-4 py-2 border rounded-lg"
        />
        <select
          value={roleFilter}
          onChange={(e) => { setRoleFilter(e.target.value); setPage(0); }}
          className="px-4 py-2 border rounded-lg"
        >
          <option value="">Tous les rôles</option>
          {roles.map((r) => (
            <option key={r.key} value={r.key}>{r.label}</option>
          ))}
        </select>
      </div>

      <div className="overflow-x-auto">
        <table className="min-w-full divide-y divide-gray-200">
          <thead className="bg-gray-50">
            <tr>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Nom</th>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Email</th>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Rôle</th>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Statut</th>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Date d'ajout</th>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Actions</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-gray-200">
            {members.map((member) => (
              <tr key={member.membershipId}>
                <td className="px-6 py-4">
                  <div className="flex items-center gap-3">
                    {member.photoUrl ? (
                      <img src={member.photoUrl} alt="" className="w-8 h-8 rounded-full" />
                    ) : (
                      <div className="w-8 h-8 rounded-full bg-gray-200 flex items-center justify-center">
                        <span className="text-sm font-medium">
                          {member.firstName[0] || member.email[0].toUpperCase()}
                        </span>
                      </div>
                    )}
                    <span className="font-medium">{member.fullName || member.email}</span>
                  </div>
                </td>
                <td className="px-6 py-4 text-sm text-gray-500">{member.email}</td>
                <td className="px-6 py-4">
                  <span className="px-2 py-1 text-xs rounded-full bg-indigo-100 text-indigo-700">
                    {member.role}
                  </span>
                </td>
                <td className="px-6 py-4">
                  <span className={"px-2 py-1 text-xs rounded-full " +
                    (member.isActive ? "bg-emerald-100 text-emerald-700" : "bg-gray-100 text-gray-700")
                  }>
                    {member.status}
                  </span>
                </td>
                <td className="px-6 py-4 text-sm text-gray-500">
                  {new Date(member.joinedAt).toLocaleDateString("fr-FR")}
                </td>
                <td className="px-6 py-4">
                  <div className="flex items-center gap-3">
                    <button
                      className="text-indigo-600 hover:text-indigo-900 text-sm font-medium disabled:opacity-40 disabled:cursor-not-allowed"
                      disabled={!hasPermission("USER_MANAGE")}
                      title={!hasPermission("USER_MANAGE") ? "USER_MANAGE" : undefined}
                      onClick={() => setEditTarget(member)}
                    >
                      {t("admin.edit")}
                    </button>
                    <button
                      className="text-red-600 hover:text-red-800 text-sm font-medium disabled:opacity-40 disabled:cursor-not-allowed"
                      disabled={!hasPermission("USER_MANAGE") || member.userId === user?.id}
                      title={!hasPermission("USER_MANAGE") ? "USER_MANAGE" : t("admin.memberRemove")}
                      onClick={() => removeMember(member)}
                    >
                      <Trash2 className="w-4 h-4" />
                    </button>
                    {/* §G1.9 — Impersonation (super admin plateforme uniquement, motif requis + journalisé) */}
                    {user?.platformSuperAdmin === true && (
                      <button
                        className="flex items-center gap-1 text-violet-600 hover:text-violet-800 text-sm font-medium"
                        title="Impersoner cet utilisateur (diagnostic)"
                        onClick={async () => {
                          const reason = window.prompt(
                            `Impersonation de ${member.email} — motif (obligatoire, journalisé) :`
                          );
                           if (!reason || !reason.trim()) {
                             toast.error("Un motif est requis pour impersoner");
                             return;
                           }
                           if (!currentTenant) {
                             toast.error("Le tenant cible est requis pour impersoner");
                             return;
                           }
                           await startImpersonation(member.email, reason.trim(), currentTenant.id);
                        }}
                      >
                        <Eye className="w-4 h-4" />
                        Impersoner
                      </button>
                    )}
                  </div>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      {members.length === 0 && (
        <div className="text-center py-12">
          <p className="text-gray-500">Aucun membre trouvé</p>
        </div>
      )}

      {members.length > 0 && (
        <div className="flex justify-between items-center mt-6">
          <button
            onClick={() => setPage(p => Math.max(0, p - 1))}
            disabled={page === 0}
            className="px-4 py-2 border rounded-lg disabled:opacity-50"
          >
            Précédent
          </button>
          <span className="px-4">Page {page + 1}</span>
          <button
            onClick={() => setPage(p => p + 1)}
            className="px-4 py-2 border rounded-lg"
          >
            Suivant
          </button>
        </div>
      )}

      {/* Affectation de rôle (§G3.2) — serveur valide tenant + dernier propriétaire */}
      {editTarget && (
        <RoleModal
          member={editTarget} roles={roles} busy={busy}
          onClose={() => setEditTarget(null)}
          onSave={(roleKey) => changeRole(editTarget, roleKey)}
          t={t}
        />
      )}

      {inviteOpen && (
        <InviteModal
          roles={roles} busy={busy}
          onClose={() => setInviteOpen(false)}
          onDone={() => { setInviteOpen(false); fetchMembers(); }}
          t={t}
        />
      )}
    </div>
  );
}

function RoleModal({ member, roles, busy, onClose, onSave, t }: {
  member: Member; roles: RoleOption[]; busy: boolean; onClose: () => void;
  onSave: (roleKey: string) => void;
  t: (k: string, p?: Record<string, string>) => string;
}) {
  const [roleKey, setRoleKey] = useState(member.role);
  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40 p-4" onClick={onClose}>
      <div className="bg-white rounded-xl shadow-xl p-6 w-full max-w-md" onClick={(e) => e.stopPropagation()}>
        <div className="flex justify-between items-center mb-4">
          <h3 className="text-lg font-bold">{member.fullName || member.email}</h3>
          <button onClick={onClose}><X className="w-5 h-5 text-gray-400" /></button>
        </div>
        <label className="block text-sm font-medium mb-1">{t("admin.memberRoleLbl")}</label>
        <select className="w-full px-3 py-2 border rounded-lg mb-4" value={roleKey} onChange={(e) => setRoleKey(e.target.value)}>
          {roles.map((r) => <option key={r.key} value={r.key}>{r.label} ({r.key})</option>)}
        </select>
        <div className="flex justify-end gap-3">
          <button className="px-4 py-2 border rounded-lg" onClick={onClose}>{t("admin.cancel")}</button>
          <button
            className="flex items-center gap-2 px-4 py-2 bg-indigo-600 text-white rounded-lg disabled:opacity-50"
            disabled={busy || !roleKey || roleKey === member.role}
            onClick={() => onSave(roleKey)}
          >
            {busy && <Loader2 className="w-4 h-4 animate-spin" />} {t("admin.save")}
          </button>
        </div>
      </div>
    </div>
  );
}

function InviteModal({ roles, busy, onClose, onDone, t }: {
  roles: RoleOption[]; busy: boolean; onClose: () => void; onDone: () => void;
  t: (k: string, p?: Record<string, string>) => string;
}) {
  const [email, setEmail] = useState("");
  const [role, setRole] = useState("MEMBRE");
  const [sending, setSending] = useState(false);

  const send = async () => {
    setSending(true);
    try {
      await api.post("/admin/invitations", { email: email.trim(), role });
      toast.success(t("admin.inviteSent"));
      setEmail("");
      onDone();
    } catch (err) {
      toast.error(getErrorMessage(err));
    } finally {
      setSending(false);
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40 p-4" onClick={onClose}>
      <div className="bg-white rounded-xl shadow-xl p-6 w-full max-w-md" onClick={(e) => e.stopPropagation()}>
        <div className="flex justify-between items-center mb-4">
          <h3 className="text-lg font-bold">{t("admin.inviteTitle")}</h3>
          <button onClick={onClose}><X className="w-5 h-5 text-gray-400" /></button>
        </div>
        <label className="block text-sm font-medium mb-1">Email</label>
        <input type="email" className="w-full px-3 py-2 border rounded-lg mb-3" value={email}
          onChange={(e) => setEmail(e.target.value)} placeholder="person@exemple.org" />
        <label className="block text-sm font-medium mb-1">{t("admin.memberRoleLbl")}</label>
        <select className="w-full px-3 py-2 border rounded-lg mb-4" value={role} onChange={(e) => setRole(e.target.value)}>
          {(roles.length > 0 ? roles : [{ key: "MEMBRE", label: "Membre" }]).map((r) => (
            <option key={r.key} value={r.key}>{r.label} ({r.key})</option>
          ))}
        </select>
        <div className="flex justify-end gap-3">
          <button className="px-4 py-2 border rounded-lg" onClick={onClose}>{t("admin.cancel")}</button>
          <button
            className="flex items-center gap-2 px-4 py-2 bg-indigo-600 text-white rounded-lg disabled:opacity-50"
            disabled={sending || busy || !/^\S+@\S+\.\S+$/.test(email.trim())}
            onClick={send}
          >
            {sending ? <Loader2 className="w-4 h-4 animate-spin" /> : <UserPlus className="w-4 h-4" />}
            {t("admin.inviteSend")}
          </button>
        </div>
      </div>
    </div>
  );
}
