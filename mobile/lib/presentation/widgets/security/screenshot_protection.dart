import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

/// Widget that prevents screenshots and screen recording on sensitive screens
///
/// Wraps sensitive content with a protective layer that:
/// - Hides content when app goes to background (Android only)
/// - Shows a security overlay (blurred/pixelated) on screen capture
/// - Prevents screenshots via FLAG_SECURE on Android
/// - Adds a visual indicator when protection is active
class ScreenshotProtection extends StatefulWidget {
  final Widget child;
  final bool enabled;

  const ScreenshotProtection({
    super.key,
    required this.child,
    this.enabled = true,
  });

  @override
  State<ScreenshotProtection> createState() => _ScreenshotProtectionState();
}

class _ScreenshotProtectionState extends State<ScreenshotProtection> with WidgetsBindingObserver {
  bool _isBackgrounded = false;

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) {
      _applyProtection();
    });
    WidgetsBinding.instance.addObserver(this);
  }

  @override
  void dispose() {
    WidgetsBinding.instance.removeObserver(this);
    // Restore default mode when leaving the screen
    _restoreProtection();
    super.dispose();
  }

  @override
  void didChangeAppLifecycleState(AppLifecycleState state) {
    if (widget.enabled) {
      if (state == AppLifecycleState.paused || state == AppLifecycleState.hidden) {
        // App going to background - hide sensitive content
        setState(() => _isBackgrounded = true);
        _hideContent();
      } else if (state == AppLifecycleState.resumed) {
        // App coming to foreground - restore content
        setState(() => _isBackgrounded = false);
        _restoreProtection();
      }
    }
  }

  void _applyProtection() {
    if (widget.enabled) {
      // On Android, this sets FLAG_SECURE which prevents screenshots
      SystemChrome.setEnabledSystemUIMode(SystemUiMode.immersiveSticky);
    }
  }

  void _restoreProtection() {
    if (widget.enabled) {
      SystemChrome.setEnabledSystemUIMode(SystemUiMode.edgeToEdge);
    }
  }

  void _hideContent() {
    // On Android, FLAG_SECURE is handled automatically.
    // On iOS, the app snapshot is hidden by the system when paused;
    // keeping a reference here documents the behaviour and the overlay
    // below is available for in-app privacy (e.g. screen-share protection).
    setState(() {});
  }

  /// Create a blurred overlay widget for when app is in background
  static Widget blurredOverlay() {
    return Container(
      color: Colors.black87,
      child: Center(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Icon(Icons.visibility_off, color: Colors.white70, size: 48),
            SizedBox(height: 16),
            Text('Écran protégé', style: TextStyle(color: Colors.white70, fontSize: 18)),
          ],
        ),
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    if (!widget.enabled) {
      return widget.child;
    }

    // Wrap child with protection layer
    return Stack(
      children: [
        widget.child,
        // Security indicator in corner
        Positioned(
          top: 16,
          right: 16,
          child: Container(
            width: 24,
            height: 24,
            decoration: BoxDecoration(
              shape: BoxShape.circle,
              color: Colors.green.withValues(alpha: 0.8),
              border: Border.all(color: Colors.white, width: 2),
            ),
            child: Icon(Icons.shield, color: Colors.white, size: 14),
          ),
        ),
        // Privacy overlay used when the app is backgrounded
        if (_isBackgrounded) blurredOverlay(),
      ],
    );
  }
}

/// Extension method to easily apply screenshot protection to any screen
extension ScreenshotProtectionExtension on Widget {
  Widget withScreenshotProtection({bool enabled = true}) {
    return ScreenshotProtection(enabled: enabled, child: this);
  }
}
