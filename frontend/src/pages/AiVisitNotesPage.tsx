import { useQuery } from '@tanstack/react-query';
import { useI18n } from '@/i18n';
import api from '@/lib/api';
import { getErrorMessage } from '@/lib/api';
import SkeletonLoader from '@/components/shared/SkeletonLoader';
import EmptyState from '@/components/shared/EmptyState';
import { FileText, CheckCircle, AlertCircle, Smile, Frown, Meh } from 'lucide-react';

interface VisitNote {
  id: string;
  memberId: string;
  pastorId: string;
  aiSummary: string;
  aiSentiment: string;
  aiActionItems: string;
  isVerified: boolean;
  createdAt: string;
}

export default function AiVisitNotesPage() {
  const { t } = useI18n();

  const { data: notes = [], isLoading, error } = useQuery({
    queryKey: ['ai-visit-notes'],
    queryFn: async () => (await api.get('/ai-visit-notes')).data as VisitNote[],
    retry: false,
  });

  const sentimentIcon = (s: string) => s === 'POSITIVE' ? <Smile className="w-5 h-5 text-green-400" /> : s === 'CONCERNING' ? <Frown className="w-5 h-5 text-orange-400" /> : s === 'CRITICAL' ? <AlertCircle className="w-5 h-5 text-red-400" /> : <Meh className="w-5 h-5 text-gray-400" />;

  const parseActionItems = (items: string): string[] => {
    try { return JSON.parse(items); } catch { return items ? items.split('\n').filter(Boolean) : []; }
  };

  if (isLoading) return <SkeletonLoader lines={4} variant="card" />;
  if (error) return <div className="text-red-500 p-6">{getErrorMessage(error)}</div>;
  if (notes.length === 0) return <EmptyState title="Aucune note de visite IA" message="Les notes de visites pastorales analysées par l'IA apparaîtront ici" />;

  return (
    <div className="space-y-6 p-6">
      <h1 className="text-2xl font-bold text-white flex items-center gap-2"><FileText className="text-teal-400" /> {t('visitNotes.title') || 'Notes IA Visites'}</h1>
      <div className="space-y-4">
        {notes.map(note => (
          <div key={note.id} className="bg-white/5 backdrop-blur rounded-2xl p-5 border border-white/10">
            <div className="flex items-start justify-between mb-3">
              <div>
                <h3 className="text-white font-semibold">{note.memberId?.slice(0, 8)}...</h3>
                <p className="text-xs text-gray-400">{note.pastorId?.slice(0, 8)}... &bull; {note.createdAt}</p>
              </div>
              <div className="flex items-center gap-2">
                {sentimentIcon(note.aiSentiment)}
                {note.isVerified ? <CheckCircle className="w-4 h-4 text-green-400" /> : <span className="text-xs text-yellow-400 bg-yellow-500/20 px-2 py-0.5 rounded-full">Non vérifié</span>}
              </div>
            </div>
            <p className="text-sm text-gray-300 mb-3 leading-relaxed">{note.aiSummary}</p>
            {note.aiActionItems && (
              <div>
                <h4 className="text-xs font-medium text-gray-400 mb-1">Actions recommandées par l'IA :</h4>
                <ul className="space-y-1">
                  {parseActionItems(note.aiActionItems).map((item, i) => (
                    <li key={i} className="text-sm text-blue-300 flex items-center gap-2">&bull; {item}</li>
                  ))}
                </ul>
              </div>
            )}
          </div>
        ))}
      </div>
    </div>
  );
}
