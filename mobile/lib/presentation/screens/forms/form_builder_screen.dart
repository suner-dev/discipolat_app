import 'dart:convert';

import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../../../l10n/app_localizations.dart';
import '../../../data/services/providers.dart';

/// FormBuilderScreen — wired to real backend FormController (/api/v1/forms).
/// Allows creating and saving form templates via the API.
class FormBuilderScreen extends ConsumerStatefulWidget {
  const FormBuilderScreen({super.key});

  @override
  ConsumerState<FormBuilderScreen> createState() => _FormBuilderScreenState();
}

class _FormBuilderScreenState extends ConsumerState<FormBuilderScreen> {
  final _titleCtrl = TextEditingController();
  final List<Map<String, dynamic>> _fields = [];
  bool _saving = false;

  @override
  void dispose() {
    _titleCtrl.dispose();
    super.dispose();
  }

  Future<void> _saveForm() async {
    if (_titleCtrl.text.trim().isEmpty || _fields.isEmpty) return;
    setState(() => _saving = true);
    try {
      final fieldsJson = jsonEncode(_fields);
      await ref.read(formServiceProvider).create(
            titre: _titleCtrl.text.trim(),
            description: 'Formulaire créé depuis mobile',
            fieldsJson: fieldsJson,
          );
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('Formulaire sauvegardé !')),
        );
        Navigator.pop(context);
      }
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text('Erreur : ${e.toString().split(':').first}')),
        );
      }
    } finally {
      if (mounted) setState(() => _saving = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context);
    final fieldTypes = [
      l10n.fieldText,
      l10n.fieldChoice,
      l10n.fieldDate,
      l10n.fieldFile,
      l10n.fieldSignature,
      l10n.fieldNote,
    ];

    return Scaffold(
      appBar: AppBar(
        title: Text(l10n.createForm),
        backgroundColor: Colors.teal.shade700,
        foregroundColor: Colors.white,
        actions: [
          IconButton(
            icon: _saving
                ? const SizedBox(
                    width: 18,
                    height: 18,
                    child: CircularProgressIndicator(strokeWidth: 2, color: Colors.white),
                  )
                : const Icon(Icons.save),
            onPressed: (_fields.isEmpty || _saving) ? null : _saveForm,
          ),
        ],
      ),
      body: Column(
        children: [
          Padding(
            padding: const EdgeInsets.all(16),
            child: TextField(
              controller: _titleCtrl,
              decoration: InputDecoration(
                labelText: l10n.formTitleLabel,
                border: const OutlineInputBorder(),
              ),
            ),
          ),
          Wrap(
            spacing: 8,
            children: fieldTypes
                .map((type) => ActionChip(
                      avatar: const Icon(Icons.add, size: 16),
                      label: Text(type),
                      onPressed: () => setState(() {
                        _fields.add({'type': type, 'label': type, 'required': false});
                      }),
                    ))
                .toList(),
          ),
          const Divider(),
          Expanded(
            child: _fields.isEmpty
                ? Center(child: Text(l10n.addFieldHint))
                : ReorderableListView.builder(
                    itemCount: _fields.length,
                    onReorder: (old, new_) => setState(() {
                      final item = _fields.removeAt(old);
                      _fields.insert(new_, item);
                    }),
                    itemBuilder: (ctx, i) => Card(
                      key: ValueKey(i),
                      child: ListTile(
                        leading: const Icon(Icons.drag_handle),
                        title: Text(_fields[i]['label'] ?? ''),
                        subtitle: Text(_fields[i]['type'] ?? ''),
                        trailing: Row(
                          mainAxisSize: MainAxisSize.min,
                          children: [
                            Switch(
                              value: _fields[i]['required'] ?? false,
                              onChanged: (v) => setState(() => _fields[i]['required'] = v),
                            ),
                            IconButton(
                              icon: const Icon(Icons.delete, color: Colors.red),
                              onPressed: () => setState(() => _fields.removeAt(i)),
                            ),
                          ],
                        ),
                      ),
                    ),
                  ),
          ),
        ],
      ),
    );
  }
}
