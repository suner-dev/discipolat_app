import 'dart:async';

import 'package:flutter/material.dart';
import '../../../data/services/api_service.dart';
import '../../../data/services/realtime_bus_service.dart';
import '../../../../l10n/app_localizations.dart';
import '../../widgets/glass_theme.dart';
import '../../widgets/app_drawer.dart';
import '../../widgets/secure_screen.dart';

/// §G4.3 — Ministère pastoral (mobile, consultation terrain).
///
/// Lecture seule côté mobile : les ÉCRITURES (nomination, fin de mandat,
/// transfert) restent sur le web, réservées ADMIN / PASTOR_PRINCIPAL
/// (@PreAuthorize). Le mobile consulte les mandats actifs/passés, les
/// transferts en attente et l'historique par pasteur — APIs RÉELLES :
/// GET /pastorate/appointments, /pastorate/transfers,
/// /pastorate/pastors/{id}/history, /org/tree/flat, /users?role=PASTEUR.
///
/// §G4.4 — nomination/fin de mandat reçue via le firehose → rechargement
/// immédiat (< 5 s), jamais de liste périmée.
class PastoralMinistryScreen extends StatefulWidget {
  const PastoralMinistryScreen({super.key, this.apiService, this.realtimeEvents});

  final ApiService? apiService;

  /// Injecté par les tests ; null → bus global de l'app.
  final Stream<RealtimeEvent>? realtimeEvents;

  @override
  State<PastoralMinistryScreen> createState() => _PastoralMinistryScreenState();
}

class _PastoralMinistryScreenState extends State<PastoralMinistryScreen>
    with SingleTickerProviderStateMixin {
  late final ApiService _api = widget.apiService ?? ApiService();
  late final TabController _tabs = TabController(length: 3, vsync: this);

  List<dynamic> _appointments = [];
  List<dynamic> _transfers = [];
  List<dynamic> _pastors = [];
  Map<String, String> _unitNames = {};
  bool _loading = true;
  String? _error;
  StreamSubscription<RealtimeEvent>? _realtime;

  // Historique par pasteur (onglet 3)
  String? _historyPastorId;
  Map<String, dynamic>? _history;
  bool _historyLoading = false;

  @override
  void initState() {
    super.initState();
    load();
    _realtime = (widget.realtimeEvents ?? RealtimeBus.instance.events)
        .listen((e) {
      if (!mounted) return;
      if (e.fullRefresh ||
          e.eventType == 'PastorAppointed' ||
          e.eventType == 'PastorEnded' ||
          e.eventType == 'PermissionsChanged') {
        unawaited(load(silent: true));
      }
    });
  }

  @override
  void dispose() {
    _realtime?.cancel();
    _tabs.dispose();
    super.dispose();
  }

  Future<void> load({bool silent = false}) async {
    if (!silent && mounted) setState(() { _loading = true; _error = null; });
    try {
      final results = await Future.wait([
        _api.get('/pastorate/appointments'),
        _api.get('/pastorate/transfers'),
        _api.get('/org/tree/flat'),
        _api.get('/users', params: {'role': 'PASTEUR', 'size': 100}),
      ]);
      if (!mounted) return;
      final appts = results[0].data;
      final transfers = results[1].data;
      final units = results[2].data;
      final usersContent = (results[3].data as Map?)?['content'];
      setState(() {
        _appointments = appts is List ? appts : [];
        _transfers = transfers is List ? transfers : [];
        _unitNames = {
          if (units is List)
            for (final u in units)
              u['id']?.toString() ?? '': u['name']?.toString() ?? '',
        };
        _pastors = usersContent is List ? usersContent : [];
        if (_historyPastorId == null && _pastors.isNotEmpty) {
          _historyPastorId = _pastors.first['id']?.toString();
        }
        _loading = false;
      });
      if (_historyPastorId != null) unawaited(_loadHistory());
    } catch (_) {
      if (!mounted || silent) return;
      setState(() {
        _error = AppLocalizations.of(context).pastoralLoadError;
        _loading = false;
      });
    }
  }

  Future<void> _loadHistory() async {
    if (_historyPastorId == null) return;
    setState(() { _historyLoading = true; _history = null; });
    try {
      final res = await _api
          .get('/pastorate/pastors/$_historyPastorId/history');
      if (!mounted) return;
      setState(() {
        _history = res.data is Map ? Map<String, dynamic>.from(res.data) : null;
        _historyLoading = false;
      });
    } catch (_) {
      if (!mounted) return;
      setState(() { _historyLoading = false; _history = null; });
    }
  }

  String _pastorName(String? pastorId) {
    for (final p in _pastors) {
      if (p is Map && p['id']?.toString() == pastorId) {
        final n =
            '${p['firstName'] ?? ''} ${p['lastName'] ?? ''}'.trim();
        if (n.isNotEmpty) return n;
      }
    }
    return pastorId != null && pastorId.length > 6
        ? '#${pastorId.substring(0, 6)}'
        : '—';
  }

  String _unitName(String? unitId) =>
      unitId == null ? '—' : (_unitNames[unitId] ?? unitId);

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context);
    return SecureScreen(
      screenName: 'PastoralMinistryScreen',
      auditAction: 'VIEW_PASTORAL_MINISTRY',
      child: Scaffold(
        appBar: AppBar(
          title: Text(l10n.pastoralTitle),
          backgroundColor: Colors.indigo,
          foregroundColor: Colors.white,
          actions: [
            IconButton(
              icon: const Icon(Icons.refresh),
              onPressed: load,
              tooltip: l10n.refresh,
            ),
          ],
          bottom: TabBar(
            controller: _tabs,
            indicatorColor: Colors.white,
            tabs: [
              Tab(text: l10n.pastoralTabMandates),
              Tab(text: l10n.pastoralTabTransfers),
              Tab(text: l10n.pastoralTabHistory),
            ],
          ),
        ),
        drawer: const AppDrawer(),
        body: _loading
            ? const Center(child: CircularProgressIndicator())
            : _error != null
                ? Center(
                    child: Column(mainAxisSize: MainAxisSize.min, children: [
                      Padding(
                        padding: const EdgeInsets.all(24),
                        child: Text(_error!, textAlign: TextAlign.center),
                      ),
                      ElevatedButton(onPressed: load, child: Text(l10n.retry)),
                    ]),
                  )
                : TabBarView(
                    controller: _tabs,
                    children: [
                      _buildMandates(),
                      _buildTransfers(),
                      _buildHistory(),
                    ],
                  ),
      ),
    );
  }

  Widget _buildMandates() {
    final l10n = AppLocalizations.of(context);
    final active = _appointments
        .where((a) => (a['status'] ?? '').toString() == 'ACTIVE')
        .toList();
    final past = _appointments
        .where((a) => (a['status'] ?? '').toString() != 'ACTIVE')
        .toList();
    if (_appointments.isEmpty) {
      return RefreshIndicator(
        onRefresh: load,
        child: ListView(
          physics: const AlwaysScrollableScrollPhysics(),
          padding: const EdgeInsets.all(24),
          children: [
            const SizedBox(height: 48),
            Center(child: Text(l10n.pastoralNoMandates)),
          ],
        ),
      );
    }
    return RefreshIndicator(
      onRefresh: load,
      child: ListView(
        padding: const EdgeInsets.fromLTRB(16, 16, 16, 24),
        children: [
          Text(l10n.pastoralActive,
              style: const TextStyle(
                  color: Colors.white,
                  fontSize: 16,
                  fontWeight: FontWeight.bold)),
          const SizedBox(height: 8),
          ...active.map((a) => _mandateCard(a, active: true)),
          if (active.isEmpty)
            GlassCard(
                child: Text(l10n.pastoralNoMandates,
                    style:
                        TextStyle(color: Colors.white.withValues(alpha: 0.5)))),
          const SizedBox(height: 16),
          Text(l10n.pastoralEnded,
              style: const TextStyle(
                  color: Colors.white,
                  fontSize: 16,
                  fontWeight: FontWeight.bold)),
          const SizedBox(height: 8),
          ...past.map((a) => _mandateCard(a, active: false)),
        ],
      ),
    );
  }

  Widget _mandateCard(dynamic a, {required bool active}) {
    return Padding(
      padding: const EdgeInsets.only(bottom: 8),
      child: GlassCard(
        padding: const EdgeInsets.all(12),
        child: Row(
          children: [
            Icon(Icons.workspace_premium,
                color: active ? Colors.green : Colors.grey, size: 22),
            const SizedBox(width: 12),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(_pastorName(a['pastorId']?.toString()),
                      style: const TextStyle(
                          color: Colors.white,
                          fontWeight: FontWeight.w600,
                          fontSize: 14)),
                  Text(
                    '${a['roleCode'] ?? ''} • ${_unitName(a['organizationUnitId']?.toString())}',
                    style: TextStyle(
                        color: Colors.white.withValues(alpha: 0.6),
                        fontSize: 12),
                  ),
                  Text(
                    '${a['startDate'] ?? ''}${a['endDate'] != null ? ' → ${a['endDate']}' : ''}',
                    style: TextStyle(
                        color: Colors.white.withValues(alpha: 0.4),
                        fontSize: 11),
                  ),
                ],
              ),
            ),
            if (a['title']?.toString().isNotEmpty == true)
              Container(
                padding:
                    const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
                decoration: BoxDecoration(
                  color: Colors.indigo.withValues(alpha: 0.25),
                  borderRadius: BorderRadius.circular(8),
                ),
                child: Text(a['title'].toString(),
                    style: const TextStyle(
                        color: Colors.white70, fontSize: 10)),
              ),
          ],
        ),
      ),
    );
  }

  Widget _buildTransfers() {
    final l10n = AppLocalizations.of(context);
    if (_transfers.isEmpty) {
      return RefreshIndicator(
        onRefresh: load,
        child: ListView(
          physics: const AlwaysScrollableScrollPhysics(),
          padding: const EdgeInsets.all(24),
          children: [
            const SizedBox(height: 48),
            Center(child: Text(l10n.pastoralNoTransfers)),
          ],
        ),
      );
    }
    return RefreshIndicator(
      onRefresh: load,
      child: ListView(
        padding: const EdgeInsets.fromLTRB(16, 16, 16, 24),
        children: [
          ..._transfers.map((t) => Padding(
                padding: const EdgeInsets.only(bottom: 8),
                child: GlassCard(
                  padding: const EdgeInsets.all(12),
                  child: Row(
                    children: [
                      Icon(
                        (t['status'] ?? '').toString() == 'PENDING'
                            ? Icons.pending_actions
                            : Icons.swap_horiz,
                        color: (t['status'] ?? '').toString() == 'PENDING'
                            ? Colors.orange
                            : Colors.green,
                        size: 22,
                      ),
                      const SizedBox(width: 12),
                      Expanded(
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Text(_pastorName(t['pastorId']?.toString()),
                                style: const TextStyle(
                                    color: Colors.white,
                                    fontWeight: FontWeight.w600,
                                    fontSize: 14)),
                            Text(
                              '${_unitName(t['fromOrgUnitId']?.toString())} → ${_unitName(t['toOrgUnitId']?.toString())}',
                              style: TextStyle(
                                  color:
                                      Colors.white.withValues(alpha: 0.6),
                                  fontSize: 12),
                            ),
                            if (t['reason']?.toString().isNotEmpty == true)
                              Text(t['reason'].toString(),
                                  style: TextStyle(
                                      color:
                                          Colors.white.withValues(alpha: 0.4),
                                      fontSize: 11)),
                          ],
                        ),
                      ),
                      Text('${t['status'] ?? ''}',
                          style: const TextStyle(
                              color: Colors.white54, fontSize: 11)),
                    ],
                  ),
                ),
              )),
        ],
      ),
    );
  }

  Widget _buildHistory() {
    final l10n = AppLocalizations.of(context);
    return RefreshIndicator(
      onRefresh: _loadHistory,
      child: ListView(
        physics: const AlwaysScrollableScrollPhysics(),
        padding: const EdgeInsets.fromLTRB(16, 16, 16, 24),
        children: [
          DropdownButtonFormField<String>(
            key: const Key('pastoralHistoryPastor'),
            value: _historyPastorId,
            hint: Text(l10n.pastoralSelectPastor),
            dropdownColor: Colors.indigo.shade900,
            items: _pastors
                .map((p) => DropdownMenuItem(
                      value: p['id']?.toString() ?? '',
                      child: Text(
                          '${p['firstName'] ?? ''} ${p['lastName'] ?? ''}'
                              .trim(),
                          style: const TextStyle(color: Colors.white)),
                    ))
                .toList(),
            onChanged: (v) {
              setState(() => _historyPastorId = v);
              unawaited(_loadHistory());
            },
          ),
          const SizedBox(height: 12),
          if (_historyLoading)
            const Padding(
              padding: EdgeInsets.all(24),
              child: Center(child: CircularProgressIndicator()),
            )
          else if (_history == null)
            GlassCard(
                child: Text(l10n.pastoralNoHistory,
                    style:
                        TextStyle(color: Colors.white.withValues(alpha: 0.5))))
          else ...[
            ...(((_history!['appointments']) as List?) ?? [])
                .map((a) => _mandateCard(a,
                    active: (a['status'] ?? '').toString() == 'ACTIVE')),
            ...(((_history!['transfers']) as List?) ?? []).map((t) => Padding(
                  padding: const EdgeInsets.only(bottom: 8),
                  child: GlassCard(
                    padding: const EdgeInsets.all(12),
                    child: Text(
                      '${l10n.pastoralTabTransfers}: '
                      '${_unitName(t['fromOrgUnitId']?.toString())} → '
                      '${_unitName(t['toOrgUnitId']?.toString())} • ${t['status'] ?? ''}',
                      style: const TextStyle(
                          color: Colors.white70, fontSize: 13),
                    ),
                  ),
                )),
          ],
        ],
      ),
    );
  }
}
