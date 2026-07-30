import { useState, useEffect } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import api, { getErrorMessage } from '@/lib/api';
import { useAuth } from '@/contexts/AuthContext';
import type { Soul, MakerReport, RaisonAbsence, MotifSortie } from '@/types';
import {
  FileText, Send, Loader2, CheckCircle2, AlertCircle, Save, Clock,
  Sparkles, Heart, UserCheck, XCircle,
} from 'lucide-react';
import toast from 'react-hot-toast';

const RAISON_ABSENCE_OPTIONS: { value: RaisonAbsence; label: string }[] = [
  { value: 'MALADIE', label: 'Maladie' },
  { value: 'VOYAGE', label: 'Voyage' },
  { value: 'INDISPONIBILITE', label: 'Indisponibilité' },
  { value: 'INJOIGNABLE', label: 'Injoignable' },
  { value: 'NON_RENSEIGNE', label: 'Non renseigné' },
  { value: 'AUTRE', label: 'Autre' },
];

const MOTIF_SORTIE_OPTIONS: { value: MotifSortie; label: string }[] = [
  { value: 'INTEGRE_AUTONOME', label: 'Intégré/autonome' },
  { value: 'TRANSFERT', label: 'Transfert' },
  { value: 'ABANDON', label: 'Abandon' },
  { value: 'INJOIGNABLE_DURABLE', label: 'Injoignable durable' },
  { value: 'DECES', label: 'Décès' },
  { value: 'AUTRE', label: 'Autre' },
];

const CULTES = ['Dimanche Matin', 'Mercredi Soir', 'Vendredi Soir'];
const DIFFICULTES_CATEGORIES = ['SPIRITUEL', 'FAMILIAL', 'FINANCIER', 'SANTE', 'AUTRE'];

interface ReportFormData {
  ameId: string;
  presencesParCulte: Record<string, boolean>;
  absenceRaison?: RaisonAbsence;
  absenceCommentaire?: string;
  difficultesCategorie?: string;
  difficultes?: string;
  nbSorties: number;
  motifSortie?: MotifSortie;
  notesComplementaires?: string;
}

export default function MakerReportPage() {
  const { user } = useAuth();
  const queryClient = useQueryClient();
  const semaine = new Date().toISOString().split('T')[0];

  const [reports, setReports] = useState<Record<string, ReportFormData>>({});
  const [lastSaved, setLastSaved] = useState<Date | null>(null);

  const { data: souls, isLoading } = useQuery({
    queryKey: ['souls', 'my'],
    queryFn: async () => {
      const res = await api.get(`/souls?faiseurId=${user?.id}&size=100`);
      return res.data.content as Soul[];
    },
    enabled: !!user?.id,
  });

  const prefillMutation = useMutation({
    mutationFn: async () => {
      const res = await api.get(`/reports/maker-weekly/prefill/${user?.id}`);
      return res.data as { ameId: string; nom: string; statut: string; etatSpirituel?: string; semaine: string; brouillonExistant: boolean; dejaSoumis: boolean }[];
    },
    onSuccess: (data) => {
      const newReports: Record<string, ReportFormData> = {};
      for (const entry of data) {
        newReports[entry.ameId] = {
          ameId: entry.ameId,
          presencesParCulte: Object.fromEntries(CULTES.map(c => [c, true])),
          nbSorties: 0,
          notesComplementaires: '',
        };
      }
      setReports(newReports);
      localStorage.setItem(DRAFT_KEY, JSON.stringify(newReports));
      toast.success(`Pré-remplissage effectué pour ${data.length} âme${data.length > 1 ? 's' : ''}`);
    },
    onError: (err) => toast.error(getErrorMessage(err)),
  });

  const { data: existingReports } = useQuery({
    queryKey: ['maker-reports', 'week', semaine],
    queryFn: async () => {
      const res = await api.get(`/reports/maker-weekly?semaine=${semaine}&faiseurId=${user?.id}`);
      return res.data.content as MakerReport[];
    },
    enabled: !!user?.id,
  });

  const submitMutation = useMutation({
    mutationFn: async (data: { faiseurId: string; ameId: string; semaine: string } & ReportFormData) => {
      await api.post('/reports/maker-weekly', data);
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['maker-reports'] });
      toast.success('Rapport soumis avec succès');
    },
    onError: (err) => toast.error(getErrorMessage(err)),
  });

  // LocalStorage draft (US-29)
  const DRAFT_KEY = `maker-report-draft-${semaine}`;

  const saveDraft = () => {
    try {
      localStorage.setItem(DRAFT_KEY, JSON.stringify(reports));
      setLastSaved(new Date());
      toast.success('Brouillon sauvegardé', { duration: 1500 });
    } catch {
      toast.error('Erreur lors de la sauvegarde du brouillon');
    }
  };

  // Load draft on mount
  useEffect(() => {
    try {
      const saved = localStorage.getItem(DRAFT_KEY);
      if (saved) {
        const parsed = JSON.parse(saved);
        setReports(parsed);
        setLastSaved(new Date());
      }
    } catch {}
  }, []);

  // Auto-save every 30s
  useEffect(() => {
    const interval = setInterval(() => {
      if (Object.keys(reports).length > 0) {
        localStorage.setItem(DRAFT_KEY, JSON.stringify(reports));
        setLastSaved(new Date());
      }
    }, 30000);
    return () => clearInterval(interval);
  }, [reports, DRAFT_KEY]);

  const initReport = (ameId: string): ReportFormData => {
    const existing = existingReports?.find((r) => r.ameId === ameId);
    if (existing) {
      return {
        ameId: existing.ameId,
        presencesParCulte: existing.presencesParCulte || Object.fromEntries(CULTES.map(c => [c, false])),
        absenceRaison: existing.absenceRaison,
        absenceCommentaire: existing.absenceCommentaire,
        difficultesCategorie: existing.difficultesCategorie,
        difficultes: existing.difficultes,
        nbSorties: existing.nbSorties || 0,
        motifSortie: existing.motifSortie,
        notesComplementaires: existing.notesComplementaires,
      };
    }
    return { ameId, presencesParCulte: Object.fromEntries(CULTES.map(c => [c, false])), nbSorties: 0 };
  };

  const getReport = (ameId: string): ReportFormData => {
    if (!reports[ameId]) setReports((prev) => ({ ...prev, [ameId]: initReport(ameId) }));
    return reports[ameId] || initReport(ameId);
  };

  const updateReport = (ameId: string, updates: Partial<ReportFormData>) => {
    setReports((prev) => ({ ...prev, [ameId]: { ...(prev[ameId] || initReport(ameId)), ...updates } }));
  };

  const handleSubmit = (ameId: string) => {
    if (!user) return;
    const report = getReport(ameId);
    submitMutation.mutate({ ...report, faiseurId: user.id, semaine });
  };

  const isSoumis = (ameId: string) => existingReports?.some((r) => r.ameId === ameId && r.soumis);

  if (isLoading) {
    return (
      <div className="page-container">
        <div className="space-y-4">
          {[1, 2, 3].map((i) => (
            <div key={i} className="glass-card p-6 animate-fade-in" style={{ animationDelay: `${i * 80}ms` }}>
              <div className="flex items-center gap-3 mb-4">
                <div className="skeleton w-10 h-10 rounded-xl" />
                <div><div className="skeleton h-5 w-40 mb-1" /><div className="skeleton h-3 w-24" /></div>
              </div>
              <div className="grid grid-cols-2 gap-4">
                <div className="skeleton h-24 rounded-xl" />
                <div className="skeleton h-24 rounded-xl" />
              </div>
            </div>
          ))}
        </div>
      </div>
    );
  }

  return (
    <div className="page-container">
      {/* Header */}
      <div className="page-header">
        <div className="animate-fade-in">
          <div className="flex items-center gap-2 mb-1">
            <FileText className="w-5 h-5 text-emerald-500" />
            <h1 className="page-title">Rapport hebdomadaire</h1>
          </div>
          <p className="page-subtitle flex items-center gap-2">
            <Clock className="w-3.5 h-3.5" />
            Semaine du {new Date(semaine).toLocaleDateString('fr-FR', { day: 'numeric', month: 'long', year: 'numeric' })}
            {lastSaved && (
              <span className="text-[10px] text-gray-400 ml-2">
                · Dernière sauvegarde : {lastSaved.toLocaleTimeString('fr-FR', { hour: '2-digit', minute: '2-digit' })}
              </span>
            )}
          </p>
        </div>
        <div className="flex gap-2 animate-fade-in">
          <button onClick={() => prefillMutation.mutate()} disabled={prefillMutation.isPending || !user?.id} className="btn-secondary btn-sm">
            {prefillMutation.isPending ? <Loader2 className="w-4 h-4 animate-spin" /> : <Sparkles className="w-4 h-4" />}
            Pré-remplir
          </button>
          <button onClick={saveDraft} disabled={!souls?.length} className="btn-secondary btn-sm">
            <Save className="w-4 h-4" /> Sauvegarder
          </button>
        </div>
      </div>

      {(!souls || souls.length === 0) ? (
        <div className="glass-card p-16 text-center animate-scale-in">
          <div className="inline-flex p-4 rounded-2xl bg-gray-100 dark:bg-gray-800/50 mb-4">
            <Heart className="w-10 h-10 text-gray-400" />
          </div>
          <h3 className="text-lg font-semibold text-gray-900 dark:text-gray-100 mb-1">Aucune âme assignée</h3>
          <p className="text-sm text-gray-500">Vous n'avez pas d'âmes à suivre pour le moment.</p>
        </div>
      ) : (
        <div className="space-y-5">
          {souls.map((soul, idx) => {
            const report = getReport(soul.id);
            const soumis = isSoumis(soul.id);

            return (
              <div key={soul.id}
                className="glass-card p-5 sm:p-6 animate-slide-up hover-lift"
                style={{ animationDelay: `${idx * 80}ms` }}
              >
                {/* Header */}
                <div className="flex items-start justify-between mb-5 pb-4 border-b border-white/20 dark:border-white/[0.06]">
                  <div className="flex items-center gap-3">
                    <div className={`w-10 h-10 rounded-xl flex items-center justify-center text-sm font-bold shadow-sm ${
                      soumis
                        ? 'bg-green-100 dark:bg-green-900/30 text-green-700 dark:text-green-400'
                        : 'bg-amber-100 dark:bg-amber-900/30 text-amber-700 dark:text-amber-400'
                    }`}>
                      {soul.prenom?.[0] || soul.nom[0]}
                    </div>
                    <div>
                      <h3 className="text-base font-semibold text-gray-900 dark:text-gray-100">
                        {soul.prenom} {soul.nom}
                      </h3>
                      <span className={soul.typeDisciple === 'NOUVEAU_CONVERTI' ? 'badge-success text-[10px]' : 'badge-info text-[10px]'}>
                        {soul.typeDisciple === 'NOUVEAU_CONVERTI' ? 'Nouveau converti' : 'Nouvel arrivant'}
                      </span>
                    </div>
                  </div>
                  <div className="flex items-center gap-2">
                    {soumis ? (
                      <span className="badge-success flex items-center gap-1 text-xs">
                        <CheckCircle2 className="w-3 h-3" /> Soumis
                      </span>
                    ) : (
                      <span className="badge-warning flex items-center gap-1 text-xs">
                        <AlertCircle className="w-3 h-3" /> En attente
                      </span>
                    )}
                  </div>
                </div>

                {/* Form content */}
                <div className="grid grid-cols-1 md:grid-cols-2 gap-5">
                  {/* Présence par culte */}
                  <div className="p-4 rounded-xl bg-white/30 dark:bg-gray-800/30">
                    <label className="label flex items-center gap-1.5">
                      <UserCheck className="w-3.5 h-3.5 text-primary-500" /> Présence par culte
                    </label>
                    <div className="space-y-2 mt-2">
                      {CULTES.map((culte) => (
                        <label key={culte}
                          className={`flex items-center gap-3 p-2.5 rounded-lg cursor-pointer transition-colors ${
                            report.presencesParCulte[culte]
                              ? 'bg-green-50 dark:bg-green-900/20 border border-green-200/50 dark:border-green-700/30'
                              : 'hover:bg-gray-50 dark:hover:bg-gray-800/50'
                          }`}>
                          <input type="checkbox" checked={report.presencesParCulte[culte]}
                            onChange={(e) => updateReport(soul.id, {
                              presencesParCulte: { ...report.presencesParCulte, [culte]: e.target.checked }
                            })}
                            className="rounded border-gray-300 text-primary-600 focus:ring-primary-500/30" />
                          <span className="text-sm text-gray-700 dark:text-gray-300">{culte}</span>
                          {report.presencesParCulte[culte] && <span className="ml-auto text-green-500 text-xs">✓</span>}
                        </label>
                      ))}
                    </div>
                  </div>

                  {/* Difficultés */}
                  <div className="p-4 rounded-xl bg-white/30 dark:bg-gray-800/30">
                    <label className="label flex items-center gap-1.5">
                      <AlertCircle className="w-3.5 h-3.5 text-amber-500" /> Difficultés / Challenges
                    </label>
                    <select value={report.difficultesCategorie || ''}
                      onChange={(e) => updateReport(soul.id, { difficultesCategorie: e.target.value })}
                      className="input text-sm mt-2">
                      <option value="">Catégorie...</option>
                      {DIFFICULTES_CATEGORIES.map((cat) => (
                        <option key={cat} value={cat}>{cat.charAt(0) + cat.slice(1).toLowerCase()}</option>
                      ))}
                    </select>
                    <textarea value={report.difficultes || ''}
                      onChange={(e) => updateReport(soul.id, { difficultes: e.target.value })}
                      className="input mt-2" rows={3} placeholder="Description des difficultés..." />
                  </div>

                  {/* Raison d'absence */}
                  <div className="p-4 rounded-xl bg-white/30 dark:bg-gray-800/30">
                    <label className="label flex items-center gap-1.5">
                      <XCircle className="w-3.5 h-3.5 text-red-500" /> Raison d'absence
                    </label>
                    <select value={report.absenceRaison || ''}
                      onChange={(e) => updateReport(soul.id, { absenceRaison: e.target.value as RaisonAbsence })}
                      className="input text-sm mt-2">
                      <option value="">Sélectionner...</option>
                      {RAISON_ABSENCE_OPTIONS.map((o) => (<option key={o.value} value={o.value}>{o.label}</option>))}
                    </select>
                    <textarea value={report.absenceCommentaire || ''}
                      onChange={(e) => updateReport(soul.id, { absenceCommentaire: e.target.value })}
                      className="input mt-2" rows={2} placeholder="Commentaire sur l'absence..." />
                  </div>

                  {/* Sorties */}
                  <div className="p-4 rounded-xl bg-white/30 dark:bg-gray-800/30">
                    <label className="label flex items-center gap-1.5">
                      <LogOutIcon className="w-3.5 h-3.5 text-orange-500" /> Sorties du suivi
                    </label>
                    <input type="number" min={0} value={report.nbSorties}
                      onChange={(e) => updateReport(soul.id, { nbSorties: parseInt(e.target.value) || 0 })}
                      className="input mt-2" />
                    {report.nbSorties > 0 && (
                      <select value={report.motifSortie || ''}
                        onChange={(e) => updateReport(soul.id, { motifSortie: e.target.value as MotifSortie })}
                        className="input mt-2 text-sm">
                        <option value="">Motif de sortie...</option>
                        {MOTIF_SORTIE_OPTIONS.map((o) => (<option key={o.value} value={o.value}>{o.label}</option>))}
                      </select>
                    )}
                  </div>
                </div>

                {/* Notes complémentaires */}
                <div className="mt-4 p-4 rounded-xl bg-white/30 dark:bg-gray-800/30">
                  <label className="label">Notes complémentaires</label>
                  <textarea value={report.notesComplementaires || ''}
                    onChange={(e) => updateReport(soul.id, { notesComplementaires: e.target.value })}
                    className="input" rows={2} placeholder="Notes additionnelles..." />
                </div>

                {/* Submit */}
                <div className="flex justify-end gap-2 mt-4 pt-3">
                  <button onClick={saveDraft} disabled={soumis} className="btn-ghost btn-sm text-gray-600">
                    <Save className="w-4 h-4" /> Brouillon
                  </button>
                  <button onClick={() => handleSubmit(soul.id)} disabled={submitMutation.isPending || soumis}
                    className={`btn-sm ${soumis ? 'btn-secondary' : 'btn-primary'}`}>
                    {submitMutation.isPending ? <Loader2 className="w-4 h-4 animate-spin" /> : <Send className="w-4 h-4" />}
                    {soumis ? 'Déjà soumis ✓' : 'Soumettre'}
                  </button>
                </div>
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
}

// Helper icon component to avoid import conflict
function LogOutIcon({ className }: { className?: string }) {
  return (
    <svg className={className} fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
      <path strokeLinecap="round" strokeLinejoin="round" d="M17 16l4-4m0 0l-4-4m4 4H7m6 4v1a3 3 0 01-3 3H6a3 3 0 01-3-3V7a3 3 0 013-3h4a3 3 0 013 3v1" />
    </svg>
  );
}
