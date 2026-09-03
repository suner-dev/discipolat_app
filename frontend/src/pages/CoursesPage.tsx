import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import api, { getErrorMessage } from '@/lib/api';
import { GraduationCap, Loader2, BookOpen, CheckCircle, PlayCircle } from 'lucide-react';
import toast from 'react-hot-toast';

interface Course {
  id: string;
  title: string;
  description: string;
  category?: string;
  moduleCount?: number;
  enrolledCount?: number;
  duration?: string;
}

interface Module {
  id: string;
  title: string;
  completed: boolean;
}

export default function CoursesPage() {
  const qc = useQueryClient();
  const [selectedCourse, setSelectedCourse] = useState<string | null>(null);

  const { data: courses = [], isLoading } = useQuery({
    queryKey: ['courses'],
    queryFn: async () => (await api.get('/courses')).data as Course[],
  });

  const { data: modules = [] } = useQuery({
    queryKey: ['courses', selectedCourse, 'modules'],
    queryFn: async () => (await api.get(`/courses/${selectedCourse}/modules`)).data as Module[],
    enabled: !!selectedCourse,
  });

  const enrollMutation = useMutation({
    mutationFn: async (id: string) => api.post(`/courses/${id}/enroll`),
    onSuccess: () => { toast.success('Inscrit au cours'); qc.invalidateQueries({ queryKey: ['courses'] }); },
    onError: (e) => toast.error(getErrorMessage(e)),
  });

  const completeModuleMutation = useMutation({
    mutationFn: async (moduleId: string) => api.post(`/courses/${selectedCourse}/modules/${moduleId}/complete`),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['courses', selectedCourse, 'modules'] }); },
    onError: (e) => toast.error(getErrorMessage(e)),
  });

  return (
    <div className="page-container">
      <div className="page-header">
        <div className="p-3 rounded-xl bg-gradient-to-br from-purple-500 to-violet-600 text-white shadow-lg">
          <GraduationCap className="w-6 h-6" />
        </div>
        <div>
          <h1 className="page-title">Formations</h1>
          <p className="page-subtitle">Cours et modules de formation</p>
        </div>
      </div>

      {isLoading ? (
        <div className="flex justify-center py-12"><Loader2 className="w-8 h-8 animate-spin text-primary-500" /></div>
      ) : courses.length === 0 ? (
        <div className="glass-card p-10 text-center text-gray-500">Aucun cours disponible</div>
      ) : (
        <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-3">
          {courses.map((course) => (
            <div key={course.id} className="glass-card p-5">
              <div className="flex items-start justify-between mb-3">
                <div className="p-2 rounded-lg bg-purple-500/20">
                  <BookOpen className="w-5 h-5 text-purple-400" />
                </div>
                {course.category && (
                  <span className="px-2 py-0.5 rounded-full bg-purple-500/20 text-purple-400 text-xs">{course.category}</span>
                )}
              </div>
              <h3 className="font-semibold text-gray-800 dark:text-gray-200 mb-1">{course.title}</h3>
              <p className="text-xs text-gray-500 mb-3">{course.description}</p>
              <div className="flex items-center gap-3 text-xs text-gray-400 mb-4">
                {course.moduleCount && <span>{course.moduleCount} modules</span>}
                {course.enrolledCount && <span>{course.enrolledCount} inscrits</span>}
              </div>
              <div className="flex gap-2">
                <button
                  onClick={() => setSelectedCourse(course.id)}
                  className="flex-1 btn-sm px-3 py-1.5 rounded-lg bg-purple-500/20 text-purple-400 text-xs hover:bg-purple-500/30"
                >
                  Voir modules
                </button>
                <button
                  onClick={() => enrollMutation.mutate(course.id)}
                  disabled={enrollMutation.isPending}
                  className="flex-1 btn-sm px-3 py-1.5 rounded-lg bg-primary-500 text-white text-xs hover:bg-primary-600"
                >
                  S'inscrire
                </button>
              </div>
              {selectedCourse === course.id && modules.length > 0 && (
                <div className="mt-4 pt-4 border-t border-white/10 space-y-2">
                  {modules.map((m) => (
                    <div key={m.id} className="flex items-center justify-between text-xs">
                      <span className="text-gray-600 dark:text-gray-300">{m.title}</span>
                      {m.completed ? (
                        <CheckCircle className="w-4 h-4 text-green-400" />
                      ) : (
                        <button onClick={() => completeModuleMutation.mutate(m.id)} className="text-primary-400 hover:text-primary-300">
                          <PlayCircle className="w-4 h-4" />
                        </button>
                      )}
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