import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import '../../../data/services/api_service.dart';
import '../../widgets/app_drawer.dart';
import '../../widgets/glass_theme.dart';

const _reportTypes = <String, String>{
  'HEBDOMADAIRE': 'Hebdomadaire',
  'MENSUEL': 'Mensuel',
  'TRIMESTRIEL': 'Trimestriel',
  'ANNUEL': 'Annuel',
  'EVENEMENT': 'Événement',
  'INCIDENT': 'Incident',
  'DISCIPLINE': 'Discipline',
  'ACTIVITE': 'Activité',
  'EFFECTIF': 'Effectif',
  'ASSIDUITE': 'Assiduité',
  'PERFORMANCE': 'Performance',
  'SYNTHESE': 'Synthèse',
};

const _checklistTargets = <String, String>{
  'GENERAL': 'Général',
  'TACHE': 'Tâche',
  'EVENEMENT': 'Événement',
  'EQUIPE': 'Équipe',
  'MEMBRE': 'Membre',
};

const _etatLabels = <String, String>{
  'NEUF': 'Neuf',
  'BON': 'Bon',
  'USAGE': 'Usagé',
  'REPARATION': 'En réparation',
  'HORS_SERVICE': 'Hors service',
};

const _etatColors = <String, Color>{
  'NEUF': Colors.green,
  'BON': Colors.lightBlue,
  'USAGE': Colors.amber,
  'REPARATION': Colors.orange,
  'HORS_SERVICE': Colors.red,
};

const _docTypeLabels = <String, String>{
  'PROCEDURE': 'Procédure',
  'GUIDE': 'Guide',
  'DOCUMENT': 'Document',
  'FORMULAIRE': 'Formulaire',
  'COMPTE_RENDU': 'Compte rendu',
  'RESSOURCE': 'Ressource',
};

/// Outils & rapports du département (backend V55) : synthèses sauvegardées
/// (génération/export CSV), checklists et inventaire matériel.
class DepartmentToolsScreen extends StatefulWidget {
  final String departmentId;
  final ApiService? apiService;

  const DepartmentToolsScreen({super.key, required this.departmentId, this.apiService});

  @override
  State<DepartmentToolsScreen> createState() => _DepartmentToolsScreenState();
}

class _DepartmentToolsScreenState extends State<DepartmentToolsScreen> {
  late final ApiService _apiService = widget.apiService ?? ApiService();
  bool _isLoading = true;

  List<Map<String, dynamic>> _reports = [];
  List<Map<String, dynamic>> _checklists = [];
  List<Map<String, dynamic>> _equipment = [];
  List<Map<String, dynamic>> _documents = [];
  Map<String, dynamic> _settings = {};
  Map<String, String> _memberNames = {};

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
        _apiService.get('/departments/$_deptId/reports/list'),
        _apiService.get('/departments/$_deptId/checklists'),
        _apiService.get('/departments/$_deptId/equipment'),
        _apiService.get('/departments/$_deptId/members', params: {'size': '200'}),
      ]);
      if (!mounted) return;
      final membersData = results[3].data;
      final members = (membersData is Map ? membersData['content'] : membersData)
              as List<dynamic>? ??
          [];
      final names = <String, String>{};
      for (final m in members) {
        final mm = m as Map;
        final id = mm['id']?.toString();
        final nom = mm['nom']?.toString();
        if (id != null && nom != null) names[id] = nom;
      }
      // Documentation et paramètres : chargés séparément (peuvent être désactivés)
      List<Map<String, dynamic>> docs = [];
      Map<String, dynamic> settings = {};
      try {
        docs = _toListOfMaps((await _apiService.get('/departments/$_deptId/documents')).data);
      } catch (_) {}
      try {
        final s = (await _apiService.get('/departments/$_deptId/settings')).data;
        if (s is Map) settings = Map<String, dynamic>.from(s);
      } catch (_) {}
      if (!mounted) return;
      setState(() {
        _reports = _toListOfMaps(results[0].data);
        _checklists = _toListOfMaps(results[1].data);
        _equipment = _toListOfMaps(results[2].data);
        _documents = docs;
        _settings = settings;
        _memberNames = names;
        _isLoading = false;
      });
    } catch (_) {
      if (mounted) setState(() => _isLoading = false);
    }
  }

  Future<void> _reload() async {
    await _loadData();
  }

  List<Map<String, dynamic>> _toListOfMaps(dynamic data) {
    if (data is! List) return [];
    return data.map((e) => e is Map ? Map<String, dynamic>.from(e) : <String, dynamic>{}).toList();
  }

  void _snack(String message, {bool error = false}) {
    ScaffoldMessenger.of(context).showSnackBar(SnackBar(
      content: Text(message),
      backgroundColor: error ? Colors.red.shade800 : null,
    ));
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Outils & rapports'),
        actions: [
          IconButton(icon: const Icon(Icons.refresh), onPressed: _reload),
        ],
      ),
      drawer: const AppDrawer(),
      body: _isLoading
          ? const ShimmerLoading(itemCount: 5)
          : DefaultTabController(
              length: 5,
              child: Column(
                children: [
                  TabBar(
                    isScrollable: true,
                    tabAlignment: TabAlignment.start,
                    labelColor: Colors.white,
                    indicatorColor: AppColors.accent,
                    tabs: const [
                      Tab(icon: Icon(Icons.description, size: 20), text: 'Rapports'),
                      Tab(icon: Icon(Icons.checklist, size: 20), text: 'Checklists'),
                      Tab(icon: Icon(Icons.inventory_2, size: 20), text: 'Inventaire'),
                      Tab(icon: Icon(Icons.menu_book, size: 20), text: 'Documentation'),
                      Tab(icon: Icon(Icons.tune, size: 20), text: 'Paramètres'),
                    ],
                  ),
                  Expanded(
                    child: TabBarView(
                      children: [
                        _buildReportsTab(),
                        _buildChecklistsTab(),
                        _buildInventoryTab(),
                        _buildDocumentsTab(),
                        _buildSettingsTab(),
                      ],
                    ),
                  ),
                ],
              ),
            ),
    );
  }

  // ============================================================
  // RAPPORTS SAUVEGARDÉS
  // ============================================================

  Widget _buildReportsTab() {
    return RefreshIndicator(
      onRefresh: _reload,
      child: ListView(
        padding: const EdgeInsets.all(12),
        children: [
          SectionTitle(
            title: 'Rapports du département (${_reports.length})',
            icon: Icons.description,
            trailing: IconButton(
              icon: const Icon(Icons.add_circle, color: Colors.amber),
              tooltip: 'Générer un rapport',
              onPressed: () => _showCreateReport(),
            ),
          ),
          if (_reports.isEmpty)
            GlassCard(
              padding: const EdgeInsets.all(24),
              child: Text('Aucun rapport sauvegardé',
                  textAlign: TextAlign.center,
                  style: TextStyle(color: Colors.white.withValues(alpha: 0.5))),
            )
          else
            ..._reports.map((r) => _reportCard(r)),
          const SizedBox(height: 80),
        ],
      ),
    );
  }

  Widget _reportCard(Map<String, dynamic> r) {
    final statut = r['statut']?.toString() ?? 'BROUILLON';
    return GlassCard(
      margin: const EdgeInsets.only(bottom: 8),
      padding: const EdgeInsets.all(12),
      borderColor: AppColors.accent.withValues(alpha: 0.3),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Expanded(
                child: Text('${r['titre'] ?? ''}',
                    style: const TextStyle(
                        color: Colors.white, fontWeight: FontWeight.w600, fontSize: 14)),
              ),
              StatusBadge(
                label: statut.replaceAll('_', ' '),
                color: statut == 'SOUMIS' ? Colors.green : Colors.white70,
              ),
            ],
          ),
          const SizedBox(height: 2),
          Text(
            '${_reportTypes[r['type']] ?? r['type'] ?? ''}'
            '${r['periodeDebut'] != null ? ' · ${r['periodeDebut']} → ${r['periodeFin'] ?? ''}' : ''}',
            style: TextStyle(color: Colors.white.withValues(alpha: 0.5), fontSize: 11),
          ),
          const SizedBox(height: 4),
          Text('${r['contenu'] ?? ''}',
              style: TextStyle(color: Colors.white.withValues(alpha: 0.6), fontSize: 12),
              maxLines: 3,
              overflow: TextOverflow.ellipsis),
          const SizedBox(height: 8),
          Row(
            children: [
              Text('${r['createdAt'] ?? ''}',
                  style: TextStyle(color: Colors.white.withValues(alpha: 0.35), fontSize: 10)),
              const Spacer(),
              IconButton(
                icon: Icon(Icons.ios_share,
                    color: Colors.white.withValues(alpha: 0.5), size: 18),
                tooltip: 'Exporter (CSV)',
                onPressed: () => _exportReport(r),
              ),
              IconButton(
                icon: Icon(Icons.visibility_outlined,
                    color: Colors.white.withValues(alpha: 0.5), size: 18),
                tooltip: 'Voir',
                onPressed: () => _viewReport(r),
              ),
              IconButton(
                icon: Icon(Icons.delete_outline,
                    color: Colors.red.withValues(alpha: 0.6), size: 18),
                tooltip: 'Supprimer',
                onPressed: () => _deleteReport(r),
              ),
            ],
          ),
        ],
      ),
    );
  }

  void _viewReport(Map<String, dynamic> r) {
    showDialog(
      context: context,
      builder: (ctx) => AlertDialog(
        backgroundColor: AppColors.cardDark,
        title: Text('${r['titre'] ?? 'Rapport'}', style: const TextStyle(color: Colors.white)),
        content: SingleChildScrollView(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text('${_reportTypes[r['type']] ?? r['type'] ?? ''} · ${r['createdAt'] ?? ''}',
                  style: TextStyle(color: Colors.white.withValues(alpha: 0.5), fontSize: 11)),
              const SizedBox(height: 10),
              Text('${r['contenu'] ?? ''}',
                  style: const TextStyle(color: Colors.white, fontSize: 13, height: 1.4)),
            ],
          ),
        ),
        actions: [
          TextButton(onPressed: () => Navigator.pop(ctx), child: const Text('Fermer')),
        ],
      ),
    );
  }

  Future<void> _exportReport(Map<String, dynamic> r) async {
    try {
      final res = await _apiService.getBytes(
          '/departments/$_deptId/reports/saved/${r['id']}/export');
      final text = String.fromCharCodes(res.data as List<int>);
      if (!mounted) return;
      await showDialog(
        context: context,
        builder: (ctx) => AlertDialog(
          backgroundColor: AppColors.cardDark,
          title: const Text('Export CSV', style: TextStyle(color: Colors.white)),
          content: SizedBox(
            width: double.maxFinite,
            child: SingleChildScrollView(
              child: SelectableText(text,
                  style: const TextStyle(color: Colors.white70, fontSize: 12)),
            ),
          ),
          actions: [
            TextButton(
              onPressed: () async {
                await Clipboard.setData(ClipboardData(text: text));
                if (ctx.mounted) Navigator.pop(ctx);
              },
              child: const Text('Copier'),
            ),
            TextButton(onPressed: () => Navigator.pop(ctx), child: const Text('Fermer')),
          ],
        ),
      );
    } catch (_) {
      _snack('Échec de l\'export', error: true);
    }
  }

  Future<void> _deleteReport(Map<String, dynamic> r) async {
    final ok = await showDialog<bool>(
      context: context,
      builder: (ctx) => AlertDialog(
        backgroundColor: AppColors.cardDark,
        title: const Text('Supprimer le rapport', style: TextStyle(color: Colors.white)),
        content: Text('Supprimer « ${r['titre'] ?? ''} » ?',
            style: TextStyle(color: Colors.white.withValues(alpha: 0.8))),
        actions: [
          TextButton(onPressed: () => Navigator.pop(ctx, false), child: const Text('Annuler')),
          FilledButton(onPressed: () => Navigator.pop(ctx, true), child: const Text('Supprimer')),
        ],
      ),
    );
    if (ok == true) {
      try {
        await _apiService.delete('/departments/$_deptId/reports/saved/${r['id']}');
        await _reload();
      } catch (_) {
        _snack('Échec de la suppression', error: true);
      }
    }
  }

  Future<void> _showCreateReport() async {
    String type = 'HEBDOMADAIRE';
    final titreCtrl = TextEditingController();
    String periodeDebut = '';
    String periodeFin = '';
    String statut = 'SOUMIS';

    await showDialog(
      context: context,
      builder: (ctx) => StatefulBuilder(
        builder: (ctx, setState) => AlertDialog(
          backgroundColor: AppColors.cardDark,
          title: const Text('Générer un rapport', style: TextStyle(color: Colors.white)),
          content: SingleChildScrollView(
            child: Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                DropdownButtonFormField<String>(
                  initialValue: type,
                  dropdownColor: AppColors.cardDark,
                  style: const TextStyle(color: Colors.white),
                  items: _reportTypes.entries.map((e) {
                    return DropdownMenuItem(
                        value: e.key,
                        child: Text(e.value, style: const TextStyle(color: Colors.white)));
                  }).toList(),
                  onChanged: (v) => setState(() => type = v ?? type),
                  decoration: const InputDecoration(labelText: 'Type *'),
                ),
                const SizedBox(height: 12),
                TextField(
                  controller: titreCtrl,
                  style: const TextStyle(color: Colors.white),
                  decoration: const InputDecoration(
                      labelText: 'Titre (optionnel — auto si vide)'),
                ),
                const SizedBox(height: 12),
                TextField(
                  style: const TextStyle(color: Colors.white),
                  onChanged: (v) => periodeDebut = v,
                  decoration: const InputDecoration(
                      labelText: 'Début de période (AAAA-MM-JJ, optionnel)'),
                ),
                const SizedBox(height: 12),
                TextField(
                  style: const TextStyle(color: Colors.white),
                  onChanged: (v) => periodeFin = v,
                  decoration: const InputDecoration(
                      labelText: 'Fin de période (AAAA-MM-JJ, optionnel)'),
                ),
                const SizedBox(height: 12),
                DropdownButtonFormField<String>(
                  initialValue: statut,
                  dropdownColor: AppColors.cardDark,
                  style: const TextStyle(color: Colors.white),
                  items: const [
                    DropdownMenuItem(value: 'SOUMIS', child: Text('Soumis', style: TextStyle(color: Colors.white))),
                    DropdownMenuItem(value: 'BROUILLON', child: Text('Brouillon', style: TextStyle(color: Colors.white))),
                  ],
                  onChanged: (v) => setState(() => statut = v ?? statut),
                  decoration: const InputDecoration(labelText: 'Statut'),
                ),
              ],
            ),
          ),
          actions: [
            TextButton(onPressed: () => Navigator.pop(ctx), child: const Text('Annuler')),
            FilledButton(
              onPressed: () async {
                try {
                  await _apiService.post('/departments/$_deptId/reports/generate', data: {
                    'type': type,
                    'titre': titreCtrl.text.trim().isEmpty ? null : titreCtrl.text.trim(),
                    'periodeDebut': periodeDebut.isEmpty ? null : periodeDebut,
                    'periodeFin': periodeFin.isEmpty ? null : periodeFin,
                    'statut': statut,
                  });
                  if (ctx.mounted) Navigator.pop(ctx);
                  await _reload();
                  if (mounted) _snack('Rapport généré sur les données réelles');
                } catch (_) {
                  if (context.mounted) {
                    // ignore: use_build_context_synchronously
                    ScaffoldMessenger.of(context).showSnackBar(
                        const SnackBar(content: Text('Échec de la génération')));
                  }
                }
              },
              child: const Text('Générer'),
            ),
          ],
        ),
      ),
    );
  }

  // ============================================================
  // CHECKLISTS
  // ============================================================

  Widget _buildChecklistsTab() {
    return RefreshIndicator(
      onRefresh: _reload,
      child: ListView(
        padding: const EdgeInsets.all(12),
        children: [
          SectionTitle(
            title: 'Checklists (${_checklists.length})',
            icon: Icons.checklist,
            trailing: IconButton(
              icon: const Icon(Icons.add_circle, color: Colors.amber),
              tooltip: 'Nouvelle checklist',
              onPressed: () => _showCreateChecklist(),
            ),
          ),
          if (_checklists.isEmpty)
            GlassCard(
              padding: const EdgeInsets.all(24),
              child: Text('Aucune checklist',
                  textAlign: TextAlign.center,
                  style: TextStyle(color: Colors.white.withValues(alpha: 0.5))),
            )
          else
            ..._checklists.map((c) => _checklistCard(c)),
          const SizedBox(height: 80),
        ],
      ),
    );
  }

  Widget _checklistCard(Map<String, dynamic> c) {
    final statut = c['statut']?.toString() ?? 'OUVERTE';
    final terminee = statut == 'TERMINEE';
    final progression = (c['progression'] as num?)?.toDouble() ?? 0;
    final items = (c['items'] as List<dynamic>? ?? [])
        .map((i) => Map<String, dynamic>.from(i as Map))
        .toList();
    return GlassCard(
      margin: const EdgeInsets.only(bottom: 10),
      padding: const EdgeInsets.all(12),
      borderColor: terminee ? Colors.green.withValues(alpha: 0.4) : null,
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Expanded(
                child: Text('${c['titre'] ?? ''}',
                    style: const TextStyle(
                        color: Colors.white, fontWeight: FontWeight.w600, fontSize: 14)),
              ),
              StatusBadge(
                label: statut.replaceAll('_', ' '),
                color: terminee ? Colors.green : Colors.amber,
              ),
              IconButton(
                icon: Icon(Icons.delete_outline,
                    color: Colors.red.withValues(alpha: 0.6), size: 18),
                tooltip: 'Supprimer',
                onPressed: () => _deleteChecklist(c),
              ),
            ],
          ),
          Row(
            children: [
              StatusBadge(
                label: _checklistTargets[c['cibleType']] ?? '${c['cibleType'] ?? ''}',
                color: Colors.lightBlue,
              ),
              const SizedBox(width: 6),
              Text('${progression.round()}%',
                  style: TextStyle(
                      color: terminee ? Colors.green : Colors.white.withValues(alpha: 0.5),
                      fontSize: 11)),
            ],
          ),
          const SizedBox(height: 10),
          ClipRRect(
            borderRadius: BorderRadius.circular(6),
            child: LinearProgressIndicator(
              value: (progression / 100).clamp(0.0, 1.0).toDouble(),
              minHeight: 6,
              backgroundColor: Colors.white.withValues(alpha: 0.08),
              valueColor: AlwaysStoppedAnimation(terminee ? Colors.green : AppColors.accent),
            ),
          ),
          const SizedBox(height: 10),
          ...items.map((item) => _checklistItemRow(c, item)),
          Row(
            children: [
              Expanded(
                child: TextButton.icon(
                  onPressed: () => _addChecklistItem(c),
                  icon: const Icon(Icons.add, size: 16),
                  label: const Text('Ajouter un élément'),
                ),
              ),
            ],
          ),
        ],
      ),
    );
  }

  Widget _checklistItemRow(Map<String, dynamic> c, Map<String, dynamic> item) {
    final fait = item['fait'] == true;
    return Row(
      children: [
        Checkbox(
          value: fait,
          activeColor: Colors.green,
          onChanged: (v) => _toggleChecklistItem(c, item, v ?? false),
        ),
        Expanded(
          child: Text('${item['libelle'] ?? ''}',
              style: TextStyle(
                  color: fait
                      ? Colors.white.withValues(alpha: 0.4)
                      : Colors.white,
                  fontSize: 13,
                  decoration: fait ? TextDecoration.lineThrough : null)),
        ),
        IconButton(
          icon: Icon(Icons.close, color: Colors.white.withValues(alpha: 0.3), size: 16),
          tooltip: 'Retirer',
          onPressed: () => _deleteChecklistItem(c, item),
        ),
      ],
    );
  }

  Future<void> _toggleChecklistItem(
      Map<String, dynamic> c, Map<String, dynamic> item, bool fait) async {
    try {
      await _apiService.put(
          '/departments/$_deptId/checklists/${c['id']}/items/${item['id']}',
          data: {'libelle': item['libelle'], 'fait': fait});
      await _reload();
    } catch (_) {
      _snack('Échec de la mise à jour', error: true);
    }
  }

  Future<void> _deleteChecklistItem(Map<String, dynamic> c, Map<String, dynamic> item) async {
    try {
      await _apiService.delete(
          '/departments/$_deptId/checklists/${c['id']}/items/${item['id']}');
      await _reload();
    } catch (_) {
      _snack('Échec de la suppression', error: true);
    }
  }

  Future<void> _addChecklistItem(Map<String, dynamic> c) async {
    final libelleCtrl = TextEditingController();
    final ok = await showDialog<bool>(
      context: context,
      builder: (ctx) => AlertDialog(
        backgroundColor: AppColors.cardDark,
        title: const Text('Nouvel élément', style: TextStyle(color: Colors.white)),
        content: TextField(
          controller: libelleCtrl,
          style: const TextStyle(color: Colors.white),
          decoration: const InputDecoration(labelText: 'Libellé *'),
        ),
        actions: [
          TextButton(onPressed: () => Navigator.pop(ctx, false), child: const Text('Annuler')),
          FilledButton(onPressed: () => Navigator.pop(ctx, true), child: const Text('Ajouter')),
        ],
      ),
    );
    if (ok == true && libelleCtrl.text.trim().isNotEmpty) {
      try {
        await _apiService.post('/departments/$_deptId/checklists/${c['id']}/items',
            data: {'libelle': libelleCtrl.text.trim()});
        await _reload();
      } catch (_) {
        _snack('Échec de l\'ajout', error: true);
      }
    }
  }

  Future<void> _deleteChecklist(Map<String, dynamic> c) async {
    final ok = await showDialog<bool>(
      context: context,
      builder: (ctx) => AlertDialog(
        backgroundColor: AppColors.cardDark,
        title: const Text('Supprimer la checklist', style: TextStyle(color: Colors.white)),
        content: Text('Supprimer « ${c['titre'] ?? ''} » ?',
            style: TextStyle(color: Colors.white.withValues(alpha: 0.8))),
        actions: [
          TextButton(onPressed: () => Navigator.pop(ctx, false), child: const Text('Annuler')),
          FilledButton(onPressed: () => Navigator.pop(ctx, true), child: const Text('Supprimer')),
        ],
      ),
    );
    if (ok == true) {
      try {
        await _apiService.delete('/departments/$_deptId/checklists/${c['id']}');
        await _reload();
      } catch (_) {
        _snack('Échec de la suppression', error: true);
      }
    }
  }

  Future<void> _showCreateChecklist() async {
    String cibleType = 'GENERAL';
    final titreCtrl = TextEditingController();
    final itemsCtrl = TextEditingController();

    await showDialog(
      context: context,
      builder: (ctx) => StatefulBuilder(
        builder: (ctx, setState) => AlertDialog(
          backgroundColor: AppColors.cardDark,
          title: const Text('Nouvelle checklist', style: TextStyle(color: Colors.white)),
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
                  initialValue: cibleType,
                  dropdownColor: AppColors.cardDark,
                  style: const TextStyle(color: Colors.white),
                  items: _checklistTargets.entries.map((e) {
                    return DropdownMenuItem(
                        value: e.key,
                        child: Text(e.value, style: const TextStyle(color: Colors.white)));
                  }).toList(),
                  onChanged: (v) => setState(() => cibleType = v ?? cibleType),
                  decoration: const InputDecoration(labelText: 'Cible'),
                ),
                const SizedBox(height: 12),
                TextField(
                  controller: itemsCtrl,
                  style: const TextStyle(color: Colors.white),
                  maxLines: 3,
                  decoration: const InputDecoration(
                      labelText: 'Éléments (un par ligne)'),
                ),
              ],
            ),
          ),
          actions: [
            TextButton(onPressed: () => Navigator.pop(ctx), child: const Text('Annuler')),
            FilledButton(
              onPressed: () async {
                if (titreCtrl.text.trim().isEmpty) return;
                final items = itemsCtrl.text
                    .split('\n')
                    .map((l) => l.trim())
                    .where((l) => l.isNotEmpty)
                    .toList();
                try {
                  await _apiService.post('/departments/$_deptId/checklists', data: {
                    'titre': titreCtrl.text.trim(),
                    'cibleType': cibleType,
                    'items': items,
                  });
                  if (ctx.mounted) Navigator.pop(ctx);
                  await _reload();
                  if (mounted) _snack('Checklist créée');
                } catch (_) {
                  if (context.mounted) {
                    // ignore: use_build_context_synchronously
                    ScaffoldMessenger.of(context).showSnackBar(
                        const SnackBar(content: Text('Échec de la création')));
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

  // ============================================================
  // INVENTAIRE MATÉRIEL
  // ============================================================

  Widget _buildInventoryTab() {
    return RefreshIndicator(
      onRefresh: _reload,
      child: ListView(
        padding: const EdgeInsets.all(12),
        children: [
          SectionTitle(
            title: 'Inventaire (${_equipment.length})',
            icon: Icons.inventory_2,
            trailing: IconButton(
              icon: const Icon(Icons.add_circle, color: Colors.amber),
              tooltip: 'Nouvel équipement',
              onPressed: () => _showEditEquipment(null),
            ),
          ),
          if (_equipment.isEmpty)
            GlassCard(
              padding: const EdgeInsets.all(24),
              child: Text('Aucun équipement enregistré',
                  textAlign: TextAlign.center,
                  style: TextStyle(color: Colors.white.withValues(alpha: 0.5))),
            )
          else
            ..._equipment.map((e) => _equipmentCard(e)),
          const SizedBox(height: 80),
        ],
      ),
    );
  }

  Widget _equipmentCard(Map<String, dynamic> e) {
    final etat = e['etat']?.toString() ?? 'BON';
    return GlassCard(
      margin: const EdgeInsets.only(bottom: 8),
      padding: const EdgeInsets.all(12),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Container(
                padding: const EdgeInsets.all(8),
                decoration: BoxDecoration(
                  color: AppColors.primary.withValues(alpha: 0.15),
                  borderRadius: BorderRadius.circular(10),
                ),
                child: Icon(Icons.inventory_2, color: AppColors.primaryLight, size: 20),
              ),
              const SizedBox(width: 10),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text('${e['nom'] ?? ''}',
                        style: const TextStyle(
                            color: Colors.white, fontWeight: FontWeight.w600, fontSize: 14)),
                    Text('Quantité : ${e['quantite'] ?? 1}',
                        style: TextStyle(
                            color: Colors.white.withValues(alpha: 0.4), fontSize: 11)),
                  ],
                ),
              ),
              StatusBadge(
                label: _etatLabels[etat] ?? etat.replaceAll('_', ' '),
                color: _etatColors[etat] ?? Colors.white70,
              ),
            ],
          ),
          if ((e['description'] ?? '').toString().isNotEmpty) ...[
            const SizedBox(height: 6),
            Text('${e['description']}',
                style: TextStyle(color: Colors.white.withValues(alpha: 0.5), fontSize: 12)),
          ],
          if ((e['localisation'] ?? '').toString().isNotEmpty) ...[
            const SizedBox(height: 4),
            Text('📍 ${e['localisation']}',
                style: TextStyle(color: Colors.white.withValues(alpha: 0.4), fontSize: 11)),
          ],
          const SizedBox(height: 4),
          Text(
            'Resp. : ${_memberNames[e['responsableId']?.toString()] ?? '—'}'
            '${e['affecteAId'] != null ? ' · Affecté à : ${_memberNames[e['affecteAId'].toString()] ?? '—'}' : ''}'
            '${e['dateAcquisition'] != null ? ' · acquis le ${e['dateAcquisition']}' : ''}',
            style: TextStyle(color: Colors.white.withValues(alpha: 0.35), fontSize: 10),
          ),
          Row(
            children: [
              const Spacer(),
              IconButton(
                icon: Icon(Icons.edit_outlined,
                    color: Colors.white.withValues(alpha: 0.5), size: 18),
                tooltip: 'Modifier',
                onPressed: () => _showEditEquipment(e),
              ),
              IconButton(
                icon: Icon(Icons.delete_outline,
                    color: Colors.red.withValues(alpha: 0.6), size: 18),
                tooltip: 'Supprimer',
                onPressed: () => _deleteEquipment(e),
              ),
            ],
          ),
        ],
      ),
    );
  }

  Future<void> _deleteEquipment(Map<String, dynamic> e) async {
    final ok = await showDialog<bool>(
      context: context,
      builder: (ctx) => AlertDialog(
        backgroundColor: AppColors.cardDark,
        title: const Text('Supprimer l\'équipement', style: TextStyle(color: Colors.white)),
        content: Text('Supprimer « ${e['nom'] ?? ''} » ?',
            style: TextStyle(color: Colors.white.withValues(alpha: 0.8))),
        actions: [
          TextButton(onPressed: () => Navigator.pop(ctx, false), child: const Text('Annuler')),
          FilledButton(onPressed: () => Navigator.pop(ctx, true), child: const Text('Supprimer')),
        ],
      ),
    );
    if (ok == true) {
      try {
        await _apiService.delete('/departments/$_deptId/equipment/${e['id']}');
        await _reload();
      } catch (_) {
        _snack('Échec de la suppression', error: true);
      }
    }
  }

  Future<void> _showEditEquipment(Map<String, dynamic>? e) async {
    final isEdit = e != null;
    final nomCtrl = TextEditingController(text: e?['nom']?.toString() ?? '');
    final descCtrl = TextEditingController(text: e?['description']?.toString() ?? '');
    final qteCtrl = TextEditingController(text: '${e?['quantite'] ?? 1}');
    final locCtrl = TextEditingController(text: e?['localisation']?.toString() ?? '');
    final dateCtrl = TextEditingController(text: e?['dateAcquisition']?.toString() ?? '');
    String etat = e?['etat']?.toString() ?? 'BON';
    String? responsableId = e?['responsableId']?.toString();
    String? affecteAId = e?['affecteAId']?.toString();

    await showDialog(
      context: context,
      builder: (ctx) => StatefulBuilder(
        builder: (ctx, setState) => AlertDialog(
          backgroundColor: AppColors.cardDark,
          title: Text(isEdit ? 'Modifier l\'équipement' : 'Nouvel équipement',
              style: const TextStyle(color: Colors.white)),
          content: SingleChildScrollView(
            child: Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                TextField(
                  controller: nomCtrl,
                  style: const TextStyle(color: Colors.white),
                  decoration: const InputDecoration(labelText: 'Nom *'),
                ),
                const SizedBox(height: 12),
                TextField(
                  controller: descCtrl,
                  style: const TextStyle(color: Colors.white),
                  decoration: const InputDecoration(labelText: 'Description'),
                ),
                const SizedBox(height: 12),
                TextField(
                  controller: qteCtrl,
                  style: const TextStyle(color: Colors.white),
                  keyboardType: TextInputType.number,
                  decoration: const InputDecoration(labelText: 'Quantité'),
                ),
                const SizedBox(height: 12),
                DropdownButtonFormField<String>(
                  initialValue: etat,
                  dropdownColor: AppColors.cardDark,
                  style: const TextStyle(color: Colors.white),
                  items: _etatLabels.entries.map((entry) {
                    return DropdownMenuItem(
                        value: entry.key,
                        child: Text(entry.value, style: const TextStyle(color: Colors.white)));
                  }).toList(),
                  onChanged: (v) => setState(() => etat = v ?? etat),
                  decoration: const InputDecoration(labelText: 'État'),
                ),
                if (_memberNames.isNotEmpty) ...[
                  const SizedBox(height: 12),
                  DropdownButtonFormField<String?>(
                    initialValue: responsableId,
                    dropdownColor: AppColors.cardDark,
                    style: const TextStyle(color: Colors.white),
                    items: [
                      const DropdownMenuItem<String?>(
                          value: null,
                          child: Text('— Aucun —', style: TextStyle(color: Colors.white))),
                      ..._memberNames.entries.map((entry) => DropdownMenuItem<String?>(
                            value: entry.key,
                            child: Text(entry.value,
                                style: const TextStyle(color: Colors.white)),
                          )),
                    ],
                    onChanged: (v) => setState(() => responsableId = v),
                    decoration: const InputDecoration(labelText: 'Responsable'),
                  ),
                  const SizedBox(height: 12),
                  DropdownButtonFormField<String?>(
                    initialValue: affecteAId,
                    dropdownColor: AppColors.cardDark,
                    style: const TextStyle(color: Colors.white),
                    items: [
                      const DropdownMenuItem<String?>(
                          value: null,
                          child: Text('— Aucun —', style: TextStyle(color: Colors.white))),
                      ..._memberNames.entries.map((entry) => DropdownMenuItem<String?>(
                            value: entry.key,
                            child: Text(entry.value,
                                style: const TextStyle(color: Colors.white)),
                          )),
                    ],
                    onChanged: (v) => setState(() => affecteAId = v),
                    decoration: const InputDecoration(labelText: 'Affecté à'),
                  ),
                ],
                const SizedBox(height: 12),
                TextField(
                  controller: locCtrl,
                  style: const TextStyle(color: Colors.white),
                  decoration: const InputDecoration(labelText: 'Localisation'),
                ),
                const SizedBox(height: 12),
                TextField(
                  controller: dateCtrl,
                  style: const TextStyle(color: Colors.white),
                  decoration: const InputDecoration(
                      labelText: 'Date d\'acquisition (AAAA-MM-JJ)'),
                ),
              ],
            ),
          ),
          actions: [
            TextButton(onPressed: () => Navigator.pop(ctx), child: const Text('Annuler')),
            FilledButton(
              onPressed: () async {
                if (nomCtrl.text.trim().isEmpty) return;
                final data = {
                  'nom': nomCtrl.text.trim(),
                  'description': descCtrl.text.trim().isEmpty ? null : descCtrl.text.trim(),
                  'quantite': int.tryParse(qteCtrl.text.trim()) ?? 1,
                  'etat': etat,
                  'responsableId': responsableId,
                  'affecteAId': affecteAId,
                  'localisation': locCtrl.text.trim().isEmpty ? null : locCtrl.text.trim(),
                  'dateAcquisition': dateCtrl.text.trim().isEmpty ? null : dateCtrl.text.trim(),
                };
                try {
                  if (isEdit) {
                    await _apiService.put(
                        '/departments/$_deptId/equipment/${e['id']}',
                        data: data);
                  } else {
                    await _apiService.post('/departments/$_deptId/equipment', data: data);
                  }
                  if (ctx.mounted) Navigator.pop(ctx);
                  await _reload();
                } catch (_) {
                  if (context.mounted) {
                    // ignore: use_build_context_synchronously
                    ScaffoldMessenger.of(context).showSnackBar(
                        const SnackBar(content: Text('Échec de l\'enregistrement')));
                  }
                }
              },                  child: Text(isEdit ? 'Enregistrer' : 'Créer'),
            ),
          ],
        ),
      ),
    );
  }

  // ============================================================
  // DOCUMENTATION DU DÉPARTEMENT
  // ============================================================

  Widget _buildDocumentsTab() {
    return RefreshIndicator(
      onRefresh: _reload,
      child: ListView(
        padding: const EdgeInsets.all(12),
        children: [
          SectionTitle(
            title: 'Documentation (${_documents.length})',
            icon: Icons.menu_book,
            trailing: IconButton(
              icon: const Icon(Icons.add_circle, color: Colors.amber),
              tooltip: 'Ajouter un document',
              onPressed: _showAddDocument,
            ),
          ),
          if (_documents.isEmpty)
            GlassCard(
              padding: const EdgeInsets.all(24),
              child: Text('Aucun document — procédures, guides, formulaires…',
                  textAlign: TextAlign.center,
                  style: TextStyle(color: Colors.white.withValues(alpha: 0.5))),
            )
          else
            ..._documents.map((d) => _documentCard(d)),
          const SizedBox(height: 80),
        ],
      ),
    );
  }

  Widget _documentCard(Map<String, dynamic> d) {
    final type = d['type']?.toString() ?? 'DOCUMENT';
    return GlassCard(
      margin: const EdgeInsets.only(bottom: 8),
      padding: const EdgeInsets.all(12),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Container(
                padding: const EdgeInsets.all(8),
                decoration: BoxDecoration(
                  color: AppColors.primary.withValues(alpha: 0.15),
                  borderRadius: BorderRadius.circular(10),
                ),
                child: Icon(Icons.menu_book, color: AppColors.primaryLight, size: 20),
              ),
              const SizedBox(width: 10),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text('${d['titre'] ?? ''}',
                        style: const TextStyle(
                            color: Colors.white, fontWeight: FontWeight.w600, fontSize: 14)),
                    Text(_docTypeLabels[type] ?? type.replaceAll('_', ' '),
                        style: TextStyle(
                            color: Colors.white.withValues(alpha: 0.4), fontSize: 11)),
                  ],
                ),
              ),
              IconButton(
                icon: Icon(Icons.delete_outline,
                    color: Colors.red.withValues(alpha: 0.6), size: 18),
                tooltip: 'Supprimer',
                onPressed: () => _deleteDocument(d),
              ),
            ],
          ),
          if ((d['description'] ?? '').toString().isNotEmpty) ...[
            const SizedBox(height: 6),
            Text('${d['description']}',
                style: TextStyle(color: Colors.white.withValues(alpha: 0.5), fontSize: 12)),
          ],
          if ((d['url'] ?? '').toString().isNotEmpty) ...[
            const SizedBox(height: 4),
            Text('🔗 ${d['url']}',
                style: TextStyle(color: AppColors.accent, fontSize: 11)),
          ],
        ],
      ),
    );
  }

  Future<void> _showAddDocument() async {
    final titreCtrl = TextEditingController();
    final descCtrl = TextEditingController();
    final urlCtrl = TextEditingController();
    var type = 'DOCUMENT';
    await showDialog(
      context: context,
      builder: (ctx) => StatefulBuilder(
        builder: (ctx, setState) => AlertDialog(
          backgroundColor: AppColors.cardDark,
          title: const Text('Nouveau document', style: TextStyle(color: Colors.white)),
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
                  items: _docTypeLabels.entries.map((e) {
                    return DropdownMenuItem(
                        value: e.key,
                        child: Text(e.value, style: const TextStyle(color: Colors.white)));
                  }).toList(),
                  onChanged: (v) => setState(() => type = v ?? type),
                  decoration: const InputDecoration(labelText: 'Type'),
                ),
                const SizedBox(height: 12),
                TextField(
                  controller: descCtrl,
                  style: const TextStyle(color: Colors.white),
                  maxLines: 2,
                  decoration: const InputDecoration(labelText: 'Description'),
                ),
                const SizedBox(height: 12),
                TextField(
                  controller: urlCtrl,
                  style: const TextStyle(color: Colors.white),
                  decoration: const InputDecoration(labelText: 'Lien du document (optionnel)'),
                ),
              ],
            ),
          ),
          actions: [
            TextButton(onPressed: () => Navigator.pop(ctx), child: const Text('Annuler')),
            FilledButton(
              onPressed: () async {
                if (titreCtrl.text.trim().isEmpty) return;
                try {
                  await _apiService.post('/departments/$_deptId/documents', data: {
                    'titre': titreCtrl.text.trim(),
                    'type': type,
                    'description': descCtrl.text.trim().isEmpty ? null : descCtrl.text.trim(),
                    'url': urlCtrl.text.trim().isEmpty ? null : urlCtrl.text.trim(),
                  });
                  if (ctx.mounted) Navigator.pop(ctx);
                  await _reload();
                  if (mounted) _snack('Document ajouté');
                } catch (_) {
                  if (context.mounted) {
                    // ignore: use_build_context_synchronously
                    ScaffoldMessenger.of(context).showSnackBar(
                        const SnackBar(content: Text('Échec de l\'ajout')));
                  }
                }
              },
              child: const Text('Ajouter'),
            ),
          ],
        ),
      ),
    );
  }

  Future<void> _deleteDocument(Map<String, dynamic> d) async {
    final ok = await showDialog<bool>(
      context: context,
      builder: (ctx) => AlertDialog(
        backgroundColor: AppColors.cardDark,
        title: const Text('Supprimer le document', style: TextStyle(color: Colors.white)),
        content: Text('Supprimer « ${d['titre'] ?? ''} » ?',
            style: TextStyle(color: Colors.white.withValues(alpha: 0.8))),
        actions: [
          TextButton(onPressed: () => Navigator.pop(ctx, false), child: const Text('Annuler')),
          FilledButton(onPressed: () => Navigator.pop(ctx, true), child: const Text('Supprimer')),
        ],
      ),
    );
    if (ok == true) {
      try {
        await _apiService.delete('/departments/$_deptId/documents/${d['id']}');
        await _reload();
        if (mounted) _snack('Document supprimé');
      } catch (_) {
        if (mounted) _snack('Échec de la suppression', error: true);
      }
    }
  }

  // ============================================================
  // PARAMÈTRES (seuils des alertes intelligentes)
  // ============================================================

  Widget _buildSettingsTab() {
    final seuilCtrl = TextEditingController(text: (_settings['absenceSeuil'] ?? 2).toString());
    final periodeCtrl =
        TextEditingController(text: (_settings['absencePeriode'] ?? 3).toString());
    final inactifCtrl =
        TextEditingController(text: (_settings['inactiviteMois'] ?? 3).toString());
    final rappelEventCtrl =
        TextEditingController(text: (_settings['eventRappelJours'] ?? 1).toString());
    return RefreshIndicator(
      onRefresh: _reload,
      child: ListView(
        padding: const EdgeInsets.all(12),
        children: [
          SectionTitle(title: 'Paramètres des alertes', icon: Icons.tune),
          GlassCard(
            padding: const EdgeInsets.all(16),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  'Les règles d\'alerte lisent ces seuils — aucune valeur codée en dur.',
                  style: TextStyle(color: Colors.white.withValues(alpha: 0.5), fontSize: 12),
                ),
                const SizedBox(height: 16),
                TextField(
                  controller: seuilCtrl,
                  style: const TextStyle(color: Colors.white),
                  keyboardType: TextInputType.number,
                  decoration: const InputDecoration(
                      labelText: 'Absences requises (1–10)', helperText: 'Alerte absences répétées'),
                ),
                const SizedBox(height: 12),
                TextField(
                  controller: periodeCtrl,
                  style: const TextStyle(color: Colors.white),
                  keyboardType: TextInputType.number,
                  decoration: const InputDecoration(
                      labelText: 'Période en semaines (1–12)', helperText: 'Fenêtre de détection'),
                ),
                const SizedBox(height: 12),
                TextField(
                  controller: inactifCtrl,
                  style: const TextStyle(color: Colors.white),
                  keyboardType: TextInputType.number,
                  decoration: const InputDecoration(
                      labelText: 'Mois sans présence (0–24)', helperText: '0 = désactivé'),
                ),
                const SizedBox(height: 12),
                TextField(
                  controller: rappelEventCtrl,
                  style: const TextStyle(color: Colors.white),
                  keyboardType: TextInputType.number,
                  decoration: const InputDecoration(
                      labelText: 'Rappel événement (0–30 jours)',
                      helperText: 'Notifie le responsable N jours avant (0 = désactivé)'),
                ),
                const SizedBox(height: 16),
                FilledButton.icon(
                  onPressed: () => _saveSettings(
                      seuilCtrl.text, periodeCtrl.text, inactifCtrl.text, rappelEventCtrl.text),
                  icon: const Icon(Icons.save, size: 18),
                  label: const Text('Enregistrer les paramètres'),
                ),
              ],
            ),
          ),
          const SizedBox(height: 80),
        ],
      ),
    );
  }

  Future<void> _saveSettings(String seuil, String periode, String inactif, String rappelEvent) async {
    final absenceSeuil = int.tryParse(seuil.trim());
    final absencePeriode = int.tryParse(periode.trim());
    final inactiviteMois = int.tryParse(inactif.trim());
    final eventRappelJours = int.tryParse(rappelEvent.trim());
    if (absenceSeuil == null || absencePeriode == null || inactiviteMois == null
        || eventRappelJours == null) {
      _snack('Valeurs invalides', error: true);
      return;
    }
    try {
      await _apiService.put('/departments/$_deptId/settings', data: {
        'absenceSeuil': absenceSeuil,
        'absencePeriode': absencePeriode,
        'inactiviteMois': inactiviteMois,
        'eventRappelJours': eventRappelJours,
      });
      await _reload();
      if (mounted) _snack('Paramètres enregistrés');
    } catch (_) {
      if (mounted) _snack('Échec de l\'enregistrement', error: true);
    }
  }
}
