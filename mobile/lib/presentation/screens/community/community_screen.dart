import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../data/services/providers.dart';
import '../../../models/testimony.dart';

/// Community screen — wired to real backend TestimonyController (/api/v1/testimonies).
/// Displays testimonies, prayers, encouragements from the API.
class CommunityScreen extends ConsumerStatefulWidget {
  const CommunityScreen({super.key});

  @override
  ConsumerState<CommunityScreen> createState() => _CommunityScreenState();
}

class _CommunityScreenState extends ConsumerState<CommunityScreen> {
  late Future<List<Testimony>> _future;
  final _titleCtrl = TextEditingController();
  final _bodyCtrl = TextEditingController();
  String _selectedCategory = 'TÉMOIGNAGE';

  static const Map<String, String> _categoryLabels = {
    'GUERISON': 'Guérison',
    'DELIVRANCE': 'Délivrance',
    'PROVISION': 'Provision',
    'FAMILLE': 'Famille',
    'CONVERSION': 'Conversion',
    'AUTRE': 'Autre',
    'TÉMOIGNAGE': 'Témoignage',
    'PRIÈRE': 'Prière',
    'ENCOURAGEMENT': 'Encouragement',
  };

  @override
  void initState() {
    super.initState();
    _reload();
  }

  @override
  void dispose() {
    _titleCtrl.dispose();
    _bodyCtrl.dispose();
    super.dispose();
  }

  void _reload() {
    setState(() {
      _future = ref.read(testimonyServiceProvider).fetchAll();
    });
  }

  Color _categoryColor(String cat) {
    switch (cat.toUpperCase()) {
      case 'GUERISON':
      case 'TÉMOIGNAGE':
        return Colors.yellow.shade700;
      case 'PRIÈRE':
      case 'DELIVRANCE':
        return Colors.pink;
      case 'PROVISION':
        return Colors.blue;
      case 'FAMILLE':
        return Colors.green;
      case 'ENCOURAGEMENT':
        return Colors.teal;
      case 'CONVERSION':
        return Colors.purple;
      default:
        return Colors.grey;
    }
  }

  Future<void> _showComposeDialog() async {
    final result = await showDialog<bool>(
      context: context,
      builder: (ctx) => AlertDialog(
        title: const Text('Partager'),
        content: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            TextField(
              controller: _titleCtrl,
              decoration: const InputDecoration(labelText: 'Titre'),
            ),
            const SizedBox(height: 8),
            TextField(
              controller: _bodyCtrl,
              maxLines: 3,
              decoration: const InputDecoration(
                hintText: 'Partagez un témoignage ou encouragement...',
              ),
            ),
            const SizedBox(height: 8),
            Wrap(
              spacing: 6,
              children: _categoryLabels.entries.map((e) {
                final isSelected = _selectedCategory == e.key;
                return ChoiceChip(
                  label: Text(e.value, style: const TextStyle(fontSize: 11)),
                  selected: isSelected,
                  onSelected: (_) => setState(() => _selectedCategory = e.key),
                );
              }).toList(),
            ),
          ],
        ),
        actions: [
          TextButton(onPressed: () => Navigator.pop(ctx, false), child: const Text('Annuler')),
          FilledButton(
            onPressed: () async {
              if (_bodyCtrl.text.trim().isEmpty) return;
              try {
                await ref.read(testimonyServiceProvider).create(
                      titre: _titleCtrl.text.trim().isNotEmpty ? _titleCtrl.text.trim() : _selectedCategory,
                      contenu: _bodyCtrl.text.trim(),
                      categorie: _selectedCategory,
                    );
                _titleCtrl.clear();
                _bodyCtrl.clear();
                if (ctx.mounted) Navigator.pop(ctx, true);
              } catch (_) {
                if (ctx.mounted) Navigator.pop(ctx, false);
              }
            },
            child: const Text('Publier'),
          ),
        ],
      ),
    );
    if (result == true) _reload();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Communauté'),
        backgroundColor: Colors.pink.shade600,
        foregroundColor: Colors.white,
        actions: [
          IconButton(
            icon: const Icon(Icons.refresh),
            onPressed: _reload,
            tooltip: 'Actualiser',
          ),
        ],
      ),
      floatingActionButton: FloatingActionButton(
        onPressed: _showComposeDialog,
        backgroundColor: Colors.pink.shade600,
        child: const Icon(Icons.add, color: Colors.white),
      ),
      body: FutureBuilder<List<Testimony>>(
        future: _future,
        builder: (context, snapshot) {
          if (snapshot.connectionState == ConnectionState.waiting) {
            return const Center(child: CircularProgressIndicator());
          }
          if (snapshot.hasError) {
            return Center(
              child: Column(
                mainAxisSize: MainAxisSize.min,
                children: [
                  const Icon(Icons.error_outline, size: 48, color: Colors.red),
                  const SizedBox(height: 12),
                  const Text('Erreur de chargement'),
                  const SizedBox(height: 8),
                  FilledButton(onPressed: _reload, child: const Text('Réessayer')),
                ],
              ),
            );
          }
          final posts = snapshot.data ?? [];
          if (posts.isEmpty) {
            return Center(
              child: Column(
                mainAxisSize: MainAxisSize.min,
                children: [
                  Icon(Icons.forum, size: 48, color: Colors.grey.shade400),
                  const SizedBox(height: 12),
                  Text('Aucun témoignage pour le moment.',
                      style: TextStyle(color: Colors.grey.shade600)),
                  const SizedBox(height: 8),
                  Text('Soyez le premier à partager !',
                      style: TextStyle(color: Colors.grey.shade500, fontSize: 12)),
                ],
              ),
            );
          }
          return RefreshIndicator(
            onRefresh: () async => _reload(),
            child: ListView.builder(
              padding: const EdgeInsets.all(16),
              itemCount: posts.length,
              itemBuilder: (context, i) => _postCard(posts[i]),
            ),
          );
        },
      ),
    );
  }

  Widget _postCard(Testimony t) {
    final color = _categoryColor(t.categorie);
    final categoryLabel = _categoryLabels[t.categorie.toUpperCase()] ?? t.categorie;

    return Card(
      margin: const EdgeInsets.only(bottom: 12),
      child: Padding(
        padding: const EdgeInsets.all(12),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                CircleAvatar(
                  backgroundColor: Colors.pink.shade100,
                  radius: 18,
                  child: Text(
                    t.auteurNom.isNotEmpty ? t.auteurNom[0].toUpperCase() : '?',
                    style: TextStyle(color: Colors.pink.shade700, fontSize: 12, fontWeight: FontWeight.bold),
                  ),
                ),
                const SizedBox(width: 8),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(t.titre,
                          style: const TextStyle(fontWeight: FontWeight.w600, fontSize: 13)),
                      const SizedBox(height: 2),
                      Container(
                        padding: const EdgeInsets.symmetric(horizontal: 6, vertical: 1),
                        decoration: BoxDecoration(
                          color: color.withOpacity(0.1),
                          borderRadius: BorderRadius.circular(6),
                        ),
                        child: Text(categoryLabel,
                            style: TextStyle(fontSize: 9, color: color, fontWeight: FontWeight.bold)),
                      ),
                    ],
                  ),
                ),
                Text(
                  '${t.createdAt.day}/${t.createdAt.month}',
                  style: TextStyle(fontSize: 10, color: Colors.grey.shade500),
                ),
              ],
            ),
            const SizedBox(height: 8),
            Text(t.contenu, style: const TextStyle(fontSize: 13, height: 1.4)),
            const SizedBox(height: 8),
            Row(
              children: [
                GestureDetector(
                  onTap: () async {
                    await ref.read(testimonyServiceProvider).like(t.id);
                    _reload();
                  },
                  child: Row(
                    children: [
                      Icon(Icons.favorite_border, size: 16, color: Colors.grey.shade400),
                      Text(' ${t.likes}', style: TextStyle(fontSize: 12, color: Colors.grey.shade500)),
                    ],
                  ),
                ),
                const SizedBox(width: 16),
                Icon(Icons.chat_bubble_outline, size: 16, color: Colors.grey.shade400),
                Text(' ${t.commentaires}', style: TextStyle(fontSize: 12, color: Colors.grey.shade500)),
              ],
            ),
          ],
        ),
      ),
    );
  }
}
