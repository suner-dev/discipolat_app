// Écran de transfert de membre — SPEC_ORGANISATION_DENOMINATION_V2 §7.3 / T-M2.
//
// Miroir exact du flux web (§4.4, `TransferPage.tsx`) : saisir le code →
// prévisualiser → confirmer. L'écran applique le contrat du backend, il
// n'invente aucune règle — c'est `sameNetwork` qui décide si l'on parle de
// transfert ou d'adhésion.
//
// T-B0bis : les jetons sont persistés par `ApiService.transfer` (voir le
// commentaire qui explique pourquoi le claim `tenantId` devait impérativement
// être réémis). L'écran ne réécrit jamais le stockage de session lui-même.

import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';

// Chemins relatifs depuis `lib/presentation/screens/transfer/` :
// `../../` atteint `lib/presentation/`, `../../../` atteint `lib/`.
// Les imports ci-dessous suivent la même profondeur que
// `screens/login/join_church_screen.dart` (un cran plus bas qu'avant).
import '../../../app.dart';
import '../../../data/models/transfer_models.dart';
import '../../../data/services/api_service.dart';
import '../widgets/glass_theme.dart';
import '../widgets/secure_screen.dart';

class TransferScreen extends StatefulWidget {
  const TransferScreen({super.key});

  @override
  State<TransferScreen> createState() => _TransferScreenState();
}

class _TransferScreenState extends State<TransferScreen> {
  final _codeController = TextEditingController();
  final _reasonController = TextEditingController();
  final _apiService = ApiService();

  TransferPreview? _preview;
  TransferOutcome? _outcome;
  String? _error;
  bool _checking = false;
  bool _transferring = false;

  @override
  void dispose() {
    _codeController.dispose();
    _reasonController.dispose();
    super.dispose();
  }

  Future<void> _check() async {
    final code = _codeController.text.trim();
    if (code.isEmpty) return;
    setState(() {
      _checking = true;
      _error = null;
      _preview = null;
    });
    try {
      final preview = await _apiService.previewTransfer(code);
      if (!mounted) return;
      setState(() => _preview = preview);
    } on Exception catch (e) {
      if (!mounted) return;
      setState(() => _error = _readable(e));
    } finally {
      if (mounted) setState(() => _checking = false);
    }
  }

  Future<void> _confirm() async {
    setState(() {
      _transferring = true;
      _error = null;
    });
    try {
      final outcome = await _apiService.transfer(
        _codeController.text.trim(),
        reason: _reasonController.text.trim(),
      );
      if (!mounted) return;
      // Les jetons sont déjà persistés. Rafraîchir l'identité affichée est un
      // confort : un échec ici ne doit pas invalider une session correcte.
      try {
        final me = await _apiService.get('/auth/me');
        if (mounted) {
          AuthState().setAuthenticated(true,
              userData: me.data as Map<String, dynamic>?);
        }
      } on Exception {
        // Volontairement ignoré : voir commentaire ci-dessus.
      }
      if (!mounted) return;
      setState(() => _outcome = outcome);
    } on Exception catch (e) {
      if (!mounted) return;
      setState(() => _error = _readable(e));
    } finally {
      if (mounted) setState(() => _transferring = false);
    }
  }

  String _readable(Exception e) {
    final text = e.toString();
    if (text.contains('404')) return "Aucune église ne correspond à ce code.";
    if (text.contains('410')) {
      return "Cette église n'accepte pas d'adhésions actuellement.";
    }
    if (text.contains('429')) {
      return "Trop de tentatives. Réessayez dans une minute.";
    }
    return "Transfert impossible. Réessayez.";
  }

  void _goHome() {
    context.go(roleHome(AuthState().activeRole,
        isPlatformSuperAdmin: AuthState().isPlatformSuperAdmin));
  }

  @override
  Widget build(BuildContext context) {
    final outcome = _outcome;

    return SecureScreen(
      screenName: 'TransferScreen',
      auditAction: AuditActions.loginAttempt,
      child: Scaffold(
        body: Container(
          decoration: const BoxDecoration(
            gradient: LinearGradient(
              begin: Alignment.topLeft,
              end: Alignment.bottomRight,
              colors: [Color(0xFF030712), Color(0xFF0F172A), Color(0xFF030712)],
            ),
          ),
          child: SafeArea(
            child: outcome != null
                ? _resultView(outcome)
                : _formView(),
          ),
        ),
      ),
    );
  }

  Widget _resultView(TransferOutcome outcome) {
    final title = outcome.wasTransferred
        ? 'Vous avez été transféré'
        : outcome.alreadyMember
            ? 'Vous êtes déjà membre'
            : 'Vous avez rejoint cette église';

    return Center(
      child: SingleChildScrollView(
        padding: const EdgeInsets.all(24),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            const Icon(Icons.check_circle_rounded, color: Color(0xFF34D399), size: 64),
            const SizedBox(height: 16),
            Text(
              title,
              textAlign: TextAlign.center,
              style: const TextStyle(
                color: Colors.white, fontSize: 22, fontWeight: FontWeight.bold,
              ),
            ),
            const SizedBox(height: 8),
            Text(
              outcome.toChurch,
              textAlign: TextAlign.center,
              style: TextStyle(color: Colors.white.withValues(alpha: 0.6), fontSize: 14),
            ),
            if (outcome.wasTransferred) ...[
              const SizedBox(height: 8),
              Text(
                'Votre parcours pastoral est conservé : vous restez la même personne.',
                textAlign: TextAlign.center,
                style: TextStyle(color: Colors.white.withValues(alpha: 0.6), fontSize: 13),
              ),
            ],
            const SizedBox(height: 28),
            SizedBox(
              width: double.infinity,
              height: 50,
              child: FilledButton.icon(
                onPressed: _goHome,
                icon: const Icon(Icons.arrow_forward, size: 18),
                label: const Text('Entrer maintenant', style: TextStyle(fontSize: 15)),
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _formView() {
    final preview = _preview;
    final error = _error;

    return Center(
      child: SingleChildScrollView(
        padding: const EdgeInsets.all(24),
        child: ConstrainedBox(
          constraints: const BoxConstraints(maxWidth: 460),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              const Icon(Icons.swap_horiz_rounded, color: Color(0xFF4ADE80), size: 48),
              const SizedBox(height: 12),
              const Text(
                'Changer d’église',
                textAlign: TextAlign.center,
                style: TextStyle(
                  color: Colors.white, fontSize: 24, fontWeight: FontWeight.bold,
                ),
              ),
              const SizedBox(height: 8),
              Text(
                'Vous ne vous réinscrivez pas : votre compte, votre mot de passe et votre parcours pastoral vous suivent.',
                textAlign: TextAlign.center,
                style: TextStyle(color: Colors.white.withValues(alpha: 0.6), fontSize: 13),
              ),
              const SizedBox(height: 24),
              if (error != null) ...[
                GlassCard(
                  padding: const EdgeInsets.all(12),
                  child: Text(
                    error,
                    style: const TextStyle(color: Color(0xFFFCA5A5), fontSize: 13),
                  ),
                ),
                const SizedBox(height: 12),
              ],
              TextField(
                controller: _codeController,
                textCapitalization: TextCapitalization.characters,
                style: const TextStyle(
                  color: Colors.white, fontFamily: 'monospace', letterSpacing: 1,
                ),
                decoration: InputDecoration(
                  hintText: 'Code de la nouvelle église',
                  hintStyle: TextStyle(color: Colors.white.withValues(alpha: 0.4)),
                  prefixIcon: const Icon(Icons.key_rounded, color: Color(0xFF4ADE80)),
                  filled: true,
                  fillColor: Colors.white.withValues(alpha: 0.05),
                  border: OutlineInputBorder(borderRadius: BorderRadius.circular(12)),
                  enabledBorder: OutlineInputBorder(
                    borderRadius: BorderRadius.circular(12),
                    borderSide: BorderSide(color: Colors.white.withValues(alpha: 0.15)),
                  ),
                  focusedBorder: const OutlineInputBorder(
                    borderRadius: BorderRadius.all(Radius.circular(12)),
                    borderSide: BorderSide(color: Color(0xFF4ADE80)),
                  ),
                ),
                onSubmitted: (_) => _check(),
              ),
              const SizedBox(height: 12),
              SizedBox(
                height: 48,
                child: OutlinedButton.icon(
                  onPressed: _checking || _codeController.text.trim().isEmpty
                      ? null
                      : _check,
                  icon: _checking
                      ? const SizedBox(
                          width: 16, height: 16,
                          child: CircularProgressIndicator(strokeWidth: 2, color: Colors.white),
                        )
                      : const Icon(Icons.search_rounded, size: 18),
                  label: const Text('Vérifier'),
                ),
              ),
              if (preview != null) ...[
                const SizedBox(height: 20),
                GlassCard(
                  padding: const EdgeInsets.all(16),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.stretch,
                    children: [
                      Text(
                        preview.toChurch,
                        style: const TextStyle(
                          color: Colors.white, fontSize: 16, fontWeight: FontWeight.bold,
                        ),
                      ),
                      const SizedBox(height: 12),
                      if (preview.alreadyMember)
                        Text(
                          'Vous êtes déjà membre de cette église : rien à faire.',
                          style: TextStyle(color: Colors.white.withValues(alpha: 0.7), fontSize: 13),
                        )
                      else if (preview.willTransfer) ...[
                        Text(
                          'Bienvenue ! Vous êtes déjà membre de la même dénomination. '
                          'Vous rejoignez ${preview.toChurch} — votre parcours pastoral est conservé.',
                          style: TextStyle(color: Colors.white.withValues(alpha: 0.7), fontSize: 13),
                        ),
                        const SizedBox(height: 12),
                        TextField(
                          controller: _reasonController,
                          style: const TextStyle(color: Colors.white, fontSize: 13),
                          decoration: InputDecoration(
                            hintText: 'Motif du transfert (facultatif)',
                            hintStyle: TextStyle(color: Colors.white.withValues(alpha: 0.4)),
                            filled: true,
                            fillColor: Colors.white.withValues(alpha: 0.05),
                            border: OutlineInputBorder(borderRadius: BorderRadius.circular(10)),
                          ),
                        ),
                        const SizedBox(height: 12),
                        SizedBox(
                          height: 48,
                          child: FilledButton.icon(
                            onPressed: _transferring ? null : _confirm,
                            icon: _transferring
                                ? const SizedBox(
                                    width: 16, height: 16,
                                    child: CircularProgressIndicator(strokeWidth: 2),
                                  )
                                : const Icon(Icons.swap_horiz_rounded, size: 18),
                            label: const Text('Confirmer le transfert'),
                          ),
                        ),
                      ] else ...[
                        Text(
                          preview.activeInOther
                              ? 'Cette église n’appartient pas à votre réseau actuel. Vous allez la rejoindre '
                                  'EN PLUS — votre adhésion actuelle est conservée.'
                              : 'Cette église n’appartient pas à votre réseau actuel. Vous allez la rejoindre.',
                          style: TextStyle(color: Colors.white.withValues(alpha: 0.7), fontSize: 13),
                        ),
                        const SizedBox(height: 12),
                        SizedBox(
                          height: 48,
                          child: FilledButton.icon(
                            onPressed: _transferring ? null : _confirm,
                            icon: _transferring
                                ? const SizedBox(
                                    width: 16, height: 16,
                                    child: CircularProgressIndicator(strokeWidth: 2),
                                  )
                                : null,
                            label: const Text('Rejoindre cette église'),
                          ),
                        ),
                      ],
                    ],
                  ),
                ),
              ],
              const SizedBox(height: 20),
              Text(
                'Aucun nouveau compte, aucun nouveau mot de passe : c’est la même identité.',
                textAlign: TextAlign.center,
                style: TextStyle(color: Colors.white.withValues(alpha: 0.4), fontSize: 12),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
