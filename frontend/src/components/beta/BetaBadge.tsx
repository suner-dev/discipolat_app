import { FlaskConical } from 'lucide-react';
import { usePlatformMeta } from '@/contexts/MetaContext';

/**
 * Badge "BÊTA" — affiché uniquement lorsque le serveur déclare le mode bêta
 * (profil Spring `beta`). Aucun affichage en production.
 */
export default function BetaBadge({ className = '' }: { className?: string }) {
  const { meta } = usePlatformMeta();
  if (!meta.betaMode) return null;

  return (
    <span
      className={`inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-[10px] font-bold uppercase tracking-wider
        bg-gradient-to-r from-amber-500 to-orange-500 text-white shadow-sm shadow-amber-500/30 ${className}`}
      title={`Version ${meta.version || 'bêta'} — environnement de test`}
    >
      <FlaskConical className="w-3 h-3" />
      Bêta
    </span>
  );
}
