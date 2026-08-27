import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import 'package:shared_preferences/shared_preferences.dart';
import '../../../../l10n/app_localizations.dart';
import '../../widgets/glass_theme.dart';

/// Landing / Onboarding screen — redesigned premium SaaS
///
/// 5 pages : Bienvenue → Suivi temps réel → Hors ligne → Notifications →
/// Sécurité → Commencer. Glassmorphism, dégradés premium, i18n FR/EN/PT.
class OnboardingScreen extends StatefulWidget {
  const OnboardingScreen({super.key});

  @override
  State<OnboardingScreen> createState() => _OnboardingScreenState();
}

class _OnboardingScreenState extends State<OnboardingScreen> {
  final PageController _pageController = PageController();
  int _currentPage = 0;
  static const int _totalPages = 5;

  @override
  void dispose() {
    _pageController.dispose();
    super.dispose();
  }

  Future<void> _completeOnboarding() async {
    final prefs = await SharedPreferences.getInstance();
    await prefs.setBool('onboarding_complete', true);
    // Go to login
    if (mounted) context.go('/login');
  }

  List<_OnboardingPageData> _pages(AppLocalizations l) => [
        _OnboardingPageData(
          title: l.translate('onboardingWelcomeTitle'),
          description: l.translate('onboardingWelcomeDesc'),
          icon: Icons.church_rounded,
          color: const Color(0xFF16A34A),
          gradient: const [Color(0xFF16A34A), Color(0xFF22C55E)],
          badge: 'Discipolat',
        ),
        _OnboardingPageData(
          title: l.translate('onboardingTrackingTitle'),
          description: l.translate('onboardingTrackingDesc'),
          icon: Icons.favorite_rounded,
          color: const Color(0xFFDC2626),
          gradient: const [Color(0xFF16A34A), Color(0xFF06B6D4)],
          badge: l.translate('onboardingTrackingTitle').split(' ').take(2).join(' '),
        ),
        _OnboardingPageData(
          title: l.translate('onboardingOfflineTitle'),
          description: l.translate('onboardingOfflineDesc'),
          icon: Icons.wifi_off_rounded,
          color: const Color(0xFF2563EB),
          gradient: const [Color(0xFF2563EB), Color(0xFF3B82F6)],
          badge: l.translate('onboardingOfflineTitle').split(' ').take(2).join(' '),
        ),
        _OnboardingPageData(
          title: l.translate('onboardingNotificationsTitle'),
          description: l.translate('onboardingNotificationsDesc'),
          icon: Icons.notifications_rounded,
          color: const Color(0xFF7C3AED),
          gradient: const [Color(0xFF7C3AED), Color(0xFF8B5CF6)],
          badge: l.translate('onboardingNotificationsTitle').split(' ').first,
        ),
        _OnboardingPageData(
          title: l.translate('onboardingSecurityTitle'),
          description: l.translate('onboardingSecurityDesc'),
          icon: Icons.lock_rounded,
          color: const Color(0xFFD97706),
          gradient: const [Color(0xFFF59E0B), Color(0xFFFBBF24)],
          badge: l.translate('onboardingSecurityTitle').split(' ').first,
        ),
      ];

  @override
  Widget build(BuildContext context) {
    final l = AppLocalizations.of(context);
    final pages = _pages(l);
    final isDark = Theme.of(context).brightness == Brightness.dark;

    return Scaffold(
      body: Stack(
        children: [
          // Background gradient
          Container(
            decoration: BoxDecoration(
              gradient: LinearGradient(
                begin: Alignment.topLeft,
                end: Alignment.bottomRight,
                colors: isDark
                    ? [const Color(0xFF030712), const Color(0xFF0F172A), const Color(0xFF030712)]
                    : [const Color(0xFFF8FAFC), const Color(0xFFF1F5F9), const Color(0xFFF8FAFC)],
              ),
            ),
          ),
          // Décor : particules / orbes lumineux
          Positioned(
            top: -80,
            right: -80,
            child: Container(
              width: 220,
              height: 220,
              decoration: BoxDecoration(
                shape: BoxShape.circle,
                gradient: RadialGradient(
                  colors: [AppColors.primary.withValues(alpha: 0.12), AppColors.primary.withValues(alpha: 0)],
                ),
              ),
            ),
          ),
          Positioned(
            bottom: -60,
            left: -60,
            child: Container(
              width: 200,
              height: 200,
              decoration: BoxDecoration(
                shape: BoxShape.circle,
                gradient: RadialGradient(
                  colors: [AppColors.accentLight.withValues(alpha: 0.10), AppColors.accentLight.withValues(alpha: 0)],
                ),
              ),
            ),
          ),
          SafeArea(
            child: Column(
              children: [
                // ── Top bar : logo + sélecteur langue ──
                Padding(
                  padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 12),
                  child: Row(
                    mainAxisAlignment: MainAxisAlignment.spaceBetween,
                    children: [
                      // Logo glassmorphism
                      Container(
                        width: 40,
                        height: 40,
                        decoration: BoxDecoration(
                          borderRadius: BorderRadius.circular(12),
                          gradient: LinearGradient(
                            colors: [AppColors.primary, AppColors.primaryDark],
                          ),
                          boxShadow: [
                            BoxShadow(
                              color: AppColors.primary.withValues(alpha: 0.35),
                              blurRadius: 12,
                              spreadRadius: 1,
                            ),
                          ],
                        ),
                        child: const Icon(Icons.church_rounded, color: Colors.white, size: 22),
                      ),
                      Text(
                        'Discipolat',
                        style: TextStyle(
                          fontSize: 16,
                          fontWeight: FontWeight.w700,
                          letterSpacing: 0.2,
                          color: isDark ? Colors.white : const Color(0xFF111827),
                        ),
                      ),
                      // Sélecteur de langue
                      PopupMenuButton<String>(
                        initialValue: l.locale.languageCode,
                        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
                        onSelected: (code) {},
                        itemBuilder: (context) => ['fr', 'en', 'pt'].map((code) {
                          const names = {'fr': '🇫🇷 Français', 'en': '🇬🇧 English', 'pt': '🇵🇹 Português'};
                          return PopupMenuItem<String>(
                            value: code,
                            child: Text(names[code]!),
                          );
                        }).toList(),
                        child: Container(
                          padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 6),
                          decoration: BoxDecoration(
                            color: isDark ? Colors.white.withValues(alpha: 0.08) : Colors.black.withValues(alpha: 0.04),
                            borderRadius: BorderRadius.circular(8),
                            border: Border.all(
                              color: isDark ? Colors.white.withValues(alpha: 0.12) : Colors.black.withValues(alpha: 0.08),
                            ),
                          ),
                          child: Text(
                            l.locale.languageCode.toUpperCase(),
                            style: TextStyle(
                              fontSize: 12,
                              fontWeight: FontWeight.w600,
                              color: isDark ? Colors.white.withValues(alpha: 0.8) : Colors.black.withValues(alpha: 0.7),
                            ),
                          ),
                        ),
                      ),
                    ],
                  ),
                ),
                // ── Progress indicator ──
                Container(
                  padding: const EdgeInsets.symmetric(horizontal: 24),
                  child: Row(
                    children: List.generate(_totalPages, (index) {
                      return Expanded(
                        child: Container(
                          height: 4,
                          margin: EdgeInsets.only(right: index < _totalPages - 1 ? 8 : 0),
                          decoration: BoxDecoration(
                            borderRadius: BorderRadius.circular(2),
                            gradient: index == _currentPage
                                ? LinearGradient(colors: pages[_currentPage].gradient)
                                : null,
                            color: index == _currentPage ? null : (isDark ? Colors.white.withValues(alpha: 0.2) : AppColors.primary.withValues(alpha: 0.15)),
                          ),
                        ),
                      );
                    }),
                  ),
                ),
                const SizedBox(height: 8),
                // ── PageView ──
                Expanded(
                  child: PageView.builder(
                    controller: _pageController,
                    itemCount: _totalPages,
                    onPageChanged: (index) => setState(() => _currentPage = index),
                    itemBuilder: (context, index) => _buildPage(pages[index], isDark),
                  ),
                ),
                // ── Boutons de navigation ──
                Padding(
                  padding: const EdgeInsets.fromLTRB(24, 0, 24, 24),
                  child: Row(
                    mainAxisAlignment: MainAxisAlignment.spaceBetween,
                    children: [
                      // Passer / Skip
                      TextButton(
                        onPressed: _completeOnboarding,
                        style: TextButton.styleFrom(
                          foregroundColor: isDark ? Colors.white70 : Colors.grey.shade600,
                        ),
                        child: Text(l.translate('onboardingSkip')),
                      ),
                      // Visite AR
                      TextButton.icon(
                        onPressed: () => context.push('/onboarding-ar'),
                        style: TextButton.styleFrom(
                          foregroundColor: isDark ? Colors.white70 : Colors.grey.shade600,
                        ),
                        icon: Icon(Icons.view_in_ar_rounded, size: 18),
                        label: Text(l.translate('onboardingAR'), style: const TextStyle(fontSize: 13)),
                      ),
                      // Bouton principal (Suivant / Commencer)
                      AnimatedContainer(
                        duration: const Duration(milliseconds: 300),
                        padding: const EdgeInsets.symmetric(horizontal: 28, vertical: 14),
                        decoration: BoxDecoration(
                          gradient: LinearGradient(colors: pages[_currentPage].gradient),
                          borderRadius: BorderRadius.circular(28),
                          boxShadow: [
                            BoxShadow(
                              color: pages[_currentPage].color.withValues(alpha: 0.35),
                              blurRadius: 12,
                              spreadRadius: 1,
                            ),
                          ],
                        ),
                        child: InkWell(
                          borderRadius: BorderRadius.circular(28),
                          onTap: _currentPage == _totalPages - 1
                              ? _completeOnboarding
                              : () => _pageController.nextPage(
                                    duration: const Duration(milliseconds: 300),
                                    curve: Curves.easeInOut,
                                  ),
                          child: Row(
                            mainAxisSize: MainAxisSize.min,
                            children: [
                              Text(
                                _currentPage == _totalPages - 1
                                    ? l.translate('onboardingStart')
                                    : l.translate('onboardingNext'),
                                style: const TextStyle(color: Colors.white, fontSize: 16, fontWeight: FontWeight.bold),
                              ),
                              const SizedBox(width: 8),
                              const Icon(Icons.arrow_forward_rounded, color: Colors.white, size: 18),
                            ],
                          ),
                        ),
                      ),
                    ],
                  ),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildPage(_OnboardingPageData page, bool isDark) {
    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: 24),
      child: Column(
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          // ── Badge (pill) ──
          Container(
            padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 6),
            decoration: BoxDecoration(
              color: page.color.withValues(alpha: 0.12),
              borderRadius: BorderRadius.circular(20),
              border: Border.all(color: page.color.withValues(alpha: 0.25)),
            ),
            child: Row(
              mainAxisSize: MainAxisSize.min,
              children: [
                Icon(page.icon, color: page.color, size: 14),
                const SizedBox(width: 6),
                Text(
                  page.badge,
                  style: TextStyle(
                    color: page.color,
                    fontSize: 12,
                    fontWeight: FontWeight.w600,
                  ),
                ),
              ],
            ),
          ),
          const SizedBox(height: 40),
          // ── Icon tile premium (glassmorphism) ──
          TweenAnimationBuilder<double>(
            tween: Tween(begin: 0, end: 1),
            duration: const Duration(milliseconds: 500),
            curve: Curves.easeOutBack,
            builder: (context, t, child) => Transform.scale(
              scale: t,
              child: Container(
                width: 120,
                height: 120,
                decoration: BoxDecoration(
                  gradient: LinearGradient(colors: page.gradient),
                  borderRadius: BorderRadius.circular(28),
                  boxShadow: [
                    BoxShadow(
                      color: page.color.withValues(alpha: 0.35),
                      blurRadius: 24,
                      spreadRadius: 4,
                    ),
                    BoxShadow(
                      color: page.color.withValues(alpha: 0.15),
                      blurRadius: 60,
                      offset: const Offset(0, 12),
                    ),
                  ],
                ),
                child: Icon(page.icon, color: Colors.white, size: 52),
              ),
            ),
          ),
          const SizedBox(height: 40),
          // ── Rings décoratifs ──
          SizedBox(
            width: 200,
            height: 8,
            child: Row(
              mainAxisSize: MainAxisSize.min,
              children: List.generate(3, (i) {
                final isActive = i <= _currentPage;
                return AnimatedContainer(
                  duration: const Duration(milliseconds: 300),
                  margin: const EdgeInsets.symmetric(horizontal: 4),
                  width: isActive ? 44 : 20,
                  height: 8,
                  decoration: BoxDecoration(
                    borderRadius: BorderRadius.circular(4),
                    gradient: isActive ? LinearGradient(colors: page.gradient) : null,
                    color: isActive ? null : (isDark ? Colors.white.withValues(alpha: 0.15) : Colors.black.withValues(alpha: 0.1)),
                  ),
                );
              }),
            ),
          ),
          const SizedBox(height: 24),
          // ── Titre ──
          Text(
            page.title,
            style: TextStyle(
              color: isDark ? Colors.white : const Color(0xFF111827),
              fontSize: 28,
              fontWeight: FontWeight.w700,
              letterSpacing: -0.5,
              height: 1.2,
            ),
            textAlign: TextAlign.center,
          ),
          const SizedBox(height: 16),
          // ── Description ──
          Text(
            page.description,
            style: TextStyle(
              color: isDark ? Colors.white.withValues(alpha: 0.65) : const Color(0xFF4B5563),
              fontSize: 16,
              height: 1.6,
            ),
            textAlign: TextAlign.center,
          ),
        ],
      ),
    );
  }
}

class _OnboardingPageData {
  final String title;
  final String description;
  final String badge;
  final IconData icon;
  final Color color;
  final List<Color> gradient;

  _OnboardingPageData({
    required this.title,
    required this.description,
    required this.badge,
    required this.icon,
    required this.color,
    required this.gradient,
  });
}
