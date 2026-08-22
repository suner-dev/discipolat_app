import { useQuery } from '@tanstack/react-query';
import { useState } from 'react';
import api from '@/lib/api';
import { Loader2, Mic, Search, Smile, Frown, Meh, Heart, UserRound } from 'lucide-react';

interface VoiceReport {
  id: string;
  authorId: string;
  transcription: string;
  analysis: string | null;
  createdAt: string;
}

interface Analysis {
  personnes: string[];
  humeur: string;
  besoinPriere: boolean;
  actions: string[];
}

const MOOD_STYLES: Record<string, { icon: typeof Smile; cls: string; label: string }> = {
  JOYEUX: { icon: Smile, cls: 'text-green-500', label: 'Joyeux' },
  TRISTE: { icon: Frown, cls: 'text-blue-500', label: 'Triste' },
  NEUTRE: { icon: Meh, cls: 'text-gray-400', label: 'Neutre' },
};

/** Rapports Vocaux IA — transcription + extraction automatique (personnes, humeur, actions). */
export default function VoiceReportsPage() {
  const [search, setSearch] = useState('');

  const reportsQuery = useQuery({
    queryKey: ['voice-reports'],
    queryFn: async () => (await api.get<VoiceReport[]>('/voice-reports')).data,
  });

  const parseAnalysis = (raw: string | null): Analysis | null => {
    if (!raw) return null;
    try {
      return JSON.parse(raw) as Analysis;
    } catch {
      return null;
    }
  };

  const filtered = (reportsQuery.data ?? []).filter(
    (r) => !search || r.transcription.toLowerCase().includes(search.toLowerCase()),
  );

  if (reportsQuery.isLoading) {
    return <Loader2 className="w-8 h-8 animate-spin text-primary-500 mx-auto mt-20" />;
  }

  return (
    <div className="page-container">
      <div className="page-header">
        <div className="p-3 rounded-xl bg-gradient-to-br from-pink-500 to-rose-600 text-white shadow-lg">
          <Mic className="w-6 h-6" />
        </div>
        <div>
          <h1 className="page-title">Rapports Vocaux</h1>
          <p className="page-subtitle">
            Dictés sur le terrain (même hors-ligne), transcrits et analysés par l'IA
          </p>
        </div>
        <div className="ml-auto relative">
          <Search className="w-4 h-4 absolute left-3 top-1/2 -translate-y-1/2 text-gray-400" />
          <input
            placeholder="Rechercher…"
            className="input pl-9 w-48"
            value={search}
            onChange={(e) => setSearch(e.target.value)}
          />
        </div>
      </div>

      <div className="grid gap-4 md:grid-cols-2">
        {filtered.map((r) => {
          const a = parseAnalysis(r.analysis);
          const mood = a ? MOOD_STYLES[a.humeur] ?? MOOD_STYLES.NEUTRE : MOOD_STYLES.NEUTRE;
          return (
            <div key={r.id} className="glass-card p-5 animate-slide-up">
              {/* En-tête */}
              <div className="flex items-center justify-between mb-3">
                <div className="flex items-center gap-2 text-xs text-gray-500 dark:text-gray-400">
                  <UserRound className="w-4 h-4" />
                  {new Date(r.createdAt).toLocaleString('fr-FR')}
                </div>
                <span className={`flex items-center gap-1 text-xs font-medium ${mood.cls}`}>
                  <mood.icon className="w-4 h-4" /> {mood.label}
                </span>
              </div>

              {/* Transcription */}
              <p className="text-sm text-gray-700 dark:text-gray-300 leading-relaxed border-l-2 border-primary-300 dark:border-primary-700 pl-3">
                « {r.transcription} »
              </p>

              {/* Analyse IA */}
              {a && (
                <div className="mt-4 space-y-2">
                  {a.personnes.length > 0 && (
                    <div className="flex flex-wrap gap-1.5">
                      {a.personnes.map((p) => (
                        <span key={p} className="badge badge-info">{p}</span>
                      ))}
                    </div>
                  )}
                  {a.actions.length > 0 && (
                    <ul className="text-xs text-gray-500 dark:text-gray-400 space-y-1">
                      {a.actions.map((act, i) => (
                        <li key={i}>• {act}</li>
                      ))}
                    </ul>
                  )}
                  {a.besoinPriere && (
                    <span className="badge badge-warning inline-flex items-center gap-1">
                      <Heart className="w-3 h-3" /> Besoin de prière détecté
                    </span>
                  )}
                </div>
              )}
            </div>
          );
        })}
      </div>

      {!reportsQuery.isLoading && filtered.length === 0 && (
        <div className="glass-card p-12 text-center">
          <Mic className="w-10 h-10 text-gray-300 dark:text-gray-600 mx-auto mb-4" />
          <p className="text-gray-500 dark:text-gray-400 max-w-md mx-auto">
            Aucun rapport vocal{search ? ' ne correspond à votre recherche' : ''}. Les rapports dictés depuis
            l'application mobile apparaîtront ici avec leur analyse IA.
          </p>
        </div>
      )}
    </div>
  );
}
