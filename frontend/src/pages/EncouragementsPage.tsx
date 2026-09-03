import { useQuery } from '@tanstack/react-query';
import api, { getErrorMessage } from '@/lib/api';
import { Heart, Loader2, Sparkles } from 'lucide-react';

interface Encouragement {
  soulId: string;
  soulName?: string;
  message: string;
  verse?: string;
  generatedAt: string;
}

interface Soul {
  id: string;
  firstName: string;
  lastName: string;
}

export default function EncouragementsPage() {
  const { data: souls = [], isLoading: loadingSouls } = useQuery({
    queryKey: ['souls-list'],
    queryFn: async () => (await api.get('/souls')).data as Soul[],
  });

  return (
    <div className="page-container">
      <div className="page-header">
        <div className="p-3 rounded-xl bg-gradient-to-br from-pink-500 to-rose-600 text-white shadow-lg">
          <Heart className="w-6 h-6" />
        </div>
        <div>
          <h1 className="page-title">Encouragements IA</h1>
          <p className="page-subtitle">Messages d'encouragement personnalisés par l'intelligence artificielle</p>
        </div>
      </div>

      {loadingSouls ? (
        <div className="flex justify-center py-12"><Loader2 className="w-8 h-8 animate-spin text-primary-500" /></div>
      ) : souls.length === 0 ? (
        <div className="glass-card p-10 text-center text-gray-500">Aucun membre trouvé</div>
      ) : (
        <div className="space-y-4">
          {souls.map((soul) => (
            <EncouragementCard key={soul.id} soul={soul} />
          ))}
        </div>
      )}
    </div>
  );
}

function EncouragementCard({ soul }: { soul: Soul }) {
  const { data, isLoading } = useQuery({
    queryKey: ['encouragement', soul.id],
    queryFn: async () => (await api.get(`/ai/encouragement/${soul.id}`)).data as Encouragement,
    retry: false,
  });

  return (
    <div className="glass-card p-5">
      <div className="flex items-center gap-3 mb-3">
        <div className="p-2 rounded-lg bg-gradient-to-br from-pink-500 to-rose-500 text-white">
          <Sparkles className="w-4 h-4" />
        </div>
        <div>
          <p className="text-sm font-medium text-gray-800 dark:text-gray-200">{soul.firstName} {soul.lastName}</p>
          <p className="text-[11px] text-gray-500">{soul.id.slice(0, 8)}...</p>
        </div>
      </div>
      {isLoading ? (
        <div className="flex items-center gap-2 text-sm text-gray-400"><Loader2 className="w-4 h-4 animate-spin" /> Génération en cours...</div>
      ) : data ? (
        <div>
          <p className="text-sm text-gray-600 dark:text-gray-300 italic leading-relaxed">"{data.message}"</p>
          {data.verse && <p className="text-xs text-primary-500 mt-2">📖 {data.verse}</p>}
          <p className="text-[11px] text-gray-500 mt-2">{new Date(data.generatedAt).toLocaleDateString('fr-FR')}</p>
        </div>
      ) : (
        <p className="text-xs text-gray-500">Aucun encouragement disponible</p>
      )}
    </div>
  );
}
