import { useState } from 'react';
import { useI18n } from '@/i18n';
import { Shield, AlertTriangle, CheckCircle, XCircle, Eye, Filter } from 'lucide-react';

interface ModerationItem { id: string; content: string; source: string; status: string; riskLevel: string; aiConfidence: number; createdAt: string; }

export default function ContentModerationPage() {
  const { t } = useI18n();
  const [filter, setFilter] = useState<'all' | 'pending' | 'approved' | 'rejected'>('all');
  const MOCK_ITEMS: ModerationItem[] = [
    { id: '1', content: 'Merci Seigneur pour cette bénédiction...', source: 'TÉMOIGNAGE', status: 'APPROVED', riskLevel: 'LOW', aiConfidence: 0.95, createdAt: '2026-08-20' },
    { id: '2', content: 'Message suspect avec liens commerciaux', source: 'MESSAGE', status: 'PENDING', riskLevel: 'HIGH', aiConfidence: 0.82, createdAt: '2026-08-21' },
    { id: '3', content: 'Rapport de prière pour la famille Mbarga', source: 'RAPPORT', status: 'APPROVED', riskLevel: 'LOW', aiConfidence: 0.98, createdAt: '2026-08-22' },
    { id: '4', content: 'Contenu offensant détecté par l\'IA', source: 'COMMENT', status: 'REJECTED', riskLevel: 'CRITICAL', aiConfidence: 0.91, createdAt: '2026-08-22' },
  ];
  const filtered = filter === 'all' ? MOCK_ITEMS : MOCK_ITEMS.filter(i => i.status === filter);

  const riskColor = (r: string) => r === 'CRITICAL' ? 'text-red-400 bg-red-500/20' : r === 'HIGH' ? 'text-orange-400 bg-orange-500/20' : r === 'MEDIUM' ? 'text-yellow-400 bg-yellow-500/20' : 'text-green-400 bg-green-500/20';
  const statusIcon = (s: string) => s === 'APPROVED' ? <CheckCircle className="w-4 h-4 text-green-400" /> : s === 'REJECTED' ? <XCircle className="w-4 h-4 text-red-400" /> : <AlertTriangle className="w-4 h-4 text-yellow-400" />;

  return (
    <div className="space-y-6 p-6">
      <h1 className="text-2xl font-bold text-white flex items-center gap-2"><Shield className="text-red-400" /> {t('moderation.title') || 'Filtre de modération IA'}</h1>
      <div className="grid grid-cols-4 gap-3">
        {[['all','Tous','text-white'],['pending','En attente','text-yellow-400'],['approved','Approuvés','text-green-400'],['rejected','Rejetés','text-red-400']].map(([k,l,c]) => (
          <button key={k} onClick={() => setFilter(k as any)} className={`p-3 rounded-xl text-sm font-medium transition ${filter === k ? 'bg-blue-600 text-white' : 'bg-white/5 text-gray-400 hover:bg-white/10'}`}>{l}</button>
        ))}
      </div>
      <div className="space-y-3">
        {filtered.map(item => (
          <div key={item.id} className="bg-white/5 backdrop-blur rounded-xl p-4 border border-white/10 flex items-start gap-4">
            {statusIcon(item.status)}
            <div className="flex-1 min-w-0">
              <p className="text-white text-sm line-clamp-2">{item.content}</p>
              <div className="flex items-center gap-3 mt-2 text-xs text-gray-400">
                <span>{item.source}</span>
                <span className={`px-2 py-0.5 rounded-full ${riskColor(item.riskLevel)}`}>{item.riskLevel}</span>
                <span>Confiance: {(item.aiConfidence * 100).toFixed(0)}%</span>
                <span>{item.createdAt}</span>
              </div>
            </div>
            {item.status === 'PENDING' && (
              <div className="flex gap-2">
                <button className="px-3 py-1 bg-green-600 hover:bg-green-700 rounded-lg text-white text-xs font-medium">Approuver</button>
                <button className="px-3 py-1 bg-red-600 hover:bg-red-700 rounded-lg text-white text-xs font-medium">Rejeter</button>
              </div>
            )}
          </div>
        ))}
      </div>
    </div>
  );
}
