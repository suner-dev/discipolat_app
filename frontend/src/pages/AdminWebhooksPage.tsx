import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import api, { getErrorMessage } from '@/lib/api';
import { Loader2, Webhook, Plus, Trash2, Send, KeyRound, CheckCircle2, XCircle } from 'lucide-react';
import toast from 'react-hot-toast';

interface Webhook {
  id: string;
  name: string;
  url: string;
  secret: string;
  events: string;
  active: boolean;
}

interface DeliveryLog {
  id: string;
  webhookId: string;
  eventType: string;
  responseCode: number | null;
  success: boolean;
  errorMessage: string | null;
  createdAt: string;
}

interface ApiKeyView {
  id: string;
  name: string;
  prefix: string;
  scopes: string;
  active: boolean;
}

/** Connecteur Écosystème — webhooks signés HMAC + clés API publiques. */
export default function AdminWebhooksPage() {
  const queryClient = useQueryClient();
  const [showCreate, setShowCreate] = useState(false);
  const [form, setForm] = useState({ name: '', url: '', events: '*' });
  const [newKeyName, setNewKeyName] = useState('');
  const [revealedKey, setRevealedKey] = useState<string | null>(null);

  const invalidate = () => {
    queryClient.invalidateQueries({ queryKey: ['admin-webhooks'] });
    queryClient.invalidateQueries({ queryKey: ['webhook-logs'] });
    queryClient.invalidateQueries({ queryKey: ['api-keys'] });
  };

  const listQuery = useQuery({
    queryKey: ['admin-webhooks'],
    queryFn: async () => (await api.get<Webhook[]>('/admin/webhooks')).data,
  });
  const logsQuery = useQuery({
    queryKey: ['webhook-logs'],
    queryFn: async () => (await api.get<DeliveryLog[]>('/admin/webhooks/logs')).data,
  });
  const keysQuery = useQuery({
    queryKey: ['api-keys'],
    queryFn: async () => (await api.get<ApiKeyView[]>('/admin/webhooks/api-keys')).data,
  });

  const createMutation = useMutation({
    mutationFn: async () =>
      (await api.post<Webhook>('/admin/webhooks', { ...form, secret: '' })).data,
    onSuccess: (w) => {
      toast.success(`Webhook créé — secret : ${w.secret.slice(0, 8)}…`);
      setShowCreate(false);
      setForm({ name: '', url: '', events: '*' });
      invalidate();
    },
    onError: (e) => toast.error(getErrorMessage(e)),
  });

  const testMutation = useMutation({
    mutationFn: async (id: string) => (await api.post<DeliveryLog>(`/admin/webhooks/${id}/test`)).data,
    onSuccess: (log) => {
      if (log.success) toast.success(`Livré (HTTP ${log.responseCode})`);
      else toast.error(`Échec de livraison${log.errorMessage ? ` : ${log.errorMessage}` : ''}`);
      invalidate();
    },
  });

  const deleteMutation = useMutation({
    mutationFn: async (id: string) => {
      await api.delete(`/admin/webhooks/${id}`);
    },
    onSuccess: () => {
      toast.success('Webhook supprimé');
      invalidate();
    },
    onError: (e) => toast.error(getErrorMessage(e)),
  });

  const createKeyMutation = useMutation({
    mutationFn: async () =>
      (
        await api.post<{ key: string }>('/admin/webhooks/api-keys', {
          name: newKeyName || 'Intégration',
          scopes: 'read',
        })
      ).data,
    onSuccess: (r) => {
      setRevealedKey(r.key);
      setNewKeyName('');
      invalidate();
    },
    onError: (e) => toast.error(getErrorMessage(e)),
  });

  const revokeKeyMutation = useMutation({
    mutationFn: async (id: string) => {
      await api.delete(`/admin/webhooks/api-keys/${id}`);
    },
    onSuccess: () => {
      toast.success('Clé révoquée');
      invalidate();
    },
  });

  return (
    <div className="page-container">
      <div className="page-header">
        <div className="p-3 rounded-xl bg-gradient-to-br from-violet-500 to-purple-600 text-white shadow-lg">
          <Webhook className="w-6 h-6" />
        </div>
        <div>
          <h1 className="page-title">Connecteur Écosystème</h1>
          <p className="page-subtitle">Webhooks signés HMAC-SHA256 & clés API pour intégrations externes</p>
        </div>
        <button onClick={() => setShowCreate(!showCreate)} className="btn-primary btn-sm ml-auto flex items-center gap-2">
          <Plus className="w-4 h-4" /> Nouveau webhook
        </button>
      </div>

      {showCreate && (
        <form
          onSubmit={(e) => {
            e.preventDefault();
            createMutation.mutate();
          }}
          className="glass-card p-5 mb-6 grid gap-3 md:grid-cols-4 animate-slide-up"
        >
          <input required placeholder="Nom" className="input"
            value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} />
          <input required type="url" placeholder="https://exemple.com/hook" className="input md:col-span-2"
            value={form.url} onChange={(e) => setForm({ ...form, url: e.target.value })} />
          <input placeholder="Événements (* ou SOUL_CREATED, …)" className="input"
            value={form.events} onChange={(e) => setForm({ ...form, events: e.target.value })} />
          <button type="submit" disabled={createMutation.isPending} className="btn-primary btn-sm md:col-span-4">
            {createMutation.isPending && <Loader2 className="inline w-4 h-4 animate-spin mr-1" />} Créer le webhook
          </button>
        </form>
      )}

      {/* Liste webhooks */}
      <div className="glass-card divide-y divide-gray-100 dark:divide-gray-800 mb-6">
        {(listQuery.data ?? []).map((w) => (
          <div key={w.id} className="flex flex-col md:flex-row md:items-center justify-between gap-2 px-5 py-4">
            <div className="min-w-0">
              <p className="font-medium text-gray-900 dark:text-gray-100">
                {w.name}
                <span className={`badge ${w.active ? 'badge-success' : 'badge-danger'} ml-2`}>
                  {w.active ? 'Actif' : 'Inactif'}
                </span>
              </p>
              <p className="text-xs text-gray-500 dark:text-gray-400 truncate">{w.url}</p>
              <p className="text-xs text-gray-400 dark:text-gray-500">Événements : {w.events}</p>
            </div>
            <div className="flex items-center gap-2 shrink-0">
              <button onClick={() => testMutation.mutate(w.id)}
                disabled={testMutation.isPending}
                className="btn-primary btn-sm flex items-center gap-1">
                <Send className="w-3.5 h-3.5" /> Tester
              </button>
              <button onClick={() => deleteMutation.mutate(w.id)}
                className="p-2 rounded-lg text-red-500 hover:bg-red-50 dark:hover:bg-red-900/20">
                <Trash2 className="w-4 h-4" />
              </button>
            </div>
          </div>
        ))}
        {!listQuery.isLoading && (listQuery.data ?? []).length === 0 && (
          <p className="text-center text-sm text-gray-500 py-8">
            Aucun webhook configuré — connectez Zapier, Make ou votre CRM.
          </p>
        )}
      </div>

      {/* Clés API */}
      <div className="glass-card p-6 mb-6">
        <h2 className="font-semibold text-gray-900 dark:text-gray-100 mb-4 flex items-center gap-2">
          <KeyRound className="w-5 h-5 text-primary-500" /> Clés API publiques
        </h2>
        <div className="flex gap-2 mb-4">
          <input placeholder="Nom de l'intégration" className="input flex-1"
            value={newKeyName} onChange={(e) => setNewKeyName(e.target.value)} />
          <button onClick={() => createKeyMutation.mutate()}
            disabled={createKeyMutation.isPending}
            className="btn-primary btn-sm whitespace-nowrap">
            {createKeyMutation.isPending ? <Loader2 className="w-4 h-4 animate-spin" /> : 'Générer une clé'}
          </button>
        </div>

        {revealedKey && (
          <div className="mb-4 p-4 rounded-xl bg-amber-50 dark:bg-amber-900/20 border border-amber-200 dark:border-amber-800">
            <p className="text-xs font-semibold text-amber-700 dark:text-amber-400 mb-2">
              ⚠️ Copiez cette clé maintenant — elle ne sera plus jamais affichée :
            </p>
            <code className="text-sm font-mono break-all text-gray-900 dark:text-gray-100">{revealedKey}</code>
            <button
              onClick={() => {
                navigator.clipboard?.writeText(revealedKey);
                toast.success('Clé copiée');
              }}
              className="ml-3 text-xs underline text-amber-700 dark:text-amber-400"
            >
              Copier
            </button>
          </div>
        )}

        <div className="divide-y divide-gray-100 dark:divide-gray-800">
          {(keysQuery.data ?? []).map((k) => (
            <div key={k.id} className="flex items-center justify-between py-3">
              <div>
                <p className="font-medium text-gray-900 dark:text-gray-100">{k.name}</p>
                <p className="text-xs font-mono text-gray-500 dark:text-gray-400">{k.prefix}… · scope {k.scopes}</p>
              </div>
              <div className="flex items-center gap-2">
                {k.active ? (
                  <span className="badge badge-success">Active</span>
                ) : (
                  <span className="badge badge-danger">Révoquée</span>
                )}
                {k.active && (
                  <button onClick={() => revokeKeyMutation.mutate(k.id)}
                    className="text-xs text-red-500 hover:underline">
                    Révoquer
                  </button>
                )}
              </div>
            </div>
          ))}
          {!keysQuery.isLoading && (keysQuery.data ?? []).length === 0 && (
            <p className="text-center text-sm text-gray-500 py-6">Aucune clé API générée.</p>
          )}
        </div>
      </div>

      {/* Journal de livraison */}
      <div className="glass-card divide-y divide-gray-100 dark:divide-gray-800">
        <div className="px-5 py-4">
          <h2 className="font-semibold text-gray-900 dark:text-gray-100">Journal de livraison</h2>
        </div>
        {(logsQuery.data ?? []).slice(0, 15).map((l) => (
          <div key={l.id} className="flex items-center justify-between px-5 py-3">
            <div>
              <p className="text-sm font-mono text-gray-700 dark:text-gray-300">{l.eventType}</p>
              <p className="text-xs text-gray-500 dark:text-gray-400">
                {l.errorMessage ?? `HTTP ${l.responseCode}`} ·{' '}
                {new Date(l.createdAt).toLocaleString('fr-FR')}
              </p>
            </div>
            {l.success ? (
              <CheckCircle2 className="w-5 h-5 text-green-500" />
            ) : (
              <XCircle className="w-5 h-5 text-red-500" />
            )}
          </div>
        ))}
        {!logsQuery.isLoading && (logsQuery.data ?? []).length === 0 && (
          <p className="text-center text-sm text-gray-500 py-8">Aucun appel sortant enregistré.</p>
        )}
      </div>
    </div>
  );
}
