import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { useParams, Link } from 'react-router-dom';
import api from '@/lib/api';
import {
  ArrowLeft, Users, BarChart3, FileText, Loader2,
  CheckCircle2, Clock, AlertCircle,
} from 'lucide-react';

export default function FamilyFaiseurPerformancePage() {
  const { id } = useParams<{ id: string }>();
  const today = new Date().toISOString().split('T')[0];
  const [semaine, setSemaine] = useState(today);

  const { data: family } = useQuery({
    queryKey: ['family', id],
    queryFn: async () => {
      const res = await api.get(`/families/${id}`);
      return res.data as any;
    },
    enabled: !!id,
  });

  const { data: performance, isLoading } = useQuery({
    queryKey: ['family', id, 'faiseur-performance', semaine],
    queryFn: async () => {
      const res = await api.get(`/families/${id}/faiseur-performance?semaine=${semaine}`);
      return res.data as any[];
    },
    enabled: !!id,
  });

  const totalAmes = performance?.reduce((sum: number, p: any) => sum + (p.totalAmes || 0), 0) || 0;
  const totalRapports = performance?.reduce((sum: number, p: any) => sum + (p.rapportsSoumis || 0), 0) || 0;

  if (isLoading) {
    return (
      <div className="page-container flex items-center justify-center min-h-[60vh]">
        <Loader2 className="w-8 h-8 animate-spin text-primary-500" />
      </div>
    );
  }

  return (
    <div className="page-container">
      <div className="page-header">
        <Link to={`/families/${id}`} className="btn-ghost btn-sm mb-2 inline-flex">
          <ArrowLeft className="w-4 h-4" /> Retour à la famille
        </Link>
        <div className="flex items-center justify-between gap-4">
          <div className="flex items-center gap-3">
            <div className="p-3 rounded-xl bg-gradient-to-br from-violet-500 to-purple-500 text-white shadow-lg">
              <BarChart3 className="w-6 h-6" />
            </div>
            <div>
              <h1 className="page-title">Performance des faiseurs</h1>
              <p className="page-subtitle">{family?.nom || 'Chargement...'}</p>
            </div>
          </div>
          <div className="flex items-center gap-2">
            <span className="text-xs text-gray-500">Semaine:</span>
            <input
              type="date"
              value={semaine}
              onChange={(e) => setSemaine(e.target.value)}
              className="input py-1.5 text-sm w-auto"
            />
          </div>
        </div>
      </div>

      {performance && (
        <>
          <div className="grid grid-cols-1 sm:grid-cols-3 gap-4 mb-6">
            <div className="stat-card">
              <span className="stat-label">Faiseurs</span>
              <div className="flex items-center gap-2 mt-1">
                <Users className="w-5 h-5 text-violet-500" />
                <p className="stat-value text-2xl">{performance.length}</p>
              </div>
            </div>
            <div className="stat-card">
              <span className="stat-label">Âmes suivies</span>
              <div className="flex items-center gap-2 mt-1">
                <Users className="w-5 h-5 text-rose-500" />
                <p className="stat-value text-2xl">{totalAmes}</p>
              </div>
            </div>
            <div className="stat-card">
              <span className="stat-label">Rapports soumis</span>
              <div className="flex items-center gap-2 mt-1">
                <FileText className="w-5 h-5 text-primary-500" />
                <p className="stat-value text-2xl">{totalRapports}/{totalAmes}</p>
              </div>
            </div>
          </div>

          <div className="glass-card p-6">
            <h2 className="text-lg font-semibold text-gray-900 dark:text-gray-100 mb-4 flex items-center gap-2">
              <Users className="w-5 h-5 text-violet-500" />
              Détail par faiseur
            </h2>
            {performance.length > 0 ? (
              <div className="space-y-4">
                {performance.map((p: any, i: number) => (
                  <div key={p.faiseurId} className="glass-card p-4 animate-slide-up" style={{ animationDelay: `${i * 60}ms` }}>
                    <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 mb-3">
                      <div className="flex items-center gap-3">
                        <div className="w-10 h-10 rounded-xl bg-gradient-to-br from-violet-400 to-violet-600 flex items-center justify-center text-white font-bold shadow-sm">
                          {p.faiseurNom?.charAt(0) || '?'}
                        </div>
                        <div>
                          <p className="font-semibold text-gray-900 dark:text-gray-100">{p.faiseurNom}</p>
                          <p className="text-xs text-gray-500">{p.totalAmes} âmes suivies</p>
                        </div>
                      </div>
                      <div className="flex items-center gap-3">
                        <span className={`badge text-xs ${p.tauxSoumission >= 80 ? 'badge-success' : p.tauxSoumission >= 50 ? 'badge-warning' : 'badge-inactive'}`}>
                          {p.rapportsSoumis}/{p.totalAmes} rapports
                        </span>
                        <span className={`badge text-xs ${p.tauxPresence >= 70 ? 'badge-success' : p.tauxPresence >= 40 ? 'badge-warning' : 'badge-inactive'}`}>
                          {p.tauxPresence}% présence
                        </span>
                      </div>
                    </div>
                    <div className="grid grid-cols-2 sm:grid-cols-4 gap-3">
                      <div className="p-2 rounded-lg bg-white/30 dark:bg-gray-800/30">
                        <span className="text-[10px] text-gray-500">Actifs</span>
                        <p className="text-sm font-semibold text-green-600">{p.actifs}</p>
                      </div>
                      <div className="p-2 rounded-lg bg-white/30 dark:bg-gray-800/30">
                        <span className="text-[10px] text-gray-500">En intégration</span>
                        <p className="text-sm font-semibold text-blue-600">{p.enIntegration}</p>
                      </div>
                      <div className="p-2 rounded-lg bg-white/30 dark:bg-gray-800/30">
                        <span className="text-[10px] text-gray-500">En veille</span>
                        <p className="text-sm font-semibold text-amber-600">{p.enVeille}</p>
                      </div>
                      <div className="p-2 rounded-lg bg-white/30 dark:bg-gray-800/30">
                        <span className="text-[10px] text-gray-500">Présents</span>
                        <p className="text-sm font-semibold text-primary-600">{p.totalPresents}</p>
                      </div>
                    </div>
                    <div className="mt-3 space-y-1">
                      <div className="flex items-center gap-2">
                        <span className="text-[10px] text-gray-500 w-28">Soumission</span>
                        <div className="progress-bar flex-1">
                          <div className="progress-bar-fill" style={{ width: `${p.tauxSoumission}%` }} />
                        </div>
                        <span className="text-[10px] font-medium">{p.tauxSoumission}%</span>
                      </div>
                      <div className="flex items-center gap-2">
                        <span className="text-[10px] text-gray-500 w-28">Présence</span>
                        <div className="progress-bar flex-1">
                          <div className="progress-bar-fill bg-green-500" style={{ width: `${p.tauxPresence}%` }} />
                        </div>
                        <span className="text-[10px] font-medium">{p.tauxPresence}%</span>
                      </div>
                    </div>
                  </div>
                ))}
              </div>
            ) : (
              <div className="text-center py-8 text-gray-500">
                <BarChart3 className="w-12 h-12 mx-auto mb-3 opacity-40" />
                <p>Aucun faiseur dans cette famille</p>
              </div>
            )}
          </div>
        </>
      )}

      {!performance && !isLoading && (
        <div className="glass-card p-10 text-center">
          <BarChart3 className="w-12 h-12 text-gray-300 dark:text-gray-600 mx-auto mb-4" />
          <p className="text-gray-500">Aucune donnée de performance disponible</p>
        </div>
      )}
    </div>
  );
}
