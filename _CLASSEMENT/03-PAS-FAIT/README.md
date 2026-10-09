# 03 — PAS FAIT (4 fichiers)

Fichiers décrivant des fonctionnalités **non implémentées** dans le code.

## Liste des fichiers

| Fichier | Justification |
|---------|---------------|
| `docs/PLAN_FUSION.md` | Plan de fusion de branches (obsolète). Les branches mentionnées ont été fusionnées autrement. Aucune fonctionnalité à implémenter — document de coordination git. |
| `docs/architecture/ADR-001-separer-les-repos-et-gitlab.md` | Découpage repos/backend frontend/mobile + migration GitLab = proposition non implémentée. Monorepo actuel, pas de GitLab CI, render.yaml reste sur GitHub. |
| `docs/architecture/frontend-target-architecture.md` | Tranches verticales, contrat-first OpenAPI généré, ESLint frontières, design system unifié, taxonomie états 4 cas, i18n lazy/ICU = cible non implémentée. Frontend actuel: pages à plat, 259 `any`, 501 clés cache littérales, 784Ko i18n au boot. |
| `docs/architecture/PREPARATION-DUE-DILIGENCE.md` | 5 travaux prioritaires (Testcontainers PG réel, alignement ddl-auto, JaCoCo, README professionnel, zéro TODO) = plan non exécuté. 189 migrations sans tests sur vrai SGBD, pas de JaCoCo, README 596 octets, pas de LICENSE, 16 fichiers md à racine. |
