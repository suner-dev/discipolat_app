import { useState } from 'react';
import { useI18n } from '@/i18n';
import { Megaphone, Send, Users, Mail, Smartphone, MessageSquare, Plus, Check, Clock } from 'lucide-react';

interface Broadcast {
  id: string;
  title: string;
  body: string;
  channel: 'all' | 'push' | 'email' | 'sms' | 'in_app';
  targetRoles: string[];
  sentAt?: string;
  readCount: number;
  totalRecipients: number;
  status: 'draft' | 'sent';
}

const MOCK_BROADCASTS: Broadcast[] = [
  { id: '1', title: 'Culte spécial de jeûne', body: 'Invitation au culte spécial de jeûne et de prière ce samedi.', channel: 'all', targetRoles: ['ALL'], sentAt: '2026-08-20T10:00', readCount: 156, totalRecipients: 230, status: 'sent' },
  { id: '2', title: 'Rappel: Réunion des responsables', body: 'Réunion des responsables de département demain à 14h.', channel: 'push', targetRoles: ['ADMIN', 'PASTEUR', 'RESPONSABLE'], sentAt: '2026-08-19T08:00', readCount: 12, totalRecipients: 15, status: 'sent' },
  { id: '3', title: 'Nouveau programme de formation', body: 'Découvrez notre nouveau programme de formation pour les faiseurs.', channel: 'email', targetRoles: ['FAISEUR'], readCount: 0, totalRecipients: 0, status: 'draft' },
];

export default function BroadcastPage() {
  const { t } = useI18n();
  const [showComposer, setShowComposer] = useState(false);
  const [newTitle, setNewTitle] = useState('');
  const [newBody, setNewBody] = useState('');
  const [newChannel, setNewChannel] = useState<Broadcast['channel']>('all');

  const channelIcon = (ch: string) => {
    if (ch === 'all') return <Megaphone className="w-4 h-4" />;
    if (ch === 'push') return <Smartphone className="w-4 h-4" />;
    if (ch === 'email') return <Mail className="w-4 h-4" />;
    if (ch === 'sms') return <MessageSquare className="w-4 h-4" />;
    return <Send className="w-4 h-4" />;
  };

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-gray-900 dark:text-white flex items-center gap-3">
            <Megaphone className="w-7 h-7 text-orange-500" />
            {t('nav.broadcast') ?? 'Diffusion / Broadcast'}
          </h1>
          <p className="text-sm text-gray-500 mt-1">Envoyez des messages à toute l'église</p>
        </div>
        <button onClick={() => setShowComposer(!showComposer)}
          className="flex items-center gap-2 px-4 py-2 rounded-xl bg-orange-500 text-white font-medium text-sm hover:bg-orange-600 transition">
          <Plus className="w-4 h-4" />
          Nouveau broadcast
        </button>
      </div>

      {/* Composer */}
      {showComposer && (
        <div className="glass rounded-2xl p-6 border border-white/20 dark:border-white/[0.06] space-y-4">
          <h3 className="font-semibold text-gray-900 dark:text-white">Composer un message</h3>
          <input value={newTitle} onChange={(e) => setNewTitle(e.target.value)}
            placeholder="Titre du message"
            className="w-full px-4 py-2.5 rounded-xl border border-gray-200 dark:border-white/10 bg-white dark:bg-white/5 text-sm" />
          <textarea value={newBody} onChange={(e) => setNewBody(e.target.value)}
            placeholder="Contenu du message..."
            rows={4}
            className="w-full px-4 py-2.5 rounded-xl border border-gray-200 dark:border-white/10 bg-white dark:bg-white/5 text-sm resize-none" />
          <div className="flex gap-2">
            <p className="text-xs text-gray-500 pt-2">Canal :</p>
            {(['all', 'push', 'email', 'sms', 'in_app'] as const).map((ch) => (
              <button key={ch} onClick={() => setNewChannel(ch)}
                className={`flex items-center gap-1 px-3 py-1.5 rounded-lg text-xs font-medium transition ${newChannel === ch ? 'bg-orange-500 text-white' : 'bg-white/5 text-gray-500 hover:bg-white/10'}`}>
                {channelIcon(ch)} {ch === 'all' ? 'Tous' : ch.toUpperCase()}
              </button>
            ))}
          </div>
          <div className="flex gap-3 pt-2">
            <button className="px-4 py-2 rounded-xl bg-orange-500 text-white text-sm font-medium hover:bg-orange-600 transition flex items-center gap-2">
              <Send className="w-4 h-4" /> Envoyer maintenant
            </button>
            <button className="px-4 py-2 rounded-xl border border-gray-200 dark:border-white/10 text-sm font-medium text-gray-600 dark:text-gray-300 hover:bg-white/5 transition">
              Sauvegarder brouillon
            </button>
          </div>
        </div>
      )}

      {/* Stats */}
      <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
        <div className="glass rounded-2xl p-5 border border-white/20 dark:border-white/[0.06]">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-xl bg-orange-100 dark:bg-orange-900/30 flex items-center justify-center">
              <Send className="w-5 h-5 text-orange-500" />
            </div>
            <div><p className="text-2xl font-bold text-gray-900 dark:text-white">2</p><p className="text-xs text-gray-500">Messages envoyés</p></div>
          </div>
        </div>
        <div className="glass rounded-2xl p-5 border border-white/20 dark:border-white/[0.06]">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-xl bg-green-100 dark:bg-green-900/30 flex items-center justify-center">
              <Check className="w-5 h-5 text-green-500" />
            </div>
            <div><p className="text-2xl font-bold text-gray-900 dark:text-white">73%</p><p className="text-xs text-gray-500">Taux de lecture</p></div>
          </div>
        </div>
        <div className="glass rounded-2xl p-5 border border-white/20 dark:border-white/[0.06]">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-xl bg-blue-100 dark:bg-blue-900/30 flex items-center justify-center">
              <Users className="w-5 h-5 text-blue-500" />
            </div>
            <div><p className="text-2xl font-bold text-gray-900 dark:text-white">230</p><p className="text-xs text-gray-500">Destinataires</p></div>
          </div>
        </div>
      </div>

      {/* History */}
      <div className="space-y-3">
        <h3 className="font-semibold text-gray-900 dark:text-white">Historique</h3>
        {MOCK_BROADCASTS.map((b) => (
          <div key={b.id} className="glass rounded-2xl p-4 border border-white/20 dark:border-white/[0.06] flex items-center justify-between">
            <div className="flex items-center gap-3">
              <div className={`w-10 h-10 rounded-xl flex items-center justify-center ${b.status === 'sent' ? 'bg-green-100 dark:bg-green-900/30' : 'bg-gray-100 dark:bg-white/5'}`}>
                {channelIcon(b.channel)}
              </div>
              <div>
                <h4 className="font-medium text-gray-900 dark:text-white text-sm">{b.title}</h4>
                <p className="text-xs text-gray-500 line-clamp-1">{b.body}</p>
              </div>
            </div>
            <div className="text-right">
              <span className={`inline-flex items-center gap-1 px-2 py-1 rounded-lg text-xs font-medium ${b.status === 'sent' ? 'bg-green-100 text-green-700 dark:bg-green-900/30 dark:text-green-300' : 'bg-gray-100 text-gray-500 dark:bg-white/5'}`}>
                {b.status === 'sent' ? <><Check className="w-3 h-3" /> Envoyé</> : <><Clock className="w-3 h-3" /> Brouillon</>}
              </span>
              {b.status === 'sent' && (
                <p className="text-xs text-gray-400 mt-1">{b.readCount}/{b.totalRecipients} lus</p>
              )}
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}
