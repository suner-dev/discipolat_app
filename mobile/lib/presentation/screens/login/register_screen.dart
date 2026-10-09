import 'package:dio/dio.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:go_router/go_router.dart';
import '../../../app.dart';
import '../../../data/services/api_service.dart';
import '../../../tenant_config.dart';
import '../../widgets/glass_theme.dart';
import '../../widgets/secure_screen.dart';
// LOT 1 §GLISE-D'ABORD (T1.6) — le picker « église d'abord » s'AJOUTE en tete
// du formulaire « classique » (ni createChurch, ni joinCode). Les trois gestes
// existants restent intacts (A1), le picker est un raccourci facultatif (D5).
import 'church_picker.dart';

/// Création de compte — trois gestes d'entrée (SPEC_ONBOARDING_FLOWS MO-1) :
///  • [createChurch] = « Créer mon église » (fondateur self-service, D1) ;
///  • [joinCode] non vide = « Rejoindre avec un code » (depuis /join) ;
///  • sinon = demande d'inscription classique approuvée par un Super Admin.
class RegisterScreen extends StatefulWidget {
  const RegisterScreen({
    super.key,
    this.createChurch = false,
    this.joinCode,
    this.joinChurchName,
  });

  final bool createChurch;
  final String? joinCode;
  final String? joinChurchName;

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
  final _churchNameController = TextEditingController();

  /// SPEC_ORGANISATION_DENOMINATION_V2 (T-M3, D2) — nature de l'organisation
  /// créée. `CHURCH` par défaut : on ne change pas le comportement historique
  /// de « Créer une église », on AJOUTE la capacité de créer une dénomination,
  /// une association, une organisation ou une méga-association.
  static const List<({String value, String label})> _orgKinds = [
    (value: 'CHURCH', label: 'Église'),
    (value: 'DENOMINATION', label: 'Dénomination (réseau d\u2019églises)'),
    (value: 'ASSOCIATION', label: 'Association'),
    (value: 'ORGANIZATION', label: 'Organisation'),
    (value: 'MEGA_ASSOCIATION', label: 'Méga-association'),
  ];
  String _orgKind = 'CHURCH';
  final _apiService = ApiService();
  bool _isLoading = false;
  bool _obscurePassword = true;
  bool _success = false;
  String? _error;
  // Fondateur : session immédiatement établie (D1) → on affiche le code de l'église.
  Map<String, dynamic>? _churchResult;
  // RGPD art. 7/9 : consentements explicites, non pré-cochés, obligatoires.
  bool _consentCgu = false;
  bool _consentPrivacy = false;
  bool _consentArt9 = false;

  // LOT 1 §GLISE-D'ABORD (T1.6) — l'église choisie via `ChurchPicker`.
  // Volontairement LOCAL et facultatif : la sélection n'est JAMAIS envoyée
  // comme preuve d'adhésion (D1/R1 : seule `joinCode` ou une invitation font
  // foi). Elle sert d'indice visuel à l'utilisateur et, le jour où un
  // back-office en a besoin, d'un futur champ `requestedChurchHint` côté
  // serveur (décision à journaliser en §8 avant d'ajouter un champ au
  // payload /auth/register).
  String? _pickedChurchName;
  String? _pickedChurchSlug;

  @override
  void dispose() {
    _firstNameController.dispose();
    _lastNameController.dispose();
    _emailController.dispose();
    _phoneController.dispose();
    _passwordController.dispose();
    _confirmController.dispose();
    _churchNameController.dispose();
    super.dispose();
  }

  Future<void> _register() async {
    if (!_formKey.currentState!.validate()) return;
    if (!_consentCgu || !_consentPrivacy || !_consentArt9) {
      setState(() => _error = 'Vous devez accepter les CGU, la politique de confidentialité et consentir au traitement des données religieuses (RGPD art. 9).');
      return;
    }
    setState(() { _isLoading = true; _error = null; });

    try {
      // §G3.1 — inscription « au nom d'une église » : si une organisation est
      // sélectionnée (uuid tenant), le compte naît rattaché à son répertoire.
      final orgId = TenantConfig.currentOrgId;
      final tenantUuid = orgId != null &&
              RegExp(r'^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$')
                  .hasMatch(orgId)
          ? orgId
          : null;
      final response = await _apiService.post('/auth/register', data: {
        'email': _emailController.text.trim(),
        'password': _passwordController.text,
        'firstName': _firstNameController.text.trim(),
        'lastName': _lastNameController.text.trim(),
        'phone': _phoneController.text.trim().isEmpty ? null : _phoneController.text.trim(),
        'consentCgu': _consentCgu,
        'consentPrivacy': _consentPrivacy,
        'consentArt9': _consentArt9,
        if (tenantUuid != null && !widget.createChurch && widget.joinCode == null) 'tenantId': tenantUuid,
        // SPEC_ONBOARDING_FLOWS (MO-1) — deux gestes self-service.
        if (widget.createChurch) 'createChurch': true,
        if (widget.createChurch) 'churchName': _churchNameController.text.trim(),
        // SPEC_ORGANISATION_DENOMINATION_V2 (T-M3, D2) — la NATURE de
        // l'organisation. Un tenant n'est plus « une église » : c'est une
        // dénomination, une église, une asso, une orga ou une méga-asso.
        // Défaut CHURCH = comportement historique.
        if (widget.createChurch) 'kind': _orgKind,
        if (widget.joinCode != null && widget.joinCode!.isNotEmpty) 'joinCode': widget.joinCode!.trim(),
      });

      final data = response.data as Map<String, dynamic>? ?? const <String, dynamic>{};
      final session = data['session'] as Map<String, dynamic>?;
      if (session != null && session['accessToken'] != null) {
        // Fondateur : session établie immédiatement — auto-login puis onboarding.
        //
        // T-B0bis (mobile) : on ADOPTE la paire de jetons renvoyée par le
        // backend. Sans cela, l'identité affichée pouvait annoncer une
        // organisation alors que le claim `tenantId` du access token courant
        // désignait encore l'ancienne — toutes les requêtes suivantes
        // partiraient ailleurs. `saveTokens` est le point d'écriture unique du
        // stockage mobile.
        await _apiService.saveTokens(session);
        if (!mounted) return;
        AuthState().setAuthenticated(true, userData: session);
        final church = data['church'] as Map<String, dynamic>?;
        setState(() => _churchResult = {
          'name': (church?['name'] ?? _churchNameController.text.trim()),
          'slug': (church?['slug'] ?? ''),
          'joinCode': (church?['joinCode'] ?? ''),
        });
        return;
      }
      // Rejointure sur validation (APPROVAL) ou inscription classique.
      if (mounted) setState(() => _success = true);
    } on DioException catch (e) {
      final message = e.response?.data?['detail'] as String?
          ?? e.response?.data?['error'] as String?
          ?? e.response?.data?['message'] as String?
           ?? 'Échec de la demande. Vérifiez vos informations.';
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
                      widget.createChurch
                          ? 'Créer mon église'
                          : (widget.joinCode != null && widget.joinCode!.isNotEmpty)
                              ? 'Rejoindre ${widget.joinChurchName ?? "mon église"}'
                              : 'Demander la création d\'une église',
                      style: Theme.of(context).textTheme.headlineSmall?.copyWith(
                        fontWeight: FontWeight.bold,
                        color: Colors.white,
                      ),
                    ),
                    const SizedBox(height: 8),
                    Text(
                      widget.createChurch
                          ? 'Votre église est créée instantanément : vous en devenez le fondateur et recevez un code d\'entrée pour vos membres.'
                          : (widget.joinCode != null && widget.joinCode!.isNotEmpty)
                              ? 'Créez votre compte : vous serez rattaché à cette église et n\'aurez plus jamais à saisir le code.'
                              : 'Soumettez une demande. Un Super Admin approuvera l\'organisation avant la création du compte.',
                      textAlign: TextAlign.center,
                      style: TextStyle(color: Colors.white.withValues(alpha: 0.5), fontSize: 13),
                    ),
                    const SizedBox(height: 32),

                    if (_churchResult != null) ...[
                      const Icon(Icons.check_circle_rounded, color: Colors.green, size: 56),
                      const SizedBox(height: 12),
                      Text(
                        'Votre église est prête !',
                        textAlign: TextAlign.center,
                        style: TextStyle(color: Colors.white.withValues(alpha: 0.9), fontSize: 18, fontWeight: FontWeight.bold),
                      ),
                      const SizedBox(height: 6),
                      Text(
                        _churchResult!['name']?.toString() ?? '',
                        textAlign: TextAlign.center,
                        style: TextStyle(color: Colors.white.withValues(alpha: 0.6), fontSize: 14),
                      ),
                      if ((_churchResult!['joinCode'] ?? '').toString().isNotEmpty) ...[
                        const SizedBox(height: 18),
                        GlassCard(
                          padding: const EdgeInsets.all(16),
                          child: Column(
                            children: [
                              Text("Code d'entrée de l'église",
                                  style: TextStyle(color: Colors.white.withValues(alpha: 0.5), fontSize: 11)),
                              const SizedBox(height: 6),
                              Text(
                                _churchResult!['joinCode'].toString(),
                                style: const TextStyle(color: Colors.white, fontSize: 24, fontWeight: FontWeight.bold, letterSpacing: 2, fontFamily: 'monospace'),
                              ),
                              const SizedBox(height: 8),
                              Text('Partagez-le avec vos futurs membres — ils ne le saisiront qu\'une seule fois.',
                                  textAlign: TextAlign.center,
                                  style: TextStyle(color: Colors.white.withValues(alpha: 0.5), fontSize: 12)),
                              // SPEC_ORGANISATION_DENOMINATION_V2 §4.1 / F21 — le
                              // LIEN est la porte la plus fluide (clic, zéro
                              // saisie) ; le code reste affiché pour ceux qui
                              // l'ont en dictée. Les deux sont copiables.
                              if ((_churchResult!['slug'] ?? '').toString().isNotEmpty) ...[
                                const SizedBox(height: 16),
                                Text('Lien d\'invitation',
                                    style: TextStyle(color: Colors.white.withValues(alpha: 0.5), fontSize: 11)),
                                const SizedBox(height: 6),
                                Text(
                                  '${Uri.base.origin}/j/${_churchResult!['slug']}',
                                  textAlign: TextAlign.center,
                                  style: const TextStyle(
                                    color: Color(0xFF4ADE80), fontSize: 13,
                                    fontFamily: 'monospace',
                                  ),
                                ),
                                const SizedBox(height: 10),
                                SizedBox(
                                  width: double.infinity,
                                  child: OutlinedButton.icon(
                                    onPressed: () async {
                                      final link =
                                          '${Uri.base.origin}/j/${_churchResult!['slug']}';
                                      await Clipboard.setData(ClipboardData(text: link));
                                      if (!mounted) return;
                                      ScaffoldMessenger.of(context).showSnackBar(
                                        const SnackBar(content: Text('Lien copié')),
                                      );
                                    },
                                    icon: const Icon(Icons.copy_rounded, size: 16),
                                    label: const Text('Copier le lien'),
                                  ),
                                ),
                              ],
                              const SizedBox(height: 8),
                              SizedBox(
                                width: double.infinity,
                                child: OutlinedButton.icon(
                                  onPressed: () async {
                                    await Clipboard.setData(ClipboardData(
                                        text: _churchResult!['joinCode'].toString()));
                                    if (!mounted) return;
                                    ScaffoldMessenger.of(context).showSnackBar(
                                      const SnackBar(content: Text('Code copié')),
                                    );
                                  },
                                  icon: const Icon(Icons.copy_rounded, size: 16),
                                  label: const Text('Copier le code'),
                                ),
                              ),
                            ],
                          ),
                        ),
                      ],
                      const SizedBox(height: 24),
                      SizedBox(
                        width: double.infinity,
                        height: 50,
                        child: FilledButton.icon(
                          onPressed: () => context.go(roleHome(AuthState().activeRole,
                              isPlatformSuperAdmin: AuthState().isPlatformSuperAdmin)),
                          icon: const Icon(Icons.arrow_forward, size: 18),
                          label: const Text('Accéder à mon église', style: TextStyle(fontSize: 15)),
                        ),
                      ),
                    ] else if (_success) ...[
                      const Icon(Icons.check_circle_rounded, color: Colors.green, size: 56),
                      const SizedBox(height: 12),
                      Text(
                         (widget.joinCode != null && widget.joinCode!.isNotEmpty)
                            ? 'Demande de rejointure transmise'
                            : 'Demande reçue. Elle sera examinée par un Super Admin.',
                        textAlign: TextAlign.center,
                        style: TextStyle(color: Colors.white.withValues(alpha: 0.8), fontSize: 14),
                      ),
                      const SizedBox(height: 8),
                      Text(
                         (widget.joinCode != null && widget.joinCode!.isNotEmpty)
                            ? 'Un responsable de l\'église validera votre entrée puis vous contactera.'
                            : 'Aucun compte ne sera créé avant approbation.',
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

                      // LOT 1 §GLISE-D'ABORD (T1.6) — le picker s'AJOUTE
                      // UNIQUEMENT sur le parcours « classique » (aucun
                      // contexte d'église dans l'URL). Les deux autres
                      // gestes (`?mode=church` = createChurch, `?joinCode=`)
                      // ont DÉJÀ leur église cible : le picker serait un
                      // pas de côté inutile et trompeur (A1/A3, R7 : ne
                      // casser aucun lien déjà distribué). Le widget lui-
                      // même n'écrit RIEN dans le payload d'inscription :
                      // D1/R1 interdisent au slug public de tenir lieu de
                      // preuve d'adhésion.
                      if (!widget.createChurch &&
                          (widget.joinCode == null ||
                              widget.joinCode!.isEmpty)) ...[
                        ChurchPicker(
                          apiService: _apiService,
                          onSelect: (name, slug) => setState(() {
                            _pickedChurchName = name;
                            _pickedChurchSlug = slug;
                          }),
                          onNotFound: (_) {
                            // Le CTA « non trouvée » reste dans le picker
                            // lui-même ; ici on ne réagit pas pour ne pas
                            // masquer le parcours classique.
                          },
                        ),
                        if (_pickedChurchName != null) ...[
                          const SizedBox(height: 8),
                          Semantics(
                            identifier: 'register.pickedChurch',
                            child: Text(
                              'Vous avez ciblé : $_pickedChurchName'
                              '${_pickedChurchSlug != null ? ' ($_pickedChurchSlug)' : ''}',
                              style: TextStyle(
                                color: Colors.white.withValues(alpha: 0.6),
                                fontSize: 12,
                              ),
                            ),
                          ),
                        ],
                        const SizedBox(height: 16),
                      ],

                      Form(
                        key: _formKey,
                        child: Column(
                          children: [
                            if (widget.createChurch) ...[
                              TextFormField(
                                controller: _churchNameController,
                                decoration: InputDecoration(
                                  labelText: _orgKind == 'CHURCH'
                                      ? 'Nom de l\'église'
                                      : 'Nom de l\'organisation',
                                  prefixIcon: const Icon(Icons.church_outlined),
                                ),
                                style: const TextStyle(color: Colors.white),
                                validator: (v) => v == null || v.trim().isEmpty
                                    ? 'Le nom est requis'
                                    : null,
                              ),
                              const SizedBox(height: 16),
                              // SPEC_ORGANISATION_DENOMINATION_V2 (T-M3, D2) —
                              // la nature de l'organisation. Une dénomination
                              // pourra ensuite accueillir ses églises enfants.
                              DropdownButtonFormField<String>(
                                initialValue: _orgKind,
                                dropdownColor: const Color(0xFF1F2937),
                                style: const TextStyle(color: Colors.white),
                                decoration: const InputDecoration(
                                  labelText: 'Type d\'organisation',
                                  prefixIcon: Icon(Icons.account_tree_outlined),
                                ),
                                items: _orgKinds
                                    .map((k) => DropdownMenuItem<String>(
                                          value: k.value,
                                          child: Text(k.label,
                                              style:
                                                  const TextStyle(color: Colors.white)),
                                        ))
                                    .toList(),
                                onChanged: (v) => setState(() => _orgKind = v ?? 'CHURCH'),
                              ),
                              if (_orgKind != 'CHURCH') ...[
                                const SizedBox(height: 8),
                                Text(
                                  'Vous pourrez créer vos églises enfants depuis '
                                  'votre espace organisation.',
                                  style: TextStyle(
                                      color: Colors.white.withValues(alpha: 0.6),
                                      fontSize: 12),
                                ),
                              ],
                              const SizedBox(height: 16),
                            ],
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
                            const SizedBox(height: 24),
                            // Consentements RGPD explicites (art. 7 & 9)
                            _ConsentCheckbox(
                              value: _consentCgu,
                              label: 'J\'accepte les conditions générales d\'utilisation',
                              onChanged: (v) => setState(() => _consentCgu = v),
                            ),
                            _ConsentCheckbox(
                              value: _consentPrivacy,
                              label: 'J\'accepte la politique de confidentialité',
                              onChanged: (v) => setState(() => _consentPrivacy = v),
                            ),
                            _ConsentCheckbox(
                              value: _consentArt9,
                              label: 'Je consens au traitement de mes données religieuses (RGPD art. 9)',
                              onChanged: (v) => setState(() => _consentArt9 = v),
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

/// Case de consentement RGPD — libellé explicite, jamais pré-cochée.
class _ConsentCheckbox extends StatelessWidget {
  const _ConsentCheckbox({
    required this.value,
    required this.label,
    required this.onChanged,
  });

  final bool value;
  final String label;
  final ValueChanged<bool> onChanged;

  @override
  Widget build(BuildContext context) => CheckboxListTile(
        value: value,
        onChanged: (v) => onChanged(v ?? false),
        controlAffinity: ListTileControlAffinity.leading,
        contentPadding: EdgeInsets.zero,
        dense: true,
        checkboxShape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(4)),
        title: Text(
          label,
          style: TextStyle(
            color: Colors.white.withValues(alpha: 0.7),
            fontSize: 12.5,
          ),
        ),
      );
}