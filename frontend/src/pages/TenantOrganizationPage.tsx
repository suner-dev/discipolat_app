/**
 * Réseau d'une dénomination — SPEC_ORGANISATION_DENOMINATION_V2 §7.2 / T-W5.
 *
 * <p>Matérialise la §1.3 : une organisation racine (la « communauté
 * WhatsApp » du client) qui accueille des églises filles. L'écran présente
 * l'arborescence, le nombre d'organisations rattachées et le lien d'invitation
 * de chacune.
 *
 * <p><b>D7 — agrégats seulement.</b> Aucune donnée nominative d'une autre
 * église : pour voir le contenu d'une église fille, le Super Admin passe par
 * l'impersonation journalisée. Cette page affiche donc des noms
 * d'organisations et des compteurs, rien d'autre.
 */
import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import {
  Building2, CheckCircle2, Copy, Link2, Loader2, Network, Plus, X,
} from 'lucide-react';
import api, { getErrorMessage } from '@/lib/api';
import { tText } from '@/i18n';

type OrgNode = {
  id: string;
  name: string;
  slug: string;
  kind: string;
  parentTenantId?: string | null;
  rootTenantId?: string | null;
  status: string;
  plan: string;
  childCount: number;
  invitePath?: string | null;
};

const KIND_LABEL: Record<string, string> = {
  CHURCH: 'Église',
  DENOMINATION: 'Dénomination',
  ASSOCIATION: 'Association',
  ORGANIZATION: 'Organisation',
  MEGA_ASSOCIATION: 'Méga-association',
};

export default function TenantOrganizationPage() {
  const queryClient = useQueryClient();
  const [error, setError] = useState('');
  const [notice, setNotice] = useState('');
  const [copied, setCopied] = useState<string | null>(null);
  const [formOpen, setFormOpen] = useState(false);
  const [name, setName] = useState('');

  const { data: current, isLoading } = useQuery<OrgNode & { children?: OrgNode[] }>({
    queryKey: ['tenant', 'organization'],
    queryFn: async () => (await api.get('/tenant/organization')).data,
  });

  const { data: network } = useQuery<OrgNode[]>({
    queryKey: ['tenant', 'organization', 'network'],
    queryFn: async () => (await api.get('/tenant/organization/network')).data,
  });

  const createMutation = useMutation({
    mutationFn: async () => {
      if (!name.trim()) throw new Error(tText('Donnez un nom à la nouvelle église.'));
      return api.post('/tenant/organization/sub-churches', {
        name: name.trim(),
        mode: 'autonomous',
      });
    },
    onSuccess: () => {
      setNotice(tText('Église créée. Elle a son propre code d’entrée.'));
      setError('');
      setFormOpen(false);
      setName('');
      queryClient.invalidateQueries({ queryKey: ['tenant', 'organization'] });
    },
    onError: (err) => setError(getErrorMessage(err)),
  });

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

  if (isLoading) {
    return (
      <div className="flex justify-center py-16">
        <Loader2 className="h-7 w-7 animate-spin text-primary-500" />
      </div>
    );
  }

  const children = current?.children ?? [];
  const all = network ?? (current ? [current, ...children] : []);

  return (
    <main className="p-6 space-y-6 max-w-5xl mx-auto">
      <header className="flex items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-gray-900 dark:text-white font-display flex items-center gap-2">
            <Network className="w-6 h-6 text-primary-500" /> {tText('Mon réseau d’églises')}
          </h1>
          <p className="text-sm text-gray-500 dark:text-gray-400">
            {tText(
              'Une dénomination peut fédérer plusieurs églises. Chacune a ses membres, ses quotas et son code d’entrée.',
            )}
          </p>
        </div>
        <button
          type="button"
          onClick={() => setFormOpen((v) => !v)}
          className="inline-flex items-center gap-2 rounded-lg bg-primary-600 px-3.5 py-2 text-sm font-medium text-white hover:bg-primary-500"
        >
          {formOpen ? <X className="w-4 h-4" /> : <Plus className="w-4 h-4" />}
          {formOpen ? tText('Annuler') : tText('Nouvelle église')}
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
          <label className="block text-sm">
            <span className="text-xs font-medium text-gray-600 dark:text-gray-300">
              {tText('Nom de l’église')}
            </span>
            <input
              value={name}
              onChange={(e) => setName(e.target.value)}
              placeholder={tText('Église de la Grâce')}
              className="mt-1 w-full rounded-lg border border-gray-300 dark:border-white/15 bg-white dark:bg-gray-800 px-3 py-2 text-sm text-gray-900 dark:text-gray-100"
            />
          </label>
          <button
            type="button"
            onClick={() => createMutation.mutate()}
            disabled={createMutation.isPending}
            className="inline-flex items-center gap-2 rounded-lg bg-primary-600 px-4 py-2 text-sm font-medium text-white hover:bg-primary-500 disabled:opacity-50"
          >
            {createMutation.isPending ? <Loader2 className="w-4 h-4 animate-spin" /> : <Plus className="w-4 h-4" />}
            {tText('Créer')}
          </button>
          <p className="text-xs text-gray-500 dark:text-gray-400">
            {tText(
              'Pour un simple regroupement interne (campus, groupe), préférez un nœud : un code dans « Codes d’entrée », sans facturation séparée.',
            )}
          </p>
        </section>
      )}

      <section className="rounded-xl border border-gray-200 dark:border-white/10 bg-white dark:bg-gray-900 overflow-hidden">
        <header className="px-5 py-3 border-b font-semibold text-sm text-gray-800 dark:text-gray-100 flex items-center gap-2">
          <Building2 className="w-4 h-4 text-primary-500" /> {tText('Organisations rattachées')}{' '}
          ({all.length})
        </header>
        <div className="divide-y divide-gray-100 dark:divide-white/5">
          {all.map((org) => (
            <div key={org.id} className="px-5 py-4 flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
              <div className="min-w-0">
                <p className="font-medium text-sm text-gray-900 dark:text-white">
                  {org.name}
                  {org.id === current?.id && (
                    <span className="ml-2 text-[10px] font-semibold uppercase tracking-wider text-primary-600 dark:text-primary-400">
                      {tText('vous')}
                    </span>
                  )}
                </p>
                <p className="text-xs text-gray-500 dark:text-gray-400">
                  {KIND_LABEL[org.kind] ?? org.kind} · {org.status} · {org.plan}
                  {org.childCount > 0 ? ` · ${org.childCount} ${tText('rattachée(s)')}` : ''}
                </p>
                {org.invitePath && (
                  <p className="flex items-center gap-1.5 mt-1 text-xs font-mono text-primary-600 dark:text-primary-400">
                    <Link2 className="w-3 h-3 shrink-0" />
                    <span className="truncate">
                      {`${window.location.origin}${org.invitePath}`}
                    </span>
                  </p>
                )}
              </div>
              {org.invitePath && (
                <button
                  type="button"
                  onClick={() => void copy(`${window.location.origin}${org.invitePath}`, org.id)}
                  className="shrink-0 rounded-md border border-primary-200 px-2 py-1.5 text-xs text-primary-700 dark:text-primary-300 hover:bg-primary-500/10"
                >
                  {copied === org.id ? <CheckCircle2 className="w-3.5 h-3.5" /> : <Copy className="w-3.5 h-3.5" />}
                </button>
              )}
            </div>
          ))}
        </div>
      </section>
    </main>
  );
}
