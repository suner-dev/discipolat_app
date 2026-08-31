/// Model for Testimony entity (backend TestimonyController /api/v1/testimonies).
class Testimony {
  final String id;
  final String titre;
  final String contenu;
  final String categorie;
  final String statut;
  final String auteurId;
  final int likes;
  final int commentaires;
  final DateTime createdAt;

  Testimony({
    required this.id,
    required this.titre,
    required this.contenu,
    required this.categorie,
    required this.statut,
    required this.auteurId,
    required this.likes,
    required this.commentaires,
    required this.createdAt,
  });

  factory Testimony.fromJson(Map<String, dynamic> json) => Testimony(
        id: (json['id'] ?? '').toString(),
        titre: (json['titre'] ?? json['title'] ?? '').toString(),
        contenu: (json['contenu'] ?? json['content'] ?? '').toString(),
        categorie: (json['categorie'] ?? json['category'] ?? 'AUTRE').toString(),
        statut: (json['statut'] ?? json['status'] ?? 'EN_ATTENTE').toString(),
        auteurId: (json['auteurId'] ?? json['auteur_id'] ?? '').toString(),
        likes: json['likes'] is int ? json['likes'] as int : int.tryParse('${json['likes']}') ?? 0,
        commentaires: json['commentaires'] is int ? json['commentaires'] as int : int.tryParse('${json['commentaires']}') ?? 0,
        createdAt: DateTime.tryParse(json['createdAt']?.toString() ?? '') ?? DateTime.now(),
      );
}
