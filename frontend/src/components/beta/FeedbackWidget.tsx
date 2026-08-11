import { useEffect, useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import toast from 'react-hot-toast';
import api, { getErrorMessage } from '@/lib/api';
import { useAuth } from '@/contexts/AuthContext';
import { usePlatformMeta } from '@/contexts/MetaContext';
import { MessageSquareText, X, Loader2, Send, Bug, Lightbulb } from 'lucide-react';
import type { CreateFeedbackRequest, FeedbackCategory, FeedbackPriority } from '@/types';

/**
 * Widget de feedback testeur — bouton flottant (bas droite) + modale.
 *
 * Disponible pour tout utilisateur authentifié. Les informations de
 * contexte (page, navigateur, OS, appareil) sont capturées automatiquement
 * et envoyées au backend (/api/v1/feedback) pour faciliter le diagnostic.
 * Aucune donnée personnelle au-delà du compte connecté.
 */

const CATEGORIES: { value: FeedbackCategory; label: string; icon: React.ElementType }[] = [
  { value: 'BUG', label: 'Bug / problème technique', icon: Bug },
  { value: 'UX', label: 'Problème d\'utilisation (UX)', icon: MessageSquareText },
  { value: 'SUGGESTION', label: 'Suggestion d\'amélioration', icon: Lightbulb },
  { value: 'FONCTIONNALITE_MANQUANTE', label: 'Fonctionnalité manquante', icon: Lightbulb },
  { value: 'PERFORMANCE', label: 'Problème de performance', icon: MessageSquareText },
  { value: 'TRADUCTION', label: 'Problème de traduction', icon: MessageSquareText },
  { value: 'AFFICHAGE', label: 'Problème d\'affichage', icon: MessageSquareText },
  { value: 'AUTRE', label: 'Autre', icon: MessageSquareText },
];

const PRIORITIES: { value: FeedbackPriority; label: string }[] = [
  { value: 'BASSE', label: 'Basse' },
  { value: 'MOYENNE', label: 'Moyenne' },
  { value: 'HAUTE', label: 'Haute' },
  { value: 'CRITIQUE', label: 'Critique' },
];

function detectBrowser(ua: string): string {
  if (ua.includes('Edg/')) return 'Edge';
  if (ua.includes('Firefox/')) return 'Firefox';
  if (ua.includes('Chrome/')) return 'Chrome';
  if (ua.includes('Safari/')) return 'Safari';
  if (ua.includes('OPR/')) return 'Opera';
  return 'Inconnu';
}

function detectOS(ua: string): string {
  if (/Android/.test(ua)) return 'Android';
  if (/iPhone|iPad|iPod/.test(ua)) return 'iOS';
  if (/Windows/.test(ua)) return 'Windows';
  if (/Mac OS X/.test(ua)) return 'macOS';
  if (/Linux/.test(ua)) return 'Linux';
  return '';
}

function detectDevice(ua: string): string {
  if (/Mobi|Android|iPhone|iPad/.test(ua)) return 'Mobile';
  return 'Desktop';
}

export default function FeedbackWidget() {
  const { user, isAuthenticated } = useAuth();
  const { meta } = usePlatformMeta();
  const [open, setOpen] = useState(false);
  const [category, setCategory] = useState<FeedbackCategory>('BUG');
  const [priority, setPriority] = useState<FeedbackPriority>('MOYENNE');
  const [subject, setSubject] = useState('');
  const [description, setDescription] = useState('');
  const [formError, setFormError] = useState('');

  // Fermer avec Échap
  useEffect(() => {
    if (!open) return;
    const onKey = (e: KeyboardEvent) => {
      if (e.key === 'Escape') setOpen(false);
    };
    window.addEventListener('keydown', onKey);
    return () => window.removeEventListener('keydown', onKey);
  }, [open]);

  const queryClient = useQueryClient();

  const mutation = useMutation({
    mutationFn: async () => {
      const ua = navigator.userAgent || '';
      const payload: CreateFeedbackRequest = {
        category,
        priority,
        subject: subject.trim(),
        description: description.trim() || undefined,
        pageUrl: window.location.href.slice(0, 500),
        userAgent: ua.slice(0, 500),
        browser: detectBrowser(ua),
        os: detectOS(ua),
        device: detectDevice(ua),
      };
      const res = await api.post('/feedback', payload);
      return res.data;
    },
    onSuccess: () => {
      toast.success('Merci ! Votre retour a bien été transmis à l\'équipe.');
      setOpen(false);
      setSubject('');
      setDescription('');
      setCategory('BUG');
      setPriority('MOYENNE');
      queryClient.invalidateQueries({ queryKey: ['admin-feedback'] });
    },
    onError: (err) => {
      setFormError(getErrorMessage(err));
    },
  });

  if (!isAuthenticated || !user) return null;

  const canSubmit = subject.trim().length >= 3;

  return (
    <>
      {/* Bouton flottant */}
      <button
        onClick={() => setOpen(true)}
        className="fixed bottom-5 right-5 z-40 group flex items-center gap-2 px-4 py-3 rounded-2xl
                   bg-gradient-to-r from-primary-600 to-primary-500 text-white text-sm font-medium
                   shadow-lg shadow-primary-500/30 hover:shadow-xl hover:shadow-primary-500/40
                   hover:scale-105 active:scale-95 transition-all duration-200"
        aria-label="Envoyer un retour (bug, suggestion…)"
        title="Un problème ? Un retour ?"
      >
        <MessageSquareText className="w-4.5 h-4.5 transition-transform duration-300 group-hover:rotate-12" />
        <span className="hidden sm:inline">Un retour ?</span>
      </button>

      {/* Modale */}
      {open && (
        <div className="fixed inset-0 z-50 flex items-end sm:items-center justify-center p-0 sm:p-4 animate-fade-in">
          <div className="absolute inset-0 bg-black/50 dark:bg-black/60 backdrop-blur-sm" onClick={() => setOpen(false)} />

          <div className="relative w-full sm:max-w-lg glass-strong rounded-t-3xl sm:rounded-3xl shadow-glass-lg
                          border border-white/20 dark:border-white/[0.06] animate-scale-in overflow-hidden max-h-[90vh] flex flex-col">
            {/* Header */}
            <div className="flex items-center justify-between px-5 py-4 border-b border-white/10 dark:border-white/[0.06] flex-shrink-0">
              <div>
                <h3 className="text-base font-bold text-gray-900 dark:text-white font-display flex items-center gap-2">
                  <MessageSquareText className="w-4 h-4 text-primary-500" />
                  Envoyer un retour
                </h3>
                <p className="text-xs text-gray-500 dark:text-gray-400 mt-0.5">
                  {meta.betaMode ? 'Aidez-nous à améliorer la version bêta.' : 'Aidez-nous à améliorer la plateforme.'}
                </p>
              </div>
              <button
                onClick={() => setOpen(false)}
                className="p-2 rounded-xl hover:bg-gray-100/80 dark:hover:bg-gray-800/50 transition-colors"
                aria-label="Fermer"
              >
                <X className="w-4 h-4 text-gray-400" />
              </button>
            </div>

            {/* Body */}
            <div className="px-5 py-4 space-y-4 overflow-y-auto flex-1">
              {formError && (
                <div className="p-3 rounded-xl bg-red-500/10 border border-red-500/20 text-sm text-red-600 dark:text-red-300">
                  {formError}
                </div>
              )}

              <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                <div>
                  <label className="label !mb-1.5">Catégorie</label>
                  <select
                    value={category}
                    onChange={(e) => setCategory(e.target.value as FeedbackCategory)}
                    className="input"
                  >
                    {CATEGORIES.map((c) => (
                      <option key={c.value} value={c.value}>{c.label}</option>
                    ))}
                  </select>
                </div>
                <div>
                  <label className="label !mb-1.5">Priorité</label>
                  <select
                    value={priority}
                    onChange={(e) => setPriority(e.target.value as FeedbackPriority)}
                    className="input"
                  >
                    {PRIORITIES.map((p) => (
                      <option key={p.value} value={p.value}>{p.label}</option>
                    ))}
                  </select>
                </div>
              </div>

              <div>
                <label className="label !mb-1.5">Sujet <span className="text-red-400">*</span></label>
                <input
                  className="input"
                  placeholder="Résumez le problème ou la suggestion"
                  value={subject}
                  onChange={(e) => setSubject(e.target.value)}
                  maxLength={255}
                />
              </div>

              <div>
                <label className="label !mb-1.5">Description</label>
                <textarea
                  className="input resize-none"
                  rows={4}
                  placeholder="Décrivez ce qui s'est passé, ce que vous attendiez, les étapes pour reproduire…"
                  value={description}
                  onChange={(e) => setDescription(e.target.value)}
                  maxLength={5000}
                />
              </div>

              <p className="text-[11px] text-gray-400 dark:text-gray-500">
                La page, le navigateur, le système et l'appareil sont transmis automatiquement pour le diagnostic.
              </p>
            </div>

            {/* Footer */}
            <div className="flex items-center justify-end gap-2 px-5 py-4 border-t border-white/10 dark:border-white/[0.06] flex-shrink-0">
              <button className="btn-ghost btn-sm" onClick={() => setOpen(false)}>
                Annuler
              </button>
              <button
                className="btn-primary btn-sm"
                disabled={!canSubmit || mutation.isPending}
                onClick={() => mutation.mutate()}
              >
                {mutation.isPending ? <Loader2 className="w-4 h-4 animate-spin" /> : <Send className="w-4 h-4" />}
                Envoyer le retour
              </button>
            </div>
          </div>
        </div>
      )}
    </>
  );
}
