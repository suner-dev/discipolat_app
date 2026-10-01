import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import api from '@/lib/api';
import { useAuth } from '@/contexts/AuthContext';
import { useI18n } from '@/i18n';
import { AlertCircle, Loader2 } from 'lucide-react';
import { clearFacebookHandoff, readFacebookCredential } from '@/features/auth/social/facebook';

/**
 * Page de retour de la redirection Facebook.
 *
 * <p>Facebook renvoie l'`id_token` dans le **fragment** de l'URL
 * (`#id_token=…`). Cette page :
 * <ol>
 *   <li>valide le relais (`state` + fraîcheur de 5 minutes) — protection CSRF ;</li>
 *   <li>échange le credential contre une session Discipolat
 *       (`POST /auth/social/facebook`) ;</li>
 *   <li>connecte l'utilisateur, puis redirige vers l'espace de son rôle.</li>
 * </ol>
 *
 * <p>Elle traite aussi l'**acceptation d'invitation** : si la session de
 * navigation venait de la page d'invitation, le flux reprend là-bas. Dans le
 * cas contraire, un compte inconnu reçoit une explication actionnable plutôt
 * qu'une page blanche.
 *
 * <p>Le fragment est nettoyé de l'URL : un `id_token` ne doit pas rester dans
 * l'historique du navigateur.
 */
export default function SocialCallbackPage() {
  const navigate = useNavigate();
  const { loginWithSocialToken } = useAuth();
  const { t } = useI18n();
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;

    const run = async () => {
      let credential: string;
      try {
        credential = readFacebookCredential();
      } catch (failure) {
        const code = (failure as { code?: string }).code;
        if (code === 'SOCIAL_LOGIN_CANCELLED') {
          navigate('/login', { replace: true });
          return;
        }
        if (!cancelled) {
          setError(
            (failure as Error).message ||
              t('auth.social.genericError')
          );
        }
        return;
      }

      try {
        const response = await api.post('/auth/social/facebook', { credential });
        const data = response.data;

        loginWithSocialToken(
          data.accessToken,
          {
            id: data.userId,
            email: data.email,
            firstName: data.firstName ?? undefined,
            lastName: data.lastName ?? undefined,
            role: data.role,
          },
          data.refreshToken
        );
        navigate('/dashboard', { replace: true });
      } catch (failure) {
        const code = (failure as { response?: { data?: { title?: string; detail?: string } } })
          ?.response?.data?.title;
        clearFacebookHandoff();
        if (cancelled) return;

        if (code === 'SOCIAL_ACCOUNT_NOT_LINKED') {
          setError(t('auth.social.accountNotLinked'));
          return;
        }
        if (code === 'SOCIAL_EMAIL_MISSING') {
          setError(t('auth.social.facebookNoEmail'));
          return;
        }
        setError(
          (failure as { response?: { data?: { detail?: string } } })?.response?.data?.detail ||
            t('auth.social.genericError')
        );
      }
    };

    void run();
    return () => {
      cancelled = true;
    };
    // Une seule exécution : le fragment est consommé une fois pour toutes.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  if (error) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-gray-50 dark:bg-gray-950 px-4">
        <div className="max-w-md w-full p-8 bg-white dark:bg-gray-900 rounded-xl shadow-sm border border-gray-200 dark:border-white/10">
          <div className="flex items-start gap-3 text-red-600 dark:text-red-400">
            <AlertCircle className="w-5 h-5 shrink-0 mt-0.5" aria-hidden="true" />
            <div>
              <h1 className="font-semibold text-gray-900 dark:text-gray-100">
                {t('auth.social.cannotComplete')}
              </h1>
              <p className="mt-1 text-sm">{error}</p>
            </div>
          </div>
          <div className="mt-6 flex flex-col gap-2">
            <button
              type="button"
              onClick={() => navigate('/login', { replace: true })}
              className="w-full py-2.5 px-4 rounded-xl bg-green-600 text-white font-medium text-sm
                         hover:bg-green-700 focus-visible:outline-none
                         focus-visible:ring-2 focus-visible:ring-green-500"
            >
              {t('auth.backToLogin')}
            </button>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div
      className="min-h-screen flex items-center justify-center bg-gray-50 dark:bg-gray-950"
      aria-busy="true"
      aria-live="polite"
    >
      <div className="flex items-center gap-3 text-gray-600 dark:text-gray-300">
        <Loader2 className="w-5 h-5 animate-spin" aria-hidden="true" />
        <span className="text-sm">{t('auth.social.completing')}</span>
      </div>
    </div>
  );
}
