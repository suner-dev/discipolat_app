import 'package:flutter/material.dart';
import '../../data/services/api_service.dart';
import '../../../presentation/widgets/glass_theme.dart';
import '../../../presentation/widgets/app_drawer.dart';

/// Détail d'un ticket — branché sur /api/v1/tickets/{id}.
class TicketDetailScreen extends StatefulWidget {
  final String ticketId;
  const TicketDetailScreen({super.key, required this.ticketId});

  @override
  State<TicketDetailScreen> createState() => _TicketDetailScreenState();
}

class _TicketDetailScreenState extends State<TicketDetailScreen> {
  final _apiService = ApiService();
  Map<String, dynamic>? _ticket;
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
      final res = await _apiService.get('/tickets/${widget.ticketId}');
      if (mounted) {
        final data = res.data;
        setState(() {
          _ticket = data is Map<String, dynamic> ? data : null;
          _isLoading = false;
        });
      }
    } catch (_) {
      if (mounted) {
        setState(() {
          _error = 'Erreur lors du chargement du ticket';
          _isLoading = false;
        });
      }
    }
  }

  Color _priorityColor(String? p) {
    switch (p?.toUpperCase()) {
      case 'HAUTE':
      case 'HIGH':
        return Colors.red;
      case 'MOYENNE':
      case 'MEDIUM':
        return Colors.amber;
      case 'BASSE':
      case 'LOW':
        return Colors.green;
      default:
        return Colors.grey;
    }
  }

  Color _statusColor(String? s) {
    switch (s?.toUpperCase()) {
      case 'RESOLU':
      case 'RESOLVED':
      case 'FERME':
      case 'CLOSED':
        return Colors.green;
      case 'EN_COURS':
      case 'EN COURS':
      case 'IN_PROGRESS':
        return Colors.blue;
      case 'EN_ATTENTE':
      case 'EN ATTENTE':
      case 'OPEN':
        return Colors.amber;
      default:
        return Colors.grey;
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: Text(_ticket?['titre']?.toString() ?? _ticket?['sujet']?.toString() ?? 'Ticket'),
        backgroundColor: AppColors.primary,
        foregroundColor: Colors.white,
        actions: [IconButton(icon: const Icon(Icons.refresh), onPressed: _loadData)],
      ),
      drawer: const AppDrawer(),
      body: _isLoading
          ? const ShimmerLoading(itemCount: 4)
          : _error != null
              ? _buildError()
              : _ticket == null
                  ? _buildEmpty()
                  : RefreshIndicator(
                      onRefresh: _loadData,
                      child: SingleChildScrollView(
                        padding: const EdgeInsets.all(16),
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            _buildHeaderCard(),
                            const SizedBox(height: 16),
                            _buildDetailsCard(),
                          ],
                        ),
                      ),
                    ),
    );
  }

  Widget _buildHeaderCard() {
    final t = _ticket!;
    final titre = t['titre']?.toString() ?? t['sujet']?.toString() ?? 'Ticket';
    final statut = t['statut']?.toString() ?? t['status']?.toString() ?? 'OUVERT';
    final priorite = t['priorite']?.toString() ?? t['priority']?.toString() ?? 'MOYENNE';
    final categorie = t['categorie']?.toString() ?? t['category']?.toString() ?? '';

    return GlassCard(
      padding: const EdgeInsets.all(16),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Container(
                width: 48,
                height: 48,
                decoration: BoxDecoration(
                  color: _priorityColor(priorite).withValues(alpha: 0.15),
                  borderRadius: BorderRadius.circular(12),
                ),
                child: Icon(Icons.support_agent, color: _priorityColor(priorite), size: 24),
              ),
              const SizedBox(width: 12),
              Expanded(
                child: Text(titre,
                    style: const TextStyle(color: Colors.white, fontWeight: FontWeight.bold, fontSize: 18)),
              ),
            ],
          ),
          const SizedBox(height: 12),
          Row(
            children: [
              StatusBadge(label: statut, color: _statusColor(statut)),
              const SizedBox(width: 8),
              StatusBadge(label: priorite, color: _priorityColor(priorite)),
              if (categorie.isNotEmpty) ...[
                const SizedBox(width: 8),
                StatusBadge(label: categorie, color: Colors.purple),
              ],
            ],
          ),
        ],
      ),
    );
  }

  Widget _buildDetailsCard() {
    final t = _ticket!;
    final description = t['description']?.toString() ?? '';
    final auteur = t['auteur']?.toString() ?? t['author']?.toString() ?? t['createur']?.toString() ?? '';
    final assigne = t['assigneA']?.toString() ?? t['assignedTo']?.toString() ?? '';
    final dateCreation = t['createdAt']?.toString().substring(0, 10) ?? '';
    final dateModification = t['updatedAt']?.toString().substring(0, 10) ?? '';

    return GlassCard(
      padding: const EdgeInsets.all(16),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const Text('Détails', style: TextStyle(color: Colors.white, fontWeight: FontWeight.bold, fontSize: 16)),
          const SizedBox(height: 12),
          if (description.isNotEmpty) ...[
            Text(description, style: TextStyle(color: Colors.white.withValues(alpha: 0.7), fontSize: 14)),
            const SizedBox(height: 12),
          ],
          _infoRow('Créé par', auteur),
          _infoRow('Assigné à', assigne),
          _infoRow('Date de création', dateCreation),
          if (dateModification.isNotEmpty) _infoRow('Dernière modification', dateModification),
        ],
      ),
    );
  }

  Widget _infoRow(String label, String value) {
    if (value.isEmpty) return const SizedBox.shrink();
    return Padding(
      padding: const EdgeInsets.only(bottom: 8),
      child: Row(
        children: [
          SizedBox(
            width: 140,
            child: Text(label, style: TextStyle(color: Colors.white.withValues(alpha: 0.4), fontSize: 12)),
          ),
          Expanded(
            child: Text(value, style: const TextStyle(color: Colors.white, fontSize: 12, fontWeight: FontWeight.w500)),
          ),
        ],
      ),
    );
  }

  Widget _buildEmpty() {
    return Center(
      child: Column(
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          Icon(Icons.support_agent, size: 48, color: Colors.white.withValues(alpha: 0.2)),
          const SizedBox(height: 12),
          Text('Ticket non trouvé', style: TextStyle(color: Colors.white.withValues(alpha: 0.5))),
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
