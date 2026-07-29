import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import api, { getErrorMessage } from '@/lib/api';
import { useAuth } from '@/contexts/AuthContext';
import DataTable from '@/components/shared/DataTable';
import type { Evenement, PageResponse, TypeEvenement, StatutEvenement, User } from '@/types';
import type { ColumnDef } from '@/types/table';
import {
  Calendar,
  Plus,
  Search,
  Filter,
  MapPin,
  Clock,
  Users,
  Loader2,
  X,
  CheckCircle2,
  UserMinus,
  Pencil,
  Trash2,
  Eye,
  UserCheck,
  UserX,
} from 'lucide-react';
import toast from 'react-hot-toast';

const TYPE_LABELS: Record<TypeEvenement, string> = {
  SORTIE: 'Sortie',
  RETRAITE: 'Retraite',
  EVANGELISATION: 'Évangélisation',
  REUNION: 'Réunion',
  VISITE: 'Visite',
  CONFERENCE: 'Conférence',
  FORMATION: 'Formation',
  ANNIVERSAIRE: 'Anniversaire',
  AUTRE: 'Autre',
};

const TYPE_COLORS: Record<TypeEvenement, string> = {
  SORTIE: 'bg-green-100 text-green-800 dark:bg-green-900/30 dark:text-green-300',
  RETRAITE: 'bg-purple-100 text-purple-800 dark:bg-purple-900/30 dark:text-purple-300',
  EVANGELISATION: 'bg-orange-100 text-orange-800 dark:bg-orange-900/30 dark:text-orange-300',
  REUNION: 'bg-blue-100 text-blue-800 dark:bg-blue-900/30 dark:text-blue-300',
  VISITE: 'bg-cyan-100 text-cyan-800 dark:bg-cyan-900/30 dark:text-cyan-300',
  CONFERENCE: 'bg-indigo-100 text-indigo-800 dark:bg-indigo-900/30 dark:text-indigo-300',
  FORMATION: 'bg-amber-100 text-amber-800 dark:bg-amber-900/30 dark:text-amber-300',
  ANNIVERSAIRE: 'bg-pink-100 text-pink-800 dark:bg-pink-900/30 dark:text-pink-300',
  AUTRE: 'bg-gray-100 text-gray-800 dark:bg-gray-700 dark:text-gray-300',
};

const STATUT_LABELS: Record<StatutEvenement, string> = {
  PLANIFIE: 'Planifié',
  EN_COURS: 'En cours',
  TERMINE: 'Terminé',
  ANNULE: 'Annulé',
};

interface EventFormState {
  titre: string;
  description: string;
  typeEvenement: TypeEvenement;
  dateDebut: string;
  dateFin: string;
  lieu: string;
  limitePlaces: number | undefined;
  statut?: StatutEvenement;
}

function eventForm(
  key: string,
  form: EventFormState,
  update: (partial: Partial<EventFormState>) => void,
  submitLabel: string,
  onSubmit: () => void,
  isPending: boolean,
  onCancel?: () => void
) {
  const isEdit = submitLabel !== 'Créer';
  return (
    <div key={key} className="card p-6 mb-6 animate-slide-up">
      <div className="flex items-center justify-between mb-4">
        <h3 className="text-lg font-semibold text-gray-900 dark:text-gray-100">
          {isEdit ? "Modifier l'événement" : 'Nouvel événement'}
        </h3>
        {onCancel && (
          <button onClick={onCancel} className="p-1 rounded-lg hover:bg-gray-100 dark:hover:bg-gray-800">
            <X className="w-5 h-5 text-gray-500" />
          </button>
        )}
      </div>
      <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
        <div className="sm:col-span-2">
          <label className="label">Titre *</label>
          <input className="input" value={form.titre} onChange={(e) => update({ titre: e.target.value })} placeholder="Ex: Retraite de prière" />
        </div>
        <div className="sm:col-span-2">
          <label className="label">Description</label>
          <textarea className="input" rows={2} value={form.description} onChange={(e) => update({ description: e.target.value })} />
        </div>
        <div>
          <label className="label">Type *</label>
          <select className="input" value={form.typeEvenement} onChange={(e) => update({ typeEvenement: e.target.value as TypeEvenement })}>
            {Object.entries(TYPE_LABELS).map(([key, label]) => (
              <option key={key} value={key}>{label}</option>
            ))}
          </select>
        </div>
        <div>
          <label className="label">Lieu</label>
          <input className="input" value={form.lieu} onChange={(e) => update({ lieu: e.target.value })} placeholder="Ex: Salle paroissiale" />
        </div>
        <div>
          <label className="label">Date de début *</label>
          <input type="datetime-local" className="input" value={form.dateDebut} onChange={(e) => update({ dateDebut: e.target.value })} />
        </div>
        <div>
          <label className="label">Date de fin</label>
          <input type="datetime-local" className="input" value={form.dateFin} onChange={(e) => update({ dateFin: e.target.value })} />
        </div>
        <div>
          <label className="label">Limite de places</label>
          <input type="number" min={1} className="input" value={form.limitePlaces || ''} onChange={(e) => update({ limitePlaces: e.target.value ? parseInt(e.target.value) : undefined })} placeholder="Illimité si vide" />
        </div>
        {isEdit && form.statut && (
          <div>
            <label className="label">Statut</label>
            <select className="input" value={form.statut} onChange={(e) => update({ statut: e.target.value as StatutEvenement })}>
              {Object.entries(STATUT_LABELS).map(([key, label]) => (
                <option key={key} value={key}>{label}</option>
              ))}
            </select>
          </div>
        )}
      </div>
      <div className="flex justify-end gap-3 mt-4">
        {onCancel && <button onClick={onCancel} className="btn-secondary btn-sm">Annuler</button>}
        <button onClick={onSubmit} disabled={!form.titre || isPending} className="btn-primary btn-sm">
          {isPending ? <Loader2 className="w-4 h-4 animate-spin" /> : isEdit ? <Pencil className="w-4 h-4" /> : <Plus className="w-4 h-4" />}
          {submitLabel}
        </button>
      </div>
    </div>
  );
}

export default function EventsPage() {
  const { user } = useAuth();
  const queryClient = useQueryClient();
  const [page, setPage] = useState(0);
  const [search, setSearch] = useState('');
  const [typeFilter, setTypeFilter] = useState<TypeEvenement | ''>('');
  const [statutFilter, setStatutFilter] = useState<StatutEvenement | ''>('');
  const [showFilters, setShowFilters] = useState(false);
  const [showCreate, setShowCreate] = useState(false);
  const [showEdit, setShowEdit] = useState(false);
  const [editingEvent, setEditingEvent] = useState<Evenement | null>(null);
  const [showDeleteConfirm, setShowDeleteConfirm] = useState<string | null>(null);
  const [showAttendance, setShowAttendance] = useState<Evenement | null>(null);
  const [newEvent, setNewEvent] = useState({
    titre: '',
    description: '',
    typeEvenement: 'REUNION' as TypeEvenement,
    dateDebut: new Date().toISOString().slice(0, 16),
    dateFin: '',
    lieu: '',
    limitePlaces: undefined as number | undefined,
  });
  const [editEvent, setEditEvent] = useState({
    titre: '',
    description: '',
    typeEvenement: 'REUNION' as TypeEvenement,
    dateDebut: '',
    dateFin: '',
    lieu: '',
    limitePlaces: undefined as number | undefined,
    statut: 'PLANIFIE' as StatutEvenement,
  });

  const { data, isLoading } = useQuery({
    queryKey: ['events', page, search, typeFilter, statutFilter],
    queryFn: async () => {
      const params = new URLSearchParams({ size: '20', page: String(page) });
      if (search) params.set('search', search);
      if (typeFilter) params.set('typeEvenement', typeFilter);
      if (statutFilter) params.set('statut', statutFilter);
      const res = await api.get(`/events?${params}`);
      return res.data as PageResponse<Evenement>;
    },
  });

  const createMutation = useMutation({
    mutationFn: async (evt: typeof newEvent) => {
      await api.post('/events', {
        ...evt,
        dateFin: evt.dateFin || undefined,
        lieu: evt.lieu || undefined,
        limitePlaces: evt.limitePlaces || undefined,
      });
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['events'] });
      toast.success('Événement créé');
      setShowCreate(false);
      setNewEvent({ titre: '', description: '', typeEvenement: 'REUNION', dateDebut: new Date().toISOString().slice(0, 16), dateFin: '', lieu: '', limitePlaces: undefined });
    },
    onError: (err) => toast.error(getErrorMessage(err)),
  });

  const registerMutation = useMutation({
    mutationFn: async (eventId: string) => {
      await api.post(`/events/${eventId}/register`);
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['events'] });
      toast.success('Inscription confirmée');
    },
    onError: (err) => toast.error(getErrorMessage(err)),
  });

  const updateEventMutation = useMutation({
    mutationFn: async ({ id, data }: { id: string; data: typeof editEvent }) => {
      await api.put(`/events/${id}`, {
        titre: data.titre,
        description: data.description || undefined,
        typeEvenement: data.typeEvenement,
        dateDebut: data.dateDebut,
        dateFin: data.dateFin || undefined,
        lieu: data.lieu || undefined,
        limitePlaces: data.limitePlaces || undefined,
        statut: data.statut,
      });
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['events'] });
      toast.success('Événement mis à jour');
      setShowEdit(false);
      setEditingEvent(null);
    },
    onError: (err) => toast.error(getErrorMessage(err)),
  });

  const deleteEventMutation = useMutation({
    mutationFn: async (id: string) => {
      await api.delete(`/events/${id}`);
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['events'] });
      toast.success('Événement supprimé');
      setShowDeleteConfirm(null);
    },
    onError: (err) => toast.error(getErrorMessage(err)),
  });

  // User lookup cache
  const { data: allUsers } = useQuery({
    queryKey: ['users', 'all'],
    queryFn: async () => {
      const res = await api.get('/users?size=100');
      return res.data.content as User[];
    },
  });

  const getUserName = (userId: string) => {
    const user = allUsers?.find((u) => u.id === userId);
    return user ? `${user.firstName} ${user.lastName}` : userId.slice(0, 8) + '...';
  };

  const { data: attendanceData } = useQuery({
    queryKey: ['events', showAttendance?.id, 'registrations'],
    queryFn: async () => {
      const res = await api.get(`/events/${showAttendance?.id}/registrations`);
      return res.data as { id?: string; utilisateurId: string; statutInscription: string; present: boolean }[];
    },
    enabled: !!showAttendance,
  });

  const markAttendanceMutation = useMutation({
    mutationFn: async ({ eventId, userId, present }: { eventId: string; userId: string; present: boolean }) => {
      await api.post(`/events/${eventId}/attendance`, { userId, present });
    },
    onSuccess: (_data, variables) => {
      queryClient.invalidateQueries({ queryKey: ['events', variables.eventId, 'registrations'] });
      queryClient.invalidateQueries({ queryKey: ['events'] });
      toast.success('Présence enregistrée');
    },
    onError: (err) => toast.error(getErrorMessage(err)),
  });

  const openEdit = (evt: Evenement) => {
    setEditingEvent(evt);
    setEditEvent({
      titre: evt.titre,
      description: evt.description || '',
      typeEvenement: evt.typeEvenement,
      dateDebut: evt.dateDebut.slice(0, 16),
      dateFin: evt.dateFin ? evt.dateFin.slice(0, 16) : '',
      lieu: evt.lieu || '',
      limitePlaces: evt.limitePlaces,
      statut: evt.statut,
    });
    setShowEdit(true);
  };

  const columns: ColumnDef<Evenement>[] = [
    {
      header: 'Événement',
      cell: (evt) => (
        <div>
          <p className="text-sm font-medium text-gray-900 dark:text-gray-100">{evt.titre}</p>
          {evt.description && (
            <p className="text-xs text-gray-500 dark:text-gray-400 truncate max-w-xs">{evt.description}</p>
          )}
        </div>
      ),
    },
    {
      header: 'Type',
      cell: (evt) => (
        <span className={`inline-flex items-center px-2 py-1 rounded-full text-xs font-medium ${TYPE_COLORS[evt.typeEvenement]}`}>
          {TYPE_LABELS[evt.typeEvenement]}
        </span>
      ),
    },
    {
      header: 'Date',
      cell: (evt) => (
        <div className="flex items-center gap-1 text-sm text-gray-600 dark:text-gray-400">
          <Clock className="w-3 h-3" />
          {new Date(evt.dateDebut).toLocaleDateString('fr-FR', { day: 'numeric', month: 'short', hour: '2-digit', minute: '2-digit' })}
        </div>
      ),
    },
    {
      header: 'Lieu',
      cell: (evt) => evt.lieu ? (
        <div className="flex items-center gap-1 text-sm text-gray-600 dark:text-gray-400">
          <MapPin className="w-3 h-3" />
          {evt.lieu}
        </div>
      ) : '-',
    },
    {
      header: 'Places',
      cell: (evt) => (
        <div className="flex items-center gap-1 text-sm text-gray-600 dark:text-gray-400">
          <Users className="w-3 h-3" />
          {evt.limitePlaces ? `${evt.nbInscrits}/${evt.limitePlaces}` : `${evt.nbInscrits} inscrits`}
        </div>
      ),
    },
    {
      header: 'Statut',
      cell: (evt) => (
        <span className={`badge ${evt.statut === 'PLANIFIE' ? 'badge-info' : evt.statut === 'TERMINE' ? 'badge-success' : evt.statut === 'ANNULE' ? 'badge-danger' : 'badge-warning'}`}>
          {STATUT_LABELS[evt.statut]}
        </span>
      ),
    },
    {
      header: 'Actions',
      cell: (evt) => {
        const isOrganizer = evt.organisateurId === user?.id;
        const canManage = isOrganizer || user?.role === 'PASTEUR' || user?.role === 'RESPONSABLE';
        return (
          <div className="flex items-center gap-1">
            {evt.statut === 'PLANIFIE' && (
              <button
                onClick={() => registerMutation.mutate(evt.id)}
                disabled={registerMutation.isPending}
                className="btn-ghost btn-sm text-primary-600"
                title="S'inscrire"
              >
                {registerMutation.isPending ? <Loader2 className="w-4 h-4 animate-spin" /> : <CheckCircle2 className="w-4 h-4" />}
              </button>
            )}
            <button
              onClick={() => setShowAttendance(evt)}
              className="btn-ghost btn-sm text-gray-600"
              title="Voir les inscriptions"
            >
              <Eye className="w-4 h-4" />
            </button>
            {canManage && (
              <>
                <button
                  onClick={() => openEdit(evt)}
                  className="btn-ghost btn-sm text-blue-600"
                  title="Modifier"
                >
                  <Pencil className="w-4 h-4" />
                </button>
                <button
                  onClick={() => setShowDeleteConfirm(evt.id)}
                  className="btn-ghost btn-sm text-red-600"
                  title="Supprimer"
                >
                  <Trash2 className="w-4 h-4" />
                </button>
              </>
            )}
          </div>
        );
      },
    },
  ];

  return (
    <div className="page-container">
      <div className="page-header">
        <div>
          <h1 className="page-title">Événements</h1>
          <p className="text-gray-500 dark:text-gray-400 mt-1">Gestion des événements de famille</p>
        </div>
        <div className="flex gap-2">
          <button onClick={() => setShowFilters(!showFilters)} className={`btn-secondary btn-sm ${showFilters ? 'bg-primary-50' : ''}`}>
            <Filter className="w-4 h-4" /> Filtres
          </button>
          <button onClick={() => setShowCreate(!showCreate)} className="btn-primary btn-sm">
            <Plus className="w-4 h-4" /> Nouvel événement
          </button>
        </div>
      </div>

      {/* Create form */}
      {showCreate && eventForm('create', newEvent, (v) => setNewEvent({ ...newEvent, ...v }), 'Créer', () => createMutation.mutate(newEvent), createMutation.isPending, () => setShowCreate(false))}

      {/* Edit form */}
      {showEdit && editingEvent && eventForm('edit', editEvent, (v) => setEditEvent({ ...editEvent, ...v }), 'Enregistrer', () => updateEventMutation.mutate({ id: editingEvent.id, data: editEvent }), updateEventMutation.isPending, () => { setShowEdit(false); setEditingEvent(null); })}

      {/* Attendance modal */}
      {showAttendance && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50" onClick={() => setShowAttendance(null)}>
          <div className="card p-6 w-full max-w-lg mx-4 animate-slide-up" onClick={(e) => e.stopPropagation()}>
            <div className="flex items-center justify-between mb-4">
              <div>
                <h3 className="text-lg font-semibold text-gray-900 dark:text-gray-100">{showAttendance.titre}</h3>
                <p className="text-sm text-gray-500 dark:text-gray-400">
                  {showAttendance.nbInscrits} inscrit{showAttendance.nbInscrits !== 1 ? 's' : ''}
                  {showAttendance.limitePlaces ? ` / ${showAttendance.limitePlaces} places` : ''}
                </p>
              </div>
              <button onClick={() => setShowAttendance(null)} className="p-1 rounded-lg hover:bg-gray-100 dark:hover:bg-gray-800">
                <X className="w-5 h-5 text-gray-500" />
              </button>
            </div>
            {/* Stats bar */}
            {attendanceData && attendanceData.length > 0 && (
              <div className="flex gap-3 mb-4 text-xs text-gray-500">
                <span>Total: <strong>{attendanceData.length}</strong></span>
                <span>Présents: <strong className="text-green-600">{attendanceData.filter(r => r.statutInscription === 'PRESENT').length}</strong></span>
                <span>Absents: <strong className="text-red-500">{attendanceData.filter(r => r.statutInscription === 'ABSENT').length}</strong></span>
              </div>
            )}
            {attendanceData && attendanceData.length > 0 ? (
              <div className="space-y-2">
                {attendanceData.map((reg, idx) => {
                  const isAnnulee = reg.statutInscription === 'ANNULEE';
                  const isPresent = reg.statutInscription === 'PRESENT';
                  const isAbsent = reg.statutInscription === 'ABSENT';
                  const isEnAttente = reg.statutInscription === 'EN_ATTENTE';
                  return (
                    <div key={reg.utilisateurId} className="flex items-center justify-between p-3 rounded-lg bg-gray-50 dark:bg-gray-800/50">
                      <div className="flex items-center gap-3">
                        <div className={`w-8 h-8 rounded-full flex items-center justify-center text-xs font-bold ${
                          isPresent ? 'bg-green-100 text-green-700' :
                          isAbsent ? 'bg-red-100 text-red-700' :
                          isAnnulee ? 'bg-gray-100 text-gray-500' :
                          'bg-amber-100 text-amber-700'
                        }`}>
                          {reg.utilisateurId.slice(0, 2).toUpperCase()}
                        </div>
                        <div>
                          <p className="text-sm font-medium text-gray-900 dark:text-gray-100">
                            {getUserName(reg.utilisateurId)}
                          </p>
                          <p className={`text-xs font-medium ${
                            isPresent ? 'text-green-600' :
                            isAbsent ? 'text-red-500' :
                            isAnnulee ? 'text-gray-400' :
                            'text-amber-600'
                          }`}>
                            {isPresent ? '✓ Présent' :
                             isAbsent ? '✗ Absent' :
                             isAnnulee ? 'Annulé' :
                             isEnAttente ? 'En attente' :
                             'Inscrit'}
                          </p>
                        </div>
                      </div>
                      {!isAnnulee && showAttendance && (
                        <div className="flex gap-1">
                          <button
                            onClick={() => markAttendanceMutation.mutate({
                              eventId: showAttendance.id,
                              userId: reg.utilisateurId,
                              present: true,
                            })}
                            disabled={markAttendanceMutation.isPending || isPresent}
                            className={`p-2 rounded-lg transition-colors ${
                              isPresent
                                ? 'bg-green-100 text-green-700 dark:bg-green-900/30 dark:text-green-300'
                                : 'hover:bg-green-50 text-gray-400 hover:text-green-600 dark:hover:bg-green-900/20'
                            }`}
                            title="Marquer présent"
                          >
                            <UserCheck className="w-4 h-4" />
                          </button>
                          <button
                            onClick={() => markAttendanceMutation.mutate({
                              eventId: showAttendance.id,
                              userId: reg.utilisateurId,
                              present: false,
                            })}
                            disabled={markAttendanceMutation.isPending || isAbsent}
                            className={`p-2 rounded-lg transition-colors ${
                              isAbsent
                                ? 'bg-red-100 text-red-700 dark:bg-red-900/30 dark:text-red-300'
                                : 'hover:bg-red-50 text-gray-400 hover:text-red-600 dark:hover:bg-red-900/20'
                            }`}
                            title="Marquer absent"
                          >
                            <UserX className="w-4 h-4" />
                          </button>
                        </div>
                      )}
                    </div>
                  );
                })}
              </div>
            ) : (
              <div className="text-center py-8">
                <Users className="w-8 h-8 text-gray-300 dark:text-gray-600 mx-auto mb-2" />
                <p className="text-sm text-gray-500 dark:text-gray-400">Aucune inscription pour le moment</p>
              </div>
            )}
            <div className="flex justify-end mt-4">
              <button onClick={() => setShowAttendance(null)} className="btn-secondary btn-sm">Fermer</button>
            </div>
          </div>
        </div>
      )}

      {/* Delete confirmation modal */}
      {showDeleteConfirm && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50" onClick={() => setShowDeleteConfirm(null)}>
          <div className="card p-6 w-full max-w-sm mx-4 animate-slide-up" onClick={(e) => e.stopPropagation()}>
            <div className="flex items-center gap-3 mb-4">
              <div className="w-10 h-10 rounded-full bg-red-100 dark:bg-red-900/30 flex items-center justify-center">
                <Trash2 className="w-5 h-5 text-red-600" />
              </div>
              <div>
                <h3 className="text-lg font-semibold text-gray-900 dark:text-gray-100">Confirmer la suppression</h3>
                <p className="text-sm text-gray-500 dark:text-gray-400">Cette action est irréversible.</p>
              </div>
            </div>
            <div className="flex justify-end gap-2">
              <button onClick={() => setShowDeleteConfirm(null)} className="btn-secondary btn-sm">Annuler</button>
              <button
                onClick={() => deleteEventMutation.mutate(showDeleteConfirm)}
                disabled={deleteEventMutation.isPending}
                className="btn-primary btn-sm bg-red-600 hover:bg-red-700"
              >
                {deleteEventMutation.isPending ? <Loader2 className="w-4 h-4 animate-spin" /> : <Trash2 className="w-4 h-4" />}
                Supprimer
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Search and filters */}
      <div className="card p-4 mb-6">
        <div className="flex flex-col sm:flex-row gap-3">
          <div className="flex-1 relative">
            <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400" />
            <input type="text" placeholder="Rechercher un événement..." value={search} onChange={(e) => { setSearch(e.target.value); setPage(0); }} className="input pl-10" />
          </div>
        </div>
        {showFilters && (
          <div className="flex flex-wrap gap-3 mt-3 pt-3 border-t border-gray-200 dark:border-gray-700">
            <select value={typeFilter} onChange={(e) => { setTypeFilter(e.target.value as TypeEvenement | ''); setPage(0); }} className="input w-auto">
              <option value="">Tous les types</option>
              {Object.entries(TYPE_LABELS).map(([key, label]) => (
                <option key={key} value={key}>{label}</option>
              ))}
            </select>
            <select value={statutFilter} onChange={(e) => { setStatutFilter(e.target.value as StatutEvenement | ''); setPage(0); }} className="input w-auto">
              <option value="">Tous les statuts</option>
              <option value="PLANIFIE">Planifié</option>
              <option value="EN_COURS">En cours</option>
              <option value="TERMINE">Terminé</option>
              <option value="ANNULE">Annulé</option>
            </select>
          </div>
        )}
      </div>

      <DataTable<Evenement>
        columns={columns}
        data={data?.content || []}
        isLoading={isLoading}
        emptyMessage="Aucun événement trouvé"
        emptyIcon={<Calendar className="w-12 h-12 text-gray-300 dark:text-gray-600 mx-auto mb-4" />}
      />

      {data && data.totalPages > 1 && (
        <div className="flex items-center justify-between mt-4">
          <p className="text-sm text-gray-500">Page {data.number + 1} / {data.totalPages}</p>
          <div className="flex gap-2">
            <button onClick={() => setPage(p => Math.max(0, p - 1))} disabled={data.first} className="btn-secondary btn-sm">Précédent</button>
            <button onClick={() => setPage(p => p + 1)} disabled={data.last} className="btn-secondary btn-sm">Suivant</button>
          </div>
        </div>
      )}
    </div>
  );
}
