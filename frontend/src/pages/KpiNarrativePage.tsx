import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { BookOpen, RefreshCw, Loader2, Sparkles, Filter } from 'lucide-react';
import toast from 'react-hot-toast';
import api, { getErrorMessage } from '@/lib/api';
import EmptyState from '@/components/shared/EmptyState';
import SkeletonLoader from '@/components/shared/SkeletonLoader';

interface KpiNarrative {
  id: string;
  typeKPI: string;
  valeurActuelle: number;
  valeurPrécédente: number;
  période: string;
  narratif: string;
  departementId?: string;
  createdAt: string;
}

const KPI_TYPES = [
  'PRÉSENCE', 'ÉVANGÉLISATION', 'FORMATIONS', 'DIZAINES',
  'FINANCES', 'BAPTÊMES', 'VISITES', 'PRIÈRE',
];

export default function KpiNarrativePage() {
  const queryClient = useQueryClient();
  const [selectedType, setSelectedType] = useState<string>('');
  const [selectedPeriode, setSelectedPeriode] = useState('');
  const [showGenerate, setShowGenerate] = useState(false);
  const [kpiData, setKpiData] = useState<Record<string, string>>({});

  const { data: narratives = [], isLoading } = useQuery({
    queryKey: ['kpi-narrative', selectedType, selectedPeriode],
    queryFn: async () => {
      if (selectedType) return (await api.get<KpiNarrative[]>(`/kpi-narrative/type/${selectedType}`)).data;
      if (selectedPeriode) return (await api.get<KpiNarrative[]>(`/kpi-narrative/période/${selectedPeriode}`)).data;
      return (await api.get<KpiNarrative[]>('/kpi-narrative')).data;
    },
  });

  const generateAllMutation = useMutation({
    mutationFn: async () => {
      const payload: Record<string, number[]> = {};
      Object.entries(kpiData).forEach(([key, val]) => {
        if (val.trim()) {
          const nums = val.split(',').map(s => parseFloat(s.trim()) || 0);
          payload[key] = nums;
        }
      });
      return (await api.post('/kpi-narrative/generate-all', payload)).data;
    },
    onSuccess: () => {
      toast.success('Narratifs générés');
      setShowGenerate(false);
      setKpiData({});
      queryClient.invalidateQueries({ queryKey: ['kpi-narrative'] });
    },
    onError: (e: unknown) => toast.error(getErrorMessage(e)),
  });

  return (
    <div className="page-container">
      <div className="page-header">
        <div className="p-3 rounded-xl bg-gradient-to-br from-amber-500 to-orange-600 text-white shadow-lg">
          <BookOpen className="w-6 h-6" />
        </div>
        <div>
          <h1 className="page-title">Narratifs KPI</h1>
          <p className="page-subtitle">Génération automatique de narratifs pour vos indicateurs</p>
        </div>
        <div className="ml-auto flex gap-2">
          <button onClick={() => setShowGenerate(true)}
            className="px-4 py-2 rounded-xl bg-gradient-to-r from-amber-500 to-orange-500 text-white text-sm font-medium hover:from-amber-600 hover:to-orange-600 transition-all shadow-lg flex items-center gap-2">
            <Sparkles className="w-4 h-4" /> Générer
          </button>
        </div>
      </div>

      {/* Filters */}
      <div className="flex gap-3 mb-6 flex-wrap">
        <select value={selectedType} onChange={e => setSelectedType(e.target.value)}
          className="px-3 py-2 rounded-xl border border-gray-200 dark:border-white/10 bg-white dark:bg-white/5 text-sm">
          <option value="">Tous les types</option>
          {KPI_TYPES.map(t => <option key={t} value={t}>{t}</option>)}
        </select>
        <input type="text" value={selectedPeriode} onChange={e => setSelectedPeriode(e.target.value)}
          className="px-3 py-2 rounded-xl border border-gray-200 dark:border-white/10 bg-white dark:bg-white/5 text-sm"
          placeholder="Période (ex: 2024-S01)" />
      </div>

      {isLoading ? <SkeletonLoader lines={4} variant="card" /> :
        narratives.length === 0 ? (
          <EmptyState icon={<BookOpen className="w-8 h-8 text-gray-400" />}
            title="Aucun narratif"
            message="Générez des narratifs automatiques pour vos KPIs"
            action={{ label: 'Générer des narratifs', onClick: () => setShowGenerate(true) }} />
        ) : (
          <div className="space-y-4">
            {narratives.map(n => (
              <div key={n.id} className="bg-white dark:bg-white/5 rounded-xl p-5 border border-gray-200 dark:border-white/10">
                <div className="flex items-center justify-between mb-3">
                  <span className="px-2 py-0.5 rounded-full bg-amber-100 dark:bg-amber-500/20 text-amber-700 dark:text-amber-400 text-xs font-medium">
                    {n.typeKPI}
                  </span>
                  <div className="text-xs text-gray-400">
                    {n.valeurActuelle} vs {n.valeurPrécédente} · {n.période}
                  </div>
                </div>
                <p className="text-sm text-gray-700 dark:text-gray-300 leading-relaxed">{n.narratif}</p>
                <div className="text-xs text-gray-400 mt-2">{new Date(n.createdAt).toLocaleString('fr-FR')}</div>
              </div>
            ))}
          </div>
        )}

      {/* Generate modal */}
      {showGenerate && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
          <div className="fixed inset-0 bg-black/50 backdrop-blur-sm" onClick={() => setShowGenerate(false)} />
          <div className="relative bg-white dark:bg-gray-800 rounded-2xl shadow-2xl max-w-lg w-full p-6 border border-gray-200 dark:border-white/10">
            <h2 className="text-lg font-bold text-gray-900 dark:text-white mb-4">Générer tous les narratifs</h2>
            <p className="text-xs text-gray-500 mb-4">Entrez les valeurs actuelles et précédentes (séparées par une virgule)</p>
            <div className="space-y-3 max-h-64 overflow-y-auto">
              {KPI_TYPES.map(type => (
                <div key={type} className="flex items-center gap-3">
                  <label className="text-xs text-gray-600 dark:text-gray-400 w-28">{type}</label>
                  <input type="text" value={kpiData[type] || ''} onChange={e => setKpiData({ ...kpiData, [type]: e.target.value })}
                    className="flex-1 px-3 py-1.5 rounded-lg border border-gray-200 dark:border-white/10 bg-white dark:bg-white/5 text-sm"
                    placeholder="actuel, précédent" />
                </div>
              ))}
            </div>
            <div className="flex justify-end gap-3 mt-6">
              <button onClick={() => setShowGenerate(false)} className="px-4 py-2 rounded-xl border text-sm">Annuler</button>
              <button onClick={() => generateAllMutation.mutate()} disabled={generateAllMutation.isPending}
                className="px-4 py-2 rounded-xl bg-amber-500 text-white text-sm font-medium hover:bg-amber-600 flex items-center gap-2">
                {generateAllMutation.isPending && <Loader2 className="w-4 h-4 animate-spin" />}
                Générer
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
