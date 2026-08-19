import { useState } from 'react';
import { Link } from 'react-router-dom';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import api from '@/lib/api';
import { useExportReport } from '@/hooks/useExportReport';
import type { PasteurDashboardData, PageResponse } from '@/types';
import {
  FileText, Eye, ChevronRight, CheckCircle, Clock, AlertTriangle,
  BarChart3, FileDown, Users, Plus, Search, Filter, X,
  Calendar, ArrowLeft, Loader2, Send, CheckCircle2,
} from 'lucide-react';
import {
  PieChart, Pie, Cell, Tooltip, ResponsiveContainer,
} from 'recharts';
import toast from 'react-hot-toast';

interface MakerReportItem {
  id: string; semaine: string; faiseurId: string; faiseurNom?: string;
  familleId?: string; familleNom?: string; statut?: string;
  presencesParCulte?: Record<string, boolean>; notesComplementaires?: string;
  nbPresents?: number; nbCultes?: number; createdAt: string;
}

interface FamilyReportItem {
  id: string; semaine: string; familleId: string; familleNom?: string;
  chefFamilleId?: string; chefFamilleNom?: string; statut?: string;
  nombreAmes?: string; nombrePresents?: string; themesAbordes?: string;
  envSpirituel?: string; createdAt: string;
}

type ReportTab = 'resume' | 'faiseur' | 'famille';

export default function PasteurReportsTab() {
  const queryClient = useQueryClient();
  const { exportReport, isExporting } = useExportReport();
  const [activeTab, setActiveTab] = useState<ReportTab>('resume');
  const [page, setPage] = useState(0);
  const [search, setSearch] = useState('');
  const [weekFilter, setWeekFilter] = useState('');
  const [showDetail, setShowDetail] = useState<MakerReportItem | FamilyReportItem | null>(null);

  // Dashboard stats
  const { data: dashboard } = useQuery({
    queryKey: ['dashboard', 'pasteur'],
    queryFn: async () => {
      const res = await api.get('/dashboard/pasteur');
      return res.data as PasteurDashboardData;
    },
  });

  // Maker reports list
  const { data: makerData, isLoading: makerLoading } = useQuery({
    queryKey: ['reports', 'maker', page, search, weekFilter],
    queryFn: async () => {
      const params = new URLSearchParams({ size: '20', page: String(page) });
      if (weekFilter) params.set('semaine', weekFilter);
      const res = await api.get(`/reports/maker-weekly?${params}`);
      return res.data as PageResponse<MakerReportItem>;
    },
    enabled: activeTab === 'faiseur',
  });

  // Family reports list
  const { data: familyData, isLoading: familyLoading } = useQuery({
    queryKey: ['reports', 'family', page, search, weekFilter],
    queryFn: async () => {
      const params = new URLSearchParams({ size: '20', page: String(page) });
      if (weekFilter) params.set('semaine', weekFilter);
      const res = await api.get(`/reports/family-weekly?${params}`);
      return res.data as PageResponse<FamilyReportItem>;
    },
    enabled: activeTab === 'famille',
  });

  // Validate family report
  const validateMutation = useMutation({
    mutationFn: async ({ id, type }: { id: string; type: string }) => {
      await api.patch(`/reports/family-weekly/${id}/validate`, { validationType: type });
    },
    onSuccess: () => { queryClient.invalidateQueries({ queryKey: ['reports'] }); toast.success('Rapport validé'); },
    onError: () => toast.error('Erreur lors de la validation'),
  });

  const rapports = dashboard?.rapports ?? { soumis: 0, enAttente: 0, tauxCompletion: 0, totalFaiseurs: 0, faiseursAyantRapporte: 0 };
  const completion = rapports.tauxCompletion ?? 0;

  const reportTypeBadge = (s?: string) => {
    if (s === 'SOUMIS' || s === 'VALIDE') return <span className="badge-success text-[10px]"><CheckCircle2 className="w-3 h-3 inline mr-1" />{s}</span>;
    if (s === 'BROUILLON' || s === 'EN_ATTENTE') return <span className="badge-warning text-[10px]"><Clock className="w-3 h-3 inline mr-1" />{s?.replace(/_/g, ' ')}</span>;
    return <span className="badge-info text-[10px]">{s || '—'}</span>;
  };

  const tabs = [
    { id: 'resume' as ReportTab, label: 'Résumé', icon: BarChart3 },
    { id: 'faiseur' as ReportTab, label: 'Rapports Faiseur', icon: Users },
    { id: 'famille' as ReportTab, label: 'Rapports Famille', icon: FileText },
  ];

  return (
    <div className="animate-slide-up">
      <div className="flex items-center justify-between mb-4">
        <div className="flex items-center gap-2">
          <FileText className="w-5 h-5 text-emerald-500" />
          <h2 className="text-lg font-semibold text-gray-900 dark:text-gray-100">Rapports</h2>
        </div>
        <div className="flex gap-2">
          <button onClick={() => exportReport({ endpoint: '/reports/export/consolidated-pdf', filename: `rapport-${new Date().toISOString().split('T')[0]}.html` })} disabled={isExporting} className="btn-secondary btn-sm">
            {isExporting ? <Loader2 className="w-4 h-4 animate-spin" /> : <FileDown className="w-4 h-4" />} Exporter
          </button>
          <Link to="/reports" className="btn-primary btn-sm"><Eye className="w-4 h-4" /> Page complète</Link>
        </div>
      </div>

      {/* Sub-tabs */}
      <div className="flex items-center gap-1 mb-4 bg-gray-100 dark:bg-gray-800/50 rounded-xl p-1">
        {tabs.map(t => {
          const Icon = t.icon;
          return (
            <button key={t.id} onClick={() => { setActiveTab(t.id); setPage(0); setSearch(''); }}
              className={`flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-xs font-medium transition-all ${activeTab === t.id ? 'bg-white dark:bg-gray-700 shadow-sm text-gray-900 dark:text-gray-100' : 'text-gray-500 hover:text-gray-700'}`}>
              <Icon className="w-3.5 h-3.5" /> {t.label}
            </button>
          );
        })}
      </div>

      {/* === RESUME TAB === */}
      {activeTab === 'resume' && (
        <>
          <div className="grid grid-cols-1 md:grid-cols-3 gap-4 mb-6">
            <div className="glass-card p-6 text-center">
              <div className="inline-flex p-3 rounded-2xl bg-green-100 dark:bg-green-900/20 mb-3">
                <CheckCircle className="w-8 h-8 text-green-500" />
              </div>
              <p className="text-3xl font-bold text-gray-900 dark:text-gray-100">{rapports.soumis}</p>
              <p className="text-xs text-gray-400 mt-1">Rapports soumis</p>
            </div>
            <div className="glass-card p-6 text-center">
              <div className="inline-flex p-3 rounded-2xl bg-amber-100 dark:bg-amber-900/20 mb-3">
                <Clock className="w-8 h-8 text-amber-500" />
              </div>
              <p className="text-3xl font-bold text-gray-900 dark:text-gray-100">{rapports.enAttente}</p>
              <p className="text-xs text-gray-400 mt-1">En attente</p>
            </div>
            <div className="glass-card p-6 text-center">
              <div className="inline-flex p-3 rounded-2xl bg-blue-100 dark:bg-blue-900/20 mb-3">
                <BarChart3 className="w-8 h-8 text-blue-500" />
              </div>
              <p className="text-3xl font-bold text-gray-900 dark:text-gray-100">{completion}%</p>
              <p className="text-xs text-gray-400 mt-1">Taux de complétion</p>
            </div>
          </div>

          {/* Progression */}
          <div className="glass-card p-6 mb-6">
            <h3 className="text-sm font-semibold text-gray-700 dark:text-gray-300 mb-4">Progression des rapports</h3>
            <div className="flex items-center gap-3 mb-2">
              <div className="flex-1 bg-gray-200 dark:bg-gray-700 rounded-full h-3">
                <div className="h-3 rounded-full bg-gradient-to-r from-emerald-500 to-green-500 transition-all duration-500" style={{ width: `${Math.min(completion, 100)}%` }} />
              </div>
              <span className="text-sm font-bold text-gray-600 dark:text-gray-400">{completion}%</span>
            </div>
            <p className="text-xs text-gray-400">{rapports.faiseursAyantRapporte} / {rapports.totalFaiseurs} faiseurs ont rapporté cette semaine</p>
          </div>

          {/* Pie chart */}
          {(rapports.soumis + rapports.enAttente) > 0 && (
            <div className="glass-card p-6 mb-6">
              <h3 className="text-sm font-semibold text-gray-700 dark:text-gray-300 mb-4">Répartition</h3>
              <div className="h-48">
                <ResponsiveContainer width="100%" height="100%">
                  <PieChart>
                    <Pie data={[
                      { name: 'Soumis', value: rapports.soumis, fill: '#22c55e' },
                      { name: 'En attente', value: rapports.enAttente, fill: '#f59e0b' },
                    ].filter(d => d.value > 0)} cx="50%" cy="50%" innerRadius={40} outerRadius={70} paddingAngle={3} dataKey="value" strokeWidth={0} />
                    <Tooltip formatter={(v: number, name: string) => [v, name]} contentStyle={{ fontSize: 11, borderRadius: 8 }} />
                  </PieChart>
                </ResponsiveContainer>
              </div>
            </div>
          )}

          {/* Liens rapides */}
          <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
            <button onClick={() => setActiveTab('faiseur')} className="glass-card p-4 hover:shadow-lg hover:-translate-y-0.5 transition-all group cursor-pointer text-left">
              <div className="flex items-center gap-3">
                <Users className="w-5 h-5 text-emerald-500" />
                <div>
                  <p className="text-sm font-semibold text-gray-900 dark:text-gray-100">Rapports faiseur</p>
                  <p className="text-[10px] text-gray-400">{rapports.soumis + rapports.enAttente} rapports</p>
                </div>
                <ChevronRight className="w-4 h-4 text-gray-400 ml-auto opacity-0 group-hover:opacity-100 transition-opacity" />
              </div>
            </button>
            <button onClick={() => setActiveTab('famille')} className="glass-card p-4 hover:shadow-lg hover:-translate-y-0.5 transition-all group cursor-pointer text-left">
              <div className="flex items-center gap-3">
                <FileText className="w-5 h-5 text-blue-500" />
                <div>
                  <p className="text-sm font-semibold text-gray-900 dark:text-gray-100">Rapports famille</p>
                  <p className="text-[10px] text-gray-400">Synthèse familiale</p>
                </div>
                <ChevronRight className="w-4 h-4 text-gray-400 ml-auto opacity-0 group-hover:opacity-100 transition-opacity" />
              </div>
            </button>
            <Link to="/reports/urgent-aid" className="glass-card p-4 hover:shadow-lg hover:-translate-y-0.5 transition-all group">
              <div className="flex items-center gap-3">
                <AlertTriangle className="w-5 h-5 text-red-500" />
                <div>
                  <p className="text-sm font-semibold text-gray-900 dark:text-gray-100">Aide urgente</p>
                  <p className="text-[10px] text-gray-400">Signalement d'urgence</p>
                </div>
                <ChevronRight className="w-4 h-4 text-gray-400 ml-auto opacity-0 group-hover:opacity-100 transition-opacity" />
              </div>
            </Link>
          </div>
        </>
      )}

      {/* === FAISEUR REPORTS TAB === */}
      {activeTab === 'faiseur' && (
        <>
          <div className="glass-card p-4 mb-4">
            <div className="flex flex-col sm:flex-row gap-3">
              <div className="flex-1 relative">
                <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400" />
                <input type="text" placeholder="Rechercher un rapport..." value={search} onChange={e => { setSearch(e.target.value); setPage(0); }} className="input pl-10" />
              </div>
              <input type="week" className="input w-auto" value={weekFilter} onChange={e => { setWeekFilter(e.target.value); setPage(0); }} title="Filtrer par semaine" />
            </div>
          </div>

          {makerLoading ? (
            <div className="space-y-3">{[...Array(5)].map((_, i) => <div key={i} className="glass-card p-4"><div className="skeleton h-12 w-full rounded-xl" /></div>)}</div>
          ) : (
            <div className="space-y-2">
              {(makerData?.content || []).map(r => (
                <div key={r.id} className="glass-card p-4 hover:bg-white/60 dark:hover:bg-gray-800/20 transition-colors cursor-pointer"
                  onClick={() => setShowDetail(r)}>
                  <div className="flex items-center justify-between">
                    <div className="flex items-center gap-3 min-w-0">
                      <div className="w-10 h-10 rounded-xl bg-emerald-50 dark:bg-emerald-900/20 flex items-center justify-center flex-shrink-0">
                        <FileText className="w-5 h-5 text-emerald-500" />
                      </div>
                      <div className="min-w-0">
                        <p className="text-sm font-medium text-gray-900 dark:text-gray-100">
                          {r.faiseurNom || 'Faiseur'} — {r.familleNom || ' Famille'}
                        </p>
                        <div className="flex items-center gap-2 text-[10px] text-gray-400 mt-0.5">
                          <span>Semaine: {r.semaine}</span>
                          {r.nbPresents !== undefined && <span>· {r.nbPresents} présents</span>}
                          {r.nbCultes !== undefined && <span>· {r.nbCultes} cultes</span>}
                          <span>· {new Date(r.createdAt).toLocaleDateString('fr-FR', { day: 'numeric', month: 'short' })}</span>
                        </div>
                      </div>
                    </div>
                    <div className="flex items-center gap-2 flex-shrink-0">
                      {reportTypeBadge(r.statut)}
                      <Eye className="w-4 h-4 text-gray-400" />
                    </div>
                  </div>
                </div>
              ))}
              {(!makerData?.content || makerData.content.length === 0) && (
                <div className="glass-card p-14 text-center">
                  <FileText className="w-10 h-10 text-gray-300 mx-auto mb-2" />
                  <p className="text-sm text-gray-400">Aucun rapport faiseur</p>
                </div>
              )}
            </div>
          )}

          {makerData && makerData.totalPages > 1 && (
            <div className="flex items-center justify-between mt-4">
              <p className="text-sm text-gray-500">Page {makerData.number + 1} / {makerData.totalPages}</p>
              <div className="flex gap-2">
                <button onClick={() => setPage(p => Math.max(0, p - 1))} disabled={makerData.first} className="btn-secondary btn-sm">← Précédent</button>
                <button onClick={() => setPage(p => p + 1)} disabled={makerData.last} className="btn-primary btn-sm">Suivant →</button>
              </div>
            </div>
          )}
        </>
      )}

      {/* === FAMILY REPORTS TAB === */}
      {activeTab === 'famille' && (
        <>
          <div className="glass-card p-4 mb-4">
            <div className="flex flex-col sm:flex-row gap-3">
              <div className="flex-1 relative">
                <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400" />
                <input type="text" placeholder="Rechercher un rapport famille..." value={search} onChange={e => { setSearch(e.target.value); setPage(0); }} className="input pl-10" />
              </div>
              <input type="week" className="input w-auto" value={weekFilter} onChange={e => { setWeekFilter(e.target.value); setPage(0); }} title="Filtrer par semaine" />
            </div>
          </div>

          {familyLoading ? (
            <div className="space-y-3">{[...Array(5)].map((_, i) => <div key={i} className="glass-card p-4"><div className="skeleton h-12 w-full rounded-xl" /></div>)}</div>
          ) : (
            <div className="space-y-2">
              {(familyData?.content || []).map(r => (
                <div key={r.id} className="glass-card p-4 hover:bg-white/60 dark:hover:bg-gray-800/20 transition-colors cursor-pointer"
                  onClick={() => setShowDetail(r)}>
                  <div className="flex items-center justify-between">
                    <div className="flex items-center gap-3 min-w-0">
                      <div className="w-10 h-10 rounded-xl bg-blue-50 dark:bg-blue-900/20 flex items-center justify-center flex-shrink-0">
                        <FileText className="w-5 h-5 text-blue-500" />
                      </div>
                      <div className="min-w-0">
                        <p className="text-sm font-medium text-gray-900 dark:text-gray-100">
                          {r.chefFamilleNom || r.familleNom || 'Famille'}
                        </p>
                        <div className="flex items-center gap-2 text-[10px] text-gray-400 mt-0.5">
                          <span>Semaine: {r.semaine}</span>
                          {r.nombreAmes && <span>· {r.nombreAmes} âmes</span>}
                          {r.nombrePresents && <span>· {r.nombrePresents} présents</span>}
                          <span>· {new Date(r.createdAt).toLocaleDateString('fr-FR', { day: 'numeric', month: 'short' })}</span>
                        </div>
                      </div>
                    </div>
                    <div className="flex items-center gap-2 flex-shrink-0">
                      {reportTypeBadge(r.statut)}
                      {r.statut !== 'VALIDE' && (
                        <button onClick={(e) => { e.stopPropagation(); validateMutation.mutate({ id: r.id, type: 'APPROUVE' }); }} className="btn-secondary btn-xs" title="Valider">
                          <CheckCircle2 className="w-3.5 h-3.5 text-green-500" />
                        </button>
                      )}
                      <Eye className="w-4 h-4 text-gray-400" />
                    </div>
                  </div>
                </div>
              ))}
              {(!familyData?.content || familyData.content.length === 0) && (
                <div className="glass-card p-14 text-center">
                  <FileText className="w-10 h-10 text-gray-300 mx-auto mb-2" />
                  <p className="text-sm text-gray-400">Aucun rapport famille</p>
                </div>
              )}
            </div>
          )}

          {familyData && familyData.totalPages > 1 && (
            <div className="flex items-center justify-between mt-4">
              <p className="text-sm text-gray-500">Page {familyData.number + 1} / {familyData.totalPages}</p>
              <div className="flex gap-2">
                <button onClick={() => setPage(p => Math.max(0, p - 1))} disabled={familyData.first} className="btn-secondary btn-sm">← Précédent</button>
                <button onClick={() => setPage(p => p + 1)} disabled={familyData.last} className="btn-primary btn-sm">Suivant →</button>
              </div>
            </div>
          )}
        </>
      )}

      {/* Detail modal */}
      {showDetail && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/50" onClick={() => setShowDetail(null)}>
          <div className="bg-white dark:bg-gray-800 rounded-xl p-6 max-w-lg w-full animate-slide-up max-h-[90vh] overflow-y-auto" onClick={e => e.stopPropagation()}>
            <div className="flex items-center justify-between mb-4">
              <h3 className="text-lg font-semibold">Détail du rapport</h3>
              <button onClick={() => setShowDetail(null)} className="p-1 rounded-lg hover:bg-gray-100 dark:hover:bg-gray-700"><X className="w-5 h-5" /></button>
            </div>
            <div className="space-y-3">
              {'semaine' in showDetail && (
                <div className="flex justify-between text-sm"><span className="text-gray-500">Semaine</span><span className="font-medium">{showDetail.semaine}</span></div>
              )}
              {'faiseurNom' in showDetail && (
                <div className="flex justify-between text-sm"><span className="text-gray-500">Faiseur</span><span className="font-medium">{(showDetail as MakerReportItem).faiseurNom || '—'}</span></div>
              )}
              {'familleNom' in showDetail && (
                <div className="flex justify-between text-sm"><span className="text-gray-500">Famille</span><span className="font-medium">{(showDetail as FamilyReportItem).familleNom || '—'}</span></div>
              )}
              {'statut' in showDetail && (
                <div className="flex justify-between text-sm"><span className="text-gray-500">Statut</span>{reportTypeBadge(showDetail.statut)}</div>
              )}
              {'notesComplementaires' in showDetail && (showDetail as MakerReportItem).notesComplementaires && (
                <div className="mt-2 p-3 rounded-xl bg-gray-50 dark:bg-gray-800/50">
                  <p className="text-xs font-medium text-gray-500 mb-1">Notes</p>
                  <p className="text-sm text-gray-700 dark:text-gray-300 whitespace-pre-wrap">{(showDetail as MakerReportItem).notesComplementaires}</p>
                </div>
              )}
              {'themesAbordes' in showDetail && (showDetail as FamilyReportItem).themesAbordes && (
                <div className="mt-2 p-3 rounded-xl bg-gray-50 dark:bg-gray-800/50">
                  <p className="text-xs font-medium text-gray-500 mb-1">Thèmes abordés</p>
                  <p className="text-sm text-gray-700 dark:text-gray-300 whitespace-pre-wrap">{(showDetail as FamilyReportItem).themesAbordes}</p>
                </div>
              )}
              {'envSpirituel' in showDetail && (showDetail as FamilyReportItem).envSpirituel && (
                <div className="mt-2 p-3 rounded-xl bg-gray-50 dark:bg-gray-800/50">
                  <p className="text-xs font-medium text-gray-500 mb-1">Environnement spirituel</p>
                  <p className="text-sm text-gray-700 dark:text-gray-300 whitespace-pre-wrap">{(showDetail as FamilyReportItem).envSpirituel}</p>
                </div>
              )}
              <div className="flex justify-between text-xs text-gray-400 mt-4 pt-3 border-t border-gray-200 dark:border-gray-700">
                <span>Créé le {new Date(showDetail.createdAt).toLocaleDateString('fr-FR', { day: 'numeric', month: 'long', year: 'numeric' })}</span>
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
