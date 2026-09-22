import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import 'package:discipolat_mobile/features/discipleship/models/discipleship_model.dart';
import 'package:discipolat_mobile/features/discipleship/services/discipleship_service.dart'
    as discipleship_service;
import 'package:discipolat_mobile/presentation/widgets/glass_theme.dart'
    show AppColors;
import 'package:discipolat_mobile/features/discipleship/widgets/journey_card.dart';
import 'package:discipolat_mobile/features/discipleship/widgets/progress_card.dart';
import 'package:discipolat_mobile/features/discipleship/widgets/mentor_card.dart';
import 'package:discipolat_mobile/features/discipleship/widgets/meeting_card.dart';

class DiscipleshipScreen extends ConsumerStatefulWidget {
  const DiscipleshipScreen({super.key});

  @override
  ConsumerState<DiscipleshipScreen> createState() => _DiscipleshipScreenState();
}

class _DiscipleshipScreenState extends ConsumerState<DiscipleshipScreen> with SingleTickerProviderStateMixin {
  late TabController _tabController;

  @override
  void initState() {
    super.initState();
    _tabController = TabController(length: 4, vsync: this);
  }

  @override
  void dispose() {
    _tabController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final journeysAsync = ref.watch(_journeysProvider);
    final progressAsync = ref.watch(_progressProvider);
    final assignmentsAsync = ref.watch(_assignmentsProvider);
    final meetingsAsync = ref.watch(_meetingsProvider);

    return Scaffold(
      backgroundColor: AppColors.surfaceDark,
      appBar: AppBar(
        title: const Text('Discipleship'),
        backgroundColor: AppColors.cardDark,
        elevation: 0,
        bottom: TabBar(
          controller: _tabController,
          indicatorColor: Colors.purple,
          labelColor: Colors.purple,
          unselectedLabelColor: AppColors.surface.withOpacity(0.7),
          tabs: const [
            Tab(text: 'Parcours'),
            Tab(text: 'Progression'),
            Tab(text: 'Mentorat'),
            Tab(text: 'Réunions'),
          ],
        ),
        actions: [
          PopupMenuButton<bool>(
            icon: const Icon(Icons.filter_list_rounded),
            onSelected: (value) => setState(() {}),
            itemBuilder: (context) => [
              const PopupMenuItem(value: true, child: Text('Actifs seulement')),
              const PopupMenuItem(value: false, child: Text('Tous')),
            ],
          ),
          IconButton(
            icon: const Icon(Icons.add_rounded),
            onPressed: () {
              switch (_tabController.index) {
                case 0:
                  context.push('/discipleship/journeys/create');
                  break;
                case 1:
                  context.push('/discipleship/progress/create');
                  break;
                case 2:
                  context.push('/discipleship/assignments/create');
                  break;
                case 3:
                  context.push('/discipleship/meetings/create');
                  break;
              }
            },
            tooltip: 'Nouveau',
          ),
        ],
      ),
      body: TabBarView(
        controller: _tabController,
        children: [
          // Journeys tab
          _buildJourneysTab(),
          // Progress tab
          _buildProgressTab(),
          // Assignments tab
          _buildAssignmentsTab(),
          // Meetings tab
          _buildMeetingsTab(),
        ],
      ),
      floatingActionButton: FloatingActionButton.extended(
        onPressed: () {
          switch (_tabController.index) {
            case 0:
              context.push('/discipleship/journeys/create');
              break;
            case 1:
              context.push('/discipleship/progress/create');
              break;
            case 2:
              context.push('/discipleship/assignments/create');
              break;
            case 3:
              context.push('/discipleship/meetings/create');
              break;
          }
        },
        icon: const Icon(Icons.add_rounded),
        label: Text(_getFabLabel()),
        backgroundColor: Colors.purple,
      ),
    );
  }

  String _getFabLabel() {
    switch (_tabController.index) {
      case 0:
        return 'Nouveau parcours';
      case 1:
        return 'Nouvelle progression';
      case 2:
        return 'Nouvelle assignation';
      case 3:
        return 'Nouvelle réunion';
    }
    return '';
  }

  Widget _buildJourneysTab() {
    final journeysAsync = ref.watch(_journeysProvider);

    return RefreshIndicator(
      onRefresh: () => ref.refresh(_journeysProvider.future),
      child: journeysAsync.when(
        data: (journeys) {
          if (journeys.isEmpty) {
            return _buildEmptyState(
              message: 'Aucun parcours de discipleship',
              action: () => context.push('/discipleship/journeys/create'),
            );
          }
          return ListView.builder(
            padding: const EdgeInsets.all(16),
            itemCount: journeys.length,
            itemBuilder: (context, index) {
              final journey = journeys[index];
              return JourneyCard(
                journey: journey,
                onTap: () => context.push('/discipleship/journeys/${journey.id}'),
              );
            },
          );
        },
        loading: () => const Center(child: CircularProgressIndicator()),
        error: (error, _) => Center(child: Text('Erreur: $error')),
      ),
    );
  }

  Widget _buildProgressTab() {
    final progressAsync = ref.watch(_progressProvider);

    return RefreshIndicator(
      onRefresh: () => ref.refresh(_progressProvider.future),
      child: progressAsync.when(
        data: (progress) {
          if (progress.isEmpty) {
            return _buildEmptyState(
              message: 'Aucune progression de discipleship',
              action: () => context.push('/discipleship/progress/create'),
            );
          }
          return ListView.builder(
            padding: const EdgeInsets.all(16),
            itemCount: progress.length,
            itemBuilder: (context, index) {
              final progress = progress[index];
              return ProgressCard(
                progress: progress,
                onTap: () => context.push('/discipleship/progress/${progress.id}'),
              );
            },
          );
        },
        loading: () => const Center(child: CircularProgressIndicator()),
        error: (error, _) => Center(child: Text('Erreur: $error')),
      ),
    );
  }

  Widget _buildAssignmentsTab() {
    final assignmentsAsync = ref.watch(_assignmentsProvider);

    return RefreshIndicator(
      onRefresh: () => ref.refresh(_assignmentsProvider.future),
      child: assignmentsAsync.when(
        data: (assignments) {
          if (assignments.isEmpty) {
            return _buildEmptyState(
              message: 'Aucune relation mentor-disciple',
              action: () => context.push('/discipleship/assignments/create'),
            );
          }
          return ListView.builder(
            padding: const EdgeInsets.all(16),
            itemCount: assignments.length,
            itemBuilder: (context, index) {
              final assignment = assignments[index];
              return MentorCard(
                assignment: assignment,
                onTap: () => context.push('/discipleship/assignments/${assignment.id}'),
              );
            },
          );
        },
        loading: () => const Center(child: CircularProgressIndicator()),
        error: (error, _) => Center(child: Text('Erreur: $error')),
      ),
    );
  }

  Widget _buildMeetingsTab() {
    final meetingsAsync = ref.watch(_meetingsProvider);

    return RefreshIndicator(
      onRefresh: () => ref.refresh(_meetingsProvider.future),
      child: meetingsAsync.when(
        data: (meetings) {
          if (meetings.isEmpty) {
            return _buildEmptyState(
              message: 'Aucune réunion de mentoring',
              action: () => context.push('/discipleship/meetings/create'),
            );
          }
          return ListView.builder(
            padding: const EdgeInsets.all(16),
            itemCount: meetings.length,
            itemBuilder: (context, index) {
              final meeting = meetings[index];
              return MeetingCard(
                meeting: meeting,
                onTap: () => context.push('/discipleship/meetings/${meeting.id}'),
              );
            },
          );
        },
        loading: () => const Center(child: CircularProgressIndicator()),
        error: (error, _) => Center(child: Text('Erreur: $error')),
      ),
    );
  }

  Widget _buildEmptyState({required String message, VoidCallback? action}) {
    return Center(
      child: Column(
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          Icon(Icons.menu_book_rounded, size: 64, color: AppColors.surface.withOpacity(0.5)),
          const SizedBox(height: 16),
          Text(message, style: TextStyle(color: AppColors.surface.withOpacity(0.7))),
          if (action != null) ...[
            const SizedBox(height: 24),
            FilledButton.icon(onPressed: action, icon: const Icon(Icons.add_rounded), label: const Text('Créer')),
          ],
        ],
      ),
    );
  }
}

// Providers
final _journeysProvider = FutureProvider<List<DiscipleshipJourney>>((ref) async {
  final service = ref.watch(discipleship_service.DiscipleshipServiceProvider);
  return service.getJourneys();
});

final _progressProvider = FutureProvider<List<DiscipleProgress>>((ref) async {
  final service = ref.watch(discipleship_service.DiscipleshipServiceProvider);
  return service.getProgress();
});

final _assignmentsProvider = FutureProvider<List<MentorAssignment>>((ref) async {
  final service = ref.watch(discipleship_service.DiscipleshipServiceProvider);
  return service.getMentorAssignments();
});

final _meetingsProvider = FutureProvider<List<MentorMeeting>>((ref) async {
  final service = ref.watch(discipleship_service.DiscipleshipServiceProvider);
  return service.getMeetings();
});