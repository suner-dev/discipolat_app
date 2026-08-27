import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import '../widgets/glass_theme.dart';
import '../../../l10n/app_localizations.dart';

class NotFoundScreen extends StatelessWidget {
  final String? path;
  const NotFoundScreen({super.key, this.path});

  static List<Map<String, dynamic>> _quickLinks(BuildContext context) => [
    {'icon': Icons.dashboard_rounded, 'title': AppLocalizations.of(context).translate('quickLinkHome'), 'route': '/dashboard'},
    {'icon': Icons.favorite_rounded, 'title': AppLocalizations.of(context).translate('quickLinkSouls'), 'route': '/souls'},
    {'icon': Icons.group_rounded, 'title': AppLocalizations.of(context).translate('quickLinkFamilies'), 'route': '/families'},
    {'icon': Icons.event_rounded, 'title': AppLocalizations.of(context).translate('quickLinkEvents'), 'route': '/events'},
    {'icon': Icons.description_rounded, 'title': AppLocalizations.of(context).translate('quickLinkReports'), 'route': '/reports/maker'},
    {'icon': Icons.folder_rounded, 'title': AppLocalizations.of(context).translate('quickLinkDocuments'), 'route': '/documents'},
  ];

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: Container(
        decoration: const BoxDecoration(
          gradient: LinearGradient(
            begin: Alignment.topLeft,
            end: Alignment.bottomRight,
            colors: [Color(0xFF030712), Color(0xFF0F172A), Color(0xFF030712)],
          ),
        ),
        child: SafeArea(
          child: Center(
            child: SingleChildScrollView(
              padding: const EdgeInsets.all(24),
              child: Column(
                mainAxisAlignment: MainAxisAlignment.center,
                children: [
                  // Icon with glow
                  Container(
                    width: 100,
                    height: 100,
                    decoration: BoxDecoration(
                      gradient: LinearGradient(
                        colors: [AppColors.primary, AppColors.primaryDark],
                        begin: Alignment.topLeft,
                        end: Alignment.bottomRight,
                      ),
                      borderRadius: BorderRadius.circular(28),
                      boxShadow: [
                        BoxShadow(
                          color: AppColors.primary.withValues(alpha: 0.4),
                          blurRadius: 24,
                          spreadRadius: 2,
                        ),
                      ],
                    ),                      child: const Icon(Icons.explore_rounded, color: Colors.white, size: 48),
                  ),
                  const SizedBox(height: 24),

                  // Error code
                  Text(
                    '404',
                    style: TextStyle(
                      fontSize: 80,
                      fontWeight: FontWeight.bold,
                      color: AppColors.primary,
                      letterSpacing: -4,
                      shadows: [
                        Shadow(color: AppColors.primary, blurRadius: 20),
                      ],
                    ),
                  ),
                  const SizedBox(height: 8),

                  // Title
                  Text(
                    AppLocalizations.of(context).translate('pageNotFound'),
                    style: const TextStyle(
                      fontSize: 24,
                      fontWeight: FontWeight.bold,
                      color: Colors.white,
                    ),
                  ),
                  const SizedBox(height: 8),

                  Text(
                    path != null
                        ? AppLocalizations.of(context).translate('pageNotFoundWithPath').replaceAll('{path}', path!)
                        : AppLocalizations.of(context).translate('pageNotFoundGeneric'),
                    textAlign: TextAlign.center,
                    style: TextStyle(
                      fontSize: 14,
                      color: Colors.white.withValues(alpha: 0.5),
                    ),
                  ),
                  const SizedBox(height: 32),

                  // Quick links
                  Text(
                    AppLocalizations.of(context).translate('availablePages'),
                    style: TextStyle(
                      fontSize: 10,
                      fontWeight: FontWeight.w600,
                      letterSpacing: 2,
                      color: Colors.white.withValues(alpha: 0.3),
                    ),
                  ),
                  const SizedBox(height: 12),

                  Wrap(
                    spacing: 8,
                    runSpacing: 8,
                    alignment: WrapAlignment.center,
                    children: _quickLinks(context).map((link) {
                      return GestureDetector(
                        onTap: () => context.go(link['route'] as String),
                        child: Container(
                          padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 10),
                          decoration: BoxDecoration(
                            color: Colors.white.withValues(alpha: 0.05),
                            borderRadius: BorderRadius.circular(12),
                            border: Border.all(color: Colors.white.withValues(alpha: 0.08)),
                          ),
                          child: Row(
                            mainAxisSize: MainAxisSize.min,
                            children: [
                              Icon(
                                link['icon'] as IconData,
                                color: AppColors.primaryLight,
                                size: 16,
                              ),
                              const SizedBox(width: 6),
                              Text(
                                link['title'] as String,
                                style: TextStyle(
                                  color: Colors.white.withValues(alpha: 0.7),
                                  fontSize: 13,
                                ),
                              ),
                            ],
                          ),
                        ),
                      );
                    }).toList(),
                  ),
                  const SizedBox(height: 32),

                  // Action buttons
                  Row(
                    mainAxisAlignment: MainAxisAlignment.center,
                    children: [
                      OutlinedButton.icon(
                        onPressed: () => Navigator.pop(context),
                        icon: const Icon(Icons.arrow_back, size: 16),
                        label: Text(AppLocalizations.of(context).back),
                      ),
                      const SizedBox(width: 12),
                      FilledButton.icon(
                        onPressed: () => context.go('/dashboard'),
                        icon: const Icon(Icons.home_rounded, size: 16),
                        label: Text(AppLocalizations.of(context).translate('navHome')),
                      ),
                    ],
                  ),

                  const SizedBox(height: 32),

                  // Footer
                  Container(
                    padding: const EdgeInsets.symmetric(vertical: 8),
                    child: Row(
                      mainAxisAlignment: MainAxisAlignment.center,
                      children: [
                        Icon(Icons.church_rounded, color: Colors.white.withValues(alpha: 0.2), size: 14),
                        const SizedBox(width: 6),
                        Text(
                          'Discipolat · ${AppLocalizations.of(context).translate('appTagline')}',
                          style: TextStyle(color: Colors.white.withValues(alpha: 0.2), fontSize: 10),
                        ),
                      ],
                    ),
                  ),
                ],
              ),
            ),
          ),
        ),
      ),
    );
  }
}
