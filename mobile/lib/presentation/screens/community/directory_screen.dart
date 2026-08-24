import 'package:flutter/material.dart';
import '../../../data/services/api_service.dart';

/// Church Directory screen
class DirectoryScreen extends StatefulWidget {
  const DirectoryScreen({super.key});

  @override
  State<DirectoryScreen> createState() => _DirectoryScreenState();
}

class _DirectoryScreenState extends State<DirectoryScreen> {
  final _api = ApiService();
  List<dynamic> entries = [];
  bool loading = true;

  @override
  void initState() {
    super.initState();
    _loadDirectory();
  }

  Future<void> _loadDirectory() async {
    try {
      final res = await _api.get('/directory');
      setState(() {
        entries = (res.data['content'] ?? res.data ?? []) as List;
        loading = false;
      });
    } catch (_) {
      setState(() => loading = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Annuaire de l\'Église')),
      body: loading
          ? const Center(child: CircularProgressIndicator())
          : entries.isEmpty
              ? const Center(child: Text('Aucun membre dans l\'annuaire'))
              : ListView.builder(
                  padding: const EdgeInsets.all(16),
                  itemCount: entries.length,
                  itemBuilder: (context, index) {
                    final entry = entries[index];
                    return Card(
                      margin: const EdgeInsets.only(bottom: 8),
                      child: ListTile(
                        leading: CircleAvatar(
                          backgroundColor: Colors.indigo.shade100,
                          child: const Icon(Icons.person, color: Colors.indigo),
                        ),
                        title: Text(entry['département'] ?? 'Membre'),
                        subtitle: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            if (entry['bio'] != null)
                              Text(entry['bio'], maxLines: 2, overflow: TextOverflow.ellipsis, style: const TextStyle(fontSize: 12)),
                            if (entry['téléphone'] != null)
                              Text(entry['téléphone'], style: const TextStyle(fontSize: 11, color: Colors.grey)),
                          ],
                        ),
                      ),
                    );
                  },
                ),
    );
  }
}
