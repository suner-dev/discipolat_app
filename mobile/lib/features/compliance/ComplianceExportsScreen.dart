import 'package:flutter/material.dart';
import '../../data/services/api_service.dart';
import '../../../presentation/widgets/glass_theme.dart';
import '../../../presentation/widgets/app_drawer.dart';

/// Exportations de conformité — branché sur /api/v1/compliance.
class ComplianceExportsScreen extends StatefulWidget {
  const ComplianceExportsScreen({super.key});

  @override
  State<ComplianceExportsScreen> createState() => _ComplianceExportsScreenState();
}

class _ComplianceExportsScreenState extends State<ComplianceExportsScreen> {
  final _apiService = ApiService();
  List<dynamic> _exports = [];
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
      final res = await _apiService.get('/compliance/exports');
      if (mounted) {
        final data = res.data;
        setState(() {
          _exports = data is List
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

  Future<void> _processGdpr(String id) async {
    try {
      await _apiService.post('/compliance/gdpr/$id/process');
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('Traitement RGPD lancé'), backgroundColor: Colors.green),
        );
        _loadData();
      }
    } catch (_) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('Erreur lors du traitement'), backgroundColor: Colors.red),
        );
      }
    }
  }

  Color _statusColor(String? statut) {
    switch (statut?.toUpperCase()) {
      case 'TERMINE':
      case 'COMPLETED':
      case 'EXPORTED':
        return Colors.green;
      case 'EN_COURS':
      case 'EN COURS':
      case 'PROCESSING':
        return Colors.blue;
      case 'EN_ATTENTE':
      case 'EN ATTENTE':
      case 'PENDING':
        return Colors.amber;
      case 'ERREUR':
      case 'ERROR':
        return Colors.red;
      default:
        return Colors.grey;
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Conformité & Exportations'),
        backgroundColor: AppColors.primary,
        foregroundColor: Colors.white,
        actions: [IconButton(icon: const Icon(Icons.refresh), onPressed: _loadData)],
      ),
      drawer: const AppDrawer(),
      body: _isLoading
          ? const ShimmerLoading(itemCount: 5)
          : _error != null
              ? _buildError()
              : RefreshIndicator(
                  onRefresh: _loadData,
                  child: ListView(
                    padding: const EdgeInsets.all(16),
                    children: [
                      if (_exports.isEmpty)
                        _buildEmpty()
                      else
                        ..._exports.map((e) => _buildExportCard(e as Map<String, dynamic>)),
                    ],
                  ),
                ),
    );
  }

  Widget _buildExportCard(Map<String, dynamic> export) {
    final type = export['type']?.toString() ?? 'Export';
    final statut = export['statut']?.toString() ?? export['status']?.toString() ?? '';
    final dateStr = export['createdAt']?.toString().substring(0, 10) ?? '';
    final id = export['id']?.toString() ?? '';
    final isGdpr = type.toUpperCase().contains('GDPR') || type.toUpperCase().contains('RGPD');

    return GlassCard(
      margin: const EdgeInsets.only(bottom: 10),
      padding: const EdgeInsets.all(14),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Container(
                width: 44,
                height: 44,
                decoration: BoxDecoration(
                  color: (isGdpr ? Colors.orange : Colors.blue).withValues(alpha: 0.15),
                  borderRadius: BorderRadius.circular(10),
                ),
                child: Icon(
                  isGdpr ? Icons.privacy_tip : Icons.download,
                  color: isGdpr ? Colors.orange : Colors.blue,
                  size: 20,
                ),
              ),
              const SizedBox(width: 12),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(type,
                        style: const TextStyle(color: Colors.white, fontWeight: FontWeight.w600, fontSize: 14)),
                    if (dateStr.isNotEmpty)
                      Text(dateStr,
                          style: TextStyle(color: Colors.white.withValues(alpha: 0.4), fontSize: 11)),
                  ],
                ),
              ),
              if (statut.isNotEmpty)
                StatusBadge(label: statut, color: _statusColor(statut)),
            ],
          ),
          if (isGdpr && id.isNotEmpty) ...[
            const SizedBox(height: 12),
            SizedBox(
              width: double.infinity,
              child: FilledButton.icon(
                onPressed: () => _processGdpr(id),
                icon: const Icon(Icons.play_arrow, size: 16),
                label: const Text('Traiter RGPD'),
                style: FilledButton.styleFrom(backgroundColor: Colors.orange),
              ),
            ),
          ],
        ],
      ),
    );
  }

  Widget _buildEmpty() {
    return Center(
      child: Column(
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          Icon(Icons.download, size: 48, color: Colors.white.withValues(alpha: 0.2)),
          const SizedBox(height: 12),
          Text('Aucune exportation', style: TextStyle(color: Colors.white.withValues(alpha: 0.5))),
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
