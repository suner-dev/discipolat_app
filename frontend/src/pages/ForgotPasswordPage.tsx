import { useState } from 'react';
import { Link } from 'react-router-dom';
import { useMutation } from '@tanstack/react-query';
import api, { getErrorMessage } from '@/lib/api';
import { Mail, Loader2, ArrowLeft, CheckCircle2, Sparkles, Shield } from 'lucide-react';

export default function ForgotPasswordPage() {
  const [email, setEmail] = useState('');
  const [success, setSuccess] = useState(false);

  const mutation = useMutation({
    mutationFn: async () => {
      await api.post('/auth/forgot-password', { email });
    },
    onSuccess: () => setSuccess(true),
  });

  if (success) {
    return (
      <div className="space-y-6 animate-slide-up">
        <div className="text-center">
          <div className="inline-flex items-center justify-center w-16 h-16 rounded-2xl bg-green-500/10 border border-green-500/20 mb-4">
            <CheckCircle2 className="w-8 h-8 text-green-400" />
          </div>
          <h2 className="text-xl font-bold text-white font-display">Email envoyé !</h2>
          <p className="mt-2 text-sm text-gray-400 leading-relaxed">
            Si un compte existe avec cette adresse, vous recevrez un email avec un lien
            de réinitialisation valable <strong className="text-gray-300">30 minutes</strong>.
          </p>
        </div>
        <div className="divider-glow my-4" />
        <Link to="/login" className="w-full py-3 px-4 rounded-xl bg-gradient-to-r from-primary-600 to-primary-500 text-white font-medium text-sm
                     hover:from-primary-500 hover:to-primary-400 transition-all duration-200
                     shadow-lg shadow-primary-500/25 hover:shadow-primary-500/40
                     flex items-center justify-center gap-2">
          <ArrowLeft className="w-4 h-4" />
          Retour à la connexion
        </Link>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      <div className="text-center">
        <div className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full bg-primary-500/10 border border-primary-500/20 text-primary-400 text-xs font-medium mb-4 animate-fade-in">
          <Shield className="w-3 h-3" />
          Mot de passe
        </div>
        <h2 className="text-2xl font-bold text-white font-display animate-slide-up">
          Mot de passe oublié
        </h2>
        <p className="mt-1.5 text-sm text-gray-400 animate-slide-up" style={{ animationDelay: '50ms' }}>
          Recevez un lien de réinitialisation par email
        </p>
      </div>

      <form
        onSubmit={(e) => { e.preventDefault(); mutation.mutate(); }}
        className="space-y-5 animate-slide-up"
        style={{ animationDelay: '100ms' }}
      >
        {mutation.isError && (
          <div className="p-3.5 rounded-xl bg-red-500/10 border border-red-500/20 backdrop-blur-sm">
            <p className="text-sm text-red-300">{getErrorMessage(mutation.error)}</p>
          </div>
        )}

        <div>
          <label htmlFor="email" className="block text-sm font-medium text-gray-300 mb-1.5">Adresse email</label>
          <div className="relative group">
            <Mail className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-500 group-focus-within:text-primary-400 transition-colors" />
            <input
              id="email"
              type="email"
              autoComplete="email"
              className="w-full rounded-xl bg-white/5 border border-white/10 text-white placeholder-gray-500
                         pl-10 pr-4 py-3 text-sm
                         focus:outline-none focus:border-primary-500/50 focus:ring-2 focus:ring-primary-500/20
                         transition-all duration-200"
              placeholder="vous@email.com"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              required
            />
          </div>
        </div>

        <button
          type="submit"
          disabled={!email || mutation.isPending}
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
            <Mail className="w-4 h-4" />
          )}
          {mutation.isPending ? 'Envoi...' : 'Envoyer le lien'}
        </button>

        <Link
          to="/login"
          className="flex items-center justify-center gap-2 text-sm text-gray-400 hover:text-primary-400 transition-colors group"
        >
          <ArrowLeft className="w-3.5 h-3.5 group-hover:-translate-x-1 transition-transform" />
          Retour à la connexion
        </Link>
      </form>
    </div>
  );
}
