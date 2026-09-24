import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import api from "@/lib/api";
import { Rocket } from "lucide-react";

interface PlatformMetrics {
  totalTenants: number;
  activeTenants: number;
  suspendedTenants: number;
  totalUsers: number;
  activeUsers: number;
  tenantsByPlan: Record<string, number>;
  organizations: { churches: number; departments: number };
  activity: { auditLogsLast7Days: number };
}

interface Plan {
  key: string;
  name: string;
  description: string;
  priceMonthly: number;
  priceYearly: number;
  limits: Record<string, unknown>;
  features: Record<string, unknown>;
  isActive: boolean;
}

interface FeatureFlag {
  key: string;
  name: string;
  description?: string;
  enabled: boolean;
  category: string;
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
  const navigate = useNavigate();
  const [metrics, setMetrics] = useState<PlatformMetrics | null>(null);
  const [plans, setPlans] = useState<Plan[]>([]);
  const [tenants, setTenants] = useState<Tenant[]>([]);
  const [featureFlags, setFeatureFlags] = useState<FeatureFlag[]>([]);
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState<string | null>(null);
  const [activeTab, setActiveTab] = useState<"dashboard" | "tenants" | "plans" | "features">("dashboard");
  const [saving, setSaving] = useState(false);
  const [actionError, setActionError] = useState<string | null>(null);
  // Édition d'un tenant (nom / plan / langue / fuseau)
  const [editTenant, setEditTenant] = useState<Tenant | null>(null);
  const [editForm, setEditForm] = useState({ name: "", plan: "free", locale: "fr", timezone: "Africa/Douala" });
  // Création / édition d'un plan SaaS
  const [planModal, setPlanModal] = useState<Plan | null>(null);
  const [planForm, setPlanForm] = useState({ key: "", name: "", description: "", priceMonthly: 0, priceYearly: 0, isActive: true });

  useEffect(() => {
    fetchData();
  }, []);

  const fetchData = async () => {
    try {
      const [metricsRes, plansRes, tenantsRes, flagsRes] = await Promise.all([
        api.get("/platform/admin/dashboard"),
        api.get("/platform/admin/plans"),
        api.get("/platform/admin/tenants?size=100"),
        api.get("/platform/admin/feature-flags")
      ]);

      const data = metricsRes.data as {
        tenants?: { total?: number; active?: number; suspended?: number; byPlan?: Record<string, number> };
        users?: { total?: number; active?: number };
        organizations?: { churches?: number; departments?: number };
        activity?: { auditLogsLast7Days?: number };
      };
      setMetrics({
        totalTenants: data.tenants?.total ?? 0,
        activeTenants: data.tenants?.active ?? 0,
        suspendedTenants: data.tenants?.suspended ?? 0,
        totalUsers: data.users?.total ?? 0,
        activeUsers: data.users?.active ?? 0,
        tenantsByPlan: data.tenants?.byPlan ?? {},
        organizations: {
          churches: data.organizations?.churches ?? 0,
          departments: data.organizations?.departments ?? 0,
        },
        activity: { auditLogsLast7Days: data.activity?.auditLogsLast7Days ?? 0 },
      });
      setPlans((plansRes.data as Array<Partial<Plan>>).map((plan) => ({
        key: String(plan.key ?? ''),
        name: String(plan.name ?? ''),
        description: String(plan.description ?? ''),
        priceMonthly: Number(plan.priceMonthly ?? 0),
        priceYearly: Number(plan.priceYearly ?? 0),
        limits: plan.limits ?? {},
        features: plan.features ?? {},
        isActive: plan.isActive !== false,
      })));
      setTenants((tenantsRes.data.content || []) as unknown as Tenant[]);
      setFeatureFlags(((flagsRes.data as { details?: FeatureFlag[] }).details || []).map((flag) => ({
        key: flag.key,
        name: flag.name,
        description: flag.description,
        enabled: flag.enabled,
        category: flag.category,
      })));
      setLoadError(null);
    } catch (error: any) {
      setLoadError(error?.response?.data?.error || "Impossible de charger les données de la plateforme");
    } finally {
      setLoading(false);
    }
  };

  if (loading) {
    return <div className="p-8 text-center">Chargement...</div>;
  }

  if (loadError) {
    return (
      <div className="p-8 text-center space-y-4">
        <p className="text-sm text-red-600">{loadError}</p>
        <button onClick={fetchData} className="px-4 py-2 bg-indigo-600 text-white rounded-lg">Réessayer</button>
      </div>
    );
  }

  // ===== Actions super admin (toutes câblées sur l'API réelle) =====

  const openTenantEdit = (tenant: Tenant) => {
    setEditTenant(tenant);
    setEditForm({ name: tenant.name, plan: tenant.plan, locale: tenant.locale, timezone: tenant.timezone });
    setActionError(null);
  };

  const saveTenant = async () => {
    if (!editTenant || !editForm.name.trim()) return;
    setSaving(true);
    setActionError(null);
    try {
      await api.put(`/platform/admin/tenants/${editTenant.id}`, editForm);
      setEditTenant(null);
      await fetchData();
    } catch (e: any) {
      setActionError(e?.response?.data?.error || "Mise à jour du tenant impossible");
    } finally {
      setSaving(false);
    }
  };

  const openPlanModal = (plan?: Plan) => {
    if (plan) {
      setPlanModal(plan);
      setPlanForm({
        key: plan.key, name: plan.name, description: plan.description,
        priceMonthly: plan.priceMonthly, priceYearly: plan.priceYearly, isActive: plan.isActive,
      });
    } else {
      setPlanModal({
        key: "", name: "", description: "", priceMonthly: 0, priceYearly: 0,
        limits: {}, features: {}, isActive: true,
      });
      setPlanForm({ key: "", name: "", description: "", priceMonthly: 0, priceYearly: 0, isActive: true });
    }
    setActionError(null);
  };

  const savePlan = async () => {
    if (!planForm.key.trim() || !planForm.name.trim()) {
      setActionError("Clé et nom du plan sont requis");
      return;
    }
    setSaving(true);
    setActionError(null);
    try {
      await api.post("/platform/admin/plans", planForm);
      setPlanModal(null);
      await fetchData();
    } catch (e: any) {
      setActionError(e?.response?.data?.error || "Enregistrement du plan impossible");
    } finally {
      setSaving(false);
    }
  };

  const setPlanActive = async (plan: Plan, isActive: boolean) => {
    setSaving(true);
    setActionError(null);
    try {
      await api.post("/platform/admin/plans", { key: plan.key, isActive });
      await fetchData();
    } catch (e: any) {
      setActionError(e?.response?.data?.error || "Mise à jour du plan impossible");
    } finally {
      setSaving(false);
    }
  };

  const setTenantStatus = async (tenant: Tenant, action: "suspend" | "reactivate") => {
    setSaving(true);
    setActionError(null);
    try {
      await api.post(`/platform/admin/tenants/${tenant.id}/${action}`);
      await fetchData();
    } catch (e: any) {
      setActionError(e?.response?.data?.error || "Action impossible");
    } finally {
      setSaving(false);
    }
  };

  return (
    <div className="p-6 space-y-6">
      <div className="flex flex-col sm:flex-row sm:justify-between sm:items-center gap-3">
        <h1 className="text-2xl font-bold">Administration Plateforme</h1>
        <button
          onClick={() => navigate("/platform/onboarding")}
          className="inline-flex items-center justify-center gap-2 px-4 py-2.5 rounded-xl bg-gradient-to-r from-violet-600 to-purple-600 text-white text-sm font-medium shadow-lg hover:opacity-90 transition"
        >
          <Rocket className="w-4 h-4" />
          Provisionner une organisation (guidé)
        </button>
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
        <button
          onClick={() => setActiveTab("features")}
          className={`px-4 py-2 ${activeTab === "features" ? "border-b-2 border-indigo-500" : ""}`}
        >
          Fonctionnalités
        </button>
        <button
          onClick={() => navigate("/platform/registration-requests")}
          className="px-4 py-2 text-sm font-medium text-indigo-600 hover:text-indigo-800"
        >
          Demandes d'églises
        </button>
        <button
          onClick={() => navigate("/platform/impersonation")}
          className="px-4 py-2 text-sm font-medium text-indigo-600 hover:text-indigo-800"
        >
          Impersonation
        </button>
        <button
          onClick={() => navigate("/platform/audit")}
          className="px-4 py-2 text-sm font-medium text-indigo-600 hover:text-indigo-800"
        >
          Audit plateforme
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
            <button
              onClick={() => navigate("/platform/onboarding")}
              className="px-4 py-2 bg-indigo-600 text-white rounded-lg hover:bg-indigo-700"
            >
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
                      <div className="flex items-center gap-3">
                        <button
                          onClick={() => openTenantEdit(tenant)}
                          className="text-indigo-600 hover:text-indigo-900 text-sm font-medium"
                        >
                          Editer
                        </button>
                        {tenant.status === "ACTIVE" ? (
                          <button
                            onClick={() => setTenantStatus(tenant, "suspend")}
                            disabled={saving}
                            className="text-red-600 hover:text-red-800 text-sm font-medium disabled:opacity-50"
                          >
                            Suspendre
                          </button>
                        ) : (
                          <button
                            onClick={() => setTenantStatus(tenant, "reactivate")}
                            disabled={saving}
                            className="text-emerald-600 hover:text-emerald-800 text-sm font-medium disabled:opacity-50"
                          >
                            Réactiver
                          </button>
                        )}
                      </div>
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
            <button
              onClick={() => navigate("/platform/saas/plans")}
              className="px-4 py-2 border border-indigo-200 text-indigo-700 rounded-lg hover:bg-indigo-50"
            >
              Gérer la page plans
            </button>
            <button
              onClick={() => openPlanModal()}
              className="px-4 py-2 bg-indigo-600 text-white rounded-lg hover:bg-indigo-700"
            >
              + Nouveau Plan
            </button>
          </div>
          <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
            {plans.map((plan) => (
              <div key={plan.key} className="border rounded-lg p-6">
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
                    <span className="font-medium">{Number(plan.limits.usersLimit ?? 0) === 0 ? "Illimité" : Number(plan.limits.usersLimit ?? 0)}</span>
                  </li>
                  <li className="flex justify-between">
                    <span>Eglises max</span>
                    <span className="font-medium">{Number(plan.limits.churchesLimit ?? 0) === 0 ? "Illimité" : Number(plan.limits.churchesLimit ?? 0)}</span>
                  </li>
                </ul>
                <div className="flex gap-2">
                  <button
                    onClick={() => openPlanModal(plan)}
                    className="flex-1 px-4 py-2 bg-indigo-600 text-white rounded-lg hover:bg-indigo-700 text-sm"
                  >
                    Editer
                  </button>
                  <button
                    onClick={() => setPlanActive(plan, false)}
                    disabled={saving || plan.key === 'free'}
                    title={plan.key === 'free' ? "Le plan gratuit ne peut pas être désactivé" : undefined}
                    className="px-4 py-2 border border-gray-300 rounded-lg hover:bg-gray-50 text-sm disabled:opacity-50 disabled:cursor-not-allowed"
                  >
                    Désactiver
                  </button>
                </div>
              </div>
            ))}
          </div>
        </div>
      )}

      {activeTab === "features" && (
        <div className="space-y-4">
          <div>
            <h2 className="text-xl font-semibold">Fonctionnalités plateforme</h2>
            <p className="text-sm text-gray-500">Ces interrupteurs activent ou désactivent des capacités globales.</p>
          </div>
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            {featureFlags.map((flag) => (
              <div key={flag.key} className="border rounded-lg p-4 flex items-center justify-between gap-4">
                <div className="min-w-0">
                  <p className="font-semibold text-sm">{flag.name}</p>
                  <p className="text-xs text-gray-500 mt-1">{flag.description || flag.key}</p>
                  <p className="text-[10px] uppercase tracking-wide text-gray-400 mt-2">{flag.category}</p>
                </div>
                <button
                  type="button"
                  role="switch"
                  aria-checked={flag.enabled}
                  disabled={saving}
                  onClick={() => {
                    setSaving(true);
                    setActionError(null);
                    api.put(`/platform/admin/feature-flags/${encodeURIComponent(flag.key)}`, { enabled: !flag.enabled })
                      .then(fetchData)
                      .catch((error: any) => setActionError(error?.response?.data?.error || 'Mise à jour impossible'))
                      .finally(() => setSaving(false));
                  }}
                  className={`relative h-6 w-11 rounded-full transition ${flag.enabled ? 'bg-emerald-500' : 'bg-gray-300'}`}
                >
                  <span className={`absolute top-1 h-4 w-4 rounded-full bg-white transition ${flag.enabled ? 'right-1' : 'left-1'}`} />
                </button>
              </div>
            ))}
          </div>
        </div>
      )}

      {/* Erreur d'action */}
      {actionError && (
        <div role="alert" className="rounded-lg bg-red-50 border border-red-200 text-red-700 px-4 py-3 text-sm">
          {actionError}
        </div>
      )}

      {/* Modale — édition d'un tenant */}
      {editTenant && (
        <div role="dialog" aria-modal="true" aria-label="Modifier l'organisation"
          className="fixed inset-0 z-50 flex items-center justify-center bg-black/40 p-4">
          <div className="w-full max-w-md bg-white rounded-2xl shadow-xl p-6 space-y-4">
            <h3 className="text-lg font-semibold">Modifier « {editTenant.name} »</h3>
            <div>
              <label className="block text-xs font-medium text-gray-500 mb-1">Nom</label>
              <input className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm"
                value={editForm.name} onChange={(e) => setEditForm((f) => ({ ...f, name: e.target.value }))} />
            </div>
            <div>
              <label className="block text-xs font-medium text-gray-500 mb-1">Plan</label>
              <select className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm"
                value={editForm.plan} onChange={(e) => setEditForm((f) => ({ ...f, plan: e.target.value }))}>
                {plans.map((p) => (
                  <option key={p.key} value={p.key}>{p.name} ({p.key})</option>
                ))}
              </select>
            </div>
            <div className="grid grid-cols-2 gap-3">
              <div>
                <label className="block text-xs font-medium text-gray-500 mb-1">Langue</label>
                <select className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm"
                  value={editForm.locale} onChange={(e) => setEditForm((f) => ({ ...f, locale: e.target.value }))}>
                  <option value="fr">Français</option>
                  <option value="en">English</option>
                  <option value="pt">Português</option>
                  <option value="es">Español</option>
                </select>
              </div>
              <div>
                <label className="block text-xs font-medium text-gray-500 mb-1">Fuseau horaire</label>
                <input className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm"
                  value={editForm.timezone} onChange={(e) => setEditForm((f) => ({ ...f, timezone: e.target.value }))} />
              </div>
            </div>
            <div className="flex justify-end gap-2 pt-2">
              <button onClick={() => setEditTenant(null)} className="px-4 py-2 text-sm border border-gray-300 rounded-lg hover:bg-gray-50">
                Annuler
              </button>
              <button onClick={saveTenant} disabled={saving}
                className="px-4 py-2 text-sm bg-indigo-600 text-white rounded-lg hover:bg-indigo-700 disabled:opacity-50">
                {saving ? 'Enregistrement...' : 'Enregistrer'}
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Modale — création / édition d'un plan SaaS */}
      {planModal && (
        <div role="dialog" aria-modal="true" aria-label="Plan SaaS"
          className="fixed inset-0 z-50 flex items-center justify-center bg-black/40 p-4">
          <div className="w-full max-w-md bg-white rounded-2xl shadow-xl p-6 space-y-4">
            <h3 className="text-lg font-semibold">{planModal.key ? `Modifier le plan ${planModal.key}` : 'Nouveau plan'}</h3>
            <div>
              <label className="block text-xs font-medium text-gray-500 mb-1">Clé *</label>
              <input disabled={!!planModal.key} className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm disabled:bg-gray-100"
                value={planForm.key} onChange={(e) => setPlanForm((f) => ({ ...f, key: e.target.value.toLowerCase() }))} placeholder="starter" />
            </div>
            <div>
              <label className="block text-xs font-medium text-gray-500 mb-1">Nom *</label>
              <input className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm"
                value={planForm.name} onChange={(e) => setPlanForm((f) => ({ ...f, name: e.target.value }))} placeholder="Starter" />
            </div>
            <div>
              <label className="block text-xs font-medium text-gray-500 mb-1">Description</label>
              <input className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm"
                value={planForm.description} onChange={(e) => setPlanForm((f) => ({ ...f, description: e.target.value }))} />
            </div>
            <div className="grid grid-cols-2 gap-3">
              <div>
                <label className="block text-xs font-medium text-gray-500 mb-1">Prix / mois</label>
                <input type="number" min={0} className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm"
                  value={planForm.priceMonthly} onChange={(e) => setPlanForm((f) => ({ ...f, priceMonthly: Number(e.target.value) }))} />
              </div>
              <div>
                <label className="block text-xs font-medium text-gray-500 mb-1">Prix / an</label>
                <input type="number" min={0} className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm"
                  value={planForm.priceYearly} onChange={(e) => setPlanForm((f) => ({ ...f, priceYearly: Number(e.target.value) }))} />
              </div>
            </div>
            <div className="flex justify-end gap-2 pt-2">
              <button onClick={() => setPlanModal(null)} className="px-4 py-2 text-sm border border-gray-300 rounded-lg hover:bg-gray-50">
                Annuler
              </button>
              <button onClick={savePlan} disabled={saving}
                className="px-4 py-2 text-sm bg-indigo-600 text-white rounded-lg hover:bg-indigo-700 disabled:opacity-50">
                {saving ? 'Enregistrement...' : 'Enregistrer'}
              </button>
            </div>
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