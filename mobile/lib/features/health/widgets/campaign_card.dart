import 'package:flutter/material.dart';

import 'package:discipolat_mobile/features/health/models/health_model.dart';
import 'package:discipolat_mobile/presentation/widgets/glass_theme.dart';

/// Fiche campagne — vue aplatie V240 (`responsibleName`, `participantsCount`
/// compté serveur, dates `yyyy-MM-dd`).
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
    final statusColor = _statusColor(campaign.status);
    final dateRange = _dateRange();

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
                    backgroundColor: Colors.green.withOpacity(0.2),
                    child: const Icon(Icons.campaign_rounded,
                        color: Colors.green, size: 24),
                  ),
                  const SizedBox(width: 12),
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(
                          campaign.title ?? 'Campagne sans titre',
                          style: Theme.of(context)
                              .textTheme
                              .titleMedium
                              ?.copyWith(fontWeight: FontWeight.w600),
                          maxLines: 1,
                          overflow: TextOverflow.ellipsis,
                        ),
                        const SizedBox(height: 2),
                        Text(
                          campaign.campaignType.displayName,
                          style: TextStyle(
                              fontSize: 12,
                              color: AppColors.surface.withOpacity(0.7)),
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
                      campaign.status.displayName,
                      style: TextStyle(
                          fontSize: 10,
                          fontWeight: FontWeight.bold,
                          color: statusColor),
                    ),
                  ),
                ],
              ),
              if (campaign.description != null) ...[
                const SizedBox(height: 12),
                Text(
                  campaign.description!,
                  style: TextStyle(
                      fontSize: 12,
                      color: AppColors.surface.withOpacity(0.85)),
                  maxLines: 2,
                  overflow: TextOverflow.ellipsis,
                ),
              ],
              const SizedBox(height: 12),
              Wrap(
                spacing: 8,
                runSpacing: 8,
                children: [
                  if (dateRange != null)
                    _badge(Icons.event_rounded, Colors.blue, dateRange),
                  if (campaign.lieu != null)
                    _badge(Icons.location_on_rounded, Colors.teal,
                        campaign.lieu!),
                  _badge(
                    Icons.groups_rounded,
                    Colors.green,
                    '${campaign.participantsCount} inscrit(s)',
                  ),
                  if (campaign.responsibleName != null)
                    _badge(Icons.person_rounded, Colors.purple,
                        'Resp. ${campaign.responsibleName}'),
                ],
              ),
              if (campaign.objectif != null) ...[
                const SizedBox(height: 8),
                Text(
                  'Objectif : ${campaign.objectif}',
                  style: TextStyle(
                      fontSize: 12, color: AppColors.surface.withOpacity(0.7)),
                  maxLines: 1,
                  overflow: TextOverflow.ellipsis,
                ),
              ],
            ],
          ),
        ),
      ),
    );
  }

  String? _dateRange() {
    final start = campaign.startDate;
    final end = campaign.endDate;
    if (start == null && end == null) return null;
    if (start != null && end != null) {
      return '${_prettyDate(start)} → ${_prettyDate(end)}';
    }
    return _prettyDate((start ?? end)!);
  }

  /// `yyyy-MM-dd` (LocalDate serveur) → `dd/MM/yyyy`, tolérant au parsing.
  static String _prettyDate(String iso) {
    final parsed = DateTime.tryParse(iso);
    if (parsed == null) return iso;
    final m = parsed.month.toString().padLeft(2, '0');
    final d = parsed.day.toString().padLeft(2, '0');
    return '$d/$m/${parsed.year}';
  }

  Widget _badge(IconData icon, Color color, String label) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 5),
      decoration: BoxDecoration(
        color: color.withOpacity(0.1),
        borderRadius: BorderRadius.circular(20),
        border: Border.all(color: color.withOpacity(0.3)),
      ),
      child: Row(
        mainAxisSize: MainAxisSize.min,
        children: [
          Icon(icon, size: 12, color: color),
          const SizedBox(width: 4),
          Text(
            label,
            style: TextStyle(fontSize: 11, fontWeight: FontWeight.w500, color: color),
          ),
        ],
      ),
    );
  }

  Color _statusColor(CampaignStatus status) {
    switch (status) {
      case CampaignStatus.planned:
        return Colors.blue;
      case CampaignStatus.inProgress:
        return Colors.green;
      case CampaignStatus.completed:
        return Colors.teal;
      case CampaignStatus.cancelled:
        return Colors.grey;
    }
  }
}
