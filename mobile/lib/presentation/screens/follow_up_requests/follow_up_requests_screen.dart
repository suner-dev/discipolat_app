import 'package:flutter/material.dart';
import '../../../data/services/api_service.dart';
import '../../../l10n/app_localizations.dart';

/// P3 #112 — Demandes de suivi : demander un faiseur ou un accompagnement spirituel.
class FollowUpRequestsScreen extends StatefulWidget {
  const FollowUpRequestsScreen({super.key, this.apiService});

  final ApiService? apiService;

  @override
  State<FollowUpRequestsScreen> createState() => _FollowUpRequestsScreenState();
}

class _FollowUpRequestsScreenState extends State<FollowUpRequestsScreen> with SingleTickerProviderStateMixin {
  late final ApiService _api = widget.apiService ?? ApiService();
  late final TabController _tab = TabController(length: 2, vsync: this);
  List<dynamic> _mine = [];
  List<dynamic> _assigned = [];
  bool _loading = true;

  Map<String, String> _types(AppLocalizations l) => {
    'FAISEUR': l.fuTypeMaker,
    'ACCOMPAGNEMENT_SPIRITUEL': l.fuTypeSpiritual,
    'CONSEIL_PASTORAL': l.fuTypePastoral,
  };

  @override
  void initState() {
    super.initState();
    _load();
  }

  Future<void> _load() async {
    try {
      final mine = await _api.get('/follow-up-requests/mine');
      final assigned = await _api.get('/follow-up-requests/assigned-to-me');
      setState(() { _mine = (mine.data as List<dynamic>?) ?? []; _assigned = (assigned.data as List<dynamic>?) ?? []; _loading = false; });
    } catch (_) {
      setState(() => _loading = false);
    }
  }

  Future<void> _createRequest() async {
    String type = 'FAISEUR';
    final ctrl = TextEditingController();
    final ok = await showModalBottomSheet<bool>(
      context: context,
      isScrollControlled: true,
      builder: (ctx) {
        final l = AppLocalizations.of(context);
        return Padding(
        padding: EdgeInsets.only(left: 16, right: 16, top: 16, bottom: MediaQuery.of(ctx).viewInsets.bottom + 16),
        child: Column(mainAxisSize: MainAxisSize.min, crossAxisAlignment: CrossAxisAlignment.start, children: [
          Text(l.newFollowUpRequest, style: const TextStyle(fontWeight: FontWeight.bold)),
          const SizedBox(height: 12),
          DropdownButtonFormField<String>(initialValue: type, items: _types(l).entries.map((e) => DropdownMenuItem(value: e.key, child: Text(e.value))).toList(), onChanged: (v) => type = v ?? 'FAISEUR'),
          const SizedBox(height: 12),
          TextField(controller: ctrl, maxLines: 3, decoration: InputDecoration(hintText: l.describeNeed, border: const OutlineInputBorder())),
          const SizedBox(height: 12),
          Row(mainAxisAlignment: MainAxisAlignment.end, children: [
            TextButton(onPressed: () => Navigator.pop(ctx, false), child: Text(l.cancel)),
            ElevatedButton(onPressed: () => Navigator.pop(ctx, true), child: Text(l.send)),
          ]),
        ]),
        );
      },
    );
    if (ok == true) {
      try {
        await _api.post('/follow-up-requests', data: {'type': type, 'message': ctrl.text.trim()});
        if (mounted) {
          ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(AppLocalizations.of(context).requestSent)));
          _load();
        }
      } catch (_) {
        if (mounted) {
          ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(AppLocalizations.of(context).requestFailed)));
        }
      }
    }
  }

  Future<void> _complete(Map<String, dynamic> r) async {
    try {
      await _api.patch('/follow-up-requests/${r['id']}/status', data: {'status': 'TERMINEE'});
      _load();
    } catch (_) {}
  }

  MaterialColor _statusColor(String status) {
    switch (status) {
      case 'EN_ATTENTE':
        return Colors.orange;
      case 'TERMINEE':
        return Colors.green;
      case 'REJETEE':
        return Colors.red;
      default:
        return Colors.blue;
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: Text(AppLocalizations.of(context).followUpTitle),
        backgroundColor: Colors.pink.shade700,
        foregroundColor: Colors.white,
        bottom: TabBar(controller: _tab, indicatorColor: Colors.white, tabs: [Tab(text: AppLocalizations.of(context).myRequests(_mine.length)), Tab(text: AppLocalizations.of(context).assignedToMe(_assigned.length))]),
      ),
      floatingActionButton: FloatingActionButton.extended(
        backgroundColor: Colors.pink.shade700,
        onPressed: _createRequest,
        icon: const Icon(Icons.add, color: Colors.white),
        label: Text(AppLocalizations.of(context).askAction, style: const TextStyle(color: Colors.white)),
      ),
      body: _loading
          ? const Center(child: CircularProgressIndicator())
          : TabBarView(controller: _tab, children: [_buildList(_mine, mine: true), _buildList(_assigned, mine: false)]),
    );
  }

  Widget _buildList(List<dynamic> list, {required bool mine}) {
    if (list.isEmpty) {
      return RefreshIndicator(
        onRefresh: _load,
        child: ListView(
          children: [
            Center(
              child: Padding(
                padding: const EdgeInsets.all(32),
                child: Text(mine ? AppLocalizations.of(context).emptyMyRequests : AppLocalizations.of(context).emptyAssignedRequests),
              ),
            ),
          ],
        ),
      );
    }
    return RefreshIndicator(
      onRefresh: _load,
      child: ListView.builder(
        padding: const EdgeInsets.fromLTRB(16, 16, 16, 88),
        itemCount: list.length,
        itemBuilder: (context, i) {
          final r = list[i] as Map<String, dynamic>;
          final status = r['status']?.toString() ?? '';
          return Card(
            margin: const EdgeInsets.only(bottom: 8),
            child: ListTile(
              leading: Icon(Icons.handshake_outlined, color: _statusColor(status)),
              title: Text(_types(AppLocalizations.of(context))[r['type']] ?? r['type']?.toString() ?? '', style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 14)),
              subtitle: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
                if (r['message'] != null && r['message'].toString().isNotEmpty) Text(r['message'].toString(), maxLines: 2, overflow: TextOverflow.ellipsis, style: const TextStyle(fontSize: 12)),
                Text('${r['createdAt']?.toString().split('T').first ?? ''} • $status${r['assignedToName'] != null ? ' • ${r['assignedToName']}' : ''}', style: const TextStyle(fontSize: 11)),
              ]),
              isThreeLine: true,
              trailing: (!mine && status != 'TERMINEE') ? IconButton(icon: const Icon(Icons.check_circle_outline, color: Colors.green), tooltip: AppLocalizations.of(context).markComplete, onPressed: () => _complete(r)) : null,
            ),
          );
        },
      ),
    );
  }
}
