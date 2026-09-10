import 'package:flutter/material.dart';
import 'package:intl/intl.dart';
import '../../../data/services/api_service.dart';
import '../../widgets/glass_theme.dart';
import '../../widgets/app_drawer.dart';

class SoulCreateScreen extends StatefulWidget {
  final String? prefilledFamilyId;
  final String? prefilledFamilyNom;

  const SoulCreateScreen({
    super.key,
    this.prefilledFamilyId,
    this.prefilledFamilyNom,
  });

  @override
  State<SoulCreateScreen> createState() => _SoulCreateScreenState();
}

class _SoulCreateScreenState extends State<SoulCreateScreen> {
  final _apiService = ApiService();
  final _formKey = GlobalKey<FormState>();
  final _nomController = TextEditingController();
  final _prenomController = TextEditingController();
  final _emailController = TextEditingController();
  final _telephoneController = TextEditingController();
  final _adresseController = TextEditingController();
  final _professionController = TextEditingController();
  DateTime _dateNaissance = DateTime.now().subtract(const Duration(days: 365 * 25));
  DateTime _dateIntegration = DateTime.now();
  DateTime? _dateConversion;
  String _typeDisciple = 'NOUVEL_ARRIVANT';
  int _niveauCroissance = 1;
  String? _faiseurId;
  String? _familleId;
  String? _situationFamiliale;
  String? _etatSpirituel;

  List<dynamic> _faiseurs = [];
  List<dynamic> _families = [];
  bool _loading = false;
  bool _saving = false;

  @override
  void initState() {
    super.initState();
    _loadData();
    if (widget.prefilledFamilyId != null) {
      _familleId = widget.prefilledFamilyId;
    }
  }

  Future<void> _loadData() async {
    setState(() => _loading = true);
    try {
      final futures = await Future.wait([
        _apiService.get('/users?role=FAISEUR&size=100'),
        _apiService.get('/families?size=100'),
      ]);
      if (mounted) {
        setState(() {
          _faiseurs = (futures[0].data['content'] as List?) ?? [];
          _families = (futures[1].data['content'] as List?) ?? [];
          _loading = false;
        });
      }
    } catch (e) {
      if (mounted) setState(() => _loading = false);
    }
  }

  Future<void> _pickDate(BuildContext context, DateTime initial, Function(DateTime) onPicked) async {
    final picked = await showDatePicker(
      context: context,
      initialDate: initial,
      firstDate: DateTime(1900),
      lastDate: DateTime.now(),
      builder: (context, child) => Theme(
        data: ThemeData.dark().copyWith(
          colorScheme: const ColorScheme.dark(
            primary: Color(0xFF06B6D4),
            onPrimary: Colors.white,
            surface: Color(0xFF111827),
            onSurface: Colors.white,
          ),
        ),
        child: child!,
      ),
    );
    if (picked != null) onPicked(picked);
  }

  Future<void> _createSoul() async {
    if (!_formKey.currentState!.validate()) return;
    if (_faiseurId == null) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Veuillez sélectionner un faiseur'), backgroundColor: Colors.red),
      );
      return;
    }

    setState(() => _saving = true);
    try {
      final payload = <String, dynamic>{
        'nom': _nomController.text.trim(),
        'prenom': _prenomController.text.trim().isEmpty ? null : _prenomController.text.trim(),
        'email': _emailController.text.trim().isEmpty ? null : _emailController.text.trim(),
        'telephone': _telephoneController.text.trim().isEmpty ? null : _telephoneController.text.trim(),
        'adresse': _adresseController.text.trim().isEmpty ? null : _adresseController.text.trim(),
        'dateNaissance': _dateNaissance.toIso8601String().split('T')[0],
        'profession': _professionController.text.trim().isEmpty ? null : _professionController.text.trim(),
        'typeDisciple': _typeDisciple,
        'dateIntegration': _dateIntegration.toIso8601String().split('T')[0],
        'dateConversion': _dateConversion?.toIso8601String().split('T')[0],
        'faiseurId': _faiseurId,
        'familleId': _familleId,
        'situationFamiliale': _situationFamiliale,
        'etatSpirituel': _etatSpirituel,
        'niveauCroissance': _niveauCroissance,
      };

      await _apiService.post('/souls', data: payload);
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('Âme créée avec succès'), backgroundColor: Colors.green),
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
        title: const Text('Nouvelle âme'),
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
                  if (widget.prefilledFamilyId != null)
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
                            child: const Icon(Icons.people, color: Colors.white, size: 20),
                          ),
                          const SizedBox(width: 12),
                          Expanded(
                            child: Column(
                              crossAxisAlignment: CrossAxisAlignment.start,
                              children: [
                                Text(
                                  'Nouvelle âme dans ${widget.prefilledFamilyNom ?? 'la famille sélectionnée'}',
                                  style: const TextStyle(color: Colors.white, fontWeight: FontWeight.bold),
                                ),
                                Text(
                                  'La famille est déjà assignée',
                                  style: TextStyle(color: Colors.white.withValues(alpha: 0.5), fontSize: 12),
                                ),
                              ],
                            ),
                          ),
                        ],
                      ),
                    ),

                  const SizedBox(height: 16),

                  // Identité
                  GlassCard(
                    padding: const EdgeInsets.all(16),
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text('Identité', style: TextStyle(color: Colors.white.withValues(alpha: 0.5), fontSize: 12)),
                        const SizedBox(height: 12),
                        TextFormField(
                          controller: _nomController,
                          style: const TextStyle(color: Colors.white),
                          decoration: const InputDecoration(labelText: 'Nom *', labelStyle: TextStyle(color: Colors.white70)),
                          validator: (v) => v == null || v.trim().isEmpty ? 'Nom requis' : null,
                        ),
                        const SizedBox(height: 12),
                        TextFormField(
                          controller: _prenomController,
                          style: const TextStyle(color: Colors.white),
                          decoration: const InputDecoration(labelText: 'Prénom', labelStyle: TextStyle(color: Colors.white70)),
                        ),
                        const SizedBox(height: 12),
                        TextFormField(
                          controller: _emailController,
                          style: const TextStyle(color: Colors.white),
                          decoration: const InputDecoration(labelText: 'Email', labelStyle: TextStyle(color: Colors.white70)),
                          keyboardType: TextInputType.emailAddress,
                          validator: (v) => v != null && v.trim().isNotEmpty && !v.contains('@') ? 'Email invalide' : null,
                        ),
                        const SizedBox(height: 12),
                        TextFormField(
                          controller: _telephoneController,
                          style: const TextStyle(color: Colors.white),
                          decoration: const InputDecoration(labelText: 'Téléphone', labelStyle: TextStyle(color: Colors.white70)),
                          keyboardType: TextInputType.phone,
                        ),
                        const SizedBox(height: 12),
                        TextFormField(
                          controller: _adresseController,
                          style: const TextStyle(color: Colors.white),
                          decoration: const InputDecoration(labelText: 'Adresse', labelStyle: TextStyle(color: Colors.white70)),
                        ),
                        const SizedBox(height: 12),
                        Row(
                          children: [
                            Expanded(
                              child: InkWell(
                                onTap: () => _pickDate(context, _dateNaissance, (d) => setState(() => _dateNaissance = d)),
                                child: InputDecorator(
                                  decoration: const InputDecoration(labelText: 'Date de naissance', labelStyle: TextStyle(color: Colors.white70)),
                                  child: Text(DateFormat('yyyy-MM-dd').format(_dateNaissance), style: const TextStyle(color: Colors.white)),
                                ),
                              ),
                            ),
                            const SizedBox(width: 12),
                            Expanded(
                              child: TextFormField(
                                controller: _professionController,
                                style: const TextStyle(color: Colors.white),
                                decoration: const InputDecoration(labelText: 'Profession', labelStyle: TextStyle(color: Colors.white70)),
                              ),
                            ),
                          ],
                        ),
                        const SizedBox(height: 12),
                        DropdownButtonFormField<String>(
                          value: _situationFamiliale?.isEmpty == true ? null : _situationFamiliale,
                          isExpanded: true,
                          style: const TextStyle(color: Colors.white),
                          decoration: const InputDecoration(labelText: 'Situation familiale', labelStyle: TextStyle(color: Colors.white70)),
                          dropdownColor: const Color(0xFF111827),
                          items: ['CELIBATAIRE', 'MARIE', 'DIVORCE', 'VEUF', 'AUTRE']
                              .map((e) => DropdownMenuItem(value: e, child: Text(e)))
                              .toList(),
                          onChanged: (v) => setState(() => _situationFamiliale = v),
                        ),
                      ],
                    ),
                  ),

                  const SizedBox(height: 16),

                  // Disciple Info
                  GlassCard(
                    padding: const EdgeInsets.all(16),
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text('Information de disciple', style: TextStyle(color: Colors.white.withValues(alpha: 0.5), fontSize: 12)),
                        const SizedBox(height: 12),
                        DropdownButtonFormField<String>(
                          value: _typeDisciple,
                          isExpanded: true,
                          style: const TextStyle(color: Colors.white),
                          decoration: const InputDecoration(labelText: 'Type *', labelStyle: TextStyle(color: Colors.white70)),
                          dropdownColor: const Color(0xFF111827),
                          items: const [
                            DropdownMenuItem(value: 'NOUVEL_ARRIVANT', child: Text('Nouvel arrivant')),
                            DropdownMenuItem(value: 'NOUVEAU_CONVERTI', child: Text('Nouveau converti')),
                          ],
                          onChanged: (v) => setState(() => _typeDisciple = v ?? 'NOUVEL_ARRIVANT'),
                        ),
                        const SizedBox(height: 12),
                        Row(
                          children: [
                            Expanded(
                              child: InkWell(
                                onTap: () => _pickDate(context, _dateIntegration, (d) => setState(() => _dateIntegration = d)),
                                child: InputDecorator(
                                  decoration: const InputDecoration(labelText: "Date d'intégration *", labelStyle: TextStyle(color: Colors.white70)),
                                  child: Text(DateFormat('yyyy-MM-dd').format(_dateIntegration), style: const TextStyle(color: Colors.white)),
                                ),
                              ),
                            ),
                            if (_typeDisciple == 'NOUVEAU_CONVERTI') ...[
                              const SizedBox(width: 12),
                              Expanded(
                                child: InkWell(
                                  onTap: () => _pickDate(context, _dateConversion ?? DateTime.now(), (d) => setState(() => _dateConversion = d)),
                                  child: InputDecorator(
                                    decoration: const InputDecoration(labelText: 'Date de conversion', labelStyle: TextStyle(color: Colors.white70)),
                                    child: Text(_dateConversion != null
                                        ? DateFormat('yyyy-MM-dd').format(_dateConversion!)
                                        : 'Sélectionner', style: const TextStyle(color: Colors.white)),
                                  ),
                                ),
                              ),
                            ],
                          ],
                        ),
                        const SizedBox(height: 12),
                        DropdownButtonFormField<String>(
                          value: _etatSpirituel?.isEmpty == true ? null : _etatSpirituel,
                          isExpanded: true,
                          style: const TextStyle(color: Colors.white),
                          decoration: const InputDecoration(labelText: 'État spirituel', labelStyle: TextStyle(color: Colors.white70)),
                          dropdownColor: const Color(0xFF111827),
                          items: ['NOUVEAU_CONVERTI', 'EN_INTEGRATION', 'ACTIF', 'EN_VEILLE', 'DECROCHE']
                              .map((e) => DropdownMenuItem(value: e, child: Text(e)))
                              .toList(),
                          onChanged: (v) => setState(() => _etatSpirituel = v),
                        ),
                        const SizedBox(height: 12),
                        Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Text('Niveau de croissance (1-5): $_niveauCroissance', style: TextStyle(color: Colors.white.withValues(alpha: 0.5), fontSize: 12)),
                            Slider(
                              value: _niveauCroissance.toDouble(),
                              min: 1,
                              max: 5,
                              divisions: 4,
                              activeColor: const Color(0xFF06B6D4),
                              onChanged: (v) => setState(() => _niveauCroissance = v.round()),
                            ),
                          ],
                        ),
                        const SizedBox(height: 12),
                        DropdownButtonFormField<String>(
                          value: _faiseurId,
                          isExpanded: true,
                          style: const TextStyle(color: Colors.white),
                          decoration: const InputDecoration(labelText: 'Faiseur assigné *', labelStyle: TextStyle(color: Colors.white70)),
                          dropdownColor: const Color(0xFF111827),
                          items: _faiseurs.map((f) {
                            final nom = '${f['firstName'] ?? ''} ${f['lastName'] ?? ''}'.trim();
                            return DropdownMenuItem(value: f['id'] as String, child: Text(nom.isEmpty ? f['email'] ?? '' : nom));
                          }).toList(),
                          onChanged: (v) => setState(() => _faiseurId = v),
                          validator: (v) => v == null ? 'Faiseur requis' : null,
                        ),
                        const SizedBox(height: 12),
                        DropdownButtonFormField<String>(
                          value: _familleId,
                          isExpanded: true,
                          style: const TextStyle(color: Colors.white),
                          decoration: const InputDecoration(labelText: 'Famille (optionnel)', labelStyle: TextStyle(color: Colors.white70)),
                          dropdownColor: const Color(0xFF111827),
                          items: [
                            const DropdownMenuItem(value: '', child: Text('Aucune')),
                            ..._families.map((f) => DropdownMenuItem(value: f['id'] as String, child: Text(f['nom'] ?? ''))),
                          ],
                          onChanged: (v) => setState(() => _familleId = v?.isEmpty == true ? null : v),
                        ),
                      ],
                    ),
                  ),

                  const SizedBox(height: 24),

                  FilledButton(
                    onPressed: _saving ? null : _createSoul,
                    style: FilledButton.styleFrom(
                      backgroundColor: const Color(0xFF06B6D4),
                      padding: const EdgeInsets.symmetric(vertical: 16),
                    ),
                    child: _saving
                        ? const SizedBox(width: 20, height: 20, child: CircularProgressIndicator(strokeWidth: 2, color: Colors.white))
                        : const Text('Créer l\'âme', style: TextStyle(fontSize: 16)),
                  ),
                ],
              ),
            ),
    );
  }

  @override
  void dispose() {
    _nomController.dispose();
    _prenomController.dispose();
    _emailController.dispose();
    _telephoneController.dispose();
    _adresseController.dispose();
    _professionController.dispose();
    super.dispose();
  }
}