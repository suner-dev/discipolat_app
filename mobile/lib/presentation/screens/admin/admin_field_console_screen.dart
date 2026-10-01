import 'dart:async';

import 'package:flutter/material.dart';
import '../../../data/services/api_service.dart';
import '../../../data/services/realtime_bus_service.dart';
import '../../../../l10n/app_localizations.dart';

/// §G5.5 (§59) — Console admin terrain : actions fréquentes mobiles sur APIs RÉELLES.
/// - Approbations workflow : GET /workflow-engine/tasks/pending (enrichi §G2.5)
///   → POST /tasks/{id}/approve | /reject avec commentaire.
/// - Validation de présence : départements → événements du département
///   → feuille de présence GET/PUT /departments/{d}/events/{e}/attendance.
/// Zéro mock : chaque bouton branché sur l'endpoint correspondant.
///
/// §G5.8 — la console écoute le firehose du tenant : une approbation ou une
/// présence saisie sur un AUTRE appareil (web ou autre mobile) recharge la
/// section active en < 5 s ; trou de delta → rafraîchissement complet.
class AdminFieldConsoleScreen extends StatefulWidget {
  const AdminFieldConsoleScreen({super.key, this.apiService, this.realtimeEvents});
  final ApiService? apiService;
  /// Injecté par les tests ; null → bus global de l'app.
  final Stream<RealtimeEvent>? realtimeEvents;

  @override
  State<AdminFieldConsoleScreen> createState() => _AdminFieldConsoleScreenState();
}

class _AdminFieldConsoleScreenState extends State<AdminFieldConsoleScreen> {
  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context);
    return DefaultTabController(
      length: 2,
      child: Scaffold(
        appBar: AppBar(
          title: Text(l10n.adminConsoleTitle),
          backgroundColor: Colors.indigo,
          foregroundColor: Colors.white,
          bottom: TabBar(
            indicatorColor: Colors.white,
            tabs: [
              Tab(icon: const Icon(Icons.fact_check_outlined, size: 18), text: l10n.adminConsoleTabApprovals),
              Tab(icon: const Icon(Icons.how_to_reg_outlined, size: 18), text: l10n.adminConsoleTabAttendance),
            ],
          ),
        ),
        body: TabBarView(
          children: [
            _ApprovalsTab(apiService: widget.apiService, realtimeEvents: widget.realtimeEvents),
            _AttendanceTab(apiService: widget.apiService, realtimeEvents: widget.realtimeEvents),
          ],
        ),
      ),
    );
  }
}

// ==================== APPROBATIONS ====================

class _ApprovalsTab extends StatefulWidget {
  const _ApprovalsTab({this.apiService, this.realtimeEvents});
  final ApiService? apiService;
  final Stream<RealtimeEvent>? realtimeEvents;

  @override
  State<_ApprovalsTab> createState() => _ApprovalsTabState();
}

class _ApprovalsTabState extends State<_ApprovalsTab> {
  late final ApiService _api = widget.apiService ?? ApiService();
  List<dynamic> _tasks = [];
  bool _loading = true;
  String? _error;
  StreamSubscription<RealtimeEvent>? _realtime;

  @override
  void initState() {
    super.initState();
    load();
    // §G5.8 — une tâche assignée/terminée ailleurs (web ou autre mobile)
    // recharge la file d'approbation ; trou de delta → tout recharger.
    _realtime = (widget.realtimeEvents ?? RealtimeBus.instance.events)
        .listen((e) {
      if (!mounted) return;
      if (e.fullRefresh ||
          e.eventType == 'TaskAssigned' ||
          e.eventType == 'TaskCompleted') {
        load();
      }
    });
  }

  @override
  void dispose() {
    _realtime?.cancel();
    super.dispose();
  }

  Future<void> load() async {
    setState(() { _loading = true; _error = null; });
    try {
      final res = await _api.get('/workflow-engine/tasks/pending');
      final d = res.data;
      if (!mounted) return;
      setState(() {
        _tasks = d is List ? d : <dynamic>[];
        _loading = false;
      });
    } catch (_) {
      if (!mounted) return;
      setState(() { _error = AppLocalizations.of(context).adminConsoleLoadError; _loading = false; });
    }
  }

  Future<void> decide(dynamic task, {required bool approve}) async {
    final l10n = AppLocalizations.of(context);
    final controller = TextEditingController();
    final confirmed = await showDialog<bool>(
      context: context,
      builder: (ctx) => AlertDialog(
        title: Text(approve ? l10n.adminConsoleApprove : l10n.adminConsoleReject),
        content: TextField(
          controller: controller,
          decoration: InputDecoration(hintText: l10n.adminConsoleCommentHint),
          maxLines: 3,
        ),
        actions: [
          TextButton(onPressed: () => Navigator.pop(ctx, false), child: Text(l10n.cancel)),
          TextButton(onPressed: () => Navigator.pop(ctx, true), child: Text(l10n.confirm)),
        ],
      ),
    );
    if (confirmed != true || !mounted) return;
    final taskId = task['taskId']?.toString() ?? '';
    try {
      await _api.post('/workflow-engine/tasks/$taskId/${approve ? 'approve' : 'reject'}',
          data: {'comment': controller.text});
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(l10n.adminConsoleDecisionDone)));
      await load();
    } catch (_) {
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(l10n.adminConsoleDecisionError)));
    }
  }

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context);
    if (_loading) return const Center(child: CircularProgressIndicator());
    if (_error != null) {
      return Center(
        child: Column(mainAxisSize: MainAxisSize.min, children: [
          Padding(padding: const EdgeInsets.all(24), child: Text(_error!, textAlign: TextAlign.center)),
          ElevatedButton(onPressed: load, child: Text(l10n.retry)),
        ]),
      );
    }
    if (_tasks.isEmpty) {
      return Center(child: Text(l10n.adminConsoleNoApprovals));
    }
    return RefreshIndicator(
      onRefresh: load,
      child: ListView.builder(
        padding: const EdgeInsets.all(16),
        itemCount: _tasks.length,
        itemBuilder: (context, i) {
          final t = _tasks[i] as Map<String, dynamic>;
          final due = DateTime.tryParse(t['dueAt']?.toString() ?? '');
          return Card(
            margin: const EdgeInsets.only(bottom: 10),
            child: ListTile(
              leading: CircleAvatar(
                backgroundColor: Colors.indigo.withValues(alpha: .12),
                child: const Icon(Icons.fact_check, color: Colors.indigo, size: 20),
              ),
              title: Text(
                '${t['workflowName'] ?? t['entityType'] ?? 'Workflow'} · ${t['stepName'] ?? ''}',
                style: const TextStyle(fontSize: 14, fontWeight: FontWeight.w600),
              ),
              subtitle: due != null
                  ? Text(due.toLocal().toString(), style: const TextStyle(fontSize: 11))
                  : null,
              trailing: Row(
                mainAxisSize: MainAxisSize.min,
                children: [
                  IconButton(
                    icon: const Icon(Icons.check_circle, color: Colors.green),
                    tooltip: l10n.adminConsoleApprove,
                    onPressed: () => decide(t, approve: true),
                  ),
                  IconButton(
                    icon: const Icon(Icons.cancel, color: Colors.red),
                    tooltip: l10n.adminConsoleReject,
                    onPressed: () => decide(t, approve: false),
                  ),
                ],
              ),
            ),
          );
        },
      ),
    );
  }
}

// ==================== PRÉSENCE ====================

class _AttendanceTab extends StatefulWidget {
  const _AttendanceTab({this.apiService, this.realtimeEvents});
  final ApiService? apiService;
  final Stream<RealtimeEvent>? realtimeEvents;

  @override
  State<_AttendanceTab> createState() => _AttendanceTabState();
}

class _AttendanceTabState extends State<_AttendanceTab> {
  late final ApiService _api = widget.apiService ?? ApiService();
  List<dynamic> _departments = [];
  List<dynamic> _events = [];
  Map<String, dynamic>? _sheet;
  String? _departmentId;
  String? _eventId;
  bool _loading = false;
  String? _error;
  StreamSubscription<RealtimeEvent>? _realtime;

  @override
  void initState() {
    super.initState();
    loadDepartments();
    // §G5.8 — présence pointée ailleurs → la feuille ouverte se rafraîchit
    // (jamais en plein envoi local), et un trou de delta recharge tout.
    _realtime = (widget.realtimeEvents ?? RealtimeBus.instance.events)
        .listen((e) {
      if (!mounted || _loading) return;
      if (e.fullRefresh || e.eventType == 'AttendanceRecorded') {
        _eventId != null ? selectEvent(_eventId) : loadDepartments();
      }
    });
  }

  @override
  void dispose() {
    _realtime?.cancel();
    super.dispose();
  }

  List<dynamic> _asList(dynamic d) => d is List
      ? d
      : (d is Map && d['content'] is List ? d['content'] as List : <dynamic>[]);

  Future<void> loadDepartments() async {
    setState(() { _loading = true; _error = null; });
    try {
      final res = await _api.get('/departments');
      if (!mounted) return;
      setState(() {
        _departments = _asList(res.data);
        _loading = false;
      });
    } catch (_) {
      if (!mounted) return;
      setState(() { _error = AppLocalizations.of(context).adminConsoleLoadError; _loading = false; });
    }
  }

  Future<void> selectDepartment(String? id) async {
    setState(() {
      _departmentId = id;
      _eventId = null;
      _sheet = null;
      _events = [];
    });
    if (id == null) return;
    setState(() { _loading = true; });
    try {
      final res = await _api.get('/events/department/$id', params: {'size': 20});
      if (!mounted) return;
      setState(() { _events = _asList(res.data); _loading = false; });
    } catch (_) {
      if (!mounted) return;
      setState(() { _error = AppLocalizations.of(context).adminConsoleLoadError; _loading = false; });
    }
  }

  Future<void> selectEvent(String? id) async {
    _eventId = id;
    _sheet = null;
    if (id == null) return;
    setState(() { _loading = true; });
    try {
      final res = await _api.get('/departments/$_departmentId/events/$id/attendance');
      if (!mounted) return;
      setState(() {
        _sheet = Map<String, dynamic>.from(res.data as Map);
        _loading = false;
      });
    } catch (_) {
      if (!mounted) return;
      setState(() { _error = AppLocalizations.of(context).adminConsoleLoadError; _loading = false; });
    }
  }

  Future<void> mark(dynamic member, bool present) async {
    final l10n = AppLocalizations.of(context);
    try {
      await _api.put('/departments/$_departmentId/events/$_eventId/attendance',
          data: {'soulId': member['soulId'], 'present': present});
      await selectEvent(_eventId);
      if (mounted) {
        ScaffoldMessenger.of(context)
            .showSnackBar(SnackBar(content: Text(l10n.adminConsoleDecisionDone)));
      }
    } catch (_) {
      if (mounted) {
        ScaffoldMessenger.of(context)
            .showSnackBar(SnackBar(content: Text(l10n.adminConsoleDecisionError)));
      }
    }
  }

  Future<void> markAll() async {
    try {
      await _api.post('/departments/$_departmentId/events/$_eventId/attendance/mark-all?present=true');
      await selectEvent(_eventId);
    } catch (_) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(SnackBar(
            content: Text(AppLocalizations.of(context).adminConsoleDecisionError)));
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context);
    return ListView(
      padding: const EdgeInsets.all(16),
      children: [
        if (_loading)
          const Padding(padding: EdgeInsets.all(16), child: Center(child: CircularProgressIndicator())),
        if (_error != null)
          Padding(padding: const EdgeInsets.all(12), child: Text(_error!, textAlign: TextAlign.center)),
        DropdownButtonFormField<String>(
          value: _departmentId,
          decoration: InputDecoration(labelText: l10n.adminConsolePickDepartment),
          items: _departments
              .map((d) => DropdownMenuItem(
                    value: d['id'].toString(),
                    child: Text(d['nom']?.toString() ?? '${d['id']}'),
                  ))
              .toList(),
          onChanged: selectDepartment,
        ),
        const SizedBox(height: 12),
        if (_departmentId != null && _events.isNotEmpty)
          DropdownButtonFormField<String>(
            value: _eventId,
            decoration: InputDecoration(labelText: l10n.adminConsolePickEvent),
            items: _events
                .map((e) => DropdownMenuItem(
                      value: e['id'].toString(),
                      child: Text(e['titre']?.toString() ?? '${e['id']}'),
                    ))
                .toList(),
            onChanged: selectEvent,
          ),
        if (_departmentId != null && !_loading && _events.isEmpty)
          Padding(padding: const EdgeInsets.all(12), child: Text(l10n.adminConsoleNoEvents, textAlign: TextAlign.center)),
        if (_sheet != null) ..._buildSheet(l10n),
      ],
    );
  }

  List<Widget> _buildSheet(AppLocalizations l10n) {
    final membres = (_sheet!['membres'] as List? ?? <dynamic>[]);
    final total = _sheet!['total'] ?? membres.length;
    final presents = _sheet!['presents'] ?? 0;
    return [
      ListTile(
        title: Text('${_sheet!['eventTitre'] ?? ''}',
            style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 16)),
        subtitle: Text('✓ $presents / $total'),
        trailing: ElevatedButton.icon(
          onPressed: markAll,
          icon: const Icon(Icons.done_all, size: 18),
          label: Text(l10n.adminConsoleMarkAll),
        ),
      ),
      ...membres.map((m) {
        final present = m['present'];
        final color = present == null
            ? Colors.grey
            : (present == true ? Colors.green : Colors.red);
        return ListTile(
          dense: true,
          leading: Icon(
            present == null
                ? Icons.radio_button_unchecked
                : (present == true ? Icons.check_circle : Icons.cancel),
            color: color,
            size: 20,
          ),
          title: Text(m['nom']?.toString() ?? '—'),
          trailing: IconButton(
            icon: const Icon(Icons.how_to_reg, size: 20),
            tooltip: l10n.adminConsoleMarkAll,
            onPressed: () => mark(m, present != true),
          ),
          onTap: () => mark(m, present != true),
        );
      }),
    ];
  }
}
