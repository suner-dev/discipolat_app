import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useParams } from 'react-router-dom';
import toast from 'react-hot-toast';
import api, { getErrorMessage } from '@/lib/api';
import {
  Heart, CheckCircle, XCircle, Clock, User, Tag, ThumbsUp,
  ArrowLeft, Loader2,
} from 'lucide-react';
import { useNavigate } from 'react-router-dom';

interface Testimony {
  id: string;
  titre: string;
  contenu: string;
  categorie: string;
  statut: string;
  auteur: { firstName: string; lastName: string; email: string };
  likes: number;
  commentaires: number;
  createdAt: string;
}

export default function TestimonyDetailPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const qc = useQueryClient();

  const { data: testimony, isLoading } = useQuery({
    queryKey: ['testimonies', id],
    queryFn: async () => {
      const res = await api.get(`/testimonies/${id}`);
      return res.data as Testimony;
    },
    enabled: !!id,
  });

  const approveMutation = useMutation({
    mutationFn: async () => {
      await api.post(`/testimonies/${id}/approve`);
    },
    onSuccess: () => {
      toast.success('Témoignage approuvé');
      qc.invalidateQueries({ queryKey: ['testimonies'] });
      navigate(-1);
    },
    onError: (err) => toast.error(getErrorMessage(err)),
  });

  const rejectMutation = useMutation({
    mutationFn: async () => {
      await api.post(`/testimonies/${id}/reject`);
    },
    onSuccess: () => {
      toast.success('Témoignage rejeté');
      qc.invalidateQueries({ queryKey: ['testimonies'] });
      navigate(-1);
    },
    onError: (err) => toast.error(getErrorMessage(err)),
  });

  if (isLoading) return <div className="min-h-[40vh] flex items-center justify-center"><Loader2 className="w-6 h-6 animate-spin text-gray-400" /></div>;
  if (!testimony) return <div className="p-6 text-center text-gray-400">Témoignage introuvable.</div>;

  const statusColor = (s: string) => {
    switch (s) {
      case 'APPROUVE': return 'badge-success';
      case 'EN_ATTENTE': return 'badge-warning';
      case 'REFUSE': return 'badge-error';
      case 'BROUILLON': return 'badge-gray';
      default: return 'badge-info';
    }
  };

  const categorieEmoji: Record<string, string> = {
    GUERISON: '💚', DELIVRANCE: '🕊️', PROVISION: '✨',
    FAMILLE: '👨‍👩‍👧‍👦', CONVERSION: '🌟', AUTRE: '🙏',
  };

  return (
    <div className="page-container max-w-3xl">
      <div className="page-header">
        <div className="animate-fade-in">
          <button onClick={() => navigate(-1)} className="btn-ghost btn-sm mb-2 -ml-2">
            <ArrowLeft className="w-4 h-4" /> Retour
          </button>
          <div className="flex items-center gap-2 mb-1">
            <Heart className="w-5 h-5 text-rose-500" />
            <h1 className="page-title">{testimony.titre}</h1>
          </div>
          <p className="page-subtitle">Détail du témoignage et modération</p>
        </div>
      </div>

      <div className="space-y-4 animate-slide-up">
        {/* Meta */}
        <div className="flex flex-wrap items-center gap-2">
          <span className={`badge text-[10px] ${statusColor(testimony.statut)}`}>{testimony.statut}</span>
          <span className="badge text-[10px]">
            {categorieEmoji[testimony.categorie] || '🙏'} {testimony.categorie}
          </span>
          <span className="text-[10px] text-gray-400 flex items-center gap-1">
            <ThumbsUp className="w-3 h-3" /> {testimony.likes}
          </span>
          <span className="text-[10px] text-gray-400 flex items-center gap-1">
            <Clock className="w-3 h-3" /> {new Date(testimony.createdAt).toLocaleString('fr-FR')}
          </span>
        </div>

        {/* Content */}
        <div className="glass-card p-6">
          <h3 className="text-sm font-bold text-gray-900 dark:text-gray-100 mb-3">Contenu</h3>
          <p className="text-sm text-gray-600 dark:text-gray-400 whitespace-pre-wrap leading-relaxed">{testimony.contenu}</p>
        </div>

        {/* Author */}
        <div className="glass-card p-4 flex items-center gap-3">
          <div className="w-10 h-10 rounded-full bg-gradient-to-br from-rose-400 to-pink-500 flex items-center justify-center text-white text-sm font-bold shrink-0">
            {testimony.auteur.firstName?.[0]}{testimony.auteur.lastName?.[0]}
          </div>
          <div>
            <p className="text-sm font-semibold text-gray-900 dark:text-gray-100">
              {testimony.auteur.firstName} {testimony.auteur.lastName}
            </p>
            <p className="text-[10px] text-gray-400">{testimony.auteur.email}</p>
          </div>
        </div>

        {/* Moderation actions */}
        {testimony.statut === 'EN_ATTENTE' && (
          <div className="glass-card p-6">
            <h3 className="text-sm font-bold text-gray-900 dark:text-gray-100 mb-4 flex items-center gap-2">
              <CheckCircle className="w-4 h-4 text-primary-500" />
              Modération
            </h3>
            <p className="text-xs text-gray-400 mb-4">Approuvez ou rejetez ce témoignage.</p>
            <div className="flex gap-3">
              <button
                onClick={() => approveMutation.mutate()}
                disabled={approveMutation.isPending}
                className="btn-primary btn-sm bg-green-600 hover:bg-green-700"
              >
                {approveMutation.isPending ? <Loader2 className="w-4 h-4 animate-spin" /> : <CheckCircle className="w-4 h-4" />}
                Approuver
              </button>
              <button
                onClick={() => rejectMutation.mutate()}
                disabled={rejectMutation.isPending}
                className="btn-primary btn-sm bg-red-600 hover:bg-red-700"
              >
                {rejectMutation.isPending ? <Loader2 className="w-4 h-4 animate-spin" /> : <XCircle className="w-4 h-4" />}
                Rejeter
              </button>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}
