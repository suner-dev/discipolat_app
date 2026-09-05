import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useAuth } from '@/contexts/AuthContext';
import { useI18n } from '@/i18n';
import api, { getErrorMessage } from '@/lib/api';
import toast from 'react-hot-toast';
import type {
  MemberDashboard,
  UpdateMemberProfileRequest,
  MemberPresence,
  SubmitPresenceRequest,
  MemberRequest,
  MemberRequestType,
  MemberRequestTarget,
  MemberRequestStatus,
  CreateMemberRequest,
  ProgramType,
} from '@/types';
import { Link } from 'react-router-dom';
import {
  Sparkles, User, Mail, Phone, Calendar, GraduationCap, Briefcase, Heart,
  Users, Building2, Camera, Edit3, Save, X, Loader2, MessageSquare,
  ChevronRight, UserCheck, Church, Cake, CalendarCheck, Send, Paperclip,
  CheckCircle, Star, TrendingUp, Zap, BookOpen, ClipboardCheck, Trophy,
} from 'lucide-react';
import AttachmentPicker from '@/components/shared/AttachmentPicker';
import AttachmentLinks from '@/components/shared/AttachmentLinks';
import { canAccessFilesModule } from '@/components/shared/AttachmentPicker';
import { useDictionaries } from '@/hooks/useDictionaries';

const STATUT_BADGES: Record<string, string> = {
  MEMBRE: 'badge-info',
  FAISEUR: 'badge-success',
  CHEF_DE_FAMILLE: 'bg-gold-100 dark:bg-gold-900/30 text-gold-700 dark:text-gold-400 border border-gold-200/50',
};

/** Repli (dictionnaire indisponible) — les valeurs réelles viennent de la base. */
const STATUT_FALLBACK: Record<string, string> = {
  MEMBRE: 'Membre',
  FAISEUR: 'Faiseur de disciples',
  CHEF_DE_FAMILLE: 'Chef de famille',
};

/** Repli (dictionnaire indisponible) — les valeurs réelles viennent de la base. */
const SITUATION_FALLBACK: Record<string, string> = {
  CELIBATAIRE: 'Célibataire',
  MARIE: 'Marié(e)',
  DIVORCE: 'Divorcé(e)',
  VEUF: 'Veuf/Veuve',
  PARENT_CELIBATAIRE: 'Parent célibataire',
  AUTRE: 'Autre',
};

const NIVEAU_ETUDES = ['Primaire', 'Secondaire', 'Baccalauréat', 'Licence', 'Master', 'Doctorat', 'Formation professionnelle', 'Autre'];

const TYPE_LABELS: Record<MemberRequestType, string> = {
  SUGGESTION: '💡 Suggestion',
  RENDEZ_VOUS: '📅 Rendez-vous',
  SIGNALEMENT: '⚠️ Signalement',
};

/** Replis (dictionnaires indisponibles) — les valeurs réelles viennent de la base. */
const CIBLE_FALLBACK: Record<MemberRequestTarget, string> = {
  PASTEUR: 'Pasteur',
  RESPONSABLE: 'Responsable',
  CHEF_DE_FAMILLE: 'Chef de famille',
};

const STATUS_BADGES: Record<MemberRequestStatus, string> = {
  OUVERT: 'badge-warning',
  EN_COURS: 'badge-info',
  RESOLU: 'badge-success',
  REJETE: 'badge-error',
};

const STATUS_FALLBACK: Record<MemberRequestStatus, string> = {
  OUVERT: 'Ouvert',
  EN_COURS: 'En cours',
  RESOLU: 'Résolu',
  REJETE: 'Rejeté',
};

/** Lundi de la semaine courante (AAAA-MM-JJ). */
const currentWeekMonday = () => {
  const d = new Date();
  d.setDate(d.getDate() - ((d.getDay() + 6) % 7));
  return d.toISOString().split('T')[0];
};

const tauxPresence = (p: MemberPresence) => {
  const entries = Object.entries(p.presences || {});
  if (entries.length === 0) return 0;
  return Math.round((entries.filter(([, v]) => v).length * 100) / entries.length);
};

const getGreeting = (t: (k: string) => string) => {
  const h = new Date().getHours();
  if (h < 12) return t('member.greetingMorning');
  if (h < 17) return t('member.greetingAfternoon');
  return t('member.greetingEvening');
};

const initials = (p?: MemberDashboard['user']) =>
  `${p?.firstName?.[0] || ''}${p?.lastName?.[0] || ''}`.toUpperCase() || '👤';

export default function MemberDashboardPage() {
  const { t, locale } = useI18n();
  const { user, activeRole } = useAuth();
  const dictionaries = useDictionaries();
  const canManage = activeRole === 'MEMBRE'; // Les autres rôles voient en lecture seule
  const queryClient = useQueryClient();
  const [editing, setEditing] = useState(false);
  const [form, setForm] = useState<UpdateMemberProfileRequest>({});

  const { data: dashboard, isLoading, isError, error } = useQuery({
    queryKey: ['members', 'me', 'dashboard'],
    queryFn: async () => {
      const res = await api.get('/members/me/dashboard');
      return res.data as MemberDashboard;
    },
  });

  const mutation = useMutation({
    mutationFn: async (payload: UpdateMemberProfileRequest) => {
      const res = await api.put('/members/me/profile', payload);
      return res.data as MemberDashboard;
    },
    onSuccess: () => {
      toast.success(t('member.updated'));
      setEditing(false);
      queryClient.invalidateQueries({ queryKey: ['members', 'me', 'dashboard'] });
    },
    onError: (err) => toast.error(getErrorMessage(err)),
  });

  // ==================== Phase 2 : présences hebdomadaires ====================
  const { data: presences = [] } = useQuery({
    queryKey: ['members', 'me', 'presences'],
    queryFn: async () => (await api.get('/members/me/presences')).data as MemberPresence[],
  });

  // Types de programmes configurés par le pasteur (saisie flexible)
  const { data: programTypes = [] } = useQuery({
    queryKey: ['programs', 'active'],
    queryFn: async () => (await api.get('/programs/active')).data as ProgramType[],
  });

  const [presenceForm, setPresenceForm] = useState<Record<string, boolean>>({});
  const [presenceNotes, setPresenceNotes] = useState('');
  const [presenceType, setPresenceType] = useState('');
  const [presenceSousType, setPresenceSousType] = useState('');

  const selectedProgramType = programTypes.find((p) => p.code === presenceType);
  const sousTypes = selectedProgramType?.aSousProgrammes ? selectedProgramType.sousProgrammes || [] : [];

  const presenceMutation = useMutation({
    mutationFn: async (payload: SubmitPresenceRequest) => {
      const res = await api.post('/members/me/presences', payload);
      return res.data as MemberPresence;
    },
    onSuccess: () => {
      toast.success(t('member.presenceSaved'));
      setPresenceNotes('');
      setPresenceForm({});
      setPresenceType('');
      setPresenceSousType('');
      queryClient.invalidateQueries({ queryKey: ['members', 'me', 'presences'] });
    },
    onError: (err) => toast.error(getErrorMessage(err)),
  });

  const submitPresence = () => {
    const selected = Object.values(presenceForm).filter(Boolean).length;
    if (selected === 0 && !presenceNotes.trim()) {
      toast.error(t('member.checkProgram'));
      return;
    }
    presenceMutation.mutate({
      semaine: currentWeekMonday(),
      presences: presenceForm,
      notes: presenceNotes.trim() || undefined,
      typeProgramme: presenceType || undefined,
      sousProgramme: presenceSousType || undefined,
    });
  };

  // ==================== Phase 2 : suggestions, rendez-vous, signalements ====================
  const { data: myRequests = [] } = useQuery({
    queryKey: ['members', 'me', 'requests'],
    queryFn: async () => (await api.get('/members/me/requests')).data as MemberRequest[],
  });

  const [requestForm, setRequestForm] = useState<{
    type: MemberRequestType;
    cible: MemberRequestTarget;
    message: string;
    fichierIds: string[];
  }>({ type: 'SUGGESTION', cible: 'PASTEUR', message: '', fichierIds: [] });

  const requestMutation = useMutation({
    mutationFn: async (payload: CreateMemberRequest) => {
      const res = await api.post('/members/me/requests', payload);
      return res.data as MemberRequest;
    },
    onSuccess: () => {
      toast.success(t('member.requestSent'));
      setRequestForm({ type: 'SUGGESTION', cible: 'PASTEUR', message: '', fichierIds: [] });
      queryClient.invalidateQueries({ queryKey: ['members', 'me', 'requests'] });
    },
    onError: (err) => toast.error(getErrorMessage(err)),
  });

  const submitRequest = () => {
    if (!requestForm.message.trim()) {
      toast.error(t('member.writeMessage'));
      return;
    }
    requestMutation.mutate(requestForm);
  };

  // ==================== Phase 3 : progression, événements, notes ====================
  const { data: progression } = useQuery({
    queryKey: ['members', 'me', 'progression'],
    queryFn: async () => (await api.get('/members/me/progression')).data as Record<string, unknown>,
  });

  const { data: myEvents = [] } = useQuery({
    queryKey: ['members', 'me', 'events'],
    queryFn: async () => (await api.get('/members/me/events')).data as Array<Record<string, unknown>>,
  });

  const { data: myNotes = [] } = useQuery({
    queryKey: ['members', 'me', 'notes'],
    queryFn: async () => (await api.get('/members/me/notes')).data as Array<Record<string, unknown>>,
  });

  const openEdit = () => {
    setForm({
      phone: dashboard?.user.phone || '',
      photoUrl: dashboard?.user.photoUrl || '',
      situationFamiliale: dashboard?.user.situationFamiliale || '',
      dateNaissance: dashboard?.user.dateNaissance || '',
      profession: dashboard?.soul?.profession || '',
      niveauEtude: dashboard?.soul?.niveauEtude || '',
      nbEnfants: dashboard?.soul?.nbEnfants ?? 0,
    });
    setEditing(true);
  };

  const submit = () => {
    mutation.mutate({
      ...form,
      phone: form.phone || undefined,
      photoUrl: form.photoUrl ?? '',
      profession: form.profession || undefined,
      niveauEtude: form.niveauEtude || undefined,
      nbEnfants: form.situationFamiliale === 'PARENT_CELIBATAIRE' ? form.nbEnfants ?? 0 : undefined,
    });
  };

  if (isLoading) {
    return (
      <div className="page-container">
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
          {[...Array(6)].map((_, i) => (
            <div key={i} className="glass-card p-6 animate-fade-in" style={{ animationDelay: `${i * 60}ms` }}>
              <div className="skeleton h-5 w-32 mb-4 rounded" />
              <div className="skeleton h-4 w-full mb-2 rounded" />
              <div className="skeleton h-4 w-3/4 rounded" />
            </div>
          ))}
        </div>
      </div>
    );
  }

  if (isError || !dashboard) {
    return (
      <div className="page-container">
        <div className="glass-card p-8 text-center animate-scale-in">
          <div className="p-3 rounded-full bg-red-100 dark:bg-red-900/20 w-fit mx-auto mb-4">
            <X className="w-6 h-6 text-red-500" />
          </div>
          <h2 className="text-lg font-semibold text-gray-900 dark:text-gray-100 mb-1">
            {t('member.loadError')}
          </h2>
          <p className="text-sm text-gray-500 dark:text-gray-400 mb-5">{getErrorMessage(error)}</p>
          <button onClick={() => queryClient.invalidateQueries({ queryKey: ['members', 'me', 'dashboard'] })} className="btn-primary btn-sm">
            <Loader2 className="w-4 h-4" /> {t('member.retry')}
          </button>
        </div>
      </div>
    );
  }

  const d = dashboard;
  const statutLabel = dictionaries.label('USER_ROLE', d.statutMembre) || STATUT_FALLBACK[d.statutMembre] || d.statutMembre;
  const statut = { label: statutLabel, badge: STATUT_BADGES[d.statutMembre] || 'badge-info' };
  const isParentCelibataire = form.situationFamiliale === 'PARENT_CELIBATAIRE';

  const infoRows = [
    { icon: Cake, label: t('member.age'), value: d.age != null ? `${d.age} ${t('member.years')}` : '—' },
    { icon: Calendar, label: t('member.churchArrival'), value: d.dateArriveeEglise ? new Date(d.dateArriveeEglise + 'T00:00:00').toLocaleDateString(locale, { day: 'numeric', month: 'long', year: 'numeric' }) : '—' },
    { icon: GraduationCap, label: t('member.education'), value: d.soul?.niveauEtude || '—' },
    { icon: Briefcase, label: t('member.profession'), value: d.soul?.profession || '—' },
    { icon: Heart, label: t('member.maritalStatus'), value: d.user.situationFamiliale ? dictionaries.label('SITUATION_FAMILIALE', d.user.situationFamiliale) || SITUATION_FALLBACK[d.user.situationFamiliale] || d.user.situationFamiliale : '—' },
    { icon: Users, label: t('member.children'), value: d.soul?.nbEnfants != null ? String(d.soul.nbEnfants) : '—' },
  ];

  return (
    <div className="page-container">
      {/* ===================== HEADER ===================== */}
      <div className="page-header">
        <div className="animate-fade-in">
          <div className="flex items-center gap-2 mb-1">
            <Sparkles className="w-5 h-5 text-primary-500" />
            <span className="text-sm font-medium text-primary-600 dark:text-primary-400 uppercase tracking-wider">
              {getGreeting(t)}, {d.user.firstName || t('member.dearMember')}
            </span>
          </div>
          <h1 className="page-title">
            {t('member.space')} <span className="text-gradient font-display">{t('member.member')}</span>
          </h1>
          <p className="page-subtitle">
            {t('member.subtitle')}
          </p>
        </div>
        {canManage && (
        <button onClick={openEdit} className="btn-glow btn-sm animate-scale-in">
          <Edit3 className="w-4 h-4" /> {t('member.editInfo')}
        </button>
        )}
      </div>

      {/* ===================== ACTIONS RAPIDES ===================== */}
      <div className="glass-card p-5 mb-6 animate-slide-up">
        <div className="flex items-center gap-2 mb-3"><Zap className="w-4 h-4 text-primary-500" /><h3 className="text-sm font-semibold text-gray-900 dark:text-gray-100">{t('member.quickAccess')}</h3></div>
        <div className="grid grid-cols-2 sm:grid-cols-4 gap-2">
          {[
            { href: '/events', icon: Calendar, label: t('member.events'), color: 'from-blue-500 to-indigo-500' },
            { href: '/prayers', icon: BookOpen, label: t('member.prayers'), color: 'from-indigo-500 to-violet-500' },
            { href: '/trainings', icon: GraduationCap, label: t('member.trainings'), color: 'from-emerald-500 to-teal-500' },
            { href: '/badges', icon: Trophy, label: t('member.badges'), color: 'from-amber-500 to-orange-500' },
          ].map((a) => (
            <Link key={a.href} to={a.href} className="flex items-center gap-2 p-2.5 rounded-xl hover:bg-gray-50 dark:hover:bg-gray-800/30 border border-gray-100 dark:border-gray-700/30 transition-all hover:shadow-md group">
              <div className={`w-8 h-8 rounded-lg bg-gradient-to-br ${a.color} text-white flex items-center justify-center shadow-sm group-hover:scale-110 transition-transform`}><a.icon className="w-4 h-4" /></div>
              <span className="text-xs font-medium text-gray-700 dark:text-gray-300">{a.label}</span>
            </Link>
          ))}
        </div>
      </div>

      {/* ===================== CARTE PROFIL ===================== */}
      <div className="glass-card p-6 mb-6 animate-slide-up overflow-hidden relative">
        <div className="absolute top-0 left-0 right-0 h-1 bg-gradient-to-r from-primary-500 via-primary-600 to-gold-400 opacity-60" />
        <div className="flex flex-col sm:flex-row items-center sm:items-start gap-6">
          {/* Avatar */}
          <div className="relative flex-shrink-0">
            {d.user.photoUrl ? (
              <img
                src={d.user.photoUrl}
                alt={t('member.profilePhoto')}
                className="w-24 h-24 rounded-2xl object-cover shadow-glow ring-2 ring-primary-500/30"
              />
            ) : (
              <div className="w-24 h-24 rounded-2xl bg-gradient-to-br from-primary-400 via-primary-500 to-primary-700 flex items-center justify-center shadow-glow">
                <span className="text-4xl font-bold text-white drop-shadow-sm">{initials(d.user)}</span>
              </div>
            )}
            <button
              onClick={openEdit}
              title={t('member.addChangePhoto')}
              className="absolute -bottom-2 -right-2 w-9 h-9 rounded-xl bg-white dark:bg-gray-800 border border-gray-200 dark:border-gray-700 shadow-md flex items-center justify-center hover:scale-110 transition-transform"
            >
              <Camera className="w-4 h-4 text-primary-600 dark:text-primary-400" />
            </button>
          </div>

          {/* Identité */}
          <div className="flex-1 text-center sm:text-left">
            <div className="flex flex-col sm:flex-row sm:items-center gap-2 sm:gap-3">
              <h2 className="text-2xl font-bold text-gray-900 dark:text-gray-100 font-display">
                {d.user.firstName} {d.user.lastName}
              </h2>
              <div className="flex justify-center sm:justify-start">
                <span className={`badge text-xs ${statut.badge}`}>{statut.label}</span>
              </div>
            </div>
            <div className="flex flex-col sm:flex-row sm:items-center gap-2 sm:gap-5 mt-2 text-sm text-gray-500 dark:text-gray-400">
              <span className="inline-flex items-center gap-1.5"><Mail className="w-3.5 h-3.5" /> {d.user.email}</span>
              {d.user.phone && <span className="inline-flex items-center gap-1.5"><Phone className="w-3.5 h-3.5" /> {d.user.phone}</span>}
            </div>
            <p className="text-xs text-gray-400 dark:text-gray-500 mt-3 max-w-xl leading-relaxed">
              {t('member.statutIs')} <strong className="text-gray-600 dark:text-gray-300">{statut.label.toLowerCase()}</strong>
              {d.estFaiseur
                ? t('member.youAccompany')
                : t('member.youAreAccompanied')}
            </p>
          </div>
        </div>
      </div>

      {/* ===================== INFORMATIONS ===================== */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6 mb-6">
        {/* Mon profil */}
        <div className="glass-card p-6 animate-slide-up lg:col-span-2">
          <div className="flex items-center justify-between mb-4">
            <div className="flex items-center gap-3">
              <div className="p-2.5 rounded-xl bg-gradient-to-br from-primary-500 to-primary-600 text-white shadow-lg">
                <User className="w-5 h-5" />
              </div>
              <h3 className="text-lg font-semibold text-gray-900 dark:text-gray-100">{t('member.myInfo')}</h3>
            </div>
            <span className="badge-info text-[10px]">{t('member.personalProfile')}</span>
          </div>
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
            {infoRows.map((row, i) => (
              <div
                key={row.label}
                className="flex items-center gap-3 p-3.5 rounded-xl bg-white/40 dark:bg-gray-800/30 border border-white/40 dark:border-white/[0.04] hover:border-primary-400/40 hover:shadow-sm transition-all animate-fade-in"
                style={{ animationDelay: `${i * 60}ms` }}
              >
                <div className="p-2 rounded-lg bg-primary-50 dark:bg-primary-900/20">
                  <row.icon className="w-4 h-4 text-primary-600 dark:text-primary-400" />
                </div>
                <div className="min-w-0">
                  <p className="text-[11px] text-gray-400 font-medium uppercase tracking-wider">{row.label}</p>
                  <p className="text-sm font-semibold text-gray-900 dark:text-gray-100 truncate">{row.value}</p>
                </div>
              </div>
            ))}
          </div>
        </div>

        {/* Ma famille de disciple */}
        <div className="glass-card p-6 animate-slide-up" style={{ animationDelay: '80ms' }}>
          <div className="flex items-center gap-3 mb-4">
            <div className="p-2.5 rounded-xl bg-gradient-to-br from-violet-500 to-purple-600 text-white shadow-lg">
              <Users className="w-5 h-5" />
            </div>
            <h3 className="text-lg font-semibold text-gray-900 dark:text-gray-100">{t('member.myFamily')}</h3>
          </div>
          {d.famille ? (
            <div className="space-y-3">
              <div className="p-4 rounded-xl bg-violet-50/70 dark:bg-violet-900/10 border border-violet-200/50 dark:border-violet-800/30">
                <p className="text-[11px] text-violet-500 font-medium uppercase tracking-wider">{t('member.family')}</p>
                <p className="text-lg font-bold text-gray-900 dark:text-gray-100 font-display mt-0.5">{d.famille.nom}</p>
              </div>
              <div className="flex items-center gap-3 p-3.5 rounded-xl bg-white/40 dark:bg-gray-800/30">
                <div className="p-2 rounded-lg bg-gold-100 dark:bg-gold-900/30">
                  <UserCheck className="w-4 h-4 text-gold-600 dark:text-gold-400" />
                </div>
                <div className="min-w-0">
                  <p className="text-[11px] text-gray-400 font-medium uppercase tracking-wider">{t('member.familyHead')}</p>
                  <p className="text-sm font-semibold text-gray-900 dark:text-gray-100 truncate">{d.famille.chefNom || '—'}</p>
                </div>
              </div>
            </div>
          ) : (
            <div className="flex flex-col items-center py-8 text-center">
              <Users className="w-8 h-8 text-gray-300 dark:text-gray-600 mb-2" />
              <p className="text-sm text-gray-500 dark:text-gray-400">{t('member.noFamily')}</p>
            </div>
          )}
        </div>

        {/* Mon encadrement */}
        <div className="glass-card p-6 animate-slide-up" style={{ animationDelay: '160ms' }}>
          <div className="flex items-center gap-3 mb-4">
            <div className="p-2.5 rounded-xl bg-gradient-to-br from-emerald-500 to-teal-600 text-white shadow-lg">
              <UserCheck className="w-5 h-5" />
            </div>
            <h3 className="text-lg font-semibold text-gray-900 dark:text-gray-100">{t('member.myGuidance')}</h3>
          </div>
          {d.faiseur ? (
            <div className="flex items-center gap-3 p-3.5 rounded-xl bg-white/40 dark:bg-gray-800/30 border border-white/40 dark:border-white/[0.04]">
              <div className="w-11 h-11 rounded-xl bg-gradient-to-br from-emerald-400 to-teal-600 flex items-center justify-center shadow">
                <span className="text-sm font-bold text-white">
                  {d.faiseur.nom.split(' ').map(w => w[0]).slice(0, 2).join('').toUpperCase()}
                </span>
              </div>
              <div className="min-w-0">
                <p className="text-[11px] text-gray-400 font-medium uppercase tracking-wider">{t('member.maker')}</p>
                <p className="text-sm font-semibold text-gray-900 dark:text-gray-100 truncate">{d.faiseur.nom}</p>
                <p className="text-[11px] text-gray-400">{t('member.yourGuide')}</p>
              </div>
            </div>
          ) : (
            <div className="flex flex-col items-center py-8 text-center">
              <UserCheck className="w-8 h-8 text-gray-300 dark:text-gray-600 mb-2" />
              <p className="text-sm text-gray-500 dark:text-gray-400">{t('member.noGuide')}</p>
            </div>
          )}
        </div>

        {/* Mes départements */}
        <div className="glass-card p-6 animate-slide-up lg:col-span-2" style={{ animationDelay: '240ms' }}>
          <div className="flex items-center gap-3 mb-4">
            <div className="p-2.5 rounded-xl bg-gradient-to-br from-amber-500 to-orange-600 text-white shadow-lg">
              <Building2 className="w-5 h-5" />
            </div>
            <h3 className="text-lg font-semibold text-gray-900 dark:text-gray-100">{t('member.myDepartments')}</h3>
            <span className="badge-warning text-[10px]">{d.departements.length} {t('member.active')}{d.departements.length > 1 ? 's' : ''}</span>
          </div>
          {d.departements.length > 0 ? (
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
              {d.departements.map((dept, i) => (
                <div
                  key={dept.id}
                  className="flex items-center gap-3 p-3.5 rounded-xl bg-white/40 dark:bg-gray-800/30 border border-white/40 dark:border-white/[0.04] hover:border-amber-400/40 hover:shadow-sm transition-all animate-fade-in"
                  style={{ animationDelay: `${i * 70}ms` }}
                >
                  <div className="p-2 rounded-lg bg-amber-100 dark:bg-amber-900/30">
                    <Church className="w-4 h-4 text-amber-600 dark:text-amber-400" />
                  </div>
                  <div className="min-w-0 flex-1">
                    <p className="text-sm font-semibold text-gray-900 dark:text-gray-100 truncate">{dept.nom}</p>
                    <p className="text-xs text-gray-400 truncate">
                      {dept.responsableNom ? `${t('member.responsable')} : ${dept.responsableNom}` : (dept.description || t('member.ministry'))}
                    </p>
                  </div>
                  <ChevronRight className="w-4 h-4 text-gray-300 dark:text-gray-600 flex-shrink-0" />
                </div>
              ))}
            </div>
          ) : (
            <div className="flex flex-col items-center py-8 text-center">
              <Building2 className="w-8 h-8 text-gray-300 dark:text-gray-600 mb-2" />
              <p className="text-sm text-gray-500 dark:text-gray-400">{t('member.noDepartment')}</p>
            </div>
          )}
        </div>

        {/* Mes présences hebdomadaires */}
        <div className="glass-card p-6 animate-slide-up lg:col-span-3" style={{ animationDelay: '320ms' }}>
          <div className="flex items-center gap-3 mb-1">
            <div className="p-2.5 rounded-xl bg-gradient-to-br from-sky-500 to-blue-600 text-white shadow-lg">
              <CalendarCheck className="w-5 h-5" />
            </div>
            <div>
              <h3 className="text-lg font-semibold text-gray-900 dark:text-gray-100">Mes présences hebdomadaires</h3>
              <p className="text-xs text-gray-400">Visibles par votre chef de famille, votre responsable de département et le pasteur</p>
            </div>
          </div>

          <div className="mt-4 grid grid-cols-1 lg:grid-cols-2 gap-5">
            {/* Saisie semaine courante */}
            <div className="p-4 rounded-2xl bg-white/40 dark:bg-gray-800/30 border border-white/40 dark:border-white/[0.04]">
              <p className="text-[11px] text-gray-400 font-medium uppercase tracking-wider mb-3">
                Semaine du{' '}
                {new Date(currentWeekMonday() + 'T00:00:00').toLocaleDateString(locale, { day: 'numeric', month: 'long' })}
              </p>

              {/* Programme (configuré par le pasteur) */}
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-2 mb-2">
                <div>
                  <label className="label">Programme</label>
                  <select
                    className="input"
                    value={presenceType}
                    onChange={(e) => {
                      setPresenceType(e.target.value);
                      setPresenceSousType('');
                    }}
                  >
                    <option value="">Choisir un programme...</option>
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
                      <option value="">Choisir...</option>
                      {sousTypes.map((st) => (
                        <option key={st.id} value={st.label}>
                          {st.label}{st.heureDebut ? ` · ${st.heureDebut.slice(0, 5)}` : ''}
                        </option>
                      ))}
                    </select>
                  </div>
                )}
              </div>

              {/* Programmes de la semaine cochables */}
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-2">
                {programTypes.map((pt) => (
                  <label
                    key={pt.id}
                    className="flex items-center gap-2.5 p-2.5 rounded-xl bg-white/60 dark:bg-gray-900/40 border border-white/40 dark:border-white/[0.05] cursor-pointer hover:border-primary-400/50 hover:shadow-sm transition-all"
                  >
                    <input
                      type="checkbox"
                      checked={!!presenceForm[pt.label]}
                      onChange={(e) => setPresenceForm((f) => ({ ...f, [pt.label]: e.target.checked }))}
                      className="w-4 h-4 rounded accent-primary-500"
                    />
                    <span className="text-sm text-gray-700 dark:text-gray-200">{pt.label}</span>
                  </label>
                ))}
              </div>
              <textarea
                className="input mt-3"
                rows={2}
                placeholder="Notes (facultatif)"
                value={presenceNotes}
                onChange={(e) => setPresenceNotes(e.target.value)}
              />
            {canManage && (
            <button
              onClick={submitPresence}
              disabled={presenceMutation.isPending}
              className="btn-primary btn-sm mt-3 w-full"
            >
              {presenceMutation.isPending ? <Loader2 className="w-4 h-4 animate-spin" /> : <Save className="w-4 h-4" />}
              {presenceMutation.isPending ? 'Enregistrement...' : 'Enregistrer ma présence'}
            </button>
            )}
            </div>

            {/* Historique */}
            <div>
              <p className="text-[11px] text-gray-400 font-medium uppercase tracking-wider mb-3">
                Mon historique ({presences.length})
              </p>
              {presences.length > 0 ? (
                <div className="space-y-2 max-h-72 overflow-y-auto pr-1">
                  {presences.map((p) => {
                    const t = tauxPresence(p);
                    const presents = Object.entries(p.presences || {}).filter(([, v]) => v).length;
                    return (
                      <div
                        key={p.id}
                        className="flex items-center justify-between p-3 rounded-xl bg-white/40 dark:bg-gray-800/30 border border-white/40 dark:border-white/[0.04] animate-fade-in"
                      >
                        <div className="min-w-0">
                          <p className="text-sm font-semibold text-gray-900 dark:text-gray-100">
                            {new Date(p.semaine + 'T00:00:00').toLocaleDateString(locale, { day: 'numeric', month: 'long' })}
                          </p>
                          <p className="text-[11px] text-gray-400 truncate">
                            {presents} / {Object.keys(p.presences || {}).length} programmes présents
                          </p>
                        </div>
                        <span className={`badge text-xs ${t >= 70 ? 'badge-success' : t >= 40 ? 'badge-warning' : 'badge-error'}`}>
                          {t}%
                        </span>
                      </div>
                    );
                  })}
                </div>
              ) : (
                <div className="flex flex-col items-center py-8 text-center">
                  <CalendarCheck className="w-8 h-8 text-gray-300 dark:text-gray-600 mb-2" />
                  <p className="text-sm text-gray-500 dark:text-gray-400">Aucune présence enregistrée pour l'instant</p>
                  <p className="text-xs text-gray-400">Cochez les programmes de la semaine et enregistrez</p>
                </div>
              )}
            </div>
          </div>
        </div>
      </div>

      {/* ===================== SUGGESTIONS & RENDEZ-VOUS ===================== */}
      <div className="glass-card p-6 animate-slide-up mb-6">
        <div className="flex items-center gap-3 mb-1">
          <div className="p-2.5 rounded-xl bg-gradient-to-br from-emerald-500 to-teal-600 text-white shadow-lg">
            <MessageSquare className="w-5 h-5" />
          </div>
          <div>
            <h3 className="text-lg font-semibold text-gray-900 dark:text-gray-100">Suggestions, rendez-vous & signalements</h3>
            <p className="text-xs text-gray-400">Envoyez un message au pasteur, à votre responsable de département ou à votre chef de famille</p>
          </div>
        </div>

        <div className="mt-4 grid grid-cols-1 lg:grid-cols-5 gap-5">
          {/* Formulaire */}
          <div className="lg:col-span-2 p-4 rounded-2xl bg-white/40 dark:bg-gray-800/30 border border-white/40 dark:border-white/[0.04]">
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
              <div>
                <label className="label">Type</label>
                <select
                  className="input"
                  value={requestForm.type}
                  onChange={(e) => setRequestForm({ ...requestForm, type: e.target.value as MemberRequestType })}
                >
                  {(dictionaries.selectOptions('MEMBER_REQUEST_TYPE').length > 0
                    ? dictionaries.selectOptions('MEMBER_REQUEST_TYPE')
                    : Object.entries(TYPE_LABELS).map(([value, label]) => ({ value, label }))
                  ).map((o) => (
                    <option key={o.value} value={o.value}>{o.label}</option>
                  ))}
                </select>
              </div>
              <div>
                <label className="label">Destinataire</label>
                <select
                  className="input"
                  value={requestForm.cible}
                  onChange={(e) => setRequestForm({ ...requestForm, cible: e.target.value as MemberRequestTarget })}
                >
                  {(dictionaries.selectOptions('MEMBER_REQUEST_TARGET').length > 0
                    ? dictionaries.selectOptions('MEMBER_REQUEST_TARGET')
                    : Object.entries(CIBLE_FALLBACK).map(([value, label]) => ({ value, label }))
                  ).map((o) => (
                    <option key={o.value} value={o.value}>{o.label}</option>
                  ))}
                </select>
              </div>
            </div>
            <textarea
              className="input mt-3"
              rows={4}
              placeholder="Votre message..."
              value={requestForm.message}
              onChange={(e) => setRequestForm({ ...requestForm, message: e.target.value })}
            />
            {canAccessFilesModule(user?.role) && (
              <div className="mt-3">
                <label className="label flex items-center gap-1.5">
                  <Paperclip className="w-3.5 h-3.5 text-primary-500" /> Pièces jointes
                </label>
                <AttachmentPicker
                  value={requestForm.fichierIds}
                  onChange={(ids) => setRequestForm({ ...requestForm, fichierIds: ids })}
                />
              </div>
            )}
            {canManage && (
            <button
              onClick={submitRequest}
              disabled={requestMutation.isPending || !requestForm.message.trim()}
              className="btn-primary btn-sm mt-3 w-full"
            >
              {requestMutation.isPending ? <Loader2 className="w-4 h-4 animate-spin" /> : <Send className="w-4 h-4" />}
              {requestMutation.isPending ? 'Envoi...' : 'Envoyer ma demande'}
            </button>
            )}
          </div>

          {/* Mes demandes */}
          <div className="lg:col-span-3">
            <p className="text-[11px] text-gray-400 font-medium uppercase tracking-wider mb-3">
              Mes demandes ({myRequests.length})
            </p>
            {myRequests.length > 0 ? (
              <div className="space-y-2 max-h-96 overflow-y-auto pr-1">
                {myRequests.map((r) => (
                  <div
                    key={r.id}
                    className="p-3.5 rounded-xl bg-white/40 dark:bg-gray-800/30 border border-white/40 dark:border-white/[0.04] animate-fade-in"
                  >
                    <div className="flex items-center justify-between gap-2 flex-wrap">
                      <div className="flex items-center gap-1.5 flex-wrap">
                        <span className="badge text-[10px] badge-info">{dictionaries.label('MEMBER_REQUEST_TYPE', r.type) || TYPE_LABELS[r.type] || r.type}</span>
                        <span className="badge text-[10px] badge-warning">{dictionaries.label('MEMBER_REQUEST_TARGET', r.cible) || CIBLE_FALLBACK[r.cible] || r.cible}</span>
                        <span className={`badge text-[10px] ${STATUS_BADGES[r.statut] || 'badge-warning'}`}>
                          {dictionaries.label('MEMBER_REQUEST_STATUS', r.statut) || STATUS_FALLBACK[r.statut] || r.statut}
                        </span>
                      </div>
                      <span className="text-[10px] text-gray-400">
                        {new Date(r.createdAt).toLocaleDateString(locale, { day: 'numeric', month: 'short' })}
                      </span>
                    </div>
                    <p className="text-sm text-gray-700 dark:text-gray-200 mt-2 leading-relaxed">{r.message}</p>
                    {r.piecesJointes && r.piecesJointes.length > 0 && (
                      <div className="mt-2">
                        <AttachmentLinks pieces={r.piecesJointes} />
                      </div>
                    )}
                    {r.reponse && (
                      <div className="mt-2 p-2.5 rounded-lg bg-emerald-50/70 dark:bg-emerald-900/10 border border-emerald-200/50 dark:border-emerald-800/30 text-sm text-emerald-800 dark:text-emerald-300">
                        <strong>Réponse :</strong> {r.reponse}
                      </div>
                    )}
                  </div>
                ))}
              </div>
            ) : (
              <div className="flex flex-col items-center py-8 text-center">
                <MessageSquare className="w-8 h-8 text-gray-300 dark:text-gray-600 mb-2" />
                <p className="text-sm text-gray-500 dark:text-gray-400">Aucune demande envoyée</p>
                <p className="text-xs text-gray-400">Vos demandes apparaîtront ici avec leur statut et la réponse</p>
              </div>
            )}
          </div>
        </div>
      </div>

      {/* ===================== ÉVÉNEMENTS À VENIR ===================== */}
      <div className="glass-card p-6 animate-slide-up mb-6">
        <div className="flex items-center justify-between mb-4">
          <div className="flex items-center gap-3">
            <div className="p-2.5 rounded-xl bg-gradient-to-br from-blue-500 to-indigo-600 text-white shadow-lg">
              <Calendar className="w-5 h-5" />
            </div>
            <div>
              <h3 className="text-lg font-semibold text-gray-900 dark:text-gray-100">{t('member.upcomingEvents')}</h3>
              <p className="text-xs text-gray-400">{t('member.communityEvents')}</p>
            </div>
          </div>
          <Link to="/events" className="text-[10px] font-medium text-primary-600">Voir tout</Link>
        </div>
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-3">
          {((dashboard as any)?.evenements?.slice(0, 6) || []).map((ev: any) => (
            <Link key={ev.id} to="/events" className="flex items-center gap-3 p-3 rounded-xl hover:bg-gray-50/50 dark:hover:bg-gray-800/30 transition-colors group">
              <div className="w-10 h-10 rounded-xl bg-blue-50 dark:bg-blue-900/20 flex items-center justify-center flex-shrink-0">
                <Calendar className="w-5 h-5 text-blue-500" />
              </div>
              <div className="min-w-0 flex-1">
                <p className="text-xs font-medium text-gray-900 dark:text-gray-100 truncate group-hover:text-primary-600">{ev.titre}</p>
                <p className="text-[10px] text-gray-400">{ev.dateDebut ? new Date(ev.dateDebut).toLocaleDateString(locale, { day: 'numeric', month: 'short' }) : '—'}{ev.lieu ? ` · ${ev.lieu}` : ''}</p>
              </div>
              <ChevronRight className="w-3 h-3 text-gray-400 opacity-0 group-hover:opacity-100 transition-opacity" />
            </Link>
          ))}
          {(!(dashboard as any)?.evenements || ((dashboard as any)).evenements.length === 0) && (
            <div className="col-span-full text-center py-6"><Calendar className="w-8 h-8 text-gray-300 mx-auto mb-2" /><p className="text-xs text-gray-400">{t('member.noEvent')}</p></div>
          )}
        </div>
      </div>

      {/* ===================== PRIÈRES & ACTIONS DE GRÂCE ===================== */}
      <div className="glass-card p-6 animate-slide-up mb-6">
        <div className="flex items-center justify-between mb-4">
          <div className="flex items-center gap-3">
            <div className="p-2.5 rounded-xl bg-gradient-to-br from-indigo-500 to-violet-600 text-white shadow-lg">
              <Church className="w-5 h-5" />
            </div>
            <div>
              <h3 className="text-lg font-semibold text-gray-900 dark:text-gray-100">{t('member.prayersThanks')}</h3>
              <p className="text-xs text-gray-400">{t('member.prayersHint')}</p>
            </div>
          </div>
          <Link to="/prayers" className="text-[10px] font-medium text-primary-600">Voir tout</Link>
        </div>
        <div className="space-y-2">
          {((dashboard as any)?.prieres?.slice(0, 5) || []).map((p: any) => (
            <div key={p.id} className="flex items-center gap-3 p-3 rounded-xl hover:bg-gray-50/50 dark:hover:bg-gray-800/30 transition-colors">
              <div className={`w-8 h-8 rounded-lg flex items-center justify-center flex-shrink-0 ${p.statut === 'EXAUCEE' ? 'bg-green-50 dark:bg-green-900/20' : 'bg-indigo-50 dark:bg-indigo-900/20'}`}>
                {p.statut === 'EXAUCEE' ? <CheckCircle className="w-4 h-4 text-green-500" /> : <Church className="w-4 h-4 text-indigo-500" />}
              </div>
              <div className="min-w-0 flex-1">
                <p className="text-xs font-medium text-gray-900 dark:text-gray-100 truncate">{p.titre}</p>
                <p className="text-[10px] text-gray-400">{p.priorite} · {p.visibilite} · {p.auteurNom || '—'}</p>
              </div>
              <span className={`text-[9px] font-semibold px-1.5 py-0.5 rounded-full ${p.statut === 'EXAUCEE' ? 'bg-green-100 dark:bg-green-900/30 text-green-700 dark:text-green-400' : 'bg-amber-100 dark:bg-amber-900/30 text-amber-700 dark:text-amber-400'}`}>
                {p.statut === 'EXAUCEE' ? 'Exaucée' : 'En cours'}
              </span>
            </div>
          ))}
          {(!(dashboard as any)?.prieres || ((dashboard as any)).prieres.length === 0) && (
            <div className="text-center py-6"><Church className="w-8 h-8 text-gray-300 mx-auto mb-2" /><p className="text-xs text-gray-400">Aucune prière</p></div>
          )}
        </div>
      </div>

      {/* ===================== PROGRESSION SPIRITUELLE ===================== */}
      {(dashboard as any)?.progression && (
        <div className="glass-card p-6 animate-slide-up mb-6">
          <div className="flex items-center gap-3 mb-4">
            <div className="p-2.5 rounded-xl bg-gradient-to-br from-amber-500 to-orange-600 text-white shadow-lg">
              <TrendingUp className="w-5 h-5" />
            </div>
            <div>
              <h3 className="text-lg font-semibold text-gray-900 dark:text-gray-100">Ma progression</h3>
              <p className="text-xs text-gray-400">Évolution spirituelle et engagement</p>
            </div>
          </div>
          <div className="grid grid-cols-2 sm:grid-cols-4 gap-3">
            {((dashboard as any)).progression.niveauCroissance !== undefined && (
              <div className="p-3 rounded-xl bg-amber-50/50 dark:bg-amber-900/10 text-center">
                <div className="flex justify-center gap-0.5 mb-1">{[1,2,3,4,5].map(i => <Star key={i} className={`w-3.5 h-3.5 ${i <= ((dashboard as any)).progression.niveauCroissance ? 'fill-amber-400 text-amber-400' : 'text-gray-300 dark:text-gray-600'}`} />)}</div>
                <p className="text-[10px] text-gray-400">Niveau spiritual</p>
              </div>
            )}
            {((dashboard as any)).progression.presencesTotal !== undefined && (
              <div className="p-3 rounded-xl bg-blue-50/50 dark:bg-blue-900/10 text-center">
                <p className="text-xl font-bold text-blue-600 dark:text-blue-400">{((dashboard as any)).progression.presencesTotal}</p>
                <p className="text-[10px] text-gray-400">Présences</p>
              </div>
            )}
            {((dashboard as any)).progression.rapportsSoumis !== undefined && (
              <div className="p-3 rounded-xl bg-green-50/50 dark:bg-green-900/10 text-center">
                <p className="text-xl font-bold text-green-600 dark:text-green-400">{((dashboard as any)).progression.rapportsSoumis}</p>
                <p className="text-[10px] text-gray-400">Rapports</p>
              </div>
            )}
            {((dashboard as any)).progression.prieres !== undefined && (
              <div className="p-3 rounded-xl bg-indigo-50/50 dark:bg-indigo-900/10 text-center">
                <p className="text-xl font-bold text-indigo-600 dark:text-indigo-400">{((dashboard as any)).progression.prieres}</p>
                <p className="text-[10px] text-gray-400">Prières</p>
              </div>
            )}
          </div>
        </div>
      )}

      {/* ===================== PHASE 3 : ÉVÉNEMENTS À VENIR ===================== */}
      {myEvents.length > 0 && (
        <div className="glass-card p-5 mb-6 animate-slide-up">
          <div className="flex items-center justify-between mb-4">
            <div className="flex items-center gap-2">
              <Calendar className="w-4 h-4 text-blue-500" />
              <h3 className="text-sm font-semibold text-gray-900 dark:text-gray-100">Événements à venir</h3>
            </div>
            <Link to="/events" className="text-[10px] font-medium text-primary-600">Voir tout <ChevronRight className="w-3 h-3 inline" /></Link>
          </div>
          <div className="space-y-2">
            {myEvents.slice(0, 4).map((ev: any) => (
              <div key={ev.id} className="flex items-center gap-3 p-2.5 rounded-xl hover:bg-gray-50 dark:hover:bg-gray-800/40 transition-all">
                <div className="p-2 rounded-lg bg-blue-50 dark:bg-blue-900/20 text-blue-600 dark:text-blue-400">
                  <Calendar className="w-4 h-4" />
                </div>
                <div className="min-w-0 flex-1">
                  <p className="text-sm font-medium text-gray-800 dark:text-gray-200 truncate">{ev.titre}</p>
                  <p className="text-[10px] text-gray-400">{ev.dateDebut ? new Date(ev.dateDebut).toLocaleDateString(locale, { day: 'numeric', month: 'short' }) : '—'}{ev.lieu ? ` · ${ev.lieu}` : ''}</p>
                </div>
              </div>
            ))}
          </div>
        </div>
      )}

      {/* ===================== PHASE 3 : NOTES DU FAISEUR ===================== */}
      {myNotes.length > 0 && (
        <div className="glass-card p-5 mb-6 animate-slide-up">
          <div className="flex items-center gap-2 mb-4">
            <BookOpen className="w-4 h-4 text-indigo-500" />
            <h3 className="text-sm font-semibold text-gray-900 dark:text-gray-100">Notes de mon faiseur</h3>
          </div>
          <div className="space-y-2">
            {myNotes.slice(0, 3).map((n: any, idx: number) => (
              <div key={idx} className="p-3 rounded-xl bg-white/40 dark:bg-gray-800/30 border border-white/40 dark:border-white/[0.04]">
                <div className="flex items-center gap-2 mb-1">
                  <span className="badge text-[10px] badge-info">{n.type || 'Note'}</span>
                  <span className="text-[10px] text-gray-400">{n.createdAt ? new Date(n.createdAt).toLocaleDateString(locale) : ''}</span>
                </div>
                <p className="text-sm text-gray-700 dark:text-gray-300">{n.contenu || n.texte || ''}</p>
              </div>
            ))}
          </div>
        </div>
      )}

      {/* ===================== MODAL D'ÉDITION ===================== */}
      {editing && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
          <div className="absolute inset-0 bg-black/50 backdrop-blur-sm" onClick={() => setEditing(false)} />
          <div className="relative w-full max-w-2xl max-h-[90vh] overflow-y-auto glass-card !bg-white/95 dark:!bg-gray-900/95 p-6 animate-scale-in">
            <div className="flex items-center justify-between mb-6">
              <h3 className="text-xl font-bold text-gray-900 dark:text-gray-100 font-display">
                Modifier mes informations
              </h3>
              <button onClick={() => setEditing(false)} className="p-2 rounded-xl hover:bg-gray-100 dark:hover:bg-gray-800 transition-colors">
                <X className="w-5 h-5 text-gray-400" />
              </button>
            </div>

            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
              {/* Photo */}
              <div className="sm:col-span-2">
                <label className="label">Photo de profil (lien, facultatif)</label>
                <div className="flex items-center gap-4">
                  <div className="w-16 h-16 rounded-xl overflow-hidden flex-shrink-0 bg-gray-100 dark:bg-gray-800 flex items-center justify-center">
                    {form.photoUrl ? (
                      <img src={form.photoUrl} alt="Aperçu" className="w-full h-full object-cover" />
                    ) : (
                      <Camera className="w-5 h-5 text-gray-400" />
                    )}
                  </div>
                  <input
                    className="input"
                    placeholder="https://exemple.com/ma-photo.jpg"
                    value={form.photoUrl || ''}
                    onChange={(e) => setForm({ ...form, photoUrl: e.target.value })}
                  />
                </div>
              </div>

              <div>
                <label className="label">Téléphone</label>
                <input
                  className="input"
                  placeholder="06 00 00 00 00"
                  value={form.phone || ''}
                  onChange={(e) => setForm({ ...form, phone: e.target.value })}
                />
              </div>

              <div>
                <label className="label">Date de naissance</label>
                <input
                  type="date"
                  className="input"
                  value={form.dateNaissance || ''}
                  onChange={(e) => setForm({ ...form, dateNaissance: e.target.value })}
                />
              </div>

              <div>
                <label className="label">Situation familiale</label>
                <select
                  className="input"
                  value={form.situationFamiliale || ''}
                  onChange={(e) => setForm({ ...form, situationFamiliale: e.target.value })}
                >
                  <option value="">Sélectionner...</option>
                  {(dictionaries.selectOptions('SITUATION_FAMILIALE').length > 0
                    ? dictionaries.selectOptions('SITUATION_FAMILIALE')
                    : Object.entries(SITUATION_FALLBACK).map(([value, label]) => ({ value, label }))
                  ).map((o) => (
                    <option key={o.value} value={o.value}>{o.label}</option>
                  ))}
                </select>
              </div>

              {isParentCelibataire ? (
                <div>
                  <label className="label">Nombre d'enfants</label>
                  <input
                    type="number"
                    min={0}
                    max={50}
                    className="input"
                    value={form.nbEnfants ?? 0}
                    onChange={(e) => setForm({ ...form, nbEnfants: Number(e.target.value) })}
                  />
                </div>
              ) : (
                <div>
                  <label className="label">Profession</label>
                  <input
                    className="input"
                    placeholder="Votre métier ou activité"
                    value={form.profession || ''}
                    onChange={(e) => setForm({ ...form, profession: e.target.value })}
                  />
                </div>
              )}

              <div className="sm:col-span-2">
                <label className="label">Niveau d'étude</label>
                <select
                  className="input"
                  value={form.niveauEtude || ''}
                  onChange={(e) => setForm({ ...form, niveauEtude: e.target.value })}
                >
                  <option value="">Sélectionner...</option>
                  {NIVEAU_ETUDES.map((n) => (
                    <option key={n} value={n}>{n}</option>
                  ))}
                </select>
              </div>
            </div>

            <div className="flex justify-end gap-3 mt-8 pt-5 border-t border-white/20 dark:border-white/[0.06]">
              <button onClick={() => setEditing(false)} className="btn-secondary btn-sm">
                <X className="w-4 h-4" /> Annuler
              </button>
              <button onClick={submit} disabled={mutation.isPending} className="btn-primary btn-sm">
                {mutation.isPending ? <Loader2 className="w-4 h-4 animate-spin" /> : <Save className="w-4 h-4" />}
                Enregistrer
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
