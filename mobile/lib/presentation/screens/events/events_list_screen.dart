import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../../data/services/providers.dart';
import '../../widgets/glass_theme.dart';

final eventsProvider = FutureProvider((ref) async {
  final api = ref.read(apiServiceProvider);
  final res = await api.get('/events?size=50');
  return res.data['content'] as List<dynamic>;
});

class EventsListScreen extends ConsumerWidget {
  const EventsListScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final eventsAsync = ref.watch(eventsProvider);

    return Scaffold(
      appBar: AppBar(
        title: const Text('Événements'),
        actions: [
          IconButton(icon: const Icon(Icons.add), onPressed: () {}),
        ],
      ),
      body: eventsAsync.when(
        data: (events) => events.isEmpty
            ? const Center(child: Text('Aucun événement'))
            : ListView.builder(
                padding: const EdgeInsets.all(16),
                itemCount: events.length,
                itemBuilder: (context, index) {
                  final event = events[index];
                  return Card(
                    color: AppColors.surface,
                    child: ListTile(
                      leading: Container(
                        padding: const EdgeInsets.all(8),
                        decoration: BoxDecoration(
                          color: AppColors.primary.withValues(alpha: 0.1),
                          borderRadius: BorderRadius.circular(10),
                        ),
                        child: Icon(
                          event['typeEvenement'] == 'SORTIE' ? Icons.trip_origin :
                          event['typeEvenement'] == 'RETRAITE' ? Icons.hotel :
                          event['typeEvenement'] == 'FORMATION' ? Icons.school : Icons.event,
                          color: AppColors.primaryLight,
                        ),
                      ),
                      title: Text(event['titre'] ?? '', style: const TextStyle(color: Colors.white)),
                      subtitle: Text(
                        '${event['dateDebut']?.toString().substring(0, 10) ?? ''} · ${event['lieu'] ?? ''}',
                        style: TextStyle(color: Colors.white.withValues(alpha: 0.6)),
                      ),
                      trailing: Chip(
                        label: Text(
                          event['statut'] ?? '',
                          style: const TextStyle(fontSize: 10, color: Colors.white),
                        ),
                        backgroundColor: event['statut'] == 'TERMINE'
                            ? Colors.green.withValues(alpha: 0.3)
                            : AppColors.primary.withValues(alpha: 0.3),
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
