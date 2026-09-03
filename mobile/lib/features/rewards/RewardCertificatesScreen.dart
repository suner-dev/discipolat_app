import 'package:flutter/material.dart';
import '../../data/services/api_service.dart';
import '../../../presentation/widgets/glass_theme.dart';
import '../../../presentation/widgets/app_drawer.dart';

/// Certificats de récompense — branché sur /api/v1/reward-certificates.
class RewardCertificatesScreen extends StatefulWidget {
  const RewardCertificatesScreen({super.key});

  @override
  State<RewardCertificatesScreen> createState() => _RewardCertificatesScreenState();
}

class _RewardCertificatesScreenState extends State<RewardCertificatesScreen> {
  final _apiService = ApiService();
  List<dynamic> _certificates = [];
  List<dynamic> _eligible = [];
  bool _isLoading = true;
  String? _error;

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
      final results = await Future.wait([
        _apiService.get('/reward-certificates'),
        _apiService.get('/reward-certificates/eligible'),
      ]);
      if (mounted) {
        final cData = results[0].data;
        final eData = results[1].data;
        setState(() {
          _certificates = cData is List ? cData : (cData is Map && cData['content'] is List ? cData['content'] as List<dynamic> : <dynamic>[]);
          _eligible = eData is List ? eData : (eData is Map && eData['content'] is List ? eData['content'] as List<dynamic> : <dynamic>[]);
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

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Certificats de récompense'),
        backgroundColor: Colors.amber.shade700,
        foregroundColor: Colors.white,
        actions: [IconButton(icon: const Icon(Icons.refresh), onPressed: _loadData)],
      ),
      drawer: const AppDrawer(),
      body: _isLoading
          ? const ShimmerLoading(itemCount: 5)
          : _error != null
              ? _buildError()
              : RefreshIndicator(
                  onRefresh: _loadData,
                  child: ListView(
                    padding: const EdgeInsets.all(16),
                    children: [
                      if (_eligible.isNotEmpty) ...[
                        Row(
                          children: [
                            Icon(Icons.stars, color: Colors.amber, size: 20),
                            const SizedBox(width: 8),
                            const Text('Éligibles', style: TextStyle(color: Colors.white, fontWeight: FontWeight.bold, fontSize: 16)),
                          ],
                        ),
                        const SizedBox(height: 8),
                        ..._eligible.map((e) {
                          final cert = e as Map<String, dynamic>;
                          return GlassCard(
                            margin: const EdgeInsets.only(bottom: 8),
                            padding: const EdgeInsets.all(14),
                            borderColor: Colors.amber.withValues(alpha: 0.3),
                            child: Row(
                              children: [
                                Icon(Icons.emoji_events, color: Colors.amber, size: 28),
                                const SizedBox(width: 12),
                                Expanded(
                                  child: Column(
                                    crossAxisAlignment: CrossAxisAlignment.start,
                                    children: [
                                      Text(cert['nom']?.toString() ?? cert['title']?.toString() ?? 'Récompense',
                                          style: const TextStyle(color: Colors.white, fontWeight: FontWeight.w600, fontSize: 14)),
                                      Text(cert['description']?.toString() ?? '',
                                          style: TextStyle(color: Colors.white.withValues(alpha: 0.5), fontSize: 12),
                                          maxLines: 2,
                                          overflow: TextOverflow.ellipsis),
                                    ],
                                  ),
                                ),
                                StatusBadge(label: 'Éligible', color: Colors.amber),
                              ],
                            ),
                          );
                        }),
                        const SizedBox(height: 16),
                      ],
                      Row(
                        children: [
                          Icon(Icons.workspace_premium, color: Colors.white.withValues(alpha: 0.5), size: 20),
                          const SizedBox(width: 8),
                          Text('Mes certificats', style: TextStyle(color: Colors.white.withValues(alpha: 0.7), fontWeight: FontWeight.bold, fontSize: 16)),
                        ],
                      ),
                      const SizedBox(height: 8),
                      if (_certificates.isEmpty)
                        _buildEmpty()
                      else
                        ..._certificates.map((c) {
                          final cert = c as Map<String, dynamic>;
                          final dateStr = cert['createdAt']?.toString().substring(0, 10) ?? '';
                          return GlassCard(
                            margin: const EdgeInsets.only(bottom: 8),
                            padding: const EdgeInsets.all(14),
                            child: Row(
                              children: [
                                Icon(Icons.workspace_premium, color: Colors.green, size: 28),
                                const SizedBox(width: 12),
                                Expanded(
                                  child: Column(
                                    crossAxisAlignment: CrossAxisAlignment.start,
                                    children: [
                                      Text(cert['nom']?.toString() ?? cert['title']?.toString() ?? 'Certificat',
                                          style: const TextStyle(color: Colors.white, fontWeight: FontWeight.w600, fontSize: 14)),
                                      if (dateStr.isNotEmpty)
                                        Text(dateStr,
                                            style: TextStyle(color: Colors.white.withValues(alpha: 0.4), fontSize: 11)),
                                    ],
                                  ),
                                ),
                                const Icon(Icons.check_circle, color: Colors.green, size: 20),
                              ],
                            ),
                          );
                        }),
                    ],
                  ),
                ),
    );
  }

  Widget _buildEmpty() {
    return Center(
      child: Padding(
        padding: const EdgeInsets.all(24),
        child: Column(
          children: [
            Icon(Icons.workspace_premium, size: 48, color: Colors.white.withValues(alpha: 0.2)),
            const SizedBox(height: 12),
            Text('Aucun certificat obtenu', style: TextStyle(color: Colors.white.withValues(alpha: 0.5))),
          ],
        ),
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
