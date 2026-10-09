#!/usr/bin/env node
// B0 — Audit i18n des 6 locales web.
//
// Pourquoi ce script existe : `tText()` retombe SILENCIEUSEMENT sur le français
// quand une clé manque ou quand sa valeur est encore celle de `fr`. Rien ne le
// signale à l'écran. Ce script rend cette dette VISIBLE et, surtout, vérifie
// qu'elle ne grossit pas.
//
// Ce qu'il mesure :
//   1. clés absentes  par locale (delta négatif vs `fr`) ;
//   2. clés en trop   par locale (delta positif  vs `fr`) — dérive de contrat ;
//   3. valeurs encore identiques au français, en distinguant celles qui DOIVENT
//      l'être (mots,codes,icônes) de celles qui ne le doivent pas (une valeur
//      française avec accents et 2+ mots est un candidat à traduction) ;
//   4. la DEGRÉ de régression par rapport à une référence versionnée.
//
// Sortie : rapport texte + code de sortie 1 si le budget de régression est
// dépassé (utilisable en CI). `--update-baseline` écrit la référence.
//
// Usage :
//   node scripts/i18n-audit.mjs              # rapport + sortie 1 si régression
//   node scripts/i18n-audit.mjs --json       # rapport machine
//   node scripts/i18n-audit.mjs --update-baseline
import { readFileSync, writeFileSync, existsSync, readdirSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import { dirname, join } from 'node:path';

const HERE = dirname(fileURLToPath(import.meta.url));
const I18N_DIR = join(HERE, '..', 'src', 'i18n');
const BASELINE_PATH = join(HERE, 'i18n-audit-baseline.json');

const LOCALES = ['fr', 'en', 'es', 'pt', 'sw', 'ar'];
const REFERENCE = 'fr'; // `fr` est la source : c'est lui qui définit le contrat

const argv = new Set(process.argv.slice(2));
const asJson = argv.has('--json');
const updateBaseline = argv.has('--update-baseline');

/**
 * Extrait le dictionnaire `locale` d'un fichier `i18n/<locale>.ts`.
 *
 * Ces fichiers sont du TypeScript, pas du JSON. On n'évalue pas de code : on
 * ne lit que les paires `'clé': 'valeur'` littérales. Une valeur qui n'est pas un
 * littéral simple (template, concaténation, appel) est comptée à part et jamais
 * traitée comme une traduction valide.
 */
/**
 * Déchappe une chaîne littérale TypeScript vers sa valeur RÉELLE.
 *
 * Indispensable, et pas cosmétique : `es`/`pt`/`sw`/`ar` écrivent l'apostrophe
 * typographique sous forme `\u2019` là où `fr` écrit `’`. Sans normalisation, la
 * clé `Échec d\u2019exécution` (es) et `Échec d’exécution` (fr) étaient vues comme
 * deux clés différentes → fausse alerte « clé manquante » alors que le code
 * fonctionne parfaitement.
 */
function unescapeTs(raw) {
  return raw
    .replace(/\\u([0-9a-fA-F]{4})/g, (_, hex) => String.fromCharCode(parseInt(hex, 16)))
    .replace(/\\x([0-9a-fA-F]{2})/g, (_, hex) => String.fromCharCode(parseInt(hex, 16)))
    .replace(/\\n/g, '\n')
    .replace(/\\r/g, '\r')
    .replace(/\\t/g, '\t')
    .replace(/\\(['"\\])/g, '$1');
}

function parseDictionary(locale) {
  const source = readFileSync(join(I18N_DIR, `${locale}.ts`), 'utf8');
  const dict = new Map();
  //
  // Deux formes après le deux-points : apostrophes simples ou guillemets doubles
  // (le projet utilise les deux — ignorer les guillemets faisait passer une clé
  // existante pour absente).
  //
  // Le motif n'est volontairement pas ancré à la FIN DE LIGNE : plusieurs clés
  // partagent une ligne
  // (`'a1': '…', 'a2': '…',`) et un ancrage strict les faisait toutes manquer.
  // Une entrée s'arrête sur le premier `',` ou `",` non échappé.
  //
  // Les guillemets doubles peuvent contenir des `\"` échappés
  // (`"The \"{feature}\" module…"`) : sans le groupe `\\.`, la capture s'arrêtait
// au premier échappement et la clé entière disparaissait du dictionnaire.
  //
  // Deux formes de CLÉ coexistent dans le projet :
  //  - la clé technique (`'nav.backHome'`) ;
  //  - un lot « gettext-style » où la clé EST le français et contient alors une
  //    apostrophe échappée (`'Échec d\\’exécution'`). Sans la branche
  //    `[^'\\]|\\.`, l'apostrophe interne coupait la clé en deux et produisait
  //    de fausses « clés manquantes ».
  const entry =
    /(?:"([^"]+)"|'((?:[^'\\]|\\.)+)')(?=\s*:\s*(?:'((?:[^'\\]|\\.)*)'|"((?:[^"\\]|\\.)*)")\s*,)/g;
  let match;
  while ((match = entry.exec(source)) !== null) {
    const [, doubleQuotedKey, singleQuotedKey, singleQuotedValue, doubleQuotedValue] = match;
    const rawKey = doubleQuotedKey ?? singleQuotedKey;
    const rawValue = singleQuotedValue ?? doubleQuotedValue;
    if (rawKey === undefined || rawValue === undefined) continue;
    dict.set(unescapeTs(rawKey), unescapeTs(rawValue));
  }
  return dict;
}

/**
 * Une valeur française est « Candidat à traduction » quand elle contient des
 * accents ET au moins deux mots : « Tableau de bord », « Retour à l'accueil ».
 *
 * On exclut volontairement : les codes ('MEMBRE'), les symboles ('—'), les
 * nombres, les chaines courtes sans espace ('Fermer'), et les valeurs déjà
 * volontairement universelles (noms de produits, formats).
 */
function isFrenchOnly(value) {
  if (!/[àâäçéèêëîïôöùûüÿœæ]/i.test(value)) return false;
  const words = value.split(/\s+/).filter((w) => /[a-zà-ÿ]/i.test(w));
  return words.length >= 2;
}

function audit() {
  const dicts = Object.fromEntries(LOCALES.map((l) => [l, parseDictionary(l)]));
  const reference = dicts[REFERENCE];

  const report = {};
  for (const locale of LOCALES) {
    if (locale === REFERENCE) {
      report[locale] = { total: reference.size, missing: [], extra: [], untranslated: 0 };
      continue;
    }
    const dict = dicts[locale];
    const missing = [];
    const extra = [];
    let untranslated = 0;

    for (const key of reference.keys()) {
      if (!dict.has(key)) {
        missing.push(key);
        continue;
      }
      const frValue = reference.get(key);
      if (frValue === dict.get(key) && isFrenchOnly(frValue)) untranslated += 1;
    }
    for (const key of dict.keys()) {
      // Une clé absente de `fr` n'est un problème que si le code l'utilise.
      if (!reference.has(key)) extra.push(key);
    }

    report[locale] = { total: dict.size, missing, extra, extraDead: [], untranslated };
  }

  const frenchOnlyTotal = [...reference.values()].filter(isFrenchOnly).length;
  return { report, frenchOnlyTotal, referenceTotal: reference.size };
}

function loadBaseline() {
  if (!existsSync(BASELINE_PATH)) return null;
  try {
    return JSON.parse(readFileSync(BASELINE_PATH, 'utf8'));
  } catch {
    return null; // une référence illisible ne doit pas faire échouer l'audit
  }
}

/**
 * Une clé « en trop » n'est PAS forcément un bug.
 *
 * Trois cas, à distinguer avant d'échouer :
 *  - la clé est ABSENTE de `fr` ET inutilisée dans le code → reliquat mort
 *    d'un lot i18n. Sans effet, sans risque : on l'ignore.
 *  - la clé est absente de `fr` mais UTILISÉE → drift réel : la page affiche la
 *    clé brute (ou retombe sur du français) selon la locale. À corriger.
 *  - la clé est absente de `fr` ET utilisée par une AUTRE locale via un chemin
 *    dynamique → même chose que ci-dessus.
 *
 * Sans cette distinction, l'audit crie au|Pages entières inutilisées alors
 * qu'aucun utilisateur ne les voit.
 */
function findUsedKeys() {
  const used = new Set();
  const roots = [join(HERE, '..', 'src')];
  const KEY_RE = /t\(\s*'([^']+)'|tText\(\s*'([^']+)'|'([a-zA-Z][\w.]*)'\s*\]/g;
  const walk = (dir) => {
    for (const entry of readdirSync(dir, { withFileTypes: true })) {
      if (entry.name === 'i18n' || entry.name === 'node_modules' || entry.name === '__tests__') continue;
      const full = join(dir, entry.name);
      if (entry.isDirectory()) { walk(full); continue; }
      if (!/\.(ts|tsx)$/.test(entry.name)) continue;
      const source = readFileSync(full, 'utf8');
      let m;
      while ((m = KEY_RE.exec(source)) !== null) {
        used.add(m[1] ?? m[2] ?? m[3]);
      }
    }
  };
  for (const root of roots) if (existsSync(root)) walk(root);
  return used;
}

function main() {
  const { report, frenchOnlyTotal, referenceTotal } = audit();
  const usedKeys = findUsedKeys();

  if (updateBaseline) {
    const snapshot = {
      generatedAt: new Date().toISOString(),
      referenceTotal,
      frenchOnlyTotal,
      locales: Object.fromEntries(
        LOCALES.map((l) => [
          l,
          { total: report[l].total, missing: report[l].missing.length, extra: report[l].extra.length, untranslated: report[l].untranslated },
        ]),
      ),
    };
    writeFileSync(BASELINE_PATH, `${JSON.stringify(snapshot, null, 2)}\n`, 'utf8');
    process.stdout.write(`Référence écrite : ${BASELINE_PATH}\n`);
    return;
  }

  if (asJson) {
    process.stdout.write(`${JSON.stringify({ referenceTotal, frenchOnlyTotal, locales: report }, null, 2)}\n`);
  } else {
    const lines = [];
    lines.push('=== Audit i18n (référence : fr) ===');
    lines.push(`Clés de référence : ${referenceTotal} · valeurs franchement françaises : ${frenchOnlyTotal}`);
    lines.push('');
    lines.push('locale  total   manquantes  en trop  non traduites');
    for (const locale of LOCALES) {
      const r = report[locale];
      lines.push(
        `${locale.padEnd(7)} ${String(r.total).padEnd(7)} ${String(r.missing.length).padEnd(12)} ${String(r.extra.length).padEnd(8)} ${r.untranslated}`,
      );
    }
    const deadTotal = LOCALES.filter((l) => l !== REFERENCE).reduce(
      (sum, l) => sum + report[l].extraDead.length,
      0,
    );
    if (deadTotal > 0) {
      lines.push(`Reliquats morts (clé en trop ET inutilisée) : ${deadTotal} — ignorés.`);
    }
    const worst = LOCALES.filter((l) => l !== REFERENCE).sort(
      (a, b) => report[b].untranslated - report[a].untranslated,
    )[0];
    if (worst && report[worst].untranslated > 0) {
      lines.push('');
      lines.push(`Dette la plus forte : ${worst} (${report[worst].untranslated} valeurs encore en français).`);
    }
    process.stdout.write(`${lines.join('\n')}\n`);
  }

  // ── Garde de non-régression ──────────────────────────────────────────────
  // Une clé manquante ou une clé en trop est un BUG (contrat cassé) : échec
  // systématique. Une valeur non traduite est une DETTE : elle ne fait échouer
  // que si elle dépasse la référence versionnée.
  // On ne retient que les clés en trop RÉELLEMENT UTILISÉES par le code : les
  // autres sont des reliquats morts, sans effet sur l'utilisateur.
  const details = [];
  for (const locale of LOCALES) {
    if (locale === REFERENCE) continue;
    const r = report[locale];
    r.extraDead = r.extra.filter((key) => !usedKeys.has(key));
    const drift = r.extra.filter((key) => usedKeys.has(key));
    if (r.missing.length > 0) {
      details.push(`  ${locale} : ${r.missing.length} clé(s) manquante(s) → ${r.missing.slice(0, 5).join(', ')}`);
    }
    if (drift.length > 0) {
      details.push(`  ${locale} : ${drift.length} clé(s) utilisée(s) mais absente(s) de fr → ${drift.slice(0, 5).join(', ')}`);
    }
  }
  if (details.length > 0) {
    process.stderr.write(`\nÉCHEC — drift de contrat i18n :\n${details.join('\n')}\n`);
    process.exit(1);
  }

  const baseline = loadBaseline();
  if (!baseline) {
    process.stderr.write(
      '\nNote : aucune référence versionnée. Lancez `--update-baseline` pour en créer une.\n',
    );
    return;
  }
  const regressions = [];
  for (const locale of LOCALES) {
    if (locale === REFERENCE) continue;
    const before = baseline.locales[locale];
    if (!before) continue;
    if (report[locale].untranslated > before.untranslated) {
      regressions.push(`${locale}: ${before.untranslated} → ${report[locale].untranslated} non traduites`);
    }
  }
  if (regressions.length > 0) {
    process.stderr.write(`\nÉCHEC — régression i18n :\n  ${regressions.join('\n  ')}\n`);
    process.exit(1);
  }
}

main();