import 'package:flutter/material.dart';
import 'form_fill_screen.dart';

/// P1 #13 — Formulaires drag & drop: vue des formulaires et soumission
class FormsScreen extends StatelessWidget {
  const FormsScreen({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('📝 Formulaires'),
        backgroundColor: Colors.teal.shade600,
        foregroundColor: Colors.white,
      ),
      body: ListView(
        padding: const EdgeInsets.all(16),
        children: [
          // Builder toggle
          Card(
            child: ListTile(
              leading: const Icon(Icons.add_chart, color: Colors.teal),
              title: const Text('Créer un formulaire'),
              subtitle: const Text('Drag & drop avec conditions logiques'),
              trailing: const Icon(Icons.chevron_right),
              onTap: () {
                Navigator.push(context, MaterialPageRoute(
                  builder: (_) => const FormBuilderScreen(),
                ));
              },
            ),
          ),
          const SizedBox(height: 16),
          // Published forms list
          const Text('Formulaires publiés',
              style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold)),
          const SizedBox(height: 8),
          _buildFormCard(context, 'Satisfaction culte', 'Sondage post-culte', 42, Colors.blue),
          _buildFormCard(context, 'Inscription événement', 'Inscription gratuit', 18, Colors.green),
          _buildFormCard(context, 'Feedback formation', 'Évaluation formations', 31, Colors.orange),
        ],
      ),
    );
  }

  Widget _buildFormCard(BuildContext context, String title, String desc, int responses, Color color) {
    return Card(
      child: ListTile(
        leading: CircleAvatar(
          backgroundColor: color.withValues(alpha: 0.1),
          child: Icon(Icons.description, color: color),
        ),
        title: Text(title),
        subtitle: Text(desc),
        trailing: Chip(label: Text('$responses réponses')),
        onTap: () {
          Navigator.push(context, MaterialPageRoute(
            builder: (_) => FormFillScreen(form: {'id': title, 'titre': title}),
          ));
        },
      ),
    );
  }
}

/// FormBuilderScreen — simplified drag & drop form builder
class FormBuilderScreen extends StatefulWidget {
  const FormBuilderScreen({super.key});
  @override
  State<FormBuilderScreen> createState() => _FormBuilderScreenState();
}

class _FormBuilderScreenState extends State<FormBuilderScreen> {
  final List<Map<String, dynamic>> _fields = [];
  final _fieldTypes = ['Texte', 'Choix multiple', 'Date', 'Fichier', 'Signature', 'Note'];

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Créateur de formulaire'),
        backgroundColor: Colors.teal.shade700,
        foregroundColor: Colors.white,
        actions: [
          IconButton(
            icon: const Icon(Icons.save),
            onPressed: _fields.isEmpty ? null : () {
              ScaffoldMessenger.of(context).showSnackBar(
                const SnackBar(content: Text('Formulaire sauvegardé !')),
              );
              Navigator.pop(context);
            },
          ),
        ],
      ),
      body: Column(
        children: [
          // Title input
          Padding(
            padding: const EdgeInsets.all(16),
            child: TextField(
              decoration: const InputDecoration(
                labelText: 'Titre du formulaire',
                border: OutlineInputBorder(),
              ),
            ),
          ),
          // Field types chips (simulating drag & drop)
          Wrap(
            spacing: 8,
            children: _fieldTypes.map((type) => ActionChip(
              avatar: const Icon(Icons.add, size: 16),
              label: Text(type),
              onPressed: () => setState(() {
                _fields.add({'type': type, 'label': 'Champ $type', 'required': false});
              }),
            )).toList(),
          ),
          const Divider(),
          // Fields preview
          Expanded(
            child: _fields.isEmpty
                ? const Center(child: Text('Appuyez sur un type de champ pour l\'ajouter'))
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
