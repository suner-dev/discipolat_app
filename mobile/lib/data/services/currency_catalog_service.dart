// G1 (côté mobile) — Consommation du catalogue de devises ISO-4217 livré par
// le backend : `GET /api/v1/platform/currencies`.
//
// Réponse attendue (contrat, cf. PlatformCurrenciesController côté backend) :
// ```json
// { "standard": "ISO-4217", "count": 3,
//   "currencies": [ { "code": "XAF", "name": "…", "symbol": "FCFA", "decimals": 0 } ] }
// ```
//
// Le décodage est **strict et explicite** (pas de `as Map<String, dynamic>`
// aveugle) : si le backend change la forme, on le voit ici, et l'écran affiche
// un repli au lieu de planter. D12 : aucune devise n'est imposée, la saisie
// reste libre.

import 'api_service.dart';

class CurrencyOption {
  const CurrencyOption({
    required this.code,
    required this.name,
    required this.symbol,
    required this.decimals,
  });

  final String code;
  final String name;
  final String symbol;
  final int decimals;

  /// `XAF — FCFA, Franc CFA (BEAC)` : l'utilisateur reconnaît la devise à son
  /// symbole autant qu'à son code.
  String get label => '$code — $symbol, $name';

  static CurrencyOption? tryParse(Object? raw) {
    if (raw is! Map) return null;
    final code = raw['code'];
    final name = raw['name'];
    final symbol = raw['symbol'];
    final decimals = raw['decimals'];
    if (code is! String || code.length != 3) return null;
    if (name is! String || name.isEmpty) return null;
    if (symbol is! String || symbol.isEmpty) return null;
    if (decimals is! int || decimals < 0 || decimals > 4) return null;
    return CurrencyOption(
      code: code,
      name: name,
      symbol: symbol,
      decimals: decimals,
    );
  }
}

/// Repli documentaire : les devises que le produit sait formater.
/// Filet de sécurité, pas une liste de référence.
const List<CurrencyOption> fallbackCurrencies = [
  CurrencyOption(code: 'EUR', name: 'Euro', symbol: '€', decimals: 2),
  CurrencyOption(code: 'XAF', name: 'Franc CFA (BEAC)', symbol: 'FCFA', decimals: 0),
  CurrencyOption(code: 'USD', name: 'Dollar des États-Unis', symbol: r'$', decimals: 2),
];

class CurrencyCatalog {
  const CurrencyCatalog({required this.currencies, required this.fromServer});

  final List<CurrencyOption> currencies;

  /// `false` = repli : l'interface doit le dire, sinon l'utilisateur croit à
  /// une liste officielle qui n'est pas celle du serveur.
  final bool fromServer;
}

class CurrencyCatalogService {
  CurrencyCatalogService(this._api);

  final ApiService _api;

  Future<CurrencyCatalog> fetch() async {
    try {
      final response = await _api.get('/platform/currencies');
      final data = response.data;
      if (data is! Map) return const CurrencyCatalog(currencies: fallbackCurrencies, fromServer: false);
      final raw = data['currencies'];
      if (raw is! List) return const CurrencyCatalog(currencies: fallbackCurrencies, fromServer: false);

      final parsed = <CurrencyOption>[];
      for (final item in raw) {
        final option = CurrencyOption.tryParse(item);
        if (option != null) parsed.add(option);
      }
      // Une réponse vide ou entièrement illisible vaut mieux traitée comme un
      // échec qu'affichée comme un catalogue vide : l'utilisateur verrait un
      // champ sans aucune suggestion.
      if (parsed.isEmpty) {
        return const CurrencyCatalog(currencies: fallbackCurrencies, fromServer: false);
      }
      parsed.sort((a, b) => a.code.compareTo(b.code));
      return CurrencyCatalog(currencies: parsed, fromServer: true);
    } catch (_) {
      return const CurrencyCatalog(currencies: fallbackCurrencies, fromServer: false);
    }
  }
}
