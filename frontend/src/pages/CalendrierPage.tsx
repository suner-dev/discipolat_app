import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import toast from 'react-hot-toast';
import api, { getErrorMessage } from '@/lib/api';
import {
  Calendar, RefreshCw, Loader2, CheckCircle, Plus, ExternalLink,
  Clock, Trash2, X,
} from 'lucide-react';

interface CalendarEvent {
  id: string;
  title: string;
  description?: string;
  start: string;
  end?: string;
  location?: string;
  status: string;
  recurrence?: string;
}

export default function CalendrierPage() {
  const qc = useQueryClient();
  const [showCreate, setShowCreate] = useState(false);
  const [newEvent, setNewEvent] = useState({ title: '', description: '', start: '', end: '', location: '' });

  const { data: events = [], isLoading } = useQuery({
    queryKey: ['calendar', 'events'],
    queryFn: async () => {
      const res = await api.get('/calendar/events');
      return (res.data.content || res.data || []) as CalendarEvent[];
    },
  });

  const createMutation = useMutation({
    mutationFn: async () => {
      await api.post('/calendar/events', newEvent);
    },
    onSuccess: () => {
      toast.success('Événement créé');
      qc.invalidateQueries({ queryKey: ['calendar'] });
      setShowCreate(false);
      setNewEvent({ title: '', description: '', start: '', end: '', location: '' });
    },
    onError: (err) => toast.error(getErrorMessage(err)),
  });

  const statusMutation = useMutation({
    mutationFn: async ({ id, status }: { id: string; status: string }) => {
      await api.put(`/calendar/${id}/status`, { status });
    },
    onSuccess: () => {
      toast.success('Statut mis à jour');
      qc.invalidateQueries({ queryKey: ['calendar'] });
    },
    onError: (err) => toast.error(getErrorMessage(err)),
  });

  const downloadFeed = async () => {
    try {
      const res = await api.get('/calendar/feed.ics', { responseType: 'blob' });
      const url = URL.createObjectURL(res.data);
      const a = document.createElement('a');
      a.href = url;
      a.download = 'calendrier.ics';
      a.click();
      URL.revokeObjectURL(url);
      toast.success('Fichier iCal téléchargé');
    } catch {
      toast.error('Erreur lors du téléchargement');
    }
  };

  const statusColor = (s: string) => {
    switch (s) {
      case 'CONFIRMED': return 'badge-success';
      case 'PENDING': return 'badge-warning';
      case 'CANCELLED': return 'badge-error';
      default: return 'badge-info';
    }
  };

  return (
    <div className="page-container max-w-5xl">
      <div className="page-header">
        <div className="animate-fade-in">
          <div className="flex items-center gap-2 mb-1">
            <Calendar className="w-5 h-5 text-primary-500" />
            <h1 className="page-title">Calendrier</h1>
          </div>
          <p className="page-subtitle">Événements, agenda partagé et synchronisation iCal</p>
        </div>
        <div className="page-header-actions">
          <button onClick={downloadFeed} className="btn-secondary btn-sm">
            <ExternalLink className="w-4 h-4" /> Exporter iCal
          </button>
          <button onClick={() => setShowCreate(true)} className="btn-primary btn-sm">
            <Plus className="w-4 h-4" /> Nouvel événement
          </button>
        </div>
      </div>

      {isLoading ? (
        <div className="p-8 text-center"><Loader2 className="w-6 h-6 animate-spin text-gray-400 mx-auto" /></div>
      ) : events.length === 0 ? (
        <div className="glass-card p-10 text-center">
          <Calendar className="w-10 h-10 text-gray-300 mb-3 mx-auto" />
          <p className="text-gray-500 font-medium">Aucun événement planifié.</p>
        </div>
      ) : (
        <div className="space-y-3">
          {events.map((event) => (
            <div key={event.id} className="glass-card px-5 py-4 flex items-center gap-4">
              <div className="w-11 h-11 rounded-xl bg-gradient-to-br from-primary-500 to-primary-600 flex items-center justify-center text-white shadow-sm shrink-0">
                <Calendar className="w-5 h-5" />
              </div>
              <div className="flex-1 min-w-0">
                <div className="flex items-center gap-2 mb-1">
                  <h3 className="text-sm font-bold text-gray-900 dark:text-gray-100">{event.title}</h3>
                  <span className={`badge text-[10px] ${statusColor(event.status)}`}>{event.status}</span>
                </div>
                <div className="flex items-center gap-3 text-[10px] text-gray-400">
                  <span className="flex items-center gap-1">
                    <Clock className="w-3 h-3" />
                    {new Date(event.start).toLocaleString('fr-FR')}
                  </span>
                  {event.location && <span>{event.location}</span>}
                  {event.recurrence && <span className="badge text-[9px]">{event.recurrence}</span>}
                </div>
              </div>
              <div className="flex gap-1">
                {event.status === 'PENDING' && (
                  <button
                    onClick={() => statusMutation.mutate({ id: event.id, status: 'CONFIRMED' })}
                    className="btn-icon text-green-500 hover:bg-green-50 dark:hover:bg-green-900/20"
                    title="Confirmer"
                  >
                    <CheckCircle className="w-4 h-4" />
                  </button>
                )}
                {event.status !== 'CANCELLED' && (
                  <button
                    onClick={() => statusMutation.mutate({ id: event.id, status: 'CANCELLED' })}
                    className="btn-icon text-red-400 hover:bg-red-50 dark:hover:bg-red-900/20"
                    title="Annuler"
                  >
                    <Trash2 className="w-4 h-4" />
                  </button>
                )}
              </div>
            </div>
          ))}
        </div>
      )}

      {/* Create modal */}
      {showCreate && (
        <div className="modal-overlay" onClick={() => setShowCreate(false)}>
          <div className="modal-content max-w-md" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <h3 className="text-base font-bold text-gray-900 dark:text-gray-100">Nouvel événement</h3>
              <button className="btn-icon" onClick={() => setShowCreate(false)}><X className="w-5 h-5" /></button>
            </div>
            <div className="modal-body space-y-4">
              <div>
                <label className="label">Titre *</label>
                <input className="input" value={newEvent.title} onChange={(e) => setNewEvent({ ...newEvent, title: e.target.value })} placeholder="Nom de l'événement" />
              </div>
              <div>
                <label className="label">Description</label>
                <textarea className="input min-h-[60px]" value={newEvent.description} onChange={(e) => setNewEvent({ ...newEvent, description: e.target.value })} placeholder="Détails..." />
              </div>
              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="label">Début *</label>
                  <input type="datetime-local" className="input" value={newEvent.start} onChange={(e) => setNewEvent({ ...newEvent, start: e.target.value })} />
                </div>
                <div>
                  <label className="label">Fin</label>
                  <input type="datetime-local" className="input" value={newEvent.end} onChange={(e) => setNewEvent({ ...newEvent, end: e.target.value })} />
                </div>
              </div>
              <div>
                <label className="label">Lieu</label>
                <input className="input" value={newEvent.location} onChange={(e) => setNewEvent({ ...newEvent, location: e.target.value })} placeholder="Lieu de l'événement" />
              </div>
            </div>
            <div className="modal-footer">
              <button className="btn-ghost btn-sm" onClick={() => setShowCreate(false)}>Annuler</button>
              <button className="btn-primary btn-sm" onClick={() => createMutation.mutate()} disabled={!newEvent.title || !newEvent.start || createMutation.isPending}>
                {createMutation.isPending ? <Loader2 className="w-4 h-4 animate-spin" /> : <CheckCircle className="w-4 h-4" />}
                Créer
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
