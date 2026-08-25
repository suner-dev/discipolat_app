import 'package:flutter/material.dart';
import '../../../data/services/api_service.dart';
import '../../../tenant_config.dart';

/// Streaming & Live — branché sur `GET /api/v1/streams`.
class StreamingScreen extends StatefulWidget {
  const StreamingScreen({super.key, this.apiService});

  final ApiService? apiService;

  @override
  State<StreamingScreen> createState() => _StreamingScreenState();
}

class _StreamingScreenState extends State<StreamingScreen> {
  late final ApiService _api = widget.apiService ?? ApiService();
  List<dynamic> _streams = [];
  bool _isLoading = true;
  String? _error;

  List<dynamic> get _live => _streams.where((s) => s['status'] == 'LIVE').toList();

  @override
  void initState() {
    super.initState();
    _load();
  }

  Future<void> _load() async {
    setState(() {
      _isLoading = _streams.isEmpty;
      _error = null;
    });
    try {
      final orgId = await TenantConfig.resolveOrgId();
      final tenantId = int.tryParse(orgId ?? '');
      final res = await _api.get('/streams',
          params: tenantId == null ? null : {'tenantId': tenantId});
      final data = res.data;
      final list = data is List
          ? data
          : (data is Map && data['content'] != null ? data['content'] : []);
      if (mounted) {
        setState(() {
          _streams = List<dynamic>.from(list);
          _isLoading = false;
        });
      }
    } catch (_) {
      if (mounted) {
        setState(() {
          _isLoading = false;
          _error = 'Impossible de charger les streams.';
        });
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Streaming & Live'),
        backgroundColor: Colors.purple.shade600,
        foregroundColor: Colors.white,
      ),
      body: _isLoading
          ? const Center(child: CircularProgressIndicator())
          : _error != null && _streams.isEmpty
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
        Center(
          child: Text(_error ?? 'Erreur',
              textAlign: TextAlign.center, style: const TextStyle(fontSize: 14)),
        ),
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
    final totalViewers = _streams.fold<int>(
        0, (sum, s) => sum + ((s['viewerCount'] as num?)?.toInt() ?? 0));
    return ListView(
      padding: const EdgeInsets.all(16),
      children: [
        if (_live.isNotEmpty) ...[
          Container(
            padding: const EdgeInsets.all(16),
            decoration: BoxDecoration(
              gradient: LinearGradient(
                colors: [Colors.red.shade400, Colors.red.shade700],
              ),
              borderRadius: BorderRadius.circular(16),
            ),
            child: Row(children: [
              const CircleAvatar(
                backgroundColor: Colors.white,
                radius: 8,
                child: Icon(Icons.circle, size: 12, color: Colors.red),
              ),
              const SizedBox(width: 12),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    const Text('EN DIRECT',
                        style: TextStyle(color: Colors.white, fontWeight: FontWeight.bold)),
                    Text('${_live.first['title']} — ${_live.first['viewerCount'] ?? 0} spectateurs',
                        style: const TextStyle(color: Colors.white70, fontSize: 12)),
                  ],
                ),
              ),
              const Icon(Icons.play_circle, color: Colors.white, size: 32),
            ]),
          ),
          const SizedBox(height: 16),
        ],
        Row(children: [
          _statCard(Icons.wifi, '${_live.length}', 'En direct', Colors.red),
          const SizedBox(width: 12),
          _statCard(Icons.people, '$totalViewers', 'Spectateurs', Colors.blue),
          const SizedBox(width: 12),
          _statCard(Icons.visibility, '${_streams.length}', 'Streams', Colors.green),
        ]),
        const SizedBox(height: 20),
        const Text('Prochains streams',
            style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold)),
        const SizedBox(height: 12),
        if (_streams.isEmpty)
          const Text('Aucun stream planifié pour le moment.',
              style: TextStyle(color: Colors.grey))
        else
          ..._streams.map((s) {
            final ended = s['status'] == 'ENDED' || s['status'] == 'CANCELLED';
            final subtitle = s['scheduledAt'] != null
                ? 'Planifié — ${_formatDate(s['scheduledAt'])}'
                : (ended
                    ? 'Terminé'
                    : (s['description']?.toString() ?? 'Planifié'));
            return _streamCard(
                s['title']?.toString() ?? 'Stream', subtitle,
                ended ? Icons.check_circle : Icons.video_library,
                ended ? Colors.grey : Colors.purple);
          }),
      ],
    );
  }

  String _formatDate(Object? d) {
    final t = DateTime.tryParse(d?.toString() ?? '');
    if (t == null) return '';
    return '${t.day.toString().padLeft(2, '0')}/${t.month.toString().padLeft(2, '0')}/${t.year}';
  }

  Widget _statCard(IconData icon, String value, String label, Color color) {
    return Expanded(
      child: Container(
        padding: const EdgeInsets.all(12),
        decoration: BoxDecoration(
          color: color.withOpacity(0.1),
          borderRadius: BorderRadius.circular(12),
        ),
        child: Column(
          children: [
            Icon(icon, color: color, size: 24),
            const SizedBox(height: 4),
            Text(value, style: TextStyle(fontSize: 20, fontWeight: FontWeight.bold, color: color)),
            Text(label, style: TextStyle(fontSize: 10, color: color)),
          ],
        ),
      ),
    );
  }

  Widget _streamCard(String title, String subtitle, IconData icon, Color color) {
    return Card(
      margin: const EdgeInsets.only(bottom: 8),
      child: ListTile(
        leading: CircleAvatar(backgroundColor: color.withOpacity(0.1), child: Icon(icon, color: color)),
        title: Text(title),
        subtitle: Text(subtitle, style: const TextStyle(fontSize: 12)),
        trailing: const Icon(Icons.chevron_right),
      ),
    );
  }
}
