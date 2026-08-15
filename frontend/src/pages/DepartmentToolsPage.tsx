import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { useParams, Link } from 'react-router-dom';
import api from '@/lib/api';
import { usePlatformConfig } from '@/contexts/PlatformContext';
import {
  ArrowLeft, Boxes, FileText, ListChecks, BookOpen, Settings, Wrench,
} from 'lucide-react';
import { SavedReportsSection } from '@/pages/DepartmentReportPage';
import {
  ChecklistsTab,
  InventoryTab,
  DocumentsTab,
  SettingsTab,
} from '@/pages/DepartmentManagementPage';

const TOOLS_TABS = [
  { key: 'reports', label: 'Rapports', icon: FileText, module: 'DEPT_REPORTS' },
  { key: 'checklists', label: 'Checklists', icon: ListChecks, module: 'DEPT_CHECKLISTS' },
  { key: 'inventory', label: 'Inventaire', icon: Boxes, module: 'DEPT_INVENTORY' },
  { key: 'docs', label: 'Documentation', icon: BookOpen, module: 'DEPT_DOCUMENTS' },
  { key: 'settings', label: 'Paramètres', icon: Settings, module: null },
] as const;

/**
 * Outils & rapports du département — parité web de l'écran « Outils » mobile :
 * synthèses sauvegardées, checklists, inventaire matériel, documentation et
 * paramètres (seuils d'alertes). Chaque sous-module est masqué lorsque
 * l'administrateur l'a désactivé.
 */
export default function DepartmentToolsPage() {
  const { id } = useParams<{ id: string }>();
  const { moduleEnabled } = usePlatformConfig();
  const [tab, setTab] = useState<(typeof TOOLS_TABS)[number]['key']>('reports');

  const visibleTabs = TOOLS_TABS.filter((t) => !t.module || moduleEnabled(t.module));

  const { data: dept } = useQuery({
    queryKey: ['department', id],
    queryFn: async () => (await api.get(`/departments/${id}/detail`)).data as any,
    enabled: !!id,
  });

  // Si un onglet visible est retiré (désactivation en direct), on retombe sur le premier
  const activeTab = visibleTabs.some((t) => t.key === tab) ? tab : visibleTabs[0]?.key ?? 'reports';

  if (visibleTabs.length === 0) {
    return (
      <div className="page-container flex flex-col items-center justify-center min-h-[60vh] text-center">
        <Wrench className="w-10 h-10 text-gray-300 mb-3" />
        <h1 className="text-lg font-semibold text-gray-800 dark:text-gray-100">Outils désactivés</h1>
        <p className="text-sm text-gray-400 mt-1">
          L'administrateur a désactivé tous les sous-modules d'outils du département.
        </p>
        <Link to={`/departments/${id}`} className="btn-ghost btn-sm mt-4">
          <ArrowLeft className="w-4 h-4" /> Retour au département
        </Link>
      </div>
    );
  }

  const invalidate = () => {};

  return (
    <div className="page-container">
      <div className="page-header">
        <Link to={`/departments/${id}`} className="btn-ghost btn-sm mb-2 inline-flex">
          <ArrowLeft className="w-4 h-4" /> Retour au département
        </Link>
        <div className="flex items-center gap-3">
          <div className="p-3 rounded-xl bg-gradient-to-br from-amber-500 to-orange-500 text-white shadow-lg">
            <Wrench className="w-6 h-6" />
          </div>
          <div className="flex-1">
            <h1 className="page-title">Outils & rapports</h1>
            <p className="page-subtitle">
              {dept?.nom ? `${dept.nom} · ` : ''}synthèses · checklists · inventaire · documentation · paramètres
            </p>
          </div>
        </div>
      </div>

      <div className="flex gap-2 mb-6 overflow-x-auto pb-1">
        {visibleTabs.map((t) => {
          const Icon = t.icon;
          return (
            <button
              key={t.key}
              onClick={() => setTab(t.key)}
              className={`px-4 py-2 rounded-xl text-sm font-medium flex items-center gap-2 transition-all cursor-pointer whitespace-nowrap ${
                activeTab === t.key
                  ? 'bg-gradient-to-r from-amber-500 to-orange-500 text-white shadow-glow'
                  : 'bg-white/70 dark:bg-gray-800/70 text-gray-600 dark:text-gray-300 hover:bg-gray-100 dark:hover:bg-gray-700'
              }`}
            >
              <Icon className="w-4 h-4" /> {t.label}
            </button>
          );
        })}
      </div>

      {activeTab === 'reports' && <SavedReportsSection departmentId={id || ''} />}
      {activeTab === 'checklists' && <ChecklistsTab deptId={id || ''} onChanged={invalidate} />}
      {activeTab === 'inventory' && <InventoryTab deptId={id || ''} onChanged={invalidate} />}
      {activeTab === 'docs' && <DocumentsTab deptId={id || ''} onChanged={invalidate} />}
      {activeTab === 'settings' && <SettingsTab deptId={id || ''} />}
    </div>
  );
}
