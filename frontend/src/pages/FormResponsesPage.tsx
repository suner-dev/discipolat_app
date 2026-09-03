import { useQuery } from '@tanstack/react-query';
import { useParams } from 'react-router-dom';
import api, { getErrorMessage } from '@/lib/api';
import { FileText, Users, BarChart3, Clock, CheckCircle, Loader2, ArrowLeft } from 'lucide-react';
import { useNavigate } from 'react-router-dom';

interface FormResponse {
  id: string;
  respondentName?: string;
  respondentEmail?: string;
  submittedAt: string;
  answers: Record<string, unknown>;
}

interface FormStats {
  totalResponses: number;
  averageCompletionTime: number;
  completionRate: number;
  lastResponseAt?: string;
}

export default function FormResponsesPage() {
  const { templateId } = useParams<{ templateId: string }>();
  const navigate = useNavigate();

  const { data: responses = [], isLoading } = useQuery({
    queryKey: ['forms', templateId, 'responses'],
    queryFn: async () => {
      const res = await api.get(`/forms/${templateId}/responses`);
      return (res.data.content || res.data || []) as FormResponse[];
    },
    enabled: !!templateId,
  });

  const { data: stats } = useQuery({
    queryKey: ['forms', templateId, 'stats'],
    queryFn: async () => {
      const res = await api.get(`/forms/${templateId}/stats`);
      return res.data as FormStats;
    },
    enabled: !!templateId,
  });

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
            <h1 className="page-title">Réponses du formulaire</h1>
          </div>
          <p className="page-subtitle">Consultez les réponses soumises et les statistiques</p>
        </div>
      </div>

      {stats && (
        <div className="grid grid-cols-2 lg:grid-cols-3 gap-4 mb-6">
          {[
            { label: 'Réponses', value: stats.totalResponses, icon: Users, color: 'from-primary-500 to-primary-600' },
            { label: 'Taux complétion', value: `${stats.completionRate}%`, icon: CheckCircle, color: 'from-green-500 to-emerald-500' },
            { label: 'Durée moyenne', value: `${stats.averageCompletionTime}min`, icon: Clock, color: 'from-blue-500 to-indigo-500' },
          ].map((stat, i) => (
            <div key={stat.label} className="stat-card animate-slide-up" style={{ animationDelay: `${i * 60}ms` }}>
              <div className="flex items-start justify-between mb-2">
                <span className="stat-label text-[10px]">{stat.label}</span>
                <div className={`p-1.5 rounded-lg bg-gradient-to-br ${stat.color} text-white shadow-sm`}>
                  <stat.icon className="w-3.5 h-3.5" />
                </div>
              </div>
              <p className="stat-value text-xl">{stat.value}</p>
            </div>
          ))}
        </div>
      )}

      {responses.length === 0 ? (
        <div className="glass-card p-10 text-center">
          <FileText className="w-10 h-10 text-gray-300 mb-3 mx-auto" />
          <p className="text-gray-500 font-medium">Aucune réponse reçue.</p>
        </div>
      ) : (
        <div className="space-y-3">
          {responses.map((r) => (
            <div key={r.id} className="glass-card px-5 py-4">
              <div className="flex items-start justify-between mb-2">
                <div>
                  <p className="text-sm font-semibold text-gray-900 dark:text-gray-100">
                    {r.respondentName || r.respondentEmail || 'Anonyme'}
                  </p>
                  <p className="text-[10px] text-gray-400 flex items-center gap-1 mt-0.5">
                    <Clock className="w-3 h-3" />
                    {new Date(r.submittedAt).toLocaleString('fr-FR')}
                  </p>
                </div>
              </div>
              <div className="mt-3 space-y-2">
                {Object.entries(r.answers).map(([key, value]) => (
                  <div key={key} className="flex items-start gap-2">
                    <span className="text-[10px] text-gray-400 font-medium min-w-[120px] shrink-0">{key}</span>
                    <span className="text-xs text-gray-700 dark:text-gray-300">{String(value)}</span>
                  </div>
                ))}
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
