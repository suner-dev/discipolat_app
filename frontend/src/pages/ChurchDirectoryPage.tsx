import React, { useState, useEffect } from 'react';
import { useI18n } from '@/i18n';
import api from '@/lib/api';
import SkeletonLoader from '@/components/shared/SkeletonLoader';
import EmptyState from '@/components/shared/EmptyState';
import { UserCircle, Search, Mail, Phone, MapPin } from 'lucide-react';

interface DirectoryEntry {
  id: string;
  firstName: string;
  lastName: string;
  email: string;
  telephone?: string;
  role: string;
  department?: string;
  bio?: string;
  optIn: boolean;
  photoUrl?: string;
}

export default function ChurchDirectoryPage() {
  const { t } = useI18n();
  const [entries, setEntries] = useState<DirectoryEntry[]>([]);
  const [loading, setLoading] = useState(true);
  const [search, setSearch] = useState('');
  const [filterRole, setFilterRole] = useState('');

  useEffect(() => { loadDirectory(); }, []);

  const loadDirectory = async () => {
    try {
      setLoading(true);
      const res = await api.get('/directory');
      setEntries(res.data.content || res.data || []);
    } catch { setEntries([]); } finally { setLoading(false); }
  };

  const filtered = entries.filter(e => {
    const matchSearch = !search || `${e.firstName} ${e.lastName}`.toLowerCase().includes(search.toLowerCase());
    const matchRole = !filterRole || e.role === filterRole;
    return matchSearch && matchRole;
  });

  return (
    <div className="min-h-screen p-4 md:p-8 max-w-5xl mx-auto">
      <div className="mb-8">
        <h1 className="text-2xl md:text-3xl font-bold text-gray-900 dark:text-white flex items-center gap-3">
          <UserCircle className="w-8 h-8 text-teal-500" />
          Annuaire de l'Église
        </h1>
        <p className="text-gray-500 dark:text-gray-400 mt-1">Fiches opt-in des membres qui souhaitent se faire connaître</p>
      </div>

      <div className="flex gap-3 mb-6">
        <div className="relative flex-1">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400" />
          <input type="text" placeholder="Rechercher un membre..." value={search} onChange={e => setSearch(e.target.value)}
            className="w-full pl-10 pr-4 py-2.5 rounded-xl border border-gray-200 dark:border-white/10 bg-white dark:bg-white/5 text-gray-900 dark:text-white text-sm focus:outline-none focus:ring-2 focus:ring-teal-500" />
        </div>
        <select value={filterRole} onChange={e => setFilterRole(e.target.value)}
          className="px-4 py-2.5 rounded-xl border border-gray-200 dark:border-white/10 bg-white dark:bg-white/5 text-gray-900 dark:text-white text-sm focus:outline-none focus:ring-2 focus:ring-teal-500">
          <option value="">Tous les rôles</option>
          <option value="PASTEUR">Pasteur</option>
          <option value="RESPONSABLE">Responsable</option>
          <option value="FAISEUR">Faiseur</option>
          <option value="MEMBRE">Membre</option>
        </select>
      </div>

      {loading ? <SkeletonLoader lines={6} variant="card" /> : filtered.length === 0 ? (
        <EmptyState icon={<UserCircle className="w-8 h-8 text-gray-400" />} title="Aucun membre"
          message="Les membres n'ont pas encore activé leur fiche publique" />
      ) : (
        <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-3">
          {filtered.map(entry => (
            <div key={entry.id} className="p-5 rounded-2xl border border-gray-200 dark:border-white/10 bg-white dark:bg-white/5 text-center">
              <div className="w-16 h-16 mx-auto mb-3 rounded-full bg-gradient-to-br from-teal-400 to-emerald-500 flex items-center justify-center text-white text-xl font-bold">
                {entry.firstName?.[0]}{entry.lastName?.[0]}
              </div>
              <h3 className="font-semibold text-gray-900 dark:text-white">{entry.firstName} {entry.lastName}</h3>
              <span className="text-xs text-teal-600 dark:text-teal-400 font-medium">{entry.role}</span>
              {entry.department && <p className="text-xs text-gray-500 mt-1">{entry.department}</p>}
              {entry.bio && <p className="text-xs text-gray-400 mt-2 line-clamp-2 italic">"{entry.bio}"</p>}
              <div className="flex justify-center gap-3 mt-3">
                {entry.email && <a href={`mailto:${entry.email}`} className="text-gray-400 hover:text-teal-500"><Mail className="w-4 h-4" /></a>}
                {entry.telephone && <a href={`tel:${entry.telephone}`} className="text-gray-400 hover:text-teal-500"><Phone className="w-4 h-4" /></a>}
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
