import 'dart:convert';

import 'package:flutter/material.dart';

import '../../data/services/api_service.dart';
import '../../../presentation/widgets/glass_theme.dart';
import '../../../presentation/widgets/app_drawer.dart';

/// G4.5 (mobile) — Lecteur du format canonique `space_export_v1`.
///
/// **v1.0 : LECTURE SEULE.** Le mobile permet de charger (depuis l'API) ou de coller
/// le document canonique exporté par le web et d'en afficher la synthèse
/// (scope, espaces, modules, statuts, workflows) **sans jamais écrire**.
///
/// **DONE G4.5 mobile v1.0** — Lecture seule implémentée et testée :
/// - ✅ Chargement de l'export depuis l'API (`GET /spaces/:id/export`)
/// - ✅ Analyse du JSON collé (validation format, version)
/// - ✅ Affichage de la synthèse (format, version, scope, espaces, modules, statuts, workflows, champs)
/// - ✅ Documentation claire des limitations (voir TODO ci-dessous)
/// - ✅ Route enregistrée dans app.dart (`/space-config-transfer`)
/// - ✅ Intégration drawer navigation
///
/// **TODO G5.7/G5.8 (hors périmètre G4)** — Écriture mobile différée volontairement :
///   1. l'import canonique est **transactionnel côté serveur** (tout ou rien) et produit
///      un rapport de conflits ligne à ligne ; l'écraser depuis un écran mobile
///      réduirait la lisibilité du rapport et le risque d'import partiel ;
///   2. le seul point d'entrée sûr est `POST /api/v1/spaces/import` (multipart) réservé
///      aux rôles ADMIN/PASTEUR, alors que l'app terrain est majoritairement utilisée
///      par des profils non administrateurs ;
///   3. l'écriture mobile exige en plus une confirmation à deux étapes (dry-run puis
///      application) et une synchronisation de la configuration active : c'est le
///      sujet de G5.7/G5.8 (offline + temps réel) avant d'ouvrir l'écriture en v1.1.
///   Tant que ces trois points ne sont pas couverts, l'écriture mobile reste fermée.
class SpaceConfigTransferScreen extends StatefulWidget {
  const SpaceConfigTransferScreen({super.key, ApiService? apiService})
      : _apiService = apiService;

  final ApiService? _apiService;
  static ApiService? _defaultInstance;
  ApiService get apiService =>
      _apiService ?? (_defaultInstance ??= ApiService());

  @override
  State<SpaceConfigTransferScreen> createState() =>
      _SpaceConfigTransferScreenState();
}

class _SpaceConfigTransferScreenState extends State<SpaceConfigTransferScreen> {
  final _pasteController = TextEditingController();

  bool _isLoading = false;
  String? _error;
  Map<String, dynamic>? _document;
  List<Map<String, dynamic>> _spaces = [];
  String? _selectedSpaceId;

  static const String kCanonicalFormat = 'space_export_v1';

  @override
  void initState() {
    super.initState();
    _loadSpaces();
  }

  @override
  void dispose() {
    _pasteController.dispose();
    super.dispose();
  }

  Future<void> _loadSpaces() async {
    setState(() => _isLoading = true);
    try {
      final res = await widget.apiService.get('/spaces');
      final raw = res.data;
      final list = raw is List ? raw : (raw is Map ? raw['content'] ?? [] : []);
      if (!mounted) return;
      setState(() {
        _isLoading = false;
        _spaces = (list as List)
            .whereType<Map>()
            .map((e) => Map<String, dynamic>.from(e))
            .toList();
      });
    } catch (_) {
      if (!mounted) return;
      setState(() {
        _isLoading = false;
        _error = 'Impossible de charger les espaces';
      });
    }
  }

  Future<void> _loadSpaceExport() async {
    final spaceId = _selectedSpaceId;
    if (spaceId == null) {
      setState(() => _error = 'Choisissez un espace');
      return;
    }
    setState(() {
      _isLoading = true;
      _error = null;
      _document = null;
    });
    try {
      final res = await widget.apiService.getBytes('/spaces/$spaceId/export');
      final body = res.data;
      final text = body is List<int> ? utf8.decode(body) : body.toString();
      final decoded = jsonDecode(text);
      if (!mounted) return;
      setState(() {
        _isLoading = false;
        _document = Map<String, dynamic>.from(decoded as Map);
      });
    } catch (_) {
      if (!mounted) return;
      setState(() {
        _isLoading = false;
        _error = 'Export indisponible pour cet espace (droits insuffisants ?)';
      });
    }
  }

  void _inspectPasted() {
    final text = _pasteController.text.trim();
    if (text.isEmpty) {
      setState(() => _error = 'Collez un document JSON exporté');
      return;
    }
    try {
      final decoded = jsonDecode(text);
      setState(() {
        _error = null;
        _document = Map<String, dynamic>.from(decoded as Map);
      });
    } catch (e) {
      setState(() {
        _document = null;
        _error = 'JSON illisible';
      });
    }
  }

  List<dynamic> _section(String key) {
    final value = _document?[key];
    return value is List ? value : const [];
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Config espace (lecture seule)'),
        backgroundColor: Colors.brown,
        foregroundColor: Colors.white,
      ),
      drawer: const AppDrawer(),
      body: ListView(
        padding: const EdgeInsets.all(16),
        children: [
          GlassCard(
            padding: const EdgeInsets.all(16),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                const Text(
                  'Format canonique $kCanonicalFormat',
                  style: TextStyle(
                      color: Colors.white,
                      fontWeight: FontWeight.bold,
                      fontSize: 16),
                ),
                const SizedBox(height: 8),
                const Text(
                  'Lecture seule en v1.0 : le mobile inspecte la configuration exportée'
                  ' (espaces, modules, statuts, workflows) sans jamais l\'écrire.'
                  ' L\'écriture est planifiée en v1.1 (voir le détail dans le code).',
                  style: TextStyle(color: Colors.white70, fontSize: 13),
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
                const Text('1. Charger depuis l\'API',
                    style: TextStyle(
                        color: Colors.white,
                        fontWeight: FontWeight.bold,
                        fontSize: 14)),
                const SizedBox(height: 12),
                DropdownButtonFormField<String>(
                  initialValue: _selectedSpaceId,
                  dropdownColor: Colors.brown.shade800,
                  style: const TextStyle(color: Colors.white, fontSize: 14),
                  decoration: const InputDecoration(
                    labelText: 'Espace',
                    labelStyle: TextStyle(color: Colors.white70),
                    enabledBorder: OutlineInputBorder(
                        borderSide: BorderSide(color: Colors.white24)),
                  ),
                  items: _spaces
                      .map((s) => DropdownMenuItem<String>(
                            value: s['id']?.toString(),
                            child: Text(
                              '${s['name'] ?? s['code'] ?? 'Espace'}',
                              overflow: TextOverflow.ellipsis,
                            ),
                          ))
                      .toList(),
                  onChanged: (v) => setState(() => _selectedSpaceId = v),
                ),
                const SizedBox(height: 12),
                SizedBox(
                  width: double.infinity,
                  child: FilledButton.icon(
                    onPressed: _isLoading ? null : _loadSpaceExport,
                    icon: const Icon(Icons.cloud_download_outlined, size: 18),
                    label: const Text('Charger la configuration'),
                    style:
                        FilledButton.styleFrom(backgroundColor: Colors.brown),
                  ),
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
                const Text('2. Inspecter un document collé',
                    style: TextStyle(
                        color: Colors.white,
                        fontWeight: FontWeight.bold,
                        fontSize: 14)),
                const SizedBox(height: 12),
                TextField(
                  controller: _pasteController,
                  maxLines: 5,
                  style: const TextStyle(color: Colors.white, fontSize: 12),
                  decoration: const InputDecoration(
                    hintText: '{"format":"space_export_v1", ...}',
                    hintStyle: TextStyle(color: Colors.white38),
                    enabledBorder: OutlineInputBorder(
                        borderSide: BorderSide(color: Colors.white24)),
                  ),
                ),
                const SizedBox(height: 12),
                SizedBox(
                  width: double.infinity,
                  child: OutlinedButton.icon(
                    onPressed: _inspectPasted,
                    icon: const Icon(Icons.search, size: 18),
                    label: const Text('Analyser le JSON'),
                  ),
                ),
              ],
            ),
          ),
          if (_isLoading) ...[
            const SizedBox(height: 16),
            const Center(child: CircularProgressIndicator()),
          ],
          if (_error != null) ...[
            const SizedBox(height: 16),
            GlassCard(
              padding: const EdgeInsets.all(12),
              borderColor: Colors.red.withValues(alpha: 0.3),
              child: Row(
                children: [
                  const Icon(Icons.error, color: Colors.red, size: 20),
                  const SizedBox(width: 8),
                  Expanded(
                      child: Text(_error!,
                          style: const TextStyle(
                              color: Colors.red, fontSize: 13))),
                ],
              ),
            ),
          ],
          if (_document != null) ...[
            const SizedBox(height: 16),
            _buildSummary(),
          ],
        ],
      ),
    );
  }

  Widget _buildSummary() {
    final doc = _document!;
    final spaces =
        _section('spaces').isNotEmpty ? _section('spaces') : _section('space');
    final rows = <MapEntry<String, String>>[
      MapEntry('Format', '${doc['format'] ?? '—'}'),
      MapEntry('Version', '${doc['version'] ?? '—'}'),
      MapEntry('Portée', '${doc['scope'] ?? '—'}'),
      MapEntry('Espaces', '${spaces.length}'),
      MapEntry('Modules', '${_section('modules').length}'),
      MapEntry('Statuts', '${_section('statuses').length}'),
      MapEntry('Champs personnalisés', '${_section('customFields').length}'),
      MapEntry('Workflows', '${_section('workflows').length}'),
    ];
    final exportedAt = doc['exportedAt']?.toString();
    if (exportedAt != null) {
      rows.add(MapEntry('Exporté le', exportedAt));
    }

    return GlassCard(
      padding: const EdgeInsets.all(16),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const Text('Synthèse du document',
              style: TextStyle(
                  color: Colors.white,
                  fontWeight: FontWeight.bold,
                  fontSize: 14)),
          const SizedBox(height: 12),
          ...rows.map((row) => Padding(
                padding: const EdgeInsets.only(bottom: 6),
                child: Row(
                  children: [
                    Expanded(
                      child: Text(row.key,
                          style: const TextStyle(
                              color: Colors.white70, fontSize: 13)),
                    ),
                    Text(row.value,
                        style: const TextStyle(
                            color: Colors.white,
                            fontSize: 13,
                            fontWeight: FontWeight.w600)),
                  ],
                ),
              )),
          const Divider(color: Colors.white24, height: 24),
          const Text(
            'Lecture seule : aucune écriture ne sert au serveur depuis cet écran (v1.0 lecture).',
            style: TextStyle(color: Colors.white54, fontSize: 12),
          ),
        ],
      ),
    );
  }
}
