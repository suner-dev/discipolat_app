import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart'
    show ConsumerStatefulWidget, ConsumerState, ref, ProviderScope, ProviderListenable;
import 'package:intl/intl.dart';
import 'package:go_router/go_router.dart';

import 'package:discipolat_mobile/features/tasks/models/task_model.dart';
import 'package:discipolat_mobile/features/tasks/services/tasks_service.dart';
import 'package:discipolat_mobile/presentation/widgets/glass_theme.dart'
    show AppColors;
import 'package:discipolat_mobile/features/tasks/widgets/task_card.dart';
import 'package:discipolat_mobile/features/tasks/widgets/task_filter_chips.dart';

class TasksScreen extends ConsumerStatefulWidget {
  const TasksScreen({super.key});

  @override
  ConsumerState<TasksScreen> createState() => _TasksScreenState();
}

class _TasksScreenState extends ConsumerState<TasksScreen> with SingleTickerProviderStateMixin {
  TaskStatus? _filterStatus;
  TaskPriority? _filterPriority;
  TaskType? _filterType;
  late TabController _tabController;

  @override
  void initState() {
    super.initState();
    _tabController = TabController(length: 3, vsync: this);
  }

  @override
  void dispose() {
    _tabController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final tasksAsync = ref.watch(_tasksProvider(_filterStatus, _filterPriority, _filterType));
    final kanbanAsync = ref.watch(_kanbanProvider);
    final statsAsync = ref.watch(_statsProvider);

    return Scaffold(
      backgroundColor: AppColors.surfaceDark,
      appBar: AppBar(
        title: const Text('Tâches & Projets'),
        backgroundColor: AppColors.cardDark,
        elevation: 0,
        bottom: TabBar(
          controller: _tabController,
          indicatorColor: AppColors.primary,
          labelColor: AppColors.primary,
          unselectedLabelColor: AppColors.surface.withOpacity(0.7),
          tabs: const [
            Tab(text: 'Liste'),
            Tab(text: 'Kanban'),
            Tab(text: 'Statistiques'),
          ],
        ),
        actions: [
          PopupMenuButton<TaskStatus?>(
            icon: const Icon(Icons.filter_list_rounded),
            onSelected: (value) => setState(() => _filterStatus = value),
            itemBuilder: (context) => [
              const PopupMenuItem(value: null, child: Text('Tous statuts')),
              ...TaskStatus.values.map((s) => PopupMenuItem(value: s, child: Text(s.displayName))),
            ],
          ),
          PopupMenuButton<TaskPriority?>(
            icon: const Icon(Icons.filter_alt_rounded),
            onSelected: (value) => setState(() => _filterPriority = value),
            itemBuilder: (context) => [
              const PopupMenuItem(value: null, child: Text('Toutes priorités')),
              ...TaskPriority.values.map((p) => PopupMenuItem(value: p, child: Row(
                children: [
                  Container(width: 10, height: 10, decoration: BoxDecoration(color: p.getColor(), shape: BoxShape.circle)),
                  const SizedBox(width: 8),
                  Text(p.displayName),
                ])),
              ),
            ],
          ),
          PopupMenuButton<TaskType?>(
            icon: const Icon(Icons.category_rounded),
            onSelected: (value) => setState(() => _filterType = value),
            itemBuilder: (context) => [
              const PopupMenuItem(value: null, child: Text('Tous types')),
              ...TaskType.values.map((t) => PopupMenuItem(value: t, child: Text(t.name))),
            ],
          ),
          IconButton(
            icon: const Icon(Icons.add_rounded),
            onPressed: () => context.push('/tasks/create'),
            tooltip: 'Nouvelle tâche',
          ),
        ],
      ),
      body: TabBarView(
        controller: _tabController,
        children: [
          // List tab
          Column(
            children: [
              TaskFilterChips(
                selectedStatus: _filterStatus,
                selectedPriority: _filterPriority,
                selectedType: _filterType,
                onStatusChanged: (s) => setState(() => _filterStatus = s),
                onPriorityChanged: (p) => setState(() => _filterPriority = p),
                onTypeChanged: (t) => setState(() => _filterType = t),
              ),
              Expanded(
                child: RefreshIndicator(
                  onRefresh: () => ref.refresh(_tasksProvider(_filterStatus, _filterPriority, _filterType).future),
                  child: tasksAsync.when(
                    data: (tasks) {
                      if (tasks.isEmpty) {
                        return _buildEmptyState();
                      }
                      return ListView.builder(
                        padding: const EdgeInsets.all(16),
                        itemCount: tasks.length,
                        itemBuilder: (context, index) {
                          final task = tasks[index];
                          return TaskCard(
                            task: task,
                            onTap: () => context.push('/tasks/${task.id}'),
                          );
                        },
                      );
                    },
                    loading: () => const Center(child: CircularProgressIndicator()),
                    error: (error, _) => Center(child: Text('Erreur: $error')),
                  ),
                ),
              ),
            ],
          ),
          // Kanban tab
          _buildKanbanTab(),
          // Stats tab
          _buildStatsTab(statsAsync),
        ],
      ),
      floatingActionButton: FloatingActionButton.extended(
        onPressed: () => context.push('/tasks/create'),
        icon: const Icon(Icons.add_rounded),
        label: const Text('Nouvelle tâche'),
        backgroundColor: AppColors.primary,
      ),
    );
  }

  Widget _buildKanbanTab() {
    final kanbanAsync = ref.watch(_kanbanProvider);

    return kanbanAsync.when(
      data: (columns) {
        return Row(
          children: columns.map((column) {
            final tasksAsync = ref.watch(_kanbanTasksProvider(column.status));
            return Expanded(
              child: Column(
                children: [
                  Container(
                    padding: const EdgeInsets.all(16),
                    decoration: BoxDecoration(
                      color: column.status.getColor().withOpacity(0.1),
                      borderRadius: const BorderRadius.vertical(top: Radius.circular(16)),
                    ),
                    child: Row(
                      children: [
                        Expanded(
                          child: Text(
                            column.name,
                            style: TextStyle(
                              fontSize: 16,
                              fontWeight: FontWeight.bold,
                              color: column.status.getColor(),
                            ),
                          ),
                        ),
                        if (column.wipLimit != null)
                          Container(
                            padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
                            decoration: BoxDecoration(
                              color: AppColors.primary.withOpacity(0.2),
                              borderRadius: BorderRadius.circular(12),
                            ),
                            child: Text(
                              '${tasksAsync.when(data: (t) => t.length, loading: () => 0, error: (_,__) => 0)}/${column.wipLimit}',
                              style: TextStyle(fontSize: 12, fontWeight: FontWeight.bold, color: AppColors.primary),
                            ),
                          ),
                      ],
                    ),
                  ),
                  Expanded(
                    child: tasksAsync.when(
                      data: (tasks) {
                        return DragTarget<Task>(
                          onAccept: (task) => _moveTaskToColumn(task, column.status),
                          builder: (context, candidateData, rejectedData) {
                            return ListView.builder(
                              padding: const EdgeInsets.all(8),
                              itemCount: tasks.length,
                              itemBuilder: (context, index) {
                                final task = tasks[index];
                                return Draggable<Task>(
                                  data: task,
                                  feedback: Material(
                                    child: TaskCard(task: task, onTap: () {}),
                                  ),
                                  childWhenDragging: Opacity(
                                    opacity: 0.5,
                                    child: TaskCard(task: task, onTap: () {}),
                                  ),
                                  child: TaskCard(
                                    task: task,
                                    onTap: () => context.push('/tasks/${task.id}'),
                                  ),
                                );
                              },
                            ),
                          );
                        },
                        loading: () => const Center(child: CircularProgressIndicator()),
                        error: (error, _) => Center(child: Text('Erreur: $error')),
                      ),
                    ),
                  ],
                ),
              );
            }).toList(),
          ),
        );
      },
      loading: () => const Center(child: CircularProgressIndicator()),
      error: (error, _) => Center(child: Text('Erreur: $error')),
    );
  }

  Widget _buildStatsTab(AsyncValue<Map<String, dynamic>> statsAsync) {
    return statsAsync.when(
      data: (stats) => SingleChildScrollView(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text('Statistiques', style: Theme.of(context).textTheme.headlineMedium?.copyWith(fontWeight: FontWeight.bold)),
            const SizedBox(height: 16),
            GridView.count(
              crossAxisCount: 2,
              shrinkWrap: true,
              physics: const NeverScrollableScrollPhysics(),
              crossAxisSpacing: 16,
              mainAxisSpacing: 16,
              childAspectRatio: 1.5,
              children: [
                _buildStatCard('Total', stats['totalTasks']?.toString() ?? '0', Icons.task_alt_rounded, AppColors.primary),
                _buildStatCard('Terminées', stats['completedTasks']?.toString() ?? '0', Icons.check_circle_rounded, Colors.green),
                _buildStatCard('En cours', stats['inProgressTasks']?.toString() ?? '0', Icons.play_circle_rounded, Colors.orange),
                _buildStatCard('En retard', stats['overdueTasks']?.toString() ?? '0', Icons.warning_rounded, Colors.red),
                _buildStatCard('Bloquées', stats['blockedTasks']?.toString() ?? '0', Icons.block_rounded, Colors.red),
                _buildStatCard('Backlog', stats['backlogTasks']?.toString() ?? '0', Icons.archive_rounded, Colors.grey),
                _buildStatCard('Cette semaine', stats['thisWeekCompleted']?.toString() ?? '0', Icons.calendar_today_rounded, AppColors.primary),
                _buildStatCard('Temps total', '${stats['totalHours'] ?? 0}h', Icons.timer_rounded, Colors.purple),
              ],
            ),
            const SizedBox(height: 24),
            Text('Par statut', style: Theme.of(context).textTheme.titleLarge?.copyWith(fontWeight: FontWeight.bold)),
            const SizedBox(height: 12),
            ...TaskStatus.values.map((status) {
              final count = (stats['byStatus'] as Map<String, dynamic>?)?[status.name] ?? 0;
              return _buildStatusBar(status.displayName, count, status.getColor());
            }).toList(),
          ),
        ),
      ),
      loading: () => const Center(child: CircularProgressIndicator()),
      error: (error, _) => Center(child: Text('Erreur: $error')),
    );
  }

  Widget _buildStatCard(String label, String value, IconData icon, Color color) {
    return Card(
      color: AppColors.cardDark,
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                Container(
                  padding: const EdgeInsets.all(8),
                  decoration: BoxDecoration(
                    color: color.withOpacity(0.2),
                    borderRadius: BorderRadius.circular(12),
                  ),
                  child: Icon(icon, color: color, size: 24),
                ),
                const Spacer(),
                Text(
                  value,
                  style: Theme.of(context).textTheme.headlineMedium?.copyWith(
                    fontWeight: FontWeight.bold,
                    color: color,
                  ),
                ),
              ],
            ),
            const SizedBox(height: 8),
            Text(label, style: Theme.of(context).textTheme.bodySmall?.copyWith(color: AppColors.surface.withOpacity(0.7))),
          ],
        ),
      ),
    );
  }

  Widget _buildStatusBar(String label, int count, Color color) {
    return Padding(
      padding: const EdgeInsets.only(bottom: 8),
      child: Row(
        children: [
          Container(
            width: 12,
            height: 12,
            decoration: BoxDecoration(color: color, borderRadius: BorderRadius.circular(2)),
          ),
          const SizedBox(width: 8),
          Expanded(child: Text(label, style: TextStyle(color: AppColors.surface))),
          Text('$count', style: TextStyle(fontWeight: FontWeight.bold, color: color)),
        ],
      ),
    );
  }

  Widget _buildEmptyState({required String message, VoidCallback? action}) {
    return Center(
      child: Column(
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          Icon(Icons.task_alt_rounded, size: 64, color: AppColors.surface.withOpacity(0.5)),
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

  void _moveTaskToColumn(Task task, TaskStatus newStatus) {
    ref.read(tasks_service.TasksServiceProvider).updateTaskStatus(task.id, newStatus);
    ref.invalidate(_kanbanProvider);
    ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text('Tâche déplacée vers ${newStatus.displayName}')));
  }

  Future<void> _createTask() async {
    // TODO: Implement create task dialog
    context.push('/tasks/create');
  }
}

// Providers
final _tasksProvider = FutureProvider.family<List<Task>, (TaskStatus?, TaskPriority?, TaskType?)>((ref, params) async {
  final (status, priority, type) = params;
  final service = ref.watch(tasks_service.TasksServiceProvider);
  return service.getTasks(status: status, priority: priority, type: type);
});

final _kanbanProvider = FutureProvider<List<KanbanColumn>>((ref) async {
  final service = ref.watch(tasks_service.TasksServiceProvider);
  return service.getKanbanColumns();
});

final _kanbanTasksProvider = FutureProvider.family<List<Task>, TaskStatus>((ref, status) async {
  final service = ref.watch(tasks_service.TasksServiceProvider);
  return service.getTasks(status: status);
});

final _statsProvider = FutureProvider<Map<String, dynamic>>((ref) async {
  final service = ref.watch(tasks_service.TasksServiceProvider);
  return service.getTaskStatistics();
});