import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import toast from 'react-hot-toast';
import api, { getErrorMessage } from '@/lib/api';
import {
  Shield, Download, Trash2, Loader2, Clock, CheckCircle2, FileText,
  AlertTriangle, Search, Eye, ExternalLink,
} from 'lucide-react';

interface ComplianceExport {
  id: string;
  type: string;
  status: string;
  requestedAt: string;
  completedAt?: string;
  fileSize?: number;
  downloadUrl?: string;
}

interface GdprRequest {
  id: string;
  userId: string;
  userName?: string;
  requestType: string;
  status: string;
  createdAt: string;
  processedAt?: string;
  details?: string;
}

export default function ComplianceExportsPage() {
  const qc = useQueryClient();
  const [activeTab, setActiveTab] = useState<'exports' | 'gdpr'>('exports');
  const [searchUser, setSearchUser] = useState('');
  const [deleteUserId, setDeleteUserId] = useState('');
  const [exportUserId, setExportUserId] = useState('');

  const { data: exports = [], isLoading } = useQuery({
    queryKey: ['compliance', 'exports'],
    queryFn: async () => {
      const res = await api.get('/compliance/exports');
      return (res.data.content || res.data || []) as ComplianceExport[];
    },
  });

  const { data: gdprRequests = [], isLoading: gdprLoading } = useQuery({
    queryKey: ['compliance', 'gdpr'],
    queryFn: async () => {
      const res = await api.get('/compliance/gdpr');
      return (res.data.content || res.data || []) as GdprRequest[];
    },
  });

  const processMutation = useMutation({
    mutationFn: async (id: string) => {
      await api.post(`/compliance/gdpr/${id}/process`);
    },
    onSuccess: () => {
      toast.success('Demande traitée');
      qc.invalidateQueries({ queryKey: ['compliance'] });
    },
    onError: (err) => toast.error(getErrorMessage(err)),
  });

  const deleteMutation = useMutation({
    mutationFn: async () => {
      await api.post('/gdpr/delete', { userId: deleteUserId });
    },
    onSuccess: () => {
      toast.success('Demande de suppression envoyée');
      setDeleteUserId('');
    },
    onError: (err) => toast.error(getErrorMessage(err)),
  });

  const exportMutation = useMutation({
    mutationFn: async () => {
      const res = await api.post('/gdpr/export', { userId: exportUserId });
      return res.data;
    },
    onSuccess: (data) => {
      const blob = new Blob([JSON.stringify(data, null, 2)], { type: 'application/json' });
      const url = URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = `gdpr-export-${new Date().toISOString().slice(0, 10)}.json`;
      a.click();
      URL.revokeObjectURL(url);
      toast.success('Export téléchargé');
      setExportUserId('');
    },
    onError: (err) => toast.error(getErrorMessage(err)),
  });

  const filteredExports = exports.filter((e) =>
    !searchUser || e.id.includes(searchUser) || e.type.includes(searchUser)
  );

  const filteredGdpr = gdprRequests.filter((r) =>
    !searchUser || r.userId.includes(searchUser) || (r.userName || '').toLowerCase().includes(searchUser.toLowerCase())
  );

  const statusColor = (s: string) => {
    switch (s) {
      case 'COMPLETED': return 'badge-success';
      case 'PENDING': return 'badge-warning';
      case 'PROCESSING': return 'badge-info';
      case 'FAILED': return 'badge-error';
      default: return 'badge-gray';
    }
  };

  return (
    <div className="page-container max-w-5xl">
      <div className="page-header">
        <div className="animate-fade-in">
          <div className="flex items-center gap-2 mb-1">
            <Shield className="w-5 h-5 text-emerald-500" />
            <h1 className="page-title">Exports & Conformité RGPD</h1>
          </div>
          <p className="page-subtitle">Exports de données, demandes RGPD et suppression</p>
        </div>
      </div>

      {/* Tabs */}
      <div className="flex gap-2 mb-6">
        {[
          { key: 'exports' as const, label: 'Exports', icon: Download },
          { key: 'gdpr' as const, label: 'Demandes RGPD', icon: Shield },
        ].map((tab) => (
          <button
            key={tab.key}
            onClick={() => setActiveTab(tab.key)}
            className={`flex items-center gap-2 px-4 py-2 rounded-xl text-sm font-medium transition-all ${
              activeTab === tab.key
                ? 'bg-emerald-100 dark:bg-emerald-500/20 text-emerald-700 dark:text-emerald-400'
                : 'text-gray-500 dark:text-gray-400 hover:bg-white/5'
            }`}
          >
            <tab.icon className="w-4 h-4" />
            {tab.label}
          </button>
        ))}
      </div>

      {/* Exports tab */}
      {activeTab === 'exports' && (
        <div className="space-y-4 animate-slide-up">
          {/* Quick actions */}
          <div className="glass-card p-5">
            <h3 className="text-sm font-bold text-gray-900 dark:text-gray-100 mb-3">Actions rapides</h3>
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              <div>
                <label className="label">Exporter les données d'un utilisateur</label>
                <div className="flex gap-2">
                  <input
                    className="input flex-1"
                    placeholder="ID utilisateur"
                    value={exportUserId}
                    onChange={(e) => setExportUserId(e.target.value)}
                  />
                  <button
                    onClick={() => exportMutation.mutate()}
                    disabled={!exportUserId || exportMutation.isPending}
                    className="btn-primary btn-sm"
                  >
                    {exportMutation.isPending ? <Loader2 className="w-4 h-4 animate-spin" /> : <Download className="w-4 h-4" />}
                    Exporter
                  </button>
                </div>
              </div>
              <div>
                <label className="label">Demander la suppression (droit à l'oubli)</label>
                <div className="flex gap-2">
                  <input
                    className="input flex-1"
                    placeholder="ID utilisateur"
                    value={deleteUserId}
                    onChange={(e) => setDeleteUserId(e.target.value)}
                  />
                  <button
                    onClick={() => {
                      if (confirm('Supprimer toutes les données de cet utilisateur ?')) deleteMutation.mutate();
                    }}
                    disabled={!deleteUserId || deleteMutation.isPending}
                    className="btn-primary btn-sm bg-red-600 hover:bg-red-700"
                  >
                    {deleteMutation.isPending ? <Loader2 className="w-4 h-4 animate-spin" /> : <Trash2 className="w-4 h-4" />}
                    Supprimer
                  </button>
                </div>
              </div>
            </div>
          </div>

          {/* Exports list */}
          <div>
            <div className="flex items-center justify-between mb-3">
              <h3 className="text-sm font-bold text-gray-900 dark:text-gray-100">Historique des exports</h3>
              <button onClick={() => qc.invalidateQueries({ queryKey: ['compliance'] })} className="btn-ghost btn-sm">
                <Loader2 className="w-4 h-4" />
              </button>
            </div>

            {isLoading ? (
              <div className="p-8 text-center"><Loader2 className="w-6 h-6 animate-spin text-gray-400 mx-auto" /></div>
            ) : filteredExports.length === 0 ? (
              <div className="glass-card p-8 text-center">
                <Download className="w-8 h-8 text-gray-300 mb-2 mx-auto" />
                <p className="text-gray-500 text-sm">Aucun export.</p>
              </div>
            ) : (
              <div className="space-y-2">
                {filteredExports.map((exp) => (
                  <div key={exp.id} className="glass-card px-5 py-3 flex items-center gap-4">
                    <div className="w-9 h-9 rounded-lg bg-gradient-to-br from-emerald-500 to-teal-600 flex items-center justify-center text-white shadow-sm shrink-0">
                      <FileText className="w-4 h-4" />
                    </div>
                    <div className="flex-1 min-w-0">
                      <div className="flex items-center gap-2">
                        <span className="text-sm font-semibold text-gray-900 dark:text-gray-100">{exp.type}</span>
                        <span className={`badge text-[10px] ${statusColor(exp.status)}`}>{exp.status}</span>
                      </div>
                      <div className="flex items-center gap-3 text-[10px] text-gray-400 mt-0.5">
                        <span className="flex items-center gap-1">
                          <Clock className="w-3 h-3" />
                          {new Date(exp.requestedAt).toLocaleString('fr-FR')}
                        </span>
                        {exp.completedAt && <span>Traité le {new Date(exp.completedAt).toLocaleDateString('fr-FR')}</span>}
                      </div>
                    </div>
                    {exp.downloadUrl && (
                      <a href={exp.downloadUrl} target="_blank" rel="noopener noreferrer" className="btn-icon text-primary-500 hover:bg-primary-50 dark:hover:bg-primary-900/20">
                        <ExternalLink className="w-4 h-4" />
                      </a>
                    )}
                  </div>
                ))}
              </div>
            )}
          </div>
        </div>
      )}

      {/* GDPR tab */}
      {activeTab === 'gdpr' && (
        <div className="space-y-4 animate-slide-up">
          <div className="flex items-center justify-between">
            <h3 className="text-sm font-bold text-gray-900 dark:text-gray-100">Demandes RGPD</h3>
          </div>

          {gdprLoading ? (
            <div className="p-8 text-center"><Loader2 className="w-6 h-6 animate-spin text-gray-400 mx-auto" /></div>
          ) : filteredGdpr.length === 0 ? (
            <div className="glass-card p-8 text-center">
              <Shield className="w-8 h-8 text-gray-300 mb-2 mx-auto" />
              <p className="text-gray-500 text-sm">Aucune demande RGPD.</p>
            </div>
          ) : (
            <div className="space-y-2">
              {filteredGdpr.map((req) => (
                <div key={req.id} className="glass-card px-5 py-4">
                  <div className="flex items-start justify-between mb-2">
                    <div>
                      <div className="flex items-center gap-2">
                        <span className="text-sm font-semibold text-gray-900 dark:text-gray-100">{req.requestType}</span>
                        <span className={`badge text-[10px] ${statusColor(req.status)}`}>{req.status}</span>
                      </div>
                      <p className="text-[10px] text-gray-400 mt-0.5">
                        Utilisateur : {req.userName || req.userId}
                      </p>
                    </div>
                  </div>
                  {req.details && (
                    <p className="text-xs text-gray-500 dark:text-gray-400 mb-2">{req.details}</p>
                  )}
                  <div className="flex items-center gap-3 text-[10px] text-gray-400">
                    <span className="flex items-center gap-1">
                      <Clock className="w-3 h-3" />
                      {new Date(req.createdAt).toLocaleString('fr-FR')}
                    </span>
                    {req.processedAt && (
                      <span>Traité le {new Date(req.processedAt).toLocaleDateString('fr-FR')}</span>
                    )}
                  </div>
                  {req.status === 'PENDING' && (
                    <div className="mt-3">
                      <button
                        onClick={() => processMutation.mutate(req.id)}
                        disabled={processMutation.isPending}
                        className="btn-primary btn-sm"
                      >
                        {processMutation.isPending ? <Loader2 className="w-4 h-4 animate-spin" /> : <CheckCircle2 className="w-4 h-4" />}
                        Traiter
                      </button>
                    </div>
                  )}
                </div>
              ))}
            </div>
          )}
        </div>
      )}
    </div>
  );
}
