import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  Search, ArrowRight, Image as ImageIcon, List, FileText, Link2,
} from 'lucide-react';
import { resolveIcon } from '@/lib/menuIcons';
import type { ResolvedBlock } from '@/types';

/** Couleurs disponibles pour les cartes KPI. */
const KPI_COLORS: Record<string, { bg: string; text: string; ring: string }> = {
  primary: { bg: 'bg-primary-50 dark:bg-primary-950/40', text: 'text-primary-600 dark:text-primary-400', ring: 'ring-primary-100 dark:ring-primary-900/40' },
  emerald: { bg: 'bg-emerald-50 dark:bg-emerald-950/40', text: 'text-emerald-600 dark:text-emerald-400', ring: 'ring-emerald-100 dark:ring-emerald-900/40' },
  amber: { bg: 'bg-amber-50 dark:bg-amber-950/40', text: 'text-amber-600 dark:text-amber-400', ring: 'ring-amber-100 dark:ring-amber-900/40' },
  violet: { bg: 'bg-violet-50 dark:bg-violet-950/40', text: 'text-violet-600 dark:text-violet-400', ring: 'ring-violet-100 dark:ring-violet-900/40' },
  rose: { bg: 'bg-rose-50 dark:bg-rose-950/40', text: 'text-rose-600 dark:text-rose-400', ring: 'ring-rose-100 dark:ring-rose-900/40' },
  sky: { bg: 'bg-sky-50 dark:bg-sky-950/40', text: 'text-sky-600 dark:text-sky-400', ring: 'ring-sky-100 dark:ring-sky-900/40' },
};

function KpiBlock({ block }: { block: ResolvedBlock }) {
  const { config, data } = block;
  const label = (config.label as string) || 'Indicateur';
  const color = KPI_COLORS[(config.color as string) || 'primary'] || KPI_COLORS.primary;
  const Icon = resolveIcon((config.icon as string) || 'CircleDot');
  const value = data && typeof data.value === 'number' ? data.value : null;

  return (
    <div className={`glass-card p-5 flex items-center gap-4 ring-1 ${color.ring}`}>
      <div className={`w-12 h-12 rounded-xl flex items-center justify-center flex-shrink-0 ${color.bg}`}>
        <Icon className={`w-6 h-6 ${color.text}`} />
      </div>
      <div className="min-w-0">
        <p className="text-2xl font-bold text-gray-900 dark:text-gray-50 leading-none">
          {value === null ? '—' : new Intl.NumberFormat('fr-FR').format(value)}
        </p>
        <p className="text-xs font-medium text-gray-500 dark:text-gray-400 mt-1 truncate">{label}</p>
      </div>
    </div>
  );
}

function TableBlock({ block }: { block: ResolvedBlock }) {
  const { config, data } = block;
  const title = (config.title as string) || 'Tableau';
  const headers = (data?.headers as string[]) || [];
  const rows = (data?.rows as unknown[][]) || [];

  return (
    <div className="glass-card overflow-hidden">
      <div className="card-header">
        <h3 className="text-sm font-bold text-gray-900 dark:text-gray-100">{title}</h3>
        <span className="text-xs text-gray-400">{rows.length} ligne(s)</span>
      </div>
      {rows.length === 0 ? (
        <p className="px-5 py-6 text-sm text-gray-400 text-center">Aucune donnée dans votre périmètre.</p>
      ) : (
        <div className="overflow-x-auto">
          <table className="w-full text-sm">
            <thead>
              <tr className="text-left text-[11px] uppercase tracking-wider text-gray-400 border-b border-gray-100/60 dark:border-gray-800/40">
                {headers.map((h) => <th key={h} className="px-5 py-2.5 font-semibold">{h}</th>)}
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-100/50 dark:divide-gray-800/30">
              {rows.map((row, i) => (
                <tr key={i} className="text-gray-700 dark:text-gray-300">
                  {row.map((cell, j) => (
                    <td key={j} className="px-5 py-2.5">{String(cell ?? '—')}</td>
                  ))}
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}

function ListBlock({ block }: { block: ResolvedBlock }) {
  const { config, data } = block;
  const title = (config.title as string) || 'Liste';
  const items = (data?.items as { label?: string; value?: string }[]) || [];

  return (
    <div className="glass-card overflow-hidden">
      <div className="card-header">
        <h3 className="text-sm font-bold text-gray-900 dark:text-gray-100">{title}</h3>
      </div>
      {items.length === 0 ? (
        <p className="px-5 py-6 text-sm text-gray-400 text-center">Aucune donnée dans votre périmètre.</p>
      ) : (
        <ul className="divide-y divide-gray-100/50 dark:divide-gray-800/30">
          {items.map((item, i) => (
            <li key={i} className="px-5 py-3 flex items-start gap-3">
              <span className="mt-1.5 w-1.5 h-1.5 rounded-full bg-primary-400 flex-shrink-0" />
              <div className="min-w-0">
                <p className="text-sm font-medium text-gray-800 dark:text-gray-100 truncate">{item.label || '—'}</p>
                {item.value && <p className="text-xs text-gray-400 truncate">{item.value}</p>}
              </div>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}

function TextBlock({ block }: { block: ResolvedBlock }) {
  const content = (block.config.content as string) || '';
  return (
    <div className="glass-card p-5">
      <div className="flex items-center gap-2 mb-2">
        <FileText className="w-4 h-4 text-primary-500" />
        <h3 className="text-sm font-bold text-gray-900 dark:text-gray-100">Texte</h3>
      </div>
      <div className="text-sm text-gray-600 dark:text-gray-300 whitespace-pre-wrap leading-relaxed">
        {content || <span className="text-gray-400">(contenu vide)</span>}
      </div>
    </div>
  );
}

function LinksBlock({ block }: { block: ResolvedBlock }) {
  const { config } = block;
  const title = (config.title as string) || 'Accès rapides';
  const items = (config.items as { label?: string; href?: string; icon?: string }[]) || [];

  return (
    <div className="glass-card p-5">
      <div className="flex items-center gap-2 mb-3">
        <Link2 className="w-4 h-4 text-primary-500" />
        <h3 className="text-sm font-bold text-gray-900 dark:text-gray-100">{title}</h3>
      </div>
      <div className="grid gap-2">
        {items.map((item, i) => {
          const Icon = resolveIcon(item.icon || 'CircleDot');
          return (
            <a
              key={i}
              href={item.href || '#'}
              className="flex items-center gap-3 px-3 py-2.5 rounded-xl bg-gray-50/80 dark:bg-gray-800/40 hover:bg-primary-50 dark:hover:bg-primary-950/30 transition-colors group"
            >
              <Icon className="w-4 h-4 text-primary-500 flex-shrink-0" />
              <span className="text-sm font-medium text-gray-700 dark:text-gray-200 flex-1 truncate">{item.label || item.href}</span>
              <ArrowRight className="w-4 h-4 text-gray-300 group-hover:text-primary-500 transition-colors flex-shrink-0" />
            </a>
          );
        })}
      </div>
    </div>
  );
}

function SearchBlock({ block }: { block: ResolvedBlock }) {
  const navigate = useNavigate();
  const [q, setQ] = useState('');
  const placeholder = (block.config.placeholder as string) || 'Rechercher…';

  const submit = (e: React.FormEvent) => {
    e.preventDefault();
    const query = q.trim();
    if (query.length >= 2) {
      navigate(`/search?q=${encodeURIComponent(query)}`);
    }
  };

  return (
    <form onSubmit={submit} className="glass-card p-5">
      <div className="flex items-center gap-2 mb-2">
        <Search className="w-4 h-4 text-primary-500" />
        <h3 className="text-sm font-bold text-gray-900 dark:text-gray-100">Recherche</h3>
      </div>
      <div className="flex gap-2">
        <input
          className="input flex-1"
          value={q}
          onChange={(e) => setQ(e.target.value)}
          placeholder={placeholder}
          aria-label={placeholder}
        />
        <button className="btn-primary btn-sm" type="submit" disabled={q.trim().length < 2}>
          <Search className="w-4 h-4" /> Chercher
        </button>
      </div>
    </form>
  );
}

function ImagesBlock({ block }: { block: ResolvedBlock }) {
  const { config } = block;
  const url = (config.url as string) || '';
  const caption = (config.caption as string) || '';
  return (
    <figure className="glass-card overflow-hidden">
      {url ? (
        <img src={url} alt={caption || 'Image'} className="w-full h-48 object-cover" loading="lazy" />
      ) : (
        <div className="h-48 flex items-center justify-center bg-gray-50 dark:bg-gray-800/40">
          <ImageIcon className="w-8 h-8 text-gray-300" />
        </div>
      )}
      {caption && (
        <figcaption className="px-5 py-3 text-sm text-gray-500 dark:text-gray-400">{caption}</figcaption>
      )}
    </figure>
  );
}

/**
 * Rendu d'un bloc de page personnalisée. Toutes les données affichées
 * proviennent du serveur (résolution réelle, scopée par espace métier) —
 * aucune statistique fictive.
 */
export default function PageBlockRenderer({ block }: { block: ResolvedBlock }) {
  switch (block.type) {
    case 'KPI':
      return <KpiBlock block={block} />;
    case 'TABLEAU':
      return <TableBlock block={block} />;
    case 'LISTE':
      return <ListBlock block={block} />;
    case 'TEXTE':
      return <TextBlock block={block} />;
    case 'LIENS':
      return <LinksBlock block={block} />;
    case 'RECHERCHE':
      return <SearchBlock block={block} />;
    case 'IMAGES':
      return <ImagesBlock block={block} />;
    default:
      return null;
  }
}

/** Icône indicative d'un type de bloc (palette de l'éditeur). */
export function blockTypeIcon(type: string) {
  switch (type) {
    case 'KPI': return resolveIcon('BarChart3');
    case 'TABLEAU': return resolveIcon('FileText');
    case 'LISTE': return List;
    case 'TEXTE': return FileText;
    case 'LIENS': return Link2;
    case 'RECHERCHE': return Search;
    case 'IMAGES': return ImageIcon;
    default: return List;
  }
}

export const BLOCK_TYPE_LABELS: Record<string, string> = {
  KPI: 'Indicateur (KPI)',
  TABLEAU: 'Tableau de données',
  LISTE: 'Liste',
  TEXTE: 'Texte',
  LIENS: 'Liens rapides',
  RECHERCHE: 'Recherche',
  IMAGES: 'Image',
};
