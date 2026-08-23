import { useI18n } from '@/i18n';
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

const MOCK_POSTS: CommunityPost[] = [
  { id: '1', author: 'Soeur Claire', role: 'Membre', content: 'Dieu a guéri ma mère de la grippe cette semaine. Gloire à Dieu! 🙏', type: 'testimony', likes: 24, comments: 8, timeAgo: 'Il y a 2h' },
  { id: '2', author: 'Frère Paul', role: 'Faiseur', content: 'Priez pour ma famille, nous traversons une saison difficile mais nous faisons confiance au Seigneur.', type: 'prayer', likes: 31, comments: 12, timeAgo: 'Il y a 4h' },
  { id: '3', author: 'Pasteur Jean', role: 'Pasteur', content: 'Rappel: Le culte spécial de jeûne aura lieu ce samedi à 6h du matin. Venez nombreux!', type: 'event', likes: 45, comments: 6, timeAgo: 'Il y a 6h' },
  { id: '4', author: 'Soeur Marie', role: 'Responsable', content: 'Félicitations à tous ceux qui ont participé à l\'événement caritatif d\'hier. Vous êtes bénis!', type: 'encouragement', likes: 38, comments: 5, timeAgo: 'Hier' },
];

export default function CommunityPage() {
  const { t } = useI18n();

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
      <div className="space-y-4">
        {MOCK_POSTS.map((post) => {
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
    </div>
  );
}
