import 'package:flutter/material.dart';
import '../../../data/services/api_service.dart';
import '../../../../l10n/app_localizations.dart';

/// P3 #115 — Mon équipe / ma famille : membres de la famille spirituelle + encouragements.
class MyTeamFamilyScreen extends StatefulWidget {
  const MyTeamFamilyScreen({super.key, this.apiService});

  final ApiService? apiService;

  @override
  State<MyTeamFamilyScreen> createState() => _MyTeamFamilyScreenState();
}

class _MyTeamFamilyScreenState extends State<MyTeamFamilyScreen> {
  late final ApiService _api = widget.apiService ?? ApiService();
  List<dynamic> _team = [];
  List<dynamic> _received = [];
  bool _loading = true;

  @override
  void initState() {
    super.initState();
    _load();
  }

  Future<void> _load() async {
    try {
      final team = await _api.get('/encouragements/my-team');
      final received = await _api.get('/encouragements/received');
      setState(() {
        _team = (team.data as List<dynamic>?) ?? [];
        _received = (received.data as List<dynamic>?) ?? [];
        _loading = false;
      });
    } catch (_) {
      setState(() => _loading = false);
    }
  }

  Future<void> _sendEncouragement(Map<String, dynamic> member) async {
    final ctrl = TextEditingController();
    final ok = await showModalBottomSheet<bool>(
      context: context,
      isScrollControlled: true,
      builder: (ctx) => Padding(
        padding: EdgeInsets.only(left: 16, right: 16, top: 16, bottom: MediaQuery.of(ctx).viewInsets.bottom + 16),
        child: Column(mainAxisSize: MainAxisSize.min, children: [
          Text(AppLocalizations.of(ctx).encourageName('${member['prenom'] ?? ''} ${member['nom'] ?? ''}'.trim()), style: const TextStyle(fontWeight: FontWeight.bold)),
          const SizedBox(height: 12),
          TextField(controller: ctrl, maxLines: 3, decoration: InputDecoration(hintText: AppLocalizations.of(ctx).writeEncouragementHint, border: const OutlineInputBorder())),
          const SizedBox(height: 12),
          Row(mainAxisAlignment: MainAxisAlignment.end, children: [
            TextButton(onPressed: () => Navigator.pop(ctx, false), child: Text(AppLocalizations.of(ctx).cancel)),
            ElevatedButton(onPressed: () => Navigator.pop(ctx, true), child: Text(AppLocalizations.of(ctx).send)),
          ]),
        ]),
      ),
    );
    if (ok == true && ctrl.text.trim().isNotEmpty && member['userId'] != null) {
      try {
        await _api.post('/encouragements', data: {'toUserId': member['userId'], 'message': ctrl.text.trim(), 'kind': 'MESSAGE'});
        if (mounted) {
          ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(AppLocalizations.of(context).encouragementSent)));
          _load();
        }
      } catch (_) {
        if (mounted) ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(AppLocalizations.of(context).sendError)));
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    return DefaultTabController(
      length: 2,
      child: Scaffold(
        appBar: AppBar(
          title: Text(AppLocalizations.of(context).myTeamFamilyTitle),
          backgroundColor: Colors.blue.shade700,
          foregroundColor: Colors.white,
          bottom: TabBar(tabs: [
            Tab(text: AppLocalizations.of(context).teamTabMembers(_team.length)),
            Tab(text: AppLocalizations.of(context).teamTabReceived(_received.length)),
          ]),
        ),
        body: _loading
            ? const Center(child: CircularProgressIndicator())
            : TabBarView(children: [_buildTeam(), _buildReceived()]),
      ),
    );
  }

  Widget _buildTeam() {
    if (_team.isEmpty) return Center(child: Text(AppLocalizations.of(context).noSpiritualFamily));
    return RefreshIndicator(
      onRefresh: _load,
      child: ListView.builder(
        padding: const EdgeInsets.all(16),
        itemCount: _team.length,
        itemBuilder: (context, i) {
          final m = _team[i] as Map<String, dynamic>;
          final name = (m['prenom'] ?? m['nom'] ?? '?').toString();
          final initials = name.isNotEmpty ? name[0] : '?';
          return Card(
            margin: const EdgeInsets.only(bottom: 8),
            child: ListTile(
              leading: CircleAvatar(child: Text(initials.toUpperCase())),
              title: Text('${m['prenom'] ?? ''} ${m['nom'] ?? ''}'.trim() + ((m['estMoi'] == true) ? ' ${AppLocalizations.of(context).meLabel}' : '')),
              subtitle: Text('${m['etatSpirituel'] ?? ''} • 💛 ${AppLocalizations.of(context).encouragementsBadge((m['encouragementsRecus'] as num?)?.toInt() ?? 0)}', style: const TextStyle(fontSize: 12)),
              trailing: IconButton(icon: const Icon(Icons.favorite_border, color: Colors.pink), tooltip: AppLocalizations.of(context).sendEncouragementTooltip, onPressed: () => _sendEncouragement(m)),
            ),
          );
        },
      ),
    );
  }

  Widget _buildReceived() {
    if (_received.isEmpty) return Center(child: Text(AppLocalizations.of(context).noEncouragementsYet));
    return RefreshIndicator(
      onRefresh: _load,
      child: ListView.builder(
        padding: const EdgeInsets.all(16),
        itemCount: _received.length,
        itemBuilder: (context, i) {
          final e = _received[i] as Map<String, dynamic>;
          return Card(
            margin: const EdgeInsets.only(bottom: 8),
            color: Colors.pink.shade50,
            child: ListTile(
              leading: const Icon(Icons.favorite, color: Colors.pink),
              title: Text(e['message']?.toString() ?? '', style: const TextStyle(fontSize: 13)),
              subtitle: Text(e['createdAt']?.toString().split('T').first ?? '', style: const TextStyle(fontSize: 11)),
            ),
          );
        },
      ),
    );
  }
}
