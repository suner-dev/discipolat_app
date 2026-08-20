import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:go_router/go_router.dart';
import '../../widgets/glass_theme.dart';
import '../../widgets/app_drawer.dart';
import '../../../data/services/api_service.dart';

/// Pipeline d'évangélisation avec vue Kanban.
/// Les âmes progressent de NOUVELLE_AME → LEADER en 11 étapes.
/// Chaque colonne montre les âmes de cette étape avec swipe pour avancer/reculer.
class EvangelismScreen extends StatefulWidget {
  const EvangelismScreen({super.key});

  @override
  State<EvangelismScreen> createState() => _EvangelismScreenState();
}

class _EvangelismScreenState extends State<EvangelismScreen> {
  final _apiService = ApiService();
  List<dynamic> _tracks = [];
  Map<String, dynamic>? _stats;
  bool _isLoading = true;
  int _selectedColumn = -1; // -1 = vue funnel, 0-10 = colonne Kanban
  String _searchQuery = '';

  static const _etapes = [
    'NOUVELLE_AME', 'PREMIER_CONTACT', 'VISITE', 'INVITATION', 'PREMIER_CULTE',
    'SUIVI', 'BAPTEME', 'DEPARTEMENT', 'FAMILLE', 'DISCIPOLAT', 'LEADER',
  ];

  static const _etapeLabels = {
    'NOUVELLE_AME': 'Nouvelle âme', 'PREMIER_CONTACT': '1er contact', 'VISITE': 'Visite',
    'INVITATION': 'Invitation', 'PREMIER_CULTE': '1er culte', 'SUIVI': 'Suivi',
    'BAPTEME': 'Baptême', 'DEPARTEMENT': 'Départ.', 'FAMILLE': 'Famille',
    'DISCIPOLAT': 'Discipolat', 'LEADER': 'Leader',
  };

  static const _etapeColors = {
    'NOUVELLE_AME': Colors.green, 'PREMIER_CONTACT': Colors.teal, 'VISITE': Colors.cyan,
    'INVITATION': Colors.lightBlue, 'PREMIER_CULTE': Colors.blue, 'SUIVI': Colors.indigo,
    'BAPTEME': Colors.purple, 'DEPARTEMENT': Colors.deepPurple, 'FAMILLE': Colors.pink,
    'DISCIPOLAT': Colors.red, 'LEADER': Colors.amber,
  };

  @override
  void initState() { super.initState(); _loadData(); }

  Future<void> _loadData() async {
    setState(() => _isLoading = true);
    try {
      final tracksRes = await _apiService.get('/evangelism', params: {'size': '100'});
      final statsRes = await _apiService.get('/evangelism/stats');
      if (mounted) {
        setState(() {
          _tracks = (tracksRes.data is Map ? tracksRes.data['content'] : tracksRes.data) as List<dynamic>? ?? [];
          _stats = statsRes.data as Map<String, dynamic>?;
          _isLoading = false;
        });
      }
    } catch (_) { if (mounted) setState(() => _isLoading = false); }
  }

  Future<void> _advanceTrack(String soulId) async {
    try {
      await _apiService.patch('/evangelism/$soulId', data: {'action': 'ADVANCE'});
      HapticFeedback.lightImpact();
      _loadData();
    } catch (_) {}
  }

  Future<void> _retreatTrack(String soulId) async {
    try {
      await _apiService.patch('/evangelism/$soulId', data: {'action': 'RETREAT'});
      HapticFeedback.lightImpact();
      _loadData();
    } catch (_) {}
  }

  int _etapeIndex(String etape) => _etapes.indexOf(etape);

  List<dynamic> _tracksForColumn(int colIndex) {
    final etape = _etapes[colIndex];
    return _tracks.where((t) {
      final track = t as Map<String, dynamic>;
      final trackEtape = track['etape']?.toString() ?? '';
      if (trackEtape != etape) return false;
      if (_searchQuery.isNotEmpty) {
        final nom = (track['soulNom']?.toString() ?? '').toLowerCase();
        if (!nom.contains(_searchQuery.toLowerCase())) return false;
      }
      return true;
    }).toList();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: Text('Évangélisation · ${_tracks.length} âmes'),
        actions: [
          IconButton(icon: const Icon(Icons.refresh), onPressed: _loadData),
        ],
      ),
      drawer: const AppDrawer(),
      body: _isLoading ? const ShimmerLoading(itemCount: 8) : _selectedColumn == -1
          ? _buildFunnelView()
          : _buildKanbanColumn(),
      bottomNavigationBar: _buildBottomNav(),
    );
  }

  // ── Funnel View ──────────────────────────────────────────────────

  Widget _buildFunnelView() {
    final funnel = _stats?['funnel'] as Map<String, dynamic>? ?? {};
    return RefreshIndicator(
      onRefresh: _loadData,
      child: ListView(
        padding: const EdgeInsets.all(12),
        children: [
          // Funnel header
          GlassCard(
            padding: const EdgeInsets.all(16),
            child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
              Row(children: [
                Icon(Icons.leak_remove, color: AppColors.primaryLight, size: 20),
                const SizedBox(width: 8),
                const Text('Funnel de conversion', style: TextStyle(color: Colors.white, fontWeight: FontWeight.w700, fontSize: 16)),
              ]),
              const SizedBox(height: 4),
              Text('Cliquez sur une étape pour voir les âmes', style: TextStyle(color: Colors.white.withValues(alpha: 0.4), fontSize: 11)),
            ]),
          ),
          const SizedBox(height: 12),
          // Funnel bars
          for (int i = 0; i < _etapes.length; i++)
            _funnelBar(i, funnel),
        ],
      ),
    );
  }

  Widget _funnelBar(int index, Map<String, dynamic> funnel) {
    final etape = _etapes[index];
    final count = (funnel[etape] as num?)?.toInt() ?? 0;
    final total = _tracks.length;
    final ratio = total > 0 ? count / total : 0.0;
    final color = _etapeColors[etape] ?? Colors.grey;

    return GestureDetector(
      onTap: () => setState(() => _selectedColumn = index),
      child: Container(
        margin: const EdgeInsets.only(bottom: 6),
        padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 10),
        decoration: BoxDecoration(
          color: color.withValues(alpha: 0.1),
          borderRadius: BorderRadius.circular(10),
          border: Border.all(color: color.withValues(alpha: 0.2)),
        ),
        child: Row(children: [
          Container(width: 28, height: 28, decoration: BoxDecoration(color: color.withValues(alpha: 0.2), borderRadius: BorderRadius.circular(6)),
            child: Center(child: Text('${index + 1}', style: TextStyle(color: color, fontSize: 11, fontWeight: FontWeight.bold)))),
          const SizedBox(width: 10),
          Expanded(child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
            Text(_etapeLabels[etape] ?? etape, style: const TextStyle(color: Colors.white, fontWeight: FontWeight.w600, fontSize: 13)),
            const SizedBox(height: 4),
            ClipRRect(borderRadius: BorderRadius.circular(3), child: LinearProgressIndicator(
              value: ratio, minHeight: 4, backgroundColor: Colors.white.withValues(alpha: 0.08),
              valueColor: AlwaysStoppedAnimation(color))),
          ])),
          const SizedBox(width: 10),
          Text('$count', style: TextStyle(color: color, fontWeight: FontWeight.bold, fontSize: 16)),
          Icon(Icons.chevron_right, color: Colors.white.withValues(alpha: 0.3), size: 18),
        ]),
      ),
    );
  }

  // ── Kanban Column ────────────────────────────────────────────────

  Widget _buildKanbanColumn() {
    final etape = _etapes[_selectedColumn];
    final color = _etapeColors[etape] ?? Colors.grey;
    final tracks = _tracksForColumn(_selectedColumn);

    return Column(children: [
      // Column header
      GlassCard(
        margin: const EdgeInsets.fromLTRB(12, 12, 12, 0),
        padding: const EdgeInsets.all(12),
        child: Column(children: [
          Row(children: [
            Container(width: 32, height: 32, decoration: BoxDecoration(color: color.withValues(alpha: 0.2), borderRadius: BorderRadius.circular(8)),
              child: Center(child: Text('${_selectedColumn + 1}', style: TextStyle(color: color, fontWeight: FontWeight.bold)))),
            const SizedBox(width: 10),
            Expanded(child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
              Text(_etapeLabels[etape] ?? etape, style: const TextStyle(color: Colors.white, fontWeight: FontWeight.w700, fontSize: 16)),
              Text('${tracks.length} âme${tracks.length > 1 ? 's' : ''}', style: TextStyle(color: Colors.white.withValues(alpha: 0.4), fontSize: 11)),
            ])),
            IconButton(icon: Icon(Icons.arrow_back, color: Colors.white.withValues(alpha: 0.5)), onPressed: () => setState(() => _selectedColumn = -1)),
          ]),
          // Search
          if (tracks.length > 5)
            Padding(padding: const EdgeInsets.only(top: 8), child: TextField(
              style: const TextStyle(color: Colors.white, fontSize: 12),
              onChanged: (v) => setState(() => _searchQuery = v),
              decoration: InputDecoration(
                hintText: 'Rechercher…', hintStyle: TextStyle(color: Colors.white.withValues(alpha: 0.3)),
                isDense: true, contentPadding: const EdgeInsets.symmetric(horizontal: 10, vertical: 6),
                border: OutlineInputBorder(borderRadius: BorderRadius.circular(8), borderSide: BorderSide(color: Colors.white.withValues(alpha: 0.1))),
                enabledBorder: OutlineInputBorder(borderRadius: BorderRadius.circular(8), borderSide: BorderSide(color: Colors.white.withValues(alpha: 0.1))),
                filled: true, fillColor: Colors.white.withValues(alpha: 0.05),
              ),
            )),
        ]),
      ),
      // Tracks list
      Expanded(child: tracks.isEmpty
          ? Center(child: Column(mainAxisAlignment: MainAxisAlignment.center, children: [
              Icon(Icons.south_east, color: Colors.white.withValues(alpha: 0.15), size: 48),
              const SizedBox(height: 12),
              Text('Aucune âme à cette étape', style: TextStyle(color: Colors.white.withValues(alpha: 0.5))),
            ]))
          : ListView.builder(
              padding: const EdgeInsets.all(12),
              itemCount: tracks.length,
              itemBuilder: (_, i) => _trackCard(tracks[i] as Map<String, dynamic>),
            )),
    ]);
  }

  Widget _trackCard(Map<String, dynamic> track) {
    final soulId = track['soulId']?.toString() ?? '';
    final soulNom = track['soulNom']?.toString() ?? '';
    final etape = track['etape']?.toString() ?? '';
    final dateEtape = track['dateEtape']?.toString() ?? '';
    final etapeIdx = _etapeIndex(etape);
    final prevLabel = etapeIdx > 0 ? _etapeLabels[_etapes[etapeIdx - 1]] : null;
    final nextLabel = etapeIdx < _etapes.length - 1 ? _etapeLabels[_etapes[etapeIdx + 1]] : null;

    return Dismissible(
      key: Key('track-$soulId'),
      confirmDismiss: (dir) async {
        if (dir == DismissDirection.startToEnd && nextLabel != null) _advanceTrack(soulId);
        if (dir == DismissDirection.endToStart && prevLabel != null) _retreatTrack(soulId);
        return false;
      },
      background: Container(alignment: Alignment.centerLeft, padding: const EdgeInsets.only(left: 20),
        margin: const EdgeInsets.only(bottom: 8), decoration: BoxDecoration(color: Colors.green.withValues(alpha: 0.15), borderRadius: BorderRadius.circular(12)),
        child: Row(children: [const Icon(Icons.arrow_forward, color: Colors.green), const SizedBox(width: 8),
          Text('Avancer → ${nextLabel ?? ""}', style: const TextStyle(color: Colors.green, fontWeight: FontWeight.w600))])),
      secondaryBackground: Container(alignment: Alignment.centerRight, padding: const EdgeInsets.only(right: 20),
        margin: const EdgeInsets.only(bottom: 8), decoration: BoxDecoration(color: Colors.orange.withValues(alpha: 0.15), borderRadius: BorderRadius.circular(12)),
        child: Row(mainAxisAlignment: MainAxisAlignment.end, children: [
          Text('← Reculer $prevLabel', style: const TextStyle(color: Colors.orange, fontWeight: FontWeight.w600)),
          const SizedBox(width: 8), const Icon(Icons.arrow_back, color: Colors.orange)])),
      child: GlassCard(
        margin: const EdgeInsets.only(bottom: 8),
        padding: const EdgeInsets.all(12),
        onTap: () => context.go('/souls/$soulId'),
        child: Row(children: [
          // Avatar
          Container(width: 38, height: 38, decoration: BoxDecoration(
            gradient: LinearGradient(colors: [_etapeColors[etape] ?? Colors.grey, (_etapeColors[etape] ?? Colors.grey).withValues(alpha: 0.7)]),
            borderRadius: BorderRadius.circular(8)),
            child: Center(child: Text(soulNom.isNotEmpty ? soulNom[0].toUpperCase() : '?',
              style: const TextStyle(color: Colors.white, fontWeight: FontWeight.bold, fontSize: 14)))),
          const SizedBox(width: 10),
          Expanded(child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
            Text(soulNom, style: const TextStyle(color: Colors.white, fontWeight: FontWeight.w600, fontSize: 13)),
            if (dateEtape.isNotEmpty)
              Text('Depuis $dateEtape', style: TextStyle(color: Colors.white.withValues(alpha: 0.3), fontSize: 10)),
          ])),
          // Advance/Retreat buttons
          if (nextLabel != null)
            GestureDetector(onTap: () => _advanceTrack(soulId),
              child: Container(padding: const EdgeInsets.all(6), decoration: BoxDecoration(
                color: Colors.green.withValues(alpha: 0.1), borderRadius: BorderRadius.circular(6)),
                child: const Icon(Icons.arrow_forward, color: Colors.green, size: 16))),
        ]),
      ),
    );
  }

  // ── Bottom Navigation ────────────────────────────────────────────

  Widget _buildBottomNav() {
    return Container(
      decoration: BoxDecoration(color: Colors.black.withValues(alpha: 0.3), border: Border(top: BorderSide(color: Colors.white.withValues(alpha: 0.06)))),
      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
      child: SafeArea(child: Row(children: [
        _navButton('Vue globale', Icons.leak_remove, -1),
        const SizedBox(width: 4),
        for (int i = 0; i < _etapes.length; i++)
          Expanded(child: _navButton('${i + 1}', Icons.circle, i, small: true)),
      ])),
    );
  }

  Widget _navButton(String label, IconData icon, int column, {bool small = false}) {
    final isActive = _selectedColumn == column;
    final color = column >= 0 ? (_etapeColors[_etapes[column]] ?? Colors.grey) : AppColors.primary;
    return GestureDetector(
      onTap: () => setState(() { _selectedColumn = column; _searchQuery = ''; }),
      child: Container(
        padding: EdgeInsets.symmetric(horizontal: small ? 4 : 8, vertical: small ? 4 : 6),
        decoration: BoxDecoration(
          color: isActive ? color.withValues(alpha: 0.2) : Colors.transparent,
          borderRadius: BorderRadius.circular(8),
          border: isActive ? Border.all(color: color.withValues(alpha: 0.4)) : null),
        child: small
            ? Text(label, style: TextStyle(color: isActive ? color : Colors.white.withValues(alpha: 0.4), fontSize: 9, fontWeight: FontWeight.bold))
            : Row(mainAxisAlignment: MainAxisAlignment.center, children: [
                Icon(icon, color: color, size: 14), const SizedBox(width: 4),
                Text(label, style: TextStyle(color: color, fontSize: 10, fontWeight: FontWeight.w600))]),
      ),
    );
  }
}
