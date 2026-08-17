import { useParams, Link } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import api from '@/lib/api';
import { LayoutTemplate, ArrowLeft, Loader2 } from 'lucide-react';
import PageBlockRenderer from '@/components/pages/PageBlockRenderer';
import type { ResolvedPage } from '@/types';

const LAYOUT_CLASSES: Record<string, string> = {
  GRID_2: 'grid grid-cols-1 md:grid-cols-2 gap-4',
  GRID_3: 'grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4',
  STACK: 'flex flex-col gap-4',
};

/**
 * Rendu public d'une page personnalisée du Page Builder.
 * La page et ses blocs sont résolus côté serveur sur des données réelles,
 * scopées selon l'espace métier de l'utilisateur connecté.
 */
export default function CustomPageView() {
  const { slug = '' } = useParams<{ slug: string }>();

  const { data, isLoading, isError, error } = useQuery({
    queryKey: ['pages', slug],
    queryFn: async () => {
      const res = await api.get(`/pages/${encodeURIComponent(slug)}`);
      return res.data as ResolvedPage;
    },
    retry: false,
  });

  if (isLoading) {
    return (
      <div className="min-h-[50vh] flex items-center justify-center">
        <div className="flex items-center gap-3 text-sm text-gray-400">
          <Loader2 className="w-5 h-5 animate-spin" /> Chargement de la page…
        </div>
      </div>
    );
  }

  if (isError || !data) {
    const status = (error as { response?: { status?: number } })?.response?.status;
    const message = status === 403
      ? 'Cette page n’est pas accessible avec votre rôle actuel.'
      : 'Cette page n’existe pas ou n’est pas publiée.';
    return (
      <div className="page-container max-w-xl">
        <div className="empty-state glass-card mt-8">
          <LayoutTemplate className="empty-state-icon" />
          <p className="text-gray-500 dark:text-gray-400">{message}</p>
          <Link to="/dashboard" className="btn-primary btn-sm mt-4">
            <ArrowLeft className="w-4 h-4" /> Retour au tableau de bord
          </Link>
        </div>
      </div>
    );
  }

  const { page, blocks } = data;
  const layoutClass = LAYOUT_CLASSES[page.layout] || LAYOUT_CLASSES.STACK;

  return (
    <div className="page-container max-w-6xl">
      <div className="page-header">
        <div>
          <h1 className="page-title flex items-center gap-2">
            <LayoutTemplate className="w-5 h-5 text-primary-500" />
            {page.title}
          </h1>
          {page.description && <p className="page-subtitle">{page.description}</p>}
        </div>
        {page.version > 1 && (
          <span className="badge badge-gray">v{page.version}</span>
        )}
      </div>

      {blocks.length === 0 ? (
        <div className="empty-state glass-card">
          <LayoutTemplate className="empty-state-icon" />
          <p className="text-gray-500 dark:text-gray-400">Cette page ne contient aucun bloc.</p>
        </div>
      ) : (
        <div className={layoutClass}>
          {blocks.map((block, i) => (
            <PageBlockRenderer key={i} block={block} pageId={page.id} index={i} />
          ))}
        </div>
      )}
    </div>
  );
}
