import React, { createContext, useContext, useState, useEffect, useCallback } from 'react';
import type { User, LoginRequest, AuthResponse, UserRole } from '@/types';
import api from '@/lib/api';
import toast from 'react-hot-toast';

interface AuthContextType {
  user: User | null;
  isAuthenticated: boolean;
  isLoading: boolean;
  login: (data: LoginRequest) => Promise<AuthResponse>;
  logout: () => void;
  updateUser: (updates: Partial<User>) => void;
  hasRole: (...roles: string[]) => boolean;
  switchRole: (newRole: string) => Promise<void>;
  roles: UserRole[];
  activeRole: UserRole | null;
}

const roleLabels: Record<string, string> = {
  ADMIN: 'Admin',
  PASTEUR: 'Pasteur',
  RESPONSABLE: 'Responsable',
  FAISEUR: 'Faiseur',
  CHEF_DE_FAMILLE: 'Chef de famille',
  MEMBRE: 'Membre',
};

function buildUserFromAuthResponse(d: any): User {
  const roles: UserRole[] = d.roles && d.roles.length > 0
    ? d.roles
    : d.role ? [d.role] : ['FAISEUR'];
  const activeRole: UserRole = d.activeRole || roles[0] || 'FAISEUR';

  return {
    id: d.userId,
    email: d.email,
    role: activeRole,
    roles,
    activeRole,
    estChefDeFamille: d.estChefDeFamille || false,
    firstName: d.firstName || '',
    lastName: d.lastName || '',
    phone: d.phone || '',
    statut: d.statut || 'ACTIVE',
    familleGereeId: d.familleGereeId || undefined,
    dateNaissance: d.dateNaissance || '',
    photoUrl: d.photoUrl || '',
    situationFamiliale: d.situationFamiliale || '',
    twoFactorEnabled: d.twoFactorEnabled || false,
    createdAt: d.createdAt || new Date().toISOString(),
    updatedAt: d.updatedAt || new Date().toISOString(),
  };
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [user, setUser] = useState<User | null>(() => {
    const stored = localStorage.getItem('user');
    if (!stored) return null;
    try {
      return JSON.parse(stored) as User;
    } catch {
      return null;
    }
  });
  const [isLoading, setIsLoading] = useState(true);

  const roles: UserRole[] = user?.roles ?? (user?.role ? [user.role] : []);
  const activeRole: UserRole | null = user?.activeRole ?? user?.role ?? null;

  useEffect(() => {
    const token = localStorage.getItem('accessToken');
    if (token) {
      api.defaults.headers.common['Authorization'] = `Bearer ${token}`;
    }
    setIsLoading(false);
  }, []);

  const login = useCallback(async (data: LoginRequest) => {
    const response = await api.post('/auth/login', data);
    const d = response.data;

    const userData = buildUserFromAuthResponse(d);

    localStorage.setItem('accessToken', d.accessToken);
    localStorage.setItem('refreshToken', d.refreshToken);
    localStorage.setItem('user', JSON.stringify(userData));
    api.defaults.headers.common['Authorization'] = `Bearer ${d.accessToken}`;
    setUser(userData);

    if (!d.twoFactorEnabled) {
      toast.success(`Bienvenue, ${userData.firstName || userData.email}!`);
    }

    return d;
  }, []);

  const logout = useCallback(() => {
    localStorage.removeItem('accessToken');
    localStorage.removeItem('refreshToken');
    localStorage.removeItem('user');
    delete api.defaults.headers.common['Authorization'];
    setUser(null);
    toast.success('Déconnexion réussie');
  }, []);

  const updateUser = useCallback((updates: Partial<User>) => {
    setUser((prev) => {
      if (!prev) return prev;
      const updated = { ...prev, ...updates };
      localStorage.setItem('user', JSON.stringify(updated));
      return updated;
    });
  }, []);

  const hasRole = useCallback(
    (...checkRoles: string[]) => {
      if (!user) return false;
      // Check against ALL user roles, not just activeRole
      return user.roles?.some((r) => checkRoles.includes(r)) ?? checkRoles.includes(user.role);
    },
    [user]
  );

  const switchRole = useCallback(async (newRole: string) => {
    try {
      const res = await api.post('/auth/switch-role', { role: newRole });
      const d = res.data;

      // Update JWT tokens returned by switch-role
      if (d.accessToken) {
        localStorage.setItem('accessToken', d.accessToken);
        localStorage.setItem('refreshToken', d.refreshToken);
        api.defaults.headers.common['Authorization'] = `Bearer ${d.accessToken}`;
      }

      setUser((prev) => {
        if (!prev) return prev;
        const updated = {
          ...prev,
          role: newRole as UserRole,
          activeRole: newRole as UserRole,
        };
        localStorage.setItem('user', JSON.stringify(updated));
        return updated;
      });

      toast.success(`Rôle actif : ${roleLabels[newRole] || newRole}`);
      // React state update triggers automatic re-render with new role
    } catch (err: any) {
      toast.error('Échec du changement de rôle');
    }
  }, []);

  return (
    <AuthContext.Provider
      value={{
        user,
        isAuthenticated: !!user,
        isLoading,
        login,
        logout,
        updateUser,
        hasRole,
        switchRole,
        roles,
        activeRole,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
}

export { roleLabels };

