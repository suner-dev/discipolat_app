import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import api, { getErrorMessage } from '@/lib/api';
import { FileText, Loader2, CheckCircle, AlertCircle, Smile, Frown, Meh } from 'lucide-react';
import toast from 'react-hot-toast';

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

export default function VisitNotesVerifyPage() {
  const qc = useQueryClient();

  const { data: notes = [], isLoading, error } = useQuery({
    queryKey: ['ai-visit-notes-verify'],
    queryFn: async () => (await api.get('/ai-visit-notes')).data as VisitNote[],
  });

  const verifyMutation = useMutation({
    mutationFn: async (id: string) => api.post(`/ai-visit-notes/${id}/verify`),
    onSuccess: () => { toast.success('Note vérifiée'); qc.invalidateQueries({ queryKey: ['ai-visit-notes-verify'] }); },
    onError: (e) => toast.error(getErrorMessage(e)),
  });

  const sentimentIcon = (s: string) => s === 'POSITIVE' ? <Smile className="w-5 h-5 text-green-400" /> : s === 'CONCERNING' ? <Frown className="w-5 h-5 text-orange-400" /> : s === 'CRITICAL' ? <AlertCircle className="w-5 h-5 text-red-400" /> : <Meh className="w-5 h-5 text-gray-400" />;

  const parseActionItems = (items: string): string[] => {
    try { return JSON.parse(items); } catch { return items ? items.split('\n').filter(Boolean) : []; }
  };

  const unverified = notes.filter((n) => !n.isVerified);
  const verified = notes.filter((n) => n.isVerified);

  return (
    <div className="page-container">
      <div className="page-header">
        <div className="p-3 rounded-xl bg-gradient-to-br from-teal-500 to-emerald-600 text-white shadow-lg">
          <FileText className="w-6 h-6" />
        </div>
        <div>
          <h1 className="page-title">Vérification des notes IA</h1>
          <p className="page-subtitle">Vérifiez les notes de visites générées par l'IA</p>
        </div>
      </div>

      <div className="grid grid-cols-3 gap-4 mb-6">
        {[
          { label: 'Total', value: notes.length, color: 'text-teal-600' },
          { label: 'Non vérifiées', value: unverified.length, color: 'text-yellow-600' },
          { label: 'Vérifiées', value: verified.length, color: 'text-green-600' },
        ].map((s) => (
          <div key={s.label} className="stat-card">
            <p className={`stat-value ${s.color}`}>{s.value}</p>
            <p className="stat-label">{s.label}</p>
          </div>
        ))}
      </div>

      {isLoading ? (
        <div className="flex justify-center py-12"><Loader2 className="w-8 h-8 animate-spin text-primary-500" /></div>
      ) : error ? (
        <div className="glass-card p-6 text-red-400">{getErrorMessage(error)}</div>
      ) : notes.length === 0 ? (
        <div className="glass-card p-10 text-center text-gray-500">Aucune note de visite IA</div>
      ) : (
        <div className="space-y-4">
          {unverified.map((note) => (
            <div key={note.id} className="glass-card p-5 border-l-[3px] border-l-yellow-500">
              <div className="flex items-start justify-between mb-3">
                <div className="flex items-center gap-2">
                  {sentimentIcon(note.aiSentiment)}
                  <span className="px-2 py-0.5 rounded-full text-xs text-yellow-400 bg-yellow-500/20">Non vérifié</span>
                  <span className="text-xs text-gray-400">{note.memberId?.slice(0, 8)}...</span>
                </div>
                <button onClick={() => verifyMutation.mutate(note.id)} disabled={verifyMutation.isPending}
                  className="btn-sm px-3 py-1.5 rounded-lg bg-green-600 text-white text-xs hover:bg-green-700 flex items-center gap-1">
                  {verifyMutation.isPending ? <Loader2 className="w-3 h-3 animate-spin" /> : <CheckCircle className="w-3 h-3" />} Vérifier
                </button>
              </div>
              <p className="text-sm text-gray-600 dark:text-gray-300 mb-2">{note.aiSummary}</p>
              {note.aiActionItems && (
                <div>
                  <h4 className="text-xs font-medium text-gray-400 mb-1">Actions recommandées :</h4>
                  <ul className="space-y-1">
                    {parseActionItems(note.aiActionItems).map((item, i) => (
                      <li key={i} className="text-sm text-blue-300 flex items-center gap-2">• {item}</li>
                    ))}
                  </ul>
                </div>
              )}
              <p className="text-[11px] text-gray-500 mt-2">{new Date(note.createdAt).toLocaleDateString('fr-FR')}</p>
            </div>
          ))}

          {verified.length > 0 && (
            <>
              <h2 className="text-sm font-semibold text-gray-400 mt-6 mb-2">Vérifiées ({verified.length})</h2>
              {verified.map((note) => (
                <div key={note.id} className="glass-card p-5 opacity-70">
                  <div className="flex items-center gap-2 mb-2">
                    {sentimentIcon(note.aiSentiment)}
                    <CheckCircle className="w-4 h-4 text-green-400" />
                    <span className="text-xs text-gray-400">{note.memberId?.slice(0, 8)}...</span>
                    <span className="text-[11px] text-gray-500 ml-auto">{new Date(note.createdAt).toLocaleDateString('fr-FR')}</span>
                  </div>
                  <p className="text-sm text-gray-500 line-clamp-2">{note.aiSummary}</p>
                </div>
              ))}
            </>
          )}
        </div>
      )}
    </div>
  );
}
