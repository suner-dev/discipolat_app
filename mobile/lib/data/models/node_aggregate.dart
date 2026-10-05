/// Snapshot agrégé d'un sous-arbre (E) — §7.2.
///
/// LECTURE SEULE côté mobile (recalcul serveur, décision T-M0 : aucune écriture
/// offline sur `node_aggregate_snapshots`). Aucun PII nominatif côté plateforme
/// (D7) — uniquement des compteurs.
library;

class NodeAggregate {
  const NodeAggregate({
    required this.nodeId,
    required this.memberCount,
    required this.churchCount,
    required this.leaderCount,
    this.levelName,
    this.responsibleName,
    this.sermonCount = 0,
    this.prayerTopicCount = 0,
  });

  final String nodeId;
  final int memberCount;
  final int churchCount;
  final int leaderCount;
  final int sermonCount;
  final int prayerTopicCount;
  final String? levelName;
  final String? responsibleName;

  factory NodeAggregate.fromJson(Map<String, dynamic> json) => NodeAggregate(
        nodeId: json['nodeId'] as String? ?? '',
        memberCount: (json['memberCount'] as num?)?.toInt() ?? 0,
        churchCount: (json['churchCount'] as num?)?.toInt() ?? 0,
        leaderCount: (json['leaderCount'] as num?)?.toInt() ?? 0,
        sermonCount: (json['sermonCount'] as num?)?.toInt() ?? 0,
        prayerTopicCount: (json['prayerTopicCount'] as num?)?.toInt() ?? 0,
        levelName: json['levelName'] as String?,
        responsibleName: json['responsibleName'] as String?,
      );
}
