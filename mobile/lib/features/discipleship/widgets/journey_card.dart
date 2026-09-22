import 'package:flutter/material.dart';

import 'package:discipolat_mobile/features/discipleship/models/discipleship_model.dart';
import 'package:discipolat_mobile/presentation/widgets/glass_theme.dart'
    show AppColors;

class JourneyCard extends StatelessWidget {
  final DiscipleshipJourney journey;
  final VoidCallback onTap;

  const JourneyCard({
    super.key,
    required this.journey,
    required this.onTap,
  });

  @override
  Widget build(BuildContext context) {
    final isActive = journey.isActive;
    final journeyTypeColor = _getTypeColor(journey.type);

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
                    padding: const EdgeInsets.all(12),
                    decoration: BoxDecoration(
                      color: Colors.purple.withOpacity(0.2),
                      borderRadius: BorderRadius.circular(12),
                    ),
                    child: Icon(Icons.menu_book_rounded, color: Colors.purple, size: 28),
                  ),
                  const SizedBox(width: 12),
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(
                          journey.name,
                          style: Theme.of(context).textTheme.titleMedium?.copyWith(
                            fontWeight: FontWeight.w600,
                          ),
                          maxLines: 1,
                          overflow: TextOverflow.ellipsis,
                        ),
                        const SizedBox(height: 2),
                        Text(
                          journey.type.name,
                          style: TextStyle(fontSize: 12, color: AppColors.surface.withOpacity(0.7)),
                        ),
                      ],
                    ),
                  ),
                  Container(
                    padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
                    decoration: BoxDecoration(
                      color: journey.isActive ? Colors.green.withOpacity(0.2) : Colors.grey.withOpacity(0.2),
                      borderRadius: BorderRadius.circular(20),
                    ),
                    child: Text(
                      journey.isActive ? 'Actif' : 'Inactif',
                      style: TextStyle(
                        fontSize: 10,
                        fontWeight: FontWeight.bold,
                        color: journey.isActive ? Colors.green : Colors.grey,
                      ),
                    ),
                  ),
                ],
              ),
              if (journey.description != null) ...[
                const SizedBox(height: 8),
                Text(
                  journey.description!,
                  style: Theme.of(context).textTheme.bodySmall?.copyWith(color: AppColors.surface.withOpacity(0.7)),
                  maxLines: 2,
                  overflow: TextOverflow.ellipsis,
                ),
              ],
              const SizedBox(height: 12),
              Row(
                children: [
                  Icon(Icons.calendar_today_rounded, size: 14, color: AppColors.surface.withOpacity(0.7)),
                  const SizedBox(width: 4),
                  Text(
                    'Du ${DateFormat('dd/MM/yyyy').format(journey.startDate.toLocal())}',
                    style: Theme.of(context).textTheme.bodySmall?.copyWith(color: AppColors.surface.withOpacity(0.7)),
                  ),
                  if (journey.endDate != null) ...[
                    const SizedBox(width: 12),
                    Icon(Icons.event_rounded, size: 14, color: AppColors.surface.withOpacity(0.7)),
                    const SizedBox(width: 4),
                    Text(
                      'Au ${DateFormat('dd/MM/yyyy').format(journey.endDate!.toLocal())}',
                      style: Theme.of(context).textTheme.bodySmall?.copyWith(color: AppColors.surface.withOpacity(0.7)),
                    ),
                  ],
                ],
              ),
            ],
          ),
        ),
      ),
    );
  }
}