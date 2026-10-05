import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../../data/services/api_service.dart';
import '../../../data/models/organization_v3_models.dart';
import 'modules_screen.dart' show moduleLabelStatic;

/// T-M11 — Fiche campus/nœud (SPEC_ORGANISATION_MODULABLE_V3 §7.1).
///
/// Écran « terrain » du berger : compteurs agrégés du sous-arbre (E, lecture
/// seule serveur, sans PII — D7), enfants avec drill-down, modules du nœud
/// (D, activables si admin du nœud) et intitulés effectifs (B).
///
/// Décision T-M0 : AUCUNE écriture hors-ligne de données V3 — les mutations
/// (modules) passent par l'API en ligne uniquement ; les agrégats ne sont
/// jamais mis en file dans `sync_service` / `offline_sync_manager`.
class NodeDetailScreen extends ConsumerStatefulWidget {
  const NodeDetailScreen({
    super.key,
    required this.nodeId,
    required this.nodeName,
    this.levelName,
    this.responsibleName,
    this.apiService,
  });

  final String nodeId;
  final String nodeName;
  final String? levelName;
  final String? responsibleName;
  final ApiService? apiService;

  @override
  ConsumerState<NodeDetailScreen> createState() => _NodeDetailScreenState();
}

class _NodeDetailScreenState extends ConsumerState<NodeDetailScreen> {
  late final ApiService _api;

  NodeAggregate? _aggregate;
  List<Map<String, dynamic>> _progression = const [];
  List<Map<String, dynamic>> _children = const [];
  List<Map<String, dynamic>> _features = const [];
  bool _loading = true;
  String? _error;

  @override
  void initState() {
    super.initState();
    _api = widget.apiService ?? ApiService();
    _load();
  }

  Future<void> _load() async {
    setState(() {
      _loading = true;
      _error = null;
    });
    try {
      final results = await Future.wait([
        _api.get('/tenant/organization/nodes/${widget.nodeId}/aggregate'),
        _api.get('/tenant/organization/nodes/${widget.nodeId}/children'),
        _api.get('/tenant/organization/nodes/${widget.nodeId}/features'),
      ]);
      // aggregate : Map avec progression ; children/features : List.
      final agg = results[0].data is Map
          ? Map<String, dynamic>.from(results[0].data as Map)
          : const <String, dynamic>{};
      NodeAggregate parsed;
      try {
        parsed = NodeAggregate.fromJson(agg);
      } catch (_) {
        parsed = NodeAggregate(
            nodeId: widget.nodeId,
            memberCount: 0,
            churchCount: 0,
            leaderCount: 0);
      }
      final progression = agg['progression'] is List
          ? (agg['progression'] as List)
              .whereType<Map>()
              .map((e) => Map<String, dynamic>.from(e))
              .toList()
          : const <Map<String, dynamic>>[];
      List<Map<String, dynamic>> asList(Response res) => res.data is List
          ? (res.data as List)
              .whereType<Map>()
              .map((e) => Map<String, dynamic>.from(e))
              .toList()
          : const <Map<String, dynamic>>[];
      setState(() {
        _aggregate = parsed;
        _progression = progression;
        _children = asList(results[1]);
        _features = asList(results[2]);
        _loading = false;
      });
    } catch (e) {
      // Lecture seule : en cas d'échec on affiche l'erreur, jamais de cache V3.
      if (mounted) {
        setState(() {
          _loading = false;
          _error = 'Chargement impossible : $e';
        });
      }
    }
  }

  /// Bascule d'un module sur CE nœud (D). La liste complète est renvoyée au
  /// serveur (PUT = remplacement), sinon les autres modules seraient perdus.
  Future<void> _toggleFeature(String moduleCode, bool enabled) async {
    final payload = _features
        .map((f) => {
              'code': f['moduleCode'],
              'enabled': f['moduleCode'] == moduleCode
                  ? enabled
                  : f['enabled'] == true,
              if (f['configurationJson'] != null)
                'configurationJson': f['configurationJson'],
            })
        .toList();
    if (!_features.any((f) => f['moduleCode'] == moduleCode)) {
      payload.add({'code': moduleCode, 'enabled': enabled});
    }
    try {
      await _api.put(
        '/tenant/organization/nodes/${widget.nodeId}/features',
        data: {'modules': payload},
      );
      await _load();
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context)
            .showSnackBar(SnackBar(content: Text('Module impossible : $e')));
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    final agg = _aggregate;
    return Scaffold(
      appBar: AppBar(
        title: Text(widget.nodeName),
        actions: [
          IconButton(icon: const Icon(Icons.refresh), onPressed: _load),
        ],
      ),
      body: _loading
          ? const Center(child: CircularProgressIndicator())
          : _error != null
              ? Center(child: Text(_error!))
              : RefreshIndicator(
                  onRefresh: _load,
                  child: ListView(
                    padding: const EdgeInsets.all(16),
                    children: [
                      if (widget.levelName != null ||
                          widget.responsibleName != null)
                        Padding(
                          padding: const EdgeInsets.only(bottom: 12),
                          child: Text(
                            [
                              if (widget.levelName != null)
                                widget.levelName!,
                              if (widget.responsibleName != null)
                                'Responsable : ${widget.responsibleName}',
                            ].join(' • '),
                            style: const TextStyle(
                                fontWeight: FontWeight.w600),
                          ),
                        ),
                      _counters(agg),
                      if (_progression.isNotEmpty) ...[
                        const SizedBox(height: 16),
                        const Text('Progression (snapshots)',
                            style: TextStyle(fontWeight: FontWeight.w600)),
                        const SizedBox(height: 8),
                        _sparkline(),
                      ],
                      const SizedBox(height: 24),
                      const Text('Unités rattachées',
                          style: TextStyle(fontWeight: FontWeight.w600)),
                      if (_children.isEmpty)
                        const Padding(
                          padding: EdgeInsets.symmetric(vertical: 12),
                          child: Text('Aucune sous-unité.',
                              style: TextStyle(color: Colors.grey)),
                        )
                      else
                        ..._children.map(_childTile),
                      const SizedBox(height: 24),
                      const Text('Modules de ce nœud',
                          style: TextStyle(fontWeight: FontWeight.w600)),
                      const SizedBox(height: 8),
                      ..._allModules(),
                    ],
                  ),
                ),
    );
  }

  Widget _counters(NodeAggregate? agg) {
    Widget cell(String label, int value, Color color) => Expanded(
          child: Card(
            child: Padding(
              padding: const EdgeInsets.symmetric(vertical: 14),
              child: Column(
                children: [
                  Text('$value',
                      style: TextStyle(
                          fontSize: 22,
                          fontWeight: FontWeight.bold,
                          color: color)),
                  Text(label,
                      style:
                          const TextStyle(fontSize: 12, color: Colors.grey)),
                ],
              ),
            ),
          ),
        );
    return Column(
      children: [
        Row(
          children: [
            cell('Membres', agg?.memberCount ?? 0, Colors.indigo),
            cell('Églises', agg?.churchCount ?? 0, Colors.teal),
          ],
        ),
        Row(
          children: [
            cell('Leaders', agg?.leaderCount ?? 0, Colors.orange),
            cell('Sermons', agg?.sermonCount ?? 0, Colors.green),
          ],
        ),
      ],
    );
  }

  /// Mini-courbe de progression (série de snapshots memberCount), sans PII.
  Widget _sparkline() {
    final values = _progression
        .map((p) => (p['memberCount'] as num?)?.toDouble() ?? 0)
        .toList();
    final maxV = values.fold<double>(1, (a, b) => a > b ? a : b);
    return SizedBox(
      height: 60,
      child: CustomPaint(
        size: const Size(double.infinity, 60),
        painter: _SparklinePainter(values.map((v) => v / maxV).toList()),
      ),
    );
  }

  Widget _childTile(Map<String, dynamic> child) {
    final id = child['id']?.toString() ?? '';
    final name = child['name']?.toString() ?? 'Unité';
    final level = child['levelName']?.toString() ?? '';
    final members = (child['memberCount'] as num?)?.toInt();
    return Card(
      margin: const EdgeInsets.symmetric(vertical: 4),
      child: ListTile(
        leading: CircleAvatar(
          child: Text(level.isNotEmpty ? level[0].toUpperCase() : '•'),
        ),
        title: Text(name),
        subtitle: Text([
          if (child['levelName'] != null) '${child['levelName']}',
          if (members != null) '$members membres',
        ].join(' • ')),
        trailing: const Icon(Icons.chevron_right),
        onTap: () => Navigator.push(
          context,
          MaterialPageRoute(
            builder: (_) => NodeDetailScreen(
              nodeId: id,
              nodeName: name,
              levelName: child['levelName']?.toString(),
              responsibleName: child['responsibleName']?.toString(),
            ),
          ),
        ),
      ),
    );
  }

  /// Modules connus (labels web/mobile) + état résolu pour ce nœud.
  List<Widget> _allModules() {
    const known = [
      'people', 'events', 'notifications', 'dashboard', 'org', 'families',
      'groups', 'discipleship', 'academy', 'finance', 'media', 'pastoral',
      'prayer', 'sermons', 'assets', 'workflow', 'reports', 'analytics',
      'messaging', 'documents', 'calendar', 'forms', 'ai', 'chat', 'payments',
    ];
    final byCode = {
      for (final f in _features) f['moduleCode']?.toString(): f,
    };
    return [
      for (final code in known)
        SwitchListTile(
          dense: true,
          title: Text(moduleLabelStatic(code)),
          subtitle: Text(
              byCode.containsKey(code) ? code : '$code (défaut)'),
          value: byCode[code]?['enabled'] == true,
          onChanged: (v) => _toggleFeature(code, v),
        ),
    ];
  }
}

class _SparklinePainter extends CustomPainter {
  _SparklinePainter(this.ratios);
  final List<double> ratios;

  @override
  void paint(Canvas canvas, Size size) {
    if (ratios.length < 2) return;
    final paint = Paint()
      ..color = Colors.indigo
      ..strokeWidth = 2
      ..style = PaintingStyle.stroke;
    final path = Path();
    for (var i = 0; i < ratios.length; i++) {
      final x = size.width * i / (ratios.length - 1);
      final y = size.height * (1 - ratios[i]);
      if (i == 0) {
        path.moveTo(x, y);
      } else {
        path.lineTo(x, y);
      }
    }
    canvas.drawPath(path, paint);
  }

  @override
  bool shouldRepaint(covariant _SparklinePainter oldDelegate) =>
      oldDelegate.ratios != ratios;
}
