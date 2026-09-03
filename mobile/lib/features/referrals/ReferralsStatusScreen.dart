import 'package:flutter/material.dart';
import '../../data/services/api_service.dart';
import '../../../presentation/widgets/glass_theme.dart';
import '../../../presentation/widgets/app_drawer.dart';

/// Statut des parrainages — branché sur /api/v1/referrals/{id}/status.
class ReferralsStatusScreen extends StatefulWidget {
  final String referralId;
  const ReferralsStatusScreen({super.key, required this.referralId});

  @override
  State<ReferralsStatusScreen> createState() => _ReferralsStatusScreenState();
}

class _ReferralsStatusScreenState extends State<ReferralsStatusScreen> {
  final _apiService = ApiService();
  Map<String, dynamic>? _referral;
  bool _isLoading = true;
  String? _error;
  bool _isUpdating = false;

  @override
  void initState() {
    super.initState();
    _loadData();
  }

  Future<void> _loadData() async {
    setState(() {
      _isLoading = true;
      _error = null;
    });
    try {
      final res = await _apiService.get('/referrals/${widget.referralId}/status');
      if (mounted) {
        final data = res.data;
        setState(() {
          _referral = data is Map<String, dynamic> ? data : null;
          _isLoading = false;
        });
      }
    } catch (_) {
      if (mounted) {
        setState(() {
          _error = 'Erreur lors du chargement';
          _isLoading = false;
        });
      }
    }
  }

  Future<void> _updateStatus(String newStatus) async {
    setState(() => _isUpdating = true);
    try {
      await _apiService.post('/referrals/${widget.referralId}/status', data: {'status': newStatus});
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text('Statut mis à jour : $newStatus'), backgroundColor: Colors.green),
        );
        _loadData();
      }
    } catch (_) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('Erreur lors de la mise à jour'), backgroundColor: Colors.red),
        );
      }
    } finally {
      if (mounted) setState(() => _isUpdating = false);
    }
  }

  Color _statusColor(String? statut) {
    switch (statut?.toUpperCase()) {
      case 'CONVERTI':
      case 'CONVERTED':
        return Colors.green;
      case 'EN_COURS':
      case 'EN COURS':
      case 'IN_PROGRESS':
        return Colors.blue;
      case 'EN_ATTENTE':
      case 'EN ATTENTE':
      case 'PENDING':
        return Colors.amber;
      case 'EXPIRE':
      case 'EXPIRED':
        return Colors.red;
      default:
        return Colors.grey;
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Statut du parrainage'),
        backgroundColor: AppColors.primary,
        foregroundColor: Colors.white,
        actions: [IconButton(icon: const Icon(Icons.refresh), onPressed: _loadData)],
      ),
      drawer: const AppDrawer(),
      body: _isLoading
          ? const ShimmerLoading(itemCount: 4)
          : _error != null
              ? _buildError()
              : _referral == null
                  ? _buildEmpty()
                  : RefreshIndicator(
                      onRefresh: _loadData,
                      child: SingleChildScrollView(
                        padding: const EdgeInsets.all(16),
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            _buildHeaderCard(),
                            const SizedBox(height: 16),
                            _buildDetailsCard(),
                            const SizedBox(height: 16),
                            _buildActionsCard(),
                          ],
                        ),
                      ),
                    ),
    );
  }

  Widget _buildHeaderCard() {
    final r = _referral!;
    final statut = r['statut']?.toString() ?? r['status']?.toString() ?? '';
    final parrain = r['parrain']?.toString() ?? r['referrer']?.toString() ?? '';
    final filleul = r['filleul']?.toString() ?? r['referee']?.toString() ?? '';

    return GlassCard(
      padding: const EdgeInsets.all(16),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Container(
                width: 48, height: 48,
                decoration: BoxDecoration(
                  color: AppColors.primary.withValues(alpha: 0.15),
                  borderRadius: BorderRadius.circular(12),
                ),
                child: Icon(Icons.person_add, color: AppColors.primary, size: 24),
              ),
              const SizedBox(width: 12),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    if (parrain.isNotEmpty)
                      Text('Parrain : $parrain',
                          style: const TextStyle(color: Colors.white, fontWeight: FontWeight.bold, fontSize: 14)),
                    if (filleul.isNotEmpty)
                      Text('Filleul : $filleul',
                          style: TextStyle(color: Colors.white.withValues(alpha: 0.6), fontSize: 13)),
                  ],
                ),
              ),
            ],
          ),
          if (statut.isNotEmpty) ...[
            const SizedBox(height: 12),
            StatusBadge(label: statut, color: _statusColor(statut)),
          ],
        ],
      ),
    );
  }

  Widget _buildDetailsCard() {
    final r = _referral!;
    final dateCreation = r['createdAt']?.toString().substring(0, 10) ?? '';
    final dateConversion = r['convertedAt']?.toString().substring(0, 10) ?? '';
    final code = r['code']?.toString() ?? r['referralCode']?.toString() ?? '';

    return GlassCard(
      padding: const EdgeInsets.all(16),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const Text('Détails', style: TextStyle(color: Colors.white, fontWeight: FontWeight.bold, fontSize: 16)),
          const SizedBox(height: 12),
          _infoRow('Date de création', dateCreation),
          if (dateConversion.isNotEmpty) _infoRow('Date de conversion', dateConversion),
          if (code.isNotEmpty) _infoRow('Code de parrainage', code),
        ],
      ),
    );
  }

  Widget _buildActionsCard() {
    final r = _referral!;
    final statut = r['statut']?.toString() ?? r['status']?.toString() ?? '';

    return GlassCard(
      padding: const EdgeInsets.all(16),
      borderColor: AppColors.primary.withValues(alpha: 0.3),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const Text('Actions', style: TextStyle(color: Colors.white, fontWeight: FontWeight.bold, fontSize: 16)),
          const SizedBox(height: 12),
          if (_isUpdating)
            const Center(child: CircularProgressIndicator())
          else
            Column(
              children: [
                if (statut.toUpperCase() != 'CONVERTI' && statut.toUpperCase() != 'CONVERTED')
                  SizedBox(
                    width: double.infinity,
                    child: FilledButton.icon(
                      onPressed: () => _updateStatus('EN_COURS'),
                      icon: const Icon(Icons.play_arrow, size: 16),
                      label: const Text('Marquer en cours'),
                      style: FilledButton.styleFrom(backgroundColor: Colors.blue),
                    ),
                  ),
                if (statut.toUpperCase() != 'CONVERTI' && statut.toUpperCase() != 'CONVERTED') ...[
                  const SizedBox(height: 8),
                  SizedBox(
                    width: double.infinity,
                    child: FilledButton.icon(
                      onPressed: () => _updateStatus('CONVERTI'),
                      icon: const Icon(Icons.check_circle, size: 16),
                      label: const Text('Marquer comme converti'),
                      style: FilledButton.styleFrom(backgroundColor: Colors.green),
                    ),
                  ),
                ],
                if (statut.toUpperCase() != 'EXPIRE' && statut.toUpperCase() != 'EXPIRED') ...[
                  const SizedBox(height: 8),
                  SizedBox(
                    width: double.infinity,
                    child: OutlinedButton.icon(
                      onPressed: () => _updateStatus('EXPIRE'),
                      icon: const Icon(Icons.cancel, size: 16),
                      label: const Text('Marquer comme expiré'),
                      style: OutlinedButton.styleFrom(foregroundColor: Colors.red),
                    ),
                  ),
                ],
              ],
            ),
        ],
      ),
    );
  }

  Widget _infoRow(String label, String value) {
    if (value.isEmpty) return const SizedBox.shrink();
    return Padding(
      padding: const EdgeInsets.only(bottom: 8),
      child: Row(
        children: [
          SizedBox(
            width: 140,
            child: Text(label, style: TextStyle(color: Colors.white.withValues(alpha: 0.4), fontSize: 12)),
          ),
          Expanded(
            child: Text(value, style: const TextStyle(color: Colors.white, fontSize: 12, fontWeight: FontWeight.w500)),
          ),
        ],
      ),
    );
  }

  Widget _buildEmpty() {
    return Center(
      child: Column(
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          Icon(Icons.person_add, size: 48, color: Colors.white.withValues(alpha: 0.2)),
          const SizedBox(height: 12),
          Text('Parrainage non trouvé', style: TextStyle(color: Colors.white.withValues(alpha: 0.5))),
        ],
      ),
    );
  }

  Widget _buildError() {
    return Center(
      child: Column(
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          Icon(Icons.error_outline, color: Colors.white.withValues(alpha: 0.3), size: 48),
          const SizedBox(height: 12),
          Text(_error!, style: TextStyle(color: Colors.white.withValues(alpha: 0.5)), textAlign: TextAlign.center),
          const SizedBox(height: 12),
          FilledButton.icon(onPressed: _loadData, icon: const Icon(Icons.refresh, size: 16), label: const Text('Réessayer')),
        ],
      ),
    );
  }
}
