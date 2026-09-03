import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../data/services/providers.dart';
import '../../../models/scheduled_announcement.dart';

/// Scheduled announcements screen — wired to real backend AnnouncementController.
class ScheduledAnnouncementsScreen extends ConsumerStatefulWidget {
  const ScheduledAnnouncementsScreen({super.key});

  @override
  ConsumerState<ScheduledAnnouncementsScreen> createState() => _ScheduledAnnouncementsScreenState();
}

class _ScheduledAnnouncementsScreenState extends ConsumerState<ScheduledAnnouncementsScreen> {
  late Future<List<ScheduledAnnouncement>> _future;

  @override
  void initState() {
    super.initState();
    _reload();
  }

  void _reload() {
    setState(() {
      _future = ref.read(announcementServiceProvider).fetchAll();
    });
  }

  Color _statusColor(String s) {
    switch (s) {
      case 'PUBLISHED':
        return Colors.green;
      case 'SCHEDULED':
        return Colors.blue;
      case 'DRAFT':
        return Colors.grey;
      case 'CANCELLED':
        return Colors.orange;
      case 'EXPIRED':
        return Colors.red;
      default:
        return Colors.grey;
    }
  }

  String _statusLabel(String s) {
    switch (s) {
      case 'PUBLISHED':
        return 'Publié';
      case 'SCHEDULED':
        return 'Planifié';
      case 'DRAFT':
        return 'Brouillon';
      case 'CANCELLED':
        return 'Annulé';
      case 'EXPIRED':
        return 'Expiré';
      default:
        return s;
    }
  }

  String _targetLabel(String t) {
    switch (t) {
      case 'ALL':
        return 'Tous';
      case 'DEPARTMENT':
        return 'Département';
      case 'FAMILY':
        return 'Famille';
      case 'ROLE':
        return 'Rôle';
      case 'SPECIFIC_USERS':
        return 'Utilisateurs';
      default:
        return t;
    }
  }

  Future<void> _showCreateDialog() async {
    final titleCtrl = TextEditingController();
    final contentCtrl = TextEditingController();
    String target = 'ALL';

    final result = await showDialog<bool>(
      context: context,
      builder: (ctx) => AlertDialog(
        title: const Text('Créer une annonce'),
        content: SingleChildScrollView(
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              TextField(
                controller: titleCtrl,
                decoration: const InputDecoration(labelText: 'Titre', border: OutlineInputBorder()),
              ),
              const SizedBox(height: 12),
              TextField(
                controller: contentCtrl,
                maxLines: 3,
                decoration: const InputDecoration(labelText: 'Contenu', border: OutlineInputBorder()),
              ),
              const SizedBox(height: 12),
              DropdownButtonFormField<String>(
                initialValue: target,
                decoration: const InputDecoration(labelText: 'Cible', border: OutlineInputBorder()),
                items: const [
                  DropdownMenuItem(value: 'ALL', child: Text('Tous')),
                  DropdownMenuItem(value: 'DEPARTMENT', child: Text('Département')),
                  DropdownMenuItem(value: 'FAMILY', child: Text('Famille')),
                  DropdownMenuItem(value: 'ROLE', child: Text('Rôle')),
                ],
                onChanged: (v) => target = v ?? 'ALL',
              ),
            ],
          ),
        ),
        actions: [
          TextButton(onPressed: () => Navigator.pop(ctx, false), child: const Text('Annuler')),
          FilledButton(
            onPressed: () => Navigator.pop(ctx, true),
            child: const Text('Créer'),
          ),
        ],
      ),
    );

    if (result == true && titleCtrl.text.isNotEmpty && contentCtrl.text.isNotEmpty) {
      try {
        final svc = ref.read(announcementServiceProvider);
        await svc.create(
          title: titleCtrl.text,
          content: contentCtrl.text,
          target: target,
        );
        _reload();
        if (mounted) {
          ScaffoldMessenger.of(context).showSnackBar(
            const SnackBar(content: Text('Annonce créée avec succès')),
          );
        }
      } catch (e) {
        if (mounted) {
          ScaffoldMessenger.of(context).showSnackBar(
            SnackBar(content: Text('Erreur: ${e.toString().split(':').first}')),
          );
        }
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Annonces programmées'),
        backgroundColor: Colors.orange.shade600,
        foregroundColor: Colors.white,
        actions: [
          IconButton(icon: const Icon(Icons.refresh), onPressed: _reload, tooltip: 'Actualiser'),
        ],
      ),
      floatingActionButton: FloatingActionButton(
        onPressed: _showCreateDialog,
        backgroundColor: Colors.orange,
        child: const Icon(Icons.add, color: Colors.white),
      ),
      body: FutureBuilder<List<ScheduledAnnouncement>>(
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
          final announcements = snapshot.data ?? [];
          if (announcements.isEmpty) {
            return Center(
              child: Column(
                mainAxisSize: MainAxisSize.min,
                children: [
                  Icon(Icons.campaign_outlined, size: 48, color: Colors.grey.shade400),
                  const SizedBox(height: 12),
                  Text('Aucune annonce.',
                      style: TextStyle(color: Colors.grey.shade600)),
                ],
              ),
            );
          }
          return RefreshIndicator(
            onRefresh: () async => _reload(),
            child: ListView.builder(
              padding: const EdgeInsets.all(16),
              itemCount: announcements.length,
              itemBuilder: (context, i) {
                final a = announcements[i];
                final color = _statusColor(a.status);
                return Card(
                  margin: const EdgeInsets.only(bottom: 12),
                  child: ListTile(
                    leading: a.pinToTop
                        ? const Text('📌', style: TextStyle(fontSize: 24))
                        : Icon(Icons.campaign, color: Colors.orange),
                    title: Text(a.title, style: const TextStyle(fontWeight: FontWeight.bold)),
                    subtitle: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        if (a.content != null && a.content!.isNotEmpty)
                          Padding(
                            padding: const EdgeInsets.only(top: 4),
                            child: Text(a.content!,
                                maxLines: 2,
                                overflow: TextOverflow.ellipsis,
                                style: TextStyle(fontSize: 12, color: Colors.grey.shade600)),
                          ),
                        const SizedBox(height: 4),
                        Row(children: [
                          Container(
                            padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 2),
                            decoration: BoxDecoration(
                                color: color.withOpacity(0.15),
                                borderRadius: BorderRadius.circular(8)),
                            child: Text(_statusLabel(a.status),
                                style: TextStyle(
                                    color: color, fontSize: 11, fontWeight: FontWeight.bold)),
                          ),
                          const SizedBox(width: 8),
                          Text('Cible: ${_targetLabel(a.target)}',
                              style: TextStyle(fontSize: 11, color: Colors.grey.shade500)),
                        ]),
                      ],
                    ),
                    trailing: PopupMenuButton<String>(
                      onSelected: (action) async {
                        final svc = ref.read(announcementServiceProvider);
                        try {
                          switch (action) {
                            case 'publish':
                              await svc.publish(a.id);
                              break;
                            case 'cancel':
                              await svc.cancel(a.id);
                              break;
                            case 'delete':
                              await svc.delete(a.id);
                              break;
                          }
                          _reload();
                        } catch (e) {
                          if (mounted) {
                            ScaffoldMessenger.of(context).showSnackBar(
                              SnackBar(content: Text('Erreur : ${e.toString().split(':').first}')),
                            );
                          }
                        }
                      },
                      itemBuilder: (_) => [
                        if (a.status == 'SCHEDULED' || a.status == 'DRAFT')
                          const PopupMenuItem(value: 'publish', child: Text('Publier')),
                        if (a.status == 'SCHEDULED')
                          const PopupMenuItem(value: 'cancel', child: Text('Annuler')),
                        const PopupMenuItem(value: 'delete', child: Text('Supprimer')),
                      ],
                    ),
                  ),
                );
              },
            ),
          );
        },
      ),
    );
  }
}
