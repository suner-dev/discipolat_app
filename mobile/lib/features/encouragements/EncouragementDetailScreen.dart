import 'package:flutter/material.dart';
import '../../data/services/api_service.dart';
import '../../../presentation/widgets/glass_theme.dart';
import '../../../presentation/widgets/app_drawer.dart';

/// Détail d'encouragement IA — branché sur /api/v1/ai/encouragement/{soulId}.
class EncouragementDetailScreen extends StatefulWidget {
  final String soulId;
  const EncouragementDetailScreen({super.key, required this.soulId});

  @override
  State<EncouragementDetailScreen> createState() => _EncouragementDetailScreenState();
}

class _EncouragementDetailScreenState extends State<EncouragementDetailScreen> {
  final _apiService = ApiService();
  Map<String, dynamic>? _encouragement;
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
      final res = await _apiService.get('/ai/encouragement/${widget.soulId}');
      if (mounted) {
        final data = res.data;
        setState(() {
          _encouragement = data is Map<String, dynamic> ? data : null;
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
        title: const Text('Encouragement'),
        backgroundColor: AppColors.primary,
        foregroundColor: Colors.white,
        actions: [IconButton(icon: const Icon(Icons.refresh), onPressed: _loadData)],
      ),
      drawer: const AppDrawer(),
      body: _isLoading
          ? const ShimmerLoading(itemCount: 4)
          : _error != null
              ? _buildError()
              : _encouragement == null
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
                            _buildMessageCard(),
                          ],
                        ),
                      ),
                    ),
    );
  }

  Widget _buildHeaderCard() {
    final e = _encouragement!;
    final destinataire = e['destinataire']?.toString() ?? e['memberName']?.toString() ?? '';
    final type = e['type']?.toString() ?? '';
    final dateStr = e['createdAt']?.toString().substring(0, 10) ?? '';

    return GlassCard(
      padding: const EdgeInsets.all(16),
      child: Row(
        children: [
          Container(
            width: 48, height: 48,
            decoration: BoxDecoration(
              gradient: LinearGradient(
                colors: [AppColors.accent, AppColors.accentLight],
              ),
              borderRadius: BorderRadius.circular(12),
            ),
            child: const Center(child: Text('💪', style: TextStyle(fontSize: 24))),
          ),
          const SizedBox(width: 12),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                if (destinataire.isNotEmpty)
                  Text(destinataire,
                      style: const TextStyle(color: Colors.white, fontWeight: FontWeight.bold, fontSize: 16)),
                Row(
                  children: [
                    if (type.isNotEmpty) ...[
                      StatusBadge(label: type, color: Colors.purple),
                      const SizedBox(width: 8),
                    ],
                    if (dateStr.isNotEmpty)
                      Text(dateStr,
                          style: TextStyle(color: Colors.white.withValues(alpha: 0.4), fontSize: 11)),
                  ],
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildMessageCard() {
    final e = _encouragement!;
    final message = e['message']?.toString() ?? e['contenu']?.toString() ?? e['texte']?.toString() ?? '';
    final reason = e['raison']?.toString() ?? e['reason']?.toString() ?? '';

    return GlassCard(
      padding: const EdgeInsets.all(16),
      borderColor: AppColors.accent.withValues(alpha: 0.3),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Icon(Icons.auto_awesome, color: AppColors.accent, size: 18),
              const SizedBox(width: 8),
              const Text('Message d\'encouragement',
                  style: TextStyle(color: Colors.white, fontWeight: FontWeight.bold, fontSize: 16)),
            ],
          ),
          const SizedBox(height: 12),
          Text(message.isNotEmpty ? message : 'Aucun message',
              style: TextStyle(
                color: message.isNotEmpty ? Colors.white.withValues(alpha: 0.8) : Colors.white.withValues(alpha: 0.3),
                fontSize: 15,
                height: 1.6,
              )),
          if (reason.isNotEmpty) ...[
            const SizedBox(height: 16),
            Container(
              padding: const EdgeInsets.all(12),
              decoration: BoxDecoration(
                color: Colors.white.withValues(alpha: 0.04),
                borderRadius: BorderRadius.circular(8),
              ),
              child: Row(
                children: [
                  Icon(Icons.info_outline, color: Colors.white.withValues(alpha: 0.4), size: 16),
                  const SizedBox(width: 8),
                  Expanded(
                    child: Text(reason,
                        style: TextStyle(color: Colors.white.withValues(alpha: 0.5), fontSize: 12)),
                  ),
                ],
              ),
            ),
          ],
        ],
      ),
    );
  }

  Widget _buildEmpty() {
    return Center(
      child: Column(
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          Icon(Icons.favorite, size: 48, color: Colors.white.withValues(alpha: 0.2)),
          const SizedBox(height: 12),
          Text('Aucun encouragement disponible', style: TextStyle(color: Colors.white.withValues(alpha: 0.5))),
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
