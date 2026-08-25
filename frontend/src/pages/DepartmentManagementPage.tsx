import { useEffect, useMemo, useState } from 'react';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import { useParams, useNavigate, useSearchParams } from 'react-router-dom';
import api from '@/lib/api';
import { useAuth } from '@/contexts/AuthContext';
import { usePlatformConfig } from '@/contexts/PlatformContext';
import {
  Building2, ArrowLeft, Network, Briefcase, Users2, ListTodo, History,
  Boxes, BookOpen, Settings, ListChecks, Search, Loader2, Users, FileText,
} from 'lucide-react';
import {
  MembresTab, OrganisationTab, PositionsTab, AssignmentsTab, TasksTab,
  ActivityTab, ChecklistsTab, InventoryTab, DocumentsTab, SettingsTab,
} from '@/components/departments';
import { SavedReportsSection } from '@/pages/DepartmentReportPage';
import type { Team, Position, Assignment, ActivityItem } from '@/components/departments/types';

/**
 * Onglets principaux de gestion de département.
 * L'ordre reflète le workflow RH d'un Responsable : d'abord les gens (membres,
 * équipes, tâches), puis la structure (organisation, postes, affectations),
 * puis les outils (checklists, inventaire, docs, rapports, paramètres).
 */
const ALL_TABS = [
  { key: 'members', label: 'Membres', icon: Users, roles: ['ADMIN', 'PASTEUR', 'RESPONSABLE'] },
  { key: 'teams', label: 'Équipes & Organisation', icon: Network, roles: ['ADMIN', 'PASTEUR', 'RESPONSABLE'] },
  { key: 'tasks', label: 'Tâches', icon: ListTodo, roles: ['ADMIN', 'PASTEUR', 'RESPONSABLE'] },
  { key: 'positions', label: 'Postes', icon: Briefcase, roles: ['ADMIN', 'PASTEUR', 'RESPONSABLE'] },
  { key: 'assignments', label: 'Affectations', icon: Users2, roles: ['ADMIN', 'PASTEUR', 'RESPONSABLE'] },
  { key: 'checklists', label: 'Checklists', icon: ListChecks, roles: ['ADMIN', 'PASTEUR', 'RESPONSABLE'], module: 'DEPT_CHECKLISTS' },
  { key: 'inventory', label: 'Inventaire', icon: Boxes, roles: ['ADMIN', 'PASTEUR', 'RESPONSABLE'], module: 'DEPT_INVENTORY' },
  { key: 'docs', label: 'Documentation', icon: BookOpen, roles: ['ADMIN', 'PASTEUR', 'RESPONSABLE'], module: 'DEPT_DOCUMENTS' },
  { key: 'reports', label: 'Rapports', icon: FileText, roles: ['ADMIN', 'PASTEUR', 'RESPONSABLE'], module: 'DEPT_REPORTS' },
  { key: 'settings', label: 'Paramètres', icon: Settings, roles: ['ADMIN', 'PASTEUR', 'RESPONSABLE'] },
  { key: 'activity', label: 'Activité', icon: History, roles: ['ADMIN', 'PASTEUR', 'RESPONSABLE'] },
] as const;

type TabKey = (typeof ALL_TABS)[number]['key'];

export default function DepartmentManagementPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const [searchParams, setSearchParams] = useSearchParams();
  const queryClient = useQueryClient();
  const { activeRole } = useAuth();
  const initialTab = (searchParams.get('tab') as TabKey) || 'members';
  const [tab, setTab] = useState<TabKey>(initialTab);
  const [search, setSearch] = useState('');
  const { moduleEnabled } = usePlatformConfig();

  // Filtrer les onglets selon le rôle actif ET les modules activés
  const visibleTabs = useMemo(() => {
    return ALL_TABS.filter((t) => {
      // Visibilité par rôle
      if (!activeRole) return false;
      if (!(t.roles as readonly string[]).includes(activeRole)) return false;
      // Visibilité par module
      if ('module' in t && t.module && !moduleEnabled(t.module)) return false;
      return true;
    });
  }, [activeRole, moduleEnabled]);

  // Si l'onglet actif n'est plus visible, revenir au premier onglet visible
  useEffect(() => {
    if (!visibleTabs.some((t) => t.key === tab)) {
      setTab(visibleTabs[0]?.key ?? 'members');
    }
  }, [visibleTabs, tab]); // eslint-disable-line react-hooks/exhaustive-deps

  const { data: searchResults } = useQuery({
    queryKey: ['department', id, 'search', search],
    queryFn: async () => (await api.get(`/departments/${id}/search`, { params: { q: search } })).data,
    enabled: !!id && search.trim().length >= 2,
  });

  const { data: dept } = useQuery({
    queryKey: ['department', id],
    queryFn: async () => (await api.get(`/departments/${id}/detail`)).data,
    enabled: !!id,
  });

  const { data: membersPage } = useQuery({
    queryKey: ['department', id, 'members'],
    queryFn: async () => (await api.get(`/departments/${id}/members?size=200`)).data,
    enabled: !!id,
  });
  const members: any[] = membersPage?.content || [];

  const { data: overview, isLoading } = useQuery({
    queryKey: ['department', id, 'management'],
    queryFn: async () => (await api.get(`/departments/${id}/management`)).data,
    enabled: !!id,
  });

  const invalidate = () => queryClient.invalidateQueries({ queryKey: ['department', id, 'management'] });

  const teams: Team[] = overview?.teams ?? [];
  const positions: Position[] = overview?.positions ?? [];
  const assignments: Assignment[] = overview?.assignments ?? [];
  const taskStats = overview?.taskStats ?? {};
  const activity: ActivityItem[] = overview?.activity ?? [];
  const org = overview?.org ?? {};
  const rootTeams = useMemo(() => teams.filter((t) => !t.parentId), [teams]);

  if (isLoading) {
    return (
      <div className="page-container flex items-center justify-center min-h-[60vh]">
        <Loader2 className="w-8 h-8 animate-spin text-primary-500" />
      </div>
    );
  }

  return (
    <div className="page-container">
      <div className="page-header">
        <button onClick={() => navigate(`/departments/${id}`)} className="btn-ghost btn-sm mb-2">
          <ArrowLeft className="w-4 h-4" /> Retour au département
        </button>
        <div className="flex items-center gap-3">
          <div className="p-3 rounded-xl bg-gradient-to-br from-amber-500 to-orange-500 text-white shadow-lg">
            <Building2 className="w-6 h-6" />
          </div>
          <div className="flex-1">
            <h1 className="page-title">Gestion de {dept?.nom || 'département'}</h1>
            <p className="page-subtitle">Membres · équipes · tâches · postes · affectations · outils · rapports</p>
          </div>
        </div>
        <div className="relative mt-3 w-full sm:w-80 sm:mt-0">
          <input
            className="input pl-9"
            placeholder="Recherche rapide : membre, équipe, poste, tâche…"
            value={search}
            onChange={(e) => setSearch(e.target.value)}
          />
          <Search className="w-4 h-4 absolute left-3 top-1/2 -translate-y-1/2 text-gray-400" />
        </div>
      </div>

      {search.trim().length >= 2 && searchResults && (
        <div className="glass-card p-5 mb-5">
          <h3 className="text-sm font-semibold text-gray-900 dark:text-gray-100 mb-2">
            {searchResults.total > 0
              ? `${searchResults.total} résultat${searchResults.total > 1 ? 's' : ''}`
              : 'Aucun résultat'}
          </h3>
          {searchResults.total === 0 ? (
            <p className="text-sm text-gray-400">Essayez un nom, un poste, une équipe ou une tâche.</p>
          ) : (
            <div className="grid grid-cols-1 lg:grid-cols-2 gap-4">
              {(searchResults.membres ?? []).length > 0 && (
                <div>
                  <p className="text-[11px] uppercase tracking-wide text-gray-400 mb-1">Membres</p>
                  {(searchResults.membres ?? []).map((m: any) => (
                    <div key={m.id} className="px-3 py-2 text-sm">{m.nomComplet || m.nom}</div>
                  ))}
                </div>
              )}
              {(searchResults.equipes ?? []).length > 0 && (
                <div>
                  <p className="text-[11px] uppercase tracking-wide text-gray-400 mb-1">Équipes</p>
                  {(searchResults.equipes ?? []).map((t: any) => (
                    <div key={t.id} className="px-3 py-2 text-sm">{t.nom}</div>
                  ))}
                </div>
              )}
            </div>
          )}
        </div>
      )}

      {/* KPIs rapides */}
      <div className="grid grid-cols-2 sm:grid-cols-4 gap-3 mb-5">
        <div className="stat-card p-3 text-center">
          <span className="stat-label text-[10px]">Membres</span>
          <p className="stat-value text-xl">{members.length}</p>
        </div>
        <div className="stat-card p-3 text-center">
          <span className="stat-label text-[10px]">Équipes actives</span>
          <p className="stat-value text-xl">{org.equipesActives ?? teams.filter((t) => t.statut === 'ACTIVE').length}</p>
        </div>
        <div className="stat-card p-3 text-center">
          <span className="stat-label text-[10px]">Postes actifs</span>
          <p className="stat-value text-xl">{org.postesActifs ?? positions.filter((p) => p.statut === 'ACTIVE').length}</p>
        </div>
        <div className="stat-card p-3 text-center">
          <span className="stat-label text-[10px]">Tâches en retard</span>
          <p className="stat-value text-xl text-red-500">{taskStats.enRetard ?? 0}</p>
        </div>
      </div>

      {/* Navigation par onglets */}
      <div className="flex gap-2 mb-6 overflow-x-auto pb-1">
        {visibleTabs.map((t) => {
          const Icon = t.icon;
          return (
            <button
              key={t.key}
              onClick={() => { setTab(t.key); setSearchParams({ tab: t.key }); }}
              className={`px-4 py-2 rounded-xl text-sm font-medium flex items-center gap-2 transition-all cursor-pointer whitespace-nowrap ${
                tab === t.key
                  ? 'bg-gradient-to-r from-amber-500 to-orange-500 text-white shadow-glow'
                  : 'bg-white/70 dark:bg-gray-800/70 text-gray-600 dark:text-gray-300 hover:bg-gray-100 dark:hover:bg-gray-700'
              }`}
            >
              <Icon className="w-4 h-4" /> {t.label}
            </button>
          );
        })}
      </div>

      {/* Contenu des onglets */}
      {tab === 'members' && (
        <MembresTab deptId={id || ''} members={members} onChanged={invalidate} />
      )}
      {tab === 'teams' && (
        <OrganisationTab deptId={id || ''} teams={teams} rootTeams={rootTeams} members={members} onChanged={invalidate} />
      )}
      {tab === 'tasks' && (
        <TasksTab deptId={id || ''} taskStats={taskStats} teams={teams} members={members} onChanged={invalidate} />
      )}

      {tab === 'positions' && <PositionsTab deptId={id || ''} positions={positions} onChanged={invalidate} />}
      {tab === 'assignments' && (
        <AssignmentsTab deptId={id || ''} assignments={assignments} teams={teams} positions={positions} members={members} onChanged={invalidate} />
      )}
      {tab === 'checklists' && <ChecklistsTab deptId={id || ''} onChanged={invalidate} />}
      {tab === 'inventory' && <InventoryTab deptId={id || ''} onChanged={invalidate} />}
      {tab === 'docs' && <DocumentsTab deptId={id || ''} onChanged={invalidate} />}
      {tab === 'reports' && <SavedReportsSection departmentId={id || ''} />}
      {tab === 'settings' && <SettingsTab deptId={id || ''} />}
      {tab === 'activity' && <ActivityTab activity={activity} />}
    </div>
  );
}
