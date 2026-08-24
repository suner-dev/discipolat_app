import React, { useState, useEffect } from 'react';
import api from '@/lib/api';
import SkeletonLoader from '@/components/shared/SkeletonLoader';
import EmptyState from '@/components/shared/EmptyState';
import Toast from '@/components/shared/Toast';
import { MapPin, Plus, CheckCircle2, Clock, Calendar } from 'lucide-react';

interface Visit {
  id: string;
  membreId: string;
  visiteurId: string;
  motif: string;
  statut: string;
  prévuLe: string;
  réaliséLe?: string;
  notes?: string;
  autoGénéré: boolean;
}

export default function PastoralVisitsPage() {
  const [visits, setVisits] = useState<Visit[]>([]);
  const [loading, setLoading] = useState(true);
  const [start, setStart] = useState(() => { const d = new Date(); return d.toISOString().slice(0, 10); });
  const [end, setEnd] = useState(() => { const d = new Date(); d.setDate(d.getDate() + 14); return d.toISOString().slice(0, 10); });

  useEffect(() => { loadVisits(); }, [start, end]);

  const loadVisits = async () => {
    try {
      setLoading(true);
      const res = await api.get(`/pastoral-visits?start=${start}T00:00:00&end=${end}T23:59:59`);
      setVisits(res.data || []);
    } catch { setVisits([]); } finally { setLoading(false); }
  };

  const complete = async (id: string) => {
    try { await api.patch(`/pastoral-visits/${id}/complete`, { notes: 'Visite réalisée' }); Toast.success('Visit marquée comme réalisée'); loadVisits(); }
    catch { Toast.error('Erreur'); }
  };

  return (
    <div className="min-h-screen p-4 md:p-8 max-w-7xl mx-auto">
      <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4 mb-8">
        <div>
          <h1 className="text-2xl md:text-3xl font-bold text-gray-900 dark:text-white flex items-center gap-3">
            <MapPin className="w-8 h-8 text-rose-500" /> Visites Pastorales
          </h1>
          <p className="text-gray-500 dark:text-gray-400 mt-1">Planification automatique des visites</p>
        </div>
      </div>

      <div className="flex gap-4 mb-6">
        <div><label className="block text-xs text-gray-500 mb-1">Du</label>
          <input type="date" value={start} onChange={e => setStart(e.target.value)}
            className="px-4 py-2 rounded-xl border border-gray-200 dark:border-white/10 bg-white dark:bg-white/5 text-sm" /></div>
        <div><label className="block text-xs text-gray-500 mb-1">Au</label>
          <input type="date" value={end} onChange={e => setEnd(e.target.value)}
            className="px-4 py-2 rounded-xl border border-gray-200 dark:border-white/10 bg-white dark:bg-white/5 text-sm" /></div>
      </div>

      {loading ? <SkeletonLoader lines={5} variant="table" /> :
        visits.length === 0 ? (
          <EmptyState icon={<MapPin className="w-8 h-8 text-gray-400" />}
            title="Aucune visite planifiée" message="Générez des visites automatiquement ou créez-en manuellement" />
        ) : (
          <div className="space-y-3">
            {visits.map(v => (
              <div key={v.id} className="bg-white dark:bg-white/5 rounded-xl p-4 border border-gray-200 dark:border-white/10 flex items-center justify-between">
                <div className="flex-1">
                  <div className="flex items-center gap-2 mb-1">
                    {v.statut === 'RÉalisée' ? <CheckCircle2 className="w-4 h-4 text-green-500" /> : <Clock className="w-4 h-4 text-amber-500" />}
                    <h3 className="font-medium text-gray-900 dark:text-white text-sm">
                      Membre: {v.membreId.slice(0, 8)}...
                    </h3>
                    {v.autoGénéré && <span className="px-2 py-0.5 rounded-full text-xs bg-blue-100 text-blue-700">Auto</span>}
                  </div>
                  <div className="text-xs text-gray-500 flex items-center gap-3">
                    <span className="flex items-center gap-1"><Calendar className="w-3 h-3" />{new Date(v.prévuLe).toLocaleDateString('fr-FR')}</span>
                    <span>Motif: {v.motif}</span>
                  </div>
                </div>
                {v.statut === 'PLANIFIÉE' && (
                  <button onClick={() => complete(v.id)}
                    className="px-3 py-1.5 rounded-lg bg-green-500 text-white text-xs font-medium hover:bg-green-600">
                    Réalisée
                  </button>
                )}
              </div>
            ))}
          </div>
        )}
    </div>
  );
}
