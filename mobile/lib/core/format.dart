/// Formatage des libellés utilisateur — transforme les énumérations backend
/// (MAJUSCULES, SNAKE_CASE) en texte lisible à l'écran.
///
///   formatLabel('EN_COURS')  → 'En cours'
///   formatLabel('PENDING')   → 'En attente'
String formatLabel(String? value) {
  if (value == null || value.isEmpty) return '';
  const overrides = <String, String>{
    'PENDING': 'En attente',
    'ACCEPTED': 'Acceptée',
    'ACCEPTEE': 'Acceptée',
    'RESOLVED': 'Résolue',
    'RESOLUE': 'Résolue',
    'REJECTED': 'Rejetée',
    'REJETEE': 'Rejetée',
    'COMPLETED': 'Terminé',
    'COMPLETEE': 'Complétée',
    'COMPLÉTÉ': 'Complété',
    'EN_COURS': 'En cours',
    'EN_PREPARATION': 'En préparation',
    'SCHEDULED': 'Planifié',
    'PLANIFIEE': 'Planifiée',
    'DRAFT': 'Brouillon',
    'PUBLISHED': 'Publié',
    'CANCELLED': 'Annulé',
    'ACTIF': 'Actif',
    'SUSPENDU': 'Suspendu',
    'RETIRED': 'Retiré',
    'HAUTE': 'Haute',
    'MOYENNE': 'Moyenne',
    'BASSE': 'Basse',
    'DÉBUTANT': 'Débutant',
    'DEBUTANT': 'Débutant',
    'INTERMÉDIAIRE': 'Intermédiaire',
    'INTERMEDIAIRE': 'Intermédiaire',
    'PRÊT': 'Prêt',
    'PRET': 'Prêt',
    'EXPERT': 'Expert',
    'LOW': 'Faible',
    'HIGH': 'Élevé',
    'CRITICAL': 'Critique',
  };
  final override = overrides[value];
  if (override != null) return override;
  final lowered = value.replaceAll('_', ' ').toLowerCase();
  if (lowered.isEmpty) return lowered;
  return lowered[0].toUpperCase() + lowered.substring(1);
}
