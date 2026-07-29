import { useState } from 'react';
import { useParams, Link } from 'react-router-dom';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import api, { getErrorMessage } from '@/lib/api';
import { useAuth } from '@/contexts/AuthContext';
import type { Soul, MakerReport, SoulHistoryEntry, SoulNote } from '@/types';
import {
  ArrowLeft, Mail, Phone, Calendar, MapPin, Briefcase, FileText,
  Activity, MessageSquare, Send, Loader2, AlertTriangle, BookOpen,
  TrendingUp, Edit, LogOut, Undo2, Heart, Sparkles, Clock, ChevronRight,
  CheckCircle2,
} from 'lucide-react';
import toast from 'react-hot-toast';

type Tab = 'info' | 'history' | 'notes' | 'reports';

const STATUT_LABELS: Record<string, string> = {
  NOUVEAU_CONVERTI: 'Nouveau converti', NOUVEL_ARRIVANT: 'Nouvel arrivant',
  EN_INTEGRATION: 'En intégration', ACTIF: 'Actif', EN_VEILLE: 'En veille', DECROCHE: 'Décroché',
};

const STATUT_STYLES: Record<string, string> = {
  NOUVEAU_CONVERTI: 'badge-success', NOUVEL_ARRIVANT: 'badge-info',
  EN_INTEGRATION: 'badge-warning', ACTIF: 'badge-success', EN_VEILLE: 'badge-gray', DECROCHE: 'badge-danger',
};

const TYPE_STYLES: Record<string, string> = {
  NOUVEAU_CONVERTI: 'badge-success', NOUVEL_ARRIVANT: 'badge-info',
};

export default function SoulDetailPage() {
  const { id } = useParams<{ id: string }>();
  const { user } = useAuth();

  const queryClient = useQueryClient();
  const [activeTab, setActiveTab] = useState<Tab>('info');
  const [newNote, setNewNote] = useState('');
  const [showRetraction, setShowRetraction] = useState(false);
  const [retractionJustification, setRetractionJustification] = useState('');
  const [showExitForm, setShowExitForm] = useState(false);
  const [exitMotif, setExitMotif] = useState('');
  const [exitMotifDetail, setExitMotifDetail] = useState('');

  const { data: soul, isLoading } = useQuery({
    queryKey: ['soul', id], queryFn: async () => { const res = await api.get(`/souls/${id}`); return res.data as Soul; }, enabled: !!id,
  });
  const { data: history } = useQuery({
    queryKey: ['soul', id, 'history'], queryFn: async () => { const res = await api.get(`/souls/${id}/history`); return res.data as SoulHistoryEntry[]; }, enabled: !!id && activeTab === 'history',
  });
  const { data: notes } = useQuery({
    queryKey: ['soul', id, 'notes'], queryFn: async () => { const res = await api.get(`/souls/${id}/notes`); return res.data as SoulNote[]; }, enabled: !!id && activeTab === 'notes',
  });
  const { data: reports } = useQuery({
    queryKey: ['soul', id, 'reports'], queryFn: async () => { const res = await api.get(`/reports/maker-weekly?ameId=${id}&size=10`); return res.data.content as MakerReport[]; }, enabled: !!id && activeTab === 'reports',
  });

  const createNoteMutation = useMutation({
    mutationFn: async (contenu: string) => { await api.post(`/souls/${id}/notes`, { contenu }); },
    onSuccess: () => { queryClient.invalidateQueries({ queryKey: ['soul', id, 'notes'] }); setNewNote(''); toast.success('Note ajoutée'); },
    onError: (err) => toast.error(getErrorMessage(err)),
  });

  const retractionMutation = useMutation({
    mutationFn: async (justification: string) => { await api.post('/souls/retraction-request', { ameId: id, justification }); },
    onSuccess: () => { queryClient.invalidateQueries({ queryKey: ['soul', id] }); setShowRetraction(false); setRetractionJustification(''); toast.success('Demande de retrait soumise'); },
    onError: (err) => toast.error(getErrorMessage(err)),
  });

  if (isLoading) return (
    <div className="page-container">
      <div className="animate-fade-in space-y-4">
        <div className="skeleton h-8 w-48 rounded-lg" />
        <div className="skeleton h-64 w-full rounded-2xl" />
        <div className="grid grid-cols-3 gap-4"><div className="skeleton h-32 rounded-2xl" /><div className="skeleton h-32 rounded-2xl" /><div className="skeleton h-32 rounded-2xl" /></div>
      </div>
    </div>
  );

  if (!soul) return (
    <div className="page-container">
      <div className="glass-card p-12 text-center animate-scale-in">
        <Heart className="w-12 h-12 text-gray-300 mx-auto mb-4" />
        <p className="text-gray-500">Âme non trouvée</p>
        <Link to="/souls" className="btn-primary btn-sm mt-4 inline-flex">Retour aux âmes</Link>
      </div>
    </div>
  );

  const tabs: { key: Tab; label: string; icon: typeof BookOpen }[] = [
    { key: 'info', label: 'Informations', icon: BookOpen },
    { key: 'history', label: 'Historique', icon: Activity },
    { key: 'notes', label: 'Notes', icon: MessageSquare },
    { key: 'reports', label: 'Rapports', icon: FileText },
  ];

  return (
    <div className="page-container">
      {/* Back */}
      <Link to="/souls" className="btn-ghost btn-sm mb-4 inline-flex animate-fade-in">
        <ArrowLeft className="w-4 h-4" /> Retour aux âmes
      </Link>

      {/* Header */}
      <div className="glass-card p-5 sm:p-6 mb-6 animate-slide-up">
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
          <div className="flex items-center gap-4">
            <div className="w-16 h-16 rounded-2xl bg-gradient-to-br from-primary-400 via-primary-500 to-primary-700 flex items-center justify-center shadow-glow">
              <span className="text-2xl font-bold text-white drop-shadow-sm">{soul.prenom?.[0]}{soul.nom[0]}</span>
            </div>
            <div>
              <h1 className="page-title">{soul.prenom ? `${soul.prenom} ${soul.nom}` : soul.nom}</h1>
              <div className="flex flex-wrap gap-2 mt-1.5">
                <span className={TYPE_STYLES[soul.typeDisciple] || 'badge-info'}>
                  {soul.typeDisciple === 'NOUVEAU_CONVERTI' ? 'Nouveau converti' : 'Nouvel arrivant'}
                </span>
                <span className={`inline-flex items-center gap-1 ${STATUT_STYLES[soul.statut] || 'badge-gray'}`}>
                  <span className={`w-1.5 h-1.5 rounded-full ${soul.statut === 'ACTIF' ? 'bg-green-500 shadow-[0_0_4px_rgba(34,197,94,0.5)]' : 'bg-gray-400'}`} />
                  {STATUT_LABELS[soul.statut] || soul.statut}
                </span>
              </div>
            </div>
          </div>
          <div className="flex flex-wrap gap-2">
            <Link to={`/souls/${soul.id}/edit`} className="btn-secondary btn-sm"><Edit className="w-4 h-4" /> Modifier</Link>
            <button onClick={() => setShowRetraction(!showRetraction)} className="btn-ghost btn-sm text-orange-600 hover:text-orange-700">
              <AlertTriangle className="w-4 h-4" /> Retrait
            </button>
            <button onClick={() => setShowExitForm(!showExitForm)} className="btn-ghost btn-sm text-red-500 hover:text-red-600">
              <LogOut className="w-4 h-4" /> Sortie
            </button>
          </div>
        </div>
      </div>

      {/* Retraction request */}
      {showRetraction && (
        <div className="glass-card p-5 mb-6 animate-slide-up border-l-[3px] border-l-orange-500">
          <div className="flex items-center gap-3 mb-3">
            <div className="p-2 rounded-xl bg-orange-100 dark:bg-orange-900/30"><AlertTriangle className="w-5 h-5 text-orange-600" /></div>
            <div><h3 className="text-base font-semibold text-gray-900 dark:text-gray-100">Demande de retrait</h3><p className="text-xs text-gray-500">Justifiez la demande de retrait de cette âme</p></div>
          </div>
          <textarea className="input mb-3" rows={3} value={retractionJustification} onChange={(e) => setRetractionJustification(e.target.value)} placeholder="Justification obligatoire..." />
          <div className="flex justify-end gap-2">
            <button onClick={() => setShowRetraction(false)} className="btn-secondary btn-sm">Annuler</button>
            <button onClick={() => retractionMutation.mutate(retractionJustification)} disabled={!retractionJustification || retractionMutation.isPending} className="btn-primary btn-sm bg-orange-600 hover:bg-orange-700">
              {retractionMutation.isPending ? <Loader2 className="w-4 h-4 animate-spin" /> : <Send className="w-4 h-4" />} Soumettre
            </button>
          </div>
        </div>
      )}

      {/* Exit form (US-22) */}
      {showExitForm && (
        <div className="glass-card p-5 mb-6 animate-slide-up border-l-[3px] border-l-red-500">
          <div className="flex items-center gap-3 mb-3">
            <div className="p-2 rounded-xl bg-red-100 dark:bg-red-900/30"><LogOut className="w-5 h-5 text-red-600" /></div>
            <div><h3 className="text-base font-semibold text-gray-900 dark:text-gray-100">Sortie du suivi</h3><p className="text-xs text-gray-500">Marquer cette âme comme sortie du suivi avec motif obligatoire</p></div>
          </div>
          <div className="space-y-3">
            <select value={exitMotif} onChange={(e) => setExitMotif(e.target.value)} className="input">
              <option value="">Motif obligatoire...</option>
              <option value="INTEGRE_AUTONOME">Intégré/autonome</option><option value="TRANSFERT">Transfert</option>
              <option value="ABANDON">Abandon</option><option value="INJOIGNABLE_DURABLE">Injoignable durable</option>
              <option value="DECES">Décès</option><option value="AUTRE">Autre</option>
            </select>
            <textarea className="input" rows={2} value={exitMotifDetail} onChange={(e) => setExitMotifDetail(e.target.value)} placeholder="Détail (optionnel)..." />
            <div className="flex justify-end gap-2">
              <button onClick={() => setShowExitForm(false)} className="btn-secondary btn-sm">Annuler</button>
              <button onClick={async () => {
                try {
                  await api.post(`/souls/${id}/exit`, { motif: exitMotif, motifDetail: exitMotifDetail || undefined, peutReintegrer: true });
                  queryClient.invalidateQueries({ queryKey: ['soul', id] }); setShowExitForm(false); setExitMotif(''); setExitMotifDetail('');
                  toast.success('Âme marquée comme sortie du suivi');
                } catch (err) { toast.error(getErrorMessage(err)); }
              }} disabled={!exitMotif} className="btn-primary btn-sm bg-red-600 hover:bg-red-700">
                <LogOut className="w-4 h-4" /> Confirmer la sortie
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Reintegration */}
      {soul.statut === 'DECROCHE' && (
        <div className="glass-card p-5 mb-6 animate-slide-up border-l-[3px] border-l-green-500">
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-3">
              <div className="p-2 rounded-xl bg-green-100 dark:bg-green-900/30"><Undo2 className="w-5 h-5 text-green-600" /></div>
              <div><h3 className="text-base font-semibold text-gray-900 dark:text-gray-100">Réintégration possible</h3><p className="text-xs text-gray-500">Cette âme peut être réintégrée au suivi actif</p></div>
            </div>
            <button onClick={async () => {
              try {
                const res = await api.post(`/souls/${id}/reintegrate`, { nouveauStatut: 'ACTIF' });
                queryClient.invalidateQueries({ queryKey: ['soul', id] }); queryClient.setQueryData(['soul', id], res.data);
                toast.success('Âme réintégrée avec succès');
              } catch (err) { toast.error(getErrorMessage(err)); }
            }} className="btn-primary btn-sm bg-green-600 hover:bg-green-700">
              <Undo2 className="w-4 h-4" /> Réintégrer
            </button>
          </div>
        </div>
      )}

      {/* Tabs */}
      <div className="tabs mb-6 animate-fade-in">
        {tabs.map((tab) => {
          const Icon = tab.icon;
          return (
            <button key={tab.key} onClick={() => setActiveTab(tab.key)}
              className={activeTab === tab.key ? 'tab-active flex items-center gap-2' : 'tab flex items-center gap-2'}>
              <Icon className="w-4 h-4" /> {tab.label}
            </button>
          );
        })}
      </div>

      {/* Tab content */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        <div className="lg:col-span-2 space-y-6">
          {/* INFO TAB */}
          {activeTab === 'info' && (
            <>
              <div className="glass-card p-5 animate-slide-up">
                <h3 className="text-base font-semibold text-gray-900 dark:text-gray-100 mb-4 flex items-center gap-2">
                  <Sparkles className="w-4 h-4 text-primary-500" /> Coordonnées
                </h3>
                <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                  {[
                    { icon: Mail, label: 'Email', value: soul.email },
                    { icon: Phone, label: 'Téléphone', value: soul.telephone },
                    { icon: MapPin, label: 'Adresse', value: soul.adresse },
                    { icon: Calendar, label: 'Date de naissance', value: soul.dateNaissance ? new Date(soul.dateNaissance).toLocaleDateString('fr-FR') : undefined },
                    { icon: Briefcase, label: 'Profession', value: soul.profession },
                    { icon: Calendar, label: "Date d'intégration", value: new Date(soul.dateIntegration).toLocaleDateString('fr-FR', { day: 'numeric', month: 'long', year: 'numeric' }) },
                  ].filter(f => f.value).map(field => (
                    <div key={field.label} className="flex items-start gap-3 p-3 rounded-xl bg-white/30 dark:bg-gray-800/30">
                      <field.icon className="w-4 h-4 text-gray-400 mt-0.5 flex-shrink-0" />
                      <div><p className="text-xs text-gray-400">{field.label}</p><p className="text-sm font-medium text-gray-900 dark:text-gray-100">{field.value}</p></div>
                    </div>
                  ))}
                </div>
              </div>

              <div className="glass-card p-5 animate-slide-up" style={{ animationDelay: '80ms' }}>
                <h3 className="text-base font-semibold text-gray-900 dark:text-gray-100 mb-4 flex items-center gap-2">
                  <TrendingUp className="w-5 h-5 text-primary-500" /> Parcours spirituel
                </h3>
                <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                  <div className="p-4 rounded-xl bg-white/30 dark:bg-gray-800/30">
                    <p className="text-xs text-gray-400 mb-1">Type de disciple</p>
                    <p className="text-sm font-medium text-gray-900 dark:text-gray-100">{soul.typeDisciple === 'NOUVEAU_CONVERTI' ? 'Nouveau converti' : "Nouvel arrivant à l'église"}</p>
                  </div>
                  <div className="p-4 rounded-xl bg-white/30 dark:bg-gray-800/30">
                    <p className="text-xs text-gray-400 mb-1">État spirituel</p>
                    <p className="text-sm font-medium text-gray-900 dark:text-gray-100">{STATUT_LABELS[soul.statut] || soul.statut}</p>
                  </div>
                  {soul.dateConversion && <div className="p-4 rounded-xl bg-white/30 dark:bg-gray-800/30">
                    <p className="text-xs text-gray-400 mb-1">Date de conversion</p>
                    <p className="text-sm font-medium text-gray-900 dark:text-gray-100">{new Date(soul.dateConversion).toLocaleDateString('fr-FR', { day: 'numeric', month: 'long', year: 'numeric' })}</p>
                  </div>}
                  {soul.dateDernierContact && <div className="p-4 rounded-xl bg-white/30 dark:bg-gray-800/30">
                    <p className="text-xs text-gray-400 mb-1">Dernier contact</p>
                    <p className="text-sm font-medium text-gray-900 dark:text-gray-100">{new Date(soul.dateDernierContact).toLocaleDateString('fr-FR', { day: 'numeric', month: 'long', hour: '2-digit', minute: '2-digit' })}</p>
                  </div>}
                </div>
              </div>
            </>
          )}

          {/* HISTORY TAB */}
          {activeTab === 'history' && (
            <div className="glass-card p-5 animate-slide-up">
              <h3 className="text-base font-semibold text-gray-900 dark:text-gray-100 mb-4 flex items-center gap-2">
                <Activity className="w-4 h-4 text-primary-500" /> Historique complet
              </h3>
              {history && history.length > 0 ? (
                <div className="relative">
                  <div className="absolute left-4 top-0 bottom-0 w-0.5 bg-gradient-to-b from-primary-500/30 via-primary-500/20 to-transparent" />
                  <div className="space-y-4">
                    {history.map((entry, i) => (
                      <div key={entry.id} className="relative pl-10 animate-slide-up" style={{ animationDelay: `${i * 40}ms` }}>
                        <div className="absolute left-2.5 top-1.5 w-3 h-3 rounded-full bg-primary-500 border-2 border-white dark:border-gray-900 shadow-[0_0_6px_rgba(22,163,74,0.4)]" />
                        <div className="p-3.5 rounded-xl bg-white/30 dark:bg-gray-800/30 hover:bg-white/40 dark:hover:bg-gray-800/40 transition-colors">
                          <div className="flex items-center justify-between mb-1">
                            <span className="text-sm font-semibold text-gray-900 dark:text-gray-100">{entry.typeEvenement}</span>
                            <span className="text-[10px] text-gray-400 flex items-center gap-1"><Clock className="w-3 h-3" />{new Date(entry.createdAt).toLocaleDateString('fr-FR', { day: 'numeric', month: 'short', hour: '2-digit', minute: '2-digit' })}</span>
                          </div>
                          {entry.description && <p className="text-sm text-gray-600 dark:text-gray-400">{entry.description}</p>}
                          {(entry.ancienStatut || entry.nouveauStatut) && (
                            <div className="flex items-center gap-2 mt-1.5 text-xs">
                              {entry.ancienStatut && <span className="badge-gray text-[10px]">{entry.ancienStatut}</span>}
                              {entry.ancienStatut && entry.nouveauStatut && <ChevronRight className="w-3 h-3 text-gray-400" />}
                              {entry.nouveauStatut && <span className="badge-success text-[10px]">{entry.nouveauStatut}</span>}
                            </div>
                          )}
                        </div>
                      </div>
                    ))}
                  </div>
                </div>
              ) : (
                <div className="text-center py-8"><Activity className="w-8 h-8 text-gray-300 dark:text-gray-600 mx-auto mb-2" /><p className="text-sm text-gray-500">Aucun historique</p></div>
              )}
            </div>
          )}

          {/* NOTES TAB */}
          {activeTab === 'notes' && (
            <div className="space-y-6">
              <div className="glass-card p-5 animate-slide-up">
                <h3 className="text-base font-semibold text-gray-900 dark:text-gray-100 mb-4 flex items-center gap-2">
                  <MessageSquare className="w-4 h-4 text-primary-500" /> Ajouter une note
                </h3>
                <textarea className="input mb-3" rows={3} value={newNote} onChange={(e) => setNewNote(e.target.value)} placeholder="Note libre (hors rapport hebdo)..." />
                <div className="flex justify-end">
                  <button onClick={() => createNoteMutation.mutate(newNote)} disabled={!newNote || createNoteMutation.isPending} className="btn-primary btn-sm">
                    {createNoteMutation.isPending ? <Loader2 className="w-4 h-4 animate-spin" /> : <Send className="w-4 h-4" />} Ajouter la note
                  </button>
                </div>
              </div>

              {notes && notes.length > 0 && (
                <div className="space-y-3">
                  {notes.map((note, i) => (
                    <div key={note.id} className="glass-card p-4 animate-slide-up" style={{ animationDelay: `${i * 50}ms` }}>
                      <div className="flex items-start gap-3">
                        <div className="w-8 h-8 rounded-lg bg-gradient-to-br from-primary-400 to-primary-600 flex items-center justify-center text-white text-xs font-bold flex-shrink-0">
                          {note.auteurId?.slice(0, 2).toUpperCase() || '?'}
                        </div>
                        <div className="flex-1">
                          <div className="flex items-center justify-between mb-1">
                            <span className="text-xs text-gray-400">{note.auteurId?.slice(0, 12)}...</span>
                            <span className="text-[10px] text-gray-400">{new Date(note.dateCreation).toLocaleDateString('fr-FR', { day: 'numeric', month: 'short', hour: '2-digit', minute: '2-digit' })}</span>
                          </div>
                          <p className="text-sm text-gray-700 dark:text-gray-300">{note.contenu}</p>
                        </div>
                      </div>
                    </div>
                  ))}
                </div>
              )}
            </div>
          )}

          {/* REPORTS TAB */}
          {activeTab === 'reports' && (
            <div className="glass-card p-5 animate-slide-up">
              <h3 className="text-base font-semibold text-gray-900 dark:text-gray-100 mb-4 flex items-center gap-2">
                <FileText className="w-4 h-4 text-primary-500" /> Rapports hebdomadaires
              </h3>
              {reports && reports.length > 0 ? (
                <div className="space-y-2">
                  {reports.map((report, i) => (
                    <div key={report.id} className="flex items-center justify-between p-3.5 rounded-xl bg-white/30 dark:bg-gray-800/30 animate-fade-in" style={{ animationDelay: `${i * 40}ms` }}>
                      <div className="flex items-center gap-3">
                        <div className={`p-1.5 rounded-lg ${report.soumis ? 'bg-green-100 dark:bg-green-900/30' : 'bg-amber-100 dark:bg-amber-900/30'}`}>
                          <FileText className={`w-4 h-4 ${report.soumis ? 'text-green-600' : 'text-amber-600'}`} />
                        </div>
                        <div>
                          <p className="text-sm font-medium text-gray-900 dark:text-gray-100">
                            Semaine du {new Date(report.semaine).toLocaleDateString('fr-FR', { day: 'numeric', month: 'long', year: 'numeric' })}
                          </p>
                          <span className="text-[10px] text-gray-400">{report.soumis ? 'Soumis' : 'Brouillon'}</span>
                        </div>
                      </div>
                      {report.soumis ? (
                        <span className="badge-success text-[10px]"><CheckCircle2 className="w-3 h-3 mr-1" /> Soumis</span>
                      ) : (
                        <span className="badge-warning text-[10px]">Brouillon</span>
                      )}
                    </div>
                  ))}
                </div>
              ) : (
                <div className="text-center py-8"><FileText className="w-8 h-8 text-gray-300 dark:text-gray-600 mx-auto mb-2" /><p className="text-sm text-gray-500">Aucun rapport</p></div>
              )}
            </div>
          )}
        </div>

        {/* Right sidebar */}
        <div className="space-y-6">
          <div className="glass-card p-5 animate-slide-up">
            <h3 className="text-xs font-semibold text-gray-400 uppercase tracking-wider mb-3">Synthèse</h3>
            <div className="space-y-3">
              <div className="flex justify-between items-center py-2 border-b border-white/10 dark:border-white/[0.04]">
                <span className="text-xs text-gray-400">Type</span>
                <span className={TYPE_STYLES[soul.typeDisciple] || 'badge-info'}>{soul.typeDisciple === 'NOUVEAU_CONVERTI' ? 'Converti' : 'Arrivant'}</span>
              </div>
              <div className="flex justify-between items-center py-2 border-b border-white/10 dark:border-white/[0.04]">
                <span className="text-xs text-gray-400">Statut</span>
                <span className={STATUT_STYLES[soul.statut] || 'badge-gray'}>{STATUT_LABELS[soul.statut] || soul.statut}</span>
              </div>
              <div className="flex justify-between items-center py-2 border-b border-white/10 dark:border-white/[0.04]">
                <span className="text-xs text-gray-400">Intégration</span>
                <span className="text-xs font-medium text-gray-700 dark:text-gray-300">{new Date(soul.dateIntegration).toLocaleDateString('fr-FR')}</span>
              </div>
              <div className="flex justify-between items-center py-2">
                <span className="text-xs text-gray-400">Dernier contact</span>
                <span className="text-xs font-medium text-gray-700 dark:text-gray-300">{soul.dateDernierContact ? new Date(soul.dateDernierContact).toLocaleDateString('fr-FR') : '-'}</span>
              </div>
            </div>
          </div>

          <div className="glass-card p-5 animate-slide-up" style={{ animationDelay: '100ms' }}>
            <h3 className="text-xs font-semibold text-gray-400 uppercase tracking-wider mb-3">Actions rapides</h3>
            <div className="space-y-2">
              <Link to={`/souls/${soul.id}/edit`} className="flex items-center gap-2 p-2.5 rounded-lg hover:bg-white/30 dark:hover:bg-gray-800/30 transition-colors text-sm text-gray-700 dark:text-gray-300">
                <Edit className="w-4 h-4 text-primary-500" /> Modifier la fiche
              </Link>
              <Link to={`/reports/maker?ameId=${soul.id}`} className="flex items-center gap-2 p-2.5 rounded-lg hover:bg-white/30 dark:hover:bg-gray-800/30 transition-colors text-sm text-gray-700 dark:text-gray-300">
                <FileText className="w-4 h-4 text-primary-500" /> Voir les rapports
              </Link>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
