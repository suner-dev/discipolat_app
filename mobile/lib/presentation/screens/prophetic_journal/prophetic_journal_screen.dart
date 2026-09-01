import 'package:flutter/material.dart';
import '../../../data/services/api_service.dart';
import '../../widgets/glass_theme.dart';
import '../../../../l10n/app_localizations.dart';

/// Journal Prophétique — visions, rêves & révélations avec corrélation IA.
/// Branché sur GET/POST/PUT /api/v1/prophetic. Accessible aux rôles pastoraux.
class PropheticJournalScreen extends StatefulWidget {
  const PropheticJournalScreen({super.key, this.apiService});
  final ApiService? apiService;

  @override
  State<PropheticJournalScreen> createState() => _PropheticJournalScreenState();
}

const _entryTypes = <String, (String, IconData, Color)>{
  'VISION': ('Vision', Icons.visibility, Color(0xFF6366F1)),
  'REVE': ('Rêve', Icons.nights_stay, Color(0xFF64748B)),
  'PROPHETIE': ('Prophétie', Icons.auto_awesome, Color(0xFFF59E0B)),
  'CONVICTION': ('Conviction', Icons.local_fire_department, Color(0xFFEF4444)),
  'AVERTISMENT': ('Avertissement', Icons.cloud, Color(0xFF6B7280)),
  'ENCOURAGEMENT': ('Encouragement', Icons.favorite, Color(0xFF10B981)),
  'REVELATION': ('Révélation', Icons.lightbulb, Color(0xFFFBBF24)),
};

class _PropheticJournalScreenState extends State<PropheticJournalScreen> {
  late final ApiService _api = widget.apiService ?? ApiService();
  List<dynamic> _entries = [];
  bool _loading = true;
  bool _tabPublic = false;
  String? _error;
  bool _showForm = false;
  bool _saving = false;

  final _titleCtrl = TextEditingController();
  final _contentCtrl = TextEditingController();
  final _tagsCtrl = TextEditingController();
  String _type = 'VISION';
  bool _isPublic = false;
  String? _correlating;

  @override
  void initState() {
    super.initState();
    _load();
  }

  @override
  void dispose() {
    _titleCtrl.dispose();
    _contentCtrl.dispose();
    _tagsCtrl.dispose();
    super.dispose();
  }

  Future<void> _load() async {
    setState(() {
      _loading = true;
      _error = null;
    });
    try {
      final res = await _api.get(_tabPublic ? '/prophetic/public' : '/prophetic/mine');
      final d = res.data;
      setState(() {
        _entries = d is List ? d : <dynamic>[];
        _loading = false;
      });
    } catch (_) {
      if (!mounted) return;
      setState(() {
        _error = AppLocalizations.of(context).predictionsError;
        _loading = false;
      });
    }
  }

  Future<void> _save() async {
    if (_titleCtrl.text.trim().isEmpty || _contentCtrl.text.trim().isEmpty) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Titre et contenu requis')),
      );
      return;
    }
    setState(() => _saving = true);
    try {
      await _api.post('/prophetic', data: {
        'type': _type,
        'title': _titleCtrl.text.trim(),
        'content': _contentCtrl.text.trim(),
        'tags': _tagsCtrl.text.trim().isEmpty ? null : _tagsCtrl.text.trim(),
        'isPublic': _isPublic,
      });
      if (!mounted) return;
      setState(() {
        _showForm = false;
        _saving = false;
      });
      _titleCtrl.clear();
      _contentCtrl.clear();
      _tagsCtrl.clear();
      _isPublic = false;
      _load();
    } catch (_) {
      if (!mounted) return;
      setState(() => _saving = false);
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text("Impossible d'enregistrer l'entrée")),
      );
    }
  }

  Future<void> _showCorrelation(String id) async {
    setState(() => _correlating = id);
    try {
      final res = await _api.get('/prophetic/$id/correlated');
      final d = res.data;
      if (!mounted) return;
      setState(() => _correlating = null);
      final correlated = d is List ? d : <dynamic>[];
      showModalBottomSheet(
        context: context,
        isScrollControlled: true,
        backgroundColor: const Color(0xFF0F172A),
        builder: (ctx) => _CorrelationSheet(entries: correlated),
      );
    } catch (_) {
      if (!mounted) return;
      setState(() => _correlating = null);
    }
  }

  @override
  Widget build(BuildContext context) {
    final typeMeta = (key: String) =>
        _entryTypes[key] ?? ('Révélation', Icons.lightbulb, const Color(0xFFFBBF24));
    return Scaffold(
      backgroundColor: const Color(0xFF0F172A),
      appBar: AppBar(
        title: const Text('Journal Prophétique',
            style: TextStyle(fontWeight: FontWeight.bold)),
        backgroundColor: const Color(0xFF1E293B),
        foregroundColor: Colors.white,
        actions: [
          IconButton(
            tooltip: 'Nouvelle entrée',
            onPressed: () => setState(() => _showForm = !_showForm),
            icon: const Icon(Icons.add),
          ),
        ],
      ),
      body: Column(
        children: [
          Padding(
            padding: const EdgeInsets.fromLTRB(16, 8, 16, 4),
            child: Row(
              children: [
                _tabChip('Mes entrées', !_tabPublic,
                    () => setState(() {
                      _tabPublic = false;
                      _load();
                    })),
                const SizedBox(width: 8),
                _tabChip('Partagées', _tabPublic,
                    () => setState(() {
                      _tabPublic = true;
                      _load();
                    })),
              ],
            ),
          ),
          if (_showForm) _buildForm(),
          Expanded(
            child: _loading
                ? const Center(child: CircularProgressIndicator())
                : _error != null
                    ? Center(
                        child: Column(
                          mainAxisSize: MainAxisSize.min,
                          children: [
                            Padding(
                              padding: const EdgeInsets.all(24),
                              child: Text(_error!,
                                  style: const TextStyle(color: Colors.white70),
                                  textAlign: TextAlign.center),
                            ),
                            ElevatedButton(
                                onPressed: _load,
                                child:
                                    Text(AppLocalizations.of(context).retry)),
                          ],
                        ),
                      )
                    : _entries.isEmpty
                        ? const Center(
                            child: Text('Aucune entrée pour le moment',
                                style: TextStyle(color: Colors.white54)))
                        : RefreshIndicator(
                            onRefresh: _load,
                            child: ListView.builder(
                              padding: const EdgeInsets.all(14),
                              itemCount: _entries.length,
                              itemBuilder: (context, i) {
                                final e =
                                    _entries[i] as Map<String, dynamic>;
                                final type = e['type']?.toString() ?? 'REVELATION';
                                final meta = typeMeta(type);
                                final isPublic = e['isPublic'] == true;
                                return GlassCard(
                                  margin: const EdgeInsets.only(bottom: 12),
                                  padding: const EdgeInsets.all(14),
                                  child: Column(
                                    crossAxisAlignment:
                                        CrossAxisAlignment.start,
                                    children: [
                                      Row(
                                        children: [
                                          Container(
                                            padding: const EdgeInsets.all(8),
                                            decoration: BoxDecoration(
                                              gradient: LinearGradient(
                                                  colors: [
                                                    meta.$3,
                                                    meta.$3.withValues(
                                                        alpha: 0.7)
                                                  ]),
                                              borderRadius:
                                                  BorderRadius.circular(10),
                                            ),
                                            child: Icon(meta.$2,
                                                color: Colors.white,
                                                size: 18),
                                          ),
                                          const SizedBox(width: 10),
                                          Expanded(
                                            child: Column(
                                              crossAxisAlignment:
                                                  CrossAxisAlignment.start,
                                              children: [
                                                Text(
                                                  e['title']?.toString() ??
                                                      'Sans titre',
                                                  style: const TextStyle(
                                                      color: Colors.white,
                                                      fontWeight:
                                                          FontWeight.w600,
                                                      fontSize: 14),
                                                ),
                                                Text(
                                                  meta.$1.toUpperCase(),
                                                  style: TextStyle(
                                                      color: meta.$3,
                                                      fontSize: 10,
                                                      fontWeight:
                                                          FontWeight.w700),
                                                ),
                                              ],
                                            ),
                                          ),
                                          Icon(
                                            isPublic
                                                ? Icons.public
                                                : Icons.lock,
                                            color: isPublic
                                                ? Colors.greenAccent
                                                : Colors.white24,
                                            size: 16,
                                          ),
                                        ],
                                      ),
                                      const SizedBox(height: 10),
                                      Text(
                                        e['content']?.toString() ?? '',
                                        style: const TextStyle(
                                            color: Colors.white70,
                                            fontSize: 13,
                                            height: 1.4),
                                      ),
                                      if ((e['tags']?.toString() ?? '')
                                          .trim()
                                          .isNotEmpty) ...[
                                        const SizedBox(height: 8),
                                        Wrap(
                                          spacing: 6,
                                          children: (e['tags']
                                                  .toString()
                                                  .split(RegExp(r'[,;]')))
                                              .map((t) => t.trim())
                                              .where((t) => t.isNotEmpty)
                                              .map((t) => Container(
                                                    padding: const EdgeInsets
                                                        .symmetric(
                                                        horizontal: 8,
                                                        vertical: 3),
                                                    decoration: BoxDecoration(
                                                      color: Colors.white
                                                          .withValues(
                                                              alpha: 0.06),
                                                      borderRadius:
                                                          BorderRadius.circular(
                                                              12),
                                                    ),
                                                    child: Text('#$t',
                                                        style: TextStyle(
                                                            color: Colors.white
                                                                .withValues(
                                                                    alpha:
                                                                        0.6),
                                                            fontSize: 11)),
                                                  ))
                                              .toList(),
                                        ),
                                      ),
                                      const SizedBox(height: 8),
                                      Row(
                                        children: [
                                          Text(
                                            _formatDate(e['createdAt']
                                                ?.toString()),
                                            style: TextStyle(
                                                color: Colors.white
                                                    .withValues(alpha: 0.4),
                                                fontSize: 11),
                                          ),
                                          const Spacer(),
                                          TextButton.icon(
                                            onPressed: _correlating ==
                                                    e['id']?.toString()
                                                ? null
                                                : () => _showCorrelation(
                                                    e['id'].toString()),
                                            icon: _correlating ==
                                                    e['id']?.toString()
                                                ? const SizedBox(
                                                    width: 14,
                                                    height: 14,
                                                    child:
                                                        CircularProgressIndicator(
                                                            strokeWidth: 2))
                                                : const Icon(Icons.link,
                                                    size: 14),
                                            label: const Text('Corrélations',
                                                style: TextStyle(
                                                    fontSize: 11)),
                                          ),
                                        ],
                                      ),
                                    ],
                                  ),
                                );
                              },
                            ),
                          ),
          ),
        ],
      ),
    );
  }

  Widget _tabChip(String label, bool active, VoidCallback onTap) {
    return GestureDetector(
      onTap: onTap,
      child: Container(
        padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 7),
        decoration: BoxDecoration(
          color: active
              ? const Color(0xFF6366F1)
              : Colors.white.withValues(alpha: 0.06),
          borderRadius: BorderRadius.circular(20),
        ),
        child: Text(
          label,
          style: TextStyle(
              color: active ? Colors.white : Colors.white60,
              fontSize: 12,
              fontWeight: FontWeight.w600),
        ),
      ),
    );
  }

  Widget _buildForm() {
    return Container(
      margin: const EdgeInsets.all(12),
      padding: const EdgeInsets.all(14),
      decoration: BoxDecoration(
        color: const Color(0xFF1E293B),
        borderRadius: BorderRadius.circular(16),
        border: Border.all(color: Colors.white.withValues(alpha: 0.06)),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const Text('Nouvelle entrée',
              style: TextStyle(
                  color: Colors.white, fontWeight: FontWeight.bold)),
          const SizedBox(height: 10),
          DropdownButtonFormField<String>(
            value: _type,
            dropdownColor: const Color(0xFF1E293B),
            decoration: _fieldDecoration('Type'),
            style: const TextStyle(color: Colors.white, fontSize: 13),
            items: _entryTypes.entries
                .map((e) => DropdownMenuItem(
                      value: e.key,
                      child: Text(e.value.$1),
                    ))
                .toList(),
            onChanged: (v) => setState(() => _type = v ?? 'VISION'),
          ),
          const SizedBox(height: 8),
          TextField(
            controller: _titleCtrl,
            style: const TextStyle(color: Colors.white, fontSize: 13),
            decoration: _fieldDecoration('Titre'),
          ),
          const SizedBox(height: 8),
          TextField(
            controller: _contentCtrl,
            maxLines: 4,
            style: const TextStyle(color: Colors.white, fontSize: 13),
            decoration: _fieldDecoration('Contenu...'),
          ),
          const SizedBox(height: 8),
          TextField(
            controller: _tagsCtrl,
            style: const TextStyle(color: Colors.white, fontSize: 13),
            decoration: _fieldDecoration('Tags (séparés par virgules)'),
          ),
          const SizedBox(height: 8),
          SwitchListTile(
            value: _isPublic,
            onChanged: (v) => setState(() => _isPublic = v),
            title: const Text('Partager publiquement',
                style: TextStyle(color: Colors.white70, fontSize: 13)),
            contentPadding: EdgeInsets.zero,
            activeTrackColor: const Color(0xFF6366F1),
          ),
          const SizedBox(height: 4),
          Row(
            mainAxisAlignment: MainAxisAlignment.end,
            children: [
              TextButton(
                onPressed: () => setState(() => _showForm = false),
                child: const Text('Annuler',
                    style: TextStyle(color: Colors.white54)),
              ),
              const SizedBox(width: 8),
              ElevatedButton(
                onPressed: _saving ? null : _save,
                style: ElevatedButton.styleFrom(
                    backgroundColor: const Color(0xFF6366F1)),
                child: _saving
                    ? const SizedBox(
                        width: 16,
                        height: 16,
                        child: CircularProgressIndicator(
                            strokeWidth: 2, color: Colors.white))
                    : const Text('Enregistrer'),
              ),
            ],
          ),
        ],
      ),
    );
  }

  InputDecoration _fieldDecoration(String hint) {
    return InputDecoration(
      hintText: hint,
      hintStyle: const TextStyle(color: Colors.white38, fontSize: 13),
      filled: true,
      fillColor: Colors.white.withValues(alpha: 0.04),
      border: OutlineInputBorder(
        borderRadius: BorderRadius.circular(10),
        borderSide: BorderSide.none,
      ),
      contentPadding: const EdgeInsets.symmetric(horizontal: 12, vertical: 10),
    );
  }

  String _formatDate(String? iso) {
    if (iso == null || iso.isEmpty) return '';
    try {
      final dt = DateTime.parse(iso).toLocal();
      final months = [
        'jan', 'fév', 'mar', 'avr', 'mai', 'juin',
        'juil', 'août', 'sep', 'oct', 'nov', 'déc'
      ];
      return '${dt.day} ${months[dt.month - 1]} ${dt.year}';
    } catch (_) {
      return iso;
    }
  }
}

class _CorrelationSheet extends StatelessWidget {
  const _CorrelationSheet({required this.entries});
  final List<dynamic> entries;

  @override
  Widget build(BuildContext context) {
    return SafeArea(
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            const Text('Entrées corrélées',
                style: TextStyle(
                    color: Colors.white,
                    fontWeight: FontWeight.bold,
                    fontSize: 16)),
            const SizedBox(height: 8),
            if (entries.isEmpty)
              const Text('Aucune corrélation détectée',
                  style: TextStyle(color: Colors.white54))
            else
              Flexible(
                child: ListView.builder(
                  shrinkWrap: true,
                  itemCount: entries.length,
                  itemBuilder: (context, i) {
                    final e = entries[i] as Map<String, dynamic>;
                    return ListTile(
                      title: Text(e['title']?.toString() ?? '',
                          style: const TextStyle(color: Colors.white)),
                      subtitle: Text(
                        e['content']?.toString() ?? '',
                        maxLines: 2,
                        overflow: TextOverflow.ellipsis,
                        style: const TextStyle(color: Colors.white54),
                      ),
                    );
                  },
                ),
              ),
          const SizedBox(height: 8),
          Center(
            child: TextButton(
              onPressed: () => Navigator.of(context).pop(),
              child: const Text('Fermer'),
            ),
          ),
        ],
      ),
    );
  }
}