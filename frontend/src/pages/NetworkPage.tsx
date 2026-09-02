import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import api from '@/lib/api';
import {
  Globe, Users, Calendar, Search, Plus, Download, MapPin, ExternalLink,
  Loader2, BookOpen, Megaphone, Star, X, Check, Trash2, Filter
} from 'lucide-react';
import toast from 'react-hot-toast';

/* ====================================================================
 * DISCIPOLAT NETWORK — Réseau inter-églises
 * Tabs : Ressources | Événements | Annuaire
 * Règle : aucune donnée privée exposée sans autorisation explicite
 * ================================================================== */

type Tab = 'resources' | 'events' | 'directory';

interface NetworkResource {
  id: string;
  title: string;
  description?: string;
  category: string;
  resourceType: string;
  fileUrl?: string;
  downloads: number;
  sharedWithPublic: boolean;
  createdAt: string;
}

interface NetworkEvent {
  id: string;
  title: string;
  description?: string;
  eventType: string;
  location?: string;
  city?: string;
  country?: string;
  startsAt: string;
  endsAt?: string;
  maxParticipants?: number;
  currentParticipants: number;
  isVirtual: boolean;
  virtualLink?: string;
}

interface NetworkDirectoryEntry {
  id: string;
  churchName: string;
  city?: string;
  country?: string;
  denomination?: string;
  pastorName?: string;
  contactEmail?: string;
  website?: string;
  memberCount?: number;
  description?: string;
}

const CATEGORY_LABELS: Record<string, string> = {
  BEST_PRACTICE: 'Bonne pratique',
  TEMPLATE: 'Template',
  TRAINING: 'Formation',
  GUIDE: 'Guide',
  TOOL: 'Outil',
  DOCUMENT: 'Document',
  FORMATION: 'Formation',
};

const EVENT_TYPE_LABELS: Record<string, string> = {
  CONFERENCE: 'Conférence',
  WORKSHOP: 'Atelier',
  RETREAT: 'Retraite',
  MEETING: 'Réunion',
  TRAINING: 'Formation',
  OPEN_DAY: 'Portes ouvertes',
  PRAYER: 'Prière',
};

const CATEGORY_COLORS: Record<string, string> = {
  BEST_PRACTICE: 'bg-emerald-500/10 text-emerald-600 dark:text-emerald-400',
  TEMPLATE: 'bg-blue-500/10 text-blue-600 dark:text-blue-400',
  TRAINING: 'bg-purple-500/10 text-purple-600 dark:text-purple-400',
  GUIDE: 'bg-amber-500/10 text-amber-600 dark:text-amber-400',
  TOOL: 'bg-rose-500/10 text-rose-600 dark:text-rose-400',
  DOCUMENT: 'bg-gray-500/10 text-gray-600 dark:text-gray-400',
  FORMATION: 'bg-indigo-500/10 text-indigo-600 dark:text-indigo-400',
};

export default function NetworkPage() {
  const [activeTab, setActiveTab] = useState<Tab>('resources');
  const [searchQuery, setSearchQuery] = useState('');
  const [showCreateResource, setShowCreateResource] = useState(false);
  const [showCreateEvent, setShowCreateEvent] = useState(false);
  const [selectedCountry, setSelectedCountry] = useState('');
  const [resourceCategory, setResourceCategory] = useState('');
  const [eventType, setEventType] = useState('');
  const [myResources, setMyResources] = useState(false);
  const [myEvents, setMyEvents] = useState(false);
  const [showAllEvents, setShowAllEvents] = useState(false);
  const queryClient = useQueryClient();

  // ======================== QUERIES ========================

  const resourcesQuery = useQuery({
    queryKey: ['network', 'resources', searchQuery, resourceCategory, myResources],
    queryFn: async () => {
      if (searchQuery) {
        if (activeTab === 'directory') {
          return (await api.get<NetworkDirectoryEntry[]>('/network/directory/search', { params: { q: searchQuery } })).data as unknown as NetworkResource[];
        }
        return (await api.get<NetworkResource[]>('/network/resources/search', { params: { q: searchQuery } })).data;
      }
      if (myResources) {
        return (await api.get<NetworkResource[]>('/network/resources/mine')).data;
      }
      if (resourceCategory) {
        return (await api.get<NetworkResource[]>(`/network/resources/category/${resourceCategory}`)).data;
      }
      return (await api.get<NetworkResource[]>('/network/resources')).data;
    },
  });

  const eventsQuery = useQuery({
    queryKey: ['network', 'events', eventType, myEvents, showAllEvents],
    queryFn: async () => {
      if (eventType) {
        return (await api.get<NetworkEvent[]>(`/network/events/type/${eventType}`)).data;
      }
      if (myEvents) {
        return (await api.get<NetworkEvent[]>('/network/events/mine')).data;
      }
      if (showAllEvents) {
        return (await api.get<NetworkEvent[]>('/network/events/all')).data;
      }
      return (await api.get<NetworkEvent[]>('/network/events')).data;
    },
  });

  const directoryQuery = useQuery({
    queryKey: ['network', 'directory', selectedCountry],
    queryFn: async () => {
      if (selectedCountry) {
        return (await api.get<NetworkDirectoryEntry[]>(`/network/directory/country/${selectedCountry}`)).data;
      }
      return (await api.get<NetworkDirectoryEntry[]>('/network/directory')).data;
    },
  });

  const statsQuery = useQuery({
    queryKey: ['network', 'stats'],
    queryFn: async () => (await api.get<Record<string, number>>('/network/stats')).data,
  });

  // ======================== MUTATIONS ========================

  const downloadMutation = useMutation({
    mutationFn: async (resourceId: string) => {
      return (await api.post(`/network/resources/${resourceId}/download`)).data;
    },
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['network', 'resources'] }),
  });

  const joinEventMutation = useMutation({
    mutationFn: async (eventId: string) => {
      return (await api.post(`/network/events/${eventId}/join`)).data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['network', 'events'] });
      toast.success('Inscription confirmée !');
    },
    onError: () => toast.error('Impossible de s\'inscrire'),
  });

  const createResourceMutation = useMutation({
    mutationFn: async (data: Partial<NetworkResource>) => {
      return (await api.post('/network/resources', data)).data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['network', 'resources'] });
      setShowCreateResource(false);
      toast.success('Ressource créée et partagée !');
    },
  });

  const createEventMutation = useMutation({
    mutationFn: async (data: Partial<NetworkEvent>) => {
      return (await api.post('/network/events', data)).data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['network', 'events'] });
      setShowCreateEvent(false);
      toast.success('Événement créé et partagé !');
    },
  });

  const leaveEventMutation = useMutation({
    mutationFn: async (eventId: string) => {
      return (await api.post(`/network/events/${eventId}/leave`)).data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['network', 'events'] });
      toast.success('Désinscription effectuée');
    },
    onError: () => toast.error('Impossible de se désinscrire'),
  });

  const deleteResourceMutation = useMutation({
    mutationFn: async (resourceId: string) => {
      await api.delete(`/network/resources/${resourceId}`);
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['network', 'resources'] });
      toast.success('Ressource supprimée');
    },
    onError: () => toast.error('Impossible de supprimer la ressource'),
  });

  const deleteEventMutation = useMutation({
    mutationFn: async (eventId: string) => {
      await api.delete(`/network/events/${eventId}`);
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['network', 'events'] });
      toast.success('Événement supprimé');
    },
    onError: () => toast.error('Impossible de supprimer l\'événement'),
  });

  const myDirectoryQuery = useQuery({
    queryKey: ['network', 'directory', 'mine'],
    queryFn: async () => (await api.get<NetworkDirectoryEntry>('/network/directory/mine')).data,
  });

  const updateMyDirectoryMutation = useMutation({
    mutationFn: async (updates: Partial<NetworkDirectoryEntry>) => {
      return (await api.put<NetworkDirectoryEntry>('/network/directory/mine', updates)).data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['network', 'directory'] });
      toast.success('Profil annuaire mis à jour');
    },
  });

  const toggleListingMutation = useMutation({
    mutationFn: async (listed: boolean) => {
      return (await api.post<NetworkDirectoryEntry>('/network/directory/mine/listing', { listed })).data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['network', 'directory'] });
      toast.success('Visibilité de l\'annuaire mise à jour');
    },
  });

  // ======================== RENDER ========================

  const tabs: { key: Tab; label: string; icon: typeof Globe }[] = [
    { key: 'resources', label: 'Ressources', icon: BookOpen },
    { key: 'events', label: 'Événements', icon: Calendar },
    { key: 'directory', label: 'Annuaire', icon: Users },
  ];

  return (
    <div className="page-container">
      {/* Header */}
      <div className="page-header">
        <div className="p-3 rounded-xl bg-gradient-to-br from-blue-500 to-indigo-600 text-white shadow-lg">
          <Globe className="w-6 h-6" />
        </div>
        <div>
          <h1 className="page-title">Discipolat Network</h1>
          <p className="page-subtitle">Réseau inter-églises — ressources, événements & annuaire</p>
        </div>
      </div>

      {/* Stats */}
      {statsQuery.data && (
        <div className="grid grid-cols-2 md:grid-cols-4 gap-3 mb-6">
          {[
            { label: 'Ressources partagées', value: statsQuery.data.totalSharedResources ?? 0, icon: BookOpen, color: 'text-blue-500' },
            { label: 'Événements à venir', value: statsQuery.data.totalPublicEvents ?? 0, icon: Calendar, color: 'text-purple-500' },
            { label: 'Églises listées', value: statsQuery.data.totalListedChurches ?? 0, icon: Users, color: 'text-emerald-500' },
            { label: 'Mes ressources', value: statsQuery.data.myResources ?? 0, icon: Megaphone, color: 'text-amber-500' },
          ].map((stat) => (
            <div key={stat.label} className="glass-card p-4 flex items-center gap-3">
              <stat.icon className={`w-5 h-5 ${stat.color}`} />
              <div>
                <p className="text-xl font-bold text-gray-900 dark:text-gray-100">{stat.value}</p>
                <p className="text-xs text-gray-500 dark:text-gray-400">{stat.label}</p>
              </div>
            </div>
          ))}
        </div>
      )}

      {/* Tabs */}
      <div className="flex gap-2 mb-6 border-b border-gray-200 dark:border-gray-700 pb-2">
        {tabs.map((tab) => (
          <button
            key={tab.key}
            onClick={() => setActiveTab(tab.key)}
            className={`flex items-center gap-2 px-4 py-2 rounded-lg font-medium text-sm transition ${
              activeTab === tab.key
                ? 'bg-primary-500/10 text-primary-600 dark:text-primary-400 ring-1 ring-primary-500/30'
                : 'text-gray-500 hover:text-gray-700 dark:hover:text-gray-300'
            }`}
          >
            <tab.icon className="w-4 h-4" />
            {tab.label}
          </button>
        ))}
      </div>

      {/* Search bar */}
      {(activeTab === 'resources' || activeTab === 'directory') && (
        <div className="relative mb-4">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400" />
          <input
            type="text"
            placeholder={activeTab === 'resources' ? 'Rechercher des ressources...' : 'Rechercher une église...'}
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            className="w-full pl-10 pr-4 py-2.5 rounded-xl bg-white/5 border border-gray-200 dark:border-gray-700 text-gray-900 dark:text-gray-100 focus:ring-2 focus:ring-primary-500/40 outline-none"
          />
        </div>
      )}

      {/* ======================== RESSOURCES ======================== */}
      {activeTab === 'resources' && (
        <div>
          <div className="flex justify-between items-center mb-4">
            <h2 className="text-lg font-semibold text-gray-900 dark:text-gray-100">Ressources partagées</h2>
            <button onClick={() => setShowCreateResource(true)} className="btn-primary text-sm flex items-center gap-1">
              <Plus className="w-4 h-4" /> Partager
            </button>
          </div>

          <div className="flex flex-wrap items-center gap-2 mb-4">
            <div className="flex items-center gap-1">
              <Filter className="w-3 h-3 text-gray-400" />
              <select value={resourceCategory} onChange={(e) => setResourceCategory(e.target.value)} className="text-xs border border-gray-200 dark:border-gray-700 rounded-lg px-2 py-1 bg-white/5 text-gray-900 dark:text-gray-100">
                <option value="">Toutes les catégories</option>
                {Object.entries(CATEGORY_LABELS).map(([k, v]) => <option key={k} value={k}>{v}</option>)}
              </select>
            </div>
            <button
              onClick={() => setMyResources(!myResources)}
              className={`text-xs px-3 py-1.5 rounded-lg font-medium transition ${myResources ? 'bg-primary-500/10 text-primary-600 dark:text-primary-400 ring-1 ring-primary-500/30' : 'bg-white/5 text-gray-500 hover:text-gray-700 dark:hover:text-gray-300'}`}
            >
              Mes ressources
            </button>
          </div>

          {resourcesQuery.isLoading ? (
            <Loader2 className="w-6 h-6 animate-spin text-primary-500 mx-auto mt-12" />
          ) : (resourcesQuery.data ?? []).length === 0 ? (
            <div className="glass-card p-12 text-center">
              <BookOpen className="w-10 h-10 text-gray-300 dark:text-gray-600 mx-auto mb-4" />
              <p className="text-gray-500">Aucune ressource partagée. Soyez le premier à contribuer !</p>
            </div>
          ) : (
            <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-3">
              {(resourcesQuery.data ?? []).map((r) => (
                <div key={r.id} className="glass-card p-5 animate-slide-up">
                  <div className="flex items-start justify-between mb-2">
                    <h3 className="font-semibold text-gray-900 dark:text-gray-100 text-sm">{r.title}</h3>
                    <span className={`badge text-xs ${CATEGORY_COLORS[r.category] ?? 'badge-info'}`}>
                      {CATEGORY_LABELS[r.category] ?? r.category}
                    </span>
                  </div>
                  {r.description && (
                    <p className="text-xs text-gray-500 dark:text-gray-400 mb-3 line-clamp-2">{r.description}</p>
                  )}
                  <div className="flex items-center justify-between mt-3">
                    <span className="text-xs text-gray-400 flex items-center gap-1">
                      <Download className="w-3 h-3" /> {r.downloads} téléchargements
                    </span>
                    <div className="flex items-center gap-1">
                      <button
                        onClick={() => downloadMutation.mutate(r.id)}
                        className="btn-ghost text-xs px-2 py-1"
                      >
                        <Download className="w-3 h-3 mr-1" /> Télécharger
                      </button>
                      <button
                        onClick={() => { if (confirm('Supprimer cette ressource ?')) deleteResourceMutation.mutate(r.id); }}
                        className="btn-ghost text-xs px-2 py-1 text-red-500 hover:text-red-600"
                      >
                        <Trash2 className="w-3 h-3" />
                      </button>
                    </div>
                  </div>
                </div>
              ))}
            </div>
          )}

          {/* Create Resource Modal */}
          {showCreateResource && (
            <CreateResourceModal
              onClose={() => setShowCreateResource(false)}
              onSubmit={(data) => createResourceMutation.mutate(data)}
            />
          )}
        </div>
      )}

      {/* ======================== ÉVÉNEMENTS ======================== */}
      {activeTab === 'events' && (
        <div>
          <div className="flex justify-between items-center mb-4">
            <h2 className="text-lg font-semibold text-gray-900 dark:text-gray-100">Événements inter-églises</h2>
            <button onClick={() => setShowCreateEvent(true)} className="btn-primary text-sm flex items-center gap-1">
              <Plus className="w-4 h-4" /> Créer
            </button>
          </div>

          {eventsQuery.isLoading ? (
            <Loader2 className="w-6 h-6 animate-spin text-primary-500 mx-auto mt-12" />
          ) : (eventsQuery.data ?? []).length === 0 ? (
            <div className="glass-card p-12 text-center">
              <Calendar className="w-10 h-10 text-gray-300 dark:text-gray-600 mx-auto mb-4" />
              <p className="text-gray-500">Aucun événement à venir. Créez le premier !</p>
            </div>
          ) : (
            <div className="space-y-4">
              {(eventsQuery.data ?? []).map((e) => (
                <div key={e.id} className="glass-card p-5 animate-slide-up">
                  <div className="flex items-start justify-between">
                    <div className="flex-1">
                      <div className="flex items-center gap-2 mb-1">
                        <h3 className="font-semibold text-gray-900 dark:text-gray-100">{e.title}</h3>
                        <span className="badge badge-info text-xs">{EVENT_TYPE_LABELS[e.eventType] ?? e.eventType}</span>
                        {e.isVirtual && <span className="badge badge-success text-xs">Virtual</span>}
                      </div>
                      {e.description && (
                        <p className="text-sm text-gray-500 dark:text-gray-400 mb-2 line-clamp-2">{e.description}</p>
                      )}
                      <div className="flex items-center gap-4 text-xs text-gray-400">
                        {e.city && <span className="flex items-center gap-1"><MapPin className="w-3 h-3" /> {e.city}{e.country ? `, ${e.country}` : ''}</span>}
                        <span>📅 {new Date(e.startsAt).toLocaleDateString('fr-FR', { day: 'numeric', month: 'long', year: 'numeric' })}</span>
                        <span>👥 {e.currentParticipants}{e.maxParticipants ? ` / ${e.maxParticipants}` : ''}</span>
                      </div>
                    </div>
                    <button
                      onClick={() => joinEventMutation.mutate(e.id)}
                      disabled={joinEventMutation.isPending || (e.maxParticipants != null && e.currentParticipants >= e.maxParticipants)}
                      className="btn-primary text-sm flex items-center gap-1 ml-4"
                    >
                      <Check className="w-4 h-4" /> S'inscrire
                    </button>
                  </div>
                </div>
              ))}
            </div>
          )}

          {/* Create Event Modal */}
          {showCreateEvent && (
            <CreateEventModal
              onClose={() => setShowCreateEvent(false)}
              onSubmit={(data) => createEventMutation.mutate(data)}
            />
          )}
        </div>
      )}

      {/* ======================== RÉPERTOIRE ======================== */}
      {activeTab === 'directory' && (
        <div>
          <div className="flex justify-between items-center mb-4">
            <h2 className="text-lg font-semibold text-gray-900 dark:text-gray-100">Annuaire des églises</h2>
            <select
              value={selectedCountry}
              onChange={(e) => setSelectedCountry(e.target.value)}
              className="text-sm border border-gray-200 dark:border-gray-700 rounded-lg px-3 py-1.5 bg-white/5 text-gray-900 dark:text-gray-100"
            >
              <option value="">Tous les pays</option>
              <option value="Côte d'Ivoire">Côte d'Ivoire</option>
              <option value="France">France</option>
              <option value="Cameroun">Cameroun</option>
              <option value="Sénégal">Sénégal</option>
              <option value="Congo">Congo</option>
              <option value="Belgique">Belgique</option>
              <option value="Canada">Canada</option>
              <option value="Autre">Autre</option>
            </select>
          </div>

          {directoryQuery.isLoading ? (
            <Loader2 className="w-6 h-6 animate-spin text-primary-500 mx-auto mt-12" />
          ) : (directoryQuery.data ?? []).length === 0 ? (
            <div className="glass-card p-12 text-center">
              <Users className="w-10 h-10 text-gray-300 dark:text-gray-600 mx-auto mb-4" />
              <p className="text-gray-500">Aucune église dans l'annuaire. Invitez des partenaires !</p>
            </div>
          ) : (
            <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-3">
              {(directoryQuery.data ?? []).map((c) => (
                <div key={c.id} className="glass-card p-5 animate-slide-up">
                  <div className="flex items-start justify-between mb-2">
                    <h3 className="font-semibold text-gray-900 dark:text-gray-100">{c.churchName}</h3>
                    {c.denomination && <span className="badge text-xs">{c.denomination}</span>}
                  </div>
                  <div className="space-y-1 text-sm text-gray-500 dark:text-gray-400">
                    {c.city && <p className="flex items-center gap-1"><MapPin className="w-3 h-3" /> {c.city}{c.country ? `, ${c.country}` : ''}</p>}
                    {c.pastorName && <p>👤 {c.pastorName}</p>}
                    {c.memberCount && <p>👥 ~{c.memberCount} membres</p>}
                  </div>
                  {c.description && <p className="text-xs text-gray-400 mt-2 line-clamp-2">{c.description}</p>}
                  {c.website && (
                    <a href={c.website} target="_blank" rel="noopener noreferrer" className="text-xs text-primary-500 hover:underline flex items-center gap-1 mt-2">
                      <ExternalLink className="w-3 h-3" /> Site web
                    </a>
                  )}
                </div>
              ))}
            </div>
          )}
        </div>
      )}
    </div>
  );
}

/* ====================================================================
 * MODALS
 * ================================================================== */

function CreateResourceModal({ onClose, onSubmit }: { onClose: () => void; onSubmit: (data: Partial<NetworkResource>) => void }) {
  const [form, setForm] = useState({ title: '', description: '', category: 'BEST_PRACTICE', resourceType: 'GUIDE' });

  return (
    <div className="fixed inset-0 z-50 bg-black/40 flex items-center justify-center p-4" onClick={onClose}>
      <div className="glass-card p-6 w-full max-w-lg" onClick={(e) => e.stopPropagation()}>
        <div className="flex justify-between items-center mb-4">
          <h3 className="font-bold text-gray-900 dark:text-gray-100">Partager une ressource</h3>
          <button onClick={onClose}><X className="w-5 h-5 text-gray-400" /></button>
        </div>
        <div className="space-y-3">
          <input placeholder="Titre" value={form.title} onChange={(e) => setForm({ ...form, title: e.target.value })} className="w-full p-2.5 rounded-lg bg-white/5 border border-gray-200 dark:border-gray-700 text-gray-900 dark:text-gray-100" />
          <textarea placeholder="Description" value={form.description} onChange={(e) => setForm({ ...form, description: e.target.value })} className="w-full p-2.5 rounded-lg bg-white/5 border border-gray-200 dark:border-gray-700 text-gray-900 dark:text-gray-100 h-20" />
          <select value={form.category} onChange={(e) => setForm({ ...form, category: e.target.value })} className="w-full p-2.5 rounded-lg bg-white/5 border border-gray-200 dark:border-gray-700 text-gray-900 dark:text-gray-100">
            {Object.entries(CATEGORY_LABELS).map(([k, v]) => <option key={k} value={k}>{v}</option>)}
          </select>
          <select value={form.resourceType} onChange={(e) => setForm({ ...form, resourceType: e.target.value })} className="w-full p-2.5 rounded-lg bg-white/5 border border-gray-200 dark:border-gray-700 text-gray-900 dark:text-gray-100">
            <option value="GUIDE">Guide</option>
            <option value="TEMPLATE">Template</option>
            <option value="COURSE">Cours</option>
            <option value="DOCUMENT">Document</option>
            <option value="VIDEO">Vidéo</option>
            <option value="LINK">Lien</option>
          </select>
        </div>
        <div className="flex justify-end gap-2 mt-4">
          <button onClick={onClose} className="btn-ghost text-sm">Annuler</button>
          <button onClick={() => onSubmit(form)} disabled={!form.title} className="btn-primary text-sm">Partager</button>
        </div>
      </div>
    </div>
  );
}

function CreateEventModal({ onClose, onSubmit }: { onClose: () => void; onSubmit: (data: Partial<NetworkEvent>) => void }) {
  const [form, setForm] = useState({
    title: '', description: '', eventType: 'CONFERENCE', location: '', city: '', country: '',
    startsAt: '', endsAt: '', isVirtual: false, virtualLink: '', maxParticipants: ''
  });

  return (
    <div className="fixed inset-0 z-50 bg-black/40 flex items-center justify-center p-4" onClick={onClose}>
      <div className="glass-card p-6 w-full max-w-lg" onClick={(e) => e.stopPropagation()}>
        <div className="flex justify-between items-center mb-4">
          <h3 className="font-bold text-gray-900 dark:text-gray-100">Créer un événement</h3>
          <button onClick={onClose}><X className="w-5 h-5 text-gray-400" /></button>
        </div>
        <div className="space-y-3">
          <input placeholder="Titre" value={form.title} onChange={(e) => setForm({ ...form, title: e.target.value })} className="w-full p-2.5 rounded-lg bg-white/5 border border-gray-200 dark:border-gray-700 text-gray-900 dark:text-gray-100" />
          <textarea placeholder="Description" value={form.description} onChange={(e) => setForm({ ...form, description: e.target.value })} className="w-full p-2.5 rounded-lg bg-white/5 border border-gray-200 dark:border-gray-700 text-gray-900 dark:text-gray-100 h-20" />
          <select value={form.eventType} onChange={(e) => setForm({ ...form, eventType: e.target.value })} className="w-full p-2.5 rounded-lg bg-white/5 border border-gray-200 dark:border-gray-700 text-gray-900 dark:text-gray-100">
            {Object.entries(EVENT_TYPE_LABELS).map(([k, v]) => <option key={k} value={k}>{v}</option>)}
          </select>
          <input placeholder="Lieu" value={form.location} onChange={(e) => setForm({ ...form, location: e.target.value })} className="w-full p-2.5 rounded-lg bg-white/5 border border-gray-200 dark:border-gray-700 text-gray-900 dark:text-gray-100" />
          <div className="grid grid-cols-2 gap-3">
            <input placeholder="Ville" value={form.city} onChange={(e) => setForm({ ...form, city: e.target.value })} className="p-2.5 rounded-lg bg-white/5 border border-gray-200 dark:border-gray-700 text-gray-900 dark:text-gray-100" />
            <input placeholder="Pays" value={form.country} onChange={(e) => setForm({ ...form, country: e.target.value })} className="p-2.5 rounded-lg bg-white/5 border border-gray-200 dark:border-gray-700 text-gray-900 dark:text-gray-100" />
          </div>
          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="text-xs text-gray-500 mb-1 block">Début</label>
              <input type="datetime-local" value={form.startsAt} onChange={(e) => setForm({ ...form, startsAt: e.target.value })} className="w-full p-2.5 rounded-lg bg-white/5 border border-gray-200 dark:border-gray-700 text-gray-900 dark:text-gray-100" />
            </div>
            <div>
              <label className="text-xs text-gray-500 mb-1 block">Fin</label>
              <input type="datetime-local" value={form.endsAt} onChange={(e) => setForm({ ...form, endsAt: e.target.value })} className="w-full p-2.5 rounded-lg bg-white/5 border border-gray-200 dark:border-gray-700 text-gray-900 dark:text-gray-100" />
            </div>
          </div>
          <label className="flex items-center gap-2 text-sm text-gray-600 dark:text-gray-300">
            <input type="checkbox" checked={form.isVirtual} onChange={(e) => setForm({ ...form, isVirtual: e.target.checked })} className="rounded" />
            Événement virtuel
          </label>
          {form.isVirtual && (
            <input placeholder="Lien de connexion" value={form.virtualLink} onChange={(e) => setForm({ ...form, virtualLink: e.target.value })} className="w-full p-2.5 rounded-lg bg-white/5 border border-gray-200 dark:border-gray-700 text-gray-900 dark:text-gray-100" />
          )}
          <input type="number" placeholder="Nombre max de participants (optionnel)" value={form.maxParticipants} onChange={(e) => setForm({ ...form, maxParticipants: e.target.value })} className="w-full p-2.5 rounded-lg bg-white/5 border border-gray-200 dark:border-gray-700 text-gray-900 dark:text-gray-100" />
        </div>
        <div className="flex justify-end gap-2 mt-4">
          <button onClick={onClose} className="btn-ghost text-sm">Annuler</button>
          <button onClick={() => onSubmit({ ...form, startsAt: form.startsAt ? new Date(form.startsAt).toISOString() : undefined, endsAt: form.endsAt ? new Date(form.endsAt).toISOString() : undefined, maxParticipants: form.maxParticipants ? parseInt(form.maxParticipants) : undefined })} disabled={!form.title || !form.startsAt} className="btn-primary text-sm">Créer</button>
        </div>
      </div>
    </div>
  );
}
