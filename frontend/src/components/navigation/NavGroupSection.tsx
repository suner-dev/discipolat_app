import { useCallback, useMemo, useState, type ReactNode } from 'react';
import { ChevronDown, Layers } from 'lucide-react';
import { useLocation } from 'react-router-dom';
import type { GroupedNavNode, NavEntry } from '@/navigation/grouping';
import { containsHref, countNodes } from '@/navigation/grouping';
import { resolveIcon } from '@/lib/menuIcons';

/**
 * LOT 2 §GR — un groupe d'onglets repliable dans la barre latérale.
 *
 * <p>Comportement : au clic sur l'en-tête, la liste des sous-onglets se
 * déploie sous le groupe. C'est exactement la demande produit (« lorsque
 * l'on clique sur le groupe, la liste des sous onglets s'affiche »), et cela
 * remplace la longue liste plate de ~175 entrées.
 *
 * <p>Trois garde-fous :
 * <ul>
 *   <li><b>Auto-ouverture</b> : un groupe contenant la route active s'ouvre
 *       toujours, sinon l'utilisateur perdrait le repère « je suis ici » ;</li>
 *   <li><b>Accessibilité</b> : replié = {@code visibility:hidden}, donc les
 *       sous-onglets sortent du parcours de tabulation et de l'arbre
 *       d'accessibilité, tout en restant montés (pas de perte de state, et les
 *       tests restent lisibles) ;</li>
 *   <li><b>Mouvement réduit</b> : l'animation est coupée si l'utilisateur
 *       l'a demandé au niveau du système (voir {@code prefers-reduced-motion}).</li>
 * </ul>
 */
interface NavGroupSectionProps {
  node: GroupedNavNode;
  renderItem: (entry: NavEntry, onNavigate: () => void) => ReactNode;
  /** Persiste l'état ouvert/replié au-delà d'un rendu. */
  openState?: Record<string, boolean>;
  onToggle?: (id: string, open: boolean) => boolean;
  onNavigate?: () => void;
  /** Mode rail (sidebar repliée en icônes) : on n'affiche que les icônes. */
  rail?: boolean;
  depth?: number;
}

export function NavGroupSection({
  node,
  renderItem,
  openState,
  onToggle,
  onNavigate,
  rail = false,
  depth = 0,
}: NavGroupSectionProps) {
  const location = useLocation();
  const isActiveBranch = useMemo(() => containsHref(node, location.pathname), [node, location.pathname]);

  // Un groupe qui contient la page courante reste ouvert : c'est le seul moyen
  // de garder le repère quand la navigation est repliée.
  const forcedOpen = isActiveBranch;
  const persisted = openState?.[node.id];
  const [internalOpen, setInternalOpen] = useState(
    persisted ?? (node.collapsedByDefault ? false : true),
  );
  // Le réglage explicite de l'utilisateur l'emporte ; sinon on suit l'état
  // interne ; la branche active force l'ouverture dans tous les cas.
  const open = forcedOpen || (persisted !== undefined ? persisted : internalOpen);

  const toggle = () => {
    if (openState && onToggle) {
      onToggle(node.id, !open);
      return;
    }
    setInternalOpen(!open);
  };

  const Icon = useMemo(() => (node.icon ? resolveIcon(node.icon) : Layers), [node.icon]);
  const total = countNodes(node);
  const showHeader = !rail;

  if (rail) {
    // En mode rail, pas d'accordéon : on montre le groupe comme une pastille.
    return (
      <div className="mb-1 flex justify-center">
        <button
          type="button"
          onClick={toggle}
          title={node.label}
          aria-label={node.label}
          className={`relative flex items-center justify-center w-9 h-9 rounded-xl transition-all duration-200 ease-smooth
            ${isActiveBranch
              ? 'bg-primary-500/15 text-primary-600 dark:text-primary-400'
              : 'text-gray-400 hover:text-gray-600 dark:hover:text-gray-300 hover:bg-white/50 dark:hover:bg-gray-800/30'}`}
        >
          <Icon className="w-4.5 h-4.5 w-[18px] h-[18px]" />
        </button>
      </div>
    );
  }

  return (
    <div className="mb-1" data-nav-group={node.id}>
      <button
        type="button"
        onClick={toggle}
        aria-expanded={open}
        className={`group flex w-full items-center gap-2.5 rounded-xl px-3 py-2 text-left transition-all duration-200 ease-smooth
          ${isActiveBranch
            ? 'bg-primary-500/[0.07] text-primary-700 dark:text-primary-400'
            : 'text-gray-500 dark:text-gray-400 hover:bg-white/50 dark:hover:bg-gray-800/30 hover:text-gray-700 dark:hover:text-gray-200'}`}
      >
        <Icon className="w-4 h-4 flex-shrink-0 opacity-80" />
        <span className="flex-1 min-w-0 truncate text-[11px] font-semibold uppercase tracking-wider">
          {node.label}
        </span>
        {node.showCount && total > 0 && (
          <span className="flex-shrink-0 rounded-full bg-gray-200/80 dark:bg-gray-700/60 px-1.5 py-0.5 text-[9px] font-semibold text-gray-500 dark:text-gray-300">
            {total}
          </span>
        )}
        <ChevronDown
          className={`w-3.5 h-3.5 flex-shrink-0 transition-transform duration-300 ease-smooth
            ${open ? 'rotate-180' : 'rotate-0'}`}
        />
      </button>

      {/* Repli animé : la hauteur passe de 0 à « auto » via grid-rows (0fr → 1fr),
          ce qui évite tout JavaScript de mesure et reste fluide.
          `invisible` (et non `hidden`) sort les sous-onglets du parcours de
          tabulation tout en gardant le DOM monté. */}
      <div
        className={`grid transition-[grid-template-rows,opacity] duration-300 ease-smooth
          ${open ? 'grid-rows-[1fr] opacity-100' : 'grid-rows-[0fr] opacity-0'}`}
      >
        <div className="overflow-hidden">
          <div
            className={`transition-opacity duration-300 ease-smooth ${open ? '' : 'invisible'}`}
            style={{ paddingLeft: depth > 0 ? `${depth * 0.75}rem` : undefined }}
          >
            <div className="pt-1 space-y-0.5">
              {node.items.map((entry) => (
                <div key={`${node.id}::${entry.href}`}>{renderItem(entry, () => onNavigate?.())}</div>
              ))}
              {node.children.map((child) => (
                <NavGroupSection
                  key={child.id}
                  node={child}
                  renderItem={renderItem}
                  openState={openState}
                  onToggle={onToggle}
                  onNavigate={onNavigate}
                  depth={depth + 1}
                />
              ))}
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}

/**
 * Mémorise l'état ouvert/replié par groupe pour toute la session de navigation,
 * afin qu'un repli ne « perde » pas le réglage fait par l'utilisateur.
 */
export function useNavGroupState() {
  const [openState, setOpenState] = useState<Record<string, boolean>>({});

  const onToggle = useCallback((id: string, open: boolean) => {
    setOpenState((previous) => ({ ...previous, [id]: open }));
    return open;
  }, []);

  return { openState, onToggle };
}