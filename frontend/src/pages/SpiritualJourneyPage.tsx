import React, { useState, useEffect } from 'react';
import { useI18n } from '@/i18n';
import api from '@/lib/api';
import SkeletonLoader from '@/components/shared/SkeletonLoader';
import { useAuth } from '@/contexts/AuthContext';
import { Sparkles, CheckCircle2, Circle, Lock, Star, BookOpen, Heart, Users, Award, Plus, Trash2, Filter } from 'lucide-react';

interface JourneyStage {
  id: string;
  titre: string;
  description: string;
  categorie: 'INITIATION' | 'FORMATION' | 'ENGAGEMENT' | 'SERVICE' | 'LEADERSHIP';
  completed: boolean;
  completedAt?: string;
  icon: string;
  order: number;
}

interface JournalEntry {
  id: string;
  titre: string;
  contenu: string;
  type: string;
  favori: boolean;
  createdAt: string;
}

const STAGE_ICONS: Record<string, any> = {
  INITIATION: Sparkles,
  FORMATION: BookOpen,
  ENGAGEMENT: Heart,
  SERVICE: Users,
  LEADERSHIP: Award,
};

const JOURNAL_TYPES = ['PRIÈRE', 'MÉDITATION', 'RÉFLEXION', 'TÉMOIGNAGE', 'GRÂCE', 'AUTRE'];

export default function SpiritualJourneyPage() {
  const { t } = useI18n();
  const { user } = useAuth();
  const [stages, setStages] = useState<JourneyStage[]>([]);
  const [loading, setLoading] = useState(true);

  const [tab, setTab] = useState<'journey' | 'journal'>('journey');
  const [journalEntries, setJournalEntries] = useState<JournalEntry[]>([]);
  const [journalLoading, setJournalLoading] = useState(false);
  const [journalTypeFilter, setJournalTypeFilter] = useState<string>('');
  const [journalView, setJournalView] = useState<'all' | 'favorites'>('all');
  const [journalStats, setJournalStats] = useState<Record<string, any> | null>(null);
  const [selectedEntry, setSelectedEntry] = useState<JournalEntry | null>(null);
  const [showCreate, setShowCreate] = useState(false);
  const [newEntry, setNewEntry] = useState({ titre: '', contenu: '', type: 'RÉFLEXION' });

  useEffect(() => { loadJourney(); }, []);

  useEffect(() => {
    if (tab === 'journal' && user) loadJournal();
  }, [tab, journalTypeFilter, journalView, user]);

  const loadJourney = async () => {
    try {
      setLoading(true);
      const res = await api.get('/spiritual-journey');
      setStages(res.data.content || res.data || []);
    } catch { setStages([]); } finally { setLoading(false); }
  };

  const loadJournal = async () => {
    if (!user) return;
    try {
      setJournalLoading(true);
      let url: string;
      if (journalView === 'favorites') {
        url = `/spiritual-journals/favorites/${user.id}`;
      } else if (journalTypeFilter) {
        url = `/spiritual-journals/by-type/${user.id}/${journalTypeFilter}`;
      } else {
        url = `/spiritual-journals/by-author/${user.id}`;
      }
      const res = await api.get(url);
      setJournalEntries(res.data || []);
      loadJournalStats();
    } catch { setJournalEntries([]); } finally { setJournalLoading(false); }
  };

  const loadJournalStats = async () => {
    if (!user) return;
    try {
      const res = await api.get(`/spiritual-journals/stats/${user.id}`);
      setJournalStats(res.data);
    } catch {}
  };

  const createJournalEntry = async () => {
    if (!newEntry.titre.trim() || !newEntry.contenu.trim()) return;
    try {
      await api.post('/spiritual-journals', { ...newEntry, authorId: user?.id });
      setShowCreate(false);
      setNewEntry({ titre: '', contenu: '', type: 'RÉFLEXION' });
      loadJournal();
    } catch {}
  };

  const toggleFavorite = async (id: string) => {
    try {
      await api.post(`/spiritual-journals/${id}/toggle-favorite`);
      loadJournal();
    } catch {}
  };

  const deleteJournalEntry = async (id: string) => {
    try {
      await api.delete(`/spiritual-journals/${id}`);
      if (selectedEntry?.id === id) setSelectedEntry(null);
      loadJournal();
    } catch {}
  };

  const viewDetail = async (id: string) => {
    try {
      const res = await api.get(`/spiritual-journals/${id}`);
      setSelectedEntry(res.data);
    } catch {}
  };

  const completedCount = stages.filter(s => s.completed).length;
  const progress = stages.length > 0 ? Math.round((completedCount / stages.length) * 100) : 0;

  const categories = ['INITIATION', 'FORMATION', 'ENGAGEMENT', 'SERVICE', 'LEADERSHIP'];

  return (
    <div className="min-h-screen p-4 md:p-8 max-w-4xl mx-auto">
      <div className="mb-8">
        <h1 className="text-2xl md:text-3xl font-bold text-gray-900 dark:text-white flex items-center gap-3">
          <Sparkles className="w-8 h-8 text-violet-500" />
          Parcours Spirituel
        </h1>
        <p className="text-gray-500 dark:text-gray-400 mt-1">Votre progression spirituelle et journal</p>
      </div>

      <div className="flex gap-2 mb-6">
        <button onClick={() => setTab('journey')} className={`px-4 py-2 rounded-lg text-sm font-medium transition-all ${tab === 'journey' ? 'bg-violet-500 text-white shadow-md' : 'bg-gray-100 dark:bg-white/10 text-gray-600 dark:text-gray-300'}`}>
          <Sparkles className="w-4 h-4 inline mr-1" /> Parcours
        </button>
        <button onClick={() => setTab('journal')} className={`px-4 py-2 rounded-lg text-sm font-medium transition-all ${tab === 'journal' ? 'bg-violet-500 text-white shadow-md' : 'bg-gray-100 dark:bg-white/10 text-gray-600 dark:text-gray-300'}`}>
          <BookOpen className="w-4 h-4 inline mr-1" /> Journal
        </button>
      </div>

      {tab === 'journal' && (
        <div className="mb-6">
          <div className="flex flex-wrap items-center gap-3 mb-4">
            <div className="flex items-center gap-2">
              <Filter className="w-4 h-4 text-gray-400" />
              <select value={journalTypeFilter} onChange={e => setJournalTypeFilter(e.target.value)}
                className="px-3 py-1.5 rounded-lg border border-gray-200 dark:border-white/10 bg-white dark:bg-white/5 text-sm text-gray-900 dark:text-white">
                <option value="">Tous les types</option>
                {JOURNAL_TYPES.map(jt => <option key={jt} value={jt}>{jt}</option>)}
              </select>
            </div>
            <div className="flex rounded-lg overflow-hidden border border-gray-200 dark:border-white/10">
              <button onClick={() => setJournalView('all')} className={`px-3 py-1.5 text-xs font-medium ${journalView === 'all' ? 'bg-violet-500 text-white' : 'bg-white dark:bg-white/5 text-gray-600 dark:text-gray-300'}`}>Tous</button>
              <button onClick={() => setJournalView('favorites')} className={`px-3 py-1.5 text-xs font-medium ${journalView === 'favorites' ? 'bg-violet-500 text-white' : 'bg-white dark:bg-white/5 text-gray-600 dark:text-gray-300'}`}>
                <Star className="w-3 h-3 inline mr-1" /> Favoris
              </button>
            </div>
            <button onClick={() => setShowCreate(true)} className="ml-auto px-3 py-1.5 rounded-lg bg-violet-500 text-white text-xs font-medium hover:bg-violet-600 transition-all flex items-center gap-1">
              <Plus className="w-3 h-3" /> Nouvelle entrée
            </button>
          </div>

          {journalStats && (
            <div className="grid grid-cols-3 gap-3 mb-4">
              {[
                { label: 'Total', value: journalStats.totalEntries ?? 0, color: 'text-violet-600' },
                { label: 'Favoris', value: journalStats.favorites ?? 0, color: 'text-amber-600' },
                { label: 'Ce mois', value: journalStats.thisMonth ?? 0, color: 'text-green-600' },
              ].map(s => (
                <div key={s.label} className="p-3 rounded-xl border border-gray-200 dark:border-white/10 bg-white dark:bg-white/5 text-center">
                  <div className={`text-xl font-bold ${s.color}`}>{s.value}</div>
                  <div className="text-xs text-gray-500">{s.label}</div>
                </div>
              ))}
            </div>
          )}
        </div>
      )}

      {showCreate && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
          <div className="fixed inset-0 bg-black/50 backdrop-blur-sm" onClick={() => setShowCreate(false)} />
          <div className="relative bg-white dark:bg-gray-800 rounded-2xl shadow-2xl max-w-lg w-full p-6 border border-gray-200 dark:border-white/10">
            <h2 className="text-lg font-bold text-gray-900 dark:text-white mb-4">Nouvelle entrée de journal</h2>
            <div className="space-y-4">
              <input type="text" value={newEntry.titre} onChange={e => setNewEntry({ ...newEntry, titre: e.target.value })}
                className="w-full px-4 py-2.5 rounded-xl border border-gray-200 dark:border-white/10 bg-white dark:bg-white/5 text-gray-900 dark:text-white text-sm" placeholder="Titre" />
              <textarea value={newEntry.contenu} onChange={e => setNewEntry({ ...newEntry, contenu: e.target.value })} rows={4}
                className="w-full px-4 py-2.5 rounded-xl border border-gray-200 dark:border-white/10 bg-white dark:bg-white/5 text-gray-900 dark:text-white text-sm resize-none" placeholder="Votre réflexion..." />
              <select value={newEntry.type} onChange={e => setNewEntry({ ...newEntry, type: e.target.value })}
                className="w-full px-4 py-2.5 rounded-xl border border-gray-200 dark:border-white/10 bg-white dark:bg-white/5 text-gray-900 dark:text-white text-sm">
                {JOURNAL_TYPES.map(jt => <option key={jt} value={jt}>{jt}</option>)}
              </select>
            </div>
            <div className="flex justify-end gap-3 mt-6">
              <button onClick={() => setShowCreate(false)} className="px-4 py-2 rounded-xl border text-sm">Annuler</button>
              <button onClick={createJournalEntry} className="px-4 py-2 rounded-xl bg-violet-500 text-white text-sm font-medium hover:bg-violet-600">Enregistrer</button>
            </div>
          </div>
        </div>
      )}

      {selectedEntry && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
          <div className="fixed inset-0 bg-black/50 backdrop-blur-sm" onClick={() => setSelectedEntry(null)} />
          <div className="relative bg-white dark:bg-gray-800 rounded-2xl shadow-2xl max-w-lg w-full p-6 border border-gray-200 dark:border-white/10">
            <div className="flex items-start justify-between mb-3">
              <h2 className="text-lg font-bold text-gray-900 dark:text-white">{selectedEntry.titre}</h2>
              <div className="flex gap-2">
                <button onClick={() => toggleFavorite(selectedEntry.id)} className="p-1.5 rounded-lg hover:bg-amber-50 dark:hover:bg-amber-500/10" title="Favori">
                  <Star className={`w-4 h-4 ${selectedEntry.favori ? 'fill-amber-400 text-amber-400' : 'text-gray-400'}`} />
                </button>
                <button onClick={() => deleteJournalEntry(selectedEntry.id)} className="p-1.5 rounded-lg hover:bg-red-50 dark:hover:bg-red-500/10 text-red-400" title="Supprimer">
                  <Trash2 className="w-4 h-4" />
                </button>
              </div>
            </div>
            <span className="text-xs px-2 py-0.5 rounded-full bg-violet-100 dark:bg-violet-500/20 text-violet-700 dark:text-violet-300">{selectedEntry.type}</span>
            <p className="mt-3 text-sm text-gray-700 dark:text-gray-300 whitespace-pre-wrap">{selectedEntry.contenu}</p>
            <p className="mt-3 text-xs text-gray-400">{new Date(selectedEntry.createdAt).toLocaleDateString('fr-FR')}</p>
            <button onClick={() => setSelectedEntry(null)} className="mt-4 w-full px-4 py-2 rounded-xl border border-gray-200 dark:border-white/10 text-sm text-gray-700 dark:text-gray-300">Fermer</button>
          </div>
        </div>
      )}

      {tab === 'journey' && stages.length > 0 && (
        <div className="p-6 rounded-2xl border border-gray-200 dark:border-white/10 bg-gradient-to-r from-violet-500/10 to-purple-500/10 mb-8">
          <div className="flex items-center justify-between mb-3">
            <span className="text-sm font-medium text-gray-700 dark:text-gray-300">Progression globale</span>
            <span className="text-sm font-bold text-violet-600">{completedCount}/{stages.length} étapes</span>
          </div>
          <div className="h-3 bg-gray-200 dark:bg-white/10 rounded-full overflow-hidden">
            <div className="h-full bg-gradient-to-r from-violet-500 to-purple-500 rounded-full transition-all duration-500" style={{ width: `${progress}%` }} />
          </div>
          <span className="text-xs text-gray-500 mt-2 block">{progress}% complété</span>
        </div>
      )}

      {loading ? <SkeletonLoader lines={6} variant="card" /> : stages.length === 0 ? (
        <div className="text-center py-12">
          <Sparkles className="w-12 h-12 text-gray-300 mx-auto mb-4" />
          <p className="text-gray-500">Votre parcours spirituel sera bientôt disponible</p>
        </div>
      ) : (
        <div className="space-y-8">
          {categories.map(cat => {
            const catStages = stages.filter(s => s.categorie === cat);
            if (catStages.length === 0) return null;
            const Icon = STAGE_ICONS[cat] || Sparkles;
            const catCompleted = catStages.filter(s => s.completed).length;
            return (
              <div key={cat}>
                <div className="flex items-center gap-3 mb-4">
                  <div className="w-10 h-10 rounded-xl bg-violet-100 dark:bg-violet-500/10 flex items-center justify-center">
                    <Icon className="w-5 h-5 text-violet-600" />
                  </div>
                  <div>
                    <h2 className="text-lg font-bold text-gray-900 dark:text-white">{cat}</h2>
                    <p className="text-xs text-gray-500">{catCompleted}/{catStages.length} complétées</p>
                  </div>
                </div>
                <div className="space-y-3 ml-5 border-l-2 border-violet-200 dark:border-violet-500/20 pl-6">
                  {catStages.map(stage => (
                    <div key={stage.id} className={`p-4 rounded-xl border transition-all ${stage.completed ? 'border-green-200 dark:border-green-500/20 bg-green-50/50 dark:bg-green-500/5' : 'border-gray-200 dark:border-white/10 bg-white dark:bg-white/5'}`}>
                      <div className="flex items-center gap-3">
                        {stage.completed ? (
                          <CheckCircle2 className="w-5 h-5 text-green-500 shrink-0" />
                        ) : (
                          <Circle className="w-5 h-5 text-gray-300 shrink-0" />
                        )}
                        <div>
                          <h3 className="text-sm font-semibold text-gray-900 dark:text-white">{stage.titre}</h3>
                          <p className="text-xs text-gray-500 dark:text-gray-400">{stage.description}</p>
                          {stage.completedAt && (
                            <span className="text-xs text-green-600 dark:text-green-400 mt-1 block">
                              Complété le {new Date(stage.completedAt).toLocaleDateString('fr-FR')}
                            </span>
                          )}
                        </div>
                      </div>
                    </div>
                  ))}
                </div>
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
}
