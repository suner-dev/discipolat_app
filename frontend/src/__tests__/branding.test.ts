import { describe, it, expect, beforeEach } from 'vitest';
import { shadeScale, fontStack, applyBranding, DEFAULT_BRANDING } from '@/lib/branding';

describe('branding — génération de la palette', () => {
  it('produit les 11 teintes depuis une couleur de base', () => {
    const scale = shadeScale('#16a34a');
    expect(Object.keys(scale)).toHaveLength(11);
    // La teinte 500 correspond à la couleur de base.
    expect(scale['500']).toBe('22 163 74');
  });

  it('les teintes claires sont plus proches du blanc, les foncées du noir', () => {
    const scale = shadeScale('#16a34a');
    const [r50, g50, b50] = scale['50'].split(' ').map(Number);
    const [r950, g950, b950] = scale['950'].split(' ').map(Number);
    expect(r50 + g50 + b50).toBeGreaterThan(r950 + g950 + b950);
  });

  it('tolère les formats courts et les défauts', () => {
    expect(shadeScale('#0f0')['500']).toBe('0 255 0');
    expect(shadeScale('')['500']).toBe('22 163 74'); // défaut primaire
  });
});

describe('branding — typographie', () => {
  it('retourne la pile Inter par défaut', () => {
    expect(fontStack('Inconnue')).toContain('Inter');
  });
  it('résout les polices connues', () => {
    expect(fontStack('Poppins')).toContain('Poppins');
  });
});

describe('branding — application du thème', () => {
  beforeEach(() => {
    document.documentElement.style.cssText = '';
    document.title = 'test';
  });

  it('applique les variables CSS et le titre', () => {
    applyBranding({
      ...DEFAULT_BRANDING,
      primaryColor: '#0f766e',
      buttonColor: '#0f766e',
      platformName: 'Ma Plateforme',
    });
    const root = document.documentElement.style;
    expect(root.getPropertyValue('--color-primary-500')).toContain('15 118 110');
    expect(root.getPropertyValue('--color-button')).toContain('15 118 110');
    expect(root.getPropertyValue('--color-gold-500')).toContain('245 158 11');
    expect(document.title).toBe('Ma Plateforme');
  });

  it('désactive le mode sombre si l’église l’interdit', () => {
    document.documentElement.classList.add('dark');
    applyBranding({ ...DEFAULT_BRANDING, allowDarkMode: false });
    expect(document.documentElement.classList.contains('dark')).toBe(false);
  });
});
