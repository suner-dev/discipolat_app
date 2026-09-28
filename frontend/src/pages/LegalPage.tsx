import { useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { ArrowLeft, FileText, Loader2, ShieldCheck } from 'lucide-react';
import api, { getErrorMessage } from '@/lib/api';

interface LegalDoc {
  code: string;
  version: number;
  language: string;
  title: string;
  content: string;
  publishedAt: string;
}

/** Codes légaux mis à disposition (version FR seedée côté serveur). */
const LEGAL_TABS: Array<{ code: string; label: string }> = [
  { code: 'CGU', label: 'Conditions générales' },
  { code: 'PRIVACY', label: 'Confidentialité' },
  { code: 'DPA', label: 'DPA (RGPD art. 28)' },
  { code: 'CONSENT_ART9', label: 'Données religieuses (art. 9)' },
];

/**
 * Documents légaux versionnés et immuables, servis par
 * GET /api/v1/public/legal/{code} (dernière version publiée par défaut).
 * ?version=N permet de relire le texte exact accepté à une date donnée
 * (preuve de consentement RGPD art. 7).
 */
export default function LegalPage() {
  const { code } = useParams<{ code: string }>();
  const activeCode = (code || 'CGU').toUpperCase();
  const [doc, setDoc] = useState<LegalDoc | null>(null);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    let active = true;
    setLoading(true);
    setError('');
    api.get<LegalDoc>(`/public/legal/${activeCode}`)
      .then(({ data }) => { if (active) setDoc(data); })
      .catch((err) => { if (active) setError(getErrorMessage(err)); })
      .finally(() => { if (active) setLoading(false); });
    return () => { active = false; };
  }, [activeCode]);

  return (
    <div className="min-h-screen bg-gradient-to-b from-gray-50 to-white dark:from-gray-950 dark:to-gray-900">
      <div className="mx-auto max-w-3xl px-4 py-12">
        {/* Header */}
        <div className="mb-8 flex items-center justify-between">
          <Link
            to="/"
            className="inline-flex items-center gap-1.5 text-sm text-gray-500 dark:text-gray-400 hover:text-primary-500 transition-colors"
          >
            <ArrowLeft className="w-4 h-4" /> Retour
          </Link>
          <span className="inline-flex items-center gap-1.5 rounded-full bg-primary-500/10 border border-primary-500/20 px-3 py-1 text-xs font-medium text-primary-500">
            <ShieldCheck className="w-3 h-3" /> Discipolat
          </span>
        </div>

        {/* Tabs */}
        <nav className="mb-8 flex flex-wrap gap-2" aria-label="Documents légaux">
          {LEGAL_TABS.map((tab) => (
            <Link
              key={tab.code}
              to={`/legal/${tab.code}`}
              className={`rounded-full px-4 py-1.5 text-xs font-medium transition-colors ${
                tab.code === activeCode
                  ? 'bg-primary-600 text-white'
                  : 'bg-gray-100 dark:bg-white/5 text-gray-600 dark:text-gray-300 hover:bg-gray-200 dark:hover:bg-white/10'
              }`}
            >
              {tab.label}
            </Link>
          ))}
        </nav>

        {/* Content */}
        {loading && (
          <div className="flex items-center justify-center gap-2 py-16 text-gray-500">
            <Loader2 className="w-5 h-5 animate-spin" /> Chargement du document…
          </div>
        )}
        {!loading && error && (
          <div role="alert" className="rounded-xl border border-red-500/20 bg-red-500/10 p-4 text-sm text-red-600 dark:text-red-300">
            {error}
          </div>
        )}
        {!loading && !error && doc && (
          <article className="rounded-2xl border border-gray-200 dark:border-white/10 bg-white dark:bg-gray-900 p-8 shadow-sm">
            <div className="mb-6 flex items-start gap-3">
              <div className="inline-flex h-10 w-10 items-center justify-center rounded-xl bg-primary-500/10 border border-primary-500/20">
                <FileText className="w-5 h-5 text-primary-500" />
              </div>
              <div>
                <h1 className="text-xl font-bold text-gray-900 dark:text-white font-display">{doc.title}</h1>
                <p className="mt-0.5 text-xs text-gray-500 dark:text-gray-400">
                  Version {doc.version} · publiée le{' '}
                  {new Date(doc.publishedAt).toLocaleDateString('fr-FR', { day: 'numeric', month: 'long', year: 'numeric' })}
                  {' '}· document {doc.code} (version {doc.version} conservée sans modification)
                </p>
              </div>
            </div>
            <div className="prose max-w-none whitespace-pre-line text-sm leading-relaxed text-gray-700 dark:text-gray-300">
              {doc.content}
            </div>
          </article>
        )}
      </div>
    </div>
  );
}
