/**
 * Destination APRÈS authentification — partagé par tous les chemins de connexion.
 *
 * <p>Ces chemins se passent le relais par `?next=` : `/j/:slug` émet
 * `/login?next=/j/<slug>`, `/login` relaie vers `/verify-2fa?next=…`, etc. Un
 * `?next=` non lu est une PROMESSE MORTE : l'utilisateur est ramené au tableau
 * de bord et perd silencieusement l'église qu'il venait de choisir.
 *
 * <p>Ce module existe pour que le FILTRE soit défini une seule fois. Dupliquer
 * une validation de redirection sur trois pages, c'est trois occasions d'en
 * oublier une — et une occasion d'oublier devient une open-redirect.
 */

/**
 * N'accepte qu'un chemin INTERNE de cet hôte.
 *
 * <p>Refusés :
 * <ul>
 *   <li>`https://evil.example`, `javascript:…` — pas de racine `/` ;</li>
 *   <li>`//evil.example` et `/\evil.example` — URLs protocol-relative : le
 *       navigateur les traite comme absolues. Le second est refusé aussi parce
 *       que les navigateurs normalisent `\` en `/` dans une URL ;</li>
 *   <li>`/https://evil.example` — schéma explicite remonté en chemin.</li>
 * </ul>
 *
 * <p>Acceptés : `/j/x`, `/join?code=ABC`, `/dashboard`, `/eglises`…
 */
export function safeInternalPath(raw: string | null | undefined): string | null {
  if (!raw) return null;
  const path = raw.trim();
  if (!path.startsWith('/')) return null;
  // Protocol-relative : `//hôte` et `/\hôte`.
  if (path.startsWith('//') || path.startsWith('/\\')) return null;
  // Schéma explicite remonté en chemin : `/https://evil.example`.
  if (/^\/[a-z][a-z0-9+.-]*:/i.test(path)) return null;
  return path;
}

/** Clé du relais pour les flux qui reviennent par un autre chemin que l'URL. */
const RELAY_KEY = 'discipolat.postAuthPath';

/**
 * Retient une destination pour un flux qui NE PEUT PAS la transporter dans son
 * propre lien — aujourd'hui le magic link, dont l'URL est construite par le
 * backend (`frontendUrl + "/auth/magic-link?token=" + …`) et ne peut donc pas
 * embarquer de `next`.
 *
 * <p>Le relais est volontairement :
 * <ul>
 *   <li><b>par onglet</b> (`sessionStorage`) : le magic link arrive par email,
 *       parfois sur un autre appareil — là, le relais ne s'applique pas et le
 *       comportement reste `/dashboard`, exactement comme aujourd'hui ;</li>
 *   <li><b>consommé à la lecture</b> : une destination ne doit pas survivre à
 *       son usage, sinon elle ressusciterait plus tard ;</li>
 *   <li><b>filtré par {@link safeInternalPath}</b> : `sessionStorage` est
 *       modifiable par un script tiers sur la même origine — c'est une entrée
 *       non fiable, donc elle passe par le même filtre que l'URL.</li>
 * </ul>
 */
export function rememberPostAuthPath(path: string | null): void {
  try {
    if (path) sessionStorage.setItem(RELAY_KEY, path);
    else sessionStorage.removeItem(RELAY_KEY);
  } catch {
    // Navigation privée / quota : le relais est un confort, jamais un contrat.
  }
}

/** Relit puis CONSOMME la destination retenue. Renvoie `null` si absente ou refusée. */
export function takeRememberedPostAuthPath(): string | null {
  try {
    const raw = sessionStorage.getItem(RELAY_KEY);
    sessionStorage.removeItem(RELAY_KEY);
    return safeInternalPath(raw);
  } catch {
    return null;
  }
}
