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

  return (
    <div className="page-container">
      <div className="page-header">
        <div>
          <h1 className="page-title">Prières</h1>
          <p className="text-gray-500 dark:text-gray-400 mt-1">
            Suivi des sujets de prière de la famille
          </p>
        </div>
        <div className="flex gap-2">
          <button onClick={() => setShowFilters(!showFilters)} className={`btn-secondary btn-sm ${showFilters ? 'bg-primary-50' : ''}`}>
            <Filter className="w-4 h-4" /> Filtres
          </button>
          <button onClick={() => setShowCreate(!showCreate)} className="btn-primary btn-sm">
            <Plus className="w-4 h-4" /> Nouveau sujet
          </button>
        </div>
      </div>

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
    </div>
  );
}
