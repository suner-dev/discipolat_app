#!/usr/bin/env bash
# ============================================
# Discipolat — Helper de déploiement Render
# ============================================
# Usage :
#   ./scripts/deploy-setup.sh            → mode interactif
#   ./scripts/deploy-setup.sh --check    → vérifie les prérequis seulement
#   ./scripts/deploy-setup.sh --keys     → génère les clés JWT (base64)
#   ./scripts/deploy-setup.sh --env      → génère le fichier .env.example
#
# Ce script :
#   - Vérifie les prérequis (openssl, git, jq)
#   - Génère les clés JWT RSA 2048 (format base64 pour Render)
#   - Affiche la liste des secrets à configurer dans Render
#   - Génère un .env.example pour référence locale

set -euo pipefail

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

info()  { echo -e "${BLUE}[INFO]${NC}  $1"; }
ok()    { echo -e "${GREEN}[OK]${NC}    $1"; }
warn()  { echo -e "${YELLOW}[WARN]${NC}  $1"; }
error() { echo -e "${RED}[ERROR]${NC} $1"; }

# ============================================
# Prérequis
# ============================================

check_prereqs() {
    local all_ok=true

    info "🔍 Vérification des prérequis..."

    if command -v openssl &>/dev/null; then
        ok "openssl : $(openssl version | awk '{print $1, $2}')"
    else
        error "openssl : non installé (apt install openssl)"
        all_ok=false
    fi

    if command -v git &>/dev/null; then
        ok "git     : $(git --version | awk '{print $3}')"
    else
        error "git     : non installé"
        all_ok=false
    fi

    if command -v jq &>/dev/null; then
        ok "jq      : $(jq --version)"
    else
        warn "jq      : non installé (optionnel, pour les API Render)"
    fi

    # Vérifier les fichiers de config essentiels
    local required_files=(
        "render.yaml"
        "docker-compose.yml"
        "backend/Dockerfile"
        "frontend/Dockerfile"
        "frontend/nginx.conf"
        ".github/workflows/ci.yml"
    )

    for f in "${required_files[@]}"; do
        if [[ -f "$f" ]]; then
            ok "fichier : $f"
        else
            error "fichier : $f — MANQUANT"
            all_ok=false
        fi
    done

    echo ""
    if $all_ok; then
        ok "✅ Tous les prérequis sont satisfaits."
        return 0
    else
        error "❌ Certains prérequis sont manquants."
        return 1
    fi
}

# ============================================
# Clés JWT
# ============================================

generate_keys() {
    local keys_dir="keys"

    info "🔑 Génération des clés JWT RSA 2048..."

    mkdir -p "$keys_dir"

    if [[ -f "$keys_dir/private.pem" ]]; then
        warn "Des clés existent déjà dans $keys_dir/"
        read -rp "Les écraser ? (o/N) " confirm
        if [[ "$confirm" != "o" && "$confirm" != "O" ]]; then
            info "Génération annulée."
            return
        fi
    fi

    openssl genpkey -algorithm RSA -out "$keys_dir/private.pem" -pkeyopt rsa_keygen_bits:2048
    openssl pkey -in "$keys_dir/private.pem" -pubout -out "$keys_dir/public.pem"

    # En-têtes compatibles Java / Spring Security
    # (Au cas où on utilise les fichiers .pem directement)
    chmod 600 "$keys_dir/private.pem"
    chmod 644 "$keys_dir/public.pem"

    ok "Clés générées :"
    echo ""
    echo "   📄 $keys_dir/private.pem"
    echo "   📄 $keys_dir/public.pem"
    echo ""

    # Format base64 (pour Render)
    local private_b64
    local public_b64
    private_b64=$(cat "$keys_dir/private.pem" | base64 -w0)
    public_b64=$(cat "$keys_dir/public.pem" | base64 -w0)

    echo "   ┌─────────────────────────────────────────────┐"
    echo "   │  Copiez ces valeurs dans Render Dashboard   │"
    echo "   └─────────────────────────────────────────────┘"
    echo ""
    echo "   🔐 JWT_PRIVATE_KEY (secret) :"
    echo "   ${private_b64:0:60}..."
    echo ""
    echo "   🔐 JWT_PUBLIC_KEY (secret)  :"
    echo "   ${public_b64:0:60}..."
    echo ""
    warn "Conservez ces clés en lieu sûr !"
}

# ============================================
# Génération .env.example
# ============================================

generate_env_template() {
    local env_file=".env.example"

    info "📝 Génération de $env_file..."

    cat > "$env_file" << 'ENVEOF'
# ============================================
# Discipolat — Variables d'environnement
# ============================================
# Copier ce fichier vers .env et remplir les valeurs
#   cp .env.example .env
#
# ⚠️ Les valeurs marquées [SECRET] doivent être protégées
#    Ne JAMAIS les commit dans Git.
# ============================================

# --- Base de données ---
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/discipolat
SPRING_DATASOURCE_USERNAME=discipolat
SPRING_DATASOURCE_PASSWORD=discipolat_secret              # [SECRET]

# --- JWT (format base64) ---
JWT_PRIVATE_KEY=                                           # [SECRET] openssl base64 -w0 keys/private.pem
JWT_PUBLIC_KEY=                                            # [SECRET] openssl base64 -w0 keys/public.pem
# Alternative : chemins vers les fichiers .pem (local)
JWT_PRIVATE_KEY_PATH=keys/private.pem
JWT_PUBLIC_KEY_PATH=keys/public.pem

# --- CORS & URLs ---
FRONTEND_URL=http://localhost:3000,http://localhost:5173
FRONTEND_URL_BASE=http://localhost:5173
VITE_API_URL=http://localhost:8080

# --- SMTP (Mail) ---
MAIL_HOST=localhost
MAIL_PORT=1025
MAIL_USERNAME=noreply@discipolat.com
MAIL_PASSWORD=                                              # [SECRET]

# --- Spring ---
SPRING_PROFILES_ACTIVE=dev                                  # dev | docker | prod | test
SERVER_PORT=8080

ENVEOF

    ok "$env_file créé avec succès."
}

# ============================================
# Aide Render
# ============================================

show_render_guide() {
    echo ""
    echo "┌─────────────────────────────────────────────────────────────┐"
    echo "│         🚀  Déploiement Render — Instructions              │"
    echo "└─────────────────────────────────────────────────────────────┘"
    echo ""
    echo "Étape 1 — Connecter le dépôt GitHub à Render"
    echo "  1. Aller sur https://dashboard.render.com/"
    echo "  2. Cliquer « New + » → « Blueprint »"
    echo "  3. Sélectionner le dépôt suner-dev/discipolat_app"
    echo "  4. Render détecte automatiquement render.yaml"
    echo ""
    echo "Étape 2 — Configurer les secrets Render"
    echo "  Dans le Dashboard Render, ajouter ces SECRETS :"
    echo ""
    echo "  🔐 JWT_PRIVATE_KEY     → clé privée RSA (base64)"
    echo "  🔐 JWT_PUBLIC_KEY      → clé publique RSA (base64)"
    echo "  🔐 MAIL_PASSWORD       → mot de passe SMTP"
    echo ""
    echo "Étape 3 — Configurer les variables non-secrètes Render"
    echo "  MAIL_HOST              → smtp.mailgun.org (ou autre)"
    echo "  MAIL_PORT              → 587"
    echo "  MAIL_USERNAME          → votre compte SMTP"
    echo "  FRONTEND_URL           → https://discipolat.onrender.com"
    echo "  FRONTEND_URL_BASE      → https://discipolat.onrender.com"
    echo ""
    echo "Étape 4 — Configurer les secrets GitHub (CI/CD)"
    echo "  Dans GitHub → Settings → Secrets and variables → Actions :"
    echo ""
    echo "  🔐 RENDER_API_KEY         → clé API Render"
    echo "  🔐 RENDER_API_SERVICE_ID  → ID du service API"
    echo ""
    echo "  Pour obtenir RENDER_API_KEY :"
    echo "    Dashboard Render → Account Settings → API Keys"
    echo ""
    echo "Étape 5 — Vérifier le déploiement"
    echo "  API  : https://discipolat-api.onrender.com/actuator/health"
    echo "  Web  : https://discipolat.onrender.com/"
    echo "  Docs : https://discipolat-api.onrender.com/swagger-ui.html"
    echo ""
    echo "Étape 6 — Pousser sur main pour déclencher le CI/CD"
    echo "  git push origin main"
    echo ""
}

# ============================================
# Main
# ============================================

show_banner() {
    echo ""
    echo " ╔══════════════════════════════════════╗"
    echo " ║      Discipolat — Deploy Helper      ║"
    echo " ║      Version 2.0.0 - Juillet 2026    ║"
    echo " ╚══════════════════════════════════════╝"
    echo ""
}

main() {
    show_banner

    case "${1:-}" in
        --check|-c)
            check_prereqs
            exit $?
            ;;
        --keys|-k)
            check_prereqs || true
            generate_keys
            exit 0
            ;;
        --env|-e)
            generate_env_template
            exit 0
            ;;
        --help|-h)
            echo "Usage: $0 [--check|--keys|--env|--help]"
            echo ""
            echo "  --check       Vérifier les prérequis seulement"
            echo "  --keys        Générer les clés JWT (format base64)"
            echo "  --env         Générer le fichier .env.example"
            echo "  --help        Afficher cette aide"
            exit 0
            ;;
        "")
            # Mode interactif
            check_prereqs || true
            echo ""
            generate_keys
            echo ""
            generate_env_template
            echo ""
            show_render_guide
            echo ""
            ok "✅ Script terminé."
            echo ""
            info "Prochaine étape : ouvrir https://dashboard.render.com/ et suivre les instructions ci-dessus."
            echo ""
            ;;
        *)
            error "Option inconnue : $1"
            echo "Usage: $0 [--check|--keys|--env|--help]"
            exit 1
            ;;
    esac
}

main "$@"
