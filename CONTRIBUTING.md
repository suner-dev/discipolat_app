# Contribuer à Discipolat

> Point d'entrée rapide. Le détail par pile vit dans
> [docs/CONTRIBUTING.md](docs/CONTRIBUTING.md) et le déploiement dans
> [docs/DEPLOYMENT.md](docs/DEPLOYMENT.md).

## Prérequis (versions exactes du projet)

| Outil | Version | Vérifié par |
|---|---|---|
| Java | **21** (Temurin) | `backend/pom.xml` → `java.version` |
| Node.js | **22 LTS** | `render.yaml` → `NODE_VERSION` |
| Flutter | **≥ 3.35.6** | `mobile/pubspec.yaml` |
| Docker | 24+ (Compose v2) | `docker-compose.yml` |
| Maven | 3.9 | `mvn -v` |

> ⚠️ Le projet compile en **Java 21** (class file major 65). Les versions
> supérieures du JDK peuvent faire échouer l'instrumentation Byte Buddy/Mockito
> dans certains tests — utiliser un JDK 21 pour `mvn test`.

## Démarrage rapide

```bash
cp .env.example .env            # renseigner au minimum ENCRYPTION_AES_KEY
bash setup-keys.sh              # génère les clés JWT RSA (gitignorées)
docker compose up -d --build    # db, redis, api, web, nginx, mailhog
```

Web : http://localhost:3000 · API : http://localhost:8081 (`/swagger-ui.html`)

## Lancer la qualité (les 3 couches) — time-to-green

```bash
# Backend (schéma de production prouvé via Testcontainers PostgreSQL)
cd backend && mvn -o verify

# Frontend
cd frontend && npx tsc -b && npx vitest run && npm run lint

# Mobile
cd mobile && flutter analyze && flutter test
```

## Règles de contribution

- **Jamais de suppression** d'une fonctionnalité existante sans remplaçant testé
  (voir [AGENT_ORCHESTRATION.md](AGENT_ORCHESTRATION.md) — règles A1→A8).
- **Migrations Flyway** numérotées en fin de chaîne, jamais de `ddl-auto` en
  production (schéma piloté par migrations uniquement).
- **Preuve par exécution** : chaque changement s'accompagne d'un test qui échoue
  sans lui.
- **Sécurité** : le tenant est toujours résolu depuis le JWT, jamais depuis le
  client. Voir [SECURITY.md](SECURITY.md).
- **Défauts connus** : les consigner dans [KNOWN_ISSUES.md](KNOWN_ISSUES.md).

## Convention de commits

Format : `<type>(<scope>): <sujet>` — types : `feat`, `fix`, `docs`, `test`,
`refactor`, `build`, `chore`. Portée : `backend`, `frontend`, `mobile`, `infra`,
`docs`. Exemple : `feat(backend): ajout du provider de paiement SEPA`.

## Avant d'ouvrir une Pull Request

- [ ] Les 3 piles sont vertes (commandes ci-dessus).
- [ ] Aucun secret commité (gitleaks passe en CI).
- [ ] `KNOWN_ISSUES.md` mis à jour si un défaut est découvert/corrigé.
