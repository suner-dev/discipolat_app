import { useCallback, useMemo, useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import api from '@/lib/api';
import type { CustomFieldDefinition } from '@/types';

/**
 * Hook de formulaire pour les champs personnalisés.
 *
 * Charge les définitions actives visibles par le rôle courant, gère les
 * valeurs saisies, valide les champs obligatoires et sauvegarde l'ensemble
 * après la création de l'entité (PUT /custom-fields/{type}/{id}).
 */
export function useCustomFieldForm(
  entiteType: 'SOUL' | 'USER' | 'DEPARTMENT' | 'FAMILY',
  options?: { enabled?: boolean }
) {
  const { data: definitions = [], isLoading } = useQuery({
    queryKey: ['custom-fields', 'definitions', entiteType],
    queryFn: async () => {
      const res = await api.get(`/custom-fields/definitions?entiteType=${entiteType}`);
      const defs = Array.isArray(res.data) ? (res.data as CustomFieldDefinition[]) : [];
      return defs.map((d) => ({
        ...d,
        value: d.defaultValue || '',
      }));
    },
    enabled: options?.enabled ?? true,
    staleTime: 60_000,
  });

  const [values, setValues] = useState<Record<string, string>>({});

  const setValue = useCallback((fieldId: string, value: string) => {
    setValues((prev) => ({ ...prev, [fieldId]: value }));
  }, []);

  const reset = useCallback(() => setValues({}), []);

  /** Libellés des champs obligatoires non renseignés. */
  const missingRequired = useMemo(() => {
    return definitions
      .filter((d) => d.obligatoire && !(values[d.id] ?? d.defaultValue ?? '').trim())
      .map((d) => d.label);
  }, [definitions, values]);

  /**
   * Sauvegarde les valeurs saisies pour l'entité créée (no-op si aucun champ).
   * Retourne false si la sauvegarde échoue, afin que la création de l'entité
   * principale ne soit pas bloquée par une erreur secondaire.
   */
  const save = useCallback(async (entityId: string): Promise<boolean> => {
    if (definitions.length === 0) return true;
    try {
      await api.put(`/custom-fields/${entiteType}/${entityId}`, values);
      return true;
    } catch {
      return false;
    }
  }, [definitions.length, entiteType, values]);

  return { definitions, isLoading, values, setValue, reset, missingRequired, save };
}
