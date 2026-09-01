import 'dart:async';
import 'dart:io';
import 'dart:typed_data';
import 'package:record/record.dart';
import 'package:flutter/material.dart';
import 'package:path_provider/path_provider.dart';
import '../../widgets/glass_theme.dart';
import '../../widgets/app_drawer.dart';
import '../../../data/services/api_service.dart';

/// P0 #5 — Assistant vocal conversationnel PasteurBot (mobile).
///
/// Chat vocal avec :
/// - Transcription de la voix (texte)
/// - Intent détectée
/// - Réponse contextuelle de l'IA
/// - Suggestions de commandes suivantes
/// - Commandes vocales prédéfinies
class VoiceAssistantScreen extends StatefulWidget {
  const VoiceAssistantScreen({super.key});

  @override
  State<VoiceAssistantScreen> createState() => _VoiceAssistantScreenState();
}

class _VoiceAssistantScreenState extends State<VoiceAssistantScreen>
    with SingleTickerProviderStateMixin {
  final _api = ApiService();
  final _inputController = TextEditingController();
  final _scrollController = ScrollController();
  final _sessionId = ValueNotifier<String>(_generateSessionId());
  final AudioRecorder _recorder = AudioRecorder();
  bool _sttConfigured = true;

  final List<_ChatMessage> _messages = [];
  bool _isProcessing = false;
  bool _isRecording = false;
  bool _showCommands = false;
  late AnimationController _pulseController;
  late Animation<double> _pulseAnimation;

  static String _generateSessionId() {
    return DateTime.now().millisecondsSinceEpoch.toRadixString(36);
  }

  static const _quickCommands = [
    'Montre-moi les familles en décrochement',
    'Combien de nouveaux convertis ce mois ?',
    'Génère un rapport de la semaine',
    'Quels sont les prochains événements ?',
    'Montre le taux de présence',
    'Quelles sont les alertes actives ?',
  ];

// ── API calls ──────────────────────────────────────

  Future<void> _processMessage(String text) async {
    if (text.trim().isEmpty || _isProcessing) return;

    final userMsg = _ChatMessage(
      role: 'user',
      content: text.trim(),
      timestamp: DateTime.now(),
    );
    setState(() {
      _messages.add(userMsg);
      _isProcessing = true;
    });
    _inputController.clear();
    _scrollToBottom();

    try {
      final res = await _api.post('/voice/process', data: {
        'transcription': text.trim(),
        'sessionId': _sessionId.value,
      });
      final data = res.data as Map<String, dynamic>;
      final assistantMsg = _ChatMessage(
        role: 'assistant',
        content: data['reply'] ?? 'Pas de réponse.',
        timestamp: DateTime.now(),
        intent: data['intent'] as String?,
        suggestions: (data['suggestions'] as List<dynamic>?)
            ?.map((s) => _Suggestion(
                  command: s['command'] as String? ?? '',
                  icon: s['icon'] as String? ?? '💡',
                ))
            .toList(),
      );
      if (mounted) {
        setState(() {
          _messages.add(assistantMsg);
          _isProcessing = false;
        });
        _scrollToBottom();
      }
    } catch (_) {
      if (mounted) {
        setState(() {
          _messages.add(_ChatMessage(
            role: 'assistant',
            content:
                '🤖 Je suis en mode hors-ligne. Réessayez quand la connexion sera rétablie.',
            timestamp: DateTime.now(),
          ));
          _isProcessing = false;
        });
      }
    }
  }

  // Future: load commands from API dynamically
  // ignore: unused_element
  Future<void> _loadCommands() async {
    try {
      final res = await _api.get('/voice/commands');
      if (mounted && res.data is List) {
        setState(() => _showCommands = true);
      }
    } catch (_) {}
  }

  // ── Real recording + STT ─────────────────────────

  @override
  void initState() {
    super.initState();
    _pulseController = AnimationController(
      vsync: this,
      duration: const Duration(milliseconds: 1200),
    )..repeat(reverse: true);
    _pulseAnimation = Tween<double>(begin: 0.8, end: 1.0).animate(
      CurvedAnimation(parent: _pulseController, curve: Curves.easeInOut),
    );
    _checkSttAvailability();
  }

  Future<void> _checkSttAvailability() async {
    try {
      final res = await _api.get('/voice/stt-status');
      if (mounted) {
        setState(() {
          _sttConfigured = (res.data as Map<String, dynamic>)['configured'] == true;
        });
      }
    } catch (_) {
      if (mounted) setState(() => _sttConfigured = false);
    }
  }

  Future<void> _toggleRecording() async {
    if (_isRecording) {
      await _stopRecording();
      return;
    }

    final hasPermission = await AudioRecorder().hasPermission();
    if (!hasPermission) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(
            content: Text('🎙️ Autorisation micro requise (paramètres système)'),
            backgroundColor: Color(0xFFB45309),
          ),
        );
      }
      return;
    }

    try {
      final dir = await getTemporaryDirectory();
      final path =
          '${dir.path}/pasteurbot_${DateTime.now().millisecondsSinceEpoch}.m4a';
      await _recorder.start(
        const RecordConfig(
          encoder: AudioEncoder.aacLc,
          bitRate: 128000,
          sampleRate: 44100,
        ),
        path: path,
      );
      if (mounted) {
        setState(() => _isRecording = true);
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(
            content: Text('🎙️ Enregistrement en cours... Speak now'),
            duration: Duration(seconds: 2),
            backgroundColor: Color(0xFF0891B2),
          ),
        );
      }
    } catch (_) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(
            content: Text('Impossible de démarrer l\'enregistrement'),
            backgroundColor: Color(0xFFB45309),
          ),
        );
      }
    }
  }

  Future<void> _stopRecording() async {
    if (!_isRecording) return;
    setState(() => _isRecording = false);
    try {
      final filePath = await _recorder.stop();
      if (filePath == null || filePath.isEmpty) return;
      final file = File(filePath);
      if (!await file.exists() || await file.length() == 0) return;

      final bytes = await file.readAsBytes();
      await _processTranscription(bytes, filePath.split('/').last);
    } catch (_) {
      // silencieux : le micro peut ne pas être disponible sur l'émulateur
    }
  }

  Future<void> _processTranscription(List<int> bytes, String filename) async {
    setState(() => _isProcessing = true);
    ScaffoldMessenger.of(context).removeCurrentSnackBar();
    try {
      final res = await _api.postMultipart(
        '/voice/transcribe',
        fieldName: 'file',
        fileBytes: Uint8List.fromList(bytes),
        filename: filename,
        data: {'sessionId': _sessionId.value},
      );
      final data = res.data as Map<String, dynamic>;
      final transcription = data['transcription']?.toString() ?? data['text']?.toString() ?? '';
      if (transcription.isEmpty) {
        if (mounted) {
          setState(() => _isProcessing = false);
          ScaffoldMessenger.of(context).showSnackBar(
            const SnackBar(content: Text('Aucune parole reconnue')),
          );
        }
        return;
      }
      _inputController.text = transcription;
      await _processMessage(transcription);
    } catch (e) {
      if (!mounted) return;
      setState(() => _isProcessing = false);
      final message = e.toString().contains('503') ||
              e.toString().contains('STT_NOT_CONFIGURED')
          ? '🤖 La transcription vocale n\'est pas configurée sur le serveur. Tapez votre question ou activez le provider STT.'
          : '🤖 Transcription impossible pour le moment. Réessayez.';
      _messages.add(_ChatMessage(
        role: 'assistant',
        content: message,
        timestamp: DateTime.now(),
      ));
      _scrollToBottom();
    }
  }

  @override
  void dispose() {
    _recorder.dispose();
    _inputController.dispose();
    _scrollController.dispose();
    _pulseController.dispose();
    _sessionId.dispose();
    super.dispose();
  }

  // ── Helpers ──────────────────────────────────────

  void _scrollToBottom() {
    WidgetsBinding.instance.addPostFrameCallback((_) {
      if (_scrollController.hasClients) {
        _scrollController.animateTo(
          _scrollController.position.maxScrollExtent,
          duration: const Duration(milliseconds: 300),
          curve: Curves.easeOut,
        );
      }
    });
  }

  void _onSuggestionTap(String command) {
    _inputController.text = command;
    _processMessage(command);
  }

  // ── Build ──────────────────────────────────────

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: const Color(0xFF030712),
      appBar: AppBar(
        title: const Text('🎙️ PasteurBot Vocal',
            style: TextStyle(color: Colors.white, fontWeight: FontWeight.bold)),
        backgroundColor: Colors.transparent,
        elevation: 0,
        actions: [
          IconButton(
            icon: Icon(
              _showCommands ? Icons.keyboard_arrow_up : Icons.help_outline,
              color: Colors.white70,
            ),
            onPressed: () => setState(() => _showCommands = !_showCommands),
          ),
        ],
      ),
      drawer: const AppDrawer(),
      body: Column(
        children: [
          // Commands panel
          if (_showCommands) _buildCommandsPanel(),

          // Messages
          Expanded(
            child: _messages.isEmpty
                ? _buildEmptyState()
                : ListView.builder(
                    controller: _scrollController,
                    padding: const EdgeInsets.all(16),
                    itemCount: _messages.length + (_isProcessing ? 1 : 0),
                    itemBuilder: (ctx, i) {
                      if (i == _messages.length) return _buildTypingIndicator();
                      return _buildMessageBubble(_messages[i]);
                    },
                  ),
          ),

          // Input bar
          _buildInputBar(),
        ],
      ),
    );
  }

  Widget _buildEmptyState() {
    return Center(
      child: SingleChildScrollView(
        padding: const EdgeInsets.all(24),
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Container(
              width: 80,
              height: 80,
              decoration: BoxDecoration(
                gradient: const LinearGradient(
                  colors: [Color(0xFF06B6D4), Color(0xFF3B82F6)],
                ),
                borderRadius: BorderRadius.circular(24),
                boxShadow: [
                  BoxShadow(
                    color: const Color(0xFF06B6D4).withAlpha(80),
                    blurRadius: 24,
                    spreadRadius: 2,
                  ),
                ],
              ),
              child: const Icon(Icons.mic, color: Colors.white, size: 40),
            ),
            const SizedBox(height: 20),
            const Text(
              'Comment puis-je vous aider ?',
              style: TextStyle(
                  color: Colors.white,
                  fontSize: 18,
                  fontWeight: FontWeight.bold),
            ),
            const SizedBox(height: 8),
            Text(
              'Tapez ou enregistrez une question vocale.',
              style: TextStyle(color: Colors.white.withAlpha(140), fontSize: 13),
            ),
            if (!_sttConfigured)
              Padding(
                padding: const EdgeInsets.only(top: 8),
                child: Text(
                  '⚙️ Transcription vocale indisponible : configurez le provider STT côté serveur, puis réessayez.',
                  textAlign: TextAlign.center,
                  style: TextStyle(
                      color: Colors.amber.shade300, fontSize: 11),
                ),
              ),
            const SizedBox(height: 24),
            Wrap(
              spacing: 8,
              runSpacing: 8,
              alignment: WrapAlignment.center,
              children: _quickCommands.map((cmd) {
                return GestureDetector(
                  onTap: () => _processMessage(cmd),
                  child: Container(
                    padding:
                        const EdgeInsets.symmetric(horizontal: 14, vertical: 8),
                    decoration: BoxDecoration(
                      color: Colors.white.withAlpha(6),
                      borderRadius: BorderRadius.circular(20),
                      border:
                          Border.all(color: const Color(0xFF06B6D4).withAlpha(40)),
                    ),
                    child: Text(
                      '🗣️ $cmd',
                      style: TextStyle(
                          color: Colors.white.withAlpha(180), fontSize: 12),
                    ),
                  ),
                );
              }).toList(),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildMessageBubble(_ChatMessage msg) {
    final isUser = msg.role == 'user';
    return Padding(
      padding: const EdgeInsets.only(bottom: 12),
      child: Row(
        mainAxisAlignment:
            isUser ? MainAxisAlignment.end : MainAxisAlignment.start,
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          if (!isUser) ...[
            Container(
              width: 32,
              height: 32,
              decoration: BoxDecoration(
                gradient: const LinearGradient(
                    colors: [Color(0xFF06B6D4), Color(0xFF3B82F6)]),
                borderRadius: BorderRadius.circular(10),
              ),
              child: const Icon(Icons.smart_toy, color: Colors.white, size: 16),
            ),
            const SizedBox(width: 8),
          ],
          Flexible(
            child: Column(
              crossAxisAlignment:
                  isUser ? CrossAxisAlignment.end : CrossAxisAlignment.start,
              children: [
                Container(
                  padding: const EdgeInsets.all(14),
                  decoration: BoxDecoration(
                    color: isUser
                        ? AppColors.primary
                        : Colors.white.withAlpha(6),
                    borderRadius: BorderRadius.only(
                      topLeft: const Radius.circular(16),
                      topRight: const Radius.circular(16),
                      bottomLeft: Radius.circular(isUser ? 16 : 4),
                      bottomRight: Radius.circular(isUser ? 4 : 16),
                    ),
                    border: isUser
                        ? null
                        : Border.all(color: Colors.white.withAlpha(10)),
                  ),
                  child: Text(
                    msg.content,
                    style: TextStyle(
                      color: Colors.white.withAlpha(isUser ? 240 : 200),
                      fontSize: 14,
                      height: 1.4,
                    ),
                  ),
                ),
                // Intent badge
                if (msg.intent != null)
                  Padding(
                    padding: const EdgeInsets.only(top: 4),
                    child: Container(
                      padding: const EdgeInsets.symmetric(
                          horizontal: 8, vertical: 2),
                      decoration: BoxDecoration(
                        color: const Color(0xFF06B6D4).withAlpha(20),
                        borderRadius: BorderRadius.circular(8),
                      ),
                      child: Text(
                        msg.intent!,
                        style: TextStyle(
                            color: const Color(0xFF06B6D4),
                            fontSize: 10,
                            fontWeight: FontWeight.w600),
                      ),
                    ),
                  ),
                // Suggestions
                if (msg.suggestions != null && msg.suggestions!.isNotEmpty)
                  Padding(
                    padding: const EdgeInsets.only(top: 8),
                    child: Wrap(
                      spacing: 6,
                      runSpacing: 6,
                      children: msg.suggestions!.map((s) {
                        return GestureDetector(
                          onTap: () => _onSuggestionTap(s.command),
                          child: Container(
                            padding: const EdgeInsets.symmetric(
                                horizontal: 10, vertical: 4),
                            decoration: BoxDecoration(
                              color: const Color(0xFF06B6D4).withAlpha(15),
                              borderRadius: BorderRadius.circular(12),
                              border: Border.all(
                                  color: const Color(0xFF06B6D4).withAlpha(30)),
                            ),
                            child: Row(
                              mainAxisSize: MainAxisSize.min,
                              children: [
                                Text(s.icon, style: const TextStyle(fontSize: 12)),
                                const SizedBox(width: 4),
                                Text(
                                  s.command,
                                  style: const TextStyle(
                                      color: Color(0xFF06B6D4),
                                      fontSize: 11,
                                      fontWeight: FontWeight.w500),
                                ),
                                const Icon(Icons.chevron_right,
                                    color: Color(0xFF06B6D4), size: 14),
                              ],
                            ),
                          ),
                        );
                      }).toList(),
                    ),
                  ),
                // Timestamp
                Padding(
                  padding: const EdgeInsets.only(top: 2),
                  child: Text(
                    '${msg.timestamp.hour}:${msg.timestamp.minute.toString().padLeft(2, '0')}',
                    style: TextStyle(
                        color: Colors.white.withAlpha(60), fontSize: 10),
                  ),
                ),
              ],
            ),
          ),
          if (isUser) ...[
            const SizedBox(width: 8),
            Container(
              width: 32,
              height: 32,
              decoration: BoxDecoration(
                color: Colors.white.withAlpha(10),
                borderRadius: BorderRadius.circular(10),
              ),
              child: Icon(Icons.person, color: Colors.white.withAlpha(120), size: 16),
            ),
          ],
        ],
      ),
    );
  }

  Widget _buildTypingIndicator() {
    return Padding(
      padding: const EdgeInsets.only(bottom: 12),
      child: Row(
        children: [
          Container(
            width: 32,
            height: 32,
            decoration: BoxDecoration(
              gradient: const LinearGradient(
                  colors: [Color(0xFF06B6D4), Color(0xFF3B82F6)]),
              borderRadius: BorderRadius.circular(10),
            ),
            child: const Icon(Icons.smart_toy, color: Colors.white, size: 16),
          ),
          const SizedBox(width: 8),
          GlassCard(
            padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
            child: Row(
              mainAxisSize: MainAxisSize.min,
              children: [
                SizedBox(
                  width: 16,
                  height: 16,
                  child: CircularProgressIndicator(
                      strokeWidth: 2,
                      color: const Color(0xFF06B6D4)),
                ),
                const SizedBox(width: 8),
                Text('Réflexion...',
                    style: TextStyle(
                        color: Colors.white.withAlpha(120), fontSize: 13)),
              ],
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildCommandsPanel() {
    return Container(
      padding: const EdgeInsets.all(12),
      decoration: BoxDecoration(
        color: const Color(0xFF06B6D4).withAlpha(8),
        border: Border(
          bottom: BorderSide(color: const Color(0xFF06B6D4).withAlpha(20)),
        ),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              const Text('🎤 Commandes vocales',
                  style: TextStyle(
                      color: Color(0xFF06B6D4),
                      fontSize: 13,
                      fontWeight: FontWeight.bold)),
              const Spacer(),
              GestureDetector(
                onTap: () => setState(() => _showCommands = false),
                child: Icon(Icons.close,
                    color: Colors.white.withAlpha(80), size: 18),
              ),
            ],
          ),
          const SizedBox(height: 8),
          ..._quickCommands.map((cmd) => Padding(
                padding: const EdgeInsets.only(bottom: 4),
                child: GestureDetector(
                  onTap: () {
                    setState(() => _showCommands = false);
                    _processMessage(cmd);
                  },
                  child: Text(
                    '🗣️ "$cmd"',
                    style: TextStyle(
                        color: Colors.white.withAlpha(160), fontSize: 12),
                  ),
                ),
              )),
        ],
      ),
    );
  }

  Widget _buildInputBar() {
    return Container(
      padding: const EdgeInsets.all(12),
      decoration: BoxDecoration(
        color: const Color(0xFF111827),
        border: Border(
          top: BorderSide(color: Colors.white.withAlpha(10)),
        ),
      ),
      child: Row(
        children: [
          // Record button
          GestureDetector(
            onTap: _toggleRecording,
            child: AnimatedBuilder(
              animation: _pulseAnimation,
              builder: (ctx, child) {
                return Container(
                  width: 44,
                  height: 44,
                  decoration: BoxDecoration(
                    color: _isRecording
                        ? Colors.red
                        : Colors.white.withAlpha(10),
                    borderRadius: BorderRadius.circular(12),
                    boxShadow: _isRecording
                        ? [
                            BoxShadow(
                              color: Colors.red.withAlpha(
                                  (_pulseAnimation.value * 80).toInt()),
                              blurRadius: 12,
                            ),
                          ]
                        : null,
                  ),
                  child: Icon(
                    _isRecording ? Icons.mic_off : Icons.mic,
                    color: _isRecording ? Colors.white : Colors.white54,
                    size: 22,
                  ),
                );
              },
            ),
          ),
          const SizedBox(width: 8),

          // Text input
          Expanded(
            child: TextField(
              controller: _inputController,
              onSubmitted: _processMessage,
              style: const TextStyle(color: Colors.white, fontSize: 14),
              decoration: InputDecoration(
                hintText: 'Tapez ou dictez votre question...',
                hintStyle: TextStyle(color: Colors.white.withAlpha(60)),
                filled: true,
                fillColor: Colors.white.withAlpha(5),
                border: OutlineInputBorder(
                  borderRadius: BorderRadius.circular(12),
                  borderSide: BorderSide.none,
                ),
                contentPadding:
                    const EdgeInsets.symmetric(horizontal: 14, vertical: 10),
              ),
            ),
          ),
          const SizedBox(width: 8),

          // Send button
          GestureDetector(
            onTap: _isProcessing
                ? null
                : () => _processMessage(_inputController.text),
            child: Container(
              width: 44,
              height: 44,
              decoration: BoxDecoration(
                gradient: LinearGradient(
                  colors: _isProcessing
                      ? [Colors.grey.shade700, Colors.grey.shade600]
                      : [const Color(0xFF06B6D4), const Color(0xFF3B82F6)],
                ),
                borderRadius: BorderRadius.circular(12),
              ),
              child: const Icon(Icons.send, color: Colors.white, size: 20),
            ),
          ),
        ],
      ),
    );
  }
}

// ── Data models ──────────────────────────────────────

class _ChatMessage {
  final String role;
  final String content;
  final DateTime timestamp;
  final String? intent;
  final List<_Suggestion>? suggestions;

  _ChatMessage({
    required this.role,
    required this.content,
    required this.timestamp,
    this.intent,
    this.suggestions,
  });
}

class _Suggestion {
  final String command;
  final String icon;

  _Suggestion({required this.command, required this.icon});
}
