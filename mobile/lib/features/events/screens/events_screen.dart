import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:intl/intl.dart';
import 'package:go_router/go_router.dart';
import 'package:table_calendar/table_calendar.dart';

import 'package:discipolat_mobile/features/events/models/event_model.dart';
import 'package:discipolat_mobile/features/events/services/events_service.dart';
import 'package:discipolat_mobile/presentation/widgets/glass_theme.dart';
import 'package:discipolat_mobile/features/events/widgets/event_card.dart';
import 'package:discipolat_mobile/features/events/widgets/event_filter_chips.dart';

class EventsScreen extends ConsumerStatefulWidget {
  const EventsScreen({super.key});

  @override
  ConsumerState<EventsScreen> createState() => _EventsScreenState();
}

class _EventsScreenState extends ConsumerState<EventsScreen> with SingleTickerProviderStateMixin {
  EventStatus? _filterStatus;
  EventType? _filterType;
  CalendarFormat _calendarFormat = CalendarFormat.month;
  DateTime _focusedDay = DateTime.now();
  DateTime? _selectedDay;
  late TabController _tabController;

  @override
  void initState() {
    super.initState();
    _tabController = TabController(length: 3, vsync: this);
    _selectedDay = DateTime.now();
  }

  @override
  void dispose() {
    _tabController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final eventsAsync = ref.watch(_eventsProvider((_filterStatus, _filterType)));
    final upcomingAsync = ref.watch(_upcomingEventsProvider);

    return Scaffold(
      backgroundColor: AppColors.surfaceDark,
      appBar: AppBar(
        title: const Text('Événements & Calendrier'),
        backgroundColor: AppColors.cardDark,
        elevation: 0,
        bottom: TabBar(
          controller: _tabController,
          indicatorColor: AppColors.primary,
          labelColor: AppColors.primary,
          unselectedLabelColor: AppColors.surface.withOpacity(0.7),
          tabs: const [
            Tab(text: 'Liste'),
            Tab(text: 'Calendrier'),
            Tab(text: 'Mes événements'),
          ],
        ),
        actions: [
          PopupMenuButton<EventStatus?>(
            icon: const Icon(Icons.filter_list_rounded),
            onSelected: (value) => setState(() => _filterStatus = value),
            itemBuilder: (context) => [
              const PopupMenuItem(value: null, child: Text('Tous les statuts')),
              ...EventStatus.values.map((s) => PopupMenuItem(value: s, child: Text(s.displayName))),
            ],
          ),
          IconButton(
            icon: const Icon(Icons.add_rounded),
            onPressed: () => context.push('/events/create'),
            tooltip: 'Nouvel événement',
          ),
        ],
      ),
      body: TabBarView(
        controller: _tabController,
        children: [
          // List tab
          Column(
            children: [
              EventFilterChips(
                selectedStatus: _filterStatus,
                selectedType: _filterType,
                onStatusChanged: (s) => setState(() => _filterStatus = s),
                onTypeChanged: (t) => setState(() => _filterType = t),
              ),
              Expanded(
                child: RefreshIndicator(
                  onRefresh: () => ref.refresh(_eventsProvider((_filterStatus, _filterType)).future),
                  child: eventsAsync.when(
                    data: (events) {
                      if (events.isEmpty) {
                        return _buildEmptyState();
                      }
                      return ListView.builder(
                        padding: const EdgeInsets.all(16),
                        itemCount: events.length,
                        itemBuilder: (context, index) {
                          final event = events[index];
                          return EventCard(
                            event: event,
                            onTap: () => context.push('/events/${event.id}'),
                            onRegister: event.isRegistered || event.status != EventStatus.published
                                ? null
                                : () => _handleRegister(event),
                            onCheckIn: event.isCheckedIn || !event.hasCheckIn || event.status != EventStatus.live
                                ? null
                                : () => _handleCheckIn(event),
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
          // Calendar tab
          _buildCalendarTab(),
          // My events tab
          _buildMyEventsTab(upcomingAsync),
        ],
      ),
      floatingActionButton: FloatingActionButton.extended(
        onPressed: () => context.push('/events/create'),
        icon: const Icon(Icons.add_rounded),
        label: const Text('Nouvel événement'),
        backgroundColor: AppColors.primary,
      ),
    );
  }

  Widget _buildCalendarTab() {
    final eventsAsync = ref.watch(_eventsProvider((_filterStatus, _filterType)));

    return eventsAsync.when(
      data: (events) {
        final eventMap = <DateTime, List<Event>>{};
        for (final event in events) {
          final date = DateTime(event.startAt.year, event.startAt.month, event.startAt.day);
          eventMap.putIfAbsent(date, () => []).add(event);
        }

        return Column(
          children: [
            TableCalendar<Event>(
              firstDay: DateTime.utc(2020, 1, 1),
              lastDay: DateTime.utc(2030, 12, 31),
              focusedDay: _focusedDay,
              calendarFormat: _calendarFormat,
              selectedDayPredicate: (day) => isSameDay(_selectedDay, day),
              onDaySelected: (selectedDay, focusedDay) {
                setState(() {
                  _selectedDay = selectedDay;
                  _focusedDay = focusedDay;
                });
              },
              onFormatChanged: (format) => setState(() => _calendarFormat = format),
              onPageChanged: (focusedDay) => setState(() => _focusedDay = focusedDay),
              eventLoader: (day) => eventMap[DateTime(day.year, day.month, day.day)] ?? [],
              calendarStyle: CalendarStyle(
                outsideDaysVisible: false,
                weekendTextStyle: TextStyle(color: AppColors.surface.withOpacity(0.7)),
                holidayTextStyle: TextStyle(color: AppColors.surface.withOpacity(0.7)),
                defaultTextStyle: TextStyle(color: AppColors.surface),
                selectedDecoration: BoxDecoration(
                  color: AppColors.primary,
                  shape: BoxShape.circle,
                ),
                todayDecoration: BoxDecoration(
                  color: AppColors.primary.withOpacity(0.5),
                  shape: BoxShape.circle,
                ),
                markerDecoration: BoxDecoration(
                  color: AppColors.primary,
                  shape: BoxShape.circle,
                ),
              ),
              calendarBuilders: CalendarBuilders(
                markerBuilder: (context, day, events) {
                  if (events.isNotEmpty) {
                    return Positioned(
                      bottom: 1,
                      child: Container(
                        padding: const EdgeInsets.all(2),
                        decoration: BoxDecoration(
                          color: events.first.type.getColorHex().toColor(),
                          shape: BoxShape.circle,
                        ),
                        constraints: const BoxConstraints(minWidth: 16, minHeight: 16),
                        child: Text(
                          events.length > 3 ? '3+' : events.length.toString(),
                          style: const TextStyle(fontSize: 8, color: Colors.white, fontWeight: FontWeight.bold),
                          textAlign: TextAlign.center,
                        ),
                      ),
                    );
                  }
                  return null;
                },
              ),
              headerStyle: HeaderStyle(
                formatButtonVisible: false,
                titleCentered: true,
                titleTextStyle: TextStyle(color: AppColors.surface, fontSize: 18, fontWeight: FontWeight.w600),
                leftChevronIcon: Icon(Icons.chevron_left_rounded, color: AppColors.surface),
                rightChevronIcon: Icon(Icons.chevron_right_rounded, color: AppColors.surface),
              ),
            ),
            const SizedBox(height: 8),
            // Events for selected day
            Expanded(
              child: _selectedDay != null
                  ? _buildDayEvents(eventMap[DateTime(_selectedDay!.year, _selectedDay!.month, _selectedDay!.day)] ?? [])
                  : const Center(child: Text('Sélectionnez un jour')),
            ),
          ],
        );
      },
      loading: () => const Center(child: CircularProgressIndicator()),
      error: (error, _) => Center(child: Text('Erreur: $error')),
    );
  }

  Widget _buildDayEvents(List<Event> events) {
    if (events.isEmpty) {
      return Center(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Icon(Icons.event_available_rounded, size: 64, color: AppColors.surface.withOpacity(0.5)),
            const SizedBox(height: 16),
            Text('Aucun événement ce jour', style: TextStyle(color: AppColors.surface.withOpacity(0.7))),
          ],
        ),
      );
    }

    return ListView.builder(
      padding: const EdgeInsets.all(16),
      itemCount: events.length,
      itemBuilder: (context, index) {
        final event = events[index];
        return EventCard(
          event: event,
          onTap: () => context.push('/events/${event.id}'),
          showTime: true,
        );
      },
    );
  }

  Widget _buildMyEventsTab(AsyncValue<List<Event>> upcomingAsync) {
    return upcomingAsync.when(
      data: (events) {
        if (events.isEmpty) {
          return _buildEmptyState(message: 'Vous n\'avez aucun événement à venir');
        }
        return RefreshIndicator(
          onRefresh: () => ref.refresh(_upcomingEventsProvider.future),
          child: ListView.builder(
            padding: const EdgeInsets.all(16),
            itemCount: events.length,
            itemBuilder: (context, index) {
              final event = events[index];
              return EventCard(
                event: event,
                onTap: () => context.push('/events/${event.id}'),
                onRegister: event.isRegistered ? null : () => _handleRegister(event),
                onCheckIn: event.isCheckedIn || !event.hasCheckIn ? null : () => _handleCheckIn(event),
              );
            },
          ),
        );
      },
      loading: () => const Center(child: CircularProgressIndicator()),
      error: (error, _) => Center(child: Text('Erreur: $error')),
    );
  }

  Widget _buildEmptyState({String message = 'Aucun événement trouvé'}) {
    return Center(
      child: Column(
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          Icon(Icons.event_available_rounded, size: 64, color: AppColors.surface.withOpacity(0.5)),
          const SizedBox(height: 16),
          Text(message, style: TextStyle(color: AppColors.surface.withOpacity(0.7))),
          const SizedBox(height: 24),
          FilledButton.icon(
            onPressed: () => context.push('/events/create'),
            icon: const Icon(Icons.add_rounded),
            label: const Text('Créer un événement'),
          ),
        ],
      ),
    );
  }

  Future<void> _handleRegister(Event event) async {
    try {
      await ref.read(eventsServiceProvider).registerForEvent(event.id);
      ref.invalidate(_eventsProvider((_filterStatus, _filterType)));
      ref.invalidate(_upcomingEventsProvider);
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('Inscription confirmée ✓')));
      }
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text('Erreur: $e')));
      }
    }
  }

  Future<void> _handleCheckIn(Event event) async {
    try {
      await ref.read(eventsServiceProvider).checkIn(event.id);
      ref.invalidate(_eventsProvider((_filterStatus, _filterType)));
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('Check-in effectué ✓')));
      }
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text('Erreur: $e')));
      }
    }
  }
}

// Providers
final _eventsProvider = FutureProvider.family<List<Event>, (EventStatus?, EventType?)>((ref, params) async {
  final (status, type) = params;
  final service = ref.watch(eventsServiceProvider);
  return service.getEvents(status: status, type: type);
});

final _upcomingEventsProvider = FutureProvider<List<Event>>((ref) async {
  final service = ref.watch(eventsServiceProvider);
  return service.getUpcomingEvents(limit: 20);
});