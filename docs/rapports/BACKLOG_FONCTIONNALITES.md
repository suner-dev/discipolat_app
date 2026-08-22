Fais# 📋 BACKLOG COMPLET — Fonctionnalités Non Implémentées & Partiellement Implémentées

> **Date** : 22 août 2026
> **Méthodologie** : Croisement de tous les fichiers `.md` du projet avec le code source réel (backend Java, frontend React, mobile Flutter).
> **Règle** : Une fonctionnalité est considérée comme **implémentée** uniquement si elle possède un backend endpoint + une page/écran fonctionnel dans le code.

---

## 📊 RÉSUMÉ

| Catégorie | Nombre |
|-----------|--------|
| 🔴 P0 — Bloquantes commercialisation | **10** |
| 🟠 P1 — Forte valeur ajoutée | **51** |
| 🟡 P2 — UX / performance / sécurité | **29** |
| 🔵 P3 — Innovation / futuriste | **26** |
| **Total non implémentées** | **116** |
| ⚠️ Partiellement implémentées | **23** |

---

# 🔴 PRIORITÉ P0 — Bloquantes pour la commercialisation

> Sans ces fonctionnalités, l'application ne peut pas être vendue à des églises importantes ou structurées.

| # | Fonctionnalité | Description | Source(s) | Effort estimé |
|---|---------------|-------------|-----------|---------------|
| 1 | **Pont WhatsApp ↔ Discipolat** | Bot WhatsApp officiel par église : diffusion des annonces vers les groupes WhatsApp existants, réception de messages, gestion d'inscription (`#rejoindre Famille-Faiseur`), rappels. WhatsApp est *la* communication des églises africaines — ce pont fait de l'app un hub, pas un silo. | AUDIT_COMPLET #2, COMMERCIALIZATION_AUDIT | 6 semaines |
| 2 | **API publique documentée (OpenAPI/Swagger)** | Endpoints REST/GraphQL avec playground interactif, SDK JavaScript/Flutter, webhooks pour intégrations externes. Swagger existe mais renvoie 401 (non exposé). Les `api_keys` existent mais le endpoint public n'est pas documenté. | AUDIT_COMPLET #15, COMMERCIALIZATION_AUDIT | 2 semaines |
| 3 | **Connecteurs tiers (Zapier/Make/QuickBooks/Google Calendar)** | Intégration native avec Planning Central, Church Metrics, QuickBooks/Xero, Google/Outlook Calendar. Webhooks sortants configurables existent mais aucun connecteur natif. | AUDIT_COMPLET #15, COMMERCIALIZATION_AUDIT | 3 semaines |
| 4 | **Compliance Manager RGPD/CCPA avancé** | Politique de rétention configurable, purge automatisée + export avant suppression, gestion des consentements, journal d'audit immuable (hash chaîné), exportabilité "portabilité des données" 1-clic. Module `gdpr` existe mais très basique. | AUDIT_COMPLET #14, COMMERCIALIZATION_AUDIT | 2 semaines |
| 5 | **Assistant vocal conversationnel "PasteurBot" offline** | Assistant vocal offline : le pasteur parle "Montre-moi les familles en décrochement" → l'IA comprend et génère le rapport. LLM léger embarqué, fonctionne sans connexion. Aucune implémentation existante. | AUDIT_COMPLET #19 | 6 semaines |
| 6 | **Multi-devise + fuseaux horaires** | Multi-devise dynamique dans les rapports financiers, fuseaux horaires configurables par utilisateur (actuellement UTC fixe), formats de date localisés. Le convertisseur de change `/exchange` existe mais n'est pas intégré aux rapports. | COMMERCIALIZATION_AUDIT, AUDIT_COMPLET | 1 semaine |
| 7 | **Rate limiting brute-force sur login** | Protection spécifique sur `/auth/login` — un attaquant peut brute-forcer les mots de passe. Bucket4j existe sur les API mais pas sur le endpoint de connexion. | COMMERCIALIZATION_AUDIT | 1 jour |
| 8 | **Chiffrement des données sensibles** | Chiffrement au repos des données sensibles (prières, notes pastorales, données santé spirituelle). JWT RS256 et 2FA TOTP existent mais pas de chiffrement des données stockées. | AUDIT_COMPLET #5, COMMERCIALIZATION_AUDIT | 1 semaine |
| 9 | **Documentation utilisateur** | Guide de démarrage rapide, tooltips contextuels, aide intégrée. README et CHANGELOG existent mais aucun guide utilisateur. | COMMERCIALIZATION_AUDIT | 3 jours |
| 10 | **Onboarding wizard multi-étapes** | Wizard de configuration : Identité → Import membres → Structure → Rôles → Premier événement. Templates prédéfinis (petite/moyenne/grande église). L'écran d'onboarding AR existe (mobile) mais pas de wizard de configuration. | COMMERCIALIZATION_AUDIT, AUDIT_COMPLET | 2 semaines |

---

# 🟠 PRIORITÉ P1 — Forte valeur ajoutée

> Fonctionnalités qui enrichissent significativement l'offre et justifient un abonnement premium.

## IA & Intelligence

| # | Fonctionnalité | Description | Source(s) | Effort estimé |
|---|---------------|-------------|-----------|---------------|
| 11 | **Automatisations pastorales configurables** | Interface type Zapier "Quand X → faire Y" : "Quand un membre est absent 3 semaines → envoyer message au faiseur", "Quand un nouveau membre rejoint → créer compte + assigner faiseur + planifier RDV". Workflow engine existe mais hardcodé. | RAPPORT_FINAL_MISSION #8, COMMERCIALIZATION_AUDIT #12 | 4 semaines |
| 12 | **Messagerie IA contextuelle** | L'IA génère des messages personnalisés pour chaque membre basés sur leur parcours, prières, situation. "Envoie un message de encouragement aux membres en difficulté" → personnalisation automatique. | RAPPORT_FINAL_MISSION #6 | 2 semaines |
| 13 | **Générateur de formulaires intelligents (drag & drop)** | Builder drag & drop avec conditions logiques (si X alors Y), validation des données par l'IA, types variés (texte, choix, fichier, date, signature). Page Builder existe (14 blocs) mais pas de builder de formulaires. | RAPPORT_FINAL_MISSION #7, COMMERCIALIZATION_AUDIT #11 | 3 semaines |
| 14 | **Parcours de discipolat IA personnalisable** | L'IA crée et adapte le parcours en fonction du profil du disciple, progression automatique basée sur les actions, recommandations de prochaine étape. Module formations existe mais sans adaptation IA. | RAPPORT_FINAL_MISSION #9, COMMERCIALIZATION_AUDIT #13 | 3 semaines |
| 15 | **Tableau de bord exécutif avec insights IA** | Dashboard avec insights automatiques générés par l'IA : "La présence a baissé de 12% chez les 18-25 ans", recommandations d'actions, tendances et prédictions. Dashboard KPI existe mais sans insights IA. | RAPPORT_FINAL_MISSION #4 | 2 semaines |
| 16 | **Rapports PDF exécutifs automatiques** | Génération automatique d'un rapport mensuel/trimestriel avec graphiques, insights, et recommandations pour le conseil d'église. PDF existe mais pas de rapport automatique周期ique. | RAPPORT_FINAL_MISSION #7, RECOMMANDATIONS #2.2 | 2 semaines |
| 17 | **Notes IA automatiques pendant visites** | Transcription automatique des conversations pastorales (voice → text → résumé IA). Whisper local + LLM pour résumé. Stocké dans le dossier du disciple. | RAPPORT_FINAL_MISSION #18 | 3 semaines |

## Communication & Réseau

| # | Fonctionnalité | Description | Source(s) | Effort estimé |
|---|---------------|-------------|-----------|---------------|
| 18 | **Portail public de l'église** | Page publique générée automatiquement : événements, contact, don, streaming. L'admin configure nom, logo, couleurs, sections. Les églises paient pour un site web séparé. | RAPPORT_FINAL_MISSION #11, COMMERCIALIZATION_AUDIT #28 | 3 semaines |
| 19 | **Streaming live intégré** | Intégration OBS/RTMP pour le streaming live, player intégré dans l'app, replay avec timestamps et notes synchronisées. HLS.js + integration YouTube/Twitch. | RAPPORT_FINAL_MISSION #16, COMMERCIALIZATION_AUDIT #18 | 4 semaines |
| 20 | **Système de parrainage gamifié** | Code de parrainage unique par membre, suivi des invitations converties, points XP et badges pour parrainage, leaderboard mensuel. Module Quest existe mais pas de système de parrainage. | RAPPORT_FINAL_MISSION #17, COMMERCIALIZATION_AUDIT #19 | 1 semaine |
| 21 | **Système de tickets internes** | Chaque membre peut créer un ticket, catégorisation automatique par l'IA, assignation au bon service, suivi et résolution. Aucun module ticketing. | COMMERCIALIZATION_AUDIT #26 | 2 semaines |
| 22 | **Traduction en direct des sermons** | Audio sermon → Whisper (FR) → LLM traduit en EN/PT/ES en temps réel → sous-titres affichés sur écran. Whisper + translation model local. | COMMERCIALIZATION_AUDIT #27 | 6 semaines |
| 23 | **Messagerie de groupe par équipe** | Conversations de groupe par département/famille, historique, notifications, partage fichiers, recherche messages. Messagerie 1:1 existe (WebSocket) mais pas de groupes. | MOBILE_AUDIT #14 | 3 semaines |
| 24 | **Intégration calendrier (Google/Outlook/iCal)** | Bouton "Add to Calendar", synchronisation avec Google/Outlook/ICal, rappels synchronisés. Événements existent mais pas de synchronisation externe. | MOBILE_AUDIT #13 | 1 semaine |
| 25 | **Sondages rapides** | Création de sondages anonymes ou nominatifs pour les membres du département avec résultats en temps réel. Aucun module sondages. | RECOMMANDATIONS #2.3 | 1 semaine |
| 26 | **Annonces programmées** | Création d'annonces avec date de publication et d'expiration automatique. Communications existent mais pas de planification fine. | RECOMMANDATIONS #2.3 | 3 jours |
| 27 | **Galerie de témoignages** | Espace de publication et modération des témoignages d'âmes, avec approbation pastorale avant diffusion. Aucun module. | RECOMMANDATIONS #2.2 | 1 semaine |
| 28 | **Broadcast ciblé avec accusé de lecture** | Envoi ciblé de messages à tous les membres, par département, par famille, ou par segment avec accusé de lecture. Communications existent mais sans accusé de lecture. | RECOMMANDATIONS #2.2 | 2 semaines |

## Gestion RH & Opérationnel

| # | Fonctionnalité | Description | Source(s) | Effort estimé |
|---|---------------|-------------|-----------|---------------|
| 29 | **Gestion des congés/absences formelle** | Demande et validation d'absences (maladie, congé, mission) avec impact sur la charge de travail. Suivis existent mais pas de workflow formel. | RECOMMANDATIONS #2.3 | 1 semaine |
| 30 | **Évaluation 360°** | Feedback anonyme entre membres d'une équipe, avec synthèse automatique et plan d'action. Évaluations anonymes existent mais pas de feedback croisé complet. | RECOMMANDATIONS #2.3 | 2 semaines |
| 31 | **Gantt des équipes** | Vue planning des affectations par équipe/événement avec détection des surcharges. Tâches existent mais pas de vue planning visuel. | RECOMMANDATIONS #2.3 | 2 semaines |
| 32 | **Inventaire intelligent (alertes stock)** | Alertes de stock bas, historique d'utilisation par événement, suggestion de réapprovisionnement. Module inventaire existe (CRUD basique). | RECOMMANDATIONS #2.3 | 1 semaine |
| 33 | **Checklist événementielle** | Génération automatique d'une checklist pré-événement (matériel, équipes, documents) avec assignation et suivi. Checklists département existent mais pas de génération auto. | RECOMMANDATIONS #2.3 | 1 semaine |
| 34 | **KPIs de performance département** | Taux de remplissage des équipes, taux de réalisation des tâches, satisfaction des membres. Stats départementales existent mais pas de métriques avancées. | RECOMMANDATIONS #2.3 | 1 semaine |
| 35 | **Matching membres ↔ compétences** | Chaque membre profile compétences/intérêts, besoins équipes, système propose matches, membre confirme. Pas de profil compétences. | COMMERCIALIZATION_AUDIT #9, MOBILE_AUDIT #4 | 3 semaines |
| 36 | **Matrice de compétences** | Évaluation des membres sur des compétences spécifiques (animation, musique, accueil) avec identification des gaps. Aucun module. | RECOMMANDATIONS #2.3 | 1 semaine |
| 37 | **Plan de développement individuel** | Génération automatique d'objectifs de développement par membre basés sur leurs performances et les besoins du département. Aucun module. | RECOMMANDATIONS #2.3 | 2 semaines |

## Accompagnement pastoral

| # | Fonctionnalité | Description | Source(s) | Effort estimé |
|---|---------------|-------------|-----------|---------------|
| 38 | **Mentorat IA pour chefs de famille** | Suggestions d'approches d'accompagnement basées sur le profil de chaque faiseur (style d'apprentissage, forces, zones de croissance). IA Pastorale existe mais sans mentorat. | RECOMMANDATIONS #2.4 | 2 semaines |
| 39 | **Suivi de développement faiseur** | Tracking des compétences acquises, des formations suivies, des âmes accompagnées avec succès. Pas de tracking compétences. | RECOMMANDATIONS #2.4 | 1 semaine |
| 40 | **Plan de succession** | Identification des faiseurs prêts à prendre plus de responsabilités, avec plan de transition. Aucun module. | RECOMMANDATIONS #2.4 | 1 semaine |
| 41 | **Réunion de famille automatisée** | Génération d'un ordre du jour basé sur les alertes, les rapports en attente, et les événements à venir. Aucun module. | RECOMMANDATIONS #2.4 | 1 semaine |
| 42 | **Indicateur de cohésion familiale** | Indicateur de santé de la famille (taux de participation aux événements, diversité des âmes, équilibre des charges). Aucun calcul. | RECOMMANDATIONS #2.4 | 1 semaine |
| 43 | **Banque de ressources familiales** | Partage de documents, vidéos, études bibliques au sein de la famille avec accès contrôlé. Module fichiers existe mais sans partage familial contrôlé. | RECOMMANDATIONS #2.4 | 1 semaine |
| 44 | **Projection de croissance familiale** | Simulation : "Si chaque faiseur ajoute 2 âmes ce trimestre, la famille passera de 15 à 23 âmes". Jumeau numérique existe mais pas de ciblage par famille. | RECOMMANDATIONS #2.4 | 1 semaine |
| 45 | **Plan de visite pastorale auto** | Génération automatique d'un planning de visites basé sur les alertes, les familles à risque, et les demandes de rendez-vous. Visites existent mais pas de génération auto. | RECOMMANDATIONS #2.2 | 1 semaine |
| 46 | **Drill-down narratif sur KPI** | Au clic sur un KPI, narration automatique : "Le taux de présence a baissé de 5% ce mois, principalement dans le département Jeunesse". Pas de narration. | RECOMMANDATIONS #2.2 | 2 semaines |
| 47 | **Comparaison d'églises (réseau)** | Si multi-églises, benchmark anonymisé entre les églises du réseau sur les KPIs clés. Benchmark inter-églises existe mais pas de comparaison intra-réseau. | RECOMMANDATIONS #2.2 | 2 semaines |

## Espace membre

| # | Fonctionnalité | Description | Source(s) | Effort estimé |
|---|---------------|-------------|-----------|---------------|
| 48 | **Timeline de vie de l'âme enrichie** | Vue chronologique complète : conversions, baptêmes, engagements, difficultés, victoires avec tags et filtres. Dossier 360° existe mais sans tags/filtres complets. | RECOMMANDATIONS #2.5 | 2 semaines |
| 49 | **Plan de lecture biblique partagé** | Création de plans de lecture personnalisés par âme avec suivi de progression et notes partagées. Aucun module. | RECOMMANDATIONS #2.5 | 2 semaines |
| 50 | **Journal de prière personnel** | Carnet de prière personnel par âme avec réponses documentées et rappels de suivi. Prières existent (communautaires) mais pas de carnet privé. | RECOMMANDATIONS #2.5 | 1 semaine |
| 51 | **Défis spirituels** | Création de défis personnalisés (jeûne, lecture, service) avec suivi et encouragement. Quest existe mais pas de défis par faiseur. | RECOMMANDATIONS #2.5 | 1 semaine |
| 52 | **Cercle de faiseurs** | Espace de partage entre faiseurs (anonyme optionnel) pour échanger sur les défis, les succès, les méthodes. Messagerie 1:1 existe mais pas d'espace collectif. | RECOMMANDATIONS #2.5 | 2 semaines |
| 53 | **Formation continue avec progression** | Catalogue de formations avec progression, quiz, et certificats de compétences. Module formations existe mais sans catalogue structuré. | RECOMMANDATIONS #2.5 | 2 semaines |
| 54 | **Mentorat inversé** | Possibilité de demander de l'aide à un faiseur plus expérimenté ou au pasteur pour des cas difficiles. Aucun module. | RECOMMANDATIONS #2.5 | 1 semaine |
| 55 | **Journal spirituel personnel** | Carnet privé de réflexions, prières, et remerciements avec rappels et encouragements. Aucun module. | RECOMMANDATIONS #2.6 | 1 semaine |
| 56 | **Objectifs spirituels personnels** | Définition d'objectifs personnels (lecture, prière, service) avec suivi et célébration des accomplissements. Objectifs existent (assignés) mais pas de définition personnelle. | RECOMMANDATIONS #2.6 | 1 semaine |
| 57 | **Demandes administratives (baptême, dédicace)** | Soumission de demandes (baptême, dédicace, accueil d'un nouveau) avec suivi de statut. Demandes membres existent mais pas de types spécifiques. | RECOMMANDATIONS #2.6 | 1 semaine |
| 58 | **Annuaire de l'église (fiches publiques opt-in)** | Répertoire des membres avec fiche publique (opt-in) pour se connaître et prier les uns pour les autres. Aucun module. | RECOMMANDATIONS #2.6 | 1 semaine |
| 59 | **Don en ligne intégré membre** | Intégration de paiement mobile (Mobile Money, carte) pour les dons avec reçu fiscal. Paiements existent mais pas d'expérience don dédiée membre. | RECOMMANDATIONS #2.6 | 1 semaine |
| 60 | **Plan de lecture biblique** | Plans de lecture personnalisés avec progression et notes partagées au sein de la famille. Aucun module. | RECOMMANDATIONS #2.5 | 2 semaines |

## Analytics & Mesure

| # | Fonctionnalité | Description | Source(s) | Effort estimé |
|---|---------------|-------------|-----------|---------------|
| 61 | **Analytics d'engagement (Plausible/Umami)** | Pages vues, actions utilisateurs, taux d'engagement, heatmaps, funnel d'inscription → engagement. Aucun tracking intégré. | COMMERCIALIZATION_AUDIT #16, RAPPORT_FINAL_MISSION #21 | 1 semaine |
| 62 | **Gestion avancée des bénévoles** | Base compétences, disponibilité, matching événements→bénévoles. Module départements existe mais pas de matching. | COMMERCIALIZATION_AUDIT #8 | 2 semaines |
| 63 | **Prédictions effectifs et engagement** | Séries historiques → modèles simples → projections effectifs, baptêmes, décrochage sur 6-12 mois. Jumeau numérique existe (simulateur) mais pas de séries historiques ML. | COMMERCIALIZATION_AUDIT #10 | 3 semaines |
| 64 | **Centre d'intelligence organisationnelle** | Tableau de bord unifié avec 50+ KPIs en temps réel, signes avant-coureur décrochage. Dashboard KPI existe mais sans 50+ KPIs temps réel. | COMMERCIALIZATION_AUDIT #1 | 4 semaines |
| 65 | **Intégration calendrier mobile** | Bouton "Add to Calendar", Google/Outlook/ICal, rappels synchronisés. Plugin standard. | MOBILE_AUDIT #13 | 3 jours |

---

# 🟡 PRIORITÉ P2 — Améliorations UX / performance / sécurité

> Problèmes techniques et UX qui dégradent l'expérience utilisateur.

## Sécurité

| # | Fonctionnalité | Description | Source(s) | Effort estimé |
|---|---------------|-------------|-----------|---------------|
| 66 | **Content-Security-Policy headers** | Headers CSP pour protéger contre les attaques XSS. Pas de headers CSP configurés. | COMMERCIALIZATION_AUDIT | 1 jour |
| 67 | **Tests IDOR/multi-tenant** | Tests formels d'isolation cross-tenant sur toutes les APIs. 0 test IDOR formel. | COMMERCIALIZATION_AUDIT | 3 jours |
| 68 | **Tests E2E (Playwright/Cypress)** | Tests bout-en-bout sur les vraies routes. 0 test E2E. | COMMERCIALIZATION_AUDIT | 1 semaine |
| 69 | **Tests de charge (JMeter)** | Tests de performance sous charge. 0 test de charge. | COMMERCIALIZATION_AUDIT | 2 jours |
| 70 | **Tests de sécurité (OWASP ZAP)** | Tests de sécurité automatisés. 0 test de sécurité. | COMMERCIALIZATION_AUDIT | 2 jours |
| 71 | **Session timeout configurable (mobile)** | Session valide indéfiniment côté mobile. Pas de timeout. | MOBILE_AUDIT | 1 jour |
| 72 | **Biométrie mobile (fingerprint/face ID)** | Support biométrique pour l'authentification. Pas de support. | MOBILE_AUDIT | 2 jours |
| 73 | **Déconnexion automatique inactivité** | Session timeout après inactivité. Pas de mécanisme. | MOBILE_AUDIT | 1 jour |
| 74 | **Protection screenshot données sensibles** | Protection contre les captures d'écran sur écrans sensibles. Pas de protection. | MOBILE_AUDIT | 1 jour |
| 75 | **Audit logging côté mobile** | Journalisation des actions utilisateur sur mobile. Pas de logging. | MOBILE_AUDIT | 2 jours |

## Performance

| # | Fonctionnalité | Description | Source(s) | Effort estimé |
|---|---------------|-------------|-----------|---------------|
| 76 | **Pagination côté serveur sur toutes les listes** | Listes sans pagination côté serveur. Nécessite `PageResponse` sur tous les endpoints. | COMMERCIALIZATION_AUDIT | 1 semaine |
| 77 | **Lazy loading des images** | Images non optimisées (pas de compression, pas de formats adaptatifs). | COMMERCIALIZATION_AUDIT | 2 jours |
| 78 | **Cache Redis optimisé (KPI fréquents)** | Cache existe mais clé identique pour tous les users. Pas de cache par rôle. | COMMERCIALIZATION_AUDIT | 2 jours |
| 79 | **Sauvegarde automatique PostgreSQL** | Pas de pg_dump automatique ni interface de restauration. | COMMERCIALIZATION_AUDIT | 1 jour |
| 80 | **Monitoring proactif (Sentry/Grafana)** | Pas de monitoring en production. | COMMERCIALIZATION_AUDIT | 2 jours |
| 81 | **CI/CD en production** | Pas de pipeline de déploiement automatisé. | COMMERCIALIZATION_AUDIT | 3 jours |
| 82 | **Code splitting Recharts** | Chunk Recharts ~443KB. Pas de `manualChunks`. | COMMERCIALIZATION_AUDIT | 1 jour |
| 83 | **Separation stockage fichiers par tenant** | Fichiers partagés entre organisations. Pas d'isolation. | MOBILE_AUDIT | 2 jours |
| 84 | **Filtre tenant dans les API calls mobile** | Pas de `X-Tenant-Id` systématique sur mobile. | MOBILE_AUDIT | 1 jour |

## UX / UI

| # | Fonctionnalité | Description | Source(s) | Effort estimé |
|---|---------------|-------------|-----------|---------------|
| 85 | **Skeleton loading states partout** | Présent sur certains écrans mais pas universel. | COMMERCIALIZATION_AUDIT | 2 jours |
| 86 | **Empty states attractifs partout** | États vides pauvres sur beaucoup de pages. | COMMERCIALIZATION_AUDIT | 2 jours |
| 87 | **Confirmation avant actions destructives** | Pas de confirmation universelle avant suppression. | COMMERCIALIZATION_AUDIT | 1 jour |
| 88 | **Toast de succès/erreur unifié** | Pas de système de toast cohérent. | COMMERCIALIZATION_AUDIT | 1 jour |
| 89 | **Bottom navigation mobile** | Pas de barre de navigation inférieure. | COMMERCIALIZATION_AUDIT | 2 jours |
| 90 | **Graphiques adaptés petit écran (<360px)** | Recharts illisibles sur petits écrans. | MOBILE_AUDIT | 2 jours |
| 91 | **Formulaires avec indication de progression** | Formulaires longs sans nb étapes complétées. | MOBILE_AUDIT | 1 jour |
| 92 | **Onboarding mobile** | Première connexion confuse, pas de tutoriel. | MOBILE_AUDIT | 2 jours |
| 93 | **Mode réduit de mouvement** | Pas de support prefers-reduced-motion. | MOBILE_AUDIT | 1 jour |
| 94 | **Optimisation screen reader** | Labels ARIA manquants sur widgets personnalisés. | MOBILE_AUDIT | 2 jours |
| 95 | **Mode lite/datasaver mobile** | Pas de réduction de données pour zones à faible connectivité. | MOBILE_AUDIT #7 | 1 semaine |
| 96 | **Orientation landscape optimisée** | Pas de support landscape. | MOBILE_AUDIT | 2 jours |
| 97 | **Taille touche tactile ≥44px** | Parfois <44px recommandé. | MOBILE_AUDIT | 1 jour |
| 98 | **Focus management** | Focus ne revient pas logiquement après opérations. | MOBILE_AUDIT | 1 jour |
| 99 | **Breakpoint tablette dédié** | Juste mobile→desktop, pas de tablette. | MOBILE_AUDIT | 2 jours |

---

# 🔵 PRIORITÉ P3 — Innovation / futuriste

> Fonctionnalités avancées qui créent une différenciation concurrentielle.

## IA avancée

| # | Fonctionnalité | Description | Source(s) | Effort estimé |
|---|---------------|-------------|-----------|---------------|
| 100 | **Filtre de modération de contenu par IA** | Détection automatique de messages/rapports inappropriés avec suggestion de modération. | RECOMMANDATIONS #2.1A | 2 semaines |
| 101 | **Assistant de migration de données** | Aide à la migration depuis Excel/autres logiciels avec mapping intelligent des champs. | RECOMMANDATIONS #2.1A | 2 semaines |
| 102 | **Prédiction de charge (pics d'activité)** | Anticipation des pics d'activité (événements, rapports) pour dimensionner les ressources. | RECOMMANDATIONS #2.1A | 2 semaines |
| 103 | **Prophétie de croissance** | Modèle prédictif basé sur l'historique des présences, conversions, et retraits pour anticiper les besoins. | RECOMMANDATIONS #2.2A | 3 semaines |
| 104 | **Analyse de santé spirituelle par quartier** | Agrégation géographique des âmes avec heatmap de présence, identification des zones de couverture faible. | RECOMMANDATIONS #2.2A | 2 semaines |

## Plateforme & Écosystème

| # | Fonctionnalité | Description | Source(s) | Effort estimé |
|---|---------------|-------------|-----------|---------------|
| 105 | **Marketplace de templates** | Bibliothèque de modèles de départements, familles, rapports pré-configurés partageables entre églises. | RECOMMANDATIONS #2.1B | 3 semaines |
| 106 | **Tableau de bord sabbatique** | Vue consolidée de l'état spirituel de l'église avec indicateurs de maturité. | RECOMMANDATIONS #2.2B | 1 semaine |
| 107 | **Benchmark anonyme inter-églises (amélioré)** | Comparaison par taille/pays/dénomination avec clustering. Benchmark existe en basique. | COMMERCIALIZATION_AUDIT #22 | 2 semaines |
| 108 | **Système de récompenses avancé** | Défis hebdomadaires, récompenses tangibles (certificats, mentions). Quest existe mais sans défis hebdo. | RAPPORT_FINAL_MISSION #19 | 1 semaine |
| 109 | **Analytics d'usage (Plausible self-hosted)** | Tracking d'usage intégré (pages vues, heatmaps, funnel). Aucun tracking. | COMMERCIALIZATION_AUDIT #16 | 1 semaine |
| 110 | **Gestion des sauvegardes PostgreSQL** | Planification backups, vérification d'intégrité, restauration point-in-time. | RECOMMANDATIONS #2.1C | 1 jour |

## Expérience utilisateur avancée

| # | Fonctionnalité | Description | Source(s) | Effort estimé |
|---|---------------|-------------|-----------|---------------|
| 111 | **Parcours spirituel visuel (membre)** | Vue visuelle de la progression spirituelle : engagements pris, formations suivies, étapes franchies. | RECOMMANDATIONS #2.6A | 2 semaines |
| 112 | **Demandes de suivi (membre)** | Possibilité de demander un faiseur ou un accompagnement spirituel directement depuis l'app. | RECOMMANDATIONS #2.6A | 1 semaine |
| 113 | **Événements à venir (membre)** | Calendrier personnel des événements auxquels le membre est inscrit ou intéressé. | RECOMMANDATIONS #2.6B | 1 semaine |
| 114 | **Sondages et feedback (membre)** | Participation aux sondages du département/église avec visualisation des résultats globaux. | RECOMMANDATIONS #2.6B | 1 semaine |
| 115 | **Mon équipe/ma famille (membre)** | Vue des membres de sa famille/équipe avec possibilité d'envoyer des encouragements. | RECOMMANDATIONS #2.6B | 1 semaine |
| 116 | **Onboarding interactif par rôle** | Tutoriel guidé pas-à-pas pour chaque rôle avec tooltips contextuels et checklist de première connexion. | RECOMMANDATIONS #2.1D | 2 semaines |

---

# ⚠️ FONCTIONNALITÉS PARTIELLEMENT IMPLÉMENTÉES

> Ces fonctionnalités existent dans le code mais de manière incomplète ou dégradée.

| # | Fonctionnalité | Ce qui existe | Ce qui manque | Source(s) |
|---|---------------|---------------|---------------|-----------|
| P1 | **i18n Multi-langue** | FR/EN/PT (100+ strings) dans `frontend/src/i18n/` | Pas de **ES (espagnol), SW (swahili), AR (arabe)**. Pas de traductions configurables par l'admin. Pas de **notifications/localisation dans la langue du destinataire**. Pas de **timezone dynamique**. | COMMERCIALIZATION_AUDIT, AUDIT_COMPLET #15 |
| P2 | **Auth social (Google)** | Bouton Google dans `LoginPage.tsx` avec Google Identity Services + callback `/auth/google` | Pas de **Apple Sign In**, pas de **Facebook Login**. Le Magic Link a été supprimé du code. Pas de **SSO pour les réseaux d'églises**. | COMMERCIALIZATION_AUDIT, AUDIT_COMPLET #22 |
| P3 | **Offline-first mobile** | `OfflineSyncManager` + `OfflineBanner` + queue de sync dans mobile | Cache Drift limité aux **âmes/rapports uniquement**. Pas de sync pour **agenda, messages, évaluations, documents**. Pas de **mode dégradé** si animation trop lente. Pas de **measurement des performances** offline. | MOBILE_AUDIT, AUDIT_COMPLET #3 |
| P4 | **Push notifications mobile** | Firebase Cloud Messaging (FCM) + notifications locales | Pas de **rappels contextuels intelligentes** ("24h avant événement", "Vous n'avez pas prié depuis 5 jours"). Pas de **ciblage intelligent** par rôle/situation. | MOBILE_AUDIT #2, COMMERCIALIZATION_AUDIT #7 |
| P5 | **Smart Alerts (détection anomalies)** | Backend `SmartAlertService` + mobile `SmartAlertsScreen` | **Pas de détection IA avancée** — fonctionne sur des règles simples (absences soutenues, pas de contact). Pas de **prédiction de décrochage 2-3 semaines à l'avance**. Pas de **plan d'intervention automatique**. | AUDIT_COMPLET #13, COMMERCIALIZATION_AUDIT #2 |
| P6 | **IA Pastorale** | Backend `AiAssistantService` (3 endpoints : analyze/resume/encouragement) | **Moteur de règles déterministe uniquement** — pas de LLM, pas de chat conversationnel, pas de RAG sur les données. Pas de génération de rapports naturels. | AUDIT_COMPLET #10, COMMERCIALIZATION_AUDIT #1 |
| P7 | **Score Spirituel Dynamique** | Backend calcul + frontend sparkline dans `SoulDetailPage` | 4 axes de score au lieu de **12** prévus (présence, prière, engagement, service manquants). Pas de **tendance sur 6 mois**. Pas de **comparaison inter-membres**. | AUDIT_COMPLET, RAPPORT_FINAL_MISSION #2 |
| P8 | **Pipeline d'évangélisation Kanban** | Backend `EvangelismService` (11 étapes) + mobile `EvangelismScreen` | Pas de **scoring de conversion** (probabilité). Pas de **prédiction de multiplication**. Pas d'**itinéraires d'accompagnement personnalisés**. | AUDIT_COMPLET #12 |
| P9 | **Gamification (Quest/Badges)** | Backend `QuestService` + `BadgeService` + mobile leaderboard | Pas de **défis hebdomadaires générés automatiquement**. Pas de **récompenses tangibles** (certificats, mentions). Pas de **badges contextualisés** par profil. Pas de **classement par famille/département**. | COMMERCIALIZATION_AUDIT #21, RAPPORT_FINAL_MISSION #19 |
| P10 | **Messagerie temps réel** | Backend WebSocket STOMP + mobile `ConversationDetailScreen` | Pas de **conversations de groupe**. Pas de **voice messages**. Pas de **réactions**. Pas de **threads**. Pas de **recherche dans les messages**. | COMMERCIALIZATION_AUDIT #7 |
| P11 | **Tontine numérique** | Backend `TontineService` (CRUD + contributions + rotation) + mobile `TontineScreen` | Pas de **liens avec les objectifs de générosité**. Pas de **notifications push** sur les échéances. Pas de **tableau de bord de santé** du groupe. Pas de **gestion des impayés**. | AUDIT_COMPLET #8 |
| P12 | **Paiements Mobile Money** | Backend `PaymentGatewayService` (sandbox) + mobile `GivingScreen` | **Pas d'intégration réelle** avec M-Pesa/MTN MoMo/Orange/Airtel/Wave — uniquement la sandbox. Pas de **webhooks opérateurs réels**. Pas de **reçu fiscal automatique**. | COMMERCIALIZATION_AUDIT |
| P13 | **Reconnaissance faciale (pointage)** | Backend `FaceRecognitionController` (dHash 256 bits) + mobile `FaceCheckinScreen` | Algorithme **fiable uniquement pour petit effectif** (église locale). Pas d'**encodeur neuronal** (embeddings 128-d) pour grandes bases. Pas de **mode photo de groupe**. | AUDIT_COMPLET #16 |
| P14 | **Rapports vocaux IA** | Backend `VoiceReport` entity + mobile `VoiceReportScreen` | Pas de **transcription Whisper locale** (utilise l'analyse d'entités basique). Pas de **suggestions d'actions** par IA. Pas de **génération automatique de rapports structurés**. | AUDIT_COMPLET #5 |
| P15 | **Formations (LMS)** | Backend `TrainingController` (cours, quiz, certificats) + mobile `TrainingsScreen` | **Module marqué BROKEN** dans COMMERCIALIZATION_AUDIT. Pas de **vidéos inline**, pas de **progression temps réel**, pas de **gamification intégrée**, pas de **mode low-data**. | COMMERCIALIZATION_AUDIT |
| P16 | **Carte interactive (Leaflet)** | Backend `MapController` (heatmap + sectors) + frontend `KingdomMappingPage` | Pas de **clustering intelligent**. Pas de **filtrage par densité**. Pas de **planification d'itinéraires** (OR-Tools). Pas de **mode hors-ligne** pour zones blanches. | AUDIT_COMPLET #4 |
| P17 | **Jumeau numérique (Digital Twin)** | Backend `TwinService` (simulateur) + frontend `DigitalTwinPage` | Pas de **scénarios prêts à l'emploi** côté UI. Pas de **prévision besoins en leaders**. Pas de **planification d'investissements**. Pas de **mobile** (web uniquement). | COMMERCIALIZATION_AUDIT |
| P18 | **Webhooks & clés API** | Backend CRUD + logs HMAC-SHA256 + frontend `AdminWebhooksPage` | Pas de **connecteurs natifs** (Zapier, Make). Pas de **playground interactif**. Pas de **documentation OpenAPI** exposée. | AUDIT_COMPLET #15 |
| P19 | **Observatoire santé spirituelle** | Backend `SpiritualHealthService` (prédiction décrochage 30j) + frontend `HealthObservatoryPage` | Pas de **modèle ML fédéré**. Pas de **planification d'interventions automatiques**. Pas de **score par département**. Pas de **tendance sur 6 mois**. | AUDIT_COMPLET #13 |
| P20 | **Géofencing présences** | Backend `GeofencingController` + mobile `GeofencingScreen` | Pas de **auto check-in/check-out** en temps réel. Pas de **historique GPS**. Pas de **mode basse consommation** batterie. | COMMERCIALIZATION_AUDIT |
| P21 | **Notifications Email/SMS multi-canal** | Backend `NotificationService` avec enum CANAL (IN_APP, EMAIL, PUSH, SMS) | Pas de **Twilio** (SMS réel). Pas de **WhatsApp**. Pas de **SMTP configuré** en production. Pas de **préférences notification** par utilisateur. | COMMERCIALIZATION_AUDIT #6 |
| P22 | **Rapports PDF** | Backend `ReportPdfService` (OpenPDF) + mobile PDF viewer | Pas de **rapports exécutifs automatiques** (mensuel/trimestriel). Pas de **personnalisation du layout**. Pas de **graphs Recharts dans le PDF**. | COMMERCIALIZATION_AUDIT #7 |
| P23 | **Multi-tenant isolation** | `@Filter(name="tenantFilter")` sur les entités + `TenantContext` + `MultiTenantInterceptor` | **Pas systématique** — certaines entités n'ont pas le filtre. Export CSV non filtré. Analytics cross-tenant. Redis cache partagé. Pas de **séparation DB par église**. | COMMERCIALIZATION_AUDIT, AUDIT_COMPLET #1 |

---

# 📈 STATISTIQUES DE COUVERTURE

| Module | Implémenté | Partiel | Non fait | Couverture |
|--------|-----------|---------|----------|------------|
| **Auth & Sécurité** | JWT, 2FA, RBAC | Google OAuth (pas Apple/Facebook) | Brute-force, chiffrement, session timeout | 70% |
| **IA & Intelligence** | Règles déterministes, analyse spirituelle | Score spirituel (4/12 axes), Smart Alerts (règles) | LLM, chat, prédiction ML, modération IA | 25% |
| **Communication** | Messagerie 1:1, WebSocket, notifications in-app | Multi-canal (pas réel), communications | WhatsApp, groupe, streaming, tickets, sondages | 30% |
| **Mobile** | 57 écrans, sync basique, QR, géolocalisation | Offline (partiel), push (basique) | Bottom nav, biométrie, widgets, datasaver, landscape | 60% |
| **Finances** | Transactions, budgets, tontine, Mobile Money (sandbox) | Paiements (pas réels), rapports PDF | Multi-devise, reçus fiscaux, portail don | 55% |
| **Administration** | Modules, menus, pages, champs custom, workflows (hardcodé) | Workflows (pas configurables), audit (basique) | Onboarding wizard, backups, API publique | 50% |
| **CRM & Discipolat** | Pipeline evangelism, CRM faiseur, rapports, visites | Score (partiel), formations (broken) | Parcours IA, mentoring, cercle, défis | 40% |
| **Gamification** | Quest, badges, leaderboard | XP (basique), niveaux | Défis hebdo, récompenses, parrainage | 35% |
| **Configuration** | ChurchSettings, branding, menus, modules | Workflows (hardcodé) | Portail public, marketplace templates | 45% |
| **Analytics** | KPIs dashboard, stats départementales | BI dashboard (basique) | Analytics usage, prédictions, intelligence org | 30% |
| **Multi-tenant** | Tenant filter, tenant context | Isolation (pas systématique) | DB separation, setup wizard, facturation | 40% |
| **Sécurité** | JWT RS256, RBAC, audit trail | Permissions (partiel), IDOR (corrigé sur paiements) | Tests IDOR, E2E, charge, OWASP, CSP | 50% |

---

# 🎯 TOP 10 — Actions immédiates recommandées

| # | Action | Impact | Effort |
|---|--------|--------|--------|
| 1 | **Pont WhatsApp** | Adoption massive en Afrique | 6 semaines |
| 2 | **Compliance RGPD avancé** | Églises institutionnelles | 2 semaines |
| 3 | **Onboarding wizard** | Rétention nouvelles églises | 2 semaines |
| 4 | **Documentation utilisateur** | Autonomie des églises | 3 jours |
| 5 | **Automatisations configurables** | Productivité pasteurale | 4 semaines |
| 6 | **Brute-force protection login** | Sécurité critique | 1 jour |
| 7 | **Tests IDOR/multi-tenant** | Confiance églises | 3 jours |
| 8 | **Multi-devise + fuseaux** | Expansion internationale | 1 semaine |
| 9 | **Bottom navigation mobile** | UX mobile | 2 jours |
| 10 | **Skeleton/empty states** | Perceived performance | 2 jours |

---

> **Total effort estimé pour clore toutes les fonctionnalités P0** : ~12 semaines
> **Total effort estimé pour clore P0 + P1** : ~35 semaines
> **Total effort estimé pour tout (P0-P3)** : ~60 semaines

*Document généré le 22 août 2026. Basé sur l'analyse de 30 fichiers .md et le code source du dépôt.*
