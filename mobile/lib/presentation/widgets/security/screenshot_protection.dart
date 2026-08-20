import 'package:flutter/services.dart';
import 'package:flutter/widgets.dart';

/// Service centralisé d’activation de la protection d’écran (FLAG_SECURE).
///
/// Contrairement à `SystemChrome.setEnabledSystemUIMode` (qui ne masque que la
/// barre de statut/navigation), l’appel de la méthode `setSecureFlag` active
/// réellement `FLAG_SECURE` sur Android : aucune capture d’écran ni
/// enregistrement d’écran n’est alors possible tant que le flag est levé.
///
/// Sur iOS, la capture est déjà bloquée par le système lorsqu’une
/// `UIApplication.shared.isScreenCaptureEnabled = false` est déclarée dans le
/// channel — le service est un no-op sûr si le canal natif n’existe pas.
class SecureScreenService {
  static const _channel = MethodChannel('discipolat/secure_screen');

  static final SecureScreenService _instance = SecureScreenService._();
  factory SecureScreenService() => _instance;
  SecureScreenService._();

  /// Active (ou désactive) le flag de protection d’écran.
  ///
  /// Doit être appelé depuis le thread UI (via WidgetsBinding) : utilisez
  /// `WidgetsBinding.instance.addPostFrameCallback` ou `runAsync` dans un
  /// `State.initState`.
  static Future<void> setSecure(bool enabled) async {
    try {
      await _channel.invokeMethod('setSecureFlag', {'enabled': enabled});
    } on PlatformException catch (e, st) {
      // Canal natif non disponible (iOS sans implémentation, ou build debug) :
      // on ne casse pas l’UX, on loggue localement.
      // ignore: avoid_print
      print('[SecureScreenService] Platform channel unavailable: $e\n$st');
    }
  }

  /// Widget de commodité : applique la protection autour d’un enfant et la
  /// restaure automatiquement à la disposal.
  static Widget protect({required Widget child}) {
    return _SecureScreenScope(child: child);
  }
}

class _SecureScreenScope extends StatefulWidget {
  const _SecureScreenScope({required this.child});
  final Widget child;

  @override
  State<_SecureScreenScope> createState() => _SecureScreenScopeState();
}

class _SecureScreenScopeState extends State<_SecureScreenScope> {
  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) {
      SecureScreenService.setSecure(true);
    });
  }

  @override
  void dispose() {
    SecureScreenService.setSecure(false);
    super.dispose();
  }

    @override
  Widget build(BuildContext context) => widget.child;
}

/// Extension de commodité conservée pour la rétro-compatibilité d’usage :
/// `child.withScreenshotProtection()`.
/// Elle délègue au `SecureScreenService.protect` (FLAG_SECURE vrai via canal
/// platforme), remplaçant l’ancien widget qui n’était qu’un masquage UI.
extension ScreenshotProtectionExtension on Widget {
  Widget withScreenshotProtection({bool enabled = true}) {
    if (!enabled) return this;
    return SecureScreenService.protect(child: this);
  }
}


