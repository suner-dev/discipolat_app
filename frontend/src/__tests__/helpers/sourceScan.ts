/**
 * Neutralise les commentaires TS/JS en préservant la numérotation des lignes.
 *
 * Indispensable pour le ratchet paramétrisation : sans cela, une clé citée
 * dans une phrase de commentaire (`@authz.can('X')`) est prise pour une clé
 * CONSOMMÉE, et une garde décrite en commentaire est prise pour une garde
 * résiduelle — le ratchet devient bruyant puis faux.
 */
export function stripComments(source: string): string {
  let out = '';
  let i = 0;
  const n = source.length;
  let state: 'code' | 'line' | 'block' | 'sq' | 'dq' | 'tpl' = 'code';
  while (i < n) {
    const c = source[i];
    const next = source[i + 1];
    if (state === 'code') {
      if (c === '/' && next === '/') { state = 'line'; out += '  '; i += 2; continue; }
      if (c === '/' && next === '*') { state = 'block'; out += '  '; i += 2; continue; }
      if (c === "'") { state = 'sq'; out += c; i++; continue; }
      if (c === '"') { state = 'dq'; out += c; i++; continue; }
      if (c === '`') { state = 'tpl'; out += c; i++; continue; }
      out += c; i++; continue;
    }
    if (state === 'line') {
      if (c === '\n') { state = 'code'; out += '\n'; } else { out += ' '; }
      i++; continue;
    }
    if (state === 'block') {
      if (c === '*' && next === '/') { state = 'code'; out += '  '; i += 2; continue; }
      out += c === '\n' ? '\n' : ' ';
      i++; continue;
    }
    // chaînes : on les recopie telles quelles (le contenu compte pour les clés)
    if (c === '\\') { out += source.slice(i, i + 2); i += 2; continue; }
    if ((state === 'sq' && c === "'") || (state === 'dq' && c === '"') || (state === 'tpl' && c === '`')) {
      state = 'code';
    }
    out += c; i++;
  }
  return out;
}

/** Texte de l'expression qui suit `from`, parenthèses crochets accolades équilibrés. */
export function balancedFrom(source: string, from: number): string {
  let depth = 0;
  let instr: string | null = null;
  for (let i = from; i < source.length; i++) {
    const c = source[i];
    if (instr) {
      if (c === '\\') { i++; continue; }
      if (c === instr) instr = null;
      continue;
    }
    if (c === "'" || c === '"' || c === '`') { instr = c; continue; }
    if (c === '(' || c === '[' || c === '{') depth++;
    else if (c === ')' || c === ']' || c === '}') {
      if (depth === 0) return source.slice(from, i);
      depth--;
    }
  }
  return source.slice(from);
}
