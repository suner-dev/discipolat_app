import { createContext, useContext, useState, useEffect, useCallback, type ReactNode } from 'react';
import api from '@/lib/api';
import type { Tenant, TenantMembership, OrganizationNode, Permission, Role } from '@/types/tenant';

interface TenantContextType {
  // État du tenant
  currentTenant: Tenant | null;
  currentMembership: TenantMembership | null;
  currentOrganizationNode: OrganizationNode | null;
  availableTenants: Tenant[];
  roles: Role[];
  permissions: Permission[];
  branding: Record<string, any> | null;
  
  // Actions
  switchTenant: (tenantId: string) => Promise<void>;
  switchOrganization: (orgNodeId: string) => Promise<void>;
  refreshContext: () => Promise<void>;
  
  // Vérifications
  hasRole: (role: string) => boolean;
  hasPermission: (permission: string) => boolean;
  hasFeature: (feature: string) => boolean;
  canAccess: (resource: string, action: string) => boolean;
  
  // État de chargement
  isLoading: boolean;
  isInitialized: boolean;
}

const TenantContext = createContext<TenantContextType | null>(null);

export function TenantProvider({ children }: { children: ReactNode }) {
  const [currentTenant, setCurrentTenant] = useState<Tenant | null>(null);
  const [currentMembership, setCurrentMembership] = useState<TenantMembership | null>(null);
  const [currentOrganizationNode, setCurrentOrganizationNode] = useState<OrganizationNode | null>(null);
  const [availableTenants, setAvailableTenants] = useState<Tenant[]>([]);
  const [roles, setRoles] = useState<Role[]>([]);
  const [permissions, setPermissions] = useState<Permission[]>([]);
  const [branding, setBranding] = useState<Record<string, any> | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [isInitialized, setIsInitialized] = useState(false);

  const refreshContext = useCallback(async () => {
    try {
      setIsLoading(true);
      
      // Charger le contexte courant
      const contextRes = await api.get('/tenant-switcher/context').catch(() => null);
      if (contextRes?.data?.tenantId) {
        const ctx = contextRes.data;
        
        // Charger les détails du tenant
        const tenantRes = await api.get(`/tenants/${ctx.tenantId}`).catch(() => null);
        if (tenantRes?.data) {
          setCurrentTenant(tenantRes.data);
        }
        
        // Charger le branding
        const brandingRes = await api.get('/platform/branding').catch(() => null);
        if (brandingRes?.data) {
          setBranding(brandingRes.data);
        }
        
        // Charger les rôles et permissions
        const rolesRes = await api.get('/admin/roles').catch(() => null);
        if (rolesRes?.data) {
          setRoles(rolesRes.data);
        }
        
        const permsRes = await api.get('/admin/roles/permissions').catch(() => null);
        if (permsRes?.data) {
          setPermissions(permsRes.data);
        }
      }
      
      // Charger les tenants disponibles
      const tenantsRes = await api.get('/tenant-switcher/my-tenants').catch(() => null);
      if (tenantsRes?.data) {
        setAvailableTenants(tenantsRes.data);
      }
      
    } catch (error) {
      console.error('Erreur chargement contexte tenant:', error);
    } finally {
      setIsLoading(false);
      setIsInitialized(true);
    }
  }, []);

  useEffect(() => {
    refreshContext();
  }, [refreshContext]);

  const switchTenant = useCallback(async (tenantId: string) => {
    try {
      await api.post('/tenant-switcher/switch', { tenantId });
      await refreshContext();
      window.location.reload();
    } catch (error) {
      console.error('Erreur changement tenant:', error);
      throw error;
    }
  }, [refreshContext]);

  const switchOrganization = useCallback(async (orgNodeId: string) => {
    try {
      await api.post('/tenant-switcher/switch-org', { organizationId: orgNodeId });
      await refreshContext();
    } catch (error) {
      console.error('Erreur changement organisation:', error);
      throw error;
    }
  }, [refreshContext]);

  const hasRole = useCallback((role: string): boolean => {
    if (!currentMembership) return false;
    return currentMembership.role.toUpperCase() === role.toUpperCase() ||
      roles.some(r => r.key.toUpperCase() === role.toUpperCase());
  }, [currentMembership, roles]);

  const hasPermission = useCallback((permission: string): boolean => {
    return permissions.some(p => p.key.toUpperCase() === permission.toUpperCase());
  }, [permissions]);

  const hasFeature = useCallback((feature: string): boolean => {
    if (!currentTenant) return false;
    // Vérifier dans les feature flags du tenant
    return true; // Simplifié - à implémenter avec les feature flags
  }, [currentTenant]);

  const canAccess = useCallback((resource: string, action: string): boolean => {
    const permissionKey = `${resource.toUpperCase()}_${action.toUpperCase()}`;
    return hasPermission(permissionKey);
  }, [hasPermission]);

  return (
    <TenantContext.Provider value={{
      currentTenant,
      currentMembership,
      currentOrganizationNode,
      availableTenants,
      roles,
      permissions,
      branding,
      switchTenant,
      switchOrganization,
      refreshContext,
      hasRole,
      hasPermission,
      hasFeature,
      canAccess,
      isLoading,
      isInitialized,
    }}>
      {children}
    </TenantContext.Provider>
  );
}

export function useTenant() {
  const context = useContext(TenantContext);
  if (!context) {
    throw new Error('useTenant must be used within a TenantProvider');
  }
  return context;
}
