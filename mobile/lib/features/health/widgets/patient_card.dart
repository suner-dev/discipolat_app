import 'package:flutter/material.dart';

import 'package:discipolat_mobile/features/health/models/health_model.dart';
import 'package:discipolat_mobile/presentation/widgets/glass_theme.dart';

/// Fiche patient — champs de la vue aplatie V240 du serveur
/// (`personName`, `familyName`, `groupeSanguin`, `allergies`… String libres).
class PatientCard extends StatelessWidget {
  final PatientRecord patient;
  final VoidCallback onTap;

  const PatientCard({
    super.key,
    required this.patient,
    required this.onTap,
  });

  @override
  Widget build(BuildContext context) {
    final name = patient.personName ?? 'Patient';
    final confidential =
        patient.confidentialityLevel == ConfidentialityLevel.strict;

    return Card(
      margin: const EdgeInsets.only(bottom: 12),
      color: AppColors.cardDark,
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
      child: InkWell(
        onTap: onTap,
        borderRadius: BorderRadius.circular(16),
        child: Padding(
          padding: const EdgeInsets.all(16),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Row(
                children: [
                  CircleAvatar(
                    radius: 28,
                    backgroundColor: Colors.red.withOpacity(0.2),
                    child: Text(
                      name.isNotEmpty ? name[0].toUpperCase() : '?',
                      style: const TextStyle(
                          fontSize: 20,
                          fontWeight: FontWeight.bold,
                          color: Colors.red),
                    ),
                  ),
                  const SizedBox(width: 12),
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(
                          name,
                          style: Theme.of(context)
                              .textTheme
                              .titleMedium
                              ?.copyWith(fontWeight: FontWeight.w600),
                          maxLines: 1,
                          overflow: TextOverflow.ellipsis,
                        ),
                        if (patient.familyName != null) ...[
                          const SizedBox(height: 2),
                          Text(
                            'Famille : ${patient.familyName}',
                            style: TextStyle(
                                fontSize: 12,
                                color: AppColors.surface.withOpacity(0.7)),
                          ),
                        ],
                      ],
                    ),
                  ),
                  if (confidential)
                    Container(
                      padding: const EdgeInsets.symmetric(
                          horizontal: 10, vertical: 4),
                      decoration: BoxDecoration(
                        color: Colors.amber.withOpacity(0.2),
                        borderRadius: BorderRadius.circular(20),
                      ),
                      child: const Row(
                        mainAxisSize: MainAxisSize.min,
                        children: [
                          Icon(Icons.lock_rounded, size: 12, color: Colors.amber),
                          SizedBox(width: 4),
                          Text(
                            'STRICT',
                            style: TextStyle(
                                fontSize: 10,
                                fontWeight: FontWeight.bold,
                                color: Colors.amber),
                          ),
                        ],
                      ),
                    ),
                ],
              ),
              if (patient.allergies != null) ...[
                const SizedBox(height: 12),
                _chip(
                  Icons.warning_amber_rounded,
                  'Allergies : ${patient.allergies}',
                  Colors.red,
                ),
              ],
              if (patient.antecedents != null) ...[
                const SizedBox(height: 6),
                _chip(
                  Icons.history_rounded,
                  'Antécédents : ${patient.antecedents}',
                  Colors.orange,
                ),
              ],
              const SizedBox(height: 12),
              Wrap(
                spacing: 8,
                runSpacing: 8,
                children: [
                  if (patient.groupeSanguin != null)
                    _badge(Icons.bloodtype_rounded, Colors.red,
                        'Groupe ${patient.groupeSanguin}'),
                  if (patient.poidsKg != null)
                    _badge(Icons.monitor_weight_rounded, Colors.blue,
                        '${patient.poidsKg?.toStringAsFixed(1)} kg'),
                  if (patient.tailleCm != null)
                    _badge(Icons.straighten_rounded, Colors.blue,
                        '${patient.tailleCm?.toStringAsFixed(0)} cm'),
                  if (patient.medecinTraitant != null)
                    _badge(Icons.medical_information_rounded, Colors.teal,
                        patient.medecinTraitant!),
                ],
              ),
            ],
          ),
        ),
      ),
    );
  }

  Widget _chip(IconData icon, String label, Color color) {
    return Row(
      children: [
        Icon(icon, size: 14, color: color),
        const SizedBox(width: 6),
        Expanded(
          child: Text(
            label,
            style: TextStyle(fontSize: 12, color: color),
            maxLines: 2,
            overflow: TextOverflow.ellipsis,
          ),
        ),
      ],
    );
  }

  Widget _badge(IconData icon, Color color, String label) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 6),
      decoration: BoxDecoration(
        color: color.withOpacity(0.1),
        borderRadius: BorderRadius.circular(20),
        border: Border.all(color: color.withOpacity(0.3)),
      ),
      child: Row(
        mainAxisSize: MainAxisSize.min,
        children: [
          Icon(icon, size: 14, color: color),
          const SizedBox(width: 6),
          Text(
            label,
            style:
                TextStyle(fontSize: 12, fontWeight: FontWeight.w500, color: color),
          ),
        ],
      ),
    );
  }
}
