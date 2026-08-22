import React, { useState, useEffect } from 'react';
import { useI18n } from '@/i18n';
import api from '@/lib/api';
import SkeletonLoader from '@/components/shared/SkeletonLoader';
import EmptyState from '@/components/shared/EmptyState';
import Toast from '@/components/shared/Toast';
import { Calendar, ExternalLink, Download, Clock, MapPin, RefreshCw } from 'lucide-react';

interface CalendarEvent {
  id: string;
  titre: string;
  description?: string;
  dateDebut: string;
  dateFin: string;
  lieu?: string;
  typeEvenement: string;
  inscrit: boolean;
}

export default function CalendarIntegrationPage() {
  const { t } = useI18n();
  const [events, setEvents] = useState<CalendarEvent[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => { loadEvents(); }, []);

  const loadEvents = async () => {
    try {
      setLoading(true);
      const res = await api.get('/events?upcoming=true');
      setEvents(res.data.content || res.data || []);
    } catch {
      setEvents([]);
    } finally {
      setLoading(false);
    }
  };

  const generateGoogleCalendarUrl = (event: CalendarEvent) => {
    const start = new Date(event.dateDebut).toISOString().replace(/-|:|\.\d{3}/g, '');
    const end = new Date(event.dateFin).toISOString().replace(/-|:|\.\d{3}/g, '');
    const params = new URLSearchParams({
      action: 'TEMPLATE',
      text: event.titre,
      dates: `${start}/${end}`,
      details: event.description || '',
      location: event.lieu || '',
    });
    return `https://calendar.google.com/calendar/render?${params.toString()}`;
  };

  const generateIcsContent = (event: CalendarEvent): string => {
    const formatIcsDate = (date: string) => new Date(date).toISOString().replace(/[-:]/g, '').replace(/\.\d{3}/, '');
    return [
      'BEGIN:VCALENDAR',
      'VERSION:2.0',
      'PRODID:-//Discipolat//Events//FR',
      'BEGIN:VEVENT',
      `DTSTART:${formatIcsDate(event.dateDebut)}`,
      `DTEND:${formatIcsDate(event.dateFin)}`,
      `SUMMARY:${event.titre}`,
      `DESCRIPTION:${event.description || ''}`,
      `LOCATION:${event.lieu || ''}`,
      `UID:${event.id}@discipolat.com`,
      'END:VEVENT',
      'END:VCALENDAR',
    ].join('\r\n');
  };

  const downloadIcs = (event: CalendarEvent) => {
    const ics = generateIcsContent(event);
    const blob = new Blob([ics], { type: 'text/calendar;charset=utf-8' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `${event.titre.replace(/[^a-zA-Z0-9]/g, '_')}.ics`;
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
    URL.revokeObjectURL(url);
    Toast.success('Fichier ICS téléchargé');
  };

  const downloadAllIcs = () => {
    if (events.length === 0) return;
    const formatIcsDate = (date: string) => new Date(date).toISOString().replace(/[-:]/g, '').replace(/\.\d{3}/, '');
    const ics = [
      'BEGIN:VCALENDAR',
      'VERSION:2.0',
      'PRODID:-//Discipolat//Events//FR',
      ...events.map(event => [
        'BEGIN:VEVENT',
        `DTSTART:${formatIcsDate(event.dateDebut)}`,
        `DTEND:${formatIcsDate(event.dateFin)}`,
        `SUMMARY:${event.titre}`,
        `DESCRIPTION:${event.description || ''}`,
        `LOCATION:${event.lieu || ''}`,
        `UID:${event.id}@discipolat.com`,
        'END:VEVENT',
      ].join('\r\n')),
      'END:VCALENDAR',
    ].join('\r\n');
    const blob = new Blob([ics], { type: 'text/calendar;charset=utf-8' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = 'discipolat_evenements.ics';
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
    URL.revokeObjectURL(url);
    Toast.success('Tous les événements téléchargés');
  };

  return (
    <div className="min-h-screen p-4 md:p-8 max-w-5xl mx-auto">
      <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4 mb-8">
        <div>
          <h1 className="text-2xl md:text-3xl font-bold text-gray-900 dark:text-white flex items-center gap-3">
            <Calendar className="w-8 h-8 text-blue-500" />
            Calendrier
          </h1>
          <p className="text-gray-500 dark:text-gray-400 mt-1">
            Synchronisez vos événements avec votre calendrier personnel
          </p>
        </div>
        <button
          onClick={downloadAllIcs}
          disabled={events.length === 0}
          className="px-4 py-2 rounded-xl bg-gradient-to-r from-blue-600 to-indigo-600 text-white text-sm font-medium hover:from-blue-700 hover:to-indigo-700 transition-all shadow-lg shadow-blue-500/25 flex items-center gap-2 disabled:opacity-50"
        >
          <Download className="w-4 h-4" />
          Tout exporter (.ics)
        </button>
      </div>

      {/* Calendar Providers */}
      <div className="p-5 rounded-2xl border border-gray-200 dark:border-white/10 bg-white dark:bg-white/5 mb-6">
        <h2 className="text-sm font-semibold text-gray-700 dark:text-gray-300 mb-3">Synchroniser avec</h2>
        <div className="flex flex-wrap gap-3">
          <a
            href={`https://calendar.google.com/calendar/r?cid=${encodeURIComponent('webcal://discipolat.com/api/v1/events/ical')}`}
            target="_blank"
            rel="noopener noreferrer"
            className="flex items-center gap-2 px-4 py-2 rounded-xl border border-gray-200 dark:border-white/10 text-sm text-gray-700 dark:text-gray-300 hover:bg-gray-50 dark:hover:bg-white/5 transition-all"
          >
            <span className="text-lg">📅</span> Google Calendar
            <ExternalLink className="w-3 h-3 text-gray-400" />
          </a>
          <a
            href={`https://outlook.live.com/owa/?path=/calendar/action/compose&rru=addcal&subject=Discipolat&body=Export+calendrier+Discipolat&location=&startdt=${new Date().toISOString()}&enddt=${new Date(Date.now() + 7 * 24 * 60 * 60 * 1000).toISOString()}`}
            target="_blank"
            rel="noopener noreferrer"
            className="flex items-center gap-2 px-4 py-2 rounded-xl border border-gray-200 dark:border-white/10 text-sm text-gray-700 dark:text-gray-300 hover:bg-gray-50 dark:hover:bg-white/5 transition-all"
          >
            <span className="text-lg">📧</span> Outlook
            <ExternalLink className="w-3 h-3 text-gray-400" />
          </a>
          <button
            onClick={() => {
              const link = document.createElement('link');
              link.rel = 'alternate';
              link.type = 'text/calendar';
              link.href = 'webcal://discipolat.com/api/v1/events/ical';
              document.head.appendChild(link);
              Toast.success('Flux ICS prêt — ajoutez-le à votre calendrier');
            }}
            className="flex items-center gap-2 px-4 py-2 rounded-xl border border-gray-200 dark:border-white/10 text-sm text-gray-700 dark:text-gray-300 hover:bg-gray-50 dark:hover:bg-white/5 transition-all"
          >
            <span className="text-lg">🍎</span> Apple Calendar
          </button>
        </div>
      </div>

      {/* Events */}
      <h2 className="text-lg font-bold text-gray-900 dark:text-white mb-4">Événements à venir</h2>
      {loading ? (
        <SkeletonLoader lines={4} variant="card" />
      ) : events.length === 0 ? (
        <EmptyState
          icon={<Calendar className="w-8 h-8 text-gray-400" />}
          title="Aucun événement à venir"
          message="Les prochains événements apparaîtront ici"
        />
      ) : (
        <div className="space-y-3">
          {events.map(event => (
            <div key={event.id} className="p-5 rounded-2xl border border-gray-200 dark:border-white/10 bg-white dark:bg-white/5 hover:shadow-lg transition-all">
              <div className="flex items-start justify-between gap-4">
                <div className="flex-1">
                  <div className="flex items-center gap-2 mb-2">
                    <h3 className="font-semibold text-gray-900 dark:text-white">{event.titre}</h3>
                    <span className="px-2 py-0.5 rounded-full text-xs font-medium bg-blue-100 dark:bg-blue-500/20 text-blue-700 dark:text-blue-400">
                      {event.typeEvenement}
                    </span>
                  </div>
                  <div className="flex flex-wrap items-center gap-4 text-sm text-gray-500 dark:text-gray-400">
                    <span className="flex items-center gap-1">
                      <Clock className="w-3.5 h-3.5" />
                      {new Date(event.dateDebut).toLocaleDateString('fr-FR', {
                        weekday: 'long', day: 'numeric', month: 'long', year: 'numeric',
                        hour: '2-digit', minute: '2-digit',
                      })}
                    </span>
                    {event.lieu && (
                      <span className="flex items-center gap-1">
                        <MapPin className="w-3.5 h-3.5" />
                        {event.lieu}
                      </span>
                    )}
                  </div>
                  {event.description && (
                    <p className="text-sm text-gray-500 dark:text-gray-400 mt-2 line-clamp-2">{event.description}</p>
                  )}
                </div>
                <div className="flex gap-2 shrink-0">
                  <a
                    href={generateGoogleCalendarUrl(event)}
                    target="_blank"
                    rel="noopener noreferrer"
                    className="px-3 py-1.5 rounded-lg bg-blue-100 dark:bg-blue-500/20 text-blue-700 dark:text-blue-400 text-xs font-medium hover:bg-blue-200 dark:hover:bg-blue-500/30 transition-all flex items-center gap-1"
                  >
                    <Calendar className="w-3 h-3" />
                    Google
                  </a>
                  <button
                    onClick={() => downloadIcs(event)}
                    className="px-3 py-1.5 rounded-lg bg-gray-100 dark:bg-white/5 text-gray-600 dark:text-gray-400 text-xs font-medium hover:bg-gray-200 dark:hover:bg-white/10 transition-all flex items-center gap-1"
                  >
                    <Download className="w-3 h-3" />
                    .ics
                  </button>
                </div>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
