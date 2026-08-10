import 'package:flutter/material.dart';
import '../../../app.dart';
import '../../../data/services/api_service.dart';
import '../../widgets/document_create_dialog.dart';
import '../../widgets/glass_theme.dart';
import 'transfer_labels.dart';

class TransferDetailScreen extends StatefulWidget {
  final String transferId;
  const TransferDetailScreen({super.key, required this.transferId});

  @override
  State<TransferDetailScreen> createState() => _TransferDetailScreenState();
}

class _TransferDetailScreenState extends State<TransferDetailScreen> {
  final _apiService = ApiService();
  Map<String, dynamic>? _data;
  List<dynamic> _history = [];
  List<Map<String, dynamic>> _files = [];
  bool _isLoading = true;

  @override
  void initState() { super.initState(); _load(); }

  Future<void> _load() async {
    try {
      final res = await _apiService.get('/transfers/${widget.transferId}');
      final hist = await _apiService.get('/transfers/${widget.transferId}/history');
      if (mounted) {
        setState(() {
        _data = res.data as Map<String, dynamic>;
        _history = hist.data as List<dynamic>;
        _isLoading = false;
      });
      }
    } catch (e) { if (mounted) setState(() => _isLoading = false); }
    // Documents disponibles (best-effort, pour l'édition des pièces jointes).
    try {
      final files = await _apiService.get('/files', params: {'size': '100'});
      if (mounted) {
        setState(() => _files = ((files.data as Map)['content'] as List).map((e) => e as Map<String, dynamic>).toList());
      }
    } catch (_) {}
  }

  Future<void> _action(String action, {Map<String, dynamic>? body}) async {
    try {
      await _apiService.post('/transfers/${widget.transferId}/$action', data: body ?? {});
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('Opération effectuée')));
      _load();
    } catch (_) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('Erreur lors de l\u2019opération')));
      }
    }
  }

  Future<void> _confirmCancel() async {
    final ok = await showDialog<bool>(
      context: context,
      builder: (ctx) => AlertDialog(
        title: const Text('Annuler la demande ?'),
        actions: [
          TextButton(onPressed: () => Navigator.pop(ctx, false), child: const Text('Non')),
          TextButton(onPressed: () => Navigator.pop(ctx, true), child: const Text('Oui')),
        ],
      ),
    );
    if (ok == true) _action('cancel');
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Demande de transfert')),
      body: _isLoading || _data == null
          ? const ShimmerLoading(itemCount: 4)
          : RefreshIndicator(
              onRefresh: _load,
              child: ListView(padding: const EdgeInsets.all(16), children: [
                _buildHeader(),
                const SizedBox(height: 16),
                _buildPiecesJointes(),
                const SizedBox(height: 16),
                _buildCircuit(),
                const SizedBox(height: 16),
                _buildHistory(),
              ]),
            ),
    );
  }

  Widget _buildHeader() {
    final t = _data!['transfert'] as Map<String, dynamic>;
    final statut = t['statut'] ?? '';
    final color = transferStatusColor(statut);
    final ancienne = (t['ancienneAffectation'] as Map?)?['nom'];
    final nouvelle = (t['nouvelleAffectation'] as Map?)?['nom'];

    final estDemandeur = t['demandeurId'] == AuthState().userId;
    final peutSoumettre = (statut == 'BROUILLON' || statut == 'SOUMIS') && estDemandeur;
    final peutAnnuler = ['BROUILLON', 'SOUMIS', 'EN_ATTENTE_VALIDATION', 'VALIDATION_PARTIELLE'].contains(statut) && estDemandeur;
    final peutValider = _data!['peutValider'] == true &&
        !['EXECUTE', 'REFUSE', 'ANNULE', 'ARCHIVE'].contains(statut) &&
        !['BROUILLON', 'SOUMIS'].contains(statut);

    return GlassCard(
      padding: const EdgeInsets.all(16),
      child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
        Row(children: [
          Expanded(child: Text(transferTypeLabel(t['type'] ?? ''), style: const TextStyle(color: Colors.white, fontSize: 16, fontWeight: FontWeight.bold))),
          StatusBadge(label: transferStatusLabel(statut), color: color),
        ]),
        const SizedBox(height: 12),
        _info('Personne concernée', t['personneNom'] ?? '—'),
        _info('Priorité', kPrioriteLabels[t['priorite']] ?? '—'),
        if (ancienne != null) _info('Affectation actuelle', ancienne),
        _info('Nouvelle affectation', nouvelle ?? '—'),
        const SizedBox(height: 8),
        Text('Justification', style: TextStyle(color: Colors.white.withValues(alpha: 0.4), fontSize: 11)),
        const SizedBox(height: 2),
        Text(t['justification'] ?? '', style: const TextStyle(color: Colors.white, fontSize: 13)),
        if (t['commentaires'] != null) ...[
          const SizedBox(height: 8),
          Text('Commentaires : ${t['commentaires']}', style: TextStyle(color: Colors.white.withValues(alpha: 0.6), fontSize: 12)),
        ],
        if (t['delaiLimite'] != null) ...[
          const SizedBox(height: 8),
          Text('Traitement attendu avant le ${t['delaiLimite']}', style: const TextStyle(color: Colors.orangeAccent, fontSize: 11)),
        ],
        if (peutSoumettre || peutAnnuler || peutValider) ...[
          const SizedBox(height: 12),
          Wrap(spacing: 8, runSpacing: 8, children: [
            if (peutSoumettre)
              FilledButton(
                onPressed: () => _action('submit'),
                child: const Row(mainAxisSize: MainAxisSize.min, children: [Icon(Icons.send, size: 16), SizedBox(width: 6), Text('Soumettre')]),
              ),
            if (peutAnnuler)
              OutlinedButton(
                style: OutlinedButton.styleFrom(foregroundColor: Colors.redAccent, side: BorderSide(color: Colors.redAccent.withValues(alpha: 0.4))),
                onPressed: _confirmCancel,
                child: const Text('Annuler'),
              ),
            if (peutValider) ...[
              FilledButton(
                style: FilledButton.styleFrom(backgroundColor: Colors.green.shade600),
                onPressed: () => _openDecisionDialog('APPROBATION'),
                child: const Text('Approuver'),
              ),
              OutlinedButton(
                style: OutlinedButton.styleFrom(foregroundColor: Colors.redAccent, side: BorderSide(color: Colors.redAccent.withValues(alpha: 0.4))),
                onPressed: () => _openDecisionDialog('REFUS'),
                child: const Text('Refuser'),
              ),
              OutlinedButton(
                onPressed: () => _openDecisionDialog('DEMANDE_INFORMATIONS'),
                child: const Text('Infos'),
              ),
              OutlinedButton(
                onPressed: () => _openDecisionDialog('RENVOI_CORRECTION'),
                child: const Text('Correction'),
              ),
            ],
          ]),
        ],
      ]),
    );
  }

  /// Dialogue de décision motivée (approbation, refus, infos, correction).
  Future<void> _openDecisionDialog(String decision) async {
    final controller = TextEditingController();
    final motivRequired = decision != 'APPROBATION';
    final confirmed = await showDialog<bool>(
      context: context,
      builder: (ctx) => StatefulBuilder(
        builder: (ctx, setDialogState) => AlertDialog(
          title: Text(kDecisionLabels[decision] ?? decision),
          content: Column(mainAxisSize: MainAxisSize.min, crossAxisAlignment: CrossAxisAlignment.start, children: [
            const SizedBox(height: 4),
            TextField(
              controller: controller,
              maxLines: 3,
              decoration: InputDecoration(
                labelText: motivRequired ? 'Motivation *' : 'Motivation (optionnelle)',
                hintText: decision == 'REFUS' ? 'Raisons du refus...' : 'Observations...',
              ),
            ),
            const SizedBox(height: 12),
            Text(
              decision == 'APPROBATION'
                  ? 'La demande avancera dans le circuit de validation.'
                  : decision == 'REFUS'
                      ? 'La demande sera définitivement refusée et notifiée.'
                      : 'La demande sera renvoyée au demandeur.',
              style: TextStyle(color: Colors.white.withValues(alpha: 0.5), fontSize: 11),
            ),
          ]),
          actions: [
            TextButton(onPressed: () => Navigator.pop(ctx, false), child: const Text('Annuler')),
            FilledButton(
              onPressed: () {
                if (motivRequired && controller.text.trim().isEmpty) {
                  setDialogState(() {}); // force re-render ; champ requis signalé par le label
                  return;
                }
                Navigator.pop(ctx, true);
              },
              child: const Text('Confirmer'),
            ),
          ],
        ),
      ),
    );
    if (confirmed == true) {
      final motivation = controller.text.trim();
      await _action('decide', body: {
        'decision': decision,
        if (motivation.isNotEmpty) 'motivation': motivation,
      });
    }
  }

  Widget _info(String label, String value) {
    return Padding(
      padding: const EdgeInsets.only(bottom: 4),
      child: Row(crossAxisAlignment: CrossAxisAlignment.start, children: [
        SizedBox(width: 130, child: Text(label, style: TextStyle(color: Colors.white.withValues(alpha: 0.4), fontSize: 12))),
        Expanded(child: Text(value, style: const TextStyle(color: Colors.white, fontSize: 13, fontWeight: FontWeight.w500))),
      ]),
    );
  }

  Widget _buildPiecesJointes() {
    final pieces = (_data!['piecesJointes'] as List? ?? []);
    final t = _data!['transfert'] as Map<String, dynamic>;
    final estDemandeur = t['demandeurId'] == AuthState().userId;
    final peutModifier = t['statut'] == 'BROUILLON' && estDemandeur;
    if (pieces.isEmpty && !peutModifier) return const SizedBox.shrink();
    return GlassCard(
      padding: const EdgeInsets.all(16),
      child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
        Row(children: [
          Icon(Icons.attach_file, color: Colors.white.withValues(alpha: 0.5), size: 18),
          const SizedBox(width: 8),
          Expanded(child: Text('Pièces jointes (${pieces.length})',
              style: const TextStyle(color: Colors.white, fontSize: 15, fontWeight: FontWeight.bold))),
          if (peutModifier)
            TextButton(
              onPressed: () => _editPieces(pieces),
              child: const Text('Modifier'),
            ),
        ]),
        const SizedBox(height: 8),
        if (pieces.isEmpty)
          Text('Aucune pièce jointe.',
              style: TextStyle(color: Colors.white.withValues(alpha: 0.4), fontSize: 12))
        else
          ...pieces.map((p) {
            final m = p as Map<String, dynamic>;
            return Padding(
              padding: const EdgeInsets.only(bottom: 4),
              child: Row(children: [
                Icon(Icons.insert_drive_file, size: 14, color: Colors.white.withValues(alpha: 0.4)),
                const SizedBox(width: 8),
                Expanded(child: Text(m['nom'] ?? m['fileId'] ?? '—',
                    style: TextStyle(color: Colors.white.withValues(alpha: 0.7), fontSize: 12))),
              ]),
            );
          }),
      ]),
    );
  }

  /// Édition des pièces jointes d'un brouillon : sélection multi (pré-cochée)
  /// puis remplacement complet via PUT (le backend remplace la liste).
  Future<void> _editPieces(List<dynamic> pieces) async {
    // Pas de garde sur _files vide : le dialogue propose « Créer un document »
    // et permet ainsi d'ajouter une pièce jointe même si le module est vide.
    final current = pieces.map((p) => (p as Map<String, dynamic>)['fileId'] as String).toSet();
    final selection = {...current};

    final validated = await showDialog<bool>(
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
                      selection.add(createdId);
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
                                value: selection.contains(f['id']),
                                onChanged: (checked) => setDialogState(() {
                                  if (checked == true) {
                                    selection.add(f['id'] as String);
                                  } else {
                                    selection.remove(f['id']);
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
            TextButton(onPressed: () => Navigator.pop(ctx, false), child: const Text('Annuler')),
            FilledButton(
              onPressed: () => Navigator.pop(ctx, true),
              child: Text('Valider (${selection.length})', style: const TextStyle(color: Colors.white)),
            ),
          ],
        ),
      ),
    );

    if (validated == true) {
      try {
        await _apiService.put('/transfers/${widget.transferId}', data: {
          'fichierIds': selection.toList(),
        });
        if (!mounted) return;
        _showMessage('Pièces jointes mises à jour');
        _load();
      } catch (_) {
        if (mounted) _showMessage('Erreur lors de la mise à jour des pièces jointes');
      }
    }
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

  void _showMessage(String msg) {
    ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(msg)));
  }

  Widget _buildCircuit() {
    final etapes = (_data!['etapes'] as List?) ?? [];
    final decisions = (_data!['decisions'] as List?) ?? [];

    return GlassCard(
      padding: const EdgeInsets.all(16),
      child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
        Row(children: [
          Icon(Icons.verified_user, color: AppColors.primaryLight, size: 18),
          const SizedBox(width: 8),
          const Text('Circuit de validation', style: TextStyle(color: Colors.white, fontSize: 15, fontWeight: FontWeight.bold)),
        ]),
        const SizedBox(height: 12),
        if (etapes.isEmpty)
          Text('Aucune validation requise — exécution automatique dès la soumission.',
              style: TextStyle(color: Colors.white.withValues(alpha: 0.5), fontSize: 12))
        else
          ...List.generate(etapes.length, (i) {
            final s = etapes[i] as Map<String, dynamic>;
            final validee = s['validee'] == true;
            return Padding(
              padding: const EdgeInsets.only(bottom: 8),
              child: Row(crossAxisAlignment: CrossAxisAlignment.start, children: [
                Container(
                  width: 24, height: 24,
                  decoration: BoxDecoration(
                    color: validee ? Colors.green : Colors.white.withValues(alpha: 0.08),
                    shape: BoxShape.circle,
                  ),
                  child: Icon(validee ? Icons.check : Icons.circle_outlined, size: 14, color: validee ? Colors.white : Colors.white38),
                ),
                const SizedBox(width: 10),
                Expanded(child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
                  Text(s['label'] ?? 'Étape', style: const TextStyle(color: Colors.white, fontSize: 13)),
                  Text((s['rolesValidateurs'] as List).join(' / '), style: TextStyle(color: Colors.white.withValues(alpha: 0.4), fontSize: 11)),
                ])),
              ]),
            );
          }),
        if (decisions.isNotEmpty) ...[
          const SizedBox(height: 8),
          const Divider(),
          const SizedBox(height: 4),
          Text('Décisions', style: TextStyle(color: Colors.white.withValues(alpha: 0.5), fontSize: 12)),
          const SizedBox(height: 6),
          ...decisions.map((d) {
            final m = d as Map<String, dynamic>;
            return Padding(
              padding: const EdgeInsets.only(bottom: 6),
              child: Row(children: [
                Icon(d['decision'] == 'APPROBATION' ? Icons.check_circle : Icons.cancel, size: 16,
                    color: d['decision'] == 'APPROBATION' ? Colors.green : Colors.red),
                const SizedBox(width: 8),
                Expanded(child: Text(
                  '${m['validateurNom'] ?? '—'} · ${kDecisionLabels[m['decision']] ?? m['decision']}'
                  '${m['motivation'] != null && (m['motivation'] as String).isNotEmpty ? ' — « ${m['motivation']} »' : ''}',
                  style: TextStyle(color: Colors.white.withValues(alpha: 0.7), fontSize: 12),
                )),
              ]),
            );
          }),
        ],
      ]),
    );
  }

  Widget _buildHistory() {
    return GlassCard(
      padding: const EdgeInsets.all(16),
      child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
        Row(children: [
          Icon(Icons.history, color: Colors.lightBlueAccent, size: 18),
          const SizedBox(width: 8),
          const Text('Historique', style: TextStyle(color: Colors.white, fontSize: 15, fontWeight: FontWeight.bold)),
        ]),
        const SizedBox(height: 12),
        if (_history.isEmpty)
          Text('Aucun événement', style: TextStyle(color: Colors.white.withValues(alpha: 0.4), fontSize: 12))
        else
          ...List.generate(_history.length, (i) {
            final h = _history[i] as Map<String, dynamic>;
            final nouveau = h['nouveauStatut'];
            final ancien = h['ancienStatut'];
            return Padding(
              padding: const EdgeInsets.only(bottom: 10),
              child: Row(crossAxisAlignment: CrossAxisAlignment.start, children: [
                Padding(
                  padding: const EdgeInsets.only(top: 5),
                  child: Container(width: 8, height: 8, decoration: BoxDecoration(color: AppColors.primaryLight, shape: BoxShape.circle)),
                ),
                const SizedBox(width: 10),
                Expanded(child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
                  Text(h['action'] ?? '', style: const TextStyle(color: Colors.white, fontSize: 13, fontWeight: FontWeight.w600)),
                  Text(
                    '${h['utilisateurNom'] ?? 'Système'}${h['roleActif'] != null ? ' · ${h['roleActif']}' : ''} · ${(h['createdAt'] ?? '').toString().substring(0, 16).replaceAll('T', ' ')}',
                    style: TextStyle(color: Colors.white.withValues(alpha: 0.35), fontSize: 10),
                  ),
                  if (h['commentaire'] != null)
                    Text('« ${h['commentaire']} »', style: TextStyle(color: Colors.white.withValues(alpha: 0.55), fontSize: 11)),
                  if (nouveau != null)
                    Text('${ancien != null ? '${transferStatusLabel(ancien)} → ' : ''}${transferStatusLabel(nouveau)}',
                        style: TextStyle(color: Colors.white.withValues(alpha: 0.4), fontSize: 10)),
                ])),
              ]),
            );
          }),
      ]),
    );
  }

  @override
  void dispose() {
    super.dispose();
  }
}
