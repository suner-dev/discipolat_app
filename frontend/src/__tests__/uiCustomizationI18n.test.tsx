import { describe, it, expect, beforeEach } from 'vitest';
import { I18nProvider, useI18n, tText } from '@/i18n';
import { render, screen } from '@testing-library/react';
import { resetUiCustomization, setUiCustomization } from '@/config/uiCustomization';


/**
 * LOT 2 §LB — la surcharge d'église doit gagner sur le dictionnaire i18n.
 *
 * <p>C'est le test qui prouve la promesse « chaque nom paramétrable » : une
 * chaîne <b>déjà traduite</b> est renommée par l'administration sans qu'aucune
 * page ne soit modifiée. Le point d'injection est unique — la fonction `t()` —
 * donc si ce test passe, toute l'application est paramétrable.
 */
function Probe() {
  const { t } = useI18n();
  return (
    <div>
      <span data-testid="by-key">{t('nav.back')}</span>
      <span data-testid="with-params">{t('greeting.hello', { name: 'Rachel' })}</span>
      <span data-testid="by-source">{tText('Retour')}</span>
      <span data-testid="unknown">{t('cle.inexistante')}</span>
    </div>
  );
}

function renderProbe() {
  return render(<I18nProvider><Probe /></I18nProvider>);
}

describe('LOT 2 §LB — la surcharge d’église prime sur le dictionnaire', () => {
  beforeEach(() => {
    resetUiCustomization();
    localStorage.clear();
  });

  it('sans configuration, le dictionnaire fait foi (comportement inchangé)', () => {
    renderProbe();
    // Comportement d'avant : le dictionnaire répond.
    expect(screen.getByTestId('by-key').textContent).toBe('Retour');
    expect(screen.getByTestId('by-source').textContent).not.toBe('');
  });

  it('la clé i18n est remplacée', () => {
    setUiCustomization({ labels: { 'nav.back': 'Revenir' } });
    renderProbe();
    expect(screen.getByTestId('by-key').textContent).toBe('Revenir');
  });

  it('un libellé source est remplacé lui aussi', () => {
    setUiCustomization({ labels: { Retour: 'Revenir' } });
    renderProbe();
    expect(screen.getByTestId('by-source').textContent).toBe('Revenir');
  });

  it('les paramètres {…} sont toujours interpolés dans la surcharge', () => {
    setUiCustomization({ labels: { 'greeting.hello': 'Bonjour {name} !' } });
    renderProbe();
    expect(screen.getByTestId('with-params').textContent).toBe('Bonjour Rachel !');
  });

  it('une clé inconnue reste la clé elle-même, jamais un blanc', () => {
    setUiCustomization({ labels: { 'nav.back': 'Revenir' } });
    renderProbe();
    expect(screen.getByTestId('unknown').textContent).toBe('cle.inexistante');
  });

  it('reset restaure le dictionnaire', () => {
    setUiCustomization({ labels: { 'nav.back': 'Revenir' } });
    const { unmount } = renderProbe();
    expect(screen.getByTestId('by-key').textContent).toBe('Revenir');
    unmount();

    resetUiCustomization();
    renderProbe();
    expect(screen.getByTestId('by-key').textContent).toBe('Retour');
  });
});

describe('LOT 2 §LB — le magasin reste sûr hors application', () => {
  it('tText ne plante pas sur une chaîne vide', () => {
    expect(tText('')).toBe('');
  });

  it('tText renvoie la chaîne source quand elle est introuvable', () => {
    // Utile hors provider et pour une chaîne jamais traduite : le texte
    // français doit rester lisible, pas disparaître.
    expect(tText('libellé jamais traduit')).toBe('libellé jamais traduit');
  });
});