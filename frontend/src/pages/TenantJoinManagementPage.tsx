/**
 * Codes d'entrée & rejointure — SPEC_ORGANISATION_DENOMINATION_V2 §7.2 / T-W8.
 *
 * <p><b>F14 — ce que l'écran ne savait pas faire.</b> La version précédente
 * proposait « Nouveau code » via deux `window.prompt` et n'envoyait
 * <b>jamais</b> `orgNodeId` : impossible de produire le code d'un campus,
 * alors que c'est une demande explicite du client (« produire un code ou un
 * lien pour chaque sous-église »). Le backend savait le faire ; l'écran non.
 *
 * <p><b>F21 — le lien, pas seulement le code.</b> Le lien `/j/<slug>` est la
 * porte la plus fluide (clic, zéro saisie) ; le code reste affiché pour ceux
 * qui l'ont en dictée. Les deux sont copiables en un clic.
 */
import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import {
  CheckCircle2, Copy, KeyRound, Link2, Loader2, Plus, RefreshCw, ShieldOff, Users, X,
} from 'lucide-react';
import api, { getErrorMessage } from '@/lib/api';
import { tText } from '@/i18n';

type JoinCode = {
  id: string;
  code: string;
  label?: string | null;
  joinMode: string;
  isActive: boolean;
  orgNodeId?: string | null;
  /** Ajouté par T-B4 : sans lui, le lien d'invitation n'était pas construisible. */
  tenantSlug?: string | null;
  createdAt: string;
};

type JoinRequest = {
  id: string;
  code: string;
  email: string;
  userId: string;
  status: string;
  createdAt: string;
};

type SubChurch = {
  id: string;
  name: string;
  type: string;
  hasActiveCode: boolean;
};

const MAIN_CHURCH = '__root__';

export default function TenantJoinManagementPage() {
  const queryClient = useQueryClient();
  const [error, setError] = useState('');
  const [notice, setNotice] = useState('');
  const [copied, setCopied] = useState<string | null>(null);
  const [formOpen, setFormOpen] = useState(false);
  const [label, setLabel] = useState('');
  const [target, setTarget] = useState<string>(MAIN_CHURCH);
  const [joinMode, setJoinMode] = useState<'OPEN' | 'APPROVAL'>('OPEN');

  const { data: codes, isLoading } = useQuery<JoinCode[]>({
    queryKey: ['tenant', 'join-codes'],
    queryFn: async () => (await api.get('/tenant/join-codes')).data,
  });

  const { data: subChurches } = useQuery<SubChurch[]>({
    queryKey: ['tenant', 'join-codes', 'sub-churches'],
    queryFn: async () => (await api.get('/tenant/join-codes/sub-churches')).data,
  });

  const { data: requests } = useQuery<JoinRequest[]>({
    queryKey: ['tenant', 'join-requests'],
    queryFn: async () => (await api.get('/tenant/join-requests')).data,
  });

  const refreshCodes = () => {
    queryClient.invalidateQueries({ queryKey: ['tenant', 'join-codes'] });
  };

  const inviteLink = (c: JoinCode) =>
    c.tenantSlug ? `${window.location.origin}/j/${c.tenantSlug}` : '';

  const copy = async (text: string, key: string) => {
    if (!text) return;
    try {
      await navigator.clipboard.writeText(text);
      setCopied(key);
      window.setTimeout(() => setCopied(null), 2000);
    } catch {
      setError(tText('Copie impossible : le texte reste sélectionnable à l’écran.'));
    }
  };

  const createMutation = useMutation({
    mutationFn: async () => {
      if (!label.trim()) {
        throw new Error(tText('Donnez un nom à ce code (ex : Bethel, Campus Nord).'));
      }
      return api.post('/tenant/join-codes', {
        label: label.trim(),
        // `undefined` = code principal de l'église. Le backend désactive alors
        // le code racine précédent (invariant D9, faille F16).
        orgNodeId: target === MAIN_CHURCH ? undefined : target,
        joinMode,
      });
    },
    onSuccess: () => {
      setNotice(tText('Code généré. Partagez le lien ou le code à vos membres.'));
      setError('');
      setFormOpen(false);
      setLabel('');
      setTarget(MAIN_CHURCH);
      setJoinMode('OPEN');
      refreshCodes();
    },
    onError: (err) => setError(getErrorMessage(err)),
  });

  const rotateMutation = useMutation({
    mutationFn: async (id: string) => api.post(`/tenant/join-codes/${id}/rotate`),
    onSuccess: () => { setNotice(tText('Code régénéré : l’ancien ne fonctionne plus.')); setError(''); refreshCodes(); },
    onError: (err) => setError(getErrorMessage(err)),
  });

  const toggleMutation = useMutation({
    mutationFn: async ({ id, isActive }: { id: string; isActive: boolean }) =>
      api.patch(`/tenant/join-codes/${id}`, { isActive: !isActive }),
    onSuccess: () => refreshCodes(),
    onError: (err) => setError(getErrorMessage(err)),
  });

  const decideMutation = useMutation({
    mutationFn: async ({ id, action }: { id: string; action: 'approve' | 'reject' }) =>
      api.post(`/tenant/join-requests/${id}/${action}`),
    onSuccess: (_d, vars) => {
      setNotice(vars.action === 'approve' ? tText('Demande approuvée.') : tText('Demande rejetée.'));
      setError('');
      queryClient.invalidateQueries({ queryKey: ['tenant', 'join-requests'] });
      refreshCodes();
    },
    onError: (err) => setError(getErrorMessage(err)),
  });

  const nodeName = (nodeId?: string | null) =>
    subChurches?.find((s) => s.id === nodeId)?.name ?? null;

  if (isLoading) {
    return (
      <div className="flex justify-center py-16">
        <Loader2 className="h-7 w-7 animate-spin text-primary-500" />
      </div>
    );
  }

  return (
    <main className="p-6 space-y-6 max-w-5xl mx-auto">
      <header className="flex items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-gray-900 dark:text-white font-display flex items-center gap-2">
            <KeyRound className="w-6 h-6 text-primary-500" /> {tText('Codes d’entrée & rejointure')}
          </h1>
          <p className="text-sm text-gray-500 dark:text-gray-400">
            {tText('Un lien par église ou sous-église. Un membre ne le saisit qu’une fois : ensuite, il entre directement.')}
          </p>
        </div>
        <button
          type="button"
          onClick={() => setFormOpen((v) => !v)}
          className="inline-flex items-center gap-2 rounded-lg bg-primary-600 px-3.5 py-2 text-sm font-medium text-white hover:bg-primary-500"
        >
          {formOpen ? <X className="w-4 h-4" /> : <Plus className="w-4 h-4" />}
          {formOpen ? tText('Annuler') : tText('Nouveau code')}
        </button>
      </header>

      {error && (
        <div className="rounded-lg border border-red-200 bg-red-50 p-3 text-sm text-red-700 dark:bg-red-500/10 dark:text-red-300 dark:border-red-500/20">
          {error}
        </div>
      )}
      {notice && (
        <div className="rounded-lg border border-emerald-200 bg-emerald-50 p-3 text-sm text-emerald-700 dark:bg-emerald-500/10 dark:text-emerald-300 dark:border-emerald-500/20">
          {notice}
        </div>
      )}

      {formOpen && (
        <section className="rounded-xl border border-primary-200 dark:border-primary-500/20 bg-primary-500/5 p-5 space-y-4">
          <div className="grid gap-3 sm:grid-cols-2">
            <label className="block text-sm">
              <span className="text-xs font-medium text-gray-600 dark:text-gray-300">
                {tText('Nom du code')}
              </span>
              <input
                value={label}
                onChange={(e) => setLabel(e.target.value)}
                placeholder={tText('Église principale, Campus Nord…')}
                className="mt-1 w-full rounded-lg border border-gray-300 dark:border-white/15 bg-white dark:bg-gray-800 px-3 py-2 text-sm text-gray-900 dark:text-gray-100"
              />
            </label>
            <label className="block text-sm">
              <span className="text-xs font-medium text-gray-600 dark:text-gray-300">
                {tText('Église concernée')}
              </span>
              <select
                value={target}
                onChange={(e) => setTarget(e.target.value)}
                className="mt-1 w-full rounded-lg border border-gray-300 dark:border-white/15 bg-white dark:bg-gray-800 px-3 py-2 text-sm text-gray-900 dark:text-gray-100"
              >
                <option value={MAIN_CHURCH}>{tText('Église principale (racine)')}</option>
                {(subChurches ?? []).map((s) => (
                  <option key={s.id} value={s.id}>
                    {s.name} · {s.type}
                  </option>
                ))}
              </select>
            </label>
          </div>
          <fieldset className="space-y-2">
            <legend className="text-xs font-medium text-gray-600 dark:text-gray-300">
              {tText('Mode d’adhésion')}
            </legend>
            <div className="flex flex-wrap gap-3">
              {([['OPEN', tText('Entrée directe')], ['APPROVAL', tText('Validation par un responsable')]] as const).map(
                ([value, labelText]) => (
                  <label key={value} className="flex items-center gap-2 text-sm text-gray-700 dark:text-gray-300">
                    <input
                      type="radio"
                      name="joinMode"
                      value={value}
                      checked={joinMode === value}
                      onChange={() => setJoinMode(value)}
                      className="h-4 w-4 text-primary-600"
                    />
                    {labelText}
                  </label>
                ),
              )}
            </div>
          </fieldset>
          <button
            type="button"
            onClick={() => createMutation.mutate()}
            disabled={createMutation.isPending}
            className="inline-flex items-center gap-2 rounded-lg bg-primary-600 px-4 py-2 text-sm font-medium text-white hover:bg-primary-500 disabled:opacity-50"
          >
            {createMutation.isPending ? <Loader2 className="w-4 h-4 animate-spin" /> : <Plus className="w-4 h-4" />}
            {tText('Générer')}
          </button>
          {target === MAIN_CHURCH && codes?.some((c) => !c.orgNodeId && c.isActive) && (
            <p className="text-xs text-amber-600 dark:text-amber-400">
              {tText('Un nouveau code racine désactive le code principal précédent : les liens partagés restent valides, l’ancien code ne fonctionne plus.')}
            </p>
          )}
        </section>
      )}

      <section className="rounded-xl border border-gray-200 dark:border-white/10 bg-white dark:bg-gray-900 overflow-hidden">
        <header className="px-5 py-3 border-b font-semibold text-sm text-gray-800 dark:text-gray-100">
          {tText('Codes d’accès')} ({codes?.length ?? 0})
        </header>
        <div className="divide-y divide-gray-100 dark:divide-white/5">
          {codes?.length ? (
            codes.map((c) => {
              const link = inviteLink(c);
              const sub = nodeName(c.orgNodeId);
              return (
                <div key={c.id} className="px-5 py-4 flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
                  <div className="min-w-0 space-y-1">
                    <p className={`font-mono font-bold text-sm ${c.isActive ? 'text-gray-900 dark:text-white' : 'text-gray-400 line-through'}`}>
                      {c.code}
                    </p>
                    <p className="text-xs text-gray-500 dark:text-gray-400">
                      {c.label || tText('Sans libellé')}
                      {sub ? ` · ${sub}` : ''}
                      {' · '}
                      {c.joinMode === 'APPROVAL' ? tText('sur validation') : tText('entrée directe')}
                    </p>
                    {link && (
                      <p className="flex items-center gap-1.5 text-xs font-mono text-primary-600 dark:text-primary-400">
                        <Link2 className="w-3 h-3 shrink-0" />
                        <span className="truncate">{link}</span>
                      </p>
                    )}
                  </div>
                  <div className="flex items-center gap-1.5 shrink-0 flex-wrap">
                    {link && (
                      <button
                        type="button"
                        title={tText('Copier le lien')}
                        onClick={() => void copy(link, `link-${c.id}`)}
                        className="rounded-md border border-primary-200 px-2 py-1.5 text-xs text-primary-700 dark:text-primary-300 hover:bg-primary-500/10"
                      >
                        {copied === `link-${c.id}` ? <CheckCircle2 className="w-3.5 h-3.5" /> : <Link2 className="w-3.5 h-3.5" />}
                      </button>
                    )}
                    <button
                      type="button"
                      title={tText('Copier le code')}
                      onClick={() => void copy(c.code, `code-${c.id}`)}
                      className="rounded-md border px-2 py-1.5 text-xs hover:bg-gray-50 dark:hover:bg-white/5"
                    >
                      {copied === `code-${c.id}` ? <CheckCircle2 className="w-3.5 h-3.5" /> : <Copy className="w-3.5 h-3.5" />}
                    </button>
                    <button
                      type="button"
                      title={tText('Régénérer')}
                      onClick={() => rotateMutation.mutate(c.id)}
                      disabled={rotateMutation.isPending}
                      className="rounded-md border px-2 py-1.5 text-xs hover:bg-gray-50 dark:hover:bg-white/5 disabled:opacity-50"
                    >
                      <RefreshCw className="h-3.5 w-3.5" />
                    </button>
                    <button
                      type="button"
                      title={c.isActive ? tText('Désactiver') : tText('Réactiver')}
                      onClick={() => toggleMutation.mutate({ id: c.id, isActive: c.isActive })}
                      disabled={toggleMutation.isPending}
                      className={`rounded-md border px-2 py-1.5 text-xs disabled:opacity-50 ${c.isActive ? 'border-amber-500/30 text-amber-600 dark:text-amber-300 hover:bg-amber-500/10' : 'border-emerald-500/30 text-emerald-600 dark:text-emerald-300 hover:bg-emerald-500/10'}`}
                    >
                      {c.isActive ? <ShieldOff className="h-3.5 w-3.5" /> : <CheckCircle2 className="h-3.5 w-3.5" />}
                    </button>
                  </div>
                </div>
              );
            })
          ) : (
            <p className="px-5 py-8 text-center text-sm text-gray-400">
              {tText('Aucun code. Créez-en un pour accueillir vos membres.')}
            </p>
          )}
        </div>
      </section>

      <section className="rounded-xl border border-gray-200 dark:border-white/10 bg-white dark:bg-gray-900 overflow-hidden">
        <header className="px-5 py-3 border-b font-semibold text-sm text-gray-800 dark:text-gray-100 flex items-center gap-2">
          <Users className="w-4 h-4" /> {tText('Demandes en attente')} ({requests?.length ?? 0})
        </header>
        <div className="divide-y divide-gray-100 dark:divide-white/5">
          {requests?.length ? (
            requests.map((r) => (
              <div key={r.id} className="px-5 py-3 flex items-center justify-between gap-3">
                <div className="min-w-0">
                  <p className="font-medium text-sm text-gray-900 dark:text-white truncate">
                    {r.email || r.userId || tText('Compte')}
                  </p>
                  <p className="text-xs font-mono text-gray-400">
                    {r.code} · {new Date(r.createdAt).toLocaleDateString()}
                  </p>
                </div>
                <div className="flex items-center gap-2 shrink-0">
                  <button
                    type="button"
                    onClick={() => decideMutation.mutate({ id: r.id, action: 'approve' })}
                    disabled={decideMutation.isPending}
                    className="rounded-lg bg-emerald-600 px-3 py-1.5 text-xs font-medium text-white hover:bg-emerald-500 disabled:opacity-50"
                  >
                    {tText('Approuver')}
                  </button>
                  <button
                    type="button"
                    onClick={() => decideMutation.mutate({ id: r.id, action: 'reject' })}
                    disabled={decideMutation.isPending}
                    className="rounded-lg border border-gray-200 dark:border-white/10 px-3 py-1.5 text-xs font-medium text-gray-600 dark:text-gray-300 hover:bg-gray-50 dark:hover:bg-white/5 disabled:opacity-50"
                  >
                    {tText('Rejeter')}
                  </button>
                </div>
              </div>
            ))
          ) : (
            <p className="px-5 py-8 text-center text-sm text-gray-400">{tText('Aucune demande en attente.')}</p>
          )}
        </div>
      </section>
    </main>
  );
}
