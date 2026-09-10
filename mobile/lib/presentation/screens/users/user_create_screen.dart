import 'package:flutter/material.dart';
import '../../../data/services/api_service.dart';
import '../../widgets/glass_theme.dart';
import '../../widgets/app_drawer.dart';
import '../../../../l10n/app_localizations.dart';

class UserCreateScreen extends StatefulWidget {
  const UserCreateScreen({super.key});

  @override
  State<UserCreateScreen> createState() => _UserCreateScreenState();
}

class _UserCreateScreenState extends State<UserCreateScreen> {
  final _apiService = ApiService();
  final _formKey = GlobalKey<FormState>();
  final _emailController = TextEditingController();
  final _prenomController = TextEditingController();
  final _nomController = TextEditingController();
  final _passwordController = TextEditingController();
  final _telephoneController = TextEditingController();

  String _role = 'MEMBRE';
  List<String> _roles = [];
  String? _activeRole;
  bool _saving = false;

  @override
  void initState() {
    super.initState();
    _loadData();
  }

  Future<void> _loadData() async {
    // Nothing to load for now
  }

  Future<void> _createUser() async {
    if (!_formKey.currentState!.validate()) return;

    setState(() => _saving = true);
    try {
      final payload = <String, dynamic>{
        'email': _emailController.text.trim(),
        'firstName': _prenomController.text.trim(),
        'lastName': _nomController.text.trim(),
        'password': _passwordController.text,
        'role': _role,
      };

      if (_roles.isNotEmpty) payload['roles'] = _roles;
      if (_activeRole != null) payload['activeRole'] = _activeRole;

      await _apiService.post('/users', data: payload);
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('Utilisateur créé avec succès'), backgroundColor: Colors.green),
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
        title: const Text('Nouvel utilisateur'),
        backgroundColor: Colors.transparent,
        elevation: 0,
      ),
      drawer: const AppDrawer(),
      body: Form(
        key: _formKey,
        child: ListView(
          padding: const EdgeInsets.all(16),
          children: [
            GlassCard(
              padding: const EdgeInsets.all(16),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text('Informations', style: TextStyle(color: Colors.white.withValues(alpha: 0.5), fontSize: 12)),
                  const SizedBox(height: 12),
                  TextFormField(
                    controller: _emailController,
                    style: const TextStyle(color: Colors.white),
                    decoration: const InputDecoration(labelText: 'Email *', labelStyle: TextStyle(color: Colors.white70)),
                    keyboardType: TextInputType.emailAddress,
                    validator: (v) => v == null || v.trim().isEmpty || !v.contains('@') ? 'Email valide requis' : null,
                  ),
                  const SizedBox(height: 12),
                  Row(
                    children: [
                      Expanded(
                        child: TextFormField(
                          controller: _prenomController,
                          style: const TextStyle(color: Colors.white),
                          decoration: const InputDecoration(labelText: 'Prénom *', labelStyle: TextStyle(color: Colors.white70)),
                          validator: (v) => v == null || v.trim().isEmpty ? 'Requis' : null,
                        ),
                      ),
                      const SizedBox(width: 12),
                      Expanded(
                        child: TextFormField(
                          controller: _nomController,
                          style: const TextStyle(color: Colors.white),
                          decoration: const InputDecoration(labelText: 'Nom *', labelStyle: TextStyle(color: Colors.white70)),
                          validator: (v) => v == null || v.trim().isEmpty ? 'Requis' : null,
                        ),
                      ),
                    ],
                  ),
                  const SizedBox(height: 12),
                  TextFormField(
                    controller: _passwordController,
                    style: const TextStyle(color: Colors.white),
                    decoration: const InputDecoration(labelText: 'Mot de passe *', labelStyle: TextStyle(color: Colors.white70)),
                    obscureText: true,
                    validator: (v) => v == null || v.length < 8 ? 'Min 8 caractères' : null,
                  ),
                  const SizedBox(height: 12),
                  TextFormField(
                    controller: _telephoneController,
                    style: const TextStyle(color: Colors.white),
                    decoration: const InputDecoration(labelText: 'Téléphone', labelStyle: TextStyle(color: Colors.white70)),
                    keyboardType: TextInputType.phone,
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
                  Text('Rôle et permissions', style: TextStyle(color: Colors.white.withValues(alpha: 0.5), fontSize: 12)),
                  const SizedBox(height: 12),
                  DropdownButtonFormField<String>(
                    value: _role,
                    isExpanded: true,
                    style: const TextStyle(color: Colors.white),
                    decoration: const InputDecoration(labelText: 'Rôle principal *', labelStyle: TextStyle(color: Colors.white70)),
                    dropdownColor: const Color(0xFF111827),
                    items: ['ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR', 'MEMBRE']
                        .map((e) => DropdownMenuItem(value: e, child: Text(e)))
                        .toList(),
                    onChanged: (v) => setState(() => _role = v ?? 'MEMBRE'),
                  ),
                  const SizedBox(height: 12),
                  Wrap(
                    spacing: 8,
                    runSpacing: 8,
                    children: ['ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR', 'MEMBRE'].map((r) {
                      final isSelected = _roles.contains(r);
                      return FilterChip(
                        label: Text(r, style: TextStyle(color: isSelected ? Colors.white : Colors.white70, fontSize: 11)),
                        selected: isSelected,
                        selectedColor: const Color(0xFF06B6D4).withValues(alpha: 0.3),
                        checkmarkColor: const Color(0xFF06B6D4),
                        onSelected: (selected) {
                          setState(() {
                            if (selected) {
                              _roles.add(r);
                            } else {
                              _roles.remove(r);
                            }
                          });
                        },
                      );
                    }).toList(),
                  ),
                  const SizedBox(height: 12),
                  DropdownButtonFormField<String>(
                    value: _activeRole?.isEmpty == true ? null : _activeRole,
                    isExpanded: true,
                    style: const TextStyle(color: Colors.white),
                    decoration: const InputDecoration(labelText: 'Rôle actif', labelStyle: TextStyle(color: Colors.white70)),
                    dropdownColor: const Color(0xFF111827),
                    items: [
                      const DropdownMenuItem(value: '', child: Text('— Aucun —')),
                      ...['ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR', 'MEMBRE']
                          .map((e) => DropdownMenuItem(value: e, child: Text(e))),
                    ],
                    onChanged: (v) => setState(() => _activeRole = v?.isEmpty == true ? null : v),
                  ),
                ],
              ),
            ),

            const SizedBox(height: 24),

            FilledButton(
              onPressed: _saving ? null : _createUser,
              style: FilledButton.styleFrom(
                backgroundColor: const Color(0xFF06B6D4),
                padding: const EdgeInsets.symmetric(vertical: 16),
              ),
              child: _saving
                  ? const SizedBox(width: 20, height: 20, child: CircularProgressIndicator(strokeWidth: 2, color: Colors.white))
                  : const Text('Créer l\'utilisateur', style: TextStyle(fontSize: 16)),
            ),
          ],
        ),
      ),
    );
  }

  @override
  void dispose() {
    _emailController.dispose();
    _prenomController.dispose();
    _nomController.dispose();
    _passwordController.dispose();
    _telephoneController.dispose();
    super.dispose();
  }
}