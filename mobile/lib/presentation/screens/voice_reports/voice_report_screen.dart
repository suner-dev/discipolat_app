import 'dart:convert';

import 'package:flutter/material.dart';
import 'package:shared_preferences/shared_preferences.dart';

import '../../widgets/glass_theme.dart';
import '../../widgets/app_drawer.dart';
import '../../../data/services/api_service.dart';

/// Rapports Vocaux IA — dictée terrain avec file d'attente hors-ligne.
///
/// Les rapports dictés sans connexion sont stockés localement puis synchronisés
/// automatiquement dès que le réseau revient (bouton ou au chargement).
class VoiceReportScreen extends StatefulWidget {
  const VoiceReportScreen({super.key});

  @override
  State<VoiceReportScreen> createState() => _VoiceReportScreenState();
}

class _VoiceReportScreenState extends State<VoiceReportScreen> {
  static const _queueKey = 'voice_report_queue';

  final _apiService = ApiService();
  final _textController = TextEditingController();

  List<dynamic> _reports = [];
  List<Map<String, dynamic>> _queue = [];
  bool _isLoading = true;
  bool _isSubmitting = false;
  bool _isSyncing = false;

  @override
  void initState() {
    super.initState();
    _loadQueueAndReports();
  }

  @override
  void dispose() {
    _textController.dispose();
    super.dispose();
  }

  // ── File d'attente locale ────────────────────────────────────────

  Future<void> _loadQueue() async {
    try {
      final prefs = await SharedPreferences.getInstance();
      final raw = prefs.getString(_queueKey);
      if (!mounted) return;
      setState(() {
        _queue = raw == null
            ? []
            : (jsonDecode(raw) as List).cast<Map<String, dynamic>>();
      });
    } catch (_) {
      if (mounted) setState(() => _queue = []);
    }
  }

  Future<void> _enqueue(String transcription) async {
    final entry = <String, dynamic>{
      'transcription': transcription,
      'queuedAt': DateTime.now().toIso8601String(),
    };
    final prefs = await SharedPreferences.getInstance();
    await prefs.setString(_queueKey, jsonEncode([..._queue, entry]));
    await _loadQueue();
  }

  Future<void> _dequeue(Map<String, dynamic> entry) async {
    final prefs = await SharedPreferences.getInstance();
    await prefs.setString(
        _queueKey,
        jsonEncode(
            _queue.where((e) => e['queuedAt'] != entry['queuedAt']).toList()));
    await _loadQueue();
  }

  // ── Chargement & soumission ──────────────────────────────────────

  Future<void> _loadQueueAndReports({bool syncFirst = false}) async {
    await _loadQueue();
    if (syncFirst && _queue.isNotEmpty) await syncQueue();
    await _loadReports();
  }

  Future<void> _loadReports() async {
    try {
      final res = await _apiService.get('/voice-reports');
      if (mounted) {
        setState(() {
          _reports = (res.data is List ? res.data : []) as List<dynamic>;
          _isLoading = false;
        });
      }
    } catch (_) {
      if (mounted) setState(() => _isLoading = false);
    }
  }

  Future<void> _submit() async {
    final text = _textController.text.trim();
    if (text.length < 5) {
      ScaffoldMessenger.of(context).showSnackBar(const SnackBar(
          content: Text('Dictée trop courte'),
          backgroundColor: Colors.orange));
      return;
    }
    setState(() => _isSubmitting = true);
    try {
      await _apiService.post('/voice-reports', data: {'transcription': text});
      _textController.clear();
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(const SnackBar(
            content: Text('✅ Rapport envoyé et analysé par l\'IA'),
            backgroundColor: Colors.green));
      }
      await _loadReports();
    } catch (_) {
      // Hors-ligne ou erreur réseau → mise en file locale
      await _enqueue(text);
      _textController.clear();
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(SnackBar(
            content:
                Text('📴 Hors-ligne — rapport en file (${_queue.length} en attente)'),
            backgroundColor: const Color(0xFF455A64)));
      }
    } finally {
      if (mounted) setState(() => _isSubmitting = false);
    }
  }

  Future<void> syncQueue() async {
    if (_queue.isEmpty || _isSyncing) return;
    setState(() => _isSyncing = true);
    var synced = 0;
    for (final entry in List<Map<String, dynamic>>.from(_queue)) {
      try {
        await _apiService.post('/voice-reports', data: {
          'transcription': entry['transcription'],
        });
        await _dequeue(entry);
        synced++;
      } catch (_) {
        break; // Toujours hors-ligne — réessai plus tard
      }
    }
    await _loadReports();
    if (mounted) {
      setState(() => _isSyncing = false);
      if (synced > 0) {
        ScaffoldMessenger.of(context).showSnackBar(SnackBar(
            content: Text('☁️ $synced rapport(s) synchronisé(s)'),
            backgroundColor: Colors.green));
      }
    }
  }

  String? _moodOf(Map<String, dynamic> report) {
    final analysis = report['analysis'];
    if (analysis is! String || analysis.isEmpty) return null;
    try {
      final decoded = jsonDecode(analysis);
      if (decoded is Map<String, dynamic>) {
        return decoded['humeur'] as String?;
      }
    } catch (_) {}
    return null;
  }

  IconData _moodIcon(String? mood) => switch (mood) {
        'JOYEUX' => Icons.sentiment_satisfied,
        'TRISTE' => Icons.sentiment_dissatisfied,
        _ => Icons.sentiment_neutral,
      };

  Color _moodColor(String? mood) => switch (mood) {
        'JOYEUX' => const Color(0xFF4CAF50),
        'TRISTE' => const Color(0xFF42A5F5),
        _ => Colors.white38,
      };

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Rapports vocaux')),
      drawer: const AppDrawer(),
      floatingActionButton: FloatingActionButton.extended(
        onPressed: _submit,
        icon: _isSubmitting
            ? const SizedBox(
                width: 16,
                height: 16,
                child: CircularProgressIndicator(strokeWidth: 2))
            : const Icon(Icons.mic, size: 18),
        label: const Text('Dicter un rapport'),
      ),
      body: _isLoading
          ? const Center(child: CircularProgressIndicator())
          : RefreshIndicator(
              onRefresh: () => _loadQueueAndReports(syncFirst: true),
              child: ListView(
                padding: const EdgeInsets.fromLTRB(16, 16, 16, 96),
                children: [
                  // Bandeau file hors-ligne
                  if (_queue.isNotEmpty)
                    Padding(
                      padding: const EdgeInsets.only(bottom: 12),
                      child: GlassCard(
                        child: Row(
                          children: [
                            const Icon(Icons.cloud_off,
                                color: Color(0xFFFFB300), size: 22),
                            const SizedBox(width: 12),
                            Expanded(
                              child: Text(
                                '${_queue.length} rapport(s) en attente de synchronisation',
                                style: const TextStyle(color: Colors.white70),
                              ),
                            ),
                            TextButton(
                              onPressed: syncQueue,
                              child: _isSyncing
                                  ? const SizedBox(
                                      width: 16,
                                      height: 16,
                                      child: CircularProgressIndicator(
                                          strokeWidth: 2))
                                  : const Text('Synchroniser'),
                            ),
                          ],
                        ),
                      ),
                    ),

                  // Zone de dictée
                  GlassCard(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Row(children: [
                          const Icon(Icons.edit_note, color: AppColors.defaultPrimary),
                          const SizedBox(width: 8),
                          const Text('Nouveau rapport',
                              style: TextStyle(fontWeight: FontWeight.w600)),
                        ],),
                        const SizedBox(height: 10),
                        TextField(
                          controller: _textController,
                          maxLines: 4,
                          maxLength: 2000,
                          style: const TextStyle(color: Colors.white),
                          decoration: InputDecoration(
                            hintText:
                                'Décrivez votre visite : âmes rencontrées, humeur, actions menées…',
                            hintStyle:
                                const TextStyle(color: Colors.white24),
                            filled: true,
                            fillColor: Colors.white.withValues(alpha: 0.04),
                            border: OutlineInputBorder(
                              borderRadius: BorderRadius.circular(12),
                              borderSide: BorderSide.none,
                            ),
                          ),
                        ),
                        Align(
                          alignment: Alignment.centerRight,
                          child: TextButton.icon(
                            onPressed: _isSubmitting ? null : _submit,
                            icon: const Icon(Icons.send, size: 16),
                            label: const Text('Envoyer'),
                          ),
                        ),
                      ],
                    ),
                  ),
                  const SizedBox(height: 16),

                  // Historique
                  const Text('Historique',
                      style:
                          TextStyle(fontSize: 15, fontWeight: FontWeight.w600)),
                  const SizedBox(height: 10),
                  ..._reports.map((r) {
                    final report = r as Map<String, dynamic>;
                    final mood = _moodOf(report);
                    return Padding(
                      padding: const EdgeInsets.only(bottom: 10),
                      child: GlassCard(
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Row(
                              mainAxisAlignment:
                                  MainAxisAlignment.spaceBetween,
                              children: [
                                Text(
                                  _formatDate(report['createdAt']),
                                  style: const TextStyle(
                                      color: Colors.white38, fontSize: 12),
                                ),
                                Icon(_moodIcon(mood),
                                    color: _moodColor(mood), size: 20),
                              ],
                            ),
                            const SizedBox(height: 6),
                            Text(
                              (report['transcription'] as String?) ?? '',
                              maxLines: 3,
                              overflow: TextOverflow.ellipsis,
                              style: const TextStyle(
                                  color: Colors.white70, fontSize: 13),
                            ),
                          ],
                        ),
                      ),
                    );
                  }),
                  if (_reports.isEmpty && _queue.isEmpty)
                    const GlassCard(
                      child: Center(
                        child: Padding(
                          padding: EdgeInsets.all(20),
                          child: Text(
                            'Aucun rapport pour le moment.\nDictez votre premier retour de terrain !',
                            textAlign: TextAlign.center,
                            style: TextStyle(color: Colors.white54),
                          ),
                        ),
                      ),
                    ),
                ],
              ),
            ),
    );
  }

  String _formatDate(dynamic iso) {
    try {
      final d = DateTime.parse(iso as String).toLocal();
      return '${d.day.toString().padLeft(2, '0')}/${d.month.toString().padLeft(2, '0')} à ${d.hour}:${d.minute.toString().padLeft(2, '0')}';
    } catch (_) {
      return '';
    }
  }
}
