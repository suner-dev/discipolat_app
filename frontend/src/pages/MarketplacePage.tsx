import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useI18n } from '@/i18n';
import toast from 'react-hot-toast';
import { Store, Search, Plus, Heart, MessageCircle, Filter, Trash2, Download, X, Loader2 } from 'lucide-react';
import api, { getErrorMessage } from '@/lib/api';
import { useAuth } from '@/contexts/AuthContext';
import SkeletonLoader from '@/components/shared/SkeletonLoader';
import EmptyState from '@/components/shared/EmptyState';

interface Listing {
  id: string;
  title: string;
  description: string;
  listingType: string;
  category: string | null;
  priceCents?: number;
  imageUrl?: string;
  sellerId: number;
  isActive: boolean;
  contactInfo?: string;
  createdAt?: string;
}

const LISTING_TYPE_LABEL: Record<string, string> = {
  OFFER: 'offer', REQUEST: 'request', SERVICE: 'service', FREE: 'free',
};

const CATEGORIES = ['SERVICE', 'BIEN', 'LOGIQUEL', 'AUTRE'];

export default function MarketplacePage() {
  const { t } = useI18n();
  const { activeRole } = useAuth();
  const queryClient = useQueryClient();
  const [search, setSearch] = useState('');
  const [catFilter, setCatFilter] = useState('all');
  const [showCreate, setShowCreate] = useState(false);
  const [showPublish, setShowPublish] = useState(false);
  const [newListing, setNewListing] = useState({ title: '', description: '', listingType: 'OFFER', category: '', priceCents: '', contactInfo: '' });
  const [newTemplate, setNewTemplate] = useState({ title: '', description: '', listingType: 'OFFER', category: 'TEMPLATE', contactInfo: '' });
  const [installId, setInstallId] = useState<string | null>(null);

  const invalidate = () => queryClient.invalidateQueries({ queryKey: ['marketplace'] });

  const { data: listings = [], isLoading, error } = useQuery({
    queryKey: ['marketplace', catFilter],
    queryFn: async () => {
      if (catFilter !== 'all') {
        return (await api.get(`/marketplace/category/${encodeURIComponent(catFilter)}`)).data as Listing[];
      }
      return (await api.get('/marketplace')).data as Listing[];
    },
    retry: false,
  });

  const createMutation = useMutation({
    mutationFn: async () =>
      (
        await api.post('/marketplace', {
          title: newListing.title,
          description: newListing.description || undefined,
          listingType: newListing.listingType,
          category: newListing.category || null,
          priceCents: newListing.priceCents ? Number(newListing.priceCents) * 100 : undefined,
          contactInfo: newListing.contactInfo || undefined,
        })
      ).data,
    onSuccess: () => {
      toast.success('Annonce publiée');
      setShowCreate(false);
      setNewListing({ title: '', description: '', listingType: 'OFFER', category: '', priceCents: '', contactInfo: '' });
      invalidate();
    },
    onError: (e) => toast.error(getErrorMessage(e)),
  });

  const publishTemplateMutation = useMutation({
    mutationFn: async () =>
      (
        await api.post('/marketplace/templates/publish', {
          title: newTemplate.title,
          description: newTemplate.description || undefined,
          listingType: newTemplate.listingType,
          category: newTemplate.category || 'TEMPLATE',
          contactInfo: newTemplate.contactInfo || undefined,
        })
      ).data,
    onSuccess: () => {
      toast.success('Template publié');
      setShowPublish(false);
      setNewTemplate({ title: '', description: '', listingType: 'OFFER', category: 'TEMPLATE', contactInfo: '' });
      invalidate();
    },
    onError: (e) => toast.error(getErrorMessage(e)),
  });

  const installMutation = useMutation({
    mutationFn: async (id: string) => {
      setInstallId(id);
      const res = await api.post(`/marketplace/${id}/install`);
      toast.success('Template installé');
      return res.data;
    },
    onSettled: () => setInstallId(null),
    onError: (e) => toast.error(getErrorMessage(e)),
  });

  const deleteMutation = useMutation({
    mutationFn: async (id: string) => api.delete(`/marketplace/${id}`),
    onSuccess: () => { invalidate(); toast.success('Annonce supprimée'); },
    onError: (e) => toast.error(getErrorMessage(e)),
  });

  const activeListings = listings.filter(l => l.isActive !== false);
  const categories = ['all', ...Array.from(new Set(activeListings.map(l => l.category).filter(Boolean) as string[]))];

  const filtered = activeListings.filter(l =>
    (l.title.toLowerCase().includes(search.toLowerCase()) ||
     (l.description || '').toLowerCase().includes(search.toLowerCase()))
  );

  const isTemplate = (l: Listing) => l.category === 'TEMPLATE';
  const isMine = (l: Listing) => activeRole === 'ADMIN' || activeRole === 'PASTEUR';

  const typeColor = (type: string) => {
    if (type === 'offer') return 'bg-blue-100 text-blue-700 dark:bg-blue-900/30 dark:text-blue-300';
    if (type === 'request') return 'bg-orange-100 text-orange-700 dark:bg-orange-900/30 dark:text-orange-300';
    if (type === 'service') return 'bg-green-100 text-green-700 dark:bg-green-900/30 dark:text-green-300';
    return 'bg-purple-100 text-purple-700 dark:bg-purple-900/30 dark:text-purple-300';
  };

  if (isLoading) return <SkeletonLoader lines={6} variant="card" />;
  if (error) return <div className="p-6 text-red-500 dark:text-red-400">{getErrorMessage(error)}</div>;

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
        <div className="flex gap-2">
          <button onClick={() => setShowPublish(!showPublish)} className="flex items-center gap-2 px-4 py-2 rounded-xl border border-emerald-500/40 text-emerald-600 dark:text-emerald-400 font-medium text-sm hover:bg-emerald-500/10 transition">
            <Download className="w-4 h-4" /> Publier un template
          </button>
          <button onClick={() => setShowCreate(!showCreate)} className="flex items-center gap-2 px-4 py-2 rounded-xl bg-emerald-500 text-white font-medium text-sm hover:bg-emerald-600 transition">
            <Plus className="w-4 h-4" /> Publier une annonce
          </button>
        </div>
      </div>

      {showCreate && (
        <form
          onSubmit={(e) => { e.preventDefault(); createMutation.mutate(); }}
          className="glass rounded-2xl border border-white/20 dark:border-white/[0.06] p-5 mb-6 grid gap-3 md:grid-cols-4 animate-slide-up"
        >
          <input required placeholder="Titre" className="px-4 py-2.5 rounded-xl border border-gray-200 dark:border-white/10 bg-white dark:bg-white/5 text-sm md:col-span-2"
            value={newListing.title} onChange={(e) => setNewListing({ ...newListing, title: e.target.value })} />
          <select className="px-4 py-2.5 rounded-xl border border-gray-200 dark:border-white/10 bg-white dark:bg-white/5 text-sm"
            value={newListing.listingType} onChange={(e) => setNewListing({ ...newListing, listingType: e.target.value })}>
            <option value="OFFER">Offre</option>
            <option value="REQUEST">Demande</option>
            <option value="SERVICE">Service</option>
            <option value="FREE">Don</option>
          </select>
          <select className="px-4 py-2.5 rounded-xl border border-gray-200 dark:border-white/10 bg-white dark:bg-white/5 text-sm"
            value={newListing.category} onChange={(e) => setNewListing({ ...newListing, category: e.target.value })}>
            <option value="">Toutes catégories</option>
            {CATEGORIES.filter(c => c !== 'TEMPLATE').map((c) => <option key={c} value={c}>{c}</option>)}
          </select>
          <input placeholder="Description" className="px-4 py-2.5 rounded-xl border border-gray-200 dark:border-white/10 bg-white dark:bg-white/5 text-sm md:col-span-2"
            value={newListing.description} onChange={(e) => setNewListing({ ...newListing, description: e.target.value })} />
          <input type="number" min="0" placeholder="Prix (FCFA)" className="px-4 py-2.5 rounded-xl border border-gray-200 dark:border-white/10 bg-white dark:bg-white/5 text-sm"
            value={newListing.priceCents} onChange={(e) => setNewListing({ ...newListing, priceCents: e.target.value })} />
          <input placeholder="Contact (téléphone / email)" className="px-4 py-2.5 rounded-xl border border-gray-200 dark:border-white/10 bg-white dark:bg-white/5 text-sm"
            value={newListing.contactInfo} onChange={(e) => setNewListing({ ...newListing, contactInfo: e.target.value })} />
          <button type="submit" disabled={createMutation.isPending} className="flex items-center justify-center gap-2 px-4 py-2.5 rounded-xl bg-emerald-500 text-white font-medium text-sm hover:bg-emerald-600 transition md:col-span-4">
            {createMutation.isPending ? <Loader2 className="w-4 h-4 animate-spin" /> : <Plus className="w-4 h-4" />} Publier
          </button>
        </form>
      )}

      {showPublish && (
        <form
          onSubmit={(e) => { e.preventDefault(); publishTemplateMutation.mutate(); }}
          className="glass rounded-2xl border border-white/20 dark:border-white/[0.06] p-5 mb-6 grid gap-3 md:grid-cols-3 animate-slide-up"
        >
          <input required placeholder="Titre du template" className="px-4 py-2.5 rounded-xl border border-gray-200 dark:border-white/10 bg-white dark:bg-white/5 text-sm md:col-span-2"
            value={newTemplate.title} onChange={(e) => setNewTemplate({ ...newTemplate, title: e.target.value })} />
          <select className="px-4 py-2.5 rounded-xl border border-gray-200 dark:border-white/10 bg-white dark:bg-white/5 text-sm"
            value={newTemplate.listingType} onChange={(e) => setNewTemplate({ ...newTemplate, listingType: e.target.value })}>
            <option value="OFFER">Offre</option>
            <option value="REQUEST">Demande</option>
            <option value="SERVICE">Service</option>
            <option value="FREE">Don</option>
          </select>
          <input placeholder="Description" className="px-4 py-2.5 rounded-xl border border-gray-200 dark:border-white/10 bg-white dark:bg-white/5 text-sm md:col-span-2"
            value={newTemplate.description} onChange={(e) => setNewTemplate({ ...newTemplate, description: e.target.value })} />
          <button type="submit" disabled={publishTemplateMutation.isPending} className="flex items-center justify-center gap-2 px-4 py-2.5 rounded-xl bg-emerald-500 text-white font-medium text-sm hover:bg-emerald-600 transition">
            {publishTemplateMutation.isPending ? <Loader2 className="w-4 h-4 animate-spin" /> : <Download className="w-4 h-4" />} Publier
          </button>
        </form>
      )}

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

      {filtered.length === 0 ? (
        <EmptyState
          icon={<Filter className="w-6 h-6 text-emerald-400" />}
          title="Aucune annonce"
          message="Publiez une annonce pour échanger avec les membres de l'église."
        />
      ) : (
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
          {filtered.map((listing) => {
            const type = LISTING_TYPE_LABEL[listing.listingType] || 'free';
            return (
              <div key={listing.id} className="glass rounded-2xl overflow-hidden border border-white/20 dark:border-white/[0.06] hover:shadow-lg transition">
                <div className="h-36 bg-gradient-to-br from-emerald-500/10 to-teal-500/10 flex items-center justify-center">
                  {listing.imageUrl ? (
                    <img src={listing.imageUrl} alt={listing.title} className="w-full h-full object-cover" />
                  ) : (
                    <Store className="w-10 h-10 text-emerald-400/30" />
                  )}
                </div>
                <div className="p-4">
                  <div className="flex items-start justify-between">
                    <h3 className="font-semibold text-gray-900 dark:text-white text-sm">{listing.title}</h3>
                    <span className={`px-2 py-0.5 rounded-lg text-[10px] font-bold uppercase ${typeColor(type)}`}>
                      {type}
                    </span>
                  </div>
                  <p className="text-xs text-gray-500 mt-1 line-clamp-2">{listing.description}</p>
                  <div className="flex items-center justify-between mt-3 pt-3 border-t border-gray-100 dark:border-white/[0.06]">
                    <div className="flex items-center gap-2">
                      <span className="text-xs text-gray-400">Vendeur #{listing.sellerId}</span>
                      {listing.category && (
                        <span className="px-1.5 py-0.5 rounded bg-gray-100 dark:bg-white/5 text-[10px] text-gray-400">{listing.category}</span>
                      )}
                    </div>
                    {listing.priceCents != null && (
                      <span className="text-sm font-bold text-emerald-500">{(listing.priceCents / 100).toLocaleString('fr-FR')} FCFA</span>
                    )}
                  </div>
                  <div className="flex gap-2 mt-3">
                    <a href={listing.contactInfo ? `mailto:${listing.contactInfo}` : '#'}
                      className="flex-1 flex items-center justify-center gap-1 px-3 py-2 rounded-xl bg-emerald-500 text-white text-xs font-medium hover:bg-emerald-600 transition">
                      <MessageCircle className="w-3 h-3" />
                      {listing.contactInfo || 'Contacter'}
                    </a>
                    <button className="p-2 rounded-xl border border-gray-200 dark:border-white/10 text-gray-400 hover:text-red-500 transition">
                      <Heart className="w-4 h-4" />
                    </button>
                  </div>
                </div>
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
}
