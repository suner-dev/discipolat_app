import { useState } from 'react';
import { useI18n } from '@/i18n';
import { HelpCircle, Clock, CheckCircle, UserPlus, AlertTriangle } from 'lucide-react';

interface ReverseRequest { id: string; requesterName: string; topic: string; description: string; status: string; urgencyLevel: number; assignedMentor?: string; createdAt: string; }

export default function ReverseMentoringPage() {
  const { t } = useI18n();
  const MOCK: ReverseRequest[] = [
    { id: '1', requesterName: 'Chef Famille Ngono', topic: 'PASTORAL_CARE', description: 'J\'ai un membre en difficulté spirituelle, j\'ai besoin de conseils.', status: 'PENDING', urgencyLevel: 4, createdAt: '2026-08-21' },
    { id: '2', requesterName: 'Faiseur Kotto', topic: 'LEADERSHIP', description: 'Comment gérer un conflit dans mon groupe ?', status: 'IN_PROGRESS', urgencyLevel: 3, assignedMentor: 'Pasteur Pierre', createdAt: '2026-08-18' },
    { id: '3', requesterName: 'Responsable Marie', topic: 'FAMILY_ISSUE', description: 'Séparation au sein d\'une famille de la congrégation.', status: 'COMPLETED', urgencyLevel: 5, assignedMentor: 'Pasteur Pierre', createdAt: '2026-08-10' },
  ];
  const statusColor = (s: string) => s === 'PENDING' ? 'bg-yellow-500/20 text-yellow-400' : s === 'IN_PROGRESS' ? 'bg-blue-500/20 text-blue-400' : 'bg-green-500/20 text-green-400';
  const urgencyDots = (n: number) => Array.from({length: 5}, (_, i) => <span key={i} className={`w-2 h-2 rounded-full ${i < n ? 'bg-red-400' : 'bg-gray-700'}`} />);

  return (
    <div className="space-y-6 p-6">
      <h1 className="text-2xl font-bold text-white flex items-center gap-2"><HelpCircle className="text-pink-400" /> {t('reverseMentoring.title') || 'Mentorat inversé'}</h1>
      <div className="space-y-3">
        {MOCK.map(r => (
          <div key={r.id} className="bg-white/5 backdrop-blur rounded-xl p-4 border border-white/10">
            <div className="flex items-center justify-between mb-2">
              <div className="flex items-center gap-3">
                <span className="text-white font-medium">{r.requesterName}</span>
                <span className={`px-2 py-0.5 rounded-full text-xs font-medium ${statusColor(r.status)}`}>{r.status}</span>
              </div>
              <div className="flex items-center gap-1">{urgencyDots(r.urgencyLevel)}</div>
            </div>
            <p className="text-sm text-gray-300 mb-2">{r.description}</p>
            <div className="flex items-center gap-3 text-xs text-gray-400">
              <span>Thème: {r.topic}</span>
              <span>{r.createdAt}</span>
              {r.assignedMentor && <span className="text-blue-400">→ {r.assignedMentor}</span>}
            </div>
            {r.status === 'PENDING' && <button className="mt-2 px-3 py-1 bg-blue-600 hover:bg-blue-700 rounded-lg text-white text-xs font-medium">Accepter</button>}
          </div>
        ))}
      </div>
    </div>
  );
}
