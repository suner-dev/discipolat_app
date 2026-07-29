import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_localizations/flutter_localizations.dart';
import 'package:firebase_core/firebase_core.dart';
import 'package:connectivity_plus/connectivity_plus.dart';
import 'data/local/sync_service.dart';
import 'data/local/database.dart';
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

class DiscipolatApp extends ConsumerWidget {
  const DiscipolatApp({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
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

    return MaterialApp.router(
      title: 'Discipolat',
      debugShowCheckedModeBanner: false,
      theme: GlassTheme.darkTheme,
      darkTheme: GlassTheme.darkTheme,
      themeMode: ThemeMode.dark,
      localizationsDelegates: const [
        GlobalMaterialLocalizations.delegate,
        GlobalWidgetsLocalizations.delegate,
        GlobalCupertinoLocalizations.delegate,
      ],
      supportedLocales: const [Locale('fr', 'FR')],
      routerConfig: appRouter,
    );
  }
}
