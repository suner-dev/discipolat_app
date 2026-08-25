import React, { useState, useEffect } from 'react';
import { useI18n } from '@/i18n';
import api, { getErrorMessage } from '@/lib/api';
import SkeletonLoader from '@/components/shared/SkeletonLoader';
import EmptyState from '@/components/shared/EmptyState';
import toast from 'react-hot-toast';
import { Calendar, Plus, Download, ExternalLink, Clock } from 'lucide-react';

interface CalEvent {
  id: string;
  titre: string;
  description?: string;
  début: string;
  fin: string;
  lieu?: string;
  source: string;
  statut: string;
}

export default function CalendarIntegrationPage() {
  const { t } = useI18n();
  const [events, setEvents] = useState<CalEvent[]>([]);
  const [loading, setLoading] = useState(true);
  const [showCreate, setShowCreate] = useState(false);
  const [newEvent, setNewEvent] = useState({ titre: '', description: '', début: '', fin: '', lieu: '' });

  useEffect(() => { loadEvents(); }, []);

  const loadEvents = async () => {
    try {
      setLoading(true);
      const res = await api.get('/calendar');
      setEvents(res.data || []);
    } catch (e) { toast.error(getErrorMessage(e)); setEvents([]); }
    finally { setLoading(false); }
  };

  const createEvent = async () => {
    if (!newEvent.titre.trim()) { toast('Titre requis', { icon: '⚠️' }); return; }
    try {
      await api.post('/calendar', { ...newEvent, source: 'INTERNE' });
      toast.success('Événement ajouté au calendrier');
      setShowCreate(false);
      setNewEvent({ titre: '', description: '', début: '', fin: '', lieu: '' });
      loadEvents();
    } catch (e) { toast.error(getErrorMessage(e)); }
  };

  const downloadICal = async (id: string) => {
    try {
      const res = await api.get(`/calendar/${id}/ical`);
      const blob = new Blob([res.data], { type: 'text/calendar' });
      const url = URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url; a.download = 'event.ics'; a.click();
      toast.success('Fichier iCal téléchargé');
    } catch (e) { toast.error(getErrorMessage(e)); }
  };

  return (
    <div className="min-h-screen p-4 md:p-8 max-w-7xl mx-auto">
      <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4 mb-8">
        <div>
          <h1 className="text-2xl md:text-3xl font-bold text-gray-900 dark:text-white flex items-center gap-3">
            <Calendar className="w-8 h-8 text-blue-500" />
            {t('calendar.title')}
          </h1>
          <p className="text-gray-500 dark:text-gray-400 mt-1">Intégrez vos événements avec Google Calendar, Outlook et iCal</p>
        </div>
        <button onClick={() => setShowCreate(true)}
          className="px-4 py-2 rounded-xl bg-gradient-to-r from-blue-500 to-indigo-500 text-white text-sm font-medium hover:from-blue-600 hover:to-indigo-600 transition-all shadow-lg flex items-center gap-2">
          <Plus className="w-4 h-4" /> {t('calendar.create')}
        </button>
      </div>

      {/* Connect buttons */}
      <div className="flex gap-3 mb-6 flex-wrap">
        {['Google Calendar', 'Outlook', 'iCal Export'].map((name, i) => (
          <button key={name} className="px-4 py-2 rounded-xl border border-gray-200 dark:border-white/10 bg-white dark:bg-white/5 text-sm font-medium text-gray-700 dark:text-gray-300 hover:bg-gray-50 dark:hover:bg-white/10 transition-all flex items-center gap-2">
            <ExternalLink className="w-4 h-4" /> {name}
          </button>
        ))}
      </div>

      {loading ? <SkeletonLoader lines={4} variant="card" /> :
        events.length === 0 ? (
          <EmptyState icon={<Calendar className="w-8 h-8 text-gray-400" />}
            title="Aucun événement"
            message="Ajoutez des événements ou connectez votre calendrier"
            action={{ label: 'Ajouter un événement', onClick: () => setShowCreate(true) }} />
        ) : (
          <div className="space-y-3">
            {events.map(ev => (
              <div key={ev.id} className="bg-white dark:bg-white/5 rounded-xl p-4 border border-gray-200 dark:border-white/10 flex items-center justify-between">
                <div className="flex-1">
                  <h3 className="font-medium text-gray-900 dark:text-white text-sm">{ev.titre}</h3>
                  <div className="text-xs text-gray-500 mt-1 flex items-center gap-3">
                    <span className="flex items-center gap-1"><Clock className="w-3 h-3" />{new Date(ev.début).toLocaleString('fr-FR')} → {new Date(ev.fin).toLocaleTimeString('fr-FR')}</span>
                    {ev.lieu && <span>📍 {ev.lieu}</span>}
                    <span className="px-2 py-0.5 rounded-full bg-gray-100 dark:bg-white/10 text-gray-600">{ev.source}</span>
                  </div>
                </div>
                <button onClick={() => downloadICal(ev.id)} className="p-2 rounded-lg hover:bg-gray-100 dark:hover:bg-white/10 text-gray-400" title="Télécharger iCal">
                  <Download className="w-4 h-4" />
                </button>
              </div>
            ))}
          </div>
        )}

      {showCreate && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
          <div className="fixed inset-0 bg-black/50 backdrop-blur-sm" onClick={() => setShowCreate(false)} />
          <div className="relative bg-white dark:bg-gray-800 rounded-2xl shadow-2xl max-w-lg w-full p-6 border border-gray-200 dark:border-white/10">
            <h2 className="text-lg font-bold text-gray-900 dark:text-white mb-4">Nouvel événement</h2>
            <div className="space-y-4">
              <input type="text" value={newEvent.titre} onChange={e => setNewEvent({ ...newEvent, titre: e.target.value })}
                className="w-full px-4 py-2.5 rounded-xl border border-gray-200 dark:border-white/10 bg-white dark:bg-white/5 text-sm" placeholder="Titre" />
              <textarea value={newEvent.description} onChange={e => setNewEvent({ ...newEvent, description: e.target.value })}
                rows={2} className="w-full px-4 py-2.5 rounded-xl border border-gray-200 dark:border-white/10 bg-white dark:bg-white/5 text-sm resize-none" placeholder="Description" />
              <div className="grid grid-cols-2 gap-4">
                <div><label className="block text-xs text-gray-500 mb-1">Début</label>
                  <input type="datetime-local" value={newEvent.début} onChange={e => setNewEvent({ ...newEvent, début: e.target.value })}
                    className="w-full px-4 py-2.5 rounded-xl border border-gray-200 dark:border-white/10 bg-white dark:bg-white/5 text-sm" /></div>
                <div><label className="block text-xs text-gray-500 mb-1">Fin</label>
                  <input type="datetime-local" value={newEvent.fin} onChange={e => setNewEvent({ ...newEvent, fin: e.target.value })}
                    className="w-full px-4 py-2.5 rounded-xl border border-gray-200 dark:border-white/10 bg-white dark:bg-white/5 text-sm" /></div>
              </div>
              <input type="text" value={newEvent.lieu} onChange={e => setNewEvent({ ...newEvent, lieu: e.target.value })}
                className="w-full px-4 py-2.5 rounded-xl border border-gray-200 dark:border-white/10 bg-white dark:bg-white/5 text-sm" placeholder="Lieu" />
            </div>
            <div className="flex justify-end gap-3 mt-6">
              <button onClick={() => setShowCreate(false)} className="px-4 py-2 rounded-xl border text-sm">Annuler</button>
              <button onClick={createEvent} className="px-4 py-2 rounded-xl bg-blue-500 text-white text-sm font-medium hover:bg-blue-600">Créer</button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
