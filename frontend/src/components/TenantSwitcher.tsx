import { useAuth } from "@/contexts/AuthContext";
import api from "@/lib/api";
import type { Tenant } from "@/types/tenant";
import { useState, useEffect, useRef, useCallback } from "react";

export default function TenantSwitcher() {
  const { user } = useAuth();
  const [isOpen, setIsOpen] = useState(false);
  const [tenants, setTenants] = useState<Tenant[]>([]);
  const [loading, setLoading] = useState(false);
  const [currentTenant, setCurrentTenant] = useState<Tenant | null>(null);
  const dropdownRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    fetchTenants();
  }, []);

  const fetchTenants = async () => {
    try {
      const res = await api.get("/tenant-switcher/my-tenants");
      setTenants(res.data);
      if (res.data.length > 0) {
        setCurrentTenant(res.data[0]);
      }
    } catch (error) {
      console.error("Erreur:", error);
    }
  };

  const switchTenant = useCallback(async (tenant: Tenant) => {
    try {
      await api.post("/tenant-switcher/switch", { tenantId: tenant.id });
      setCurrentTenant(tenant);
      setIsOpen(false);
      window.location.reload();
    } catch (error) {
      console.error("Erreur:", error);
    }
    });

  useEffect(() => {
    function handleClickOutside(event: MouseEvent) {
      if (dropdownRef.current && !dropdownRef.current.contains(event.target as Node)) {
        setIsOpen(false);
      }
    }
    document.addEventListener("mousedown", handleClickOutside);
    return () => document.removeEventListener("mousedown", handleClickOutside);
  }, []);

  if (tenants.length === 0) {
    return null;
  }

  return (
    <div className="relative" ref={dropdownRef}>
      <button
        onClick={() => setIsOpen(!isOpen)}
        className="flex items-center gap-2 px-4 py-2 bg-white border rounded-lg hover:bg-gray-50"
      >
        <svg className="w-5 h-5 text-gray-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 21V5a2 2 0 00-2-2H7a2 2 0 00-2 2v16m14 0h2m-2 0h-5m-9 0H3m2 0h5M9 7h1m-1 4h1m4-4h1m-1 4h1m-5 10v-5a1 1 0 011-1h2a1 1 0 011 1v5m-4 0h4" />
        </svg>
        <div className="text-left">
          <p className="font-medium text-gray-900">{currentTenant?.name || "Organisation"}</p>
          <p className="text-xs text-gray-500">{currentTenant?.slug}</p>
        </div>
        <svg className="w-4 h-4 text-gray-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 9l-7 7-7-7" />
        </svg>
      </button>

      {isOpen && (
        <div className="absolute right-0 mt-2 w-64 bg-white border rounded-lg shadow-lg z-50">
          <div className="p-3 border-b">
            <p className="text-sm font-medium text-gray-500">Changer d'organisation</p>
          </div>
          <div className="max-h-60 overflow-y-auto">
            {tenants.map((tenant) => (
              <button
                key={tenant.id}
                onClick={() => switchTenant(tenant)}
                className={"w-full px-4 py-3 text-left hover:bg-gray-50 " +
                  (tenant.id === currentTenant?.id ? "bg-indigo-50" : "")
                }
              >
                <div className="flex items-center justify-between">
                  <div>
                    <p className="font-medium">{tenant.name}</p>
                    <p className="text-sm text-gray-500">{tenant.slug}</p>
                  </div>
                  {tenant.id === currentTenant?.id && (
                    <svg className="w-5 h-5 text-indigo-600" fill="currentColor" viewBox="0 0 20 20">
                      <path fillRule="evenodd" d="M16.707 5.293a1 1 0 010 1.414l-8 8a1 1 0 01-1.414 0l-4-4a1 1 0 011.414-1.414L8 12.586l7.293-7.293a1 1 0 011.414 0z" clipRule="evenodd" />
                    </svg>
                  )}
                </div>
              </button>
            ))}
          </div>
        </div>
      )}
    </div>
  );
}
