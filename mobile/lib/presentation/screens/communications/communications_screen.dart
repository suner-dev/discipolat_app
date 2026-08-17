import 'package:flutter/material.dart';
import '../../../app.dart';
import '../../../data/services/api_service.dart';
import '../../widgets/glass_theme.dart';

/// Outil métier COMMUNICATION — parité web.
///
/// Annonces de l'église avec diffusion ciblée (TOUS / rôle / famille /
/// département). La publication notifie les destinataires (IN_APP).
/// Lecture : tout rôle authentifié (annonces publiées dans sa cible) ;
/// gestion (création / modification / publication / suppression) :
/// ADMIN / PASTEUR.
class CommunicationsScreen extends StatefulWidget {
  const CommunicationsScreen({super.key, this.apiService});

  /// Permet d'injecter un ApiService mocké dans les tests widget.
  final ApiService? apiService;

  @override
  State<CommunicationsScreen> createState() => _CommunicationsScreenState();
}

class _CommunicationsScreenState extends State<CommunicationsScreen> {
  late final ApiService _apiService = widget.apiService ?? ApiService();
  List<Map<String, dynamic>> _published = [];
  List<Map<String, dynamic>> _all = [];
  bool _isLoading = true;

  static const _cibleLabels = {
    'TOUS': 'Toute l’église',
    'ROLE': 'Par rôle',
    'FAMILLE': 'Par famille',
    'DEPARTEMENT': 'Par département',
  };

  bool get _canManage => AuthState().hasActiveRole(['ADMIN', 'PASTEUR']);

  @override
  void initState() {
    super.initState();
    _load();
  }

  Future<void> _load() async {
    try {
      final publishedRes = await _apiService.get('/communications');
      final List<Map<String, dynamic>> published =
          (publishedRes.data as List).map((e) => e as Map<String, dynamic>).toList();
      List<Map<String, dynamic>> all = [];
      if (_canManage) {
        final allRes = await _apiService.get('/communications/admin');
        all = (allRes.data as List).map((e) => e as Map<String, dynamic>).toList();
      }
      if (mounted) {
        setState(() {
          _published = published;
          _all = all;
          _isLoading = false;
        });
      }
    } catch (_) {
      if (mounted) setState(() => _isLoading = false);
    }
  }

  Future<void> _publish(Map<String, dynamic> c) async {
    try {
      final res = await _apiService.post('/communications/admin/${c['id']}/publish');
      final destinataires = (res.data as Map<String, dynamic>?)?['destinataires'];
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text('Annonce publiée et diffusée à $destinataires destinataire(s)')),
        );
      }
      _load();
    } catch (_) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('Erreur lors de la publication')),
        );
      }
    }
  }

  Future<void> _delete(Map<String, dynamic> c) async {
    final ok = await showDialog<bool>(
      context: context,
      builder: (ctx) => AlertDialog(
        title: const Text('Supprimer l’annonce ?'),
        content: Text('${c['titre']}'),
        actions: [
          TextButton(onPressed: () => Navigator.pop(ctx, false), child: const Text('Non')),
          TextButton(onPressed: () => Navigator.pop(ctx, true), child: const Text('Oui')),
        ],
      ),
    );
    if (ok != true) return;
    try {
      await _apiService.delete('/communications/admin/${c['id']}');
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('Annonce supprimée')),
        );
      }
      _load();
    } catch (_) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('Erreur lors de la suppression')),
        );
      }
    }
  }

  Future<void> _openEditor([Map<String, dynamic>? edit]) async {
    final payload = await showModalBottomSheet<Map<String, dynamic>>(
      context: context,
      isScrollControlled: true,
      backgroundColor: Colors.transparent,
      builder: (c) => _CommunicationSheet(edit: edit, apiService: _apiService),
    );
    if (payload == null) return;
    try {
      if (edit != null) {
        await _apiService.put('/communications/admin/${edit['id']}', data: payload);
        if (mounted) {
          ScaffoldMessenger.of(context).showSnackBar(
            const SnackBar(content: Text('Annonce modifiée')),
          );
        }
      } else {
        await _apiService.post('/communications/admin', data: payload);
        if (mounted) {
          ScaffoldMessenger.of(context).showSnackBar(
            const SnackBar(content: Text('Annonce créée')),
          );
        }
      }
      _load();
    } catch (_) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text("Échec de l'enregistrement")),
        );
      }
    }
  }

  String _cibleLabel(Object? cible) => _cibleLabels[cible] ?? '$cible';

  Widget _cibleChip(Map<String, dynamic> c) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 6, vertical: 2),
      decoration: BoxDecoration(
        color: AppColors.primary.withValues(alpha: 0.12),
        borderRadius: BorderRadius.circular(6),
      ),
      child: Text(
        _cibleLabel(c['cible']) + (c['cible'] == 'ROLE' ? ' : ${(c['roles'] as List?)?.join(', ') ?? ''}' : ''),
        style: TextStyle(color: AppColors.primaryLight, fontSize: 8, fontWeight: FontWeight.w600),
      ),
    );
  }

  String _statutLabel(Object? statut) {
    switch (statut) {
      case 'PUBLIEE': return 'Publiée';
      case 'ARCHIVEE': return 'Archivée';
      default: return 'Brouillon';
    }
  }

  Color _statutColor(Object? statut) {
    switch (statut) {
      case 'PUBLIEE': return const Color(0xFF22C55E);
      case 'ARCHIVEE': return const Color(0xFFEF4444);
      default: return Colors.white54;
    }
  }

  String _formatDate(Object? iso) {
    final s = '$iso';
    if (s.isEmpty) return '';
    return s.substring(0, 10);
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Annonces')),
      floatingActionButton: _canManage
          ? FloatingActionButton.extended(
              onPressed: () => _openEditor(),
              backgroundColor: AppColors.primary,
              foregroundColor: Colors.white,
              icon: const Icon(Icons.add),
              label: const Text('Annonce'),
            )
          : null,
      body: _isLoading
          ? const ShimmerLoading(itemCount: 4)
          : RefreshIndicator(
              onRefresh: _load,
              child: ListView(
                padding: const EdgeInsets.all(16),
                children: [
                  if (_canManage) ...[
                    Row(children: [
                      Icon(Icons.campaign_rounded, color: AppColors.primaryLight, size: 16),
                      const SizedBox(width: 6),
                      Text('Gestion des annonces',
                          style: TextStyle(color: Colors.white.withValues(alpha: 0.8), fontSize: 12, fontWeight: FontWeight.bold)),
                    ]),
                    const SizedBox(height: 8),
                    if (_all.isEmpty)
                      Padding(
                        padding: const EdgeInsets.symmetric(vertical: 20),
                        child: Center(
                          child: Text('Aucune annonce. Créez la première avec le bouton +.',
                              style: TextStyle(color: Colors.white.withValues(alpha: 0.4), fontSize: 12)),
                        ),
                      )
                    else
                      ..._all.map(_adminCard),
                    const SizedBox(height: 20),
                  ],
                  if (_published.isEmpty)
                    Padding(
                      padding: const EdgeInsets.only(top: 80),
                      child: Column(children: [
                        const Icon(Icons.campaign_outlined, size: 56, color: Colors.white24),
                        const SizedBox(height: 12),
                        Text('Aucune annonce publiée pour vous pour le moment',
                            style: TextStyle(color: Colors.white.withValues(alpha: 0.4), fontSize: 13)),
                      ]),
                    )
                  else ...[
                    Row(children: [
                      Icon(Icons.campaign_rounded, color: AppColors.primaryLight, size: 16),
                      const SizedBox(width: 6),
                      Text('Annonces publiées',
                          style: TextStyle(color: Colors.white.withValues(alpha: 0.8), fontSize: 12, fontWeight: FontWeight.bold)),
                    ]),
                    const SizedBox(height: 8),
                    ..._published.map(_publishedCard),
                  ],
                  const SizedBox(height: 80),
                ],
              ),
            ),
    );
  }

  Widget _adminCard(Map<String, dynamic> c) {
    final published = c['statut'] == 'PUBLIEE';
    return GlassCard(
      margin: const EdgeInsets.only(bottom: 8),
      padding: const EdgeInsets.all(12),
      child: Row(children: [
        Expanded(
          child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
            Row(children: [
              Flexible(
                child: Text('${c['titre']}',
                    style: const TextStyle(color: Colors.white, fontSize: 13, fontWeight: FontWeight.w600),
                    overflow: TextOverflow.ellipsis),
              ),
              const SizedBox(width: 6),
              Container(
                padding: const EdgeInsets.symmetric(horizontal: 5, vertical: 1),
                decoration: BoxDecoration(
                  color: _statutColor(c['statut']).withValues(alpha: 0.12),
                  borderRadius: BorderRadius.circular(6),
                ),
                child: Text(_statutLabel(c['statut']),
                    style: TextStyle(color: _statutColor(c['statut']), fontSize: 8, fontWeight: FontWeight.bold)),
              ),
            ]),
            const SizedBox(height: 4),
            Row(children: [_cibleChip(c)]),
            const SizedBox(height: 2),
            Text('${c['contenu'] ?? ''}',
                style: TextStyle(color: Colors.white.withValues(alpha: 0.35), fontSize: 10),
                maxLines: 1, overflow: TextOverflow.ellipsis),
          ]),
        ),
        IconButton(
          visualDensity: VisualDensity.compact,
          icon: const Icon(Icons.send_rounded, color: Color(0xFF22C55E), size: 16),
          onPressed: published ? null : () => _publish(c),
        ),
        IconButton(
          visualDensity: VisualDensity.compact,
          icon: const Icon(Icons.edit_rounded, color: Colors.amberAccent, size: 16),
          onPressed: () => _openEditor(c),
        ),
        IconButton(
          visualDensity: VisualDensity.compact,
          icon: const Icon(Icons.delete_rounded, color: Colors.redAccent, size: 16),
          onPressed: () => _delete(c),
        ),
      ]),
    );
  }

  Widget _publishedCard(Map<String, dynamic> c) {
    return GlassCard(
      margin: const EdgeInsets.only(bottom: 8),
      padding: const EdgeInsets.all(14),
      child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
        Row(children: [
          Expanded(
            child: Text('${c['titre']}',
                style: const TextStyle(color: Colors.white, fontSize: 13, fontWeight: FontWeight.bold)),
          ),
          _cibleChip(c),
        ]),
        if (_formatDate(c['datePublication']).isNotEmpty) ...[
          const SizedBox(height: 4),
          Text(_formatDate(c['datePublication']),
              style: TextStyle(color: Colors.white.withValues(alpha: 0.3), fontSize: 10)),
        ],
        const SizedBox(height: 6),
        Text('${c['contenu'] ?? ''}',
            style: TextStyle(color: Colors.white.withValues(alpha: 0.6), fontSize: 12, height: 1.4)),
      ]),
    );
  }
}

/// Feuille de création / modification d'une annonce.
class _CommunicationSheet extends StatefulWidget {
  const _CommunicationSheet({this.edit, required this.apiService});

  final Map<String, dynamic>? edit;
  final ApiService apiService;

  @override
  State<_CommunicationSheet> createState() => _CommunicationSheetState();
}

class _CommunicationSheetState extends State<_CommunicationSheet> {
  static const _roles = ['ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR', 'MEMBRE'];

  late final TextEditingController _titreCtrl;
  late final TextEditingController _contenuCtrl;
  late String _cible;
  late final List<String> _selectedRoles;
  late String _familleId;
  late String _departmentId;

  @override
  void initState() {
    super.initState();
    _titreCtrl = TextEditingController(text: '${widget.edit?['titre'] ?? ''}');
    _contenuCtrl = TextEditingController(text: '${widget.edit?['contenu'] ?? ''}');
    _cible = '${widget.edit?['cible'] ?? 'TOUS'}';
    _selectedRoles = List<String>.from((widget.edit?['roles'] as List?)?.cast<String>() ?? []);
    _familleId = '${widget.edit?['familleId'] ?? ''}';
    _departmentId = '${widget.edit?['departmentId'] ?? ''}';
  }

  // (initialisés dans initState — accès à widget interdit dans les initialiseurs)
  Future<List<Map<String, dynamic>>>? _familiesFuture;
  Future<List<Map<String, dynamic>>>? _departmentsFuture;

  @override
  void dispose() {
    _titreCtrl.dispose();
    _contenuCtrl.dispose();
    super.dispose();
  }

  Future<List<Map<String, dynamic>>> _fetchFamilies() async {
    final res = await widget.apiService.get('/families');
    return (res.data as List).map((e) => e as Map<String, dynamic>).toList();
  }

  Future<List<Map<String, dynamic>>> _fetchDepartments() async {
    final res = await widget.apiService.get('/departments');
    return (res.data as List).map((e) => e as Map<String, dynamic>).toList();
  }

  void _submit() {
    if (_titreCtrl.text.trim().isEmpty || _contenuCtrl.text.trim().isEmpty) return;
    if (_cible == 'ROLE' && _selectedRoles.isEmpty) return;
    if (_cible == 'FAMILLE' && _familleId.isEmpty) return;
    if (_cible == 'DEPARTEMENT' && _departmentId.isEmpty) return;
    Navigator.pop(context, {
      'titre': _titreCtrl.text.trim(),
      'contenu': _contenuCtrl.text.trim(),
      'cible': _cible,
      'roles': _cible == 'ROLE' ? _selectedRoles : null,
      'familleId': _cible == 'FAMILLE' ? _familleId : null,
      'departmentId': _cible == 'DEPARTEMENT' ? _departmentId : null,
    });
  }

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: EdgeInsets.only(bottom: MediaQuery.of(context).viewInsets.bottom),
      child: Container(
        padding: const EdgeInsets.all(20),
        decoration: const BoxDecoration(
          gradient: LinearGradient(colors: [Color(0xFF111C33), Color(0xFF0B1220)]),
          borderRadius: BorderRadius.vertical(top: Radius.circular(24)),
        ),
        child: SingleChildScrollView(
          child: Column(
            mainAxisSize: MainAxisSize.min,
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(widget.edit != null ? 'Modifier l’annonce' : 'Nouvelle annonce',
                  style: const TextStyle(color: Colors.white, fontSize: 16, fontWeight: FontWeight.bold)),
              const SizedBox(height: 16),
              TextField(
                controller: _titreCtrl,
                style: const TextStyle(color: Colors.white),
                decoration: _input('Titre', Icons.title),
              ),
              const SizedBox(height: 10),
              TextField(
                controller: _contenuCtrl,
                style: const TextStyle(color: Colors.white),
                maxLines: 4,
                decoration: _input('Contenu', Icons.notes),
              ),
              const SizedBox(height: 10),
              DropdownButtonFormField<String>(
                initialValue: _cible,
                dropdownColor: const Color(0xFF111C33),
                style: const TextStyle(color: Colors.white, fontSize: 13),
                decoration: _input('Cible de diffusion', Icons.people),
                items: const [
                  DropdownMenuItem(value: 'TOUS', child: Text('Toute l’église')),
                  DropdownMenuItem(value: 'ROLE', child: Text('Par rôle')),
                  DropdownMenuItem(value: 'FAMILLE', child: Text('Par famille')),
                  DropdownMenuItem(value: 'DEPARTEMENT', child: Text('Par département')),
                ],
                onChanged: (v) => setState(() {
                  _cible = v ?? 'TOUS';
                  if (_cible == 'FAMILLE') _familiesFuture ??= _fetchFamilies();
                  if (_cible == 'DEPARTEMENT') _departmentsFuture ??= _fetchDepartments();
                }),
              ),
              if (_cible == 'ROLE') ...[
                const SizedBox(height: 12),
                Wrap(
                  spacing: 6,
                  runSpacing: 6,
                  children: _roles.map((r) {
                    final active = _selectedRoles.contains(r);
                    return GestureDetector(
                      onTap: () => setState(() {
                        active ? _selectedRoles.remove(r) : _selectedRoles.add(r);
                      }),
                      child: Container(
                        padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 5),
                        decoration: BoxDecoration(
                          color: active
                              ? AppColors.primary.withValues(alpha: 0.3)
                              : Colors.white.withValues(alpha: 0.05),
                          borderRadius: BorderRadius.circular(20),
                          border: Border.all(
                              color: active ? AppColors.primary : Colors.white.withValues(alpha: 0.1)),
                        ),
                        child: Text(r,
                            style: TextStyle(
                                color: active ? AppColors.primaryLight : Colors.white54,
                                fontSize: 11,
                                fontWeight: FontWeight.w600)),
                      ),
                    );
                  }).toList(),
                ),
              ],
              if (_cible == 'FAMILLE') ...[
                const SizedBox(height: 12),
                FutureBuilder<List<Map<String, dynamic>>>(
                  future: _familiesFuture,
                  builder: (context, snapshot) {
                    final items = snapshot.data ?? [];
                    return DropdownButtonFormField<String>(
                      initialValue: _familleId.isEmpty && items.isNotEmpty ? null : _familleId,
                      dropdownColor: const Color(0xFF111C33),
                      style: const TextStyle(color: Colors.white, fontSize: 13),
                      decoration: _input('Famille', Icons.home),
                      items: items
                          .map((f) => DropdownMenuItem(value: '${f['id']}', child: Text('${f['nom']}')))
                          .toList(),
                      onChanged: (v) => setState(() => _familleId = v ?? ''),
                    );
                  },
                ),
              ],
              if (_cible == 'DEPARTEMENT') ...[
                const SizedBox(height: 12),
                FutureBuilder<List<Map<String, dynamic>>>(
                  future: _departmentsFuture,
                  builder: (context, snapshot) {
                    final items = snapshot.data ?? [];
                    return DropdownButtonFormField<String>(
                      initialValue: _departmentId.isEmpty && items.isNotEmpty ? null : _departmentId,
                      dropdownColor: const Color(0xFF111C33),
                      style: const TextStyle(color: Colors.white, fontSize: 13),
                      decoration: _input('Département', Icons.business),
                      items: items
                          .map((d) => DropdownMenuItem(value: '${d['id']}', child: Text('${d['nom']}')))
                          .toList(),
                      onChanged: (v) => setState(() => _departmentId = v ?? ''),
                    );
                  },
                ),
              ],
              const SizedBox(height: 16),
              SizedBox(
                width: double.infinity,
                child: ElevatedButton(
                  style: ElevatedButton.styleFrom(
                    backgroundColor: AppColors.primary,
                    foregroundColor: Colors.white,
                    padding: const EdgeInsets.symmetric(vertical: 14),
                  ),
                  onPressed: _submit,
                  child: Text(widget.edit != null ? 'Enregistrer' : 'Créer',
                      style: const TextStyle(fontWeight: FontWeight.bold)),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }

  InputDecoration _input(String label, IconData icon) {
    return InputDecoration(
      labelText: label,
      labelStyle: TextStyle(color: Colors.white.withValues(alpha: 0.5), fontSize: 12),
      prefixIcon: Icon(icon, color: Colors.white38, size: 18),
      filled: true,
      fillColor: Colors.white.withValues(alpha: 0.05),
      border: OutlineInputBorder(borderRadius: BorderRadius.circular(12), borderSide: BorderSide.none),
      contentPadding: const EdgeInsets.symmetric(horizontal: 12, vertical: 12),
    );
  }
}
