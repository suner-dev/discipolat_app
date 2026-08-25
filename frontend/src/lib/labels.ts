/**
 * Formatage des libellés utilisateur — transforme les valeurs d'énumération
 * backend (SNAKE_CASE, MAJUSCULES) en texte lisible.
 *
 *   formatEnum('EN_COURS')        → 'En cours'
 *   formatEnum('PUBLISHED')       → 'Publié'      (via LABEL_OVERRIDES)
 *   formatEnum('ALL')             → 'Tous'
 */

const LABEL_OVERRIDES: Record<string, string> = {
  // Statuts génériques
  ACTIVE: 'Actif', ACTIF: 'Actif', INACTIVE: 'Inactif', INACTIF: 'Inactif',
  PENDING: 'En attente', EN_ATTENTE: 'En attente',
  APPROVED: 'Approuvé', APPROUVEE: 'Approuvée', APPROVED_F: 'Approuvée',
  REJECTED: 'Rejeté', REJETEE: 'Rejetée',
  COMPLETED: 'Terminé', COMPLETEE: 'Complétée', TERMINE: 'Terminé',
  SCHEDULED: 'Planifié', PLANIFIEE: 'Planifiée',
  DRAFT: 'Brouillon', BROUILLON: 'Brouillon',
  PUBLISHED: 'Publié', PUBLIEE: 'Publiée', PUBLIE: 'Publié',
  EXPIRED: 'Expiré', EXPIREE: 'Expirée',
  CANCELLED: 'Annulé', ANNULER: 'Annulé', ANNULEE: 'Annulée',
  EN_COURS: 'En cours', EN_PREPARATION: 'En préparation',
  SOUMISE: 'Soumise', TRAITEE: 'Traitée',
  ACCEPTED: 'Acceptée', ACCEPTEE: 'Acceptée', RESOLVED: 'Résolue', RESOLUE: 'Résolue',
  ESCALATED: 'Escaladé',
  // Cibles d'annonces
  ALL: 'Tous', DEPARTMENT: 'Département', DEPARTEMENT: 'Département',
  FAMILY: 'Famille', FAMILLE: 'Famille', USER: 'Utilisateur', MEMBRE: 'Membre',
  // Marketplace
  OFFER: 'Offre', REQUEST: 'Demande', SERVICE: 'Service', FREE: 'Gratuit',
  // Modération / risques
  LOW: 'Faible', MEDIUM: 'Moyen', HIGH: 'Élevé', CRITICAL: 'Critique',
  HAUTE: 'Haute', MOYENNE: 'Moyenne', BASSE: 'Basse',
  // Insights
  WARNING: 'Alerte', OPPORTUNITY: 'Opportunité', INFO: 'Info',
  ENGAGEMENT: 'Engagement', GROWTH: 'Croissance', FINANCE: 'Finance', RETENTION: 'Rétention',
  // Succession
  'DÉBUTANT': 'Débutant', DEBUTANT: 'Débutant', 'INTERMÉDIAIRE': 'Intermédiaire', INTERMEDIAIRE: 'Intermédiaire', 'PRÊT': 'Prêt', PRET: 'Prêt', EXPERT: 'Expert',
  // Objectifs
  'COMPLÉTÉ': 'Complété', COMPLETE: 'Complété', ATTEINT: 'Atteint',
  // Défis
  EASY: 'Facile', HARD: 'Difficile',
  UP: 'Hausse', DOWN: 'Baisse', STABLE: 'Stable',
  HAUSSE: 'Hausse', BAISSE: 'Baisse',
};

export function formatEnum(value?: string | null): string {
  if (!value) return '';
  const override = LABEL_OVERRIDES[value];
  if (override) return override;
  const lowered = value.replace(/_/g, ' ').toLowerCase();
  return lowered.charAt(0).toUpperCase() + lowered.slice(1);
}

/** Libellés des cibles d'annonce (identiques quel que soit le casing backend). */
export function formatTarget(target?: string | null): string {
  if (!target) return '';
  return LABEL_OVERRIDES[target.toUpperCase()] ?? formatEnum(target);
}
