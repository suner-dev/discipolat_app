import { useEffect, useState } from 'react';
import { Link, useNavigate, useParams, useSearchParams } from 'react-router-dom';
import { ArrowRight, Church, KeyRound, Loader2, Search, ShieldQuestion } from 'lucide-react';
import api, { getErrorMessage } from '@/lib/api';
import { useAuth } from '@/contexts/AuthContext';
import { tText } from '@/i18n';

/**
 * SPEC_ONBOARDING_FLOWS (FE-2) — page publique de rejointure :
 * `/join` (saisie d'un code) et `/j/:slug` (lien vanity).
 * Résolution via /public/join/lookup (vitrine sans PII), puis :
 *  • connecté → POST /tenant/join (OPEN = entrée directe, sans resaisie
 *    du code aux connexions suivantes — D7) ;
 *  • non connecté → inscription/connexion avec le code pré-transmis.
 */
type LookupResult = {
  found: boolean;
  churchName?: string;
  orgNodeLabel?: string;
  slug?: string;
  joinMode?: string;
  requiresApproval?: boolean;
  reason?: string;
};

export default function JoinChurchPage() {
  const { slug } = useParams<{ slug?: string }>();
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const { isAuthenticated } = useAuth();

  const [code, setCode] = useState(searchParams.get('code')?.trim() ?? '');
  const [lookup, setLookup] = useState<LookupResult | null>(null);
  const [looking, setLooking] = useState(false);
  const [joining, setJoining] = useState(false);
  const [error, setError] = useState('');
  const [done, setDone] = useState<'JOINED' | 'PENDING_APPROVAL' | null>(null);

  const resolveSlug = async () => {
    if (!slug) return;
    setLooking(true);
    setError('');
    try {
      const { data } = await api.get<LookupResult>('/public/join/resolve', { params: { slug } });
      setLookup(data);
      if (data.found && !data.joinMode) {
        // lien parent → église principale : le code n'est jamais exposé ici.
      }
    } catch (err) {
      setError(getErrorMessage(err));
    } finally {
      setLooking(false);
    }
  };

  useEffect(() => {
    if (slug) void resolveSlug();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [slug]);

  const lookupCode = async () => {
    const value = code.trim();
    if (!value) return;
    setLooking(true);
    setError('');
    setLookup(null);
    try {
      const { data } = await api.post<LookupResult>('/public/join/lookup', { code: value });
      setLookup(data);
      if (!data.found && data.reason === 'RATE_LIMITED') {
        setError(tText('Trop de tentatives. Réessayez dans une minute.'));
      } else if (!data.found) {
        setError(tText('Aucune église ne correspond à ce code.'));
      }
    } catch (err) {
      setError(getErrorMessage(err));
    } finally {
      setLooking(false);
    }
  };

  const joinAuthenticated = async () => {
    setJoining(true);
    setError('');
    try {
      // Code saisi prioritaire ; sinon, lien vanity → le backend résout par slug.
      const body = code.trim() ? { code: code.trim() } : { slug: lookup?.slug ?? slug };
      const { data } = await api.post<{ status: string }>('/tenant/join', body);
      if (data.status === 'PENDING_APPROVAL') {
        setDone('PENDING_APPROVAL');
      } else {
        setDone('JOINED');
        // Session réémise sur l'église rejointe — plus jamais de resaisie (D7).
        window.setTimeout(() => { window.location.href = '/dashboard'; }, 900);
      }
    } catch (err) {
      setError(getErrorMessage(err));
    } finally {
      setJoining(false);
    }
  };

  const registerWithCode = () => {
    // Lien vanity sans code saisi → flux d'inscription par slug (§G3.1).
    if (!code.trim() && lookup?.slug) {
      navigate(`/register?tenant=${encodeURIComponent(lookup.slug)}`);
      return;
    }
    const church = encodeURIComponent(lookup?.churchName ?? '');
    navigate(`/register?joinCode=${encodeURIComponent(code.trim())}&church=${church}`);
  };

  if (done) {
    return (
      <div className="min-h-[70vh] flex items-center justify-center px-4">
        <div className="max-w-md w-full text-center space-y-5 animate-slide-up">
          <div className="mx-auto inline-flex items-center justify-center w-16 h-16 rounded-2xl bg-green-500/10 border border-green-500/20">
            <Church className="w-8 h-8 text-green-500" />
          </div>
          <h1 className="text-2xl font-bold text-gray-900 dark:text-white font-display">
            {done === 'JOINED' ? tText('Bienvenue dans votre église !') : tText('Demande transmise')}
          </h1>
          <p className="text-sm text-gray-500 dark:text-gray-400">
            {done === 'JOINED'
              ? tText('Vous y êtes rattaché durablement : plus besoin de saisir le code aux connexions suivantes.')
              : tText('Un responsable de l’église examinera votre demande et vous contactera.')}
          </p>
          {done === 'JOINED' && (
            <Link to="/dashboard" className="inline-flex items-center gap-2 rounded-xl bg-gradient-to-r from-primary-600 to-primary-500 px-5 py-3 text-sm font-medium text-white">
              {tText('Entrer maintenant')} <ArrowRight className="w-4 h-4" />
            </Link>
          )}
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-[70vh] flex items-center justify-center px-4 py-12">
      <div className="max-w-md w-full">
        <div className="text-center mb-8">
          <div className="inline-flex items-center justify-center w-14 h-14 rounded-2xl bg-primary-500/10 border border-primary-500/20 mb-4">
            <KeyRound className="w-7 h-7 text-primary-500" />
          </div>
          <h1 className="text-2xl font-bold text-gray-900 dark:text-white font-display">
            {slug ? tText('Rejoindre cette église') : tText('Rejoindre une église')}
          </h1>
          <p className="mt-2 text-sm text-gray-500 dark:text-gray-400">
            {slug
              ? tText('Vous avez été invité via un lien. Confirmez pour entrer.')
              : tText("Saisissez le code d'entrée fourni par votre église (ex : BETHEL-7K2M).")}
          </p>
        </div>

        {error && (
          <div className="mb-4 p-3.5 rounded-xl bg-red-500/10 border border-red-500/20">
            <p className="text-sm text-red-600 dark:text-red-300">{error}</p>
          </div>
        )}

        {looking && !lookup && (
          <div className="flex justify-center py-6"><Loader2 className="h-6 w-6 animate-spin text-primary-500" /></div>
        )}

        {lookup?.found ? (
          <div className="rounded-2xl border border-gray-200 dark:border-white/10 bg-white/70 dark:bg-white/5 p-6 space-y-4 animate-slide-up backdrop-blur">
            <div className="flex items-center gap-3">
              <div className="w-11 h-11 rounded-xl bg-gradient-to-br from-primary-500 to-emerald-600 flex items-center justify-center text-white">
                <Church className="w-5 h-5" />
              </div>
              <div>
                <p className="font-semibold text-gray-900 dark:text-white">{lookup.churchName}</p>
                {lookup.orgNodeLabel && (
                  <p className="text-xs text-gray-500 dark:text-gray-400">{lookup.orgNodeLabel}</p>
                )}
              </div>
            </div>
            {lookup.requiresApproval && (
              <p className="text-xs rounded-lg bg-amber-500/10 border border-amber-500/20 text-amber-700 dark:text-amber-300 px-3 py-2">
                {tText('Cette église valide chaque demande d’adhésion avant l’entrée.')}
              </p>
            )}
            {isAuthenticated ? (
              <button
                type="button"
                onClick={() => void joinAuthenticated()}
                disabled={joining || (!code.trim() && !lookup?.slug)}
                className="w-full py-3 px-4 rounded-xl bg-gradient-to-r from-primary-600 to-primary-500 text-white font-medium text-sm
                           disabled:opacity-50 flex items-center justify-center gap-2"
              >
                {joining ? <Loader2 className="w-4 h-4 animate-spin" /> : <ArrowRight className="w-4 h-4" />}
                {tText('Rejoindre maintenant')}
              </button>
            ) : (
              <div className="space-y-2">
                <button
                  type="button"
                  onClick={registerWithCode}
                  className="w-full py-3 px-4 rounded-xl bg-gradient-to-r from-primary-600 to-primary-500 text-white font-medium text-sm
                             flex items-center justify-center gap-2"
                >
                  <ArrowRight className="w-4 h-4" />
                  {tText("Créer un compte et rejoindre")}
                </button>
                <Link
                  to={`/login?next=${encodeURIComponent('/join' + (slug ? `/${slug}` : `?code=${encodeURIComponent(code.trim())}`))}`}
                  className="block w-full py-3 px-4 rounded-xl border border-gray-200 dark:border-white/10 text-sm font-medium text-gray-700 dark:text-gray-200 text-center hover:bg-gray-50 dark:hover:bg-white/5"
                >
                  {tText('J\u2019ai déjà un compte — me connecter')}
                </Link>
              </div>
            )}
          </div>
        ) : !slug ? (
          <div className="space-y-3">
            <div className="flex gap-2">
              <div className="relative flex-1">
                <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400" />
                <input
                  value={code}
                  onChange={(e) => setCode(e.target.value.toUpperCase())}
                  onKeyDown={(e) => { if (e.key === 'Enter') void lookupCode(); }}
                  placeholder={tText("Code de l'église")}
                  className="w-full rounded-xl bg-gray-100/80 dark:bg-white/5 border border-gray-200 dark:border-white/10 text-gray-900 dark:text-white placeholder-gray-400 pl-9 pr-4 py-3 text-sm font-mono uppercase focus:outline-none focus:border-primary-500/50 focus:ring-2 focus:ring-primary-500/20"
                />
              </div>
              <button
                type="button"
                onClick={() => void lookupCode()}
                disabled={looking || !code.trim()}
                className="rounded-xl bg-primary-600 px-4 py-3 text-sm font-medium text-white disabled:opacity-50 hover:bg-primary-500"
              >
                {looking ? <Loader2 className="w-4 h-4 animate-spin" /> : tText('Vérifier')}
              </button>
            </div>
            <p className="flex items-center gap-2 text-xs text-gray-400 dark:text-gray-500">
              <ShieldQuestion className="w-4 h-4" />
              {tText('Le code sert une seule fois : après la rejointure, vous entrez directement à chaque connexion.')}
            </p>
          </div>
        ) : null}

        <div className="mt-8 text-center text-sm text-gray-500 dark:text-gray-400">
          {tText('Pas encore de code ?')}{' '}
          <Link to="/register?mode=church" className="font-medium text-primary-600 dark:text-primary-400 hover:underline">
            {tText('Créez votre église')}
          </Link>
        </div>
      </div>
    </div>
  );
}
