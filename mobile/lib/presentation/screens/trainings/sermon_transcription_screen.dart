import 'package:flutter/material.dart';
import '../../../data/services/api_service.dart';

/// Sermon Transcription screen — browse, search, and view transcribed sermons.
class SermonTranscriptionScreen extends StatefulWidget {
  final ApiService? apiService;
  const SermonTranscriptionScreen({super.key, this.apiService});

  @override
  State<SermonTranscriptionScreen> createState() => _SermonTranscriptionScreenState();
}

class _SermonTranscriptionScreenState extends State<SermonTranscriptionScreen> {
  late final ApiService _api = widget.apiService ?? ApiService();
  List<dynamic> _sermons = [];
  bool _isLoading = true;
  String _search = '';

  @override
  void initState() {
    super.initState();
    _loadSermons();
  }

  Future<void> _loadSermons() async {
    setState(() => _isLoading = true);
    try {
      final params = _search.isNotEmpty ? '?q=$_search&size=50' : '?size=50';
      final res = await _api.get('/sermons$params');
      final data = res.data;
      List<dynamic> sermons = [];
      if (data is Map && data.containsKey('content')) {
        sermons = data['content'] as List<dynamic>;
      } else if (data is List) {
        sermons = data;
      }
      if (mounted) setState(() { _sermons = sermons; _isLoading = false; });
    } catch (e) {
      if (mounted) setState(() { _sermons = []; _isLoading = false; });
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: const Color(0xFF0A0A1A),
      appBar: AppBar(
        title: const Text('🎙️ Transcriptions', style: TextStyle(color: Colors.white, fontWeight: FontWeight.bold)),
        backgroundColor: Colors.transparent,
        elevation: 0,
        actions: [
          IconButton(
            icon: const Icon(Icons.refresh, color: Colors.white),
            onPressed: _loadSermons,
          ),
        ],
      ),
      body: Column(
        children: [
          _buildSearchBar(),
          Expanded(
            child: _isLoading
                ? const Center(child: CircularProgressIndicator(color: Colors.cyanAccent))
                : _sermons.isEmpty
                    ? _buildEmptyState()
                    : RefreshIndicator(
                        onRefresh: _loadSermons,
                        child: ListView.builder(
                          padding: const EdgeInsets.all(16),
                          itemCount: _sermons.length,
                          itemBuilder: (_, i) => _buildSermonCard(_sermons[i]),
                        ),
                      ),
          ),
        ],
      ),
    );
  }

  Widget _buildSearchBar() {
    return Padding(
      padding: const EdgeInsets.all(16),
      child: TextField(
        style: const TextStyle(color: Colors.white),
        decoration: InputDecoration(
          hintText: 'Rechercher un sermon...',
          hintStyle: TextStyle(color: Colors.white.withAlpha(100)),
          prefixIcon: Icon(Icons.search, color: Colors.white.withAlpha(100)),
          filled: true,
          fillColor: Colors.white.withAlpha(10),
          border: OutlineInputBorder(
            borderRadius: BorderRadius.circular(12),
            borderSide: BorderSide.none,
          ),
          contentPadding: const EdgeInsets.symmetric(vertical: 0, horizontal: 16),
        ),
        onChanged: (v) {
          _search = v;
          _loadSermons();
        },
      ),
    );
  }

  Widget _buildEmptyState() {
    return Center(
      child: Column(
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          Icon(Icons.mic, color: Colors.white.withAlpha(50), size: 64),
          const SizedBox(height: 16),
          Text('Aucune transcription', style: TextStyle(color: Colors.white.withAlpha(150), fontSize: 16)),
          const SizedBox(height: 8),
          Text('Les sermons enregistrés apparaîtront ici', style: TextStyle(color: Colors.white.withAlpha(100), fontSize: 12)),
        ],
      ),
    );
  }

  Widget _buildSermonCard(dynamic sermon) {
    final title = sermon['title']?.toString() ?? 'Sans titre';
    final speaker = sermon['speaker']?.toString() ?? '';
    final theme = sermon['theme']?.toString() ?? '';
    final dateStr = sermon['recordedAt']?.toString() ?? '';
    final status = sermon['transcriptionStatus']?.toString() ?? 'PENDING';
    final duration = sermon['durationSeconds'] as int? ?? 0;
    Color statusColor;
    String statusLabel;
    switch (status) {
      case 'COMPLETED':
        statusColor = Colors.green;
        statusLabel = 'Terminé';
        break;
      case 'PROCESSING':
        statusColor = Colors.amber;
        statusLabel = 'En cours';
        break;
      case 'FAILED':
        statusColor = Colors.red;
        statusLabel = 'Échoué';
        break;
      default:
        statusColor = Colors.white.withAlpha(100);
        statusLabel = 'En attente';
    }

    final durationMin = duration > 0 ? '${(duration / 60).toInt()} min' : '';

    return GestureDetector(
      onTap: () => _showSermonDetail(sermon),
      child: Container(
        margin: const EdgeInsets.only(bottom: 12),
        padding: const EdgeInsets.all(16),
        decoration: BoxDecoration(
          color: Colors.white.withAlpha(6),
          borderRadius: BorderRadius.circular(16),
          border: Border.all(color: Colors.white.withAlpha(10)),
        ),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                Expanded(
                  child: Text(title, style: const TextStyle(color: Colors.white, fontSize: 16, fontWeight: FontWeight.bold)),
                ),
                Container(
                  padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 3),
                  decoration: BoxDecoration(
                    color: statusColor.withAlpha(30),
                    borderRadius: BorderRadius.circular(8),
                  ),
                  child: Text(statusLabel, style: TextStyle(color: statusColor, fontSize: 10, fontWeight: FontWeight.bold)),
                ),
              ],
            ),
            if (speaker.isNotEmpty) ...[
              const SizedBox(height: 6),
              Row(
                children: [
                  Icon(Icons.person, color: Colors.cyanAccent.withAlpha(150), size: 14),
                  const SizedBox(width: 6),
                  Text(speaker, style: TextStyle(color: Colors.cyanAccent.withAlpha(180), fontSize: 13)),
                  if (durationMin.isNotEmpty) ...[
                    const SizedBox(width: 12),
                    Icon(Icons.timer, color: Colors.white.withAlpha(100), size: 14),
                    const SizedBox(width: 4),
                    Text(durationMin, style: TextStyle(color: Colors.white.withAlpha(100), fontSize: 12)),
                  ],
                ],
              ),
            ],
            if (theme.isNotEmpty) ...[
              const SizedBox(height: 4),
              Container(
                padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 3),
                decoration: BoxDecoration(
                  color: Colors.purple.withAlpha(30),
                  borderRadius: BorderRadius.circular(6),
                ),
                child: Text(theme, style: TextStyle(color: Colors.purpleAccent.withAlpha(200), fontSize: 11)),
              ),
            ],
            if (dateStr.isNotEmpty) ...[
              const SizedBox(height: 6),
              Text(dateStr.substring(0, dateStr.length > 10 ? 10 : dateStr.length),
                  style: TextStyle(color: Colors.white.withAlpha(80), fontSize: 11)),
            ],
          ],
        ),
      ),
    );
  }

  void _showSermonDetail(dynamic sermon) {
    final title = sermon['title']?.toString() ?? '';
    final speaker = sermon['speaker']?.toString() ?? '';
    final fullText = sermon['fullText']?.toString() ?? '';
    final summary = sermon['summary']?.toString() ?? '';
    final keyVerses = sermon['keyVerses']?.toString() ?? '';

    showModalBottomSheet(
      context: context,
      isScrollControlled: true,
      backgroundColor: const Color(0xFF12122A),
      shape: const RoundedRectangleBorder(borderRadius: BorderRadius.vertical(top: Radius.circular(20))),
      builder: (_) => DraggableScrollableSheet(
        initialChildSize: 0.85,
        maxChildSize: 0.95,
        minChildSize: 0.5,
        expand: false,
        builder: (_, scrollController) => ListView(
          controller: scrollController,
          padding: const EdgeInsets.all(20),
          children: [
            Center(
              child: Container(width: 40, height: 4, decoration: BoxDecoration(color: Colors.white.withAlpha(40), borderRadius: BorderRadius.circular(2))),
            ),
            const SizedBox(height: 20),
            Text(title, style: const TextStyle(color: Colors.white, fontSize: 20, fontWeight: FontWeight.bold)),
            const SizedBox(height: 8),
            if (speaker.isNotEmpty)
              Text(speaker, style: TextStyle(color: Colors.cyanAccent.withAlpha(200), fontSize: 14)),
            if (summary.isNotEmpty) ...[
              const SizedBox(height: 20),
              const Text('Résumé', style: TextStyle(color: Colors.amber, fontSize: 16, fontWeight: FontWeight.bold)),
              const SizedBox(height: 8),
              Text(summary, style: TextStyle(color: Colors.white.withAlpha(200), fontSize: 14, height: 1.5)),
            ],
            if (keyVerses.isNotEmpty) ...[
              const SizedBox(height: 20),
              const Text('Versets clés', style: TextStyle(color: Colors.green, fontSize: 16, fontWeight: FontWeight.bold)),
              const SizedBox(height: 8),
              Text(keyVerses, style: TextStyle(color: Colors.white.withAlpha(200), fontSize: 14, height: 1.5)),
            ],
            if (fullText.isNotEmpty) ...[
              const SizedBox(height: 20),
              const Text('Texte complet', style: TextStyle(color: Colors.cyanAccent, fontSize: 16, fontWeight: FontWeight.bold)),
              const SizedBox(height: 8),
              Text(fullText, style: TextStyle(color: Colors.white.withAlpha(180), fontSize: 13, height: 1.6)),
            ],
          ],
        ),
      ),
    );
  }
}
