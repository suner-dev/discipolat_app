import { useEffect, useState } from 'react';
import { Link, useNavigate, useSearchParams } from 'react-router-dom';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import api, { getErrorMessage } from '@/lib/api';
import { useI18n, tText } from '@/i18n';
import { useAuth } from '@/contexts/AuthContext';
import { Loader2, UserPlus, MailCheck, ShieldCheck, ArrowRight, CheckCircle2, Church, KeyRound, Copy, MessageCircle } from 'lucide-react';

const registerSchema = z.object({
  firstName: z.string().min(1, 'Requis'),
  lastName: z.string().min(1, 'Requis'),
  email: z.string().email('Email invalide').min(1, 'Email requis'),
  phone: z.string().optional(),
  churchName: z.string().optional(),
  password: z.string().min(8, 'Au moins 8 caractères'),
  confirmPassword: z.string(),
  // RGPD art. 7 : consentement explicite, actif, par document — pas de case pré-cochée.
  // `boolean + refine` (et non literal(true)) : mêmes validation et message, mais
  // la valeur par défaut du formulaire peut légalement être `false`.
  consentCgu: z.boolean().refine((v) => v === true, { message: 'Vous devez accepter les CGU' }),
  consentPrivacy: z.boolean().refine((v) => v === true, { message: 'Vous devez accepter la politique de confidentialité' }),
  consentArt9: z.boolean().refine((v) => v === true, { message: 'Vous devez consentir au traitement des données religieuses (RGPD art. 9)' }),
}).refine((d) => d.password === d.confirmPassword, {
  message: 'Les mots de passe ne correspondent pas',
  path: ['confirmPassword'],
});

type RegisterForm = z.infer<typeof registerSchema>;

interface LegalDocSummary { code: string; version: number; title: string }

/** Version des documents légaux affichée au pied du formulaire (preuve art. 7). */
function useLegalVersions() {
  const [docs, setDocs] = useState<LegalDocSummary[]>([]);
  useEffect(() => {
    let active = true;
    api.get<LegalDocSummary[]>('/public/legal')
      .then(({ data }) => { if (active && Array.isArray(data)) setDocs(data); })
      .catch(() => { /* version résolue côté serveur si l'affichage échoue */ });
    return () => { active = false; };
  }, []);
  const versionOf = (code: string) => docs.find((d) => d.code === code)?.version;
  const legalVersion = versionOf('CGU') ? `CGU-v${versionOf('CGU')}` : undefined;
  return { versionOf, legalVersion };
}

export default function RegisterPage() {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const { t } = useI18n();
  // SPEC_ORGANISATION_DENOMINATION_V2 §7.0 / T-W0 : adoption de session.
  const { adoptSession } = useAuth();
  const requestedPlan = searchParams.get('plan')?.trim().toUpperCase() || 'DISCOVERY';
  // §G3.1 (Develop1) — lien d'inscription d'une église : /register?tenant=<slug>
  // rattache le compte (puis la fiche répertoire) à cette église. Le champ est
  // optionnel : absent, on reste sur le flux SaaS de main (demande à valider).
  const tenantSlug = searchParams.get('tenant')?.trim() || undefined;
  // SPEC_ONBOARDING_FLOWS (FE-2) — deux gestes d'entrée self-service :
  //  • /register?mode=church → « Créer mon église » (fondateur, D1) ;
  //  • /register?joinCode=…  → « Rejoindre avec un code » (depuis /join).
  const createChurch = searchParams.get('mode') === 'church';
  const joinCode = searchParams.get('joinCode')?.trim() || undefined;
  const joinChurchName = searchParams.get('church')?.trim() || undefined;
  const [error, setError] = useState('');
  const [success, setSuccess] = useState(false);
  const [churchResult, setChurchResult] = useState<{ name: string; slug: string; joinCode: string } | null>(null);
  const [copied, setCopied] = useState(false);

  /**
   * SPEC_ORGANISATION_DENOMINATION_V2 (T-M3, D2) — NATURE de l'organisation
   * créée. `CHURCH` par défaut : on ne change pas le comportement historique,
   * on AJOUTE la capacité de créer une dénomination / asso / orga /
   * méga-association.
   */
  const [orgKind, setOrgKind] = useState('CHURCH');

  /** SPEC_ONBOARDING_FLOWS §4.1 — URL d'invitation canonique (vanity link). */
  const inviteUrl = (slug: string) => `${window.location.origin}/j/${slug}`;

  /** Copie le LIEN en priorité ; à défaut (pas de slug), le code. */
  const copyInvite = async (slug: string, code: string) => {
    const text = slug ? inviteUrl(slug) : code;
    try {
      await navigator.clipboard.writeText(text);
      setCopied(true);
      window.setTimeout(() => setCopied(false), 2000);
    } catch {
      // Clipboard refusé (contexte non sécurisé / permission) : on reste
      // silencieux, le texte reste sélectionnable à l'écran.
    }
  };

  /** Message WhatsApp prêt à envoyer : lien + code en secours. */
  const waShareLink = (churchName: string, code: string) => {
    const link = churchResult?.slug ? inviteUrl(churchResult.slug) : '';
    const lines = [
      `Rejoignez ${churchName} sur Discipolat`,
      link,
      code ? `Code d'entrée : ${code}` : '',
      '',
      'Cliquez sur le lien, créez votre compte : vous rejoignez directement.',
    ];
    return `https://wa.me/?text=${encodeURIComponent(lines.filter(Boolean).join('\n'))}`;
  };
  const [approvalInfo, setApprovalInfo] = useState<string | null>(null);
  const { versionOf, legalVersion } = useLegalVersions();

  const {
    register,
    handleSubmit,
    getValues, // B3 : pré-remplissage du suivi de demande
    formState: { errors, isSubmitting },
  } = useForm<RegisterForm>({
    resolver: zodResolver(registerSchema),
    defaultValues: { consentCgu: false, consentPrivacy: false, consentArt9: false },
  });

  const onSubmit = async (data: RegisterForm) => {
    if (createChurch && !(data.churchName ?? '').trim()) {
      setError("Le nom de l'église est requis pour la créer.");
      return;
    }
    try {
      setError('');
      const response = await api.post('/auth/register', {
        email: data.email.trim(),
        password: data.password,
        firstName: data.firstName.trim(),
        lastName: data.lastName.trim(),
        phone: data.phone?.trim() || undefined,
        plan: requestedPlan.toLowerCase(),
        tenantSlug,
        createChurch: createChurch || undefined,
        churchName: createChurch ? data.churchName?.trim() : undefined,
        // SPEC_ORGANISATION_DENOMINATION_V2 (T-M3, D2) — nature de
        // l'organisation. Envoyée seulement en mode création.
        kind: createChurch ? orgKind : undefined,
        joinCode: joinCode || undefined,
        consentCgu: data.consentCgu,
        consentPrivacy: data.consentPrivacy,
        consentArt9: data.consentArt9,
        legalVersion,
      });
      const payload = response.data as {
        session?: any;
        church?: { name: string; slug: string; joinCode: string };
        status?: string;
        churchName?: string;
      };
      if (payload.session?.accessToken) {
        // Fondateur : session immédiatement établie (D1).
        // SPEC_ORGANISATION_DENOMINATION_V2 §7.0 / T-W0 (F9) — on passe par
        // adoptSession() : c'est le SEUL chemin qui écrit les trois clés
        // attendues par le bootstrap d'AuthContext (accessToken, refreshToken,
        // user). L'ancienne version posait les jetons puis supprimait `user` en
        // pensant que `/auth/me` le régénérerait : or AuthProvider retourne
        // AVANT l'appel `/auth/me` si `user` est absent, donc `isAuthenticated`
        // restait `false` et le fondateur était renvoyé vers `/login` juste
        // après avoir créé son église.
        adoptSession(payload.session);
        setChurchResult({
          name: payload.church?.name ?? data.churchName ?? '',
          // SPEC_ONBOARDING_FLOWS §4.1 : le lien d'invitation est un
          // CONTRAT — sans lui, le fondateur n'a que le code à dicter.
          slug: payload.church?.slug ?? '',
          joinCode: payload.church?.joinCode ?? '',
        });
        return;
      }
      if (payload.status === 'PENDING_APPROVAL') {
        setApprovalInfo(payload.churchName || '');
      }
      setSuccess(true);
    } catch (err) {
      setError(getErrorMessage(err));
    }
  };

  if (churchResult) {
    return (
      <div className="space-y-6 text-center animate-slide-up">
        <div className="inline-flex items-center justify-center w-16 h-16 rounded-2xl bg-primary-500/10 border border-primary-500/20 mb-4">
          <Church className="w-8 h-8 text-primary-500" />
        </div>
        <h2 className="text-2xl font-bold text-gray-900 dark:text-white font-display">
          {tText('Votre église est prête !')}
        </h2>
        <p className="text-sm text-gray-500 dark:text-gray-400">
          {churchResult.name}
        </p>
        {churchResult.joinCode && (
          <div className="mx-auto max-w-sm rounded-xl border border-gray-200 dark:border-white/10 bg-gray-50/60 dark:bg-white/5 p-4">
            <p className="text-xs uppercase tracking-wider text-gray-400 mb-1">{tText("Code d'entrée de l'église")}</p>
            <p className="flex items-center justify-center gap-2 text-xl font-mono font-bold text-gray-900 dark:text-white">
              <KeyRound className="w-5 h-5 text-primary-500" /> {churchResult.joinCode}
            </p>
            <p className="mt-2 text-xs text-gray-500 dark:text-gray-400">
              {tText('Partagez-le avec vos futurs membres — ils le saisiront une seule fois à la rejointure.')}
            </p>
          </div>
        )}
{/* SPEC_ONBOARDING_FLOWS §4.1 — le LIEN d'invitation : la porte la
            plus fluide (clic, zéro saisie). Le code reste affiché : certains
            membres l'ont en dictée ou n'ont pas de smartphone. */}
        {churchResult.slug && (
          <div className="mx-auto max-w-sm rounded-xl border border-primary-500/20 bg-primary-500/5 p-4">
            <p className="text-xs uppercase tracking-wider text-gray-400 mb-1">
              {tText("Lien d'invitation")}
            </p>
            <a
              href={`/j/${churchResult.slug}`}
              className="block truncate text-sm font-mono font-semibold text-primary-600 dark:text-primary-400 hover:underline"
            >
              {`${window.location.origin}/j/${churchResult.slug}`}
            </a>
            <p className="mt-2 text-xs text-gray-500 dark:text-gray-400">
              {tText("Partagez ce lien : vos membres cliquent, rejoignent, et n'ont plus rien à saisir ensuite.")}
            </p>
            <div className="mt-3 grid grid-cols-2 gap-2">
              <button
                type="button"
                onClick={() => copyInvite(churchResult.slug, churchResult.joinCode)}
                className="inline-flex items-center justify-center gap-2 rounded-lg border border-gray-300 dark:border-white/15 px-3 py-2 text-xs font-medium text-gray-700 dark:text-gray-200 hover:bg-gray-100 dark:hover:bg-white/5"
              >
                {copied ? <CheckCircle2 className="w-3.5 h-3.5" /> : <Copy className="w-3.5 h-3.5" />}
                {copied ? tText('Copié !') : tText('Copier le lien')}
              </button>
              <a
                href={waShareLink(churchResult.name, churchResult.joinCode)}
                target="_blank"
                rel="noopener noreferrer"
                className="inline-flex items-center justify-center gap-2 rounded-lg bg-green-600 px-3 py-2 text-xs font-medium text-white hover:bg-green-700"
              >
                <MessageCircle className="w-3.5 h-3.5" />
                {tText('Partager')}
              </a>
            </div>
          </div>
        )}
        <button
          onClick={() => { navigate('/dashboard'); }}
          className="w-full py-3 px-4 rounded-xl bg-gradient-to-r from-primary-600 to-primary-500 text-white font-medium text-sm flex items-center justify-center gap-2"
        >
          <ArrowRight className="w-4 h-4" /> {tText('Accéder à mon église')}
        </button>
      </div>
    );
  }

  if (success) {
    return (
      <div className="space-y-6 text-center animate-slide-up">
        <div className="inline-flex items-center justify-center w-16 h-16 rounded-2xl bg-green-500/10 border border-green-500/20 mb-4">
          <CheckCircle2 className="w-8 h-8 text-green-400" />
        </div>
        <h2 className="text-2xl font-bold text-gray-900 dark:text-white font-display">
          {approvalInfo ? tText('Demande transmise') : tText('Demande reçue')}
        </h2>
        <p className="text-sm text-gray-500 dark:text-gray-400">
          {approvalInfo
            ? tText(`Votre demande de rejoindre ${approvalInfo} a été enregistrée. Un responsable vous contactera après validation.`)
            : joinCode
              ? tText('Compte créé. Vérifiez votre email pour activer le compte et rejoindre définitivement le groupe de la nouvelle église.')
              : tText('Votre demande d\'église sera examinée par un Super Admin avant toute création de compte.')}
        </p>
        <button
          onClick={() => navigate('/login')}
          className="w-full py-3 px-4 rounded-xl bg-gradient-to-r from-primary-600 to-primary-500 text-white font-medium text-sm
                     hover:from-primary-500 hover:to-primary-400 transition-all duration-200 shadow-lg shadow-primary-500/25
                     flex items-center justify-center gap-2"
        >
          <ArrowRight className="w-4 h-4" />
          {t('auth.loginNow')}
        </button>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="text-center">
        <div className="inline-flex items-center gap-2.5 mb-4 animate-fade-in">
          <span className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full bg-primary-500/10 border border-primary-500/20 text-primary-400 text-xs font-medium">
            <ShieldCheck className="w-3 h-3" />
            {t('auth.registerRoleHint')}
          </span>
        </div>
        <h2 className="text-2xl font-bold text-gray-900 dark:text-white font-display animate-slide-up">
          {t('auth.registerTitle')}
        </h2>
        <p className="mt-1.5 text-sm text-gray-500 dark:text-gray-400 animate-slide-up" style={{ animationDelay: '50ms' }}>
          {t('auth.registerSubtitle')}
        </p>
        {requestedPlan !== 'DISCOVERY' && (
          <p className="mt-3 text-xs font-medium text-violet-600 dark:text-violet-300">
            Plan sélectionné : {requestedPlan}
          </p>
        )}
        {createChurch && (
          <p className="mt-3 inline-flex items-center gap-1.5 text-xs font-medium text-primary-600 dark:text-primary-400">
            <Church className="w-3.5 h-3.5" /> {tText('Vous créez une nouvelle église — vous en serez le fondateur.')}
          </p>
        )}
        {joinChurchName && (
          <p className="mt-3 inline-flex items-center gap-1.5 text-xs font-medium text-primary-600 dark:text-primary-400">
            <KeyRound className="w-3.5 h-3.5" /> {tText(`Vous rejoignez ${joinChurchName}`)}
          </p>
        )}
      </div>

      {/* Error */}
      {error && (
        <div className="animate-slide-up p-3.5 rounded-xl bg-red-500/10 border border-red-500/20 backdrop-blur-sm">
          <p className="text-sm text-red-600 dark:text-red-300">{error}</p>
        </div>
      )}

      {/* Form */}
      <form onSubmit={handleSubmit(onSubmit)} className="space-y-4 animate-slide-up" style={{ animationDelay: '100ms' }}>
        {createChurch && (
          <div>
            <label htmlFor="churchName" className="block text-sm font-medium text-gray-600 dark:text-gray-300 mb-1.5">
              {tText(orgKind === 'CHURCH' ? "Nom de l'église" : "Nom de l'organisation")}
            </label>
            <input
              id="churchName"
              type="text"
              autoComplete="organization"
              className="w-full rounded-xl bg-gray-100/80 dark:bg-white/5 border border-gray-200 dark:border-white/10 text-gray-900 dark:text-white placeholder-gray-400
                         px-4 py-3 text-sm focus:outline-none focus:border-primary-500/50 focus:ring-2 focus:ring-primary-500/20 transition-all duration-200"
              placeholder={tText(orgKind === 'CHURCH' ? 'Église Bethel' : 'Dénomination Bethel')}
              {...register('churchName')}
            />
          </div>
        )}

        {/* SPEC_ORGANISATION_DENOMINATION_V2 (T-M3, D2) — la NATURE de
            l'organisation. Un tenant n'est plus « une église » : c'est une
            dénomination, une église, une association, une organisation ou une
            méga-association. CHURCH reste le défaut (comportement historique). */}
        {createChurch && (
          <div>
            <label htmlFor="orgKind" className="block text-sm font-medium text-gray-600 dark:text-gray-300 mb-1.5">
              {tText("Type d'organisation")}
            </label>
            {/* La liste deroulante native herite la couleur de texte du select
                mais se peint sur le fond « field » du navigateur (clair) : sans
                fond opaque sur les <option> et sans color-scheme, le texte
                devenait blanc sur blanc en mode sombre. */}
            <select
              id="orgKind"
              value={orgKind}
              onChange={(e) => setOrgKind(e.target.value)}
              className="w-full rounded-xl bg-gray-100/80 dark:bg-white/5 border border-gray-200 dark:border-white/10
                         text-gray-900 dark:text-white px-4 py-3 text-sm focus:outline-none
                         focus:border-primary-500/50 focus:ring-2 focus:ring-primary-500/20 transition-all duration-200
                         [color-scheme:light] dark:[color-scheme:dark]
                         [&>option]:bg-white [&>option]:text-gray-900
                         dark:[&>option]:bg-gray-900 dark:[&>option]:text-gray-100"
            >
              <option value="CHURCH">{tText('Église')}</option>
              <option value="DENOMINATION">{tText("Dénomination (réseau d'églises)")}</option>
              <option value="ASSOCIATION">{tText('Association')}</option>
              <option value="ORGANIZATION">{tText('Organisation')}</option>
              <option value="MEGA_ASSOCIATION">{tText('Méga-association')}</option>
            </select>
            {orgKind !== 'CHURCH' && (
              <p className="mt-1.5 text-xs text-gray-500 dark:text-gray-400">
                {tText("Vous pourrez créer vos églises enfants depuis votre espace organisation.")}
              </p>
            )}
          </div>
        )}
        <div className="grid grid-cols-2 gap-3">
          <div>
            <label htmlFor="firstName" className="block text-sm font-medium text-gray-600 dark:text-gray-300 mb-1.5">
              {t('auth.firstName')}
            </label>
            <input
              id="firstName"
              type="text"
              autoComplete="given-name"
              className={`w-full rounded-xl bg-gray-100/80 dark:bg-white/5 border border-gray-200 dark:border-white/10 text-gray-900 dark:text-white placeholder-gray-400
                         px-4 py-3 text-sm focus:outline-none focus:border-primary-500/50 focus:ring-2 focus:ring-primary-500/20 transition-all duration-200
                         ${errors.firstName ? 'border-red-500/50 focus:border-red-500 focus:ring-red-500/20' : ''}`}
              placeholder="Jean"
              {...register('firstName')}
            />
            {errors.firstName && <p className="mt-1 text-xs text-red-400">{errors.firstName.message}</p>}
          </div>
          <div>
            <label htmlFor="lastName" className="block text-sm font-medium text-gray-600 dark:text-gray-300 mb-1.5">
              {t('auth.lastName')}
            </label>
            <input
              id="lastName"
              type="text"
              autoComplete="family-name"
              className={`w-full rounded-xl bg-gray-100/80 dark:bg-white/5 border border-gray-200 dark:border-white/10 text-gray-900 dark:text-white placeholder-gray-400
                         px-4 py-3 text-sm focus:outline-none focus:border-primary-500/50 focus:ring-2 focus:ring-primary-500/20 transition-all duration-200
                         ${errors.lastName ? 'border-red-500/50 focus:border-red-500 focus:ring-red-500/20' : ''}`}
              placeholder="Kouassi"
              {...register('lastName')}
            />
            {errors.lastName && <p className="mt-1 text-xs text-red-400">{errors.lastName.message}</p>}
          </div>
        </div>

        <div>
          <label htmlFor="email" className="block text-sm font-medium text-gray-600 dark:text-gray-300 mb-1.5">
            {t('auth.email')}
          </label>
          <input
            id="email"
            type="email"
            autoComplete="email"
            className={`w-full rounded-xl bg-gray-100/80 dark:bg-white/5 border border-gray-200 dark:border-white/10 text-gray-900 dark:text-white placeholder-gray-400
                       px-4 py-3 text-sm focus:outline-none focus:border-primary-500/50 focus:ring-2 focus:ring-primary-500/20 transition-all duration-200
                       ${errors.email ? 'border-red-500/50 focus:border-red-500 focus:ring-red-500/20' : ''}`}
            placeholder="vous@email.com"
            {...register('email')}
          />
          {errors.email && <p className="mt-1 text-xs text-red-400">{errors.email.message}</p>}
        </div>

        <div>
          <label htmlFor="phone" className="block text-sm font-medium text-gray-600 dark:text-gray-300 mb-1.5">
            {t('auth.phone')}
          </label>
          <input
            id="phone"
            type="tel"
            autoComplete="tel"
            className="w-full rounded-xl bg-gray-100/80 dark:bg-white/5 border border-gray-200 dark:border-white/10 text-gray-900 dark:text-white placeholder-gray-400
                       px-4 py-3 text-sm focus:outline-none focus:border-primary-500/50 focus:ring-2 focus:ring-primary-500/20 transition-all duration-200"
            placeholder="+225 07 00 00 00 00"
            {...register('phone')}
          />
        </div>

        <div className="grid grid-cols-2 gap-3">
          <div>
            <label htmlFor="password" className="block text-sm font-medium text-gray-600 dark:text-gray-300 mb-1.5">
              {t('auth.password')}
            </label>
            <input
              id="password"
              type="password"
              autoComplete="new-password"
              className={`w-full rounded-xl bg-gray-100/80 dark:bg-white/5 border border-gray-200 dark:border-white/10 text-gray-900 dark:text-white placeholder-gray-400
                         px-4 py-3 text-sm focus:outline-none focus:border-primary-500/50 focus:ring-2 focus:ring-primary-500/20 transition-all duration-200
                         ${errors.password ? 'border-red-500/50 focus:border-red-500 focus:ring-red-500/20' : ''}`}
              placeholder={t('auth.passwordMin')}
              {...register('password')}
            />
            {errors.password && <p className="mt-1 text-xs text-red-400">{errors.password.message}</p>}
          </div>
          <div>
            <label htmlFor="confirmPassword" className="block text-sm font-medium text-gray-600 dark:text-gray-300 mb-1.5">
              {t('auth.register')} ✓
            </label>
            <input
              id="confirmPassword"
              type="password"
              autoComplete="new-password"
              className={`w-full rounded-xl bg-gray-100/80 dark:bg-white/5 border border-gray-200 dark:border-white/10 text-gray-900 dark:text-white placeholder-gray-400
                         px-4 py-3 text-sm focus:outline-none focus:border-primary-500/50 focus:ring-2 focus:ring-primary-500/20 transition-all duration-200
                         ${errors.confirmPassword ? 'border-red-500/50 focus:border-red-500 focus:ring-red-500/20' : ''}`}
              placeholder="••••••••"
              {...register('confirmPassword')}
            />
            {errors.confirmPassword && <p className="mt-1 text-xs text-red-400">{errors.confirmPassword.message}</p>}
          </div>
        </div>

        {/* Consentements RGPD explicites (art. 7 & 9) — cases vides par défaut */}
        <div className="space-y-2.5 rounded-xl border border-gray-200 dark:border-white/10 bg-gray-50/60 dark:bg-white/5 p-4">
          {([
            { field: 'consentCgu', label: "J'accepte les", link: '/legal/CGU', doc: 'conditions générales d\u2019utilisation', version: versionOf('CGU') },
            { field: 'consentPrivacy', label: 'J\u2019accepte la', link: '/legal/PRIVACY', doc: 'politique de confidentialité', version: versionOf('PRIVACY') },
            { field: 'consentArt9', label: 'Je consens au traitement de mes', link: '/legal/CONSENT_ART9', doc: 'données religieuses (RGPD art. 9)', version: versionOf('CONSENT_ART9') },
          ] as const).map((item) => (
            <div key={item.field} className="flex items-start gap-2.5">
              <input
                id={item.field}
                type="checkbox"
                className="mt-0.5 h-4 w-4 rounded border-gray-300 bg-white text-primary-600 focus:ring-primary-500/40 dark:border-white/20 dark:bg-white/5"
                {...register(item.field)}
              />
              <label htmlFor={item.field} className="text-xs text-gray-600 dark:text-gray-300 leading-relaxed">
                {item.label}{' '}
                <Link to={item.link} target="_blank" className="font-medium text-primary-600 dark:text-primary-400 hover:underline">
                  {item.doc}
                </Link>
                {item.version ? ` (v${item.version})` : ''}
              </label>
            </div>
          ))}
          {(['consentCgu', 'consentPrivacy', 'consentArt9'] as const).map((field) => (
            errors[field] ? <p key={field} className="text-xs text-red-400">{errors[field]?.message as string}</p> : null
          ))}
        </div>

        <button
          type="submit"
          disabled={isSubmitting}
          className="relative w-full py-3 px-4 rounded-xl bg-gradient-to-r from-primary-600 to-primary-500 text-white font-medium text-sm
                     hover:from-primary-500 hover:to-primary-400 focus:outline-none focus:ring-2 focus:ring-primary-500/40
                     disabled:opacity-50 disabled:cursor-not-allowed transition-all duration-200 shadow-lg shadow-primary-500/25
                     hover:shadow-primary-500/40 active:scale-[0.98] flex items-center justify-center gap-2"
        >
          {isSubmitting ? (
            <><Loader2 className="w-4 h-4 animate-spin" /> {t('auth.register')}...</>
          ) : (
            <><UserPlus className="w-4 h-4" /> {t('auth.register')}</>
          )}
        </button>
      </form>

      {/* Login link */}
      <div className="text-center animate-slide-up" style={{ animationDelay: '150ms' }}>
        <Link to="/login" className="inline-flex items-center gap-1.5 text-sm text-gray-500 dark:text-gray-400 hover:text-primary-500 dark:hover:text-primary-400 transition-colors">
          {t('auth.haveAccount')} <span className="text-primary-500 font-medium">{t('auth.loginNow')}</span>
        </Link>
      </div>

      {/* B3 — Suivi de la demande : on pré-remplit l'email si l'utilisateur en a
          déjà saisi un, pour lui éviter de le ressaisir. */}
      <div className="text-center animate-slide-up" style={{ animationDelay: '175ms' }}>
        <Link
          to={(() => {
            // getValues() renvoie undefined tant que le champ n'a jamais été touché.
            const current = (getValues('email') ?? '').trim();
            return current ? `/registration-status?email=${encodeURIComponent(current)}` : '/registration-status';
          })()}
          className="inline-flex items-center gap-1.5 text-sm text-gray-500 dark:text-gray-400 hover:text-primary-500 dark:hover:text-primary-400 transition-colors min-h-[44px]"
        >
          {tText('Suivre ma demande')}
        </Link>
      </div>

      {/* Activation notice */}
      <div className="animate-fade-in p-3.5 rounded-xl bg-blue-500/5 border border-blue-500/15 flex items-start gap-2" style={{ animationDelay: '200ms' }}>
        <MailCheck className="w-4 h-4 text-blue-400 mt-0.5 shrink-0" />
        <p className="text-xs text-blue-600 dark:text-blue-300 text-left">
          {t('auth.registerSuccess')}
        </p>
      </div>
    </div>
  );
}