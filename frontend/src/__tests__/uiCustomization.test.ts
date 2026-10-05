import { describe, it, expect, beforeEach } from 'vitest';
import {
  getUiCustomization,
  isFeatureEnabled,
  resetUiCustomization,
  resolveFeature,
  resolveFeatureLabel,
  resolveLabel,
  setUiCustomization,
  subscribeUiCustomization,
} from '@/config/uiCustomization';

/**
 * LOT 2 §LB — le magasin de paramétrage.
 *
 * Ces tests verrouillent le contrat qui rend « chaque nom paramétrable » vrai :
 * une surcharge gagne sur le dictionnaire, et son ABSENCE ne change rien au
 * comportement par défaut (le vide est le mode normal de l'application).
 */
describe('LOT 2 §LB — magasin de paramétrage de l’interface', () => {
  beforeEach(() => {
    resetUiCustomization();
  });

  describe('libellés', () => {
    it('renvoie undefined quand rien n’est configuré', () => {
      expect(resolveLabel('souls.title')).toBeUndefined();
      expect(resolveLabel('')).toBeUndefined();
    });

    it('surcharge une clé i18n', () => {
      setUiCustomization({ labels: { 'souls.title': 'Mes âmes' } });
      expect(resolveLabel('souls.title')).toBe('Mes âmes');
    });

    it('surcharge un libellé source sans clé i18n', () => {
      // C'est le cas de centaines de chaînes écrites en dur et traduites à la
      // volée par tText : sans ce point, la moitié de l'app resterait figée.
      setUiCustomization({ labels: { Retour: 'Revenir en arrière' } });
      expect(resolveLabel('Retour')).toBe('Revenir en arrière');
    });

    it('accepte une surcharge suffixée par la langue', () => {
      setUiCustomization({ labels: { 'Retour@fr': 'Revenir' } });
      expect(resolveLabel('Retour', 'fr')).toBe('Revenir');
      expect(resolveLabel('Retour', 'en')).toBeUndefined();
    });

    it('privilégie la clé i18n sur le libellé source', () => {
      setUiCustomization({ labels: { 'nav.back': 'Depuis la clé', Retour: 'Depuis le texte' } });
      expect(resolveLabel('nav.back')).toBe('Depuis la clé');
      expect(resolveLabel('Retour')).toBe('Depuis le texte');
    });

    it('accepte une chaîne vide (l’admin peut vouloir masquer un libellé)', () => {
      setUiCustomization({ labels: { 'souls.title': '' } });
      expect(resolveLabel('souls.title')).toBe('');
    });

    it('une réponse partielle ne casse rien', () => {
      setUiCustomization({ labels: { a: 'A' } });
      expect(resolveLabel('b')).toBeUndefined();
      expect(resolveLabel('a')).toBe('A');
    });

    it('reset vide le magasin', () => {
      setUiCustomization({ labels: { a: 'A' }, features: {} });
      resetUiCustomization();
      expect(getUiCustomization()).toEqual({ labels: {}, features: {} });
      expect(resolveLabel('a')).toBeUndefined();
    });

    it('null ou undefined réinitialise sans planter', () => {
      setUiCustomization({ labels: { a: 'A' } });
      setUiCustomization(null);
      expect(resolveLabel('a')).toBeUndefined();
    });
  });

  describe('fonctionnalités et boutons', () => {
    it('une fonctionnalité ABSENTE reste active : le défaut est ouvert', () => {
      // Sûr : un défaut « caché » ferait disparaître l'application entière au
      // premier réglage enregistré par un admin.
      expect(isFeatureEnabled('/souls', 'export')).toBe(true);
      expect(resolveFeature('/souls', 'export')).toBeUndefined();
    });

    it('une fonctionnalité explicitement désactivée est retirée', () => {
      setUiCustomization({
        features: { '/souls:export': { enabled: false } },
      });
      expect(isFeatureEnabled('/souls', 'export')).toBe(false);
    });

    it('une désactivation ne concerne que son écran', () => {
      setUiCustomization({ features: { '/souls:export': { enabled: false } } });
      expect(isFeatureEnabled('/departments', 'export')).toBe(true);
    });

    it('une fonctionnalité activée explicitement reste active', () => {
      setUiCustomization({ features: { '/souls:import': { enabled: true } } });
      expect(isFeatureEnabled('/souls', 'import')).toBe(true);
    });

    it('le bouton peut être renommé', () => {
      setUiCustomization({
        features: { '/souls:export': { enabled: true, label: 'Télécharger la liste' } },
      });
      expect(resolveFeatureLabel('/souls', 'export')).toBe('Télécharger la liste');
    });

    it('un libellé vide ne compte pas comme renommage', () => {
      setUiCustomization({ features: { '/souls:export': { enabled: true, label: '' } } });
      expect(resolveFeatureLabel('/souls', 'export')).toBeUndefined();
    });

    it('expose l’ordre d’affichage personnalisé', () => {
      setUiCustomization({ features: { '/souls:export': { enabled: true, displayOrder: 7 } } });
      expect(resolveFeature('/souls', 'export')?.displayOrder).toBe(7);
    });
  });

  describe('abonnements', () => {
    it('notifie les abonnés à chaque installation', () => {
      let calls = 0;
      const unsubscribe = subscribeUiCustomization(() => { calls += 1; });

      setUiCustomization({ labels: { a: 'A' } });
      setUiCustomization({ labels: { b: 'B' } });
      resetUiCustomization();
      unsubscribe();
      setUiCustomization({ labels: { c: 'C' } });

      expect(calls).toBe(3);
    });
  });
});