import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import api, { getErrorMessage } from '@/lib/api';
import { Rocket, Loader2, CheckCircle2, Circle, ArrowRight } from 'lucide-react';
import toast from 'react-hot-toast';

interface OnboardingStep {
  id: string;
  title: string;
  description?: string;
  order: number;
  isCompleted: boolean;
  completedAt?: string;
}

export default function OnboardingWizardPage() {
  const qc = useQueryClient();

  const { data: steps = [], isLoading, error } = useQuery({
    queryKey: ['onboarding-wizard'],
    queryFn: async () => (await api.get('/onboarding-wizard')).data as OnboardingStep[],
  });

  const completeMutation = useMutation({
    mutationFn: async (id: string) => api.post(`/onboarding-wizard/${id}/complete`),
    onSuccess: () => { toast.success('Onboarding complété !'); qc.invalidateQueries({ queryKey: ['onboarding-wizard'] }); },
    onError: (e) => toast.error(getErrorMessage(e)),
  });

  const sorted = [...steps].sort((a, b) => a.order - b.order);
  const completedCount = sorted.filter((s) => s.isCompleted).length;
  const progress = sorted.length > 0 ? Math.round((completedCount / sorted.length) * 100) : 0;
  const allDone = sorted.length > 0 && sorted.every((s) => s.isCompleted);

  return (
    <div className="page-container">
      <div className="page-header">
        <div className="p-3 rounded-xl bg-gradient-to-br from-violet-500 to-purple-600 text-white shadow-lg">
          <Rocket className="w-6 h-6" />
        </div>
        <div>
          <h1 className="page-title">Assistant de configuration</h1>
          <p className="page-subtitle">Complétez les étapes pour démarrer sur la plateforme</p>
        </div>
      </div>

      {isLoading ? (
        <div className="flex justify-center py-12"><Loader2 className="w-8 h-8 animate-spin text-primary-500" /></div>
      ) : error ? (
        <div className="glass-card p-6 text-red-400">{getErrorMessage(error)}</div>
      ) : steps.length === 0 ? (
        <div className="glass-card p-10 text-center text-gray-500">Aucune étape de configuration</div>
      ) : (
        <>
          <div className="glass-card p-6 mb-6">
            <div className="flex items-center justify-between mb-3">
              <span className="text-sm text-gray-500">Progression</span>
              <span className="text-sm font-medium text-gray-800 dark:text-gray-200">{completedCount}/{sorted.length} ({progress}%)</span>
            </div>
            <div className="h-3 bg-gray-200 dark:bg-gray-700 rounded-full overflow-hidden">
              <div className="h-full bg-gradient-to-r from-violet-500 to-purple-500 rounded-full transition-all" style={{ width: `${progress}%` }} />
            </div>
          </div>

          <div className="space-y-3 mb-6">
            {sorted.map((step, idx) => (
              <div key={step.id} className={`glass-card p-5 flex items-center gap-4 ${step.isCompleted ? 'opacity-70' : ''}`}>
                <div className="shrink-0">
                  {step.isCompleted ? <CheckCircle2 className="w-6 h-6 text-green-400" /> : <Circle className="w-6 h-6 text-gray-500" />}
                </div>
                <div className="flex-1">
                  <div className="flex items-center gap-2">
                    <span className="text-xs text-gray-400 font-mono">#{idx + 1}</span>
                    <p className="text-sm font-medium text-gray-800 dark:text-gray-200">{step.title}</p>
                  </div>
                  {step.description && <p className="text-xs text-gray-500 mt-1">{step.description}</p>}
                  {step.completedAt && <p className="text-[11px] text-green-400 mt-1">Complété le {new Date(step.completedAt).toLocaleDateString('fr-FR')}</p>}
                </div>
                {!step.isCompleted && (
                  <ArrowRight className="w-4 h-4 text-violet-400 shrink-0" />
                )}
              </div>
            ))}
          </div>

          {!allDone && (
            <div className="text-center">
              <p className="text-sm text-gray-500 mb-3">{sorted.length - completedCount} étape(s) restante(s)</p>
            </div>
          )}

          {allDone && (
            <div className="glass-card p-6 text-center">
              <CheckCircle2 className="w-12 h-12 text-green-400 mx-auto mb-3" />
              <p className="text-lg font-bold text-gray-800 dark:text-gray-200 mb-2">Toutes les étapes sont complétées !</p>
              <button onClick={() => completeMutation.mutate(sorted[sorted.length - 1].id)} disabled={completeMutation.isPending}
                className="btn-primary inline-flex items-center gap-2">
                {completeMutation.isPending ? <Loader2 className="w-4 h-4 animate-spin" /> : <Rocket className="w-4 h-4" />}
                Finaliser la configuration
              </button>
            </div>
          )}
        </>
      )}
    </div>
  );
}
