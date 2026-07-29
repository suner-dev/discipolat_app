# Sécurité

## Authentification

### JWT (JSON Web Tokens)
- **Algorithme** : RS256 (signature asymétrique RSA)
- **Access Token** : 15 minutes (configurable via `JWT_ACCESS_TOKEN_EXPIRATION`)
- **Refresh Token** : 7 jours (configurable via `JWT_REFRESH_TOKEN_EXPIRATION`)
- **Rotation** : Nouveau refresh token émis à chaque rafraîchissement
- **Stockage** : Les tokens sont stockés côté client (localStorage) avec possibilité cookies HttpOnly

### Génération des clés

```bash
openssl genpkey -algorithm RSA -out keys/private.pem -pkeyopt rsa_keygen_bits:2048
openssl pkey -in keys/private.pem -pubout -out keys/public.pem
```

Les clés sont passées à l'application via les variables d'environnement `JWT_PRIVATE_KEY` et `JWT_PUBLIC_KEY`.

## Autorisation (RBAC)

### Rôles et permissions

| Rôle | Âmes | Familles | Départements | Rapports | Alertes | Dashboard | Utilisateurs |
|---|---|---|---|---|---|---|---|
| PASTEUR | Toutes | Toutes | Tous | Tous | Toutes | Global | Tous |
| RESPONSABLE | Son dépt. | Son dépt. | Son dépt. | Son dépt. | Son dépt. | Son dépt. | Non |
| CHEF (FAISEUR+chief) | Sa famille | Sa famille | Lecture | Sa famille | Sa famille | Sa famille | Non |
| FAISEUR | Ses âmes | Lecture | Lecture | Ses rapports | Ses alertes | Non | Non |

### Scope par données

Chaque requête vérifie non seulement le rôle, mais aussi la propriété des données :
- Un faiseur ne peut accéder qu'à ses propres âmes
- Un chef de famille accède aux âmes de tous les faiseurs de sa famille
- Un responsable accède aux familles de son département uniquement

## Protection

### Rate Limiting
- **Endpoint** : `/auth/login`
- **Limite** : 10 requêtes/minute par IP
- **Implémentation** : Bucket4j

### Headers de sécurité
- **CSP** : Content-Security-Policy configurée
- **HSTS** : Strict-Transport-Security
- **CORS** : Domaines autorisés configurés via `FRONTEND_URL`
- **XSS** : Validation stricte des entrées (Bean Validation)

### Sécurité des mots de passe
- **Hash** : BCrypt avec coût 12
- **Taille minimale** : 8 caractères

### Protection des données
- **Base de données** : PostgreSQL avec utilisateur dédié
- **Communications** : HTTPS/TLS 1.2+ en production
- **Secrets** : Variables d'environnement via GitHub Secrets / AWS Secrets Manager
- **Audit** : Journalisation de toutes les actions sensibles

## Configuration recommandée pour la production

```yaml
app:
  jwt:
    access-token-expiration-minutes: 15
    refresh-token-expiration-days: 7
  cors:
    allowed-origins: https://votre-domaine.com
server:
  ssl:
    enabled: true
    key-store: /etc/ssl/keystore.p12
```
