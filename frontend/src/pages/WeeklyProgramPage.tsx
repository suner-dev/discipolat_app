import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import api, { getErrorMessage } from '@/lib/api';
import { useAuth } from '@/contexts/AuthContext';
import type { WeeklyProgramTemplate, Evenement, JourSemaine, TypeEvenement } from '@/types';
import {
  Calendar, Plus, Save, Loader2, X, Clock, MapPin, Sparkles,
  CheckCircle2, Trash2, ToggleLeft, ToggleRight, Sun, Moon,
  Sunrise, Sunset, ChevronLeft, ChevronRight, Pencil,
  Church, BookOpen, Flame, Coffee, Music,
} from 'lucide-react';
import toast from 'react-hot-toast';

const JOUR_LABELS: Record<JourSemaine, string> = {
  LUNDI: 'Lundi', MARDI: 'Mardi', MERCREDI: 'Mercredi',
  JEUDI: 'Jeudi', VENDREDI: 'Vendredi', SAMEDI: 'Samedi', DIMANCHE: 'Dimanche',
};

const TYPE_LABELS: Record<string, string> = {
  CULTE: 'Culte', ETUDE_BIBLIQUE: 'Étude biblique', VEILLEE: 'Veillée',
  PRIERE: 'Temps de prière', REUNION: 'Réunion', SORTIE: 'Sortie',
  RETRAITE: 'Retraite', EVANGELISATION: 'Évangélisation', VISITE: 'Visite',
  CONFERENCE: 'Conférence', FORMATION: 'Formation', ANNIVERSAIRE: 'Anniversaire',
  AUTRE: 'Autre',
};

const TYPE_ICONS: Record<string, typeof Church> = {
  CULTE: Church, ETUDE_BIBLIQUE: BookOpen, VEILLEE: Moon,
  PRIERE: Flame, REUNION: Coffee, SORTIE: Sunrise,
};

const JOUR_ORDER: JourSemaine[] = ['LUNDI', 'MARDI', 'MERCREDI', 'JEUDI', 'VENDREDI', 'SAMEDI', 'DIMANCHE'];

const DEFAULT_COLORS: Record<string, string> = {
  CULTE: '#16a34a', ETUDE_BIBLIQUE: '#2563eb', VEILLEE: '#7c3aed',
  PRIERE: '#d97706', REUNION: '#0891b2', AUTRE: '#6b7280',
};

function getMonday(d: Date): Date {
  const date = new Date(d);
  const day = date.getDay();
  const diff = date.getDate() - day + (day === 0 ? -6 : 1);
  date.setDate(diff);
  date.setHours(0, 0, 0, 0);
  return date;
}

function formatDate(d: Date): string {
  return d.toISOString().split('T')[0];
}

export default function WeeklyProgramPage() {
  const { user } = useAuth();
  const queryClient = useQueryClient();
  const [currentWeekStart, setCurrentWeekStart] = useState(() => getMonday(new Date()));
  const [showCreateTemplate, setShowCreateTemplate] = useState(false);
  const [editingTemplate, setEditingTemplate] = useState<WeeklyProgramTemplate | null>(null);
  const [templateForm, setTemplateForm] = useState({
    titre: '', description: '', typeEvenement: 'CULTE' as string,
    jourSemaine: 'DIMANCHE' as JourSemaine, heureDebut: '09:00',
    heureFin: '', lieu: '', dureeMinutes: 120, couleur: '#16a34a',
  });
  const [showConfirmDelete, setShowConfirmDelete] = useState<string | null>(null);
  const [selectedEvent, setSelectedEvent] = useState<Evenement | null>(null);

  const weekParam = formatDate(currentWeekStart);

  const { data: templates, isLoading: templatesLoading } = useQuery({
    queryKey: ['events', 'templates'],
    queryFn: async () => {
      const res = await api.get('/events/templates');
      return res.data as WeeklyProgramTemplate[];
    },
  });

  const { data: weekProgram, isLoading: programLoading } = useQuery({
    queryKey: ['events', 'program', weekParam],
    queryFn: async () => {
      const res = await api.get(`/events/program/week?semaine=${weekParam}`);
      return res.data as Evenement[];
    },
  });

  // Mutations
  const createTemplateMutation = useMutation({
    mutationFn: async (data: typeof templateForm) => {
      await api.post('/events/templates', {
        ...data,
        actif: true,
        dureeMinutes: parseInt(String(data.dureeMinutes)) || 120,
      });
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['events', 'templates'] });
      toast.success('Template créé');
      setShowCreateTemplate(false);
      resetForm();
    },
    onError: (err) => toast.error(getErrorMessage(err)),
  });

  const updateTemplateMutation = useMutation({
    mutationFn: async ({ id, data }: { id: string; data: typeof templateForm }) => {
      await api.put(`/events/templates/${id}`, data);
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['events', 'templates'] });
      toast.success('Template mis à jour');
      setEditingTemplate(null);
      resetForm();
    },
    onError: (err) => toast.error(getErrorMessage(err)),
  });

  const deleteTemplateMutation = useMutation({
    mutationFn: async (id: string) => {
      await api.delete(`/events/templates/${id}`);
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['events', 'templates'] });
      toast.success('Template supprimé');
      setShowConfirmDelete(null);
    },
    onError: (err) => toast.error(getErrorMessage(err)),
  });

  const toggleTemplateMutation = useMutation({
    mutationFn: async ({ id, actif }: { id: string; actif: boolean }) => {
      await api.patch(`/events/templates/${id}/toggle`, { actif: !actif });
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['events', 'templates'] });
    },
    onError: (err) => toast.error(getErrorMessage(err)),
  });

  const generateWeekMutation = useMutation({
    mutationFn: async (semaine: string) => {
      const res = await api.post(`/events/program/generate?semaine=${semaine}`);
      return res.data as Evenement[];
    },
    onSuccess: (data) => {
      queryClient.invalidateQueries({ queryKey: ['events', 'program'] });
      queryClient.invalidateQueries({ queryKey: ['events'] });
      toast.success(`${data.length} événement(s) créé(s) pour cette semaine`);
    },
    onError: (err) => toast.error(getErrorMessage(err)),
  });

  const generateMonthMutation = useMutation({
    mutationFn: async () => {
      const res = await api.post('/events/program/generate-month');
      return res.data as Evenement[];
    },
    onSuccess: (data) => {
      queryClient.invalidateQueries({ queryKey: ['events', 'program'] });
      queryClient.invalidateQueries({ queryKey: ['events'] });
      toast.success(`${data.length} événement(s) créé(s) pour le mois`);
    },
    onError: (err) => toast.error(getErrorMessage(err)),
  });

  const resetForm = () => {
    setTemplateForm({
      titre: '', description: '', typeEvenement: 'CULTE',
      jourSemaine: 'DIMANCHE', heureDebut: '09:00',
      heureFin: '', lieu: '', dureeMinutes: 120, couleur: '#16a34a',
    });
  };

  const openEdit = (t: WeeklyProgramTemplate) => {
    setEditingTemplate(t);
    setTemplateForm({
      titre: t.titre,
      description: t.description || '',
      typeEvenement: t.typeEvenement,
      jourSemaine: t.jourSemaine,
      heureDebut: t.heureDebut.slice(0, 5),
      heureFin: t.heureFin ? t.heureFin.slice(0, 5) : '',
      lieu: t.lieu || '',
      dureeMinutes: t.dureeMinutes || 120,
      couleur: t.couleur || DEFAULT_COLORS[t.typeEvenement] || '#16a34a',
    });
    setShowCreateTemplate(true);
  };

  const handleSubmitTemplate = () => {
    if (!templateForm.titre) return;
    if (editingTemplate) {
      updateTemplateMutation.mutate({ id: editingTemplate.id, data: templateForm });
    } else {
      createTemplateMutation.mutate(templateForm);
    }
  };

  const navigateWeek = (direction: number) => {
    const newDate = new Date(currentWeekStart);
    newDate.setDate(newDate.getDate() + direction * 7);
    setCurrentWeekStart(newDate);
  };

  // Generate week days
  const weekDays = JOUR_ORDER.map((jour, idx) => {
    const d = new Date(currentWeekStart);
    d.setDate(d.getDate() + idx);
    return { jour, date: d, dateStr: formatDate(d) };
  });

  // Get events for a specific day
  const getEventsForDay = (dateStr: string) => {
    return (weekProgram || []).filter((e) => e.dateDebut.startsWith(dateStr));
  };

  const weekLabel = `${weekDays[0].date.toLocaleDateString('fr-FR', { day: 'numeric', month: 'long' })} - ${weekDays[6].date.toLocaleDateString('fr-FR', { day: 'numeric', month: 'long', year: 'numeric' })}`;

  return (
    <div className="page-container">
      <div className="page-header">
        <div className="animate-fade-in">
          <div className="flex items-center gap-2 mb-1">
            <Calendar className="w-5 h-5 text-primary-500" />
            <h1 className="page-title">Programme hebdomadaire</h1>
          </div>
          <p className="page-subtitle">Définissez les événements récurrents et générez le programme</p>
        </div>
        <div className="flex gap-2 animate-fade-in">
          {user?.role === 'PASTEUR' && (
            <>
              <button
                onClick={() => generateMonthMutation.mutate()}
                disabled={generateMonthMutation.isPending}
                className="btn-secondary btn-sm"
              >
                {generateMonthMutation.isPending ? <Loader2 className="w-4 h-4 animate-spin" /> : <Sparkles className="w-4 h-4" />}
                Générer le mois
              </button>
              <button
                onClick={() => generateWeekMutation.mutate(weekParam)}
                disabled={generateWeekMutation.isPending || !templates || templates.length === 0}
                className="btn-primary btn-sm"
              >
                {generateWeekMutation.isPending ? <Loader2 className="w-4 h-4 animate-spin" /> : <Calendar className="w-4 h-4" />}
                Générer la semaine
              </button>
            </>
          )}
        </div>
      </div>

      {/* Weekly navigation */}
      <div className="glass-card p-4 mb-6 animate-slide-up">
        <div className="flex items-center justify-between">
          <button onClick={() => navigateWeek(-1)} className="btn-ghost btn-sm">
            <ChevronLeft className="w-4 h-4" /> Semaine précédente
          </button>
          <div className="text-center">
            <p className="text-sm font-semibold text-gray-900 dark:text-gray-100">{weekLabel}</p>
            <p className="text-xs text-gray-400">Semaine du {weekParam}</p>
          </div>
          <button onClick={() => navigateWeek(1)} className="btn-ghost btn-sm">
            Semaine suivante <ChevronRight className="w-4 h-4" />
          </button>
        </div>
      </div>

      {/* Weekly calendar */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-7 gap-3 mb-8 animate-slide-up">
        {weekDays.map(({ jour, date, dateStr }) => {
          const dayEvents = getEventsForDay(dateStr);
          const isToday = formatDate(new Date()) === dateStr;
          return (
            <div
              key={jour}
              className={`glass-card p-3 ${isToday ? 'ring-2 ring-primary-500/30' : ''}`}
            >
              <div className="text-center mb-2 pb-2 border-b border-white/10 dark:border-white/[0.04]">
                <p className="text-xs font-semibold text-gray-500 dark:text-gray-400 uppercase">{JOUR_LABELS[jour]}</p>
                <p className={`text-lg font-bold ${isToday ? 'text-primary-500' : 'text-gray-900 dark:text-gray-100'}`}>
                  {date.getDate()}
                </p>
              </div>
              <div className="space-y-1.5 min-h-[80px]">
                {dayEvents.length > 0 ? (
                  dayEvents.map((evt) => (
                    <div
                      key={evt.id}
                      onClick={() => setSelectedEvent(evt)}
                      className="p-1.5 rounded-lg text-[10px] leading-tight cursor-pointer hover:scale-[1.02] transition-transform"
                      style={{
                        backgroundColor: `${TYPE_COLORS[evt.typeEvenement] || '#6b7280'}20`,
                        borderLeft: `3px solid ${TYPE_COLORS[evt.typeEvenement] || '#6b7280'}`,
                      }}
                      title={evt.titre}
                    >
                      <p className="font-semibold truncate text-gray-900 dark:text-gray-100">
                        {new Date(evt.dateDebut).toLocaleTimeString('fr-FR', { hour: '2-digit', minute: '2-digit' })}
                      </p>
                      <p className="truncate text-gray-600 dark:text-gray-400">{evt.titre}</p>
                      {evt.lieu && (
                        <p className="truncate text-gray-400 flex items-center gap-0.5">
                          <MapPin className="w-2.5 h-2.5" /> {evt.lieu}
                        </p>
                      )}
                    </div>
                  ))
                ) : (
                  <p className="text-[10px] text-gray-300 dark:text-gray-600 text-center pt-4">—</p>
                )}
              </div>
            </div>
          );
        })}
      </div>

      {/* Template management (Pasteur only) */}
      {user?.role === 'PASTEUR' && (
        <>
          <div className="page-header mt-8">
            <h2 className="text-lg font-bold text-gray-900 dark:text-gray-100 flex items-center gap-2">
              <Sparkles className="w-5 h-5 text-primary-500" /> Templates d'événements récurrents
            </h2>
            <button
              onClick={() => { setEditingTemplate(null); resetForm(); setShowCreateTemplate(true); }}
              className="btn-primary btn-sm"
            >
              <Plus className="w-4 h-4" /> Nouveau template
            </button>
          </div>

          {/* Create/Edit template modal */}
          {showCreateTemplate && (
            <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50" onClick={() => setShowCreateTemplate(false)}>
              <div className="card p-6 w-full max-w-lg mx-4 animate-slide-up" onClick={(e) => e.stopPropagation()}>
                <div className="flex items-center justify-between mb-4">
                  <h3 className="text-lg font-semibold text-gray-900 dark:text-gray-100">
                    {editingTemplate ? 'Modifier le template' : 'Nouveau template'}
                  </h3>
                  <button onClick={() => setShowCreateTemplate(false)} className="p-1 rounded-lg hover:bg-gray-100 dark:hover:bg-gray-800">
                    <X className="w-5 h-5 text-gray-500" />
                  </button>
                </div>
                <div className="space-y-4">
                  <div>
                    <label className="label">Titre *</label>
                    <input className="input" value={templateForm.titre}
                      onChange={(e) => setTemplateForm({ ...templateForm, titre: e.target.value })}
                      placeholder="Ex: Culte du dimanche" />
                  </div>
                  <div>
                    <label className="label">Description</label>
                    <textarea className="input" rows={2} value={templateForm.description}
                      onChange={(e) => setTemplateForm({ ...templateForm, description: e.target.value })} />
                  </div>
                  <div className="grid grid-cols-2 gap-4">
                    <div>
                      <label className="label">Type</label>
                      <select className="input" value={templateForm.typeEvenement}
                        onChange={(e) => setTemplateForm({
                          ...templateForm, typeEvenement: e.target.value,
                          couleur: DEFAULT_COLORS[e.target.value] || templateForm.couleur,
                        })}>
                        {Object.entries(TYPE_LABELS).map(([k, v]) => (
                          <option key={k} value={k}>{v}</option>
                        ))}
                      </select>
                    </div>
                    <div>
                      <label className="label">Jour</label>
                      <select className="input" value={templateForm.jourSemaine}
                        onChange={(e) => setTemplateForm({ ...templateForm, jourSemaine: e.target.value as JourSemaine })}>
                        {JOUR_ORDER.map((j) => (
                          <option key={j} value={j}>{JOUR_LABELS[j]}</option>
                        ))}
                      </select>
                    </div>
                  </div>
                  <div className="grid grid-cols-2 gap-4">
                    <div>
                      <label className="label">Heure début</label>
                      <input type="time" className="input" value={templateForm.heureDebut}
                        onChange={(e) => setTemplateForm({ ...templateForm, heureDebut: e.target.value })} />
                    </div>
                    <div>
                      <label className="label">Heure fin</label>
                      <input type="time" className="input" value={templateForm.heureFin}
                        onChange={(e) => setTemplateForm({ ...templateForm, heureFin: e.target.value })} />
                    </div>
                  </div>
                  <div className="grid grid-cols-2 gap-4">
                    <div>
                      <label className="label">Durée (minutes)</label>
                      <input type="number" min={15} step={15} className="input" value={templateForm.dureeMinutes}
                        onChange={(e) => setTemplateForm({ ...templateForm, dureeMinutes: parseInt(e.target.value) || 120 })} />
                    </div>
                    <div>
                      <label className="label">Lieu</label>
                      <input className="input" value={templateForm.lieu}
                        onChange={(e) => setTemplateForm({ ...templateForm, lieu: e.target.value })}
                        placeholder="Ex: Temple" />
                    </div>
                  </div>
                  <div>
                    <label className="label">Couleur (affichage calendrier)</label>
                    <input type="color" className="input h-10 p-1" value={templateForm.couleur}
                      onChange={(e) => setTemplateForm({ ...templateForm, couleur: e.target.value })} />
                  </div>
                  <div className="flex justify-end gap-3 pt-4 border-t border-gray-200 dark:border-gray-700">
                    <button onClick={() => setShowCreateTemplate(false)} className="btn-secondary btn-sm">Annuler</button>
                    <button onClick={handleSubmitTemplate}
                      disabled={!templateForm.titre || createTemplateMutation.isPending || updateTemplateMutation.isPending}
                      className="btn-primary btn-sm">
                      {(createTemplateMutation.isPending || updateTemplateMutation.isPending) ? (
                        <Loader2 className="w-4 h-4 animate-spin" />
                      ) : editingTemplate ? <Pencil className="w-4 h-4" /> : <Plus className="w-4 h-4" />}
                      {editingTemplate ? 'Enregistrer' : 'Créer'}
                    </button>
                  </div>
                </div>
              </div>
            </div>
          )}

          {/* Template list */}
          {templatesLoading ? (
            <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
              {[1,2,3].map(i => (
                <div key={i} className="glass-card p-5 animate-pulse">
                  <div className="skeleton h-5 w-32 mb-3" />
                  <div className="skeleton h-3 w-24" />
                </div>
              ))}
            </div>
          ) : templates && templates.length > 0 ? (
            <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4 animate-slide-up">
              {templates.map((template) => {
                const TypeIcon = TYPE_ICONS[template.typeEvenement] || Church;
                return (
                  <div
                    key={template.id}
                    className={`glass-card p-4 relative overflow-hidden ${!template.actif ? 'opacity-60' : ''}`}
                  >
                    <div className="absolute top-0 left-0 w-1 h-full" style={{ backgroundColor: template.couleur || '#16a34a' }} />
                    <div className="flex items-start justify-between">
                      <div className="flex items-center gap-3">
                        <div className="w-10 h-10 rounded-xl flex items-center justify-center"
                          style={{ backgroundColor: `${template.couleur || '#16a34a'}20` }}>
                          <TypeIcon className="w-5 h-5" style={{ color: template.couleur || '#16a34a' }} />
                        </div>
                        <div>
                          <p className="text-sm font-semibold text-gray-900 dark:text-gray-100">{template.titre}</p>
                          <p className="text-xs text-gray-400">{JOUR_LABELS[template.jourSemaine]} à {template.heureDebut.slice(0, 5)}</p>
                        </div>
                      </div>
                      <div className="flex gap-1">
                        <button
                          onClick={() => toggleTemplateMutation.mutate({ id: template.id, actif: template.actif })}
                          className="p-1.5 rounded-lg hover:bg-gray-100 dark:hover:bg-gray-800 transition-colors"
                          title={template.actif ? 'Désactiver' : 'Activer'}
                        >
                          {template.actif
                            ? <ToggleRight className="w-4 h-4 text-green-500" />
                            : <ToggleLeft className="w-4 h-4 text-gray-400" />}
                        </button>
                        <button
                          onClick={() => openEdit(template)}
                          className="p-1.5 rounded-lg hover:bg-gray-100 dark:hover:bg-gray-800 transition-colors"
                        >
                          <Pencil className="w-4 h-4 text-blue-500" />
                        </button>
                        <button
                          onClick={() => setShowConfirmDelete(template.id)}
                          className="p-1.5 rounded-lg hover:bg-gray-100 dark:hover:bg-gray-800 transition-colors"
                        >
                          <Trash2 className="w-4 h-4 text-red-500" />
                        </button>
                      </div>
                    </div>
                    {template.lieu && (
                      <p className="text-xs text-gray-400 mt-2 flex items-center gap-1">
                        <MapPin className="w-3 h-3" /> {template.lieu}
                      </p>
                    )}
                    {template.description && (
                      <p className="text-xs text-gray-500 dark:text-gray-400 mt-1">{template.description}</p>
                    )}
                  </div>
                );
              })}
            </div>
          ) : (
            <div className="glass-card p-10 text-center">
              <Sparkles className="w-12 h-12 text-gray-300 mx-auto mb-3" />
              <p className="text-gray-500">Aucun template. Créez votre premier template pour générer le programme automatiquement.</p>
            </div>
          )}

          {/* Delete confirmation */}
          {showConfirmDelete && (
            <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50" onClick={() => setShowConfirmDelete(null)}>
              <div className="card p-6 w-full max-w-sm mx-4 animate-slide-up" onClick={(e) => e.stopPropagation()}>
                <div className="flex items-center gap-3 mb-4">
                  <div className="w-10 h-10 rounded-full bg-red-100 dark:bg-red-900/30 flex items-center justify-center">
                    <Trash2 className="w-5 h-5 text-red-600" />
                  </div>
                  <div>
                    <h3 className="text-lg font-semibold">Confirmer la suppression</h3>
                    <p className="text-sm text-gray-500">Ce template sera définitivement supprimé.</p>
                  </div>
                </div>
                <div className="flex justify-end gap-2">
                  <button onClick={() => setShowConfirmDelete(null)} className="btn-secondary btn-sm">Annuler</button>
                  <button
                    onClick={() => deleteTemplateMutation.mutate(showConfirmDelete)}
                    disabled={deleteTemplateMutation.isPending}
                    className="btn-primary btn-sm bg-red-600 hover:bg-red-700"
                  >
                    {deleteTemplateMutation.isPending ? <Loader2 className="w-4 h-4 animate-spin" /> : <Trash2 className="w-4 h-4" />}
                    Supprimer
                  </button>
                </div>
              </div>
            </div>
          )}
        </>
      )}

      {/* Event detail modal */}
      {selectedEvent && (
        <div
          className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 dark:bg-black/70 backdrop-blur-sm p-4"
          onClick={() => setSelectedEvent(null)}
        >
          <div className="card p-6 w-full max-w-md mx-4 animate-slide-up" onClick={(e) => e.stopPropagation()}>
            <div className="flex items-start justify-between gap-3 mb-4">
              <div className="flex items-center gap-3">
                <div className="w-10 h-10 rounded-full flex items-center justify-center text-white"
                  style={{ backgroundColor: TYPE_COLORS[selectedEvent.typeEvenement] || '#6b7280' }}>
                  {(() => { const Icon = TYPE_ICONS[selectedEvent.typeEvenement] || Church; return <Icon className="w-5 h-5" />; })()}
                </div>
                <div>
                  <h3 className="text-lg font-semibold text-gray-900 dark:text-gray-100">{selectedEvent.titre}</h3>
                  <p className="text-xs text-gray-400 uppercase tracking-wider">{TYPE_LABELS[selectedEvent.typeEvenement] || selectedEvent.typeEvenement}</p>
                </div>
              </div>
              <button onClick={() => setSelectedEvent(null)} className="p-2 rounded-xl hover:bg-gray-100 dark:hover:bg-gray-800/50 transition-colors">
                <X className="w-5 h-5 text-gray-400" />
              </button>
            </div>

            <div className="space-y-3 text-sm">
              <div className="flex items-center gap-2 text-gray-600 dark:text-gray-300">
                <Clock className="w-4 h-4 text-gray-400" />
                <span>
                  {new Date(selectedEvent.dateDebut).toLocaleDateString('fr-FR', { weekday: 'long', day: 'numeric', month: 'long' })}
                  {' à '}
                  {new Date(selectedEvent.dateDebut).toLocaleTimeString('fr-FR', { hour: '2-digit', minute: '2-digit' })}
                  {selectedEvent.dateFin && ` — ${new Date(selectedEvent.dateFin).toLocaleTimeString('fr-FR', { hour: '2-digit', minute: '2-digit' })}`}
                </span>
              </div>
              {selectedEvent.lieu && (
                <div className="flex items-center gap-2 text-gray-600 dark:text-gray-300">
                  <MapPin className="w-4 h-4 text-gray-400" /> {selectedEvent.lieu}
                </div>
              )}
              {selectedEvent.description && (
                <p className="text-gray-600 dark:text-gray-300">{selectedEvent.description}</p>
              )}
              <div className="flex items-center gap-2 text-gray-500 dark:text-gray-400 text-xs">
                <span className="font-medium">{selectedEvent.nbInscrits}</span> inscrit(s)
                {selectedEvent.limitePlaces && <span>· place limitée à {selectedEvent.limitePlaces}</span>}
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

const TYPE_COLORS: Record<string, string> = {
  CULTE: '#16a34a', ETUDE_BIBLIQUE: '#2563eb', VEILLEE: '#7c3aed',
  PRIERE: '#d97706', REUNION: '#0891b2', SORTIE: '#ea580c',
  RETRAITE: '#8b5cf6', EVANGELISATION: '#f97316', VISITE: '#06b6d4',
  CONFERENCE: '#6366f1', FORMATION: '#f59e0b', ANNIVERSAIRE: '#ec4899',
  AUTRE: '#6b7280',
};
