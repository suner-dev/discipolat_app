import { useRealtimeStatus } from '@/hooks/useRealtimeBus';
import { useI18n } from '@/i18n';

/**
 * §G5.8 — Indicateur de fraîcheur du bus temps réel (Navbar).
 *
 * Point vert : connecté au firehose du tenant, données poussées < 5 s.
 * Point ambre pulsé : jamais d'événement reçu depuis la connexion.
 * Point gris : canal coupé (reconnexion auto tous les 3 s — les écrans
 * restent consultables, un rafraîchissement total rattrape le delta au retour).
 */
export default function RealtimeBadge() {
  const { t } = useI18n();
  const status = useRealtimeStatus();

  const connected = !!status?.connected;
  const gotEvent = !!status?.lastEventAt;
  const color = !connected ? 'bg-gray-400' : gotEvent ? 'bg-green-500' : 'bg-amber-400 animate-pulse';
  const label = !connected
    ? t('realtime.disconnected') || 'Temps réel déconnecté — reconnexion en cours'
    : gotEvent
      ? t('realtime.live') || 'Données en direct'
      : t('realtime.connected') || 'Connecté — en attente d’événements';

  return (
    <span
      className="hidden lg:flex items-center gap-1.5 px-2 h-9 rounded-xl border border-gray-200/60 dark:border-white/10 bg-gray-50/60 dark:bg-white/[0.03]"
      title={label}
      aria-label={label}
      data-testid="realtime-badge"
    >
      <span className={`w-2 h-2 rounded-full ${color}`} />
      <span className="text-[10px] font-medium text-gray-400 uppercase tracking-wider">
        {t('realtime.badge') || 'Direct'}
      </span>
    </span>
  );
}
