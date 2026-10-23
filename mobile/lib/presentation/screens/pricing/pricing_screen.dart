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

  /// Opérateurs Mobile Money servis par l'agrégateur CinetPay. Noms de marques
  /// — non traduits, comme sur l'écran des dîmes & offrandes.
  static const Map<String, String> _momoOperators = {
    'ORANGE_MONEY': 'Orange Money',
    'MTN_MOMO': 'MTN MoMo',
    'WAVE': 'Wave',
  };

  /// Lance le checkout pour un plan donné.
  ///
  /// Trois principes, identiques au web :
  /// 1. On ne propose que les canaux réellement renvoyés par le serveur
  ///    (`display.channels`) — un bouton qui n'ouvre aucune caisse est pire
  ///    qu'une absence de bouton.
  /// 2. Stripe reste la voie privilégiée pour la carte quand la caisse est
  ///    active ; sinon (et pour Mobile Money) on passe par la caisse unifiée
  ///    de l'agrégateur, qui renvoie une `checkoutUrl` de redirection externe.
  /// 3. Rien n'est calculé ici : c'est le serveur qui résout prix, devise et
  ///    activation à la confirmation du webhook.
  Future<void> _startCheckout(PricingPlan plan) async {
    final config = _config;
    final display = plan.display;
    if (config == null || display == null || !display.available) return;

    final channels = display.channels;
    final cardChannel = channels.contains('CARD')
        ? 'CARD'
        : (channels.contains('STRONG') ? 'STRONG' : null);
    final hasMomo = channels.contains('MOMO');
    if (cardChannel == null && !hasMomo) return;

    // Choix du canal : on ne le demande que si plusieurs sont réellement
    // ouverts. Autrement, on va droit au seul canal disponible.
    String chosen;
    if (cardChannel != null && hasMomo) {
      final picked = await _chooseChannel();
      if (picked == null) return;
      chosen = picked;
    } else {
      chosen = cardChannel ?? 'MOMO';
    }

    setState(() => _busyPlanKey = plan.key);
    try {
      if (chosen == 'MOMO') {
        await _startMomoFlow(plan);
      } else if (config.stripeEnabled) {
        await _startStripeCheckout(plan, chosen);
      } else {
        // Carte sans Stripe : caisse unifiée via l'agrégateur.
        await _startUnifiedCheckout(plan, channel: 'CARD');
      }
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text('$e'), backgroundColor: Theme.of(context).colorScheme.error),
        );
      }
    } finally {
      if (mounted) setState(() => _busyPlanKey = null);
    }
  }

  /// Fond de dialogue proposant les canaux ouverts pour ce plan.
  Future<String?> _chooseChannel() async {
    final l10n = AppLocalizations.of(context);
    return showModalBottomSheet<String>(
      context: context,
      builder: (sheetContext) => SafeArea(
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Padding(
              padding: const EdgeInsets.all(16),
              child: Text(l10n.pricingChooseChannel,
                  style: Theme.of(context).textTheme.titleMedium),
            ),
            ListTile(
              leading: const Icon(Icons.credit_card),
              title: Text(l10n.pricingPayCard),
              onTap: () => Navigator.pop(sheetContext, 'CARD'),
            ),
            ListTile(
              leading: const Icon(Icons.smartphone),
              title: Text(l10n.pricingPayMomo),
              onTap: () => Navigator.pop(sheetContext, 'MOMO'),
            ),
            const SizedBox(height: 8),
          ],
        ),
      ),
    );
  }

  /// Demande l'opérateur Mobile Money puis le numéro, et lance la caisse
  /// unifiée (redirection externe vers le CPS de l'agrégateur).
  Future<void> _startMomoFlow(PricingPlan plan) async {
    final l10n = AppLocalizations.of(context);
    final phoneController = TextEditingController();
    var operator = 'ORANGE_MONEY';

    final confirmed = await showDialog<bool>(
      context: context,
      builder: (dialogContext) => StatefulBuilder(
        builder: (context, setDialogState) => AlertDialog(
          title: Text(l10n.pricingMomoPhoneTitle()),
          content: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              DropdownButtonFormField<String>(
                initialValue: operator,
                decoration: InputDecoration(labelText: l10n.pricingPayOperator),
                items: _momoOperators.entries
                    .map((e) => DropdownMenuItem(value: e.key, child: Text(e.value)))
                    .toList(),
                onChanged: (v) => setDialogState(() => operator = v ?? operator),
              ),
              const SizedBox(height: 12),
              TextField(
                controller: phoneController,
                keyboardType: TextInputType.phone,
                decoration: InputDecoration(
                  labelText: l10n.pricingMomoPhoneLabel(),
                  hintText: '+237 6xx xx xx xx',
                ),
              ),
            ],
          ),
          actions: [
            TextButton(
              onPressed: () => Navigator.pop(dialogContext, false),
              child: Text(l10n.cancel),
            ),
            FilledButton(
              onPressed: () => Navigator.pop(dialogContext, true),
              child: Text(l10n.confirm),
            ),
          ],
        ),
      ),
    );

    if (confirmed != true) return;
    final phone = phoneController.text.trim();
    if (phone.isEmpty) return;
    await _startUnifiedCheckout(plan, channel: 'MOMO', operator: operator, phone: phone);
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

  /// Caisse unifiée OM / MoMo / Wave / Visa via l'agrégateur. Renvoie une
  /// `checkoutUrl` de redirection externe ; l'activation de l'abonnement se
  /// fait côté serveur à la confirmation du webhook — le client ne fait que
  /// ouvrir la caisse puis launcher l'URL.
  Future<void> _startUnifiedCheckout(
    PricingPlan plan, {
    required String channel,
    String? operator,
    String? phone,
  }) async {
    final cycleStr = _cycle == BillingCycleMobile.yearly ? 'YEARLY' : 'MONTHLY';
    final region = _config?.resolvedRegion;
    final response = await _api.post(
      '/billing/subscription/checkout',
      data: {
        'planKey': plan.key.toUpperCase(),
        'billingCycle': cycleStr,
        'currency': _currency,
        if (region != null && region.isNotEmpty) 'region': region,
        'channel': channel,
        if (channel == 'MOMO') 'operator': operator,
        if (channel == 'MOMO') 'phone': phone,
      },
    );

    final data = response.data;
    final checkoutUrl = data is Map ? data['checkoutUrl'] as String? : null;
    if (checkoutUrl != null && checkoutUrl.isNotEmpty) {
      final uri = Uri.parse(checkoutUrl);
      if (await canLaunchUrl(uri)) {
        await launchUrl(uri, mode: LaunchMode.externalApplication);
        return;
      }
      throw Exception('Impossible d\'ouvrir la page de paiement');
    }
    final instructions = data is Map ? data['instructions'] as String? : null;
    throw Exception(instructions ?? 'Réponse de caisse inattendue');
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