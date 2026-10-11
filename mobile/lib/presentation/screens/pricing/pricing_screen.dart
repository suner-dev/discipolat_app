import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
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
/// Le CTA d'abonnement n'est volontairement pas implémenté : ouvrir une URL
/// Stripe demande `url_launcher` côté mobile, ce qui n'est pas dans le
/// `pubspec.yaml` aujourd'hui. C'est tracé en §0.1 du TODO plutôt que simulé.
class PricingScreen extends ConsumerStatefulWidget {
  const PricingScreen({super.key});

  @override
  ConsumerState<PricingScreen> createState() => _PricingScreenState();
}

class _PricingScreenState extends ConsumerState<PricingScreen> {
  final PricingService _pricing = PricingService(ApiService());

  PricingConfigMobile? _config;
  PricingCatalog? _catalog;
  String _currency = 'XAF';
  BillingCycleMobile _cycle = BillingCycleMobile.monthly;

  bool _loading = true;
  String? _error;

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
  });

  final PricingPlan plan;
  final BillingCycleMobile cycle;
  final AppLocalizations l10n;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final display = plan.display;

    // Un prix absent se dit. Un « 0 » se lirait comme un plan gratuit.
    final amountMinor = display?.amountMinor;
    final hasPrice = display?.available == true && amountMinor != null;
    final currencyCode = display?.currency ?? 'XAF';
    final decimals = display?.decimals ?? 2;

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