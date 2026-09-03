import { useQuery } from '@tanstack/react-query';
import { useParams } from 'react-router-dom';
import api from '@/lib/api';
import {
  FileText, Activity, Users, BarChart3, Clock, TrendingUp, Loader2, ArrowLeft,
} from 'lucide-react';
import { useNavigate } from 'react-router-dom';

interface DepartmentReport {
  id: string;
  title: string;
  type: string;
  generatedAt: string;
  summary?: string;
  generatedBy?: string;
}

interface DepartmentActivity {
  id: string;
  action: string;
  actorName: string;
  targetName?: string;
  timestamp: string;
  details?: string;
}

export default function DepartmentReportsPage() {
  const { departmentId } = useParams<{ departmentId: string }>();
  const navigate = useNavigate();

  const { data: reports = [], isLoading: reportsLoading } = useQuery({
    queryKey: ['departments', departmentId, 'reports'],
    queryFn: async () => {
      const res = await api.get(`/departments/${departmentId}/reports`);
      return (res.data.content || res.data || []) as DepartmentReport[];
    },
    enabled: !!departmentId,
  });

  const { data: activity = [], isLoading: activityLoading } = useQuery({
    queryKey: ['departments', departmentId, 'activity'],
    queryFn: async () => {
      const res = await api.get(`/departments/${departmentId}/activity`);
      return (res.data.content || res.data || []) as DepartmentActivity[];
    },
    enabled: !!departmentId,
  });

  const isLoading = reportsLoading || activityLoading;
  if (isLoading) return <div className="min-h-[40vh] flex items-center justify-center"><Loader2 className="w-6 h-6 animate-spin text-gray-400" /></div>;

  return (
    <div className="page-container max-w-5xl">
      <div className="page-header">
        <div className="animate-fade-in">
          <button onClick={() => navigate(-1)} className="btn-ghost btn-sm mb-2 -ml-2">
            <ArrowLeft className="w-4 h-4" /> Retour
          </button>
          <div className="flex items-center gap-2 mb-1">
            <FileText className="w-5 h-5 text-primary-500" />
            <h1 className="page-title">Rapports du département</h1>
          </div>
          <p className="page-subtitle">Rapports générés et activité récente</p>
        </div>
      </div>

      {/* Reports */}
      <div className="mb-8">
        <h3 className="text-sm font-bold text-gray-900 dark:text-gray-100 mb-3 flex items-center gap-2">
          <BarChart3 className="w-4 h-4 text-primary-500" />
          Rapports
        </h3>
        {reports.length === 0 ? (
          <div className="glass-card p-8 text-center">
            <FileText className="w-8 h-8 text-gray-300 mb-2 mx-auto" />
            <p className="text-gray-500 text-sm">Aucun rapport disponible.</p>
          </div>
        ) : (
          <div className="space-y-3">
            {reports.map((report) => (
              <div key={report.id} className="glass-card px-5 py-4">
                <div className="flex items-start justify-between mb-2">
                  <div>
                    <h4 className="text-sm font-semibold text-gray-900 dark:text-gray-100">{report.title}</h4>
                    {report.summary && <p className="text-xs text-gray-400 mt-1 line-clamp-2">{report.summary}</p>}
                  </div>
                  <span className="badge text-[10px]">{report.type}</span>
                </div>
                <div className="flex items-center gap-3 text-[10px] text-gray-400 mt-2">
                  <span className="flex items-center gap-1">
                    <Clock className="w-3 h-3" />
                    {new Date(report.generatedAt).toLocaleString('fr-FR')}
                  </span>
                  {report.generatedBy && <span>Généré par {report.generatedBy}</span>}
                </div>
              </div>
            ))}
          </div>
        )}
      </div>

      {/* Activity */}
      <div>
        <h3 className="text-sm font-bold text-gray-900 dark:text-gray-100 mb-3 flex items-center gap-2">
          <Activity className="w-4 h-4 text-green-500" />
          Activité récente
        </h3>
        {activity.length === 0 ? (
          <div className="glass-card p-8 text-center">
            <Activity className="w-8 h-8 text-gray-300 mb-2 mx-auto" />
            <p className="text-gray-500 text-sm">Aucune activité récente.</p>
          </div>
        ) : (
          <div className="space-y-2">
            {activity.map((act) => (
              <div key={act.id} className="glass-card px-5 py-3 flex items-center gap-3">
                <div className="w-8 h-8 rounded-full bg-gradient-to-br from-green-400 to-emerald-500 flex items-center justify-center text-white text-[10px] font-bold shrink-0">
                  {act.actorName.split(' ').map((w) => w[0]).join('').slice(0, 2)}
                </div>
                <div className="flex-1 min-w-0">
                  <p className="text-xs text-gray-700 dark:text-gray-300">
                    <span className="font-semibold">{act.actorName}</span> {act.action}
                    {act.targetName && <span className="text-primary-500"> {act.targetName}</span>}
                  </p>
                  {act.details && <p className="text-[10px] text-gray-400 mt-0.5">{act.details}</p>}
                </div>
                <span className="text-[10px] text-gray-400 shrink-0">
                  {new Date(act.timestamp).toLocaleDateString('fr-FR')}
                </span>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
}
