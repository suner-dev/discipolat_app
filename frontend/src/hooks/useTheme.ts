import { useCallback, useEffect, useState } from 'react';

/**
 * Thème clair / sombre partagé.
 * Persisté dans `localStorage.darkMode` et appliqué via la classe `dark`
 * sur `document.documentElement` (tailwind darkMode: 'class').
 * Réutilisé par la Navbar et les pages d'authentification.
 */
export function useTheme() {
  const [darkMode, setDarkMode] = useState(() => {
    if (typeof window !== 'undefined') {
      return localStorage.getItem('darkMode') === 'true';
    }
    return false;
  });

  useEffect(() => {
    if (darkMode) {
      document.documentElement.classList.add('dark');
    } else {
      document.documentElement.classList.remove('dark');
    }
    localStorage.setItem('darkMode', String(darkMode));
  }, [darkMode]);

  const toggleTheme = useCallback(() => setDarkMode((d) => !d), []);

  return { darkMode, toggleTheme };
}
