import 'package:flutter/material.dart';
import '../../../data/services/api_service.dart';
import '../../widgets/glass_theme.dart';
import '../../widgets/app_drawer.dart';
import '../../../../l10n/app_localizations.dart';

class FamilyCreateScreen extends StatefulWidget {
  const FamilyCreateScreen({super.key});

  @override
  State<FamilyCreateScreen> createState() => _FamilyCreateScreenState();
}

class _FamilyCreateScreenState extends State<FamilyCreateScreen> {
  final _apiService = ApiService();
  final _formKey = GlobalKey<FormState>();
  final _nomController = TextEditingController();
  final _newChefPrenomController = TextEditingController();
  final _newChefNomController = TextEditingController();
  final _newChefEmailController = TextEditingController();
  final _newChefTelephoneController = TextEditingController();

  List<dynamic> _potentielsChefs = [];
  String _mode = 'existing'; // 'existing', 'new', 'self'
  String? _chefFamilleId;
  String? _newChefSexe;
  bool _loading = false;
  bool _saving = false;

  @override
  void initState() {
    super.initState();
    _loadChefs();
  }

  Future<void> _loadChefs() async {
    setState(() => _loading = true);
    try {
      final res = await _apiService.get('/users?size=100');
      if (mounted) {
        setState(() {
          _potentielsChefs = (res.data['content'] as List?)
                  ?.where((u) => !(u['estChefDeFamille'] == true && u['familleGereeId'] != null))
                  .toList() ?? [];
          _loading = false;
        });
      }
    } catch (e) {
      if (mounted) setState(() => _loading = false);
    }
  }

  Future<void> _createFamily() async {
    if (!_formKey.currentState!.validate()) return;

    String? chefId;
    if (_mode == 'self') {
      // Get current user ID from auth
      final authRes = await _apiService.get('/users/me');
      chefId = authRes.data['id'] as String?;
    } else if (_mode == 'existing') {
      chefId = _chefFamilleId;
    } else if (_mode == 'new') {
      // The backend handles creating the new chef
      chefId = null;
    }

    if (_mode == 'existing' && (chefId == null || chefId.isEmpty)) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Veuillez sélectionner un chef'), backgroundColor: Colors.red),
      );
      return;
    }

    setState(() => _saving = true);
    try {
      final payload = <String, dynamic>{
        'nom': _nomController.text.trim(),
      };

      if (_mode == 'self' || _mode == 'existing') {
        payload['chefFamilleId'] = chefId;
      } else {
        payload['createNewChef'] = true;
        payload['newChefFirstName'] = _newChefPrenomController.text.trim();
        payload['newChefLastName'] = _newChefNomController.text.trim();
        payload['newChefEmail'] = _newChefEmailController.text.trim();
        payload['newChefPhone'] = _newChefTelephoneController.text.trim().isEmpty
            ? null
            : _newChefTelephoneController.text.trim();
        payload['newChefSexe'] = _newChefSexe;
      }

      await _apiService.post('/families', data: payload);
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('Famille créée avec succès'), backgroundColor: Colors.green),
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
        title: const Text('Nouvelle famille'),
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
                        Text('Nom de la famille', style: TextStyle(color: Colors.white.withValues(alpha: 0.5), fontSize: 12)),
                        const SizedBox(height: 8),
                        TextFormField(
                          controller: _nomController,
                          style: const TextStyle(color: Colors.white),
                          decoration: const InputDecoration(
                            labelText: 'Nom *',
                            hintText: 'Ex: Famille Emmanuel',
                            labelStyle: TextStyle(color: Colors.white70),
                          ),
                          validator: (v) => v == null || v.trim().length < 2 ? 'Le nom doit contenir au moins 2 caractères' : null,
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
                        Text('Chef de famille', style: TextStyle(color: Colors.white.withValues(alpha: 0.5), fontSize: 12)),
                        const SizedBox(height: 8),
                        Row(
                          children: [
                            Expanded(
                              child: _ModeButton(
                                label: 'Me désigner chef',
                                icon: Icons.how_to_reg,
                                selected: _mode == 'self',
                                onTap: () => setState(() {
                                  _mode = 'self';
                                  _chefFamilleId = null;
                                }),
                              ),
                            ),
                            const SizedBox(width: 8),
                            Expanded(
                              child: _ModeButton(
                                label: 'Sélectionner un chef',
                                icon: Icons.check_circle,
                                selected: _mode == 'existing',
                                onTap: () => setState(() {
                                  _mode = 'existing';
                                  _chefFamilleId = null;
                                }),
                              ),
                            ),
                            const SizedBox(width: 8),
                            Expanded(
                              child: _ModeButton(
                                label: 'Créer un nouveau chef',
                                icon: Icons.person_add,
                                selected: _mode == 'new',
                                onTap: () => setState(() {
                                  _mode = 'new';
                                  _chefFamilleId = null;
                                }),
                              ),
                            ),
                          ],
                        ),

                        const SizedBox(height: 16),

                        if (_mode == 'self') ...[
                          GlassCard(
                            padding: const EdgeInsets.all(12),
                            borderColor: const Color(0xFF06B6D4).withValues(alpha: 0.3),
                            child: Row(
                              children: [
                                Container(
                                  padding: const EdgeInsets.all(8),
                                  decoration: BoxDecoration(
                                    gradient: const LinearGradient(colors: [Color(0xFF06B6D4), Color(0xFF3B82F6)]),
                                    borderRadius: BorderRadius.circular(12),
                                  ),
                                  child: const Icon(Icons.how_to_reg, color: Colors.white, size: 20),
                                ),
                                const SizedBox(width: 12),
                                const Expanded(
                                  child: Column(
                                    crossAxisAlignment: CrossAxisAlignment.start,
                                    children: [
                                      Text('Vous serez désigné chef de cette famille', style: TextStyle(color: Colors.white, fontWeight: FontWeight.bold)),
                                      Text('Votre compte sera utilisé', style: TextStyle(color: Colors.white70, fontSize: 12)),
                                    ],
                                  ),
                                ),
                              ],
                            ),
                          ),
                        ] else if (_mode == 'existing') ...[
                          if (_potentielsChefs.isEmpty)
                            Text(
                              'Aucun chef disponible. Basculez sur "Créer un nouveau chef".',
                              style: TextStyle(color: Colors.amber, fontSize: 12),
                            )
                          else
                            DropdownButtonFormField<String>(
                              value: _chefFamilleId?.isEmpty == true ? null : _chefFamilleId,
                              isExpanded: true,
                              style: const TextStyle(color: Colors.white),
                              decoration: const InputDecoration(labelText: 'Chef de famille *', labelStyle: TextStyle(color: Colors.white70)),
                              dropdownColor: const Color(0xFF111827),
                              items: _potentielsChefs.map((u) {
                                final nom = '${u['firstName'] ?? ''} ${u['lastName'] ?? ''}'.trim();
                                return DropdownMenuItem(
                                  value: u['id'] as String,
                                  child: Text('${nom.isEmpty ? u['email'] : nom} (${u['email']})'),
                                );
                              }).toList(),
                              onChanged: (v) => setState(() => _chefFamilleId = v),
                              validator: (v) => v == null ? 'Sélectionnez un chef' : null,
                            ),
                        ] else if (_mode == 'new') ...[
                          TextFormField(
                            controller: _newChefPrenomController,
                            style: const TextStyle(color: Colors.white),
                            decoration: const InputDecoration(labelText: 'Prénom *', labelStyle: TextStyle(color: Colors.white70)),
                            validator: (v) => v == null || v.trim().isEmpty ? 'Requis' : null,
                          ),
                          const SizedBox(height: 12),
                          TextFormField(
                            controller: _newChefNomController,
                            style: const TextStyle(color: Colors.white),
                            decoration: const InputDecoration(labelText: 'Nom *', labelStyle: TextStyle(color: Colors.white70)),
                            validator: (v) => v == null || v.trim().isEmpty ? 'Requis' : null,
                          ),
                          const SizedBox(height: 12),
                          TextFormField(
                            controller: _newChefEmailController,
                            style: const TextStyle(color: Colors.white),
                            decoration: const InputDecoration(labelText: 'Email *', labelStyle: TextStyle(color: Colors.white70)),
                            keyboardType: TextInputType.emailAddress,
                            validator: (v) => v == null || v.trim().isEmpty || !v.contains('@') ? 'Email valide requis' : null,
                          ),
                          const SizedBox(height: 12),
                          TextFormField(
                            controller: _newChefTelephoneController,
                            style: const TextStyle(color: Colors.white),
                            decoration: const InputDecoration(labelText: 'Téléphone', labelStyle: TextStyle(color: Colors.white70)),
                            keyboardType: TextInputType.phone,
                          ),
                          const SizedBox(height: 12),
                          DropdownButtonFormField<String>(
                            value: _newChefSexe?.isEmpty == true ? null : _newChefSexe,
                            isExpanded: true,
                            style: const TextStyle(color: Colors.white),
                            decoration: const InputDecoration(labelText: 'Sexe', labelStyle: TextStyle(color: Colors.white70)),
                            dropdownColor: const Color(0xFF111827),
                            items: ['M', 'F'].map((e) => DropdownMenuItem(value: e, child: Text(e == 'M' ? 'Masculin' : 'Féminin'))).toList(),
                            onChanged: (v) => setState(() => _newChefSexe = v),
                          ),
                          const SizedBox(height: 8),
                          Text(
                            'Le compte du chef sera créé avec le rôle CHEF_DE_FAMILLE',
                            style: TextStyle(color: Colors.white.withValues(alpha: 0.4), fontSize: 11),
                          ),
                        ],
                      ],
                    ),
                  ),

                  const SizedBox(height: 24),

                  FilledButton(
                    onPressed: _saving ? null : _createFamily,
                    style: FilledButton.styleFrom(
                      backgroundColor: const Color(0xFF06B6D4),
                      padding: const EdgeInsets.symmetric(vertical: 16),
                    ),
                    child: _saving
                        ? const SizedBox(width: 20, height: 20, child: CircularProgressIndicator(strokeWidth: 2, color: Colors.white))
                        : const Text('Créer la famille', style: TextStyle(fontSize: 16)),
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