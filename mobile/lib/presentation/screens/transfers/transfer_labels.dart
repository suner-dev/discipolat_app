import 'package:flutter/material.dart';

/// Libellés des types de transfert (workflow configurable).
const Map<String, String> kTransferTypeLabels = {
  'MEMBRE_DEPARTEMENT_TRANSFERT': 'Transfert de membre entre départements',
  'MEMBRE_DEPARTEMENT_AJOUT': 'Ajout de membre dans un département',
  'MEMBRE_DEPARTEMENT_RETRAIT': "Retrait de membre d'un département",
  'DISCIPLE_FAMILLE_TRANSFERT': 'Transfert de disciple entre familles',
  'FAISEUR_FAMILLE_TRANSFERT': 'Transfert de faiseur entre familles',
  'CHEF_FAMILLE_TRANSFERT': 'Transfert de chef de famille',
  'FAISEUR_DISCIPLE_CHANGEMENT': "Changement du faiseur d'un disciple",
  'RESPONSABLE_DEPARTEMENT_CHANGEMENT': "Changement du responsable d'un département",
  'CHEF_ADJOINT_CHANGEMENT': "Changement du chef adjoint d'une famille",
};

/// Libellés des statuts du cycle de vie.
const Map<String, String> kTransferStatusLabels = {
  'BROUILLON': 'Brouillon',
  'SOUMIS': 'Soumis',
  'EN_ATTENTE_VALIDATION': 'En attente de validation',
  'VALIDATION_PARTIELLE': 'Validation partielle',
  'VALIDE': 'Validé',
  'REFUSE': 'Refusé',
  'ANNULE': 'Annulé',
  'EXECUTE': 'Exécuté',
  'ARCHIVE': 'Archivé',
};

const Map<String, String> kDecisionLabels = {
  'APPROBATION': 'Approbation',
  'REFUS': 'Refus',
  'DEMANDE_INFORMATIONS': "Demande d'informations",
  'RENVOI_CORRECTION': 'Renvoi pour correction',
};

const Map<String, String> kPrioriteLabels = {
  'BASSE': 'Basse',
  'MOYENNE': 'Moyenne',
  'HAUTE': 'Haute',
  'URGENTE': 'Urgente',
};

/// Couleur du statut pour les badges.
Color transferStatusColor(String statut) {
  switch (statut) {
    case 'BROUILLON': return Colors.blueGrey;
    case 'SOUMIS': return Colors.lightBlue;
    case 'EN_ATTENTE_VALIDATION': return Colors.orange;
    case 'VALIDATION_PARTIELLE': return Colors.lightBlue;
    case 'VALIDE': return Colors.teal;
    case 'REFUSE': return Colors.red;
    case 'ANNULE': return Colors.blueGrey;
    case 'EXECUTE': return Colors.green;
    case 'ARCHIVE': return Colors.blueGrey;
    default: return Colors.grey;
  }
}

IconData transferStatusIcon(String statut) {
  switch (statut) {
    case 'BROUILLON': return Icons.edit_note;
    case 'SOUMIS': return Icons.send;
    case 'EN_ATTENTE_VALIDATION': return Icons.hourglass_top;
    case 'VALIDATION_PARTIELLE': return Icons.verified_user;
    case 'VALIDE': return Icons.verified;
    case 'REFUSE': return Icons.cancel;
    case 'ANNULE': return Icons.block;
    case 'EXECUTE': return Icons.swap_horiz;
    case 'ARCHIVE': return Icons.archive;
    default: return Icons.description;
  }
}

Color prioriteColor(String priorite) {
  switch (priorite) {
    case 'URGENTE': return Colors.red;
    case 'HAUTE': return Colors.orange;
    case 'MOYENNE': return Colors.lightBlue;
    default: return Colors.blueGrey;
  }
}

/// Libellé d'un type de transfert (avec repli).
String transferTypeLabel(String type) => kTransferTypeLabels[type] ?? type;

/// Libellé d'un statut (avec repli).
String transferStatusLabel(String statut) => kTransferStatusLabels[statut] ?? statut;
