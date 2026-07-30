import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../../data/services/providers.dart';
import '../../widgets/glass_theme.dart';

final prayersProvider = FutureProvider.family<List<dynamic>, Map<String, String>>((ref, params) async {
  final api = ref.read(apiServiceProvider);
  final query = params.entries.map((e) => '${e.key}=${e.value}').join('&');
  final res = await api.get('/prayers?$query');
  return res.data['content'] as List<dynamic>;
});

class PrayersListScreen extends ConsumerWidget {
  const PrayersListScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final prayersAsync = ref.watch(prayersProvider({}));

    return Scaffold(
      appBar: AppBar(
        title: const Text('Prières'),
        actions: [
          IconButton(
            icon: const Icon(Icons.add),
            onPressed: () {},
          ),
        ],
      ),
      body: prayersAsync.when(
        data: (prayers) => prayers.isEmpty
            ? const Center(child: Text('Aucun sujet de prière'))
            : ListView.builder(
                padding: const EdgeInsets.all(16),
                itemCount: prayers.length,
                itemBuilder: (context, index) {
                  final prayer = prayers[index];
                  return Card(
                    color: AppColors.surface,
                    child: ListTile(
                      leading: Icon(
                        prayer['categorie'] == 'SANTE' ? Icons.medical_services :
                        prayer['categorie'] == 'FAMILLE' ? Icons.family_restroom :
                        prayer['categorie'] == 'TRAVAIL' ? Icons.work : Icons.book,
                        color: AppColors.primaryLight,
                      ),
                      title: Text(prayer['titre'] ?? '', style: const TextStyle(color: Colors.white)),
                      subtitle: Text(
                        '${prayer['categorie'] ?? ''} · ${prayer['statut'] == 'EXAUCE' ? 'Exaucé' : 'En cours'}',
                        style: TextStyle(color: Colors.white.withValues(alpha: 0.6)),
                      ),
                      trailing: prayer['statut'] == 'EXAUCE'
                          ? const Icon(Icons.check_circle, color: Colors.green)
                          : null,
                    ),
                  );
                },
              ),
        loading: () => const Center(child: CircularProgressIndicator()),
        error: (e, _) => Center(child: Text('Erreur: $e')),
      ),
    );
  }
}
