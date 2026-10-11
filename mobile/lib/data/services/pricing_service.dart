import 'api_service.dart';

// TODO_TARIFS_PAIEMENTS_INTERNATIONAL §T9 — Client du catalogue de tarifs.
//
// Contrat avec `PublicPricingController` (backend) :
// ```
// GET /public/pricing/config -> PricingConfigMobile
// GET /public/pricing/plans  -> { currency, cycle, region, plans: [...] }
// ```
//
// Décodage STRICT et explicite (pas de `as Map<String, dynamic>` aveugle) :
// si la forme du backend change, on le voit ici et l'écran affiche un repli
// plutôt que de planter. Même exigence que `currency_catalog_service.dart`.
//
// POINT DE CONTRAT : `amountMinor` est en UNITÉS MINEURES ISO-4217.
// 15000 XAF = 15 000 FCFA (0 décimale) ; 2900 EUR = 29,00 € (2 décimales).
// `formatMinor` est le SEUL endroit du mobile autorisé à convertir. Aucun
// `* 100` ici, ni ailleurs.

enum BillingCycleMobile { monthly, yearly }

BillingCycleMobile _cycleOf(String raw) =>
    raw.toUpperCase() == 'YEARLY' ? BillingCycleMobile.yearly : BillingCycleMobile.monthly;

class PricingCurrency {
  const PricingCurrency({
    required this.code,
    required this.symbol,
    required this.name,
    required this.decimals,
    required this.selected,
  });

  final String code;
  final String symbol;
  final String name;
  final int decimals;
  final bool selected;

  static PricingCurrency? tryParse(Object? raw) {
    if (raw is! Map) return null;
    final code = raw['code'];
    final symbol = raw['symbol'];
    final name = raw['name'];
    final decimals = raw['decimals'];
    final selected = raw['selected'];
    if (code is! String || code.length != 3) return null;
    if (symbol is! String || symbol.isEmpty) return null;
    if (name is! String || name.isEmpty) return null;
    if (decimals is! int || decimals < 0 || decimals > 4) return null;
    return PricingCurrency(
      code: code,
      symbol: symbol,
      name: name,
      decimals: decimals,
      selected: selected is bool && selected,
    );
  }
}

class PricingConfigMobile {
  const PricingConfigMobile({
    required this.catalogVersion,
    required this.displayMode,
    required this.resolvedCurrency,
    required this.resolvedRegion,
    required this.allowCurrencyChoice,
    required this.allowCycleChoice,
    required this.defaultCycle,
    required this.allowMonthly,
    required this.allowYearly,
    required this.stripeEnabled,
    required this.currencies,
  });

  final int catalogVersion;
  final String displayMode;
  final String resolvedCurrency;
  final String resolvedRegion;
  final bool allowCurrencyChoice;
  final bool allowCycleChoice;
  final BillingCycleMobile defaultCycle;
  final bool allowMonthly;
  final bool allowYearly;
  final bool stripeEnabled;
  final List<PricingCurrency> currencies;

  static PricingConfigMobile? tryParse(Object? raw) {
    if (raw is! Map) return null;
    final currenciesRaw = raw['enabledCurrencies'];
    final currencies = <PricingCurrency>[];
    if (currenciesRaw is List) {
      for (final item in currenciesRaw) {
        final parsed = PricingCurrency.tryParse(item);
        if (parsed != null) currencies.add(parsed);
      }
    }
    final version = raw['catalogVersion'];
    return PricingConfigMobile(
      catalogVersion: version is int ? version : 1,
      displayMode: raw['displayMode'] is String ? raw['displayMode'] as String : 'SINGLE',
      resolvedCurrency: raw['resolvedCurrency'] is String ? raw['resolvedCurrency'] as String : 'XAF',
      resolvedRegion: raw['resolvedRegion'] is String ? raw['resolvedRegion'] as String : 'AF',
      allowCurrencyChoice: raw['allowCurrencyChoice'] == true,
      allowCycleChoice: raw['allowCycleChoice'] == true,
      defaultCycle: _cycleOf(raw['defaultCycle'] is String ? raw['defaultCycle'] as String : 'MONTHLY'),
      allowMonthly: raw['allowMonthly'] != false,
      allowYearly: raw['allowYearly'] != false,
      stripeEnabled: raw['stripeEnabled'] == true,
      currencies: currencies,
    );
  }
}

class PlanPrice {
  const PlanPrice({
    required this.currency,
    required this.cycle,
    required this.amountMinor,
    required this.decimals,
    required this.selectable,
    required this.channels,
  });

  final String currency;
  final BillingCycleMobile cycle;

  /// CONTRAT §2.1 : unités mineures ISO-4217.
  final int amountMinor;
  final int decimals;
  final bool selectable;
  final List<String> channels;

  static PlanPrice? tryParse(Object? raw) {
    if (raw is! Map) return null;
    final currency = raw['currency'];
    final amountMinor = raw['amountMinor'];
    final decimals = raw['decimals'];
    if (currency is! String || currency.length != 3) return null;
    if (amountMinor is! int || amountMinor < 0) return null;
    final channelsRaw = raw['channels'];
    return PlanPrice(
      currency: currency,
      cycle: _cycleOf(raw['cycle'] is String ? raw['cycle'] as String : 'MONTHLY'),
      amountMinor: amountMinor,
      decimals: decimals is int && decimals >= 0 && decimals <= 4 ? decimals : 2,
      selectable: raw['selectable'] != false,
      channels: channelsRaw is List
          ? channelsRaw.whereType<String>().toList(growable: false)
          : const <String>[],
    );
  }
}

class PlanDisplay {
  const PlanDisplay({
    required this.available,
    required this.amountMinor,
    required this.currency,
    required this.decimals,
    required this.channels,
    required this.checkoutCurrency,
  });

  /// `false` = aucun prix dans la devise demandée. L'écran affiche alors
  /// « tarif non communiqué » : jamais un 0 qui se lirait comme un plan gratuit.
  final bool available;
  final int? amountMinor;
  final String currency;
  final int decimals;
  final List<String> channels;
  final String? checkoutCurrency;

  /// Le débit se fait dans une autre devise que l'affichage (canal STRONG).
  bool get chargedInDifferentCurrency =>
      checkoutCurrency != null && checkoutCurrency != currency;

  static PlanDisplay? tryParse(Object? raw) {
    if (raw is! Map) return null;
    final currency = raw['currency'];
    final decimals = raw['decimals'];
    final amountMinor = raw['amountMinor'];
    final channelsRaw = raw['channels'];
    return PlanDisplay(
      available: raw['available'] == true,
      amountMinor: amountMinor is int ? amountMinor : null,
      currency: currency is String && currency.length == 3 ? currency : 'XAF',
      decimals: decimals is int && decimals >= 0 && decimals <= 4 ? decimals : 2,
      channels: channelsRaw is List
          ? channelsRaw.whereType<String>().toList(growable: false)
          : const <String>[],
      checkoutCurrency: raw['checkoutCurrency'] is String ? raw['checkoutCurrency'] as String : null,
    );
  }
}

class PricingPlan {
  const PricingPlan({
    required this.key,
    required this.name,
    required this.description,
    required this.seatsLimit,
    required this.storageLimitMb,
    required this.aiCreditsLimit,
    required this.prices,
    required this.display,
  });

  final String key;
  final String name;
  final String? description;
  final int? seatsLimit;
  final int? storageLimitMb;
  final int? aiCreditsLimit;
  final List<PlanPrice> prices;
  final PlanDisplay? display;

  static PricingPlan? tryParse(Object? raw) {
    if (raw is! Map) return null;
    final key = raw['key'];
    final name = raw['name'];
    if (key is! String || key.isEmpty) return null;
    if (name is! String || name.isEmpty) return null;
    final pricesRaw = raw['prices'];
    final prices = <PlanPrice>[];
    if (pricesRaw is List) {
      for (final item in pricesRaw) {
        final parsed = PlanPrice.tryParse(item);
        if (parsed != null) prices.add(parsed);
      }
    }
    return PricingPlan(
      key: key,
      name: name,
      description: raw['description'] is String ? raw['description'] as String : null,
      seatsLimit: raw['seatsLimit'] is int ? raw['seatsLimit'] as int : null,
      storageLimitMb: raw['storageLimitMb'] is int ? raw['storageLimitMb'] as int : null,
      aiCreditsLimit: raw['aiCreditsLimit'] is int ? raw['aiCreditsLimit'] as int : null,
      prices: prices,
      display: PlanDisplay.tryParse(raw['display']),
    );
  }
}

class PricingCatalog {
  const PricingCatalog({
    required this.currency,
    required this.cycle,
    required this.region,
    required this.plans,
  });

  final String currency;
  final BillingCycleMobile cycle;
  final String region;
  final List<PricingPlan> plans;

  static PricingCatalog? tryParse(Object? raw) {
    if (raw is! Map) return null;
    final plansRaw = raw['plans'];
    final plans = <PricingPlan>[];
    if (plansRaw is List) {
      for (final item in plansRaw) {
        final parsed = PricingPlan.tryParse(item);
        if (parsed != null) plans.add(parsed);
      }
    }
    return PricingCatalog(
      currency: raw['currency'] is String ? raw['currency'] as String : 'XAF',
      cycle: _cycleOf(raw['cycle'] is String ? raw['cycle'] as String : 'MONTHLY'),
      region: raw['region'] is String ? raw['region'] as String : 'AF',
      plans: plans,
    );
  }
}

class PricingService {
  PricingService(this._api);

  final ApiService _api;

  Future<PricingConfigMobile?> fetchConfig({String? currency}) async {
    try {
      final response = await _api.get(
        '/public/pricing/config',
        params: currency == null ? null : {'currency': currency},
      );
      return PricingConfigMobile.tryParse(response.data);
    } catch (_) {
      return null;
    }
  }

  Future<PricingCatalog?> fetchPlans({
    required String currency,
    required BillingCycleMobile cycle,
    String? region,
  }) async {
    try {
      final response = await _api.get('/public/pricing/plans', params: {
        'currency': currency,
        'cycle': cycle == BillingCycleMobile.yearly ? 'YEARLY' : 'MONTHLY',
        if (region != null && region.isNotEmpty) 'region': region,
      });
      return PricingCatalog.tryParse(response.data);
    } catch (_) {
      return null;
    }
  }
}

/// Formatte un montant MINEUR. Ne jamais lui passer un montant majeur.
///
/// Le nombre de décimales vient du serveur : XAF et XOF n'en acceptent aucune,
/// et afficher « 15 000,00 FCFA » serait une faute visible pour un client local.
String formatMinor(int amountMinor, String currency, int decimals, {String? locale}) {
  final divisor = decimals == 0 ? 1 : _pow10(decimals);
  final value = amountMinor / divisor;
  final symbol = _symbolOf(currency);
  final formatted = _formatNumber(value, decimals, locale);
  // `value` porte déjà les décimales ; on n'ajoute le symbole qu'une fois.
  return symbol == null ? formatted : '$formatted $symbol';
}

int _pow10(int exponent) {
  var result = 1;
  for (var i = 0; i < exponent; i++) {
    result *= 10;
  }
  return result;
}

String _formatNumber(double value, int decimals, String? locale) {
  // Formatage « maison » plutôt que NumberFormat : évite une dépendance
  // intl supplémentaire et garantit un rendu stable sur les six plateformes.
  final negative = value < 0;
  final absolute = value.abs();
  final divisor = decimals == 0 ? 1 : _pow10(decimals);
  final units = (absolute / divisor).floor();
  final cents = (absolute - units * divisor).round();
  final grouped = _group(units.toString());
  final body = decimals == 0 ? grouped : '$grouped.${cents.toString().padLeft(decimals, '0')}';
  return negative ? '-$body' : body;
}

String _group(String digits) {
  final buffer = StringBuffer();
  final count = digits.length;
  for (var i = 0; i < count; i++) {
    if (i > 0 && (count - i) % 3 == 0) buffer.write(' ');
    buffer.write(digits[i]);
  }
  return buffer.toString();
}

String? _symbolOf(String currency) {
  switch (currency.toUpperCase()) {
    case 'XAF':
      return 'FCFA';
    case 'XOF':
      return 'F CFA';
    case 'EUR':
      return '€';
    case 'USD':
      return r'$';
    case 'CAD':
      return r'CA$';
    case 'GBP':
      return '£';
    case 'CHF':
      return 'CHF';
    default:
      // Devise inconnue : on n'invente pas de symbole.
      return null;
  }
}