import 'package:flutter/material.dart';
import '../../../data/services/api_service.dart';
import '../../../app.dart';
import '../../widgets/glass_theme.dart';

/// P3 — Assistant de migration de données (import Excel/CSV)
class DataMigrationScreen extends StatefulWidget {
  const DataMigrationScreen({super.key});

  @override
  State<DataMigrationScreen> createState() => _DataMigrationScreenState();
}

class _DataMigrationScreenState extends State<DataMigrationScreen> {
  final ApiService _api = ApiService();
  List<dynamic> _migrations = [];
  bool _isLoading = true;
  bool _isAnalyzing = false;
  String? _selectedFile;
  Map<String, dynamic>? _analysisResult;

  @override
  void initState() {
    super.initState();
    _loadMigrations();
  }

  Future<void> _loadMigrations() async {
    setState(() => _isLoading = true);
    try {
      final res = await _api.get('/data-migration');
      setState(() {
        _migrations = res.data as List<dynamic>? ?? [];
        _isLoading = false;
      });
    } catch (_) {
      if (mounted) setState(() => _isLoading = false);
    }
  }

  Future<void> _analyzeFile() async {
    setState(() => _isAnalyzing = true);
    try {
      final res = await _api.post('/data-migration/analyze', data: {
        'fileName': _selectedFile ?? 'import.csv',
        'fileType': 'CSV',
      });
      setState(() {
        _analysisResult = res.data as Map<String, dynamic>?;
        _isAnalyzing = false;
      });
    } catch (_) {
      if (mounted) setState(() => _isAnalyzing = false);
    }
  }

  Future<void> _executeMigration(String id) async {
    try {
      await _api.post('/data-migration/$id/execute');
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Migration lancée ✅')),
      );
      _loadMigrations();
    } catch (_) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('Erreur lors de la migration')),
        );
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Migration de données')),
      body: _isLoading
          ? const Center(child: CircularProgressIndicator())
          : RefreshIndicator(
              onRefresh: _loadMigrations,
              child: ListView(
                padding: const EdgeInsets.all(16),
                children: [
                  // ── Import Section ──
                  GlassCard(
                    padding: const EdgeInsets.all(16),
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Row(
                          children: [
                            Icon(Icons.upload_file, color: AppColors.primaryLight, size: 20),
                            const SizedBox(width: 8),
                            const Text('Importer des données',
                                style: TextStyle(color: Colors.white, fontWeight: FontWeight.bold, fontSize: 16)),
                          ],
                        ),
                        const SizedBox(height: 12),
                        const Text(
                          'Importez vos membres depuis Excel ou CSV. L\'assistant mapping détecte automatiquement les colonnes.',
                          style: TextStyle(color: Colors.white54, fontSize: 13),
                        ),
                        const SizedBox(height: 16),
                        // File picker placeholder
                        GestureDetector(
                          onTap: () {
                            setState(() => _selectedFile = 'membres_export.csv');
                            _analyzeFile();
                          },
                          child: Container(
                            padding: const EdgeInsets.all(20),
                            decoration: BoxDecoration(
                              color: Colors.white.withValues(alpha: 0.05),
                              borderRadius: BorderRadius.circular(12),
                              border: Border.all(color: AppColors.primaryLight.withValues(alpha: 0.3), style: BorderStyle.solid),
                            ),
                            child: Row(
                              mainAxisAlignment: MainAxisAlignment.center,
                              children: [
                                Icon(Icons.cloud_upload, color: AppColors.primaryLight, size: 24),
                                const SizedBox(width: 8),
                                Text(_selectedFile ?? 'Sélectionner un fichier',
                                    style: TextStyle(color: Colors.white.withValues(alpha: 0.7), fontSize: 14)),
                              ],
                            ),
                          ),
                        ),
                      ],
                    ),
                  ),

                  const SizedBox(height: 16),

                  // ── AI Analysis Result ──
                  if (_analysisResult != null)
                    GlassCard(
                      padding: const EdgeInsets.all(16),
                      borderColor: Colors.green.withValues(alpha: 0.3),
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Row(
                            children: [
                              const Icon(Icons.auto_awesome, color: Colors.green, size: 18),
                              const SizedBox(width: 8),
                              const Text('Analyse IA', style: TextStyle(color: Colors.white, fontWeight: FontWeight.bold)),
                            ],
                          ),
                          const SizedBox(height: 12),
                          _analysisRow('Fichier détecté', '${_analysisResult!['fileName'] ?? 'N/A'}'),
                          _analysisRow('Lignes détectées', '${_analysisResult!['totalRows'] ?? 0}'),
                          _analysisRow('Colonnes mappées', '${_analysisResult!['mappedColumns'] ?? 0}/${_analysisResult!['totalColumns'] ?? 0}'),
                          _analysisRow('Confiance mapping', '${_analysisResult!['confidence'] ?? 0}%'),
                          const SizedBox(height: 12),
                          SizedBox(
                            width: double.infinity,
                            child: ElevatedButton.icon(
                              onPressed: () => _executeMigration(_analysisResult!['migrationId'] ?? ''),
                              icon: const Icon(Icons.play_arrow, size: 18),
                              label: const Text('Lancer la migration'),
                              style: ElevatedButton.styleFrom(
                                backgroundColor: Colors.green,
                                foregroundColor: Colors.white,
                                shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
                              ),
                            ),
                          ),
                        ],
                      ),
                    ),

                  const SizedBox(height: 16),

                  // ── Previous Migrations ──
                  const Text('Migrations précédentes',
                      style: TextStyle(color: Colors.white, fontWeight: FontWeight.bold, fontSize: 14)),
                  const SizedBox(height: 8),

                  if (_migrations.isEmpty)
                    GlassCard(
                      padding: const EdgeInsets.all(24),
                      child: Column(
                        children: [
                          Icon(Icons.history, color: Colors.white.withValues(alpha: 0.2), size: 40),
                          const SizedBox(height: 8),
                          Text('Aucune migration effectuée',
                              style: TextStyle(color: Colors.white.withValues(alpha: 0.5))),
                        ],
                      ),
                    )
                  else
                    ..._migrations.map((m) => GlassCard(
                      margin: const EdgeInsets.only(bottom: 8),
                      padding: const EdgeInsets.all(12),
                      child: Row(
                        children: [
                          Icon(
                            m['status'] == 'COMPLETED' ? Icons.check_circle :
                            m['status'] == 'FAILED' ? Icons.error : Icons.hourglass_empty,
                            color: m['status'] == 'COMPLETED' ? Colors.green :
                                   m['status'] == 'FAILED' ? Colors.red : Colors.orange,
                            size: 20,
                          ),
                          const SizedBox(width: 12),
                          Expanded(
                            child: Column(
                              crossAxisAlignment: CrossAxisAlignment.start,
                              children: [
                                Text(m['fileName'] ?? 'Migration',
                                    style: const TextStyle(color: Colors.white, fontWeight: FontWeight.w600, fontSize: 13)),
                                Text('${m['totalRows'] ?? 0} lignes • ${m['status'] ?? 'UNKNOWN'}',
                                    style: TextStyle(color: Colors.white.withValues(alpha: 0.5), fontSize: 11)),
                              ],
                            ),
                          ),
                          if (m['status'] == 'COMPLETED')
                            TextButton(
                              onPressed: () => _executeMigration(m['id'].toString()),
                              child: const Text('Relancer', style: TextStyle(fontSize: 12)),
                            ),
                        ],
                      ),
                    )),
                ],
              ),
            ),
    );
  }

  Widget _analysisRow(String label, String value) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 4),
      child: Row(
        mainAxisAlignment: MainAxisAlignment.spaceBetween,
        children: [
          Text(label, style: TextStyle(color: Colors.white.withValues(alpha: 0.6), fontSize: 12)),
          Text(value, style: const TextStyle(color: Colors.white, fontWeight: FontWeight.w600, fontSize: 12)),
        ],
      ),
    );
  }
}
