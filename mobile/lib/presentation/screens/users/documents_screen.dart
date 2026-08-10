import 'package:flutter/material.dart';
import 'package:intl/intl.dart';
import '../../../data/services/api_service.dart';
import '../../widgets/glass_theme.dart';
import '../../widgets/app_drawer.dart';
import '../../widgets/open_url.dart';

class DocumentsScreen extends StatefulWidget {
  const DocumentsScreen({super.key});

  @override
  State<DocumentsScreen> createState() => _DocumentsScreenState();
}

class _DocumentsScreenState extends State<DocumentsScreen> {
  final _apiService = ApiService();
  List<dynamic> _files = [];
  bool _isLoading = true;
  bool _showCreate = false;
  bool _showFilters = false;
  String _search = '';
  String _catFilter = '';

  final _nameCtrl = TextEditingController();
  final _descCtrl = TextEditingController();
  final _cheminCtrl = TextEditingController();
  final _mimeCtrl = TextEditingController();
  final _tailleCtrl = TextEditingController(text: '0');
  String _categorie = 'AUTRE';
  bool _isProcessing = false;

  static const _categorieLabels = {
    'COMPTE_RENDU': 'Compte rendu',
    'FORMATION': 'Formation',
    'PHOTO': 'Photo',
    'RESOURCES': 'Ressources',
    'AUTRE': 'Autre',
  };

  static const _categorieIcons = {
    'COMPTE_RENDU': Icons.description,
    'FORMATION': Icons.book,
    'PHOTO': Icons.image,
    'RESOURCES': Icons.folder,
    'AUTRE': Icons.insert_drive_file,
  };

  static const _categorieColors = {
    'COMPTE_RENDU': Color(0xFF3B82F6),
    'FORMATION': Color(0xFF8B5CF6),
    'PHOTO': Color(0xFF22C55E),
    'RESOURCES': Color(0xFFF59E0B),
    'AUTRE': Color(0xFF6B7280),
  };

  @override
  void initState() { super.initState(); _loadData(); }

  @override
  void dispose() {
    _nameCtrl.dispose(); _descCtrl.dispose(); _cheminCtrl.dispose(); _mimeCtrl.dispose(); _tailleCtrl.dispose();
    super.dispose();
  }

  Future<void> _loadData() async {
    setState(() => _isLoading = true);
    try {
      final params = <String, dynamic>{'size': '50'};
      if (_catFilter.isNotEmpty) params['categorie'] = _catFilter;
      if (_search.isNotEmpty) params['search'] = _search;
      final res = await _apiService.get('/files', params: params);
      if (mounted) {
        _files = (res.data['content'] as List?) ?? [];
        setState(() => _isLoading = false);
      }
    } catch (_) { if (mounted) setState(() => _isLoading = false); }
  }

  Future<void> _createFile() async {
    setState(() => _isProcessing = true);
    try {
      await _apiService.post('/files', data: {
        'nom': _nameCtrl.text.trim(),
        'description': _descCtrl.text.trim().isEmpty ? null : _descCtrl.text.trim(),
        'chemin': _cheminCtrl.text.trim(),
        'categorie': _categorie,
        'typeFichier': _mimeCtrl.text.trim().isEmpty ? 'application/pdf' : _mimeCtrl.text.trim(),
        'taille': int.tryParse(_tailleCtrl.text) ?? 0,
      });
      _nameCtrl.clear(); _descCtrl.clear(); _cheminCtrl.clear(); _mimeCtrl.clear(); _tailleCtrl.clear();
      _categorie = 'AUTRE';
      setState(() => _showCreate = false);
      _loadData();
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('Document ajouté')),
        );
      }
    } catch (_) { if (mounted) _showMessage('Erreur lors de la création'); }
    finally { if (mounted) setState(() => _isProcessing = false); }
  }

  void _showMessage(String msg) {
    ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(msg)));
  }

  String _formatSize(int bytes) {
    if (bytes < 1024) return '$bytes o';
    if (bytes < 1024 * 1024) return '${(bytes / 1024).toStringAsFixed(1)} Ko';
    return '${(bytes / (1024 * 1024)).toStringAsFixed(1)} Mo';
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Documents'),
        actions: [
          IconButton(
            icon: Icon(_showFilters ? Icons.filter_list_off : Icons.filter_list),
            onPressed: () => setState(() => _showFilters = !_showFilters),
          ),
          IconButton(
            icon: const Icon(Icons.add),
            onPressed: () => setState(() => _showCreate = !_showCreate),
          ),
        ],
      ),
      drawer: const AppDrawer(),
      body: _isLoading
          ? const ShimmerLoading(itemCount: 5)
          : RefreshIndicator(
              onRefresh: _loadData,
              child: SingleChildScrollView(
                physics: const AlwaysScrollableScrollPhysics(),
                padding: const EdgeInsets.all(16),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    // Create form
                    if (_showCreate)
                      GlassCard(
                        padding: const EdgeInsets.all(16),
                        margin: const EdgeInsets.only(bottom: 16),
                        borderColor: AppColors.primary.withValues(alpha: 0.3),
                        child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
                          Row(children: [
                            Icon(Icons.upload_file, color: AppColors.primary, size: 18),
                            const SizedBox(width: 8),
                            const Text('Nouveau document',
                                style: TextStyle(color: Colors.white, fontSize: 16, fontWeight: FontWeight.bold)),
                          ]),
                          const SizedBox(height: 16),
                          TextField(controller: _nameCtrl,
                              decoration: const InputDecoration(labelText: 'Nom du document *', hintText: 'Compte rendu réunion')),
                          const SizedBox(height: 10),
                          TextField(controller: _descCtrl, maxLines: 2,
                              decoration: const InputDecoration(labelText: 'Description', hintText: 'Brève description...')),
                          const SizedBox(height: 10),
                          TextField(controller: _cheminCtrl,
                              decoration: const InputDecoration(labelText: 'URL / chemin du fichier *', hintText: 'https://drive.google.com/...', prefixIcon: Icon(Icons.link, size: 16))),
                          const SizedBox(height: 10),
                          Row(children: [
                            Expanded(
                              child: DropdownButtonFormField<String>(
                                initialValue: _categorie,
                                dropdownColor: const Color(0xFF111827),
                                decoration: const InputDecoration(labelText: 'Catégorie'),
                                items: _categorieLabels.entries.map((e) =>
                                  DropdownMenuItem(value: e.key, child: Text(e.value)),
                                ).toList(),
                                onChanged: (v) => setState(() => _categorie = v ?? 'AUTRE'),
                              ),
                            ),
                            const SizedBox(width: 12),
                            Expanded(
                              child: TextField(controller: _mimeCtrl,
                                  decoration: const InputDecoration(labelText: 'Type MIME', hintText: 'application/pdf')),
                            ),
                          ]),
                          const SizedBox(height: 10),
                          TextField(controller: _tailleCtrl,
                              keyboardType: TextInputType.number,
                              decoration: const InputDecoration(labelText: 'Taille (octets)', hintText: '0')),
                          const SizedBox(height: 16),
                          Row(mainAxisAlignment: MainAxisAlignment.end, children: [
                            TextButton(onPressed: () => setState(() => _showCreate = false), child: const Text('Annuler')),
                            const SizedBox(width: 8),
                            FilledButton.icon(
                              onPressed: _createFile,
                              icon: _isProcessing
                                  ? const SizedBox(width: 16, height: 16, child: CircularProgressIndicator(strokeWidth: 2, color: Colors.white))
                                  : const Icon(Icons.save, size: 16),
                              label: const Text('Enregistrer'),
                            ),
                          ]),
                        ]),
                      ),

                    // Search & filters
                    if (_showFilters)
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
                            onChanged: (v) { _search = v; _loadData(); },
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

                    // Files list
                    if (_files.isEmpty)
                      GlassCard(
                        padding: const EdgeInsets.all(32),
                        child: Column(children: [
                          Icon(Icons.folder_open, size: 48, color: Colors.white.withValues(alpha: 0.15)),
                          const SizedBox(height: 12),
                          Text('Aucun document',
                              style: TextStyle(color: Colors.white.withValues(alpha: 0.4), fontSize: 16)),
                        ]),
                      )
                    else
                      ..._files.asMap().entries.map((entry) {
                        final f = entry.value as Map<String, dynamic>;
                        final cat = (f['categorie'] as String?) ?? 'AUTRE';
                        final icon = _categorieIcons[cat] ?? Icons.insert_drive_file;
                        final color = _categorieColors[cat] ?? Colors.grey;
                        final size = (f['taille'] as int?) ?? 0;
                        final date = f['dateCreation'] as String?;

                        return GlassCard(
                          margin: const EdgeInsets.only(bottom: 8),
                          padding: const EdgeInsets.all(12),
                          child: Row(children: [
                            Container(
                              padding: const EdgeInsets.all(10),
                              decoration: BoxDecoration(
                                color: color.withValues(alpha: 0.15),
                                borderRadius: BorderRadius.circular(12),
                              ),
                              child: Icon(icon, color: color, size: 22),
                            ),
                            const SizedBox(width: 12),
                            Expanded(
                              child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
                                Text('${f['nom'] ?? '—'}',
                                    style: const TextStyle(color: Colors.white, fontSize: 14, fontWeight: FontWeight.w600)),
                                if (f['description'] != null && (f['description'] as String).isNotEmpty)
                                  Padding(
                                    padding: const EdgeInsets.only(top: 2),
                                    child: Text('${f['description']}',
                                        style: TextStyle(color: Colors.white.withValues(alpha: 0.4), fontSize: 11),
                                        maxLines: 1, overflow: TextOverflow.ellipsis),
                                  ),
                                const SizedBox(height: 4),
                                Row(children: [
                                  _smallBadge(color, _categorieLabels[cat] ?? cat),
                                  const SizedBox(width: 6),
                                  Text(_formatSize(size),
                                      style: TextStyle(color: Colors.white.withValues(alpha: 0.3), fontSize: 10)),
                                  if (date != null) ...[
                                    const SizedBox(width: 6),
                                    Text(DateFormat('d MMM yyyy', 'fr_FR').format(DateTime.parse(date)),
                                        style: TextStyle(color: Colors.white.withValues(alpha: 0.3), fontSize: 10)),
                                  ],
                                ]),
                              ]),
                            ),
                            // Download & Delete
                            Column(children: [
                              GestureDetector(
                                onTap: () => _openUrl(f['chemin'] as String?),
                                child: Container(
                                  padding: const EdgeInsets.all(8),
                                  decoration: BoxDecoration(
                                    color: Colors.white.withValues(alpha: 0.05),
                                    borderRadius: BorderRadius.circular(8),
                                  ),
                                  child: Icon(Icons.download, color: AppColors.primary, size: 18),
                                ),
                              ),
                              const SizedBox(height: 4),
                              GestureDetector(
                                onTap: () => _deleteFile(f['id'] as String?),
                                child: Container(
                                  padding: const EdgeInsets.all(8),
                                  decoration: BoxDecoration(
                                    color: Colors.red.withValues(alpha: 0.1),
                                    borderRadius: BorderRadius.circular(8),
                                  ),
                                  child: const Icon(Icons.delete_outline, color: Colors.red, size: 18),
                                ),
                              ),
                            ]),
                          ]),
                        );
                      }),
                  ],
                ),
              ),
            ),
    );
  }

  void _openUrl(String? url) {
    if (url == null) return;
    // Ouvre le lien dans un vrai navigateur (url_launcher) — SnackBar en échec.
    showUrlLink(context, url);
  }

  Future<void> _deleteFile(String? id) async {
    if (id == null) return;
    final confirm = await showDialog<bool>(
      context: context,
      builder: (ctx) => AlertDialog(
        backgroundColor: const Color(0xFF111827),
        title: const Text('Confirmer', style: TextStyle(color: Colors.white)),
        content: const Text('Supprimer ce document ?', style: TextStyle(color: Colors.white70)),
        actions: [
          TextButton(onPressed: () => Navigator.pop(ctx, false), child: const Text('Annuler')),
          TextButton(onPressed: () => Navigator.pop(ctx, true),
              child: const Text('Supprimer', style: TextStyle(color: Colors.red))),
        ],
      ),
    );
    if (confirm == true) {
      try {
        await _apiService.delete('/files/$id');
        _loadData();
        if (mounted) _showMessage('Document supprimé');
      } catch (_) { if (mounted) _showMessage('Erreur lors de la suppression'); }
    }
  }

  Widget _smallBadge(Color color, String label) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 6, vertical: 2),
      decoration: BoxDecoration(color: color.withValues(alpha: 0.15), borderRadius: BorderRadius.circular(6)),
      child: Text(label, style: TextStyle(color: color, fontSize: 8, fontWeight: FontWeight.w600)),
    );
  }
}
