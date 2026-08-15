import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useAuth } from '@/contexts/AuthContext';
import api, { getErrorMessage } from '@/lib/api';
import { useState, useEffect } from 'react';
import {
  Building2, Users, UserPlus, Calendar, UserCheck, FileText, Activity,
  Star, ChevronRight, Cake, CheckCircle, Clock, UserX, Network, AlertCircle,
  Filter, RefreshCw, Heart, ClipboardCheck, Save, Loader2, UserRound, ListTodo, ArrowLeftRight,
  CalendarDays,
} from 'lucide-react';
import { Link, useNavigate } from 'react-router-dom';
import toast from 'react-hot-toast';
import type { DepartmentPresenceRecord, ProgramType } from '@/types';
import { EventAttendanceModal } from '@/pages/DepartmentManagementPage';

const currentWeekMonday = () => {
  const d = new Date();
  d.setDate(d.getDate() - ((d.getDay() + 6) % 7));
  return d.toISOString().split('T')[0];
};

const getGreeting = () => {
  const h = new Date().getHours();
  if (h < 12) return 'Bonjour';
  if (h < 17) return 'Bon après-midi';
  return 'Bonsoir';
};

const formatDate = (d?: string) => {
  if (!d) return '—';
  return new Date(d + 'T00:00:00').toLocaleDateString('fr-FR', { day: 'numeric', month: 'short' });
};

export default function ResponsableDashboardPage() {
  const { user } = useAuth();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const [selectedDeptId, setSelectedDeptId] = useState<string | undefined>(undefined);
  const [presenceSemaine, setPresenceSemaine] = useState(currentWeekMonday());
  const [presenceType, setPresenceType] = useState('');
  const [presenceSousType, setPresenceSousType] = useState('');
  const [attendanceEvent, setAttendanceEvent] = useState<any | null>(null);

  const { data: dashboard, isLoading, refetch } = useQuery({
    queryKey: ['dashboard', 'responsable', selectedDeptId],
    queryFn: async () => {
      const res = await api.get('/dashboard/responsable', {
        params: selectedDeptId ? { deptId: selectedDeptId } : undefined,
      });
      return res.data as any;
    },
  });

  const activeDeptId = selectedDeptId ?? dashboard?.selectedDeptId;

  // ==================== Événements du département (pointage des présences) ====================
  const { data: deptEvents = [] } = useQuery({
    queryKey: ['dashboard', 'responsable', activeDeptId, 'events'],
    queryFn: async () => (await api.get(`/events/department/${activeDeptId}?size=50`)).data?.content ?? [],
    enabled: !!activeDeptId,
  });

  // ==================== Saisie des présences (responsable) ====================
  const { data: programTypes = [] } = useQuery({
    queryKey: ['programs', 'active'],
    queryFn: async () => (await api.get('/programs/active')).data as ProgramType[],
  });

  const { data: presenceSheet = [], isLoading: sheetLoading, refetch: refetchSheet } = useQuery({
    queryKey: ['members', 'departments', activeDeptId, 'presences', presenceSemaine],
    queryFn: async () => {
      const res = await api.get(`/members/departments/${activeDeptId}/presences`, {
        params: { semaine: presenceSemaine },
      });
      return res.data as DepartmentPresenceRecord[];
    },
    enabled: !!activeDeptId,
  });

  const [presenceForm, setPresenceForm] = useState<Record<string, boolean>>({});
  const [presenceNotes, setPresenceNotes] = useState<Record<string, string>>({});

  const presenceMutation = useMutation({
    mutationFn: async ({ deptId, payload }: { deptId: string; payload: any }) => {
      const res = await api.post(`/members/departments/${deptId}/presences`, payload);
      return res.data;
    },
    onSuccess: () => {
      toast.success('Présences enregistrées ✅');
      queryClient.invalidateQueries({ queryKey: ['members', 'departments', activeDeptId, 'presences'] });
      queryClient.invalidateQueries({ queryKey: ['dashboard', 'responsable'] });
    },
    onError: (err) => toast.error(getErrorMessage(err)),
  });

  const submitPresences = () => {
    if (!activeDeptId) return;
    const items = presenceSheet
      .filter((m) => presenceForm[m.soulId] !== undefined)
      .map((m) => ({
        soulId: m.soulId,
        present: presenceForm[m.soulId] ?? false,
        notes: presenceNotes[m.soulId] || undefined,
      }));
    if (items.length === 0) {
      toast.error('Cochez au moins un membre avant d\'enregistrer');
      return;
    }
    presenceMutation.mutate({
      deptId: activeDeptId,
      payload: {
        semaine: presenceSemaine,
        typeProgramme: presenceType || undefined,
        sousProgramme: presenceSousType || undefined,
        presences: items,
      },
    });
  };

  /** Fait défiler la page jusqu'à la fiche de saisie des présences. */
  const scrollToPresences = () => {
    document.getElementById('saisie-presences')?.scrollIntoView({ behavior: 'smooth', block: 'start' });
  };

  // Initialiser le formulaire à partir de la fiche chargée (une fois par semaine/département)
  useEffect(() => {
    setPresenceForm({});
    setPresenceNotes({});
    (presenceSheet ?? []).forEach((m) => {
      setPresenceForm((f) => ({ ...f, [m.soulId]: m.present ?? false }));
      if (m.notes) setPresenceNotes((n) => ({ ...n, [m.soulId]: m.notes as string }));
    });
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [activeDeptId, presenceSemaine, sheetLoading]);

  const selectedProgram = programTypes.find((p) => p.code === presenceType);
  const sousTypes = selectedProgram?.aSousProgrammes ? selectedProgram.sousProgrammes || [] : [];

  const stats = dashboard?.statistiques ?? {};
  const departements = dashboard?.departements ?? [];
  const deptDetail = dashboard?.departement ?? {};
  const anniversaires = deptDetail?.anniversaires ?? [];
  const nouveauxRecents = deptDetail?.nouveauxRecents ?? [];
  const membresSuivi = deptDetail?.membresSuivi ?? [];
  const evenementsAvenir = deptDetail?.evenementsAvenir ?? [];
  const alertes = deptDetail?.alertes ?? [];

  return (
    <div className="page-container">
      {/* Header */}
      <div className="page-header">
        <div className="animate-fade-in">
          <div className="flex items-center gap-2 mb-1">
            <Building2 className="w-5 h-5 text-amber-500" />
            <span className="text-sm font-medium text-amber-600 dark:text-amber-400 uppercase tracking-wider">
              {getGreeting()}, {user?.firstName}
            </span>
          </div>
          <h1 className="page-title">
            Espace{' '}
            <span className="text-gradient font-display">Responsable</span>
          </h1>
          <p className="page-subtitle">
            Gestion des membres de votre département · {new Date().toLocaleDateString('fr-FR', {
              weekday: 'long', day: 'numeric', month: 'long', year: 'numeric'
            })}
          </p>
        </div>

        {/* Department selector (multi-departments) */}
        {departements?.length > 1 && (
          <div className="flex items-center gap-2 mt-3 animate-fade-in">
            <Filter className="w-4 h-4 text-gray-400" />
            <span className="text-xs font-medium text-gray-500 dark:text-gray-400">Département :</span>
            <select
              value={selectedDeptId ?? dashboard?.selectedDeptId ?? ''}
              onChange={(e) => setSelectedDeptId(e.target.value || undefined)}
              className="px-3 py-1.5 rounded-xl text-sm font-medium bg-white/70 dark:bg-gray-800/70 border border-gray-200 dark:border-gray-700 text-gray-800 dark:text-gray-200 focus:outline-none focus:ring-2 focus:ring-primary-500/40"
            >
              {departements.map((d: any) => (
                <option key={d.id} value={d.id}>{d.nom}</option>
              ))}
            </select>
            <button
              onClick={() => refetch()}
              className="p-2 rounded-xl hover:bg-gray-100 dark:hover:bg-gray-800 transition-colors"
              title="Rafraîchir"
            >
              <RefreshCw className="w-4 h-4 text-gray-400" />
            </button>
          </div>
        )}
      </div>

      {dashboard?.message ? (
        <div className="glass-card p-12 text-center animate-fade-in">
          <Building2 className="w-12 h-12 text-gray-300 mx-auto mb-4" />
          <h2 className="text-lg font-semibold text-gray-600 dark:text-gray-400 mb-2">{dashboard.message}</h2>
          <p className="text-sm text-gray-400">Contactez le pasteur pour être affecté à un département.</p>
        </div>
      ) : isLoading ? (
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4 mb-8">
          {[...Array(6)].map((_, i) => (
            <div key={i} className="glass-card p-5 animate-fade-in" style={{ animationDelay: `${i * 50}ms` }}>
              <div className="skeleton h-4 w-24 mb-3 rounded" />
              <div className="skeleton h-8 w-20 rounded" />
            </div>
          ))}
        </div>
      ) : (
        <>
          {/* Active department banner */}
          <div className="glass-card p-4 mb-6 animate-slide-up flex items-center justify-between">
            <div className="flex items-center gap-3">
              <div className="p-2.5 rounded-xl bg-gradient-to-br from-amber-500 to-orange-500 text-white shadow-lg">
                <Building2 className="w-5 h-5" />
              </div>
              <div>
                <p className="text-xs text-gray-400 uppercase tracking-wider">Département actif</p>
                <h2 className="text-lg font-bold text-gray-900 dark:text-gray-100">{dashboard?.selectedDeptNom}</h2>
              </div>
            </div>
            <Link to={`/departments/${activeDeptId}`} className="btn-ghost btn-xs">
              Voir le détail <ChevronRight className="w-3 h-3 ml-1" />
            </Link>
          </div>

          {/* Member Stats Cards */}
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4 mb-6">
            <div className="stat-card animate-slide-up">
              <div className="absolute top-0 left-0 right-0 h-1 bg-gradient-to-r from-amber-500 to-orange-500 opacity-60" />
              <div className="flex items-start justify-between mb-3">
                <span className="stat-label">Membres</span>
                <div className="p-2 rounded-xl bg-gradient-to-br from-amber-500 to-orange-500 text-white shadow-lg">
                  <Users className="w-4 h-4" />
                </div>
              </div>
              <span className="stat-value">{stats.totalMembres ?? 0}</span>
              <span className="text-[10px] text-gray-400 mt-1 block">dans {dashboard?.selectedDeptNom}</span>
            </div>
            <div className="stat-card animate-slide-up" style={{ animationDelay: '60ms' }}>
              <div className="absolute top-0 left-0 right-0 h-1 bg-gradient-to-r from-emerald-500 to-green-500 opacity-60" />
              <div className="flex items-start justify-between mb-3">
                <span className="stat-label">Membres actifs</span>
                <div className="p-2 rounded-xl bg-gradient-to-br from-emerald-500 to-green-500 text-white shadow-lg">
                  <CheckCircle className="w-4 h-4" />
                </div>
              </div>
              <span className="stat-value text-emerald-500">{stats.totalActifs ?? 0}</span>
            </div>
            <div className="stat-card animate-slide-up" style={{ animationDelay: '120ms' }}>
              <div className="absolute top-0 left-0 right-0 h-1 bg-gradient-to-r from-blue-500 to-indigo-500 opacity-60" />
              <div className="flex items-start justify-between mb-3">
                <span className="stat-label">Nouveaux membres</span>
                <div className="p-2 rounded-xl bg-gradient-to-br from-blue-500 to-indigo-500 text-white shadow-lg">
                  <UserPlus className="w-4 h-4" />
                </div>
              </div>
              <span className="stat-value text-blue-500">{stats.nouveauxMembres ?? 0}</span>
              <span className="text-[10px] text-gray-400 mt-1 block">30 derniers jours</span>
            </div>
            <div className="stat-card animate-slide-up" style={{ animationDelay: '180ms' }}>
              <div className="absolute top-0 left-0 right-0 h-1 bg-gradient-to-r from-violet-500 to-purple-500 opacity-60" />
              <div className="flex items-start justify-between mb-3">
                <span className="stat-label">Taux de présence</span>
                <div className="p-2 rounded-xl bg-gradient-to-br from-violet-500 to-purple-500 text-white shadow-lg">
                  <Activity className="w-4 h-4" />
                </div>
              </div>
              <span className="stat-value text-violet-500">{stats.tauxPresence ?? 0}%</span>
            </div>
            <div className="stat-card animate-slide-up" style={{ animationDelay: '240ms' }}>
              <div className="absolute top-0 left-0 right-0 h-1 bg-gradient-to-r from-emerald-500 to-teal-500 opacity-60" />
              <div className="flex items-start justify-between mb-3">
                <span className="stat-label">Rapports reçus</span>
                <div className="p-2 rounded-xl bg-gradient-to-br from-emerald-500 to-teal-500 text-white shadow-lg">
                  <FileText className="w-4 h-4" />
                </div>
              </div>
              <span className="stat-value">{stats.rapportsSoumis ?? 0}</span>
              <span className="text-[10px] text-gray-400 mt-1 block">/ {stats.rapportsAttendus ?? 0} attendus</span>
            </div>
            <div className="stat-card animate-slide-up" style={{ animationDelay: '300ms' }}>
              <div className="absolute top-0 left-0 right-0 h-1 bg-gradient-to-r from-amber-500 to-orange-500 opacity-60" />
              <div className="flex items-start justify-between mb-3">
                <span className="stat-label">Anniversaires</span>
                <div className="p-2 rounded-xl bg-gradient-to-br from-pink-500 to-rose-500 text-white shadow-lg">
                  <Cake className="w-4 h-4" />
                </div>
              </div>
              <span className="stat-value text-pink-500">{anniversaires.length}</span>
              <span className="text-[10px] text-gray-400 mt-1 block">ce mois-ci</span>
            </div>
            <div className="stat-card animate-slide-up" style={{ animationDelay: '360ms' }}>
              <div className="absolute top-0 left-0 right-0 h-1 bg-gradient-to-r from-amber-500 to-orange-500 opacity-60" />
              <div className="flex items-start justify-between mb-3">
                <span className="stat-label">Équipes</span>
                <div className="p-2 rounded-xl bg-gradient-to-br from-amber-500 to-orange-500 text-white shadow-lg">
                  <Network className="w-4 h-4" />
                </div>
              </div>
              <span className="stat-value">{stats.equipesActives ?? 0}</span>
              <span className="text-[10px] text-gray-400 mt-1 block">actives · {stats.postesActifs ?? 0} postes</span>
            </div>
            <div className="stat-card animate-slide-up" style={{ animationDelay: '420ms' }}>
              <div className="absolute top-0 left-0 right-0 h-1 bg-gradient-to-r from-red-500 to-rose-500 opacity-60" />
              <div className="flex items-start justify-between mb-3">
                <span className="stat-label">Tâches en retard</span>
                <div className="p-2 rounded-xl bg-gradient-to-br from-red-500 to-rose-500 text-white shadow-lg">
                  <AlertCircle className="w-4 h-4" />
                </div>
              </div>
              <span className="stat-value text-red-500">{stats.tachesEnRetard ?? 0}</span>
              <span className="text-[10px] text-gray-400 mt-1 block">{stats.tachesOuvertes ?? 0} ouvertes</span>
            </div>
          </div>

          {/* Reports progress */}
          <div className="glass-card p-4 mb-6 animate-slide-up" style={{ animationDelay: '360ms' }}>
            <div className="flex items-center justify-between mb-2">
              <span className="text-xs font-medium text-gray-600 dark:text-gray-400">Progression des rapports</span>
              <span className="text-xs font-semibold text-gray-900 dark:text-gray-100">
                {stats.rapportsSoumis ?? 0} / {stats.rapportsAttendus ?? 0}
              </span>
            </div>
            <div className="flex items-center gap-2">
              <div className="flex-1 bg-gray-200 dark:bg-gray-700 rounded-full h-3">
                <div
                  className="h-3 rounded-full bg-gradient-to-r from-amber-500 to-emerald-500 transition-all duration-500"
                  style={{ width: `${Math.min(stats.tauxCompletion ?? 0, 100)}%` }}
                />
              </div>
              <span className="text-xs font-bold text-gray-700 dark:text-gray-300">{stats.tauxCompletion ?? 0}%</span>
            </div>
          </div>

          {/* ==================== SAISIE DES PRÉSENCES ==================== */}
          <div id="saisie-presences" className="glass-card p-5 mb-6 animate-slide-up scroll-mt-24" style={{ animationDelay: '380ms' }}>
            <div className="flex items-center justify-between flex-wrap gap-3 mb-4">
              <div className="flex items-center gap-3">
                <div className="p-2.5 rounded-xl bg-gradient-to-br from-sky-500 to-blue-600 text-white shadow-lg">
                  <ClipboardCheck className="w-5 h-5" />
                </div>
                <div>
                  <h3 className="text-base font-semibold text-gray-900 dark:text-gray-100">Saisie des présences</h3>
                  <p className="text-xs text-gray-400">Marquez la présence des membres de {dashboard?.selectedDeptNom} pour la semaine</p>
                </div>
              </div>
              <span className="badge text-[10px] badge-info">{presenceSheet.length} membre{presenceSheet.length > 1 ? 's' : ''}</span>
            </div>

            {/* Filtres : semaine + programme */}
            <div className="grid grid-cols-1 sm:grid-cols-4 gap-3 mb-4">
              <div>
                <label className="label">Semaine</label>
                <input
                  type="date"
                  className="input"
                  value={presenceSemaine}
                  onChange={(e) => setPresenceSemaine(e.target.value)}
                />
              </div>
              <div>
                <label className="label">Programme</label>
                <select
                  className="input"
                  value={presenceType}
                  onChange={(e) => { setPresenceType(e.target.value); setPresenceSousType(''); }}
                >
                  <option value="">Général</option>
                  {programTypes.map((pt) => (
                    <option key={pt.id} value={pt.code}>{pt.label}</option>
                  ))}
                </select>
              </div>
              {sousTypes.length > 0 && (
                <div>
                  <label className="label">Sous-programme</label>
                  <select
                    className="input"
                    value={presenceSousType}
                    onChange={(e) => setPresenceSousType(e.target.value)}
                  >
                    <option value="">—</option>
                    {sousTypes.map((st) => (
                      <option key={st.id} value={st.label}>
                        {st.label}{st.heureDebut ? ` · ${st.heureDebut.slice(0, 5)}` : ''}
                      </option>
                    ))}
                  </select>
                </div>
              )}
              <div className="flex items-end justify-end">
                <button
                  onClick={submitPresences}
                  disabled={presenceMutation.isPending}
                  className="btn-primary btn-sm w-full sm:w-auto"
                >
                  {presenceMutation.isPending ? <Loader2 className="w-4 h-4 animate-spin" /> : <Save className="w-4 h-4" />}
                  Enregistrer les présences
                </button>
              </div>
            </div>

            {/* Liste des membres avec case présent/absent */}
            {sheetLoading ? (
              <div className="space-y-2">
                {[1, 2, 3].map((i) => (
                  <div key={i} className="skeleton h-14 w-full rounded-xl" />
                ))}
              </div>
            ) : presenceSheet.length === 0 ? (
              <div className="text-center py-6">
                <ClipboardCheck className="w-8 h-8 text-gray-300 mx-auto mb-2" />
                <p className="text-sm text-gray-400">Aucun membre à pointer pour cette semaine</p>
              </div>
            ) : (
              <div className="space-y-1.5 max-h-96 overflow-y-auto pr-1">
                {presenceSheet.map((m) => (
                  <div key={m.soulId} className="flex items-center gap-3 p-2.5 rounded-xl hover:bg-gray-50 dark:hover:bg-gray-800/40 transition-all">
                    <button
                      onClick={() => setPresenceForm((f) => ({ ...f, [m.soulId]: !(f[m.soulId] ?? false) }))}
                      className={`w-9 h-9 rounded-xl flex items-center justify-center shrink-0 transition-all ${
                        presenceForm[m.soulId]
                          ? 'bg-emerald-500 text-white shadow-glow' 
                          : 'bg-gray-100 dark:bg-gray-800 text-gray-400'
                      }`}
                      title={presenceForm[m.soulId] ? 'Marquer absent' : 'Marquer présent'}
                    >
                      {presenceForm[m.soulId]
                        ? <CheckCircle className="w-5 h-5" />
                        : <UserX className="w-5 h-5" />}
                    </button>
                    <div className="min-w-0 flex-1">
                      <p className="text-sm font-medium text-gray-800 dark:text-gray-200 truncate">{m.nom}</p>
                      <p className="text-[10px] text-gray-400 truncate">
                        {m.familleNom ? `Famille ${m.familleNom}` : 'Sans famille'}
                        {m.statut ? ` · ${m.statut.replace(/_/g, ' ').toLowerCase()}` : ''}
                      </p>
                    </div>
                    {!m.userId && (
                      <span className="badge text-[10px] badge-gray" title="Sans compte utilisateur lié">Pas de compte</span>
                    )}
                    <input
                      className="input w-28 hidden sm:block"
                      placeholder="Note"
                      value={presenceNotes[m.soulId] || ''}
                      onChange={(e) => setPresenceNotes((n) => ({ ...n, [m.soulId]: e.target.value }))}
                    />
                    <span className={`text-[10px] font-medium ${presenceForm[m.soulId] ? 'text-emerald-500' : 'text-red-400'}`}>
                      {presenceForm[m.soulId] ? 'Présent' : 'Absent'}
                    </span>
                  </div>
                ))}
              </div>
            )}
          </div>

          {/* ==================== POINTAGE DES PRÉSENCES AUX ÉVÉNEMENTS ==================== */}
          <div className="glass-card p-5 mb-6 animate-slide-up" style={{ animationDelay: '400ms' }}>
            <div className="flex items-center justify-between flex-wrap gap-3 mb-4">
              <div className="flex items-center gap-3">
                <div className="p-2.5 rounded-xl bg-gradient-to-br from-violet-500 to-purple-600 text-white shadow-lg">
                  <CalendarDays className="w-5 h-5" />
                </div>
                <div>
                  <h3 className="text-base font-semibold text-gray-900 dark:text-gray-100">Présence aux événements</h3>
                  <p className="text-xs text-gray-400">Pointez les membres de {dashboard?.selectedDeptNom} à chaque événement du département</p>
                </div>
              </div>
              <span className="badge text-[10px] badge-info">{deptEvents.length} événement{deptEvents.length > 1 ? 's' : ''}</span>
            </div>
            {deptEvents.length === 0 ? (
              <p className="text-sm text-gray-400 text-center py-6">Aucun événement rattaché à ce département</p>
            ) : (
              <div className="space-y-1.5 max-h-80 overflow-y-auto pr-1">
                {deptEvents.map((e: any) => (
                  <div key={e.id} className="flex items-center justify-between gap-3 p-2.5 rounded-xl hover:bg-gray-50 dark:hover:bg-gray-800/40 transition-all">
                    <div className="flex items-center gap-3 min-w-0">
                      <div className="p-2 rounded-lg bg-violet-500/10 text-violet-600 dark:text-violet-300 shrink-0">
                        <Calendar className="w-4 h-4" />
                      </div>
                      <div className="min-w-0">
                        <p className="text-sm font-medium text-gray-900 dark:text-gray-100 truncate">{e.titre}</p>
                        <p className="text-[10px] text-gray-400">
                          {e.dateDebut ? new Date(e.dateDebut).toLocaleDateString('fr-FR', { day: 'numeric', month: 'short', hour: '2-digit', minute: '2-digit' }) : '—'}
                          {e.statut ? ` · ${e.statut.replace('_', ' ').toLowerCase()}` : ''}
                        </p>
                      </div>
                    </div>
                    <button
                      onClick={() => setAttendanceEvent(e)}
                      className="btn-ghost btn-xs inline-flex cursor-pointer shrink-0"
                      title="Pointer la présence des membres"
                    >
                      <UserCheck className="w-3.5 h-3.5" /> Présences
                    </button>
                  </div>
                ))}
              </div>
            )}
          </div>

          <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
            {/* Members list — actions centrées membre (fiche / présence / rapport) */}
            <div className="glass-card p-5 animate-slide-up">
              <div className="flex items-center justify-between mb-4">
                <div className="flex items-center gap-2">
                  <Users className="w-4 h-4 text-primary-500" />
                  <div>
                    <h3 className="text-sm font-semibold text-gray-900 dark:text-gray-100">Fiches membres</h3>
                    <p className="text-[10px] text-gray-400">fiche · présence · rapport</p>
                  </div>
                </div>
                <Link to={`/departments/${activeDeptId}`} className="text-[10px] font-medium text-primary-600">
                  Gérer les membres
                </Link>
              </div>
              <div className="space-y-2 max-h-80 overflow-y-auto">
                {(deptDetail?.membres ?? []).slice(0, 10).map((m: any) => (
                  <div
                    key={m.id}
                    className="flex items-center gap-2 p-2.5 rounded-xl hover:bg-gray-50 dark:hover:bg-gray-800/40 transition-all"
                  >
                    <Link
                      to={`/souls/${m.id}`}
                      title="Ouvrir la fiche membre"
                      className="flex items-center gap-2 min-w-0 flex-1"
                    >
                      <div className={`w-8 h-8 rounded-lg flex items-center justify-center text-xs font-bold text-white shrink-0
                        ${m.statut === 'ACTIF' ? 'bg-emerald-500' : m.statut === 'EN_INTEGRATION' ? 'bg-blue-500' : m.statut === 'EN_VEILLE' ? 'bg-amber-500' : 'bg-red-500'}`}>
                        {m.nom?.split(' ').map((p: string) => p?.[0]).join('').slice(0, 2) || '?'}
                      </div>
                      <div className="min-w-0">
                        <p className="text-sm font-medium text-gray-800 dark:text-gray-200 truncate">{m.nom}</p>
                        <p className="text-[10px] text-gray-400 truncate">
                          {m.familleNom ? `Famille ${m.familleNom}` : 'Sans famille'}
                          {m.faiseurNom ? ` · ${m.faiseurNom}` : ''}
                        </p>
                      </div>
                    </Link>
                    {/* Actions centrées membre */}
                    <div className="flex items-center gap-1 flex-shrink-0">
                      <button
                        type="button"
                        onClick={() => navigate(`/souls/${m.id}`)}
                        title="Fiche membre"
                        className="p-1.5 rounded-lg cursor-pointer text-gray-400 hover:text-primary-600 hover:bg-primary-500/10 transition-all"
                      >
                        <UserRound className="w-3.5 h-3.5" />
                      </button>
                      <button
                        type="button"
                        onClick={scrollToPresences}
                        title="Pointer sa présence"
                        className="p-1.5 rounded-lg cursor-pointer text-gray-400 hover:text-sky-600 hover:bg-sky-500/10 transition-all"
                      >
                        <ClipboardCheck className="w-3.5 h-3.5" />
                      </button>
                      <button
                        type="button"
                        onClick={() => navigate(`/departments/${activeDeptId}/report`)}
                        title="Rapport du département"
                        className="p-1.5 rounded-lg cursor-pointer text-gray-400 hover:text-emerald-600 hover:bg-emerald-500/10 transition-all"
                      >
                        <FileText className="w-3.5 h-3.5" />
                      </button>
                    </div>
                  </div>
                ))}
                {(deptDetail?.membres ?? []).length === 0 && (
                  <div className="text-center py-8">
                    <UserX className="w-8 h-8 text-gray-300 mx-auto mb-2" />
                    <p className="text-sm text-gray-400">Aucun membre dans ce département</p>
                  </div>
                )}
              </div>
            </div>

            {/* Birthdays + stats */}
            <div className="space-y-6">
              <div className="glass-card p-5 animate-slide-up" style={{ animationDelay: '100ms' }}>
                <div className="flex items-center gap-2 mb-4">
                  <Cake className="w-4 h-4 text-pink-500" />
                  <h3 className="text-sm font-semibold text-gray-900 dark:text-gray-100">Anniversaires du mois</h3>
                </div>
                <div className="space-y-2">
                  {anniversaires.length === 0 && (
                    <p className="text-sm text-gray-400 text-center py-4">Aucun anniversaire ce mois-ci</p>
                  )}
                  {anniversaires.map((a: any) => (
                    <div key={a.id} className="flex items-center justify-between p-2.5 rounded-xl bg-gray-50 dark:bg-gray-800/50">
                      <div className="flex items-center gap-2">
                        <div className="w-8 h-8 rounded-lg bg-gradient-to-br from-pink-500 to-rose-500 flex items-center justify-center text-white">
                          <Cake className="w-4 h-4" />
                        </div>
                        <span className="text-sm font-medium text-gray-800 dark:text-gray-200">{a.nom}</span>
                      </div>
                      <span className="text-xs font-semibold text-pink-500">{formatDate(a.dateNaissance)}</span>
                    </div>
                  ))}
                </div>
              </div>

              {/* Status distribution */}
              <div className="glass-card p-5 animate-slide-up" style={{ animationDelay: '150ms' }}>
                <div className="flex items-center gap-2 mb-4">
                  <Activity className="w-4 h-4 text-primary-500" />
                  <h3 className="text-sm font-semibold text-gray-900 dark:text-gray-100">Répartition des membres</h3>
                </div>
                <div className="grid grid-cols-2 gap-2">
                  {[
                    { label: 'Actifs', value: deptDetail.actifs ?? 0, color: 'text-emerald-500' },
                    { label: 'En intégration', value: deptDetail.enIntegration ?? 0, color: 'text-blue-500' },
                    { label: 'En veille', value: deptDetail.enVeille ?? 0, color: 'text-amber-500' },
                    { label: 'Décrochés', value: deptDetail.decroches ?? 0, color: 'text-red-500' },
                  ].map((s) => (
                    <div key={s.label} className="text-center p-3 rounded-xl bg-gray-50 dark:bg-gray-800/50">
                      <p className={`text-xl font-bold ${s.color}`}>{s.value}</p>
                      <p className="text-[9px] text-gray-400">{s.label}</p>
                    </div>
                  ))}
                </div>
              </div>
            </div>
          </div>

          {/* Quick Actions — centrées sur la gestion des membres (HRM) */}
          <div className="grid grid-cols-2 sm:grid-cols-4 gap-3 mt-6 animate-slide-up">
            <button
              type="button"
              onClick={scrollToPresences}
              className="glass-card p-4 text-center cursor-pointer hover:bg-gray-50/80 dark:hover:bg-gray-800/40 transition-all duration-200"
            >
              <ClipboardCheck className="w-5 h-5 text-sky-500 mx-auto mb-1" />
              <span className="text-[10px] font-medium text-gray-600 dark:text-gray-400">Saisie des présences</span>
            </button>
            <Link to={`/departments/${activeDeptId}`} className="glass-card p-4 text-center hover:bg-gray-50/80 dark:hover:bg-gray-800/40 transition-all duration-200">
              <Users className="w-5 h-5 text-amber-500 mx-auto mb-1" />
              <span className="text-[10px] font-medium text-gray-600 dark:text-gray-400">Fiches membres</span>
            </Link>
            <Link to={`/departments/${activeDeptId}/manage`} className="glass-card p-4 text-center hover:bg-gray-50/80 dark:hover:bg-gray-800/40 transition-all duration-200">
              <Network className="w-5 h-5 text-orange-500 mx-auto mb-1" />
              <span className="text-[10px] font-medium text-gray-600 dark:text-gray-400">Gestion & équipes</span>
            </Link>
            <Link to={`/departments/${activeDeptId}/report`} className="glass-card p-4 text-center hover:bg-gray-50/80 dark:hover:bg-gray-800/40 transition-all duration-200">
              <FileText className="w-5 h-5 text-emerald-500 mx-auto mb-1" />
              <span className="text-[10px] font-medium text-gray-600 dark:text-gray-400">Rapport</span>
            </Link>
            <Link to={`/events`} className="glass-card p-4 text-center hover:bg-gray-50/80 dark:hover:bg-gray-800/40 transition-all duration-200">
              <Calendar className="w-5 h-5 text-primary-500 mx-auto mb-1" />
              <span className="text-[10px] font-medium text-gray-600 dark:text-gray-400">Événements</span>
            </Link>
            <Link to={`/departments/${activeDeptId}/stats`} className="glass-card p-4 text-center hover:bg-gray-50/80 dark:hover:bg-gray-800/40 transition-all duration-200">
              <Activity className="w-5 h-5 text-violet-500 mx-auto mb-1" />
              <span className="text-[10px] font-medium text-gray-600 dark:text-gray-400">Statistiques</span>
            </Link>
          </div>

          {/* ==================== ALERTES & SUIVI ==================== */}
          <div className="grid grid-cols-1 lg:grid-cols-2 gap-6 mt-6">
            {/* Alertes intelligentes */}
            <div className="glass-card p-5 animate-slide-up">
              <div className="flex items-center justify-between mb-4">
                <div className="flex items-center gap-2">
                  <AlertCircle className="w-4 h-4 text-red-500" />
                  <h3 className="text-sm font-semibold text-gray-900 dark:text-gray-100">Alertes à traiter</h3>
                </div>
                <span className="badge text-[10px] badge-danger">{alertes.length}</span>
              </div>
              {alertes.length === 0 ? (
                <p className="text-sm text-gray-400 text-center py-6">Aucune alerte active 🎉</p>
              ) : (
                <div className="space-y-2 max-h-72 overflow-y-auto pr-1">
                  {alertes.map((a: any) => (
                    <div key={a.id} className="p-3 rounded-xl bg-red-50 dark:bg-red-900/20 border border-red-200/50 dark:border-red-500/20">
                      <p className="text-sm font-medium text-gray-800 dark:text-gray-200">{a.titre}</p>
                      <p className="text-[10px] text-gray-500 dark:text-gray-400 mt-0.5">{a.message}</p>
                    </div>
                  ))}
                </div>
              )}
            </div>

            {/* Nouveaux membres + à suivre */}
            <div className="space-y-6">
              <div className="glass-card p-5 animate-slide-up">
                <div className="flex items-center gap-2 mb-4">
                  <UserPlus className="w-4 h-4 text-blue-500" />
                  <h3 className="text-sm font-semibold text-gray-900 dark:text-gray-100">Nouveaux membres (30 jours)</h3>
                </div>
                {nouveauxRecents.length === 0 ? (
                  <p className="text-sm text-gray-400 text-center py-4">Aucun nouveau membre récent</p>
                ) : (
                  <div className="space-y-1.5">
                    {nouveauxRecents.map((m: any) => (
                      <Link
                        key={m.id}
                        to={`/departments/${activeDeptId}/members/${m.id}`}
                        className="flex items-center gap-3 p-2 rounded-xl hover:bg-gray-50 dark:hover:bg-gray-800/40 transition-all"
                      >
                        <div className="w-8 h-8 rounded-lg bg-gradient-to-br from-blue-500 to-indigo-500 flex items-center justify-center text-white text-xs font-bold shrink-0">
                          {m.nom?.split(' ').map((p: string) => p?.[0]).join('').slice(0, 2) || '?'}
                        </div>
                        <div className="min-w-0 flex-1">
                          <p className="text-sm font-medium text-gray-800 dark:text-gray-200 truncate">{m.nom}</p>
                          <p className="text-[10px] text-gray-400">
                            intégré le {m.dateIntegration || '—'}
                            {m.origine === 'SIGNUP' ? ' · inscription' : ''}
                          </p>
                        </div>
                        <ChevronRight className="w-3.5 h-3.5 text-gray-300" />
                      </Link>
                    ))}
                  </div>
                )}
              </div>

              <div className="glass-card p-5 animate-slide-up">
                <div className="flex items-center justify-between mb-4">
                  <div className="flex items-center gap-2">
                    <UserX className="w-4 h-4 text-amber-500" />
                    <h3 className="text-sm font-semibold text-gray-900 dark:text-gray-100">À suivre cette semaine</h3>
                  </div>
                  <span className="badge text-[10px] badge-warning">{stats.membresSuivi ?? 0}</span>
                </div>
                {membresSuivi.length === 0 ? (
                  <p className="text-sm text-gray-400 text-center py-4">Tous les membres ont un rapport cette semaine ✅</p>
                ) : (
                  <div className="space-y-1.5 max-h-40 overflow-y-auto pr-1">
                    {membresSuivi.map((m: any) => (
                      <Link key={m.id} to={`/departments/${activeDeptId}/members/${m.id}`} className="flex items-center gap-2 p-2 rounded-xl hover:bg-gray-50 dark:hover:bg-gray-800/40 transition-all">
                        <UserX className="w-3.5 h-3.5 text-amber-500 shrink-0" />
                        <span className="text-sm text-gray-800 dark:text-gray-200 truncate flex-1">{m.nom}</span>
                        <span className="text-[9px] text-gray-400">{m.statut}</span>
                      </Link>
                    ))}
                  </div>
                )}
              </div>
            </div>
          </div>

          {/* Événements à venir + transferts en attente */}
          <div className="grid grid-cols-1 lg:grid-cols-2 gap-6 mt-6">
            <div className="glass-card p-5 animate-slide-up">
              <div className="flex items-center gap-2 mb-4">
                <Calendar className="w-4 h-4 text-primary-500" />
                <h3 className="text-sm font-semibold text-gray-900 dark:text-gray-100">Événements à venir (30 jours)</h3>
              </div>
              {evenementsAvenir.length === 0 ? (
                <p className="text-sm text-gray-400 text-center py-4">Aucun événement à venir</p>
              ) : (
                <div className="space-y-1.5">
                  {evenementsAvenir.map((ev: any) => (
                    <Link key={ev.id} to="/events" className="flex items-center gap-3 p-2.5 rounded-xl hover:bg-gray-50 dark:hover:bg-gray-800/40 transition-all">
                      <div className="p-2 rounded-lg bg-primary-500/10 text-primary-600 dark:text-primary-300">
                        <Calendar className="w-4 h-4" />
                      </div>
                      <div className="min-w-0 flex-1">
                        <p className="text-sm font-medium text-gray-800 dark:text-gray-200 truncate">{ev.titre}</p>
                        <p className="text-[10px] text-gray-400">{formatDate(ev.dateDebut?.slice(0, 10))}{ev.lieu ? ` · ${ev.lieu}` : ''}</p>
                      </div>
                    </Link>
                  ))}
                </div>
              )}
            </div>
            <div className="glass-card p-5 animate-slide-up">
              <div className="flex items-center justify-between mb-4">
                <div className="flex items-center gap-2">
                  <ArrowLeftRight className="w-4 h-4 text-orange-500" />
                  <h3 className="text-sm font-semibold text-gray-900 dark:text-gray-100">Transferts en attente</h3>
                </div>
                <span className={`badge text-[10px] ${stats.transfertsEnAttente > 0 ? 'badge-warning' : 'badge-gray'}`}>{stats.transfertsEnAttente ?? 0}</span>
              </div>
              <p className="text-sm text-gray-500 dark:text-gray-400 mb-3">
                {stats.transfertsEnAttente > 0
                  ? 'Des demandes de transfert concernant vos membres attendent une validation.'
                  : 'Aucune demande de transfert en attente.'}
              </p>
              <Link to="/transfers" className="btn-ghost btn-xs">
                Voir les demandes <ChevronRight className="w-3 h-3 ml-1" />
              </Link>
            </div>
          </div>

          {/* Modale de pointage des présences à un événement */}
          {attendanceEvent && activeDeptId && (
            <EventAttendanceModal
              deptId={activeDeptId}
              event={attendanceEvent as any}
              onClose={() => setAttendanceEvent(null)}
            />
          )}
        </>
      )}
    </div>
  );
}
