import 'package:flutter/material.dart';
import '../../data/services/api_service.dart';
import '../../../presentation/widgets/glass_theme.dart';
import '../../../presentation/widgets/app_drawer.dart';

/// Échange d'aide — branché sur /api/v1/aid/exchange.
class AidExchangeScreen extends StatefulWidget {
  const AidExchangeScreen({super.key});

  @override
  State<AidExchangeScreen> createState() => _AidExchangeScreenState();
}

class _AidExchangeScreenState extends State<AidExchangeScreen> {
  final _apiService = ApiService();
  List<dynamic> _exchanges = [];
  Map<String, dynamic>? _rates;
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
      final results = await Future.wait([
        _apiService.get('/aid/exchange'),
        _apiService.get('/aid/exchange/rates'),
      ]);
      if (mounted) {
        final exData = results[0].data;
        final rtData = results[1].data;
        setState(() {
          _exchanges = exData is List ? exData : (exData is Map && exData['content'] is List ? exData['content'] as List<dynamic> : <dynamic>[]);
          _rates = rtData is Map<String, dynamic> ? rtData : null;
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
    return Scaffold(
      appBar: AppBar(
        title: const Text('Échange d\'aide'),
        backgroundColor: Colors.teal,
        foregroundColor: Colors.white,
        actions: [IconButton(icon: const Icon(Icons.refresh), onPressed: _loadData)],
      ),
      drawer: const AppDrawer(),
      body: _isLoading
          ? const ShimmerLoading(itemCount: 4)
          : _error != null
              ? _buildError()
              : RefreshIndicator(
                  onRefresh: _loadData,
                  child: ListView(
                    padding: const EdgeInsets.all(16),
                    children: [
                      if (_rates != null) _buildRatesCard(),
                      const SizedBox(height: 16),
                      if (_exchanges.isEmpty)
                        _buildEmpty()
                      else
                        ..._exchanges.map((e) => _buildExchangeCard(e as Map<String, dynamic>)),
                    ],
                  ),
                ),
    );
  }

  Widget _buildRatesCard() {
    return GlassCard(
      padding: const EdgeInsets.all(16),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const Text('Taux d\'échange', style: TextStyle(color: Colors.white, fontWeight: FontWeight.bold, fontSize: 16)),
          const SizedBox(height: 12),
          ...(_rates!.entries.map((e) => Padding(
                padding: const EdgeInsets.only(bottom: 8),
                child: Row(
                  children: [
                    Icon(Icons.currency_exchange, color: Colors.teal, size: 16),
                    const SizedBox(width: 8),
                    Expanded(
                        child: Text(e.key, style: TextStyle(color: Colors.white.withValues(alpha: 0.7), fontSize: 13))),
                    Text('${e.value}', style: const TextStyle(color: Colors.teal, fontWeight: FontWeight.bold)),
                  ],
                ),
              ))),
        ],
      ),
    );
  }

  Widget _buildExchangeCard(Map<String, dynamic> e) {
    final statut = e['statut']?.toString() ?? 'EN_ATTENTE';
    final color = statut == 'ACCEPTE' ? Colors.green : (statut == 'REFUSE' ? Colors.red : Colors.amber);
    return GlassCard(
      margin: const EdgeInsets.only(bottom: 10),
      padding: const EdgeInsets.all(14),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Icon(Icons.swap_horiz, color: color, size: 20),
              const SizedBox(width: 8),
              Expanded(
                child: Text(e['description']?.toString() ?? 'Échange',
                    style: const TextStyle(color: Colors.white, fontWeight: FontWeight.w600, fontSize: 14)),
              ),
              StatusBadge(label: statut, color: color),
            ],
          ),
          const SizedBox(height: 8),
          Text('Crédits: ${e['credits'] ?? 0}',
              style: TextStyle(color: Colors.teal, fontSize: 12, fontWeight: FontWeight.w600)),
        ],
      ),
    );
  }

  Widget _buildEmpty() {
    return Center(
      child: Column(
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          Icon(Icons.swap_horiz, size: 48, color: Colors.white.withValues(alpha: 0.2)),
          const SizedBox(height: 12),
          Text('Aucun échange en cours', style: TextStyle(color: Colors.white.withValues(alpha: 0.5))),
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
