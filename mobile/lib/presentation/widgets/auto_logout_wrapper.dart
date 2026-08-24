import 'package:flutter/material.dart';
import 'dart:async';

/// P2 #73 — Auto-logout après inactivité (mobile)
/// Wrapper qui détecte l'inactivité et redirige vers l'écran de connexion.
class AutoLogoutWrapper extends StatefulWidget {
  final Widget child;
  final Duration timeout;
  final VoidCallback? onLogout;

  const AutoLogoutWrapper({
    super.key,
    required this.child,
    this.timeout = const Duration(minutes: 15),
    this.onLogout,
  });

  @override
  State<AutoLogoutWrapper> createState() => _AutoLogoutWrapperState();
}

class _AutoLogoutWrapperState extends State<AutoLogoutWrapper> {
  Timer? _inactivityTimer;
  Timer? _warningTimer;
  bool _showWarning = false;

  @override
  void initState() {
    super.initState();
    _resetTimer();
  }

  @override
  void dispose() {
    _inactivityTimer?.cancel();
    _warningTimer?.cancel();
    super.dispose();
  }

  void _resetTimer() {
    _inactivityTimer?.cancel();
    _warningTimer?.cancel();
    setState(() => _showWarning = false);

    // Show warning 2 minutes before timeout
    final warningTime = widget.timeout - const Duration(minutes: 2);
    if (warningTime.inSeconds > 0) {
      _warningTimer = Timer(warningTime, () {
        if (mounted) setState(() => _showWarning = true);
      });
    }

    _inactivityTimer = Timer(widget.timeout, _logout);
  }

  void _logout() {
    widget.onLogout?.call();
    if (mounted) {
      Navigator.of(context).pushNamedAndRemoveUntil('/', (route) => false);
    }
  }

  void _handleUserInteraction([_]) {
    if (_showWarning) {
      setState(() => _showWarning = false);
    }
    _resetTimer();
  }

  @override
  Widget build(BuildContext context) {
    return GestureDetector(
      onTap: _handleUserInteraction,
      onPanDown: _handleUserInteraction,
      behavior: HitTestBehavior.translucent,
      child: Stack(
        children: [
          widget.child,
          if (_showWarning)
            Positioned(
              bottom: 0,
              left: 0,
              right: 0,
              child: Material(
                color: Colors.orange.shade700,
                child: SafeArea(
                  child: Padding(
                    padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
                    child: Row(
                      children: [
                        const Icon(Icons.warning_amber_rounded, color: Colors.white, size: 20),
                        const SizedBox(width: 10),
                        const Expanded(
                          child: Text(
                            'Session expire dans 2 minutes. Touchez pour continuer.',
                            style: TextStyle(color: Colors.white, fontSize: 13),
                          ),
                        ),
                        TextButton(
                          onPressed: _logout,
                          child: const Text('Déconnexion', style: TextStyle(color: Colors.white)),
                        ),
                      ],
                    ),
                  ),
                ),
              ),
            ),
        ],
      ),
    );
  }
}
