import { useEffect, useRef, useState } from 'react';
import { X, Send, Loader2, CheckCircle2 } from 'lucide-react';
import { useI18n } from '@/i18n';
import { useSettings } from '@/contexts/SettingsContext';

interface DemoModalProps {
  open: boolean;
  onClose: () => void;
}

/**
 * Modale « Demander une démonstration » — réellement fonctionnelle.
 * Ouvre la messagerie (mailto) vers le contact configuré (branding.email)
 * avec un message pré-rempli. Aucun bouton mort.
 */
export default function DemoModal({ open, onClose }: DemoModalProps) {
  const { t } = useI18n();
  const { branding } = useSettings();
  const [form, setForm] = useState({ name: '', email: '', church: '', role: '', message: '' });
  const [error, setError] = useState(false);
  const [sending, setSending] = useState(false);
  const [done, setDone] = useState(false);
  const firstField = useRef<HTMLInputElement>(null);

  const targetEmail = branding.email || 'demo@discipolat.app';

  useEffect(() => {
    if (!open) return;
    setDone(false);
    setError(false);
    const t0 = window.setTimeout(() => firstField.current?.focus(), 60);
    const prevOverflow = document.body.style.overflow;
    document.body.style.overflow = 'hidden';
    const onKey = (e: KeyboardEvent) => { if (e.key === 'Escape') onClose(); };
    document.addEventListener('keydown', onKey);
    return () => {
      window.clearTimeout(t0);
      document.body.style.overflow = prevOverflow;
      document.removeEventListener('keydown', onKey);
    };
  }, [open, onClose]);

  if (!open) return null;

  const update = (key: keyof typeof form) => (e: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement>) =>
    setForm((f) => ({ ...f, [key]: e.target.value }));

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!form.name.trim() || !form.email.trim() || !form.church.trim()) {
      setError(true);
      return;
    }
    setError(false);
    setSending(true);

    const subject = encodeURIComponent(`[Discipolat] Demande de démonstration — ${form.church.trim()}`);
    const body =
      'Nom : ' + form.name.trim() + '\n' +
      'Email : ' + form.email.trim() + '\n' +
      'Église : ' + form.church.trim() + '\n' +
      'Rôle : ' + (form.role.trim() || '—') + '\n\n' +
      form.message.trim();
    const encodedBody = encodeURIComponent(body).replace(/%0A/g, '%0D%0A');

    // Ouvre la messagerie par défaut avec le message pré-rempli.
    window.open(`mailto:${targetEmail}?subject=${subject}&body=${encodedBody}`, '_self');

    window.setTimeout(() => { setSending(false); setDone(true); }, 700);
  };

  const inputCls =
    'w-full px-4 py-3 rounded-xl bg-gray-50/80 dark:bg-gray-800/60 border border-gray-200 dark:border-white/10 ' +
    'text-gray-900 dark:text-white text-sm placeholder-gray-400 dark:placeholder-gray-500 ' +
    'focus:outline-none focus:ring-2 focus:ring-primary-500/40 transition';

  return (
    <div
      className="fixed inset-0 z-[80] flex items-end sm:items-center justify-center p-0 sm:p-6"
      role="dialog"
      aria-modal="true"
      aria-label={t('landing.demo.title')}
    >
      <div className="absolute inset-0 bg-gray-950/60 dark:bg-black/70 backdrop-blur-sm animate-fade-in" onClick={onClose} />

      <div className="relative w-full sm:max-w-lg bg-white dark:bg-gray-900 rounded-t-3xl sm:rounded-3xl shadow-2xl shadow-gray-950/40 border border-gray-200/70 dark:border-white/10 max-h-[92vh] overflow-y-auto animate-slide-up">
        <div className="absolute -top-24 -right-16 w-56 h-56 rounded-full bg-primary-500/[0.12] blur-3xl pointer-events-none" />

        <div className="relative p-6 sm:p-8">
          <button
            onClick={onClose}
            aria-label={t('landing.demo.close')}
            className="absolute top-4 right-4 p-2 rounded-xl text-gray-400 hover:text-gray-600 dark:hover:text-gray-200 hover:bg-gray-100 dark:hover:bg-gray-800 transition-colors"
          >
            <X className="w-5 h-5" />
          </button>

          {done ? (
            <div className="text-center py-8 animate-fade-in">
              <div className="w-16 h-16 mx-auto mb-5 rounded-2xl bg-primary-500/10 flex items-center justify-center">
                <CheckCircle2 className="w-9 h-9 text-primary-600 dark:text-primary-400" />
              </div>
              <h3 className="text-xl font-bold text-gray-900 dark:text-white font-display mb-2">{t('landing.demo.successTitle')}</h3>
              <p className="text-sm text-gray-500 dark:text-gray-400">{t('landing.demo.successBody')}</p>
              <p className="mt-3 text-xs text-gray-400 dark:text-gray-500">{t('landing.demo.fallback')}</p>
              <button onClick={onClose} className="btn-primary btn-lg mt-6">{t('landing.demo.close')}</button>
            </div>
          ) : (
            <form onSubmit={handleSubmit} noValidate>
              <span className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full bg-primary-500/10 border border-primary-500/20 text-primary-600 dark:text-primary-400 text-xs font-medium mb-4">
                {t('landing.demo.title')}
              </span>
              <h3 className="text-2xl font-bold text-gray-900 dark:text-white font-display tracking-tight mb-2">
                {t('landing.demo.title')}
              </h3>
              <p className="text-sm text-gray-500 dark:text-gray-400 mb-6">{t('landing.demo.subtitle')}</p>

              {error && (
                <p className="mb-4 px-3 py-2 rounded-xl bg-red-500/10 border border-red-500/20 text-sm text-red-600 dark:text-red-400" role="alert">
                  {t('landing.demo.required')}
                </p>
              )}

              <div className="space-y-4">
                <Field label={t('landing.demo.name')}>
                  <input ref={firstField} type="text" value={form.name} onChange={update('name')} placeholder={t('landing.demo.namePh')} className={inputCls} />
                </Field>
                <Field label={t('landing.demo.email')}>
                  <input type="email" value={form.email} onChange={update('email')} placeholder={t('landing.demo.emailPh')} className={inputCls} />
                </Field>
                <div className="grid sm:grid-cols-2 gap-4">
                  <Field label={t('landing.demo.church')}>
                    <input type="text" value={form.church} onChange={update('church')} placeholder={t('landing.demo.churchPh')} className={inputCls} />
                  </Field>
                  <Field label={t('landing.demo.role')}>
                    <input type="text" value={form.role} onChange={update('role')} placeholder={t('landing.demo.rolePh')} className={inputCls} />
                  </Field>
                </div>
                <Field label={t('landing.demo.message')}>
                  <textarea value={form.message} onChange={update('message')} placeholder={t('landing.demo.messagePh')} rows={3} className={`${inputCls} resize-none`} />
                </Field>

                <button type="submit" disabled={sending} className="w-full btn-primary btn-lg flex items-center justify-center gap-2 disabled:opacity-60 disabled:cursor-not-allowed">
                  {sending ? <Loader2 className="w-4 h-4 animate-spin" /> : <Send className="w-4 h-4" />}
                  {sending ? t('landing.demo.sending') : t('landing.demo.send')}
                </button>
              </div>
            </form>
          )}
        </div>
      </div>
    </div>
  );
}

function Field({ label, children }: { label: string; children: React.ReactNode }) {
  return (
    <label className="block">
      <span className="block text-xs font-medium text-gray-600 dark:text-gray-300 mb-1.5">{label}</span>
      {children}
    </label>
  );
}