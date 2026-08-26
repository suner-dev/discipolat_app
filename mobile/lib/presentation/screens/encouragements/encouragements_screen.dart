import 'package:flutter/material.dart';
import '../../../data/services/api_service.dart';
import '../../../l10n/app_localizations.dart';
import '../../widgets/glass_theme.dart';

/// P3 — Encouragements (envoyer/recevoir des mots d'encouragement)
class EncouragementsScreen extends StatefulWidget {
  const EncouragementsScreen({super.key});

  @override
  State<EncouragementsScreen> createState() => _EncouragementsScreenState();
}

class _EncouragementsScreenState extends State<EncouragementsScreen>
    with SingleTickerProviderStateMixin {
  final ApiService _api = ApiService();
  late TabController _tabCtrl;
  List<dynamic> _received = [];
  List<dynamic> _sent = [];
  List<dynamic> _team = [];
  bool _isLoading = true;

  @override
  void initState() {
    super.initState();
    _tabCtrl = TabController(length: 3, vsync: this);
    _loadAll();
  }

  @override
  void dispose() {
    _tabCtrl.dispose();
    super.dispose();
  }

  Future<void> _loadAll() async {
    setState(() => _isLoading = true);
    try {
      final r = await _api.get('/encouragements/received');
      final s = await _api.get('/encouragements/sent');
      final t = await _api.get('/encouragements/my-team');
      if (mounted) {
        setState(() {
          _received = (r.data as List<dynamic>?) ?? [];
          _sent = (s.data as List<dynamic>?) ?? [];
          _team = (t.data as List<dynamic>?) ?? [];
          _isLoading = false;
        });
      }
    } catch (_) {
      if (mounted) setState(() => _isLoading = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: Text(AppLocalizations.of(context).encouragementsTitle),
        bottom: TabBar(
          controller: _tabCtrl,
          tabs: [
            Tab(text: AppLocalizations.of(context).tabReceived(_received.length)),
            Tab(text: AppLocalizations.of(context).tabSent(_sent.length)),
            Tab(text: AppLocalizations.of(context).tabTeam(_team.length)),
          ],
        ),
      ),
      floatingActionButton: FloatingActionButton(
        onPressed: _showComposeSheet,
        backgroundColor: Colors.pink,
        child: const Icon(Icons.add, color: Colors.white),
      ),
      body: _isLoading
          ? const Center(child: CircularProgressIndicator())
          : TabBarView(
              controller: _tabCtrl,
              children: [
                _buildList(_received, isReceived: true),
                _buildList(_sent, isReceived: false),
                _buildTeamList(),
              ],
            ),
    );
  }

  Widget _buildList(List<dynamic> items, {required bool isReceived}) {
    if (items.isEmpty) {
      return Center(
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Icon(isReceived ? Icons.inbox : Icons.send, size: 48,
                color: Colors.white.withValues(alpha: 0.2)),
            const SizedBox(height: 12),
            Text(isReceived ? AppLocalizations.of(context).emptyReceivedEnc : AppLocalizations.of(context).emptySentEnc,
                style: TextStyle(color: Colors.white.withValues(alpha: 0.5))),
          ],
        ),
      );
    }
    return RefreshIndicator(
      onRefresh: _loadAll,
      child: ListView.builder(
        padding: const EdgeInsets.all(12),
        itemCount: items.length,
        itemBuilder: (ctx, i) => _encCard(items[i]),
      ),
    );
  }

  Widget _encCard(Map<String, dynamic> enc) {
    final icons = {'PRAYER': '🙏', 'PRAISE': '⭐', 'THANKS': '❤️', 'SUPPORT': '💪', 'WELCOME': '👋', 'SCRIPTURE': '📖'};
    final icon = icons[enc['type']] ?? '💝';

    return GlassCard(
      margin: const EdgeInsets.only(bottom: 8),
      padding: const EdgeInsets.all(12),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Container(
            width: 40,
            height: 40,
            decoration: BoxDecoration(
              color: Colors.pink.withValues(alpha: 0.15),
              borderRadius: BorderRadius.circular(12),
            ),
            child: Center(child: Text(icon, style: const TextStyle(fontSize: 18))),
          ),
          const SizedBox(width: 10),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Row(
                  children: [
                    Text(enc['senderName'] ?? '', style: const TextStyle(
                        color: Colors.white, fontWeight: FontWeight.w600, fontSize: 13)),
                    const SizedBox(width: 6),
                    Text('→ ${enc['recipientName'] ?? ''}',
                        style: TextStyle(color: Colors.white.withValues(alpha: 0.5), fontSize: 11)),
                  ],
                ),
                const SizedBox(height: 4),
                Text('"${enc['message'] ?? ''}"',
                    style: TextStyle(color: Colors.white.withValues(alpha: 0.7), fontSize: 12, fontStyle: FontStyle.italic),
                    maxLines: 3, overflow: TextOverflow.ellipsis),
                const SizedBox(height: 4),
                Text(_formatDate(enc['createdAt']),
                    style: TextStyle(color: Colors.white.withValues(alpha: 0.3), fontSize: 10)),
              ],
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildTeamList() {
    if (_team.isEmpty) {
      return Center(
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Icon(Icons.group, size: 48, color: Colors.white.withValues(alpha: 0.2)),
            const SizedBox(height: 12),
            Text(AppLocalizations.of(context).emptyTeam,
                style: TextStyle(color: Colors.white.withValues(alpha: 0.5))),
          ],
        ),
      );
    }
    return RefreshIndicator(
      onRefresh: _loadAll,
      child: ListView.builder(
        padding: const EdgeInsets.all(12),
        itemCount: _team.length,
        itemBuilder: (ctx, i) {
          final member = _team[i] as Map<String, dynamic>;
          final name = member['name'] ?? '';
          final initials = name.split(' ').map((n) => n[0]).join().substring(0, 2).toUpperCase();
          return GlassCard(
            margin: const EdgeInsets.only(bottom: 8),
            padding: const EdgeInsets.all(12),
            child: Row(
              children: [
                CircleAvatar(
                  backgroundColor: Colors.pink.withValues(alpha: 0.3),
                  child: Text(initials, style: const TextStyle(color: Colors.white, fontWeight: FontWeight.bold, fontSize: 12)),
                ),
                const SizedBox(width: 12),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(name, style: const TextStyle(color: Colors.white, fontWeight: FontWeight.w600, fontSize: 13)),
                      Text('${member['role'] ?? ''} • ${AppLocalizations.of(context).encouragementsReceived((member['encouragementsReceived'] as num?)?.toInt() ?? 0)}',
                          style: TextStyle(color: Colors.white.withValues(alpha: 0.5), fontSize: 11)),
                    ],
                  ),
                ),
                IconButton(
                  icon: Icon(Icons.favorite_border, color: Colors.pink.withValues(alpha: 0.7), size: 20),
                  onPressed: () => _showComposeSheet(recipientId: member['userId'], recipientName: name),
                ),
              ],
            ),
          );
        },
      ),
    );
  }

  void _showComposeSheet({String? recipientId, String? recipientName}) {
    final msgCtrl = TextEditingController();
    String type = 'PRAYER';
    showModalBottomSheet(
      context: context,
      isScrollControlled: true,
      shape: const RoundedRectangleBorder(borderRadius: BorderRadius.vertical(top: Radius.circular(20))),
      builder: (ctx) => StatefulBuilder(
        builder: (ctx, setModalState) => Padding(
          padding: EdgeInsets.only(
            bottom: MediaQuery.of(ctx).viewInsets.bottom,
            left: 16, right: 16, top: 16,
          ),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(AppLocalizations.of(context).composeEncouragement,
                  style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 16)),
              const SizedBox(height: 12),
              // Type chips
              Wrap(
                spacing: 8,
                children: [
                  {'value': 'PRAYER', 'label': AppLocalizations.of(context).encTypePrayer},
                  {'value': 'PRAISE', 'label': AppLocalizations.of(context).encTypePraise},
                  {'value': 'THANKS', 'label': AppLocalizations.of(context).encTypeThanks},
                  {'value': 'SUPPORT', 'label': AppLocalizations.of(context).encTypeSupport},
                  {'value': 'WELCOME', 'label': AppLocalizations.of(context).encTypeWelcome},
                  {'value': 'SCRIPTURE', 'label': AppLocalizations.of(context).encTypeScripture},
                ].map((t) => ChoiceChip(
                  label: Text(t['label']!, style: const TextStyle(fontSize: 12)),
                  selected: type == t['value'],
                  onSelected: (_) => setModalState(() => type = t['value']!),
                )).toList(),
              ),
              const SizedBox(height: 12),
              TextField(
                controller: msgCtrl,
                maxLines: 3,
                decoration: InputDecoration(
                  hintText: AppLocalizations.of(context).writeEncouragementHint,
                  border: const OutlineInputBorder(),
                ),
              ),
              const SizedBox(height: 12),
              SizedBox(
                width: double.infinity,
                child: ElevatedButton(
                  onPressed: () async {
                    if (msgCtrl.text.isEmpty) return;
                    try {
                      await _api.post('/encouragements', data: {
                        'recipientId': recipientId ?? '',
                        'message': msgCtrl.text,
                        'type': type,
                      });
                      Navigator.pop(ctx);
                      ScaffoldMessenger.of(context).showSnackBar(
                        SnackBar(content: Text(AppLocalizations.of(context).encouragementSent)),
                      );
                      _loadAll();
                    } catch (_) {}
                  },
                  style: ElevatedButton.styleFrom(backgroundColor: Colors.pink),
                  child: Text(AppLocalizations.of(context).send, style: const TextStyle(color: Colors.white)),
                ),
              ),
              const SizedBox(height: 16),
            ],
          ),
        ),
      ),
    );
  }

  String _formatDate(String? dateStr) {
    if (dateStr == null) return '';
    try {
      final d = DateTime.parse(dateStr);
      return '${d.day}/${d.month}/${d.year} ${d.hour}:${d.minute.toString().padLeft(2, '0')}';
    } catch (_) {
      return dateStr;
    }
  }
}
