import { useState, useEffect } from 'react';

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
      setShowInstall(true);
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
          📡 Vous êtes hors ligne — les modifications seront synchronisées à la reconnexion
        </div>
      )}

      {/* Install PWA button */}
      {showInstall && (
        <div
          style={{
            position: 'fixed',
            bottom: 24,
            right: 24,
            zIndex: 9998,
          }}
        >
          <button
            onClick={handleInstall}
            style={{
              background: 'linear-gradient(135deg, #6366f1, #8b5cf6)',
              color: 'white',
              border: 'none',
              borderRadius: 12,
              padding: '12px 20px',
              fontSize: '14px',
              fontWeight: 600,
              cursor: 'pointer',
              boxShadow: '0 4px 16px rgba(99,102,241,0.4)',
              display: 'flex',
              alignItems: 'center',
              gap: 8,
            }}
          >
            📱 Installer l'application
          </button>
        </div>
      )}
    </>
  );
}

export default OfflineIndicator;
