import { useI18n } from '@/i18n';
import { Sparkles, AlertTriangle, TrendingUp, TrendingDown, Lightbulb, Eye, X } from 'lucide-react';

interface Insight { id: string; title: string; description: string; severity: string; category: string; recommendedAction: string; metricValue: string; metricChange: string; isRead: boolean; }

export default function ExecutiveInsightsPage() {
  const { t } = useI18n();
  const MOCK: Insight[] = [
    { id: '1', title: 'Tendance de présence en baisse', description: 'La présence moyenne a diminué de 8% sur les 3 dernières semaines, principalement chez les 18-25 ans.', severity: 'WARNING', category: 'ENGAGEMENT', recommendedAction: 'Organiser un événement ciblé jeunesse et contacter les absents.', metricValue: '62%', metricChange: '-8%', isRead: false },
    { id: '2', title: 'Opportunité : Nouveau groupe de maison', description: 'Le quartier Nord a 15 membres actifs sans groupe de maison.', severity: 'OPPORTUNITY', category: 'GROWTH', recommendedAction: 'Identifier un leader potentiel et planifier une réunion de lancement.', metricValue: '15', metricChange: '+15 membres', isRead: false },
    { id: '3', title: 'Finances : Dons en hausse de 12%', description: 'Les dons du mois ont augmenté de 12%. Tendance positive.', severity: 'INFO', category: 'FINANCE', recommendedAction: 'Continuer la communication sur les projets financés.', metricValue: '€4,250', metricChange: '+12%', isRead: true },
    { id: '4', title: 'Alerte : 5 membres à risque de décrochage', description: 'Cinq membres n\'ont pas participé depuis plus de 3 semaines.', severity: 'CRITICAL', category: 'RETENTION', recommendedAction: 'Contacter immédiatement ces membres par leurs faiseurs respectifs.', metricValue: '5', metricChange: '+2', isRead: false },
  ];
  const severityIcon = (s: string) => s === 'CRITICAL' ? <AlertTriangle className="w-5 h-5 text-red-400" /> : s === 'WARNING' ? <AlertTriangle className="w-5 h-5 text-orange-400" /> : s === 'OPPORTUNITY' ? <Lightbulb className="w-5 h-5 text-blue-400" /> : <Sparkles className="w-5 h-5 text-gray-400" />;
  const severityBorder = (s: string) => s === 'CRITICAL' ? 'border-red-500/40' : s === 'WARNING' ? 'border-orange-500/30' : s === 'OPPORTUNITY' ? 'border-blue-500/30' : 'border-white/10';

  return (
    <div className="space-y-6 p-6">
      <h1 className="text-2xl font-bold text-white flex items-center gap-2"><Sparkles className="text-violet-400" /> {t('executiveInsights.title') || 'Insights exécutifs IA'}</h1>
      <div className="space-y-4">
        {MOCK.map(insight => (
          <div key={insight.id} className={`bg-white/5 backdrop-blur rounded-2xl p-5 border transition hover:scale-[1.01] ${severityBorder(insight.severity)}`}>
            <div className="flex items-start gap-3">
              {severityIcon(insight.severity)}
              <div className="flex-1">
                <div className="flex items-center justify-between mb-1">
                  <h3 className={`font-semibold ${insight.isRead ? 'text-gray-300' : 'text-white'}`}>{insight.title}</h3>
                  <div className="flex items-center gap-3">
                    <span className="text-lg font-bold text-white">{insight.metricValue}</span>
                    <span className={`text-sm font-medium ${insight.metricChange.startsWith('+') ? 'text-green-400' : 'text-red-400'}`}>{insight.metricChange}</span>
                  </div>
                </div>
                <p className="text-sm text-gray-300 mb-2">{insight.description}</p>
                <div className="bg-blue-500/10 rounded-lg p-3 flex items-start gap-2">
                  <Lightbulb className="w-4 h-4 text-blue-400 mt-0.5" />
                  <p className="text-sm text-blue-300">{insight.recommendedAction}</p>
                </div>
              </div>
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}
