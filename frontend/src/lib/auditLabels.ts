/**
 * Humanisation du fil d'activité (audit) — le dashboard du pasteur doit lire
 * « Franck a créé une famille », pas « CREER_FAMILY » ni un UUID d'acteur.
 * Les actions backend sont des constantes UPPER_SNAKE : on traduit le verbe
 * (préfixe) et la cible (suffixe ou entiteType), avec repli lisible.
 */

const VERBE_LABELS: Record<string, string> = {
  CREER: 'a créé',
  CREATE: 'a créé',
  CREATED: 'a créé',
  MODIFIER: 'a modifié',
  UPDATE: 'a mis à jour',
  UPDATED: 'a mis à jour',
  SUPPRIMER: 'a supprimé',
  DELETE: 'a supprimé',
  DELETED: 'a supprimé',
  ARCHIVE: 'a archivé',
  ARCHIVED: 'a archivé',
  TRANSFERT: 'a transféré',
  TRANSFER: 'a transféré',
  EXPORT: 'a exporté',
  IMPORT: 'a importé',
  LOGIN: 's\'est connecté',
  LOGOUT: 's\'est déconnecté',
  CONNECT: 's\'est connecté',
  ACCEPTED: 'a accepté',
  APPROVED: 'a approuvé',
  REJECTED: 'a refusé',
  CANCELLED: 'a annulé',
  SENT: 'a envoyé',
  COMPLETED: 'a terminé',
  REGISTERED: 's\'est inscrit à',
  ANSWERED: 'a exaucé',
  SELF_SERVICE_CREATED: 'a créé',
};

const ENTITE_LABELS: Record<string, string> = {
  USER: 'un membre',
  FAMILY: 'une famille',
  FAMILLE: 'une famille',
  DEPARTMENT: 'un département',
  PRAYER: 'une prière',
  EVENT: 'un événement',
  VISIT: 'une visite pastorale',
  EVALUATION: 'une évaluation',
  TASK: 'une tâche',
  SOUL: 'une âme',
  DISCIPLE: 'un disciple',
  INVITATION: 'une invitation',
  TENANT: 'l\'église',
  TENANT_SETTINGS: 'les réglages de l\'église',
  ORGANIZATION_NODE: 'une structure',
  ORG_NODE: 'une structure',
  FINANCE_TRANSACTION: 'une transaction',
  WORKFLOW_DEFINITION: 'un flux de travail',
  WORKFLOW_STEP: 'une étape de flux',
  INVENTORY_ITEM: 'un article',
  DOCUMENT: 'un document',
  MEDIA: 'un média',
  REPORT: 'un rapport',
  WEBHOOK: 'un webhook',
  DICTIONARY: 'un dictionnaire',
  MEMBERSHIP: 'une adhésion',
  ATTENDANCE: 'une présence',
};

/** « JOIN_CODE_UPDATED » → verbe + cible extraits du nom d'action lui-même. */
function splitAction(action: string): { verb: string; target: string } {
  const parts = action.split('_');
  // Verbe en premier (CREER_...) ou en dernier (..._UPDATED) : on teste les deux.
  const head = parts[0].toUpperCase();
  if (VERBE_LABELS[head]) {
    return { verb: VERBE_LABELS[head], target: parts.slice(1).join('_') };
  }
  const tail = parts[parts.length - 1].toUpperCase();
  if (VERBE_LABELS[tail]) {
    return { verb: VERBE_LABELS[tail], target: parts.slice(0, -1).join('_') };
  }
  // Repli sur l'action entière (ex. « LOGIN », « UPDATE »).
  if (parts.length === 1 && VERBE_LABELS[head]) {
    return { verb: VERBE_LABELS[head], target: '' };
  }
  return { verb: action.toLowerCase().replace(/_/g, ' '), target: '' };
}

function labelEntite(entiteType?: string | null): string {
  if (!entiteType) return '';
  return ENTITE_LABELS[entiteType.toUpperCase()] || entiteType.toLowerCase().replace(/_/g, ' ');
}

/** Phrase lisible : « a créé une famille » (sans sujet). */
export function humanizeAuditAction(action?: string | null, entiteType?: string | null): string {
  if (!action) return 'a effectué une action';
  const { verb, target } = splitAction(action.toUpperCase());
  let objet = target ? ENTITE_LABELS[target] : undefined;
  if (!objet && target && target !== entiteType?.toUpperCase()) {
    objet = target.toLowerCase().replace(/_/g, ' ');
  }
  if (!objet) objet = labelEntite(entiteType);
  return objet ? `${verb} ${objet}` : verb;
}

/** Libellé d'entité seul, pour la ligne de détail. */
export function humanizeEntite(entiteType?: string | null): string {
  return labelEntite(entiteType);
}

/** Temps relatif court : « il y a 5 min », « hier », « 3 j ». */
export function relativeTimeFr(iso: string): string {
  const then = new Date(iso).getTime();
  if (Number.isNaN(then)) return '';
  const min = Math.floor((Date.now() - then) / 60_000);
  if (min < 1) return "à l'instant";
  if (min < 60) return `il y a ${min} min`;
  const h = Math.floor(min / 60);
  if (h < 24) return `il y a ${h} h`;
  const d = Math.floor(h / 24);
  if (d === 1) return 'hier';
  if (d < 30) return `il y a ${d} j`;
  return new Date(iso).toLocaleDateString('fr-FR', { day: 'numeric', month: 'short' });
}
