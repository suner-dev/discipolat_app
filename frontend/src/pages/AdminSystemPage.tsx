import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import toast from 'react-hot-toast';
import api from '@/lib/api';
import {
  Server, Cpu, Database, Zap, Clock, RefreshCw, HardDrive, Activity,
  Wifi, MemoryStick, Thermometer, Users, Heart, FileText, AlertTriangle,
  Shield, Loader2, Trash2, Settings, BarChart3, Layers,
} from 'lucide-react';
import { useState } from 'react';

/* ============================================================================
 * ADMIN SYSTEM PAGE — Paramètres système, santé, cache et performances
 * L'admin technique gère l'infrastructure sans toucher au code.
 * ========================================================================== */

interface SystemHealth {
  jvm: {
    heapUsed: number; heapMax: number; heapFree: number;
    nonHeapUsed: number; nonHeapMax: number;
    threads: number; threadPeak: number; threadDaemon: number;
    uptime: string; startTime: string;
  };
  database: {
    status: string;
    totalUsers: number; totalSouls: number; totalFamilies: number;
    totalAuditLogs: number; tableCount: number;
  };
  uptime: string;
}

interface CacheStats {
  caches: Record<string, {
    hits: number; misses: number; puts: number; evictions: number;
    ratio: string; health: string; averageLoadTime?: number;
  }>;
  summary: {
    totalHits: number; totalMisses: number; totalPuts: number;
    overallRatio: string; healthStatus: string;
    cacheCount: number;
  };
}

function formatBytes(bytes: number): string {
  if (bytes < 1024) return `${bytes} B`;
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} Ko`;
  if (bytes < 1024 * 1024 * 1024) return `${(bytes / (1024 * 1024)).toFixed(1)} Mo`;
  return `${(bytes / (1024 * 1024 * 1024)).toFixed(1)} Go`;
}

function StatCard({ icon: Icon, label, value, sub, color, trend }: {
  icon: typeof Cpu; label: string; value: string | number; sub?: string;
  color: string; trend?: 'up' | 'down' | 'neutral';
}) {
  return (
    <div className="glass-card p-4 animate-slide-up">
      <div className="flex items-start justify-between mb-2">
        <span className="text-[10px] font-semibold text-gray-500 dark:text-gray-400 uppercase tracking-wider">{label}</span>
        <div className={`p-1.5 rounded-lg bg-gradient-to-br ${color} text-white shadow-sm`}>
          <Icon className="w-3.5 h-3.5" />
        </div>
      </div>
      <p className="text-xl font-bold text-gray-900 dark:text-gray-100">{value}</p>
      {sub && <p className="text-[10px] text-gray-400 mt-0.5">{sub}</p>}
    </div>
  );
}

function ProgressBar({ value, max, color = 'primary' }: { value: number; max: number; color?: string }) {
  const pct = max > 0 ? Math.min((value / max) * 100, 100) : 0;
  const colorClass = pct > 85 ? 'bg-red-500' : pct > 60 ? 'bg-amber-500' : 'bg-green-500';
  return (
    <div className="w-full bg-gray-200 dark:bg-gray-700 rounded-full h-2">
      <div className={`h-2 rounded-full transition-all duration-500 ${colorClass}`} style={{ width: `${pct}%` }} />
    </div>
  );
}

export default function AdminSystemPage() {
  const queryClient = useQueryClient();
  const [activeTab, setActiveTab] = useState<'health' | 'cache' | 'database'>('health');

  const { data: health, isLoading: healthLoading, refetch: refetchHealth } = useQuery({
    queryKey: ['admin', 'system-health'],
    queryFn: async () => {
      const res = await api.get('/admin/system-health');
      return res.data as SystemHealth;
    },
    refetchInterval: 15000,
  });

  const { data: cache, isLoading: cacheLoading, refetch: refetchCache } = useQuery({
    queryKey: ['admin', 'cache-stats'],
    queryFn: async () => {
      const res = await api.get('/admin/cache-stats');
      return res.data as CacheStats;
    },
    refetchInterval: 15000,
  });

  const evictCache = useMutation({
    mutationFn: async (cacheName: string) => {
      await api.delete(`/admin/cache-stats/${cacheName}`);
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin', 'cache-stats'] });
      toast.success('Cache vidé');
    },
    onError: () => toast.error('Erreur lors du vidage du cache'),
  });

  const evictAll = useMutation({
    mutationFn: async () => {
      await api.delete('/admin/cache-stats');
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin', 'cache-stats'] });
      toast.success('Tous les caches vidés');
    },
    onError: () => toast.error('Erreur lors du vidage des caches'),
  });

  const tabs = [
    { key: 'health' as const, label: 'Santé', icon: Activity },
    { key: 'cache' as const, label: 'Cache', icon: Zap },
    { key: 'database' as const, label: 'Base de données', icon: Database },
  ];

  const isLoading = healthLoading || cacheLoading;

  if (isLoading) {
    return (
      <div className="min-h-[50vh] flex items-center justify-center">
        <div className="spinner h-8 w-8" />
      </div>
    );
  }

  return (
    <div className="page-container max-w-6xl">
      <div className="page-header">
        <div className="flex items-center gap-3">
          <div className="p-3 rounded-xl bg-gradient-to-br from-cyan-500 to-blue-600 text-white shadow-lg">
            <Server className="w-6 h-6" />
          </div>
          <div>
            <h1 className="page-title">Système</h1>
            <p className="page-subtitle">Santé technique, cache, performances et paramètres de la plateforme</p>
          </div>
        </div>
        <div className="page-header-actions">
          <button
            onClick={() => { refetchHealth(); refetchCache(); }}
            className="btn-secondary btn-sm"
          >
            <RefreshCw className="w-4 h-4" /> Actualiser
          </button>
        </div>
      </div>

      {/* Tabs */}
      <div className="tabs mb-6">
        {tabs.map((t) => (
          <button key={t.key} className={activeTab === t.key ? 'tab-active' : 'tab'} onClick={() => setActiveTab(t.key)}>
            <t.icon className="w-4 h-4" />
            {t.label}
          </button>
        ))}
      </div>

      {/* Health Tab */}
      {activeTab === 'health' && health && (
        <div className="space-y-6 animate-slide-up">
          {/* JVM Stats */}
          <section className="glass-card p-6">
            <div className="flex items-center gap-2 mb-4">
              <Cpu className="w-4 h-4 text-blue-500" />
              <h3 className="text-sm font-bold text-gray-900 dark:text-gray-100 uppercase tracking-wider">JVM (Java Virtual Machine)</h3>
            </div>
            <div className="grid grid-cols-2 sm:grid-cols-4 gap-4 mb-4">
              <div className="p-3 rounded-xl bg-blue-50/50 dark:bg-blue-900/10 border border-blue-200/40 dark:border-blue-700/30">
                <p className="text-[10px] text-blue-500 font-semibold uppercase">Heap utilisé</p>
                <p className="text-lg font-bold text-gray-900 dark:text-gray-100">{formatBytes(health.jvm.heapUsed)}</p>
                <ProgressBar value={health.jvm.heapUsed} max={health.jvm.heapMax} />
                <p className="text-[10px] text-gray-400 mt-1">/ {formatBytes(health.jvm.heapMax)}</p>
              </div>
              <div className="p-3 rounded-xl bg-green-50/50 dark:bg-green-900/10 border border-green-200/40 dark:border-green-700/30">
                <p className="text-[10px] text-green-500 font-semibold uppercase">Heap libre</p>
                <p className="text-lg font-bold text-gray-900 dark:text-gray-100">{formatBytes(health.jvm.heapFree)}</p>
              </div>
              <div className="p-3 rounded-xl bg-amber-50/50 dark:bg-amber-900/10 border border-amber-200/40 dark:border-amber-700/30">
                <p className="text-[10px] text-amber-500 font-semibold uppercase">Non-heap</p>
                <p className="text-lg font-bold text-gray-900 dark:text-gray-100">{formatBytes(health.jvm.nonHeapUsed)}</p>
              </div>
              <div className="p-3 rounded-xl bg-purple-50/50 dark:bg-purple-900/10 border border-purple-200/40 dark:border-purple-700/30">
                <p className="text-[10px] text-purple-500 font-semibold uppercase">Uptime</p>
                <p className="text-lg font-bold text-gray-900 dark:text-gray-100 truncate">{health.uptime || health.jvm.uptime}</p>
              </div>
            </div>
            <div className="grid grid-cols-3 gap-3">
              <div className="p-2.5 rounded-lg bg-gray-50/50 dark:bg-gray-800/30 text-center">
                <p className="text-[10px] text-gray-400">Threads actifs</p>
                <p className="text-sm font-bold text-gray-900 dark:text-gray-100">{health.jvm.threads}</p>
              </div>
              <div className="p-2.5 rounded-lg bg-gray-50/50 dark:bg-gray-800/30 text-center">
                <p className="text-[10px] text-gray-400">Peak threads</p>
                <p className="text-sm font-bold text-gray-900 dark:text-gray-100">{health.jvm.threadPeak}</p>
              </div>
              <div className="p-2.5 rounded-lg bg-gray-50/50 dark:bg-gray-800/30 text-center">
                <p className="text-[10px] text-gray-400">Daemon threads</p>
                <p className="text-sm font-bold text-gray-900 dark:text-gray-100">{health.jvm.threadDaemon}</p>
              </div>
            </div>
          </section>

          {/* System overview */}
          <section className="glass-card p-6">
            <div className="flex items-center gap-2 mb-4">
              <Thermometer className="w-4 h-4 text-emerald-500" />
              <h3 className="text-sm font-bold text-gray-900 dark:text-gray-100 uppercase tracking-wider">Vue d'ensemble</h3>
            </div>
            <div className="grid grid-cols-2 sm:grid-cols-4 gap-3">
              {[
                { icon: Users, label: 'Utilisateurs', value: health.database.totalUsers, color: 'from-blue-500 to-indigo-500' },
                { icon: Heart, label: 'Âmes', value: health.database.totalSouls, color: 'from-rose-500 to-pink-500' },
                { icon: Users, label: 'Familles', value: health.database.totalFamilies, color: 'from-emerald-500 to-teal-500' },
                { icon: Shield, label: 'Logs audit', value: health.database.totalAuditLogs, color: 'from-violet-500 to-purple-500' },
              ].map((s) => (
                <div key={s.label} className="p-3 rounded-xl bg-gradient-to-br from-gray-50 to-white dark:from-gray-800/50 dark:to-gray-800/30 border border-gray-100 dark:border-gray-700/50">
                  <div className="flex items-center gap-2 mb-1">
                    <div className={`p-1 rounded bg-gradient-to-br ${s.color} text-white`}>
                      <s.icon className="w-3 h-3" />
                    </div>
                    <span className="text-[10px] text-gray-400">{s.label}</span>
                  </div>
                  <p className="text-lg font-bold text-gray-900 dark:text-gray-100">{s.value.toLocaleString()}</p>
                </div>
              ))}
            </div>
          </section>
        </div>
      )}

      {/* Cache Tab */}
      {activeTab === 'cache' && cache && (
        <div className="space-y-6 animate-slide-up">
          {/* Summary */}
          <section className="glass-card p-6">
            <div className="flex items-center justify-between mb-4">
              <div className="flex items-center gap-2">
                <Zap className="w-4 h-4 text-amber-500" />
                <h3 className="text-sm font-bold text-gray-900 dark:text-gray-100 uppercase tracking-wider">Résumé global</h3>
              </div>
              <button
                onClick={() => evictAll.mutate()}
                disabled={evictAll.isPending}
                className="btn-danger btn-sm"
              >
                {evictAll.isPending ? <Loader2 className="w-4 h-4 animate-spin" /> : <Trash2 className="w-4 h-4" />}
                Vider tout
              </button>
            </div>
            <div className="grid grid-cols-2 sm:grid-cols-4 gap-3">
              <div className="p-3 rounded-xl bg-green-50/50 dark:bg-green-900/10 border border-green-200/40 dark:border-green-700/30 text-center">
                <p className="text-[10px] text-green-500 font-semibold uppercase">Hits</p>
                <p className="text-lg font-bold text-green-700 dark:text-green-400">{cache.summary.totalHits.toLocaleString()}</p>
              </div>
              <div className="p-3 rounded-xl bg-red-50/50 dark:bg-red-900/10 border border-red-200/40 dark:border-red-700/30 text-center">
                <p className="text-[10px] text-red-500 font-semibold uppercase">Misses</p>
                <p className="text-lg font-bold text-red-700 dark:text-red-400">{cache.summary.totalMisses.toLocaleString()}</p>
              </div>
              <div className="p-3 rounded-xl bg-blue-50/50 dark:bg-blue-900/10 border border-blue-200/40 dark:border-blue-700/30 text-center">
                <p className="text-[10px] text-blue-500 font-semibold uppercase">Ratio</p>
                <p className="text-lg font-bold text-blue-700 dark:text-blue-400">{cache.summary.overallRatio}</p>
              </div>
              <div className="p-3 rounded-xl bg-purple-50/50 dark:bg-purple-900/10 border border-purple-200/40 dark:border-purple-700/30 text-center">
                <p className="text-[10px] text-purple-500 font-semibold uppercase">Caches</p>
                <p className="text-lg font-bold text-purple-700 dark:text-purple-400">{cache.summary.cacheCount}</p>
              </div>
            </div>
          </section>

          {/* Per-cache detail */}
          <section className="glass-card overflow-hidden">
            <div className="card-header">
              <h3 className="text-sm font-bold text-gray-900 dark:text-gray-100 uppercase tracking-wider">Caches individuels</h3>
            </div>
            <div className="divide-y divide-gray-100/50 dark:divide-gray-800/30">
              {Object.entries(cache.caches).map(([name, stats]) => (
                <div key={name} className="px-5 py-4 flex items-center gap-4">
                  <div className={`w-9 h-9 rounded-xl flex items-center justify-center flex-shrink-0 ${stats.health === 'UP' ? 'bg-green-100 dark:bg-green-900/30 text-green-600' : 'bg-red-100 dark:bg-red-900/30 text-red-600'}`}>
                    <Layers className="w-4 h-4" />
                  </div>
                  <div className="flex-1 min-w-0">
                    <p className="text-sm font-semibold text-gray-900 dark:text-gray-100 truncate">{name}</p>
                    <div className="flex items-center gap-4 mt-1">
                      <span className="text-[10px] text-green-600 dark:text-green-400">Hits: {stats.hits.toLocaleString()}</span>
                      <span className="text-[10px] text-red-600 dark:text-red-400">Misses: {stats.misses.toLocaleString()}</span>
                      <span className="text-[10px] text-blue-600 dark:text-blue-400">Ratio: {stats.ratio}</span>
                      {stats.averageLoadTime !== undefined && (
                        <span className="text-[10px] text-amber-600 dark:text-amber-400">Avg: {stats.averageLoadTime.toFixed(0)}ms</span>
                      )}
                    </div>
                  </div>
                  <span className={`badge text-[10px] ${stats.health === 'UP' ? 'badge-success' : 'badge-danger'}`}>
                    {stats.health === 'UP' ? 'Actif' : 'Erreur'}
                  </span>
                  <button
                    onClick={() => evictCache.mutate(name)}
                    className="btn-ghost btn-sm text-red-500 hover:text-red-700"
                    title="Vider ce cache"
                  >
                    <Trash2 className="w-3.5 h-3.5" />
                  </button>
                </div>
              ))}
            </div>
          </section>
        </div>
      )}

      {/* Database Tab */}
      {activeTab === 'database' && health && (
        <div className="space-y-6 animate-slide-up">
          <section className="glass-card p-6">
            <div className="flex items-center gap-2 mb-4">
              <Database className="w-4 h-4 text-emerald-500" />
              <h3 className="text-sm font-bold text-gray-900 dark:text-gray-100 uppercase tracking-wider">Statistiques de la base</h3>
            </div>
            <div className="grid grid-cols-2 sm:grid-cols-3 gap-4">
              <div className="p-4 rounded-xl bg-gradient-to-br from-emerald-50 to-teal-50 dark:from-emerald-900/20 dark:to-teal-900/10 border border-emerald-200/40 dark:border-emerald-700/30">
                <div className="flex items-center gap-2 mb-2">
                  <div className="w-2 h-2 rounded-full bg-green-500 shadow-[0_0_6px_rgba(34,197,94,0.5)]" />
                  <span className="text-[10px] font-semibold text-emerald-700 dark:text-emerald-300 uppercase">Statut</span>
                </div>
                <p className="text-lg font-bold text-green-600 dark:text-green-400">{health.database.status}</p>
              </div>
              <div className="p-4 rounded-xl bg-blue-50/50 dark:bg-blue-900/10 border border-blue-200/40 dark:border-blue-700/30">
                <p className="text-[10px] text-blue-500 font-semibold uppercase mb-1">Tables</p>
                <p className="text-lg font-bold text-gray-900 dark:text-gray-100">{health.database.tableCount}</p>
              </div>
              <div className="p-4 rounded-xl bg-amber-50/50 dark:bg-amber-900/10 border border-amber-200/40 dark:border-amber-700/30">
                <p className="text-[10px] text-amber-500 font-semibold uppercase mb-1">Logs d'audit</p>
                <p className="text-lg font-bold text-gray-900 dark:text-gray-100">{health.database.totalAuditLogs.toLocaleString()}</p>
              </div>
            </div>
          </section>
        </div>
      )}
    </div>
  );
}
