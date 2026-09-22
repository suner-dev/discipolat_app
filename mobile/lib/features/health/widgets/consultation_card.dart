import 'package:flutter/material.dart';
import 'package:intl/intl.dart';

import 'package:discipolat_mobile/features/health/models/health_model.dart';
import 'package:discipolat_mobile/presentation/widgets/glass_theme.dart';

class ConsultationCard extends StatelessWidget {
  final Consultation consultation;
  final VoidCallback onTap;

  const ConsultationCard({
    super.key,
    required this.consultation,
    required this.onTap,
  });

  @override
  Widget build(BuildContext context) {
    final statusColor = _getStatusColor(consultation.status);
    final typeColor = _getTypeColor(consultation.type);
    final isToday = _isToday(consultation.dateTime);

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
                  Container(
                    padding: const EdgeInsets.all(8),
                    decoration: BoxDecoration(
                      color: typeColor.withOpacity(0.2),
                      borderRadius: BorderRadius.circular(12),
                    ),
                    child: Icon(_getTypeIcon(consultation.type), color: typeColor, size: 24),
                  ),
                  const SizedBox(width: 12),
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Row(
                          children: [
                            Text(
                              consultation.type.displayName,
                              style: Theme.of(context).textTheme.titleMedium?.copyWith(fontWeight: FontWeight.w600),
                            ),
                            if (isToday) ...[
                              const SizedBox(width: 8),
                              Container(
                                padding: const EdgeInsets.symmetric(horizontal: 6, vertical: 2),
                                decoration: BoxDecoration(
                                  color: Colors.red,
                                  borderRadius: BorderRadius.circular(8),
                                ),
                                child: const Text('AUJOURD\'HUI', style: TextStyle(fontSize: 8, fontWeight: FontWeight.bold, color: Colors.white)),
                              ),
                            ],
                          ],
                        ),
                        Text(
                          'Dr. ${consultation.doctorName} • ${consultation.patientName}',
                          style: TextStyle(fontSize: 12, color: AppColors.surface.withOpacity(0.7)),
                        ),
                      ],
                    ),
                  ),
                  Container(
                    padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
                    decoration: BoxDecoration(
                      color: statusColor.withOpacity(0.2),
                      borderRadius: BorderRadius.circular(20),
                    ),
                    child: Text(
                      consultation.status.displayName,
                      style: TextStyle(fontSize: 10, fontWeight: FontWeight.bold, color: _getStatusColor(consultation.status)),
                    ),
                  ),
                ],
              ),
              const SizedBox(height: 12),
              Row(
                children: [
                  Icon(Icons.schedule_rounded, size: 14, color: AppColors.surface.withOpacity(0.7)),
                  const SizedBox(width: 4),
                  Text(
                    DateFormat('EEEE dd MMMM yyyy à HH:mm', 'fr_FR').format(consultation.dateTime.toLocal()),
                    style: Theme.of(context).textTheme.bodySmall?.copyWith(color: AppColors.surface.withOpacity(0.7)),
                  ),
                ],
              ),
              if (consultation.chiefComplaint != null) ...[
                const SizedBox(height: 8),
                Text(
                  'Motif: ${consultation.chiefComplaint}',
                  style: Theme.of(context).textTheme.bodySmall?.copyWith(color: AppColors.surface.withOpacity(0.7)),
                  maxLines: 2,
                  overflow: TextOverflow.ellipsis,
                ),
              ],
              if (consultation.diagnosis != null) ...[
                const SizedBox(height: 4),
                Container(
                  padding: const EdgeInsets.all(8),
                  decoration: BoxDecoration(
                    color: Colors.blue.withOpacity(0.1),
                    borderRadius: BorderRadius.circular(8),
                  ),
                  child: Row(
                    children: [
                      Icon(Icons.medical_information_rounded, size: 14, color: Colors.blue),
                      const SizedBox(width: 8),
                      Expanded(
                        child: Text(
                          'Diagnostic: ${consultation.diagnosis}',
                          style: TextStyle(fontSize: 11, color: Colors.blue, fontStyle: FontStyle.italic),
                        ),
                      ),
                    ],
                  ),
                ),
              ],
              if (consultation.prescriptions != null && consultation.prescriptions!.isNotEmpty) ...[
                const SizedBox(height: 8),
                Row(
                  children: [
                    Icon(Icons.medication_rounded, size: 14, color: Colors.green),
                    const SizedBox(width: 4),
                    Text(
                      '${consultation.prescriptions!.length} prescription(s)',
                      style: TextStyle(fontSize: 11, color: Colors.green, fontWeight: FontWeight.w500),
                    ),
                  ],
                ),
              ],
              const SizedBox(height: 12),
              Row(
                children: [
                  Container(
                    padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
                    decoration: BoxDecoration(
                      color: _getStatusColor(consultation.status).withOpacity(0.2),
                      borderRadius: BorderRadius.circular(20),
                    ),
                    child: Text(
                      consultation.status.displayName,
                      style: TextStyle(fontSize: 10, fontWeight: FontWeight.bold, color: _getStatusColor(consultation.status)),
                    ),
                  ),
                ],
              ),
            ],
          ),
        ),
      ),
    );
  }

  bool _isToday(DateTime dateTime) {
    final now = DateTime.now();
    final date = dateTime;
    return date.year == DateTime.now().year &&
        date.month == DateTime.now().month &&
        date.day == DateTime.now().day;
  }

  Color _getStatusColor(ConsultationStatus status) {
    switch (status) {
      case ConsultationStatus.scheduled:
        return Colors.blue;
      case ConsultationStatus.inProgress:
        return Colors.orange;
      case ConsultationStatus.completed:
        return Colors.green;
      case ConsultationStatus.cancelled:
        return Colors.red;
      case ConsultationStatus.noShow:
        return Colors.grey;
    }
  }

  Color _getTypeColor(ConsultationType type) {
    switch (type) {
      case ConsultationType.general:
        return Colors.blue;
      case ConsultationType.specialist:
        return Colors.purple;
      case ConsultationType.emergency:
        return Colors.red;
      case ConsultationType.followUp:
        return Colors.green;
      case ConsultationType.preventive:
        return Colors.teal;
      case ConsultationType.prenatal:
        return Colors.pink;
      case ConsultationType.vaccination:
        return Colors.orange;
    }
  }

  IconData _getTypeIcon(ConsultationType type) {
    switch (type) {
      case ConsultationType.general:
        return Icons.medical_services_rounded;
      case ConsultationType.specialist:
        return Icons.local_hospital_rounded;
      case ConsultationType.emergency:
        return Icons.emergency_rounded;
      case ConsultationType.followUp:
        return Icons.follow_the_signs_rounded;
      case ConsultationType.preventive:
        return Icons.shield_rounded;
      case ConsultationType.prenatal:
        return Icons.pregnant_woman_rounded;
      case ConsultationType.vaccination:
        return Icons.vaccines_rounded;
    }
  }
}