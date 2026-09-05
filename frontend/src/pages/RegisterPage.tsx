import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import api, { getErrorMessage } from '@/lib/api';
import { useI18n } from '@/i18n';
import { Loader2, UserPlus, MailCheck, ShieldCheck, ArrowRight, CheckCircle2 } from 'lucide-react';

const registerSchema = z.object({
  firstName: z.string().min(1, 'Requis'),
  lastName: z.string().min(1, 'Requis'),
  email: z.string().email('Email invalide').min(1, 'Email requis'),
  phone: z.string().optional(),
  password: z.string().min(8, 'Au moins 8 caractères'),
  confirmPassword: z.string(),
}).refine((d) => d.password === d.confirmPassword, {
  message: 'Les mots de passe ne correspondent pas',
  path: ['confirmPassword'],
});

type RegisterForm = z.infer<typeof registerSchema>;

export default function RegisterPage() {
  const navigate = useNavigate();
  const { t } = useI18n();
  const [error, setError] = useState('');
  const [success, setSuccess] = useState(false);

  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<RegisterForm>({
    resolver: zodResolver(registerSchema),
  });

  const onSubmit = async (data: RegisterForm) => {
    try {
      setError('');
      await api.post('/auth/register', {
        email: data.email.trim(),
        password: data.password,
        firstName: data.firstName.trim(),
        lastName: data.lastName.trim(),
        phone: data.phone?.trim() || undefined,
      });
      setSuccess(true);
    } catch (err) {
      setError(getErrorMessage(err));
    }
  };

  if (success) {
    return (
      <div className="space-y-6 text-center animate-slide-up">
        <div className="inline-flex items-center justify-center w-16 h-16 rounded-2xl bg-green-500/10 border border-green-500/20 mb-4">
          <CheckCircle2 className="w-8 h-8 text-green-400" />
        </div>
        <h2 className="text-2xl font-bold text-gray-900 dark:text-white font-display">
          {t('auth.registerSuccess')}
        </h2>
        <p className="text-sm text-gray-500 dark:text-gray-400">
          {t('auth.registerRoleHint')}
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
      </div>

      {/* Error */}
      {error && (
        <div className="animate-slide-up p-3.5 rounded-xl bg-red-500/10 border border-red-500/20 backdrop-blur-sm">
          <p className="text-sm text-red-600 dark:text-red-300">{error}</p>
        </div>
      )}

      {/* Form */}
      <form onSubmit={handleSubmit(onSubmit)} className="space-y-4 animate-slide-up" style={{ animationDelay: '100ms' }}>
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