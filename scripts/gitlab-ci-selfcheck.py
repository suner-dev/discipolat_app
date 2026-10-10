#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
gitlab-ci-selfcheck.py — la pipeline GitLab s'auto-valide avant son premier run.

POURQUOI CE SCRIPT EXISTE
-------------------------
Le pipeline GitLab n'a jamais tourné (aucun projet, aucun jeton). Relire le YAML à l'œil ne
détecte que ce que l'œil cherche : trois défauts **bloquants** ont survécu à plusieurs relectures
et ne se voient qu'en appliquant la sémantique GitLab au fichier :

  1. `needs:` vers un job que les `rules:` n'ont pas ajouté à la pipeline. La référence YAML est
     explicite : « If a needed job has optional: false, but it was not added to the pipeline, the
     pipeline fails to create ». Un push sur un tag = pipeline refusée, sans message dans le dépôt.
  2. `rules: when: manual` sans `allow_failure: true`. Référence YAML, rules:when:manual :
     « allow_failure becomes **false** by default » — l'inverse de `when: manual` hors rules.
     Le job est donc **bloquant** : la pipeline reste `blocked`, les stages suivants ne partent
     pas, et `only_allow_merge_if_pipeline_succeeds` (posé par `scripts/gitlab-init-project.sh
     finalize`) rend toute merge impossible.
  3. un `image:` qui n'embarque pas le binaire que son `script:` appelle. Mesurable : l'image
     Maven n'a ni node ni npm (vérifié en conteneur), donc le SBOM frontend y échouait.

CE QUE LE SCRIPT VÉRIFIE
------------------------
S1 stages résolus · S2 `needs` ciblent des jobs réels · S3 `needs` dans un stage égal ou antérieur
· S4 `needs` satisfait dans CHAQUE contexte de pipeline (push main, push develop, MR, tag) ·
S5 aucun job manuel bloquant · S6 images et services épinglés (pas de `latest`, pas d'absence
de tag) · S7 chaque binaire appelé est présent dans l'image ou installé par le job ·
S8 chaque chemin cité (rules:changes, scripts/, performance-tests/) existe réellement ·
S9 pour une image qui embarque son outillage À UNE VERSION FIXE, le tag et la version épinglée par
le dépôt concordent : deux sources de vérité, donc un contrôle machine (le YAML, lui, ne dit rien).

HORS LIGNE par défaut : les images sont seulement listées, pas interrogées. `--registry` ajoute
la preuve d'existence auprès des registres (Docker Hub / GHCR / MCR), en jeton anonyme uniquement.

DÉPENDANCE : PyYAML (la même que §8 d'ADR-008 utilise déjà pour relire ce fichier). Absente ->
sortie 3 nommée : le script ne remplace pas un parseur YAML par du grep, il refuserait de lire.

SORTIES
-------
0  aucune anomalie   1  anomalie(s) rouge(s)   2  fichier absent ou YAML illisible
3  dépendance absente (PyYAML)
"""

import argparse
import json
import os
import re
import sys
import urllib.error
import urllib.request

try:
    import yaml
except ImportError:
    sys.stderr.write("dépendance absente : PyYAML (python3 -m pip install pyyaml, "
                     "ou apt install python3-yaml)\n")
    sys.exit(3)

CONTEXTES = {
    "push main": {"CI_COMMIT_BRANCH": "main", "CI_PIPELINE_SOURCE": "push", "CI_COMMIT_TAG": ""},
    "push develop": {"CI_COMMIT_BRANCH": "develop", "CI_PIPELINE_SOURCE": "push", "CI_COMMIT_TAG": ""},
    "merge request": {"CI_COMMIT_BRANCH": "", "CI_PIPELINE_SOURCE": "merge_request_event", "CI_COMMIT_TAG": ""},
    "tag": {"CI_COMMIT_BRANCH": "", "CI_PIPELINE_SOURCE": "tag", "CI_COMMIT_TAG": "v1.0.0"},
}

# ------------------------------------------------------------------ table d'outillage MESURÉE
# Revérification (conteneurs locaux, 2026-10-10) :
#   docker run --rm --entrypoint sh <IMAGE> -c 'for t in mvn node npm npx python3 git curl \
#     openssl find xargs awk apt-get apk docker; do command -v $t >/dev/null && printf "%s " $t; done'
OUTILS_PAR_IMAGE = {
    "maven:3.9-eclipse-temurin-21": {"mvn", "git", "curl", "openssl", "find", "xargs", "awk", "apt-get"},
    "ubuntu:24.04": {"find", "xargs", "awk", "apt-get"},
    "node:23": {"node", "npm", "npx", "python3", "git", "curl", "openssl", "find", "xargs", "awk", "apt-get"},
    "python:3.12-slim": {"python3", "pip", "openssl", "find", "xargs", "awk", "apt-get"},
    "docker:27": {"git", "openssl", "find", "xargs", "awk", "apk", "docker"},
    "curlimages/curl:8.13.0": {"curl", "find", "xargs", "awk", "apk"},
    "aquasec/trivy:0.75.0": {"trivy", "git", "find", "xargs", "awk", "apk"},
}

# Nom de paquet -> binaire (pour « ce que le job installe lui-même »)
PAQUETS = {
    "maven": "mvn", "openjdk21": "java", "nodejs": "node", "npm": "npm", "python3": "python3",
    "python3-yaml": "python3", "git": "git", "curl": "curl", "openssl": "openssl", "k6": "k6",
    "bandit": "bandit", "coreutils": "sort", "ca-certificates": None, "gnupg": "gpg",
    "jq": "jq", "trivy": "trivy",
}

BINAIRES = ["mvn", "npm", "npx", "node", "python3", "python", "git", "curl", "openssl",
            "trivy", "k6", "flutter", "docker", "bandit", "jq"]


class Anomalie(object):
    def __init__(self, niveau, code, job, message):
        self.niveau = niveau      # "ROUGE" | "info"
        self.code = code
        self.job = job
        self.message = message

    def __str__(self):
        qui = (" (%s)" % self.job) if self.job else ""
        return "[%s] %s%s : %s" % (self.niveau, self.code, qui, self.message)


# ------------------------------------------------------------------------------------ lecture
def charger(chemin):
    if not os.path.isfile(chemin):
        sys.stderr.write("fichier absent : %s\n" % chemin)
        sys.exit(2)
    with open(chemin, "r") as f:
        texte = f.read()
    try:
        d = yaml.safe_load(texte)
    except yaml.YAMLError as e:
        sys.stderr.write("YAML illisible : %s\n" % e)
        sys.exit(2)
    if not isinstance(d, dict):
        sys.stderr.write("YAML lisible mais pas un mapping en racine\n")
        sys.exit(2)
    return d


def jobs_de(d):
    """Un job = clé de premier niveau dont la valeur est un mapping avec `script` (ou needs/stage).
    Les clés réservées GitLab sont exclues explicitement : rien n'est deviné en silence."""
    reserves = {"stages", "workflow", "default", "include", "variables", "spec", "image", "services",
                "before_script", "after_script", "cache", "artifacts", "concurrent"}
    out = {}
    for cle, val in d.items():
        if cle in reserves:
            continue
        if isinstance(val, dict):
            out[cle] = val
    return out


# ------------------------------------------------------------------- évaluation des `rules:`
def eval_if(expr, ctx):
    """Supporte : $VAR, $VAR == "lit", $VAR != "lit", && , || . Toute autre forme -> None (inconnu),
    jamais une évaluation silencieuse qui inventerait un résultat."""
    if expr is None:
        return True
    termes = re.split(r"\s*\|\|\s*", str(expr).strip())
    résultats = []
    for groupe in termes:
        sous = [_eval_terme(t.strip(), ctx) for t in re.split(r"\s*&&\s*", groupe)]
        if any(s is None for s in sous):
            résultats.append(None)                     # groupe indécidable
        else:
            résultats.append(all(sous))
    if any(r is True for r in résultats):
        return True                                    # un groupe sûr passe
    if any(r is None for r in résultats):
        return None                                    # rien de sûr, mais rien d'exclu
    return False


def _eval_terme(term, ctx):
    m = re.fullmatch(r'(\$\w+)\s*(==|!=)\s*"?([A-Za-z0-9_./*-]*)"?', term)
    if m:
        var, op, lit = m.group(1), m.group(2), m.group(3)
        val = ctx.get(var[1:], "")
        return val == lit if op == "==" else val != lit
    m = re.fullmatch(r"(\$\w+)", term)
    if m:
        return bool(ctx.get(m.group(1)[1:], ""))
    return None


def outcome_job(job, ctx):
    """(état, quand, allow_failure) : état in {CREE, PEUT_ETRE, ABSENT} pour un contexte donné.

    Ordre des règles : GitLab arrête la pipeline à la PREMIÈRE règle qui correspond. Une règle
    `changes:` est indécidable sans diff ; on continue donc à scanner les règles suivantes, et une
    règle SUREMENT déclenchée plus bas rend le job sûrement créé (cas de `- changes: [...]` suivi
    de `- if: $CI_COMMIT_BRANCH == "main"` : sur main le job existe, changes ou non).
    """
    rules = job.get("rules")
    if not isinstance(rules, list):
        quand = job.get("when", "on_success")
        return CREE, quand, bool(job.get("allow_failure", False))
    etat, possibles = ABSENT, []
    for r in rules:
        if not isinstance(r, dict):
            return PEUT_ETRE, job.get("when", "on_success"), bool(job.get("allow_failure", False))
        cond = eval_if(r.get("if"), ctx)
        if cond is False:
            continue
        a_changes = "changes" in r
        a_quand = r.get("when", "on_success")
        # allow_failure : la règle écrase le job ; pour `rules: when: manual` le DÉFAUT est false
        # (référence YAML, rules:when:manual) — c'est ce qui rend le manuel BLOQUANT.
        if "allow_failure" in r:
            af = bool(r["allow_failure"])
        elif a_quand == "manual":
            af = False
        else:
            af = bool(job.get("allow_failure", False))
        possibles.append((a_quand, af))
        if cond is True and not a_changes:
            etat = CREE
            break
        if etat != CREE:
            etat = PEUT_ETRE
    if etat == ABSENT:
        return ABSENT, job.get("when", "on_success"), bool(job.get("allow_failure", False))
    # `quand`/`allow_failure` retenus = le pire reachable (un manuel bloquant dans un scénario
    # possible est un risque réel de pipeline `blocked`, donc on le signale)
    quand = "manual" if any(q == "manual" for q, _ in possibles) else possibles[0][0]
    af = all(a for _, a in possibles)
    return etat, quand, af


CREE, PEUT_ETRE, ABSENT = "CREE", "PEUT_ETRE", "ABSENT"


def workflow_accepte(d, ctx):
    wf = d.get("workflow") or {}
    return outcome_job(wf, ctx)[0] != ABSENT


# --------------------------------------------------------------------------------- les checks
def check(d, jobs, racine):
    anomalies = []
    stages = d.get("stages") or []
    if not isinstance(stages, list):
        stages = []

    # S1 stages résolues
    for nom, job in sorted(jobs.items()):
        st = job.get("stage", "test")
        if stages and st not in stages:
            anomalies.append(Anomalie("ROUGE", "S1-stage", nom, "stage %r absente de la liste %s" % (st, stages)))
    # stages déclarées sans emploi (info, pas anomalie)
    utilisees = {job.get("stage", "test") for job in jobs.values()}
    for st in stages:
        if st not in utilisees:
            anomalies.append(Anomalie("info", "S1-stage-inutilise", None,
                                      "stage %r déclarée mais aucun job ne l'utilise" % st))

    index = {st: i for i, st in enumerate(stages)}

    # S2 / S3 / S4 needs
    for nom, job in sorted(jobs.items()):
        for need in _liste_needs(job):
            cible, optional = need
            if cible not in jobs:
                anomalies.append(Anomalie("ROUGE", "S2-need-inconnu", nom,
                                          "needs cible %r : aucun job de ce nom dans le fichier" % cible))
                continue
            st_a, st_b = job.get("stage", "test"), jobs[cible].get("stage", "test")
            if stages and st_a in index and st_b in index and index[st_b] > index[st_a]:
                anomalies.append(Anomalie("ROUGE", "S3-need-futur", nom,
                                          "needs %r (stage %s) alors que %s est stage %s : GitLab exige "
                                          "le même stage ou un stage antérieur" % (cible, st_b, nom, st_a)))
            for ctx_nom, ctx in CONTEXTES.items():
                if not workflow_accepte(d, ctx):
                    continue
                etat_demandeur = outcome_job(job, ctx)[0]
                etat_cible = outcome_job(jobs[cible], ctx)[0]
                if etat_demandeur == ABSENT:
                    continue
                if etat_cible == ABSENT and not optional:
                    anomalies.append(Anomalie("ROUGE", "S4-need-absent", nom,
                                              "pipeline « %s » : %s est créé mais pas %s (cible de needs). "
                                              "La référence YAML dit que la pipeline NE SE CREERAIT PAS — "
                                              "ajouter optional: true à ce need" % (ctx_nom, nom, cible)))
                elif etat_cible == PEUT_ETRE and not optional and etat_demandeur == CREE:
                    anomalies.append(Anomalie("info", "S4-need-incertain", nom,
                                              "pipeline « %s » : %s dépend de %s, dont l'existence est "
                                              "conditionnée par un `changes:` — à surveiller au premier run "
                                              "(ou `optional: true`)" % (ctx_nom, nom, cible)))

    # S5 manuel bloquant
    for nom, job in sorted(jobs.items()):
        for ctx_nom, ctx in CONTEXTES.items():
            if not workflow_accepte(d, ctx):
                continue
            etat, quand, af = outcome_job(job, ctx)
            if etat == ABSENT or quand != "manual":
                continue
            if not af:
                anomalies.append(Anomalie("ROUGE", "S5-manuel-bloquant", nom,
                                          "pipeline « %s » : job manuel sans allow_failure: true. Par "
                                          "définition (rules:when:manual), allow_failure vaut false -> "
                                          "pipeline `blocked`, stages suivants annulés, et merge impossible "
                                          "tant que finalize a posé only_allow_merge_if_pipeline_succeeds" % ctx_nom))

    # S6 images épinglées + S7 outillage
    for nom, job in sorted(jobs.items()):
        for img in _liste_images(job):
            if ":" not in img or img.endswith(":latest"):
                anomalies.append(Anomalie("ROUGE", "S6-image-flottante", nom,
                                          "image %r non épinglée : un run qui change de base n'est plus "
                                          "la même mesure (le fichier dit lui-même « version épinglée exprès »)" % img))
        anomalies += _check_outillage(nom, job)

    # S8 chemins cités
    anomalies += _check_chemins(jobs, racine)

    # S9 couplages de version image <-> épingle du dépôt
    anomalies += _check_couplages(jobs, racine)
    return anomalies


def _liste_needs(job):
    out = []
    n = job.get("needs")
    if isinstance(n, list):
        for e in n:
            if isinstance(e, str):
                out.append((e, False))
            elif isinstance(e, dict) and "job" in e:
                out.append((e["job"], bool(e.get("optional", False))))
    return out


def _liste_images(job):
    out = []
    img = job.get("image")
    if isinstance(img, str):
        out.append(img)
    elif isinstance(img, dict) and isinstance(img.get("name"), str):
        out.append(img["name"])
    for s in job.get("services") or []:
        if isinstance(s, str):
            out.append(s)
        elif isinstance(s, dict) and isinstance(s.get("name"), str):
            out.append(s["name"])
    return out


def _check_outillage(nom, job):
    texte = "\n".join(_plat(job.get("before_script"))) + "\n" + "\n".join(_plat(job.get("script")))
    installs = set()
    # Ce que le job installe lui-même : `apt-get install ...`, `apk add ...`, `pip install ...`.
    # Les paquets Alpine/Debian n'ont pas le nom du binaire (maven -> mvn), d'où la table PAQUETS.
    for m in re.finditer(r"(?:apt-get|apk|pip|python3? -m pip|npm)[^\n]*?\b(?:install|add)\b[^\n]*", texte):
        apres = re.split(r"\b(?:install|add)\b", m.group(0), 1)[1]
        for tok in re.findall(r"[A-Za-z0-9._+-]+", apres):
            if tok in PAQUETS and PAQUETS[tok]:
                installs.add(PAQUETS[tok])
            elif tok in BINAIRES:
                installs.add("python3" if tok == "python" else tok)

    img = None
    for i in _liste_images(job):
        img = i
        break
    fournis = OUTILS_PAR_IMAGE.get(img)
    if fournis is None:
        if img:
            return [Anomalie("info", "S7-image-non-auditee", nom,
                             "image %r : outillage interne non mesuré ; passer la commande de "
                             "revérification de l'en-tête du script pour l'ajouter à la table" % img)]
        return []
    requis = set()
    for b in BINAIRES:
        if re.search(r"(?:^|[\s;|&$(`])%s(?:\s|$)" % re.escape(b), texte, re.M):
            requis.add("python3" if b == "python" else b)
    manquants = sorted(r for r in requis if r not in fournis and r not in installs)
    if manquants:
        return [Anomalie("ROUGE", "S7-binaire-absent", nom,
                         "image %r (outillage mesuré) ne fournit pas : %s — et le job ne l'installe pas. "
                         "Le script appellerait un binaire absent : %s"
                         % (img, ", ".join(manquants), _extraits(texte, manquants)))]
    return []


def _extraits(texte, binaires):
    lignes = []
    for l in texte.split("\n"):
        if any(re.search(r"(?:^|[\s;|&$(`])%s(?:\s|$)" % re.escape(b), l) for b in binaires):
            lignes.append(l.strip()[:90])
        if len(lignes) >= 2:
            break
    return " | ".join(lignes) or "(non localisé)"


def _plat(v):
    if v is None:
        return []
    if isinstance(v, str):
        return [v]
    if isinstance(v, list):
        out = []
        for e in v:
            if isinstance(e, str):
                out.append(e)
            elif isinstance(e, dict):
                out.append(" ".join(str(x) for x in e.values()))
        return out
    return [str(v)]


# ------------------------------------------------------------------ couplages de version (S9)
# Une image qui EMBARQUE l'outillage à une version fixe est une deuxième source de vérité à côté
# de l'épingle du dépôt (lock npm, contrainte de SDK). Leur divergence n'apparaît dans aucun champ
# du YAML et ne se voit qu'à l'exécution. Rouge MESURÉ le 2026-10-10 en conteneur, avec l'image
# playwright v1.49.0-jammy et e2/package-lock.json à 1.63.0 :
#   browserType.launch: Executable doesn't exist at /ms-playwright/chromium_headless_shell-1243/…
#   « Looks like Playwright was just updated to 1.63.0. Please update docker image as well. »
# Le job était dit « vert » depuis sa rédaction : seul le comptage de specs (`exit 0` si 0 spec)
# l'a empêché de rougir — donc le premier dépôt de spec aurait cassé sans autre signe avant-coureur.


def _tuple_version(v):
    return tuple(int(x) for x in re.findall(r"\d+", v)[:3])


def _verrou_playwright(racine):
    """Ce que `npm ci` installe réellement : le lock, pas la plage de e2/package.json."""
    p = os.path.join(racine, "e2", "package-lock.json")
    if not os.path.exists(p):
        return None, "e2/package-lock.json absent du dépôt"
    try:
        with open(p, encoding="utf-8") as f:
            d = json.load(f)
    except ValueError as e:
        return None, "e2/package-lock.json illisible (%s)" % e
    paquets = d.get("packages") or {}
    v = (paquets.get("node_modules/@playwright/test") or {}).get("version")
    if v is None:  # lockfileVersion 1/2 : `dependencies`
        v = ((d.get("dependencies") or {}).get("@playwright/test") or {}).get("version")
    if v is None:
        return None, "e2/package-lock.json : aucun paquet @playwright/test"
    return [(v, "e2/package-lock.json (version installée par `npm ci`)", "eq")], None


def _verrou_flutter(racine):
    """Borne basse du SDK déclarée par l'app, et pin posé par la plateforme qui tourne déjà."""
    contraintes, manques = [], []
    p = os.path.join(racine, "mobile", "pubspec.yaml")
    if os.path.exists(p):
        with open(p, encoding="utf-8") as f:
            m = re.search(r"^\s*flutter:\s*['\"]?>=\s*([0-9][0-9.]*)", f.read(), re.M)
        if m:
            contraintes.append((m.group(1), "mobile/pubspec.yaml (environment.flutter, borne basse)", "ge"))
        else:
            manques.append("mobile/pubspec.yaml sans contrainte flutter")
    else:
        manques.append("mobile/pubspec.yaml absent")
    g = os.path.join(racine, ".github", "workflows", "ci.yml")
    if os.path.exists(g):
        with open(g, encoding="utf-8") as f:
            m = re.search(r"flutter-version:\s*['\"]?([0-9][0-9.]*)", f.read())
        if m:
            # Les deux plateformes testent le MEME code : si GitLab embarque une autre version que
            # celle que GitHub épingle, « vert » sur l'une ne veut plus rien dire sur l'autre.
            contraintes.append((m.group(1), ".github/workflows/ci.yml (pin flutter-version, parité des plateformes)", "eq"))
        else:
            manques.append(".github/workflows/ci.yml sans pin flutter-version")
    else:
        manques.append(".github/workflows/ci.yml absent")
    if not contraintes:
        return None, " et ".join(manques)
    return contraintes, None


COUPLAGES_VERSION = [
    (r"^mcr\.microsoft\.com/playwright:v(\d+\.\d+\.\d+)(?:-[a-z0-9.]+)?$",
     "Playwright : navigateurs embarqués par l'image", _verrou_playwright),
    (r"^ghcr\.io/cirruslabs/flutter:(\d+\.\d+\.\d+)(?:-[a-z0-9.]+)?$",
     "Flutter : SDK embarqué par l'image", _verrou_flutter),
]


def _check_couplages(jobs, racine):
    anomalies = []
    for nom, job in sorted(jobs.items()):
        for img in _liste_images(job):
            for motif, libelle, lire in COUPLAGES_VERSION:
                m = re.match(motif, img)
                if not m:
                    continue
                contraintes, absence = lire(racine)
                if contraintes is None:
                    anomalies.append(Anomalie("info", "S9-source-absente", nom,
                                              "%s : couplage non contrôlable ici (%s)" % (libelle, absence)))
                    continue
                for attendu, source, mode in contraintes:
                    tag = _tuple_version(m.group(1))
                    cible = _tuple_version(attendu)
                    ok = tag == cible if mode == "eq" else tag >= cible
                    if not ok:
                        anomalies.append(Anomalie(
                            "ROUGE", "S9-couplage-version", nom,
                            "%s : l'image %r embarque la version %s, %s exige %s. Rien dans le YAML "
                            "n'est faux, et le job échoue à l'exécution (rouge mesuré le 2026-10-10 en "
                            "conteneur sur ce couple : « Executable doesn't exist at …chromium_headless_shell-1243/… »)"
                            % (libelle, img, m.group(1), source, attendu)))
    return anomalies


def _check_chemins(jobs, racine):
    anomalies = []
    motif_fichier = re.compile(r"^[A-Za-z0-9._/-]+\.[A-Za-z0-9]+$")
    for nom, job in sorted(jobs.items()):
        cites = set()
        for r in job.get("rules") or []:
            if isinstance(r, dict):
                for c in r.get("changes") or []:
                    if isinstance(c, str) and motif_fichier.match(c) and "*" not in c:
                        cites.add(c)
        texte = "\n".join(_plat(job.get("before_script")) + _plat(job.get("script")))
        for m in re.finditer(r"(?:^|[\s'\"(])((?:scripts|performance-tests|backend|frontend|mobile|e2)/[A-Za-z0-9._/-]+\.[A-Za-z0-9]+)", texte):
            cites.add(m.group(1))
        for c in sorted(cites):
            p = os.path.join(racine, c)
            if not os.path.exists(p):
                anomalies.append(Anomalie("ROUGE", "S8-chemin-faux", nom,
                                          "chemin cité par le job inexistant dans le dépôt : %s" % c))
            elif c.endswith(".sh") and not os.access(p, os.X_OK):
                anomalies.append(Anomalie("info", "S8-script-non-executable", nom,
                                          "%s n'est pas exécutable (les jobs l'appellent via `bash`, "
                                          "donc non bloquant)" % c))
    return anomalies


# ----------------------------------------------------------------------------- preuve registre
def _http(url, headers=None, method="GET"):
    req = urllib.request.Request(url, headers=headers or {}, method=method)
    try:
        with urllib.request.urlopen(req, timeout=25) as r:
            return r.status, r.headers, r.read()
    except urllib.error.HTTPError as e:
        return e.code, e.headers, e.read()
    except Exception as e:
        return None, {}, str(e).encode()


ACCEPT = {"Accept": "application/vnd.oci.image.index.v1+json,"
                    "application/vnd.docker.distribution.manifest.list.v2+json,"
                    "application/vnd.docker.distribution.manifest.v2+json"}


def _decompose(ref):
    """Découpe une référence d'image en (dépôt, tag, hôte) pour la requête registre.

    `node:23` -> (library/node, 23, registry-1.docker.io)
    `aquasec/trivy:0.75.0` -> (aquasec/trivy, 0.75.0, registry-1.docker.io)
    `ghcr.io/cirruslabs/flutter:3.35.6` -> (cirruslabs/flutter, 3.35.6, ghcr.io)
    Le premier segment n'est un HÔTE que s'il porte un point ou un deux-points (règle Docker).
    """
    partie, tag = ref.rsplit(":", 1) if ":" in ref and not ref.endswith(":") else (ref, "latest")
    if "/" not in partie:
        return "library/" + partie, tag, "registry-1.docker.io"
    hote = partie.split("/", 1)[0]
    if "." not in hote and ":" not in hote:
        return partie, tag, "registry-1.docker.io"
    if hote in ("docker.io", "index.docker.io"):
        repo = partie.split("/", 1)[1]
        return ("library/" + repo if "/" not in repo else repo), tag, "registry-1.docker.io"
    return partie.split("/", 1)[1], tag, hote


def preuve_images(d, jobs):
    """Interroge les registres publics (jeton anonyme) : le tag existe-t-il vraiment ?"""
    refs = set()
    for job in jobs.values():
        refs.update(_liste_images(job))
    resultat = []
    for ref in sorted(refs):
        repo, tag, hote = _decompose(ref)
        if hote == "registry-1.docker.io":
            st, _, body = _http("https://auth.docker.io/token?service=registry.docker.io&scope=repository:%s:pull" % repo)
            if st != 200:
                resultat.append((ref, "AUTH ANONYME REFUSE"))
                continue
            tok = json.loads(body)["token"]
            base = "https://registry-1.docker.io/v2/%s/manifests/%s" % (repo, tag)
        elif hote == "ghcr.io":
            st, _, body = _http("https://ghcr.io/token?scope=repository:%s:pull&service=ghcr.io" % repo)
            if st != 200:
                resultat.append((ref, "AUTH ANONYME REFUSE"))
                continue
            tok = json.loads(body)["token"]
            base = "https://ghcr.io/v2/%s/manifests/%s" % (repo, tag)
        else:
            tok = None
            base = "https://%s/v2/%s/manifests/%s" % (hote, repo, tag)
        hdrs = dict(ACCEPT)
        if tok:
            hdrs["Authorization"] = "Bearer " + tok
        st, h, _ = _http(base, hdrs, "HEAD")
        resultat.append((ref, "present (%s)" % h.get("Docker-Content-Digest", "?")[:20] if st == 200
                         else "ABSENT HTTP %s" % st))
    return resultat


# --------------------------------------------------------------------------------------- main
def main():
    ap = argparse.ArgumentParser(description="Auto-validation structurelle de .gitlab-ci.yml")
    ap.add_argument("--file", default=".gitlab-ci.yml")
    ap.add_argument("--jobs", action="store_true",
                    help="inventaire jobs / stages / tolérants / manuels / environnements puis sort")
    ap.add_argument("--registry", action="store_true", help="prouve l'existence des images (RESEAU)")
    args = ap.parse_args()

    racine = os.getcwd()
    d = charger(args.file)
    jobs = jobs_de(d)
    stages = d.get("stages") or []

    if args.jobs:
        print("jobs (%d) : %s" % (len(jobs), ", ".join(sorted(jobs))))
        print("stages (%d) : %s" % (len(stages), ", ".join(stages)))
        # Inventaire promis par ADR-008 §8 : quels jobs sont tolérants, lesquels sont manuels, et
        # quels déploiements laissent un enregistrement (`environment:`) — c'est précisément ce que
        # le runbook GITLAB-BOOTSTRAP §0/§4 laissait vérifier à la main, et ce recomptage manuel est
        # exactement ce qui a laissé passer le manuel bloquant `performance:k6`.
        ctx = CONTEXTES["push main"]
        tolis, manuels, envs = [], [], []
        for nom in sorted(jobs):
            etat, quand, af = outcome_job(jobs[nom], ctx)
            if quand == "manual":
                manuels.append("%s (%s)" % (nom, "tolérant" if af else "BLOQUANT"))
            if af:
                tolis.append(nom)
            env = jobs[nom].get("environment")
            if env:
                envs.append("%s -> %s" % (nom, env.get("name") if isinstance(env, dict) else env))
        print("tolérants (%d) : %s" % (len(tolis), ", ".join(tolis) or "aucun"))
        print("manuels (%d) : %s" % (len(manuels), ", ".join(manuels) or "aucun"))
        print("environnements (%d) : %s" % (len(envs), ", ".join(envs) or "aucun"))
        return 0

    anomalies = check(d, jobs, racine)
    rouges = [a for a in anomalies if a.niveau == "ROUGE"]
    absence_registre = 0

    print("== auto-validation %s : %d jobs, %d stages ==" % (args.file, len(jobs), len(stages)))
    if args.registry:
        print("-- existence des images, prouvee aupres des registres (jeton anonyme) --")
        for ref, etat in preuve_images(d, jobs):
            if not etat.startswith("present"):
                absence_registre += 1
            print("   [%s] %s" % ("ok" if etat.startswith("present") else "ROU", ref + " -> " + etat))
    for a in anomalies:
        print("  " + str(a))
    if not anomalies:
        print("  [ok] aucune anomalie : la structure est conforme aux règles GitLab vérifiées ici")
    print("-- bilan : %d rouge(s), %d info, %d image(s) absente(s) du registre --"
          % (len(rouges), len(anomalies) - len(rouges), absence_registre))
    # `--registry` est une preuve MANUELLE : elle rougit quand on la demande, mais la porte de
    # pipeline tourne sans ce drapeau, pour ne pas rougir d'un registre en panne. Même arbitrage
    # que security:owasp-backend (port aléatoire = pas une porte).
    return 1 if (rouges or absence_registre) else 0


if __name__ == "__main__":
    sys.exit(main())
