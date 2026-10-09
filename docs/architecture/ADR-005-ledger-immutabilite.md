# ADR-005 — Ledger append-only en partie double, solde dérivé, idempotence sur toute écriture d'argent

> **Statut** : **Accepté sur le principe** (humain 2026-10-09, « fais tout ce que tu proposes à
> trancher ») — **exécution non démarrée**. Ce document **ne change aucune ligne de code** ; il
> fixe la décision de modèle de données et le périmètre du premier lot exécutable.
> **Date** : 2026-10-09. **Mesures** : exécutées sur `main` @ `9148caa1`.
> **Ordre** : placé **avant** ADR-006 (data/IA) — arbitrage humain explicite du 2026-10-09.
> **Dépendance dure** : exécution **après** V0-F (rupture des 45 couples réciproques), sinon le
> contexte `finances` ne peut pas être extrait proprement.

---

## 1. Le problème, mesuré (et non opinioné)

| # | Fait vérifiable | Où |
|---|---|---|
| 1 | Le modèle est en **écriture simple**, pas en partie double : `type VARCHAR(20) CHECK (type IN ('RECETTE','DEPENSE'))`, un seul `montant NUMERIC(14,2) CHECK (montant >= 0)` | `db/migration/V68__finances_module.sql:9-20` |
| 2 | **Aucun rattachement écriture ↔ compte** : `grep -rn "account_id\|accountId" modules/finances` renvoie **0 résultat**, y compris sur l'entité `FinanceTransaction` (104 l., 16 champs persistés, pas de compte) | `modules/finances/domain/FinanceTransaction.java` |
| 3 | Le solde d'un compte est **déclaré par le client** : `createAccount(...)` construit l'entité avec `.balance(decimalOrDefault(body.get("balance"), ZERO))`, et **aucun code ne recalcule** `balance` à partir des transactions (`setBalance` : 0 occurrence) | `modules/finances/domain/FinanceService.java:504-515` |
| 4 | Les écritures sont **mutables** : colonnes `updated_at`, `deleted` (suppression logique) et `reconciled` modifiables → un historique financier peut être réécrit après coup | `V68` + `V237__finance_transactions_reconciled.sql` |
| 5 | La **gestion multi-devises est déjà correcte** : `montant BigDecimal`, `montantMinor Long` (unités mineures), `devise` ISO 4217 (V190), `tauxVersBase`/`montantBase` | `FinanceTransaction.java:52-69` |
| 6 | Une **réconciliation existe déjà** : `FinanceReconciliationService` (221 l.) rapproche `finance_bank_statement_lines` et les écritures, et manipule une notion d'idempotence | `modules/finances/service/` |
| 7 | Les rails de paiement sont **déjà écrits** (42 fichiers / 5 303 l. : MTN MoMo, Orange Money, M-Pesa, Stripe, PayPal payout, `MobileMoneyProviderRegistry`, `WebhookLog`, `WebhookSignatureVerifier`) — **mais l'argent entrant n'atterrit pas dans un journal** | `modules/payments/` |
| 8 | Trois défauts de typage monétaire : `CurrencyService.convertAmount(Double, …)`, `CurrencyController` qui convertit en `double`, `InventoryItem.totalMaintenanceCost` en `Double` | lignes 63 / 62 / 89 de ces fichiers |

**Synthèse honnête** : il y a un *module finances* sérieux (devise, budgets, dons, tontines, rapprochement
bancaire). Il n'y a **pas de ledger**. La question de due-diligence « pouvez-vous prouver que le solde
d'un compte égale la somme de ses écritures, et que ces écritures n'ont pas été modifiées ? » a
aujourd'hui pour réponse : **non**, et le solde stocké n'est pas non plus garanti provenir d'une écriture.

## 2. Pourquoi c'est le levier n°1 de valorisation (et pas de la kosmetik)

- Le **take-rate** (revenu en % d'un flux) est le seul modèle qui multiplie la valeur sans multiplier
  les sièges. Il exige un journal dont on peut **prouver l'exhaustivité** : chaque centime perçu est
  une écriture, chaque écriture a une cause, la somme est contrôlable contre l'extrait du provider.
- Sans ledger : pas de **licence/partenariat bancaire**, pas d'assurance auditeur, et un refus probable
  d'un processeur de paiement en due-diligence. Avec : le module `finances` devient **vendable séparément**
  (une brique, une frontière, un contrat).
- Coût de report **asymétrique** : c'est une décision de **modèle de données**. Chaque mois de volume
  écrit sur `finance_transactions` rend le backfill plus long et plus litigieux. Un broker (ADR-006)
  ou un lakehouse sont des décisions d'**infrastructure** : réversibles, achetables plus tard.

## 3. Décision

1. **Journal append-only** : nouvelle table `finance_journal_entry` (une écriture = une cause, un
   `occurred_at`, une `idem_key`, un `aggregate` d'origine) + `finance_journal_post` (au moins **deux
   lignes de post**, `account_id`, `direction` D/C, `montant_minor BIGINT`, `devise CHAR(3)`,
   `montant_minor_base BIGINT`). Contrainte d'équilibre **au niveau de la base** :
   `CHECK` différé ou trigger `assert_balanced(entry_id)` — la partie double doit être **impossible à
   violer depuis le code**, sinon elle sera violée.
2. **Le solde devient une vue dérivée**, plus une colonne mutable. `finance_accounts.balance` est
   conservée pour la compatibilité de lecture, **marquée non faisant foi**, et recalculée par
   `finance_account_balances` (vue matérialisée rafraîchie, puis projection Redis au sens d'ADR-006).
3. **Idempotence sur toute écriture d'argent** : `UNIQUE (tenant_id, idem_key)` ; la clé vient du
   client (déjà pratiqué dans `payments`) ou du `event_id` du webhook provider. Un replay de webhook
   **ne doit pas** créer un second post — c'est le scénario de double débit que paie un acquéreur.
4. **Unités mineures partout** : `BIGINT` + `CHAR(3)` ISO 4217. La règle de conversion devient : on
   stocke le montant **dans la devise de l'opération** et sa contre-valeur **au taux du moment**, jamais
   un `double`. Les trois sites en `Double`/`double` (§1.8) passent en `BigDecimal`/unités mineures.
5. **Interdiction de muter et de supprimer** une écriture : `updated_at`/`deleted` retirés du chemin
   d'écriture des posts ; une correction est une **écriture inverse** (redressement) liée à
   l'originale. Le `reconciled boolean` devient un champ de **rapprochement** séparé (table
   `finance_reconciliation`) qui ne touche pas l'écriture.
6. **Saga de remboursement** explicite (demande → provision provider → post de remboursement →
   contrôle de parité), jamais un post isolé inséré à la main.
7. **`payments` continue de vivre** : la façade multi-rails existante (`MobileMoneyProviderRegistry`)
   est **le bon design**, on ne la touche pas. Règle nouvelle : l'adaptateur de rail **écrit dans le
   journal**, le domaine ne connaît aucun rail (déjà la grammaire d'ADR-002).
8. **Migration sans big-bang** : (a) créer les tables + trigger ; (b) **backfill** idempotent et
   rejouable depuis `finance_transactions` existantes (chaque RECETTE → post D sur le compte de
   trésorerie résolu par `categorie`, contrepartie = compte d'accès) ; (c) **double-lecture**
   pendant une fenêtre : le code compare solde déclaré vs solde dérivé et **journalise l'écart** sans
   le bloquer ; (d) couper l'écriture du solde déclaré quand l'écart est nul sur la fenêtre.
   Aucun `UPDATE` destructif sur l'historique : le backfill écrit des lignes nouvelles.

## 4. Ce que cet ADR ne fait PAS

- Il **n'éclate pas** `FinanceService` (821 l.) en microservices ; il ajoute une frontière.
- Il **ne réécrit pas** l'API : les routes `/finances/*` (`FinanceController`, `@RequestMapping("/api/v1/finances")`)
  et leurs payloads sont **gelés** (règle A4 du plan V0). Les clients existants (web, mobile, reçus
  fiscaux `TaxReceiptService`) ne voient rien.
- Il **n'introduit pas** de blockchain, de jeton, ni de « ledger publie ». Le besoin est
  l'immutabilité **prouvable**, pas la distribution.
- Il **ne déclenche pas** PCI-DSS complet : la scope PCI réduite passe par tokenisation + champs
  hébergés (§7 d'ADR-005 n'est pas ce lot).

## 5. Options rejetées (et pourquoi)

| Option | Rejetée parce que |
|---|---|
| Garder l'écriture simple et exposer un solde calculé en SQL (`SUM`) | règle le §1.3 mais pas l'absence de compte (§1.2) ni l'immutabilité (§1.4) ; un `SUM` sur une table mutable n'est pas une preuve |
| Acheter un moteur de ledger tiers | ajoute une dépendance critique et un transport de données ; le métier (dons, tontines, budgets) est déjà ici, le ledger n'est pas notre différenciateur |
| Faire d'abord P4 (événements) ou P5 (data) | un lac d'événements rempli depuis une source dont le solde n'est pas vérifiable **fige** l'erreur à grande échelle ; et c'est l'infrastructure, donc réversible et différable |
| Attendre la fin de V0 avant tout ledger | la fenêtre de non-écriture se paie en volume à backfiller ; on **décide** maintenant (coût nul) et on **exécute** après V0-F |
| Migration « gros bang » pendant une fenêtre de maintenance | 135 modules et un `deleted` logique déjà présent partout : un cut-over sans double-lecture ne peut pas être prouvé avant qu'il casse |

## 6. Lot exécutable (une fois V0-F fait) — chaque tâche rouge **puis** verte

| Tâche | Contenu | Preuve rouge | Preuve verte |
|---|---|---|---|
| **L1** | Tables `finance_journal_entry` / `finance_journal_post` + trigger d'équilibre + contrainte d'idempotence (migration Flyway `V2xx`) | un post déséquilibré inséré en SQL **doit** échouer ; un replay d'`idem_key` **doit** échouer | migration appliquée sur PostgreSQL 16, test de contrainte vert |
| **L2** | Écriture : `FinanceService.createTransaction` produit **2 posts** et ne touche plus `balance` | test d'invariant `solde_dérivé == somme(posts)` cassé avant, vert après | les tests `finances` existants passent **sans modification** |
| **L3** | Backfill idempotent + vue `finance_account_balances` + double-lecture qui journalise l'écart | l'écart déclaré↔dérivé est **visible** sur les données réelles (chiffre publié dans le rapport) | écart nul sur la fenêtre, ou liste nommée des comptes à corriger manuellement |
| **L4** | Rails : `WebhookSignatureVerifier` / providers écrivent dans le journal avec `event_id` comme clé d'idempotence | rejouer un webhook Stripe **double** aujourd'hui → test rouge | rejouer un webhook est **sans effet** sur le journal |

**Gates** : `mvn -o test` (2 178) inchangé et vert ; gates PostgreSQL 19/19 ; gel ArchUnit **non
augmenté** (le nouveau code part dans les couches `domain/application/adapters` d'ADR-002, il ne doit
pas ajouter une arête R3) ; contrat `/finances/*` inchangé.

**Contrainte mesurée sur le périmètre** (rejouée le 2026-10-09 : `mvn -o test -Dtest=ArchitectureRulesTest`,
rapport `target/architecture-report.txt`) : `finances`, `payments`, `transfers`, `users`, `tenants`,
`core` font partie du **composant fortement connexe de 41 contexts** — pas `currency` ni `tontine`.
Conséquence : L1..L4 s'exécutent **dans le monolithe** (ils n'extraient rien), et l'idempotence doit
être posée sur les rails `payments` **sans** les sortir de leur module. Prétendre le contraire
produirait exactement le distributed monolith refusé en VALORISATION §6.

## 7. Comment revérifier les faits de §1

```bash
cd backend/src/main/resources/db/migration && grep -n -A12 "CREATE TABLE IF NOT EXISTS finance_transactions" V68__finances_module.sql
grep -rn "account_id\|accountId" backend/src/main/java/com/discipolat/modules/finances            # 0
sed -n '504,516p' backend/src/main/java/com/discipolat/modules/finances/domain/FinanceService.java # balance vient du corps de requete
grep -rn "setBalance" backend/src/main/java --include=*.java                                      # 0
grep -rn "montantMinor\|tauxVersBase" backend/src/main/java/com/discipolat/modules/finances/domain/FinanceTransaction.java
grep -rn "Double convertAmount" backend/src/main/java/com/discipolat/modules/currency/domain/CurrencyService.java
```
