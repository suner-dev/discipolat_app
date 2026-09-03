import { useQuery } from '@tanstack/react-query';
import api, { getErrorMessage } from '@/lib/api';
import { Activity, Loader2, CheckCircle2, XCircle, Clock, Cpu } from 'lucide-react';

interface AiHealth {
  status: string;
  uptime?: number;
  lastCheck?: string;
  modelVersion?: string;
  latencyMs?: number;
  requestsToday?: number;
  errorRate?: number;
  services?: Array<{ name: string; status: string; latencyMs?: number }>;
}

export default function AiHealthPage() {
  const { data: health, isLoading, error } = useQuery({
    queryKey: ['ai-health'],
    queryFn: async () => (await api.get('/ai/health')).data as AiHealth,
    refetchInterval: 30000,
  });

  const isHealthy = health?.status === 'healthy' || health?.status === 'ok';

  return (
    <div className="page-container">
      <div className="page-header">
        <div className="p-3 rounded-xl bg-gradient-to-br from-green-500 to-emerald-600 text-white shadow-lg">
          <Activity className="w-6 h-6" />
        </div>
        <div>
          <h1 className="page-title">Santé IA</h1>
          <p className="page-subtitle">Tableau de bord de l'état du système IA</p>
        </div>
      </div>

      {isLoading ? (
        <div className="flex justify-center py-12"><Loader2 className="w-8 h-8 animate-spin text-primary-500" /></div>
      ) : error ? (
        <div className="glass-card p-6 text-red-400">{getErrorMessage(error)}</div>
      ) : !health ? (
        <div className="glass-card p-10 text-center text-gray-500">Impossible de récupérer l'état de santé IA</div>
      ) : (
        <>
          <div className="glass-card p-6 mb-6 text-center">
            <div className={`mx-auto mb-4 p-4 rounded-full ${isHealthy ? 'bg-green-500/20' : 'bg-red-500/20'}`}>
              {isHealthy ? <CheckCircle2 className="w-12 h-12 text-green-400" /> : <XCircle className="w-12 h-12 text-red-400" />}
            </div>
            <h2 className="text-xl font-bold text-gray-800 dark:text-gray-200 mb-1">Système {isHealthy ? 'opérationnel' : 'dégradé'}</h2>
            <p className="text-sm text-gray-500">Statut: {health.status}</p>
          </div>

          <div className="grid grid-cols-2 md:grid-cols-4 gap-4 mb-6">
            {[
              { label: 'Uptime', value: health.uptime != null ? `${health.uptime}%` : '—', icon: Clock },
              { label: 'Latence', value: health.latencyMs != null ? `${health.latencyMs}ms` : '—', icon: Activity },
              { label: 'Requêtes/jour', value: health.requestsToday?.toLocaleString() ?? '—', icon: Cpu },
              { label: 'Taux d\'erreur', value: health.errorRate != null ? `${health.errorRate}%` : '—', icon: XCircle },
            ].map(({ label, value, icon: Icon }) => (
              <div key={label} className="stat-card">
                <Icon className="w-5 h-5 text-primary-500 opacity-80" />
                <p className="stat-value">{value}</p>
                <p className="stat-label">{label}</p>
              </div>
            ))}
          </div>

          {(health.modelVersion || health.lastCheck) && (
            <div className="glass-card p-5 mb-6">
              <div className="flex justify-between text-sm">
                {health.modelVersion && <span className="text-gray-500">Modèle: <span className="text-gray-800 dark:text-gray-200 font-medium">{health.modelVersion}</span></span>}
                {health.lastCheck && <span className="text-gray-500">Dernière vérif: <span className="text-gray-800 dark:text-gray-200">{new Date(health.lastCheck).toLocaleString('fr-FR')}</span></span>}
              </div>
            </div>
          )}

          {health.services && health.services.length > 0 && (
            <div>
              <h2 className="text-lg font-semibold text-gray-800 dark:text-gray-200 mb-4">Services</h2>
              <div className="space-y-2">
                {health.services.map((svc) => (
                  <div key={svc.name} className="glass-card p-4 flex items-center justify-between">
                    <div className="flex items-center gap-3">
                      {svc.status === 'healthy' ? <CheckCircle2 className="w-4 h-4 text-green-400" /> : <XCircle className="w-4 h-4 text-red-400" />}
                      <span className="text-sm font-medium text-gray-800 dark:text-gray-200">{svc.name}</span>
                    </div>
                    <div className="flex items-center gap-3">
                      {svc.latencyMs != null && <span className="text-xs text-gray-400">{svc.latencyMs}ms</span>}
                      <span className={`px-2 py-0.5 rounded-full text-xs ${svc.status === 'healthy' ? 'text-green-400 bg-green-500/20' : 'text-red-400 bg-red-500/20'}`}>{svc.status}</span>
                    </div>
                  </div>
                ))}
              </div>
            </div>
          )}
        </>
      )}
    </div>
  );
}
