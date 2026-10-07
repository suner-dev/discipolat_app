import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import 'package:discipolat_mobile/features/tasks/models/task_model.dart';
import 'package:discipolat_mobile/features/tasks/services/tasks_service.dart'
    as tasks_service;
import 'package:discipolat_mobile/presentation/widgets/glass_theme.dart'
    show AppColors;
import 'package:discipolat_mobile/features/tasks/widgets/task_card.dart';
import 'package:discipolat_mobile/features/tasks/widgets/task_filter_chips.dart';

/// Tâches & Projets — branché sur `/api/v1/tasks` (TaskController, V234).
///
/// La route `/tasks` avait été retirée le 2026-09-29 faute d'endpoints
/// serveur ; le backend a depuis publié TaskController sur exactement ce
/// contrat. L'écran est réactivé avec les VUES RÉELLES du serveur :
/// - statistiques = {total, done, inProgress, blocked} (rapport serveur) ;
/// - répartition par statut = GET /reports/by-status (liste [{status,count}]) ;
/// - création et détail via feuilles modales (le serveur n'expose pas de
///   pages dédiées, et les routes /tasks/create et /tasks/:id n'ont jamais
///   existé dans le routeur).
class TasksScreen extends ConsumerStatefulWidget {
  const TasksScreen({super.key});

  @override
  ConsumerState<TasksScreen> createState() => _TasksScreenState();
}

class _TasksScreenState extends ConsumerState<TasksScreen>
    with SingleTickerProviderStateMixin {
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
    final tasksAsync = ref.watch(
        _tasksProvider((_filterStatus, _filterPriority, _filterType)));
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
              ...TaskStatus.values
                  .map((s) => PopupMenuItem(value: s, child: Text(s.displayName))),
            ],
          ),
          PopupMenuButton<TaskPriority?>(
            icon: const Icon(Icons.filter_alt_rounded),
            onSelected: (value) => setState(() => _filterPriority = value),
            itemBuilder: (context) => [
              const PopupMenuItem(value: null, child: Text('Toutes priorités')),
              ...TaskPriority.values.map((p) => PopupMenuItem(
                    value: p,
                    child: Row(
                      children: [
                        Container(
                            width: 10,
                            height: 10,
                            decoration: BoxDecoration(
                                color: p.getColor(), shape: BoxShape.circle)),
                        const SizedBox(width: 8),
                        Text(p.displayName),
                      ],
                    ),
                  )),
            ],
          ),
          PopupMenuButton<TaskType?>(
            icon: const Icon(Icons.category_rounded),
            onSelected: (value) => setState(() => _filterType = value),
            itemBuilder: (context) => [
              const PopupMenuItem(value: null, child: Text('Tous types')),
              ...TaskType.values
                  .map((t) => PopupMenuItem(value: t, child: Text(t.displayName))),
            ],
          ),
          IconButton(
            icon: const Icon(Icons.add_rounded),
            onPressed: _showCreateSheet,
            tooltip: 'Nouvelle tâche',
          ),
        ],
      ),
      body: TabBarView(
        controller: _tabController,
        children: [
          // Onglet Liste
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
                  onRefresh: () => ref.refresh(
                      _tasksProvider((_filterStatus, _filterPriority, _filterType))
                          .future),
                  child: tasksAsync.when(
                    data: (tasks) {
                      if (tasks.isEmpty) {
                        return _buildEmptyState(
                            message: 'Aucune tâche', action: _showCreateSheet);
                      }
                      return ListView.builder(
                        padding: const EdgeInsets.all(16),
                        itemCount: tasks.length,
                        itemBuilder: (context, index) {
                          final task = tasks[index];
                          return TaskCard(
                            task: task,
                            onTap: () => _showTaskDetail(task),
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
          // Onglet Kanban
          _buildKanbanTab(),
          // Onglet Statistiques
          _buildStatsTab(statsAsync),
        ],
      ),
      floatingActionButton: FloatingActionButton.extended(
        onPressed: _showCreateSheet,
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
                      borderRadius:
                          const BorderRadius.vertical(top: Radius.circular(16)),
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
                            padding: const EdgeInsets.symmetric(
                                horizontal: 8, vertical: 4),
                            decoration: BoxDecoration(
                              color: AppColors.primary.withOpacity(0.2),
                              borderRadius: BorderRadius.circular(12),
                            ),
                            child: Text(
                              '${tasksAsync.when(data: (t) => t.length, loading: () => 0, error: (_, __) => 0)}/${column.wipLimit}',
                              style: TextStyle(
                                  fontSize: 12,
                                  fontWeight: FontWeight.bold,
                                  color: AppColors.primary),
                            ),
                          ),
                      ],
                    ),
                  ),
                  Expanded(
                    child: tasksAsync.when(
                      data: (tasks) {
                        return DragTarget<Task>(
                          onAcceptWithDetails: (details) =>
                              _moveTaskToColumn(details.data, column.status),
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
                                    onTap: () => _showTaskDetail(task),
                                  ),
                                );
                              },
                            );
                          },
                        );
                      },
                      loading: () =>
                          const Center(child: CircularProgressIndicator()),
                      error: (error, _) => Center(child: Text('Erreur: $error')),
                    ),
                  ),
                ],
              ),
            );
          }).toList(),
        );
      },
      loading: () => const Center(child: CircularProgressIndicator()),
      error: (error, _) => Center(child: Text('Erreur: $error')),
    );
  }

  Widget _buildStatsTab(AsyncValue<Map<String, dynamic>> statsAsync) {
    final byStatusAsync = ref.watch(_byStatusProvider);

    return statsAsync.when(
      data: (stats) => SingleChildScrollView(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text('Statistiques',
                style: Theme.of(context)
                    .textTheme
                    .headlineMedium
                    ?.copyWith(fontWeight: FontWeight.bold)),
            const SizedBox(height: 16),
            GridView.count(
              crossAxisCount: 2,
              shrinkWrap: true,
              physics: const NeverScrollableScrollPhysics(),
              crossAxisSpacing: 16,
              mainAxisSpacing: 16,
              childAspectRatio: 1.5,
              children: [
                // Clés EXACTES de reportStatistics côté serveur.
                _buildStatCard('Total', '${_asInt(stats['total'])}',
                    Icons.task_alt_rounded, AppColors.primary),
                _buildStatCard('Terminées', '${_asInt(stats['done'])}',
                    Icons.check_circle_rounded, Colors.green),
                _buildStatCard('En cours', '${_asInt(stats['inProgress'])}',
                    Icons.play_circle_rounded, Colors.orange),
                _buildStatCard('Bloquées', '${_asInt(stats['blocked'])}',
                    Icons.block_rounded, Colors.red),
              ],
            ),
            const SizedBox(height: 24),
            Text('Par statut',
                style: Theme.of(context)
                    .textTheme
                    .titleLarge
                    ?.copyWith(fontWeight: FontWeight.bold)),
            const SizedBox(height: 12),
            byStatusAsync.when(
              data: (rows) {
                // Le serveur renvoie déjà les 7 statuts : on les rend dans
                // l'ordre du modèle, pas dans celui du serveur.
                final counts = <TaskStatus, int>{
                  for (final row in rows)
                    TaskStatus.fromWire(row['status']?.toString()):
                        _asInt(row['count']),
                };
                return Column(
                  children: TaskStatus.values
                      .map((status) => _buildStatusBar(
                          status.displayName, counts[status] ?? 0,
                          status.getColor()))
                      .toList(),
                );
              },
              loading: () =>
                  const Center(child: CircularProgressIndicator()),
              error: (error, _) => Center(child: Text('Erreur: $error')),
            ),
            const SizedBox(height: 24),
            Text('Par assigné',
                style: Theme.of(context)
                    .textTheme
                    .titleLarge
                    ?.copyWith(fontWeight: FontWeight.bold)),
            const SizedBox(height: 12),
            ref.watch(_byAssigneeProvider).when(
              data: (rows) => rows.isEmpty
                  ? Text('Aucune tâche assignée',
                      style:
                          TextStyle(color: AppColors.surface.withOpacity(0.7)))
                  : Column(
                      children: rows
                          .map((row) => _buildStatusBar(
                              (row['assignedToName'] ?? '—').toString(),
                              _asInt(row['count']),
                              AppColors.primary))
                          .toList(),
                    ),
              loading: () =>
                  const Center(child: CircularProgressIndicator()),
              error: (error, _) => Center(child: Text('Erreur: $error')),
            ),
          ],
        ),
      ),
      loading: () => const Center(child: CircularProgressIndicator()),
      error: (error, _) => Center(child: Text('Erreur: $error')),
    );
  }

  static int _asInt(Object? v) => v is num ? v.toInt() : int.tryParse('${v ?? ''}') ?? 0;

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
            Text(label,
                style: Theme.of(context)
                    .textTheme
                    .bodySmall
                    ?.copyWith(color: AppColors.surface.withOpacity(0.7))),
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
            decoration:
                BoxDecoration(color: color, borderRadius: BorderRadius.circular(2)),
          ),
          const SizedBox(width: 8),
          Expanded(child: Text(label, style: TextStyle(color: AppColors.surface))),
          Text('$count',
              style: TextStyle(fontWeight: FontWeight.bold, color: color)),
        ],
      ),
    );
  }

  Widget _buildEmptyState({required String message, VoidCallback? action}) {
    return Center(
      child: Column(
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          Icon(Icons.task_alt_rounded,
              size: 64, color: AppColors.surface.withOpacity(0.5)),
          const SizedBox(height: 16),
          Text(message, style: TextStyle(color: AppColors.surface.withOpacity(0.7))),
          if (action != null) ...[
            const SizedBox(height: 24),
            FilledButton.icon(
                onPressed: action,
                icon: const Icon(Icons.add_rounded),
                label: const Text('Créer')),
          ],
        ],
      ),
    );
  }

  /// Glisser-déposer kanban : POST /tasks/{id}/reorder persiste désormais le
  /// statut ET le rang côté serveur (corrigé en V239).
  Future<void> _moveTaskToColumn(Task task, TaskStatus newStatus) async {
    if (task.status == newStatus) return;
    try {
      await ref
          .read(tasks_service.tasksServiceProvider)
          .reorderTask(task.id, status: newStatus);
      _refreshAll();
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
            SnackBar(content: Text('Tâche déplacée vers ${newStatus.displayName}')));
      }
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context)
            .showSnackBar(SnackBar(content: Text('Échec du déplacement: $e')));
      }
    }
  }

  void _showCreateSheet() {
    showModalBottomSheet<void>(
      context: context,
      isScrollControlled: true,
      backgroundColor: AppColors.cardDark,
      shape: const RoundedRectangleBorder(
        borderRadius: BorderRadius.vertical(top: Radius.circular(24)),
      ),
      builder: (sheetContext) => Padding(
        padding: EdgeInsets.only(
            bottom: MediaQuery.of(sheetContext).viewInsets.bottom),
        child: const _TaskCreateSheet(),
      ),
    );
  }

  void _showTaskDetail(Task task) {
    showModalBottomSheet<void>(
      context: context,
      isScrollControlled: true,
      backgroundColor: AppColors.cardDark,
      shape: const RoundedRectangleBorder(
        borderRadius: BorderRadius.vertical(top: Radius.circular(24)),
      ),
      builder: (_) => Consumer(
        builder: (context, ref, _) => _TaskDetailSheet(
          task: ref.watch(_taskDetailProvider(task.id)).value ?? task,
          onArchived: () {
            Navigator.pop(context);
            _refreshAll();
          },
        ),
      ),
    );
  }

  void _refreshAll() {
    ref.invalidate(_tasksProvider);
    ref.invalidate(_kanbanProvider);
    ref.invalidate(_kanbanTasksProvider);
    ref.invalidate(_statsProvider);
    ref.invalidate(_byStatusProvider);
    ref.invalidate(_byAssigneeProvider);
    ref.invalidate(_taskDetailProvider);
  }
}

// ── Feuille de création ─────────────────────────────────────────────────────

class _TaskCreateSheet extends ConsumerStatefulWidget {
  const _TaskCreateSheet();

  @override
  ConsumerState<_TaskCreateSheet> createState() => _TaskCreateSheetState();
}

class _TaskCreateSheetState extends ConsumerState<_TaskCreateSheet> {
  final _formKey = GlobalKey<FormState>();
  final _titleController = TextEditingController();
  final _descriptionController = TextEditingController();
  TaskType _type = TaskType.task;
  TaskPriority _priority = TaskPriority.medium;
  DateTime? _dueDate;
  bool _submitting = false;

  @override
  void dispose() {
    _titleController.dispose();
    _descriptionController.dispose();
    super.dispose();
  }

  Future<void> _pickDueDate() async {
    final now = DateTime.now();
    final picked = await showDatePicker(
      context: context,
      initialDate: _dueDate ?? now,
      firstDate: now.subtract(const Duration(days: 365)),
      lastDate: now.add(const Duration(days: 365 * 3)),
    );
    if (picked != null) setState(() => _dueDate = picked);
  }

  Future<void> _submit() async {
    if (!(_formKey.currentState?.validate() ?? false)) return;
    setState(() => _submitting = true);
    try {
      await ref.read(tasks_service.tasksServiceProvider).createTask(
            title: _titleController.text.trim(),
            description: _descriptionController.text.trim().isEmpty
                ? null
                : _descriptionController.text.trim(),
            type: _type,
            priority: _priority,
            dueDate: _dueDate,
          );
      if (!mounted) return;
      Navigator.pop(context);
      // Le State parent peut avoir été démonté pendant le pop.
      _refRefresh();
    } catch (e) {
      if (mounted) {
        setState(() => _submitting = false);
        ScaffoldMessenger.of(context)
            .showSnackBar(SnackBar(content: Text('Échec de la création: $e')));
      }
    }
  }

  void _refRefresh() {
    // Passe par le ConsumerState: invalide les providers via le conteneur
    // global du widget parent (le sheet vit dans son propre élément).
    ref.invalidate(_tasksProvider);
    ref.invalidate(_kanbanProvider);
    ref.invalidate(_kanbanTasksProvider);
    ref.invalidate(_statsProvider);
    ref.invalidate(_byStatusProvider);
    ref.invalidate(_byAssigneeProvider);
  }

  @override
  Widget build(BuildContext context) {
    return SafeArea(
      child: Padding(
        padding: const EdgeInsets.all(20),
        child: Form(
          key: _formKey,
          child: Column(
            mainAxisSize: MainAxisSize.min,
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text('Nouvelle tâche',
                  style: Theme.of(context)
                      .textTheme
                      .titleLarge
                      ?.copyWith(fontWeight: FontWeight.bold)),
              const SizedBox(height: 16),
              TextFormField(
                controller: _titleController,
                decoration: const InputDecoration(labelText: 'Titre *'),
                textInputAction: TextInputAction.next,
                validator: (v) =>
                    (v == null || v.trim().isEmpty) ? 'Titre requis' : null,
              ),
              const SizedBox(height: 12),
              TextFormField(
                controller: _descriptionController,
                decoration: const InputDecoration(labelText: 'Description'),
                maxLines: 3,
              ),
              const SizedBox(height: 12),
              Row(
                children: [
                  Expanded(
                    child: DropdownButtonFormField<TaskType>(
                      initialValue: _type,
                      decoration: const InputDecoration(labelText: 'Type'),
                      items: TaskType.values
                          .map((t) => DropdownMenuItem(
                              value: t, child: Text(t.displayName)))
                          .toList(),
                      onChanged: (v) => setState(() => _type = v ?? TaskType.task),
                    ),
                  ),
                  const SizedBox(width: 12),
                  Expanded(
                    child: DropdownButtonFormField<TaskPriority>(
                      initialValue: _priority,
                      decoration: const InputDecoration(labelText: 'Priorité'),
                      items: TaskPriority.values
                          .map((p) => DropdownMenuItem(
                              value: p, child: Text(p.displayName)))
                          .toList(),
                      onChanged: (v) =>
                          setState(() => _priority = v ?? TaskPriority.medium),
                    ),
                  ),
                ],
              ),
              const SizedBox(height: 12),
              OutlinedButton.icon(
                onPressed: _pickDueDate,
                icon: const Icon(Icons.schedule_rounded, size: 18),
                label: Text(_dueDate == null
                    ? 'Aucune échéance'
                    : 'Échéance: ${_dueDate!.day}/${_dueDate!.month}/${_dueDate!.year}'),
              ),
              const SizedBox(height: 20),
              SizedBox(
                width: double.infinity,
                child: FilledButton(
                  onPressed: _submitting ? null : _submit,
                  child: _submitting
                      ? const SizedBox(
                          height: 20,
                          width: 20,
                          child: CircularProgressIndicator(strokeWidth: 2))
                      : const Text('Créer la tâche'),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}

// ── Feuille de détail ───────────────────────────────────────────────────────

class _TaskDetailSheet extends ConsumerStatefulWidget {
  const _TaskDetailSheet({required this.task, required this.onArchived});

  final Task task;
  final VoidCallback onArchived;

  @override
  ConsumerState<_TaskDetailSheet> createState() => _TaskDetailSheetState();
}

class _TaskDetailSheetState extends ConsumerState<_TaskDetailSheet> {
  bool _busy = false;

  Future<void> _changeStatus(TaskStatus status) async {
    setState(() => _busy = true);
    try {
      await ref
          .read(tasks_service.tasksServiceProvider)
          .updateTaskStatus(widget.task.id, status);
      ref.invalidate(_taskDetailProvider(widget.task.id));
      ref.invalidate(_tasksProvider);
      ref.invalidate(_kanbanProvider);
      ref.invalidate(_kanbanTasksProvider);
      ref.invalidate(_statsProvider);
      ref.invalidate(_byStatusProvider);
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context)
            .showSnackBar(SnackBar(content: Text('Échec: $e')));
      }
    } finally {
      if (mounted) setState(() => _busy = false);
    }
  }

  Future<void> _archive() async {
    final confirmed = await showDialog<bool>(
      context: context,
      builder: (dialogContext) => AlertDialog(
        title: const Text('Archiver cette tâche ?'),
        content: const Text(
            'La tâche passera au statut « Annulé » (le serveur archive, il ne supprime pas).'),
        actions: [
          TextButton(
              onPressed: () => Navigator.pop(dialogContext, false),
              child: const Text('Annuler')),
          FilledButton(
              onPressed: () => Navigator.pop(dialogContext, true),
              child: const Text('Archiver')),
        ],
      ),
    );
    if (confirmed != true) return;
    setState(() => _busy = true);
    try {
      await ref
          .read(tasks_service.tasksServiceProvider)
          .deleteTask(widget.task.id);
      widget.onArchived();
    } catch (e) {
      if (mounted) {
        setState(() => _busy = false);
        ScaffoldMessenger.of(context)
            .showSnackBar(SnackBar(content: Text('Échec: $e')));
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    final task = widget.task;
    final commentsAsync = ref.watch(_commentsProvider(task.id));

    return SafeArea(
      child: SingleChildScrollView(
        padding: const EdgeInsets.all(20),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                Expanded(
                  child: Text(task.title,
                      style: Theme.of(context)
                          .textTheme
                          .titleLarge
                          ?.copyWith(fontWeight: FontWeight.bold)),
                ),
                Container(
                  padding:
                      const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
                  decoration: BoxDecoration(
                    color: task.status.getColor().withOpacity(0.2),
                    borderRadius: BorderRadius.circular(20),
                  ),
                  child: Text(task.status.displayName,
                      style: TextStyle(
                          fontSize: 12,
                          fontWeight: FontWeight.bold,
                          color: task.status.getColor())),
                ),
              ],
            ),
            const SizedBox(height: 8),
            Text('${task.type.displayName} · priorité ${task.priority.displayName}',
                style: TextStyle(color: AppColors.surface.withOpacity(0.7))),
            if (task.dueDate != null) ...[
              const SizedBox(height: 4),
              Text(
                  task.isOverdue
                      ? 'Échéance dépassée: ${task.dueDate!.day}/${task.dueDate!.month}/${task.dueDate!.year}'
                      : 'Échéance: ${task.dueDate!.day}/${task.dueDate!.month}/${task.dueDate!.year}',
                  style: TextStyle(
                      color: task.isOverdue ? Colors.red : AppColors.primary)),
            ],
            if (task.description != null && task.description!.isNotEmpty) ...[
              const SizedBox(height: 12),
              Text(task.description!,
                  style: const TextStyle(height: 1.4)),
            ],
            const SizedBox(height: 16),
            Text('Changer le statut',
                style: Theme.of(context)
                    .textTheme
                    .titleMedium
                    ?.copyWith(fontWeight: FontWeight.bold)),
            const SizedBox(height: 8),
            Wrap(
              spacing: 8,
              runSpacing: 8,
              children: TaskStatus.values.map((s) {
                final selected = s == task.status;
                return ChoiceChip(
                  label: Text(s.displayName),
                  selected: selected,
                  onSelected: (selected || _busy)
                      ? null
                      : (_) => _changeStatus(s),
                );
              }).toList(),
            ),
            const SizedBox(height: 16),
            Text('Commentaires',
                style: Theme.of(context)
                    .textTheme
                    .titleMedium
                    ?.copyWith(fontWeight: FontWeight.bold)),
            const SizedBox(height: 8),
            commentsAsync.when(
              data: (comments) => comments.isEmpty
                  ? Text('Aucun commentaire',
                      style: TextStyle(
                          color: AppColors.surface.withOpacity(0.7)))
                  : Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: comments
                          .map((c) => Padding(
                                padding: const EdgeInsets.only(bottom: 8),
                                child: Text(
                                    '${c.authorName ?? '—'}: ${c.content}',
                                    style: const TextStyle(fontSize: 14)),
                              ))
                          .toList(),
                    ),
              loading: () => const SizedBox(
                  height: 24,
                  child: Center(
                      child: CircularProgressIndicator(strokeWidth: 2))),
              error: (error, _) => Text('Erreur: $error',
                  style: const TextStyle(fontSize: 13)),
            ),
            const SizedBox(height: 16),
            Row(
              children: [
                Expanded(
                  child: OutlinedButton.icon(
                    onPressed: _busy ? null : _archive,
                    icon: const Icon(Icons.archive_rounded, size: 18),
                    label: const Text('Archiver'),
                    style: OutlinedButton.styleFrom(
                        foregroundColor: Colors.redAccent,
                        side: const BorderSide(color: Colors.redAccent)),
                  ),
                ),
              ],
            ),
          ],
        ),
      ),
    );
  }
}

// ── Providers ───────────────────────────────────────────────────────────────

final _tasksProvider = FutureProvider.family<List<Task>,
    (TaskStatus?, TaskPriority?, TaskType?)>((ref, params) async {
  final (status, priority, type) = params;
  final service = ref.watch(tasks_service.tasksServiceProvider);
  return service.getTasks(status: status, priority: priority, type: type);
});

final _kanbanProvider = FutureProvider<List<KanbanColumn>>((ref) async {
  final service = ref.watch(tasks_service.tasksServiceProvider);
  return service.getKanbanColumns();
});

final _kanbanTasksProvider =
    FutureProvider.family<List<Task>, TaskStatus>((ref, status) async {
  final service = ref.watch(tasks_service.tasksServiceProvider);
  return service.getTasks(status: status, size: 100);
});

final _statsProvider = FutureProvider<Map<String, dynamic>>((ref) async {
  final service = ref.watch(tasks_service.tasksServiceProvider);
  return service.getTaskStatistics();
});

final _byStatusProvider =
    FutureProvider<List<Map<String, dynamic>>>((ref) async {
  final service = ref.watch(tasks_service.tasksServiceProvider);
  return service.getTasksByStatus();
});

final _byAssigneeProvider =
    FutureProvider<List<Map<String, dynamic>>>((ref) async {
  final service = ref.watch(tasks_service.tasksServiceProvider);
  return service.getTasksByAssignee();
});

/// Rechargement de la tâche affichée dans la feuille de détail après
/// changement de statut (vue taskView fraîche côté serveur).
final _taskDetailProvider = FutureProvider.family<Task?, int>((ref, id) async {
  final service = ref.watch(tasks_service.tasksServiceProvider);
  try {
    return await service.getTask(id);
  } catch (e) {
    return null;
  }
});

final _commentsProvider =
    FutureProvider.family<List<TaskComment>, int>((ref, taskId) async {
  final service = ref.watch(tasks_service.tasksServiceProvider);
  return service.getComments(taskId);
});
