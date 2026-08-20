import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import '../../../data/services/api_service.dart';
import '../../widgets/glass_theme.dart';
import '../../widgets/app_drawer.dart';

/// Dashboard du Responsable de département.
/// Basé sur les MEMBRES (et non les disciples) : stats par département
/// avec sélection dynamique quand le responsable gère plusieurs départements.
class ResponsableDashboardScreen extends StatefulWidget {
  final ApiService? apiService;
  const ResponsableDashboardScreen({super.key, this.apiService});

  @override
  State<ResponsableDashboardScreen> createState() => _ResponsableDashboardScreenState();
}

class _ResponsableDashboardScreenState extends State<ResponsableDashboardScreen>
    with SingleTickerProviderStateMixin {
  late final ApiService _apiService = widget.apiService ?? ApiService();
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
    final membresSuivi = dept['membresSuivi'] as List<dynamic>? ?? [];
    final alertes = dept['alertes'] as List<dynamic>? ?? [];
    final evenementsAvenir = dept['evenementsAvenir'] as List<dynamic>? ?? [];

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
                        DropdownButton<String>(
                          value: _selectedDeptId,
                          dropdownColor: AppColors.cardDark,
                          isExpanded: true,
                          underline: const SizedBox(),
                          style: const TextStyle(color: Colors.white, fontSize: 14),
                          items: departements.map<DropdownMenuItem<String>>((d) {
                            final dp = d as Map<String, dynamic>;
                            return DropdownMenuItem(
                              value: dp['id'] as String?,
                              child: Text(dp['nom'] ?? ''),
                            );
                          }).toList(),
                          onChanged: _onDeptChanged,
                        ),
                        const SizedBox(height: 8),
                      ],

                      // ==================== STATISTIQUES GLOBALES ====================
                      SectionTitle(title: 'Vue d\'ensemble', icon: Icons.dashboard),
                      const SizedBox(height: 8),
                      GridView.builder(
                        shrinkWrap: true,
                        physics: const NeverScrollableScrollPhysics(),
                        gridDelegate: const SliverGridDelegateWithFixedCrossAxisCount(
                          crossAxisCount: 3, childAspectRatio: 1.2, crossAxisSpacing: 8, mainAxisSpacing: 8,
                        ),
                        itemCount: 9,
                        itemBuilder: (_, i) {
                          final items = [
                            {'label': 'Membres', 'value': '${stats['totalMembres'] ?? 0}', 'icon': Icons.people, 'color': Colors.blue},
                            {'label': 'Actifs', 'value': '${stats['totalActifs'] ?? 0}', 'icon': Icons.check_circle, 'color': Colors.green},
                            {'label': 'Nouveaux', 'value': '${stats['nouveauxMembres'] ?? 0}', 'icon': Icons.person_add, 'color': Colors.teal},
                            {'label': 'Présence', 'value': '${stats['tauxPresence'] ?? 0}%', 'icon': Icons.trending_up, 'color': Colors.purple},
                            {'label': 'Rapports', 'value': '${stats['rapportsSoumis'] ?? 0}/${stats['rapportsAttendus'] ?? 0}', 'icon': Icons.description, 'color': Colors.amber},
                            {'label': 'Taux', 'value': '${stats['tauxCompletion'] ?? 0}%', 'icon': Icons.pie_chart, 'color': Colors.cyan},
                            {'label': 'Équipes', 'value': '${stats['equipesActives'] ?? 0}', 'icon': Icons.account_tree, 'color': Colors.orange},
                            {'label': 'Tâches retard', 'value': '${stats['tachesEnRetard'] ?? 0}', 'icon': Icons.alarm, 'color': Colors.red},
                            {'label': 'Transferts', 'value': '${stats['transfertsEnAttente'] ?? 0}', 'icon': Icons.swap_horiz, 'color': Colors.indigo},
                          ];
                          final item = items[i];
                          return GlassStatCard(
                            label: item['label'] as String,
                            value: item['value'] as String,
                            icon: item['icon'] as IconData,
                            gradientStart: item['color'] as Color,
                            gradientEnd: (item['color'] as Color).withValues(alpha: 0.7),
                            onTap: () {
                              final label = item['label'] as String;
                              if (label == 'Membres' || label == 'Actifs' || label == 'Nouveaux') {
                                context.go('/departments/$_selectedDeptId');
                              } else if (label == 'Présence' || label == 'Taux') {
                                context.go('/departments/$_selectedDeptId/stats');
                              } else if (label == 'Rapports') {
                                context.go('/reports');
                              } else if (label == 'Équipes' || label == 'Tâches retard') {
                                context.go('/departments/$_selectedDeptId/manage');
                              } else {
                                context.go('/transfers');
                              }
                            },
                          );
                        },
                      ),
                      const SizedBox(height: 16),

                      // ==================== DÉPARTEMENT SÉLECTIONNÉ ====================
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
                              const SizedBox(height: 16),
                              Row(
                                children: [
                                  Expanded(
                                    child: FilledButton.icon(
                                      icon: const Icon(Icons.account_tree, size: 18),
                                      label: const Text('Gestion du département'),
                                      onPressed: () => context.go('/departments/$_selectedDeptId/manage'),
                                    ),
                                  ),
                                ],
                              ),
                            ],
                          ),
                        ),
                        const SizedBox(height: 16),
                      ],

                      // ==================== TÂCHES EN RETARD ====================
                      if ((stats['tachesEnRetard'] ?? 0) > 0) ...[
                        SectionTitle(
                          title: 'Tâches en retard',
                          icon: Icons.alarm,
                          trailing: Container(
                            padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 2),
                            decoration: BoxDecoration(
                              color: Colors.red.withValues(alpha: 0.15),
                              borderRadius: BorderRadius.circular(12),
                            ),
                            child: Text('${stats['tachesEnRetard']}', style: const TextStyle(color: Colors.red, fontSize: 12, fontWeight: FontWeight.bold)),
                          ),
                        ),
                        GlassCard(
                          padding: const EdgeInsets.all(16),
                          onTap: () => context.go('/departments/$_selectedDeptId/manage'),
                          child: Row(
                            children: [
                              Container(
                                padding: const EdgeInsets.all(10),
                                decoration: BoxDecoration(
                                  color: Colors.red.withValues(alpha: 0.15),
                                  borderRadius: BorderRadius.circular(12),
                                ),
                                child: const Icon(Icons.alarm, color: Colors.red, size: 20),
                              ),
                              const SizedBox(width: 12),
                              Expanded(
                                child: Column(
                                  crossAxisAlignment: CrossAxisAlignment.start,
                                  children: [
                                    Text('${stats['tachesEnRetard']} tâche${(stats['tachesEnRetard'] ?? 0) > 1 ? 's' : ''} en retard',
                                        style: const TextStyle(color: Colors.white, fontWeight: FontWeight.w600)),
                                    Text('${stats['tachesOuvertes'] ?? 0} tâches ouvertes au total',
                                        style: TextStyle(color: Colors.white.withValues(alpha: 0.4), fontSize: 11)),
                                  ],
                                ),
                              ),
                              Icon(Icons.chevron_right, color: Colors.white.withValues(alpha: 0.3)),
                            ],
                          ),
                        ),
                        const SizedBox(height: 16),
                      ],

                      // ==================== ÉQUIPES & POSTES ====================
                      SectionTitle(title: 'Équipes & Postes', icon: Icons.account_tree),
                      GlassCard(
                        padding: const EdgeInsets.all(16),
                        onTap: () => context.go('/departments/$_selectedDeptId/manage'),
                        child: Row(
                          children: [
                            _statChip('Équipes', '${stats['equipesActives'] ?? 0}', Colors.orange),
                            const SizedBox(width: 24),
                            _statChip('Postes', '${stats['postesActifs'] ?? 0}', Colors.indigo),
                            const Spacer(),
                            Icon(Icons.chevron_right, color: Colors.white.withValues(alpha: 0.3)),
                          ],
                        ),
                      ),
                      const SizedBox(height: 16),

                      // ==================== PROGRESSION DES RAPPORTS ====================
                      SectionTitle(title: 'Rapports', icon: Icons.description),
                      GlassCard(
                        padding: const EdgeInsets.all(16),
                        onTap: () => context.go('/reports'),
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Row(
                              mainAxisAlignment: MainAxisAlignment.spaceBetween,
                              children: [
                                Text('Progression : ${stats['rapportsSoumis'] ?? 0} / ${stats['rapportsAttendus'] ?? 0}',
                                    style: const TextStyle(color: Colors.white, fontWeight: FontWeight.w600)),
                                Text('${stats['tauxCompletion'] ?? 0}%',
                                    style: TextStyle(color: AppColors.primaryLight, fontWeight: FontWeight.bold)),
                              ],
                            ),
                            const SizedBox(height: 8),
                            ClipRRect(
                              borderRadius: BorderRadius.circular(6),
                              child: LinearProgressIndicator(
                                value: ((stats['tauxCompletion'] ?? 0) as num).toDouble() / 100,
                                backgroundColor: Colors.white.withValues(alpha: 0.1),
                                valueColor: AlwaysStoppedAnimation(AppColors.primary),
                                minHeight: 8,
                              ),
                            ),
                          ],
                        ),
                      ),
                      const SizedBox(height: 16),

                      // ==================== TRANSFERTS EN ATTENTE ====================
                      if ((stats['transfertsEnAttente'] ?? 0) > 0) ...[
                        SectionTitle(title: 'Transferts en attente', icon: Icons.swap_horiz),
                        GlassCard(
                          padding: const EdgeInsets.all(16),
                          onTap: () => context.go('/transfers'),
                          child: Row(
                            children: [
                              Container(
                                padding: const EdgeInsets.all(10),
                                decoration: BoxDecoration(
                                  color: Colors.orange.withValues(alpha: 0.15),
                                  borderRadius: BorderRadius.circular(12),
                                ),
                                child: const Icon(Icons.swap_horiz, color: Colors.orange, size: 20),
                              ),
                              const SizedBox(width: 12),
                              Expanded(
                                child: Column(
                                  crossAxisAlignment: CrossAxisAlignment.start,
                                  children: [
                                    Text('${stats['transfertsEnAttente']} demande${(stats['transfertsEnAttente'] ?? 0) > 1 ? 's' : ''} de transfert',
                                        style: const TextStyle(color: Colors.white, fontWeight: FontWeight.w600)),
                                    Text('Des membres souhaitent changer de famille',
                                        style: TextStyle(color: Colors.white.withValues(alpha: 0.4), fontSize: 11)),
                                  ],
                                ),
                              ),
                              Icon(Icons.chevron_right, color: Colors.white.withValues(alpha: 0.3)),
                            ],
                          ),
                        ),
                        const SizedBox(height: 16),
                      ],

                      // ==================== ÉVÉNEMENTS À VENIR ====================
                      if (evenementsAvenir.isNotEmpty) ...[
                        SectionTitle(title: 'Événements à venir', icon: Icons.event),
                        ...evenementsAvenir.take(5).map((ev) {
                          final e = ev as Map<String, dynamic>;
                          return GlassCard(
                            margin: const EdgeInsets.only(bottom: 8),
                            padding: const EdgeInsets.all(12),
                            onTap: () => context.go('/events'),
                            child: Row(
                              children: [
                                Container(
                                  padding: const EdgeInsets.all(8),
                                  decoration: BoxDecoration(
                                    color: AppColors.primary.withValues(alpha: 0.15),
                                    borderRadius: BorderRadius.circular(8),
                                  ),
                                  child: Icon(Icons.event, color: AppColors.primaryLight, size: 18),
                                ),
                                const SizedBox(width: 12),
                                Expanded(
                                  child: Column(
                                    crossAxisAlignment: CrossAxisAlignment.start,
                                    children: [
                                      Text(e['titre'] ?? '', style: const TextStyle(color: Colors.white, fontWeight: FontWeight.w600, fontSize: 13)),
                                      Text('${e['dateDebut'] ?? '—'}${e['lieu'] != null ? ' · ${e['lieu']}' : ''}',
                                          style: TextStyle(color: Colors.white.withValues(alpha: 0.4), fontSize: 11)),
                                    ],
                                  ),
                                ),
                                Icon(Icons.chevron_right, color: Colors.white.withValues(alpha: 0.3)),
                              ],
                            ),
                          );
                        }),
                        const SizedBox(height: 16),
                      ],

                      // ==================== ALERTEZ ====================
                      if (alertes.isNotEmpty) ...[
                        SectionTitle(
                          title: 'Alertes à traiter',
                          icon: Icons.warning_amber,
                          trailing: Container(
                            padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 2),
                            decoration: BoxDecoration(
                              color: Colors.red.withValues(alpha: 0.15),
                              borderRadius: BorderRadius.circular(12),
                            ),
                            child: Text('${alertes.length}', style: const TextStyle(color: Colors.red, fontSize: 12, fontWeight: FontWeight.bold)),
                          ),
                        ),
                        ...alertes.take(5).map((a) {
                          final alert = a as Map<String, dynamic>;
                          return GlassCard(
                            margin: const EdgeInsets.only(bottom: 8),
                            padding: const EdgeInsets.all(12),
                            onTap: () => context.go('/alerts'),
                            borderColor: Colors.red.withValues(alpha: 0.3),
                            child: Row(
                              children: [
                                Icon(Icons.warning_amber_rounded,
                                    color: alert['priorite'] == 'HAUTE' || alert['priorite'] == 'URGENTE' ? Colors.red : Colors.amber,
                                    size: 18),
                                const SizedBox(width: 12),
                                Expanded(
                                  child: Column(
                                    crossAxisAlignment: CrossAxisAlignment.start,
                                    children: [
                                      Text(alert['titre'] ?? alert['message'] ?? '',
                                          style: const TextStyle(color: Colors.white, fontWeight: FontWeight.w600, fontSize: 13),
                                          maxLines: 1, overflow: TextOverflow.ellipsis),
                                      Text(alert['message'] ?? '',
                                          style: TextStyle(color: Colors.white.withValues(alpha: 0.4), fontSize: 11),
                                          maxLines: 1, overflow: TextOverflow.ellipsis),
                                    ],
                                  ),
                                ),
                              ],
                            ),
                          );
                        }),
                        const SizedBox(height: 16),
                      ],

                      // ==================== ANNIVERSAIRES ====================
                      if (annivs.isNotEmpty) ...[
                        SectionTitle(title: 'Anniversaires du mois', icon: Icons.cake),
                        ...annivs.take(5).map((a) {
                          final m = a as Map<String, dynamic>;
                          return GlassCard(
                            margin: const EdgeInsets.only(bottom: 8),
                            padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 10),
                            onTap: () => context.go('/departments/$_selectedDeptId/manage'),
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
                                  '${m['dateNaissance'] ?? ''}'.length >= 10
                                      ? '${m['dateNaissance']}'.substring(5, 10)
                                      : '${m['dateNaissance'] ?? ''}',
                                  style: TextStyle(color: AppColors.accent, fontSize: 12),
                                ),
                              ],
                            ),
                          );
                        }),
                        const SizedBox(height: 16),
                      ],

                      // ==================== À SUIVRE CETTE SEMAINE ====================
                      if (membresSuivi.isNotEmpty) ...[
                        SectionTitle(
                          title: 'À suivre cette semaine',
                          icon: Icons.person_search,
                          trailing: Container(
                            padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 2),
                            decoration: BoxDecoration(
                              color: Colors.amber.withValues(alpha: 0.15),
                              borderRadius: BorderRadius.circular(12),
                            ),
                            child: Text('${membresSuivi.length}', style: const TextStyle(color: Colors.amber, fontSize: 12, fontWeight: FontWeight.bold)),
                          ),
                        ),
                        ...membresSuivi.take(5).map((m) {
                          final member = m as Map<String, dynamic>;
                          return GlassCard(
                            margin: const EdgeInsets.only(bottom: 8),
                            padding: const EdgeInsets.all(12),
                            onTap: () => context.go('/departments/$_selectedDeptId/members/${member['id']}'),
                            child: Row(
                              children: [
                                GradientAvatar(text: '${member['nom'] ?? ''}', radius: 16),
                                const SizedBox(width: 10),
                                Expanded(
                                  child: Column(
                                    crossAxisAlignment: CrossAxisAlignment.start,
                                    children: [
                                      Text(member['nom'] ?? '', style: const TextStyle(color: Colors.white, fontWeight: FontWeight.w600, fontSize: 13)),
                                      Text(member['statut'] ?? '', style: TextStyle(color: Colors.white.withValues(alpha: 0.4), fontSize: 11)),
                                    ],
                                  ),
                                ),
                                Icon(Icons.chevron_right, color: Colors.white.withValues(alpha: 0.3)),
                              ],
                            ),
                          );
                        }),
                        const SizedBox(height: 16),
                      ],

                      // ==================== ACTIONS RAPIDES ====================
                      SectionTitle(title: 'Actions rapides', icon: Icons.flash_on),
                      GridView.count(
                        shrinkWrap: true,
                        physics: const NeverScrollableScrollPhysics(),
                        crossAxisCount: 4,
                        crossAxisSpacing: 8,
                        mainAxisSpacing: 8,
                        childAspectRatio: 0.9,
                        children: [
                          _quickAction(Icons.account_tree, 'Gestion', () => context.go('/departments/$_selectedDeptId/manage')),
                          _quickAction(Icons.trending_up, 'Stats', () => context.go('/departments/$_selectedDeptId/stats')),
                          _quickAction(Icons.description, 'Rapport', () => context.go('/reports')),
                          _quickAction(Icons.event, 'Événements', () => context.go('/events')),
                          _quickAction(Icons.star, 'Évaluations', () => context.go('/evaluations')),
                          _quickAction(Icons.warning_amber, 'Alertes', () => context.go('/alerts')),
                          _quickAction(Icons.swap_horiz, 'Transferts', () => context.go('/transfers')),
                          _quickAction(Icons.mail, 'Demandes', () => context.go('/members/requests')),
                        ],
                      ),
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

  Widget _quickAction(IconData icon, String label, VoidCallback onTap) {
    return GestureDetector(
      onTap: onTap,
      child: Container(
        decoration: BoxDecoration(
          color: Colors.white.withValues(alpha: 0.05),
          borderRadius: BorderRadius.circular(12),
          border: Border.all(color: Colors.white.withValues(alpha: 0.08)),
        ),
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Icon(icon, color: AppColors.primaryLight, size: 22),
            const SizedBox(height: 4),
            Text(label, style: TextStyle(color: Colors.white.withValues(alpha: 0.7), fontSize: 10)),
          ],
        ),
      ),
    );
  }
}
