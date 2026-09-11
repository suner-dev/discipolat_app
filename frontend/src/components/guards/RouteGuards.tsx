import { ReactNode } from 'react';
import { useTenant } from '@/contexts/TenantContext';

/**
 * RequireAuthentication — Guard pour vérifier que l'utilisateur est authentifié.
 */
export function RequireAuthentication({ children }: { children: ReactNode }) {
  const { isInitialized, currentMembership } = useTenant();

  if (!isInitialized) {
    return (
      <div className="flex items-center justify-center min-h-screen">
        <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-indigo-600"></div>
      </div>
    );
  }

  if (!currentMembership) {
    return (
      <div className="flex items-center justify-center min-h-screen bg-gray-50">
        <div className="text-center">
          <p className="text-red-600 text-lg">Authentification requise</p>
          <a href="/auth/login" className="mt-4 inline-block px-4 py-2 bg-indigo-600 text-white rounded-lg">
            Se connecter
          </a>
        </div>
      </div>
    );
  }

  return <>{children}</>;
}

/**
 * RequireTenant — Guard pour vérifier qu'un tenant est actif.
 */
export function RequireTenant({ children }: { children: ReactNode }) {
  const { isInitialized, currentTenant } = useTenant();

  if (!isInitialized) {
    return (
      <div className="flex items-center justify-center min-h-screen">
        <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-indigo-600"></div>
      </div>
    );
  }

  if (!currentTenant) {
    return (
      <div className="flex items-center justify-center min-h-screen bg-gray-50">
        <div className="text-center">
          <p className="text-yellow-600 text-lg">Aucun tenant sélectionné</p>
          <a href="/tenant-switcher" className="mt-4 inline-block px-4 py-2 bg-indigo-600 text-white rounded-lg">
            Choisir une organisation
          </a>
        </div>
      </div>
    );
  }

  return <>{children}</>;
}

/**
 * RequirePermission — Guard pour vérifier une permission spécifique.
 */
export function RequirePermission({ 
  permission, 
  children 
}: { 
  permission: string; 
  children: ReactNode 
}) {
  const { hasPermission, isInitialized } = useTenant();

  if (!isInitialized) {
    return (
      <div className="flex items-center justify-center min-h-screen">
        <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-indigo-600"></div>
      </div>
    );
  }

  if (!hasPermission(permission)) {
    return (
      <div className="flex items-center justify-center min-h-screen bg-gray-50">
        <div className="text-center">
          <p className="text-red-600 text-lg">Accès refusé</p>
          <p className="text-gray-500 text-sm mt-2">
            Permission requise : {permission}
          </p>
        </div>
      </div>
    );
  }

  return <>{children}</>;
}

/**
 * RequireRole — Guard pour vérifier un rôle spécifique.
 */
export function RequireRole({ 
  role, 
  children 
}: { 
  role: string; 
  children: ReactNode 
}) {
  const { hasRole, isInitialized } = useTenant();

  if (!isInitialized) {
    return (
      <div className="flex items-center justify-center min-h-screen">
        <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-indigo-600"></div>
      </div>
    );
  }

  if (!hasRole(role)) {
    return (
      <div className="flex items-center justify-center min-h-screen bg-gray-50">
        <div className="text-center">
          <p className="text-red-600 text-lg">Accès refusé</p>
          <p className="text-gray-500 text-sm mt-2">
            Rôle requis : {role}
          </p>
        </div>
      </div>
    );
  }

  return <>{children}</>;
}

/**
 * RequireFeature — Guard pour vérifier qu'une fonctionnalité est activée.
 */
export function RequireFeature({ 
  feature, 
  children 
}: { 
  feature: string; 
  children: ReactNode 
}) {
  const { hasFeature, isInitialized } = useTenant();

  if (!isInitialized) {
    return (
      <div className="flex items-center justify-center min-h-screen">
        <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-indigo-600"></div>
      </div>
    );
  }

  if (!hasFeature(feature)) {
    return (
      <div className="flex items-center justify-center min-h-screen bg-gray-50">
        <div className="text-center">
          <p className="text-yellow-600 text-lg">Fonctionnalité non disponible</p>
          <p className="text-gray-500 text-sm mt-2">
            Cette fonctionnalité n'est pas incluse dans votre plan.
          </p>
        </div>
      </div>
    );
  }

  return <>{children}</>;
}

/**
 * RequireAnyPermission — Guard pour vérifier qu'au moins une permission est présente.
 */
export function RequireAnyPermission({ 
  permissions, 
  children 
}: { 
  permissions: string[]; 
  children: ReactNode 
}) {
  const { hasPermission, isInitialized } = useTenant();

  if (!isInitialized) {
    return (
      <div className="flex items-center justify-center min-h-screen">
        <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-indigo-600"></div>
      </div>
    );
  }

  const hasAny = permissions.some(p => hasPermission(p));

  if (!hasAny) {
    return (
      <div className="flex items-center justify-center min-h-screen bg-gray-50">
        <div className="text-center">
          <p className="text-red-600 text-lg">Accès refusé</p>
          <p className="text-gray-500 text-sm mt-2">
            Permission requise : {permissions.join(' ou ')}
          </p>
        </div>
      </div>
    );
  }

  return <>{children}</>;
}
