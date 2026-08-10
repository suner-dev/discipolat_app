import 'package:flutter/material.dart';
import '../../../data/services/api_service.dart';
import '../../widgets/app_drawer.dart';
import '../../widgets/attachment_chips.dart';
import '../../widgets/glass_theme.dart';

/// Rapport hebdomadaire d'un département — équivalent mobile de la page web
/// `/departments/:id/report`. Pensé pour l'espace RESPONSABLE (et super-utilisateurs) :
/// synthèse de la semaine (familles, présences, sorties, maintenus) + détail par
/// famille + indicateurs de référence.
class DepartmentReportScreen extends StatefulWidget {
  final String departmentId;
  /// Permet d'injecter un ApiService mocké dans les tests widget.
  final ApiService? apiService;

  const DepartmentReportScreen({super.key, required this.departmentId, this.apiService});

  @override
  State<DepartmentReportScreen> createState() => _DepartmentReportScreenState();
}

class _DepartmentReportScreenState extends State<DepartmentReportScreen> {
  late final ApiService _apiService = widget.apiService ?? ApiService();
  bool _isLoading = true;
  bool _hasError = false;

  String _semaine = DateTime.now().toIso8601String().split('T')[0];

  String _deptNom = 'Rapport du département';
  int _totalFamilles = 0;
  int _familyReportsSoumis = 0;
  int _totalPresents = 0;
  int _totalAbsents = 0;
  int _totalSorties = 0;
  int _totalMaintenus = 0;
  double _presenceMoyenne = 0;
  Map<String, dynamic> _statsParFamille = {};

  // KPI de référence (carte « Indicateurs de la semaine »)
  int _kpiRapportsSoumis = 0;
  int _kpiRapportsAttendus = 0;
  double _kpiTauxSoumission = 0;
  double _kpiTauxPresence = 0;
  int _kpiTotalFaiseurs = 0;

  @override
  void initState() {
    super.initState();
    _loadData();
  }

  Future<void> _loadData() async {
    setState(() {
      _isLoading = true;
      _hasError = false;
    });
    try {
      // Les données critiques (detail + report) sont chargées ensemble ;
      // le KPI est best-effort (un échec KPI ne doit pas masquer le rapport).
      final results = await Future.wait([
        _apiService.get('/departments/${widget.departmentId}/detail'),
        _apiService.get(
          '/departments/${widget.departmentId}/report',
          params: {'semaine': _semaine},
        ),
      ]);
      final kpiFuture = _apiService
          .get('/departments/${widget.departmentId}/kpi')
          .then((r) => r.data)
          .catchError((_) => <String, dynamic>{});
      if (!mounted) return;

      final detail = results[0].data is Map
          ? results[0].data as Map<String, dynamic>
          : <String, dynamic>{};
      final report = results[1].data is Map
          ? results[1].data as Map<String, dynamic>
          : <String, dynamic>{};
      final kpiData = await kpiFuture;
      final kpi = kpiData is Map
          ? Map<String, dynamic>.from(kpiData)
          : <String, dynamic>{};
      if (!mounted) return;

      setState(() {
        _deptNom = (detail['nom'] as String?) ?? 'Rapport du département';
        _totalFamilles = (report['totalFamilles'] as num?)?.toInt() ?? 0;
        _familyReportsSoumis = (report['familyReportsSoumis'] as num?)?.toInt() ?? 0;
        _totalPresents = (report['totalPresents'] as num?)?.toInt() ?? 0;
        _totalAbsents = (report['totalAbsents'] as num?)?.toInt() ?? 0;
        _totalSorties = (report['totalSorties'] as num?)?.toInt() ?? 0;
        _totalMaintenus = (report['totalMaintenus'] as num?)?.toInt() ?? 0;
        _presenceMoyenne = (report['presenceMoyenne'] as num?)?.toDouble() ?? 0;
        _statsParFamille = (report['statsParFamille'] as Map<String, dynamic>?) ?? {};

        _kpiRapportsSoumis = (kpi['rapportsSoumisSemaine'] as num?)?.toInt() ?? 0;
        _kpiRapportsAttendus = (kpi['rapportsAttendusSemaine'] as num?)?.toInt() ?? 0;
        _kpiTauxSoumission = (kpi['tauxSoumission'] as num?)?.toDouble() ?? 0;
        _kpiTauxPresence = (kpi['tauxPresence'] as num?)?.toDouble() ?? 0;
        _kpiTotalFaiseurs = (kpi['totalFaiseurs'] as num?)?.toInt() ?? 0;
        _isLoading = false;
      });
    } catch (_) {
      if (mounted) {
        setState(() {
          _isLoading = false;
          _hasError = true;
        });
      }
    }
  }

  Future<void> _pickSemaine() async {
    final now = DateTime.now();
    final initial = DateTime.tryParse(_semaine) ?? now;
    final picked = await showDatePicker(
      context: context,
      initialDate: initial,
      firstDate: DateTime(now.year - 2),
      lastDate: now,
      helpText: 'Choisir la semaine',
    );
    if (picked == null || !mounted) return;
    final iso = picked.toIso8601String().split('T')[0];
    if (iso != _semaine) {
      setState(() => _semaine = iso);
      _loadData();
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      extendBodyBehindAppBar: true,
      appBar: AppBar(
        title: const Text('Rapport du département', style: TextStyle(color: Colors.white, fontSize: 18)),
        actions: [
          IconButton(
            icon: const Icon(Icons.refresh, color: Colors.white60),
            onPressed: _loadData,
          ),
        ],
      ),
      drawer: const AppDrawer(),
      body: Container(
        decoration: BoxDecoration(
          gradient: LinearGradient(
            begin: Alignment.topCenter,
            end: Alignment.bottomCenter,
            colors: [
              const Color(0xFF030712),
              const Color(0xFF111827).withValues(alpha: 0.9),
              const Color(0xFF030712),
            ],
          ),
        ),
        child: _isLoading
            ? const ShimmerLoading(itemCount: 5)
            : RefreshIndicator(
                onRefresh: _loadData,
                color: AppColors.primary,
                child: ListView(
                  padding: const EdgeInsets.fromLTRB(16, 16, 16, 100),
                  children: [
                    if (_hasError)
                      _buildErrorCard()
                    else ...[
                      _buildHeader(),
                      const SizedBox(height: 16),
                      _buildStatGrid(),
                      const SizedBox(height: 10),
                      _statTile(
                        'Présence de la semaine',
                        '${_fmt(_presenceMoyenne)}%',
                        Icons.bar_chart_rounded,
                        const Color(0xFF3B82F6),
                        const Color(0xFF6366F1),
                      ),
                      if (_totalSorties > 0 || _totalMaintenus > 0) ...[
                        const SizedBox(height: 12),
                        _buildSortiesMaintenus(),
                      ],
                      const SizedBox(height: 20),
                      _buildSectionLabel('Détail par famille'),
                      const SizedBox(height: 10),
                      if (_statsParFamille.isEmpty)
                        _buildEmptyState()
                      else
                        ..._statsParFamille.entries.map(
                          (e) => _buildFamilyCard(e.value),
                        ),
                      const SizedBox(height: 20),
                      _buildSectionLabel('Indicateurs de la semaine'),
                      const SizedBox(height: 10),
                      _buildKpiCard(),
                    ],
                  ],
                ),
              ),
      ),
    );
  }

  Widget _buildErrorCard() {
    return GlassCard(
      padding: const EdgeInsets.all(24),
      child: Column(
        children: [
          Icon(Icons.cloud_off, size: 40, color: Colors.white.withValues(alpha: 0.3)),
          const SizedBox(height: 12),
          Text(
            'Impossible de charger le rapport',
            textAlign: TextAlign.center,
            style: TextStyle(color: Colors.white.withValues(alpha: 0.6), fontSize: 14),
          ),
          const SizedBox(height: 12),
          OutlinedButton.icon(
            onPressed: _loadData,
            icon: const Icon(Icons.refresh, size: 16),
            label: const Text('Réessayer'),
          ),
        ],
      ),
    );
  }

  Widget _buildHeader() {
    final semaine = DateTime.tryParse(_semaine);
    final label = semaine != null
        ? 'Semaine du ${semaine.day.toString().padLeft(2, '0')}/${semaine.month.toString().padLeft(2, '0')}/${semaine.year}'
        : 'Semaine du $_semaine';

    return GlassCard(
      padding: const EdgeInsets.all(16),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Container(
                padding: const EdgeInsets.all(12),
                decoration: BoxDecoration(
                  gradient: const LinearGradient(
                    colors: [Color(0xFFF59E0B), Color(0xFFF97316)],
                    begin: Alignment.topLeft,
                    end: Alignment.bottomRight,
                  ),
                  borderRadius: BorderRadius.circular(14),
                  boxShadow: [
                    BoxShadow(
                      color: const Color(0xFFF59E0B).withValues(alpha: 0.35),
                      blurRadius: 12,
                      offset: const Offset(0, 4),
                    ),
                  ],
                ),
                child: const Icon(Icons.description_rounded, color: Colors.white, size: 24),
              ),
              const SizedBox(width: 14),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      _deptNom,
                      style: const TextStyle(
                        color: Colors.white,
                        fontSize: 18,
                        fontWeight: FontWeight.bold,
                      ),
                    ),
                    const SizedBox(height: 4),
                    Text(
                      label,
                      style: TextStyle(
                        color: Colors.white.withValues(alpha: 0.5),
                        fontSize: 12,
                      ),
                    ),
                  ],
                ),
              ),
            ],
          ),
          const SizedBox(height: 14),
          InkWell(
            borderRadius: BorderRadius.circular(12),
            onTap: _pickSemaine,
            child: Container(
              padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 10),
              decoration: BoxDecoration(
                color: Colors.white.withValues(alpha: 0.06),
                borderRadius: BorderRadius.circular(12),
                border: Border.all(color: Colors.white.withValues(alpha: 0.1)),
              ),
              child: Row(
                children: [
                  Icon(Icons.calendar_today, size: 16, color: AppColors.primaryLight),
                  const SizedBox(width: 10),
                  Expanded(
                    child: Text(
                      'Changer de semaine',
                      style: TextStyle(color: Colors.white.withValues(alpha: 0.7), fontSize: 13),
                    ),
                  ),
                  Icon(Icons.chevron_right, color: Colors.white38, size: 18),
                ],
              ),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildStatGrid() {
    final rapportsSoumis = _familyReportsSoumis;
    final totalFamilles = _totalFamilles;
    final rapportsLabel = totalFamilles > 0 ? '$rapportsSoumis/$totalFamilles' : '$rapportsSoumis';

    return Row(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Expanded(
          child: Column(
            children: [
              _statTile(
                'Familles',
                '$_totalFamilles',
                Icons.group_work_rounded,
                const Color(0xFF8B5CF6),
                const Color(0xFF7C3AED),
              ),
              const SizedBox(height: 10),
              _statTile(
                'Présents',
                '$_totalPresents',
                Icons.check_circle_rounded,
                const Color(0xFF10B981),
                const Color(0xFF14B8A6),
              ),
            ],
          ),
        ),
        const SizedBox(width: 10),
        Expanded(
          child: Column(
            children: [
              _statTile(
                'Rapports soumis',
                rapportsLabel,
                Icons.task_alt_rounded,
                rapportsSoumis == totalFamilles && totalFamilles > 0
                    ? const Color(0xFF10B981)
                    : const Color(0xFFF59E0B),
                rapportsSoumis == totalFamilles && totalFamilles > 0
                    ? const Color(0xFF14B8A6)
                    : const Color(0xFFF97316),
              ),
              const SizedBox(height: 10),
              _statTile(
                'Absents',
                '$_totalAbsents',
                Icons.cancel_rounded,
                const Color(0xFFEF4444),
                const Color(0xFFF43F5E),
              ),
            ],
          ),
        ),
      ],
    );
  }

  Widget _statTile(String label, String value, IconData icon, Color start, Color end) {
    return GlassCard(
      padding: const EdgeInsets.all(14),
      child: Row(
        children: [
          Container(
            padding: const EdgeInsets.all(8),
            decoration: BoxDecoration(
              gradient: LinearGradient(colors: [start, end], begin: Alignment.topLeft, end: Alignment.bottomRight),
              borderRadius: BorderRadius.circular(10),
            ),
            child: Icon(icon, color: Colors.white, size: 16),
          ),
          const SizedBox(width: 10),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  value,
                  style: const TextStyle(color: Colors.white, fontSize: 18, fontWeight: FontWeight.bold, fontFamily: 'monospace'),
                ),
                Text(
                  label,
                  style: TextStyle(color: Colors.white.withValues(alpha: 0.5), fontSize: 10),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildSortiesMaintenus() {
    return GlassCard(
      padding: const EdgeInsets.all(16),
      child: Row(
        children: [
          Expanded(
            child: Row(
              children: [
                Icon(Icons.trending_down, color: Colors.red.shade400, size: 18),
                const SizedBox(width: 8),
                Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text('$_totalSorties', style: const TextStyle(color: Colors.red, fontSize: 18, fontWeight: FontWeight.bold, fontFamily: 'monospace')),
                    Text('Sorties', style: TextStyle(color: Colors.white.withValues(alpha: 0.5), fontSize: 11)),
                  ],
                ),
              ],
            ),
          ),
          Container(width: 1, height: 32, color: Colors.white.withValues(alpha: 0.08)),
          Expanded(
            child: Row(
              mainAxisAlignment: MainAxisAlignment.end,
              children: [
                Column(
                  crossAxisAlignment: CrossAxisAlignment.end,
                  children: [
                    Text('$_totalMaintenus', style: const TextStyle(color: Colors.green, fontSize: 18, fontWeight: FontWeight.bold, fontFamily: 'monospace')),
                    Text('Maintenus', style: TextStyle(color: Colors.white.withValues(alpha: 0.5), fontSize: 11)),
                  ],
                ),
                const SizedBox(width: 8),
                Icon(Icons.trending_up, color: Colors.green.shade400, size: 18),
              ],
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildSectionLabel(String label) {
    return Text(
      label,
      style: TextStyle(
        color: Colors.white.withValues(alpha: 0.5),
        fontSize: 12,
        fontWeight: FontWeight.w600,
        letterSpacing: 0.5,
      ),
    );
  }

  Widget _buildFamilyCard(dynamic raw) {
    final stats = raw is Map ? Map<String, dynamic>.from(raw) : <String, dynamic>{};
    final nom = (stats['familleNom'] as String?) ?? 'Famille';
    final soumis = stats['soumis'] == true;
    final presence = stats['presenceMoyenne'] as num?;
    final presents = stats['totalPresents'] as num?;
    final absents = stats['totalAbsents'] as num?;
    final sorties = stats['totalSorties'] as num?;
    final maintenus = stats['totalMaintenus'] as num?;
    final piecesJointes = (stats['piecesJointes'] as List<dynamic>?) ?? const [];

    return Padding(
      padding: const EdgeInsets.only(bottom: 10),
      child: GlassCard(
        padding: const EdgeInsets.all(14),
        borderColor: soumis
            ? AppColors.primary.withValues(alpha: 0.3)
            : Colors.amber.withValues(alpha: 0.2),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                Expanded(
                  child: Text(
                    nom,
                    style: const TextStyle(color: Colors.white, fontSize: 14, fontWeight: FontWeight.w600),
                    overflow: TextOverflow.ellipsis,
                  ),
                ),
                StatusBadge(
                  label: soumis ? 'Soumis' : 'Non soumis',
                  color: soumis ? Colors.green : Colors.amber,
                ),
              ],
            ),
            const SizedBox(height: 10),
            Row(
              children: [
                _familyStat('Présence', presence != null ? '${_fmt(presence)}%' : '-', AppColors.primaryLight),
                _familyStat('Présents', presents?.toString() ?? '-', Colors.green),
                _familyStat('Absents', absents?.toString() ?? '-', Colors.red),
                _familyStat('Sorties', sorties?.toString() ?? '-', Colors.orange),
                _familyStat('Maintenus', maintenus?.toString() ?? '-', Colors.blue),
              ],
            ),
            if (piecesJointes.isNotEmpty) ...[const SizedBox(height: 10), AttachmentChips(pieces: piecesJointes)],
          ],
        ),
      ),
    );
  }

  Widget _familyStat(String label, String value, Color color) {
    return Expanded(
      child: Column(
        children: [
          Text(value, style: TextStyle(color: color, fontSize: 13, fontWeight: FontWeight.bold)),
          const SizedBox(height: 2),
          Text(label, style: TextStyle(color: Colors.white.withValues(alpha: 0.4), fontSize: 9)),
        ],
      ),
    );
  }

  Widget _buildEmptyState() {
    return GlassCard(
      padding: const EdgeInsets.all(28),
      child: Column(
        children: [
          Icon(Icons.description_outlined, size: 44, color: Colors.white.withValues(alpha: 0.25)),
          const SizedBox(height: 12),
          Text(
            'Aucun rapport disponible pour cette semaine',
            textAlign: TextAlign.center,
            style: TextStyle(color: Colors.white.withValues(alpha: 0.5), fontSize: 14),
          ),
          const SizedBox(height: 8),
          Text(
            'Choisissez une autre semaine pour consulter un rapport existant.',
            textAlign: TextAlign.center,
            style: TextStyle(color: Colors.white.withValues(alpha: 0.3), fontSize: 12),
          ),
        ],
      ),
    );
  }

  Widget _buildKpiCard() {
    return GlassCard(
      padding: const EdgeInsets.all(16),
      child: Column(
        children: [
          _kpiRow(
            Icons.task_alt,
            'Taux de soumission',
            '${_fmt(_kpiTauxSoumission)}%',
            _kpiTauxSoumission / 100,
            AppColors.primary,
          ),
          const SizedBox(height: 12),
          _kpiRow(
            Icons.event_available,
            'Taux de présence',
            '${_fmt(_kpiTauxPresence)}%',
            _kpiTauxPresence / 100,
            Colors.green,
          ),
          const SizedBox(height: 12),
          Row(
            children: [
              Icon(Icons.description_rounded, color: Colors.amber, size: 16),
              const SizedBox(width: 10),
              Text('Rapports soumis', style: TextStyle(color: Colors.white.withValues(alpha: 0.6), fontSize: 12)),
              const Spacer(),
              Text(
                '$_kpiRapportsSoumis/$_kpiRapportsAttendus',
                style: const TextStyle(color: Colors.white, fontSize: 15, fontWeight: FontWeight.bold),
              ),
            ],
          ),
          const SizedBox(height: 12),
          Row(
            children: [
              Icon(Icons.people_rounded, color: Colors.blue, size: 16),
              const SizedBox(width: 10),
              Text('Faiseurs actifs', style: TextStyle(color: Colors.white.withValues(alpha: 0.6), fontSize: 12)),
              const Spacer(),
              Text(
                '$_kpiTotalFaiseurs',
                style: const TextStyle(color: Colors.white, fontSize: 15, fontWeight: FontWeight.bold),
              ),
            ],
          ),
        ],
      ),
    );
  }

  Widget _kpiRow(IconData icon, String label, String value, double progress, Color color) {
    return Column(
      children: [
        Row(
          children: [
            Icon(icon, color: color, size: 16),
            const SizedBox(width: 10),
            Expanded(
              child: Text(label, style: TextStyle(color: Colors.white.withValues(alpha: 0.6), fontSize: 12)),
            ),
            Text(value, style: const TextStyle(color: Colors.white, fontSize: 15, fontWeight: FontWeight.bold)),
          ],
        ),
        const SizedBox(height: 6),
        ClipRRect(
          borderRadius: BorderRadius.circular(4),
          child: LinearProgressIndicator(
            value: progress.clamp(0.0, 1.0).toDouble(),
            backgroundColor: Colors.white.withValues(alpha: 0.08),
            valueColor: AlwaysStoppedAnimation(color),
            minHeight: 3,
          ),
        ),
      ],
    );
  }

  /// Formate un nombre décimal sans décimales inutiles (ex: 87.5 → "87.5", 80 → "80").
  String _fmt(num value) {
    if (value == value.roundToDouble()) return value.toInt().toString();
    return value.toStringAsFixed(1).replaceAll(RegExp(r'\.0$'), '');
  }
}
