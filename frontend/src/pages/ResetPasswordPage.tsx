import { useState } from 'react';
import { useSearchParams, useNavigate, Link } from 'react-router-dom';
import { useMutation } from '@tanstack/react-query';
import api, { getErrorMessage } from '@/lib/api';
import { Lock, Loader2, CheckCircle2, Eye, EyeOff, Shield, AlertTriangle } from 'lucide-react';

export default function ResetPasswordPage() {
  const [searchParams] = useSearchParams();
  const token = searchParams.get('token');
  const navigate = useNavigate();

  const [password, setPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState(false);

  const mutation = useMutation({
    mutationFn: async () => {
      await api.post('/auth/reset-password', { token, newPassword: password });
    },
    onSuccess: () => setSuccess(true),
    onError: (err) => setError(getErrorMessage(err)),
  });

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    setError('');

    if (password.length < 8) {
      setError('Le mot de passe doit contenir au moins 8 caractères');
      return;
    }
    if (!/(?=.*[a-z])(?=.*[A-Z])(?=.*\d)/.test(password)) {
      setError('Le mot de passe doit contenir au moins une majuscule, une minuscule et un chiffre');
      return;
    }
    if (password !== confirmPassword) {
      setError('Les mots de passe ne correspondent pas');
      return;
    }

    mutation.mutate();
  };

  // Invalid token
  if (!token) {
    return (
      <div className="space-y-6 text-center animate-slide-up">
        <div className="inline-flex items-center justify-center w-16 h-16 rounded-2xl bg-red-500/10 border border-red-500/20 mb-4">
          <AlertTriangle className="w-8 h-8 text-red-400" />
        </div>
        <h2 className="text-xl font-bold text-white font-display">Lien invalide</h2>
        <p className="text-sm text-gray-400">Ce lien de réinitialisation est invalide ou a expiré.</p>
        <div className="divider-glow my-4" />
        <Link to="/forgot-password"
          className="w-full py-3 px-4 rounded-xl bg-gradient-to-r from-primary-600 to-primary-500 text-white font-medium text-sm
                     hover:from-primary-500 hover:to-primary-400 transition-all duration-200 shadow-lg shadow-primary-500/25
                     flex items-center justify-center gap-2">
          Renvoyer un lien
        </Link>
      </div>
    );
  }

  // Success
  if (success) {
    return (
      <div className="space-y-6 text-center animate-slide-up">
        <div className="inline-flex items-center justify-center w-16 h-16 rounded-2xl bg-green-500/10 border border-green-500/20 mb-4">
          <CheckCircle2 className="w-8 h-8 text-green-400" />
        </div>
        <h2 className="text-xl font-bold text-white font-display">Mot de passe réinitialisé !</h2>
        <p className="text-sm text-gray-400">Votre mot de passe a été modifié avec succès.</p>
        <div className="divider-glow my-4" />
        <button onClick={() => navigate('/login')}
          className="w-full py-3 px-4 rounded-xl bg-gradient-to-r from-primary-600 to-primary-500 text-white font-medium text-sm
                     hover:from-primary-500 hover:to-primary-400 transition-all duration-200 shadow-lg shadow-primary-500/25
                     flex items-center justify-center gap-2">
          <Lock className="w-4 h-4" />
          Se connecter
        </button>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      <div className="text-center">
        <div className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full bg-primary-500/10 border border-primary-500/20 text-primary-400 text-xs font-medium mb-4 animate-fade-in">
          <Shield className="w-3 h-3" />
          Sécurité
        </div>
        <h2 className="text-2xl font-bold text-white font-display animate-slide-up">
          Nouveau mot de passe
        </h2>
        <p className="mt-1.5 text-sm text-gray-400 animate-slide-up" style={{ animationDelay: '50ms' }}>
          Choisissez un mot de passe sécurisé
        </p>
      </div>

      <form onSubmit={handleSubmit} className="space-y-5 animate-slide-up" style={{ animationDelay: '100ms' }}>
        {error && (
          <div className="p-3.5 rounded-xl bg-red-500/10 border border-red-500/20 backdrop-blur-sm">
            <p className="text-sm text-red-300">{error}</p>
          </div>
        )}

        <div>
          <label htmlFor="password" className="block text-sm font-medium text-gray-300 mb-1.5">Nouveau mot de passe</label>
          <div className="relative group">
            <input
              id="password"
              type={showPassword ? 'text' : 'password'}
              autoComplete="new-password"
              className="w-full rounded-xl bg-white/5 border border-white/10 text-white placeholder-gray-500
                         px-4 py-3 pr-12 text-sm
                         focus:outline-none focus:border-primary-500/50 focus:ring-2 focus:ring-primary-500/20
                         transition-all duration-200"
              placeholder="Minimum 8 caractères"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              required
            />
            <button
              type="button"
              onClick={() => setShowPassword(!showPassword)}
              className="absolute right-3 top-1/2 -translate-y-1/2 text-gray-500 hover:text-gray-300 transition-colors"
            >
              {showPassword ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
            </button>
          </div>
        </div>

        <div>
          <label htmlFor="confirmPassword" className="block text-sm font-medium text-gray-300 mb-1.5">Confirmer le mot de passe</label>
          <input
            id="confirmPassword"
            type="password"
            autoComplete="new-password"
            className="w-full rounded-xl bg-white/5 border border-white/10 text-white placeholder-gray-500
                       px-4 py-3 text-sm
                       focus:outline-none focus:border-primary-500/50 focus:ring-2 focus:ring-primary-500/20
                       transition-all duration-200"
            placeholder="Confirmez le mot de passe"
            value={confirmPassword}
            onChange={(e) => setConfirmPassword(e.target.value)}
            required
          />
        </div>

        <button
          type="submit"
          disabled={!password || !confirmPassword || mutation.isPending}
          className="w-full py-3 px-4 rounded-xl bg-gradient-to-r from-primary-600 to-primary-500 text-white font-medium text-sm
                     hover:from-primary-500 hover:to-primary-400
                     disabled:opacity-50 disabled:cursor-not-allowed
                     transition-all duration-200
                     shadow-lg shadow-primary-500/25 hover:shadow-primary-500/40
                     active:scale-[0.98]
                     flex items-center justify-center gap-2"
        >
          {mutation.isPending ? (
            <Loader2 className="w-4 h-4 animate-spin" />
          ) : (
            <Lock className="w-4 h-4" />
          )}
          {mutation.isPending ? 'Réinitialisation...' : 'Réinitialiser le mot de passe'}
        </button>
      </form>
    </div>
  );
}
