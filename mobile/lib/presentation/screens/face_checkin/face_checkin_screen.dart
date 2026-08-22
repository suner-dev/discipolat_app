import 'dart:convert';
import 'dart:typed_data';

import 'package:flutter/material.dart';
import 'package:image_picker/image_picker.dart';

import '../../widgets/glass_theme.dart';
import '../../widgets/app_drawer.dart';
import '../../../data/services/api_service.dart';

/// Pointage par reconnaissance faciale — capture d'un portrait puis
/// identification côté serveur (empreinte perceptuelle, aucune image stockée).
class FaceCheckinScreen extends StatefulWidget {
  /// Mode enrôlement (ADMIN/PASTEUR/RESPONSABLE) : enregistre le gabarit.
  final bool enrollMode;

  final ApiService? apiService;

  const FaceCheckinScreen({super.key, this.enrollMode = false, this.apiService});

  @override
  State<FaceCheckinScreen> createState() => _FaceCheckinScreenState();
}

class _FaceCheckinScreenState extends State<FaceCheckinScreen> {
  late final ApiService _apiService = widget.apiService ?? ApiService();
  final ImagePicker _picker = ImagePicker();

  Uint8List? _preview;
  String? _base64Image;
  Map<String, dynamic>? _result;
  bool _busy = false;
  bool _enrollMode = false;
  final _nameCtrl = TextEditingController();

  bool get _isAdmin =>
      widget.enrollMode; // l'écran est ouvert en mode admin explicitement

  @override
  void initState() {
    super.initState();
    _enrollMode = widget.enrollMode;
  }

  @override
  void dispose() {
    _nameCtrl.dispose();
    super.dispose();
  }

  Future<void> _capture(ImageSource source) async {
    try {
      final XFile? photo =
          await _picker.pickImage(source: source, maxWidth: 1024, imageQuality: 85);
      if (photo == null || !mounted) return;
      final bytes = await photo.readAsBytes();
      setState(() {
        _preview = bytes;
        _base64Image = base64Encode(bytes);
        _result = null;
      });
    } catch (_) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(const SnackBar(
            content: Text('Impossible d\'accéder à la caméra'),
            backgroundColor: Colors.red));
      }
    }
  }

  Future<void> _submit() async {
    if (_base64Image == null) return;
    setState(() => _busy = true);
    try {
      final dynamic res;
      if (_enrollMode) {
        // Sans userId explicite, le backend enrôle l'utilisateur courant.
        res = await _apiService.post('/face/enroll', data: {
          'displayName': _nameCtrl.text.trim(),
          'imageBase64': _base64Image,
        });
        if (mounted) {
          ScaffoldMessenger.of(context).showSnackBar(SnackBar(
              content: Text('✅ Visage de ${(res.data as Map)['displayName']} enrôlé'),
              backgroundColor: Colors.green));
        }
      } else {
        res = await _apiService.post('/face/identify', data: {
          'imageBase64': _base64Image,
        });
        if (mounted) setState(() => _result = (res.data as Map).cast<String, dynamic>());
      }
    } catch (_) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(const SnackBar(
            content: Text('Échec — vérifiez la connexion'),
            backgroundColor: Colors.red));
      }
    } finally {
      if (mounted) setState(() => _busy = false);
    }
  }

  Color _confidenceColor(double confidence) => confidence >= 0.8
      ? const Color(0xFF4CAF50)
      : confidence >= 0.6
          ? const Color(0xFFFFB300)
          : const Color(0xFFEF5350);

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: Text(_enrollMode ? 'Enrôlement facial' : 'Pointage facial'),
        actions: [
          if (_isAdmin)
            IconButton(
              tooltip: _enrollMode ? 'Passer en identification' : 'Passer en enrôlement',
              icon: Icon(_enrollMode ? Icons.face_retouching_natural : Icons.person_add_alt),
              onPressed: () => setState(() {
                _enrollMode = !_enrollMode;
                _result = null;
                _preview = null;
                _base64Image = null;
              }),
            ),
        ],
      ),
      drawer: const AppDrawer(),
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            GlassCard(
              child: Column(
                children: [
                  Icon(Icons.face_retouching_natural,
                      color: AppColors.primary, size: 40),
                  const SizedBox(height: 8),
                  Text(
                    _enrollMode
                        ? 'Enrôlez votre visage pour activer le pointage sans contact. Aucune image n\'est conservée : seule une empreinte mathématique non réversible est enregistrée.'
                        : 'Cadrez le visage dans un endroit éclairé puis lancez l\'identification.',
                    textAlign: TextAlign.center,
                    style: const TextStyle(color: Colors.white70, fontSize: 13),
                  ),
                ],
              ),
            ),
            const SizedBox(height: 16),

            // Aperçu photo
            Container(
              height: 240,
              decoration: BoxDecoration(
                borderRadius: BorderRadius.circular(16),
                color: Colors.white.withValues(alpha: 0.04),
                border: Border.all(color: Colors.white12),
              ),
              clipBehavior: Clip.antiAlias,
              child: _preview != null
                  ? Image.memory(_preview!, fit: BoxFit.cover)
                  : const Center(
                      child: Icon(Icons.camera_alt,
                          color: Colors.white24, size: 48)),
            ),
            const SizedBox(height: 16),

            Row(
              children: [
                Expanded(
                  child: OutlinedButton.icon(
                    onPressed: () => _capture(ImageSource.camera),
                    icon: const Icon(Icons.photo_camera, size: 18),
                    label: const Text('Caméra'),
                  ),
                ),
                const SizedBox(width: 10),
                Expanded(
                  child: OutlinedButton.icon(
                    onPressed: () => _capture(ImageSource.gallery),
                    icon: const Icon(Icons.photo_library, size: 18),
                    label: const Text('Galerie'),
                  ),
                ),
              ],
            ),

            if (_enrollMode) ...[
              const SizedBox(height: 14),
              TextField(
                controller: _nameCtrl,
                style: const TextStyle(color: Colors.white),
                decoration: InputDecoration(
                  labelText: 'Nom affiché',
                  labelStyle: const TextStyle(color: Colors.white54),
                  filled: true,
                  fillColor: Colors.white.withValues(alpha: 0.05),
                  border: OutlineInputBorder(
                      borderRadius: BorderRadius.circular(12)),
                ),
              ),
            ],

            const SizedBox(height: 16),
            FilledButton.icon(
              onPressed:
                  (_base64Image == null || _busy || (_enrollMode && _nameCtrl.text.trim().isEmpty))
                      ? null
                      : _submit,
              icon: _busy
                  ? const SizedBox(
                      width: 16,
                      height: 16,
                      child: CircularProgressIndicator(strokeWidth: 2))
                  : Icon(_enrollMode ? Icons.save : Icons.radar, size: 18),
              label: Text(_enrollMode ? 'Enrôler ce visage' : 'Identifier'),
            ),

            // Résultat d'identification
            if (!_enrollMode && _result != null) ...[
              const SizedBox(height: 20),
              GlassCard(
                child: Column(
                  children: [
                    Icon(
                      _result!['matched'] == true
                          ? Icons.verified_user
                          : Icons.gpp_bad,
                      color: _result!['matched'] == true
                          ? const Color(0xFF4CAF50)
                          : const Color(0xFFEF5350),
                      size: 44,
                    ),
                    const SizedBox(height: 10),
                    Text(
                      _result!['matched'] == true
                          ? '${_result!['displayName']}'
                          : 'Visage non reconnu',
                      style: const TextStyle(
                          fontSize: 18, fontWeight: FontWeight.w600),
                    ),
                    const SizedBox(height: 6),
                    Text('${_result!['message'] ?? ''}',
                        style:
                            const TextStyle(color: Colors.white54, fontSize: 12)),
                    const SizedBox(height: 10),
                    // Jauge de confiance
                    Stack(children: [
                      Container(
                        height: 10,
                        decoration: BoxDecoration(
                            color: Colors.white12,
                            borderRadius: BorderRadius.circular(6)),
                      ),
                      FractionallySizedBox(
                        widthFactor:
                            ((_result!['confidence'] as num?)?.toDouble() ?? 0)
                                .clamp(0.0, 1.0),
                        child: Container(
                          height: 10,
                          decoration: BoxDecoration(
                              color: _confidenceColor(
                                  ((_result!['confidence'] as num?) ?? 0)
                                      .toDouble()),
                              borderRadius: BorderRadius.circular(6)),
                        ),
                      ),
                    ]),
                    const SizedBox(height: 6),
                    Text('Confiance : ${(((_result!['confidence'] as num?) ?? 0) * 100).toStringAsFixed(1)} %',
                        style: const TextStyle(color: Colors.white70, fontSize: 12)),
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
