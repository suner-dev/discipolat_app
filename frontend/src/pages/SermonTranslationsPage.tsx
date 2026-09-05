import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import api, { getErrorMessage } from '@/lib/api';
import { Languages, Loader2, Play, BookOpen } from 'lucide-react';
import toast from 'react-hot-toast';

interface Translation {
  id: string;
  sermonId: string;
  langueCible: string;
  statut: 'EN_COURS' | 'TERMINE' | 'ERREUR';
  confiance: number;
  creeLe: string;
  termineLe?: string;
}

interface SermonOption {
  id: string;
  title: string;
}

const STATUT_LABELS: Record<string, string> = {
  EN_COURS: 'En cours',
  TERMINE: 'Terminé',
  ERREUR: 'Erreur',
};

export default function SermonTranslationsPage() {
  const qc = useQueryClient();
  const [selectedSermonId, setSelectedSermonId] = useState<string>('');

  const { data: sermons = [] } = useQuery<SermonOption[]>({
    queryKey: ['sermons', 'options'],
    queryFn: async () => {
      const res = await api.get('/sermons');
      return ((res.data.content || res.data || []) as { id: string; title: string }[]).map((s) => ({ id: s.id, title: s.title }));
    },
  });

  const { data: translations = [], isLoading } = useQuery({
    queryKey: ['sermon-translations', selectedSermonId],
    queryFn: async () =>
      selectedSermonId
        ? (await api.get(`/sermons/translations/by-sermon/${selectedSermonId}`)).data as Translation[]
        : (await api.get('/sermons/translations')).data as Translation[],
    enabled: true,
  });

  const transcribeMutation = useMutation({
    mutationFn: async (sermonId: string) => api.post(`/sermons/${sermonId}/transcribe`),
    onSuccess: () => {
      toast.success('Transcription lancée');
      qc.invalidateQueries({ queryKey: ['sermon-translations', selectedSermonId] });
    },
    onError: (e) => toast.error(getErrorMessage(e)),
  });

  const selectedTitle = sermons.find((s) => s.id === selectedSermonId)?.title;

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

      <div className="glass-card p-4 mb-6 flex items-center gap-3">
        <BookOpen className="w-5 h-5 text-indigo-500 flex-shrink-0" />
        <div className="flex-1">
          <label className="block text-[10px] text-gray-400 uppercase tracking-wider mb-1">Filtrer par sermon</label>
          <select
            className="input"
            value={selectedSermonId}
            onChange={(e) => setSelectedSermonId(e.target.value)}
          >
            <option value="">Tous les sermons</option>
            {sermons.map((s) => (
              <option key={s.id} value={s.id}>{s.title}</option>
            ))}
          </select>
        </div>
      </div>

      <div className="grid grid-cols-3 gap-4 mb-6">
        {[
          { label: 'Total', value: translations.length, color: 'text-blue-600' },
          { label: 'Terminées', value: translations.filter((t) => t.statut === 'TERMINE').length, color: 'text-green-600' },
          { label: 'En cours', value: translations.filter((t) => t.statut === 'EN_COURS').length, color: 'text-yellow-600' },
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
        <div className="glass-card p-10 text-center text-gray-500">
          {selectedSermonId
            ? "Aucune traduction pour ce sermon"
            : 'Aucune traduction de sermon'}
        </div>
      ) : (
        <div className="space-y-3">
          {translations.map((t) => (
            <div key={t.id} className="glass-card p-5 flex items-center justify-between">
              <div>
                <div className="flex items-center gap-2 mb-1">
                  <span className="text-sm font-medium text-gray-800 dark:text-gray-200">
                    {t.sermonId === selectedSermonId && selectedTitle ? selectedTitle : `Sermon #${t.sermonId.slice(0, 8)}`}
                  </span>
                  <span className={`px-2 py-0.5 rounded-full text-xs font-medium ${t.statut === 'TERMINE' ? 'text-green-400 bg-green-500/20' : t.statut === 'EN_COURS' ? 'text-yellow-400 bg-yellow-500/20' : 'text-red-400 bg-red-500/20'}`}>
                    {STATUT_LABELS[t.statut] || t.statut}
                  </span>
                </div>
                <div className="flex items-center gap-2 text-xs text-gray-400">
                  <span>Cible : {t.langueCible}</span>
                  <span>• Confiance : {Math.round(t.confiance * 100)}%</span>
                  <span>• {new Date(t.creeLe).toLocaleDateString('fr-FR')}</span>
                </div>
              </div>
              {t.statut === 'EN_COURS' && (
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
