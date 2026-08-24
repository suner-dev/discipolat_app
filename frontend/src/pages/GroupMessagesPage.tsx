import { useState } from 'react';
import { useI18n } from '@/i18n';
import { MessageCircle, Send, Smile, Image, Users } from 'lucide-react';

interface GroupMsg { id: string; groupId: string; groupName: string; senderName: string; content: string; createdAt: string; reactions: number; }

export default function GroupMessagesPage() {
  const { t } = useI18n();
  const [selectedGroup, setSelectedGroup] = useState('dept-worship');
  const GROUPS = [
    { id: 'dept-worship', name: 'Équipe Louange', members: 12, icon: '🎵' },
    { id: 'dept-accueil', name: 'Équipe Accueil', members: 8, icon: '👋' },
    { id: 'famille-mbarga', name: 'Famille Mbarga', members: 6, icon: '👨‍👩‍👧‍👦' },
    { id: 'dept-jeunesse', name: 'Département Jeunesse', members: 25, icon: '🔥' },
  ];
  const MOCK_MSGS: GroupMsg[] = [
    { id: '1', groupId: 'dept-worship', groupName: 'Équipe Louange', senderName: 'Sarah', content: 'Bonsoir à tous ! N\'oubliez pas la répétition demain à 18h 🎶', createdAt: '20:15', reactions: 3 },
    { id: '2', groupId: 'dept-worship', groupName: 'Équipe Louange', senderName: 'David', content: 'Je serai là ! J\'apporte la guitare.', createdAt: '20:18', reactions: 1 },
    { id: '3', groupId: 'dept-worship', groupName: 'Équipe Louange', senderName: 'Grace', content: 'Merci Sarah pour le rappel. Quel sera le thème ?', createdAt: '20:22', reactions: 0 },
  ];
  const messages = MOCK_MSGS.filter(m => m.groupId === selectedGroup);

  return (
    <div className="flex h-[calc(100vh-120px)] gap-4 p-6">
      {/* Group list */}
      <div className="w-64 bg-white/5 backdrop-blur rounded-2xl border border-white/10 overflow-hidden flex flex-col">
        <div className="p-4 border-b border-white/10"><h2 className="text-white font-semibold flex items-center gap-2"><Users className="w-4 h-4" /> {t('groupMessages.groups') || 'Groupes'}</h2></div>
        <div className="flex-1 overflow-y-auto">
          {GROUPS.map(g => (
            <button key={g.id} onClick={() => setSelectedGroup(g.id)} className={`w-full text-left p-3 border-b border-white/5 transition ${selectedGroup === g.id ? 'bg-blue-600/20 border-l-2 border-l-blue-400' : 'hover:bg-white/5'}`}>
              <div className="text-white text-sm font-medium">{g.icon} {g.name}</div>
              <div className="text-xs text-gray-400">{g.members} membres</div>
            </button>
          ))}
        </div>
      </div>

      {/* Chat area */}
      <div className="flex-1 bg-white/5 backdrop-blur rounded-2xl border border-white/10 flex flex-col">
        <div className="p-4 border-b border-white/10"><h3 className="text-white font-semibold">{GROUPS.find(g => g.id === selectedGroup)?.name}</h3></div>
        <div className="flex-1 overflow-y-auto p-4 space-y-3">
          {messages.length === 0 ? (
            <div className="text-center text-gray-500 py-10">{t('groupMessages.empty') || 'Aucun message dans ce groupe'}</div>
          ) : messages.map(msg => (
            <div key={msg.id} className="flex gap-3">
              <div className="w-8 h-8 bg-gradient-to-br from-blue-500 to-purple-500 rounded-full flex items-center justify-center text-white text-xs font-bold">{msg.senderName[0]}</div>
              <div>
                <div className="flex items-center gap-2"><span className="text-white text-sm font-medium">{msg.senderName}</span><span className="text-xs text-gray-500">{msg.createdAt}</span></div>
                <p className="text-gray-300 text-sm">{msg.content}</p>
                {msg.reactions > 0 && <span className="text-xs text-gray-500">❤️ {msg.reactions}</span>}
              </div>
            </div>
          ))}
        </div>
        <div className="p-4 border-t border-white/10 flex items-center gap-2">
          <input className="flex-1 bg-white/10 rounded-xl px-4 py-2 text-white text-sm placeholder-gray-500 outline-none focus:ring-2 focus:ring-blue-500" placeholder={t('groupMessages.placeholder') || 'Écrire un message...'} />
          <button className="p-2 text-gray-400 hover:text-white"><Image className="w-5 h-5" /></button>
          <button className="p-2 bg-blue-600 hover:bg-blue-700 rounded-xl text-white"><Send className="w-5 h-5" /></button>
        </div>
      </div>
    </div>
  );
}
