import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { Landmark, HandCoins, RefreshCcw, PiggyBank, Scale, BarChart3, Loader2, Plus, Users } from 'lucide-react';
import toast from 'react-hot-toast';
import { getErrorMessage } from '@/lib/api';
import { tText } from '@/i18n';
import * as svc from '@/services/financeService';

/**
 * Modules Finances V236 — consomme le contrat exact
 * (`services/financeService.ts`, monture `/api/v1/finances`).
 * Clés de body uniquement celles lues par FinanceService (createAccount,
 * createDonation, createTontine, createTontineMember — dates en ISO-8601
 * `Instant.parse`, frequency = enum FinanceTontine.Frequency). Les vues
 * rapprochement (unmatched/ledger), dont la forme exacte n'est pas
 * contractée, sont rendues en JSON générique — rien d'inventé.
 */
type Row = Record<string, unknown>;
const sv = (r: Row, k: string): string => {
  const v = r[k];
  if (v == null || v === '') return '—';
  if (typeof v === 'object') return JSON.stringify(v);
  return String(v);
};

const TABS = ['accounts', 'donations', 'tontines', 'budgets', 'reconciliation', 'reports'] as const;
type Tab = (typeof TABS)[number];

export default function FinanceModulesPage() {
  const qc = useQueryClient();
  const [tab, setTab] = useState<Tab>('accounts');
  const [showForm, setShowForm] = useState(false);
  const [annee, setAnnee] = useState(new Date().getFullYear());

  const [accForm, setAccForm] = useState({ name: '', accountNumber: '', bankName: '', balance: '', devise: 'XOF' });
  const [donForm, setDonForm] = useState({ donorName: '', amount: '', devise: 'XOF', donationDate: '', purpose: '', isAnonymous: false });
  const [tonForm, setTonForm] = useState({ name: '', description: '', amountPerTurn: '', frequency: 'MONTHLY', startDate: '' });
  const [memberForm, setMemberForm] = useState({ tontineId: '', userId: '', turnOrder: '' });
  const [budgetForm, setBudgetForm] = useState({ categorie: '', montant: '' });

  const { data: accounts = [], isLoading: loadingAccounts } = useQuery({
    queryKey: ['finance-v236', 'accounts'],
    queryFn: () => svc.listAccounts(),
    enabled: tab === 'accounts',
  });
  const { data: donations = [], isLoading: loadingDonations } = useQuery({
    queryKey: ['finance-v236', 'donations'],
    queryFn: () => svc.listDonations(),
    enabled: tab === 'donations',
  });
  const { data: tontines = [], isLoading: loadingTontines } = useQuery({
    queryKey: ['finance-v236', 'tontines'],
    queryFn: () => svc.listTontines(),
    enabled: tab === 'tontines',
  });
  const [openTontine, setOpenTontine] = useState<string>('');
  const { data: members = [] } = useQuery({
    queryKey: ['finance-v236', 'tontine-members', openTontine],
    queryFn: () => svc.listTontineMembers(openTontine),
    enabled: tab === 'tontines' && openTontine !== '',
  });
  const { data: payouts = [] } = useQuery({
    queryKey: ['finance-v236', 'tontine-payouts', openTontine],
    queryFn: () => svc.listTontinePayouts(openTontine),
    enabled: tab === 'tontines' && openTontine !== '',
  });
  const { data: budgets = [], isLoading: loadingBudgets } = useQuery({
    queryKey: ['finance-v236', 'budgets', annee],
    queryFn: () => svc.listBudgets(annee),
    enabled: tab === 'budgets',
  });
  const { data: unmatched } = useQuery({
    queryKey: ['finance-v236', 'unmatched'],
    queryFn: () => svc.unmatched(),
    enabled: tab === 'reconciliation',
  });
  const { data: ledger } = useQuery({
    queryKey: ['finance-v236', 'ledger'],
    queryFn: () => svc.ledger(),
    enabled: tab === 'reconciliation',
  });
  const { data: summary } = useQuery({
    queryKey: ['finance-v236', 'report-summary'],
    queryFn: () => svc.reportSummary(),
    enabled: tab === 'reports',
  });
  const { data: byCategory = [] } = useQuery({
    queryKey: ['finance-v236', 'report-category'],
    queryFn: () => svc.reportByCategory(),
    enabled: tab === 'reports',
  });
  const { data: cashFlow = [] } = useQuery({
    queryKey: ['finance-v236', 'report-cashflow'],
    queryFn: () => svc.reportCashFlow(),
    enabled: tab === 'reports',
  });

  const invalidate = () => qc.invalidateQueries({ queryKey: ['finance-v236'] });
  const onErr = (e: unknown) => toast.error(getErrorMessage(e));
  const ok = (msg: string) => {
    return () => {
      toast.success(tText(msg));
      setShowForm(false);
      invalidate();
    };
  };

  const createAccount = useMutation({
    mutationFn: () =>
      svc.createAccount({
        name: accForm.name,
        accountNumber: accForm.accountNumber || undefined,
        bankName: accForm.bankName || undefined,
        balance: accForm.balance === '' ? undefined : Number(accForm.balance),
        devise: accForm.devise || undefined,
      }),
    onSuccess: ok('Compte créé'),
    onError: onErr,
  });

  const createDonation = useMutation({
    mutationFn: () =>
      svc.createDonation({
        donorName: donForm.donorName || undefined,
        amount: Number(donForm.amount),
        devise: donForm.devise || undefined,
        donationDate: donForm.donationDate ? new Date(`${donForm.donationDate}T00:00:00Z`).toISOString() : undefined,
        purpose: donForm.purpose || undefined,
        isAnonymous: donForm.isAnonymous,
      }),
    onSuccess: ok('Don enregistré'),
    onError: onErr,
  });

  const createTontine = useMutation({
    mutationFn: () =>
      svc.createTontine({
        name: tonForm.name,
        description: tonForm.description || undefined,
        amountPerTurn: Number(tonForm.amountPerTurn),
        frequency: tonForm.frequency,
        startDate: tonForm.startDate ? new Date(`${tonForm.startDate}T00:00:00Z`).toISOString() : undefined,
      }),
    onSuccess: ok('Tontine créée'),
    onError: onErr,
  });

  const addMember = useMutation({
    mutationFn: () => svc.createTontineMember(memberForm.tontineId, memberForm.userId, memberForm.turnOrder === '' ? undefined : Number(memberForm.turnOrder)),
    onSuccess: () => {
      toast.success(tText('Membre ajouté'));
      setMemberForm({ tontineId: '', userId: '', turnOrder: '' });
      invalidate();
    },
    onError: onErr,
  });

  const upsertBudget = useMutation({
    mutationFn: () => svc.upsertBudget({ annee, categorie: budgetForm.categorie, montant: Number(budgetForm.montant) }),
    onSuccess: ok('Budget enregistré'),
    onError: onErr,
  });

  const deleteBudget = useMutation({
    mutationFn: (id: string) => svc.deleteBudget(id),
    onSuccess: () => {
      toast.success(tText('Budget supprimé'));
      invalidate();
    },
    onError: onErr,
  });

  const autoMatch = useMutation({
    mutationFn: () => svc.autoMatch(),
    onSuccess: () => {
      toast.success(tText('Rapprochement automatique effectué'));
      invalidate();
    },
    onError: onErr,
  });

  const spinner = <div className="flex justify-center py-12"><Loader2 className="w-8 h-8 animate-spin text-primary-500" /></div>;

  return (
    <div className="page-container">
      <div className="page-header">
        <div className="p-3 rounded-xl bg-gradient-to-br from-teal-500 to-emerald-600 text-white shadow-lg"><Landmark className="w-6 h-6" /></div>
        <div><h1 className="page-title">{tText('Modules finances')}</h1><p className="page-subtitle">{tText('Comptes, dons, tontines, budgets, rapprochement et rapports V236')}</p></div>
        {['accounts', 'donations', 'tontines', 'budgets'].includes(tab) && (
          <button onClick={() => setShowForm(!showForm)} className="btn-primary btn-sm ml-auto inline-flex items-center gap-1"><Plus className="w-4 h-4" /> {tText('Nouveau')}</button>
        )}
      </div>

      <div className="flex flex-wrap gap-2 mb-6">
        {TABS.map((t) => (
          <button key={t} onClick={() => { setTab(t); setShowForm(false); }} className={`btn-sm px-4 py-2 rounded-lg ${tab === t ? 'btn-primary' : 'glass-card'}`}>{tText(t)}</button>
        ))}
      </div>

      {tab === 'accounts' && (
        <>
          {showForm && (
            <div className="glass-card p-6 mb-6 grid gap-4 md:grid-cols-2">
              <div><label className="text-xs font-medium text-gray-500 mb-1 block">{tText('Nom *')}</label><input className="input w-full" value={accForm.name} onChange={(e) => setAccForm({ ...accForm, name: e.target.value })} /></div>
              <div><label className="text-xs font-medium text-gray-500 mb-1 block">{tText('Numéro de compte')}</label><input className="input w-full" value={accForm.accountNumber} onChange={(e) => setAccForm({ ...accForm, accountNumber: e.target.value })} /></div>
              <div><label className="text-xs font-medium text-gray-500 mb-1 block">{tText('Banque')}</label><input className="input w-full" value={accForm.bankName} onChange={(e) => setAccForm({ ...accForm, bankName: e.target.value })} /></div>
              <div><label className="text-xs font-medium text-gray-500 mb-1 block">{tText('Solde initial')}</label><input type="number" className="input w-full" value={accForm.balance} onChange={(e) => setAccForm({ ...accForm, balance: e.target.value })} /></div>
              <div><label className="text-xs font-medium text-gray-500 mb-1 block">{tText('Devise')}</label><input className="input w-full" value={accForm.devise} onChange={(e) => setAccForm({ ...accForm, devise: e.target.value })} /></div>
              <div className="md:col-span-2 flex justify-end gap-3">
                <button onClick={() => setShowForm(false)} className="btn-sm px-4 py-2 rounded-lg glass-card">{tText('Annuler')}</button>
                <button onClick={() => createAccount.mutate()} disabled={!accForm.name.trim() || createAccount.isPending} className="btn-primary btn-sm">{tText('Créer')}</button>
              </div>
            </div>
          )}
          {loadingAccounts ? spinner : accounts.length === 0 ? <div className="glass-card p-10 text-center text-gray-500">{tText('Aucun compte')}</div> : (
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
              {accounts.map((a) => (
                <div key={String(a.id)} className="glass-card p-5">
                  <h3 className="font-semibold text-gray-800 dark:text-gray-200">{sv(a, 'name')}</h3>
                  <p className="text-xs text-gray-500">{sv(a, 'bankName')} • {sv(a, 'accountNumber')}</p>
                  <p className="text-lg font-bold text-teal-600 mt-2">{sv(a, 'balance')} {sv(a, 'devise')}</p>
                  <p className="text-xs text-gray-400">{sv(a, 'isActive') === 'true' ? tText('actif') : tText('inactif')}</p>
                </div>
              ))}
            </div>
          )}
        </>
      )}

      {tab === 'donations' && (
        <>
          {showForm && (
            <div className="glass-card p-6 mb-6 grid gap-4 md:grid-cols-2">
              <div><label className="text-xs font-medium text-gray-500 mb-1 block">{tText('Nom du donateur')}</label><input className="input w-full" value={donForm.donorName} onChange={(e) => setDonForm({ ...donForm, donorName: e.target.value })} disabled={donForm.isAnonymous} /></div>
              <div><label className="text-xs font-medium text-gray-500 mb-1 block">{tText('Montant *')}</label><input type="number" className="input w-full" value={donForm.amount} onChange={(e) => setDonForm({ ...donForm, amount: e.target.value })} /></div>
              <div><label className="text-xs font-medium text-gray-500 mb-1 block">{tText('Devise')}</label><input className="input w-full" value={donForm.devise} onChange={(e) => setDonForm({ ...donForm, devise: e.target.value })} /></div>
              <div><label className="text-xs font-medium text-gray-500 mb-1 block">{tText('Date')}</label><input type="date" className="input w-full" value={donForm.donationDate} onChange={(e) => setDonForm({ ...donForm, donationDate: e.target.value })} /></div>
              <div><label className="text-xs font-medium text-gray-500 mb-1 block">{tText('Objet')}</label><input className="input w-full" value={donForm.purpose} onChange={(e) => setDonForm({ ...donForm, purpose: e.target.value })} /></div>
              <label className="flex items-center gap-2 text-sm text-gray-600"><input type="checkbox" checked={donForm.isAnonymous} onChange={(e) => setDonForm({ ...donForm, isAnonymous: e.target.checked })} /> {tText('Anonyme')}</label>
              <div className="md:col-span-2 flex justify-end gap-3">
                <button onClick={() => setShowForm(false)} className="btn-sm px-4 py-2 rounded-lg glass-card">{tText('Annuler')}</button>
                <button onClick={() => createDonation.mutate()} disabled={!donForm.amount || Number(donForm.amount) <= 0 || createDonation.isPending} className="btn-primary btn-sm">{tText('Enregistrer')}</button>
              </div>
            </div>
          )}
          {loadingDonations ? spinner : donations.length === 0 ? <div className="glass-card p-10 text-center text-gray-500">{tText('Aucun don')}</div> : (
            <div className="space-y-3">
              {donations.map((d) => (
                <div key={String(d.id)} className="glass-card p-4 flex flex-wrap items-center gap-x-4 gap-y-1 text-sm">
                  <HandCoins className="w-4 h-4 text-teal-500" />
                  <span className="font-medium text-gray-800 dark:text-gray-200">{sv(d, 'isAnonymous') === 'true' ? tText('Anonyme') : sv(d, 'donorName')}</span>
                  <span className="text-teal-600 font-semibold">{sv(d, 'amount')} {sv(d, 'devise')}</span>
                  <span className="text-xs text-gray-500">{sv(d, 'purpose')}</span>
                  <span className="ml-auto text-xs text-gray-500">{sv(d, 'donationDate')}</span>
                </div>
              ))}
            </div>
          )}
        </>
      )}

      {tab === 'tontines' && (
        <>
          {showForm && (
            <div className="glass-card p-6 mb-6 grid gap-4 md:grid-cols-2">
              <div><label className="text-xs font-medium text-gray-500 mb-1 block">{tText('Nom *')}</label><input className="input w-full" value={tonForm.name} onChange={(e) => setTonForm({ ...tonForm, name: e.target.value })} /></div>
              <div><label className="text-xs font-medium text-gray-500 mb-1 block">{tText('Montant par tour *')}</label><input type="number" className="input w-full" value={tonForm.amountPerTurn} onChange={(e) => setTonForm({ ...tonForm, amountPerTurn: e.target.value })} /></div>
              <div><label className="text-xs font-medium text-gray-500 mb-1 block">{tText('Fréquence *')}</label>
                <select className="input w-full" value={tonForm.frequency} onChange={(e) => setTonForm({ ...tonForm, frequency: e.target.value })}>
                  <option value="WEEKLY">WEEKLY</option><option value="MONTHLY">MONTHLY</option><option value="QUARTERLY">QUARTERLY</option><option value="YEARLY">YEARLY</option>
                </select>
              </div>
              <div><label className="text-xs font-medium text-gray-500 mb-1 block">{tText('Date de début')}</label><input type="date" className="input w-full" value={tonForm.startDate} onChange={(e) => setTonForm({ ...tonForm, startDate: e.target.value })} /></div>
              <div className="md:col-span-2"><label className="text-xs font-medium text-gray-500 mb-1 block">{tText('Description')}</label><input className="input w-full" value={tonForm.description} onChange={(e) => setTonForm({ ...tonForm, description: e.target.value })} /></div>
              <div className="md:col-span-2 flex justify-end gap-3">
                <button onClick={() => setShowForm(false)} className="btn-sm px-4 py-2 rounded-lg glass-card">{tText('Annuler')}</button>
                <button onClick={() => createTontine.mutate()} disabled={!tonForm.name.trim() || !tonForm.amountPerTurn || createTontine.isPending} className="btn-primary btn-sm">{tText('Créer')}</button>
              </div>
            </div>
          )}
          <div className="glass-card p-4 mb-4">
            <h3 className="text-sm font-semibold text-gray-700 dark:text-gray-300 mb-2 inline-flex items-center gap-2"><Users className="w-4 h-4 text-teal-500" />{tText('Ajouter un membre')}</h3>
            <div className="grid gap-3 md:grid-cols-4">
              <select className="input" value={memberForm.tontineId} onChange={(e) => setMemberForm({ ...memberForm, tontineId: e.target.value })}>
                <option value="">{tText('Tontine…')}</option>
                {tontines.map((t) => <option key={String(t.id)} value={String(t.id)}>{sv(t, 'name')}</option>)}
              </select>
              <input className="input" placeholder={tText('UUID utilisateur')} value={memberForm.userId} onChange={(e) => setMemberForm({ ...memberForm, userId: e.target.value })} />
              <input type="number" className="input" placeholder={tText('Ordre de tour (optionnel)')} value={memberForm.turnOrder} onChange={(e) => setMemberForm({ ...memberForm, turnOrder: e.target.value })} />
              <button onClick={() => addMember.mutate()} disabled={!memberForm.tontineId || !memberForm.userId.trim() || addMember.isPending} className="btn-primary btn-sm">{tText('Ajouter')}</button>
            </div>
          </div>
          {loadingTontines ? spinner : tontines.length === 0 ? <div className="glass-card p-10 text-center text-gray-500">{tText('Aucune tontine')}</div> : (
            <div className="space-y-3">
              {tontines.map((t) => (
                <div key={String(t.id)} className="glass-card p-5">
                  <div className="flex flex-wrap items-center gap-x-4 gap-y-1">
                    <PiggyBank className="w-4 h-4 text-teal-500" />
                    <span className="font-semibold text-gray-800 dark:text-gray-200">{sv(t, 'name')}</span>
                    <span className="text-sm text-teal-600">{sv(t, 'amountPerTurn')} / {sv(t, 'frequency')}</span>
                    <span className="text-xs text-gray-500">{sv(t, 'startDate')} → {sv(t, 'endDate')}</span>
                    <button onClick={() => setOpenTontine(openTontine === String(t.id) ? '' : String(t.id))} className="ml-auto text-xs btn-sm px-3 py-1 rounded-lg glass-card">{tText('Détails')}</button>
                  </div>
                  {openTontine === String(t.id) && (
                    <div className="mt-3 border-t border-gray-200 dark:border-gray-700 pt-3 grid gap-4 md:grid-cols-2">
                      <div>
                        <h4 className="text-xs font-semibold text-gray-500 mb-2">{tText('Membres')}</h4>
                        {members.length === 0 ? <p className="text-xs text-gray-500">{tText('Aucun membre')}</p> : members.map((m) => (
                          <p key={String(m.id)} className="text-xs text-gray-600 py-0.5">{tText('Tour')} {sv(m, 'turnOrder')} — {sv(m, 'userId').slice(0, 8)}…</p>
                        ))}
                      </div>
                      <div>
                        <h4 className="text-xs font-semibold text-gray-500 mb-2">{tText('Versements')}</h4>
                        {payouts.length === 0 ? <p className="text-xs text-gray-500">{tText('Aucun versement')}</p> : payouts.map((p) => (
                          <p key={String(p.id)} className="text-xs text-gray-600 py-0.5">#{sv(p, 'turnNumber')} — {sv(p, 'amount')} — {sv(p, 'payoutDate')}</p>
                        ))}
                      </div>
                    </div>
                  )}
                </div>
              ))}
            </div>
          )}
        </>
      )}

      {tab === 'budgets' && (
        <>
          {showForm && (
            <div className="glass-card p-6 mb-6 grid gap-4 md:grid-cols-2">
              <div><label className="text-xs font-medium text-gray-500 mb-1 block">{tText('Catégorie *')}</label><input className="input w-full" value={budgetForm.categorie} onChange={(e) => setBudgetForm({ ...budgetForm, categorie: e.target.value })} /></div>
              <div><label className="text-xs font-medium text-gray-500 mb-1 block">{tText('Montant *')}</label><input type="number" className="input w-full" value={budgetForm.montant} onChange={(e) => setBudgetForm({ ...budgetForm, montant: e.target.value })} /></div>
              <div className="md:col-span-2 flex justify-end gap-3">
                <button onClick={() => setShowForm(false)} className="btn-sm px-4 py-2 rounded-lg glass-card">{tText('Annuler')}</button>
                <button onClick={() => upsertBudget.mutate()} disabled={!budgetForm.categorie.trim() || !budgetForm.montant || upsertBudget.isPending} className="btn-primary btn-sm">{tText('Enregistrer')}</button>
              </div>
            </div>
          )}
          <div className="flex items-center gap-3 mb-4">
            <label className="text-sm text-gray-600">{tText('Année')}</label>
            <input type="number" className="input w-32" value={annee} onChange={(e) => setAnnee(Number(e.target.value))} />
          </div>
          {loadingBudgets ? spinner : budgets.length === 0 ? <div className="glass-card p-10 text-center text-gray-500">{tText('Aucun budget')}</div> : (
            <div className="space-y-3">
              {budgets.map((b) => (
                <div key={String(b.id)} className="glass-card p-4 flex flex-wrap items-center gap-x-4 gap-y-1 text-sm">
                  <Scale className="w-4 h-4 text-teal-500" />
                  <span className="font-medium text-gray-800 dark:text-gray-200">{sv(b, 'categorie')}</span>
                  <span className="text-xs text-gray-500">{sv(b, 'annee')}</span>
                  <span className="text-teal-600">{sv(b, 'montant')}</span>
                  <span className="text-xs text-gray-500">{tText('Dépensé')} : {sv(b, 'depenseReelle')} ({sv(b, 'consommationPct')}%)</span>
                  <span className="text-xs px-2 py-0.5 rounded-full bg-teal-100 text-teal-700">{sv(b, 'statut')}</span>
                  <button onClick={() => deleteBudget.mutate(String(b.id))} className="ml-auto text-xs text-red-400 hover:text-red-300">{tText('Supprimer')}</button>
                </div>
              ))}
            </div>
          )}
        </>
      )}

      {tab === 'reconciliation' && (
        <div className="space-y-4">
          <button onClick={() => autoMatch.mutate()} disabled={autoMatch.isPending} className="btn-primary btn-sm inline-flex items-center gap-1"><RefreshCcw className="w-4 h-4" /> {tText('Rapprochement automatique')}</button>
          <div className="glass-card p-5">
            <h3 className="font-semibold text-gray-800 dark:text-gray-200 mb-2">{tText('Non rapprochés')}</h3>
            <pre className="text-xs text-gray-600 bg-gray-50 dark:bg-gray-800 rounded-lg p-3 overflow-auto max-h-96">{unmatched ? JSON.stringify(unmatched, null, 2) : tText('Chargement…')}</pre>
          </div>
          <div className="glass-card p-5">
            <h3 className="font-semibold text-gray-800 dark:text-gray-200 mb-2">{tText('Grand livre')}</h3>
            <pre className="text-xs text-gray-600 bg-gray-50 dark:bg-gray-800 rounded-lg p-3 overflow-auto max-h-96">{ledger ? JSON.stringify(ledger, null, 2) : tText('Chargement…')}</pre>
          </div>
        </div>
      )}

      {tab === 'reports' && (
        <div className="space-y-4">
          {summary && (
            <div className="glass-card p-5">
              <h3 className="font-semibold text-gray-800 dark:text-gray-200 mb-3 inline-flex items-center gap-2"><BarChart3 className="w-4 h-4" />{tText('Résumé')}</h3>
              <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
                {Object.entries(summary).map(([k, v]) => (
                  <div key={k}><p className="stat-value">{v == null ? '—' : typeof v === 'object' ? JSON.stringify(v) : String(v)}</p><p className="stat-label">{k}</p></div>
                ))}
              </div>
            </div>
          )}
          <div className="glass-card p-5">
            <h3 className="font-semibold text-gray-800 dark:text-gray-200 mb-2">{tText('Par catégorie')}</h3>
            {byCategory.length === 0 ? <p className="text-sm text-gray-500">{tText('Aucune donnée')}</p> : byCategory.map((r, i) => (
              <div key={i} className="text-sm text-gray-700 dark:text-gray-300 py-1 border-b border-gray-100 last:border-0">{Object.entries(r).map(([k, v]) => `${k}: ${v == null ? '—' : String(v)}`).join(' • ')}</div>
            ))}
          </div>
          <div className="glass-card p-5">
            <h3 className="font-semibold text-gray-800 dark:text-gray-200 mb-2">{tText('Flux de trésorerie')}</h3>
            {cashFlow.length === 0 ? <p className="text-sm text-gray-500">{tText('Aucune donnée')}</p> : cashFlow.map((r, i) => (
              <div key={i} className="text-sm text-gray-700 dark:text-gray-300 py-1 border-b border-gray-100 last:border-0">{Object.entries(r).map(([k, v]) => `${k}: ${v == null ? '—' : String(v)}`).join(' • ')}</div>
            ))}
          </div>
        </div>
      )}
    </div>
  );
}
