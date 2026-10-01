import { describe, it, expect, vi, beforeEach } from 'vitest';
import { renderHook, waitFor } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import type { ReactNode } from 'react';

/**
 * §G5.8 — Bus temps réel web : chaque événement du firehose outbox doit
 * invalider les requêtes React Query concernées ( écrans actifs re-rendus
 * < 5 s), avec déduplication par eventId monotone et RAFRAÎCHISSEMENT TOTAL
 * quand un trou de delta est détecté (coupure réseau entre deux frames).
 */

const { subscriptions, clients } = vi.hoisted(() => ({
  subscriptions: [] as { dest: string; cb: (frame: { body: string }) => void }[],
  clients: [] as any[],
}));

vi.mock('@stomp/stompjs', () => ({
  Client: class FakeClient {
    config: any;
    constructor(config: any) {
      this.config = config;
      clients.push(this);
    }
    activate() {}
    deactivate() {}
    subscribe(dest: string, cb: (frame: { body: string }) => void) {
      subscriptions.push({ dest, cb });
      return { unsubscribe: () => undefined };
    }
  },
}));

vi.mock('sockjs-client', () => ({
  default: class FakeSockJS {},
}));

// §G5.8 — transport natif : le bus n'utilise plus SockJS (package non installé),
// il se connecte au endpoint STOMP WS brut /ws-church du backend.

vi.mock('@/contexts/AuthContext', () => ({
  useAuth: () => ({ isAuthenticated: true }),
}));

vi.mock('@/contexts/TenantContext', () => ({
  useTenant: () => ({ currentTenant: { id: 'tenant-aaa' } }),
  // useRealtimeBus consomme la variante tolérante (null hors provider).
  useTenantOptional: () => ({ currentTenant: { id: 'tenant-aaa' }, refreshContext: () => Promise.resolve() }),
}));

vi.mock('@/lib/api', () => ({
  default: { defaults: { baseURL: '/api/v1' } },
}));

import { useRealtimeBus, useRealtimeStatus } from '@/hooks/useRealtimeBus';

const TENANT = 'tenant-aaa';

let queryClient: QueryClient;

function wrapper({ children }: { children: ReactNode }) {
  return <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>;
}

function fire(id: number | string, eventType: string) {
  const frame = {
    body: JSON.stringify({ eventId: id, eventType, timestamp: '2026-09-21T10:00:00Z' }),
  };
  for (const s of subscriptions) s.cb(frame);
}

describe('useRealtimeBus (§G5.8)', () => {
  beforeEach(() => {
    subscriptions.length = 0;
    clients.length = 0;
    localStorage.clear();
    localStorage.setItem('accessToken', 'jwt-token');
    queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  });

  it('se connecte au firehose du tenant avec le JWT', async () => {
    renderHook(() => useRealtimeBus(), { wrapper });
    expect(clients).toHaveLength(1);
    expect(clients[0].config.connectHeaders.token).toBe('jwt-token');
    clients[0].config.onConnect();
    await waitFor(() => expect(subscriptions.length).toBeGreaterThan(0));
    expect(subscriptions[0].dest).toBe(`/topic/tenant:${TENANT}/events`);
  });

  it('invalide les requêtes mappées selon eventType', async () => {
    const spy = vi.spyOn(queryClient, 'invalidateQueries');
    renderHook(() => useRealtimeBus(), { wrapper });
    clients[0].config.onConnect();

    fire(1, 'AssetCheckedOut');
    const keys = spy.mock.calls.map((c) => JSON.stringify(c[0]));
    expect(keys).toContain(JSON.stringify({ queryKey: ['assets'] }));
    expect(keys).toContain(JSON.stringify({ queryKey: ['inventory'] }));
    // Pas de rafraîchissement total pour un type connu.
    expect(spy.mock.calls.some((c) => c[0] === undefined)).toBe(false);
  });

  it('déduplique les events rejoués (eventId <= dernier)', async () => {
    const spy = vi.spyOn(queryClient, 'invalidateQueries');
    renderHook(() => useRealtimeBus(), { wrapper });
    clients[0].config.onConnect();

    fire(2, 'TaskCompleted');
    const afterFirst = spy.mock.calls.length;
    fire(2, 'TaskCompleted'); // rejeu exact
    fire(1, 'TaskCompleted'); // plus ancien
    expect(spy.mock.calls.length).toBe(afterFirst);
  });

  it('trou de delta → rafraîchissement TOTAL', async () => {
    const spy = vi.spyOn(queryClient, 'invalidateQueries');
    renderHook(() => useRealtimeBus(), { wrapper });
    clients[0].config.onConnect();

    fire(10, 'TaskAssigned');
    spy.mockClear();
    fire(14, 'TaskCompleted'); // 11-13 perdus pendant une coupure
    expect(spy).toHaveBeenCalledWith();
  });

  it('type inconnu → rafraîchissement total (jamais de donnée périmée)', async () => {
    const spy = vi.spyOn(queryClient, 'invalidateQueries');
    renderHook(() => useRealtimeBus(), { wrapper });
    clients[0].config.onConnect();

    fire(1, 'SomeFutureEventType');
    expect(spy).toHaveBeenCalledWith();
  });

  it('publie l’indicateur de fraîcheur (connected + dernier événement)', async () => {
    renderHook(() => useRealtimeBus(), { wrapper });
    clients[0].config.onConnect();
    fire(1, 'DressCodePublished');

    const { result } = renderHook(() => useRealtimeStatus(), { wrapper });
    await waitFor(() => {
      expect(result.current?.connected).toBe(true);
      expect(result.current?.lastEventType).toBe('DressCodePublished');
      expect(result.current?.lastEventAt).toBeTruthy();
    });
  });
});
