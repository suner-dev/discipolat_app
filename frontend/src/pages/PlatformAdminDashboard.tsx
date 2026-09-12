import { useEffect, useState } from "react";
import { useTenant } from "@/contexts/TenantContext";
import api from "@/lib/api";
import type { UserRole } from "@/types";

interface PlatformMetrics {
  totalTenants: number;
  activeTenants: number;
  suspendedTenants: number;
  totalUsers: number;
  activeUsers: number;
  tenantsByPlan: Record<string, number>;
}

interface Plan {
  id: string;
  key: string;
  name: string;
  priceMonthly: number;
  priceYearly: number;
  usersLimit: number;
  churchesLimit: number;
}

interface Tenant {
  id: string;
  name: string;
  slug: string;
  status: string;
  plan: string;
  country: string;
  currency: string;
  timezone: string;
  locale: string;
  createdAt: string;
  updatedAt: string;
}

export default function PlatformAdminDashboard() {
  const { hasPermission } = useTenant();
  const [metrics, setMetrics] = useState<PlatformMetrics | null>(null);
  const [plans, setPlans] = useState<Plan[]>([]);
  const [tenants, setTenants] = useState<Tenant[]>([]);
  const [loading, setLoading] = useState(true);
  const [activeTab, setActiveTab] = useState<"dashboard" | "tenants" | "plans">("dashboard");

  useEffect(() => {
    if (!hasPermission("TENANT_VIEW")) {
      return;
    }
    fetchData();
  }, [hasPermission]);

  const fetchData = async () => {
    try {
      const [metricsRes, plansRes, tenantsRes] = await Promise.all([
        api.get("/platform/admin/dashboard").catch(() => ({ data: {} })),
        api.get("/platform/admin/plans").catch(() => ({ data: [] })),
        api.get("/platform/admin/tenants?size=100").catch(() => ({ data: { content: [] } }))
      ]);

      setMetrics(metricsRes.data as unknown as PlatformMetrics);
      setPlans(plansRes.data as unknown as Plan[]);
      setTenants((tenantsRes.data.content || []) as unknown as Tenant[]);
    } catch (error) {
      console.error("Error fetching admin data:", error);
    } finally {
      setLoading(false);
    }
  };

  if (loading) {
    return <div className="p-8 text-center">Chargement...</div>;
  }

  return (
    <div className="p-6 space-y-6">
      <div className="flex justify-between items-center">
        <h1 className="text-2xl font-bold">Administration Plateforme</h1>
      </div>

      <div className="flex gap-4 border-b">
        <button
          onClick={() => setActiveTab("dashboard")}
          className={`px-4 py-2 ${activeTab === "dashboard" ? "border-b-2 border-indigo-500" : ""}`}
        >
          Dashboard
        </button>
        <button
          onClick={() => setActiveTab("tenants")}
          className={`px-4 py-2 ${activeTab === "tenants" ? "border-b-2 border-indigo-500" : ""}`}
        >
          Tenants ({tenants.length})
        </button>
        <button
          onClick={() => setActiveTab("plans")}
          className={`px-4 py-2 ${activeTab === "plans" ? "border-b-2 border-indigo-500" : ""}`}
        >
          Plans
        </button>
      </div>

      {activeTab === "dashboard" && metrics && (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
          <MetricCard
            title="Total Tenants"
            value={metrics.totalTenants}
            subtitle={`${metrics.activeTenants} actifs`}
            variant="indigo"
          />
          <MetricCard
            title="Utilisateurs"
            value={metrics.totalUsers}
            subtitle={`${metrics.activeUsers} actifs`}
            variant="emerald"
          />
          <MetricCard
            title="Tenants Actifs"
            value={metrics.activeTenants}
            subtitle="100%"
            variant="blue"
          />
          <MetricCard
            title="Tenants Suspendus"
            value={metrics.suspendedTenants}
            subtitle="À surveiller"
            variant="red"
          />
        </div>
      )}

      {activeTab === "tenants" && (
        <div className="space-y-4">
          <div className="flex justify-between items-center">
            <h2 className="text-xl font-semibold">Tenants ({tenants.length})</h2>
            <button className="px-4 py-2 bg-indigo-600 text-white rounded-lg hover:bg-indigo-700">
              + Nouveau Tenant
            </button>
          </div>
          <div className="overflow-x-auto">
            <table className="min-w-full divide-y divide-gray-200">
              <thead className="bg-gray-50">
                <tr>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Nom</th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Slug</th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Plan</th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Statut</th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Pays</th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-200">
                {tenants.map((tenant) => (
                  <tr key={tenant.id} className="hover:bg-gray-50">
                    <td className="px-6 py-4">{tenant.name}</td>
                    <td className="px-6 py-4 text-sm text-gray-500">{tenant.slug}</td>
                    <td className="px-6 py-4">
                      <span className={"px-2 py-1 text-xs rounded-full " +
                        (tenant.plan === "free" ? "bg-gray-100 text-gray-700" :
                         tenant.plan === "starter" ? "bg-blue-100 text-blue-700" :
                         tenant.plan === "professional" ? "bg-indigo-100 text-indigo-700" :
                         "bg-purple-100 text-purple-700")}
                      >
                        {tenant.plan}
                      </span>
                    </td>
                    <td className="px-6 py-4">
                      <span className={"px-2 py-1 text-xs rounded-full " +
                        (tenant.status === "ACTIVE" ? "bg-emerald-100 text-emerald-700" :
                         tenant.status === "SUSPENDED" ? "bg-red-100 text-red-700" :
                         "bg-gray-100 text-gray-700")}
                      >
                        {tenant.status}
                      </span>
                    </td>
                    <td className="px-6 py-4 text-sm text-gray-500">{tenant.country}</td>
                    <td className="px-6 py-4">
                      <button className="text-indigo-600 hover:text-indigo-900 text-sm font-medium">
                        Editer
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {activeTab === "plans" && (
        <div className="space-y-4">
          <div className="flex justify-between items-center">
            <h2 className="text-xl font-semibold">Plans SaaS</h2>
            <button className="px-4 py-2 bg-indigo-600 text-white rounded-lg hover:bg-indigo-700">
              + Nouveau Plan
            </button>
          </div>
          <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
            {plans.map((plan) => (
              <div key={plan.id} className="border rounded-lg p-6">
                <div className="flex justify-between items-start mb-4">
                  <div>
                    <h3 className="text-xl font-bold">{plan.name}</h3>
                    <p className="text-3xl font-bold text-indigo-600">
                      {plan.priceMonthly > 0 ? plan.priceMonthly + "€/mois" : "Gratuit"}
                    </p>
                  </div>
                  <span className={"px-3 py-1 text-xs rounded-full " +
                    (plan.key === "free" ? "bg-gray-100 text-gray-700" :
                     plan.key === "starter" ? "bg-blue-100 text-blue-700" :
                     plan.key === "professional" ? "bg-indigo-100 text-indigo-700" :
                     "bg-purple-100 text-purple-700")}
                  >
                    {plan.key.toUpperCase()}
                  </span>
                </div>
                <ul className="space-y-2 mb-4">
                  <li className="flex justify-between">
                    <span>Utilisateurs max</span>
                    <span className="font-medium">{plan.usersLimit === 0 ? "Illimité" : plan.usersLimit}</span>
                  </li>
                  <li className="flex justify-between">
                    <span>Eglises max</span>
                    <span className="font-medium">{plan.churchesLimit === 0 ? "Illimité" : plan.churchesLimit}</span>
                  </li>
                </ul>
                <div className="flex gap-2">
                  <button className="flex-1 px-4 py-2 bg-indigo-600 text-white rounded-lg hover:bg-indigo-700 text-sm">
                    Editer
                  </button>
                  <button className="px-4 py-2 border border-gray-300 rounded-lg hover:bg-gray-50 text-sm">
                    Désactiver
                  </button>
                </div>
              </div>
            ))}
          </div>
        </div>
      )}
    </div>
  );
}

function MetricCard({ title, value, subtitle, variant }: {
  title: string;
  value: number;
  subtitle: string;
  variant: string;
}) {
  const colors: Record<string, string> = {
    indigo: "bg-indigo-50 border-indigo-200 text-indigo-800",
    emerald: "bg-emerald-50 border-emerald-200 text-emerald-800",
    blue: "bg-blue-50 border-blue-200 text-blue-800",
    red: "bg-red-50 border-red-200 text-red-800",
  };

  return (
    <div className={"p-6 rounded-lg border " + colors[variant]}>
      <p className="text-sm font-medium opacity-75">{title}</p>
      <p className="text-3xl font-bold mt-2">{value}</p>
      <p className="text-sm opacity-75 mt-1">{subtitle}</p>
    </div>
  );
}