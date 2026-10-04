/**
 * Console « Propriété & délégation » — SPEC_ORGANISATION_DENOMINATION_V2 §7.2 / T-W7.
 *
 * <p>Ces endpoints existaient côté serveur (`TenantOwnershipController`) mais
 * <b>aucun composant ne les appelait</b> : `grep ownership frontend/src` ne
 * renvoyait rien. La fonctionnalité existait donc pour personne. Le client a
 * demandé explicitement « il peut déléguer d'autres admins » et « se défaire
 * de ce rôle et choisir un remplaçant » — c'est cet écran qui répond à cette
 * demande.
 *
 * <p><b>Sécurité :</b> la garde de route est alignée sur ADMIN/PASTEUR, mais
 * la garde de <b>métier</b> reste côté serveur : seul
 * {@code @authz.isTenantOwner()} peut céder la propriété ou nommer un
 * délégué. Un administrateur délégué qui masquerait ces boutons n'aurait pas
 * pour autant la sécurité — l'API refuse.
 */
import { useCallback, useEffect, useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import {
  AlertTriangle, CheckCircle2, Crown, Loader2, LogOut, ShieldCheck, UserCog, UserPlus, X,
} from 'lucide-react';
import api, { getErrorMessage } from '@/lib/api';
import { tText } from '@/i18n';
import { useAuth } from '@/contexts/AuthContext';

type AdminRow = {
  userId: string;
  email: string;
  name: string;
  joinedAt?: string;
};

type OwnershipView = {
  ownerUserId?: string | null;
  ownerEmail?: string | null;
  ownerName?: string | null;
  admins: AdminRow[];
};

export default function TenantOwnershipPage() {
  const queryClient = useQueryClient();
  const { user } = useAuth();
  const [error, setError] = useState('');
  const [notice, setNotice] = useState('');
  const [transferTarget, setTransferTarget] = useState('');
  const [replacementReason, setReplacementReason] = useState('');
  const [replacementOpen, setReplacementOpen] = useState(false);

  const { data, isLoading } = useQuery<OwnershipView>({
    queryKey: ['tenant', 'ownership'],
    queryFn: async () => (await api.get('/tenant/ownership')).data,
  });

  // Déclaré AVANT la requête de candidats : celle-ci s'appuie dessus pour ne
  // pas charger une liste que seul le propriétaire peut exploiter.
  const isOwner = !!data?.ownerUserId && data.ownerUserId === user?.id;

  // Un membre connecté peut être observé comme cible de délégation : on ne se
  // limite pas aux administrateurs déjà listés, sinon il n'y aurait aucun moyen
  // de nommer quelqu'un qui ne l'est pas encore.
  //
  // T-W7 : l'endpoint est `GET /tenant/ownership/promotable`, pas un
  // « /admin/members » — ce dernier n'existe pas, et le sélecteur serait resté
  // vide, rendant la délégation inatteignable.
  const { data: candidates } = useQuery<Array<{ userId: string; label: string }>>({
    queryKey: ['tenant', 'ownership', 'promotable'],
    queryFn: async () => {
      const items = (await api.get('/tenant/ownership/promotable')).data as Array<Record<string, unknown>>;
      return items
        .map((m) => ({
          userId: String(m.userId ?? ''),
          label: String(m.name || m.email || ''),
        }))
        .filter((m) => m.userId.length > 0 && m.label.length > 0);
    },
    enabled: !!data && isOwner,
  });

  const invalidate = useCallback(() => {
    queryClient.invalidateQueries({ queryKey: ['tenant', 'ownership'] });
  }, [queryClient]);

  const transferMutation = useMutation({
    mutationFn: async () => {
      if (!transferTarget) throw new Error(tText('Choisissez le membre qui recevra l’église.'));
      // Confirmation explicite : la cession est quasi irréversible et l'ancien
      // propriétaire perd l'ensemble des droits attachés à ce rôle.
      const confirmed = window.confirm(
        tText(
          'Confirmez-vous la cession ? Vous n’aurez plus les droits de propriétaire sur cette église, mais vous resterez administrateur.',
        ),
      );
      if (!confirmed) return null;
      return api.post('/tenant/ownership/transfer', { toUserId: transferTarget });
    },
    onSuccess: (result) => {
      if (result) {
        setNotice(tText('Propriété transmise. Un e-mail a été envoyé aux deux parties.'));
        setError('');
        setTransferTarget('');
        invalidate();
      }
    },
    onError: (err) => setError(getErrorMessage(err)),
  });

  const promoteMutation = useMutation({
    mutationFn: (userId: string) => api.post(`/tenant/ownership/members/${userId}/promote-admin`),
    onSuccess: () => { setNotice(tText('Administrateur délégué.')); setError(''); invalidate(); },
    onError: (err) => setError(getErrorMessage(err)),
  });

  const demoteMutation = useMutation({
    mutationFn: (userId: string) => api.post(`/tenant/ownership/members/${userId}/demote-admin`),
    onSuccess: () => { setNotice(tText('Délégation retirée.')); setError(''); invalidate(); },
    onError: (err) => setError(getErrorMessage(err)),
  });

  const replacementMutation = useMutation({
    mutationFn: () => api.post('/tenant/ownership/request-replacement', {
      reason: replacementReason.trim() || undefined,
    }),
    onSuccess: () => {
      setNotice(tText('Demande enregistrée — la plateforme (Super Admin) arbitrera.'));
      setError('');
      setReplacementOpen(false);
      setReplacementReason('');
    },
    onError: (err) => setError(getErrorMessage(err)),
  });

  if (isLoading) {
    return (
      <div className="flex justify-center py-16">
        <Loader2 className="h-7 w-7 animate-spin text-primary-500" />
      </div>
    );
  }

  return (
    <main className="p-6 space-y-6 max-w-4xl mx-auto">
      <header>
        <h1 className="text-2xl font-bold text-gray-900 dark:text-white font-display flex items-center gap-2">
          <Crown className="w-6 h-6 text-gold-500" /> {tText('Propriété & délégation')}
        </h1>
        <p className="text-sm text-gray-500 dark:text-gray-400">
          {tText(
            'Le propriétaire administre l’église. Il peut nommer des administrateurs delegués, puis leur céder l’église quand il le souhaite.',
          )}
        </p>
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

      <section className="rounded-xl border border-gray-200 dark:border-white/10 bg-white dark:bg-gray-900 p-5 space-y-2">
        <h2 className="font-semibold text-gray-900 dark:text-white flex items-center gap-2">
          <ShieldCheck className="w-4 h-4 text-primary-500" /> {tText('Propriétaire actuel')}
        </h2>
        {data?.ownerEmail ? (
          <p className="text-sm text-gray-600 dark:text-gray-300">
            <span className="font-medium">{data.ownerName || data.ownerEmail}</span>
            {data.ownerName ? ` — ${data.ownerEmail}` : ''}
          </p>
        ) : (
          <p className="text-sm text-amber-600 dark:text-amber-400">
            {tText('Aucun propriétaire désigné. Contactez la plateforme pour une reprise assistée.')}
          </p>
        )}
      </section>

      <section className="rounded-xl border border-gray-200 dark:border-white/10 bg-white dark:bg-gray-900 overflow-hidden">
        <header className="px-5 py-3 border-b font-semibold text-sm text-gray-800 dark:text-gray-100 flex items-center gap-2">
          <UserCog className="w-4 h-4 text-primary-500" /> {tText('Administrateurs delegués')}{' '}
          ({data?.admins?.length ?? 0})
        </header>
        <div className="divide-y divide-gray-100 dark:divide-white/5">
          {data?.admins?.length ? (
            data.admins.map((admin) => (
              <div key={admin.userId} className="px-5 py-3 flex items-center justify-between gap-3">
                <div className="min-w-0">
                  <p className="font-medium text-sm text-gray-900 dark:text-white truncate">
                    {admin.name || admin.email}
                  </p>
                  <p className="text-xs text-gray-500 dark:text-gray-400 truncate">{admin.email}</p>
                </div>
                {isOwner && (
                  <button
                    type="button"
                    onClick={() => demoteMutation.mutate(admin.userId)}
                    disabled={demoteMutation.isPending}
                    className="shrink-0 rounded-md border border-amber-500/30 px-3 py-1.5 text-xs text-amber-700 dark:text-amber-300 hover:bg-amber-500/10 disabled:opacity-50"
                  >
                    {tText('Retirer la delegation')}
                  </button>
                )}
              </div>
            ))
          ) : (
            <p className="px-5 py-8 text-center text-sm text-gray-400">
              {tText('Aucun administrateur délégué pour le moment.')}
            </p>
          )}
        </div>
      </section>

      {isOwner ? (
        <section className="rounded-xl border border-gray-200 dark:border-white/10 bg-white dark:bg-gray-900 p-5 space-y-4">
          <h2 className="font-semibold text-gray-900 dark:text-white flex items-center gap-2">
            <UserPlus className="w-4 h-4 text-primary-500" /> {tText('Nommer un administrateur')}
          </h2>
          <div className="grid gap-3 sm:grid-cols-[1fr_auto]">
            <select
              value={transferTarget}
              onChange={(e) => setTransferTarget(e.target.value)}
              className="rounded-lg border border-gray-300 dark:border-white/15 bg-white dark:bg-gray-800 px-3 py-2 text-sm text-gray-900 dark:text-gray-100"
            >
              <option value="">{tText('Choisir un membre')}</option>
              {(candidates ?? []).map((c) => (
                <option key={c.userId} value={c.userId}>{c.label}</option>
              ))}
            </select>
            <button
              type="button"
              onClick={() => promoteMutation.mutate(transferTarget)}
              disabled={!transferTarget || promoteMutation.isPending}
              className="rounded-lg bg-primary-600 px-4 py-2 text-sm font-medium text-white hover:bg-primary-500 disabled:opacity-50"
            >
              {promoteMutation.isPending ? <Loader2 className="w-4 h-4 animate-spin" /> : tText('Nommer')}
            </button>
          </div>

          <div className="border-t pt-4 space-y-3">
            <h3 className="text-sm font-semibold text-gray-800 dark:text-gray-100 flex items-center gap-2">
              <LogOut className="w-4 h-4 text-red-500" /> {tText('Céder la propriété')}
            </h3>
            <p className="text-xs text-gray-500 dark:text-gray-400">
              {tText(
                'Le membre choisi devient propriétaire. Vous resterez administrateur délégué et pourrez continuer à gérer l’église.',
              )}
            </p>
            <button
              type="button"
              onClick={() => transferMutation.mutate()}
              disabled={!transferTarget || transferMutation.isPending}
              className="rounded-lg border border-red-500/30 px-4 py-2 text-sm font-medium text-red-700 dark:text-red-300 hover:bg-red-500/10 disabled:opacity-50"
            >
              {transferMutation.isPending ? <Loader2 className="w-4 h-4 animate-spin" /> : tText('Ceder')}
            </button>
          </div>
        </section>
      ) : (
        <section className="rounded-xl border border-amber-200 dark:border-amber-500/20 bg-amber-50 dark:bg-amber-500/5 p-5 space-y-3">
          <h2 className="font-semibold text-amber-800 dark:text-amber-300 flex items-center gap-2">
            <AlertTriangle className="w-4 h-4" /> {tText('Propriétaire indisponible ?')}
          </h2>
          <p className="text-sm text-amber-700 dark:text-amber-300">
            {tText(
              'Si le propriétaire est absent ou injoignable, un administrateur délégué peut demander un remplacement. La plateforme arbitre — jamais automatiquement.',
            )}
          </p>
          {replacementOpen ? (
            <div className="space-y-3">
              <textarea
                value={replacementReason}
                onChange={(e) => setReplacementReason(e.target.value)}
                rows={3}
                placeholder={tText('Expliquez la situation')}
                className="w-full rounded-lg border border-amber-300 dark:border-white/15 bg-white dark:bg-gray-800 px-3 py-2 text-sm text-gray-900 dark:text-gray-100"
              />
              <div className="flex gap-2">
                <button
                  type="button"
                  onClick={() => replacementMutation.mutate()}
                  disabled={replacementMutation.isPending}
                  className="rounded-lg bg-amber-600 px-4 py-2 text-sm font-medium text-white hover:bg-amber-500 disabled:opacity-50"
                >
                  {replacementMutation.isPending ? <Loader2 className="w-4 h-4 animate-spin" /> : tText('Envoyer la demande')}
                </button>
                <button
                  type="button"
                  onClick={() => setReplacementOpen(false)}
                  className="rounded-lg border border-amber-300 dark:border-white/15 px-3 py-2 text-sm text-amber-800 dark:text-amber-300"
                >
                  <X className="w-4 h-4" />
                </button>
              </div>
            </div>
          ) : (
            <button
              type="button"
              onClick={() => setReplacementOpen(true)}
              className="rounded-lg border border-amber-500/30 px-4 py-2 text-sm font-medium text-amber-700 dark:text-amber-300 hover:bg-amber-500/10"
            >
              {tText('Demander un remplacement')}
            </button>
          )}
        </section>
      )}

      <p className="flex items-start gap-2 text-xs text-gray-400 dark:text-gray-500">
        <CheckCircle2 className="w-3.5 h-3.5 mt-0.5 shrink-0" />
        {tText(
          'Toute cession est journalisée et notifiée par e-mail aux deux parties. Si un changement vous surprend, contactez immédiatement la plateforme.',
        )}
      </p>
    </main>
  );
}
