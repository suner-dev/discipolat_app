import { useEffect, useState } from "react";
import { useTenant } from "@/contexts/TenantContext";
import api from "@/lib/api";

interface Role {
  id: string;
  key: string;
  label: string;
  description?: string;
  priority: number;
  isSystem: boolean;
  permissions?: string[];
}

export default function TenantAdminRolesPage() {
  const { hasPermission } = useTenant();
  const [roles, setRoles] = useState<Role[]>([]);
  const [loading, setLoading] = useState(true);
  const [activeTab, setActiveTab] = useState<"list" | "permissions">("list");

  useEffect(() => {
    if (!hasPermission("ROLE_READ")) return;
    fetchRoles();
  }, [hasPermission]);

  const fetchRoles = async () => {
    try {
      const res = await api.get("/admin/roles");
      setRoles(res.data);
    } catch (error) {
      console.error("Erreur:", error);
    } finally {
      setLoading(false);
    }
  };

  if (loading) return <div className="p-8 text-center">Chargement...</div>;

  const systemRoles = roles.filter(r => r.isSystem);
  const customRoles = roles.filter(r => !r.isSystem);

  return (
    <div className="p-6">
      <div className="flex justify-between items-center mb-6">
        <h1 className="text-2xl font-bold">Rôles et Permissions</h1>
        <button
          className="px-4 py-2 bg-indigo-600 text-white rounded-lg hover:bg-indigo-700"
          disabled={!hasPermission("ROLE_CREATE")}
        >
          + Créer un rôle
        </button>
      </div>

      <div className="space-y-4">
        <h2 className="text-lg font-semibold text-gray-900">Rôles système (non modifiables)</h2>
        {systemRoles.length === 0 ? (
          <p className="text-gray-500 text-sm">Aucun rôle système</p>
        ) : (
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            {systemRoles.map((role) => (
              <CardRole key={role.id} role={role} />
            ))}
          </div>
        )}

        <h2 className="text-lg font-semibold text-gray-900 mt-6">Rôles personnalisés</h2>
        {customRoles.length === 0 ? (
          <div className="text-center py-8 bg-gray-50 rounded-lg border">
            <p className="text-gray-500 mb-2">Aucun rôle personnalisé créé</p>
            <p className="text-sm text-gray-400">Vous pouvez créer des rôles sur mesure pour votre organisation</p>
          </div>
        ) : (
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            {customRoles.map((role) => (
              <CardRole key={role.id} role={role} />
            ))}
          </div>
        )}
      </div>
    </div>
  );
}

function CardRole({ role }: { role: Role }) {
  return (
    <div className="bg-white rounded-lg border p-4">
      <div className="flex items-start justify-between">
        <div>
          <div className="flex items-center gap-2 mb-1">
            <h3 className="font-semibold">{role.label}</h3>
            {role.isSystem && (
              <span className="px-2 py-0.5 text-xs bg-gray-100 text-gray-600 rounded-full">
                Systeme
              </span>
            )}
          </div>
          <p className="text-sm text-gray-500">{role.key}</p>
          {role.description && (
            <p className="text-sm text-gray-600 mt-1">{role.description}</p>
          )}
        </div>
        <div className="text-right">
          <p className="text-sm text-gray-500">Priorite: {role.priority}</p>
          <p className="text-sm text-gray-500">{role.permissions?.length || 0} permissions</p>
        </div>
      </div>
      {role.permissions && role.permissions.length > 0 && (
        <div className="mt-3 pt-3 border-t">
          <p className="text-xs text-gray-500 mb-1">Permissions:</p>
          <div className="flex flex-wrap gap-1">
            {role.permissions.slice(0, 5).map((perm) => (
              <span key={perm} className="px-2 py-0.5 text-xs bg-indigo-50 text-indigo-700 rounded">
                {perm}
              </span>
            ))}
            {role.permissions.length > 5 && (
              <span className="text-xs text-gray-500">+{role.permissions.length - 5} autres</span>
            )}
          </div>
        </div>
      )}
    </div>
  );
}
