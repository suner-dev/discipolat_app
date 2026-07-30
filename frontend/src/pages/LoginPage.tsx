import { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { useAuth, roleLabels } from '@/contexts/AuthContext';
import { getErrorMessage } from '@/lib/api';
import { Eye, EyeOff, Loader2, LogIn, HelpCircle, Sparkles, Shield, RotateCw, ShieldCheck } from 'lucide-react';

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

export default function LoginPage() {
  const navigate = useNavigate();
  const { login, roles, user, switchRole } = useAuth();
  const [showPassword, setShowPassword] = useState(false);
  const [error, setError] = useState('');
  const [showRoleSelector, setShowRoleSelector] = useState(false);
  const [selectedRole, setSelectedRole] = useState<string | null>(null);
  const [roleLoading, setRoleLoading] = useState(false);

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
          <h2 className="text-2xl font-bold text-white font-display animate-slide-up">
            Bienvenue, {user.firstName} 🙌
          </h2>
          <p className="mt-1.5 text-sm text-gray-400 animate-slide-up" style={{ animationDelay: '50ms' }}>
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
                  <p className="text-base font-semibold text-white">{roleLabels[r] || r}</p>
                  <p className="text-sm text-gray-400 mt-0.5">{roleDescriptions[r] || ''}</p>
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

        <p className="text-xs text-gray-500 text-center">
          Vous pourrez changer de rôle à tout moment depuis le menu
        </p>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="text-center">
        <div className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full bg-primary-500/10 border border-primary-500/20 text-primary-400 text-xs font-medium mb-4 animate-fade-in">
          <Shield className="w-3 h-3" />
          Espace sécurisé
        </div>
        <h2 className="text-2xl font-bold text-white font-display animate-slide-up">
          Connexion
        </h2>
        <p className="mt-1.5 text-sm text-gray-400 animate-slide-up" style={{ animationDelay: '50ms' }}>
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
            <p className="text-sm text-red-300">{error}</p>
          </div>
        </div>
      )}

      {/* Form */}
      <form onSubmit={handleSubmit(onSubmit)} className="space-y-5 animate-slide-up" style={{ animationDelay: '100ms' }}>
        <div>
          <label htmlFor="email" className="block text-sm font-medium text-gray-300 mb-1.5">
            Adresse email
          </label>
          <div className="relative group">
            <input
              id="email"
              type="email"
              autoComplete="email"
              className={`w-full rounded-xl bg-white/5 border border-white/10 text-white placeholder-gray-500
                         px-4 py-3 text-sm
                         focus:outline-none focus:border-primary-500/50 focus:ring-2 focus:ring-primary-500/20
                         transition-all duration-200
                         group-hover:border-white/20
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
            <label htmlFor="password" className="text-sm font-medium text-gray-300">
              Mot de passe
            </label>
          </div>
          <div className="relative group">
            <input
              id="password"
              type={showPassword ? 'text' : 'password'}
              autoComplete="current-password"
              className={`w-full rounded-xl bg-white/5 border border-white/10 text-white placeholder-gray-500
                         px-4 py-3 pr-12 text-sm
                         focus:outline-none focus:border-primary-500/50 focus:ring-2 focus:ring-primary-500/20
                         transition-all duration-200
                         group-hover:border-white/20
                         ${errors.password ? 'border-red-500/50 focus:border-red-500 focus:ring-red-500/20' : ''}`}
              placeholder="••••••••"
              {...register('password')}
            />
            <button
              type="button"
              onClick={() => setShowPassword(!showPassword)}
              className="absolute right-3 top-1/2 -translate-y-1/2 text-gray-500 hover:text-gray-300 transition-colors"
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
            className="inline-flex items-center gap-1.5 text-sm text-gray-400 hover:text-primary-400 transition-colors group"
          >
            <HelpCircle className="w-3.5 h-3.5 group-hover:rotate-12 transition-transform" />
            Mot de passe oublié ?
          </Link>
        </div>
      </form>

      {/* Divider */}
      <div className="relative animate-fade-in" style={{ animationDelay: '200ms' }}>
        <div className="absolute inset-0 flex items-center">
          <div className="w-full border-t border-white/5" />
        </div>
        <div className="relative flex justify-center">
          <span className="px-3 text-xs text-gray-500 bg-gray-900">Démonstration</span>
        </div>
      </div>

      {/* Demo accounts */}
      <div className="animate-slide-up" style={{ animationDelay: '250ms' }}>
        <div className="p-3.5 rounded-xl bg-white/[0.03] border border-white/5">
          <div className="flex items-center gap-2 mb-2">
            <Sparkles className="w-3.5 h-3.5 text-gold-400" />
            <span className="text-xs font-medium text-gold-400">Comptes de test</span>
          </div>
          <div className="space-y-1.5">
            {[
              { role: 'Pasteur', email: 'pasteur@discipolat.com' },
              { role: 'Responsable', email: 'responsable@discipolat.com' },
              { role: 'Chef de famille', email: 'chef@discipolat.com' },
              { role: 'Faiseur', email: 'faiseur@discipolat.com' },
            ].map((account) => (
              <div key={account.email} className="flex items-center justify-between text-xs">
                <span className="text-gray-400">{account.role}</span>
                <code className="text-gray-500 font-mono text-[11px]">{account.email}</code>
              </div>
            ))}
          </div>
          <p className="text-[11px] text-gray-600 mt-2 text-center">Mot de passe : <code className="text-gray-500 font-mono">password123</code></p>
        </div>
      </div>
    </div>
  );
}
