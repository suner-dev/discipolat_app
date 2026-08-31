import { useEffect } from 'react';
import { useQueryClient } from '@tanstack/react-query';
import { useAuth } from '@/contexts/AuthContext';
import { api } from '@/lib/api';

/**
 * Synchronisation temps réel (SSE) entre rôles.
 *
 * Se connecte au flux {@code /api/v1/events/entity-changes} poussé par le
 * backend (EntityPropagationPublisher). À chaque événement d'entité, on
 * INVALIDE les requêtes React Query concernées afin que tout utilisateur
 * connecté (quel que soit son rôle) voie immédiatement les données mises à
 * jour par les autres, sans rechargement manuel.
 *
 * Exemple : une âme créée par un faiseur → les pasteurs/responsables sur une
 * autre session reçoivent l'événement SOUL → leurs listes d'âmes se
 * rafraîchissent automatiquement.
 */

/** Correspondance entité backend → préfixes de queryKey React Query à invalider. */
const ENTITY_TO_QUERY_PREFIXES: Record<string, string[]> = {
  SOUL: ['souls', 'soul'],
  FAMILY: ['families', 'family'],
  USER: ['users', 'user'],
  PAYMENT_INTENT: ['payments'],
  EVENT: ['events', 'event'],
  VISIT: ['visits', 'visit'],
  PRAYER: ['prayers', 'prayer'],
  PRAYER_JOURNAL: ['prayer-journal'],
  DEPARTMENT: ['departments', 'department'],
  REPORT: ['reports', 'report'],
  MESSAGE: ['messages', 'message'],
  NOTIFICATION: ['notifications', 'notification'],
  ALERT: ['alerts', 'alert'],
  TRANSFER: ['transfers', 'transfer'],
  TRAINING: ['trainings', 'course'],
  OBJECTIVE: ['objectives', 'objective'],
  EVALUATION: ['evaluations'],
  DOCUMENT: ['documents'],
  TICKET: ['tickets'],
  SURVEY: ['surveys'],
};

function prefixesForEntity(entityType: string): string[] {
  const upper = entityType.toUpperCase();
  if (ENTITY_TO_QUERY_PREFIXES[upper]) return ENTITY_TO_QUERY_PREFIXES[upper];
  return [upper.toLowerCase()];
}

export function useRealtimeSync() {
  const { isAuthenticated } = useAuth();
  const queryClient = useQueryClient();

  useEffect(() => {
    if (!isAuthenticated) return;

    const token = localStorage.getItem('accessToken');
    if (!token) return;

    const sseUrl = `${api.defaults.baseURL}/events/entity-changes`;
    let active = true;
    let retryTimer: ReturnType<typeof setTimeout> | null = null;
    let controller: AbortController | null = null;

    const connect = async () => {
      controller = new AbortController();
      const signal = controller.signal;
      try {
        const response = await fetch(sseUrl, {
          headers: { Authorization: `Bearer ${token}` },
          signal,
        });
        if (!response.ok || !response.body) throw new Error(`SSE HTTP ${response.status}`);
        const reader = response.body.getReader();
        const decoder = new TextDecoder();
        let buffer = '';
        while (active) {
          const { done, value } = await reader.read();
          if (done) break;
          buffer += decoder.decode(value, { stream: true });
          // Découpage des événements SSE séparés par une ligne vide.
          const blocks = buffer.split('\n\n');
          buffer = blocks.pop() ?? '';
          for (const block of blocks) {
            if (!block) continue;
            let eventName = 'message';
            let data = '';
            for (const line of block.split('\n')) {
              if (line.startsWith('event:')) eventName = line.slice(6).trim();
              else if (line.startsWith('data:')) data += line.slice(5).trim();
            }
            if (eventName !== 'entity-change' || !data) continue;
            try {
              const change = JSON.parse(data);
              if (change?.entityType) {
                const prefixes = prefixesForEntity(change.entityType);
                for (const prefix of prefixes) {
                  queryClient.invalidateQueries({ queryKey: [prefix] });
                }
              }
            } catch {
              // événement non-JSON ignoré
            }
          }
        }
      } catch {
        // connexion perdue
      }
      if (active) {
        retryTimer = setTimeout(connect, 3000);
      }
    };

    connect();

    return () => {
      active = false;
      if (controller) controller.abort();
      if (retryTimer) clearTimeout(retryTimer);
    };
  }, [isAuthenticated, queryClient]);
}

/** Monte la synchronisation temps réel dans un composant de layout. */
export function RealtimeSync() {
  useRealtimeSync();
  return null;
}
