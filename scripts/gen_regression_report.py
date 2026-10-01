#!/usr/bin/env python3
"""§G6.4 — Génère reports/REGRESSION_REPORT.md à partir des VRAIS résultats :
- backend : backend/target/surefire-reports/*.xml (tests d'intégration/unitaires)
- frontend : frontend/src/__tests__ via vitest (fichier e2e/results.json pour Playwright)
- mobile : mobile/test (résumé manuel si pas de JSON)

Usage : python3 scripts/gen_regression_report.py
"""
import glob
import json
import os
import pathlib
import re
import sys
from datetime import date, datetime, timezone

ROOT = pathlib.Path(__file__).resolve().parent.parent

def parse_surefire(pattern):
    total = fails = errs = skipped = 0
    suites = []
    for f in sorted(glob.glob(str(ROOT / pattern))):
        try:
            xml = open(f, encoding="utf-8", errors="ignore").read()
        except OSError:
            continue
        m = re.search(r'<testsuite[^>]*name="([^"]+)"[^>]*tests="(\d+)"[^>]*(?:errors="(\d+)")?[^>]*(?:skipped="(\d+)")?[^>]*(?:failures="(\d+)")?', xml)
        if not m:
            m = None
        # attributs dans un ordre variable : extraction robuste
        tag = re.search(r"<testsuite\b[^>]*>", xml)
        if not tag:
            continue
        attrs = dict(re.findall(r'(\w+)="([^"]*)"', tag.group(0)))
        name = attrs.get("name", os.path.basename(f))
        t = int(attrs.get("tests", 0) or 0)
        fa = int(attrs.get("failures", 0) or 0)
        er = int(attrs.get("errors", 0) or 0)
        sk = int(attrs.get("skipped", 0) or 0)
        total += t; fails += fa; errs += er; skipped += sk
        status = "✅" if fa == 0 and er == 0 else "❌"
        suites.append((name, t, fa, er, sk, status))
    return total, fails, errs, skipped, suites

def parse_playwright():
    p = ROOT / "frontend/e2e/results.json"
    if not p.exists():
        return None
    data = json.loads(p.read_text())
    stats = data.get("stats")
    suites = data.get("suites") or []
    results = []
    def walk(s):
        for spec in s.get("specs") or []:
            for t in spec.get("tests") or []:
                status = t.get("status")  # expected/flaky/unexpected/skipped
                ok = status == "expected"
                results.append((spec.get("projectId") or "chromium", spec.get("title"), "✅" if ok else ("⏭️" if status=="skipped" else "❌")))
        for child in s.get("suites") or []:
            walk(child)
    for s in suites:
        walk(s)
    return stats, results

def main():
    lines = []
    lines.append("# Rapport de régression — Discipolat (G6.4)")
    lines.append("")
    lines.append(f"- Généré le : {datetime.now(timezone.utc).astimezone().strftime('%Y-%m-%d %H:%M')}")
    lines.append("- Sources : `mvn test` (surefire), `vitest run`, `npx playwright test` (e2e/results.json)")
    lines.append("")

    # ── Backend ──
    bt, bf, be, bs, bsuites = parse_surefire("backend/target/surefire-reports/TEST-*.xml")
    lines.append("## 1. Backend (JUnit / surefire)")
    if bt:
        verdict = "✅ VERT" if bf == 0 and be == 0 else "❌ ROUGE"
        lines.append(f"- Total : **{bt} tests** — {bf} échecs, {be} erreurs, {bs} sautés → **{verdict}**")
        lines.append("")
        lines.append("<details><summary>Détail par classe (" + str(len(bsuites)) + ")</summary>")
        lines.append("")
        lines.append("| Classe | Tests | Échecs | Erreurs | Sautés | |")
        lines.append("|---|---|---|---|---|---|")
        for name, t, fa, er, sk, st in bsuites:
            lines.append(f"| {name} | {t} | {fa} | {er} | {sk} | {st} |")
        lines.append("")
        lines.append("</details>")
    else:
        lines.append("- ⚠️ Pas de rapports surefire : lancer `mvn test` d'abord.")
    lines.append("")

    # ── Frontend unit ──
    vlog = None
    for cand in ("/tmp/vitest-full.log",):
        if os.path.exists(cand):
            vlog = cand
    lines.append("## 2. Frontend (Vitest)")
    if vlog:
        txt = open(vlog, encoding="utf-8", errors="ignore").read()
        m = re.search(r"Test Files\s+(\d+) passed.*?Tests\s+(\d+) passed", txt, re.S)
        f2 = re.search(r"(\d+) failed", txt)
        if m:
            lines.append(f"- Fichiers : {m.group(1)} verts ; Tests : **{m.group(2)} passés**" +
                         (f" — ⚠️ {f2.group(1)} failed" if f2 else " — ✅ 0 échec"))
        else:
            lines.append(f"- Dernières lignes : {txt.strip().splitlines()[-2:]}")
    else:
        lines.append("- ⚠️ /tmp/vitest-full.log absent : lancer `npx vitest run > /tmp/vitest-full.log`.")
    lines.append("")

    # ── E2E ──
    lines.append("## 3. Parcours critiques E2E (Playwright)")
    pw = parse_playwright()
    if pw:
        stats, results = pw
        lines.append(f"- stats : {json.dumps(stats, ensure_ascii=False)}" if stats else "- (pas de bloc stats)")
        lines.append("")
        lines.append("| Projet | Parcours | |")
        lines.append("|---|---|---|")
        for proj, title, st in results:
            lines.append(f"| {proj} | {title} | {st} |")
    else:
        lines.append("- ⚠️ frontend/e2e/results.json absent : lancer `npx playwright test`.")
    lines.append("")

    # ── Mobile ──
    lines.append("## 4. Mobile (Flutter)")
    lines.append("- `flutter test` : voir sortie de la passe de validation (nombre de tests verts," +
                 " 0 échec exigé pour le GO).")
    lines.append("")

    out = ROOT / "reports/REGRESSION_REPORT.md"
    out.parent.mkdir(exist_ok=True)
    out.write_text("\n".join(lines) + "\n", encoding="utf-8")
    print(f"Écrit : {out}")

if __name__ == "__main__":
    main()
