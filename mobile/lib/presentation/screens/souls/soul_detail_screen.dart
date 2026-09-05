import 'dart:math';
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
  Map<String, dynamic>? _spiritualScoreDetail;
  List<dynamic> _scoreHistory = [];
  List<dynamic> _history = [];
  Map<String, dynamic>? _aiAnalysis;
  bool _isLoading = true;
  bool _isLoadingAI = false;
  String? _aiEncouragement;

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
      Map<String, dynamic>? scoreDetail;
      try {
        final sdRes = await _apiService.get('/souls/${widget.soulId}/spiritual-score-detail');
        scoreDetail = sdRes.data as Map<String, dynamic>?;
      } catch (_) {}
      List<dynamic> scoreHist = [];
      try {
        final shRes = await _apiService.get('/souls/${widget.soulId}/spiritual-score/history');
        scoreHist = (shRes.data as List?) ?? [];
      } catch (_) {}
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
          _spiritualScoreDetail = scoreDetail;
          _scoreHistory = scoreHist;
          _history = (histRes.data is List ? histRes.data : []) as List<dynamic>;
          _isLoading = false;
        });
        _loadAIAnalysis();
      }
    } catch (_) {
      if (mounted) setState(() => _isLoading = false);
    }
  }

  Future<void> _loadAIAnalysis() async {
    if (!mounted) return;
    setState(() => _isLoadingAI = true);
    try {
      final aiRes = await _apiService.get('/ai/analyze/${widget.soulId}');
      final encRes = await _apiService.get('/ai/encouragement/${widget.soulId}');
      if (mounted) {
        setState(() {
          _aiAnalysis = aiRes.data as Map<String, dynamic>?;
          _aiEncouragement = (encRes.data as Map<String, dynamic>?)?['encouragement'] as String?;
          _isLoadingAI = false;
        });
      }
    } catch (_) {
      if (mounted) setState(() => _isLoadingAI = false);
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
                          _buildSpiritualScoreCard(),
                          const SizedBox(height: 12),
                        ],

                        // AI Pastorale analysis
                        _sectionTitle('Analyse IA Pastorale', Icons.psychology),
                        _buildAISection(),
                        const SizedBox(height: 12),

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

  Widget _buildAISection() {
    if (_isLoadingAI) {
      return GlassCard(
        padding: const EdgeInsets.all(20),
        child: const Row(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            SizedBox(width: 20, height: 20, child: CircularProgressIndicator(strokeWidth: 2)),
            SizedBox(width: 12),
            Text('Analyse IA en cours...', style: TextStyle(color: Colors.white54)),
          ],
        ),
      );
    }

    final analysis = _aiAnalysis;
    final signaux = (analysis?['signaux'] as List?) ?? [];
    final suggestions = (analysis?['suggestions'] as List?) ?? [];
    final encouragement = _aiEncouragement ?? analysis?['encouragement']?.toString() ?? '';

    if (analysis == null && signaux.isEmpty) {
      return GlassCard(
        padding: const EdgeInsets.all(16),
        child: Column(
          children: [
            Icon(Icons.psychology, size: 40, color: Colors.white.withValues(alpha: 0.2)),
            const SizedBox(height: 8),
            Text('Appuyez pour lancer l\'analyse IA', style: TextStyle(color: Colors.white.withValues(alpha: 0.4), fontSize: 13)),
            const SizedBox(height: 12),
            OutlinedButton.icon(
              onPressed: _loadAIAnalysis,
              icon: const Icon(Icons.auto_awesome, size: 16),
              label: const Text('Analyser'),
            ),
          ],
        ),
      );
    }

    return GlassCard(
      padding: const EdgeInsets.all(16),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          // Signals
          if (signaux.isNotEmpty) ...[
            Text('Signaux détectés', style: TextStyle(color: Colors.amber[400], fontWeight: FontWeight.bold, fontSize: 13)),
            const SizedBox(height: 6),
            ...signaux.map((s) {
              final message = s['message']?.toString() ?? '';
              final gravite = s['gravite']?.toString() ?? 'INFO';
              final color = gravite == 'CRITIQUE' ? Colors.red :
                            gravite == 'ALERTE' ? Colors.orange : Colors.amber;
              return Padding(
                padding: const EdgeInsets.only(bottom: 4),
                child: Row(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Icon(Icons.warning_amber_rounded, size: 14, color: color),
                    const SizedBox(width: 6),
                    Expanded(child: Text(message, style: TextStyle(color: color.withValues(alpha: 0.9), fontSize: 12))),
                  ],
                ),
              );
            }),
            const SizedBox(height: 12),
          ],

          // Suggestions
          if (suggestions.isNotEmpty) ...[
            Text('Actions suggérées', style: TextStyle(color: Colors.cyan[400], fontWeight: FontWeight.bold, fontSize: 13)),
            const SizedBox(height: 6),
            ...suggestions.map((s) {
              final action = s['action']?.toString() ?? '';
              final priorite = s['priorite']?.toString() ?? 'NORMALE';
              final color = priorite == 'HAUTE' ? Colors.red : Colors.cyan;
              return Padding(
                padding: const EdgeInsets.only(bottom: 4),
                child: Row(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Icon(Icons.check_circle_outline, size: 14, color: color),
                    const SizedBox(width: 6),
                    Expanded(child: Text(action, style: TextStyle(color: Colors.white.withValues(alpha: 0.8), fontSize: 12))),
                  ],
                ),
              );
            }),
            const SizedBox(height: 12),
          ],

          // Encouragement
          if (encouragement.isNotEmpty) ...[
            Container(
              padding: const EdgeInsets.all(10),
              decoration: BoxDecoration(
                color: Colors.green.withValues(alpha: 0.08),
                borderRadius: BorderRadius.circular(8),
                border: Border.all(color: Colors.green.withValues(alpha: 0.2)),
              ),
              child: Row(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  const Icon(Icons.auto_awesome, size: 16, color: Colors.green),
                  const SizedBox(width: 8),
                  Expanded(child: Text(encouragement, style: TextStyle(color: Colors.green.withValues(alpha: 0.9), fontSize: 12, fontStyle: FontStyle.italic))),
                ],
              ),
            ),
          ],

          // Refresh button
          const SizedBox(height: 10),
          Center(
            child: TextButton.icon(
              onPressed: _loadAIAnalysis,
              icon: const Icon(Icons.refresh, size: 14),
              label: const Text('Rafraîchir l\'analyse', style: TextStyle(fontSize: 12)),
            ),
          ),
        ],
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

  Widget _buildSpiritualScoreCard() {
    final score = _spiritualScore!;
    final global = (score['global'] ?? score['scoreGlobal'] ?? 0) as num;
    final sante = (score['sante'] ?? 0) as num;
    final fidelite = (score['fidelite'] ?? 0) as num;
    final engagement = (score['engagement'] ?? 0) as num;
    final participation = (score['participation'] ?? 0) as num;
    final label = score['label']?.toString() ?? '';
    final semaine = score['semaine']?.toString() ?? '';
    final scoreColor = global >= 80 ? Colors.green
        : global >= 65 ? Colors.lightGreen
        : global >= 45 ? Colors.amber
        : global >= 25 ? Colors.orange
        : Colors.red;

    return GlassCard(
      padding: const EdgeInsets.all(16),
      child: Column(
        children: [
          // Gauge + label
          Row(
            children: [
              // Circular gauge
              SizedBox(
                width: 80,
                height: 80,
                child: Stack(
                  alignment: Alignment.center,
                  children: [
                    SizedBox(
                      width: 80, height: 80,
                      child: CircularProgressIndicator(
                        value: global.toDouble() / 100,
                        strokeWidth: 8,
                        backgroundColor: Colors.white.withValues(alpha: 0.1),
                        valueColor: AlwaysStoppedAnimation(scoreColor),
                        strokeCap: StrokeCap.round,
                      ),
                    ),
                    Column(
                      mainAxisSize: MainAxisSize.min,
                      children: [
                        Text('${global.toInt()}',
                            style: TextStyle(color: scoreColor, fontSize: 24, fontWeight: FontWeight.bold)),
                        Text(label.replaceAll('_', ' '),
                            style: TextStyle(color: Colors.white.withValues(alpha: 0.6), fontSize: 8, fontWeight: FontWeight.w600)),
                      ],
                    ),
                  ],
                ),
              ),
              const SizedBox(width: 16),
              // Sub-scores
              Expanded(
                child: Column(
                  children: [
                    _subScoreBar('Santé', sante, Colors.green),
                    const SizedBox(height: 6),
                    _subScoreBar('Fidélité', fidelite, Colors.blue),
                    const SizedBox(height: 6),
                    _subScoreBar('Engagement', engagement, Colors.purple),
                    const SizedBox(height: 6),
                    _subScoreBar('Participation', participation, Colors.teal),
                  ],
                ),
              ),
            ],
          ),
          if (semaine.isNotEmpty) ...[
            const SizedBox(height: 8),
            Text('Semaine du $semaine',
                style: TextStyle(color: Colors.white.withValues(alpha: 0.3), fontSize: 10)),
          ],
          // Detailed breakdown from spiritual-score-detail endpoint
          if (_spiritualScoreDetail != null) ...[
            const SizedBox(height: 12),
            const GlassDivider(),
            const SizedBox(height: 8),
            Text('Détail du score', style: TextStyle(color: Colors.white.withValues(alpha: 0.7), fontSize: 12, fontWeight: FontWeight.w600)),
            const SizedBox(height: 6),
            ..._buildScoreDetailRows(_spiritualScoreDetail!),
          ],
          // History sparkline
          if (_scoreHistory.length > 1) ...[
            const SizedBox(height: 12),
            SizedBox(
              height: 40,
              child: CustomPaint(
                painter: _SparklinePainter(
                  data: _scoreHistory.map((h) => (h['global'] as num?)?.toDouble() ?? 0).toList(),
                  color: scoreColor,
                ),
                size: const Size(double.infinity, 40),
              ),
            ),
          ],
        ],
      ),
    );
  }

  List<Widget> _buildScoreDetailRows(Map<String, dynamic> detail) {
    final breakdown = detail['breakdown'] ?? detail['details'];
    if (breakdown is Map) {
      return breakdown.entries.map((e) {
        final label = e.key.toString().replaceAll('_', ' ').toLowerCase();
        final value = e.value;
        final val = value is num ? value : double.tryParse('$value') ?? 0;
        final color = val >= 80 ? Colors.green : val >= 45 ? Colors.amber : Colors.red;
        return Padding(
          padding: const EdgeInsets.symmetric(vertical: 2),
          child: Row(
            children: [
              SizedBox(
                width: 90,
                child: Text(label, style: TextStyle(color: Colors.white.withValues(alpha: 0.6), fontSize: 10)),
              ),
              Expanded(
                child: ClipRRect(
                  borderRadius: BorderRadius.circular(4),
                  child: LinearProgressIndicator(
                    value: val.toDouble().clamp(0.0, 100.0) / 100,
                    minHeight: 6,
                    backgroundColor: Colors.white.withValues(alpha: 0.08),
                    valueColor: AlwaysStoppedAnimation(color),
                  ),
                ),
              ),
              const SizedBox(width: 6),
              Text('${val.toInt()}',
                  style: TextStyle(color: color, fontSize: 11, fontWeight: FontWeight.bold)),
            ],
          ),
        );
      }).toList();
    }
    final criteres = detail['criteres'];
    if (criteres is List) {
      return criteres.map((c) {
        final cMap = c as Map<String, dynamic>;
        final label = (cMap['critere'] ?? cMap['label'] ?? '')?.toString() ?? '';
        final val = cMap['score'] is num ? cMap['score'] as num : (double.tryParse('${cMap['score']}') ?? 0);
        final color = val >= 80 ? Colors.green : val >= 45 ? Colors.amber : Colors.red;
        return Padding(
          padding: const EdgeInsets.symmetric(vertical: 2),
          child: Row(
            children: [
              SizedBox(
                width: 90,
                child: Text(label, style: TextStyle(color: Colors.white.withValues(alpha: 0.6), fontSize: 10)),
              ),
              Expanded(
                child: ClipRRect(
                  borderRadius: BorderRadius.circular(4),
                  child: LinearProgressIndicator(
                    value: val.toDouble().clamp(0.0, 100.0) / 100,
                    minHeight: 6,
                    backgroundColor: Colors.white.withValues(alpha: 0.08),
                    valueColor: AlwaysStoppedAnimation(color),
                  ),
                ),
              ),
              const SizedBox(width: 6),
              Text('${val.toInt()}',
                  style: TextStyle(color: color, fontSize: 11, fontWeight: FontWeight.bold)),
            ],
          ),
        );
      }).toList();
    }
    return [];
  }

  Widget _subScoreBar(String label, num value, Color color) {    return Row(
      children: [
        SizedBox(
          width: 70,
          child: Text(label, style: TextStyle(color: Colors.white.withValues(alpha: 0.6), fontSize: 10)),
        ),
        Expanded(
          child: ClipRRect(
            borderRadius: BorderRadius.circular(4),
            child: LinearProgressIndicator(
              value: value.toDouble() / 100,
              minHeight: 6,
              backgroundColor: Colors.white.withValues(alpha: 0.08),
              valueColor: AlwaysStoppedAnimation(color),
            ),
          ),
        ),
        const SizedBox(width: 6),
        Text('${value.toInt()}',
            style: TextStyle(color: color, fontSize: 11, fontWeight: FontWeight.bold)),
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

/// Mini sparkline painter for spiritual score history.
class _SparklinePainter extends CustomPainter {
  final List<double> data;
  final Color color;

  _SparklinePainter({required this.data, required this.color});

  @override
  void paint(Canvas canvas, Size size) {
    if (data.length < 2) return;
    final paint = Paint()
      ..color = color
      ..strokeWidth = 2
      ..style = PaintingStyle.stroke
      ..strokeCap = StrokeCap.round;

    final minVal = data.reduce(min);
    final maxVal = data.reduce(max);
    final range = (maxVal - minVal).clamp(1.0, 100.0);
    final step = size.width / (data.length - 1);

    final path = Path();
    for (int i = 0; i < data.length; i++) {
      final x = i * step;
      final y = size.height - ((data[i] - minVal) / range) * size.height;
      if (i == 0) {
        path.moveTo(x, y);
      } else {
        path.lineTo(x, y);
      }
    }
    canvas.drawPath(path, paint);

    // Fill under the line
    final fillPaint = Paint()
      ..color = color.withValues(alpha: 0.1)
      ..style = PaintingStyle.fill;
    final fillPath = Path.from(path)
      ..lineTo(size.width, size.height)
      ..lineTo(0, size.height)
      ..close();
    canvas.drawPath(fillPath, fillPaint);
  }

  @override
  bool shouldRepaint(covariant _SparklinePainter old) => old.data != data;
}
