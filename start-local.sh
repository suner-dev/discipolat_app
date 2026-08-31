#!/bin/bash
# start-local.sh — Démarrage local du backend + frontend
set -e

PROJECT_DIR="$(cd "$(dirname "$0")" && pwd)"

echo "🔧 Démarrage de Discipolat en local..."

# --- Backend ---
echo "📦 Démarrage du backend (Spring Boot, port 8080)..."
cd "$PROJECT_DIR"
nohup java -jar backend/target/discipolat-backend-1.0.0.jar \
  --spring.profiles.active=beta \
  --spring.datasource.url=jdbc:postgresql://localhost:5433/discipolat \
  --spring.datasource.username=discipolat \
  --spring.datasource.password=discipolat_secret \
  --app.beta-testing.seed-demo-accounts=true \
  --app.beta-testing.demo-accounts-enabled=true \
  --app.environment=beta \
  --app.jwt.private-key-path=keys/private.pem \
  --app.jwt.public-key-path=keys/public.pem \
  --app.encryption.aes-key="$ENCRYPTION_AES_KEY" \
  </dev/null > /tmp/discipolat-backend.log 2>&1 &
BACKEND_PID=$!
echo "   PID: $BACKEND_PID"

# --- Frontend ---
echo "🎨 Démarrage du frontend (Vite, port 5173)..."
cd "$PROJECT_DIR/frontend"
nohup npm run dev </dev/null > /tmp/discipolat-frontend.log 2>&1 &
FRONTEND_PID=$!
echo "   PID: $FRONTEND_PID"

# --- Attendre que les services soient prêts ---
echo "⏳ Attente du démarrage..."
sleep 15

# Vérifier le frontend
if kill -0 $FRONTEND_PID 2>/dev/null; then
  echo "✅ Frontend: http://localhost:5173/"
else
  echo "❌ Frontend a crashé — voir /tmp/discipolat-frontend.log"
fi

# Vérifier le backend
if kill -0 $BACKEND_PID 2>/dev/null; then
  if curl -s http://localhost:8080/actuator/health >/dev/null 2>&1; then
    echo "✅ Backend: http://localhost:8080/"
  else
    echo "⏳ Backend: en cours de démarrage (PID $BACKEND_PID) — attendre encore ~10s"
  fi
else
  echo "❌ Backend a crashé — voir /tmp/discipolat-backend.log"
fi

echo ""
echo "📋 Comptes de test (profil beta):"
echo "   Admin:       admin@discipolat.com / password123"
echo "   Pasteur:     pasteur@discipolat.com / password123"
echo "   Responsable: responsable@discipolat.com / password123"
echo "   Chef:        chef@discipolat.com / password123"
echo "   Faiseur:     faiseur@discipolat.com / password123"
echo "   Membre:      membre@discipolat.com / password123"
echo ""
echo "📋 Logs:"
echo "   Backend:  tail -f /tmp/discipolat-backend.log"
echo "   Frontend: tail -f /tmp/discipolat-frontend.log"
echo ""
echo "🛑 Arrêt: kill $BACKEND_PID $FRONTEND_PID"
