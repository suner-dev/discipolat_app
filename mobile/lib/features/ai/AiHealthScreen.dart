import 'package:flutter/material.dart';
import '../../data/services/api_service.dart';
import '../../../presentation/widgets/glass_theme.dart';
import '../../../presentation/widgets/app_drawer.dart';

/// Santé IA — branché sur GET /api/v1/ai/health.
class AiHealthScreen extends StatefulWidget {
  const AiHealthScreen({super.key});

  @override
  State<AiHealthScreen> createState() => _AiHealthScreenState();
}

class _AiHealthScreenState extends State<AiHealthScreen> {
  final _apiService = ApiService();
  Map<String, dynamic>? _health;
  bool _isLoading = true;
  String? _error;

  @override
  void initState() {
    super.initState();
    _loadData();
  }

  Future<void> _loadData() async {
    setState(() {
      _isLoading = true;
      _error = null;
    });
    try {
      final res = await _apiService.get('/ai/health');
      if (mounted) {
        setState(() {
          _health = res.data is Map<String, dynamic> ? res.data as Map<String, dynamic> : null;
          _isLoading = false;
        });
      }
    } catch (_) {
      if (mounted) {
        setState(() {
          _error = 'Erreur lors du chargement';
          _isLoading = false;
        });
      }
    }
  }

  Color _statusColor(String? s) {
    switch (s?.toUpperCase()) {
      case 'UP':
      case 'HEALTHY':
        return Colors.green;
      case 'DEGRADED':
        return Colors.orange;
      case 'DOWN':
        return Colors.red;
      default:
        return Colors.grey;
    }
  }

  @override
  Widget build(BuildContext context) {
    final status = _health?['status']?.toString() ?? 'UNKNOWN';

    return Scaffold(
      appBar: AppBar(
        title: const Text('Santé IA'),
        backgroundColor: Colors.purple,
        foregroundColor: Colors.white,
        actions: [IconButton(icon: const Icon(Icons.refresh), onPressed: _loadData)],
      ),
      drawer: const AppDrawer(),
      body: _isLoading
          ? const Center(child: CircularProgressIndicator())
          : _error != null
              ? _buildError()
              : RefreshIndicator(
                  onRefresh: _loadData,
                  child: ListView(
                    padding: const EdgeInsets.all(16),
                    children: [
                      GlassCard(
                        padding: const EdgeInsets.all(24),
                        child: Column(
                          children: [
                            Icon(
                              status.toUpperCase() == 'UP' ? Icons.check_circle : Icons.error_outline,
                              size: 64,
                              color: _statusColor(status),
                            ),
                            const SizedBox(height: 16),
                            Text(
                              'Statut: $status',
                              style: TextStyle(color: _statusColor(status), fontSize: 18, fontWeight: FontWeight.bold),
                            ),
                          ],
                        ),
                      ),
                      const SizedBox(height: 16),
                      if (_health != null)
                        ...(_health!.entries.where((e) => e.key != 'status').map((e) => GlassCard(
                              margin: const EdgeInsets.only(bottom: 8),
                              padding: const EdgeInsets.all(14),
                              child: Row(
                                children: [
                                  Expanded(
                                      child: Text(e.key, style: TextStyle(color: Colors.white.withValues(alpha: 0.7), fontSize: 13))),
                                  Text('${e.value}', style: const TextStyle(color: Colors.white, fontSize: 13, fontWeight: FontWeight.w600)),
                                ],
                              ),
                            ))),
                    ],
                  ),
                ),
    );
  }

  Widget _buildError() {
    return Center(
      child: Column(
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          Icon(Icons.error_outline, color: Colors.white.withValues(alpha: 0.3), size: 48),
          const SizedBox(height: 12),
          Text(_error!, style: TextStyle(color: Colors.white.withValues(alpha: 0.5)), textAlign: TextAlign.center),
          const SizedBox(height: 12),
          FilledButton.icon(onPressed: _loadData, icon: const Icon(Icons.refresh, size: 16), label: const Text('Réessayer')),
        ],
      ),
    );
  }
}
