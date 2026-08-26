import 'package:flutter/material.dart';
import '../../../data/services/api_service.dart';
import '../../../l10n/app_localizations.dart';

/// Récompenses (certificats) — branché sur GET /api/v1/reward-certificates/mine.
class RewardsScreen extends StatefulWidget {
  const RewardsScreen({super.key, this.apiService});
  final ApiService? apiService;

  @override
  State<RewardsScreen> createState() => _RewardsScreenState();
}

class _RewardsScreenState extends State<RewardsScreen> {
  late final ApiService _api = widget.apiService ?? ApiService();
  List<dynamic> _items = [];
  bool _loading = true;
  String? _error;

  @override
  void initState() { super.initState(); _load(); }

  Future<void> _load() async {
    setState(() { _loading = true; _error = null; });
    try {
      final res = await _api.get('/reward-certificates/mine');
      final d = res.data;
      setState(() { _items = d is List ? d : <dynamic>[]; _loading = false; });
    } catch (_) {
      if (!mounted) return;
      setState(() { _error = AppLocalizations.of(context).rewardsError; _loading = false; });
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: Text(AppLocalizations.of(context).rewardsTitle), backgroundColor: Colors.amber.shade800, foregroundColor: Colors.white),
      body: _loading
          ? const Center(child: CircularProgressIndicator())
          : _error != null
              ? Center(child: Column(mainAxisSize: MainAxisSize.min, children: [
                  Padding(padding: const EdgeInsets.all(24), child: Text(_error!, textAlign: TextAlign.center)),
                  ElevatedButton(onPressed: _load, child: Text(AppLocalizations.of(context).retry)),
                ]))
              : _items.isEmpty
                  ? Center(child: Text(AppLocalizations.of(context).rewardsEmpty))
                  : RefreshIndicator(
                      onRefresh: _load,
                      child: ListView.builder(
                        padding: const EdgeInsets.all(16),
                        itemCount: _items.length,
                        itemBuilder: (context, i) {
                          final r = _items[i] as Map<String, dynamic>;
                          return Card(
                            margin: const EdgeInsets.only(bottom: 10),
                            child: ListTile(
                              leading: CircleAvatar(backgroundColor: Colors.amber.withValues(alpha: .18), child: const Icon(Icons.emoji_events, color: Colors.amber, size: 20)),
                              title: Text(r['title']?.toString() ?? 'Certificat', style: const TextStyle(fontWeight: FontWeight.w600)),
                              subtitle: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
                                if ((r['mention'] ?? '').toString().isNotEmpty) Text(r['mention'].toString(), style: const TextStyle(fontSize: 11, fontStyle: FontStyle.italic)),
                                if ((r['description'] ?? '').toString().isNotEmpty)
                                  Padding(padding: const EdgeInsets.only(top: 2), child: Text(r['description'].toString(), maxLines: 2, overflow: TextOverflow.ellipsis, style: const TextStyle(fontSize: 12))),
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
