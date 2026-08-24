#!/bin/bash
# start-services.sh — Start backend + frontend fully detached
set -e

PROJECT_DIR="$(cd "$(dirname "$0")" && pwd)"

# Kill any existing processes
pkill -f "discipolat-1.0.0.jar" 2>/dev/null || true
pkill -f "npm run dev" 2>/dev/null || true
sleep 2

echo "🔧 Démarrage de Discipolat..."

# Backend — use setsid to detach from terminal
setsid java -Xms256m -Xmx1g -XX:MaxMetaspaceSize=256m \
  -jar "$PROJECT_DIR/backend/target/discipolat-backend-1.0.0.jar" \
  --spring.profiles.active=beta \
  --spring.datasource.url=jdbc:postgresql://localhost:5433/discipolat \
  --spring.datasource.username=discipolat \
  --spring.datasource.password=discipolat_secret \
  --app.beta-testing.seed-demo-accounts=false \
  --app.beta-testing.demo-accounts-enabled=false \
  --app.environment=beta \
  --app.jwt.private-key-path=keys/private.pem \
  --app.jwt.public-key-path=keys/public.pem \
  </dev/null > /tmp/discipolat-backend.log 2>&1 &
BACKEND_PID=$!
echo "   Backend PID: $BACKEND_PID"

# Frontend
cd "$PROJECT_DIR/frontend"
setsid npm run dev </dev/null > /tmp/discipolat-frontend.log 2>&1 &
FRONTEND_PID=$!
echo "   Frontend PID: $FRONTEND_PID"

# Wait for backend
echo "⏳ Attente du backend..."
for i in $(seq 1 40); do
  sleep 1
  if curl -s http://localhost:8080/actuator/health >/dev/null 2>&1; then
    echo "✅ Backend: http://localhost:8080/"
    break
  fi
done

# Wait for frontend
for i in $(seq 1 15); do
  sleep 1
  if curl -s http://localhost:5173/ >/dev/null 2>&1; then
    echo "✅ Frontend: http://localhost:5173/"
    break
  fi
done

echo ""
echo "📋 Comptes de test (profil beta):"
echo "   Admin:       admin@discipolat.com / password123"
echo "   Pasteur:     pasteur@discipolat.com / password123"
echo "   Responsable: responsable@discipolat.com / password123"
echo "   Membre:      membre@discipolat.com / password123"
echo ""
echo "📋 Logs: tail -f /tmp/discipolat-backend.log"
echo "🛑 Arrêt: kill $BACKEND_PID $FRONTEND_PID"
