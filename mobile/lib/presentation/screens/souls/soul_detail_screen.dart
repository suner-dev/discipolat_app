import 'package:flutter/material.dart';
import '../../../data/services/api_service.dart';
import '../../../data/models/soul.dart';
import '../../widgets/glass_theme.dart';

class SoulDetailScreen extends StatefulWidget {
  final String soulId;
  /// Permet d'injecter un ApiService mocké dans les tests widget.
  final ApiService? apiService;

  const SoulDetailScreen({super.key, required this.soulId, this.apiService});

  @override
  State<SoulDetailScreen> createState() => _SoulDetailScreenState();
}

class _SoulDetailScreenState extends State<SoulDetailScreen> {
  late final ApiService _apiService = widget.apiService ?? ApiService();
  Soul? _soul;
  Map<String, dynamic>? _pastoral360;
  Map<String, dynamic>? _spiritualScore;
  List<dynamic> _history = [];
  bool _isLoading = true;

  @override
  void initState() {
    super.initState();
    _loadData();
  }

  Future<void> _loadData() async {
    setState(() => _isLoading = true);
    try {
      final soulRes = await _apiService.get('/souls/${widget.soulId}');
      final p360Res = await _apiService.get('/souls/${widget.soulId}/pastoral-360');
      final scoreRes = await _apiService.get('/souls/${widget.soulId}/spiritual-score');
      final histRes = await _apiService.get('/souls/${widget.soulId}/history');
      if (mounted) {
        final baseSoul = Soul.fromJson(soulRes.data as Map<String, dynamic>);
        // Enrichir avec les infos d'encadrement de la fiche 360°
        final p360 = p360Res.data as Map<String, dynamic>?;
        final encadrement = p360?['encadrement'] as Map<String, dynamic>? ?? {};
        final spirituel = p360?['spirituel'] as Map<String, dynamic>? ?? {};
        final soul = baseSoul.withEncadrement(
          faiseurNom: encadrement['faiseurNom'] as String?,
          familleNom: encadrement['familleNom'] as String?,
          departementNom: encadrement['departementNom'] as String?,
          dateBapteme: spirituel['dateBapteme'] as String?,
        );
        setState(() {
          _soul = soul;
          _pastoral360 = p360;
          _spiritualScore = scoreRes.data as Map<String, dynamic>?;
          _history = (histRes.data is List ? histRes.data : []) as List<dynamic>;
          _isLoading = false;
        });
      }
    } catch (_) {
      if (mounted) setState(() => _isLoading = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: Text(_soul?.nomComplet ?? 'Détail'),
        actions: [
          IconButton(icon: const Icon(Icons.refresh), onPressed: _loadData),
        ],
      ),
      body: _isLoading
          ? const ShimmerLoading(itemCount: 5)
          : _soul == null
              ? const Center(child: Text('Âme non trouvée', style: TextStyle(color: Colors.white54)))
              : RefreshIndicator(
                  onRefresh: _loadData,
                  child: SingleChildScrollView(
                    padding: const EdgeInsets.all(12),
                    child: Column(
                      children: [
                        // Header
                        GlassCard(
                          padding: const EdgeInsets.all(24),
                          child: Column(
                            children: [
                              GradientAvatar(text: _soul!.nomComplet, radius: 40, showGlow: true),
                              const SizedBox(height: 12),
                              Text(_soul!.nomComplet, style: const TextStyle(color: Colors.white, fontSize: 20, fontWeight: FontWeight.bold)),
                              const SizedBox(height: 8),
                              StatusBadge(
                                label: _soul!.typeDisciple == 'NOUVEAU_CONVERTI' ? 'Converti' : 'Arrivant',
                                color: _soul!.typeDisciple == 'NOUVEAU_CONVERTI' ? Colors.green : Colors.blue,
                                glowing: true,
                              ),
                              const SizedBox(height: 8),
                              Text(_soul!.statut, style: TextStyle(color: Colors.white.withValues(alpha: 0.5), fontSize: 12)),
                            ],
                          ),
                        ),
                        const SizedBox(height: 12),

                        // Spiritual score
                        if (_spiritualScore != null) ...[
                          _sectionTitle('Score Spirituel', Icons.auto_awesome),
                          GlassCard(
                            padding: const EdgeInsets.all(16),
                            child: Column(
                              children: [
                                Row(
                                  mainAxisAlignment: MainAxisAlignment.spaceAround,
                                  children: [
                                    _scoreItem('Global', _spiritualScore!['scoreGlobal'] ?? '—', Colors.amber),
                                    _scoreItem('Présence', _spiritualScore!['presence'] ?? '—', Colors.green),
                                    _scoreItem('Fidélité', _spiritualScore!['fidelite'] ?? '—', Colors.blue),
                                  ],
                                ),
                                const SizedBox(height: 12),
                                Row(
                                  mainAxisAlignment: MainAxisAlignment.spaceAround,
                                  children: [
                                    _scoreItem('Engagement', _spiritualScore!['engagement'] ?? '—', Colors.purple),
                                    _scoreItem('Participation', _spiritualScore!['participation'] ?? '—', Colors.teal),
                                  ],
                                ),
                              ],
                            ),
                          ),
                          const SizedBox(height: 12),
                        ],

                        // Personal info
                        _sectionTitle('Informations personnelles', Icons.person),
                        GlassCard(
                          padding: const EdgeInsets.all(16),
                          child: Column(
                            children: [
                              _infoRow(Icons.email_outlined, 'Email', _soul!.email ?? '—'),
                              const GlassDivider(),
                              _infoRow(Icons.phone_outlined, 'Téléphone', _soul!.telephone ?? '—'),
                              const GlassDivider(),
                              _infoRow(Icons.calendar_today, 'Intégration', _soul!.dateIntegration),
                              const GlassDivider(),
                              _infoRow(Icons.cake, 'Date de naissance', _soul!.dateNaissance ?? '—'),
                            ],
                          ),
                        ),
                        const SizedBox(height: 12),

                        // Spiritual info
                        _sectionTitle('Parcours spirituel', Icons.church),
                        GlassCard(
                          padding: const EdgeInsets.all(16),
                          child: Column(
                            children: [
                              _infoRow(Icons.family_restroom, 'Famille', _soul!.familleNom ?? '—'),
                              const GlassDivider(),
                              _infoRow(Icons.eco, 'Faiseur', _soul!.faiseurNom ?? '—'),
                              const GlassDivider(),
                              _infoRow(Icons.business, 'Département', _soul!.departementNom ?? '—'),
                              if (_soul!.dateBapteme != null) ...[
                                const GlassDivider(),
                                _infoRow(Icons.water_drop, 'Baptême', _soul!.dateBapteme!),
                              ],
                            ],
                          ),
                        ),
                        const SizedBox(height: 12),

                        // Pastoral 360
                        if (_pastoral360 != null) ...[
                          _sectionTitle('Fiche 360°', Icons.visibility),
                          GlassCard(
                            padding: const EdgeInsets.all(16),
                            child: Column(
                              children: [
                                if (_pastoral360!['noteGenerale'] != null)
                                  _infoRow(Icons.star, 'Note générale', '${_pastoral360!['noteGenerale']}/5'),
                                if (_pastoral360!['pointsFort'] != null) ...[
                                  const GlassDivider(),
                                  _infoRow(Icons.thumb_up, 'Points forts', _pastoral360!['pointsFort']),
                                ],
                                if (_pastoral360!['pointsAmeliorer'] != null) ...[
                                  const GlassDivider(),
                                  _infoRow(Icons.trending_up, 'À améliorer', _pastoral360!['pointsAmeliorer']),
                                ],
                              ],
                            ),
                          ),
                          const SizedBox(height: 12),
                        ],

                        // History
                        if (_history.isNotEmpty) ...[
                          _sectionTitle('Historique', Icons.history),
                          ..._history.take(5).map((h) {
                            final entry = h as Map;
                            final type = entry['typeEvenement'] ?? entry['type'] ?? '—';
                            final desc = entry['description'] ?? '';
                            final date = entry['createdAt']?.toString().substring(0, 10) ?? '';
                            return GlassCard(
                              margin: const EdgeInsets.only(bottom: 8),
                              padding: const EdgeInsets.all(12),
                              child: Row(
                                children: [
                                  Container(
                                    padding: const EdgeInsets.all(6),
                                    decoration: BoxDecoration(
                                      color: Colors.blue.withValues(alpha: 0.15),
                                      borderRadius: BorderRadius.circular(8),
                                    ),
                                    child: const Icon(Icons.circle, color: Colors.blue, size: 8),
                                  ),
                                  const SizedBox(width: 10),
                                  Expanded(
                                    child: Column(
                                      crossAxisAlignment: CrossAxisAlignment.start,
                                      children: [
                                        Text(type, style: const TextStyle(color: Colors.white, fontWeight: FontWeight.w500, fontSize: 13)),
                                        if (desc.toString().isNotEmpty)
                                          Text(desc, style: TextStyle(color: Colors.white.withValues(alpha: 0.5), fontSize: 11), maxLines: 1, overflow: TextOverflow.ellipsis),
                                      ],
                                    ),
                                  ),
                                  Text(date, style: TextStyle(color: Colors.white.withValues(alpha: 0.3), fontSize: 10)),
                                ],
                              ),
                            );
                          }),
                        ],
                      ],
                    ),
                  ),
                ),
    );
  }

  Widget _sectionTitle(String title, IconData icon) {
    return Padding(
      padding: const EdgeInsets.only(bottom: 8),
      child: Row(
        children: [
          Icon(icon, size: 18, color: Colors.white.withValues(alpha: 0.6)),
          const SizedBox(width: 8),
          Text(title, style: TextStyle(color: Colors.white.withValues(alpha: 0.8), fontSize: 16, fontWeight: FontWeight.w600)),
        ],
      ),
    );
  }

  Widget _scoreItem(String label, dynamic value, Color color) {
    return Column(
      children: [
        Text('$value', style: TextStyle(color: color, fontSize: 22, fontWeight: FontWeight.bold)),
        Text(label, style: TextStyle(color: Colors.white.withValues(alpha: 0.5), fontSize: 10)),
      ],
    );
  }

  Widget _infoRow(IconData icon, String label, String value) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 6),
      child: Row(
        children: [
          Icon(icon, color: Colors.white38, size: 18),
          const SizedBox(width: 10),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(label, style: TextStyle(color: Colors.white.withValues(alpha: 0.4), fontSize: 11)),
                Text(value, style: const TextStyle(color: Colors.white, fontSize: 13)),
              ],
            ),
          ),
        ],
      ),
    );
  }
}
