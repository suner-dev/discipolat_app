import 'package:flutter/material.dart';
import '../../widgets/glass_theme.dart';
import '../../widgets/app_drawer.dart';
import '../../../data/services/api_service.dart';

class AppointmentsScreen extends StatefulWidget {
  const AppointmentsScreen({super.key});

  @override
  State<AppointmentsScreen> createState() => _AppointmentsScreenState();
}

class _AppointmentsScreenState extends State<AppointmentsScreen>
    with SingleTickerProviderStateMixin {
  final _apiService = ApiService();
  late TabController _tabController;
  List<dynamic> _myRequests = [];
  List<dynamic> _inbox = [];
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
      final myRes = await _apiService.get('/appointments/my');
      final inboxRes = await _apiService.get('/appointments/inbox');
      if (mounted) {
        setState(() {
          _myRequests = myRes.data as List<dynamic>? ?? [];
          _inbox = inboxRes.data as List<dynamic>? ?? [];
          _isLoading = false;
        });
      }
    } catch (_) {
      if (mounted) setState(() => _isLoading = false);
    }
  }

  Future<void> _updateStatus(String id, String status, {String? reponse}) async {
    try {
      await _apiService.patch('/appointments/$id/status', data: {
        'statut': status,
        if (reponse != null) 'reponse': reponse,
      });
      _loadData();
    } catch (_) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('Erreur lors de la mise à jour')),
        );
      }
    }
  }

  Color _statusColor(String statut) {
    switch (statut) {
      case 'CONFIRME': return Colors.green;
      case 'REFUSE': return Colors.red;
      case 'ANNULE': return Colors.orange;
      case 'TERMINE': return Colors.blue;
      default: return Colors.amber;
    }
  }

  IconData _motifIcon(String? motif) {
    switch (motif) {
      case 'CONSEIL': return Icons.psychology;
      case 'SUIVI': return Icons.track_changes;
      case 'FORMATION': return Icons.school;
      case 'DISCIPLINE': return Icons.gavel;
      default: return Icons.event;
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Rendez-vous'),
        bottom: TabBar(
          controller: _tabController,
          indicatorColor: Colors.white,
          tabs: const [
            Tab(text: 'Mes demandes'),
            Tab(text: 'Reçues'),
          ],
        ),
      ),
      drawer: const AppDrawer(),
      body: _isLoading
          ? const ShimmerLoading(itemCount: 4)
          : RefreshIndicator(
              onRefresh: _loadData,
              child: TabBarView(
                controller: _tabController,
                children: [
                  _buildList(_myRequests, isOutbox: true),
                  _buildList(_inbox, isOutbox: false),
                ],
              ),
            ),
    );
  }

  Widget _buildList(List<dynamic> items, {required bool isOutbox}) {
    if (items.isEmpty) {
      return Center(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Icon(Icons.event_available, size: 48, color: Colors.white.withValues(alpha: 0.3)),
            const SizedBox(height: 12),
            Text(
              isOutbox ? 'Aucune demande envoyée' : 'Aucune demande reçue',
              style: TextStyle(color: Colors.white.withValues(alpha: 0.5)),
            ),
          ],
        ),
      );
    }
    return ListView.builder(
      padding: const EdgeInsets.all(12),
      itemCount: items.length,
      itemBuilder: (context, index) {
        final a = items[index] as Map<String, dynamic>;
        final statut = a['statut'] ?? 'EN_ATTENTE';
        final isConfirme = statut == 'CONFIRME';
        final isPending = statut == 'EN_ATTENTE';
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
                    child: Icon(_motifIcon(a['motif']), color: _statusColor(statut), size: 20),
                  ),
                  const SizedBox(width: 10),
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(
                          isOutbox
                              ? 'Avec: ${a['recepteurNom'] ?? '—'}'
                              : 'De: ${a['demandeurNom'] ?? '—'}',
                          style: const TextStyle(color: Colors.white, fontWeight: FontWeight.w600, fontSize: 14),
                        ),
                        Text(
                          a['objet'] ?? '',
                          style: TextStyle(color: Colors.white.withValues(alpha: 0.6), fontSize: 12),
                          maxLines: 1,
                          overflow: TextOverflow.ellipsis,
                        ),
                      ],
                    ),
                  ),
                  Container(
                    padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 3),
                    decoration: BoxDecoration(
                      color: _statusColor(statut).withValues(alpha: 0.15),
                      borderRadius: BorderRadius.circular(12),
                    ),
                    child: Text(
                      statut.toLowerCase().replaceAll('_', ' '),
                      style: TextStyle(color: _statusColor(statut), fontSize: 10, fontWeight: FontWeight.w600),
                    ),
                  ),
                ],
              ),
              if (a['datePrevue'] != null) ...[
                const SizedBox(height: 10),
                Row(
                  children: [
                    Icon(Icons.access_time, size: 14, color: Colors.white.withValues(alpha: 0.4)),
                    const SizedBox(width: 4),
                    Text(
                      '${a['datePrevue'].toString().substring(0, 16).replaceAll('T', ' ')} (${a['dureeMinutes'] ?? 30} min)',
                      style: TextStyle(color: Colors.white.withValues(alpha: 0.5), fontSize: 12),
                    ),
                  ],
                ),
              ],
              if (a['reponse'] != null && (a['reponse'] as String).isNotEmpty) ...[
                const SizedBox(height: 8),
                Container(
                  padding: const EdgeInsets.all(8),
                  decoration: BoxDecoration(
                    color: Colors.white.withValues(alpha: 0.04),
                    borderRadius: BorderRadius.circular(8),
                  ),
                  child: Row(
                    children: [
                      Icon(Icons.reply, size: 14, color: Colors.white.withValues(alpha: 0.4)),
                      const SizedBox(width: 6),
                      Expanded(
                        child: Text(
                          a['reponse'],
                          style: TextStyle(color: Colors.white.withValues(alpha: 0.6), fontSize: 12),
                        ),
                      ),
                    ],
                  ),
                ),
              ],
              if (!isOutbox && isPending) ...[
                const SizedBox(height: 10),
                Row(
                  children: [
                    Expanded(
                      child: OutlinedButton.icon(
                        onPressed: () => _updateStatus(a['id'], 'REFUSE'),
                        icon: const Icon(Icons.close, size: 16),
                        label: const Text('Refuser', style: TextStyle(fontSize: 12)),
                        style: OutlinedButton.styleFrom(
                          foregroundColor: Colors.red,
                          side: BorderSide(color: Colors.red.withValues(alpha: 0.3)),
                          shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(10)),
                        ),
                      ),
                    ),
                    const SizedBox(width: 8),
                    Expanded(
                      child: ElevatedButton.icon(
                        onPressed: () => _updateStatus(a['id'], 'CONFIRME'),
                        icon: const Icon(Icons.check, size: 16),
                        label: const Text('Confirmer', style: TextStyle(fontSize: 12)),
                        style: ElevatedButton.styleFrom(
                          backgroundColor: Colors.green,
                          foregroundColor: Colors.white,
                          shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(10)),
                        ),
                      ),
                    ),
                  ],
                ),
              ],
              if (!isOutbox && isConfirme) ...[
                const SizedBox(height: 10),
                SizedBox(
                  width: double.infinity,
                  child: OutlinedButton.icon(
                    onPressed: () => _updateStatus(a['id'], 'TERMINE', reponse: 'RDV terminé'),
                    icon: const Icon(Icons.check_circle, size: 16),
                    label: const Text('Marquer terminé', style: TextStyle(fontSize: 12)),
                    style: OutlinedButton.styleFrom(
                      foregroundColor: Colors.blue,
                      side: BorderSide(color: Colors.blue.withValues(alpha: 0.3)),
                      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(10)),
                    ),
                  ),
                ),
              ],
            ],
          ),
        );
      },
    );
  }
}
