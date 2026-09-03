import { useQuery } from '@tanstack/react-query';
import api, { getErrorMessage } from '@/lib/api';
import { ArrowLeftRight, Loader2, TrendingUp } from 'lucide-react';

interface Exchange {
  id: string;
  fromType: string;
  toType: string;
  amount: number;
  rate: number;
  status: string;
  createdAt: string;
}

interface ExchangeRate {
  fromType: string;
  toType: string;
  rate: number;
  lastUpdated: string;
}

export default function AidExchangePage() {
  const { data: exchanges = [], isLoading } = useQuery({
    queryKey: ['aid-exchange'],
    queryFn: async () => (await api.get('/aid/exchange')).data as Exchange[],
  });

  const { data: rates = [], isLoading: loadingRates } = useQuery({
    queryKey: ['aid-exchange-rates'],
    queryFn: async () => (await api.get('/aid/exchange/rates')).data as ExchangeRate[],
  });

  return (
    <div className="page-container">
      <div className="page-header">
        <div className="p-3 rounded-xl bg-gradient-to-br from-cyan-500 to-blue-600 text-white shadow-lg">
          <ArrowLeftRight className="w-6 h-6" />
        </div>
        <div>
          <h1 className="page-title">Échange d'aide</h1>
          <p className="page-subtitle">Taux et transactions d'échange de ressources</p>
        </div>
      </div>

      <div className="mb-6">
        <h2 className="text-lg font-semibold text-gray-800 dark:text-gray-200 flex items-center gap-2 mb-4">
          <TrendingUp className="w-5 h-5 text-cyan-400" /> Taux de change
        </h2>
        {loadingRates ? (
          <div className="flex justify-center py-6"><Loader2 className="w-6 h-6 animate-spin text-primary-500" /></div>
        ) : rates.length === 0 ? (
          <div className="glass-card p-6 text-center text-gray-500 text-sm">Aucun taux disponible</div>
        ) : (
          <div className="grid gap-3 md:grid-cols-2 lg:grid-cols-3">
            {rates.map((r, i) => (
              <div key={i} className="glass-card p-4">
                <div className="flex items-center justify-between mb-2">
                  <span className="text-sm font-medium text-gray-800 dark:text-gray-200">{r.fromType}</span>
                  <ArrowLeftRight className="w-4 h-4 text-cyan-400" />
                  <span className="text-sm font-medium text-gray-800 dark:text-gray-200">{r.toType}</span>
                </div>
                <p className="text-2xl font-bold text-cyan-400 text-center">{r.rate}</p>
                <p className="text-[11px] text-gray-500 text-center mt-1">Mis à jour: {new Date(r.lastUpdated).toLocaleDateString('fr-FR')}</p>
              </div>
            ))}
          </div>
        )}
      </div>

      <h2 className="text-lg font-semibold text-gray-800 dark:text-gray-200 mb-4">Transactions récentes</h2>
      {isLoading ? (
        <div className="flex justify-center py-6"><Loader2 className="w-6 h-6 animate-spin text-primary-500" /></div>
      ) : exchanges.length === 0 ? (
        <div className="glass-card p-10 text-center text-gray-500">Aucune transaction d'échange</div>
      ) : (
        <div className="space-y-3">
          {exchanges.map((ex) => (
            <div key={ex.id} className="glass-card p-5 flex items-center justify-between">
              <div>
                <div className="flex items-center gap-2 mb-1">
                  <span className="text-sm font-medium text-gray-800 dark:text-gray-200">{ex.amount} {ex.fromType}</span>
                  <ArrowLeftRight className="w-3 h-3 text-cyan-400" />
                  <span className="text-sm font-medium text-gray-800 dark:text-gray-200">{(ex.amount * ex.rate).toFixed(2)} {ex.toType}</span>
                </div>
                <p className="text-[11px] text-gray-500">{new Date(ex.createdAt).toLocaleDateString('fr-FR')} — Taux: {ex.rate}</p>
              </div>
              <span className={`px-2 py-0.5 rounded-full text-xs font-medium ${ex.status === 'COMPLETED' ? 'text-green-400 bg-green-500/20' : ex.status === 'PENDING' ? 'text-yellow-400 bg-yellow-500/20' : 'text-gray-400 bg-gray-500/20'}`}>{ex.status}</span>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
