import 'package:flutter/material.dart';
import '../../../data/services/api_service.dart';
import '../../widgets/document_create_dialog.dart';
import '../../widgets/glass_theme.dart';
import 'transfer_labels.dart';

/// Méta par type de transfert : entité « personne concernée » et entité « cible ».
class TransferTypeMeta {
  final String personneType; // SOUL | USER
  final String personneLabel;
  final String targetKind; // FAMILLE | DEPARTEMENT | FAISEUR
  final String targetLabel;
  const TransferTypeMeta(this.personneType, this.personneLabel, this.targetKind, this.targetLabel);
}

const Map<String, TransferTypeMeta> kTypeMeta = {
  'MEMBRE_DEPARTEMENT_TRANSFERT': TransferTypeMeta('SOUL', 'Membre (âme) à transférer', 'DEPARTEMENT', 'Département de destination'),
  'MEMBRE_DEPARTEMENT_AJOUT': TransferTypeMeta('SOUL', 'Membre (âme) à ajouter', 'DEPARTEMENT', "Département d'ajout"),
  'MEMBRE_DEPARTEMENT_RETRAIT': TransferTypeMeta('SOUL', 'Membre (âme) à retirer', 'DEPARTEMENT', 'Département de retrait'),
  'DISCIPLE_FAMILLE_TRANSFERT': TransferTypeMeta('SOUL', 'Disciple (âme) à transférer', 'FAMILLE', 'Famille de destination'),
  'FAISEUR_FAMILLE_TRANSFERT': TransferTypeMeta('USER', 'Faiseur à transférer', 'FAMILLE', 'Famille de destination'),
  'CHEF_FAMILLE_TRANSFERT': TransferTypeMeta('USER', 'Nouveau chef de famille', 'FAMILLE', 'Famille concernée'),
  'FAISEUR_DISCIPLE_CHANGEMENT': TransferTypeMeta('SOUL', 'Disciple (âme)', 'FAISEUR', 'Nouveau faiseur'),
  'RESPONSABLE_DEPARTEMENT_CHANGEMENT': TransferTypeMeta('USER', 'Nouveau responsable', 'DEPARTEMENT', 'Département concerné'),
  'CHEF_ADJOINT_CHANGEMENT': TransferTypeMeta('USER', 'Nouveau chef adjoint', 'FAMILLE', 'Famille concernée'),
};

class TransferCreateScreen extends StatefulWidget {
  const TransferCreateScreen({super.key});

  @override
  State<TransferCreateScreen> createState() => _TransferCreateScreenState();
}

class _TransferCreateScreenState extends State<TransferCreateScreen> {
  final _apiService = ApiService();
  List<Map<String, dynamic>> _configs = [];
  bool _loadingConfigs = true;

  String _type = '';
  String _personneId = '';
  String _targetId = '';
  final _justificationCtrl = TextEditingController();
  final _commentairesCtrl = TextEditingController();
  String _priorite = 'MOYENNE';
  bool _soumettreDirectement = true;
  bool _saving = false;

  List<Map<String, dynamic>> _souls = [];
  List<Map<String, dynamic>> _users = [];
  List<Map<String, dynamic>> _families = [];
  List<Map<String, dynamic>> _departments = [];
  List<Map<String, dynamic>> _files = [];
  final Set<String> _fichierIds = {};

  @override
  void initState() {
    super.initState();
    _loadConfigs();
    _loadBaseData();
  }

  Future<void> _loadConfigs() async {
    try {
      final res = await _apiService.get('/transfers/configurations');
      if (mounted) {
        setState(() {
        _configs = (res.data as List).map((e) => e as Map<String, dynamic>).toList();
        _loadingConfigs = false;
      });
      }
    } catch (e) { if (mounted) setState(() => _loadingConfigs = false); }
  }

  Future<void> _loadBaseData() async {
    try {
      final souls = await _apiService.get('/souls', params: {'size': '200'});
      final users = await _apiService.get('/users', params: {'size': '200'});
      final families = await _apiService.get('/families', params: {'size': '200'});
      final departments = await _apiService.get('/departments', params: {'size': '200'});
      final files = await _apiService.get('/files', params: {'size': '100'});
      if (mounted) {
        setState(() {
        _souls = ((souls.data as Map)['content'] as List).map((e) => e as Map<String, dynamic>).toList();
        _users = ((users.data as Map)['content'] as List).map((e) => e as Map<String, dynamic>).toList();
        _families = ((families.data as Map)['content'] as List).map((e) => e as Map<String, dynamic>).toList();
        _departments = ((departments.data as Map)['content'] as List).map((e) => e as Map<String, dynamic>).toList();
        _files = ((files.data as Map)['content'] as List).map((e) => e as Map<String, dynamic>).toList();
      });
      }
    } catch (_) {}
  }

  TransferTypeMeta? get _meta => _type.isEmpty ? null : kTypeMeta[_type];

  List<Map<String, dynamic>> get _personnes {
    final meta = _meta;
    if (meta == null) return [];
    if (meta.personneType == 'SOUL') return _souls;
    return _users;
  }

  List<Map<String, dynamic>> get _cibles {
    final meta = _meta;
    if (meta == null) return [];
    switch (meta.targetKind) {
      case 'FAMILLE': return _families;
      case 'DEPARTEMENT': return _departments;
      default: return _users.where((u) => (u['roles'] as List? ?? []).contains('FAISEUR') || u['activeRole'] == 'FAISEUR').toList();
    }
  }

  String _entityName(Map<String, dynamic> e) {
    final nom = e['nom'];
    if (nom != null && nom.toString().isNotEmpty) return nom.toString();
    return '${e['firstName'] ?? ''} ${e['lastName'] ?? ''}'.trim();
  }

  /// Circuit de validation réel du type sélectionné (config du workflow).
  String _circuit() {
    if (_type.isEmpty) return '';
    final config = _configs.where((c) => c['type'] == _type).firstOrNull;
    if (config == null) return 'non configuré';
    final etapes = (config['etapes'] as List? ?? []);
    if (etapes.isEmpty) return 'exécution immédiate sans validation';
    return etapes.join(' → ');
  }

  /// Dialogue de sélection des pièces jointes (documents du module Fichiers),
  /// avec création directe d'un document si besoin.
  Future<void> _pickFiles() async {
    await showDialog<void>(
      context: context,
      builder: (ctx) => StatefulBuilder(
        builder: (ctx, setDialogState) => AlertDialog(
          backgroundColor: const Color(0xFF111827),
          title: const Text('Pièces jointes', style: TextStyle(color: Colors.white)),
          content: SizedBox(
            width: double.maxFinite,
            child: ConstrainedBox(
              constraints: const BoxConstraints(maxHeight: 360),
              child: Column(mainAxisSize: MainAxisSize.min, crossAxisAlignment: CrossAxisAlignment.start, children: [
                // Création directe d'un document (module Fichiers)
                TextButton.icon(
                  onPressed: () async {
                    final createdId = await showDocumentCreateDialog(ctx, _apiService);
                    if (createdId != null) {
                      _fichierIds.add(createdId);
                      await _reloadFiles();
                      if (ctx.mounted) setDialogState(() {});
                    }
                  },
                  icon: const Icon(Icons.add, size: 18, color: Colors.greenAccent),
                  label: const Text('Créer un document', style: TextStyle(color: Colors.greenAccent)),
                ),
                const Divider(height: 16),
                Expanded(
                  child: _files.isEmpty
                      ? Center(
                          child: Text('Aucun document dans le module Documents.',
                              style: TextStyle(color: Colors.white.withValues(alpha: 0.6), fontSize: 13)),
                        )
                      : ListView(
                          shrinkWrap: true,
                          children: [
                            for (final f in _files)
                              CheckboxListTile(
                                value: _fichierIds.contains(f['id']),
                                onChanged: (checked) => setDialogState(() {
                                  if (checked == true) {
                                    _fichierIds.add(f['id'] as String);
                                  } else {
                                    _fichierIds.remove(f['id']);
                                  }
                                }),
                                controlAffinity: ListTileControlAffinity.leading,
                                dense: true,
                                activeColor: Colors.green,
                                title: Text('${f['nom'] ?? '—'}',
                                    style: const TextStyle(color: Colors.white, fontSize: 13),
                                    overflow: TextOverflow.ellipsis),
                                secondary: Icon(Icons.insert_drive_file,
                                    size: 18, color: Colors.white.withValues(alpha: 0.4)),
                              ),
                          ],
                        ),
                ),
              ]),
            ),
          ),
          actions: [
            TextButton(
              onPressed: () => Navigator.pop(ctx),
              child: const Text('OK', style: TextStyle(color: Colors.greenAccent)),
            ),
          ],
        ),
      ),
    );
    if (mounted) setState(() {});
  }

  /// Recharge la liste des documents disponibles (après création, par ex.).
  Future<void> _reloadFiles() async {
    try {
      final files = await _apiService.get('/files', params: {'size': '100'});
      if (mounted) {
        setState(() => _files = ((files.data as Map)['content'] as List).map((e) => e as Map<String, dynamic>).toList());
      }
    } catch (_) {}
  }

  Future<void> _create() async {
    if (_type.isEmpty || _personneId.isEmpty || _targetId.isEmpty || _justificationCtrl.text.trim().isEmpty) {
      ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('Formulaire incomplet')));
      return;
    }
    setState(() => _saving = true);
    try {
      final meta = _meta!;
      final cible = _cibles.firstWhere((c) => c['id'] == _targetId);
      final res = await _apiService.post('/transfers', data: {
        'type': _type,
        'personneId': _personneId,
        'personneType': meta.personneType,
        'nouvelleAffectation': {'type': meta.targetKind, 'id': _targetId, 'nom': _entityName(cible)},
        'justification': _justificationCtrl.text.trim(),
        'priorite': _priorite,
        'commentaires': _commentairesCtrl.text.trim().isEmpty ? null : _commentairesCtrl.text.trim(),
        if (_fichierIds.isNotEmpty) 'fichierIds': _fichierIds.toList(),
      });
      if (_soumettreDirectement) {
        await _apiService.post('/transfers/${res.data['id']}/submit');
      }
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(SnackBar(
        content: Text(_soumettreDirectement ? 'Demande soumise au circuit de validation' : 'Brouillon enregistré'),
      ));
      Navigator.pop(context, true);
    } catch (e) {
      if (mounted) {
        setState(() => _saving = false);
        ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('Erreur lors de la création')));
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    final meta = _meta;
    final availableTypes = _configs.where((c) => c['actif'] == true && c['canInitier'] == true).toList();

    return Scaffold(
      appBar: AppBar(title: const Text('Nouvelle demande')),
      body: _loadingConfigs
          ? const ShimmerLoading(itemCount: 3)
          : ListView(padding: const EdgeInsets.all(16), children: [
              GlassCard(
                padding: const EdgeInsets.all(16),
                child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
                  Text('Type de transfert *', style: TextStyle(color: Colors.white.withValues(alpha: 0.5), fontSize: 12)),
                  const SizedBox(height: 6),
                  DropdownButtonFormField<String>(
                    initialValue: _type.isEmpty ? null : _type,
                    isExpanded: true,
                    items: [
                      for (final c in availableTypes)
                        DropdownMenuItem(value: c['type'] as String, child: Text(transferTypeLabel(c['type'] as String))),
                    ],
                    onChanged: (v) => setState(() { _type = v ?? ''; _personneId = ''; _targetId = ''; _fichierIds.clear(); }),
                  ),
                  if (meta != null) ...[
                    const SizedBox(height: 6),
                    Text('Circuit : ${_circuit()}',
                        style: TextStyle(color: Colors.white.withValues(alpha: 0.35), fontSize: 10)),
                  ],
                ]),
              ),
              if (meta != null) ...[
                const SizedBox(height: 12),
                GlassCard(
                  padding: const EdgeInsets.all(16),
                  child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
                    Text('${meta.personneLabel} *', style: TextStyle(color: Colors.white.withValues(alpha: 0.5), fontSize: 12)),
                    const SizedBox(height: 6),
                    DropdownButtonFormField<String>(
                      initialValue: _personneId.isEmpty ? null : _personneId,
                      isExpanded: true,
                      items: [for (final p in _personnes) DropdownMenuItem(value: p['id'] as String, child: Text(_entityName(p), overflow: TextOverflow.ellipsis))],
                      onChanged: (v) => setState(() => _personneId = v ?? ''),
                    ),
                    const SizedBox(height: 12),
                    Text('${meta.targetLabel} *', style: TextStyle(color: Colors.white.withValues(alpha: 0.5), fontSize: 12)),
                    const SizedBox(height: 6),
                    DropdownButtonFormField<String>(
                      initialValue: _targetId.isEmpty ? null : _targetId,
                      isExpanded: true,
                      items: [for (final c in _cibles) DropdownMenuItem(value: c['id'] as String, child: Text(_entityName(c), overflow: TextOverflow.ellipsis))],
                      onChanged: (v) => setState(() => _targetId = v ?? ''),
                    ),
                    const SizedBox(height: 12),
                    Text('Justification détaillée *', style: TextStyle(color: Colors.white.withValues(alpha: 0.5), fontSize: 12)),
                    const SizedBox(height: 6),
                    TextField(
                      controller: _justificationCtrl,
                      maxLines: 3,
                      decoration: const InputDecoration(hintText: 'Motifs pastoraux, organisationnels...'),
                    ),
                    const SizedBox(height: 12),
                    Text('Priorité', style: TextStyle(color: Colors.white.withValues(alpha: 0.5), fontSize: 12)),
                    const SizedBox(height: 6),
                    DropdownButtonFormField<String>(
                      initialValue: _priorite,
                      items: [for (final e in kPrioriteLabels.entries) DropdownMenuItem(value: e.key, child: Text(e.value))],
                      onChanged: (v) => setState(() => _priorite = v ?? 'MOYENNE'),
                    ),
                    const SizedBox(height: 12),
                    TextField(
                      controller: _commentairesCtrl,
                      decoration: const InputDecoration(labelText: 'Commentaires (optionnel)'),
                    ),
                    const SizedBox(height: 12),
                    // Pièces jointes
                    Row(children: [
                      Icon(Icons.attach_file, size: 16, color: Colors.white.withValues(alpha: 0.5)),
                      const SizedBox(width: 8),
                      Expanded(
                        child: Text('Pièces jointes',
                            style: TextStyle(color: Colors.white.withValues(alpha: 0.5), fontSize: 12)),
                      ),
                      if (_fichierIds.isNotEmpty)
                        Container(
                          padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 2),
                          decoration: BoxDecoration(
                            color: Colors.green.withValues(alpha: 0.2),
                            borderRadius: BorderRadius.circular(10),
                          ),
                          child: Text('${_fichierIds.length}',
                              style: const TextStyle(color: Colors.greenAccent, fontSize: 11, fontWeight: FontWeight.bold)),
                        ),
                      TextButton(
                        // Toujours actif : le dialogue permet de créer un document
                        // même si le module Fichiers est vide.
                        onPressed: _pickFiles,
                        child: const Text('Choisir'),
                      ),
                    ]),
                    if (_fichierIds.isNotEmpty) ...[
                      ..._files.where((f) => _fichierIds.contains(f['id'])).map((f) => Padding(
                        padding: const EdgeInsets.only(bottom: 4),
                        child: Row(children: [
                          Icon(Icons.insert_drive_file, size: 14, color: Colors.white.withValues(alpha: 0.4)),
                          const SizedBox(width: 8),
                          Expanded(
                            child: Text('${f['nom'] ?? '—'}',
                                style: TextStyle(color: Colors.white.withValues(alpha: 0.7), fontSize: 12),
                                overflow: TextOverflow.ellipsis),
                          ),
                          GestureDetector(
                            onTap: () => setState(() => _fichierIds.remove(f['id'])),
                            child: Icon(Icons.close, size: 16, color: Colors.redAccent.withValues(alpha: 0.7)),
                          ),
                        ]),
                      )),
                      const SizedBox(height: 4),
                    ],
                    const SizedBox(height: 12),
                    CheckboxListTile(
                      value: _soumettreDirectement,
                      onChanged: (v) => setState(() => _soumettreDirectement = v ?? true),
                      title: Text('Soumettre immédiatement au circuit de validation',
                          style: TextStyle(color: Colors.white.withValues(alpha: 0.7), fontSize: 12)),
                      contentPadding: EdgeInsets.zero,
                      controlAffinity: ListTileControlAffinity.leading,
                    ),
                  ]),
                ),
                const SizedBox(height: 16),
                FilledButton(
                  onPressed: _saving ? null : _create,
                  child: _saving
                      ? const SizedBox(width: 18, height: 18, child: CircularProgressIndicator(strokeWidth: 2, color: Colors.white))
                      : Text(_soumettreDirectement ? 'Créer et soumettre' : 'Enregistrer le brouillon'),
                ),
              ],
            ]),
    );
  }
}
