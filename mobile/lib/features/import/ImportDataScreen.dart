import 'package:flutter/material.dart';
import '../../data/services/api_service.dart';
import '../../../presentation/widgets/glass_theme.dart';
import '../../../presentation/widgets/app_drawer.dart';

/// Import de données — branché sur /api/v1/import.
class ImportDataScreen extends StatefulWidget {
  const ImportDataScreen({super.key});

  @override
  State<ImportDataScreen> createState() => _ImportDataScreenState();
}

class _ImportDataScreenState extends State<ImportDataScreen> {
  final _apiService = ApiService();
  bool _isLoading = false;
  String? _error;
  String? _success;
  String _selectedType = 'souls';
  final _types = const [
    {'key': 'souls', 'label': 'Âmes', 'icon': Icons.favorite},
    {'key': 'families', 'label': 'Familles', 'icon': Icons.family_restroom},
    {'key': 'users', 'label': 'Utilisateurs', 'icon': Icons.people},
  ];

  Future<void> _importData(String type) async {
    setState(() {
      _isLoading = true;
      _error = null;
      _success = null;
    });
    try {
      await _apiService.post('/import/$type', data: {});
      if (mounted) {
        setState(() {
          _isLoading = false;
          _success = 'Import lancé pour $type';
        });
      }
    } catch (_) {
      if (mounted) {
        setState(() {
          _isLoading = false;
          _error = 'Erreur lors de l\'import';
        });
      }
    }
  }

  Future<void> _validateImport(String type) async {
    try {
      setState(() {
        _isLoading = true;
        _error = null;
        _success = null;
      });
      final res = await _apiService.post('/import/$type/validate');
      if (mounted) {
        final data = res.data;
        setState(() {
          _isLoading = false;
          _success = 'Validation terminée: ${data is Map ? data['message'] ?? 'OK' : 'OK'}';
        });
      }
    } catch (_) {
      if (mounted) {
        setState(() {
          _isLoading = false;
          _error = 'Erreur lors de la validation';
        });
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Import de données'),
        backgroundColor: Colors.brown,
        foregroundColor: Colors.white,
      ),
      drawer: const AppDrawer(),
      body: ListView(
        padding: const EdgeInsets.all(16),
        children: [
          GlassCard(
            padding: const EdgeInsets.all(16),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                const Text('Type de données', style: TextStyle(color: Colors.white, fontWeight: FontWeight.bold, fontSize: 16)),
                const SizedBox(height: 12),
                ...(_types.map((t) => Padding(
                      padding: const EdgeInsets.only(bottom: 8),
                      child: ListTile(
                        leading: Icon(t['icon'] as IconData, color: Colors.white70, size: 20),
                        title: Text(t['label'] as String, style: const TextStyle(color: Colors.white, fontSize: 14)),
                        trailing: Radio<String>(
                          value: t['key'] as String,
                          groupValue: _selectedType,
                          onChanged: (v) => setState(() => _selectedType = v ?? _selectedType),
                          activeColor: Colors.brown,
                        ),
                        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
                      ),
                    ))),
              ],
            ),
          ),
          const SizedBox(height: 16),
          if (_isLoading)
            const Center(child: CircularProgressIndicator())
          else ...[
            SizedBox(
              width: double.infinity,
              child: FilledButton.icon(
                onPressed: () => _importData(_selectedType),
                icon: const Icon(Icons.upload_file, size: 18),
                label: const Text('Lancer l\'import'),
                style: FilledButton.styleFrom(backgroundColor: Colors.brown),
              ),
            ),
            const SizedBox(height: 12),
            SizedBox(
              width: double.infinity,
              child: OutlinedButton.icon(
                onPressed: () => _validateImport(_selectedType),
                icon: const Icon(Icons.check_circle_outline, size: 18),
                label: const Text('Valider les données'),
              ),
            ),
          ],
          if (_error != null) ...[
            const SizedBox(height: 16),
            GlassCard(
              padding: const EdgeInsets.all(12),
              borderColor: Colors.red.withValues(alpha: 0.3),
              child: Row(
                children: [
                  const Icon(Icons.error, color: Colors.red, size: 20),
                  const SizedBox(width: 8),
                  Expanded(child: Text(_error!, style: const TextStyle(color: Colors.red, fontSize: 13))),
                ],
              ),
            ),
          ],
          if (_success != null) ...[
            const SizedBox(height: 16),
            GlassCard(
              padding: const EdgeInsets.all(12),
              borderColor: Colors.green.withValues(alpha: 0.3),
              child: Row(
                children: [
                  const Icon(Icons.check_circle, color: Colors.green, size: 20),
                  const SizedBox(width: 8),
                  Expanded(child: Text(_success!, style: const TextStyle(color: Colors.green, fontSize: 13))),
                ],
              ),
            ),
          ],
        ],
      ),
    );
  }
}
