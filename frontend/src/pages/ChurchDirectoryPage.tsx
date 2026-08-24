import React, { useState, useEffect } from 'react';
import { useI18n } from '@/i18n';
import api from '@/lib/api';
import SkeletonLoader from '@/components/shared/SkeletonLoader';
import EmptyState from '@/components/shared/EmptyState';
import Toast from '@/components/shared/Toast';
import { BookUser, Search, Phone, Mail, Eye, EyeOff } from 'lucide-react';

interface DirEntry {
  id: string;
  membreId: string;
  publicProfil: boolean;
  bio?: string;
  téléphone?: string;
  email?: string;
  département?: string;
  rôle?: string;
}

export default function ChurchDirectoryPage() {
  const { t } = useI18n();
  const [entries, setEntries] = useState<DirEntry[]>([]);
  const [loading, setLoading] = useState(true);
  const [search, setSearch] = useState('');
  const [myEntry, setMyEntry] = useState<DirEntry | null>(null);
  const [editing, setEditing] = useState(false);
  const [editData, setEditData] = useState({ bio: '', téléphone: '', email: '', publicProfil: true });

  useEffect(() => { loadDirectory(); loadMyEntry(); }, []);

  const loadDirectory = async () => {
    try {
      setLoading(true);
      const res = await api.get('/directory');
      setEntries(res.data.content || res.data || []);
    } catch { setEntries([]); }
    finally { setLoading(false); }
  };

  const loadMyEntry = async () => {
    try { const res = await api.get('/directory/me'); setMyEntry(res.data); setEditData({ bio: res.data.bio || '', téléphone: res.data.téléphone || '', email: res.data.email || '', publicProfil: res.data.publicProfil }); } catch {}
  };

  const updateMyEntry = async () => {
    try {
      await api.put('/directory/me', editData);
      Toast.success('Profil mis à jour');
      setEditing(false);
      loadMyEntry();
    } catch { Toast.error('Erreur'); }
  };

  const togglePublic = async () => {
    try { await api.patch('/directory/me/toggle'); Toast.success('Visibilité mise à jour'); loadMyEntry(); } catch {}
  };

  const filtered = entries.filter(e =>
    !search || e.bio?.toLowerCase().includes(search.toLowerCase()) || e.département?.toLowerCase().includes(search.toLowerCase())
  );

  return (
    <div className="min-h-screen p-4 md:p-8 max-w-7xl mx-auto">
      <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4 mb-8">
        <div>
          <h1 className="text-2xl md:text-3xl font-bold text-gray-900 dark:text-white flex items-center gap-3">
            <BookUser className="w-8 h-8 text-indigo-500" />
            {t('directory.title')}
          </h1>
          <p className="text-gray-500 dark:text-gray-400 mt-1">Annuaire des membres de votre église</p>
        </div>
        <button onClick={() => setEditing(true)}
          className="px-4 py-2 rounded-xl bg-gradient-to-r from-indigo-500 to-purple-500 text-white text-sm font-medium hover:from-indigo-600 hover:to-purple-600 transition-all shadow-lg flex items-center gap-2">
          {myEntry?.publicProfil ? <Eye className="w-4 h-4" /> : <EyeOff className="w-4 h-4" />}
          Mon profil ({myEntry?.publicProfil ? 'Public' : 'Privé'})
        </button>
      </div>

      <div className="relative mb-6">
        <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400" />
        <input type="text" placeholder="Rechercher dans l'annuaire..." value={search} onChange={e => setSearch(e.target.value)}
          className="w-full pl-10 pr-4 py-2.5 rounded-xl border border-gray-200 dark:border-white/10 bg-white dark:bg-white/5 text-sm" />
      </div>

      {loading ? <SkeletonLoader lines={5} variant="card" /> :
        filtered.length === 0 ? (
          <EmptyState icon={<BookUser className="w-8 h-8 text-gray-400" />}
            title="Aucun membre dans l'annuaire"
            message="Les membres qui ont rendu leur profil public apparaîtront ici" />
        ) : (
          <div className="grid gap-3 md:grid-cols-2 lg:grid-cols-3">
            {filtered.map(entry => (
              <div key={entry.id} className="bg-white dark:bg-white/5 rounded-xl p-4 border border-gray-200 dark:border-white/10">
                <div className="flex items-center gap-3 mb-2">
                  <div className="w-10 h-10 rounded-full bg-indigo-100 dark:bg-indigo-500/20 flex items-center justify-center">
                    <BookUser className="w-5 h-5 text-indigo-500" />
                  </div>
                  <div>
                    <div className="text-sm font-medium text-gray-900 dark:text-white">{entry.département || 'Membre'}</div>
                    {entry.rôle && <div className="text-xs text-gray-500">{entry.rôle}</div>}
                  </div>
                </div>
                {entry.bio && <p className="text-xs text-gray-500 dark:text-gray-400 mb-2 line-clamp-2">{entry.bio}</p>}
                <div className="flex items-center gap-3 text-xs text-gray-400">
                  {entry.téléphone && <span className="flex items-center gap-1"><Phone className="w-3 h-3" />{entry.téléphone}</span>}
                  {entry.email && <span className="flex items-center gap-1"><Mail className="w-3 h-3" />{entry.email}</span>}
                </div>
              </div>
            ))}
          </div>
        )}

      {editing && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
          <div className="fixed inset-0 bg-black/50 backdrop-blur-sm" onClick={() => setEditing(false)} />
          <div className="relative bg-white dark:bg-gray-800 rounded-2xl shadow-2xl max-w-lg w-full p-6 border border-gray-200 dark:border-white/10">
            <h2 className="text-lg font-bold text-gray-900 dark:text-white mb-4">Mon profil annuaire</h2>
            <div className="space-y-4">
              <textarea value={editData.bio} onChange={e => setEditData({ ...editData, bio: e.target.value })}
                rows={3} className="w-full px-4 py-2.5 rounded-xl border border-gray-200 dark:border-white/10 bg-white dark:bg-white/5 text-sm resize-none" placeholder="À propos de moi" />
              <div className="grid grid-cols-2 gap-4">
                <input type="text" value={editData.téléphone} onChange={e => setEditData({ ...editData, téléphone: e.target.value })}
                  className="px-4 py-2.5 rounded-xl border border-gray-200 dark:border-white/10 bg-white dark:bg-white/5 text-sm" placeholder="Téléphone" />
                <input type="email" value={editData.email} onChange={e => setEditData({ ...editData, email: e.target.value })}
                  className="px-4 py-2.5 rounded-xl border border-gray-200 dark:border-white/10 bg-white dark:bg-white/5 text-sm" placeholder="Email" />
              </div>
              <label className="flex items-center gap-2 text-sm">
                <input type="checkbox" checked={editData.publicProfil} onChange={e => setEditData({ ...editData, publicProfil: e.target.checked })} className="w-4 h-4 text-indigo-600 rounded" />
                Profil visible dans l'annuaire public
              </label>
            </div>
            <div className="flex justify-end gap-3 mt-6">
              <button onClick={() => setEditing(false)} className="px-4 py-2 rounded-xl border text-sm">Annuler</button>
              <button onClick={updateMyEntry} className="px-4 py-2 rounded-xl bg-indigo-500 text-white text-sm font-medium hover:bg-indigo-600">Enregistrer</button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
