import 'package:flutter/material.dart';
import '../../data/services/api_service.dart';
import '../../../presentation/widgets/glass_theme.dart';
import '../../../presentation/widgets/app_drawer.dart';

/// Géofencing — historique et auto-check-in — branché sur /api/v1/geofencing.
class GeofencingScreen extends StatefulWidget {
  const GeofencingScreen({super.key});

  @override
  State<GeofencingScreen> createState() => _GeofencingScreenState();
}

class _GeofencingScreenState extends State<GeofencingScreen> {
  final _apiService = ApiService();
  List<dynamic> _history = [];
  bool _isLoading = true;
  String? _error;
  bool _isCheckingIn = false;

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
      final res = await _apiService.get('/geofencing/history/all');
      if (mounted) {
        final data = res.data;
        setState(() {
          _history = data is List
              ? data
              : (data is Map && data['content'] is List
                  ? data['content'] as List<dynamic>
                  : <dynamic>[]);
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

  Future<void> _autoCheckIn() async {
    setState(() => _isCheckingIn = true);
    try {
      await _apiService.post('/geofencing/auto-check-in');
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('Auto-check-in effectué'), backgroundColor: Colors.green),
        );
        _loadData();
      }
    } catch (_) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('Erreur lors de l\'auto-check-in'), backgroundColor: Colors.red),
        );
      }
    } finally {
      if (mounted) setState(() => _isCheckingIn = false);
    }
  }

  Color _typeColor(String? type) {
    switch (type?.toUpperCase()) {
      case 'CHECK_IN':
      case 'ENTREE':
        return Colors.green;
      case 'CHECK_OUT':
      case 'SORTIE':
        return Colors.red;
      case 'AUTO':
        return Colors.blue;
      default:
        return Colors.grey;
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Géofencing'),
        backgroundColor: AppColors.primary,
        foregroundColor: Colors.white,
        actions: [IconButton(icon: const Icon(Icons.refresh), onPressed: _loadData)],
      ),
      drawer: const AppDrawer(),
      floatingActionButton: FloatingActionButton.extended(
        onPressed: _isCheckingIn ? null : _autoCheckIn,
        icon: _isCheckingIn
            ? const SizedBox(width: 16, height: 16, child: CircularProgressIndicator(strokeWidth: 2, color: Colors.white))
            : const Icon(Icons.location_on, size: 18),
        label: Text(_isCheckingIn ? 'Vérification...' : 'Auto check-in'),
        backgroundColor: AppColors.primary,
      ),
      body: _isLoading
          ? const ShimmerLoading(itemCount: 5)
          : _error != null
              ? _buildError()
              : RefreshIndicator(
                  onRefresh: _loadData,
                  child: _history.isEmpty
                      ? _buildEmpty()
                      : ListView.builder(
                          padding: const EdgeInsets.all(16),
                          itemCount: _history.length,
                          itemBuilder: (context, i) {
                            final entry = _history[i] as Map<String, dynamic>;
                            final lieu = entry['lieu']?.toString() ?? entry['location']?.toString() ?? 'Lieu inconnu';
                            final type = entry['type']?.toString() ?? entry['action']?.toString() ?? '';
                            final dateStr = entry['date']?.toString().substring(0, 16) ??
                                entry['createdAt']?.toString().substring(0, 16) ?? '';
                            final membre = entry['membre']?.toString() ?? entry['memberName']?.toString() ?? '';

                            return GlassCard(
                              margin: const EdgeInsets.only(bottom: 8),
                              padding: const EdgeInsets.all(12),
                              child: Row(
                                children: [
                                  Container(
                                    width: 44, height: 44,
                                    decoration: BoxDecoration(
                                      color: _typeColor(type).withValues(alpha: 0.15),
                                      borderRadius: BorderRadius.circular(10),
                                    ),
                                    child: Icon(
                                      type.toUpperCase().contains('IN') ? Icons.login : Icons.logout,
                                      color: _typeColor(type), size: 20,
                                    ),
                                  ),
                                  const SizedBox(width: 12),
                                  Expanded(
                                    child: Column(
                                      crossAxisAlignment: CrossAxisAlignment.start,
                                      children: [
                                        Text(lieu,
                                            style: const TextStyle(color: Colors.white, fontWeight: FontWeight.w600, fontSize: 13)),
                                        if (membre.isNotEmpty)
                                          Text(membre,
                                              style: TextStyle(color: Colors.white.withValues(alpha: 0.5), fontSize: 11)),
                                        if (dateStr.isNotEmpty)
                                          Text(dateStr,
                                              style: TextStyle(color: Colors.white.withValues(alpha: 0.3), fontSize: 10)),
                                      ],
                                    ),
                                  ),
                                  if (type.isNotEmpty)
                                    StatusBadge(label: type, color: _typeColor(type)),
                                ],
                              ),
                            );
                          },
                        ),
                ),
    );
  }

  Widget _buildEmpty() {
    return Center(
      child: Column(
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          Icon(Icons.location_off, size: 48, color: Colors.white.withValues(alpha: 0.2)),
          const SizedBox(height: 12),
          Text('Aucun historique de géolocalisation', style: TextStyle(color: Colors.white.withValues(alpha: 0.5))),
        ],
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
