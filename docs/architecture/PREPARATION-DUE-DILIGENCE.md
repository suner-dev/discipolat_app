# PRÉPARATION À LA DUE DILIGENCE TECHNIQUE — ce qu'un acquéreur senior regarde

> **Date** : 2026-09-29 · **Mesures** sur `main` @ `72ec85d5` · 2 730 fichiers versionnés, 469 commits.
> **Question traitée** : « une équipe senior en Europe/Amérique rachète le projet ; il ne doit pas
> rester de travail important, et tout doit être parfaitement propre et compris très vite ».
> **Documents liés** : `docs/AUDIT_ARCHITECTURE_FRONTEND.md` (constats front),
> `docs/architecture/frontend-target-architecture.md` (cible front),
> `docs/architecture/ADR-001-…` (dépôts), `PLAN_CORRECTIFS_ONBOARDING_TENANT_2AGENTS.md` (en cours).

---

## 0. D'ABORD, CORRIGER L'OBJECTIF — parce que l'objectif actuel est un piège

**« Qu'il ne reste plus grand chose comme travail » est un objectif contre-productif, et l'annoncer
ainsi serait une faute.**

- **Aucun acquéreur n'achète un produit figé.** Ils achètent une base : une équipe, une roadmap, une
  base de clients, un ARR. Un produit sans travail à faire est un produit sans marché. Ce qu'ils
  paient, ce sont les **coûts non planifiés** : le temps qu'un ingénieur senior perd à comprendre,
  à faire tourner, à corriger.
- **La due diligence a une dimension juridique.** Les déclarations et garanties de l'acquéreur
  portent sur l'état du code. Masquer un défaut connu (même par un document déplacé) est un risque
  de procédure, pas seulement d'image. **La règle professionnelle est : corriger, ou déclarer
  explicitement dans un `KNOWN_ISSUES.md` tenu à jour.** Un acquéreur senior préfère de loin une
  liste honnête et datée à un trou découvert à J+30.
- **L'indicateur qui compte n'est pas « reste-t-il du travail » mais :**
  **combien de temps faut-il à un ingénieur senior stranger pour (a) faire tourner les tests,
  (b) livrer un changement, (c) comprendre l'architecture ?**

| Indicateur | Ce qu'il faut viser | Pourquoi c'est celui-là |
|---|---|---|
| **Time-to-green** | `git clone` → suite de tests verte en **< 10 min**, en suivant le README | c'est la première impression, et c'est chronométré |
| **Time-to-first-commit** | comprendre + changer + merger en **< 1 jour** | c'est le coût réel d'embauche |
| **Dépréciations** | **0** `TODO/FIXME/HACK` dans le code produit | un TODO = « je n'ai pas fini » |
| **Faux positifs** | **0** affirmation non prouvée dans la doc | une doc fausse est pire que pas de doc |
| **Traces de dette** | **0** fichier de travail d'agent, 0 rapport périmé | un dépôt qui contient le plan du futureacheteur est un dépôt qui contient son passé |

**Traduction** : l'objectif n'est pas « zéro travail », c'est **zéro travail non planifié et zéro
surprise**. Un produit avec 6 mois de roadmap honnête et une base indéchiffrable vaut infiniment
plus qu'un produit figé et illisible.

---

## 1. LES 10 QUESTIONS D'UNE ÉQUIPE SENIOR — ET LA RÉPONSE HONNÊTE DU DÉPÔT

| # | Question posée en due diligence | Réponse actuelle mesurée | Verdict |
|---|---|---|---|
| 1 | « Montrez-moi comment on lance le produit et les tests. » | `README.md` = **596 octets**, une commande (`start-local.sh`), aucun prérequis, aucune commande de test, aucun diagramme | 🔴 |
| 2 | « La base de données est-elle réellementissue des migrations ? » | **Non.** `application-test.yml` : `flyway.enabled: false`, `ddl-auto: create-drop`, `h2-init.sql` de **7 lignes**. Les 1 469 tests tournent sur un schéma **généré depuis les entités**, jamais sur les 189 migrations | 🔴 **critique** |
| 3 | « Comment détectez-vous une dérive entité/migration ? » | **Vous ne la détectez pas** — c'est exactement la cause de H1 (`slug` manquant) et H2 (table `events`) : deux 500 en production avec une suite verte. Le constat est déjà écrit dans votre propre `agentA.md` | 🔴 **critique** |
| 4 | « Le schéma est-il reproductible de façon identique selon les environnements ? » | **Non** : `ddl-auto: none` (prod, `application.yml:21`), `update` (profil `docker`, **:253**), `validate` (**:279**), `create-drop` (tests). Trois comportements selon le profil | 🔴 |
| 5 | « Quelle est la couverture de tests ? » | **Inconnue et non mesurable** : pas de JaCoCo dans `pom.xml`. « 1 469 tests verts » est un nombre qu'un acquéreur **décote à zéro** dès qu'il apprend la question 2 | 🔴 |
| 6 | « Les tests tournent-ils sur le vrai SGBD ? » | Testcontainers est **déclaré dans `pom.xml`** mais **0 test ne l'utilise** (`grep -rl @Testcontainers backend/src/test` → 0). La dépendance donne l'illusion de la couverture | 🔴 |
| 7 | « Les dépendances sont-elles scannées ? » | `npm audit` oui (front). **Java : ni OWASP dependency-check, ni SpotBugs/PMD.** Aucune mesure de couverture. Pas de SBOM | 🟠 |
| 8 | « Qui possède la propriété intellectuelle ? » | **Aucun fichier `LICENSE`**, aucun `NOTICE`. Point bloquant classique d'un comité d'acquisition | 🔴 |
| 9 | « Comment vous décidez, et comment on lit vos décisions ? » | Pas d'ADR formel. `docs/DECISIONS.md` existe mais ne couvre pas les choix structurants. 2 documents de 67 et 135 KB nommés `*_AGENT*` dans la racine | 🟠 |
| 10 | « Montrez-moi l'architecture. » | `docs/architecture/target-architecture.md` : backend uniquement, **zéro section frontend/mobile**. `docs/À faire.md` annonce « ~100 erreurs de compilation préexistantes » et 30+ fonctionnalités manquantes | 🔴 |

**La patterns est nette : les 3 questions bloquantes (2, 3, 6) portent toutes sur la même racine —
les tests ne prouvent pas ce que le backend fait réellement en production.** Ce n'est pas un défaut
de qualité de code : c'est un défaut de **discipline de vérification**, et c'est ce type de défaut
qui fait chuter une valorisation de plusieurs dizaines de points, parce qu'il signifie que *aucun
vert historique n'est opposable*.

---

## 2. LES 5 TRAVAUX QUI CHANGENT LE PRIX (classés par impact sur la valorisation)

| Rang | Travail | Pourquoi c'est le plus rentable | Effort |
|---|---|---|---|
| **1** | **Faire tourner la suite backend sur PostgreSQL réel via Testcontainers** (la dépendance est déjà dans le `pom.xml`), et y appliquer **les migrations**, pas `ddl-auto`. Puis corriger ce que ça casse (les 5 dérives H sont déjà connues et documentées). | C'est **le** point sur lequel un comité technique peut refuser le projet. Il transforme « 1 469 tests verts qui ne prouvent rien » en « suite verte sur le schéma de production ». Le travail de diagnostic est **déjà fait** (H1-H8 documentés, 6 corrigés). | 1-2 semaines |
| **2** | **Aligner les 3 profils sur un seul comportement de schéma** : `validate` partout, jamais `update`. Un profil `docker` qui modifie le schéma via Hibernate est un défaut de production, pas un détail. | 30 minutes de travail, effet disproportionné sur la confiance. C'est le genre de détail que l'on retient à un comité (« ils laissent Hibernate écrire le schéma »). | 1 h |
| **3** | **JaCoCo + rapport de couverture + seuil de non-régression en CI** + `dependency-check` (Java). | Rend la qualité **mesurable** au lieu d'affirmée. Un taux de couverture *faible mais honnête et publié* vaut mieux que « 1 469 tests » non mesuré. | 2-3 j |
| **4** | **README professionnel + `ARCHITECTURE.md` (3 piles) + `docs/adr/` + `KNOWN_ISSUES.md` + `LICENSE` + nettoyage de la racine.** | C'est la demande explicite « très vite compréhensible ». Un dépôt dont la racine contient 16 `.md` dont 2 font 200 KB et sont des plans de travail d'agents donne une image de chaos organisationnel. | 3-5 j |
| **5** | **Zéro `TODO`/`FIXME` dans le code produit**, ou alors tracked dans `KNOWN_ISSUES.md` avec owner et date. | Un TODO est une promesse non tenue ; 200 TODO sont un aveu. | 1-2 j |

**Ce que ces 5 travaux ne font pas :** ils ne remplacent pas l'architecture front
(`frontend-target-architecture.md`) ni la dette mobile. Mais ils Treat l'ordre des priorités est
**ce qui se voit en premier**, donc ils viennent d'abord.

---

## 3. L'OBJECTIF « COMPRIS EN 5 MINUTES » — livrable concret

Un acquéreur ouvre le dépôt. Voici, dans cet ordre, ce qu'il doit voir — et ce qu'il trouve aujourd'hui.

| Attendu | Aujourd'hui | Action |
|---|---|---|
| `README.md` : ce que c'est, à qui ça sert, capture, stack, ** prérequis**, `start` en 3 commandes, **comment lancer les tests**, **où est la doc**, **license** | 596 octets, pas de tests, pas de prérequis | Réécriture complète (~200 lignes) |
| `ARCHITECTURE.md` : 3 piles, un **schéma ASCII des flux**, les frontières, où va chaque type de changement, les 3 décisions non évidentes et leur raison | Absent (le `target-architecture.md` ne couvre que le backend) | Nouveau (~300 lignes) + le frontend/mobile |
| `docs/adr/` : 5 à 8 décisions, format court (contexte / décision / conséquence / date) | Aucun ADR formel | Créer 8 ADR à partir de ce qui a **déjà été décidé** (auth, multi-tenant, migrations, i18n, design system, choix de plates-formes) |
| `KNOWN_ISSUES.md` : **14 constats inventoriés**, **avec owner et statut**, y compris H2 et l'arbitrage `users.tenant_id`. Deux ajoutés le 2026-10-09 à l'occasion de l'arbitrage de valorisation : **A3** (le module `finances` n'est pas un ledger — solde déclaré par le client, aucune colonne `account_id`) et **A4** (outbox : un seul consommateur par type, dernier inscrit gagne) | Dispersés dans 3 rapports de 100+ Ko | Consolidation |
| `CONTRIBUTING.md` réel : conventions de commit, de branche, comment lancer **chaque** pile | Mentionné, non présent | Nouveau |
| `LICENSE` + `NOTICE` | **Absents** | Juridique : à faire valider |
| Racine propre : 3-4 fichiers (`README`, `LICENSE`, `CHANGELOG`, `SECURITY`) + `docs/` | **16 `.md`**, dont `AGENT_ORCHESTRATION.md` (67 Ko), `PLAN_CORRECTIFS…` (135 Ko), `TODO_REPRISE…`, `DISCIPOLAT_MASTER_DEVELOPMENT_PROMPT.md` | **Déplacer le plan de travail et les rapports d'agents hors du dépôt produit** (ou dans `docs/internal/` clairement exclu). Ces documents sont un process interne, pas un actif produit |

> **Point d'honnêteté important** : déplacer les documents de travail ne doit **pas** servir à
> masquer un défaut. Le `KNOWN_ISSUES.md` doit lister **tout**, y compris ce que les rapports
> mentionnent (H2, arbitrage multi-tenant, dette mobile). On range, on ne cache pas.

---

## 4. LES 3 PILES — plan d'architecture, dans l'ordre où le risque se paie

| Pile | État actuel (mesuré) | Architecture cible | Indicateur de réussite pour l'acquéreur |
|---|---|---|---|
| **Backend** | 1 264 classes, 189 migrations, boundaries seulement par packages ; **le test ne prouve pas le schéma de production** (question 2) ; 1 tente de correction d'architecture multi-tenant en cours (arbitrage D2) | **Ne pas refondre.** Le découpage par modules métier est déjà bon. Il faut : (a) tests sur PostgreSQL réel, (b) frontières vérifiées par architecture-test (`ArchUnit` — 2 j), (c) l'arbitrage `users.tenant_id` **tranché et documenté** (c'est le seul vrai défaut d'architecture connu) | `mvn verify` vert **sur PostgreSQL** ; 0 dérive entité/migration ; 1 ADR sur le multi-tenant |
| **Frontend** | 240 pages à plat, 0 frontière, 259 `any`, 501 clés de cache littérales, 784 Ko d'i18n au boot, 0 observabilité, lint qui ne peut pas échouer | `docs/architecture/frontend-target-architecture.md` : tranches verticales + contrat OpenAPI généré + frontières ESLint + socle (http, clés, i18n, DS, erreurs) | `npm run lint` en 0 ; 0 `any` hors `generated/` ; `-1,2 Mo` au boot ; 5 parcours E2E verts en CI |
| **Mobile** | **Non audité par moi** — je n'ai pas d'inventaire fiable au-delà de ce que dit le plan : 5 écrans > 1 000 lignes (`department_management_screen` 1 441, `app_drawer` 1 377, `department_member_dossier` 1 360, `department_tools` 1 336, `department_detail` 1 326) ; même modèle probable (écrans géants, données + UI mélangées) | **À auditer avant de décider.** Structure équivalente à celle du front (features verticales), et **c'est le candidat naturel au partage de code avec le front** (contrats générés, pas de logique métier dupliquée) | `flutter analyze` 0 · `flutter test` vert · aucun écran > 500 lignes · contrats partagés avec le front |

**L'ordre de vérité** : le backend d'abord (il porte le risque de production et le doute), le
frontend ensuite (c'est le plus endetté), le mobile en parallèle mais **après son audit**.

---

## 5. SÉQUENCE RESPECTANT VOTRE CONTRAINTE (« les correctifs d'abord »)

| Étape | Contenu | Condition de passage |
|---|---|---|
| **S0 — Maintenant** | Finir `PLAN_CORRECTIFS_ONBOARDING_TENANT_2AGENTS.md` (tâches B restantes + intégrations) et trancher les 6 arbitrages | Les 2 branches fusionnées, la recette E2E jouée, les rapports de clôture rédigés |
| **S1 — Crédibilité backend (2-3 sem.)** | Testcontainers sur PostgreSQL + migrations réelles + correction des dérives restantes · profil `ddl-auto` aligné · JaCoCo + `dependency-check` | **Une seule commande** `mvn verify` prouve le schéma de production |
| **S2 — Lisibilité immédiate (3-5 j)** | README · `ARCHITECTURE.md` 3 piles · `docs/adr/` (8) · `KNOWN_ISSUES.md` · `LICENSE` · nettoyage de la racine | Un senior qui ne connaît pas le projet comprend le projet en 5 min et lance les tests en 10 |
| **S3 — Socle frontend (3 sem.)** | Les 7 décisions D1→D7 du document d'architecture front | Lint honnête, contrat généré, −1,2 Mo, erreurs observées |
| **S4 — Mobile : audit puis socle (à chiffrer après audit)** | Audit réel, puis structure par features | Audit livré, chiffré honnêtement |
| **S5 — Migrer par tranches (3 mois)** | Une slice par PR, jamais d'arrêt de production | Compteur → 0 |

**Ce que je advise de ne pas faire pendant S1-S2 :** ne pas toucher à la fonctionnalité. Ces deux
étapes sont de la **fiabilité et de la lisibilité** : c'est exactement ce que le prix paie.

---

## 6. LE COÛT, SANS ARRONDI

| Étape | Charge | Remarque |
|---|---|---|
| S1 backend crédibilité | 1-2 sem. | Le diagnostic existe déjà (H1-H8) ; c'est de l'exécution, pas de l'investigation |
| S2 lisibilité | 3-5 j | Le meilleur rapport valeur/effort de toute la liste |
| S3 socle front | 3 sem. | Additif, aucun risque de régression |
| S4 mobile | **inconnu tant que l'audit n'est pas fait** | Je refuse de chiffrer sans avoir audité — un chiffrage inventé ici serait exactement le genre d'affirmation que l'audit du dépôt raconte (rapport A13 : « 4 citations de test inventées, trouvées et remplacées ») |
| S5 migration | ~3 mois temps partiel | Le vrai coût, et le seul qui ne se compresse pas |

---

## 7. INTÉGRITÉ : LA RÈGLE À ÉCRIRE DANS LE `KNOWN_ISSUES.md`

> Ce document liste **tout** ce qui est connu, y compris ce qui est dispersé dans les rapports
> internes. Chaque ligne : **constat · preuve (commande reproductible) · impact · owner · statut ·
> date**. Aucun défaut connu ne doit être déplacé, reformulé pour avoir l'air plus léger, ou
> omis. Une due diligence sérieuse lit `KNOWN_ISSUES.md` avant le code : **c'est le document qui
> crée le plus de confiance, et le seul dont l'inexactitude ait des conséquences juridiques.**

---

## 8. CE QUE JE N'AI PAS VÉRIFIÉ (et qu'il faut vérifier avant de conclure)

1. **Le mobile n'a pas été audité.** Je n'ai que les mesures de taille d'écran du plan
   d'orchestration. Toute affirmation sur son architecture serait inventée.
2. **Aucun scan de sécurité** n'a été exécuté (dépendances Maven/pub, secrets dans l'historique,
   configuration). Un commit de clé privée RSA **de test** est bien versionné
   (`application-test.yml:25-26`) : c'est acceptable s'il est explicitement marqué test-only, mais
   un scanner le signalera — il faut donc le documenter.
3. **Aucune exécution de la suite complète** n'a été faite par moi (le plan fournit 1 253-1 469 tests
   selon les branches ; l'écart n'a pas été expliqué). Un chiffre de test non reproductible est
   exactement ce que l'audit A13 a déjà corrigé une fois.
4. **Aucun accès à la production**, ni aux métriques (logs, incidents, performance réelle). Les
   constats sont **statiques**.
5. **Le dépôt `platform` / GitLab** (ADR-001) n'est pas tranché : cela modifie le visage du dépôt
   et donc, indirectement, sa valeur.

---

*Toutes les mesures sont reproductibles depuis `main` : `cat backend/src/test/resources/application-test.yml`,
`grep -n "ddl-auto" backend/src/main/resources/application.yml`, `grep -rl @Testcontainers backend/src/test`,
`grep -n "jacoco\|dependency-check" backend/pom.xml`, `ls LICENSE*`, `wc -c README.md`, `ls *.md | wc -l`.*
