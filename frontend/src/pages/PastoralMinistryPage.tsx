import { useMemo, useState, type ReactNode } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import api, { getErrorMessage } from '@/lib/api';
import { useAuth } from '@/contexts/AuthContext';
import { tText } from '@/i18n';
import type { OrganizationNode } from '@/types/tenant';
import {
  Crown, Loader2, X, Plus, CheckCircle2, Ban, ArrowLeftRight, History,
  Users, Building2, AlertTriangle,
} from 'lucide-react';
import toast from 'react-hot-toast';

/**
 * §G4.3 — Ministère pastoral : organigramme des pasteurs par unité (campus…),
 * mandats en cours et passés, wizard de transfert avec aperçu des impacts,
 * consultation de l'historique d'un pasteur (« qui dirigeait le campus A en 2024 ? »).
 *
 * Parité garde avec le backend (PastorateController) :
 * - lecture : ADMIN, PASTEUR, PASTOR_PRINCIPAL ;
 * - écritures (nommer, clore, transférer/approuver) : ADMIN, PASTOR_PRINCIPAL.
 */

interface PastorateAppointment {
  id: string;
  pastorId: string;
  organizationUnitId: string;
  roleCode: string;
  title: string | null;
  startDate: string;
  endDate: string | null;
  appointmentType: string;
  status: string;
  reason: string | null;
}

interface PastorateTransfer {
  id: string;
  pastorId: string;
  fromOrgUnitId: string | null;
  toOrgUnitId: string;
  transferDate: string;
  reason: string | null;
  status: string;
}

interface PastorOption { id: string; firstName: string; lastName: string; email: string }

type Tab = 'mandates' | 'orgchart' | 'transfers' | 'history';

export default function PastoralMinistryPage() {
  const { user } = useAuth();
  const queryClient = useQueryClient();
  const [tab, setTab] = useState<Tab>('mandates');

  // PASTOR_PRINCIPAL est un rôle de tenant (pas un UserRole plateforme) — d'où le string large.
  const activeRole = user?.activeRole as string | undefined;
  // Parité exacte des @PreAuthorize serveur.
  const canWrite = activeRole === 'ADMIN' || activeRole === 'PASTOR_PRINCIPAL';

  const { data: appointments } = useQuery({
    queryKey: ['pastorate', 'appointments'],
    queryFn: async () => (await api.get('/pastorate/appointments')).data as PastorateAppointment[],
  });

  const { data: transfers } = useQuery({
    queryKey: ['pastorate', 'transfers'],
    queryFn: async () => (await api.get('/pastorate/transfers')).data as PastorateTransfer[],
  });

  const { data: orgUnits } = useQuery({
    queryKey: ['pastorate', 'org-units'],
    queryFn: async () => (await api.get('/org/tree/flat')).data as OrganizationNode[],
  });

  const { data: pastors } = useQuery({
    queryKey: ['pastorate', 'pastors'],
    queryFn: async () => {
      const res = await api.get('/users?role=PASTEUR&size=100');
      return res.data.content as PastorOption[];
    },
  });

  const unitName = useMemo(() => {
    const map = new Map((orgUnits ?? []).map((u) => [u.id, u.name]));
    return (id: string | null | undefined) => (id ? map.get(id) ?? id.slice(0, 8) : '—');
  }, [orgUnits]);

  const pastorName = useMemo(() => {
    const map = new Map((pastors ?? []).map((p) => [p.id, `${p.firstName} ${p.lastName}`]));
    return (id: string) => map.get(id) ?? id.slice(0, 8);
  }, [pastors]);

  const invalidatePastorate = () => queryClient.invalidateQueries({ queryKey: ['pastorate'] });

  const tabs: { id: Tab; label: string; icon: typeof Crown }[] = [
    { id: 'mandates', label: 'Mandats', icon: Crown },
    { id: 'orgchart', label: 'Organigramme', icon: Building2 },
    { id: 'transfers', label: 'Transferts', icon: ArrowLeftRight },
    { id: 'history', label: 'Historique', icon: History },
  ];

  return (
    <div className="page-container">
      <div className="glass-card p-5 sm:p-6 mb-6 animate-slide-up">
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
          <div className="flex items-center gap-4">
            <div className="w-14 h-14 rounded-2xl bg-gradient-to-br from-gold-400 to-gold-600 flex items-center justify-center shadow-glow">
              <Crown className="w-7 h-7 text-white" />
            </div>
            <div>
              <h1 className="page-title">{tText('Ministère pastoral')}</h1>
              <p className="text-xs text-gray-500 mt-1">
                {tText('Nominations, mandats et transferts des pasteurs par campus')}
              </p>
            </div>
          </div>
        </div>
        <div className="flex flex-wrap gap-1 mt-5 border-b border-gray-200/60 dark:border-gray-700/60 pb-3">
          {tabs.map(({ id, label, icon: Icon }) => (
            <button
              key={id}
              onClick={() => setTab(id)}
              className={`inline-flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-xs font-semibold transition-all ${
                tab === id
                  ? 'bg-primary-500/10 text-primary-600 dark:text-primary-400 ring-1 ring-primary-500/30'
                  : 'text-gray-500 hover:bg-white/40 dark:hover:bg-gray-800/40'
              }`}
            >
              <Icon className="w-3.5 h-3.5" /> {tText(label)}
            </button>
          ))}
        </div>
      </div>

      {tab === 'mandates' && (
        <MandatesTab
          appointments={appointments}
          canWrite={canWrite}
          unitName={unitName}
          pastorName={pastorName}
          onSaved={invalidatePastorate}
        />
      )}
      {tab === 'orgchart' && (
        <OrgChartTab orgUnits={orgUnits ?? []} appointments={appointments ?? []} pastorName={pastorName} unitName={unitName} />
      )}
      {tab === 'transfers' && (
        <TransfersTab
          transfers={transfers}
          appointments={appointments}
          pastors={pastors}
          orgUnits={orgUnits}
          canWrite={canWrite}
          unitName={unitName}
          pastorName={pastorName}
          onSaved={invalidatePastorate}
        />
      )}
      {tab === 'history' && (
        <HistoryTab pastors={pastors} unitName={unitName} />
      )}
    </div>
  );
}

/* ============================ MANDATS ============================ */

function MandatesTab({ appointments, canWrite, unitName, pastorName, onSaved }: {
  appointments: PastorateAppointment[] | undefined;
  canWrite: boolean;
  unitName: (id: string | null | undefined) => string;
  pastorName: (id: string) => string;
  onSaved: () => void;
}) {
  const [showCreate, setShowCreate] = useState(false);
  const [ending, setEnding] = useState<PastorateAppointment | null>(null);

  const active = (appointments ?? []).filter((a) => a.status === 'ACTIVE');
  const past = (appointments ?? []).filter((a) => a.status !== 'ACTIVE');

  return (
    <div className="space-y-6">
      <div className="glass-card p-5 animate-slide-up">
        <div className="flex items-center justify-between mb-4">
          <h3 className="text-base font-semibold text-gray-900 dark:text-gray-100 flex items-center gap-2">
            <Crown className="w-4 h-4 text-gold-500" /> {tText('Mandats en cours')} ({active.length})
          </h3>
          {canWrite && (
            <button onClick={() => setShowCreate(true)} className="btn-primary btn-sm" data-testid="pastoral-create-btn">
              <Plus className="w-4 h-4" /> {tText('Nommer un pasteur')}
            </button>
          )}
        </div>
        {active.length > 0 ? (
          <div className="os-list">
            {active.map((a) => (
              <div key={a.id} className="os-row">
                <div className="flex-1 min-w-0">
                  <p className="text-sm font-medium text-gray-900 dark:text-gray-100 truncate">
                    {pastorName(a.pastorId)}
                    <span className="text-gray-500"> · {unitName(a.organizationUnitId)}</span>
                  </p>
                  <p className="text-xs text-gray-500 truncate">
                    {a.roleCode}{a.title ? ` · ${a.title}` : ''} · {tText('depuis')} {a.startDate}
                  </p>
                </div>
                <div className="flex items-center gap-2 flex-shrink-0">
                  <span className="badge badge-success text-[10px]">{tText('Actif')}</span>
                  {canWrite && (
                    <button
                      onClick={() => setEnding(a)}
                      className="p-1.5 rounded-lg hover:bg-red-50 dark:hover:bg-red-900/20 text-red-500"
                      title={tText('Clore le mandat')}
                      data-testid={`pastoral-end-${a.id}`}
                    >
                      <Ban className="w-4 h-4" />
                    </button>
                  )}
                </div>
              </div>
            ))}
          </div>
        ) : (
          <p className="text-sm text-gray-500 py-6 text-center">{tText('Aucun mandat actif')}</p>
        )}
      </div>

      {past.length > 0 && (
        <div className="glass-card p-5 animate-slide-up">
          <h3 className="text-base font-semibold text-gray-900 dark:text-gray-100 mb-4 flex items-center gap-2">
            <History className="w-4 h-4 text-gray-400" /> {tText('Mandats passés')} ({past.length})
          </h3>
          <div className="os-list">
            {past.map((a) => (
              <div key={a.id} className="os-row">
                <div className="flex-1 min-w-0">
                  <p className="text-sm font-medium text-gray-700 dark:text-gray-300 truncate">
                    {pastorName(a.pastorId)}
                    <span className="text-gray-500"> · {unitName(a.organizationUnitId)}</span>
                  </p>
                  <p className="text-xs text-gray-500 truncate">
                    {a.roleCode} · {a.startDate} → {a.endDate ?? '—'}
                    {a.reason ? ` · ${a.reason}` : ''}
                  </p>
                </div>
                <span className="badge text-[10px] badge-gray">{tText(a.status.toLowerCase())}</span>
              </div>
            ))}
          </div>
        </div>
      )}

      {showCreate && <CreateAppointmentModal onClose={() => setShowCreate(false)} onSaved={onSaved} unitName={unitName} />}
      {ending && <EndAppointmentModal appointment={ending} pastorLabel={pastorName(ending.pastorId)} onClose={() => setEnding(null)} onSaved={onSaved} />}
    </div>
  );
}

function CreateAppointmentModal({ onClose, onSaved, unitName }: {
  onClose: () => void;
  onSaved: () => void;
  unitName: (id: string | null | undefined) => string;
}) {
  const { data: pastors } = useQuery({
    queryKey: ['pastorate', 'pastors'],
    queryFn: async () => {
      const res = await api.get('/users?role=PASTEUR&size=100');
      return res.data.content as PastorOption[];
    },
  });
  const { data: units } = useQuery({
    queryKey: ['pastorate', 'org-units'],
    queryFn: async () => (await api.get('/org/tree/flat')).data as OrganizationNode[],
  });
  const [form, setForm] = useState({
    pastorId: '', organizationUnitId: '', roleCode: 'PASTEUR', title: '',
    appointmentType: 'NOMINATION', startDate: new Date().toISOString().slice(0, 10),
  });

  const create = useMutation({
    mutationFn: async () => {
      await api.post('/pastorate/appointments', {
        pastorId: form.pastorId,
        organizationUnitId: form.organizationUnitId,
        roleCode: form.roleCode,
        title: form.title || null,
        appointmentType: form.appointmentType,
        startDate: form.startDate,
      });
    },
    onSuccess: () => {
      toast.success(tText('Pasteur nommé'));
      onSaved();
      onClose();
    },
    onError: (err) => toast.error(getErrorMessage(err)),
  });

  // Règle serveur G4.3 (miroir UX) : pas de double mandat actif sur un AUTRE unité.
  const selectedPastorActive = useQuery({
    queryKey: ['pastorate', 'appointments'],
    queryFn: async () => (await api.get('/pastorate/appointments')).data as PastorateAppointment[],
    enabled: !!form.pastorId,
  });
  const impactWarning = selectedPastorActive.data?.find(
    (a) => a.pastorId === form.pastorId && a.status === 'ACTIVE' && a.organizationUnitId !== form.organizationUnitId,
  );

  return (
    <ModalShell title="Nommer un pasteur" subtitle="Un pasteur ne peut porter qu'un seul mandat actif" onClose={onClose}>
      <div className="space-y-3">
        <select className="input" data-testid="apt-pastor" value={form.pastorId} onChange={(e) => setForm({ ...form, pastorId: e.target.value })}>
          <option value="">{tText('Pasteur')}…</option>
          {pastors?.map((p) => <option key={p.id} value={p.id}>{p.firstName} {p.lastName}</option>)}
        </select>
        <select className="input" data-testid="apt-unit" value={form.organizationUnitId} onChange={(e) => setForm({ ...form, organizationUnitId: e.target.value })}>
          <option value="">{tText('Unité (église/campus)')}…</option>
          {units?.filter((u) => u.status === 'ACTIVE').map((u) => <option key={u.id} value={u.id}>{u.name} ({u.type})</option>)}
        </select>
        <div className="grid grid-cols-2 gap-3">
          <input className="input" placeholder={tText('Clé de rôle (ex: PASTEUR)')} value={form.roleCode} onChange={(e) => setForm({ ...form, roleCode: e.target.value })} data-testid="apt-role" />
          <input className="input" placeholder={tText('Titre (ex: Pasteur de campus)')} value={form.title} onChange={(e) => setForm({ ...form, title: e.target.value })} />
        </div>
        <div className="grid grid-cols-2 gap-3">
          <select className="input" value={form.appointmentType} onChange={(e) => setForm({ ...form, appointmentType: e.target.value })}>
            {['NOMINATION', 'TRANSFERT', 'SUCCESSION', 'ADJOINT'].map((t) => <option key={t} value={t}>{tText(t)}</option>)}
          </select>
          <input type="date" className="input" value={form.startDate} onChange={(e) => setForm({ ...form, startDate: e.target.value })} />
        </div>
        {impactWarning && (
          <p className="text-xs text-amber-600 flex items-start gap-1.5 p-2.5 rounded-xl bg-amber-50 dark:bg-amber-900/20">
            <AlertTriangle className="w-3.5 h-3.5 mt-0.5 flex-shrink-0" />
            {tText('Ce pasteur a déjà un mandat actif')} : {unitName(impactWarning.organizationUnitId)} ({impactWarning.roleCode}) — {tText('le serveur refusera tant que le mandat n\'est pas clos.')}
          </p>
        )}
      </div>
      <div className="modal-footer px-0 pb-0">
        <button onClick={onClose} className="btn-secondary btn-sm">{tText('Annuler')}</button>
        <button
          onClick={() => create.mutate()}
          disabled={!form.pastorId || !form.organizationUnitId || !form.roleCode || create.isPending}
          className="btn-primary btn-sm"
          data-testid="apt-submit"
        >
          {create.isPending ? <Loader2 className="w-4 h-4 animate-spin" /> : <CheckCircle2 className="w-4 h-4" />}
          {tText('Nommer')}
        </button>
      </div>
    </ModalShell>
  );
}

function EndAppointmentModal({ appointment, pastorLabel, onClose, onSaved }: {
  appointment: PastorateAppointment;
  pastorLabel: string;
  onClose: () => void;
  onSaved: () => void;
}) {
  const [reason, setReason] = useState('');
  const end = useMutation({
    mutationFn: async () => {
      await api.delete(`/pastorate/appointments/${appointment.id}`, { params: { reason: reason || 'Fin de mandat' } });
    },
    onSuccess: () => {
      toast.success(tText('Mandat clos'));
      onSaved();
      onClose();
    },
    onError: (err) => toast.error(getErrorMessage(err)),
  });

  return (
    <ModalShell title="Clore un mandat" subtitle={pastorLabel} onClose={onClose}>
      <div className="space-y-3">
        <p className="text-xs text-gray-500">
          {tText('Les permissions liées à ce mandat seront retirées immédiatement (rôle vivant §G4.4).')}
        </p>
        <input className="input" placeholder={tText('Motif (optionnel)')} value={reason} onChange={(e) => setReason(e.target.value)} data-testid="end-reason" />
      </div>
      <div className="modal-footer px-0 pb-0">
        <button onClick={onClose} className="btn-secondary btn-sm">{tText('Annuler')}</button>
        <button onClick={() => end.mutate()} disabled={end.isPending} className="btn-primary btn-sm bg-red-600 hover:bg-red-700" data-testid="end-submit">
          {end.isPending ? <Loader2 className="w-4 h-4 animate-spin" /> : <Ban className="w-4 h-4" />}
          {tText('Clore le mandat')}
        </button>
      </div>
    </ModalShell>
  );
}

/* ============================ ORGANIGRAMME ============================ */

function OrgChartTab({ orgUnits, appointments, pastorName, unitName }: {
  orgUnits: OrganizationNode[];
  appointments: PastorateAppointment[];
  pastorName: (id: string) => string;
  unitName: (id: string | null | undefined) => string;
}) {
  const active = appointments.filter((a) => a.status === 'ACTIVE');
  // Unités qui portent un mandat, plus les campus/églises sans pasteur (angles morts visibles).
  const unitsWithMandate = new Set(active.map((a) => a.organizationUnitId));
  const pastoralUnits = orgUnits.filter((u) =>
    ['ROOT_CHURCH', 'CHURCH', 'CAMPUS', 'SUB_CHURCH', 'DISTRICT', 'REGION'].includes(u.type) && u.status === 'ACTIVE',
  );

  return (
    <div className="glass-card p-5 animate-slide-up">
      <h3 className="text-base font-semibold text-gray-900 dark:text-gray-100 mb-4 flex items-center gap-2">
        <Building2 className="w-4 h-4 text-primary-500" /> {tText('Pasteurs par unité')}
      </h3>
      <div className="os-list">
        {pastoralUnits.map((u) => {
          const pastorsHere = active.filter((a) => a.organizationUnitId === u.id);
          return (
            <div key={u.id} className="os-row">
              <div className="flex-1 min-w-0">
                <p className="text-sm font-medium text-gray-900 dark:text-gray-100 truncate">
                  {u.name} <span className="text-[10px] text-gray-400 uppercase">{u.type.replace(/_/g, ' ')}</span>
                </p>
                <p className="text-xs text-gray-500 truncate">
                  {pastorsHere.length > 0
                    ? pastorsHere.map((a) => `${pastorName(a.pastorId)} (${a.roleCode})`).join(', ')
                    : unitsWithMandate.has(u.id) ? '' : tText('Aucun pasteur en poste')}
                </p>
              </div>
              {pastorsHere.length > 0
                ? <span className="badge badge-success text-[10px]">{pastorsHere.length}</span>
                : <span className="badge badge-warning text-[10px]">{tText('À couvrir')}</span>}
            </div>
          );
        })}
      </div>
      {pastoralUnits.length === 0 && (
        <p className="text-sm text-gray-500 py-6 text-center">{tText('Aucune unité pastorale configurée')} — {unitName(null)}</p>
      )}
    </div>
  );
}

/* ============================ TRANSFERTS ============================ */

function TransfersTab({ transfers, appointments, pastors, orgUnits, canWrite, unitName, pastorName, onSaved }: {
  transfers: PastorateTransfer[] | undefined;
  appointments: PastorateAppointment[] | undefined;
  pastors: PastorOption[] | undefined;
  orgUnits: OrganizationNode[] | undefined;
  canWrite: boolean;
  unitName: (id: string | null | undefined) => string;
  pastorName: (id: string) => string;
  onSaved: () => void;
}) {
  const [showWizard, setShowWizard] = useState(false);

  const decide = useMutation({
    mutationFn: async ({ id, action, reason }: { id: string; action: 'approve' | 'reject'; reason?: string }) => {
      if (action === 'approve') await api.post(`/pastorate/transfers/${id}/approve`);
      else await api.post(`/pastorate/transfers/${id}/reject`, null, { params: { reason: reason || 'Refusé' } });
    },
    onSuccess: (_d, v) => {
      toast.success(v.action === 'approve' ? tText('Transfert approuvé') : tText('Transfert refusé'));
      onSaved();
    },
    onError: (err) => toast.error(getErrorMessage(err)),
  });

  const pending = (transfers ?? []).filter((t) => t.status === 'PENDING');
  const done = (transfers ?? []).filter((t) => t.status !== 'PENDING');

  return (
    <div className="space-y-6">
      <div className="glass-card p-5 animate-slide-up">
        <div className="flex items-center justify-between mb-4">
          <h3 className="text-base font-semibold text-gray-900 dark:text-gray-100 flex items-center gap-2">
            <ArrowLeftRight className="w-4 h-4 text-primary-500" /> {tText('Transferts en attente')} ({pending.length})
          </h3>
          {canWrite && (
            <button onClick={() => setShowWizard(true)} className="btn-primary btn-sm" data-testid="transfer-create-btn">
              <Plus className="w-4 h-4" /> {tText('Transfer un pasteur')}
            </button>
          )}
        </div>
        {pending.length > 0 ? (
          <div className="os-list">
            {pending.map((t) => (
              <div key={t.id} className="os-row">
                <div className="flex-1 min-w-0">
                  <p className="text-sm font-medium text-gray-900 dark:text-gray-100 truncate">
                    {pastorName(t.pastorId)} · {unitName(t.fromOrgUnitId)} → {unitName(t.toOrgUnitId)}
                  </p>
                  <p className="text-xs text-gray-500 truncate">{t.transferDate}{t.reason ? ` · ${t.reason}` : ''}</p>
                </div>
                {canWrite && (
                  <div className="flex items-center gap-2 flex-shrink-0">
                    <button
                      onClick={() => decide.mutate({ id: t.id, action: 'approve' })}
                      className="p-1.5 rounded-lg hover:bg-green-50 dark:hover:bg-green-900/20 text-green-600"
                      title={tText('Approuver')}
                      data-testid={`transfer-approve-${t.id}`}
                    >
                      <CheckCircle2 className="w-4 h-4" />
                    </button>
                    <button
                      onClick={() => decide.mutate({ id: t.id, action: 'reject' })}
                      className="p-1.5 rounded-lg hover:bg-red-50 dark:hover:bg-red-900/20 text-red-500"
                      title={tText('Refuser')}
                      data-testid={`transfer-reject-${t.id}`}
                    >
                      <Ban className="w-4 h-4" />
                    </button>
                  </div>
                )}
              </div>
            ))}
          </div>
        ) : (
          <p className="text-sm text-gray-500 py-6 text-center">{tText('Aucun transfert en attente')}</p>
        )}
      </div>

      {done.length > 0 && (
        <div className="glass-card p-5 animate-slide-up">
          <h3 className="text-base font-semibold text-gray-900 dark:text-gray-100 mb-4">{tText('Historique des transferts')} ({done.length})</h3>
          <div className="os-list">
            {done.map((t) => (
              <div key={t.id} className="os-row">
                <div className="flex-1 min-w-0">
                  <p className="text-sm font-medium text-gray-700 dark:text-gray-300 truncate">
                    {pastorName(t.pastorId)} · {unitName(t.fromOrgUnitId)} → {unitName(t.toOrgUnitId)}
                  </p>
                  <p className="text-xs text-gray-500">{t.transferDate}</p>
                </div>
                <span className={`badge text-[10px] ${t.status === 'COMPLETED' || t.status === 'APPROVED' ? 'badge-success' : 'badge-danger'}`}>
                  {tText(t.status)}
                </span>
              </div>
            ))}
          </div>
        </div>
      )}

      {showWizard && (
        <TransferWizard
          pastors={pastors ?? []}
          orgUnits={orgUnits ?? []}
          appointments={appointments ?? []}
          onClose={() => setShowWizard(false)}
          onSaved={onSaved}
        />
      )}
    </div>
  );
}

/** Wizard de transfert avec APERÇU DES IMPACTS (§G4.3-3). */
function TransferWizard({ pastors, orgUnits, appointments, onClose, onSaved }: {
  pastors: PastorOption[];
  orgUnits: OrganizationNode[];
  appointments: PastorateAppointment[];
  onClose: () => void;
  onSaved: () => void;
}) {
  const [pastorId, setPastorId] = useState('');
  const [toOrgUnitId, setToOrgUnitId] = useState('');
  const [reason, setReason] = useState('');

  const currentMandates = appointments.filter((a) => a.pastorId === pastorId && a.status === 'ACTIVE');

  const create = useMutation({
    mutationFn: async () => {
      await api.post('/pastorate/transfers', null, { params: { pastorId, toOrgUnitId, reason: reason || 'Transfert pastoral' } });
    },
    onSuccess: () => {
      toast.success(tText('Demande de transfert créée'));
      onSaved();
      onClose();
    },
    onError: (err) => toast.error(getErrorMessage(err)),
  });

  return (
    <ModalShell title="Transfer un pasteur" subtitle="Aperçu des impacts avant validation" onClose={onClose}>
      <div className="space-y-3">
        <select className="input" data-testid="tr-pastor" value={pastorId} onChange={(e) => setPastorId(e.target.value)}>
          <option value="">{tText('Pasteur à transférer')}…</option>
          {pastors.map((p) => <option key={p.id} value={p.id}>{p.firstName} {p.lastName}</option>)}
        </select>
        <select className="input" data-testid="tr-unit" value={toOrgUnitId} onChange={(e) => setToOrgUnitId(e.target.value)}>
          <option value="">{tText('Unité cible (église/campus)')}…</option>
          {orgUnits.filter((u) => u.status === 'ACTIVE').map((u) => <option key={u.id} value={u.id}>{u.name} ({u.type.replace(/_/g, ' ')})</option>)}
        </select>
        <input className="input" placeholder={tText('Motif du transfert')} value={reason} onChange={(e) => setReason(e.target.value)} data-testid="tr-reason" />

        {pastorId && (
          <div className="p-3 rounded-xl bg-white/30 dark:bg-gray-800/30 text-xs space-y-1.5" data-testid="tr-impact">
            <p className="font-semibold text-gray-700 dark:text-gray-300 flex items-center gap-1.5">
              <AlertTriangle className="w-3.5 h-3.5 text-amber-500" /> {tText('Impacts prévis')}
            </p>
            {currentMandates.length === 0 ? (
              <p className="text-gray-500">{tText('Aucun mandat actif : ce transfert créera une nouvelle nomination à l\'approbation.')}</p>
            ) : (
              currentMandates.map((a) => (
                <p key={a.id} className="text-gray-600 dark:text-gray-400">
                  • {tText('Mandat actif à clore')} : {a.roleCode} — {a.title ?? a.organizationUnitId.slice(0, 8)} ({tText('depuis')} {a.startDate})
                </p>
              ))
            )}
            <p className="text-gray-500">{tText('À l\'approbation : permissions recalées en direct (rôles vivants) + notification au pasteur.')}</p>
          </div>
        )}
      </div>
      <div className="modal-footer px-0 pb-0">
        <button onClick={onClose} className="btn-secondary btn-sm">{tText('Annuler')}</button>
        <button
          onClick={() => create.mutate()}
          disabled={!pastorId || !toOrgUnitId || create.isPending}
          className="btn-primary btn-sm"
          data-testid="tr-submit"
        >
          {create.isPending ? <Loader2 className="w-4 h-4 animate-spin" /> : <ArrowLeftRight className="w-4 h-4" />}
          {tText('Demander le transfert')}
        </button>
      </div>
    </ModalShell>
  );
}

/* ============================ HISTORIQUE ============================ */

function HistoryTab({ pastors, unitName }: { pastors: PastorOption[] | undefined; unitName: (id: string | null | undefined) => string }) {
  const [pastorId, setPastorId] = useState('');
  const { data, isFetching } = useQuery({
    queryKey: ['pastorate', 'history', pastorId],
    queryFn: async () => (await api.get(`/pastorate/pastors/${pastorId}/history`)).data as {
      appointments: PastorateAppointment[];
      transfers: PastorateTransfer[];
    },
    enabled: !!pastorId,
  });

  return (
    <div className="glass-card p-5 animate-slide-up">
      <h3 className="text-base font-semibold text-gray-900 dark:text-gray-100 mb-4 flex items-center gap-2">
        <Users className="w-4 h-4 text-primary-500" /> {tText("Historique d'un pasteur")}
      </h3>
      <select className="input mb-4" value={pastorId} onChange={(e) => setPastorId(e.target.value)} data-testid="hist-pastor">
        <option value="">{tText('Sélectionner un pasteur')}…</option>
        {pastors?.map((p) => <option key={p.id} value={p.id}>{p.firstName} {p.lastName}</option>)}
      </select>
      {isFetching && <PanelLoader />}
      {data && (
        <div className="space-y-4">
          <div>
            <p className="text-xs font-semibold text-gray-500 uppercase tracking-wide mb-2">{tText('Mandats')} ({data.appointments.length})</p>
            <div className="os-list">
              {data.appointments.map((a) => (
                <div key={a.id} className="os-row">
                  <div className="flex-1 min-w-0">
                    <p className="text-sm font-medium text-gray-900 dark:text-gray-100 truncate">{a.roleCode} · {unitName(a.organizationUnitId)}</p>
                    <p className="text-xs text-gray-500">{a.startDate} → {a.endDate ?? tText('en cours')} · {tText(a.appointmentType)}</p>
                  </div>
                  <span className={`badge text-[10px] ${a.status === 'ACTIVE' ? 'badge-success' : 'badge-gray'}`}>{tText(a.status)}</span>
                </div>
              ))}
            </div>
          </div>
          <div>
            <p className="text-xs font-semibold text-gray-500 uppercase tracking-wide mb-2">{tText('Transferts effectués')} ({data.transfers.length})</p>
            <div className="os-list">
              {data.transfers.map((t) => (
                <div key={t.id} className="os-row">
                  <div className="flex-1 min-w-0">
                    <p className="text-sm font-medium text-gray-900 dark:text-gray-100 truncate">{unitName(t.fromOrgUnitId)} → {unitName(t.toOrgUnitId)}</p>
                    <p className="text-xs text-gray-500">{t.transferDate}{t.reason ? ` · ${t.reason}` : ''}</p>
                  </div>
                </div>
              ))}
              {data.transfers.length === 0 && <p className="p-3 text-xs text-gray-400">{tText('Aucun transfert')}</p>}
            </div>
          </div>
        </div>
      )}
      {!pastorId && !isFetching && (
        <p className="text-sm text-gray-500 py-6 text-center">{tText('Sélectionnez un pasteur pour consulter son historique complet')}</p>
      )}
    </div>
  );
}

/* ============================ PARTAGÉS ============================ */

function ModalShell({ title, subtitle, onClose, children }: {
  title: string;
  subtitle?: string;
  onClose: () => void;
  children: ReactNode;
}) {
  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal-content max-w-md w-full" onClick={(e) => e.stopPropagation()}>
        <div className="modal-header">
          <div className="flex items-center gap-3">
            <div className="p-2.5 rounded-xl bg-gold-100 dark:bg-gold-900/30">
              <Crown className="w-5 h-5 text-gold-600 dark:text-gold-400" />
            </div>
            <div>
              <h3 className="text-lg font-semibold text-gray-900 dark:text-gray-100">{tText(title)}</h3>
              {subtitle && <p className="text-xs text-gray-500">{tText(subtitle)}</p>}
            </div>
          </div>
          <button onClick={onClose} className="p-1.5 rounded-lg hover:bg-gray-100 dark:hover:bg-gray-800">
            <X className="w-5 h-5 text-gray-400" />
          </button>
        </div>
        <div className="modal-body">{children}</div>
      </div>
    </div>
  );
}

function PanelLoader() {
  return <div className="flex justify-center py-8"><Loader2 className="w-5 h-5 animate-spin text-gray-400" /></div>;
}
