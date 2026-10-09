import { useEffect, useRef, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { Church as ChurchIcon, MapPin, ExternalLink } from 'lucide-react';
import SkeletonLoader from '@/components/shared/SkeletonLoader';
import EmptyState from '@/components/shared/EmptyState';
import BackButton from '@/components/navigation/BackButton';
import PublicBreadcrumbs from '@/components/navigation/PublicBreadcrumbs';
import { useI18n } from '@/i18n';
import { applyScopedBranding } from '@/lib/branding';
import type { PublicBranding } from '@/types';

/**
 * LOT 2 §GLISE-D'ABORD (T2.4) — landing publique d'une église, `/e/:slug`.
 *
 * <p>Page PUBLIQUE, additive : elle ne remplace aucune route existante. Elle
 * lit la projection liste blanche du serveur
 * ({@code GET /api/v1/public/churches/{slug}}) et n'affiche QUE ce que l'église
 * a doublement consenti (annuaire {@code isListed} ET {@code landing_enabled},
 * verrouillés côté serveur — R2 / RGPD art. 9).</p>
 *
 * <p>Trois verrous tenus ici, côté client :</p>
 * <ul>
 *   <li><b>R4 — pas de fuite de palette.</b> La marque est appliquée via
 *       {@code applyScopedBranding(conteneur, …)} (variables CSS posées sur le
 *       conteneur, jamais sur {@code :root}) ; le nettoyage en démontage rend
 *       la plateforme intacte. {@code applyBranding} global n'est pas appelé.</li>
 *   <li><b>noindex dès l'origine.</b> Une page cultuelle ne doit pas être
 *       indexée par surprise : {@code <meta name="robots" content="noindex,
 *       nofollow">} + entête de même nom posés au montage, retirés au démontage.</li>
 *   <li><b>R3 — 404 indistinguable.</b> Le serveur rend un même 404 pour « slug
 *       inconnu », « non listée » et « landing éteinte » ; ici on affiche un
 *       message UNIQUE, sans jamais dire lequel (aucun oracle côté client).</li>
 * </ul>
 */

interface LandingBranding {
  primaryColor?: string;
  secondaryColor?: string;
  accentColor?: string;
  surfaceColor?: string;
  backgroundColor?: string;
  textPrimaryColor?: string;
  textSecondaryColor?: string;
  primaryFont?: string;
  headingFont?: string;
}

interface ChurchLanding {
  slug: string;
  name: string;
  landingEnabled: boolean;
  slogan?: string;
  description?: string;
  logoUrl?: string;
  logoDarkUrl?: string;
  coverUrl?: string;
  faviconUrl?: string;
  website?: string;
  city?: string;
  country?: string;
  locale?: string;
  branding?: LandingBranding;
  sections?: Array<Record<string, unknown>>;
}

type State =
  | { kind: 'loading' }
  | { kind: 'absent' }            // 404 : introuvable OU non publiée OU landing éteinte (indistinguables)
  | { kind: 'error' }            // 429 / réseau : indisponible temporairement
  | { kind: 'ready'; data: ChurchLanding };

const API_BASE = (import.meta as unknown as { env?: Record<string, string> }).env?.VITE_API_URL ?? '';

export default function ChurchLandingPage() {
  const { slug = '' } = useParams<{ slug: string }>();
  const { t } = useI18n();
  const [state, setState] = useState<State>({ kind: 'loading' });
  const containerRef = useRef<HTMLDivElement>(null);

  // 1) Lecture de la projection publique. Le 404 n'est PAS une erreur réseau :
  //    c'est le « introuvable » uniforme voulu par R3.
  useEffect(() => {
    let alive = true;
    (async () => {
      if (!slug) {
        setState({ kind: 'absent' });
        return;
      }
      setState({ kind: 'loading' });
      try {
        const res = await fetch(`${API_BASE}/api/v1/public/churches/${encodeURIComponent(slug)}`, {
          headers: { Accept: 'application/json' },
        });
        if (!alive) return;
        if (res.status === 404) {
          setState({ kind: 'absent' });
          return;
        }
        if (!res.ok) {
          setState({ kind: 'error' });
          return;
        }
        const data = (await res.json()) as ChurchLanding;
        setState({ kind: 'ready', data });
      } catch {
        if (alive) setState({ kind: 'error' });
      }
    })();
    return () => {
      alive = false;
    };
  }, [slug]);

  const data = state.kind === 'ready' ? state.data : null;

  // 2) Marque SCOPÉE (R4) : variables posées sur le conteneur, retirées au démontage.
  useEffect(() => {
    if (!data) return;
    const node = containerRef.current;
    if (!node) return;
    const scoped: Partial<PublicBranding> = {};
    const br = data.branding ?? {};
    if (br.primaryColor) scoped.primaryColor = br.primaryColor;
    if (br.accentColor) scoped.accentColor = br.accentColor;
    if (br.primaryFont) scoped.fontFamily = br.primaryFont;
    // cleanup() retire exactement les propriétés posées : zéro trace sur la plateforme.
    return applyScopedBranding(node, scoped);
  }, [data]);

  // 3) Titre + robots noindex + og:* (nettoyés au démontage — rien ne déborde
  //    sur un autre écran de l'onglet).
  useEffect(() => {
    if (!data) return;
    const previousTitle = document.title;
    document.title = data.name || 'Discipolat';

    const ensureMeta = (selector: string, create: () => HTMLMetaElement): HTMLMetaElement => {
      const existing = document.head.querySelector<HTMLMetaElement>(selector);
      if (existing) return existing;
      const el = create();
      document.head.appendChild(el);
      return el;
    };

    const robotsExisting = !!document.head.querySelector<HTMLMetaElement>('meta[name="robots"]');
    const robots = ensureMeta('meta[name="robots"]', () => {
      const m = document.createElement('meta');
      m.name = 'robots';
      return m;
    });
    const previousRobots = robots.content;
    robots.content = 'noindex, nofollow';

    const ogPairs: Array<[string, string | undefined]> = [
      ['og:type', 'website'],
      ['og:title', data.name],
      ['og:description', data.slogan || data.description],
      ['og:image', data.logoUrl || data.coverUrl],
      ['og:url', window.location.href],
    ];
    const createdOg: HTMLMetaElement[] = [];
    for (const [property, content] of ogPairs) {
      if (!content) continue;
      const sel = `meta[property="${property}"]`;
      if (!document.head.querySelector(sel)) {
        const m = document.createElement('meta');
        m.setAttribute('property', property);
        document.head.appendChild(m);
        createdOg.push(m);
      }
      document.head.querySelector<HTMLMetaElement>(sel)?.setAttribute('content', content);
    }

    return () => {
      document.title = previousTitle;
      // Ce qu'on a créé, on le retire ; ce qui existait, on le laisse intact.
      if (robotsExisting) robots.content = previousRobots ?? '';
      else robots.remove();
      createdOg.forEach((m) => m.remove());
    };
  }, [data]);

  const location = data ? [data.city, data.country].filter(Boolean).join(', ') : '';

  return (
    <div
      ref={containerRef}
      className="min-h-screen bg-white dark:bg-gray-950 text-gray-900 dark:text-white"
      data-testid="church-landing"
    >
      {state.kind === 'loading' && (
        <div className="max-w-3xl mx-auto px-4 py-24">
          <SkeletonLoader lines={5} />
        </div>
      )}

      {state.kind === 'absent' && (
        <div className="max-w-xl mx-auto px-4 py-24">
          <EmptyState
            title="Page indisponible"
            message="Cette page n'existe pas, n'est pas publiée, ou a été désactivée par son église."
            action={{ label: 'Retour à l\'accueil', onClick: () => { window.location.href = '/'; } }}
          />
        </div>
      )}

      {state.kind === 'error' && (
        <div className="max-w-xl mx-auto px-4 py-24">
          <EmptyState
            title="Indisponible temporairement"
            message="Servir cette page a échoué. Réessayez dans un instant."
            action={{ label: 'Réessayer', onClick: () => setState({ kind: 'loading' }) }}
          />
        </div>
      )}

      {data && (
        <>
          {/* LOT 3 §BK (T3.2) — zone vitrine : contrôle de retour + fil d'Ariane.
              `fallbackTo` mène à l'annuaire (et non à `/`) : on revient d'où l'on
              vient, la fiche étant atteignable depuis l'annuaire ou un lien partagé.
              Le fil n'écrase PAS `currentLabel` avec `data.name` : le nom de
              l'église s'affiche déjà en `<h1>` juste en dessous ; le dupliquer
              dans le dernier maillon rendrait les sélecteurs `getByText`
              ambigus (contrat des tests T2.4 existants, A1/A6 : ne rien
              casser chez les voisins). Le prop `currentLabel` reste disponible
              pour les futures pages qui n'ont pas de titre équivalent. */}
          <div className="mx-auto max-w-3xl px-4 pt-6 flex items-center justify-between gap-3">
            <PublicBreadcrumbs />
            <BackButton detectHistory fallbackTo="/eglises" label={t('publicNav.directory')} />
          </div>
          <main className="mx-auto max-w-3xl px-4 py-8 sm:py-12">
          <header className="flex flex-col items-center text-center gap-4">
            {data.logoUrl ? (
              <img src={data.logoUrl} alt={data.name} className="h-20 w-auto object-contain" />
            ) : (
              <span className="flex h-20 w-20 items-center justify-center rounded-2xl bg-primary-500/10 text-primary-600">
                <ChurchIcon className="h-10 w-10" />
              </span>
            )}
            <h1 className="text-3xl font-bold sm:text-4xl">{data.name}</h1>
            {data.slogan && <p className="text-lg text-primary-600 dark:text-primary-400">{data.slogan}</p>}
            {location && (
              <p className="flex items-center gap-1.5 text-sm text-gray-500">
                <MapPin className="h-4 w-4" /> {location}
              </p>
            )}
          </header>

          {data.description && (
            <section className="mt-10 rounded-2xl border border-gray-200 dark:border-white/10 p-6">
              <p className="whitespace-pre-line text-sm leading-relaxed text-gray-700 dark:text-gray-200">
                {data.description}
              </p>
            </section>
          )}

          {/* Actions vers des flux QUI EXISTENT DÉJÀ (A1/A3) : rejoindre via le slug
              (convention /register?tenant= de l'annuaire) et le site officiel éventuel. */}
          <div className="mt-10 flex flex-wrap items-center justify-center gap-3">
            <Link
              to={`/register?tenant=${encodeURIComponent(data.slug)}`}
              className="rounded-xl bg-primary-600 px-5 py-2.5 text-sm font-medium text-white hover:bg-primary-700"
            >
              Rejoindre cette église
            </Link>
            {data.website && (
              <a
                href={data.website}
                target="_blank"
                rel="noreferrer nofollow"
                className="flex items-center gap-1.5 rounded-xl border px-5 py-2.5 text-sm font-medium hover:bg-gray-50 dark:hover:bg-white/5"
              >
                <ExternalLink className="h-4 w-4" /> Site officiel
              </a>
            )}
          </div>

          <p className="mt-12 text-center text-xs text-gray-400">
            Page publiée par son église · <Link to="/eglises" className="underline">Annuaire public</Link>
          </p>
        </main>
          </>
      )}
    </div>
  );
}
