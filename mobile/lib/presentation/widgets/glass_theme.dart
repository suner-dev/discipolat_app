import 'dart:ui';
import 'package:flutter/material.dart';

// ============================================
// Discipolat — Glassmorphism Design System
// ============================================

/// Primary brand colors
///
/// Les trois variantes `primary`/`primaryLight`/`primaryDark` sont **mutables** :
/// elles sont initialisées avec le vert Discipolat par défaut puis remplacées
/// par l'identité de l'église chargée depuis `GET /api/v1/public/settings`
/// (voir [AppColors.applyBranding]). Toute la palette en dérive à l'exécution.
class AppColors {
  AppColors._();

  /// Couleur par défaut — vert Discipolat.
  static const defaultPrimary = Color(0xFF16A34A);
  static const defaultGold = Color(0xFFF59E0B);
  static const defaultGoldLight = Color(0xFFFBBF24);
  static const surface = Color(0xFFF8FAFC);
  static const surfaceDark = Color(0xFF030712);
  static const cardDark = Color(0xFF111827);
  static const cardLight = Color(0xFFFFFFFF);

  static Color primary = defaultPrimary;
  static Color primaryLight = const Color(0xFF22C55E);
  static Color primaryDark = const Color(0xFF15803D);
  static Color accent = defaultGold;
  static Color accentLight = defaultGoldLight;

  /// Couleur des boutons (bouton principal) — par défaut = couleur principale.
  static Color button = defaultPrimary;

  /// Police de caractères de l'église (null = police par défaut de l'app).
  static String? fontFamily;

  /// Applique l'identité de l'église à la palette : la couleur principale
  /// pilote `primary`, `primaryLight` et `primaryDark` (nuances dérivées),
  /// la couleur d'accent pilote `accent`/`accentLight`, et la couleur de
  /// bouton pilote `button` si fournie.
  static void applyBranding(
    Color primaryColor, {
    Color? accentColor,
    Color? buttonColor,
    String? font,
  }) {
    primary = primaryColor;
    primaryLight = _lighten(primaryColor, 0.28);
    primaryDark = _darken(primaryColor, 0.22);
    if (accentColor != null) {
      accent = accentColor;
      accentLight = _lighten(accentColor, 0.24);
    }
    if (buttonColor != null) {
      button = buttonColor;
    } else {
      button = primaryColor;
    }
    fontFamily = font;
  }

  static Color _lighten(Color color, double amount) => Color.lerp(color, Colors.white, amount) ?? color;
  static Color _darken(Color color, double amount) => Color.lerp(color, Colors.black, amount) ?? color;
}

/// Dark glassmorphism theme
class GlassTheme {
  static ThemeData get darkTheme {
    return ThemeData(
      useMaterial3: true,
      brightness: Brightness.dark,
      colorSchemeSeed: AppColors.primary,
      scaffoldBackgroundColor: const Color(0xFF030712),
      fontFamily: 'Inter',
      appBarTheme: const AppBarTheme(
        backgroundColor: Colors.transparent,
        elevation: 0,
        centerTitle: true,
        titleTextStyle: TextStyle(
          color: Colors.white,
          fontSize: 18,
          fontWeight: FontWeight.bold,
          fontFamily: 'Inter',
        ),
        iconTheme: IconThemeData(color: Colors.white70),
      ),
      cardTheme: CardThemeData(
        color: Colors.transparent,
        elevation: 0,
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
      ),
      inputDecorationTheme: InputDecorationTheme(
        filled: true,
        fillColor: Colors.white.withValues(alpha: 0.05),
        border: OutlineInputBorder(
          borderRadius: BorderRadius.circular(12),
          borderSide: BorderSide(color: Colors.white.withValues(alpha: 0.1)),
        ),
        enabledBorder: OutlineInputBorder(
          borderRadius: BorderRadius.circular(12),
          borderSide: BorderSide(color: Colors.white.withValues(alpha: 0.1)),
        ),
        focusedBorder: OutlineInputBorder(
          borderRadius: BorderRadius.circular(12),
          borderSide: BorderSide(color: AppColors.primary, width: 1.5),
        ),
        labelStyle: const TextStyle(color: Colors.white60),
        hintStyle: TextStyle(color: Colors.white.withValues(alpha: 0.3)),
        prefixIconColor: Colors.white38,
      ),
      filledButtonTheme: FilledButtonThemeData(
        style: FilledButton.styleFrom(
          backgroundColor: AppColors.button,
          foregroundColor: Colors.white,
          shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
          padding: const EdgeInsets.symmetric(horizontal: 24, vertical: 14),
          textStyle: const TextStyle(fontSize: 15, fontWeight: FontWeight.w600),
        ),
      ),
      outlinedButtonTheme: OutlinedButtonThemeData(
        style: OutlinedButton.styleFrom(
          foregroundColor: Colors.white70,
          side: BorderSide(color: Colors.white.withValues(alpha: 0.2)),
          shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
          padding: const EdgeInsets.symmetric(horizontal: 24, vertical: 14),
        ),
      ),
      dividerTheme: DividerThemeData(
        color: Colors.white.withValues(alpha: 0.06),
        thickness: 1,
      ),
      bottomNavigationBarTheme: const BottomNavigationBarThemeData(
        backgroundColor: Colors.transparent,
      ),
      drawerTheme: DrawerThemeData(
        backgroundColor: const Color(0xFF030712).withValues(alpha: 0.95),
        shape: const RoundedRectangleBorder(),
      ),
    );
  }
}

// ============================================
// Shared helpers
// ============================================

/// Initiales d'un nom complet (« Marie Martin » → « MM »).
String initialsFromName(String? name) {
  if (name == null || name.trim().isEmpty) return '?';
  final parts = name.trim().split(RegExp(r'\s+'));
  if (parts.length >= 2) {
    return '${parts[0][0]}${parts[1][0]}'.toUpperCase();
  }
  return name.trim().substring(0, 1).toUpperCase();
}

/// Initiales à partir d'un objet utilisateur (firstName/lastName).
String initialsFromUser(Map<String, dynamic> u) {
  final first = (u['firstName'] as String? ?? '').trim();
  final last = (u['lastName'] as String? ?? '').trim();
  return initialsFromName('$first $last');
}

// ============================================
// Reusable Glass Widgets
// ============================================

/// Glass card with backdrop blur
class GlassCard extends StatelessWidget {
  final Widget child;
  final EdgeInsetsGeometry? padding;
  final EdgeInsetsGeometry? margin;
  final BorderRadius? borderRadius;
  final double blur;
  final VoidCallback? onTap;
  final Color? borderColor;

  const GlassCard({
    super.key,
    required this.child,
    this.padding,
    this.margin,
    this.borderRadius,
    this.blur = 20,
    this.onTap,
    this.borderColor,
  });

  @override
  Widget build(BuildContext context) {
    return Container(
      margin: margin,
      decoration: BoxDecoration(
        borderRadius: borderRadius ?? BorderRadius.circular(16),
        border: Border.all(
          color: borderColor ?? Colors.white.withValues(alpha: 0.08),
          width: 1,
        ),
        gradient: LinearGradient(
          begin: Alignment.topLeft,
          end: Alignment.bottomRight,
          colors: [
            Colors.white.withValues(alpha: 0.08),
            Colors.white.withValues(alpha: 0.03),
          ],
        ),
      ),
      child: ClipRRect(
        borderRadius: borderRadius ?? BorderRadius.circular(16),
        child: BackdropFilter(
          filter: ImageFilter.blur(sigmaX: blur, sigmaY: blur),
          child: Material(
            color: Colors.transparent,
            child: InkWell(
              onTap: onTap,
              borderRadius: borderRadius ?? BorderRadius.circular(16),
              child: Padding(
                padding: padding ?? const EdgeInsets.all(16),
                child: child,
              ),
            ),
          ),
        ),
      ),
    );
  }
}

/// Glass card with top gradient accent line
class GlassStatCard extends StatelessWidget {
  final String label;
  final String value;
  final IconData icon;
  final Color gradientStart;
  final Color gradientEnd;
  final Color? iconColor;
  final String? trend;
  final bool trendUp;

  const GlassStatCard({
    super.key,
    required this.label,
    required this.value,
    required this.icon,
    required this.gradientStart,
    required this.gradientEnd,
    this.iconColor,
    this.trend,
    this.trendUp = true,
  });

  @override
  Widget build(BuildContext context) {
    return GlassCard(
      padding: const EdgeInsets.all(16),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              Text(label, style: TextStyle(color: Colors.white.withValues(alpha: 0.5), fontSize: 12)),
              Container(
                padding: const EdgeInsets.all(8),
                decoration: BoxDecoration(
                  gradient: LinearGradient(
                    colors: [gradientStart, gradientEnd],
                    begin: Alignment.topLeft,
                    end: Alignment.bottomRight,
                  ),
                  borderRadius: BorderRadius.circular(10),
                ),
                child: Icon(icon, color: Colors.white, size: 16),
              ),
            ],
          ),
          const SizedBox(height: 12),
          Text(
            value,
            style: const TextStyle(
              color: Colors.white,
              fontSize: 28,
              fontWeight: FontWeight.bold,
              fontFamily: 'monospace',
            ),
          ),
          if (trend != null) ...[
            const SizedBox(height: 4),
            Row(
              children: [
                Icon(
                  trendUp ? Icons.trending_up : Icons.trending_down,
                  color: trendUp ? Colors.green : Colors.red,
                  size: 14,
                ),
                const SizedBox(width: 4),
                Text(
                  trend!,
                  style: TextStyle(
                    color: trendUp ? Colors.green : Colors.red,
                    fontSize: 11,
                  ),
                ),
              ],
            ),
          ],
        ],
      ),
    );
  }
}

/// Gradient circular avatar
class GradientAvatar extends StatelessWidget {
  final String text;
  final double radius;
  final Color? gradientStart;
  final Color? gradientEnd;
  final bool showGlow;
  final bool showStatus;

  const GradientAvatar({
    super.key,
    required this.text,
    this.radius = 24,
    this.gradientStart,
    this.gradientEnd,
    this.showGlow = false,
    this.showStatus = false,
  });

  @override
  Widget build(BuildContext context) {
    return Container(
      width: radius * 2,
      height: radius * 2,
      decoration: BoxDecoration(
        gradient: LinearGradient(
          colors: [
            gradientStart ?? AppColors.primary,
            gradientEnd ?? AppColors.primaryLight,
          ],
          begin: Alignment.topLeft,
          end: Alignment.bottomRight,
        ),
        borderRadius: BorderRadius.circular(radius),
        boxShadow: showGlow
            ? [BoxShadow(color: AppColors.primary.withValues(alpha: 0.4), blurRadius: 12, spreadRadius: 1)]
            : null,
      ),
      child: Stack(
        children: [
          Center(
            child: Text(
              text.length > 2 ? text.substring(0, 2).toUpperCase() : text.toUpperCase(),
              style: TextStyle(
                color: Colors.white,
                fontSize: radius * 0.6,
                fontWeight: FontWeight.bold,
              ),
            ),
          ),
          if (showStatus)
            Positioned(
              bottom: 1,
              right: 1,
              child: Container(
                width: radius * 0.35,
                height: radius * 0.35,
                decoration: const BoxDecoration(
                  color: Colors.green,
                  shape: BoxShape.circle,
                  boxShadow: [BoxShadow(color: Colors.green, blurRadius: 6)],
                ),
              ),
            ),
        ],
      ),
    );
  }
}

/// Status badge with color
class StatusBadge extends StatelessWidget {
  final String label;
  final Color color;
  final bool glowing;

  const StatusBadge({
    super.key,
    required this.label,
    required this.color,
    this.glowing = false,
  });

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
      decoration: BoxDecoration(
        color: color.withValues(alpha: 0.15),
        borderRadius: BorderRadius.circular(20),
        border: Border.all(color: color.withValues(alpha: 0.3)),
        boxShadow: glowing ? [BoxShadow(color: color.withValues(alpha: 0.3), blurRadius: 8)] : null,
      ),
      child: Row(
        mainAxisSize: MainAxisSize.min,
        children: [
          Container(
            width: 6,
            height: 6,
            decoration: BoxDecoration(
              color: color,
              shape: BoxShape.circle,
              boxShadow: glowing ? [BoxShadow(color: color, blurRadius: 4)] : null,
            ),
          ),
          const SizedBox(width: 6),
          Text(label, style: TextStyle(color: color, fontSize: 11, fontWeight: FontWeight.w600)),
        ],
      ),
    );
  }
}

/// Shimmer loading placeholder
class ShimmerLoading extends StatelessWidget {
  final int itemCount;

  const ShimmerLoading({super.key, this.itemCount = 3});

  @override
  Widget build(BuildContext context) {
    return ListView.builder(
      padding: const EdgeInsets.all(16),
      itemCount: itemCount,
      itemBuilder: (context, index) {
        return GlassCard(
          margin: const EdgeInsets.only(bottom: 12),
          padding: const EdgeInsets.all(16),
          child: Row(
            children: [
              Container(
                width: 48,
                height: 48,
                decoration: BoxDecoration(
                  color: Colors.white.withValues(alpha: 0.05),
                  borderRadius: BorderRadius.circular(12),
                ),
              ),
              const SizedBox(width: 12),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Container(
                      height: 14,
                      width: 150,
                      decoration: BoxDecoration(
                        color: Colors.white.withValues(alpha: 0.05),
                        borderRadius: BorderRadius.circular(4),
                      ),
                    ),
                    const SizedBox(height: 8),
                    Container(
                      height: 10,
                      width: 100,
                      decoration: BoxDecoration(
                        color: Colors.white.withValues(alpha: 0.03),
                        borderRadius: BorderRadius.circular(4),
                      ),
                    ),
                  ],
                ),
              ),
            ],
          ),
        );
      },
    );
  }
}

/// Section title with decorative elements
class SectionTitle extends StatelessWidget {
  final String title;
  final IconData? icon;
  final Widget? trailing;

  const SectionTitle({super.key, required this.title, this.icon, this.trailing});

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 16),
      child: Row(
        children: [
          if (icon != null) ...[
            Icon(icon, color: AppColors.primary, size: 18),
            const SizedBox(width: 8),
          ],
          Expanded(
            child: Text(
              title,
              maxLines: 1,
              overflow: TextOverflow.ellipsis,
              style: const TextStyle(
                color: Colors.white,
                fontSize: 18,
                fontWeight: FontWeight.bold,
              ),
            ),
          ),
          if (trailing != null) ...[
            const SizedBox(width: 8),
            trailing!,
          ],
        ],
      ),
    );
  }
}

/// Gradient divider
class GlassDivider extends StatelessWidget {
  const GlassDivider({super.key});

  @override
  Widget build(BuildContext context) {
    return Container(
      height: 1,
      decoration: BoxDecoration(
        gradient: LinearGradient(
          colors: [
            Colors.transparent,
            Colors.white.withValues(alpha: 0.1),
            Colors.transparent,
          ],
        ),
      ),
    );
  }
}

/// Animated bottom nav bar
class GlassBottomNav extends StatelessWidget {
  final int currentIndex;
  final Function(int) onTap;

  const GlassBottomNav({super.key, required this.currentIndex, required this.onTap});

  @override
  Widget build(BuildContext context) {
    return Container(
      margin: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        borderRadius: BorderRadius.circular(20),
        border: Border.all(color: Colors.white.withValues(alpha: 0.08)),
        gradient: LinearGradient(
          colors: [
            const Color(0xFF111827).withValues(alpha: 0.9),
            const Color(0xFF111827).withValues(alpha: 0.8),
          ],
        ),
        boxShadow: [BoxShadow(color: Colors.black.withValues(alpha: 0.3), blurRadius: 20)],
      ),
      child: ClipRRect(
        borderRadius: BorderRadius.circular(20),
        child: BackdropFilter(
          filter: ImageFilter.blur(sigmaX: 15, sigmaY: 15),
          child: BottomNavigationBar(
            currentIndex: currentIndex,
            onTap: onTap,
            backgroundColor: Colors.transparent,
            elevation: 0,
            selectedItemColor: AppColors.primary,
            unselectedItemColor: Colors.white38,
            type: BottomNavigationBarType.fixed,
            items: const [
              BottomNavigationBarItem(icon: Icon(Icons.dashboard), label: 'Accueil'),
              BottomNavigationBarItem(icon: Icon(Icons.favorite), label: 'Âmes'),
              BottomNavigationBarItem(icon: Icon(Icons.description), label: 'Rapports'),
              BottomNavigationBarItem(icon: Icon(Icons.person), label: 'Profil'),
            ],
          ),
        ),
      ),
    );
  }
}
