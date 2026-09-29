// B7 — Écran mobile du wizard d'onboarding (contrat §3.1).
//
// §5.0.2 : les 5 états sont traités — chargement (indicateur + texte), vide, erreur
// (actionnable), succès (écran de fin), hors-ligne (erreur réseau distinguée).
// Aucune donnée en dur : tout provient de l'API.

import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../data/services/tenant_onboarding_service.dart';
import '../../../models/onboarding_step.dart';
import '../../widgets/glass_theme.dart';
import 'onboarding_step_forms.dart';

final tenantOnboardingServiceProvider =
    Provider<TenantOnboardingService>((ref) => TenantOnboardingService());

class TenantOnboardingScreen extends ConsumerStatefulWidget {
  const TenantOnboardingScreen({super.key});

  @override
  ConsumerState<TenantOnboardingScreen> createState() => _TenantOnboardingScreenState();
}

class _TenantOnboardingScreenState extends ConsumerState<TenantOnboardingScreen> {
  // §5.0.2 : chargement par liste vide impossible à confondre avec du contenu.
  bool _loading = true;
  bool _busy = false;
  String? _loadError;
  String? _actionError;
  OnboardingProgress? _progress;
  int _index = 0;
  String _skipReason = '';
  final TextEditingController _skipController = TextEditingController();

  @override
  void dispose() {
    _skipController.dispose();
    super.dispose();
  }

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) => _load());
  }

  TenantOnboardingService get _service => ref.read(tenantOnboardingServiceProvider);

  /// Libellés de l'écran. Le repo n'expose pas encore de pont i18n (.arb) dans
  /// cet écran (cf. users_screen.dart qui n'utilise pas AppLocalizations) : on ne
  /// s'invente donc PAS une API d'i18n. Les libellés sont centralisés ici (en
  /// français, en cohérence avec les écrans tenants existants) et branchés sur
  /// le pont i18n à la tâche B13. Une clé inconnue retombe sur sa valeur de
  /// secours pour ne jamais afficher une clé brute à l'utilisateur.
  static const Map<String, String> _labels = <String, String>{
    'onboarding.title': "Configuration de l'église",
    'onboarding.progressLabel': 'Progression',
    'onboarding.loadError': 'Chargement impossible',
    'onboarding.loadErrorHint': 'Vérifiez votre connexion puis réessayez.',
    'onboarding.retry': 'Réessayer',
    'onboarding.noSteps': 'Aucune étape de configuration',
    'onboarding.noStepsHint': 'Tout est déjà en place.',
    'onboarding.allDone': 'Configuration terminée',
    'onboarding.allDoneHint': 'Toutes les étapes sont complètes.',
    'onboarding.goToDashboard': 'Aller au tableau de bord',
    'onboarding.stepSaved': 'Étape enregistrée',
    'onboarding.stepSkipped': 'Étape ignorée',
    'onboarding.completed': 'Étape terminée',
    'onboarding.skipped': 'Étape ignorée',
    'onboarding.skip': 'Ignorer cette étape',
    'onboarding.skipReason': 'Motif (obligatoire)',
    'onboarding.skipReasonRequired':
        'Un motif est requis pour ignorer cette étape.',
    'onboarding.errOrder': 'Ordre des étapes invalide.',
    'onboarding.errAlreadyDone': 'Cette étape est déjà terminée.',
    'onboarding.errDataInvalid': 'Les données saisies sont invalides.',
    'onboarding.errNotSkippable': 'Cette étape ne peut pas être ignorée.',
    'onboarding.errSuspended':
        "Organisation suspendue : contact de l'administrateur requis.",
    'onboarding.errGeneric': 'Action impossible. Réessayez.',
  };

  String _msg(String key) => _labels[key] ?? key;

  Future<void> _load() async {
    setState(() {
      _loading = true;
      _loadError = null;
    });
    try {
      final progress = await _service.fetchProgress();
      if (!mounted) return;
      final firstOpen = progress.steps.indexWhere((s) => !s.isCompleted);
      setState(() {
        _progress = progress;
        _index = firstOpen >= 0 ? firstOpen : 0;
        _loading = false;
      });
    } on Object catch (e) {
      if (!mounted) return;
      setState(() {
        _loadError = e.toString();
        _loading = false;
      });
    }
  }

  Future<void> _refreshStatus() async {
    try {
      final p = await _service.fetchProgress();
      if (mounted) setState(() => _progress = p);
    } on Object {
      // Rafraîchissement non bloquant : on garde l'affichage courant.
    }
  }

  Future<void> _complete(OnboardingStep step, Map<String, dynamic> data) async {
    setState(() {
      _busy = true;
      _actionError = null;
    });
    try {
      await _service.completeStep(step.id, data);
      await _refreshStatus();
      if (!mounted) return;
      setState(() {
        _busy = false;
        _skipController.clear();
        _skipReason = '';
        final next = _progress?.steps.indexWhere(
            (s) => s.stepOrder > step.stepOrder && !s.isCompleted);
        if (next != null && next > 0) _index = next;
      });
      _toast(_msg('onboarding.stepSaved'));
    } on Object catch (e) {
      if (!mounted) return;
      setState(() {
        _busy = false;
        _actionError = _friendlyError(e);
      });
    }
  }

  Future<void> _skip(OnboardingStep step) async {
    if (step.skipRequiresReason && _skipReason.trim().isEmpty) {
      setState(() => _actionError = _msg('onboarding.skipReasonRequired'));
      return;
    }
    setState(() {
      _busy = true;
      _actionError = null;
    });
    try {
      await _service.skipStep(step.id, reason: _skipReason);
      await _refreshStatus();
      if (!mounted) return;
      setState(() {
        _busy = false;
        _skipController.clear();
        _skipReason = '';
        final next = _progress?.steps.indexWhere(
            (s) => s.stepOrder > step.stepOrder && !s.isCompleted);
        if (next != null && next > 0) _index = next;
      });
      _toast(_msg('onboarding.stepSkipped'));
    } on Object catch (e) {
      if (!mounted) return;
      setState(() {
        _busy = false;
        _actionError = _friendlyError(e);
      });
    }
  }

  /// Traduit les codes métier du §3.1 en messages actionnables.
  String _friendlyError(Object e) {
    final raw = e.toString();
    for (final entry in {
      'STEP_ORDER_VIOLATION': 'onboarding.errOrder',
      'STEP_ALREADY_COMPLETED': 'onboarding.errAlreadyDone',
      'STEP_DATA_INVALID': 'onboarding.errDataInvalid',
      'STEP_SKIP_REASON_REQUIRED': 'onboarding.skipReasonRequired',
      'STEP_NOT_SKIPPABLE': 'onboarding.errNotSkippable',
      'TENANT_SUSPENDED': 'onboarding.errSuspended',
    }.entries) {
      if (raw.contains(entry.key)) return _msg(entry.value);
    }
    return _msg('onboarding.errGeneric');
  }

  void _toast(String message) {
    if (!mounted) return;
    ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(message)));
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: Text(_msg('onboarding.title'))),
      body: _buildBody(context),
    );
  }

  Widget _buildBody(BuildContext context) {
    // État 1 : chargement
    if (_loading) {
      return const Center(
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            CircularProgressIndicator(),
            SizedBox(height: 12),
            Text('Chargement de la configuration…'),
          ],
        ),
      );
    }

    // État 2/5 : erreur de chargement (dont réseau) — actionnable
    if (_loadError != null) {
      return _centered(
        icon: Icons.cloud_off,
        title: _msg('onboarding.loadError'),
        subtitle: _msg('onboarding.loadErrorHint'),
        action: FilledButton(
          onPressed: _load,
          style: FilledButton.styleFrom(minimumSize: const Size(180, 48)),
          child: Text(_msg('onboarding.retry')),
        ),
      );
    }

    final progress = _progress;
    if (progress == null || progress.steps.isEmpty) {
      // État 3 : vide
      return _centered(
        icon: Icons.check_circle_outline,
        title: _msg('onboarding.noSteps'),
        subtitle: _msg('onboarding.noStepsHint'),
      );
    }

    // État 4 : succès
    if (progress.isComplete) {
      return _centered(
        icon: Icons.celebration_outlined,
        title: _msg('onboarding.allDone'),
        subtitle: _msg('onboarding.allDoneHint'),
        action: FilledButton(
          onPressed: () => Navigator.of(context).maybePop(),
          style: FilledButton.styleFrom(minimumSize: const Size(200, 48)),
          child: Text(_msg('onboarding.goToDashboard')),
        ),
      );
    }

    final steps = progress.steps;
    final current = steps[_index.clamp(0, steps.length - 1)];

    return ListView(
      padding: const EdgeInsets.all(16),
      children: [
        _buildProgressBar(progress),
        const SizedBox(height: 12),
        _buildStepper(steps, current),
        const SizedBox(height: 16),
        Text(current.title,
            style: Theme.of(context).textTheme.titleLarge),
        if (current.description != null && current.description!.isNotEmpty) ...[
          const SizedBox(height: 4),
          Text(current.description!,
              style: Theme.of(context).textTheme.bodyMedium),
        ],
        const SizedBox(height: 16),
        if (current.isCompleted)
          _completedPanel(current)
        else ...[
          _buildForm(current),
          if (_actionError != null) ...[
            const SizedBox(height: 12),
            _errorBanner(_actionError!),
          ],
          if (current.isSkippable) ...[
            const SizedBox(height: 12),
            _buildSkip(current),
          ],
        ],
      ],
    );
  }

  Widget _buildProgressBar(OnboardingProgress p) {
    final done = p.completedSteps + p.skippedSteps;
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text('${_msg('onboarding.progressLabel')}: $done/${p.totalSteps} (${p.percentage} %)'),
        const SizedBox(height: 6),
        ClipRRect(
          borderRadius: BorderRadius.circular(8),
          child: LinearProgressIndicator(
            value: p.totalSteps == 0 ? 0 : p.percentage / 100,
            minHeight: 8,
            backgroundColor: Colors.white24,
            valueColor: AlwaysStoppedAnimation<Color>(AppColors.primary),
          ),
        ),
      ],
    );
  }

  Widget _buildStepper(List<OnboardingStep> steps, OnboardingStep current) {
    return SizedBox(
      height: 48,
      child: ListView.separated(
        scrollDirection: Axis.horizontal,
        itemCount: steps.length,
        separatorBuilder: (_, __) => const Icon(Icons.chevron_right, size: 16),
        itemBuilder: (context, i) {
          final s = steps[i];
          final selected = s.id == current.id;
          return Semantics(
            selected: selected,
            button: true,
            child: InkWell(
              // Une étape future n'est pas navigable : on ne montre pas un
              // formulaire qu'on ne peut pas encore soumettre (contrat d'ordre).
              onTap: (i <= _index || s.isCompleted) ? () => setState(() => _index = i) : null,
              child: Container(
                padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
                decoration: BoxDecoration(
                  color: selected
                      ? AppColors.primary
                      : (s.isCompleted ? Colors.green.shade700 : Colors.white12),
                  borderRadius: BorderRadius.circular(20),
                ),
                child: Row(
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    if (s.isCompleted)
                      const Icon(Icons.check, size: 14, color: Colors.white)
                    else
                      Text('${s.stepOrder + 1}',
                          style: const TextStyle(color: Colors.white)),
                    const SizedBox(width: 6),
                    // pastille d'étape + libellé tronqué
                    ConstrainedBox(
                      constraints: const BoxConstraints(maxWidth: 110),
                      child: Text(s.title,
                          overflow: TextOverflow.ellipsis,
                          style: const TextStyle(color: Colors.white, fontSize: 12)),
                    ),
                  ],
                ),
              ),
            ),
          );
        },
      ),
    );
  }


  /// Formulaire correspondant à l'étape. Les `data` envoyés sont exactement
  /// ceux acceptés par la colonne « data acceptée » du contrat §3.1.
  Widget _buildForm(OnboardingStep step) {
    switch (step.stepType) {
      case OnboardingStepType.churchIdentity:
        return ChurchIdentityForm(
            enabled: !_busy, onSubmit: (d) => _complete(step, d));
      case OnboardingStepType.memberImport:
        return MemberImportForm(
            enabled: !_busy, onSubmit: (d) => _complete(step, d));
      case OnboardingStepType.structure:
        return StructureForm(
            enabled: !_busy, onSubmit: (d) => _complete(step, d));
      case OnboardingStepType.roles:
        return RolesForm(
            enabled: !_busy, onSubmit: (d) => _complete(step, d));
      case OnboardingStepType.branding:
        return BrandingForm(
            enabled: !_busy, onSubmit: (d) => _complete(step, d));
      case OnboardingStepType.modules:
        return ModulesForm(
            enabled: !_busy, onSubmit: (d) => _complete(step, d));
      case OnboardingStepType.firstEvent:
        return FirstEventForm(
            enabled: !_busy, onSubmit: (d) => _complete(step, d));
    }
  }

  Widget _completedPanel(OnboardingStep s) => GlassCard(
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                const Icon(Icons.check_circle, color: Colors.green),
                const SizedBox(width: 8),
                Text(
                  s.status == OnboardingStepStatus.skipped
                      ? _msg('onboarding.skipped')
                      : _msg('onboarding.completed'),
                  style: const TextStyle(fontWeight: FontWeight.bold),
                ),
              ],
            ),
            if (s.completedData != null && s.completedData!.isNotEmpty) ...[
              const SizedBox(height: 8),
              // Données figées en lecture seule : la réouverture d'une étape
              // terminée ne doit jamais permettre une réécriture.
              Text(s.completedData.toString(),
                  style: const TextStyle(fontFamily: 'monospace', fontSize: 11)),
            ],
          ],
        ),
      );

  Widget _buildSkip(OnboardingStep step) => GlassCard(
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            if (step.skipRequiresReason) ...[
              TextField(
                controller: _skipController,
                onChanged: (v) => _skipReason = v,
                enabled: !_busy,
                maxLines: 2,
                decoration: InputDecoration(
                  labelText: _msg('onboarding.skipReason'),
                  border: const OutlineInputBorder(),
                ),
              ),
              const SizedBox(height: 8),
            ],
            OutlinedButton(
              onPressed: _busy ? null : () => _skip(step),
              style: OutlinedButton.styleFrom(minimumSize: const Size(200, 48)),
              child: Text(_msg('onboarding.skip')),
            ),
          ],
        ),
      );

  Widget _errorBanner(String message) => Container(
        width: double.infinity,
        padding: const EdgeInsets.all(12),
        decoration: BoxDecoration(
          color: Colors.red.withValues(alpha: 0.12),
          border: Border.all(color: Colors.red.withValues(alpha: 0.4)),
          borderRadius: BorderRadius.circular(10),
        ),
        child: Text(message, style: const TextStyle(color: Colors.redAccent)),
      );

  Widget _centered({
    required IconData icon,
    required String title,
    String? subtitle,
    Widget? action,
  }) =>
      Center(
        child: Padding(
          padding: const EdgeInsets.all(24),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              Icon(icon, size: 48),
              const SizedBox(height: 12),
              Text(title, textAlign: TextAlign.center,
                  style: Theme.of(context).textTheme.titleMedium),
              if (subtitle != null) ...[
                const SizedBox(height: 8),
                Text(subtitle, textAlign: TextAlign.center),
              ],
              if (action != null) ...[const SizedBox(height: 16), action],
            ],
          ),
        ),
      );
}
