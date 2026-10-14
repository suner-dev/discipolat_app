/**
 * Statut « exaucé » des prières — le backend écrit « EXAUCE » (une seule E),
 * l'interface historiquement comparait « EXAUCEE ». Toute comparaison de statut
 * doit passer par ce prédicat pour ne jamais masquer un sujet exaucé.
 */
export function isPriereExaucee(statut?: string | null): boolean {
  return statut === 'EXAUCE' || statut === 'EXAUCEE' || statut === 'RESOLUE';
}
