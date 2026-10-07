#!/usr/bin/env node
/**
 * Audit contract : routes backend reelles (annotations @RequestMapping de
 * classe + mappings de methodes, y compris sans chemin) vs appels clients
 * mobile (.dart) et frontend (.ts/.tsx), avec fichier:ligne de l'appelant.
 *
 * Port scripts/audit_contract.py (python absent du poste Windows) +
 * corrections : le .py ignorait les mappings sans parenthese (@GetMapping
 * nu = route de classe) et les tableaux de chemins (value = {"/a", "/b"}).
 *
 * Trois angles morts de l'analyse statique sont traites explicitement, parce
 * que chacun cachait un vrai bug deja parti en production :
 *  1. CONCATENATION  `'/discipleship-paths/member/' + memberId` : la partie
 *     variable vaut {X}, sinon le chemin parait inexistant (faux positif).
 *  2. DYNAMIQUE      `POST /transfers/${id}/${action}` : l'identifiant d'action
 *     n'est pas resolvable. Le client DOIT declarer le jeu de routes attendues
 *     (`// audit-routes: /transfers/{id}/submit, ...`) et chaque route declaree
 *     est verifiee contre le backend — une declaration qui ne correspond a
 *     aucune route reelle est une ERREUR, pas une echappatoire.
 *  3. LITERAL-SHADOW `/backups/restore-points` n'existe pas, mais est avale
 *     par le pattern `/backups/{id}` : un literal qui ne matche QUE via un
 *     chemin-variable est signale (le serveur repondra 400, pas 404).
 *
 * Usage : node scripts/audit_contract.mjs [racine-projet]
 * Sortie : liste des chemins appeles sans route backend correspondante.
 * Code de sortie : 0 = aucun ecart, 1 = ecart(s) trouve(s).
 */
import fs from 'node:fs';
import path from 'node:path';

const ROOT = process.argv[2] || '.';

/* ---------- 1. routes backend reelles ---------- */

function walk(dir, pred, out = []) {
  if (!fs.existsSync(dir)) return out;
  for (const e of fs.readdirSync(dir, { withFileTypes: true })) {
    const fp = path.join(dir, e.name);
    if (e.isDirectory()) walk(fp, pred, out);
    else if (pred(e.name)) out.push(fp);
  }
  return out;
}

// @RequestMapping("...") / (value = "...") / (path = "...") — chaine OU
// tableau ({"/a", "/b"}) : les deux formes existent dans le code (V240).
const RE_CLASS_BASE = /@RequestMapping\(\s*(?:value\s*=\s*|path\s*=\s*)?(\{[^)]*\}|"[^"]*")/;
// Mappements de methode avec chemin (Get/Post/Put/Patch/Delete).
const RE_METHOD_PATHED = /@(Get|Post|Put|Patch|Delete)Mapping\(\s*(?:value\s*=\s*|path\s*=\s*)?\s*(\{[^}]*\}|"[^"]*")/g;
// Mappements de methode SANS chemin : nu (@GetMapping + nouvelle ligne) OU
// avec parentheses mais sans chemin (@GetMapping(params = …), (produces = …))
// — dans les deux cas Spring mappe sur la base de classe.
const RE_METHOD_BARE = /@(?:Get|Post|Put|Patch|Delete)Mapping\s*(?:\(\s*(?:\)|(?![")]))|(?!["(]))/g;

// Deux index SEPARes : le code est l'autorite ; openapi.json ne doit jamais
// masquer une route morte (le spec peut etre plus vieille que le code).
const codeRoutes = new Set();
function expandQuoted(token) {
  const found = token.match(/"[^"]*"/g) || [];
  return found.length ? found.map((s) => s.replace(/^"|"$/g, '')) : [''];
}
for (const f of walk(path.join(ROOT, 'backend/src/main/java'), (n) => n.endsWith('Controller.java'))) {
  const txt = fs.readFileSync(f, 'utf8');
  const m = txt.match(RE_CLASS_BASE);
  const bases = m ? expandQuoted(m[1]) : [''];
  for (const base of bases) {
    // Mappements avec chemin : chaine unique ou tableau de chaines.
    for (const mm of txt.matchAll(RE_METHOD_PATHED)) {
      for (const p of expandQuoted(mm[2])) addRoute(joinRoute(base, p));
    }
    // Mappements nus : route = base de classe.
    for (const mm of txt.matchAll(RE_METHOD_BARE)) addRoute(joinRoute(base, ''));
  }
}

function addRoute(r) {
  // Une route blanchie par un tableau multigne ({"/a",\n "/b"}) n'est pas une
  // route : elle fausserait l'indexage par nombre de segments.
  const clean = r.trim();
  if (!clean || /\s/.test(clean)) return;
  codeRoutes.add(clean);
}

function joinRoute(base, p) {
  const b = (base || '').replace(/\/+$/, '');
  if (!p) return b || '/';
  return b + '/' + p.replace(/^\/+/, '');
}

// OpenAPI : index SEPARE. Une route presente uniquement dans la spec est une
// alerte (spec plus vieille que le code), JAMAIS une couverture.
const apiRoutes = new Set();
try {
  const openapi = JSON.parse(fs.readFileSync(path.join(ROOT, 'docs/openapi.json'), 'utf8'));
  for (const p of Object.keys(openapi.paths || {})) apiRoutes.add(p.replace(/^\/+/, '/'));
} catch { /* docs/openapi.json absent ou illisible : non bloquant */ }

// Indexe par nb de segments pour eviter un balayage O(routes) par appel.
// Chaque entree garde la route d'origine et la position de ses segments
// « identifiant » ({id}, {*Id}) : seul un literal plaque sur UN de ceux-la est
// suspect. Un literal plaque sur une variable non-identifiante ({category},
// {provider}, {key}) est du REST normal.
const RE_ID_VAR = /^\{(?:id|[A-Za-z0-9_]*Id)\}$/;
const looksLikeId = (s) => s === '{X}' || /^[0-9a-fA-F-]{36}$/.test(s) || /^\d+$/.test(s);

function buildIndex(set) {
  const bySeg = new Map();
  for (const r of set) {
    const segs = r.split('/');
    const rx = new RegExp('^' + segs.map((seg) =>
      seg.startsWith('{') ? '[^/]+' : seg.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
    ).join('/') + '$');
    const n = segs.length;
    if (!bySeg.has(n)) bySeg.set(n, []);
    bySeg.get(n).push({
      rx, route: r,
      idPos: segs.reduce((acc, s, i) => (RE_ID_VAR.test(s) ? acc.concat(i) : acc), []),
    });
  }
  return bySeg;
}
const codeIdx = buildIndex(codeRoutes);
const apiIdx = buildIndex(apiRoutes);

// Renvoie {exact, shadow} : exact = route couvrante ; shadow = seule une route
// a {id} correspond et le client y plaque un literal qui n'est pas un
// identifiant (Spring tentera la conversion UUID/Long -> 400, jamais 404).
function matchIn(idx, candidate) {
  const cp = candidate.split('?')[0].replace(/\/+$/, '');
  const variants = cp.startsWith('/api') ? [cp] : ['/api/v1' + cp, cp];
  let shadow = null;
  for (const v of variants) {
    const segs = v.split('/');
    for (const e of idx.get(segs.length) || []) {
      if (!e.rx.test(v)) continue;
      const offending = e.idPos.filter((i) => !looksLikeId(segs[i]));
      if (!offending.length) return { exact: e.route, shadow: null };
      shadow ??= e.route;
    }
  }
  return { exact: null, shadow };
}
function exists(candidate) {
  const m = matchIn(codeIdx, candidate);
  return !!m.exact;
}
// Un chemin couvert UNIQUEMENT par une route /{id} sur laquelle le client
// plaque un non-identifiant est une route morte : le serveur repondra 400.
function isShadowedLiteral(candidate) {
  const m = matchIn(codeIdx, candidate);
  return !m.exact && m.shadow ? m.shadow : null;
}
function existsOnlySpec(candidate) {
  return !exists(candidate) && !!matchIn(apiIdx, candidate).exact;
}

/* ---------- 2. appels clients ---------- */

// Tete d'appel : recepteur connue + methode HTTP + parenthese ouvrante.
// L'argument est ensuite extrait par parcours equilibre (voir readArg), car
// les regex de literals ne peuvent pas couvrir a la fois les template
// strings `${r['id']}` et les concatenations '/x/' + id.
const RE_CALL_HEAD = /(?:_api|api|axios|client|http(?:Client)?|dio|restClient)\s*\.\s*(?:get|post|put|patch|delete)(?:<[^>]+>)?\s*\(\s*/g;

const OPEN = { '(': ')', '[': ']', '{': '}' };
const CLOSE = { ')': 1, ']': 1, '}': 1 };

/** Lit l'expression du premier argument a partir de `i`, jusqu'a la virgule
 *  ou la parenthese fermante de meme profondeur.
 *
 *  Machine a etats explicite, parce que les trois pieges sont reels dans ce
 *  depot : (1) une interpolation Dart contient des quotes
 *  `'/forms/${widget.form?['id'] ?? "general"}/responses'` — couper a la quote
 *  interieure tronquait le chemin ; (2) `${}` imbriques ; (3) les parentheses
 *  d'un objet JS `api.get('/x', { params })` ne doivent pas etre avalées. */
function readArg(txt, i) {
  const stack = [];
  let out = '';
  let k = i;
  while (k < txt.length) {
    const c = txt[k];
    const mode = stack[stack.length - 1];
    if (!mode) {                                     // niveau de l'argument
      if (c === ',' || c === ')') return out;
      if (c === "'" || c === '"' || c === '`') { stack.push({ type: 'str', q: c }); out += c; k++; continue; }
      if (OPEN[c]) { stack.push({ type: 'grp' }); out += c; k++; continue; }
      out += c; k++; continue;
    }
    if (mode.type === 'grp') {
      if (c === ')' || c === ']' || c === '}') { stack.pop(); out += c; k++; continue; }
      if (c === "'" || c === '"' || c === '`') { stack.push({ type: 'str', q: c }); out += c; k++; continue; }
      if (OPEN[c]) { stack.push({ type: 'grp' }); out += c; k++; continue; }
      out += c; k++; continue;
    }
    if (mode.type === 'str') {
      if (c === '\\') { out += c + (txt[k + 1] ?? ''); k += 2; continue; }
      if (c === mode.q) { stack.pop(); out += c; k++; continue; }
      if (c === '$' && txt[k + 1] === '{') { stack.push({ type: 'interp' }); out += '${'; k += 2; continue; }
      out += c; k++; continue;
    }
    // interp : l'interieur de `${…}` est du code — les quotes y ouvrent des
    // literals independants de la chaine hote.
    if (c === '}') { stack.pop(); out += c; k++; continue; }
    if (OPEN[c]) { stack.push({ type: 'grp' }); out += c; k++; continue; }
    if (c === "'" || c === '"' || c === '`') { stack.push({ type: 'str', q: c }); out += c; k++; continue; }
    if (c === '$' && txt[k + 1] === '{') { stack.push({ type: 'interp' }); out += '${'; k += 2; continue; }
    out += c; k++;
  }
  return out;
}

/** Contenu d'un literal ('...', "...", `...`) ou null si ce n'est pas un
 *  literal (variable, appel de fonction, …). */
function quotedContent(s) {
  const t = s.trim();
  const q = t[0];
  if ((q === "'" || q === '"' || q === '`') && t.length >= 2 && t[t.length - 1] === q) {
    return t.slice(1, -1);
  }
  return null;
}

/** Chemin brut d'un argument : literal, template, ou concatenation de
 *  literals et d'expressions (chaque expression non littérale vaut {X}). */
function argToPath(arg) {
  const parts = splitTopLevel(arg, '+');
  if (parts.length > 1) {
    let out = '';
    for (const p of parts) {
      const q = quotedContent(p);
      out += q === null ? '{X}' : q;
    }
    return out.replace(/\/{2,}/g, '/');
  }
  return quotedContent(arg);
}

/** Decoupe sur un separateur en ignorant ceux contenus dans les quotes,
 *  parentheses, crochets et gabarits `${}`. */
function splitTopLevel(s, sep) {
  const out = [];
  let depth = 0, quote = null, cur = '';
  for (let k = 0; k < s.length; k++) {
    const c = s[k];
    if (quote) {
      cur += c;
      if (c === '\\') { cur += s[++k] ?? ''; continue; }
      if (c === quote) quote = null;
      continue;
    }
    if (c === "'" || c === '"' || c === '`') { quote = c; cur += c; continue; }
    if (OPEN[c]) { depth++; cur += c; continue; }
    if (CLOSE[c]) { if (depth > 0) depth--; cur += c; continue; }
    if (c === sep && depth === 0) { out.push(cur); cur = ''; continue; }
    cur += c;
  }
  out.push(cur);
  return out;
}

function norm(p) {
  return p
    .replace(/\$\{[^{}]*\}/g, '{X}')       // interpolations completes
    .replace(/\$\{[^}]*\}*/g, '{X}')       // interpolations non fermées
    .replace(/\$[a-zA-Z_][a-zA-Z0-9_[\].]*/g, '{X}')
    .replace(/\{\}/g, '{X}')
    .replace(/'/g, '');                    // quotes residuelles dans ${x['k']}
}

// Declaration explicite d'un jeu de routes pour un chemin dynamique :
//   // audit-routes: /transfers/{id}/submit, /transfers/{id}/cancel
// Verifiee contre le backend : elle ne peut pas couvrir une route morte.
const RE_DECLARED = /audit-routes:\s*(.+)$/;
function declaredRoutes(lines, idx) {
  for (let k = Math.max(0, idx - 8); k <= Math.min(lines.length - 1, idx + 2); k++) {
    const m = lines[k].match(RE_DECLARED);
    if (!m) continue;
    return m[1].split(',').map((s) => s.trim().replace(/\{[^}]*\}/g, '{X}')).filter(Boolean);
  }
  return null;
}

function scan(roots, exts) {
  const bad = [];
  const files = [];
  for (const r of roots) walk(path.join(ROOT, r), (n) => exts.some((e) => n.endsWith(e)), files);
  for (const fp of files) {
    const txt = fs.readFileSync(fp, 'utf8');
    // split sur /\r?\n/ : les sources sont en CRLF sous Windows. Un split
    // sur '\n' seul laissait un '\r' en fin de ligne, empechant RE_DECLARED
    // (. n'avale pas \r et $ sans flag m exige la vraie fin) de jamais matcher
    // une declaration « // audit-routes: » — les declarations etaient
    // silencieusement ignorees. Le compte de lignes (lineIdx via split('\n'))
    // reste identique car chaque separateur contient exactement un '\n'.
    const lines = txt.split(/\r?\n/);
    // Resolution des bases locales Dart (`final _base = '/health'`) : sans
    // cela, '$_base/consultations/$id' echappait a l'audit (ne commence pas
    // par /) — trou de couverture reel corrige ici.
    const decls = {};
    for (const d of txt.matchAll(/(?:final|const|late final)\s+(_\w*[Bb]ase\w*)\s*=\s*'([^']+)'/g)) {
      if (d[2].startsWith('/')) decls[d[1]] = d[2].replace(/\/+$/, '');
    }
    for (const m of txt.matchAll(RE_CALL_HEAD)) {
      const raw0 = argToPath(readArg(txt, m.index + m[0].length));
      if (raw0 == null) continue;                          // argument = variable seule, non resolvable
      let raw = raw0;
      for (const [k, v] of Object.entries(decls)) {
        raw = raw.replaceAll('${' + k + '}', v).replaceAll('$' + k, v);
      }
      if (!raw.startsWith('/')) continue;                  // chemins relatifs a une base deja auditee
      const lineIdx = txt.slice(0, m.index).split('\n').length - 1;
      const line = lineIdx + 1;
      const loc = `${path.relative(ROOT, fp)}:${line}`;

      // 1. declaration audit-routes : le jeu de routes doit exister cote backend.
      const declared = declaredRoutes(lines, lineIdx);
      if (declared) {
        const missing = declared.filter((d) => !exists(d));
        if (!missing.length) continue;
        bad.push({ path: norm(raw), loc, reason: `audit-routes DECLAREES MAIS ABSENTES: ${missing.join(', ')}` });
        continue;
      }

      const variants = variantsOf(raw);
      if (variants.some((v) => exists(v))) continue;
      // Aucune route exacte : un literal avale par une route /{id} est une
      // route morte cote serveur (Spring appellera {id} avec ce literal).
      const shadow = variants.map(isShadowedLiteral).find(Boolean);
      if (shadow) {
        bad.push({ path: norm(raw), loc, reason: `aucune route exacte — seul ${shadow} matche et son {id} avalerait ce literal` });
        continue;
      }
      const specOnly = variants.some((v) => existsOnlySpec(v));
      bad.push({ path: norm(raw), loc, specOnly });
    }
  }
  return bad;
}

// Variants concrets d'un chemin client : chaque ${...} devient soit {X}
// (identifiant dynamique), soit ses litteraux s'il en contient (ternaire
// Dart '${joined ? 'leave' : 'join'}' -> join | leave). Produit cartesian
// borne (2 choices max par atome, 4 atomes max).
function variantsOf(raw) {
  let acc = [''];
  const re = /(\$\{(?:[^{}]|\{[^{}]*\})*\}|\$[a-zA-Z_][a-zA-Z0-9_]*)/g;
  let last = 0;
  for (const m of raw.matchAll(re)) {
    // Interpolation collée a un segment (caractere precedent != '/') : la
    // suite n'est pas du chemin (query string '.../sermons${qs}', concat).
    // Le chemin s'arrete avant l'atome — on conserve le prefixe et on retourne.
    const beforeChar = m.index > 0 ? raw[m.index - 1] : '';
    if (beforeChar && beforeChar !== '/') {
      return acc.map((p) => p + raw.slice(last, m.index));
    }
    acc = acc.map((p) => p + raw.slice(last, m.index));
    const atom = m[0];
    // Un ternaire `${c ? 'a' : 'b'}` : les litteraux sont les variantes.
    // RIEN d'autre ne doit creer de variante — `${x['id'] ?? 'general'}`
    // (indexage + valeur de repli) n'est pas un choix, et le deviner par
    // « 2 quotes + un point d'interrogation » fabrique de faux chemins.
    const choice = atom.match(/\?\s*'([^']*)'\s*:\s*'([^']*)'/)
      || atom.match(/\?\s*"([^"]*)"\s*:\s*"([^"]*)"/);
    const opts = choice ? [choice[1], choice[2]] : ['{X}'];
    acc = acc.flatMap((p) => opts.map((o) => p + o));
    last = m.index + atom.length;
  }
  acc = acc.map((p) => p + raw.slice(last));
  return acc;
}

/* ---------- 3. rapport ---------- */

// --dump-routes [filtre] : debug, liste les routes code parsees.
if (process.argv.includes('--dump-routes')) {
  const filtre = process.argv[process.argv.indexOf('--dump-routes') + 1] || '';
  for (const r of [...codeRoutes].filter((r) => r.includes(filtre)).sort()) console.log(r);
  process.exit(0);
}

// --probe <chemin> : debug, montre la couverture reelle d'un candidat.
if (process.argv.includes('--probe')) {
  const cand = process.argv[process.argv.indexOf('--probe') + 1];
  const m = matchIn(codeIdx, cand);
  console.log(JSON.stringify({
    cand, code: !!m.exact, via: m.exact || m.shadow || null,
    shadowed: !!isShadowedLiteral(cand), specOnly: existsOnlySpec(cand),
  }));
  process.exit(0);
}

let total = 0;
for (const [name, roots, exts] of [
  ['MOBILE', ['mobile/lib'], ['.dart']],
  ['FRONTEND', ['frontend/src'], ['.ts', '.tsx']],
]) {
  const bad = scan(roots, exts);
  const byPath = new Map();
  for (const b of bad) {
    if (!byPath.has(b.path)) byPath.set(b.path, []);
    byPath.get(b.path).push(b);
  }
  console.log(`== ${name}: ${bad.length} sites, ${byPath.size} chemins distincts sans route`);
  for (const p of [...byPath.keys()].sort()) {
    const entries = byPath.get(p);
    const flag = entries.every((e) => e.specOnly) ? '   [SPEC-ONLY: present dans openapi.json mais PAS dans le code]' : '';
    const reason = entries.map((e) => e.reason).find(Boolean);
    console.log(`   ${p}   (${entries.slice(0, 2).map((e) => e.loc).join(', ')})${flag}${reason ? ` — ${reason}` : ''}`);
  }
  total += byPath.size;
}
process.exit(total === 0 ? 0 : 1);
