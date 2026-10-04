/**
 * Transfert de membre — SPEC_ORGANISATION_DENOMINATION_V2 §7.2 / T-W6, §4.4.
 *
 * <p>Le besoin énoncé par le client : « un membre qui change d'église ne se
 * réinscrit pas ». L'écran applique le contrat du backend — il n'invente
 * aucune règle :
 * <ul>
 *   <li><b>même réseau</b> (même dénomination) → <b>transfert</b> : le
 *       parcours pastoral est conservé, c'est la même personne ;</li>
 *   <li><b>réseaux différents</b> → simple <b>adhésion</b> : il garde son
 *       appartenance d'origine, il rejoint la nouvelle en plus.</li>
 * </ul>
 *
 * <p>La distinction est demandée au serveur ({@code /transfer/preview}) et
 * <b>montrée</b> avant confirmation : annoncer « vous rejoignez » quand
 * l'historique pastoral sera préservé — ou l'inverse — serait trompeur.
 */
import { useState } from 'react';
import { useMutation } from '@tanstack/react-query';
import { ArrowRightLeft, CheckCircle2, KeyRound, Loader2, ShieldQuestion } from 'lucide-react';
import api, { getErrorMessage } from '@/lib/api';
import { tText } from '@/i18n';
import { useAuth } from '@/contexts/AuthContext';
import { useNavigate } from 'react-router-dom';

type Preview = {
  sameNetwork: boolean;
  alreadyMember: boolean;
  activeInOther: boolean;
  fromChurch: string;
  toChurch: string;
  toKind: string;
  willTransfer: boolean;
};

type TransferResponse = {
  status: string;
  fromTenantId?: string | null;
  toTenantId?: string | null;
  toChurch: string;
  accessToken?: string;
  refreshToken?: string;
};

export default function TransferPage() {
  const { adoptSession } = useAuth();
  const navigate = useNavigate();
  const [code, setCode] = useState('');
  const [reason, setReason] = useState('');
  const [preview, setPreview] = useState<Preview | null>(null);
  const [error, setError] = useState('');
  const [done, setDone] = useState<TransferResponse | null>(null);

  const previewMutation = useMutation({
    mutationFn: async () => {
      const res = await api.get('/tenant/transfer/preview', {
        params: { code: code.trim() },
      });
      return res.data as Preview;
    },
    onSuccess: (data) => { setPreview(data); setError(''); },
    onError: (err) => { setPreview(null); setError(getErrorMessage(err)); },
  });

  const transferMutation = useMutation({
    mutationFn: async () => {
      const res = await api.post('/tenant/transfer', {
        code: code.trim(),
        reason: reason.trim() || undefined,
      });
      return res.data as TransferResponse;
    },
    onSuccess: (data) => {
      // T-B0bis : le backend réémet les jetons sur l'organisation d'accueil.
      // Les adopter avant de naviguer est indispensable — sinon le claim
      // tenantId du jeton courant continue de désigner l'ancienne église et le
      // membre atterrit chez lui, contrairement à ce qui est affiché.
      if (data.accessToken) {
        adoptSession({ accessToken: data.accessToken, refreshToken: data.refreshToken ?? '' });
      }
      setDone(data);
      setError('');
    },
    onError: (err) => setError(getErrorMessage(err)),
  });

  if (done) {
    return (
      <main className="p-6 max-w-xl mx-auto space-y-5 text-center">
        <div className="mx-auto inline-flex items-center justify-center w-16 h-16 rounded-2xl bg-green-500/10 border border-green-500/20">
          <CheckCircle2 className="w-8 h-8 text-green-500" />
        </div>
        <h1 className="text-2xl font-bold text-gray-900 dark:text-white font-display">
          {done.status === 'TRANSFERRED'
            ? tText('Vous avez été transféré')
            : done.status === 'ALREADY_MEMBER'
              ? tText('Vous êtes déjà membre')
              : tText('Vous avez rejoint cette église')}
        </h1>
        <p className="text-sm text-gray-500 dark:text-gray-400">{done.toChurch}</p>
        {done.status === 'TRANSFERRED' && (
          <p className="text-sm text-gray-500 dark:text-gray-400">
            {tText('Votre parcours pastoral est conservé : vous restez la même personne.')}
          </p>
        )}
        <button
          type="button"
          onClick={() => navigate('/dashboard', { replace: true })}
          className="inline-flex items-center gap-2 rounded-xl bg-gradient-to-r from-primary-600 to-primary-500 px-5 py-3 text-sm font-medium text-white"
        >
          {tText('Entrer maintenant')} <ArrowRightLeft className="w-4 h-4" />
        </button>
      </main>
    );
  }

  return (
    <main className="p-6 max-w-xl mx-auto space-y-6">
      <header className="text-center">
        <div className="mx-auto inline-flex items-center justify-center w-14 h-14 rounded-2xl bg-primary-500/10 border border-primary-500/20 mb-4">
          <ArrowRightLeft className="w-7 h-7 text-primary-500" />
        </div>
        <h1 className="text-2xl font-bold text-gray-900 dark:text-white font-display">
          {tText('Changer d’église')}
        </h1>
        <p className="mt-2 text-sm text-gray-500 dark:text-gray-400">
          {tText(
            'Vous ne vous réinscrivez pas : votre compte, votre mot de passe et votre parcours pastoral vous suivent.',
          )}
        </p>
      </header>

      {error && (
        <div className="rounded-xl bg-red-500/10 border border-red-500/20 p-3.5">
          <p className="text-sm text-red-600 dark:text-red-300">{error}</p>
        </div>
      )}

      <div className="space-y-3">
        <div className="flex gap-2">
          <div className="relative flex-1">
            <KeyRound className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400" />
            <input
              value={code}
              onChange={(e) => setCode(e.target.value.toUpperCase())}
              onKeyDown={(e) => { if (e.key === 'Enter') void previewMutation.mutate(); }}
              placeholder={tText("Code de la nouvelle église")}
              className="w-full rounded-xl bg-gray-100/80 dark:bg-white/5 border border-gray-200 dark:border-white/10 text-gray-900 dark:text-white placeholder-gray-400 pl-9 pr-4 py-3 text-sm font-mono uppercase focus:outline-none focus:border-primary-500/50"
            />
          </div>
          <button
            type="button"
            onClick={() => void previewMutation.mutate()}
            disabled={previewMutation.isPending || !code.trim()}
            className="rounded-xl bg-primary-600 px-4 py-3 text-sm font-medium text-white disabled:opacity-50 hover:bg-primary-500"
          >
            {previewMutation.isPending ? <Loader2 className="w-4 h-4 animate-spin" /> : tText('Vérifier')}
          </button>
        </div>

        {preview && (
          <div className="rounded-2xl border border-gray-200 dark:border-white/10 bg-white/70 dark:bg-white/5 p-5 space-y-4">
            <div className="flex items-center gap-3">
              <p className="font-semibold text-gray-900 dark:text-white">{preview.toChurch}</p>
            </div>
            {preview.alreadyMember ? (
              <p className="text-sm rounded-lg bg-sky-500/10 border border-sky-500/20 text-sky-700 dark:text-sky-300 px-3 py-2">
                {tText('Vous êtes déjà membre de cette église : rien à faire.')}
              </p>
            ) : preview.willTransfer ? (
              <>
                <p className="text-sm rounded-lg bg-emerald-500/10 border border-emerald-500/20 text-emerald-700 dark:text-emerald-300 px-3 py-2">
                  {tText(
                    `Bienvenue ! Vous êtes déjà membre de la même dénomination. Vous rejoignez ${preview.toChurch} — votre parcours pastoral est conservé.`,
                  )}
                </p>
                <label className="block text-sm">
                  <span className="text-xs font-medium text-gray-600 dark:text-gray-300">
                    {tText('Motif du transfert (facultatif)')}
                  </span>
                  <input
                    value={reason}
                    onChange={(e) => setReason(e.target.value)}
                    placeholder={tText('Déménagement, évolution…')}
                    className="mt-1 w-full rounded-lg border border-gray-300 dark:border-white/15 bg-white dark:bg-gray-800 px-3 py-2 text-sm text-gray-900 dark:text-gray-100"
                  />
                </label>
                <button
                  type="button"
                  onClick={() => transferMutation.mutate()}
                  disabled={transferMutation.isPending}
                  className="w-full py-3 rounded-xl bg-gradient-to-r from-primary-600 to-primary-500 text-white font-medium text-sm flex items-center justify-center gap-2 disabled:opacity-50"
                >
                  {transferMutation.isPending ? <Loader2 className="w-4 h-4 animate-spin" /> : <ArrowRightLeft className="w-4 h-4" />}
                  {tText('Confirmer le transfert')}
                </button>
              </>
            ) : (
              <>
                <p className="text-sm rounded-lg bg-amber-500/10 border border-amber-500/20 text-amber-700 dark:text-amber-300 px-3 py-2 flex items-start gap-2">
                  <ShieldQuestion className="w-4 h-4 mt-0.5 shrink-0" />
                  <span>
                    {tText(
                      `Cette église n’appartient pas à votre réseau actuel. Vous allez la rejoindre en plus${preview.activeInOther ? ' — votre adhésion actuelle est conservée' : ''}.`,
                    )}
                  </span>
                </p>
                <button
                  type="button"
                  onClick={() => transferMutation.mutate()}
                  disabled={transferMutation.isPending}
                  className="w-full py-3 rounded-xl bg-gradient-to-r from-primary-600 to-primary-500 text-white font-medium text-sm flex items-center justify-center gap-2 disabled:opacity-50"
                >
                  {transferMutation.isPending ? <Loader2 className="w-4 h-4 animate-spin" /> : null}
                  {tText('Rejoindre cette église')}
                </button>
              </>
            )}
          </div>
        )}
      </div>

      <p className="flex items-center gap-2 text-xs text-gray-400 dark:text-gray-500">
        <ShieldQuestion className="w-4 h-4 shrink-0" />
        {tText('Aucun nouveau compte, aucun nouveau mot de passe : c’est la même identité.')}
      </p>
    </main>
  );
}
