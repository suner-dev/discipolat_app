import { useQuery } from '@tanstack/react-query';
import api from '@/lib/api';
import { Server, Database, HardDrive, Activity, Clock, Loader2, AlertTriangle } from 'lucide-react';

interface SystemHealth {
  jvm: {
    heapUsed: string;
    heapMax: string;
    heapUsedPercent: number;
    nonHeapUsed: string;
    totalMemory: string;
    freeMemory: string;
    maxMemory: string;
  };
  database: {
    status: 'UP' | 'DOWN';
    error?: string;
    userCount?: number;
    soulCount?: number;
    familyCount?: number;
    auditCount?: number;
    tableCount?: number;
  };
  uptime: { milliseconds: number; formatted: string; startTime: string };
  thread: { peakCount: number; daemonCount: number; currentCount: number };
  processors: number;
}

/** Widget de santé technique du système (JVM, base de données, uptime) — réservé ADMIN. */
export default function SystemHealthWidget() {
  const { data, isLoading, isError } = useQuery({
    queryKey: ['admin', 'system-health'],
    queryFn: async () => {
      const res = await api.get('/admin/system-health');
      return res.data as SystemHealth;
    },
  });

  if (isLoading) {
    return (
      <div className="glass-card p-5 animate-fade-in">
        <div className="flex items-center justify-center gap-2 py-4 text-xs text-gray-400">
          <Loader2 className="w-4 h-4 animate-spin" /> Chargement de la santé système…
        </div>
      </div>
    );
  }

  if (isError || !data) {
    return (
      <div className="glass-card p-5 animate-fade-in">
        <div className="flex items-center gap-2 text-xs text-amber-600 dark:text-amber-400">
          <AlertTriangle className="w-4 h-4" />
          Santé système indisponible (endpoint ADMIN réservé au rôle administrateur).
        </div>
      </div>
    );
  }

  const dbUp = data.database.status === 'UP';
  const heapPercent = Math.min(100, Math.max(0, data.jvm.heapUsedPercent ?? 0));
  const heapBarColor = heapPercent < 60 ? 'bg-emerald-500' : heapPercent < 85 ? 'bg-amber-500' : 'bg-red-500';

  return (
    <div className="glass-card p-5 animate-fade-in">
      <div className="flex items-center gap-2 mb-4">
        <Server className="w-4 h-4 text-primary-500" />
        <h3 className="text-xs font-semibold text-gray-700 dark:text-gray-300 uppercase tracking-wider">
          Santé système
        </h3>
        <span className={`badge text-[9px] ${dbUp ? 'badge-success' : 'badge-warning'}`}>
          {dbUp ? 'Opérationnel' : 'Attention'}
        </span>
      </div>

      <div className="grid grid-cols-2 sm:grid-cols-2 lg:grid-cols-4 gap-3">
        {/* Base de données */}
        <div className="p-3 rounded-xl bg-gray-50/50 dark:bg-gray-800/30">
          <div className="flex items-center gap-1.5 mb-2">
            <Database className={`w-3.5 h-3.5 ${dbUp ? 'text-emerald-500' : 'text-red-500'}`} />
            <p className="text-[10px] text-gray-400 uppercase">Base de données</p>
          </div>
          <p className="text-sm font-semibold text-gray-900 dark:text-gray-100">{dbUp ? 'UP' : 'DOWN'}</p>
          <p className="text-[9px] text-gray-400 mt-1">
            {data.database.tableCount ?? '-'} tables · {data.database.userCount ?? '-'} users
          </p>
          {data.database.error && (
            <p className="text-[9px] text-red-500 mt-1 line-clamp-2">{data.database.error}</p>
          )}
        </div>

        {/* JVM / mémoire */}
        <div className="p-3 rounded-xl bg-gray-50/50 dark:bg-gray-800/30">
          <div className="flex items-center gap-1.5 mb-2">
            <HardDrive className="w-3.5 h-3.5 text-indigo-500" />
            <p className="text-[10px] text-gray-400 uppercase">Mémoire JVM</p>
          </div>
          <p className="text-sm font-semibold text-gray-900 dark:text-gray-100">
            {data.jvm.heapUsedPercent}%
          </p>
          <div className="h-1.5 rounded-full bg-gray-200 dark:bg-gray-700 mt-2 overflow-hidden">
            <div className={`h-full rounded-full transition-all ${heapBarColor}`} style={{ width: `${heapPercent}%` }} />
          </div>
          <p className="text-[9px] text-gray-400 mt-1">
            {data.jvm.heapUsed} / {data.jvm.heapMax}
          </p>
        </div>

        {/* Uptime */}
        <div className="p-3 rounded-xl bg-gray-50/50 dark:bg-gray-800/30">
          <div className="flex items-center gap-1.5 mb-2">
            <Clock className="w-3.5 h-3.5 text-amber-500" />
            <p className="text-[10px] text-gray-400 uppercase">Uptime</p>
          </div>
          <p className="text-sm font-semibold text-gray-900 dark:text-gray-100">{data.uptime.formatted}</p>
          <p className="text-[9px] text-gray-400 mt-1">
            Démarrage {new Date(data.uptime.startTime).toLocaleString('fr-FR')}
          </p>
        </div>

        {/* Threads & processeurs */}
        <div className="p-3 rounded-xl bg-gray-50/50 dark:bg-gray-800/30">
          <div className="flex items-center gap-1.5 mb-2">
            <Activity className="w-3.5 h-3.5 text-teal-500" />
            <p className="text-[10px] text-gray-400 uppercase">Runtime</p>
          </div>
          <p className="text-sm font-semibold text-gray-900 dark:text-gray-100">
            {data.thread.currentCount} threads
          </p>
          <p className="text-[9px] text-gray-400 mt-1">
            Pic {data.thread.peakCount} · {data.processors} CPU
          </p>
        </div>
      </div>
    </div>
  );
}