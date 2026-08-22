import React, { useState, useEffect } from 'react';
import { useI18n } from '@/i18n';
import api from '@/lib/api';
import SkeletonLoader from '@/components/shared/SkeletonLoader';
import EmptyState from '@/components/shared/EmptyState';
import Toast from '@/components/shared/Toast';
import { BookOpen, Plus, CheckCircle2, Clock, Heart, Star } from 'lucide-react';

interface PrayerEntry {
  id: string;
  titre: string;
  contenu: string;
  categorie: 'PRIERE' | 'REMERCIEMENT' | 'DEMANDE' | 'INTERCESSION' | 'ELOGE';
  repondu: boolean;
  dateReponse?: string;
  noteReponse?: string;
  createdAt: string;
}

const CATEGORIES = [
  { key: 'PRIERE', label: 'Prière', icon: '🙏', color: 'bg-blue-100 text-blue-700' },
  { key: 'REMERCIEMENT', label: 'Remerciement', icon: '💚', color: 'bg-green-100 text-green-700' },
  { key: 'DEMANDE', label: 'Demande', icon: '📋', color: 'bg-purple-100 text-purple-700' },
  { key: 'INTERCESSION', label: 'Intercession', icon: '🕊️', color: 'bg-amber-100 text-amber-700' },
  { key: 'ELOGE', label: 'Éloge', icon: '⭐', color: 'bg-pink-100 text-pink-700' },
];

export default function PrayerJournalPage() {
  const { t } = useI18n();
  const [entries, setEntries] = useState<PrayerEntry[]>([]);
  const [loading, setLoading] = useState(true);
  const [showCreate, setShowCreate] = useState(false);
  const [filterCategory, setFilterCategory] = useState('');
  const [newEntry, setNewEntry] = useState({ titre: '', contenu: '', categorie: 'PRIERE' });

  useEffect(() => { loadEntries(); }, []);

  const loadEntries = async () => {
    try {
      setLoading(true);
      const params = filterCategory ? `?categorie=${filterCategory}` : '';
      const res = await api.get(`/prayer-journal${params}`);
      setEntries(res.data.content || res.data || []);
    } catch { setEntries([]); } finally { setLoading(false); }
  };

  const createEntry = async () => {
    if (!newEntry.titre.trim()) { Toast.warning('Titre requis'); return; }
    try { await api.post('/prayer-journal', newEntry); Toast.success('Entrée ajoutée !'); setShowCreate(false); setNewEntry({ titre: '', contenu: '', categorie: 'PRIERE' }); loadEntries(); }
    catch { Toast.error('Erreur'); }
  };

  const markAnswered = async (id: string) => {
    try { await api.post(`/prayer-journal/${id}/answered`); Toast.success('Prière exaucée ! 🎉'); loadEntries(); }
    catch { Toast.error('Erreur'); }
  };

  const getCatInfo = (key: string) => CATEGORIES.find(c => c.key === key) || CATEGORIES[0];
  const answeredCount = entries.filter(e => e.repondu).length;

  return (
    <div className="min-h-screen p-4 md:p-8 max-w-4xl mx-auto">
      <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4 mb-8">
        <div>
          <h1 className="text-2xl md:text-3xl font-bold text-gray-900 dark:text-white flex items-center gap-3">
            <BookOpen className="w-8 h-8 text-indigo-500" />
            Journal de Prière
          </h1>
          <p className="text-gray-500 dark:text-gray-400 mt-1">
            Votre carnet personnel de prières, remerciements et demandes
          </p>
        </div>
        <button onClick={() => setShowCreate(true)}
          className="px-4 py-2 rounded-xl bg-gradient-to-r from-indigo-600 to-purple-600 text-white text-sm font-medium hover:from-indigo-700 hover:to-purple-700 transition-all shadow-lg shadow-indigo-500/25 flex items-center gap-2">
          <Plus className="w-4 h-4" /> Ajouter
        </button>
      </div>

      {entries.length > 0 && (
        <div className="grid grid-cols-3 gap-4 mb-6">
          <div className="p-4 rounded-xl bg-indigo-50 dark:bg-indigo-500/10 border border-indigo-200 dark:border-indigo-500/20">
            <div className="text-2xl font-bold text-indigo-900 dark:text-indigo-300">{entries.length}</div>
            <div className="text-xs text-indigo-700 dark:text-indigo-400">Total prières</div>
          </div>
          <div className="p-4 rounded-xl bg-green-50 dark:bg-green-500/10 border border-green-200 dark:border-green-500/20">
            <div className="text-2xl font-bold text-green-900 dark:text-green-300">{answeredCount}</div>
            <div className="text-xs text-green-700 dark:text-green-400">Exaucées</div>
          </div>
          <div className="p-4 rounded-xl bg-amber-50 dark:bg-amber-500/10 border border-amber-200 dark:border-amber-500/20">
            <div className="text-2xl font-bold text-amber-900 dark:text-amber-300">{entries.length - answeredCount}</div>
            <div className="text-xs text-amber-700 dark:text-amber-400">En attente</div>
          </div>
        </div>
      )}

      <div className="flex gap-2 mb-6 flex-wrap">
        <button onClick={() => setFilterCategory('')}
          className={`px-3 py-1.5 rounded-lg text-xs font-medium transition-all ${!filterCategory ? 'bg-indigo-600 text-white' : 'bg-gray-100 dark:bg-white/5 text-gray-600'}`}>
          Toutes
        </button>
        {CATEGORIES.map(c => (
          <button key={c.key} onClick={() => setFilterCategory(c.key)}
            className={`px-3 py-1.5 rounded-lg text-xs font-medium transition-all ${filterCategory === c.key ? 'bg-indigo-600 text-white' : 'bg-gray-100 dark:bg-white/5 text-gray-600'}`}>
            {c.icon} {c.label}
          </button>
        ))}
      </div>

      {loading ? <SkeletonLoader lines={4} variant="card" /> : entries.length === 0 ? (
        <EmptyState icon={<BookOpen className="w-8 h-8 text-gray-400" />} title="Journal vide"
          message="Commencez à documenter vos prières et remerciements"
          action={{ label: 'Ajouter une prière', onClick: () => setShowCreate(true) }} />
      ) : (
        <div className="space-y-3">
          {entries.map(entry => {
            const cat = getCatInfo(entry.categorie);
            return (
              <div key={entry.id} className={`p-4 rounded-xl border transition-all ${entry.repondu ? 'border-green-200 dark:border-green-500/20 bg-green-50/50 dark:bg-green-500/5' : 'border-gray-200 dark:border-white/10 bg-white dark:bg-white/5'}`}>
                <div className="flex items-start justify-between gap-3">
                  <div className="flex-1">
                    <div className="flex items-center gap-2 mb-1">
                      <span>{cat.icon}</span>
                      <span className={`px-2 py-0.5 rounded-full text-xs font-medium ${cat.color}`}>{cat.label}</span>
                      {entry.repondu && <span className="px-2 py-0.5 rounded-full text-xs font-medium bg-green-100 text-green-700 flex items-center gap-1"><CheckCircle2 className="w-3 h-3" /> Exaucée</span>}
                    </div>
                    <h3 className="text-sm font-semibold text-gray-900 dark:text-white mb-1">{entry.titre}</h3>
                    <p className="text-xs text-gray-500 dark:text-gray-400 line-clamp-3">{entry.contenu}</p>
                    <span className="text-xs text-gray-400 mt-2 block">{new Date(entry.createdAt).toLocaleDateString('fr-FR')}</span>
                  </div>
                  {!entry.repondu && (
                    <button onClick={() => markAnswered(entry.id)}
                      className="px-2 py-1 rounded-lg bg-green-100 text-green-700 text-xs font-medium hover:bg-green-200 transition-all flex items-center gap-1 shrink-0">
                      <Star className="w-3 h-3" /> Exaucée
                    </button>
                  )}
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
            <h2 className="text-lg font-bold text-gray-900 dark:text-white mb-4">Nouvelle entrée</h2>
            <div className="space-y-4">
              <div className="grid grid-cols-5 gap-2">
                {CATEGORIES.map(c => (
                  <button key={c.key} onClick={() => setNewEntry({ ...newEntry, categorie: c.key })}
                    className={`p-2 rounded-xl border text-center text-xs transition-all ${newEntry.categorie === c.key ? 'border-indigo-500 bg-indigo-50 dark:bg-indigo-500/10' : 'border-gray-200 dark:border-white/10'}`}>
                    <span className="text-lg">{c.icon}</span>
                    <div className="mt-1">{c.label}</div>
                  </button>
                ))}
              </div>
              <input type="text" value={newEntry.titre} onChange={e => setNewEntry({ ...newEntry, titre: e.target.value })}
                className="w-full px-4 py-2.5 rounded-xl border border-gray-200 dark:border-white/10 bg-white dark:bg-white/5 text-gray-900 dark:text-white text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500"
                placeholder="Titre de la prière..." />
              <textarea value={newEntry.contenu} onChange={e => setNewEntry({ ...newEntry, contenu: e.target.value })}
                rows={4} className="w-full px-4 py-2.5 rounded-xl border border-gray-200 dark:border-white/10 bg-white dark:bg-white/5 text-gray-900 dark:text-white text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500 resize-none"
                placeholder="Décrivez votre prière..." />
            </div>
            <div className="flex justify-end gap-3 mt-6">
              <button onClick={() => setShowCreate(false)} className="px-4 py-2 rounded-xl border border-gray-200 dark:border-white/10 text-gray-700 dark:text-gray-300 text-sm">Annuler</button>
              <button onClick={createEntry} className="px-4 py-2 rounded-xl bg-indigo-600 text-white text-sm font-medium hover:bg-indigo-700 transition-all">Ajouter</button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
