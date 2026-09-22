import { useEffect, useRef, useState, useCallback } from 'react';
import { Client, Message, StompSubscription } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { useAuth } from '@/contexts/AuthContext';

export interface StreamChatMessage {
  id: string;
  senderName: string;
  content: string;
  messageType: string;
  emoji?: string;
  createdAt: string;
  isSystem?: boolean;
}

export interface StreamChatWebSocketOptions {
  streamId: number;
  url?: string;
  onMessage?: (message: StreamChatMessage) => void;
  onPresenceChange?: (presence: { type: 'JOIN' | 'LEAVE'; userId: string; displayName: string; timestamp: string }) => void;
}

export interface UseStreamChatWebSocketReturn {
  isConnected: boolean;
  messages: StreamChatMessage[];
  sendMessage: (content: string) => void;
  sendReaction: (emoji: string) => void;
  joinStream: () => void;
  leaveStream: () => void;
  connect: () => void;
  disconnect: () => void;
}

export function useStreamChatWebSocket(options: StreamChatWebSocketOptions): UseStreamChatWebSocketReturn {
  const { streamId, url = '/ws', onMessage, onPresenceChange } = options;
  const { user } = useAuth();
  const token = typeof window !== 'undefined' ? localStorage.getItem('accessToken') : null;
  const clientRef = useRef<Client | null>(null);
  const subscriptionsRef = useRef<StompSubscription[]>([]);

  const [isConnected, setIsConnected] = useState(false);
  const [messages, setMessages] = useState<StreamChatMessage[]>([]);

  const handleMessage = useCallback((msg: StreamChatMessage) => {
    setMessages((prev) => [...prev, msg]);
    onMessage?.(msg);
  }, [onMessage]);

  const handlePresence = useCallback((presence: { type: 'JOIN' | 'LEAVE'; userId: string; displayName: string; timestamp: string }) => {
    const systemMsg: StreamChatMessage = {
      id: `system-${Date.now()}`,
      senderName: 'Système',
      content: presence.type === 'JOIN' ? `${presence.displayName} a rejoint le chat` : `${presence.displayName} a quitté le chat`,
      messageType: 'SYSTEM',
      createdAt: presence.timestamp,
      isSystem: true,
    };
    setMessages((prev) => [...prev, systemMsg]);
    onPresenceChange?.(presence);
  }, [onPresenceChange]);

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

      // Subscribe to stream chat messages
      const chatSub = client.subscribe(`/topic/streams/${streamId}/chat`, (message: Message) => {
        try {
          const msg = JSON.parse(message.body) as StreamChatMessage;
          handleMessage(msg);
        } catch (e) {
          console.error('[StreamChatWS] Error parsing message:', e);
        }
      });
      subscriptionsRef.current.push(chatSub);

      // Subscribe to presence (join/leave)
      const presenceSub = client.subscribe(`/topic/streams/${streamId}/presence`, (message: Message) => {
        try {
          const presence = JSON.parse(message.body);
          handlePresence(presence);
        } catch (e) {
          console.error('[StreamChatWS] Error parsing presence:', e);
        }
      });
      subscriptionsRef.current.push(presenceSub);
    };

    client.onDisconnect = () => setIsConnected(false);
    client.onStompError = (frame) => console.error('[StreamChatWS] STOMP Error:', frame.headers.message);

    client.activate();
    clientRef.current = client;
  }, [token, user, url, streamId, handleMessage, handlePresence]);

  const disconnect = useCallback(() => {
    subscriptionsRef.current.forEach((sub) => {
      try { sub.unsubscribe(); } catch (error) { console.error('[StreamChatWS] Unsubscribe error:', error); }
    });
    subscriptionsRef.current = [];

    if (clientRef.current) {
      clientRef.current.deactivate();
      clientRef.current = null;
    }
    setIsConnected(false);
  }, []);

  const sendMessage = useCallback((content: string) => {
    if (!clientRef.current?.connected) return;
    clientRef.current.publish({
      destination: `/app/streams/${streamId}/chat`,
      body: JSON.stringify({ content, senderName: 'Vous' }),
    });
  }, [streamId]);

  const sendReaction = useCallback((emoji: string) => {
    if (!clientRef.current?.connected) return;
    clientRef.current.publish({
      destination: `/app/streams/${streamId}/chat`,
      body: JSON.stringify({ content: emoji, emoji, senderName: 'Vous' }),
    });
  }, [streamId]);

  const joinStream = useCallback(() => {
    if (!clientRef.current?.connected) return;
    clientRef.current.publish({
      destination: `/app/streams/${streamId}/join`,
      body: JSON.stringify({ senderName: 'Vous' }),
    });
  }, [streamId]);

  const leaveStream = useCallback(() => {
    if (!clientRef.current?.connected) return;
    clientRef.current.publish({
      destination: `/app/streams/${streamId}/leave`,
      body: JSON.stringify({}),
    });
  }, [streamId]);

  useEffect(() => {
    if (token && user && streamId) {
      connect();
    }
    return () => disconnect();
  }, [token, user, streamId, connect, disconnect]);

  return {
    isConnected,
    messages,
    sendMessage,
    sendReaction,
    joinStream,
    leaveStream,
    connect,
    disconnect,
  };
}

export default useStreamChatWebSocket;