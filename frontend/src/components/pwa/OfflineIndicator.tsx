import { useState, useEffect } from 'react';
import { X } from 'lucide-react';

import { tText } from '@/i18n';
/**
 * PWA Offline Indicator + Install Prompt.
 * Shows a banner when offline and a button to install the PWA.
 */
export function OfflineIndicator() {
  const [isOffline, setIsOffline] = useState(!navigator.onLine);
  const [deferredPrompt, setDeferredPrompt] = useState<any>(null);
  const [showInstall, setShowInstall] = useState(false);

  useEffect(() => {
    const handleOnline = () => setIsOffline(false);
    const handleOffline = () => setIsOffline(true);

    window.addEventListener('online', handleOnline);
    window.addEventListener('offline', handleOffline);

    // Capture the install prompt
    const handleBeforeInstallPrompt = (e: Event) => {
      e.preventDefault();
      setDeferredPrompt(e);
      // Le pasteur a fermé l'onglet « Installer » pendant cette session :
      // ne plus le faire réapparaître (pas de rappel tenace sur la carte user).
      if (sessionStorage.getItem('pwaInstallDismissed') !== '1') {
        setShowInstall(true);
      }
    };

    window.addEventListener('beforeinstallprompt', handleBeforeInstallPrompt);

    return () => {
      window.removeEventListener('online', handleOnline);
      window.removeEventListener('offline', handleOffline);
      window.removeEventListener('beforeinstallprompt', handleBeforeInstallPrompt);
    };
  }, []);

  const handleInstall = async () => {
    if (!deferredPrompt) return;
    deferredPrompt.prompt();
    const { outcome } = await deferredPrompt.userChoice;
    if (outcome === 'accepted') {
      setShowInstall(false);
    }
    setDeferredPrompt(null);
  };

  const dismissInstall = () => {
    setShowInstall(false);
    sessionStorage.setItem('pwaInstallDismissed', '1');
  };

  return (
    <>
      {/* Offline banner */}
      {isOffline && (
        <div
          style={{
            position: 'fixed',
            top: 0,
            left: 0,
            right: 0,
            zIndex: 9999,
            background: 'linear-gradient(90deg, #ef4444, #dc2626)',
            color: 'white',
            padding: '8px 16px',
            textAlign: 'center',
            fontSize: '13px',
            fontWeight: 500,
            boxShadow: '0 2px 8px rgba(0,0,0,0.3)',
          }}
        >
          {tText('📡 Vous êtes hors ligne — les modifications seront synchronisées à la reconnexion')}
        </div>
      )}

      {/* Install PWA — bas-GAUCHE en décalant la barre latérale (lg:left-[17.5rem]
          = sidebar 256 px + marge) : en fix left:24 la pill recouvrait la carte
          utilisateur du bas de sidebar. Repli feedback : le widget « Un retour ? »
          vit en bas à droite. Fermeture possible, tenue sur la session. */}
      {showInstall && (
        <div className="fixed bottom-6 left-6 lg:left-[17.5rem] z-[9998] max-w-[calc(100vw-3rem)] animate-slide-up">
          <div className="flex items-center gap-1 rounded-xl bg-gradient-to-br from-indigo-500 to-violet-600 text-white shadow-[0_4px_16px_rgba(99,102,241,0.4)] pl-4 pr-1.5 py-1.5">
            <button
              onClick={handleInstall}
              className="text-sm font-semibold whitespace-nowrap cursor-pointer"
            >
              📱 {tText("Installer l'application")}
            </button>
            <button
              onClick={dismissInstall}
              aria-label={tText('Fermer')}
              title={tText('Fermer')}
              className="p-1.5 rounded-lg hover:bg-white/15 transition-colors cursor-pointer"
            >
              <X className="w-3.5 h-3.5" />
            </button>
          </div>
        </div>
      )}
    </>
  );
}

export default OfflineIndicator;
