# SECURITY — Discipolat

> Politique de sécurité et de signalement de vulnérabilités.
> Ce fichier racine est le point d'entrée qu'un évaluateur/acquéreur lit en
> premier. Le détail technique vit dans [docs/SECURITY.md](docs/SECURITY.md),
> [docs/RBAC.md](docs/RBAC.md) et [docs/security/SECURITY_MATRIX.md](docs/security/SECURITY_MATRIX.md).

## 1. Signalement d'une vulnérabilité (divulgation responsable)

**Ne signalez PAS une vulnérabilité via une issue publique.**

Merci d'envoyer un rapport à **security@discipolat.com** (ou, à défaut,
support@discipolat.com) décrivant :

1. Le composant touché (backend / web / mobile) et sa version ;
2. La nature de la vulnérabilité et son impact potentiel ;
3. Les étapes de reproduction (proof-of-concept si possible) ;
4. Votre suggestion de correctif, le cas échéant.

**Engagements de l'équipe :**
- Accusé de réception sous **72 h ouvrées**.
- Évaluation de la sévérité (CVSS v3.1) et plan de correction communiqués.
- Correctif priorisé selon la sévérité : critique < 7 jours, haute < 30 jours.
- Crédit du chercheur dans l'avis de sécurité (avec son accord).

## 2. Périmètre couvert

| Surface | Éléments |
|---|---|
| Backend Spring Boot | API REST (`/api/v1/**`), WebSocket/STOMP, uploads, exports |
| Frontend web | SPA React (PWA), stockage local, i18n |
| Mobile | application Flutter, base locale Drift, notifications FCM |
| Infra | configuration de déploiement, secrets, chaîne CI/CD |

## 3. Mesures de sécurité en place (vue d'ensemble)

- **Authentification** : JWT asymétrique **RS256** (clés RSA hors dépôt), access
  15 min / refresh 7 j avec rotation ; clés gérées hors dépôt (`setup-keys.sh`).
- **2FA TOTP** : double facteur disponible (`TwoFactorService`).
- **Autorisation** : RBAC serveur centralisé (`@PreAuthorize` + `AuthorizationService`),
  hiérarchie de 9 rôles, jamais de décision d'autorisation prise côté client.
- **Isolation multi-tenant** : filtre Hibernate + `TenantContext` résolu **depuis
  le JWT uniquement** (jamais depuis le client) ; `findById` rendu tenant-aware
  globalement (anti-IDOR). Vérifié bout-en-bout par
  `TenantModuleIsolationEndToEndHttpTest` (bloquant en CI).
- **Chiffrement** : données sensibles chiffrées au repos (AES-256-GCM), mots de
  passe BCrypt (coût 12).
- **Réseau** : rate limiting distribué (Bucket4j + Redis), CORS en liste blanche,
  TLS 1.2+.
- **Audit** : journal d'audit à chaîne de hachage (`prev_hash`/`hash`) sur les
  mutations.
- **Chaîne CI** : scan de secrets (gitleaks), revue des dépendances, `npm audit`
  bloquant ; OWASP `dependency-check` pour l'écosystème Java (voir §5).

## 4. Schéma de production (honnêteté)

- `ddl-auto: none` en production ; le schéma est **exclusivement** piloté par les
  migrations Flyway (fail-fast au démarrage).
- La chaîne complète des migrations est validée sur **PostgreSQL réel** en CI via
  Testcontainers (`FlywayMigrationChainPostgreSqlTest`) : application de toutes
  les migrations sans erreur, `validate()` propre (aucune dérive entité/schéma),
  et neutralisation des comptes de démonstration en bout de chaîne.

## 5. Analyse des dépendances (SCA)

- **Java** : scan OWASP `dependency-check` exécuté par le job CI dédié
  « owasp-backend » (`.github/workflows/security.yml`), rapport HTML conservé en
  artefact, **non bloquant** (sans clé NVD, le débit gratuit rendrait la porte
  aléatoire). La **porte bloquante** côté dépendances Java est `dependency-review`
  (GitHub Advisory Database, sévérité high+ sur chaque PR). Voir `security.yml`.
- **Frontend** : `npm audit --omit=dev --audit-level=high` (bloquant en CI).
- **Mobile** : chaîne verrouillée par `pubspec.lock` ; scan en CI.

## 6. Portée et limites connues

Les limites, dettes et arbitrages de sécurité **connus et assumés** sont listés
honnêtement et datés dans [KNOWN_ISSUES.md](KNOWN_ISSUES.md) — ce document est
maintenu à jour et prime sur toute ancienne affirmation.
