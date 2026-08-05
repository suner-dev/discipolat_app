import 'package:flutter/material.dart';
import '../../widgets/glass_theme.dart';
import '../../widgets/app_drawer.dart';
import '../../../data/services/api_service.dart';

class EventsListScreen extends StatefulWidget {
  const EventsListScreen({super.key});

  @override
  State<EventsListScreen> createState() => _EventsListScreenState();
}

class _EventsListScreenState extends State<EventsListScreen> {
  final _apiService = ApiService();
  List<dynamic> _events = [];
  bool _isLoading = true;
  String _filter = 'TOUS';

  @override
  void initState() {
    super.initState();
    _loadData();
  }

  Future<void> _loadData() async {
    setState(() => _isLoading = true);
    try {
      final res = await _apiService.get('/events', params: {'size': '50'});
      if (mounted) {
        setState(() {
          _events = (res.data is Map ? res.data['content'] : res.data) as List<dynamic>? ?? [];
          _isLoading = false;
        });
      }
    } catch (_) {
      if (mounted) setState(() => _isLoading = false);
    }
  }

  Color _typeColor(String? type) {
    switch (type) {
      case 'CULTE': return Colors.purple;
      case 'REUNION': return Colors.blue;
      case 'SEMINAIRE': return Colors.teal;
      case 'VISITE': return Colors.green;
      case 'EVANGELISATION': return Colors.orange;
      case 'FORMATION': return Colors.indigo;
      case 'ANNIVERSAIRE': return Colors.pink;
      case 'CELEBRATION': return Colors.amber;
      default: return Colors.grey;
    }
  }

  IconData _typeIcon(String? type) {
    switch (type) {
      case 'CULTE': return Icons.church;
      case 'REUNION': return Icons.groups;
      case 'SEMINAIRE': return Icons.school;
      case 'VISITE': return Icons.map;
      case 'EVANGELISATION': return Icons.share;
      case 'FORMATION': return Icons.menu_book;
      case 'ANNIVERSAIRE': return Icons.cake;
      case 'CELEBRATION': return Icons.celebration;
      default: return Icons.event;
    }
  }

  @override
  Widget build(BuildContext context) {
    final upcoming = _events.where((e) => (e as Map)['statut'] == 'PLANIFIE').length;
    final termine = _events.where((e) => (e as Map)['statut'] == 'TERMINE').length;

    return Scaffold(
      appBar: AppBar(
        title: const Text('Événements'),
        actions: [
          IconButton(icon: const Icon(Icons.add), onPressed: () {}),
        ],
      ),
      drawer: const AppDrawer(),
      body: _isLoading
          ? const ShimmerLoading(itemCount: 4)
          : RefreshIndicator(
              onRefresh: _loadData,
              child: ListView(
                padding: const EdgeInsets.all(12),
                children: [
                  // Stats
                  Row(
                    children: [
                      _statMini('Total', '${_events.length}', Colors.blue),
                      const SizedBox(width: 8),
                      _statMini('À venir', '$upcoming', Colors.green),
                      const SizedBox(width: 8),
                      _statMini('Terminés', '$termine', Colors.grey),
                    ],
                  ),
                  const SizedBox(height: 12),
                  // Filter chips
                  SingleChildScrollView(
                    scrollDirection: Axis.horizontal,
                    child: Row(
                      children: ['TOUS', 'PLANIFIE', 'EN_COURS', 'TERMINE'].map((f) {
                        final isActive = _filter == f;
                        final label = f == 'TOUS' ? 'Tous' : f == 'PLANIFIE' ? 'À venir' : f == 'EN_COURS' ? 'En cours' : 'Terminés';
                        return Padding(
                          padding: const EdgeInsets.only(right: 6),
                          child: ChoiceChip(
                            label: Text(label, style: TextStyle(color: isActive ? Colors.white : Colors.white.withValues(alpha: 0.6), fontSize: 12)),
                            selected: isActive,
                            onSelected: (_) => setState(() => _filter = f),
                            selectedColor: Colors.blue,
                            backgroundColor: Colors.white.withValues(alpha: 0.06),
                            side: BorderSide.none,
                          ),
                        );
                      }).toList(),
                    ),
                  ),
                  const SizedBox(height: 12),
                  // Events list
                  if (_filtered.isEmpty)
                    GlassCard(
                      padding: const EdgeInsets.all(32),
                      child: Column(
                        children: [
                          Icon(Icons.event_outlined, size: 48, color: Colors.white.withValues(alpha: 0.3)),
                          const SizedBox(height: 12),
                          Text('Aucun événement', style: TextStyle(color: Colors.white.withValues(alpha: 0.5))),
                        ],
                      ),
                    )
                  else
                    ..._filtered.map((e) {
                      final event = e as Map;
                      final titre = event['titre'] ?? 'Événement';
                      final type = event['typeEvenement'] ?? 'AUTRE';
                      final lieu = event['lieu'] ?? '';
                      final statut = event['statut'] ?? 'PLANIFIE';
                      final dateStr = event['dateDebut']?.toString().substring(0, 16).replaceAll('T', ' ') ?? '';
                      final nbInscrits = event['nbInscrits'] ?? 0;
                      final limitePlaces = event['limitePlaces'];
                      final desc = event['description'] ?? '';

                      return GlassCard(
                        margin: const EdgeInsets.only(bottom: 8),
                        padding: const EdgeInsets.all(14),
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Row(
                              children: [
                                Container(
                                  padding: const EdgeInsets.all(8),
                                  decoration: BoxDecoration(
                                    color: _typeColor(type).withValues(alpha: 0.15),
                                    borderRadius: BorderRadius.circular(10),
                                  ),
                                  child: Icon(_typeIcon(type), color: _typeColor(type), size: 20),
                                ),
                                const SizedBox(width: 10),
                                Expanded(
                                  child: Column(
                                    crossAxisAlignment: CrossAxisAlignment.start,
                                    children: [
                                      Text(titre, style: const TextStyle(color: Colors.white, fontWeight: FontWeight.w600, fontSize: 14)),
                                      Text(type, style: TextStyle(color: _typeColor(type), fontSize: 11)),
                                    ],
                                  ),
                                ),
                                StatusBadge(
                                  label: statut,
                                  color: statut == 'TERMINE' ? Colors.green : statut == 'EN_COURS' ? Colors.amber : Colors.blue,
                                ),
                              ],
                            ),
                            if (desc.toString().isNotEmpty) ...[
                              const SizedBox(height: 8),
                              Text(desc, style: TextStyle(color: Colors.white.withValues(alpha: 0.6), fontSize: 12), maxLines: 2, overflow: TextOverflow.ellipsis),
                            ],
                            const SizedBox(height: 8),
                            Row(
                              children: [
                                Icon(Icons.access_time, size: 12, color: Colors.white.withValues(alpha: 0.4)),
                                const SizedBox(width: 3),
                                Text(dateStr, style: TextStyle(color: Colors.white.withValues(alpha: 0.5), fontSize: 11)),
                              ],
                            ),
                            if (lieu.toString().isNotEmpty) ...[
                              const SizedBox(height: 4),
                              Row(
                                children: [
                                  Icon(Icons.location_on, size: 12, color: Colors.white.withValues(alpha: 0.4)),
                                  const SizedBox(width: 3),
                                  Text(lieu, style: TextStyle(color: Colors.white.withValues(alpha: 0.5), fontSize: 11)),
                                ],
                              ),
                            ],
                            if (limitePlaces != null) ...[
                              const SizedBox(height: 4),
                              Row(
                                children: [
                                  Icon(Icons.people, size: 12, color: Colors.white.withValues(alpha: 0.4)),
                                  const SizedBox(width: 3),
                                  Text('$nbInscrits/$limitePlaces inscrits', style: TextStyle(color: Colors.white.withValues(alpha: 0.5), fontSize: 11)),
                                ],
                              ),
                            ],
                          ],
                        ),
                      );
                    }),
                ],
              ),
            ),
    );
  }

  List<dynamic> get _filtered {
    if (_filter == 'TOUS') return _events;
    return _events.where((e) => (e as Map)['statut'] == _filter).toList();
  }

  Widget _statMini(String label, String value, Color color) {
    return Expanded(
      child: GlassCard(
        padding: const EdgeInsets.all(10),
        child: Column(
          children: [
            Text(value, style: TextStyle(color: color, fontSize: 18, fontWeight: FontWeight.bold)),
            Text(label, style: TextStyle(color: Colors.white.withValues(alpha: 0.5), fontSize: 10)),
          ],
        ),
      ),
    );
  }
}
