import 'package:flutter/material.dart';
import '../../data/services/api_service.dart';
import '../../../presentation/widgets/glass_theme.dart';
import '../../../presentation/widgets/app_drawer.dart';

/// Détail du défi spirituel — branché sur /api/v1/spiritual-challenges/{id}/status.
class SpiritualChallengesDetailScreen extends StatefulWidget {
  final String challengeId;
  const SpiritualChallengesDetailScreen({super.key, required this.challengeId});

  @override
  State<SpiritualChallengesDetailScreen> createState() => _SpiritualChallengesDetailScreenState();
}

class _SpiritualChallengesDetailScreenState extends State<SpiritualChallengesDetailScreen> {
  final _apiService = ApiService();
  Map<String, dynamic>? _challenge;
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
      final res = await _apiService.get('/spiritual-challenges/${widget.challengeId}');
      if (mounted) {
        final data = res.data;
        setState(() {
          _challenge = data is Map<String, dynamic> ? data : null;
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
      await _apiService.post('/spiritual-challenges/${widget.challengeId}/status', data: {'status': newStatus});
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
      case 'TERMINE':
      case 'COMPLETED':
      case 'ACCOMPLI':
        return Colors.green;
      case 'EN_COURS':
      case 'EN COURS':
      case 'IN_PROGRESS':
        return Colors.blue;
      case 'EN_ATTENTE':
      case 'EN ATTENTE':
      case 'PENDING':
        return Colors.amber;
      case 'ABANDONNE':
      case 'ABANDONED':
        return Colors.red;
      default:
        return Colors.grey;
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Défi spirituel'),
        backgroundColor: AppColors.primary,
        foregroundColor: Colors.white,
        actions: [IconButton(icon: const Icon(Icons.refresh), onPressed: _loadData)],
      ),
      drawer: const AppDrawer(),
      body: _isLoading
          ? const ShimmerLoading(itemCount: 4)
          : _error != null
              ? _buildError()
              : _challenge == null
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
                            _buildActionsCard(),
                          ],
                        ),
                      ),
                    ),
    );
  }

  Widget _buildHeaderCard() {
    final c = _challenge!;
    final titre = c['titre']?.toString() ?? c['nom']?.toString() ?? 'Défi';
    final statut = c['statut']?.toString() ?? c['status']?.toString() ?? '';
    final categorie = c['categorie']?.toString() ?? c['category']?.toString() ?? '';
    final difficulte = c['difficulte']?.toString() ?? c['difficulty']?.toString() ?? '';
    final dateStr = c['createdAt']?.toString().substring(0, 10) ?? '';

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
                  color: Colors.indigo.withValues(alpha: 0.15),
                  borderRadius: BorderRadius.circular(12),
                ),
                child: const Icon(Icons.self_improvement, color: Colors.indigo, size: 24),
              ),
              const SizedBox(width: 12),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(titre,
                        style: const TextStyle(color: Colors.white, fontWeight: FontWeight.bold, fontSize: 18)),
                    if (dateStr.isNotEmpty)
                      Text(dateStr,
                          style: TextStyle(color: Colors.white.withValues(alpha: 0.4), fontSize: 12)),
                  ],
                ),
              ),
            ],
          ),
          const SizedBox(height: 12),
          Row(
            children: [
              if (statut.isNotEmpty)
                StatusBadge(label: statut, color: _statusColor(statut)),
              if (statut.isNotEmpty && categorie.isNotEmpty) const SizedBox(width: 8),
              if (categorie.isNotEmpty)
                StatusBadge(label: categorie, color: Colors.purple),
              if ((statut.isNotEmpty || categorie.isNotEmpty) && difficulte.isNotEmpty) const SizedBox(width: 8),
              if (difficulte.isNotEmpty)
                StatusBadge(label: difficulte, color: Colors.orange),
            ],
          ),
        ],
      ),
    );
  }

  Widget _buildContentCard() {
    final c = _challenge!;
    final description = c['description']?.toString() ?? '';
    final objectif = c['objectif']?.toString() ?? c['goal']?.toString() ?? '';
    final duree = c['duree']?.toString() ?? c['duration']?.toString() ?? '';
    final progression = (c['progression'] as num?)?.toDouble();

    return GlassCard(
      padding: const EdgeInsets.all(16),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const Text('Détails', style: TextStyle(color: Colors.white, fontWeight: FontWeight.bold, fontSize: 16)),
          const SizedBox(height: 12),
          if (description.isNotEmpty) ...[
            Text(description, style: TextStyle(color: Colors.white.withValues(alpha: 0.7), fontSize: 14, height: 1.5)),
            const SizedBox(height: 12),
          ],
          if (objectif.isNotEmpty) ...[
            _infoRow('Objectif', objectif),
          ],
          if (duree.isNotEmpty) ...[
            _infoRow('Durée', duree),
          ],
          if (progression != null) ...[
            const SizedBox(height: 8),
            Row(
              children: [
                Expanded(
                  child: ClipRRect(
                    borderRadius: BorderRadius.circular(4),
                    child: LinearProgressIndicator(
                      value: progression / 100,
                      minHeight: 8,
                      backgroundColor: Colors.white.withValues(alpha: 0.08),
                      valueColor: AlwaysStoppedAnimation(_statusColor(_challenge!['statut']?.toString() ?? _challenge!['status']?.toString())),
                    ),
                  ),
                ),
                const SizedBox(width: 8),
                Text('${progression.round()}%',
                    style: TextStyle(color: Colors.white.withValues(alpha: 0.6), fontSize: 12, fontWeight: FontWeight.bold)),
              ],
            ),
          ],
        ],
      ),
    );
  }

  Widget _buildActionsCard() {
    final c = _challenge!;
    final statut = c['statut']?.toString() ?? c['status']?.toString() ?? '';

    return GlassCard(
      padding: const EdgeInsets.all(16),
      borderColor: Colors.indigo.withValues(alpha: 0.3),
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
                if (statut.toUpperCase() != 'EN_COURS' && statut.toUpperCase() != 'IN_PROGRESS')
                  SizedBox(
                    width: double.infinity,
                    child: FilledButton.icon(
                      onPressed: () => _updateStatus('EN_COURS'),
                      icon: const Icon(Icons.play_arrow, size: 16),
                      label: const Text('Commencer'),
                      style: FilledButton.styleFrom(backgroundColor: Colors.blue),
                    ),
                  ),
                if (statut.toUpperCase() != 'TERMINE' && statut.toUpperCase() != 'COMPLETED' && statut.toUpperCase() != 'ACCOMPLI') ...[
                  const SizedBox(height: 8),
                  SizedBox(
                    width: double.infinity,
                    child: FilledButton.icon(
                      onPressed: () => _updateStatus('TERMINE'),
                      icon: const Icon(Icons.check_circle, size: 16),
                      label: const Text('Marquer comme terminé'),
                      style: FilledButton.styleFrom(backgroundColor: Colors.green),
                    ),
                  ),
                ],
                if (statut.toUpperCase() != 'ABANDONNE' && statut.toUpperCase() != 'ABANDONED') ...[
                  const SizedBox(height: 8),
                  SizedBox(
                    width: double.infinity,
                    child: OutlinedButton.icon(
                      onPressed: () => _updateStatus('ABANDONNE'),
                      icon: const Icon(Icons.cancel, size: 16),
                      label: const Text('Abandonner'),
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
            width: 100,
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
          Icon(Icons.self_improvement, size: 48, color: Colors.white.withValues(alpha: 0.2)),
          const SizedBox(height: 12),
          Text('Défi non trouvé', style: TextStyle(color: Colors.white.withValues(alpha: 0.5))),
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
