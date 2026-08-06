import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import '../../../app.dart';
import '../../../data/services/api_service.dart';
import '../../widgets/app_drawer.dart';
import '../../widgets/glass_theme.dart';

/// Hub « Rapports » — équivalent mobile de la page web `/reports`.
///
/// Pensé pour l'espace RESPONSABLE (et les super-utilisateurs) :
/// statistiques de complétion hebdomadaire + accès rapide aux rapports
/// du faiseur et de famille.
class ReportsScreen extends StatefulWidget {
  /// Permet d'injecter un ApiService mocké dans les tests widget.
  final ApiService? apiService;

  const ReportsScreen({super.key, this.apiService});

  @override
  State<ReportsScreen> createState() => _ReportsScreenState();
}

class _ReportsScreenState extends State<ReportsScreen> {
  late final ApiService _apiService = widget.apiService ?? ApiService();
  bool _isLoading = true;
  bool _hasError = false;

  int _totalRapports = 0;
  int _rapportsSoumis = 0;
  double _tauxCompletion = 0;

  @override
  void initState() {
    super.initState();
    _loadStats();
  }

  Future<void> _loadStats() async {
    setState(() {
      _isLoading = true;
      _hasError = false;
    });
    try {
      final res = await _apiService.get('/dashboard/report-completion');
      final data = res.data is Map ? res.data as Map<String, dynamic> : <String, dynamic>{};
      if (mounted) {
        setState(() {
          _totalRapports = (data['totalRapports'] as num?)?.toInt() ?? 0;
          _rapportsSoumis = (data['rapportsSoumis'] as num?)?.toInt() ?? 0;
          _tauxCompletion = (data['tauxCompletion'] as num?)?.toDouble() ?? 0;
          _isLoading = false;
        });
      }
    } catch (_) {
      if (mounted) {
        setState(() {
          _isLoading = false;
          _hasError = true;
        });
      }
    }
  }

  int get _enAttente =>
      _totalRapports > _rapportsSoumis ? _totalRapports - _rapportsSoumis : 0;

  double get _taux =>
      _tauxCompletion.clamp(0, 100).toDouble();

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      extendBodyBehindAppBar: true,
      appBar: AppBar(
        title: const Text('Rapports', style: TextStyle(color: Colors.white, fontSize: 18)),
        actions: [
          IconButton(
            icon: const Icon(Icons.refresh, color: Colors.white60),
            onPressed: _loadStats,
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
            ? const ShimmerLoading(itemCount: 4)
            : RefreshIndicator(
                onRefresh: _loadStats,
                color: AppColors.primary,
                child: ListView(
                  padding: const EdgeInsets.fromLTRB(16, 16, 16, 100),
                  children: [
                    if (_hasError)
                      _buildErrorCard()
                    else
                      _buildHeader(),
                    const SizedBox(height: 16),
                    _buildSectionLabel('Rapports hebdomadaires'),
                    const SizedBox(height: 10),
                    _buildNavCard(
                      'Rapport du faiseur',
                      'Présences par culte, difficultés et sorties du suivi',
                      Icons.description_rounded,
                      '/reports/maker',
                      const Color(0xFF10B981),
                      const Color(0xFF14B8A6),
                    ),
                    _buildNavCard(
                      'Rapport de famille',
                      'Rapport consolidé de la famille de disciples',
                      Icons.group_work_rounded,
                      '/reports/family',
                      const Color(0xFF3B82F6),
                      const Color(0xFF6366F1),
                    ),
                    if (AuthState().hasActiveRole(['RESPONSABLE', 'ADMIN', 'PASTEUR']))
                      _buildNavCard(
                        'Rapport du département',
                        'Rapport hebdomadaire d\'un département',
                        Icons.business_rounded,
                        null,
                        const Color(0xFFF59E0B),
                        const Color(0xFFF97316),
                        onTap: _openDepartmentPicker,
                      ),
                  ],
                ),
              ),
      ),
    );
  }

  /// Ouvre le sélecteur de département (liste des départements du responsable)
  /// puis navigue vers le rapport du département choisi.
  Future<void> _openDepartmentPicker() async {
    showModalBottomSheet(
      context: context,
      backgroundColor: const Color(0xFF0F172A),
      shape: const RoundedRectangleBorder(
        borderRadius: BorderRadius.vertical(top: Radius.circular(20)),
      ),
      builder: (sheetContext) => FutureBuilder<List<Map<String, dynamic>>>(
        future: _loadDepartments(),
        builder: (context, snapshot) {
          return SafeArea(
            child: SizedBox(
              height: MediaQuery.of(context).size.height * 0.6,
              child: Column(
                children: [
                  Padding(
                    padding: const EdgeInsets.fromLTRB(20, 16, 20, 8),
                    child: Row(
                      children: [
                        const Icon(Icons.business_rounded, color: Color(0xFFF59E0B), size: 20),
                        const SizedBox(width: 10),
                        const Expanded(
                          child: Text(
                            'Choisir un département',
                            style: TextStyle(color: Colors.white, fontSize: 16, fontWeight: FontWeight.w600),
                          ),
                        ),
                        IconButton(
                          icon: Icon(Icons.close, color: Colors.white.withValues(alpha: 0.4), size: 20),
                          onPressed: () => Navigator.pop(sheetContext),
                        ),
                      ],
                    ),
                  ),
                  const Divider(color: Color(0x14FFFFFF), height: 1),
                  Expanded(
                    child: snapshot.connectionState == ConnectionState.waiting
                        ? const ShimmerLoading(itemCount: 4)
                        : snapshot.hasError
                            ? _buildPickerError(sheetContext)
                            : (snapshot.data ?? []).isEmpty
                                ? Center(
                                    child: Text(
                                      'Aucun département trouvé',
                                      style: TextStyle(color: Colors.white.withValues(alpha: 0.5), fontSize: 14),
                                    ),
                                  )
                                : ListView.builder(
                                    padding: const EdgeInsets.all(12),
                                    itemCount: snapshot.data!.length,
                                    itemBuilder: (context, index) {
                                      final dept = snapshot.data![index];
                                      final nom = (dept['nom'] as String?) ?? 'Département';
                                      final nbMembres = dept['nombreMembres'];
                                      return Padding(
                                        padding: const EdgeInsets.only(bottom: 8),
                                        child: GlassCard(
                                          padding: const EdgeInsets.all(14),
                                          onTap: () {
                                            final id = dept['id'] as String?;
                                            if (id == null) return;
                                            Navigator.pop(sheetContext);
                                            context.go('/departments/$id/report');
                                          },
                                          child: Row(
                                            children: [
                                              Container(
                                                padding: const EdgeInsets.all(9),
                                                decoration: BoxDecoration(
                                                  color: Colors.amber.withValues(alpha: 0.15),
                                                  borderRadius: BorderRadius.circular(10),
                                                ),
                                                child: const Icon(Icons.business, color: Colors.amber, size: 18),
                                              ),
                                              const SizedBox(width: 12),
                                              Expanded(
                                                child: Text(
                                                  nom,
                                                  style: const TextStyle(color: Colors.white, fontSize: 14, fontWeight: FontWeight.w600),
                                                  overflow: TextOverflow.ellipsis,
                                                ),
                                              ),
                                              if (nbMembres != null)
                                                Text(
                                                  '$nbMembres membres',
                                                  style: TextStyle(color: Colors.white.withValues(alpha: 0.4), fontSize: 11),
                                                ),
                                              const Icon(Icons.chevron_right, color: Colors.white38, size: 18),
                                            ],
                                          ),
                                        ),
                                      );
                                    },
                                  ),
                  ),
                ],
              ),
            ),
          );
        },
      ),
    );
  }

  /// Charge les départements : ceux administrés par le responsable (par défaut)
  /// ou tous les départements pour les super-utilisateurs.
  Future<List<Map<String, dynamic>>> _loadDepartments() async {
    final auth = AuthState();
    final isSuper = auth.hasActiveRole(['ADMIN', 'PASTEUR']);
    if (!isSuper) {
      final userId = auth.userId;
      if (userId != null) {
        final res = await _apiService.get('/departments/by-responsable/$userId');
        final data = res.data is List ? res.data as List : <dynamic>[];
        return data
            .whereType<Map>()
            .map((e) => Map<String, dynamic>.from(e))
            .toList();
      }
    }
    final res = await _apiService.get('/departments', params: {'size': '50'});
    final content = res.data is Map ? (res.data as Map)['content'] : null;
    final data = content is List ? content : <dynamic>[];
    return data
        .whereType<Map>()
        .map((e) => Map<String, dynamic>.from(e))
        .toList();
  }

  Widget _buildPickerError(BuildContext sheetContext) {
    return Center(
      child: Column(
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          Icon(Icons.cloud_off, size: 40, color: Colors.white.withValues(alpha: 0.3)),
          const SizedBox(height: 12),
          Text(
            'Impossible de charger les départements',
            textAlign: TextAlign.center,
            style: TextStyle(color: Colors.white.withValues(alpha: 0.6), fontSize: 14),
          ),
          const SizedBox(height: 12),
          OutlinedButton.icon(
            onPressed: () => Navigator.pop(sheetContext),
            icon: const Icon(Icons.close, size: 16),
            label: const Text('Fermer'),
          ),
        ],
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
            'Impossible de charger les statistiques',
            textAlign: TextAlign.center,
            style: TextStyle(color: Colors.white.withValues(alpha: 0.6), fontSize: 14),
          ),
          const SizedBox(height: 12),
          OutlinedButton.icon(
            onPressed: _loadStats,
            icon: const Icon(Icons.refresh, size: 16),
            label: const Text('Réessayer'),
          ),
        ],
      ),
    );
  }

  Widget _buildHeader() {
    final progress = _taux / 100;
    return GlassCard(
      padding: const EdgeInsets.all(16),
      child: Column(
        children: [
          Row(
            children: [
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      'Complétion de la semaine',
                      style: TextStyle(
                        color: Colors.white.withValues(alpha: 0.6),
                        fontSize: 12,
                      ),
                    ),
                    const SizedBox(height: 4),
                    Text(
                      '$_rapportsSoumis / $_totalRapports rapports',
                      style: const TextStyle(
                        color: Colors.white,
                        fontSize: 20,
                        fontWeight: FontWeight.bold,
                      ),
                    ),
                  ],
                ),
              ),
              SizedBox(
                width: 48,
                height: 48,
                child: Stack(
                  alignment: Alignment.center,
                  children: [
                    SizedBox(
                      width: 48,
                      height: 48,
                      child: CircularProgressIndicator(
                        value: progress,
                        strokeWidth: 4,
                        backgroundColor: Colors.white.withValues(alpha: 0.08),
                        valueColor: const AlwaysStoppedAnimation(AppColors.primary),
                      ),
                    ),
                    Text(
                      '${_taux.round()}%',
                      style: const TextStyle(
                        color: Colors.white,
                        fontSize: 11,
                        fontWeight: FontWeight.bold,
                      ),
                    ),
                  ],
                ),
              ),
            ],
          ),
          const SizedBox(height: 14),
          Row(
            children: [
              _miniStat(Icons.task_alt, 'Soumis', '$_rapportsSoumis', Colors.green),
              const SizedBox(width: 8),
              _miniStat(Icons.pending_actions, 'En attente', '$_enAttente', Colors.amber),
              const SizedBox(width: 8),
              _miniStat(Icons.trending_up, 'Taux', '${_taux.round()}%', AppColors.primaryLight),
            ],
          ),
          if (_taux < 100) ...[
            const SizedBox(height: 12),
            ClipRRect(
              borderRadius: BorderRadius.circular(4),
              child: LinearProgressIndicator(
                value: progress,
                backgroundColor: Colors.white.withValues(alpha: 0.08),
                valueColor: const AlwaysStoppedAnimation(AppColors.primary),
                minHeight: 3,
              ),
            ),
          ],
        ],
      ),
    );
  }

  Widget _miniStat(IconData icon, String label, String value, Color color) {
    return Expanded(
      child: GlassCard(
        padding: const EdgeInsets.all(10),
        child: Column(
          children: [
            Icon(icon, color: color, size: 18),
            const SizedBox(height: 4),
            Text(
              value,
              style: TextStyle(color: color, fontSize: 16, fontWeight: FontWeight.bold),
            ),
            Text(
              label,
              style: TextStyle(color: Colors.white.withValues(alpha: 0.5), fontSize: 10),
            ),
          ],
        ),
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

  Widget _buildNavCard(
    String title,
    String subtitle,
    IconData icon,
    String? route,
    Color gradientStart,
    Color gradientEnd, {
    VoidCallback? onTap,
  })
  {
    return Padding(
      padding: const EdgeInsets.only(bottom: 12),
      child: GlassCard(
        padding: EdgeInsets.zero,
        child: InkWell(
          borderRadius: BorderRadius.circular(16),
          onTap: onTap ?? (route != null ? () => context.go(route) : null),
          child: Padding(
            padding: const EdgeInsets.all(16),
            child: Row(
              children: [
                Container(
                  padding: const EdgeInsets.all(12),
                  decoration: BoxDecoration(
                    gradient: LinearGradient(
                      colors: [gradientStart, gradientEnd],
                      begin: Alignment.topLeft,
                      end: Alignment.bottomRight,
                    ),
                    borderRadius: BorderRadius.circular(14),
                    boxShadow: [
                      BoxShadow(
                        color: gradientStart.withValues(alpha: 0.35),
                        blurRadius: 12,
                        offset: const Offset(0, 4),
                      ),
                    ],
                  ),
                  child: Icon(icon, color: Colors.white, size: 22),
                ),
                const SizedBox(width: 14),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        title,
                        style: const TextStyle(
                          color: Colors.white,
                          fontSize: 15,
                          fontWeight: FontWeight.w600,
                        ),
                      ),
                      const SizedBox(height: 3),
                      Text(
                        subtitle,
                        style: TextStyle(
                          color: Colors.white.withValues(alpha: 0.45),
                          fontSize: 12,
                        ),
                      ),
                    ],
                  ),
                ),
                const Icon(Icons.chevron_right, color: Colors.white38, size: 20),
              ],
            ),
          ),
        ),
      ),
    );
  }
}
