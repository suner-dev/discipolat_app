import 'package:flutter/material.dart';
import '../../../data/services/api_service.dart';
import '../../../l10n/app_localizations.dart';

/// Modération IA — branché sur `GET /api/v1/moderation`.
class ModerationScreen extends StatefulWidget {
  const ModerationScreen({super.key, this.apiService});

  final ApiService? apiService;

  @override
  State<ModerationScreen> createState() => _ModerationScreenState();
}

class _ModerationScreenState extends State<ModerationScreen> {
  late final ApiService _api = widget.apiService ?? ApiService();
  List<dynamic> _items = [];
  bool _isLoading = true;
  String? _error;

  @override
  void initState() {
    super.initState();
    _load();
  }

  Future<void> _load() async {
    setState(() {
      _isLoading = _items.isEmpty;
      _error = null;
    });
    try {
      final res = await _api.get('/moderation');
      final data = res.data;
      final list = data is List ? data : (data is Map && data['content'] != null ? data['content'] : []);
      if (mounted) {
        setState(() {
          _items = List<dynamic>.from(list);
          _isLoading = false;
        });
      }
    } catch (_) {
      if (mounted) {
        setState(() {
          _isLoading = false;
          _error = AppLocalizations.of(context).moderationError;
        });
      }
    }
  }

  Future<void> _review(dynamic item, String decision) async {
    try {
      await _api.put('/moderation/${item['id']}/review', data: {'decision': decision});
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
            SnackBar(content: Text(AppLocalizations.of(context).reviewDone(decision))));
      }
      _load();
    } catch (_) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
            SnackBar(content: Text(AppLocalizations.of(context).reviewError)));
      }
    }
  }

  Color riskColor(String r) {
    switch (r) {
      case 'CRITICAL':
        return Colors.red;
      case 'HIGH':
        return Colors.orange;
      case 'MEDIUM':
        return Colors.amber;
      default:
        return Colors.green;
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: Text(AppLocalizations.of(context).moderationTitle), backgroundColor: Colors.red.shade600, foregroundColor: Colors.white),
      body: _isLoading
          ? const Center(child: CircularProgressIndicator())
          : _error != null && _items.isEmpty
              ? ListView(
                  padding: const EdgeInsets.all(24),
                  children: [
                    const SizedBox(height: 60),
                    const Icon(Icons.cloud_off, size: 56, color: Colors.grey),
                    const SizedBox(height: 12),
                    Center(child: Text(_error ?? 'Erreur', textAlign: TextAlign.center)),
                    const SizedBox(height: 16),
                    Center(
                      child: FilledButton.icon(
                        onPressed: _load,
                        icon: const Icon(Icons.refresh),
                        label: Text(AppLocalizations.of(context).retry),
                      ),
                    ),
                  ],
                )
              : RefreshIndicator(
                  onRefresh: _load,
                  child: _items.isEmpty
                      ? ListView(
                          children: [
                            const SizedBox(height: 120),
                            const Icon(Icons.inbox, size: 56, color: Colors.grey),
                            Center(child: Padding(padding: const EdgeInsets.all(8), child: Text(AppLocalizations.of(context).moderationEmpty, style: const TextStyle(color: Colors.grey)))),
                          ],
                        )
                      : ListView.builder(
                          padding: const EdgeInsets.all(16),
                          itemCount: _items.length,
                          itemBuilder: (context, i) {
                            final item = _items[i];
                            final status = item['status']?.toString() ?? 'PENDING';
                            return Card(
                              margin: const EdgeInsets.only(bottom: 12),
                              child: Padding(
                                padding: const EdgeInsets.all(12),
                                child: Column(
                                  crossAxisAlignment: CrossAxisAlignment.start,
                                  children: [
                                    Row(children: [
                                      Icon(
                                        status == 'APPROVED' ? Icons.check_circle : status == 'REJECTED' ? Icons.cancel : Icons.warning,
                                        color: status == 'APPROVED' ? Colors.green : status == 'REJECTED' ? Colors.red : Colors.amber,
                                        size: 20,
                                      ),
                                      const SizedBox(width: 8),
                                      Expanded(child: Text(item['content']?.toString() ?? '', style: const TextStyle(fontSize: 14))),
                                    ]),
                                    const SizedBox(height: 8),
                                    Row(children: [
                                      Chip(label: Text(item['source']?.toString() ?? '', style: const TextStyle(fontSize: 11)), backgroundColor: Colors.grey.shade200),
                                      const SizedBox(width: 8),
                                      Chip(
                                        label: Text(item['riskLevel']?.toString() ?? 'LOW', style: TextStyle(fontSize: 11, color: riskColor(item['riskLevel']?.toString() ?? 'LOW'))),
                                        backgroundColor: riskColor(item['riskLevel']?.toString() ?? 'LOW').withOpacity(0.15),
                                      ),
                                      const Spacer(),
                                      if (status == 'PENDING') ...[
                                        TextButton(onPressed: () => _review(item, 'APPROVED'), child: Text(AppLocalizations.of(context).approve, style: const TextStyle(color: Colors.green))),
                                        TextButton(onPressed: () => _review(item, 'REJECTED'), child: Text(AppLocalizations.of(context).reject, style: const TextStyle(color: Colors.red))),
                                      ],
                                    ]),
                                  ],
                                ),
                              ),
                            );
                          },
                        ),
                ),
    );
  }
}
