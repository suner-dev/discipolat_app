import React, { useState, useEffect } from 'react';
import { useI18n } from '@/i18n';
import api from '@/lib/api';
import SkeletonLoader from '@/components/shared/SkeletonLoader';
import EmptyState from '@/components/shared/EmptyState';
import Toast from '@/components/shared/Toast';
import { Users, Send, Heart, MessageCircle, Plus, Shield } from 'lucide-react';

interface CerclePost {
  id: string;
  contenu: string;
  categorie: 'DEFI' | 'SUCCES' | 'METHODE' | 'PRIERE' | 'AUTRE';
  anonyme: boolean;
  auteur: { firstName: string; lastName: string };
  likes: number;
  commentaires: number;
  createdAt: string;
}

const CATEGORIES = [
  { key: 'DEFI', label: 'Défi', icon: '⚡', color: 'bg-orange-100 text-orange-700' },
  { key: 'SUCCES', label: 'Succès', icon: '🎉', color: 'bg-green-100 text-green-700' },
  { key: 'METHODE', label: 'Méthode', icon: '💡', color: 'bg-blue-100 text-blue-700' },
  { key: 'PRIERE', label: 'Prière', icon: '🙏', color: 'bg-purple-100 text-purple-700' },
  { key: 'AUTRE', label: 'Autre', icon: '💬', color: 'bg-gray-100 text-gray-700' },
];

export default function CercleFaiseursPage() {
  const { t } = useI18n();
  const [posts, setPosts] = useState<CerclePost[]>([]);
  const [loading, setLoading] = useState(true);
  const [showCreate, setShowCreate] = useState(false);
  const [filterCategory, setFilterCategory] = useState('');
  const [newPost, setNewPost] = useState({ contenu: '', categorie: 'AUTRE', anonyme: false });

  useEffect(() => { loadPosts(); }, []);

  const loadPosts = async () => {
    try {
      setLoading(true);
      const params = filterCategory ? `?categorie=${filterCategory}` : '';
      const res = await api.get(`/cercle-faiseurs${params}`);
      setPosts(res.data.content || res.data || []);
    } catch { setPosts([]); } finally { setLoading(false); }
  };

  const createPost = async () => {
    if (!newPost.contenu.trim()) { Toast.warning('Veuillez écrire quelque chose'); return; }
    try {
      await api.post('/cercle-faiseurs', newPost);
      Toast.success('Partagé avec le cercle !');
      setShowCreate(false);
      setNewPost({ contenu: '', categorie: 'AUTRE', anonyme: false });
      loadPosts();
    } catch { Toast.error('Erreur lors du partage'); }
  };

  const likePost = async (id: string) => {
    try { await api.post(`/cercle-faiseurs/${id}/like`); loadPosts(); } catch {}
  };

  const getCatInfo = (key: string) => CATEGORIES.find(c => c.key === key) || CATEGORIES[4];

  return (
    <div className="min-h-screen p-4 md:p-8 max-w-4xl mx-auto">
      <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4 mb-8">
        <div>
          <h1 className="text-2xl md:text-3xl font-bold text-gray-900 dark:text-white flex items-center gap-3">
            <Users className="w-8 h-8 text-emerald-500" />
            Cercle de Faiseurs
          </h1>
          <p className="text-gray-500 dark:text-gray-400 mt-1">
            Espace d'entraide confidentiel entre faiseurs — partagez défis, succès et méthodes
          </p>
        </div>
        <button onClick={() => setShowCreate(true)}
          className="px-4 py-2 rounded-xl bg-gradient-to-r from-emerald-600 to-teal-600 text-white text-sm font-medium hover:from-emerald-700 hover:to-teal-700 transition-all shadow-lg shadow-emerald-500/25 flex items-center gap-2">
          <Plus className="w-4 h-4" /> Partager
        </button>
      </div>

      <div className="flex gap-2 mb-6 flex-wrap">
        <button onClick={() => setFilterCategory('')}
          className={`px-3 py-1.5 rounded-lg text-xs font-medium transition-all ${!filterCategory ? 'bg-emerald-600 text-white' : 'bg-gray-100 dark:bg-white/5 text-gray-600 dark:text-gray-400'}`}>
          Tous
        </button>
        {CATEGORIES.map(c => (
          <button key={c.key} onClick={() => setFilterCategory(c.key)}
            className={`px-3 py-1.5 rounded-lg text-xs font-medium transition-all ${filterCategory === c.key ? 'bg-emerald-600 text-white' : 'bg-gray-100 dark:bg-white/5 text-gray-600 dark:text-gray-400'}`}>
            {c.icon} {c.label}
          </button>
        ))}
      </div>

      {loading ? <SkeletonLoader lines={4} variant="card" /> : posts.length === 0 ? (
        <EmptyState icon={<Users className="w-8 h-8 text-gray-400" />} title="Le cercle est vide"
          message="Soyez le premier à partager avec vos collègues faiseurs"
          action={{ label: 'Partager', onClick: () => setShowCreate(true) }} />
      ) : (
        <div className="space-y-4">
          {posts.map(post => {
            const cat = getCatInfo(post.categorie);
            return (
              <div key={post.id} className="p-5 rounded-2xl border border-gray-200 dark:border-white/10 bg-white dark:bg-white/5">
                <div className="flex items-center gap-2 mb-3">
                  <span className="text-lg">{cat.icon}</span>
                  <span className={`px-2 py-0.5 rounded-full text-xs font-medium ${cat.color}`}>{cat.label}</span>
                  {post.anonyme && <span className="flex items-center gap-1 text-xs text-gray-400"><Shield className="w-3 h-3" />Anonyme</span>}
                </div>
                <p className="text-sm text-gray-700 dark:text-gray-300 mb-3 whitespace-pre-wrap">{post.contenu}</p>
                <div className="flex items-center justify-between">
                  <div className="flex items-center gap-2">
                    {!post.anonyme && (
                      <span className="text-xs text-gray-500">{post.auteur.firstName} {post.auteur.lastName}</span>
                    )}
                    <span className="text-xs text-gray-400">• {new Date(post.createdAt).toLocaleDateString('fr-FR')}</span>
                  </div>
                  <div className="flex items-center gap-3">
                    <button onClick={() => likePost(post.id)} className="flex items-center gap-1 text-xs text-gray-400 hover:text-rose-500">
                      <Heart className="w-3.5 h-3.5" /> {post.likes}
                    </button>
                    <span className="flex items-center gap-1 text-xs text-gray-400">
                      <MessageCircle className="w-3.5 h-3.5" /> {post.commentaires}
                    </span>
                  </div>
                </div>
              </div>
            );
          })}
        </div>
      )}

      {showCreate && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
          <div className="fixed inset-0 bg-black/50 backdrop-blur-sm" onClick={() => setShowCreate(false)} />
          <div className="relative bg-white dark:bg-gray-800 rounded-2xl shadow-2xl max-w-lg w-full p-6 border border-gray-200 dark:border-white/10">
            <h2 className="text-lg font-bold text-gray-900 dark:text-white mb-4">Partager au cercle</h2>
            <div className="space-y-4">
              <div>
                <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">Catégorie</label>
                <div className="flex gap-2 flex-wrap">
                  {CATEGORIES.map(c => (
                    <button key={c.key} onClick={() => setNewPost({ ...newPost, categorie: c.key })}
                      className={`px-3 py-1.5 rounded-lg text-xs font-medium transition-all ${newPost.categorie === c.key ? 'bg-emerald-600 text-white' : 'bg-gray-100 dark:bg-white/5 text-gray-600'}`}>
                      {c.icon} {c.label}
                    </button>
                  ))}
                </div>
              </div>
              <textarea value={newPost.contenu} onChange={e => setNewPost({ ...newPost, contenu: e.target.value })}
                rows={5} className="w-full px-4 py-2.5 rounded-xl border border-gray-200 dark:border-white/10 bg-white dark:bg-white/5 text-gray-900 dark:text-white text-sm focus:outline-none focus:ring-2 focus:ring-emerald-500 resize-none"
                placeholder="Partagez votre expérience, un défi, une méthode..." />
              <label className="flex items-center gap-2 text-sm text-gray-700 dark:text-gray-300">
                <input type="checkbox" checked={newPost.anonyme} onChange={e => setNewPost({ ...newPost, anonyme: e.target.checked })} className="w-4 h-4 text-emerald-600 rounded" />
                Publier anonymement
              </label>
            </div>
            <div className="flex justify-end gap-3 mt-6">
              <button onClick={() => setShowCreate(false)} className="px-4 py-2 rounded-xl border border-gray-200 dark:border-white/10 text-gray-700 dark:text-gray-300 text-sm">Annuler</button>
              <button onClick={createPost} className="px-4 py-2 rounded-xl bg-emerald-600 text-white text-sm font-medium hover:bg-emerald-700 transition-all flex items-center gap-2">
                <Send className="w-4 h-4" /> Partager
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
