import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:video_player/video_player.dart';
import 'package:intl/intl.dart';
import 'package:go_router/go_router.dart';
import 'package:url_launcher/url_launcher.dart';

import 'package:discipolat_mobile/features/events/models/event_model.dart';
import 'package:discipolat_mobile/features/events/services/events_service.dart';
import 'package:discipolat_mobile/presentation/widgets/glass_theme.dart';
import 'package:discipolat_mobile/features/events/widgets/event_chat_overlay.dart';

class EventDetailScreen extends ConsumerStatefulWidget {
  final int eventId;

  const EventDetailScreen({super.key, required this.eventId});

  @override
  ConsumerState<EventDetailScreen> createState() => _EventDetailScreenState();
}

class _EventDetailScreenState extends ConsumerState<EventDetailScreen> {
  VideoPlayerController? _videoController;
  bool _showChat = false;
  bool _isInitialized = false;

  @override
  void initState() {
    super.initState();
    _loadEvent();
  }

  Future<void> _loadEvent() async {
    try {
      final event = await ref.read(eventsServiceProvider).getEvent(widget.eventId);
      
      if (event.streamUrl != null && event.streamUrl!.isNotEmpty) {
        _videoController = VideoPlayerController.networkUrl(Uri.parse(event.streamUrl!))
          ..initialize().then((_) {
            if (mounted) {
              setState(() {
                _isInitialized = true;
              });
              if (event.status == EventStatus.live) {
                _videoController!.play();
              }
            }
          }).catchError((error) {
            print('Video init error: $error');
          });
      }
      
      // Load registration if exists
      ref.read(eventsServiceProvider).getMyRegistration(widget.eventId);
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text('Erreur: $e')),
        );
      }
    }
  }

  @override
  void dispose() {
    _videoController?.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final eventAsync = ref.watch(_eventProvider(widget.eventId));
    final registrationAsync = ref.watch(_registrationProvider(widget.eventId));

    return Scaffold(
      backgroundColor: Colors.black,
      body: eventAsync.when(
        data: (event) => _buildContent(event, registrationAsync),
        loading: () => Center(child: CircularProgressIndicator(color: AppColors.primary)),
        error: (error, _) => Center(
          child: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              Icon(Icons.error_outline_rounded, size: 64, color: Colors.red),
              const SizedBox(height: 16),
              Text('Erreur de chargement', style: Theme.of(context).textTheme.headlineSmall?.copyWith(color: Colors.white)),
              const SizedBox(height: 8),
              Text(error.toString(), style: Theme.of(context).textTheme.bodyMedium?.copyWith(color: AppColors.surface.withOpacity(0.7))),
            ],
          ),
        ),
      ),
    );
  }

  Widget _buildContent(Event event, AsyncValue<EventRegistration?> registrationAsync) {
    final isLive = event.status == EventStatus.live;
    final isUpcoming = event.status == EventStatus.published;
    final canRegister = isUpcoming && !event.isRegistered && event.requiresRegistration;
    final canCheckIn = isLive && event.hasCheckIn && !event.isCheckedIn;
    final isTeam = event.isTeamMember;
    final isOrganizer = event.isOrganizer;

    return Stack(
      children: [
        // Video player or image
        Positioned.fill(
          child: _isInitialized && _videoController != null
              ? AspectRatio(
                  aspectRatio: _videoController!.value.aspectRatio,
                  child: VideoPlayer(_videoController!),
                )
              : event.thumbnailUrl != null
                  ? Image.network(
                      event.thumbnailUrl!,
                      fit: BoxFit.cover,
                      errorBuilder: (_, __, ___) => _buildPlaceholder(event),
                    )
                  : _buildPlaceholder(event),
        ),
        // Gradient overlay
        Positioned.fill(
          child: Container(
            decoration: BoxDecoration(
              gradient: LinearGradient(
                begin: Alignment.topCenter,
                end: Alignment.bottomCenter,
                colors: [
                  Colors.transparent,
                  Colors.black.withOpacity(0.3),
                  Colors.black.withOpacity(0.7),
                ],
                stops: const [0.0, 0.5, 1.0],
              ),
            ),
          ),
        ),
        // Top bar
        Positioned(
          top: 0,
          left: 0,
          right: 0,
          child: SafeArea(
            child: Container(
              padding: const EdgeInsets.all(16),
              child: Row(
                children: [
                  IconButton(
                    icon: const Icon(Icons.arrow_back_rounded, color: Colors.white),
                    onPressed: () => context.pop(),
                  ),
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(
                          event.title,
                          style: const TextStyle(
                            color: Colors.white,
                            fontSize: 18,
                            fontWeight: FontWeight.w600,
                          ),
                          maxLines: 1,
                          overflow: TextOverflow.ellipsis,
                        ),
                        if (event.startAt != null)
                          Text(
                            isLive ? 'En direct' :
                                DateFormat('EEEE dd MMMM yyyy', 'fr_FR').format(event.startAt.toLocal()),
                            style: TextStyle(color: Colors.white70, fontSize: 12),
                          ),
                      ],
                    ),
                  ),
                  // Live indicator
                  if (isLive)
                    Container(
                      padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 6),
                      decoration: BoxDecoration(
                        color: Colors.red,
                        borderRadius: BorderRadius.circular(20),
                      ),
                      child: Row(
                        mainAxisSize: MainAxisSize.min,
                        children: [
                          const SizedBox(
                            width: 8,
                            height: 8,
                            child: CircularProgressIndicator(strokeWidth: 2, color: Colors.white),
                          ),
                          const SizedBox(width: 8),
                          Text('EN DIRECT', style: const TextStyle(color: Colors.white, fontWeight: FontWeight.bold, fontSize: 11)),
                        ],
                      ),
                    ),
                  // Chat toggle
                  IconButton(
                    icon: Icon(
                      _showChat ? Icons.chat_bubble_rounded : Icons.chat_bubble_outline_rounded,
                      color: _showChat ? AppColors.primary : Colors.white,
                    ),
                    onPressed: () => setState(() => _showChat = !_showChat),
                  ),
                  // More actions
                  PopupMenuButton<String>(
                    icon: const Icon(Icons.more_vert_rounded, color: Colors.white),
                    onSelected: (value) => _handleAction(value, event),
                    itemBuilder: (context) => [
                      if (canRegister)
                        const PopupMenuItem(value: 'register', child: Row(children: [Icon(Icons.how_to_reg_rounded), SizedBox(width: 8), Text('S\'inscrire')])),
                      if (canCheckIn)
                        const PopupMenuItem(value: 'checkin', child: Row(children: [Icon(Icons.check_circle_rounded, color: Colors.green), SizedBox(width: 8), Text('Check-in')])),
                      if (isTeam || isOrganizer)
                        const PopupMenuItem(value: 'team', child: Row(children: [Icon(Icons.group_rounded), SizedBox(width: 8), Text('Équipe')])),
                      const PopupMenuItem(value: 'share', child: Row(children: [Icon(Icons.share_rounded), SizedBox(width: 8), Text('Partager')])),
                      const PopupMenuItem(value: 'calendar', child: Row(children: [Icon(Icons.calendar_month_rounded), SizedBox(width: 8), Text('Ajouter au calendrier')])),
                      if (isOrganizer) ...[
                        const PopupMenuItem(value: 'edit', child: Row(children: [Icon(Icons.edit_rounded), SizedBox(width: 8), Text('Modifier')])),
                        const PopupMenuItem(value: 'stats', child: Row(children: [Icon(Icons.analytics_rounded), SizedBox(width: 8), Text('Statistiques')])),
                        const PopupMenuItem(value: 'cancel', child: Row(children: [Icon(Icons.cancel_rounded, color: Colors.red), SizedBox(width: 8), Text('Annuler', style: TextStyle(color: Colors.red))])),
                      ],
                    ],
                  ),
                ],
              ),
            ),
          ),
        ),
        // Chat overlay
        if (_showChat)
          Positioned(
            top: 0,
            right: 0,
            bottom: 0,
            width: MediaQuery.of(context).size.width * 0.85,
            child: EventChatOverlay(
              eventId: widget.eventId,
              onClose: () => setState(() => _showChat = false),
            ),
          ),
        // Bottom info panel
        Positioned(
          bottom: 0,
          left: 0,
          right: 0,
          child: _buildBottomPanel(event, registrationAsync, canRegister, canCheckIn, isTeam, isOrganizer),
        ),
        // Controls overlay
        if (_isInitialized && _videoController != null)
          Positioned(
            bottom: 300,
            left: 0,
            right: 0,
            child: SafeArea(
              child: Container(
                padding: const EdgeInsets.all(16),
                child: VideoProgressIndicator(
                  _videoController!,
                  allowScrubbing: true,
                  colors: VideoProgressColors(
                    playedColor: AppColors.primary,
                    bufferedColor: AppColors.primary.withOpacity(0.3),
                    backgroundColor: Colors.white24,
                  ),
                ),
              ),
            ),
          ),
      ],
    );
  }

  Widget _buildPlaceholder(Event event) {
    final typeColor = event.type.getColorHex().toColor();
    final isLive = event.status == EventStatus.live;
    return Container(
      color: typeColor.withOpacity(0.1),
      child: Center(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Icon(Icons.event_rounded, size: 64, color: typeColor.withOpacity(0.5)),
            const SizedBox(height: 16),
            Text(
              isLive ? 'Connexion au direct...' : 'Chargement...',
              style: TextStyle(color: AppColors.surface.withOpacity(0.7)),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildBottomPanel(Event event, AsyncValue<EventRegistration?> registrationAsync, bool canRegister, bool canCheckIn, bool isTeam, bool isOrganizer) {
    return Container(
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: AppColors.cardDark,
        borderRadius: const BorderRadius.vertical(top: Radius.circular(20)),
        boxShadow: [
          BoxShadow(
            color: Colors.black.withOpacity(0.3),
            blurRadius: 20,
            offset: const Offset(0, -5),
          ),
        ],
      ),
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          // Description
          if (event.description != null) ...[
            Text(
              event.description!,
              style: TextStyle(color: AppColors.surface, fontSize: 14, height: 1.5),
            ),
            const SizedBox(height: 16),
          ],
          // Info rows
          Wrap(
            spacing: 16,
            runSpacing: 12,
            children: [
              if (event.location != null)
                _buildInfoChip(Icons.location_on_rounded, event.location!, AppColors.primary),
              if (event.dressCodeName != null)
                _buildInfoChip(Icons.checkroom_rounded, 'Dress code: ${event.dressCodeName!}', AppColors.accent),
              if (event.maxAttendees != null && event.maxAttendees! > 0)
                _buildInfoChip(Icons.people_rounded, '${event.currentAttendees}/${event.maxAttendees} inscrits', Colors.blue),
            ],
          ),
          const SizedBox(height: 16),
          // Actions
          Row(
            children: [
              if (canRegister)
                Expanded(
                  child: FilledButton.icon(
                    onPressed: () => _handleRegister(event),
                    icon: const Icon(Icons.how_to_reg_rounded),
                    label: const Text('S\'inscrire'),
                    style: FilledButton.styleFrom(
                      backgroundColor: event.type.getColorHex().toColor(),
                      padding: const EdgeInsets.symmetric(vertical: 14),
                      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
                    ),
                  ),
                ),
              if (canRegister && canCheckIn) const SizedBox(width: 12),
              if (canCheckIn)
                Expanded(
                  child: FilledButton.icon(
                    onPressed: () => _handleCheckIn(event),
                    icon: const Icon(Icons.check_circle_rounded),
                    label: const Text('Check-in'),
                    style: FilledButton.styleFrom(
                      backgroundColor: Colors.green,
                      padding: const EdgeInsets.symmetric(vertical: 14),
                      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
                    ),
                  ),
                ),
              if (!canRegister && !canCheckIn)
                Expanded(
                  child: OutlinedButton.icon(
                    onPressed: () => _addToCalendar(event),
                    icon: const Icon(Icons.calendar_month_rounded),
                    label: const Text('Ajouter au calendrier'),
                    style: OutlinedButton.styleFrom(
                      padding: const EdgeInsets.symmetric(vertical: 14),
                      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
                    ),
                  ),
                ),
            ],
          ),
          if (isTeam || isOrganizer) ...[
            const SizedBox(height: 12),
            Row(
              children: [
                if (isOrganizer)
                  Expanded(
                    child: OutlinedButton.icon(
                      onPressed: () => context.push('/events/${event.id}/edit'),
                      icon: const Icon(Icons.edit_rounded),
                      label: const Text('Modifier'),
                    ),
                  ),
                if (isOrganizer) const SizedBox(width: 12),
                Expanded(
                  child: OutlinedButton.icon(
                    onPressed: () => _handleAction('team', event),
                    icon: const Icon(Icons.group_rounded),
                    label: Text(isTeam ? 'Mon équipe' : 'Équipe'),
                  ),
                ),
              ],
            ),
          ],
        ],
      ),
    );
  }

  Widget _buildInfoChip(IconData icon, String label, Color color) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
      decoration: BoxDecoration(
        color: color.withOpacity(0.1),
        borderRadius: BorderRadius.circular(20),
        border: Border.all(color: color.withOpacity(0.3)),
      ),
      child: Row(
        mainAxisSize: MainAxisSize.min,
        children: [
          Icon(icon, size: 16, color: color),
          const SizedBox(width: 6),
          Text(label, style: TextStyle(fontSize: 12, fontWeight: FontWeight.w500, color: color)),
        ],
      ),
    );
  }

  Future<void> _handleRegister(Event event) async {
    try {
      await ref.read(eventsServiceProvider).registerForEvent(event.id);
      ref.invalidate(_registrationProvider(event.id));
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
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('Check-in effectué ✓')));
      }
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text('Erreur: $e')));
      }
    }
  }

  void _handleAction(String action, Event event) {
    switch (action) {
      case 'register':
        _handleRegister(event);
        break;
      case 'checkin':
        _handleCheckIn(event);
        break;
      case 'team':
        context.push('/events/${event.id}/team');
        break;
      case 'share':
        _shareEvent(event);
        break;
      case 'calendar':
        _addToCalendar(event);
        break;
      case 'edit':
        context.push('/events/${event.id}/edit');
        break;
      case 'stats':
        context.push('/events/${event.id}/stats');
        break;
      case 'cancel':
        _cancelEvent(event);
        break;
    }
  }

  Future<void> _cancelEvent(Event event) async {
    final confirm = await showDialog<bool>(
      context: context,
      builder: (context) => AlertDialog(
        backgroundColor: AppColors.cardDark,
        title: const Text('Annuler l\'événement'),
        content: const Text('Cette action est irréversible. Confirmer l\'annulation ?'),
        actions: [
          TextButton(onPressed: () => Navigator.pop(context, false), child: const Text('Annuler')),
          FilledButton(onPressed: () => Navigator.pop(context, true), style: FilledButton.styleFrom(backgroundColor: Colors.red), child: const Text('Annuler l\'événement')),
        ],
      ),
    );
    if (confirm == true && mounted) {
      // TODO: Implement cancel
      ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('Annulation bientôt disponible')));
    }
  }

  void _shareEvent(Event event) {
    // TODO: Implement share
    ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('Partage bientôt disponible')));
  }

  void _addToCalendar(Event event) {
    final url = 'https://calendar.google.com/calendar/render?action=TEMPLATE'
        '&text=${Uri.encodeComponent(event.title)}'
        '&dates=${Uri.encodeComponent('${DateFormat('yyyyMMddTHHmmssZ').format(event.startAt.toUtc())}/${DateFormat('yyyyMMddTHHmmssZ').format(event.endAt.toUtc())}')}'
        '&details=${Uri.encodeComponent(event.description ?? '')}'
        '&location=${Uri.encodeComponent(event.location ?? '')}';
    launchUrl(Uri.parse(url));
    ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('Ouverture du calendrier...')));
  }
}

// Providers
final _eventProvider = FutureProvider.family<Event, int>((ref, id) async {
  final service = ref.watch(eventsServiceProvider);
  return service.getEvent(id);
});

final _registrationProvider = FutureProvider.family<EventRegistration?, int>((ref, eventId) async {
  final service = ref.watch(eventsServiceProvider);
  return service.getMyRegistration(eventId);
});