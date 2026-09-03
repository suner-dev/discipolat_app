import 'package:flutter/material.dart';
import '../../data/services/api_service.dart';
import '../../../presentation/widgets/glass_theme.dart';
import '../../../presentation/widgets/app_drawer.dart';

/// Détail et modération d'un témoignage — branché sur /api/v1/testimonies/{id}.
class TestimonyDetailScreen extends StatefulWidget {
  final String testimonyId;
  const TestimonyDetailScreen({super.key, required this.testimonyId});

  @override
  State<TestimonyDetailScreen> createState() => _TestimonyDetailScreenState();
}

class _TestimonyDetailScreenState extends State<TestimonyDetailScreen> {
  final _apiService = ApiService();
  Map<String, dynamic>? _testimony;
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
      final res = await _apiService.get('/testimonies/${widget.testimonyId}');
      if (mounted) {
        final data = res.data;
        setState(() {
          _testimony = data is Map<String, dynamic> ? data : null;
          _isLoading = false;
        });
      }
    } catch (_) {
      if (mounted) {
        setState(() {
          _error = 'Erreur lors du chargement du témoignage';
          _isLoading = false;
        });
      }
    }
  }

  Color _statusColor(String? statut) {
    switch (statut?.toUpperCase()) {
      case 'APPROUVE':
      case 'APPROVED':
        return Colors.green;
      case 'REJETE':
      case 'REJECTED':
        return Colors.red;
      case 'EN_ATTENTE':
      case 'EN ATTENTE':
      case 'PENDING':
        return Colors.amber;
      default:
        return Colors.grey;
    }
  }

  Future<void> _moderate(String action) async {
    try {
      final endpoint = action == 'approve'
          ? '/testimonies/${widget.testimonyId}/approve'
          : '/testimonies/${widget.testimonyId}/reject';
      await _apiService.post(endpoint);
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text(action == 'approve' ? 'Témoignage approuvé' : 'Témoignage rejeté'),
            backgroundColor: action == 'approve' ? Colors.green : Colors.orange,
          ),
        );
        _loadData();
      }
    } catch (_) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('Erreur lors de la modération'), backgroundColor: Colors.red),
        );
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Détail du témoignage'),
        backgroundColor: AppColors.primary,
        foregroundColor: Colors.white,
        actions: [IconButton(icon: const Icon(Icons.refresh), onPressed: _loadData)],
      ),
      drawer: const AppDrawer(),
      body: _isLoading
          ? const ShimmerLoading(itemCount: 4)
          : _error != null
              ? _buildError()
              : _testimony == null
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
                            _buildContentCard(),
                            const SizedBox(height: 16),
                            if (_testimony!['statut']?.toString().toUpperCase() == 'EN_ATTENTE' ||
                                _testimony!['status']?.toString().toUpperCase() == 'PENDING')
                              _buildModerationCard(),
                          ],
                        ),
                      ),
                    ),
    );
  }

  Widget _buildHeaderCard() {
    final t = _testimony!;
    final auteur = t['auteur']?.toString() ?? t['author']?.toString() ?? t['membreName']?.toString() ?? '';
    final statut = t['statut']?.toString() ?? t['status']?.toString() ?? 'EN_ATTENTE';
    final dateStr = t['createdAt']?.toString().substring(0, 10) ?? '';
    final theme = t['theme']?.toString() ?? t['categorie']?.toString() ?? '';

    return GlassCard(
      padding: const EdgeInsets.all(16),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Container(
                width: 48,
                height: 48,
                decoration: BoxDecoration(
                  color: AppColors.primary.withValues(alpha: 0.15),
                  borderRadius: BorderRadius.circular(12),
                ),
                child: Icon(Icons.auto_stories, color: AppColors.primary, size: 24),
              ),
              const SizedBox(width: 12),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    if (auteur.isNotEmpty)
                      Text(auteur,
                          style: const TextStyle(color: Colors.white, fontWeight: FontWeight.bold, fontSize: 16)),
                    if (dateStr.isNotEmpty)
                      Text(dateStr,
                          style: TextStyle(color: Colors.white.withValues(alpha: 0.4), fontSize: 12)),
                  ],
                ),
              ),
              StatusBadge(label: statut, color: _statusColor(statut)),
            ],
          ),
          if (theme.isNotEmpty) ...[
            const SizedBox(height: 12),
            StatusBadge(label: theme, color: Colors.purple),
          ],
        ],
      ),
    );
  }

  Widget _buildContentCard() {
    final t = _testimony!;
    final contenu = t['contenu']?.toString() ?? t['texte']?.toString() ?? t['content']?.toString() ?? '';

    return GlassCard(
      padding: const EdgeInsets.all(16),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const Text('Témoignage',
              style: TextStyle(color: Colors.white, fontWeight: FontWeight.bold, fontSize: 16)),
          const SizedBox(height: 12),
          Text(contenu.isNotEmpty ? contenu : 'Aucun contenu',
              style: TextStyle(
                color: contenu.isNotEmpty ? Colors.white.withValues(alpha: 0.8) : Colors.white.withValues(alpha: 0.3),
                fontSize: 14,
                height: 1.5,
              )),
        ],
      ),
    );
  }

  Widget _buildModerationCard() {
    return GlassCard(
      padding: const EdgeInsets.all(16),
      borderColor: Colors.amber.withValues(alpha: 0.3),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Icon(Icons.gavel, color: Colors.amber, size: 18),
              const SizedBox(width: 8),
              const Text('Modération',
                  style: TextStyle(color: Colors.white, fontWeight: FontWeight.bold, fontSize: 16)),
            ],
          ),
          const SizedBox(height: 12),
          Text('Ce témoignage attend votre validation.',
              style: TextStyle(color: Colors.white.withValues(alpha: 0.6), fontSize: 13)),
          const SizedBox(height: 16),
          Row(
            children: [
              Expanded(
                child: FilledButton.icon(
                  onPressed: () => _moderate('approve'),
                  icon: const Icon(Icons.check, size: 16),
                  label: const Text('Approuver'),
                  style: FilledButton.styleFrom(backgroundColor: Colors.green),
                ),
              ),
              const SizedBox(width: 8),
              Expanded(
                child: OutlinedButton.icon(
                  onPressed: () => _moderate('reject'),
                  icon: const Icon(Icons.close, size: 16),
                  label: const Text('Rejeter'),
                  style: OutlinedButton.styleFrom(foregroundColor: Colors.red),
                ),
              ),
            ],
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
          Icon(Icons.auto_stories, size: 48, color: Colors.white.withValues(alpha: 0.2)),
          const SizedBox(height: 12),
          Text('Témoignage non trouvé', style: TextStyle(color: Colors.white.withValues(alpha: 0.5))),
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
