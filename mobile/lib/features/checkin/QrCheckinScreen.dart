import 'package:flutter/material.dart';
import 'package:dio/dio.dart' show DioMediaType;
import 'package:image_picker/image_picker.dart';
import 'dart:convert';
import '../../data/local/offline_sync_manager.dart';
import '../../data/services/api_service.dart';
import '../../l10n/app_localizations.dart';
import '../../../presentation/widgets/glass_theme.dart';
import '../../../presentation/widgets/app_drawer.dart';

/// §G5.6 — Pointage QR de terrain sur APIs RÉELLES :
/// - photo du QR du membre → POST /members/qr-checkin/scan (décodage ZXing serveur) ;
/// - code saisi (contenu `discipolat:soul:<uuid>` ou UUID) → POST /members/qr-checkin ;
/// - « Mon QR à présenter » → GET /members/me/qr-code (à faire scanner par le responsable).
/// Zéro mock.
///
/// §G5.7 — En cas de vraie panne réseau (aucune réponse HTTP), le pointage par
/// code est mis en file idempotente (`QR_CHECKIN` + clientUuid) et rejoué par
/// lot via POST /sync/batch à la reconnexion — sous réserve du toggle tenant
/// `offline_mode` (LECTURE = écritures refusées, message explicite).
class QrCheckinScreen extends StatefulWidget {
  const QrCheckinScreen({super.key, this.apiService, this.syncManager});
  final ApiService? apiService;
  @visibleForTesting
  final OfflineSyncManager? syncManager;

  @override
  State<QrCheckinScreen> createState() => _QrCheckinScreenState();
}

class _QrCheckinScreenState extends State<QrCheckinScreen> {
  late final ApiService _api = widget.apiService ?? ApiService();
  late final OfflineSyncManager _sync =
      widget.syncManager ?? sharedOfflineSyncManager(_api);
  final _codeCtrl = TextEditingController();
  final _picker = ImagePicker();
  bool _isLoading = false;
  String? _success;
  String? _error;

  @override
  void initState() {
    super.initState();
    // Reconstruit l'état activé/désactivé du bouton à la frappe.
    _codeCtrl.addListener(() {
      if (mounted) setState(() {});
    });
  }

  @override
  void dispose() {
    _codeCtrl.dispose();
    super.dispose();
  }

  Future<void> _checkInWithCode(String code) async {
    setState(() { _isLoading = true; _error = null; _success = null; });
    try {
      final res = await _api.post('/members/qr-checkin', data: {'code': code});
      if (!mounted) return;
      setState(() {
        _isLoading = false;
        _success = res.data is Map
            ? (res.data['message'] ?? AppLocalizations.of(context).checkinSaved)
            : AppLocalizations.of(context).checkinSaved;
      });
    } catch (e) {
      if (!mounted) return;
      // §G5.7 — panne réseau : file hors-ligne idempotente (rejeu par lot).
      if (ApiService.isOfflineError(e)) {
        if (await _sync.queueQrCheckin(code)) {
          if (!mounted) return;
          setState(() {
            _isLoading = false;
            _success = AppLocalizations.of(context).offlineQueued;
          });
          return;
        }
        if (!mounted) return;
        setState(() {
          _isLoading = false;
          _error = AppLocalizations.of(context).offlineReadonly;
        });
        return;
      }
      if (!mounted) return;
      setState(() {
        _isLoading = false;
        _error = AppLocalizations.of(context).error;
      });
    }
  }

  /// Photo du QR du membre → le serveur décode (ZXing) et enregistre la présence.
  Future<void> _scanPhotoAndCheckIn() async {
    try {
      final shot = await _picker.pickImage(source: ImageSource.camera, maxWidth: 1600, imageQuality: 85);
      if (shot == null) return;
      final bytes = await shot.readAsBytes();
      setState(() { _isLoading = true; _error = null; _success = null; });
      final res = await _api.postMultipart(
        '/members/qr-checkin/scan',
        fieldName: 'file',
        fileBytes: bytes,
        filename: shot.name.isEmpty ? 'qr.jpg' : shot.name,
        mimeType: DioMediaType('image', 'jpeg'),
      );
      if (!mounted) return;
      setState(() {
        _isLoading = false;
        _success = res.data is Map
            ? (res.data['message'] ?? AppLocalizations.of(context).checkinSaved)
            : AppLocalizations.of(context).checkinSaved;
      });
    } on Exception {
      if (!mounted) return;
      setState(() {
        _isLoading = false;
        _error = AppLocalizations.of(context).checkinNoQr;
      });
    }
  }

  /// Onglet présentation : mon propre QR, à faire scanner.
  Future<void> _showMyQr() async {
    final l10n = AppLocalizations.of(context);
    try {
      final res = await _api.get('/members/me/qr-code');
      if (!mounted) return;
      final data = res.data;
      final dataUrl = data is Map ? data['qrPngDataUrl'] as String? : null;
      if (dataUrl == null || !dataUrl.contains(',')) {
        setState(() { _error = l10n.myQrLoadError; });
        return;
      }
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
              Text(l10n.myQrPresentation,
                  style: const TextStyle(color: Colors.white, fontWeight: FontWeight.bold, fontSize: 16)),
              const SizedBox(height: 16),
              Image.memory(png, width: 260, height: 260),
              const SizedBox(height: 8),
              Text(data['soulId']?.toString() ?? '',
                  style: TextStyle(color: Colors.white.withValues(alpha: 0.4), fontSize: 11)),
            ],
          ),
        ),
      );
    } catch (_) {
      if (mounted) setState(() { _error = l10n.myQrLoadError; });
    }
  }

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context);
    return Scaffold(
      appBar: AppBar(
        title: Text(l10n.qrCheckinTitle),
        backgroundColor: Colors.indigo,
        foregroundColor: Colors.white,
        actions: [
          IconButton(
            onPressed: _showMyQr,
            icon: const Icon(Icons.qr_code_2),
            tooltip: l10n.myQrPresentation,
          ),
        ],
      ),
      drawer: const AppDrawer(),
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(24),
        child: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              Icon(
                Icons.qr_code_scanner,
                size: 80,
                color: Colors.white.withValues(alpha: 0.2),
              ),
              const SizedBox(height: 24),
              Text(
                l10n.qrCheckinTitle,
                style: const TextStyle(color: Colors.white, fontSize: 20, fontWeight: FontWeight.bold),
              ),
              const SizedBox(height: 8),
              Text(
                l10n.qrCheckinSubtitle,
                style: TextStyle(color: Colors.white.withValues(alpha: 0.5), fontSize: 14),
                textAlign: TextAlign.center,
              ),
              const SizedBox(height: 32),
              GlassCard(
                padding: const EdgeInsets.all(16),
                child: TextField(
                  controller: _codeCtrl,
                  style: const TextStyle(color: Colors.white, fontSize: 16),
                  textAlign: TextAlign.center,
                  decoration: InputDecoration(
                    hintText: l10n.manualCodeLabel,
                    hintStyle: TextStyle(color: Colors.white.withValues(alpha: 0.3)),
                    border: OutlineInputBorder(borderRadius: BorderRadius.circular(12), borderSide: BorderSide.none),
                    filled: true,
                    fillColor: Colors.white.withValues(alpha: 0.06),
                  ),
                  onSubmitted: (v) {
                    if (v.isNotEmpty) _checkInWithCode(v);
                  },
                ),
              ),
              const SizedBox(height: 16),
              SizedBox(
                width: double.infinity,
                child: FilledButton.icon(
                  onPressed: _isLoading || _codeCtrl.text.isEmpty
                      ? null
                      : () => _checkInWithCode(_codeCtrl.text),
                  icon: _isLoading
                      ? const SizedBox(width: 16, height: 16, child: CircularProgressIndicator(strokeWidth: 2, color: Colors.white))
                      : const Icon(Icons.check, size: 18),
                  label: Text(l10n.submitCheckin),
                  style: FilledButton.styleFrom(backgroundColor: Colors.indigo),
                ),
              ),
              const SizedBox(height: 12),
              SizedBox(
                width: double.infinity,
                child: OutlinedButton.icon(
                  onPressed: _isLoading ? null : _scanPhotoAndCheckIn,
                  icon: const Icon(Icons.photo_camera_outlined, size: 18),
                  label: Text(l10n.takeQrPhoto),
                  style: OutlinedButton.styleFrom(
                    foregroundColor: Colors.white,
                    side: BorderSide(color: Colors.white.withValues(alpha: 0.25)),
                  ),
                ),
              ),
              if (_success != null) ...[
                const SizedBox(height: 24),
                GlassCard(
                  padding: const EdgeInsets.all(16),
                  borderColor: Colors.green.withValues(alpha: 0.3),
                  child: Row(
                    children: [
                      const Icon(Icons.check_circle, color: Colors.green, size: 28),
                      const SizedBox(width: 12),
                      Expanded(
                        child: Text(_success!, style: const TextStyle(color: Colors.green, fontSize: 14, fontWeight: FontWeight.w600)),
                      ),
                    ],
                  ),
                ),
              ],
              if (_error != null) ...[
                const SizedBox(height: 24),
                GlassCard(
                  padding: const EdgeInsets.all(16),
                  borderColor: Colors.red.withValues(alpha: 0.3),
                  child: Row(
                    children: [
                      const Icon(Icons.error, color: Colors.red, size: 28),
                      const SizedBox(width: 12),
                      Expanded(
                        child: Text(_error!, style: const TextStyle(color: Colors.red, fontSize: 14)),
                      ),
                    ],
                  ),
                ),
              ],
            ],
        ),
      ),
    );
  }
}
