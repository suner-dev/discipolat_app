import 'dart:io';
import 'dart:typed_data';
import 'package:flutter/material.dart';
import 'package:path_provider/path_provider.dart';
import 'package:record/record.dart';
import '../../data/services/api_service.dart';
import '../../../presentation/widgets/glass_theme.dart';
import '../../../presentation/widgets/app_drawer.dart';

/// Voix — branché sur /api/v1/voice.
class VoicesScreen extends StatefulWidget {
  const VoicesScreen({super.key});

  @override
  State<VoicesScreen> createState() => _VoicesScreenState();
}

class _VoicesScreenState extends State<VoicesScreen> {
  final _apiService = ApiService();
  final AudioRecorder _recorder = AudioRecorder();
  Map<String, dynamic>? _health;
  bool _isLoading = true;
  bool _isTranscribing = false;
  bool _isRecording = false;
  String? _error;
  String? _transcriptionResult;

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
      final res = await _apiService.get('/voice/health');
      if (mounted) {
        setState(() {
          _health = res.data is Map<String, dynamic> ? res.data as Map<String, dynamic> : null;
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

  /// Enregistre réellement le micro (package `record`), puis envoie le fichier
  /// audio en multipart à POST /api/v1/voice/transcribe (part `file`).
  Future<void> _transcribe() async {
    if (_isRecording) {
      await _stopRecordingAndTranscribe();
      return;
    }

    final hasPermission = await _recorder.hasPermission();
    if (!hasPermission) {
      if (mounted) {
        setState(() => _error = 'Autorisation micro requise (paramètres système)');
      }
      return;
    }

    try {
      final dir = await getTemporaryDirectory();
      final path = '${dir.path}/voix_${DateTime.now().millisecondsSinceEpoch}.m4a';
      await _recorder.start(
        const RecordConfig(
          encoder: AudioEncoder.aacLc,
          bitRate: 128000,
          sampleRate: 44100,
        ),
        path: path,
      );
      if (mounted) {
        setState(() {
          _isRecording = true;
          _transcriptionResult = null;
          _error = null;
        });
      }
    } catch (_) {
      if (mounted) {
        setState(() {
          _isRecording = false;
          _error = "Impossible de démarrer l'enregistrement";
        });
      }
    }
  }

  Future<void> _stopRecordingAndTranscribe() async {
    setState(() => _isRecording = false);
    try {
      final filePath = await _recorder.stop();
      if (filePath == null || filePath.isEmpty) {
        if (mounted) setState(() => _error = "Enregistrement vide");
        return;
      }
      final file = File(filePath);
      if (!await file.exists() || await file.length() == 0) {
        if (mounted) setState(() => _error = "Enregistrement vide");
        return;
      }

      if (mounted) setState(() => _isTranscribing = true);
      final bytes = await file.readAsBytes();
      await _uploadAndShowTranscription(Uint8List.fromList(bytes), filePath.split('/').last);
    } catch (_) {
      if (mounted) {
        setState(() {
          _isTranscribing = false;
          _error = 'Erreur lors de la transcription';
        });
      }
    }
  }

  /// POST /voice/transcribe (le préfixe /api/v1 est porté par ApiConfig.baseUrl)
  /// en multipart/form-data avec le part `file` attendu par le backend.
  Future<void> _uploadAndShowTranscription(Uint8List bytes, String filename) async {
    try {
      final res = await _apiService.postMultipart(
        '/voice/transcribe',
        fieldName: 'file',
        fileBytes: bytes,
        filename: filename,
        data: {'language': 'fr'},
      );
      if (mounted) {
        setState(() {
          _isTranscribing = false;
          final data = res.data is Map ? res.data as Map<String, dynamic> : null;
          _transcriptionResult =
              data?['transcription']?.toString() ?? data?['text']?.toString() ?? '';
          if (_transcriptionResult!.isEmpty) {
            _transcriptionResult = 'Aucune parole reconnue';
          }
        });
      }
    } catch (e) {
      if (mounted) {
        final notConfigured = e.toString().contains('503') ||
            e.toString().contains('STT_NOT_CONFIGURED');
        setState(() {
          _isTranscribing = false;
          _error = notConfigured
              ? 'Transcription non configurée sur le serveur'
              : 'Erreur lors de la transcription';
        });
      }
    }
  }

  @override
  void dispose() {
    _recorder.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final status = _health?['status']?.toString() ?? 'UNKNOWN';

    return Scaffold(
      appBar: AppBar(
        title: const Text('Voix & Transcription'),
        backgroundColor: Colors.deepPurple,
        foregroundColor: Colors.white,
        actions: [IconButton(icon: const Icon(Icons.refresh), onPressed: _loadData)],
      ),
      drawer: const AppDrawer(),
      body: _isLoading
          ? const Center(child: CircularProgressIndicator())
          : _error != null && _health == null
              ? _buildError()
              : RefreshIndicator(
                  onRefresh: _loadData,
                  child: ListView(
                    padding: const EdgeInsets.all(16),
                    children: [
                      GlassCard(
                        padding: const EdgeInsets.all(20),
                        child: Column(
                          children: [
                            Icon(
                              status.toUpperCase() == 'UP' ? Icons.mic : Icons.mic_off,
                              size: 48,
                              color: status.toUpperCase() == 'UP' ? Colors.green : Colors.red,
                            ),
                            const SizedBox(height: 12),
                            Text('Service vocal: $status',
                                style: TextStyle(
                                    color: status.toUpperCase() == 'UP' ? Colors.green : Colors.red,
                                    fontSize: 16,
                                    fontWeight: FontWeight.bold)),
                            if (_health != null) ...[
                              const SizedBox(height: 16),
                              ...(_health!.entries.where((e) => e.key != 'status').map((e) => Padding(
                                    padding: const EdgeInsets.only(bottom: 8),
                                    child: Row(
                                      mainAxisAlignment: MainAxisAlignment.spaceBetween,
                                      children: [
                                        Text(e.key, style: TextStyle(color: Colors.white.withValues(alpha: 0.6), fontSize: 13)),
                                        Text('${e.value}',
                                            style: const TextStyle(color: Colors.white, fontSize: 13, fontWeight: FontWeight.w600)),
                                      ],
                                    ),
                                  ))),
                            ],
                          ],
                        ),
                      ),
                      const SizedBox(height: 24),
                      GlassCard(
                        padding: const EdgeInsets.all(20),
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            const Text('Transcription',
                                style: TextStyle(color: Colors.white, fontWeight: FontWeight.bold, fontSize: 16)),
                            const SizedBox(height: 16),
                            SizedBox(
                              width: double.infinity,
                              child: FilledButton.icon(
                                onPressed: _isTranscribing ? null : _transcribe,
                                icon: _isTranscribing
                                    ? const SizedBox(width: 16, height: 16, child: CircularProgressIndicator(strokeWidth: 2, color: Colors.white))
                                    : Icon(_isRecording ? Icons.stop : Icons.mic, size: 18),
                                label: Text(_isTranscribing
                                    ? 'Transcription en cours...'
                                    : _isRecording
                                        ? 'Arrêter et transcrire'
                                        : 'Enregistrer et transcrire'),
                                style: FilledButton.styleFrom(
                                  backgroundColor: _isRecording ? Colors.red : Colors.deepPurple,
                                ),
                              ),
                            ),
                          ],
                        ),
                      ),
                      if (_transcriptionResult != null) ...[
                        const SizedBox(height: 16),
                        GlassCard(
                          padding: const EdgeInsets.all(16),
                          borderColor: Colors.green.withValues(alpha: 0.3),
                          child: Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              Row(
                                children: [
                                  const Icon(Icons.check_circle, color: Colors.green, size: 20),
                                  const SizedBox(width: 8),
                                  const Text('Résultat', style: TextStyle(color: Colors.green, fontWeight: FontWeight.bold)),
                                ],
                              ),
                              const SizedBox(height: 8),
                              Text(_transcriptionResult!, style: TextStyle(color: Colors.white.withValues(alpha: 0.8), fontSize: 14)),
                            ],
                          ),
                        ),
                      ],
                      if (_error != null && _health != null) ...[
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
                    ],
                  ),
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
