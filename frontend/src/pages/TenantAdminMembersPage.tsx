import { useEffect, useState } from "react";
import { useAuth } from "@/contexts/AuthContext";
import api from "@/lib/api";

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

export default function TenantAdminMembersPage() {
  const { hasRole } = useAuth();
  const [members, setMembers] = useState<Member[]>([]);
  const [loading, setLoading] = useState(true);
  const [page, setPage] = useState(0);
  const [search, setSearch] = useState("");
  const [roleFilter, setRoleFilter] = useState("");

  useEffect(() => {
    if (!hasRole("TENANT_OWNER") && !hasRole("TENANT_ADMIN")) {
      return;
    }
    fetchMembers();
  }, [page, search, roleFilter]);

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

  const hasAccess = hasRole("TENANT_OWNER") || hasRole("TENANT_ADMIN");
  if (!hasAccess) {
    return <div className="p-8 text-center">Acces non autorise</div>;
  }

  if (loading) {
    return <div className="p-8 text-center">Chargement...</div>;
  }

  return (
    <div className="p-6">
      <div className="flex justify-between items-center mb-6">
        <h1 className="text-2xl font-bold">Membres</h1>
        <button className="px-4 py-2 bg-indigo-600 text-white rounded-lg hover:bg-indigo-700">
          + Inviter un membre
        </button>
      </div>

      <div className="flex gap-4 mb-6">
        <input
          type="text"
          placeholder="Rechercher par nom ou email..."
          value={search}
          onChange={(e) => setSearch(e.target.value)}
          className="flex-1 px-4 py-2 border rounded-lg"
        />
        <select
          value={roleFilter}
          onChange={(e) => setRoleFilter(e.target.value)}
          className="px-4 py-2 border rounded-lg"
        >
          <option value="">Tous les roles</option>
          <option value="TENANT_OWNER">Proprietaire</option>
          <option value="TENANT_ADMIN">Administrateur</option>
          <option value="ADMIN">Admin</option>
          <option value="PASTEUR">Pastor</option>
          <option value="RESPONSABLE">Responsable</option>
          <option value="CHEF_DE_FAMILLE">Chef de famille</option>
          <option value="FAISEUR">Faiseur</option>
          <option value="MEMBRE">Membre</option>
        </select>
      </div>

      <div className="overflow-x-auto">
        <table className="min-w-full divide-y divide-gray-200">
          <thead className="bg-gray-50">
            <tr>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Nom</th>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Email</th>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Role</th>
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
                  <button className="text-indigo-600 hover:text-indigo-900 text-sm font-medium">
                    Modifier
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      {members.length === 0 && (
        <div className="text-center py-12">
          <p className="text-gray-500">Aucun membre trouve</p>
        </div>
      )}

      {members.length > 0 && (
        <div className="flex justify-between items-center mt-6">
          <button
            onClick={() => setPage(p => Math.max(0, p - 1))}
            disabled={page === 0}
            className="px-4 py-2 border rounded-lg disabled:opacity-50"
          >
            Precedent
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
    </div>
  );
}
