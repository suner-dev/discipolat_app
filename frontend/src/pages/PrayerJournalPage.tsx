import React, { useState, useEffect } from 'react';
import { useI18n } from '@/i18n';
import api from '@/lib/api';
import SkeletonLoader from '@/components/shared/SkeletonLoader';
import EmptyState from '@/components/shared/EmptyState';
import Toast from '@/components/shared/Toast';
import { BookOpen, Plus, CheckCircle2, Heart, Clock, Trash2 } from 'lucide-react';

interface PrayerEntry {
  id: string;
  contenu: string;
  statut: 'EN_COURS' | 'EXAUCÉE' | 'MÉMORISÉE';
  visibilité: string;
  catégorie?: string;
  réponse?: string;
  createdAt: string;
  exaucéeAt?: string;
}

const CATÉGORIES = ['PRIÈRE', 'LOUANGE', 'INTERCESSION', 'REPENTIR', 'GRÂCE', 'AUTRE'];

export default function PrayerJournalPage() {
  const { t } = useI18n();
  const [entries, setEntries] = useState<PrayerEntry[]>([]);
  const [loading, setLoading] = useState(true);
  const [showCreate, setShowCreate] = useState(false);
  const [newEntry, setNewEntry] = useState({ contenu: '', catégorie: 'PRIÈRE', visibilité: 'PRIVÉE' });
  const [stats, setStats] = useState({ total: 0, enCours: 0, exaucées: 0 });

  useEffect(() => { loadEntries(); loadStats(); }, []);

  const loadEntries = async () => {
    try {
      setLoading(true);
      const res = await api.get('/prayer-journal');
      setEntries(res.data.content || res.data || []);
    } catch { setEntries([]); }
    finally { setLoading(false); }
  };

  const loadStats = async () => {
    try {
      const res = await api.get('/prayer-journal/stats');
      setStats(res.data);
    } catch {}
  };

  const createEntry = async () => {
    if (!newEntry.contenu.trim()) { Toast.warning('Écrivez votre prière'); return; }
    try {
      await api.post('/prayer-journal', newEntry);
      Toast.success('Prière ajoutée au journal');
      setShowCreate(false);
      setNewEntry({ contenu: '', catégorie: 'PRIÈRE', visibilité: 'PRIVÉE' });
      loadEntries(); loadStats();
    } catch { Toast.error('Erreur lors de la création'); }
  };

  const markAnswered = async (id: string) => {
    try {
      await api.patch(`/prayer-journal/${id}/answered`, { réponse: 'Exaucée par Dieu' });
      Toast.success('Marquée comme exaucée ! 🙏');
      loadEntries(); loadStats();
    } catch { Toast.error('Erreur'); }
  };

  const markRemembered = async (id: string) => {
    try {
      await api.patch(`/prayer-journal/${id}/remembered`);
      Toast.success('Mémorisée');
      loadEntries(); loadStats();
    } catch { Toast.error('Erreur'); }
  };

  const deleteEntry = async (id: string) => {
    try {
      await api.delete(`/prayer-journal/${id}`);
      Toast.success('Supprimée');
      loadEntries(); loadStats();
    } catch { Toast.error('Erreur'); }
  };

  const getStatutInfo = (s: string) => {
    switch (s) {
      case 'EXAUCÉE': return { color: 'bg-green-100 text-green-700', icon: CheckCircle2, label: 'Exaucée' };
      case 'MÉMORISÉE': return { color: 'bg-purple-100 text-purple-700', icon: Heart, label: 'Mémorisée' };
      default: return { color: 'bg-amber-100 text-amber-700', icon: Clock, label: 'En cours' };
    }
  };

  return (
    <div className="min-h-screen p-4 md:p-8 max-w-7xl mx-auto">
      <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4 mb-8">
        <div>
          <h1 className="text-2xl md:text-3xl font-bold text-gray-900 dark:text-white flex items-center gap-3">
            <BookOpen className="w-8 h-8 text-purple-500" />
            {t('prayerJournal.title')}
          </h1>
          <p className="text-gray-500 dark:text-gray-400 mt-1">Votre carnet de prières personnel</p>
        </div>
        <button onClick={() => setShowCreate(true)}
          className="px-4 py-2 rounded-xl bg-gradient-to-r from-purple-600 to-pink-600 text-white text-sm font-medium hover:from-purple-700 hover:to-pink-700 transition-all shadow-lg shadow-purple-500/25 flex items-center gap-2">
          <Plus className="w-4 h-4" /> Nouvelle prière
        </button>
      </div>

      {/* Stats */}
      <div className="grid grid-cols-3 gap-4 mb-6">
        {[
          { label: 'Total', value: stats.total, color: 'text-purple-600' },
          { label: 'En cours', value: stats.enCours, color: 'text-amber-600' },
          { label: 'Exaucées', value: stats.exaucées, color: 'text-green-600' },
        ].map(s => (
          <div key={s.label} className="bg-white dark:bg-white/5 rounded-xl p-4 border border-gray-200 dark:border-white/10 text-center">
            <div className={`text-2xl font-bold ${s.color}`}>{s.value}</div>
            <div className="text-xs text-gray-500">{s.label}</div>
          </div>
        ))}
      </div>

      {loading ? <SkeletonLoader lines={4} variant="card" /> :
        entries.length === 0 ? (
          <EmptyState icon={<BookOpen className="w-8 h-8 text-gray-400" />}
            title="Aucune prière dans votre journal"
            message="Commencez à écrire vos prières pour garder une trace de votre vie spirituelle"
            action={{ label: 'Ajouter une prière', onClick: () => setShowCreate(true) }} />
        ) : (
          <div className="space-y-3">
            {entries.map(entry => {
              const info = getStatutInfo(entry.statut);
              const StatusIcon = info.icon;
              return (
                <div key={entry.id} className="bg-white dark:bg-white/5 rounded-xl p-4 border border-gray-200 dark:border-white/10">
                  <div className="flex items-start justify-between gap-3">
                    <div className="flex-1">
                      <p className="text-sm text-gray-900 dark:text-white whitespace-pre-wrap">{entry.contenu}</p>
                      <div className="flex items-center gap-2 mt-2">
                        <span className={`px-2 py-0.5 rounded-full text-xs font-medium ${info.color}`}>
                          <StatusIcon className="w-3 h-3 inline mr-1" />{info.label}
                        </span>
                        {entry.catégorie && <span className="px-2 py-0.5 rounded-full text-xs bg-gray-100 dark:bg-white/10 text-gray-600">{entry.catégorie}</span>}
                        <span className="text-xs text-gray-400">{new Date(entry.createdAt).toLocaleDateString('fr-FR')}</span>
                      </div>
                    </div>
                    <div className="flex gap-1">
                      {entry.statut === 'EN_COURS' && (
                        <>
                          <button onClick={() => markAnswered(entry.id)} className="p-1.5 rounded-lg hover:bg-green-50 dark:hover:bg-green-500/10 text-green-500" title="Exaucée">
                            <CheckCircle2 className="w-4 h-4" />
                          </button>
                          <button onClick={() => markRemembered(entry.id)} className="p-1.5 rounded-lg hover:bg-purple-50 dark:hover:bg-purple-500/10 text-purple-500" title="Mémorisée">
                            <Heart className="w-4 h-4" />
                          </button>
                        </>
                      )}
                      <button onClick={() => deleteEntry(entry.id)} className="p-1.5 rounded-lg hover:bg-red-50 dark:hover:bg-red-500/10 text-red-400" title="Supprimer">
                        <Trash2 className="w-4 h-4" />
                      </button>
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
            <h2 className="text-lg font-bold text-gray-900 dark:text-white mb-4">Nouvelle prière</h2>
            <textarea value={newEntry.contenu} onChange={e => setNewEntry({ ...newEntry, contenu: e.target.value })}
              rows={4} className="w-full px-4 py-2.5 rounded-xl border border-gray-200 dark:border-white/10 bg-white dark:bg-white/5 text-gray-900 dark:text-white text-sm focus:outline-none focus:ring-2 focus:ring-purple-500 resize-none"
              placeholder="Écrivez votre prière ici..." />
            <div className="grid grid-cols-2 gap-4 mt-4">
              <select value={newEntry.catégorie} onChange={e => setNewEntry({ ...newEntry, catégorie: e.target.value })}
                className="px-4 py-2.5 rounded-xl border border-gray-200 dark:border-white/10 bg-white dark:bg-white/5 text-gray-900 dark:text-white text-sm">
                {CATÉGORIES.map(c => <option key={c} value={c}>{c}</option>)}
              </select>
              <select value={newEntry.visibilité} onChange={e => setNewEntry({ ...newEntry, visibilité: e.target.value })}
                className="px-4 py-2.5 rounded-xl border border-gray-200 dark:border-white/10 bg-white dark:bg-white/5 text-gray-900 dark:text-white text-sm">
                <option value="PRIVÉE">Privée</option>
                <option value="FAISEUR">Avec mon faiseur</option>
                <option value="FAMILLE">Avec ma famille</option>
              </select>
            </div>
            <div className="flex justify-end gap-3 mt-6">
              <button onClick={() => setShowCreate(false)} className="px-4 py-2 rounded-xl border text-sm">Annuler</button>
              <button onClick={createEntry} className="px-4 py-2 rounded-xl bg-purple-600 text-white text-sm font-medium hover:bg-purple-700">Ajouter</button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
