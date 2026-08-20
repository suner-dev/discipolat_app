import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import toast from 'react-hot-toast';
import api from '@/lib/api';
import {
  Globe, Mail, Cloud, Key, Link2, Webhook, CheckCircle2, AlertTriangle,
  Loader2, Save, Settings, Shield, Database, Send, TestTube, RefreshCw,
  Server, ExternalLink, Lock, Unlock, Eye, EyeOff, Copy, Plus, Trash2,
} from 'lucide-react';

/* ============================================================================
 * ADMIN INTEGRATIONS PAGE — Gestion des intégrations externes
 * SMTP, stockage, clés API, webhooks, services tiers.
 * L'admin configure ces paramètres sans toucher au code.
 * ========================================================================== */

interface IntegrationConfig {
  smtp: {
    host: string; port: number; username: string; password: string;
    fromAddress: string; fromName: string; tls: boolean; enabled: boolean;
  };
  storage: {
    provider: string; bucket: string; region: string;
    accessKey: string; secretKey: string; endpoint: string; enabled: boolean;
  };
  jwt: {
    accessTokenTtlMinutes: number; refreshTokenTtlDays: number;
    algorithm: string; enabled: boolean;
  };
  rateLimiting: {
    enabled: boolean; requestsPerMinute: number;
    burstSize: number; blockDurationMinutes: number;
  };
}

const INTEGRATIONS = [
  {
    key: 'smtp',
    icon: Mail,
    title: 'Email (SMTP)',
    description: 'Configuration du serveur de messagerie pour les emails transactionnels',
    gradient: 'from-blue-500 to-indigo-600',
    fields: [
      { key: 'host', label: 'Serveur SMTP', placeholder: 'smtp.example.com', type: 'text' },
      { key: 'port', label: 'Port', placeholder: '587', type: 'number' },
      { key: 'username', label: 'Nom d\'utilisateur', placeholder: 'user@example.com', type: 'text' },
      { key: 'password', label: 'Mot de passe', placeholder: '••••••••', type: 'password' },
      { key: 'fromAddress', label: 'Adresse d\'envoi', placeholder: 'noreply@eglise.com', type: 'email' },
      { key: 'fromName', label: 'Nom d\'expéditeur', placeholder: 'Discipolat', type: 'text' },
      { key: 'tls', label: 'TLS/SSL', type: 'toggle' },
    ],
  },
  {
    key: 'storage',
    icon: Cloud,
    title: 'Stockage (S3 / MinIO)',
    description: 'Stockage cloud pour les fichiers, images et documents',
    gradient: 'from-emerald-500 to-teal-600',
    fields: [
      { key: 'provider', label: 'Fournisseur', type: 'select', options: ['MINIO', 'AWS_S3', 'DO_SPACES', 'CUSTOM'] },
      { key: 'bucket', label: 'Bucket', placeholder: 'discipolat-files', type: 'text' },
      { key: 'region', label: 'Région', placeholder: 'eu-west-1', type: 'text' },
      { key: 'endpoint', label: 'Endpoint (optionnel)', placeholder: 'https://minio.example.com', type: 'text' },
      { key: 'accessKey', label: 'Access Key', placeholder: 'AKIA...', type: 'text' },
      { key: 'secretKey', label: 'Secret Key', placeholder: '••••••••', type: 'password' },
    ],
  },
  {
    key: 'jwt',
    icon: Key,
    title: 'Sécurité JWT',
    description: 'Durées de vie des tokens et algorithme de signature',
    gradient: 'from-violet-500 to-purple-600',
    fields: [
      { key: 'accessTokenTtlMinutes', label: 'Durée access token (minutes)', type: 'number', placeholder: '15' },
      { key: 'refreshTokenTtlDays', label: 'Durée refresh token (jours)', type: 'number', placeholder: '7' },
      { key: 'algorithm', label: 'Algorithme', type: 'select', options: ['RS256', 'HS256', 'ES256'] },
    ],
  },
  {
    key: 'rateLimiting',
    icon: Shield,
    title: 'Limitation de débit',
    description: 'Protection contre les abus et attaques par force brute',
    gradient: 'from-amber-500 to-orange-600',
    fields: [
      { key: 'requestsPerMinute', label: 'Requêtes par minute', type: 'number', placeholder: '60' },
      { key: 'burstSize', label: 'Taille du burst', type: 'number', placeholder: '10' },
      { key: 'blockDurationMinutes', label: 'Durée de blocage (min)', type: 'number', placeholder: '30' },
    ],
  },
];

function IntegrationSection({ integration, config, onConfigChange, saving, onTest }: {
  integration: typeof INTEGRATIONS[0];
  config: Record<string, unknown>;
  onConfigChange: (key: string, value: unknown) => void;
  saving: boolean;
  onTest?: () => void;
}) {
  const [showPasswords, setShowPasswords] = useState<Record<string, boolean>>({});

  return (
    <section className="glass-card overflow-hidden animate-slide-up">
      <div className="p-5">
        <div className="flex items-start gap-3 mb-4">
          <div className={`w-10 h-10 rounded-xl bg-gradient-to-br ${integration.gradient} text-white flex items-center justify-center shadow-md flex-shrink-0`}>
            <integration.icon className="w-5 h-5" />
          </div>
          <div className="flex-1">
            <h3 className="text-sm font-bold text-gray-900 dark:text-gray-100">{integration.title}</h3>
            <p className="text-xs text-gray-500 dark:text-gray-400">{integration.description}</p>
          </div>
          {onTest && (
            <button onClick={onTest} className="btn-ghost btn-sm" title="Tester la connexion">
              <TestTube className="w-4 h-4" /> Tester
            </button>
          )}
        </div>
        <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
          {integration.fields.map((field) => (
            <div key={field.key} className={field.type === 'toggle' ? 'sm:col-span-2' : ''}>
              {field.type === 'toggle' ? (
                <div className="flex items-center justify-between p-3 rounded-xl border border-gray-200/60 dark:border-gray-700/60 bg-white/40 dark:bg-gray-900/30">
                  <span className="text-sm font-medium text-gray-700 dark:text-gray-300">{field.label}</span>
                  <button
                    role="switch"
                    aria-checked={!!config[field.key]}
                    onClick={() => onConfigChange(field.key, !config[field.key])}
                    className={`relative w-11 h-6 rounded-full transition-colors ${config[field.key] ? 'bg-primary-600' : 'bg-gray-300 dark:bg-gray-600'}`}
                  >
                    <span className={`absolute top-0.5 left-0.5 w-5 h-5 rounded-full bg-white shadow transition-transform ${config[field.key] ? 'translate-x-5' : ''}`} />
                  </button>
                </div>
              ) : (
                <div>
                  <label className="label">{field.label}</label>
                  <div className="relative">
                    {field.type === 'password' ? (
                      <input
                        className="input pr-10"
                        type={showPasswords[field.key] ? 'text' : 'password'}
                        value={String(config[field.key] || '')}
                        onChange={(e) => onConfigChange(field.key, e.target.value)}
                        placeholder={field.placeholder}
                      />
                    ) : field.type === 'select' ? (
                      <select
                        className="input"
                        value={String(config[field.key] || '')}
                        onChange={(e) => onConfigChange(field.key, e.target.value)}
                      >
                        {(field as { options?: string[] }).options?.map((o) => (
                          <option key={o} value={o}>{o}</option>
                        ))}
                      </select>
                    ) : (
                      <input
                        className="input"
                        type={field.type}
                        value={String(config[field.key] || '')}
                        onChange={(e) => onConfigChange(field.key, field.type === 'number' ? Number(e.target.value) : e.target.value)}
                        placeholder={field.placeholder}
                      />
                    )}
                    {field.type === 'password' && (
                      <button
                        className="absolute right-2 top-1/2 -translate-y-1/2 p-1 rounded text-gray-400 hover:text-gray-600"
                        onClick={() => setShowPasswords((p) => ({ ...p, [field.key]: !p[field.key] }))}
                      >
                        {showPasswords[field.key] ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
                      </button>
                    )}
                  </div>
                </div>
              )}
            </div>
          ))}
        </div>
      </div>
    </section>
  );
}

export default function AdminIntegrationsPage() {
  const queryClient = useQueryClient();
  const [configs, setConfigs] = useState<Record<string, Record<string, unknown>>>({});

  const { data: serverConfigs, isLoading } = useQuery({
    queryKey: ['admin', 'integrations'],
    queryFn: async () => {
      // Fetch whatever config endpoints exist on the backend
      const results: Record<string, Record<string, unknown>> = {};
      try {
        const [smtpRes, storageRes, jwtRes, rateRes] = await Promise.allSettled([
          api.get('/admin/integrations/smtp'),
          api.get('/admin/integrations/storage'),
          api.get('/admin/integrations/jwt'),
          api.get('/admin/integrations/rate-limiting'),
        ]);
        if (smtpRes.status === 'fulfilled') results.smtp = smtpRes.value.data;
        if (storageRes.status === 'fulfilled') results.storage = storageRes.value.data;
        if (jwtRes.status === 'fulfilled') results.jwt = jwtRes.value.data;
        if (rateRes.status === 'fulfilled') results.rateLimiting = rateRes.value.data;
      } catch {}
      return results;
    },
  });

  const saveMutation = useMutation({
    mutationFn: async ({ key, data }: { key: string; data: Record<string, unknown> }) => {
      await api.put(`/admin/integrations/${key}`, data);
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin', 'integrations'] });
      toast.success('Configuration enregistrée');
    },
    onError: () => toast.error('Erreur lors de l\'enregistrement'),
  });

  const testMutation = useMutation({
    mutationFn: async (key: string) => {
      const res = await api.post(`/admin/integrations/${key}/test`);
      return res.data;
    },
    onSuccess: (data: { success: boolean; message?: string }) => {
      if (data.success) {
        toast.success(data.message || 'Connexion réussie');
      } else {
        toast.error(data.message || 'Échec de la connexion');
      }
    },
    onError: () => toast.error('Erreur lors du test'),
  });

  const getConfig = (key: string) => configs[key] || serverConfigs?.[key] || {};

  const updateConfig = (intKey: string, fieldKey: string, value: unknown) => {
    setConfigs((prev) => ({
      ...prev,
      [intKey]: { ...(prev[intKey] || serverConfigs?.[intKey] || {}), [fieldKey]: value },
    }));
  };

  if (isLoading) {
    return (
      <div className="min-h-[50vh] flex items-center justify-center">
        <div className="spinner h-8 w-8" />
      </div>
    );
  }

  return (
    <div className="page-container max-w-5xl">
      <div className="page-header">
        <div className="flex items-center gap-3">
          <div className="p-3 rounded-xl bg-gradient-to-br from-blue-500 to-indigo-600 text-white shadow-lg">
            <Globe className="w-6 h-6" />
          </div>
          <div>
            <h1 className="page-title">Intégrations</h1>
            <p className="page-subtitle">
              Configurez les services externes : email, stockage, sécurité et protection — sans écrire de code.
            </p>
          </div>
        </div>
        <div className="page-header-actions">
          <button
            className="btn-primary btn-sm"
            onClick={() => {
              Object.entries(configs).forEach(([key, data]) => {
                saveMutation.mutate({ key, data });
              });
              if (Object.keys(configs).length === 0) {
                toast.success('Aucune modification à enregistrer');
              }
            }}
            disabled={saveMutation.isPending || Object.keys(configs).length === 0}
          >
            {saveMutation.isPending ? <Loader2 className="w-4 h-4 animate-spin" /> : <Save className="w-4 h-4" />}
            Enregistrer tout
          </button>
        </div>
      </div>

      <div className="space-y-6">
        {INTEGRATIONS.map((int) => (
          <IntegrationSection
            key={int.key}
            integration={int}
            config={getConfig(int.key)}
            onConfigChange={(field, value) => updateConfig(int.key, field, value)}
            saving={saveMutation.isPending}
            onTest={() => testMutation.mutate(int.key)}
          />
        ))}
      </div>

      {/* Info footer */}
      <div className="mt-6 glass-card p-4 text-center">
        <p className="text-xs text-gray-400 dark:text-gray-500">
          Les paramètres sont stockés de manière sécurisée côté serveur. Les mots de passe sont chiffrés.
          Utilisez le bouton "Tester" pour valider chaque connexion avant d'enregistrer.
        </p>
      </div>
    </div>
  );
}
