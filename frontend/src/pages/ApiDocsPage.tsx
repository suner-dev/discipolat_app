import React, { useState, useEffect } from 'react';
import { useI18n } from '@/i18n';
import api from '@/lib/api';
import { Code, ExternalLink, Key, Copy, CheckCircle2, Shield, Globe, Zap } from 'lucide-react';
import Toast from '@/components/shared/Toast';

interface ApiKey {
  id: string;
  nom: string;
  cle: string;
  dateCreation: string;
  derniereUtilisation?: string;
  actif: boolean;
  permissions: string[];
}

interface EndpointInfo {
  method: string;
  path: string;
  description: string;
  permission: string;
  module: string;
}

const METHOD_COLORS: Record<string, string> = {
  GET: 'bg-green-100 text-green-700',
  POST: 'bg-blue-100 text-blue-700',
  PUT: 'bg-amber-100 text-amber-700',
  PATCH: 'bg-orange-100 text-orange-700',
  DELETE: 'bg-red-100 text-red-700',
};

const MODULES = [
  { key: 'auth', label: 'Authentification', icon: '🔐' },
  { key: 'souls', label: 'Âmes', icon: '👤' },
  { key: 'families', label: 'Familles', icon: '👨‍👩‍👧‍👦' },
  { key: 'departments', label: 'Départements', icon: '🏢' },
  { key: 'reports', label: 'Rapports', icon: '📋' },
  { key: 'events', label: 'Événements', icon: '📅' },
  { key: 'prayers', label: 'Prières', icon: '🙏' },
  { key: 'finances', label: 'Finances', icon: '💰' },
  { key: 'ai', label: 'IA Pastorale', icon: '🤖' },
  { key: 'notifications', label: 'Notifications', icon: '🔔' },
  { key: 'platform', label: 'Configuration', icon: '⚙️' },
  { key: 'audit', label: 'Audit', icon: '📝' },
];

const SAMPLE_ENDPOINTS: EndpointInfo[] = [
  { method: 'GET', path: '/api/v1/souls', description: 'Liste des âmes avec pagination', permission: 'Tous authentifiés', module: 'souls' },
  { method: 'POST', path: '/api/v1/souls', description: 'Créer une nouvelle âme', permission: 'ADMIN, PASTEUR', module: 'souls' },
  { method: 'GET', path: '/api/v1/souls/{id}', description: 'Détail d\'une âme', permission: 'Tous authentifiés', module: 'souls' },
  { method: 'PUT', path: '/api/v1/souls/{id}', description: 'Modifier une âme', permission: 'ADMIN, PASTEUR', module: 'souls' },
  { method: 'GET', path: '/api/v1/dashboard/kpi', description: 'KPIs du tableau de bord', permission: 'Tous authentifiés', module: 'dashboard' },
  { method: 'GET', path: '/api/v1/events', description: 'Liste des événements', permission: 'Tous authentifiés', module: 'events' },
  { method: 'POST', path: '/api/v1/auth/login', description: 'Connexion (public)', permission: 'Public', module: 'auth' },
  { method: 'GET', path: '/api/v1/ai/analyze/{soulId}', description: 'Analyse spirituelle IA', permission: 'ADMIN, PASTEUR', module: 'ai' },
  { method: 'POST', path: '/api/v1/payments/initiate', description: 'Initier un paiement Mobile Money', permission: 'Tous authentifiés', module: 'payments' },
  { method: 'GET', path: '/api/v1/audit', description: 'Journal d\'audit', permission: 'ADMIN, PASTEUR', module: 'audit' },
];

export default function ApiDocsPage() {
  const { t } = useI18n();
  const [apiKeys, setApiKeys] = useState<ApiKey[]>([]);
  const [loading, setLoading] = useState(true);
  const [filterModule, setFilterModule] = useState('');
  const [showCreateKey, setShowCreateKey] = useState(false);
  const [newKeyName, setNewKeyName] = useState('');
  const [copiedKey, setCopiedKey] = useState<string | null>(null);

  useEffect(() => { loadApiKeys(); }, []);

  const loadApiKeys = async () => {
    try {
      setLoading(true);
      const res = await api.get('/api-keys');
      setApiKeys(res.data.content || res.data || []);
    } catch {
      setApiKeys([]);
    } finally {
      setLoading(false);
    }
  };

  const createApiKey = async () => {
    if (!newKeyName.trim()) {
      Toast.warning('Veuillez entrer un nom');
      return;
    }
    try {
      const res = await api.post('/api-keys', { nom: newKeyName });
      Toast.success('Clé API créée ! Copiez-la maintenant.');
      setApiKeys([res.data, ...apiKeys]);
      setShowCreateKey(false);
      setNewKeyName('');
    } catch {
      Toast.error('Erreur lors de la création');
    }
  };

  const revokeApiKey = async (id: string) => {
    if (!confirm('Révoquer cette clé API ? Cette action est irréversible.')) return;
    try {
      await api.delete(`/api-keys/${id}`);
      Toast.success('Clé révoquée');
      setApiKeys(apiKeys.filter(k => k.id !== id));
    } catch {
      Toast.error('Erreur lors de la révocation');
    }
  };

  const copyKey = (key: string) => {
    navigator.clipboard.writeText(key);
    setCopiedKey(key);
    Toast.success('Clé copiée !');
    setTimeout(() => setCopiedKey(null), 2000);
  };

  const filteredEndpoints = SAMPLE_ENDPOINTS.filter(e =>
    !filterModule || e.module === filterModule
  );

  return (
    <div className="min-h-screen p-4 md:p-8 max-w-6xl mx-auto">
      <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4 mb-8">
        <div>
          <h1 className="text-2xl md:text-3xl font-bold text-gray-900 dark:text-white flex items-center gap-3">
            <Code className="w-8 h-8 text-cyan-500" />
            API & Documentation
          </h1>
          <p className="text-gray-500 dark:text-gray-400 mt-1">
            API publique, clés d'accès et documentation développeur
          </p>
        </div>
        <div className="flex gap-2">
          <a
            href="/swagger-ui.html"
            target="_blank"
            rel="noopener noreferrer"
            className="px-4 py-2 rounded-xl border border-gray-200 dark:border-white/10 text-gray-700 dark:text-gray-300 text-sm font-medium hover:bg-gray-50 dark:hover:bg-white/5 transition-all flex items-center gap-2"
          >
            <ExternalLink className="w-4 h-4" />
            Swagger UI
          </a>
          <button
            onClick={() => setShowCreateKey(true)}
            className="px-4 py-2 rounded-xl bg-gradient-to-r from-cyan-600 to-blue-600 text-white text-sm font-medium hover:from-cyan-700 hover:to-blue-700 transition-all shadow-lg shadow-cyan-500/25 flex items-center gap-2"
          >
            <Key className="w-4 h-4" />
            Nouvelle clé API
          </button>
        </div>
      </div>

      {/* Quick Start */}
      <div className="p-6 rounded-2xl border border-gray-200 dark:border-white/10 bg-white dark:bg-white/5 mb-6">
        <h2 className="text-lg font-bold text-gray-900 dark:text-white mb-3">🚀 Démarrage rapide</h2>
        <div className="space-y-3">
          <div className="p-3 rounded-xl bg-gray-50 dark:bg-white/2 font-mono text-sm text-gray-700 dark:text-gray-300 overflow-x-auto">
            <span className="text-gray-400"># Authentification</span>{'\n'}
            <span className="text-green-600">curl</span> -X POST https://api.discipolat.com/api/v1/auth/login \{'\n'}
            {'  '}-H "Content-Type: application/json" \{'\n'}
            {'  }'}-d '{'{'}"email": "votre@email.com", "password": "..."{'}'}'
          </div>
          <div className="p-3 rounded-xl bg-gray-50 dark:bg-white/2 font-mono text-sm text-gray-700 dark:text-gray-300 overflow-x-auto">
            <span className="text-gray-400"># Requête authentifiée</span>{'\n'}
            <span className="text-green-600">curl</span> https://api.discipolat.com/api/v1/souls \{'\n'}
            {'  '}-H "Authorization: Bearer VOTRE_TOKEN"
          </div>
          <div className="p-3 rounded-xl bg-gray-50 dark:bg-white/2 font-mono text-sm text-gray-700 dark:text-gray-300 overflow-x-auto">
            <span className="text-gray-400"># Avec clé API</span>{'\n'}
            <span className="text-green-600">curl</span> https://api.discipolat.com/api/v1/souls \{'\n'}
            {'  '}-H "X-API-Key: dk_votre_cle_api"
          </div>
        </div>
      </div>

      {/* API Endpoints */}
      <div className="mb-6">
        <h2 className="text-lg font-bold text-gray-900 dark:text-white mb-4">📚 Endpoints principaux</h2>
        <div className="flex gap-2 mb-4 flex-wrap">
          <button
            onClick={() => setFilterModule('')}
            className={`px-3 py-1.5 rounded-lg text-xs font-medium transition-all ${
              !filterModule ? 'bg-cyan-600 text-white' : 'bg-gray-100 dark:bg-white/5 text-gray-600 dark:text-gray-400 hover:bg-gray-200'
            }`}
          >
            Tous
          </button>
          {MODULES.map(m => (
            <button
              key={m.key}
              onClick={() => setFilterModule(m.key)}
              className={`px-3 py-1.5 rounded-lg text-xs font-medium transition-all ${
                filterModule === m.key ? 'bg-cyan-600 text-white' : 'bg-gray-100 dark:bg-white/5 text-gray-600 dark:text-gray-400 hover:bg-gray-200'
              }`}
            >
              {m.icon} {m.label}
            </button>
          ))}
        </div>
        <div className="space-y-2">
          {filteredEndpoints.map((ep, i) => (
            <div key={i} className="flex items-center gap-3 p-3 rounded-xl border border-gray-200 dark:border-white/10 bg-white dark:bg-white/5 hover:bg-gray-50 dark:hover:bg-white/2 transition-colors">
              <span className={`px-2 py-0.5 rounded text-xs font-bold font-mono ${METHOD_COLORS[ep.method]}`}>
                {ep.method}
              </span>
              <code className="text-sm text-gray-700 dark:text-gray-300 font-mono flex-1">{ep.path}</code>
              <span className="text-xs text-gray-500 hidden md:block">{ep.description}</span>
              <span className="px-2 py-0.5 rounded-full text-xs bg-gray-100 dark:bg-white/10 text-gray-600 dark:text-gray-400 hidden lg:block">
                {ep.permission}
              </span>
            </div>
          ))}
        </div>
      </div>

      {/* API Keys */}
      <div className="mb-6">
        <h2 className="text-lg font-bold text-gray-900 dark:text-white mb-4">🔑 Clés API</h2>
        {apiKeys.length === 0 ? (
          <div className="p-6 rounded-2xl border border-dashed border-gray-300 dark:border-white/10 text-center">
            <Key className="w-8 h-8 text-gray-400 mx-auto mb-3" />
            <p className="text-sm text-gray-500 dark:text-gray-400">
              Aucune clé API. Créez-en une pour accéder à l'API programmatiquement.
            </p>
          </div>
        ) : (
          <div className="space-y-3">
            {apiKeys.map(key => (
              <div key={key.id} className="flex items-center justify-between p-4 rounded-xl border border-gray-200 dark:border-white/10 bg-white dark:bg-white/5">
                <div className="flex-1 min-w-0">
                  <div className="text-sm font-medium text-gray-900 dark:text-white">{key.nom}</div>
                  <div className="flex items-center gap-2 mt-1">
                    <code className="text-xs text-gray-500 font-mono">
                      {key.cle.substring(0, 12)}...{key.cle.substring(key.cle.length - 4)}
                    </code>
                    <button onClick={() => copyKey(key.cle)} className="text-gray-400 hover:text-cyan-500 transition-colors">
                      {copiedKey === key.cle ? <CheckCircle2 className="w-3.5 h-3.5 text-green-500" /> : <Copy className="w-3.5 h-3.5" />}
                    </button>
                  </div>
                  <div className="text-xs text-gray-400 mt-1">
                    Créée le {new Date(key.dateCreation).toLocaleDateString('fr-FR')}
                    {key.derniereUtilisation && ` • Utilisée le ${new Date(key.derniereUtilisation).toLocaleDateString('fr-FR')}`}
                  </div>
                </div>
                <button
                  onClick={() => revokeApiKey(key.id)}
                  className="px-3 py-1.5 rounded-lg bg-red-100 text-red-700 text-xs font-medium hover:bg-red-200 transition-all"
                >
                  Révoquer
                </button>
              </div>
            ))}
          </div>
        )}
      </div>

      {/* Create Key Modal */}
      {showCreateKey && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
          <div className="fixed inset-0 bg-black/50 backdrop-blur-sm" onClick={() => setShowCreateKey(false)} />
          <div className="relative bg-white dark:bg-gray-800 rounded-2xl shadow-2xl max-w-md w-full p-6 border border-gray-200 dark:border-white/10">
            <h2 className="text-lg font-bold text-gray-900 dark:text-white mb-4">Nouvelle clé API</h2>
            <div className="space-y-4">
              <div>
                <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">Nom de la clé</label>
                <input
                  type="text"
                  value={newKeyName}
                  onChange={e => setNewKeyName(e.target.value)}
                  className="w-full px-4 py-2.5 rounded-xl border border-gray-200 dark:border-white/10 bg-white dark:bg-white/5 text-gray-900 dark:text-white text-sm focus:outline-none focus:ring-2 focus:ring-cyan-500"
                  placeholder="Ex: Application mobile"
                />
              </div>
              <div className="p-3 rounded-xl bg-amber-50 dark:bg-amber-500/10 border border-amber-200 dark:border-amber-500/20">
                <p className="text-xs text-amber-700 dark:text-amber-400 flex items-center gap-1">
                  <Shield className="w-3 h-3" />
                  La clé ne sera affichée qu'une seule fois. Copiez-la immédiatement.
                </p>
              </div>
            </div>
            <div className="flex justify-end gap-3 mt-6">
              <button onClick={() => setShowCreateKey(false)} className="px-4 py-2 rounded-xl border border-gray-200 dark:border-white/10 text-gray-700 dark:text-gray-300 text-sm hover:bg-gray-50 dark:hover:bg-white/5 transition-all">
                Annuler
              </button>
              <button onClick={createApiKey} className="px-4 py-2 rounded-xl bg-cyan-600 text-white text-sm font-medium hover:bg-cyan-700 transition-all">
                Créer la clé
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Webhooks Info */}
      <div className="p-6 rounded-2xl border border-gray-200 dark:border-white/10 bg-white dark:bg-white/5">
        <h2 className="text-lg font-bold text-gray-900 dark:text-white mb-3">🔗 Webhooks</h2>
        <p className="text-sm text-gray-500 dark:text-gray-400 mb-4">
          Configurez des webhooks pour recevoir des notifications en temps réel sur vos applications externes.
          Les livraisons sont signées HMAC-SHA256 avec l'en-tête <code>X-Discipolat-Signature</code>.
        </p>
        <div className="p-3 rounded-xl bg-gray-50 dark:bg-white/2 font-mono text-sm text-gray-700 dark:text-gray-300 overflow-x-auto">
          <span className="text-gray-400"># Exemple de vérification webhook</span>{'\n'}
          <span className="text-green-600">const</span> signature = crypto.createHmac('sha256', SECRET){'\n'}
          {'  '}.update(req.body).digest('hex');{'\n'}
          <span className="text-green-600">if</span> (signature !== req.headers['x-discipolat-signature']) {'{'}{'\n'}
          {'  '}res.status(401).send('Invalid signature');{'\n'}
          {'}'}
        </div>
        <a
          href="/admin/webhooks"
          className="inline-flex items-center gap-2 mt-4 px-4 py-2 rounded-xl bg-cyan-100 dark:bg-cyan-500/20 text-cyan-700 dark:text-cyan-400 text-sm font-medium hover:bg-cyan-200 dark:hover:bg-cyan-500/30 transition-all"
        >
          <ExternalLink className="w-4 h-4" />
          Gérer les webhooks
        </a>
      </div>
    </div>
  );
}
