# DATA SYNCHRONIZATION AUDIT
**Date:** 2026-08-25

## MATRICE DE PROPAGATION DES DONNÉES

### MEMBRE CRÉÉ
| Destination | API Existe | Données Synchronisées | Statut |
|---|---|---|---|
| Dashboard Responsable | OUI | Vérifié | OK |
| Liste membres Responsable | OUI | Vérifié | OK |
| Dashboard Pasteur | OUI | Vérifié | OK |
| Dashboard Admin | OUI | Vérifié | OK |
| Département | OUI | M2M via member_departments | OK |
| Famille | OUI | Via family_id FK | OK |
| Faiseur | OUI | Via assigned_faiseur_id | OK |
| Statistiques | OUI | Via COUNT queries | OK |
| Recherche | OUI | Via SearchController | OK |
| Mobile | OUI | Même API REST | OK |
| Notifications | PARTIEL | Pas de notif auto à la création | AMÉLIORER |
| Historique | PARTIEL | Pas d'historique de création | AMÉLIORER |

### MEMBRE MODIFIÉ
| Destination | API Existe | Données Synchronisées | Statut |
|---|---|---|---|
| Toutes les vues | OUI | Même source DB | OK |
| SSE Propagation | OUI | EntityPropagationListener | OK |
| Cache invalidation | PARTIEL | Redis cache model | VÉRIFIER |

### MEMBRE TRANSFÉRÉ
| Destination | API Existe | Données Synchronisées | Statut |
|---|---|---|---|
| Ancien département | OUI | Via TransferWorkflowService | OK |
| Nouveau département | OUI | Via TransferWorkflowService | OK |
| Responsable ancien | OUI | Notification | OK |
| Responsable nouveau | OUI | Notification | OK |
| Pasteur | OUI | Historique transfert | OK |
| Historique | OUI | TransferHistory entity | OK |
| Statistiques | OUI | Recalcul | OK |

### DISCIPLE TRANSFÉRÉ
| Destination | API Existe | Données Synchronisées | Statut |
|---|---|---|---|
| Ancien faiseur | OUI | Via ReassignSoulRequest | OK |
| Nouveau faiseur | OUI | Via ReassignSoulRequest | OK |
| Famille | OUI | Via soul.department | OK |
| Statistiques | OUI | Recalcul | OK |
| Notifications | OUI | Notification push | OK |

### RAPPORT SOUMIS
| Destination | API Existe | Données Synchronisées | Statut |
|---|---|---|---|
| Responsable | OUI | Dashboard alert | OK |
| Chef de famille | OUI | Famille detail | OK |
| Historique | OUI | Report entity | OK |
| Notifications | OUI | Auto-notif | OK |
| Statistiques | OUI | Count | OK |

### ÉVÉNEMENT CRÉÉ
| Destination | API Existe | Données Synchronisées | Statut |
|---|---|---|---|
| Calendrier | OUI | Même DB | OK |
| Département | OUI | Via department_id | OK |
| Inscriptions | OUI | EventRegistration | OK |
| Notifications | OUI | Auto-notif | OK |
| Mobile | OUI | Même API | OK |
| Rappels | OUI | ScheduledJobs cron | OK |

### TRANSFERT DEMANDÉ
| Destination | API Existe | Données Synchronisées | Statut |
|---|---|---|---|
| Demandeur | OUI | Via transfer_requests | OK |
| Responsable cible | OUI | Notification | OK |
| Pasteur | OUI | Historique | OK |
| Admin | OUI | Dashboard | OK |
| Workflow | OUI | TransferWorkflowStep | OK |
| Mobile | OUI | Même API | OK |

---

## PROPAGATION SSE (Server-Sent Events)

| Module | SSE Actif | Écouteurs Frontend | Statut |
|---|---|---|---|
| EntityChangeBroadcaster | OUI | EntityPropagationListener | OK |
| EntityChangeSseController | OUI | /api/v1/events/changes | OK |
| Messages WebSocket | OUI | STOMP via WebSocketConfig | OK |
| Notifications push | OUI | Firebase Cloud Messaging | OK |

## PROBLÈMES DE SYNCHRONISATION IDENTIFIÉS

| ID | Description | Impact | Priorité |
|---|---|---|---|
| SYNC-001 | Pas de notification automatique lors de la création d'un membre | Le responsable ne voit pas le nouveau membre en temps réel | P2 |
| SYNC-002 | Pas d'historique de création d'entité (seulement modification) | Traçabilité incomplète | P2 |
| SYNC-003 | Cache Redis potentiellement non invalidé après modification | Données stale possibles | P2 |
| SYNC-004 | KPI dashboard calculés en temps réel (pas de cache) | Performance dégradée sous charge | P3 |
| SYNC-005 | Les 20 pages MOCK n'ont aucune synchronisation | Données figées | P0 |
| SYNC-006 | WorkflowConfigController utilise un Map statique in-memory | Données perdues au restart, partagées inter-tenant | P0 |
