import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useParams, Link, useNavigate } from 'react-router-dom';
import api, { getErrorMessage } from '@/lib/api';
import toast from 'react-hot-toast';
import {
  ArrowLeft, UserRound, Building2, Users2, ListTodo, ClipboardCheck, Gavel,
  FileText, Star, Calendar, StickyNote, Megaphone, ArrowLeftRight, History,
  Loader2, Plus, Send, Trash2, FolderOpen, Bell, ExternalLink, Target, TrendingUp,
  CheckCircle2, CalendarDays, UserCheck, UserX, Download,
} from 'lucide-react';

type Dossier = any;

const TABS = [
  { key: 'profil', label: 'Profil', icon: UserRound },
  { key: 'appartenance', label: 'Appartenance', icon: Building2 },
  { key: 'affectations', label: 'Affectations', icon: Users2 },
  { key: 'taches', label: 'Tâches', icon: ListTodo },
  { key: 'presences', label: 'Présences', icon: ClipboardCheck },
  { key: 'discipline', label: 'Discipline', icon: Gavel },
  { key: 'rapports', label: 'Rapports', icon: FileText },
  { key: 'evaluations', label: 'Évaluations', icon: Star },
  { key: 'evenements', label: 'Événements', icon: Calendar },
  { key: 'annonces', label: 'Annonces', icon: Megaphone },
  { key: 'notes', label: 'Notes', icon: StickyNote },
  { key: 'objectifs', label: 'Objectifs', icon: Target },
  { key: 'documents', label: 'Documents', icon: FolderOpen },
  { key: 'transferts', label: 'Transferts', icon: ArrowLeftRight },
  { key: 'activite', label: 'Activité', icon: History },
] as const;

const STATUT_LABELS: Record<string, string> = {
  ACTIF: 'Actif', EN_INTEGRATION: 'En intégration', EN_VEILLE: 'En veille', DECROCHE: 'Décroché',
};
const STATUT_BADGE: Record<string, string> = {
  ACTIF: 'badge-success', EN_INTEGRATION: 'badge-info', EN_VEILLE: 'badge-warning', DECROCHE: 'badge-inactive',
};
const ORIGINE_LABELS: Record<string, string> = {
  MANUEL: 'Ajout manuel', SIGNUP: 'Inscription', TRANSFERT: 'Transfert',
};
const TASK_BADGE: Record<string, string> = {
  A_FAIRE: 'badge-gray', EN_COURS: 'badge-info', BLOQUEE: 'badge-danger',
  TERMINEE: 'badge-success', VALIDEE: 'badge-success', ANNULEE: 'badge-inactive',
};

export default function DepartmentMemberDossierPage() {
  const { id, memberId } = useParams<{ id: string; memberId: string }>();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const [tab, setTab] = useState<(typeof TABS)[number]['key']>('profil');

  const { data: dossier, isLoading } = useQuery({
    queryKey: ['department', id, 'dossier', memberId],
    queryFn: async () => (await api.get(`/departments/${id}/members/${memberId}/dossier`)).data as Dossier,
    enabled: !!id && !!memberId,
  });

  const removeMutation = useMutation({
    mutationFn: async () => api.delete(`/departments/${id}/members/${memberId}`),
    onSuccess: () => {
      toast.success('Membre retiré du département');
      queryClient.invalidateQueries({ queryKey: ['department', id, 'members'] });
      navigate(`/departments/${id}`);
    },
    onError: (err) => toast.error(getErrorMessage(err)),
  });

  if (isLoading) {
    return (
      <div className="page-container flex items-center justify-center min-h-[60vh]">
        <Loader2 className="w-8 h-8 animate-spin text-primary-500" />
      </div>
    );
  }

  if (!dossier) {
    return (
      <div className="page-container">
        <div className="glass-card p-10 text-center">
          <h2 className="text-lg font-semibold text-gray-900 dark:text-gray-100">Dossier introuvable</h2>
          <Link to={`/departments/${id}`} className="btn-primary btn-sm mt-4 inline-flex">Retour au département</Link>
        </div>
      </div>
    );
  }

  const p = dossier.profil || {};
  const initiales = `${p.prenom?.[0] || ''}${p.nom?.[0] || ''}`;

  return (
    <div className="page-container">
      <div className="page-header">
        <button onClick={() => navigate(`/departments/${id}`)} className="btn-ghost btn-sm mb-2">
          <ArrowLeft className="w-4 h-4" /> Retour au département
        </button>
        <div className="flex flex-wrap items-center gap-4">
          <div className="w-14 h-14 rounded-2xl bg-gradient-to-br from-amber-500 to-orange-500 flex items-center justify-center text-white text-xl font-bold shadow-lg">
            {initiales || '?'}
          </div>
          <div className="flex-1 min-w-0">
            <h1 className="page-title truncate">{p.nomComplet || 'Membre'}</h1>
            <div className="flex flex-wrap items-center gap-2 mt-1">
              <span className={`badge text-[10px] ${STATUT_BADGE[p.statut] || 'badge-gray'}`}>
                {STATUT_LABELS[p.statut] || p.statut || '—'}
              </span>
              {p.origine && (
                <span className="badge text-[10px] badge-info">{ORIGINE_LABELS[p.origine] || p.origine}</span>
              )}
              {p.ajoutePar && <span className="text-[10px] text-gray-400">Ajouté par {p.ajoutePar}</span>}
            </div>
          </div>
          <div className="flex flex-wrap gap-2">
            <Link to={`/souls/${p.id}`} className="btn-ghost btn-sm">
              <ExternalLink className="w-4 h-4" /> Fiche âme
            </Link>
            <Link to="/transfers/new" className="btn-primary btn-sm">
              <ArrowLeftRight className="w-4 h-4" /> Demander un transfert
            </Link>
            {p.membreActif && (
              <button
                onClick={() => {
                  if (confirm(`Retirer « ${p.nomComplet} » de ce département ?`)) removeMutation.mutate();
                }}
                disabled={removeMutation.isPending}
                className="btn-ghost btn-sm text-red-500 hover:bg-red-500/10 cursor-pointer"
              >
                <Trash2 className="w-4 h-4" /> Retirer du département
              </button>
            )}
          </div>
        </div>
      </div>

      {/* Tabs */}
      <div className="flex gap-2 mb-6 overflow-x-auto pb-1">
        {TABS.map((t) => {
          const Icon = t.icon;
          const count = tabCount(dossier, t.key);
          return (
            <button
              key={t.key}
              onClick={() => setTab(t.key)}
              className={`px-4 py-2 rounded-xl text-sm font-medium flex items-center gap-2 transition-all cursor-pointer whitespace-nowrap ${
                tab === t.key
                  ? 'bg-gradient-to-r from-amber-500 to-orange-500 text-white shadow-glow'
                  : 'bg-white/70 dark:bg-gray-800/70 text-gray-600 dark:text-gray-300 hover:bg-gray-100 dark:hover:bg-gray-700'
              }`}
            >
              <Icon className="w-4 h-4" /> {t.label}
              {count !== null && <span className="badge text-[9px] badge-gray">{count}</span>}
            </button>
          );
        })}
      </div>

      {tab === 'profil' && <ProfilTab p={p} alertes={dossier.alertes || []} />}
      {tab === 'appartenance' && <AppartenanceTab items={dossier.appartenance || []} />}
      {tab === 'affectations' && <AffectationsTab items={dossier.affectations || []} />}
      {tab === 'taches' && <TachesTab data={dossier.taches || {}} />}
      {tab === 'presences' && <PresencesTab id={id!} memberId={memberId!} data={dossier.presences || {}} />}
      {tab === 'discipline' && <DisciplineTab data={dossier.discipline || {}} memberId={p.id} />}
      {tab === 'rapports' && <RapportsTab id={id!} memberId={memberId!} data={dossier.rapports || {}} items={dossier.rapportsResponsable || []} />}
      {tab === 'evaluations' && <EvaluationsTab data={dossier.evaluations || {}} />}
      {tab === 'evenements' && <EvenementsTab data={dossier.evenements || {}} />}
      {tab === 'annonces' && <AnnoncesTab items={dossier.annonces || []} />}
      {tab === 'notes' && <NotesTab id={id!} memberId={memberId!} items={dossier.notes || []} />}
      {tab === 'objectifs' && <ObjectifsTab id={id!} memberId={memberId!} items={dossier.objectifs || []} />}
      {tab === 'documents' && <DocumentsTab documents={dossier.documents || []} notesDisciple={dossier.notesDisciple || []} />}
      {tab === 'transferts' && <TransfertsTab items={dossier.transferts || []} />}
      {tab === 'activite' && <ActiviteTab items={dossier.activite || []} />}
    </div>
  );
}

function tabCount(dossier: Dossier, key: string): number | null {
  const data = dossier?.[key];
  if (!data) return null;
  if (Array.isArray(data)) return data.length;
  if (typeof data.total === 'number') return data.total;
  return null;
}

// ============================================================
// PROFIL
// ============================================================
function ProfilTab({ p, alertes }: { p: any; alertes: any[] }) {
  const rows: [string, string | undefined][] = [
    ['Email', p.email], ['Téléphone', p.telephone], ['Adresse', p.adresse],
    ['Date de naissance', p.dateNaissance], ['Profession', p.profession],
    ['Niveau d\'études', p.niveauEtude], ['Situation familiale', p.situationFamiliale],
    ['Type de disciple', p.typeDisciple], ['État spirituel', p.etatSpirituel],
    ['Niveau de croissance', p.niveauCroissance != null ? String(p.niveauCroissance) : undefined],
    ['Date d\'intégration', p.dateIntegration], ['Date de conversion', p.dateConversion],
    ['Dernier contact', p.dateDernierContact], ['Famille de disciples', p.familleNom],
    ['Faiseur', p.faiseurNom], ['Compte utilisateur', p.userNom],
    ['Date d\'affectation au département', p.dateAffectation],
  ];
  return (
    <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
      <div className="glass-card p-5">
        <h3 className="text-sm font-semibold text-gray-900 dark:text-gray-100 mb-4 flex items-center gap-2">
          <UserRound className="w-4 h-4 text-amber-500" /> Identité & coordonnées
        </h3>
        <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
          {rows.map(([label, value]) => (
            <div key={label} className="p-3 rounded-xl bg-gray-50 dark:bg-gray-800/40">
              <p className="text-[10px] text-gray-400 uppercase tracking-wider">{label}</p>
              <p className="text-sm font-medium text-gray-800 dark:text-gray-200 truncate">{value || '—'}</p>
            </div>
          ))}
        </div>
      </div>
      <div className="space-y-4">
        <div className="glass-card p-5">
          <h3 className="text-sm font-semibold text-gray-900 dark:text-gray-100 mb-3 flex items-center gap-2">
            <History className="w-4 h-4 text-amber-500" /> Traçabilité du rattachement
          </h3>
          <div className="space-y-2 text-sm">
            <p className="text-gray-600 dark:text-gray-300"><span className="text-gray-400">Ajouté par :</span> <b>{p.ajoutePar || '—'}</b></p>
            <p className="text-gray-600 dark:text-gray-300"><span className="text-gray-400">Origine :</span> <b>{ORIGINE_LABELS[p.origine] || p.origine || '—'}</b></p>
            <p className="text-gray-600 dark:text-gray-300"><span className="text-gray-400">Date d'affectation :</span> <b>{p.dateAffectation || '—'}</b></p>
            <p className="text-gray-600 dark:text-gray-300"><span className="text-gray-400">Membre actif :</span> <b>{p.membreActif ? 'Oui' : 'Non'}</b></p>
            {p.dateDesaffectation && (
              <p className="text-gray-600 dark:text-gray-300"><span className="text-gray-400">Date de désaffectation :</span> <b>{p.dateDesaffectation}</b></p>
            )}
          </div>
        </div>
        <div className="glass-card p-5">
          <h3 className="text-sm font-semibold text-gray-900 dark:text-gray-100 mb-3 flex items-center gap-2">
            <Bell className="w-4 h-4 text-amber-500" /> Alertes actives
          </h3>
          <AlertsList items={alertes} />
        </div>
      </div>
    </div>
  );
}

function AlertsList({ items }: { items: any[] }) {
  if (!items || items.length === 0) return <p className="text-sm text-gray-400">Aucune alerte active</p>;
  return (
    <div className="space-y-2">
      {items.map((a) => (
        <div key={a.id} className="p-2.5 rounded-xl bg-red-50 dark:bg-red-900/20 border border-red-200/50 dark:border-red-500/20">
          <p className="text-sm font-medium text-gray-800 dark:text-gray-200">{a.titre}</p>
          <p className="text-[10px] text-gray-400">{a.message}</p>
        </div>
      ))}
    </div>
  );
}

// ============================================================
// APPARTENANCE
// ============================================================
function AppartenanceTab({ items }: { items: any[] }) {
  return (
    <div className="glass-card p-5">
      <h3 className="text-sm font-semibold text-gray-900 dark:text-gray-100 mb-4">Départements du membre</h3>
      {items.length === 0 ? (
        <p className="text-sm text-gray-400 text-center py-6">Aucun département</p>
      ) : (
        <div className="space-y-2">
          {items.map((d, i) => (
            <div key={`${d.departmentId}-${i}`} className="flex items-center gap-3 p-3 rounded-xl bg-gray-50 dark:bg-gray-800/40">
              <div className="p-2 rounded-lg bg-amber-500/10 text-amber-600">
                <Building2 className="w-4 h-4" />
              </div>
              <div className="min-w-0 flex-1">
                <p className="text-sm font-semibold text-gray-900 dark:text-gray-100">{d.departmentNom || d.departmentId}</p>
                <p className="text-[10px] text-gray-400">
                  {d.actif ? 'Membre actif' : 'Ancien membre'} · depuis {d.dateAffectation || '—'}
                  {d.dateDesaffectation ? ` · jusqu'au ${d.dateDesaffectation}` : ''}
                </p>
              </div>
              <span className="badge text-[10px] badge-info">{ORIGINE_LABELS[d.origine] || d.origine || '—'}</span>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}

// ============================================================
// AFFECTATIONS
// ============================================================
function AffectationsTab({ items }: { items: any[] }) {
  const active = items.filter((a) => a.actif);
  const history = items.filter((a) => !a.actif);
  const roleBadge: Record<string, string> = { CHEF: 'badge-warning', ADJOINT: 'badge-info', MEMBRE: 'badge-gray' };
  return (
    <div className="glass-card p-5">
      <h3 className="text-sm font-semibold text-gray-900 dark:text-gray-100 mb-4">
        Affectations ({active.length} actives)
      </h3>
      {items.length === 0 ? (
        <p className="text-sm text-gray-400 text-center py-6">Aucune affectation</p>
      ) : (
        <div className="space-y-2">
          {active.map((a) => (
            <div key={a.id} className="flex items-center gap-3 p-3 rounded-xl bg-emerald-50/50 dark:bg-emerald-900/10 border border-emerald-200/40 dark:border-emerald-500/20">
              <Users2 className="w-4 h-4 text-emerald-500" />
              <div className="min-w-0 flex-1">
                <p className="text-sm font-medium text-gray-900 dark:text-gray-100">
                  {a.teamNom || '—'} {a.positionNom ? ` · ${a.positionNom}` : ''}
                </p>
                <p className="text-[10px] text-gray-400">
                  {a.dateDebut ? `depuis ${a.dateDebut}` : ''}{a.dateFin ? ` · fin ${a.dateFin}` : ''}
                </p>
              </div>
              <span className={`badge text-[10px] ${roleBadge[a.role] || 'badge-gray'}`}>{a.role}</span>
            </div>
          ))}
          {history.map((a) => (
            <div key={a.id} className="flex items-center gap-3 p-3 rounded-xl bg-gray-50 dark:bg-gray-800/40 opacity-70">
              <Users2 className="w-4 h-4 text-gray-400" />
              <div className="min-w-0 flex-1">
                <p className="text-sm text-gray-500 dark:text-gray-400 line-through">{a.teamNom || a.positionNom || '—'}</p>
                <p className="text-[10px] text-gray-400">terminé le {a.dateFin || '—'}</p>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}

// ============================================================
// TÂCHES
// ============================================================
function TachesTab({ data }: { data: any }) {
  const kpis = [
    { label: 'Total', value: data.total ?? 0, color: 'text-gray-500' },
    { label: 'Ouvertes', value: data.ouvertes ?? 0, color: 'text-blue-500' },
    { label: 'En retard', value: data.enRetard ?? 0, color: 'text-red-500' },
    { label: 'Terminées', value: data.terminees ?? 0, color: 'text-emerald-500' },
  ];
  return (
    <div className="space-y-4">
      <div className="grid grid-cols-2 sm:grid-cols-4 gap-3">
        {kpis.map((k) => (
          <div key={k.label} className="stat-card p-3 text-center">
            <p className={`stat-value text-xl ${k.color}`}>{k.value}</p>
            <span className="stat-label text-[10px]">{k.label}</span>
          </div>
        ))}
      </div>
      <div className="glass-card p-5">
        {(data.liste || []).length === 0 ? (
          <p className="text-sm text-gray-400 text-center py-6">Aucune tâche assignée</p>
        ) : (
          <div className="space-y-2">
            {(data.liste as any[]).map((t) => (
              <div key={t.id} className={`p-3 rounded-xl bg-gray-50 dark:bg-gray-800/40 ${t.enRetard ? 'border border-red-300/50 dark:border-red-500/30' : ''}`}>
                <div className="flex items-center gap-2 flex-wrap">
                  <span className="text-sm font-medium text-gray-900 dark:text-gray-100">{t.titre}</span>
                  <span className={`badge text-[9px] ${TASK_BADGE[t.statut] || 'badge-gray'}`}>{t.statut.replace('_', ' ')}</span>
                  {t.enRetard && <span className="badge text-[9px] badge-danger">En retard</span>}
                </div>
                <p className="text-[10px] text-gray-400 mt-0.5">
                  {t.teamNom ? `${t.teamNom} · ` : ''}échéance {t.echeance || '—'} · avancement {t.avancement}%
                </p>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
}

// ============================================================
// PRÉSENCES — fiches hebdomadaires + événements du département
// ============================================================
function PresencesTab({ id, memberId, data }: { id: string; memberId: string; data: any }) {
  const queryClient = useQueryClient();
  const { data: eventAttendance, isLoading: loadingEvents } = useQuery({
    queryKey: ['department', id, 'dossier', memberId, 'event-attendance'],
    queryFn: async () => (await api.get(`/departments/${id}/members/${memberId}/event-attendance`)).data as any,
    enabled: !!id && !!memberId,
  });

  const markMutation = useMutation({
    mutationFn: async ({ eventId, present }: { eventId: string; present: boolean }) =>
      (await api.put(`/departments/${id}/events/${eventId}/attendance`, { soulId: memberId, present })).data,
    onSuccess: () => {
      toast.success('Présence enregistrée ✅');
      queryClient.invalidateQueries({ queryKey: ['department', id, 'dossier', memberId, 'event-attendance'] });
      queryClient.invalidateQueries({ queryKey: ['department', id, 'dossier', memberId] });
    },
    onError: (err) => toast.error(getErrorMessage(err)),
  });

  const markAllMutation = useMutation({
    mutationFn: async () =>
      (await api.post(`/departments/${id}/members/${memberId}/event-attendance/mark-all?present=true`)).data,
    onSuccess: (r) => {
      toast.success(`${r?.marques ?? 0} événements marqués présents ✅`);
      queryClient.invalidateQueries({ queryKey: ['department', id, 'dossier', memberId, 'event-attendance'] });
    },
    onError: (err) => toast.error(getErrorMessage(err)),
  });

  const downloadCsv = () => {
    api.get(`/departments/${id}/members/${memberId}/event-attendance/export`, { responseType: 'blob' })
      .then((res) => {
        const url = URL.createObjectURL(res.data as Blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = 'presence-membre-evenements.csv';
        a.click();
        URL.revokeObjectURL(url);
        toast.success('Présences exportées 📥');
      })
      .catch((err) => toast.error(getErrorMessage(err)));
  };

  const events: any[] = eventAttendance?.events ?? [];

  return (
    <div className="space-y-4">
      <div className="grid grid-cols-2 sm:grid-cols-4 gap-3">
        {[
          { label: 'Taux de présence', value: `${data.tauxPresence ?? 0}%`, color: 'text-emerald-500' },
          { label: 'Présences', value: data.presents ?? 0, color: 'text-blue-500' },
          { label: 'Absences', value: data.absents ?? 0, color: 'text-red-500' },
          { label: 'Fiches', value: data.total ?? 0, color: 'text-gray-500' },
        ].map((k) => (
          <div key={k.label} className="stat-card p-3 text-center">
            <p className={`stat-value text-xl ${k.color}`}>{k.value}</p>
            <span className="stat-label text-[10px]">{k.label}</span>
          </div>
        ))}
      </div>
      <div className="glass-card p-5">
        {(data.liste || []).length === 0 ? (
          <p className="text-sm text-gray-400 text-center py-6">Aucune fiche de présence</p>
        ) : (
          <div className="space-y-1.5">
            {(data.liste as any[]).map((r) => (
              <div key={r.id} className="flex items-center gap-3 p-2.5 rounded-xl bg-gray-50 dark:bg-gray-800/40">
                <span className={`w-8 h-8 rounded-xl flex items-center justify-center ${r.present ? 'bg-emerald-500 text-white' : 'bg-red-500 text-white'}`}>
                  {r.present ? '✓' : '✗'}
                </span>
                <div className="min-w-0 flex-1">
                  <p className="text-sm font-medium text-gray-800 dark:text-gray-200">Semaine du {r.semaine}</p>
                  <p className="text-[10px] text-gray-400">
                    {r.typeProgramme || 'Général'}{r.sousProgramme ? ` · ${r.sousProgramme}` : ''}{r.notes ? ` · ${r.notes}` : ''}
                  </p>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>

      {/* Présence aux événements du département */}
      <div className="glass-card p-5">
        <div className="flex items-center gap-2 mb-1">
          <CalendarDays className="w-4 h-4 text-primary-500" />
          <h3 className="text-sm font-semibold text-gray-900 dark:text-gray-100">Présence aux événements du département</h3>
        </div>
        <p className="text-[11px] text-gray-400 mb-4">
          Marquez ce membre présent ou absent à chaque événement rattaché au département.
        </p>
        {loadingEvents ? (
          <div className="flex justify-center py-6"><Loader2 className="w-5 h-5 animate-spin text-primary-500" /></div>
        ) : events.length === 0 ? (
          <p className="text-sm text-gray-400 text-center py-6">Aucun événement rattaché à ce département</p>
        ) : (
          <>
            <div className="flex flex-wrap gap-3 mb-3 text-xs text-gray-500">
              <span>Événements : <strong className="text-gray-800 dark:text-gray-100">{eventAttendance?.total ?? 0}</strong></span>
              <span>Présents : <strong className="text-green-600">{eventAttendance?.presents ?? 0}</strong></span>
              <span>Absents : <strong className="text-red-500">{eventAttendance?.absents ?? 0}</strong></span>
              <span>Non pointés : <strong className="text-amber-600">{eventAttendance?.nonMarques ?? 0}</strong></span>
            </div>
            <div className="space-y-1.5">
              {events.map((e: any) => {
                const isPresent = e.present === true;
                const isAbsent = e.present === false;
                return (
                  <div key={e.eventId} className="flex items-center justify-between gap-3 p-2.5 rounded-xl bg-gray-50 dark:bg-gray-800/40">
                    <div className="min-w-0 flex-1">
                      <p className="text-sm font-medium text-gray-900 dark:text-gray-100 truncate">{e.titre}</p>
                      <p className="text-[10px] text-gray-400">
                        {e.dateDebut ? new Date(e.dateDebut).toLocaleDateString('fr-FR', { day: 'numeric', month: 'short', year: 'numeric' }) : '—'}
                        {e.statut ? ` · ${e.statut.replace('_', ' ').toLowerCase()}` : ''}
                      </p>
                    </div>
                    <span className={`text-[10px] font-medium shrink-0 ${isPresent ? 'text-emerald-600' : isAbsent ? 'text-red-500' : 'text-gray-400'}`}>
                      {isPresent ? '✓ Présent' : isAbsent ? '✗ Absent' : 'Non pointé'}
                    </span>
                    <div className="flex gap-1 shrink-0">
                      <button
                        onClick={() => markMutation.mutate({ eventId: e.eventId, present: true })}
                        disabled={markMutation.isPending || isPresent}
                        className={`p-2 rounded-lg transition-colors cursor-pointer ${isPresent ? 'bg-emerald-100 text-emerald-700' : 'text-gray-400 hover:bg-emerald-50 hover:text-emerald-600'}`}
                        title="Marquer présent"
                      >
                        <UserCheck className="w-4 h-4" />
                      </button>
                      <button
                        onClick={() => markMutation.mutate({ eventId: e.eventId, present: false })}
                        disabled={markMutation.isPending || isAbsent}
                        className={`p-2 rounded-lg transition-colors cursor-pointer ${isAbsent ? 'bg-red-100 text-red-700' : 'text-gray-400 hover:bg-red-50 hover:text-red-600'}`}
                        title="Marquer absent"
                      >
                        <UserX className="w-4 h-4" />
                      </button>
                    </div>
                  </div>
                );
              })}
            </div>

            <div className="flex flex-wrap items-center justify-between gap-2 mt-4 pt-3 border-t border-gray-100 dark:border-gray-700/60">
              <button
                onClick={() => markAllMutation.mutate()}
                disabled={markAllMutation.isPending || events.length === 0}
                className="btn-secondary btn-sm cursor-pointer"
                title="Marquer ce membre présent à tous les événements du département"
              >
                {markAllMutation.isPending ? <Loader2 className="w-4 h-4 animate-spin" /> : <UserCheck className="w-4 h-4" />}
                Marquer tous présents
              </button>
              <button
                onClick={downloadCsv}
                disabled={events.length === 0}
                className="btn-ghost btn-sm cursor-pointer"
                title="Exporter la présence de ce membre en CSV"
              >
                <Download className="w-4 h-4" /> Exporter CSV
              </button>
            </div>
          </>
        )}
      </div>
    </div>
  );
}

// ============================================================
// DISCIPLINE
// ============================================================
function DisciplineTab({ data, memberId }: { data: any; memberId: string }) {
  return (
    <div className="glass-card p-5">
      <div className="flex items-center justify-between mb-4 flex-wrap gap-2">
        <h3 className="text-sm font-semibold text-gray-900 dark:text-gray-100">
          Dossier disciplinaire ({data.total ?? 0})
          {data.nonResolus > 0 && <span className="badge text-[9px] badge-danger ml-2">{data.nonResolus} non résolus</span>}
        </h3>
        <Link to={`/souls/${memberId}`} className="btn-ghost btn-xs">
          <Gavel className="w-3 h-3" /> Gérer sur la fiche
        </Link>
      </div>
      {(data.liste || []).length === 0 ? (
        <p className="text-sm text-gray-400 text-center py-6">Aucun événement disciplinaire</p>
      ) : (
        <div className="space-y-2">
          {(data.liste as any[]).map((e) => (
            <div key={e.id} className="p-3 rounded-xl bg-gray-50 dark:bg-gray-800/40">
              <div className="flex items-center gap-2 flex-wrap">
                <span className="text-sm font-medium text-gray-900 dark:text-gray-100">{e.titre}</span>
                <span className="badge text-[9px] badge-gray">{e.categorie?.replace('_', ' ')}</span>
                {e.gravite && <span className="badge text-[9px] badge-warning">{e.gravite}</span>}
                {!e.resolu && <span className="badge text-[9px] badge-danger">Non résolu</span>}
              </div>
              {e.description && <p className="text-[10px] text-gray-400 mt-1">{e.description}</p>}
              <p className="text-[10px] text-gray-400 mt-0.5">{e.dateEvenement || ''}</p>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}

// ============================================================
// RAPPORTS
// ============================================================
const REPORT_TYPE_LABELS: Record<string, string> = {
  PROGRESSION: 'Progression', DIFFICULTE: 'Difficulté', COMPORTEMENT: 'Comportement', GENERAL: 'Général',
};

function RapportsTab({ id, memberId, data, items }: { id: string; memberId: string; data: any; items: any[] }) {
  const queryClient = useQueryClient();
  const [type, setType] = useState('PROGRESSION');
  const [contenu, setContenu] = useState('');

  const invalidate = () => queryClient.invalidateQueries({ queryKey: ['department', id, 'dossier', memberId] });

  const addMutation = useMutation({
    mutationFn: async () => (await api.post(`/departments/${id}/members/${memberId}/reports`, {
      type, contenu: contenu.trim(),
    })).data,
    onSuccess: () => {
      toast.success('Rapport ajouté ✅');
      setContenu('');
      invalidate();
    },
    onError: (err) => toast.error(getErrorMessage(err)),
  });

  const deleteMutation = useMutation({
    mutationFn: async (reportId: string) => api.delete(`/departments/${id}/reports/${reportId}`),
    onSuccess: () => {
      toast.success('Rapport supprimé');
      invalidate();
    },
    onError: (err) => toast.error(getErrorMessage(err)),
  });

  return (
    <div className="grid grid-cols-1 lg:grid-cols-2 gap-4">
      <div className="glass-card p-5">
        <h3 className="text-sm font-semibold text-gray-900 dark:text-gray-100 mb-4">
          Rapports du faiseur ({data.soumis ?? 0} soumis / {data.total ?? 0})
        </h3>
        {(data.liste || []).length === 0 ? (
          <p className="text-sm text-gray-400 text-center py-6">Aucun rapport</p>
        ) : (
          <div className="space-y-2">
            {(data.liste as any[]).map((r) => (
              <div key={r.id} className="flex items-center gap-3 p-3 rounded-xl bg-gray-50 dark:bg-gray-800/40">
                <FileText className="w-4 h-4 text-emerald-500" />
                <div className="min-w-0 flex-1">
                  <p className="text-sm font-medium text-gray-900 dark:text-gray-100">Semaine du {r.semaine}</p>
                  {r.difficultes && <p className="text-[10px] text-gray-400 truncate">{r.difficultes}</p>}
                </div>
                <span className={`badge text-[10px] ${r.soumis ? 'badge-success' : 'badge-gray'}`}>{r.soumis ? 'Soumis' : 'Brouillon'}</span>
              </div>
            ))}
          </div>
        )}
      </div>
      <div className="glass-card p-5">
        <h3 className="text-sm font-semibold text-gray-900 dark:text-gray-100 mb-4 flex items-center gap-2">
          <ClipboardCheck className="w-4 h-4 text-amber-500" /> Rapports du responsable ({items.length})
        </h3>
        <div className="p-4 rounded-2xl bg-gray-50 dark:bg-gray-800/40 border border-gray-200/60 dark:border-gray-700/40 mb-4">
          <div className="flex flex-col sm:flex-row gap-2 mb-2">
            <select className="input sm:w-44" value={type} onChange={(e) => setType(e.target.value)}>
              {Object.entries(REPORT_TYPE_LABELS).map(([k, v]) => (
                <option key={k} value={k}>{v}</option>
              ))}
            </select>
            <input className="input flex-1" value={contenu} onChange={(e) => setContenu(e.target.value)} placeholder="Point de situation sur ce membre…" />
          </div>
          <button
            onClick={() => addMutation.mutate()}
            disabled={!contenu.trim() || addMutation.isPending}
            className="btn-primary btn-sm cursor-pointer"
          >
            {addMutation.isPending ? <Loader2 className="w-3 h-3 animate-spin" /> : <Plus className="w-3 h-3" />} Ajouter le rapport
          </button>
        </div>
        {items.length === 0 ? (
          <p className="text-sm text-gray-400 text-center py-6">Aucun rapport rédigé par un responsable</p>
        ) : (
          <div className="space-y-2">
            {items.map((r) => (
              <div key={r.id} className="p-3 rounded-xl bg-gray-50 dark:bg-gray-800/40 border-l-[3px] border-l-amber-500">
                <div className="flex items-center gap-2 flex-wrap">
                  <span className={`badge text-[9px] badge-info`}>{REPORT_TYPE_LABELS[r.type] || r.type || 'Général'}</span>
                  <button
                    onClick={() => { if (confirm('Supprimer ce rapport ?')) deleteMutation.mutate(r.id); }}
                    className="p-1 ml-auto text-gray-400 hover:text-red-500 transition-all cursor-pointer"
                    title="Supprimer"
                  >
                    <Trash2 className="w-3 h-3" />
                  </button>
                </div>
                <p className="text-sm text-gray-800 dark:text-gray-200 mt-1">{r.contenu}</p>
                <p className="text-[10px] text-gray-400 mt-1">{r.auteurNom || '—'} · {new Date(r.createdAt).toLocaleString('fr-FR')}</p>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
}

// ============================================================
// ÉVALUATIONS
// ============================================================
function EvaluationsTab({ data }: { data: any }) {
  const avg = (data.liste || []).length
    ? ((data.liste as any[]).reduce((s: number, e: any) => s + e.note, 0) / (data.liste as any[]).length).toFixed(1)
    : null;
  return (
    <div className="glass-card p-5">
      <h3 className="text-sm font-semibold text-gray-900 dark:text-gray-100 mb-4">
        Évaluations ({data.total ?? 0}){avg !== null && <span className="badge text-[10px] badge-info ml-2">Moyenne : {avg}/5</span>}
      </h3>
      {(data.liste || []).length === 0 ? (
        <p className="text-sm text-gray-400 text-center py-6">Aucune évaluation</p>
      ) : (
        <div className="space-y-2">
          {(data.liste as any[]).map((e) => (
            <div key={e.id} className="flex items-center gap-3 p-3 rounded-xl bg-gray-50 dark:bg-gray-800/40">
              <div className="flex items-center gap-0.5">
                {[1, 2, 3, 4, 5].map((n) => (
                  <Star key={n} className={`w-3.5 h-3.5 ${n <= e.note ? 'fill-amber-400 text-amber-400' : 'text-gray-300'}`} />
                ))}
              </div>
              <div className="min-w-0 flex-1">
                <p className="text-sm font-medium text-gray-900 dark:text-gray-100 capitalize">{e.categorie?.replace('_', ' ')}</p>
                {e.commentaire && <p className="text-[10px] text-gray-400 truncate">{e.commentaire}</p>}
              </div>
              <span className="text-[10px] text-gray-400">{e.date}</span>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}

// ============================================================
// ÉVÉNEMENTS
// ============================================================
function EvenementsTab({ data }: { data: any }) {
  return (
    <div className="glass-card p-5">
      <h3 className="text-sm font-semibold text-gray-900 dark:text-gray-100 mb-4">Événements ({data.total ?? 0})</h3>
      {(data.liste || []).length === 0 ? (
        <p className="text-sm text-gray-400 text-center py-6">Aucune inscription à un événement</p>
      ) : (
        <div className="space-y-2">
          {(data.liste as any[]).map((e) => (
            <div key={e.id} className="flex items-center gap-3 p-3 rounded-xl bg-gray-50 dark:bg-gray-800/40">
              <Calendar className="w-4 h-4 text-primary-500" />
              <div className="min-w-0 flex-1">
                <p className="text-sm font-medium text-gray-900 dark:text-gray-100">{e.titre || 'Événement'}</p>
                <p className="text-[10px] text-gray-400">{e.dateDebut || ''}{e.lieu ? ` · ${e.lieu}` : ''}</p>
              </div>
              <span className="badge text-[10px] badge-info">{e.statut || e.statutEvenement || 'INSCRIT'}</span>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}

// ============================================================
// ANNONCES
// ============================================================
function AnnoncesTab({ items }: { items: any[] }) {
  return (
    <div className="glass-card p-5">
      <h3 className="text-sm font-semibold text-gray-900 dark:text-gray-100 mb-4 flex items-center gap-2">
        <Megaphone className="w-4 h-4 text-amber-500" /> Annonces du département pour ce membre
      </h3>
      {items.length === 0 ? (
        <p className="text-sm text-gray-400 text-center py-6">Aucune annonce ciblée</p>
      ) : (
        <div className="space-y-2">
          {items.map((a) => (
            <div key={a.id} className="p-3 rounded-xl bg-gray-50 dark:bg-gray-800/40 border-l-[3px] border-l-amber-500">
              <p className="text-sm font-semibold text-gray-900 dark:text-gray-100">{a.titre}</p>
              <p className="text-xs text-gray-600 dark:text-gray-300 mt-0.5">{a.message}</p>
              <p className="text-[10px] text-gray-400 mt-1">
                {a.auteurNom || ''} · {new Date(a.createdAt).toLocaleDateString('fr-FR')}
                {a.teamNom ? ` · ${a.teamNom}` : ''}{a.positionNom ? ` · ${a.positionNom}` : ''}
              </p>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}

// ============================================================
// NOTES
// ============================================================
function NotesTab({ id, memberId, items }: { id: string; memberId: string; items: any[] }) {
  const queryClient = useQueryClient();
  const [contenu, setContenu] = useState('');
  const addMutation = useMutation({
    mutationFn: async () => (await api.post(`/departments/${id}/members/${memberId}/notes`, { contenu })).data,
    onSuccess: () => {
      toast.success('Note ajoutée ✅');
      setContenu('');
      queryClient.invalidateQueries({ queryKey: ['department', id, 'dossier', memberId] });
    },
    onError: (err) => toast.error(getErrorMessage(err)),
  });
  const deleteMutation = useMutation({
    mutationFn: async (noteId: string) => api.delete(`/departments/${id}/notes/${noteId}`),
    onSuccess: () => {
      toast.success('Note supprimée');
      queryClient.invalidateQueries({ queryKey: ['department', id, 'dossier', memberId] });
    },
    onError: (err) => toast.error(getErrorMessage(err)),
  });

  return (
    <div className="glass-card p-5">
      <h3 className="text-sm font-semibold text-gray-900 dark:text-gray-100 mb-4 flex items-center gap-2">
        <StickyNote className="w-4 h-4 text-amber-500" /> Notes du dossier ({items.length})
      </h3>
      <div className="p-4 rounded-2xl bg-gray-50 dark:bg-gray-800/40 border border-gray-200/60 dark:border-gray-700/40 mb-4">
        <textarea
          className="input mb-2" rows={2} value={contenu}
          onChange={(e) => setContenu(e.target.value)}
          placeholder="Note interne sur ce membre (suivi, comportement, besoins…)"
        />
        <button
          onClick={() => addMutation.mutate()}
          disabled={!contenu.trim() || addMutation.isPending}
          className="btn-primary btn-sm cursor-pointer"
        >
          {addMutation.isPending ? <Loader2 className="w-3 h-3 animate-spin" /> : <Plus className="w-3 h-3" />} Ajouter
        </button>
      </div>
      {items.length === 0 ? (
        <p className="text-sm text-gray-400 text-center py-6">Aucune note pour ce membre</p>
      ) : (
        <div className="space-y-2">
          {items.map((n) => (
            <div key={n.id} className="p-3 rounded-xl bg-gray-50 dark:bg-gray-800/40">
              <p className="text-sm text-gray-800 dark:text-gray-200">{n.contenu}</p>
              <div className="flex items-center justify-between mt-1">
                <p className="text-[10px] text-gray-400">{n.auteurNom || '—'} · {new Date(n.createdAt).toLocaleString('fr-FR')}</p>
                <button onClick={() => deleteMutation.mutate(n.id)} className="p-1 text-gray-400 hover:text-red-500 cursor-pointer">
                  <Trash2 className="w-3 h-3" />
                </button>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}

// ============================================================
// OBJECTIFS DE PROGRESSION
// ============================================================
const OBJECTIVE_BADGE: Record<string, string> = {
  A_FAIRE: 'badge-gray', EN_COURS: 'badge-info', ATTEINT: 'badge-success', ANNULE: 'badge-inactive',
};

function ObjectifsTab({ id, memberId, items }: { id: string; memberId: string; items: any[] }) {
  const queryClient = useQueryClient();
  const [titre, setTitre] = useState('');
  const [description, setDescription] = useState('');
  const [echeance, setEcheance] = useState('');

  const invalidate = () => queryClient.invalidateQueries({ queryKey: ['department', id, 'dossier', memberId] });

  const createMutation = useMutation({
    mutationFn: async () => {
      const res = await api.post(`/departments/${id}/members/${memberId}/objectives`, {
        titre: titre.trim(), description: description.trim() || null, echeance: echeance || null,
      });
      return res.data;
    },
    onSuccess: () => {
      toast.success('Objectif créé ✅');
      setTitre(''); setDescription(''); setEcheance('');
      invalidate();
    },
    onError: (err) => toast.error(getErrorMessage(err)),
  });

  const updateMutation = useMutation({
    mutationFn: async ({ objectiveId, data }: { objectiveId: string; data: any }) =>
      (await api.put(`/departments/${id}/objectives/${objectiveId}`, data)).data,
    onSuccess: () => invalidate(),
    onError: (err) => toast.error(getErrorMessage(err)),
  });

  const deleteMutation = useMutation({
    mutationFn: async (objectiveId: string) => api.delete(`/departments/${id}/objectives/${objectiveId}`),
    onSuccess: () => {
      toast.success('Objectif supprimé');
      invalidate();
    },
    onError: (err) => toast.error(getErrorMessage(err)),
  });

  const setAvancement = (o: any, avancement: number) =>
    updateMutation.mutate({ objectiveId: o.id, data: { titre: o.titre, description: o.description, echeance: o.echeance, avancement } });
  const setStatut = (o: any, statut: string) =>
    updateMutation.mutate({ objectiveId: o.id, data: { titre: o.titre, description: o.description, echeance: o.echeance, statut } });

  const enCours = items.filter((o) => o.statut === 'EN_COURS' || o.statut === 'A_FAIRE');
  const atteints = items.filter((o) => o.statut === 'ATTEINT');
  const moyenne = items.length
    ? Math.round(items.reduce((s: number, o) => s + (o.avancement ?? 0), 0) / items.length)
    : 0;

  return (
    <div className="space-y-4">
      <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
        <div className="stat-card p-3 text-center">
          <Target className="w-4 h-4 mx-auto mb-1 text-blue-500" />
          <p className="stat-value text-xl">{enCours.length}</p>
          <span className="stat-label text-[10px]">En cours</span>
        </div>
        <div className="stat-card p-3 text-center">
          <TrendingUp className="w-4 h-4 mx-auto mb-1 text-emerald-500" />
          <p className="stat-value text-xl">{atteints.length}</p>
          <span className="stat-label text-[10px]">Atteints</span>
        </div>
        <div className="stat-card p-3 text-center">
          <CheckCircle2 className="w-4 h-4 mx-auto mb-1 text-amber-500" />
          <p className="stat-value text-xl">{moyenne}%</p>
          <span className="stat-label text-[10px]">Avancement moyen</span>
        </div>
      </div>

      <div className="glass-card p-5">
        <h3 className="text-sm font-semibold text-gray-900 dark:text-gray-100 mb-4 flex items-center gap-2">
          <Target className="w-4 h-4 text-amber-500" /> Objectifs de progression ({items.length})
        </h3>

        <div className="p-4 rounded-2xl bg-gray-50 dark:bg-gray-800/40 border border-gray-200/60 dark:border-gray-700/40 mb-4">
          <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
            <div className="sm:col-span-1">
              <label className="label">Objectif *</label>
              <input className="input" value={titre} onChange={(e) => setTitre(e.target.value)} placeholder="Ex : Être confirmé, intégrer l'équipe…" />
            </div>
            <div>
              <label className="label">Description</label>
              <input className="input" value={description} onChange={(e) => setDescription(e.target.value)} placeholder="Étapes, critères…" />
            </div>
            <div>
              <label className="label">Échéance</label>
              <input type="date" className="input" value={echeance} onChange={(e) => setEcheance(e.target.value)} />
            </div>
          </div>
          <button
            onClick={() => createMutation.mutate()}
            disabled={!titre.trim() || createMutation.isPending}
            className="btn-primary btn-sm mt-3 cursor-pointer"
          >
            {createMutation.isPending ? <Loader2 className="w-4 h-4 animate-spin" /> : <Plus className="w-4 h-4" />} Créer l'objectif
          </button>
        </div>

        {items.length === 0 ? (
          <p className="text-sm text-gray-400 text-center py-6">Aucun objectif fixé pour ce membre</p>
        ) : (
          <div className="space-y-2">
            {items.map((o) => (
              <div key={o.id} className={`p-3 rounded-xl bg-gray-50 dark:bg-gray-800/40 ${o.enRetard ? 'border border-red-300/50 dark:border-red-500/30' : ''}`}>
                <div className="flex items-center gap-2 flex-wrap">
                  <span className="text-sm font-semibold text-gray-900 dark:text-gray-100">{o.titre}</span>
                  <span className={`badge text-[9px] ${OBJECTIVE_BADGE[o.statut] || 'badge-gray'}`}>{o.statut?.replace('_', ' ')}</span>
                  {o.enRetard && <span className="badge text-[9px] badge-danger">Échéance dépassée</span>}
                </div>
                {o.description && <p className="text-[10px] text-gray-400 mt-0.5">{o.description}</p>}
                <p className="text-[10px] text-gray-400 mt-0.5">
                  {o.echeance ? `Échéance : ${o.echeance} · ` : ''}fixé par {o.creeParNom || '—'}
                </p>
                <div className="flex items-center gap-3 mt-2 flex-wrap">
                  <div className="flex items-center gap-2 min-w-[180px] flex-1 max-w-xs">
                    <input
                      type="range" min={0} max={100} step={5}
                      value={o.avancement}
                      onChange={(e) => setAvancement(o, Number(e.target.value))}
                      className="w-full accent-emerald-500"
                    />
                    <span className="text-[10px] font-bold text-emerald-600 shrink-0">{o.avancement}%</span>
                  </div>
                  <select
                    value={o.statut}
                    onChange={(e) => setStatut(o, e.target.value)}
                    className="input w-36 py-1 text-xs"
                  >
                    {Object.keys(OBJECTIVE_BADGE).map((k) => (
                      <option key={k} value={k}>{k.replace('_', ' ').toLowerCase()}</option>
                    ))}
                  </select>
                  <button
                    onClick={() => { if (confirm(`Supprimer l'objectif « ${o.titre} » ?`)) deleteMutation.mutate(o.id); }}
                    className="p-1.5 rounded-lg text-gray-400 hover:text-red-600 hover:bg-red-500/10 transition-all cursor-pointer"
                    title="Supprimer"
                  >
                    <Trash2 className="w-3.5 h-3.5" />
                  </button>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
}

// ============================================================
// DOCUMENTS & NOTES DE LA FICHE ÂME
// ============================================================
function DocumentsTab({ documents, notesDisciple }: { documents: any[]; notesDisciple: any[] }) {
  return (
    <div className="grid grid-cols-1 lg:grid-cols-2 gap-4">
      <div className="glass-card p-5">
        <h3 className="text-sm font-semibold text-gray-900 dark:text-gray-100 mb-4 flex items-center gap-2">
          <FolderOpen className="w-4 h-4 text-amber-500" /> Documents du dossier ({documents.length})
        </h3>
        {documents.length === 0 ? (
          <p className="text-sm text-gray-400 text-center py-6">Aucun document joint</p>
        ) : (
          <div className="space-y-2">
            {documents.map((d) => (
              <a
                key={d.id}
                href={d.url || `#/files/${d.fileId}`}
                target="_blank"
                rel="noopener noreferrer"
                className="flex items-center gap-3 p-3 rounded-xl bg-gray-50 dark:bg-gray-800/40 hover:bg-gray-100 dark:hover:bg-gray-700 transition-all"
              >
                <FolderOpen className="w-4 h-4 text-primary-500 shrink-0" />
                <span className="min-w-0 flex-1 text-sm font-medium text-gray-900 dark:text-gray-100 truncate">{d.nom || d.fileId}</span>
                <ExternalLink className="w-3.5 h-3.5 text-gray-400 shrink-0" />
              </a>
            ))}
          </div>
        )}
      </div>
      <div className="glass-card p-5">
        <h3 className="text-sm font-semibold text-gray-900 dark:text-gray-100 mb-4 flex items-center gap-2">
          <StickyNote className="w-4 h-4 text-amber-500" /> Notes de la fiche âme ({notesDisciple.length})
        </h3>
        {notesDisciple.length === 0 ? (
          <p className="text-sm text-gray-400 text-center py-6">Aucune note sur la fiche âme</p>
        ) : (
          <div className="space-y-2">
            {notesDisciple.map((n) => (
              <div key={n.id} className="p-3 rounded-xl bg-gray-50 dark:bg-gray-800/40">
                <p className="text-sm text-gray-800 dark:text-gray-200">{n.contenu}</p>
                <p className="text-[10px] text-gray-400 mt-1">{n.auteurNom || '—'} · {new Date(n.createdAt).toLocaleString('fr-FR')}</p>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
}

// ============================================================
// TRANSFERTS
// ============================================================
function TransfertsTab({ items }: { items: any[] }) {
  const badge: Record<string, string> = {
    BROUILLON: 'badge-gray', SOUMIS: 'badge-info', EN_ATTENTE_VALIDATION: 'badge-warning',
    VALIDATION_PARTIELLE: 'badge-warning', VALIDE: 'badge-info', EXECUTE: 'badge-success',
    REFUSE: 'badge-danger', ANNULE: 'badge-inactive', ARCHIVE: 'badge-inactive',
  };
  return (
    <div className="glass-card p-5">
      <h3 className="text-sm font-semibold text-gray-900 dark:text-gray-100 mb-4 flex items-center gap-2">
        <ArrowLeftRight className="w-4 h-4 text-amber-500" /> Historique des mouvements ({items.length})
      </h3>
      {items.length === 0 ? (
        <p className="text-sm text-gray-400 text-center py-6">Aucun transfert enregistré</p>
      ) : (
        <div className="space-y-2">
          {items.map((t) => (
            <div key={t.id} className="p-3 rounded-xl bg-gray-50 dark:bg-gray-800/40">
              <div className="flex items-center gap-2 flex-wrap">
                <span className="text-sm font-medium text-gray-900 dark:text-gray-100">{t.type?.replace(/_/g, ' ')}</span>
                <span className={`badge text-[9px] ${badge[t.statut] || 'badge-gray'}`}>{t.statut?.replace(/_/g, ' ')}</span>
              </div>
              <p className="text-[10px] text-gray-400 mt-0.5">
                {t.ancienneAffectation?.nom ? `de « ${t.ancienneAffectation.nom} » ` : ''}
                {t.nouvelleAffectation?.nom ? `vers « ${t.nouvelleAffectation.nom} »` : ''}
                {t.demandeurNom ? ` · par ${t.demandeurNom}` : ''}
              </p>
              {t.justification && <p className="text-[10px] text-gray-400 mt-0.5 italic">{t.justification}</p>}
              <p className="text-[10px] text-gray-400 mt-0.5">{t.dateSoumission ? `Soumis le ${t.dateSoumission}` : `Créé le ${t.createdAt}`}</p>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}

// ============================================================
// ACTIVITÉ
// ============================================================
const ACTION_LABELS: Record<string, string> = {
  MEMBER_ADDED: 'Ajouté au département', MEMBER_CREATED: 'Membre créé', MEMBER_REMOVED: 'Retiré du département',
  MEMBER_ASSIGNED: 'Affecté', ASSIGNMENT_ENDED: 'Fin d\'affectation', TASK_CREATED: 'Tâche créée',
  TASK_UPDATED: 'Tâche mise à jour', TASK_CANCELLED: 'Tâche annulée', NOTE_ADDED: 'Note ajoutée',
};

function ActiviteTab({ items }: { items: any[] }) {
  return (
    <div className="glass-card p-5">
      <h3 className="text-sm font-semibold text-gray-900 dark:text-gray-100 mb-4 flex items-center gap-2">
        <History className="w-4 h-4 text-amber-500" /> Journal d'activité du département pour ce membre
      </h3>
      {items.length === 0 ? (
        <p className="text-sm text-gray-400 text-center py-6">Aucune activité enregistrée</p>
      ) : (
        <div className="space-y-0">
          {items.map((a, i) => (
            <div key={a.id} className="relative flex gap-3 pb-4">
              {i < items.length - 1 && <span className="absolute left-[9px] top-6 bottom-0 w-px bg-gray-200 dark:bg-gray-700" />}
              <div className="w-[18px] h-[18px] rounded-full bg-gradient-to-br from-amber-500 to-orange-500 shrink-0 mt-0.5" />
              <div className="min-w-0 flex-1">
                <p className="text-sm text-gray-800 dark:text-gray-200">
                  <span className="font-semibold">{ACTION_LABELS[a.action] || a.action.replace(/_/g, ' ')}</span>
                  {a.details && <span className="text-gray-500 dark:text-gray-400"> — {a.details}</span>}
                </p>
                <p className="text-[10px] text-gray-400">{a.actorNom ? `par ${a.actorNom} · ` : ''}{new Date(a.createdAt).toLocaleString('fr-FR')}</p>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
