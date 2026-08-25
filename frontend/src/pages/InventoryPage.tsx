import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useI18n } from '@/i18n';
import api, { getErrorMessage } from '@/lib/api';
import SkeletonLoader from '@/components/shared/SkeletonLoader';
import EmptyState from '@/components/shared/EmptyState';
import toast from 'react-hot-toast';
import { Package, Plus, Search, Edit2, Trash2, AlertTriangle, BarChart3 } from 'lucide-react';

interface InventoryItem {
  id: string;
  name: string;
  category: string;
  quantity: number;
  unit: string;
  location: string;
  lastUpdated: string;
  lowStock: boolean;
}

interface InventoryStats {
  totalItems: number;
  lowStockCount: number;
  lowStockItems: InventoryItem[];
}


export default function InventoryPage() {
  const { t } = useI18n();
  const queryClient = useQueryClient();
  const [search, setSearch] = useState('');
  const [catFilter, setCatFilter] = useState<string>('all');

  const { data: itemsPage, isLoading, error } = useQuery({
    queryKey: ['inventory', { search, catFilter }],
    queryFn: async () => {
            const res = await api.get('/inventory', { params: { categorie: catFilter !== 'all' ? catFilter : undefined, q: search || undefined, page: 0, size: 50 } });
      const page = res.data as { content?: Array<Record<string, unknown>> };
      const content = page.content ?? [];
      return content.map((i) => ({
        id: String(i.id),
        name: String(i.nom ?? ''),
        category: String(i.categorie ?? ''),
        quantity: Number(i.quantiteDisponible ?? i.quantite ?? 0),
        unit: 'unités',
        location: String(i.lieuStockage ?? ''),
        lastUpdated: i.updatedAt as string | undefined,
        lowStock: Number(i.quantiteDisponible ?? i.quantite ?? 1) <= 0 || String(i.statut) === 'EN_MAINTENANCE',
      })) as InventoryItem[];
    },
    retry: false,
  });

  const items = itemsPage ?? [];
  const categories = ['all', ...new Set(items.map((i) => i.category))];
  const filtered = items; /* search/filter already on server side */
  const lowStock = items.filter((i) => i.lowStock);

  const deleteMutation = useMutation({
    mutationFn: async (id: string) => { await api.delete(`/inventory/${id}`); },
    onSuccess: () => { toast.success('Article supprimé'); queryClient.invalidateQueries({ queryKey: ['inventory'] }); },
    onError: () => toast.error('Erreur suppression'),
  });

  if (isLoading) return <SkeletonLoader lines={6} variant="card" />;
  if (error) return <div className="text-red-500 p-6">{getErrorMessage(error)}</div>;
  if (!items || items.length === 0) return <EmptyState title="Aucun article" message="L'inventaire est vide pour le moment." />;

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-gray-900 dark:text-white flex items-center gap-3">
            <Package className="w-7 h-7 text-indigo-500" />
            {t('nav.inventory') ?? 'Inventaire intelligent'}
          </h1>
          <p className="text-sm text-gray-500 mt-1">Gestion des ressources matérielles de l'église</p>
        </div>
        <button className="flex items-center gap-2 px-4 py-2 rounded-xl bg-indigo-500 text-white font-medium text-sm hover:bg-indigo-600 transition">
          <Plus className="w-4 h-4" /> Ajouter un article
        </button>
      </div>

      {/* Stats */}
      <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
        <div className="glass rounded-2xl p-5 border border-white/20 dark:border-white/[0.06]">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-xl bg-indigo-100 dark:bg-indigo-900/30 flex items-center justify-center">
              <Package className="w-5 h-5 text-indigo-500" />
            </div>
                        <div><p className="text-2xl font-bold text-gray-900 dark:text-white">{items.length}</p><p className="text-xs text-gray-500">Articles total</p></div>
          </div>
        </div>
        <div className="glass rounded-2xl p-5 border border-white/20 dark:border-white/[0.06]">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-xl bg-green-100 dark:bg-green-900/30 flex items-center justify-center">
              <BarChart3 className="w-5 h-5 text-green-500" />
            </div>
                        <div><p className="text-2xl font-bold text-gray-900 dark:text-white">{items.length - lowStock.length}</p><p className="text-xs text-gray-500">En stock suffisant</p></div>
          </div>
        </div>
        <div className="glass rounded-2xl p-5 border border-white/20 dark:border-white/[0.06]">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-xl bg-amber-100 dark:bg-amber-900/30 flex items-center justify-center">
              <AlertTriangle className="w-5 h-5 text-amber-500" />
            </div>
            <div><p className="text-2xl font-bold text-gray-900 dark:text-white">{lowStock.length}</p><p className="text-xs text-gray-500">Stock bas</p></div>
          </div>
        </div>
      </div>

      {/* Low stock alerts */}
      {lowStock.length > 0 && (
        <div className="bg-amber-50 dark:bg-amber-900/10 border border-amber-200 dark:border-amber-800/30 rounded-2xl p-4">
          <h3 className="text-sm font-semibold text-amber-700 dark:text-amber-300 flex items-center gap-2">
            <AlertTriangle className="w-4 h-4" /> Alertes de stock bas
          </h3>
          <div className="mt-2 space-y-1">
            {lowStock.map((item) => (
              <p key={item.id} className="text-xs text-amber-600 dark:text-amber-400">
                                {item.name}: {item.quantity} {item.unit} — rupture de stock
              </p>
            ))}
          </div>
        </div>
      )}

      {/* Search & filter */}
      <div className="flex gap-3">
        <div className="flex-1 relative">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400" />
          <input value={search} onChange={(e) => setSearch(e.target.value)}
            placeholder="Rechercher un article..."
            className="w-full pl-10 pr-4 py-2.5 rounded-xl border border-gray-200 dark:border-white/10 bg-white dark:bg-white/5 text-sm" />
        </div>
        <select value={catFilter} onChange={(e) => setCatFilter(e.target.value)}
          className="px-4 py-2.5 rounded-xl border border-gray-200 dark:border-white/10 bg-white dark:bg-white/5 text-sm">
          {categories.map((c) => <option key={c} value={c}>{c === 'all' ? 'Toutes catégories' : c}</option>)}
        </select>
      </div>

      {/* Items table */}
      <div className="glass rounded-2xl border border-white/20 dark:border-white/[0.06] overflow-hidden">
        <table className="w-full">
          <thead>
            <tr className="border-b border-gray-100 dark:border-white/[0.06]">
              <th className="text-left px-4 py-3 text-xs font-medium text-gray-500">Article</th>
              <th className="text-left px-4 py-3 text-xs font-medium text-gray-500">Catégorie</th>
              <th className="text-left px-4 py-3 text-xs font-medium text-gray-500">Quantité</th>
              <th className="text-left px-4 py-3 text-xs font-medium text-gray-500">Emplacement</th>
              <th className="text-left px-4 py-3 text-xs font-medium text-gray-500">Actions</th>
            </tr>
          </thead>
          <tbody>
            {filtered.map((item) => (
              <tr key={item.id} className="border-b border-gray-50 dark:border-white/[0.03] hover:bg-white/5 transition">
                <td className="px-4 py-3 text-sm font-medium text-gray-900 dark:text-white">{item.name}</td>
                <td className="px-4 py-3">
                  <span className="px-2 py-1 rounded-lg text-xs bg-indigo-100 text-indigo-700 dark:bg-indigo-900/30 dark:text-indigo-300">{item.category}</span>
                </td>
                <td className="px-4 py-3">
                                    <span className={`text-sm font-medium ${item.lowStock ? 'text-red-500' : 'text-gray-900 dark:text-white'}`}>
                    {item.quantity} {item.unit}
                  </span>
                </td>
                <td className="px-4 py-3 text-sm text-gray-500">{item.location}</td>
                <td className="px-4 py-3">
                  <div className="flex gap-2">
                    <button className="p-1.5 rounded-lg hover:bg-white/10 text-gray-400 hover:text-gray-600 transition">
                      <Edit2 className="w-4 h-4" />
                    </button>
                                        <button onClick={() => deleteMutation.mutate(item.id)} className="p-1.5 rounded-lg hover:bg-white/10 text-gray-400 hover:text-red-500 transition">
                      <Trash2 className="w-4 h-4" />
                    </button>
                  </div>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}
