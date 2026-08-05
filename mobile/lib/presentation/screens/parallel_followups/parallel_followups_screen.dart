import 'package:flutter/material.dart';
import '../../widgets/glass_theme.dart';
import '../../widgets/app_drawer.dart';
import '../../../data/services/api_service.dart';

class ParallelFollowupsScreen extends StatefulWidget {
  const ParallelFollowupsScreen({super.key});

  @override
  State<ParallelFollowupsScreen> createState() => _ParallelFollowupsScreenState();
}

class _ParallelFollowupsScreenState extends State<ParallelFollowupsScreen> with SingleTickerProviderStateMixin {
  final _apiService = ApiService();
  late TabController _tabController;
  List<dynamic> _all = [];
  List<dynamic> _active = [];
  bool _isLoading = true;

  @override
  void initState() {
    super.initState();
    _tabController = TabController(length: 2, vsync: this);
    _loadData();
  }

  @override
  void dispose() {
    _tabController.dispose();
    super.dispose();
  }

  Future<void> _loadData() async {
    setState(() => _isLoading = true);
    try {
      final allRes = await _apiService.get('/parallel-followups', params: {'size': '50'});
      final activeRes = await _apiService.get('/parallel-followups/active');
      if (mounted) {
        setState(() {
          _all = (allRes.data is Map ? allRes.data['content'] : allRes.data) as List<dynamic>? ?? [];
          _active = (activeRes.data is Map ? activeRes.data['content'] : activeRes.data) as List<dynamic>? ?? [];
          _isLoading = false;
        });
      }
    } catch (_) {
      if (mounted) setState(() => _isLoading = false);
    }
  }

  Color _statusColor(String? s) {
    switch (s) {
      case 'ACTIF': return Colors.green;
      case 'EN_COURS': return Colors.amber;
      case 'CLOTURE': return Colors.grey;
      default: return Colors.blue;
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Suivis parallèles'),
        bottom: TabBar(
          controller: _tabController,
          indicatorColor: Colors.white,
          tabs: const [
            Tab(text: 'Actifs'),
            Tab(text: 'Tous'),
          ],
        ),
      ),
      drawer: const AppDrawer(),
      body: _isLoading
          ? const ShimmerLoading(itemCount: 3)
          : RefreshIndicator(
              onRefresh: _loadData,
              child: TabBarView(
                controller: _tabController,
                children: [
                  _buildList(_active),
                  _buildList(_all),
                ],
              ),
            ),
    );
  }

  Widget _buildList(List<dynamic> items) {
    if (items.isEmpty) {
      return Center(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Icon(Icons.track_changes_outlined, size: 48, color: Colors.white.withValues(alpha: 0.3)),
            const SizedBox(height: 12),
            Text('Aucun suivi', style: TextStyle(color: Colors.white.withValues(alpha: 0.5))),
          ],
        ),
      );
    }
    return ListView.builder(
      padding: const EdgeInsets.all(12),
      itemCount: items.length,
      itemBuilder: (context, index) {
        final f = items[index] as Map<String, dynamic>;
        final titre = f['titre'] ?? f['nom'] ?? 'Suivi';
        final statut = f['statut'] ?? 'ACTIF';
        final dateDebut = f['dateDebut'] ?? f['createdAt'] ?? '';
        final description = f['description'] ?? f['motif'] ?? '';
        final responsable = f['responsableNom'] ?? f['faiseurNom'] ?? '—';
        return GlassCard(
          padding: const EdgeInsets.all(14),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Row(
                children: [
                  Container(
                    padding: const EdgeInsets.all(8),
                    decoration: BoxDecoration(
                      color: _statusColor(statut).withValues(alpha: 0.15),
                      borderRadius: BorderRadius.circular(10),
                    ),
                    child: Icon(Icons.track_changes, color: _statusColor(statut), size: 20),
                  ),
                  const SizedBox(width: 10),
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(titre, style: const TextStyle(color: Colors.white, fontWeight: FontWeight.w600, fontSize: 14)),
                        Text('Responsable: $responsable', style: TextStyle(color: Colors.white.withValues(alpha: 0.5), fontSize: 11)),
                      ],
                    ),
                  ),
                  Container(
                    padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 3),
                    decoration: BoxDecoration(
                      color: _statusColor(statut).withValues(alpha: 0.15),
                      borderRadius: BorderRadius.circular(12),
                    ),
                    child: Text(statut, style: TextStyle(color: _statusColor(statut), fontSize: 10, fontWeight: FontWeight.w600)),
                  ),
                ],
              ),
              if (description.toString().isNotEmpty) ...[
                const SizedBox(height: 8),
                Text(
                  description,
                  style: TextStyle(color: Colors.white.withValues(alpha: 0.6), fontSize: 12),
                  maxLines: 2,
                  overflow: TextOverflow.ellipsis,
                ),
              ],
              if (dateDebut.toString().isNotEmpty) ...[
                const SizedBox(height: 6),
                Row(
                  children: [
                    Icon(Icons.access_time, size: 12, color: Colors.white.withValues(alpha: 0.4)),
                    const SizedBox(width: 4),
                    Text(dateDebut.toString().substring(0, 10), style: TextStyle(color: Colors.white.withValues(alpha: 0.4), fontSize: 11)),
                  ],
                ),
              ],
            ],
          ),
        );
      },
    );
  }
}
