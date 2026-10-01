import { useState, useMemo } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import toast from 'react-hot-toast';
import api, { getErrorMessage } from '@/lib/api';
import axios from 'axios';
import {
  Plus, Pencil, Building2, Loader2, Save, Globe, Calendar,
  Search, Filter, Users, Activity, Eye, AlertTriangle,
  Shield, X, RefreshCw, BarChart3, Ban, RotateCcw, CheckCircle2,
  // PORT Develop1 (§57 / §G1.9) — archivage et impersonation super admin.
  Archive, UserCheck,
} from 'lucide-react';
import type { Tenant, TenantStatus } from '@/types';
import { QuotaUsageCards } from '@/components/admin/QuotaUsageCards';
import { normalizeQuotaUsage } from '@/types/quota';
import { EmptyState, SkeletonDashboard, VisuallyHidden } from '@/components/ui/UXComponents';
import { useImpersonationOptional } from '@/contexts/ImpersonationContext';
import { useAuthOptional } from '@/contexts/AuthContext';
import { isPlatformAdmin } from '@/workspaces';

import { getI18nLocale } from '@/i18n';
import { tText } from '@/i18n';
const STATUS_OPTIONS: { value: TenantStatus; label: string; color: string; dot: string }[] = [
  { value: 'ACTIVE', label: 'Active', color: 'badge-success', dot: 'bg-green-500' },
  { value: 'SUSPENDED', label: 'Suspendue', color: 'badge-warning', dot: 'bg-amber-500' },
  { value: 'CANCELLED', label: 'Annulée', color: 'badge-error', dot: 'bg-red-500' },
  { value: 'PENDING_SETUP', label: 'En attente', color: 'badge-info', dot: 'bg-blue-500' },
];

/**
 * Plans SaaS — source de vérité : `GET /api/v1/platform/admin/plans`
 * (`SuperAdminController.listPlans()`, ligne 462, protégé par isPlatformSuperAdmin).
 *
 * Le fallback ci-dessous est le jeu de clés CANONIQUE seedé par
 * `V144__seed_saas_plans.sql` puis purgé par `V177__complete_canonical_saas_plans.sql`
 * (`WHERE key NOT IN ('DISCOVERY','STARTUP','GROWTH','NETWORK')`).
 * Il ne sert QU'À éviter un écran vide si l'appel échoue ; en ce cas un avertissement
 * visible est affiché (un fallback silencieux serait un mensonge).
 */
const CANONICAL_PLAN_KEYS = ['DISCOVERY', 'STARTUP', 'GROWTH', 'NETWORK'] as const;

interface PlanOption {
  value: string;
  label: string;
  badge: string;
  color: string;
}

const PLAN_BADGES = ['badge-gray', 'badge-info', 'badge-primary', 'badge-purple'];

/**
 * Couleurs décoratives attribuées par position dans le catalogue renvoyé par le
 * backend. Elles n'ont aucune valeur sémantique : un plan n'est « mieux » qu'un
 * autre sous prétexte de couleur. Elles tournent simplement (modulo) pour que la
 * carte de répartition reste lisible quel que soit le nombre de plans publiés.
 */
const PLAN_TEXT_COLORS = [
  'text-gray-700 dark:text-gray-200',
  'text-blue-600 dark:text-blue-400',
  'text-primary-600 dark:text-primary-400',
  'text-purple-600 dark:text-purple-400',
];

const buildPlanOptions = (payload: unknown): { options: PlanOption[]; usedFallback: boolean } => {
  const rows = Array.isArray(payload)
    ? payload
    : Array.isArray((payload as { content?: unknown } | null)?.content)
      ? (payload as { content: unknown[] }).content
      : [];

  const parsed = rows.reduce<PlanOption[]>((acc, row) => {
    const record = (row ?? {}) as Record<string, unknown>;
    const key = typeof record.key === 'string' && record.key.trim() !== '' ? record.key : null;
    if (!key) return acc;
    const name = typeof record.name === 'string' && record.name.trim() !== '' ? record.name : key;
    acc.push({ value: key, label: name, badge: 'badge-gray', color: 'text-gray-700 dark:text-gray-200' });
    return acc;
  }, []);

  const usedFallback = parsed.length === 0;
  const source = usedFallback
    ? CANONICAL_PLAN_KEYS.map((key) => ({
        value: key,
        label: key,
        badge: 'badge-gray',
        color: 'text-gray-700 dark:text-gray-200',
      }))
    : parsed;

  return {
    usedFallback,
    options: source.map((option, index) => ({
      ...option,
      badge: PLAN_BADGES[index % PLAN_BADGES.length],
      color: PLAN_TEXT_COLORS[index % PLAN_TEXT_COLORS.length],
    })),
  };
};

/**
 * Un tenant porte toujours un plan, mais le backend peut introduire une clé que
 * cette version du front ne connaît pas. On affiche alors la clé BRUTE : retomber
 * sur le premier plan de la liste afficherait « Free » pour une clé inconnue,
 * c'est-à-dire un mensonge visible.
 */
const planInfoFor = (plan: string, options: PlanOption[]): PlanOption => {
  const known = options.find((option) => option.value === plan);
  return known ?? { value: plan, label: plan, badge: 'badge-gray', color: 'text-gray-700 dark:text-gray-200' };
};

/** Même principe côté statut : une valeur inconnue est affichée brute, jamais « Active ». */
const statusInfoFor = (status: string): { label: string; color: string; dot: string } => {
  const known = STATUS_OPTIONS.find((option) => option.value === status);
  return known ?? { label: status, color: 'badge-gray', dot: 'bg-gray-400' };
};

interface TenantForm {
  name: string;
  slug: string;
  plan: string;
  status?: TenantStatus;
}

const EMPTY_FORM: TenantForm = { name: '', slug: '', plan: '' };

export default function AdminTenantsPage() {
  const queryClient = useQueryClient();
  // PORT Develop1 — impersonation (§G1.9) : réservée au super admin plateforme.
  // Consommation tolérante : hors AuthProvider/ImpersonationProvider (tests
  // unitaires isolés, rendus autonomes), le bloc s'efface sans lever.
  const authUser = useAuthOptional()?.user;
  const startImpersonation = useImpersonationOptional()?.startImpersonation;
  const isSuperAdmin = isPlatformAdmin(authUser?.role);
  const [modalOpen, setModalOpen] = useState(false);
  const [editId, setEditId] = useState<string | null>(null);
  const [form, setForm] = useState<TenantForm>(EMPTY_FORM);
  const [searchTerm, setSearchTerm] = useState('');
  const [statusFilter, setStatusFilter] = useState<TenantStatus | ''>('');
  const [planFilter, setPlanFilter] = useState('');
  const [detailTenant, setDetailTenant] = useState<Tenant | null>(null);

  const {
    data: tenants = [],
    isLoading,
    isError: tenantsFailed,
    error: tenantsError,
    refetch,
  } = useQuery({
    queryKey: ['admin', 'tenants'],
    queryFn: async () => {
      const res = await api.get('/tenants');
      return res.data as Tenant[];
    },
    retry: false,
  });

  /**
   * 403 sur une page super-admin = session expirée ou rôle révoqué, pas un bug.
   * Le message le dit explicitement pour ne pas envoyer l'utilisateur chercher
   * un défaut front qui n'existe pas.
   */
  const describeError = (error: unknown): string => {
    const status = axios.isAxiosError(error) ? error.response?.status : undefined;
    if (status === 403) {
      return tText('Accès refusé : votre session a expiré ou votre rôle super-admin a été révoqué.');
    }
    return getErrorMessage(error);
  };

  // ── Plans SaaS : source de vérité = backend ──────────────────────────────
  const { data: planPayload, isError: plansFailed } = useQuery({
    queryKey: ['platform', 'admin', 'plans'],
    queryFn: async () => {
      const res = await api.get('/platform/admin/plans');
      return res.data as unknown;
    },
    // Un 403 sur cette page signifie session expirée ou rôle révoqué : on ne
    // réessaie pas (retry:false) pour éviter de marteler l'API.
    retry: false,
  });

  const { options: planOptions, usedFallback: plansFallback } = useMemo(
    () => buildPlanOptions(planPayload),
    [planPayload],
  );

  // ── Usage réel des tenants (remplace l'appel mort à /admin/system-health) ──
  // `GET /api/v1/platform/admin/quota-usage/tenants` (PlatformQuotaUsageController:38)
  // renvoie un `PageResponse<TenantUsageOverview>` : 1 requête pour tout le tableau,
  // là où un appel par tenant en ferait N. L'échec est isolé : le tableau s'affiche
  // quand même et affiche « — » (jamais 0, qui serait un mensonge).
  interface TenantUsageOverview {
    tenantId: string;
    users?: number;
    aiCredits?: number;
    storageBytes?: number;
    subscriptionStatus?: string | null;
  }

  const { data: usageByTenant } = useQuery({
    queryKey: ['platform', 'admin', 'quota-usage', 'tenants'],
    queryFn: async () => {
      const res = await api.get('/platform/admin/quota-usage/tenants', {
        params: { page: 0, size: 100 },
      });
      const content = (res.data as { content?: unknown })?.content;
      return Array.isArray(content) ? (content as TenantUsageOverview[]) : [];
    },
    retry: false,
  });

  const usageIndex = useMemo(() => {
    const index = new Map<string, TenantUsageOverview>();
    (usageByTenant ?? []).forEach((row) => {
      if (row && typeof row.tenantId === 'string') index.set(row.tenantId, row);
    });
    return index;
  }, [usageByTenant]);

  // Détail : snapshot complet d'un tenant (limites + application côté serveur).
  // `GET /api/v1/platform/admin/quota-usage/tenants/{tenantId}`
  // (PlatformQuotaUsageController:33). La réponse est déjà au format attendu par
  // `normalizeQuotaUsage`, donc on RÉUTILISE QuotaUsageCards au lieu d'en écrire
  // un second (§5.0.1 du plan).
  const { data: detailUsage, isLoading: detailUsageLoading } = useQuery({
    queryKey: ['platform', 'admin', 'quota-usage', 'tenant', detailTenant?.id],
    queryFn: async () => {
      const res = await api.get(`/platform/admin/quota-usage/tenants/${detailTenant?.id}`);
      return normalizeQuotaUsage(res.data);
    },
    enabled: !!detailTenant,
    retry: false,
  });

  const invalidate = () => {
    queryClient.invalidateQueries({ queryKey: ['admin', 'tenants'] });
    queryClient.invalidateQueries({ queryKey: ['platform', 'admin', 'quota-usage'] });
  };

  /**
   * ⚠️ `DELETE /api/v1/tenants/{id}` appelle `TenantService.deactivate(id)` côté
   * backend (TenantController:68) : ce n'est PAS une suppression. L'ancienne UI
   * annonçait « Supprimer l'église » puis « action irréversible », ce qui était
   * faux. On parle donc de suspension, et la réactivation est proposée en miroir.
   */
  const suspendMutation = useMutation({
    mutationFn: async (id: string) => api.delete(`/tenants/${id}`),
    onSuccess: () => {
      invalidate();
      setDetailTenant(null);
      toast.success(tText('Église suspendue'));
    },
    onError: (err: unknown) => toast.error(describeError(err)),
  });

  const reactivateMutation = useMutation({
    mutationFn: async (id: string) => api.post(`/tenants/${id}/reactivate`),
    onSuccess: () => {
      invalidate();
      setDetailTenant(null);
      toast.success(tText('Église réactivée'));
    },
    onError: (err: unknown) => toast.error(describeError(err)),
  });

  /**
   * PORT Develop1 (§57) — cycle de vie super admin via les endpoints dedies
   * `POST /platform/admin/tenants/{id}/archive` (SuperAdminController:317, statut
   * CANCELLED + journalisation serveur). Les suspend/réactiver de main restent
   * les leurs : rien n'est deplace, seulement ajoute.
   */
  const archiveMutation = useMutation({
    mutationFn: async (id: string) => api.post(`/platform/admin/tenants/${id}/archive`),
    onSuccess: () => {
      invalidate();
      setDetailTenant(null);
      toast.success(tText('Église archivée'));
    },
    onError: (err: unknown) => toast.error(describeError(err)),
  });

  // PORT Develop1 (§G1.9) — impersonation depuis la fiche tenant : motif
  // obligatoire, la demande est journalisee cote serveur.
  const impersonateTenant = async (tenant: Tenant) => {
    const email = window.prompt(
      tText('Email du membre à impersoner dans {name}').replace('{name}', tenant.name),
    );
    if (!email || !email.trim()) return;
    const reason = window.prompt(tText("Motif de l'impersonation (obligatoire, journalisé) :"));
    if (!reason || !reason.trim()) {
      toast.error(tText('Un motif est requis pour impersoner'));
      return;
    }
    await startImpersonation?.(email.trim(), reason.trim(), tenant.id);
  };

  const saveMutation = useMutation({
    mutationFn: async () => {
      if (editId) {
        await api.put(`/tenants/${editId}`, { name: form.name, status: form.status, plan: form.plan });
      } else {
        await api.post('/tenants', { name: form.name, slug: form.slug, plan: form.plan });
      }
    },
    onSuccess: () => {
      invalidate();
      setModalOpen(false);
      setEditId(null);
      toast.success(editId ? 'Église mise à jour' : 'Église créée');
    },
    onError: (err: unknown) => toast.error(describeError(err)),
  });

  // Le plan par défaut est le PREMIER plan réellement publié par le backend :
  // plus aucune clé de plan n'est devinée par le front.
  const openCreate = () => {
    setEditId(null);
    setForm({ ...EMPTY_FORM, plan: planOptions[0]?.value ?? '' });
    setModalOpen(true);
  };
  const openEdit = (t: Tenant) => {
    setEditId(t.id);
    setForm({ name: t.name, slug: t.slug, plan: t.plan, status: t.status });
    setModalOpen(true);
  };

  const autoSlug = (name: string) =>
    name.toLowerCase().replace(/[^a-z0-9]+/g, '-').replace(/^-|-$/g, '');

  const filtered = tenants.filter((t) => {
    if (searchTerm) {
      const q = searchTerm.toLowerCase();
      if (!t.name.toLowerCase().includes(q) && !t.slug.toLowerCase().includes(q)) return false;
    }
    if (statusFilter && t.status !== statusFilter) return false;
    if (planFilter && t.plan !== planFilter) return false;
    return true;
  });

  const statsByStatus = STATUS_OPTIONS.map((s) => ({
    ...s,
    count: tenants.filter((t) => t.status === s.value).length,
  }));

  const statsByPlan = planOptions.map((p) => ({
    ...p,
    count: tenants.filter((t) => t.plan === p.value).length,
  }));

  const getPlanInfo = (plan: string) => planInfoFor(plan, planOptions);
  const getStatusInfo = (status: string) => statusInfoFor(status);

  /** Badge d'onboarding : absent du type ⇒ rendu sans crash (§6.2 du plan). */
  const onboardingBadge = (tenant: Tenant) => {
    if (tenant.onboardingCompletedAt) {
      return {
        tone: 'badge-success',
        icon: <CheckCircle2 className="w-3 h-3" />,
        text: `${tText('Onboarding terminé le')} ${new Date(tenant.onboardingCompletedAt)
          .toLocaleDateString(getI18nLocale(), { day: 'numeric', month: 'long', year: 'numeric' })}`,
      };
    }
    return { tone: 'badge-warning', icon: null, text: tText('Onboarding en configuration') };
  };

  const formatCount = (value: number | undefined): string =>
    typeof value === 'number' && Number.isFinite(value)
      ? new Intl.NumberFormat(getI18nLocale()).format(value)
      : '—';

  if (isLoading) {
    return (
      <div className="page-container max-w-6xl" role="status" aria-busy="true" aria-live="polite">
        <VisuallyHidden>{tText('Chargement des églises…')}</VisuallyHidden>
        <SkeletonDashboard />
      </div>
    );
  }

  // État d'erreur (§5.0.2) : message actionnable + retry, jamais un toast qui disparaît.
  if (tenantsFailed) {
    return (
      <div className="page-container max-w-6xl">
        <EmptyState
          icon={<AlertTriangle className="w-8 h-8 text-red-400" />}
          title={tText('Impossible de charger les églises')}
          description={describeError(tenantsError)}
          action={{ label: tText('Réessayer'), onClick: () => { void refetch(); } }}
        />
      </div>
    );
  }

  return (
    <div className="page-container max-w-6xl">
      {/* Header */}
      <div className="page-header">
        <div>
          <h1 className="page-title flex items-center gap-2">
            <Building2 className="w-5 h-5 text-primary-500" />
            Églises (Tenants)
          </h1>
          <p className="page-subtitle">
            Gérez les églises de la plateforme — chaque église possède ses propres
            données et utilisateurs isolés.
          </p>
        </div>
        <div className="page-header-actions">
          <button onClick={() => refetch()} className="btn-ghost btn-sm">
            <RefreshCw className="w-4 h-4" /> Actualiser
          </button>
          <button className="btn-primary btn-sm" onClick={openCreate}>
            <Plus className="w-4 h-4" /> {tText('Nouvelle église')}
          </button>
        </div>
      </div>

      {/* Un plan indisponible est annoncé : un repli silencieux sur les clés
          canoniques afficherait des plans que le backend ne publie peut-être pas. */}
      {plansFallback && (
        <div
          role="status"
          className="mb-4 flex items-start gap-2 rounded-xl border border-amber-500/30 bg-amber-500/10 px-4 py-3 text-xs text-amber-700 dark:text-amber-300"
        >
          <AlertTriangle className="w-4 h-4 shrink-0 mt-0.5" />
          <span>
            {plansFailed
              ? tText("Catalogue des plans indisponible : les clés canoniques sont affichées à titre provisoire.")
              : tText("Catalogue des plans vide : les clés canoniques sont affichées à titre provisoire.")}
          </span>
        </div>
      )}

      {/* Stats Overview */}
      <div className="grid grid-cols-2 lg:grid-cols-4 gap-4 mb-6">
        {statsByStatus.map((s, i) => (
          <button
            key={s.value}
            type="button"
            onClick={() => setStatusFilter(statusFilter === s.value ? '' : s.value)}
            className={`stat-card animate-slide-up text-left cursor-pointer hover:shadow-lg hover:-translate-y-0.5 transition-all duration-200 ${statusFilter === s.value ? 'ring-2 ring-primary-500/50' : ''}`}
            style={{ animationDelay: `${i * 60}ms` }}
          >
            <div className="flex items-start justify-between mb-2">
              <span className="stat-label text-[10px]">{statusFilter === s.value ? `${s.label} (filtré)` : s.label}</span>
              <span className={`w-3 h-3 rounded-full ${s.dot} shadow-[0_0_6px_rgba(0,0,0,0.15)]`} />
            </div>
            <p className="stat-value text-2xl">{s.count}</p>
            <p className="text-[10px] text-gray-400 mt-0.5">
              {s.count === 1 ? 'église' : 'églises'}
            </p>
          </button>
        ))}
      </div>

      {/* Plan distribution */}
      <div className="glass-card p-4 mb-6 animate-slide-up" style={{ animationDelay: '120ms' }}>
        <div className="flex items-center gap-2 mb-3">
          <BarChart3 className="w-4 h-4 text-primary-500" />
          <h3 className="text-sm font-semibold text-gray-700 dark:text-gray-300">{tText('Répartition par plan')}</h3>
        </div>
        <div className="flex items-center gap-3">
          {statsByPlan.map((p) => {
            const pct = tenants.length > 0 ? Math.round((p.count / tenants.length) * 100) : 0;
            return (
              <button
                key={p.value}
                type="button"
                onClick={() => setPlanFilter(planFilter === p.value ? '' : p.value)}
                className={`flex-1 p-3 rounded-xl border text-center transition-all ${
                  planFilter === p.value
                    ? 'border-primary-500 bg-primary-50 dark:bg-primary-900/20 ring-2 ring-primary-500/20'
                    : 'border-gray-200 dark:border-gray-700 hover:border-gray-300'
                }`}
              >
                <p className={`text-lg font-bold ${p.color}`}>{p.count}</p>
                <p className="text-[10px] text-gray-400">{p.label} ({pct}%)</p>
              </button>
            );
          })}
        </div>
      </div>

      {/* Search bar */}
      <div className="glass-card p-4 mb-6 animate-slide-up" style={{ animationDelay: '180ms' }}>
        <div className="flex flex-wrap items-center gap-3">
          <div className="relative flex-1 min-w-[200px]">
            <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400" />
            <input
              type="text"
              placeholder="Rechercher une église par nom ou slug..."
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              className="input pl-9"
            />
          </div>
          {(searchTerm || statusFilter || planFilter) && (
            <button
              onClick={() => { setSearchTerm(''); setStatusFilter(''); setPlanFilter(''); }}
              className="btn-ghost btn-sm"
            >
              <X className="w-3.5 h-3.5" /> {tText('Réinitialiser')}
            </button>
          )}
        </div>
        {(statusFilter || planFilter) && (
          <div className="flex items-center gap-2 mt-2 pt-2 border-t border-gray-200/60 dark:border-gray-700/60">
            <Filter className="w-3.5 h-3.5 text-gray-400" />
            <span className="text-xs text-gray-500">Filtres actifs :</span>
            {statusFilter && (
              <span className="badge text-[10px] badge-primary">
                {getStatusInfo(statusFilter).label}
                <button onClick={() => setStatusFilter('')} className="ml-1 hover:text-red-500">×</button>
              </span>
            )}
            {planFilter && (
              <span className="badge text-[10px] badge-info">
                {getPlanInfo(planFilter).label}
                <button onClick={() => setPlanFilter('')} className="ml-1 hover:text-red-500">×</button>
              </span>
            )}
          </div>
        )}
      </div>

      {/* Tenant list */}
      {filtered.length === 0 ? (
        tenants.length === 0 ? (
          <EmptyState
            icon={<Building2 className="w-8 h-8 text-gray-400" />}
            title={tText('Aucune église configurée')}
            description={tText("Créez la première église de la plateforme. Chaque église possède ses propres données et utilisateurs isolés.")}
            action={{ label: tText('Créer la première église'), onClick: openCreate }}
          />
        ) : (
          <EmptyState
            icon={<Search className="w-8 h-8 text-gray-400" />}
            title={tText('Aucune église ne correspond aux filtres')}
            description={tText('Modifiez la recherche ou réinitialisez les filtres pour revoir toutes les églises.')}
            action={{
              label: tText('Réinitialiser les filtres'),
              onClick: () => { setSearchTerm(''); setStatusFilter(''); setPlanFilter(''); },
            }}
          />
        )
      ) : (
        <div className="space-y-3">
          {filtered.map((t, i) => {
            const statusInfo = getStatusInfo(t.status);
            const planInfo = getPlanInfo(t.plan);
            // undefined = donnée non disponible (échec de l'appel agrégé OU tenant
            // hors de la première page) → « — », jamais 0.
            const usage = usageIndex.get(t.id);
            return (
              <div
                key={t.id}
                className="glass-card px-5 py-4 flex items-center gap-4 animate-slide-up hover:shadow-md transition-shadow"
                style={{ animationDelay: `${i * 40}ms` }}
              >
                <div className="w-12 h-12 rounded-xl bg-gradient-to-br from-primary-500 to-emerald-600 flex items-center justify-center text-white text-base font-bold flex-shrink-0 shadow-sm">
                  {t.name.charAt(0).toUpperCase()}
                </div>
                <div className="flex-1 min-w-0">
                  <div className="flex items-center gap-2 flex-wrap">
                    <span className="text-sm font-bold text-gray-900 dark:text-gray-100">{t.name}</span>
                    <span className={`badge text-[10px] ${statusInfo.color}`}>
                      <span className={`w-1.5 h-1.5 rounded-full ${statusInfo.dot}`} />
                      {statusInfo.label}
                    </span>
                    <span className={`badge text-[10px] ${planInfo.badge}`}>{planInfo.label}</span>
                    {(() => {
                      const badge = onboardingBadge(t);
                      return (
                        <span className={`badge text-[10px] ${badge.tone}`} title={badge.text}>
                          {badge.icon}
                          {badge.text}
                        </span>
                      );
                    })()}
                  </div>
                  <div className="flex items-center gap-3 mt-1 text-xs text-gray-400">
                    <span className="flex items-center gap-1">
                      <Globe className="w-3 h-3" /> {t.slug}
                    </span>
                    <span className="flex items-center gap-1">
                      <Calendar className="w-3 h-3" />
                      Créée le {new Date(t.createdAt).toLocaleDateString(getI18nLocale(), { day: 'numeric', month: 'long', year: 'numeric' })}
                    </span>
                    <span className="flex items-center gap-1" title={tText('Utilisateurs et crédits IA consommés')}>
                      <Users className="w-3 h-3" />
                      {tText('Utilisateurs')} : {formatCount(usage?.users)}
                      {' · '}
                      {tText('Crédits IA')} : {formatCount(usage?.aiCredits)}
                    </span>
                  </div>
                </div>
                <div className="flex items-center gap-1">
                  <button
                    type="button"
                    aria-label={tText('Voir le détail')}
                    className="btn-icon text-gray-400 hover:text-blue-500 hover:bg-blue-50 dark:hover:bg-blue-900/20"
                    onClick={() => setDetailTenant(t)}
                    title="Voir le détail"
                  >
                    <Eye className="w-4 h-4" />
                  </button>
                  <button
                    type="button"
                    aria-label={tText('Modifier')}
                    className="btn-icon text-gray-400 hover:text-gray-700 dark:hover:text-gray-200 hover:bg-gray-100/70"
                    onClick={() => openEdit(t)}
                    title="Modifier"
                  >
                    <Pencil className="w-4 h-4" />
                  </button>
                  {t.status === 'SUSPENDED' ? (
                    <button
                      type="button"
                      aria-label={tText('Réactiver l\u2019église')}
                      disabled={reactivateMutation.isPending}
                      className="btn-icon text-gray-400 hover:text-emerald-600 hover:bg-emerald-50 dark:hover:bg-emerald-900/20 disabled:opacity-50"
                      onClick={() => reactivateMutation.mutate(t.id)}
                      title={tText('Réactiver l\u2019église')}
                    >
                      {reactivateMutation.isPending
                        ? <Loader2 className="w-4 h-4 animate-spin" />
                        : <RotateCcw className="w-4 h-4" />}
                    </button>
                  ) : (
                    <button
                      type="button"
                      aria-label={tText('Suspendre l\u2019église')}
                      disabled={suspendMutation.isPending}
                      className="btn-icon text-gray-400 hover:text-amber-600 hover:bg-amber-50 dark:hover:bg-amber-900/20 disabled:opacity-50"
                      onClick={() => {
                        if (confirm(`${tText("Suspendre l\u2019église")} « ${t.name} » ? ${tText("Ses utilisateurs ne pourront plus se connecter.")}`)) {
                          suspendMutation.mutate(t.id);
                        }
                      }}
                      title={tText('Suspendre l\u2019église')}
                    >
                      {suspendMutation.isPending
                        ? <Loader2 className="w-4 h-4 animate-spin" />
                        : <Ban className="w-4 h-4" />}
                    </button>
                  )}
                  {/* PORT Develop1 (§57) — archivage, distinct de la suspension. */}
                  {t.status !== 'CANCELLED' && (
                    <button
                      type="button"
                      aria-label={tText('Archiver l\u2019église')}
                      disabled={archiveMutation.isPending}
                      className="btn-icon text-gray-400 hover:text-gray-700 hover:bg-gray-100/70 dark:hover:bg-gray-800/60 disabled:opacity-50"
                      onClick={() => {
                        if (confirm(`${tText('Archiver cette église ?')} « ${t.name} » ?`)) {
                          archiveMutation.mutate(t.id);
                        }
                      }}
                      title={tText('Archiver (statut annulée, données conservées)')}
                    >
                      {archiveMutation.isPending
                        ? <Loader2 className="w-4 h-4 animate-spin" />
                        : <Archive className="w-4 h-4" />}
                    </button>
                  )}
                </div>
              </div>
            );
          })}
        </div>
      )}

      {/* Create / Edit Modal */}
      {modalOpen && (
        <div className="modal-overlay" onClick={() => setModalOpen(false)}>
          <div className="modal-content max-w-lg" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <div className="flex items-center gap-3">
                <div className="p-2.5 rounded-xl bg-gradient-to-br from-primary-500 to-primary-600 text-white shadow-sm">
                  <Building2 className="w-5 h-5" />
                </div>
                <div>
                  <h3 className="text-base font-bold text-gray-900 dark:text-gray-100">
                    {editId ? 'Modifier l\'église' : 'Nouvelle église'}
                  </h3>
                  <p className="text-xs text-gray-500 dark:text-gray-400">
                    {editId ? 'Mettez à jour les informations' : 'Configurez une nouvelle église sur la plateforme'}
                  </p>
                </div>
              </div>
              <button className="btn-icon text-gray-400 hover:text-gray-600" onClick={() => setModalOpen(false)}>
                <X className="w-5 h-5" />
              </button>
            </div>
            <div className="modal-body space-y-4 max-h-[60vh] overflow-y-auto">
              <div>
                <label className="label">Nom de l'église</label>
                <input
                  className="input"
                  value={form.name}
                  onChange={(e) => {
                    const name = e.target.value;
                    setForm({ ...form, name, slug: editId ? form.slug : autoSlug(name) });
                  }}
                  placeholder="Ex. : Église de la Grâce"
                />
              </div>
              <div>
                <label className="label">Slug (identifiant URL)</label>
                <input
                  className="input font-mono"
                  value={form.slug}
                  onChange={(e) => setForm({ ...form, slug: e.target.value.toLowerCase().replace(/[^a-z0-9-]/g, '') })}
                  disabled={!!editId}
                  placeholder="eglise-de-la-grace"
                />
                <p className="text-[10px] text-gray-400 mt-1">
                  {editId ? 'Le slug ne peut pas être modifié.' : 'Minuscules, chiffres et tirets uniquement.'}
                </p>
              </div>
              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="label">Plan</label>
                  <select className="input" value={form.plan} onChange={(e) => setForm({ ...form, plan: e.target.value })}>
                    {planOptions.map((p) => (
                      <option key={p.value} value={p.value}>{p.label}</option>
                    ))}
                  </select>
                </div>
                {editId && (
                  <div>
                    <label className="label">Statut</label>
                    <select className="input" value={form.status} onChange={(e) => setForm({ ...form, status: e.target.value as TenantStatus })}>
                      {/* Résilience : si le backend renvoie un statut que cette version
                          ne connaît pas, il reste sélectionnable tel quel. Sans cette
                          option, le <select> afficherait « Active » et l'enregistrement
                          silently réécrirait le statut réel — une perte de données. */}
                      {form.status && !STATUS_OPTIONS.some((s) => s.value === form.status) && (
                        <option value={form.status}>{form.status}</option>
                      )}
                      {STATUS_OPTIONS.map((s) => (
                        <option key={s.value} value={s.value}>{s.label}</option>
                      ))}
                    </select>
                  </div>
                )}
              </div>
            </div>
            <div className="modal-footer">
              <button className="btn-ghost btn-sm" onClick={() => setModalOpen(false)}>{tText('Annuler')}</button>
              <button
                className="btn-primary btn-sm"
                onClick={() => saveMutation.mutate()}
                disabled={
                  saveMutation.isPending
                  || !form.name.trim()
                  || !form.plan
                  || (!editId && !form.slug.trim())
                }
              >
                {saveMutation.isPending ? <Loader2 className="w-4 h-4 animate-spin" /> : <Save className="w-4 h-4" />}
                {editId ? 'Enregistrer' : 'Créer'}
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Detail Modal */}
      {detailTenant && (
        <div className="modal-overlay" onClick={() => setDetailTenant(null)}>
          <div className="modal-content max-w-xl" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <div className="flex items-center gap-3">
                <div className="w-12 h-12 rounded-xl bg-gradient-to-br from-primary-500 to-emerald-600 flex items-center justify-center text-white text-lg font-bold shadow-sm">
                  {detailTenant.name.charAt(0).toUpperCase()}
                </div>
                <div>
                  <h3 className="text-base font-bold text-gray-900 dark:text-gray-100">{detailTenant.name}</h3>
                  <div className="flex items-center gap-2 mt-0.5">
                    <span className={`badge text-[10px] ${getStatusInfo(detailTenant.status).color}`}>
                      {getStatusInfo(detailTenant.status).label}
                    </span>
                    <span className={`badge text-[10px] ${getPlanInfo(detailTenant.plan).badge}`}>
                      {getPlanInfo(detailTenant.plan).label}
                    </span>
                  </div>
                </div>
              </div>
              <button className="btn-icon text-gray-400 hover:text-gray-600" onClick={() => setDetailTenant(null)}>
                <X className="w-5 h-5" />
              </button>
            </div>
            <div className="modal-body space-y-4">
              <div className="grid grid-cols-2 gap-4">
                <div className="p-4 rounded-xl bg-gray-50 dark:bg-gray-800/40 border border-gray-100 dark:border-gray-700/40">
                  <div className="flex items-center gap-2 mb-1">
                    <Globe className="w-3.5 h-3.5 text-gray-400" />
                    <span className="text-[10px] text-gray-400 uppercase font-semibold">Slug</span>
                  </div>
                  <p className="text-sm font-mono text-gray-900 dark:text-gray-100">{detailTenant.slug}</p>
                </div>
                <div className="p-4 rounded-xl bg-gray-50 dark:bg-gray-800/40 border border-gray-100 dark:border-gray-700/40">
                  <div className="flex items-center gap-2 mb-1">
                    <Calendar className="w-3.5 h-3.5 text-gray-400" />
                    <span className="text-[10px] text-gray-400 uppercase font-semibold">{tText('Créée le')}</span>
                  </div>
                  <p className="text-sm text-gray-900 dark:text-gray-100">
                    {new Date(detailTenant.createdAt).toLocaleDateString(getI18nLocale(), { day: 'numeric', month: 'long', year: 'numeric' })}
                  </p>
                </div>
              </div>

              <div className="grid grid-cols-3 gap-3">
                <div className="p-3 rounded-xl bg-blue-50/50 dark:bg-blue-900/10 border border-blue-200/40 dark:border-blue-700/30 text-center">
                  <Users className="w-4 h-4 text-blue-500 mx-auto mb-1" />
                  <p className="text-[10px] text-blue-500 font-semibold uppercase">ID</p>
                  <p className="text-xs font-mono text-gray-900 dark:text-gray-100 truncate">{detailTenant.id.slice(0, 8)}…</p>
                </div>
                <div className="p-3 rounded-xl bg-emerald-50/50 dark:bg-emerald-900/10 border border-emerald-200/40 dark:border-emerald-700/30 text-center">
                  <Shield className="w-4 h-4 text-emerald-500 mx-auto mb-1" />
                  <p className="text-[10px] text-emerald-500 font-semibold uppercase">Plan</p>
                  <p className="text-sm font-bold text-gray-900 dark:text-gray-100">{getPlanInfo(detailTenant.plan).label}</p>
                </div>
                <div className="p-3 rounded-xl bg-amber-50/50 dark:bg-amber-900/10 border border-amber-200/40 dark:border-amber-700/30 text-center">
                  <Activity className="w-4 h-4 text-amber-500 mx-auto mb-1" />
                  <p className="text-[10px] text-amber-500 font-semibold uppercase">Statut</p>
                  <p className="text-sm font-bold text-gray-900 dark:text-gray-100">{getStatusInfo(detailTenant.status).label}</p>
                </div>
              </div>

              {detailTenant.status === 'SUSPENDED' && (
                <div className="p-3 rounded-xl bg-amber-50/70 dark:bg-amber-900/10 border border-amber-200/50 dark:border-amber-800/30">
                  <p className="text-xs text-amber-700 dark:text-amber-400 flex items-center gap-2">
                    <Shield className="w-4 h-4" />
                    Cette église est suspendue — les utilisateurs ne peuvent pas se connecter.
                  </p>
                  <button
                    type="button"
                    className="btn-secondary btn-sm mt-2"
                    disabled={reactivateMutation.isPending}
                    onClick={() => reactivateMutation.mutate(detailTenant.id)}
                  >
                    {reactivateMutation.isPending
                      ? <Loader2 className="w-3.5 h-3.5 animate-spin" />
                      : <RotateCcw className="w-3.5 h-3.5" />}
                    {tText('Réactiver cette église')}
                  </button>
                </div>
              )}

              {/* PORT Develop1 (§G1.9) — porte d'entree de l'impersonation. */}
              {isSuperAdmin && (
                <div className="p-3 rounded-xl bg-violet-50/60 dark:bg-violet-900/10 border border-violet-200/50 dark:border-violet-800/30">
                  <p className="text-xs text-violet-700 dark:text-violet-300 flex items-center gap-2">
                    <UserCheck className="w-4 h-4" />
                    {tText('Support technique : se faire passer pour un membre de cette église.')}
                  </p>
                  <button
                    type="button"
                    className="btn-secondary btn-sm mt-2 text-violet-600"
                    onClick={() => impersonateTenant(detailTenant)}
                  >
                    {tText('Impersoner un membre')}
                  </button>
                </div>
              )}

              {/* Onboarding + usage réel */}
              <div className="p-4 rounded-xl bg-gray-50 dark:bg-gray-800/40 border border-gray-100 dark:border-gray-700/40">
                <p className="text-[10px] text-gray-400 uppercase font-semibold mb-2">{tText('Onboarding')}</p>
                <span className={`badge text-[10px] ${onboardingBadge(detailTenant).tone}`}>
                  {onboardingBadge(detailTenant).icon}
                  {onboardingBadge(detailTenant).text}
                </span>
              </div>

              <QuotaUsageCards
                metrics={detailUsage?.metrics ?? []}
                title={tText('Quotas et usage de cette église')}
                loading={detailUsageLoading}
                onRetry={() => { void queryClient.invalidateQueries({ queryKey: ['platform', 'admin', 'quota-usage', 'tenant', detailTenant.id] }); }}
              />
            </div>
            <div className="modal-footer">
              <button className="btn-ghost btn-sm" onClick={() => setDetailTenant(null)}>Fermer</button>
              <button
                className="btn-secondary btn-sm"
                onClick={() => { setDetailTenant(null); openEdit(detailTenant); }}
              >
                <Pencil className="w-3.5 h-3.5" /> Modifier
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
