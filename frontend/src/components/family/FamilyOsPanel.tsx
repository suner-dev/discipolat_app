import { useState, type ReactNode } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import api, { getErrorMessage } from '@/lib/api';
import { tText } from '@/i18n';
import {
  LayoutDashboard, DoorOpen, HandHeart, CalendarDays, ScrollText, Users,
  Plus, X, Loader2, CheckCircle2, Ban, Search, UserCheck, AlertTriangle,
} from 'lucide-react';
import toast from 'react-hot-toast';

/**
 * G4.1/G4.2 — Espace « Family OS » : la famille est un espace de premier rang
 * (presque un département) dédié au suivi des âmes : visites, réceptions,
 * réunions, journal d'activités unifié, membres — le tout branché sur les
 * endpoints réels `/families/{id}/os/*` (garde serveur par famille).
 *
 * Les queryKeys sont préfixées `['family-os', familyId, …]` pour que le
 * bus temps réel (§G5.8) puisse invalider ces écrans à la volée.
 */

type Tab = 'dashboard' | 'visits' | 'receptions' | 'meetings' | 'journal' | 'members';

const TABS: { id: Tab; label: string; icon: typeof LayoutDashboard }[] = [
  { id: 'dashboard', label: 'Tableau de bord', icon: LayoutDashboard },
  { id: 'visits', label: 'Visites', icon: DoorOpen },
  { id: 'receptions', label: 'Réceptions', icon: HandHeart },
  { id: 'meetings', label: 'Réunions', icon: CalendarDays },
  { id: 'journal', label: 'Journal', icon: ScrollText },
  { id: 'members', label: 'Membres', icon: Users },
];

export interface FamilyOsVisit {
  id: string;
  familyId: string;
  soulId: string | null;
  faiseurId: string | null;
  visitDate: string;
  visitType: string | null;
  subject: string | null;
  report: string | null;
  decisions: string | null;
  nextActionDate: string | null;
  nextActionType: string | null;
  status: string;
}

export interface FamilyOsReception {
  id: string;
  familyId: string;
  soulId: string | null;
  receptionDate: string;
  receptionType: string | null;
  notes: string | null;
  assignedFaiseurId: string | null;
}

export interface FamilyOsMeeting {
  id: string;
  familyId: string;
  title: string | null;
  description: string | null;
  meetingDate: string;
  startTime: string | null;
  endTime: string | null;
  location: string | null;
  meetingType: string | null;
  status: string;
}

interface FamilyOsActivity {
  id: string;
  activityType: string;
  title: string | null;
  description: string | null;
  activityDate: string;
  status: string;
}

interface SoulCandidate {
  soulId: string;
  userId: string | null;
  prenom: string | null;
  nom: string | null;
}

interface OsSoul {
  id: string;
  prenom: string | null;
  nom: string;
  typeDisciple: string | null;
  faiseurId: string | null;
}

interface OsDashboard {
  familyId: string;
  upcomingVisits: { id: string; soulName: string; visitDate: string; status: string }[];
  followUps: { id: string; soulName: string; nextActionDate: string; status: string }[];
  recentReceptions: FamilyOsReception[];
  upcomingMeetings: FamilyOsMeeting[];
  overdueFollowUps: number;
}

const today = () => new Date().toISOString().slice(0, 10);

const statusBadge = (status: string) =>
  status === 'COMPLETED' || status === 'DONE' ? 'badge-success'
    : status === 'CANCELLED' ? 'badge-danger'
      : status === 'OVERDUE' ? 'badge-danger'
        : 'badge-warning';

export default function FamilyOsPanel({ familyId, canWrite }: { familyId: string; canWrite: boolean }) {
  const [tab, setTab] = useState<Tab>('dashboard');
  const queryClient = useQueryClient();
  const osKey = ['family-os', familyId];
  const invalidateOs = () => {
    queryClient.invalidateQueries({ queryKey: osKey });
    queryClient.invalidateQueries({ queryKey: ['souls', 'family', familyId] });
  };

  return (
    <div className="glass-card p-5 mb-6 animate-slide-up">
      {/* Tabs */}
      <div className="flex flex-wrap gap-1 mb-5 border-b border-gray-200/60 dark:border-gray-700/60 pb-3">
        {TABS.map(({ id, label, icon: Icon }) => (
          <button
            key={id}
            onClick={() => setTab(id)}
            className={`inline-flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-xs font-semibold transition-all ${
              tab === id
                ? 'bg-primary-500/10 text-primary-600 dark:text-primary-400 ring-1 ring-primary-500/30'
                : 'text-gray-500 hover:bg-white/40 dark:hover:bg-gray-800/40'
            }`}
          >
            <Icon className="w-3.5 h-3.5" />
            {tText(label)}
          </button>
        ))}
      </div>

      {tab === 'dashboard' && <DashboardTab familyId={familyId} osKey={osKey} />}
      {tab === 'visits' && <VisitsTab familyId={familyId} osKey={osKey} canWrite={canWrite} onSaved={invalidateOs} />}
      {tab === 'receptions' && <ReceptionsTab familyId={familyId} osKey={osKey} canWrite={canWrite} onSaved={invalidateOs} />}
      {tab === 'meetings' && <MeetingsTab familyId={familyId} osKey={osKey} canWrite={canWrite} onSaved={invalidateOs} />}
      {tab === 'journal' && <JournalTab familyId={familyId} osKey={osKey} />}
      {tab === 'members' && <MembersTab familyId={familyId} osKey={osKey} canWrite={canWrite} onSaved={invalidateOs} />}
    </div>
  );
}

/* ============================ DASHBOARD ============================ */

function DashboardTab({ familyId, osKey }: { familyId: string; osKey: (string | number)[] }) {
  const { data, isLoading } = useQuery({
    queryKey: [...osKey, 'dashboard'],
    queryFn: async () => (await api.get(`/families/${familyId}/os/dashboard`)).data as OsDashboard,
  });

  if (isLoading) return <PanelLoader />;
  if (!data) return null;

  const kpis = [
    { label: 'Visites à venir (7 j)', value: data.upcomingVisits?.length ?? 0, tone: 'text-primary-600' },
    { label: 'Suivis en cours', value: data.followUps?.length ?? 0, tone: 'text-amber-600' },
    { label: 'Suivis en retard', value: data.overdueFollowUps ?? 0, tone: (data.overdueFollowUps ?? 0) > 0 ? 'text-red-500' : 'text-green-600' },
    { label: 'Réceptions (30 j)', value: data.recentReceptions?.length ?? 0, tone: 'text-rose-600' },
    { label: 'Réunions (30 j)', value: data.upcomingMeetings?.length ?? 0, tone: 'text-gray-900 dark:text-gray-100' },
  ];

  return (
    <div className="space-y-5">
      <div className="grid grid-cols-2 sm:grid-cols-5 gap-3">
        {kpis.map((k) => (
          <div key={k.label} className="p-3.5 rounded-xl bg-white/30 dark:bg-gray-800/30">
            <p className="stat-label">{tText(k.label)}</p>
            <p className={`stat-value text-xl ${k.tone}`}>{k.value}</p>
          </div>
        ))}
      </div>

      {data.followUps && data.followUps.length > 0 && (
        <OsSection title="Âmes en suivi" icon={AlertTriangle}>
          {data.followUps.slice(0, 6).map((f) => (
            <OsRow key={f.id} left={f.soulName || f.id} right={<span className={`badge text-[10px] ${statusBadge(f.status)}`}>{tText(f.status)}</span>} sub={`${tText('Prochaine action')} : ${f.nextActionDate}`} />
          ))}
        </OsSection>
      )}

      {data.upcomingVisits && data.upcomingVisits.length > 0 && (
        <OsSection title="Prochaines visites" icon={DoorOpen}>
          {data.upcomingVisits.slice(0, 6).map((v) => (
            <OsRow key={v.id} left={v.soulName || v.id} right={<span className={`badge text-[10px] ${statusBadge(v.status)}`}>{tText(v.status)}</span>} sub={v.visitDate} />
          ))}
        </OsSection>
      )}
    </div>
  );
}

/* ============================ VISITES ============================ */

function VisitsTab({ familyId, osKey, canWrite, onSaved }: TabProps) {
  const { data: visits, isLoading } = useQuery({
    queryKey: [...osKey, 'visits'],
    queryFn: async () => (await api.get(`/families/${familyId}/os/visits`)).data as FamilyOsVisit[],
  });
  const { data: members } = useQuery({
    queryKey: [...osKey, 'members'],
    queryFn: async () => (await api.get(`/families/${familyId}/os/members`)).data as OsSoul[],
  });
  const [showForm, setShowForm] = useState(false);
  const [form, setForm] = useState({ soulId: '', visitDate: today(), visitType: 'VISITE', subject: '', report: '', nextActionDate: '', nextActionType: '' });

  const create = useMutation({
    mutationFn: async () => {
      await api.post(`/families/${familyId}/os/visits`, {
        soulId: form.soulId || null,
        visitDate: form.visitDate,
        visitType: form.visitType,
        subject: form.subject || null,
        report: form.report || null,
        nextActionDate: form.nextActionDate || null,
        nextActionType: form.nextActionType || null,
      });
    },
    onSuccess: () => {
      toast.success(tText('Visite enregistrée'));
      setShowForm(false);
      setForm({ soulId: '', visitDate: today(), visitType: 'VISITE', subject: '', report: '', nextActionDate: '', nextActionType: '' });
      onSaved();
    },
    onError: (err) => toast.error(getErrorMessage(err)),
  });

  const setStatus = useMutation({
    mutationFn: async ({ id, status }: { id: string; status: string }) => {
      await api.put(`/families/${familyId}/os/visits/${id}`, { status });
    },
    onSuccess: () => { toast.success(tText('Statut mis à jour')); onSaved(); },
    onError: (err) => toast.error(getErrorMessage(err)),
  });

  if (isLoading) return <PanelLoader />;

  return (
    <div className="space-y-3">
      <TabHeader title="Visites" onCreate={canWrite ? () => setShowForm(true) : undefined} createLabel="Enregistrer une visite" />
      {showForm && (
        <InlineForm onClose={() => setShowForm(false)} onSubmit={() => create.mutate()} pending={create.isPending}>
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
            <select className="input" value={form.soulId} onChange={(e) => setForm({ ...form, soulId: e.target.value })}>
              <option value="">{tText('Âme visitée (optionnel)')}…</option>
              {members?.map((s) => <option key={s.id} value={s.id}>{s.prenom ? `${s.prenom} ${s.nom}` : s.nom}</option>)}
            </select>
            <input type="date" className="input" value={form.visitDate} onChange={(e) => setForm({ ...form, visitDate: e.target.value })} />
            <select className="input" value={form.visitType} onChange={(e) => setForm({ ...form, visitType: e.target.value })}>
              {['VISITE', 'ENTRETIEN', 'SUIVI', 'URGENCÉ', 'TELEPHONE'].map((t) => <option key={t} value={t}>{tText(t)}</option>)}
            </select>
            <input className="input" placeholder={tText('Sujet')} value={form.subject} onChange={(e) => setForm({ ...form, subject: e.target.value })} />
            <textarea className="input sm:col-span-2" rows={2} placeholder={tText('Compte-rendu')} value={form.report} onChange={(e) => setForm({ ...form, report: e.target.value })} />
            <input type="date" className="input" value={form.nextActionDate} onChange={(e) => setForm({ ...form, nextActionDate: e.target.value })} />
            <input className="input" placeholder={tText('Prochaine action (ex: rappeler)')} value={form.nextActionType} onChange={(e) => setForm({ ...form, nextActionType: e.target.value })} />
          </div>
        </InlineForm>
      )}
      {visits && visits.length > 0 ? (
        <OsList>
          {visits.map((v) => (
            <div key={v.id} className="os-row">
              <div className="flex-1 min-w-0">
                <p className="text-sm font-medium text-gray-900 dark:text-gray-100 truncate">
                  {v.subject || tText('Visite')} · <span className="text-gray-500">{v.visitDate}</span>
                </p>
                <p className="text-xs text-gray-500 truncate">
                  {v.visitType ? tText(v.visitType) : '—'}
                  {v.nextActionDate && <> · {tText('Prochaine action')} : {v.nextActionDate}{v.nextActionType ? ` (${v.nextActionType})` : ''}</>}
                </p>
                {v.report && <p className="text-xs text-gray-400 mt-0.5 line-clamp-2">{v.report}</p>}
              </div>
              <div className="flex items-center gap-2 flex-shrink-0">
                <span className={`badge text-[10px] ${statusBadge(v.status)}`}>{tText(v.status)}</span>
                {canWrite && v.status !== 'COMPLETED' && v.status !== 'CANCELLED' && (
                  <>
                    <button title={tText('Terminer')} onClick={() => setStatus.mutate({ id: v.id, status: 'COMPLETED' })} className="p-1.5 rounded-lg hover:bg-green-50 dark:hover:bg-green-900/20 text-green-600" data-testid={`os-visit-complete-${v.id}`}>
                      <CheckCircle2 className="w-4 h-4" />
                    </button>
                    <button title={tText('Annuler')} onClick={() => setStatus.mutate({ id: v.id, status: 'CANCELLED' })} className="p-1.5 rounded-lg hover:bg-red-50 dark:hover:bg-red-900/20 text-red-500" data-testid={`os-visit-cancel-${v.id}`}>
                      <Ban className="w-4 h-4" />
                    </button>
                  </>
                )}
              </div>
            </div>
          ))}
        </OsList>
      ) : (
        <EmptyState icon={DoorOpen} label="Aucune visite enregistrée" />
      )}
    </div>
  );
}

/* ============================ RÉCEPTIONS ============================ */

function ReceptionsTab({ familyId, osKey, canWrite, onSaved }: TabProps) {
  const { data: receptions, isLoading } = useQuery({
    queryKey: [...osKey, 'receptions'],
    queryFn: async () => (await api.get(`/families/${familyId}/os/receptions`)).data as FamilyOsReception[],
  });
  const { data: members } = useQuery({
    queryKey: [...osKey, 'members'],
    queryFn: async () => (await api.get(`/families/${familyId}/os/members`)).data as OsSoul[],
  });
  const [showForm, setShowForm] = useState(false);
  const [form, setForm] = useState({ soulId: '', receptionDate: today(), receptionType: 'NOUVEAU_ARRIVANT', notes: '' });

  const create = useMutation({
    mutationFn: async () => {
      await api.post(`/families/${familyId}/os/receptions`, {
        soulId: form.soulId || null,
        receptionDate: form.receptionDate,
        receptionType: form.receptionType,
        notes: form.notes || null,
      });
    },
    onSuccess: () => {
      toast.success(tText('Réception enregistrée'));
      setShowForm(false);
      onSaved();
    },
    onError: (err) => toast.error(getErrorMessage(err)),
  });

  if (isLoading) return <PanelLoader />;

  return (
    <div className="space-y-3">
      <TabHeader title="Réceptions" onCreate={canWrite ? () => setShowForm(true) : undefined} createLabel="Enregistrer une réception" />
      {showForm && (
        <InlineForm onClose={() => setShowForm(false)} onSubmit={() => create.mutate()} pending={create.isPending}>
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
            <select className="input" value={form.soulId} onChange={(e) => setForm({ ...form, soulId: e.target.value })}>
              <option value="">{tText('Âme reçue (optionnel)')}…</option>
              {members?.map((s) => <option key={s.id} value={s.id}>{s.prenom ? `${s.prenom} ${s.nom}` : s.nom}</option>)}
            </select>
            <input type="date" className="input" value={form.receptionDate} onChange={(e) => setForm({ ...form, receptionDate: e.target.value })} />
            <select className="input" value={form.receptionType} onChange={(e) => setForm({ ...form, receptionType: e.target.value })}>
              {['NOUVEAU_ARRIVANT', 'NOUVEAU_CONVERTI', 'RETOUR', 'VISITE'].map((t) => <option key={t} value={t}>{tText(t.replace(/_/g, ' '))}</option>)}
            </select>
            <textarea className="input sm:col-span-2" rows={2} placeholder={tText('Notes')} value={form.notes} onChange={(e) => setForm({ ...form, notes: e.target.value })} />
          </div>
        </InlineForm>
      )}
      {receptions && receptions.length > 0 ? (
        <OsList>
          {receptions.map((r) => (
            <div key={r.id} className="os-row">
              <div className="flex-1 min-w-0">
                <p className="text-sm font-medium text-gray-900 dark:text-gray-100">
                  {r.receptionType ? tText(r.receptionType.replace(/_/g, ' ')) : tText('Réception')} · <span className="text-gray-500">{r.receptionDate}</span>
                </p>
                {r.notes && <p className="text-xs text-gray-500 mt-0.5 line-clamp-2">{r.notes}</p>}
              </div>
            </div>
          ))}
        </OsList>
      ) : (
        <EmptyState icon={HandHeart} label="Aucune réception enregistrée" />
      )}
    </div>
  );
}

/* ============================ RÉUNIONS ============================ */

function MeetingsTab({ familyId, osKey, canWrite, onSaved }: TabProps) {
  const { data: meetings, isLoading } = useQuery({
    queryKey: [...osKey, 'meetings'],
    queryFn: async () => (await api.get(`/families/${familyId}/os/meetings`)).data as FamilyOsMeeting[],
  });
  const [showForm, setShowForm] = useState(false);
  const [form, setForm] = useState({ title: '', meetingDate: today(), startTime: '', endTime: '', location: '', meetingType: 'REUNION_FAMILLE' });

  const create = useMutation({
    mutationFn: async () => {
      await api.post(`/families/${familyId}/os/meetings`, {
        title: form.title,
        meetingDate: form.meetingDate,
        startTime: form.startTime || null,
        endTime: form.endTime || null,
        location: form.location || null,
        meetingType: form.meetingType,
      });
    },
    onSuccess: () => {
      toast.success(tText('Réunion programmée'));
      setShowForm(false);
      setForm({ title: '', meetingDate: today(), startTime: '', endTime: '', location: '', meetingType: 'REUNION_FAMILLE' });
      onSaved();
    },
    onError: (err) => toast.error(getErrorMessage(err)),
  });

  if (isLoading) return <PanelLoader />;

  return (
    <div className="space-y-3">
      <TabHeader title="Réunions" onCreate={canWrite ? () => setShowForm(true) : undefined} createLabel="Programmer une réunion" />
      {showForm && (
        <InlineForm onClose={() => setShowForm(false)} onSubmit={() => create.mutate()} pending={create.isPending}>
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
            <input className="input sm:col-span-2" placeholder={tText('Titre *')} required value={form.title} onChange={(e) => setForm({ ...form, title: e.target.value })} />
            <input type="date" className="input" value={form.meetingDate} onChange={(e) => setForm({ ...form, meetingDate: e.target.value })} />
            <select className="input" value={form.meetingType} onChange={(e) => setForm({ ...form, meetingType: e.target.value })}>
              {['REUNION_FAMILLE', 'ETUDE_BIBLIQUE', 'PRIERE', 'CELEBRATION'].map((t) => <option key={t} value={t}>{tText(t.replace(/_/g, ' '))}</option>)}
            </select>
            <input type="time" className="input" value={form.startTime} onChange={(e) => setForm({ ...form, startTime: e.target.value })} />
            <input type="time" className="input" value={form.endTime} onChange={(e) => setForm({ ...form, endTime: e.target.value })} />
            <input className="input sm:col-span-2" placeholder={tText('Lieu')} value={form.location} onChange={(e) => setForm({ ...form, location: e.target.value })} />
          </div>
        </InlineForm>
      )}
      {meetings && meetings.length > 0 ? (
        <OsList>
          {meetings.map((m) => (
            <div key={m.id} className="os-row">
              <div className="flex-1 min-w-0">
                <p className="text-sm font-medium text-gray-900 dark:text-gray-100 truncate">
                  {m.title || tText('Réunion')} · <span className="text-gray-500">{m.meetingDate}{m.startTime ? ` ${m.startTime.slice(0, 5)}` : ''}</span>
                </p>
                <p className="text-xs text-gray-500 truncate">
                  {m.meetingType ? tText(m.meetingType.replace(/_/g, ' ')) : '—'}{m.location ? ` · ${m.location}` : ''}
                </p>
              </div>
              <span className={`badge text-[10px] ${statusBadge(m.status)}`}>{tText(m.status)}</span>
            </div>
          ))}
        </OsList>
      ) : (
        <EmptyState icon={CalendarDays} label="Aucune réunion programmée" />
      )}
    </div>
  );
}

/* ============================ JOURNAL ============================ */

function JournalTab({ familyId, osKey }: { familyId: string; osKey: (string | number)[] }) {
  const [page, setPage] = useState(0);
  const { data, isLoading } = useQuery({
    queryKey: [...osKey, 'activities', page],
    queryFn: async () => {
      const res = await api.get(`/families/${familyId}/os/activities?page=${page}&size=20`);
      return res.data as { content: FamilyOsActivity[]; totalPages: number; totalElements: number };
    },
  });

  if (isLoading) return <PanelLoader />;

  return (
    <div className="space-y-3">
      <TabHeader title="Journal des activités" />
      {data?.content && data.content.length > 0 ? (
        <>
          <OsList>
            {data.content.map((a) => (
              <div key={a.id} className="os-row">
                <div className="flex-1 min-w-0">
                  <p className="text-sm font-medium text-gray-900 dark:text-gray-100 truncate">{a.title || a.activityType}</p>
                  {a.description && <p className="text-xs text-gray-500 mt-0.5 line-clamp-2">{a.description}</p>}
                </div>
                <div className="flex items-center gap-2 flex-shrink-0">
                  <span className="badge text-[10px] badge-info">{tText(a.activityType.replace(/_/g, ' '))}</span>
                  <span className="text-xs text-gray-400">{a.activityDate}</span>
                </div>
              </div>
            ))}
          </OsList>
          <div className="flex items-center justify-between">
            <button disabled={page === 0} onClick={() => setPage((p) => p - 1)} className="btn-secondary btn-sm disabled:opacity-40">{tText('Précédent')}</button>
            <span className="text-xs text-gray-400">{tText('Page')} {page + 1} / {Math.max(data.totalPages, 1)}</span>
            <button disabled={page + 1 >= data.totalPages} onClick={() => setPage((p) => p + 1)} className="btn-secondary btn-sm disabled:opacity-40">{tText('Suivant')}</button>
          </div>
        </>
      ) : (
        <EmptyState icon={ScrollText} label="Aucune activité enregistrée" />
      )}
    </div>
  );
}

/* ============================ MEMBRES (G4.2) ============================ */

interface FaiseurOption { id: string; firstName: string; lastName: string }

function MembersTab({ familyId, osKey, canWrite, onSaved }: TabProps) {
  const queryClient = useQueryClient();
  const { data: members, isLoading } = useQuery({
    queryKey: [...osKey, 'members'],
    queryFn: async () => (await api.get(`/families/${familyId}/os/members`)).data as OsSoul[],
  });
  const [showAdd, setShowAdd] = useState(false);

  const add = useMutation({
    mutationFn: async ({ soulId, faiseurId }: { soulId: string; faiseurId?: string }) => {
      const params = new URLSearchParams({ soulId });
      if (faiseurId) params.set('faiseurId', faiseurId);
      await api.post(`/families/${familyId}/os/members?${params.toString()}`);
    },
    onSuccess: () => {
      toast.success(tText('Âme ajoutée à la famille'));
      setShowAdd(false);
      queryClient.invalidateQueries({ queryKey: ['users', 'potentiels-chefs', familyId] });
      onSaved();
    },
    onError: (err) => toast.error(getErrorMessage(err)),
  });

  if (isLoading) return <PanelLoader />;

  return (
    <div className="space-y-3">
      <TabHeader title="Membres de la famille" onCreate={canWrite ? () => setShowAdd(true) : undefined} createLabel="Ajouter une âme" />
      {showAdd && (
        <AddSoulModal
          familyId={familyId}
          osKey={osKey}
          onClose={() => setShowAdd(false)}
          onConfirm={(soulId, faiseurId) => add.mutate({ soulId, faiseurId })}
          pending={add.isPending}
        />
      )}
      {members && members.length > 0 ? (
        <OsList>
          {members.map((s) => (
            <div key={s.id} className="os-row">
              <div className="flex items-center gap-3 flex-1 min-w-0">
                <div className="w-8 h-8 rounded-lg bg-gradient-to-br from-rose-400 to-rose-600 flex items-center justify-center text-white text-xs font-bold flex-shrink-0">
                  {(s.prenom?.[0] || s.nom[0])?.toUpperCase()}
                </div>
                <div className="min-w-0">
                  <p className="text-sm font-medium text-gray-900 dark:text-gray-100 truncate">{s.prenom ? `${s.prenom} ${s.nom}` : s.nom}</p>
                  <p className="text-[10px] text-gray-400">{s.typeDisciple === 'NOUVEAU_CONVERTI' ? tText('Nouveau converti') : tText('Nouvel arrivant')}</p>
                </div>
              </div>
            </div>
          ))}
        </OsList>
      ) : (
        <EmptyState icon={Users} label="Aucun membre dans cette famille" />
      )}
    </div>
  );
}

/** §G4.2 — recherche scopée (église/campus), données minimales, ajout avec faiseur optionnel. */
function AddSoulModal({ familyId, osKey, onClose, onConfirm, pending }: {
  familyId: string;
  osKey: (string | number)[];
  onClose: () => void;
  onConfirm: (soulId: string, faiseurId?: string) => void;
  pending: boolean;
}) {
  const [search, setSearch] = useState('');
  const [scope, setScope] = useState<'CHURCH' | 'CAMPUS'>('CAMPUS');
  const [selected, setSelected] = useState<string | null>(null);
  const [faiseurId, setFaiseurId] = useState('');

  const { data: candidates, isFetching } = useQuery({
    queryKey: [...osKey, 'search-souls', scope, search],
    queryFn: async () => {
      const params = new URLSearchParams({ scope });
      if (search.trim()) params.set('search', search.trim());
      const res = await api.get(`/families/${familyId}/os/search-souls?${params.toString()}`);
      return res.data as SoulCandidate[];
    },
  });

  const { data: faiseurs } = useQuery({
    queryKey: ['users', 'faiseurs-family', familyId],
    queryFn: async () => {
      const res = await api.get('/users?role=FAISEUR&size=100');
      return res.data.content as FaiseurOption[];
    },
  });

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal-content max-w-lg w-full" onClick={(e) => e.stopPropagation()}>
        <div className="modal-header">
          <div className="flex items-center gap-3">
            <div className="p-2.5 rounded-xl bg-rose-100 dark:bg-rose-900/30">
              <Plus className="w-5 h-5 text-rose-600 dark:text-rose-400" />
            </div>
            <div>
              <h3 className="text-lg font-semibold text-gray-900 dark:text-gray-100">{tText('Ajouter une âme à la famille')}</h3>
              <p className="text-xs text-gray-500">{tText('Membres non affectés visibles selon le scope (campus/église)')}</p>
            </div>
          </div>
          <button onClick={onClose} className="p-1.5 rounded-lg hover:bg-gray-100 dark:hover:bg-gray-800">
            <X className="w-5 h-5 text-gray-400" />
          </button>
        </div>
        <div className="modal-body space-y-3">
          <div className="flex gap-2">
            <div className="relative flex-1">
              <Search className="w-4 h-4 text-gray-400 absolute left-3 top-1/2 -translate-y-1/2" />
              <input
                className="input pl-9"
                placeholder={tText('Rechercher (nom)…')}
                value={search}
                onChange={(e) => setSearch(e.target.value)}
                data-testid="os-soul-search"
              />
            </div>
            <select className="input w-32" value={scope} onChange={(e) => setScope(e.target.value as 'CHURCH' | 'CAMPUS')} data-testid="os-scope">
              <option value="CAMPUS">{tText('Campus')}</option>
              <option value="CHURCH">{tText('Église')}</option>
            </select>
          </div>

          <div className="max-h-56 overflow-y-auto rounded-xl border border-gray-200/60 dark:border-gray-700/60 divide-y divide-gray-100 dark:divide-gray-800">
            {isFetching && <div className="p-4 flex justify-center"><Loader2 className="w-4 h-4 animate-spin text-gray-400" /></div>}
            {!isFetching && (!candidates || candidates.length === 0) && (
              <p className="p-4 text-sm text-gray-500 text-center">{tText('Aucun membre non affecté dans ce scope')}</p>
            )}
            {candidates?.map((c) => (
              <button
                key={c.soulId}
                type="button"
                onClick={() => setSelected(c.soulId)}
                className={`w-full flex items-center justify-between px-3 py-2.5 text-left text-sm transition-colors ${
                  selected === c.soulId ? 'bg-primary-500/10 text-primary-700 dark:text-primary-300' : 'hover:bg-white/50 dark:hover:bg-gray-800/40 text-gray-900 dark:text-gray-100'
                }`}
              >
                <span>{c.prenom ? `${c.prenom} ${c.nom ?? ''}` : c.nom}</span>
                {selected === c.soulId && <CheckCircle2 className="w-4 h-4" />}
              </button>
            ))}
          </div>

          <div>
            <p className="text-xs font-medium text-gray-500 mb-1.5 flex items-center gap-1"><UserCheck className="w-3.5 h-3.5" />{tText('Faiseur assigné (optionnel)')}</p>
            <select className="input" value={faiseurId} onChange={(e) => setFaiseurId(e.target.value)} data-testid="os-faiseur">
              <option value="">{tText('Aucun')}…</option>
              {faiseurs?.map((f) => <option key={f.id} value={f.id}>{f.firstName} {f.lastName}</option>)}
            </select>
          </div>
        </div>
        <div className="modal-footer">
          <button onClick={onClose} className="btn-secondary btn-sm">{tText('Annuler')}</button>
          <button
            onClick={() => selected && onConfirm(selected, faiseurId || undefined)}
            disabled={!selected || pending}
            className="btn-primary btn-sm"
            data-testid="os-add-confirm"
          >
            {pending ? <Loader2 className="w-4 h-4 animate-spin" /> : <Plus className="w-4 h-4" />}
            {tText('Ajouter')}
          </button>
        </div>
      </div>
    </div>
  );
}

/* ============================ PARTAGÉS ============================ */

interface TabProps {
  familyId: string;
  osKey: (string | number)[];
  canWrite: boolean;
  onSaved: () => void;
}

function PanelLoader() {
  return <div className="flex justify-center py-8"><Loader2 className="w-5 h-5 animate-spin text-gray-400" /></div>;
}

function TabHeader({ title, onCreate, createLabel }: { title: string; onCreate?: () => void; createLabel?: string }) {
  return (
    <div className="flex items-center justify-between">
      <h3 className="text-sm font-semibold text-gray-900 dark:text-gray-100">{tText(title)}</h3>
      {onCreate && (
        <button onClick={onCreate} className="btn-primary btn-sm" data-testid="os-create-btn">
          <Plus className="w-4 h-4" /> {createLabel ? tText(createLabel) : tText('Créer')}
        </button>
      )}
    </div>
  );
}

function OsSection({ title, icon: Icon, children }: { title: string; icon: typeof LayoutDashboard; children: ReactNode }) {
  return (
    <div>
      <p className="text-xs font-semibold text-gray-500 uppercase tracking-wide mb-2 flex items-center gap-1.5">
        <Icon className="w-3.5 h-3.5" /> {tText(title)}
      </p>
      <div className="os-list">{children}</div>
    </div>
  );
}

function OsList({ children }: { children: ReactNode }) {
  return <div className="os-list">{children}</div>;
}

function OsRow({ left, right, sub }: { left: ReactNode; right?: ReactNode; sub?: string }) {
  return (
    <div className="os-row">
      <div className="flex-1 min-w-0">
        <p className="text-sm font-medium text-gray-900 dark:text-gray-100 truncate">{left}</p>
        {sub && <p className="text-xs text-gray-400">{sub}</p>}
      </div>
      {right}
    </div>
  );
}

function EmptyState({ icon: Icon, label }: { icon: typeof LayoutDashboard; label: string }) {
  return (
    <div className="text-center py-8">
      <Icon className="w-8 h-8 text-gray-300 dark:text-gray-600 mx-auto mb-2" />
      <p className="text-sm text-gray-500">{tText(label)}</p>
    </div>
  );
}

function InlineForm({ children, onClose, onSubmit, pending }: {
  children: ReactNode;
  onClose: () => void;
  onSubmit: () => void;
  pending: boolean;
}) {
  return (
    <form
      className="p-4 rounded-xl border border-primary-500/20 bg-primary-500/5 space-y-3 animate-scale-in"
      onSubmit={(e) => { e.preventDefault(); onSubmit(); }}
    >
      {children}
      <div className="flex justify-end gap-2">
        <button type="button" onClick={onClose} className="btn-secondary btn-sm">{tText('Annuler')}</button>
        <button type="submit" disabled={pending} className="btn-primary btn-sm">
          {pending ? <Loader2 className="w-4 h-4 animate-spin" /> : <CheckCircle2 className="w-4 h-4" />}
          {tText('Enregistrer')}
        </button>
      </div>
    </form>
  );
}
