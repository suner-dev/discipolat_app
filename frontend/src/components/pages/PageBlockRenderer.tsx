import { useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  Search, ArrowRight, Image as ImageIcon, List, FileText, Link2,
  CalendarDays, CheckSquare, Clock, ChevronLeft, ChevronRight, MapPin,
} from 'lucide-react';
import {
  PieChart, Pie, Cell, BarChart, Bar, LineChart, Line,
  XAxis, YAxis, CartesianGrid, Tooltip, Legend, ResponsiveContainer,
} from 'recharts';
import { resolveIcon } from '@/lib/menuIcons';
import type { ResolvedBlock } from '@/types';

/** Couleurs disponibles pour les cartes KPI. */
const CHART_COLORS = ['#22c55e', '#f59e0b', '#ef4444', '#3b82f6', '#8b5cf6', '#ec4899', '#14b8a6'];
const FR_MONTHS = ['janvier', 'février', 'mars', 'avril', 'mai', 'juin', 'juillet', 'août', 'septembre', 'octobre', 'novembre', 'décembre'];
const FR_DAYS = ['L', 'M', 'M', 'J', 'V', 'S', 'D'];
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

interface ChartDatum { name: string; value: number }

function ChartBlock({ block }: { block: ResolvedBlock }) {
  const { config, data } = block;
  const title = (config.title as string) || 'Graphique';
  const chartType = (config.chartType as string) || 'PIE';
  const chartData = (data?.data as ChartDatum[]) || [];

  return (
    <div className="glass-card p-5">
      <div className="card-header">
        <h3 className="text-sm font-bold text-gray-900 dark:text-gray-100">{title}</h3>
        {chartData.length === 0 && <span className="text-xs text-gray-400">Aucune donnée</span>}
      </div>
      {chartData.length === 0 ? (
        <p className="py-6 text-sm text-gray-400 text-center">Aucune donnée dans votre périmètre.</p>
      ) : (
        <div className="h-56">
          <ResponsiveContainer width="100%" height="100%">
            {chartType === 'PIE' ? (
              <PieChart>
                <Pie
                  data={chartData}
                  dataKey="value"
                  nameKey="name"
                  innerRadius="45%"
                  outerRadius="75%"
                  paddingAngle={2}
                >
                  {chartData.map((_, i) => (
                    <Cell key={i} fill={CHART_COLORS[i % CHART_COLORS.length]} />
                  ))}
                </Pie>
                <Tooltip />
                <Legend />
              </PieChart>
            ) : chartType === 'BAR' ? (
              <BarChart data={chartData}>
                <CartesianGrid strokeDasharray="3 3" stroke="#333" opacity={0.2} />
                <XAxis dataKey="name" tick={{ fontSize: 11 }} />
                <YAxis allowDecimals={false} tick={{ fontSize: 11 }} />
                <Tooltip />
                <Bar dataKey="value" radius={[4, 4, 0, 0]}>
                  {chartData.map((_, i) => (
                    <Cell key={i} fill={CHART_COLORS[i % CHART_COLORS.length]} />
                  ))}
                </Bar>
              </BarChart>
            ) : (
              <LineChart data={chartData}>
                <CartesianGrid strokeDasharray="3 3" stroke="#333" opacity={0.2} />
                <XAxis dataKey="name" tick={{ fontSize: 11 }} />
                <YAxis allowDecimals={false} tick={{ fontSize: 11 }} />
                <Tooltip />
                <Line type="monotone" dataKey="value" stroke="#22c55e" strokeWidth={2} dot={{ r: 3 }} />
              </LineChart>
            )}
          </ResponsiveContainer>
        </div>
      )}
    </div>
  );
}

interface CalendarEvent { date: string; title: string; lieu?: string; type?: string }

function CalendarBlock({ block }: { block: ResolvedBlock }) {
  const { config, data } = block;
  const title = (config.title as string) || 'Calendrier';
  const events = (data?.events as CalendarEvent[]) || [];
  const [monthOffset, setMonthOffset] = useState(0);

  const now = new Date();
  const viewYear = now.getFullYear();
  const viewMonth = now.getMonth() + monthOffset;
  const displayDate = new Date(viewYear, viewMonth, 1);

  const eventsByDay = useMemo(() => {
    const map = new Map<number, CalendarEvent[]>();
    for (const e of events) {
      const d = new Date(`${e.date}T00:00:00`);
      if (d.getFullYear() === displayDate.getFullYear() && d.getMonth() === displayDate.getMonth()) {
        const day = d.getDate();
        map.set(day, [...(map.get(day) || []), e]);
      }
    }
    return map;
  }, [events, displayDate]);

  const firstWeekday = (displayDate.getDay() + 6) % 7; // lundi = 0
  const daysInMonth = new Date(displayDate.getFullYear(), displayDate.getMonth() + 1, 0).getDate();
  const today = now.getDate();
  const isCurrentMonth = monthOffset === 0;

  const monthEvents = events
    .filter((e) => {
      const d = new Date(`${e.date}T00:00:00`);
      return d.getFullYear() === displayDate.getFullYear() && d.getMonth() === displayDate.getMonth();
    })
    .sort((a, b) => a.date.localeCompare(b.date));

  return (
    <div className="glass-card p-5">
      <div className="flex items-center justify-between mb-3">
        <div className="flex items-center gap-2">
          <CalendarDays className="w-4 h-4 text-primary-500" />
          <h3 className="text-sm font-bold text-gray-900 dark:text-gray-100">{title}</h3>
        </div>
        <div className="flex items-center gap-1">
          <button type="button" className="btn-icon btn-icon-sm text-gray-400" onClick={() => setMonthOffset((m) => m - 1)} aria-label="Mois précédent">
            <ChevronLeft className="w-4 h-4" />
          </button>
          <span className="text-xs font-semibold text-gray-600 dark:text-gray-300 min-w-[110px] text-center capitalize">
            {FR_MONTHS[displayDate.getMonth()]} {displayDate.getFullYear()}
          </span>
          <button type="button" className="btn-icon btn-icon-sm text-gray-400" onClick={() => setMonthOffset((m) => m + 1)} aria-label="Mois suivant">
            <ChevronRight className="w-4 h-4" />
          </button>
        </div>
      </div>

      <div className="grid grid-cols-7 gap-1 mb-1">
        {FR_DAYS.map((d, i) => (
          <div key={i} className="text-center text-[10px] font-semibold text-gray-400 py-1">{d}</div>
        ))}
      </div>
      <div className="grid grid-cols-7 gap-1">
        {Array.from({ length: firstWeekday }).map((_, i) => (
          <div key={`empty-${i}`} />
        ))}
        {Array.from({ length: daysInMonth }).map((_, i) => {
          const day = i + 1;
          const dayEvents = eventsByDay.get(day) || [];
          const isToday = isCurrentMonth && day === today;
          return (
            <div
              key={day}
              className={`relative h-8 rounded-lg flex items-center justify-center text-xs ${
                isToday
                  ? 'bg-primary-600 text-white font-bold'
                  : dayEvents.length > 0
                    ? 'bg-primary-50 dark:bg-primary-950/40 text-primary-700 dark:text-primary-300 font-semibold'
                    : 'text-gray-600 dark:text-gray-400'
              }`}
            >
              {day}
              {dayEvents.length > 0 && !isToday && (
                <span className="absolute bottom-0.5 w-1 h-1 rounded-full bg-primary-500" />
              )}
            </div>
          );
        })}
      </div>

      {monthEvents.length > 0 && (
        <ul className="mt-3 space-y-1.5 border-t border-gray-100/60 dark:border-gray-800/40 pt-3">
          {monthEvents.map((e, i) => (
            <li key={i} className="flex items-start gap-2 text-xs">
              <span className="w-16 flex-shrink-0 font-mono text-gray-400">{e.date.slice(8, 10)}/{e.date.slice(5, 7)}</span>
              <span className="text-gray-700 dark:text-gray-300 font-medium">{e.title}</span>
              {e.lieu && (
                <span className="text-gray-400 flex items-center gap-1 ml-auto truncate max-w-[180px]">
                  <MapPin className="w-3 h-3 flex-shrink-0" /> {e.lieu}
                </span>
              )}
            </li>
          ))}
        </ul>
      )}
      {monthEvents.length === 0 && events.length > 0 && (
        <p className="mt-3 pt-3 text-xs text-gray-400 border-t border-gray-100/60 dark:border-gray-800/40">
          Aucun événement ce mois-ci.
        </p>
      )}
    </div>
  );
}

interface TimelineItem { date: string; label: string; value?: string }

function TimelineBlock({ block }: { block: ResolvedBlock }) {
  const { config, data } = block;
  const title = (config.title as string) || 'Timeline';
  const items = (data?.items as TimelineItem[]) || [];

  return (
    <div className="glass-card p-5">
      <div className="flex items-center gap-2 mb-4">
        <Clock className="w-4 h-4 text-primary-500" />
        <h3 className="text-sm font-bold text-gray-900 dark:text-gray-100">{title}</h3>
      </div>
      {items.length === 0 ? (
        <p className="py-6 text-sm text-gray-400 text-center">Aucune donnée dans votre périmètre.</p>
      ) : (
        <ol className="relative border-l border-gray-200 dark:border-gray-700 ml-2 space-y-4">
          {items.map((item, i) => (
            <li key={i} className="ml-4">
              <span className="absolute -left-[5px] mt-1.5 w-2.5 h-2.5 rounded-full bg-primary-500 ring-4 ring-primary-100 dark:ring-primary-950/40" />
              <p className="text-[11px] font-mono text-gray-400">{item.date}</p>
              <p className="text-sm font-medium text-gray-800 dark:text-gray-100">{item.label}</p>
              {item.value && <p className="text-xs text-gray-400">{item.value}</p>}
            </li>
          ))}
        </ol>
      )}
    </div>
  );
}

function ChecklistBlock({ block, pageId, index }: { block: ResolvedBlock; pageId?: string; index?: number }) {
  const { config } = block;
  const title = (config.title as string) || 'Checklist';
  const items = (config.items as string[]) || [];
  const storageKey = pageId && index !== undefined ? `pb-checklist-${pageId}-${index}` : null;

  const [checked, setChecked] = useState<Set<number>>(() => {
    if (!storageKey) return new Set();
    try {
      const raw = localStorage.getItem(storageKey);
      return raw ? new Set(JSON.parse(raw) as number[]) : new Set();
    } catch {
      return new Set();
    }
  });

  const toggle = (i: number) => {
    setChecked((prev) => {
      const next = new Set(prev);
      if (next.has(i)) next.delete(i); else next.add(i);
      if (storageKey) localStorage.setItem(storageKey, JSON.stringify([...next]));
      return next;
    });
  };

  const progress = items.length === 0 ? 0 : Math.round((checked.size / items.length) * 100);

  return (
    <div className="glass-card p-5">
      <div className="flex items-center gap-2 mb-3">
        <CheckSquare className="w-4 h-4 text-primary-500" />
        <h3 className="text-sm font-bold text-gray-900 dark:text-gray-100">{title}</h3>
        <span className="ml-auto text-xs font-semibold text-gray-400">{checked.size}/{items.length} · {progress} %</span>
      </div>
      <div className="h-1.5 rounded-full bg-gray-100 dark:bg-gray-800 mb-3 overflow-hidden">
        <div className="h-full bg-gradient-to-r from-primary-500 to-emerald-500 rounded-full transition-all duration-300" style={{ width: `${progress}%` }} />
      </div>
      {items.length === 0 ? (
        <p className="py-4 text-sm text-gray-400 text-center">Aucun élément.</p>
      ) : (
        <ul className="space-y-1.5">
          {items.map((item, i) => (
            <li key={i}>
              <label className="flex items-center gap-3 cursor-pointer rounded-lg px-2 py-1.5 hover:bg-gray-50 dark:hover:bg-gray-800/40 transition-colors">
                <input
                  type="checkbox"
                  checked={checked.has(i)}
                  onChange={() => toggle(i)}
                  className="w-4 h-4 rounded border-gray-300 text-primary-600 focus:ring-primary-500"
                />
                <span className={`text-sm ${checked.has(i) ? 'line-through text-gray-400' : 'text-gray-700 dark:text-gray-200'}`}>
                  {item}
                </span>
              </label>
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
export default function PageBlockRenderer({ block, pageId, index }: {
  block: ResolvedBlock;
  /** Identifiant de la page (persistance locale des checklists). */
  pageId?: string;
  /** Position du bloc dans la page (persistance locale des checklists). */
  index?: number;
}) {
  switch (block.type) {
    case 'KPI':
      return <KpiBlock block={block} />;
    case 'TABLEAU':
      return <TableBlock block={block} />;
    case 'LISTE':
      return <ListBlock block={block} />;
    case 'GRAPHIQUE':
      return <ChartBlock block={block} />;
    case 'CALENDRIER':
      return <CalendarBlock block={block} />;
    case 'TIMELINE':
      return <TimelineBlock block={block} />;
    case 'CHECKLIST':
      return <ChecklistBlock block={block} pageId={pageId} index={index} />;
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
    case 'GRAPHIQUE': return resolveIcon('BarChart3');
    case 'CALENDRIER': return CalendarDays;
    case 'TIMELINE': return Clock;
    case 'CHECKLIST': return CheckSquare;
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
  GRAPHIQUE: 'Graphique',
  CALENDRIER: 'Calendrier',
  TIMELINE: 'Timeline',
  CHECKLIST: 'Checklist',
  TEXTE: 'Texte',
  LIENS: 'Liens rapides',
  RECHERCHE: 'Recherche',
  IMAGES: 'Image',
};
