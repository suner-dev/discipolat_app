// B3 — Suivi public d'une demande d'inscription (contrat §3.3).
//
// POST /auth/registration-status { email }
//   -> { status: PENDING_APPROVAL|APPROVED|REJECTED|NONE,
//        decidedAt, reason, canLogin }
//
// Points de vigilance respectés :
//   - `reason` n'est renvoyé QUE si status = REJECTED (on ne l'affiche qu'à ce cas) ;
//   - aucune donnée personnelle n'est stockée ni envoyée ailleurs ;
//   - `429` affiché comme une attente, pas comme une erreur fatale ;
//   - pas de cache React Query sur la requête (staleTime: 0, gcTime: 0) : un statut
//     d'approbation périmé afficherait un faux « Refusé ».

import { useState } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import { CheckCircle2, Clock, XCircle, Search, Loader2 } from 'lucide-react';
import api from '@/lib/api';
import { useI18n, tText } from '@/i18n';

type Status = 'PENDING_APPROVAL' | 'APPROVED' | 'REJECTED' | 'NONE';

interface RegistrationStatusResponse {
  status: Status;
  decidedAt: string | null;
  reason: string | null;
  canLogin: boolean;
}

const isStatus = (v: unknown): v is Status =>
  v === 'PENDING_APPROVAL' || v === 'APPROVED' || v === 'REJECTED' || v === 'NONE';

export default function RegistrationStatusPage() {
  const { t } = useI18n();
  const [params] = useSearchParams();
  const [email, setEmail] = useState<string>(params.get('email') ?? '');
  const [loading, setLoading] = useState(false);
  const [rateLimited, setRateLimited] = useState(false);
  const [data, setData] = useState<RegistrationStatusResponse | null>(null);
  const [error, setError] = useState<string | null>(null);

  const submit = async (e: React.FormEvent) => {
    e.preventDefault();
    const value = email.trim();
    if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(value)) {
      setError(tText('Adresse email invalide.'));
      return;
    }
    setLoading(true);
    setError(null);
    setRateLimited(false);
    setData(null);
    try {
      const res = await api.post<RegistrationStatusResponse>('/auth/registration-status', {
        email: value,
      });
      const body = res.data;
      // Défense : on ne fait confiance qu'à un statut connu du contrat.
      setData({ ...body, status: isStatus(body?.status) ? body.status : 'NONE' });
    } catch (err) {
      const status = (err as { response?: { status?: number } })?.response?.status;
      if (status === 429) {
        setRateLimited(true);
      } else {
        setError(tText('Impossible de vérifier le statut pour le moment. Réessayez.'));
      }
    } finally {
      setLoading(false);
    }
  };

  const renderResult = () => {
    if (!data) return null;
    switch (data.status) {
      case 'PENDING_APPROVAL':
        return (
          <div className="glass-card p-6 border-amber-500/30" role="status">
            <Clock className="w-8 h-8 text-amber-500 mb-3" aria-hidden="true" />
            <h2 className="font-semibold mb-1">{t('Demande en cours d\u2019examen')}</h2>
            <p className="text-sm text-gray-600 dark:text-gray-400">
              {t('Votre demande a bien été reçue. Un Super Admin doit encore l\u2019approuver avant que vous puissiez vous connecter.')}
            </p>
          </div>
        );
      case 'APPROVED':
        return (
          <div className="glass-card p-6 border-emerald-500/30" role="status">
            <CheckCircle2 className="w-8 h-8 text-emerald-500 mb-3" aria-hidden="true" />
            <h2 className="font-semibold mb-1">{t('Demande approuvée')}</h2>
            <p className="text-sm text-gray-600 dark:text-gray-400 mb-4">
              {t('Vous pouvez maintenant vous connecter avec cette adresse email.')}
            </p>
            {data.canLogin && (
              <Link to="/login" className="btn-primary btn-sm inline-flex items-center min-h-[44px] px-4">
                {t('Se connecter')}
              </Link>
            )}
          </div>
        );
      case 'REJECTED':
        return (
          <div className="glass-card p-6 border-red-500/30" role="status">
            <XCircle className="w-8 h-8 text-red-500 mb-3" aria-hidden="true" />
            <h2 className="font-semibold mb-1">{t('Demande refusée')}</h2>
            {/* Le motif n'est divulgué que dans ce cas (contrat §3.3). */}
            {data.reason && (
              <p className="text-sm text-gray-600 dark:text-gray-400">
                {t('Motif :')} {data.reason}
              </p>
            )}
            {data.decidedAt && (
              <p className="text-xs text-gray-500 mt-2">
                {new Date(data.decidedAt).toLocaleDateString(undefined, { dateStyle: 'long' })}
              </p>
            )}
          </div>
        );
      case 'NONE':
      default:
        return (
          <div className="glass-card p-6" role="status">
            <Search className="w-8 h-8 text-gray-400 mb-3" aria-hidden="true" />
            <h2 className="font-semibold mb-1">{t('Aucune demande trouvée')}</h2>
            <p className="text-sm text-gray-600 dark:text-gray-400">
              {t('Aucune demande d\u2019inscription n\u2019est enregistrée pour cette adresse. Vous pouvez en faire une.')}
            </p>
            <Link to="/register" className="btn-sm underline mt-3 inline-flex items-center min-h-[44px]">
              {t('Demander une inscription')}
            </Link>
          </div>
        );
    }
  };

  return (
    <div className="page-container max-w-2xl mx-auto">
      <div className="page-header">
        <div>
          <h1 className="page-title">{t('Suivre ma demande')}</h1>
          <p className="page-subtitle">
            {t('Saisissez l\u2019adresse email utilisée lors de votre demande.')}
          </p>
        </div>
      </div>

      <form onSubmit={submit} className="glass-card p-6 space-y-4" noValidate>
        <div>
          <label htmlFor="statusEmail" className="block text-sm font-medium mb-1">
            {t('Adresse email')}
          </label>
          <input
            id="statusEmail"
            name="email"
            type="email"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            disabled={loading}
            autoComplete="email"
            required
            className="w-full rounded-lg border border-gray-300 dark:border-gray-700 bg-white dark:bg-gray-900 px-3 py-2 min-h-[44px]"
          />
        </div>
        {error && <p role="alert" className="text-sm text-red-500">{error}</p>}
        <button type="submit" disabled={loading} className="btn-primary btn-sm min-h-[44px]">
          {loading ? (
            <>
              <Loader2 className="w-4 h-4 mr-2 animate-spin" aria-hidden="true" />
              {t('Vérification…')}
            </>
          ) : (
            t('Vérifier le statut')
          )}
        </button>
      </form>

      {rateLimited && (
        <div className="glass-card p-4 mt-4 border-amber-500/30" role="status">
          <p className="text-sm text-amber-700 dark:text-amber-300">
            {t('Trop de vérifications depuis cette connexion. Merci de patienter quelques minutes avant de réessayer.')}
          </p>
        </div>
      )}

      <div className="mt-4">{renderResult()}</div>

      <p className="mt-6 text-sm">
        <Link to="/login" className="underline min-h-[44px] inline-flex items-center">
          {t('Retour à la connexion')}
        </Link>
      </p>
    </div>
  );
}
