import { useState } from 'react';
import { useI18n } from '@/i18n';
import { Activity, AlertTriangle, TrendingUp, TrendingDown, Minus, Zap, Bell } from 'lucide-react';

interface KpiData { name: string; category: string; value: number; unit: string; trend: string; change: number; isAlert: boolean; narrative: string; }
const CATEGORIES = ['PRESENCE','GROWTH','ENGAGEMENT','SPIRITUAL','FINANCES','OUTREACH','ALERTS','OPERATIONS','QUALITY'];
const MOCK_KPIS: KpiData[] = [
  { name: 'Taux de présence global', category: 'PRESENCE', value: 72, unit: '%', trend: 'UP', change: 3.2, isAlert: false, narrative: 'En hausse (+3.2%)' },
  { name: 'Croissance effectifs', category: 'GROWTH', value: 5.1, unit: '%', trend: 'UP', change: 1.8, isAlert: false, narrative: 'En hausse (+1.8%)' },
  { name: 'Score d\'engagement moyen', category: 'ENGAGEMENT', value: 68, unit: '/100', trend: 'DOWN', change: -4.5, isAlert: true, narrative: '⚠️ Chute significative (-4.5%)' },
  { name: 'Score spirituel moyen', category: 'SPIRITUAL', value: 55, unit: '/100', trend: 'DOWN', change: -2.1, isAlert: false, narrative: 'En baisse (-2.1%)' },
  { name: 'Dons moyens/membre', category: 'FINANCES', value: 4250, unit: 'FCFA', trend: 'UP', change: 12, isAlert: false, narrative: 'En hausse (+12%)' },
  { name: 'Alertes actives', category: 'ALERTS', value: 7, unit: '', trend: 'UP', change: 3, isAlert: true, narrative: '⚠️ Augmentation des alertes' },
  { name: 'Membres à risque décrochage', category: 'ALERTS', value: 5, unit: '', trend: 'UP', change: 2, isAlert: true, narrative: '⚠️ 2 nouveaux membres à risque' },
  { name: 'Événements ce mois', category: 'OPERATIONS', value: 8, unit: '', trend: 'STABLE', change: 0, isAlert: false, narrative: 'Stable' },
  { name: 'Score satisfaction fidèles', category: 'QUALITY', value: 82, unit: '/100', trend: 'UP', change: 5, isAlert: false, narrative: 'En hausse (+5)' },
  { name: 'Conversions/mois', category: 'OUTREACH', value: 8, unit: '', trend: 'UP', change: 3, isAlert: false, narrative: 'En hausse (+3)' },
];

export default function IntelligenceCenterPage() {
  const { t } = useI18n();
  const [selectedCat, setSelectedCat] = useState<string>('ALL');
  const filtered = selectedCat === 'ALL' ? MOCK_KPIS : MOCK_KPIS.filter(k => k.category === selectedCat);
  const alerts = MOCK_KPIS.filter(k => k.isAlert);
  const trendIcon = (trend: string) => trend === 'UP' ? <TrendingUp className="w-4 h-4 text-green-400" /> : trend === 'DOWN' ? <TrendingDown className="w-4 h-4 text-red-400" /> : <Minus className="w-4 h-4 text-gray-400" />;

  return (
    <div className="space-y-6 p-6">
      <h1 className="text-2xl font-bold text-white flex items-center gap-2"><Activity className="text-cyan-400" /> {t('intelligence.title') || 'Centre d\'Intelligence (50+ KPIs)'}</h1>

      {/* Alert banner */}
      {alerts.length > 0 && (
        <div className="bg-red-500/10 border border-red-500/30 rounded-xl p-4 flex items-center gap-3">
          <AlertTriangle className="w-5 h-5 text-red-400" />
          <span className="text-red-300 text-sm font-medium">{alerts.length} alerte{alerts.length > 1 ? 's' : ''} active{alerts.length > 1 ? 's' : ''}</span>
        </div>
      )}

      {/* Category filter */}
      <div className="flex flex-wrap gap-2">
        <button onClick={() => setSelectedCat('ALL')} className={`px-3 py-1.5 rounded-lg text-xs font-medium transition ${selectedCat === 'ALL' ? 'bg-blue-600 text-white' : 'bg-white/5 text-gray-400 hover:bg-white/10'}`}>Tout ({MOCK_KPIS.length})</button>
        {CATEGORIES.map(cat => (
          <button key={cat} onClick={() => setSelectedCat(cat)} className={`px-3 py-1.5 rounded-lg text-xs font-medium transition ${selectedCat === cat ? 'bg-blue-600 text-white' : 'bg-white/5 text-gray-400 hover:bg-white/10'}`}>{cat} ({MOCK_KPIS.filter(k => k.category === cat).length})</button>
        ))}
      </div>

      {/* KPI Grid */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-4">
        {filtered.map((kpi, i) => (
          <div key={i} className={`bg-white/5 backdrop-blur rounded-2xl p-4 border transition hover:scale-[1.02] ${kpi.isAlert ? 'border-red-500/40' : 'border-white/10 hover:border-white/20'}`}>
            <div className="flex items-start justify-between mb-2">
              <span className="text-xs text-gray-400 font-medium uppercase tracking-wide">{kpi.category}</span>
              {trendIcon(kpi.trend)}
            </div>
            <div className="text-2xl font-bold text-white mb-1">{typeof kpi.value === 'number' ? kpi.value.toLocaleString() : kpi.value}<span className="text-sm font-normal text-gray-400 ml-1">{kpi.unit}</span></div>
            <div className="text-sm text-gray-300 mb-2">{kpi.name}</div>
            <div className={`text-xs ${kpi.change > 0 ? 'text-green-400' : kpi.change < 0 ? 'text-red-400' : 'text-gray-400'}`}>{kpi.change > 0 ? '+' : ''}{kpi.change}%</div>
            {kpi.isAlert && <div className="mt-2 text-xs text-red-400 flex items-center gap-1"><AlertTriangle className="w-3 h-3" /> Alerte</div>}
          </div>
        ))}
      </div>
    </div>
  );
}
