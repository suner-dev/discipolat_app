import 'package:flutter/material.dart';
import '../../../l10n/app_localizations.dart';

/// FormBuilderScreen — simplified drag & drop form builder.
class FormBuilderScreen extends StatefulWidget {
  const FormBuilderScreen({super.key});
  @override
  State<FormBuilderScreen> createState() => _FormBuilderScreenState();
}

class _FormBuilderScreenState extends State<FormBuilderScreen> {
  final List<Map<String, dynamic>> _fields = [];

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context);
    final fieldTypes = [l10n.fieldText, l10n.fieldChoice, l10n.fieldDate, l10n.fieldFile, l10n.fieldSignature, l10n.fieldNote];

    return Scaffold(
      appBar: AppBar(
        title: Text(l10n.createForm),
        backgroundColor: Colors.teal.shade700,
        foregroundColor: Colors.white,
        actions: [
          IconButton(
            icon: const Icon(Icons.save),
            onPressed: _fields.isEmpty ? null : () {
              ScaffoldMessenger.of(context).showSnackBar(
                SnackBar(content: Text(l10n.formSaved)),
              );
              Navigator.pop(context);
            },
          ),
        ],
      ),
      body: Column(
        children: [
          Padding(
            padding: const EdgeInsets.all(16),
            child: TextField(
              decoration: InputDecoration(
                labelText: l10n.formTitleLabel,
                border: const OutlineInputBorder(),
              ),
            ),
          ),
          Wrap(
            spacing: 8,
            children: fieldTypes.map((type) => ActionChip(
              avatar: const Icon(Icons.add, size: 16),
              label: Text(type),
              onPressed: () => setState(() {
                _fields.add({'type': type, 'label': '${l10n.createForm} $type', 'required': false});
              }),
            )).toList(),
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
                        title: Text(_fields[i]['label']),
                        subtitle: Text(_fields[i]['type']),
                        trailing: Row(
                          mainAxisSize: MainAxisSize.min,
                          children: [
                            Switch(
                              value: _fields[i]['required'],
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
