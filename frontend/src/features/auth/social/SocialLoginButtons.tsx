import { useCallback, useEffect, useState } from 'react';
import { useI18n } from '@/i18n';
import { AlertCircle, Loader2 } from 'lucide-react';
import api from '@/lib/api';
import { facebookRedirectUri, startFacebookLogin } from './facebook';
import { requestGoogleCredential } from './google';
import { requestMicrosoftCredential } from './microsoft';
import {
  buildTimeClientIds,
  fetchSocialProviders,
  providerLabel,
  toSocialAuthError,
} from './providers';
import {
  SocialAuthError,
  type SocialProviderId,
  type SocialProvidersState,
} from './types';

/**
 * Boutons « Se connecter avec Google / Microsoft ».
 *
 * <p>Trois principes tenus ici :
 * <ol>
 *   <li><b>Pas de bouton mort</b> : un bouton n'apparaît que si le serveur
 *       déclare ce fournisseur actif ({@code GET /auth/social/providers}). Le
 *       client-id de build est un second filtre, jamais le seul.</li>
 *   <li><b>Aucun secret dans le navigateur</b> : le credential obtenu est
 *       envoyé au serveur, qui seul décide.</li>
 *   <li><b>Échecs distingués et honnêtes</b> : « aucun compte pour cette
 *       adresse » propose le lien d'invitation, une annulation reste silencieuse,
 *       un fournisseur non configuré fait disparaître le bouton.</li>
 * </ol>
 */

export interface SocialLoginButtonsProps {
  /** Appelé avec la réponse d'authentification complète (mêmes champs que `/auth/login`). */
  onAuthenticated: (tokens: {
    accessToken: string;
    refreshToken: string;
    user: {
      id: string;
      email: string;
      firstName?: string | null;
      lastName?: string | null;
      role: string;
      photoUrl?: string | null;
      activeRole?: string;
    };
  }) => void;
  /** Message d'erreur à afficher (contrôlé par le parent). */
  error?: string | null;
  /** Rappel : le parent doit effacer son erreur quand l'utilisateur réessaie. */
  onErrorChange?: (message: string | null) => void;
  /** Désactive les boutons (envoi de formulaire en cours). */
  disabled?: boolean;
}

const CANCELLED_CODES = new Set(['SOCIAL_LOGIN_CANCELLED']);

export function SocialLoginButtons({
  onAuthenticated,
  error,
  onErrorChange,
  disabled = false,
}: SocialLoginButtonsProps) {
  const { t } = useI18n();
  const [state, setState] = useState<SocialProvidersState | null>(null);
  const [pending, setPending] = useState<SocialProviderId | null>(null);

  useEffect(() => {
    let active = true;
    fetchSocialProviders()
      .then((providers) => {
        if (active) setState(providers);
      })
      .catch(() => {
        if (active) setState({ providers: [], accountLinkingEnabled: true });
      });
    return () => {
      active = false;
    };
  }, []);

  const connect = useCallback(
    async (provider: SocialProviderId) => {
      const clientIds = buildTimeClientIds();
      const clientId =
        provider === 'google'
          ? clientIds.google
          : provider === 'microsoft'
            ? clientIds.microsoft
            : clientIds.facebook;

      if (provider === 'facebook') {
        // Flux par redirection : la page se recharge vers le dialogue Meta puis
        // revient sur /auth/social/callback. Aucun état à conserver ici.
        startFacebookLogin({
          appId: clientId ?? '',
          apiVersion: import.meta.env.VITE_FACEBOOK_API_VERSION || 'v21.0',
          redirectUri: facebookRedirectUri(),
        });
        return;
      }

      if (!clientId) {
        onErrorChange?.(
          t('auth.social.notConfigured', { provider: labelOf(provider) })
        );
        return;
      }

      setPending(provider);
      onErrorChange?.(null);
      try {
        const credential =
          provider === 'google'
            ? await requestGoogleCredential(clientId)
            : await requestMicrosoftCredential(
                clientId,
                import.meta.env.VITE_MICROSOFT_TENANT_ID || 'common'
              );

        const response = await api.post(`/auth/social/${provider}`, { credential });
        const data = response.data;

        onAuthenticated({
          accessToken: data.accessToken,
          refreshToken: data.refreshToken,
          user: {
            id: data.userId,
            email: data.email,
            firstName: data.firstName ?? undefined,
            lastName: data.lastName ?? undefined,
            role: data.role,
          },
        });
      } catch (rawError) {
        if (CANCELLED_CODES.has((rawError as SocialAuthError)?.code ?? '')) {
          // Annulation volontaire : aucun message d'erreur.
          return;
        }
        const failure = toSocialAuthError(rawError);
        if (failure.providerUnavailable) {
          // Le serveur ne sert plus ce fournisseur : on retire le bouton au lieu
          // de laisser l'utilisateur cliquer pour rien.
          setState((current) =>
            current
              ? {
                  ...current,
                  providers: current.providers.filter((entry) => entry.provider !== provider),
                }
              : current
          );
        }
        onErrorChange?.(messageFor(failure, provider, t));
      } finally {
        setPending(null);
      }
    },
    [onAuthenticated, onErrorChange, t]
  );

  // Tant que l'état des fournisseurs est inconnu, on n'affiche rien : afficher
  // deux boutons puis les retirer est pire que ne rien afficher.
  if (!state || state.providers.length === 0) return null;

  return (
    <div className="space-y-3" data-testid="social-login-buttons">
      {state.providers.map(({ provider, label }) => {
        const isPending = pending === provider;
        return (
          <button
            key={provider}
            type="button"
            onClick={() => connect(provider)}
            disabled={disabled || pending !== null}
            aria-busy={isPending}
            aria-label={t('auth.social.connectWith', { provider: label })}
            data-testid={`social-button-${provider}`}
            className="w-full py-3 px-4 rounded-xl border border-gray-200 dark:border-white/10
                       bg-white dark:bg-white/5 text-gray-700 dark:text-gray-300
                       font-medium text-sm hover:bg-gray-50 dark:hover:bg-white/10
                       transition-all duration-200 flex items-center justify-center gap-3
                       disabled:opacity-60 disabled:cursor-not-allowed
                       focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-green-500"
          >
            {isPending ? (
              <Loader2 className="w-5 h-5 animate-spin" aria-hidden="true" />
            ) : (
              <ProviderIcon provider={provider} />
            )}
            <span>
              {isPending
                ? t('auth.social.connecting', { provider: label })
                : t('auth.social.connectWith', { provider: label })}
            </span>
          </button>
        );
      })}

      {error && (
        <p
          role="alert"
          data-testid="social-login-error"
          className="flex items-start gap-2 text-sm text-red-600 dark:text-red-400"
        >
          <AlertCircle className="w-4 h-4 mt-0.5 shrink-0" aria-hidden="true" />
          <span>{error}</span>
        </p>
      )}
    </div>
  );
}

function labelOf(provider: SocialProviderId): string {
  return providerLabel(provider);
}

/**
 * Message d'erreur : on distingue les cas où l'utilisateur peut agir de ceux
 * où il ne peut rien faire, et on ne prétend jamais qu'un compte a été créé.
 */
function messageFor(
  failure: SocialAuthError,
  provider: SocialProviderId,
  t: (key: string, options?: Record<string, string>) => string
): string {
  const providerLabel = labelOf(provider);
  switch (failure.code) {
    case 'SOCIAL_ACCOUNT_NOT_LINKED':
      return t('auth.social.accountNotLinked');
    case 'ACCOUNT_NOT_ACTIVATED':
      return t('auth.social.accountNotActivated');
    case 'ACCOUNT_INACTIVE':
      return t('auth.social.accountInactive');
    case 'ACCOUNT_LOCKED':
      return t('auth.social.accountLocked');
    case 'SOCIAL_EMAIL_NOT_VERIFIED':
      return t('auth.social.emailNotVerified');
    case 'SOCIAL_EMAIL_MISMATCH':
      return t('auth.social.emailMismatch');
    case 'SOCIAL_TENANT_NOT_ALLOWED':
      return t('auth.social.tenantNotAllowed');
    case 'SOCIAL_EMAIL_MISSING':
      return t('auth.social.facebookNoEmail');
    case 'SOCIAL_IDENTITY_ALREADY_LINKED':
    case 'SOCIAL_PROVIDER_ALREADY_LINKED':
      return t('auth.social.identityAlreadyLinked');
    case 'SOCIAL_PROVIDER_NOT_CONFIGURED':
      return t('auth.social.notConfigured', { provider: providerLabel });
    case 'RATE_LIMITED':
      return t('auth.social.rateLimited');
    case 'ERR_NETWORK':
      return t('auth.social.networkError');
    default:
      return t('auth.social.genericError');
  }
}

/** Icônes officielles, en SVG inline (aucune requête réseau supplémentaire). */
function ProviderIcon({ provider }: { provider: SocialProviderId }) {
  if (provider === 'facebook') {
    return (
      <svg className="w-5 h-5" viewBox="0 0 24 24" aria-hidden="true">
        <path
          fill="#1877F2"
          d="M24 12.073c0-6.627-5.373-12-12-12s-12 5.373-12 12c0 5.99 4.388 10.954 10.125 11.854v-8.385H7.078v-3.47h3.047V9.43c0-3.007 1.792-4.669 4.533-4.669 1.312 0 2.686.235 2.686.235v2.953H15.83c-1.491 0-1.956.925-1.956 1.874v2.25h3.328l-.532 3.47h-2.796v8.385C19.612 23.027 24 18.062 24 12.073z"
        />
      </svg>
    );
  }
  if (provider === 'microsoft') {
    return (
      <svg className="w-5 h-5" viewBox="0 0 23 23" aria-hidden="true">
        <path fill="#F25022" d="M1 1h10v10H1z" />
        <path fill="#7FBA00" d="M12 1h10v10H12z" />
        <path fill="#00A4EF" d="M1 12h10v10H1z" />
        <path fill="#FFB900" d="M12 12h10v10H12z" />
      </svg>
    );
  }
  return (
    <svg className="w-5 h-5" viewBox="0 0 24 24" aria-hidden="true">
      <path
        fill="#4285F4"
        d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92a5.06 5.06 0 0 1-2.2 3.32v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.1z"
      />
      <path
        fill="#34A853"
        d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z"
      />
      <path
        fill="#FBBC05"
        d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.07H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.93l2.85-2.22.81-.62z"
      />
      <path
        fill="#EA4335"
        d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.07l3.66 2.84c.87-2.6 3.3-4.53 6.16-4.53z"
      />
    </svg>
  );
}

export default SocialLoginButtons;
