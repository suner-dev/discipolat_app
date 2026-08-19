import { useMemo, useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import api, { getErrorMessage } from '@/lib/api';
import { useAuth } from '@/contexts/AuthContext';
import DataTable from '@/components/shared/DataTable';
import { useDictionaries } from '@/hooks/useDictionaries';
import type { FileEntity, PageResponse, CategorieDocument } from '@/types';
import type { ColumnDef } from '@/types/table';
import {
  FolderOpen, Plus, Search, Filter, FileText, Image, BookOpen,
  Download, Trash2, Loader2, X, Upload, Sparkles, Link2, BarChart3,
} from 'lucide-react';
import toast from 'react-hot-toast';

/** Replis (dictionnaires indisponibles) — les valeurs réelles viennent de la base. */
const CATEGORIE_FALLBACK: Record<string, string> = {
  COMPTE_RENDU: 'Compte rendu',
  FORMATION: 'Formation',
  PHOTO: 'Photo',
  RESOURCES: 'Ressources',
  AUTRE: 'Autre',
};

function formatFileSize(bytes: number): string {
  if (bytes < 1024) return `${bytes} o`;
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} Ko`;
  return `${(bytes / (1024 * 1024)).toFixed(1)} Mo`;
}

const INITIAL_FORM = { nom: '', description: '', chemin: '', typeFichier: 'application/pdf', taille: 0, categorie: 'AUTRE' as CategorieDocument };

export default function DocumentsPage() {
  const { user } = useAuth();
  const queryClient = useQueryClient();
  const dictionaries = useDictionaries();
  const [page, setPage] = useState(0);
  const [search, setSearch] = useState('');
  const [catFilter, setCatFilter] = useState<CategorieDocument | ''>('');
  const [showFilters, setShowFilters] = useState(false);
  const [showCreate, setShowCreate] = useState(false);
  const [newFile, setNewFile] = useState(INITIAL_FORM);

  const categorieEntries = useMemo(() => {
    const configured = dictionaries.options('DOCUMENT_CATEGORIE');
    return configured.length > 0
      ? configured.map((e) => ({ code: e.code, label: e.label, color: e.color }))
      : Object.entries(CATEGORIE_FALLBACK).map(([code, label]) => ({ code, label }));
  }, [dictionaries]);

  const categorieLabel = (code: string) =>
    dictionaries.label('DOCUMENT_CATEGORIE', code) || CATEGORIE_FALLBACK[code] || code;
  const categorieColor = (code: string) => dictionaries.color('DOCUMENT_CATEGORIE', code);

  const { data, isLoading } = useQuery({
    queryKey: ['files', page, search, catFilter],
    queryFn: async () => {
      const params = new URLSearchParams({ size: '20', page: String(page) });
      if (search) params.set('search', search);
      if (catFilter) params.set('categorie', catFilter);
      const res = await api.get(`/files?${params}`);
      return res.data as PageResponse<FileEntity>;
    },
  });

  // Compute stats from loaded data
  const allFiles = data?.content || [];
  const docStats = useMemo(() => ({
    total: data?.totalElements ?? allFiles.length,
    totalSize: allFiles.reduce((sum, f) => sum + (f.taille || 0), 0),
    categories: allFiles.reduce((acc, f) => { acc[f.categorie] = (acc[f.categorie] || 0) + 1; return acc; }, {} as Record<string, number>),
  }), [data, allFiles]);

  // Export CSV
  const exportCsv = () => {
    const rows = [['Nom', 'Description', 'Catégorie', 'Taille', 'Date']];
    allFiles.forEach((f) => {
      rows.push([
        f.nom,
        f.description || '',
        categorieLabel(f.categorie),
        formatFileSize(f.taille),
        new Date(f.dateCreation).toLocaleDateString('fr-FR'),
      ]);
    });
    const csv = rows.map(r => r.map(c => `"${c}"`).join(',')).join('\n');
    const blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `documents_${new Date().toISOString().slice(0, 10)}.csv`;
    a.click();
    URL.revokeObjectURL(url);
  };

  const createMutation = useMutation({
    mutationFn: async (file: typeof newFile) => {
      await api.post('/files', file);
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['files'] });
      toast.success('Document enregistré avec succès');
      setShowCreate(false);
      setNewFile(INITIAL_FORM);
    },
    onError: (err) => toast.error(getErrorMessage(err)),
  });

  const deleteMutation = useMutation({
    mutationFn: async (id: string) => {
      await api.delete(`/files/${id}`);
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['files'] });
      toast.success('Document supprimé');
    },
    onError: (err) => toast.error(getErrorMessage(err)),
  });

  const columns: ColumnDef<FileEntity>[] = [
    {
      header: 'Document',
      cell: (file) => {
        const Icon = FileText;
        return (
          <div className="flex items-center gap-3">
            <div
              className="p-2 rounded-xl bg-gray-100 text-gray-600 dark:bg-gray-800 dark:text-gray-300"
              style={categorieColor(file.categorie) ? { backgroundColor: `${categorieColor(file.categorie)}22`, color: categorieColor(file.categorie) } : undefined}
            >
              <Icon className="w-4 h-4" />
            </div>
            <div>
              <p className="text-sm font-medium text-gray-900 dark:text-gray-100">{file.nom}</p>
              {file.description && <p className="text-xs text-gray-500 truncate max-w-[200px]">{file.description}</p>}
            </div>
          </div>
        );
      },
    },
    {
      header: 'Catégorie',
      cell: (file) => (
        <span
          className="inline-flex items-center px-2.5 py-1 rounded-full text-xs font-medium border border-gray-200/60 dark:border-gray-700/60 bg-gray-100 text-gray-800 dark:bg-gray-700 dark:text-gray-300"
          style={categorieColor(file.categorie) ? { backgroundColor: `${categorieColor(file.categorie)}22`, color: categorieColor(file.categorie) } : undefined}
        >
          {categorieLabel(file.categorie)}
        </span>
      ),
    },
    {
      header: 'Taille',
      cell: (file) => <span className="text-sm text-gray-500 font-mono">{formatFileSize(file.taille)}</span>,
    },
    {
      header: 'Ajouté le',
      cell: (file) => (
        <span className="text-sm text-gray-500">
          {new Date(file.dateCreation).toLocaleDateString('fr-FR', { day: 'numeric', month: 'short', year: 'numeric' })}
        </span>
      ),
    },
    {
      header: 'Actions',
      cell: (file) => (
        <div className="flex items-center gap-1">
          <a href={file.chemin} target="_blank" rel="noopener noreferrer"
            className="btn-ghost btn-sm text-primary-600 hover:text-primary-700">
            <Download className="w-4 h-4" />
          </a>
          {(file.auteurId === user?.id || user?.role === 'PASTEUR') && (
            <button onClick={() => { if (confirm('Supprimer ce document ?')) deleteMutation.mutate(file.id); }}
              disabled={deleteMutation.isPending} className="btn-ghost btn-sm text-red-500 hover:text-red-600">
              <Trash2 className="w-4 h-4" />
            </button>
          )}
        </div>
      ),
    },
  ];

  return (
    <div className="page-container">
      <div className="page-header">
        <div className="animate-fade-in">
          <div className="flex items-center gap-2 mb-1">
            <FolderOpen className="w-5 h-5 text-primary-500" />
            <h1 className="page-title">Documents</h1>
          </div>
          <p className="page-subtitle">Gestion documentaire de la famille</p>
        </div>
        <div className="flex gap-2 animate-fade-in">
          <button onClick={exportCsv} className="btn-secondary btn-sm">
            <Download className="w-4 h-4" /> Export
          </button>
          <button onClick={() => setShowFilters(!showFilters)}
            className={`btn-secondary btn-sm ${showFilters ? 'bg-primary-50 dark:bg-primary-900/20' : ''}`}>
            <Filter className="w-4 h-4" /> Filtres
          </button>
          <button onClick={() => setShowCreate(!showCreate)} className="btn-primary btn-sm">
            <Plus className="w-4 h-4" /> Ajouter
          </button>
        </div>
      </div>

      {/* Stats cards */}
      <div className="grid grid-cols-2 sm:grid-cols-4 gap-3 mb-6">
        {[
          { label: 'Total documents', value: docStats.total, color: 'from-primary-500 to-primary-600' },
          { label: 'Taille totale', value: formatFileSize(docStats.totalSize), color: 'from-emerald-500 to-green-500' },
          { label: 'Catégories', value: Object.keys(docStats.categories).length, color: 'from-violet-500 to-purple-500' },
          { label: 'Ce mois', value: allFiles.filter(f => { const d = new Date(f.dateCreation); const now = new Date(); return d.getMonth() === now.getMonth() && d.getFullYear() === now.getFullYear(); }).length, color: 'from-amber-500 to-orange-500' },
        ].map((s, i) => (
          <div key={s.label} className="stat-card animate-slide-up" style={{ animationDelay: `${i * 50}ms` }}>
            <div className={`absolute top-0 left-0 right-0 h-0.5 bg-gradient-to-r ${s.color} opacity-60`} />
            <span className="stat-label text-[10px]">{s.label}</span>
            <p className="stat-value text-xl">{s.value}</p>
          </div>
        ))}
      </div>

      {/* Category distribution */}
      {Object.keys(docStats.categories).length > 0 && (
        <div className="card p-4 mb-6">
          <div className="flex items-center gap-2 mb-3">
            <BarChart3 className="w-4 h-4 text-gray-400" />
            <p className="text-xs font-semibold text-gray-500 uppercase">Répartition par catégorie</p>
          </div>
          <div className="flex flex-wrap gap-3">
            {Object.entries(docStats.categories).sort((a, b) => b[1] - a[1]).map(([cat, count]) => {
              const pct = docStats.total > 0 ? Math.round((count / docStats.total) * 100) : 0;
              return (
                <button key={cat} onClick={() => { setCatFilter(catFilter === cat ? '' : cat as CategorieDocument); setPage(0); }}
                  className={`flex items-center gap-2 px-3 py-2 rounded-xl text-xs font-medium transition-all ${catFilter === cat ? 'ring-2 ring-primary-500/50 shadow-sm' : ''} bg-gray-50 dark:bg-gray-800/50 hover:bg-gray-100 dark:hover:bg-gray-700/50`}>
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
      {showCreate && (
        <div className="glass-card p-6 mb-6 animate-slide-up">
          <div className="flex items-center justify-between mb-5">
            <div className="flex items-center gap-3">
              <div className="p-2.5 rounded-xl bg-gradient-to-br from-primary-500 to-primary-600 text-white">
                <Upload className="w-5 h-5" />
              </div>
              <div>
                <h3 className="text-lg font-semibold text-gray-900 dark:text-gray-100">Nouveau document</h3>
                <p className="text-xs text-gray-500">Ajoutez un lien vers un document</p>
              </div>
            </div>
            <button onClick={() => setShowCreate(false)} className="p-1.5 rounded-lg hover:bg-gray-100 dark:hover:bg-gray-800">
              <X className="w-5 h-5 text-gray-400" />
            </button>
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div className="sm:col-span-2">
              <label className="label">Nom du document *</label>
              <input className="input" value={newFile.nom} onChange={(e) => setNewFile({ ...newFile, nom: e.target.value })} placeholder="Ex: Compte rendu réunion Mars" />
            </div>
            <div className="sm:col-span-2">
              <label className="label">Description</label>
              <textarea className="input" rows={2} value={newFile.description} onChange={(e) => setNewFile({ ...newFile, description: e.target.value })} placeholder="Brève description..." />
            </div>
            <div className="sm:col-span-2">
              <label className="label">URL / chemin du fichier *</label>
              <div className="relative">
                <Link2 className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400" />
                <input className="input pl-10" value={newFile.chemin} onChange={(e) => setNewFile({ ...newFile, chemin: e.target.value })} placeholder="https://drive.google.com/..." />
              </div>
            </div>
            <div>
              <label className="label">Catégorie</label>
              <select className="input" value={newFile.categorie} onChange={(e) => setNewFile({ ...newFile, categorie: e.target.value as CategorieDocument })}>
                {categorieEntries.map((o) => (<option key={o.code} value={o.code}>{o.label}</option>))}
              </select>
            </div>
            <div>
              <label className="label">Type MIME</label>
              <input className="input" value={newFile.typeFichier} onChange={(e) => setNewFile({ ...newFile, typeFichier: e.target.value })} />
            </div>
            <div>
              <label className="label">Taille (octets)</label>
              <input type="number" min={0} className="input" value={newFile.taille || ''} onChange={(e) => setNewFile({ ...newFile, taille: parseInt(e.target.value) || 0 })} />
            </div>
          </div>

          <div className="flex justify-end gap-3 mt-5 pt-4 border-t border-white/20 dark:border-white/[0.06]">
            <button onClick={() => setShowCreate(false)} className="btn-secondary btn-sm">Annuler</button>
            <button onClick={() => createMutation.mutate(newFile)} disabled={!newFile.nom || !newFile.chemin || createMutation.isPending} className="btn-primary btn-sm">
              {createMutation.isPending ? <Loader2 className="w-4 h-4 animate-spin" /> : <Upload className="w-4 h-4" />}
              Enregistrer
            </button>
          </div>
        </div>
      )}

      {/* Search & filters */}
      <div className="glass-card p-4 mb-6 animate-slide-up">
        <div className="flex flex-col sm:flex-row gap-3">
          <div className="flex-1 relative">
            <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400" />
            <input type="text" placeholder="Rechercher un document..." value={search}
              onChange={(e) => { setSearch(e.target.value); setPage(0); }} className="input pl-10" />
          </div>
        </div>
        {showFilters && (
          <div className="flex flex-wrap gap-3 mt-3 pt-3 border-t border-white/20 dark:border-white/[0.06] animate-slide-up">
            <select value={catFilter} onChange={(e) => { setCatFilter(e.target.value as CategorieDocument | ''); setPage(0); }} className="input w-auto">
              <option value="">Toutes catégories</option>
              {categorieEntries.map((o) => (<option key={o.code} value={o.code}>{o.label}</option>))}
            </select>
          </div>
        )}
      </div>

      <DataTable<FileEntity>
        columns={columns}
        data={data?.content || []}
        isLoading={isLoading}
        emptyMessage="Aucun document"
        emptyIcon={<FolderOpen className="w-16 h-16 text-gray-300 dark:text-gray-600" />}
      />

      {data && data.totalPages > 1 && (
        <div className="flex items-center justify-between mt-4">
          <p className="text-sm text-gray-500">{data.number + 1} / {data.totalPages}</p>
          <div className="flex gap-2">
            <button onClick={() => setPage(p => Math.max(0, p - 1))} disabled={data.first} className="btn-secondary btn-sm">← Précédent</button>
            <button onClick={() => setPage(p => p + 1)} disabled={data.last} className="btn-primary btn-sm">Suivant →</button>
          </div>
        </div>
      )}
    </div>
  );
}
