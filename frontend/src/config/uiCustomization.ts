/**
 * LOT 2 §LB — magasin des surcharges de libellés et de fonctionnalités.
 *
 * <p><b>Pourquoi ce module existe.</b> L'application traduit ses libellés via un
 * dictionnaire statique de ~3 300 entrées. Rendre « chaque nom paramétrable »
 * ne peut pas consistanter à ajouter une nouvelle chaîne au code pour chaque
 * besoin : il faut que l'administration puisse <b>renommer ce qui existe déjà</b>.
 *
 * <p>La solution retenue est un magasin <b>module-level</b>, volontairement hors
 * de React : c'est ce qui permet à la fonction {@code t()} — appelée partout,
 * y compris hors composant — de consulter la surcharge <b>avant</b> le
 * dictionnaire. Un contexte React n'aurait pas pu être utilisé par {@code t()}
 * sans la rendre asynchrone ou la faire échouer hors composant.
 *
 * <p><b>Ordre de résolution</b> implémenté par {@link resolveLabel} :
 * surcharge d'`*` → surcharge de la locale → dictionnaire de la locale →
 * dictionnaire `fr` → la clé elle-même.
 *
 * <p>Le magasin est vide par défaut : sans configuration serveur, le
 * comportement de l'application est **strictement identique** à celui d'avant.
 */

/** Réglage de fonctionnalité résolu pour un couple écran/fonctionnalité. */
export interface UiFeatureSetting {
  enabled: boolean;
  label?: string | null;
  displayOrder?: number;
  moduleKey?: string | null;
}

export interface UiCustomizationState {
  /** Surcharges de libellés déjà résolues par le serveur (par langue). */
  labels: Record<string, string>;
  /** Réglages de fonctionnalités indexés par `pageKey:featureKey`. */
  features: Record<string, UiFeatureSetting>;
}

const EMPTY: UiCustomizationState = { labels: {}, features: {} };

let state: UiCustomizationState = EMPTY;
const listeners = new Set<() => void>();

/** Installe les surcharges reçues du serveur. Réponse incomplète = ignorée. */
export function setUiCustomization(next: Partial<UiCustomizationState> | null | undefined): void {
  state = {
    labels: next?.labels ?? {},
    features: next?.features ?? {},
  };
  listeners.forEach((listener) => listener());
}

/** Vide le magasin — utilisé à la déconnexion et à la sortie d'une église. */
export function resetUiCustomization(): void {
  setUiCustomization(EMPTY);
}

/** Instantané courant (utile pour les tests et le débogage). */
export function getUiCustomization(): UiCustomizationState {
  return state;
}

/** S'abonner aux changements, pour les composants qui doivent se redessiner. */
export function subscribeUiCustomization(listener: () => void): () => void {
  listeners.add(listener);
  return () => {
    listeners.delete(listener);
  };
}

/**
 * Surcharge d'un libellé, ou `undefined` si l'administration n'a rien changé.
 *
 * <p>On accepte aussi bien une **clé i18n** (`souls.title`) qu'un **libellé
 * source** en français (`Retour`) : le dépôt compte des centaines de chaînes
 * écrites en dur et traduites à la volée par {@code tText}, qui n'ont aucune
 * clé. Sans cela, la moitié de l'application resterait non paramétrable.
 */
export function resolveLabel(key: string, locale?: string): string | undefined {
  if (!key) return undefined;
  const labels = state.labels;
  if (Object.keys(labels).length === 0) return undefined;

  // Clé i18n d'abord…
  const direct = labels[key];
  if (direct !== undefined) return direct;

  // …puis le libellé source, éventuellement préfixé par la langue telle que
  // le dépôt l'écrit (« Retour@fr ») ou suffixe «<locale> ».
  if (locale) {
    const localized = labels[`${key}@${locale}`];
    if (localized !== undefined) return localized;
  }
  return undefined;
}

/** Clé composite d'une fonctionnalité, alignée sur le backend. */
export function featureKey(pageKey: string, featureKeyValue: string): string {
  return `${pageKey}:${featureKeyValue}`;
}

/**
 * Réglage d'une fonctionnalité.
 *
 * <p><b>Défaut ouvert</b> : une fonctionnalité absente est <b>activée</b>. Seule
 * une ligne explicite `enabled: false` la retire. C'est ce qui rend le
 * paramétrage intuitif — « je désactive ce bouton » — sans le rendre
 * destructeur au premier réglage enregistré.
 */
export function resolveFeature(
  pageKey: string,
  featureKeyValue: string,
): UiFeatureSetting | undefined {
  return state.features[featureKey(pageKey, featureKeyValue)];
}

/** `true` si la fonctionnalité doit être rendue sur cet écran. */
export function isFeatureEnabled(pageKey: string, featureKeyValue: string): boolean {
  return resolveFeature(pageKey, featureKeyValue)?.enabled !== false;
}

/** Libellé personnalisé d'un bouton, ou `undefined` s'il n'est pas renommé. */
export function resolveFeatureLabel(
  pageKey: string,
  featureKeyValue: string,
): string | undefined {
  const label = resolveFeature(pageKey, featureKeyValue)?.label;
  return label ? label : undefined;
}