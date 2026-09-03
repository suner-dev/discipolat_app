import 'package:flutter/material.dart';
import '../../data/services/api_service.dart';
import '../../../presentation/widgets/glass_theme.dart';
import '../../../presentation/widgets/app_drawer.dart';

/// Traductions de sermons — branché sur /api/v1/sermons/translations.
class SermonTranslationsScreen extends StatefulWidget {
  const SermonTranslationsScreen({super.key});

  @override
  State<SermonTranslationsScreen> createState() => _SermonTranslationsScreenState();
}

class _SermonTranslationsScreenState extends State<SermonTranslationsScreen> {
  final _apiService = ApiService();
  List<dynamic> _translations = [];
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
      final res = await _apiService.get('/sermons/translations');
      if (mounted) {
        final data = res.data;
        setState(() {
          _translations = data is List ? data : (data is Map && data['content'] is List ? data['content'] as List<dynamic> : <dynamic>[]);
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

  Future<void> _transcribe(String sermonId) async {
    try {
      await _apiService.post('/sermons/$sermonId/transcribe');
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('Transcription lancée'), backgroundColor: Colors.green),
        );
        _loadData();
      }
    } catch (_) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('Erreur lors de la transcription'), backgroundColor: Colors.red),
        );
      }
    }
  }

  Color _statusColor(String? s) {
    switch (s) {
      case 'EN_COURS':
        return Colors.blue;
      case 'TERMINE':
        return Colors.green;
      case 'ERREUR':
        return Colors.red;
      default:
        return Colors.grey;
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Traductions de sermons'),
        backgroundColor: Colors.indigo.shade600,
        foregroundColor: Colors.white,
        actions: [IconButton(icon: const Icon(Icons.refresh), onPressed: _loadData)],
      ),
      drawer: const AppDrawer(),
      body: _isLoading
          ? const ShimmerLoading(itemCount: 4)
          : _error != null
              ? _buildError()
              : RefreshIndicator(
                  onRefresh: _loadData,
                  child: _translations.isEmpty
                      ? _buildEmpty()
                      : ListView.builder(
                          padding: const EdgeInsets.all(16),
                          itemCount: _translations.length,
                          itemBuilder: (context, i) {
                            final t = _translations[i] as Map<String, dynamic>;
                            final statut = t['statut']?.toString() ?? 'EN_ATTENTE';
                            final id = t['id']?.toString() ?? t['sermonId']?.toString() ?? '';
                            return GlassCard(
                              margin: const EdgeInsets.only(bottom: 10),
                              padding: const EdgeInsets.all(14),
                              child: Column(
                                crossAxisAlignment: CrossAxisAlignment.start,
                                children: [
                                  Row(
                                    children: [
                                      Icon(Icons.translate, color: _statusColor(statut), size: 20),
                                      const SizedBox(width: 8),
                                      Expanded(
                                        child: Text(t['titre']?.toString() ?? t['sermon']?.toString() ?? 'Sermon',
                                            style: const TextStyle(color: Colors.white, fontWeight: FontWeight.w600, fontSize: 14)),
                                      ),
                                      StatusBadge(label: statut, color: _statusColor(statut)),
                                    ],
                                  ),
                                  const SizedBox(height: 8),
                                  Text('Langues: ${t['langues']?.toString() ?? ''}',
                                      style: TextStyle(color: Colors.white.withValues(alpha: 0.5), fontSize: 12)),
                                  if (statut == 'EN_COURS' && t['progression'] != null) ...[
                                    const SizedBox(height: 8),
                                    LinearProgressIndicator(
                                      value: (t['progression'] as num).toDouble() / 100.0,
                                      backgroundColor: Colors.white.withValues(alpha: 0.1),
                                    ),
                                    const SizedBox(height: 4),
                                    Text('${t['progression']}%',
                                        style: TextStyle(color: Colors.white.withValues(alpha: 0.4), fontSize: 11)),
                                  ],
                                  if (statut != 'EN_COURS') ...[
                                    const SizedBox(height: 8),
                                    Align(
                                      alignment: Alignment.centerRight,
                                      child: OutlinedButton.icon(
                                        onPressed: () => _transcribe(id),
                                        icon: const Icon(Icons.play_arrow, size: 16),
                                        label: const Text('Transcrire'),
                                      ),
                                    ),
                                  ],
                                ],
                              ),
                            );
                          },
                        ),
                ),
    );
  }

  Widget _buildEmpty() {
    return Center(
      child: Column(
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          Icon(Icons.translate, size: 48, color: Colors.white.withValues(alpha: 0.2)),
          const SizedBox(height: 12),
          Text('Aucune traduction', style: TextStyle(color: Colors.white.withValues(alpha: 0.5))),
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
