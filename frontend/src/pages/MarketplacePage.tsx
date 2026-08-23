import { useState } from 'react';
import { useI18n } from '@/i18n';
import { Store, Search, Tag, DollarSign, Plus, Heart, MessageCircle } from 'lucide-react';

interface Listing {
  id: string;
  title: string;
  description: string;
  type: 'offer' | 'request' | 'service' | 'free';
  category: string;
  priceCents?: number;
  seller: string;
  imageUrl?: string;
}

const MOCK_LISTINGS: Listing[] = [
  { id: '1', title: 'Clavier Yamaha PSR-E473', description: 'Clavier 61 touches avec fonction de batteries. Bon état, peu utilisé.', type: 'offer', category: 'Musique', priceCents: 45000, seller: 'Frère Samuel' },
  { id: '2', title: 'Cours de guitare gratuit', description: 'Je propose des cours de guitare débutant le samedi après-midi.', type: 'service', category: 'Formation', seller: 'Soeur Marie' },
  { id: '3', title: 'Recherche babysitter', description: 'Pour les cultes du dimanche, zone Bonamoussadi.', type: 'request', category: 'Services', seller: 'Sœur Priscilla' },
  { id: '4', title: 'Vestes d\'hiver', description: '5 vestes chaudes pour les frères dans le besoin. Gratuit!', type: 'free', category: 'Vêtements', seller: 'Diaconie' },
  { id: '5', title: 'Bible NBS étudiant', description: 'Bible Nouveaux Berculois Studium, reliée cuir.', type: 'offer', category: 'Livres', priceCents: 8000, seller: 'Librairie Chrétiene' },
];

export default function MarketplacePage() {
  const { t } = useI18n();
  const [search, setSearch] = useState('');
  const [catFilter, setCatFilter] = useState<string>('all');
  const categories = ['all', ...new Set(MOCK_LISTINGS.map((l) => l.category))];

  const filtered = MOCK_LISTINGS.filter((l) =>
    (catFilter === 'all' || l.category === catFilter) &&
    (l.title.toLowerCase().includes(search.toLowerCase()) || l.description.toLowerCase().includes(search.toLowerCase()))
  );

  const typeColor = (type: string) => {
    if (type === 'offer') return 'bg-blue-100 text-blue-700 dark:bg-blue-900/30 dark:text-blue-300';
    if (type === 'request') return 'bg-orange-100 text-orange-700 dark:bg-orange-900/30 dark:text-orange-300';
    if (type === 'service') return 'bg-green-100 text-green-700 dark:bg-green-900/30 dark:text-green-300';
    return 'bg-purple-100 text-purple-700 dark:bg-purple-900/30 dark:text-purple-300';
  };

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-gray-900 dark:text-white flex items-center gap-3">
            <Store className="w-7 h-7 text-emerald-500" />
            {t('nav.marketplace') ?? 'Marketplace communautaire'}
          </h1>
          <p className="text-sm text-gray-500 mt-1">Échangez entre membres de l'église</p>
        </div>
        <button className="flex items-center gap-2 px-4 py-2 rounded-xl bg-emerald-500 text-white font-medium text-sm hover:bg-emerald-600 transition">
          <Plus className="w-4 h-4" /> Publier une annonce
        </button>
      </div>

      {/* Search & filter */}
      <div className="flex gap-3">
        <div className="flex-1 relative">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400" />
          <input value={search} onChange={(e) => setSearch(e.target.value)}
            placeholder="Rechercher une annonce..."
            className="w-full pl-10 pr-4 py-2.5 rounded-xl border border-gray-200 dark:border-white/10 bg-white dark:bg-white/5 text-sm" />
        </div>
        <select value={catFilter} onChange={(e) => setCatFilter(e.target.value)}
          className="px-4 py-2.5 rounded-xl border border-gray-200 dark:border-white/10 bg-white dark:bg-white/5 text-sm">
          {categories.map((c) => <option key={c} value={c}>{c === 'all' ? 'Toutes catégories' : c}</option>)}
        </select>
      </div>

      {/* Listings grid */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
        {filtered.map((listing) => (
          <div key={listing.id} className="glass rounded-2xl overflow-hidden border border-white/20 dark:border-white/[0.06] hover:shadow-lg transition">
            <div className="h-36 bg-gradient-to-br from-emerald-500/10 to-teal-500/10 flex items-center justify-center">
              <Store className="w-10 h-10 text-emerald-400/30" />
            </div>
            <div className="p-4">
              <div className="flex items-start justify-between">
                <h3 className="font-semibold text-gray-900 dark:text-white text-sm">{listing.title}</h3>
                <span className={`px-2 py-0.5 rounded-lg text-[10px] font-bold uppercase ${typeColor(listing.type)}`}>
                  {listing.type}
                </span>
              </div>
              <p className="text-xs text-gray-500 mt-1 line-clamp-2">{listing.description}</p>
              <div className="flex items-center justify-between mt-3 pt-3 border-t border-gray-100 dark:border-white/[0.06]">
                <div className="flex items-center gap-2">
                  <span className="text-xs text-gray-400">{listing.seller}</span>
                  <span className="px-1.5 py-0.5 rounded bg-gray-100 dark:bg-white/5 text-[10px] text-gray-400">{listing.category}</span>
                </div>
                {listing.priceCents != null && (
                  <span className="text-sm font-bold text-emerald-500">{(listing.priceCents / 100).toLocaleString()} FCFA</span>
                )}
              </div>
              <div className="flex gap-2 mt-3">
                <button className="flex-1 flex items-center justify-center gap-1 px-3 py-2 rounded-xl bg-emerald-500 text-white text-xs font-medium hover:bg-emerald-600 transition">
                  <MessageCircle className="w-3 h-3" /> Contacter
                </button>
                <button className="p-2 rounded-xl border border-gray-200 dark:border-white/10 text-gray-400 hover:text-red-500 transition">
                  <Heart className="w-4 h-4" />
                </button>
              </div>
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}
