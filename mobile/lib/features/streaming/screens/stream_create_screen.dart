import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:image_picker/image_picker.dart';
import 'package:intl/intl.dart';

import 'package:discipolat_mobile/features/streaming/models/stream_model.dart';
import 'package:discipolat_mobile/features/streaming/services/streaming_service.dart';
import 'package:discipolat_mobile/presentation/widgets/glass_theme.dart';

class StreamCreateScreen extends ConsumerStatefulWidget {
  final StreamModel? stream;

  const StreamCreateScreen({super.key, this.stream});

  @override
  ConsumerState<StreamCreateScreen> createState() => _StreamCreateScreenState();
}

class _StreamCreateScreenState extends ConsumerState<StreamCreateScreen> {
  final _formKey = GlobalKey<FormState>();
  final _titleController = TextEditingController();
  final _descriptionController = TextEditingController();
  final _streamUrlController = TextEditingController();
  DateTime? _scheduledAt;
  String? _thumbnailUrl;
  bool _isLoading = false;

  @override
  void initState() {
    super.initState();
    if (widget.stream != null) {
      _titleController.text = widget.stream!.title;
      _descriptionController.text = widget.stream!.description ?? '';
      _streamUrlController.text = widget.stream!.streamUrl ?? '';
      _scheduledAt = widget.stream!.scheduledAt;
      _thumbnailUrl = widget.stream!.thumbnailUrl;
    } else {
      _scheduledAt = DateTime.now().add(const Duration(hours: 1));
    }
  }

  @override
  void dispose() {
    _titleController.dispose();
    _descriptionController.dispose();
    _streamUrlController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final isEditing = widget.stream != null;

    return Scaffold(
      backgroundColor: AppColors.surfaceDark,
      appBar: AppBar(
        title: Text(isEditing ? 'Modifier le stream' : 'Nouveau stream'),
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
                hintText: 'Titre de votre stream',
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
                hintText: 'Description optionnelle',
                border: OutlineInputBorder(borderRadius: BorderRadius.circular(12)),
                filled: true,
                fillColor: AppColors.cardDark,
              ),
              maxLines: 4,
            ),
            const SizedBox(height: 16),

            // Stream URL
            TextFormField(
              controller: _streamUrlController,
              decoration: InputDecoration(
                labelText: 'URL du flux (HLS/DASH/RTMP) *',
                hintText: 'https://example.com/stream.m3u8',
                border: OutlineInputBorder(borderRadius: BorderRadius.circular(12)),
                filled: true,
                fillColor: AppColors.cardDark,
                prefixIcon: const Icon(Icons.link_rounded),
              ),
              validator: (value) {
                if (value == null || value.trim().isEmpty) {
                  return 'L\'URL du flux est requise';
                }
                if (!value.startsWith('http')) {
                  return 'L\'URL doit commencer par http:// ou https://';
                }
                return null;
              },
            ),
            const SizedBox(height: 16),

            // Scheduled date/time
            _buildDateTimePicker(),
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
                        'Formats supportés',
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
              onPressed: _isLoading ? null : _saveStream,
              icon: _isLoading
                  ? const SizedBox(width: 20, height: 20, child: CircularProgressIndicator(strokeWidth: 2, color: Colors.white))
                  : Icon(isEditing ? Icons.save_rounded : Icons.add_rounded),
              label: Text(isEditing ? 'Enregistrer' : 'Créer le stream'),
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
                onPressed: _isLoading ? null : _deleteStream,
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
        Icon(Icons.image_outlined, size: 48, color: AppColors.primary.withOpacity(0.5)),
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

  Widget _buildDateTimePicker() {
    return InkWell(
      onTap: _pickDateTime,
      child: Container(
        padding: const EdgeInsets.all(16),
        decoration: BoxDecoration(
          color: AppColors.cardDark,
          borderRadius: BorderRadius.circular(12),
          border: Border.all(color: AppColors.primary.withOpacity(0.3)),
        ),
        child: Row(
          children: [
            Icon(Icons.schedule_rounded, color: AppColors.primary),
            const SizedBox(width: 12),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    'Date et heure de diffusion *',
                    style: TextStyle(fontSize: 12, color: AppColors.surface.withOpacity(0.7)),
                  ),
                  const SizedBox(height: 4),
                  Text(
                    _scheduledAt != null
                        ? '${DateFormat('dd/MM/yyyy').format(_scheduledAt!)} à ${DateFormat('HH:mm').format(_scheduledAt!)}'
                        : 'Sélectionner',
                    style: Theme.of(context).textTheme.bodyMedium?.copyWith(
                      fontWeight: FontWeight.w500,
                    ),
                  ),
                ],
              ),
            ),
            Icon(Icons.arrow_forward_ios_rounded, size: 16, color: AppColors.surface.withOpacity(0.5)),
          ],
        ),
      ),
    );
  }

  Future<void> _pickDateTime() async {
    final date = await showDatePicker(
      context: context,
      initialDate: _scheduledAt ?? DateTime.now().add(const Duration(hours: 1)),
      firstDate: DateTime.now(),
      lastDate: DateTime.now().add(const Duration(days: 365)),
    );

    if (date != null && mounted) {
      final time = await showTimePicker(
        context: context,
        initialTime: TimeOfDay.fromDateTime(_scheduledAt ?? DateTime.now().add(const Duration(hours: 1))),
      );

      if (time != null && mounted) {
        setState(() {
          _scheduledAt = DateTime(
            date.year,
            date.month,
            date.day,
            time.hour,
            time.minute,
          );
        });
      }
    }
  }

  Future<void> _pickThumbnail() async {
    final picker = ImagePicker();
    final image = await picker.pickImage(source: ImageSource.gallery, maxWidth: 1280, maxHeight: 720, imageQuality: 85);

    if (image != null) {
      // TODO: Upload to server and get URL
      // For now, use a placeholder
      setState(() {
        _thumbnailUrl = 'https://picsum.photos/seed/${DateTime.now().millisecondsSinceEpoch}/1280/720';
      });
    }
  }

  Future<void> _saveStream() async {
    if (!_formKey.currentState!.validate()) return;
    if (_scheduledAt == null) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Veuillez sélectionner une date/heure')),
      );
      return;
    }

    setState(() => _isLoading = true);

    try {
      final stream = StreamModel(
        id: widget.stream?.id ?? 0,
        title: _titleController.text.trim(),
        description: _descriptionController.text.trim().isEmpty ? null : _descriptionController.text.trim(),
        status: StreamStatus.scheduled,
        streamUrl: _streamUrlController.text.trim(),
        thumbnailUrl: _thumbnailUrl,
        scheduledAt: _scheduledAt!,
        createdAt: DateTime.now(),
        viewerCount: 0,
        totalViews: 0,
      );

      if (widget.stream != null) {
        // TODO: Implement update
        // await ref.read(streamingServiceProvider).updateStream(widget.stream!.id, stream);
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('Modification bientôt disponible')),
        );
      } else {
        await ref.read(streamingServiceProvider).createStream(stream);
        if (mounted) {
          ScaffoldMessenger.of(context).showSnackBar(
            const SnackBar(content: Text('Stream créé avec succès ✓')),
          );
          context.pop();
        }
      }
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text('Erreur: $e')),
        );
      }
    } finally {
      if (mounted) setState(() => _isLoading = false);
    }
  }

  Future<void> _deleteStream() async {
    final confirm = await showDialog<bool>(
      context: context,
      builder: (context) => AlertDialog(
        backgroundColor: AppColors.cardDark,
        title: const Text('Supprimer le stream'),
        content: const Text('Cette action est irréversible. Confirmer la suppression ?'),
        actions: [
          TextButton(onPressed: () => Navigator.pop(context, false), child: const Text('Annuler')),
          FilledButton(
            onPressed: () => Navigator.pop(context, true),
            style: FilledButton.styleFrom(backgroundColor: Colors.red),
            child: const Text('Supprimer'),
          ),
        ],
      ),
    );

    if (confirm == true && widget.stream != null) {
      // TODO: Implement delete
      // await ref.read(streamingServiceProvider).deleteStream(widget.stream!.id);
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('Suppression bientôt disponible')),
        );
      }
    }
  }
}
