import 'package:flutter/material.dart';

import '../../../data/services/api_service.dart';
import '../../widgets/app_drawer.dart';
import '../../widgets/glass_theme.dart';
import '../../widgets/secure_screen.dart';

class PlatformRegistrationRequestsScreen extends StatefulWidget {
  const PlatformRegistrationRequestsScreen({super.key, this.apiService});

  final ApiService? apiService;

  @override
  State<PlatformRegistrationRequestsScreen> createState() =>
      _PlatformRegistrationRequestsScreenState();
}

class _PlatformRegistrationRequestsScreenState
    extends State<PlatformRegistrationRequestsScreen> {
  late final ApiService _api = widget.apiService ?? ApiService();
  List<Map<String, dynamic>> _requests = [];
  int _page = 0;
  int _totalPages = 1;
  final _reasonController = TextEditingController();
  bool _loading = true;
  String? _error;

  @override
  void dispose() {
    _reasonController.dispose();
    super.dispose();
  }

  @override
  void initState() {
    super.initState();
    _load();
  }

  Future<void> _load() async {
    if (mounted) setState(() => _loading = true);
    try {
      final response = await _api.get(
        '/platform/admin/registration-requests',
        queryParameters: {'page': _page, 'size': 50},
      );
      final data = Map<String, dynamic>.from(response.data as Map);
      if (!mounted) return;
      setState(() {
        _requests = List<dynamic>.from(data['content'] as List? ?? [])
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
        _error = 'Impossible de charger les demandes.';
        _loading = false;
      });
    }
  }

  Future<void> _decide(Map<String, dynamic> request, String action) async {
    _reasonController.clear();
    final reason = await showDialog<String>(
      context: context,
      builder: (context) => AlertDialog(
        title: Text(action == 'approve'
            ? 'Approuver la demande'
            : 'Rejeter la demande'),
        content: TextField(
          controller: _reasonController,
          maxLines: 3,
          decoration: const InputDecoration(labelText: 'Motif'),
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(context),
            child: const Text('Annuler'),
          ),
          FilledButton(
            onPressed: () =>
                Navigator.pop(context, _reasonController.text.trim()),
            child: const Text('Confirmer'),
          ),
        ],
      ),
    );
    if (reason == null || reason.isEmpty) return;
    try {
      await _api.post(
        '/platform/admin/registration-requests/${request['id']}/$action',
        data: {'reason': reason},
      );
      await _load();
    } catch (_) {
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Action impossible.')),
      );
    }
  }

  @override
  Widget build(BuildContext context) {
    return SecureScreen(
      screenName: 'PlatformRegistrationRequestsScreen',
      auditAction: 'platformRegistrationRequestsView',
      child: Scaffold(
        appBar: AppBar(
          title: const Text('Demandes d\'églises'),
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
                        if (_requests.isEmpty)
                          const Padding(
                            padding: EdgeInsets.all(32),
                            child: Center(
                                child: Text('Aucune demande en attente.')),
                          ),
                        ..._requests.map(_requestCard),
                        const SizedBox(height: 16),
                        Row(
                          mainAxisAlignment: MainAxisAlignment.spaceBetween,
                          children: [
                            OutlinedButton(
                              onPressed: _page == 0
                                  ? null
                                  : () {
                                      setState(() => _page--);
                                      _load();
                                    },
                              child: const Text('Précédent'),
                            ),
                            OutlinedButton(
                              onPressed: _page + 1 >= _totalPages
                                  ? null
                                  : () {
                                      setState(() => _page++);
                                      _load();
                                    },
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

  Widget _requestCard(Map<String, dynamic> request) {
    final date = DateTime.tryParse(request['createdAt']?.toString() ?? '');
    final name = [
      request['firstName']?.toString() ?? '',
      request['lastName']?.toString() ?? '',
    ].where((value) => value.isNotEmpty).join(' ');
    return GlassCard(
      margin: const EdgeInsets.only(bottom: 8),
      padding: const EdgeInsets.all(14),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(request['organizationName']?.toString() ?? 'Organisation',
              style: const TextStyle(
                  color: Colors.white, fontWeight: FontWeight.w600)),
          const SizedBox(height: 4),
          Text(name, style: const TextStyle(color: Colors.white70)),
          Text(request['email']?.toString() ?? '',
              style: const TextStyle(color: Colors.white54)),
          if (date != null)
            Text('Demandée le ${date.day}/${date.month}/${date.year}',
                style: const TextStyle(color: Colors.white54, fontSize: 12)),
          const SizedBox(height: 8),
          Row(
            children: [
              FilledButton(
                onPressed: () => _decide(request, 'approve'),
                child: const Text('Approuver'),
              ),
              const SizedBox(width: 8),
              OutlinedButton(
                onPressed: () => _decide(request, 'reject'),
                child: const Text('Rejeter'),
              ),
            ],
          ),
        ],
      ),
    );
  }
}
