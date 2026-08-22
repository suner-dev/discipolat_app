import React, { useState, useEffect } from 'react';
import { useI18n } from '@/i18n';
import api from '@/lib/api';
import SkeletonLoader from '@/components/shared/SkeletonLoader';
import EmptyState from '@/components/shared/EmptyState';
import { GanttChart, ChevronLeft, ChevronRight, Users } from 'lucide-react';

interface GanttTask {
  id: string;
  titre: string;
  dateDebut: string;
  dateFin: string;
  statut: 'A_FAIRE' | 'EN_COURS' | 'TERMINE';
  assigneA?: string;
  assigneNom?: string;
  departmentId: string;
  departmentNom: string;
  color: string;
}

const STATUS_COLORS = {
  A_FAIRE: 'bg-gray-300',
  EN_COURS: 'bg-blue-500',
  TERMINE: 'bg-green-500',
};

const DEPT_COLORS = [
  'bg-blue-500', 'bg-purple-500', 'bg-green-500', 'bg-amber-500',
  'bg-red-500', 'bg-pink-500', 'bg-indigo-500', 'bg-teal-500',
];

export default function TeamGanttPage() {
  const { t } = useI18n();
  const [tasks, setTasks] = useState<GanttTask[]>([]);
  const [loading, setLoading] = useState(true);
  const [weekOffset, setWeekOffset] = useState(0);

  useEffect(() => { loadTasks(); }, [weekOffset]);

  const loadTasks = async () => {
    try {
      setLoading(true);
      const startOfWeek = getWeekStart(weekOffset);
      const endOfWeek = getWeekEnd(weekOffset);
      const res = await api.get(`/departments/tasks/gantt?start=${startOfWeek}&end=${endOfWeek}`);
      setTasks(res.data.content || res.data || []);
    } catch {
      setTasks([]);
    } finally {
      setLoading(false);
    }
  };

  const getWeekStart = (offset: number) => {
    const d = new Date();
    d.setDate(d.getDate() - d.getDay() + 1 + offset * 7);
    return d.toISOString().split('T')[0];
  };

  const getWeekEnd = (offset: number) => {
    const d = new Date();
    d.setDate(d.getDate() - d.getDay() + 7 + offset * 7);
    return d.toISOString().split('T')[0];
  };

  const days = Array.from({ length: 7 }, (_, i) => {
    const d = new Date();
    d.setDate(d.getDate() - d.getDay() + 1 + weekOffset * 7 + i);
    return d;
  });

  const getDayIndex = (dateStr: string) => {
    const d = new Date(dateStr);
    const start = new Date(getWeekStart(weekOffset));
    return Math.floor((d.getTime() - start.getTime()) / (1000 * 60 * 60 * 24));
  };

  const getTaskSpan = (task: GanttTask) => {
    const startIdx = Math.max(0, getDayIndex(task.dateDebut));
    const endIdx = Math.min(6, getDayIndex(task.dateFin));
    return { start: startIdx, span: Math.max(1, endIdx - startIdx + 1) };
  };

  // Group tasks by department
  const tasksByDept = new Map<string, GanttTask[]>();
  tasks.forEach(task => {
    const dept = task.departmentNom || 'Non assigné';
    if (!tasksByDept.has(dept)) tasksByDept.set(dept, []);
    tasksByDept.get(dept)!.push(task);
  });

  let colorIdx = 0;

  return (
    <div className="min-h-screen p-4 md:p-8 max-w-7xl mx-auto">
      <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4 mb-8">
        <div>
          <h1 className="text-2xl md:text-3xl font-bold text-gray-900 dark:text-white flex items-center gap-3">
            <GanttChart className="w-8 h-8 text-indigo-500" />
            Planning des équipes
          </h1>
          <p className="text-gray-500 dark:text-gray-400 mt-1">
            Vue Gantt des tâches par département
          </p>
        </div>
        <div className="flex items-center gap-3">
          <button
            onClick={() => setWeekOffset(weekOffset - 1)}
            className="p-2 rounded-xl border border-gray-200 dark:border-white/10 hover:bg-gray-50 dark:hover:bg-white/5 transition-all"
          >
            <ChevronLeft className="w-4 h-4 text-gray-600 dark:text-gray-400" />
          </button>
          <span className="text-sm font-medium text-gray-700 dark:text-gray-300 min-w-[180px] text-center">
            {new Date(getWeekStart(weekOffset)).toLocaleDateString('fr-FR', { day: 'numeric', month: 'long' })}
            {' → '}
            {new Date(getWeekEnd(weekOffset)).toLocaleDateString('fr-FR', { day: 'numeric', month: 'long' })}
          </span>
          <button
            onClick={() => setWeekOffset(weekOffset + 1)}
            className="p-2 rounded-xl border border-gray-200 dark:border-white/10 hover:bg-gray-50 dark:hover:bg-white/5 transition-all"
          >
            <ChevronRight className="w-4 h-4 text-gray-600 dark:text-gray-400" />
          </button>
          {weekOffset !== 0 && (
            <button
              onClick={() => setWeekOffset(0)}
              className="px-3 py-1.5 rounded-lg bg-indigo-100 dark:bg-indigo-500/20 text-indigo-700 dark:text-indigo-400 text-xs font-medium hover:bg-indigo-200 dark:hover:bg-indigo-500/30 transition-all"
            >
              Aujourd'hui
            </button>
          )}
        </div>
      </div>

      {loading ? (
        <SkeletonLoader lines={6} variant="table" />
      ) : tasks.length === 0 ? (
        <EmptyState
          icon={<GanttChart className="w-8 h-8 text-gray-400" />}
          title="Aucune tâche planifiée"
          message="Les tâches assignées aux départements apparaîtront ici"
        />
      ) : (
        <div className="overflow-x-auto rounded-2xl border border-gray-200 dark:border-white/10 bg-white dark:bg-white/5">
          {/* Day headers */}
          <div className="flex border-b border-gray-200 dark:border-white/10 sticky top-0 bg-white dark:bg-gray-800 z-10">
            <div className="w-48 shrink-0 px-4 py-3 text-sm font-semibold text-gray-700 dark:text-gray-300 border-r border-gray-200 dark:border-white/10">
              Département / Tâche
            </div>
            <div className="flex-1 flex">
              {days.map((day, i) => {
                const isToday = day.toDateString() === new Date().toDateString();
                return (
                  <div
                    key={i}
                    className={`flex-1 px-2 py-3 text-center text-xs font-medium border-r border-gray-100 dark:border-white/5 last:border-r-0 ${
                      isToday ? 'bg-indigo-50 dark:bg-indigo-500/10 text-indigo-700 dark:text-indigo-400' : 'text-gray-500 dark:text-gray-400'
                    }`}
                  >
                    <div>{day.toLocaleDateString('fr-FR', { weekday: 'short' })}</div>
                    <div className="text-lg font-bold">{day.getDate()}</div>
                  </div>
                );
              })}
            </div>
          </div>

          {/* Task rows grouped by department */}
          {Array.from(tasksByDept.entries()).map(([dept, deptTasks]) => {
            const deptColor = DEPT_COLORS[colorIdx++ % DEPT_COLORS.length];
            return (
              <React.Fragment key={dept}>
                {/* Department header */}
                <div className="flex border-b border-gray-100 dark:border-white/5 bg-gray-50 dark:bg-white/2">
                  <div className="w-48 shrink-0 px-4 py-2 flex items-center gap-2">
                    <Users className="w-4 h-4 text-gray-400" />
                    <span className="text-sm font-semibold text-gray-700 dark:text-gray-300">{dept}</span>
                  </div>
                  <div className="flex-1" />
                </div>
                {/* Tasks */}
                {deptTasks.map(task => {
                  const { start, span } = getTaskSpan(task);
                  return (
                    <div key={task.id} className="flex border-b border-gray-100 dark:border-white/5 hover:bg-gray-50 dark:hover:bg-white/2 transition-colors">
                      <div className="w-48 shrink-0 px-4 py-2 flex items-center">
                        <div>
                          <div className="text-sm text-gray-900 dark:text-white">{task.titre}</div>
                          {task.assigneNom && (
                            <div className="text-xs text-gray-400">{task.assigneNom}</div>
                          )}
                        </div>
                      </div>
                      <div className="flex-1 flex items-center relative py-1">
                        {days.map((_, i) => (
                          <div key={i} className="flex-1 border-r border-gray-50 dark:border-white/2 last:border-r-0" />
                        ))}
                        <div
                          className={`absolute h-6 rounded-lg ${STATUS_COLORS[task.statut]} opacity-80 flex items-center justify-center px-2`}
                          style={{
                            left: `${(start / 7) * 100}%`,
                            width: `${(span / 7) * 100}%`,
                          }}
                        >
                          <span className="text-xs text-white font-medium truncate">{task.titre}</span>
                        </div>
                      </div>
                    </div>
                  );
                })}
              </React.Fragment>
            );
          })}

          {/* Legend */}
          <div className="flex items-center gap-4 px-4 py-3 border-t border-gray-200 dark:border-white/10">
            <span className="text-xs text-gray-500">Légende :</span>
            {Object.entries(STATUS_COLORS).map(([key, color]) => (
              <div key={key} className="flex items-center gap-1">
                <div className={`w-3 h-3 rounded ${color}`} />
                <span className="text-xs text-gray-500">
                  {key === 'A_FAIRE' ? 'À faire' : key === 'EN_COURS' ? 'En cours' : 'Terminé'}
                </span>
              </div>
            ))}
          </div>
        </div>
      )}
    </div>
  );
}
