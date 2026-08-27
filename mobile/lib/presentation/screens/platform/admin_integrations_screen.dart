import 'package:flutter/material.dart';
import '../../widgets/glass_theme.dart';
import '../../widgets/app_drawer.dart';
import '../../../data/services/api_service.dart';
import '../../../l10n/app_localizations.dart';

/// Administration des intégrations externes (SMTP, stockage, JWT, rate-limiting).
class AdminIntegrationsScreen extends StatefulWidget {
  const AdminIntegrationsScreen({super.key});

  @override
  State<AdminIntegrationsScreen> createState() => _AdminIntegrationsScreenState();
}

class _AdminIntegrationsScreenState extends State<AdminIntegrationsScreen> {
  final _apiService = ApiService();
  final Map<String, dynamic> _configs = {};
  bool _isLoading = true;
  bool _isTesting = false;
  String? _testResult;

  static const _categoryKeys = ['smtp', 'storage', 'jwt', 'rate-limiting'];
  static const _categoryLabels = {'smtp': 'SMTP / Email', 'storage': 'Stockage / MinIO', 'jwt': 'JWT / Auth', 'rate-limiting': 'Rate Limiting'};
  static const _categoryIcons = {'smtp': Icons.email, 'storage': Icons.cloud, 'jwt': Icons.key, 'rate-limiting': Icons.speed};
  static const _categoryColors = {'smtp': Colors.blue, 'storage': Colors.teal, 'jwt': Colors.amber, 'rate-limiting': Colors.red};

  @override
  void initState() { super.initState(); _loadAll(); }

  Future<void> _loadAll() async {
    setState(() => _isLoading = true);
    for (final key in _categoryKeys) {
      try {
        final res = await _apiService.get('/admin/integrations/$key');
        _configs[key] = res.data as Map<String, dynamic>;
      } catch (_) {}
    }
    if (mounted) setState(() => _isLoading = false);
  }

  Future<void> _testConnection(String category) async {
    setState(() { _isTesting = true; _testResult = null; });
    try {
      final res = await _apiService.post('/admin/integrations/$category/test');
      final data = res.data as Map<String, dynamic>;
      setState(() => _testResult = '${data['success'] == true ? "✅" : "❌"} ${data['message'] ?? ""}');
    } catch (e) {
      setState(() => _testResult = '❌ Erreur: $e');
    } finally {
      if (mounted) setState(() => _isTesting = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: Text(AppLocalizations.of(context).integTitle), actions: [IconButton(icon: const Icon(Icons.refresh), onPressed: _loadAll)]),
      drawer: const AppDrawer(),
      body: _isLoading ? const ShimmerLoading(itemCount: 4) : RefreshIndicator(
        onRefresh: _loadAll,
        child: ListView(padding: const EdgeInsets.all(12), children: [
          if (_testResult != null)
            GlassCard(margin: const EdgeInsets.only(bottom: 12), padding: const EdgeInsets.all(12),
              borderColor: _testResult!.startsWith('✅') ? Colors.green.withValues(alpha: 0.3) : Colors.red.withValues(alpha: 0.3),
              child: Text(_testResult!, style: const TextStyle(color: Colors.white, fontSize: 12))),
          for (final key in _categoryKeys) _integrationCard(key),
        ]),
      ),
    );
  }

  Widget _integrationCard(String key) {
    final config = _configs[key] ?? {};
    final enabled = config['enabled'] == true;
    final color = _categoryColors[key] ?? Colors.grey;
    return GlassCard(margin: const EdgeInsets.only(bottom: 10), padding: const EdgeInsets.all(14), child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
      Row(children: [
        Container(width: 38, height: 38, decoration: BoxDecoration(
          color: color.withValues(alpha: 0.15), borderRadius: BorderRadius.circular(10)),
          child: Icon(_categoryIcons[key] ?? Icons.settings, color: color, size: 18)),
        const SizedBox(width: 10),
        Expanded(child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
          Text(_categoryLabels[key] ?? key, style: const TextStyle(color: Colors.white, fontWeight: FontWeight.w600, fontSize: 14)),
          Text(enabled ? AppLocalizations.of(context).integEnabled : AppLocalizations.of(context).integDisabled, style: TextStyle(color: enabled ? Colors.green : Colors.white.withValues(alpha: 0.4), fontSize: 11)),
        ])),
        Switch(value: enabled, activeThumbColor: Colors.green, onChanged: (v) async {
          config['enabled'] = v;
          try { await _apiService.put('/admin/integrations/$key', data: config); _loadAll(); } catch (_) {}
        }),
      ]),
      if (config.isNotEmpty) ...[
        const SizedBox(height: 8),
        ...config.entries.where((e) => e.key != 'enabled').take(4).map((e) => Padding(
          padding: const EdgeInsets.only(bottom: 2),
          child: Row(children: [
            SizedBox(width: 100, child: Text(e.key, style: TextStyle(color: Colors.white.withValues(alpha: 0.4), fontSize: 10))),
            Expanded(child: Text('${e.value}', style: const TextStyle(color: Colors.white, fontSize: 11), overflow: TextOverflow.ellipsis)),
          ]),
        )),
      ],
      const SizedBox(height: 8),
      SizedBox(
        width: double.infinity,
        child: OutlinedButton.icon(
          onPressed: _isTesting ? null : () => _testConnection(key),
          icon: _isTesting ? const SizedBox(width: 14, height: 14, child: CircularProgressIndicator(strokeWidth: 2)) : const Icon(Icons.wifi_find, size: 14),
          label: Text(AppLocalizations.of(context).integTestConn, style: TextStyle(fontSize: 11)),
          style: OutlinedButton.styleFrom(side: BorderSide(color: Colors.white.withValues(alpha: 0.2))),
        ),
      ),
    ]));
  }
}
