import 'package:flutter/material.dart';
import '../../../data/services/api_service.dart';
import '../../widgets/app_drawer.dart';

/// Dress Code Screen — view dress codes for events/services.
/// Connected to API: /api/v1/dress-codes
class DressCodeScreen extends StatefulWidget {
  const DressCodeScreen({super.key});

  @override
  State<DressCodeScreen> createState() => _DressCodeScreenState();
}

class _DressCodeScreenState extends State<DressCodeScreen> {
  final _apiService = ApiService();
  List<dynamic> _dressCodes = [];
  bool _isLoading = true;
  String? _error;

  @override
  void initState() {
    super.initState();
    _load();
  }

  Future<void> _load() async {
    setState(() {
      _isLoading = _dressCodes.isEmpty;
      _error = null;
    });
    try {
      final res = await _apiService.get('/dress-codes');
      final data = res.data;
      final list = data is List ? data : (data is Map && data['content'] != null ? data['content'] : []);
      if (mounted) {
        setState(() {
          _dressCodes = List<dynamic>.from(list);
          _isLoading = false;
        });
      }
    } catch (_) {
      if (mounted) {
        setState(() {
          _isLoading = false;
          _error = 'Impossible de charger les dress codes';
        });
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Dress Codes'),
        backgroundColor: Colors.purple.shade600,
        foregroundColor: Colors.white,
      ),
      drawer: const AppDrawer(),
      body: _isLoading
          ? const Center(child: CircularProgressIndicator())
          : _error != null
              ? _errorView()
              : RefreshIndicator(onRefresh: _load, child: _content()),
    );
  }

  Widget _errorView() {
    return ListView(
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
            label: const Text('Réessayer'),
          ),
        ),
      ],
    );
  }

  Widget _content() {
    if (_dressCodes.isEmpty) {
      return const Center(child: Text('Aucun dress code'));
    }
    return ListView.builder(
      padding: const EdgeInsets.all(16),
      itemCount: _dressCodes.length,
      itemBuilder: (context, index) {
        final dc = _dressCodes[index];
        final rules = dc['rules'] as List<dynamic>? ?? [];
        return Card(
          margin: const EdgeInsets.only(bottom: 12),
          child: Padding(
            padding: const EdgeInsets.all(16),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(dc['title'] ?? '', style: const TextStyle(fontSize: 16, fontWeight: FontWeight.bold)),
                const SizedBox(height: 4),
                Text('${dc['serviceName'] ?? ''} • ${dc['status'] ?? ''}',
                    style: TextStyle(color: Colors.grey.shade600)),
                if (rules.isNotEmpty) ...[
                  const SizedBox(height: 8),
                  Wrap(
                    spacing: 8,
                    runSpacing: 4,
                    children: rules.map<Widget>((r) {
                      return Chip(
                        label: Text(r['groupName'] ?? '', style: const TextStyle(fontSize: 12)),
                        backgroundColor: Colors.purple.shade50,
                      );
                    }).toList(),
                  ),
                ],
              ],
            ),
          ),
        );
      },
    );
  }
}
