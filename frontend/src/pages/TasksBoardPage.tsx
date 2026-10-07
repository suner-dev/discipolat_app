import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { KanbanSquare, ListTodo, AlarmClock, BarChart3, Loader2, Plus, Trash2, ArrowRight } from 'lucide-react';
import toast from 'react-hot-toast';
import { getErrorMessage } from '@/lib/api';
import { tText } from '@/i18n';
import * as svc from '@/services/tasksService';

/**
 * Page Tâches — consomme le contrat exact V234
 * (`services/tasksService.ts`, monture `/api/v1/tasks`).
 * Clés de body uniquement celles lues par TaskService.createTask
 * (title, description, type, priority, assignedToId, dueDate — assignedToId
 * optionné : requireTenantUser tolère null) ; statuts = enum TaskStatus.
 */
type Row = Record<string, unknown>;
const sv = (r: Row, k: string): string => {
  const v = r[k];
  return v == null || v === '' ? '—' : String(v);
};

const STATUSES = ['BACKLOG', 'TODO', 'IN_PROGRESS', 'IN_REVIEW', 'BLOCKED', 'DONE', 'CANCELLED'];
const PRIORITIES = ['LOW', 'MEDIUM', 'HIGH', 'URGENT', 'CRITICAL'];
const TYPES = ['TASK', 'EPIC', 'STORY', 'BUG', 'FEATURE', 'CHORES', 'MEETING', 'CALL', 'REVIEW'];
const TABS = ['list', 'kanban', 'overdue', 'reports'] as const;
type Tab = (typeof TABS)[number];

const priorityColor: Record<string, string> = {
  LOW: 'bg-gray-100 text-gray-600',
  MEDIUM: 'bg-blue-100 text-blue-700',
  HIGH: 'bg-amber-100 text-amber-700',
  URGENT: 'bg-orange-100 text-orange-700',
  CRITICAL: 'bg-red-100 text-red-700',
};

export default function TasksBoardPage() {
  const qc = useQueryClient();
  const [tab, setTab] = useState<Tab>('list');
  const [showForm, setShowForm] = useState(false);
  const [search, setSearch] = useState('');
  const [statusFilter, setStatusFilter] = useState('');
  const [form, setForm] = useState({ title: '', description: '', type: 'TASK', priority: 'MEDIUM', assignedToId: '', dueDate: '' });

  const { data: tasks = [], isLoading } = useQuery({
    queryKey: ['tasks', 'list', search, statusFilter],
    queryFn: () => svc.listTasks({
      search: search || undefined,
      status: statusFilter || undefined,
      size: 50,
    }),
  });

  const { data: kanbanTasks = [], isLoading: loadingKanban } = useQuery({
    queryKey: ['tasks', 'kanban'],
    queryFn: () => svc.listTasks({ size: 200 }),
    enabled: tab === 'kanban',
  });
  const { data: columns = [] } = useQuery({
    queryKey: ['tasks', 'columns'],
    queryFn: () => svc.listKanbanColumns(),
    enabled: tab === 'kanban',
  });

  const { data: overdue = [], isLoading: loadingOverdue } = useQuery({
    queryKey: ['tasks', 'overdue'],
    queryFn: () => svc.listOverdue(),
    enabled: tab === 'overdue',
  });

  const { data: stats } = useQuery({
    queryKey: ['tasks', 'stats'],
    queryFn: () => svc.reportStatistics(),
    enabled: tab === 'reports',
  });
  const { data: byStatus = [] } = useQuery({
    queryKey: ['tasks', 'by-status'],
    queryFn: () => svc.reportByStatus(),
    enabled: tab === 'reports',
  });
  const { data: byAssignee = [] } = useQuery({
    queryKey: ['tasks', 'by-assignee'],
    queryFn: () => svc.reportByAssignee(),
    enabled: tab === 'reports',
  });

  const invalidate = () => qc.invalidateQueries({ queryKey: ['tasks'] });
  const onErr = (e: unknown) => toast.error(getErrorMessage(e));

  const createTask = useMutation({
    mutationFn: () =>
      svc.createTask({
        title: form.title,
        description: form.description || undefined,
        type: form.type,
        priority: form.priority,
        assignedToId: form.assignedToId.trim() || undefined,
        dueDate: form.dueDate ? new Date(`${form.dueDate}T00:00:00Z`).toISOString() : undefined,
      }),
    onSuccess: () => {
      toast.success(tText('Tâche créée'));
      setShowForm(false);
      setForm({ title: '', description: '', type: 'TASK', priority: 'MEDIUM', assignedToId: '', dueDate: '' });
      invalidate();
    },
    onError: onErr,
  });

  const changeStatus = useMutation({
    mutationFn: ({ id, status }: { id: number; status: string }) => svc.updateTaskStatus(id, status),
    onSuccess: () => {
      toast.success(tText('Statut mis à jour'));
      invalidate();
    },
    onError: onErr,
  });

  const moveTask = useMutation({
    mutationFn: ({ id, status }: { id: number; status: string }) => svc.reorderTask(id, status, null),
    onSuccess: () => invalidate(),
    onError: onErr,
  });

  const archiveTask = useMutation({
    mutationFn: (id: number) => svc.deleteTask(id),
    onSuccess: () => {
      toast.success(tText('Tâche archivée (statut CANCELLED)'));
      invalidate();
    },
    onError: onErr,
  });

  const spinner = <div className="flex justify-center py-12"><Loader2 className="w-8 h-8 animate-spin text-primary-500" /></div>;

  const TaskRow = ({ t }: { t: Row }) => (
    <div key={String(t.id)} className="glass-card p-4 flex flex-wrap items-center gap-x-4 gap-y-1">
      <span className="font-medium text-sm text-gray-800 dark:text-gray-200">{sv(t, 'title')}</span>
      <span className={`text-xs px-2 py-0.5 rounded-full ${priorityColor[sv(t, 'priority')] || 'bg-gray-100 text-gray-600'}`}>{sv(t, 'priority')}</span>
      <span className="text-xs text-gray-500">{sv(t, 'type')}</span>
      <span className="text-xs text-gray-500">{tText('Échéance')} : {sv(t, 'dueDate')}</span>
      <select
        className="ml-auto text-xs input px-2 py-1"
        value={sv(t, 'status')}
        onChange={(e) => changeStatus.mutate({ id: Number(t.id), status: e.target.value })}
      >
        {STATUSES.map((s) => <option key={s} value={s}>{s}</option>)}
      </select>
      <button onClick={() => archiveTask.mutate(Number(t.id))} className="text-red-400 hover:text-red-300"><Trash2 className="w-4 h-4" /></button>
    </div>
  );

  return (
    <div className="page-container">
      <div className="page-header">
        <div className="p-3 rounded-xl bg-gradient-to-br from-sky-500 to-blue-600 text-white shadow-lg"><KanbanSquare className="w-6 h-6" /></div>
        <div><h1 className="page-title">{tText('Tâches')}</h1><p className="page-subtitle">{tText('Liste, kanban, retards et rapports')}</p></div>
        {tab === 'list' && (
          <button onClick={() => setShowForm(!showForm)} className="btn-primary btn-sm ml-auto inline-flex items-center gap-1"><Plus className="w-4 h-4" /> {tText('Nouvelle tâche')}</button>
        )}
      </div>

      <div className="flex flex-wrap gap-2 mb-6">
        {TABS.map((t) => (
          <button key={t} onClick={() => { setTab(t); setShowForm(false); }} className={`btn-sm px-4 py-2 rounded-lg ${tab === t ? 'btn-primary' : 'glass-card'}`}>{tText(t)}</button>
        ))}
      </div>

      {tab === 'list' && (
        <>
          {showForm && (
            <div className="glass-card p-6 mb-6 grid gap-4 md:grid-cols-2">
              <div><label className="text-xs font-medium text-gray-500 mb-1 block">{tText('Titre *')}</label><input className="input w-full" value={form.title} onChange={(e) => setForm({ ...form, title: e.target.value })} /></div>
              <div><label className="text-xs font-medium text-gray-500 mb-1 block">{tText('Description')}</label><input className="input w-full" value={form.description} onChange={(e) => setForm({ ...form, description: e.target.value })} /></div>
              <div><label className="text-xs font-medium text-gray-500 mb-1 block">{tText('Type')}</label>
                <select className="input w-full" value={form.type} onChange={(e) => setForm({ ...form, type: e.target.value })}>{TYPES.map((ty) => <option key={ty} value={ty}>{ty}</option>)}</select>
              </div>
              <div><label className="text-xs font-medium text-gray-500 mb-1 block">{tText('Priorité')}</label>
                <select className="input w-full" value={form.priority} onChange={(e) => setForm({ ...form, priority: e.target.value })}>{PRIORITIES.map((p) => <option key={p} value={p}>{p}</option>)}</select>
              </div>
              <div><label className="text-xs font-medium text-gray-500 mb-1 block">{tText('Assigné à (UUID, optionnel)')}</label><input className="input w-full" value={form.assignedToId} onChange={(e) => setForm({ ...form, assignedToId: e.target.value })} /></div>
              <div><label className="text-xs font-medium text-gray-500 mb-1 block">{tText('Échéance')}</label><input type="date" className="input w-full" value={form.dueDate} onChange={(e) => setForm({ ...form, dueDate: e.target.value })} /></div>
              <div className="md:col-span-2 flex justify-end gap-3">
                <button onClick={() => setShowForm(false)} className="btn-sm px-4 py-2 rounded-lg glass-card">{tText('Annuler')}</button>
                <button onClick={() => createTask.mutate()} disabled={!form.title.trim() || createTask.isPending} className="btn-primary btn-sm">{tText('Créer')}</button>
              </div>
            </div>
          )}
          <div className="flex flex-wrap gap-3 mb-4">
            <input className="input md:w-64" placeholder={tText('Rechercher…')} value={search} onChange={(e) => setSearch(e.target.value)} />
            <select className="input md:w-48" value={statusFilter} onChange={(e) => setStatusFilter(e.target.value)}>
              <option value="">{tText('Tous statuts')}</option>
              {STATUSES.map((s) => <option key={s} value={s}>{s}</option>)}
            </select>
          </div>
          {isLoading ? spinner : tasks.length === 0 ? <div className="glass-card p-10 text-center text-gray-500">{tText('Aucune tâche')}</div> : (
            <div className="space-y-3">{tasks.map((t) => <TaskRow key={String(t.id)} t={t} />)}</div>
          )}
        </>
      )}

      {tab === 'kanban' && (
        loadingKanban ? spinner : (
          <div className="grid grid-cols-1 md:grid-cols-3 lg:grid-cols-4 gap-4 items-start">
            {(columns.length > 0 ? columns : STATUSES.filter((s) => s !== 'CANCELLED').map((s) => ({ status: s, name: s }))).map((c) => {
              const colStatus = sv(c, 'status');
              const inCol = kanbanTasks.filter((t) => sv(t, 'status') === colStatus);
              return (
                <div key={colStatus} className="glass-card p-3">
                  <h3 className="text-sm font-semibold text-gray-700 dark:text-gray-300 mb-2">{sv(c, 'name')} <span className="text-xs text-gray-400">({inCol.length})</span></h3>
                  <div className="space-y-2">
                    {inCol.map((t) => (
                      <div key={String(t.id)} className="rounded-lg border border-gray-200 dark:border-gray-700 p-3 bg-white/60 dark:bg-gray-800/60">
                        <p className="text-sm text-gray-800 dark:text-gray-200">{sv(t, 'title')}</p>
                        <p className={`text-xs mt-1 inline-block px-2 py-0.5 rounded-full ${priorityColor[sv(t, 'priority')] || ''}`}>{sv(t, 'priority')}</p>
                        <div className="flex gap-1 mt-2 flex-wrap">
                          {STATUSES.filter((s) => s !== colStatus && s !== 'CANCELLED').map((s) => (
                            <button key={s} title={tText('Déplacer')} onClick={() => moveTask.mutate({ id: Number(t.id), status: s })} className="text-[10px] px-1.5 py-0.5 rounded bg-gray-100 dark:bg-gray-700 text-gray-600 dark:text-gray-300 inline-flex items-center gap-0.5"><ArrowRight className="w-2.5 h-2.5" />{s}</button>
                          ))}
                        </div>
                      </div>
                    ))}
                  </div>
                </div>
              );
            })}
          </div>
        )
      )}

      {tab === 'overdue' && (
        loadingOverdue ? spinner : overdue.length === 0 ? <div className="glass-card p-10 text-center text-gray-500">{tText('Aucune tâche en retard')}</div> : (
          <div className="space-y-3">
            {overdue.map((t) => (
              <div key={String(t.id)} className="glass-card p-4 flex items-center gap-4">
                <AlarmClock className="w-4 h-4 text-red-500" />
                <span className="text-sm text-gray-800 dark:text-gray-200">{sv(t, 'title')}</span>
                <span className="text-xs text-gray-500 ml-auto">{tText('Échéance')} : {sv(t, 'dueDate')}</span>
                <span className="text-xs px-2 py-0.5 rounded-full bg-red-100 text-red-700">{sv(t, 'status')}</span>
              </div>
            ))}
          </div>
        )
      )}

      {tab === 'reports' && (
        <div className="space-y-6">
          {stats && (
            <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
              <div className="stat-card"><ListTodo className="w-5 h-5 text-sky-500" /><p className="stat-value">{sv(stats, 'total')}</p><p className="stat-label">{tText('Total')}</p></div>
              <div className="stat-card"><p className="stat-value">{sv(stats, 'done')}</p><p className="stat-label">{tText('Terminées')}</p></div>
              <div className="stat-card"><p className="stat-value">{sv(stats, 'inProgress')}</p><p className="stat-label">{tText('En cours')}</p></div>
              <div className="stat-card"><p className="stat-value">{sv(stats, 'blocked')}</p><p className="stat-label">{tText('Bloquées')}</p></div>
            </div>
          )}
          <div className="glass-card p-5">
            <h3 className="font-semibold text-gray-800 dark:text-gray-200 mb-3 inline-flex items-center gap-2"><BarChart3 className="w-4 h-4" />{tText('Par statut')}</h3>
            {byStatus.map((r) => (
              <div key={sv(r, 'status')} className="flex justify-between text-sm text-gray-700 dark:text-gray-300 py-1 border-b border-gray-100 last:border-0">
                <span>{sv(r, 'status')}</span><span>{sv(r, 'count')}</span>
              </div>
            ))}
          </div>
          <div className="glass-card p-5">
            <h3 className="font-semibold text-gray-800 dark:text-gray-200 mb-3">{tText('Par assigné')}</h3>
            {byAssignee.length === 0 ? <p className="text-sm text-gray-500">{tText('Aucune donnée')}</p> : byAssignee.map((r, i) => (
              <div key={i} className="flex justify-between text-sm text-gray-700 dark:text-gray-300 py-1 border-b border-gray-100 last:border-0">
                <span>{sv(r, 'assignedToName')}</span><span>{sv(r, 'count')}</span>
              </div>
            ))}
          </div>
        </div>
      )}
    </div>
  );
}
