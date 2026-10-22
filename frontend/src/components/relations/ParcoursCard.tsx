import { useQuery } from '@tanstack/react-query';
import { GitBranch, History, UserPlus } from 'lucide-react';
import api from '@/lib/api';
import { useI18n, getI18nLocale } from '@/i18n';
import { useAuthOptional } from '@/contexts/AuthContext';

/**
 * « Mon parcours pastoral » — le RÉCIT d'un membre dans le temps.
 *
 * <p>Reflet exact de `MyHierarchyCard` : le sujet (moi ou le membre consulté
 * depuis une fiche) lit son PROPRE endpoint — `GET /hierarchy/me/parcours`
 * quand on est devant sa propre vue, `GET /hierarchy/users/{id}/parcours`
 * sinon (même garde que la fiche hiérarchie). Vue en LECTURE SEULE composée
 * côté serveur à partir des seules données déjà persistées : adhésions,
 * transferts, capacités et rattachements, triés chronologiquement.</p>
 *
 * <p>Dégradation silencieuse : si le compte n'a aucun récit (ou si la brique
 * est absente sur un backend antérieur), rien s'affiche — jamais une erreur
 * trompeuse, jamais un bloc vide. Additif strict : n'altère aucune vue
 * existante.</p>
 */

interface ParcoursEvent {
  horodatage?: string | null;
  type?: string;
  action?: string;
  libelle?: string;
}

/** Icône et teinte par nature d'événement — le serveur renvoie le CODE technique. */
const TYPE_ICONS: Record<string, { icon: typeof UserPlus; cls: string }> = {
  ADHESION: { icon: UserPlus, cls: 'text-emerald-500' },
  TRANSFERT: { icon: GitBranch, cls: 'text-amber-500' },
  CAPACITE: { icon: History, cls: 'text-indigo-500' },
  RATTACHEMENT: { icon: History, cls: 'text-violet-500' },
};

function formatParcoursDate(iso: string | null | undefined): string | null {
  if (!iso) return null;
  const d = new Date(iso);
  if (Number.isNaN(d.getTime())) return null;
  return d.toLocaleDateString(getI18nLocale(), { day: 'numeric', month: 'short', year: 'numeric' });
}

export function ParcoursCard({ userId }: { userId?: string }) {
  const { t } = useI18n();
  const auth = useAuthOptional();
  const isSelf = !userId || userId === auth?.user?.id;

  const { data } = useQuery({
    // Clés distinctes : la vue de soi et la vue d'un tiers ne se marchent pas
    // l'une sur l'autre dans le cache (cohérent avec ['hierarchy','me'] vs
    // ['users', id, 'hierarchy']).
    queryKey: isSelf ? ['hierarchy', 'parcours', 'me'] : ['users', userId, 'parcours'],
    queryFn: async () =>
      (await api.get(isSelf ? '/hierarchy/me/parcours' : `/hierarchy/users/${userId}/parcours`))
        .data as any,
    enabled: !isSelf || !!auth?.user?.id,
    retry: 1,
  });

  const events: ParcoursEvent[] = Array.isArray(data?.evenements) ? data.evenements : [];
  if (events.length === 0) return null;

  const resume = data?.resume ?? {};
  const since = formatParcoursDate(data?.depuis);
  const transferts = Number(resume?.transferts ?? 0);
  const ruptures = Number(resume?.rattachementsRompus ?? 0);

  return (
    <div className="glass-card p-6 mt-6 animate-slide-up">
      <h3 className="text-sm font-bold text-gray-900 dark:text-gray-100 mb-1 flex items-center gap-2">
        <History className="w-4 h-4 text-rose-500" /> {t('hierarchy.parcoursTitle')}
      </h3>
      {since && (
        <p className="text-xs text-gray-500 dark:text-gray-400 mb-4">
          {t('hierarchy.parcoursSince', { date: since })}
        </p>
      )}

      <ol className="space-y-2">
        {events.map((e, i) => {
          const meta = TYPE_ICONS[e.type ?? ''] ?? { icon: History, cls: 'text-gray-400' };
          const Icon = meta.icon;
          const date = formatParcoursDate(e.horodatage);
          return (
            <li key={`${e.horodatage ?? 'sans-date'}-${e.type}-${i}`} className="flex items-start gap-2">
              <Icon className={`w-3.5 h-3.5 mt-0.5 shrink-0 ${meta.cls}`} />
              <div className="min-w-0">
                <p className="text-[11px] text-gray-700 dark:text-gray-300" title={e.libelle}>
                  <span className="font-medium">{t(`hierarchy.parcoursType.${e.type}`)}</span>
                  {' · '}
                  {t(e.action === 'FIN' ? 'hierarchy.parcoursActionFin' : 'hierarchy.parcoursActionDebut')}
                  {date ? ` · ${date}` : ''}
                </p>
              </div>
            </li>
          );
        })}
      </ol>

      {(transferts > 0 || ruptures > 0) && (
        <p className="text-[10px] text-gray-400 mt-3">
          {transferts > 0 && t('hierarchy.parcoursTransferts', { n: String(transferts) })}
          {transferts > 0 && ruptures > 0 ? ' · ' : ''}
          {ruptures > 0 && t('hierarchy.parcoursRuptures', { n: String(ruptures) })}
        </p>
      )}
    </div>
  );
}
