import 'dart:async';

import 'package:camera/camera.dart';
import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import 'package:sensors_plus/sensors_plus.dart';
import 'package:shared_preferences/shared_preferences.dart';

import '../../widgets/glass_theme.dart';

/// Onboarding immersif « Réalité Augmentée ».
///
/// La caméra arrière sert de fond live (couche réalité), les cartes de
/// présentation flottent au-dessus avec une inclinaison 3D pilotée par
/// l'accéléromètre (parallaxe). Sans caméra (émulateur, permission refusée,
/// tests), un dégradé animé prend le relais : l'écran reste fonctionnel.
class ArOnboardingScreen extends StatefulWidget {
  /// Injéctable pour les tests (sinon `availableCameras()`).
  final Future<List<CameraDescription>> Function()? camerasProvider;

  const ArOnboardingScreen({super.key, this.camerasProvider});

  @override
  State<ArOnboardingScreen> createState() => _ArOnboardingScreenState();
}

class _ArOnboardingScreenState extends State<ArOnboardingScreen>
    with SingleTickerProviderStateMixin {
  static const int _stepCount = 4;
  static const String _routeAfter = '/login';

  CameraController? _cameraController;
  bool _cameraReady = false;
  StreamSubscription<dynamic>? _accelSub;
  double _tiltX = 0;
  double _tiltY = 0;
  int _step = 0;
  late final AnimationController _pulse =
      AnimationController(vsync: this, duration: const Duration(seconds: 3))
        ..repeat(reverse: true);

  @override
  void initState() {
    super.initState();
    _initCamera();
    _initParallax();
  }

  Future<void> _initCamera() async {
    try {
      final provider = widget.camerasProvider ?? availableCameras;
      final cameras = await provider();
      if (cameras.isEmpty || !mounted) return;
      final controller = CameraController(
        cameras.firstWhere(
          (c) => c.lensDirection == CameraLensDirection.back,
          orElse: () => cameras.first,
        ),
        ResolutionPreset.low,
        enableAudio: false,
      );
      await controller.initialize();
      if (!mounted) {
        await controller.dispose();
        return;
      }
      setState(() {
        _cameraController = controller;
        _cameraReady = true;
      });
    } catch (_) {
      // Caméra indisponible → fond dégradé, l'expérience continue.
    }
  }

  void _initParallax() {
    try {
      _accelSub = accelerometerEventStream().listen((event) {
        if (!mounted) return;
        setState(() {
          _tiltX = (event.x / 10).clamp(-1.0, 1.0);
          _tiltY = (event.y / 10).clamp(-1.0, 1.0);
        });
      });
    } catch (_) {
      // Capteurs absents (tests/web) — parallaxe neutre.
    }
  }

  @override
  void dispose() {
    _accelSub?.cancel();
    _pulse.dispose();
    _cameraController?.dispose();
    super.dispose();
  }

  Future<void> _finish(BuildContext context) async {
    try {
      final prefs = await SharedPreferences.getInstance();
      await prefs.setBool('onboarding_complete', true);
    } catch (_) {}
    if (context.mounted) context.go(_routeAfter);
  }

  static const List<({IconData icon, String title, String body})> _steps = [
    (
      icon: Icons.church_rounded,
      title: 'Bienvenue dans Discipolat',
      body:
          'La plateforme complète pour faire grandir vos disciples : familles, faiseurs, départements et âmes.',
    ),
    (
      icon: Icons.route_rounded,
      title: 'Suivi de croissance',
      body:
          'Pipeline d\'évangélisation, visites, objectifs hebdomadaires et rapports de terrain — même hors-ligne.',
    ),
    (
      icon: Icons.face_retouching_natural,
      title: 'Pointage facial & vocal',
      body:
          'Marquez les présences d\'un regard et dictez vos rapports : l\'IA s\'occupe du reste.',
    ),
    (
      icon: Icons.auto_graph_rounded,
      title: 'Pilotage temps réel',
      body:
          'Tableaux de bord pasteurs, observatoire de santé spirituelle et jumeau numérique de votre église.',
    ),
  ];

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: Colors.black,
      body: Stack(
        fit: StackFit.expand,
        children: [
          // ── Couche réalité ──
          if (_cameraReady)
            CameraPreview(_cameraController!)
          else
            AnimatedBuilder(
              animation: _pulse,
              builder: (context, _) => Container(
                decoration: BoxDecoration(
                  gradient: LinearGradient(
                    begin: Alignment.topLeft,
                    end: Alignment.bottomRight,
                    colors: [
                      Color.lerp(const Color(0xFF052E16),
                          const Color(0xFF065F46), _pulse.value)!,
                      Color.lerp(const Color(0xFF111827),
                          const Color(0xFF1F2937), _pulse.value)!,
                    ],
                  ),
                ),
              ),
            ),

          // Scrim pour la lisibilité
          DecoratedBox(
            decoration: BoxDecoration(
              gradient: LinearGradient(
                begin: Alignment.topCenter,
                end: Alignment.bottomCenter,
                colors: [
                  Colors.black.withValues(alpha: 0.35),
                  Colors.transparent,
                  Colors.black.withValues(alpha: 0.75),
                ],
              ),
            ),
          ),

          // Réticule AR décoratif
          Center(
            child: AnimatedBuilder(
              animation: _pulse,
              builder: (context, _) => Container(
                width: 180 + _pulse.value * 14,
                height: 180 + _pulse.value * 14,
                decoration: BoxDecoration(
                  shape: BoxShape.circle,
                  border: Border.all(
                    color: AppColors.primary.withValues(alpha: 0.25 + _pulse.value * 0.3),
                    width: 1.5,
                  ),
                ),
                child: Icon(Icons.center_focus_weak,
                    color: Colors.white.withValues(alpha: 0.35)),
              ),
            ),
          ),

          // ── Couche augmentée : cartes flottantes avec parallaxe ──
          SafeArea(
            child: Column(
              children: [
                const Spacer(),
                Padding(
                  padding: const EdgeInsets.symmetric(horizontal: 24),
                  child: Transform(
                    alignment: FractionalOffset.center,
                    transform: Matrix4.identity()
                      ..setEntry(3, 2, 0.002)
                      ..rotateX(-_tiltY * 0.12)
                      ..rotateY(_tiltX * 0.18)
                      ..translateByDouble(_tiltX * 8, _tiltY * 6, 0, 1),
                    child: GlassCard(
                      blur: 24,
                      child: Column(
                        mainAxisSize: MainAxisSize.min,
                        children: [
                          CircleAvatar(
                            radius: 28,
                            backgroundColor:
                                AppColors.primary.withValues(alpha: 0.18),
                            child: Icon(_steps[_step].icon,
                                color: AppColors.primaryLight, size: 30),
                          ),
                          const SizedBox(height: 14),
                          Text(_steps[_step].title,
                              textAlign: TextAlign.center,
                              style: const TextStyle(
                                  color: Colors.white,
                                  fontSize: 20,
                                  fontWeight: FontWeight.w700)),
                          const SizedBox(height: 8),
                          Text(_steps[_step].body,
                              textAlign: TextAlign.center,
                              style: const TextStyle(
                                  color: Colors.white70, fontSize: 13.5,
                                  height: 1.45)),
                        ],
                      ),
                    ),
                  ),
                ),
                const SizedBox(height: 20),

                // Points de progression
                Row(
                  mainAxisAlignment: MainAxisAlignment.center,
                  children: List.generate(_stepCount, (i) {
                    final active = i == _step;
                    return AnimatedContainer(
                      duration: const Duration(milliseconds: 250),
                      margin: const EdgeInsets.symmetric(horizontal: 4),
                      width: active ? 22 : 8,
                      height: 8,
                      decoration: BoxDecoration(
                        color: active ? AppColors.primaryLight : Colors.white24,
                        borderRadius: BorderRadius.circular(4),
                      ),
                    );
                  }),
                ),
                const SizedBox(height: 20),

                // Actions
                Padding(
                  padding: const EdgeInsets.fromLTRB(24, 0, 24, 12),
                  child: Row(
                    children: [
                      TextButton(
                        onPressed: () => _finish(context),
                        child: const Text('Passer',
                            style: TextStyle(color: Colors.white54)),
                      ),
                      const Spacer(),
                      FilledButton.icon(
                        style: FilledButton.styleFrom(
                          backgroundColor: AppColors.primary,
                        ),
                        onPressed: () {
                          if (_step < _stepCount - 1) {
                            setState(() => _step++);
                          } else {
                            _finish(context);
                          }
                        },
                        icon: Icon(
                            _step < _stepCount - 1
                                ? Icons.arrow_forward_rounded
                                : Icons.rocket_launch_rounded,
                            size: 18),
                        label: Text(_step < _stepCount - 1
                            ? 'Suivant'
                            : 'Commencer'),
                      ),
                    ],
                  ),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }
}
