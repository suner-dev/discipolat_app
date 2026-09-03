import { useQuery } from '@tanstack/react-query';
import { useParams } from 'react-router-dom';
import api, { getErrorMessage } from '@/lib/api';
import {
  ClipboardList, BarChart3, Users, CheckCircle, Clock, AlertTriangle,
  Loader2, ArrowLeft,
} from 'lucide-react';
import { useNavigate } from 'react-router-dom';

interface Survey {
  id: string;
  title: string;
  description: string;
  status: string;
  totalQuestions: number;
  totalResponses: number;
  createdAt: string;
  closesAt?: string;
}

interface SurveyResults {
  surveyId: string;
  totalResponses: number;
  completionRate: number;
  averageTimeMinutes: number;
  questionResults: { questionId: string; questionText: string; type: string; responseCount: number; average?: number; distribution?: Record<string, number> }[];
}

interface SurveyStatus {
  surveyId: string;
  status: string;
  isOpen: boolean;
  responseCount: number;
  deadline?: string;
}

export default function SurveyDetailPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();

  const { data: survey, isLoading } = useQuery({
    queryKey: ['surveys', id],
    queryFn: async () => {
      const res = await api.get(`/surveys/${id}`);
      return res.data as Survey;
    },
    enabled: !!id,
  });

  const { data: results } = useQuery({
    queryKey: ['surveys', id, 'results'],
    queryFn: async () => {
      const res = await api.get(`/surveys/${id}/results`);
      return res.data as SurveyResults;
    },
    enabled: !!id,
  });

  const { data: status } = useQuery({
    queryKey: ['surveys', id, 'status'],
    queryFn: async () => {
      const res = await api.get(`/surveys/${id}/status`);
      return res.data as SurveyStatus;
    },
    enabled: !!id,
  });

  if (isLoading) return <div className="min-h-[40vh] flex items-center justify-center"><Loader2 className="w-6 h-6 animate-spin text-gray-400" /></div>;

  if (!survey) return <div className="p-6 text-center text-gray-400">Enquête introuvable.</div>;

  return (
    <div className="page-container max-w-5xl">
      <div className="page-header">
        <div className="animate-fade-in">
          <button onClick={() => navigate(-1)} className="btn-ghost btn-sm mb-2 -ml-2">
            <ArrowLeft className="w-4 h-4" /> Retour
          </button>
          <div className="flex items-center gap-2 mb-1">
            <ClipboardList className="w-5 h-5 text-primary-500" />
            <h1 className="page-title">{survey.title}</h1>
          </div>
          <p className="page-subtitle">{survey.description}</p>
        </div>
      </div>

      {/* Status banner */}
      {status && (
        <div className={`glass-card p-4 mb-6 flex items-center gap-3 ${status.isOpen ? 'border-l-4 border-green-500' : 'border-l-4 border-gray-400'}`}>
          {status.isOpen ? <CheckCircle className="w-5 h-5 text-green-500" /> : <Clock className="w-5 h-5 text-gray-400" />}
          <div>
            <p className="text-sm font-medium text-gray-900 dark:text-gray-100">
              Enquête {status.isOpen ? 'ouverte' : 'fermée'} — {status.responseCount} réponse(s)
            </p>
            {status.deadline && (
              <p className="text-xs text-gray-400">Clôture le {new Date(status.deadline).toLocaleDateString('fr-FR')}</p>
            )}
          </div>
        </div>
      )}

      {/* Stats */}
      {results && (
        <div className="grid grid-cols-2 lg:grid-cols-3 gap-4 mb-6">
          {[
            { label: 'Réponses', value: results.totalResponses, icon: Users, color: 'from-primary-500 to-primary-600' },
            { label: 'Taux complétion', value: `${results.completionRate}%`, icon: BarChart3, color: 'from-green-500 to-emerald-500' },
            { label: 'Durée moyenne', value: `${results.averageTimeMinutes}min`, icon: Clock, color: 'from-blue-500 to-indigo-500' },
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

      {/* Question results */}
      {results && results.questionResults.length > 0 && (
        <div className="space-y-3">
          <h3 className="text-sm font-bold text-gray-900 dark:text-gray-100">Résultats par question</h3>
          {results.questionResults.map((qr) => (
            <div key={qr.questionId} className="glass-card px-5 py-4">
              <h4 className="text-sm font-semibold text-gray-900 dark:text-gray-100 mb-2">{qr.questionText}</h4>
              <div className="flex items-center gap-4 text-[10px] text-gray-400 mb-2">
                <span>{qr.responseCount} réponse(s)</span>
                {qr.average !== undefined && <span>Moyenne : {qr.average.toFixed(1)}</span>}
                <span className="badge text-[9px]">{qr.type}</span>
              </div>
              {qr.distribution && (
                <div className="space-y-1.5 mt-3">
                  {Object.entries(qr.distribution).map(([label, count]) => {
                    const pct = qr.responseCount > 0 ? Math.round((count / qr.responseCount) * 100) : 0;
                    return (
                      <div key={label}>
                        <div className="flex items-center justify-between text-[10px] text-gray-400 mb-0.5">
                          <span>{label}</span>
                          <span>{count} ({pct}%)</span>
                        </div>
                        <div className="w-full h-1.5 rounded-full bg-gray-200 dark:bg-gray-700 overflow-hidden">
                          <div className="h-full rounded-full bg-primary-500" style={{ width: `${pct}%` }} />
                        </div>
                      </div>
                    );
                  })}
                </div>
              )}
            </div>
          ))}
        </div>
      )}

      {(!results || results.questionResults.length === 0) && (
        <div className="glass-card p-10 text-center">
          <BarChart3 className="w-10 h-10 text-gray-300 mb-3 mx-auto" />
          <p className="text-gray-500 font-medium">Aucun résultat disponible.</p>
        </div>
      )}
    </div>
  );
}
