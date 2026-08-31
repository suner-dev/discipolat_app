# DISCIPOLAT — MASTER DEVELOPMENT PROMPT
## Transformation de l’audit en fonctionnalités réelles Full-Stack + Mobile

**Projet :** `suner-dev/discipolat_app`  
**Stack existante :** Spring Boot / Spring Modulith / architecture hexagonale / PostgreSQL / React 19 + Vite + Tailwind / Flutter / Drift SQLite / offline-first  
**Objectif :** transformer les recommandations de l’audit en fonctionnalités réellement opérationnelles, intégrées au produit existant, sans mock, sans fake data et sans écrans de démonstration.

---

# 1. MISSION DE L’AGENT

Tu agis comme **Lead Software Engineer, Software Architect, Backend Engineer, Frontend Engineer, Mobile Engineer, Database Engineer, DevSecOps et QA Engineer senior**.

Ta mission est de prendre le dépôt Discipolat existant et de **terminer, industrialiser et déployer fonctionnellement les fonctionnalités identifiées dans l’audit**.

Le résultat attendu n’est pas une maquette.

Il doit s’agir de fonctionnalités **réelles, persistées en base de données, sécurisées, testées, accessibles via API, intégrées au frontend React et au mobile Flutter lorsque pertinent, compatibles avec l’architecture existante et réellement utilisables par un utilisateur final**.

## Règle absolue

> **INTERDICTION DE LIVRER DES MOCKS.**

Ne crée jamais une fonctionnalité qui se contente de :
- données statiques ;
- tableaux codés en dur ;
- fake repositories ;
- réponses API simulées ;
- boutons sans logique ;
- formulaires qui ne sauvegardent rien ;
- pages qui affichent uniquement une démonstration ;
- `TODO` laissé comme comportement final ;
- `kDemoDataRoutes` utilisé comme solution définitive ;
- faux paiements ;
- fausses notifications ;
- fausses synchronisations ;
- fausses certifications ;
- faux QR codes ;
- faux états métier.

Si une intégration externe n’est pas encore disponible, implémente une **architecture réelle d’intégration**, avec configuration, contrats, interfaces, persistance, gestion des erreurs et tests. Ne prétends jamais qu’un paiement, SMS, USSD ou fournisseur externe fonctionne réellement si aucun compte/API crédible n’est configuré.

---

# 2. SOURCE DE VÉRITÉ

Commence par analyser le dépôt réel avant de modifier quoi que ce soit.

Tu dois considérer comme sources de vérité :

1. le code actuel du repository ;
2. le schéma et les migrations PostgreSQL ;
3. les modules Spring Modulith existants ;
4. les API réellement disponibles ;
5. les écrans React existants ;
6. les écrans Flutter existants ;
7. les mécanismes offline/synchronisation Drift ;
8. les tests existants ;
9. les rapports d’audit présents dans le dépôt ;
10. le présent document.

**Ne réécris pas inutilement ce qui fonctionne déjà.**

Si une fonctionnalité existe déjà réellement, vérifie-la, améliore-la si nécessaire et connecte les parties manquantes.

Si une fonctionnalité existe en mock alors qu'un backend réel existe déjà, priorité au câblage réel.

---

# 3. ÉTAPE 0 — AUDIT TECHNIQUE AVANT DÉVELOPPEMENT

Avant toute implémentation :

### 3.1 Cartographier

Produis une cartographie interne du projet :

- backend ;
- frontend web ;
- mobile ;
- database ;
- authentication ;
- authorization ;
- multi-tenancy ;
- notifications ;
- fichiers ;
- paiements ;
- IA ;
- offline/sync ;
- observabilité ;
- tests ;
- CI/CD.

### 3.2 Identifier les fonctionnalités existantes

Pour chacune des fonctionnalités ci-dessous, classe-la :

- `EXISTE ET FONCTIONNE`
- `EXISTE MAIS PARTIELLE`
- `EXISTE EN MOCK`
- `BACKEND EXISTE / FRONT À CONNECTER`
- `FRONT EXISTE / BACKEND À CRÉER`
- `N’EXISTE PAS`

### 3.3 Identifier les écrans démo

Inspecte particulièrement les routes et composants correspondant à `kDemoDataRoutes` et aux écrans mobiles historiques.

Pour chaque écran concerné :

1. identifier les données affichées ;
2. trouver le backend correspondant ;
3. vérifier les DTO ;
4. vérifier les endpoints ;
5. vérifier les permissions ;
6. connecter l’écran à l’API ;
7. gérer loading/error/empty state ;
8. connecter la persistance ;
9. ajouter les tests ;
10. supprimer le comportement mock.

**Objectif prioritaire : zéro fonctionnalité commerciale présentée comme réelle alors qu’elle utilise encore des données fictives.**

---

# 4. ORDRE D’EXÉCUTION OBLIGATOIRE

Ne développe pas tout simultanément.

Travaille par lots indépendants et validables.

## PHASE 1 — Fiabilisation et suppression des mocks

Priorité maximale.

- Identifier les 16–20 écrans mobiles encore en démonstration.
- Identifier les écrans web utilisant des données fictives.
- Connecter les écrans aux vrais services backend.
- Corriger les endpoints incomplets.
- Corriger les DTO.
- Corriger les repositories.
- Corriger les migrations.
- Vérifier les permissions.
- Vérifier le tenant courant.
- Ajouter les tests.
- Tester web + mobile.
- Supprimer progressivement les routes de démonstration.

**Critère de sortie : aucune fonctionnalité existante importante ne doit dépendre de fake data.**

---

# 5. PHASE 2 — PASSEPORT SPIRITUEL PORTABLE ET VÉRIFIABLE

## Objectif

Créer un dossier de discipolat portable permettant à une personne de conserver un historique vérifiable de son parcours.

## Données possibles

Selon le modèle métier existant :

- identité du membre ;
- église actuelle ;
- historique d’appartenance ;
- baptême ;
- formations suivies ;
- certifications ;
- services ;
- recommandations ;
- étapes de discipolat ;
- mentors/faiseurs de disciples ;
- dates ;
- organisations émettrices.

## Backend

Créer ou adapter :

- modèle de domaine ;
- tables PostgreSQL ;
- migrations ;
- repositories ;
- services ;
- DTO ;
- contrôleurs ;
- permissions ;
- événements de domaine si pertinents.

Le passeport doit posséder une identité vérifiable.

Mettre en place une signature cryptographique ou un mécanisme de credential vérifiable compatible avec l’architecture existante.

## QR

Créer :

- génération du QR ;
- endpoint de vérification ;
- page publique minimale de vérification ;
- statut valide/révoqué/expiré ;
- traçabilité des vérifications.

Ne jamais exposer inutilement des données privées.

## Mobile

Permettre :

- consultation offline ;
- présentation du QR ;
- synchronisation ;
- mise à jour lorsque la connexion revient.

## Sécurité

Empêcher :

- falsification ;
- modification locale non vérifiée ;
- accès inter-tenant ;
- exposition de données privées.

---

# 6. PHASE 3 — RÉSEAU FÉDÉRÉ INTER-ÉGLISES

## Objectif

Permettre à une organisation supérieure de gérer plusieurs églises.

Exemples :

- dénomination ;
- convention ;
- diocèse ;
- fédération ;
- réseau.

## Architecture

Introduire une hiérarchie claire :

`Organisation → Église → Département/Entité → Utilisateurs`

Tout accès doit respecter :

- tenant ;
- organisation ;
- rôle ;
- permission ;
- scope géographique ou organisationnel lorsque nécessaire.

## Fonctionnalités

- création d’un réseau ;
- rattachement d’églises ;
- administration centralisée ;
- dashboard consolidé ;
- statistiques ;
- comparaison ;
- reporting ;
- gestion des permissions ;
- consentement ;
- audit.

Les données individuelles ne doivent être consolidées ou partagées que selon les règles de confidentialité et de consentement définies par le produit.

---

# 7. PHASE 4 — PASSERELLE USSD + SMS

## Objectif

Rendre Discipolat utilisable par des personnes ne possédant pas de smartphone ou ayant une connectivité limitée.

## Backend

Créer un module indépendant de type :

`ussd-gateway`

et/ou

`sms-gateway`

avec :

- abstraction fournisseur ;
- webhooks entrants ;
- messages sortants ;
- sessions USSD ;
- authentification ;
- rate limiting ;
- idempotence ;
- logs ;
- audit ;
- gestion des erreurs ;
- retries.

## Cas d’usage minimum

- confirmation de présence ;
- rapport hebdomadaire ;
- réception d’une alerte ;
- demande de rappel ;
- consultation d’informations simples ;
- confirmation d’un événement ;
- éventuellement dons selon la conformité du pays.

## Important

Ne code pas un faux opérateur.

Prévois une interface fournisseur et une implémentation réelle pour le fournisseur effectivement retenu et configuré.

---

# 8. PHASE 5 — AGRÉGATEUR MOBILE MONEY

## Objectif

Transformer le `PaymentGatewayService` générique en architecture réelle d’orchestration des paiements.

## Architecture

Créer une couche d’abstraction capable d'accueillir :

- MTN MoMo ;
- Orange Money ;
- Airtel Money ;
- Wave ;
- M-Pesa ;
- cartes/Stripe pour les cas appropriés.

Ne pas prétendre que tous les opérateurs sont intégrés si leurs credentials/API ne sont pas disponibles.

## Flux

`Client → Discipolat → Payment Orchestrator → Provider → Webhook → Reconciliation → Receipt`

## Exigences

- transaction ID ;
- idempotency key ;
- statut ;
- montant ;
- devise ;
- tenant ;
- bénéficiaire ;
- provider ;
- timestamps ;
- références externes ;
- webhook signé ;
- réconciliation ;
- retries ;
- timeout ;
- journal d’audit.

## Sécurité

- aucune donnée sensible en clair ;
- secrets dans variables d’environnement/secrets manager ;
- validation des webhooks ;
- protection contre double paiement ;
- contrôle du tenant.

---

# 9. PHASE 6 — ASSISTANT VOCAL ET LANGUES AFRICAINES

## Objectif

Permettre à l’utilisateur de parler et d’écouter certaines fonctionnalités.

Architecture :

`Flutter/Web → Audio → Speech-to-Text → Backend/AI → Action ou réponse → Text-to-Speech`

Préparer une architecture extensible pour :

- français ;
- anglais ;
- wolof ;
- lingala ;
- swahili ;
- yoruba ;
- haoussa ;
- ewondo ;
- autres langues selon les fournisseurs réellement disponibles.

Ne jamais simuler une transcription.

Si un fournisseur externe est requis :

- créer son adapter ;
- gérer configuration ;
- erreurs ;
- quotas ;
- confidentialité ;
- consentement ;
- stockage des fichiers ;
- suppression des données audio selon politique définie.

---

# 10. PHASE 7 — MARKETPLACE COMMUNAUTAIRE

## Objectif

Transformer le marketplace existant en véritable module transactionnel et communautaire.

## Fonctionnalités

### Annonces

- création ;
- modification ;
- publication ;
- expiration ;
- recherche ;
- filtres ;
- catégories ;
- images ;
- signalement ;
- modération.

### Emploi

- offres ;
- candidatures ;
- statut ;
- contact.

### Entraide

- demande d’aide ;
- niveau de visibilité ;
- collecte ;
- statut ;
- modération ;
- historique.

## Sécurité

- permissions ;
- modération ;
- anti-spam ;
- rate limiting ;
- audit ;
- isolation tenant.

Aucune annonce ne doit être créée uniquement en mémoire.

---

# 11. PHASE 8 — CERTIFICATION DE DISCIPOLAT

## Objectif

Créer une vraie certification numérique vérifiable.

## Fonctionnalités

- parcours ;
- modules ;
- progression ;
- validation ;
- évaluations ;
- critères ;
- certification ;
- émission ;
- révocation ;
- renouvellement si nécessaire ;
- QR de vérification ;
- historique.

Relier cette certification au passeport spirituel lorsque pertinent.

## Vérification publique

Créer une page/API permettant de vérifier :

- certificat valide ;
- titulaire ;
- émetteur ;
- date ;
- statut ;
- identifiant de certification.

Ne pas exposer d’informations personnelles inutiles.

---

# 12. PHASE 9 — API PUBLIQUE ET PLUG-INS

## Objectif

Préparer Discipolat à devenir une plateforme extensible.

## API

Mettre en place :

- versioning ;
- OAuth2/API keys selon architecture ;
- scopes ;
- rate limiting ;
- quotas ;
- audit ;
- documentation OpenAPI ;
- pagination ;
- idempotence lorsque nécessaire ;
- gestion d’erreurs standardisée.

## Developer experience

Préparer :

- documentation ;
- exemples ;
- webhooks ;
- sandbox si utile ;
- conventions ;
- gestion des versions.

## Plugins

Concevoir un modèle d’extensions sécurisé.

Un plugin ne doit jamais pouvoir contourner :

- multi-tenancy ;
- permissions ;
- sécurité ;
- confidentialité ;
- audit.

---

# 13. PHASE 10 — DISCIPOLAT LITE / CONNECTIVITÉ FAIBLE

## Objectif

Optimiser le produit pour les téléphones modestes et les réseaux instables.

## Mobile

Créer un mode léger :

- bundle raisonnable ;
- images optimisées ;
- pagination ;
- cache ;
- synchronisation différentielle ;
- files d’attente offline ;
- retry ;
- compression ;
- store-and-forward.

## Drift

Vérifier :

- migrations ;
- conflits ;
- synchronisation ;
- idempotence ;
- ordre des événements ;
- suppression ;
- reprise après crash.

Ne jamais perdre une donnée saisie offline.

---

# 14. PHASE 11 — RÉSEAU D’ENTRAIDE D’URGENCE

## Objectif

Permettre la coordination entre églises d’un réseau lors d’une crise.

## Fonctionnalités

- création d’une alerte ;
- zone concernée ;
- niveau de gravité ;
- personnes concernées ;
- besoins ;
- ressources ;
- volontaires ;
- églises participantes ;
- suivi ;
- résolution ;
- historique.

## Sécurité

Les données sensibles doivent avoir des niveaux d’accès stricts.

Prévoir :

- permissions ;
- audit ;
- expiration ;
- anonymisation lorsque possible ;
- contrôle du partage inter-organisation.

---

# 15. PHASE 12 — ARBRE GÉNÉALOGIQUE SPIRITUEL

## Objectif

Visualiser la transmission du discipolat :

`Faiseur → Disciple → Nouveau faiseur → Génération suivante`

## Backend

Modéliser les relations de manière robuste.

Contraintes :

- pas de boucle invalide ;
- pas de relation auto-référente ;
- tenant isolation ;
- historique ;
- suppression logique si nécessaire.

## Frontend

Créer une visualisation interactive :

- zoom ;
- navigation ;
- recherche ;
- profil ;
- générations ;
- filtres.

## Mobile

Version adaptée à l'écran mobile.

---

# 16. PHASE 13 — CONFORMITÉ LÉGALE MULTI-PAYS

## Objectif

Préparer Discipolat à gérer différentes exigences réglementaires.

Pays initiaux potentiels :

- Cameroun ;
- Nigeria ;
- RDC ;
- Côte d’Ivoire ;
- Kenya ;
- France pour certains cas de diaspora.

Ne jamais inventer une obligation légale.

La plateforme doit utiliser une architecture de règles/configuration :

`Country → Regulatory Profile → Rules → Documents/Reports`

## Fonctionnalités

- rapports ;
- reçus ;
- exports ;
- journalisation ;
- conservation ;
- suppression ;
- consentement ;
- droits utilisateurs ;
- configuration par pays.

Faire valider les règles juridiques par une source compétente avant de les considérer comme juridiquement garanties.

---

# 17. EXIGENCES FULL-STACK

Chaque fonctionnalité doit traverser toute la chaîne lorsque nécessaire :

`Database → Domain → Application → API → Web → Mobile → Tests`

## Backend

Respecter l’architecture hexagonale existante et Spring Modulith.

Ne pas transformer le projet en architecture anarchique.

Créer si nécessaire :

- entity/domain model ;
- value objects ;
- repository port ;
- repository adapter ;
- use case ;
- service ;
- DTO ;
- mapper ;
- controller ;
- validation ;
- authorization ;
- domain events ;
- integration events.

## Database

Toute donnée persistante doit avoir :

- migration versionnée ;
- contraintes ;
- index appropriés ;
- clés ;
- foreign keys ;
- tenant identifier lorsque nécessaire ;
- timestamps ;
- audit si nécessaire.

Ne jamais modifier manuellement la production comme méthode de développement.

---

# 18. FRONTEND REACT

Pour chaque nouvelle fonctionnalité :

- route ;
- page ;
- composants ;
- hooks ;
- API client ;
- loading ;
- empty state ;
- error state ;
- validation ;
- permissions ;
- responsive design ;
- accessibilité ;
- i18n ;
- tests.

Aucun bouton décoratif.

Tout bouton doit déclencher une vraie action ou être explicitement désactivé avec une raison.

---

# 19. MOBILE FLUTTER

Pour chaque fonctionnalité mobile :

- écran ;
- navigation ;
- repository ;
- datasource ;
- API ;
- Drift lorsque offline nécessaire ;
- sync ;
- états ;
- gestion réseau ;
- erreurs ;
- retry ;
- permissions ;
- i18n ;
- tests.

Respecter l’architecture Flutter déjà présente dans le projet.

Ne pas contourner le mécanisme offline-first existant.

---

# 20. MULTI-TENANCY — RÈGLE CRITIQUE

Aucune fonctionnalité nouvelle ne doit introduire une fuite entre églises.

Pour chaque requête :

1. déterminer le tenant courant ;
2. vérifier l'identité ;
3. vérifier le rôle ;
4. vérifier la permission ;
5. vérifier le scope ;
6. filtrer les données ;
7. vérifier les ressources ciblées ;
8. auditer les opérations sensibles.

Tester explicitement les scénarios :

- Église A tente d'accéder à Église B ;
- utilisateur A modifie un ID appartenant à B ;
- admin d'une église tente d'accéder au réseau ;
- utilisateur non autorisé appelle directement l'API.

---

# 21. SÉCURITÉ

Toutes les nouvelles fonctionnalités doivent respecter les corrections P0 déjà réalisées.

Vérifier notamment :

- CORS ;
- JWT ;
- 2FA ;
- permissions ;
- IDOR ;
- tenant isolation ;
- secrets ;
- chiffrement ;
- logs ;
- webhooks ;
- uploads ;
- rate limiting ;
- validation ;
- injection ;
- CSRF lorsque pertinent ;
- XSS ;
- SSRF lorsque pertinent ;
- audit trail.

**Ne jamais logger un mot de passe, token, secret ou donnée sensible.**

---

# 22. TESTS — OBLIGATOIRES

Une fonctionnalité n'est pas terminée parce que le code compile.

Chaque fonctionnalité doit avoir des tests adaptés :

### Backend

- unit tests ;
- integration tests ;
- repository tests ;
- API tests ;
- security tests ;
- tenant isolation tests.

### Frontend

- composants critiques ;
- hooks ;
- API states ;
- permissions ;
- parcours utilisateur critiques.

### Mobile

- widgets ;
- repositories ;
- sync ;
- offline ;
- erreurs ;
- navigation critique.

### End-to-end

Ajouter des parcours E2E lorsque la fonctionnalité est suffisamment critique.

---

# 23. OBSERVABILITÉ

Pour les opérations critiques :

- logs structurés ;
- correlation ID ;
- audit ;
- métriques ;
- erreurs traçables.

Ne pas enregistrer de secrets.

Prévoir une stratégie de monitoring pour :

- paiements ;
- synchronisation ;
- webhooks ;
- USSD/SMS ;
- certifications ;
- imports ;
- jobs asynchrones.

---

# 24. GESTION DES INTÉGRATIONS EXTERNES

Pour chaque fournisseur externe :

Créer une interface interne.

Exemple :

`PaymentProvider`

avec des adapters :

`MtnPaymentProvider`  
`OrangePaymentProvider`  
etc.

Même logique pour :

- SMS ;
- USSD ;
- STT ;
- TTS ;
- email ;
- stockage ;
- IA.

Ne jamais coupler tout le domaine métier directement à un fournisseur.

---

# 25. CONFIGURATION ET ENVIRONNEMENTS

Toute clé/API/credential doit être externalisée.

Prévoir :

- `.env.example` ;
- configuration locale ;
- configuration test ;
- configuration staging ;
- configuration production.

Ne jamais commit :

- secrets ;
- clés privées ;
- tokens ;
- credentials.

---

# 26. DOCKER ET ENVIRONNEMENT LOCAL

Le projet doit pouvoir être démarré proprement.

Maintenir ou améliorer :

- PostgreSQL ;
- backend ;
- frontend ;
- services nécessaires.

Fournir/maintenir un `docker-compose.yml` cohérent lorsque pertinent.

Documenter :

- démarrage ;
- migrations ;
- variables d'environnement ;
- tests ;
- arrêt ;
- reset local.

---

# 27. GIT — TRAVAIL PAR INCRÉMENTS

Ne fais pas un énorme commit final.

Pour chaque fonctionnalité :

1. analyse ;
2. DB/migration ;
3. backend ;
4. tests backend ;
5. frontend ;
6. tests frontend ;
7. mobile ;
8. tests mobile ;
9. intégration ;
10. validation ;
11. commit.

Utiliser des commits explicites, par exemple :

`feat(passport): implement verifiable spiritual passport`

`feat(federation): add multi-church federation management`

`feat(ussd): add real USSD gateway abstraction`

`feat(payments): add payment orchestration and reconciliation`

`fix(mobile): replace demo routes with real API data`

Ne mélange pas une fonctionnalité avec un refactoring sans rapport.

---

# 28. DOCUMENTATION À MAINTENIR

À chaque fonctionnalité importante, mettre à jour si nécessaire :

- README ;
- architecture ;
- API/OpenAPI ;
- variables d’environnement ;
- migrations ;
- documentation utilisateur ;
- documentation développeur ;
- changelog.

Créer également un fichier de suivi du projet :

`IMPLEMENTATION_STATUS.md`

avec :

| Fonctionnalité | Backend | DB | Web | Mobile | Tests | Statut |
|---|---|---|---|---|---|---|
| Suppression des mocks | | | | | | |
| Passeport spirituel | | | | | | |
| Réseau fédéré | | | | | | |
| USSD/SMS | | | | | | |
| Mobile Money | | | | | | |
| Assistant vocal | | | | | | |
| Marketplace | | | | | | |
| Certification | | | | | | |
| API/Plugins | | | | | | |
| Discipolat Lite | | | | | | |
| Entraide urgence | | | | | | |
| Arbre spirituel | | | | | | |
| Conformité multi-pays | | | | | | |

---

# 29. DEFINITION OF DONE

Une fonctionnalité est considérée comme **DONE** uniquement si :

- [ ] le besoin métier est compris ;
- [ ] le code existant a été inspecté ;
- [ ] l'architecture existante est respectée ;
- [ ] la DB est réelle et migrée ;
- [ ] les données sont persistées ;
- [ ] le backend fonctionne ;
- [ ] les permissions sont appliquées ;
- [ ] le multi-tenant est sécurisé ;
- [ ] les API fonctionnent ;
- [ ] le frontend est connecté ;
- [ ] le mobile est connecté lorsque pertinent ;
- [ ] offline/sync fonctionne lorsque pertinent ;
- [ ] loading/error/empty states sont gérés ;
- [ ] i18n est respectée ;
- [ ] accessibilité raisonnable ;
- [ ] tests écrits ;
- [ ] tests existants toujours verts ;
- [ ] aucune donnée mock utilisée ;
- [ ] aucun bouton fake ;
- [ ] aucune route démo nécessaire ;
- [ ] logs et audit présents si nécessaire ;
- [ ] documentation mise à jour ;
- [ ] commit propre ;
- [ ] fonctionnalité validée manuellement.

---

# 30. PROTOCOLE OBLIGATOIRE POUR CHAQUE TICKET

Avant de coder :

### A. ANALYSE

Explique en interne :

- problème ;
- utilisateur ;
- flux ;
- données ;
- modules impactés ;
- risques ;
- dépendances.

### B. INSPECTION

Cherche dans le repo :

- classes existantes ;
- endpoints ;
- tables ;
- migrations ;
- composants ;
- écrans ;
- tests ;
- services réutilisables.

### C. PLAN

Établis une checklist précise.

### D. IMPLÉMENTATION

Développe verticalement :

`DB → Backend → API → Web → Mobile → Tests`

### E. VALIDATION

Lance :

- build ;
- tests ;
- lint ;
- typecheck ;
- tests sécurité ;
- tests mobile ;
- tests frontend ;
- migrations.

### F. CORRECTION

Ne masque jamais une erreur.

Corrige la cause racine.

### G. RAPPORT

À la fin du lot, donne :

- fichiers modifiés ;
- fonctionnalités ajoutées ;
- migrations ;
- endpoints ;
- écrans ;
- tests ;
- problèmes rencontrés ;
- commandes de validation ;
- statut.

---

# 31. RÈGLE CONTRE LES RACCOURCIS

Si tu rencontres une difficulté :

**INTERDIT :**

> "Je vais mettre des données fictives pour continuer."

**INTERDIT :**

> "Je vais créer un bouton qui sera branché plus tard."

**INTERDIT :**

> "Je vais simplement retourner HTTP 200."

**INTERDIT :**

> "Je vais commenter cette partie et passer à la suivante."

**INTERDIT :**

> "Je vais créer une version simplifiée qui ne persiste rien."

À la place :

1. identifier le blocage ;
2. comprendre la cause ;
3. implémenter la vraie solution ;
4. tester ;
5. documenter si une dépendance externe est nécessaire.

---

# 32. ORDRE FINAL DE PRIORITÉ

Respecte cet ordre :

## PRIORITÉ P0
### 1. Supprimer les mocks
### 2. Sécurité
### 3. Multi-tenancy
### 4. Permissions/IDOR
### 5. Tests et stabilité

## PRIORITÉ P1
### 6. Passeport spirituel
### 7. Réseau fédéré
### 8. USSD/SMS
### 9. Mobile Money
### 10. Discipolat Lite

## PRIORITÉ P2
### 11. Certification
### 12. Marketplace
### 13. Assistant vocal
### 14. API publique / plugins

## PRIORITÉ P3
### 15. Réseau d'entraide
### 16. Arbre spirituel
### 17. Conformité multi-pays

L'ordre peut être ajusté uniquement si l'analyse du dépôt montre une dépendance technique réelle.

---

# 33. OBJECTIF PRODUIT

Ne cherche pas simplement à ajouter des fonctionnalités.

Le but est de transformer Discipolat en une **infrastructure numérique complète de discipolat et de gestion ecclésiale**, particulièrement adaptée aux réalités africaines :

- connectivité limitée ;
- Mobile Money ;
- USSD/SMS ;
- multilinguisme ;
- réseaux d'églises ;
- mobilité internationale ;
- historique du discipolat ;
- formation ;
- entraide ;
- administration ;
- conformité ;
- offline-first.

La différenciation doit venir de **fonctionnalités réellement utiles**, de données correctement protégées, d'une excellente expérience utilisateur et d'effets de réseau légitimes — jamais de mécanismes manipulateurs destinés à empêcher artificiellement un utilisateur de quitter la plateforme.

---

# 34. INSTRUCTION FINALE À L’AGENT

**Commence maintenant par l’audit du repository.**

Ne commence pas immédiatement à créer du code.

Première étape :

1. inspecter l'architecture ;
2. inspecter les modules backend ;
3. inspecter les migrations PostgreSQL ;
4. inspecter les API ;
5. inspecter React ;
6. inspecter Flutter ;
7. identifier précisément les mocks ;
8. identifier ce qui existe déjà ;
9. identifier les dépendances ;
10. construire `IMPLEMENTATION_STATUS.md`.

Ensuite, commence par la **PHASE 1 : suppression des mocks et câblage des fonctionnalités existantes**.

Après validation technique de cette phase, continue progressivement vers les fonctionnalités P1, P2 puis P3.

À aucun moment tu ne dois considérer une fonctionnalité comme terminée tant qu'elle n'est pas **réellement utilisable de bout en bout**.

> **FULL-STACK + MOBILE signifie : une fonctionnalité métier complète, persistée, sécurisée, testée et réellement utilisable. Pas une maquette. Pas un mock. Pas une promesse.**
