import 'package:flutter/material.dart';
import 'package:intl/intl.dart';
import '../../../data/services/api_service.dart';
import '../../widgets/glass_theme.dart';
import '../../widgets/app_drawer.dart';

class AuditScreen extends StatefulWidget {
  const AuditScreen({super.key, this.apiService});

  /// Permet d'injecter un ApiService mocké dans les tests widget.
  final ApiService? apiService;

  @override
  State<AuditScreen> createState() => _AuditScreenState();
}

class _AuditScreenState extends State<AuditScreen> {
  late final ApiService _apiService = widget.apiService ?? ApiService();
  List<dynamic> _entries = [];
  bool _isLoading = true;
  int _page = 0;
  int _totalPages = 0;
  String _entityFilter = '';

  static const _entityLabels = {
    '': 'Toutes les entités',
    'USER': 'Utilisateurs',
    'FAMILY': 'Familles',
    'SOUL': 'Âmes',
    'REPORT': 'Rapports',
    'DEPARTMENT': 'Départements',
  };

  Color _actionColor(String action) {
    if (action.contains('DELETE') || action.contains('SUPPR')) return Colors.red;
    if (action.contains('CREATE') || action.contains('CREATION')) return Colors.green;
    if (action.contains('UPDATE') || action.contains('MODIF')) return Colors.blue;
    return Colors.grey;
  }

  @override
  void initState() { super.initState(); _loadData(); }

  Future<void> _loadData() async {
    setState(() => _isLoading = true);
    try {
      final params = <String, dynamic>{'page': '$_page', 'size': '20'};
      if (_entityFilter.isNotEmpty) params['entiteType'] = _entityFilter;
      final res = await _apiService.get('/audit', params: params);
      if (mounted) {
        _entries = (res.data['content'] as List?) ?? [];
        _totalPages = (res.data['totalPages'] as int?) ?? 0;
        setState(() => _isLoading = false);
      }
    } catch (_) { if (mounted) setState(() => _isLoading = false); }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Journal d\'audit'),
        actions: [
          IconButton(icon: const Icon(Icons.refresh), onPressed: () { _page = 0; _loadData(); }),
        ],
      ),
      drawer: const AppDrawer(),
      body: Column(
        children: [
          // Entity filter
          Container(
            padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
            child: DropdownButtonFormField<String>(
              value: _entityFilter.isEmpty ? '' : _entityFilter,
              dropdownColor: const Color(0xFF111827),
              decoration: InputDecoration(
                prefixIcon: Icon(Icons.filter_list, color: Colors.white.withValues(alpha: 0.4), size: 20),
                filled: true,
              ),
              items: _entityLabels.entries.map((e) =>
                DropdownMenuItem(value: e.key, child: Text(e.value)),
              ).toList(),
              onChanged: (v) { _entityFilter = v ?? ''; _page = 0; _loadData(); },
            ),
          ),

          // Entries list
          Expanded(
            child: _isLoading
                ? const ShimmerLoading(itemCount: 6)
                : RefreshIndicator(
                    onRefresh: () async { _page = 0; await _loadData(); },
                    child: _entries.isEmpty
                        ? ListView(
                            children: [
                              SizedBox(height: MediaQuery.of(context).size.height * 0.2),
                              Center(
                                child: Column(children: [
                                  Icon(Icons.history, size: 48, color: Colors.white.withValues(alpha: 0.15)),
                                  const SizedBox(height: 12),
                                  Text('Aucune entrée d\'audit',
                                      style: TextStyle(color: Colors.white.withValues(alpha: 0.4), fontSize: 16)),
                                ]),
                              ),
                            ],
                          )
                        : ListView.builder(
                            padding: const EdgeInsets.symmetric(horizontal: 16),
                            itemCount: _entries.length + 1,
                            itemBuilder: (context, index) {
                              if (index == _entries.length) {
                                return _buildPagination();
                              }
                              final entry = _entries[index] as Map<String, dynamic>;
                              return _buildEntry(entry);
                            },
                          ),
                  ),
          ),
        ],
      ),
    );
  }

  Widget _buildEntry(Map<String, dynamic> entry) {
    final action = (entry['action'] as String?) ?? '—';
    final entityType = (entry['entiteType'] as String?) ?? '—';
    final dateStr = entry['createdAt'] as String?;
    final email = entry['emailUtilisateur'] as String?;
    final userId = entry['utilisateurId'] as String?;
    final details = entry['details'] as String?;
    final color = _actionColor(action);

    String dateLabel = '—';
    if (dateStr != null) {
      try {
        final date = DateTime.parse(dateStr);
        dateLabel = DateFormat('d MMM yyyy HH:mm', 'fr_FR').format(date);
      } catch (_) { dateLabel = dateStr; }
    }

    return GlassCard(
      margin: const EdgeInsets.only(bottom: 6),
      padding: const EdgeInsets.all(10),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          // Left accent
          Container(
            width: 3, height: 48,
            decoration: BoxDecoration(
              borderRadius: BorderRadius.circular(2),
              color: color,
              boxShadow: [BoxShadow(color: color.withValues(alpha: 0.4), blurRadius: 4)],
            ),
          ),
          const SizedBox(width: 10),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Row(children: [
                  _badge(color, action),
                  const SizedBox(width: 6),
                  Text(dateLabel,
                      style: TextStyle(color: Colors.white.withValues(alpha: 0.3), fontSize: 9)),
                ]),
                const SizedBox(height: 4),
                Row(children: [
                  Icon(Icons.person, size: 10, color: Colors.white.withValues(alpha: 0.3)),
                  const SizedBox(width: 4),
                  Text(email ?? userId?.substring(0, 8) ?? '—',
                      style: TextStyle(color: Colors.white.withValues(alpha: 0.5), fontSize: 11)),
                  const SizedBox(width: 8),
                  Icon(Icons.category, size: 10, color: Colors.white.withValues(alpha: 0.3)),
                  const SizedBox(width: 4),
                  Text(entityType,
                      style: TextStyle(color: Colors.white.withValues(alpha: 0.5), fontSize: 11)),
                ]),
                if (details != null && details.isNotEmpty)
                  Padding(
                    padding: const EdgeInsets.only(top: 4),
                    child: Text(details,
                        style: TextStyle(color: Colors.white.withValues(alpha: 0.4), fontSize: 10),
                        maxLines: 2, overflow: TextOverflow.ellipsis),
                  ),
              ],
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildPagination() {
    if (_totalPages <= 1) return const SizedBox(height: 80);
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 16),
      child: Row(
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          IconButton(
            icon: const Icon(Icons.chevron_left, color: Colors.white54),
            onPressed: _page > 0 ? () { setState(() => _page--); _loadData(); } : null,
          ),
          Container(
            padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 6),
            decoration: BoxDecoration(
              color: Colors.white.withValues(alpha: 0.06),
              borderRadius: BorderRadius.circular(8),
            ),
            child: Text('Page ${_page + 1} / $_totalPages',
                style: TextStyle(color: Colors.white.withValues(alpha: 0.5), fontSize: 11)),
          ),
          IconButton(
            icon: const Icon(Icons.chevron_right, color: Colors.white54),
            onPressed: _page < _totalPages - 1 ? () { setState(() => _page++); _loadData(); } : null,
          ),
        ],
      ),
    );
  }

  Widget _badge(Color color, String label) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 6, vertical: 2),
      decoration: BoxDecoration(color: color.withValues(alpha: 0.15), borderRadius: BorderRadius.circular(6)),
      child: Text(label, style: TextStyle(color: color, fontSize: 8, fontWeight: FontWeight.w600)),
    );
  }
}
