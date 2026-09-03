import 'package:flutter/material.dart';
import '../../data/services/api_service.dart';
import '../../../presentation/widgets/glass_theme.dart';
import '../../../presentation/widgets/app_drawer.dart';

/// Statut 2FA — branché sur GET /api/v1/auth/2fa/status.
class TwoFactorStatusScreen extends StatefulWidget {
  const TwoFactorStatusScreen({super.key});

  @override
  State<TwoFactorStatusScreen> createState() => _TwoFactorStatusScreenState();
}

class _TwoFactorStatusScreenState extends State<TwoFactorStatusScreen> {
  final _apiService = ApiService();
  Map<String, dynamic>? _status;
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
      final res = await _apiService.get('/auth/2fa/status');
      if (mounted) {
        setState(() {
          _status = res.data is Map<String, dynamic> ? res.data as Map<String, dynamic> : null;
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

  @override
  Widget build(BuildContext context) {
    final enabled = _status?['enabled'] == true;
    final method = _status?['method']?.toString() ?? 'Aucun';

    return Scaffold(
      appBar: AppBar(
        title: const Text('Double authentification'),
        backgroundColor: Colors.indigo,
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
                              enabled ? Icons.shield : Icons.shield_outlined,
                              size: 64,
                              color: enabled ? Colors.green : Colors.white.withValues(alpha: 0.3),
                            ),
                            const SizedBox(height: 16),
                            Text(
                              enabled ? '2FA activée' : '2FA non activée',
                              style: TextStyle(
                                color: enabled ? Colors.green : Colors.white.withValues(alpha: 0.5),
                                fontSize: 18,
                                fontWeight: FontWeight.bold,
                              ),
                            ),
                            const SizedBox(height: 8),
                            Text(
                              'Méthode: $method',
                              style: TextStyle(color: Colors.white.withValues(alpha: 0.6), fontSize: 14),
                            ),
                            const SizedBox(height: 16),
                            if (_status != null) ...[
                              _infoRow('Dernière vérification', _status!['lastVerified']?.toString() ?? 'Jamais'),
                              const SizedBox(height: 8),
                              _infoRow('Méthode de récupération', _status!['recoveryMethod']?.toString() ?? 'Non configurée'),
                            ],
                          ],
                        ),
                      ),
                    ],
                  ),
                ),
    );
  }

  Widget _infoRow(String label, String value) {
    return Row(
      mainAxisAlignment: MainAxisAlignment.spaceBetween,
      children: [
        Text(label, style: TextStyle(color: Colors.white.withValues(alpha: 0.5), fontSize: 13)),
        Text(value, style: const TextStyle(color: Colors.white, fontSize: 13, fontWeight: FontWeight.w600)),
      ],
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
