import { useState, useEffect } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import api, { getErrorMessage } from '@/lib/api';
import { useAuth } from '@/contexts/AuthContext';
import { useExportReport } from '@/hooks/useExportReport';
import { useDictionaries } from '@/hooks/useDictionaries';
import AttachmentPicker from '@/components/shared/AttachmentPicker';
import AttachmentLinks from '@/components/shared/AttachmentLinks';
import type { FamilyReport, Family, MakerReport } from '@/types';
import {
  FileText, Send, Loader2, CheckCircle2, AlertCircle, FileDown,
  Sparkles, Users, BarChart3, Clock, ChevronDown, Paperclip,
  type LucideIcon,
} from 'lucide-react';
import toast from 'react-hot-toast';

/** Replis (dictionnaires indisponibles) — les valeurs réelles viennent de la base. */
const STATUT_FALLBACK: Record<string, string> = {
  BROUILLON: 'Brouillon',
  SOUMIS: 'Soumis ✓',
  VU_PAR_RESPONSABLE: 'Vu responsable',
  VU_PAR_PASTEUR: 'Vu pasteur',
};

export default function FamilyReportPage() {
  const { user } = useAuth();
  const dictionaries = useDictionaries();
  const queryClient = useQueryClient();
  const { exportReport, isExporting } = useExportReport();
  const semaine = new Date().toISOString().split('T')[0];

  const [selectedFamilyId, setSelectedFamilyId] = useState('');
  const [commentaire, setCommentaire] = useState('');
  const [fichierIds, setFichierIds] = useState<string[]>([]);

  const { data: families } = useQuery({
    queryKey: ['families', 'managed'],
    queryFn: async () => {
      if (user?.estChefDeFamille) {
        const res = await api.get(`/families?chefFamilleId=${user.id}&size=100`);
        return res.data.content as Family[];
      }
      const res = await api.get('/families?size=100');
      return res.data.content as Family[];
    },
    enabled: !!user,
  });

  const { data: familyReport } = useQuery({
    queryKey: ['family-report', 'current', selectedFamilyId, semaine],
    queryFn: async () => {
      const res = await api.get(`/reports/family-weekly/${selectedFamilyId}?semaine=${semaine}`);
      const reports = res.data as FamilyReport[];
      return reports.length > 0 ? reports[0] : null;
    },
    enabled: !!selectedFamilyId,
  });

  const { data: makerReports } = useQuery({
    queryKey: ['maker-reports', 'family', selectedFamilyId, semaine],
    queryFn: async () => {
      const res = await api.get(`/reports/maker-weekly?familleId=${selectedFamilyId}&semaine=${semaine}&size=100`);
      return res.data.content as MakerReport[];
    },
    enabled: !!selectedFamilyId,
  });

  // Recharge les pièces jointes existantes quand le rapport de famille change.
  useEffect(() => {
    setFichierIds(familyReport?.piecesJointes?.map(a => a.fileId) ?? []);
  }, [familyReport]);

  const submitMutation = useMutation({
    mutationFn: async () => {
      await api.post('/reports/family-weekly', {
        familleId: selectedFamilyId,
        chefFamilleId: user?.id,
        semaine,
        commentaireSynthese: commentaire,
        fichierIds: fichierIds,
      });
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['family-report'] });
      toast.success('Rapport de famille soumis avec succès');
    },
    onError: (err) => toast.error(getErrorMessage(err)),
  });

  const totalPresents = makerReports?.filter((r) =>
    r.presencesParCulte && Object.values(r.presencesParCulte).some(Boolean)
  ).length || 0;
  const totalSoumis = makerReports?.filter((r) => r.soumis).length || 0;
  const totalAmes = makerReports?.length || 0;

  /** Couleur du statut depuis le dictionnaire REPORT_STATUS (repli local sinon). */
  const getStatutColor = (statut?: string) =>
    dictionaries.color('REPORT_STATUS', statut);

  const getStatutStyle = (statut?: string) => {
    if (dictionaries.color('REPORT_STATUS', statut)) return undefined;
    switch (statut) {
      case 'SOUMIS': return 'text-green-600';
      case 'VU_PAR_PASTEUR': return 'text-blue-600';
      case 'BROUILLON': return 'text-amber-600';
      default: return 'text-gray-400';
    }
  };

  const getStatutLabel = (statut?: string) => {
    if (statut && dictionaries.label('REPORT_STATUS', statut)) {
      // Libellé configuré par l'admin — la coche reste dans le repli local uniquement.
      return dictionaries.label('REPORT_STATUS', statut);
    }
    switch (statut) {
      case 'BROUILLON': return STATUT_FALLBACK.BROUILLON;
      case 'SOUMIS': return STATUT_FALLBACK.SOUMIS;
      case 'VU_PAR_RESPONSABLE': return STATUT_FALLBACK.VU_PAR_RESPONSABLE;
      case 'VU_PAR_PASTEUR': return STATUT_FALLBACK.VU_PAR_PASTEUR;
      default: return 'Non créé';
    }
  };

  const selectedFamily = families?.find(f => f.id === selectedFamilyId);

  /** Cartes de statistiques (couleur du statut pilotée par le dictionnaire). */
  const stats: Array<{
    label: string;
    value: string | number;
    icon: LucideIcon;
    gradient: string;
    valueClass?: string;
    valueColor?: string;
  }> = [
    { label: 'Âmes dans la famille', value: totalAmes, icon: Users, gradient: 'from-primary-500 to-primary-600' },
    { label: 'Rapports soumis', value: `${totalSoumis}/${totalAmes}`, icon: FileText, gradient: totalSoumis === totalAmes ? 'from-green-500 to-emerald-500' : 'from-amber-500 to-orange-500' },
    { label: 'Présents', value: totalPresents, icon: BarChart3, gradient: 'from-blue-500 to-indigo-500' },
    { label: 'État du rapport', value: getStatutLabel(familyReport?.statutValidation), icon: CheckCircle2, gradient: 'from-violet-500 to-purple-500', valueClass: getStatutStyle(familyReport?.statutValidation), valueColor: getStatutColor(familyReport?.statutValidation) },
  ];

  return (
    <div className="page-container">
      <div className="page-header">
        <div className="animate-fade-in">
          <div className="flex items-center gap-2 mb-1">
            <Users className="w-5 h-5 text-primary-500" />
            <h1 className="page-title">Rapport de famille</h1>
          </div>
          <p className="page-subtitle">
            <Clock className="w-3.5 h-3.5 inline mr-1" />
            Semaine du {new Date(semaine).toLocaleDateString('fr-FR', { day: 'numeric', month: 'long', year: 'numeric' })}
          </p>
        </div>
      </div>

      {/* Family selector */}
      <div className="glass-card p-4 mb-6 animate-slide-up">
        <label className="label flex items-center gap-1.5">
          <Users className="w-4 h-4 text-gray-400" />
          Sélectionner une famille
        </label>
        <select value={selectedFamilyId} onChange={(e) => setSelectedFamilyId(e.target.value)} className="input mt-1">
          <option value="">Choisir une famille...</option>
          {families?.map((f) => (
            <option key={f.id} value={f.id}>{f.nom}</option>
          ))}
        </select>
      </div>

      {selectedFamilyId && (
        <>
          {/* Stats cards */}
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4 mb-6">
            {stats.map((stat, i) => (
              <div key={stat.label} className="stat-card animate-slide-up" style={{ animationDelay: `${i * 80}ms` }}>
                <div className={`absolute top-0 left-0 right-0 h-1 bg-gradient-to-r ${stat.gradient} opacity-60`} />
                <div className="flex items-start justify-between mb-2">
                  <span className="stat-label">{stat.label}</span>
                  <div className={`p-2 rounded-xl bg-gradient-to-br ${stat.gradient} text-white shadow-sm`}>
                    <stat.icon className="w-4 h-4" />
                  </div>
                </div>
                <p className={`stat-value text-xl sm:text-2xl ${stat.valueClass || ''}`} style={stat.valueColor ? { color: stat.valueColor } : undefined}>{stat.value}</p>
                {stat.label === 'Rapports soumis' && (
                  <div className="progress-bar mt-3">
                    <div className="progress-bar-fill" style={{ width: `${totalAmes > 0 ? (totalSoumis / totalAmes) * 100 : 0}%` }} />
                  </div>
                )}
              </div>
            ))}
          </div>

          {/* Reports table */}
          {makerReports && makerReports.length > 0 && (
            <div className="glass-card mb-6 animate-slide-up" style={{ animationDelay: '200ms' }}>
              <div className="px-5 py-4 border-b border-white/20 dark:border-white/[0.06]">
                <h3 className="text-base font-semibold text-gray-900 dark:text-gray-100 flex items-center gap-2">
                  <FileText className="w-4 h-4 text-primary-500" />
                  Rapports des faiseurs
                </h3>
              </div>
              <div className="p-5">
                <div className="table-container overflow-hidden">
                  <table className="table">
                    <thead>
                      <tr>
                        <th>Âme</th>
                        <th>Statut</th>
                        <th>Présences</th>
                        <th>Difficultés</th>
                      </tr>
                    </thead>
                    <tbody>
                      {makerReports.map((report, i) => (
                        <tr key={report.id} className="animate-fade-in" style={{ animationDelay: `${i * 30}ms` }}>
                          <td className="font-medium">{report.ameId.slice(0, 12)}...</td>
                          <td>
                            {report.soumis ? (
                              <span className="badge-success text-xs"><CheckCircle2 className="w-3 h-3 mr-1" /> Soumis</span>
                            ) : (
                              <span className="badge-warning text-xs"><AlertCircle className="w-3 h-3 mr-1" /> Brouillon</span>
                            )}
                          </td>
                          <td>
                            {report.presencesParCulte && (
                              <div className="flex flex-wrap gap-1">
                                {Object.entries(report.presencesParCulte).map(([culte, present]) => (
                                  <span key={culte} className={`inline-flex items-center px-1.5 py-0.5 rounded text-[10px] font-medium ${
                                    present ? 'bg-green-100 dark:bg-green-900/30 text-green-700' : 'bg-red-100 dark:bg-red-900/30 text-red-700'
                                  }`}>
                                    {present ? '✓' : '✗'} {culte.split(' ')[0]}
                                  </span>
                                ))}
                              </div>
                            )}
                          </td>
                          <td className="max-w-[150px] truncate text-sm text-gray-500">{report.difficultes || '-'}</td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              </div>
            </div>
          )}

          {/* Synthesis comment */}
          <div className="glass-card p-5 mb-6 animate-slide-up" style={{ animationDelay: '250ms' }}>
            <h3 className="text-base font-semibold text-gray-900 dark:text-gray-100 mb-3 flex items-center gap-2">
              <Sparkles className="w-4 h-4 text-amber-500" />
              Commentaire de synthèse
            </h3>
            <textarea value={familyReport?.commentaireSynthese || commentaire}
              onChange={(e) => setCommentaire(e.target.value)}
              className="input" rows={4}
              placeholder="Appréciation qualitative globale de l'état de la famille..." />
            <div className="mt-3">
              <label className="label flex items-center gap-1.5">
                <Paperclip className="w-3.5 h-3.5 text-primary-500" /> Pièces jointes
              </label>
              {familyReport?.statutValidation && ['SOUMIS', 'VU_PAR_RESPONSABLE', 'VU_PAR_PASTEUR'].includes(familyReport.statutValidation) ? (
                <AttachmentLinks pieces={familyReport?.piecesJointes} />
              ) : (
                <AttachmentPicker value={fichierIds} onChange={setFichierIds} />
              )}
            </div>
          </div>

          {/* Actions */}
          <div className="glass-card p-5 animate-slide-up" style={{ animationDelay: '300ms' }}>
            <div className="flex flex-col sm:flex-row justify-between items-stretch sm:items-center gap-3">
              <button onClick={() => exportReport({
                  endpoint: `/reports/export/consolidated-pdf?familleId=${selectedFamilyId}`,
                  filename: `rapport-famille-${selectedFamilyId.slice(0, 8)}-${semaine}.html`,
                })}
                disabled={isExporting}
                className="btn-secondary btn-sm">
                {isExporting ? <Loader2 className="w-4 h-4 animate-spin" /> : <FileDown className="w-4 h-4" />}
                Exporter PDF
              </button>
              <button onClick={() => submitMutation.mutate()}
                disabled={submitMutation.isPending || familyReport?.statutValidation === 'SOUMIS'}
                className="btn-primary btn-sm">
                {submitMutation.isPending ? <Loader2 className="w-4 h-4 animate-spin" /> : <Send className="w-4 h-4" />}
                {familyReport?.statutValidation === 'SOUMIS' ? 'Déjà soumis ✓' : 'Soumettre le rapport de famille'}
              </button>
            </div>
          </div>
        </>
      )}
    </div>
  );
}
