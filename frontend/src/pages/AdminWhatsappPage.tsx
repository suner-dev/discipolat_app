import { useEffect, useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { api, getErrorMessage } from '@/lib/api';
import { toast } from 'react-hot-toast';
import {
  MessageCircle,
  Send,
  RefreshCw,
  CheckCircle2,
  XCircle,
  Inbox,
  Radio,
} from 'lucide-react';

/**
 * Pont WhatsApp ↔ Discipolat (feature #1).
 * Configuration de la WhatsApp Business Cloud API par église,
 * journal des messages, diffusion d'annonces et statistiques.
 */

interface WaConfig {
  configured: boolean;
  enabled: boolean;
  phoneNumberId: string | null;
  displayPhoneNumber: string | null;
  welcomeMessage: string | null;
  hasToken: boolean;
}

interface WaMessage {
  id: string;
  direction: 'INBOUND' | 'OUTBOUND';
  phoneNumber: string;
  status: string;
  kind: string;
  body: string | null;
  createdAt: string;
}

interface WaStats {
  inbound: number;
  outbound: number;
  deliveredOrRead: number;
}

const statusBadge = (s: string) => {
  const map: Record<string, string> = {
    SENT: 'badge badge-info',
    DELIVERED: 'badge badge-success',
    READ: 'badge badge-success',
    QUEUED: 'badge badge-gray',
    FAILED: 'badge badge-danger',
    RECEIVED: 'badge badge-violet',
  };
  return map[s] ?? 'badge badge-gray';
};

export default function AdminWhatsappPage() {
  const qc = useQueryClient();
  const [form, setForm] = useState<Partial<WaConfig> & { accessToken?: string; webhookVerifyToken?: string }>({});
  const [broadcast, setBroadcast] = useState({ titre: '', contenu: '' });

  const { data: config } = useQuery({
    queryKey: ['whatsapp-config'],
    queryFn: async () => (await api.get<WaConfig>('/whatsapp/config')).data,
  });

  const { data: messages } = useQuery({
    queryKey: ['whatsapp-messages'],
    queryFn: async () => (await api.get<WaMessage[]>('/whatsapp/messages', { params: { limit: 50 } })).data,
  });

  const { data: stats } = useQuery({
    queryKey: ['whatsapp-stats'],
    queryFn: async () => (await api.get<WaStats>('/whatsapp/stats')).data,
  });

  useEffect(() => {
    if (config) setForm((f) => ({ ...f, ...config }));
  }, [config]);

  const saveMutation = useMutation({
    mutationFn: async () =>
      (await api.put('/whatsapp/config', form)).data as WaConfig,
    onSuccess: () => {
      toast.success('Configuration WhatsApp enregistrée');
      qc.invalidateQueries({ queryKey: ['whatsapp-config'] });
    },
    onError: (e) => toast.error(getErrorMessage(e)),
  });

  const testMutation = useMutation({
    mutationFn: async () => (await api.post<{ success: boolean; error?: string }>('/whatsapp/config/test')).data,
    onSuccess: (res) => {
      if (res.success) toast.success('Connexion à l’API WhatsApp réussie');
      else toast.error(`Échec : ${res.error}`);
      qc.invalidateQueries({ queryKey: ['whatsapp-config'] });
    },
    onError: (e) => toast.error(getErrorMessage(e)),
  });

  const broadcastMutation = useMutation({
    mutationFn: async () => (await api.post('/whatsapp/broadcast', broadcast)).data,
    onSuccess: () => {
      toast.success('Annonce diffusée aux membres opt-in');
      setBroadcast({ titre: '', contenu: '' });
      qc.invalidateQueries({ queryKey: ['whatsapp-messages'] });
    },
    onError: (e) => toast.error(getErrorMessage(e)),
  });

  return (
    <div className="page-container">
      <div className="page-header">
        <div className="flex h-12 w-12 items-center justify-center rounded-xl bg-gradient-to-br from-green-500 to-emerald-600 text-white shadow-lg">
          <MessageCircle size={24} />
        </div>
        <div>
          <h1 className="page-title">Pont WhatsApp</h1>
          <p className="page-subtitle">
            Diffusez vos annonces et recevez les commandes (#rejoindre, #stop) via la WhatsApp Business Cloud API.
          </p>
        </div>
        {config?.enabled && (
          <span className="badge badge-success ml-auto flex items-center gap-1">
            <CheckCircle2 size={14} /> Actif
          </span>
        )}
      </div>

      {/* Statistiques */}
      <div className="mb-6 grid grid-cols-3 gap-4">
        {[
          { label: 'Messages reçus', value: stats?.inbound ?? 0, icon: Inbox },
          { label: 'Messages envoyés', value: stats?.outbound ?? 0, icon: Send },
          { label: 'Livrés / lus', value: stats?.deliveredOrRead ?? 0, icon: CheckCircle2 },
        ].map(({ label, value, icon: Icon }) => (
          <div key={label} className="glass-card flex items-center gap-3 p-4">
            <Icon size={20} className="text-primary" />
            <div>
              <p className="text-sm text-muted">{label}</p>
              <p className="text-xl font-semibold">{value}</p>
            </div>
          </div>
        ))}
      </div>

      {/* Configuration */}
      <div className="glass-card mb-6 p-5">
        <h2 className="mb-4 font-semibold">Configuration Cloud API</h2>
        {!config?.configured && (
          <p className="mb-3 rounded-lg bg-blue-500/10 px-3 py-2 text-sm text-blue-400">
            Webhook Meta à configurer : <code>POST /api/v1/public/whatsapp/webhook</code>
          </p>
        )}
        <div className="grid gap-4 md:grid-cols-2">
          <div>
            <label className="mb-1 block text-sm">Phone Number ID</label>
            <input
              className="input"
              value={form.phoneNumberId ?? ''}
              onChange={(e) => setForm({ ...form, phoneNumberId: e.target.value })}
              placeholder="1234567890"
            />
          </div>
          <div>
            <label className="mb-1 block text-sm">Numéro affiché</label>
            <input
              className="input"
              value={form.displayPhoneNumber ?? ''}
              onChange={(e) => setForm({ ...form, displayPhoneNumber: e.target.value })}
              placeholder="+225 07 00 00 00 00"
            />
          </div>
          <div>
            <label className="mb-1 block text-sm">
              Token d’accès {config?.hasToken && <span className="text-xs text-green-500">(enregistré, chiffré)</span>}
            </label>
            <input
              type="password"
              className="input"
              value={form.accessToken ?? ''}
              onChange={(e) => setForm({ ...form, accessToken: e.target.value })}
              placeholder="EAAG..."
            />
          </div>
          <div>
            <label className="mb-1 block text-sm">Webhook verify token</label>
            <input
              className="input"
              value={form.webhookVerifyToken ?? ''}
              onChange={(e) => setForm({ ...form, webhookVerifyToken: e.target.value })}
              placeholder="mon-token-secret"
            />
          </div>
          <div className="md:col-span-2">
            <label className="mb-1 block text-sm">Message de bienvenue (réponse à #rejoindre)</label>
            <textarea
              className="input min-h-20"
              value={form.welcomeMessage ?? ''}
              onChange={(e) => setForm({ ...form, welcomeMessage: e.target.value })}
            />
          </div>
        </div>
        <div className="mt-4 flex items-center gap-3">
          <label className="flex items-center gap-2 text-sm">
            <input
              type="checkbox"
              checked={!!form.enabled}
              onChange={(e) => setForm({ ...form, enabled: e.target.checked })}
            />
            Activer le pont WhatsApp
          </label>
          <button
            className="btn-primary btn-sm ml-auto"
            disabled={saveMutation.isPending}
            onClick={() => saveMutation.mutate()}
          >
            {saveMutation.isPending ? <RefreshCw size={14} className="animate-spin" /> : 'Enregistrer'}
          </button>
          <button
            className="btn-sm btn-outline"
            disabled={testMutation.isPending}
            onClick={() => testMutation.mutate()}
          >
            Tester la connexion
          </button>
        </div>
      </div>

      {/* Diffusion */}
      <div className="glass-card mb-6 p-5">
        <h2 className="mb-4 flex items-center gap-2 font-semibold">
          <Radio size={18} /> Diffuser une annonce
        </h2>
        <div className="space-y-3">
          <input
            className="input"
            placeholder="Titre de l'annonce"
            value={broadcast.titre}
            onChange={(e) => setBroadcast({ ...broadcast, titre: e.target.value })}
          />
          <textarea
            className="input min-h-24"
            placeholder="Contenu du message…"
            value={broadcast.contenu}
            onChange={(e) => setBroadcast({ ...broadcast, contenu: e.target.value })}
          />
          <button
            className="btn-primary btn-sm"
            disabled={broadcastMutation.isPending || !broadcast.titre || !broadcast.contenu}
            onClick={() => broadcastMutation.mutate()}
          >
            <Send size={14} /> Envoyer aux membres opt-in
          </button>
        </div>
      </div>

      {/* Journal */}
      <div className="glass-card p-5">
        <h2 className="mb-4 font-semibold">Journal des messages</h2>
        {!messages?.length ? (
          <p className="py-8 text-center text-sm text-muted">
            Aucun message. Les échanges apparaîtront ici dès que le webhook sera actif.
          </p>
        ) : (
          <div className="divide-y divide-border">
            {messages.map((m) => (
              <div key={m.id} className="flex items-start justify-between gap-4 py-3">
                <div className="min-w-0">
                  <p className="flex items-center gap-2 text-sm font-medium">
                    {m.direction === 'INBOUND' ? (
                      <XCircle size={14} className="text-blue-400" />
                    ) : (
                      <Send size={14} className="text-green-400" />
                    )}
                    {m.phoneNumber}
                    <span className="text-xs font-normal text-muted">
                      {new Date(m.createdAt).toLocaleString()}
                    </span>
                  </p>
                  <p className="mt-1 line-clamp-2 text-sm text-muted">{m.body}</p>
                </div>
                <div className="flex shrink-0 items-center gap-2">
                  <span className="text-xs text-muted">{m.kind}</span>
                  <span className={statusBadge(m.status)}>{m.status}</span>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
}
