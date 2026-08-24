import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import toast from 'react-hot-toast';
import api from '@/lib/api';
import {
  Shield, Download, Trash2, Clock, FileText, CheckCircle2, AlertTriangle,
  Plus, RefreshCw, Eye, X, Lock, Users, Hash, ChevronDown, Search, Filter,
  ArrowRight, ExternalLink, Check, XCircle, Loader2,
} from 'lucide-react';

interface ComplianceOverview {
  activeRetentionPolicies: number;
  pendingGdprRequests: number;
  activeConsents: number;
  lastAuditActivity?: string;
  auditIntegrityValid: boolean;
  auditTotalEntries: number;
}

interface RetentionPolicy {
  id: string;
  dataCategory: string;
  retentionDays: number;
  actionOnExpiry: string;
  isActive: boolean;
  lastPurgeAt?: string;
  lastPurgeCount: number;
}

interface ConsentRecord {
  id: string;
  userId: string;
  consentType: string;
  policyVersion?: string;
  granted: boolean;
  method: string;
  consentedAt: string;
  revokedAt?: string;
}

interface AuditEntry {
  id: string;
  actorId: string;
  actorEmail?: string;
  action: string;
  resourceType: string;
  resourceId?: string;
  details?: string;
  entryHash: string;
  createdAt: string;
}

const TABS = [
  { key: 'overview', label: 'Vue d\'ensemble', icon: Shield },
  { key: 'retention', label: 'Rétention', icon: Clock },
  { key: 'consents', label: 'Consentements', icon: Users },
  { key: 'audit', label: 'Audit Trail', icon: Hash },
  { key: 'portability', label: 'Portabilité', icon: Download },
] as const;

const DATA_CATEGORIES = [
  { value: 'SOULS', label: 'Âmes (disciples)', icon: '👤' },
  { value: 'PRAYERS', label: 'Prières', icon: '🙏' },
  { value: 'NOTES_PASTORALES', label: 'Notes pastorales', icon: '📝' },
  { value: 'MESSAGES', label: 'Messages', icon: '💬' },
  { value: 'TRANSACTIONS', label: 'Transactions', icon: '💰' },
  { value: 'GDPR_REQUESTS', label: 'Demandes RGPD', icon: '📋' },
  { value: 'AUDIT_LOGS', label: 'Logs d\'audit', icon: '🔒' },
];

const CONSENT_TYPES = [
  { value: 'DATA_PROCESSING', label: 'Traitement des données' },
  { value: 'MARKETING', label: 'Communications marketing' },
  { value: 'ANALYTICS', label: 'Analyse d\'usage' },
  { value: 'THIRD_PARTY_SHARING', label: 'Partage avec des tiers' },
  { value: 'LOCATION_TRACKING', label: 'Suivi de géolocalisation' },
];

export default function ComplianceDashboardPage() {
  const queryClient = useQueryClient();
  const [activeTab, setActiveTab] = useState<string>('overview');
  const [showAddPolicy, setShowAddPolicy] = useState(false);
  const [showGrantConsent, setShowGrantConsent] = useState(false);
  const [selectedUser, setSelectedUser] = useState('');
  const [newPolicy, setNewPolicy] = useState({ dataCategory: 'SOULS', retentionDays: 365, actionOnExpiry: 'ANONYMIZE' });
  const [newConsent, setNewConsent] = useState({ userId: '', consentType: 'DATA_PROCESSING', policyVersion: '2.0' });
  const [auditLimit, setAuditLimit] = useState(50);

  // ── Queries ──────────────────────────────────────
  const { data: overview, isLoading: overviewLoading } = useQuery({
    queryKey: ['compliance', 'overview'],
    queryFn: async () => {
      const res = await api.get('/compliance/overview');
      return res.data as ComplianceOverview;
    },
  });

  const { data: policies = [], refetch: refetchPolicies } = useQuery({
    queryKey: ['compliance', 'retention-policies'],
    queryFn: async () => {
      const res = await api.get('/compliance/retention-policies');
      return res.data as RetentionPolicy[];
    },
  });

  const { data: auditEntries = [], refetch: refetchAudit } = useQuery({
    queryKey: ['compliance', 'audit', auditLimit],
    queryFn: async () => {
      const res = await api.get('/compliance/audit', { params: { limit: auditLimit } });
      return res.data as AuditEntry[];
    },
  });

  // ── Mutations ──────────────────────────────────────
  const createPolicyMutation = useMutation({
    mutationFn: async () => {
      await api.post('/compliance/retention-policies', newPolicy);
    },
    onSuccess: () => {
      toast.success('Politique de rétention créée');
      refetchPolicies();
      setShowAddPolicy(false);
      setNewPolicy({ dataCategory: 'SOULS', retentionDays: 365, actionOnExpiry: 'ANONYMIZE' });
    },
    onError: () => toast.error('Erreur lors de la création'),
  });

  const deletePolicyMutation = useMutation({
    mutationFn: async (id: string) => {
      await api.delete(`/compliance/retention-policies/${id}`);
    },
    onSuccess: () => {
      toast.success('Politique désactivée');
      refetchPolicies();
    },
    onError: () => toast.error('Erreur lors de la suppression'),
  });

  const executePurgeMutation = useMutation({
    mutationFn: async (id: string) => {
      await api.post(`/compliance/retention-policies/${id}/execute`);
    },
    onSuccess: () => {
      toast.success('Purge exécutée');
      refetchPolicies();
    },
    onError: () => toast.error('Erreur lors de la purge'),
  });

  const grantConsentMutation = useMutation({
    mutationFn: async () => {
      await api.post('/compliance/consents', newConsent);
    },
    onSuccess: () => {
      toast.success('Consentement enregistré');
      setShowGrantConsent(false);
      setNewConsent({ userId: '', consentType: 'DATA_PROCESSING', policyVersion: '2.0' });
    },
    onError: () => toast.error('Erreur lors de l\'enregistrement'),
  });

  const verifyIntegrityMutation = useMutation({
    mutationFn: async () => {
      const res = await api.get('/compliance/audit/verify');
      return res.data as { integrityValid: boolean; totalEntries: number; brokenLinks: number };
    },
    onSuccess: (data) => {
      if (data.integrityValid) {
        toast.success(`Chaîne d'audit valide — ${data.totalEntries} entrées vérifiées`);
      } else {
        toast.error(`ATTENTION : ${data.brokenLinks} lien(s) brisé(s) dans la chaîne d'audit !`);
      }
      queryClient.invalidateQueries({ queryKey: ['compliance', 'overview'] });
    },
    onError: () => toast.error('Vérification impossible'),
  });

  const portabilityMutation = useMutation({
    mutationFn: async (userId: string) => {
      const res = await api.get(`/compliance/portability/${userId}`);
      return res.data;
    },
    onSuccess: (data) => {
      // Download as JSON
      const blob = new Blob([JSON.stringify(data, null, 2)], { type: 'application/json' });
      const url = URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = `portability-export-${new Date().toISOString().slice(0, 10)}.json`;
      a.click();
      URL.revokeObjectURL(url);
      toast.success('Export de portabilité téléchargé');
    },
    onError: () => toast.error('Erreur lors de l\'export'),
  });

  // ── Render ──────────────────────────────────────
  if (overviewLoading) {
    return (
      <div className="min-h-[40vh] flex items-center justify-center">
        <div className="spinner h-8 w-8" />
      </div>
    );
  }

  return (
    <div className="page-container max-w-6xl">
      {/* Header */}
      <div className="page-header">
        <div>
          <h1 className="page-title flex items-center gap-2">
            <Shield className="w-5 h-5 text-emerald-500" />
            Compliance Manager RGPD/CCPA
          </h1>
          <p className="page-subtitle">
            Rétention configurable • Consentements • Audit trail immuable • Portabilité 1-clic
          </p>
        </div>
        <div className="page-header-actions">
          <button onClick={() => { refetchPolicies(); refetchAudit(); }} className="btn-ghost btn-sm">
            <RefreshCw className="w-4 h-4" /> Actualiser
          </button>
          <button
            onClick={() => verifyIntegrityMutation.mutate()}
            disabled={verifyIntegrityMutation.isPending}
            className="btn-primary btn-sm"
          >
            {verifyIntegrityMutation.isPending ? (
              <Loader2 className="w-4 h-4 animate-spin" />
            ) : (
              <CheckCircle2 className="w-4 h-4" />
            )}
            Vérifier l'audit
          </button>
        </div>
      </div>

      {/* Tabs */}
      <div className="flex flex-wrap gap-2 mb-6">
        {TABS.map((tab) => {
          const TabIcon = tab.icon;
          return (
            <button
              key={tab.key}
              onClick={() => setActiveTab(tab.key)}
              className={`flex items-center gap-2 px-4 py-2 rounded-xl text-sm font-medium transition-all ${
                activeTab === tab.key
                  ? 'bg-emerald-100 dark:bg-emerald-500/20 text-emerald-700 dark:text-emerald-400 shadow-sm'
                  : 'text-gray-500 dark:text-gray-400 hover:bg-gray-100 dark:hover:bg-white/5'
              }`}
            >
              <TabIcon className="w-4 h-4" />
              {tab.label}
            </button>
          );
        })}
      </div>

      {/* ════════════ OVERVIEW TAB ════════════ */}
      {activeTab === 'overview' && (
        <div className="space-y-6 animate-slide-up">
          {/* Stats */}
          <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
            {[
              { label: 'Politiques actives', value: overview?.activeRetentionPolicies ?? 0, icon: Clock, color: 'emerald' },
              { label: 'Consentements actifs', value: overview?.activeConsents ?? 0, icon: Users, color: 'blue' },
              { label: 'Entrées audit', value: overview?.auditTotalEntries ?? 0, icon: Hash, color: 'purple' },
              { label: 'Demandes en attente', value: overview?.pendingGdprRequests ?? 0, icon: FileText, color: 'amber' },
            ].map((stat, i) => (
              <div
                key={stat.label}
                className={`stat-card animate-slide-up`}
                style={{ animationDelay: `${i * 60}ms` }}
              >
                <div className="flex items-start justify-between mb-2">
                  <span className="stat-label text-[10px]">{stat.label}</span>
                  <stat.icon className={`w-4 h-4 text-${stat.color}-500`} />
                </div>
                <p className="stat-value text-2xl">{stat.value}</p>
              </div>
            ))}
          </div>

          {/* Audit Integrity */}
          <div className={`glass-card p-6 animate-slide-up ${overview?.auditIntegrityValid ? 'border-l-4 border-green-500' : 'border-l-4 border-red-500'}`}>
            <div className="flex items-center gap-3 mb-3">
              {overview?.auditIntegrityValid ? (
                <CheckCircle2 className="w-6 h-6 text-green-500" />
              ) : (
                <AlertTriangle className="w-6 h-6 text-red-500" />
              )}
              <div>
                <h3 className="text-sm font-bold text-gray-900 dark:text-gray-100">
                  Intégrité de la chaîne d'audit
                </h3>
                <p className="text-xs text-gray-500 dark:text-gray-400">
                  {overview?.auditIntegrityValid
                    ? `✅ Valide — ${overview.auditTotalEntries} entrées vérifiées, aucune altération`
                    : `⚠️ Altération détectée — contactez l'administrateur`}
                </p>
              </div>
            </div>
            {overview?.lastAuditActivity && (
              <p className="text-xs text-gray-400">
                Dernière activité d'audit : {new Date(overview.lastAuditActivity).toLocaleString('fr-FR')}
              </p>
            )}
          </div>

          {/* Compliance Checklist */}
          <div className="glass-card p-6 animate-slide-up">
            <h3 className="text-sm font-bold text-gray-900 dark:text-gray-100 mb-4">Checklist de conformité</h3>
            <div className="space-y-3">
              {[
                { label: 'Politique de rétention des données configurée', done: (overview?.activeRetentionPolicies ?? 0) > 0 },
                { label: 'Consentements collectés', done: (overview?.activeConsents ?? 0) > 0 },
                { label: 'Journal d\'audit immuable (hash chaîné)', done: overview?.auditIntegrityValid === true },
                { label: 'Portabilité des données (export 1-clic)', done: true },
                { label: 'Suppression des données (droit à l\'oubli)', done: true },
                { label: 'Chiffrement AES-256-GCM des données sensibles', done: true },
              ].map((item, i) => (
                <div key={i} className="flex items-center gap-3 p-3 rounded-xl bg-gray-50 dark:bg-gray-800/40">
                  {item.done ? (
                    <CheckCircle2 className="w-5 h-5 text-green-500 shrink-0" />
                  ) : (
                    <AlertTriangle className="w-5 h-5 text-amber-500 shrink-0" />
                  )}
                  <span className={`text-sm ${item.done ? 'text-gray-700 dark:text-gray-300' : 'text-amber-700 dark:text-amber-400'}`}>
                    {item.label}
                  </span>
                </div>
              ))}
            </div>
          </div>
        </div>
      )}

      {/* ════════════ RETENTION TAB ════════════ */}
      {activeTab === 'retention' && (
        <div className="space-y-4 animate-slide-up">
          <div className="flex items-center justify-between">
            <h2 className="text-sm font-bold text-gray-900 dark:text-gray-100">Politiques de rétention</h2>
            <button onClick={() => setShowAddPolicy(true)} className="btn-primary btn-sm">
              <Plus className="w-4 h-4" /> Nouvelle politique
            </button>
          </div>

          {policies.length === 0 ? (
            <div className="glass-card p-10 text-center">
              <Clock className="w-10 h-10 text-gray-300 mb-3 mx-auto" />
              <p className="text-gray-500 font-medium">Aucune politique de rétention configurée.</p>
              <button onClick={() => setShowAddPolicy(true)} className="text-primary-500 hover:underline text-sm mt-2">
                Créer la première politique
              </button>
            </div>
          ) : (
            <div className="space-y-3">
              {policies.map((p) => {
                const catInfo = DATA_CATEGORIES.find(c => c.value === p.dataCategory);
                return (
                  <div key={p.id} className="glass-card px-5 py-4 flex items-center gap-4">
                    <div className="w-11 h-11 rounded-xl bg-gradient-to-br from-emerald-500 to-teal-600 flex items-center justify-center text-white shadow-sm flex-shrink-0 text-lg">
                      {catInfo?.icon || '📁'}
                    </div>
                    <div className="flex-1 min-w-0">
                      <div className="flex items-center gap-2">
                        <span className="text-sm font-bold text-gray-900 dark:text-gray-100">
                          {catInfo?.label || p.dataCategory}
                        </span>
                        <span className={`badge text-[10px] ${p.isActive ? 'badge-success' : 'badge-error'}`}>
                          {p.isActive ? 'Actif' : 'Inactif'}
                        </span>
                      </div>
                      <div className="flex items-center gap-3 mt-1 text-xs text-gray-400">
                        <span>📅 {p.retentionDays} jours</span>
                        <span>⚡ {p.actionOnExpiry === 'ANONYMIZE' ? 'Anonymiser' : p.actionOnExpiry === 'DELETE' ? 'Supprimer' : 'Archiver'}</span>
                        {p.lastPurgeAt && (
                          <span>Dernière purge : {new Date(p.lastPurgeAt).toLocaleDateString('fr-FR')} ({p.lastPurgeCount} records)</span>
                        )}
                      </div>
                    </div>
                    <div className="flex items-center gap-1">
                      <button
                        onClick={() => executePurgeMutation.mutate(p.id)}
                        className="btn-icon text-gray-400 hover:text-amber-500 hover:bg-amber-50 dark:hover:bg-amber-900/20"
                        title="Exécuter la purge"
                      >
                        <Trash2 className="w-4 h-4" />
                      </button>
                      <button
                        onClick={() => deletePolicyMutation.mutate(p.id)}
                        className="btn-icon text-gray-400 hover:text-red-500 hover:bg-red-50 dark:hover:bg-red-900/20"
                        title="Désactiver"
                      >
                        <XCircle className="w-4 h-4" />
                      </button>
                    </div>
                  </div>
                );
              })}
            </div>
          )}

          {/* Suggested policies */}
          <div className="glass-card p-5 animate-slide-up">
            <h3 className="text-sm font-semibold text-gray-700 dark:text-gray-300 mb-3">Durées de rétention suggérées</h3>
            <div className="grid md:grid-cols-2 gap-2">
              {[
                { type: 'SOULS', days: 1095, note: '3 ans' },
                { type: 'PRAYERS', days: 365, note: '1 an' },
                { type: 'NOTES_PASTORALES', days: 1825, note: '5 ans' },
                { type: 'MESSAGES', days: 365, note: '1 an' },
                { type: 'TRANSACTIONS', days: 2555, note: '7 ans (fiscal)' },
                { type: 'AUDIT_LOGS', days: 2555, note: '7 ans (immuable)' },
              ].map((item) => (
                <button
                  key={item.type}
                  onClick={() => {
                    setNewPolicy({ dataCategory: item.type, retentionDays: item.days, actionOnExpiry: 'ANONYMIZE' });
                    setShowAddPolicy(true);
                  }}
                  className="flex items-center justify-between p-3 rounded-xl bg-gray-50 dark:bg-gray-800/40 hover:bg-emerald-50 dark:hover:bg-emerald-900/10 transition-all text-left"
                >
                  <div>
                    <div className="text-sm text-gray-700 dark:text-gray-300">{item.type}</div>
                    <div className="text-xs text-gray-400">{item.note}</div>
                  </div>
                  <ArrowRight className="w-4 h-4 text-gray-400" />
                </button>
              ))}
            </div>
          </div>
        </div>
      )}

      {/* ════════════ CONSENTS TAB ════════════ */}
      {activeTab === 'consents' && (
        <div className="space-y-4 animate-slide-up">
          <div className="flex items-center justify-between">
            <h2 className="text-sm font-bold text-gray-900 dark:text-gray-100">Gestion des consentements</h2>
            <button onClick={() => setShowGrantConsent(true)} className="btn-primary btn-sm">
              <Plus className="w-4 h-4" /> Enregistrer un consentement
            </button>
          </div>

          {/* Consent types overview */}
          <div className="grid grid-cols-2 lg:grid-cols-3 gap-3">
            {CONSENT_TYPES.map((ct) => (
              <div key={ct.value} className="glass-card p-4">
                <div className="flex items-center gap-2 mb-2">
                  <Lock className="w-4 h-4 text-emerald-500" />
                  <span className="text-sm font-semibold text-gray-900 dark:text-gray-100">{ct.label}</span>
                </div>
                <p className="text-[10px] text-gray-400">Clé : {ct.value}</p>
              </div>
            ))}
          </div>

          <div className="glass-card p-6 text-center">
            <Users className="w-10 h-10 text-gray-300 mb-3 mx-auto" />
            <p className="text-sm text-gray-500">
              Consultez les consentements individuels via le profil de chaque utilisateur.
            </p>
            <p className="text-xs text-gray-400 mt-1">
              Utilisez l'endpoint <code>GET /api/v1/compliance/consents/{'{userId}'}</code> pour voir l'historique complet.
            </p>
          </div>
        </div>
      )}

      {/* ════════════ AUDIT TAB ════════════ */}
      {activeTab === 'audit' && (
        <div className="space-y-4 animate-slide-up">
          <div className="flex items-center justify-between">
            <h2 className="text-sm font-bold text-gray-900 dark:text-gray-100">Journal d'audit immuable</h2>
            <div className="flex items-center gap-2">
              <select
                className="input w-auto text-sm"
                value={auditLimit}
                onChange={(e) => setAuditLimit(Number(e.target.value))}
              >
                <option value={25}>25 entrées</option>
                <option value={50}>50 entrées</option>
                <option value={100}>100 entrées</option>
                <option value={250}>250 entrées</option>
              </select>
              <button onClick={() => refetchAudit()} className="btn-ghost btn-sm">
                <RefreshCw className="w-4 h-4" />
              </button>
            </div>
          </div>

          {auditEntries.length === 0 ? (
            <div className="glass-card p-10 text-center">
              <Hash className="w-10 h-10 text-gray-300 mb-3 mx-auto" />
              <p className="text-gray-500 font-medium">Aucune entrée d'audit.</p>
            </div>
          ) : (
            <div className="space-y-2">
              {auditEntries.map((entry) => (
                <div key={entry.id} className="glass-card px-5 py-3 flex items-center gap-4">
                  <div className="w-9 h-9 rounded-lg bg-gradient-to-br from-purple-500 to-indigo-600 flex items-center justify-center text-white shadow-sm flex-shrink-0">
                    <Hash className="w-4 h-4" />
                  </div>
                  <div className="flex-1 min-w-0">
                    <div className="flex items-center gap-2">
                      <span className="text-sm font-bold text-gray-900 dark:text-gray-100">{entry.action}</span>
                      <span className="badge text-[10px]">{entry.resourceType}</span>
                    </div>
                    <div className="flex items-center gap-3 mt-0.5 text-[10px] text-gray-400">
                      <span>👤 {entry.actorEmail || entry.actorId.slice(0, 8)}</span>
                      <span>📅 {new Date(entry.createdAt).toLocaleString('fr-FR')}</span>
                      {entry.resourceId && <span>🔗 {entry.resourceId.slice(0, 8)}...</span>}
                    </div>
                  </div>
                  <div className="text-[10px] text-gray-400 font-mono max-w-[120px] truncate" title={entry.entryHash}>
                    {entry.entryHash.slice(0, 12)}…
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>
      )}

      {/* ════════════ PORTABILITY TAB ════════════ */}
      {activeTab === 'portability' && (
        <div className="space-y-4 animate-slide-up">
          <div className="glass-card p-6">
            <div className="flex items-center gap-3 mb-4">
              <Download className="w-6 h-6 text-emerald-500" />
              <div>
                <h3 className="text-sm font-bold text-gray-900 dark:text-gray-100">Portabilité des données (RGPD Art. 20)</h3>
                <p className="text-xs text-gray-500 dark:text-gray-400">
                  Export complet des données personnelles d'un utilisateur au format JSON
                </p>
              </div>
            </div>
            <div className="flex gap-2">
              <input
                type="text"
                className="input flex-1"
                placeholder="ID de l'utilisateur (UUID)"
                value={selectedUser}
                onChange={(e) => setSelectedUser(e.target.value)}
              />
              <button
                onClick={() => portabilityMutation.mutate(selectedUser)}
                disabled={!selectedUser || portabilityMutation.isPending}
                className="btn-primary btn-sm"
              >
                {portabilityMutation.isPending ? (
                  <Loader2 className="w-4 h-4 animate-spin" />
                ) : (
                  <Download className="w-4 h-4" />
                )}
                Exporter
              </button>
            </div>
            <p className="text-[10px] text-gray-400 mt-2">
              L'export inclut : profil utilisateur, âmes liées, consentements, historique RGPD.
              Conformément au RGPD, l'export doit être fourni sous 30 jours.
            </p>
          </div>

          <div className="glass-card p-6">
            <h3 className="text-sm font-semibold text-gray-700 dark:text-gray-300 mb-3">Contenu de l'export</h3>
            <div className="grid grid-cols-2 gap-2">
              {[
                'Profil utilisateur (nom, email, téléphone)',
                'Âmes liées (disciples)',
                'Historique des consentements',
                'Demandes RGPD passées',
                'Métadonnées (format, version, date)',
              ].map((item, i) => (
                <div key={i} className="flex items-center gap-2 p-2 rounded-lg bg-gray-50 dark:bg-gray-800/40">
                  <Check className="w-3.5 h-3.5 text-emerald-500 shrink-0" />
                  <span className="text-xs text-gray-700 dark:text-gray-300">{item}</span>
                </div>
              ))}
            </div>
          </div>
        </div>
      )}

      {/* ════════════ ADD POLICY MODAL ════════════ */}
      {showAddPolicy && (
        <div className="modal-overlay" onClick={() => setShowAddPolicy(false)}>
          <div className="modal-content max-w-md" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <h3 className="text-base font-bold text-gray-900 dark:text-gray-100">Nouvelle politique de rétention</h3>
              <button className="btn-icon text-gray-400 hover:text-gray-600" onClick={() => setShowAddPolicy(false)}>
                <X className="w-5 h-5" />
              </button>
            </div>
            <div className="modal-body space-y-4">
              <div>
                <label className="label">Catégorie de données</label>
                <select
                  className="input"
                  value={newPolicy.dataCategory}
                  onChange={(e) => setNewPolicy({ ...newPolicy, dataCategory: e.target.value })}
                >
                  {DATA_CATEGORIES.map((c) => (
                    <option key={c.value} value={c.value}>{c.icon} {c.label}</option>
                  ))}
                </select>
              </div>
              <div>
                <label className="label">Durée de rétention (jours)</label>
                <input
                  type="number"
                  className="input"
                  value={newPolicy.retentionDays}
                  onChange={(e) => setNewPolicy({ ...newPolicy, retentionDays: Number(e.target.value) })}
                  min={1}
                />
                <p className="text-[10px] text-gray-400 mt-1">
                  {newPolicy.retentionDays} jours ≈ {Math.round(newPolicy.retentionDays / 30)} mois
                </p>
              </div>
              <div>
                <label className="label">Action à l'expiration</label>
                <select
                  className="input"
                  value={newPolicy.actionOnExpiry}
                  onChange={(e) => setNewPolicy({ ...newPolicy, actionOnExpiry: e.target.value })}
                >
                  <option value="ANONYMIZE">Anonymiser les données</option>
                  <option value="DELETE">Supprimer définitivement</option>
                  <option value="ARCHIVE">Archiver</option>
                </select>
              </div>
            </div>
            <div className="modal-footer">
              <button className="btn-ghost btn-sm" onClick={() => setShowAddPolicy(false)}>Annuler</button>
              <button
                className="btn-primary btn-sm"
                onClick={() => createPolicyMutation.mutate()}
                disabled={createPolicyMutation.isPending}
              >
                {createPolicyMutation.isPending ? <Loader2 className="w-4 h-4 animate-spin" /> : <Plus className="w-4 h-4" />}
                Créer
              </button>
            </div>
          </div>
        </div>
      )}

      {/* ════════════ GRANT CONSENT MODAL ════════════ */}
      {showGrantConsent && (
        <div className="modal-overlay" onClick={() => setShowGrantConsent(false)}>
          <div className="modal-content max-w-md" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <h3 className="text-base font-bold text-gray-900 dark:text-gray-100">Enregistrer un consentement</h3>
              <button className="btn-icon text-gray-400 hover:text-gray-600" onClick={() => setShowGrantConsent(false)}>
                <X className="w-5 h-5" />
              </button>
            </div>
            <div className="modal-body space-y-4">
              <div>
                <label className="label">ID Utilisateur (UUID)</label>
                <input
                  type="text"
                  className="input"
                  placeholder="UUID de l'utilisateur"
                  value={newConsent.userId}
                  onChange={(e) => setNewConsent({ ...newConsent, userId: e.target.value })}
                />
              </div>
              <div>
                <label className="label">Type de consentement</label>
                <select
                  className="input"
                  value={newConsent.consentType}
                  onChange={(e) => setNewConsent({ ...newConsent, consentType: e.target.value })}
                >
                  {CONSENT_TYPES.map((c) => (
                    <option key={c.value} value={c.value}>{c.label}</option>
                  ))}
                </select>
              </div>
              <div>
                <label className="label">Version de la politique</label>
                <input
                  type="text"
                  className="input"
                  value={newConsent.policyVersion}
                  onChange={(e) => setNewConsent({ ...newConsent, policyVersion: e.target.value })}
                />
              </div>
            </div>
            <div className="modal-footer">
              <button className="btn-ghost btn-sm" onClick={() => setShowGrantConsent(false)}>Annuler</button>
              <button
                className="btn-primary btn-sm"
                onClick={() => grantConsentMutation.mutate()}
                disabled={grantConsentMutation.isPending || !newConsent.userId}
              >
                {grantConsentMutation.isPending ? <Loader2 className="w-4 h-4 animate-spin" /> : <Check className="w-4 h-4" />}
                Enregistrer
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
