import { useQuery } from '@tanstack/react-query';
import { useState, useMemo } from 'react';
import api from '@/lib/api';
import toast from 'react-hot-toast';
import { useDictionaries } from '@/hooks/useDictionaries';
import type { PageResponse, User } from '@/types';
import {
  Activity,
  Search,
  Filter,
  Loader2,
  Clock,
  User as UserIcon,
  Shield,
  Plus,
  Pencil,
  Trash2,
  ChevronLeft,
  ChevronRight,
  FileText,
  Database,
  Sparkles,
  Download,
  CalendarRange,
  RotateCcw,
} from 'lucide-react';

interface AuditEntry {
  id: string;
  utilisateurId: string;
  emailUtilisateur?: string;
  action: string;
  entiteType: string;
  entiteId?: string;
  details?: string;
  createdAt: string;
}

/** Repli (dictionnaire indisponible) — les valeurs réelles viennent de la base. */
const ENTITY_FALLBACK: Record<string, string> = {
  USER: 'Utilisateur',
  FAMILY: 'Famille',
  SOUL: 'Âme',
  REPORT: 'Rapport',
  DEPARTMENT: 'Département',
  TRANSFER: 'Transfert',
  EVENT: 'Événement',
  MEMBER_REQUEST: 'Demande membre',
};

function actionCategory(action: string): 'CREATE' | 'UPDATE' | 'DELETE' | 'OTHER' {
  const a = action.toUpperCase();
  if (a.includes('DELETE') || a.includes('SUPPR') || a.includes('REMOVE')) return 'DELETE';
  if (a.includes('CREATE') || a.includes('CREATION') || a.includes('ADD')) return 'CREATE';
  if (a.includes('UPDATE') || a.includes('MODIF') || a.includes('EDIT') || a.includes('CHANGE')) return 'UPDATE';
  return 'OTHER';
}

const ACTION_STYLES: Record<string, string> = {
  CREATE: 'bg-emerald-100/80 dark:bg-emerald-900/30 text-emerald-700 dark:text-emerald-300 border-emerald-200/60 dark:border-emerald-700/40',
  UPDATE: 'bg-blue-100/80 dark:bg-blue-900/30 text-blue-700 dark:text-blue-300 border-blue-200/60 dark:border-blue-700/40',
  DELETE: 'bg-red-100/80 dark:bg-red-900/30 text-red-700 dark:text-red-300 border-red-200/60 dark:border-red-700/40',
  OTHER: 'bg-gray-100/80 dark:bg-gray-800/40 text-gray-600 dark:text-gray-300 border-gray-200/60 dark:border-gray-700/40',
};

const ACTION_ICONS: Record<string, typeof Plus> = {
  CREATE: Plus,
  UPDATE: Pencil,
  DELETE: Trash2,
  OTHER: Shield,
};

function toIsoStart(date: string) {
  return date ? `${date}T00:00:00` : '';
}

function toIsoEnd(date: string) {
  return date ? `${date}T23:59:59` : '';
}

export default function AuditPage() {
  const dictionaries = useDictionaries();
  const [page, setPage] = useState(0);
  const [searchTerm, setSearchTerm] = useState('');
  const [entityFilter, setEntityFilter] = useState('');
  const [userFilter, setUserFilter] = useState('');
  const [dateDebut, setDateDebut] = useState('');
  const [dateFin, setDateFin] = useState('');
  const [isExporting, setIsExporting] = useState(false);

  // Liste des utilisateurs pour le filtre et la résolution des emails.
  const { data: users } = useQuery({
    queryKey: ['users', 'audit'],
    queryFn: async () => {
      const res = await api.get('/users?size=200');
      return res.data.content as User[];
    },
  });
  const userMap = useMemo(() => {
    const m = new Map<string, string>();
    (users ?? []).forEach((u) => m.set(u.id, `${u.firstName} ${u.lastName}`.trim() || u.email));
    return m;
  }, [users]);

  const hasFilters = !!(entityFilter || userFilter || dateDebut || dateFin);

  const resetFilters = () => {
    setSearchTerm('');
    setEntityFilter('');
    setUserFilter('');
    setDateDebut('');
    setDateFin('');
    setPage(0);
  };

  const buildParams = (includePagination = true) => {
    const params = new URLSearchParams();
    if (includePagination) {
      params.set('page', String(page));
      params.set('size', '20');
    }
    if (entityFilter) params.set('entiteType', entityFilter);
    if (userFilter) params.set('utilisateurId', userFilter);
    if (dateDebut) params.set('debut', toIsoStart(dateDebut));
    if (dateFin) params.set('fin', toIsoEnd(dateFin));
    return params;
  };

  const { data, isLoading } = useQuery({
    queryKey: ['audit', page, entityFilter, userFilter, dateDebut, dateFin],
    queryFn: async () => {
      const res = await api.get(`/audit?${buildParams()}`);
      return res.data as PageResponse<AuditEntry>;
    },
  });

  // Recherche client-side (le backend /audit ne filtre pas par texte).
  const entries = useMemo(() => {
    if (!data?.content) return [];
    const q = searchTerm.trim().toLowerCase();
    if (!q) return data.content;
    return data.content.filter((e) =>
      (e.action?.toLowerCase().includes(q)) ||
      (e.emailUtilisateur?.toLowerCase().includes(q)) ||
      (e.utilisateurId?.toLowerCase().includes(q)) ||
      (e.entiteType?.toLowerCase().includes(q)) ||
      (e.details?.toLowerCase().includes(q))
    );
  }, [data, searchTerm]);

  const stats = useMemo(() => {
    const items = data?.content ?? [];
    return {
      total: data?.totalElements ?? 0,
      create: items.filter((e) => actionCategory(e.action) === 'CREATE').length,
      update: items.filter((e) => actionCategory(e.action) === 'UPDATE').length,
      delete: items.filter((e) => actionCategory(e.action) === 'DELETE').length,
    };
  }, [data]);

  const handleExport = async () => {
    setIsExporting(true);
    try {
      const res = await api.get(`/audit/export?${buildParams(false)}`, { responseType: 'blob' });
      const url = window.URL.createObjectURL(new Blob([res.data]));
      const link = document.createElement('a');
      link.href = url;
      link.setAttribute('download', `journal-audit-${new Date().toISOString().split('T')[0]}.csv`);
      document.body.appendChild(link);
      link.click();
      link.remove();
      window.URL.revokeObjectURL(url);
      toast.success('Journal d’audit exporté en CSV');
    } catch {
      toast.error("Erreur lors de l'export du journal");
    } finally {
      setIsExporting(false);
    }
  };

  return (
    <div className="page-container">
      {/* Header */}
      <div className="page-header">
        <div className="flex items-center gap-3">
          <div className="p-3 rounded-xl bg-gradient-to-br from-violet-500 to-purple-600 text-white shadow-lg glow-blue">
            <Activity className="w-6 h-6" />
          </div>
          <div>
            <h1 className="page-title flex items-center gap-2">
              Journal d'audit
              <span className="badge-info text-[10px]"><Shield className="w-3 h-3" /> Immuable</span>
            </h1>
            <p className="page-subtitle">Traçabilité de toutes les actions sensibles</p>
          </div>
        </div>
        <div className="page-header-actions">
          <button onClick={handleExport} disabled={isExporting} className="btn-secondary btn-sm">
            {isExporting ? <Loader2 className="w-4 h-4 animate-spin" /> : <Download className="w-4 h-4" />}
            {isExporting ? 'Génération…' : 'Exporter CSV'}
          </button>
          {hasFilters && (
            <button onClick={resetFilters} className="btn-ghost btn-sm">
              <RotateCcw className="w-4 h-4" /> Réinitialiser
            </button>
          )}
        </div>
      </div>

      {/* Stats cards */}
      <div className="grid grid-cols-2 lg:grid-cols-4 gap-4 mb-6">
        {[
          { label: 'Entrées (filtre)', value: stats.total, icon: Database, gradient: 'from-violet-500 to-purple-500' },
          { label: 'Créations (page)', value: stats.create, icon: Plus, gradient: 'from-emerald-500 to-teal-500' },
          { label: 'Modifications (page)', value: stats.update, icon: Pencil, gradient: 'from-blue-500 to-indigo-500' },
          { label: 'Suppressions (page)', value: stats.delete, icon: Trash2, gradient: 'from-red-500 to-rose-500' },
        ].map((s, i) => (
          <div key={s.label} className="stat-card animate-slide-up" style={{ animationDelay: `${i * 60}ms` }}>
            <div className={`absolute top-0 left-0 right-0 h-0.5 bg-gradient-to-r ${s.gradient} opacity-60`} />
            <div className="flex items-start justify-between mb-2">
              <span className="stat-label text-[10px]">{s.label}</span>
              <div className={`p-1.5 rounded-lg bg-gradient-to-br ${s.gradient} text-white shadow-sm`}>
                <s.icon className="w-3.5 h-3.5" />
              </div>
            </div>
            <p className="stat-value text-xl">{s.value}</p>
          </div>
        ))}
      </div>

      {/* Filters */}
      <div className="glass-card p-4 mb-6 animate-slide-up" style={{ animationDelay: '240ms' }}>
        <div className="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-5 gap-3">
          <div className="xl:col-span-2">
            <div className="relative">
              <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400" />
              <input
                type="text"
                placeholder="Rechercher une action, un utilisateur, un détail..."
                value={searchTerm}
                onChange={(e) => setSearchTerm(e.target.value)}
                className="input pl-9"
              />
            </div>
          </div>
          <div className="relative">
            <Filter className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400 pointer-events-none" />
            <select
              value={entityFilter}
              onChange={(e) => { setEntityFilter(e.target.value); setPage(0); }}
              className="input pl-9 appearance-none"
            >
              <option value="">Toutes les entités</option>
              {(dictionaries.options('AUDIT_ENTITY').length > 0
                ? dictionaries.options('AUDIT_ENTITY')
                : Object.entries(ENTITY_FALLBACK).map(([code, label]) => ({ code, label }))
              ).map((o) => (
                <option key={o.code} value={o.code}>{o.label}</option>
              ))}
            </select>
          </div>
          <div className="relative">
            <UserIcon className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400 pointer-events-none" />
            <select
              value={userFilter}
              onChange={(e) => { setUserFilter(e.target.value); setPage(0); }}
              className="input pl-9 appearance-none"
            >
              <option value="">Tous les utilisateurs</option>
              {(users ?? []).map((u) => (
                <option key={u.id} value={u.id}>
                  {u.firstName} {u.lastName} · {u.email}
                </option>
              ))}
            </select>
          </div>
          <div className="flex items-center gap-2">
            <div className="flex items-center gap-1.5 flex-1">
              <CalendarRange className="w-4 h-4 text-gray-400 shrink-0" />
              <input
                type="date"
                value={dateDebut}
                max={dateFin || undefined}
                onChange={(e) => { setDateDebut(e.target.value); setPage(0); }}
                className="input py-2"
                aria-label="Date de début"
              />
            </div>
            <span className="text-gray-400 text-sm">→</span>
            <input
              type="date"
              value={dateFin}
              min={dateDebut || undefined}
              onChange={(e) => { setDateFin(e.target.value); setPage(0); }}
              className="input py-2 flex-1"
              aria-label="Date de fin"
            />
          </div>
        </div>
      </div>

      {/* Table */}
      {isLoading ? (
        <div className="flex justify-center py-16">
          <Loader2 className="w-8 h-8 animate-spin text-primary-500" />
        </div>
      ) : (
        <div className="glass-card overflow-hidden animate-slide-up" style={{ animationDelay: '300ms' }}>
          <div className="overflow-x-auto">
            <table className="data-table">
              <thead>
                <tr>
                  <th>Date</th>
                  <th>Utilisateur</th>
                  <th>Action</th>
                  <th>Entité</th>
                  <th>Détails</th>
                </tr>
              </thead>
              <tbody>
                {entries.length === 0 ? (
                  <tr>
                    <td colSpan={5} className="py-12 text-center">
                      <div className="flex flex-col items-center gap-2">
                        <Sparkles className="w-8 h-8 text-gray-300 dark:text-gray-600" />
                        <p className="text-sm text-gray-400">Aucune entrée ne correspond à la recherche</p>
                      </div>
                    </td>
                  </tr>
                ) : entries.map((entry) => {
                  const cat = actionCategory(entry.action);
                  const ActionIcon = ACTION_ICONS[cat];
                  const entiteLabel = dictionaries.label('AUDIT_ENTITY', entry.entiteType) || ENTITY_FALLBACK[entry.entiteType] || entry.entiteType;
                  const userName = entry.emailUtilisateur || userMap.get(entry.utilisateurId) || entry.utilisateurId?.slice(0, 8);
                  return (
                    <tr key={entry.id}>
                      <td className="text-sm text-gray-500 dark:text-gray-400 whitespace-nowrap">
                        <div className="flex items-center gap-1.5">
                          <Clock className="w-3 h-3 text-gray-400" />
                          {new Date(entry.createdAt).toLocaleString('fr-FR', {
                            day: '2-digit', month: 'short', year: 'numeric', hour: '2-digit', minute: '2-digit',
                          })}
                        </div>
                      </td>
                      <td className="whitespace-nowrap">
                        <div className="flex items-center gap-2">
                          <div className="w-6 h-6 rounded-lg bg-gradient-to-br from-gray-500 to-gray-600 flex items-center justify-center">
                            <UserIcon className="w-3 h-3 text-white" />
                          </div>
                          <span className="text-sm text-gray-700 dark:text-gray-300 max-w-[140px] truncate" title={userName}>
                            {userName}
                          </span>
                        </div>
                      </td>
                      <td className="whitespace-nowrap">
                        <span className={`inline-flex items-center gap-1.5 px-2.5 py-1 rounded-lg text-xs font-medium border ${ACTION_STYLES[cat]}`}>
                          <ActionIcon className="w-3 h-3" />
                          {entry.action}
                        </span>
                      </td>
                      <td className="whitespace-nowrap">
                        <span className="inline-flex items-center gap-1.5 text-sm text-gray-600 dark:text-gray-300">
                          <FileText className="w-3.5 h-3.5 text-gray-400" />
                          {entiteLabel}
                        </span>
                      </td>
                      <td className="text-sm text-gray-500 dark:text-gray-400 max-w-xs truncate" title={entry.details}>
                        {entry.details || '-'}
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>

          {/* Pagination */}
          {data && (
            <div className="flex flex-col sm:flex-row items-center justify-between gap-3 px-6 py-4 border-t border-gray-100 dark:border-gray-800">
              <span className="text-sm text-gray-500 dark:text-gray-400">
                Page {page + 1} sur {Math.max(data.totalPages, 1)} · {data.totalElements} entrées
              </span>
              <div className="flex gap-2">
                <button
                  onClick={() => setPage(p => Math.max(0, p - 1))}
                  disabled={page === 0}
                  className="btn-ghost btn-sm"
                >
                  <ChevronLeft className="w-3.5 h-3.5" /> Précédent
                </button>
                <button
                  onClick={() => setPage(p => p + 1)}
                  disabled={page >= data.totalPages - 1}
                  className="btn-ghost btn-sm"
                >
                  Suivant <ChevronRight className="w-3.5 h-3.5" />
                </button>
              </div>
            </div>
          )}
        </div>
      )}
    </div>
  );
}
