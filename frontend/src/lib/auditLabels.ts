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
  ADDED: 'a ajouté',
  JOINED: 'a rejoint',
  LEFT: 'a quitté',
  RESTORED: 'a restauré',
  PURGED: 'a effacé',
  ASSIGNED: 'a assigné',
  CLOSED: 'a clôturé',
  OPENED: 'a ouvert',
  PAID: 'a payé',
  REFUNDED: 'a remboursé',
  STARTED: 'a démarré',
  SUBMITTED: "a soumis",
  VERIFIED: 'a vérifié',
  INVITED: 'a invité',
  IMPORTED: 'a importé',
  RESENT: 'a renvoyé',
  REISSUED: 'a réémis',
};

/**
 * Tokens verbe reconnus par `splitAction` : des mots seuls uniquement. Une
 * clé multi-tokens ne peut jamais être atteinte par la découpe sur « _ » —
 * elle serait morte ; ces actions-là passent par ACTION_LABELS.
 */
const VERBE_TOKENS = Object.keys(VERBE_LABELS).filter((c) => !c.includes('_'));

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
  JOIN_REQUEST: "une demande d'adhésion",
  TENANT_ORG_CHILD: 'une structure',
  TENANT_JOIN_CODE: "un code d'adhésion",
  JOIN_CODE: "un code d'adhésion",
  INVITATION_REMINDER: "un rappel d'invitation",
  TENANT_MEMBERS: 'des membres',
  TENANT_STRUCTURE: 'une structure',
  TENANT_ONBOARDING: "l'installation de l'église",
  TENANT_REGISTRATION: "l'enregistrement de l'église",
  MEMBER: 'un membre',
  BRANDING: "l'identité visuelle",
};

/**
 * Cas particuliers où le nom d'action lui-même porte le sens complet :
 * un verbe multi-tokens ou une qualification finale (« …_BY_LINK ») ne se
 * décompose pas proprement, et le rendu littéral afficherait de l'anglais
 * au pasteur.
 */
const ACTION_LABELS: Record<string, string> = {
  JOIN_REQUEST_CREATED_BY_LINK: "a créé une demande d'adhésion par lien",
  TENANT_SELF_SERVICE_CREATED: "a créé l'église en libre-service",
  MEMBER_JOINED_BY_CODE: "a rejoint l'église par code",
  JOIN_REQUEST_INVITATION_RESENT: 'a renvoyé une invitation',
  TENANT_ONBOARDING_BRANDING_UPDATED: "a mis à jour l'identité visuelle de l'église",
  TENANT_ONBOARDING_FIRST_EVENT_CREATED: "a créé le premier événement de l'église",
  TENANT_ONBOARDING_CHURCH_IDENTITY: "a défini l'identité de l'église",
  TENANT_ROLES_INVITED: 'a invité des membres à des rôles',
};

/** « JOIN_CODE_UPDATED » → verbe + cible extraits du nom d'action lui-même. */
function splitAction(action: string): { verb: string | null; target: string } {
  const parts = action.split('_').filter(Boolean);
  // Le verbe peut être n'importe où dans le nom : on cherche le DERNIER verbe
  // reconnu en partant de la fin (les tokens qui le suivent sont des
  // qualificatifs — « …_CREATED_BY_LINK » — et se perdent dans le libellé).
  for (let i = parts.length - 1; i >= 0; i--) {
    if (VERBE_TOKENS.includes(parts[i])) {
      return { verb: VERBE_LABELS[parts[i]], target: parts.slice(0, i).join('_') };
    }
  }
  // Aucun verbe reconnu : on ne rendra PAS le code en anglais littéral.
  return { verb: null, target: parts.join('_') };
}

/**
 * Libellé d'entité : uniquement ce qui est répertorié. Un type inconnu rend
 * '' plutôt que son jargon technique, pour que rien d'anglais ne paraisse.
 */
function labelEntite(entiteType?: string | null): string {
  if (!entiteType) return '';
  return ENTITE_LABELS[entiteType.toUpperCase()] || '';
}

/** Phrase lisible : « a créé une famille » (sans sujet). */
export function humanizeAuditAction(action?: string | null, entiteType?: string | null): string {
  if (!action) return 'a effectué une action';
  const code = action.toUpperCase();
  if (ACTION_LABELS[code]) return ACTION_LABELS[code];
  const { verb, target } = splitAction(code);
  // Cible : d'abord ce que le nom d'action porte lui-même, puis le type
  // d'entité déclaré par le backend.
  const objet = (target ? ENTITE_LABELS[target] : undefined) || labelEntite(entiteType);
  if (!verb) {
    // Verbe non répertorié : une formulation française neutre vaut mieux que
    // l'anglais littéral du nom d'action (« join request created by link »).
    return objet ? `action sur ${objet}` : 'a effectué une action';
  }
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
