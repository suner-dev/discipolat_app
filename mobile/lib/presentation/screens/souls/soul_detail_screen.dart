import 'package:flutter/material.dart';
import '../../../data/services/api_service.dart';
import '../../../data/models/soul.dart';
import '../../widgets/glass_theme.dart';

class SoulDetailScreen extends StatefulWidget {
  final String soulId;
  const SoulDetailScreen({super.key, required this.soulId});

  @override
  State<SoulDetailScreen> createState() => _SoulDetailScreenState();
}

class _SoulDetailScreenState extends State<SoulDetailScreen> {
  final _apiService = ApiService();
  Soul? _soul;
  bool _isLoading = true;

  @override
  void initState() {
    super.initState();
    _loadSoul();
  }

  Future<void> _loadSoul() async {
    try {
      final response = await _apiService.get('/souls/${widget.soulId}');
      if (mounted) setState(() { _soul = Soul.fromJson(response.data as Map<String, dynamic>); _isLoading = false; });
    } catch (e) { if (mounted) setState(() => _isLoading = false); }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: Text(_soul?.nomComplet ?? 'Détail')),
      body: _isLoading
          ? const ShimmerLoading(itemCount: 3)
          : _soul == null
              ? const Center(child: Text('Âme non trouvée', style: TextStyle(color: Colors.white54)))
              : SingleChildScrollView(
                  padding: const EdgeInsets.all(16),
                  child: Column(children: [
                    // Header card
                    GlassCard(
                      padding: const EdgeInsets.all(24),
                      child: Column(children: [
                        GradientAvatar(text: _soul!.nomComplet, radius: 40, showGlow: true),
                        const SizedBox(height: 16),
                        Text(_soul!.nomComplet, style: const TextStyle(color: Colors.white, fontSize: 22, fontWeight: FontWeight.bold)),
                        const SizedBox(height: 8),
                        StatusBadge(
                          label: _soul!.typeDisciple == 'NOUVEAU_CONVERTI' ? 'Nouveau converti' : 'Nouvel arrivant',
                          color: _soul!.typeDisciple == 'NOUVEAU_CONVERTI' ? Colors.green : Colors.blue,
                          glowing: true,
                        ),
                      ]),
                    ),
                    const SizedBox(height: 16),

                    // Info card
                    GlassCard(
                      padding: const EdgeInsets.all(16),
                      child: Column(children: [
                        _infoRow(Icons.email_outlined, 'Email', _soul!.email ?? '-'),
                        const GlassDivider(),
                        _infoRow(Icons.phone_outlined, 'Téléphone', _soul!.telephone ?? '-'),
                        const GlassDivider(),
                        _infoRow(Icons.calendar_today, 'Date dintégration', _soul!.dateIntegration),
                      ]),
                    ),
                    const SizedBox(height: 16),

                    // Spiritual info
                    GlassCard(
                      padding: const EdgeInsets.all(16),
                      child: Column(children: [
                        _infoRow(Icons.auto_awesome, 'Type', _soul!.typeDisciple == 'NOUVEAU_CONVERTI' ? 'Nouveau converti' : 'Nouvel arrivant'),
                        const GlassDivider(),
                        _infoRow(Icons.monitor_heart, 'Statut', _soul!.statut),
                      ]),
                    ),
                  ]),
                ),
    );
  }

  Widget _infoRow(IconData icon, String label, String value) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 8),
      child: Row(children: [
        Icon(icon, color: Colors.white38, size: 20),
        const SizedBox(width: 12),
        Expanded(
          child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
            Text(label, style: TextStyle(color: Colors.white.withValues(alpha: 0.4), fontSize: 11)),
            const SizedBox(height: 2),
            Text(value, style: const TextStyle(color: Colors.white, fontSize: 14)),
          ]),
        ),
      ]),
    );
  }
}
