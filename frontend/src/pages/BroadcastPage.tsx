import React, { useState, useEffect } from 'react';
import { useI18n } from '@/i18n';
import api from '@/lib/api';
import SkeletonLoader from '@/components/shared/SkeletonLoader';
import EmptyState from '@/components/shared/EmptyState';
import Toast from '@/components/shared/Toast';
import { Megaphone, Plus, Send, Eye, Clock, Users } from 'lucide-react';

interface BroadcastMsg {
  id: string;
  titre: string;
  contenu: string;
  cible: string;
  statut: 'BROUILLON' | 'PROGRAMMÉ' | 'ENVOYÉ' | 'ÉCHOUÉ';
  totalEnvoyé: number;
  totalLu: number;
  createdAt: string;
  envoyéAt?: string;
}

export default function BroadcastPage() {
  const { t } = useI18n();
  const [messages, setMessages] = useState<BroadcastMsg[]>([]);
  const [loading, setLoading] = useState(true);
  const [showCreate, setShowCreate] = useState(false);
  const [newMsg, setNewMsg] = useState({ titre: '', contenu: '', cible: 'TOUS' });

  useEffect(() => { loadMessages(); }, []);

  const loadMessages = async () => {
    try {
      setLoading(true);
      const res = await api.get('/broadcast');
      setMessages(res.data.content || res.data || []);
    } catch { setMessages([]); }
    finally { setLoading(false); }
  };

  const createAndSend = async () => {
    if (!newMsg.titre.trim() || !newMsg.contenu.trim()) { Toast.warning('Remplissez tous les champs'); return; }
    try {
      const res = await api.post('/broadcast', newMsg);
      await api.patch(`/broadcast/${res.data.id}/send`);
      Toast.success('Broadcast envoyé !');
      setShowCreate(false);
      setNewMsg({ titre: '', contenu: '', cible: 'TOUS' });
      loadMessages();
    } catch { Toast.error('Erreur'); }
  };

  const saveDraft = async () => {
    if (!newMsg.titre.trim()) { Toast.warning('Titre requis'); return; }
    try {
      await api.post('/broadcast', newMsg);
      Toast.success('Brouillon sauvegardé');
      setShowCreate(false);
      loadMessages();
    } catch { Toast.error('Erreur'); }
  };

  const getStatutInfo = (s: string) => {
    switch (s) {
      case 'ENVOYÉ': return { color: 'bg-green-100 text-green-700', icon: Send };
      case 'PROGRAMMÉ': return { color: 'bg-blue-100 text-blue-700', icon: Clock };
      case 'ÉCHOUÉ': return { color: 'bg-red-100 text-red-700', icon: Megaphone };
      default: return { color: 'bg-gray-100 text-gray-500', icon: Megaphone };
    }
  };

  return (
    <div className="min-h-screen p-4 md:p-8 max-w-7xl mx-auto">
      <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4 mb-8">
        <div>
          <h1 className="text-2xl md:text-3xl font-bold text-gray-900 dark:text-white flex items-center gap-3">
            <Megaphone className="w-8 h-8 text-rose-500" />
            {t('nav.broadcast')}
          </h1>
          <p className="text-gray-500 dark:text-gray-400 mt-1">Envoi ciblé de messages avec accusé de lecture</p>
        </div>
        <button onClick={() => setShowCreate(true)}
          className="px-4 py-2 rounded-xl bg-gradient-to-r from-rose-500 to-pink-500 text-white text-sm font-medium hover:from-rose-600 hover:to-pink-600 transition-all shadow-lg flex items-center gap-2">
          <Plus className="w-4 h-4" /> {t('broadcast.new')}
        </button>
      </div>

      {loading ? <SkeletonLoader lines={4} variant="card" /> :
        messages.length === 0 ? (
          <EmptyState icon={<Megaphone className="w-8 h-8 text-gray-400" />}
            title="Aucun broadcast"
            message="Envoyez des messages ciblés à votre congrégation"
            action={{ label: 'Nouveau broadcast', onClick: () => setShowCreate(true) }} />
        ) : (
          <div className="space-y-3">
            {messages.map(msg => {
              const info = getStatutInfo(msg.statut);
              const StatusIcon = info.icon;
              return (
                <div key={msg.id} className="bg-white dark:bg-white/5 rounded-xl p-4 border border-gray-200 dark:border-white/10">
                  <div className="flex items-start justify-between">
                    <div className="flex-1">
                      <div className="flex items-center gap-2 mb-1">
                        <StatusIcon className={`w-4 h-4 ${info.color.includes('green') ? 'text-green-500' : info.color.includes('blue') ? 'text-blue-500' : 'text-gray-400'}`} />
                        <h3 className="font-medium text-gray-900 dark:text-white text-sm">{msg.titre}</h3>
                        <span className={`px-2 py-0.5 rounded-full text-xs font-medium ${info.color}`}>{msg.statut}</span>
                      </div>
                      <p className="text-xs text-gray-500 line-clamp-2 mb-2">{msg.contenu}</p>
                      <div className="flex items-center gap-4 text-xs text-gray-400">
                        <span className="flex items-center gap-1"><Users className="w-3 h-3" />{msg.cible}</span>
                        <span className="flex items-center gap-1"><Send className="w-3 h-3" />{msg.totalEnvoyé} envoyés</span>
                        <span className="flex items-center gap-1"><Eye className="w-3 h-3" />{msg.totalLu} lus</span>
                        <span>{new Date(msg.createdAt).toLocaleDateString('fr-FR')}</span>
                      </div>
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
            <h2 className="text-lg font-bold text-gray-900 dark:text-white mb-4">Nouveau broadcast</h2>
            <div className="space-y-4">
              <input type="text" value={newMsg.titre} onChange={e => setNewMsg({ ...newMsg, titre: e.target.value })}
                className="w-full px-4 py-2.5 rounded-xl border border-gray-200 dark:border-white/10 bg-white dark:bg-white/5 text-sm" placeholder="Titre" />
              <textarea value={newMsg.contenu} onChange={e => setNewMsg({ ...newMsg, contenu: e.target.value })}
                rows={4} className="w-full px-4 py-2.5 rounded-xl border border-gray-200 dark:border-white/10 bg-white dark:bg-white/5 text-sm resize-none" placeholder="Message à envoyer..." />
              <select value={newMsg.cible} onChange={e => setNewMsg({ ...newMsg, cible: e.target.value })}
                className="w-full px-4 py-2.5 rounded-xl border border-gray-200 dark:border-white/10 bg-white dark:bg-white/5 text-sm">
                <option value="TOUS">Tous les membres</option>
                <option value="DÉPARTEMENT">Par département</option>
                <option value="FAMILLE">Par famille</option>
                <option value="RÔLE">Par rôle</option>
                <option value="SEGMENT">Segment personnalisé</option>
              </select>
            </div>
            <div className="flex justify-end gap-3 mt-6">
              <button onClick={() => setShowCreate(false)} className="px-4 py-2 rounded-xl border text-sm">Annuler</button>
              <button onClick={saveDraft} className="px-4 py-2 rounded-xl border border-gray-200 dark:border-white/10 text-sm text-gray-600">{t('broadcast.draft')}</button>
              <button onClick={createAndSend} className="px-4 py-2 rounded-xl bg-rose-500 text-white text-sm font-medium hover:bg-rose-600 flex items-center gap-2">
                <Send className="w-4 h-4" /> {t('broadcast.send')}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
