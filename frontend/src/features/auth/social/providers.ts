import api from '@/lib/api';
import {
  SocialAuthError,
  SocialErrorCode,
  type SocialProviderId,
  type SocialProvidersState,
} from './types';

/**
 * État des fournisseurs d'identité : ce que le SERVEUR accepte réellement.
 *
 * <p>Pourquoi interroger l'API plutôt que se fier au seul build :
 * `VITE_GOOGLE_CLIENT_ID` peut être présent à la compilation (build de préprod)
 * alors que le serveur de destination ne l'a pas configuré — le bouton
 * s'afficherait alors pour échouer avec un 503 au clic. L'API est la seule
 * source de vérité, `GET /auth/social/providers` étant public et sans secret.
 *
 * <p>Échec réseau : on ne masque PAS tout, on retombe sur la configuration de
 * build. Un bouton visible qui échoue est moins grave qu'un bouton invisible
 * alors que tout fonctionne.
 */

const EMPTY_STATE: SocialProvidersState = { providers: [], accountLinkingEnabled: true };

let cached: SocialProvidersState | null = null;
let inFlight: Promise<SocialProvidersState> | null = null;

function isSocialProviderId(value: unknown): value is SocialProviderId {
  return value === 'google' || value === 'microsoft' || value === 'facebook';
}

/** Identifiants présents dans le build (non secrets, publics par nature). */
export function buildTimeClientIds(): {
  google?: string;
  microsoft?: string;
  facebook?: string;
} {
  return {
    google: import.meta.env.VITE_GOOGLE_CLIENT_ID || undefined,
    microsoft: import.meta.env.VITE_MICROSOFT_CLIENT_ID || undefined,
    facebook: import.meta.env.VITE_FACEBOOK_APP_ID || undefined,
  };
}

/** Nom de marque affiché : jamais traduit (Facebook reste Facebook). */
export function providerLabel(provider: SocialProviderId): string {
  switch (provider) {
    case 'google':
      return 'Google';
    case 'microsoft':
      return 'Microsoft';
    case 'facebook':
      return 'Facebook';
  }
}

/**
 * Fournisseurs prêts à l'emploi. Une seule requête réseau, résultat mis en
 * cache pour la durée de la session de la page.
 */
export async function fetchSocialProviders(
  force = false
): Promise<SocialProvidersState> {
  if (!force && cached) return cached;
  if (!force && inFlight) return inFlight;

  inFlight = api
    .get<{ providers?: unknown; accountLinkingEnabled?: boolean }>('/auth/social/providers')
    .then((response) => {
      const rawProviders = Array.isArray(response.data?.providers) ? response.data.providers : [];
      const state: SocialProvidersState = {
        providers: rawProviders
          .map((entry) => (entry as { provider?: unknown })?.provider)
          .filter(isSocialProviderId)
          .map((provider) => ({ provider, label: providerLabel(provider) })),
        accountLinkingEnabled: response.data?.accountLinkingEnabled !== false,
      };
      cached = state;
      return state;
    })
    .catch(() => {
      // Repli sur la configuration de build : mieux vaut un bouton qui échoue
      // qu'une fonctionnalité manquante.
      const ids = buildTimeClientIds();
      const providers: SocialProvidersState['providers'] = [];
      if (ids.google) providers.push({ provider: 'google', label: providerLabel('google') });
      if (ids.microsoft)
        providers.push({ provider: 'microsoft', label: providerLabel('microsoft') });
      if (ids.facebook)
        providers.push({ provider: 'facebook', label: providerLabel('facebook') });
      return { providers, accountLinkingEnabled: true };
    })
    .finally(() => {
      inFlight = null;
    });

  return inFlight;
}

/** Réinitialise le cache (tests, changement d'environnement). */
export function resetSocialProvidersCache(): void {
  cached = null;
  inFlight = null;
}

export function cachedSocialProviders(): SocialProvidersState | null {
  return cached;
}

/** Traduit une erreur axios en `SocialAuthError` avec le code serveur. */
export function toSocialAuthError(error: unknown): SocialAuthError {
  const response = (error as { response?: { status?: number; data?: unknown } })?.response;
  const data = (response?.data ?? {}) as {
    title?: string;
    detail?: string;
    code?: string;
    error?: string;
  };
  const code = data.title ?? data.code ?? SocialErrorCode.CREDENTIAL_REJECTED;
  const message =
    data.detail ?? data.error ?? 'Connexion impossible. Veuillez réessayer.';
  return new SocialAuthError(code, message, response?.status);
}
