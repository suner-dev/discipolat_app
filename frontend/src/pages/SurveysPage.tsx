import React, { useState, useEffect } from 'react';
import { useI18n } from '@/i18n';
import api from '@/lib/api';
import SkeletonLoader from '@/components/shared/SkeletonLoader';
import EmptyState from '@/components/shared/EmptyState';
import Toast from '@/components/shared/Toast';
import { ClipboardList, Plus, BarChart3, Users, Clock, CheckCircle2 } from 'lucide-react';

interface Survey {
  id: string;
  titre: string;
  description: string;
  type: 'CHOIX_UNIQUE' | 'CHOIX_MULTIPLE' | 'ECHAUFFEMENT' | 'TEXTE_LIBRE';
  options: string[];
  statut: 'BROUILLON' | 'ACTIF' | 'FERME';
  totalReponses: number;
  anonyme: boolean;
  creePar: { firstName: string; lastName: string };
  createdAt: string;
  expiresAt?: string;
  results?: Record<string, number>;
}

export default function SurveysPage() {
  const { t } = useI18n();
  const [surveys, setSurveys] = useState<Survey[]>([]);
  const [loading, setLoading] = useState(true);
  const [showCreate, setShowCreate] = useState(false);
  const [selectedSurvey, setSelectedSurvey] = useState<Survey | null>(null);
  const [voteSelection, setVoteSelection] = useState<string[]>([]);
  const [textAnswer, setTextAnswer] = useState('');
  const [newSurvey, setNewSurvey] = useState({
    titre: '',
    description: '',
    type: 'CHOIX_UNIQUE' as Survey['type'],
    options: ['', ''],
    anonyme: true,
    expiresAt: '',
  });

  useEffect(() => { loadSurveys(); }, []);

  const loadSurveys = async () => {
    try {
      setLoading(true);
      const res = await api.get('/surveys');
      setSurveys(res.data.content || res.data || []);
    } catch {
      // Fallback: surveys module may not have backend yet
      setSurveys([]);
    } finally {
      setLoading(false);
    }
  };

  const createSurvey = async () => {
    if (!newSurvey.titre.trim()) {
      Toast.warning('Veuillez entrer un titre');
      return;
    }
    const validOptions = newSurvey.options.filter(o => o.trim());
    if ((newSurvey.type === 'CHOIX_UNIQUE' || newSurvey.type === 'CHOIX_MULTIPLE') && validOptions.length < 2) {
      Toast.warning('Il faut au moins 2 options');
      return;
    }
    try {
      await api.post('/surveys', { ...newSurvey, options: validOptions });
      Toast.success('Sondage créé !');
      setShowCreate(false);
      setNewSurvey({ titre: '', description: '', type: 'CHOIX_UNIQUE', options: ['', ''], anonyme: true, expiresAt: '' });
      loadSurveys();
    } catch {
      Toast.error('Erreur lors de la création');
    }
  };

  const submitVote = async () => {
    if (!selectedSurvey) return;
    try {
      if (selectedSurvey.type === 'TEXTE_LIBRE') {
        await api.post(`/surveys/${selectedSurvey.id}/responses`, { reponse: textAnswer });
      } else {
        await api.post(`/surveys/${selectedSurvey.id}/responses`, { selections: voteSelection });
      }
      Toast.success('Réponse enregistrée !');
      setVoteSelection([]);
      setTextAnswer('');
      loadSurveys();
    } catch {
      Toast.error('Erreur lors de la soumission');
    }
  };

  const getStatutColor = (statut: string) => {
    switch (statut) {
      case 'ACTIF': return 'bg-green-100 text-green-700';
      case 'FERME': return 'bg-gray-100 text-gray-600';
      default: return 'bg-amber-100 text-amber-700';
    }
  };

  const maxResults = selectedSurvey?.results ? Math.max(...Object.values(selectedSurvey.results), 1) : 1;

  return (
    <div className="min-h-screen p-4 md:p-8 max-w-7xl mx-auto">
      <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4 mb-8">
        <div>
          <h1 className="text-2xl md:text-3xl font-bold text-gray-900 dark:text-white flex items-center gap-3">
            <ClipboardList className="w-8 h-8 text-indigo-500" />
            Sondages
          </h1>
          <p className="text-gray-500 dark:text-gray-400 mt-1">
            Créez des sondages rapides et consultez les résultats en temps réel
          </p>
        </div>
        <button
          onClick={() => setShowCreate(true)}
          className="px-4 py-2 rounded-xl bg-gradient-to-r from-indigo-600 to-purple-600 text-white text-sm font-medium hover:from-indigo-700 hover:to-purple-700 transition-all shadow-lg shadow-indigo-500/25 flex items-center gap-2"
        >
          <Plus className="w-4 h-4" />
          Nouveau sondage
        </button>
      </div>

      {loading ? (
        <SkeletonLoader lines={4} variant="card" />
      ) : surveys.length === 0 ? (
        <EmptyState
          icon={<ClipboardList className="w-8 h-8 text-gray-400" />}
          title="Aucun sondage"
          message="Créez votre premier sondage pour recueillir les avis de votre communauté"
          action={{ label: 'Créer un sondage', onClick: () => setShowCreate(true) }}
        />
      ) : (
        <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-3">
          {surveys.map(survey => (
            <button
              key={survey.id}
              onClick={() => { setSelectedSurvey(survey); setVoteSelection([]); setTextAnswer(''); }}
              className={`text-left p-5 rounded-2xl border transition-all ${
                selectedSurvey?.id === survey.id
                  ? 'border-indigo-500 bg-indigo-50 dark:bg-indigo-500/10'
                  : 'border-gray-200 dark:border-white/10 bg-white dark:bg-white/5 hover:border-gray-300 dark:hover:border-white/20'
              }`}
            >
              <div className="flex items-start justify-between mb-3">
                <h3 className="font-semibold text-gray-900 dark:text-white text-sm line-clamp-2">{survey.titre}</h3>
                <span className={`px-2 py-0.5 rounded-full text-xs font-medium shrink-0 ml-2 ${getStatutColor(survey.statut)}`}>
                  {survey.statut}
                </span>
              </div>
              {survey.description && (
                <p className="text-xs text-gray-500 dark:text-gray-400 line-clamp-2 mb-3">{survey.description}</p>
              )}
              <div className="flex items-center gap-4 text-xs text-gray-400">
                <span className="flex items-center gap-1"><Users className="w-3 h-3" />{survey.totalReponses} réponses</span>
                {survey.anonyme && <span className="px-1.5 py-0.5 bg-purple-100 dark:bg-purple-500/20 text-purple-600 rounded">Anonyme</span>}
                {survey.expiresAt && (
                  <span className="flex items-center gap-1"><Clock className="w-3 h-3" />Expire le {new Date(survey.expiresAt).toLocaleDateString('fr-FR')}</span>
                )}
              </div>
            </button>
          ))}
        </div>
      )}

      {/* Vote / Results Panel */}
      {selectedSurvey && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
          <div className="fixed inset-0 bg-black/50 backdrop-blur-sm" onClick={() => setSelectedSurvey(null)} />
          <div className="relative bg-white dark:bg-gray-800 rounded-2xl shadow-2xl max-w-lg w-full p-6 border border-gray-200 dark:border-white/10 max-h-[90vh] overflow-y-auto">
            <div className="flex items-center justify-between mb-4">
              <h2 className="text-lg font-bold text-gray-900 dark:text-white">{selectedSurvey.titre}</h2>
              <button onClick={() => setSelectedSurvey(null)} className="text-gray-400 hover:text-gray-600 text-sm">Fermer</button>
            </div>

            {selectedSurvey.description && (
              <p className="text-sm text-gray-500 dark:text-gray-400 mb-4">{selectedSurvey.description}</p>
            )}

            {/* Results visualization */}
            {selectedSurvey.results && Object.keys(selectedSurvey.results).length > 0 && (
              <div className="mb-6 space-y-3">
                <h3 className="text-sm font-medium text-gray-700 dark:text-gray-300 flex items-center gap-2">
                  <BarChart3 className="w-4 h-4" /> Résultats
                </h3>
                {Object.entries(selectedSurvey.results).map(([option, count]) => (
                  <div key={option}>
                    <div className="flex justify-between text-sm mb-1">
                      <span className="text-gray-700 dark:text-gray-300">{option}</span>
                      <span className="text-gray-500">{count} ({Math.round((count / selectedSurvey.totalReponses) * 100)}%)</span>
                    </div>
                    <div className="h-3 bg-gray-100 dark:bg-white/5 rounded-full overflow-hidden">
                      <div
                        className="h-full bg-gradient-to-r from-indigo-500 to-purple-500 rounded-full transition-all duration-500"
                        style={{ width: `${(count / maxResults) * 100}%` }}
                      />
                    </div>
                  </div>
                ))}
                <p className="text-xs text-gray-400 mt-2">{selectedSurvey.totalReponses} réponse(s) au total</p>
              </div>
            )}

            {/* Vote form */}
            {selectedSurvey.statut === 'ACTIF' && (
              <div className="border-t border-gray-200 dark:border-white/10 pt-4">
                <h3 className="text-sm font-medium text-gray-700 dark:text-gray-300 mb-3">Votre réponse</h3>
                {selectedSurvey.type === 'TEXTE_LIBRE' ? (
                  <textarea
                    value={textAnswer}
                    onChange={e => setTextAnswer(e.target.value)}
                    rows={3}
                    className="w-full px-4 py-2.5 rounded-xl border border-gray-200 dark:border-white/10 bg-white dark:bg-white/5 text-gray-900 dark:text-white text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500 resize-none"
                    placeholder="Votre réponse..."
                  />
                ) : (
                  <div className="space-y-2">
                    {selectedSurvey.options.map((option, i) => (
                      <label key={i} className="flex items-center gap-3 p-3 rounded-xl border border-gray-200 dark:border-white/10 hover:bg-gray-50 dark:hover:bg-white/5 cursor-pointer transition-all">
                        <input
                          type={selectedSurvey.type === 'CHOIX_MULTIPLE' ? 'checkbox' : 'radio'}
                          name={`survey-${selectedSurvey.id}`}
                          value={option}
                          checked={voteSelection.includes(option)}
                          onChange={e => {
                            if (selectedSurvey.type === 'CHOIX_MULTIPLE') {
                              setVoteSelection(e.target.checked
                                ? [...voteSelection, option]
                                : voteSelection.filter(v => v !== option)
                              );
                            } else {
                              setVoteSelection([option]);
                            }
                          }}
                          className="w-4 h-4 text-indigo-600"
                        />
                        <span className="text-sm text-gray-700 dark:text-gray-300">{option}</span>
                      </label>
                    ))}
                  </div>
                )}
                <button
                  onClick={submitVote}
                  disabled={selectedSurvey.type === 'TEXTE_LIBRE' ? !textAnswer.trim() : voteSelection.length === 0}
                  className="w-full mt-4 px-4 py-2.5 rounded-xl bg-indigo-600 text-white text-sm font-medium hover:bg-indigo-700 transition-all disabled:opacity-50 disabled:cursor-not-allowed"
                >
                  Soumettre ma réponse
                </button>
              </div>
            )}
          </div>
        </div>
      )}

      {/* Create Modal */}
      {showCreate && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
          <div className="fixed inset-0 bg-black/50 backdrop-blur-sm" onClick={() => setShowCreate(false)} />
          <div className="relative bg-white dark:bg-gray-800 rounded-2xl shadow-2xl max-w-lg w-full p-6 border border-gray-200 dark:border-white/10">
            <h2 className="text-lg font-bold text-gray-900 dark:text-white mb-4">Nouveau sondage</h2>
            <div className="space-y-4">
              <div>
                <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">Titre *</label>
                <input
                  type="text"
                  value={newSurvey.titre}
                  onChange={e => setNewSurvey({ ...newSurvey, titre: e.target.value })}
                  className="w-full px-4 py-2.5 rounded-xl border border-gray-200 dark:border-white/10 bg-white dark:bg-white/5 text-gray-900 dark:text-white text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500"
                  placeholder="Ex: Quel sujet pour le prochain culte ?"
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">Description</label>
                <textarea
                  value={newSurvey.description}
                  onChange={e => setNewSurvey({ ...newSurvey, description: e.target.value })}
                  rows={2}
                  className="w-full px-4 py-2.5 rounded-xl border border-gray-200 dark:border-white/10 bg-white dark:bg-white/5 text-gray-900 dark:text-white text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500 resize-none"
                />
              </div>
              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">Type</label>
                  <select
                    value={newSurvey.type}
                    onChange={e => setNewSurvey({ ...newSurvey, type: e.target.value as Survey['type'] })}
                    className="w-full px-4 py-2.5 rounded-xl border border-gray-200 dark:border-white/10 bg-white dark:bg-white/5 text-gray-900 dark:text-white text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500"
                  >
                    <option value="CHOIX_UNIQUE">Choix unique</option>
                    <option value="CHOIX_MULTIPLE">Choix multiple</option>
                    <option value="ECHAUFFEMENT">Échelle (1-5)</option>
                    <option value="TEXTE_LIBRE">Texte libre</option>
                  </select>
                </div>
                <div>
                  <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">Expiration</label>
                  <input
                    type="datetime-local"
                    value={newSurvey.expiresAt}
                    onChange={e => setNewSurvey({ ...newSurvey, expiresAt: e.target.value })}
                    className="w-full px-4 py-2.5 rounded-xl border border-gray-200 dark:border-white/10 bg-white dark:bg-white/5 text-gray-900 dark:text-white text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500"
                  />
                </div>
              </div>
              {(newSurvey.type === 'CHOIX_UNIQUE' || newSurvey.type === 'CHOIX_MULTIPLE') && (
                <div>
                  <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">Options</label>
                  {newSurvey.options.map((opt, i) => (
                    <div key={i} className="flex gap-2 mb-2">
                      <input
                        type="text"
                        value={opt}
                        onChange={e => {
                          const updated = [...newSurvey.options];
                          updated[i] = e.target.value;
                          setNewSurvey({ ...newSurvey, options: updated });
                        }}
                        className="flex-1 px-4 py-2 rounded-xl border border-gray-200 dark:border-white/10 bg-white dark:bg-white/5 text-gray-900 dark:text-white text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500"
                        placeholder={`Option ${i + 1}`}
                      />
                      {newSurvey.options.length > 2 && (
                        <button
                          onClick={() => setNewSurvey({ ...newSurvey, options: newSurvey.options.filter((_, j) => j !== i) })}
                          className="px-2 text-red-400 hover:text-red-600"
                        >✕</button>
                      )}
                    </div>
                  ))}
                  <button
                    onClick={() => setNewSurvey({ ...newSurvey, options: [...newSurvey.options, ''] })}
                    className="text-sm text-indigo-600 hover:text-indigo-700"
                  >
                    + Ajouter une option
                  </button>
                </div>
              )}
              <label className="flex items-center gap-2 text-sm text-gray-700 dark:text-gray-300">
                <input
                  type="checkbox"
                  checked={newSurvey.anonyme}
                  onChange={e => setNewSurvey({ ...newSurvey, anonyme: e.target.checked })}
                  className="w-4 h-4 text-indigo-600 rounded"
                />
                Réponses anonymes
              </label>
            </div>
            <div className="flex justify-end gap-3 mt-6">
              <button onClick={() => setShowCreate(false)} className="px-4 py-2 rounded-xl border border-gray-200 dark:border-white/10 text-gray-700 dark:text-gray-300 text-sm hover:bg-gray-50 dark:hover:bg-white/5 transition-all">
                Annuler
              </button>
              <button onClick={createSurvey} className="px-4 py-2 rounded-xl bg-indigo-600 text-white text-sm font-medium hover:bg-indigo-700 transition-all">
                Créer
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
