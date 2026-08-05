import 'package:flutter/material.dart';
import '../../widgets/glass_theme.dart';
import '../../widgets/app_drawer.dart';
import '../../../data/services/api_service.dart';

class MapScreen extends StatefulWidget {
  const MapScreen({super.key});

  @override
  State<MapScreen> createState() => _MapScreenState();
}

class _MapScreenState extends State<MapScreen> {
  final _apiService = ApiService();
  List<dynamic> _points = [];
  String _filter = 'TOUS';
  bool _isLoading = true;

  @override
  void initState() {
    super.initState();
    _loadData();
  }

  Future<void> _loadData() async {
    setState(() => _isLoading = true);
    try {
      final res = await _apiService.get('/map/points');
      if (mounted) {
        setState(() {
          _points = res.data as List<dynamic>? ?? [];
          _isLoading = false;
        });
      }
    } catch (_) {
      if (mounted) setState(() => _isLoading = false);
    }
  }

  List<dynamic> get _filtered {
    if (_filter == 'TOUS') return _points;
    return _points.where((p) => (p as Map)['type'] == _filter).toList();
  }

  Set<String> get _zones {
    return _points.map((p) => (p as Map)['zone']?.toString() ?? 'Non assigné').toSet();
  }

  @override
  Widget build(BuildContext context) {
    final souls = _points.where((p) => (p as Map)['type'] == 'SOUL').length;
    final families = _points.where((p) => (p as Map)['type'] == 'FAMILY').length;

    return Scaffold(
      appBar: AppBar(title: const Text('Cartographie')),
      drawer: const AppDrawer(),
      body: _isLoading
          ? const ShimmerLoading(itemCount: 3)
          : RefreshIndicator(
              onRefresh: _loadData,
              child: ListView(
                padding: const EdgeInsets.all(12),
                children: [
                  // Stats
                  Row(
                    children: [
                      _statMini('Disciples', '$souls', Colors.red),
                      const SizedBox(width: 8),
                      _statMini('Familles', '$families', Colors.purple),
                      const SizedBox(width: 8),
                      _statMini('Zones', '${_zones.length}', Colors.blue),
                    ],
                  ),
                  const SizedBox(height: 12),
                  // Filters
                  SingleChildScrollView(
                    scrollDirection: Axis.horizontal,
                    child: Row(
                      children: ['TOUS', 'SOUL', 'FAMILY'].map((f) {
                        final isActive = _filter == f;
                        return Padding(
                          padding: const EdgeInsets.only(right: 6),
                          child: ChoiceChip(
                            label: Text(f == 'TOUS' ? 'Tous' : f == 'SOUL' ? 'Disciples' : 'Familles', style: TextStyle(color: isActive ? Colors.white : Colors.white.withValues(alpha: 0.6), fontSize: 12)),
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
                  // Zone legend
                  if (_zones.isNotEmpty) ...[
                    Wrap(
                      spacing: 6,
                      runSpacing: 6,
                      children: _zones.map((z) => Container(
                        padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 3),
                        decoration: BoxDecoration(
                          color: Colors.white.withValues(alpha: 0.06),
                          borderRadius: BorderRadius.circular(12),
                        ),
                        child: Text(z, style: TextStyle(color: Colors.white.withValues(alpha: 0.6), fontSize: 10)),
                      )).toList(),
                    ),
                    const SizedBox(height: 12),
                  ],
                  // Points list
                  if (_filtered.isEmpty)
                    GlassCard(
                      padding: const EdgeInsets.all(32),
                      child: Column(
                        children: [
                          Icon(Icons.map_outlined, size: 48, color: Colors.white.withValues(alpha: 0.3)),
                          const SizedBox(height: 12),
                          Text('Aucun point', style: TextStyle(color: Colors.white.withValues(alpha: 0.5))),
                        ],
                      ),
                    )
                  else
                    ..._filtered.map((p) {
                      final point = p as Map<String, dynamic>;
                      final type = point['type'] ?? 'SOUL';
                      final nom = point['nom'] ?? '—';
                      final zone = point['zone'] ?? '—';
                      final lat = point['latitude'];
                      final lng = point['longitude'];
                      final isSoul = type == 'SOUL';
                      return GlassCard(
                        padding: const EdgeInsets.all(12),
                        child: Row(
                          children: [
                            Container(
                              width: 40, height: 40,
                              decoration: BoxDecoration(
                                color: (isSoul ? Colors.red : Colors.purple).withValues(alpha: 0.15),
                                borderRadius: BorderRadius.circular(10),
                              ),
                              child: Icon(
                                isSoul ? Icons.person : Icons.family_restroom,
                                color: isSoul ? Colors.red : Colors.purple,
                                size: 20,
                              ),
                            ),
                            const SizedBox(width: 12),
                            Expanded(
                              child: Column(
                                crossAxisAlignment: CrossAxisAlignment.start,
                                children: [
                                  Text(nom, style: const TextStyle(color: Colors.white, fontWeight: FontWeight.w500, fontSize: 14)),
                                  Row(
                                    children: [
                                      Icon(Icons.location_on, size: 12, color: Colors.white.withValues(alpha: 0.4)),
                                      const SizedBox(width: 3),
                                      Text(zone, style: TextStyle(color: Colors.white.withValues(alpha: 0.5), fontSize: 11)),
                                    ],
                                  ),
                                ],
                              ),
                            ),
                            if (lat != null && lng != null)
                              Text(
                                '${lat.toStringAsFixed(3)}, ${lng.toStringAsFixed(3)}',
                                style: TextStyle(color: Colors.white.withValues(alpha: 0.3), fontSize: 10),
                              ),
                          ],
                        ),
                      );
                    }),
                ],
              ),
            ),
    );
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
