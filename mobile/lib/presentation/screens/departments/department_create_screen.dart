import 'package:flutter/material.dart';
import '../../../data/services/api_service.dart';
import '../../widgets/glass_theme.dart';
import '../../widgets/app_drawer.dart';

class DepartmentCreateScreen extends StatefulWidget {
  const DepartmentCreateScreen({super.key});

  @override
  State<DepartmentCreateScreen> createState() => _DepartmentCreateScreenState();
}

class _DepartmentCreateScreenState extends State<DepartmentCreateScreen> {
  final _apiService = ApiService();
  final _formKey = GlobalKey<FormState>();
  final _nomController = TextEditingController();
  final _descriptionController = TextEditingController();
  final _newRespPrenomController = TextEditingController();
  final _newRespNomController = TextEditingController();
  final _newRespEmailController = TextEditingController();
  final _newRespTelephoneController = TextEditingController();

  List<dynamic> _potentielsResponsables = [];
  String _mode = 'existing'; // 'existing', 'new'
  String? _responsableId;
  bool _loading = false;
  bool _saving = false;

  @override
  void initState() {
    super.initState();
    _loadResponsables();
  }

  Future<void> _loadResponsables() async {
    setState(() => _loading = true);
    try {
      final res = await _apiService.get('/users?role=RESPONSABLE&size=100');
      if (mounted) {
        setState(() {
          _potentielsResponsables = (res.data['content'] as List?) ?? [];
          _loading = false;
        });
      }
    } catch (e) {
      if (mounted) setState(() => _loading = false);
    }
  }

  Future<void> _createDepartment() async {
    if (!_formKey.currentState!.validate()) return;

    setState(() => _saving = true);
    try {
      final payload = <String, dynamic>{
        'nom': _nomController.text.trim(),
        'description': _descriptionController.text.trim().isEmpty ? null : _descriptionController.text.trim(),
      };

      if (_mode == 'existing') {
        if (_responsableId == null || _responsableId!.isEmpty) {
          ScaffoldMessenger.of(context).showSnackBar(
            const SnackBar(content: Text('Veuillez sélectionner un responsable'), backgroundColor: Colors.red),
          );
          setState(() => _saving = false);
          return;
        }
        payload['responsableId'] = _responsableId;
      } else {
        payload['createNewResponsable'] = true;
        payload['newResponsableFirstName'] = _newRespPrenomController.text.trim();
        payload['newResponsableLastName'] = _newRespNomController.text.trim();
        payload['newResponsableEmail'] = _newRespEmailController.text.trim();
        payload['newResponsablePhone'] = _newRespTelephoneController.text.trim().isEmpty
            ? null
            : _newRespTelephoneController.text.trim();
      }

      await _apiService.post('/departments', data: payload);
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('Département créé avec succès'), backgroundColor: Colors.green),
        );
        Navigator.pop(context, true);
      }
    } catch (e) {
      if (mounted) {
        setState(() => _saving = false);
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text('Erreur: $e'), backgroundColor: Colors.red),
        );
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    // final l10n = AppLocalizations.of(context);

    return Scaffold(
      appBar: AppBar(
        title: const Text('Nouveau département'),
        backgroundColor: Colors.transparent,
        elevation: 0,
      ),
      drawer: const AppDrawer(),
      body: _loading
          ? const Center(child: CircularProgressIndicator(color: Color(0xFF06B6D4)))
          : Form(
              key: _formKey,
              child: ListView(
                padding: const EdgeInsets.all(16),
                children: [
                  GlassCard(
                    padding: const EdgeInsets.all(16),
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text('Informations du département', style: TextStyle(color: Colors.white.withValues(alpha: 0.5), fontSize: 12)),
                        const SizedBox(height: 12),
                        TextFormField(
                          controller: _nomController,
                          style: const TextStyle(color: Colors.white),
                          decoration: const InputDecoration(labelText: 'Nom *', labelStyle: TextStyle(color: Colors.white70)),
                          validator: (v) => v == null || v.trim().isEmpty ? 'Nom requis' : null,
                        ),
                        const SizedBox(height: 12),
                        TextFormField(
                          controller: _descriptionController,
                          style: const TextStyle(color: Colors.white),
                          decoration: const InputDecoration(labelText: 'Description', labelStyle: TextStyle(color: Colors.white70)),
                          maxLines: 3,
                        ),
                      ],
                    ),
                  ),

                  const SizedBox(height: 16),

                  GlassCard(
                    padding: const EdgeInsets.all(16),
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text('Responsable', style: TextStyle(color: Colors.white.withValues(alpha: 0.5), fontSize: 12)),
                        const SizedBox(height: 8),
                        Row(
                          children: [
                            Expanded(
                              child: _ModeButton(
                                label: 'Sélectionner un responsable',
                                icon: Icons.check_circle,
                                selected: _mode == 'existing',
                                onTap: () => setState(() {
                                  _mode = 'existing';
                                  _responsableId = null;
                                }),
                              ),
                            ),
                            const SizedBox(width: 8),
                            Expanded(
                              child: _ModeButton(
                                label: 'Créer un nouveau responsable',
                                icon: Icons.person_add,
                                selected: _mode == 'new',
                                onTap: () => setState(() {
                                  _mode = 'new';
                                  _responsableId = null;
                                }),
                              ),
                            ),
                          ],
                        ),

                        const SizedBox(height: 16),

                        if (_mode == 'existing') ...[
                          if (_potentielsResponsables.isEmpty)
                            Text(
                              'Aucun responsable disponible. Basculez sur "Créer un nouveau responsable".',
                              style: TextStyle(color: Colors.amber, fontSize: 12),
                            )
                          else
                            DropdownButtonFormField<String>(
                              value: _responsableId?.isEmpty == true ? null : _responsableId,
                              isExpanded: true,
                              style: const TextStyle(color: Colors.white),
                              decoration: const InputDecoration(labelText: 'Responsable *', labelStyle: TextStyle(color: Colors.white70)),
                              dropdownColor: const Color(0xFF111827),
                              items: _potentielsResponsables.map((u) {
                                final nom = '${u['firstName'] ?? ''} ${u['lastName'] ?? ''}'.trim();
                                return DropdownMenuItem(
                                  value: u['id'] as String,
                                  child: Text(nom.isEmpty ? u['email'] ?? '' : nom),
                                );
                              }).toList(),
                              onChanged: (v) => setState(() => _responsableId = v),
                              validator: (v) => v == null ? 'Sélectionnez un responsable' : null,
                            ),
                        ] else if (_mode == 'new') ...[
                          TextFormField(
                            controller: _newRespPrenomController,
                            style: const TextStyle(color: Colors.white),
                            decoration: const InputDecoration(labelText: 'Prénom *', labelStyle: TextStyle(color: Colors.white70)),
                            validator: (v) => v == null || v.trim().isEmpty ? 'Requis' : null,
                          ),
                          const SizedBox(height: 12),
                          TextFormField(
                            controller: _newRespNomController,
                            style: const TextStyle(color: Colors.white),
                            decoration: const InputDecoration(labelText: 'Nom *', labelStyle: TextStyle(color: Colors.white70)),
                            validator: (v) => v == null || v.trim().isEmpty ? 'Requis' : null,
                          ),
                          const SizedBox(height: 12),
                          TextFormField(
                            controller: _newRespEmailController,
                            style: const TextStyle(color: Colors.white),
                            decoration: const InputDecoration(labelText: 'Email *', labelStyle: TextStyle(color: Colors.white70)),
                            keyboardType: TextInputType.emailAddress,
                            validator: (v) => v == null || v.trim().isEmpty || !v.contains('@') ? 'Email valide requis' : null,
                          ),
                          const SizedBox(height: 12),
                          TextFormField(
                            controller: _newRespTelephoneController,
                            style: const TextStyle(color: Colors.white),
                            decoration: const InputDecoration(labelText: 'Téléphone', labelStyle: TextStyle(color: Colors.white70)),
                            keyboardType: TextInputType.phone,
                          ),
                          const SizedBox(height: 8),
                          Text(
                            'Le compte du responsable sera créé avec le rôle RESPONSABLE',
                            style: TextStyle(color: Colors.white.withValues(alpha: 0.4), fontSize: 11),
                          ),
                        ],
                      ],
                    ),
                  ),

                  const SizedBox(height: 24),

                  FilledButton(
                    onPressed: _saving ? null : _createDepartment,
                    style: FilledButton.styleFrom(
                      backgroundColor: const Color(0xFF06B6D4),
                      padding: const EdgeInsets.symmetric(vertical: 16),
                    ),
                    child: _saving
                        ? const SizedBox(width: 20, height: 20, child: CircularProgressIndicator(strokeWidth: 2, color: Colors.white))
                        : const Text('Créer le département', style: TextStyle(fontSize: 16)),
                  ),
                ],
              ),
            ),
    );
  }
}

class _ModeButton extends StatelessWidget {
  final String label;
  final IconData icon;
  final bool selected;
  final VoidCallback onTap;

  const _ModeButton({
    required this.label,
    required this.icon,
    required this.selected,
    required this.onTap,
  });

  @override
  Widget build(BuildContext context) {
    return GestureDetector(
      onTap: onTap,
      child: Container(
        padding: const EdgeInsets.symmetric(vertical: 12, horizontal: 8),
        decoration: BoxDecoration(
          gradient: selected
              ? const LinearGradient(colors: [Color(0xFF06B6D4), Color(0xFF3B82F6)])
              : null,
          color: selected ? null : Colors.white.withValues(alpha: 0.05),
          borderRadius: BorderRadius.circular(12),
          border: Border.all(
            color: selected ? Colors.transparent : Colors.white.withValues(alpha: 0.1),
          ),
        ),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Icon(icon, color: selected ? Colors.white : Colors.white70, size: 20),
            const SizedBox(height: 4),
            Text(label, style: TextStyle(color: selected ? Colors.white : Colors.white70, fontSize: 11), textAlign: TextAlign.center),
          ],
        ),
      ),
    );
  }
}