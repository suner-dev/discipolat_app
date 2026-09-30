/// Verrou asynchrone global de vidange de la file de synchronisation.
///
/// Deux moteurs drainent la même `SyncQueueTable` (SyncService — déclenché
/// par le listener de connectivité de main.dart — et OfflineSyncManager —
/// déclenché par son propre listener et le bouton du bandeau hors-ligne).
/// Sans exclusion croisée, les deux peuvent envoyer le MÊME item
/// simultanément → doubles POST (données de discipline/présences créées
/// deux fois côté serveur). Ce verrou sérialise les vidanges ; le second
/// moteur trouve alors une file vide et ne renvoie rien.
class SyncFlushLock {
  Future<void> _tail = Future<void>.value();

  /// Exécute [action] après toutes les actions précédemment en file.
  Future<T> run<T>(Future<T> Function() action) {
    final result = _tail.then((_) => action());
    // La chaîne survit à l'échec de l'action : une erreur ne doit pas
    // bloquer définitivement les vidanges suivantes.
    _tail = result.then((_) {}, onError: (_) {});
    return result;
  }
}

/// Instance globale partagée par les deux moteurs de sync.
final syncFlushLock = SyncFlushLock();
