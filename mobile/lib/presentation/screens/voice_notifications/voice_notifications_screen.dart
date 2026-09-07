import 'package:flutter/material.dart';
import '../../../core/tts/tts_service.dart';
import '../../../core/network/websocket/stomp_service.dart';
import '../../../data/services/auth_service.dart';

class VoiceNotificationsScreen extends StatefulWidget {
  const VoiceNotificationsScreen({super.key});

  @override
  State<VoiceNotificationsScreen> createState() => _VoiceNotificationsScreenState();
}

class _VoiceNotificationsScreenState extends State<VoiceNotificationsScreen> {
  final TtsService _ttsService = TtsService();
  StompService? _stompService;
  final List<VoiceNotificationDTO> _notifications = [];
  bool _isConnected = false;
  bool _isVoiceEnabled = false;

  @override
  void initState() {
    super.initState();
    _initializeServices();
  }

  Future<void> _initializeServices() async {
    await _ttsService.initialize();
    final authService = AuthService();
    final user = authService.currentUser;
    final token = authService.token;

    if (user != null && token != null) {
      _stompService = StompService(
        token: token,
        userId: user.id,
        activeRole: user.activeRole,
      );
      _stompService!.addCallback(_onNotificationReceived);
      _stompService!.connect();
    }

    if (mounted) {
      setState(() {
        _isConnected = _stompService?.isConnected ?? false;
        _isVoiceEnabled = true;
      });
    }
  }

  void _onNotificationReceived(VoiceNotificationDTO notification) {
    if (!mounted) return;
    setState(() {
      _notifications.insert(0, notification);
      if (_notifications.length > 50) _notifications.removeLast();
    });

    if (_isVoiceEnabled) {
      switch (notification.priority) {
        case 'URGENT':
          _ttsService.stop();
          _ttsService.speakPriority(notification.speakText);
          break;
        case 'HIGH':
          _ttsService.speakPriority(notification.speakText);
          break;
        case 'NORMAL':
          _ttsService.speak(notification.speakText);
          break;
        case 'LOW':
          break;
      }
    }
  }

  Future<void> _enableVoice() async {
    await _ttsService.initialize();
    if (mounted) setState(() => _isVoiceEnabled = true);
  }

  Future<void> _stopSpeaking() async {
    await _ttsService.stop();
  }

  void _clearNotifications() {
    if (mounted) setState(() => _notifications.clear());
  }

  Future<void> _speakLastNotification() async {
    if (_notifications.isNotEmpty) {

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Notifications Vocales'),
        backgroundColor: Colors.transparent,
        elevation: 0,
        actions: [
          IconButton(
            icon: Icon(
              _isConnected ? Icons.wifi : Icons.wifi_off,
              color: _isConnected ? Colors.green : Colors.red,
            ),
            onPressed: null,
          ),
          IconButton(
            icon: const Icon(Icons.delete_outline),
            onPressed: _clearNotifications,
          ),
        ],
      ),
      body: Column(
        children: [
          Container(
            padding: const EdgeInsets.all(16),
            child: Row(
              children: [
                Expanded(
                  child: ElevatedButton.icon(
                    onPressed: _isVoiceEnabled ? _stopSpeaking : _enableVoice,
                    icon: Icon(_isVoiceEnabled ? Icons.volume_off : Icons.volume_up),
                    label: Text(_isVoiceEnabled ? 'Arrêter' : 'Activer la voix'),
                    style: ElevatedButton.styleFrom(
                      backgroundColor: _isVoiceEnabled ? Colors.red : Colors.indigo,
                      foregroundColor: Colors.white,
                    ),
                  ),
                ),
                const SizedBox(width: 8),
                IconButton(
                  onPressed: _speakLastNotification,
                  icon: const Icon(Icons.replay),
                  tooltip: 'Relire la dernière',
                ),
              ],
            ),
          ),
          Padding(
            padding: const EdgeInsets.symmetric(horizontal: 16),
            child: Row(
              children: [
                Icon(
                  _isConnected ? Icons.check_circle : Icons.error,
                  size: 16,
                  color: _isConnected ? Colors.green : Colors.red,
                ),
                const SizedBox(width: 8),
                Text(
                  _isConnected ? 'Connecté au serveur' : 'Déconnecté',
                  style: TextStyle(
                    color: _isConnected ? Colors.green : Colors.red,
                    fontSize: 12,
                  ),
                ),
                const Spacer(),
                Text(
                  '${_notifications.length} notification${_notifications.length != 1 ? 's' : ''}',
                  style: const TextStyle(color: Colors.grey, fontSize: 12),
                ),
              ],
            ),
          ),
          const Divider(),
          Expanded(
            child: _notifications.isEmpty
                ? Center(
                    child: Column(
                      mainAxisAlignment: MainAxisAlignment.center,
                      children: [
                        Icon(Icons.notifications_off, size: 48, color: Colors.grey.withAlpha(80)),
                        const SizedBox(height: 16),
                        const Text('Aucune notification', style: TextStyle(color: Colors.grey)),
                      ],
                    ),
                  )
                : ListView.builder(
                    itemCount: _notifications.length,
                    itemBuilder: (context, index) {
                      final notification = _notifications[index];
                      return Card(
                        margin: const EdgeInsets.symmetric(horizontal: 16, vertical: 4),
                        child: ListTile(
                          leading: CircleAvatar(
                            backgroundColor: _getPriorityColor(notification.priority),
                            child: Icon(_getPriorityIcon(notification.priority), color: Colors.white, size: 20),
                          ),
                          title: Text(notification.title, style: const TextStyle(fontWeight: FontWeight.w600)),
                          subtitle: Text(notification.body, maxLines: 2, overflow: TextOverflow.ellipsis),
                          trailing: Text(_formatTime(notification.timestamp), style: const TextStyle(color: Colors.grey, fontSize: 11)),
                          onTap: () => _ttsService.speak(notification.speakText),
                        ),
                      );
                    },
                  ),
          ),
        ],
      ),
    );
  }

  Color _getPriorityColor(String priority) {
    switch (priority) {
      case 'URGENT': return Colors.red;
      case 'HIGH': return Colors.orange;
      case 'NORMAL': return Colors.blue;
      default: return Colors.grey;
    }
  }

  IconData _getPriorityIcon(String priority) {
    switch (priority) {
      case 'URGENT': return Icons.priority_high;
      case 'HIGH': return Icons.arrow_upward;
      case 'NORMAL': return Icons.remove;
      default: return Icons.arrow_downward;
    }
  }

  String _formatTime(String timestamp) {
    try {
      final date = DateTime.parse(timestamp);
      return '${date.hour}:${date.minute.toString().padLeft(2, '0')}';
    } catch (_) {
      return '';
    }
  }
}

      await _ttsService.speak(_notifications.first.speakText);
    }
  }

  @override
  void dispose() {
    _stompService?.disconnect();
    _ttsService.dispose();
    super.dispose();
  }
}
