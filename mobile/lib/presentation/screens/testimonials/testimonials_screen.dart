import 'package:flutter/material.dart';
import '../../../data/services/api_service.dart';

class TestimonialsScreen extends StatefulWidget {
  const TestimonialsScreen({super.key});

  @override
  State<TestimonialsScreen> createState() => _TestimonialsScreenState();
}

class _TestimonialsScreenState extends State<TestimonialsScreen> {
  List<dynamic> testimonies = [];
  bool loading = true;

  @override
  void initState() {
    super.initState();
    _loadTestimonies();
  }

  Future<void> _loadTestimonies() async {
    try {
      final res = await ApiService().get('/testimonies');
      setState(() {
        testimonies = (res.data['content'] ?? res.data ?? []) as List;
        loading = false;
      });
    } catch (_) {
      setState(() => loading = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Témoignages'),
        actions: [
          IconButton(
            icon: const Icon(Icons.add),
            onPressed: () {},
          ),
        ],
      ),
      body: loading
          ? const Center(child: CircularProgressIndicator())
          : testimonies.isEmpty
              ? const Center(child: Text('Aucun témoignage'))
              : ListView.builder(
                  itemCount: testimonies.length,
                  itemBuilder: (context, index) {
                    final t = testimonies[index];
                    return Card(
                      margin: const EdgeInsets.symmetric(horizontal: 16, vertical: 4),
                      child: Padding(
                        padding: const EdgeInsets.all(16),
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Row(
                              children: [
                                Text('${t['categorie'] ?? ''}',
                                    style: const TextStyle(fontSize: 12, color: Colors.pink)),
                                const SizedBox(width: 8),
                                if (t['statut'] == 'EN_ATTENTE')
                                  const Chip(label: Text('En attente', style: TextStyle(fontSize: 10)),
                                      backgroundColor: Colors.amber),
                              ],
                            ),
                            const SizedBox(height: 8),
                            Text(t['titre'] ?? '', style: const TextStyle(fontWeight: FontWeight.bold)),
                            const SizedBox(height: 4),
                            Text(t['contenu'] ?? '', maxLines: 3, overflow: TextOverflow.ellipsis,
                                style: const TextStyle(fontSize: 13)),
                            const SizedBox(height: 8),
                            Row(
                              children: [
                                const Icon(Icons.thumb_up, size: 14, color: Colors.grey),
                                const SizedBox(width: 4),
                                Text('${t['likes'] ?? 0}', style: const TextStyle(fontSize: 12, color: Colors.grey)),
                              ],
                            ),
                          ],
                        ),
                      ),
                    );
                  },
                ),
    );
  }
}
