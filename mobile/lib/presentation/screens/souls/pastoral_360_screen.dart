import 'package:flutter/material.dart';
import 'package:intl/intl.dart';
import 'package:fl_chart/fl_chart.dart';
import '../../../data/services/api_service.dart';
import '../../widgets/attachment_chips.dart';
import '../../widgets/glass_theme.dart';

class Pastoral360Screen extends StatefulWidget {
  final String soulId;
  /// Permet d'injecter un ApiService mocké dans les tests widget.
  final ApiService? apiService;

  const Pastoral360Screen({super.key, required this.soulId, this.apiService});

  @override
  State<Pastoral360Screen> createState() => _Pastoral360ScreenState();
}

class _Pastoral360ScreenState extends State<Pastoral360Screen> with SingleTickerProviderStateMixin {
  late final ApiService _apiService = widget.apiService ?? ApiService();
  Map<String, dynamic>? _dossier;
  bool _isLoading = true;

  late final AnimationController _animCtrl;
  late final Animation<double> _fadeAnim;

  @override
  void initState() {
    super.initState();
    _animCtrl = AnimationController(vsync: this, duration: const Duration(milliseconds: 600));
    _fadeAnim = CurvedAnimation(parent: _animCtrl, curve: Curves.easeOut);
    _loadData();
  }

  @override
  void dispose() {
    _animCtrl.dispose();
    super.dispose();
  }

  Future<void> _loadData() async {
    try {
      final response = await _apiService.get('/souls/${widget.soulId}/pastoral-360');
      if (mounted) {
        setState(() { _dossier = response.data as Map<String, dynamic>?; _isLoading = false; });
        _animCtrl.forward();
      }
    } catch (_) {
      if (mounted) setState(() => _isLoading = false);
    }
  }

  Map<String, dynamic> get _infos => (_dossier?['informations'] as Map<String, dynamic>?) ?? {};
  Map<String, dynamic> get _spirituel => (_dossier?['spirituel'] as Map<String, dynamic>?) ?? {};
  Map<String, dynamic> get _indices => (_dossier?['indices'] as Map<String, dynamic>?) ?? {};
  List<Map<String, dynamic>> get _alertes => ((_dossier?['alertesAutomatiques'] as List<dynamic>?) ?? []).cast<Map<String, dynamic>>();
  Map<String, dynamic> get _encadrement => (_dossier?['encadrement'] as Map<String, dynamic>?) ?? {};
  List<Map<String, dynamic>> get _timeline => ((_dossier?['timeline'] as List<dynamic>?) ?? []).cast<Map<String, dynamic>>();
  Map<String, dynamic> get _evaluations => (_dossier?['evaluations'] as Map<String, dynamic>?) ?? {};
  List<Map<String, dynamic>> get _notes => ((_dossier?['notes'] as List<dynamic>?) ?? []).cast<Map<String, dynamic>>();
  List<dynamic> get _piecesJointes => (_dossier?['piecesJointes'] as List<dynamic>?) ?? const [];

  static const _indiceLabels = {
    'santeSpirituelle': 'Santé\nspirituelle',
    'fidelite': 'Fidélité',
    'engagement': 'Engagement',
    'participation': 'Participation',
    'global': 'Global',
  };

  static const _indiceColors = {
    'santeSpirituelle': Color(0xFF22C55E),
    'fidelite': Color(0xFF3B82F6),
    'engagement': Color(0xFFF59E0B),
    'participation': Color(0xFF8B5CF6),
    'global': Color(0xFF22C55E),
  };

  static const _statutLabels = {
    'NOUVEAU_CONVERTI': 'Nouveau converti',
    'NOUVEL_ARRIVANT': 'Nouvel arrivant',
    'EN_INTEGRATION': 'En intégration',
    'ACTIF': 'Actif',
    'EN_VEILLE': 'En veille',
    'DECROCHE': 'Décroché',
  };

  @override
  Widget build(BuildContext context) {
    final nomComplet = _infos['prenom'] != null
        ? '${_infos['prenom']} ${_infos['nom']}'
        : (_infos['nom'] ?? 'Dossier Pastoral');

    return Scaffold(
      appBar: AppBar(
        title: Text(nomComplet, style: const TextStyle(fontSize: 16)),
        actions: [IconButton(icon: const Icon(Icons.refresh), onPressed: _loadData)],
      ),
      body: _isLoading
          ? const ShimmerLoading(itemCount: 5)
          : _dossier == null
              ? Center(
                  child: Column(
                    mainAxisAlignment: MainAxisAlignment.center,
                    children: [
                      Icon(Icons.favorite, color: Colors.white.withValues(alpha: 0.1), size: 64),
                      const SizedBox(height: 16),
                      Text('Membre non trouvé',
                          style: TextStyle(color: Colors.white.withValues(alpha: 0.4), fontSize: 16)),
                    ],
                  ),
                )
              : RefreshIndicator(
                  onRefresh: _loadData,
                  child: FadeTransition(
                    opacity: _fadeAnim,
                    child: SingleChildScrollView(
                      physics: const AlwaysScrollableScrollPhysics(),
                      padding: const EdgeInsets.all(16),
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          // Header
                          Container(
                            padding: const EdgeInsets.all(16),
                            decoration: BoxDecoration(
                              gradient: LinearGradient(
                                colors: [AppColors.primary.withValues(alpha: 0.15), Colors.transparent],
                                begin: Alignment.topLeft, end: Alignment.bottomRight,
                              ),
                              borderRadius: BorderRadius.circular(16),
                              border: Border.all(color: Colors.white.withValues(alpha: 0.06)),
                            ),
                            child: Column(
                              crossAxisAlignment: CrossAxisAlignment.start,
                              children: [
                                Row(
                                  children: [
                                    Icon(Icons.auto_awesome, color: AppColors.primary, size: 16),
                                    const SizedBox(width: 8),
                                    Text('Dossier Pastoral 360°',
                                        style: TextStyle(color: AppColors.primaryLight, fontSize: 10, fontWeight: FontWeight.w600, letterSpacing: 1)),
                                  ],
                                ),
                                const SizedBox(height: 8),
                                Text(nomComplet,
                                    style: const TextStyle(color: Colors.white, fontSize: 24, fontWeight: FontWeight.bold)),
                                Text(
                                  'Fiche complète · ${DateFormat('EEEE d MMMM yyyy', 'fr_FR').format(DateTime.now())}',
                                  style: TextStyle(color: Colors.white.withValues(alpha: 0.4), fontSize: 11),
                                ),
                              ],
                            ),
                          ),
                          const SizedBox(height: 16),

                          // Auto-alerts
                          if (_alertes.isNotEmpty)
                            ..._alertes.map((alert) => Container(
                              margin: const EdgeInsets.only(bottom: 8),
                              padding: const EdgeInsets.all(12),
                              decoration: BoxDecoration(
                                color: (alert['priorite'] == 'HAUTE' ? Colors.red : Colors.amber).withValues(alpha: 0.1),
                                borderRadius: BorderRadius.circular(12),
                                border: Border.all(color: (alert['priorite'] == 'HAUTE' ? Colors.red : Colors.amber).withValues(alpha: 0.25)),
                              ),
                              child: Row(
                                children: [
                                  Icon(Icons.warning_amber_rounded,
                                      color: alert['priorite'] == 'HAUTE' ? Colors.red : Colors.amber, size: 20),
                                  const SizedBox(width: 10),
                                  Expanded(
                                    child: Text('${alert['message']}',
                                        style: TextStyle(
                                          color: alert['priorite'] == 'HAUTE' ? Colors.red.shade300 : Colors.amber.shade300,
                                          fontSize: 13, fontWeight: FontWeight.w500,
                                        )),
                                  ),
                                  _smallBadge(alert['priorite'] == 'HAUTE' ? Colors.red : Colors.amber, '${alert['priorite']}'),
                                ],
                              ),
                            )),

                          // Indices intelligents
                          GlassCard(
                            padding: const EdgeInsets.all(16),
                            child: Column(
                              crossAxisAlignment: CrossAxisAlignment.start,
                              children: [
                                Row(
                                  children: [
                                    Icon(Icons.monitor_heart, color: AppColors.primary, size: 18),
                                    const SizedBox(width: 8),
                                    Text('INDICES DE SANTÉ',
                                        style: TextStyle(color: Colors.white.withValues(alpha: 0.7), fontSize: 11, fontWeight: FontWeight.w600, letterSpacing: 1)),
                                  ],
                                ),
                                const SizedBox(height: 16),
                                // Gauges
                                Row(
                                  mainAxisAlignment: MainAxisAlignment.spaceEvenly,
                                  children: _indiceKeys.map((key) => _indiceGauge(
                                    label: _indiceLabels[key] ?? key,
                                    value: (_indices[key] ?? 0) as num,
                                    color: _indiceColors[key] ?? AppColors.primary,
                                    isSmall: key != 'global',
                                  )).toList(),
                                ),
                                // Radar chart
                                if (_radarData.isNotEmpty) ...[
                                  const SizedBox(height: 16),
                                  SizedBox(
                                    height: 180,
                                    child: RadarChart(
                                      RadarChartData(
                                        radarShape: RadarShape.polygon,
                                        tickCount: 5,
                                        dataSets: [
                                          RadarDataSet(
                                            fillColor: AppColors.primary.withValues(alpha: 0.15),
                                            borderColor: AppColors.primary,
                                            borderWidth: 2,
                                            dataEntries: _radarData,
                                          ),
                                        ],
                                        getTitle: (i, _) {
                                          final keys = ['santeSpirituelle', 'fidelite', 'engagement', 'participation'];
                                          return RadarChartTitle(text: _indiceLabels[keys[i]] ?? '');
                                        },
                                        titlePositionPercentageOffset: 0.2,
                                        tickBorderData: BorderSide(color: Colors.white.withValues(alpha: 0.06)),
                                        gridBorderData: BorderSide(color: Colors.white.withValues(alpha: 0.06)),
                                      ),
                                    ),
                                  ),
                                ],
                              ],
                            ),
                          ),
                          const SizedBox(height: 16),

                          // Coordonnées + Parcours spirituel
                          Row(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              // Coordonnées
                              Expanded(
                                child: GlassCard(
                                  padding: const EdgeInsets.all(12),
                                  child: Column(
                                    crossAxisAlignment: CrossAxisAlignment.start,
                                    children: [
                                      Row(
                                        children: [
                                          Icon(Icons.auto_awesome, color: AppColors.primary, size: 14),
                                          const SizedBox(width: 6),
                                          Text('Coordonnées',
                                              style: TextStyle(color: Colors.white.withValues(alpha: 0.7), fontSize: 11, fontWeight: FontWeight.w600)),
                                        ],
                                      ),
                                      const SizedBox(height: 12),
                                      ..._infoFields.map((field) {
                                        final value = _infos[field['key'] as String] as String?;
                                        if (value == null || value.isEmpty) return const SizedBox.shrink();
                                        return Padding(
                                          padding: const EdgeInsets.only(bottom: 8),
                                          child: Row(
                                            children: [
                                              Icon(field['icon'] as IconData, color: Colors.white38, size: 14),
                                              const SizedBox(width: 8),
                                              Expanded(
                                                child: Column(
                                                  crossAxisAlignment: CrossAxisAlignment.start,
                                                  children: [
                                                    Text('${field['label']}',
                                                        style: TextStyle(color: Colors.white.withValues(alpha: 0.3), fontSize: 9)),
                                                    Text(value,
                                                        style: const TextStyle(color: Colors.white, fontSize: 13)),
                                                  ],
                                                ),
                                              ),
                                            ],
                                          ),
                                        );
                                      }),
                                    ],
                                  ),
                                ),
                              ),
                              const SizedBox(width: 10),
                              // Parcours spirituel
                              Expanded(
                                child: GlassCard(
                                  padding: const EdgeInsets.all(12),
                                  child: Column(
                                    crossAxisAlignment: CrossAxisAlignment.start,
                                    children: [
                                      Row(
                                        children: [
                                          Icon(Icons.church, color: AppColors.primary, size: 14),
                                          const SizedBox(width: 6),
                                          Text('Parcours spirituel',
                                              style: TextStyle(color: Colors.white.withValues(alpha: 0.7), fontSize: 11, fontWeight: FontWeight.w600)),
                                        ],
                                      ),
                                      const SizedBox(height: 12),
                                      _spiritualRow('Type', _statutLabels[_spirituel['typeDisciple']] ?? '${_spirituel['typeDisciple'] ?? '-'}'),
                                      _spiritualRow('Statut', _statutLabels[_spirituel['statut']] ?? '${_spirituel['statut'] ?? '-'}'),
                                      _spiritualRow('État spirituel', '${_spirituel['etatSpirituel'] ?? '-'}'),
                                      _spiritualStars('Niveau', _spirituel['niveauCroissance'] as int? ?? 1),
                                      if (_spirituel['dateIntegration'] != null)
                                        _spiritualRow('Intégré le', _formatDate(_spirituel['dateIntegration'] as String?)),
                                      if (_spirituel['dateConversion'] != null)
                                        _spiritualRow('Conversion le', _formatDate(_spirituel['dateConversion'] as String?)),
                                      _spiritualRow('Dernier contact', _formatDate(_spirituel['dateDernierContact'] as String?)),
                                    ],
                                  ),
                                ),
                              ),
                            ],
                          ),
                          const SizedBox(height: 16),

                          // Encadrement + Évaluations
                          Row(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              Expanded(
                                child: GlassCard(
                                  padding: const EdgeInsets.all(12),
                                  child: Column(
                                    crossAxisAlignment: CrossAxisAlignment.start,
                                    children: [
                                      Row(
                                        children: [
                                          Icon(Icons.shield, color: AppColors.primary, size: 14),
                                          const SizedBox(width: 6),
                                          Text('Encadrement',
                                              style: TextStyle(color: Colors.white.withValues(alpha: 0.7), fontSize: 11, fontWeight: FontWeight.w600)),
                                        ],
                                      ),
                                      const SizedBox(height: 12),
                                      _spiritualRow('Faiseur', _encadrement['faiseurNom'] ?? '${_encadrement['faiseurId'] ?? '—'}'),
                                      _spiritualRow('Famille', _encadrement['familleNom'] ?? _encadrement['familleId'] ?? 'Non assigné'),
                                    ],
                                  ),
                                ),
                              ),
                              const SizedBox(width: 10),
                              Expanded(
                                child: GlassCard(
                                  padding: const EdgeInsets.all(12),
                                  child: Column(
                                    crossAxisAlignment: CrossAxisAlignment.start,
                                    children: [
                                      Row(
                                        children: [
                                          Icon(Icons.star, color: Colors.amber, size: 14),
                                          const SizedBox(width: 6),
                                          Text('Évaluations',
                                              style: TextStyle(color: Colors.white.withValues(alpha: 0.7), fontSize: 11, fontWeight: FontWeight.w600)),
                                        ],
                                      ),
                                      const SizedBox(height: 12),
                                      if (_evaluations.isNotEmpty)
                                        ..._evaluations.entries.map((e) {
                                          final data = e.value as Map<String, dynamic>;
                                          final avg = (data['moyenne'] ?? 0).toDouble();
                                          final total = data['total'] ?? 0;
                                          return Padding(
                                            padding: const EdgeInsets.only(bottom: 6),
                                            child: Row(
                                              children: [
                                                Expanded(
                                                  child: Text(
                                                    e.key == 'FAISEUR' ? 'Faiseur'
                                                        : e.key == 'CHEF_FAMILLE' ? 'Chef'
                                                        : e.key == 'RESPONSABLE' ? 'Responsable' : e.key,
                                                    style: TextStyle(color: Colors.white.withValues(alpha: 0.5), fontSize: 10),
                                                  ),
                                                ),
                                                ...List.generate(5, (s) => Icon(
                                                  s < avg.round() ? Icons.star : Icons.star_border,
                                                  size: 10,
                                                  color: s < avg.round() ? Colors.amber : Colors.white.withValues(alpha: 0.15),
                                                )),
                                                const SizedBox(width: 4),
                                                Text('${avg.toStringAsFixed(1)} ($total)',
                                                    style: TextStyle(color: Colors.white.withValues(alpha: 0.4), fontSize: 9)),
                                              ],
                                            ),
                                          );
                                        })
                                      else
                                        Center(
                                          child: Text('Aucune évaluation',
                                              style: TextStyle(color: Colors.white.withValues(alpha: 0.3), fontSize: 11)),
                                        ),
                                    ],
                                  ),
                                ),
                              ),
                            ],
                          ),
                          const SizedBox(height: 16),

                          // Timeline
                          GlassCard(
                            padding: const EdgeInsets.all(16),
                            child: Column(
                              crossAxisAlignment: CrossAxisAlignment.start,
                              children: [
                                Row(
                                  children: [
                                    Icon(Icons.timeline, color: AppColors.primary, size: 18),
                                    const SizedBox(width: 8),
                                    Text('TIMELINE · ${_timeline.length} événements',
                                        style: TextStyle(color: Colors.white.withValues(alpha: 0.7), fontSize: 11, fontWeight: FontWeight.w600, letterSpacing: 1)),
                                  ],
                                ),
                                const SizedBox(height: 16),
                                if (_timeline.isNotEmpty)
                                  SizedBox(
                                    child: Stack(
                                      children: [
                                        // Timeline line
                                        Positioned(
                                          left: 6, top: 0, bottom: 0,
                                          child: Container(
                                            width: 2,
                                            decoration: BoxDecoration(
                                              gradient: LinearGradient(
                                                colors: [
                                                  AppColors.primary.withValues(alpha: 0.4),
                                                  AppColors.primary.withValues(alpha: 0.1),
                                                  Colors.transparent,
                                                ],
                                              ),
                                            ),
                                          ),
                                        ),
                                        Column(
                                          children: _timeline.take(30).toList().asMap().entries.map((entry) {
                                            final e = entry.value;
                                            return Padding(
                                              padding: const EdgeInsets.only(left: 20, bottom: 8),
                                              child: Row(
                                                crossAxisAlignment: CrossAxisAlignment.start,
                                                children: [
                                                  // Dot
                                                  Container(
                                                    margin: const EdgeInsets.only(top: 4),
                                                    width: 10, height: 10,
                                                    decoration: BoxDecoration(
                                                      color: AppColors.primary,
                                                      shape: BoxShape.circle,
                                                      border: Border.all(color: const Color(0xFF030712), width: 2),
                                                      boxShadow: [BoxShadow(color: AppColors.primary.withValues(alpha: 0.4), blurRadius: 4)],
                                                    ),
                                                  ),
                                                  const SizedBox(width: 10),
                                                  Expanded(
                                                    child: Container(
                                                      padding: const EdgeInsets.all(10),
                                                      decoration: BoxDecoration(
                                                        color: Colors.white.withValues(alpha: 0.04),
                                                        borderRadius: BorderRadius.circular(10),
                                                      ),
                                                      child: Column(
                                                        crossAxisAlignment: CrossAxisAlignment.start,
                                                        children: [
                                                          Row(
                                                            crossAxisAlignment: CrossAxisAlignment.start,
                                                            children: [
                                                              Expanded(
                                                                child: Text('${e['type']}',
                                                                    style: const TextStyle(color: Colors.white, fontWeight: FontWeight.w600, fontSize: 12)),
                                                              ),
                                                              Text(_formatDate(e['date'] as String?),
                                                                  style: TextStyle(color: Colors.white.withValues(alpha: 0.3), fontSize: 9)),
                                                            ],
                                                          ),
                                                          if (e['description'] != null && (e['description'] as String).isNotEmpty)
                                                            Padding(
                                                              padding: const EdgeInsets.only(top: 4),
                                                              child: Text('${e['description']}',
                                                                  style: TextStyle(color: Colors.white.withValues(alpha: 0.5), fontSize: 11)),
                                                            ),
                                                          if (e['ancienStatut'] != null || e['nouveauStatut'] != null)
                                                            Padding(
                                                              padding: const EdgeInsets.only(top: 4),
                                                              child: Row(
                                                                children: [
                                                                  if (e['ancienStatut'] != null)
                                                                    _smallBadge(Colors.grey, '${e['ancienStatut']}'),
                                                                  if (e['ancienStatut'] != null && e['nouveauStatut'] != null)
                                                                    Padding(
                                                                      padding: const EdgeInsets.symmetric(horizontal: 4),
                                                                      child: Icon(Icons.chevron_right, color: Colors.white38, size: 12),
                                                                    ),
                                                                  if (e['nouveauStatut'] != null)
                                                                    _smallBadge(AppColors.primary, '${e['nouveauStatut']}'),
                                                                ],
                                                              ),
                                                            ),
                                                        ],
                                                      ),
                                                    ),
                                                  ),
                                                ],
                                              ),
                                            );
                                          }).toList(),
                                        ),
                                      ],
                                    ),
                                  )
                                else
                                  Center(
                                    child: Column(
                                      children: [
                                        Icon(Icons.timeline, color: Colors.white.withValues(alpha: 0.1), size: 40),
                                        const SizedBox(height: 8),
                                        Text('Aucun historique',
                                            style: TextStyle(color: Colors.white.withValues(alpha: 0.3), fontSize: 13)),
                                      ],
                                    ),
                                  ),
                              ],
                            ),
                          ),
                          const SizedBox(height: 16),

                          // Pièces jointes (documents des rapports de suivi)
                          if (_piecesJointes.isNotEmpty)
                            GlassCard(
                              padding: const EdgeInsets.all(16),
                              child: Column(
                                crossAxisAlignment: CrossAxisAlignment.start,
                                children: [
                                  Row(
                                    children: [
                                      Icon(Icons.attach_file, color: AppColors.primary, size: 18),
                                      const SizedBox(width: 8),
                                      Text('PIÈCES JOINTES · ${_piecesJointes.length}',
                                          style: TextStyle(color: Colors.white.withValues(alpha: 0.7), fontSize: 11, fontWeight: FontWeight.w600, letterSpacing: 1)),
                                    ],
                                  ),
                                  const SizedBox(height: 12),
                                  AttachmentChips(pieces: _piecesJointes, sourceKey: 'source'),
                                ],
                              ),
                            ),
                          const SizedBox(height: 16),

                          // Notes privées
                          if (_notes.isNotEmpty)
                            GlassCard(
                              padding: const EdgeInsets.all(16),
                              child: Column(
                                crossAxisAlignment: CrossAxisAlignment.start,
                                children: [
                                  Row(
                                    children: [
                                      Icon(Icons.message, color: AppColors.primary, size: 18),
                                      const SizedBox(width: 8),
                                      Text('NOTES · ${_notes.length}',
                                          style: TextStyle(color: Colors.white.withValues(alpha: 0.7), fontSize: 11, fontWeight: FontWeight.w600, letterSpacing: 1)),
                                    ],
                                  ),
                                  const SizedBox(height: 12),
                                  ..._notes.map((note) => Container(
                                    margin: const EdgeInsets.only(bottom: 6),
                                    padding: const EdgeInsets.all(10),
                                    decoration: BoxDecoration(
                                      color: Colors.white.withValues(alpha: 0.04),
                                      borderRadius: BorderRadius.circular(10),
                                    ),
                                    child: Column(
                                      crossAxisAlignment: CrossAxisAlignment.start,
                                      children: [
                                        Row(
                                          children: [
                                            Text('${note['auteurId']?.toString().substring(0, 8) ?? ''}...',
                                                style: TextStyle(color: Colors.white.withValues(alpha: 0.3), fontSize: 9)),
                                            const Spacer(),
                                            Text(_formatDate(note['date'] as String?),
                                                style: TextStyle(color: Colors.white.withValues(alpha: 0.3), fontSize: 9)),
                                          ],
                                        ),
                                        const SizedBox(height: 4),
                                        Text('${note['contenu'] ?? ''}',
                                            style: TextStyle(color: Colors.white.withValues(alpha: 0.7), fontSize: 12)),
                                      ],
                                    ),
                                  )),
                                ],
                              ),
                            ),
                          const SizedBox(height: 80),
                        ],
                      ),
                    ),
                  ),
                ),
    );
  }

  List<String> get _indiceKeys => ['santeSpirituelle', 'fidelite', 'engagement', 'participation', 'global'];

  List<RadarEntry> get _radarData {
    return ['santeSpirituelle', 'fidelite', 'engagement', 'participation'].map((key) {
      final val = (_indices[key] ?? 0) as num;
      return RadarEntry(value: val.toDouble());
    }).toList();
  }

  static const _infoFields = [
    {'key': 'email', 'label': 'Email', 'icon': Icons.email_outlined},
    {'key': 'telephone', 'label': 'Téléphone', 'icon': Icons.phone_outlined},
    {'key': 'adresse', 'label': 'Adresse', 'icon': Icons.location_on_outlined},
    {'key': 'dateNaissance', 'label': 'Né(e) le', 'icon': Icons.calendar_today},
    {'key': 'profession', 'label': 'Profession', 'icon': Icons.work_outlined},
    {'key': 'situationFamiliale', 'label': 'Situation', 'icon': Icons.people_outline},
  ];

  Widget _indiceGauge({required String label, required num value, required Color color, bool isSmall = true}) {
    final size = isSmall ? 60.0 : 80.0;
    final strokeWidth = isSmall ? 6.0 : 8.0;
    final fontSize = isSmall ? 14.0 : 22.0;

    return SizedBox(
      width: size + 8,
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          SizedBox(
            width: size,
            height: size,
            child: CustomPaint(
              painter: _GaugePainter(
                value: value.toDouble(),
                maxValue: 100,
                color: color,
                strokeWidth: strokeWidth,
                bgColor: Colors.white.withValues(alpha: 0.06),
              ),
              child: Center(
                child: Text('${value.toInt()}',
                    style: TextStyle(color: color, fontSize: fontSize, fontWeight: FontWeight.bold)),
              ),
            ),
          ),
          const SizedBox(height: 4),
          Text(label, textAlign: TextAlign.center,
              style: TextStyle(color: Colors.white.withValues(alpha: 0.5), fontSize: isSmall ? 8 : 10)),
        ],
      ),
    );
  }

  Widget _spiritualRow(String label, String value) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 4),
      child: Row(
        mainAxisAlignment: MainAxisAlignment.spaceBetween,
        children: [
          Text(label, style: TextStyle(color: Colors.white.withValues(alpha: 0.4), fontSize: 10)),
          Text(value, style: const TextStyle(color: Colors.white, fontSize: 12, fontWeight: FontWeight.w600)),
        ],
      ),
    );
  }

  Widget _spiritualStars(String label, int niveau) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 4),
      child: Row(
        mainAxisAlignment: MainAxisAlignment.spaceBetween,
        children: [
          Text(label, style: TextStyle(color: Colors.white.withValues(alpha: 0.4), fontSize: 10)),
          Row(children: List.generate(5, (s) => Icon(
            s < niveau ? Icons.star : Icons.star_border,
            size: 12,
            color: s < niveau ? Colors.amber : Colors.white.withValues(alpha: 0.15),
          ))),
        ],
      ),
    );
  }

  Widget _smallBadge(Color color, String label) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 6, vertical: 2),
      decoration: BoxDecoration(
        color: color.withValues(alpha: 0.15),
        borderRadius: BorderRadius.circular(6),
      ),
      child: Text(label,
          style: TextStyle(color: color, fontSize: 8, fontWeight: FontWeight.w600)),
    );
  }

  String _formatDate(String? dateStr) {
    if (dateStr == null) return '—';
    try {
      final d = DateTime.parse(dateStr);
      return DateFormat('d MMM yyyy', 'fr_FR').format(d);
    } catch (_) {
      return dateStr;
    }
  }
}

/// Custom painter for gauge circles
class _GaugePainter extends CustomPainter {
  final double value;
  final double maxValue;
  final Color color;
  final double strokeWidth;
  final Color bgColor;

  _GaugePainter({
    required this.value,
    required this.maxValue,
    required this.color,
    required this.strokeWidth,
    required this.bgColor,
  });

  @override
  void paint(Canvas canvas, Size size) {
    final center = Offset(size.width / 2, size.height / 2);
    final radius = (size.width - strokeWidth) / 2;

    // Background circle
    final bgPaint = Paint()
      ..color = bgColor
      ..style = PaintingStyle.stroke
      ..strokeWidth = strokeWidth;
    canvas.drawCircle(center, radius, bgPaint);

    // Value arc
    final progress = (value / maxValue).clamp(0.0, 1.0);
    final sweepAngle = 2 * 3.14159265 * progress;
    final paint = Paint()
      ..color = color
      ..style = PaintingStyle.stroke
      ..strokeWidth = strokeWidth
      ..strokeCap = StrokeCap.round;
    canvas.drawArc(
      Rect.fromCircle(center: center, radius: radius),
      -3.14159265 / 2, // Start at top
      sweepAngle,
      false,
      paint,
    );
  }

  @override
  bool shouldRepaint(covariant _GaugePainter oldDelegate) => oldDelegate.value != value;
}
