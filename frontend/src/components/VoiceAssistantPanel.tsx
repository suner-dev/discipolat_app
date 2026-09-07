import { useState } from 'react';
import { Volume2, VolumeX, Bell, BellOff, X, Wifi, WifiOff, Trash2 } from 'lucide-react';
import useVoiceWebSocket from '@/hooks/useVoiceWebSocket';
import ttsEngine from '@/lib/ttsEngine';

/**
 * Panneau de contrôle de l'assistant vocal — affiche les notifications en temps réel
 * et permet de contrôler la lecture vocale.
 */
export function VoiceAssistantPanel() {
  const [isOpen, setIsOpen] = useState(false);
  const {
    isConnected,
    notifications,
    lastNotification,
    enableVoice,
    isVoiceEnabled,
    clearNotifications,
    speakLastNotification,
  } = useVoiceWebSocket({ autoSpeak: true });

  const handleEnableVoice = async () => {
    await enableVoice();
  };

  const handleStopSpeaking = () => {
    ttsEngine.stop();
  };

  return (
    <>
      <button
        onClick={() => setIsOpen(!isOpen)}
        className="fixed bottom-6 right-6 z-50 p-4 bg-indigo-600 text-white rounded-full shadow-lg hover:bg-indigo-700 transition-all hover:scale-105"
        title="Assistant vocal"
      >
        {isOpen ? <X size={24} /> : <Volume2 size={24} />}
        {notifications.length > 0 && !isOpen && (
          <span className="absolute -top-1 -right-1 w-5 h-5 bg-red-500 text-white text-xs rounded-full flex items-center justify-center">
            {notifications.length > 9 ? '9+' : notifications.length}
          </span>
        )}
      </button>

      {isOpen && (
        <div className="fixed bottom-24 right-6 z-50 w-80 bg-white rounded-xl shadow-2xl border border-gray-200 overflow-hidden">
          <div className="p-4 bg-indigo-600 text-white">
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-2">
                <Volume2 size={20} />
                <h3 className="font-semibold">Assistant Vocal</h3>
              </div>
              <div className="flex items-center gap-2">
                {isConnected ? (
                  <Wifi size={16} className="text-green-300" />
                ) : (
                  <WifiOff size={16} className="text-red-300" />
                )}
              </div>
            </div>
            <p className="text-xs text-indigo-200 mt-1">
              {isConnected ? 'Connecté au serveur' : 'Déconnecté'}
              {isVoiceEnabled && ' • Voix active'}
            </p>
          </div>
}


          <div className="p-3 border-b border-gray-100 flex gap-2">
            {!isVoiceEnabled ? (
              <button
                onClick={handleEnableVoice}
                className="flex-1 flex items-center justify-center gap-2 px-3 py-2 bg-indigo-50 text-indigo-700 rounded-lg hover:bg-indigo-100 transition-colors text-sm font-medium"
              >
                <Volume2 size={16} />
                Activer la voix
              </button>
            ) : (
              <button
                onClick={handleStopSpeaking}
                className="flex-1 flex items-center justify-center gap-2 px-3 py-2 bg-red-50 text-red-700 rounded-lg hover:bg-red-100 transition-colors text-sm font-medium"
              >
                <VolumeX size={16} />
                Arrêter
              </button>
            )}
            <button
              onClick={speakLastNotification}
              disabled={!lastNotification}
              className="px-3 py-2 bg-gray-50 text-gray-700 rounded-lg hover:bg-gray-100 transition-colors text-sm disabled:opacity-50"
            >
              <Bell size={16} />
            </button>
            <button
              onClick={clearNotifications}
              disabled={notifications.length === 0}
              className="px-3 py-2 bg-gray-50 text-gray-700 rounded-lg hover:bg-gray-100 transition-colors text-sm disabled:opacity-50"
            >
              <Trash2 size={16} />
            </button>
          </div>

          <div className="max-h-64 overflow-y-auto">
            {notifications.length === 0 ? (
              <div className="p-6 text-center text-gray-400 text-sm">
                <BellOff size={32} className="mx-auto mb-2 opacity-50" />
                <p>Aucune notification</p>
              </div>
            ) : (
              <div className="divide-y divide-gray-50">
                {notifications.slice(0, 10).map((notification) => (
                  <div key={notification.id} className="p-3 hover:bg-gray-50 transition-colors">
                    <div className="flex items-start justify-between gap-2">
                      <div className="flex-1 min-w-0">
                        <p className="text-sm font-medium text-gray-900 truncate">
                          {notification.title}
                        </p>
                        <p className="text-xs text-gray-500 mt-0.5 line-clamp-2">
                          {notification.body}
                        </p>
                        <p className="text-xs text-gray-400 mt-1">
                          {new Date(notification.timestamp).toLocaleTimeString('fr-FR')}
                        </p>
                      </div>
                      <span
                        className={`shrink-0 w-2 h-2 rounded-full mt-1.5 ${
                          notification.priority === 'URGENT'
                            ? 'bg-red-500'
                            : notification.priority === 'HIGH'
                            ? 'bg-orange-500'
                            : notification.priority === 'NORMAL'
                            ? 'bg-blue-500'
                            : 'bg-gray-300'
                        }`}
                      />
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>

          <div className="p-2 bg-gray-50 border-t border-gray-100 text-center">
            <p className="text-xs text-gray-400">
              {notifications.length} notification{notifications.length !== 1 ? 's' : ''}
              {ttsEngine.getQueueLength() > 0 && ` • ${ttsEngine.getQueueLength()} en attente`}
            </p>
          </div>
        </div>
      )}
    </>
  );
}

export default VoiceAssistantPanel;
