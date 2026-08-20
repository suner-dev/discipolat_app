import 'dart:async';
import 'package:flutter/material.dart';
import '../../../data/services/api_service.dart';
import '../../widgets/glass_theme.dart';

class DocumentsScreen extends StatefulWidget {
  const DocumentsScreen({super.key});

  @override
  State<DocumentsScreen> createState() => _DocumentsScreenState();
}

class _DocumentsScreenState extends State<DocumentsScreen> {
  final _api = ApiService();
  List<dynamic> _files = [];
  bool _isLoading = true;
  bool _showFilters = false;
  String _search = '';
  String _catFilter = '';
  Timer? _searchDebounce;

  static const Map<String, String> _categorieLabels = {
    'CONSTITUTION': 'Constitution',
    'REGLEMENT': 'Règlement',
    'PROCEDURE': 'Procédure',
    'FORMULAIRE': 'Formulaire',
    'RAPPORT': 'Rapport',
    'AUTRE': 'Autre',
  };

  @override
  void initState() {
    super.initState();
    _loadData();
  }

  @override
  void dispose() {
    _searchDebounce?.cancel();
    super.dispose();
  }

  Future<void> _loadData() async {
    setState(() => _isLoading = true);
    try {
      final params = <String, dynamic>{};
      if (_search.isNotEmpty) params['q'] = _search;
      if (_catFilter.isNotEmpty) params['categorie'] = _catFilter;
      params['size'] = '100';
      final res = await _api.get('/documents', params: params);
      final data = res.data;
      List<dynamic> files = [];
      if (data is Map && data.containsKey('content')) {
        files = data['content'] as List<dynamic>;
      } else if (data is List) {
        files = data;
      }
      if (mounted) setState(() { _files = files; _isLoading = false; });
    } catch (e) {
      if (mounted) setState(() { _files = []; _isLoading = false; });
    }
  }

  void _onSearchChanged(String value) {
    _searchDebounce?.cancel();
    _searchDebounce = Timer(const Duration(milliseconds: 400), () {
      _search = value;
      _loadData();
    });
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: Colors.black,
      appBar: AppBar(
        title: const Text('📁 Documents', style: TextStyle(color: Colors.white)),
        backgroundColor: Colors.transparent,
        elevation: 0,
        actions: [
          IconButton(
            icon: Icon(_showFilters ? Icons.filter_list_off : Icons.filter_list,
                color: Colors.white),
            onPressed: () => setState(() => _showFilters = !_showFilters),
          ),
        ],
      ),
      body: _isLoading
          ? const Center(child: CircularProgressIndicator(color: Colors.cyanAccent))
          : ListView(
              padding: const EdgeInsets.all(16),
              children: [
                if (_showFilters) ...[
                  GlassCard(
                    padding: const EdgeInsets.all(12),
                    margin: const EdgeInsets.only(bottom: 16),
                    child: Column(children: [
                      TextField(
                        decoration: const InputDecoration(
                          hintText: 'Rechercher un document...',
                          prefixIcon: Icon(Icons.search, size: 20),
                          filled: true,
                        ),
                        onChanged: _onSearchChanged,
                      ),
                      const SizedBox(height: 10),
                      DropdownButtonFormField<String>(
                        initialValue: _catFilter.isEmpty ? null : _catFilter,
                        dropdownColor: const Color(0xFF111827),
                        decoration: const InputDecoration(labelText: 'Catégorie'),
                        items: [
                          const DropdownMenuItem(value: '', child: Text('Toutes catégories')),
                          ..._categorieLabels.entries.map((e) =>
                            DropdownMenuItem(value: e.key, child: Text(e.value)),
                          ),
                        ],
                        onChanged: (v) { _catFilter = v ?? ''; _loadData(); },
                      ),
                    ]),
                  ),
                ],
                if (_files.isEmpty)
                  GlassCard(
                    padding: const EdgeInsets.all(32),
                    child: Column(children: [
                      Icon(Icons.folder_open, size: 48, color: Colors.white.withAlpha(40)),
                      const SizedBox(height: 12),
                      Text('Aucun document', style: TextStyle(color: Colors.white.withAlpha(100), fontSize: 16)),
                    ]),
                  )
                else
                  ...List.generate(_files.length, (i) {
                    final file = _files[i];
                    return _buildFileCard(file);
                  }),
              ],
            ),
    );
  }

  Widget _buildFileCard(Map<String, dynamic> file) {
    final name = file['nom']?.toString() ?? 'Document';
    final categorie = file['categorie']?.toString() ?? '';
    final size = file['tailleOctets'] as int? ?? 0;
    final sizeStr = size > 1024 * 1024
        ? '${(size / (1024 * 1024)).toStringAsFixed(1)} MB'
        : '${(size / 1024).toStringAsFixed(0)} KB';

    return GlassCard(
      margin: const EdgeInsets.only(bottom: 8),
      child: ListTile(
        leading: Icon(Icons.description, color: Colors.cyanAccent.withAlpha(200)),
        title: Text(name, style: const TextStyle(color: Colors.white, fontSize: 14)),
        subtitle: Text('$categorie · $sizeStr',
            style: TextStyle(color: Colors.white.withAlpha(120), fontSize: 12)),
        trailing: Icon(Icons.download, color: Colors.white.withAlpha(80)),
      ),
    );
  }
}
