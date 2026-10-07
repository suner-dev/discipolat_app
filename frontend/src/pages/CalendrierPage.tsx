import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import toast from 'react-hot-toast';
import api, { getErrorMessage } from '@/lib/api';
import { getI18nLocale } from '@/i18n';
import { tText } from '@/i18n';
import {
  Calendar, Loader2, CheckCircle, Plus, ExternalLink,
  Clock, Trash2, X,
} from 'lucide-react';

// Contrat réel — CalendarController / CalendarEvent (GET /api/v1/calendar) :
// les champs sont français et le statut est un enum { CONFIRMÉ, EN_ATTENTE, ANNULÉ }.
interface CalendarEvent {
  id: string;
  titre: string;
  description?: string;
  début: string;
  fin?: string;
  lieu?: string;
  statut: string;
  source?: string;
}

export default function CalendrierPage() {
  const qc = useQueryClient();
  const [showCreate, setShowCreate] = useState(false);
  const [newEvent, setNewEvent] = useState({ titre: '', description: '', début: '', fin: '', lieu: '' });

  const { data: events = [], isLoading } = useQuery({
    queryKey: ['calendar', 'events'],
    queryFn: async () => {
      const res = await api.get('/calendar');
      return (res.data.content || res.data || []) as CalendarEvent[];
    },
  });

  const createMutation = useMutation({
    mutationFn: async () => {
      // L'API exige début ET fin (LocalDateTime.parse côté serveur) : on replie
      // fin sur début + 1h si l'utilisateur n'a pas renseigné la fin.
      const fin = newEvent.fin
        ? newEvent.fin
        : new Date(new Date(newEvent.début).getTime() + 3600_000).toISOString().slice(0, 16);
      await api.post('/calendar', { ...newEvent, fin, source: 'INTERNE' });
    },
    onSuccess: () => {
      toast.success(tText('Événement créé'));
      qc.invalidateQueries({ queryKey: ['calendar'] });
      setShowCreate(false);
      setNewEvent({ titre: '', description: '', début: '', fin: '', lieu: '' });
    },
    onError: (err) => toast.error(getErrorMessage(err)),
  });

  const statusMutation = useMutation({
    mutationFn: async ({ id, statut }: { id: string; statut: string }) => {
      await api.patch(`/calendar/${id}/status`, { statut });
    },
    onSuccess: () => {
      toast.success(tText('Statut mis à jour'));
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
      toast.success(tText('Fichier iCal téléchargé'));
    } catch {
      toast.error(tText('Erreur lors du téléchargement'));
    }
  };

  const statusColor = (s: string) => {
    switch (s) {
      case 'CONFIRMÉ': return 'badge-success';
      case 'EN_ATTENTE': return 'badge-warning';
      case 'ANNULÉ': return 'badge-error';
      default: return 'badge-info';
    }
  };

  return (
    <div className="page-container max-w-5xl">
      <div className="page-header">
        <div className="animate-fade-in">
          <div className="flex items-center gap-2 mb-1">
            <Calendar className="w-5 h-5 text-primary-500" />
            <h1 className="page-title">{tText('Calendrier')}</h1>
          </div>
          <p className="page-subtitle">{tText('Événements, agenda partagé et synchronisation iCal')}</p>
        </div>
        <div className="page-header-actions">
          <button onClick={downloadFeed} className="btn-secondary btn-sm">
            <ExternalLink className="w-4 h-4" /> {tText('Exporter iCal')}
          </button>
          <button onClick={() => setShowCreate(true)} className="btn-primary btn-sm">
            <Plus className="w-4 h-4" /> {tText('Nouvel événement')}
          </button>
        </div>
      </div>

      {isLoading ? (
        <div className="p-8 text-center"><Loader2 className="w-6 h-6 animate-spin text-gray-400 mx-auto" /></div>
      ) : events.length === 0 ? (
        <div className="glass-card p-10 text-center">
          <Calendar className="w-10 h-10 text-gray-300 mb-3 mx-auto" />
          <p className="text-gray-500 font-medium">{tText('Aucun événement planifié.')}</p>
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
                  <h3 className="text-sm font-bold text-gray-900 dark:text-gray-100">{event.titre}</h3>
                  <span className={`badge text-[10px] ${statusColor(event.statut)}`}>{event.statut}</span>
                </div>
                <div className="flex items-center gap-3 text-[10px] text-gray-400">
                  <span className="flex items-center gap-1">
                    <Clock className="w-3 h-3" />
                    {new Date(event.début).toLocaleString(getI18nLocale())}
                  </span>
                  {event.lieu && <span>{event.lieu}</span>}
                </div>
              </div>
              <div className="flex gap-1">
                {event.statut === 'EN_ATTENTE' && (
                  <button
                    onClick={() => statusMutation.mutate({ id: event.id, statut: 'CONFIRMÉ' })}
                    className="btn-icon text-green-500 hover:bg-green-50 dark:hover:bg-green-900/20"
                    title={tText('Confirmer')}
                  >
                    <CheckCircle className="w-4 h-4" />
                  </button>
                )}
                {event.statut !== 'ANNULÉ' && (
                  <button
                    onClick={() => statusMutation.mutate({ id: event.id, statut: 'ANNULÉ' })}
                    className="btn-icon text-red-400 hover:bg-red-50 dark:hover:bg-red-900/20"
                    title={tText('Annuler')}
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
              <h3 className="text-base font-bold text-gray-900 dark:text-gray-100">{tText('Nouvel événement')}</h3>
              <button className="btn-icon" onClick={() => setShowCreate(false)}><X className="w-5 h-5" /></button>
            </div>
            <div className="modal-body space-y-4">
              <div>
                <label className="label">{tText('Titre')} *</label>
                <input className="input" value={newEvent.titre} onChange={(e) => setNewEvent({ ...newEvent, titre: e.target.value })} placeholder={tText("Nom de l'événement")} />
              </div>
              <div>
                <label className="label">{tText('Description')}</label>
                <textarea className="input min-h-[60px]" value={newEvent.description} onChange={(e) => setNewEvent({ ...newEvent, description: e.target.value })} placeholder={tText('Détails...')} />
              </div>
              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="label">{tText('Début')} *</label>
                  <input type="datetime-local" className="input" value={newEvent.début} onChange={(e) => setNewEvent({ ...newEvent, début: e.target.value })} />
                </div>
                <div>
                  <label className="label">{tText('Fin')}</label>
                  <input type="datetime-local" className="input" value={newEvent.fin} onChange={(e) => setNewEvent({ ...newEvent, fin: e.target.value })} />
                </div>
              </div>
              <div>
                <label className="label">{tText('Lieu')}</label>
                <input className="input" value={newEvent.lieu} onChange={(e) => setNewEvent({ ...newEvent, lieu: e.target.value })} placeholder={tText("Lieu de l'événement")} />
              </div>
            </div>
            <div className="modal-footer">
              <button className="btn-ghost btn-sm" onClick={() => setShowCreate(false)}>{tText('Annuler')}</button>
              <button className="btn-primary btn-sm" onClick={() => createMutation.mutate()} disabled={!newEvent.titre || !newEvent.début || createMutation.isPending}>
                {createMutation.isPending ? <Loader2 className="w-4 h-4 animate-spin" /> : <CheckCircle className="w-4 h-4" />}
                {tText('Créer')}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
