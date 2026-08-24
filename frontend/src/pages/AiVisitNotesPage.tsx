import { useState } from 'react';
import { useI18n } from '@/i18n';
import { FileText, Mic, CheckCircle, AlertCircle, Smile, Frown, Meh } from 'lucide-react';

interface VisitNote { id: string; memberId: string; memberName: string; pastorName: string; aiSummary: string; aiSentiment: string; aiActionItems: string[]; createdAt: string; isVerified: boolean; }

export default function AiVisitNotesPage() {
  const { t } = useI18n();
  const MOCK: VisitNote[] = [
    { id: '1', memberId: 'm1', memberName: 'Jean Mbarga', pastorName: 'Pasteur Pierre', aiSummary: 'Visite pastorale enregistrée. Jean exprime gratitude pour le soutien. Santé améliorée. Besoin d\'accompagnement pour reconquérir sa femme.', aiSentiment: 'POSITIVE', aiActionItems: ['Planifier session de prière', 'Organiser rencontre avec épouse'], createdAt: '2026-08-20', isVerified: true },
    { id: '2', memberId: 'm2', memberName: 'Marie Ngo', pastorName: 'Pasteur Pierre', aiSummary: 'Marie traverse une période difficile après le décès de sa mère. Besoin urgent de soutien pastoral et communautaire.', aiSentiment: 'CONCERNING', aiActionItems: ['Soutien pastoral immédiat', 'Organiser aide communautaire', 'Suivi hebdomadaire'], createdAt: '2026-08-22', isVerified: false },
  ];
  const sentimentIcon = (s: string) => s === 'POSITIVE' ? <Smile className="w-5 h-5 text-green-400" /> : s === 'CONCERNING' ? <Frown className="w-5 h-5 text-orange-400" /> : s === 'CRITICAL' ? <AlertCircle className="w-5 h-5 text-red-400" /> : <Meh className="w-5 h-5 text-gray-400" />;

  return (
    <div className="space-y-6 p-6">
      <h1 className="text-2xl font-bold text-white flex items-center gap-2"><FileText className="text-teal-400" /> {t('visitNotes.title') || 'Notes IA Visites'}</h1>
      <div className="space-y-4">
        {MOCK.map(note => (
          <div key={note.id} className="bg-white/5 backdrop-blur rounded-2xl p-5 border border-white/10">
            <div className="flex items-start justify-between mb-3">
              <div>
                <h3 className="text-white font-semibold">{note.memberName}</h3>
                <p className="text-xs text-gray-400">{note.pastorName} • {note.createdAt}</p>
              </div>
              <div className="flex items-center gap-2">
                {sentimentIcon(note.aiSentiment)}
                {note.isVerified ? <CheckCircle className="w-4 h-4 text-green-400" /> : <span className="text-xs text-yellow-400 bg-yellow-500/20 px-2 py-0.5 rounded-full">Non vérifié</span>}
              </div>
            </div>
            <p className="text-sm text-gray-300 mb-3 leading-relaxed">{note.aiSummary}</p>
            <div>
              <h4 className="text-xs font-medium text-gray-400 mb-1">Actions recommandées par l'IA :</h4>
              <ul className="space-y-1">
                {note.aiActionItems.map((item, i) => (
                  <li key={i} className="text-sm text-blue-300 flex items-center gap-2">• {item}</li>
                ))}
              </ul>
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}
