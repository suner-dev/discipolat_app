import { useState } from 'react';
import { useI18n } from '@/i18n';
import { Package, Plus, Search, Edit2, Trash2, AlertTriangle, BarChart3 } from 'lucide-react';

interface InventoryItem {
  id: string;
  name: string;
  category: string;
  quantity: number;
  minQuantity: number;
  unit: string;
  location: string;
  lastUpdated: string;
}

const MOCK_ITEMS: InventoryItem[] = [
  { id: '1', name: 'Chaises pliantes', category: 'Mobilier', quantity: 200, minQuantity: 50, unit: 'unités', location: 'Salle principale', lastUpdated: '2026-08-20' },
  { id: '2', name: 'Bibles', category: 'Literature', quantity: 45, minQuantity: 30, unit: 'exemplaires', location: 'Bibliothèque', lastUpdated: '2026-08-18' },
  { id: '3', name: 'Microphones sans fil', category: 'Audio/Video', quantity: 8, minQuantity: 4, unit: 'unités', location: 'Réserve technique', lastUpdated: '2026-08-22' },
  { id: '4', name: 'Projecteur HD', category: 'Audio/Video', quantity: 2, minQuantity: 1, unit: 'unités', location: 'Salle principale', lastUpdated: '2026-08-15' },
  { id: '5', name: 'Nappes blanches', category: 'Événementiel', quantity: 15, minQuantity: 10, unit: 'unités', location: 'Réserve', lastUpdated: '2026-08-10' },
];

export default function InventoryPage() {
  const { t } = useI18n();
  const [search, setSearch] = useState('');
  const [catFilter, setCatFilter] = useState<string>('all');

  const categories = ['all', ...new Set(MOCK_ITEMS.map((i) => i.category))];
  const filtered = MOCK_ITEMS.filter((i) =>
    (catFilter === 'all' || i.category === catFilter) &&
    i.name.toLowerCase().includes(search.toLowerCase())
  );

  const lowStock = MOCK_ITEMS.filter((i) => i.quantity <= i.minQuantity);

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
            <div><p className="text-2xl font-bold text-gray-900 dark:text-white">{MOCK_ITEMS.length}</p><p className="text-xs text-gray-500">Articles total</p></div>
          </div>
        </div>
        <div className="glass rounded-2xl p-5 border border-white/20 dark:border-white/[0.06]">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-xl bg-green-100 dark:bg-green-900/30 flex items-center justify-center">
              <BarChart3 className="w-5 h-5 text-green-500" />
            </div>
            <div><p className="text-2xl font-bold text-gray-900 dark:text-white">{MOCK_ITEMS.length - lowStock.length}</p><p className="text-xs text-gray-500">En stock suffisant</p></div>
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
                {item.name}: {item.quantity} {item.unit} (minimum: {item.minQuantity})
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
                  <span className={`text-sm font-medium ${item.quantity <= item.minQuantity ? 'text-red-500' : 'text-gray-900 dark:text-white'}`}>
                    {item.quantity} {item.unit}
                  </span>
                </td>
                <td className="px-4 py-3 text-sm text-gray-500">{item.location}</td>
                <td className="px-4 py-3">
                  <div className="flex gap-2">
                    <button className="p-1.5 rounded-lg hover:bg-white/10 text-gray-400 hover:text-gray-600 transition">
                      <Edit2 className="w-4 h-4" />
                    </button>
                    <button className="p-1.5 rounded-lg hover:bg-white/10 text-gray-400 hover:text-red-500 transition">
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
