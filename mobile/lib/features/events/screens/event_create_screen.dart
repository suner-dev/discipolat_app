import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:image_picker/image_picker.dart';
import 'package:intl/intl.dart';

import 'package:discipolat_mobile/features/events/models/event_model.dart';
import 'package:discipolat_mobile/features/events/services/events_service.dart';
import 'package:discipolat_mobile/presentation/widgets/glass_theme.dart';

class EventCreateScreen extends ConsumerStatefulWidget {
  final Event? event;

  const EventCreateScreen({super.key, this.event});

  @override
  ConsumerState<EventCreateScreen> createState() => _EventCreateScreenState();
}

class _EventCreateScreenState extends ConsumerState<EventCreateScreen> {
  final _formKey = GlobalKey<FormState>();
  final _titleController = TextEditingController();
  final _descriptionController = TextEditingController();
  final _locationController = TextEditingController();
  final _streamUrlController = TextEditingController();
  final _tagsController = TextEditingController();
  DateTime? _startAt;
  DateTime? _endAt;
  EventType _selectedType = EventType.culte;
  EventStatus _selectedStatus = EventStatus.draft;
  String? _thumbnailUrl;
  bool _requiresRegistration = false;
  int? _maxAttendees;
  bool _hasCheckIn = false;
  bool _hasGeofencing = false;
  bool _hasFaceCheckIn = false;
  bool _isPublic = true;
  bool _isLoading = false;
  int? _selectedDressCodeId;

  @override
  void initState() {
    super.initState();
    if (widget.event != null) {
      _titleController.text = widget.event!.title;
      _descriptionController.text = widget.event!.description ?? '';
      _locationController.text = widget.event!.location ?? '';
      _streamUrlController.text = widget.event!.streamUrl ?? '';
      _tagsController.text = widget.event!.tags?.join(', ') ?? '';
      _startAt = widget.event!.startAt;
      _endAt = widget.event!.endAt;
      _selectedType = widget.event!.type;
      _selectedStatus = widget.event!.status;
      _thumbnailUrl = widget.event!.thumbnailUrl;
      _requiresRegistration = widget.event!.requiresRegistration;
      _maxAttendees = widget.event!.maxAttendees;
      _hasCheckIn = widget.event!.hasCheckIn;
      _hasGeofencing = widget.event!.hasGeofencing;
      _hasFaceCheckIn = widget.event!.hasFaceCheckIn;
      _isPublic = widget.event!.isPublic;
      _selectedDressCodeId = widget.event!.dressCodeId != null ? int.tryParse(widget.event!.dressCodeId!) : null;
    } else {
      _startAt = DateTime.now().add(const Duration(days: 1));
      _endAt = DateTime.now().add(const Duration(days: 1, hours: 2));
    }
  }

  @override
  void dispose() {
    _titleController.dispose();
    _descriptionController.dispose();
    _locationController.dispose();
    _streamUrlController.dispose();
    _tagsController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final isEditing = widget.event != null;

    return Scaffold(
      backgroundColor: AppColors.surfaceDark,
      appBar: AppBar(
        title: Text(isEditing ? 'Modifier l\'événement' : 'Nouvel événement'),
        backgroundColor: AppColors.cardDark,
        elevation: 0,
      ),
      body: Form(
        key: _formKey,
        child: ListView(
          padding: const EdgeInsets.all(16),
          children: [
            // Thumbnail
            _buildThumbnailPicker(),
            const SizedBox(height: 24),

            // Title
            TextFormField(
              controller: _titleController,
              decoration: InputDecoration(
                labelText: 'Titre *',
                hintText: 'Titre de votre événement',
                border: OutlineInputBorder(borderRadius: BorderRadius.circular(12)),
                filled: true,
                fillColor: AppColors.cardDark,
              ),
              validator: (value) {
                if (value == null || value.trim().isEmpty) {
                  return 'Le titre est requis';
                }
                return null;
              },
            ),
            const SizedBox(height: 16),

            // Description
            TextFormField(
              controller: _descriptionController,
              decoration: InputDecoration(
                labelText: 'Description',
                hintText: 'Description de l\'événement',
                border: OutlineInputBorder(borderRadius: BorderRadius.circular(12)),
                filled: true,
                fillColor: AppColors.cardDark,
              ),
              maxLines: 4,
            ),
            const SizedBox(height: 16),

            // Type & Status
            Row(
              children: [
                Expanded(
                  child: _buildTypeSelector(),
                ),
                const SizedBox(width: 12),
                Expanded(
                  child: _buildStatusSelector(),
                ),
              ],
            ),
            const SizedBox(height: 16),

            // Dates
            Row(
              children: [
                Expanded(child: _buildDateTimePicker('Début *', _startAt, (date) => setState(() => _startAt = date), true)),
                const SizedBox(width: 12),
                Expanded(child: _buildDateTimePicker('Fin *', _endAt, (date) => setState(() => _endAt = date), false)),
              ],
            ),
            const SizedBox(height: 16),

            // Location
            TextFormField(
              controller: _locationController,
              decoration: InputDecoration(
                labelText: 'Lieu',
                hintText: 'Adresse ou nom du lieu',
                border: OutlineInputBorder(borderRadius: BorderRadius.circular(12)),
                filled: true,
                fillColor: AppColors.cardDark,
                prefixIcon: const Icon(Icons.location_on_rounded),
              ),
            ),
            const SizedBox(height: 16),

            // Stream URL
            TextFormField(
              controller: _streamUrlController,
              decoration: InputDecoration(
                labelText: 'URL du flux (HLS/DASH/RTMP)',
                hintText: 'https://example.com/stream.m3u8',
                border: OutlineInputBorder(borderRadius: BorderRadius.circular(12)),
                filled: true,
                fillColor: AppColors.cardDark,
                prefixIcon: const Icon(Icons.link_rounded),
              ),
            ),
            const SizedBox(height: 16),

            // Max attendees & registration
            Row(
              children: [
                Expanded(
                  child: TextFormField(
                    initialValue: _maxAttendees?.toString() ?? '',
                    decoration: InputDecoration(
                      labelText: 'Max participants',
                      hintText: 'Laisser vide = illimité',
                      border: OutlineInputBorder(borderRadius: BorderRadius.circular(12)),
                      filled: true,
                      fillColor: AppColors.cardDark,
                      prefixIcon: const Icon(Icons.people_rounded),
                    ),
                    keyboardType: TextInputType.number,
                    onChanged: (value) => _maxAttendees = value.isEmpty ? null : int.tryParse(value),
                  ),
                ),
                const SizedBox(width: 12),
                Expanded(
                  child: SwitchListTile(
                    title: const Text('Inscription requise'),
                    value: _requiresRegistration,
                    onChanged: (value) => setState(() => _requiresRegistration = value),
                    activeColor: AppColors.primary,
                    contentPadding: EdgeInsets.zero,
                    dense: true,
                  ),
                ),
              ],
            ),
            const SizedBox(height: 8),

            // Features toggles
            _buildFeatureToggles(),
            const SizedBox(height: 16),

            // Tags
            TextFormField(
              controller: _tagsController,
              decoration: InputDecoration(
                labelText: 'Tags (séparés par des virgules)',
                hintText: 'culte, jeunesse, worship',
                border: OutlineInputBorder(borderRadius: BorderRadius.circular(12)),
                filled: true,
                fillColor: AppColors.cardDark,
                prefixIcon: const Icon(Icons.tag_rounded),
              ),
            ),
            const SizedBox(height: 24),

            // Info box
            Container(
              padding: const EdgeInsets.all(16),
              decoration: BoxDecoration(
                color: AppColors.primary.withOpacity(0.1),
                borderRadius: BorderRadius.circular(12),
                border: Border.all(color: AppColors.primary.withOpacity(0.3)),
              ),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Row(
                    children: [
                      Icon(Icons.info_outline_rounded, color: AppColors.primary, size: 20),
                      const SizedBox(width: 8),
                      Text(
                        'Formats de flux supportés',
                        style: TextStyle(fontWeight: FontWeight.w600, color: AppColors.primary),
                      ),
                    ],
                  ),
                  const SizedBox(height: 8),
                  Text(
                    '• HLS (.m3u8) - Recommandé pour le direct\n'
                    '• DASH (.mpd) - Streaming adaptatif\n'
                    '• RTMP - Pour les encodeurs (OBS, etc.)\n'
                    '• MP4 - Pour les replays uniquement',
                    style: Theme.of(context).textTheme.bodySmall?.copyWith(color: AppColors.surface.withOpacity(0.7)),
                  ),
                ],
              ),
            ),
            const SizedBox(height: 32),

            // Save button
            FilledButton.icon(
              onPressed: _isLoading ? null : _saveEvent,
              icon: _isLoading
                  ? const SizedBox(width: 20, height: 20, child: CircularProgressIndicator(strokeWidth: 2, color: Colors.white))
                  : Icon(isEditing ? Icons.save_rounded : Icons.add_rounded),
              label: Text(isEditing ? 'Enregistrer' : 'Créer l\'événement'),
              style: FilledButton.styleFrom(
                backgroundColor: AppColors.primary,
                foregroundColor: Colors.white,
                padding: const EdgeInsets.symmetric(vertical: 16),
                shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
              ),
            ),
            const SizedBox(height: 16),

            if (isEditing)
              OutlinedButton.icon(
                onPressed: _isLoading ? null : _deleteEvent,
                icon: const Icon(Icons.delete_rounded),
                label: const Text('Supprimer'),
                style: OutlinedButton.styleFrom(
                  foregroundColor: Colors.red,
                  side: const BorderSide(color: Colors.red),
                  padding: const EdgeInsets.symmetric(vertical: 16),
                  shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
                ),
              ),
          ],
        ),
      ),
    );
  }

  Widget _buildTypeSelector() {
    return DropdownButtonFormField<EventType>(
      value: _selectedType,
      decoration: InputDecoration(
        labelText: 'Type *',
        border: OutlineInputBorder(borderRadius: BorderRadius.circular(12)),
        filled: true,
        fillColor: AppColors.cardDark,
      ),
      items: EventType.values.map((type) {
        return DropdownMenuItem(
          value: type,
          child: Row(
            children: [
              Container(width: 12, height: 12, decoration: BoxDecoration(color: type.getColorHex().toColor(), shape: BoxShape.circle)),
              const SizedBox(width: 8),
              Text(type.displayName),
            ],
          ),
        );
      }).toList(),
      onChanged: (value) => setState(() => _selectedType = value!),
      validator: (value) => value == null ? 'Sélectionnez un type' : null,
    );
  }

  Widget _buildStatusSelector() {
    return DropdownButtonFormField<EventStatus>(
      value: _selectedStatus,
      decoration: InputDecoration(
        labelText: 'Statut',
        border: OutlineInputBorder(borderRadius: BorderRadius.circular(12)),
        filled: true,
        fillColor: AppColors.cardDark,
      ),
      items: EventStatus.values.map((status) {
        return DropdownMenuItem(
          value: status,
          child: Text(status.displayName),
        );
      }).toList(),
      onChanged: (value) => setState(() => _selectedStatus = value!),
    );
  }

  Widget _buildDateTimePicker(String label, DateTime? dateTime, Function(DateTime) onChanged, bool isStart) {
    return InkWell(
      onTap: () async {
        final date = await showDatePicker(
          context: context,
          initialDate: dateTime ?? (isStart ? DateTime.now().add(const Duration(days: 1)) : DateTime.now().add(const Duration(days: 1, hours: 2))),
          firstDate: isStart ? DateTime.now() : (dateTime ?? DateTime.now()),
          lastDate: DateTime.now().add(const Duration(days: 365)),
        );
        if (date != null && mounted) {
          final time = await showTimePicker(
            context: context,
            initialTime: TimeOfDay.fromDateTime(dateTime ?? DateTime.now().add(const Duration(hours: 1))),
          );
          if (time != null && mounted) {
            onChanged(DateTime(date.year, date.month, date.day, time.hour, time.minute));
          }
        }
      },
      child: Container(
        padding: const EdgeInsets.all(16),
        decoration: BoxDecoration(
          color: AppColors.cardDark,
          borderRadius: BorderRadius.circular(12),
          border: Border.all(color: AppColors.surface.withOpacity(0.3)),
        ),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(
              label,
              style: TextStyle(fontSize: 12, color: AppColors.surface.withOpacity(0.7)),
            ),
            const SizedBox(height: 4),
            Text(
              dateTime != null
                  ? '${DateFormat('dd/MM/yyyy').format(dateTime!)} à ${DateFormat('HH:mm').format(dateTime!)}'
                  : 'Sélectionner',
              style: Theme.of(context).textTheme.bodyMedium?.copyWith(
                fontWeight: FontWeight.w500,
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildFeatureToggles() {
    return Column(
      children: [
        SwitchListTile(
          title: const Text('Check-in activé'),
          subtitle: const Text('Permet le check-in (QR, géolocalisation, visage)'),
          value: _hasCheckIn,
          onChanged: (value) => setState(() => _hasCheckIn = value),
          activeColor: AppColors.primary,
          contentPadding: EdgeInsets.zero,
          dense: true,
        ),
        SwitchListTile(
          title: const Text('Géofencing'),
          subtitle: const Text('Check-in automatique en entrant dans la zone'),
          value: _hasGeofencing,
          onChanged: (value) => setState(() => _hasGeofencing = value),
          activeColor: AppColors.primary,
          contentPadding: EdgeInsets.zero,
          dense: true,
        ),
        SwitchListTile(
          title: const Text('Check-in visage'),
          subtitle: const Text('Reconnaissance faciale pour le check-in'),
          value: _hasFaceCheckIn,
          onChanged: (value) => setState(() => _hasFaceCheckIn = value),
          activeColor: AppColors.primary,
          contentPadding: EdgeInsets.zero,
          dense: true,
        ),
        SwitchListTile(
          title: const Text('Événement public'),
          subtitle: const Text('Visible dans l\'annuaire public'),
          value: _isPublic,
          onChanged: (value) => setState(() => _isPublic = value),
          activeColor: AppColors.primary,
          contentPadding: EdgeInsets.zero,
          dense: true,
        ),
      ],
    );
  }

  Widget _buildThumbnailPicker() {
    return GestureDetector(
      onTap: _pickThumbnail,
      child: Container(
        height: 180,
        width: double.infinity,
        decoration: BoxDecoration(
          color: AppColors.cardDark,
          borderRadius: BorderRadius.circular(16),
          border: Border.all(color: AppColors.primary.withOpacity(0.3)),
        ),
        child: _thumbnailUrl != null
            ? Stack(
                fit: StackFit.expand,
                children: [
                  ClipRRect(
                    borderRadius: BorderRadius.circular(16),
                    child: Image.network(
                      _thumbnailUrl!,
                      fit: BoxFit.cover,
                      errorBuilder: (_, __, ___) => _buildPlaceholder(),
                    ),
                  ),
                  Positioned(
                    bottom: 12,
                    right: 12,
                    child: CircleAvatar(
                      backgroundColor: AppColors.primary,
                      child: IconButton(
                        icon: const Icon(Icons.edit_rounded, color: Colors.white, size: 20),
                        onPressed: _pickThumbnail,
                      ),
                    ),
                  ),
                ],
              )
            : _buildPlaceholder(),
      ),
    );
  }

  Widget _buildPlaceholder() {
    return Column(
      mainAxisAlignment: MainAxisAlignment.center,
      children: [
        Icon(Icons.image_outlined, size: 48, color: AppColors.surface.withOpacity(0.5)),
        const SizedBox(height: 8),
        Text(
          'Ajouter une miniature',
          style: TextStyle(color: AppColors.surface.withOpacity(0.7)),
        ),
        const SizedBox(height: 4),
        Text(
          'Recommandé: 1280x720 (16:9)',
          style: TextStyle(fontSize: 12, color: AppColors.surface.withOpacity(0.5)),
        ),
      ],
    );
  }

  Future<void> _pickThumbnail() async {
    final picker = ImagePicker();
    final image = await picker.pickImage(source: ImageSource.gallery, maxWidth: 1280, maxHeight: 720, imageQuality: 85);
    if (image != null) {
      setState(() {
        _thumbnailUrl = 'https://picsum.photos/seed/${DateTime.now().millisecondsSinceEpoch}/1280/720';
      });
    }
  }

  Future<void> _saveEvent() async {
    if (!_formKey.currentState!.validate()) return;
    if (_startAt == null || _endAt == null) {
      ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('Veuillez sélectionner les dates')));
      return;
    }
    if (_endAt!.isBefore(_startAt!)) {
      ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('La date de fin doit être après la date de début')));
      return;
    }

    setState(() => _isLoading = true);

    try {
      final event = Event(
        id: widget.event?.id ?? 0,
        title: _titleController.text.trim(),
        description: _descriptionController.text.trim().isEmpty ? null : _descriptionController.text.trim(),
        startAt: _startAt!,
        endAt: _endAt!,
        location: _locationController.text.trim().isEmpty ? null : _locationController.text.trim(),
        streamUrl: _streamUrlController.text.trim().isEmpty ? null : _streamUrlController.text.trim(),
        thumbnailUrl: _thumbnailUrl,
        status: _selectedStatus,
        requiresRegistration: _requiresRegistration,
        maxAttendees: _maxAttendees,
        hasCheckIn: _hasCheckIn,
        hasGeofencing: _hasGeofencing,
        hasFaceCheckIn: _hasFaceCheckIn,
        isPublic: _isPublic,
        type: _selectedType,
        tags: _tagsController.text.split(',').map((e) => e.trim()).where((e) => e.isNotEmpty).toList(),
        createdAt: DateTime.now(),
      );

      if (widget.event != null) {
        await ref.read(eventsServiceProvider).updateEvent(widget.event!.id, event);
        if (mounted) {
          ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('Événement modifié ✓')));
          context.pop();
        }
      } else {
        await ref.read(eventsServiceProvider).createEvent(event);
        if (mounted) {
          ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('Événement créé ✓')));
          context.pop();
        }
      }
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text('Erreur: $e')));
      }
    } finally {
      if (mounted) setState(() => _isLoading = false);
    }
  }

  Future<void> _deleteEvent() async {
    if (widget.event == null) return;
    final confirm = await showDialog<bool>(
      context: context,
      builder: (context) => AlertDialog(
        backgroundColor: AppColors.cardDark,
        title: const Text('Supprimer l\'événement'),
        content: const Text('Cette action est irréversible. Confirmer la suppression ?'),
        actions: [
          TextButton(onPressed: () => Navigator.pop(context, false), child: const Text('Annuler')),
          FilledButton(onPressed: () => Navigator.pop(context, true), style: FilledButton.styleFrom(backgroundColor: Colors.red), child: const Text('Supprimer')),
        ],
      ),
    );
    if (confirm == true && widget.event != null) {
      await ref.read(eventsServiceProvider).deleteEvent(widget.event!.id);
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('Événement supprimé ✓')));
        context.pop();
      }
    }
  }
}