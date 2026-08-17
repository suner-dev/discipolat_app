import { useMemo, useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import toast from 'react-hot-toast';
import { Link } from 'react-router-dom';
import api from '@/lib/api';
import {
  LayoutTemplate, Plus, Pencil, Trash2, Loader2, Eye, EyeOff, ArrowUp, ArrowDown,
  X, ExternalLink,
} from 'lucide-react';
import type { CustomPage, CustomPageBlock, PageDataSource, ResolvedPage } from '@/types';
import { MENU_ICON_KEYS } from '@/lib/menuIcons';
import ConfigRevisionHistory from '@/components/ConfigRevisionHistory';
import PageBlockRenderer, { BLOCK_TYPE_LABELS } from '@/components/pages/PageBlockRenderer';

const ALL_ROLES = ['ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR', 'MEMBRE'];
const LAYOUTS = ['STACK', 'GRID_2', 'GRID_3'];
const KPI_COLORS = ['primary', 'emerald', 'amber', 'violet', 'rose', 'sky'];
const CHART_TYPES = ['PIE', 'BAR', 'LINE'];
const BLOCK_TYPES = ['KPI', 'TABLEAU', 'LISTE', 'GRAPHIQUE', 'CALENDRIER', 'TIMELINE', 'CHECKLIST', 'TEXTE', 'LIENS', 'RECHERCHE', 'IMAGES'];
const EMPTY_BLOCK: CustomPageBlock = { type: 'KPI', config: {} };

interface PageForm {
  key: string;
  title: string;
  description: string;
  slug: string;
  layout: string;
  roles: string[];
  enabled: boolean;
  blocks: CustomPageBlock[];
}

function toForm(p?: CustomPage): PageForm {
  return {
    key: p?.key || '',
    title: p?.title || '',
    description: p?.description || '',
    slug: p?.slug || '',
    layout: p?.layout || 'STACK',
    roles: p?.roles || [],
    enabled: p?.enabled ?? true,
    blocks: p?.blocks?.map((b) => ({ type: b.type, config: { ...b.config } })) || [],
  };
}

/** Configuration par défaut d'un bloc selon son type. */
function defaultConfig(type: string): Record<string, unknown> {
  switch (type) {
    case 'KPI': return { label: 'Indicateur', source: 'SOULS_TOTAL', icon: 'BarChart3', color: 'primary' };
    case 'TABLEAU': return { title: 'Tableau', source: 'RECENT_SOULS' };
    case 'LISTE': return { title: 'Liste', source: 'RECENT_ALERTS' };
    case 'GRAPHIQUE': return { title: 'Graphique', source: 'SOULS_BY_STATUT', chartType: 'PIE' };
    case 'CALENDRIER': return { title: 'Calendrier', source: 'CALENDAR_EVENTS' };
    case 'TIMELINE': return { title: 'Timeline', source: 'SOULS_TIMELINE' };
    case 'CHECKLIST': return { title: 'Checklist', items: ['Premier élément'] };
    case 'TEXTE': return { content: '' };
    case 'LIENS': return { title: 'Accès rapides', items: [{ label: 'Recherche', href: '/search', icon: 'Search' }] };
    case 'RECHERCHE': return { placeholder: 'Rechercher…' };
    case 'IMAGES': return { url: '', caption: '' };
    default: return {};
  }
}

function BlockEditor({
  block, index, sources, onChange, onRemove, onMove,
}: {
  block: CustomPageBlock;
  index: number;
  sources: PageDataSource[];
  onChange: (b: CustomPageBlock) => void;
  onRemove: () => void;
  onMove: (dir: -1 | 1) => void;
}) {
  const set = (patch: Record<string, unknown>) => onChange({ ...block, config: { ...block.config, ...patch } });
  const sourceOptions = sources.filter((s) => s.type === block.type);

  return (
    <div className="border border-gray-200/70 dark:border-gray-700/50 rounded-xl p-4 bg-white/40 dark:bg-gray-900/30">
      <div className="flex items-center gap-2 mb-3">
        <span className="badge badge-primary text-[10px]">{BLOCK_TYPE_LABELS[block.type] || block.type}</span>
        <span className="text-xs text-gray-400">Bloc {index + 1}</span>
        <div className="flex-1" />
        <button type="button" className="btn-icon btn-icon-sm text-gray-400 hover:text-gray-600" onClick={() => onMove(-1)} aria-label="Monter le bloc">
          <ArrowUp className="w-4 h-4" />
        </button>
        <button type="button" className="btn-icon btn-icon-sm text-gray-400 hover:text-gray-600" onClick={() => onMove(1)} aria-label="Descendre le bloc">
          <ArrowDown className="w-4 h-4" />
        </button>
        <button type="button" className="btn-icon btn-icon-sm text-gray-400 hover:text-red-500" onClick={onRemove} aria-label="Supprimer le bloc">
          <Trash2 className="w-4 h-4" />
        </button>
      </div>

      {block.type === 'KPI' && (
        <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
          <div>
            <label className="label">Libellé</label>
            <input className="input" value={(block.config.label as string) || ''} onChange={(e) => set({ label: e.target.value })} />
          </div>
          <div>
            <label className="label">Source de données</label>
            <select className="input" value={(block.config.source as string) || ''} onChange={(e) => set({ source: e.target.value })}>
              {sourceOptions.map((s) => <option key={s.key} value={s.key}>{s.label}</option>)}
            </select>
          </div>
          <div>
            <label className="label">Icône</label>
            <select className="input" value={(block.config.icon as string) || 'BarChart3'} onChange={(e) => set({ icon: e.target.value })}>
              {MENU_ICON_KEYS.map((k) => <option key={k} value={k}>{k}</option>)}
            </select>
          </div>
          <div>
            <label className="label">Couleur</label>
            <select className="input" value={(block.config.color as string) || 'primary'} onChange={(e) => set({ color: e.target.value })}>
              {KPI_COLORS.map((c) => <option key={c} value={c}>{c}</option>)}
            </select>
          </div>
        </div>
      )}

      {(block.type === 'TABLEAU' || block.type === 'LISTE' || block.type === 'CALENDRIER' || block.type === 'TIMELINE') && (
        <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
          <div>
            <label className="label">Titre</label>
            <input className="input" value={(block.config.title as string) || ''} onChange={(e) => set({ title: e.target.value })} />
          </div>
          <div>
            <label className="label">Source de données</label>
            <select className="input" value={(block.config.source as string) || ''} onChange={(e) => set({ source: e.target.value })}>
              {sourceOptions.map((s) => <option key={s.key} value={s.key}>{s.label}</option>)}
            </select>
          </div>
        </div>
      )}

      {block.type === 'GRAPHIQUE' && (
        <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
          <div>
            <label className="label">Titre</label>
            <input className="input" value={(block.config.title as string) || ''} onChange={(e) => set({ title: e.target.value })} />
          </div>
          <div>
            <label className="label">Source de données</label>
            <select className="input" value={(block.config.source as string) || ''} onChange={(e) => set({ source: e.target.value })}>
              {sourceOptions.map((s) => <option key={s.key} value={s.key}>{s.label}</option>)}
            </select>
          </div>
          <div>
            <label className="label">Type de graphique</label>
            <select className="input" value={(block.config.chartType as string) || 'PIE'} onChange={(e) => set({ chartType: e.target.value })}>
              {CHART_TYPES.map((c) => <option key={c} value={c}>{c === 'PIE' ? 'Camembert' : c === 'BAR' ? 'Barres' : 'Courbe'}</option>)}
            </select>
          </div>
        </div>
      )}

      {block.type === 'CHECKLIST' && (
        <ChecklistEditor block={block} set={set} />
      )}

      {block.type === 'TEXTE' && (
        <div>
          <label className="label">Contenu (texte libre, retours à la ligne conservés)</label>
          <textarea className="input min-h-[90px]" value={(block.config.content as string) || ''} onChange={(e) => set({ content: e.target.value })} />
        </div>
      )}

      {block.type === 'RECHERCHE' && (
        <div>
          <label className="label">Texte d'aide</label>
          <input className="input" value={(block.config.placeholder as string) || ''} onChange={(e) => set({ placeholder: e.target.value })} />
        </div>
      )}

      {block.type === 'IMAGES' && (
        <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
          <div>
            <label className="label">URL de l'image</label>
            <input className="input font-mono text-xs" value={(block.config.url as string) || ''} onChange={(e) => set({ url: e.target.value })} />
          </div>
          <div>
            <label className="label">Légende</label>
            <input className="input" value={(block.config.caption as string) || ''} onChange={(e) => set({ caption: e.target.value })} />
          </div>
        </div>
      )}

      {block.type === 'LIENS' && (
        <LinksEditor block={block} set={set} />
      )}
    </div>
  );
}

function ChecklistEditor({ block, set }: { block: CustomPageBlock; set: (p: Record<string, unknown>) => void }) {
  const items = (block.config.items as string[]) || [];
  const update = (i: number, value: string) => {
    set({ items: items.map((it, j) => (j === i ? value : it)) });
  };
  return (
    <div className="space-y-2">
      <div>
        <label className="label">Titre</label>
        <input className="input" value={(block.config.title as string) || ''} onChange={(e) => set({ title: e.target.value })} />
      </div>
      <label className="label">Éléments à cocher</label>
      {items.map((item, i) => (
        <div key={i} className="flex items-center gap-2">
          <input
            className="input flex-1"
            placeholder="Élément…"
            value={item}
            onChange={(e) => update(i, e.target.value)}
          />
          <button type="button" className="btn-icon btn-icon-sm text-gray-400 hover:text-red-500" onClick={() => set({ items: items.filter((_, j) => j !== i) })} aria-label="Retirer l'élément">
            <X className="w-4 h-4" />
          </button>
        </div>
      ))}
      <button
        type="button"
        className="btn-ghost btn-sm"
        onClick={() => set({ items: [...items, 'Nouvel élément'] })}
      >
        <Plus className="w-4 h-4" /> Ajouter un élément
      </button>
    </div>
  );
}

function LinksEditor({ block, set }: { block: CustomPageBlock; set: (p: Record<string, unknown>) => void }) {
  const items = (block.config.items as { label?: string; href?: string; icon?: string }[]) || [];
  const update = (i: number, patch: Record<string, unknown>) => {
    const next = items.map((it, j) => (j === i ? { ...it, ...patch } : it));
    set({ items: next });
  };
  return (
    <div className="space-y-2">
      <div>
        <label className="label">Titre de la section</label>
        <input className="input" value={(block.config.title as string) || ''} onChange={(e) => set({ title: e.target.value })} />
      </div>
      <label className="label">Liens</label>
      {items.map((item, i) => (
        <div key={i} className="flex items-center gap-2">
          <input
            className="input flex-1"
            placeholder="Libellé"
            value={item.label || ''}
            onChange={(e) => update(i, { label: e.target.value })}
          />
          <input
            className="input flex-1 font-mono text-xs"
            placeholder="/chemin"
            value={item.href || ''}
            onChange={(e) => update(i, { href: e.target.value })}
          />
          <button type="button" className="btn-icon btn-icon-sm text-gray-400 hover:text-red-500" onClick={() => set({ items: items.filter((_, j) => j !== i) })} aria-label="Retirer le lien">
            <X className="w-4 h-4" />
          </button>
        </div>
      ))}
      <button
        type="button"
        className="btn-ghost btn-sm"
        onClick={() => set({ items: [...items, { label: 'Nouveau lien', href: '/', icon: 'CircleDot' }] })}
      >
        <Plus className="w-4 h-4" /> Ajouter un lien
      </button>
    </div>
  );
}

export default function PlatformPagesPage() {
  const queryClient = useQueryClient();
  const [createOpen, setCreateOpen] = useState(false);
  const [editing, setEditing] = useState<CustomPage | null>(null);
  const [form, setForm] = useState<PageForm>(toForm());
  const [preview, setPreview] = useState(false);

  const { data: pages = [], isLoading } = useQuery({
    queryKey: ['platform', 'pages', 'admin'],
    queryFn: async () => {
      const res = await api.get('/pages');
      return res.data as CustomPage[];
    },
  });

  const { data: sources = [] } = useQuery({
    queryKey: ['platform', 'pages', 'sources'],
    queryFn: async () => {
      const res = await api.get('/pages/sources');
      return res.data as PageDataSource[];
    },
  });

  const invalidate = () => {
    queryClient.invalidateQueries({ queryKey: ['platform', 'pages'] });
    queryClient.invalidateQueries({ queryKey: ['pages'] });
  };

  const publishMutation = useMutation({
    mutationFn: async ({ id, published }: { id: string; published: boolean }) => {
      await api.post(`/pages/${id}/publish`, { published });
    },
    onSuccess: () => { invalidate(); toast.success('Publication mise à jour'); },
    onError: () => toast.error('Erreur lors de la publication'),
  });

  const saveMutation = useMutation({
    mutationFn: async (payload: PageForm) => {
      if (editing) {
        await api.put(`/pages/${editing.id}`, payload);
      } else {
        await api.post('/pages', payload);
      }
    },
    onSuccess: () => {
      invalidate();
      setCreateOpen(false);
      setPreview(false);
      toast.success(editing ? 'Page modifiée' : 'Page créée');
    },
    onError: (e: unknown) => {
      const msg = (e as { response?: { data?: { detail?: string } } }).response?.data?.detail;
      toast.error(msg || 'Erreur lors de l’enregistrement');
    },
  });

  const deleteMutation = useMutation({
    mutationFn: async (id: string) => { await api.delete(`/pages/${id}`); },
    onSuccess: () => { invalidate(); toast.success('Page supprimée'); },
    onError: (e: unknown) => {
      const msg = (e as { response?: { data?: { detail?: string } } }).response?.data?.detail;
      toast.error(msg || 'Impossible de supprimer cette page');
    },
  });

  const openCreate = () => {
    setEditing(null);
    // Nouvelle page : un bloc KPI prêt à configurer.
    setForm({ ...toForm(), blocks: [{ type: 'KPI', config: defaultConfig('KPI') }] });
    setPreview(false);
    setCreateOpen(true);
  };
  const openEdit = (p: CustomPage) => {
    setEditing(p);
    setForm(toForm(p));
    setPreview(false);
    setCreateOpen(true);
  };

  // Aperçu local : les blocs configurés, sans données résolues (les valeurs
  // réelles ne sont calculées qu'à la publication, côté serveur).
  const previewPage: ResolvedPage | null = useMemo(() => {
    if (!preview) return null;
    return {
      page: {
        id: editing?.id || 'preview',
        key: form.key || 'APERCU',
        title: form.title || 'Aperçu',
        description: form.description,
        slug: form.slug || 'apercu',
        layout: form.layout,
        blocks: form.blocks,
        roles: form.roles,
        enabled: form.enabled,
        published: false,
        version: 1,
        createdAt: '',
        updatedAt: '',
      },
      blocks: form.blocks.map((b) => ({ type: b.type, config: b.config, data: null })),
    };
  }, [preview, form, editing]);

  const toggleRole = (role: string) => {
    setForm((f) => ({
      ...f,
      roles: f.roles.includes(role) ? f.roles.filter((r) => r !== role) : [...f.roles, role],
    }));
  };

  const updateBlock = (i: number, b: CustomPageBlock) => {
    setForm((f) => ({ ...f, blocks: f.blocks.map((x, j) => (j === i ? b : x)) }));
  };
  const addBlock = (type: string) => {
    setForm((f) => ({ ...f, blocks: [...f.blocks, { type, config: defaultConfig(type) }] }));
  };
  const removeBlock = (i: number) => {
    setForm((f) => ({ ...f, blocks: f.blocks.filter((_, j) => j !== i) }));
  };
  const moveBlock = (i: number, dir: -1 | 1) => {
    setForm((f) => {
      const blocks = [...f.blocks];
      const j = i + dir;
      if (j < 0 || j >= blocks.length) return f;
      [blocks[i], blocks[j]] = [blocks[j], blocks[i]];
      return { ...f, blocks };
    });
  };

  if (isLoading) {
    return <div className="min-h-[40vh] flex items-center justify-center"><div className="spinner h-8 w-8" /></div>;
  }

  return (
    <div className="page-container max-w-5xl">
      <div className="page-header">
        <div>
          <h1 className="page-title">Pages personnalisées</h1>
          <p className="page-subtitle">
            Créez des pages métier avec des blocs (KPI, tableaux, listes, textes, liens…).
            Toutes les données sont résolues côté serveur sur les entités réelles, scopées
            selon l'espace métier de chaque utilisateur. Chaque modification est versionnée.
          </p>
        </div>
        <div className="page-header-actions">
          <button className="btn-primary btn-sm" onClick={openCreate}>
            <Plus className="w-4 h-4" /> Nouvelle page
          </button>
        </div>
      </div>

      <div className="mb-6">
        <ConfigRevisionHistory entityType="CUSTOM_PAGE" title="Historique des pages" />
      </div>

      {pages.length === 0 && (
        <div className="empty-state glass-card">
          <LayoutTemplate className="empty-state-icon" />
          <p className="text-gray-500 dark:text-gray-400">Aucune page personnalisée.</p>
        </div>
      )}

      <div className="space-y-3">
        {pages.map((p) => (
          <div key={p.id} className="glass-card px-5 py-4 flex items-center gap-4">
            <div className={`w-10 h-10 rounded-xl flex items-center justify-center flex-shrink-0 ${p.published ? 'bg-gradient-to-br from-primary-500 to-primary-700 text-white' : 'bg-gray-100 dark:bg-gray-800 text-gray-400'}`}>
              <LayoutTemplate className="w-5 h-5" />
            </div>
            <div className="flex-1 min-w-0">
              <div className="flex items-center gap-2 flex-wrap">
                <p className="text-sm font-semibold text-gray-900 dark:text-gray-100 truncate">{p.title}</p>
                {p.published
                  ? <span className="badge badge-emerald">Publiée · v{p.version}</span>
                  : <span className="badge badge-gray">Brouillon</span>}
                {!p.enabled && <span className="badge badge-rose">Désactivée</span>}
              </div>
              <p className="text-xs text-gray-400 mt-0.5 font-mono">/pages/{p.slug}</p>
              {p.roles.length > 0 && (
                <p className="text-[11px] text-gray-400 mt-1">
                  Rôles : {p.roles.join(', ')}
                </p>
              )}
            </div>
            <div className="flex items-center gap-1.5">
              {p.published && (
                <Link to={`/pages/${p.slug}`} className="btn-icon text-gray-400 hover:text-primary-600" aria-label={`Ouvrir ${p.title}`}>
                  <ExternalLink className="w-4 h-4" />
                </Link>
              )}
              <button className="btn-icon text-gray-400 hover:text-gray-700 dark:hover:text-gray-200" onClick={() => openEdit(p)} aria-label={`Modifier ${p.title}`}>
                <Pencil className="w-4 h-4" />
              </button>
              <button className="btn-icon text-gray-400 hover:text-red-500" onClick={() => { if (confirm(`Supprimer la page « ${p.title} » ?`)) deleteMutation.mutate(p.id); }} aria-label={`Supprimer ${p.title}`}>
                <Trash2 className="w-4 h-4" />
              </button>
              <button
                role="switch"
                aria-checked={p.published}
                aria-label={`Publier ${p.title}`}
                onClick={() => publishMutation.mutate({ id: p.id, published: !p.published })}
                className={`relative ml-2 w-11 h-6 rounded-full transition-colors duration-200 ${p.published ? 'bg-emerald-500' : 'bg-gray-300 dark:bg-gray-600'}`}
              >
                <span className={`absolute top-0.5 left-0.5 w-5 h-5 rounded-full bg-white shadow transition-transform duration-200 ${p.published ? 'translate-x-5' : ''}`} />
              </button>
            </div>
          </div>
        ))}
      </div>

      {/* Modal éditeur */}
      {createOpen && (
        <div className="modal-overlay" onClick={() => setCreateOpen(false)}>
          <div className="modal-content max-w-3xl" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <h3 className="text-base font-bold text-gray-900 dark:text-gray-100">
                {editing ? `Modifier la page — ${editing.title}` : 'Nouvelle page'}
              </h3>
              <button className="btn-icon text-gray-400 hover:text-gray-600" onClick={() => setCreateOpen(false)}>×</button>
            </div>
            <div className="modal-body max-h-[70vh] overflow-y-auto space-y-5">
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                <div>
                  <label className="label">Titre</label>
                  <input className="input" value={form.title} onChange={(e) => setForm({ ...form, title: e.target.value })} />
                </div>
                <div>
                  <label className="label">Adresse (/pages/…)</label>
                  <input className="input font-mono" value={form.slug} onChange={(e) => setForm({ ...form, slug: e.target.value })} placeholder="ma-page" />
                </div>
              </div>
              {!editing && (
                <div>
                  <label className="label">Clé (unique, technique)</label>
                  <input className="input font-mono" value={form.key} onChange={(e) => setForm({ ...form, key: e.target.value.toUpperCase() })} placeholder="MA_PAGE" />
                </div>
              )}
              <div>
                <label className="label">Description</label>
                <input className="input" value={form.description} onChange={(e) => setForm({ ...form, description: e.target.value })} />
              </div>
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                <div>
                  <label className="label">Disposition</label>
                  <select className="input" value={form.layout} onChange={(e) => setForm({ ...form, layout: e.target.value })}>
                    {LAYOUTS.map((l) => <option key={l} value={l}>{l === 'STACK' ? 'Une colonne' : l === 'GRID_2' ? 'Deux colonnes' : 'Trois colonnes'}</option>)}
                  </select>
                </div>
                <div className="flex items-end gap-3">
                  <button
                    role="switch"
                    aria-checked={form.enabled}
                    onClick={() => setForm({ ...form, enabled: !form.enabled })}
                    className={`relative w-11 h-6 rounded-full transition-colors duration-200 ${form.enabled ? 'bg-primary-600' : 'bg-gray-300 dark:bg-gray-600'}`}
                  >
                    <span className={`absolute top-0.5 left-0.5 w-5 h-5 rounded-full bg-white shadow transition-transform duration-200 ${form.enabled ? 'translate-x-5' : ''}`} />
                  </button>
                  <span className="text-sm text-gray-500 dark:text-gray-400">{form.enabled ? 'Page active' : 'Page désactivée'}</span>
                </div>
              </div>

              <div>
                <label className="label">Rôles autorisés (aucun = tous les utilisateurs connectés)</label>
                <div className="flex flex-wrap gap-2">
                  {ALL_ROLES.map((role) => (
                    <button
                      key={role}
                      type="button"
                      onClick={() => toggleRole(role)}
                      className={`px-3 py-1.5 rounded-full text-xs font-medium transition-colors border ${
                        form.roles.includes(role)
                          ? 'bg-primary-600 border-primary-600 text-white'
                          : 'bg-gray-50 dark:bg-gray-800 border-gray-200 dark:border-gray-700 text-gray-500 dark:text-gray-400 hover:border-primary-300'
                      }`}
                    >
                      {role}
                    </button>
                  ))}
                </div>
              </div>

              <div>
                <div className="flex items-center justify-between mb-2">
                  <label className="label !mb-0">Blocs de la page</label>
                  <div className="flex items-center gap-2">
                    <button
                      type="button"
                      className={`btn-ghost btn-sm ${preview ? 'text-primary-600' : ''}`}
                      onClick={() => setPreview((p) => !p)}
                    >
                      {preview ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
                      {preview ? 'Masquer l’aperçu' : 'Aperçu'}
                    </button>
                  </div>
                </div>

                {preview && previewPage && (
                  <div className="mb-4 p-4 rounded-xl border border-dashed border-primary-300/60 bg-primary-50/40 dark:bg-primary-950/20">
                    <p className="text-[11px] text-primary-600 dark:text-primary-400 font-medium mb-3">
                      Aperçu local — les valeurs des KPI et les tableaux sont calculés à la publication,
                      avec les données réelles de chaque utilisateur.
                    </p>
                    <div className={previewPage.page.layout === 'GRID_2'
                      ? 'grid grid-cols-1 md:grid-cols-2 gap-3'
                      : previewPage.page.layout === 'GRID_3'
                        ? 'grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-3'
                        : 'flex flex-col gap-3'}>
                      {previewPage.blocks.map((b, i) => (
                        <PageBlockRenderer key={i} block={b} pageId={editing?.id} index={i} />
                      ))}
                    </div>
                  </div>
                )}

                <div className="space-y-3">
                  {form.blocks.map((b, i) => (
                    <BlockEditor
                      key={i}
                      block={b}
                      index={i}
                      sources={sources}
                      onChange={(nb) => updateBlock(i, nb)}
                      onRemove={() => removeBlock(i)}
                      onMove={(dir) => moveBlock(i, dir)}
                    />
                  ))}
                </div>

                <div className="mt-3">
                  <label className="label">Ajouter un bloc</label>
                  <div className="flex flex-wrap gap-2">
                    {BLOCK_TYPES.map((t) => (
                      <button key={t} type="button" className="btn-ghost btn-sm" onClick={() => addBlock(t)}>
                        <Plus className="w-4 h-4" /> {BLOCK_TYPE_LABELS[t]}
                      </button>
                    ))}
                  </div>
                </div>
              </div>
            </div>
            <div className="modal-footer">
              <button className="btn-ghost btn-sm" onClick={() => setCreateOpen(false)}>Annuler</button>
              <button className="btn-primary btn-sm" onClick={() => saveMutation.mutate(form)} disabled={saveMutation.isPending}>
                {saveMutation.isPending ? <Loader2 className="w-4 h-4 animate-spin" /> : <LayoutTemplate className="w-4 h-4" />}
                {editing ? 'Enregistrer' : 'Créer'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
