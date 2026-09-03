import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useParams } from 'react-router-dom';
import toast from 'react-hot-toast';
import api, { getErrorMessage } from '@/lib/api';
import {
  Megaphone, Clock, Calendar, Loader2, CheckCircle, Send, ArrowLeft,
} from 'lucide-react';
import { useState } from 'react';
import { useNavigate } from 'react-router-dom';

interface Announcement {
  id: string;
  title: string;
  content: string;
  status: string;
  scheduledAt?: string;
}

export default function AnnouncementSchedulePage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const qc = useQueryClient();
  const [scheduleDate, setScheduleDate] = useState('');
  const [scheduleTime, setScheduleTime] = useState('');

  const { data: announcement, isLoading } = useQuery({
    queryKey: ['announcements', id],
    queryFn: async () => {
      const res = await api.get(`/announcements/${id}`);
      return res.data as Announcement;
    },
    enabled: !!id,
  });

  const scheduleMutation = useMutation({
    mutationFn: async () => {
      const scheduledAt = `${scheduleDate}T${scheduleTime}:00`;
      await api.post(`/announcements/${id}/schedule`, { scheduledAt });
    },
    onSuccess: () => {
      toast.success('Annonce programmée');
      qc.invalidateQueries({ queryKey: ['announcements'] });
      navigate(-1);
    },
    onError: (err) => toast.error(getErrorMessage(err)),
  });

  if (isLoading) return <div className="min-h-[40vh] flex items-center justify-center"><Loader2 className="w-6 h-6 animate-spin text-gray-400" /></div>;
  if (!announcement) return <div className="p-6 text-center text-gray-400">Annonce introuvable.</div>;

  return (
    <div className="page-container max-w-2xl">
      <div className="page-header">
        <div className="animate-fade-in">
          <button onClick={() => navigate(-1)} className="btn-ghost btn-sm mb-2 -ml-2">
            <ArrowLeft className="w-4 h-4" /> Retour
          </button>
          <div className="flex items-center gap-2 mb-1">
            <Megaphone className="w-5 h-5 text-orange-500" />
            <h1 className="page-title">Programmer l'annonce</h1>
          </div>
          <p className="page-subtitle">{announcement.title}</p>
        </div>
      </div>

      <div className="glass-card p-6 animate-slide-up">
        {/* Preview */}
        <div className="mb-6 p-4 rounded-xl bg-gray-50 dark:bg-gray-800/40 border border-gray-200/60 dark:border-gray-700/60">
          <p className="text-xs text-gray-400 mb-2 uppercase tracking-wide">Aperçu</p>
          <h4 className="text-sm font-bold text-gray-900 dark:text-gray-100 mb-1">{announcement.title}</h4>
          <p className="text-xs text-gray-500 dark:text-gray-400 line-clamp-3">{announcement.content}</p>
          <div className="flex items-center gap-2 mt-3">
            <span className={`badge text-[10px] ${announcement.status === 'DRAFT' ? 'badge-warning' : 'badge-info'}`}>
              {announcement.status}
            </span>
            {announcement.scheduledAt && (
              <span className="text-[10px] text-gray-400">
                Déjà programmé : {new Date(announcement.scheduledAt).toLocaleString('fr-FR')}
              </span>
            )}
          </div>
        </div>

        {/* Schedule form */}
        <div className="space-y-4">
          <h3 className="text-sm font-bold text-gray-900 dark:text-gray-100 flex items-center gap-2">
            <Clock className="w-4 h-4 text-orange-500" />
            Choisir la date et l'heure de publication
          </h3>
          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="label">Date *</label>
              <input type="date" className="input" value={scheduleDate} onChange={(e) => setScheduleDate(e.target.value)} />
            </div>
            <div>
              <label className="label">Heure *</label>
              <input type="time" className="input" value={scheduleTime} onChange={(e) => setScheduleTime(e.target.value)} />
            </div>
          </div>
          {scheduleDate && scheduleTime && (
            <div className="p-3 rounded-xl bg-orange-50/50 dark:bg-orange-900/10 border border-orange-200/50 dark:border-orange-800/30">
              <p className="text-xs text-orange-700 dark:text-orange-400 flex items-center gap-1">
                <Calendar className="w-3 h-3" />
                Publication prévue le {new Date(`${scheduleDate}T${scheduleTime}`).toLocaleString('fr-FR')}
              </p>
            </div>
          )}
          <button
            onClick={() => scheduleMutation.mutate()}
            disabled={!scheduleDate || !scheduleTime || scheduleMutation.isPending}
            className="btn-primary btn-sm"
          >
            {scheduleMutation.isPending ? <Loader2 className="w-4 h-4 animate-spin" /> : <Send className="w-4 h-4" />}
            Programmer
          </button>
        </div>
      </div>
    </div>
  );
}
