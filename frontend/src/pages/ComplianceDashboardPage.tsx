import React, { useState, useEffect } from 'react';
import { useI18n } from '@/i18n';
import api from '@/lib/api';
import Toast from '@/components/shared/Toast';
import { Shield, Download, Trash2, Clock, FileText, CheckCircle2, AlertTriangle, Settings } from 'lucide-react';

interface ComplianceStats {
  totalDataRecords: number;
  pendingDeletions: number;
  dataRetentionDays: number;
  lastAuditDate: string;
  consentCoverage: number;
  exportRequests: number;
}

interface RetentionPolicy {
  id: string;
  entityType: string;
  retentionDays: number;
  autoDelete: boolean;
  lastPurgeDate?: string;
}

interface DataExportRequest {
  id: string;
  userId: string;
  userName: string;
  status: 'EN_ATTENTE' | 'EN_COURS' | 'TERMINE' | 'ECHEC';
  requestedAt: string;
  completedAt?: string;
  format: 'CSV' | 'JSON' | 'PDF';
}

export default function ComplianceDashboardPage() {
  const { t } = useI18n();
  const [stats, setStats] = useState<ComplianceStats | null>(null);
  const [policies, setPolicies] = useState<RetentionPolicy[]>([]);
  const [exports, setExports] = useState<DataExportRequest[]>([]);
  const [loading, setLoading] = useState(true);
  const [activeTab, setActiveTab] = useState<'overview' | 'retention' | 'exports'>('overview');

  useEffect(() => { loadData(); }, []);

  const loadData = async () => {
    try {
      setLoading(true);
      const [statsRes, policiesRes, exportsRes] = await Promise.allSettled([
        api.get('/compliance/stats'),
        api.get('/compliance/retention-policies'),
        api.get('/compliance/exports'),
      ]);
      if (statsRes.status === 'fulfilled') setStats(statsRes.value.data);
      if (policiesRes.status === 'fulfilled') setPolicies(policiesRes.value.data?.content || policiesRes.value.data || []);
      if (exportsRes.status === 'fulfilled') setExports(exportsRes.value.data?.content || exportsRes.value.data || []);
    } catch {
      // Fallback
    } finally {
      setLoading(false);
    }
  };

  const triggerPurge = async (policyId: string) => {
    if (!confirm('Êtes-vous sûr de vouloir lancer la purge maintenant ?')) return;
    try {
      await api.post(`/compliance/retention-policies/${policyId}/purge`);
      Toast.success('Purge lancée avec succès');
      loadData();
    } catch {
      Toast.error('Erreur lors de la purge');
    }
  };

  const requestDataExport = async (userId: string, format: string) => {
    try {
      await api.post('/compliance/exports', { userId, format });
      Toast.success('Demande d\'export créée');
      loadData();
    } catch {
      Toast.error('Erreur lors de la demande');
    }
  };

  const hashChainVerify = async () => {
    try {
      const res = await api.get('/compliance/audit-hash/verify');
      if (res.data.valid) {
        Toast.success('Chaîne d\'audit intégrée — aucune altération détectée');
      } else {
        Toast.error('ATTENTION : altération détectée dans la chaîne d\'audit !');
      }
    } catch {
      Toast.error('Vérification impossible');
    }
  };

  const getStatutColor = (statut: string) => {
    switch (statut) {
      case 'TERMINE': return 'bg-green-100 text-green-700';
      case 'EN_COURS': return 'bg-blue-100 text-blue-700';
      case 'EN_ATTENTE': return 'bg-amber-100 text-amber-700';
      default: return 'bg-red-100 text-red-700';
    }
  };

  const tabs = [
    { key: 'overview', label: 'Vue d\'ensemble', icon: Shield },
    { key: 'retention', label: 'Rétention', icon: Clock },
    { key: 'exports', label: 'Exports', icon: Download },
  ] as const;

  return (
    <div className="min-h-screen p-4 md:p-8 max-w-6xl mx-auto">
      <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4 mb-8">
        <div>
          <h1 className="text-2xl md:text-3xl font-bold text-gray-900 dark:text-white flex items-center gap-3">
            <Shield className="w-8 h-8 text-emerald-500" />
            Conformité RGPD / CCPA
          </h1>
          <p className="text-gray-500 dark:text-gray-400 mt-1">
            Gestion de la conformité, rétention des données et portabilité
          </p>
        </div>
        <button
          onClick={hashChainVerify}
          className="px-4 py-2 rounded-xl bg-gradient-to-r from-emerald-600 to-teal-600 text-white text-sm font-medium hover:from-emerald-700 hover:to-teal-700 transition-all shadow-lg shadow-emerald-500/25 flex items-center gap-2"
        >
          <CheckCircle2 className="w-4 h-4" />
          Vérifier chaîne d'audit
        </button>
      </div>

      {/* Tabs */}
      <div className="flex gap-2 mb-6 border-b border-gray-200 dark:border-white/10 pb-2">
        {tabs.map(tab => {
          const TabIcon = tab.icon;
          return (
            <button
              key={tab.key}
              onClick={() => setActiveTab(tab.key)}
              className={`flex items-center gap-2 px-4 py-2 rounded-xl text-sm font-medium transition-all ${
                activeTab === tab.key
                  ? 'bg-emerald-100 dark:bg-emerald-500/20 text-emerald-700 dark:text-emerald-400'
                  : 'text-gray-500 dark:text-gray-400 hover:bg-gray-100 dark:hover:bg-white/5'
              }`}
            >
              <TabIcon className="w-4 h-4" />
              {tab.label}
            </button>
          );
        })}
      </div>

      {/* Overview Tab */}
      {activeTab === 'overview' && (
        <div className="space-y-6">
          <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
            <div className="p-5 rounded-2xl bg-emerald-50 dark:bg-emerald-500/10 border border-emerald-200 dark:border-emerald-500/20">
              <FileText className="w-6 h-6 text-emerald-500 mb-2" />
              <div className="text-2xl font-bold text-emerald-900 dark:text-emerald-300">{stats?.totalDataRecords || 0}</div>
              <div className="text-xs text-emerald-600">Enregistrements données</div>
            </div>
            <div className="p-5 rounded-2xl bg-amber-50 dark:bg-amber-500/10 border border-amber-200 dark:border-amber-500/20">
              <Trash2 className="w-6 h-6 text-amber-500 mb-2" />
              <div className="text-2xl font-bold text-amber-900 dark:text-amber-300">{stats?.pendingDeletions || 0}</div>
              <div className="text-xs text-amber-600">Suppressions en attente</div>
            </div>
            <div className="p-5 rounded-2xl bg-blue-50 dark:bg-blue-500/10 border border-blue-200 dark:border-blue-500/20">
              <Download className="w-6 h-6 text-blue-500 mb-2" />
              <div className="text-2xl font-bold text-blue-900 dark:text-blue-300">{stats?.exportRequests || 0}</div>
              <div className="text-xs text-blue-600">Demandes d'export</div>
            </div>
            <div className="p-5 rounded-2xl bg-purple-50 dark:bg-purple-500/10 border border-purple-200 dark:border-purple-500/20">
              <Shield className="w-6 h-6 text-purple-500 mb-2" />
              <div className="text-2xl font-bold text-purple-900 dark:text-purple-300">{stats?.consentCoverage || 0}%</div>
              <div className="text-xs text-purple-600">Couverture consentements</div>
            </div>
          </div>

          {/* Compliance Checklist */}
          <div className="p-6 rounded-2xl border border-gray-200 dark:border-white/10 bg-white dark:bg-white/5">
            <h2 className="text-lg font-bold text-gray-900 dark:text-white mb-4">Checklist de conformité</h2>
            <div className="space-y-3">
              {[
                { label: 'Politique de confidentialité publiée', status: true },
                { label: 'Consentements collectés pour chaque membre', status: (stats?.consentCoverage || 0) >= 80 },
                { label: 'Droit à l\'oubli implémenté (suppression données)', status: true },
                { label: 'Portabilité des données (export 1-clic)', status: true },
                { label: 'Journal d\'audit immuable (hash chaîné)', status: true },
                { label: 'Rétention des données configurée', status: policies.length > 0 },
                { label: 'Purge automatique activée', status: policies.some(p => p.autoDelete) },
                { label: 'Chiffrement des données sensibles', status: false },
              ].map((item, i) => (
                <div key={i} className="flex items-center gap-3 p-3 rounded-xl bg-gray-50 dark:bg-white/2">
                  {item.status ? (
                    <CheckCircle2 className="w-5 h-5 text-green-500 shrink-0" />
                  ) : (
                    <AlertTriangle className="w-5 h-5 text-amber-500 shrink-0" />
                  )}
                  <span className={`text-sm ${item.status ? 'text-gray-700 dark:text-gray-300' : 'text-amber-700 dark:text-amber-400'}`}>
                    {item.label}
                  </span>
                </div>
              ))}
            </div>
          </div>
        </div>
      )}

      {/* Retention Tab */}
      {activeTab === 'retention' && (
        <div className="space-y-4">
          <div className="p-5 rounded-2xl border border-gray-200 dark:border-white/10 bg-white dark:bg-white/5">
            <h2 className="text-lg font-bold text-gray-900 dark:text-white mb-4">Politiques de rétention</h2>
            {policies.length === 0 ? (
              <p className="text-sm text-gray-500 dark:text-gray-400">
                Aucune politique de rétention configurée. Les données sont conservées indéfiniment.
              </p>
            ) : (
              <div className="space-y-3">
                {policies.map(policy => (
                  <div key={policy.id} className="flex items-center justify-between p-4 rounded-xl bg-gray-50 dark:bg-white/2">
                    <div>
                      <div className="text-sm font-medium text-gray-900 dark:text-white">{policy.entityType}</div>
                      <div className="text-xs text-gray-500 dark:text-gray-400">
                        Rétention : {policy.retentionDays} jours
                        {policy.autoDelete && ' • Suppression auto'}
                        {policy.lastPurgeDate && ` • Dernière purge : ${new Date(policy.lastPurgeDate).toLocaleDateString('fr-FR')}`}
                      </div>
                    </div>
                    <button
                      onClick={() => triggerPurge(policy.id)}
                      className="px-3 py-1.5 rounded-lg bg-red-100 text-red-700 text-xs font-medium hover:bg-red-200 transition-all flex items-center gap-1"
                    >
                      <Trash2 className="w-3 h-3" />
                      Purger
                    </button>
                  </div>
                ))}
              </div>
            )}
          </div>

          <div className="p-5 rounded-2xl border border-gray-200 dark:border-white/10 bg-white dark:bg-white/5">
            <h3 className="text-sm font-semibold text-gray-700 dark:text-gray-300 mb-3">Types de données et rétention suggérée</h3>
            <div className="grid md:grid-cols-2 gap-3">
              {[
                { type: 'Rapports faiseur', days: 730, note: '2 ans' },
                { type: 'Présences', days: 365, note: '1 an' },
                { type: 'Messages privés', days: 365, note: '1 an' },
                { type: 'Audit logs', days: 1095, note: '3 ans (immuable)' },
                { type: 'Notes pastorales', days: 1825, note: '5 ans' },
                { type: 'Données financières', days: 2555, note: '7 ans (fiscal)' },
              ].map(item => (
                <div key={item.type} className="flex items-center justify-between p-3 rounded-xl bg-gray-50 dark:bg-white/2">
                  <div>
                    <div className="text-sm text-gray-700 dark:text-gray-300">{item.type}</div>
                    <div className="text-xs text-gray-400">{item.note}</div>
                  </div>
                  <span className="text-xs font-medium text-gray-500">{item.days}j</span>
                </div>
              ))}
            </div>
          </div>
        </div>
      )}

      {/* Exports Tab */}
      {activeTab === 'exports' && (
        <div className="space-y-4">
          <div className="p-5 rounded-2xl border border-gray-200 dark:border-white/10 bg-white dark:bg-white/5">
            <h2 className="text-lg font-bold text-gray-900 dark:text-white mb-4">Demandes d'export de données</h2>
            {exports.length === 0 ? (
              <p className="text-sm text-gray-500 dark:text-gray-400">
                Aucune demande d'export en cours.
              </p>
            ) : (
              <div className="space-y-3">
                {exports.map(exp => (
                  <div key={exp.id} className="flex items-center justify-between p-4 rounded-xl bg-gray-50 dark:bg-white/2">
                    <div>
                      <div className="text-sm font-medium text-gray-900 dark:text-white">{exp.userName}</div>
                      <div className="text-xs text-gray-500 dark:text-gray-400">
                        Format : {exp.format} • Demandé le {new Date(exp.requestedAt).toLocaleDateString('fr-FR')}
                        {exp.completedAt && ` • Terminé le ${new Date(exp.completedAt).toLocaleDateString('fr-FR')}`}
                      </div>
                    </div>
                    <div className="flex items-center gap-2">
                      <span className={`px-2 py-1 rounded-full text-xs font-medium ${getStatutColor(exp.status)}`}>
                        {exp.status}
                      </span>
                      {exp.status === 'TERMINE' && (
                        <button className="px-3 py-1 rounded-lg bg-emerald-100 text-emerald-700 text-xs font-medium hover:bg-emerald-200 transition-all flex items-center gap-1">
                          <Download className="w-3 h-3" />
                          Télécharger
                        </button>
                      )}
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>

          <div className="p-5 rounded-2xl border border-dashed border-gray-300 dark:border-white/10 text-center">
            <Download className="w-8 h-8 text-gray-400 mx-auto mb-3" />
            <h3 className="text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">Demander un export de données</h3>
            <p className="text-xs text-gray-500 dark:text-gray-400 mb-3">
              Conformément au RGPD, chaque membre peut demander l'export de ses données personnelles.
            </p>
            <button
              onClick={() => requestDataExport('current', 'JSON')}
              className="px-4 py-2 rounded-xl bg-emerald-600 text-white text-sm font-medium hover:bg-emerald-700 transition-all"
            >
              Exporter mes données (JSON)
            </button>
          </div>
        </div>
      )}
    </div>
  );
}
