import 'package:flutter/material.dart';
import '../../widgets/glass_theme.dart';
import '../../widgets/app_drawer.dart';
import '../../../data/services/api_service.dart';
import '../../../../l10n/app_localizations.dart';

/// P1 #58 — Annuaire de l'église (fiches publiques opt-in) — branché API.
class ChurchDirectoryScreen extends StatefulWidget {
  const ChurchDirectoryScreen({super.key, this.apiService});

  final ApiService? apiService;

  @override
  State<ChurchDirectoryScreen> createState() => _ChurchDirectoryScreenState();
}

class _ChurchDirectoryScreenState extends State<ChurchDirectoryScreen> {
  late final ApiService _api = widget.apiService ?? ApiService();
  List<dynamic> _entries = [];
  bool _isLoading = true;
  String? _error;
  String _searchQuery = '';

  @override
  void initState() {
    super.initState();
    _load();
  }

  Future<void> _load() async {
    setState(() { _isLoading = true; _error = null; });
    try {
      final res = await _api.get('/api/v1/directory/all');
      if (mounted) {
        setState(() {
          _entries = (res.data is List ? res.data : []) as List<dynamic>;
          _isLoading = false;
        });
      }
    } catch (e) {
      if (mounted) setState(() { _error = e.toString(); _isLoading = false; });
    }
  }

  List<dynamic> get _filtered {
    if (_searchQuery.isEmpty) return _entries;
    final q = _searchQuery.toLowerCase();
    return _entries.where((e) {
      final entry = e as Map<String, dynamic>;
      final name = '${entry['prenom'] ?? ''} ${entry['nom'] ?? ''}'.toLowerCase();
      final dept = (entry['departement'] ?? '').toString().toLowerCase();
      return name.contains(q) || dept.contains(q);
    }).toList();
  }

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context);

    return Scaffold(
      appBar: AppBar(
        title: Text(l10n.churchDirectoryTitle),
        backgroundColor: Colors.blue.shade600,
        foregroundColor: Colors.white,
        actions: [IconButton(icon: const Icon(Icons.refresh), onPressed: _load)],
      ),
      drawer: const AppDrawer(),
      body: _isLoading
          ? const ShimmerLoading(itemCount: 8)
          : _error != null
              ? Center(child: Column(
                  mainAxisAlignment: MainAxisAlignment.center,
                  children: [
                    Icon(Icons.error_outline, color: Colors.white.withValues(alpha: 0.3), size: 48),
                    const SizedBox(height: 12),
                    Text(l10n.directoryError, style: TextStyle(color: Colors.white.withValues(alpha: 0.5))),
                    const SizedBox(height: 12),
                    FilledButton.icon(onPressed: _load, icon: const Icon(Icons.refresh, size: 16), label: Text(l10n.retry)),
                  ],
                ))
              : RefreshIndicator(
                  onRefresh: _load,
                  child: ListView(
                    padding: const EdgeInsets.all(16),
                    children: [
                      // Search bar
                      TextField(
                        onChanged: (v) => setState(() => _searchQuery = v),
                        decoration: InputDecoration(
                          hintText: l10n.searchMember,
                          prefixIcon: const Icon(Icons.search),
                          border: OutlineInputBorder(borderRadius: BorderRadius.circular(12)),
                          filled: true,
                          fillColor: Colors.white.withValues(alpha: 0.08),
                        ),
                      ),
                      const SizedBox(height: 16),
                      if (_filtered.isEmpty)
                        Padding(
                          padding: const EdgeInsets.all(24),
                          child: Center(child: Text(l10n.directoryEmpty, style: TextStyle(color: Colors.white.withValues(alpha: 0.5)))),
                        )
                      else
                        ..._filtered.map((e) {
                          final entry = e as Map<String, dynamic>;
                          final prenom = entry['prenom']?.toString() ?? '';
                          final nom = entry['nom']?.toString() ?? '';
                          final name = '$prenom $nom'.trim();
                          final family = entry['famille']?.toString() ?? entry['familyName']?.toString() ?? l10n.unknownFamily;
                          final role = entry['role']?.toString() ?? entry['departement']?.toString() ?? l10n.unknownRole;
                          final isPublic = entry['publicProfil'] == true || entry['publicProfile'] == true;
                          final initial = name.isNotEmpty ? name[0].toUpperCase() : '?';
                          return GlassCard(
                            margin: const EdgeInsets.only(bottom: 8),
                            padding: const EdgeInsets.all(0),
                            child: ListTile(
                              leading: CircleAvatar(
                                backgroundColor: isPublic ? Colors.blue.withValues(alpha: 0.2) : Colors.white.withValues(alpha: 0.1),
                                child: Text(initial, style: TextStyle(color: isPublic ? Colors.blue : Colors.grey)),
                              ),
                              title: Text(name),
                              subtitle: Text('$family • $role'),
                              trailing: Icon(
                                isPublic ? Icons.visibility : Icons.visibility_off,
                                color: isPublic ? Colors.green : Colors.grey,
                                size: 18,
                              ),
                            ),
                          );
                        }),
                    ],
                  ),
                ),
    );
  }
}
