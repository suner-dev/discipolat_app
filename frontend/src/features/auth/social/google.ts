import { SocialAuthError, SocialErrorCode } from './types';

/**
 * Google Identity Services — récupération d'un `id_token`.
 *
 * <p>Choix d'implémentation, et pourquoi :
 * <ul>
 *   <li><b>Chargement dynamique du script</b> `accounts.google.com/gsi/client`
 *       : Google n'est pas chargé si l'utilisateur ne clique jamais, donc zéro
 *       coût pour 95 % des connexions, et aucune dépendance externe au premier
 *       rendu.</li>
 *   <li><b>Pas d'ajout dans index.html</b> : le script était auparavant absent
 *       (le bouton affichait « Google indisponible » en permanence).</li>
 *   <li><b>Le credential est renvoyé au backend, jamais décodé ici</b> : le
 *       navigateur ne fait aucune confiance, seule la signature vérifiée par le
 *       serveur compte.</li>
 *   <li><b>Origin explicite</b> : le client ne déclare que son propre domaine,
 *       ce qui empêche un site tiers d'exploiter ce même client-id.</li>
 * </ul>
 */

const GIS_SRC = 'https://accounts.google.com/gsi/client';
const GIS_READY_TIMEOUT_MS = 10_000;

declare global {
  interface Window {
    google?: {
      accounts?: {
        id?: {
          initialize: (config: {
            client_id: string;
            callback: (response: { credential?: string }) => void;
            ux_mode?: string;
            cancel_on_tap_outside?: boolean;
          }) => void;
          prompt: (notification?: (res: NonNullable<unknown>) => void) => void;
          cancel: () => void;
        };
      };
    };
  }
}

let scriptPromise: Promise<void> | null = null;

/** Charge le script GIS une seule fois par session de page. */
function loadGoogleScript(): Promise<void> {
  if (window.google?.accounts?.id) return Promise.resolve();
  if (scriptPromise) return scriptPromise;

  scriptPromise = new Promise<void>((resolve, reject) => {
    const existing = document.querySelector<HTMLScriptElement>(
      `script[src="${GIS_SRC}"]`
    );
    if (existing) {
      existing.addEventListener('load', () => resolve());
      existing.addEventListener('error', () =>
        reject(new SocialAuthError('SOCIAL_SCRIPT_LOAD_FAILED', 'Google indisponible'))
      );
      return;
    }

    const script = document.createElement('script');
    script.src = GIS_SRC;
    script.async = true;
    script.defer = true;
    script.onload = () => resolve();
    script.onerror = () =>
      reject(
        new SocialAuthError(
          'SOCIAL_SCRIPT_LOAD_FAILED',
          'Impossible de charger le service Google. Vérifiez votre connexion.'
        )
      );
    document.head.appendChild(script);

    // Filet de sécurité : sans lui, un réseau bloqué (entreprise, DNS) laisserait
    // le bouton dans un état « chargement » éternel.
    window.setTimeout(() => {
      if (!window.google?.accounts?.id) {
        reject(
          new SocialAuthError(
            'SOCIAL_SCRIPT_LOAD_FAILED',
            'Le service Google a mis trop de temps à répondre.'
          )
        );
      }
    }, GIS_READY_TIMEOUT_MS);
  }).catch((error) => {
    // On allows a retry: the cached promise must not stay rejected.
    scriptPromise = null;
    throw error;
  });

  return scriptPromise;
}

/**
 * Déclenche la fenêtre Google et résout avec l'`id_token`.
 *
 * @throws SocialAuthError si le script est bloqué, si aucun client-id n'est
 *         configuré, ou si l'utilisateur ferme la fenêtre.
 */
export async function requestGoogleCredential(
  clientId: string
): Promise<string> {
  if (!clientId) {
    throw new SocialAuthError(
      SocialErrorCode.PROVIDER_NOT_CONFIGURED,
      'La connexion Google n’est pas configurée sur ce serveur.'
    );
  }

  await loadGoogleScript();

  const googleId = window.google?.accounts?.id;
  if (!googleId) {
    throw new SocialAuthError(
      'SOCIAL_SCRIPT_LOAD_FAILED',
      'Le service Google est indisponible.'
    );
  }

  return new Promise<string>((resolve, reject) => {
    let settled = false;
    const finish = (error?: SocialAuthError, credential?: string) => {
      if (settled) return;
      settled = true;
      if (error) reject(error);
      else resolve(credential as string);
    };

    googleId.initialize({
      client_id: clientId,
      ux_mode: 'popup',
      cancel_on_tap_outside: true,
      callback: (response) => {
        if (response?.credential) {
          finish(undefined, response.credential);
        } else {
          finish(
            new SocialAuthError(
              SocialErrorCode.CREDENTIAL_INVALID,
              'Google n’a pas renvoyé d’identité.'
            )
          );
        }
      },
    });

    try {
      googleId.prompt((notification: NonNullable<unknown>) => {
        const type = (notification as { type?: string } | undefined)?.type;
        // L'utilisateur a fermé la fenêtre : ce n'est pas une erreur serveur,
        // mais un silence propre (pas de message d'erreur en rouge).
        if (type === 'dismiss' || type === 'skipped' || type === 'canceled') {
          settled = true;
          googleId.cancel?.();
          return;
        }
        if (type && type !== 'display' && type !== 'isdisplayed') {
          finish(
            new SocialAuthError(
              'SOCIAL_SCRIPT_LOAD_FAILED',
              'La connexion Google n’a pas pu être lancée.'
            )
          );
        }
      });
    } catch {
      finish(
        new SocialAuthError(
          'SOCIAL_SCRIPT_LOAD_FAILED',
          'La connexion Google n’a pas pu être lancée.'
        )
      );
    }
  });
}

/** Réinitialise l'état (tests). */
export function resetGoogleScriptState(): void {
  scriptPromise = null;
}
