import { createContext, useContext, useState, useEffect, useCallback, type ReactNode } from 'react';
import api from '@/lib/api';
import type { 
  Tenant, 
  TenantMembership, 
  OrganizationNode, 
  Permission, 
  Role,
  Subscription,
  Quotas,
  BrandingConfig,
  TenantSettings,
  TenantContextValue,
  SwitchTenantResponse
} from '@/types/tenant';

const TenantContext = createContext<TenantContextValue | null>(null);

export function TenantProvider({ children }: { children: ReactNode }) {
  const [currentTenant, setCurrentTenant] = useState<Tenant | null>(null);
  const [currentMembership, setCurrentMembership] = useState<TenantMembership | null>(null);
  const [currentOrganizationNode, setCurrentOrganizationNode] = useState<OrganizationNode | null>(null);
  const [availableTenants, setAvailableTenants] = useState<Tenant[]>([]);
  const [roles, setRoles] = useState<Role[]>([]);
  const [permissions, setPermissions] = useState<Permission[]>([]);
  const [subscription, setSubscription] = useState<Subscription | null>(null);
  const [quotas, setQuotas] = useState<Quotas | null>(null);
  const [branding, setBranding] = useState<BrandingConfig | null>(null);
  const [features, setFeatures] = useState<Record<string, boolean>>({});
  const [settings, setSettings] = useState<TenantSettings | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [isInitialized, setIsInitialized] = useState(false);

  const refreshContext = useCallback(async () => {
    try {
      setIsLoading(true);
      
      const contextRes = await api.get('/tenant-switcher/context').catch(() => null);
      
      if (contextRes?.data?.requiresSelection) {
        // User has multiple tenants - show selection
        setAvailableTenants(contextRes.data.availableTenants || []);
        setCurrentTenant(null);
        setCurrentMembership(null);
        setCurrentOrganizationNode(null);
        setSubscription(null);
        setQuotas(null);
        setBranding(null);
        setFeatures({});
        setSettings(null);
      } else if (contextRes?.data?.tenantId) {
        // Single tenant context - populate all data
        const ctx = contextRes.data;
        
        setCurrentTenant({
          id: ctx.tenantId,
          name: ctx.tenantName,
          slug: ctx.tenantSlug,
          status: ctx.tenantStatus,
          plan: ctx.plan,
          country: '', currency: '', timezone: '', locale: '',
          createdAt: '', updatedAt: '',
          branding: ctx.branding,
          features: ctx.features,
          settings: ctx.settings
        });

        setCurrentMembership({
          id: '',
          tenantId: ctx.tenantId,
          userId: ctx.userId,
          role: ctx.role,
          scopeType: ctx.scopeType,
          scopeId: ctx.scopeId,
          status: 'ACTIVE',
          joinedAt: ''
        });

        if (ctx.accessibleNodes && ctx.accessibleNodes.length > 0) {
          setCurrentOrganizationNode(ctx.accessibleNodes[0]);
        }

        setSubscription(ctx.subscription || null);
        setQuotas(ctx.subscription?.quotas || null);
        setBranding(ctx.branding || null);
        setFeatures(ctx.features || {});
        setSettings(ctx.settings || null);

        if (ctx.permissions) {
          const permObjects = ctx.permissions.map((key: string) => ({ key }));
          setPermissions(permObjects);
        }

        // Load roles
        const rolesRes = await api.get('/admin/roles').catch(() => null);
        if (rolesRes?.data) {
          setRoles(rolesRes.data);
        }
      }
      
      // Load available tenants for switcher
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
      const response = await api.post<SwitchTenantResponse>('/tenant-switcher/switch', { tenantId });
      if (response.data.success) {
        await refreshContext();
        window.location.reload();
      }
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
    return features[feature] === true;
  }, [features]);

  const canAccess = useCallback((resource: string, action: string, scopeType?: string, scopeId?: string): boolean => {
    const permissionKey = `${resource.toUpperCase()}_${action.toUpperCase()}`;
    return hasPermission(permissionKey);
  }, [hasPermission]);

  const isQuotaExceeded = useCallback((quotaKey: keyof Quotas): boolean => {
    if (!quotas) return false;
    const current = quotas[`current${quotaKey.charAt(0).toUpperCase() + quotaKey.slice(1).replace('Max', '')}` as keyof Quotas] as number;
    const max = quotas[quotaKey] as number;
    return current >= max;
  }, [quotas]);

  return (
    <TenantContext.Provider value={{
      currentTenant,
      currentMembership,
      currentOrganizationNode,
      availableTenants,
      roles,
      permissions,
      subscription,
      quotas,
      branding,
      features,
      settings,
      switchTenant,
      switchOrganization,
      refreshContext,
      hasRole,
      hasPermission,
      hasFeature,
      canAccess,
      isQuotaExceeded,
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