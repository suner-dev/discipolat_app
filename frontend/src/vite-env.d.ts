/// <reference types="vite/client" />

interface ImportMetaEnv {
  readonly VITE_API_URL: string;

  /**
   * Identifiants publics des fournisseurs d'identité externe.
   *
   * <p>Ce ne sont **pas** des secrets : ce sont des identifiants lisibles par le
   * navigateur, utilisés pour construire l'URL du dialogue du fournisseur. La
   * vérification du `id_token` (signature, audience, expiration) est faite par
   * le backend seul.
   *
   * <p>Le serveur reste la source de vérité pour l'affichage des boutons
   * (`GET /auth/social/providers`, fail-closed) : une valeur absente ici n'a pas
   * d'incidence sur la sécurité, seulement sur ce que le navigateur peut tenter.
   */
  readonly VITE_GOOGLE_CLIENT_ID?: string;
  readonly VITE_MICROSOFT_CLIENT_ID?: string;
  readonly VITE_MICROSOFT_TENANT_ID?: string;

  /** App ID Meta (public) + version de l'API attendue par l'application Meta. */
  readonly VITE_FACEBOOK_APP_ID?: string;
  readonly VITE_FACEBOOK_API_VERSION?: string;
}

interface ImportMeta {
  readonly env: ImportMetaEnv;
}
