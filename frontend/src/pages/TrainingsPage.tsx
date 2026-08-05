import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import api, { getErrorMessage } from '@/lib/api';
import toast from 'react-hot-toast';
import {
  BookOpen,
  PlayCircle,
  Clock,
  Users,
  GraduationCap,
  Award,
  ChevronRight,
  ArrowLeft,
  CheckCircle2,
  FileBadge,
  Plus,
  Layers,
  BookMarked,
} from 'lucide-react';
import { useAuth } from '@/contexts/AuthContext';
import type {
  Course,
  CourseModule,
  QuizQuestion,
  CourseEnrollment,
  Certificate,
  CreateCourseRequest,
  CreateModuleRequest,
  CreateQuestionRequest,
  SubmitQuizRequest,
  QuizResult,
} from '@/types';

const NIVEAU_STYLE: Record<string, string> = {
  DEBUTANT: 'bg-emerald-500/10 text-emerald-600 dark:text-emerald-400',
  INTERMEDIAIRE: 'bg-amber-500/10 text-amber-600 dark:text-amber-400',
  AVANCE: 'bg-rose-500/10 text-rose-600 dark:text-rose-400',
};

const NIVEAU_LABEL: Record<string, string> = {
  DEBUTANT: 'Débutant',
  INTERMEDIAIRE: 'Intermédiaire',
  AVANCE: 'Avancé',
};

export default function TrainingsPage() {
  const { user } = useAuth();
  const isAdmin = !!user && (user.roles.includes('ADMIN') || user.roles.includes('PASTEUR'));
  const queryClient = useQueryClient();

  const [selected, setSelected] = useState<Course | null>(null);
  const [activeModule, setActiveModule] = useState<string | null>(null);
  const [quizOpen, setQuizOpen] = useState(false);
  const [quizAnswers, setQuizAnswers] = useState<Record<string, number>>({});
  const [quizResult, setQuizResult] = useState<QuizResult | null>(null);
  const [showCreate, setShowCreate] = useState(false);
  const [courseForm, setCourseForm] = useState<CreateCourseRequest>({
    titre: '', description: '', categorie: 'DISCIPOLAT', niveau: 'DEBUTANT', dureeMinutes: 60,
  });
  const [moduleForm, setModuleForm] = useState<CreateModuleRequest>({ titre: '', contenu: '', ordre: 0 });
  const [showModuleForm, setShowModuleForm] = useState(false);
  const [questionForm, setQuestionForm] = useState<CreateQuestionRequest>({
    question: '', propositions: '["Vrai","Faux"]', reponseIndex: 0, ordre: 0,
  });

  const coursesQuery = useQuery({
    queryKey: ['trainings', 'courses'],
    queryFn: async () => {
      const res = await api.get('/trainings/courses');
      return res.data as Course[];
    },
  });

  const modulesQuery = useQuery({
    queryKey: ['trainings', 'modules', selected?.id],
    queryFn: async () => {
      const res = await api.get(`/trainings/courses/${selected!.id}/modules`);
      return res.data as CourseModule[];
    },
    enabled: !!selected,
  });

  const enrollmentsQuery = useQuery({
    queryKey: ['trainings', 'my-enrollments'],
    queryFn: async () => {
      const res = await api.get('/trainings/my-enrollments');
      return res.data as CourseEnrollment[];
    },
  });

  const certificatesQuery = useQuery({
    queryKey: ['trainings', 'my-certificates'],
    queryFn: async () => {
      const res = await api.get('/trainings/my-certificates');
      return res.data as Certificate[];
    },
  });

  const quizQuery = useQuery({
    queryKey: ['trainings', 'quiz', activeModule],
    queryFn: async () => {
      const res = await api.get(`/trainings/modules/${activeModule}/quiz`);
      return res.data as QuizQuestion[];
    },
    enabled: quizOpen && !!activeModule,
  });

  const enrollMutation = useMutation({
    mutationFn: async (courseId: string) => {
      const res = await api.post(`/trainings/courses/${courseId}/enroll`);
      return res.data as CourseEnrollment;
    },
    onSuccess: () => {
      toast.success('Inscription réussie !');
      queryClient.invalidateQueries({ queryKey: ['trainings'] });
    },
    onError: (err) => toast.error(getErrorMessage(err)),
  });

  const submitQuizMutation = useMutation({
    mutationFn: async ({ courseId, payload }: { courseId: string; payload: SubmitQuizRequest }) => {
      const res = await api.post(`/trainings/courses/${courseId}/quiz/submit`, payload);
      return res.data as QuizResult;
    },
    onSuccess: (data) => {
      setQuizResult(data);
      queryClient.invalidateQueries({ queryKey: ['trainings'] });
      if (data.certificat) {
        toast.success('Félicitations, votre certificat a été délivré ! 🎓');
      } else if (data.reussi) {
        toast.success(`Quiz réussi : ${data.score}%`);
      } else {
        toast.error(`Quiz non réussi : ${data.score}%. Rejouez !`);
      }
    },
    onError: (err) => toast.error(getErrorMessage(err)),
  });

  const createCourseMutation = useMutation({
    mutationFn: async (payload: CreateCourseRequest) => {
      const res = await api.post('/trainings/courses', payload);
      return res.data as Course;
    },
    onSuccess: () => {
      toast.success('Cours créé');
      setShowCreate(false);
      setCourseForm({ titre: '', description: '', categorie: 'DISCIPOLAT', niveau: 'DEBUTANT', dureeMinutes: 60 });
      queryClient.invalidateQueries({ queryKey: ['trainings'] });
    },
    onError: (err) => toast.error(getErrorMessage(err)),
  });

  const createModuleMutation = useMutation({
    mutationFn: async ({ courseId, payload }: { courseId: string; payload: CreateModuleRequest }) => {
      const res = await api.post(`/trainings/courses/${courseId}/modules`, payload);
      return res.data as CourseModule;
    },
    onSuccess: () => {
      toast.success('Module ajouté');
      setModuleForm({ titre: '', contenu: '', ordre: (modulesQuery.data?.length ?? 0) + 1 });
      setShowModuleForm(false);
      queryClient.invalidateQueries({ queryKey: ['trainings'] });
    },
    onError: (err) => toast.error(getErrorMessage(err)),
  });

  const completeModuleMutation = useMutation({
    mutationFn: async ({ courseId, moduleId }: { courseId: string; moduleId: string }) => {
      const res = await api.post(`/trainings/courses/${courseId}/modules/${moduleId}/complete`);
      return res.data as CourseEnrollment;
    },
    onSuccess: () => {
      toast.success('Module marqué comme lu');
      queryClient.invalidateQueries({ queryKey: ['trainings'] });
    },
    onError: (err) => toast.error(getErrorMessage(err)),
  });

  const addQuestionMutation = useMutation({
    mutationFn: async ({ moduleId, payload }: { moduleId: string; payload: CreateQuestionRequest }) => {
      const res = await api.post(`/trainings/modules/${moduleId}/questions`, payload);
      return res.data as QuizQuestion;
    },
    onSuccess: () => {
      toast.success('Question ajoutée au quiz');
      setQuestionForm({ question: '', propositions: '["Vrai","Faux"]', reponseIndex: 0, ordre: 0 });
      queryClient.invalidateQueries({ queryKey: ['trainings'] });
    },
    onError: (err) => toast.error(getErrorMessage(err)),
  });

  const courses = coursesQuery.data ?? [];
  const enrollments = enrollmentsQuery.data ?? [];
  const certificates = certificatesQuery.data ?? [];
  const modules = modulesQuery.data ?? [];
  const quizQuestions = quizQuery.data ?? [];

  const enrollmentFor = (courseId: string) => enrollments.find(e => e.courseId === courseId);
  const certificateFor = (courseId: string) => certificates.find(c => c.courseId === courseId);

  const openQuiz = (moduleId: string) => {
    setActiveModule(moduleId);
    setQuizOpen(true);
    setQuizAnswers({});
    setQuizResult(null);
  };

  const submitQuiz = (courseId: string) => {
    if (!activeModule) return;
    submitQuizMutation.mutate({ courseId, payload: { moduleId: activeModule, reponses: quizAnswers } });
  };

  // ============================================================
  // Vue détail
  // ============================================================
  if (selected) {
    const enrollment = enrollmentFor(selected.id);
    const certificate = certificateFor(selected.id);
    return (
      <div className="animate-fade-in space-y-6">
        <button onClick={() => setSelected(null)} className="flex items-center gap-1.5 text-sm text-gray-500 dark:text-gray-400 hover:text-primary-600 transition-colors">
          <ArrowLeft className="w-4 h-4" /> Retour au catalogue
        </button>

        <div className="glass-card p-6">
          <div className="flex flex-wrap items-start justify-between gap-4">
            <div>
              <div className="flex items-center gap-2 mb-2">
                <span className={`text-[10px] font-bold px-2 py-1 rounded-full ${NIVEAU_STYLE[selected.niveau]}`}>
                  {NIVEAU_LABEL[selected.niveau]}
                </span>
                <span className="text-[10px] font-medium text-gray-400 uppercase">{selected.categorie}</span>
              </div>
              <h1 className="text-2xl font-bold text-gray-900 dark:text-gray-100">{selected.titre}</h1>
              <p className="text-sm text-gray-500 dark:text-gray-400 mt-2 max-w-2xl">{selected.description}</p>
              <div className="flex flex-wrap items-center gap-4 mt-3 text-xs text-gray-400">
                <span className="flex items-center gap-1"><Layers className="w-3.5 h-3.5" /> {selected.nbModules} module(s)</span>
                <span className="flex items-center gap-1"><Clock className="w-3.5 h-3.5" /> {selected.dureeMinutes} min</span>
                {selected.formateurNom && (
                  <span className="flex items-center gap-1"><GraduationCap className="w-3.5 h-3.5" /> {selected.formateurNom}</span>
                )}
                <span className="flex items-center gap-1"><Users className="w-3.5 h-3.5" /> {selected.nbInscrits} inscrit(s)</span>
              </div>
            </div>
            <div className="flex flex-col items-end gap-2">
              {certificate && (
                <span className="inline-flex items-center gap-1.5 text-xs font-bold text-cyan-600 dark:text-cyan-400 bg-cyan-500/10 px-3 py-1.5 rounded-full">
                  <FileBadge className="w-4 h-4" /> Certificat obtenu
                </span>
              )}
              {enrollment && (
                <div className="w-48">
                  <div className="flex justify-between text-[11px] text-gray-500 dark:text-gray-400 mb-1">
                    <span>Progression</span><span>{enrollment.progression}%</span>
                  </div>
                  <div className="h-2 bg-gray-100 dark:bg-gray-800 rounded-full overflow-hidden">
                    <div className={`h-full rounded-full ${enrollment.progression >= 100 ? 'bg-emerald-500' : 'bg-primary-500'}`} style={{ width: `${enrollment.progression}%` }} />
                  </div>
                </div>
              )}
              {!enrollment && (
                <button onClick={() => enrollMutation.mutate(selected.id)} className="btn btn-primary text-sm">
                  S'inscrire
                </button>
              )}
            </div>
          </div>
        </div>

        {/* Modules */}
        <div className="glass-card divide-y divide-gray-100 dark:divide-gray-800">
          <div className="px-4 py-3 flex items-center justify-between">
            <h2 className="text-sm font-semibold text-gray-900 dark:text-gray-100 flex items-center gap-2">
              <BookOpen className="w-4 h-4 text-primary-500" /> Modules du cours
            </h2>
            {isAdmin && (
              <button
                onClick={() => setShowModuleForm(v => !v)}
                className="text-xs text-primary-600 hover:text-primary-700 flex items-center gap-1"
              >
                <Plus className="w-3.5 h-3.5" /> Ajouter un module
              </button>
            )}
          </div>

          {showModuleForm && isAdmin && (
            <div className="px-4 py-3 bg-gray-50/60 dark:bg-gray-800/40 space-y-2">
              <div className="grid grid-cols-1 sm:grid-cols-3 gap-2">
                <input
                  type="text"
                  placeholder="Titre du module"
                  value={moduleForm.titre}
                  onChange={e => setModuleForm({ ...moduleForm, titre: e.target.value })}
                  className="input text-sm"
                />
                <input
                  type="text"
                  placeholder="URL vidéo (optionnel)"
                  value={moduleForm.videoUrl ?? ''}
                  onChange={e => setModuleForm({ ...moduleForm, videoUrl: e.target.value })}
                  className="input text-sm"
                />
                <input
                  type="text"
                  placeholder="Contenu / résumé"
                  value={moduleForm.contenu}
                  onChange={e => setModuleForm({ ...moduleForm, contenu: e.target.value })}
                  className="input text-sm"
                />
              </div>
              <div className="flex gap-2">
                <button
                  onClick={() => moduleForm.titre && createModuleMutation.mutate({ courseId: selected.id, payload: { ...moduleForm, ordre: modules.length + 1 } })}
                  disabled={!moduleForm.titre}
                  className="btn btn-primary text-xs"
                >
                  Créer le module
                </button>
                <button onClick={() => setShowModuleForm(false)} className="btn text-xs">Annuler</button>
              </div>
            </div>
          )}

          {modulesQuery.isLoading ? (
            <div className="p-6 text-center text-sm text-gray-400">Chargement…</div>
          ) : modules.length === 0 ? (
            <div className="p-8 text-center text-sm text-gray-400">Aucun module pour ce cours pour le moment.</div>
          ) : (
            modules.map((m, idx) => (
              <div key={m.id} className="px-4 py-3 flex flex-wrap items-center gap-3 hover:bg-gray-50/60 dark:hover:bg-gray-800/30 transition-colors">
                <span className="w-7 h-7 rounded-full bg-primary-500/10 text-primary-600 dark:text-primary-400 flex items-center justify-center text-xs font-bold">
                  {idx + 1}
                </span>
                <div className="flex-1 min-w-0">
                  <p className="text-sm font-medium text-gray-900 dark:text-gray-100">{m.titre}</p>
                  {m.contenu && <p className="text-xs text-gray-400 line-clamp-1">{m.contenu}</p>}
                </div>
                {m.videoUrl && <PlayCircle className="w-4 h-4 text-gray-300 shrink-0" />}
                <div className="flex items-center gap-1.5">
                  <button
                    onClick={() => completeModuleMutation.mutate({ courseId: selected.id, moduleId: m.id })}
                    className="btn btn-secondary text-xs flex items-center gap-1"
                    title="Marquer ce module comme lu"
                  >
                    <BookMarked className="w-3.5 h-3.5" /> Marquer lu
                  </button>
                  {isAdmin && (
                    <button
                      onClick={() => openQuiz(m.id)}
                      className="btn btn-secondary text-xs"
                      title="Gérer les questions du quiz"
                    >
                      Quiz
                    </button>
                  )}
                  {!isAdmin && (
                    <button onClick={() => openQuiz(m.id)} className="btn btn-secondary text-xs">
                      Quiz
                    </button>
                  )}
                </div>
              </div>
            ))
          )}
        </div>

        {/* Quiz modal */}
        {quizOpen && (
          <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4" onClick={() => setQuizOpen(false)}>
            <div className="glass-card w-full max-w-lg max-h-[85vh] overflow-y-auto p-6" onClick={e => e.stopPropagation()}>
              <div className="flex items-center justify-between mb-4">
                <h3 className="text-lg font-bold text-gray-900 dark:text-gray-100">Quiz du module</h3>
                <button onClick={() => setQuizOpen(false)} className="text-gray-400 hover:text-gray-600 text-xl">×</button>
              </div>

              {quizResult ? (
                <div className="text-center py-6 space-y-3">
                  <span className={`inline-flex items-center justify-center w-16 h-16 rounded-full text-2xl font-bold ${quizResult.reussi ? 'bg-emerald-500/10 text-emerald-500' : 'bg-rose-500/10 text-rose-500'}`}>
                    {quizResult.score}%
                  </span>
                  <p className="text-sm text-gray-600 dark:text-gray-300">
                    {quizResult.bonnesReponses} / {quizResult.totalQuestions} bonnes réponses
                  </p>
                  <p className="text-xs text-gray-400">
                    {quizResult.certificat
                      ? '🎓 Certificat délivré ! Consultez l\'onglet certificats.'
                      : quizResult.reussi
                        ? 'Module validé ! Continuez la progression.'
                        : 'Il faut au moins 70% pour valider ce module. Réessayez !'}
                  </p>
                  <button onClick={() => setQuizOpen(false)} className="btn btn-primary text-sm">Fermer</button>
                </div>
              ) : quizQuery.isLoading ? (
                <p className="text-center text-sm text-gray-400 py-6">Chargement…</p>
              ) : quizQuestions.length === 0 ? (
                <div className="text-center py-6 space-y-3">
                  <p className="text-sm text-gray-400">Aucune question pour ce module.</p>
                  {isAdmin && (
                    <div className="text-left space-y-2 border-t border-gray-100 dark:border-gray-800 pt-4">
                      <p className="text-xs font-semibold text-gray-500 uppercase tracking-wide">Ajouter une question</p>
                      <input
                        type="text"
                        placeholder="Question"
                        value={questionForm.question}
                        onChange={e => setQuestionForm({ ...questionForm, question: e.target.value })}
                        className="input text-sm w-full"
                      />
                      <input
                        type="text"
                        placeholder={'Propositions JSON, ex: ["Vrai","Faux"]'}
                        value={questionForm.propositions}
                        onChange={e => setQuestionForm({ ...questionForm, propositions: e.target.value })}
                        className="input text-sm w-full"
                      />
                      <div className="flex items-center gap-2">
                        <label className="text-xs text-gray-500">Bonne réponse (index)</label>
                        <input
                          type="number"
                          min={0}
                          value={questionForm.reponseIndex}
                          onChange={e => setQuestionForm({ ...questionForm, reponseIndex: Number(e.target.value) })}
                          className="input text-sm w-20"
                        />
                      </div>
                      <button
                        onClick={() => activeModule && addQuestionMutation.mutate({ moduleId: activeModule, payload: questionForm })}
                        disabled={!questionForm.question}
                        className="btn btn-primary text-xs"
                      >
                        Ajouter la question
                      </button>
                    </div>
                  )}
                </div>
              ) : (
                <div className="space-y-4">
                  {quizQuestions.map((q, qi) => {
                    let props: string[] = [];
                    try { props = JSON.parse(q.propositions); } catch { /* ignore */ }
                    return (
                      <div key={q.id} className="space-y-2">
                        <p className="text-sm font-medium text-gray-800 dark:text-gray-200">
                          {qi + 1}. {q.question}
                        </p>
                        <div className="space-y-1.5">
                          {props.map((p, pi) => (
                            <button
                              key={pi}
                              onClick={() => setQuizAnswers({ ...quizAnswers, [q.id]: pi })}
                              className={`w-full text-left text-sm px-3 py-2 rounded-lg border transition-colors ${
                                quizAnswers[q.id] === pi
                                  ? 'border-primary-500 bg-primary-500/10 text-primary-700 dark:text-primary-300'
                                  : 'border-gray-100 dark:border-gray-800 hover:border-primary-300'
                              }`}
                            >
                              <span className="inline-flex items-center gap-2">
                                {quizAnswers[q.id] === pi && <CheckCircle2 className="w-4 h-4 text-primary-500" />}
                                {p}
                              </span>
                            </button>
                          ))}
                        </div>
                      </div>
                    );
                  })}
                  <button
                    onClick={() => submitQuiz(selected.id)}
                    disabled={Object.keys(quizAnswers).length < quizQuestions.length}
                    className="btn btn-primary w-full"
                  >
                    Valider le quiz
                  </button>

                  {isAdmin && activeModule && (
                    <div className="border-t border-gray-100 dark:border-gray-800 pt-4 space-y-2">
                      <p className="text-xs font-semibold text-gray-500 uppercase tracking-wide">Ajouter une question</p>
                      <input
                        type="text"
                        placeholder="Question"
                        value={questionForm.question}
                        onChange={e => setQuestionForm({ ...questionForm, question: e.target.value })}
                        className="input text-sm w-full"
                      />
                      <input
                        type="text"
                        placeholder="Propositions JSON"
                        value={questionForm.propositions}
                        onChange={e => setQuestionForm({ ...questionForm, propositions: e.target.value })}
                        className="input text-sm w-full"
                      />
                      <div className="flex items-center gap-2">
                        <label className="text-xs text-gray-500">Bonne réponse (index)</label>
                        <input
                          type="number"
                          min={0}
                          value={questionForm.reponseIndex}
                          onChange={e => setQuestionForm({ ...questionForm, reponseIndex: Number(e.target.value) })}
                          className="input text-sm w-20"
                        />
                      </div>
                      <button
                        onClick={() => activeModule && addQuestionMutation.mutate({ moduleId: activeModule, payload: questionForm })}
                        disabled={!questionForm.question}
                        className="btn btn-primary text-xs"
                      >
                        Ajouter la question
                      </button>
                    </div>
                  )}
                </div>
              )}
            </div>
          </div>
        )}
      </div>
    );
  }

  // ============================================================
  // Vue catalogue
  // ============================================================
  return (
    <div className="animate-fade-in space-y-6">
      <div className="flex flex-wrap items-start justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-gray-900 dark:text-gray-100">Formations</h1>
          <p className="text-sm text-gray-500 dark:text-gray-400 mt-1">
            Plateforme de formation : cours, quiz, progression et certificats
          </p>
        </div>
        {isAdmin && (
          <button onClick={() => setShowCreate(v => !v)} className="btn btn-primary flex items-center gap-2">
            <Plus className="w-4 h-4" /> Nouveau cours
          </button>
        )}
      </div>

      {certificates.length > 0 && (
        <div className="glass-card p-4">
          <h2 className="text-sm font-semibold text-gray-900 dark:text-gray-100 mb-3 flex items-center gap-2">
            <FileBadge className="w-4 h-4 text-cyan-500" /> Mes certificats ({certificates.length})
          </h2>
          <div className="flex flex-wrap gap-2">
            {certificates.map(c => (
              <span key={c.id} className="inline-flex items-center gap-1.5 text-xs font-semibold text-cyan-600 dark:text-cyan-400 bg-cyan-500/10 px-3 py-1.5 rounded-full">
                <Award className="w-3.5 h-3.5" /> {c.numero} · {c.scoreFinal}%
              </span>
            ))}
          </div>
        </div>
      )}

      {showCreate && isAdmin && (
        <div className="glass-card p-4 space-y-4">
          <h2 className="text-sm font-semibold text-gray-900 dark:text-gray-100">Nouveau cours</h2>
          <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
            <div>
              <label className="text-xs font-medium text-gray-500 mb-1 block">Titre</label>
              <input
                type="text"
                value={courseForm.titre}
                onChange={e => setCourseForm({ ...courseForm, titre: e.target.value })}
                className="input w-full"
                placeholder="Titre du cours"
              />
            </div>
            <div>
              <label className="text-xs font-medium text-gray-500 mb-1 block">Catégorie</label>
              <select value={courseForm.categorie} onChange={e => setCourseForm({ ...courseForm, categorie: e.target.value })} className="input w-full">
                <option value="DISCIPOLAT">Discipolat</option>
                <option value="SPIRITUEL">Spirituel</option>
                <option value="MINISTERE">Ministère</option>
                <option value="LEADERSHIP">Leadership</option>
              </select>
            </div>
            <div>
              <label className="text-xs font-medium text-gray-500 mb-1 block">Niveau</label>
              <select value={courseForm.niveau} onChange={e => setCourseForm({ ...courseForm, niveau: e.target.value as Course['niveau'] })} className="input w-full">
                <option value="DEBUTANT">Débutant</option>
                <option value="INTERMEDIAIRE">Intermédiaire</option>
                <option value="AVANCE">Avancé</option>
              </select>
            </div>
          </div>
          <div>
            <label className="text-xs font-medium text-gray-500 mb-1 block">Description</label>
            <textarea
              rows={2}
              value={courseForm.description}
              onChange={e => setCourseForm({ ...courseForm, description: e.target.value })}
              className="input w-full"
              placeholder="Description du cours…"
            />
          </div>
          <div className="flex gap-2">
            <button
              onClick={() => courseForm.titre && createCourseMutation.mutate(courseForm)}
              disabled={!courseForm.titre}
              className="btn btn-primary text-sm"
            >
              Créer
            </button>
            <button onClick={() => setShowCreate(false)} className="btn text-sm">Annuler</button>
          </div>
        </div>
      )}

      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
        {coursesQuery.isLoading ? (
          <div className="col-span-full glass-card p-8 text-center text-sm text-gray-400">Chargement…</div>
        ) : courses.length === 0 ? (
          <div className="col-span-full glass-card p-12 text-center">
            <BookOpen className="w-10 h-10 text-gray-300 mx-auto mb-3" />
            <p className="text-sm text-gray-500 dark:text-gray-400">Aucune formation disponible pour le moment.</p>
          </div>
        ) : (
          courses.map(course => {
            const enrollment = enrollmentFor(course.id);
            return (
              <button
                key={course.id}
                onClick={() => setSelected(course)}
                className="glass-card p-5 text-left hover:shadow-lg transition-shadow group"
              >
                <div className="flex items-start justify-between mb-3">
                  <span className="p-2.5 rounded-xl bg-primary-500/10 text-primary-500">
                    <GraduationCap className="w-5 h-5" />
                  </span>
                  <span className={`text-[10px] font-bold px-2 py-1 rounded-full ${NIVEAU_STYLE[course.niveau]}`}>
                    {NIVEAU_LABEL[course.niveau]}
                  </span>
                </div>
                <h3 className="text-sm font-semibold text-gray-900 dark:text-gray-100 group-hover:text-primary-600 transition-colors">
                  {course.titre}
                </h3>
                <p className="text-xs text-gray-500 dark:text-gray-400 mt-1 line-clamp-2">{course.description}</p>
                <div className="flex flex-wrap items-center gap-3 mt-3 text-[11px] text-gray-400">
                  <span className="flex items-center gap-1"><Layers className="w-3 h-3" /> {course.nbModules}</span>
                  <span className="flex items-center gap-1"><Clock className="w-3 h-3" /> {course.dureeMinutes} min</span>
                  <span className="flex items-center gap-1"><Users className="w-3 h-3" /> {course.nbInscrits}</span>
                </div>
                {enrollment && (
                  <div className="mt-3">
                    <div className="h-1.5 bg-gray-100 dark:bg-gray-800 rounded-full overflow-hidden">
                      <div className={`h-full rounded-full ${enrollment.progression >= 100 ? 'bg-emerald-500' : 'bg-primary-500'}`} style={{ width: `${enrollment.progression}%` }} />
                    </div>
                    <p className="text-[10px] text-gray-400 mt-1 text-right">{enrollment.progression}%</p>
                  </div>
                )}
                <span className="inline-flex items-center gap-1 text-xs font-medium text-primary-600 dark:text-primary-400 mt-2">
                  Ouvrir <ChevronRight className="w-3.5 h-3.5" />
                </span>
              </button>
            );
          })
        )}
      </div>
    </div>
  );
}
