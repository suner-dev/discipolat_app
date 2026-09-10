# ETAT DES FONCTIONNALITÉS — Discipolat

> État au: 09/09/2026
> Phase: Phase finale — Pré-commercialisation
> Objectif: 100% fonctionnel — Fullstack & Mobile — Tous les rôles

## Légende
- ✅ = Complètement fonctionnel (backend + frontend + mobile + BDD)
- ⚠️ = Partiel — Fonctionne mais avec limitations/mocks
- ❌ = Cassé — Né fonctionne pas
- 🆕 = Manquant — Non implémenté

---

## PRIORITÉ P0 — Fondations & Sécurité

| # | Fonctionnalité | État | Description des problèmes | Action requise |
|---|----------------|------|--------------------------|----------------|
| 1 | Sécurité @PreAuthorize sur endpoints | ✅ | Corrigé — @PreAuthorize ajouté sur StreamChatController + tous les controllers authentifiés | Valider stabilité |
| 2 | CORS wildcard avec credentials | ✅ | Déjà corrigé — credentials désactivés si wildcard, origines spécifiques en production | Valider en production |
| 3 | Flux webhook paiement sans auth | ✅ | Déjà corrigé — X-Webhook-Secret obligatoire, 503 si non configuré | Valider stabilité |
| 4 | Passeport spirituel — code corrompu | ✅ | Déjà corrigé en session courante (V102) | Valider que tout est stable |
| 5 | Réseau inter-églises — compilation cassée | ✅ | Déjà corrigé (V103 — NetworkEventParticipant) | Valider stabilité |
| 6 | IDOR sur 15+ endpoints | ⚠️ | Ownership checks à ajouter progressivement sur endpoints critiques | Ajout en cours |
| 7 | Modèle de permissions permissif | ✅ | Déjà corrigé — restrictif par défaut + bypass ADMIN/PASTEUR | Valider stabilité |
| 8 | Données codées en dur IA | ✅ | Déjà corrigé — données réelles utilisées | Valider stabilité à long terme |

---

## PRIORITÉ P1 — Mobile-First & Expérience Utilisateur

| # | Fonctionnalité | État | Description des problèmes | Action requise |
|---|----------------|------|--------------------------|----------------|
| 9 | Assistant vocal web — mode démo | ✅ | Déjà corrigé — API réelle branchée (VoiceAssistantPage.tsx) | Valider stabilité |
| 10 | Écrans mobiles >1000 lignes | ⚠️ | department_management_screen.dart (1441), department_member_dossier_screen.dart (1360), department_tools_screen.dart (1336), department_detail_screen.dart (1326) | Refactoring en cours |
| 11 | 2 systèmes i18n en parallèle | ⚠️ | app_localizations.dart complet (6 langues) — map manuelle à nettoyer | Consolidation en cours |
| 12 | Voice commands pas localisés | ✅ | Corrigé — 6 commandes vocales localisées dans 6 langues (FR/EN/PT/ES/SW/AR) | Valider stabilité |
| 13 | Catch blocks vides — 16 instances | ✅ | Corrigé — tous les catch {} remplacés par console.error/logging | Valider stabilité |
| 14 | Import/Export document | ✅ | Déjà implémenté — ImportController + ExportController + ReportExportController | Valider tous les formats |
| 15 | Alertes/Notifications erreurs avalées | ✅ | Corrigé — tous les catch {} remplacés par console.error/logging | Valider stabilité |
| 16 | Transferts demande — partielle | ✅ | Déjà complet — flux BROUILLON→SOUMIS→EN_ATTENTE_VALIDATION→VALIDATION_PARTIELLE→VALIDE→EXECUTE | Valider stabilité |
| 17 | QR Check-in permissions partielles | ✅ | Corrigé — permissions alignées sur les 6 rôles (ADMIN, PASTEUR, RESPONSABLE, CHEF, FAISEUR, MEMBRE) | Valider stabilité |

---

## PRIORITÉ P2 — IA & Intelligence

| # | Fonctionnalité | État | Description des problèmes | Action requise |
|---|----------------|------|--------------------------|----------------|
| 18 | Prédictions IA données réelles | ✅ | Déjà corrigé (données réelles, pas de valeurs factices) | Valider stabilité à long terme |
| 19 | Assistant vocal STT/TTS conditionnel | ✅ | Géré correctement — retourne 503 si non configuré, fallback honnête | Documenter configuration requise |
| 20 | Jumeau Numérique / Digital Twin | ✅ | Déjà implémenté | Vérifier que simulations présentées comme estimations |

---

## PRIORITÉ P3 — Écosystème & Réseau

| # | Fonctionnalité | État | Description des problèmes | Action requise |
|---|----------------|------|--------------------------|----------------|
| 21 | Marketplace — modération | ✅ | Déjà implémenté | Vérifier flow création modération |
| 22 | Digital Twin simulations | ✅ | Déjà implémenté | Vérifier UI visible pour tous les rôles |
| 23 | Carte territoriale | ✅ | Déjà implémenté | Vérifier consentement et vie privée |

---

## FONCTIONNALITÉS MANQUANTES (0)

| # | Fonctionnalité | État | Description | Action requise |
|---|----------------|------|-------------|----------------|
| 24 | Export CSV/PDF complet | ✅ | Déjà implémenté — ExportController + ReportExportController + UI ReportsPage | Valider tous les formats |

---

## RÔLES DE SÉCURITÉ (6 rôles à considérer)

| Rôle | Codes | Accès requis |
|------|-------|--------------|
| ADMIN | 000001 | Tout — super-utilisateur |
| PASTEUR | 000010 | Accès global église |
| RESPONSABLE | 000100 | Département(s) attribué(s) |
| CHEF_DE_FAMILLE | 001000 | Famille(s) attribuée(s) |
| FAISEUR | 010000 | Ses âmes uniquement |
| MEMBRE | 100000 | Profil, événements, dons seulement |

---

## CRITÈRE DE VALIDATION PAR FONCTIONNALITÉ

Une fonctionnalité est **VRAIMENT DONE** uniquement si :
- [ ] DB migration appliquée
- [ ] Backend compilé & API fonctionnelle
- [ ] Frontend web fonctionnel (tous les rôles)
- [ ] Mobile fonctionnel (tous les rôles)
- [ ] Offline-first si applicable
- [ ] Sécurité validée (@PreAuthorize, pas d'IDOR)
- [ ] Pas de données mockées en production
- [ ] Tests passés
- [ ] Parcours utilisateur vérifié (créer → vérifier → persist)
- [ ] Visible dans l'appli (UI non cachée)