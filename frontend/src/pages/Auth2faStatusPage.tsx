import { useQuery } from '@tanstack/react-query';
import api, { getErrorMessage } from '@/lib/api';
import { ShieldCheck, Loader2, CheckCircle2, XCircle, Smartphone } from 'lucide-react';

interface TwoFAStatus {
  enabled: boolean;
  method?: string;
  lastVerified?: string;
  backupCodesCount?: number;
}

export default function Auth2faStatusPage() {
  const { data: status, isLoading, error } = useQuery({
    queryKey: ['auth-2fa-status'],
    queryFn: async () => (await api.get('/auth/2fa/status')).data as TwoFAStatus,
  });

  return (
    <div className="page-container">
      <div className="page-header">
        <div className="p-3 rounded-xl bg-gradient-to-br from-emerald-500 to-teal-600 text-white shadow-lg">
          <ShieldCheck className="w-6 h-6" />
        </div>
        <div>
          <h1 className="page-title">Authentification à deux facteurs</h1>
          <p className="page-subtitle">État de la sécurité de votre compte</p>
        </div>
      </div>

      {isLoading ? (
        <div className="flex justify-center py-12"><Loader2 className="w-8 h-8 animate-spin text-primary-500" /></div>
      ) : error ? (
        <div className="glass-card p-6 text-red-400">{getErrorMessage(error)}</div>
      ) : !status ? (
        <div className="glass-card p-10 text-center text-gray-500">Impossible de récupérer le statut 2FA</div>
      ) : (
        <div className="max-w-lg mx-auto space-y-6">
          <div className="glass-card p-6 text-center">
            <div className={`mx-auto mb-4 p-4 rounded-full ${status.enabled ? 'bg-green-500/20' : 'bg-yellow-500/20'}`}>
              {status.enabled ? <CheckCircle2 className="w-12 h-12 text-green-400" /> : <XCircle className="w-12 h-12 text-yellow-400" />}
            </div>
            <h2 className="text-xl font-bold text-gray-800 dark:text-gray-200 mb-2">
              {status.enabled ? '2FA Activée' : '2FA Non activée'}
            </h2>
            <p className="text-sm text-gray-500">
              {status.enabled
                ? 'Votre compte est protégé par l\'authentification à deux facteurs.'
                : 'Activez la 2FA pour sécuriser davantage votre compte.'}
            </p>
          </div>

          <div className="glass-card p-5 space-y-4">
            <h3 className="text-sm font-semibold text-gray-800 dark:text-gray-200">Détails</h3>
            <div className="flex items-center justify-between py-2 border-b border-white/10">
              <span className="text-sm text-gray-500 flex items-center gap-2"><Smartphone className="w-4 h-4" /> Méthode</span>
              <span className="text-sm font-medium text-gray-800 dark:text-gray-200">{status.method ?? 'Non configuré'}</span>
            </div>
            <div className="flex items-center justify-between py-2 border-b border-white/10">
              <span className="text-sm text-gray-500">Statut</span>
              <span className={`text-sm font-medium ${status.enabled ? 'text-green-400' : 'text-yellow-400'}`}>{status.enabled ? 'Activée' : 'Désactivée'}</span>
            </div>
            {status.lastVerified && (
              <div className="flex items-center justify-between py-2 border-b border-white/10">
                <span className="text-sm text-gray-500">Dernière vérification</span>
                <span className="text-sm text-gray-800 dark:text-gray-200">{new Date(status.lastVerified).toLocaleDateString('fr-FR')}</span>
              </div>
            )}
            {status.backupCodesCount != null && (
              <div className="flex items-center justify-between py-2">
                <span className="text-sm text-gray-500">Codes de secours</span>
                <span className="text-sm font-medium text-gray-800 dark:text-gray-200">{status.backupCodesCount} restants</span>
              </div>
            )}
          </div>
        </div>
      )}
    </div>
  );
}
