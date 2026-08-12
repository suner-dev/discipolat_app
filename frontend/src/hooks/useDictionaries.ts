import { useMemo } from 'react';
import { useQuery } from '@tanstack/react-query';
import api from '@/lib/api';
import type { DictionaryEntry, DictionariesMap } from '@/types';

/**
 * Référentiels configurables de la plateforme (dictionnaires).
 *
 * Charge une seule fois les listes (types d'événements, statuts, raisons
 * d'absence, catégories…) depuis le backend et les expose par clé.
 * Chaque dictionnaire peut être adapté par l'administrateur sans code ;
 * le frontend n'affiche que les entrées actives, dans l'ordre configuré.
 *
 * Exemple :
 *   const { options, label, color } = useDictionaries();
 *   options('EVENT_TYPE')            → entrées actives triées
 *   label('EVENT_TYPE', 'CULTE')     → « Culte » (ou le code si inconnu)
 *   color('EVENT_TYPE', 'CULTE')     → '#22c55e'
 */
export function useDictionaries() {
  const { data, isLoading } = useQuery<DictionariesMap>({
    queryKey: ['dictionaries'],
    queryFn: async () => (await api.get('/dictionaries')).data as DictionariesMap,
    staleTime: 10 * 60 * 1000, // les listes changent rarement
  });

  const dicts = useMemo(() => data || {}, [data]);

  /** Entrées actives d'un dictionnaire, dans l'ordre configuré. */
  const options = (key: string): DictionaryEntry[] => dicts[key] || [];

  /**
   * Libellé d'une entrée. Retourne '' si le code est inconnu ou si le
   * dictionnaire n'est pas encore chargé : les pages combinent avec leur
   * repli local (`label(...) || FALLBACK[code] || code`), ce qui garantit
   * un affichage correct même hors-ligne ou sans dictionnaires.
   */
  const label = (key: string, code?: string | null): string => {
    if (!code) return '';
    return dicts[key]?.find((e) => e.code === code)?.label || '';
  };

  /** Couleur d'une entrée (badges). */
  const color = (key: string, code?: string | null): string | undefined => {
    if (!code) return undefined;
    return dicts[key]?.find((e) => e.code === code)?.color;
  };

  /** Convertit un dictionnaire en options de <select> ({value, label}). */
  const selectOptions = (key: string) =>
    options(key).map((e) => ({ value: e.code, label: e.label }));

  return { dicts, options, label, color, selectOptions, isLoading };
}
