import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import '../../widgets/glass_theme.dart';
import '../../widgets/app_drawer.dart';
import '../../../data/services/api_service.dart';

const _cibleLabels = <String, String>{
  'TOUS': 'Tout le département',
  'EQUIPE': 'Une équipe',
  'POSTE': 'Un poste',
};

const _alertLabels = <String, String>{
  'ABSENCE_REPETEE': 'Absences répétées',
  'TACHE_EN_RETARD': 'Tâche en retard',
};

/// Détail d'un département (parité web) : KPIs, alertes (âmes non assignées,
/// alertes intelligentes), annonces, membres (recherche, ajout/création,
/// retrait) et familles, avec navigation vers la gestion, les stats et le
/// rapport hebdomadaire.
class DepartmentDetailScreen extends StatefulWidget {
  final String departmentId;

  const DepartmentDetailScreen({super.key, required this.departmentId});

  @override
  State<DepartmentDetailScreen> createState() => _DepartmentDetailScreenState();
}

class _DepartmentDetailScreenState extends State<DepartmentDetailScreen> {
  final _apiService = ApiService();
  Map<String, dynamic>? _detail;
  Map<String, dynamic>? _kpi;
  List<dynamic> _members = [];
  List<dynamic> _unassigned = [];
  List<dynamic> _announcements = [];
  List<dynamic> _alerts = [];
  List<dynamic> _teams = [];
  List<dynamic> _positions = [];
  bool _isLoading = true;
  String _searchQuery = '';

  String get _deptId => widget.departmentId;

  @override
  void initState() {
    super.initState();
    _loadData();
  }

  Future<void> _loadData() async {
    setState(() => _isLoading = true);
    try {
      final results = await Future.wait([
        _apiService.get('/departments/$_deptId/detail'),
        _apiService.get('/departments/$_deptId/kpi'),
        _apiService.get('/departments/$_deptId/members', params: {'size': '200'}),
        _apiService.get('/departments/$_deptId/unassigned'),
        _apiService.get('/departments/$_deptId/announcements'),
        _apiService.get('/departments/$_deptId/alerts/smart'),
        _apiService.get('/departments/$_deptId/management'),
      ]);
      if (!mounted) return;
      final overview = results[6].data as Map<String, dynamic>? ?? {};
      setState(() {
        _detail = results[0].data as Map<String, dynamic>?;
        _kpi = results[1].data as Map<String, dynamic>?;
        _members = (results[2].data is Map
                ? results[2].data['content']
                : results[2].data) as List<dynamic>? ??
            [];
        _unassigned = results[3].data as List<dynamic>? ?? [];
        _announcements = results[4].data as List<dynamic>? ?? [];
        _alerts = results[5].data as List<dynamic>? ?? [];
        _teams = (overview['teams'] as List<dynamic>? ?? [])
            .where((t) => (t as Map)['statut'] == 'ACTIVE')
            .toList();
        _positions = (overview['positions'] as List<dynamic>? ?? [])
            .where((p) => (p as Map)['statut'] == 'ACTIVE')
            .toList();
        _isLoading = false;
      });
    } catch (_) {
      if (mounted) setState(() => _isLoading = false);
    }
  }

  Future<void> _reload() async {
    await _loadData();
  }

  List<dynamic> get _filteredMembers {
    if (_searchQuery.isEmpty) return _members;
    return _members.where((m) {
      final nom = (m as Map)['nom']?.toString().toLowerCase() ?? '';
      final email = m['email']?.toString().toLowerCase() ?? '';
      final q = _searchQuery.toLowerCase();
      return nom.contains(q) || email.contains(q);
    }).toList();
  }

  @override
  Widget build(BuildContext context) {
    final dept = _detail ?? {};
    return Scaffold(
      appBar: AppBar(
        title: Text(dept['nom'] ?? 'Département'),
        actions: [
          IconButton(icon: const Icon(Icons.bar_chart), tooltip: 'Statistiques',
              onPressed: () => context.go('/departments/$_deptId/stats')),
          IconButton(icon: const Icon(Icons.refresh), onPressed: _reload),
        ],
      ),
      drawer: const AppDrawer(),
      body: _isLoading
          ? const ShimmerLoading(itemCount: 6)
          : RefreshIndicator(
              onRefresh: _reload,
              child: ListView(
                padding: const EdgeInsets.all(12),
                children: [
                  _headerCard(context, dept),
                  const SizedBox(height: 12),
                  _actionRow(context),
                  if (_kpi != null) ...[
                    const SizedBox(height: 12),
                    _kpiGrid(_kpi!),
                    const SizedBox(height: 12),
                    _participationCard(_kpi!),
                  ],
                  if (_unassigned.isNotEmpty) ...[
                    const SizedBox(height: 12),
                    _unassignedCard(),
                  ],
                  const SizedBox(height: 12),
                  _announcementsCard(context),
                  const SizedBox(height: 12),
                  _alertsCard(),
                  const SizedBox(height: 12),
                  _membersCard(context),
                  const SizedBox(height: 12),
                  _familiesCard(context),
                  const SizedBox(height: 80),
                ],
              ),
            ),
    );
  }

  // ============================================================
  // HEADER
  // ============================================================

  Widget _headerCard(BuildContext context, Map<String, dynamic> dept) {
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
                ),
                child: const Icon(Icons.business, color: Colors.white, size: 26),
              ),
              const SizedBox(width: 12),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(dept['nom'] ?? 'Département',
                        style: const TextStyle(
                            color: Colors.white, fontWeight: FontWeight.w700, fontSize: 18)),
                    const SizedBox(height: 2),
                    Text(dept['description'] ?? 'Aucune description',
                        style: TextStyle(
                            color: Colors.white.withValues(alpha: 0.6), fontSize: 12),
                        maxLines: 2,
                        overflow: TextOverflow.ellipsis),
                  ],
                ),
              ),
            ],
          ),
          const SizedBox(height: 12),
          const GlassDivider(),
          const SizedBox(height: 10),
          Row(
            children: [
              Icon(Icons.person_pin, color: AppColors.primary, size: 16),
              const SizedBox(width: 6),
              Expanded(
                child: Text('Responsable : ${dept['responsableNom'] ?? 'N/A'}',
                    style: TextStyle(
                        color: Colors.white.withValues(alpha: 0.7), fontSize: 12)),
              ),
            ],
          ),
          if ((dept['responsableEmail'] ?? '').toString().isNotEmpty) ...[
            const SizedBox(height: 4),
            Row(
              children: [
                Icon(Icons.mail_outline, color: Colors.white.withValues(alpha: 0.4), size: 14),
                const SizedBox(width: 6),
                Expanded(
                  child: Text('${dept['responsableEmail']}',
                      style: TextStyle(
                          color: Colors.white.withValues(alpha: 0.5), fontSize: 12)),
                ),
              ],
            ),
          ],
        ],
      ),
    );
  }

  Widget _actionRow(BuildContext context) {
    return Row(
      children: [
        Expanded(
          child: FilledButton.icon(
            icon: const Icon(Icons.account_tree, size: 18),
            label: const Text('Gérer'),
            onPressed: () => context.go('/departments/$_deptId/manage'),
          ),
        ),
        const SizedBox(width: 8),
        Expanded(
          child: OutlinedButton.icon(
            icon: const Icon(Icons.bar_chart, size: 18),
            label: const Text('Stats'),
            onPressed: () => context.go('/departments/$_deptId/stats'),
          ),
        ),
        const SizedBox(width: 8),
        Expanded(
          child: OutlinedButton.icon(
            icon: const Icon(Icons.description, size: 18),
            label: const Text('Rapport'),
            onPressed: () => context.go('/departments/$_deptId/report'),
          ),
        ),
      ],
    );
  }

  // ============================================================
  // KPIS
  // ============================================================

  Widget _kpiGrid(Map<String, dynamic> kpi) {
    final items = <(String, String, Color)>[
      ('Actifs', '${kpi['membresActifs'] ?? 0}', Colors.green),
      ('En intégration', '${kpi['membresEnIntegration'] ?? 0}', Colors.lightBlue),
      ('En veille', '${kpi['membresEnVeille'] ?? 0}', Colors.amber),
      ('Décrochés', '${kpi['membresDecroches'] ?? 0}', Colors.red),
      ('Nvx convertis', '${kpi['nouveauxConvertis'] ?? 0}', Colors.teal),
      ('Faiseurs', '${kpi['totalFaiseurs'] ?? 0}', Colors.purple),
    ];
    return GridView.builder(
      shrinkWrap: true,
      physics: const NeverScrollableScrollPhysics(),
      gridDelegate: const SliverGridDelegateWithFixedCrossAxisCount(
        crossAxisCount: 3, childAspectRatio: 1.35, crossAxisSpacing: 8, mainAxisSpacing: 8,
      ),
      itemCount: items.length,
      itemBuilder: (_, i) {
        final it = items[i];
        return GlassCard(
          padding: const EdgeInsets.all(10),
          child: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              Text(it.$2,
                  style: TextStyle(color: it.$3, fontSize: 20, fontWeight: FontWeight.bold)),
              const SizedBox(height: 2),
              Text(it.$1,
                  style: TextStyle(color: Colors.white.withValues(alpha: 0.5), fontSize: 10),
                  maxLines: 1,
                  overflow: TextOverflow.ellipsis),
            ],
          ),
        );
      },
    );
  }

  Widget _participationCard(Map<String, dynamic> kpi) {
    final soumission = ((kpi['tauxSoumission'] ?? 0) as num).toDouble().clamp(0, 100).toDouble();
    final presence = ((kpi['tauxPresence'] ?? 0) as num).toDouble().clamp(0, 100).toDouble();
    return GlassCard(
      padding: const EdgeInsets.all(14),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          SectionTitle(title: 'Participation', icon: Icons.query_stats),
          _progressRow('Taux de soumission', soumission, AppColors.primary,
              '${kpi['rapportsSoumisSemaine'] ?? 0}/${kpi['rapportsAttendusSemaine'] ?? 0} rapports cette semaine'),
          const SizedBox(height: 12),
          _progressRow('Présence moyenne', presence, Colors.green,
              '${kpi['totalPresents'] ?? 0} présents cette semaine'),
          const SizedBox(height: 12),
          Row(
            children: [
              Icon(Icons.description, color: Colors.white.withValues(alpha: 0.4), size: 16),
              const SizedBox(width: 8),
              Expanded(
                child: Text('Rapports famille soumis',
                    style: TextStyle(color: Colors.white.withValues(alpha: 0.6), fontSize: 13)),
              ),
              Text('${kpi['familyReportsSoumis'] ?? 0}/${kpi['totalFamilles'] ?? 0}',
                  style: const TextStyle(color: Colors.white, fontWeight: FontWeight.w600)),
            ],
          ),
        ],
      ),
    );
  }

  Widget _progressRow(String label, double value, Color color, String subtitle) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Row(
          children: [
            Expanded(
              child: Text(label,
                  style: TextStyle(color: Colors.white.withValues(alpha: 0.6), fontSize: 13)),
            ),
            Text('${value.round()}%',
                style: TextStyle(color: color, fontWeight: FontWeight.bold, fontSize: 14)),          ],
        ),
        const SizedBox(height: 6),
        ClipRRect(
          borderRadius: BorderRadius.circular(6),
          child: LinearProgressIndicator(
            value: value / 100,
            minHeight: 8,
            backgroundColor: Colors.white.withValues(alpha: 0.08),
            valueColor: AlwaysStoppedAnimation(color),
          ),
        ),
        const SizedBox(height: 4),
        Text(subtitle,
            style: TextStyle(color: Colors.white.withValues(alpha: 0.35), fontSize: 10)),
      ],
    );
  }

  // ============================================================
  // ÂMES NON ASSIGNÉES
  // ============================================================

  Widget _unassignedCard() {
    return GlassCard(
      padding: const EdgeInsets.all(14),
      borderColor: Colors.amber.withValues(alpha: 0.5),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              const Icon(Icons.warning_amber_rounded, color: Colors.amber, size: 20),
              const SizedBox(width: 8),
              Expanded(
                child: Text('Âmes non assignées à une famille (${_unassigned.length})',
                    style: const TextStyle(
                        color: Colors.white, fontWeight: FontWeight.w600, fontSize: 14)),
              ),
            ],
          ),
          const SizedBox(height: 4),
          Text(
            'Ces âmes sont suivies par des faiseurs du département mais ne sont assignées à aucune famille.',
            style: TextStyle(color: Colors.white.withValues(alpha: 0.5), fontSize: 11),
          ),
          const SizedBox(height: 8),
          ..._unassigned.take(20).map((s) {
            final m = s as Map<String, dynamic>;
            return ListTile(
              dense: true,
              contentPadding: EdgeInsets.zero,
              leading: GradientAvatar(text: '${m['nom'] ?? '?'}', radius: 14),
              title: Text('${m['nom'] ?? ''}',
                  style: const TextStyle(color: Colors.white, fontSize: 13)),
              subtitle: Text('Faiseur : ${m['faiseurNom'] ?? m['faiseurId']?.toString().substring(0, 8) ?? '—'}',
                  style: TextStyle(color: Colors.white.withValues(alpha: 0.4), fontSize: 11)),
              onTap: () {
                final soulId = m['id']?.toString();
                if (soulId != null && soulId.isNotEmpty) context.go('/souls/$soulId');
              },
            );
          }),
        ],
      ),
    );
  }

  // ============================================================
  // ANNONCES
  // ============================================================

  Widget _announcementsCard(BuildContext context) {
    return GlassCard(
      padding: const EdgeInsets.all(14),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          SectionTitle(
            title: 'Annonces du département (${_announcements.length})',
            icon: Icons.campaign,
            trailing: IconButton(
              icon: const Icon(Icons.add_circle, color: Colors.amber),
              tooltip: 'Publier une annonce',
              onPressed: () => _showCreateAnnouncement(context),
            ),
          ),
          if (_announcements.isEmpty)
            GlassCard(
              padding: const EdgeInsets.all(20),
              child: Text('Aucune annonce — publiez la première',
                  textAlign: TextAlign.center,
                  style: TextStyle(color: Colors.white.withValues(alpha: 0.5))),
            )
          else
            ..._announcements.map((a) {
              final m = a as Map<String, dynamic>;
              return GlassCard(
                margin: const EdgeInsets.only(bottom: 8),
                padding: const EdgeInsets.all(12),
                borderColor: Colors.amber.withValues(alpha: 0.35),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Row(
                      children: [
                        Expanded(
                          child: Text('${m['titre'] ?? ''}',
                              style: const TextStyle(
                                  color: Colors.white, fontWeight: FontWeight.w600, fontSize: 14)),
                        ),
                        StatusBadge(
                          label: _cibleLabels[m['cible']] ?? '${m['cible'] ?? ''}',
                          color: Colors.lightBlue,
                        ),
                        IconButton(
                          icon: Icon(Icons.delete_outline,
                              color: Colors.red.withValues(alpha: 0.6), size: 18),
                          tooltip: 'Supprimer',
                          onPressed: () => _deleteAnnouncement(context, m),
                        ),
                      ],
                    ),
                    Text('${m['message'] ?? ''}',
                        style: TextStyle(color: Colors.white.withValues(alpha: 0.6), fontSize: 12)),
                    const SizedBox(height: 4),
                    Text(
                      '${m['auteurNom'] ?? ''} · ${m['createdAt'] ?? ''}'
                      '${m['teamNom'] != null ? ' · ${m['teamNom']}' : ''}'
                      '${m['positionNom'] != null ? ' · ${m['positionNom']}' : ''}',
                      style: TextStyle(color: Colors.white.withValues(alpha: 0.35), fontSize: 10),
                    ),
                  ],
                ),
              );
            }),
        ],
      ),
    );
  }

  Future<void> _showCreateAnnouncement(BuildContext context) async {
    final titreCtrl = TextEditingController();
    final messageCtrl = TextEditingController();
    String cible = 'TOUS';
    String? teamId;
    String? positionId;

    await showDialog(
      context: context,
      builder: (ctx) => StatefulBuilder(
        builder: (ctx, setState) => AlertDialog(
          backgroundColor: AppColors.cardDark,
          title: const Text('Publier une annonce', style: TextStyle(color: Colors.white)),
          content: SingleChildScrollView(
            child: Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                TextField(
                  controller: titreCtrl,
                  style: const TextStyle(color: Colors.white),
                  decoration: const InputDecoration(labelText: 'Titre *'),
                ),
                const SizedBox(height: 12),
                TextField(
                  controller: messageCtrl,
                  style: const TextStyle(color: Colors.white),
                  maxLines: 3,
                  decoration: const InputDecoration(labelText: 'Message *'),
                ),
                const SizedBox(height: 12),
                DropdownButtonFormField<String>(
                  initialValue: cible,
                  dropdownColor: AppColors.cardDark,
                  style: const TextStyle(color: Colors.white),
                  items: _cibleLabels.entries.map((e) {
                    return DropdownMenuItem(
                        value: e.key,
                        child: Text(e.value, style: const TextStyle(color: Colors.white)));
                  }).toList(),
                  onChanged: (v) => setState(() {
                    cible = v ?? cible;
                    teamId = null;
                    positionId = null;
                  }),
                  decoration: const InputDecoration(labelText: 'Cible'),
                ),
                if (cible == 'EQUIPE' && _teams.isNotEmpty) ...[
                  const SizedBox(height: 12),
                  DropdownButtonFormField<String?>(
                    initialValue: teamId,
                    dropdownColor: AppColors.cardDark,
                    style: const TextStyle(color: Colors.white),
                    items: [
                      const DropdownMenuItem<String?>(
                          value: null,
                          child: Text('— Choisir —', style: TextStyle(color: Colors.white))),
                      ..._teams.map((t) => DropdownMenuItem<String?>(
                            value: (t as Map)['id'] as String?,
                            child: Text('${t['nom']}',
                                style: const TextStyle(color: Colors.white)),
                          )),
                    ],
                    onChanged: (v) => setState(() => teamId = v),
                    decoration: const InputDecoration(labelText: 'Équipe *'),
                  ),
                ],
                if (cible == 'POSTE' && _positions.isNotEmpty) ...[
                  const SizedBox(height: 12),
                  DropdownButtonFormField<String?>(
                    initialValue: positionId,
                    dropdownColor: AppColors.cardDark,
                    style: const TextStyle(color: Colors.white),
                    items: [
                      const DropdownMenuItem<String?>(
                          value: null,
                          child: Text('— Choisir —', style: TextStyle(color: Colors.white))),
                      ..._positions.map((p) => DropdownMenuItem<String?>(
                            value: (p as Map)['id'] as String?,
                            child: Text('${p['nom']}',
                                style: const TextStyle(color: Colors.white)),
                          )),
                    ],
                    onChanged: (v) => setState(() => positionId = v),
                    decoration: const InputDecoration(labelText: 'Poste *'),
                  ),
                ],
              ],
            ),
          ),
          actions: [
            TextButton(onPressed: () => Navigator.pop(ctx), child: const Text('Annuler')),
            FilledButton(
              onPressed: () async {
                if (titreCtrl.text.trim().isEmpty || messageCtrl.text.trim().isEmpty) return;
                try {
                  await _apiService.post('/departments/$_deptId/announcements', data: {
                    'titre': titreCtrl.text.trim(),
                    'message': messageCtrl.text.trim(),
                    'cible': cible,
                    'teamId': cible == 'EQUIPE' ? teamId : null,
                    'positionId': cible == 'POSTE' ? positionId : null,
                  });
                  if (ctx.mounted) Navigator.pop(ctx);
                  await _reload();
                } catch (_) {
                  if (ctx.mounted) {
                    ScaffoldMessenger.of(ctx).showSnackBar(
                        const SnackBar(content: Text('Échec de la publication')));
                  }
                }
              },
              child: const Text('Publier'),
            ),
          ],
        ),
      ),
    );
  }

  Future<void> _deleteAnnouncement(BuildContext context, Map<String, dynamic> a) async {
    final ok = await showDialog<bool>(
      context: context,
      builder: (ctx) => AlertDialog(
        backgroundColor: AppColors.cardDark,
        title: const Text('Supprimer l\'annonce', style: TextStyle(color: Colors.white)),
        content: Text('Supprimer « ${a['titre']} » ?',
            style: TextStyle(color: Colors.white.withValues(alpha: 0.8))),
        actions: [
          TextButton(onPressed: () => Navigator.pop(ctx, false), child: const Text('Annuler')),
          FilledButton(onPressed: () => Navigator.pop(ctx, true), child: const Text('Supprimer')),
        ],
      ),
    );
    if (ok == true) {
      try {
        await _apiService.delete('/departments/$_deptId/announcements/${a['id']}');
        await _reload();
      } catch (_) {}
    }
  }

  // ============================================================
  // ALERTES INTELLIGENTES
  // ============================================================

  Widget _alertsCard() {
    return GlassCard(
      padding: const EdgeInsets.all(14),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          SectionTitle(title: 'Alertes intelligentes (${_alerts.length})', icon: Icons.notifications_active),
          Text(
            'Détection automatique : absences répétées et tâches en retard. Ces alertes sont aussi visibles dans les dossiers membres.',
            style: TextStyle(color: Colors.white.withValues(alpha: 0.4), fontSize: 11),
          ),
          const SizedBox(height: 8),
          if (_alerts.isEmpty)
            GlassCard(
              padding: const EdgeInsets.all(20),
              child: Column(
                children: [
                  const Icon(Icons.check_circle, color: Colors.green, size: 32),
                  const SizedBox(height: 6),
                  Text('Aucune alerte en cours — tout est sous contrôle',
                      textAlign: TextAlign.center,
                      style: TextStyle(color: Colors.white.withValues(alpha: 0.5))),
                ],
              ),
            )
          else
            ..._alerts.map((a) {
              final m = a as Map<String, dynamic>;
              final haute = m['priorite'] == 'HAUTE';
              final color = haute ? Colors.red : Colors.amber;
              return GlassCard(
                margin: const EdgeInsets.only(bottom: 8),
                padding: const EdgeInsets.all(12),
                borderColor: color.withValues(alpha: 0.4),
                child: Row(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Icon(haute ? Icons.warning_amber_rounded : Icons.schedule,
                        color: color, size: 20),
                    const SizedBox(width: 10),
                    Expanded(
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Row(
                            children: [
                              Expanded(
                                child: Text('${m['titre'] ?? ''}',
                                    style: const TextStyle(
                                        color: Colors.white, fontWeight: FontWeight.w600, fontSize: 13)),
                              ),
                              StatusBadge(
                                label: _alertLabels[m['typeAlerte']] ??
                                    '${m['typeAlerte'] ?? ''}'.replaceAll('_', ' '),
                                color: color,
                              ),
                            ],
                          ),
                          if (m['message'] != null)
                            Text('${m['message']}',
                                style: TextStyle(
                                    color: Colors.white.withValues(alpha: 0.5), fontSize: 12)),
                          if (m['ameNom'] != null && m['ameId'] != null)
                            InkWell(
                              onTap: () => context.go('/departments/$_deptId/members/${m['ameId']}'),
                              child: Padding(
                                padding: const EdgeInsets.symmetric(vertical: 4),
                                child: Text('Voir le dossier de ${m['ameNom']}',
                                    style: TextStyle(
                                        color: AppColors.primaryLight,
                                        fontSize: 11,
                                        fontWeight: FontWeight.w600)),
                              ),
                            ),
                        ],
                      ),
                    ),
                  ],
                ),
              );
            }),
        ],
      ),
    );
  }

  // ============================================================
  // MEMBRES
  // ============================================================

  Widget _membersCard(BuildContext context) {
    final list = _filteredMembers;
    return GlassCard(
      padding: const EdgeInsets.all(14),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          SectionTitle(
            title: 'Membres du département (${_members.length})',
            icon: Icons.favorite,
            trailing: IconButton(
              icon: const Icon(Icons.person_add_alt_1, color: Colors.amber),
              tooltip: 'Ajouter un membre',
              onPressed: () => _showAddMember(context),
            ),
          ),
          Container(
            padding: const EdgeInsets.symmetric(horizontal: 12),
            decoration: BoxDecoration(
              color: Colors.white.withValues(alpha: 0.06),
              borderRadius: BorderRadius.circular(12),
            ),
            child: TextField(
              onChanged: (v) => setState(() => _searchQuery = v),
              style: const TextStyle(color: Colors.white),
              decoration: InputDecoration(
                hintText: 'Rechercher un membre...',
                hintStyle: TextStyle(color: Colors.white.withValues(alpha: 0.4)),
                prefixIcon: Icon(Icons.search, color: Colors.white.withValues(alpha: 0.4), size: 20),
                border: InputBorder.none,
              ),
            ),
          ),
          const SizedBox(height: 10),
          if (list.isEmpty)
            Padding(
              padding: const EdgeInsets.symmetric(vertical: 24),
              child: Center(
                child: Text(_searchQuery.isEmpty ? 'Aucun membre dans ce département' : 'Aucun résultat',
                    style: TextStyle(color: Colors.white.withValues(alpha: 0.5))),
              ),
            )
          else
            ...list.map((m) => _memberCard(context, m as Map<String, dynamic>)),
        ],
      ),
    );
  }

  Widget _memberCard(BuildContext context, Map<String, dynamic> m) {
    final statut = m['statut'] ?? 'ACTIF';
    final statutColor = statut == 'DECROCHE' ? Colors.grey : Colors.green;
    final typeDisciple = m['typeDisciple'] ?? 'NOUVEL_ARRIVANT';
    return GlassCard(
      margin: const EdgeInsets.only(bottom: 8),
      padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 10),
      onTap: () => context.go('/departments/$_deptId/members/${m['id']}'),
      child: Row(
        children: [
          GradientAvatar(text: '${m['nom'] ?? '?'}', radius: 20),
          const SizedBox(width: 10),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Row(
                  children: [
                    Flexible(
                      child: Text('${m['nom'] ?? ''}',
                          style: const TextStyle(
                              color: Colors.white, fontWeight: FontWeight.w600, fontSize: 14),
                          overflow: TextOverflow.ellipsis),
                    ),
                    const SizedBox(width: 6),
                    StatusBadge(
                      label: typeDisciple == 'NOUVEAU_CONVERTI' ? 'Nv converti' : 'Nv arrivant',
                      color: typeDisciple == 'NOUVEAU_CONVERTI' ? Colors.green : Colors.lightBlue,
                    ),
                  ],
                ),
                const SizedBox(height: 2),
                if (m['familleNom'] != null)
                  Text('Famille : ${m['familleNom']}',
                      style: TextStyle(color: Colors.white.withValues(alpha: 0.5), fontSize: 11)),
                if (m['faiseurNom'] != null)
                  Text('Faiseur : ${m['faiseurNom']}',
                      style: TextStyle(color: Colors.white.withValues(alpha: 0.4), fontSize: 11)),
              ],
            ),
          ),
          StatusBadge(
            label: '$statut'.replaceAll('_', ' '),
            color: statutColor,
          ),
          IconButton(
            icon: Icon(Icons.delete_outline, color: Colors.red.withValues(alpha: 0.6), size: 18),
            tooltip: 'Retirer du département',
            onPressed: () => _removeMember(context, m),
          ),
          Icon(Icons.chevron_right, color: Colors.white.withValues(alpha: 0.3)),
        ],
      ),
    );
  }

  Future<void> _removeMember(BuildContext context, Map<String, dynamic> m) async {
    final ok = await showDialog<bool>(
      context: context,
      builder: (ctx) => AlertDialog(
        backgroundColor: AppColors.cardDark,
        title: const Text('Retirer du département', style: TextStyle(color: Colors.white)),
        content: Text('Retirer « ${m['nom']} » de ce département ?',
            style: TextStyle(color: Colors.white.withValues(alpha: 0.8))),
        actions: [
          TextButton(onPressed: () => Navigator.pop(ctx, false), child: const Text('Annuler')),
          FilledButton(onPressed: () => Navigator.pop(ctx, true), child: const Text('Retirer')),
        ],
      ),
    );
    if (ok == true) {
      try {
        await _apiService.delete('/departments/$_deptId/members/${m['id']}');
        await _reload();
      } catch (_) {
        if (context.mounted) {
          ScaffoldMessenger.of(context).showSnackBar(
              const SnackBar(content: Text('Échec du retrait')));
        }
      }
    }
  }

  // ============================================================
  // FAMILLES
  // ============================================================

  Widget _familiesCard(BuildContext context) {
    final familles = (_detail?['familles'] as List<dynamic>? ?? [])
        .map((f) => f as Map<String, dynamic>)
        .toList();
    return GlassCard(
      padding: const EdgeInsets.all(14),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          SectionTitle(title: 'Familles du département (${familles.length})', icon: Icons.family_restroom),
          if (familles.isEmpty)
            Padding(
              padding: const EdgeInsets.symmetric(vertical: 24),
              child: Center(
                child: Text('Aucune famille dans ce département',
                    style: TextStyle(color: Colors.white.withValues(alpha: 0.5))),
              ),
            )
          else
            ...familles.map((f) => GlassCard(
                  margin: const EdgeInsets.only(bottom: 8),
                  padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 10),
                  onTap: () => context.go('/families'),
                  child: Row(
                    children: [
                      Container(
                        padding: const EdgeInsets.all(8),
                        decoration: BoxDecoration(
                          color: Colors.purple.withValues(alpha: 0.15),
                          borderRadius: BorderRadius.circular(10),
                        ),
                        child: const Icon(Icons.family_restroom,
                            color: Colors.purple, size: 20),
                      ),
                      const SizedBox(width: 10),
                      Expanded(
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Text('${f['nom'] ?? ''}',
                                style: const TextStyle(
                                    color: Colors.white, fontWeight: FontWeight.w600, fontSize: 13)),
                            Text('Chef : ${f['chefNom'] ?? '—'}',
                                style: TextStyle(
                                    color: Colors.white.withValues(alpha: 0.4), fontSize: 11)),
                          ],
                        ),
                      ),
                      Text('${f['totalMembres'] ?? 0} membres',
                          style: TextStyle(
                              color: Colors.white.withValues(alpha: 0.5), fontSize: 11)),
                    ],
                  ),
                )),
        ],
      ),
    );
  }

  // ============================================================
  // AJOUT / CRÉATION DE MEMBRE
  // ============================================================

  Future<void> _showAddMember(BuildContext context) async {
    final sheet = _AddMemberSheet(deptId: _deptId, api: _apiService, onDone: _reload);
    await showModalBottomSheet(
      context: context,
      isScrollControlled: true,
      backgroundColor: AppColors.cardDark,
      shape: const RoundedRectangleBorder(
        borderRadius: BorderRadius.vertical(top: Radius.circular(24)),
      ),
      builder: (ctx) => Padding(
        padding: EdgeInsets.only(bottom: MediaQuery.of(ctx).viewInsets.bottom),
        child: sheet,
      ),
    );
  }
}

// ============================================================
// FEUILLE D'AJOUT / CRÉATION DE MEMBRE
// ============================================================

class _AddMemberSheet extends StatefulWidget {
  final String deptId;
  final ApiService api;
  final VoidCallback onDone;

  const _AddMemberSheet({
    required this.deptId,
    required this.api,
    required this.onDone,
  });

  @override
  State<_AddMemberSheet> createState() => _AddMemberSheetState();
}

class _AddMemberSheetState extends State<_AddMemberSheet> {
  bool _modeCreate = true;
  bool _saving = false;

  // Création
  final _nomCtrl = TextEditingController();
  final _prenomCtrl = TextEditingController();
  final _emailCtrl = TextEditingController();
  final _telephoneCtrl = TextEditingController();
  final _professionCtrl = TextEditingController();
  final _situationCtrl = TextEditingController();
  String _typeDisciple = 'NOUVEL_ARRIVANT';
  String _statut = 'EN_INTEGRATION';
  String _dateIntegration = '';
  String _dateConversion = '';

  // Existant
  String _query = '';
  List<dynamic> _candidates = [];
  bool _searching = false;
  Map<String, dynamic>? _selected;

  @override
  void dispose() {
    _nomCtrl.dispose();
    _prenomCtrl.dispose();
    _emailCtrl.dispose();
    _telephoneCtrl.dispose();
    _professionCtrl.dispose();
    _situationCtrl.dispose();
    super.dispose();
  }

  Future<void> _search(String q) async {
    if (q.trim().length < 2) {
      setState(() {
        _query = q;
        _candidates = [];
        _selected = null;
      });
      return;
    }
    setState(() {
      _query = q;
      _selected = null;
      _searching = true;
    });
    try {
      final res = await widget.api
          .get('/departments/${widget.deptId}/members/candidates', params: {'q': q.trim()});
      if (mounted) {
        setState(() {
          _candidates = res.data as List<dynamic>? ?? [];
          _searching = false;
        });
      }
    } catch (_) {
      if (mounted) setState(() => _searching = false);
    }
  }

  Future<void> _create() async {
    if (_nomCtrl.text.trim().length < 2 || _saving) return;
    setState(() => _saving = true);
    try {
      await widget.api.post('/departments/${widget.deptId}/members/create', data: {
        'nom': _nomCtrl.text.trim(),
        'prenom': _prenomCtrl.text.trim().isEmpty ? null : _prenomCtrl.text.trim(),
        'email': _emailCtrl.text.trim().isEmpty ? null : _emailCtrl.text.trim(),
        'telephone': _telephoneCtrl.text.trim().isEmpty ? null : _telephoneCtrl.text.trim(),
        'profession': _professionCtrl.text.trim().isEmpty ? null : _professionCtrl.text.trim(),
        'situationFamiliale': _situationCtrl.text.trim().isEmpty ? null : _situationCtrl.text.trim(),
        'typeDisciple': _typeDisciple,
        'statut': _statut,
        'dateIntegration': _dateIntegration.isEmpty ? null : _dateIntegration,
        'dateConversion': _dateConversion.isEmpty ? null : _dateConversion,
      });
      if (mounted) {
        Navigator.pop(context);
        _showDone('Membre créé et ajouté au département');
      }
      widget.onDone();
    } catch (_) {
      if (mounted) {
        setState(() => _saving = false);
        ScaffoldMessenger.of(context).showSnackBar(
            const SnackBar(content: Text('Échec de la création du membre')));
      }
    }
  }

  Future<void> _addExisting() async {
    final selected = _selected;
    if (selected == null || _saving) return;
    setState(() => _saving = true);
    try {
      await widget.api.post('/departments/${widget.deptId}/members',
          data: {'soulId': selected['id']});
      if (mounted) {
        Navigator.pop(context);
        _showDone('Personne ajoutée au département');
      }
      widget.onDone();
    } catch (_) {
      if (mounted) {
        setState(() => _saving = false);
        ScaffoldMessenger.of(context).showSnackBar(
            const SnackBar(content: Text('Échec de l\'ajout')));
      }
    }
  }

  void _showDone(String message) {
    ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(message)));
  }

  @override
  Widget build(BuildContext context) {
    return SafeArea(
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: SingleChildScrollView(
          child: Column(
            mainAxisSize: MainAxisSize.min,
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Row(
                children: [
                  Container(
                    padding: const EdgeInsets.all(10),
                    decoration: BoxDecoration(
                      gradient: const LinearGradient(
                        colors: [Color(0xFFF59E0B), Color(0xFFF97316)],
                        begin: Alignment.topLeft,
                        end: Alignment.bottomRight,
                      ),
                      borderRadius: BorderRadius.circular(12),
                    ),
                    child: const Icon(Icons.person_add_alt_1, color: Colors.white, size: 22),
                  ),
                  const SizedBox(width: 12),
                  const Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text('Ajouter un membre',
                            style: TextStyle(
                                color: Colors.white, fontWeight: FontWeight.w700, fontSize: 16)),
                        Text('Créez un nouveau membre ou rattachez une personne déjà inscrite',
                            style: TextStyle(
                                color: Colors.white38, fontSize: 11)),
                      ],
                    ),
                  ),
                  IconButton(
                    icon: const Icon(Icons.close, color: Colors.white54),
                    onPressed: () => Navigator.pop(context),
                  ),
                ],
              ),
              const SizedBox(height: 16),
              SegmentedButton<bool>(
                segments: const [
                  ButtonSegment(value: true, label: Text('Nouveau membre'), icon: Icon(Icons.person_add)),
                  ButtonSegment(value: false, label: Text('Déjà inscrite'), icon: Icon(Icons.search)),
                ],
                selected: {_modeCreate},
                onSelectionChanged: (s) => setState(() {
                  _modeCreate = s.first;
                  _selected = null;
                }),
              ),
              const SizedBox(height: 16),
              if (_modeCreate)
                _buildCreateForm()
              else
                _buildExistingForm(),
              const SizedBox(height: 8),
            ],
          ),
        ),
      ),
    );
  }

  Widget _buildCreateForm() {
    return Column(
      mainAxisSize: MainAxisSize.min,
      children: [
        TextField(
          controller: _nomCtrl,
          style: const TextStyle(color: Colors.white),
          decoration: const InputDecoration(labelText: 'Nom *'),
        ),
        const SizedBox(height: 12),
        TextField(
          controller: _prenomCtrl,
          style: const TextStyle(color: Colors.white),
          decoration: const InputDecoration(labelText: 'Prénom'),
        ),
        const SizedBox(height: 12),
        TextField(
          controller: _emailCtrl,
          style: const TextStyle(color: Colors.white),
          keyboardType: TextInputType.emailAddress,
          decoration: const InputDecoration(labelText: 'Email'),
        ),
        const SizedBox(height: 12),
        TextField(
          controller: _telephoneCtrl,
          style: const TextStyle(color: Colors.white),
          keyboardType: TextInputType.phone,
          decoration: const InputDecoration(labelText: 'Téléphone'),
        ),
        const SizedBox(height: 12),
        TextField(
          controller: _professionCtrl,
          style: const TextStyle(color: Colors.white),
          decoration: const InputDecoration(labelText: 'Profession'),
        ),
        const SizedBox(height: 12),
        TextField(
          controller: _situationCtrl,
          style: const TextStyle(color: Colors.white),
          decoration: const InputDecoration(labelText: 'Situation familiale'),
        ),
        const SizedBox(height: 12),
        DropdownButtonFormField<String>(
          initialValue: _typeDisciple,
          dropdownColor: AppColors.cardDark,
          style: const TextStyle(color: Colors.white),
          items: const [
            DropdownMenuItem(value: 'NOUVEL_ARRIVANT', child: Text('Nouvel arrivant', style: TextStyle(color: Colors.white))),
            DropdownMenuItem(value: 'NOUVEAU_CONVERTI', child: Text('Nouveau converti', style: TextStyle(color: Colors.white))),
          ],
          onChanged: (v) => setState(() => _typeDisciple = v ?? _typeDisciple),
          decoration: const InputDecoration(labelText: 'Type de disciple'),
        ),
        const SizedBox(height: 12),
        DropdownButtonFormField<String>(
          initialValue: _statut,
          dropdownColor: AppColors.cardDark,
          style: const TextStyle(color: Colors.white),
          items: const [
            DropdownMenuItem(value: 'NOUVEL_ARRIVANT', child: Text('Nouvel arrivant', style: TextStyle(color: Colors.white))),
            DropdownMenuItem(value: 'NOUVEAU_CONVERTI', child: Text('Nouveau converti', style: TextStyle(color: Colors.white))),
            DropdownMenuItem(value: 'EN_INTEGRATION', child: Text('En intégration', style: TextStyle(color: Colors.white))),
            DropdownMenuItem(value: 'ACTIF', child: Text('Actif', style: TextStyle(color: Colors.white))),
            DropdownMenuItem(value: 'EN_VEILLE', child: Text('En veille', style: TextStyle(color: Colors.white))),
            DropdownMenuItem(value: 'DECROCHE', child: Text('Décroché', style: TextStyle(color: Colors.white))),
          ],
          onChanged: (v) => setState(() => _statut = v ?? _statut),
          decoration: const InputDecoration(labelText: 'Statut'),
        ),
        const SizedBox(height: 12),
        TextField(
          style: const TextStyle(color: Colors.white),
          onChanged: (v) => _dateIntegration = v,
          decoration: const InputDecoration(
              labelText: 'Date d\'intégration (AAAA-MM-JJ, optionnel)'),
        ),
        const SizedBox(height: 12),
        TextField(
          style: const TextStyle(color: Colors.white),
          onChanged: (v) => _dateConversion = v,
          decoration: const InputDecoration(
              labelText: 'Date de conversion (AAAA-MM-JJ, optionnel)'),
        ),
        const SizedBox(height: 16),
        SizedBox(
          width: double.infinity,
          child: FilledButton.icon(
            onPressed: _create,
            icon: _saving
                ? const SizedBox(
                    width: 16,
                    height: 16,
                    child: CircularProgressIndicator(strokeWidth: 2, color: Colors.white))
                : const Icon(Icons.check_circle, size: 18),
            label: const Text('Créer et ajouter'),
          ),
        ),
      ],
    );
  }

  Widget _buildExistingForm() {
    return Column(
      mainAxisSize: MainAxisSize.min,
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        TextField(
          onChanged: _search,
          style: const TextStyle(color: Colors.white),
          decoration: InputDecoration(
            labelText: 'Rechercher par nom, téléphone ou email...',
            prefixIcon: const Icon(Icons.search),
            suffixIcon: _searching
                ? const Padding(
                    padding: EdgeInsets.all(12),
                    child: SizedBox(
                        width: 16,
                        height: 16,
                        child: CircularProgressIndicator(strokeWidth: 2)),
                  )
                : null,
          ),
        ),
        const SizedBox(height: 4),
        Text(
          'Saisissez au moins 2 caractères — seules les personnes non encore rattachées à ce département sont proposées.',
          style: TextStyle(color: Colors.white.withValues(alpha: 0.35), fontSize: 10),
        ),
        const SizedBox(height: 8),
        if (_candidates.isNotEmpty)
          ..._candidates.map((c) {
            final m = c as Map<String, dynamic>;
            final isSelected = _selected?['id'] == m['id'];
            return GlassCard(
              margin: const EdgeInsets.only(bottom: 6),
              padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
              borderColor: isSelected ? AppColors.primary.withValues(alpha: 0.5) : null,
              onTap: () => setState(() => _selected = m),
              child: Row(
                children: [
                  GradientAvatar(text: '${m['nomComplet'] ?? '?'}', radius: 16),
                  const SizedBox(width: 10),
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text('${m['nomComplet'] ?? ''}',
                            style: const TextStyle(
                                color: Colors.white, fontWeight: FontWeight.w600, fontSize: 13)),
                        Text(
                          '${m['telephone'] ?? m['email'] ?? '—'} · ${(m['statut'] ?? '').toString().replaceAll('_', ' ').toLowerCase()}',
                          style: TextStyle(
                              color: Colors.white.withValues(alpha: 0.4), fontSize: 11),
                          overflow: TextOverflow.ellipsis,
                        ),
                      ],
                    ),
                  ),
                  if (isSelected)
                    Icon(Icons.check_circle, color: AppColors.primary, size: 18),
                ],
              ),
            );
          }),
        if (!_searching && _query.trim().length >= 2 && _candidates.isEmpty)
          Padding(
            padding: const EdgeInsets.symmetric(vertical: 16),
            child: Center(
              child: Text('Aucune personne trouvée.',
                  style: TextStyle(color: Colors.white.withValues(alpha: 0.5))),
            ),
          ),
        const SizedBox(height: 12),
        SizedBox(
          width: double.infinity,
          child: FilledButton.icon(
            onPressed: _addExisting,
            icon: _saving
                ? const SizedBox(
                    width: 16,
                    height: 16,
                    child: CircularProgressIndicator(strokeWidth: 2, color: Colors.white))
                : const Icon(Icons.person_add_alt_1, size: 18),
            label: const Text('Ajouter au département'),
          ),
        ),
      ],
    );
  }
}
