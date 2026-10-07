import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import api, { getErrorMessage } from '@/lib/api';
import { GraduationCap, Loader2, BookOpen, PlayCircle } from 'lucide-react';
import toast from 'react-hot-toast';

import { tText } from '@/i18n';
// Contrat réel : GET /api/v1/trainings/courses renvoie CourseResponse[]
// { id, titre, description, categorie, niveau, dureeMinutes, nbModules, nbInscrits, ... }
interface Course {
  id: string;
  titre: string;
  description?: string;
  categorie?: string;
  dureeMinutes?: number;
  nbModules?: number;
  nbInscrits?: number;
}

// GET /api/v1/trainings/courses/{courseId}/modules renvoie CourseModule[]
// { id, titre, contenu, videoUrl, ordre } — la liste ne rapporte pas l'état
// de complétion par module ; l'action « terminer » appelle l'endpoint dédié.
interface Module {
  id: string;
  titre: string;
  videoUrl?: string;
}

export default function CoursesPage() {
  const qc = useQueryClient();
  const [selectedCourse, setSelectedCourse] = useState<string | null>(null);

  const { data: courses = [], isLoading } = useQuery({
    queryKey: ['courses'],
    queryFn: async () => (await api.get('/trainings/courses')).data as Course[],
  });

  const { data: modules = [] } = useQuery({
    queryKey: ['courses', selectedCourse, 'modules'],
    queryFn: async () => (await api.get(`/trainings/courses/${selectedCourse}/modules`)).data as Module[],
    enabled: !!selectedCourse,
  });

  const enrollMutation = useMutation({
    mutationFn: async (id: string) => api.post(`/trainings/courses/${id}/enroll`),
    onSuccess: () => { toast.success(tText('Inscrit au cours')); qc.invalidateQueries({ queryKey: ['courses'] }); },
    onError: (e) => toast.error(getErrorMessage(e)),
  });

  const completeModuleMutation = useMutation({
    mutationFn: async (moduleId: string) => api.post(`/trainings/courses/${selectedCourse}/modules/${moduleId}/complete`),
    onSuccess: () => { toast.success(tText('Module terminé')); qc.invalidateQueries({ queryKey: ['courses', selectedCourse, 'modules'] }); },
    onError: (e) => toast.error(getErrorMessage(e)),
  });

  return (
    <div className="page-container">
      <div className="page-header">
        <div className="p-3 rounded-xl bg-gradient-to-br from-purple-500 to-violet-600 text-white shadow-lg">
          <GraduationCap className="w-6 h-6" />
        </div>
        <div>
          <h1 className="page-title">{tText('Formations')}</h1>
          <p className="page-subtitle">{tText('Cours et modules de formation')}</p>
        </div>
      </div>

      {isLoading ? (
        <div className="flex justify-center py-12"><Loader2 className="w-8 h-8 animate-spin text-primary-500" /></div>
      ) : courses.length === 0 ? (
        <div className="glass-card p-10 text-center text-gray-500">{tText('Aucun cours disponible')}</div>
      ) : (
        <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-3">
          {courses.map((course) => (
            <div key={course.id} className="glass-card p-5">
              <div className="flex items-start justify-between mb-3">
                <div className="p-2 rounded-lg bg-purple-500/20">
                  <BookOpen className="w-5 h-5 text-purple-400" />
                </div>
                {course.categorie && (
                  <span className="px-2 py-0.5 rounded-full bg-purple-500/20 text-purple-400 text-xs">{course.categorie}</span>
                )}
              </div>
              <h3 className="font-semibold text-gray-800 dark:text-gray-200 mb-1">{course.titre}</h3>
              <p className="text-xs text-gray-500 mb-3">{course.description}</p>
              <div className="flex items-center gap-3 text-xs text-gray-400 mb-4">
                {course.dureeMinutes ? <span>{course.dureeMinutes} min</span> : null}
                {course.nbModules ? <span>{course.nbModules} {tText('modules')}</span> : null}
                {course.nbInscrits ? <span>{course.nbInscrits} {tText('inscrits')}</span> : null}
              </div>
              <div className="flex gap-2">
                <button
                  onClick={() => setSelectedCourse(course.id)}
                  className="flex-1 btn-sm px-3 py-1.5 rounded-lg bg-purple-500/20 text-purple-400 text-xs hover:bg-purple-500/30"
                >
                  {tText('Voir modules')}
                </button>
                <button
                  onClick={() => enrollMutation.mutate(course.id)}
                  disabled={enrollMutation.isPending}
                  className="flex-1 btn-sm px-3 py-1.5 rounded-lg bg-primary-500 text-white text-xs hover:bg-primary-600"
                >
                  {tText("S'inscrire")}
                </button>
              </div>
              {selectedCourse === course.id && modules.length > 0 && (
                <div className="mt-4 pt-4 border-t border-white/10 space-y-2">
                  {modules.map((m) => (
                    <div key={m.id} className="flex items-center justify-between text-xs">
                      <span className="text-gray-600 dark:text-gray-300">{m.titre}</span>
                      <button onClick={() => completeModuleMutation.mutate(m.id)} className="text-primary-400 hover:text-primary-300">
                        <PlayCircle className="w-4 h-4" />
                      </button>
                    </div>
                  ))}
                </div>
              )}
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
