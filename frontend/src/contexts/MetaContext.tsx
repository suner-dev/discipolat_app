import { createContext, useCallback, useContext, useEffect, useState, type ReactNode } from 'react';
import api from '@/lib/api';
import type { PlatformMeta } from '@/types';

/**
 * Méta-données publiques de la plateforme (endpoint /api/v1/public/meta,
 * sans authentification) : nom, version, environnement, mode bêta et
 * visibilité des comptes de démonstration.
 *
 * Le mode bêta pilote l'affichage du badge BÊTA, du bandeau testeur et
 * des comptes de démonstration sur l'écran de connexion. En cas d'échec
 * réseau, on retombe sur des valeurs neutres (aucun mode bêta) — fail-closed.
 */

const DEFAULT_META: PlatformMeta = {
  appName: 'Discipolat',
  version: '',
  environment: 'dev',
  betaMode: false,
  demoAccountsEnabled: false,
};

interface MetaContextValue {
  meta: PlatformMeta;
  isLoaded: boolean;
  refresh: () => Promise<void>;
}

const MetaContext = createContext<MetaContextValue | null>(null);

export function MetaProvider({ children }: { children: ReactNode }) {
  const [meta, setMeta] = useState<PlatformMeta>(DEFAULT_META);
  const [isLoaded, setIsLoaded] = useState(false);

  const refresh = useCallback(async () => {
    try {
      const res = await api.get('/public/meta');
      setMeta({ ...DEFAULT_META, ...res.data });
    } catch {
      // Serveur indisponible : valeurs par défaut (aucun mode bêta).
    } finally {
      setIsLoaded(true);
    }
  }, []);

  useEffect(() => {
    void refresh();
  }, [refresh]);

  return (
    <MetaContext.Provider value={{ meta, isLoaded, refresh }}>
      {children}
    </MetaContext.Provider>
  );
}

/** Contexte par défaut (composants rendus hors provider : tests, aperçus). */
const DEFAULT_CONTEXT: MetaContextValue = {
  meta: DEFAULT_META,
  isLoaded: true,
  refresh: async () => undefined,
};

export function usePlatformMeta(): MetaContextValue {
  return useContext(MetaContext) ?? DEFAULT_CONTEXT;
}
