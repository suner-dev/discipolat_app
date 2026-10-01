import 'social_credential.dart';

/// Orchestre les fournisseurs d'identité externe et la session Discipolat.
///
/// Règle upheld ici, identique au serveur : un credential externe
/// **n'authentifie que**. Il ne crée jamais de compte de son propre chef : un
/// compte Discipolat existe parce qu'une invitation a été acceptée, ou parce
/// qu'un compte antérieur porte la même adresse vérifiée.
class SocialAuthService {
  SocialAuthService({
    required Map<SocialProvider, SocialCredentialSource> sources,
  }) : _sources = sources;

  final Map<SocialProvider, SocialCredentialSource> _sources;

  /// Fournisseurs utilisables : ceux pour lesquels une source est configurée.
  List<SocialProvider> get availableProviders => _sources.keys.toList();

  bool isAvailable(SocialProvider provider) => _sources.containsKey(provider);

  /// Fournisseurs dont le serveur annonce le service actif.
  ///
  /// L'état vient du backend, pas du build : un client-id peut embarquer dans
  /// l'application alors que le serveur ne l'a pas configuré, et un bouton qui
  /// échoue en 503 est pire qu'un bouton absent.
  List<SocialProvider> enabledProviders(List<dynamic> serverProviders) {
    return serverProviders
        .map((entry) => _providerFromWireName((entry as Map)['provider']))
        .whereType<SocialProvider>()
        .where(isAvailable)
        .toList();
  }

  SocialProvider? _providerFromWireName(Object? wireName) {
    if (wireName is! String) return null;
    for (final SocialProvider provider in SocialProvider.values) {
      if (provider.wireName == wireName) return provider;
    }
    return null;
  }

  /// Demande un credential au fournisseur choisi.
  Future<SocialCredential> signIn(SocialProvider provider) async {
    final SocialCredentialSource? source = _sources[provider];
    if (source == null) {
      throw SocialAuthException(
        'SOCIAL_PROVIDER_NOT_CONFIGURED',
        'Ce mode de connexion n’est pas disponible.',
      );
    }
    return source.signIn();
  }
}
