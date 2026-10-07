import 'package:flutter/material.dart';

import 'package:discipolat_mobile/features/health/models/health_model.dart';
import 'package:discipolat_mobile/presentation/widgets/glass_theme.dart';

/// Fiche consultation — vue aplatie V240 (`patientName`, `practitionerName`,
/// `consultationDate` au format `yyyy-MM-dd`, `typeConsultation` String
/// bornée TRIAGE/CONSULTATION/SUIVI côté serveur).
class ConsultationCard extends StatelessWidget {
  final MedicalConsultation consultation;
  final VoidCallback onTap;

  const ConsultationCard({
    super.key,
    required this.consultation,
    required this.onTap,
  });

  @override
  Widget build(BuildContext context) {
    final statusColor = _statusColor(consultation.status);

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
                    radius: 24,
                    backgroundColor: Colors.blue.withOpacity(0.2),
                    child: const Icon(Icons.medical_information_rounded,
                        color: Colors.blue, size: 24),
                  ),
                  const SizedBox(width: 12),
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(
                          consultation.patientName ?? 'Patient inconnu',
                          style: Theme.of(context)
                              .textTheme
                              .titleMedium
                              ?.copyWith(fontWeight: FontWeight.w600),
                          maxLines: 1,
                          overflow: TextOverflow.ellipsis,
                        ),
                        const SizedBox(height: 2),
                        Text(
                          _subtitle(),
                          style: TextStyle(
                              fontSize: 12,
                              color: AppColors.surface.withOpacity(0.7)),
                          maxLines: 1,
                          overflow: TextOverflow.ellipsis,
                        ),
                      ],
                    ),
                  ),
                  Container(
                    padding: const EdgeInsets.symmetric(
                        horizontal: 10, vertical: 4),
                    decoration: BoxDecoration(
                      color: statusColor.withOpacity(0.2),
                      borderRadius: BorderRadius.circular(20),
                    ),
                    child: Text(
                      consultation.status.displayName,
                      style: TextStyle(
                          fontSize: 10,
                          fontWeight: FontWeight.bold,
                          color: statusColor),
                    ),
                  ),
                ],
              ),
              if (consultation.motif != null) ...[
                const SizedBox(height: 12),
                _line(Icons.chat_rounded, 'Motif', consultation.motif!),
              ],
              if (consultation.diagnostic != null) ...[
                const SizedBox(height: 6),
                _line(Icons.medical_information_rounded, 'Diagnostic',
                    consultation.diagnostic!),
              ],
              if (consultation.traitement != null) ...[
                const SizedBox(height: 6),
                _line(Icons.healing_rounded, 'Traitement',
                    consultation.traitement!),
              ],
            ],
          ),
        ),
      ),
    );
  }

  String _subtitle() {
    final parts = <String>[
      if (consultation.consultationDate != null)
        _prettyDate(consultation.consultationDate!),
      if (consultation.typeConsultation != null) consultation.typeConsultation!,
      if (consultation.practitionerName != null)
        'Praticien : ${consultation.practitionerName}',
    ];
    return parts.join(' • ');
  }

  /// `yyyy-MM-dd` (LocalDate serveur) → `dd/MM/yyyy` ; retombe sur la valeur
  /// brute si le format n'est pas analysable (tolérance, jamais de crash).
  static String _prettyDate(String iso) {
    final parsed = DateTime.tryParse(iso);
    if (parsed == null) return iso;
    final m = parsed.month.toString().padLeft(2, '0');
    final d = parsed.day.toString().padLeft(2, '0');
    return '$d/$m/${parsed.year}';
  }

  Widget _line(IconData icon, String label, String value) {
    return Row(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Icon(icon, size: 14, color: AppColors.surface.withOpacity(0.7)),
        const SizedBox(width: 6),
        Expanded(
          child: Text(
            '$label : $value',
            style:
                TextStyle(fontSize: 12, color: AppColors.surface.withOpacity(0.85)),
            maxLines: 2,
            overflow: TextOverflow.ellipsis,
          ),
        ),
      ],
    );
  }

  Color _statusColor(ConsultationStatus status) {
    switch (status) {
      case ConsultationStatus.scheduled:
        return Colors.blue;
      case ConsultationStatus.inProgress:
        return Colors.orange;
      case ConsultationStatus.completed:
        return Colors.green;
      case ConsultationStatus.cancelled:
        return Colors.grey;
    }
  }
}
