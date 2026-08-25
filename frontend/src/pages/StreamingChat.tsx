import { useState, useRef, useEffect } from 'react';
import { Send, MessageCircle } from 'lucide-react';

interface ChatMessage {
  id: string;
  author: string;
  text: string;
  timestamp: string;
  isSystem?: boolean;
}

const MOCK_MESSAGES: ChatMessage[] = [
  { id: '1', author: 'Système', text: 'Le stream a commencé ! Bienvenue à tous 🙏', timestamp: '10:00', isSystem: true },
  { id: '2', author: 'Soeur Claire', text: 'Que Dieu soit loué !', timestamp: '10:01' },
  { id: '3', author: 'Frère Paul', text: 'Amen ! Présent depuis Douala 🇨🇲', timestamp: '10:02' },
  { id: '4', author: 'Soeur Marie', text: 'Prions ensemble 🙏', timestamp: '10:03' },
  { id: '5', author: 'Frère Samuel', text: 'Gloire à Dieu pour ce temps de prière', timestamp: '10:04' },
];

export default function StreamingChat() {
  const [messages, setMessages] = useState<ChatMessage[]>(MOCK_MESSAGES);
  const [newMsg, setNewMsg] = useState('');
  const [viewers, setViewers] = useState(47);
  const bottomRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages]);

  // Simulate viewers fluctuation
  useEffect(() => {
    const interval = setInterval(() => {
      setViewers((v) => v + Math.floor(Math.random() * 3) - 1);
    }, 5000);
    return () => clearInterval(interval);
  }, []);

  const sendMessage = () => {
    if (!newMsg.trim()) return;
    const msg: ChatMessage = {
      id: String(Date.now()),
      author: 'Vous',
      text: newMsg,
      timestamp: new Date().toLocaleTimeString('fr-FR', { hour: '2-digit', minute: '2-digit' }),
    };
    setMessages((prev) => [...prev, msg]);
    setNewMsg('');
  };

  const quickReact = (emoji: string) => {
    const msg: ChatMessage = {
      id: String(Date.now()),
      author: 'Vous',
      text: emoji,
      timestamp: new Date().toLocaleTimeString('fr-FR', { hour: '2-digit', minute: '2-digit' }),
    };
    setMessages((prev) => [...prev, msg]);
  };

  return (
    <div className="flex flex-col h-[600px] glass rounded-2xl border border-white/20 dark:border-white/[0.06] overflow-hidden">
      {/* Header */}
      <div className="flex items-center justify-between px-4 py-3 border-b border-gray-100 dark:border-white/[0.06]">
        <div className="flex items-center gap-2">
          <MessageCircle className="w-4 h-4 text-pink-500" />
          <span className="text-sm font-semibold text-gray-900 dark:text-white">Chat en direct</span>
          <span className="flex items-center gap-1 px-2 py-0.5 rounded-lg bg-red-100 dark:bg-red-900/30 text-[10px] font-bold text-red-600">
            🔴 {viewers} connectés
          </span>
        </div>
      </div>

      {/* Messages */}
      <div className="flex-1 overflow-y-auto p-4 space-y-3">
        {messages.map((msg) => (
          <div key={msg.id} className={`flex flex-col ${msg.author === 'Vous' ? 'items-end' : 'items-start'}`}>
            {msg.isSystem ? (
              <div className="px-3 py-1.5 rounded-lg bg-violet-100 dark:bg-violet-900/20 text-xs text-violet-600 dark:text-violet-300 text-center mx-auto">
                {msg.text}
              </div>
            ) : (
              <>
                <div className="flex items-center gap-1.5 mb-0.5">
                  <span className="text-[10px] font-medium text-gray-400">{msg.author}</span>
                  <span className="text-[9px] text-gray-300 dark:text-gray-600">{msg.timestamp}</span>
                </div>
                <div className={`px-3 py-2 rounded-2xl max-w-[80%] text-sm ${
                  msg.author === 'Vous'
                    ? 'bg-pink-500 text-white rounded-br-md'
                    : 'bg-gray-100 dark:bg-white/5 text-gray-700 dark:text-gray-300 rounded-bl-md'
                }`}>
                  {msg.text}
                </div>
              </>
            )}
          </div>
        ))}
        <div ref={bottomRef} />
      </div>

      {/* Quick reactions */}
      <div className="flex gap-2 px-4 py-2 border-t border-gray-100 dark:border-white/[0.06]">
        {['🙏', '❤️', '🔥', '👏', '✨', '💯'].map((emoji) => (
          <button key={emoji} onClick={() => quickReact(emoji)}
            className="w-8 h-8 rounded-lg bg-gray-50 dark:bg-white/5 hover:bg-white/10 text-sm flex items-center justify-center transition hover:scale-110">
            {emoji}
          </button>
        ))}
      </div>

      {/* Input */}
      <div className="flex gap-2 px-4 py-3 border-t border-gray-100 dark:border-white/[0.06]">
        <input value={newMsg} onChange={(e) => setNewMsg(e.target.value)}
          onKeyDown={(e) => e.key === 'Enter' && sendMessage()}
          placeholder="Écrire un message..."
          className="flex-1 px-4 py-2 rounded-xl bg-gray-50 dark:bg-white/5 border border-gray-200 dark:border-white/10 text-sm outline-none focus:ring-2 focus:ring-pink-500/20" />
        <button onClick={sendMessage}
          className="w-10 h-10 rounded-xl bg-pink-500 text-white flex items-center justify-center hover:bg-pink-600 transition">
          <Send className="w-4 h-4" />
        </button>
      </div>
    </div>
  );
}
