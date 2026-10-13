import 'package:flutter/material.dart';
import 'glass_theme.dart';

/// Enveloppe [Scaffold] + [AppBar] réutilisable pour les écrans « poussés »
/// (détail, création, recherche, etc.). Elle garantit un **header cohérent**
/// (titre, bouton retour, actions, onglets éventuels) et ne possède **jamais**
/// le footer.
///
/// Le pied de page (navigation par onglets) appartient exclusivement au shell
/// d'onglets [MainShell] : un écran empilé par‑dessus la navigation ne doit
/// pas afficher de barre d'onglets, conformément aux applications mobiles de
/// référence (Instagram, WhatsApp, Notion…). Pour un écran plein écran sans
/// header, utiliser directement un [Scaffold].
///
/// Usage :
/// ```dart
/// AppShell(
///   title: 'Événements & Calendrier',
///   leading: const BackButton(),
///   actions: [IconButton(icon: const Icon(Icons.add), onPressed: () {})],
///   bottom: TabBar(controller: _tabController, tabs: const [ /* … */ ]),
///   body: TabBarView(controller: _tabController, children: const [ /* … */ ]),
/// )
/// ```
class AppShell extends StatelessWidget {
  /// Titre affiché dans l'AppBar (header).
  final String title;

  /// Widget de la zone gauche de l'AppBar (ex. [BackButton]).
  final Widget? leading;

  /// Actions à droite de l'AppBar (recherche, ajout, popup, etc.).
  final List<Widget>? actions;

  /// Zone [AppBar.bottom] — typiquement un [TabBar]. Doit être un
  /// [PreferredSizeWidget] (utiliser [TabBar], qui l'implémente).
  final PreferredSizeWidget? bottom;

  /// Contenu principal de l'écran.
  final Widget body;

  /// Réduction du corps quand le clavier apparaît.
  final bool resizeToAvoidBottomInset;

  /// Fond du Scaffold ; par défaut la surface sombre du thème.
  final Color? backgroundColor;

  /// FAB éventuel.
  final Widget? floatingActionButton;

  const AppShell({
    super.key,
    required this.title,
    this.leading,
    this.actions,
    this.bottom,
    required this.body,
    this.resizeToAvoidBottomInset = true,
    this.backgroundColor,
    this.floatingActionButton,
  });

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: backgroundColor ?? AppColors.surfaceDark,
      appBar: AppBar(
        title: Text(
          title,
          style: const TextStyle(
            fontFamily: 'Inter',
            fontWeight: FontWeight.bold,
            fontSize: 18,
          ),
        ),
        backgroundColor: AppColors.cardDark,
        elevation: 0,
        leading: leading,
        actions: actions,
        bottom: bottom,
      ),
      body: body,
      floatingActionButton: floatingActionButton,
      resizeToAvoidBottomInset: resizeToAvoidBottomInset,
      // Volontairement AUCUN bottomNavigationBar : le footer appartient au
      // shell d'onglets ([MainShell]), jamais à un écran poussé.
    );
  }
}
