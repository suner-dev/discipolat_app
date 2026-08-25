import 'package:flutter/material.dart';
import '../../../data/services/api_service.dart';
import '../../../core/format.dart';

/// Mentorat inversé — branché sur GET /api/v1/reverse-mentoring.
class ReverseMentoringScreen extends StatefulWidget {
  const ReverseMentoringScreen({super.key, this.apiService});
  final ApiService? apiService;

  @override
  State<ReverseMentoringScreen> createState() => _ReverseMentoringScreenState();
}

class _ReverseMentoringScreenState extends State<ReverseMentoringScreen> {
  late final ApiService _api = widget.apiService ?? ApiService();
  List<dynamic> _items = [];
  bool _loading = true;
  String? _error;

  @override
  void initState() { super.initState(); _load(); }

  Future<void> _load() async {
    setState(() { _loading = true; _error = null; });
    try {
      final res = await _api.get('/reverse-mentoring');
      final d = res.data;
      setState(() {
        _items = d is List ? d : (d is Map ? (d['content'] as List<dynamic>? ?? []) : <dynamic>[]);
        _loading = false;
      });
    } catch (_) {
      setState(() { _error = 'Impossible de charger les demandes.'; _loading = false; });
    }
  }

  Color _statusColor(String? s) {
    switch (s) {
      case 'ACCEPTED': return Colors.green;
      case 'RESOLVED': return Colors.teal;
      case 'REJECTED': return Colors.red;
      default: return Colors.amber;
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('🔄 Mentorat inversé'), backgroundColor: Colors.indigo, foregroundColor: Colors.white),
      body: _loading
          ? const Center(child: CircularProgressIndicator())
          : _error != null
              ? Center(child: Column(mainAxisSize: MainAxisSize.min, children: [
                  Padding(padding: const EdgeInsets.all(24), child: Text(_error!, textAlign: TextAlign.center)),
                  ElevatedButton(onPressed: _load, child: const Text('Réessayer')),
                ]))
              : _items.isEmpty
                  ? const Center(child: Text('Aucune demande de mentorat inversé.'))
                  : RefreshIndicator(
                      onRefresh: _load,
                      child: ListView.builder(
                        padding: const EdgeInsets.all(16),
                        itemCount: _items.length,
                        itemBuilder: (context, i) {
                          final r = _items[i] as Map<String, dynamic>;
                          final status = r['status']?.toString() ?? 'PENDING';
                          return Card(
                            margin: const EdgeInsets.only(bottom: 10),
                            child: ListTile(
                              leading: CircleAvatar(backgroundColor: _statusColor(status).withValues(alpha: .15), child: Icon(Icons.swap_horiz, color: _statusColor(status), size: 20)),
                              title: Text(r['topic']?.toString() ?? 'Sujet', style: const TextStyle(fontWeight: FontWeight.w600)),
                              subtitle: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
                                if ((r['description'] ?? '').toString().isNotEmpty)
                                  Padding(padding: const EdgeInsets.only(top: 2), child: Text(r['description'].toString(), maxLines: 2, overflow: TextOverflow.ellipsis)),
                                const SizedBox(height: 4),
                                Row(children: [
                                  Container(padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 2), decoration: BoxDecoration(color: _statusColor(status).withValues(alpha: .12), borderRadius: BorderRadius.circular(10)), child: Text(formatLabel(status), style: TextStyle(fontSize: 10, color: _statusColor(status), fontWeight: FontWeight.bold))),
                                  const SizedBox(width: 8),
                                  Text('Urgence ${r['urgencyLevel'] ?? 3}/5', style: const TextStyle(fontSize: 11)),
                                ]),
                              ]),
                              isThreeLine: true,
                            ),
                          );
                        },
                      ),
                    ),
    );
  }
}
