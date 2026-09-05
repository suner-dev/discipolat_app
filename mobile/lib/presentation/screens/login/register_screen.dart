import 'package:dio/dio.dart';
import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import '../../../data/services/api_service.dart';
import '../../widgets/glass_theme.dart';
import '../../widgets/secure_screen.dart';

/// Création de compte membre — tout compte démarre avec le rôle MEMBRE.
/// Un administrateur ou pasteur attribue ensuite les autres rôles.
class RegisterScreen extends StatefulWidget {
  const RegisterScreen({super.key});

  @override
  State<RegisterScreen> createState() => _RegisterScreenState();
}

class _RegisterScreenState extends State<RegisterScreen> {
  final _formKey = GlobalKey<FormState>();
  final _firstNameController = TextEditingController();
  final _lastNameController = TextEditingController();
  final _emailController = TextEditingController();
  final _phoneController = TextEditingController();
  final _passwordController = TextEditingController();
  final _confirmController = TextEditingController();
  final _apiService = ApiService();
  bool _isLoading = false;
  bool _obscurePassword = true;
  bool _success = false;
  String? _error;

  @override
  void dispose() {
    _firstNameController.dispose();
    _lastNameController.dispose();
    _emailController.dispose();
    _phoneController.dispose();
    _passwordController.dispose();
    _confirmController.dispose();
    super.dispose();
  }

  Future<void> _register() async {
    if (!_formKey.currentState!.validate()) return;
    setState(() { _isLoading = true; _error = null; });

    try {
      await _apiService.post('/auth/register', data: {
        'email': _emailController.text.trim(),
        'password': _passwordController.text,
        'firstName': _firstNameController.text.trim(),
        'lastName': _lastNameController.text.trim(),
        'phone': _phoneController.text.trim().isEmpty ? null : _phoneController.text.trim(),
      });
      if (mounted) setState(() => _success = true);
    } on DioException catch (e) {
      final message = e.response?.data?['detail'] as String?
          ?? e.response?.data?['error'] as String?
          ?? e.response?.data?['message'] as String?
          ?? 'Échec de la création du compte. Vérifiez vos informations.';
      if (mounted) setState(() => _error = message);
    } catch (_) {
      if (mounted) setState(() => _error = 'Une erreur est survenue');
    } finally {
      if (mounted) setState(() => _isLoading = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    return SecureScreen(
      screenName: 'RegisterScreen',
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
          child: Center(
            child: SingleChildScrollView(
              padding: const EdgeInsets.all(24),
              child: ConstrainedBox(
                constraints: const BoxConstraints(maxWidth: 480),
                child: Column(
                  mainAxisAlignment: MainAxisAlignment.center,
                  children: [
                    // Logo
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
                      child: const Icon(Icons.person_add_alt_1_rounded, color: Colors.white, size: 36),
                    ),
                    const SizedBox(height: 20),
                    Text(
                      'Créer votre compte membre',
                      style: Theme.of(context).textTheme.headlineSmall?.copyWith(
                        fontWeight: FontWeight.bold,
                        color: Colors.white,
                      ),
                    ),
                    const SizedBox(height: 8),
                    Text(
                      'Inscrivez-vous en tant que membre — un administrateur ou un pasteur pourra ensuite vous attribuer d\'autres rôles.',
                      textAlign: TextAlign.center,
                      style: TextStyle(color: Colors.white.withValues(alpha: 0.5), fontSize: 13),
                    ),
                    const SizedBox(height: 32),

                    if (_success) ...[
                      const Icon(Icons.check_circle_rounded, color: Colors.green, size: 56),
                      const SizedBox(height: 12),
                      Text(
                        'Compte créé ! Vérifiez votre email pour activer votre compte.',
                        textAlign: TextAlign.center,
                        style: TextStyle(color: Colors.white.withValues(alpha: 0.8), fontSize: 14),
                      ),
                      const SizedBox(height: 8),
                      Text(
                        'Votre compte démarre avec le rôle Membre',
                        style: TextStyle(color: Colors.white.withValues(alpha: 0.4), fontSize: 12),
                      ),
                      const SizedBox(height: 24),
                      SizedBox(
                        width: double.infinity,
                        height: 50,
                        child: FilledButton.icon(
                          onPressed: () => context.go('/login'),
                          icon: const Icon(Icons.login, size: 18),
                          label: const Text('Se connecter', style: TextStyle(fontSize: 15)),
                        ),
                      ),
                    ] else ...[
                      // Error
                      if (_error != null)
                        GlassCard(
                          padding: const EdgeInsets.all(12),
                          margin: const EdgeInsets.only(bottom: 16),
                          borderColor: Colors.red.withValues(alpha: 0.3),
                          child: Row(
                            children: [
                              const Icon(Icons.error_outline, color: Colors.red, size: 20),
                              const SizedBox(width: 8),
                              Expanded(child: Text(_error!, style: const TextStyle(color: Colors.red, fontSize: 13))),
                            ],
                          ),
                        ),

                      Form(
                        key: _formKey,
                        child: Column(
                          children: [
                            // Prénom / Nom
                            Row(
                              children: [
                                Expanded(
                                  child: TextFormField(
                                    controller: _firstNameController,
                                    decoration: const InputDecoration(
                                      labelText: 'Prénom',
                                      prefixIcon: Icon(Icons.person_outline),
                                    ),
                                    style: const TextStyle(color: Colors.white),
                                    validator: (v) => v == null || v.trim().isEmpty ? 'Prénom requis' : null,
                                  ),
                                ),
                                const SizedBox(width: 12),
                                Expanded(
                                  child: TextFormField(
                                    controller: _lastNameController,
                                    decoration: const InputDecoration(
                                      labelText: 'Nom',
                                      prefixIcon: Icon(Icons.badge_outlined),
                                    ),
                                    style: const TextStyle(color: Colors.white),
                                    validator: (v) => v == null || v.trim().isEmpty ? 'Nom requis' : null,
                                  ),
                                ),
                              ],
                            ),
                            const SizedBox(height: 16),
                            TextFormField(
                              controller: _emailController,
                              decoration: const InputDecoration(
                                labelText: 'Adresse email',
                                prefixIcon: Icon(Icons.email_outlined),
                              ),
                              keyboardType: TextInputType.emailAddress,
                              style: const TextStyle(color: Colors.white),
                              validator: (v) => v == null || v.isEmpty ? 'Email requis'
                                  : v.contains('@') ? null : 'Email invalide',
                            ),
                            const SizedBox(height: 16),
                            TextFormField(
                              controller: _phoneController,
                              decoration: const InputDecoration(
                                labelText: 'Téléphone (optionnel)',
                                prefixIcon: Icon(Icons.phone_outlined),
                              ),
                              keyboardType: TextInputType.phone,
                              style: const TextStyle(color: Colors.white),
                            ),
                            const SizedBox(height: 16),
                            TextFormField(
                              controller: _passwordController,
                              decoration: InputDecoration(
                                labelText: 'Mot de passe',
                                prefixIcon: const Icon(Icons.lock_outlined),
                                suffixIcon: IconButton(
                                  icon: Icon(_obscurePassword ? Icons.visibility_off : Icons.visibility, color: Colors.white38),
                                  onPressed: () => setState(() => _obscurePassword = !_obscurePassword),
                                ),
                              ),
                              obscureText: _obscurePassword,
                              style: const TextStyle(color: Colors.white),
                              validator: (v) => v == null || v.length < 8 ? 'Au moins 8 caractères' : null,
                            ),
                            const SizedBox(height: 16),
                            TextFormField(
                              controller: _confirmController,
                              decoration: const InputDecoration(
                                labelText: 'Confirmer le mot de passe',
                                prefixIcon: Icon(Icons.lock_outline),
                              ),
                              obscureText: true,
                              style: const TextStyle(color: Colors.white),
                              validator: (v) => v != _passwordController.text ? 'Les mots de passe ne correspondent pas' : null,
                            ),
                          ],
                        ),
                      ),
                      const SizedBox(height: 24),
                      SizedBox(
                        width: double.infinity,
                        height: 50,
                        child: FilledButton(
                          onPressed: _isLoading ? null : _register,
                          child: _isLoading
                              ? const SizedBox(width: 22, height: 22, child: CircularProgressIndicator(strokeWidth: 2.5, color: Colors.white))
                              : const Row(
                                  mainAxisAlignment: MainAxisAlignment.center,
                                  children: [
                                    Icon(Icons.person_add_alt_1_rounded, size: 18),
                                    SizedBox(width: 8),
                                    Text('Créer mon compte', style: TextStyle(fontSize: 15)),
                                  ],
                                ),
                        ),
                      ),
                      const SizedBox(height: 12),
                      TextButton(
                        onPressed: () => context.go('/login'),
                        child: Text(
                          'Déjà un compte ? Se connecter',
                          style: TextStyle(color: Colors.white.withValues(alpha: 0.5)),
                        ),
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
}