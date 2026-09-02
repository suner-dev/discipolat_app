import { useState } from 'react';
import { useSearchParams, useNavigate, Link } from 'react-router-dom';
import { useMutation } from '@tanstack/react-query';
import api, { getErrorMessage } from '@/lib/api';
import { Shield, Loader2, CheckCircle2, Mail } from 'lucide-react';

export default function ActivateAccountPage() {
  const [searchParams] = useSearchParams();
  const token = searchParams.get('token');
  const navigate = useNavigate();

  const [email, setEmail] = useState('');
  const [error, setError] = useState('');
  const [success, setSuccess] = useState(false);
  const [resendSuccess, setResendSuccess] = useState(false);

  const activateMutation = useMutation({
    mutationFn: async () => { await api.post('/auth/activate', { token }); },
    onSuccess: () => setSuccess(true),
    onError: (err) => setError(getErrorMessage(err)),
  });

  const resendMutation = useMutation({
    mutationFn: async () => { await api.post('/auth/resend-activation', { email: email.trim() }); },
    onSuccess: () => setResendSuccess(true),
    onError: (err) => setError(getErrorMessage(err)),
  });

  if (token) {
    if (success) {
      return (
        <div className="space-y-6 text-center animate-slide-up">
          <div className="inline-flex items-center justify-center w-16 h-16 rounded-2xl bg-green-500/10 border border-green-500/20 mb-4">
            <CheckCircle2 className="w-8 h-8 text-green-400" />
          </div>
          <h2 className="text-xl font-bold text-gray-900 dark:text-white font-display">Compte activé !</h2>
          <p className="text-sm text-gray-500 dark:text-gray-400">Votre compte a été activé avec succès. Vous pouvez vous connecter.</p>
          <button onClick={() => navigate('/login')}
            className="w-full py-3 px-4 rounded-xl bg-gradient-to-r from-primary-600 to-primary-500 text-white font-medium text-sm hover:from-primary-500 hover:to-primary-400 transition-all duration-200 shadow-lg shadow-primary-500/25 flex items-center justify-center gap-2">
            Se connecter
          </button>
        </div>
      );
    }
    return (
      <div className="max-w-md mx-auto mt-12 p-8 rounded-2xl bg-white/60 dark:bg-gray-800/40 backdrop-blur-xl border border-white/20 dark:border-white/10 shadow-xl">
        <div className="space-y-6 text-center animate-slide-up">
          <div className="inline-flex items-center justify-center w-16 h-16 rounded-2xl bg-primary-500/10 border border-primary-500/20 mb-4">
            <Shield className="w-8 h-8 text-primary-400" />
          </div>
          <h2 className="text-xl font-bold text-gray-900 dark:text-white font-display">Activation de votre compte</h2>
          <p className="text-sm text-gray-500 dark:text-gray-400">Cliquez pour activer votre compte.</p>
          {error && (
            <div className="p-3.5 rounded-xl bg-red-500/10 border border-red-500/20 backdrop-blur-sm">
              <p className="text-sm text-red-600 dark:text-red-300">{error}</p>
            </div>
          )}
          <button onClick={() => { setError(''); activateMutation.mutate(); }} disabled={activateMutation.isPending}
            className="w-full py-3 px-4 rounded-xl bg-gradient-to-r from-primary-600 to-primary-500 text-white font-medium text-sm hover:from-primary-500 hover:to-primary-400 disabled:opacity-50 disabled:cursor-not-allowed transition-all duration-200 shadow-lg shadow-primary-500/25 flex items-center justify-center gap-2">
            {activateMutation.isPending ? <Loader2 className="w-4 h-4 animate-spin" /> : <Shield className="w-4 h-4" />}
            {activateMutation.isPending ? 'Activation...' : 'Activer mon compte'}
          </button>
        </div>
      </div>
    );
  }

  // Pas de token → formulaire de renvoi du mail d'activation
  return (
    <div className="max-w-md mx-auto mt-12 p-8 rounded-2xl bg-white/60 dark:bg-gray-800/40 backdrop-blur-xl border border-white/20 dark:border-white/10 shadow-xl">
      <div className="space-y-6 text-center animate-slide-up">
        <div className="inline-flex items-center justify-center w-16 h-16 rounded-2xl bg-amber-500/10 border border-amber-500/20 mb-4">
          <Mail className="w-8 h-8 text-amber-400" />
        </div>
        <h2 className="text-xl font-bold text-gray-900 dark:text-white font-display">Renvoyer le mail d'activation</h2>
        <p className="text-sm text-gray-500 dark:text-gray-400">Saisissez votre email pour recevoir un nouveau lien d'activation.</p>
        {error && (
          <div className="p-3.5 rounded-xl bg-red-500/10 border border-red-500/20 backdrop-blur-sm">
            <p className="text-sm text-red-600 dark:text-red-300">{error}</p>
          </div>
        )}
        {resendSuccess && (
          <div className="p-3.5 rounded-xl bg-green-500/10 border border-green-500/20 backdrop-blur-sm">
            <p className="text-sm text-green-600 dark:text-green-300">Si l'email existe, un nouveau lien d'activation a été envoyé.</p>
          </div>
        )}
        <form onSubmit={(e) => { e.preventDefault(); setError(''); setResendSuccess(false); resendMutation.mutate(); }} className="space-y-4 text-left">
          <div>
            <label htmlFor="email" className="block text-sm font-medium text-gray-600 dark:text-gray-300 mb-1.5">Email</label>
            <input id="email" type="email" required value={email} onChange={e => setEmail(e.target.value)}
              className="w-full rounded-xl bg-gray-100/80 dark:bg-white/5 border border-gray-200 dark:border-white/10 text-gray-900 dark:text-white placeholder-gray-400 dark:placeholder-gray-500 px-4 py-3 text-sm focus:outline-none focus:border-primary-500/50 focus:ring-2 focus:ring-primary-500/20 transition-all duration-200"
              placeholder="votre@email.com" />
          </div>
          <button type="submit" disabled={!email || resendMutation.isPending}
            className="w-full py-3 px-4 rounded-xl bg-gradient-to-r from-primary-600 to-primary-500 text-white font-medium text-sm hover:from-primary-500 hover:to-primary-400 disabled:opacity-50 disabled:cursor-not-allowed transition-all duration-200 shadow-lg shadow-primary-500/25 flex items-center justify-center gap-2">
            {resendMutation.isPending ? <Loader2 className="w-4 h-4 animate-spin" /> : <Mail className="w-4 h-4" />}
            {resendMutation.isPending ? 'Envoi...' : 'Renvoyer le mail'}
          </button>
        </form>
        <Link to="/login" className="inline-block text-sm text-primary-600 hover:text-primary-500 mt-2">← Retour à la connexion</Link>
      </div>
    </div>
  );
}