import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useParams, Link, useNavigate } from 'react-router-dom';
import api, { getErrorMessage } from '@/lib/api';
import toast from 'react-hot-toast';
import {
  ArrowLeft, UserRound, Building2, Users2, ListTodo, ClipboardCheck, Gavel,
  FileText, Star, Calendar, StickyNote, Megaphone, ArrowLeftRight, History,
  Loader2, Plus, Send, Trash2, FolderOpen, Bell, ExternalLink,
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
      {tab === 'presences' && <PresencesTab data={dossier.presences || {}} />}
      {tab === 'discipline' && <DisciplineTab data={dossier.discipline || {}} memberId={p.id} />}
      {tab === 'rapports' && <RapportsTab data={dossier.rapports || {}} />}
      {tab === 'evaluations' && <EvaluationsTab data={dossier.evaluations || {}} />}
      {tab === 'evenements' && <EvenementsTab data={dossier.evenements || {}} />}
      {tab === 'annonces' && <AnnoncesTab items={dossier.annonces || []} />}
      {tab === 'notes' && <NotesTab id={id!} memberId={memberId!} items={dossier.notes || []} />}
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
            <p className="text-gray-600 dark:text-gray-300"><span className="text-gray-400">Date d\'affectation :</span> <b>{p.dateAffectation || '—'}</b></p>
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
// PRÉSENCES
// ============================================================
function PresencesTab({ data }: { data: any }) {
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
function RapportsTab({ data }: { data: any }) {
  return (
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
        <History className="w-4 h-4 text-amber-500" /> Journal d\'activité du département pour ce membre
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
