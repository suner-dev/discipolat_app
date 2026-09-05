import React, { useState, useEffect } from 'react';
import { useI18n } from '@/i18n';
import api, { getErrorMessage } from '@/lib/api';
import SkeletonLoader from '@/components/shared/SkeletonLoader';
import EmptyState from '@/components/shared/EmptyState';
import toast from 'react-hot-toast';
import { Zap, Plus, Play, Pause, Trash2, Clock, CheckCircle2, AlertTriangle } from 'lucide-react';

interface AutomationRule {
  id: string;
  titre: string;
  description: string;
  triggerEvent: string;
  triggerParams: string;
  actionType: string;
  actionParams: string;
  statut: 'ACTIVE' | 'EN_PAUSE' | 'DÉSACTIVÉE';
  totalExécutions: number;
  dernièreExécution?: string;
  createdAt: string;
}

const TRIGGERS: Record<string, string> = {
  ABSENCE_SOUTENUE: 'Membre absent X semaines',
  NOUVEAU_MEMBRE: 'Nouveau membre rejoint',
  RAPPORT_SOUMIS: 'Rapport soumis',
  RAPPORT_EN_RETARD: 'Rapport en retard',
  PRIÈRE_CRÉÉE: 'Nouvelle prière',
  ÉVÉNEMENT_CRÉÉ: 'Nouvel événement',
  DEMANDE_TRANSFERT: 'Demande de transfert',
  SCORE_SPRITUEL_BAISSE: 'Score en baisse',
  QUOTIDIEN: 'Chaque jour',
  HEBDOMADAIRE: 'Chaque semaine',
};

const ACTIONS: Record<string, string> = {
  ENVOYER_MESSAGE: 'Envoyer un message',
  ENVOYER_EMAIL: 'Envoyer un email',
  ASSIGNER_FISEUR: 'Assigner un faiseur',
  CRÉER_ALERTE: 'Créer une alerte',
  CRÉER_SUIVI: 'Créer un suivi',
  NOTIFIER_ROLE: 'Notifier un rôle',
};

export default function AutomationsPage() {
  const { t, locale } = useI18n();
  const [rules, setRules] = useState<AutomationRule[]>([]);
  const [loading, setLoading] = useState(true);
  const [showCreate, setShowCreate] = useState(false);
  const [stats, setStats] = useState({ totalRègles: 0, règlesEnPause: 0 });
  const [newRule, setNewRule] = useState({
    titre: '', description: '', triggerEvent: 'ABSENCE_SOUTENUE',
    triggerParams: '{"semaines": 3}', actionType: 'ENVOYER_MESSAGE',
    actionParams: '{"message": "Rappel pastoral"}',
  });

  useEffect(() => { loadRules(); loadStats(); }, []);

  const loadRules = async () => {
    try { setLoading(true); const res = await api.get('/automations'); setRules(res.data.content || res.data || []); }
    catch (e) { toast.error(getErrorMessage(e)); setRules([]); } finally { setLoading(false); }
  };

  const loadStats = async () => { try { const res = await api.get('/automations/stats'); setStats(res.data); } catch (e) { toast.error(getErrorMessage(e)); } };

  const createRule = async () => {
    if (!newRule.titre.trim()) { toast('Titre requis', { icon: '⚠️' }); return; }
    try {
      await api.post('/automations', newRule);
      toast.success('Automatisation créée !');
      setShowCreate(false);
      setNewRule({ titre: '', description: '', triggerEvent: 'ABSENCE_SOUTENUE', triggerParams: '{"semaines": 3}', actionType: 'ENVOYER_MESSAGE', actionParams: '{"message": "Rappel pastoral"}' });
      loadRules(); loadStats();
    } catch (e) { toast.error(getErrorMessage(e)); }
  };

  const toggleRule = async (id: string) => {
    try { await api.patch(`/automations/${id}/toggle`); toast.success('Statut mis à jour'); loadRules(); loadStats(); }
    catch (e) { toast.error(getErrorMessage(e)); }
  };

  const deleteRule = async (id: string) => {
    try { await api.delete(`/automations/${id}`); toast.success('Supprimée'); loadRules(); loadStats(); }
    catch (e) { toast.error(getErrorMessage(e)); }
  };

  return (
    <div className="min-h-screen p-4 md:p-8 max-w-7xl mx-auto">
      <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4 mb-8">
        <div>
          <h1 className="text-2xl md:text-3xl font-bold text-gray-900 dark:text-white flex items-center gap-3">
            <Zap className="w-8 h-8 text-yellow-500" />
            {t('automations.title')}
          </h1>
          <p className="text-gray-500 dark:text-gray-400 mt-1">Automatisations type "Quand X → faire Y"</p>
        </div>
        <button onClick={() => setShowCreate(true)}
          className="px-4 py-2 rounded-xl bg-gradient-to-r from-yellow-500 to-orange-500 text-white text-sm font-medium hover:from-yellow-600 hover:to-orange-600 transition-all shadow-lg flex items-center gap-2">
          <Plus className="w-4 h-4" /> {t('automations.create')}
        </button>
      </div>

      {/* Stats */}
      <div className="grid grid-cols-3 gap-4 mb-6">
        {[
          { label: 'Actives', value: stats.totalRègles, color: 'text-green-600', icon: Play },
          { label: 'En pause', value: stats.règlesEnPause, color: 'text-amber-600', icon: Pause },
          { label: 'Total exécutions', value: rules.reduce((s, r) => s + r.totalExécutions, 0), color: 'text-blue-600', icon: CheckCircle2 },
        ].map(s => (
          <div key={s.label} className="bg-white dark:bg-white/5 rounded-xl p-4 border border-gray-200 dark:border-white/10 text-center">
            <s.icon className={`w-5 h-5 mx-auto mb-1 ${s.color}`} />
            <div className={`text-2xl font-bold ${s.color}`}>{s.value}</div>
            <div className="text-xs text-gray-500">{s.label}</div>
          </div>
        ))}
      </div>

      {loading ? <SkeletonLoader lines={4} variant="card" /> :
        rules.length === 0 ? (
          <EmptyState icon={<Zap className="w-8 h-8 text-gray-400" />}
            title="Aucune automatisation"
            message="Créez des règles pour automatiser les tâches répétitives"
            action={{ label: 'Créer une automatisation', onClick: () => setShowCreate(true) }} />
        ) : (
          <div className="space-y-3">
            {rules.map(rule => (
              <div key={rule.id} className="bg-white dark:bg-white/5 rounded-xl p-5 border border-gray-200 dark:border-white/10">
                <div className="flex items-start justify-between">
                  <div className="flex-1">
                    <div className="flex items-center gap-2 mb-1">
                      <Zap className={`w-4 h-4 ${rule.statut === 'ACTIVE' ? 'text-yellow-500' : 'text-gray-400'}`} />
                      <h3 className="font-semibold text-gray-900 dark:text-white text-sm">{rule.titre}</h3>
                      <span className={`px-2 py-0.5 rounded-full text-xs font-medium ${rule.statut === 'ACTIVE' ? 'bg-green-100 text-green-700' : rule.statut === 'EN_PAUSE' ? 'bg-amber-100 text-amber-700' : 'bg-gray-100 text-gray-500'}`}>
                        {rule.statut}
                      </span>
                    </div>
                    {rule.description && <p className="text-xs text-gray-500 mb-2">{rule.description}</p>}
                    <div className="flex items-center gap-4 text-xs text-gray-400">
                      <span className="flex items-center gap-1 bg-gray-50 dark:bg-white/5 px-2 py-1 rounded">
                        ⚡ QUAND: <span className="text-gray-600 dark:text-gray-300">{TRIGGERS[rule.triggerEvent] || rule.triggerEvent}</span>
                      </span>
                      <span className="text-gray-300">→</span>
                      <span className="flex items-center gap-1 bg-gray-50 dark:bg-white/5 px-2 py-1 rounded">
                        🎯 ALORS: <span className="text-gray-600 dark:text-gray-300">{ACTIONS[rule.actionType] || rule.actionType}</span>
                      </span>
                    </div>
                    <div className="flex items-center gap-3 mt-2 text-xs text-gray-400">
                      <span className="flex items-center gap-1"><CheckCircle2 className="w-3 h-3" />{rule.totalExécutions} exécutions</span>
                      {rule.dernièreExécution && <span>Dernière: {new Date(rule.dernièreExécution).toLocaleDateString('fr-FR')}</span>}
                    </div>
                  </div>
                  <div className="flex gap-1">
                    <button onClick={() => toggleRule(rule.id)}
                      className={`p-2 rounded-lg transition-all ${rule.statut === 'ACTIVE' ? 'hover:bg-amber-50 text-amber-500' : 'hover:bg-green-50 text-green-500'}`}
                      title={rule.statut === 'ACTIVE' ? 'Mettre en pause' : 'Activer'}>
                      {rule.statut === 'ACTIVE' ? <Pause className="w-4 h-4" /> : <Play className="w-4 h-4" />}
                    </button>
                    <button onClick={() => deleteRule(rule.id)}
                      className="p-2 rounded-lg hover:bg-red-50 text-red-400" title="Supprimer">
                      <Trash2 className="w-4 h-4" />
                    </button>
                  </div>
                </div>
              </div>
            ))}
          </div>
        )}

      {showCreate && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
          <div className="fixed inset-0 bg-black/50 backdrop-blur-sm" onClick={() => setShowCreate(false)} />
          <div className="relative bg-white dark:bg-gray-800 rounded-2xl shadow-2xl max-w-lg w-full p-6 border border-gray-200 dark:border-white/10 max-h-[90vh] overflow-y-auto">
            <h2 className="text-lg font-bold text-gray-900 dark:text-white mb-4">Nouvelle automatisation</h2>
            <div className="space-y-4">
              <div>
                <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">Titre</label>
                <input type="text" value={newRule.titre} onChange={e => setNewRule({ ...newRule, titre: e.target.value })}
                  className="w-full px-4 py-2.5 rounded-xl border border-gray-200 dark:border-white/10 bg-white dark:bg-white/5 text-sm"
                  placeholder="Ex: Rappel après 3 semaines d'absence" />
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">Description</label>
                <textarea value={newRule.description} onChange={e => setNewRule({ ...newRule, description: e.target.value })}
                  rows={2} className="w-full px-4 py-2.5 rounded-xl border border-gray-200 dark:border-white/10 bg-white dark:bg-white/5 text-sm resize-none" />
              </div>

              {/* QUAND */}
              <div className="bg-yellow-50 dark:bg-yellow-500/10 rounded-xl p-4 border border-yellow-200 dark:border-yellow-500/20">
                <div className="flex items-center gap-2 mb-2">
                  <span className="text-lg">⚡</span>
                  <span className="text-sm font-semibold text-yellow-700 dark:text-yellow-400">QUAND (Déclencheur)</span>
                </div>
                <select value={newRule.triggerEvent} onChange={e => setNewRule({ ...newRule, triggerEvent: e.target.value })}
                  className="w-full px-4 py-2.5 rounded-xl border border-yellow-200 dark:border-yellow-500/20 bg-white dark:bg-white/5 text-sm">
                  {Object.entries(TRIGGERS).map(([k, v]) => <option key={k} value={k}>{v}</option>)}
                </select>
                <textarea value={newRule.triggerParams} onChange={e => setNewRule({ ...newRule, triggerParams: e.target.value })}
                  rows={2} className="w-full px-4 py-2.5 rounded-xl border border-yellow-200 dark:border-yellow-500/20 bg-white dark:bg-white/5 text-sm resize-none mt-2 font-mono text-xs"
                  placeholder='{"semaines": 3}' />
              </div>

              {/* ALORS */}
              <div className="bg-green-50 dark:bg-green-500/10 rounded-xl p-4 border border-green-200 dark:border-green-500/20">
                <div className="flex items-center gap-2 mb-2">
                  <span className="text-lg">🎯</span>
                  <span className="text-sm font-semibold text-green-700 dark:text-green-400">ALORS (Action)</span>
                </div>
                <select value={newRule.actionType} onChange={e => setNewRule({ ...newRule, actionType: e.target.value })}
                  className="w-full px-4 py-2.5 rounded-xl border border-green-200 dark:border-green-500/20 bg-white dark:bg-white/5 text-sm">
                  {Object.entries(ACTIONS).map(([k, v]) => <option key={k} value={k}>{v}</option>)}
                </select>
                <textarea value={newRule.actionParams} onChange={e => setNewRule({ ...newRule, actionParams: e.target.value })}
                  rows={2} className="w-full px-4 py-2.5 rounded-xl border border-green-200 dark:border-green-500/20 bg-white dark:bg-white/5 text-sm resize-none mt-2 font-mono text-xs"
                  placeholder='{"message": "Rappel pastoral"}' />
              </div>
            </div>
            <div className="flex justify-end gap-3 mt-6">
              <button onClick={() => setShowCreate(false)} className="px-4 py-2 rounded-xl border text-sm">Annuler</button>
              <button onClick={createRule} className="px-4 py-2 rounded-xl bg-yellow-500 text-white text-sm font-medium hover:bg-yellow-600 flex items-center gap-2">
                <Zap className="w-4 h-4" /> Créer
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
