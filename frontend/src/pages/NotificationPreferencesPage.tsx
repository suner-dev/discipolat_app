import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { api } from '@/lib/api';
import { Bell, Mail, Smartphone, MessageSquare, Inbox, Moon } from 'lucide-react';

interface Pref {
  emailEnabled: boolean; pushEnabled: boolean; smsEnabled: boolean;
  whatsappEnabled: boolean; inAppEnabled: boolean;
  quietHoursStart?: number | null; quietHoursEnd?: number | null;
}

/** P21 — Préférences de notification par utilisateur. */
export default function NotificationPreferencesPage() {
  const qc = useQueryClient();
  const { data, isLoading } = useQuery({
    queryKey: ['notif-prefs'],
    queryFn: async () => (await api.get('/notifications/preferences')).data as Pref,
  });
  const [local, setLocal] = useState<Pref | null>(null);
  const prefs = local ?? data;

  const save = useMutation({
    mutationFn: async () => api.put('/notifications/preferences', { ...prefs }),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['notif-prefs'] }); setLocal(null); },
  });

  if (isLoading) return <div className="p-6 text-gray-400">Chargement…</div>;
  if (!prefs) return null;

  const toggle = (key: keyof Pref) => {
    const next = { ...prefs, [key]: !prefs[key] };
    setLocal(next as unknown as Pref);
  };

  return (
    <div className="space-y-6 p-6 max-w-2xl">
      <h1 className="text-2xl font-bold text-white flex items-center gap-2"><Bell className="text-amber-400" /> Préférences de notification</h1>
      <p className="text-sm text-gray-400">Choisissez les canaux par lesquels vous acceptez d'être notifié. Les canaux refusés sont automatiquement redirigés vers la messagerie intégrée.</p>
      <div className="space-y-3">
        <ToggleRow icon={Inbox} label="Notifications intégrées" checked={prefs.inAppEnabled} onChange={() => toggle('inAppEnabled')} />
        <ToggleRow icon={Mail} label="Email" checked={prefs.emailEnabled} onChange={() => toggle('emailEnabled')} />
        <ToggleRow icon={Smartphone} label="Notifications push" checked={prefs.pushEnabled} onChange={() => toggle('pushEnabled')} />
        <ToggleRow icon={MessageSquare} label="SMS (opérateur — coûts pouvant s'appliquer)" checked={prefs.smsEnabled} onChange={() => toggle('smsEnabled')} />
        <ToggleRow icon={Bell} label="WhatsApp" checked={prefs.whatsappEnabled} onChange={() => toggle('whatsappEnabled')} />
      </div>
      <div className="bg-white/5 backdrop-blur rounded-2xl p-5 border border-white/10">
        <h2 className="text-white font-semibold flex items-center gap-2 mb-3"><Moon className="w-4 h-4 text-indigo-400" /> Heures de silence</h2>
        <div className="grid grid-cols-2 gap-4">
          <label className="block text-xs text-gray-400">Début
            <select value={prefs.quietHoursStart ?? 22} onChange={(e) => setLocal({ ...prefs, quietHoursStart: Number(e.target.value) })}
              className="w-full bg-black/30 border border-white/10 rounded-lg px-3 py-2 text-sm text-gray-200 mt-1">
              {Array.from({ length: 24 }, (_, h) => <option key={h} value={h}>{h}:00</option>)}
            </select>
          </label>
          <label className="block text-xs text-gray-400">Fin
            <select value={prefs.quietHoursEnd ?? 7} onChange={(e) => setLocal({ ...prefs, quietHoursEnd: Number(e.target.value) })}
              className="w-full bg-black/30 border border-white/10 rounded-lg px-3 py-2 text-sm text-gray-200 mt-1">
              {Array.from({ length: 24 }, (_, h) => <option key={h} value={h}>{h}:00</option>)}
            </select>
          </label>
        </div>
      </div>
      <button onClick={() => save.mutate()} disabled={save.isPending || !local}
        className="px-5 py-2.5 rounded-xl bg-gradient-to-r from-amber-600 to-orange-600 text-white text-sm font-medium hover:opacity-90 disabled:opacity-50">
        {save.isPending ? 'Enregistrement…' : 'Enregistrer mes préférences'}
      </button>
    </div>
  );
}

function ToggleRow({ icon: Icon, label, checked, onChange }: { icon: typeof Bell; label: string; checked: boolean; onChange: () => void }) {
  return (
    <button onClick={onChange} className="w-full flex items-center justify-between bg-white/5 backdrop-blur rounded-2xl p-4 border border-white/10 hover:border-amber-500/30 transition">
      <span className="flex items-center gap-3 text-sm text-gray-200"><Icon className="w-4 h-4 text-amber-400" /> {label}</span>
      <span className={`relative w-11 h-6 rounded-full transition ${checked ? 'bg-green-500' : 'bg-gray-600'}`}>
        <span className={`absolute top-0.5 w-5 h-5 rounded-full bg-white transition-all ${checked ? 'left-[22px]' : 'left-0.5'}`} />
      </span>
    </button>
  );
}
