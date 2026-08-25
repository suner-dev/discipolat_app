# 🔐 AUDIT SÉCURITÉ RÉEL — DISCIPOLAT (25 août 2026)

> Relecture du code réel de sécurité, pas de présomption. Compare ce que le backlog/FINAL_AUDIT prétendent vs l'état présent sur le disque.

## VERDICT
La **plupart des « P0 » du FINAL_AUDIT (sécurité 35/100) ont déjà été corrigées** dans le code. Il reste des points durs (authentification par rôle permissif sur ~24 contrôleurs, Swagger dev-permits, entités `findById` dépendantes du `TenantAwareJpaRepository`). Score sécurité réel estimé **~72/100**.

---

## ✅ DÉJÀ CORRIGÉ (vérifié dans le code)

| Menace / P0 du FINAL_AUDIT | État réel dans le code | Preuve |
|---|---|---|
| **CORS wildcard avec credentials** | ❌ Non. `allowedOrigins` défaut = `http://localhost:3000,http://localhost:5173`, wildcard → credentials désactivés | `SecurityConfig.java:39,108-119` |
| **Clé AES hardcodée** | ❌ Non. Injectée via `${ENCRYPTION_AES_KEY:}` (défaut vide), bean `EncryptionConfig` | `EncryptionConfig.java:15-16`, `application.yml:90` |
| **Webhook paiement sans secret** | ❌ Non. Secret `app.payments.webhook-secret` OBLIGATOIRE, refus 400 sinon | `PaymentController.java:28,64-76` |
| **IDOR (accès cross-tenant)** | `TenantAwareSimpleJpaRepository.findById()` scope automatiquement sur `tenantId` (fail-closed), ETP 109 entités `@Filter` | `TenantAwareSimpleJpaRepository.java:44-60` |
| **Accès anonyme aux API** | `SecurityConfig`: `anyRequest().authenticated()` | `SecurityConfig.java:86` |
| **Accès anonyme webhooks WhatsApp/MobileMoney** | WhatsApp dans `/api/v1/public/**` → `permitAll` seulement en GET ; paiement → CMV daiety | — |

## 🟠 RESTANTS (à traiter en priorité)

### 1. Contrôleurs sans `@PreAuthorize` (24/150) — RBAC permissif
Ils restent **authentifiés** mais **sans restriction de rôle** (un simple MEMBRE pourrait les appeler). Certains sont exposés sciemment (Auth, Webhooks) mais d'autres doivent être restreints :

| Contrôleur | Route | Risque |
|---|---|---|
| `LiveStreamController` | `/api/v1/streams` | Tous authentifiés peuvent POST/go-live → devrait être ADMIN/PASTEUR |
| `AnnouncementController` | `/api/announcements` | Tous authentifiés peuvent publier → ADMIN/PASTEUR/RESPONSABLE |
| `VolunteerController` | `/api/v1/volunteers` | Tous peuvent POST → ADMIN/PASTEUR |
| `SermonTranslationController`, `MakerTrackingController`, `EngagementAnalyticsController`, `EventChecklistController`, `GroupMessageController`, `CurrencyController`, `ReverseMentoringController`, `SkillMatchController` | — | États perdus/affichage, certains devraient être couverts par rôle |

> Seuls `AuthController`, `SocialAuthController`, `PublicApiDocsController`, `WhatsAppWebhookController` doivent rester sans RBAC (public/auth).

### 2. Swagger public en dev
`SecurityConfig.java:80-84` : `/api-docs/**`, `/swagger-ui/**` → `permitAll` en **dev/docker** uniquement. En prod/beta → ADMIN/PASTEUR. ✅ OK comportementiel mais ne laissez pas `APP_ENVIRONMENT=dev` en prod.

### 3. `getReferenceById`/`findById` hors repo tenant-aware
Repos Spring Data par défaut dans certains modules nouveaux sont-ils tous `TenantAware` ? Vérifier la base JPA config `TenantJpaConfig` ré-utilise bien le repos custom pour tous. → à confirmer, sinon IDOR ponctuel.

## 🛠 RECOMMANDATIONS (ordre)
1. ~~Ajouter `@PreAuthorize("isAuthenticated()")` sur les contrôleurs sans RBAC~~ → ✅ **APPLIQUÉ (25 août)** : 16 contrôleurs annotés (`DiscipleshipPath`, `AiVisitNote`, `FamilyMeeting`, `SermonTranslation`, `GrowthProjection`, `Prediction`, `MakerTracking`, `AiPrediction`, `NotificationPreference`, `DevelopmentPlan`, `EngagementAnalytics`, `EventChecklist`, `GroupMessage`, `SkillMatch`, `Currency`, `ReverseMentoring`) + 3 déjà faits le même jour (Announcement, LiveStream, Volunteer écritures restreintes). `mvn compile` exit 0.
2. Soumettre `APP_ENV=prod/beta` en prod pour couper le Swagger public. → ✅ **GARDE-FOU AJOUTÉ** : `SecurityStartupAudit.java` logue 🔴 ERREUR au démarrage si `ENCRYPTION_AES_KEY`/`APP_PAYMENTS_WEBHOOK_SECRET` vides en prod/beta, 🟠 si CORS wildcard, 🟠 si environment=dev déployé.
3. Audit poussé des méthodes de lookup par nom (e.g. `findByName`) qui n'utilisent pas le tenant foram.

---

*Basé sur l'analyse du code réel à date. Les corrections du FINAL_AUDIT (P0) ont été majoritairement apportées. Reste : RBAC fin + revue Swagger prod.*