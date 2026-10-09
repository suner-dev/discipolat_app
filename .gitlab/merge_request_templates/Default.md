<!--
Modèle par défaut des merge requests GitLab (ADR-008 §3 : la pipeline est l'unique définition de
la preuve, le MR est l'endroit où elle devient lisible). Palier gratuit : les modèles du dépôt
sont disponibles sur tous les paliers — seules les règles d'approbation par utilisateur sont
Premium. Les variables %{...} sont substituées par GitLab parce que ce fichier est le modèle
DEFAULT ; dans un autre nom de fichier elles resteraient littérales.

Ce modèle n'ajoute AUCUNE porte : il reformule les portes déjà exécutées par .gitlab-ci.yml.
Une case qu'on ne peut pas cocher honnêtement se dit, elle ne se cache pas.
-->

# MR depuis `%{source_branch}` vers `%{target_branch}`

**En une phrase** : (ce que le client/la plateforme peut désormais faire ou ne plus casser)

**Décision qui autorise ce changement** : (ADR-00x §y / VALORISATION-PLATEFORME §z / — si aucune,
le dire : un changement sans décision est hors périmètre)

## 1. Preuve rouge → preuve verte

| | Commande | Résultat |
|---|---|---|
| **Avant** (le défaut existait) | | |
| **Après** (il ne peut plus exister) | | |

> Une correction sans test rouge préalable n'est pas une correction : c'est un changement de
> comportement. Si le lot est documentaire ou de la pure configuration, écrire « non applicable,
> parce que… » dans les deux lignes.

## 2. Non-régression (à cocher seulement si réellement exécutée)

- [ ] Backend `mvn -o test` → **2 178** tests, 0 échec (les skips Docker/Redis comptabilisés)
- [ ] Gates PostgreSQL → **19/19** (`FlywayMigrationChainPostgreSqlTest,EventTableContractTest`) — **obligatoire si un fichier `db/migration/` est touché**
- [ ] Frontend `tsc -b` 0 erreur · `vitest run` **862/862** · ratchets `i18n:audit` et `debt:audit` respectés
- [ ] Mobile `flutter test` (116 fichiers) si `mobile/` est touché — sinon l'écrire explicitement
- [ ] Gel d'architecture : `architecture-freeze.txt` ne **monte** pas (R1 ≤ 7 867 · R2 = 0 · R4 ≤ 2 948, R3/R5/R6 sans nouvelle clé)

```bash
git diff --stat backend/src/test/resources/architecture/architecture-freeze.txt   # attendu : vide
```

## 3. Contrats et surface publique

- [ ] Aucune route supprimée ni changée de sens (contrat HTTP gelé)
- [ ] Aucun champ de payload d'événement renommé sans bump de version (ADR-006 §3.1.2)
- [ ] Taille : `scripts/report-size.sh` n'introduit aucun fichier > 500 lignes, ou la dérogation est justifiée ici :

## 4. Hygiène

- [ ] Aucun secret dans le code, les logs, ni l'historique (`gitleaks`/Secret Detection)
- [ ] `KNOWN_ISSUES.md` : nouvelle ligne si un défaut est **contourné** plutôt que corrigé (A3/A4 pour le précédent)
- [ ] `TODO_BACKEND_V0_CLEAN_ARCH.md` : ligne de journal datée (quoi, preuve, commit)
- [ ] Docs d'architecture mises à jour si une décision a changé (ADR-REGISTRE + lien)

## 5. Ce que le réviseur doit lire en premier

(par ordre de risque, pas de bas en haut — 3 lignes maximum)

---

<details>
<summary>Rappel : ce MR ne fusionne que si la pipeline est verte</summary>

`only_allow_merge_if_pipeline_succeeds` est posé sur le projet (`scripts/gitlab-init-project.sh
finalize`). Les jobs `sbom:release` et `scan:image` sont en `allow_failure: true` tant que leur
premier vert n'a pas été observé en pipeline réelle — la condition de bascule est dans
`.gitlab-ci.yml`, pas dans ma tête.

</details>
