/// « Mon parcours pastoral » (mobile) — le RÉCIT d'un membre dans le temps.
///
/// Reflet de `frontend/src/components/relations/ParcoursCard.tsx` : la section
/// du PROFIL charge son PROPRE endpoint `GET /hierarchy/me/parcours` — vue en
/// LECTURE SEULE composée côté serveur à partir des seules tables déjà
/// persistées (adhésions, transferts, capacités, rattachements). Additif strict :
/// aucune écriture, aucune nouvelle table, aucune altération d'une vue existante.
///
/// Calqué sur `hierarchy_card.dart` (MyHierarchyCard) : chargement isolé,
/// repli silencieux quand il n'y a rien à raconter, message d'erreur explicite
/// quand la brique tombe — jamais un vide qui ferait croire à « aucun parcours ».
library;

import 'package:flutter/material.dart';

import '../../l10n/app_localizations.dart';
import '../../data/services/api_service.dart';
import '../../data/services/relation_service.dart';
import 'hierarchy_card.dart' show GlassCardLike;

/// Une ligne du récit, telle que produite par le serveur :
/// `horodatage` (ISO) / `type` / `action` / `libelle` / `details`.
class ParcoursEvent {
  const ParcoursEvent({
    this.date,
    this.type = '',
    this.action = '',
    this.libelle = '',
  });

  final DateTime? date;
  final String type;
  final String action;
  final String libelle;

  bool get isFin => action == 'FIN';

  factory ParcoursEvent.fromJson(Map<String, dynamic> json) {
    final raw = json['horodatage'];
    return ParcoursEvent(
      date: raw == null ? null : DateTime.tryParse('$raw'),
      type: '${json['type'] ?? ''}',
      action: '${json['action'] ?? ''}',
      libelle: '${json['libelle'] ?? ''}',
    );
  }
}

/// Parsing isolé (testable sans service HTTP) : une réponse dégradée donne une
/// liste vide, jamais une exception.
class ParcoursParsing {
  const ParcoursParsing._();

  static List<ParcoursEvent> eventsOf(Map<String, dynamic> parcours) {
    final raw = parcours['evenements'];
    if (raw is! List) return const [];
    return raw
        .whereType<Map<String, dynamic>>()
        .map(ParcoursEvent.fromJson)
        .toList();
  }

  static Map<String, dynamic> resumeOf(Map<String, dynamic> parcours) {
    final raw = parcours['resume'];
    return raw is Map<String, dynamic> ? raw : const {};
  }

  static int intOf(Map<String, dynamic> map, String key) {
    final v = map[key];
    return v is num ? v.toInt() : 0;
  }
}

/// « Mon parcours pastoral » — section du PROFIL.
class MyParcoursCard extends StatefulWidget {
  const MyParcoursCard({super.key, this.apiService});

  final ApiService? apiService;

  @override
  State<MyParcoursCard> createState() => _MyParcoursCardState();
}

class _MyParcoursCardState extends State<MyParcoursCard> {
  late final RelationService _service =
      RelationService(widget.apiService ?? ApiService());
  Map<String, dynamic> _parcours = const {};
  bool _loading = true;
  bool _error = false;

  @override
  void initState() {
    super.initState();
    _load();
  }

  Future<void> _load() async {
    setState(() {
      _loading = true;
      _error = false;
    });
    try {
      final p = await _service.myParcours();
      if (!mounted) return;
      setState(() {
        _parcours = p;
        _loading = false;
      });
    } catch (_) {
      if (!mounted) return;
      setState(() {
        _loading = false;
        _error = true;
      });
    }
  }

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context);
    if (_loading) {
      return const GlassCardLike(
        child: Padding(
          padding: EdgeInsets.all(8),
          child: Center(child: CircularProgressIndicator()),
        ),
      );
    }
    if (_error) {
      return GlassCardLike(
        child: Row(
          children: [
            Expanded(
              child: Text(l10n.parcoursUnavailable,
                  style: TextStyle(color: Colors.white.withValues(alpha: 0.6))),
            ),
            TextButton(onPressed: _load, child: Text(l10n.relationsRetry)),
          ],
        ),
      );
    }
    return ParcoursCard(parcours: _parcours);
  }
}

/// Rendu du récit — widget pur, testable en isolant le parsing de HTTP.
class ParcoursCard extends StatelessWidget {
  const ParcoursCard({super.key, required this.parcours});

  final Map<String, dynamic> parcours;

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context);
    final events = ParcoursParsing.eventsOf(parcours);
    // Dégradation silencieuse : l'absence de récit est un état normal (nouveau
    // membre, backend sans la brique) — jamais un bloc vide ni une erreur.
    if (events.isEmpty) return const SizedBox.shrink();

    final resume = ParcoursParsing.resumeOf(parcours);
    final transferts = ParcoursParsing.intOf(resume, 'transferts');
    final ruptures = ParcoursParsing.intOf(resume, 'rattachementsRompus');
    final depuis = DateTime.tryParse('${parcours['depuis'] ?? ''}');

    final white70 = Colors.white.withValues(alpha: 0.7);
    final white40 = Colors.white.withValues(alpha: 0.4);

    return GlassCardLike(
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              const Icon(Icons.timeline,
                  color: Colors.pinkAccent, size: 20),
              const SizedBox(width: 8),
              Expanded(
                child: Text(l10n.parcoursTitle,
                    style: const TextStyle(
                        color: Colors.white,
                        fontSize: 14,
                        fontWeight: FontWeight.w600)),
              ),
            ],
          ),
          const SizedBox(height: 10),
          if (depuis != null) ...[
            Text(l10n.parcoursSince(_formatDate(depuis)),
                style: TextStyle(color: white70, fontSize: 11)),
            const SizedBox(height: 8),
          ],
          ...events.map((e) => _EventRow(event: e)),
          if (transferts > 0 || ruptures > 0) ...[
            const SizedBox(height: 8),
            Text(
              [
                if (transferts > 0) l10n.parcoursTransferts(transferts),
                if (ruptures > 0) l10n.parcoursRuptures(ruptures),
              ].join(' · '),
              style: TextStyle(color: white40, fontSize: 10),
            ),
          ],
        ],
      ),
    );
  }
}

/// Une ligne du récit : icône par nature, libellé traduit, date bornée.
class _EventRow extends StatelessWidget {
  const _EventRow({required this.event});

  final ParcoursEvent event;

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context);
    final meta = _typeVisual(event.type);
    final date = event.date == null ? null : _formatDate(event.date!);
    final line = [
      _typeLabel(l10n, event.type),
      event.isFin ? l10n.parcoursActionFin : l10n.parcoursActionDebut,
      if (date != null) date,
    ].join(' · ');

    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 3),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Icon(meta.$1, size: 14, color: meta.$2),
          const SizedBox(width: 6),
          Expanded(
            child: Text(line,
                style: TextStyle(
                    color: Colors.white.withValues(alpha: 0.75), fontSize: 11)),
          ),
        ],
      ),
    );
  }
}

/// Libellé localisé d'une nature d'événement — le serveur renvoie le CODE
/// technique (ADHESION | TRANSFERT | CAPACITE | RATTACHEMENT).
String _typeLabel(AppLocalizations l10n, String code) {
  switch (code) {
    case 'ADHESION':
      return l10n.parcoursTypeAdhesion;
    case 'TRANSFERT':
      return l10n.parcoursTypeTransfert;
    case 'CAPACITE':
      return l10n.parcoursTypeCapacite;
    case 'RATTACHEMENT':
      return l10n.parcoursTypeRattachement;
    default:
      return code;
  }
}

/// Icône et teinte par nature — même vocabulaire visuel que le web.
(IconData, Color) _typeVisual(String code) {
  switch (code) {
    case 'ADHESION':
      return (Icons.person_add_alt, Colors.greenAccent);
    case 'TRANSFERT':
      return (Icons.swap_horiz, Colors.amberAccent);
    case 'CAPACITE':
      return (Icons.emoji_events, Colors.lightBlueAccent);
    case 'RATTACHEMENT':
      return (Icons.favorite, Colors.pinkAccent);
    default:
      return (Icons.timeline, Colors.white54);
  }
}

/// Date courte et déterministe (aaaamm-mm-jj → jj/mm/aaaa) — le serveur envoie
/// de l'ISO horodaté ; l'heure n'a aucun sens pastoral dans un récit.
String _formatDate(DateTime d) {
  final dd = d.day.toString().padLeft(2, '0');
  final mm = d.month.toString().padLeft(2, '0');
  return '$dd/$mm/${d.year}';
}
