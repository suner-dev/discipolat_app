import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import '../../../app.dart';
import '../../../data/services/api_service.dart';
import '../../../data/services/realtime_bus_service.dart';
import '../../../../l10n/app_localizations.dart';
import '../../widgets/glass_theme.dart';
import '../../widgets/app_drawer.dart';
import '../../widgets/secure_screen.dart';

/// FamilySpaceScreen — « système d'exploitation » de la famille (§G4.1/G4.2).
///
/// Données 100 % RÉELLES via /families/{id}/os/* (garde par famille côté
/// service : chef de famille, faiseur de la famille, ADMIN/PASTEUR).
/// - Aperçu : KPI suivis en retard, visites à venir, suivis, réceptions.
/// - Visites : liste + mise à jour de statut (PUT) + création (POST).
/// - Membres : âmes rattachées + ajout d'une âme via recherche scopée
///   CAMPUS/CHURCH (§G4.2 minimisation) avec faiseur attitré optionnel.
/// - Journal : activités paginées scopées famille (fuite corrigée §G4.1).
///
/// §G5.8 — le firehose du tenant recharge les sections (ajout d'âme fait sur
/// le web ⇄ mobile < 5 s) ; trou de delta → rechargement complet.
class FamilySpaceScreen extends ConsumerStatefulWidget {
  const FamilySpaceScreen({super.key, this.apiService, this.realtimeEvents});

  final ApiService? apiService;

  /// Injecté par les tests ; null → bus global de l'app.
  final Stream<RealtimeEvent>? realtimeEvents;

  @override
  ConsumerState<FamilySpaceScreen> createState() => _FamilySpaceScreenState();
}

class _FamilySpaceScreenState extends ConsumerState<FamilySpaceScreen>
    with SingleTickerProviderStateMixin {
  late final ApiService _api = widget.apiService ?? ApiService();
  late final TabController _tabs = TabController(length: 4, vsync: this);

  String? _familyId;
  String? _familyName;
  Map<String, dynamic>? _dashboard;
  List<dynamic> _visits = [];
  List<dynamic> _members = [];
  List<dynamic> _activities = [];
  int _journalTotal = 0;
  bool _isLoading = true;
  bool _noFamily = false;
  String? _error;
  int _currentNavIndex = 0;
  StreamSubscription<RealtimeEvent>? _realtime;

  @override
  void initState() {
    super.initState();
    _boot();
    // §G5.8/§G4.4 — une mutation vue famille faite ailleurs recharge l'écran.
    _realtime = (widget.realtimeEvents ?? RealtimeBus.instance.events)
        .listen((e) {
      if (!mounted) return;
      final mine = e.aggregateId != null && e.aggregateId == _familyId;
      if (e.fullRefresh || e.eventType == 'FamilyMemberAdded' || mine) {
        unawaited(_loadAll(silent: true));
      }
    });
  }

  @override
  void dispose() {
    _realtime?.cancel();
    _tabs.dispose();
    super.dispose();
  }

  String? get _userId => AuthState().userId;

  /// Résout LA famille de l'utilisateur courant puis charge tout.
  /// Ordre : famille en session → chef de famille (FamilyService) →
  /// famille de la première âme suivie comme faiseur.
  Future<void> _boot() async {
    setState(() { _isLoading = true; _error = null; _noFamily = false; });
    try {
      if (_familyId == null) {
        final auth = AuthState();
        String? famId = auth.familleGereeId;
        final userId = _userId;
        if (famId == null && userId != null) {
          final famRes = await _api
              .get('/families', params: {'chefFamilleId': userId, 'size': 1});
          final content = (famRes.data as Map?)?['content'];
          if (content is List && content.isNotEmpty) {
            famId = content.first['id']?.toString();
            _familyName = content.first['nom']?.toString();
          }
        }
        if (famId == null && userId != null) {
          final soulsRes = await _api
              .get('/souls', params: {'faiseurId': userId, 'size': 1});
          final content = (soulsRes.data as Map?)?['content'];
          if (content is List && content.isNotEmpty) {
            famId = content.first['familleId']?.toString();
          }
        }
        _familyId = famId;
      }
      if (_familyId == null) {
        if (!mounted) return;
        setState(() { _noFamily = true; _isLoading = false; });
        return;
      }
      if (_familyName == null) {
        try {
          final f = await _api.get('/families/$_familyId');
          _familyName = (f.data as Map?)?['nom']?.toString();
        } catch (_) {/* le nom reste optionnel */}
      }
      await _loadAll();
    } catch (e) {
      if (!mounted) return;
      setState(() {
        _error = AppLocalizations.of(context).familyError;
        _isLoading = false;
      });
    }
  }

  Future<void> _loadAll({bool silent = false}) async {
    if (_familyId == null) return;
    if (!silent && mounted) setState(() { _isLoading = true; _error = null; });
    final f = _familyId!;
    try {
      final results = await Future.wait([
        _api.get('/families/$f/os/dashboard'),
        _api.get('/families/$f/os/visits'),
        _api.get('/families/$f/os/members'),
        _api.get('/families/$f/os/activities',
            params: {'page': 0, 'size': 20}),
      ]);
      if (!mounted) return;
      final dash = results[0].data;
      final visits = results[1].data;
      final members = results[2].data;
      final acts = results[3].data;
      setState(() {
        _dashboard = dash is Map ? Map<String, dynamic>.from(dash) : null;
        _visits = visits is List ? visits : [];
        _members = members is List ? members : [];
        _activities = acts is Map && acts['content'] is List
            ? acts['content'] as List
            : <dynamic>[];
        _journalTotal =
            acts is Map ? int.tryParse('${acts['totalElements']}') ?? 0 : 0;
        _isLoading = false;
      });
    } catch (e) {
      // Rechargement silencieux (temps réel) : on ne casse pas l'écran affiché.
      if (!mounted || silent) return;
      setState(() {
        _error = AppLocalizations.of(context).familyError;
        _isLoading = false;
      });
    }
  }

  void _snack(String message) {
    if (!mounted) return;
    ScaffoldMessenger.of(context)
        .showSnackBar(SnackBar(content: Text(message)));
  }

  // ── Actions réelles ───────────────────────────────────────────────────────

  Future<void> _completeVisit(dynamic visit) async {
    final l10n = AppLocalizations.of(context);
    final vid = visit['id']?.toString() ?? '';
    try {
      await _api.put('/families/$_familyId/os/visits/$vid',
          data: {'status': 'COMPLETED'});
      _snack(l10n.familyOsVisitUpdated);
      await _loadAll(silent: true);
    } catch (_) {
      _snack(l10n.familyOsActionError);
    }
  }

  String _soulName(dynamic soulId) {
    final id = soulId?.toString();
    for (final m in _members) {
      if (m is Map && m['id']?.toString() == id) {
        final n = '${m['prenom'] ?? ''} ${m['nom'] ?? ''}'.trim();
        if (n.isNotEmpty) return n;
      }
    }
    // Les visites du dashboard portent déjà le nom résolu côté serveur.
    return id != null && id.length > 6 ? '#${id.substring(0, 6)}' : '—';
  }

  Future<void> _createVisitSheet() async {
    final l10n = AppLocalizations.of(context);
    if (_members.isEmpty) return;
    String soulId = _members.first['id']?.toString() ?? '';
    String visitType = 'VISITE';
    DateTime date = DateTime.now();
    final subject = TextEditingController();
    final ok = await showModalBottomSheet<bool>(
      context: context,
      isScrollControlled: true,
      builder: (ctx) => StatefulBuilder(
        builder: (ctx, setSheet) => Padding(
          padding: EdgeInsets.fromLTRB(16, 16, 16,
              MediaQuery.of(ctx).viewInsets.bottom + 16),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              Text(l10n.familyOsNewVisit,
                  style: const TextStyle(fontWeight: FontWeight.bold)),
              const SizedBox(height: 12),
              DropdownButtonFormField<String>(
                value: soulId,
                items: _members
                    .map((m) => DropdownMenuItem(
                          value: m['id']?.toString() ?? '',
                          child: Text(
                              '${m['prenom'] ?? ''} ${m['nom'] ?? ''}'.trim()),
                        ))
                    .toList(),
                onChanged: (v) => setSheet(() => soulId = v ?? soulId),
              ),
              const SizedBox(height: 8),
              DropdownButtonFormField<String>(
                value: visitType,
                items: ['VISITE', 'SUIVI', 'RENCONTRE']
                    .map((t) => DropdownMenuItem(value: t, child: Text(t)))
                    .toList(),
                onChanged: (v) => setSheet(() => visitType = v ?? visitType),
              ),
              const SizedBox(height: 8),
              ListTile(
                contentPadding: EdgeInsets.zero,
                title: Text('${l10n.familyOsVisitDate}: '
                    '${date.year}-${date.month.toString().padLeft(2, '0')}-${date.day.toString().padLeft(2, '0')}'),
                trailing: const Icon(Icons.event),
                onTap: () async {
                  final picked = await showDatePicker(
                      context: ctx, initialDate: date,
                      firstDate: DateTime.now().subtract(const Duration(days: 365)),
                      lastDate: DateTime.now().add(const Duration(days: 365)));
                  if (picked != null) setSheet(() => date = picked);
                },
              ),
              TextField(
                controller: subject,
                decoration: InputDecoration(hintText: l10n.familyOsSubjectHint),
              ),
              const SizedBox(height: 12),
              FilledButton(
                key: const Key('familyOsVisitSubmit'),
                onPressed: () => Navigator.pop(ctx, true),
                child: Text(l10n.confirm),
              ),
            ],
          ),
        ),
      ),
    );
    if (ok != true || soulId.isEmpty) return;
    try {
      await _api.post('/families/$_familyId/os/visits', data: {
        'soulId': soulId,
        'visitType': visitType,
        'visitDate':
            '${date.year}-${date.month.toString().padLeft(2, '0')}-${date.day.toString().padLeft(2, '0')}',
        if (subject.text.trim().isNotEmpty) 'subject': subject.text.trim(),
        'status': 'PLANNED',
      });
      _snack(l10n.familyOsVisitSaved);
      await _loadAll(silent: true);
    } catch (_) {
      _snack(l10n.familyOsActionError);
    }
  }

  /// Ajout d'âme §G4.2 : recherche scopée CAMPUS (défaut) / CHURCH,
  /// sélection du résultat, faiseur attitré optionnel → POST réel.
  Future<void> _addSoulSheet() async {
    final l10n = AppLocalizations.of(context);
    String scope = 'CAMPUS';
    String? selectedSoulId;
    String? faiseurId;
    String query = '';
    List<dynamic> candidates = [];
    List<dynamic> faiseurs = [];
    bool searching = false;
    final searchCtl = TextEditingController();

    Future<void> runSearch() async {
      // Le rebuild vient de la StatefulBuilder du sheet si elle est posée,
      // sinon de l'écran (avant l'ouverture du sheet).
      final set = _sheetSetter ?? (void Function() fn) => setState(fn);
      set(() => searching = true);
      try {
        final res = await _api.get('/families/$_familyId/os/search-souls',
            params: {if (query.isNotEmpty) 'search': query, 'scope': scope});
        set(() {
          candidates = res.data is List ? res.data as List : <dynamic>[];
          searching = false;
        });
      } catch (_) {
        set(() {
          candidates = [];
          searching = false;
        });
      }
    }

    await showModalBottomSheet<void>(
      context: context,
      isScrollControlled: true,
      builder: (ctx) => StatefulBuilder(
        builder: (ctx, setSheet) {
          _sheetSetter = (fn) => setSheet(fn);
          return Padding(
            padding: EdgeInsets.fromLTRB(16, 16, 16,
                MediaQuery.of(ctx).viewInsets.bottom + 16),
            child: SizedBox(
              height: MediaQuery.of(ctx).size.height * 0.7,
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.stretch,
                children: [
                  Text(l10n.familyOsAddSoul,
                      style: const TextStyle(fontWeight: FontWeight.bold)),
                  const SizedBox(height: 8),
                  Row(
                    children: [
                      ChoiceChip(
                        label: Text(l10n.familyOsScopeCampus),
                        selected: scope == 'CAMPUS',
                        onSelected: (_) {
                          scope = 'CAMPUS';
                          unawaited(runSearch());
                        },
                      ),
                      const SizedBox(width: 8),
                      ChoiceChip(
                        label: Text(l10n.familyOsScopeChurch),
                        selected: scope == 'CHURCH',
                        onSelected: (_) {
                          scope = 'CHURCH';
                          unawaited(runSearch());
                        },
                      ),
                    ],
                  ),
                  const SizedBox(height: 8),
                  Row(
                    children: [
                      Expanded(
                        child: TextField(
                          key: const Key('familyOsSoulSearch'),
                          controller: searchCtl,
                          decoration: InputDecoration(
                              hintText: l10n.familyOsSoulSearchHint),
                          onSubmitted: (v) {
                            query = v.trim();
                            unawaited(runSearch());
                          },
                        ),
                      ),
                      IconButton(
                        icon: const Icon(Icons.search),
                        onPressed: () {
                          query = searchCtl.text.trim();
                          unawaited(runSearch());
                        },
                      ),
                    ],
                  ),
                  const SizedBox(height: 8),
                  Expanded(
                    child: searching
                        ? const Center(child: CircularProgressIndicator())
                        : candidates.isEmpty
                            ? Center(
                                child: Text(l10n.familyOsNoCandidates,
                                    textAlign: TextAlign.center))
                            : ListView.builder(
                                itemCount: candidates.length,
                                itemBuilder: (_, i) {
                                  final c = candidates[i] as Map;
                                  final sid = c['soulId']?.toString() ?? '';
                                  final name =
                                      '${c['prenom'] ?? ''} ${c['nom'] ?? ''}'
                                          .trim();
                                  return RadioListTile<String>(
                                    key: Key('familyOsCandidate-$sid'),
                                    title: Text(name),
                                    value: sid,
                                    groupValue: selectedSoulId,
                                    onChanged: (v) =>
                                        setSheet(() => selectedSoulId = v),
                                  );
                                },
                              ),
                  ),
                  FutureBuilder<List<dynamic>>(
                    future: _faiseursFuture ??= _api
                        .get('/users',
                            params: {'role': 'FAISEUR', 'size': 100})
                        .then((r) {
                      final content = (r.data as Map?)?['content'];
                      return content is List ? content : <dynamic>[];
                    }),
                    builder: (ctx, snap) {
                      faiseurs = snap.data ?? <dynamic>[];
                      return DropdownButtonFormField<String?>(
                        key: const Key('familyOsFaiseur'),
                        value: faiseurId,
                        hint: Text(l10n.familyOsNoFaiseur),
                        items: [
                          DropdownMenuItem<String?>(
                              value: null, child: Text(l10n.familyOsNoFaiseur)),
                          ...faiseurs.map((u) => DropdownMenuItem<String?>(
                                value: u['id']?.toString() ?? '',
                                child: Text(
                                    '${u['firstName'] ?? ''} ${u['lastName'] ?? ''}'
                                        .trim()),
                              )),
                        ],
                        onChanged: (v) => setSheet(() => faiseurId = v),
                      );
                    },
                  ),
                  const SizedBox(height: 12),
                  FilledButton(
                    key: const Key('familyOsAddConfirm'),
                    onPressed: selectedSoulId == null
                        ? null
                        : () async {
                            Navigator.pop(ctx);
                            final q =
                                'soulId=$selectedSoulId${faiseurId != null && faiseurId!.isNotEmpty ? '&faiseurId=$faiseurId' : ''}';
                            try {
                              await _api.post(
                                  '/families/$_familyId/os/members?$q');
                              _snack(l10n.familyOsSoulAdded);
                              await _loadAll(silent: true);
                            } catch (_) {
                              _snack(l10n.familyOsActionError);
                            }
                          },
                    child: Text(l10n.familyOsConfirmAdd),
                  ),
                ],
              ),
            ),
          );
        },
      ),
    );
    _sheetSetter = null;
  }

  /// Setter de la StatefulBuilder active (le bottom sheet pilote son rebuild).
  void Function(void Function())? _sheetSetter;
  Future<List<dynamic>>? _faiseursFuture;

  // ── Rendu ─────────────────────────────────────────────────────────────────

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context);
    return SecureScreen(
      screenName: 'FamilySpaceScreen',
      auditAction: 'VIEW_FAMILY_SPACE',
      child: Scaffold(
        appBar: AppBar(
          title: Text(_familyName ?? l10n.familySpaceTitle),
          backgroundColor: Colors.redAccent,
          foregroundColor: Colors.white,
          actions: [
            IconButton(
              icon: const Icon(Icons.refresh),
              onPressed: _boot,
              tooltip: l10n.refresh,
            ),
          ],
          bottom: _noFamily
              ? null
              : TabBar(
                  controller: _tabs,
                  indicatorColor: Colors.white,
                  tabs: [
                    Tab(text: l10n.familyOsTabOverview),
                    Tab(text: l10n.familyOsTabVisits),
                    Tab(text: l10n.familyOsTabMembers),
                    Tab(text: l10n.familyOsTabJournal),
                  ],
                ),
        ),
        drawer: const AppDrawer(),
        body: _isLoading
            ? const Center(child: CircularProgressIndicator())
            : _error != null
                ? _buildErrorState()
                : _noFamily
                    ? _buildNoFamily()
                    : TabBarView(
                        controller: _tabs,
                        children: [
                          _buildOverview(),
                          _buildVisits(),
                          _buildMembers(),
                          _buildJournal(),
                        ],
                      ),
        floatingActionButton: _noFamily || _isLoading || _error != null
            ? null
            : FloatingActionButton(
                key: const Key('familyOsAddFab'),
                backgroundColor: Colors.redAccent,
                onPressed: _addSoulSheet,
                child: const Icon(Icons.person_add_alt_1, color: Colors.white),
              ),
        bottomNavigationBar: _buildBottomNav(),
      ),
    );
  }

  Widget _buildErrorState() {
    final l10n = AppLocalizations.of(context);
    return Center(
      child: Padding(
        padding: const EdgeInsets.all(24),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            const Icon(Icons.error_outline, color: Colors.red, size: 48),
            const SizedBox(height: 16),
            Text(_error!, textAlign: TextAlign.center,
                style: const TextStyle(fontSize: 16)),
            const SizedBox(height: 16),
            ElevatedButton.icon(
              onPressed: _boot,
              icon: const Icon(Icons.refresh),
              label: Text(l10n.retry),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildNoFamily() {
    final l10n = AppLocalizations.of(context);
    return RefreshIndicator(
      onRefresh: _boot,
      child: ListView(
        physics: const AlwaysScrollableScrollPhysics(),
        padding: const EdgeInsets.all(24),
        children: [
          const SizedBox(height: 48),
          Icon(Icons.family_restroom,
              size: 64, color: Colors.redAccent.withValues(alpha: 0.6)),
          const SizedBox(height: 16),
          Text(
            l10n.familyOsNoFamily,
            textAlign: TextAlign.center,
            style: const TextStyle(fontSize: 15),
          ),
        ],
      ),
    );
  }

  Widget _buildOverview() {
    final l10n = AppLocalizations.of(context);
    final dash = _dashboard ?? const <String, dynamic>{};
    final overdue = int.tryParse('${dash['overdueFollowUps'] ?? 0}') ?? 0;
    final upcoming = (dash['upcomingVisits'] as List?) ?? [];
    final followUps = (dash['followUps'] as List?) ?? [];
    final receptions = (dash['recentReceptions'] as List?) ?? [];
    return RefreshIndicator(
      onRefresh: _boot,
      child: ListView(
        padding: const EdgeInsets.fromLTRB(16, 16, 16, 96),
        children: [
          GlassCard(
            child: Row(
              children: [
                _kpi(l10n.familyOsOverdueFollowUps, '$overdue',
                    overdue > 0 ? Colors.orange : Colors.green),
                _kpi(l10n.familyOsUpcomingVisits, '${upcoming.length}',
                    Colors.redAccent),
                _kpi(l10n.familyMembers, '${_members.length}', Colors.blue),
              ],
            ),
          ),
          const SizedBox(height: 16),
          Text(l10n.familyOsUpcomingVisits,
              style: const TextStyle(
                  color: Colors.white,
                  fontSize: 16,
                  fontWeight: FontWeight.bold)),
          const SizedBox(height: 8),
          if (upcoming.isEmpty)
            GlassCard(
                child: Text(l10n.familyOsNoVisits,
                    style: TextStyle(
                        color: Colors.white.withValues(alpha: 0.5)))),
          ...upcoming.map((v) => Padding(
                padding: const EdgeInsets.only(bottom: 8),
                child: GlassCard(
                  padding: const EdgeInsets.all(12),
                  child: ListTile(
                    dense: true,
                    contentPadding: EdgeInsets.zero,
                    leading: const Icon(Icons.directions_walk,
                        color: Colors.redAccent, size: 20),
                    title: Text('${v['soulName'] ?? '—'}',
                        style: const TextStyle(
                            color: Colors.white,
                            fontWeight: FontWeight.w600,
                            fontSize: 14)),
                    subtitle: Text(
                        '${v['visitType'] ?? ''} • ${v['visitDate'] ?? ''}',
                        style: TextStyle(
                            color: Colors.white.withValues(alpha: 0.5),
                            fontSize: 11)),
                  ),
                ),
              )),
          const SizedBox(height: 16),
          Text(l10n.familyOsFollowUpList,
              style: const TextStyle(
                  color: Colors.white,
                  fontSize: 16,
                  fontWeight: FontWeight.bold)),
          const SizedBox(height: 8),
          if (followUps.isEmpty)
            GlassCard(
                child: Text(l10n.noRecentActivity,
                    style: TextStyle(
                        color: Colors.white.withValues(alpha: 0.5)))),
          ...followUps.map((v) => Padding(
                padding: const EdgeInsets.only(bottom: 8),
                child: GlassCard(
                  padding: const EdgeInsets.all(12),
                  child: ListTile(
                    dense: true,
                    contentPadding: EdgeInsets.zero,
                    leading: const Icon(Icons.update,
                        color: Colors.orange, size: 20),
                    title: Text('${v['soulName'] ?? '—'}',
                        style: const TextStyle(
                            color: Colors.white,
                            fontWeight: FontWeight.w600,
                            fontSize: 14)),
                    subtitle: Text(
                      '${l10n.familyOsNextAction}: ${v['nextActionDate'] ?? '—'}',
                      style: TextStyle(
                          color: Colors.white.withValues(alpha: 0.5),
                          fontSize: 11),
                    ),
                  ),
                ),
              )),
          const SizedBox(height: 16),
          Text(l10n.familyOsRecentReceptions,
              style: const TextStyle(
                  color: Colors.white,
                  fontSize: 16,
                  fontWeight: FontWeight.bold)),
          const SizedBox(height: 8),
          if (receptions.isEmpty)
            GlassCard(
                child: Text(l10n.noRecentActivity,
                    style: TextStyle(
                        color: Colors.white.withValues(alpha: 0.5)))),
          ...receptions.take(5).map((r) => Padding(
                padding: const EdgeInsets.only(bottom: 8),
                child: GlassCard(
                  padding: const EdgeInsets.all(12),
                  child: ListTile(
                    dense: true,
                    contentPadding: EdgeInsets.zero,
                    leading: const Icon(Icons.local_cafe_outlined,
                        color: Colors.greenAccent, size: 20),
                    title: Text(_soulName(r['soulId']),
                        style: const TextStyle(
                            color: Colors.white,
                            fontWeight: FontWeight.w600,
                            fontSize: 14)),
                    subtitle: Text('${r['receptionDate'] ?? ''}',
                        style: TextStyle(
                            color: Colors.white.withValues(alpha: 0.5),
                            fontSize: 11)),
                  ),
                ),
              )),
        ],
      ),
    );
  }

  Widget _kpi(String label, String value, Color color) {
    return Expanded(
      child: Column(
        children: [
          Text(value,
              style: TextStyle(
                  color: color, fontSize: 26, fontWeight: FontWeight.bold)),
          const SizedBox(height: 4),
          Text(label,
              textAlign: TextAlign.center,
              style: TextStyle(
                  color: Colors.white.withValues(alpha: 0.6), fontSize: 11)),
        ],
      ),
    );
  }

  Widget _buildVisits() {
    final l10n = AppLocalizations.of(context);
    return RefreshIndicator(
      onRefresh: _loadAll,
      child: _visits.isEmpty
          ? ListView(
              physics: const AlwaysScrollableScrollPhysics(),
              padding: const EdgeInsets.all(24),
              children: [
                const SizedBox(height: 48),
                Center(child: Text(l10n.familyOsNoVisits)),
              ],
            )
          : ListView.builder(
              padding: const EdgeInsets.fromLTRB(16, 16, 16, 96),
              itemCount: _visits.length + 1,
              itemBuilder: (context, i) {
                if (i == _visits.length) {
                  return Padding(
                    padding: const EdgeInsets.only(top: 8),
                    child: FilledButton.icon(
                      key: const Key('familyOsNewVisitBtn'),
                      style: FilledButton.styleFrom(
                          backgroundColor: Colors.redAccent),
                      icon: const Icon(Icons.add),
                      label: Text(l10n.familyOsNewVisit),
                      onPressed: _members.isEmpty ? null : _createVisitSheet,
                    ),
                  );
                }
                final v = _visits[i] as Map<String, dynamic>;
                final done = (v['status'] ?? '').toString() == 'COMPLETED';
                final cancelled =
                    (v['status'] ?? '').toString() == 'CANCELLED';
                return Padding(
                  padding: const EdgeInsets.only(bottom: 8),
                  child: GlassCard(
                    padding: const EdgeInsets.all(12),
                    child: Row(
                      children: [
                        Icon(
                          done
                              ? Icons.check_circle
                              : cancelled
                                  ? Icons.cancel
                                  : Icons.schedule,
                          color: done
                              ? Colors.green
                              : cancelled
                                  ? Colors.grey
                                  : Colors.orange,
                          size: 20,
                        ),
                        const SizedBox(width: 12),
                        Expanded(
                          child: Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              Text(
                                  v['subject']?.toString().isNotEmpty == true
                                      ? v['subject'].toString()
                                      : _soulName(v['soulId']),
                                  style: const TextStyle(
                                      color: Colors.white,
                                      fontWeight: FontWeight.w600,
                                      fontSize: 14)),
                              Text(
                                  '${v['visitType'] ?? ''} • ${v['visitDate'] ?? ''} • ${v['status'] ?? ''}',
                                  style: TextStyle(
                                      color: Colors.white.withValues(alpha: 0.5),
                                      fontSize: 11)),
                            ],
                          ),
                        ),
                        if (!done && !cancelled)
                          IconButton(
                            key: Key('familyOsVisitComplete-${v['id']}'),
                            icon: const Icon(Icons.check_circle_outline,
                                color: Colors.green),
                            tooltip: l10n.familyOsMarkDone,
                            onPressed: () => _completeVisit(v),
                          ),
                      ],
                    ),
                  ),
                );
              },
            ),
    );
  }

  Widget _buildMembers() {
    final l10n = AppLocalizations.of(context);
    return RefreshIndicator(
      onRefresh: _loadAll,
      child: _members.isEmpty
          ? ListView(
              physics: const AlwaysScrollableScrollPhysics(),
              padding: const EdgeInsets.all(24),
              children: [
                const SizedBox(height: 48),
                Center(child: Text(l10n.familyOsNoMembers)),
              ],
            )
          : ListView.builder(
              padding: const EdgeInsets.fromLTRB(16, 16, 16, 96),
              itemCount: _members.length,
              itemBuilder: (context, i) {
                final m = _members[i] as Map<String, dynamic>;
                final name =
                    '${m['prenom'] ?? ''} ${m['nom'] ?? ''}'.trim();
                final display = name.isEmpty ? '—' : name;
                return Padding(
                  padding: const EdgeInsets.only(bottom: 8),
                  child: GlassCard(
                    padding: const EdgeInsets.all(12),
                    child: Row(
                      children: [
                        CircleAvatar(
                          backgroundColor:
                              Colors.redAccent.withValues(alpha: 0.2),
                          child: Text(
                            display.isNotEmpty ? display[0] : '?',
                            style: const TextStyle(
                                color: Colors.redAccent,
                                fontWeight: FontWeight.bold),
                          ),
                        ),
                        const SizedBox(width: 12),
                        Expanded(
                          child: Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              Text(display,
                                  style: const TextStyle(
                                      color: Colors.white,
                                      fontWeight: FontWeight.w600,
                                      fontSize: 14)),
                              Text(
                                m['typeDisciple']?.toString() ?? '',
                                style: TextStyle(
                                    color:
                                        Colors.white.withValues(alpha: 0.5),
                                    fontSize: 11),
                              ),
                            ],
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

  Widget _buildJournal() {
    final l10n = AppLocalizations.of(context);
    return RefreshIndicator(
      onRefresh: _loadAll,
      child: _activities.isEmpty
          ? ListView(
              physics: const AlwaysScrollableScrollPhysics(),
              padding: const EdgeInsets.all(24),
              children: [
                const SizedBox(height: 48),
                Center(child: Text(l10n.noRecentActivity)),
              ],
            )
          : ListView.builder(
              padding: const EdgeInsets.fromLTRB(16, 16, 16, 96),
              itemCount: _activities.length + 1,
              itemBuilder: (context, i) {
                if (i == 0) {
                  return Padding(
                    padding: const EdgeInsets.only(bottom: 8),
                    child: Text(
                      '${l10n.recentActivity} ($_journalTotal)',
                      style: TextStyle(
                          color: Colors.white.withValues(alpha: 0.6),
                          fontSize: 12),
                    ),
                  );
                }
                final a = _activities[i - 1] as Map<String, dynamic>;
                final title =
                    a['title']?.toString().isNotEmpty == true
                        ? a['title'].toString()
                        : l10n.activity;
                return Padding(
                  padding: const EdgeInsets.only(bottom: 8),
                  child: GlassCard(
                    padding: const EdgeInsets.all(12),
                    child: Row(
                      children: [
                        Container(
                          width: 40,
                          height: 40,
                          decoration: BoxDecoration(
                            color: Colors.redAccent.withValues(alpha: 0.2),
                            borderRadius: BorderRadius.circular(10),
                          ),
                          child: const Icon(Icons.history,
                              color: Colors.redAccent, size: 20),
                        ),
                        const SizedBox(width: 12),
                        Expanded(
                          child: Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              Text(title,
                                  style: const TextStyle(
                                      color: Colors.white,
                                      fontWeight: FontWeight.w600,
                                      fontSize: 14)),
                              Text(
                                  '${a['activityType'] ?? ''} • ${a['activityDate'] ?? ''}',
                                  style: TextStyle(
                                      color:
                                          Colors.white.withValues(alpha: 0.5),
                                      fontSize: 11)),
                            ],
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

  Widget _buildBottomNav() {
    const routes = ['/dashboard', '/souls', '/reports/maker', '/profile'];
    return GlassBottomNav(
      currentIndex: _currentNavIndex,
      onTap: (i) {
        setState(() => _currentNavIndex = i);
        if (i < routes.length) context.go(routes[i]);
      },
    );
  }
}
