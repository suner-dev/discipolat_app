import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { useMutation } from '@tanstack/react-query';
import { useAuth } from '@/contexts/AuthContext';
import api, { getErrorMessage } from '@/lib/api';
import { Shield, Loader2, LogOut, Smartphone } from 'lucide-react';
import toast from 'react-hot-toast';

export default function TwoFactorChallengePage() {
  const navigate = useNavigate();
  const { user, logout } = useAuth();
  const [code, setCode] = useState('');
  const [error, setError] = useState('');

  const verifyMutation = useMutation({
    mutationFn: async (value: string) => {
      const res = await api.post('/auth/2fa/verify', { code: value });
      return res.data as { valid: boolean };
    },
    onSuccess: (data) => {
      if (data.valid) {
        toast.success(`Bienvenue, ${user?.firstName || user?.email}!`);
        navigate('/dashboard', { replace: true });
      } else {
        setError('Code invalide. Veuillez réessayer.');
      }
    },
    onError: (err) => setError(getErrorMessage(err)),
  });

  // Redirection hors du challenge, en useEffect (jamais pendant le rendu) :
  // - utilisateur déconnecté (user null, ex. « Retour à la connexion ») → /login
  // - utilisateur sans 2FA → /dashboard
  // Avant ce correctif, le navigate() exécuté pendant le rendu écrasait la
  // navigation explicite « Retour à la connexion » (logout → /login) : la page
  // renvoyait alors vers /dashboard juste après la déconnexion.
  useEffect(() => {
    if (!user) {
      navigate('/login', { replace: true });
    } else if (!user.twoFactorEnabled) {
      navigate('/dashboard', { replace: true });
    }
  }, [user, navigate]);

  if (!user || !user.twoFactorEnabled) {
    return null;
  }

  return (
    <div className="min-h-screen flex items-center justify-center bg-gradient-to-br from-gray-900 via-gray-900 to-gray-800 p-4">
      <div className="w-full max-w-md">
        <div className="text-center mb-8">
          <div className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full bg-violet-500/10 border border-violet-500/20 text-violet-400 text-xs font-medium mb-4 animate-fade-in">
            <Shield className="w-3 h-3" />
            Vérification en deux étapes
          </div>
          <h2 className="text-2xl font-bold text-white font-display animate-slide-up">
            Authentification à deux facteurs
          </h2>
          <p className="mt-1.5 text-sm text-gray-400 animate-slide-up" style={{ animationDelay: '50ms' }}>
            Entrez le code à 6 chiffres généré par votre application d'authentification
          </p>
        </div>

        <div className="animate-slide-up" style={{ animationDelay: '100ms' }}>
          <div className="p-8 rounded-2xl bg-white/[0.03] border border-white/10 backdrop-blur-sm">
            <div className="flex justify-center mb-6">
              <div className="p-4 rounded-2xl bg-violet-500/10">
                <Smartphone className="w-10 h-10 text-violet-400" />
              </div>
            </div>

            {error && (
              <div className="mb-4 p-3 rounded-xl bg-red-500/10 border border-red-500/20">
                <p className="text-sm text-red-300 text-center">{error}</p>
              </div>
            )}

            <div className="mb-6">
              <input
                type="text"
                inputMode="numeric"
                maxLength={6}
                className="w-full text-center text-3xl font-mono tracking-[0.5em] bg-white/5 border border-white/10 text-white rounded-xl px-4 py-4 focus:outline-none focus:border-violet-500/50 focus:ring-2 focus:ring-violet-500/20 transition-all"
                placeholder="000000"
                value={code}
                onChange={(e) => { const v = e.target.value.replace(/\D/g, '').slice(0, 6); setCode(v); setError(''); }}
                autoFocus
              />
            </div>

            <button
              onClick={() => verifyMutation.mutate(code)}
              disabled={code.length !== 6 || verifyMutation.isPending}
              className="w-full py-3 px-4 rounded-xl bg-gradient-to-r from-violet-600 to-violet-500 text-white font-medium text-sm hover:from-violet-500 hover:to-violet-400 focus:outline-none focus:ring-2 focus:ring-violet-500/40 disabled:opacity-50 disabled:cursor-not-allowed transition-all duration-200 shadow-lg shadow-violet-500/25 hover:shadow-violet-500/40 active:scale-[0.98] flex items-center justify-center gap-2"
            >
              {verifyMutation.isPending ? (
                <><Loader2 className="w-4 h-4 animate-spin" /> Vérification...</>
              ) : (
                <><Shield className="w-4 h-4" /> Vérifier</>
              )}
            </button>

            <div className="mt-4 text-center">
              <button
                onClick={() => { logout(); navigate('/login', { replace: true }); }}
                className="inline-flex items-center gap-1.5 text-sm text-gray-400 hover:text-gray-200 transition-colors"
              >
                <LogOut className="w-3.5 h-3.5" />
                Retour à la connexion
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
