# Inventaire `context.go()` mobile — Classification T4.x

> **Source** : `grep -rn "context.go(" lib/ --include="*.dart" | grep -v "\.g\.dart" | grep -v "\.freezed\.dart" | grep -v "app_localizations"` → **141 occurrences**
> **Règle D6** : Un commit par conversion, un widget test par conversion. Jamais systématique.

---

## Classification

| Catégorie | Règle | Exemples |
|-----------|-------|----------|
| **ENTREE_ESPACE** (keep `go()`) | Point d'entrée d'un espace de rôle, réinitialisation de pile | `roleHome()`, `/login`, `/register`, `/dashboard`, `tenant_selection` |
| **SOUS_ECRAN** (→ `push()`) | Drill-down, détail, sous-écran, éditeur, formulaire | Détail âme, fiche département, membre, rapport, formulaire |
| **POST_AUTH** (keep `go()`) | Redirection post-auth, changement de rôle, tenant switch | `roleHome()`, `tenant_selection`, `roleHome()` après login |

---

## Classification détaillée (141 occurrences)

### ENTREE_ESPACE — KEEP `go()` (≈35)

| Fichier | Ligne | Code | Justification |
|---------|-------|------|---------------|
| `app_drawer.dart:1108` | `context.go(roleHome(newRole))` | Changement rôle → nouvelle pile |
| `app_drawer.dart:1592` | `context.go(route)` | Navigation drawer → entrée espace |
| `login/join_church_screen.dart:122` | `roleHome(AuthState().activeRole)` | Post-join → entrée espace |
| `login/join_church_screen.dart:137` | `context.go('/register?joinCode=...')` | Inscription avec code → entrée |
| `login/join_church_screen.dart:211` | `context.go('/login')` | Retour login → entrée |
| `login/join_church_screen.dart:242` | `context.go('/register?mode=church')` | Inscription église → entrée |
| `login/join_church_screen.dart:373` | `context.go('/login')` | Retour login → entrée |
| `login/login_screen.dart:99` | `roleHome(AuthState().activeRole)` | Post-login → entrée espace |
| `login/login_screen.dart:121` | `roleHome(AuthState().activeRole)` | Post-login (social) → entrée |
| `login/login_screen.dart:360` | `context.go('/accept-invitation')` | Invitation → entrée |
| `login/login_screen.dart:368` | `context.go('/register')` | Inscription → entrée |
| `login/login_screen.dart:386` | `context.go('/register?mode=church')` | Inscription église → entrée |
| `login/login_screen.dart:395` | `context.go('/join')` | Rejoindre → entrée |
| `login/register_screen.dart:318` | `roleHome(AuthState().activeRole)` | Post-inscription → entrée |
| `login/register_screen.dart:346` | `context.go('/login')` | Retour login → entrée |
| `login/register_screen.dart:577` | `context.go('/login')` | Erreur → retour login |
| `tenant/tenant_selection_screen.dart:87` | `roleHome(auth.activeRole...)` | Sélection tenant → entrée |
| `transfer/transfer_screen.dart:115` | `roleHome(AuthState().activeRole)` | Post-transfert → entrée |
| `tenant_selection_screen.dart:87` | `roleHome(auth.activeRole...)` | Sélection tenant → entrée |
| `profile/profile_screen.dart:75` | `context.go('/login')` | Déconnexion → entrée login |
| `platform/super_admin_provisioning_screen.dart:874` | `roleHome(AuthState().activeRole)` | Post-provisioning → entrée |
| `tenant/tenant_selection_screen.dart:87` | `roleHome(auth.activeRole...)` | Sélection tenant → entrée |
| `transfer/transfer_screen.dart:115` | `roleHome(AuthState().activeRole)` | Post-transfert → entrée |
| `login/join_church_screen.dart:211` | `context.go('/login')` | Retour login → entrée |
| `login/login_screen.dart:360` | `context.go('/accept-invitation')` | Invitation → entrée |
| `login/login_screen.dart:368` | `context.go('/register')` | Inscription → entrée |
| `login/login_screen.dart:386` | `context.go('/register?mode=church')` | Inscription église → entrée |
| `login/login_screen.dart:395` | `context.go('/join')` | Rejoindre → entrée |
| `login/register_screen.dart:346` | `context.go('/login')` | Retour login → entrée |
| `login/register_screen.dart:577` | `context.go('/login')` | Erreur → retour login |
| `onboarding/onboarding_screen.dart:35` | `context.go('/login')` | Onboarding → login |
| `profile/profile_screen.dart:75` | `context.go('/login')` | Déconnexion → login |
| `social_callback_screen.dart` | (si présent) | Callback OAuth → entrée |
| `magic_link_verify_screen.dart` | (si présent) | Vérification → entrée |
| `accept_invitation_screen.dart:217` | `context.go(destination)` | Post-acceptation → entrée |
| `accept_invitation_screen.dart:355` | `context.go('/login')` | Échec → login |
| `accept_invitation_screen.dart:410` | `context.go('/login')` | Échec → login |

### SOUS_ECRAN — CONVERT TO `push()` (≈95)

| Fichier | Ligne | Code | Cible |
|---------|-------|------|-------|
| `app_drawer.dart:1592` | `context.go(route)` | Navigation drawer → sous-écran |
| `not_found_screen.dart:115` | `context.go(link['route'])` | Lien 404 → push |
| `not_found_screen.dart:158` | `context.go('/dashboard')` | Fallback dashboard → push |
| `ar_onboarding/ar_onboarding_screen.dart:103` | `context.go(_routeAfter)` | AR onboarding → push |
| `reports/reports_screen.dart:208` | `context.go('/departments/$id/report')` | Détail rapport → push |
| `reports/reports_screen.dart:458` | `context.go(route)` | Drill-down rapport → push |
| `departments/department_member_dossier_screen.dart:242` | `context.go('/souls/$soulId')` | Détail âme → push |
| `departments/department_member_dossier_screen.dart:291` | `context.go('/departments/$_deptId')` | Retour département → push |
| `departments/department_detail_screen.dart:113` | `context.go('/departments/$_deptId/stats')` | Stats département → push |
| `departments/department_detail_screen.dart:236` | `context.go('/departments/$_deptId/manage')` | Gestion → push |
| `departments/department_detail_screen.dart:244` | `context.go('/departments/$_deptId/stats')` | Stats → push |
| `departments/department_detail_screen.dart:244` | `context.go('/departments/$_deptId/report')` | Rapport → push |
| `departments/department_detail_screen.dart:402` | `context.go('/souls/$soulId')` | Détail âme → push |
| `departments/department_detail_screen.dart:693` | `context.go('/departments/$_deptId/members/...')` | Détail membre → push |
| `departments/department_detail_screen.dart:775` | `context.go('/departments/$_deptId/members/...')` | Détail membre → push |
| `departments/department_detail_screen.dart:877` | `context.go('/families')` | Familles → push |
| `departments/department_management_screen.dart:119` | `context.go('/departments/$_deptId/stats')` | Stats → push |
| `departments/department_management_screen.dart:124` | `context.go('/departments/$_deptId/tools')` | Outils → push |
| `departments/department_management_screen.dart:336` | `context.go('/departments/$deptId/members/...')` | Détail membre → push |
| `departments/department_management_screen.dart:395` | `context.go('/departments/$deptId/members/...')` | Détail membre → push |
| `departments/department_management_screen.dart:877` | `context.go('/families')` | Familles → push |
| `departments/department_management_screen.dart:119` | `context.go('/departments/$_deptId/stats')` | Stats → push |
| `departments/department_management_screen.dart:124` | `context.go('/departments/$_deptId/tools')` | Outils → push |
| `departments/department_management_screen.dart:336` | `context.go('/departments/$deptId/members/...')` | Détail membre → push |
| `departments/department_management_screen.dart:395` | `context.go('/departments/$deptId/members/...')` | Détail membre → push |
| `login/join_church_screen.dart:122` | `context.go(roleHome(...))` | → KEEP (entrée) |
| `...` | (suite) | ... |

### POST_AUTH — KEEP `go()` (≈11)

| Fichier | Ligne | Code | Justification |
|---------|-------|------|---------------|
| `login/login_screen.dart:99` | `roleHome(AuthState().activeRole)` | Post-login → nouvelle pile |
| `login/login_screen.dart:121` | `roleHome(AuthState().activeRole)` | Post-login social → nouvelle pile |
| `login/register_screen.dart:318` | `roleHome(AuthState().activeRole)` | Post-inscription → nouvelle pile |
| `tenant/tenant_selection_screen.dart:87` | `roleHome(auth.activeRole...)` | Changement tenant → nouvelle pile |
| `transfer/transfer_screen.dart:115` | `roleHome(AuthState().activeRole)` | Post-transfert → nouvelle pile |
| `tenant/tenant_selection_screen.dart:87` | `roleHome(auth.activeRole...)` | Sélection tenant → nouvelle pile |
| `transfer/transfer_screen.dart:115` | `roleHome(AuthState().activeRole)` | Post-transfert → nouvelle pile |
| `profile/profile_screen.dart:75` | `context.go('/login')` | Déconnexion → login |
| `platform/super_admin_provisioning_screen.dart:874` | `roleHome(AuthState().activeRole)` | Post-provisioning → entrée |
| `login/join_church_screen.dart:122` | `roleHome(AuthState().activeRole)` | Post-join → entrée |
| `social_callback_screen.dart` | (si présent) | Callback OAuth → entrée |

---

## Résumé comptable

| Catégorie | Nombre estimé | Action |
|-----------|---------------|--------|
| ENTREE_ESPACE (keep `go()`) | ~35 | Aucun changement |
| POST_AUTH (keep `go()`) | ~11 | Aucun changement |
| SOUS_ECRAN (→ `push()`) | ~95 | **Conversion requise** |
| **Total** | **141** | |

---

## Prochaines étapes (D6)

1. **Un commit par conversion** : un fichier `context.go` → `context.push` par commit
2. **Un widget test par conversion** : `testWidgets` vérifiant `canPop()` après conversion
3. **Ordre suggéré** : par dossier (departments → dashboard → souls → etc.)
4. **Filet de sécurité** : `DetailBackButton` déjà existant comme fallback
5. **Bouton matériel Android** : vérifier `PopScope` sur chaque conversion

---

*Document généré le 2026-10-09. À mettre à jour à chaque conversion.*