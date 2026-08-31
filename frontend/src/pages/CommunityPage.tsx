import { useI18n } from '@/i18n';
import { useQuery } from '@tanstack/react-query';
import api from '@/lib/api';
import SkeletonLoader from '@/components/shared/SkeletonLoader';
import EmptyState from '@/components/shared/EmptyState';
import { Users, MessageSquare, Heart, Share2, Star, Calendar } from 'lucide-react';

interface CommunityPost {
  id: string;
  author: string;
  role: string;
  content: string;
  type: 'testimony' | 'prayer' | 'encouragement' | 'event';
  likes: number;
  comments: number;
  timeAgo: string;
}

export default function CommunityPage() {
  const { t } = useI18n();

  const { data: postsData, isLoading, error } = useQuery({
    queryKey: ['community-posts'],
    queryFn: async () => {
      const res = await api.get('/testimonies', { params: { page: 0, size: 50 } });
      return res.data;
    },
  });

  const posts: CommunityPost[] = (postsData?.content ?? postsData ?? []).map((p: any) => ({
    id: p.id,
    author: p.auteurNom || p.author || p.auteur || 'Anonyme',
    role: p.role || p.categorie || '',
    content: p.contenu || p.content || '',
    type: (p.type || p.categorie || 'testimony') as CommunityPost['type'],
    likes: p.likes || p.nbLikes || 0,
    comments: p.comments || p.nbComments || 0,
    timeAgo: p.createdAt ? new Date(p.createdAt).toLocaleDateString('fr-FR') : '',
  }));

  const typeConfig = (type: string) => {
    if (type === 'testimony') return { icon: Star, color: 'text-yellow-500 bg-yellow-100 dark:bg-yellow-900/30', label: 'Témoignage' };
    if (type === 'prayer') return { icon: Heart, color: 'text-pink-500 bg-pink-100 dark:bg-pink-900/30', label: 'Prières' };
    if (type === 'event') return { icon: Calendar, color: 'text-blue-500 bg-blue-100 dark:bg-blue-900/30', label: 'Événement' };
    return { icon: Share2, color: 'text-green-500 bg-green-100 dark:bg-green-900/30', label: 'Encouragement' };
  };

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold text-gray-900 dark:text-white flex items-center gap-3">
          <Users className="w-7 h-7 text-pink-500" />
          {t('nav.community') ?? 'Communauté'}
        </h1>
        <p className="text-sm text-gray-500 mt-1">Partagez, témoignez et encouragez-vous mutuellement</p>
      </div>

      {/* Compose */}
      <div className="glass rounded-2xl p-4 border border-white/20 dark:border-white/[0.06]">
        <textarea placeholder="Partagez un témoignage, une prière ou un encouragement..."
          rows={3}
          className="w-full px-4 py-3 rounded-xl border border-gray-200 dark:border-white/10 bg-white dark:bg-white/5 text-sm resize-none" />
        <div className="flex items-center justify-between mt-3">
          <div className="flex gap-2">
            {['Témoignage', 'Prières', 'Encouragement'].map((l) => (
              <button key={l} className="px-3 py-1.5 rounded-lg text-xs font-medium bg-white/5 text-gray-500 hover:bg-white/10 transition">{l}</button>
            ))}
          </div>
          <button className="px-4 py-2 rounded-xl bg-pink-500 text-white text-sm font-medium hover:bg-pink-600 transition">Publier</button>
        </div>
      </div>

      {/* Posts */}
      {isLoading ? <SkeletonLoader lines={4} variant="card" /> :
       error ? <EmptyState icon={<Users className="w-8 h-8 text-gray-400" />} title="Erreur de chargement" message="Impossible de charger les posts" /> :
       posts.length === 0 ? (
        <EmptyState icon={<Users className="w-8 h-8 text-gray-400" />} title="Aucun post" message="Soyez le premier à partager un témoignage ou une prière" />
      ) : (
        <div className="space-y-4">
          {posts.map((post) => {
            const cfg = typeConfig(post.type);
            const Icon = cfg.icon;
            return (
              <div key={post.id} className="glass rounded-2xl p-5 border border-white/20 dark:border-white/[0.06]">
                <div className="flex items-start gap-3">
                  <div className="w-10 h-10 rounded-full bg-gradient-to-br from-pink-400 to-purple-400 flex items-center justify-center text-white text-sm font-bold">
                    {post.author.split(' ').map(w => w[0]).join('')}
                  </div>
                  <div className="flex-1">
                    <div className="flex items-center gap-2">
                      <h4 className="font-medium text-gray-900 dark:text-white text-sm">{post.author}</h4>
                      <span className="text-[10px] text-gray-400">{post.role}</span>
                      <span className={`flex items-center gap-1 px-2 py-0.5 rounded-lg text-[10px] font-medium ${cfg.color}`}>
                        <Icon className="w-2.5 h-2.5" /> {cfg.label}
                      </span>
                      <span className="text-[10px] text-gray-400 ml-auto">{post.timeAgo}</span>
                    </div>
                    <p className="text-sm text-gray-700 dark:text-gray-300 mt-2 leading-relaxed">{post.content}</p>
                    <div className="flex items-center gap-4 mt-3">
                      <button className="flex items-center gap-1 text-xs text-gray-400 hover:text-pink-500 transition">
                        <Heart className="w-3.5 h-3.5" /> {post.likes}
                      </button>
                      <button className="flex items-center gap-1 text-xs text-gray-400 hover:text-blue-500 transition">
                        <MessageSquare className="w-3.5 h-3.5" /> {post.comments}
                      </button>
                      <button className="flex items-center gap-1 text-xs text-gray-400 hover:text-green-500 transition">
                        <Share2 className="w-3.5 h-3.5" /> Partager
                      </button>
                    </div>
                  </div>
                </div>
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
}
