import { useEffect } from 'react';
import { useLocation } from 'react-router-dom';
import { api } from '@/lib/api';

const SESSION_START = Date.now();
let lastPath = window.location.pathname;
let lastTime = Date.now();

function detectDevice(): string {
  const w = window.innerWidth;
  if (w < 640) return 'MOBILE';
  if (w < 1024) return 'TABLET';
  return 'DESKTOP';
}

/**
 * P3 #109 — Tracking d'usage self-hosted (pages vues + durée).
 * Envoie les PAGE_VIEW par lots au changement de route. Silencieux en cas d'erreur.
 */
export function useUsageTracking() {
  const location = useLocation();

  useEffect(() => {
    const now = Date.now();
    const durationMs = now - lastTime;
    const event = {
      page: lastPath,
      action: 'PAGE_VIEW',
      referrer: document.referrer || undefined,
      durationMs,
      device: detectDevice(),
    };

    lastPath = location.pathname;
    lastTime = now;

    const token = localStorage.getItem('accessToken');
    if (!token) return; // endpoint authentifié uniquement

    // Fire & forget — ne bloque jamais le rendu.
    api.post('/usage-analytics/track', [event]).catch(() => {
      /* analytics best-effort */
    });
  }, [location.pathname]);
}

export function sessionDurationSec(): number {
  return Math.round((Date.now() - SESSION_START) / 1000);
}
