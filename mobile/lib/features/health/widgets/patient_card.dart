import 'package:flutter/material.dart';
import 'package:intl/intl.dart';

import 'package:discipolat_mobile/features/health/models/health_model.dart';
import 'package:discipolat_mobile/presentation/widgets/glass_theme.dart';

class PatientCard extends StatelessWidget {
  final Patient patient;
  final VoidCallback onTap;

  const PatientCard({
    super.key,
    required this.patient,
    required this.onTap,
  });

  @override
  Widget build(BuildContext context) {
    final statusColor = _getStatusColor(patient.status);

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
                    backgroundImage: patient.photoUrl != null ? NetworkImage(patient.photoUrl!) : null,
                    child: patient.photoUrl == null
                        ? Text(
                            patient.firstName.isNotEmpty ? patient.firstName[0] : '?',
                            style: TextStyle(fontSize: 20, fontWeight: FontWeight.bold, color: Colors.red),
                          )
                        : null,
                  ),
                  const SizedBox(width: 12),
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(
                          patient.fullName,
                          style: Theme.of(context).textTheme.titleMedium?.copyWith(fontWeight: FontWeight.w600),
                        ),
                        const SizedBox(height: 2),
                        Text(
                          '${patient.age} ans • ${patient.gender}',
                          style: TextStyle(fontSize: 12, color: AppColors.surface.withOpacity(0.7)),
                        ),
                      ],
                    ),
                  ),
                  Container(
                    padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
                    decoration: BoxDecoration(
                      color: _getStatusColor(patient.status).withOpacity(0.2),
                      borderRadius: BorderRadius.circular(20),
                    ),
                    child: Text(
                      patient.status.displayName,
                      style: TextStyle(fontSize: 10, fontWeight: FontWeight.bold, color: _getStatusColor(patient.status)),
                    ),
                  ),
                ],
              ),
              if (patient.chronicConditions != null && patient.chronicConditions!.isNotEmpty) ...[
                const SizedBox(height: 12),
                Wrap(
                  spacing: 6,
                  runSpacing: 6,
                  children: patient.chronicConditions!.take(3).map((condition) {
                    return Container(
                      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
                      decoration: BoxDecoration(
                        color: Colors.orange.withOpacity(0.1),
                        borderRadius: BorderRadius.circular(8),
                      ),
                      child: Text(
                        condition,
                        style: TextStyle(fontSize: 10, color: Colors.orange),
                      ),
                    );
                  }).toList(),
                ),
              ],
              if (patient.allergies != null && patient.allergies!.isNotEmpty) ...[
                const SizedBox(height: 8),
                Wrap(
                  spacing: 6,
                  runSpacing: 6,
                  children: patient.allergies!.take(3).map((allergy) {
                    return Container(
                      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
                      decoration: BoxDecoration(
                        color: Colors.red.withOpacity(0.1),
                        borderRadius: BorderRadius.circular(8),
                      ),
                      child: Row(
                        mainAxisSize: MainAxisSize.min,
                        children: [
                          Icon(Icons.warning_amber_rounded, size: 10, color: Colors.red),
                          const SizedBox(width: 4),
                          Text(allergy, style: TextStyle(fontSize: 10, color: Colors.red)),
                        ],
                      ),
                    );
                  }).toList(),
                ),
              ],
              if (patient.bloodType != null) ...[
                const SizedBox(height: 12),
                Container(
                  padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 6),
                  decoration: BoxDecoration(
                    color: Colors.red.withOpacity(0.1),
                    borderRadius: BorderRadius.circular(20),
                    border: Border.all(color: Colors.red.withOpacity(0.3)),
                  ),
                  child: Row(
                    mainAxisSize: MainAxisSize.min,
                    children: [
                      Icon(Icons.bloodtype_rounded, size: 14, color: Colors.red),
                      const SizedBox(width: 6),
                      Text(
                        'Groupe sanguin: ${patient.bloodType}',
                        style: TextStyle(fontSize: 12, fontWeight: FontWeight.w500, color: Colors.red),
                      ),
                    ],
                  ),
                ),
              ],
            ],
          ),
        ),
      ),
    );
  }

  Color _getStatusColor(PatientStatus status) {
    switch (status) {
      case PatientStatus.active:
        return Colors.green;
      case PatientStatus.inactive:
        return Colors.orange;
      case PatientStatus.discharged:
        return Colors.blue;
      case PatientStatus.deceased:
        return Colors.grey;
    }
  }
}