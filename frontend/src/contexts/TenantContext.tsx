import { createContext, useContext, useRef, useState, useEffect, useCallback, type ReactNode } from 'react';
import { useQueryClient } from '@tanstack/react-query';
import { useAuthOptional } from '@/contexts/AuthContext';
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
  // Lecture tolérante : main ne charge le contexte qu'un fois authentifié,
  // mais le provider peut être monté seul (tests, écran de sélection).
  const auth = useAuthOptional();
  const isAuthenticated = auth?.isAuthenticated ?? true;
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
  const queryClient = useQueryClient();
  // G5.4 (§55) « chargement unique » : StrictMode / appels concurrents ne
  // doivent pas déclencher plusieurs fetch du contexte (promesse partagée).
  const inflightRef = useRef<Promise<void> | null>(null);

  const refreshContext = useCallback(async () => {
    if (inflightRef.current) return inflightRef.current;
    const run = (async () => {
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

        if (ctx.permissions || ctx.tenantId) {
          const permKeys = new Set<string>((ctx.permissions || []) as string[]);
          // §G4.4 « rôles vivants » : fusion des permissions RÉSOLUES serveur
          // (PermissionResolver — affectations actives dont pastorate, version
          // permission_version). Les rôles du JWT de session ne reflètent pas
          // une nomination/fin de mandat en cours de session.
          const liveRes = await api.get('/me/permissions').catch(() => null);
          if (Array.isArray(liveRes?.data?.permissions)) {
            for (const key of liveRes.data.permissions as string[]) permKeys.add(key);
          }
          setPermissions([...permKeys].map((key) => ({ key })) as unknown as Permission[]);
        }

        // Load roles
        const rolesRes = await api.get('/admin/roles/overview').catch(() => null);
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
    })();
    inflightRef.current = run;
    void run.finally(() => { inflightRef.current = null; });
    return run;
  }, []);

  useEffect(() => {
    if (!isAuthenticated) {
      setIsLoading(false);
      setIsInitialized(true);
      return;
    }
    refreshContext();
  }, [isAuthenticated, refreshContext]);

  const switchTenant = useCallback(async (tenantId: string) => {
    try {
      const response = await api.post<SwitchTenantResponse>('/tenant-switcher/switch', { tenantId });
      if (response.data.success) {
        // G5.4 (§55) : le backend réémet des JWT portant le nouveau claim
        // tenantId — sans les stocker, la requête suivante rejouerait l'ancien
        // tenant. Le header de main est réaligné, puis le cache de requêtes est
        // vidé et le contexte rechargé : la bascule est instantanée et conserve
        // la route de l'utilisateur (pas de window.location.reload).
        if (response.data.accessToken) {
          localStorage.setItem('accessToken', response.data.accessToken);
          // Réaligne le header global si l'instance axios l'expose. Défensif :
          // les doubles de test peuvent ne pas fournir defaults.headers, et
          // l'intercepteur requête relit le localStorage à chaque appel de
          // toute façon — le header est donc correct au prochain ping.
          const common = (api.defaults as unknown as
            { headers?: { common?: Record<string, string> } })?.headers?.common;
          if (common) common['Authorization'] = `Bearer ${response.data.accessToken}`;
        }
        if (response.data.refreshToken) {
          localStorage.setItem('refreshToken', response.data.refreshToken);
        }
        queryClient.clear();
        await refreshContext();
      }
    } catch (error) {
      console.error('Erreur changement tenant:', error);
      throw error;
    }
  }, [queryClient, refreshContext]);

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

  /**
   * G5.4 (§55-2 RequireFeature) : refus uniquement quand le drapeau résolu par
   * le serveur dit explicitement false (miroir du 403 backend). Une absence de
   * drapeau = module non géré -> jamais un refus arbitraire du frontend.
   */
  const isFeatureBlocked = useCallback((feature: string): boolean => {
    return features[feature] === false || features[feature.toUpperCase()] === false;
  }, [features]);

  const canAccess = useCallback((resource: string, action: string, _scopeType?: string, _scopeId?: string): boolean => {
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
      isFeatureBlocked,
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

/// Variante tolérante : retourne null hors TenantProvider (au lieu de lever).
/// Destinée aux composants additionnels (ex. bus temps réel) qui doivent
/// s'effacer gracieusement quand aucun contexte tenant n'est monté — jamais
/// pour le cœur applicatif qui exige un tenant actif.
export function useTenantOptional() {
  return useContext(TenantContext);
}