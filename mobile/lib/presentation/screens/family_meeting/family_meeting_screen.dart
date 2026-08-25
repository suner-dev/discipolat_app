import 'package:flutter/material.dart';
import '../../../data/services/api_service.dart';
import '../../../core/format.dart';

/// Réunions de famille — branché sur GET /api/v1/family-meetings.
class FamilyMeetingScreen extends StatefulWidget {
  const FamilyMeetingScreen({super.key, this.apiService});
  final ApiService? apiService;

  @override
  State<FamilyMeetingScreen> createState() => _FamilyMeetingScreenState();
}

class _FamilyMeetingScreenState extends State<FamilyMeetingScreen> {
  late final ApiService _api = widget.apiService ?? ApiService();
  List<dynamic> _items = [];
  bool _loading = true;
  String? _error;

  @override
  void initState() { super.initState(); _load(); }

  Future<void> _load() async {
    setState(() { _loading = true; _error = null; });
    try {
      final res = await _api.get('/family-meetings');
      final d = res.data;
      setState(() { _items = d is List ? d : <dynamic>[]; _loading = false; });
    } catch (_) {
      setState(() { _error = 'Impossible de charger les réunions.'; _loading = false; });
    }
  }

  Color _statusColor(String? s) => s == 'COMPLETED' || s == 'TERMINÉE' ? Colors.green : (s == 'SCHEDULED' || s == 'PLANIFIÉE' ? Colors.blue : Colors.grey);

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('👨‍👩‍👧 Réunions famille'), backgroundColor: Colors.blue, foregroundColor: Colors.white),
      body: _loading
          ? const Center(child: CircularProgressIndicator())
          : _error != null
              ? Center(child: Column(mainAxisSize: MainAxisSize.min, children: [
                  Padding(padding: const EdgeInsets.all(24), child: Text(_error!, textAlign: TextAlign.center)),
                  ElevatedButton(onPressed: _load, child: const Text('Réessayer')),
                ]))
              : _items.isEmpty
                  ? const Center(child: Text('Aucune réunion programmée.'))
                  : RefreshIndicator(
                      onRefresh: _load,
                      child: ListView.builder(
                        padding: const EdgeInsets.all(16),
                        itemCount: _items.length,
                        itemBuilder: (context, i) {
                          final m = _items[i] as Map<String, dynamic>;
                          final status = m['status']?.toString() ?? 'DRAFT';
                          return Card(
                            margin: const EdgeInsets.only(bottom: 10),
                            child: Padding(
                              padding: const EdgeInsets.all(12),
                              child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
                                Row(children: [
                                  Expanded(child: Text('Famille #${(m['familyId'] ?? '').toString().substring(0, (m['familyId'] ?? '').toString().length.clamp(0, 8))}', style: const TextStyle(fontWeight: FontWeight.w600))),
                                  Container(padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 2), decoration: BoxDecoration(color: _statusColor(status).withValues(alpha: .12), borderRadius: BorderRadius.circular(10)), child: Text(formatLabel(status), style: TextStyle(fontSize: 10, color: _statusColor(status), fontWeight: FontWeight.bold))),
                                ]),
                                const SizedBox(height: 6),
                                if ((m['agenda'] ?? '').toString().isNotEmpty)
                                  Container(width: double.infinity, padding: const EdgeInsets.all(8), decoration: BoxDecoration(color: Colors.grey.withValues(alpha: .08), borderRadius: BorderRadius.circular(8)), child: Text(m['agenda'].toString(), style: const TextStyle(fontSize: 12), maxLines: 4, overflow: TextOverflow.ellipsis)),
                                const SizedBox(height: 4),
                                Text('${m['attendeesCount'] ?? 0} participants', style: TextStyle(fontSize: 11, color: Colors.grey.shade600)),
                              ]),
                            ),
                          );
                        },
                      ),
                    ),
    );
  }
}
