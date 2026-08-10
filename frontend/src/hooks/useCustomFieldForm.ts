import { useCallback, useMemo, useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import api from '@/lib/api';
import { useAuth } from '@/contexts/AuthContext';
import type { CustomFieldDefinition } from '@/types';

/**
 * Hook de formulaire pour les champs personnalisés.
 *
 * Charge les définitions actives visibles par le rôle courant, gère les
 * valeurs saisies, valide les champs obligatoires et sauvegarde l'ensemble
 * après la création de l'entité (PUT /custom-fields/{type}/{id}).
 *
 * Les champs dont le rôle actif n'est pas autorisé à écrire (roles_ecriture)
 * sont rendus en lecture seule côté interface — le backend les ignore aussi
 * (CustomFieldService.saveValues) : pas de perte silencieuse de saisie.
 */
export function useCustomFieldForm(
  entiteType: 'SOUL' | 'USER' | 'DEPARTMENT' | 'FAMILY',
  options?: { enabled?: boolean }
) {
  const { activeRole } = useAuth();
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

  /**
   * Champs visibles mais non éditables par le rôle actif (lecture seule).
   * Le backend applique la même restriction à la sauvegarde.
   */
  const readOnlyFieldIds = useMemo(() => {
    return new Set(
      definitions
        .filter(
          (d) =>
            d.rolesEcriture &&
            d.rolesEcriture.length > 0 &&
            !d.rolesEcriture.includes(activeRole ?? '')
        )
        .map((d) => d.id)
    );
  }, [definitions, activeRole]);

  /** Libellés des champs obligatoires non renseignés (hors champs non éditables). */
  const missingRequired = useMemo(() => {
    return definitions
      .filter(
        (d) =>
          !readOnlyFieldIds.has(d.id) &&
          d.obligatoire &&
          !(values[d.id] ?? d.defaultValue ?? '').trim()
      )
      .map((d) => d.label);
  }, [definitions, values, readOnlyFieldIds]);

  /**
   * Sauvegarde les valeurs saisies pour l'entité créée (no-op si aucun champ).
   * Retourne false si la sauvegarde échoue, afin que la création de l'entité
   * principale ne soit pas bloquée par une erreur secondaire.
   */
  const save = useCallback(async (entityId: string): Promise<boolean> => {
    if (definitions.length === 0) return true;
    try {
      // N'envoie que les champs que le rôle actif peut écrire (le backend
      // applique la même restriction côté serveur, par défense en profondeur).
      const editable = Object.fromEntries(
        Object.entries(values).filter(([fieldId]) => !readOnlyFieldIds.has(fieldId))
      );
      await api.put(`/custom-fields/${entiteType}/${entityId}`, editable);
      return true;
    } catch {
      return false;
    }
  }, [definitions.length, entiteType, values, readOnlyFieldIds]);

  return { definitions, isLoading, values, setValue, reset, missingRequired, readOnlyFieldIds, save };
}
