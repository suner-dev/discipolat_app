import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../../data/services/api_service.dart';

class AiDashboardScreen extends ConsumerStatefulWidget {
  const AiDashboardScreen({super.key});
  @override
  ConsumerState<AiDashboardScreen> createState() => _AiDashboardScreenState();
}

class _AiDashboardScreenState extends ConsumerState<AiDashboardScreen> {
  Map<String, dynamic>? _summary;
  Map<String, dynamic>? _narrative;
  bool _isLoading = true;

  @override
  void initState() {
    super.initState();
    _loadData();
  }

  Future<void> _loadData() async {
    try {
      final api = ref.read(apiServiceProvider);
      final results = await Future.wait([
        api.get('/ai/module/summary').catchError((_) => {}),
        api.get('/ai/module/kpi-narrative').catchError((_) => {}),
      ]);
      if (mounted) {
        setState(() {
          _summary = results[0] is Map ? results[0] as Map<String, dynamic> : null;
          _narrative = results[1] is Map ? results[1] as Map<String, dynamic> : null;
          _isLoading = false;
        });
      }
    } catch (e) {
      if (mounted) setState(() => _isLoading = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Intelligence Artificielle'),
        actions: [IconButton(icon: const Icon(Icons.refresh), onPressed: _loadData)],
      ),
      body: _isLoading
          ? const Center(child: CircularProgressIndicator())
          : RefreshIndicator(onRefresh: _loadData, child: _buildContent()),
    );
  }

  Widget _buildContent() {
    return ListView(
      padding: const EdgeInsets.all(16),
      children: [
        if (_narrative?['headline'] != null)
          Card(
            color: Colors.indigo,
            child: Padding(
              padding: const EdgeInsets.all(16),
              child: Text(_narrative!['headline'],
                style: const TextStyle(color: Colors.white, fontSize: 16, fontWeight: FontWeight.bold)),
            ),
          ),
        const SizedBox(height: 16),
        Row(children: [
          _statCard('Âmes', '${_summary?['totalSouls'] ?? '—'}', Icons.people, Colors.indigo),
          const SizedBox(width: 12),
          _statCard('Familles', '${_summary?['totalFamilies'] ?? '—'}', Icons.family_restroom, Colors.purple),
        ]),
        const SizedBox(height: 16),
        if (_narrative?['highlights'] != null) ...[
          const Text('Points forts', style: TextStyle(fontSize: 16, fontWeight: FontWeight.bold)),
          const SizedBox(height: 8),
          ...(_narrative!['highlights'] as List).map((h) => ListTile(
            leading: const Icon(Icons.check_circle, color: Colors.green, size: 20),
            title: Text(h.toString(), style: const TextStyle(fontSize: 14)),
            dense: true, contentPadding: EdgeInsets.zero,
          )),
        ],
        const SizedBox(height: 16),
        if (_narrative?['recommendations'] != null) ...[
          const Text('Recommandations', style: TextStyle(fontSize: 16, fontWeight: FontWeight.bold)),
          const SizedBox(height: 8),
          ...(_narrative!['recommendations'] as List).map((r) => ListTile(
            leading: const Text('→', style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold)),
            title: Text(r.toString(), style: const TextStyle(fontSize: 14)),
            dense: true, contentPadding: EdgeInsets.zero,
          )),
        ],
        const SizedBox(height: 24),
        const Text('Actions IA', style: TextStyle(fontSize: 16, fontWeight: FontWeight.bold)),
        const SizedBox(height: 8),
        ListTile(
          leading: const Icon(Icons.trending_up, color: Colors.indigo),
          title: const Text('Prédictions'),
          subtitle: const Text('Croissance, risque, engagement'),
          trailing: const Icon(Icons.chevron_right),
          onTap: () => Navigator.pushNamed(context, '/ai/predictions'),
        ),
        ListTile(
          leading: const Icon(Icons.family_restroom, color: Colors.purple),
          title: const Text('Cohésion Familiale'),
          trailing: const Icon(Icons.chevron_right),
          onTap: () => Navigator.pushNamed(context, '/ai/family-cohesion'),
        ),
        ListTile(
          leading: const Icon(Icons.menu_book, color: Colors.amber),
          title: const Text('Assistant Sermon'),
          trailing: const Icon(Icons.chevron_right),
          onTap: () => Navigator.pushNamed(context, '/ai/sermon'),
        ),
      ],
    );
  }

  Widget _statCard(String label, String value, IconData icon, Color color) {
    return Expanded(child: Card(
      child: Padding(padding: const EdgeInsets.all(16), child: Column(children: [
        Icon(icon, color: color, size: 28),
        const SizedBox(height: 8),
        Text(value, style: const TextStyle(fontSize: 24, fontWeight: FontWeight.bold)),
        Text(label, style: TextStyle(fontSize: 12, color: Colors.grey[600])),
      ])),
    ));
  }
}
