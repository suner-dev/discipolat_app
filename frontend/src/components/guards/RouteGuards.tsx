import { ReactNode } from 'react';
import { useNavigate } from 'react-router-dom';
import { useTenant } from '@/contexts/TenantContext';

/**
 * LoadingSpinner — indicateur de chargement partagé par toutes les guards.
 */
function LoadingSpinner() {
  return (
    <div className="flex items-center justify-center min-h-screen">
      <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-indigo-600"></div>
    </div>
  );
}

/**
 * AccessDenied — écran 403 avec bouton « Demander l'accès ».
 * Envoie une notification au responsable hiérarchique.
 */
export function AccessDenied({
  permission,
  roles,
  onRequestAccess,
}: {
  permission?: string;
  roles?: string[];
  onRequestAccess?: () => void;
}) {
  const navigate = useNavigate();

  return (
    <div className="flex items-center justify-center min-h-screen bg-gray-50 dark:bg-gray-950">
      <div className="text-center max-w-md mx-auto p-8">
        <div className="inline-flex items-center justify-center w-16 h-16 rounded-full bg-red-100 dark:bg-red-900/20 mb-4">
          <svg className="w-8 h-8 text-red-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2}
              d="M12 9v2m0 4h.01m-.01-6a3 3 0 11-6 0 3 3 0 016 0z" />
            <circle cx="12" cy="12" r="10" strokeWidth={2} />
          </svg>
        </div>
        <h1 className="text-2xl font-bold text-gray-900 dark:text-gray-100 mb-2">Accès refusé</h1>
        <p className="text-gray-600 dark:text-gray-400 mb-4">
          Vous n'avez pas les droits nécessaires pour accéder à cette ressource.
        </p>
        {permission && (
          <p className="text-sm text-gray-500 dark:text-gray-500 mb-4">
            Permission requise : <code className="bg-gray-200 dark:bg-gray-800 px-2 py-1 rounded">{permission}</code>
          </p>
        )}
        {roles && roles.length > 0 && (
          <p className="text-sm text-gray-500 dark:text-gray-500 mb-4">
            Rôle(s) requis : {roles.join(', ')}
          </p>
        )}
        <div className="flex gap-3 justify-center">
          <button
            onClick={() => navigate(-1)}
            className="px-4 py-2 text-sm border border-gray-300 dark:border-gray-700 rounded-lg hover:bg-gray-100 dark:hover:bg-gray-800 transition"
          >
            Retour
          </button>
          {onRequestAccess && (
            <button
              onClick={() => {
                onRequestAccess();
              }}
              className="px-4 py-2 text-sm bg-indigo-600 text-white rounded-lg hover:bg-indigo-700 transition"
            >
              Demander l'accès
            </button>
          )}
        </div>
      </div>
    </div>
  );
}

/**
 * RequireAuth — Guard pour vérifier que l'utilisateur est authentifié.
 * Alias canonical utilisé par le contrat maître (§G5.4 : RequireAuth).
 */
export function RequireAuth({ children }: { children: ReactNode }) {
  const { isInitialized, currentMembership } = useTenant();

  if (!isInitialized) {
    return <LoadingSpinner />;
  }

  if (!currentMembership) {
    const navigate = useNavigate();
    return (
      <div className="flex items-center justify-center min-h-screen bg-gray-50 dark:bg-gray-950">
        <div className="text-center">
          <p className="text-red-600 text-lg">Authentification requise</p>
          <button
            onClick={() => navigate('/login')}
            className="mt-4 inline-block px-4 py-2 bg-indigo-600 text-white rounded-lg"
          >
            Se connecter
          </button>
        </div>
      </div>
    );
  }

  return <>{children}</>;
}

/**
 * RequireAuthentication — alias historique de RequireAuth (compatibilité ascendante).
 */
export const RequireAuthentication = RequireAuth;

/**
 * RequireTenant — Guard pour vérifier qu'un tenant est actif.
 */
export function RequireTenant({ children }: { children: ReactNode }) {
  const { isInitialized, currentTenant } = useTenant();

  if (!isInitialized) {
    return <LoadingSpinner />;
  }

  if (!currentTenant) {
    return (
      <div className="flex items-center justify-center min-h-screen bg-gray-50 dark:bg-gray-950">
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
 * RequireTenantAccess — Guard pour vérifier que l'utilisateur possède un
 * accès actif au tenant courant (membership ACTIVE) — exigence §G5.4.
 */
export function RequireTenantAccess({ children }: { children: ReactNode }) {
  const { isInitialized, currentMembership, currentTenant } = useTenant();

  if (!isInitialized) {
    return <LoadingSpinner />;
  }

  if (!currentTenant) {
    return (
      <div className="flex items-center justify-center min-h-screen bg-gray-50 dark:bg-gray-950">
        <div className="text-center">
          <p className="text-yellow-600 text-lg">Aucun tenant sélectionné</p>
          <a href="/tenant-switcher" className="mt-4 inline-block px-4 py-2 bg-indigo-600 text-white rounded-lg">
            Choisir une organisation
          </a>
        </div>
      </div>
    );
  }

  if (!currentMembership || currentMembership.status !== 'ACTIVE') {
    return (
      <div className="flex items-center justify-center min-h-screen bg-gray-50 dark:bg-gray-955">
        <div className="text-center">
          <p className="text-red-600 text-lg">Accès refusé</p>
          <p className="text-gray-500 dark:text-gray-400 text-sm mt-2">
            Votre accès à cette organisation n'est pas actif.
          </p>
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
  children,
}: {
  permission: string;
  children: ReactNode;
}) {
  const { hasPermission, isInitialized } = useTenant();

  if (!isInitialized) {
    return <LoadingSpinner />;
  }

  if (!hasPermission(permission)) {
    return <AccessDenied permission={permission} />;
  }

  return <>{children}</>;
}

/**
 * RequireRole — Guard pour vérifier un rôle ou un ensemble de rôles.
 * Accepte un rôle unique (string) ou une liste de rôles (string[]).
 */
export function RequireRole({
  role,
  children,
}: {
  role: string | string[];
  children: ReactNode;
}) {
  const { hasRole, isInitialized } = useTenant();
  const roles = Array.isArray(role) ? role : [role];

  if (!isInitialized) {
    return <LoadingSpinner />;
  }

  const hasRequiredRole = roles.some((r) => hasRole(r));

  if (!hasRequiredRole) {
    return <AccessDenied roles={roles} />;
  }

  return <>{children}</>;
}

/**
 * RequireScope — Guard pour vérifier un droit d'accès resource/action.
 * Utilise le canAccess du TenantContext (permissions résolues par le backend §G4.4).
 */
export function RequireScope({
  resource,
  action,
  scopeType,
  scopeId,
  children,
}: {
  resource: string;
  action: string;
  scopeType?: string;
  scopeId?: string;
  children: ReactNode;
}) {
  const { canAccess, isInitialized } = useTenant();

  if (!isInitialized) {
    return <LoadingSpinner />;
  }

  if (!canAccess(resource, action, scopeType, scopeId)) {
    return (
      <AccessDenied permission={`${resource.toUpperCase()}_${action.toUpperCase()}`} />
    );
  }

  return <>{children}</>;
}

/**
 * RequireFeature — Guard pour vérifier qu'une fonctionnalité est activée.
 */
export function RequireFeature({
  feature,
  children,
}: {
  feature: string;
  children: ReactNode;
}) {
  const { hasFeature, isInitialized } = useTenant();

  if (!isInitialized) {
    return <LoadingSpinner />;
  }

  if (!hasFeature(feature)) {
    return (
      <div className="flex items-center justify-center min-h-screen bg-gray-50 dark:bg-gray-950">
        <div className="text-center max-w-md mx-auto p-8">
          <div className="inline-flex items-center justify-center w-16 h-16 rounded-full bg-yellow-100 dark:bg-yellow-900/20 mb-4">
            <svg className="w-8 h-8 text-yellow-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2}
                d="M12 9v2m0 4h.01m-6.28 0h12.56c1.17 0 2.14-.94 2.14-2.1v-4.2c0-1.17-.94-2.1-2.14-2.1H5.72c-1.17 0-2.12.83-2.12 2.1v4.2c0 1.16.95 2.1 2.12 2.1z" />
            </svg>
          </div>
          <h1 className="text-2xl font-bold text-gray-900 dark:text-gray-100 mb-2">
            Fonctionnalité non disponible
          </h1>
          <p className="text-gray-600 dark:text-gray-400">
            Cette fonctionnalité n'est pas incluse dans votre plan.
            Contactez un administrateur pour plus d'informations.
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
  children,
}: {
  permissions: string[];
  children: ReactNode;
}) {
  const { hasPermission, isInitialized } = useTenant();

  if (!isInitialized) {
    return <LoadingSpinner />;
  }

  const hasAny = permissions.some((p) => hasPermission(p));

  if (!hasAny) {
    return <AccessDenied permission={permissions.join(' ou ')} />;
  }

  return <>{children}</>;
}
