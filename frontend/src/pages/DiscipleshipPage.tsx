import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { Route, Users, CalendarCheck, Layers, Loader2, Plus, Flag } from 'lucide-react';
import toast from 'react-hot-toast';
import { getErrorMessage } from '@/lib/api';
import { tText } from '@/i18n';
import * as svc from '@/services/discipleshipService';

/**
 * Page Discipleship — consomme le contrat exact V233
 * (`services/discipleshipService.ts`, monture `/api/v1/discipleship`).
 * Les formulaires n'utilisent QUE les clés de body lues côté serveur
 * (DiscipleshipService : createJourney/createStage/createProgress/
 * createAssignment/scheduleMeeting/completeMeeting) — rien d'inventé.
 */
type Row = Record<string, unknown>;
const sv = (r: Row, k: string): string => {
  const v = r[k];
  return v == null || v === '' ? '—' : String(v);
};

const JOURNEY_TYPES = ['NEW_BELIEVER', 'GROWTH', 'LEADERSHIP', 'MINISTRY', 'CUSTOM'];
const TABS = ['journeys', 'progress', 'assignments', 'meetings', 'reports'] as const;
type Tab = (typeof TABS)[number];

export default function DiscipleshipPage() {
  const qc = useQueryClient();
  const [tab, setTab] = useState<Tab>('journeys');
  const [showForm, setShowForm] = useState(false);
  const [journeyFilter, setJourneyFilter] = useState<number | ''>('');
  const [journeyId, setJourneyId] = useState<number | ''>('');

  const [jForm, setJForm] = useState({ name: '', description: '', type: 'GROWTH', totalStages: 5 });
  const [pForm, setPForm] = useState({ discipleId: '', journeyId: '' });
  const [aForm, setAForm] = useState({ mentorId: '', discipleId: '', journeyId: '', meetingFrequencyDays: 7, notes: '' });
  const [mForm, setMForm] = useState({ assignmentId: '', scheduledAt: '', location: '', isGroup: false });

  const { data: journeys = [], isLoading: loadingJourneys } = useQuery({
    queryKey: ['discipleship', 'journeys'],
    queryFn: () => svc.listJourneys(),
  });

  const { data: stages = [] } = useQuery({
    queryKey: ['discipleship', 'stages', journeyFilter],
    queryFn: () => svc.listStages(Number(journeyFilter)),
    enabled: tab === 'journeys' && journeyFilter !== '',
  });

  const { data: progress = [], isLoading: loadingProgress } = useQuery({
    queryKey: ['discipleship', 'progress', journeyFilter],
    queryFn: () => svc.listProgress(journeyFilter === '' ? {} : { journeyId: Number(journeyFilter) }),
    enabled: tab === 'progress',
  });

  const { data: assignments = [], isLoading: loadingAssignments } = useQuery({
    queryKey: ['discipleship', 'assignments'],
    queryFn: () => svc.listAssignments({}),
    enabled: tab === 'assignments',
  });

  const { data: meetings = [], isLoading: loadingMeetings } = useQuery({
    queryKey: ['discipleship', 'meetings'],
    queryFn: () => svc.listMeetings({}),
    enabled: tab === 'meetings',
  });

  const { data: report } = useQuery({
    queryKey: ['discipleship', 'report', journeyId],
    queryFn: () => svc.getReport(Number(journeyId)),
    enabled: tab === 'reports' && journeyId !== '',
  });
  const { data: topMentors = [] } = useQuery({
    queryKey: ['discipleship', 'topMentors', journeyId],
    queryFn: () => svc.getTopMentors(Number(journeyId)),
    enabled: tab === 'reports' && journeyId !== '',
  });

  const invalidate = () => qc.invalidateQueries({ queryKey: ['discipleship'] });
  const onOk = (msg: string) => {
    return () => {
      toast.success(tText(msg));
      setShowForm(false);
      invalidate();
    };
  };
  const onErr = (e: unknown) => toast.error(getErrorMessage(e));

  const createJourney = useMutation({
    mutationFn: () =>
      svc.createJourney({
        name: jForm.name,
        description: jForm.description || undefined,
        type: jForm.type,
        totalStages: jForm.totalStages,
      }),
    onSuccess: onOk('Parcours créé'),
    onError: onErr,
  });

  const createProgress = useMutation({
    mutationFn: () =>
      svc.createProgress({
        discipleId: pForm.discipleId,
        journeyId: pForm.journeyId === '' ? undefined : Number(pForm.journeyId),
      }),
    onSuccess: onOk('Progression créée'),
    onError: onErr,
  });

  const createAssignment = useMutation({
    mutationFn: () =>
      svc.createAssignment({
        mentorId: aForm.mentorId,
        discipleId: aForm.discipleId,
        journeyId: Number(aForm.journeyId),
        meetingFrequencyDays: aForm.meetingFrequencyDays,
        notes: aForm.notes || undefined,
      }),
    onSuccess: onOk('Assignation créée'),
    onError: onErr,
  });

  const scheduleMeeting = useMutation({
    mutationFn: () =>
      svc.scheduleMeeting({
        assignmentId: Number(mForm.assignmentId),
        scheduledAt: mForm.scheduledAt ? new Date(mForm.scheduledAt).toISOString() : undefined,
        location: mForm.location || undefined,
        isGroup: mForm.isGroup,
      }),
    onSuccess: onOk('Rencontre planifiée'),
    onError: onErr,
  });

  const endAssignment = useMutation({
    mutationFn: (id: number) => svc.endAssignment(id),
    onSuccess: () => {
      toast.success(tText('Assignation terminée'));
      invalidate();
    },
    onError: onErr,
  });

  const completeMeeting = useMutation({
    mutationFn: (id: number) => svc.completeMeeting(id),
    onSuccess: () => {
      toast.success(tText('Rencontre complétée'));
      invalidate();
    },
    onError: onErr,
  });

  const renderForm = () => {
    if (!showForm) return null;
    if (tab === 'journeys') {
      return (
        <div className="glass-card p-6 mb-6 grid gap-4 md:grid-cols-2">
          <div><label className="text-xs font-medium text-gray-500 mb-1 block">{tText('Nom *')}</label><input className="input w-full" value={jForm.name} onChange={(e) => setJForm({ ...jForm, name: e.target.value })} /></div>
          <div><label className="text-xs font-medium text-gray-500 mb-1 block">{tText('Type')}</label>
            <select className="input w-full" value={jForm.type} onChange={(e) => setJForm({ ...jForm, type: e.target.value })}>{JOURNEY_TYPES.map((t) => <option key={t} value={t}>{t}</option>)}</select>
          </div>
          <div><label className="text-xs font-medium text-gray-500 mb-1 block">{tText('Description')}</label><input className="input w-full" value={jForm.description} onChange={(e) => setJForm({ ...jForm, description: e.target.value })} /></div>
          <div><label className="text-xs font-medium text-gray-500 mb-1 block">{tText('Nombre d\'étapes')}</label><input type="number" className="input w-full" value={jForm.totalStages} onChange={(e) => setJForm({ ...jForm, totalStages: Number(e.target.value) })} /></div>
          <div className="md:col-span-2 flex justify-end gap-3">
            <button onClick={() => setShowForm(false)} className="btn-sm px-4 py-2 rounded-lg glass-card">{tText('Annuler')}</button>
            <button onClick={() => createJourney.mutate()} disabled={!jForm.name.trim() || createJourney.isPending} className="btn-primary btn-sm">{tText('Créer')}</button>
          </div>
        </div>
      );
    }
    if (tab === 'progress') {
      return (
        <div className="glass-card p-6 mb-6 grid gap-4 md:grid-cols-2">
          <div><label className="text-xs font-medium text-gray-500 mb-1 block">{tText('UUID du disciple *')}</label><input className="input w-full" placeholder="uuid" value={pForm.discipleId} onChange={(e) => setPForm({ ...pForm, discipleId: e.target.value })} /></div>
          <div><label className="text-xs font-medium text-gray-500 mb-1 block">{tText('Parcours')}</label>
            <select className="input w-full" value={pForm.journeyId} onChange={(e) => setPForm({ ...pForm, journeyId: e.target.value })}>
              <option value="">—</option>{journeys.map((j) => <option key={String(j.id)} value={String(j.id)}>{sv(j, 'name')}</option>)}
            </select>
          </div>
          <div className="md:col-span-2 flex justify-end gap-3">
            <button onClick={() => setShowForm(false)} className="btn-sm px-4 py-2 rounded-lg glass-card">{tText('Annuler')}</button>
            <button onClick={() => createProgress.mutate()} disabled={!pForm.discipleId.trim() || createProgress.isPending} className="btn-primary btn-sm">{tText('Créer')}</button>
          </div>
        </div>
      );
    }
    if (tab === 'assignments') {
      return (
        <div className="glass-card p-6 mb-6 grid gap-4 md:grid-cols-2">
          <div><label className="text-xs font-medium text-gray-500 mb-1 block">{tText('UUID mentor *')}</label><input className="input w-full" value={aForm.mentorId} onChange={(e) => setAForm({ ...aForm, mentorId: e.target.value })} /></div>
          <div><label className="text-xs font-medium text-gray-500 mb-1 block">{tText('UUID disciple *')}</label><input className="input w-full" value={aForm.discipleId} onChange={(e) => setAForm({ ...aForm, discipleId: e.target.value })} /></div>
          <div><label className="text-xs font-medium text-gray-500 mb-1 block">{tText('Parcours *')}</label>
            <select className="input w-full" value={aForm.journeyId} onChange={(e) => setAForm({ ...aForm, journeyId: e.target.value })}>
              <option value="">—</option>{journeys.map((j) => <option key={String(j.id)} value={String(j.id)}>{sv(j, 'name')}</option>)}
            </select>
          </div>
          <div><label className="text-xs font-medium text-gray-500 mb-1 block">{tText('Fréquence des rencontres (jours)')}</label><input type="number" className="input w-full" value={aForm.meetingFrequencyDays} onChange={(e) => setAForm({ ...aForm, meetingFrequencyDays: Number(e.target.value) })} /></div>
          <div className="md:col-span-2 flex justify-end gap-3">
            <button onClick={() => setShowForm(false)} className="btn-sm px-4 py-2 rounded-lg glass-card">{tText('Annuler')}</button>
            <button onClick={() => createAssignment.mutate()} disabled={!aForm.mentorId.trim() || !aForm.discipleId.trim() || !aForm.journeyId || createAssignment.isPending} className="btn-primary btn-sm">{tText('Créer')}</button>
          </div>
        </div>
      );
    }
    if (tab === 'meetings') {
      return (
        <div className="glass-card p-6 mb-6 grid gap-4 md:grid-cols-2">
          <div><label className="text-xs font-medium text-gray-500 mb-1 block">{tText('Assignation *')}</label>
            <select className="input w-full" value={mForm.assignmentId} onChange={(e) => setMForm({ ...mForm, assignmentId: e.target.value })}>
              <option value="">—</option>{assignments.map((a) => <option key={String(a.id)} value={String(a.id)}>#{sv(a, 'id')} {sv(a, 'mentorName')} → {sv(a, 'discipleName')}</option>)}
            </select>
          </div>
          <div><label className="text-xs font-medium text-gray-500 mb-1 block">{tText('Date planifiée')}</label><input type="datetime-local" className="input w-full" value={mForm.scheduledAt} onChange={(e) => setMForm({ ...mForm, scheduledAt: e.target.value })} /></div>
          <div><label className="text-xs font-medium text-gray-500 mb-1 block">{tText('Lieu')}</label><input className="input w-full" value={mForm.location} onChange={(e) => setMForm({ ...mForm, location: e.target.value })} /></div>
          <label className="flex items-center gap-2 text-sm text-gray-600"><input type="checkbox" checked={mForm.isGroup} onChange={(e) => setMForm({ ...mForm, isGroup: e.target.checked })} /> {tText('Groupe')}</label>
          <div className="md:col-span-2 flex justify-end gap-3">
            <button onClick={() => setShowForm(false)} className="btn-sm px-4 py-2 rounded-lg glass-card">{tText('Annuler')}</button>
            <button onClick={() => scheduleMeeting.mutate()} disabled={!mForm.assignmentId || scheduleMeeting.isPending} className="btn-primary btn-sm">{tText('Planifier')}</button>
          </div>
        </div>
      );
    }
    return null;
  };

  const spinner = <div className="flex justify-center py-12"><Loader2 className="w-8 h-8 animate-spin text-primary-500" /></div>;

  return (
    <div className="page-container">
      <div className="page-header">
        <div className="p-3 rounded-xl bg-gradient-to-br from-violet-500 to-purple-600 text-white shadow-lg"><Route className="w-6 h-6" /></div>
        <div><h1 className="page-title">Discipleship</h1><p className="page-subtitle">{tText('Parcours, progressions, assignations et rencontres')}</p></div>
        {tab !== 'reports' && (
          <button onClick={() => setShowForm(!showForm)} className="btn-primary btn-sm ml-auto inline-flex items-center gap-1"><Plus className="w-4 h-4" /> {tText('Nouveau')}</button>
        )}
      </div>

      <div className="flex flex-wrap gap-2 mb-6">
        {TABS.map((t) => (
          <button key={t} onClick={() => { setTab(t); setShowForm(false); }} className={`btn-sm px-4 py-2 rounded-lg ${tab === t ? 'btn-primary' : 'glass-card'}`}>{tText(t)}</button>
        ))}
      </div>

      {tab === 'journeys' && (
        <>
          {renderForm()}
          {loadingJourneys ? spinner : journeys.length === 0 ? <div className="glass-card p-10 text-center text-gray-500">{tText('Aucun parcours')}</div> : (
            <div className="space-y-3">
              {journeys.map((j) => (
                <div key={String(j.id)} className="glass-card p-5">
                  <div className="flex items-center justify-between mb-2">
                    <div>
                      <h3 className="font-semibold text-gray-800 dark:text-gray-200">{sv(j, 'name')}</h3>
                      <p className="text-xs text-gray-500">{sv(j, 'type')} • {sv(j, 'totalStages')} {tText('étapes')} • {sv(j, 'isActive') === 'true' ? tText('actif') : tText('inactif')}</p>
                    </div>
                    <button onClick={() => { setJourneyFilter(journeyFilter === Number(j.id) ? '' : Number(j.id)); }} className="text-xs btn-sm px-3 py-1 rounded-lg glass-card inline-flex items-center gap-1"><Layers className="w-3 h-3" /> {tText('Étapes')}</button>
                  </div>
                  {sv(j, 'description') !== '—' && <p className="text-sm text-gray-600">{sv(j, 'description')}</p>}
                  {journeyFilter === Number(j.id) && (
                    <div className="mt-3 border-t border-gray-200 dark:border-gray-700 pt-3 space-y-2">
                      {stages.length === 0 ? <p className="text-xs text-gray-500">{tText('Aucune étape')}</p> : stages.map((st, i) => (
                        <div key={String(st.id)} className="text-sm text-gray-700 dark:text-gray-300">{i + 1}. {sv(st, 'name')}{sv(st, 'description') !== '—' ? ` — ${sv(st, 'description')}` : ''}</div>
                      ))}
                    </div>
                  )}
                </div>
              ))}
            </div>
          )}
        </>
      )}

      {tab === 'progress' && (
        <>
          {renderForm()}
          {loadingProgress ? spinner : progress.length === 0 ? <div className="glass-card p-10 text-center text-gray-500">{tText('Aucune progression')}</div> : (
            <div className="space-y-3">
              {progress.map((p) => (
                <div key={String(p.id)} className="glass-card p-5 flex flex-wrap items-center gap-x-6 gap-y-1">
                  <span className="font-semibold text-gray-800 dark:text-gray-200">{sv(p, 'discipleName')}</span>
                  <span className="text-xs text-gray-500">{sv(p, 'journeyName')}</span>
                  <span className="text-xs text-gray-500">{tText('Étape')} : {sv(p, 'currentStageName')}</span>
                  <span className="text-xs text-gray-500">{sv(p, 'completedStages')}/{sv(p, 'totalStages')} {tText('étapes')}</span>
                  <span className="text-xs text-gray-500">{sv(p, 'completedRequirements')}/{sv(p, 'totalRequirements')} {tText('exigences')}</span>
                  <span className="ml-auto text-xs px-2 py-1 rounded-full bg-violet-100 text-violet-700">{sv(p, 'status')}</span>
                </div>
              ))}
            </div>
          )}
        </>
      )}

      {tab === 'assignments' && (
        <>
          {renderForm()}
          {loadingAssignments ? spinner : assignments.length === 0 ? <div className="glass-card p-10 text-center text-gray-500">{tText('Aucune assignation')}</div> : (
            <div className="space-y-3">
              {assignments.map((a) => (
                <div key={String(a.id)} className="glass-card p-5 flex flex-wrap items-center gap-x-6 gap-y-1">
                  <Users className="w-4 h-4 text-violet-500" />
                  <span className="text-sm text-gray-700 dark:text-gray-300">{sv(a, 'mentorName')} → {sv(a, 'discipleName')}</span>
                  <span className="text-xs text-gray-500">{tText('Prochaine')} : {sv(a, 'nextMeetingAt')}</span>
                  <span className="text-xs px-2 py-1 rounded-full bg-violet-100 text-violet-700">{sv(a, 'status')}</span>
                  {sv(a, 'status') === 'ACTIVE' && (
                    <button onClick={() => endAssignment.mutate(Number(a.id))} className="ml-auto text-red-400 hover:text-red-300 inline-flex items-center gap-1 text-xs"><Flag className="w-3 h-3" /> {tText('Terminer')}</button>
                  )}
                </div>
              ))}
            </div>
          )}
        </>
      )}

      {tab === 'meetings' && (
        <>
          {renderForm()}
          {loadingMeetings ? spinner : meetings.length === 0 ? <div className="glass-card p-10 text-center text-gray-500">{tText('Aucune rencontre')}</div> : (
            <div className="space-y-3">
              {meetings.map((m) => (
                <div key={String(m.id)} className="glass-card p-5 flex flex-wrap items-center gap-x-6 gap-y-1">
                  <CalendarCheck className="w-4 h-4 text-violet-500" />
                  <span className="text-sm text-gray-700 dark:text-gray-300">{sv(m, 'mentorName')} → {sv(m, 'discipleName')}</span>
                  <span className="text-xs text-gray-500">{sv(m, 'scheduledAt')}</span>
                  <span className="text-xs text-gray-500">{sv(m, 'location')}</span>
                  <span className="text-xs px-2 py-1 rounded-full bg-violet-100 text-violet-700">{sv(m, 'status')}</span>
                  {sv(m, 'status') === 'SCHEDULED' && (
                    <button onClick={() => completeMeeting.mutate(Number(m.id))} className="ml-auto text-xs btn-sm px-3 py-1 rounded-lg glass-card">{tText('Compléter')}</button>
                  )}
                </div>
              ))}
            </div>
          )}
        </>
      )}

      {tab === 'reports' && (
        <div className="space-y-4">
          <select className="input w-full md:w-72" value={journeyId} onChange={(e) => setJourneyId(e.target.value === '' ? '' : Number(e.target.value))}>
            <option value="">{tText('Choisir un parcours…')}</option>
            {journeys.map((j) => <option key={String(j.id)} value={String(j.id)}>{sv(j, 'name')}</option>)}
          </select>
          {report && (
            <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
              <div className="stat-card"><p className="stat-value">{sv(report, 'totalDisciples')}</p><p className="stat-label">{tText('Total disciples')}</p></div>
              <div className="stat-card"><p className="stat-value">{sv(report, 'activeDisciples')}</p><p className="stat-label">{tText('Actifs')}</p></div>
              <div className="stat-card"><p className="stat-value">{sv(report, 'completedDisciples')}</p><p className="stat-label">{tText('Terminés')}</p></div>
              <div className="stat-card"><p className="stat-value">{sv(report, 'stalledDisciples')}</p><p className="stat-label">{tText('En pause')}</p></div>
            </div>
          )}
          {report && (
            <div className="glass-card p-5 text-sm text-gray-600 flex flex-wrap gap-x-6">
              <span>{tText('Complétion moyenne')} : {sv(report, 'averageCompletion')}%</span>
              <span>{tText('Rencontres')} : {sv(report, 'completedMeetings')}/{sv(report, 'totalMeetings')}</span>
              <span>{tText('Généré')} : {sv(report, 'generatedAt')}</span>
            </div>
          )}
          {topMentors.length > 0 && (
            <div className="glass-card p-5">
              <h3 className="font-semibold text-gray-800 dark:text-gray-200 mb-3">{tText('Top mentors')}</h3>
              {topMentors.map((tm, i) => (
                <div key={i} className="text-sm text-gray-700 dark:text-gray-300 py-1 border-b border-gray-100 last:border-0">
                  {i + 1}. {Object.entries(tm).map(([k, v]) => `${k}: ${v == null ? '—' : String(v)}`).join(' • ')}
                </div>
              ))}
            </div>
          )}
          {journeyId === '' && <div className="glass-card p-10 text-center text-gray-500">{tText('Sélectionnez un parcours pour voir le rapport')}</div>}
        </div>
      )}
    </div>
  );
}
