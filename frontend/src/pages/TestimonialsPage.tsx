import React, { useState, useEffect } from 'react';
import { useI18n } from '@/i18n';
import api from '@/lib/api';
import SkeletonLoader from '@/components/shared/SkeletonLoader';
import EmptyState from '@/components/shared/EmptyState';
import Toast from '@/components/shared/Toast';
import { Heart, Plus, ThumbsUp, MessageCircle, Shield, Clock } from 'lucide-react';

interface Testimony {
  id: string;
  titre: string;
  contenu: string;
  categorie: 'GUERISON' | 'DELIVRANCE' | 'PROVISION' | 'FAMILLE' | 'CONVERSION' | 'AUTRE';
  statut: 'BROUILLON' | 'EN_ATTENTE' | 'APPROUVE' | 'REFUSE';
  auteur: { firstName: string; lastName: string };
  likes: number;
  commentaires: number;
  createdAt: string;
  isLiked?: boolean;
}

const CATEGORIES = [
  { key: 'GUERISON', label: 'Guérison', icon: '💚', color: 'bg-green-100 text-green-700' },
  { key: 'DELIVRANCE', label: 'Délivrance', icon: '🕊️', color: 'bg-blue-100 text-blue-700' },
  { key: 'PROVISION', label: 'Provision', icon: '✨', color: 'bg-amber-100 text-amber-700' },
  { key: 'FAMILLE', label: 'Famille', icon: '👨‍👩‍👧‍👦', color: 'bg-pink-100 text-pink-700' },
  { key: 'CONVERSION', label: 'Conversion', icon: '🌟', color: 'bg-purple-100 text-purple-700' },
  { key: 'AUTRE', label: 'Autre', icon: '🙏', color: 'bg-gray-100 text-gray-700' },
];

export default function TestimonialsPage() {
  const { t } = useI18n();
  const [testimonies, setTestimonies] = useState<Testimony[]>([]);
  const [loading, setLoading] = useState(true);
  const [showCreate, setShowCreate] = useState(false);
  const [filterCategory, setFilterCategory] = useState('');
  const [newTestimony, setNewTestimony] = useState({
    titre: '',
    contenu: '',
    categorie: 'AUTRE',
  });

  useEffect(() => { loadTestimonies(); }, []);

  const loadTestimonies = async () => {
    try {
      setLoading(true);
      const params = filterCategory ? `?categorie=${filterCategory}` : '';
      const res = await api.get(`/testimonies${params}`);
      setTestimonies(res.data.content || res.data || []);
    } catch {
      setTestimonies([]);
    } finally {
      setLoading(false);
    }
  };

  const createTestimony = async () => {
    if (!newTestimony.titre.trim() || !newTestimony.contenu.trim()) {
      Toast.warning('Veuillez remplir tous les champs');
      return;
    }
    try {
      await api.post('/testimonies', newTestimony);
      Toast.success('Témoignage soumis pour approbation');
      setShowCreate(false);
      setNewTestimony({ titre: '', contenu: '', categorie: 'AUTRE' });
      loadTestimonies();
    } catch {
      Toast.error('Erreur lors de la soumission');
    }
  };

  const likeTestimony = async (id: string) => {
    try {
      await api.post(`/testimonies/${id}/like`);
      setTestimonies(testimonies.map(t =>
        t.id === id ? { ...t, likes: t.likes + 1, isLiked: true } : t
      ));
    } catch {
      // silently fail
    }
  };

  const getCategorieInfo = (key: string) => CATEGORIES.find(c => c.key === key) || CATEGORIES[5];

  return (
    <div className="min-h-screen p-4 md:p-8 max-w-5xl mx-auto">
      <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4 mb-8">
        <div>
          <h1 className="text-2xl md:text-3xl font-bold text-gray-900 dark:text-white flex items-center gap-3">
            <Heart className="w-8 h-8 text-rose-500" />
            Témoignages
          </h1>
          <p className="text-gray-500 dark:text-gray-400 mt-1">
            Partagez et lisez les témoignages de la communauté
          </p>
        </div>
        <button
          onClick={() => setShowCreate(true)}
          className="px-4 py-2 rounded-xl bg-gradient-to-r from-rose-500 to-pink-500 text-white text-sm font-medium hover:from-rose-600 hover:to-pink-600 transition-all shadow-lg shadow-rose-500/25 flex items-center gap-2"
        >
          <Plus className="w-4 h-4" />
          Témoigner
        </button>
      </div>

      {/* Category filter */}
      <div className="flex gap-2 mb-6 flex-wrap">
        <button
          onClick={() => { setFilterCategory(''); }}
          className={`px-3 py-1.5 rounded-lg text-xs font-medium transition-all ${
            !filterCategory ? 'bg-rose-600 text-white' : 'bg-gray-100 dark:bg-white/5 text-gray-600 dark:text-gray-400 hover:bg-gray-200 dark:hover:bg-white/10'
          }`}
        >
          Tous
        </button>
        {CATEGORIES.map(c => (
          <button
            key={c.key}
            onClick={() => setFilterCategory(c.key)}
            className={`px-3 py-1.5 rounded-lg text-xs font-medium transition-all ${
              filterCategory === c.key ? 'bg-rose-600 text-white' : 'bg-gray-100 dark:bg-white/5 text-gray-600 dark:text-gray-400 hover:bg-gray-200 dark:hover:bg-white/10'
            }`}
          >
            {c.icon} {c.label}
          </button>
        ))}
      </div>

      {loading ? (
        <SkeletonLoader lines={4} variant="card" />
      ) : testimonies.length === 0 ? (
        <EmptyState
          icon={<Heart className="w-8 h-8 text-gray-400" />}
          title="Aucun témoignage"
          message="Soyez le premier à partager votre témoignage"
          action={{ label: 'Témoigner', onClick: () => setShowCreate(true) }}
        />
      ) : (
        <div className="grid gap-4 md:grid-cols-2">
          {testimonies.map(testimony => {
            const catInfo = getCategorieInfo(testimony.categorie);
            return (
              <div key={testimony.id} className="p-5 rounded-2xl border border-gray-200 dark:border-white/10 bg-white dark:bg-white/5 hover:shadow-lg transition-all">
                <div className="flex items-start justify-between mb-3">
                  <div className="flex items-center gap-2">
                    <span className="text-xl">{catInfo.icon}</span>
                    <span className={`px-2 py-0.5 rounded-full text-xs font-medium ${catInfo.color}`}>
                      {catInfo.label}
                    </span>
                  </div>
                  {testimony.statut === 'EN_ATTENTE' && (
                    <span className="px-2 py-0.5 rounded-full text-xs font-medium bg-amber-100 text-amber-700 flex items-center gap-1">
                      <Shield className="w-3 h-3" /> En attente
                    </span>
                  )}
                </div>
                <h3 className="font-semibold text-gray-900 dark:text-white mb-2">{testimony.titre}</h3>
                <p className="text-sm text-gray-600 dark:text-gray-400 line-clamp-4 mb-4">{testimony.contenu}</p>
                <div className="flex items-center justify-between">
                  <div className="flex items-center gap-2">
                    <div className="w-6 h-6 rounded-full bg-gradient-to-br from-rose-400 to-pink-500 flex items-center justify-center text-white text-xs font-bold">
                      {testimony.auteur.firstName?.[0]}{testimony.auteur.lastName?.[0]}
                    </div>
                    <span className="text-xs text-gray-500 dark:text-gray-400">
                      {testimony.auteur.firstName} {testimony.auteur.lastName}
                    </span>
                    <span className="text-xs text-gray-400">•</span>
                    <span className="text-xs text-gray-400">
                      {new Date(testimony.createdAt).toLocaleDateString('fr-FR')}
                    </span>
                  </div>
                  <div className="flex items-center gap-3">
                    <button
                      onClick={() => likeTestimony(testimony.id)}
                      className={`flex items-center gap-1 text-xs transition-all ${
                        testimony.isLiked ? 'text-rose-500' : 'text-gray-400 hover:text-rose-500'
                      }`}
                    >
                      <ThumbsUp className="w-3.5 h-3.5" />
                      {testimony.likes}
                    </button>
                    <span className="flex items-center gap-1 text-xs text-gray-400">
                      <MessageCircle className="w-3.5 h-3.5" />
                      {testimony.commentaires}
                    </span>
                  </div>
                </div>
              </div>
            );
          })}
        </div>
      )}

      {/* Create Modal */}
      {showCreate && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
          <div className="fixed inset-0 bg-black/50 backdrop-blur-sm" onClick={() => setShowCreate(false)} />
          <div className="relative bg-white dark:bg-gray-800 rounded-2xl shadow-2xl max-w-lg w-full p-6 border border-gray-200 dark:border-white/10">
            <h2 className="text-lg font-bold text-gray-900 dark:text-white mb-4">Partager un témoignage</h2>
            <div className="space-y-4">
              <div>
                <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">Catégorie</label>
                <div className="grid grid-cols-3 gap-2">
                  {CATEGORIES.map(c => (
                    <button
                      key={c.key}
                      onClick={() => setNewTestimony({ ...newTestimony, categorie: c.key })}
                      className={`p-2 rounded-xl border text-center text-sm transition-all ${
                        newTestimony.categorie === c.key
                          ? 'border-rose-500 bg-rose-50 dark:bg-rose-500/10'
                          : 'border-gray-200 dark:border-white/10 hover:border-gray-300'
                      }`}
                    >
                      <span className="text-lg">{c.icon}</span>
                      <div className="text-xs mt-1">{c.label}</div>
                    </button>
                  ))}
                </div>
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">Titre *</label>
                <input
                  type="text"
                  value={newTestimony.titre}
                  onChange={e => setNewTestimony({ ...newTestimony, titre: e.target.value })}
                  className="w-full px-4 py-2.5 rounded-xl border border-gray-200 dark:border-white/10 bg-white dark:bg-white/5 text-gray-900 dark:text-white text-sm focus:outline-none focus:ring-2 focus:ring-rose-500"
                  placeholder="Résumé du témoignage"
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">Votre témoignage *</label>
                <textarea
                  value={newTestimony.contenu}
                  onChange={e => setNewTestimony({ ...newTestimony, contenu: e.target.value })}
                  rows={5}
                  className="w-full px-4 py-2.5 rounded-xl border border-gray-200 dark:border-white/10 bg-white dark:bg-white/5 text-gray-900 dark:text-white text-sm focus:outline-none focus:ring-2 focus:ring-rose-500 resize-none"
                  placeholder="Racontez ce que Dieu a fait dans votre vie..."
                />
              </div>
              <p className="text-xs text-gray-400 flex items-center gap-1">
                <Shield className="w-3 h-3" />
                Votre témoignage sera publié après approbation pastorale.
              </p>
            </div>
            <div className="flex justify-end gap-3 mt-6">
              <button onClick={() => setShowCreate(false)} className="px-4 py-2 rounded-xl border border-gray-200 dark:border-white/10 text-gray-700 dark:text-gray-300 text-sm hover:bg-gray-50 dark:hover:bg-white/5 transition-all">
                Annuler
              </button>
              <button onClick={createTestimony} className="px-4 py-2 rounded-xl bg-rose-600 text-white text-sm font-medium hover:bg-rose-700 transition-all">
                Soumettre
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
