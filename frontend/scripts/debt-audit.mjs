#!/usr/bin/env node
// Dette technique frontend — ratchet de non-régression (additif).
//
// Pourquoi ce script existe : la dette frontend (`any`, fichiers > 1000 lignes,
// `console.log` résiduels, TODO/FIXME) était invisible et non mesurée. Ce script
// la rend VISIBLE et garantit qu'elle NE GROSSIT PAS : c'est un « cliquet » —
// chaque amélioration fait baisser la référence, aucune PR ne peut l'augmenter.
//
// Ce qu'il mesure (sur frontend/src, hors *.test.* / *.d.ts) :
//   1. `any` explicites (`: any`, `as any`, `<any>`) — dette de typage ;
//   2. fichiers de plus de LARGE_LIGNES lignes — dette de découpage ;
//   3. `TODO|FIXME|HACK` — dette de finition ;
//   4. `console.*` hors error/warn/info — dette de journalisation.
//
// Sortie : rapport texte + code de sortie 1 si une métrique DÉPASSE la
// référence versionnée (utilisable en CI). `--update-baseline` écrit la
// référence (à lancer une fois, et à chaque amélioration volontaire).
//
// Usage :
//   node scripts/debt-audit.mjs                 # rapport + exit 1 si régression
//   node scripts/debt-audit.mjs --json          # rapport machine
//   node scripts/debt-audit.mjs --update-baseline
import { readFileSync, writeFileSync, existsSync, readdirSync, statSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import { dirname, join, extname } from 'node:path';

const HERE = dirname(fileURLToPath(import.meta.url));
const SRC_DIR = join(HERE, '..', 'src');
const BASELINE_PATH = join(HERE, 'debt-audit-baseline.json');

const LARGE_FILE_LINES = 1000;   // seuil « fichier trop gros »
const CODE_EXT = new Set(['.ts', '.tsx']);
const asJson = process.argv.includes('--json');
const update = process.argv.includes('--update-baseline');

/** Récupère récursivement les fichiers de code (hors tests/déclarations). */
function collect(dir, acc = []) {
  for (const entry of readdirSync(dir)) {
    const full = join(dir, entry);
    const st = statSync(full);
    if (st.isDirectory()) {
      if (entry === 'node_modules' || entry === 'generated') continue;
      collect(full, acc);
    } else if (CODE_EXT.has(extname(entry)) && !entry.endsWith('.test.ts')
               && !entry.endsWith('.test.tsx') && !entry.endsWith('.d.ts')) {
      acc.push(full);
    }
  }
  return acc;
}

function measure() {
  const files = collect(SRC_DIR);
  let anyCount = 0;
  let todoCount = 0;
  let consoleCount = 0;
  const largeFiles = [];
  for (const file of files) {
    const text = readFileSync(file, 'utf8');
    const lines = text.split('\n');
    const rel = file.slice(SRC_DIR.length + 1);
    if (lines.length > LARGE_FILE_LINES) {
      largeFiles.push({ file: rel, lines: lines.length });
    }
    for (const line of lines) {
      if (/:\s*any\b|\bas\s+any\b|<any>/.test(line)) anyCount++;
      if (/\bTODO\b|\bFIXME\b|\bHACK\b/.test(line)) todoCount++;
      if (/console\.(log|debug|trace)\(/.test(line)) consoleCount++;
    }
  }
  largeFiles.sort((a, b) => b.lines - a.lines);
  return {
    generatedAt: new Date().toISOString(),
    thresholds: { largeFileLines: LARGE_FILE_LINES },
    metrics: {
      anyCount,
      largeFileCount: largeFiles.length,
      todoCount,
      consoleDebugCount: consoleCount,
    },
    largeFiles,
  };
}

function loadBaseline() {
  if (!existsSync(BASELINE_PATH)) return null;
  return JSON.parse(readFileSync(BASELINE_PATH, 'utf8'));
}

function printText(report, baseline) {
  const m = report.metrics;
  const out = [];
  out.push('Dette technique frontend (ratchet de non-régression)');
  out.push('======================================================');
  const fmt = (key, label) => {
    if (!baseline) return `  ${label.padEnd(28)} ${m[key]}`;
    const before = baseline.metrics[key] ?? 0;
    const delta = m[key] - before;
    const arrow = delta > 0 ? `+${delta} ⚠` : delta < 0 ? `${delta} ✅` : '=';
    return `  ${label.padEnd(28)} ${String(m[key]).padStart(5)}  (réf ${before}, ${arrow})`;
  };
  out.push(fmt('anyCount', '`any` explicites'));
  out.push(fmt('largeFileCount', `fichiers > ${LARGE_FILE_LINES} lignes`));
  out.push(fmt('todoCount', 'TODO/FIXME/HACK'));
  out.push(fmt('consoleDebugCount', 'console.log/debug/trace'));
  if (report.largeFiles.length) {
    out.push('  Fichiers les plus gros :');
    for (const f of report.largeFiles.slice(0, 8)) out.push(`    ${f.lines} l.  ${f.file}`);
  }
  process.stdout.write(out.join('\n') + '\n');
}

function main() {
  const report = measure();
  if (asJson) {
    process.stdout.write(JSON.stringify(report, null, 2) + '\n');
  } else {
    const baseline = loadBaseline();
    printText(report, baseline);
  }

  if (update) {
    writeFileSync(BASELINE_PATH, JSON.stringify(report, null, 2) + '\n');
    process.stdout.write(`\nRéférence écrite dans ${BASELINE_PATH}\n`);
    return;
  }

  const baseline = loadBaseline();
  if (!baseline) {
    process.stdout.write(
      '\nNote : aucune référence versionnée. Lancez `--update-baseline` pour la créer.\n',
    );
    return;
  }
  // Ratchet : toute métrique qui AUGMENTE fait échouer (utilisable en CI).
  const regressions = [];
  for (const key of Object.keys(report.metrics)) {
    const before = baseline.metrics[key] ?? 0;
    const now = report.metrics[key];
    if (now > before) regressions.push(`${key}: ${before} → ${now}`);
  }
  if (regressions.length) {
    process.stderr.write(`\nÉCHEC — régression de dette frontend :\n  ${regressions.join('\n  ')}\n`);
    process.exit(1);
  }
  process.stdout.write('\nOK — aucune régression de dette (ratchet respecté).\n');
}

main();
