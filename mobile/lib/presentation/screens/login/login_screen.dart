import 'package:flutter/material.dart';
import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import '../../../data/services/api_service.dart';
import '../../../data/services/providers.dart';
import '../../../app.dart';
import '../../widgets/beta_badge.dart';
import '../../widgets/glass_theme.dart';

class LoginScreen extends StatefulWidget {
  const LoginScreen({super.key});

  @override
  State<LoginScreen> createState() => _LoginScreenState();
}

class _LoginScreenState extends State<LoginScreen> with SingleTickerProviderStateMixin {
  final _formKey = GlobalKey<FormState>();
  final _emailController = TextEditingController();
  final _passwordController = TextEditingController();
  final _apiService = ApiService();
  bool _isLoading = false;
  bool _obscurePassword = true;
  String? _error;

  late final AnimationController _fadeController;
  late final Animation<double> _fadeAnimation;

  @override
  void initState() {
    super.initState();
    _fadeController = AnimationController(
      vsync: this,
      duration: const Duration(milliseconds: 800),
    );
    _fadeAnimation = CurvedAnimation(parent: _fadeController, curve: Curves.easeOut);
    _fadeController.forward();
  }

  @override
  void dispose() {
    _emailController.dispose();
    _passwordController.dispose();
    _fadeController.dispose();
    super.dispose();
  }

  Future<void> _forgotPassword() async {
    final controller = TextEditingController();
    final message = await showDialog<String>(
      context: context,
      builder: (ctx) => AlertDialog(
        backgroundColor: const Color(0xFF1E2A4A),
        title: const Text('Mot de passe oublié', style: TextStyle(color: Colors.white, fontSize: 18)),
        content: TextField(
          controller: controller,
          style: const TextStyle(color: Colors.white),
          decoration: InputDecoration(
            labelText: 'Votre email',
            labelStyle: TextStyle(color: Colors.white.withValues(alpha: 0.5)),
          ),
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(ctx, controller.text.trim()),
            child: const Text('Envoyer'),
          ),
        ],
      ),
    );
    if (message == null || message.isEmpty) return;
    try {
      await _apiService.post('/auth/forgot-password', data: {'email': message});
    } catch (_) {
      // L'API renvoie toujours le même message pour ne pas divulguer l'existence d'un compte.
    }
    if (mounted) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Si cet email existe, un lien de réinitialisation a été envoyé.')),
      );
    }
  }

  Future<void> _login() async {
    if (!_formKey.currentState!.validate()) return;
    setState(() { _isLoading = true; _error = null; });

    try {
      final response = await _apiService.post('/auth/login', data: {
        'email': _emailController.text.trim(),
        'password': _passwordController.text,
      });
      await _apiService.saveTokens(response.data);
      if (!mounted) return;
      // Notify the auth guard with full user data (multi-role)
      AuthState().setAuthenticated(true, userData: response.data as Map<String, dynamic>?);
      // Aller directement vers l'espace métier du rôle actif
      context.go(roleHome(AuthState().activeRole));
    } on DioException catch (e) {
      String message;
      switch (e.type) {
        case DioExceptionType.connectionError:
        case DioExceptionType.unknown:
        case DioExceptionType.badCertificate:
          message = 'Impossible de joindre le serveur. Vérifiez votre connexion internet et réessayez.';
          break;
        case DioExceptionType.connectionTimeout:
        case DioExceptionType.receiveTimeout:
        case DioExceptionType.sendTimeout:
          message = 'Le serveur met trop de temps à répondre. Veuillez réessayer.';
          break;
        default:
          message = e.response?.data?['detail'] as String?
              ?? e.response?.data?['error'] as String?
              ?? 'Email ou mot de passe incorrect';
      }
      setState(() => _error = message);
    } catch (e) {
      setState(() => _error = 'Une erreur est survenue');
    } finally {
      if (mounted) setState(() => _isLoading = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
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
            child: FadeTransition(
              opacity: _fadeAnimation,
              child: Column(
                mainAxisAlignment: MainAxisAlignment.center,
                children: [
                  // Logo with glow
                  Container(
                    width: 88,
                    height: 88,
                    decoration: BoxDecoration(
                      gradient: LinearGradient(
                        colors: [AppColors.primary, AppColors.primaryLight],
                        begin: Alignment.topLeft,
                        end: Alignment.bottomRight,
                      ),
                      borderRadius: BorderRadius.circular(24),
                      boxShadow: [BoxShadow(color: AppColors.primary.withValues(alpha: 0.4), blurRadius: 24, spreadRadius: 2)],
                    ),
                    child: const Icon(Icons.church_rounded, color: Colors.white, size: 44),
                  ),
                  const SizedBox(height: 24),
                  Row(
                    mainAxisAlignment: MainAxisAlignment.center,
                    children: [
                      Flexible(
                        child: Text(
                          'Discipolat',
                          style: Theme.of(context).textTheme.headlineMedium?.copyWith(
                            fontWeight: FontWeight.bold,
                            color: Colors.white,
                            letterSpacing: -0.5,
                          ),
                        ),
                      ),
                      const SizedBox(width: 8),
                      // Badge BÊTA — uniquement en environnement bêta (serveur-driven)
                      const BetaBadge(),
                    ],
                  ),
                  const SizedBox(height: 8),
                  Text(
                    'Application de Gestion du Discipolat',
                    style: TextStyle(color: Colors.white.withValues(alpha: 0.5), fontSize: 13),
                  ),
                  const SizedBox(height: 48),

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

                  // Email
                  TextFormField(
                    controller: _emailController,
                    decoration: const InputDecoration(
                      labelText: 'Adresse email',
                      prefixIcon: Icon(Icons.email_outlined),
                    ),
                    keyboardType: TextInputType.emailAddress,
                    style: const TextStyle(color: Colors.white),
                    validator: (v) => v == null || v.isEmpty ? 'Email requis' : v.contains('@') ? null : 'Email invalide',
                  ),
                  const SizedBox(height: 16),

                  // Password
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
                    validator: (v) => v == null || v.isEmpty ? 'Mot de passe requis' : null,
                  ),
                  const SizedBox(height: 24),

                  // Login button
                  SizedBox(
                    width: double.infinity,
                    height: 50,
                    child: FilledButton(
                      onPressed: _isLoading ? null : _login,
                      child: _isLoading
                          ? const SizedBox(width: 22, height: 22, child: CircularProgressIndicator(strokeWidth: 2.5, color: Colors.white))
                          : const Row(
                              mainAxisAlignment: MainAxisAlignment.center,
                              children: [
                                Icon(Icons.login, size: 18),
                                SizedBox(width: 8),
                                Text('Se connecter', style: TextStyle(fontSize: 16)),
                              ],
                            ),
                    ),
                  ),
                  const SizedBox(height: 16),

                  // Forgot password
                  TextButton(
                    onPressed: _forgotPassword,
                    child: Text('Mot de passe oublié ?', style: TextStyle(color: Colors.white.withValues(alpha: 0.5))),
                  ),

                  const SizedBox(height: 24),
                  GlassDivider(),
                  const SizedBox(height: 16),

                  // Comptes de démonstration — visibles UNIQUEMENT si le serveur les
                  // autorise (profil bêta) : jamais de données de test en production.
                  Consumer(builder: (context, ref, _) {
                    final meta = ref.watch(metaProvider).valueOrNull;
                    final showDemo = meta?.demoAccountsEnabled ?? false;
                    if (!showDemo) return const SizedBox.shrink();
                    return GlassCard(
                      padding: const EdgeInsets.all(12),
                      child: Column(
                        children: [
                          Row(
                            mainAxisAlignment: MainAxisAlignment.center,
                            children: [
                              Icon(Icons.science, color: AppColors.accent, size: 16),
                              const SizedBox(width: 6),
                              Text('Comptes de démonstration (bêta)', style: TextStyle(color: AppColors.accentLight, fontSize: 12, fontWeight: FontWeight.w600)),
                            ],
                          ),
                          const SizedBox(height: 8),
                          _demoAccount('Admin (multi-rôles)', 'admin@discipolat.com'),
                          _demoAccount('Pasteur', 'pasteur@discipolat.com'),
                          _demoAccount('Responsable (multi-rôles)', 'responsable@discipolat.com'),
                          _demoAccount('Chef de famille', 'chef@discipolat.com'),
                          _demoAccount('Faiseur', 'faiseur@discipolat.com'),
                          _demoAccount('Membre', 'membre@discipolat.com'),
                          _demoAccount('Multi-rôles', 'paul@discipolat.com'),
                          const SizedBox(height: 8),
                          Text('Mot de passe : password123',
                            style: TextStyle(color: Colors.white.withValues(alpha: 0.35), fontSize: 10),
                          ),
                        ],
                      ),
                    );
                  }),
                ],
              ),
            ),
          ),
        ),
      ),
    );
  }

  Widget _demoAccount(String role, String email) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 2),
      child: Row(
        mainAxisAlignment: MainAxisAlignment.spaceBetween,
        children: [
          Text(role, style: TextStyle(color: Colors.white.withValues(alpha: 0.6), fontSize: 11)),
          Text(email, style: TextStyle(color: Colors.white.withValues(alpha: 0.3), fontSize: 10, fontFamily: 'monospace')),
        ],
      ),
    );
  }
}
