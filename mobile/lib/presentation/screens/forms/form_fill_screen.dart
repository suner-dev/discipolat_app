import 'package:flutter/material.dart';
import '../../../data/services/api_service.dart';

/// P3 #91 — Remplissage de formulaire avec indication de progression (Stepper).
/// Chaque étape valide ses champs avant de passer à la suivante ; une barre de
/// progression globale et le libellé « Étape x/y » sont affichés en permanence.
class FormFillScreen extends StatefulWidget {
  const FormFillScreen({super.key, this.apiService, this.form});

  final ApiService? apiService;
  final Map<String, dynamic>? form;

  @override
  State<FormFillScreen> createState() => _FormFillScreenState();
}

class _FormFillScreenState extends State<FormFillScreen> {
  late final ApiService _api = widget.apiService ?? ApiService();
  int _currentStep = 0;
  bool _submitting = false;

  final _nameCtrl = TextEditingController();
  final _emailCtrl = TextEditingController();
  final _phoneCtrl = TextEditingController();
  String _subject = 'Satisfaction culte';
  String _satisfaction = 'Bien';
  final _commentsCtrl = TextEditingController();
  bool _accepted = false;

  final _formKey = GlobalKey<FormState>();

  @override
  void dispose() {
    _nameCtrl.dispose();
    _emailCtrl.dispose();
    _phoneCtrl.dispose();
    _commentsCtrl.dispose();
    super.dispose();
  }

  double get _progress => (_currentStep + 1) / 3;

  Future<void> _submit() async {
    setState(() => _submitting = true);
    try {
      await _api.post('/forms/${widget.form?['id'] ?? 'general'}/responses', data: {
        'nom': _nameCtrl.text.trim(),
        'email': _emailCtrl.text.trim(),
        'telephone': _phoneCtrl.text.trim(),
        'sujet': _subject,
        'satisfaction': _satisfaction,
        'commentaires': _commentsCtrl.text.trim(),
      });
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('Formulaire envoyé ✅')));
        Navigator.pop(context);
      }
    } catch (_) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('Échec de l\'envoi — réessayez')));
      }
    } finally {
      if (mounted) setState(() => _submitting = false);
    }
  }

  bool _validateCurrentStep() {
    switch (_currentStep) {
      case 0:
        return _formKey.currentState?.validate() ?? false;
      case 1:
        return true; // choix multiples toujours valides
      default:
        return true;
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Formulaire'),
        backgroundColor: Colors.teal.shade700,
        foregroundColor: Colors.white,
      ),
      body: Column(
        children: [
          // P3 #91 — Indication de progression permanente
          Padding(
            padding: const EdgeInsets.fromLTRB(16, 12, 16, 0),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Row(
                  mainAxisAlignment: MainAxisAlignment.spaceBetween,
                  children: [
                    Text('Étape ${_currentStep + 1}/3', style: const TextStyle(fontSize: 12, color: Colors.black54)),
                    Text('${(_progress * 100).round()} %', style: TextStyle(fontSize: 12, fontWeight: FontWeight.bold, color: Colors.teal.shade700)),
                  ],
                ),
                const SizedBox(height: 4),
                LinearProgressIndicator(value: _progress, minHeight: 6, borderRadius: BorderRadius.circular(3)),
              ],
            ),
          ),
          Expanded(
            child: Form(
              key: _formKey,
              child: Stepper(
                currentStep: _currentStep,
                onStepTapped: (i) => setState(() => _currentStep = i),
                onStepContinue: () {
                  if (!_validateCurrentStep()) return;
                  if (_currentStep < 2) {
                    setState(() => _currentStep += 1);
                  } else {
                    _submit();
                  }
                },
                onStepCancel: () {
                  if (_currentStep > 0) {
                    setState(() => _currentStep -= 1);
                  } else {
                    Navigator.pop(context);
                  }
                },
                controlsBuilder: (context, details) => Padding(
                  padding: const EdgeInsets.only(top: 12),
                  child: Row(children: [
                    ElevatedButton(
                      style: ElevatedButton.styleFrom(backgroundColor: Colors.teal.shade700, foregroundColor: Colors.white),
                      onPressed: _submitting ? null : details.onStepContinue,
                      child: Text(_currentStep == 2 ? 'Envoyer' : 'Continuer'),
                    ),
                    const SizedBox(width: 8),
                    TextButton(onPressed: _submitting ? null : details.onStepCancel, child: Text(_currentStep == 0 ? 'Annuler' : 'Retour')),
                  ]),
                ),
                steps: [
                  Step(
                    title: const Text('Vos coordonnées'),
                    isActive: _currentStep >= 0,
                    state: _currentStep > 0 ? StepState.complete : StepState.editing,
                    content: Column(children: [
                      TextFormField(controller: _nameCtrl, decoration: const InputDecoration(labelText: 'Nom complet *', border: OutlineInputBorder()), validator: (v) => (v == null || v.trim().isEmpty) ? 'Nom requis' : null),
                      const SizedBox(height: 10),
                      TextFormField(controller: _emailCtrl, keyboardType: TextInputType.emailAddress, decoration: const InputDecoration(labelText: 'Email', border: OutlineInputBorder()), validator: (v) { if (v != null && v.isNotEmpty && !v.contains('@')) return 'Email invalide'; return null; }),
                      const SizedBox(height: 10),
                      TextFormField(controller: _phoneCtrl, keyboardType: TextInputType.phone, decoration: const InputDecoration(labelText: 'Téléphone', border: OutlineInputBorder())),
                    ]),
                  ),
                  Step(
                    title: const Text('Votre avis'),
                    isActive: _currentStep >= 1,
                    state: _currentStep > 1 ? StepState.complete : StepState.editing,
                    content: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
                      DropdownButtonFormField<String>(
                        initialValue: _subject,
                        decoration: const InputDecoration(labelText: 'Sujet', border: OutlineInputBorder()),
                        items: ['Satisfaction culte', 'Inscription événement', 'Feedback formation'].map((s) => DropdownMenuItem(value: s, child: Text(s))).toList(),
                        onChanged: (v) => setState(() => _subject = v ?? _subject),
                      ),
                      const SizedBox(height: 10),
                      const Text('Niveau de satisfaction'),
                      Wrap(
                        spacing: 8,
                        children: ['Excellent', 'Bien', 'Moyen', 'Insuffisant'].map((s) => ChoiceChip(
                          label: Text(s),
                          selected: _satisfaction == s,
                          selectedColor: Colors.teal.shade200,
                          onSelected: (_) => setState(() => _satisfaction = s),
                        )).toList(),
                      ),
                    ]),
                  ),
                  Step(
                    title: const Text('Confirmation'),
                    isActive: _currentStep >= 2,
                    state: _currentStep == 2 ? StepState.editing : StepState.indexed,
                    content: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
                      Text('Sujet : $_subject', style: const TextStyle(fontSize: 13)),
                      Text('Satisfaction : $_satisfaction', style: const TextStyle(fontSize: 13)),
                      const SizedBox(height: 10),
                      TextFormField(controller: _commentsCtrl, maxLines: 3, decoration: const InputDecoration(labelText: 'Commentaires libres', border: OutlineInputBorder())),
                      const SizedBox(height: 10),
                      CheckboxListTile(
                        value: _accepted,
                        onChanged: (v) => setState(() => _accepted = v ?? false),
                        title: const Text('Je confirme l\'exactitude des informations', style: TextStyle(fontSize: 13)),
                        controlAffinity: ListTileControlAffinity.leading,
                        contentPadding: EdgeInsets.zero,
                      ),
                    ]),
                  ),
                ],
              ),
            ),
          ),
        ],
      ),
    );
  }
}
