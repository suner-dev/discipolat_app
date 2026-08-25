import 'package:flutter/material.dart';
import '../../../data/services/api_service.dart';
import '../../../core/format.dart';

/// Plan de succession — branché sur GET /api/v1/succession.
class SuccessionScreen extends StatefulWidget {
  const SuccessionScreen({super.key, this.apiService});
  final ApiService? apiService;

  @override
  State<SuccessionScreen> createState() => _SuccessionScreenState();
}

class _SuccessionScreenState extends State<SuccessionScreen> {
  late final ApiService _api = widget.apiService ?? ApiService();
  List<dynamic> _items = [];
  bool _loading = true;
  String? _error;

  @override
  void initState() { super.initState(); _load(); }

  Future<void> _load() async {
    setState(() { _loading = true; _error = null; });
    try {
      final res = await _api.get('/succession');
      final d = res.data;
      setState(() { _items = d is List ? d : <dynamic>[]; _loading = false; });
    } catch (_) {
      setState(() { _error = 'Impossible de charger les plans de succession.'; _loading = false; });
    }
  }

  Color _readinessColor(String? r) {
    switch (r) {
      case 'PRÊT': return Colors.green;
      case 'INTERMÉDIAIRE': return Colors.orange;
      default: return Colors.blueGrey;
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('👑 Succession'), backgroundColor: Colors.brown, foregroundColor: Colors.white),
      body: _loading
          ? const Center(child: CircularProgressIndicator())
          : _error != null
              ? Center(child: Column(mainAxisSize: MainAxisSize.min, children: [
                  Padding(padding: const EdgeInsets.all(24), child: Text(_error!, textAlign: TextAlign.center)),
                  ElevatedButton(onPressed: _load, child: const Text('Réessayer')),
                ]))
              : _items.isEmpty
                  ? const Center(child: Text('Aucun plan de succession.'))
                  : RefreshIndicator(
                      onRefresh: _load,
                      child: ListView.builder(
                        padding: const EdgeInsets.all(16),
                        itemCount: _items.length,
                        itemBuilder: (context, i) {
                          final p = _items[i] as Map<String, dynamic>;
                          final readiness = p['readiness']?.toString() ?? 'DÉBUTANT';
                          return Card(
                            margin: const EdgeInsets.only(bottom: 10),
                            child: ListTile(
                              leading: CircleAvatar(backgroundColor: _readinessColor(readiness).withValues(alpha: .15), child: Icon(Icons.workspace_premium, color: _readinessColor(readiness), size: 20)),
                              title: Text(p['rôleCible']?.toString() ?? 'Rôle', style: const TextStyle(fontWeight: FontWeight.w600)),
                              subtitle: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
                                Text('Candidat #${(p['candidatId'] ?? '').toString().substring(0, (p['candidatId'] ?? '').toString().length.clamp(0, 8))}', style: const TextStyle(fontSize: 12)),
                                if ((p['planFormation'] ?? '').toString().isNotEmpty)
                                  Padding(padding: const EdgeInsets.only(top: 2), child: Text(p['planFormation'].toString(), maxLines: 1, overflow: TextOverflow.ellipsis, style: const TextStyle(fontSize: 11))),
                              ]),
                              trailing: Container(padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 2), decoration: BoxDecoration(color: _readinessColor(readiness).withValues(alpha: .12), borderRadius: BorderRadius.circular(10)), child: Text(formatLabel(readiness), style: TextStyle(fontSize: 10, color: _readinessColor(readiness), fontWeight: FontWeight.bold))),
                            ),
                          );
                        },
                      ),
                    ),
    );
  }
}
