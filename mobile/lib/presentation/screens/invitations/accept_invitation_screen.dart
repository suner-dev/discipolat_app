import 'dart:convert';

import 'package:dio/dio.dart';
import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';

import '../../../core/invitation_token.dart';
import '../../../data/services/api_service.dart';
import '../../widgets/glass_theme.dart';
import '../../widgets/secure_screen.dart';

class AcceptInvitationScreen extends StatefulWidget {
  const AcceptInvitationScreen({
    super.key,
    this.initialToken,
    this.apiService,
    this.onCompleted,
  });

  final String? initialToken;
  final ApiService? apiService;
  final VoidCallback? onCompleted;

  @override
  State<AcceptInvitationScreen> createState() => _AcceptInvitationScreenState();
}

class _AcceptInvitationScreenState extends State<AcceptInvitationScreen> {
  final _formKey = GlobalKey<FormState>();
  final _tokenController = TextEditingController();
  final _firstNameController = TextEditingController();
  final _lastNameController = TextEditingController();
  final _passwordController = TextEditingController();
  final _confirmPasswordController = TextEditingController();
  late final ApiService _api = widget.apiService ?? ApiService();
  Map<String, dynamic>? _invitation;
  String? _activeToken;
  String? _error;
  bool _loading = false;
  bool _accepting = false;
  bool _obscurePassword = true;

  bool get _accountExists => _invitation?['accountExists'] == true;

  @override
  void initState() {
    super.initState();
    _activeToken = normalizeInvitationToken(widget.initialToken);
    if (_activeToken != null) _validateInvitation(_activeToken);
  }

  @override
  void dispose() {
    _tokenController.clear();
    _firstNameController.clear();
    _lastNameController.clear();
    _passwordController.clear();
    _confirmPasswordController.clear();
    _tokenController.dispose();
    _firstNameController.dispose();
    _lastNameController.dispose();
    _passwordController.dispose();
    _confirmPasswordController.dispose();
    super.dispose();
  }

  Future<void> _validateInvitation([String? candidate]) async {
    if (_loading || _accepting) return;
    final token = normalizeInvitationToken(
      candidate ?? _activeToken ?? _tokenController.text,
    );
    if (token == null) {
      setState(() => _error = 'Code d’invitation invalide.');
      return;
    }
    setState(() {
      _activeToken = token;
      _loading = true;
      _error = null;
    });
    try {
      final response = await _api.get(
        '/admin/invitations/validate/${Uri.encodeComponent(token)}',
      );
      final data = Map<String, dynamic>.from(response.data as Map);
      if (data['valid'] != true || data['email'] is! String) {
        throw const FormatException();
      }
      if (!mounted) return;
      setState(() {
        _invitation = data;
        _loading = false;
      });
    } catch (error) {
      if (!mounted) return;
      setState(() {
        _loading = false;
        _invitation = null;
        _error = _messageForError(error);
      });
    }
  }

  Future<void> _accept() async {
    final token = _activeToken;
    if (token == null || _accepting) return;
    if (!_accountExists && _formKey.currentState?.validate() != true) return;
    setState(() {
      _accepting = true;
      _error = null;
    });
    try {
      await _api.post(
        '/admin/invitations/accept/${Uri.encodeComponent(token)}',
        data: _accountExists
            ? <String, dynamic>{}
            : {
                'firstName': _firstNameController.text.trim(),
                'lastName': _lastNameController.text.trim(),
                'password': _passwordController.text,
              },
      );
      if (mounted) setState(() => _accepting = false);
      _complete();
    } on DioException catch (error) {
      if (_isAlreadyAccepted(error)) {
        if (mounted) setState(() => _accepting = false);
        _complete();
        return;
      }
      if (!mounted) return;
      setState(() {
        _accepting = false;
        _error = _messageForError(error);
      });
    } catch (_) {
      if (!mounted) return;
      setState(() {
        _accepting = false;
        _error = 'Impossible d’accepter l’invitation.';
      });
    }
  }

  bool _isAlreadyAccepted(DioException error) {
    if (error.response?.statusCode != 410) return false;
    final data = error.response?.data;
    if (data is! Map) return false;
    final details = data['details'];
    return details is Map && details['status'] == 'ACCEPTED';
  }

  void _complete() {
    _tokenController.clear();
    _activeToken = null;
    if (widget.onCompleted != null) {
      widget.onCompleted!();
    } else if (mounted) {
      context.go('/login');
    }
  }

  String _messageForError(Object error) {
    if (error is DioException) {
      final status = error.response?.statusCode;
      if (status == 404 || status == 410) {
        return 'Cette invitation est invalide, expirée ou déjà utilisée.';
      }
      if (status == 409) {
        return 'Ce compte est déjà rattaché à une autre organisation.';
      }
      if (status == 429) {
        return 'Trop de tentatives. Réessayez dans quelques minutes.';
      }
      final data = error.response?.data;
      if (data is Map) {
        final detail = data['detail'] ?? data['error'];
        if (detail is String && detail.trim().isNotEmpty) return detail;
      }
      if (error.type == DioExceptionType.connectionError ||
          error.type == DioExceptionType.connectionTimeout ||
          error.type == DioExceptionType.receiveTimeout ||
          error.type == DioExceptionType.sendTimeout) {
        return 'Impossible de joindre le serveur. Vérifiez votre connexion.';
      }
    }
    return 'Invitation invalide ou indisponible.';
  }

  String _formatExpiration(String? value) {
    final parsed = DateTime.tryParse(value ?? '');
    if (parsed == null) return 'Date inconnue';
    const months = [
      'janvier',
      'février',
      'mars',
      'avril',
      'mai',
      'juin',
      'juillet',
      'août',
      'septembre',
      'octobre',
      'novembre',
      'décembre',
    ];
    final local = parsed.toLocal();
    return '${local.day} ${months[local.month - 1]} ${local.year}';
  }

  String? _validatePassword(String? value) {
    final password = value ?? '';
    if (password.isEmpty) return 'Mot de passe requis';
    if (password.length < 8) return '8 caractères minimum';
    if (utf8.encode(password).length > 72) return 'Mot de passe trop long';
    return null;
  }

  @override
  Widget build(BuildContext context) {
    return SecureScreen(
      screenName: 'AcceptInvitationScreen',
      auditAction: 'acceptInvitationView',
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
            child: Center(
              child: SingleChildScrollView(
                padding: const EdgeInsets.all(24),
                child: ConstrainedBox(
                  constraints: const BoxConstraints(maxWidth: 520),
                  child: _buildContent(),
                ),
              ),
            ),
          ),
        ),
      ),
    );
  }

  Widget _buildContent() {
    if (_loading) {
      return const Card(
        child: Padding(
          padding: EdgeInsets.all(40),
          child: Center(child: CircularProgressIndicator()),
        ),
      );
    }
    final invitation = _invitation;
    if (invitation == null) return _buildTokenEntry();
    return _buildInvitationForm(invitation);
  }

  Widget _buildTokenEntry() {
    return GlassCard(
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          const Icon(Icons.mark_email_read_outlined,
              color: Colors.white, size: 48),
          const SizedBox(height: 16),
          const Text(
            'Accepter une invitation',
            textAlign: TextAlign.center,
            style: TextStyle(
              color: Colors.white,
              fontSize: 22,
              fontWeight: FontWeight.bold,
            ),
          ),
          const SizedBox(height: 8),
          const Text(
            'Saisissez le code reçu dans le lien d’invitation.',
            textAlign: TextAlign.center,
            style: TextStyle(color: Colors.white70),
          ),
          const SizedBox(height: 24),
          TextField(
            controller: _tokenController,
            obscureText: true,
            autocorrect: false,
            enableSuggestions: false,
            decoration: const InputDecoration(
              labelText: 'Code d’invitation',
              prefixIcon: Icon(Icons.key_outlined),
            ),
            onSubmitted: (_) => _validateInvitation(),
          ),
          if (_error != null) ...[
            const SizedBox(height: 12),
            Text(_error!, style: const TextStyle(color: Colors.redAccent)),
          ],
          const SizedBox(height: 20),
          FilledButton.icon(
            onPressed: _validateInvitation,
            icon: const Icon(Icons.verified_user_outlined),
            label: const Text('Vérifier l’invitation'),
          ),
          TextButton(
            onPressed: () => context.go('/login'),
            child: const Text('Retour à la connexion'),
          ),
        ],
      ),
    );
  }

  Widget _buildInvitationForm(Map<String, dynamic> invitation) {
    final organizationName = invitation['organizationName']?.toString();
    return GlassCard(
      child: Form(
        key: _formKey,
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            Icon(
              _accountExists
                  ? Icons.group_add_outlined
                  : Icons.person_add_alt_1_rounded,
              color: AppColors.primaryLight,
              size: 48,
            ),
            const SizedBox(height: 16),
            Text(
              _accountExists
                  ? 'Rejoindre ${invitation['tenantName']}'
                  : 'Invitation pour ${invitation['tenantName']}',
              textAlign: TextAlign.center,
              style: const TextStyle(
                color: Colors.white,
                fontSize: 22,
                fontWeight: FontWeight.bold,
              ),
            ),
            const SizedBox(height: 8),
            Text(
              _accountExists
                  ? 'Un compte existe déjà. Acceptez l’invitation, puis connectez-vous.'
                  : 'Rôle attribué : ${invitation['role']}',
              textAlign: TextAlign.center,
              style: const TextStyle(color: Colors.white70),
            ),
            if (organizationName != null && organizationName.isNotEmpty)
              Text(
                organizationName,
                textAlign: TextAlign.center,
                style: const TextStyle(color: Colors.white54),
              ),
            Text(
              'Expire le ${_formatExpiration(invitation['expiresAt']?.toString())}',
              textAlign: TextAlign.center,
              style: const TextStyle(color: Colors.white54, fontSize: 12),
            ),
            const SizedBox(height: 24),
            TextFormField(
              initialValue: invitation['email']?.toString(),
              readOnly: true,
              decoration: const InputDecoration(
                labelText: 'Email',
                prefixIcon: Icon(Icons.email_outlined),
              ),
            ),
            if (!_accountExists) ...[
              const SizedBox(height: 12),
              Row(
                children: [
                  Expanded(
                    child: TextFormField(
                      controller: _firstNameController,
                      textInputAction: TextInputAction.next,
                      decoration: const InputDecoration(labelText: 'Prénom'),
                      validator: (value) =>
                          value == null || value.trim().isEmpty
                              ? 'Prénom requis'
                              : value.trim().length > 100
                                  ? 'Prénom trop long'
                                  : null,
                    ),
                  ),
                  const SizedBox(width: 12),
                  Expanded(
                    child: TextFormField(
                      controller: _lastNameController,
                      textInputAction: TextInputAction.next,
                      decoration: const InputDecoration(labelText: 'Nom'),
                      validator: (value) =>
                          value == null || value.trim().isEmpty
                              ? 'Nom requis'
                              : value.trim().length > 100
                                  ? 'Nom trop long'
                                  : null,
                    ),
                  ),
                ],
              ),
              const SizedBox(height: 12),
              TextFormField(
                controller: _passwordController,
                obscureText: _obscurePassword,
                autofillHints: const [AutofillHints.newPassword],
                decoration: InputDecoration(
                  labelText: 'Mot de passe',
                  prefixIcon: const Icon(Icons.lock_outline),
                  suffixIcon: IconButton(
                    onPressed: () => setState(
                      () => _obscurePassword = !_obscurePassword,
                    ),
                    icon: Icon(
                      _obscurePassword
                          ? Icons.visibility_outlined
                          : Icons.visibility_off_outlined,
                    ),
                  ),
                ),
                validator: _validatePassword,
              ),
              const SizedBox(height: 12),
              TextFormField(
                controller: _confirmPasswordController,
                obscureText: _obscurePassword,
                autofillHints: const [AutofillHints.newPassword],
                decoration: const InputDecoration(
                  labelText: 'Confirmer le mot de passe',
                  prefixIcon: Icon(Icons.lock_reset_outlined),
                ),
                validator: (value) => value != _passwordController.text
                    ? 'Les mots de passe ne correspondent pas'
                    : null,
              ),
            ],
            if (_error != null) ...[
              const SizedBox(height: 12),
              Text(_error!, style: const TextStyle(color: Colors.redAccent)),
            ],
            const SizedBox(height: 20),
            FilledButton.icon(
              onPressed: _accepting ? null : _accept,
              icon: _accepting
                  ? const SizedBox(
                      width: 18,
                      height: 18,
                      child: CircularProgressIndicator(strokeWidth: 2),
                    )
                  : const Icon(Icons.check_circle_outline),
              label: Text(
                _accepting
                    ? 'Acceptation…'
                    : _accountExists
                        ? 'Accepter l’invitation'
                        : 'Créer mon compte',
              ),
            ),
            TextButton(
              onPressed: _accepting ? null : () => context.go('/login'),
              child: const Text('Retour à la connexion'),
            ),
          ],
        ),
      ),
    );
  }
}
