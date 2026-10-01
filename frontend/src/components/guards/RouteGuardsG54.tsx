import { ReactNode, useState } from 'react';
import { Navigate, useNavigate } from 'react-router-dom';
import { Loader2, ShieldAlert, Send, Building2 } from 'lucide-react';
import { useTenant } from '@/contexts/TenantContext';
import { useAuth } from '@/contexts/AuthContext';
import { useI18n } from '@/i18n';
import api from '@/lib/api';
import toast from 'react-hot-toast';

/**
 * PORT Develop1 (§G5.4-55/56) — le jeu de gardes complet de Develop1, porté tel quel.
 *
 * main a son propre fichier `RouteGuards.tsx` (utilise par `App.tsx` via
 * `RequireTenantAccess`) : il est CONSERVE, intact. Ce module ajoute les gardes
 * de Develop1 qui n'existaient pas sous ce nom — `PermissionGate`, `ScopeGate`,
 * `RequestAccessButton`, `AccessDeniedScreen` — et leurs semantics propres
 * (refus uniquement sur drapeau serveur explicitement false, formulations
 * « Rôles acceptes » et « Fonctionnalité non incluse », bascule sans
 * rechargement de page). Rien n'est doble a l'ecran : les pages de main restent
 * branchees sur les gardes de main.
 *
 * G5.4 (§55-56) — Gardes centralisées du frontend.
 *
 * Règle absolue (§0.3) : ces gardes sont une UX de confort, JAMAIS une
 * autorisation. Chaque permission testée ici est résolue CÔTÉ SERVEUR
 * (/tenant-switcher/context, §G4.4) et re-vérifiée à chaque appel d'API.
 * Parité Security Matrix : chaque règle front a sa règle back.
 *
 * Gardes livrées (§55-2) :
 *  - RequireAuth ................ session authentifiée
 *  - RequireTenantAccess ........ tenant sélectionné + adhésion active
 *  - RequireScope(resource, action) miroir des clés RESOURCE_ACTION serveur
 *  - RequireRole(roles[]) ....... rôle résolu (membership tenant)
 *  - RequireFeature(code) ....... module activé au tenant (G1.3) — refus
 *    uniquement sur drapeau serveur explicitement false (miroir du 403)
 *
 * Sur refus : écran « Accès refusé » + bouton « Demander l'accès » qui
 * notifie réellement les responsables (POST /access-requests, §G5.4).
 */

/* ─────────────────────────── parties communes ─────────────────────────── */

function GuardLoader() {
  return (
    <div className="flex items-center justify-center min-h-screen" role="status" aria-label="loading">
      <Loader2 className="w-10 h-10 animate-spin text-indigo-600" />
    </div>
  );
}

/** Bouton « Demander l'accès » — notification réelle des responsables. */
export function RequestAccessButton({ permissionKey, resourceLabel }: {
  permissionKey: string;
  resourceLabel?: string;
}) {
  const { t } = useI18n();
  const [sent, setSent] = useState(false);
  const [pending, setPending] = useState(false);

  const onClick = async () => {
    setPending(true);
    try {
      const res = await api.post('/access-requests', { permissionKey, resourceLabel });
      const status = (res.data as { status?: string })?.status;
      setSent(true);
      toast.success(status === 'ALREADY_SENT' ? t('guard.requestAlready') : t('guard.requestSent'));
    } catch {
      toast.error(t('guard.requestErr'));
    } finally {
      setPending(false);
    }
  };

  return (
    <button
      type="button"
      onClick={onClick}
      disabled={sent || pending}
      className="mt-4 inline-flex items-center gap-2 px-4 py-2 bg-indigo-600 hover:bg-indigo-700 disabled:opacity-50 disabled:cursor-not-allowed text-white text-sm rounded-lg transition-colors"
    >
      {pending ? <Loader2 className="w-4 h-4 animate-spin" /> : <Send className="w-4 h-4" />}
      {sent ? t('guard.requestSent') : t('guard.requestAccess')}
    </button>
  );
}

/** Écran de refus partagé (403 UX) — même gabarit partout. */
export function AccessDeniedScreen({ title, detail, permissionKey, resourceLabel, action }: {
  title: string;
  detail?: string;
  permissionKey?: string;
  resourceLabel?: string;
  action?: ReactNode;
}) {
  const { t } = useI18n();
  const navigate = useNavigate();
  return (
    <div className="flex items-center justify-center min-h-screen bg-gray-50 dark:bg-gray-950 p-4">
      <div className="glass-card p-10 max-w-md w-full text-center">
        <ShieldAlert className="w-12 h-12 text-amber-500 mx-auto mb-4" aria-hidden />
        <h1 className="text-xl font-semibold text-gray-900 dark:text-gray-100">{title}</h1>
        {detail && (
          <p className="text-sm text-gray-500 dark:text-gray-400 mt-2 break-words">{detail}</p>
        )}
        {permissionKey && <RequestAccessButton permissionKey={permissionKey} resourceLabel={resourceLabel} />}
        <div className="mt-3 flex items-center justify-center gap-2">
          {action}
          <button
            type="button"
            onClick={() => navigate('/dashboard')}
            className="text-sm text-gray-500 hover:text-gray-700 dark:hover:text-gray-300 underline underline-offset-2"
          >
            {t('guard.backHome')}
          </button>
        </div>
      </div>
    </div>
  );
}

/* ─────────────────────────────── gardes ──────────────────────────────── */

/** Session authentifiée (sinon redirection login). */
export function RequireAuth({ children }: { children: ReactNode }) {
  const { isAuthenticated, isLoading } = useAuth();

  if (isLoading) return <GuardLoader />;
  if (!isAuthenticated) return <Navigate to="/login" replace />;
  return <>{children}</>;
}

/** Tenant actif sélectionné + adhésion (sinon écran de choix d'organisation). */
export function RequireTenantAccess({ children }: { children: ReactNode }) {
  const { isInitialized, currentTenant } = useTenant();
  const { t } = useI18n();

  if (!isInitialized) return <GuardLoader />;

  if (!currentTenant) {
    return (
      <AccessDeniedScreen
        title={t('guard.noTenant')}
        detail={t('guard.noTenantDetail')}
        action={
          <a
            href="/tenant-switcher"
            className="mt-0 inline-flex items-center gap-2 px-4 py-2 bg-indigo-600 hover:bg-indigo-700 text-white text-sm rounded-lg"
          >
            <Building2 className="w-4 h-4" /> {t('guard.chooseOrg')}
          </a>
        }
      />
    );
  }

  return <>{children}</>;
}

/**
 * Scope métier : permission résolue serveur RESOURCE_ACTION (§G4.4).
 * La resource peut être multi-segments : RequireScope(resource="ORG_NODE",
 * action="MOVE") teste la clé serveur exacte ORG_NODE_MOVE.
 * Le refus affiche « Demander l'accès » avec la CLÉ EXACTE vérifiée — le
 * responsable notifié retrouve directement la règle dans l'éditeur de rôles.
 */
export function RequireScope({
  resource,
  action,
  resourceLabel,
  children,
}: {
  resource: string;
  action: string;
  resourceLabel?: string;
  children: ReactNode;
}) {
  const { canAccess, isInitialized } = useTenant();
  const { t } = useI18n();

  if (!isInitialized) return <GuardLoader />;

  if (!canAccess(resource, action)) {
    const key = `${resource.toUpperCase()}_${action.toUpperCase()}`;
    return (
      <AccessDeniedScreen
        title={t('guard.accessDenied')}
        detail={`${t('guard.requiredPermission')}${key}`}
        permissionKey={key}
        resourceLabel={resourceLabel ?? key}
      />
    );
  }

  return <>{children}</>;
}

/** Permission spécifique (clé exacte résolue serveur). */
export function RequirePermission({ permission, children }: { permission: string; children: ReactNode }) {
  const { hasPermission, isInitialized } = useTenant();
  const { t } = useI18n();

  if (!isInitialized) return <GuardLoader />;

  if (!hasPermission(permission)) {
    return (
      <AccessDeniedScreen
        title={t('guard.accessDenied')}
        detail={`${t('guard.requiredPermission')}${permission}`}
        permissionKey={permission}
        resourceLabel={permission}
      />
    );
  }

  return <>{children}</>;
}

/** Au moins une des permissions listées. */
export function RequireAnyPermission({ permissions, children }: { permissions: string[]; children: ReactNode }) {
  const { hasPermission, isInitialized } = useTenant();
  const { t } = useI18n();

  if (!isInitialized) return <GuardLoader />;

  if (!permissions.some((p) => hasPermission(p))) {
    return (
      <AccessDeniedScreen
        title={t('guard.accessDenied')}
        detail={`${t('guard.requiredPermission')}${permissions.join(' · ')}`}
        permissionKey={permissions[0]}
        resourceLabel={permissions[0]}
      />
    );
  }

  return <>{children}</>;
}

/** Rôle résolu (membership tenant §G3.2 ou rôle de session). */
export function RequireRole({ roles, children }: { roles: string[]; children: ReactNode }) {
  const { hasRole, isInitialized } = useTenant();
  const { t } = useI18n();

  if (!isInitialized) return <GuardLoader />;

  if (!roles.some((r) => hasRole(r))) {
    return (
      <AccessDeniedScreen
        title={t('guard.accessDenied')}
        detail={`${t('guard.requiredRoles')}${roles.join(' · ')}`}
      />
    );
  }

  return <>{children}</>;
}

/**
 * Module activé au tenant (G1.3). Refus seulement sur drapeau serveur
 * explicitement false — miroir du 403 backend (§339), jamais un tri
 * frontend arbitraire. Absence de drapeau = non géré = on laisse passer
 * (le backend garde la main).
 */
export function RequireFeature({ feature, children }: { feature: string; children: ReactNode }) {
  const { isFeatureBlocked, isInitialized } = useTenant();
  const { t } = useI18n();

  if (!isInitialized) return <GuardLoader />;

  if (isFeatureBlocked(feature)) {
    return (
      <AccessDeniedScreen
        title={t('guard.featureUnavailable')}
        detail={t('guard.featureUnavailableDetail', { feature })}
      />
    );
  }

  return <>{children}</>;
}

/* ─────────────── UX désactivée propre (option d'affichage) ─────────────── */

/**
 * §55-3 : action non autorisée = masquée OU désactivée avec tooltip.
 * Enveloppe une action pour la rendre inertement visible quand le droit
 * manque (meilleure découvrabilité que la disparition silencieuse).
 */
export function PermissionGate({ permission, mode = 'hide', tooltipKey = 'guard.requiredPermission', children }: {
  permission: string;
  mode?: 'hide' | 'disable';
  tooltipKey?: string;
  children: ReactNode;
}) {
  const { hasPermission } = useTenant();
  const { t } = useI18n();
  const allowed = hasPermission(permission);

  if (allowed) return <>{children}</>;
  if (mode === 'hide') return null;

  return (
    <span
      className="inline-flex cursor-not-allowed opacity-50"
      title={`${t(tooltipKey)}${permission}`}
      onClick={(e) => e.preventDefault()}
      aria-disabled="true"
    >
      {children}
    </span>
  );
}

/** Variante par clé RESOURCE_ACTION (parité canAccess serveur). */
export function ScopeGate({ resource, action, mode = 'hide', children }: {
  resource: string;
  action: string;
  mode?: 'hide' | 'disable';
  children: ReactNode;
}) {
  return (
    <PermissionGate
      permission={`${resource.toUpperCase()}_${action.toUpperCase()}`}
      mode={mode}
    >
      {children}
    </PermissionGate>
  );
}
