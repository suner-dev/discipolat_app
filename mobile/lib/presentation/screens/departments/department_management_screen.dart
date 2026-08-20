import 'dart:async';
import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import '../../widgets/glass_theme.dart';
import '../../widgets/app_drawer.dart';
import '../../../data/services/api_service.dart';

/// Department Management System — écran mobile de gestion du département :
/// Organisation (sous-départements/équipes récursifs), Tâches (charge de
/// travail), Affectations membres → équipes/postes et Journal d'activité.
class DepartmentManagementScreen extends StatefulWidget {
  final String departmentId;
  final ApiService? apiService;

  const DepartmentManagementScreen({super.key, required this.departmentId, this.apiService});

  @override
  State<DepartmentManagementScreen> createState() => _DepartmentManagementScreenState();
}

class _DepartmentManagementScreenState extends State<DepartmentManagementScreen> {
  late final ApiService _apiService = widget.apiService ?? ApiService();
  final _searchCtrl = TextEditingController();
  Timer? _searchDebounce;
  Map<String, dynamic>? _overview;
  bool _isLoading = true;
  List<dynamic> _members = [];
  Map<String, dynamic> _searchResults = const {};
  bool _searching = false;
  String _searchQuery = '';

  @override
  void initState() {
    super.initState();
    _loadData();
  }

  @override
  void dispose() {
    _searchDebounce?.cancel();
    _searchCtrl.dispose();
    super.dispose();
  }

  Future<void> _loadData() async {
    setState(() => _isLoading = true);
    try {
      final res = await _apiService.get('/departments/${widget.departmentId}/management');
      final membersRes = await _apiService.get('/departments/${widget.departmentId}/members',
          params: {'size': '200'});
      if (mounted) {
        setState(() {
          _overview = res.data as Map<String, dynamic>?;
          _members = (membersRes.data is Map ? membersRes.data['content'] : membersRes.data)
                  as List<dynamic>? ??
              [];
          _isLoading = false;
        });
      }
    } catch (_) {
      if (mounted) setState(() => _isLoading = false);
    }
  }

  /// Recherche globale dans le département (membres, équipes, postes, tâches,
  /// événements) — déclenchée à partir de 2 caractères, données réelles.
  Future<void> _runSearch(String value) async {
    final q = value.trim();
    if (q.length < 2) {
      if (mounted) {
        setState(() {
          _searchResults = const {};
          _searchQuery = '';
          _searching = false;
        });
      }
      return;
    }
    if (mounted) {
      setState(() {
        _searchQuery = q;
        _searching = true;
      });
    }
    try {
      final res = await _apiService.get('/departments/${widget.departmentId}/search',
          params: {'q': q});
      if (mounted && _searchCtrl.text.trim() == q) {
        setState(() {
          _searchResults = (res.data is Map ? res.data : <String, dynamic>{})
              as Map<String, dynamic>;
          _searching = false;
        });
      }
    } catch (_) {
      if (mounted) setState(() => _searching = false);
    }
  }

  Future<void> _reload() async {
    await _loadData();
  }

  String get _deptId => widget.departmentId;

  @override
  Widget build(BuildContext context) {
    final overview = _overview ?? {};
    final org = overview['org'] as Map<String, dynamic>? ?? {};

    return Scaffold(
      appBar: AppBar(
        title: const Text('Gestion du département'),
        actions: [
          IconButton(
            icon: const Icon(Icons.bar_chart),
            tooltip: 'Statistiques',
            onPressed: () => context.go('/departments/${widget.departmentId}/stats'),
          ),
          IconButton(
            icon: const Icon(Icons.inventory_2),
            tooltip: 'Rapports · Checklists · Inventaire',
            onPressed: () => context.go('/departments/${widget.departmentId}/tools'),
          ),
          IconButton(icon: const Icon(Icons.refresh), onPressed: _reload),
        ],
      ),
      drawer: const AppDrawer(),
      body: _isLoading
          ? const ShimmerLoading(itemCount: 5)
          : DefaultTabController(
              length: 6,
              child: Column(
                children: [
                  Padding(
                    padding: const EdgeInsets.fromLTRB(12, 4, 12, 0),
                    child: TextField(
                      controller: _searchCtrl,
                      style: const TextStyle(color: Colors.white),
                      onChanged: (v) {
                        _searchDebounce?.cancel();
                        _searchDebounce = Timer(const Duration(milliseconds: 400), () => _runSearch(v));
                      },
                      decoration: InputDecoration(
                        hintText: 'Recherche rapide : membre, équipe, tâche…',
                        hintStyle: TextStyle(color: Colors.white.withValues(alpha: 0.4), fontSize: 13),
                        prefixIcon: Icon(Icons.search, color: Colors.white.withValues(alpha: 0.4)),
                        isDense: true,
                        filled: true,
                        fillColor: Colors.white.withValues(alpha: 0.08),
                        border: OutlineInputBorder(
                          borderRadius: BorderRadius.circular(12),
                          borderSide: BorderSide.none,
                        ),
                      ),
                    ),
                  ),
                  if (_searchQuery.isNotEmpty)
                    _SearchResultsPanel(
                      query: _searchQuery,
                      results: _searchResults,
                      searching: _searching,
                      deptId: _deptId,
                      onClear: () {
                        _searchCtrl.clear();
                        setState(() {
                          _searchResults = const {};
                          _searchQuery = '';
                          _searching = false;
                        });
                      },
                    )
                  else ...[
                    Padding(
                      padding: const EdgeInsets.symmetric(horizontal: 12),
                      child: Row(
                        children: [
                          Expanded(
                            child: GlassCard(
                              padding: const EdgeInsets.all(10),
                              child: Column(
                                children: [
                                  Text('${org['equipesActives'] ?? 0}',
                                      style: const TextStyle(color: Colors.amber, fontSize: 18, fontWeight: FontWeight.bold)),
                                  Text('Équipes', style: TextStyle(color: Colors.white.withValues(alpha: 0.5), fontSize: 10)),
                                ],
                              ),
                            ),
                          ),
                          const SizedBox(width: 8),
                          Expanded(
                            child: GlassCard(
                              padding: const EdgeInsets.all(10),
                              child: Column(
                                children: [
                                  Text('${org['postesActifs'] ?? 0}',
                                      style: const TextStyle(color: Colors.blue, fontSize: 18, fontWeight: FontWeight.bold)),
                                  Text('Postes', style: TextStyle(color: Colors.white.withValues(alpha: 0.5), fontSize: 10)),
                                ],
                              ),
                            ),
                          ),
                          const SizedBox(width: 8),
                          Expanded(
                            child: GlassCard(
                              padding: const EdgeInsets.all(10),
                              child: Column(
                                children: [
                                  Text('${org['membresAffectes'] ?? 0}',
                                      style: const TextStyle(color: Colors.green, fontSize: 18, fontWeight: FontWeight.bold)),
                                  Text('Affectés', style: TextStyle(color: Colors.white.withValues(alpha: 0.5), fontSize: 10)),
                                ],
                              ),
                            ),
                          ),
                        ],
                      ),
                    ),
                    TabBar(
                      isScrollable: true,
                      tabAlignment: TabAlignment.start,
                      labelColor: Colors.white,
                      indicatorColor: AppColors.accent,
                      tabs: const [
                        Tab(icon: Icon(Icons.group, size: 20), text: 'Membres'),
                        Tab(icon: Icon(Icons.account_tree, size: 20), text: 'Organisation'),
                        Tab(icon: Icon(Icons.checklist, size: 20), text: 'Tâches'),
                        Tab(icon: Icon(Icons.group_add, size: 20), text: 'Affectations'),
                        Tab(icon: Icon(Icons.event, size: 20), text: 'Événements'),
                        Tab(icon: Icon(Icons.history, size: 20), text: 'Activité'),
                      ],
                    ),
                    Expanded(
                      child: TabBarView(
                        children: [
                          _MembersTab(deptId: _deptId, members: _members),
                          _OrganisationTab(overview: overview, deptId: _deptId, onChanged: _reload),
                          _TasksTab(overview: overview, members: _members, deptId: _deptId, onChanged: _reload),
                          _AssignmentsTab(overview: overview, members: _members, deptId: _deptId, onChanged: _reload),
                          _EventsTab(deptId: _deptId, apiService: _apiService, onChanged: _reload),
                          _ActivityTab(activity: overview['activity'] as List<dynamic>? ?? []),
                        ],
                      ),
                    ),
                  ],
                ],
              ),
            ),
    );
  }
}

// ============================================================
// RECHERCHE GLOBALE — résultats par catégorie (données réelles)
// ============================================================

class _SearchResultsPanel extends StatelessWidget {
  final String query;
  final Map<String, dynamic> results;
  final bool searching;
  final String deptId;
  final VoidCallback onClear;

  const _SearchResultsPanel({
    required this.query,
    required this.results,
    required this.searching,
    required this.deptId,
    required this.onClear,
  });

  @override
  Widget build(BuildContext context) {
    if (searching) {
      return const Padding(
        padding: EdgeInsets.all(24),
        child: Center(child: CircularProgressIndicator()),
      );
    }
    final sections = <(String, String, IconData, Color, List<dynamic>)>[
      ('membres', 'Membres', Icons.person, Colors.green, results['membres'] as List<dynamic>? ?? []),
      ('equipes', 'Équipes', Icons.account_tree, Colors.amber, results['equipes'] as List<dynamic>? ?? []),
      ('postes', 'Postes', Icons.work, Colors.blue, results['postes'] as List<dynamic>? ?? []),
      ('taches', 'Tâches', Icons.checklist, Colors.purple, results['taches'] as List<dynamic>? ?? []),
      ('evenements', 'Événements', Icons.event, Colors.orange, results['evenements'] as List<dynamic>? ?? []),
    ];
    final total = results['total'] as int? ?? 0;
    return Expanded(
      child: ListView(
        padding: const EdgeInsets.all(12),
        children: [
          Row(
            children: [
              Expanded(
                child: Text(
                  total > 0 ? '$total résultat(s) pour « $query »' : 'Aucun résultat pour « $query »',
                  style: const TextStyle(color: Colors.white, fontWeight: FontWeight.w600, fontSize: 13),
                ),
              ),
              IconButton(
                icon: Icon(Icons.close, color: Colors.white.withValues(alpha: 0.5), size: 18),
                onPressed: onClear,
              ),
            ],
          ),
          if (total == 0)
            GlassCard(
              padding: const EdgeInsets.all(24),
              child: Text('Essayez un nom, un poste, une équipe, une tâche ou un événement.',
                  textAlign: TextAlign.center,
                  style: TextStyle(color: Colors.white.withValues(alpha: 0.5))),
            )
          else
            ...sections.where((s) => s.$5.isNotEmpty).map((s) {
              final (_, label, icon, color, items) = s;
              return Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Padding(
                    padding: const EdgeInsets.only(top: 8, bottom: 4),
                    child: Text('$label (${items.length})',
                        style: TextStyle(
                            color: Colors.white.withValues(alpha: 0.5),
                            fontSize: 11,
                            fontWeight: FontWeight.w600,
                            letterSpacing: 0.5)),
                  ),
                  ...items.map((r) {
                    final item = r as Map<String, dynamic>;
                    final label = '${item['titre'] ?? item['nom'] ?? item['nomComplet'] ?? ''}';
                    return GlassCard(
                      margin: const EdgeInsets.only(bottom: 6),
                      padding: const EdgeInsets.all(12),
                      onTap: item['id'] != null
                          ? () => context.go('/departments/$deptId/members/${item['id']}')
                          : null,
                      child: Row(
                        children: [
                          Icon(icon, color: color, size: 18),
                          const SizedBox(width: 10),
                          Expanded(
                            child: Text(label, style: const TextStyle(color: Colors.white, fontSize: 13)),
                          ),
                          Icon(Icons.chevron_right, color: Colors.white.withValues(alpha: 0.3)),
                        ],
                      ),
                    );
                  }),
                ],
              );
            }),
          const SizedBox(height: 40),
        ],
      ),
    );
  }
}

// ============================================================
// MEMBRES — liste des membres → dossier individuel
// ============================================================

class _MembersTab extends StatelessWidget {
  final String deptId;
  final List<dynamic> members;

  const _MembersTab({required this.deptId, required this.members});

  @override
  Widget build(BuildContext context) {
    final list = members.map((m) => m as Map<String, dynamic>).toList();

    return RefreshIndicator(
      onRefresh: () async {},
      child: ListView(
        padding: const EdgeInsets.all(12),
        children: [
          SectionTitle(
            title: 'Membres du département (${list.length})',
            icon: Icons.group,
          ),
          if (list.isEmpty)
            GlassCard(
              padding: const EdgeInsets.all(24),
              child: Text('Aucun membre dans ce département',
                  textAlign: TextAlign.center,
                  style: TextStyle(color: Colors.white.withValues(alpha: 0.5))),
            )
          else
            ...list.map((m) => GlassCard(
                  margin: const EdgeInsets.only(bottom: 8),
                  padding: const EdgeInsets.all(12),
                  onTap: () =>
                      context.go('/departments/$deptId/members/${m['id']}'),
                  child: Row(
                    children: [
                      GradientAvatar(text: '${m['nom'] ?? '?'}', radius: 20),
                      const SizedBox(width: 10),
                      Expanded(
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Text('${m['nom'] ?? ''}',
                                style: const TextStyle(
                                    color: Colors.white,
                                    fontWeight: FontWeight.w600,
                                    fontSize: 14)),
                            if (m['familleNom'] != null)
                              Text('Famille : ${m['familleNom']}',
                                  style: TextStyle(
                                      color: Colors.white.withValues(alpha: 0.5),
                                      fontSize: 11)),
                            if (m['faiseurNom'] != null)
                              Text('Faiseur : ${m['faiseurNom']}',
                                  style: TextStyle(
                                      color: Colors.white.withValues(alpha: 0.4),
                                      fontSize: 11)),
                          ],
                        ),
                      ),
                      StatusBadge(
                        label: '${m['statut'] ?? 'ACTIF'}'.replaceAll('_', ' '),
                        color: m['statut'] == 'DECROCHE'
                            ? Colors.grey
                            : Colors.green,
                      ),
                      Icon(Icons.chevron_right,
                          color: Colors.white.withValues(alpha: 0.3)),
                    ],
                  ),
                )),
          const SizedBox(height: 80),
        ],
      ),
    );
  }
}

// ============================================================
// ORGANISATION — arbre des équipes
// ============================================================

class _OrganisationTab extends StatelessWidget {
  final Map<String, dynamic> overview;
  final String deptId;
  final VoidCallback onChanged;

  const _OrganisationTab({required this.overview, required this.deptId, required this.onChanged});

  @override
  Widget build(BuildContext context) {
    final teams = (overview['teams'] as List<dynamic>? ?? [])
        .map((t) => t as Map<String, dynamic>)
        .toList();
    final roots = teams.where((t) => t['parentId'] == null).toList();

    return RefreshIndicator(
      onRefresh: () async => onChanged(),
      child: ListView(
        padding: const EdgeInsets.all(12),
        children: [
          SectionTitle(
            title: 'Organigramme',
            icon: Icons.account_tree,
            trailing: IconButton(
              icon: const Icon(Icons.add_circle, color: Colors.amber),
              onPressed: () => _showCreateTeam(context, teams),
            ),
          ),
          if (roots.isEmpty)
            GlassCard(
              padding: const EdgeInsets.all(28),
              child: Column(
                children: [
                  Icon(Icons.account_tree, size: 40, color: Colors.white.withValues(alpha: 0.3)),
                  const SizedBox(height: 8),
                  Text('Aucune équipe — créez votre premier sous-département',
                      style: TextStyle(color: Colors.white.withValues(alpha: 0.5))),
                ],
              ),
            )
          else
            ...roots.map((t) => _teamNode(context, t, teams, 0)),
        ],
      ),
    );
  }

  Widget _teamNode(BuildContext context, Map<String, dynamic> team, List<Map<String, dynamic>> teams, int depth) {
    final type = team['type'] ?? 'EQUIPE_PERMANENTE';
    final typeLabel = switch (type) {
      'SOUS_DEPARTEMENT' => 'Sous-département',
      'EQUIPE_TEMPORAIRE' => 'Temporaire',
      _ => 'Équipe permanente',
    };
    final typeColor = switch (type) {
      'SOUS_DEPARTEMENT' => Colors.lightBlue,
      'EQUIPE_TEMPORAIRE' => Colors.orange,
      _ => Colors.green,
    };

    return GlassCard(
      margin: EdgeInsets.only(bottom: 8, left: depth * 20.0),
      padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 10),
      child: Row(
        children: [
          Icon(Icons.folder, color: depth == 0 ? AppColors.accent : Colors.white38, size: 20),
          const SizedBox(width: 8),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text('${team['nom'] ?? ''}',
                    style: const TextStyle(color: Colors.white, fontWeight: FontWeight.w600, fontSize: 14)),
                Row(
                  children: [
                    StatusBadge(label: typeLabel, color: typeColor),
                    const SizedBox(width: 6),
                    Text('${team['nbMembres'] ?? 0} membres',
                        style: TextStyle(color: Colors.white.withValues(alpha: 0.4), fontSize: 11)),
                  ],
                ),
                if (team['eventTitre'] != null) ...[
                  const SizedBox(height: 3),
                  Row(
                    children: [
                      Icon(Icons.event, size: 12, color: Colors.orange.withValues(alpha: 0.8)),
                      const SizedBox(width: 4),
                      Expanded(
                        child: Text('Événement : ${team['eventTitre']}',
                            maxLines: 1,
                            overflow: TextOverflow.ellipsis,
                            style: TextStyle(
                                color: Colors.orange.withValues(alpha: 0.8),
                                fontSize: 11)),
                      ),
                    ],
                  ),
                ],
              ],
            ),
          ),
          Icon(Icons.chevron_right, color: Colors.white.withValues(alpha: 0.3)),
        ],
      ),
    );
  }

  Future<void> _showCreateTeam(BuildContext context, List<Map<String, dynamic>> teams) async {
    final nomCtrl = TextEditingController();
    final dateDebutCtrl = TextEditingController();
    final dateFinCtrl = TextEditingController();
    String type = 'EQUIPE_PERMANENTE';
    String? parentId;
    String objectif = '';
    String? eventId;
    List<Map<String, dynamic>> deptEvents = [];
    bool eventsLoading = false;

    await showDialog(
      context: context,
      builder: (ctx) => StatefulBuilder(
        builder: (ctx, setState) {
          Future<void> loadEvents() async {
            if (deptEvents.isNotEmpty || eventsLoading) return;
            setState(() => eventsLoading = true);
            try {
              final res = await ApiService().get('/events/department/$deptId', params: {'size': '100'});
              if (ctx.mounted) {
                setState(() {
                  deptEvents = ((res.data is Map ? res.data['content'] : res.data) as List<dynamic>? ?? [])
                      .map((e) => e as Map<String, dynamic>)
                      .toList();
                  eventsLoading = false;
                });
              }
            } catch (_) {
              if (ctx.mounted) setState(() => eventsLoading = false);
            }
          }

          return AlertDialog(
            backgroundColor: AppColors.cardDark,
            title: const Text('Nouvelle équipe', style: TextStyle(color: Colors.white)),
            content: SingleChildScrollView(
              child: Column(
                mainAxisSize: MainAxisSize.min,
                children: [
                  TextField(
                    controller: nomCtrl,
                    style: const TextStyle(color: Colors.white),
                    decoration: const InputDecoration(labelText: "Nom de l'équipe"),
                  ),
                  const SizedBox(height: 12),
                  DropdownButtonFormField<String>(
                    initialValue: type,
                    dropdownColor: AppColors.cardDark,
                    style: const TextStyle(color: Colors.white),
                    items: const [
                      DropdownMenuItem(value: 'SOUS_DEPARTEMENT', child: Text('Sous-département', style: TextStyle(color: Colors.white))),
                      DropdownMenuItem(value: 'EQUIPE_PERMANENTE', child: Text('Équipe permanente', style: TextStyle(color: Colors.white))),
                      DropdownMenuItem(value: 'EQUIPE_TEMPORAIRE', child: Text('Équipe temporaire', style: TextStyle(color: Colors.white))),
                    ],
                    onChanged: (v) {
                      setState(() => type = v ?? type);
                      if (v == 'EQUIPE_TEMPORAIRE') loadEvents();
                    },
                    decoration: const InputDecoration(labelText: 'Type'),
                  ),
                  if (teams.isNotEmpty) ...[
                    const SizedBox(height: 12),
                    DropdownButtonFormField<String?>(
                      initialValue: parentId,
                      dropdownColor: AppColors.cardDark,
                      style: const TextStyle(color: Colors.white),
                      items: [
                        const DropdownMenuItem<String?>(value: null, child: Text('— Aucune (racine) —', style: TextStyle(color: Colors.white))),
                        ...teams.map((t) => DropdownMenuItem<String?>(
                              value: t['id'] as String?,
                              child: Text('${t['nom']}', style: const TextStyle(color: Colors.white)),
                            )),
                      ],
                      onChanged: (v) => setState(() => parentId = v),
                      decoration: const InputDecoration(labelText: 'Équipe parente'),
                    ),
                  ],
                  if (type == 'EQUIPE_TEMPORAIRE') ...[
                    const SizedBox(height: 12),
                    DropdownButtonFormField<String?>(
                      initialValue: eventId,
                      dropdownColor: AppColors.cardDark,
                      style: const TextStyle(color: Colors.white),
                      items: [
                        const DropdownMenuItem<String?>(value: null, child: Text('— Aucun événement —', style: TextStyle(color: Colors.white))),
                        ...deptEvents.map((e) => DropdownMenuItem<String?>(
                              value: e['id'] as String?,
                              child: Text(e['titre'] as String? ?? '',
                                  maxLines: 1,
                                  overflow: TextOverflow.ellipsis,
                                  style: const TextStyle(color: Colors.white)),
                            )),
                      ],
                      onChanged: (v) {
                        setState(() {
                          eventId = v;
                          final ev = deptEvents.where((e) => e['id'] == v).firstOrNull;
                          if (ev != null) {
                            if (dateDebutCtrl.text.isEmpty && ev['dateDebut'] != null) {
                              dateDebutCtrl.text = ev['dateDebut'].toString().substring(0, 10);
                            }
                            if (dateFinCtrl.text.isEmpty && ev['dateFin'] != null) {
                              dateFinCtrl.text = ev['dateFin'].toString().substring(0, 10);
                            }
                          }
                        });
                      },
                      decoration: InputDecoration(
                        labelText: 'Événement lié (optionnel)',
                        helperText: eventsLoading ? 'Chargement des événements…' : null,
                      ),
                    ),
                    const SizedBox(height: 12),
                    TextField(
                      controller: dateDebutCtrl,
                      style: const TextStyle(color: Colors.white),
                      decoration: const InputDecoration(labelText: 'Date début (AAAA-MM-JJ)'),
                    ),
                    const SizedBox(height: 12),
                    TextField(
                      controller: dateFinCtrl,
                      style: const TextStyle(color: Colors.white),
                      decoration: const InputDecoration(labelText: 'Date fin (AAAA-MM-JJ)'),
                    ),
                  ],
                  const SizedBox(height: 12),
                  TextField(
                    style: const TextStyle(color: Colors.white),
                    onChanged: (v) => objectif = v,
                    decoration: const InputDecoration(labelText: 'Objectif (optionnel)'),
                  ),
                ],
              ),
            ),
            actions: [
              TextButton(onPressed: () => Navigator.pop(ctx), child: const Text('Annuler')),
              FilledButton(
                onPressed: () async {
                  final api = ApiService();
                  try {
                    await api.post('/departments/$deptId/teams', data: {
                      'nom': nomCtrl.text.trim(),
                      'type': type,
                      'parentId': parentId,
                      'objectif': objectif.isEmpty ? null : objectif,
                      'eventId': type == 'EQUIPE_TEMPORAIRE' ? eventId : null,
                      'dateDebut': dateDebutCtrl.text.trim().isEmpty ? null : dateDebutCtrl.text.trim(),
                      'dateFin': dateFinCtrl.text.trim().isEmpty ? null : dateFinCtrl.text.trim(),
                    });
                    if (ctx.mounted) Navigator.pop(ctx);
                    onChanged();
                  } catch (_) {
                    if (ctx.mounted) {
                      ScaffoldMessenger.of(ctx).showSnackBar(
                          const SnackBar(content: Text('Échec de la création de l\'équipe')));
                    }
                  }
                },
                child: const Text('Créer'),
              ),
            ],
          );
        },
      ),
    );
  }
}

// ============================================================
// TÂCHES
// ============================================================

class _TasksTab extends StatelessWidget {
  final Map<String, dynamic> overview;
  final List<dynamic> members;
  final String deptId;
  final VoidCallback onChanged;

  const _TasksTab({required this.overview, required this.members, required this.deptId, required this.onChanged});

  @override
  Widget build(BuildContext context) {
    final stats = overview['taskStats'] as Map<String, dynamic>? ?? {};
    return RefreshIndicator(
      onRefresh: () async => onChanged(),
      child: ListView(
        padding: const EdgeInsets.all(12),
        children: [
          Row(
            children: [
              _statChip('En cours', '${stats['enCours'] ?? 0}', Colors.blue),
              const SizedBox(width: 8),
              _statChip('À faire', '${stats['aFaire'] ?? 0}', Colors.white70),
              const SizedBox(width: 8),
              _statChip('En retard', '${stats['enRetard'] ?? 0}', Colors.red),
              const SizedBox(width: 8),
              _statChip('Terminées', '${(stats['terminees'] ?? 0) + (stats['validees'] ?? 0)}', Colors.green),
            ],
          ),
          const SizedBox(height: 8),
          SectionTitle(
            title: 'Tâches du département',
            icon: Icons.checklist,
            trailing: IconButton(
              icon: const Icon(Icons.add_circle, color: Colors.amber),
              onPressed: () => _showCreateTask(context),
            ),
          ),
          FutureBuilder(
            future: ApiService().get('/departments/$deptId/tasks'),
            builder: (context, snapshot) {
              if (snapshot.connectionState != ConnectionState.done) {
                return const ShimmerLoading(itemCount: 3);
              }
              if (snapshot.hasError) {
                return Text('Erreur de chargement', style: TextStyle(color: Colors.white.withValues(alpha: 0.5)));
              }
              final tasks = (snapshot.data?.data as List<dynamic>? ?? [])
                  .map((t) => t as Map<String, dynamic>)
                  .toList();
              if (tasks.isEmpty) {
                return GlassCard(
                  padding: const EdgeInsets.all(24),
                  child: Text('Aucune tâche', textAlign: TextAlign.center,
                      style: TextStyle(color: Colors.white.withValues(alpha: 0.5))),
                );
              }
              return Column(
                children: tasks.map((t) => _taskCard(context, t)).toList(),
              );
            },
          ),
          const SizedBox(height: 80),
        ],
      ),
    );
  }

  Widget _statChip(String label, String value, Color color) {
    return Expanded(
      child: GlassCard(
        padding: const EdgeInsets.all(10),
        child: Column(
          children: [
            Text(value, style: TextStyle(color: color, fontSize: 16, fontWeight: FontWeight.bold)),
            Text(label, style: TextStyle(color: Colors.white.withValues(alpha: 0.5), fontSize: 9)),
          ],
        ),
      ),
    );
  }

  Widget _taskCard(BuildContext context, Map<String, dynamic> t) {
    final prioriteColor = switch (t['priorite']) {
      'HAUTE' => Colors.red,
      'BASSE' => Colors.white38,
      _ => Colors.amber,
    };
    final statutColor = switch (t['statut']) {
      'TERMINEE' || 'VALIDEE' => Colors.green,
      'BLOQUEE' => Colors.red,
      'EN_COURS' => Colors.lightBlue,
      'ANNULEE' => Colors.grey,
      _ => Colors.white70,
    };
    final enRetard = t['enRetard'] == true;

    return GlassCard(
      margin: const EdgeInsets.only(bottom: 8),
      padding: const EdgeInsets.all(12),
      borderColor: enRetard ? Colors.red.withValues(alpha: 0.4) : null,
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Icon(Icons.flag, color: prioriteColor, size: 18),
              const SizedBox(width: 8),
              Expanded(
                child: Text('${t['titre'] ?? ''}',
                    style: const TextStyle(color: Colors.white, fontWeight: FontWeight.w600, fontSize: 14)),
              ),
              StatusBadge(label: '${t['statut'] ?? ''}'.replaceAll('_', ' ').toLowerCase(), color: statutColor),
            ],
          ),
          const SizedBox(height: 6),
          Row(
            children: [
              Icon(Icons.person_outline, size: 13, color: Colors.white.withValues(alpha: 0.4)),
              const SizedBox(width: 4),
              Text('${t['assigneeNom'] ?? 'Non assignée'}',
                  style: TextStyle(color: Colors.white.withValues(alpha: 0.5), fontSize: 11)),
              if (t['echeance'] != null) ...[
                const SizedBox(width: 10),
                Icon(Icons.event, size: 13, color: enRetard ? Colors.red : Colors.white.withValues(alpha: 0.4)),
                const SizedBox(width: 4),
                Text('${t['echeance']}', style: TextStyle(color: enRetard ? Colors.red : Colors.white.withValues(alpha: 0.5), fontSize: 11)),
              ],
            ],
          ),
        ],
      ),
    );
  }

  Future<void> _showCreateTask(BuildContext context) async {
    final titreCtrl = TextEditingController();
    String priorite = 'MOYENNE';
    String? assignedTo;
    String echeance = '';

    await showDialog(
      context: context,
      builder: (ctx) => StatefulBuilder(
        builder: (ctx, setState) => AlertDialog(
          backgroundColor: AppColors.cardDark,
          title: const Text('Nouvelle tâche', style: TextStyle(color: Colors.white)),
          content: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              TextField(
                controller: titreCtrl,
                style: const TextStyle(color: Colors.white),
                decoration: const InputDecoration(labelText: 'Titre'),
              ),
              const SizedBox(height: 12),
              DropdownButtonFormField<String?>(
                initialValue: assignedTo,
                dropdownColor: AppColors.cardDark,
                style: const TextStyle(color: Colors.white),
                items: [
                  const DropdownMenuItem<String?>(value: null, child: Text('— Non assignée —', style: TextStyle(color: Colors.white))),
                  ...members.map((m) => DropdownMenuItem<String?>(
                        value: (m as Map)['id'] as String?,
                        child: Text('${m['nom'] ?? ''}', style: const TextStyle(color: Colors.white)),
                      )),
                ],
                onChanged: (v) => setState(() => assignedTo = v),
                decoration: const InputDecoration(labelText: 'Assignée à'),
              ),
              const SizedBox(height: 12),
              DropdownButtonFormField<String>(
                initialValue: priorite,
                dropdownColor: AppColors.cardDark,
                style: const TextStyle(color: Colors.white),
                items: const [
                  DropdownMenuItem(value: 'BASSE', child: Text('Basse', style: TextStyle(color: Colors.white))),
                  DropdownMenuItem(value: 'MOYENNE', child: Text('Moyenne', style: TextStyle(color: Colors.white))),
                  DropdownMenuItem(value: 'HAUTE', child: Text('Haute', style: TextStyle(color: Colors.white))),
                ],
                onChanged: (v) => setState(() => priorite = v ?? priorite),
                decoration: const InputDecoration(labelText: 'Priorité'),
              ),
              const SizedBox(height: 12),
              TextField(
                style: const TextStyle(color: Colors.white),
                onChanged: (v) => echeance = v,
                decoration: const InputDecoration(labelText: 'Échéance (AAAA-MM-JJ, optionnel)'),
              ),
            ],
          ),
          actions: [
            TextButton(onPressed: () => Navigator.pop(ctx), child: const Text('Annuler')),
            FilledButton(
              onPressed: () async {
                final api = ApiService();
                try {
                  await api.post('/departments/$deptId/tasks', data: {
                    'titre': titreCtrl.text.trim(),
                    'assignedTo': assignedTo,
                    'priorite': priorite,
                    'echeance': echeance.isEmpty ? null : echeance,
                  });
                  if (ctx.mounted) Navigator.pop(ctx);
                  onChanged();
                } catch (_) {
                  if (ctx.mounted) {
                    ScaffoldMessenger.of(ctx).showSnackBar(
                        const SnackBar(content: Text('Échec de la création de la tâche')));
                  }
                }
              },
              child: const Text('Créer'),
            ),
          ],
        ),
      ),
    );
  }
}

// ============================================================
// AFFECTATIONS
// ============================================================

class _AssignmentsTab extends StatelessWidget {
  final Map<String, dynamic> overview;
  final List<dynamic> members;
  final String deptId;
  final VoidCallback onChanged;

  const _AssignmentsTab({required this.overview, required this.members, required this.deptId, required this.onChanged});

  @override
  Widget build(BuildContext context) {
    final assignments = (overview['assignments'] as List<dynamic>? ?? [])
        .map((a) => a as Map<String, dynamic>)
        .where((a) => a['actif'] == true)
        .toList();
    final teams = (overview['teams'] as List<dynamic>? ?? [])
        .map((t) => t as Map<String, dynamic>)
        .toList();

    return RefreshIndicator(
      onRefresh: () async => onChanged(),
      child: ListView(
        padding: const EdgeInsets.all(12),
        children: [
          SectionTitle(
            title: 'Affectations actives',
            icon: Icons.group_add,
            trailing: IconButton(
              icon: const Icon(Icons.add_circle, color: Colors.amber),
              onPressed: () => _showAssign(context, teams),
            ),
          ),
          if (assignments.isEmpty)
            GlassCard(
              padding: const EdgeInsets.all(24),
              child: Text('Aucune affectation active', textAlign: TextAlign.center,
                  style: TextStyle(color: Colors.white.withValues(alpha: 0.5))),
            )
          else
            ...assignments.map((a) => GlassCard(
                  margin: const EdgeInsets.only(bottom: 8),
                  padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 10),
                  child: Row(
                    children: [
                      GradientAvatar(text: '${a['memberNom'] ?? '?'}', radius: 18),
                      const SizedBox(width: 10),
                      Expanded(
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Text('${a['memberNom'] ?? ''}',
                                style: const TextStyle(color: Colors.white, fontWeight: FontWeight.w600, fontSize: 13)),
                            Text(
                              '${a['teamNom'] ?? 'Sans équipe'}${a['positionNom'] != null ? ' · ${a['positionNom']}' : ''} · ${a['role'] ?? 'MEMBRE'}',
                              style: TextStyle(color: Colors.white.withValues(alpha: 0.5), fontSize: 11),
                            ),
                          ],
                        ),
                      ),
                      IconButton(
                        icon: Icon(Icons.close, color: Colors.red.withValues(alpha: 0.7), size: 18),
                        tooltip: 'Mettre fin',
                        onPressed: () async {
                          final api = ApiService();
                          try {
                            await api.delete('/departments/$deptId/assignments/${a['id']}');
                            onChanged();
                          } catch (_) {}
                        },
                      ),
                    ],
                  ),
                )),
          const SizedBox(height: 80),
        ],
      ),
    );
  }

  Future<void> _showAssign(BuildContext context, List<Map<String, dynamic>> teams) async {
    String? memberId;
    String? teamId;
    String role = 'MEMBRE';

    await showDialog(
      context: context,
      builder: (ctx) => StatefulBuilder(
        builder: (ctx, setState) => AlertDialog(
          backgroundColor: AppColors.cardDark,
          title: const Text('Affecter un membre', style: TextStyle(color: Colors.white)),
          content: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              DropdownButtonFormField<String?>(
                initialValue: memberId,
                dropdownColor: AppColors.cardDark,
                style: const TextStyle(color: Colors.white),
                items: [
                  ...members.map((m) => DropdownMenuItem<String?>(
                        value: (m as Map)['id'] as String?,
                        child: Text('${m['nom'] ?? ''}', style: const TextStyle(color: Colors.white)),
                      )),
                ],
                onChanged: (v) => setState(() => memberId = v),
                decoration: const InputDecoration(labelText: 'Membre'),
              ),
              const SizedBox(height: 12),
              DropdownButtonFormField<String?>(
                initialValue: teamId,
                dropdownColor: AppColors.cardDark,
                style: const TextStyle(color: Colors.white),
                items: [
                  ...teams
                      .where((t) => t['statut'] == 'ACTIVE')
                      .map((t) => DropdownMenuItem<String?>(
                            value: t['id'] as String?,
                            child: Text('${t['nom'] ?? ''}', style: const TextStyle(color: Colors.white)),
                          )),
                ],
                onChanged: (v) => setState(() => teamId = v),
                decoration: const InputDecoration(labelText: 'Équipe'),
              ),
              const SizedBox(height: 12),
              DropdownButtonFormField<String>(
                initialValue: role,
                dropdownColor: AppColors.cardDark,
                style: const TextStyle(color: Colors.white),
                items: const [
                  DropdownMenuItem(value: 'CHEF', child: Text('Chef', style: TextStyle(color: Colors.white))),
                  DropdownMenuItem(value: 'ADJOINT', child: Text('Adjoint', style: TextStyle(color: Colors.white))),
                  DropdownMenuItem(value: 'MEMBRE', child: Text('Membre', style: TextStyle(color: Colors.white))),
                ],
                onChanged: (v) => setState(() => role = v ?? role),
                decoration: const InputDecoration(labelText: 'Rôle'),
              ),
            ],
          ),
          actions: [
            TextButton(onPressed: () => Navigator.pop(ctx), child: const Text('Annuler')),
            FilledButton(
              onPressed: () async {
                if (memberId == null || teamId == null) return;
                final api = ApiService();
                try {
                  await api.post('/departments/$deptId/assignments', data: {
                    'memberId': memberId,
                    'teamId': teamId,
                    'role': role,
                  });
                  if (ctx.mounted) Navigator.pop(ctx);
                  onChanged();
                } catch (_) {
                  if (ctx.mounted) {
                    ScaffoldMessenger.of(ctx).showSnackBar(
                        const SnackBar(content: Text('Échec de l\'affectation')));
                  }
                }
              },
              child: const Text('Affecter'),
            ),
          ],
        ),
      ),
    );
  }
}

// ============================================================
// ÉVÉNEMENTS DU DÉPARTEMENT — liste + création (departmentId)
// ============================================================

class _EventsTab extends StatelessWidget {
  final String deptId;
  final ApiService apiService;
  final VoidCallback onChanged;

  const _EventsTab({required this.deptId, required this.apiService, required this.onChanged});

  static const _types = ['REUNION', 'SORTIE', 'RETRAITE', 'EVANGELISATION', 'VISITE', 'CONFERENCE', 'FORMATION', 'AUTRE'];
  static const _typeLabels = {
    'REUNION': 'Réunion', 'SORTIE': 'Sortie', 'RETRAITE': 'Retraite',
    'EVANGELISATION': 'Évangélisation', 'VISITE': 'Visite', 'CONFERENCE': 'Conférence',
    'FORMATION': 'Formation', 'ANNIVERSAIRE': 'Anniversaire', 'AUTRE': 'Autre',
  };

  @override
  Widget build(BuildContext context) {
    return FutureBuilder(
      future: apiService.get('/events/department/$deptId', params: {'size': '100'}),
      builder: (context, snapshot) {
        if (snapshot.connectionState != ConnectionState.done) {
          return const ShimmerLoading(itemCount: 3);
        }
        final events = (snapshot.hasError
                ? <dynamic>[]
                : (snapshot.data?.data is Map ? snapshot.data?.data['content'] : snapshot.data?.data) as List<dynamic>? ??
                    [])
            .map((e) => e as Map<String, dynamic>)
            .toList();
        final upcoming = events
            .where((e) => e['statut'] == 'PLANIFIE' || e['statut'] == 'EN_COURS')
            .toList()
          ..sort((a, b) => '${a['dateDebut']}'.compareTo('${b['dateDebut']}'));
        final past = events
            .where((e) => e['statut'] == 'TERMINE' || e['statut'] == 'ANNULE')
            .toList()
          ..sort((a, b) => '${b['dateDebut']}'.compareTo('${a['dateDebut']}'));

        return RefreshIndicator(
          onRefresh: () async => onChanged(),
          child: ListView(
            padding: const EdgeInsets.all(12),
            children: [
              SectionTitle(
                title: 'Événements du département',
                icon: Icons.event,
                trailing: IconButton(
                  icon: const Icon(Icons.add_circle, color: Colors.amber),
                  onPressed: () => _showCreateEvent(context),
                ),
              ),
              Text('À venir (${upcoming.length})',
                  style: TextStyle(
                      color: Colors.white.withValues(alpha: 0.6),
                      fontSize: 12,
                      fontWeight: FontWeight.w600)),
              const SizedBox(height: 6),
              if (upcoming.isEmpty)
                GlassCard(
                  padding: const EdgeInsets.all(20),
                  child: Text('Aucun événement planifié. Créez le premier événement de votre département.',
                      textAlign: TextAlign.center,
                      style: TextStyle(color: Colors.white.withValues(alpha: 0.5))),
                )
              else
                ...upcoming.map((e) => _eventCard(context, e)),
              if (past.isNotEmpty) ...[
                const SizedBox(height: 14),
                Text('Passés (${past.length})',
                    style: TextStyle(
                        color: Colors.white.withValues(alpha: 0.6),
                        fontSize: 12,
                        fontWeight: FontWeight.w600)),
                const SizedBox(height: 6),
                ...past.map((e) => _eventCard(context, e)),
              ],
              const SizedBox(height: 80),
            ],
          ),
        );
      },
    );
  }

  Widget _eventCard(BuildContext context, Map<String, dynamic> e) {
    final type = '${e['typeEvenement'] ?? 'AUTRE'}';
    final statut = '${e['statut'] ?? 'PLANIFIE'}';
    final statutColor = statut == 'TERMINE'
        ? Colors.green
        : statut == 'ANNULE'
            ? Colors.grey
            : statut == 'EN_COURS'
                ? Colors.amber
                : Colors.blue;    final dateStr = e['dateDebut']?.toString().substring(0, 16).replaceAll('T', ' ') ?? '';
    return GlassCard(
      margin: const EdgeInsets.only(bottom: 8),
      padding: const EdgeInsets.all(12),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Icon(Icons.event, color: Colors.orange, size: 18),
              const SizedBox(width: 8),
              Expanded(
                child: Text('${e['titre'] ?? ''}',
                    style: const TextStyle(color: Colors.white, fontWeight: FontWeight.w600, fontSize: 14)),
              ),
              StatusBadge(label: statut, color: statutColor),
            ],
          ),
          const SizedBox(height: 6),
          Wrap(
            spacing: 12,
            runSpacing: 4,
            children: [
              _meta(Icons.label, _typeLabels[type] ?? type),

              if (dateStr.isNotEmpty) _meta(Icons.access_time, dateStr),
              if (e['lieu'] != null && e['lieu'].toString().isNotEmpty)
                _meta(Icons.location_on, e['lieu'] as String),
              if (e['description'] != null && e['description'].toString().isNotEmpty)
                _meta(Icons.notes, '${e['description']}', expanded: true),
            ],
          ),
        ],
      ),
    );
  }

  Widget _meta(IconData icon, String text, {bool expanded = false}) {
    return Row(
      mainAxisSize: expanded ? MainAxisSize.max : MainAxisSize.min,
      children: [
        Icon(icon, size: 13, color: Colors.white.withValues(alpha: 0.4)),
        const SizedBox(width: 4),
        Flexible(
          child: Text(text,
              maxLines: 1,
              overflow: TextOverflow.ellipsis,
              style: TextStyle(color: Colors.white.withValues(alpha: 0.5), fontSize: 11)),
        ),
      ],
    );
  }

  Future<void> _showCreateEvent(BuildContext context) async {
    final titreCtrl = TextEditingController();
    final descCtrl = TextEditingController();
    final lieuCtrl = TextEditingController();
    String type = 'REUNION';
    DateTime dateDebut = DateTime.now().add(const Duration(days: 7));

    Future<void> pickDate() async {
      final picked = await showDatePicker(
        context: context,
        initialDate: dateDebut,
        firstDate: DateTime.now().subtract(const Duration(days: 30)),
        lastDate: DateTime.now().add(const Duration(days: 365)),
        builder: (context, child) => Theme(data: ThemeData.dark(), child: child!),
      );
      if (picked != null) {
        dateDebut = DateTime(picked.year, picked.month, picked.day, dateDebut.hour, dateDebut.minute);
      }
    }

    await showDialog(
      context: context,
      builder: (ctx) => StatefulBuilder(
        builder: (ctx, setState) => AlertDialog(
          backgroundColor: AppColors.cardDark,
          title: const Text('Nouvel événement du département', style: TextStyle(color: Colors.white)),
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
                DropdownButtonFormField<String>(
                  initialValue: type,
                  dropdownColor: AppColors.cardDark,
                  style: const TextStyle(color: Colors.white),
                  items: _types
                      .map((t) => DropdownMenuItem(
                            value: t,
                            child: Text(_typeLabels[t] ?? t, style: const TextStyle(color: Colors.white)),
                          ))
                      .toList(),
                  onChanged: (v) => setState(() => type = v ?? type),
                  decoration: const InputDecoration(labelText: 'Type'),
                ),
                const SizedBox(height: 12),
                ListTile(
                  contentPadding: EdgeInsets.zero,
                  leading: const Icon(Icons.calendar_today, color: Colors.amber),
                  title: Text(
                    dateDebut.toString().substring(0, 16).replaceAll('T', ' '),
                    style: const TextStyle(color: Colors.white),
                  ),
                  trailing: const Text('Modifier', style: TextStyle(color: Colors.amber)),
                  onTap: () async {
                    await pickDate();
                    if (ctx.mounted) setState(() {});
                  },
                ),
                TextField(
                  controller: lieuCtrl,
                  style: const TextStyle(color: Colors.white),
                  decoration: const InputDecoration(labelText: 'Lieu (optionnel)'),
                ),
                const SizedBox(height: 12),
                TextField(
                  controller: descCtrl,
                  style: const TextStyle(color: Colors.white),
                  maxLines: 2,
                  decoration: const InputDecoration(labelText: 'Description (optionnelle)'),
                ),
              ],
            ),
          ),
          actions: [
            TextButton(onPressed: () => Navigator.pop(ctx), child: const Text('Annuler')),
            FilledButton(
              onPressed: () async {
                if (titreCtrl.text.trim().isEmpty) {
                  ScaffoldMessenger.of(ctx).showSnackBar(
                      const SnackBar(content: Text('Le titre est requis')));
                  return;
                }
                final api = apiService;
                try {
                  await api.post('/events', data: {
                    'typeEvenement': type,
                    'titre': titreCtrl.text.trim(),
                    'description': descCtrl.text.trim().isEmpty ? null : descCtrl.text.trim(),
                    'lieu': lieuCtrl.text.trim().isEmpty ? null : lieuCtrl.text.trim(),
                    'dateDebut': dateDebut.toIso8601String(),
                    'departmentId': deptId,
                  });
                  if (ctx.mounted) Navigator.pop(ctx);
                  onChanged();
                } catch (_) {
                  if (ctx.mounted) {
                    ScaffoldMessenger.of(ctx).showSnackBar(
                        const SnackBar(content: Text('Échec de la création de l\'événement')));
                  }
                }
              },
              child: const Text('Créer'),
            ),
          ],
        ),
      ),
    );
  }
}

// ============================================================
// ACTIVITÉ
// ============================================================

class _ActivityTab extends StatelessWidget {
  final List<dynamic> activity;

  const _ActivityTab({required this.activity});

  @override
  Widget build(BuildContext context) {
    if (activity.isEmpty) {
      return Center(
        child: Text('Aucune activité pour l\'instant',
            style: TextStyle(color: Colors.white.withValues(alpha: 0.5))),
      );
    }
    return ListView.builder(
      padding: const EdgeInsets.all(12),
      itemCount: activity.length,
      itemBuilder: (context, index) {
        final a = activity[index] as Map<String, dynamic>;
        final action = (a['action'] ?? '').toString().replaceAll('_', ' ').toLowerCase();
        return GlassCard(
          margin: const EdgeInsets.only(bottom: 8),
          padding: const EdgeInsets.all(12),
          child: Row(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Container(
                width: 20,
                height: 20,
                decoration: BoxDecoration(
                  gradient: LinearGradient(colors: [AppColors.accent, AppColors.accentLight]),
                  shape: BoxShape.circle,
                ),
                child: const Icon(Icons.check, size: 12, color: Colors.white),
              ),
              const SizedBox(width: 10),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(action,
                        style: const TextStyle(color: Colors.white, fontWeight: FontWeight.w600, fontSize: 13)),
                    if (a['details'] != null)
                      Text('${a['details']}', style: TextStyle(color: Colors.white.withValues(alpha: 0.5), fontSize: 12)),
                    const SizedBox(height: 2),
                    Text(
                      '${a['actorNom'] ?? ''}${(a['actorNom'] ?? '').toString().isNotEmpty ? ' · ' : ''}${a['createdAt'] ?? ''}',
                      style: TextStyle(color: Colors.white.withValues(alpha: 0.3), fontSize: 10),
                    ),
                  ],
                ),
              ),
            ],
          ),
        );
      },
    );
  }
}
