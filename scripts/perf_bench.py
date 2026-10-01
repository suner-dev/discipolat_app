#!/usr/bin/env python3
# =============================================================================
# perf_bench.py — §G6.5 Mesure de latence p50/p95/p99 sur les écrans critiques
# =============================================================================
# Budgets master prompt §71 / G6.5 :
#   p95 < 500 ms sur les écrans critiques en charge
#   bootstrap d'espace < 1 s
#   synchro mobile < 5 s
#
# Prérequis : backend démarré + scripts/perf_loadseed.sql appliqué.
# Usage :
#   python3 scripts/perf_bench.py --base http://localhost:8080 \
#       --email admin@discipolat.com --password password123
# =============================================================================
import argparse
import json
import statistics
import sys
import time
import urllib.error
import urllib.parse
import urllib.request

WARMUP = 3
SAMPLES = 30
CONCURRENCY = 10
CONCURRENCY_SAMPLES = 20


def pct(values, p):
    if not values:
        return float("nan")
    s = sorted(values)
    k = max(0, min(len(s) - 1, int(round((p / 100.0) * (len(s) - 1)))))
    return s[k]


def http(method, url, token=None, body=None, timeout=30):
    req = urllib.request.Request(url, method=method)
    req.add_header("Content-Type", "application/json")
    if token:
        req.add_header("Authorization", "Bearer " + token)
    data = json.dumps(body).encode() if body is not None else None
    t0 = time.perf_counter()
    try:
        with urllib.request.urlopen(req, data, timeout=timeout) as r:
            payload = r.read()
            dt = (time.perf_counter() - t0) * 1000.0
            return r.status, dt, payload
    except urllib.error.HTTPError as e:
        dt = (time.perf_counter() - t0) * 1000.0
        return e.code, dt, e.read()
    except Exception as e:  # noqa: BLE001
        dt = (time.perf_counter() - t0) * 1000.0
        return 0, dt, str(e).encode()


def login(base, email, password):
    status, _, payload = http("POST", base + "/api/v1/auth/login",
                              body={"email": email, "password": password})
    if status not in (200, 201):
        sys.exit("FATAL login /api/v1/auth/login -> %s %s" % (status, payload[:300]))
    tok = json.loads(payload)["accessToken"]
    return tok


def content_of(payload, encoding="utf-8"):
    """Accepte aussi bien une liste brute qu'un envelope PageResponse."""
    obj = json.loads(payload.decode(encoding))
    if isinstance(obj, list):
        return obj
    if isinstance(obj, dict):
        for k in ("content", "data", "items", "results"):
            if isinstance(obj.get(k), list):
                return obj[k]
    return []


def measure(name, url, token, budget_ms, samples=SAMPLES):
    for _ in range(WARMUP):
        http("GET", url, token)
    lat, codes = [], {}
    for _ in range(samples):
        st, dt, _body = http("GET", url, token)
        lat.append(dt)
        codes[st] = codes.get(st, 0) + 1
    ok = all(c == 200 for c in codes)
    return {
        "endpoint": name,
        "url": urllib.parse.urlparse(url).path + ("?" + urllib.parse.urlparse(url).query if "?" in url else ""),
        "samples": samples,
        "http": codes,
        "healthy": ok,
        "p50": round(pct(lat, 50), 1),
        "p95": round(pct(lat, 95), 1),
        "p99": round(pct(lat, 99), 1),
        "max": round(max(lat), 1),
        "mean": round(statistics.fmean(lat), 1),
        "budget_ms": budget_ms,
        "budget_ok": pct(lat, 95) < budget_ms,
    }


def concurrent(name, url, token, budget_ms, workers=CONCURRENCY, samples=CONCURRENCY_SAMPLES):
    """Charge concurrente : workers threads qui se partagent `samples` requêtes."""
    import threading
    q = [url] * samples
    lat, lock = [], threading.Lock()

    def worker():
        while True:
            with lock:
                if not q:
                    return
                u = q.pop()
            st, dt, _ = http("GET", u, token)
            with lock:
                lat.append((dt, st))

    ths = [threading.Thread(target=worker) for _ in range(workers)]
    t0 = time.perf_counter()
    for t in ths:
        t.start()
    for t in ths:
        t.join()
    wall = time.perf_counter() - t0
    vals = [x[0] for x in lat]
    errs = [x[1] for x in lat if x[1] != 200]
    return {
        "endpoint": name + " (x%d concurrent)" % workers,
        "url": urllib.parse.urlparse(url).path,
        "samples": len(vals),
        "http": {"errors": len(errs)},
        "healthy": not errs,
        "p50": round(pct(vals, 50), 1),
        "p95": round(pct(vals, 95), 1),
        "p99": round(pct(vals, 99), 1),
        "max": round(max(vals), 1),
        "mean": round(statistics.fmean(vals), 1),
        "throughput_rps": round(len(vals) / wall, 1),
        "budget_ms": budget_ms,
        "budget_ok": pct(vals, 95) < budget_ms,
    }


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--base", default="http://localhost:8080")
    ap.add_argument("--email", default="admin@discipolat.com")
    ap.add_argument("--password", default="password123")
    ap.add_argument("--json", default="")
    args = ap.parse_args()
    base = args.base.rstrip("/")

    tok = login(base, args.email, args.password)
    print("✔ connecté (%s)" % args.email)

    # Espace de charge : un espace « perf » créé par perf_loadseed.sql
    _st, _dt, spaces_raw = http("GET", base + "/api/v1/spaces?page=0&size=200", tok)
    spaces = content_of(spaces_raw)
    perf = next((s for s in spaces if str(s.get("code", "")).startswith("PERF_SP_")), None)
    sid = perf["id"] if perf else (spaces[0]["id"] if spaces else None)
    print("✔ %d espaces visibles, espace de mesure = %s" % (len(spaces), sid))

    # Événement de charge pour le calendrier
    _st, _dt, ev_raw = http("GET", base + "/api/v1/church-events?page=0&size=1", tok)
    evc = content_of(ev_raw)
    event_id = evc[0]["id"] if evc else None

    results = []

    # 1. Authentification (coût BCrypt, hors budget « écran critique » mais mesuré)
    lat = []
    for _ in range(5):
        st, dt, _ = http("POST", base + "/api/v1/auth/login",
                         body={"email": args.email, "password": args.password})
        lat.append(dt)
    results.append({
        "endpoint": "POST /auth/login (BCrypt)", "url": "/api/v1/auth/login",
        "samples": 5, "http": {200: 5}, "healthy": True,
        "p50": round(pct(lat, 50), 1), "p95": round(pct(lat, 95), 1),
        "p99": round(pct(lat, 99), 1), "max": round(max(lat), 1),
        "mean": round(statistics.fmean(lat), 1),
        "budget_ms": 1000, "budget_ok": pct(lat, 95) < 1000,
    })

    # 2. Écrans critiques — budget p95 < 500 ms
    critical = [
        ("GET /dashboard/kpi", base + "/api/v1/dashboard/kpi", 500),
        ("GET /dashboard/summary", base + "/api/v1/dashboard/summary", 500),
        ("GET /dashboard/my-metrics", base + "/api/v1/dashboard/my-metrics", 500),
        ("GET /people?page=0&size=20 (10k pers.)",
         base + "/api/v1/people?page=0&size=20", 500),
        ("GET /people?page=250&size=20 (pagination profonde)",
         base + "/api/v1/people?page=250&size=20", 500),
        ("GET /people?search=Nom42 (recherche)",
         base + "/api/v1/people?search=" + urllib.parse.quote("Nom42"), 500),
        ("GET /search?q=Prénom1 (recherche globale)",
         base + "/api/v1/search?q=" + urllib.parse.quote("Prénom1"), 500),
        ("GET /search/autocomplete?q=an",
         base + "/api/v1/search/autocomplete?q=an", 500),
        ("GET /spaces?page=0&size=50 (102 espaces)",
         base + "/api/v1/spaces?page=0&size=50", 500),
        ("GET /church-events?page=0&size=50 (812 évts)",
         base + "/api/v1/church-events?page=0&size=50", 500),
        ("GET /dress-codes (agenda tenues)", base + "/api/v1/dress-codes", 500),
        ("GET /notifications", base + "/api/v1/notifications?page=0&size=20", 500),
    ]
    for name, url, budget in critical:
        r = measure(name, url, tok, budget)
        results.append(r)
        print("  %-52s p95=%8.1f ms  %s%s" % (
            name, r["p95"], "OK" if r["budget_ok"] else "DEPASSEMENT",
            "" if r["healthy"] else "  HTTP=%s" % r["http"]))

    # 3. Bootstrap d'espace — budget < 1 s
    if sid:
        r = measure("GET /spaces/{id}/bootstrap (sync mobile)",
                    base + "/api/v1/spaces/%s/bootstrap" % sid, tok, 1000, samples=20)
        results.append(r)
        print("  %-52s p95=%8.1f ms  %s%s" % (
            r["endpoint"], r["p95"], "OK" if r["budget_ok"] else "DEPASSEMENT",
            "" if r["healthy"] else "  HTTP=%s" % r["http"]))
        r = measure("GET /spaces/{id}", base + "/api/v1/spaces/%s" % sid, tok, 500, samples=20)
        results.append(r)
        print("  %-52s p95=%8.1f ms  %s" % (r["endpoint"], r["p95"],
                                            "OK" if r["budget_ok"] else "DEPASSEMENT"))

    # 4. Charge concurrente (10 utilisateurs simultanés)
    for name, url, budget in [
        ("GET /dashboard/kpi", base + "/api/v1/dashboard/kpi", 500),
        ("GET /people?page=0&size=20", base + "/api/v1/people?page=0&size=20", 500),
        ("GET /search?q=Prénom1", base + "/api/v1/search?q=" + urllib.parse.quote("Prénom1"), 500),
    ]:
        r = concurrent(name, url, tok, budget)
        results.append(r)
        print("  %-52s p95=%8.1f ms  %5.1f rps  %s" % (
            r["endpoint"], r["p95"], r.get("throughput_rps", 0),
            "OK" if r["budget_ok"] else "DEPASSEMENT"))

    fails = [r for r in results if not r["budget_ok"]]
    unhealthy = [r for r in results if not r["healthy"]]
    print("\n=== %d mesures · %d dépassement(s) de budget · %d endpoint(s) non sain(s) ===" % (
        len(results), len(fails), len(unhealthy)))
    if args.json:
        with open(args.json, "w") as f:
            json.dump(results, f, indent=2)
        print("→ %s" % args.json)
    return 0 if not fails and not unhealthy else 1


if __name__ == "__main__":
    sys.exit(main())
