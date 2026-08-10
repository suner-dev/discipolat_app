import 'dart:ui';

/// Identité publique de l'église (thème dynamique).
///
/// Aligné sur le contrat backend `PublicBrandingResponse`
/// (`GET /api/v1/public/settings` — accessible sans authentification).
class Branding {
  final String churchName;
  final String platformName;
  final String? slogan;
  final String? description;
  final String? logoUrl;
  final String? faviconUrl;
  final String? bannerUrl;

  /// Couleur principale de l'église (fallback : vert Discipolat par défaut).
  final Color primaryColor;
  final Color accentColor;
  final Color buttonColor;
  final String? fontFamily;
  final bool allowDarkMode;

  const Branding({
    this.churchName = 'Discipolat',
    this.platformName = 'Discipolat',
    this.slogan,
    this.description,
    this.logoUrl,
    this.faviconUrl,
    this.bannerUrl,
    this.primaryColor = const Color(0xFF16A34A),
    this.accentColor = const Color(0xFFF59E0B),
    this.buttonColor = const Color(0xFF16A34A),
    this.fontFamily,
    this.allowDarkMode = true,
  });

  factory Branding.fromJson(Map<String, dynamic> json) {
    return Branding(
      churchName: json['churchName'] as String? ?? 'Discipolat',
      platformName: json['platformName'] as String? ?? 'Discipolat',
      slogan: json['slogan'] as String?,
      description: json['description'] as String?,
      logoUrl: json['logoUrl'] as String?,
      faviconUrl: json['faviconUrl'] as String?,
      bannerUrl: json['bannerUrl'] as String?,
      primaryColor: colorFromHex(json['primaryColor'] as String?, const Color(0xFF16A34A)),
      accentColor: colorFromHex(json['accentColor'] as String?, const Color(0xFFF59E0B)),
      buttonColor: colorFromHex(json['buttonColor'] as String?, const Color(0xFF16A34A)),
      fontFamily: json['fontFamily'] as String?,
      allowDarkMode: json['allowDarkMode'] as bool? ?? true,
    );
  }
}

/// Parse une couleur hexadécimale (`#RRGGBB` ou `#AARRGGBB`) avec fallback.
///
/// Retourne [fallback] si la valeur est absente, mal formée ou hors plage.
Color colorFromHex(String? hex, Color fallback) {
  if (hex == null || hex.isEmpty) return fallback;
  var h = hex.trim().replaceFirst('#', '');
  if (h.length == 6) {
    h = 'FF$h'; // RRGGBB → AARRGGBB
  }
  if (h.length != 8) return fallback;
  final value = int.tryParse(h, radix: 16);
  if (value == null) return fallback;
  return Color(value);
}
