import { formatEnum } from '@/lib/labels';
import React, { useState, useEffect } from 'react';
import { useI18n } from '@/i18n';
import api from '@/lib/api';
import SkeletonLoader from '@/components/shared/SkeletonLoader';
import EmptyState from '@/components/shared/EmptyState';
import Toast from '@/components/shared/Toast';
import { CalendarDays, Plus, AlertTriangle, Users } from 'lucide-react';

interface Assignment {
  id: string;
  équipeId: string;
  rôle: string;
  membreId?: string;
  début: string;
  fin: string;
  statut: string;
  notes?: string;
}

export default function TeamGanttPage() {
  const { t } = useI18n();
  const [assignments, setAssignments] = useState<Assignment[]>([]);
  const [loading, setLoading] = useState(true);
  const [start, setStart] = useState(() => { const d = new Date(); d.setDate(1); return d.toISOString().slice(0, 10); });
  const [end, setEnd] = useState(() => { const d = new Date(); d.setMonth(d.getMonth() + 1); d.setDate(0); return d.toISOString().slice(0, 10); });

  useEffect(() => { loadAssignments(); }, [start, end]);

  const loadAssignments = async () => {
    try {
      setLoading(true);
      const res = await api.get(`/team-gantt?start=${start}T00:00:00&end=${end}T23:59:59`);
      setAssignments(res.data || []);
    } catch { setAssignments([]); }
    finally { setLoading(false); }
  };

  const getStatutColor = (s: string) => {
    switch (s) {
      case 'EN_COURS': return 'bg-green-100 text-green-700';
      case 'TERMINÉE': return 'bg-gray-100 text-gray-500';
      case 'ANNULÉE': return 'bg-red-100 text-red-700';
      default: return 'bg-blue-100 text-blue-700';
    }
  };

  return (
    <div className="min-h-screen p-4 md:p-8 max-w-7xl mx-auto">
      <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4 mb-8">
        <div>
          <h1 className="text-2xl md:text-3xl font-bold text-gray-900 dark:text-white flex items-center gap-3">
            <CalendarDays className="w-8 h-8 text-indigo-500" />
            {t('gantt.title')}
          </h1>
          <p className="text-gray-500 dark:text-gray-400 mt-1">Planification des affectations d'équipe</p>
        </div>
      </div>

      {/* Date range filter */}
      <div className="flex gap-4 mb-6">
        <div>
          <label className="block text-xs text-gray-500 mb-1">Du</label>
          <input type="date" value={start} onChange={e => setStart(e.target.value)}
            className="px-4 py-2 rounded-xl border border-gray-200 dark:border-white/10 bg-white dark:bg-white/5 text-sm" />
        </div>
        <div>
          <label className="block text-xs text-gray-500 mb-1">Au</label>
          <input type="date" value={end} onChange={e => setEnd(e.target.value)}
            className="px-4 py-2 rounded-xl border border-gray-200 dark:border-white/10 bg-white dark:bg-white/5 text-sm" />
        </div>
      </div>

      {loading ? <SkeletonLoader lines={6} variant="table" /> :
        assignments.length === 0 ? (
          <EmptyState icon={<CalendarDays className="w-8 h-8 text-gray-400" />}
            title="Aucune affectation"
            message="Planifiez des affectations d'équipe pour cette période" />
        ) : (
          <div className="space-y-3">
            {/* Gantt-like timeline */}
            <div className="bg-white dark:bg-white/5 rounded-xl border border-gray-200 dark:border-white/10 p-4 overflow-x-auto">
              <div className="min-w-[600px]">
                {/* Header with dates */}
                <div className="flex gap-1 mb-2 text-xs text-gray-400">
                  {Array.from({ length: 7 }).map((_, i) => {
                    const d = new Date(start);
                    d.setDate(d.getDate() + i);
                    return <div key={i} className="flex-1 text-center">{d.toLocaleDateString('fr-FR', { weekday: 'short', day: 'numeric' })}</div>;
                  })}
                </div>
                {/* Assignments as bars */}
                {assignments.map(a => (
                  <div key={a.id} className="flex items-center gap-2 mb-2">
                    <div className="w-32 text-xs text-gray-600 dark:text-gray-400 truncate">{a.rôle}</div>
                    <div className="flex-1 relative h-8 bg-gray-50 dark:bg-white/5 rounded-lg">
                      <div className={`absolute top-1 bottom-1 rounded-md ${getStatutColor(a.statut)} flex items-center px-2`}
                        style={{ left: '10%', width: '30%' }}>
                        <span className="text-xs font-medium truncate">{a.rôle}</span>
                      </div>
                    </div>
                    <span className={`px-2 py-0.5 rounded-full text-xs font-medium ${getStatutColor(a.statut)}`}>{formatEnum(a.statut)}</span>
                  </div>
                ))}
              </div>
            </div>

            {/* List view */}
            <div className="grid gap-3 md:grid-cols-2">
              {assignments.map(a => (
                <div key={a.id} className="bg-white dark:bg-white/5 rounded-xl p-4 border border-gray-200 dark:border-white/10">
                  <div className="flex items-center justify-between mb-2">
                    <h3 className="font-medium text-gray-900 dark:text-white text-sm">{a.rôle}</h3>
                    <span className={`px-2 py-0.5 rounded-full text-xs font-medium ${getStatutColor(a.statut)}`}>{formatEnum(a.statut)}</span>
                  </div>
                  <div className="text-xs text-gray-500 space-y-1">
                    <div>📅 {new Date(a.début).toLocaleDateString('fr-FR')} → {new Date(a.fin).toLocaleDateString('fr-FR')}</div>
                    {a.membreId && <div>👤 {a.membreId.slice(0, 8)}...</div>}
                    {a.notes && <div>📝 {a.notes}</div>}
                  </div>
                </div>
              ))}
            </div>
          </div>
        )}
    </div>
  );
}
