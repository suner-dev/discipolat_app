import { useState, useCallback } from 'react';
import { useAuth } from '@/contexts/AuthContext';
import { useExportReport } from '@/hooks/useExportReport';
import {
  Church, Heart, Users, Building2, FileText, Bell, ArrowLeftRight,
  Calendar, Shield, Target, BookOpen, CalendarClock, Star,
  FileDown, Loader2,
} from 'lucide-react';

// Lazy-loaded tab components
import PasteurDashboardTab from '@/components/pasteur/PasteurDashboardTab';
import PasteurSoulsTab from '@/components/pasteur/PasteurSoulsTab';
import PasteurFamiliesTab from '@/components/pasteur/PasteurFamiliesTab';
import PasteurUsersTab from '@/components/pasteur/PasteurUsersTab';
import PasteurDepartmentsTab from '@/components/pasteur/PasteurDepartmentsTab';
import PasteurCrmTab from '@/components/pasteur/PasteurCrmTab';
import PasteurAlertsTab from '@/components/pasteur/PasteurAlertsTab';
import PasteurTransfersTab from '@/components/pasteur/PasteurTransfersTab';
import PasteurAuditTab from '@/components/pasteur/PasteurAuditTab';
import PasteurVisitsTab from '@/components/pasteur/PasteurVisitsTab';
import PasteurPrayersTab from '@/components/pasteur/PasteurPrayersTab';
import PasteurEventsTab from '@/components/pasteur/PasteurEventsTab';
import PasteurEvaluationsTab from '@/components/pasteur/PasteurEvaluationsTab';
import PasteurReportsTab from '@/components/pasteur/PasteurReportsTab';

const TABS = [
  { id: 'dashboard', label: 'Tableau de bord', icon: Church, color: 'text-primary-500' },
  { id: 'ames', label: 'Âmes', icon: Heart, color: 'text-rose-500' },
  { id: 'familles', label: 'Familles', icon: Users, color: 'text-primary-500' },
  { id: 'utilisateurs', label: 'Utilisateurs', icon: Building2, color: 'text-blue-500' },
  { id: 'departements', label: 'Départements', icon: Building2, color: 'text-amber-500' },
  { id: 'rapports', label: 'Rapports', icon: FileText, color: 'text-emerald-500' },
  { id: 'alertes', label: 'Alertes', icon: Bell, color: 'text-red-500' },
  { id: 'transferts', label: 'Transferts', icon: ArrowLeftRight, color: 'text-violet-500' },
  { id: 'visites', label: 'Visites', icon: Calendar, color: 'text-teal-500' },
  { id: 'audit', label: 'Audit', icon: Shield, color: 'text-gray-500' },
  { id: 'crm', label: 'CRM Faiseur', icon: Target, color: 'text-pink-500' },
  { id: 'prieres', label: 'Prières', icon: BookOpen, color: 'text-indigo-500' },
  { id: 'evenements', label: 'Événements', icon: CalendarClock, color: 'text-orange-500' },
  { id: 'evaluations', label: 'Évaluations', icon: Star, color: 'text-yellow-500' },
];

const getGreeting = () => {
  const h = new Date().getHours();
  if (h < 12) return 'Bonjour';
  if (h < 17) return 'Bon après-midi';
  return 'Bonsoir';
};

export default function PasteurDashboardPage() {
  const { user } = useAuth();
  const { exportReport, isExporting } = useExportReport();
  const [activeTab, setActiveTab] = useState('dashboard');

  const onNavigateToTab = useCallback((tab: string) => {
    setActiveTab(tab);
    window.scrollTo({ top: 0, behavior: 'smooth' });
  }, []);

  const renderTab = () => {
    switch (activeTab) {
      case 'dashboard': return <PasteurDashboardTab onNavigateToTab={onNavigateToTab} />;
      case 'ames': return <PasteurSoulsTab />;
      case 'familles': return <PasteurFamiliesTab />;
      case 'utilisateurs': return <PasteurUsersTab />;
      case 'departements': return <PasteurDepartmentsTab />;
      case 'rapports': return <PasteurReportsTab />;
      case 'alertes': return <PasteurAlertsTab />;
      case 'transferts': return <PasteurTransfersTab />;
      case 'visites': return <PasteurVisitsTab />;
      case 'audit': return <PasteurAuditTab />;
      case 'crm': return <PasteurCrmTab />;
      case 'prieres': return <PasteurPrayersTab />;
      case 'evenements': return <PasteurEventsTab />;
      case 'evaluations': return <PasteurEvaluationsTab />;
      default: return <PasteurDashboardTab onNavigateToTab={onNavigateToTab} />;
    }
  };

  return (
    <div className="page-container">
      {/* Header */}
      <div className="page-header">
        <div className="animate-fade-in">
          <div className="flex items-center gap-2 mb-1">
            <Church className="w-5 h-5 text-primary-500" />
            <span className="text-sm font-medium text-primary-600 dark:text-primary-400 uppercase tracking-wider">
              {getGreeting()}, {user?.firstName}
            </span>
          </div>
          <h1 className="page-title">
            Centre de Pilotage{' '}
            <span className="text-gradient font-display">Pasteur</span>
          </h1>
          <p className="page-subtitle">
            Vision 360° de l'église · {new Date().toLocaleDateString('fr-FR', {
              weekday: 'long', day: 'numeric', month: 'long', year: 'numeric'
            })}
          </p>
        </div>
        <div className="flex items-center gap-2">
          <button
            onClick={() => exportReport({ endpoint: '/reports/export/consolidated-pdf', filename: `rapport-pasteur-${new Date().toISOString().split('T')[0]}.html` })}
            disabled={isExporting}
            className="btn-glow btn-sm animate-scale-in"
          >
            {isExporting ? <Loader2 className="w-4 h-4 animate-spin" /> : <FileDown className="w-4 h-4" />}
            {isExporting ? 'Génération...' : 'Exporter'}
          </button>
        </div>
      </div>

      {/* Tab navigation */}
      <div className="mb-6 -mx-4 px-4 overflow-x-auto scrollbar-hide">
        <div className="flex items-center gap-1 pb-2 min-w-max">
          {TABS.map(tab => {
            const Icon = tab.icon;
            const isActive = activeTab === tab.id;
            return (
              <button
                key={tab.id}
                type="button"
                onClick={() => onNavigateToTab(tab.id)}
                className={`flex items-center gap-1.5 px-3 py-2 rounded-xl text-xs font-medium transition-all duration-200 whitespace-nowrap ${
                  isActive
                    ? 'bg-primary-500/10 text-primary-600 dark:text-primary-400 shadow-sm'
                    : 'text-gray-500 hover:text-gray-700 dark:hover:text-gray-300 hover:bg-gray-100 dark:hover:bg-gray-800/50'
                }`}
              >
                <Icon className={`w-3.5 h-3.5 ${isActive ? tab.color : ''}`} />
                {tab.label}
              </button>
            );
          })}
        </div>
      </div>

      {/* Tab content */}
      {renderTab()}
    </div>
  );
}
