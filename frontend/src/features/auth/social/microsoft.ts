import { SocialAuthError, SocialErrorCode } from './types';

/**
 * Microsoft Entra ID — récupération d'un `id_token`.
 *
 * <p>Implémentation : `@azure/msal-browser`, bibliothèque officielle et gratuite,
 * importée **dynamiquement**. Conséquences :
 * <ul>
 *   <li>MSAL n'est pas dans le bundle initial : il n'est téléchargé que si
 *       l'utilisateur clique sur « Microsoft » ;</li>
 *   <li>MSAL gère PKCE, CORS, et le renouvellement — donc <b>aucune
 *       implémentation maison de cryptographie</b> dans ce dépôt ;</li>
 *   <li>le client public n'a besoin que de son {@code client_id} : aucun secret
 *       dans le navigateur (le serveur ne stocke que le même client-id public,
 *       et valide la signature via JWKS).</li>
 * </ul>
 */

const SCOPE = 'openid email profile';

interface MsalModule {
  PublicClientApplication: new (config: {
    auth: { clientId: string; authority: string; redirectUri: string };
  }) => {
    initialize(): Promise<void>;
    loginPopup(request: { scopes: string[] }): Promise<{
      account: { username: string; localAccountId: string; name?: string };
      idToken: string;
    }>;
  };
}

let msalPromise: Promise<MsalModule> | null = null;
let instanceCache: { clientId: string; app: InstanceType<MsalModule['PublicClientApplication']> } | null =
  null;

function loadMsal(): Promise<MsalModule> {
  if (!msalPromise) {
    msalPromise = import('@azure/msal-browser')
      .then((module) => module as unknown as MsalModule)
      .catch(() => {
        msalPromise = null;
        throw new SocialAuthError(
          'SOCIAL_SCRIPT_LOAD_FAILED',
          'Impossible de charger la connexion Microsoft.'
        );
      });
  }
  return msalPromise;
}

/**
 * Déclenche la fenêtre Microsoft et résout avec l'`id_token`.
 *
 * @param clientId  client-id public de l'enregistrement d'application
 * @param tenantId  tenant attendu (`common` par défaut)
 * @throws SocialAuthError si MSAL est bloqué, si aucun client-id n'est
 *         configuré, ou si l'utilisateur ferme la fenêtre
 */
export async function requestMicrosoftCredential(
  clientId: string,
  tenantId = 'common'
): Promise<string> {
  if (!clientId) {
    throw new SocialAuthError(
      SocialErrorCode.PROVIDER_NOT_CONFIGURED,
      'La connexion Microsoft n’est pas configurée sur ce serveur.'
    );
  }

  const msal = await loadMsal();
  if (!instanceCache || instanceCache.clientId !== clientId) {
    const app = new msal.PublicClientApplication({
      auth: {
        clientId,
        authority: `https://login.microsoftonline.com/${encodeURIComponent(tenantId)}`,
        // URI de redirection : indispensable pour MSAL, mais le flux retenu est
        // la popup, donc aucune navigation réelle.
        redirectUri: window.location.origin,
      },
    });
    await app.initialize();
    instanceCache = { clientId, app: app as InstanceType<MsalModule['PublicClientApplication']> };
  }

  try {
    const result = await instanceCache.app.loginPopup({ scopes: [SCOPE] });
    if (!result?.idToken) {
      throw new SocialAuthError(
        SocialErrorCode.CREDENTIAL_INVALID,
        'Microsoft n’a pas renvoyé d’identité.'
      );
    }
    return result.idToken;
  } catch (error) {
    if (error instanceof SocialAuthError) throw error;
    // L'utilisateur a ferme la popup : silence honnete plutot qu'« erreur ».
    const message =
      (error as { errorCode?: string })?.errorCode ?? '';
    if (/user_cancelled|popup.*closed|cancelled/i.test(message)) {
      throw new SocialAuthError(
        'SOCIAL_LOGIN_CANCELLED',
        'Connexion Microsoft annulée.'
      );
    }
    throw new SocialAuthError(
      SocialErrorCode.CREDENTIAL_REJECTED,
      'Connexion Microsoft impossible. Veuillez réessayer.'
    );
  }
}

/** Réinitialise l'état (tests). */
export function resetMicrosoftState(): void {
  msalPromise = null;
  instanceCache = null;
}
