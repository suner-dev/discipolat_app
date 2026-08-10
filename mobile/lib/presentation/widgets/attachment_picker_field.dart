import 'package:flutter/material.dart';
import '../../data/services/api_service.dart';
import 'document_create_dialog.dart';

/// Sélecteur multi de pièces jointes (module Fichiers — références, pas d'upload
/// binaire), réutilisé par tous les formulaires : demandes membres, événements,
/// rapports. La liste des documents est chargée à la première ouverture (lazy),
/// avec création directe d'un document via [showDocumentCreateDialog].
class AttachmentPickerField extends StatefulWidget {
  const AttachmentPickerField({
    super.key,
    required this.apiService,
    required this.value,
    required this.onChanged,
  });

  final ApiService apiService;
  final Set<String> value;
  final ValueChanged<Set<String>> onChanged;

  @override
  State<AttachmentPickerField> createState() => _AttachmentPickerFieldState();
}

class _AttachmentPickerFieldState extends State<AttachmentPickerField> {
  List<dynamic> _files = [];
  bool _loading = false;

  Future<void> _loadFiles() async {
    setState(() => _loading = true);
    try {
      final res = await widget.apiService.get('/files', params: {'size': '100'});
      final data = res.data;
      if (mounted) {
        setState(() {
          _files = (data is Map ? data['content'] : data) as List<dynamic>? ?? [];
          _loading = false;
        });
      }
    } catch (_) {
      if (mounted) setState(() => _loading = false);
    }
  }

  Future<void> _openPicker() async {
    if (_files.isEmpty && !_loading) await _loadFiles();
    if (!mounted) return;

    final selection = Set<String>.from(widget.value);

    await showModalBottomSheet(
      context: context,
      isScrollControlled: true,
      backgroundColor: const Color(0xFF1E293B),
      shape: const RoundedRectangleBorder(borderRadius: BorderRadius.vertical(top: Radius.circular(20))),
      builder: (ctx) => StatefulBuilder(
        builder: (ctx, setSheetState) => Padding(
          padding: EdgeInsets.fromLTRB(20, 20, 20, MediaQuery.of(ctx).viewInsets.bottom + 20),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Row(
                children: [
                  const Text(
                    'Pièces jointes',
                    style: TextStyle(color: Colors.white, fontSize: 16, fontWeight: FontWeight.bold),
                  ),
                  const Spacer(),
                  IconButton(
                    tooltip: 'Créer un document',
                    icon: const Icon(Icons.add_circle_outline, color: Colors.white),
                    onPressed: () async {
                      final createdId = await showDocumentCreateDialog(ctx, widget.apiService);
                      if (createdId == null || !ctx.mounted) return;
                      setSheetState(() {
                        selection.add(createdId);
                        _files.insert(0, {'id': createdId, 'nom': 'Nouveau document'});
                      });
                      // Recharge en arrière-plan pour afficher le vrai nom.
                      _loadFiles().then((_) {
                        if (ctx.mounted) setSheetState(() {});
                      });
                    },
                  ),
                ],
              ),
              const SizedBox(height: 8),
              Text(
                'Documents du module Fichiers',
                style: TextStyle(color: Colors.white.withValues(alpha: 0.4), fontSize: 11),
              ),
              const SizedBox(height: 8),
              if (_loading && _files.isEmpty)
                const Padding(
                  padding: EdgeInsets.all(16),
                  child: Center(child: CircularProgressIndicator(strokeWidth: 2)),
                )
              else if (_files.isEmpty)
                Padding(
                  padding: const EdgeInsets.all(16),
                  child: Center(
                    child: Text(
                      'Aucun document disponible. Créez-en un avec le bouton +.',
                      textAlign: TextAlign.center,
                      style: TextStyle(color: Colors.white.withValues(alpha: 0.4), fontSize: 12),
                    ),
                  ),
                )
              else
                ConstrainedBox(
                  constraints: const BoxConstraints(maxHeight: 320),
                  child: ListView(
                    shrinkWrap: true,
                    children: _files.map((f) {
                      final id = f['id']?.toString();
                      if (id == null) return const SizedBox.shrink();
                      final checked = selection.contains(id);
                      return CheckboxListTile(
                        dense: true,
                        value: checked,
                        activeColor: Colors.blue,
                        controlAffinity: ListTileControlAffinity.leading,
                        contentPadding: EdgeInsets.zero,
                        title: Text(
                          f['nom']?.toString() ?? 'Document',
                          style: TextStyle(
                            color: Colors.white.withValues(alpha: 0.8),
                            fontSize: 13,
                          ),
                          overflow: TextOverflow.ellipsis,
                        ),
                        secondary: Icon(
                          Icons.description_outlined,
                          size: 18,
                          color: Colors.white.withValues(alpha: 0.4),
                        ),
                        onChanged: (v) => setSheetState(() {
                          if (v == true) {
                            selection.add(id);
                          } else {
                            selection.remove(id);
                          }
                        }),
                      );
                    }).toList(),
                  ),
                ),
              const SizedBox(height: 16),
              SizedBox(
                width: double.infinity,
                child: ElevatedButton(
                  onPressed: () {
                    widget.onChanged(selection);
                    Navigator.pop(ctx);
                  },
                  style: ElevatedButton.styleFrom(
                    backgroundColor: Colors.blue,
                    foregroundColor: Colors.white,
                    padding: const EdgeInsets.symmetric(vertical: 12),
                    shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
                  ),
                  child: const Text('Valider la sélection', style: TextStyle(fontWeight: FontWeight.w600)),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }

  Widget _buildChips() {
    final selected = _files
        .where((f) => widget.value.contains(f['id']?.toString()))
        .toList();
    if (selected.isEmpty) return const SizedBox.shrink();
    return Padding(
      padding: const EdgeInsets.only(top: 8),
      child: Wrap(
        spacing: 6,
        runSpacing: 6,
        children: selected.map((f) {
          final id = f['id']?.toString() ?? '';
          return Chip(
            backgroundColor: Colors.white.withValues(alpha: 0.06),
            side: BorderSide(color: Colors.white.withValues(alpha: 0.1)),
            label: Row(
              mainAxisSize: MainAxisSize.min,
              children: [
                Icon(Icons.description_outlined, size: 14, color: Colors.white.withValues(alpha: 0.5)),
                const SizedBox(width: 4),
                ConstrainedBox(
                  constraints: const BoxConstraints(maxWidth: 140),
                  child: Text(
                    f['nom']?.toString() ?? 'Document',
                    overflow: TextOverflow.ellipsis,
                    style: const TextStyle(color: Colors.white, fontSize: 11),
                  ),
                ),
              ],
            ),
            onDeleted: () => widget.onChanged(Set.of(widget.value)..remove(id)),
            deleteIconColor: Colors.white54,
            deleteButtonTooltipMessage: 'Retirer',
          );
        }).toList(),
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        OutlinedButton.icon(
          onPressed: _openPicker,
          style: OutlinedButton.styleFrom(
            foregroundColor: Colors.white.withValues(alpha: 0.7),
            side: BorderSide(color: Colors.white.withValues(alpha: 0.15)),
            padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 10),
            shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(10)),
          ),
          icon: const Icon(Icons.attach_file, size: 18),
          label: Text(
            widget.value.isEmpty ? 'Joindre des documents' : 'Modifier les documents',
            style: const TextStyle(fontSize: 12),
          ),
        ),
        _buildChips(),
      ],
    );
  }
}
