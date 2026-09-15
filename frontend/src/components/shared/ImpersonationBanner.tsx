import { Eye, LogOut } from 'lucide-react';
import { useImpersonation } from '@/contexts/ImpersonationContext';

/**
 * §G1.9 — Bandeau permanent d'impersonation : « 👁 Impersonation de X — Quitter ».
 * Rendu par MainLayout au-dessus de tout le contenu ; présent tant que la
 * session d'impersonation est active (sessionStorage).
 */
export default function ImpersonationBanner() {
  const { isImpersonating, targetEmail, tenantName, expiresAt, stopImpersonation } = useImpersonation();

  if (!isImpersonating) return null;

  const expiry = expiresAt ? new Date(expiresAt).toLocaleTimeString('fr-FR', { hour: '2-digit', minute: '2-digit' }) : '—';

  return (
    <div className="mx-4 sm:mx-6 lg:mx-8 mt-4 animate-slide-up">
      <div className="flex items-center gap-3 px-4 py-2.5 rounded-xl border border-violet-500/30 bg-gradient-to-r from-violet-600/15 via-violet-500/10 to-transparent">
        <Eye className="w-4 h-4 text-violet-500 flex-shrink-0" />
        <p className="text-xs flex-1 text-violet-700 dark:text-violet-300">
          <span className="font-semibold">👁 Impersonation de {targetEmail || '…'}</span>
          {tenantName ? ` — ${tenantName}` : ''}
          {' '}· session expire à {expiry} · vos actions sont journalisées.
        </p>
        <button
          onClick={() => stopImpersonation()}
          className="flex items-center gap-1.5 px-3 py-1.5 rounded-lg bg-violet-600 text-white text-xs font-medium hover:bg-violet-700 flex-shrink-0"
        >
          <LogOut className="w-3.5 h-3.5" />
          Quitter
        </button>
      </div>
    </div>
  );
}