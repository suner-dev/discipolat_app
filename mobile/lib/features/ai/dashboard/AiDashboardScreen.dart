import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../../data/services/api_service.dart';

class AiDashboardScreen extends ConsumerStatefulWidget {
  const AiDashboardScreen({super.key});
  @override
  ConsumerState<AiDashboardScreen> createState() => _AiDashboardScreenState();
}

class _AiDashboardScreenState extends ConsumerState<AiDashboardScreen> {
  Map<String, dynamic>? _summary;
  Map<String, dynamic>? _narrative;
  Map<String, dynamic>? _providers;
  bool _isLoading = true;
  String _chatMessage = '';
  String _chatResponse = '';
  bool _chatLoading = false;

  @override
  void initState() {
    super.initState();
    _loadData();
  }

  Future<void> _loadData() async {
    try {
      final api = ref.read(apiServiceProvider);
      final results = await Future.wait([
        api.get('/ai/module/summary').catchError((_) => {}),
        api.get('/ai/module/kpi-narrative').catchError((_) => {}),
        api.get('/ai/module/providers').catchError((_) => {}),
      ]);
      if (mounted) {
        setState(() {
          _summary = results[0] is Map ? results[0] as Map<String, dynamic> : null;
          _narrative = results[1] is Map ? results[1] as Map<String, dynamic> : null;
          final p = results[2] is Map ? results[2] as Map<String, dynamic> : null;
          _providers = p?['providers'] is Map ? p['providers'] as Map<String, dynamic> : null;
          _isLoading = false;
        });
      }
    } catch (e) {
      if (mounted) setState(() => _isLoading = false);
    }
  }

  Future<void> _sendChat() async {
    if (_chatMessage.trim().isEmpty || _chatLoading) return;
    setState(() { _chatLoading = true; _chatResponse = ''; });
    try {
      final api = ref.read(apiServiceProvider);
      final res = await api.post('/ai/module/chat', {'message': _chatMessage});
      if (mounted) {
        setState(() {
          _chatResponse = res is Map ? (res as Map)['response']?.toString() ?? 'Pas de réponse' : 'Pas de réponse';
          _chatMessage = '';
        });
      }
    } catch (e) {
      if (mounted) setState(() => _chatResponse = 'Erreur: ${e.toString()}');
    } finally {
      if (mounted) setState(() => _chatLoading = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Intelligence Artificielle'),
        actions: [IconButton(icon: const Icon(Icons.refresh), onPressed: _loadData)],
      ),
      body: _isLoading
          ? const Center(child: CircularProgressIndicator())
          : RefreshIndicator(onRefresh: _loadData, child: _buildContent()),
    );
  }

  Widget _buildContent() {
    return ListView(
      padding: const EdgeInsets.all(16),
      children: [
        if (_narrative?['headline'] != null)
          Card(
            color: Colors.indigo,
            child: Padding(
              padding: const EdgeInsets.all(16),
              child: Text(_narrative!['headline'],
                style: const TextStyle(color: Colors.white, fontSize: 16, fontWeight: FontWeight.bold)),
            ),
          ),
        const SizedBox(height: 16),
        // Providers status
        if (_providers != null) ...[
          const Text('Modèles IA (gratuits)', style: TextStyle(fontSize: 14, fontWeight: FontWeight.bold)),
          const SizedBox(height: 8),
          ..._buildProviderChips(),
          const SizedBox(height: 16),
        ],
        // Chat with AI
        ...[
          const Text('Assistant IA', style: TextStyle(fontSize: 14, fontWeight: FontWeight.bold)),
          const SizedBox(height: 8),
          TextField(
            hintText: 'Posez une question...',
            onChanged: (v) => _chatMessage = v,
            onSubmitted: (_) => _sendChat(),
          ),
          const SizedBox(height: 8),
          ElevatedButton.icon(
            onPressed: _sendChat,
            icon: const Icon(Icons.send),
            label: _chatLoading ? const Text('...') : const Text('Envoyer'),
          ),
          if (_chatResponse.isNotEmpty) ...[
            const SizedBox(height: 8),
            Container(
              width: double.infinity,
              padding: const EdgeInsets.all(12),
              color: Colors.indigo[50],
              child: SelectableText(_chatResponse),
            ),
          ],
          const SizedBox(height: 16),
        ],
        Row(children: [
          _statCard('Âmes', '${_summary?['totalSouls'] ?? '—'}', Icons.people, Colors.indigo),
          const SizedBox(width: 12),
          _statCard('Familles', '${_summary?['totalFamilies'] ?? '—'}', Icons.family_restroom, Colors.purple),
        ]),
        const SizedBox(height: 16),
        if (_narrative?['highlights'] != null) ...[
          const Text('Points forts', style: TextStyle(fontSize: 16, fontWeight: FontWeight.bold)),
          const SizedBox(height: 8),
          ...(_narrative!['highlights'] as List).map((h) => ListTile(
            leading: const Icon(Icons.check_circle, color: Colors.green, size: 20),
            title: Text(h.toString(), style: const TextStyle(fontSize: 14)),
            dense: true, contentPadding: EdgeInsets.zero,
          )),
        ],
        const SizedBox(height: 16),
        if (_narrative?['recommendations'] != null) ...[
          const Text('Recommandations', style: TextStyle(fontSize: 16, fontWeight: FontWeight.bold)),
          const SizedBox(height: 8),
          ...(_narrative!['recommendations'] as List).map((r) => ListTile(
            leading: const Text('→', style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold)),
            title: Text(r.toString(), style: const TextStyle(fontSize: 14)),
            dense: true, contentPadding: EdgeInsets.zero,
          )),
        ],
        const SizedBox(height: 24),
        const Text('Actions IA', style: TextStyle(fontSize: 16, fontWeight: FontWeight.bold)),
        const SizedBox(height: 8),
        ListTile(
          leading: const Icon(Icons.trending_up, color: Colors.indigo),
          title: const Text('Prédictions'),
          subtitle: const Text('Croissance, risque, engagement'),
          trailing: const Icon(Icons.chevron_right),
          onTap: () => Navigator.pushNamed(context, '/ai/predictions'),
        ),
        ListTile(
          leading: const Icon(Icons.family_restroom, color: Colors.purple),
          title: const Text('Cohésion Familiale'),
          trailing: const Icon(Icons.chevron_right),
          onTap: () => Navigator.pushNamed(context, '/ai/family-cohesion'),
        ),
        ListTile(
          leading: const Icon(Icons.menu_book, color: Colors.amber),
          title: const Text('Assistant Sermon'),
          trailing: const Icon(Icons.chevron_right),
          onTap: () => Navigator.pushNamed(context, '/ai/sermon'),
        ),
      ],
    );
  }

  Widget _statCard(String label, String value, IconData icon, Color color) {
    return Expanded(child: Card(
      child: Padding(padding: const EdgeInsets.all(16), child: Column(children: [
        Icon(icon, color: color, size: 28),
        const SizedBox(height: 8),
        Text(value, style: const TextStyle(fontSize: 24, fontWeight: FontWeight.bold)),
        Text(label, style: TextStyle(fontSize: 12, color: Colors.grey[600])),
      ])),
    ));
  }

  List<Widget> _buildProviderChips() {
    final labels = {
      'groq': 'Groq (Llama 3.1 70B)',
      'gemini': 'Gemini 1.5 Flash',
      'mistral': 'Mistral 7B',
      'huggingface': 'HuggingFace',
      'fallback': 'Mode local',
    };
    return _providers!.entries.map((e) {
      final enabled = e.value == true;
      return Wrap(
        spacing: const EdgeInsets.all(4),
        children: [
          Chip(
            label: Text('${labels[e.key] ?? e.key}${enabled ? ' ✓' : ''}'),
            backgroundColor: enabled ? Colors.green[50] : Colors.grey[100],
            labelStyle: TextStyle(fontSize: 11, color: enabled ? Colors.green[700] : Colors.grey[500]),
          ),
        ],
      );
    }).toList();
  }
}
