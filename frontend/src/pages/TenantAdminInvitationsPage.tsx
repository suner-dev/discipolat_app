import { useEffect, useState } from "react";
import { useAuth } from "@/contexts/AuthContext";
import api from "@/lib/api";

interface Invitation {
  id: string;
  email: string;
  role: string;
  status: string;
  createdAt: string;
  expiresAt: string;
  invitedBy?: string;
}

export default function TenantAdminInvitationsPage() {
  const { hasRole } = useAuth();
  const [invitations, setInvitations] = useState<Invitation[]>([]);
  const [loading, setLoading] = useState(true);
  const [showModal, setShowModal] = useState(false);
  const [newEmail, setNewEmail] = useState("");
  const [newRole, setNewRole] = useState("");
  const [sending, setSending] = useState(false);

  useEffect(() => {
    if (!hasRole("TENANT_OWNER") && !hasRole("TENANT_ADMIN")) return;
    fetchInvitations();
  }, [hasRole]);

  const fetchInvitations = async () => {
    try {
      const res = await api.get("/admin/invitations");
      setInvitations(res.data);
    } catch (error) {
      console.error("Erreur:", error);
    } finally {
      setLoading(false);
    }
  };

  const sendInvitation = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!newEmail || !newRole) return;
    setSending(true);
    try {
      await api.post("/admin/invitations", { email: newEmail, role: newRole });
      setShowModal(false);
      setNewEmail("");
      setNewRole("");
      fetchInvitations();
    } catch (error: any) {
      alert(error.response?.data?.error || "Erreur lors de l'envoi");
    } finally {
      setSending(false);
    }
  };

  const cancelInvitation = async (id: string) => {
    if (!confirm("Annuler cette invitation ?")) return;
    try {
      await api.delete(`/admin/invitations/${id}`);
      fetchInvitations();
    } catch (error) {
      console.error("Erreur:", error);
    }
  };

  const hasAccess = hasRole("TENANT_OWNER") || hasRole("TENANT_ADMIN");
  if (!hasAccess) {
    return <div className="p-8 text-center">Acces non autorise</div>;
  }

  if (loading) return <div className="p-8 text-center">Chargement...</div>;

  const pendingCount = invitations.filter(i => i.status === "PENDING").length;
  const acceptedCount = invitations.filter(i => i.status === "ACCEPTED").length;

  return (
    <div className="p-6">
      <div className="flex justify-between items-center mb-6">
        <h1 className="text-2xl font-bold">Invitations</h1>
        <button
          onClick={() => setShowModal(true)}
          className="px-4 py-2 bg-indigo-600 text-white rounded-lg hover:bg-indigo-700"
        >
          + Nouvelle invitation
        </button>
      </div>

      <div className="grid grid-cols-3 gap-4 mb-6">
        <StatCard label="Total" value={invitations.length} color="indigo" />
        <StatCard label="En attente" value={pendingCount} color="yellow" />
        <StatCard label="Acceptes" value={acceptedCount} color="green" />
      </div>

      {invitations.length === 0 ? (
        <div className="text-center py-12 bg-gray-50 rounded-lg border">
          <p className="text-gray-500 mb-2">Aucune invitation</p>
          <p className="text-sm text-gray-400">Envoyez des invitations pour ajouter des membres a votre organisation</p>
        </div>
      ) : (
        <div className="overflow-x-auto">
          <table className="min-w-full divide-y divide-gray-200">
            <thead className="bg-gray-50">
              <tr>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Email</th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Role</th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Statut</th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Date envoi</th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Expiration</th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Action</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-200">
              {invitations.map((inv) => (
                <tr key={inv.id}>
                  <td className="px-6 py-4">
                    <span className="font-medium">{inv.email}</span>
                  </td>
                  <td className="px-6 py-4">
                    <span className="px-2 py-1 text-xs rounded-full bg-indigo-100 text-indigo-700">
                      {inv.role}
                    </span>
                  </td>
                  <td className="px-6 py-4">
                    <span className={"px-2 py-1 text-xs rounded-full " +
                      (inv.status === "PENDING" ? "bg-yellow-100 text-yellow-700" :
                       inv.status === "ACCEPTED" ? "bg-green-100 text-green-700" :
                       inv.status === "EXPIRED" ? "bg-gray-100 text-gray-700" :
                       "bg-red-100 text-red-700")
                    }>
                      {inv.status}
                    </span>
                  </td>
                  <td className="px-6 py-4 text-sm text-gray-500">
                    {new Date(inv.createdAt).toLocaleDateString("fr-FR")}
                  </td>
                  <td className="px-6 py-4 text-sm text-gray-500">
                    {new Date(inv.expiresAt).toLocaleDateString("fr-FR")}
                  </td>
                  <td className="px-6 py-4">
                    {inv.status === "PENDING" && (
                      <button
                        onClick={() => cancelInvitation(inv.id)}
                        className="text-red-600 hover:text-red-900 text-sm font-medium"
                      >
                        Annuler
                      </button>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {showModal && (
        <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50">
          <div className="bg-white rounded-lg p-6 w-full max-w-md">
            <h2 className="text-xl font-bold mb-4">Nouvelle invitation</h2>
            <form onSubmit={sendInvitation} className="space-y-4">
              <div>
                <label className="block text-sm font-medium mb-1">Email</label>
                <input
                  type="email"
                  value={newEmail}
                  onChange={(e) => setNewEmail(e.target.value)}
                  className="w-full px-4 py-2 border rounded-lg"
                  placeholder="email@exemple.com"
                  required
                />
              </div>
              <div>
                <label className="block text-sm font-medium mb-1">Role</label>
                <select
                  value={newRole}
                  onChange={(e) => setNewRole(e.target.value)}
                  className="w-full px-4 py-2 border rounded-lg"
                  required
                >
                  <option value="">Selectionnez un role</option>
                  <option value="TENANT_OWNER">Proprietaire</option>
                  <option value="TENANT_ADMIN">Administrateur</option>
                  <option value="CHURCH_ADMIN">Admin Eglise</option>
                  <option value="RESPONSABLE">Responsable</option>
                  <option value="CHEF_DE_FAMILLE">Chef de famille</option>
                  <option value="FAISEUR">Faiseur</option>
                  <option value="MEMBRE">Membre</option>
                </select>
              </div>
              <div className="flex gap-4">
                <button
                  type="button"
                  onClick={() => setShowModal(false)}
                  className="flex-1 px-4 py-2 border rounded-lg"
                >
                  Annuler
                </button>
                <button
                  type="submit"
                  disabled={sending}
                  className="flex-1 px-4 py-2 bg-indigo-600 text-white rounded-lg"
                >
                  {sending ? "Envoi..." : "Envoyer"}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}

function StatCard({ label, value, color }: { label: string; value: number; color: string }) {
  const colors: Record<string, string> = {
    indigo: "bg-indigo-50 text-indigo-700",
    yellow: "bg-yellow-50 text-yellow-700",
    green: "bg-green-50 text-green-700",
  };

  return (
    <div className={"p-4 rounded-lg border " + colors[color]}>
      <p className="text-sm font-medium">{label}</p>
      <p className="text-2xl font-bold mt-1">{value}</p>
    </div>
  );
}
