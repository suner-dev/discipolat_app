import 'package:flutter/material.dart';
import '../../../data/services/api_service.dart';
import '../../widgets/glass_theme.dart';
import '../../widgets/app_drawer.dart';

/// Dashboard du Responsable de département.
/// Basé sur les MEMBRES (et non les disciples) : stats par département
/// avec sélection dynamique quand le responsable gère plusieurs départements.
class ResponsableDashboardScreen extends StatefulWidget {
  const ResponsableDashboardScreen({super.key});

  @override
  State<ResponsableDashboardScreen> createState() => _ResponsableDashboardScreenState();
}

class _ResponsableDashboardScreenState extends State<ResponsableDashboardScreen>
    with SingleTickerProviderStateMixin {
  final _apiService = ApiService();
  Map<String, dynamic>? _dashboard;
  bool _isLoading = true;
  String? _selectedDeptId;

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

  Future<void> _loadData({String? deptId}) async {
    try {
      final uri = deptId != null && deptId.isNotEmpty
          ? '/dashboard/responsable?deptId=$deptId'
          : '/dashboard/responsable';
      final response = await _apiService.get(uri);
      if (mounted) {
        setState(() {
          _dashboard = response.data as Map<String, dynamic>?;
          _isLoading = false;
          _selectedDeptId = _dashboard?['selectedDeptId'] as String?;
        });
        _animCtrl.forward(from: 0);
      }
    } catch (_) {
      if (mounted) setState(() => _isLoading = false);
    }
  }

  Future<void> _onDeptChanged(String? deptId) async {
    if (deptId == null) return;
    setState(() => _isLoading = true);
    await _loadData(deptId: deptId);
  }

  @override
  Widget build(BuildContext context) {
    final stats = _dashboard?['statistiques'] as Map<String, dynamic>? ?? {};
    final departements = _dashboard?['departements'] as List<dynamic>? ?? [];
    final dept = _dashboard?['departement'] as Map<String, dynamic>? ?? {};
    final annivs = dept['anniversaires'] as List<dynamic>? ?? [];

    return Scaffold(
      appBar: AppBar(
        title: const Text('Espace Responsable'),
        actions: [
          IconButton(icon: const Icon(Icons.refresh), onPressed: () => _loadData(deptId: _selectedDeptId)),
        ],
      ),
      drawer: const AppDrawer(),
      body: _isLoading
          ? const ShimmerLoading(itemCount: 6)
          : RefreshIndicator(
              onRefresh: () => _loadData(deptId: _selectedDeptId),
              child: FadeTransition(
                opacity: _fadeAnim,
                child: SingleChildScrollView(
                  physics: const AlwaysScrollableScrollPhysics(),
                  padding: const EdgeInsets.all(16),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      // Sélecteur de département (multi-départements)
                      if (departements.length > 1) ...[
                        SectionTitle(title: 'Département à administrer', icon: Icons.swap_horiz),
                        DropdownButtonFormField<String>(
                          initialValue: _selectedDeptId,
                          dropdownColor: AppColors.cardDark,
                          decoration: const InputDecoration(
                            labelText: 'Choisir le département',
                            prefixIcon: Icon(Icons.business),
                          ),
                          items: departements.map<DropdownMenuItem<String>>((d) {
                            final dp = d as Map<String, dynamic>;
                            return DropdownMenuItem(
                              value: dp['id'] as String?,
                              child: Text(dp['nom'] ?? '', style: const TextStyle(color: Colors.white)),
                            );
                          }).toList(),
                          onChanged: _onDeptChanged,
                        ),
                        const SizedBox(height: 8),
                      ],

                      // Vue d'ensemble (tous départements)
                      SectionTitle(title: 'Vue d\'ensemble', icon: Icons.dashboard),
                      const SizedBox(height: 8),
                      GridView.builder(
                        shrinkWrap: true,
                        physics: const NeverScrollableScrollPhysics(),
                        gridDelegate: const SliverGridDelegateWithFixedCrossAxisCount(
                          crossAxisCount: 3, childAspectRatio: 1.2, crossAxisSpacing: 8, mainAxisSpacing: 8,
                        ),
                        itemCount: 6,
                        itemBuilder: (_, i) {
                          final items = [
                            {'label': 'Membres', 'value': '${stats['totalMembres'] ?? 0}', 'icon': Icons.people, 'color': Colors.blue},
                            {'label': 'Actifs', 'value': '${stats['totalActifs'] ?? 0}', 'icon': Icons.check_circle, 'color': Colors.green},
                            {'label': 'Nouveaux', 'value': '${stats['nouveauxMembres'] ?? 0}', 'icon': Icons.person_add, 'color': Colors.teal},
                            {'label': 'Départements', 'value': '${stats['totalDepartements'] ?? 0}', 'icon': Icons.business, 'color': Colors.purple},
                            {'label': 'Rapports', 'value': '${stats['rapportsSoumis'] ?? 0}', 'icon': Icons.description, 'color': Colors.amber},
                            {'label': 'Taux', 'value': '${stats['tauxCompletion'] ?? 0}%', 'icon': Icons.pie_chart, 'color': Colors.cyan},
                          ];
                          final item = items[i];
                          return GlassStatCard(
                            label: item['label'] as String,
                            value: item['value'] as String,
                            icon: item['icon'] as IconData,
                            gradientStart: item['color'] as Color,
                            gradientEnd: (item['color'] as Color).withValues(alpha: 0.7),
                          );
                        },
                      ),
                      const SizedBox(height: 16),

                      // Département sélectionné
                      if (dept.isNotEmpty) ...[
                        SectionTitle(
                          title: _dashboard?['selectedDeptNom'] ?? 'Mon département',
                          icon: Icons.business,
                        ),
                        GlassCard(
                          padding: const EdgeInsets.all(16),
                          child: Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              Row(
                                mainAxisAlignment: MainAxisAlignment.spaceAround,
                                children: [
                                  _statChip('Membres', '${dept['totalMembres'] ?? 0}', Colors.blue),
                                  _statChip('Nouveaux', '${dept['nouveauxMembres'] ?? 0}', Colors.teal),
                                  _statChip('Actifs', '${dept['actifs'] ?? 0}', Colors.green),
                                  _statChip('Décrochés', '${dept['decroches'] ?? 0}', Colors.red),
                                ],
                              ),
                              const SizedBox(height: 16),
                              Row(
                                mainAxisAlignment: MainAxisAlignment.spaceAround,
                                children: [
                                  _statChip('Présents', '${dept['presents'] ?? 0}', Colors.green),
                                  _statChip('Absents', '${dept['absents'] ?? 0}', Colors.orange),
                                  _statChip('Présence', '${dept['tauxPresence'] ?? 0}%', Colors.amber),
                                  _statChip('Rapports', '${dept['rapportsSoumis'] ?? 0}', Colors.purple),
                                ],
                              ),
                            ],
                          ),
                        ),
                      ],

                      // Anniversaires du département
                      if (annivs.isNotEmpty) ...[
                        const SizedBox(height: 16),
                        SectionTitle(title: 'Anniversaires', icon: Icons.cake),
                        ...annivs.take(5).map((a) {
                          final m = a as Map<String, dynamic>;
                          return GlassCard(
                            margin: const EdgeInsets.only(bottom: 8),
                            padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 10),
                            child: Row(
                              children: [
                                GradientAvatar(
                                  text: '${m['prenom'] ?? ''} ${m['nom'] ?? ''}',
                                  radius: 18,
                                ),
                                const SizedBox(width: 10),
                                Expanded(
                                  child: Text(
                                    '${m['prenom'] ?? ''} ${m['nom'] ?? ''}'.trim(),
                                    style: const TextStyle(color: Colors.white, fontWeight: FontWeight.w600),
                                  ),
                                ),
                                Text(
                                  '${m['dateNaissance'] ?? ''}'.substring(5, 10),
                                  style: TextStyle(color: AppColors.accent, fontSize: 12),
                                ),
                              ],
                            ),
                          );
                        }),
                      ],

                      // Liste des départements gérés
                      if (departements.isNotEmpty) ...[
                        const SizedBox(height: 16),
                        SectionTitle(title: 'Mes départements', icon: Icons.apartment),
                        ...departements.map((d) {
                          final dp = d as Map<String, dynamic>;
                          final isSelected = dp['id'] == _selectedDeptId;
                          return GlassCard(
                            margin: const EdgeInsets.only(bottom: 10),
                            padding: const EdgeInsets.all(16),
                            onTap: () => _onDeptChanged(dp['id'] as String?),
                            borderColor: isSelected ? AppColors.primary.withValues(alpha: 0.6) : null,
                            child: Row(
                              children: [
                                Container(
                                  padding: const EdgeInsets.all(10),
                                  decoration: BoxDecoration(
                                    color: AppColors.primary.withValues(alpha: 0.15),
                                    borderRadius: BorderRadius.circular(12),
                                  ),
                                  child: Icon(Icons.business, color: AppColors.primaryLight, size: 20),
                                ),
                                const SizedBox(width: 12),
                                Expanded(
                                  child: Column(
                                    crossAxisAlignment: CrossAxisAlignment.start,
                                    children: [
                                      Text(
                                        dp['nom'] ?? '',
                                        style: const TextStyle(color: Colors.white, fontWeight: FontWeight.bold, fontSize: 15),
                                      ),
                                      if (dp['description'] != null)
                                        Text(
                                          dp['description'],
                                          maxLines: 1,
                                          overflow: TextOverflow.ellipsis,
                                          style: TextStyle(color: Colors.white.withValues(alpha: 0.4), fontSize: 11),
                                        ),
                                    ],
                                  ),
                                ),
                                if (isSelected)
                                  Icon(Icons.check_circle, color: AppColors.primary, size: 20),
                                const Icon(Icons.chevron_right, color: Colors.white24),
                              ],
                            ),
                          );
                        }),
                      ],

                      const SizedBox(height: 80),
                    ],
                  ),
                ),
              ),
            ),
    );
  }

  Widget _statChip(String label, String value, Color color) {
    return Column(
      children: [
        Text(value, style: TextStyle(color: color, fontWeight: FontWeight.bold, fontSize: 16)),
        Text(label, style: TextStyle(color: Colors.white.withValues(alpha: 0.4), fontSize: 9)),
      ],
    );
  }
}
