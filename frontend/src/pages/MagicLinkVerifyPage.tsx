import { useEffect, useState } from 'react';
import { useSearchParams, useNavigate, Link } from 'react-router-dom';
import { Loader2, CheckCircle2, AlertTriangle, LogIn } from 'lucide-react';
import api, { getErrorMessage } from '@/lib/api';
import { useAuth } from '@/contexts/AuthContext';

/**
 * Vérification d'un Magic Link (connexion sans mot de passe).
 * Le lien reçu par email pointe ici avec ?token=… ; le backend valide,
 * consomme le token et renvoie une session JWT.
 */
export default function MagicLinkVerifyPage() {
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const { loginWithSocialToken } = useAuth();
  const [state, setState] = useState<'verifying' | 'success' | 'error'>('verifying');
  const [message, setMessage] = useState('');

  useEffect(() => {
    const token = searchParams.get('token');
    if (!token) {
      setState('error');
      setMessage('Lien incomplet : jeton manquant.');
      return;
    }
    let cancelled = false;
    (async () => {
      try {
        const res = await api.get('/auth/magic-link/verify', { params: { token } });
        if (cancelled) return;
        const d = res.data;
        if (!d.token || !d.user) throw new Error('Réponse inattendue du serveur');
        loginWithSocialToken(d.token, {
          id: d.user.id,
          email: d.user.email,
          firstName: d.user.firstName,
          lastName: d.user.lastName,
          role: d.user.role,
        });
        setState('success');
        setTimeout(() => navigate('/dashboard', { replace: true }), 900);
      } catch (e) {
        if (cancelled) return;
        setState('error');
        setMessage(
          getErrorMessage(e) ||
            'Lien invalide ou expiré. Demandez un nouveau lien de connexion.'
        );
      }
    })();
    return () => { cancelled = true; };
  }, [searchParams, navigate, loginWithSocialToken]);

  return (
    <div className="space-y-6 text-center">
      <div className="inline-flex items-center gap-2.5 mb-4 animate-fade-in">
        <span className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full bg-primary-500/10 border border-primary-500/20 text-primary-400 text-xs font-medium">
          <LogIn className="w-3 h-3" />
          Connexion sans mot de passe
        </span>
      </div>

      {state === 'verifying' && (
        <div className="animate-fade-in py-8 flex flex-col items-center gap-4">
          <Loader2 className="w-10 h-10 animate-spin text-primary-500" />
          <p className="text-sm text-gray-500 dark:text-gray-400">
            Vérification de votre lien sécurisé…
          </p>
        </div>
      )}

      {state === 'success' && (
        <div className="animate-slide-up py-8 flex flex-col items-center gap-4">
          <CheckCircle2 className="w-12 h-12 text-green-500" />
          <h2 className="text-xl font-bold text-gray-900 dark:text-white">Connecté !</h2>
          <p className="text-sm text-gray-500 dark:text-gray-400">
            Redirection vers votre tableau de bord…
          </p>
        </div>
      )}

      {state === 'error' && (
        <div className="animate-slide-up space-y-5 py-4">
          <div className="flex flex-col items-center gap-4">
            <AlertTriangle className="w-12 h-12 text-red-400" />
            <h2 className="text-xl font-bold text-gray-900 dark:text-white">
              Lien invalide
            </h2>
            <div className="p-3.5 rounded-xl bg-red-500/10 border border-red-500/20 w-full">
              <p className="text-sm text-red-600 dark:text-red-300">{message}</p>
            </div>
          </div>
          <Link
            to="/login"
            className="block w-full py-3 px-4 rounded-xl bg-gradient-to-r from-primary-600 to-primary-500 text-white font-medium text-sm hover:from-primary-500 hover:to-primary-400 transition-all shadow-lg shadow-primary-500/25 text-center"
          >
            Retour à la connexion
          </Link>
        </div>
      )}
    </div>
  );
}
