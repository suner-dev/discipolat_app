import { SocialAuthError, SocialErrorCode } from './types';

/**
 * Facebook Login — flux OIDC par redirection.
 *
 * <h3>Pourquoi pas le SDK JavaScript de Facebook ?</h3>
 * `FB.login()` renvoie un **access token**, jamais un `id_token`. Or notre
 * architecture vérifie un **JWT signé** : c'est ce qui permet une vérification
 * locale, gratuite et illimitée, sans appel réseau par connexion
 * (`oauth2.googleapis.com/tokeninfo` ayant été écarté pour la même raison).
 * L'unique moyen d'obtenir un `id_token` vérifiable est donc le flux OIDC
 * implicite (`response_type=id_token`), qui renvoie le JWT dans le fragment de
 * l'URL.
 *
 * <h3>Pourquoi ce flux est acceptable ici</h3>
 * Le flux implicite est normalement déconseillé car le jeton passe par le
 * fragment de l'URL. Ici ce n'est pas un défaut :
 * <ul>
 *   <li>aucun jeton de rafraîchissement fournisseur n'est nécessaire — c'est
 *       NOTRE backend qui émet la session Discipolat ;</li>
 *   <li>le jeton est à durée de vie courte et lié à notre App ID (audience) ;</li>
 *   <li>la vérification de signature, d'audience et d'expiration est faite
 *       côté serveur, jamais dans le navigateur.</li>
 * </ul>
 *
 * <h3>Redirection de page entière, et non popup</h3>
 * Le popup par `postMessage` est fragile : il casse avec
 * `Cross-Origin-Opener-Policy: same-origin`, et Meta impose son propre
 * `X-Frame-Options`. On utilise donc une **redirection de page entière** avec
 * un relais par `sessionStorage` : pas de popup, pas de COOP, et le même code
 * fonctionne dans un navigateur mobile.
 *
 * <h3>Protection CSRF</h3>
 * Un `state` aléatoire (32 octets, `crypto.getRandomValues`) est comparé au retour.
 * Sans lui, un attaquant pourrait injecter SON `id_token` dans le flux de
 * connexion d'un utilisateur.
 */

const STORAGE_KEY = 'discipolat:social-handoff';

/** Durée de validité du relais : au-delà, le jeton est considéré périmé. */
const HANDOFF_TTL_MS = 5 * 60 * 1000;

const AUTH_BASE = 'https://www.facebook.com';

export interface FacebookStartOptions {
  appId: string;
  apiVersion?: string;
  /** URL de retour enregistrée dans l'application Meta. */
  redirectUri: string;
}

/**
 * Lance le flux Facebook : construit l'URL d'autorisation, y injecte un
 * `state` aléatoire, puis redirige l'utilisateur.
 */
export function startFacebookLogin(options: FacebookStartOptions): void {
  if (!options.appId) {
    throw new SocialAuthError(
      SocialErrorCode.PROVIDER_NOT_CONFIGURED,
      'La connexion Facebook n’est pas configurée sur ce serveur.'
    );
  }

  const state = randomState();
  sessionStorage.setItem(
    STORAGE_KEY,
    JSON.stringify({ state, provider: 'facebook', issuedAt: Date.now() })
  );

  const params = new URLSearchParams({
    client_id: options.appId,
    redirect_uri: options.redirectUri,
    response_type: 'id_token',
    scope: 'email,public_profile',
    state,
  });

  const version = options.apiVersion || 'v21.0';
  window.location.assign(`${AUTH_BASE}/${version}/dialog/oauth?${params.toString()}`);
}

/** Lecture et validation du relais au retour de la redirection. */
export function readFacebookCredential(): string {
  const raw = sessionStorage.getItem(STORAGE_KEY);
  sessionStorage.removeItem(STORAGE_KEY);

  if (!raw) {
    throw new SocialAuthError(
      'SOCIAL_HANDOFF_MISSING',
      'Session de connexion Facebook expirée. Recommencez.'
    );
  }

  let handoff: { state?: string; provider?: string; issuedAt?: number };
  try {
    handoff = JSON.parse(raw);
  } catch {
    throw new SocialAuthError('SOCIAL_HANDOFF_MISSING', 'Session de connexion Facebook invalide.');
  }

  // Une older de quelques minutes est la limite : au-delà, on ne veut pas
  // réaccepter un jeton rejoué plus tard depuis l'historique du navigateur.
  if (
    typeof handoff.issuedAt !== 'number' ||
    Date.now() - handoff.issuedAt > HANDOFF_TTL_MS
  ) {
    throw new SocialAuthError(
      'SOCIAL_HANDOFF_MISSING',
      'Session de connexion Facebook expirée. Recommencez.'
    );
  }

  const fragment = new URLSearchParams(window.location.hash.replace(/^#/, ''));
  const error = fragment.get('error');
  if (error) {
    throw new SocialAuthError(
      error === 'access_denied' ? 'SOCIAL_LOGIN_CANCELLED' : SocialErrorCode.CREDENTIAL_REJECTED,
      error === 'access_denied'
        ? 'Connexion Facebook annulée.'
        : 'Facebook a refusé la connexion.'
    );
  }

  const credential = fragment.get('id_token');
  const returnedState = fragment.get('state');

  if (!credential) {
    throw new SocialAuthError(
      SocialErrorCode.CREDENTIAL_INVALID,
      'Facebook n’a pas renvoyé d’identité.'
    );
  }

  // Contrôle CSRF : l'état doit correspondre à celui émis. Comparaison
  // stricte, et le message ne révèle rien du state attendu.
  if (!returnedState || returnedState !== handoff.state) {
    throw new SocialAuthError(
      'SOCIAL_STATE_MISMATCH',
      'Connexion Facebook non sécurisée. Recommencez.'
    );
  }

  // Nettoyage de l'URL : le jeton ne doit pas rester dans l'historique.
  window.history.replaceState({}, document.title, window.location.pathname + window.location.search);

  return credential;
}

/** Nettoie le relais (annulation, erreur). */
export function clearFacebookHandoff(): void {
  sessionStorage.removeItem(STORAGE_KEY);
}

function randomState(): string {
  const bytes = new Uint8Array(32);
  crypto.getRandomValues(bytes);
  let out = '';
  bytes.forEach((byte) => {
    out += byte.toString(16).padStart(2, '0');
  });
  return out;
}

/** URL de retour à enregistrer dans l'application Meta. */
export function facebookRedirectUri(): string {
  return `${window.location.origin}/auth/social/callback`;
}
