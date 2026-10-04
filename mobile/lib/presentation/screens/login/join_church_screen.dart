import 'package:dio/dio.dart';
import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import '../../../app.dart';
import '../../../data/services/api_service.dart';
import '../../widgets/glass_theme.dart';

/// SPEC_ONBOARDING_FLOWS (MO-1) — rejointure par code.
/// Saisie du code d'église → résolution publique (/public/join/lookup, vitrine
/// sans PII) → selon l'état de session :
///  • connecté → POST /tenant/join (OPEN = entrée directe, sans resaisie du
///    code aux connexions suivantes — D7) ;
///  • non connecté → inscription avec le code pré-transmis, ou connexion.
class JoinChurchScreen extends StatefulWidget {
  const JoinChurchScreen({super.key, this.initialCode});

  final String? initialCode;

  @override
  State<JoinChurchScreen> createState() => _JoinChurchScreenState();
}

class _JoinChurchScreenState extends State<JoinChurchScreen> {
  final _codeController = TextEditingController();
  final _apiService = ApiService();

  bool _looking = false;
  bool _joining = false;
  String? _error;
  Map<String, dynamic>? _lookup;
  bool _done = false;
  String? _pendingApproval;

  @override
  void initState() {
    super.initState();
    final seed = widget.initialCode?.trim();
    if (seed != null && seed.isNotEmpty) {
      _codeController.text = seed.toUpperCase();
      WidgetsBinding.instance.addPostFrameCallback((_) => _lookupCode());
    }
  }

  @override
  void dispose() {
    _codeController.dispose();
    super.dispose();
  }

  Future<void> _lookupCode() async {
    final value = _codeController.text.trim();
    if (value.isEmpty) {
      setState(() => _error = "Saisissez le code de votre église.");
      return;
    }
    setState(() { _looking = true; _error = null; _lookup = null; });
    try {
      final res = await _apiService.post('/public/join/lookup', data: {'code': value});
      final data = res.data as Map<String, dynamic>? ?? const <String, dynamic>{};
      if (!mounted) return;
      if (data['found'] == true) {
        setState(() => _lookup = data);
      } else {
        setState(() => _error = data['reason'] == 'RATE_LIMITED'
            ? 'Trop de tentatives. Réessayez dans une minute.'
            : 'Aucune église ne correspond à ce code.');
      }
    } on DioException catch (e) {
      if (!mounted) return;
      setState(() => _error = e.response?.data?['detail'] as String?
          ?? e.response?.data?['message'] as String?
          ?? 'Vérification impossible. Réessayez.');
    } finally {
      if (mounted) setState(() => _looking = false);
    }
  }

  Future<void> _joinAuthenticated() async {
    setState(() { _joining = true; _error = null; });
    try {
      final res = await _apiService.post('/tenant/join',
          data: {'code': _codeController.text.trim()});
      final status = (res.data as Map<String, dynamic>?)?['status'] as String?;
      if (!mounted) return;
      if (status == 'PENDING_APPROVAL') {
        setState(() { _pendingApproval = _lookup?['churchName']?.toString(); _done = true; });
      } else {
        // Entrée directe : rechargement de session sur la nouvelle église (D7).
        final me = await _apiService.get('/auth/me');
        if (!mounted) return;
        AuthState().setAuthenticated(true,
            userData: me.data as Map<String, dynamic>?);
        setState(() => _done = true);
        context.go(roleHome(AuthState().activeRole,
            isPlatformSuperAdmin: AuthState().isPlatformSuperAdmin));
      }
    } on DioException catch (e) {
      if (!mounted) return;
      setState(() => _error = e.response?.data?['detail'] as String?
          ?? e.response?.data?['message'] as String?
          ?? 'Rejointure impossible. Réessayez.');
    } finally {
      if (mounted) setState(() => _joining = false);
    }
  }

  void _registerWithCode() {
    final church = Uri.encodeComponent(_lookup?['churchName']?.toString() ?? '');
    final code = Uri.encodeComponent(_codeController.text.trim());
    context.go('/register?joinCode=$code&church=$church');
  }

  @override
  Widget build(BuildContext context) {
    final authed = AuthState().isAuthenticated;

    return Scaffold(
      body: Container(
        decoration: const BoxDecoration(
          gradient: LinearGradient(
            begin: Alignment.topLeft,
            end: Alignment.bottomRight,
            colors: [Color(0xFF030712), Color(0xFF0F172A), Color(0xFF030712)],
          ),
        ),
        child: SafeArea(
          child: Center(
            child: SingleChildScrollView(
              padding: const EdgeInsets.all(24),
              child: ConstrainedBox(
                constraints: const BoxConstraints(maxWidth: 480),
                child: Column(
                  mainAxisAlignment: MainAxisAlignment.center,
                  children: [
                    Container(
                      width: 72,
                      height: 72,
                      decoration: BoxDecoration(
                        gradient: LinearGradient(
                          colors: [AppColors.primary, AppColors.primaryLight],
                          begin: Alignment.topLeft,
                          end: Alignment.bottomRight,
                        ),
                        borderRadius: BorderRadius.circular(20),
                        boxShadow: [BoxShadow(color: AppColors.primary.withValues(alpha: 0.4), blurRadius: 20, spreadRadius: 2)],
                      ),
                      child: const Icon(Icons.vpn_key_rounded, color: Colors.white, size: 34),
                    ),
                    const SizedBox(height: 20),
                    Text(
                      'Rejoindre une église',
                      style: Theme.of(context).textTheme.headlineSmall?.copyWith(
                        fontWeight: FontWeight.bold, color: Colors.white),
                    ),
                    const SizedBox(height: 8),
                    Text(
                      "Saisissez le code d'entrée fourni par votre église (ex : BETHEL-7K2M).",
                      textAlign: TextAlign.center,
                      style: TextStyle(color: Colors.white.withValues(alpha: 0.5), fontSize: 13),
                    ),
                    const SizedBox(height: 28),

                    if (_done) ...[
                      const Icon(Icons.check_circle_rounded, color: Colors.green, size: 56),
                      const SizedBox(height: 12),
                      Text(
                        _pendingApproval != null ? 'Demande transmise' : 'Bienvenue dans votre église !',
                        textAlign: TextAlign.center,
                        style: TextStyle(color: Colors.white.withValues(alpha: 0.9), fontSize: 18, fontWeight: FontWeight.bold),
                      ),
                      const SizedBox(height: 8),
                      Text(
                        _pendingApproval != null
                            ? 'Un responsable de ${_pendingApproval ?? "l’église"} examinera votre demande puis vous contactera.'
                            : 'Vous y êtes rattaché durablement : plus besoin de saisir le code.',
                        textAlign: TextAlign.center,
                        style: TextStyle(color: Colors.white.withValues(alpha: 0.6), fontSize: 13),
                      ),
                      if (_pendingApproval != null) ...[
                        const SizedBox(height: 24),
                        SizedBox(
                          width: double.infinity, height: 50,
                          child: OutlinedButton(
                            onPressed: () => context.go('/login'),
                            child: const Text('Se connecter plus tard'),
                          ),
                        ),
                      ],
                    ] else ...[
                      if (_error != null)
                        GlassCard(
                          padding: const EdgeInsets.all(12),
                          margin: const EdgeInsets.only(bottom: 16),
                          borderColor: Colors.red.withValues(alpha: 0.3),
                          child: Row(children: [
                            const Icon(Icons.error_outline, color: Colors.red, size: 20),
                            const SizedBox(width: 8),
                            Expanded(child: Text(_error!, style: const TextStyle(color: Colors.red, fontSize: 13))),
                          ]),
                        ),

                      if (_lookup != null && _lookup!['found'] == true)
                        _churchCard(authed)
                      else
                        _codeInput(),

                      const SizedBox(height: 20),
                      GlassDivider(),
                      const SizedBox(height: 12),
                      Row(
                        mainAxisAlignment: MainAxisAlignment.center,
                        children: [
                          Text("Pas de code ? ", style: TextStyle(color: Colors.white.withValues(alpha: 0.4), fontSize: 13)),
                          TextButton(
                            onPressed: () => context.go('/register?mode=church'),
                            child: const Text('Créez votre église', style: TextStyle(fontSize: 13)),
                          ),
                        ],
                      ),
                    ],
                  ],
                ),
              ),
            ),
          ),
        ),
      ),
    );
  }

  Widget _codeInput() {
    return Column(
      children: [
        TextField(
          controller: _codeController,
          textCapitalization: TextCapitalization.characters,
          style: const TextStyle(color: Colors.white, fontFamily: 'monospace', letterSpacing: 1.5, fontSize: 16),
          keyboardType: TextInputType.text,
          onSubmitted: (_) => _lookupCode(),
          decoration: InputDecoration(
            labelText: "Code de l'église",
            prefixIcon: const Icon(Icons.key_rounded),
            suffixIcon: _looking
                ? const Padding(padding: EdgeInsets.all(14), child: SizedBox(width: 20, height: 20, child: CircularProgressIndicator(strokeWidth: 2)))
                : null,
          ),
        ),
        const SizedBox(height: 16),
        SizedBox(
          width: double.infinity, height: 50,
          child: FilledButton.icon(
            onPressed: _looking ? null : _lookupCode,
            icon: const Icon(Icons.search_rounded, size: 18),
            label: const Text('Vérifier le code', style: TextStyle(fontSize: 15)),
          ),
        ),
        const SizedBox(height: 10),
        Row(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Icon(Icons.shield_outlined, size: 14, color: Colors.white.withValues(alpha: 0.35)),
            const SizedBox(width: 6),
            Expanded(
              child: Text(
                'Le code sert une seule fois : après la rejointure, vous entrez directement à chaque connexion.',
                style: TextStyle(color: Colors.white.withValues(alpha: 0.35), fontSize: 11),
              ),
            ),
          ],
        ),
      ],
    );
  }

  Widget _churchCard(bool authed) {
    final name = _lookup?['churchName']?.toString() ?? '';
    final node = _lookup?['orgNodeLabel']?.toString();
    final requiresApproval = _lookup?['requiresApproval'] == true;

    return GlassCard(
      padding: const EdgeInsets.all(18),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Container(
                width: 44, height: 44,
                decoration: BoxDecoration(
                  gradient: LinearGradient(colors: [AppColors.primary, AppColors.primaryLight]),
                  borderRadius: BorderRadius.circular(12),
                ),
                child: const Icon(Icons.church_rounded, color: Colors.white, size: 22),
              ),
              const SizedBox(width: 12),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(name.isEmpty ? 'Église' : name,
                        style: const TextStyle(color: Colors.white, fontSize: 16, fontWeight: FontWeight.bold)),
                    if (node != null && node.isNotEmpty)
                      Text(node, style: TextStyle(color: Colors.white.withValues(alpha: 0.5), fontSize: 12)),
                  ],
                ),
              ),
              TextButton(
                onPressed: () => setState(() { _lookup = null; }),
                child: const Text('Modifier', style: TextStyle(fontSize: 12)),
              ),
            ],
          ),
          if (requiresApproval) ...[
            const SizedBox(height: 8),
            Container(
              padding: const EdgeInsets.all(10),
              decoration: BoxDecoration(
                color: Colors.amber.withValues(alpha: 0.1),
                borderRadius: BorderRadius.circular(10),
              ),
              child: Row(children: [
                const Icon(Icons.hourglass_top_rounded, color: Colors.amber, size: 16),
                const SizedBox(width: 8),
                Expanded(child: Text('Cette église valide chaque demande avant l’entrée.',
                    style: TextStyle(color: Colors.amber.shade200, fontSize: 12))),
              ]),
            ),
          ],
          const SizedBox(height: 16),
          SizedBox(
            width: double.infinity, height: 50,
            child: FilledButton.icon(
              onPressed: _joining ? null : (authed ? _joinAuthenticated : _registerWithCode),
              icon: _joining
                  ? const SizedBox(width: 18, height: 18, child: CircularProgressIndicator(strokeWidth: 2, color: Colors.white))
                  : const Icon(Icons.arrow_forward_rounded, size: 18),
              label: Text(authed ? 'Rejoindre maintenant' : 'Créer un compte et rejoindre',
                  style: const TextStyle(fontSize: 15)),
            ),
          ),
          if (!authed) ...[
            const SizedBox(height: 8),
            SizedBox(
              width: double.infinity, height: 48,
              child: OutlinedButton.icon(
                onPressed: () => context.go('/login'),
                icon: const Icon(Icons.login, size: 18),
                label: const Text('J’ai déjà un compte — me connecter'),
              ),
            ),
          ],
        ],
      ),
    );
  }
}
