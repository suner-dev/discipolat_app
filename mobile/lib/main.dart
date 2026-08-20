import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_localizations/flutter_localizations.dart';
import 'package:firebase_core/firebase_core.dart';
import 'package:connectivity_plus/connectivity_plus.dart';
import 'l10n/app_localizations.dart';
import 'data/local/sync_service.dart';
import 'data/local/database.dart';
import 'data/models/branding.dart';
import 'data/services/providers.dart';
import 'presentation/widgets/glass_theme.dart';
import 'app.dart';

void main() async {
  WidgetsFlutterBinding.ensureInitialized();
  try { await Firebase.initializeApp(); } catch (_) {}

  runApp(
    const ProviderScope(
      child: DiscipolatApp(),
    ),
  );
}

class DiscipolatApp extends ConsumerStatefulWidget {
  const DiscipolatApp({super.key});

  @override
  ConsumerState<DiscipolatApp> createState() => _DiscipolatAppState();
}

class _DiscipolatAppState extends ConsumerState<DiscipolatApp> {
  /// Dernier branding appliqué à la palette (évite de re-dériver les couleurs
  /// à chaque rebuild alors qu'elles n'ont pas changé).
  Branding? _appliedBranding;

  @override
  Widget build(BuildContext context) {
    // Sync listener for connectivity changes
    ref.listen(connectivityProvider, (prev, next) {
      next.whenData((results) {
        final isOnline = results.any((r) =>
            r == ConnectivityResult.wifi ||
            r == ConnectivityResult.mobile ||
            r == ConnectivityResult.ethernet);
        if (prev?.value?.any((r) =>
                r != ConnectivityResult.wifi &&
                r != ConnectivityResult.mobile &&
                r != ConnectivityResult.ethernet) ==
            true &&
            isOnline) {
          ref.read(syncServiceProvider).syncPending();
        }
      });
    });

    // Thème dynamique : identité de l'église chargée depuis /public/settings.
    // Dès que le branding arrive, la palette est dérivée PUIS le MaterialApp est
    // reconstruit — le thème et tous les widgets lisent AppColors.primary au
    // moment du build. En cas d'échec réseau, le branding par défaut est utilisé.
    final branding = ref.watch(brandingProvider).valueOrNull;
    if (branding != null && branding != _appliedBranding) {
      _appliedBranding = branding;
      AppColors.applyBranding(
        branding.primaryColor,
        accentColor: branding.accentColor,
      );
    }

    return MaterialApp.router(
      title: branding?.churchName ?? 'Discipolat',
      debugShowCheckedModeBanner: false,
      theme: GlassTheme.darkTheme,
      darkTheme: GlassTheme.darkTheme,
      themeMode: ThemeMode.dark,
      localizationsDelegates: [
        AppLocalizations.delegate,
        GlobalMaterialLocalizations.delegate,
        GlobalWidgetsLocalizations.delegate,
        GlobalCupertinoLocalizations.delegate,
      ],
      supportedLocales: const [
        Locale('fr'),
        Locale('en'),
      ],
      routerConfig: appRouter,
    );
  }
}
