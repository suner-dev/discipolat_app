import 'package:flutter/material.dart';
import '../../data/services/api_service.dart';
import '../../../presentation/widgets/glass_theme.dart';
import '../../../presentation/widgets/app_drawer.dart';

/// Détail d'un document — branché sur /api/v1/documents/{id}.
class DocumentDetailScreen extends StatefulWidget {
  final String documentId;
  const DocumentDetailScreen({super.key, required this.documentId});

  @override
  State<DocumentDetailScreen> createState() => _DocumentDetailScreenState();
}

class _DocumentDetailScreenState extends State<DocumentDetailScreen> {
  final _apiService = ApiService();
  Map<String, dynamic>? _document;
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
      final res = await _apiService.get('/documents/${widget.documentId}');
      if (mounted) {
        final data = res.data;
        setState(() {
          _document = data is Map<String, dynamic> ? data : null;
          _isLoading = false;
        });
      }
    } catch (_) {
      if (mounted) {
        setState(() {
          _error = 'Erreur lors du chargement du document';
          _isLoading = false;
        });
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: Text(_document?['titre']?.toString() ?? _document?['nom']?.toString() ?? 'Document'),
        backgroundColor: AppColors.primary,
        foregroundColor: Colors.white,
        actions: [IconButton(icon: const Icon(Icons.refresh), onPressed: _loadData)],
      ),
      drawer: const AppDrawer(),
      body: _isLoading
          ? const ShimmerLoading(itemCount: 4)
          : _error != null
              ? _buildError()
              : _document == null
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
                            _buildInfoCard(),
                          ],
                        ),
                      ),
                    ),
    );
  }

  Widget _buildHeaderCard() {
    final doc = _document!;
    final titre = doc['titre']?.toString() ?? doc['nom']?.toString() ?? 'Document';
    final type = doc['type']?.toString() ?? '';
    final categorie = doc['categorie']?.toString() ?? doc['category']?.toString() ?? '';
    final statut = doc['statut']?.toString() ?? doc['status']?.toString() ?? '';

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
                  color: AppColors.primary.withValues(alpha: 0.15),
                  borderRadius: BorderRadius.circular(12),
                ),
                child: Icon(Icons.description, color: AppColors.primary, size: 24),
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
              if (type.isNotEmpty)
                StatusBadge(label: type, color: Colors.blue),
              if (type.isNotEmpty && categorie.isNotEmpty) const SizedBox(width: 8),
              if (categorie.isNotEmpty)
                StatusBadge(label: categorie, color: Colors.purple),
              if ((type.isNotEmpty || categorie.isNotEmpty) && statut.isNotEmpty) const SizedBox(width: 8),
              if (statut.isNotEmpty)
                StatusBadge(label: statut, color: Colors.green),
            ],
          ),
        ],
      ),
    );
  }

  Widget _buildInfoCard() {
    final doc = _document!;
    final description = doc['description']?.toString() ?? '';
    final auteur = doc['auteur']?.toString() ?? doc['author']?.toString() ?? '';
    final dateCreation = doc['createdAt']?.toString().substring(0, 10) ?? '';
    final dateModification = doc['updatedAt']?.toString().substring(0, 10) ?? '';
    final taille = doc['taille']?.toString() ?? doc['size']?.toString() ?? '';
    final format = doc['format']?.toString() ?? doc['mimeType']?.toString() ?? '';

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
          _infoRow('Auteur', auteur),
          _infoRow('Date de création', dateCreation),
          if (dateModification.isNotEmpty) _infoRow('Dernière modification', dateModification),
          if (taille.isNotEmpty) _infoRow('Taille', taille),
          if (format.isNotEmpty) _infoRow('Format', format),
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
          Icon(Icons.description, size: 48, color: Colors.white.withValues(alpha: 0.2)),
          const SizedBox(height: 12),
          Text('Document non trouvé', style: TextStyle(color: Colors.white.withValues(alpha: 0.5))),
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
