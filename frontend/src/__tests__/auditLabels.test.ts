import { describe, it, expect } from 'vitest';
import { humanizeAuditAction, humanizeEntite, relativeTimeFr } from '@/lib/auditLabels';

/**
 * Le fil « Activité récente » du pasteur ne doit JAMAIS rendre un libellé
 * anglais littéral ni un code brut : les noms d'action backend varient
 * (verbe en tête, verbe en queue, verbe multi-tokens, qualification finale).
 * Les cas ci-dessous sont relevés sur la base de démonstration réelle.
 */
describe('auditLabels — humanisation du fil d\'activité', () => {
  it('traduit les actions dont le verbe est en tête', () => {
    expect(humanizeAuditAction('CREER_SOUL', 'SOUL')).toBe('a créé une âme');
    expect(humanizeAuditAction('MODIFIER_FAMILLE', 'FAMILY')).toBe('a modifié une famille');
    expect(humanizeAuditAction('PRAYER_CREATED', 'PRAYER')).toBe('a créé une prière');
  });

  it('traduit les actions dont le verbe est en queue', () => {
    expect(humanizeAuditAction('EVENT_UPDATED', 'EVENT')).toBe('a mis à jour un événement');
    expect(humanizeAuditAction('TASK_COMPLETED', 'TASK')).toBe('a terminé une tâche');
    expect(humanizeAuditAction('VISIT_CREATED', 'VISIT')).toBe('a créé une visite pastorale');
  });

  it('reconnait un verbe placé au milieu de l\'action', () => {
    // JOIN_REQUEST_CREATED_BY_LINK : le verbe n'est ni en tête ni en queue.
    expect(humanizeAuditAction('JOIN_REQUEST_CREATED_BY_LINK', 'TENANT'))
      .toBe("a créé une demande d'adhésion par lien");
  });

  it('ne rend jamais l\'anglais littéral d\'une action inconnue', () => {
    const rendu = humanizeAuditAction('TENANT_SELF_SERVICE_CREATED', 'TENANT');
    expect(rendu).not.toMatch(/self_service|self service/i);
    expect(rendu).toBe("a créé l'église en libre-service");

    // Action totalement inconnue : repli français sur l'entité déclarée —
    // le code brut ne doit jamais apparaître à l'écran.
    const inconnue = humanizeAuditAction('ZZZ_OVNI_DEPLOYE', 'PRAYER');
    expect(inconnue).toBe('action sur une prière');
    expect(inconnue).not.toMatch(/OVNI|DEPLOYE/);

    // Ni même quand l'entité elle-même est inconnue : on reste français.
    expect(humanizeAuditAction('ZZZ_OVNI_DEPLOYE', undefined)).not.toMatch(/DEPLOYE/);
  });

  it('conserve la cible quand le verbe seul ne la porte pas', () => {
    expect(humanizeAuditAction('UPDATE', 'USER')).toBe('a mis à jour un membre');
    expect(humanizeAuditAction('ORGANIZATION_NODE_CREATED', 'ORGANIZATION_NODE'))
      .toBe('a créé une structure');
    expect(humanizeAuditAction('WORKFLOW_STEP_ADDED', 'WORKFLOW_STEP'))
      .toBe('a ajouté une étape de flux');
  });

  it('humanise les entités et reste muet sans donnée', () => {
    expect(humanizeEntite('TENANT')).toBe("l'église");
    expect(humanizeEntite(undefined)).toBe('');
    expect(humanizeAuditAction(undefined, undefined)).toBe('a effectué une action');
  });

  it('formate le temps relatif en français', () => {
    const mins = (n: number) => new Date(Date.now() - n * 60_000).toISOString();
    expect(relativeTimeFr(mins(0))).toBe("à l'instant");
    expect(relativeTimeFr(mins(5))).toBe('il y a 5 min');
    expect(relativeTimeFr(mins(120))).toBe('il y a 2 h');
    expect(relativeTimeFr(mins(60 * 24 + 10))).toBe('hier');
    expect(relativeTimeFr('non-date')).toBe('');
  });
});

/**
 * Couples (action, entite_type) réellement présents dans la base de
 * démonstration (SELECT action, entite_type FROM audit_logs GROUP BY 1,2).
 * Aucun ne doit rendre du code brut ni de l'anglais au pasteur.
 */
const OBSERVEES: Array<[string, string]> = [
  ['TENANT_CREATED', 'TENANT'],
  ['ORGANIZATION_NODE_CREATED', 'ORGANIZATION_NODE'],
  ['ORG_NODE_CREATED', 'ORGANIZATION_NODE'],
  ['ORG_NODE_UPDATED', 'ORGANIZATION_NODE'],
  ['TENANT_SELF_SERVICE_CREATED', 'TENANT'],
  ['TENANT_ORG_CHILD_CREATED', 'TENANT'],
  ['TENANT_STRUCTURE_CREATED', 'TENANT'],
  ['USER_UPDATED', 'USER'],
  ['UPDATE', 'USER'],
  ['INVITATION_CREATED', 'INVITATION'],
  ['INVITATION_ACCEPTED', 'INVITATION'],
  ['INVITATION_REISSUED', 'INVITATION'],
  ['INVITATION_REMINDER_SENT', 'INVITATION'],
  ['FINANCE_TRANSACTION_CREATED', 'FINANCE_TRANSACTION'],
  ['WORKFLOW_STEP_ADDED', 'WORKFLOW_STEP'],
  ['WORKFLOW_DEFINITION_CREATED', 'WORKFLOW_DEFINITION'],
  ['INVENTORY_ITEM_CREATED', 'INVENTORY_ITEM'],
  ['JOIN_CODE_UPDATED', 'TENANT_JOIN_CODE'],
  ['JOIN_CODE_CREATED', 'TENANT_JOIN_CODE'],
  ['JOIN_REQUEST_CREATED_BY_LINK', 'TENANT'],
  ['JOIN_REQUEST_APPROVED', 'TENANT'],
  ['JOIN_REQUEST_APPROVED_INVITATION_ISSUED', 'TENANT'],
  ['JOIN_REQUEST_INVITATION_RESENT', 'TENANT'],
  ['MEMBER_JOINED_BY_CODE', 'TENANT'],
  ['DEPARTMENT_CREATED', 'DEPARTMENT'],
  ['DEPARTMENT_UPDATED', 'DEPARTMENT'],
  ['FAMILY_CREATED', 'FAMILY'],
  ['FAMILY_UPDATED', 'FAMILY'],
  ['VISIT_CREATED', 'VISIT'],
  ['EVALUATION_CREATED', 'EVALUATION'],
  ['EVENT_CREATED', 'EVENT'],
  ['EVENT_UPDATED', 'EVENT'],
  ['EVENT_REGISTERED', 'EVENT'],
  ['PRAYER_CREATED', 'PRAYER'],
  ['PRAYER_ANSWERED', 'PRAYER'],
  ['TASK_COMPLETED', 'TASK'],
  ['PROVISIONING_CHURCH_CREATED', 'ORGANIZATION_NODE'],
  ['PROVISIONING_FAMILY_CREATED', 'FAMILY'],
  ['PROVISIONING_DEPARTMENT_CREATED', 'DEPARTMENT'],
  ['TENANT_ONBOARDING_BRANDING_UPDATED', 'TENANT'],
  ['TENANT_ONBOARDING_FIRST_EVENT_CREATED', 'TENANT'],
  ['TENANT_ONBOARDING_CHURCH_IDENTITY', 'TENANT'],
  ['TENANT_ONBOARDING_COMPLETED', 'TENANT'],
  ['TENANT_MEMBERS_IMPORTED', 'TENANT'],
  ['TENANT_ROLES_INVITED', 'TENANT'],
  ['TENANT_REGISTRATION_APPROVED', 'TENANT'],
  ['BRANDING_UPDATED', 'TENANT_SETTINGS'],
  ['TENANT_SETTINGS_UPDATED', 'TENANT_SETTINGS'],
];

// Vocabulaire anglais qui n'a rien à faire à l'écran (le français « service »
// n'est testé qu'enraciné : « libre-service » est une formulation française).
const ANGLAIS = /\b(created|updated|deleted|sent|tenant|link|joined|invited|imported|branding|provisioning|onboarding|roles|registration|identity|event|church|request|reminder|issued|reissued|resent|approved|accepted|answered|completed|exported|user|family|department|prayer|visit|task|soul|inventory|workflow|definition|step|item|member|unknown|self_service)\b/i;

describe('auditLabels — couverture des actions réellement observées', () => {
  it.each(OBSERVEES)('%s → rendu entièrement français', (action, entiteType) => {
    const rendu = humanizeAuditAction(action, entiteType);
    // Journalisation volontaire : la relecture humaine des libellés fait foi.
    // eslint-disable-next-line no-console
    console.log(`${action} → ${rendu}`);
    expect(rendu.length).toBeGreaterThan(0);
    expect(rendu).not.toMatch(/[A-Z]{2,}/);   // aucun token de code brut
    expect(rendu).not.toMatch(ANGLAIS);
    expect(rendu).not.toMatch(/_/);           // aucun UPPER_SNAKE résiduel
  });

  it('garde une ligne de détail française pour chaque entité observée', () => {
    const entites = [...new Set(OBSERVEES.map(([, e]) => e))];
    for (const entite of entites) {
      const rendu = humanizeEntite(entite);
      expect(rendu, entite).not.toMatch(ANGLAIS);
      expect(rendu, entite).not.toMatch(/[A-Z]{2,}|_/);
    }
  });
});
