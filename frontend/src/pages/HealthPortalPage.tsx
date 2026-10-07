import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { HeartPulse, Users, Stethoscope, Pill, AlertTriangle, Megaphone, Loader2, Plus } from 'lucide-react';
import toast from 'react-hot-toast';
import { getErrorMessage } from '@/lib/api';
import { tText } from '@/i18n';
import * as svc from '@/services/healthService';

/**
 * Portail Santé — consomme le contrat exact V240
 * (`services/healthService.ts`, monture `/api/v1/health`).
 *
 * Les endpoints renvoient désormais des vues aplaties ({personId, personName},
 * {patientId, patientName}, {itemId, itemName}, {responsibleName},
 * {participantsCount}) : plus d'entités LAZY partiellement sérialisées. Les
 * écritures de cette page n'utilisent QUE les champs scalaires réellement
 * appliqués côté serveur (HealthService.updatePatientRecord,
 * updateConsultation, updatePharmacyItem, updatePharmacyStock,
 * createPharmacyItem / createCampaign — champs d'entité sans association).
 * Pas de formulaire de création de consultation : ses références
 * (`patient`, `practitioner`, nullable=false) sont des associations que ce
 * contrat ne permet pas de garantir sans vérification d'intégration — la
 * page se limite aux lectures et aux mises à jour scalar-vérifiées.
 */
type Row = Record<string, unknown>;
const sv = (r: Row, k: string): string => {
  const v = r[k];
  if (v == null || v === '') return '—';
  if (typeof v === 'object') return JSON.stringify(v);
  return String(v);
};

const TABS = ['dashboard', 'patients', 'consultations', 'pharmacy', 'campaigns'] as const;
type Tab = (typeof TABS)[number];

export default function HealthPortalPage() {
  const qc = useQueryClient();
  const [tab, setTab] = useState<Tab>('dashboard');
  const [showForm, setShowForm] = useState(false);

  const [patientSearch, setPatientSearch] = useState('');
  const [editingPatient, setEditingPatient] = useState<Row | null>(null);
  const [pForm, setPForm] = useState({ allergies: '', antecedents: '', medecinTraitant: '', numeroAssurance: '', poidsKg: '', tailleCm: '' });

  const [editingConsultation, setEditingConsultation] = useState<Row | null>(null);
  const [cForm, setCForm] = useState({ motif: '', diagnostic: '', traitement: '', resultat: '', orientation: '' });

  const [itemForm, setItemForm] = useState({ code: '', nom: '', description: '', categorie: 'MEDICAMENT', unite: '', fournisseur: '', prixAchat: '' });
  const [stockId, setStockId] = useState('');
  const [stockForm, setStockForm] = useState({ quantite: '', seuilAlerte: '', prixUnitaire: '', dateExpiration: '' });

  const [campaignForm, setCampaignForm] = useState({ title: '', description: '', campaignType: 'VACCINATION', startDate: '', endDate: '', lieu: '' });
  const [registering, setRegistering] = useState<{ campaignId: string; userId: string }>({ campaignId: '', userId: '' });

  const { data: dashStats } = useQuery({
    queryKey: ['health', 'dashboard'],
    queryFn: () => svc.getDashboardStats(),
    enabled: tab === 'dashboard',
  });
  const { data: reports } = useQuery({
    queryKey: ['health', 'reports'],
    queryFn: () => svc.getHealthReportsStatistics(),
    enabled: tab === 'dashboard',
  });

  const { data: patients, isLoading: loadingPatients } = useQuery({
    queryKey: ['health', 'patients', patientSearch],
    queryFn: () => svc.getPatients({ search: patientSearch || undefined, size: 50 }),
    enabled: tab === 'patients',
  });

  const { data: consultations, isLoading: loadingConsultations } = useQuery({
    queryKey: ['health', 'consultations'],
    queryFn: () => svc.getConsultations({ size: 50 }),
    enabled: tab === 'consultations',
  });

  const { data: items, isLoading: loadingItems } = useQuery({
    queryKey: ['health', 'pharmacy-items'],
    queryFn: () => svc.getPharmacyItems({ size: 100 }),
    enabled: tab === 'pharmacy',
  });
  const { data: stock, isLoading: loadingStock } = useQuery({
    queryKey: ['health', 'pharmacy-stock'],
    queryFn: () => svc.getPharmacyStock({ size: 50 }),
    enabled: tab === 'pharmacy',
  });
  const { data: lowStock = [] } = useQuery({
    queryKey: ['health', 'low-stock'],
    queryFn: () => svc.getLowStockAlerts(),
    enabled: tab === 'pharmacy',
  });
  const { data: expiring = [] } = useQuery({
    queryKey: ['health', 'expiring'],
    queryFn: () => svc.getExpiringSoonAlerts(30),
    enabled: tab === 'pharmacy',
  });

  const { data: campaigns, isLoading: loadingCampaigns } = useQuery({
    queryKey: ['health', 'campaigns'],
    queryFn: () => svc.getCampaigns({ size: 20 }),
    enabled: tab === 'campaigns',
  });

  const invalidate = () => qc.invalidateQueries({ queryKey: ['health'] });
  const onErr = (e: unknown) => toast.error(getErrorMessage(e));
  const ok = (msg: string) => {
    return () => {
      toast.success(tText(msg));
      setShowForm(false);
      setEditingPatient(null);
      setEditingConsultation(null);
      invalidate();
    };
  };

  const updatePatient = useMutation({
    mutationFn: (id: string) =>
      svc.updatePatient(id, {
        allergies: pForm.allergies || undefined,
        antecedents: pForm.antecedents || undefined,
        medecinTraitant: pForm.medecinTraitant || undefined,
        numeroAssurance: pForm.numeroAssurance || undefined,
        poidsKg: pForm.poidsKg === '' ? undefined : Number(pForm.poidsKg),
        tailleCm: pForm.tailleCm === '' ? undefined : Number(pForm.tailleCm),
      }),
    onSuccess: ok('Dossier patient mis à jour'),
    onError: onErr,
  });

  const updateConsultation = useMutation({
    mutationFn: (id: string) =>
      svc.updateConsultation(id, {
        motif: cForm.motif || undefined,
        diagnostic: cForm.diagnostic || undefined,
        traitement: cForm.traitement || undefined,
        resultat: cForm.resultat || undefined,
        orientation: cForm.orientation || undefined,
      }),
    onSuccess: ok('Consultation mise à jour'),
    onError: onErr,
  });

  const createItem = useMutation({
    mutationFn: () =>
      svc.createPharmacyItem({
        code: itemForm.code || undefined,
        nom: itemForm.nom,
        description: itemForm.description || undefined,
        categorie: itemForm.categorie,
        unite: itemForm.unite || undefined,
        fournisseur: itemForm.fournisseur || undefined,
        prixAchat: itemForm.prixAchat === '' ? undefined : Number(itemForm.prixAchat),
      }),
    onSuccess: ok('Article créé'),
    onError: onErr,
  });

  const updateStock = useMutation({
    mutationFn: () =>
      svc.updatePharmacyStock(stockId, {
        quantite: stockForm.quantite === '' ? undefined : Number(stockForm.quantite),
        seuilAlerte: stockForm.seuilAlerte === '' ? undefined : Number(stockForm.seuilAlerte),
        prixUnitaire: stockForm.prixUnitaire === '' ? undefined : Number(stockForm.prixUnitaire),
        dateExpiration: stockForm.dateExpiration || undefined,
      }),
    onSuccess: () => {
      toast.success(tText('Stock mis à jour'));
      setStockId('');
      invalidate();
    },
    onError: onErr,
  });

  const createCampaign = useMutation({
    mutationFn: () =>
      svc.createCampaign({
        title: campaignForm.title,
        description: campaignForm.description || undefined,
        campaignType: campaignForm.campaignType,
        startDate: campaignForm.startDate || undefined,
        endDate: campaignForm.endDate || undefined,
        lieu: campaignForm.lieu || undefined,
      }),
    onSuccess: ok('Campagne créée'),
    onError: onErr,
  });

  const registerParticipant = useMutation({
    mutationFn: () => svc.registerCampaignParticipant(registering.campaignId, registering.userId),
    onSuccess: () => {
      toast.success(tText('Participant inscrit'));
      setRegistering({ campaignId: '', userId: '' });
      invalidate();
    },
    onError: onErr,
  });

  const spinner = <div className="flex justify-center py-12"><Loader2 className="w-8 h-8 animate-spin text-primary-500" /></div>;

  return (
    <div className="page-container">
      <div className="page-header">
        <div className="p-3 rounded-xl bg-gradient-to-br from-rose-500 to-pink-600 text-white shadow-lg"><HeartPulse className="w-6 h-6" /></div>
        <div><h1 className="page-title">{tText('Portail santé')}</h1><p className="page-subtitle">{tText('Patients, consultations, pharmacie et campagnes')}</p></div>
        {(tab === 'pharmacy' || tab === 'campaigns') && (
          <button onClick={() => setShowForm(!showForm)} className="btn-primary btn-sm ml-auto inline-flex items-center gap-1"><Plus className="w-4 h-4" /> {tText('Nouveau')}</button>
        )}
      </div>

      <div className="flex flex-wrap gap-2 mb-6">
        {TABS.map((t) => (
          <button key={t} onClick={() => { setTab(t); setShowForm(false); setEditingPatient(null); setEditingConsultation(null); }} className={`btn-sm px-4 py-2 rounded-lg ${tab === t ? 'btn-primary' : 'glass-card'}`}>{tText(t)}</button>
        ))}
      </div>

      {tab === 'dashboard' && (
        <div className="space-y-6">
          {dashStats && (
            <div className="glass-card p-5">
              <h3 className="font-semibold text-gray-800 dark:text-gray-200 mb-3">{tText('Tableau de bord')}</h3>
              <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
                {Object.entries(dashStats).map(([k, v]) => (
                  <div key={k}><p className="stat-value">{v == null ? '—' : String(v)}</p><p className="stat-label">{tText(k)}</p></div>
                ))}
              </div>
            </div>
          )}
          {reports && (
            <div className="glass-card p-5">
              <h3 className="font-semibold text-gray-800 dark:text-gray-200 mb-3">{tText('Statistiques rapports')}</h3>
              <div className="grid grid-cols-2 md:grid-cols-3 gap-4">
                {Object.entries(reports).map(([k, v]) => (
                  <div key={k}><p className="stat-value">{v == null ? '—' : String(v)}</p><p className="stat-label">{tText(k)}</p></div>
                ))}
              </div>
            </div>
          )}
        </div>
      )}

      {tab === 'patients' && (
        <>
          <input className="input md:w-72 mb-4" placeholder={tText('Rechercher…')} value={patientSearch} onChange={(e) => setPatientSearch(e.target.value)} />
          {editingPatient && (
            <div className="glass-card p-6 mb-6 grid gap-4 md:grid-cols-2">
              <div><label className="text-xs font-medium text-gray-500 mb-1 block">{tText('Allergies')}</label><input className="input w-full" value={pForm.allergies} onChange={(e) => setPForm({ ...pForm, allergies: e.target.value })} /></div>
              <div><label className="text-xs font-medium text-gray-500 mb-1 block">{tText('Antécédents')}</label><input className="input w-full" value={pForm.antecedents} onChange={(e) => setPForm({ ...pForm, antecedents: e.target.value })} /></div>
              <div><label className="text-xs font-medium text-gray-500 mb-1 block">{tText('Médecin traitant')}</label><input className="input w-full" value={pForm.medecinTraitant} onChange={(e) => setPForm({ ...pForm, medecinTraitant: e.target.value })} /></div>
              <div><label className="text-xs font-medium text-gray-500 mb-1 block">{tText('N° assurance')}</label><input className="input w-full" value={pForm.numeroAssurance} onChange={(e) => setPForm({ ...pForm, numeroAssurance: e.target.value })} /></div>
              <div><label className="text-xs font-medium text-gray-500 mb-1 block">{tText('Poids (kg)')}</label><input type="number" className="input w-full" value={pForm.poidsKg} onChange={(e) => setPForm({ ...pForm, poidsKg: e.target.value })} /></div>
              <div><label className="text-xs font-medium text-gray-500 mb-1 block">{tText('Taille (cm)')}</label><input type="number" className="input w-full" value={pForm.tailleCm} onChange={(e) => setPForm({ ...pForm, tailleCm: e.target.value })} /></div>
              <div className="md:col-span-2 flex justify-end gap-3">
                <button onClick={() => setEditingPatient(null)} className="btn-sm px-4 py-2 rounded-lg glass-card">{tText('Annuler')}</button>
                <button onClick={() => updatePatient.mutate(String(editingPatient.id))} disabled={updatePatient.isPending} className="btn-primary btn-sm">{tText('Enregistrer')}</button>
              </div>
            </div>
          )}
          {loadingPatients ? spinner : !patients || patients.content.length === 0 ? <div className="glass-card p-10 text-center text-gray-500">{tText('Aucun patient')}</div> : (
            <div className="space-y-3">
              {patients.content.map((p) => (
                <div key={String(p.id)} className="glass-card p-4 flex flex-wrap items-center gap-x-4 gap-y-1 text-sm">
                  <Users className="w-4 h-4 text-rose-500" />
                  <span className="font-medium text-gray-800 dark:text-gray-200">{sv(p, 'personName')}</span>
                  <span className="text-gray-500 text-xs">#{String(p.id).slice(0, 8)}</span>
                  <span className="text-gray-700 dark:text-gray-300">{tText('Médecin')} : {sv(p, 'medecinTraitant')}</span>
                  <span className="text-gray-700 dark:text-gray-300">{tText('Poids')} : {sv(p, 'poidsKg')} kg</span>
                  <span className="text-gray-700 dark:text-gray-300">{tText('Taille')} : {sv(p, 'tailleCm')} cm</span>
                  <button
                    onClick={() => {
                      setEditingPatient(p);
                      setPForm({
                        allergies: p.allergies == null ? '' : String(p.allergies),
                        antecedents: p.antecedents == null ? '' : String(p.antecedents),
                        medecinTraitant: p.medecinTraitant == null ? '' : String(p.medecinTraitant),
                        numeroAssurance: p.numeroAssurance == null ? '' : String(p.numeroAssurance),
                        poidsKg: p.poidsKg == null ? '' : String(p.poidsKg),
                        tailleCm: p.tailleCm == null ? '' : String(p.tailleCm),
                      });
                    }}
                    className="ml-auto text-xs btn-sm px-3 py-1 rounded-lg glass-card"
                  >{tText('Modifier')}</button>
                </div>
              ))}
            </div>
          )}
        </>
      )}

      {tab === 'consultations' && (
        <>
          {editingConsultation && (
            <div className="glass-card p-6 mb-6 grid gap-4 md:grid-cols-2">
              <div><label className="text-xs font-medium text-gray-500 mb-1 block">{tText('Motif')}</label><input className="input w-full" value={cForm.motif} onChange={(e) => setCForm({ ...cForm, motif: e.target.value })} /></div>
              <div><label className="text-xs font-medium text-gray-500 mb-1 block">{tText('Diagnostic')}</label><input className="input w-full" value={cForm.diagnostic} onChange={(e) => setCForm({ ...cForm, diagnostic: e.target.value })} /></div>
              <div><label className="text-xs font-medium text-gray-500 mb-1 block">{tText('Traitement')}</label><input className="input w-full" value={cForm.traitement} onChange={(e) => setCForm({ ...cForm, traitement: e.target.value })} /></div>
              <div><label className="text-xs font-medium text-gray-500 mb-1 block">{tText('Résultat')}</label><input className="input w-full" value={cForm.resultat} onChange={(e) => setCForm({ ...cForm, resultat: e.target.value })} /></div>
              <div><label className="text-xs font-medium text-gray-500 mb-1 block">{tText('Orientation')}</label><input className="input w-full" value={cForm.orientation} onChange={(e) => setCForm({ ...cForm, orientation: e.target.value })} /></div>
              <div className="md:col-span-2 flex justify-end gap-3">
                <button onClick={() => setEditingConsultation(null)} className="btn-sm px-4 py-2 rounded-lg glass-card">{tText('Annuler')}</button>
                <button onClick={() => updateConsultation.mutate(String(editingConsultation.id))} disabled={updateConsultation.isPending} className="btn-primary btn-sm">{tText('Enregistrer')}</button>
              </div>
            </div>
          )}
          {loadingConsultations ? spinner : !consultations || consultations.content.length === 0 ? <div className="glass-card p-10 text-center text-gray-500">{tText('Aucune consultation')}</div> : (
            <div className="space-y-3">
              {consultations.content.map((c) => (
                <div key={String(c.id)} className="glass-card p-4 flex flex-wrap items-center gap-x-4 gap-y-1 text-sm">
                  <Stethoscope className="w-4 h-4 text-rose-500" />
                  <span className="font-medium text-gray-800 dark:text-gray-200">{sv(c, 'patientName')}</span>
                  <span className="text-gray-500 text-xs">{tText('Praticien')} : {sv(c, 'practitionerName')}</span>
                  <span className="text-gray-700 dark:text-gray-300">{sv(c, 'consultationDate')}</span>
                  <span className="text-gray-700 dark:text-gray-300">{sv(c, 'typeConsultation')}</span>
                  <span className="text-gray-700 dark:text-gray-300">{sv(c, 'motif')}</span>
                  <span className="text-xs px-2 py-0.5 rounded-full bg-rose-100 text-rose-700">{sv(c, 'status')}</span>
                  <button
                    onClick={() => {
                      setEditingConsultation(c);
                      setCForm({
                        motif: c.motif == null ? '' : String(c.motif),
                        diagnostic: c.diagnostic == null ? '' : String(c.diagnostic),
                        traitement: c.traitement == null ? '' : String(c.traitement),
                        resultat: c.resultat == null ? '' : String(c.resultat),
                        orientation: c.orientation == null ? '' : String(c.orientation),
                      });
                    }}
                    className="ml-auto text-xs btn-sm px-3 py-1 rounded-lg glass-card"
                  >{tText('Modifier')}</button>
                </div>
              ))}
            </div>
          )}
        </>
      )}

      {tab === 'pharmacy' && (
        <>
          {showForm && (
            <div className="glass-card p-6 mb-6 grid gap-4 md:grid-cols-2">
              <div><label className="text-xs font-medium text-gray-500 mb-1 block">{tText('Nom *')}</label><input className="input w-full" value={itemForm.nom} onChange={(e) => setItemForm({ ...itemForm, nom: e.target.value })} /></div>
              <div><label className="text-xs font-medium text-gray-500 mb-1 block">{tText('Code')}</label><input className="input w-full" value={itemForm.code} onChange={(e) => setItemForm({ ...itemForm, code: e.target.value })} /></div>
              <div><label className="text-xs font-medium text-gray-500 mb-1 block">{tText('Catégorie')}</label>
                <select className="input w-full" value={itemForm.categorie} onChange={(e) => setItemForm({ ...itemForm, categorie: e.target.value })}>
                  <option value="MEDICAMENT">MEDICAMENT</option><option value="VACCIN">VACCIN</option><option value="MATERIEL">MATERIEL</option><option value="CONSUMABLE">CONSUMABLE</option>
                </select>
              </div>
              <div><label className="text-xs font-medium text-gray-500 mb-1 block">{tText('Unité')}</label><input className="input w-full" value={itemForm.unite} onChange={(e) => setItemForm({ ...itemForm, unite: e.target.value })} /></div>
              <div><label className="text-xs font-medium text-gray-500 mb-1 block">{tText('Fournisseur')}</label><input className="input w-full" value={itemForm.fournisseur} onChange={(e) => setItemForm({ ...itemForm, fournisseur: e.target.value })} /></div>
              <div><label className="text-xs font-medium text-gray-500 mb-1 block">{tText('Prix d\'achat')}</label><input type="number" className="input w-full" value={itemForm.prixAchat} onChange={(e) => setItemForm({ ...itemForm, prixAchat: e.target.value })} /></div>
              <div className="md:col-span-2 flex justify-end gap-3">
                <button onClick={() => setShowForm(false)} className="btn-sm px-4 py-2 rounded-lg glass-card">{tText('Annuler')}</button>
                <button onClick={() => createItem.mutate()} disabled={!itemForm.nom.trim() || createItem.isPending} className="btn-primary btn-sm">{tText('Créer')}</button>
              </div>
            </div>
          )}

          {lowStock.length > 0 && (
            <div className="glass-card p-4 mb-4 border border-amber-300">
              <h3 className="text-sm font-semibold text-amber-700 mb-2 inline-flex items-center gap-2"><AlertTriangle className="w-4 h-4" />{tText('Alertes stock bas')}</h3>
              {lowStock.map((s, i) => (
                <p key={i} className="text-xs text-gray-600">{sv(s, 'itemName')} — {tText('Lot')} {sv(s, 'lotNumber')} — {sv(s, 'quantite')} / {tText('seuil')} {sv(s, 'seuilAlerte')}</p>
              ))}
            </div>
          )}
          {expiring.length > 0 && (
            <div className="glass-card p-4 mb-4 border border-red-300">
              <h3 className="text-sm font-semibold text-red-700 mb-2 inline-flex items-center gap-2"><AlertTriangle className="w-4 h-4" />{tText('Expirations proches (30 j)')}</h3>
              {expiring.map((s, i) => (
                <p key={i} className="text-xs text-gray-600">{sv(s, 'itemName')} — {tText('Lot')} {sv(s, 'lotNumber')} — {tText('expire')} {sv(s, 'dateExpiration')}</p>
              ))}
            </div>
          )}

          <div className="glass-card p-4 mb-4">
            <h3 className="text-sm font-semibold text-gray-700 dark:text-gray-300 mb-2 inline-flex items-center gap-2"><Pill className="w-4 h-4 text-rose-500" />{tText('Mise à jour d\'un lot')}</h3>
            <div className="grid gap-3 md:grid-cols-5">
              <select className="input" value={stockId} onChange={(e) => setStockId(e.target.value)}>
                <option value="">{tText('Choisir un lot…')}</option>
                {(stock?.content ?? []).map((s) => <option key={String(s.id)} value={String(s.id)}>{sv(s, 'itemName')} · {sv(s, 'lotNumber')} ({sv(s, 'quantite')})</option>)}
              </select>
              <input type="number" className="input" placeholder={tText('Quantité')} value={stockForm.quantite} onChange={(e) => setStockForm({ ...stockForm, quantite: e.target.value })} />
              <input type="number" className="input" placeholder={tText('Seuil alerte')} value={stockForm.seuilAlerte} onChange={(e) => setStockForm({ ...stockForm, seuilAlerte: e.target.value })} />
              <input type="date" className="input" value={stockForm.dateExpiration} onChange={(e) => setStockForm({ ...stockForm, dateExpiration: e.target.value })} />
              <button onClick={() => updateStock.mutate()} disabled={!stockId || updateStock.isPending} className="btn-primary btn-sm">{tText('Enregistrer')}</button>
            </div>
          </div>

          {loadingItems ? spinner : !items || items.content.length === 0 ? <div className="glass-card p-10 text-center text-gray-500">{tText('Aucun article')}</div> : (
            <div className="space-y-3">
              {items.content.map((it) => (
                <div key={String(it.id)} className="glass-card p-4 flex flex-wrap items-center gap-x-4 gap-y-1 text-sm">
                  <Pill className="w-4 h-4 text-rose-500" />
                  <span className="font-medium text-gray-800 dark:text-gray-200">{sv(it, 'nom')}</span>
                  <span className="text-xs text-gray-500">{sv(it, 'categorie')}</span>
                  <span className="text-xs text-gray-500">{sv(it, 'unite')}</span>
                  <span className="text-xs text-gray-500">{sv(it, 'fournisseur')}</span>
                  <span className="ml-auto text-xs text-gray-500">{sv(it, 'prixAchat')}</span>
                </div>
              ))}
            </div>
          )}
        </>
      )}

      {tab === 'campaigns' && (
        <>
          {showForm && (
            <div className="glass-card p-6 mb-6 grid gap-4 md:grid-cols-2">
              <div><label className="text-xs font-medium text-gray-500 mb-1 block">{tText('Titre *')}</label><input className="input w-full" value={campaignForm.title} onChange={(e) => setCampaignForm({ ...campaignForm, title: e.target.value })} /></div>
              <div><label className="text-xs font-medium text-gray-500 mb-1 block">{tText('Type')}</label>
                <select className="input w-full" value={campaignForm.campaignType} onChange={(e) => setCampaignForm({ ...campaignForm, campaignType: e.target.value })}>
                  <option value="VACCINATION">VACCINATION</option><option value="DEPISTAGE">DEPISTAGE</option><option value="SENSIBILISATION">SENSIBILISATION</option><option value="DON_SANG">DON_SANG</option><option value="ATELIER">ATELIER</option>
                </select>
              </div>
              <div><label className="text-xs font-medium text-gray-500 mb-1 block">{tText('Description')}</label><input className="input w-full" value={campaignForm.description} onChange={(e) => setCampaignForm({ ...campaignForm, description: e.target.value })} /></div>
              <div><label className="text-xs font-medium text-gray-500 mb-1 block">{tText('Lieu')}</label><input className="input w-full" value={campaignForm.lieu} onChange={(e) => setCampaignForm({ ...campaignForm, lieu: e.target.value })} /></div>
              <div><label className="text-xs font-medium text-gray-500 mb-1 block">{tText('Début')}</label><input type="date" className="input w-full" value={campaignForm.startDate} onChange={(e) => setCampaignForm({ ...campaignForm, startDate: e.target.value })} /></div>
              <div><label className="text-xs font-medium text-gray-500 mb-1 block">{tText('Fin')}</label><input type="date" className="input w-full" value={campaignForm.endDate} onChange={(e) => setCampaignForm({ ...campaignForm, endDate: e.target.value })} /></div>
              <div className="md:col-span-2 flex justify-end gap-3">
                <button onClick={() => setShowForm(false)} className="btn-sm px-4 py-2 rounded-lg glass-card">{tText('Annuler')}</button>
                <button onClick={() => createCampaign.mutate()} disabled={!campaignForm.title.trim() || createCampaign.isPending} className="btn-primary btn-sm">{tText('Créer')}</button>
              </div>
            </div>
          )}
          {loadingCampaigns ? spinner : !campaigns || campaigns.content.length === 0 ? <div className="glass-card p-10 text-center text-gray-500">{tText('Aucune campagne')}</div> : (
            <div className="space-y-3">
              {campaigns.content.map((c) => (
                <div key={String(c.id)} className="glass-card p-4">
                  <div className="flex flex-wrap items-center gap-x-4 gap-y-1 text-sm">
                    <Megaphone className="w-4 h-4 text-rose-500" />
                    <span className="font-medium text-gray-800 dark:text-gray-200">{sv(c, 'title')}</span>
                    <span className="text-xs text-gray-500">{sv(c, 'campaignType')}</span>
                    <span className="text-xs text-gray-500">{sv(c, 'startDate')} → {sv(c, 'endDate')}</span>
                    <span className="text-xs text-gray-500">{sv(c, 'lieu')}</span>
                    <span className="text-xs text-gray-500">{tText('Responsable')} : {sv(c, 'responsibleName')}</span>
                    <span className="text-xs text-gray-500">{sv(c, 'participantsCount')} {tText('inscrit(s)')}</span>
                    <span className="text-xs px-2 py-0.5 rounded-full bg-rose-100 text-rose-700">{sv(c, 'status')}</span>
                  </div>
                  <div className="mt-3 flex flex-wrap items-center gap-2">
                    <input
                      className="input text-xs md:w-64"
                      placeholder={tText('UUID utilisateur à inscrire')}
                      value={registering.campaignId === String(c.id) ? registering.userId : ''}
                      onChange={(e) => setRegistering({ campaignId: String(c.id), userId: e.target.value })}
                    />
                    <button
                      onClick={() => registerParticipant.mutate()}
                      disabled={registering.campaignId !== String(c.id) || !registering.userId.trim() || registerParticipant.isPending}
                      className="btn-sm px-3 py-1 rounded-lg glass-card"
                    >{tText('Inscrire')}</button>
                  </div>
                </div>
              ))}
            </div>
          )}
        </>
      )}
    </div>
  );
}
