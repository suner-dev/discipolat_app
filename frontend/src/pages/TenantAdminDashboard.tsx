import { useCallback, useEffect, useState } from "react";
import { QuotaUsagePanel } from "@/components/admin/QuotaUsageCards";
import api, { getErrorMessage } from "@/lib/api";

interface TenantDashboard {
  tenantId: string;
  tenantName: string;
  plan: string | null;
  users: {
    total: number;
    active: number;
    inactive: number;
    totalMemberships: number;
    activeMemberships: number;
    pendingMemberships: number;
    membersByRole: Record<string, number>;
  };
  organizations: {
    churches: number;
    campuses: number;
    subChurches: number;
    departments: number;
    groups: number;
  };
  subscription: {
    planKey: string;
    status: string;
    billingCycle: string;
    currentPeriodEnd: string;
    plan: { name: string } | null;
  } | null;
  quotas: Record<string, unknown>;
  activity: {
    auditLogsLast7Days: number;
  };
}

export default function TenantAdminDashboardPage() {
  const [dashboard, setDashboard] = useState<TenantDashboard | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const fetchDashboard = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const response = await api.get<TenantDashboard>("/admin/dashboard/overview");
      setDashboard(response.data);
    } catch (requestError) {
      setDashboard(null);
      setError(getErrorMessage(requestError));
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    let active = true;
    setLoading(true);
    setError(null);
    api.get<TenantDashboard>("/admin/dashboard/overview")
      .then((response) => {
        if (active) setDashboard(response.data);
      })
      .catch((requestError) => {
        if (!active) return;
        setDashboard(null);
        setError(getErrorMessage(requestError));
      })
      .finally(() => {
        if (active) setLoading(false);
      });
    return () => {
      active = false;
    };
  }, []);

  if (loading) {
    return <div className="p-8 text-center" role="status">Chargement du tableau de bord…</div>;
  }

  if (error) {
    return (
      <div className="space-y-4 p-8 text-center">
        <p className="text-sm text-red-600" role="alert">{error}</p>
        <button type="button" onClick={() => { void fetchDashboard(); }} className="rounded-lg bg-indigo-600 px-4 py-2 text-white">
          Réessayer
        </button>
      </div>
    );
  }

  if (!dashboard) {
    return (
      <div className="space-y-4 p-8 text-center">
        <p className="text-sm text-gray-600" role="alert">Aucune donnée de tableau de bord disponible.</p>
        <button type="button" onClick={() => { void fetchDashboard(); }} className="rounded-lg bg-indigo-600 px-4 py-2 text-white">
          Réessayer
        </button>
      </div>
    );
  }

  const organizationMetrics = [
    { label: "Églises", value: dashboard.organizations.churches },
    { label: "Sous-églises", value: dashboard.organizations.subChurches },
    { label: "Campus", value: dashboard.organizations.campuses },
    { label: "Départements", value: dashboard.organizations.departments },
    { label: "Groupes", value: dashboard.organizations.groups },
  ];

  return (
    <div className="space-y-6 p-6">
      <div className="flex flex-col gap-2 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <h1 className="text-2xl font-bold">Tableau de bord — {dashboard.tenantName}</h1>
          <p className="text-sm text-gray-500">Plan : {dashboard.plan || dashboard.subscription?.planKey || "Indisponible"}</p>
        </div>
        {dashboard.subscription && (
          <div className="rounded-lg border border-gray-200 bg-white px-4 py-3 text-sm">
            <p className="font-medium">{dashboard.subscription.plan?.name || dashboard.subscription.planKey}</p>
            <p className="text-gray-500">Abonnement {dashboard.subscription.status.toLowerCase()}</p>
          </div>
        )}
      </div>

      <section aria-labelledby="tenant-users-title" className="space-y-4">
        <h2 id="tenant-users-title" className="text-lg font-semibold">Utilisateurs et membres</h2>
        <div className="grid grid-cols-1 gap-4 md:grid-cols-2 lg:grid-cols-4">
          <MetricCard title="Utilisateurs" value={dashboard.users.total} subtitle={`${dashboard.users.active} actifs · ${dashboard.users.inactive} inactifs`} color="indigo" />
          <MetricCard title="Adhésions" value={dashboard.users.totalMemberships} subtitle={`${dashboard.users.activeMemberships} actives`} color="emerald" />
          <MetricCard title="Adhésions en attente" value={dashboard.users.pendingMemberships} subtitle="Validation requise" color="amber" />
          <MetricCard title="Activité sur 7 jours" value={dashboard.activity.auditLogsLast7Days} subtitle="Entrées d’audit" color="blue" />
        </div>
        <div className="rounded-lg border border-gray-200 bg-white p-5">
          <h3 className="mb-4 font-semibold">Membres actifs par rôle</h3>
          {Object.keys(dashboard.users.membersByRole).length > 0 ? (
            <div className="grid grid-cols-2 gap-3 md:grid-cols-3 lg:grid-cols-4">
              {Object.entries(dashboard.users.membersByRole).map(([role, count]) => (
                <div key={role} className="rounded-lg bg-gray-50 p-3">
                  <p className="text-sm text-gray-600">{role}</p>
                  <p className="text-2xl font-bold">{count}</p>
                </div>
              ))}
            </div>
          ) : (
            <p className="text-sm text-gray-500">Aucun membre actif.</p>
          )}
        </div>
      </section>

      <section aria-labelledby="tenant-organizations-title" className="space-y-4">
        <h2 id="tenant-organizations-title" className="text-lg font-semibold">Organisation</h2>
        <div className="grid grid-cols-2 gap-4 md:grid-cols-3 lg:grid-cols-5">
          {organizationMetrics.map((metric) => (
            <MetricCard key={metric.label} title={metric.label} value={metric.value} subtitle="Sur le tenant courant" color="slate" />
          ))}
        </div>
      </section>

      <QuotaUsagePanel />
    </div>
  );
}

interface MetricCardProps {
  title: string;
  value: number;
  subtitle: string;
  color: "indigo" | "emerald" | "amber" | "blue" | "slate";
}

function MetricCard({ title, value, subtitle, color }: MetricCardProps) {
  const colors = {
    indigo: "border-indigo-200 bg-indigo-50 text-indigo-800",
    emerald: "border-emerald-200 bg-emerald-50 text-emerald-800",
    amber: "border-amber-200 bg-amber-50 text-amber-800",
    blue: "border-blue-200 bg-blue-50 text-blue-800",
    slate: "border-gray-200 bg-gray-50 text-gray-800",
  };

  return (
    <article className={`rounded-lg border p-5 ${colors[color]}`}>
      <p className="text-sm font-medium opacity-75">{title}</p>
      <p className="mt-2 text-3xl font-bold">{value}</p>
      <p className="mt-1 text-sm opacity-75">{subtitle}</p>
    </article>
  );
}
