import { useMemo, useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import api, { getErrorMessage } from '@/lib/api';
import { useAuth } from '@/contexts/AuthContext';
import DataTable from '@/components/shared/DataTable';
import { useDictionaries } from '@/hooks/useDictionaries';
import type { Prayer, PageResponse, CategoriePriere, PrioritePriere, VisibilitePriere } from '@/types';
import type { ColumnDef } from '@/types/table';
import {
  Heart,
  Plus,
  Search,
  Filter,
  CheckCircle2,
  Clock,
  Loader2,
  X,
  Flame,
  Star,
  Tag,
  Eye,
  EyeOff,
  Pencil,
  Trash2,
  MessageSquare,
  Download,
  BarChart3,
} from 'lucide-react';
import toast from 'react-hot-toast';

/** Replis (dictionnaires indisponibles) — les valeurs réelles viennent de la base. */
const CATEGORIE_FALLBACK: Record<string, string> = {
  SANTE: 'Santé',
  FAMILLE: 'Famille',
  TRAVAIL: 'Travail',
  SPIRITUEL: 'Spirituel',
  AUTRE: 'Autre',
};

const PRIORITE_FALLBACK: Record<string, string> = {
  BASSE: 'Basse',
  MOYENNE: 'Moyenne',
  HAUTE: 'Haute',
};

const PRIORITE_ICONS: Record<PrioritePriere, typeof Flame> = {
  BASSE: Tag,
  MOYENNE: Star,
  HAUTE: Flame,
};

const PRIORITE_COLORS: Record<PrioritePriere, string> = {
  BASSE: 'text-gray-500',
  MOYENNE: 'text-amber-500',
  HAUTE: 'text-red-500',
};

interface PrayerFormState {
  titre: string;
  contenu: string;
  categorie: CategoriePriere;
  priorite: PrioritePriere;
  visibilite: VisibilitePriere;
}

function createPrayerForm(
  key: string,
  form: PrayerFormState,
  update: (partial: Partial<PrayerFormState>) => void,
  submitLabel: string,
  onSubmit: () => void,
  isPending: boolean,
  onCancel: () => void,
  categorieOptions: { code: string; label: string }[],
  prioriteOptions: { code: string; label: string }[]
) {
  return (
    <div key={key} className="card p-6 mb-6 animate-slide-up">
      <div className="flex items-center justify-between mb-4">
        <h3 className="text-lg font-semibold text-gray-900 dark:text-gray-100">
          {submitLabel === 'Créer' ? 'Nouveau sujet de prière' : 'Modifier le sujet'}
        </h3>
        {onCancel && (
          <button onClick={onCancel} className="p-1 rounded-lg hover:bg-gray-100 dark:hover:bg-gray-800">
            <X className="w-5 h-5 text-gray-500" />
          </button>
        )}
      </div>
      <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
        <div className="sm:col-span-2">
          <label className="label">Titre *</label>
          <input
            className="input"
            value={form.titre}
            onChange={(e) => update({ titre: e.target.value })}
            placeholder="Ex: Guérison pour..."
          />
        </div>
        <div className="sm:col-span-2">
          <label className="label">Description</label>
          <textarea
            className="input"
            rows={3}
            value={form.contenu}
            onChange={(e) => update({ contenu: e.target.value })}
            placeholder="Détails du sujet de prière..."
          />
        </div>
        <div>
          <label className="label">Catégorie</label>
          <select className="input" value={form.categorie} onChange={(e) => update({ categorie: e.target.value as CategoriePriere })}>
            {categorieOptions.map((o) => (<option key={o.code} value={o.code}>{o.label}</option>))}
          </select>
        </div>
        <div>
          <label className="label">Priorité</label>
          <select className="input" value={form.priorite} onChange={(e) => update({ priorite: e.target.value as PrioritePriere })}>
            {prioriteOptions.map((o) => (<option key={o.code} value={o.code}>{o.label}</option>))}
          </select>
        </div>
        <div>
          <label className="label">Visibilité</label>
          <select className="input" value={form.visibilite} onChange={(e) => update({ visibilite: e.target.value as VisibilitePriere })}>
            <option value="GENERALE">Général (tous)</option>
            <option value="PASTEUR_RESPONSABLE">Pasteur + Responsables</option>
            <option value="FAISEUR">Chefs de famille + Faiseurs</option>
            <option value="PARTAGEE">Famille</option>
            <option value="PRIVEE">Privé (moi uniquement)</option>
          </select>
        </div>
      </div>
      <div className="flex justify-end gap-3 mt-4">
        {onCancel && <button onClick={onCancel} className="btn-secondary btn-sm">Annuler</button>}
        <button
          onClick={onSubmit}
          disabled={!form.titre || isPending}
          className="btn-primary btn-sm"
        >
          {isPending ? <Loader2 className="w-4 h-4 animate-spin" /> : submitLabel === 'Créer' ? <Plus className="w-4 h-4" /> : <Pencil className="w-4 h-4" />}
          {submitLabel}
        </button>
      </div>
    </div>
  );
}

export default function PrayersPage() {
  const { user } = useAuth();
  const queryClient = useQueryClient();
  const dictionaries = useDictionaries();
  const [page, setPage] = useState(0);
  const [viewMode, setViewMode] = useState<'list' | 'grace'>('list');
  const [search, setSearch] = useState('');
  const [catFilter, setCatFilter] = useState<CategoriePriere | ''>('');
  const [statutFilter, setStatutFilter] = useState('');
  const [visibiliteFilter, setVisibiliteFilter] = useState<VisibilitePriere | ''>('');
  const [showFilters, setShowFilters] = useState(false);
  const [showCreate, setShowCreate] = useState(false);
  const [showEdit, setShowEdit] = useState(false);
  const [editingPrayer, setEditingPrayer] = useState<Prayer | null>(null);
  const [showDeleteConfirm, setShowDeleteConfirm] = useState<string | null>(null);
  const [showAnswerForm, setShowAnswerForm] = useState<string | null>(null);
  const [temoignageText, setTemoignageText] = useState('');
  const [newPrayer, setNewPrayer] = useState({
    titre: '',
    contenu: '',
    categorie: 'AUTRE' as CategoriePriere,
    priorite: 'MOYENNE' as PrioritePriere,
    visibilite: 'PARTAGEE' as VisibilitePriere,
  });
  const [editForm, setEditForm] = useState({
    titre: '',
    contenu: '',
    categorie: 'AUTRE' as CategoriePriere,
    priorite: 'MOYENNE' as PrioritePriere,
    visibilite: 'PARTAGEE' as VisibilitePriere,
  });

  const categorieEntries = useMemo(() => {
    const configured = dictionaries.options('PRAYER_CATEGORIE');
    return configured.length > 0
      ? configured.map((e) => ({ code: e.code, label: e.label, color: e.color }))
      : Object.entries(CATEGORIE_FALLBACK).map(([code, label]) => ({ code, label }));
  }, [dictionaries]);

  const prioriteEntries = useMemo(() => {
    const configured = dictionaries.options('PRAYER_PRIORITE');
    return configured.length > 0
      ? configured.map((e) => ({ code: e.code, label: e.label }))
      : Object.entries(PRIORITE_FALLBACK).map(([code, label]) => ({ code, label }));
  }, [dictionaries]);

  const categorieLabel = (code: string) =>
    dictionaries.label('PRAYER_CATEGORIE', code) || CATEGORIE_FALLBACK[code] || code;
  const categorieColor = (code: string) => dictionaries.color('PRAYER_CATEGORIE', code);
  const prioriteLabel = (code: string) =>
    dictionaries.label('PRAYER_PRIORITE', code) || PRIORITE_FALLBACK[code] || code;

  const { data, isLoading } = useQuery({
    queryKey: ['prayers', page, search, catFilter, statutFilter, visibiliteFilter],
    queryFn: async () => {
      const params = new URLSearchParams({ size: '20', page: String(page) });
      if (search) params.set('search', search);
      if (catFilter) params.set('categorie', catFilter);
      if (statutFilter) params.set('statut', statutFilter);
      if (visibiliteFilter) params.set('visibilite', visibiliteFilter);
      const res = await api.get(`/prayers?${params}`);
      return res.data as PageResponse<Prayer>;
    },
  });

  const createMutation = useMutation({
    mutationFn: async (prayer: typeof newPrayer) => {
      await api.post('/prayers', {
        titre: prayer.titre,
        description: prayer.contenu,
        categorie: prayer.categorie,
        priorite: prayer.priorite,
        visibilite: prayer.visibilite,
      });
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['prayers'] });
      toast.success('Sujet de prière créé');
      setShowCreate(false);
      setNewPrayer({ titre: '', contenu: '', categorie: 'AUTRE', priorite: 'MOYENNE', visibilite: 'PARTAGEE' });
    },
    onError: (err) => toast.error(getErrorMessage(err)),
  });

  const answerMutation = useMutation({
    mutationFn: async ({ id, temoignage }: { id: string; temoignage?: string }) => {
      await api.patch(`/prayers/${id}/answer`, { temoignage });
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['prayers'] });
      toast.success('Sujet marqué comme exaucé');
      setShowAnswerForm(null);
      setTemoignageText('');
    },
    onError: (err) => toast.error(getErrorMessage(err)),
  });

  const updateMutation = useMutation({
    mutationFn: async ({ id, data }: { id: string; data: typeof editForm }) => {
      await api.put(`/prayers/${id}`, {
        titre: data.titre,
        contenu: data.contenu || undefined,
        categorie: data.categorie,
        priorite: data.priorite,
        visibilite: data.visibilite,
      });
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['prayers'] });
      toast.success('Sujet de prière mis à jour');
      setShowEdit(false);
      setEditingPrayer(null);
    },
    onError: (err) => toast.error(getErrorMessage(err)),
  });

  const deleteMutation = useMutation({
    mutationFn: async (id: string) => {
      await api.delete(`/prayers/${id}`);
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['prayers'] });
      toast.success('Sujet de prière supprimé');
      setShowDeleteConfirm(null);
    },
    onError: (err) => toast.error(getErrorMessage(err)),
  });

  const openEdit = (prayer: Prayer) => {
    setEditingPrayer(prayer);
    setEditForm({
      titre: prayer.titre,
      contenu: prayer.contenu || '',
      categorie: prayer.categorie,
      priorite: prayer.priorite,
      visibilite: prayer.visibilite,
    });
    setShowEdit(true);
  };

  const columns: ColumnDef<Prayer>[] = [
    {
      header: 'Titre',
      cell: (prayer) => (
        <div>
          <p className="text-sm font-medium text-gray-900 dark:text-gray-100">{prayer.titre}</p>
          <div className="flex flex-wrap gap-1 mt-1">
            {prayer.contenu && (
              <p className="text-xs text-gray-500 dark:text-gray-400 truncate max-w-xs">{prayer.contenu}</p>
            )}
            {prayer.statut === 'EXAUCEE' && prayer.temoignage && (
              <span className="inline-flex items-center gap-1 text-xs text-green-600 dark:text-green-400 italic">
                <MessageSquare className="w-3 h-3" />
                "{prayer.temoignage.slice(0, 60)}{prayer.temoignage.length > 60 ? '...' : ''}"
              </span>
            )}
          </div>
        </div>
      ),
    },
    {
      header: 'Catégorie',
      cell: (prayer) => (
        <span
          className="inline-flex items-center px-2 py-1 rounded-full text-xs font-medium bg-gray-100 text-gray-800 dark:bg-gray-700 dark:text-gray-300"
          style={categorieColor(prayer.categorie) ? { backgroundColor: `${categorieColor(prayer.categorie)}22`, color: categorieColor(prayer.categorie) } : undefined}
        >
          {categorieLabel(prayer.categorie)}
        </span>
      ),
    },
    {
      header: 'Priorité',
      cell: (prayer) => {
        const Icon = prayer.priorite === 'HAUTE' ? Flame : prayer.priorite === 'MOYENNE' ? Star : Tag;
        return (
          <span className="inline-flex items-center gap-1 text-xs font-medium text-gray-500">
            <Icon className="w-3 h-3" />
            {prioriteLabel(prayer.priorite)}
          </span>
        );
      },
    },
    {
      header: 'Visibilité',
      cell: (prayer) => {
        const visColors: Record<string, string> = {
          GENERALE: 'bg-sky-100 text-sky-800 dark:bg-sky-900/30 dark:text-sky-300',
          PASTEUR_RESPONSABLE: 'bg-purple-100 text-purple-800 dark:bg-purple-900/30 dark:text-purple-300',
          FAISEUR: 'bg-amber-100 text-amber-800 dark:bg-amber-900/30 dark:text-amber-300',
          PARTAGEE: 'bg-blue-100 text-blue-800 dark:bg-blue-900/30 dark:text-blue-300',
          PRIVEE: 'bg-gray-100 text-gray-800 dark:bg-gray-700 dark:text-gray-300',
        };
        const visLabels: Record<string, string> = {
          GENERALE: 'Général',
          PASTEUR_RESPONSABLE: 'Pasteur + Resp.',
          FAISEUR: 'Chefs + Faiseurs',
          PARTAGEE: 'Famille',
          PRIVEE: 'Privé',
        };
        return (
          <span className={`inline-flex items-center gap-1 px-2 py-1 rounded-full text-xs font-medium ${visColors[prayer.visibilite] || visColors.PRIVEE}`}>
            {prayer.visibilite === 'PRIVEE' ? <EyeOff className="w-3 h-3" /> : <Eye className="w-3 h-3" />}
            {visLabels[prayer.visibilite] || prayer.visibilite}
          </span>
        );
      },
    },
    {
      header: 'Statut',
      cell: (prayer) => (
        <span className={prayer.statut === 'EXAUCEE' ? 'badge-success' : 'badge-warning'}>
          {prayer.statut === 'EXAUCEE' ? 'Exaucé' : 'En cours'}
        </span>
      ),
    },
    {
      header: 'Date',
      cell: (prayer) => new Date(prayer.dateCreation).toLocaleDateString('fr-FR', { day: 'numeric', month: 'short' }),
    },
    {
      header: 'Actions',
      cell: (prayer) => {
        const isOwner = prayer.auteurId === user?.id;
        return (
          <div className="flex items-center gap-1">
            {prayer.statut === 'EN_COURS' && isOwner && (
              <>
                <button
                  onClick={() => {
                    setShowAnswerForm(prayer.id);
                    setTemoignageText(prayer.temoignage || '');
                  }}
                  className="btn-ghost btn-sm text-green-600"
                  title="Marquer comme exaucé"
                >
                  <CheckCircle2 className="w-4 h-4" />
                </button>
                <button
                  onClick={() => openEdit(prayer)}
                  className="btn-ghost btn-sm text-blue-600"
                  title="Modifier"
                >
                  <Pencil className="w-4 h-4" />
                </button>
              </>
            )}
            {isOwner && (
              <button
                onClick={() => setShowDeleteConfirm(prayer.id)}
                className="btn-ghost btn-sm text-red-600"
                title="Supprimer"
              >
                <Trash2 className="w-4 h-4" />
              </button>
            )}
          </div>
        );
      },
    },
  ];

  // Export CSV
  const exportCsv = () => {
    const rows = [['Titre', 'Catégorie', 'Priorité', 'Visibilité', 'Statut', 'Date']];
    allPrayers.forEach((p) => {
      rows.push([
        p.titre,
        categorieLabel(p.categorie),
        prioriteLabel(p.priorite),
        p.visibilite,
        p.statut === 'EXAUCEE' ? 'Exaucé' : 'En cours',
        new Date(p.dateCreation).toLocaleDateString('fr-FR'),
      ]);
    });
    const csv = rows.map(r => r.map(c => `"${c}"`).join(',')).join('\n');
    const blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `prieres_${new Date().toISOString().slice(0, 10)}.csv`;
    a.click();
    URL.revokeObjectURL(url);
  };

  // Compute stats from all loaded data
  const allPrayers = data?.content || [];
  const stats = useMemo(() => ({
    total: data?.totalElements ?? allPrayers.length,
    enCours: allPrayers.filter(p => p.statut === 'EN_COURS').length,
    exauces: allPrayers.filter(p => p.statut === 'EXAUCEE').length,
    haute: allPrayers.filter(p => p.priorite === 'HAUTE').length,
  }), [data, allPrayers]);

  // Category distribution
  const catDistribution = useMemo(() => {
    const dist: Record<string, number> = {};
    allPrayers.forEach((p) => { dist[p.categorie] = (dist[p.categorie] || 0) + 1; });
    return Object.entries(dist).sort((a, b) => b[1] - a[1]);
  }, [allPrayers]);

  const hasActiveFilters = Boolean(catFilter || statutFilter || visibiliteFilter || search);

  return (
    <div className="page-container">
      <div className="page-header">
        <div>
          <h1 className="page-title">Prières</h1>
          <p className="text-gray-500 dark:text-gray-400 mt-1">
            Suivi des sujets de prière de la famille — {stats.total} sujet(s)
          </p>
        </div>
        <div className="flex gap-2">
          <button onClick={() => setViewMode(viewMode === 'list' ? 'grace' : 'list')} className={`btn-secondary btn-sm ${viewMode === 'grace' ? 'bg-green-50 dark:bg-green-900/20 border-green-300 dark:border-green-700' : ''}`}>
            {viewMode === 'grace' ? <Heart className="w-4 h-4" /> : <CheckCircle2 className="w-4 h-4" />} {viewMode === 'grace' ? 'Sujets actifs' : 'Actions de grâce'}
          </button>
          <button onClick={exportCsv} className="btn-secondary btn-sm">
            <Download className="w-4 h-4" /> Export
          </button>
          <button onClick={() => setShowFilters(!showFilters)} className={`btn-secondary btn-sm ${showFilters || hasActiveFilters ? 'bg-primary-50 dark:bg-primary-900/20 border-primary-300 dark:border-primary-700' : ''}`}>
            <Filter className="w-4 h-4" /> Filtres
          </button>
          <button onClick={() => setShowCreate(!showCreate)} className="btn-primary btn-sm">
            <Plus className="w-4 h-4" /> Nouveau sujet
          </button>
        </div>
      </div>

      {/* Stats cards */}
      <div className="grid grid-cols-2 sm:grid-cols-4 gap-3 mb-6">
        {[
          { label: 'Total', value: stats.total, icon: Heart, color: 'from-primary-500 to-primary-600', filterVal: '' },
          { label: 'En cours', value: stats.enCours, icon: Clock, color: 'from-amber-500 to-orange-500', filterVal: 'EN_COURS' },
          { label: 'Exaucés', value: stats.exauces, icon: CheckCircle2, color: 'from-emerald-500 to-green-500', filterVal: 'EXAUCEE' },
          { label: 'Haute priorité', value: stats.haute, icon: Flame, color: 'from-red-500 to-rose-500', filterVal: '' },
        ].map((s, i) => (
          <button
            key={s.label}
            type="button"
            onClick={() => { if (s.filterVal) { setStatutFilter(statutFilter === s.filterVal ? '' : s.filterVal); setPage(0); } }}
            className={`stat-card animate-slide-up text-left cursor-pointer hover:shadow-lg hover:-translate-y-0.5 transition-all duration-200 ${statutFilter === s.filterVal && s.filterVal ? 'ring-2 ring-primary-500/50' : ''}`}
            style={{ animationDelay: `${i * 50}ms` }}
          >
            <div className={`absolute top-0 left-0 right-0 h-0.5 bg-gradient-to-r ${s.color} opacity-60`} />
            <div className="flex items-start justify-between mb-2">
              <span className="stat-label text-[10px]">{s.label}</span>
              <div className={`p-1.5 rounded-lg bg-gradient-to-br ${s.color} text-white shadow-sm`}>
                <s.icon className="w-3.5 h-3.5" />
              </div>
            </div>
            <p className="stat-value text-xl">{s.value}</p>
          </button>
        ))}
      </div>

      {/* Category distribution */}
      {catDistribution.length > 0 && viewMode === 'list' && (
        <div className="card p-4 mb-6">
          <div className="flex items-center gap-2 mb-3">
            <BarChart3 className="w-4 h-4 text-gray-400" />
            <p className="text-xs font-semibold text-gray-500 uppercase">Répartition par catégorie</p>
          </div>
          <div className="flex flex-wrap gap-3">
            {catDistribution.map(([cat, count]) => {
              const pct = stats.total > 0 ? Math.round((count / stats.total) * 100) : 0;
              return (
                <button
                  key={cat}
                  onClick={() => { setCatFilter(catFilter === cat ? '' : cat as CategoriePriere); setPage(0); }}
                  className={`flex items-center gap-2 px-3 py-2 rounded-xl text-xs font-medium transition-all ${catFilter === cat ? 'ring-2 ring-primary-500/50 shadow-sm' : ''} bg-gray-50 dark:bg-gray-800/50 hover:bg-gray-100 dark:hover:bg-gray-700/50`}
                >
                  <span className="w-2 h-2 rounded-full" style={{ backgroundColor: categorieColor(cat) || '#9ca3af' }} />
                  <span className="text-gray-700 dark:text-gray-300">{categorieLabel(cat)}</span>
                  <span className="text-gray-400">{count}</span>
                  <span className="text-[10px] text-gray-400">{pct}%</span>
                </button>
              );
            })}
          </div>
        </div>
      )}

      {/* Create form */}
      {showCreate && createPrayerForm('newPrayer', newPrayer, (v) => setNewPrayer({ ...newPrayer, ...v }), 'Créer', () => createMutation.mutate(newPrayer), createMutation.isPending, () => setShowCreate(false), categorieEntries, prioriteEntries)}

      {/* Edit form */}
      {showEdit && editingPrayer && createPrayerForm('edit', editForm, (v) => setEditForm({ ...editForm, ...v }), 'Enregistrer', () => updateMutation.mutate({ id: editingPrayer.id, data: editForm }), updateMutation.isPending, () => { setShowEdit(false); setEditingPrayer(null); }, categorieEntries, prioriteEntries)}

      {/* Answer / témoignage modal */}
      {showAnswerForm && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50" onClick={() => { setShowAnswerForm(null); setTemoignageText(''); }}>
          <div className="card p-6 w-full max-w-md mx-4 animate-slide-up" onClick={(e) => e.stopPropagation()}>
            <div className="flex items-center justify-between mb-4">
              <h3 className="text-lg font-semibold text-gray-900 dark:text-gray-100">
                Marquer comme exaucé
              </h3>
              <button onClick={() => { setShowAnswerForm(null); setTemoignageText(''); }} className="p-1 rounded-lg hover:bg-gray-100 dark:hover:bg-gray-800">
                <X className="w-5 h-5 text-gray-500" />
              </button>
            </div>
            <p className="text-sm text-gray-500 dark:text-gray-400 mb-4">
              Vous pouvez ajouter un témoignage pour partager comment Dieu a répondu.
            </p>
            <textarea
              className="input mb-4"
              rows={3}
              value={temoignageText}
              onChange={(e) => setTemoignageText(e.target.value)}
              placeholder="Témoignage (optionnel)..."
            />
            <div className="flex justify-end gap-2">
              <button onClick={() => { setShowAnswerForm(null); setTemoignageText(''); }} className="btn-secondary btn-sm">Annuler</button>
              <button
                onClick={() => answerMutation.mutate({ id: showAnswerForm, temoignage: temoignageText || undefined })}
                disabled={answerMutation.isPending}
                className="btn-primary btn-sm bg-green-600 hover:bg-green-700"
              >
                {answerMutation.isPending ? <Loader2 className="w-4 h-4 animate-spin" /> : <CheckCircle2 className="w-4 h-4" />}
                Confirmer exaucé
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Delete confirmation modal */}
      {showDeleteConfirm && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50" onClick={() => setShowDeleteConfirm(null)}>
          <div className="card p-6 w-full max-w-sm mx-4 animate-slide-up" onClick={(e) => e.stopPropagation()}>
            <div className="flex items-center gap-3 mb-4">
              <div className="w-10 h-10 rounded-full bg-red-100 dark:bg-red-900/30 flex items-center justify-center">
                <Trash2 className="w-5 h-5 text-red-600" />
              </div>
              <div>
                <h3 className="text-lg font-semibold text-gray-900 dark:text-gray-100">Confirmer la suppression</h3>
                <p className="text-sm text-gray-500 dark:text-gray-400">Cette action est irréversible.</p>
              </div>
            </div>
            <div className="flex justify-end gap-2">
              <button onClick={() => setShowDeleteConfirm(null)} className="btn-secondary btn-sm">Annuler</button>
              <button
                onClick={() => deleteMutation.mutate(showDeleteConfirm)}
                disabled={deleteMutation.isPending}
                className="btn-primary btn-sm bg-red-600 hover:bg-red-700"
              >
                {deleteMutation.isPending ? <Loader2 className="w-4 h-4 animate-spin" /> : <Trash2 className="w-4 h-4" />}
                Supprimer
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Search and filters */}
      <div className="card p-4 mb-6">
        <div className="flex flex-col sm:flex-row gap-3">
          <div className="flex-1 relative">
            <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400" />
            <input
              type="text"
              placeholder="Rechercher un sujet de prière..."
              value={search}
              onChange={(e) => { setSearch(e.target.value); setPage(0); }}
              className="input pl-10"
            />
          </div>
        </div>
        {showFilters && (
          <div className="flex flex-wrap gap-3 mt-3 pt-3 border-t border-gray-200 dark:border-gray-700">
            <select value={catFilter} onChange={(e) => { setCatFilter(e.target.value as CategoriePriere | ''); setPage(0); }} className="input w-auto">
              <option value="">Toutes catégories</option>
              {categorieEntries.map((o) => (<option key={o.code} value={o.code}>{o.label}</option>))}
            </select>
            <select value={statutFilter} onChange={(e) => { setStatutFilter(e.target.value); setPage(0); }} className="input w-auto">
              <option value="">Tous statuts</option>
              <option value="EN_COURS">En cours</option>
              <option value="EXAUCEE">Exaucé</option>
            </select>
            <select value={visibiliteFilter} onChange={(e) => { setVisibiliteFilter(e.target.value as VisibilitePriere | ''); setPage(0); }} className="input w-auto">
              <option value="">Toutes visibilités</option>
              <option value="GENERALE">Général</option>
              <option value="PASTEUR_RESPONSABLE">Pasteur + Responsables</option>
              <option value="FAISEUR">Chefs + Faiseurs</option>
              <option value="PARTAGEE">Famille</option>
              <option value="PRIVEE">Privé</option>
            </select>
          </div>
        )}
      </div>

      {/* Actions de grâce view */}
      {viewMode === 'grace' && (
        <div className="space-y-4 mb-6">
          {allPrayers.filter(p => p.statut === 'EXAUCEE').length > 0 ? (
            <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
              {allPrayers.filter(p => p.statut === 'EXAUCEE').map((prayer) => (
                <div key={prayer.id} className="card p-5 border-l-4 border-green-400 animate-slide-up">
                  <div className="flex items-center gap-2 mb-2">
                    <CheckCircle2 className="w-5 h-5 text-green-500" />
                    <span className="text-sm font-semibold text-green-700 dark:text-green-400">Exaucé !</span>
                  </div>
                  <p className="text-sm font-medium text-gray-900 dark:text-gray-100 mb-1">{prayer.titre}</p>
                  <span className="inline-flex items-center px-2 py-0.5 rounded-full text-[10px] font-medium bg-gray-100 text-gray-600 dark:bg-gray-700 dark:text-gray-400">
                    {categorieLabel(prayer.categorie)}
                  </span>
                  {prayer.temoignage && (
                    <div className="mt-3 p-3 rounded-lg bg-green-50 dark:bg-green-900/10">
                      <p className="text-xs text-green-700 dark:text-green-300 italic">"{prayer.temoignage}"</p>
                    </div>
                  )}
                </div>
              ))}
            </div>
          ) : (
            <div className="card p-10 text-center">
              <CheckCircle2 className="w-12 h-12 text-gray-300 mx-auto mb-3" />
              <p className="text-gray-500">Aucune prière exaucée pour le moment</p>
              <p className="text-xs text-gray-400 mt-1">Marquez une prière comme exaucée pour voir les actions de grâce</p>
            </div>
          )}
        </div>
      )}

      {/* Regular list view */}
      {viewMode === 'list' && (
        <>
          <DataTable<Prayer>
            columns={columns}
            data={data?.content || []}
            isLoading={isLoading}
            emptyMessage="Aucun sujet de prière"
            emptyIcon={<Heart className="w-12 h-12 text-gray-300 dark:text-gray-600 mx-auto mb-4" />}
          />

          {data && data.totalPages > 1 && (
            <div className="flex items-center justify-between mt-4">
              <p className="text-sm text-gray-500">Page {data.number + 1} / {data.totalPages}</p>
              <div className="flex gap-2">
                <button onClick={() => setPage(p => Math.max(0, p - 1))} disabled={data.first} className="btn-secondary btn-sm">Précédent</button>
                <button onClick={() => setPage(p => p + 1)} disabled={data.last} className="btn-secondary btn-sm">Suivant</button>
              </div>
            </div>
          )}
        </>
      )}
    </div>
  );
}
