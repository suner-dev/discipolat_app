import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import api, { getErrorMessage } from '@/lib/api';
import { Languages, Loader2, Play } from 'lucide-react';
import toast from 'react-hot-toast';

interface Translation {
  id: string;
  sermonId: string;
  sermonTitle?: string;
  sourceLang: string;
  targetLang: string;
  status: string;
  translatedAt?: string;
}

export default function SermonTranslationsPage() {
  const qc = useQueryClient();

  const { data: translations = [], isLoading } = useQuery({
    queryKey: ['sermon-translations'],
    queryFn: async () => (await api.get('/sermons/translations')).data as Translation[],
  });

  const transcribeMutation = useMutation({
    mutationFn: async (sermonId: string) => api.post(`/sermons/${sermonId}/transcribe`),
    onSuccess: () => { toast.success('Transcription lancée'); qc.invalidateQueries({ queryKey: ['sermon-translations'] }); },
    onError: (e) => toast.error(getErrorMessage(e)),
  });

  return (
    <div className="page-container">
      <div className="page-header">
        <div className="p-3 rounded-xl bg-gradient-to-br from-indigo-500 to-blue-600 text-white shadow-lg">
          <Languages className="w-6 h-6" />
        </div>
        <div>
          <h1 className="page-title">Traductions de sermons</h1>
          <p className="page-subtitle">Transcription et traduction automatique des sermons</p>
        </div>
      </div>

      <div className="grid grid-cols-3 gap-4 mb-6">
        {[
          { label: 'Total', value: translations.length, color: 'text-blue-600' },
          { label: 'Terminées', value: translations.filter((t) => t.status === 'COMPLETED').length, color: 'text-green-600' },
          { label: 'En cours', value: translations.filter((t) => t.status === 'PENDING' || t.status === 'PROCESSING').length, color: 'text-yellow-600' },
        ].map((s) => (
          <div key={s.label} className="stat-card">
            <p className={`stat-value ${s.color}`}>{s.value}</p>
            <p className="stat-label">{s.label}</p>
          </div>
        ))}
      </div>

      {isLoading ? (
        <div className="flex justify-center py-12"><Loader2 className="w-8 h-8 animate-spin text-primary-500" /></div>
      ) : translations.length === 0 ? (
        <div className="glass-card p-10 text-center text-gray-500">Aucune traduction de sermon</div>
      ) : (
        <div className="space-y-3">
          {translations.map((t) => (
            <div key={t.id} className="glass-card p-5 flex items-center justify-between">
              <div>
                <div className="flex items-center gap-2 mb-1">
                  <span className="text-sm font-medium text-gray-800 dark:text-gray-200">{t.sermonTitle ?? `Sermon #${t.sermonId.slice(0, 8)}`}</span>
                  <span className={`px-2 py-0.5 rounded-full text-xs font-medium ${t.status === 'COMPLETED' ? 'text-green-400 bg-green-500/20' : t.status === 'PROCESSING' ? 'text-yellow-400 bg-yellow-500/20' : 'text-gray-400 bg-gray-500/20'}`}>{t.status}</span>
                </div>
                <div className="flex items-center gap-2 text-xs text-gray-400">
                  <span>{t.sourceLang}</span>
                  <span>→</span>
                  <span>{t.targetLang}</span>
                  {t.translatedAt && <span>• {new Date(t.translatedAt).toLocaleDateString('fr-FR')}</span>}
                </div>
              </div>
              {t.status !== 'COMPLETED' && t.status !== 'PROCESSING' && (
                <button onClick={() => transcribeMutation.mutate(t.sermonId)} disabled={transcribeMutation.isPending}
                  className="btn-sm px-3 py-1.5 rounded-lg bg-primary-600 text-white text-xs hover:bg-primary-700 flex items-center gap-1">
                  {transcribeMutation.isPending ? <Loader2 className="w-3 h-3 animate-spin" /> : <Play className="w-3 h-3" />} Lancer
                </button>
              )}
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
