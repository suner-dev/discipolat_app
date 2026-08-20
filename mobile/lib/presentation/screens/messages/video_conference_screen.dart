import 'package:flutter/material.dart';
import 'package:url_launcher/url_launcher.dart';
import '../../../data/services/api_service.dart';

/// Video Conference screen — opens Jitsi Meet in browser via url_launcher.
/// For group pastoral meetings, follow-ups, and remote discipleship.
class VideoConferenceScreen extends StatefulWidget {
  final ApiService? apiService;
  const VideoConferenceScreen({super.key, this.apiService});

  @override
  State<VideoConferenceScreen> createState() => _VideoConferenceScreenState();
}

class _VideoConferenceScreenState extends State<VideoConferenceScreen> {
  late final ApiService _api = widget.apiService ?? ApiService();
  final _roomController = TextEditingController();
  final _nameController = TextEditingController();
  bool _isLoading = true;
  List<dynamic> _scheduledMeetings = [];

  @override
  void initState() {
    super.initState();
    _loadUserInfo();
    _loadMeetings();
  }

  @override
  void dispose() {
    _roomController.dispose();
    _nameController.dispose();
    super.dispose();
  }

  Future<void> _loadUserInfo() async {
    try {
      final res = await _api.get('/users/me');
      final data = res.data;
      if (data is Map) {
        _nameController.text = '${data['firstName'] ?? ''} ${data['lastName'] ?? ''}'.trim();
      }
    } catch (_) {}
    if (mounted) setState(() => _isLoading = false);
  }

  Future<void> _loadMeetings() async {
    try {
      final res = await _api.get('/events?type=MEETING&size=20');
      final data = res.data;
      List<dynamic> meetings = [];
      if (data is Map && data.containsKey('content')) {
        meetings = data['content'] as List<dynamic>;
      }
      if (mounted) setState(() => _scheduledMeetings = meetings);
    } catch (_) {}
  }

  Future<void> _joinMeeting(String roomName) async {
    final displayName = _nameController.text.isNotEmpty ? _nameController.text : 'Participant';
    final url = Uri.parse('https://meet.jit.si/$roomName#userInfo.displayName="$displayName"');

    if (await canLaunchUrl(url)) {
      await launchUrl(url, mode: LaunchMode.externalApplication);
    } else {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('Impossible d\'ouvrir la visioconférence'), backgroundColor: Colors.red),
        );
      }
    }
  }

  void _showJoinDialog() {
    _roomController.text = 'discipolat-${DateTime.now().millisecondsSinceEpoch % 10000}';
    showDialog(
      context: context,
      builder: (_) => AlertDialog(
        backgroundColor: const Color(0xFF1A1A3A),
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
        title: const Text('Rejoindre une visioconférence', style: TextStyle(color: Colors.white)),
        content: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            TextField(
              controller: _nameController,
              style: const TextStyle(color: Colors.white),
              decoration: InputDecoration(
                labelText: 'Votre nom',
                labelStyle: TextStyle(color: Colors.white.withAlpha(150)),
                filled: true,
                fillColor: Colors.white.withAlpha(10),
                border: OutlineInputBorder(borderRadius: BorderRadius.circular(8), borderSide: BorderSide.none),
              ),
            ),
            const SizedBox(height: 12),
            TextField(
              controller: _roomController,
              style: const TextStyle(color: Colors.white),
              decoration: InputDecoration(
                labelText: 'Nom de la salle',
                labelStyle: TextStyle(color: Colors.white.withAlpha(150)),
                filled: true,
                fillColor: Colors.white.withAlpha(10),
                border: OutlineInputBorder(borderRadius: BorderRadius.circular(8), borderSide: BorderSide.none),
              ),
            ),
          ],
        ),
        actions: [
          TextButton(onPressed: () => Navigator.pop(context), child: Text('Annuler', style: TextStyle(color: Colors.white.withAlpha(150)))),
          ElevatedButton(
            onPressed: () {
              Navigator.pop(context);
              _joinMeeting(_roomController.text);
            },
            style: ElevatedButton.styleFrom(backgroundColor: Colors.green, foregroundColor: Colors.white),
            child: const Text('Rejoindre'),
          ),
        ],
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: const Color(0xFF0A0A1A),
      appBar: AppBar(
        title: const Text('📹 Visioconférence', style: TextStyle(color: Colors.white, fontWeight: FontWeight.bold)),
        backgroundColor: Colors.transparent,
        elevation: 0,
      ),
      body: _isLoading
          ? const Center(child: CircularProgressIndicator(color: Colors.cyanAccent))
          : ListView(
              padding: const EdgeInsets.all(16),
              children: [
                _buildNewMeetingCard(),
                const SizedBox(height: 20),
                _buildQuickJoinCard(),
                const SizedBox(height: 20),
                _buildScheduledMeetings(),
              ],
            ),
    );
  }

  Widget _buildNewMeetingCard() {
    return Container(
      padding: const EdgeInsets.all(24),
      decoration: BoxDecoration(
        gradient: LinearGradient(
          colors: [Colors.green.withAlpha(40), Colors.cyanAccent.withAlpha(20)],
        ),
        borderRadius: BorderRadius.circular(20),
        border: Border.all(color: Colors.green.withAlpha(40)),
      ),
      child: Column(
        children: [
          const Icon(Icons.video_call, color: Colors.green, size: 48),
          const SizedBox(height: 16),
          const Text('Démarrer une réunion', style: TextStyle(color: Colors.white, fontSize: 18, fontWeight: FontWeight.bold)),
          const SizedBox(height: 8),
          Text('Créez un lien de visioconférence pour un suivi pastoral ou une réunion d\'équipe',
              style: TextStyle(color: Colors.white.withAlpha(150), fontSize: 13), textAlign: TextAlign.center),
          const SizedBox(height: 16),
          SizedBox(
            width: double.infinity,
            child: ElevatedButton.icon(
              onPressed: _showJoinDialog,
              icon: const Icon(Icons.play_arrow, size: 20),
              label: const Text('Démarrer maintenant'),
              style: ElevatedButton.styleFrom(
                backgroundColor: Colors.green,
                foregroundColor: Colors.white,
                padding: const EdgeInsets.symmetric(vertical: 14),
                shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
              ),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildQuickJoinCard() {
    return Container(
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: Colors.white.withAlpha(6),
        borderRadius: BorderRadius.circular(16),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const Text('Rejoindre rapidement', style: TextStyle(color: Colors.white, fontSize: 16, fontWeight: FontWeight.bold)),
          const SizedBox(height: 12),
          Row(
            children: [
              Expanded(
                child: TextField(
                  controller: _roomController,
                  style: const TextStyle(color: Colors.white),
                  decoration: InputDecoration(
                    hintText: 'Nom de la salle...',
                    hintStyle: TextStyle(color: Colors.white.withAlpha(80)),
                    filled: true,
                    fillColor: Colors.white.withAlpha(10),
                    border: OutlineInputBorder(borderRadius: BorderRadius.circular(8), borderSide: BorderSide.none),
                    contentPadding: const EdgeInsets.symmetric(horizontal: 12, vertical: 10),
                  ),
                ),
              ),
              const SizedBox(width: 12),
              ElevatedButton(
                onPressed: () {
                  if (_roomController.text.isNotEmpty) {
                    _joinMeeting(_roomController.text);
                  }
                },
                style: ElevatedButton.styleFrom(
                  backgroundColor: Colors.cyanAccent,
                  foregroundColor: Colors.black,
                  padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 14),
                  shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(8)),
                ),
                child: const Text('Rejoindre'),
              ),
            ],
          ),
        ],
      ),
    );
  }

  Widget _buildScheduledMeetings() {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        const Text('Réunions à venir', style: TextStyle(color: Colors.white, fontSize: 18, fontWeight: FontWeight.bold)),
        const SizedBox(height: 12),
        if (_scheduledMeetings.isEmpty)
          Container(
            padding: const EdgeInsets.all(32),
            decoration: BoxDecoration(
              color: Colors.white.withAlpha(6),
              borderRadius: BorderRadius.circular(16),
            ),
            child: Center(
              child: Column(
                children: [
                  Icon(Icons.event_busy, color: Colors.white.withAlpha(50), size: 48),
                  const SizedBox(height: 12),
                  Text('Aucune réunion planifiée', style: TextStyle(color: Colors.white.withAlpha(120), fontSize: 14)),
                ],
              ),
            ),
          )
        else
          ...List.generate(_scheduledMeetings.length, (i) {
            final meeting = _scheduledMeetings[i];
            return _buildMeetingItem(meeting);
          }),
      ],
    );
  }

  Widget _buildMeetingItem(Map<String, dynamic> meeting) {
    final title = meeting['title']?.toString() ?? 'Réunion';
    final dateStr = meeting['startDate']?.toString() ?? '';

    return Container(
      margin: const EdgeInsets.only(bottom: 8),
      padding: const EdgeInsets.all(14),
      decoration: BoxDecoration(
        color: Colors.white.withAlpha(6),
        borderRadius: BorderRadius.circular(12),
      ),
      child: ListTile(
        contentPadding: EdgeInsets.zero,
        leading: Container(
          padding: const EdgeInsets.all(10),
          decoration: BoxDecoration(
            color: Colors.cyanAccent.withAlpha(20),
            borderRadius: BorderRadius.circular(10),
          ),
          child: const Icon(Icons.video_call, color: Colors.cyanAccent, size: 24),
        ),
        title: Text(title, style: const TextStyle(color: Colors.white, fontSize: 14, fontWeight: FontWeight.w500)),
        subtitle: dateStr.isNotEmpty
            ? Text(dateStr.substring(0, dateStr.length > 16 ? 16 : dateStr.length),
                style: TextStyle(color: Colors.white.withAlpha(120), fontSize: 12))
            : null,
        trailing: ElevatedButton(
          onPressed: () => _joinMeeting('meeting-${meeting['id']}'),
          style: ElevatedButton.styleFrom(
            backgroundColor: Colors.green,
            foregroundColor: Colors.white,
            padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 6),
            shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(8)),
          ),
          child: const Text('Rejoindre', style: TextStyle(fontSize: 12)),
        ),
      ),
    );
  }
}
