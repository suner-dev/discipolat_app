import 'package:flutter/material.dart';
import '../../../data/services/api_service.dart';
import '../../widgets/glass_theme.dart';
import '../../widgets/app_drawer.dart';

/// Parcours Spirituel — progression visuelle du disciple à travers les étapes.
/// Connecté à l'API Backend : /api/v1/spiritual-journey
class SpiritualJourneyScreen extends StatefulWidget {
  const SpiritualJourneyScreen({super.key});

  @override
  State<SpiritualJourneyScreen> createState() => _SpiritualJourneyScreenState();
}

class _SpiritualJourneyScreenState extends State<SpiritualJourneyScreen> {
  final _apiService = ApiService();
  List<dynamic> _stages = [];
  bool _isLoading = true;

  @override
  void initState() {
    super.initState();
    _load();
  }

  Future<void> _load() async {
    setState(() => _isLoading = true);
    try {
      final res = await _apiService.get('/spiritual-journey');
      if (mounted) {
        setState(() {
          _stages = (res.data is List ? res.data : []) as List<dynamic>;
          _isLoading = false;
        });
      }
    } catch (e) {
      debugPrint('Error loading spiritual journey: $e');
      if (mounted) setState(() => _isLoading = false);
    }
  }

  @override
  void setState(VoidCallback fn) {
    if (mounted) super.setState(fn);
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: const Color(0xFF030712),
      appBar: AppBar(
          backgroundColor: Colors.transparent,
          title: const Text('Parcours Spirituel')),
      drawer: const AppDrawer(),
      body: _isLoading
          ? const Center(
              child: CircularProgressIndicator(color: Color(0xFFFFB300)))
          : RefreshIndicator(onRefresh: _load, child: _buildStages()),
    );
  }

  Widget _buildStages() {
    if (_stages.isEmpty) {
      return ListView(padding: const EdgeInsets.all(24), children: const [
        SizedBox(height: 100),
        Center(
            child: Column(children: [
          Icon(Icons.account_tree, size: 64, color: Colors.white24),
          SizedBox(height: 16),
          Text('Aucune étape pour le moment',
              style: TextStyle(color: Colors.white70, fontSize: 16)),
        ])),
      ]);
    }
    return ListView.builder(
      padding: const EdgeInsets.all(16),
      itemCount: _stages.length,
      itemBuilder: (context, i) {
        final stage = _stages[i] as Map<String, dynamic>;
        final completed = stage['completed'] == true;
        final order = stage['order'] ?? (i + 1);
        final progress = (_stages.where((s) => s['completed'] == true).length /
                _stages.length)
            .clamp(0.0, 1.0);
        return Card(
          color: const Color(0xFF111827),
          elevation: 4,
          shape:
              RoundedRectangleBorder(borderRadius: BorderRadius.circular(14)),
          child: Padding(
            padding: const EdgeInsets.all(16),
            child: Row(crossAxisAlignment: CrossAxisAlignment.start, children: [
              CircleAvatar(
                backgroundColor: completed
                    ? AppColors.primary
                    : Colors.white.withValues(alpha: 0.1),
                radius: 20,
                child: completed
                    ? const Icon(Icons.check_rounded,
                        color: Colors.white, size: 22)
                    : null,
              ),
              const SizedBox(width: 12),
              Expanded(
                child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Row(children: [
                        Text('Étape $order',
                            style:
                                TextStyle(color: Colors.white54, fontSize: 12)),
                        const SizedBox(width: 8),
                        if (completed)
                          const Icon(Icons.verified_rounded,
                              size: 14, color: Color(0xFF4CAF50)),
                      ]),
                      Text(stage['name'] ?? '',
                          style: TextStyle(
                              color: Colors.white,
                              fontWeight: FontWeight.w600,
                              fontSize: 15)),
                      const SizedBox(height: 4),
                      Text(stage['description'] ?? '',
                          style: TextStyle(
                              color: Colors.white54,
                              fontSize: 12,
                              height: 1.4)),
                      const SizedBox(height: 8),
                      ClipRRect(
                        borderRadius: BorderRadius.circular(4),
                        child: LinearProgressIndicator(
                            value: progress,
                            minHeight: 4,
                            color: AppColors.primary,
                            backgroundColor:
                                Colors.white.withValues(alpha: 0.1)),
                      ),
                    ]),
              ),
            ]),
          ),
        );
      },
    );
  }
}
