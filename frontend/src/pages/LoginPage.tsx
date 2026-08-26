import { useState, useEffect, useRef } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { useAuth, roleLabels } from '@/contexts/AuthContext';
import { usePlatformMeta } from '@/contexts/MetaContext';
import BetaBadge from '@/components/beta/BetaBadge';
import api, { getErrorMessage } from '@/lib/api';
import { Eye, EyeOff, Loader2, LogIn, HelpCircle, Shield, RotateCw, ShieldCheck, FlaskConical, Wand2, MailCheck } from 'lucide-react';
import { useI18n } from '@/i18n';

const loginSchema = z.object({
  email: z.string().email('Email invalide').min(1, 'Email requis'),
  password: z.string().min(1, 'Mot de passe requis'),
});

type LoginForm = z.infer<typeof loginSchema>;

const roleIcons: Record<string, string> = {
  PASTEUR: '👑',
  RESPONSABLE: '📋',
  FAISEUR: '🌱',
  CHEF_DE_FAMILLE: '👪',
  ADMIN: '⚙️',
  MEMBRE: '🙏',
};

const roleDescriptions: Record<string, string> = {
  PASTEUR: 'Accès complet à toute l\'église',
  RESPONSABLE: 'Gestion de votre département',
  FAISEUR: 'Suivi de vos disciples',
  CHEF_DE_FAMILLE: 'Coordination de votre famille',
  ADMIN: 'Administration technique',
  MEMBRE: 'Espace membre',
};

const roleColor = (r: string) => {
  switch (r) {
    case 'ADMIN': return 'from-red-600 to-red-500';
    case 'PASTEUR': return 'from-primary-600 to-primary-500';
    case 'RESPONSABLE': return 'from-amber-600 to-amber-500';
    case 'FAISEUR': return 'from-emerald-600 to-emerald-500';
    case 'CHEF_DE_FAMILLE': return 'from-gold-600 to-gold-500';
    default: return 'from-gray-600 to-gray-500';
  }
};

const roleBg = (r: string) => {
  switch (r) {
    case 'ADMIN': return 'bg-red-500/10 border-red-500/20 hover:bg-red-500/20';
    case 'PASTEUR': return 'bg-primary-500/10 border-primary-500/20 hover:bg-primary-500/20';
    case 'RESPONSABLE': return 'bg-amber-500/10 border-amber-500/20 hover:bg-amber-500/20';
    case 'FAISEUR': return 'bg-emerald-500/10 border-emerald-500/20 hover:bg-emerald-500/20';
    case 'CHEF_DE_FAMILLE': return 'bg-gold-500/10 border-gold-500/20 hover:bg-gold-500/20';
    default: return 'bg-gray-500/10 border-gray-500/20 hover:bg-gray-500/20';
  }
};

const DEMO_ACCOUNTS = [
  { role: 'Admin (multi-rôles)', email: 'admin@discipolat.com' },
  { role: 'Pasteur', email: 'pasteur@discipolat.com' },
  { role: 'Responsable (multi-rôles)', email: 'responsable@discipolat.com' },
  { role: 'Chef de famille', email: 'chef@discipolat.com' },
  { role: 'Faiseur', email: 'faiseur@discipolat.com' },
  { role: 'Membre', email: 'membre@discipolat.com' },
  { role: 'Multi-rôles', email: 'paul@discipolat.com' },
];

export default function LoginPage() {
  const navigate = useNavigate();
  const { login, roles, user, switchRole, isAuthenticated, loginWithSocialToken } = useAuth();
  const { meta } = usePlatformMeta();
  const { t } = useI18n();
  const [showPassword, setShowPassword] = useState(false);
  const [error, setError] = useState('');
  const [showRoleSelector, setShowRoleSelector] = useState(false);
  const [selectedRole, setSelectedRole] = useState<string | null>(null);
  const [roleLoading, setRoleLoading] = useState(false);

  // ── Magic link (connexion sans mot de passe) ──
  const [showMagicLink, setShowMagicLink] = useState(false);
  const [magicEmail, setMagicEmail] = useState('');
  const [magicPending, setMagicPending] = useState(false);
  const [magicSent, setMagicSent] = useState(false);

  const sendMagicLink = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!magicEmail.includes('@')) return;
    setMagicPending(true);
    try {
      await api.post('/auth/magic-link', { email: magicEmail.trim() });
      // Réponse volontairement identique que l'email existe ou non (anti-énumération).
      setMagicSent(true);
    } catch {
      setMagicSent(true);
    } finally {
      setMagicPending(false);
    }
  };

  // Marqueur : la navigation post-connexion a déjà été décidée par le formulaire
  // (dashboard, 2FA, sélecteur de rôle). Empêche le garde ci-dessous d'écraser
  // une navigation plus spécifique (ex. /verify-2fa) quand isAuthenticated passe
  // à true au même instant que la soumission.
  const navigationHandledRef = useRef(false);

  // Already authenticated (and not in the middle of choosing a role) → go to dashboard
  useEffect(() => {
    if (isAuthenticated && !showRoleSelector && !navigationHandledRef.current) {
      navigate('/dashboard', { replace: true });
    }
  }, [isAuthenticated, showRoleSelector, navigate]);

  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<LoginForm>({
    resolver: zodResolver(loginSchema),
  });

  const onSubmit = async (data: LoginForm) => {
    try {
      setError('');
      const result = await login(data);

      // Check if user has multiple roles — show role selector
      if (result.roles && result.roles.length > 1 && !result.twoFactorEnabled) {
        setShowRoleSelector(true);
        return;
      }

      // La navigation est décidée ici : le garde useEffect ne doit pas la remplacer.
      navigationHandledRef.current = true;

      if (result?.twoFactorEnabled) {
        navigate('/verify-2fa');
      } else {
        navigate('/dashboard');
      }
    } catch (err) {
      setError(getErrorMessage(err));
    }
  };

  const handleSelectRole = async (role: string) => {
    setSelectedRole(role);
    setRoleLoading(true);
    try {
      await switchRole(role);
      navigate('/dashboard');
    } catch {
      setError('Échec de la sélection du rôle');
      setRoleLoading(false);
    }
  };

  if (showRoleSelector && user && roles.length > 1) {
    return (
      <div className="space-y-6">
        {/* Header */}
        <div className="text-center">
          <div className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full bg-gold-500/10 border border-gold-500/20 text-gold-400 text-xs font-medium mb-4 animate-fade-in">
            <RotateCw className="w-3 h-3" />
            Choisissez un rôle
          </div>
          <h2 className="text-2xl font-bold text-gray-900 dark:text-white font-display animate-slide-up">
            Bienvenue, {user.firstName} 🙌
          </h2>
          <p className="mt-1.5 text-sm text-gray-500 dark:text-gray-400 animate-slide-up" style={{ animationDelay: '50ms' }}>
            Vous avez plusieurs rôles. Sélectionnez celui à utiliser
          </p>
        </div>

        {/* Role cards */}
        <div className="space-y-3 animate-slide-up" style={{ animationDelay: '100ms' }}>
          {roles.map((r) => (
            <button
              key={r}
              onClick={() => handleSelectRole(r)}
              disabled={roleLoading && selectedRole === r}
              className={`w-full p-4 rounded-2xl border text-left transition-all duration-200
                         ${roleBg(r)}
                         ${roleLoading && selectedRole === r ? 'opacity-60 cursor-wait' : 'hover:scale-[1.02] active:scale-[0.98]'}
                         ${selectedRole === r ? 'ring-2 ring-primary-500/50' : ''}
                       `}
            >
              <div className="flex items-center gap-4">
                <div className={`w-12 h-12 rounded-xl bg-gradient-to-br ${roleColor(r)} flex items-center justify-center text-xl shadow-lg`}>
                  {roleIcons[r] || '🔘'}
                </div>
                <div className="flex-1">
                  <p className="text-base font-semibold text-gray-900 dark:text-white">{roleLabels[r] || r}</p>
                  <p className="text-sm text-gray-500 dark:text-gray-400 mt-0.5">{roleDescriptions[r] || ''}</p>
                </div>
                {roleLoading && selectedRole === r ? (
                  <Loader2 className="w-5 h-5 animate-spin text-primary-400" />
                ) : (
                  <ShieldCheck className="w-5 h-5 text-gray-500" />
                )}
              </div>
            </button>
          ))}
        </div>

        <p className="text-xs text-gray-400 dark:text-gray-500 text-center">
          Vous pourrez changer de rôle à tout moment depuis le menu
        </p>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="text-center">
        <div className="inline-flex items-center gap-2.5 mb-4 animate-fade-in">
          <span className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full bg-primary-500/10 border border-primary-500/20 text-primary-400 text-xs font-medium">
            <Shield className="w-3 h-3" />
            Espace sécurisé
          </span>
          <BetaBadge />
        </div>
        <h2 className="text-2xl font-bold text-gray-900 dark:text-white font-display animate-slide-up">
          Connexion
        </h2>
        <p className="mt-1.5 text-sm text-gray-500 dark:text-gray-400 animate-slide-up" style={{ animationDelay: '50ms' }}>
          Accédez à votre espace de gestion
        </p>
      </div>

      {/* Error message */}
      {error && (
        <div className="animate-slide-up p-3.5 rounded-xl bg-red-500/10 border border-red-500/20 backdrop-blur-sm">
          <div className="flex items-center gap-2">
            <div className="p-1 rounded-lg bg-red-500/20">
              <LogIn className="w-3.5 h-3.5 text-red-400" />
            </div>
            <p className="text-sm text-red-600 dark:text-red-300">{error}</p>
          </div>
        </div>
      )}

      {/* Form */}
      <form onSubmit={handleSubmit(onSubmit)} className="space-y-5 animate-slide-up" style={{ animationDelay: '100ms' }}>
        <div>
          <label htmlFor="email" className="block text-sm font-medium text-gray-600 dark:text-gray-300 mb-1.5">
            Adresse email
          </label>
          <div className="relative group">
            <input
              id="email"
              type="email"
              autoComplete="email"
              className={`w-full rounded-xl bg-gray-100/80 dark:bg-white/5 border border-gray-200 dark:border-white/10 text-gray-900 dark:text-white placeholder-gray-400 dark:placeholder-gray-500
                         px-4 py-3 text-sm
                         focus:outline-none focus:border-primary-500/50 focus:ring-2 focus:ring-primary-500/20
                         transition-all duration-200
                         group-hover:border-gray-300 dark:group-hover:border-white/20
                         ${errors.email ? 'border-red-500/50 focus:border-red-500 focus:ring-red-500/20' : ''}`}
              placeholder="vous@email.com"
              {...register('email')}
            />
          </div>
          {errors.email && (
            <p className="mt-1.5 text-xs text-red-400 flex items-center gap-1">
              <span className="w-1 h-1 rounded-full bg-red-400" />
              {errors.email.message}
            </p>
          )}
        </div>

        <div>
          <div className="flex items-center justify-between mb-1.5">
            <label htmlFor="password" className="text-sm font-medium text-gray-600 dark:text-gray-300">
              Mot de passe
            </label>
          </div>
          <div className="relative group">
            <input
              id="password"
              type={showPassword ? 'text' : 'password'}
              autoComplete="current-password"
              className={`w-full rounded-xl bg-gray-100/80 dark:bg-white/5 border border-gray-200 dark:border-white/10 text-gray-900 dark:text-white placeholder-gray-400 dark:placeholder-gray-500
                         px-4 py-3 pr-12 text-sm
                         focus:outline-none focus:border-primary-500/50 focus:ring-2 focus:ring-primary-500/20
                         transition-all duration-200
                         group-hover:border-gray-300 dark:group-hover:border-white/20
                         ${errors.password ? 'border-red-500/50 focus:border-red-500 focus:ring-red-500/20' : ''}`}
              placeholder="••••••••"
              {...register('password')}
            />
            <button
              type="button"
              onClick={() => setShowPassword(!showPassword)}
              className="absolute right-3 top-1/2 -translate-y-1/2 text-gray-400 dark:text-gray-500 hover:text-gray-700 dark:hover:text-gray-300 transition-colors"
              tabIndex={-1}
            >
              {showPassword ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
            </button>
          </div>
          {errors.password && (
            <p className="mt-1.5 text-xs text-red-400 flex items-center gap-1">
              <span className="w-1 h-1 rounded-full bg-red-400" />
              {errors.password.message}
            </p>
          )}
        </div>

        <button
          type="submit"
          disabled={isSubmitting}
          className="relative w-full py-3 px-4 rounded-xl bg-gradient-to-r from-primary-600 to-primary-500 text-white font-medium text-sm
                     hover:from-primary-500 hover:to-primary-400
                     focus:outline-none focus:ring-2 focus:ring-primary-500/40
                     disabled:opacity-50 disabled:cursor-not-allowed
                     transition-all duration-200
                     shadow-lg shadow-primary-500/25 hover:shadow-primary-500/40
                     active:scale-[0.98]
                     flex items-center justify-center gap-2"
        >
          {isSubmitting ? (
            <>
              <Loader2 className="w-4 h-4 animate-spin" />
              Connexion en cours...
            </>
          ) : (
            <>
              <LogIn className="w-4 h-4" />
              Se connecter
            </>
          )}
        </button>

        <div className="text-center">
          <Link
            to="/forgot-password"
            className="inline-flex items-center gap-1.5 text-sm text-gray-500 dark:text-gray-400 hover:text-primary-500 dark:hover:text-primary-400 transition-colors group"
          >
            <HelpCircle className="w-3.5 h-3.5 group-hover:rotate-12 transition-transform" />
            Mot de passe oublié ?
          </Link>
        </div>

        {/* Connexion sans mot de passe (magic link) */}
        <div className="animate-fade-in" style={{ animationDelay: '150ms' }}>
          {!showMagicLink ? (
            <button
              type="button"
              onClick={() => setShowMagicLink(true)}
              className="w-full flex items-center justify-center gap-2 py-2.5 rounded-xl border border-gray-200 dark:border-white/10
                         text-sm text-gray-600 dark:text-gray-300 hover:border-primary-500/40 hover:text-primary-500
                         dark:hover:text-primary-400 transition-all duration-200"
            >
              <Wand2 className="w-4 h-4" />
              Recevoir un lien de connexion par email
            </button>
          ) : magicSent ? (
            <div className="p-3.5 rounded-xl bg-green-500/10 border border-green-500/20 flex items-start gap-2">
              <MailCheck className="w-4 h-4 text-green-500 mt-0.5 shrink-0" />
              <p className="text-xs text-green-600 dark:text-green-400 text-left">
                Si cet email est enregistré, vous recevrez un lien de connexion
                (valable 15 minutes). Vérifiez votre boîte de réception.
              </p>
            </div>
          ) : (
            <form onSubmit={sendMagicLink} className="flex gap-2">
              <input
                type="email"
                required
                value={magicEmail}
                onChange={(e) => setMagicEmail(e.target.value)}
                placeholder="vous@email.com"
                autoComplete="email"
                className="flex-1 min-w-0 rounded-xl bg-gray-100/80 dark:bg-white/5 border border-gray-200 dark:border-white/10
                           text-gray-900 dark:text-white placeholder-gray-400 px-3.5 py-2.5 text-sm
                           focus:outline-none focus:border-primary-500/50 focus:ring-2 focus:ring-primary-500/20 transition-all"
              />
              <button
                type="submit"
                disabled={magicPending}
                className="shrink-0 inline-flex items-center gap-1.5 px-4 py-2.5 rounded-xl bg-gray-900 dark:bg-white/10 text-white dark:text-white
                           text-sm font-medium hover:bg-gray-800 disabled:opacity-50 transition-all"
              >
                {magicPending ? <Loader2 className="w-4 h-4 animate-spin" /> : <Wand2 className="w-4 h-4" />}
                Envoyer
              </button>
            </form>
          )}
        </div>
      </form>

      {/* Social Auth — Google + Magic Link */}
      <div className="space-y-3 animate-slide-up" style={{ animationDelay: '150ms' }}>
        <div className="relative flex items-center">
          <div className="flex-1 border-t border-gray-200 dark:border-white/5" />
          <span className="px-3 text-xs text-gray-400 dark:text-gray-500">ou</span>
          <div className="flex-1 border-t border-gray-200 dark:border-white/5" />
        </div>

        {/* Google OAuth — Google Identity Services ; le credential JWT est
            validé côté serveur (audience + email vérifié). Sans client-id
            configuré, l'utilisateur reçoit un message clair. */}
        <button
          type="button"
          onClick={async () => {
            const google = (window as unknown as {
              google?: { accounts?: { id?: {
                initialize: (cfg: { client_id: string; callback: (r: { credential: string }) => void }) => void;
                prompt: () => void;
              } } };
              DISCIPOLAT_GOOGLE_CLIENT_ID?: string;
            }).google;
            const clientId = (window as unknown as { DISCIPOLAT_GOOGLE_CLIENT_ID?: string })
              .DISCIPOLAT_GOOGLE_CLIENT_ID;
            if (!google?.accounts?.id || !clientId) {
              setError(t('auth.googleUnavailable'));
              return;
            }
            google.accounts.id.initialize({
              client_id: clientId,
              callback: async (response) => {
                try {
                  const res = await api.post('/auth/google', { credential: response.credential });
                  loginWithSocialToken(res.data.token, res.data.user, res.data.refreshToken);
                  navigate('/dashboard', { replace: true });
                } catch {
                  setError(t('auth.googleUnavailable'));
                }
              },
            });
            google.accounts.id.prompt();
          }}
          className="w-full py-3 px-4 rounded-xl border border-gray-200 dark:border-white/10 bg-white dark:bg-white/5
                     text-gray-700 dark:text-gray-300 font-medium text-sm
                     hover:bg-gray-50 dark:hover:bg-white/10 transition-all duration-200
                     flex items-center justify-center gap-3"
        >
          <svg className="w-5 h-5" viewBox="0 0 24 24">
            <path fill="#4285F4" d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92a5.06 5.06 0 0 1-2.2 3.32v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.1z"/>
            <path fill="#34A853" d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z"/>
            <path fill="#FBBC05" d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.07H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.93l2.85-2.22.81-.62z"/>
            <path fill="#EA4335" d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.07l3.66 2.84c.87-2.6 3.3-4.53 6.16-4.53z"/>
          </svg>
          {t('auth.loginWith')} Google
        </button>

        {/* Apple Sign In — P2 #2 */}
        <button
          type="button"
          onClick={() => setError('Apple Sign In — bientôt disponible')}
          className="w-full py-3 px-4 rounded-xl border border-gray-200 dark:border-white/10 bg-black dark:bg-white/10
                     text-white dark:text-gray-200 font-medium text-sm
                     hover:bg-gray-800 dark:hover:bg-white/15 transition-all duration-200
                     flex items-center justify-center gap-3"
        >
          <svg className="w-5 h-5" viewBox="0 0 24 24" fill="currentColor">
            <path d="M17.05 20.28c-.98.95-2.05.88-3.08.4-1.09-.5-2.08-.48-3.24 0-1.44.62-2.2.44-3.06-.4C2.79 15.25 3.51 7.59 9.05 7.31c1.35.07 2.29.74 3.08.8 1.18-.24 2.31-.93 3.57-.84 1.51.12 2.65.72 3.4 1.8-3.12 1.87-2.38 5.98.48 7.13-.57 1.5-1.31 2.99-2.54 4.09zM12.03 7.25c-.15-2.23 1.66-4.07 3.74-4.25.29 2.58-2.34 4.5-3.74 4.25z"/>
          </svg>
          {t('auth.loginWith')} Apple
        </button>

        {/* Facebook Login — P2 #2 */}
        <button
          type="button"
          onClick={() => setError('Facebook Login — bientôt disponible')}
          className="w-full py-3 px-4 rounded-xl border border-gray-200 dark:border-white/10 bg-[#1877F2] dark:bg-[#1877F2]/20
                     text-white dark:text-blue-400 font-medium text-sm
                     hover:bg-[#166FE5] dark:hover:bg-[#1877F2]/30 transition-all duration-200
                     flex items-center justify-center gap-3"
        >
          <svg className="w-5 h-5" viewBox="0 0 24 24" fill="currentColor">
            <path d="M24 12.073c0-6.627-5.373-12-12-12s-12 5.373-12 12c0 5.99 4.388 10.954 10.125 11.854v-8.385H7.078v-3.47h3.047V9.43c0-3.007 1.792-4.669 4.533-4.669 1.312 0 2.686.235 2.686.235v2.953H15.83c-1.491 0-1.956.925-1.956 1.874v2.25h3.328l-.532 3.47h-2.796v8.385C19.612 23.027 24 18.062 24 12.073z"/>
          </svg>
          {t('auth.loginWith')} Facebook
        </button>
      </div>

      {/* Comptes de démonstration — visibles UNIQUEMENT si le serveur les autorise
          (profil bêta) : jamais de données de test mélangées à la production. */}
      {meta.demoAccountsEnabled && (
        <>
          {/* Divider */}
          <div className="relative animate-fade-in" style={{ animationDelay: '200ms' }}>
            <div className="absolute inset-0 flex items-center">
              <div className="w-full border-t border-gray-200 dark:border-white/5" />
            </div>
            <div className="relative flex justify-center">
              <span className="px-3 text-xs text-gray-400 dark:text-gray-500 bg-white dark:bg-gray-900">Démonstration</span>
            </div>
          </div>

          {/* Demo accounts */}
          <div className="animate-slide-up" style={{ animationDelay: '250ms' }}>
            <div className="p-3.5 rounded-xl bg-gray-100/70 dark:bg-white/[0.03] border border-gray-200/70 dark:border-white/5">
              <div className="flex items-center gap-2 mb-2">
                <FlaskConical className="w-3.5 h-3.5 text-amber-500" />
                <span className="text-xs font-medium text-amber-500">Comptes de démonstration (bêta)</span>
              </div>
              <p className="text-[11px] text-gray-500 dark:text-gray-500 mb-2">
                Données entièrement fictives — choisissez un espace et explorez librement.
              </p>
              <div className="space-y-1.5">
                {DEMO_ACCOUNTS.map((account) => (
                  <div key={account.email} className="flex items-center justify-between text-xs">
                    <span className="text-gray-500 dark:text-gray-400">{account.role}</span>
                    <code className="text-gray-400 dark:text-gray-500 font-mono text-[11px]">{account.email}</code>
                  </div>
                ))}
              </div>
              <p className="text-[11px] text-gray-500 dark:text-gray-600 mt-2 text-center">Mot de passe : <code className="text-gray-400 dark:text-gray-500 font-mono">password123</code></p>
            </div>
          </div>
        </>
      )}
    </div>
  );
}
