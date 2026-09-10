import { useEffect, useRef, useState, useCallback } from 'react';
import { Client, Message, StompSubscription } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { useAuth } from '@/contexts/AuthContext';
import ttsEngine from '@/lib/ttsEngine';

/**
 * DTO correspondant au VoiceNotificationDTO du backend.
 */
export interface VoiceNotificationDTO {
  id: string;
  title: string;
  body: string;
  timestamp: string;
  targetRole: string | null;
  targetUserId: string | null;
  actionUrl: string | null;
  priority: 'LOW' | 'NORMAL' | 'HIGH' | 'URGENT';
  locale: string;
}

interface UseVoiceWebSocketOptions {
  url?: string;
  autoSpeak?: boolean;
  onNotification?: (notification: VoiceNotificationDTO) => void;
}

interface UseVoiceWebSocketReturn {
  isConnected: boolean;
  notifications: VoiceNotificationDTO[];
  lastNotification: VoiceNotificationDTO | null;
  connect: () => void;
  disconnect: () => void;
  clearNotifications: () => void;
  speakLastNotification: () => void;
  enableVoice: () => Promise<boolean>;
  isVoiceEnabled: boolean;
}

/**
 * Hook React pour la connexion WebSocket STOMP aux notifications vocales.
 */
export function useVoiceWebSocket(options: UseVoiceWebSocketOptions = {}): UseVoiceWebSocketReturn {
  const { url = '/ws-church', autoSpeak = true, onNotification } = options;
  const { user } = useAuth();
  const token = typeof window !== 'undefined' ? localStorage.getItem('accessToken') : null;
  const clientRef = useRef<Client | null>(null);
  const subscriptionsRef = useRef<StompSubscription[]>([]);

  const [isConnected, setIsConnected] = useState(false);
  const [notifications, setNotifications] = useState<VoiceNotificationDTO[]>([]);
  const [lastNotification, setLastNotification] = useState<VoiceNotificationDTO | null>(null);
  const [isVoiceEnabled, setIsVoiceEnabled] = useState(false);

  const handleNotification = useCallback((notification: VoiceNotificationDTO) => {
    setNotifications((prev) => [notification, ...prev].slice(0, 50));
    setLastNotification(notification);
    onNotification?.(notification);

    if (autoSpeak && isVoiceEnabled && ttsEngine.isAvailable()) {
      const textToRead = `${notification.title}. ${notification.body}`;
      switch (notification.priority) {
        case 'URGENT':
          ttsEngine.stop();
          ttsEngine.speakPriority(textToRead, { rate: 1.0 });
          break;
        case 'HIGH':
          ttsEngine.speakPriority(textToRead, { rate: 0.95 });
          break;
        case 'NORMAL':
          ttsEngine.speak(textToRead);
          break;
        case 'LOW':
          break;
      }
    }
  }, [autoSpeak, isVoiceEnabled, onNotification]);

  const enableVoice = useCallback(async (): Promise<boolean> => {
    const success = await ttsEngine.unlock();
    setIsVoiceEnabled(success);
    return success;
  }, []);

  const speakLastNotification = useCallback(() => {
    if (lastNotification && ttsEngine.isAvailable()) {
      ttsEngine.speak(`${lastNotification.title}. ${lastNotification.body}`);
    }
  }, [lastNotification]);

  const clearNotifications = useCallback(() => {
    setNotifications([]);
    setLastNotification(null);
  }, []);

  const connect = useCallback(() => {
    if (!token || !user) return;

    if (clientRef.current?.connected) {
      clientRef.current.deactivate();
    }

    const client = new Client({
      webSocketFactory: () => new SockJS(url),
      connectHeaders: {
        Authorization: `Bearer ${token}`,
      },
      debug: () => {},
      reconnectDelay: 5000,
      heartbeatIncoming: 4000,
      heartbeatOutgoing: 4000,
    });

    client.onConnect = () => {
      setIsConnected(true);

      // Topic global
      const allSub = client.subscribe('/topic/voice/all', (message: Message) => {
        try {
          const notification = JSON.parse(message.body) as VoiceNotificationDTO;
          handleNotification(notification);
        } catch (e) {
          console.error('[VoiceWS] Erreur parsing:', e);
        }
      });
      subscriptionsRef.current.push(allSub);

      // Topic par rôle
      if (user.activeRole) {
        const roleSub = client.subscribe(`/topic/voice/role/${user.activeRole}`, (message: Message) => {
          try {
            const notification = JSON.parse(message.body) as VoiceNotificationDTO;
            handleNotification(notification);
          } catch (e) {
            console.error('[VoiceWS] Erreur parsing rôle:', e);
          }
        });
        subscriptionsRef.current.push(roleSub);
      }

      // Topic personnel
      if (user.id) {
        const userSub = client.subscribe(`/topic/voice/user/${user.id}`, (message: Message) => {
          try {
            const notification = JSON.parse(message.body) as VoiceNotificationDTO;
            handleNotification(notification);
          } catch (e) {
            console.error('[VoiceWS] Erreur parsing user:', e);
          }
        });
        subscriptionsRef.current.push(userSub);
      }
    };

    client.onDisconnect = () => setIsConnected(false);
    client.onStompError = (frame) => console.error('[VoiceWS] Erreur STOMP:', frame.headers.message);

    client.activate();
    clientRef.current = client;
  }, [token, user, url, handleNotification]);

  const disconnect = useCallback(() => {
    subscriptionsRef.current.forEach((sub) => {
      try { sub.unsubscribe(); } catch (error) { console.error('[VoiceWS] Erreur désinscription:', error); }
    });
    subscriptionsRef.current = [];

    if (clientRef.current) {
      clientRef.current.deactivate();
      clientRef.current = null;
    }
    setIsConnected(false);
  }, []);

  useEffect(() => {
    if (token && user) {
      connect();
    }
    return () => disconnect();
  }, [token, user, connect, disconnect]);

  return {
    isConnected,
    notifications,
    lastNotification,
    connect,
    disconnect,
    clearNotifications,
    speakLastNotification,
    enableVoice,
    isVoiceEnabled,
  };
}

export default useVoiceWebSocket;