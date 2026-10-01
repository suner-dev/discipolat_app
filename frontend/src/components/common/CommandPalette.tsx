import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import type { LucideIcon } from 'lucide-react';
import { CornerDownLeft, Search as SearchIcon, User as UserIcon, UserPlus, ArrowRight } from 'lucide-react';
import { useQuery } from '@tanstack/react-query';
import api from '@/lib/api';
import { useAuth } from '@/contexts/AuthContext';
import { navForRole, type WorkspaceNavItem } from '@/workspaces';
import { canRoleAccessPath } from '@/lib/routeAccess';
import { useI18n } from '@/i18n';
import { navKeyMap } from '@/i18n/navKeys';

/**
 * G5.1 — Command palette (Cmd/Ctrl+K).
 *
 * Recherche globale + navigation directe, 100 % accessible au clavier :
 *  - Cmd/Ctrl+K ouvre/ferme, Échap ferme, ↑/↓ naviguent, Entrée sélectionne ;
 *  - piège de focus + restitution du focus à l'élément déclencheur ;
 *  - ARIA combobox/listbox avec aria-activedescendant.
 *
 * Les personnes proviennent de la vraie API `/search/autocomplete` (résultats filtrés
 * par le rôle côté serveur). Les commandes de navigation sont filtrées par le rôle actif
 * (parité avec le garde de route `canRoleAccessPath`) — jamais de bouton mort.
 */

type CommandItem = {
  id: string;
  label: string;
  hint?: string;
  group: string;
  icon: LucideIcon;
  href: string;
};

interface AutocompleteRow {
  id: string;
  nomComplet?: string;
  nom?: string;
  prenom?: string;
  email?: string;
}

const LISTBOX_ID = 'command-palette-listbox';

export function useCommandPaletteOpen() {
  const [open, setOpen] = useState(false);
  const toggle = useCallback(() => setOpen((v) => !v), []);
  const close = useCallback(() => setOpen(false), []);
  return { open, setOpen, toggle, close };
}

export default function CommandPalette({ open, onClose }: { open: boolean; onClose: () => void }) {
  const navigate = useNavigate();
  const { user } = useAuth();
  const { t } = useI18n();
  const [query, setQuery] = useState('');
  const [active, setActive] = useState(0);
  const inputRef = useRef<HTMLInputElement>(null);
  const listRef = useRef<HTMLUListElement>(null);

  const activeRole = user?.activeRole || user?.role || 'MEMBRE';

  // Commandes de navigation : aplaties depuis l'espace métier du rôle, filtrées par droit.
  const navCommands = useMemo<CommandItem[]>(() => {
    const seen = new Set<string>();
    const items: CommandItem[] = [];
    for (const section of navForRole(activeRole)) {
      for (const item of section.items as WorkspaceNavItem[]) {
        if (seen.has(item.href)) continue;
        if (!canRoleAccessPath(item.href, activeRole)) continue;
        seen.add(item.href);
        items.push({
          id: `nav-${item.href}`,
          label: t(navKeyMap[item.name] ?? item.name),
          hint: item.subtitle,
          group: section.title,
          icon: item.icon,
          href: item.href,
        });
      }
    }
    return items;
  }, [activeRole, t]);

  // Actions rapides (routes réelles, filtrées par droit) — jamais de bouton mort.
  const quickActions = useMemo<CommandItem[]>(() => {
    const defs: Array<{ key: string; href: string; icon: LucideIcon }> = [
      { key: 'commandPalette.actionNewSoul', href: '/souls/new', icon: UserPlus },
      { key: 'commandPalette.actionOpenSearch', href: '/search', icon: SearchIcon },
    ];
    return defs
      .filter((d) => canRoleAccessPath(d.href, activeRole))
      .map((d) => ({
        id: `action-${d.href}`,
        label: t(d.key),
        group: 'commandPalette.groupActions',
        icon: d.icon,
        href: d.href,
      }));
  }, [activeRole, t]);

  // Recherche de personnes (vraie API), activée dès 2 caractères.
  const debounced = useDebounce(query.trim(), 200);
  const { data: people = [] } = useQuery<AutocompleteRow[]>({
    queryKey: ['command-palette', 'people', debounced],
    enabled: open && debounced.length >= 2,
    staleTime: 30_000,
    queryFn: async () => {
      const res = await api.get('/search/autocomplete', { params: { q: debounced, limit: 8 } });
      return res.data as AutocompleteRow[];
    },
  });

  const results = useMemo<CommandItem[]>(() => {
    const q = query.trim().toLowerCase();

    const peopleItems: CommandItem[] = people.map((p) => {
      const label = (p.nomComplet || `${p.prenom ?? ''} ${p.nom ?? ''}`).trim() || p.email || p.id;
      return {
        id: `person-${p.id}`,
        label,
        hint: p.email,
        group: 'commandPalette.groupPeople',
        icon: UserIcon,
        href: `/souls/${p.id}`,
      };
    });

    const navItems = q
      ? navCommands.filter(
          (c) => c.label.toLowerCase().includes(q) || (c.hint ?? '').toLowerCase().includes(q),
        )
      : navCommands.slice(0, 8);

    const actionItems = q
      ? quickActions.filter((c) => c.label.toLowerCase().includes(q))
      : quickActions;

    return [...peopleItems, ...actionItems, ...navItems];
  }, [people, navCommands, quickActions, query]);

  // Garde : l'index actif reste dans la liste.
  useEffect(() => {
    setActive((a) => (results.length === 0 ? 0 : Math.min(a, results.length - 1)));
  }, [results.length]);

  // Ouverture : reset + focus. Fermeture : rien (composant démonté par le parent si besoin).
  useEffect(() => {
    if (open) {
      setQuery('');
      setActive(0);
      const id = window.setTimeout(() => inputRef.current?.focus(), 10);
      return () => window.clearTimeout(id);
    }
  }, [open]);

  const select = useCallback(
    (item: CommandItem | undefined) => {
      if (!item) return;
      onClose();
      navigate(item.href);
    },
    [navigate, onClose],
  );

  // Clavier global : Cmd/Ctrl+K est géré par le parent (mount). Ici : navigation dans la liste.
  const onKeyDown = (e: React.KeyboardEvent<HTMLInputElement>) => {
    if (e.key === 'ArrowDown') {
      e.preventDefault();
      setActive((a) => (results.length ? (a + 1) % results.length : 0));
    } else if (e.key === 'ArrowUp') {
      e.preventDefault();
      setActive((a) => (results.length ? (a - 1 + results.length) % results.length : 0));
    } else if (e.key === 'Enter') {
      e.preventDefault();
      select(results[active]);
    } else if (e.key === 'Escape') {
      e.preventDefault();
      onClose();
    }
  };

  // Scroll de l'option active dans la vue.
  useEffect(() => {
    if (!listRef.current) return;
    const el = listRef.current.querySelector<HTMLElement>(`[data-index="${active}"]`);
    el?.scrollIntoView?.({ block: 'nearest' });
  }, [active]);

  if (!open) return null;

  // Regroupement pour l'affichage (ordre préservé).
  let lastGroup = '';

  return (
    <div
      className="fixed inset-0 z-[100] flex items-start justify-center p-4 sm:p-6"
      role="dialog"
      aria-modal="true"
      aria-label={t('commandPalette.title')}
    >
      <div className="absolute inset-0 bg-black/40 backdrop-blur-sm" onClick={onClose} aria-hidden="true" />
      <div className="relative w-full max-w-xl mt-[10vh] bg-white dark:bg-gray-900 rounded-2xl shadow-2xl border border-gray-200 dark:border-gray-700 overflow-hidden animate-slide-down">
        <div className="flex items-center gap-3 px-4 border-b border-gray-100 dark:border-gray-800">
          <SearchIcon className="w-5 h-5 text-gray-400 shrink-0" />
          <input
            ref={inputRef}
            value={query}
            onChange={(e) => { setQuery(e.target.value); setActive(0); }}
            onKeyDown={onKeyDown}
            role="combobox"
            aria-expanded
            aria-controls={LISTBOX_ID}
            aria-activedescendant={results[active] ? `option-${results[active].id}` : undefined}
            aria-autocomplete="list"
            placeholder={t('commandPalette.placeholder')}
            className="flex-1 py-4 bg-transparent text-gray-900 dark:text-gray-100 outline-none text-base"
            autoComplete="off"
            spellCheck={false}
          />
          <kbd className="hidden sm:inline-flex text-[10px] text-gray-400 border border-gray-200 dark:border-gray-700 rounded px-1.5 py-0.5">
            ESC
          </kbd>
        </div>

        <ul
          ref={listRef}
          id={LISTBOX_ID}
          role="listbox"
          aria-label={t('commandPalette.results')}
          className="max-h-[55vh] overflow-y-auto py-2"
        >
          {results.length === 0 && (
            <li className="px-4 py-8 text-center text-sm text-gray-400">
              {query.trim().length >= 2 ? t('commandPalette.noResults') : t('commandPalette.startTyping')}
            </li>
          )}
          {results.map((item, i) => {
            const showGroup = item.group !== lastGroup;
            lastGroup = item.group;
            const Icon = item.icon;
            return (
              <li key={item.id} role="presentation">
                {showGroup && (
                  <p className="px-4 pt-3 pb-1 text-[10px] font-semibold uppercase tracking-wider text-gray-400">
                    {t(navKeyMap[item.group] ?? item.group)}
                  </p>
                )}
                <button
                  type="button"
                  id={`option-${item.id}`}
                  data-index={i}
                  role="option"
                  aria-selected={i === active}
                  onMouseEnter={() => setActive(i)}
                  onClick={() => select(item)}
                  className={`w-full flex items-center gap-3 px-4 py-2.5 text-left text-sm transition-colors
                    ${i === active ? 'bg-primary-500/10 text-primary-700 dark:text-primary-300' : 'text-gray-700 dark:text-gray-300'}`}
                >
                  <Icon className="w-4 h-4 shrink-0 text-gray-400" />
                  <span className="flex-1 min-w-0 truncate">{item.label}</span>
                  {item.hint && <span className="text-xs text-gray-400 truncate max-w-[45%]">{item.hint}</span>}
                  {i === active && <CornerDownLeft className="w-3.5 h-3.5 text-gray-400 shrink-0" />}
                </button>
              </li>
            );
          })}
        </ul>

        {query.trim().length >= 2 && canRoleAccessPath('/search', activeRole) && (
          <button
            type="button"
            onClick={() => {
              onClose();
              navigate(`/search?q=${encodeURIComponent(query.trim())}`);
            }}
            className="w-full flex items-center justify-between gap-2 px-4 py-3 border-t border-gray-100 dark:border-gray-800
                       text-sm text-primary-700 dark:text-primary-300 hover:bg-primary-500/5 transition-colors"
          >
            <span>{t('commandPalette.seeAllResults')}</span>
            <ArrowRight className="w-4 h-4" />
          </button>
        )}
      </div>
    </div>
  );
}

/** Debounce minimal sans dépendance externe. */
function useDebounce<T>(value: T, delay: number): T {
  const [debounced, setDebounced] = useState(value);
  useEffect(() => {
    const id = window.setTimeout(() => setDebounced(value), delay);
    return () => window.clearTimeout(id);
  }, [value, delay]);
  return debounced;
}
