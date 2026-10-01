import { useEffect } from 'react';
import { Client } from '@stomp/stompjs';
import { useQueryClient, useQuery } from '@tanstack/react-query';
import toast from 'react-hot-toast';
import api from '@/lib/api';
import { useAuth } from '@/contexts/AuthContext';
import { useTenantOptional } from '@/contexts/TenantContext';
import { tText } from '@/i18n';

/**
 * §G5.8 — Bus temps réel WEB : abonné au « firehose » du tenant
 * `/topic/tenant:{id}/events` (push outbox immédiat, garantie < 5 s).
 *
 * Chaque événement invalide les requêtes React Query concernées : ce qui est
 * modifié sur un client (web OU mobile, file hors-ligne §G5.7 comprise) se
 * reflète immédiatement sur tous les écrans actifs — config, membres, rôles,
 * dress codes, tâches, événements, inventaire…
 *
 * Contrôles (§1129) :
 * - déduplication par `eventId` monotone (BIGSERIAL de l'outbox) ;
 * - trou de delta (reconnexion après coupure) → rafraîchissement TOTAL ;
 * - indicateur de fraîcheur : query ['realtime','status'] (connected,
 *   lastEventAt) consommée par le badge du Navbar.
 */

/** eventType outbox → préfixes de queryKey à invalider. */
const EVENT_QUERY_PREFIXES: Record<string, string[]> = {
  // Membres / âmes / parcours
  MemberRegistered: ['souls', 'members', 'directory', 'dashboard', 'feed'],
  MemberTransferred: ['souls', 'members', 'org', 'dashboard'],
  JourneyStageChanged: ['souls', 'journey', 'space'],
  MentorAssigned: ['souls', 'mentoring', 'space'],
  InvitationAccepted: ['invitations', 'members', 'dashboard'],
  // Présence / événements
  AttendanceRecorded: ['attendance', 'presence', 'space', 'dashboard', 'reports'],
  EventCreated: ['events', 'space', 'dashboard', 'feed'],
  EventDeleted: ['events', 'space', 'dashboard'],
  SermonPublished: ['sermons', 'events', 'media'],
  // Dress codes (§G3.4)
  DressCodePublished: ['dress-codes', 'dresscodes', 'events', 'space'],
  // Inventaire / assets (§G3.5, mobile §G5.6)
  AssetCheckedOut: ['assets', 'inventory', 'space', 'checkouts'],
  AssetReturned: ['assets', 'inventory', 'space', 'checkouts'],
  AssetDamaged: ['assets', 'inventory', 'finance', 'maintenance'],
  MaintenanceStarted: ['maintenance', 'assets', 'finance'],
  MaintenanceCompleted: ['maintenance', 'assets'],
  StockLowAlert: ['inventory', 'stock', 'alerts'],
  // Santé (§G3.11)
  HealthConsultationCreated: ['health', 'space'],
  HealthReferralCreated: ['health', 'alerts', 'space'],
  MedicineExpiring: ['health', 'inventory', 'alerts'],
  PrescriptionIssued: ['health'],
  // Campagnes / kits
  CampaignStarted: ['campaigns', 'health', 'dashboard'],
  KitDistributed: ['campaigns', 'inventory', 'health'],
  // Rôles & config (rares → rafraîchissement large volontaire)
  RoleAssigned: ['roles', 'members', 'org', 'space', 'bootstrap'],
  RoleEnded: ['roles', 'members', 'org'],
  // §G4.3/G4.4 — pastorat : mandats, organigramme, Space OS et écrans famille recablés.
  PastorAppointed: ['roles', 'members', 'org', 'families', 'pastorate', 'family-os', 'dashboard'],
  PastorEnded: ['roles', 'members', 'org', 'pastorate', 'family-os', 'dashboard'],
  // §G4.2 — le chef de famille ajoute une âme (web émetteur + autres clients).
  FamilyMemberAdded: ['family-os', 'souls', 'family', 'members', 'dashboard', 'feed'],
  // §G4.4 « rôles vivants » : permission_version bumpée → TOUT est re-résolu.
  PermissionsChanged: [],
  SpaceConfigChanged: [],
  StatusChanged: ['statuses', 'space', 'bootstrap'],
  // Tâches / finance / divers
  TaskAssigned: ['tasks', 'space', 'dashboard', 'feed'],
  TaskCompleted: ['tasks', 'space', 'dashboard', 'feed'],
  ExpenseCreated: ['finance', 'expenses', 'space'],
  LegacyMigrationCompleted: ['migration', 'space'],
};

export interface RealtimeStatus {
  connected: boolean;
  lastEventAt: string | null;
  lastEventType: string | null;
  fullRefreshes: number;
}

const STATUS_KEY = ['realtime', 'status'];

function brokerUrl(): string {
  // baseURL = '/api/v1' (dev, proxy Vite ws:true) ou 'https://api…/api/v1' (prod).
  // Le backend expose /ws-church en WebSocket NATIF (registerStompEndpoints).
  const base = (api.defaults.baseURL as string) || '/api/v1';
  if (/^https?:\/\//i.test(base)) {
    const u = new URL(base);
    return `${u.protocol === 'https:' ? 'wss' : 'ws'}://${u.host}/ws-church`;
  }
  const proto = typeof window !== 'undefined' && window.location.protocol === 'https:' ? 'wss' : 'ws';
  return `${proto}://${window.location.host}/ws-church`;
}

export function useRealtimeBus() {
  const { isAuthenticated, user } = useAuth();
  const tenant = useTenantOptional();
  const currentTenant = tenant?.currentTenant ?? null;
  const refreshContext = tenant?.refreshContext;
  const queryClient = useQueryClient();
  const tenantId = currentTenant?.id ?? null;

  useEffect(() => {
    const token = localStorage.getItem('accessToken');
    if (!isAuthenticated || !tenantId || !token) return;

    let lastEventId = -1;
    let fullRefreshes = 0;

    /**
     * §G4.4 « rôles vivants » — composant « Ce qui a changé pour vous » :
     * le serveur a bumpé permission_version → on re-résout le contexte
     * (TenantContext fusionne GET /me/permissions résolu serveur, pastorate
     * inclus) et l'utilisateur est notifié. Objectif de service < 5 s.
     */
    const notifyPermissionsChanged = () => {
      if (refreshContext) void refreshContext().catch(() => undefined);
      toast.success(tText('Ce qui a changé pour vous : vos permissions et menus ont été mis à jour'), {
        id: 'permissions-changed',
      });
    };

    const publishStatus = (partial: Partial<RealtimeStatus>) => {
      const prev = queryClient.getQueryData<RealtimeStatus>(STATUS_KEY);
      const next = { connected: false, lastEventAt: null, lastEventType: null, fullRefreshes: 0, ...prev, ...partial };
      lastPublishedStatus = next;
      queryClient.setQueryData(STATUS_KEY, next);
    };

    const invalidateForEvent = (eventType: string) => {
      const known = Object.prototype.hasOwnProperty.call(EVENT_QUERY_PREFIXES, eventType);
      const prefixes = known ? EVENT_QUERY_PREFIXES[eventType] : null;
      if (!known || prefixes === null || prefixes.length === 0) {
        // Type inconnu ou purement configuration → tout recharger, jamais de dato stale.
        queryClient.invalidateQueries();
        return;
      }
      for (const prefix of prefixes) {
        queryClient.invalidateQueries({ queryKey: [prefix] });
      }
    };

    const client = new Client({
      brokerURL: brokerUrl(),
      connectHeaders: { token, Authorization: `Bearer ${token}` },
      reconnectDelay: 3000,
      heartbeatIncoming: 10000,
      heartbeatOutgoing: 10000,
      onConnect: () => {
        publishStatus({ connected: true });
        client.subscribe(`/topic/tenant:${tenantId}/events`, (frame) => {
          try {
            const evt = JSON.parse(frame.body);
            const id = Number(evt.eventId);
            if (Number.isFinite(id)) {
              if (id <= lastEventId) return; // rejeu/doublon
              if (lastEventId >= 0 && id > lastEventId + 1) {
                // TROU de delta (coupure réseau) → rafraîchissement total.
                queryClient.invalidateQueries();
                fullRefreshes += 1;
              }
              lastEventId = id;
            }
            invalidateForEvent(String(evt.eventType ?? ''));
            // §G4.4 — rôles vivants : toast ciblé « Ce qui a changé pour vous ».
            if (String(evt.eventType ?? '') === 'PermissionsChanged') {
              const targetUser = evt.payload?.userId as string | undefined;
              if (!targetUser || targetUser === user?.id) notifyPermissionsChanged();
            }
            publishStatus({
              connected: true,
              lastEventAt: String(evt.timestamp ?? new Date().toISOString()),
              lastEventType: String(evt.eventType ?? ''),
              fullRefreshes,
            });
          } catch {
            // frame non-JSON ignorée
          }
        });
      },
      onDisconnect: () => publishStatus({ connected: false }),
      onWebSocketError: () => publishStatus({ connected: false }),
      onStompError: () => publishStatus({ connected: false }),
    });

    client.activate();
    return () => {
      client.deactivate();
      publishStatus({ connected: false });
    };
  }, [isAuthenticated, tenantId, queryClient, user?.id, refreshContext]);
}

/** Composant de montage (MainLayout) — rend null, branche le bus. */
export function RealtimeBus() {
  useRealtimeBus();
  return null;
}

/** Lecture réactive de l'indicateur de fraîcheur (badge du Navbar). */
export function useRealtimeStatus(): RealtimeStatus | undefined {
  // La valeur est POUSSÉE par le bus (setQueryData) — jamais auto-fetchée.
  const { data } = useQuery({
    queryKey: STATUS_KEY,
    queryFn: () => queryClientFallback(),
    staleTime: Infinity,
  });
  return data;
}

// queryFn de secours : lit la dernière valeur publiée (ex. après remontage).
let lastPublishedStatus: RealtimeStatus | undefined;
function queryClientFallback(): RealtimeStatus {
  return lastPublishedStatus ?? { connected: false, lastEventAt: null, lastEventType: null, fullRefreshes: 0 };
}

