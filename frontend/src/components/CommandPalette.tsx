import { useEffect, useLayoutEffect, useMemo, useRef, useState } from 'react';
import { createPortal } from 'react-dom';
import { useNavigate } from 'react-router-dom';
import {
  LayoutDashboard, Users, CalendarDays, HeartHandshake, UserPlus,
  Search, ArrowRight, Settings, ShieldCheck, BarChart3, Bell,
  FileText, Church, Wallet, Boxes, UsersRound, Megaphone, ScrollText,
  User,
} from 'lucide-react';
import { useTheme } from '@/hooks/useTheme';
import { useAuth } from '@/contexts/AuthContext';
import api from '@/lib/api';

/** Résultat d'autocomplete backend (/search/autocomplete). */
interface AutocompleteSoul {
  type: 'AME';
  id: string | number;
  nomComplet: string;
  email?: string | null;
}

export interface CommandItem {
  id: string;
  label: string;
  href: string;
  section: 'Navigation' | 'Actions' | 'Gestion' | 'Admin' | 'Personnes';
  icon: React.ComponentType<{ className?: string }>;
  roles?: string[]; // si absent => visible pour tous les rôles authentifiés
}

export const COMMAND_ITEMS: CommandItem[] = [
  // Navigation
  { id: 'nav-dashboard', label: 'Tableau de bord', href: '/dashboard', section: 'Navigation', icon: LayoutDashboard },
  { id: 'nav-souls', label: 'Âmes', href: '/souls', section: 'Navigation', icon: Users, roles: ['ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR'] },
  { id: 'nav-events', label: 'Événements', href: '/events', section: 'Navigation', icon: CalendarDays },
  { id: 'nav-families', label: 'Familles', href: '/families', section: 'Navigation', icon: HeartHandshake, roles: ['ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE'] },
  { id: 'nav-departments', label: 'Départements', href: '/departments', section: 'Navigation', icon: UsersRound },
  { id: 'nav-prayers', label: 'Prière', href: '/prayers', section: 'Navigation', icon: Church },
  { id: 'nav-calendar', label: 'Calendrier', href: '/calendar', section: 'Navigation', icon: CalendarDays },
  { id: 'nav-search', label: 'Recherche intelligente', href: '/search', section: 'Navigation', icon: Search, roles: ['ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR'] },
  { id: 'nav-directory', label: 'Annuaire', href: '/directory', section: 'Navigation', icon: Users },

  // Actions
  { id: 'act-new-soul', label: 'Ajouter une âme', href: '/souls/new', section: 'Actions', icon: UserPlus, roles: ['ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR'] },
  { id: 'act-new-family', label: 'Nouvelle famille', href: '/families/new', section: 'Actions', icon: HeartHandshake, roles: ['ADMIN', 'PASTEUR', 'RESPONSABLE'] },
  { id: 'act-new-transfer', label: 'Nouveau transfert', href: '/transfers/new', section: 'Actions', icon: ArrowRight, roles: ['ADMIN', 'PASTEUR', 'RESPONSABLE'] },
  { id: 'act-qr-checkin', label: 'Check-in QR', href: '/qr-checkin', section: 'Actions', icon: Search },

  // Gestion
  { id: 'mgmt-finances', label: 'Finances', href: '/finances', section: 'Gestion', icon: Wallet, roles: ['ADMIN', 'PASTEUR'] },
  { id: 'mgmt-inventory', label: 'Inventaire', href: '/inventory', section: 'Gestion', icon: Boxes, roles: ['ADMIN', 'RESPONSABLE'] },
  { id: 'mgmt-reports', label: 'Rapports', href: '/reports', section: 'Gestion', icon: FileText },
  { id: 'mgmt-alerts', label: 'Alertes', href: '/alerts', section: 'Gestion', icon: Bell },
  { id: 'mgmt-announcements', label: 'Annonces programmées', href: '/scheduled-announcements', section: 'Gestion', icon: Megaphone, roles: ['ADMIN', 'PASTEUR', 'RESPONSABLE'] },
  { id: 'mgmt-analytics', label: 'Analytics d\u2019engagement', href: '/engagement-analytics', section: 'Gestion', icon: BarChart3, roles: ['ADMIN', 'PASTEUR'] },
  { id: 'mgmt-audit', label: 'Journal d\u2019audit', href: '/audit', section: 'Gestion', icon: ScrollText, roles: ['ADMIN'] },

  // Admin
  { id: 'admin-settings', label: 'Paramètres', href: '/admin/settings', section: 'Admin', icon: Settings, roles: ['ADMIN'] },
  { id: 'admin-roles', label: 'Rôles & permissions', href: '/admin/roles', section: 'Admin', icon: ShieldCheck, roles: ['ADMIN'] },
  { id: 'admin-users', label: 'Utilisateurs', href: '/admin/users', section: 'Admin', icon: Users, roles: ['ADMIN'] },
  { id: 'admin-modules', label: 'Modules', href: '/admin/modules', section: 'Admin', icon: Boxes, roles: ['ADMIN'] },
  { id: 'admin-branding', label: 'Identité visuelle', href: '/admin/branding', section: 'Admin', icon: Church, roles: ['ADMIN'] },
];

interface CommandPaletteProps {
  open: boolean;
  onClose: () => void;
}

const SECTION_ORDER: CommandItem['section'][] = ['Personnes', 'Navigation', 'Actions', 'Gestion', 'Admin'];

export default function CommandPalette({ open, onClose }: CommandPaletteProps) {
  const navigate = useNavigate();
  const { darkMode } = useTheme();
  const { activeRole, user } = useAuth();
  const dark = darkMode;
  const role = (activeRole || user?.role || 'FAISEUR') as string;

  const [query, setQuery] = useState('');
  const [selected, setSelected] = useState(0);
  const [souls, setSouls] = useState<AutocompleteSoul[]>([]);
  const [soulsLoading, setSoulsLoading] = useState(false);
  const inputRef = useRef<HTMLInputElement>(null);
  const dialogRef = useRef<HTMLDivElement>(null);
  const listRef = useRef<HTMLDivElement>(null);

  // Reset à chaque ouverture
  useEffect(() => {
    if (open) {
      setQuery('');
      setSelected(0);
      setSouls([]);
      const t = window.setTimeout(() => inputRef.current?.focus(), 10);
      return () => window.clearTimeout(t);
    }
    return undefined;
  }, [open]);

  // Recherche globale temps réel : autocomplete backend (debounce 250ms).
  // L'API /search/autocomplete est déjà scopée par rôle côté serveur.
  useEffect(() => {
    if (!open) return undefined;
    const q = query.trim();
    if (q.length < 2) {
      setSouls([]);
      setSoulsLoading(false);
      return undefined;
    }
    setSoulsLoading(true);
    const controller = new AbortController();
    const t = window.setTimeout(async () => {
      try {
        const res = await api.get('/search/autocomplete', {
          params: { q, limit: 6 },
          signal: controller.signal,
        });
        setSouls((res.data || []) as AutocompleteSoul[]);
      } catch (e) {
        if (!(e && (e as { name?: string }).name === 'CanceledError')) setSouls([]);
      } finally {
        setSoulsLoading(false);
      }
    }, 250);
    return () => {
      controller.abort();
      window.clearTimeout(t);
    };
  }, [query, open]);

  const results = useMemo(() => {
    // 1. Filtrage par rôle : un item sans `roles` est visible de tous.
    const roleFiltered = COMMAND_ITEMS.filter(
      (i) => !i.roles || i.roles.includes(role),
    );
    // 2. Filtrage par requête.
    const q = query.trim().toLowerCase();
    if (!q) return roleFiltered;
    return roleFiltered.filter(
      (i) => i.label.toLowerCase().includes(q) || i.section.toLowerCase().includes(q),
    );
  }, [query, role]);

  const grouped = useMemo(() => {
    // Section dynamique "Personnes" : résultats de l'autocomplete backend.
    const soulItems: CommandItem[] = souls.map((s) => ({
      id: `soul-${s.id}`,
      label: s.nomComplet,
      href: `/souls/${s.id}`,
      section: 'Personnes' as const,
      icon: User,
    }));
    const sections: { section: CommandItem['section']; items: CommandItem[] }[] = [
      { section: 'Personnes', items: soulItems },
      ...SECTION_ORDER.slice(1).map((section) => ({
        section,
        items: results.filter((i) => i.section === section),
      })),
    ];
    return sections.filter((g) => g.items.length > 0);
  }, [results, souls]);

  const flatItems = useMemo(() => grouped.flatMap((g) => g.items), [grouped]);

  // Clamp de la sélection sur le nombre réel d'items affichés (âmes incluses)
  useEffect(() => {
    setSelected((s) => Math.min(s, Math.max(0, flatItems.length - 1)));
  }, [flatItems.length]);

  const runItem = (item: CommandItem) => {
    onClose();
    navigate(item.href);
  };

  // Gestion clavier
  useLayoutEffect(() => {
    if (!open) return undefined;
    const handler = (e: KeyboardEvent) => {
      if (e.key === 'Escape') {
        e.preventDefault();
        onClose();
        return;
      }
      if (e.key === 'ArrowDown') {
        e.preventDefault();
        setSelected((i) => Math.min(i + 1, flatItems.length - 1));
        return;
      }
      if (e.key === 'ArrowUp') {
        e.preventDefault();
        setSelected((i) => Math.max(i - 1, 0));
        return;
      }
      if (e.key === 'Enter') {
        e.preventDefault();
        const item = flatItems[selected];
        if (item) runItem(item);
      }
    };
    window.addEventListener('keydown', handler);
    return () => window.removeEventListener('keydown', handler);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [open, flatItems, selected, onClose]);

  // Verrouille le scroll du body quand la palette est ouverte
  useEffect(() => {
    if (!open) return undefined;
    const prev = document.body.style.overflow;
    document.body.style.overflow = 'hidden';
    return () => {
      document.body.style.overflow = prev;
    };
  }, [open]);

  // Scroll automatique vers l'élément sélectionné
  useEffect(() => {
    const el = listRef.current?.querySelector(`#cmd-item-${selected}`);
    el?.scrollIntoView({ block: 'nearest' });
  }, [selected]);


  if (!open) return null;

  return createPortal(
    <div
      className="fixed inset-0 z-[100] flex items-start justify-center pt-[12vh] px-4"
      onMouseDown={onClose}
      data-testid="command-palette-overlay"
    >
      <div className="absolute inset-0 bg-black/60 backdrop-blur-sm" />
      <div
        ref={dialogRef}
        role="dialog"
        aria-modal="true"
        aria-label="Palette de commandes"
        className={`relative w-full max-w-xl rounded-2xl shadow-2xl border overflow-hidden ${
          dark ? 'bg-gray-900/95 border-gray-700' : 'bg-white/95 border-gray-200'
        }`}
        onMouseDown={(e) => e.stopPropagation()}
      >
        {/* Champ de recherche */}
        <div className={`flex items-center gap-3 px-4 py-3.5 border-b ${dark ? 'border-gray-700/80' : 'border-gray-200'}`}>
          <Search className="w-5 h-5 shrink-0 text-gray-400" />
          <input
            ref={inputRef}
            type="text"
            value={query}
            onChange={(e) => { setQuery(e.target.value); setSelected(0); }}
            placeholder="Rechercher une page, une action..."
            className="flex-1 bg-transparent border-none outline-none text-sm text-gray-900 dark:text-gray-100 placeholder:text-gray-400"
            aria-label="Recherche"
            aria-controls="cmd-results"
            aria-activedescendant={flatItems[selected] ? `cmd-item-${selected}` : undefined}
          />
          <kbd className={`text-[10px] px-1.5 py-0.5 rounded border font-medium ${dark ? 'bg-gray-800 text-gray-400 border-gray-700' : 'bg-gray-100 text-gray-500 border-gray-200'}`}>
            ESC
          </kbd>
        </div>

        {/* Résultats */}
        <div ref={listRef} id="cmd-results" role="listbox" aria-label="Résultats" className="max-h-80 overflow-y-auto py-2">
          {soulsLoading && (
            <div className={`px-4 py-2 text-xs ${dark ? 'text-gray-500' : 'text-gray-400'}`}>
              Recherche des personnes…
            </div>
          )}
          {flatItems.length === 0 && !soulsLoading ? (
            <div className={`px-4 py-10 text-center text-sm ${dark ? 'text-gray-400' : 'text-gray-500'}`}>
              Aucun résultat pour «&nbsp;{query}&nbsp;»
            </div>
          ) : (
            grouped.map((group) => (
              <div key={group.section}>
                <div className={`px-4 py-1.5 text-[11px] font-semibold uppercase tracking-wider ${dark ? 'text-gray-500' : 'text-gray-400'}`}>
                  {group.section}
                </div>
                {group.items.map((item) => {
                  const globalIdx = flatItems.indexOf(item);
                  const isSelected = globalIdx === selected;
                  const Icon = item.icon;
                  return (
                    <button
                      key={item.id}
                      id={`cmd-item-${globalIdx}`}
                      type="button"
                      role="option"
                      aria-selected={isSelected}
                      onClick={() => runItem(item)}
                      onMouseEnter={() => setSelected(globalIdx)}
                      className={`w-full flex items-center gap-3 px-4 py-2.5 text-left transition-colors ${
                        isSelected
                          ? dark ? 'bg-primary-500/15 text-white' : 'bg-primary-500/10 text-gray-900'
                          : dark ? 'text-gray-300 hover:bg-gray-800/60' : 'text-gray-700 hover:bg-gray-100'
                      }`}
                    >
                      <span className={`flex items-center justify-center w-8 h-8 rounded-lg shrink-0 ${
                        isSelected
                          ? 'bg-primary-500/25 text-primary-500'
                          : dark ? 'bg-gray-800 text-gray-400' : 'bg-gray-100 text-gray-500'
                      }`}>
                        <Icon className="w-4 h-4" />
                      </span>
                      <span className="flex-1 min-w-0 text-sm font-medium truncate">{item.label}</span>
                      {isSelected && <ArrowRight className="w-4 h-4 shrink-0 text-primary-500" />}
                    </button>
                  );
                })}
              </div>
            ))
          )}
        </div>

        {/* Footer raccourcis */}
        <div className={`flex items-center gap-4 px-4 py-2.5 border-t text-[11px] ${dark ? 'border-gray-700/80 text-gray-500' : 'border-gray-200 text-gray-400'}`}>
          <span className="flex items-center gap-1.5">
            <kbd className={`px-1.5 py-0.5 rounded border font-medium ${dark ? 'bg-gray-800 border-gray-700' : 'bg-gray-100 border-gray-200'}`}>↑↓</kbd>
            naviguer
          </span>
          <span className="flex items-center gap-1.5">
            <kbd className={`px-1.5 py-0.5 rounded border font-medium ${dark ? 'bg-gray-800 border-gray-700' : 'bg-gray-100 border-gray-200'}`}>↵</kbd>
            ouvrir
          </span>
          <span className="flex items-center gap-1.5">
            <kbd className={`px-1.5 py-0.5 rounded border font-medium ${dark ? 'bg-gray-800 border-gray-700' : 'bg-gray-100 border-gray-200'}`}>esc</kbd>
            fermer
          </span>
          <span className="ml-auto flex items-center gap-1.5">
            <kbd className={`px-1.5 py-0.5 rounded border font-medium ${dark ? 'bg-gray-800 border-gray-700' : 'bg-gray-100 border-gray-200'}`}>Ctrl</kbd>
            <kbd className={`px-1.5 py-0.5 rounded border font-medium ${dark ? 'bg-gray-800 border-gray-700' : 'bg-gray-100 border-gray-200'}`}>K</kbd>
          </span>
        </div>
      </div>
    </div>,
    document.body,
  );
}


