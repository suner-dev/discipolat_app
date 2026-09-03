import 'package:flutter/material.dart';
import '../../data/services/api_service.dart';
import '../../../presentation/widgets/glass_theme.dart';
import '../../../presentation/widgets/app_drawer.dart';

/// Vérification des notes de visite IA — branché sur /api/v1/ai-visit-notes.
class VisitNotesVerifyScreen extends StatefulWidget {
  final String memberId;
  const VisitNotesVerifyScreen({super.key, required this.memberId});

  @override
  State<VisitNotesVerifyScreen> createState() => _VisitNotesVerifyScreenState();
}

class _VisitNotesVerifyScreenState extends State<VisitNotesVerifyScreen> {
  final _apiService = ApiService();
  List<dynamic> _notes = [];
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
      final res = await _apiService.get('/ai-visit-notes/member/${widget.memberId}');
      if (mounted) {
        final data = res.data;
        setState(() {
          _notes = data is List
              ? data
              : (data is Map && data['content'] is List
                  ? data['content'] as List<dynamic>
                  : <dynamic>[]);
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

  Future<void> _verifyNote(String id) async {
    try {
      await _apiService.post('/ai-visit-notes/$id/verify');
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('Note vérifiée'), backgroundColor: Colors.green),
        );
        _loadData();
      }
    } catch (_) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('Erreur lors de la vérification'), backgroundColor: Colors.red),
        );
      }
    }
  }

  Color _statusColor(String? statut) {
    switch (statut?.toUpperCase()) {
      case 'VERIFIE':
      case 'VERIFIED':
        return Colors.green;
      case 'EN_ATTENTE':
      case 'EN ATTENTE':
      case 'PENDING':
        return Colors.amber;
      case 'REJETE':
      case 'REJECTED':
        return Colors.red;
      default:
        return Colors.grey;
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Notes de visite IA'),
        backgroundColor: AppColors.primary,
        foregroundColor: Colors.white,
        actions: [IconButton(icon: const Icon(Icons.refresh), onPressed: _loadData)],
      ),
      drawer: const AppDrawer(),
      body: _isLoading
          ? const ShimmerLoading(itemCount: 5)
          : _error != null
              ? _buildError()
              : RefreshIndicator(
                  onRefresh: _loadData,
                  child: _notes.isEmpty
                      ? _buildEmpty()
                      : ListView.builder(
                          padding: const EdgeInsets.all(16),
                          itemCount: _notes.length,
                          itemBuilder: (context, i) {
                            final note = _notes[i] as Map<String, dynamic>;
                            final titre = note['titre']?.toString() ?? note['nom']?.toString() ?? 'Note';
                            final contenu = note['contenu']?.toString() ?? note['texte']?.toString() ?? '';
                            final statut = note['statut']?.toString() ?? note['status']?.toString() ?? '';
                            final dateStr = note['dateVisite']?.toString().substring(0, 10) ??
                                note['createdAt']?.toString().substring(0, 10) ?? '';
                            final id = note['id']?.toString() ?? '';
                            final isPending = statut.toUpperCase().contains('ATTENTE') || statut.toUpperCase().contains('PENDING');

                            return GlassCard(
                              margin: const EdgeInsets.only(bottom: 10),
                              padding: const EdgeInsets.all(14),
                              child: Column(
                                crossAxisAlignment: CrossAxisAlignment.start,
                                children: [
                                  Row(
                                    children: [
                                      Icon(Icons.note, color: _statusColor(statut), size: 20),
                                      const SizedBox(width: 8),
                                      Expanded(
                                        child: Text(titre,
                                            style: const TextStyle(color: Colors.white, fontWeight: FontWeight.w600, fontSize: 14)),
                                      ),
                                      if (statut.isNotEmpty)
                                        StatusBadge(label: statut, color: _statusColor(statut)),
                                    ],
                                  ),
                                  if (dateStr.isNotEmpty) ...[
                                    const SizedBox(height: 4),
                                    Text(dateStr,
                                        style: TextStyle(color: Colors.white.withValues(alpha: 0.4), fontSize: 11)),
                                  ],
                                  if (contenu.isNotEmpty) ...[
                                    const SizedBox(height: 8),
                                    Text(contenu,
                                        style: TextStyle(color: Colors.white.withValues(alpha: 0.6), fontSize: 12),
                                        maxLines: 3,
                                        overflow: TextOverflow.ellipsis),
                                  ],
                                  if (isPending && id.isNotEmpty) ...[
                                    const SizedBox(height: 12),
                                    SizedBox(
                                      width: double.infinity,
                                      child: FilledButton.icon(
                                        onPressed: () => _verifyNote(id),
                                        icon: const Icon(Icons.check_circle, size: 16),
                                        label: const Text('Vérifier cette note'),
                                        style: FilledButton.styleFrom(backgroundColor: Colors.green),
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
          Icon(Icons.note, size: 48, color: Colors.white.withValues(alpha: 0.2)),
          const SizedBox(height: 12),
          Text('Aucune note de visite', style: TextStyle(color: Colors.white.withValues(alpha: 0.5))),
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
