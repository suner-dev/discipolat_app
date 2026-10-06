import { useQuery } from '@tanstack/react-query';
import { GitBranch, Shield, Loader2, UserX } from 'lucide-react';
import api from '@/lib/api';
import { useI18n } from '@/i18n';
import { HierarchyTree, useBranchTree, type HierarchyBranch, type TreeNodeModel } from './HierarchyTree';

/**
 * « Ma hiérarchie » — version profil.
 *
 * <p>Lit son PROPRE endpoint `GET /hierarchy/me` (les branches, la chaîne des
 * responsables de chaque niveau, les ascendants unifiés et l'encadrement
 * pastoral). Distinct de la carte « Mon encadrement » : celle-ci rapatrie mes
 * encadrants <em>déclaratifs</em>, celle-là mon <em>rattachement
 * organisationnel</em>. Les deux ensemble = la vue complète demandée par le
 * plan V231 (« un membre sait en détail son rôle et sa hiérarchie »).
 */
export function MyHierarchyCard({ onOpenUser }: { onOpenUser?: (id: string) => void }) {
  const { t } = useI18n();
  const { data, isLoading, isError, refetch } = useQuery({
    queryKey: ['hierarchy', 'me'],
    queryFn: async () => (await api.get('/hierarchy/me')).data as any,
    retry: 1,
  });

  const branches = data?.branches as HierarchyBranch[] | undefined;
  const ascendants: any[] = Array.isArray(data?.ascendants) ? data.ascendants : [];
  const tree = useBranchTree(branches, (id) => onOpenUser?.(id), t);

  if (isLoading) {
    return (
      <div className="glass-card p-6 mt-6 animate-slide-up">
        <h3 className="text-sm font-bold text-gray-900 dark:text-gray-100 mb-3 flex items-center gap-2">
          <GitBranch className="w-4 h-4 text-amber-500" /> {t('hierarchy.title')}
        </h3>
        <p className="text-xs text-gray-400 flex items-center gap-1.5">
          <Loader2 className="w-3 h-3 animate-spin" /> {t('relations.loading')}
        </p>
      </div>
    );
  }

  if (isError) {
    return (
      <div className="glass-card p-6 mt-6 animate-slide-up">
        <h3 className="text-sm font-bold text-gray-900 dark:text-gray-100 mb-3 flex items-center gap-2">
          <GitBranch className="w-4 h-4 text-amber-500" /> {t('hierarchy.title')}
        </h3>
        <div className="flex items-center justify-between gap-3">
          <p className="text-xs text-red-500">{t('relations.loadError')}</p>
          <button onClick={() => refetch()} className="btn-secondary btn-sm">
            {t('relations.retry')}
          </button>
        </div>
      </div>
    );
  }

  return (
    <div className="glass-card p-6 mt-6 animate-slide-up">
      <h3 className="text-sm font-bold text-gray-900 dark:text-gray-100 mb-1 flex items-center gap-2">
        <GitBranch className="w-4 h-4 text-amber-500" /> {t('hierarchy.title')}
      </h3>
      <p className="text-xs text-gray-500 dark:text-gray-400 mb-4">{t('hierarchy.branchesSubtitle') ?? ''}</p>

      {ascendants.length > 0 && (
        <div className="mb-4">
          <p className="text-[10px] font-semibold text-gray-400 uppercase tracking-wider mb-1.5">
            {t('hierarchy.ascendants')}
          </p>
          <div className="flex flex-wrap gap-1.5">
            {ascendants.map((a: any, i: number) => (
              <button
                key={`${a.via}-${a.id}-${i}`}
                type="button"
                onClick={() => onOpenUser?.(a.id)}
                title={t('hierarchy.viewProfile')}
                className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-[11px] font-medium bg-white/70 dark:bg-gray-900/40 border border-gray-200/60 dark:border-gray-700/50 text-gray-700 dark:text-gray-200 hover:border-violet-300 cursor-pointer"
              >
                <Shield className={`w-3 h-3 ${a.via === 'DECLARATIF' ? 'text-violet-500' : 'text-blue-500'}`} />
                {a.nom}
                <span className="text-[9px] text-gray-400">
                  {a.via === 'DECLARATIF' ? a.typeLabel ?? t('hierarchy.viaDeclared') : a.noeud ?? t('hierarchy.viaOrg')}
                </span>
              </button>
            ))}
          </div>
        </div>
      )}

      <p className="text-[10px] font-semibold text-gray-400 uppercase tracking-wider mb-1.5 flex items-center gap-1.5">
        <GitBranch className="w-3 h-3 text-amber-500" /> {t('hierarchy.branches')}
      </p>
      <HierarchyTree
        nodes={tree}
        emptyLabel={t('hierarchy.branchesEmpty')}
        emptyIcon={<UserX className="w-3 h-3" />}
      />
    </div>
  );
}
