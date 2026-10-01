import 'package:dio/dio.dart';
import 'package:flutter/material.dart';

import '../../../data/services/api_service.dart';
import '../../features/auth/social/google_credential_source.dart';
import '../../features/auth/social/microsoft_credential_source.dart';
import '../../features/auth/social/social_auth_service.dart';
import '../../features/auth/social/social_credential.dart';

/// Boutons « Se connecter avec Google / Microsoft ».
///
/// Principes tenus ici :
///
/// * **Aucun bouton mort** : le serveur déclare lui-même les fournisseurs actifs
///   (`GET /auth/social/providers`). Un bouton affiché alors que le serveur ne
///   sert pas le fournisseur échouerait en 503 au clic.
/// * **Aucun secret dans l'application** : les client-id sont publics par nature ;
///   la confiance repose sur la signature vérifiée par le serveur, jamais sur eux.
/// * **Échecs honnêtes** : « aucun compte pour cette adresse » propose
///   l'invitation ; une annulation reste silencieuse.
class SocialLoginButtons extends StatefulWidget {
  const SocialLoginButtons({
    super.key,
    required this.apiService,
    required this.onAuthenticated,
    this.googleServerClientId = '',
    this.microsoftClientId = '',
    this.microsoftTenantId = 'common',
    this.microsoftRedirectUri = 'com.discipolat.app://auth/microsoft',
  });

  final ApiService apiService;

  /// Reçoit la réponse complète (mêmes champs que `POST /auth/login`).
  final void Function(Map<String, dynamic> response) onAuthenticated;

  /// Client-id **web** Google : c'est lui qui fournit l'`id_token` exploitable
  /// côté serveur sur Android.
  final String googleServerClientId;
  final String microsoftClientId;
  final String microsoftTenantId;
  final String microsoftRedirectUri;

  @override
  State<SocialLoginButtons> createState() => _SocialLoginButtonsState();
}

class _SocialLoginButtonsState extends State<SocialLoginButtons> {
  late final SocialAuthService _service = SocialAuthService(
    sources: {
      if (widget.googleServerClientId.isNotEmpty)
        SocialProvider.google: GoogleCredentialSource(
          serverClientId: widget.googleServerClientId,
        ),
      if (widget.microsoftClientId.isNotEmpty)
        SocialProvider.microsoft: MicrosoftCredentialSource(
          clientId: widget.microsoftClientId,
          tenantId: widget.microsoftTenantId,
          redirectUri: widget.microsoftRedirectUri,
        ),
    },
  );

  List<SocialProvider> _providers = const [];
  SocialProvider? _pending;
  String? _error;
  bool _loading = true;

  @override
  void initState() {
    super.initState();
    _loadProviders();
  }

  Future<void> _loadProviders() async {
    List<SocialProvider> providers = const [];
    try {
      final response = await widget.apiService.get('/auth/social/providers');
      final Object? raw = response.data is Map
          ? (response.data as Map)['providers']
          : null;
      if (raw is List) {
        providers = _service.enabledProviders(raw);
      }
    } catch (_) {
      // Réseau indisponible : on n'affiche rien plutôt qu'un bouton qui échoue.
      providers = const [];
    }
    if (!mounted) return;
    setState(() {
      _providers = providers;
      _loading = false;
    });
  }

  Future<void> _connect(SocialProvider provider) async {
    setState(() {
      _pending = provider;
      _error = null;
    });
    try {
      final SocialCredential credential = await _service.signIn(provider);
      final response = await widget.apiService.post(
        '/auth/social/${provider.wireName}',
        data: {'credential': credential.idToken},
      );
      final Map<String, dynamic> data =
          Map<String, dynamic>.from(response.data as Map);
      widget.onAuthenticated(data);
    } on SocialAuthException catch (failure) {
      if (failure.isCancelled) return;
      if (!mounted) return;
      setState(() => _error = _messageForCode(failure.code, provider));
      if (failure.providerUnavailable) {
        // Le serveur ne sert plus ce fournisseur : on retire le bouton.
        await _loadProviders();
      }
    } on DioException catch (error) {
      if (!mounted) return;
      final Map<String, dynamic>? body =
          error.response?.data is Map ? error.response!.data as Map<String, dynamic> : null;
      final String? code = body?['title'] as String?;
      setState(() => _error = _messageForCode(code, provider));
    } finally {
      if (mounted) setState(() => _pending = null);
    }
  }

  String _messageForCode(String? code, SocialProvider provider) {
    switch (code) {
      case 'SOCIAL_ACCOUNT_NOT_LINKED':
        return "Aucun compte Discipolat pour cette adresse. Utilisez le lien "
            "d'invitation reçu de votre église.";
      case 'ACCOUNT_NOT_ACTIVATED':
        return 'Ce compte attend encore son activation. Consultez vos emails.';
      case 'ACCOUNT_INACTIVE':
        return "Ce compte est désactivé. Contactez l'administrateur de votre église.";
      case 'ACCOUNT_LOCKED':
        return 'Ce compte est temporairement verrouillé après plusieurs tentatives.';
      case 'SOCIAL_EMAIL_NOT_VERIFIED':
        return "Le fournisseur n'a pas confirmé votre adresse email.";
      case 'SOCIAL_EMAIL_MISMATCH':
        return "L'adresse vérifiée par ce compte externe ne correspond pas à votre "
            'compte Discipolat.';
      case 'SOCIAL_TENANT_NOT_ALLOWED':
        return "Votre organisation n'est pas autorisée sur cette plateforme.";
      case 'SOCIAL_IDENTITY_ALREADY_LINKED':
      case 'SOCIAL_PROVIDER_ALREADY_LINKED':
        return 'Cette adresse est déjà rattachée à un autre compte.';
      case 'SOCIAL_PROVIDER_NOT_CONFIGURED':
        return 'La connexion avec ${provider.label} n\'est pas configurée sur ce serveur.';
      case 'RATE_LIMITED':
        return 'Trop de tentatives. Patientez quelques instants.';
      default:
        return 'Connexion impossible. Veuillez réessayer.';
    }
  }

  @override
  Widget build(BuildContext context) {
    if (_loading || _providers.isEmpty) {
      // Tant que l'état est inconnu, on n'affiche rien : afficher puis retirer
      // un bouton est pire que de ne jamais l'afficher.
      return const SizedBox.shrink();
    }

    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        Row(
          children: [
            const Expanded(child: Divider()),
            Padding(
              padding: const EdgeInsets.symmetric(horizontal: 12),
              child: Text(
                'ou',
                style: TextStyle(color: Colors.grey.shade500, fontSize: 12),
              ),
            ),
            const Expanded(child: Divider()),
          ],
        ),
        const SizedBox(height: 16),
        ..._providers.map(_button),
        if (_error != null) ...[
          const SizedBox(height: 12),
          Semantics(
            liveRegion: true,
            child: Text(
              _error!,
              style: TextStyle(color: Theme.of(context).colorScheme.error, fontSize: 13),
            ),
          ),
        ],
      ],
    );
  }

  Widget _button(SocialProvider provider) {
    final bool busy = _pending == provider;
    return Padding(
      padding: const EdgeInsets.only(bottom: 12),
      child: Semantics(
        button: true,
        enabled: _pending == null,
        label: 'Se connecter avec ${provider.label}',
        child: OutlinedButton.icon(
          onPressed: _pending == null ? () => _connect(provider) : null,
          icon: busy
              ? const SizedBox(
                  width: 18,
                  height: 18,
                  child: CircularProgressIndicator(strokeWidth: 2),
                )
              : Icon(_iconFor(provider), size: 20),
          label: Text(busy ? 'Connexion…' : 'Se connecter avec ${provider.label}'),
          style: OutlinedButton.styleFrom(
            minimumSize: const Size.fromHeight(50),
          ),
        ),
      ),
    );
  }

  IconData _iconFor(SocialProvider provider) =>
      provider == SocialProvider.google ? Icons.g_mobiledata : Icons.work_outline;
}
