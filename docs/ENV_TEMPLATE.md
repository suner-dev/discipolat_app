# 🌐 Variables d'environnement — Discipolat

## Référence complète pour le déploiement Render

### Structure

```
                                Variables d'environnement
    ┌─────────────────────────────┬──────────────────────────────┬──────────────────────────┐
    │       Backend (Java)        │      Frontend (Node/React)    │       Infrastructure     │
    │   SPRING_*, JWT_*, MAIL_*   │        VITE_API_URL          │   RENDER_API_* (GitHub)   │
    └─────────────────────────────┴──────────────────────────────┴──────────────────────────┘
```

---

## 1. Backend — Spring Boot

### 🔐 Secrets (à définir dans Render Dashboard)

| Variable | Description | Commande de génération | Secret |
|----------|-------------|----------------------|--------|
| `JWT_PRIVATE_KEY` | Clé privée RSA 2048 bits (base64) | `cat keys/private.pem \| base64 -w0` | ✅ |
| `JWT_PUBLIC_KEY` | Clé publique RSA 2048 bits (base64) | `cat keys/public.pem \| base64 -w0` | ✅ |
| `MAIL_PASSWORD` | Mot de passe du serveur SMTP | — | ✅ |
| `SPRING_DATASOURCE_PASSWORD` | Mot de passe PostgreSQL | Généré automatiquement par Render | ✅ |

### 🔓 Variables non-secrètes

| Variable | Valeur Render | Valeur locale (docker-compose) |
|----------|--------------|-------------------------------|
| `SPRING_PROFILES_ACTIVE` | `prod` | `docker` |
| `SPRING_DATASOURCE_URL` | Généré automatiquement (Render DB) | `jdbc:postgresql://db:5432/discipolat` |
| `SPRING_DATASOURCE_USERNAME` | Généré automatiquement (Render DB) | `discipolat` |
| `MAIL_HOST` | `smtp.mailgun.org` (ou autre) | `mailhog` |
| `MAIL_PORT` | `587` | `1025` |
| `MAIL_USERNAME` | À renseigner | `noreply@discipolat.com` |
| `FRONTEND_URL` | `https://discipolat.onrender.com` | `http://localhost:3000,http://localhost:5173` |
| `FRONTEND_URL_BASE` | `https://discipolat.onrender.com` | `http://localhost:5173` |
| `SERVER_PORT` | `10000` (port Render) | `8080` |

---

## 2. Frontend — React / Vite

### 🔓 Variables

| Variable | Valeur Render | Valeur locale | Description |
|----------|--------------|---------------|-------------|
| `VITE_API_URL` | `https://discipolat-api.onrender.com` | `http://localhost:8080` | URL de base de l'API backend |

---

## 3. Infrastructure (GitHub Secrets uniquement)

| Variable | Valeur | Description |
|----------|--------|-------------|
| `RENDER_API_KEY` | Clé API Render (GitHub Secret) | Obtenir via Dashboard Render → Account Settings |
| `RENDER_API_SERVICE_ID` | ID du service API sur Render (GitHub Secret) | Obtenir via Dashboard ou API |

> **Note :** `INTERNAL_API_KEY` et `RENDER_WEB_SERVICE_ID` ne sont plus nécessaires
> depuis 2.1.3 (cron jobs Render supprimés — scheduler Spring interne — et static site
> en auto-deploy).

---

## 4. Procédure de configuration

### Étape 1 : Générer les clés JWT

```bash
# Se placer à la racine du projet
cd /chemin/vers/discipolat

# Avec le script helper
bash scripts/deploy-setup.sh --keys

# Ou manuellement
mkdir -p keys
openssl genpkey -algorithm RSA -out keys/private.pem -pkeyopt rsa_keygen_bits:2048
openssl pkey -in keys/private.pem -pubout -out keys/public.pem

# Encoder en base64 (pour coller dans Render)
JWT_PRIVATE_KEY=$(cat keys/private.pem | base64 -w0)
JWT_PUBLIC_KEY=$(cat keys/public.pem | base64 -w0)

echo "JWT_PRIVATE_KEY : $JWT_PRIVATE_KEY"
echo "JWT_PUBLIC_KEY  : $JWT_PUBLIC_KEY"
```

### Étape 2 : Connecter le dépôt à Render

1. Aller sur https://dashboard.render.com/
2. **New + → Blueprint**
3. Sélectionner `suner-dev/discipolat_app`
4. Render détecte automatiquement `render.yaml`

### Étape 3 : Définir les secrets dans Render Dashboard

| Service | Secrets à définir |
|---------|------------------|
| `discipolat-api` | `JWT_PRIVATE_KEY`, `JWT_PUBLIC_KEY`, `MAIL_PASSWORD` |

### Étape 4 : Configurer les GitHub Secrets (CI/CD)

Dans GitHub → Settings → Secrets and variables → Actions :

| Secret | Valeur |
|--------|--------|
| `RENDER_API_KEY` | Clé API générée dans Render |
| `RENDER_API_SERVICE_ID` | ID du service discipolat-api |

#### Obtenir les Service IDs

```bash
# Avec une clé API Render
RENDER_API_KEY="votre_clé_api"

curl -s -H "Authorization: Bearer $RENDER_API_KEY" \
  https://api.render.com/v1/services | \
  jq '.[] | {name: .service.name, id: .service.id}'
```

### Étape 5 : Pousser sur `main`

```bash
git push origin main
```

Le pipeline CI/CD va :
1. 🧪 Compiler et tester le backend
2. 🎨 Compiler et tester le frontend
3. 🐳 Builder et pousser les images Docker sur GHCR
4. 🚀 Déclencher le déploiement Render

---

## 5. Vérification du déploiement

```bash
# Health check API
curl -s https://discipolat-api.onrender.com/actuator/health | jq .

# Réponse attendue :
# {
#   "status": "UP",
#   "components": {
#     "db": { "status": "UP" },
#     "ping": { "status": "UP" }
#   }
# }

# Vérifier la page d'accueil
curl -s -o /dev/null -w "%{http_code}" https://discipolat.onrender.com/
# Réponse attendue : 200
```

---

## 6. Dépannage

| Symptôme | Cause probable | Solution |
|----------|---------------|----------|
| `401 Unauthorized` | Clés JWT invalides | Vérifier les secrets `JWT_PRIVATE_KEY`/`JWT_PUBLIC_KEY` |
| `Connection refused` à la DB | URL de connexion erronée | Render génère automatiquement `SPRING_DATASOURCE_URL` |
| Build Docker échoue | Cache périmé | Ajouter `"clearCache": "clear"` au body du deploy |
| CORS bloque les requêtes | `FRONTEND_URL` incorrect | Vérifier l'URL exacte du frontend Render |
| Tâches planifiées inactives | Scheduler Spring arrêté | Vérifier les logs API + keep-alive actif |
| Page blanche (frontend) | `VITE_API_URL` incorrect | Vérifier la variable d'env du service Web |
| Flyway échoue | Migration incompatible | Vider `flyway_schema_history` (⚠️ perte de données) |
