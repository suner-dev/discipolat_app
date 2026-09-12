import { useEffect, useState } from "react";
import { useTenant } from "@/contexts/TenantContext";
import api from "@/lib/api";

interface TenantDashboard {
  tenantId: string;
  tenantName: string;
  totalUsers: number;
  activeUsers: number;
  totalMemberships: number;
  membersByRole: Record<string, number>;
  churchCount: number;
  departmentCount: number;
  subChurchCount: number;
  campusCount: number;
  groupCount: number;
  subscription?: {
    planKey: string;
    status: string;
    currentPeriodEnd: string;
  };
}

export default function TenantAdminDashboard() {
  const { hasPermission, hasRole, currentTenant } = useTenant();
  const [dashboard, setDashboard] = useState<TenantDashboard | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (!hasPermission("TENANT_SETTINGS_READ")) {
      return;
    }
    fetchDashboard();
  }, [hasPermission]);

  const fetchDashboard = async () => {
    try {
      const res = await api.get("/admin/dashboard");
      setDashboard(res.data as unknown as TenantDashboard);
    } catch (error) {
      console.error("Error fetching dashboard:", error);
    } finally {
      setLoading(false);
    }
  };

  const hasAccess = hasPermission("TENANT_SETTINGS_READ");
  if (!hasAccess) {
    return <div className="p-8 text-center">Accès non autorisé</div>;
  }

  if (loading) {
    return <div className="p-8 text-center">Chargement...</div>;
  }

  return (
    <div className="p-6 space-y-6">
      <div className="flex justify-between items-center">
        <h1 className="text-2xl font-bold">Tableau de bord - {dashboard?.tenantName || currentTenant?.name}</h1>
      </div>

      {dashboard && (
        <>
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
            <div className="p-6 bg-indigo-50 rounded-lg border border-indigo-200">
              <p className="text-sm font-medium text-indigo-700">Total Utilisateurs</p>
              <p className="text-3xl font-bold mt-2">{dashboard.totalUsers}</p>
              <p className="text-sm text-indigo-600">{dashboard.activeUsers} actifs</p>
            </div>
            <div className="p-6 bg-emerald-50 rounded-lg border border-emerald-200">
              <p className="text-sm font-medium text-emerald-700">Membres</p>
              <p className="text-3xl font-bold mt-2">{dashboard.totalMemberships}</p>
            </div>
            <div className="p-6 bg-blue-50 rounded-lg border border-blue-200">
              <p className="text-sm font-medium text-blue-700">Églises</p>
              <p className="text-3xl font-bold mt-2">{dashboard.churchCount}</p>
            </div>
            <div className="p-6 bg-purple-50 rounded-lg border border-purple-200">
              <p className="text-sm font-medium text-purple-700">Départements</p>
              <p className="text-3xl font-bold mt-2">{dashboard.departmentCount}</p>
            </div>
          </div>

          <div className="bg-white rounded-lg border p-6">
            <h3 className="text-lg font-semibold mb-4">Membres par rôle</h3>
            <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-4">
              {Object.entries(dashboard.membersByRole).map(([role, count]) => (
                <div key={role} className="p-3 bg-gray-50 rounded-lg">
                  <p className="text-sm text-gray-600">{role}</p>
                  <p className="text-2xl font-bold">{count}</p>
                </div>
              ))}
            </div>
          </div>
        </>
      )}
    </div>
  );
}
