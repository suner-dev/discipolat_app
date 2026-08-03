import { Link } from 'react-router-dom';
import { useAuth } from '@/contexts/AuthContext';
import api from '@/lib/api';
import { useExportReport } from '@/hooks/useExportReport';
import {
  FileText,
  Users,
  FileSpreadsheet,
  ArrowRight,
  Download,
  FileDown,
  Loader2,
  BarChart3,
  Sparkles,
  ChevronRight,
} from 'lucide-react';
import toast from 'react-hot-toast';

export default function ReportsPage() {
  const { user } = useAuth();
  const { exportReport, isExporting } = useExportReport();

  const sections = [
    {
      title: 'Rapport du faiseur',
      description: 'Saisir le rapport hebdomadaire pour chaque disciple suivi. Présence par culte, difficultés, mouvements.',
      icon: FileSpreadsheet,
      link: '/reports/maker',
      gradient: 'from-emerald-500 to-teal-500',
      roles: ['ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR'],
      stats: 'Hebdomadaire',
    },
    {
      title: 'Rapport de famille',
      description: 'Consulter ou soumettre le rapport consolidé de la famille. Statistiques agrégées, synthèse, validation.',
      icon: Users,
      link: '/reports/family',
      gradient: 'from-blue-500 to-indigo-500',
      roles: ['ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR'],
      stats: 'Consolidé',
    },
  ];

  const filteredSections = sections.filter((s) => user && s.roles.includes(user.role));

  const handleExport = async (type: 'maker' | 'family') => {
    try {
      const response = await api.get(`/reports/export/${type}-weekly`, {
        responseType: 'blob',
      });
      const url = window.URL.createObjectURL(new Blob([response.data]));
      const link = document.createElement('a');
      link.href = url;
      link.setAttribute('download', `rapports-${type}-${new Date().toISOString().split('T')[0]}.csv`);
      document.body.appendChild(link);
      link.click();
      link.remove();
      window.URL.revokeObjectURL(url);
      toast.success('Export téléchargé avec succès');
    } catch (error) {
      toast.error("Erreur lors de l'export");
    }
  };

  return (
    <div className="page-container">
      {/* Header */}
      <div className="page-header">
        <div className="animate-fade-in">
          <div className="flex items-center gap-2 mb-1">
            <FileText className="w-5 h-5 text-primary-500" />
            <h1 className="page-title">Rapports</h1>
          </div>
          <p className="page-subtitle">Gestion des rapports hebdomadaires</p>
        </div>
        {user?.role === 'PASTEUR' && (
          <div className="flex flex-wrap gap-2 animate-fade-in">
            <button onClick={() => handleExport('maker')} className="btn-secondary btn-sm">
              <Download className="w-4 h-4" /> CSV faiseur
            </button>
            <button onClick={() => handleExport('family')} className="btn-secondary btn-sm">
              <Download className="w-4 h-4" /> CSV famille
            </button>
            <button
              onClick={() => exportReport({
                endpoint: '/reports/export/consolidated-pdf',
                filename: `rapport-consolide-${new Date().toISOString().split('T')[0]}.html`,
              })}
              disabled={isExporting}
              className="btn-glow btn-sm"
            >
              {isExporting ? <Loader2 className="w-4 h-4 animate-spin" /> : <FileDown className="w-4 h-4" />}
              PDF consolidé
            </button>
          </div>
        )}
      </div>

      {/* Stats overview */}
      <div className="grid grid-cols-1 sm:grid-cols-3 gap-4 mb-8 animate-slide-up">
        <div className="glass-card p-4">
          <div className="flex items-center gap-3">
            <div className="p-2.5 rounded-xl bg-emerald-100 dark:bg-emerald-900/30">
              <FileSpreadsheet className="w-5 h-5 text-emerald-600 dark:text-emerald-400" />
            </div>
            <div>
              <p className="text-2xl font-bold text-gray-900 dark:text-gray-100 font-mono">24</p>
              <p className="text-xs text-gray-500 dark:text-gray-400">Rapports cette semaine</p>
            </div>
          </div>
        </div>
        <div className="glass-card p-4">
          <div className="flex items-center gap-3">
            <div className="p-2.5 rounded-xl bg-blue-100 dark:bg-blue-900/30">
              <BarChart3 className="w-5 h-5 text-blue-600 dark:text-blue-400" />
            </div>
            <div>
              <p className="text-2xl font-bold text-gray-900 dark:text-gray-100 font-mono">87%</p>
              <p className="text-xs text-gray-500 dark:text-gray-400">Taux de soumission</p>
            </div>
          </div>
        </div>
        <div className="glass-card p-4">
          <div className="flex items-center gap-3">
            <div className="p-2.5 rounded-xl bg-amber-100 dark:bg-amber-900/30">
              <Sparkles className="w-5 h-5 text-amber-600 dark:text-amber-400" />
            </div>
            <div>
              <p className="text-2xl font-bold text-gray-900 dark:text-gray-100 font-mono">3</p>
              <p className="text-xs text-gray-500 dark:text-gray-400">En attente</p>
            </div>
          </div>
        </div>
      </div>

      {/* Navigation cards */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
        {filteredSections.map((section, i) => {
          const Icon = section.icon;
          return (
            <Link
              key={section.link}
              to={section.link}
              className="group glass-card p-6 animate-slide-up hover-lift"
              style={{ animationDelay: `${i * 100}ms` }}
            >
              {/* Gradient top accent */}
              <div className={`absolute top-0 left-0 right-0 h-1 bg-gradient-to-r ${section.gradient} opacity-0 group-hover:opacity-100 transition-opacity duration-300 rounded-t-2xl`} />

              <div className="flex items-start gap-5">
                <div className={`p-3.5 rounded-xl bg-gradient-to-br ${section.gradient} text-white shadow-lg group-hover:shadow-xl transition-all duration-300 group-hover:scale-105`}>
                  <Icon className="w-6 h-6" />
                </div>
                <div className="flex-1 min-w-0">
                  <div className="flex items-center justify-between">
                    <h3 className="text-lg font-semibold text-gray-900 dark:text-gray-100 group-hover:text-primary-600 dark:group-hover:text-primary-400 transition-colors">
                      {section.title}
                    </h3>
                    <ChevronRight className="w-5 h-5 text-gray-300 dark:text-gray-600 group-hover:text-primary-500 group-hover:translate-x-1 transition-all" />
                  </div>
                  <p className="mt-2 text-sm text-gray-500 dark:text-gray-400 leading-relaxed">
                    {section.description}
                  </p>
                  <div className="flex items-center gap-2 mt-3">
                    <span className="badge-info text-[10px]">{section.stats}</span>
                    <span className="text-xs text-gray-400">•</span>
                    <span className="text-xs text-gray-400">Mise à jour en temps réel</span>
                  </div>
                </div>
              </div>
            </Link>
          );
        })}
      </div>

      {/* Quick actions */}
      <div className="glass-card p-6 mt-8 animate-slide-up">
        <h3 className="text-sm font-semibold text-gray-900 dark:text-gray-100 mb-3">Actions rapides</h3>
        <div className="flex flex-wrap gap-2">
          <button onClick={() => handleExport('maker')} className="btn-secondary btn-sm">
            <Download className="w-4 h-4" /> Exporter tous les rapports faiseur
          </button>
          <button onClick={() => handleExport('family')} className="btn-secondary btn-sm">
            <Download className="w-4 h-4" /> Exporter rapports famille
          </button>
        </div>
      </div>
    </div>
  );
}
