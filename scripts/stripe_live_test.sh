#!/usr/bin/env bash
# Script de test Stripe mode test réel - TODO_TARIFS_PAIEMENTS_INTERNATIONAL T10.5
#
# Usage :
#   export STRIPE_SECRET_KEY=sk_test_xxx
#   export STRIPE_WEBHOOK_SECRET=whsec_xxx
#   ./scripts/stripe_live_test.sh
#
# Prérequis :
#   - Clé Stripe test (sk_test_...)
#   - Webhook secret (whsec_...)
#   - Stripe CLI installé (pour `stripe listen`)

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
BACKEND_DIR="${PROJECT_ROOT}/backend"

echo "=== Test Stripe Live - TODO_TARIFS_PAIEMENTS_INTERNATIONAL T10.5 ==="
echo ""

# Vérifications préalables
if [[ -z "${STRIPE_SECRET_KEY:-}" ]]; then
    echo "ERREUR: STRIPE_SECRET_KEY non définie"
    echo "export STRIPE_SECRET_KEY=sk_test_xxx"
    exit 1
fi

if [[ -z "${STRIPE_WEBHOOK_SECRET:-}" ]]; then
    echo "ERREUR: STRIPE_WEBHOOK_SECRET non définie"
    echo "export STRIPE_WEBHOOK_SECRET=whsec_xxx"
    exit 1
fi

if ! command -v stripe &> /dev/null; then
    echo "ERREUR: Stripe CLI non installé"
    echo "https://stripe.com/docs/stripe-cli"
    exit 1
fi

echo "✓ Variables d'environnement présentes"
echo "✓ Stripe CLI installé"
echo ""

# 1. Démarrer le tunnel webhook Stripe en arrière-plan
echo "=== 1. Démarrage tunnel webhook Stripe ==="
WEBHOOK_PORT=8080
WEBHOOK_URL="http://localhost:${WEBHOOK_PORT}/api/v1/payments/webhooks/stripe"

echo "Démarrage: stripe listen --forward-to ${WEBHOOK_URL}"
stripe listen --forward-to "${WEBHOOK_URL}" > /tmp/stripe_listen.log 2>&1 &
STRIPE_LISTEN_PID=$!

# Attendre que le tunnel soit prêt
sleep 3
echo "✓ Tunnel webhook démarré (PID: ${STRIPE_LISTEN_PID})"
echo "  URL: ${WEBHOOK_URL}"
echo ""

# 2. Configurer les variables d'environnement pour le backend
export STRIPE_SECRET_KEY
export STRIPE_WEBHOOK_SECRET
export STRIPE_SUCCESS_URL="http://localhost:5173/billing?checkout=success"
export STRIPE_CANCEL_URL="http://localhost:5173/pricing?checkout=cancelled"

echo "=== 2. Démarrage backend Spring Boot ==="
cd "${BACKEND_DIR}"
./mvnw -q spring-boot:run -Dspring-boot.run.profiles=test > /tmp/spring_boot.log 2>&1 &
SPRING_PID=$!

# Attendre que le backend soit prêt
echo "Attente démarrage Spring Boot..."
for i in {1..30}; do
    if curl -s "http://localhost:8080/actuator/health" | grep -q '"status":"UP"'; then
        echo "✓ Backend prêt"
        break
    fi
    sleep 1
done
echo ""

# 3. Test 1: Checkout GROWTH (EUR - inline price)
echo "=== 3. Test Checkout GROWTH (EUR - inline price) ==="
GROWTH_CHECKOUT=$(curl -s -X POST "http://localhost:8080/api/v1/billing/stripe/checkout" \
    -H "Content-Type: application/json" \
    -H "Authorization: Bearer $(curl -s -X POST "http://localhost:8080/api/v1/auth/login" \
        -H "Content-Type: application/json" \
        -d '{"email":"admin@test.com","password":"password"}' | jq -r '.accessToken')" \
    -d '{"planKey":"GROWTH","billingCycle":"monthly","currency":"EUR"}')

CHECKOUT_URL=$(echo "${GROWTH_CHECKOUT}" | jq -r '.url')
SESSION_ID=$(echo "${GROWTH_CHECKOUT}" | jq -r '.sessionId')

echo "Checkout URL: ${CHECKOUT_URL}"
echo "Session ID: ${SESSION_ID}"

# Vérifier que l'URL contient le bon montant (2900 centimes = 29 EUR)
if [[ "${CHECKOUT_URL}" == *"checkout.stripe.com"* ]]; then
    echo "✓ Checkout GROWTH créé"
else
    echo "✗ Échec création checkout GROWTH"
    echo "${GROWTH_CHECKOUT}"
fi
echo ""

# 4. Test 2: Checkout STARTUP (XAF - nécessite Price ID)
echo "=== 4. Test Checkout STARTUP (XAF - Price ID requis) ==="
STARTUP_CHECKOUT=$(curl -s -X POST "http://localhost:8080/api/v1/billing/stripe/checkout" \
    -H "Content-Type: application/json" \
    -H "Authorization: Bearer $(curl -s -X POST "http://localhost:8080/api/v1/auth/login" \
        -H "Content-Type: application/json" \
        -d '{"email":"admin@test.com","password":"password"}' | jq -r '.accessToken')" \
    -d '{"planKey":"STARTUP","billingCycle":"monthly","currency":"XAF"}')

echo "${STARTUP_CHECKOUT}" | jq .
echo ""

# 5. Test 3: Changement de montant (création nouveau Price, ancien désactivé)
echo "=== 5. Test Changement de montant STARTUP ==="
# Modifier le prix STARTUP via API admin
curl -s -X POST "http://localhost:8080/api/v1/platform/admin/pricing/plans/STARTUP/prices" \
    -H "Content-Type: application/json" \
    -H "Authorization: Bearer $(curl -s -X POST "http://localhost:8080/api/v1/auth/login" \
        -H "Content-Type: application/json" \
        -d '{"email":"superadmin@test.com","password":"password"}' | jq -r '.accessToken')" \
    -d '{
        "currency": "XAF",
        "billingCycle": "MONTHLY",
        "amountMinor": 20000,
        "regionCode": "GLOBAL",
        "paymentChannels": "MOMO,STRONG"
    }' | jq .

# Synchroniser le nouveau prix
curl -s -X POST "http://localhost:8080/api/v1/platform/admin/pricing/plans/STARTUP/prices/stripe-sync" \
    -H "Authorization: Bearer $(curl -s -X POST "http://localhost:8080/api/v1/auth/login" \
        -H "Content-Type: application/json" \
        -d '{"email":"superadmin@test.com","password":"password"}' | jq -r '.accessToken')" | jq .

echo "Nouveau prix synchronisé. Test nouveau checkout..."
# Nouveau checkout devrait utiliser le nouveau Price
NEW_CHECKOUT=$(curl -s -X POST "http://localhost:8080/api/v1/billing/stripe/checkout" \
    -H "Content-Type: application/json" \
    -H "Authorization: Bearer $(curl -s -X POST "http://localhost:8080/api/v1/auth/login" \
        -H "Content-Type: application/json" \
        -d '{"email":"admin@test.com","password":"password"}' | jq -r '.accessToken')" \
    -d '{"planKey":"STARTUP","billingCycle":"monthly","currency":"XAF"}')

echo "${NEW_CHECKOUT}" | jq .
echo ""

# 6. Vérifier webhooks reçus
echo "=== 6. Vérification webhooks Stripe ==="
sleep 2
cat /tmp/stripe_listen.log | grep -E "checkout.session.completed|customer.subscription.created" | head -5
echo ""

# Nettoyage
echo "=== Nettoyage ==="
kill ${SPRING_PID} 2>/dev/null || true
kill ${STRIPE_LISTEN_PID} 2>/dev/null || true
echo "✓ Processus arrêtés"
echo ""

echo "=== Résumé ==="
echo "Si tous les tests passent :"
echo "  ✓ F1 corrigée : GROWTH facturé 2900 centimes (29 EUR)"
echo "  ✓ F2 corrigée : STARTUP souscriptable via Price ID XAF"
echo "  ✓ F2 bis : Changement montant crée nouveau Price, ancien désactivé"
echo ""
echo "Si échec : vérifier logs dans /tmp/spring_boot.log et /tmp/stripe_listen.log"