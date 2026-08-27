import 'dart:convert';
import 'package:flutter/material.dart';
import '../../../data/services/api_service.dart';
import '../../../../l10n/app_localizations.dart';

/// Notes IA de visites — branché sur GET /api/v1/ai-visit-notes.
class AiVisitNotesScreen extends StatefulWidget {
  const AiVisitNotesScreen({super.key, this.apiService});
  final ApiService? apiService;

  @override
  State<AiVisitNotesScreen> createState() => _AiVisitNotesScreenState();
}

class _AiVisitNotesScreenState extends State<AiVisitNotesScreen> {
  late final ApiService _api = widget.apiService ?? ApiService();
  List<dynamic> _items = [];
  bool _loading = true;
  String? _error;

  @override
  void initState() { super.initState(); _load(); }

  Future<void> _load() async {
    setState(() { _loading = true; _error = null; });
    try {
      final res = await _api.get('/ai-visit-notes');
      final d = res.data;
      setState(() {
        _items = d is List ? d : (d is Map ? (d['content'] as List<dynamic>? ?? []) : <dynamic>[]);
        _loading = false;
      });
    } catch (_) {
      if (!mounted) return;
      setState(() { _error = AppLocalizations.of(context).aiVisitNotesError; _loading = false; });
    }
  }

  IconData _sentimentIcon(String? s) {
    switch (s) {
      case 'POSITIVE': return Icons.sentiment_very_satisfied;
      case 'CONCERNING': return Icons.sentiment_dissatisfied;
      case 'CRITICAL': return Icons.report_problem;
      default: return Icons.sentiment_neutral;
    }
  }

  Color _sentimentColor(String? s) {
    switch (s) {
      case 'POSITIVE': return Colors.green;
      case 'CONCERNING': return Colors.orange;
      case 'CRITICAL': return Colors.red;
      default: return Colors.grey;
    }
  }

  List<String> _parseActions(dynamic raw) {
    if (raw == null) return const [];
    try {
      final decoded = jsonDecode(raw.toString());
      if (decoded is List) return decoded.map((e) => e.toString()).toList();
    } catch (_) {}
    return raw.toString().split('\n').where((l) => l.trim().isNotEmpty).toList();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: Text(AppLocalizations.of(context).aiVisitNotesTitle), backgroundColor: Colors.blueGrey, foregroundColor: Colors.white),
      body: _loading
          ? const Center(child: CircularProgressIndicator())
          : _error != null
              ? Center(child: Column(mainAxisSize: MainAxisSize.min, children: [
                  Padding(padding: const EdgeInsets.all(24), child: Text(_error!, textAlign: TextAlign.center)),
                  ElevatedButton(onPressed: _load, child: Text(AppLocalizations.of(context).retry)),
                ]))
              : _items.isEmpty
                  ? Center(child: Text(AppLocalizations.of(context).aiVisitNotesEmpty))
                  : RefreshIndicator(
                      onRefresh: _load,
                      child: ListView.builder(
                        padding: const EdgeInsets.all(16),
                        itemCount: _items.length,
                        itemBuilder: (context, i) {
                          final n = _items[i] as Map<String, dynamic>;
                          final sentiment = n['aiSentiment']?.toString() ?? 'NEUTRAL';
                          final actions = _parseActions(n['aiActionItems']);
                          return Card(
                            margin: const EdgeInsets.only(bottom: 10),
                            child: Padding(
                              padding: const EdgeInsets.all(12),
                              child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
                                Row(children: [
                                  Icon(_sentimentIcon(sentiment), size: 18, color: _sentimentColor(sentiment)),
                                  const SizedBox(width: 6),
                                  Expanded(child: Text('Visite #${(n['visitId'] ?? '').toString().substring(0, (n['visitId'] ?? '').toString().length.clamp(0, 8))}', style: const TextStyle(fontWeight: FontWeight.w600))),
                                  if (n['isVerified'] == true) const Icon(Icons.verified, size: 16, color: Colors.green),
                                ]),
                                const SizedBox(height: 6),
                                Text(n['aiSummary']?.toString() ?? '', style: const TextStyle(fontSize: 13)),
                                if (actions.isNotEmpty) ...[
                                  const SizedBox(height: 6),
                                  ...actions.map((a) => Padding(padding: const EdgeInsets.only(top: 2), child: Row(children: [
                                    const Icon(Icons.arrow_right, size: 14, color: Colors.blueGrey),
                                    Expanded(child: Text(a, style: const TextStyle(fontSize: 12, color: Colors.blueGrey))),
                                  ]))),
                                ],
                              ]),
                            ),
                          );
                        },
                      ),
                    ),
    );
  }
}
