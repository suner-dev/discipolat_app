# ADR-007 — Identité : un seul contexte `identity`, OIDC + SCIM, autorisation policy-driven

> **Statut** : **Accepté sur le principe** (humain 2026-10-09) — **exécution non démarrée**, et
> **volontairement placée après ADR-005 et ADR-006** : l'IAM est le point de rupture le plus cher,
> on ne l'ouvre que quand le modèle d'argent et le flux de données sont posés. Aucun code touché.
> **Date** : 2026-10-09. **Mesures** : exécutées sur `main` @ `9148caa1`.
> **Couvre** la plaque **P1** de [VALORISATION-PLATEFORME.md](VALORISATION-PLATEFORME.md) §2.

---

## 1. Ce qui existe déjà — et ce n'est pas rien

| Brique | État mesuré | Où |
|---|---|---|
| **JWT asymétrique RS256** | `Jwts.SIG.RS256`, clé privée injectée par env/fichier (`JWT_PRIVATE_KEY`, `JWT_PRIVATE_KEY_PATH`) ; **pas de secret partagé à distribuer** aux clients | `common/infrastructure/security/JwtTokenProvider.java:105,137,182` |
| **Durées correctes** | access **15 min**, refresh **7 jours** (`ACCESS_TOKEN_VALIDITY_MINUTES = 15`, `REFRESH_TOKEN_VALIDITY_DAYS = 7`, reprises dans `application.yml:97-98`) | idem + `backend/src/main/resources/application.yml` |
| **Rotation + révocation des refresh tokens** | entités/services `RefreshTokenSession`, `RevokedToken`, `TokenRevocationService` : la session est un objet, pas une chaîne opaque perdue | `modules/security/domain/` (6 fichiers) |
| **2FA** | `TwoFactorService` + `TwoFactorController`/`TwoFactorSetupResponse` | `modules/authentication/` |
| **Vérification d'identité sociale OIDC** | `OidcSocialIdentityVerifier`, `SocialIdentityVerifier`, `SocialProvider`, `SocialAuthProperties` + **audit au démarrage** (`SocialAuthStartupAudit`) | `modules/authentication/{domain,config}/` |
| **Changement de rôle explicite** | `SwitchRoleRequest` — le rôle actif est une notion du produit (scoping par rôle : `FAISEUR`, `CHEF_DE_FAMILLE`, `RESPONSABLE`) | `modules/authentication/api/`, `WorkspaceScopeService` |
| **Multi-tenant dans le jeton** | émission avec `targetTenantId` (parcours admin/impersonation), `CurrentTenantResolver`, `TenantFilterIntegrator` | `JwtTokenProvider:165`, `common/multitenancy` |

Contexte aujourd'hui réparti sur **deux** modules : `authentication` (31 fichiers) et `security`
(6 fichiers), plus `users`, `tenants`, `scoping`, `compliance` qui touchent à l'identité.

## 2. Les trois limites mesurées

| # | Limite | Mesure | Ce que ça coûte |
|---|---|---|---|
| 1 | **Autorisation codée dans les adaptateurs** : `@PreAuthorize` apparaît **1 001 fois dans 231 fichiers** | `grep -r "@PreAuthorize" backend/src/main/java \| wc -l` | aucune réponse possible à « qui peut faire quoi ? » sans lire 231 fichiers ; un changement de politique = une PR monstre ; **impossible de vendre une politique par client** |
| 2 | **Pas de provisioning enterprise** : **0** occurrence de SCIM | `grep -rli "scim" backend/src/main/java \| wc -l` → 0 | un client à 5 000 membres gère les comptes à la main → le churn est mécanique, et l'argument « enterprise » tombe |
| 3 | **Pas d'IdP** : le produit **consomme** de l'OIDC (réseaux sociaux) mais n'en **émet** pas ; 0 occurrence de `openpolicyagent`/`cerbos` | idem | pas de SSO d'entreprise (Entra ID / Google Workspace / Okta) → exclusion des appels d'offres, et NRR enterprise inaccessible |

## 3. Décision

1. **Un seul contexte `identity`** : `authentication` + `security` fusionnés (déplacement de code,
   comportement identique), et `users`/`tenants`/`scoping` ne gardent que ce qui est métier. C'est le
   **deuxième** pilote de couches propres après `governance/departments` (arbitrage V0-B), et il est
   choisi parce que sa surface de contrat est **petite** (le login, le refresh, le rôle) et son
   invariante est **forte**.
2. **Le produit devient émetteur OIDC** (provider) **en plus** de rester ce qu'il est :
   *validation* des jetons existants. Deux émissions coexistent pendant la migration
   (**double émission** : mêmes claims, deux émetteurs) ; aucun client n'est forcé de basculer.
   Choix d'implémentation : d'abord **Keycloak auto-hébergé** devant le schéma existant (coût
   d'intégration, pas de réécriture), et la question « provider interne (Spring Authorization
   Server) ou externe » n'est tranchée qu'au moment de l'exécution, avec une mesure de charge.
3. **SCIM 2.0** côté serveur : `/scim/v2/Users`, `/scim/v2/Groups` adossés au contexte `identity`.
   C'est la porte d'entrée des achats de siège en self-serve et des DSI clientes.
4. **Autorisation policy-driven** : le *quoi* (droit) est séparé du *où* (scope : tenant, famille,
   département, unité d'organisation) et du *qui* (rôle actif). La décision devient une fonction
   `can(acteur, action, ressource, contexte)` évaluée par un **moteur de politique versionné**
   (OPA/Cerbos, ou un évaluateur interne si la dépendance est refusée) et **testable**. Les 1 001
   `@PreAuthorize` sont migrés **progressivement, écran par écran**, chaque migration étant prouvée
   par un test d'autorisation *avant/après* identique (aucun élargissement de droit toléré).
5. **Journal d'authentification et d'élévation de privilège** append-only (qui, d'où, quel rôle
   actif, quel tenant, succès/échec) : condition SOC 2 / ISO 27001, et aujourd'hui non couvert par
   l'`audit` applicatif ordinaire.
6. **Secrets de jeton** : la paire RS256 reste hors du dépôt ; la rotation de clés (**jwks** exposé,
   `kid` dans l'en-tête) devient obligatoire, car un acquéreur technique demandera « quand avez-vous
   changé la clé pour la dernière fois ? ». Aujourd'hui : **aucun `kid` et aucun endpoint `jwks` pour
   les jetons émis** par Discipolat (les `RemoteJWKSet` de Nimbus rencontrés dans le code ne concernent
   que la **vérification** des jetons sociaux **entrants** — `OidcSocialIdentityVerifier:380-386`).

## 4. Règles non négociables (pendant la migration)

- **Aucun élargissement de droit** : si un test d'autorisation change de résultat, la PR est refusée.
- **Aucun big-bang d'authentification** : double émission + bascule par client, avec rollback
  possible par feature flag.
- **Aucune migration des mots de passe en clair, aucune réinitialisation de force** : si un
  fournisseur externe est introduit, la projection se fait par import haché, et l'utilisateur
  garde son mot de passe.
- **Le gel ArchUnit ne monte pas** : la fusion `authentication`+`security` doit *retirer* des
  arêtes R3, pas en ajouter.

## 5. Options rejetées (et pourquoi)

| Option | Rejetée parce que |
|---|---|
| Refondre l'auth autour d'un SaaS (Auth0/Clerk/WorkOS) dès maintenant | lie la rétention enterprise (le vrai levier) à un tiers, et **externalise l'isolation multi-tenant** déjà codée (`TenantFilterIntegrator`, sharding) ; de plus les rôles actifs et le scoping famille/département n'existent nulle part sur étagère |
| Garder `@PreAuthorize` et documenter les droits dans un wiki | c'est exactement ce que la due-diligence ne croit pas ; une politique non exécutable n'est pas une politique |
| Fusionner `identity` dans `users` | `users` porte le profil métier (disciples, familles, historique) ; mélanger identité et CRM est le piège qui rend l'extraction impossible plus tard |
| RBAC strict (rôles seulement) | insuffisant : les droits dépendent déjà du **scope actif** (famille, département, unité d'organisation) et de la ressource ; sans `can(...)`, on réécrit la même logique dans chaque contrôleur |
| Tout faire maintenant, avant ADR-005 | l'IAM est le point de rupture **le plus cher** du produit : une panne d'auth = plus aucun client ne peut travailler. Le ledger et le flux n'ont pas ce rayon de casse |

## 6. Lot exécutable (après ADR-005 et ADR-006) — rouge puis verte

| Tâche | Contenu | Rouge | Verte |
|---|---|---|---|
| **I1** | Contexte `identity` unique (déplacement pur), couches ADR-002 | — | les tests `authentication` + `security` passent **sans modification** ; R3 ne monte pas |
| **I2** | `can(acteur, action, ressource, contexte)` + moteur de politique versionné, **sans** toucher aux contrôleurs | un droit aujourd'hui implicite rendu **explicite et testé** : le test échoue tant que la politique n'existe pas | 100 % des parcours sensibles couverts par un test de politique, et table de décision publiée |
| **I3** | Migrer les 1 001 `@PreAuthorize` par vagues (écran par écran) | un écran migré **sans** test d'équivalence → refus | diff de droits vide (prouvé par test d'autorisation avant/après) |
| **I4** | Émission OIDC + jwks + rotation de clés (`kid`) | rejouer un jeton signé avec l'ancienne clé après rotation doit être **refusé** | double émission fonctionne, rotation testée de bout en bout |
| **I5** | SCIM 2.0 + journal d'authentification append-only | un client SCIM standard (Okta/Entra) ne trouve rien aujourd'hui | provisioning/déprovisioning vérifié par un run d'interopérabilité ; chaque élévation de privilège traçable |

**Gates** : `mvn -o test` inchangé et vert ; gates PostgreSQL 19/19 ; **tests de contrat d'auth**
(`/api/v1/auth/*`) gelés ; FE et mobile inchangés pendant I1/I2 ; gel ArchUnit non augmenté.

**Contrainte mesurée** : `authentication` (comme `users`, `tenants`, `scoping`, `core`) est **dans le
SCC de 41 contexts** (rejoué le 2026-10-09 par `mvn -o test -Dtest=ArchitectureRulesTest`, rapport
`target/architecture-report.txt`). Donc I1 est un **déplacement dans le monolithe**, pas une
extraction : c'est justement pour cela qu'il est classé après V0-F, et que la policy `can(...)` (I2)
doit être posée **avant** toute idée de service identité autonome.

## 7. Comment revérifier les faits

```bash
grep -r "@PreAuthorize" backend/src/main/java --include=*.java | wc -l                    # 1 001
grep -rl "@PreAuthorize" backend/src/main/java --include=*.java | wc -l                   # 231
grep -rli "scim\|openpolicyagent\|cerbos" backend/src/main/java | wc -l                   # 0
grep -n "ACCESS_TOKEN_VALIDITY_MINUTES\|REFRESH_TOKEN_VALIDITY_DAYS" \
  backend/src/main/java/com/discipolat/common/infrastructure/security/JwtTokenProvider.java  # 15 / 7
ls backend/src/main/java/com/discipolat/modules/security/domain/                          # rotation + revocation
find backend/src/main/java/com/discipolat/modules/authentication -name '*.java' | wc -l   # 31
```
