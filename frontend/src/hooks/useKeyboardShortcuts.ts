import { useEffect, useCallback, useRef } from 'react';

interface KeyboardShortcut {
  key: string;
  ctrl?: boolean;
  meta?: boolean;
  shift?: boolean;
  alt?: boolean;
  action: () => void;
  description: string;
  enabled?: () => boolean;
}

interface UseKeyboardShortcutsOptions {
  enabled?: boolean;
  excludeSelectors?: string[];
}

export function useKeyboardShortcuts(
  shortcuts: KeyboardShortcut[],
  options: UseKeyboardShortcutsOptions = {},
) {
  const { enabled = true, excludeSelectors = ['INPUT', 'TEXTAREA', 'SELECT'] } = options;
  const shortcutsRef = useRef(shortcuts);
  const enabledRef = useRef(enabled);

  useEffect(() => {
    shortcutsRef.current = shortcuts;
  }, [shortcuts]);

  useEffect(() => {
    enabledRef.current = enabled;
  }, [enabled]);

  const handleKeyDown = useCallback(
    (e: KeyboardEvent) => {
      if (!enabledRef.current) return;

      // Ignore si on est dans un champ de formulaire (sauf si shortcut spécifiquement pour ça)
      const target = e.target as HTMLElement;
      if (excludeSelectors.some((sel) => target.matches(sel))) {
        // Exception : Ctrl+K marche même dans les formulaires
        if (!(e.ctrlKey || e.metaKey) || e.key.toLowerCase() !== 'k') {
          return;
        }
      }

      const currentShortcuts = shortcutsRef.current;
      for (const shortcut of currentShortcuts) {
        if (!shortcut.enabled || shortcut.enabled()) {
          const keyMatch =
            e.key.toLowerCase() === shortcut.key.toLowerCase() ||
            e.key === shortcut.key;
          const ctrlMatch = (e.ctrlKey || e.metaKey) === (shortcut.ctrl || shortcut.meta || false);
          const shiftMatch = shortcut.shift ? e.shiftKey : true;
          const altMatch = shortcut.alt ? e.altKey : true;

          if (keyMatch && ctrlMatch && shiftMatch && altMatch) {
            e.preventDefault();
            shortcut.action();
            return;
          }
        }
      }
    },
    [],
  );

  useEffect(() => {
    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [handleKeyDown]);

  return {
    register: (shortcut: KeyboardShortcut) => {
      shortcutsRef.current = [...shortcutsRef.current, shortcut];
    },
    unregister: (key: string, ctrl?: boolean, meta?: boolean) => {
      shortcutsRef.current = shortcutsRef.current.filter(
        (s) =>
          !(s.key === key && (s.ctrl || s.meta || false) === (ctrl || meta || false)),
      );
    },
  };
}

// Hooks spécialisés
export function useCommandPalette(openCommandPalette: () => void) {
  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if ((e.ctrlKey || e.metaKey) && e.key.toLowerCase() === 'k') {
        e.preventDefault();
        openCommandPalette();
      }
    };

    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [openCommandPalette]);
}

export function useEscapeClose(onClose: () => void, enabled = true) {
  useEffect(() => {
    if (!enabled) return;

    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === 'Escape') {
        e.preventDefault();
        onClose();
      }
    };

    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [onClose, enabled]);
}

export function useFocusTrap(containerRef: React.RefObject<HTMLElement>, enabled = true) {
  useEffect(() => {
    if (!enabled || !containerRef.current) return;

    const container = containerRef.current;
    const focusableSelectors = [
      'a[href]',
      'button:not([disabled])',
      'input:not([disabled])',
      'select:not([disabled])',
      'textarea:not([disabled])',
      '[tabindex]:not([tabindex="-1"])',
    ].join(', ');

    const getFocusableElements = () =>
      Array.from(container.querySelectorAll<HTMLElement>(focusableSelectors)).filter(
        (el) => el.offsetParent !== null,
      );

    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key !== 'Tab') return;

      const focusable = getFocusableElements();
      if (focusable.length === 0) return;

      const first = focusable[0];
      const last = focusable[focusable.length - 1];

      if (e.shiftKey) {
        if (document.activeElement === first) {
          e.preventDefault();
          last.focus();
        }
      } else {
        if (document.activeElement === last) {
          e.preventDefault();
          first.focus();
        }
      }
    };

    container.addEventListener('keydown', handleKeyDown);
    return () => container.removeEventListener('keydown', handleKeyDown);
  }, [containerRef, enabled]);
}