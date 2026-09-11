import { useEffect, useState } from "react";
import { useAuth } from "@/contexts/AuthContext";
import api from "@/lib/api";
import type { Tenant } from "@/types/tenant";

export default function TenantSwitcherPage() {
  const { user, updateUser } = useAuth();
  const [tenants, setTenants] = useState<Tenant[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => { fetchTenants(); }, []);

  const fetchTenants = async () => {
    try {
      const res = await api.get("/tenant-switcher/my-tenants");
      setTenants(res.data);
    } catch (err) {
      console.error("Erreur:", err);
      setError("Impossible de charger vos organisations");
    } finally {
      setLoading(false);
    }
  };

  const switchTenant = async (tenant: Tenant) => {
    try {
      await api.post("/tenant-switcher/switch", { tenantId: tenant.id });
      updateUser({ tenantId: tenant.id, role: tenant.role });
      window.location.href = "/";
    } catch (err) {
      console.error("Erreur:", err);
      alert("Impossible de changer d'organisation");
    }
  };

  if (loading) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-gray-50">
        <div className="text-center">
          <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-indigo-600 mx-auto"></div>
          <p className="mt-4 text-gray-600">Chargement de vos organisations...</p>
        </div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-gray-50">
        <div className="text-center">
          <p className="text-red-600 mb-4">{error}</p>
          <button onClick={() => window.location.href = "/auth/logout"}
            className="px-4 py-2 bg-red-600 text-white rounded-lg">
            Deconnexion
          </button>
        </div>
      </div>
    );
  }

  if (tenants.length === 0) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-gray-50">
        <div className="text-center">
          <h1 className="text-2xl font-bold text-gray-900 mb-2">Aucune organisation</h1>
          <p className="text-gray-600 mb-6">Vous n'appartenez a aucune organisation.</p>
          <button onClick={() => window.location.href = "/auth/logout"}
            className="px-4 py-2 bg-indigo-600 text-white rounded-lg hover:bg-indigo-700">
            Se deconnecter
          </button>
        </div>
      </div>
    );
  }

  if (tenants.length === 1) {
    const t = tenants[0];
    return (
      <div className="min-h-screen flex items-center justify-center bg-gray-50">
        <div className="text-center">
          <div className="inline-flex items-center justify-center w-16 h-16 bg-indigo-100 rounded-full mb-4">
            <svg className="w-8 h-8 text-indigo-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 21V5a2 2 0 00-2-2H7a2 2 0 00-2 2v16m14 0h2m-2 0h-5m-9 0H3m2 0h5M9 7h1m-1 4h1m4-4h1m-1 4h1m-1 4h1m-1 4h1" />
            </svg>
          </div>
          <h1 className="text-2xl font-bold text-gray-900 mb-2">Bienvenue !</h1>
          <p className="text-gray-600 mb-2">Vous etes connecte a <span className="font-semibold">{t.name}</span></p>
          <p className="text-sm text-gray-500 mb-6">Role: {t.role}</p>
          <button onClick={() => switchTenant(t)}
            className="px-6 py-3 bg-indigo-600 text-white rounded-lg hover:bg-indigo-700">
            Continuer
          </button>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gray-50">
      <header className="bg-white shadow-sm">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-4">
          <h1 className="text-2xl font-bold text-gray-900">Choisir une organisation</h1>
          <p className="text-gray-600 mt-1">Selectionnez l'organisation avec laquelle travailler</p>
        </div>
      </header>
      <main className="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
          {tenants.map((tenant) => (
            <div key={tenant.id} onClick={() => switchTenant(tenant)}
              className={"cursor-pointer bg-white rounded-xl border-2 p-6 transition-all " +
                (tenant.id === user?.tenantId
                  ? "border-indigo-500 ring-2 ring-indigo-200"
                  : "border-gray-200 hover:border-indigo-300 hover:shadow-md")}>
              <div className="flex items-start justify-between">
                <div className="flex-1">
                  <div className="flex items-center gap-3 mb-2">
                    <div className={"w-10 h-10 rounded-lg flex items-center justify-center font-bold text-lg " +
                      (tenant.id === user?.tenantId ? "bg-indigo-600 text-white" : "bg-gray-100 text-gray-600")}>
                      {tenant.name.charAt(0).toUpperCase()}
                    </div>
                    <div>
                      <h3 className="text-lg font-semibold text-gray-900">{tenant.name}</h3>
                      <p className="text-sm text-gray-500">{tenant.slug}</p>
                    </div>
                  </div>
                  <div className="mt-3 flex items-center gap-2">
                    <span className={"px-2 py-1 text-xs rounded-full " +
                      (tenant.id === user?.tenantId ? "bg-indigo-100 text-indigo-700" : "bg-gray-100 text-gray-600")}>
                      {tenant.role}
                    </span>
                    <span className="px-2 py-1 text-xs rounded-full bg-blue-100 text-blue-700">{tenant.plan}</span>
                  </div>
                </div>
              </div>
            </div>
          ))}
        </div>
      </main>
    </div>
  );
}
