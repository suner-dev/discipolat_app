# Test Stripe Live - Guide d'exécution (TODO_TARIFS_PAIEMENTS_INTERNATIONAL T10.5)

## Objectif
Valider les corrections F1 (unités mineures) et F2 (Price ID pour XAF) avec un vrai compte Stripe test.

## Prérequis

1. **Compte Stripe test** : `sk_test_...` et `whsec_...`
2. **Stripe CLI** : `brew install stripe/stripe-cli/stripe` ou téléchargement
3. **Compte admin Discipolat** : superadmin + tenant admin pour tester

## Variables d'environnement requises

```bash
export STRIPE_SECRET_KEY=sk_test_xxx
export STRIPE_WEBHOOK_SECRET=whsec_xxx
```

## Lancement

```bash
# Depuis la racine du projet
chmod +x scripts/stripe_live_test.sh
./scripts/stripe_live_test.sh
```

## Ce que le script teste

### 1. Tunnel webhook Stripe
- `stripe listen --forward-to http://localhost:8080/api/v1/payments/webhooks/stripe`
- Capture les événements `checkout.session.completed`, `customer.subscription.created`

### 2. Checkout GROWTH (EUR - inline price)
- Vérifie que `unit_amount = 2900` (29 EUR en centimes)
- **F1** : Avant correction, envoyait 29 (major) → 0,29 € facturé

### 3. Checkout STARTUP (XAF - Price ID requis)
- Sans Price ID → erreur `STRIPE_PRICE_ID_REQUIRED`
- Après sync admin → checkout passe

### 4. Changement de montant (Price rotation)
- Admin modifie prix STARTUP → 20000 XAF
- `POST /platform/admin/pricing/plans/STARTUP/prices/stripe-sync`
- Nouveau Price créé, ancien désactivé (`active=false`)
- Ancien abonnement continue sur l'ancien Price

### 4. Webhooks reçus
- `checkout.session.completed` → fige prix sur abonnement
- `customer.subscription.created` → active l'abonnement

## Critères de succès

| Test | Critère | Faille couverte |
|------|---------|-----------------|
| GROWTH checkout | `unit_amount = 2900` (pas 29) | **F1** |
| STARTUP sans Price | Erreur `STRIPE_PRICE_ID_REQUIRED` | **F2** |
| STARTUP avec Price | Checkout réussi | **F2** |
| Changement montant | Nouvel ID Price, ancien `active=false` | F1/F2 |
| Webhook reçu | `checkout.session.completed` reçu | F1/F2 |
| Abonnement créé | `price_minor = 2900` sur `tenant_subscriptions` | **F1** |

## Dépannage

| Symptôme | Cause probable | Solution |
|----------|----------------|----------|
| `STRIPE_DISABLED` | `STRIPE_SECRET_KEY` vide | Vérifier export |
| `STRIPE_PRICE_ID_REQUIRED` sur XAF | Pas de Price synchro | Sync admin ou saisie manuelle |
| `PLAN_PRICE_MISSING` | Plan sans prix dans devise | Ajouter prix via admin |
| Webhook 404 | Tunnel Stripe arrêté | Relancer `stripe listen` |
| Webhook 500 | Secret webhook invalide | Vérifier `STRIPE_WEBHOOK_SECRET` |

## Logs utiles

```bash
# Logs Spring Boot
tail -f /tmp/spring_boot.log

# Logs tunnel Stripe
tail -f /tmp/stripe_listen.log

# Base de données (vérifier prix figé)
psql -c "SELECT price_minor, price_currency, price_source FROM tenant_subscriptions;"
```

## Automatisation CI (optionnel)

Pour intégrer dans la CI, utiliser les secrets GitHub Actions :
```yaml
env:
  STRIPE_SECRET_KEY: ${{ secrets.STRIPE_SECRET_KEY }}
  STRIPE_WEBHOOK_SECRET: ${{ secrets.STRIPE_WEBHOOK_SECRET }}
```

Et ajouter un job qui lance le script.

## Note sur `KNOWN_ISSUES.md:33`

Une fois ce test passé avec succès, mettre à jour :
- `KNOWN_ISSUES.md` : J3 → `RÉSOLU 2026-10-XX` (avec commit de référence)
- Fermer l'issue "Bloquant monétisation"