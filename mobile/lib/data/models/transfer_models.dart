/// Modèles du transfert de membre — SPEC_ORGANISATION_DENOMINATION_V2 §4.4 / T-M1.
///
/// Miroir strict du contrat `GET /api/v1/tenant/transfer/preview` et
/// `POST /api/v1/tenant/transfer`. Les analyser dans un seul endroit évite que
/// chaque écran interprète `willTransfer` ou `status` différemment — ce qui
/// produirait exactement le reproche du client : annoncer « vous rejoignez »
/// quand l'historique pastoral est en réalité préservé.

/// Ce qu'il faut savoir avant de confirmer le transfert.
class TransferPreview {
  const TransferPreview({
    required this.sameNetwork,
    required this.alreadyMember,
    required this.activeInOther,
    required this.fromChurch,
    required this.toChurch,
    required this.toKind,
    required this.willTransfer,
  });

  /// Même `root_tenant_id` (§4.4 cas 1) → transfert, parcours préservé.
  final bool sameNetwork;

  /// Déjà actif dans l'organisation cible : rien à faire.
  final bool alreadyMember;

  /// Membre d'une AUTRE organisation → l'adhésion d'origine reste intacte.
  final bool activeInOther;

  final String fromChurch;
  final String toChurch;
  final String toKind;

  /// Raccourci serveur : la conversation doit-elle parler de transfert ?
  final bool willTransfer;

  static const empty = TransferPreview(
    sameNetwork: false,
    alreadyMember: false,
    activeInOther: false,
    fromChurch: '',
    toChurch: '',
    toKind: '',
    willTransfer: false,
  );
}

class TransferOutcome {
  const TransferOutcome({
    required this.status,
    required this.toChurch,
    this.accessToken,
    this.refreshToken,
  });

  /// `TRANSFERRED`, `JOINED` ou `ALREADY_MEMBER`.
  final String status;
  final String toChurch;

  /// Jeton réémis sur l'organisation d'accueil (T-B0bis). Absent si aucune
  /// bascule n'a eu lieu.
  final String? accessToken;
  final String? refreshToken;

  bool get wasTransferred => status == 'TRANSFERRED';
  bool get wasJoined => status == 'JOINED';
  bool get alreadyMember => status == 'ALREADY_MEMBER';
}
