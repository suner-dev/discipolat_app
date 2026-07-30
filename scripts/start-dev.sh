#!/usr/bin/env bash
# ============================================
# Discipolat — Démarrage en environnement de développement
# ============================================
# Ce script :
# 1. S'assure que le profil Spring est 'default' (pas 'docker')
# 2. Démarre le backend Spring Boot
# 3. Démarre le frontend Vite
# ============================================
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"
cd "$PROJECT_DIR"

# Vérifier les outils requis
command -v nc >/dev/null 2>&1 || { echo "❌ nc (netcat) est requis"; exit 1; }
command -v curl >/dev/null 2>&1 || { echo "❌ curl est requis"; exit 1; }
command -v mvn >/dev/null 2>&1 || { echo "❌ Maven est requis"; exit 1; }
command -v npm >/dev/null 2>&1 || { echo "❌ Node.js/npm est requis"; exit 1; }

# Couleurs pour les logs
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

log()   { echo -e "${GREEN}[✓]${NC} $1"; }
info()  { echo -e "${BLUE}[i]${NC} $1"; }
warn()  { echo -e "${YELLOW}[!]${NC} $1"; }
error() { echo -e "${RED}[✗]${NC} $1"; }

cleanup() {
    info "Arrêt des services..."
    kill $BACKEND_PID 2>/dev/null || true
    kill $FRONTEND_PID 2>/dev/null || true
    wait 2>/dev/null || true
    info "Services arrêtés."
}
trap cleanup EXIT INT TERM

# ============================================
# Prérequis
# ============================================

info "Vérification des prérequis..."

# Vérifier PostgreSQL
if ! nc -z localhost 5432 2>/dev/null; then
    error "PostgreSQL n'est pas en cours d'exécution sur localhost:5432"
    info "Assurez-vous que PostgreSQL est démarré."
    info "→ Sous Linux : sudo systemctl start postgresql"
    info "→ Sous Docker : docker compose up -d db"
    exit 1
fi
log "PostgreSQL disponible sur localhost:5432"

# Vérifier les clés JWT
if [ ! -f keys/private.pem ] || [ ! -f keys/public.pem ]; then
    info "Génération des clés JWT..."
    mkdir -p keys
    openssl genpkey -algorithm RSA -out keys/private.pem -pkeyopt rsa_keygen_bits:2048
    openssl pkey -in keys/private.pem -pubout -out keys/public.pem
    chmod 600 keys/private.pem
fi
log "Clés JWT présentes"

# Vérifier le fichier .env du frontend
if [ ! -f frontend/.env ]; then
    info "Création du .env frontend..."
    echo "VITE_API_URL=
VITE_THEME=dark" > frontend/.env
fi
log "Frontend .env configuré"

# S'assurer que le backend est compilé
if ! ls backend/target/*.jar 1>/dev/null 2>&1; then
    info "Compilation du backend (première fois)..."
    cd backend && mvn compile -q && cd "$PROJECT_DIR"
fi

# ============================================
# Démarrage du backend
# ============================================

info "Démarrage du backend (Spring Boot, profil: default)..."
cd backend
SPRING_PROFILES_ACTIVE=default \
JWT_PRIVATE_KEY_PATH="$PROJECT_DIR/keys/private.pem" \
JWT_PUBLIC_KEY_PATH="$PROJECT_DIR/keys/public.pem" \
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/discipolat \
mvn spring-boot:run -DskipTests -q > "$PROJECT_DIR/backend.log" 2>&1 &
BACKEND_PID=$!
cd "$PROJECT_DIR"

# Attendre que le backend soit prêt
info "Attente du démarrage du backend..."
for i in $(seq 1 30); do
    if curl -s -o /dev/null -w '%{http_code}' http://localhost:8080/actuator/health 2>/dev/null | grep -q 200; then
        log "Backend démarré sur http://localhost:8080"
        break
    fi
    sleep 2
    if [ $i -eq 30 ]; then
        error "Le backend n'a pas démarré dans les délais."
        info "Consultez backend.log pour les détails : $PROJECT_DIR/backend.log"
        exit 1
    fi
done

# ============================================
# Démarrage du frontend
# ============================================

info "Démarrage du frontend (Vite)..."
cd frontend && npm run dev > "$PROJECT_DIR/frontend.log" 2>&1 &
FRONTEND_PID=$!
cd "$PROJECT_DIR"

# Attendre que le frontend soit prêt
sleep 5
if kill -0 $FRONTEND_PID 2>/dev/null; then
    log "Frontend démarré sur http://localhost:5173"
else
    error "Le frontend n'a pas démarré."
    info "Consultez frontend.log pour les détails : $PROJECT_DIR/frontend.log"
    exit 1
fi

# ============================================
# Test de connexion
# ============================================

info "Test de connexion à l'API..."
sleep 2
LOGIN_TEST=$(curl -s -X POST http://localhost:8080/api/v1/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"pasteur@discipolat.com","password":"password123"}' 2>&1)

if echo "$LOGIN_TEST" | grep -q '"accessToken"'; then
    log "Authentification OK — l'API répond et génère des tokens JWT"
    log "Comptes de test disponibles :"
    echo "    Pasteur       → pasteur@discipolat.com / password123"
    echo "    Responsable   → responsable@discipolat.com / password123"
    echo "    Chef famille  → chef@discipolat.com / password123"
    echo "    Faiseur       → faiseur@discipolat.com / password123"
else
    warn "Le test de connexion a échoué : $LOGIN_TEST"
    info "Le backend est peut-être encore en train de démarrer."
fi

# ============================================
# Résumé
# ============================================

echo ""
echo -e "${GREEN}══════════════════════════════════════════════${NC}"
echo -e "${GREEN}  🚀 Discipolat — Environnement de développement${NC}"
echo -e "${GREEN}══════════════════════════════════════════════${NC}"
echo -e ""
echo -e "  Frontend  →  ${BLUE}http://localhost:5173${NC}"
echo -e "  Backend   →  ${BLUE}http://localhost:8080${NC}"
echo -e "  API Docs  →  ${BLUE}http://localhost:8080/swagger-ui.html${NC}"
echo -e "  Actuator  →  ${BLUE}http://localhost:8080/actuator/health${NC}"
echo -e ""
echo -e "  ${YELLOW}Ctrl+C${NC} pour arrêter tous les services"
echo -e ""

# Attendre que les processus se terminent
wait
