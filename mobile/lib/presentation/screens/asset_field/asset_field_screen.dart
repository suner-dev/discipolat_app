import 'package:flutter/material.dart';
import 'package:dio/dio.dart' show DioMediaType, DioException;
import 'package:image_picker/image_picker.dart';
import 'dart:async';
import 'dart:convert';
import 'dart:typed_data';

import '../../../data/local/offline_sync_manager.dart';
import '../../../data/services/api_service.dart';
import '../../../data/services/realtime_bus_service.dart';
import '../../../l10n/app_localizations.dart';
import '../../widgets/glass_theme.dart';
import '../../widgets/app_drawer.dart';

/// §G5.6 — Inventaire terrain (mobile connecté, APIs RÉELLES) :
/// - scan photo du QR d'actif → POST /inventory/qr/scan (décodage ZXing serveur)
///   ou saisie du jeton → GET /inventory/qr/resolve ;
/// - affichage du QR signé d'un objet → GET /inventory/{id}/qr-code (à imprimer/coller) ;
/// - sortie de matériel (check-out, kit remis à une âme) → POST /assets/{id}/checkout,
///   l'identité du membre vient du scan de SON QR → POST /members/qr-resolve ;
/// - retour + dommage → POST /assets/{id}/damage-photo (photo) puis
///   POST /assets/{id}/return (condition/notes). Zéro mock.
///
/// §G5.7 — Vraie panne réseau : sortie, retour et photo de dommage partent en
/// file idempotente (clientUuid + base64 de la photo, jamais perdue) rejouée
/// par lot via POST /sync/batch ; le toggle tenant `offline_mode` (LECTURE)
/// refuse l'entrée en file avec un message explicite.
class AssetFieldScreen extends StatefulWidget {
  const AssetFieldScreen({super.key, this.apiService, this.syncManager, this.realtimeEvents});
  final ApiService? apiService;
  @visibleForTesting
  final OfflineSyncManager? syncManager;
  /// §G5.8 — stream du firehose (injecté par les tests ; null → bus global).
  @visibleForTesting
  final Stream<RealtimeEvent>? realtimeEvents;

  @override
  State<AssetFieldScreen> createState() => _AssetFieldScreenState();
}

class _AssetFieldScreenState extends State<AssetFieldScreen> {
  late final ApiService _api = widget.apiService ?? ApiService();
  late final OfflineSyncManager _sync =
      widget.syncManager ?? sharedOfflineSyncManager(_api);
  final _picker = ImagePicker();
  final _codeCtrl = TextEditingController();
  final _assigneeCtrl = TextEditingController();

  Map<String, dynamic>? _asset;
  String? _assigneeId;
  String? _assigneeName;
  String _condition = 'GOOD';
  bool _busy = false;
  String? _message;
  bool _messageIsError = false;
  StreamSubscription<RealtimeEvent>? _realtime;

  @override
  void initState() {
    super.initState();
    // Reconstruit l'état activé/désactivé des boutons à la frappe.
    _assigneeCtrl.addListener(_refreshAssigneePreview);
    // §G5.8 — un mouvement d'inventaire sur un autre appareil (web ou autre
    // mobile) resynchronise la fiche affichée ; trou de delta → rechargement.
    _realtime = (widget.realtimeEvents ?? RealtimeBus.instance.events)
        .listen((e) {
      if (!mounted || _busy || _asset == null) return;
      const assetEvents = {
        'AssetCheckedOut', 'AssetReturned', 'AssetDamaged',
        'MaintenanceStarted', 'MaintenanceCompleted',
      };
      if (e.fullRefresh ||
          (assetEvents.contains(e.eventType) &&
              (e.aggregateId == null || e.aggregateId == _asset!['itemId']))) {
        _reloadQuietly();
      }
    });
  }

  void _refreshAssigneePreview() {
    if (mounted) setState(() {});
  }

  @override
  void dispose() {
    _realtime?.cancel();
    _codeCtrl.dispose();
    _assigneeCtrl.dispose();
    super.dispose();
  }

  void _say(String msg, {bool isError = false}) {
    if (!mounted) return;
    setState(() {
      _busy = false;
      _message = msg;
      _messageIsError = isError;
    });
  }

  bool _isNotFound(Object e) =>
      e is DioException && e.response?.statusCode == 404;

  void _applyResolution(dynamic data) {
    if (!mounted) return;
    setState(() {
      _busy = false;
      _asset = data is Map<String, dynamic> ? data : Map<String, dynamic>.from(data as Map);
      _message = null;
      _assigneeId = null;
      _assigneeName = null;
    });
  }

  /// Résolution par jeton/contenu saisi (saisie manuelle tolérée sur le terrain).
  Future<void> _resolveByCode() async {
    final code = _codeCtrl.text.trim();
    if (code.isEmpty) return;
    setState(() { _busy = true; _message = null; });
    try {
      final res = await _api.get('/inventory/qr/resolve', params: {'content': code});
      _applyResolution(res.data);
    } catch (e) {
      _say(_isNotFound(e)
          ? AppLocalizations.of(context).assetNotFound
          : AppLocalizations.of(context).error, isError: true);
    }
  }

  /// Scan photo d'un QR (actif) → décodage serveur puis résolution scopée tenant.
  Future<void> _scanAssetPhoto() async {
    try {
      final shot = await _picker.pickImage(source: ImageSource.camera, maxWidth: 1600, imageQuality: 85);
      if (shot == null) return;
      final bytes = await shot.readAsBytes();
      setState(() { _busy = true; _message = null; });
      final res = await _api.postMultipart(
        '/inventory/qr/scan',
        fieldName: 'file',
        fileBytes: bytes,
        filename: shot.name.isEmpty ? 'asset.jpg' : shot.name,
        mimeType: DioMediaType('image', 'jpeg'),
      );
      _applyResolution(res.data);
    } on Exception {
      _say(AppLocalizations.of(context).checkinNoQr, isError: true);
    }
  }

  /// Scan du QR du membre (distribution de kit par scan de « l'âme »).
  Future<void> _scanAssignee() async {
    try {
      final shot = await _picker.pickImage(source: ImageSource.camera, maxWidth: 1600, imageQuality: 85);
      if (shot == null) return;
      final bytes = await shot.readAsBytes();
      setState(() { _busy = true; _message = null; });
      final res = await _api.postMultipart(
        '/members/qr-resolve',
        fieldName: 'file',
        fileBytes: bytes,
        filename: shot.name.isEmpty ? 'soul.jpg' : shot.name,
        mimeType: DioMediaType('image', 'jpeg'),
      );
      final d = res.data;
      if (!mounted) return;
      if (d is Map && d['soulId'] != null) {
        setState(() {
          _busy = false;
          _assigneeId = d['soulId'].toString();
          _assigneeName = [d['prenom'], d['nom']]
              .where((p) => p != null && '$p'.isNotEmpty)
              .join(' ');
        });
      } else {
        _say(AppLocalizations.of(context).error, isError: true);
      }
    } on Exception {
      _say(AppLocalizations.of(context).checkinNoQr, isError: true);
    }
  }

  /// Collage du code du membre (contenu `discipolat:soul:<uuid>` ou UUID) —
  /// solution de repli quand la caméra est indisponible sur le terrain.
  void _applyAssigneeCode() {
    final raw = _assigneeCtrl.text.trim();
    final candidate = raw.split(':').last.trim();
    final uuidPattern = RegExp(
        r'^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$');
    if (uuidPattern.hasMatch(candidate)) {
      setState(() {
        _assigneeId = candidate.toLowerCase();
        _assigneeName = null;
        _message = null;
      });
    } else {
      _say(AppLocalizations.of(context).error, isError: true);
    }
  }

  Future<void> _checkout() async {
    final asset = _asset;
    if (asset == null || _assigneeId == null) return;
    setState(() { _busy = true; _message = null; });
    try {
      await _api.post('/assets/${asset['itemId']}/checkout', data: {
        'memberId': _assigneeId,
        'condition': 'GOOD',
        'notes': 'Check-out mobile terrain',
      });
      _say(AppLocalizations.of(context).checkoutDone);
      await _reloadQuietly();
    } catch (e) {
      // §G5.7 — panne réseau : la sortie est fichée puis rejouée par lot.
      final queued = ApiService.isOfflineError(e) &&
          await _sync.queueAssetCheckout(
              itemId: asset['itemId'].toString(), memberId: _assigneeId!);
      _say(queued
          ? AppLocalizations.of(context).offlineQueued
          : ApiService.isOfflineError(e)
              ? AppLocalizations.of(context).offlineReadonly
              : AppLocalizations.of(context).error,
          isError: !queued);
    }
  }

  Future<void> _attachDamagePhoto() async {
    final asset = _asset;
    if (asset == null) return;
    Uint8List? bytes;
    try {
      final shot = await _picker.pickImage(source: ImageSource.camera, maxWidth: 1600, imageQuality: 85);
      if (shot == null) return;
      bytes = await shot.readAsBytes();
      setState(() { _busy = true; _message = null; });
      await _api.postMultipart(
        '/assets/${asset['itemId']}/damage-photo',
        fieldName: 'file',
        fileBytes: bytes,
        filename: shot.name.isEmpty ? 'damage.jpg' : shot.name,
        mimeType: DioMediaType('image', 'jpeg'),
      );
      setState(() { _condition = 'DAMAGED'; });
      // Feedback visuel : l'état bascule sur « Endommagé » (le message serait redondant)
      setState(() { _busy = false; });
    } catch (e) {
      // §G5.7 — la photo capturée n'est JAMAIS perdue : base64 en file.
      if (ApiService.isOfflineError(e) && bytes != null &&
          await _sync.queueDamagePhoto(
              itemId: asset['itemId'].toString(), bytes: bytes)) {
        setState(() { _condition = 'DAMAGED'; });
        _say(AppLocalizations.of(context).offlineQueued);
        return;
      }
      _say(ApiService.isOfflineError(e)
          ? AppLocalizations.of(context).offlineReadonly
          : AppLocalizations.of(context).error,
          isError: true);
    }
  }

  Future<void> _retour() async {
    final asset = _asset;
    if (asset == null) return;
    setState(() { _busy = true; _message = null; });
    try {
      await _api.post('/assets/${asset['itemId']}/return', data: {
        'condition': _condition,
        'notes': 'Retour mobile terrain',
      });
      _say(AppLocalizations.of(context).returnDone);
      await _reloadQuietly();
    } catch (e) {
      final queued = ApiService.isOfflineError(e) &&
          await _sync.queueAssetReturn(
              itemId: asset['itemId'].toString(), condition: _condition);
      _say(queued
          ? AppLocalizations.of(context).offlineQueued
          : ApiService.isOfflineError(e)
              ? AppLocalizations.of(context).offlineReadonly
              : AppLocalizations.of(context).error,
          isError: !queued);
    }
  }

  /// Rafraîchit l'état de l'actif après une mutation (retry silencieux).
  Future<void> _reloadQuietly() async {
    final asset = _asset;
    final token = asset == null ? null : asset['qrToken']?.toString();
    if (asset == null || token == null || token.isEmpty) return;
    try {
      final res = await _api.get('/inventory/qr/resolve', params: {'content': token});
      if (mounted && res.data is Map) {
        setState(() => _asset = Map<String, dynamic>.from(res.data as Map));
      }
    } catch (_) {
      // l'état affiché reste la dernière vérité connue ; le prochain scan re-synchronise
    }
  }

  /// Affiche le QR signé de l'objet (à imprimer/coller sur le matériel).
  Future<void> _showAssetQr() async {
    final asset = _asset;
    final l10n = AppLocalizations.of(context);
    if (asset == null) return;
    try {
      final res = await _api.get('/inventory/${asset['itemId']}/qr-code');
      final data = res.data;
      final dataUrl = data is Map ? data['qrPngDataUrl'] as String? : null;
      if (dataUrl == null || !dataUrl.contains(',')) return;
      final png = base64Decode(dataUrl.split(',').last);
      if (!mounted) return;
      showModalBottomSheet(
        context: context,
        backgroundColor: const Color(0xFF14141F),
        builder: (ctx) => SingleChildScrollView(
          padding: const EdgeInsets.all(24),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              Text('${l10n.assetQrShow} — ${asset['nom'] ?? ''}',
                  textAlign: TextAlign.center,
                  style: const TextStyle(color: Colors.white, fontWeight: FontWeight.bold)),
              const SizedBox(height: 16),
              Image.memory(png, width: 260, height: 260),
            ],
          ),
        ),
      );
    } catch (_) {
      _say(l10n.error, isError: true);
    }
  }

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context);
    final asset = _asset;
    return Scaffold(
      appBar: AppBar(
        title: Text(l10n.assetFieldTitle),
        backgroundColor: Colors.indigo,
        foregroundColor: Colors.white,
      ),
      drawer: const AppDrawer(),
      body: ListView(
        padding: const EdgeInsets.all(16),
        children: [
          GlassCard(
            padding: const EdgeInsets.all(16),
            child: Column(
              children: [
                TextField(
                  controller: _codeCtrl,
                  style: const TextStyle(color: Colors.white, fontSize: 15),
                  decoration: InputDecoration(
                    hintText: l10n.assetCodeLabel,
                    hintStyle: TextStyle(color: Colors.white.withValues(alpha: 0.3)),
                    border: OutlineInputBorder(borderRadius: BorderRadius.circular(12), borderSide: BorderSide.none),
                    filled: true,
                    fillColor: Colors.white.withValues(alpha: 0.06),
                  ),
                  onSubmitted: (_) => _resolveByCode(),
                ),
                const SizedBox(height: 12),
                Row(
                  children: [
                    Expanded(
                      child: FilledButton.icon(
                        onPressed: _busy ? null : _resolveByCode,
                        icon: const Icon(Icons.search, size: 18),
                        label: Text(l10n.resolveButton),
                        style: FilledButton.styleFrom(backgroundColor: Colors.indigo),
                      ),
                    ),
                    const SizedBox(width: 12),
                    Expanded(
                      child: OutlinedButton.icon(
                        onPressed: _busy ? null : _scanAssetPhoto,
                        icon: const Icon(Icons.photo_camera_outlined, size: 18),
                        label: Text(l10n.scanAssetPhoto, overflow: TextOverflow.ellipsis),
                        style: OutlinedButton.styleFrom(
                          foregroundColor: Colors.white,
                          side: BorderSide(color: Colors.white.withValues(alpha: 0.25)),
                        ),
                      ),
                    ),
                  ],
                ),
              ],
            ),
          ),
          if (_busy) ...[
            const Padding(
              padding: EdgeInsets.symmetric(vertical: 24),
              child: Center(child: CircularProgressIndicator()),
            ),
          ],
          if (asset != null) ...[
            const SizedBox(height: 8),
            GlassCard(
              padding: const EdgeInsets.all(16),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Row(
                    children: [
                      const Icon(Icons.inventory_2_outlined, color: Colors.indigoAccent, size: 20),
                      const SizedBox(width: 8),
                      Expanded(
                        child: Text('${asset['nom'] ?? ''}',
                            style: const TextStyle(color: Colors.white, fontSize: 17, fontWeight: FontWeight.bold)),
                      ),
                    ],
                  ),
                  const SizedBox(height: 8),
                  Text('${l10n.conditionLabel} : ${asset['statut'] ?? '-'}',
                      style: TextStyle(color: Colors.white.withValues(alpha: 0.7), fontSize: 13)),
                  if (asset['categorie'] != null)
                    Text('${asset['categorie']}',
                        style: TextStyle(color: Colors.white.withValues(alpha: 0.5), fontSize: 13)),
                  if (asset['checkedOut'] == true)
                    Padding(
                      padding: const EdgeInsets.only(top: 6),
                      child: Text(
                        '${l10n.borrowedLabel} : ${asset['borrowedByMemberId'] ?? '?'}',
                        style: const TextStyle(color: Colors.orangeAccent, fontSize: 13),
                      ),
                    ),
                  const SizedBox(height: 8),
                  SizedBox(
                    width: double.infinity,
                    child: TextButton.icon(
                      onPressed: _busy ? null : _showAssetQr,
                      icon: const Icon(Icons.qr_code_2, size: 18, color: Colors.indigoAccent),
                      label: Text(l10n.assetQrShow,
                          style: const TextStyle(color: Colors.indigoAccent, fontSize: 13)),
                    ),
                  ),
                ],
              ),
            ),
            const SizedBox(height: 12),
            if (asset['checkedOut'] != true)
              GlassCard(
                padding: const EdgeInsets.all(16),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(l10n.assigneeLabel,
                        style: TextStyle(color: Colors.white.withValues(alpha: 0.6), fontSize: 13)),
                    const SizedBox(height: 8),
                    Row(
                      children: [
                        Expanded(
                          child: OutlinedButton.icon(
                            onPressed: _busy ? null : _scanAssignee,
                            icon: const Icon(Icons.person_search, size: 18),
                            label: Text(_assigneeName?.isNotEmpty == true
                                ? _assigneeName!
                                : l10n.scanSoulButton),
                            style: OutlinedButton.styleFrom(
                              foregroundColor: Colors.white,
                              side: BorderSide(color: Colors.white.withValues(alpha: 0.25)),
                            ),
                          ),
                        ),
                      ],
                    ),
                    const SizedBox(height: 8),
                    TextField(
                      key: const Key('assignee-code-field'),
                      controller: _assigneeCtrl,
                      style: const TextStyle(color: Colors.white, fontSize: 14),
                      decoration: InputDecoration(
                        hintText: l10n.manualCodeLabel,
                        hintStyle: TextStyle(color: Colors.white.withValues(alpha: 0.3)),
                        border: OutlineInputBorder(borderRadius: BorderRadius.circular(12), borderSide: BorderSide.none),
                        filled: true,
                        fillColor: Colors.white.withValues(alpha: 0.06),
                      ),
                      onSubmitted: (_) => _applyAssigneeCode(),
                    ),
                    const SizedBox(height: 12),
                    SizedBox(
                      width: double.infinity,
                      child: FilledButton.icon(
                        onPressed: _busy || _assigneeId == null
                            ? null
                            : _checkout,
                        icon: const Icon(Icons.logout, size: 18),
                        label: Text(l10n.checkoutButton),
                        style: FilledButton.styleFrom(backgroundColor: Colors.teal),
                      ),
                    ),
                  ],
                ),
              )
            else
              GlassCard(
                padding: const EdgeInsets.all(16),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    DropdownButtonFormField<String>(
                      initialValue: _condition,
                      dropdownColor: const Color(0xFF1E1E2E),
                      style: const TextStyle(color: Colors.white, fontSize: 15),
                      decoration: InputDecoration(
                        labelText: l10n.conditionLabel,
                        labelStyle: TextStyle(color: Colors.white.withValues(alpha: 0.5)),
                        border: OutlineInputBorder(borderRadius: BorderRadius.circular(12), borderSide: BorderSide.none),
                        filled: true,
                        fillColor: Colors.white.withValues(alpha: 0.06),
                      ),
                      items: [
                        DropdownMenuItem(value: 'GOOD', child: Text(l10n.conditionGood)),
                        DropdownMenuItem(value: 'DAMAGED', child: Text(l10n.conditionDamaged)),
                      ],
                      onChanged: (v) => setState(() => _condition = v ?? 'GOOD'),
                    ),
                    const SizedBox(height: 12),
                    Row(
                      children: [
                        Expanded(
                          child: OutlinedButton.icon(
                            onPressed: _busy ? null : _attachDamagePhoto,
                            icon: const Icon(Icons.add_a_photo_outlined, size: 18),
                            label: Text(l10n.damagePhotoButton, overflow: TextOverflow.ellipsis),
                            style: OutlinedButton.styleFrom(
                              foregroundColor: Colors.orangeAccent,
                              side: BorderSide(color: Colors.orangeAccent.withValues(alpha: 0.4)),
                            ),
                          ),
                        ),
                        const SizedBox(width: 12),
                        Expanded(
                          child: FilledButton.icon(
                            onPressed: _busy ? null : _retour,
                            icon: const Icon(Icons.login, size: 18),
                            label: Text(l10n.returnButton),
                            style: FilledButton.styleFrom(backgroundColor: Colors.indigo),
                          ),
                        ),
                      ],
                    ),
                  ],
                ),
              ),
          ],
          if (_message != null) ...[
            const SizedBox(height: 16),
            GlassCard(
              padding: const EdgeInsets.all(16),
              borderColor: (_messageIsError ? Colors.red : Colors.green).withValues(alpha: 0.3),
              child: Row(
                children: [
                  Icon(_messageIsError ? Icons.error : Icons.check_circle,
                      color: _messageIsError ? Colors.red : Colors.green, size: 28),
                  const SizedBox(width: 12),
                  Expanded(
                    child: Text(_message!,
                        style: TextStyle(
                            color: _messageIsError ? Colors.red : Colors.green,
                            fontSize: 14,
                            fontWeight: FontWeight.w600)),
                  ),
                ],
              ),
            ),
          ],
        ],
      ),
    );
  }
}
