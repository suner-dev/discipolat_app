import { createContext, useCallback, useContext, useEffect, useState, type ReactNode } from 'react';
import api from '@/lib/api';
import { applyBranding, cacheBranding, DEFAULT_BRANDING } from '@/lib/branding';
import type { PublicBranding } from '@/types';

/**
 * Contexte de marque de l'église.
 *
 * Charge l'identité publique une seule fois (avec cache local anti-flash),
 * l'applique globalement (couleurs, typographie, favicon, titre) et la met
 * à disposition de tous les composants (sidebar, navbar, landing, pages).
 */
interface SettingsContextValue {
  branding: PublicBranding;
  isLoaded: boolean;
  refresh: () => Promise<void>;
  apply: (branding: PublicBranding) => void;
}

const SettingsContext = createContext<SettingsContextValue | null>(null);

export function SettingsProvider({ children }: { children: ReactNode }) {
  const [branding, setBranding] = useState<PublicBranding>(DEFAULT_BRANDING);
  const [isLoaded, setIsLoaded] = useState(false);

  const apply = useCallback((b: PublicBranding) => {
    setBranding(b);
    applyBranding(b);
    cacheBranding(b);
  }, []);

  const refresh = useCallback(async () => {
    try {
      const res = await api.get('/public/settings');
      const data = res.data as PublicBranding;
      apply(data);
      setIsLoaded(true);
    } catch {
      // Hors-ligne ou serveur indisponible : on conserve le cache local / le défaut.
      setIsLoaded(true);
    }
  }, [apply]);

  useEffect(() => {
    void refresh();
  }, [refresh]);

  return (
    <SettingsContext.Provider value={{ branding, isLoaded, refresh, apply }}>
      {children}
    </SettingsContext.Provider>
  );
}

/** Contexte par défaut (composants rendus hors provider : tests, aperçus). */
const DEFAULT_CONTEXT: SettingsContextValue = {
  branding: DEFAULT_BRANDING,
  isLoaded: true,
  refresh: async () => undefined,
  apply: () => undefined,
};

export function useSettings(): SettingsContextValue {
  return useContext(SettingsContext) ?? DEFAULT_CONTEXT;
}
