import 'package:flutter/material.dart';
import '../../../data/services/api_service.dart';
import '../../../../l10n/app_localizations.dart';

/// Demandes administratives — branché sur GET /api/v1/admin-requests.
class AdminRequestsScreen extends StatefulWidget {
  const AdminRequestsScreen({super.key, this.apiService});
  final ApiService? apiService;

  @override
  State<AdminRequestsScreen> createState() => _AdminRequestsScreenState();
}

class _AdminRequestsScreenState extends State<AdminRequestsScreen> {
  late final ApiService _api = widget.apiService ?? ApiService();
  List<dynamic> _items = [];
  bool _loading = true;
  String? _error;

  @override
  void initState() { super.initState(); _load(); }

  Future<void> _load() async {
    setState(() { _loading = true; _error = null; });
    try {
      final res = await _api.get('/admin-requests');
      final d = res.data;
      setState(() { _items = d is List ? d : <dynamic>[]; _loading = false; });
    } catch (_) {
      if (!mounted) return;
      setState(() { _error = AppLocalizations.of(context).adminRequestsError; _loading = false; });
    }
  }

  Color _statutColor(String? s) => s == 'APPROUVÉE' ? Colors.green : (s == 'REJETÉE' ? Colors.red : (s == 'SOUMISE' ? Colors.amber : Colors.grey));

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: Text(AppLocalizations.of(context).adminRequestsTitle), backgroundColor: Colors.indigo, foregroundColor: Colors.white),
      body: _loading
          ? const Center(child: CircularProgressIndicator())
          : _error != null
              ? Center(child: Column(mainAxisSize: MainAxisSize.min, children: [
                  Padding(padding: const EdgeInsets.all(24), child: Text(_error!, textAlign: TextAlign.center)),
                  ElevatedButton(onPressed: _load, child: Text(AppLocalizations.of(context).retry)),
                ]))
              : _items.isEmpty
                  ? Center(child: Text(AppLocalizations.of(context).adminRequestsEmpty))
                  : RefreshIndicator(
                      onRefresh: _load,
                      child: ListView.builder(
                        padding: const EdgeInsets.all(16),
                        itemCount: _items.length,
                        itemBuilder: (context, i) {
                          final r = _items[i] as Map<String, dynamic>;
                          final statut = r['statut']?.toString() ?? 'SOUMISE';
                          return Card(
                            margin: const EdgeInsets.only(bottom: 10),
                            child: ListTile(
                              leading: CircleAvatar(backgroundColor: _statutColor(statut).withValues(alpha: .15), child: Icon(Icons.description, color: _statutColor(statut), size: 20)),
                              title: Text(r['typeDemande']?.toString() ?? 'Demande', style: const TextStyle(fontSize: 14, fontWeight: FontWeight.w600)),
                              subtitle: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
                                if ((r['motif'] ?? '').toString().isNotEmpty)
                                  Padding(padding: const EdgeInsets.only(top: 2), child: Text(r['motif'].toString(), maxLines: 2, overflow: TextOverflow.ellipsis)),
                                const SizedBox(height: 4),
                                Container(padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 2), decoration: BoxDecoration(color: _statutColor(statut).withValues(alpha: .12), borderRadius: BorderRadius.circular(10)), child: Text(statut, style: TextStyle(fontSize: 10, color: _statutColor(statut), fontWeight: FontWeight.bold))),
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
