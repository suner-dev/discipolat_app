# Guide Utilisateur — Discipolat Church OS

> Version 2.0 — 2026-09-22 · §72 / G6.8. Web + mobile. Aide integree : tooltips, modales « ? » sur ecrans cles, wizard de premiere connexion, PasteurBot vocal (« Aide »). Support : support@discipolat.com · Swagger `/swagger-ui.html`.
> Voir [ARCHITECTURE.md](ARCHITECTURE.md) · [RBAC.md](RBAC.md) · [Back-office/Commercial](GUIDE_BACK_OFFICE_COMMERCIAL.md).

## 1. Demarrage rapide (tous roles)

1. Ouvrez l'app (web ou mobile), saisissez email + mot de passe (JWT 15 min, refresh 7 j ; 2FA TOTP si activee).
2. Premiere connexion : suivez le wizard — PASTEUR/ADMIN : Identite → Structure → Roles → Import → Modules ; CHEF : Famille → Disciples → Evenements → Dashboard ; FAISEUR : Disciples → Contacts → Defis → Bobby ; MEMBRE : Profil → Famille → Evenements → Sondages.
3. Navigation web : menu lateral (modules actives par votre espace) ; mobile : onglets Accueil/Engagements/Communique/Plus + mode hors-ligne (file outbox, synchro auto, indicateur d'etat).

## 2. Parcours par role

| Role | Quotidien type |
|---|---|
| Membre | Profil · inscription evenements (J'y vais/Interesse/Annule) · messages 1:1 · prieres · sondages · export RGPD |
| Chef d'espace / departement | Organisation (postes, affectations, taches) · KPI departement · evenements + checklists + pointage geolocalise · rapports · equipement/documents |
| Chef de famille | Dossier 360 des ames (profil, timeline, score spirituel, alertes decrochage, notes pastorales) · visites/receptions · rapports famille · prieres |
| Faiseur | Creer/suivre ames (Menu Ames → Nouvelle ame → famille+faiseur) · rapports hebdo (`/reports/maker-weekly`) · suivis paralleles · defis · mentoring |
| Pasteur | Dashboard KPI + narratifs IA · rapports executifs mensuels (tendances, convertis, alertes, recommandations) · chat IA (« familles en decrochement ? ») · cas pastoraux · succession |
| Admin eglise | Parametres (nom, logo, devise EUR/FCFA/USD, fuseau, langue FR/EN/PT/ES/SW/AR) · modules par espace · branding · invitations · connecteurs (Zapier/Make, Google Calendar, QuickBooks/Xero) · WhatsApp Business (Phone ID + token → test → activation, rappels J-1 auto) |

## 3. Modules cles (pas-a-pas)

- **Ames** : recherche/filtres (statut, famille, etat Tiède/Actif/Brulant, faiseur) ; creation ; dossier 360 ; transfert workflow (demande → validation → historique immuable).
- **Evenements** : Nouvel evenement (titre, lieu, date, quota) → brouillon/publication → RSVP temps reel → rappels WhatsApp J-1 → pointage (QR/geoloc) → checklists.
- **Rapports** : Nouveau rapport (hebdo/mensuel/special : presences, activites, alertes, prieres) → notification pasteur → consolidation famille/departement.
- **IA pastorale** : Chat IA + commandes vocales (« montre les familles en decrochement », « nouveaux convertis du mois ? », « genere le rapport semaine ») ; credits IA par plan (DECOUVERTE 500 → RESEAU 50 000/mois).
- **Messagerie** : 1:1 temps reel (WebSocket) ; broadcast cible (tous/departement/famille/segment → in-app + email + WhatsApp si configure).
- **Finances** : Nouvelle transaction (recette/depense, categorie, devise auto) · budgets par categorie · rapports multi-devises · dons Mobile Money (Orange/MTN/M-Pesa) · tontines · recurrences.
- **Formation** : parcours discipolat, mentorat (+ inverse), trainings, plans de developpement, badges/recompenses, defis hebdo, passeport spirituel.
- **Sante/pastorale** : journal de priere, cas pastoraux (confidentiels, scopes), visites IA-resumee, infirmerie.

## 4. Securite & confidentialite

JWT RS256 · 2FA · multi-roles (switch en 1 clic) · scopes stricts (un chef ne voit que sa famille) · notes pastorales restreintes · RGPD : export (`/gdpr`), effacement, consentements, audit personnel.

## 5. Aide & support

Tooltips · modales « ? » · wizard · PasteurBot · support@discipolat.com · FAQ produit · [Guide back-office/commercial](GUIDE_BACK_OFFICE_COMMERCIAL.md) pour l'onboarding eglise.
