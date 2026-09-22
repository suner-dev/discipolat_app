import 'package:flutter/material.dart';
import 'package:intl/intl.dart';

import 'package:discipolat_mobile/features/health/models/health_model.dart';
import 'package:discipolat_mobile/presentation/widgets/glass_theme.dart';

class CampaignCard extends StatelessWidget {
  final HealthCampaign campaign;
  final VoidCallback onTap;

  const CampaignCard({
    super.key,
    required this.campaign,
    required this.onTap,
  });

  @override
  Widget build(BuildContext context) {
    final isActive = campaign.isActive;
    final isUpcoming = campaign.isUpcoming;
    final isCompleted = campaign.isCompleted;
    final statusColor = _getStatusColor(campaign.status);
    final typeColor = _getTypeColor(campaign.type);
    final progress = campaign.targetPopulation != null && campaign.targetPopulation! > 0
        ? campaign.registeredCount / campaign.targetPopulation!
        : 0.0;

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
                    padding: const EdgeInsets.all(10),
                    decoration: BoxDecoration(
                      color: typeColor.withOpacity(0.2),
                      borderRadius: BorderRadius.circular(12),
                    ),
                    child: Icon(_getTypeIcon(campaign.type), color: typeColor, size: 24),
                  ),
                  const SizedBox(width: 12),
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(
                          campaign.name,
                          style: Theme.of(context).textTheme.titleMedium?.copyWith(fontWeight: FontWeight.w600),
                          maxLines: 1,
                          overflow: TextOverflow.ellipsis,
                        ),
                        const SizedBox(height: 2),
                        Text(
                          campaign.type.displayName,
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
                      campaign.status.displayName,
                      style: TextStyle(fontSize: 10, fontWeight: FontWeight.bold, color: statusColor),
                    ),
                  ),
                ],
              ),
              if (campaign.description != null) ...[
                const SizedBox(height: 8),
                Text(
                  campaign.description!,
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
                    '${DateFormat('dd/MM/yyyy').format(campaign.startDate.toLocal())} - ${DateFormat('dd/MM/yyyy').format(campaign.endDate.toLocal())}',
                    style: Theme.of(context).textTheme.bodySmall?.copyWith(color: AppColors.surface.withOpacity(0.7)),
                  ),
                  if (campaign.location != null) ...[
                    const SizedBox(width: 16),
                    Icon(Icons.location_on_rounded, size: 14, color: AppColors.surface.withOpacity(0.7)),
                    const SizedBox(width: 4),
                    Flexible(
                      child: Text(
                        campaign.location!,
                        style: Theme.of(context).textTheme.bodySmall?.copyWith(color: AppColors.surface.withOpacity(0.7)),
                        maxLines: 1,
                        overflow: TextOverflow.ellipsis,
                      ),
                    ),
                  ],
                ],
              ),
              const SizedBox(height: 12),
              if (campaign.targetPopulation != null) ...[
                Text(
                  'Inscriptions: ${campaign.registeredCount}/${campaign.targetPopulation} (${campaign.attendedCount} présents)',
                  style: TextStyle(fontSize: 12, color: AppColors.surface.withOpacity(0.7)),
                ),
                const SizedBox(height: 8),
                LinearProgressIndicator(
                  value: progress.clamp(0.0, 1.0),
                  backgroundColor: AppColors.surfaceDark,
                  valueColor: AlwaysStoppedAnimation<Color>(isActive ? Colors.green : (isUpcoming ? Colors.blue : Colors.grey)),
                  minHeight: 6,
                  borderRadius: BorderRadius.circular(3),
                ),
                const SizedBox(height: 4),
                Text(
                  '${(progress * 100).toStringAsFixed(1)}% de l\'objectif',
                  style: TextStyle(fontSize: 10, color: AppColors.surface.withOpacity(0.7)),
                ),
                const SizedBox(height: 12),
              ],
              Row(
                children: [
                  if (campaign.coordinatorName != null) ...[
                    Icon(Icons.person_rounded, size: 14, color: AppColors.surface.withOpacity(0.7)),
                    const SizedBox(width: 4),
                    Text(
                      'Coordinateur: ${campaign.coordinatorName}',
                      style: TextStyle(fontSize: 11, color: AppColors.surface.withOpacity(0.7)),
                    ),
                    const SizedBox(width: 16),
                  ],
                  if (campaign.targetGroups != null && campaign.targetGroups!.isNotEmpty) ...[
                    Container(
                      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 2),
                      decoration: BoxDecoration(
                        color: typeColor.withOpacity(0.1),
                        borderRadius: BorderRadius.circular(8),
                      ),
                      child: Text(
                        campaign.targetGroups!.first,
                        style: TextStyle(fontSize: 10, color: typeColor),
                      ),
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

  Color _getStatusColor(CampaignStatus status) {
    switch (status) {
      case CampaignStatus.planned:
        return Colors.blue;
      case CampaignStatus.active:
        return Colors.green;
      case CampaignStatus.completed:
        return Colors.purple;
      case CampaignStatus.cancelled:
        return Colors.red;
    }
  }

  Color _getTypeColor(CampaignType type) {
    switch (type) {
      case CampaignType.vaccination:
        return Colors.red;
      case CampaignType.screening:
        return Colors.blue;
      case CampaignType.awareness:
        return Colors.orange;
      case CampaignType.bloodDonation:
        return Colors.red;
      case CampaignType.healthCheck:
        return Colors.green;
      case CampaignType.nutrition:
        return Colors.amber;
      case CampaignType.maternalChild:
        return Colors.pink;
    }
  }

  IconData _getTypeIcon(CampaignType type) {
    switch (type) {
      case CampaignType.vaccination:
        return Icons.vaccines_rounded;
      case CampaignType.screening:
        return Icons.search_rounded;
      case CampaignType.awareness:
        return Icons.campaign_rounded;
      case CampaignType.bloodDonation:
        return Icons.favorite_rounded;
      case CampaignType.healthCheck:
        return Icons.health_and_safety_rounded;
      case CampaignType.nutrition:
        return Icons.restaurant_rounded;
      case CampaignType.maternalChild:
        return Icons.pregnant_woman_rounded;
    }
  }
}