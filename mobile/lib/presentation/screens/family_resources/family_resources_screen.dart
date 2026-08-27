import 'package:flutter/material.dart';
import '../../widgets/glass_theme.dart';
import '../../../app.dart';
import '../../../data/services/api_service.dart';
import '../../../../l10n/app_localizations.dart';

/// P1 #43 — Banque de ressources familiales — branché sur API réelle.
class FamilyResourcesScreen extends StatefulWidget {
  const FamilyResourcesScreen({super.key, this.apiService});
  
  final ApiService? apiService;

  @override
  State<FamilyResourcesScreen> createState() => _FamilyResourcesScreenState();
}

class _FamilyResourcesScreenState extends State<FamilyResourcesScreen> {
  late final ApiService _api = widget.apiService ?? ApiService();
  List<dynamic> _resources = [];
  bool _isLoading = true;
  String? _error;

  @override
  void initState() {
    super.initState();
    _load();
  }

  Future<void> _load() async {
    setState(() { _isLoading = true; _error = null; });
    try {
      final userId = AuthState().userId;
      final res = await _api.get('/api/v1/family-resources', params: {'userId': userId, 'size': '50'});
      if (mounted) {
        final data = res.data;
        setState(() {
          _resources = (data is Map && data['content'] is List)
              ? data['content'] as List<dynamic>
              : (data is List ? data : []);
          _isLoading = false;
        });
      }
    } catch (e) {
      if (mounted) setState(() { _error = e.toString(); _isLoading = false; });
    }
  }

  String _categoryIcon(String? cat) {
    switch (cat?.toLowerCase()) {
      case 'etude': case 'biblique': return '📖';
      case 'video': return '🎥';
      case 'document': return '📄';
      case 'musique': return '🎵';
      default: return '📝';
    }
  }

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context);

    // Group by category
    final categories = <String, int>{};
    for (final r in _resources) {
      final cat = (r as Map<String, dynamic>)['categorie'] ?? r['category'] ?? 'Autre';
      categories[cat.toString()] = (categories[cat.toString()] ?? 0) + 1;
    }

    return Scaffold(
      appBar: AppBar(
        title: Text(l10n.familyResourcesTitle),
        backgroundColor: Colors.amber.shade700,
        foregroundColor: Colors.white,
        actions: [IconButton(icon: const Icon(Icons.refresh), onPressed: _load)],
      ),
      floatingActionButton: FloatingActionButton(
        onPressed: () {},
        backgroundColor: Colors.amber.shade700,
        child: const Icon(Icons.add, color: Colors.white),
      ),
      body: _isLoading
          ? const ShimmerLoading(itemCount: 4)
          : _error != null
              ? _buildError(l10n)
              : RefreshIndicator(
                  onRefresh: _load,
                  child: ListView(
                    padding: const EdgeInsets.all(16),
                    children: [
                      // Categories
                      Text(l10n.categories, style: const TextStyle(fontSize: 16, fontWeight: FontWeight.bold)),
                      const SizedBox(height: 8),
                      Wrap(
                        spacing: 8,
                        runSpacing: 8,
                        children: categories.entries.map((e) => Chip(
                          avatar: Text('${e.value}', style: TextStyle(fontSize: 11, color: Colors.amber.shade700, fontWeight: FontWeight.bold)),
                          label: Text('${_categoryIcon(e.key)} ${e.key}'),
                        )).toList(),
                      ),
                      const SizedBox(height: 16),
                      // Resources list
                      Text(l10n.recentResources, style: const TextStyle(fontSize: 16, fontWeight: FontWeight.bold)),
                      const SizedBox(height: 8),
                      if (_resources.isEmpty)
                        Padding(
                          padding: const EdgeInsets.all(24),
                          child: Center(child: Text(l10n.noResources, style: TextStyle(color: Colors.white.withValues(alpha: 0.5)))),
                        )
                      else
                        ..._resources.map((r) {
                          final res = r as Map<String, dynamic>;
                          final title = res['titre'] ?? res['title'] ?? '';
                          final cat = res['categorie'] ?? res['category'] ?? '';
                          final date = res['createdAt']?.toString().split('T').first ?? '';
                          return Card(child: ListTile(
                            leading: Text(_categoryIcon(cat.toString()), style: const TextStyle(fontSize: 24)),
                            title: Text(title, style: const TextStyle(fontSize: 13)),
                            subtitle: Text('$cat • $date'),
                            trailing: const Icon(Icons.chevron_right, size: 20),
                          ));
                        }),
                    ],
                  ),
                ),
    );
  }

  Widget _buildError(AppLocalizations l10n) {
    return Center(child: Column(
      mainAxisAlignment: MainAxisAlignment.center,
      children: [
        Icon(Icons.error_outline, color: Colors.white.withValues(alpha: 0.3), size: 48),
        const SizedBox(height: 12),
        Text(l10n.error, style: TextStyle(color: Colors.white.withValues(alpha: 0.5))),
        const SizedBox(height: 12),
        FilledButton.icon(onPressed: _load, icon: const Icon(Icons.refresh, size: 16), label: Text(l10n.retry)),
      ],
    ));
  }
}
