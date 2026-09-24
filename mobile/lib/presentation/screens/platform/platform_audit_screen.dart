import 'package:flutter/material.dart';

import '../../../data/services/api_service.dart';
import '../../widgets/app_drawer.dart';
import '../../widgets/glass_theme.dart';
import '../../widgets/secure_screen.dart';

class PlatformAuditScreen extends StatefulWidget {
  const PlatformAuditScreen({super.key, this.apiService});

  final ApiService? apiService;

  @override
  State<PlatformAuditScreen> createState() => _PlatformAuditScreenState();
}

class _PlatformAuditScreenState extends State<PlatformAuditScreen> {
  late final ApiService _api = widget.apiService ?? ApiService();
  List<Map<String, dynamic>> _logs = [];
  int _page = 0;
  int _totalPages = 1;
  bool _loading = true;
  String? _error;

  @override
  void initState() {
    super.initState();
    _load();
  }

  Future<void> _load() async {
    if (mounted) setState(() => _loading = true);
    try {
      final response = await _api.get(
        '/platform/admin/audit-logs',
        queryParameters: {'page': _page, 'size': 50},
      );
      final data = Map<String, dynamic>.from(response.data as Map);
      if (!mounted) return;
      setState(() {
        _logs = List<dynamic>.from(data['content'] as List? ?? [])
            .whereType<Map>()
            .map((item) => Map<String, dynamic>.from(item))
            .toList();
        _totalPages = (data['totalPages'] as num?)?.toInt() ?? 1;
        _error = null;
        _loading = false;
      });
    } catch (_) {
      if (!mounted) return;
      setState(() {
        _error = 'Impossible de charger les journaux plateforme.';
        _loading = false;
      });
    }
  }

  @override
  Widget build(BuildContext context) {
    return SecureScreen(
      screenName: 'PlatformAuditScreen',
      auditAction: 'platformAuditView',
      child: Scaffold(
        appBar: AppBar(
          title: const Text('Audit plateforme'),
          actions: [
            IconButton(onPressed: _load, icon: const Icon(Icons.refresh))
          ],
        ),
        drawer: const AppDrawer(),
        body: _loading
            ? const Center(child: CircularProgressIndicator())
            : _error != null
                ? Center(
                    child: Column(
                      mainAxisSize: MainAxisSize.min,
                      children: [
                        Text(_error!),
                        const SizedBox(height: 12),
                        FilledButton(
                            onPressed: _load, child: const Text('Réessayer')),
                      ],
                    ),
                  )
                : RefreshIndicator(
                    onRefresh: _load,
                    child: ListView(
                      padding: const EdgeInsets.all(16),
                      children: [
                        Text('$_totalPages page(s) d\'historique',
                            style: const TextStyle(color: Colors.white70)),
                        const SizedBox(height: 12),
                        if (_logs.isEmpty)
                          const Padding(
                            padding: EdgeInsets.all(32),
                            child: Center(
                                child: Text('Aucun journal disponible.')),
                          ),
                        ..._logs.map(_logCard),
                        const SizedBox(height: 16),
                        Row(
                          mainAxisAlignment: MainAxisAlignment.spaceBetween,
                          children: [
                            OutlinedButton(
                              onPressed: _page == 0
                                  ? null
                                  : () => setState(() => _page--),
                              child: const Text('Précédent'),
                            ),
                            OutlinedButton(
                              onPressed: _page + 1 >= _totalPages
                                  ? null
                                  : () => setState(() => _page++),
                              child: const Text('Suivant'),
                            ),
                          ],
                        ),
                      ],
                    ),
                  ),
      ),
    );
  }

  Widget _logCard(Map<String, dynamic> log) {
    final action = log['action']?.toString() ?? 'ACTION';
    final entity = log['entityType']?.toString() ?? '—';
    final date = DateTime.tryParse(log['createdAt']?.toString() ?? '');
    return GlassCard(
      margin: const EdgeInsets.only(bottom: 8),
      padding: const EdgeInsets.all(14),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              const Icon(Icons.shield_outlined, color: Colors.indigoAccent),
              const SizedBox(width: 8),
              Expanded(
                  child: Text(action,
                      style: const TextStyle(
                          color: Colors.white, fontWeight: FontWeight.w600))),
              if (date != null)
                Text(_formatDate(date),
                    style:
                        const TextStyle(color: Colors.white54, fontSize: 11)),
            ],
          ),
          const SizedBox(height: 8),
          Text('Entité : $entity',
              style: const TextStyle(color: Colors.white70)),
          Text('Tenant : ${_shortId(log['tenantId'])}',
              style: const TextStyle(color: Colors.white54, fontSize: 12)),
          Text('Utilisateur : ${_shortId(log['userId'])}',
              style: const TextStyle(color: Colors.white54, fontSize: 12)),
          if (log['ipAddress'] != null)
            Text('IP : ${log['ipAddress']}',
                style: const TextStyle(color: Colors.white54, fontSize: 12)),
        ],
      ),
    );
  }

  String _shortId(Object? value) {
    final text = value?.toString() ?? '—';
    return text.length > 8 ? '${text.substring(0, 8)}…' : text;
  }

  String _formatDate(DateTime date) =>
      '${date.day.toString().padLeft(2, '0')}/${date.month.toString().padLeft(2, '0')}/${date.year} ${date.hour.toString().padLeft(2, '0')}:${date.minute.toString().padLeft(2, '0')}';
}
