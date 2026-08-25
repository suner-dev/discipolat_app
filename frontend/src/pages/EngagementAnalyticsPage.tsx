import { useState } from 'react';
import { useI18n } from '@/i18n';
import { BarChart3, TrendingUp, TrendingDown } from 'lucide-react';

interface Metric { name: string; category: string; value: number; change: number; unit: string; }

export default function EngagementAnalyticsPage() {
  const { t } = useI18n();
  const MOCK: Metric[] = [
    { name: 'Pages vues totales', category: 'pages', value: 12450, change: 15.2, unit: '' },
    { name: 'Utilisateurs actifs/jour', category: 'pages', value: 89, change: 5.3, unit: '' },
    { name: 'Actions sur l\'app', category: 'actions', value: 3280, change: -2.1, unit: '' },
    { name: 'Temps moyen session', category: 'actions', value: 12.5, change: 8.7, unit: 'min' },
    { name: 'Taux conversion inscription', category: 'funnels', value: 34, change: 12, unit: '%' },
    { name: 'Taux rétention 30j', category: 'retention', value: 72, change: -3.4, unit: '%' },
    { name: 'Messages envoyés/mois', category: 'actions', value: 567, change: 22, unit: '' },
    { name: 'Notes de prière créées', category: 'actions', value: 189, change: 18, unit: '' },
  ];

  return (
    <div className="space-y-6 p-6">
      <h1 className="text-2xl font-bold text-white flex items-center gap-2"><BarChart3 className="text-indigo-400" /> {t('engagement.title') || 'Analytics d\'engagement'}</h1>
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
        {MOCK.map((m, i) => (
          <div key={i} className="bg-white/5 backdrop-blur rounded-2xl p-4 border border-white/10 hover:border-indigo-500/30 transition">
            <div className="text-xs text-gray-400 uppercase tracking-wide mb-1">{m.category}</div>
            <div className="text-2xl font-bold text-white">{m.value.toLocaleString()}<span className="text-sm font-normal text-gray-400 ml-1">{m.unit}</span></div>
            <div className="text-sm text-gray-300 mb-2">{m.name}</div>
            <div className={`text-xs flex items-center gap-1 ${m.change > 0 ? 'text-green-400' : 'text-red-400'}`}>
              {m.change > 0 ? <TrendingUp className="w-3 h-3" /> : <TrendingDown className="w-3 h-3" />}
              {m.change > 0 ? '+' : ''}{m.change}%
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}
