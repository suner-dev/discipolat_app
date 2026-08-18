/**
 * Types partagés pour tous les onglets du DepartmentManagementPage.
 * Extraits du monolithe (2032 lignes) lors de la Phase 3 — réduction de la dette technique.
 */

export type Team = {
  id: string;
  nom: string;
  parentId?: string | null;
  type: string;
  chefNom?: string;
  adjointNom?: string;
  objectif?: string;
  description?: string;
  dateDebut?: string;
  dateFin?: string;
  statut: string;
  nbMembres: number;
};

export type Position = {
  id: string;
  nom: string;
  description?: string;
  competencesRequises?: string;
  statut: string;
  nbMembres: number;
};

export type Assignment = {
  id: string;
  memberId: string;
  memberNom?: string;
  teamId?: string;
  teamNom?: string;
  positionId?: string;
  positionNom?: string;
  role: string;
  dateDebut?: string;
  dateFin?: string;
  actif: boolean;
};

export type Task = {
  id: string;
  titre: string;
  description?: string;
  teamId?: string;
  teamNom?: string;
  assignedTo?: string;
  assigneeNom?: string;
  priorite: string;
  statut: string;
  echeance?: string;
  avancement: number;
  enRetard: boolean;
};

export type ActivityItem = {
  id: string;
  action: string;
  details?: string;
  actorNom?: string;
  createdAt: string;
};

export type Checklist = {
  id: string;
  nom: string;
  description?: string;
  items: { id: string; label: string; checked: boolean }[];
  statut: string;
};

export type Equipment = {
  id: string;
  nom: string;
  description?: string;
  categorie?: string;
  quantite?: number;
  statut: string;
  dateAcquisition?: string;
};

export type DeptEvent = {
  id: string;
  titre: string;
  description?: string;
  type: string;
  dateDebut: string;
  dateFin?: string;
  lieu?: string;
  statut: string;
  nbInscrits: number;
  nbPresents?: number;
};

export type DeptDocument = {
  id: string;
  nom: string;
  categorie?: string;
  description?: string;
  url: string;
  taille?: number;
  dateAjout: string;
  ajoutePar?: string;
};

export type SearchResults = {
  members?: { id: string; nom: string; email?: string }[];
  teams?: { id: string; nom: string }[];
  tasks?: { id: string; titre: string; statut: string }[];
};

// ===================== Constantes =====================

export const TYPE_LABELS: Record<string, string> = {
  SOUS_DEPARTEMENT: 'Sous-département',
  EQUIPE_PERMANENTE: 'Équipe permanente',
  EQUIPE_TEMPORAIRE: 'Équipe temporaire',
};

export const TYPE_COLORS: Record<string, string> = {
  SOUS_DEPARTEMENT: 'badge-info',
  EQUIPE_PERMANENTE: 'badge-success',
  EQUIPE_TEMPORAIRE: 'badge-warning',
};

export const PRIORITE_COLORS: Record<string, string> = {
  BASSE: 'text-gray-400',
  MOYENNE: 'text-amber-500',
  HAUTE: 'text-red-500',
};

export const STATUT_TASK_BADGE: Record<string, string> = {
  A_FAIRE: 'badge-gray',
  EN_COURS: 'badge-info',
  BLOQUEE: 'badge-danger',
  TERMINEE: 'badge-success',
  VALIDEE: 'badge-success',
  ANNULEE: 'badge-inactive',
};

export const ROLE_LABELS: Record<string, string> = {
  CHEF: 'Chef',
  ADJOINT: 'Adjoint',
  MEMBRE: 'Membre',
};

export const ACTION_LABELS: Record<string, string> = {
  TEAM_CREATED: 'Équipe créée',
  TEAM_UPDATED: 'Équipe modifiée',
  TEAM_ARCHIVED: 'Équipe archivée',
  POSITION_CREATED: 'Poste créé',
  POSITION_UPDATED: 'Poste modifié',
  POSITION_ARCHIVED: 'Poste archivé',
  MEMBER_ASSIGNED: 'Membre affecté',
  ASSIGNMENT_ENDED: "Fin d'affectation",
  TASK_CREATED: 'Tâche créée',
  TASK_UPDATED: 'Tâche mise à jour',
  TASK_DELETED: 'Tâche supprimée',
};

export const DOC_TYPES = ['PROCEDURE', 'GUIDE', 'DOCUMENT', 'FORMULAIRE', 'COMPTE_RENDU', 'RESSOURCE'] as const;

export const DOC_TYPE_LABELS: Record<string, string> = {
  PROCEDURE: 'Procédure', GUIDE: 'Guide', DOCUMENT: 'Document',
  FORMULAIRE: 'Formulaire', COMPTE_RENDU: 'Compte rendu', RESSOURCE: 'Ressource',
};

export const DOC_TYPE_BADGES: Record<string, string> = {
  PROCEDURE: 'badge-danger', GUIDE: 'badge-info', DOCUMENT: 'badge-gray',
  FORMULAIRE: 'badge-warning', COMPTE_RENDU: 'badge-success', RESSOURCE: 'badge-violet',
};
