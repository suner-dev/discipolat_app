import { useQuery } from '@tanstack/react-query';
import { useState } from 'react';
import api from '@/lib/api';
import type { Family } from '@/types';
import { BarChart3, Users, Heart, TrendingUp, AlertTriangle, Loader2, ArrowLeft } from 'lucide-react';
import { Link } from 'react-router-dom';

export default function CompareFamiliesPage() {
  const [selectedFamilies, setSelectedFamilies] = useState<string[]>([]);

  const { data: families } = useQuery({
    queryKey: ['families', 'all'],
    queryFn: async () => {
      const res = await api.get('/families?size=100');
      return res.data.content as Family[];
    },
  });

  interface FamilyComparison {
    familyId: string;
    nom: string;
    totalAmes: number;
    amesActives: number;
    amesEnDifficulte: number;
    amesEnVeille: number;
    nombreFaiseurs: number;
    tauxRetention: number;
    [key: string]: any;
  }

  const { data: comparison, isLoading } = useQuery({
    queryKey: ['families', 'compare', selectedFamilies],
    queryFn: async () => {
      const res = await api.post('/families/compare', { familyIds: selectedFamilies });
      return res.data as FamilyComparison[];
    },
    enabled: selectedFamilies.length >= 2,
  });

  const toggleFamily = (id: string) => {
    setSelectedFamilies(prev =>
      prev.includes(id) ? prev.filter(f => f !== id) : [...prev, id]
    );
  };

  const metricKeys = comparison && comparison.length > 0
    ? Object.keys(comparison[0]).filter(k => k !== 'familyId' && k !== 'nom')
    : [];

  const metricLabels: Record<string, string> = {
    totalAmes: 'Total âmes',
    amesActives: 'Âmes actives',
    amesEnDifficulte: 'Âmes en difficulté',
    amesEnVeille: 'Âmes en veille',
    nombreFaiseurs: 'Nombre faiseurs',
    tauxRetention: 'Taux de rétention',
  };

  const metricIcons: Record<string, React.ReactNode> = {
    totalAmes: <Heart className="w-4 h-4 text-rose-500" />,
    amesActives: <Heart className="w-4 h-4 text-green-500" />,
    amesEnDifficulte: <AlertTriangle className="w-4 h-4 text-amber-500" />,
    amesEnVeille: <TrendingUp className="w-4 h-4 text-gray-500" />,
    nombreFaiseurs: <Users className="w-4 h-4 text-blue-500" />,
    tauxRetention: <TrendingUp className="w-4 h-4 text-indigo-500" />,
  };

  return (
    <div className="page-container">
      <div className="page-header">
        <Link to="/families" className="btn-ghost btn-sm mb-2">
          <ArrowLeft className="w-4 h-4" />
          Retour aux familles
        </Link>
        <div className="flex items-center gap-3">
          <div className="p-3 rounded-xl bg-gradient-to-br from-blue-500 to-indigo-500 text-white shadow-lg">
            <BarChart3 className="w-6 h-6" />
          </div>
          <div>
            <h1 className="page-title">Comparer les familles</h1>
            <p className="page-subtitle">Sélectionnez 2 familles ou plus pour comparer leurs KPIs</p>
          </div>
        </div>
      </div>

      <div className="glass-card p-6 mb-6">
        <h2 className="text-sm font-semibold text-gray-900 dark:text-gray-100 mb-3">
          Sélectionner les familles à comparer ({selectedFamilies.length} sélectionnée{selectedFamilies.length > 1 ? 's' : ''})
        </h2>
        <div className="flex flex-wrap gap-2">
          {families?.map((fam) => (
            <button
              key={fam.id}
              onClick={() => toggleFamily(fam.id)}
              className={`px-3 py-1.5 rounded-lg text-sm font-medium transition-all ${
                selectedFamilies.includes(fam.id)
                  ? 'bg-primary-500 text-white shadow-md'
                  : 'bg-gray-100 dark:bg-gray-800 text-gray-600 dark:text-gray-400 hover:bg-gray-200'
              }`}
            >
              {fam.nom}
            </button>
          ))}
        </div>
      </div>

      {isLoading && (
        <div className="flex justify-center py-12">
          <Loader2 className="w-8 h-8 animate-spin text-primary-500" />
        </div>
      )}

      {comparison && selectedFamilies.length >= 2 && (
        <div className="overflow-x-auto">
          <table className="data-table glass-card">
            <thead>
              <tr>
                <th className="text-left">Métrique</th>
                {comparison.map((fam) => (
                  <th key={fam.familyId} className="text-center">{fam.nom}</th>
                ))}
              </tr>
            </thead>
            <tbody>
              {metricKeys.map((key) => (
                <tr key={key}>
                  <td className="font-medium text-gray-900 dark:text-gray-100">
                    <div className="flex items-center gap-2">
                      {metricIcons[key] || <BarChart3 className="w-4 h-4 text-gray-400" />}
                      {metricLabels[key] || key.replace(/([A-Z])/g, ' $1').replace(/^./, s => s.toUpperCase())}
                    </div>
                  </td>
                  {comparison.map((fam) => (
                    <td key={fam.familyId} className="text-center font-semibold">
                      {key === 'tauxRetention' ? `${fam[key]}%` : fam[key]}
                    </td>
                  ))}
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {selectedFamilies.length < 2 && !isLoading && (
        <div className="glass-card p-10 text-center">
          <BarChart3 className="w-12 h-12 mx-auto mb-3 text-gray-300 dark:text-gray-600" />
          <p className="text-gray-500 dark:text-gray-400">Sélectionnez au moins 2 familles pour afficher la comparaison</p>
        </div>
      )}
    </div>
  );
}
