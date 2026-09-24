import React, { createContext, useContext, useState, useCallback } from 'react';
import api from '@/lib/api';
import toast from 'react-hot-toast';

/**
 * §G1.9 — Contexte d'impersonation Super Admin (côté web).
 *
 * Le token d'impersonation est un JWT court (30 min, côté backend) portant
 * l'identité de la CIBLE : une fois posé comme `accessToken`, l'admin voit
 * exactement ce que voit l'utilisateur (aucune permission supplémentaire).
 * Le vrai token admin est conservé en sessionStorage et restauré à la sortie.
 */
interface ImpersonationState {
  token: string;
  adminAccessToken: string;
  adminRefreshToken: string;
  tenantId: string;
  tenantName: string;
  targetEmail: string;
  startTime: string;
  expiresAt: string;
}

interface ImpersonationContextType {
  isImpersonating: boolean;
  targetEmail: string | null;
  tenantName: string | null;
  expiresAt: string | null;
  /** Démarre une impersonation (le motif est journalisé côté serveur). */
  startImpersonation: (targetUserEmail: string, reason: string, tenantId: string) => Promise<void>;
  /** Termine la session et restaure la session admin réelle. */
  stopImpersonation: () => Promise<void>;
}

const ImpersonationContext = createContext<ImpersonationContextType | undefined>(undefined);

const STORAGE_KEY = 'discipolat:impersonation';

function readState(): ImpersonationState | null {
  try {
    const raw = sessionStorage.getItem(STORAGE_KEY);
    return raw ? (JSON.parse(raw) as ImpersonationState) : null;
  } catch {
    return null;
  }
}

export function ImpersonationProvider({ children }: { children: React.ReactNode }) {
  const [state, setState] = useState<ImpersonationState | null>(readState);

  const startImpersonation = useCallback(async (targetUserEmail: string, reason: string, tenantId: string) => {
    try {
      if (!tenantId.trim()) {
        throw new Error('Le tenant cible est requis');
      }
      const res = await api.post('/platform/admin/impersonate', {
        tenantId,
        targetUserEmail,
        reason,
      });
      const d = res.data;
      const adminAccessToken = localStorage.getItem('accessToken') ?? '';
      const adminRefreshToken = localStorage.getItem('refreshToken') ?? '';

      // On devient la cible : le JWT d'impersonation remplace le token admin.
      localStorage.setItem('accessToken', d.impersonationToken);
      localStorage.setItem('refreshToken', '');
      api.defaults.headers.common['Authorization'] = `Bearer ${d.impersonationToken}`;

      const next: ImpersonationState = {
        token: d.impersonationToken,
        adminAccessToken,
        adminRefreshToken,
        tenantId: d.tenantId,
        tenantName: d.tenantName,
        targetEmail: targetUserEmail,
        startTime: d.startTime,
        expiresAt: d.expiresAt,
      };
      sessionStorage.setItem(STORAGE_KEY, JSON.stringify(next));
      setState(next);
      toast.success(`Impersonation de ${targetUserEmail} démarrée (30 min max)`);
      // Rechargement complet : tous les contextes repartent avec l'identité cible.
      window.location.href = '/';
    } catch (err: any) {
      const detail = err?.response?.data?.detail || err?.response?.data?.error;
      toast.error(detail || 'Impersonation refusée');
      throw err;
    }
  }, []);

  const stopImpersonation = useCallback(async () => {
    const current = readState();
    try {
      if (current?.token) {
        // Journalise la durée réelle côté serveur (IP, UA, durée).
        await api.post('/platform/admin/impersonate/stop', { impersonationToken: current.token });
      }
    } catch {
      // La trace serveur peut échouer (token expiré) : on restaure quand même l'admin.
    }
    if (current) {
      localStorage.setItem('accessToken', current.adminAccessToken);
      localStorage.setItem('refreshToken', current.adminRefreshToken);
      api.defaults.headers.common['Authorization'] = `Bearer ${current.adminAccessToken}`;
    }
    sessionStorage.removeItem(STORAGE_KEY);
    setState(null);
    toast.success('Impersonation terminée');
    window.location.href = '/';
  }, []);

  return (
    <ImpersonationContext.Provider
      value={{
        isImpersonating: !!state,
        targetEmail: state?.targetEmail ?? null,
        tenantName: state?.tenantName ?? null,
        expiresAt: state?.expiresAt ?? null,
        startImpersonation,
        stopImpersonation,
      }}
    >
      {children}
    </ImpersonationContext.Provider>
  );
}

export function useImpersonation() {
  const ctx = useContext(ImpersonationContext);
  if (!ctx) {
    throw new Error('useImpersonation must be used within an ImpersonationProvider');
  }
  return ctx;
}
