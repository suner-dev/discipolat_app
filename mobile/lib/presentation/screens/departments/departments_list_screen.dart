import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../../data/services/providers.dart';
import '../../widgets/glass_theme.dart';

final departmentsProvider = FutureProvider((ref) async {
  final api = ref.read(apiServiceProvider);
  final res = await api.get('/departments?size=50');
  return res.data['content'] as List<dynamic>;
});

class DepartmentsListScreen extends ConsumerWidget {
  const DepartmentsListScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final deptsAsync = ref.watch(departmentsProvider);

    return Scaffold(
      appBar: AppBar(title: const Text('Départements')),
      body: deptsAsync.when(
        data: (depts) => depts.isEmpty
            ? const Center(child: Text('Aucun département'))
            : ListView.builder(
                padding: const EdgeInsets.all(16),
                itemCount: depts.length,
                itemBuilder: (context, index) {
                  final dept = depts[index];
                  return Card(
                    color: AppColors.surface,
                    child: ListTile(
                      leading: Container(
                        padding: const EdgeInsets.all(8),
                        decoration: BoxDecoration(
                          color: Colors.amber.withValues(alpha: 0.1),
                          borderRadius: BorderRadius.circular(10),
                        ),
                        child: const Icon(Icons.business, color: Colors.amber),
                      ),
                      title: Text(dept['nom'] ?? '', style: const TextStyle(color: Colors.white)),
                      subtitle: Text(
                        dept['description'] ?? 'Aucune description',
                        style: TextStyle(color: Colors.white.withValues(alpha: 0.6)),
                        maxLines: 1,
                        overflow: TextOverflow.ellipsis,
                      ),
                      trailing: Chip(
                        label: Text(
                          dept['statut'] ?? '',
                          style: const TextStyle(fontSize: 10, color: Colors.white),
                        ),
                        backgroundColor: Colors.green.withValues(alpha: 0.3),
                      ),
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
