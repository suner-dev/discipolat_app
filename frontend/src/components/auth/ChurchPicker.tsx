import { useCallback, useEffect, useId, useRef, useState } from 'react';
import { Link } from 'react-router-dom';
import { Loader2, Search, Church, Check, AlertCircle, X } from 'lucide-react';
import api, { getErrorMessage } from '@/lib/api';
import { useI18n } from '@/i18n';

/**
 * LOT 1 §GLISE-D'ABORD (T1.2) — sélecteur d'église « église d'abord ».
 *
 * <p>Composent NOUVEAU, à deux hôtes ({@code /register} et {@code /login}). Il
 * raccourcit le chemin vers des capacités DÉJÀ là, sans jamais en inventer :
 * <ul>
 *   <li><b>A3 — parle la langue de l'existant.</b> Une église trouvée n'émet
 *       AUCUN état nouveau : le parent écrit les paramètres d'URL déjà
 *       implémentés ({@code ?church=}, {@code ?tenant=}). Le payload
 *       d'inscription et le contrôleur serveur ne bougent pas.</li>
 *   <li><b>D1/R1 — la preuve reste obligatoire.</b> Ce picker ne liste QUE des
 *       églises en opt-in public ({@code /public/churches/suggest} filtre
 *       {@code isListed=true} côté serveur) ; le slug qu'il peut transmettre est
 *       déjà exposé par l'annuaire. Il ne peut donc pas faire joindre une église
 *       arbitraire. « non trouvée » renvoie vers les flux de preuve existants
 *       (code d'invitation, lien d'invitation).</li>
 *   <li><b>D2/R3 — invisible = inexistant.</b> Une église non listée répond
 *       exactement comme une église fantôme ; le picker ne distingue jamais les
 *       deux.</li>
 * </ul>
 *
 * <p>Cinq états mesurés : {@code idle · searching · found · notFound · error}.
 * Combobox accessible (ARIA 1.2) : {@code aria-expanded}, flèches, Entrée, Échap,
 * {@code aria-activedescendant}. Recherche debouncée 300 ms.</p>
 */

type SuggestItem = { name: string; slug?: string; city?: string; country?: string };
type Status = 'idle' | 'searching' | 'found' | 'notFound' | 'error';

export interface ChurchPickerProps {
  /**
   * Église listée confirmée (sélection dans la liste ou {@code /exists} à
   * l'Entrée). Le parent applique les paramètres d'URL existants — le picker
   * n'écrit pas lui-même l'état d'authentification.
   */
  onSelect?: (church: { name: string; slug?: string }) => void;
  /** Le nom saisi ne correspond à aucune église publiée (D1 : prouver ensuite). */
  onNotFound?: (typedName: string) => void;
  /** Label associé (a11y) ; défaut « Votre église ». */
  label?: string;
  id?: string;
  /** Afficher le bloc de CTA « non trouvée » (défaut true). */
  showNotFoundCtas?: boolean;
  /** Cibles des CTA (pages déjà existantes). */
  joinHref?: string;
  acceptHref?: string;
  createHref?: string;
  /** Rendu compact pour la zone de connexion (D5 — facultatif). */
  compact?: boolean;
}

const SUGGEST_MIN = 2;
const DEBOUNCE_MS = 300;

export default function ChurchPicker({
  onSelect,
  onNotFound,
  label,
  id,
  showNotFoundCtas = true,
  joinHref = '/join',
  acceptHref = '/accept-invitation',
  createHref = '/register?mode=church',
  compact = false,
}: ChurchPickerProps) {
  const { t } = useI18n();
  const autoId = useId();
  const inputId = id ?? `church-picker-${autoId}`;
  const listboxId = `${inputId}-listbox`;

  const [query, setQuery] = useState('');
  const [status, setStatus] = useState<Status>('idle');
  const [items, setItems] = useState<SuggestItem[]>([]);
  const [open, setOpen] = useState(false);
  const [active, setActive] = useState(-1);
  const [selected, setSelected] = useState<SuggestItem | null>(null);
  const [errorMsg, setErrorMsg] = useState('');

  const timer = useRef<ReturnType<typeof setTimeout> | null>(null);
  const reqSeq = useRef(0);
  const inputRef = useRef<HTMLInputElement>(null);

  const clearTimer = () => {
    if (timer.current) {
      clearTimeout(timer.current);
      timer.current = null;
    }
  };

  const runSuggest = useCallback(async (raw: string) => {
    const q = raw.trim();
    if (q.length < SUGGEST_MIN) {
      setStatus('idle');
      setItems([]);
      setOpen(false);
      return;
    }
    setStatus('searching');
    const seq = ++reqSeq.current;
    try {
      const { data } = await api.get<{ total: number; items: SuggestItem[] }>(
        '/public/churches/suggest',
        { params: { q } },
      );
      // Requête obsolète (l'utilisateur a continué à taper) : on l'ignore.
      if (seq !== reqSeq.current) return;
      const list = Array.isArray(data?.items) ? data.items : [];
      setItems(list);
      setStatus(list.length ? 'found' : 'notFound');
      setOpen(list.length > 0);
      setActive(-1);
    } catch (err) {
      if (seq !== reqSeq.current) return;
      // Quota dépassé / réseau : on reste non-bloquant, l'utilisateur peut
      // continuer le formulaire classique — le picker est un raccourci, pas une
      // porte obligatoire.
      setStatus('error');
      setItems([]);
      setOpen(false);
      setErrorMsg(getErrorMessage(err));
    }
  }, []);

  // Debounce 300 ms sur la saisie.
  useEffect(() => {
    clearTimer();
    if (!query.trim()) {
      setStatus('idle');
      setItems([]);
      setOpen(false);
      return;
    }
    timer.current = setTimeout(() => void runSuggest(query), DEBOUNCE_MS);
    return clearTimer;
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [query]);

  const choose = useCallback((item: SuggestItem) => {
    setSelected(item);
    setQuery(item.name);
    setOpen(false);
    setActive(-1);
    setStatus('found');
    onSelect?.({ name: item.name, slug: item.slug });
  }, [onSelect]);

  /** Entrée sans suggestion active : confirme le nom exact via /exists (anti-
   *  énumération — « non listée » répond comme « inexistante »). */
  const confirmExact = useCallback(async () => {
    const q = query.trim();
    if (!q) return;
    if (active >= 0 && items[active]) {
      choose(items[active]);
      return;
    }
    try {
      const { data } = await api.get<{ found: boolean; slug?: string; name?: string }>(
        '/public/churches/exists',
        { params: { q } },
      );
      if (data?.found) {
        const item: SuggestItem = { name: data.name ?? q, slug: data.slug };
        choose(item);
      } else {
        setStatus('notFound');
        setOpen(false);
        onNotFound?.(q);
      }
    } catch {
      setStatus('error');
      setOpen(false);
    }
  }, [query, active, items, choose, onNotFound]);

  const onKeyDown = (e: React.KeyboardEvent<HTMLInputElement>) => {
    if (e.key === 'ArrowDown') {
      if (items.length) {
        e.preventDefault();
        setOpen(true);
        setActive((i) => (i + 1) % items.length);
      }
    } else if (e.key === 'ArrowUp') {
      if (items.length) {
        e.preventDefault();
        setOpen(true);
        setActive((i) => (i - 1 + items.length) % items.length);
      }
    } else if (e.key === 'Enter') {
      // Ne pas soumettre le formulaire hôte : on confirme l'église d'abord.
      e.preventDefault();
      void confirmExact();
    } else if (e.key === 'Escape') {
      setOpen(false);
      setActive(-1);
    }
  };

  const reset = () => {
    setSelected(null);
    setQuery('');
    setStatus('idle');
    setItems([]);
    setOpen(false);
    setActive(-1);
    inputRef.current?.focus();
  };

  const showList = open && items.length > 0;

  return (
    <div className={compact ? 'space-y-2' : 'space-y-3'} data-testid="church-picker">
      <label htmlFor={inputId} className="block text-sm font-medium text-gray-600 dark:text-gray-300">
        {label ?? t('churchPicker.label')}
        <span className="ml-1.5 text-xs font-normal text-gray-400">
          {t('churchPicker.optional')}
        </span>
      </label>

      <div className="relative">
        <span className="pointer-events-none absolute inset-y-0 left-0 flex items-center pl-3 text-gray-400">
          {status === 'searching'
            ? <Loader2 className="w-4 h-4 animate-spin" />
            : <Search className="w-4 h-4" />}
        </span>
        <input
          ref={inputRef}
          id={inputId}
          type="text"
          role="combobox"
          aria-expanded={showList}
          aria-controls={listboxId}
          aria-autocomplete="list"
          aria-activedescendant={showList && active >= 0 ? `${listboxId}-opt-${active}` : undefined}
          autoComplete="off"
          value={query}
          placeholder={t('churchPicker.placeholder')}
          onChange={(e) => {
            setSelected(null);
            setQuery(e.target.value);
          }}
          onKeyDown={onKeyDown}
          className={`w-full rounded-xl bg-gray-100/80 dark:bg-white/5 border border-gray-200 dark:border-white/10 text-gray-900 dark:text-white placeholder-gray-400
                     pl-10 pr-9 ${compact ? 'py-2' : 'py-3'} text-sm focus:outline-none focus:border-primary-500/50 focus:ring-2 focus:ring-primary-500/20 transition-all duration-200`}
        />
        {query && (
          <button
            type="button"
            onClick={reset}
            aria-label={t('churchPicker.clear')}
            className="absolute inset-y-0 right-0 flex items-center pr-3 text-gray-400 hover:text-gray-600 dark:hover:text-gray-200"
          >
            <X className="w-4 h-4" />
          </button>
        )}

        {showList && (
          <ul
            id={listboxId}
            role="listbox"
            aria-label={t('churchPicker.results')}
            className="absolute z-20 mt-1 max-h-60 w-full overflow-auto rounded-xl border border-gray-200 dark:border-white/10 bg-white dark:bg-gray-900 shadow-lg py-1"
          >
            {items.map((it, i) => (
              <li
                key={`${it.slug ?? it.name}-${i}`}
                id={`${listboxId}-opt-${i}`}
                role="option"
                aria-selected={i === active}
                onMouseEnter={() => setActive(i)}
                onClick={() => choose(it)}
                className={`flex cursor-pointer items-center gap-2 px-3 py-2 text-sm ${
                  i === active ? 'bg-primary-500/10 text-primary-700 dark:text-primary-300' : 'text-gray-700 dark:text-gray-200'
                }`}
              >
                <Church className="w-4 h-4 shrink-0 opacity-70" />
                <span className="truncate">{it.name}</span>
                {(it.city || it.country) && (
                  <span className="ml-auto shrink-0 text-xs text-gray-400">
                    {[it.city, it.country].filter(Boolean).join(', ')}
                  </span>
                )}
              </li>
            ))}
          </ul>
        )}
      </div>

      {/* État « trouvée » : on confirme ce que fait le parent (params URL existants). */}
      {selected && (
        <p className="flex items-center gap-1.5 text-xs font-medium text-green-600 dark:text-green-400">
          <Check className="w-3.5 h-3.5" /> {t('churchPicker.found', { name: selected.name })}
        </p>
      )}

      {/* État « non trouvée » : texte explicite + CTA vers les pages DÉJÀ là. */}
      {status === 'notFound' && !selected && (
        <div className="rounded-xl border border-amber-500/25 bg-amber-500/5 p-3">
          <p className="flex items-start gap-1.5 text-xs text-amber-700 dark:text-amber-300">
            <AlertCircle className="w-3.5 h-3.5 mt-0.5 shrink-0" />
            {t('churchPicker.notFound')}
          </p>
          {showNotFoundCtas && (
            <div className="mt-2 flex flex-wrap gap-2">
              <Link to={joinHref} className="text-xs font-medium text-primary-600 dark:text-primary-400 hover:underline">
                {t('churchPicker.ctaCode')}
              </Link>
              <span className="text-gray-300 dark:text-white/20">·</span>
              <Link to={acceptHref} className="text-xs font-medium text-primary-600 dark:text-primary-400 hover:underline">
                {t('churchPicker.ctaInvite')}
              </Link>
              <span className="text-gray-300 dark:text-white/20">·</span>
              <Link to={createHref} className="text-xs font-medium text-primary-600 dark:text-primary-400 hover:underline">
                {t('churchPicker.ctaCreate')}
              </Link>
            </div>
          )}
        </div>
      )}

      {/* État « erreur » : non-bloquant, le picker reste un raccourci. */}
      {status === 'error' && (
        <p className="text-xs text-gray-500 dark:text-gray-400">
          {errorMsg || t('churchPicker.error')}
        </p>
      )}
    </div>
  );
}
