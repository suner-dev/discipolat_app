import 'package:flutter/material.dart';
import '../../../data/services/api_service.dart';
import '../../widgets/glass_theme.dart';
import '../../widgets/app_drawer.dart';

/// Cercle de Faiseurs — espace d'entraide entre faiseurs de disciples.
/// Connecté à l'API Backend : /api/v1/cercle-faiseurs
class CercleFaiseursScreen extends StatefulWidget {
  const CercleFaiseursScreen({super.key});

  @override
  State<CercleFaiseursScreen> createState() => _CercleFaiseursScreenState();
}

class _CercleFaiseursScreenState extends State<CercleFaiseursScreen> {
  final _apiService = ApiService();
  final _titleCtrl = TextEditingController();
  final _bodyCtrl = TextEditingController();
  String _selectedCategory = 'TÉMOIGNAGE';
  List<dynamic> _posts = [];
  bool _isLoading = true;

  static const Map<String, String> _categoryLabels = {
    'ENTRAIDE': 'Entraide',
    'CONSEIL': 'Conseil',
    'RESOURCES': 'Ressources',
    'PRIÈRE': 'Prière',
    'TÉMOIGNAGE': 'Témoignage',
    'AUTRE': 'Autre',
  };

  @override
  void initState() {
    super.initState();
    _load();
  }

  @override
  void dispose() {
    _titleCtrl.dispose();
    _bodyCtrl.dispose();
    super.dispose();
  }

  Future<void> _load() async {
    setState(() => _isLoading = true);
    try {
      final res = await _apiService.get('/cercle-faiseurs');
      if (mounted) {
        setState(() {
          _posts = (res.data is List ? res.data : []) as List<dynamic>;
          _isLoading = false;
        });
      }
    } catch (e) {
      debugPrint('Error loading cercle posts: $e');
      if (mounted) setState(() => _isLoading = false);
    }
  }

  Future<void> _createPost() async {
    if (_titleCtrl.text.trim().isEmpty || _bodyCtrl.text.trim().isEmpty) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(
            content: Text('Titre et contenu requis'),
            backgroundColor: Color(0xFFC62828)),
      );
      return;
    }
    try {
      await _apiService.post('/cercle-faiseurs', data: {
        'titre': _titleCtrl.text.trim(),
        'contenu': _bodyCtrl.text.trim(),
        'categorie': _selectedCategory,
      });
      _titleCtrl.clear();
      _bodyCtrl.clear();
      if (mounted) Navigator.pop(context);
      await _load();
    } catch (e) {
      debugPrint('Error creating post: $e');
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(
              content: Text('Échec de la publication'),
              backgroundColor: Color(0xFFC62828)),
        );
      }
    }
  }

  Future<void> _likePost(String id) async {
    try {
      await _apiService.post('/cercle-faiseurs/$id/like');
      await _load();
    } catch (e) {
      debugPrint('Error liking post: $e');
    }
  }

  Future<void> _showCreateDialog() async {
    _titleCtrl.clear();
    _bodyCtrl.clear();
    setState(() => _selectedCategory = 'TÉMOIGNAGE');
    await showDialog(
      context: context,
      builder: (ctx) => AlertDialog(
        backgroundColor: const Color(0xFF1E293B),
        title:
            const Text('Nouveau post', style: TextStyle(color: Colors.white)),
        content: SizedBox(
          width: double.maxFinite,
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              TextField(
                controller: _titleCtrl,
                decoration: InputDecoration(
                  labelText: 'Titre',
                  labelStyle: const TextStyle(color: Colors.white70),
                  border: const OutlineInputBorder(),
                  filled: true,
                  fillColor: const Color(0xFF0F172A),
                ),
                style: const TextStyle(color: Colors.white),
              ),
              const SizedBox(height: 12),
              TextField(
                controller: _bodyCtrl,
                decoration: InputDecoration(
                  hintText: 'Partagez un conseil, une ressource...',
                  hintStyle: const TextStyle(color: Colors.white70),
                  border: const OutlineInputBorder(),
                  filled: true,
                  fillColor: const Color(0xFF0F172A),
                ),
                style: const TextStyle(color: Colors.white),
                maxLines: 3,
              ),
              const SizedBox(height: 12),
              Wrap(
                spacing: 6,
                children: _categoryLabels.entries.map((e) {
                  final selected = _selectedCategory == e.key;
                  return ChoiceChip(
                    label: Text(e.value, style: const TextStyle(fontSize: 11)),
                    selected: selected,
                    onSelected: (_) =>
                        setState(() => _selectedCategory = e.key),
                    selectedColor: AppColors.primary,
                    backgroundColor: const Color(0xFF334155),
                    labelStyle: TextStyle(
                        color: selected ? Colors.white : Colors.white70),
                  );
                }).toList(),
              ),
            ],
          ),
        ),
        actions: [
          TextButton(
              onPressed: () => Navigator.pop(ctx),
              child: const Text('Annuler')),
          FilledButton(
            onPressed: _createPost,
            style: FilledButton.styleFrom(backgroundColor: AppColors.primary),
            child: const Text('Publier'),
          ),
        ],
      ),
    );
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
        title: const Text('Cercle de Faiseurs'),
        actions: [
          IconButton(
            icon: const Icon(Icons.add_rounded, size: 26),
            onPressed: _showCreateDialog,
          ),
        ],
      ),
      drawer: const AppDrawer(),
      body: _isLoading
          ? const Center(
              child: CircularProgressIndicator(color: Color(0xFFFFB300)))
          : RefreshIndicator(
              onRefresh: _load,
              child: _posts.isEmpty
                  ? ListView(
                      padding: const EdgeInsets.all(24),
                      children: const [
                        SizedBox(height: 80),
                        Center(
                          child: Column(
                            children: [
                              Icon(Icons.forum_outlined,
                                  size: 64, color: Colors.white24),
                              SizedBox(height: 16),
                              Text('Aucun post pour le moment',
                                  style: TextStyle(
                                      color: Colors.white70, fontSize: 16)),
                              SizedBox(height: 8),
                              Text('Soyez le premier à partager un conseil !',
                                  style: TextStyle(
                                      color: Colors.white38, fontSize: 13)),
                            ],
                          ),
                        ),
                      ],
                    )
                  : ListView.separated(
                      padding: const EdgeInsets.all(16),
                      itemCount: _posts.length,
                      separatorBuilder: (_, __) => const SizedBox(height: 12),
                      itemBuilder: (context, i) {
                        final post = _posts[i] as Map<String, dynamic>;
                        final likes = post['likes'] ?? 0;
                        return Card(
                          color: const Color(0xFF111827),
                          elevation: 4,
                          shape: RoundedRectangleBorder(
                              borderRadius: BorderRadius.circular(14)),
                          child: Padding(
                            padding: const EdgeInsets.all(16),
                            child: Column(
                              crossAxisAlignment: CrossAxisAlignment.start,
                              children: [
                                Row(
                                  children: [
                                    CircleAvatar(
                                      backgroundColor: const Color(0xFFFFB300)
                                          .withValues(alpha: 0.2),
                                      radius: 16,
                                      child: Text(
                                        (post['auteur'] ?? '🤝')[0]
                                            .toUpperCase(),
                                        style: const TextStyle(
                                            color: Color(0xFFFFB300),
                                            fontSize: 14,
                                            fontWeight: FontWeight.bold),
                                      ),
                                    ),
                                    const SizedBox(width: 8),
                                    Expanded(
                                      child: Text(
                                        post['auteur'] ?? 'Faiseur',
                                        style: const TextStyle(
                                            color: Colors.white70,
                                            fontSize: 13),
                                      ),
                                    ),
                                    if (post['categorie'] != null)
                                      Container(
                                        padding: const EdgeInsets.symmetric(
                                            horizontal: 8, vertical: 2),
                                        decoration: BoxDecoration(
                                          color: const Color(0xFFFFB300)
                                              .withValues(alpha: 0.15),
                                          borderRadius:
                                              BorderRadius.circular(10),
                                        ),
                                        child: Text(
                                          _categoryLabels[post['categorie']] ??
                                              post['categorie'],
                                          style: const TextStyle(
                                              color: Color(0xFFFFB300),
                                              fontSize: 10),
                                        ),
                                      ),
                                  ],
                                ),
                                const SizedBox(height: 8),
                                Text(
                                  post['titre'] ?? '',
                                  style: const TextStyle(
                                      color: Colors.white,
                                      fontWeight: FontWeight.w600,
                                      fontSize: 15),
                                ),
                                const SizedBox(height: 6),
                                Text(
                                  post['contenu'] ?? '',
                                  style: const TextStyle(
                                      color: Colors.white70,
                                      fontSize: 13,
                                      height: 1.4),
                                ),
                                const SizedBox(height: 12),
                                Row(
                                  mainAxisAlignment: MainAxisAlignment.end,
                                  children: [
                                    Text(
                                      '${post['createdAt'] ?? ''}',
                                      style: TextStyle(
                                          color: Colors.white
                                              .withValues(alpha: 0.4),
                                          fontSize: 11),
                                    ),
                                    const SizedBox(width: 16),
                                    GestureDetector(
                                      onTap: () =>
                                          _likePost(post['id'] as String),
                                      child: Row(
                                        children: [
                                          Icon(Icons.favorite_border,
                                              size: 18,
                                              color: Colors.grey.shade400),
                                          const SizedBox(width: 4),
                                          Text('$likes',
                                              style: TextStyle(
                                                  color: Colors.grey.shade400,
                                                  fontSize: 12)),
                                        ],
                                      ),
                                    ),
                                  ],
                                ),
                              ],
                            ),
                          ),
                        );
                      },
                    ),
            ),
      floatingActionButton: FloatingActionButton(
        onPressed: _showCreateDialog,
        backgroundColor: AppColors.primary,
        foregroundColor: Colors.white,
        child: const Icon(Icons.add),
      ),
    );
  }
}
