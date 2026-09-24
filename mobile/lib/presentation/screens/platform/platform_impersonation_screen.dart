import 'package:flutter/material.dart';

import '../../../app.dart';
import '../../../data/services/impersonation_service.dart';
import '../../widgets/app_drawer.dart';
import '../../widgets/glass_theme.dart';
import '../../widgets/secure_screen.dart';

class PlatformImpersonationScreen extends StatefulWidget {
  const PlatformImpersonationScreen({super.key, this.impersonationService});

  final ImpersonationService? impersonationService;

  @override
  State<PlatformImpersonationScreen> createState() =>
      _PlatformImpersonationScreenState();
}

class _PlatformImpersonationScreenState
    extends State<PlatformImpersonationScreen> {
  late final ImpersonationService _service =
      widget.impersonationService ?? ImpersonationService.instance;
  final _tenantController = TextEditingController();
  final _emailController = TextEditingController();
  final _reasonController = TextEditingController();
  bool _loading = false;
  String? _error;

  @override
  void dispose() {
    _tenantController.dispose();
    _emailController.dispose();
    _reasonController.dispose();
    super.dispose();
  }

  Future<void> _start() async {
    final tenantId = _tenantController.text.trim();
    final email = _emailController.text.trim();
    final reason = _reasonController.text.trim();
    if (tenantId.isEmpty) {
      setState(() => _error = 'Tenant ID requis.');
      return;
    }
    if (email.isEmpty) {
      setState(() => _error = 'Email cible requis.');
      return;
    }
    if (reason.length < 10) {
      setState(() => _error = 'Le motif doit contenir au moins 10 caractères.');
      return;
    }
    setState(() {
      _loading = true;
      _error = null;
    });
    try {
      await _service.start(
        targetEmail: email,
        reason: reason,
        tenantId: tenantId,
      );
      if (!mounted) return;
      appRouter.go(roleHome(AuthState().activeRole));
    } catch (_) {
      if (!mounted) return;
      setState(() {
        _loading = false;
        _error = 'Impossible de démarrer l\'impersonation.';
      });
    }
  }

  @override
  Widget build(BuildContext context) {
    return SecureScreen(
      screenName: 'PlatformImpersonationScreen',
      auditAction: 'platformImpersonationStart',
      child: Scaffold(
        appBar: AppBar(title: const Text('Impersonation')),
        drawer: const AppDrawer(),
        body: ListView(
          padding: const EdgeInsets.all(20),
          children: [
            GlassCard(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  const Text(
                    'Les actions réalisées pendant l\'impersonation sont journalisées. '
                    'La session cible expire automatiquement.',
                    style: TextStyle(color: Colors.white70),
                  ),
                  const SizedBox(height: 20),
                  TextField(
                    controller: _tenantController,
                    decoration: const InputDecoration(
                      labelText: 'Tenant ID',
                      hintText: 'UUID du tenant',
                    ),
                  ),
                  const SizedBox(height: 12),
                  TextField(
                    controller: _emailController,
                    keyboardType: TextInputType.emailAddress,
                    decoration: const InputDecoration(
                        labelText: 'Email utilisateur cible'),
                  ),
                  const SizedBox(height: 12),
                  TextField(
                    controller: _reasonController,
                    maxLines: 3,
                    decoration: const InputDecoration(
                        labelText: 'Motif de l\'impersonation'),
                  ),
                  if (_error != null) ...[
                    const SizedBox(height: 12),
                    Text(_error!,
                        style: const TextStyle(color: Colors.redAccent)),
                  ],
                  const SizedBox(height: 20),
                  FilledButton.icon(
                    onPressed: _loading ? null : _start,
                    icon: _loading
                        ? const SizedBox(
                            width: 18,
                            height: 18,
                            child: CircularProgressIndicator(strokeWidth: 2),
                          )
                        : const Icon(Icons.login_rounded),
                    label: const Text('Démarrer l\'impersonation'),
                  ),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }
}
