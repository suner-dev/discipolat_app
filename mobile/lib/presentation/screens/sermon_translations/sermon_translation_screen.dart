import 'package:flutter/material.dart';
import '../../widgets/glass_theme.dart';
import '../../../data/services/api_service.dart';
import '../../../../l10n/app_localizations.dart';

/// P1 #22 — Traduction en direct des sermons — branché sur API réelle.
class SermonTranslationScreen extends StatefulWidget {
  const SermonTranslationScreen({super.key, this.apiService});
  
  final ApiService? apiService;

  @override
  State<SermonTranslationScreen> createState() => _SermonTranslationScreenState();
}

class _SermonTranslationScreenState extends State<SermonTranslationScreen> {
  late final ApiService _api = widget.apiService ?? ApiService();
  List<dynamic> _translations = [];
  Map<String, dynamic>? _activeTranslation;
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
      final res = await _api.get('/api/v1/sermons/translations');
      if (mounted) {
        final data = res.data;
        final list = (data is Map && data['content'] is List)
            ? data['content'] as List<dynamic>
            : (data is List ? data : []);
        setState(() {
          _translations = list;
          _activeTranslation = list.where((t) =>
              (t as Map<String, dynamic>)['statut'] == 'EN_COURS').firstOrNull as Map<String, dynamic>?;
          _isLoading = false;
        });
      }
    } catch (e) {
      if (mounted) setState(() { _error = e.toString(); _isLoading = false; });
    }
  }

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context);
    final completedTranslations = _translations.where((t) =>
        (t as Map<String, dynamic>)['statut'] == 'TERMINE').toList();

    return Scaffold(
      appBar: AppBar(
        title: Text(l10n.sermonTranslationTitle),
        backgroundColor: Colors.indigo.shade600,
        foregroundColor: Colors.white,
        actions: [IconButton(icon: const Icon(Icons.refresh), onPressed: _load)],
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
                      // Active translation
                      if (_activeTranslation != null) ...[
                        GlassCard(
                          padding: const EdgeInsets.all(16),
                          child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
                            Row(children: [
                              const Icon(Icons.circle, color: Colors.red, size: 12),
                              const SizedBox(width: 8),
                              Text(l10n.translationInProgress, style: const TextStyle(fontWeight: FontWeight.bold)),
                              const Spacer(),
                              const CircularProgressIndicator(strokeWidth: 2),
                            ]),
                            const SizedBox(height: 12),
                            Text(_activeTranslation!['langues']?.toString() ?? '',
                                style: TextStyle(color: Colors.grey)),
                            const SizedBox(height: 8),
                            LinearProgressIndicator(value: ((_activeTranslation!['progression'] ?? 65) as num).toDouble() / 100.0),
                            const SizedBox(height: 4),
                            Text('${_activeTranslation!['progression'] ?? 65}%'),
                          ]),
                        ),
                        const SizedBox(height: 16),
                      ],
                      // Completed translations
                      Text(l10n.recentTranslations, style: const TextStyle(fontSize: 16, fontWeight: FontWeight.bold)),
                      const SizedBox(height: 8),
                      if (completedTranslations.isEmpty)
                        Padding(
                          padding: const EdgeInsets.all(24),
                          child: Center(child: Text(l10n.noTranslations, style: TextStyle(color: Colors.white.withValues(alpha: 0.5)))),
                        )
                      else
                        ...completedTranslations.map((t) {
                          final tr = t as Map<String, dynamic>;
                          return Card(child: ListTile(
                            leading: const Icon(Icons.translate),
                            title: Text(tr['titre'] ?? tr['sermon'] ?? ''),
                            subtitle: Text(tr['langues']?.toString() ?? ''),
                            trailing: Chip(
                              label: Text(tr['statut'] ?? '', style: const TextStyle(fontSize: 12)),
                              backgroundColor: Colors.green.shade50,
                            ),
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
