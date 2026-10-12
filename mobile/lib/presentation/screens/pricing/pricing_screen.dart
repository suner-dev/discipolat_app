import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:url_launcher/url_launcher.dart';
import '../../../../data/services/api_service.dart';
import '../../../../data/services/pricing_service.dart';
import '../../../../l10n/app_localizations.dart';

/// Catalogue des tarifs côté mobile — TODO_TARIFS_PAIEMENTS_INTERNATIONAL §T9.2.
///
/// Deux principes :
///
/// 1. **Le serveur décide de tout.** Ce que cet écran propose (choix de devise,
///    choix de cycle) vient de `/public/pricing/config`. Un sélecteur absent
///    signifie « non autorisé », pas « oublié » — on n'invente donc jamais
///    d'interrupteur.
///
/// 2. **Aucun montant n'est calculé ici.** `amountMinor` arrive en unités
///    mineures ISO-4217 ; `formatMinor` est le seul endroit du mobile qui
///    convertit, et il applique les décimales du serveur — ce qui évite
///    d'afficher « 15 000,00 FCFA ».
///
/// Le CTA d'abonnement lance le checkout Stripe via `url_launcher` pour les
/// canaux CARD/STRONG, ou initie le flux Mobile Money natif pour le canal MOMO.
class PricingScreen extends ConsumerStatefulWidget {
  const PricingScreen({super.key});

  @override
  ConsumerState<PricingScreen> createState() => _PricingScreenState();
}

class _PricingScreenState extends ConsumerState<PricingScreen> {
  final PricingService _pricing = PricingService(ApiService());
  final ApiService _api = ApiService();

  PricingConfigMobile? _config;
  PricingCatalog? _catalog;
  String _currency = 'XAF';
  BillingCycleMobile _cycle = BillingCycleMobile.monthly;

  bool _loading = true;
  String? _error;
  String? _busyPlanKey;

  @override
  void initState() {
    super.initState();
    _load();
  }

  Future<void> _load() async {
    setState(() {
      _loading = true;
      _error = null;
    });

    final config = await _pricing.fetchConfig(currency: _currency);
    if (config != null && mounted) {
      // Le serveur tranche : si l'utilisateur n'a pas le droit de choisir,
      // on adopte sa devise sans poser de sélecteur.
      final chosen = config.resolvedCurrency;
      final cycle = _cycle == BillingCycleMobile.yearly && !config.allowYearly
          ? BillingCycleMobile.monthly
          : _cycle;
      setState(() {
        _config = config;
        _currency = chosen;
        _cycle = config.allowCycleChoice ? cycle : config.defaultCycle;
      });
    }

    final catalog = await _pricing.fetchPlans(currency: _currency, cycle: _cycle);
    if (!mounted) return;
    setState(() {
      _catalog = catalog;
      _loading = false;
      if (catalog == null) {
        // On le dit plutôt que d'afficher un écran vide : l'utilisateur doit
        // pouvoir distinguer « pas de prix » de « service indisponible ».
        _error = AppLocalizations.of(context).pricingUnavailable;
      }
    });
  }

  void _selectCurrency(String code) {
    if (code == _currency) return;
    setState(() => _currency = code);
    _load();
  }

  void _selectCycle(BillingCycleMobile cycle) {
    if (cycle == _cycle) return;
    setState(() => _cycle = cycle);
    _load();
  }

  /// Lance le checkout pour un plan donné.
  Future<void> _startCheckout(PricingPlan plan) async {
    final config = _config;
    final display = plan.display;
    if (config == null || display == null || !display.available) return;

    // Déterminer le canal par défaut (premier disponible)
    final channels = display.channels;
    String? selectedChannel;
    if (channels.contains('CARD')) selectedChannel = 'CARD';
    else if (channels.contains('STRONG')) selectedChannel = 'STRONG';
    else if (channels.contains('MOMO')) selectedChannel = 'MOMO';
    else if (channels.contains('BANK')) selectedChannel = 'BANK';

    if (selectedChannel == null) return;

    setState(() => _busyPlanKey = plan.key);

    try {
      if (selectedChannel == 'MOMO') {
        await _startMomoCheckout(plan);
      } else {
        await _startStripeCheckout(plan, selectedChannel);
      }
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text('Erreur: $e'), backgroundColor: Theme.of(context).colorScheme.error),
        );
      }
    } finally {
      if (mounted) setState(() => _busyPlanKey = null);
    }
  }

  /// Lance le checkout Stripe (CARD ou STRONG) et ouvre l'URL dans le navigateur.
  Future<void> _startStripeCheckout(PricingPlan plan, String channel) async {
    final cycleStr = _cycle == BillingCycleMobile.yearly ? 'yearly' : 'monthly';
    final response = await _api.post(
      '/billing/stripe/checkout',
      data: {
        'planKey': plan.key.toUpperCase(),
        'billingCycle': cycleStr,
        'currency': _currency,
      },
    );

    final url = response.data?['url'] as String?;
    if (url != null && url.isNotEmpty) {
      final uri = Uri.parse(url);
      if (await canLaunchUrl(uri)) {
        await launchUrl(uri, mode: LaunchMode.externalApplication);
      } else {
        throw Exception('Impossible d\'ouvrir l\'URL de paiement');
      }
    } else {
      throw Exception('URL de checkout introuvable');
    }
  }

  /// Initie le paiement Mobile Money (MTN MoMo) pour un abonnement récurrent.
  Future<void> _startMomoCheckout(PricingPlan plan) async {
    // Pour Mobile Money, on a besoin du numéro de téléphone de l'utilisateur.
    // On ouvre un dialogue pour le saisir.
    final phoneController = TextEditingController();
    final phone = await showDialog<String>(
      context: context,
      builder: (context) => AlertDialog(
        title: Text(AppLocalizations.of(context).pricingMomoPhoneTitle()),
        content: TextField(
          controller: phoneController,
          keyboardType: TextInputType.phone,
          decoration: InputDecoration(
            labelText: AppLocalizations.of(context).pricingMomoPhoneLabel(),
            hintText: '+237 6xx xx xx xx',
          ),
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(context),
            child: Text(AppLocalizations.of(context).cancel),
          ),
          FilledButton(
            onPressed: () => Navigator.pop(context, phoneController.text.trim()),
            child: Text(AppLocalizations.of(context).confirm),
          ),
        ],
      ),
    );

    if (phone == null || phone.isEmpty) return;

    final cycleStr = _cycle == BillingCycleMobile.yearly ? 'yearly' : 'monthly';
    final response = await _api.post(
      '/billing/momo/subscribe',
      data: {
        'planKey': plan.key.toUpperCase(),
        'billingCycle': cycleStr,
        'phone': phone,
      },
    );

    if (mounted) {
      final message = response.data?['message'] as String? ?? 'Demande de paiement envoyée';
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text(message), backgroundColor: Theme.of(context).colorScheme.primary),
      );
    }
  }

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context);
    final config = _config;
    final showCurrencyPicker = config?.allowCurrencyChoice == true &&
        (config?.currencies.length ?? 0) > 1;
    final showCyclePicker = config?.allowCycleChoice == true &&
        (config?.allowMonthly != false || config?.allowYearly != false);

    return Scaffold(
      appBar: AppBar(title: Text(l10n.pricingTitle)),
      body: _loading && _catalog == null
          ? const Center(child: CircularProgressIndicator())
          : RefreshIndicator(
              onRefresh: _load,
              child: ListView(
                padding: const EdgeInsets.all(16),
                children: [
                  if (_error != null)
                    _Banner(
                      text: _error!,
                      color: Theme.of(context).colorScheme.error,
                      icon: Icons.info_outline,
                    ),
                  if (showCyclePicker) ...[
                    const SizedBox(height: 8),
                    SegmentedButton<BillingCycleMobile>(
                      segments: [
                        if (config?.allowMonthly != false)
                          ButtonSegment(
                            value: BillingCycleMobile.monthly,
                            label: Text(l10n.pricingMonthly),
                          ),
                        if (config?.allowYearly != false)
                          ButtonSegment(
                            value: BillingCycleMobile.yearly,
                            label: Text(l10n.pricingYearly),
                          ),
                      ],
                      selected: {_cycle},
                      onSelectionChanged: (selection) =>
                          _selectCycle(selection.first),
                    ),
                  ],
                  if (showCurrencyPicker) ...[
                    const SizedBox(height: 12),
                    _CurrencyPicker(
                      currencies: config!.currencies,
                      selected: _currency,
                      onSelected: _selectCurrency,
                    ),
                  ],
                  const SizedBox(height: 16),
                  ...?_catalog?.plans.map((plan) => Padding(
                        padding: const EdgeInsets.only(bottom: 12),
                        child: _PlanCard(
                          plan: plan,
                          cycle: _cycle,
                          l10n: l10n,
                          isBusy: _busyPlanKey == plan.key,
                          onSubscribe: () => _startCheckout(plan),
                        ),
                      )),
                ],
              ),
            ),
    );
  }
}

class _CurrencyPicker extends StatelessWidget {
  const _CurrencyPicker({
    required this.currencies,
    required this.selected,
    required this.onSelected,
  });

  final List<PricingCurrency> currencies;
  final String selected;
  final ValueChanged<String> onSelected;

  @override
  Widget build(BuildContext context) {
    return Wrap(
      spacing: 8,
      runSpacing: 8,
      children: currencies.map((currency) {
        final isSelected = currency.code == selected;
        return ChoiceChip(
          selected: isSelected,
          // Le sigle ISO-4217 n'est jamais traduit : « EUR » se lit « EUR » partout.
          label: Text('${currency.symbol} ${currency.code}'),
          onSelected: (_) => onSelected(currency.code),
        );
      }).toList(),
    );
  }
}

class _PlanCard extends StatelessWidget {
  const _PlanCard({
    required this.plan,
    required this.cycle,
    required this.l10n,
    required this.isBusy,
    required this.onSubscribe,
  });

  final PricingPlan plan;
  final BillingCycleMobile cycle;
  final AppLocalizations l10n;
  final bool isBusy;
  final VoidCallback onSubscribe;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final display = plan.display;

    // Un prix absent se dit. Un « 0 » se lirait comme un plan gratuit.
    final amountMinor = display?.amountMinor;
    final hasPrice = display?.available == true && amountMinor != null;
    final currencyCode = display?.currency ?? 'XAF';
    final decimals = display?.decimals ?? 2;
    final isFree = hasPrice && amountMinor == 0;
    final canSubscribe = hasPrice && !isFree && display?.selectable == true;

    return Card(
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(plan.name, style: theme.textTheme.titleMedium),
            if (plan.description != null) ...[
              const SizedBox(height: 4),
              Text(
                plan.description!,
                style: theme.textTheme.bodySmall,
              ),
            ],
            const SizedBox(height: 12),
            if (hasPrice)
              Text(
                formatMinor(amountMinor, currencyCode, decimals),
                style: theme.textTheme.headlineMedium,
              )
            else
              Text(l10n.pricingNotPublished, style: theme.textTheme.titleMedium),
            const SizedBox(height: 4),
            Text(
              cycle == BillingCycleMobile.yearly
                  ? l10n.pricingPerYear
                  : l10n.pricingPerMonth,
              style: theme.textTheme.bodySmall,
            ),
            // Le débit en devise forte doit être annoncé : sinon la surprise
            // arrive à l'écran de carte, après le clic.
            if (display != null && display.chargedInDifferentCurrency) ...[
              const SizedBox(height: 4),
              Text(
                l10n.pricingChargedIn(display.checkoutCurrency ?? ''),
                style: theme.textTheme.bodySmall?.copyWith(
                  color: theme.colorScheme.onSurfaceVariant,
                ),
              ),
            ],
            const SizedBox(height: 12),
            if (plan.seatsLimit != null) _Feature(l10n.pricingSeats(plan.seatsLimit!)),
            if (plan.storageLimitMb != null) _Feature(l10n.pricingStorage(plan.storageLimitMb!)),
            if (plan.aiCreditsLimit != null)
              _Feature(l10n.pricingAiCredits(plan.aiCreditsLimit!)),
            if (canSubscribe) ...[
              const SizedBox(height: 16),
              SizedBox(
                width: double.infinity,
                child: FilledButton.icon(
                  onPressed: isBusy ? null : onSubscribe,
                  icon: isBusy
                      ? const SizedBox(
                          width: 18,
                          height: 18,
                          child: CircularProgressIndicator(strokeWidth: 2, color: Colors.white),
                        )
                      : const Icon(Icons.credit_card),
                  label: Text(isBusy
                      ? l10n.pricingSubscribing
                      : l10n.pricingSubscribeOnline),
                ),
              ),
            ] else if (!hasPrice) ...[
              const SizedBox(height: 16),
              SizedBox(
                width: double.infinity,
                child: OutlinedButton.icon(
                  onPressed: () {
                    // TODO: Ouvrir la page de contact
                  },
                  icon: const Icon(Icons.contact_support),
                  label: Text(l10n.pricingContactUs),
                ),
              ),
],
          ],
        ),
      ),
    );
  }
}

class _Feature extends StatelessWidget {
  const _Feature(this.label);

  final String label;

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.only(bottom: 4),
      child: Row(
        children: [
          Icon(Icons.check, size: 16, color: Theme.of(context).colorScheme.primary),
          const SizedBox(width: 8),
          Expanded(child: Text(label, style: Theme.of(context).textTheme.bodyMedium)),
        ],
      ),
    );
  }
}

class _Banner extends StatelessWidget {
  const _Banner({required this.text, required this.color, required this.icon});

  final String text;
  final Color color;
  final IconData icon;

  @override
  Widget build(BuildContext context) {
    return Container(
      margin: const EdgeInsets.only(bottom: 12),
      padding: const EdgeInsets.all(12),
      decoration: BoxDecoration(
        borderRadius: BorderRadius.circular(12),
        border: Border.all(color: color.withValues(alpha: 0.4)),
      ),
      child: Row(
        children: [
          Icon(icon, size: 18, color: color),
          const SizedBox(width: 8),
          Expanded(child: Text(text, style: TextStyle(color: color))),
        ],
      ),
    );
  }
}