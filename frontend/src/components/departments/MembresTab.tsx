import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useNavigate } from 'react-router-dom';
import api, { getErrorMessage } from '@/lib/api';
import toast from 'react-hot-toast';
import {
  Users, UserPlus, UserMinus, Search, ChevronRight, Mail, Phone,
  CheckCircle, Clock, UserX, Heart, Download, Upload, Loader2, Filter,
} from 'lucide-react';

type DeptMember = {
  id: string;
  soulId: string;
  nom: string;
  email?: string;
  telephone?: string;
  statut: string;
  familleNom?: string;
  faiseurNom?: string;
  dateIntegration?: string;
  dateDernierContact?: string;
  equipeNom?: string;
  postesNom?: string[];
};

type MemberStatus = 'ALL' | 'ACTIF' | 'EN_INTEGRATION' | 'EN_VEILLE' | 'DECROCHE';

const STATUS_CONFIG: Record<string, { label: string; color: string; bg: string }> = {
  ACTIF: { label: 'Actif', color: 'text-emerald-600', bg: 'bg-emerald-500' },
  EN_INTEGRATION: { label: 'En intégration', color: 'text-blue-600', bg: 'bg-blue-500' },
  EN_VEILLE: { label: 'En veille', color: 'text-amber-600', bg: 'bg-amber-500' },
  DECROCHE: { label: 'Décroché', color: 'text-red-600', bg: 'bg-red-500' },
};

function initials(name: string) {
  const parts = name.trim().split(/\s+/).filter(Boolean);
  if (parts.length === 0) return '?';
  if (parts.length === 1) return parts[0].slice(0, 2).toUpperCase();
  return (parts[0][0] + parts[parts.length - 1][0]).toUpperCase();
}

export function MembresTab({ deptId, members, onChanged }: { deptId: string; members: any[]; onChanged: () => void }) {
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const [search, setSearch] = useState('');
  const [filterStatus, setFilterStatus] = useState<MemberStatus>('ALL');
  const [showAdd, setShowAdd] = useState(false);

  // Fetch full member details for management view
  const { data: membersMgmt = [] } = useQuery({
    queryKey: ['department', deptId, 'members', 'management'],
    queryFn: async () => (await api.get(`/departments/${deptId}/members/management`)).data as DeptMember[],
    enabled: !!deptId,
  });

  const { data: candidates = [] } = useQuery({
    queryKey: ['department', deptId, 'members', 'candidates', search],
    queryFn: async () => (await api.get(`/departments/${deptId}/members/candidates`, { params: { q: search } })).data as any[],
    enabled: !!deptId && showAdd && search.trim().length >= 2,
  });

  const addMutation = useMutation({
    mutationFn: async (soulId: string) => {
      await api.post(`/departments/${deptId}/members`, { soulId });
    },
    onSuccess: () => {
      toast.success('Membre ajouté au département ✅');
      queryClient.invalidateQueries({ queryKey: ['department', deptId, 'members'] });
      onChanged();
    },
    onError: (err) => toast.error(getErrorMessage(err)),
  });

  const removeMutation = useMutation({
    mutationFn: async (memberId: string) => api.delete(`/departments/${deptId}/members/${memberId}`),
    onSuccess: () => {
      toast.success('Membre retiré du département');
      queryClient.invalidateQueries({ queryKey: ['department', deptId, 'members'] });
      onChanged();
    },
    onError: (err) => toast.error(getErrorMessage(err)),
  });

  const filtered = membersMgmt.filter((m) => {
    if (filterStatus !== 'ALL' && m.statut !== filterStatus) return false;
    if (search.trim()) {
      const q = search.toLowerCase();
      return m.nom.toLowerCase().includes(q) ||
        (m.email?.toLowerCase().includes(q)) ||
        (m.familleNom?.toLowerCase().includes(q));
    }
    return true;
  });

  const stats = {
    total: membersMgmt.length,
    actifs: membersMgmt.filter((m) => m.statut === 'ACTIF').length,
    integration: membersMgmt.filter((m) => m.statut === 'EN_INTEGRATION').length,
    veille: membersMgmt.filter((m) => m.statut === 'EN_VEILLE').length,
    decroches: membersMgmt.filter((m) => m.statut === 'DECROCHE').length,
  };

  const handleExport = async () => {
    try {
      const res = await api.get(`/departments/${deptId}/members/export`, { responseType: 'blob' });
      const url = URL.createObjectURL(res.data as Blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = `membres-departement.csv`;
      a.click();
      URL.revokeObjectURL(url);
      toast.success('Export CSV téléchargé 📥');
    } catch (err) {
      toast.error(getErrorMessage(err));
    }
  };

  return (
    <div className="space-y-5">
      {/* KPIs */}
      <div className="grid grid-cols-2 sm:grid-cols-4 gap-3">
        <div className="stat-card p-3 text-center">
          <Users className="w-4 h-4 mx-auto mb-1 text-primary-500" />
          <p className="stat-value text-xl">{stats.total}</p>
          <span className="stat-label text-[10px]">Total membres</span>
        </div>
        <div className="stat-card p-3 text-center">
          <CheckCircle className="w-4 h-4 mx-auto mb-1 text-emerald-500" />
          <p className="stat-value text-xl text-emerald-500">{stats.actifs}</p>
          <span className="stat-label text-[10px]">Actifs</span>
        </div>
        <div className="stat-card p-3 text-center">
          <Clock className="w-4 h-4 mx-auto mb-1 text-blue-500" />
          <p className="stat-value text-xl text-blue-500">{stats.integration}</p>
          <span className="stat-label text-[10px]">En intégration</span>
        </div>
        <div className="stat-card p-3 text-center">
          <Heart className="w-4 h-4 mx-auto mb-1 text-amber-500" />
          <p className="stat-value text-xl text-amber-500">{stats.veille}</p>
          <span className="stat-label text-[10px]">En veille</span>
        </div>
      </div>

      {/* Search + filters + actions */}
      <div className="glass-card p-4">
        <div className="flex flex-wrap items-center gap-3">
          <div className="relative flex-1 min-w-[200px]">
            <Search className="w-4 h-4 absolute left-3 top-1/2 -translate-y-1/2 text-gray-400" />
            <input
              className="input pl-9"
              placeholder="Rechercher un membre (nom, email, famille…)"
              value={search}
              onChange={(e) => setSearch(e.target.value)}
            />
          </div>
          <div className="flex items-center gap-2">
            <Filter className="w-4 h-4 text-gray-400" />
            <select
              className="input py-1.5"
              value={filterStatus}
              onChange={(e) => setFilterStatus(e.target.value as MemberStatus)}
            >
              <option value="ALL">Tous les statuts</option>
              {Object.entries(STATUS_CONFIG).map(([k, v]) => (
                <option key={k} value={k}>{v.label}</option>
              ))}
            </select>
          </div>
          <button onClick={() => setShowAdd(!showAdd)} className="btn-primary btn-sm cursor-pointer">
            <UserPlus className="w-4 h-4" /> {showAdd ? 'Fermer' : 'Ajouter'}
          </button>
          <button onClick={handleExport} className="btn-ghost btn-sm cursor-pointer">
            <Download className="w-4 h-4" /> CSV
          </button>
        </div>

        {/* Add member search */}
        {showAdd && (
          <div className="mt-3 p-3 rounded-2xl bg-gray-50 dark:bg-gray-800/40 border border-gray-200/60 dark:border-gray-700/40">
            <p className="text-xs text-gray-400 mb-2">Recherchez une personne déjà inscrite pour l'ajouter au département :</p>
            {search.trim().length >= 2 && candidates.length > 0 ? (
              <div className="space-y-1 max-h-48 overflow-y-auto">
                {candidates.map((c: any) => (
                  <div key={c.id} className="flex items-center justify-between gap-2 p-2 rounded-lg hover:bg-gray-100 dark:hover:bg-gray-700/50">
                    <div className="flex items-center gap-2 min-w-0">
                      <div className="w-7 h-7 rounded-lg bg-primary-500/10 text-primary-600 flex items-center justify-center text-[10px] font-bold shrink-0">
                        {initials(c.nom)}
                      </div>
                      <div className="min-w-0">
                        <p className="text-sm font-medium text-gray-900 dark:text-gray-100 truncate">{c.nom}</p>
                        <p className="text-[10px] text-gray-400">{c.email || '—'}</p>
                      </div>
                    </div>
                    <button
                      onClick={() => addMutation.mutate(c.id)}
                      disabled={addMutation.isPending}
                      className="btn-primary btn-xs cursor-pointer"
                    >
                      <UserPlus className="w-3 h-3" /> Ajouter
                    </button>
                  </div>
                ))}
              </div>
            ) : search.trim().length >= 2 ? (
              <p className="text-xs text-gray-400 text-center py-3">Aucun résultat pour « {search} »</p>
            ) : (
              <p className="text-xs text-gray-400 text-center py-3">Tapez au moins 2 caractères pour rechercher</p>
            )}
          </div>
        )}
      </div>

      {/* Members list */}
      <div className="glass-card p-5">
        <div className="flex items-center gap-2 mb-4">
          <Users className="w-4 h-4 text-primary-500" />
          <h3 className="text-sm font-semibold text-gray-900 dark:text-gray-100">Membres du département</h3>
          <span className="badge text-[10px] badge-info">{filtered.length}</span>
        </div>

        {filtered.length === 0 ? (
          <div className="text-center py-10">
            <Users className="w-10 h-10 text-gray-300 mx-auto mb-2" />
            <p className="text-sm text-gray-400">
              {membersMgmt.length === 0
                ? 'Aucun membre dans ce département'
                : 'Aucun membre ne correspond aux filtres'}
            </p>
          </div>
        ) : (
          <div className="space-y-1.5">
            {filtered.map((m) => {
              const statusConf = STATUS_CONFIG[m.statut] || { label: m.statut, color: 'text-gray-500', bg: 'bg-gray-400' };
              return (
                <div
                  key={m.id}
                  className="flex items-center gap-3 p-3 rounded-xl hover:bg-gray-50 dark:hover:bg-gray-800/40 transition-all group"
                >
                  <div className={`w-10 h-10 rounded-xl flex items-center justify-center text-sm font-bold text-white shrink-0 ${statusConf.bg}`}>
                    {initials(m.nom)}
                  </div>
                  <div className="min-w-0 flex-1">
                    <div className="flex items-center gap-2 flex-wrap">
                      <span className="text-sm font-semibold text-gray-900 dark:text-gray-100">{m.nom}</span>
                      <span className={`badge text-[9px] ${m.statut === 'ACTIF' ? 'badge-success' : m.statut === 'EN_INTEGRATION' ? 'badge-info' : m.statut === 'EN_VEILLE' ? 'badge-warning' : 'badge-danger'}`}>
                        {statusConf.label}
                      </span>
                      {m.equipeNom && <span className="badge text-[9px] badge-gray">{m.equipeNom}</span>}
                    </div>
                    <div className="flex items-center gap-3 text-[10px] text-gray-400 mt-0.5 flex-wrap">
                      {m.familleNom && <span>Famille {m.familleNom}</span>}
                      {m.faiseurNom && <span>· {m.faiseurNom}</span>}
                      {m.email && <span className="flex items-center gap-0.5"><Mail className="w-2.5 h-2.5" /> {m.email}</span>}
                      {m.telephone && <span className="flex items-center gap-0.5"><Phone className="w-2.5 h-2.5" /> {m.telephone}</span>}
                    </div>
                  </div>
                  <div className="flex items-center gap-1 flex-shrink-0 opacity-0 group-hover:opacity-100 transition-opacity">
                    <button
                      onClick={() => navigate(`/souls/${m.soulId}`)}
                      title="Fiche membre"
                      className="p-1.5 rounded-lg text-gray-400 hover:text-primary-600 hover:bg-primary-500/10 transition-all cursor-pointer"
                    >
                      <ChevronRight className="w-4 h-4" />
                    </button>
                    <button
                      onClick={() => {
                        if (confirm(`Retirer ${m.nom} du département ?`)) removeMutation.mutate(m.id);
                      }}
                      title="Retirer du département"
                      className="p-1.5 rounded-lg text-gray-400 hover:text-red-600 hover:bg-red-500/10 transition-all cursor-pointer"
                    >
                      <UserMinus className="w-3.5 h-3.5" />
                    </button>
                  </div>
                </div>
              );
            })}
          </div>
        )}
      </div>
    </div>
  );
}
