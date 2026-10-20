#!/usr/bin/env python3
"""
Génère ``backend/src/test/resources/import.sql`` — le seed H2 des tests
d'intégration de la PARAMÉTRISATION PAR ÉGLISE.

Pourquoi ce fichier est dans le dépôt
-------------------------------------
Le profil de test tourne sur H2 avec ``ddl-auto: create-drop`` et **Flyway
coupé**. Depuis la phase 3, les gardes HTTP passent par ``@authz.can('CLÉ')``,
résolu par la matrice rôle→permission : sans les lignes ``roles`` /
``permissions`` / ``role_permissions``, le pont d'équivalence ne résout rien et
les tests d'intégration tombent à 403 sur des cas légitimes.

``import.sql`` est donc un **artefact généré** : sa vérité, ce sont les
migrations Flyway. Il est versionné (les tests en ont besoin), mais il ne doit
JAMAIS être édité à la main — la dérive est bloquée côté Java par
``TestPermissionMatrixSeedGuardTest``, qui compare les paires effectives du seed
à celles des migrations.

Usage (depuis la racine du dépôt)::

    python3 tools/parametrisation/gen_h2_seed.py           # écrit le fichier
    python3 tools/parametrisation/gen_h2_seed.py --check   # verifie sans ecrire

``--check`` sort en code 1 si le seed committé diffère de ce que produiraient
les migrations : c'est le contrôle à lancer dans une revue plutôt que de
comparer 1 400 paires à l'œil.

Après TOUTE migration qui touche la matrice ou le catalogue : régénérer, puis
rejouer ``TestPermissionMatrixSeedGuardTest`` **et** la chaîne Flyway
``FlywayMigrationChainPostgreSqlTest`` (la suite H2 n'exécute jamais le SQL des
migrations, elle ne rejoue que ce seed).

Limite connue : les tuples sont lus par analyse de parenthèses, donc un libellé
contenant une apostrophe SQL échappée (``''``) est géré — mais il faut éviter
l'apostrophe dans les libellés de ce type de bloc, que le verrou Java
(`TestPermissionMatrixSeedGuardTest`) analyse encore par regex.
"""
from __future__ import annotations

import re
import sys
from pathlib import Path

# Racine du dépôt déduite de l'emplacement du script : le générateur reste
# exécutable depuis n'importe quel machine et n'importe quel répertoire.
REPO = Path(__file__).resolve().parents[2]
MIG = REPO / "backend/src/main/resources/db/migration"
OUT = REPO / "backend/src/test/resources/import.sql"

# Migrations qui portent le catalogue servi + la matrice par défaut.
# À ÉTENDRE à chaque nouvelle migration de permissions : c'est l'oubli le plus
# courant, il se voit immédiatement (le seed devient plus pauvre que la vérité).
#
# V135/V136 (catalogue historique `MEMBER_READ`, `ROLE_*`, `BRANDING_*`…) en sont
# VOLONTAIREMENT absents, pour deux raisons : (1) le verrou Java
# `TestPermissionMatrixSeedGuardTest` borne sa comparaison à cette liste — l'y
# ajouter ici rendrait le seed plus riche que sa vérité de référence et le
# test tomberait ; (2) aucune garde `@authz.can` convertie n'utilise ces clés,
# donc le seed n'en a pas besoin. Les clients, eux, lisent bien V135+V136 pour
# le catalogue servi (cf. `parametrisationGate.test.ts`).
FILES = [
    "V258__parametrisation_permissions_par_eglise.sql",
    "V259__parametrisation_permissions_suite_phase3.sql",
    "V260__parametrisation_admin_events_read.sql",
    "V261__parametrisation_cles_ui_routes.sql",
    "V262__parametrisation_cles_ui_intra_pages.sql",
    "V270__parametrisation_cles_ui_residuel_dashboard.sql",
    "V271__visibilite_archives_et_assistants.sql",
    # V272 : pas de permission nouvelle, mais deux paires de plus dans la
    # matrice (UI_ARCHIVES_ACCESS → DEPARTMENT_LEADER/FAMILY_LEADER).
    "V272__archives_parcours_complet.sql",
]

# Rôles cités par la matrice sans ligne de création dans V258/V259 : en
# production ils existent DÉJÀ (V135 §12 / V154 §6, tenant_id NULL) — mêmes
# clés, labels et priorités que la baseline, pour que le seed H2 reproduise
# l'équivalence exacte des paires « mortes » filtrées par le JOIN.
BASELINE_ROLES = {
    "PLATFORM_SUPER_ADMIN": ("Super Admin Plateforme", 1000),  # V135
    "TENANT_OWNER": ("Proprietaire Tenant", 900),              # V135
    "TENANT_ADMIN": ("Admin Tenant", 800),                    # V135
    "CHURCH_ADMIN": ("Admin Eglise", 700),                    # V135
    "CHURCH_LEADER": ("Leader Eglise", 600),                  # V135
    "DEPARTMENT_ADMIN": ("Admin Departement", 500),           # V135
    "DEPARTMENT_LEADER": ("Leader Departement", 400),         # V135
    "FAMILY_LEADER": ("Chef de Famille", 300),                # V135/V154
    "DISCIPLE_MAKER": ("Faiseur de Disciples", 200),          # V135
    "MEMBER": ("Membre", 100),                                # V135
    "GUEST": ("Invite", 0),                                   # V135
    "HEALTH_STAFF": ("Personnel Sante", 250),                 # V154
    "HEALTH_LEAD": ("Responsable Sante", 300),                # V154
}

pair_re = re.compile(r"\(\s*'([A-Z0-9_]+)'\s*,\s*'([A-Z0-9_]+)'\s*\)")
role3_re = re.compile(r"\(\s*'([A-Z0-9_]+)'\s*,\s*'([^']+)'\s*,\s*(\d+)\s*\)")
perm3_re = re.compile(r"\(\s*'([A-Z0-9_]+)'\s*,\s*'([^']+)'\s*,\s*'([A-Z0-9_]+)'\s*\)")

roles: dict[str, tuple[str, str]] = {}   # key -> (label, priority)
perms: dict[str, tuple[str, str]] = {}   # key -> (label, category)
pairs: set[tuple[str, str]] = set()      # (role_key, perm_key)


def main() -> int:
    missing = [f for f in FILES if not (MIG / f).is_file()]
    if missing:
        print(f"migration absente : {missing}\n"
              f"liste FILES à mettre à jour ?", file=sys.stderr)
        return 2

    for vf in FILES:
        txt = (MIG / vf).read_text(encoding="utf-8")
        for sec in re.findall(r"INSERT INTO roles\b.*?;", txt, re.S):
            for m in role3_re.findall(sec):
                roles.setdefault(m[0], (m[1], m[2]))
        for sec in re.findall(r"INSERT INTO permissions\b.*?;", txt, re.S):
            for m in perm3_re.findall(sec):
                perms.setdefault(m[0], (m[1], m[2]))
        for sec in re.findall(r"INSERT INTO role_permissions\b.*?;", txt, re.S):
            for role, perm in pair_re.findall(sec):
                pairs.add((role, perm))

    for role, _ in list(pairs):
        if role not in roles:
            roles[role] = BASELINE_ROLES.get(role, (role, "500"))

    # paires dont la permission est inconnue du catalogue = bruit de parsing.
    # (`effective_pairs` et non `pairs` : réaffecter `pairs` ici en ferait une
    # variable locale de `main()` pour toute la fonction, y compris les lignes
    # du dessus — cf. UnboundLocalError.)
    effective_pairs = {(r, p) for (r, p) in pairs if p in perms}

    def vals(items):
        return ", ".join(items)

    lines = [
        "-- PARAMÉTRISATION PAR ÉGLISE — seed H2 des tests d'intégration.",
        "-- Généré par tools/parametrisation/gen_h2_seed.py depuis "
        + "+".join(f.split("__")[0] for f in FILES)
        + " (vérité = migrations).",
        "-- Exécuté par Hibernate après ddl-auto create-drop (profil test, Flyway coupé).",
        "-- Le pont d'équivalence @authz.can a besoin des lignes globales",
        "-- roles/permissions/role_permissions ; la dérive est bloquée par",
        "-- TestPermissionMatrixSeedGuardTest. NE PAS éditer à la main.",
        "",
        "ALTER TABLE event_publication ALTER COLUMN serialized_event TYPE TEXT;",
        "",
    ]
    role_tuples = [f"('{k}', '{lab}', {pri})" for k, (lab, pri) in sorted(roles.items())]
    lines.append(
        "INSERT INTO roles (id, tenant_id, key, label, description, system, priority, created_at, updated_at) "
        "SELECT RANDOM_UUID(), NULL, k.key, k.label, k.label, TRUE, k.priority, "
        "CURRENT_TIMESTAMP, CURRENT_TIMESTAMP FROM (VALUES " + vals(role_tuples) + ") AS k(key, label, priority) "
        "WHERE NOT EXISTS (SELECT 1 FROM roles ro WHERE ro.tenant_id IS NULL AND ro.key = k.key);")
    lines.append("")
    perm_tuples = [f"('{k}', '{lab}', '{cat}')" for k, (lab, cat) in sorted(perms.items())]
    lines.append(
        "INSERT INTO permissions (id, tenant_id, key, label, description, scope, category, system, "
        "created_at, updated_at) SELECT RANDOM_UUID(), NULL, p.key, p.label, p.label, 'TENANT', p.category, "
        "TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP FROM (VALUES " + vals(perm_tuples) + ") AS p(key, label, category) "
        "WHERE NOT EXISTS (SELECT 1 FROM permissions po WHERE po.key = p.key);")
    lines.append("")
    pair_tuples = [f"('{r}', '{p}')" for r, p in sorted(effective_pairs)]
    lines.append(
        "INSERT INTO role_permissions (role_id, permission_id) "
        "SELECT r.id, p.id FROM (VALUES " + vals(pair_tuples) + ") AS m(role_key, perm_key) "
        "JOIN roles r ON r.key = m.role_key AND r.tenant_id IS NULL "
        "JOIN permissions p ON p.key = m.perm_key "
        "WHERE NOT EXISTS (SELECT 1 FROM role_permissions rp "
        "WHERE rp.role_id = r.id AND rp.permission_id = p.id);")

    content = "\n".join(lines) + "\n"
    print(f"roles={len(roles)} permissions={len(perms)} paires={len(effective_pairs)}")

    if "--check" in sys.argv:
        if not OUT.is_file():
            print(f"{OUT} absent", file=sys.stderr)
            return 1
        current = OUT.read_text(encoding="utf-8")
        if current == content:
            print("OK : le seed committé correspond aux migrations")
            return 0
        print(f"DIVERGENCE : {OUT} ne correspond plus aux migrations\n"
              f"  lancer : python3 tools/parametrisation/gen_h2_seed.py", file=sys.stderr)
        return 1

    OUT.write_text(content, encoding="utf-8")
    print(f"-> {OUT}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())