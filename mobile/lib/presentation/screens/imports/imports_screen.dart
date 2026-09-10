import 'package:flutter/material.dart';
import '../../../../l10n/app_localizations.dart';
import '../../../data/services/api_service.dart';
import '../../widgets/glass_theme.dart';
import '../../widgets/app_drawer.dart';

class ImportsScreen extends StatefulWidget {
  const ImportsScreen({super.key});

  @override
  State<ImportsScreen> createState() => _ImportsScreenState();
}

class _ImportsScreenState extends State<ImportsScreen> {
  final _apiService = ApiService();
  List<Map<String, dynamic>> _imports = [];
  bool _isLoading = true;
  String _statut = '';
  String _type = 'souls';

  static const kImportTypeLabels = {
    'souls': 'Âmes',
    'families': 'Familles',
    'users': 'Utilisateurs',
  };

  static const kImportTypeColors = {
    'souls': 'from-rose-500 to-pink-500',
    'families': 'from-blue-500 to-indigo-500',
    'users': 'from-emerald-500 to-teal-500',
  };

  @override
  void initState() { super.initState(); _loadImports(); }

  Future<void> _loadImports() async {
    try {
      final params = <String, String>{'size': '20', 'type': _type};
      if (_statut.isNotEmpty) params['statut'] = _statut;
      final response = await _apiService.get('/import', params: params);
      final data = response.data as Map<String, dynamic>;
      if (mounted) {
        setState(() {
          _imports = (data['content'] as List).map((e) => e as Map<String, dynamic>).toList();
          _isLoading = false;
        });
      }
    } catch (e) { if (mounted) setState(() => _isLoading = false); }
  }

  Future<void> _validateImport(String id) async {
    try { await _apiService.post('/import/validate/$id'); _loadImports(); } catch (_) {}
  }

  Future<void> _startImport(String id) async {
    try { await _apiService.post('/import/$id'); _loadImports(); } catch (_) {}
  }

  Color _getTypeColor(String type) {
    return Color(0xFFFFFFFF); // placeholder
  }

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context)!;
    return Scaffold(
      appBar: AppBar(
        title: Text(l10n.importsTitle),
        actions: [
          if (_isLoading)
            const Padding(
              padding: EdgeInsets.only(right: 8),
              child: CircularProgressIndicator(strokeWidth: 2),
            ),
        ],
      ),
      drawer: const AppDrawer(),
      body: _isLoading
          ? const ShimmerLoading(itemCount: 5)
          : RefreshIndicator(
              onRefresh: _loadImports,
              child: _imports.isEmpty
                  ? Center(
                      child: Column(
                        mainAxisAlignment: MainAxisAlignment.center,
                        children: [
                          Icon(Icons.upload_file, size: 64, color: Colors.white24),
                          const SizedBox(height: 16),
                          Text(l10n.importsEmpty, style: const TextStyle(color: Colors.white)),
                        ],
                      ),
                    )
                  : ListView.builder(
                      padding: const EdgeInsets.all(16),
                      itemCount: _imports.length,
                      itemBuilder: (context, index) {
                        final imp = _imports[index];
                        final type = imp['type'] ?? 'souls';
                        final statut = imp['statut'] ?? 'PENDING';
                        final total = imp['totalRows'] ?? 0;
                        final valid = imp['validRows'] ?? 0;
                        final imported = imp['imported'] ?? 0;

                        return GlassCard(
                          margin: const EdgeInsets.only(bottom: 8),
                          padding: const EdgeInsets.all(12),
                          borderColor: Colors.teal.withValues(alpha: 0.2),
                          child: Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              Row(
                                children: [
                                  Container(
                                    padding: const EdgeInsets.all(4),
                                    decoration: BoxDecoration(
                                      color: Colors.teal.withValues(alpha: 0.1),
                                      borderRadius: BorderRadius.circular(8),
                                    ),
                                    child: Text(
                                      kImportTypeLabels[type] ?? type,
                                      style: TextStyle(
                                        color: Colors.teal,
                                        fontSize: 12,
                                        fontWeight: FontWeight.w600,
                                      ),
                                    ),
                                  ),
                                  const Spacer(),
                                  Text(
                                    '$imported/$total importés',
                                    style: TextStyle(
                                      color: Colors.white.withValues(alpha: 0.7),
                                      fontSize: 12,
                                    ),
                                  ),
                                ],
                              ),
                              const SizedBox(height: 8),
                              Text(
                                imp['message'] ?? 'Aucune description',
                                style: TextStyle(
                                  color: Colors.white.withValues(alpha: 0.6),
                                  fontSize: 13,
                                ),
                              ),
                              const SizedBox(height: 8),
                              Row(
                                mainAxisAlignment: MainAxisAlignment.end,
                                children: [
                                  statut == 'VALIDATED'
                                      ? ElevatedButton.icon(
                                          onPressed: () => _startImport(imp['id'] as String),
                                          icon: const Icon(Icons.import_export),
                                          label: Text(l10n.importStart),
                                          style: ElevatedButton.styleFrom(
                                            backgroundColor: Colors.teal,
                                            foregroundColor: Colors.white,
                                          ),
                                        )
                                      : TextButton.icon(
                                          onPressed: () => _validateImport(imp['id'] as String),
                                          icon: const Icon(Icons.check_circle),
                                          label: Text(l10n.importValidate),
                                          style: TextButton.styleFrom(
                                            foregroundColor: Colors.teal,
                                          ),
                                        ),
                                ],
                              ),
                            ],
                          ),
                        );
                      },
                    ),
            ),
    );
  }
}