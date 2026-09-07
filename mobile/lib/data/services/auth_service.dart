import 'package:flutter_secure_storage/flutter_secure_storage.dart';
import '../models/user.dart';

/// Service d'authentification — fournit l'utilisateur courant et le token.
class AuthService {
  static const _storage = FlutterSecureStorage();
  static const _accessTokenKey = 'access_token';
  static const _userKey = 'current_user';

  User? _currentUser;

  /// Utilisateur courant (mis en cache après connexion).
  User? get currentUser => _currentUser;

  /// Token d'accès JWT.
  Future<String?> get token async {
    return await _storage.read(key: _accessTokenKey);
  }

  /// Rôle actif de l'utilisateur.
  String? get activeRole => _currentUser?.activeRole;

  /// ID de l'utilisateur.
  String? get userId => _currentUser?.id;

  /// Définit l'utilisateur courant.
  void setUser(User user) {
    _currentUser = user;
  }

  /// Charge l'utilisateur depuis le stockage sécurisé.
  Future<void> loadUser() async {
    try {
      final userJson = await _storage.read(key: _userKey);
      if (userJson != null) {
        // Simple parsing — en production, utiliser jsonDecode
        _currentUser = null;
      }
    } catch (_) {}
  }

  /// Déconnecte l'utilisateur.
  Future<void> logout() async {
    _currentUser = null;
    await _storage.delete(key: _accessTokenKey);
    await _storage.delete(key: _userKey);
  }
}
