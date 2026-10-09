# Récupération du lot sécurité non commité dans `main` (TODO reprise §0.2 / §1.2)

## Ce qui a été trouvé (vérifié le 2026-09-29, worktree `/home/arise/discipolat/discipolat_app`)

`main` @ `72ec85d5` portait un lot **non commité d'une autre campagne**, intact à ce jour :

| Fichier | Contenu |
|---|---|
| `backend/.../users/domain/User.java` | `@JsonProperty(WRITE_ONLY)` sur `twoFactorSecret` + `twoFactorBackupCodes` (fuite JSON de secrets 2FA par sérialisations imbriquées) |
| `backend/.../payments/api/PaymentController.java` | webhook fail-closed (503 sans secret) |
| `backend/.../ussd/api/UssdController.java` | webhook fail-closed (503 sans secret) |
| `mobile/lib/data/local/sync_service.dart`, `offline_sync_manager.dart`, **nouveau** `sync_lock.dart` | verrou de synchronisation |
| `render.yaml`, `.github/workflows/backup-postgres.yml` | durcissement déploiement/backup |
| **nouveau** `scripts/bootstrap_prod.sql` | amorçage production |

Hors périmètre de ce bundle (décision d'orchestrateur explicite du TODO §0.3) :
les modifications des **documents d'autorité** (`AGENT_ORCHESTRATION.md`, plan) restent
en l'air dans `main` — elles appartiennent à l'humain. Bruit de build volontairement
exclu : `frontend/dist-ts/*`, `frontend/test-results/`.

## Ce qui a été fait — sans toucher à `main` (règle R1)

1. Bundle complet reconstitué et versionné ici : **`SECURITY_LOT_MAIN.patch`**
   (593 lignes, 10 fichiers : les 9 du lot + le test de non-régression §1.2).
   Applicable depuis `72ec85d5` nu : `git apply --check` puis `git apply` → **OK vérifié**.
2. **Test de non-régression imposé par §1.2** : `UserSecretSerializationTest`
   (3 cas : entité seule, imbriquée dans une Map « réponse brute santé/transfers »,
   en collection ; asserts d'absence de `twoFactorSecret`, `twoFactorBackupCodes`,
   `passwordHash`). Livré dans le patch.
3. Exécution sur un **worktree jetable** détaché à `72ec85d5` + patch appliqué :
   `mvn -B -o test -Dtest=UserSecretSerializationTest` → **3/3 PASS, EXIT=0** (0,673 s).
4. **Preuve de discrimination** (le test doit être rouge sans le correctif) : même
   arbre, `User.java` revenu à l'état nu → le test doit échouer. Résultat exact
   archivé ci-dessous §Preuves.

## Ce que l'orchestrateur humain doit décider (nécessaire, pas optionnel — §0.2)

Appliquer le bundle sur une **branche dédiée** (jamais dans la branche de campagne) :

```bash
cd <repo>   # worktree principal
git switch -c fix/securite-secrets-json 72ec85d5
git apply reports/plan-2agents/security-recovery/SECURITY_LOT_MAIN.patch
# puis revues des faces mobile/render/workflow (non testées ici, hors backend)
```

Limites assumées : les faces **mobile (dart)**, `render.yaml` et le workflow de backup
sont préservées et rejouables à l'identique, mais **exécutées uniquement pour le lot
backend** dans cette récupération. Un `flutter analyze`/`dart test` sur la branche
dédiée reste à faire par le propriétaire de ce lot.

## Preuves

```text
$ git apply --check SECURITY_LOT_MAIN.patch   (arbre nu 72ec85d5)   → OK (silencieux)
$ mvn -B -o test -Dtest=UserSecretSerializationTest                 → 3/3 PASS, BUILD SUCCESS, EXIT=0
  [INFO] Tests run: 3, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.673 s
  -- in com.discipolat.modules.users.domain.UserSecretSerializationTest
$ (User.java revenu à l'état nu) idem                               → ROUGE, EXIT=1 :
  [ERROR] Tests run: 3, Failures: 3, Errors: 0, Skipped: 0
    UserSecretSerializationTest.userAloneLeaksNoSecret:58
    UserSecretSerializationTest.nestedUserInMapLeaksNoSecret:77
    UserSecretSerializationTest.userInCollectionLeaksNoSecret:89
```

Le test est donc **discriminant** : vert avec le `WRITE_ONLY`, rouge sans lui —
ce n'est pas une assertion décorative. Le worktree jetable de récupération a été
démonté après exécution ; `main` n'a jamais été touché (aucun commit, aucun
déplacement de fichier dans le worktree principal).
