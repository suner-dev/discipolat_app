import { useEffect, useMemo, useRef, useState, type ReactNode } from 'react';
import { ChevronDown } from 'lucide-react';
import { DURATION, EASING, staggerDelay, usePrefersReducedMotion } from '@/lib/motion';

/**
 * LOT 2 §GR — <b>groupes d'onglets</b> pour les écrans à beaucoup d'onglets.
 *
 * <p>Objectif produit : « on ait des groupes d'onglets et lorsqu'on clique
 * dessus, la liste des sous-onglets s'affiche ». Un écran à 14 onglets devient
 * 4 groupes ; le clic sur un groupe déploie ses onglets.
 *
 * <p><b>Pourquoi un composant et pas 34 barres d'onglets copiées.</b> Le dépôt
 * compte 34 implémentations indépendantes, dans une dizaine de styles visuels
 * différents, dont 2 seulement conformes ARIA. Aucune ne dédupliquait les
 * libellés, et aucune ne se souvenait du groupe ouvert d'un écran à l'autre.
 * Ce composant donne un point d'entrée unique — sans obliger à migrer les
 * écrans existants, qui continuent de fonctionner inchangés.
 *
 * <p><b>Accessibilité</b> : `role="tablist"` / `role="tab"` / `aria-selected` /
 * `aria-expanded`, navigation au clavier (← → sur les onglets du groupe,
 * ↑ ↓ pour changer de groupe), et respect du mouvement réduit.
 */
export interface GroupedTabItem {
  id: string;
  label: string;
  /** Nombre d'éléments, affiché en badge. */
  count?: number;
  disabled?: boolean;
}

export interface GroupedTabGroup {
  id: string;
  label: string;
  icon?: ReactNode;
  tabs: GroupedTabItem[];
}

interface GroupedTabsProps {
  groups: GroupedTabGroup[];
  /** Onglet actif (identifiant de longlet, pas du groupe). */
  value: string;
  onChange: (tabId: string) => void;
  className?: string;
  /** Groupe ouvert au premier rendu. Par défaut : celui de l'onglet actif. */
  defaultOpenGroupId?: string;
  /** Rend le contenu de chaque onglet ; sinon seul le bandeau est rendu. */
  renderPanel?: (tabId: string) => ReactNode;
}

export function GroupedTabs({
  groups,
  value,
  onChange,
  className,
  defaultOpenGroupId,
  renderPanel,
}: GroupedTabsProps) {
  const reducedMotion = usePrefersReducedMotion();
  const activeGroupId = useMemo(
    () => groups.find((group) => group.tabs.some((tab) => tab.id === value))?.id ?? groups[0]?.id,
    [groups, value],
  );
  const [openGroupId, setOpenGroupId] = useState<string | null>(
    defaultOpenGroupId ?? activeGroupId ?? null,
  );
  const tabRefs = useRef<Record<string, HTMLButtonElement | null>>({});

  // Le groupe de l'onglet actif s'ouvre toujours : sinon l'utilisateur change
  // d'onglet par programme (lien, notification) sans voir lequel.
  useEffect(() => {
    if (activeGroupId) setOpenGroupId(activeGroupId);
  }, [activeGroupId]);

  const selectGroup = (groupId: string) => {
    setOpenGroupId((current) => (current === groupId ? null : groupId));
  };

  const moveTabFocus = (group: GroupedTabGroup, direction: 1 | -1) => {
    const enabled = group.tabs.filter((tab) => !tab.disabled);
    if (enabled.length === 0) return;
    const currentIndex = enabled.findIndex((tab) => tab.id === value);
    const nextIndex = (currentIndex + direction + enabled.length) % enabled.length;
    const next = enabled[nextIndex === -1 ? 0 : nextIndex];
    if (!next) return;
    onChange(next.id);
    tabRefs.current[next.id]?.focus();
  };

  return (
    <div className={className}>
      <div
        role="tablist"
        aria-orientation="horizontal"
        aria-label="Groupes d’onglets"
        className="flex items-center gap-1.5 -mx-1 px-1 pb-2 overflow-x-auto"
      >
        {groups.map((group, groupIndex) => {
          const open = openGroupId === group.id;
          const isActiveGroup = activeGroupId === group.id;
          const hasActiveTab = group.tabs.some((tab) => tab.id === value);
          return (
            <div key={group.id} className="flex-shrink-0">
              <button
                type="button"
                aria-expanded={open}
                aria-controls={`tab-panel-${group.id}`}
                onClick={() => selectGroup(group.id)}
                style={
                  reducedMotion
                    ? undefined
                    : {
                        transitionDelay: `${staggerDelay(groupIndex, 30)}ms`,
                        animation: `fade-in ${DURATION.base}ms ${EASING.enter} both`,
                      }
                }
                className={`inline-flex items-center gap-1.5 px-3 py-2 rounded-xl text-sm font-medium
                  transition-all duration-200 ease-smooth active:scale-[0.98]
                  ${
                    isActiveGroup
                      ? 'bg-primary-500/12 text-primary-700 dark:text-primary-400 shadow-sm'
                      : 'text-gray-500 dark:text-gray-400 hover:text-gray-800 dark:hover:text-gray-200 hover:bg-white/60 dark:hover:bg-gray-800/40'
                  }`}
              >
                {group.icon}
                <span className="whitespace-nowrap">{group.label}</span>
                {hasActiveTab && (
                  <span
                    aria-hidden="true"
                    className="w-1.5 h-1.5 rounded-full bg-primary-500"
                    style={
                      reducedMotion
                        ? undefined
                        : { animation: `pulse-glow ${DURATION.slow}ms ${EASING.smooth} infinite` }
                    }
                  />
                )}
                <ChevronDown
                  className={`w-3.5 h-3.5 transition-transform duration-300 ease-smooth ${open ? 'rotate-180' : ''}`}
                />
              </button>

              {/* Déploiement des sous-onglets : hauteur 0 → auto via grid-rows,
                  donc fluide et sans mesure JavaScript. */}
              <div
                id={`tab-panel-${group.id}`}
                role="tabpanel"
                aria-label={group.label}
                className={`grid transition-[grid-template-rows,opacity] duration-300 ease-smooth
                  ${open ? 'grid-rows-[1fr] opacity-100' : 'grid-rows-[0fr] opacity-0'}`}
              >
                <div className="overflow-hidden">
                  <div className={`pt-1 pb-2 ${open ? '' : 'invisible'}`}>
                    <div
                      role="tablist"
                      aria-label={group.label}
                      className="flex items-center gap-1 flex-wrap ps-3 pe-2"
                    >
                      {group.tabs.map((tab) => {
                        const selected = tab.id === value;
                        return (
                          <button
                            key={tab.id}
                            ref={(element) => {
                              tabRefs.current[tab.id] = element;
                            }}
                            type="button"
                            role="tab"
                            aria-selected={selected}
                            disabled={tab.disabled}
                            onClick={() => onChange(tab.id)}
                            onKeyDown={(event) => {
                              if (event.key === 'ArrowRight' || event.key === 'ArrowDown') {
                                event.preventDefault();
                                moveTabFocus(group, 1);
                              } else if (event.key === 'ArrowLeft' || event.key === 'ArrowUp') {
                                event.preventDefault();
                                moveTabFocus(group, -1);
                              }
                            }}
                            className={`inline-flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-xs font-medium
                              transition-all duration-200 ease-smooth
                              ${selected
                                ? 'bg-primary-500 text-white shadow-sm'
                                : 'bg-white/60 dark:bg-gray-800/50 text-gray-600 dark:text-gray-300 hover:bg-white dark:hover:bg-gray-700'}
                              disabled:opacity-40 disabled:cursor-not-allowed`}
                          >
                            <span className="whitespace-nowrap">{tab.label}</span>
                            {tab.count !== undefined && (
                              <span
                                className={`rounded-full px-1.5 py-0.5 text-[10px] font-semibold ${
                                  selected
                                    ? 'bg-white/25 text-white'
                                    : 'bg-gray-200/80 dark:bg-gray-700 text-gray-600 dark:text-gray-300'
                                }`}
                              >
                                {tab.count}
                              </span>
                            )}
                          </button>
                        );
                      })}
                    </div>
                  </div>
                </div>
              </div>
            </div>
          );
        })}
      </div>

      {renderPanel && <div className="animate-fade-in">{renderPanel(value)}</div>}
    </div>
  );
}

export default GroupedTabs;