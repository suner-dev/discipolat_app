/**
 * Connexion par identité externe — types partagés (web).
 *
 * <p>Les fournisseurs exposés ici sont **gratuits et sans plafond** :
 * le backend vérifie les `id_token` localement contre les clés publiques
 * (JWKS), donc aucun appel à un service de vérification payant et aucun quota.
 * Apple (99 $/an) et le SMS (facturé) sont volontairement hors
 * périmètre — voir `docs/AUTH_SOCIAL.md`.
 */

/** Fournisseurs acceptés par l'API. La liste est fermée côté serveur. */
export type SocialProviderId = 'google' | 'microsoft' | 'facebook';

/** Réponse de `GET /api/v1/auth/social/providers`. */
export interface SocialProvidersState {
  /** Fournisseurs réellement prêts côté serveur (bouton affiché si présent). */
  providers: Array<{ provider: SocialProviderId; label: string }>;
  /** Le serveur autorise-t-il le rattachement d'une identité à un compte ? */
  accountLinkingEnabled: boolean;
}

/** Charge utile de `POST /api/v1/auth/social/{provider}` (miroir de `/auth/login`). */
export interface SocialAuthTokens {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  userId: string;
  email: string;
  role: string;
  roles: string[];
  activeRole: string;
  estChefDeFamille: boolean;
  firstName?: string | null;
  lastName?: string | null;
  twoFactorEnabled: boolean;
  platformRoles: string[];
  platformSuperAdmin: boolean;
}

/** Réponse de `POST /api/v1/admin/invitations/accept-identity/{token}`. */
export interface SocialInvitationAcceptance {
  success: boolean;
  provider: SocialProviderId;
  identityCreated: boolean;
  userId: string;
  email: string;
  tenantId: string;
  alreadyMember: boolean;
  crossTenantIdentity: boolean;
  welcomeEmailSent: boolean;
  /** Jeton de session : l'invité est directement connecté après acceptation. */
  accessToken: string;
  refreshToken: string;
  role: string;
  roles: string[];
  activeRole: string;
  firstName?: string | null;
  lastName?: string | null;
  platformRoles: string[];
  platformSuperAdmin: boolean;
  message?: string;
}

/**
 * Codes d'erreur du serveur, dans le `title` du ProblemDetail (RFC 7807).
 *
 * <p>Ils permettent un comportement honnête plutôt qu'un message générique :
 * `SOCIAL_ACCOUNT_NOT_LINKED` doit proposer le lien d'invitation,
 * `SOCIAL_PROVIDER_NOT_CONFIGURED` doit disparaître de l'interface.
 */
export const SocialErrorCode = {
  CREDENTIAL_INVALID: 'SOCIAL_CREDENTIAL_INVALID',
  CREDENTIAL_REJECTED: 'SOCIAL_CREDENTIAL_REJECTED',
  PROVIDER_NOT_CONFIGURED: 'SOCIAL_PROVIDER_NOT_CONFIGURED',
  ACCOUNT_NOT_LINKED: 'SOCIAL_ACCOUNT_NOT_LINKED',
  EMAIL_NOT_VERIFIED: 'SOCIAL_EMAIL_NOT_VERIFIED',
  EMAIL_MISSING: 'SOCIAL_EMAIL_MISSING',
  EMAIL_MISMATCH: 'SOCIAL_EMAIL_MISMATCH',
  IDENTITY_ALREADY_LINKED: 'SOCIAL_IDENTITY_ALREADY_LINKED',
  PROVIDER_ALREADY_LINKED: 'SOCIAL_PROVIDER_ALREADY_LINKED',
  TENANT_NOT_ALLOWED: 'SOCIAL_TENANT_NOT_ALLOWED',
  IDENTITY_ORPHANED: 'SOCIAL_IDENTITY_ORPHANED',
  ACCOUNT_NOT_ACTIVATED: 'ACCOUNT_NOT_ACTIVATED',
  ACCOUNT_INACTIVE: 'ACCOUNT_INACTIVE',
  ACCOUNT_LOCKED: 'ACCOUNT_LOCKED',
  LINKING_DISABLED: 'SOCIAL_LINKING_DISABLED',
  RATE_LIMITED: 'RATE_LIMITED',
} as const;

export type SocialErrorCodeValue =
  (typeof SocialErrorCode)[keyof typeof SocialErrorCode];

/** Erreur normalisée d'un échec de connexion sociale. */
export class SocialAuthError extends Error {
  readonly code: string;
  readonly status?: number;

  constructor(code: string, message: string, status?: number) {
    super(message);
    this.name = 'SocialAuthError';
    this.code = code;
    this.status = status;
  }

  /** Le compte doit être créé/rattaché via une invitation : l'UI doit le dire. */
  get requiresInvitation(): boolean {
    return this.code === SocialErrorCode.ACCOUNT_NOT_LINKED;
  }

  /** Le serveur ne sert pas ce fournisseur : l'UI doit masquer le bouton. */
  get providerUnavailable(): boolean {
    return this.code === SocialErrorCode.PROVIDER_NOT_CONFIGURED;
  }
}
