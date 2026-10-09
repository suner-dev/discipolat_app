import 'package:dio/dio.dart';
import 'package:flutter/material.dart';

import '../../../data/services/api_service.dart';
import '../../widgets/detail_back_button.dart';
import '../../widgets/glass_theme.dart';

/// LOT 2 §GLISE-D'ABORD (T2.6, mobile) — landing publique d'une église.
///
/// <p>Version mobile de `frontend/src/pages/ChurchLandingPage.tsx` (T2.4),
/// **additive** : aucun écran existant n'est remplacé, aucune route existante
/// n'est modifiée, aucun champ hors liste blanche n'est lu. La route
/// `/e/:slug` (deeplink) est déclarée dans `lib/app.dart` comme PUBLIQUE —
/// elle ne déclenche pas le redirect `/login`.</p>
///
/// <p>Contrats tenus ici :</p>
/// <ul>
///   <li><b>R3 — 404 indistinguable.</b> Une réponse HTTP 404 (slug inconnu,
///       église non listée, landing éteinte) affiche un message UNIQUE, sans
///       jamais dire lequel. Aucun oracle côté client : le serveur ne le
///       fournit pas, le widget ne l'invente pas.</li>
///   <li><b>R2 / liste blanche §6.</b> La projection Dart ne lit QUE les clés
///       autorisées (`slug name landingEnabled slogan description logoUrl
///       logoDarkUrl coverUrl faviconUrl website city country locale
///       branding sections`). Tout ajout côté serveur reste INVISIBLE ici —
///       la lecture est défensive (`_pick`) et non exhaustive, aucune
///       désérialisation automatique.</li>
///   <li><b>R4 — zéro fuite de palette.</b> Ce fichier ne mutate PAS
///       `AppColors` (contrairement à `main.dart:131-143` qui applique la
///       marque du tenant courant au niveau app). La landing est affichée
///       avec la palette DÉJÀ en place ; le web fait mieux (`applyScopedBranding`)
///       et le plan (ligne T2.6) indique explicitement « palette lue via la
///       config tenant existante ». Une version ultérieure pourra brancher
///       un `Theme.copyWith` local si le besoin se manifeste ; ici, RIEN
///       n'est cassé chez les voisins (A1).</li>
///   <li><b>A1 — filet de retour.</b> `DetailBackButton` (widget existant,
///       §LOT 2 §BK) est monté dans l'`AppBar` : s'il y a une pile, `pop()` ;
///       sinon, repli sur la racine. Aucun bouton système dupliqué.</li>
/// </ul>
///
/// <p>Esthétique : palette sombre homogène avec `login_screen.dart` /
/// `register_screen.dart` / `church_picker.dart`.</p>
class ChurchLandingData {
  const ChurchLandingData({
    required this.slug,
    required this.name,
    required this.landingEnabled,
    this.slogan,
    this.description,
    this.logoUrl,
    this.coverUrl,
    this.website,
    this.city,
    this.country,
  });

  final String slug;
  final String name;
  final bool landingEnabled;
  final String? slogan;
  final String? description;
  final String? logoUrl;
  final String? coverUrl;
  final String? website;
  final String? city;
  final String? country;

  /// Libellé secondaire (ville, pays) — jamais de PII.
  String get locality {
    final parts = [
      if (city != null && city!.isNotEmpty) city!,
      if (country != null && country!.isNotEmpty) country!,
    ];
    return parts.join(', ');
  }

  /// Lecture DÉFENSIVE : on ne consomme QUE les clés listées §6. Si le
  /// serveur ajoute un champ par accident, il reste invisible ici (R2).
  factory ChurchLandingData.fromJson(Map<String, dynamic> json) {
    return ChurchLandingData(
      slug: _pick(json, 'slug') ?? '',
      name: _pick(json, 'name') ?? '',
      landingEnabled: json['landingEnabled'] == true,
      slogan: _pick(json, 'slogan'),
      description: _pick(json, 'description'),
      logoUrl: _pick(json, 'logoUrl'),
      coverUrl: _pick(json, 'coverUrl'),
      website: _pick(json, 'website'),
      city: _pick(json, 'city'),
      country: _pick(json, 'country'),
    );
  }

  static String? _pick(Map<String, dynamic> json, String key) {
    final v = json[key];
    if (v == null) return null;
    final s = v.toString();
    return s.isEmpty ? null : s;
  }
}

/// Machine à états UI, volontairement fermière : `absent` ne distingue PAS
/// « slug inconnu » / « non listée » / « landing éteinte » (R3).
enum ChurchLandingStatus { loading, absent, error, ready }

class ChurchLandingScreen extends StatefulWidget {
  const ChurchLandingScreen({
    super.key,
    required this.slug,
    this.apiService,
    this.initialState,
  });

  final String slug;
  final ApiService? apiService;

  /// Injecté par les tests pour observer chaque branche sans réseau.
  final ({ChurchLandingStatus status, ChurchLandingData? data, String? message})?
      initialState;

  @override
  State<ChurchLandingScreen> createState() => _ChurchLandingScreenState();
}

class _ChurchLandingScreenState extends State<ChurchLandingScreen> {
  late final ApiService _api;
  ChurchLandingStatus _status = ChurchLandingStatus.loading;
  ChurchLandingData? _data;
  String? _errorMsg;
  int _requestSeq = 0;

  @override
  void initState() {
    super.initState();
    _api = widget.apiService ?? ApiService();
    if (widget.initialState != null) {
      _status = widget.initialState!.status;
      _data = widget.initialState!.data;
      _errorMsg = widget.initialState!.message;
      return;
    }
    // Slug vide : on passe DIRECTEMENT à `absent`, sans setState (on est
    // encore dans `initState`, le build n'a pas eu lieu). Aucun appel
    // réseau : une landing sans slug n'a rien à demander au serveur.
    if (widget.slug.isEmpty) {
      _status = ChurchLandingStatus.absent;
      return;
    }
    // Sinon : `_status` reste à sa valeur par défaut (`loading`), et on
    // amorce le fetch en asynchrone. L'appel à `_load()` depuis `initState`
    // est sûr car son premier `setState` n'intervient qu'APRÈS le `await`
    // (donc après le premier build).
    _load();
  }

  Future<void> _load() async {
    final seq = ++_requestSeq;
    if (widget.slug.isEmpty) {
      // Défense en profondeur : on ne devrait jamais arriver ici (le
      // `initState` court-circuite), mais si le parent change le `slug`
      // en cours de vie vers '', on retombe sur `absent` sans requête.
      if (!mounted) return;
      setState(() {
        _status = ChurchLandingStatus.absent;
        _data = null;
      });
      return;
    }
    setState(() {
      _status = ChurchLandingStatus.loading;
      _errorMsg = null;
    });
    try {
      final res = await _api.get('/public/churches/${Uri.encodeComponent(widget.slug)}');
      if (!mounted || seq != _requestSeq) return;
      final body = res.data;
      if (body is Map) {
        final parsed = ChurchLandingData.fromJson(body.cast<String, dynamic>());
        // Le serveur ne renvoie 200 que si landing_enabled=true ; côté client,
        // on redoute aussi une réponse 200 avec landingEnabled=false (par
        // exemple si un futur déploiement desserre le serveur). Dans ce cas,
        // on traite comme `absent` pour rester fidèle au contrat R3.
        if (parsed.slug.isEmpty || !parsed.landingEnabled) {
          setState(() {
            _status = ChurchLandingStatus.absent;
            _data = null;
          });
          return;
        }
        setState(() {
          _status = ChurchLandingStatus.ready;
          _data = parsed;
        });
      } else {
        setState(() => _status = ChurchLandingStatus.absent);
      }
    } on DioException catch (e) {
      if (!mounted || seq != _requestSeq) return;
      // 404 (toutes causes confondues) = absent, message unique (R3).
      if (e.response?.statusCode == 404) {
        setState(() {
          _status = ChurchLandingStatus.absent;
          _data = null;
          _errorMsg = null;
        });
        return;
      }
      // 429 / 5xx / réseau = error, on propose de réessayer.
      setState(() {
        _status = ChurchLandingStatus.error;
        _errorMsg = e.message ?? 'Réseau indisponible';
      });
    } catch (_) {
      if (!mounted || seq != _requestSeq) return;
      setState(() {
        _status = ChurchLandingStatus.error;
        _errorMsg = 'Erreur inattendue';
      });
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: const Color(0xFF030712),
      appBar: AppBar(
        // L'`AppBar` reste une CHROME statique « Page publique » : le nom de
        // l'église s'affiche en gros titre dans le corps (h1-like), et le
        // dupliquer ici rendrait les sélecteurs `find.text(...)` ambigus
        // (même piège que T3.2 web où le fil d'Ariane écrasait le `<h1>`).
        // Le web fait l'inverse — `document.title = data.name` — parce que
        // le navigateur a un ONGLET distinct du contenu visible ; sur mobile,
        // l'`AppBar` est à la fois l'onglet ET le chrome visible, un seul
        // des deux doit porter le nom. Le corps gagne : il est plus grand,
        // plus lisible, et reste cohérent avec l'état `absent` où le nom
        // est justement caché.
        title: const Text('Page publique'),
        leading: const DetailBackButton(fallbackRoute: '/'),
      ),
      body: Container(
        decoration: const BoxDecoration(
          gradient: LinearGradient(
            begin: Alignment.topLeft,
            end: Alignment.bottomRight,
            colors: [Color(0xFF030712), Color(0xFF0F172A), Color(0xFF030712)],
          ),
        ),
        child: SafeArea(
          child: RefreshIndicator(
            onRefresh: _load,
            child: SingleChildScrollView(
              physics: const AlwaysScrollableScrollPhysics(),
              padding: const EdgeInsets.all(24),
              child: Center(
                child: ConstrainedBox(
                  constraints: const BoxConstraints(maxWidth: 640),
                  child: _buildBody(),
                ),
              ),
            ),
          ),
        ),
      ),
    );
  }

  Widget _buildBody() {
    switch (_status) {
      case ChurchLandingStatus.loading:
        return const Padding(
          padding: EdgeInsets.symmetric(vertical: 80),
          child: Center(
            child: SizedBox(
              width: 32,
              height: 32,
              child: CircularProgressIndicator(strokeWidth: 2.5),
            ),
          ),
        );
      case ChurchLandingStatus.absent:
        // R3 : message UNIQUE, aucune distinction fantôme / non listée /
        // landing éteinte. Pas de SnackBar, pas de dialog : reste dans la
        // page, l'utilisateur peut revenir via DetailBackButton.
        return Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            const SizedBox(height: 40),
            Icon(Icons.public_off_rounded,
                color: Colors.white.withValues(alpha: 0.35), size: 56),
            const SizedBox(height: 16),
            Text(
              'Page indisponible',
              textAlign: TextAlign.center,
              style: TextStyle(
                color: Colors.white.withValues(alpha: 0.85),
                fontSize: 18,
                fontWeight: FontWeight.w600,
              ),
            ),
            const SizedBox(height: 8),
            Text(
              'Cette page n\'existe pas, n\'est pas publiée, ou a été désactivée par son église.',
              textAlign: TextAlign.center,
              style: TextStyle(
                color: Colors.white.withValues(alpha: 0.5),
                fontSize: 13,
              ),
            ),
          ],
        );
      case ChurchLandingStatus.error:
        final detail = _errorMsg == null || _errorMsg!.isEmpty
            ? ''
            : ' ($_errorMsg)';
        return Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            const SizedBox(height: 40),
            Icon(Icons.wifi_off_rounded,
                color: Colors.white.withValues(alpha: 0.35), size: 56),
            const SizedBox(height: 16),
            Text(
              'Indisponible temporairement$detail',
              textAlign: TextAlign.center,
              style: TextStyle(
                color: Colors.white.withValues(alpha: 0.85),
                fontSize: 16,
                fontWeight: FontWeight.w600,
              ),
            ),
            const SizedBox(height: 8),
            Text(
              'Servir cette page a échoué. Réessayez dans un instant.',
              textAlign: TextAlign.center,
              style: TextStyle(
                color: Colors.white.withValues(alpha: 0.5),
                fontSize: 13,
              ),
            ),
            const SizedBox(height: 20),
            OutlinedButton.icon(
              onPressed: _load,
              icon: const Icon(Icons.refresh_rounded, size: 18),
              label: const Text('Réessayer'),
              style: OutlinedButton.styleFrom(
                foregroundColor: Colors.white70,
                side: BorderSide(
                    color: Colors.white.withValues(alpha: 0.24)),
              ),
            ),
          ],
        );
      case ChurchLandingStatus.ready:
        return _buildReady();
    }
  }

  Widget _buildReady() {
    final data = _data!;
    final location = data.locality;
    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        // Identité visuelle : logo si fourni, sinon icône générique.
        Center(
          child: data.logoUrl != null
              ? ClipRRect(
                  borderRadius: BorderRadius.circular(20),
                  child: Image.network(
                    data.logoUrl!,
                    height: 96,
                    width: 96,
                    fit: BoxFit.contain,
                    errorBuilder: (_, __, ___) => _logoFallback(),
                  ),
                )
              : _logoFallback(),
        ),
        const SizedBox(height: 16),
        Text(
          data.name,
          textAlign: TextAlign.center,
          style: TextStyle(
            color: Colors.white,
            fontSize: 26,
            fontWeight: FontWeight.bold,
          ),
        ),
        if (data.slogan != null) ...[
          const SizedBox(height: 8),
          Text(
            data.slogan!,
            textAlign: TextAlign.center,
            style: TextStyle(
              color: AppColors.primaryLight,
              fontSize: 15,
              fontWeight: FontWeight.w500,
            ),
          ),
        ],
        if (location.isNotEmpty) ...[
          const SizedBox(height: 10),
          Row(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              Icon(Icons.location_on_outlined,
                  size: 16, color: Colors.white.withValues(alpha: 0.55)),
              const SizedBox(width: 6),
              Text(
                location,
                style: TextStyle(
                  color: Colors.white.withValues(alpha: 0.55),
                  fontSize: 13,
                ),
              ),
            ],
          ),
        ],
        if (data.description != null) ...[
          const SizedBox(height: 24),
          GlassCard(
            padding: const EdgeInsets.all(16),
            child: Text(
              data.description!,
              style: TextStyle(
                color: Colors.white.withValues(alpha: 0.85),
                fontSize: 14,
                height: 1.5,
              ),
            ),
          ),
        ],
        if (data.website != null) ...[
          const SizedBox(height: 20),
          Center(
            child: Semantics(
              identifier: 'churchLanding.website',
              button: true,
              child: OutlinedButton.icon(
                onPressed: () {
                  // On n'ajoute PAS url_launcher ici : le plan T2.6 ne
                  // l'exige pas, et aucune dépendance nouvelle ne doit
                  // être introduite sans accord. Le lien reste lisible
                  // par les technologies d'assistance et copiable via
                  // le presse-papier si l'utilisateur sait où regarder.
                },
                icon: const Icon(Icons.open_in_new_rounded, size: 16),
                label: Text(data.website!),
              ),
            ),
          ),
        ],
        const SizedBox(height: 32),
        Text(
          'Page publiée par son église · via Discipolat',
          textAlign: TextAlign.center,
          style: TextStyle(
            color: Colors.white.withValues(alpha: 0.35),
            fontSize: 11,
          ),
        ),
      ],
    );
  }

  Widget _logoFallback() {
    return Container(
      height: 96,
      width: 96,
      decoration: BoxDecoration(
        gradient: LinearGradient(
          colors: [AppColors.primary, AppColors.primaryLight],
          begin: Alignment.topLeft,
          end: Alignment.bottomRight,
        ),
        borderRadius: BorderRadius.circular(20),
      ),
      child: const Icon(Icons.church_outlined,
          color: Colors.white, size: 42),
    );
  }
}
