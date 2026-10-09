import 'dart:async';

import 'package:dio/dio.dart';
import 'package:flutter/material.dart';

import '../../../data/services/api_service.dart';

/// LOT 1 §GLISE-D'ABORD (T1.6, mobile) — sélecteur d'église « église d'abord ».
///
/// <p>Version mobile du composant web `frontend/src/components/auth/ChurchPicker.tsx`
/// (T1.2). Elle en respecte le **contrat gelé** (§6 du plan, verrouillé par
/// `frontend/src__tests__churchesSuggestExistsContract.test.ts`) :
/// <ul>
///   <li><b>chemins strictement limités</b> : `/public/churches/suggest` et
///       `/public/churches/exists`, jamais rien d’autre ;</li>
///   <li><b>champs en liste blanche</b> : `total`, `items`, `name`, `slug`,
///       `city`, `country`, `found`. Tout identifiant technique, toute
///       coordonnéepersonnelle, tout descriptif large (nom de réseau,
///       site public, texte long, visuel, devise, compteur de fidèles,
///       etc.) est volontairement ABSENT du modèle Dart : la lecture
///       défensive `fromJson` ne garde QUE les 4 clés citées (R2 art. 9
///       RGPD, R3 anti-énumération) ;</li>
///   <li><b>le 404 ne se distingue pas</b> d’une église non listée : le
///       serveur répond `{found:false}` ou une liste vide dans les deux cas,
///       et l’UI reste muette sur la différence.</li>
/// </ul>
///
/// <p><b>Additif</b> (A1) : les 4 boutons existants de `RegisterScreen`
/// (« Créer mon église », « Rejoindre avec un code », « Invitation », « S'inscrire »)
/// restent en place ; ce picker s'AJOUTE en tête du formulaire quand aucun
/// contexte n'est présent dans l'URL. S'il y a `?joinCode=` ou `?church=` ou
/// `?mode=church` (les trois gestes déjà implémentés), l'appelant masque le
/// picker et le parcours reste identique à hier (A3/R7).</p>
///
/// <p><b>Non bloquant</b> (D5) : un rejet réseau, un quota dépassé, un 4xx
/// — le picker affiche un message discret et permet de continuer. Il ne
/// remplace PAS le formulaire, il le raccourcit.</p>
///
/// <p><b>Esthétique</b> : palette sombre cohérente avec `login_screen.dart` /
/// `register_screen.dart`. Aucun `applyBranding` global, aucune dépendance à
/// `TenantConfig.currentOrgId` (le picker fonctionne hors tenant, c'est une
/// porte d'entrée).</p>
enum ChurchPickerStatus { idle, searching, found, notFound, error }

/// Suggestion minimale retournée par `/public/churches/suggest`. Les champs
/// sont volontairement restreints à la liste blanche du §6.
class ChurchSuggestion {
  const ChurchSuggestion({
    required this.name,
    this.slug,
    this.city,
    this.country,
  });

  final String name;
  final String? slug;
  final String? city;
  final String? country;

  factory ChurchSuggestion.fromJson(Map<String, dynamic> json) {
    // Lecture défensive : on n'utilise QUE les 4 clés listées, même si le
    // serveur en renvoie d'autres par accident. Le contrat testé est une
    // liste blanche, pas une liste noire.
    return ChurchSuggestion(
      name: (json['name'] ?? '').toString(),
      slug: json['slug']?.toString(),
      city: json['city']?.toString(),
      country: json['country']?.toString(),
    );
  }

  /// Libellé secondaire sous le nom (ville, pays) — jamais de PII.
  String get locality {
    final parts = [
      if (city != null && city!.isNotEmpty) city!,
      if (country != null && country!.isNotEmpty) country!,
    ];
    return parts.join(', ');
  }
}

/// Label « texte brut » FR pour l'instant, cf. journal §8 (convention héritée
/// des écrans existants `login_screen.dart` / `register_screen.dart` qui
/// n'utilisaient pas `AppLocalizations` avant T1.6). Les 6 `.arb` de
/// `lib/l10n/` portent les mêmes clés pour préparer la migration.
class ChurchPickerLabels {
  const ChurchPickerLabels({
    this.label = 'Votre église',
    this.optional = '(facultatif)',
    this.placeholder = 'Commencez à taper le nom…',
    this.searching = 'Recherche en cours…',
    this.notFound = 'Aucune église publique ne correspond.',
    this.retry = 'Réessayer',
    this.clear = 'Effacer',
    this.networkError = 'Indisponible pour le moment — vous pouvez continuer sans choisir d\u2019église.',
    this.joinHint = 'Rejoindre avec un code',
    this.acceptHint = 'Accepter une invitation',
    this.createHint = 'Créer mon église',
    this.resultsSemantics = 'Suggestions d\u00e9glises',
  });

  final String label;
  final String optional;
  final String placeholder;
  final String searching;
  final String notFound;
  final String retry;
  final String clear;
  final String networkError;
  final String joinHint;
  final String acceptHint;
  final String createHint;
  final String resultsSemantics;
}

typedef ChurchPickerSelect = void Function(String name, String? slug);
typedef ChurchPickerNotFound = void Function(String typedName);

/// Sélecteur d'église (T1.6, mobile).
///
/// Utilisé par `RegisterScreen` (branchement conditionnel) et `LoginScreen`
/// (D5, mode facultatif/compact) ; les deux hôtes existants restent intacts.
class ChurchPicker extends StatefulWidget {
  const ChurchPicker({
    super.key,
    this.apiService,
    this.onSelect,
    this.onNotFound,
    this.compact = false,
    this.showNotFoundCtas = true,
    this.labels = const ChurchPickerLabels(),
    this.debounce = const Duration(milliseconds: 300),
    this.minLength = 2,
  });

  final ApiService? apiService;
  final ChurchPickerSelect? onSelect;
  final ChurchPickerNotFound? onNotFound;
  final bool compact;
  final bool showNotFoundCtas;
  final ChurchPickerLabels labels;
  final Duration debounce;
  final int minLength;

  @override
  State<ChurchPicker> createState() => _ChurchPickerState();
}

class _ChurchPickerState extends State<ChurchPicker> {
  late final ApiService _api;
  final TextEditingController _controller = TextEditingController();
  final FocusNode _focusNode = FocusNode();
  Timer? _debounce;
  int _requestSeq = 0;

  ChurchPickerStatus _status = ChurchPickerStatus.idle;
  List<ChurchSuggestion> _items = const [];
  String _errorMsg = '';
  bool _open = false;

  @override
  void initState() {
    super.initState();
    _api = widget.apiService ?? ApiService();
  }

  @override
  void dispose() {
    _debounce?.cancel();
    _controller.dispose();
    _focusNode.dispose();
    super.dispose();
  }

  /// Debounce + séquentialisation : si l'utilisateur continue à taper, la
  /// réponse de la requête précédente est ignorée (`_requestSeq`).
  void _scheduleSearch(String raw) {
    _debounce?.cancel();
    final q = raw.trim();
    if (q.length < widget.minLength) {
      setState(() {
        _status = ChurchPickerStatus.idle;
        _items = const [];
        _open = false;
        _errorMsg = '';
      });
      return;
    }
    setState(() => _status = ChurchPickerStatus.searching);
    _debounce = Timer(widget.debounce, () => _runSuggest(q));
  }

  Future<void> _runSuggest(String q) async {
    final seq = ++_requestSeq;
    try {
      final response = await _api.get(
        '/public/churches/suggest',
        params: {'q': q},
      );
      if (!mounted || seq != _requestSeq) return;
      final body = response.data;
      final List<ChurchSuggestion> parsed;
      if (body is Map && body['items'] is List) {
        parsed = (body['items'] as List)
            .whereType<Map>()
            .map((m) => ChurchSuggestion.fromJson(m.cast<String, dynamic>()))
            .toList(growable: false);
      } else {
        parsed = const [];
      }
      setState(() {
        _items = parsed;
        _status = parsed.isEmpty
            ? ChurchPickerStatus.notFound
            : ChurchPickerStatus.found;
        _open = parsed.isNotEmpty;
      });
    } on DioException catch (err) {
      if (!mounted || seq != _requestSeq) return;
      // Quota / réseau / 5xx : on reste non-bloquant.
      setState(() {
        _status = ChurchPickerStatus.error;
        _items = const [];
        _open = false;
        _errorMsg = err.message ?? 'Erreur réseau';
      });
    } catch (_) {
      if (!mounted || seq != _requestSeq) return;
      setState(() {
        _status = ChurchPickerStatus.error;
        _items = const [];
        _open = false;
        _errorMsg = 'Erreur inattendue';
      });
    }
  }

  /// Entrée clavier sans suggestion active : confirme le nom exact via
  /// `/public/churches/exists`. Réponse identique qu'une église fantôme ou
  /// non listée → jamais d'oracle d'existence.
  Future<void> _confirmExact() async {
    final q = _controller.text.trim();
    if (q.isEmpty) return;
    if (_status == ChurchPickerStatus.found && _items.isNotEmpty) {
      // L'utilisateur a tape exactement le premier resultat : on le prend.
      _choose(_items.first);
      return;
    }
    try {
      final response = await _api.get(
        '/public/churches/exists',
        params: {'q': q},
      );
      if (!mounted) return;
      final body = response.data;
      final found = body is Map && body['found'] == true;
      if (found) {
        final slug = (body['slug'] as String?);
        final name = (body['name'] as String?) ?? q;
        _choose(ChurchSuggestion(name: name, slug: slug));
      } else {
        setState(() {
          _status = ChurchPickerStatus.notFound;
          _open = false;
        });
        widget.onNotFound?.call(q);
      }
    } on DioException {
      if (!mounted) return;
      setState(() {
        _status = ChurchPickerStatus.error;
        _open = false;
        _errorMsg = 'Indisponible';
      });
    } catch (_) {
      if (!mounted) return;
      setState(() {
        _status = ChurchPickerStatus.error;
        _open = false;
      });
    }
  }

  void _choose(ChurchSuggestion item) {
    setState(() {
      _controller.value = TextEditingValue(
        text: item.name,
        selection: TextSelection.collapsed(offset: item.name.length),
      );
      _status = ChurchPickerStatus.found;
      _open = false;
    });
    widget.onSelect?.call(item.name, item.slug);
  }

  void _reset() {
    _debounce?.cancel();
    setState(() {
      _controller.clear();
      _status = ChurchPickerStatus.idle;
      _items = const [];
      _open = false;
      _errorMsg = '';
    });
    _focusNode.requestFocus();
  }

  @override
  Widget build(BuildContext context) {
    final showList = _open && _items.isNotEmpty;
    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      mainAxisSize: MainAxisSize.min,
      children: [
        _buildHeader(),
        const SizedBox(height: 6),
        _buildInput(),
        if (showList) _buildSuggestionList(),
        if (_status == ChurchPickerStatus.notFound && !showList)
          _buildNotFoundSlot(),
        if (_status == ChurchPickerStatus.error)
          _buildErrorSlot(),
      ],
    );
  }

  Widget _buildHeader() {
    return Row(
      children: [
        Flexible(
          child: Text(
            widget.labels.label,
            style: TextStyle(
              color: Colors.white.withValues(alpha: 0.85),
              fontSize: widget.compact ? 13 : 14,
              fontWeight: FontWeight.w500,
            ),
          ),
        ),
        const SizedBox(width: 6),
        Text(
          widget.labels.optional,
          style: TextStyle(
            color: Colors.white.withValues(alpha: 0.45),
            fontSize: 12,
          ),
        ),
      ],
    );
  }

  Widget _buildInput() {
    return TextField(
      controller: _controller,
      focusNode: _focusNode,
      onChanged: _scheduleSearch,
      onSubmitted: (_) => _confirmExact(),
      textInputAction: TextInputAction.search,
      style: const TextStyle(color: Colors.white),
      cursorColor: const Color(0xFF7C5CFF),
      decoration: InputDecoration(
        hintText: widget.labels.placeholder,
        hintStyle: TextStyle(color: Colors.white.withValues(alpha: 0.35)),
        prefixIcon: Icon(
          _status == ChurchPickerStatus.searching
              ? Icons.hourglass_top
              : Icons.search,
          color: Colors.white.withValues(alpha: 0.55),
        ),
        suffixIcon: _controller.text.isNotEmpty
            ? IconButton(
                icon: Icon(Icons.close,
                    color: Colors.white.withValues(alpha: 0.55)),
                onPressed: _reset,
                tooltip: widget.labels.clear,
              )
            : null,
        filled: true,
        fillColor: Colors.white.withValues(alpha: 0.06),
        contentPadding: EdgeInsets.symmetric(
          horizontal: 14,
          vertical: widget.compact ? 10 : 14,
        ),
        border: OutlineInputBorder(
          borderRadius: BorderRadius.circular(14),
          borderSide: BorderSide(color: Colors.white.withValues(alpha: 0.10)),
        ),
        enabledBorder: OutlineInputBorder(
          borderRadius: BorderRadius.circular(14),
          borderSide: BorderSide(color: Colors.white.withValues(alpha: 0.10)),
        ),
        focusedBorder: OutlineInputBorder(
          borderRadius: BorderRadius.circular(14),
          borderSide: const BorderSide(color: Color(0xFF7C5CFF), width: 1.4),
        ),
      ),
    );
  }

  Widget _buildSuggestionList() {
    return Container(
      margin: const EdgeInsets.only(top: 6),
      decoration: BoxDecoration(
        color: const Color(0xFF1E2A4A),
        borderRadius: BorderRadius.circular(14),
        border: Border.all(color: Colors.white.withValues(alpha: 0.10)),
      ),
      child: ConstrainedBox(
        constraints: const BoxConstraints(maxHeight: 240),
        child: Scrollbar(
          child: ListView.separated(
            shrinkWrap: true,
            padding: EdgeInsets.zero,
            itemCount: _items.length,
            separatorBuilder: (_, __) => Divider(
              height: 1,
              color: Colors.white.withValues(alpha: 0.06),
            ),
            itemBuilder: (context, index) {
              final item = _items[index];
              final subtitle = item.locality;
              return ListTile(
                dense: widget.compact,
                leading: const Icon(Icons.church_outlined,
                    color: Color(0xFF7C5CFF)),
                title: Text(
                  item.name,
                  style: const TextStyle(color: Colors.white),
                ),
                subtitle: subtitle.isEmpty
                    ? null
                    : Text(
                        subtitle,
                        style: TextStyle(
                            color: Colors.white.withValues(alpha: 0.55)),
                      ),
                onTap: () => _choose(item),
              );
            },
          ),
        ),
      ),
    );
  }

  Widget _buildNotFoundSlot() {
    return Padding(
      padding: const EdgeInsets.only(top: 6),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              const Icon(Icons.info_outline,
                  size: 16, color: Colors.amberAccent),
              const SizedBox(width: 6),
              Expanded(
                child: Text(
                  widget.labels.notFound,
                  style: TextStyle(
                      color: Colors.white.withValues(alpha: 0.8),
                      fontSize: 13),
                ),
              ),
            ],
          ),
          if (widget.showNotFoundCtas) ...[
            const SizedBox(height: 8),
            Wrap(
              spacing: 8,
              runSpacing: 8,
              children: [
                _ChipCta(label: widget.labels.joinHint, semantic: 'join'),
                _ChipCta(
                    label: widget.labels.acceptHint,
                    semantic: 'accept-invitation'),
                _ChipCta(
                    label: widget.labels.createHint, semantic: 'create-church'),
              ],
            ),
          ],
        ],
      ),
    );
  }

  Widget _buildErrorSlot() {
    final detail = _errorMsg.isEmpty ? '' : ' — $_errorMsg';
    return Padding(
      padding: const EdgeInsets.only(top: 6),
      child: Row(
        children: [
          Icon(Icons.wifi_off,
              size: 16, color: Colors.white.withValues(alpha: 0.55)),
          const SizedBox(width: 6),
          Expanded(
            child: Text(
              '${widget.labels.networkError}$detail',
              style: TextStyle(
                  color: Colors.white.withValues(alpha: 0.6), fontSize: 12),
            ),
          ),
          TextButton(
            onPressed: () => _runSuggest(_controller.text.trim()),
            child: Text(widget.labels.retry),
          ),
        ],
      ),
    );
  }
}

/// Chip purement visuel — le branchement de navigation est laissé à l'hôte
/// (`RegisterScreen`/`LoginScreen`) pour ne PAS dupliquer la logique de
/// GoRouter déjà en place (A1/A3). L'hôte peut intercepter via un `Semantics`
/// identifier ou brancher un `onTap` en étendant le widget ; le picker n'est
/// qu'un raccourci.
class _ChipCta extends StatelessWidget {
  const _ChipCta({required this.label, required this.semantic});
  final String label;
  final String semantic;

  @override
  Widget build(BuildContext context) {
    return Semantics(
      identifier: 'churchPicker.cta.$semantic',
      button: true,
      child: Container(
        padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
        decoration: BoxDecoration(
          borderRadius: BorderRadius.circular(14),
          border: Border.all(color: Colors.white.withValues(alpha: 0.16)),
          color: Colors.white.withValues(alpha: 0.04),
        ),
        child: Text(
          label,
          style: TextStyle(
            color: Colors.white.withValues(alpha: 0.85),
            fontSize: 12,
          ),
        ),
      ),
    );
  }
}
