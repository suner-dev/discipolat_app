import 'package:flutter/material.dart';
import '../../data/services/api_service.dart';
import '../../../presentation/widgets/glass_theme.dart';
import '../../../presentation/widgets/app_drawer.dart';

class ConversationsScreen extends StatefulWidget {
  const ConversationsScreen({super.key});

  @override
  State<ConversationsScreen> createState() => _ConversationsScreenState();
}

class _ConversationsScreenState extends State<ConversationsScreen> {
  final _apiService = ApiService();
  List<dynamic> _conversations = [];
  bool _isLoading = true;
  String? _error;

  @override
  void initState() {
    super.initState();
    _loadData();
  }

  Future<void> _loadData() async {
    setState(() { _isLoading = true; _error = null; });
    try {
      final res = await _apiService.get('/conversations');
      if (mounted) {
        final data = res.data;
        setState(() {
          _conversations = data is List ? data : (data is Map && data['content'] is List ? data['content'] as List<dynamic> : <dynamic>[]);
          _isLoading = false;
        });
      }
    } catch (_) {
      if (mounted) setState(() { _error = 'Erreur de chargement'; _isLoading = false; });
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Conversations'), backgroundColor: const Color(0xFF0A0E27)),
      drawer: const AppDrawer(),
      body: Container(
        decoration: const BoxDecoration(gradient: LinearGradient(begin: Alignment.topLeft, end: Alignment.bottomRight, colors: [Color(0xFF0A0E27), Color(0xFF16213A)])),
        child: _isLoading
            ? const Center(child: CircularProgressIndicator())
            : _error != null
                ? Center(child: Text(_error!, style: const TextStyle(color: Colors.white54)))
                : _conversations.isEmpty
                    ? const Center(child: Text('Aucune conversation', style: TextStyle(color: Colors.white54)))
                    : ListView.builder(
                        padding: const EdgeInsets.all(16),
                        itemCount: _conversations.length,
                        itemBuilder: (ctx, i) {
                          final c = _conversations[i];
                          return Card(
                            color: Colors.white.withValues(alpha: 0.05),
                            margin: const EdgeInsets.only(bottom: 8),
                            child: ListTile(
                              leading: CircleAvatar(backgroundColor: Colors.blue.withValues(alpha: 0.2), child: const Icon(Icons.person, color: Colors.blue)),
                              title: Text(c['participantName'] ?? 'Conversation', style: const TextStyle(color: Colors.white)),
                              subtitle: Text(c['lastMessage'] ?? '', style: TextStyle(color: Colors.white.withValues(alpha: 0.5)), maxLines: 1, overflow: TextOverflow.ellipsis),
                              trailing: (c['unreadCount'] ?? 0) > 0
                                  ? CircleAvatar(radius: 12, backgroundColor: Colors.blue, child: Text('${c['unreadCount']}', style: const TextStyle(color: Colors.white, fontSize: 11)))
                                  : null,
                            ),
                          );
                        },
                      ),
      ),
    );
  }
}