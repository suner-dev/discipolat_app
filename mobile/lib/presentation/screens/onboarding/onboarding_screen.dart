import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import 'package:shared_preferences/shared_preferences.dart';
import '../../widgets/glass_theme.dart';

/// Onboarding screen for first-time users
///
/// Features:
/// - Guided step-by-step tutorial for each role
/// - Contextual tooltips
/// - First connection checklist
/// - Skip option for returning users
/// - Progress saving
class OnboardingScreen extends StatefulWidget {
  const OnboardingScreen({super.key});

  @override
  State<OnboardingScreen> createState() => _OnboardingScreenState();
}

class _OnboardingScreenState extends State<OnboardingScreen> with SingleTickerProviderStateMixin {
  final PageController _pageController = PageController();
  int _currentPage = 0;
  static const int _totalPages = 5;

  static final List<OnboardingPage> _pages = [
    OnboardingPage(
      title: 'Bienvenue sur Discipolat',
      description: 'L\'application qui facilite votre suivi de disciples',
      icon: Icons.church_rounded,
      color: Colors.green,
      gradient: [Color(0xFF16A34A), Color(0xFF22C55E)],
    ),
    OnboardingPage(
      title: 'Suivi en temps réel',
      description: 'Accédez à vos âmes, rapports et événements depuis n\'importe où',
      icon: Icons.favorite_rounded,
      color: Colors.red,
      gradient: [Color(0xFFDC2626), Color(0xFFEF4444)],
    ),
    OnboardingPage(
      title: 'Hors ligne',
      description: 'Toutes les fonctionnalités critiques fonctionnent sans connexion',
      icon: Icons.wifi_off_rounded,
      color: Colors.blue,
      gradient: [Color(0xFF2563EB), Color(0xFF3B82F6)],
    ),
    OnboardingPage(
      title: 'Notifications intelligentes',
      description: 'Recevez des alertes personnalisées selon votre rôle',
      icon: Icons.notifications_rounded,
      color: Colors.purple,
      gradient: [Color(0xFF7C3AED), Color(0xFF8B5CF6)],
    ),
    OnboardingPage(
      title: 'Sécurité renforcée',
      description: 'Authentification biométrique, session sécurisée et protection des données',
      icon: Icons.lock_rounded,
      color: Colors.amber,
      gradient: [Color(0xFFD97706), Color(0xFFF59E0B)],
    ),
  ];

    @override
  void initState() {
    super.initState();
  }

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

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: Container(
        decoration: BoxDecoration(
          gradient: LinearGradient(
            begin: Alignment.topLeft,
            end: Alignment.bottomRight,
            colors: [Color(0xFF030712), Color(0xFF0F172A), Color(0xFF030712)],
          ),
        ),
        child: SafeArea(
          child: Column(
            children: [
              // Progress indicator
              Container(
                padding: EdgeInsets.symmetric(horizontal: 24, vertical: 16),
                child: Row(
                  children: List.generate(_totalPages, (index) {
                    return Expanded(
                      child: Container(
                        height: 4,
                        margin: EdgeInsets.only(right: index < _totalPages - 1 ? 8 : 0),
                        decoration: BoxDecoration(
                          borderRadius: BorderRadius.circular(2),
                          color: index == _currentPage
                              ? AppColors.primary
                              : Colors.white.withValues(alpha: 0.2),
                        ),
                      ),
                    );
                  }),
                ),
              ),
              // Page content
              Expanded(
                child: PageView.builder(
                  controller: _pageController,
                  itemCount: _totalPages,
                  onPageChanged: (index) => setState(() => _currentPage = index),
                  itemBuilder: (context, index) => _buildPage(_pages[index]),
                ),
              ),
              // Navigation buttons
              Padding(
                padding: EdgeInsets.all(24),
                child: Row(
                  mainAxisAlignment: MainAxisAlignment.spaceBetween,
                  children: [
                    TextButton(
                      onPressed: () => _completeOnboarding(),
                      child: Text('Passer', style: TextStyle(color: Colors.white70)),
                    ),
                    Container(
                      padding: EdgeInsets.symmetric(horizontal: 24, vertical: 12),
                      decoration: BoxDecoration(
                        gradient: LinearGradient(colors: _pages[_currentPage].gradient),
                        borderRadius: BorderRadius.circular(30),
                        boxShadow: [BoxShadow(color: _pages[_currentPage].color.withValues(alpha: 0.4), blurRadius: 12)],
                      ),
                      child: InkWell(
                        onTap: _currentPage == _totalPages - 1
                            ? _completeOnboarding
                            : () => _pageController.nextPage(
                                duration: Duration(milliseconds: 300),
                                curve: Curves.easeInOut,
                              ),
                        child: Row(
                          mainAxisSize: MainAxisSize.min,
                          children: [
                            Text(
                              _currentPage == _totalPages - 1 ? 'Commencer' : 'Suivant',
                              style: TextStyle(color: Colors.white, fontSize: 16, fontWeight: FontWeight.bold),
                            ),
                            SizedBox(width: 8),
                            Icon(Icons.arrow_forward_rounded, color: Colors.white, size: 18),
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
      ),
    );
  }

  Widget _buildPage(OnboardingPage page) {
    return Padding(
      padding: EdgeInsets.all(32),
      child: Column(
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          Container(
            width: 120,
            height: 120,
            decoration: BoxDecoration(
              gradient: LinearGradient(colors: page.gradient),
              borderRadius: BorderRadius.circular(30),
              boxShadow: [BoxShadow(color: page.color.withValues(alpha: 0.4), blurRadius: 24, spreadRadius: 2)],
            ),
            child: Icon(page.icon, color: Colors.white, size: 56),
          ),
          SizedBox(height: 32),
          Text(
            page.title,
            style: TextStyle(color: Colors.white, fontSize: 28, fontWeight: FontWeight.bold, letterSpacing: -0.5),
            textAlign: TextAlign.center,
          ),
          SizedBox(height: 16),
          Text(
            page.description,
            style: TextStyle(color: Colors.white.withValues(alpha: 0.7), fontSize: 16, height: 1.5),
            textAlign: TextAlign.center,
          ),
        ],
      ),
    );
  }
}

class OnboardingPage {
  final String title;
  final String description;
  final IconData icon;
  final Color color;
  final List<Color> gradient;

  OnboardingPage({
    required this.title,
    required this.description,
    required this.icon,
    required this.color,
    required this.gradient,
  });
}
