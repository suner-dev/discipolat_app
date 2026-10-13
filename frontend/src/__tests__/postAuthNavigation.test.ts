import { describe, it, expect, beforeEach } from 'vitest';
import {
  safeInternalPath,
  rememberPostAuthPath,
  takeRememberedPostAuthPath,
} from '@/lib/postAuthNavigation';

/**
 * Filtre de redirection PARTAGÉ par `/login`, le challenge 2FA et la
 * vérification du magic link. Une seule définition pour les trois : c'est
 * justement le but du module, un filtre dupliqué étant un filtre qu'on oublie
 * quelque part.
 */
describe('safeInternalPath', () => {
  it.each([
    ['/j/bethel-cocody'],
    ['/join'],
    ['/join?code=ABC-123'],
    ['/dashboard'],
    ['/eglises'],
    ['/verify-2fa?next=%2Fj%2Fx'],
  ])('accepte le chemin interne %s', (path) => {
    expect(safeInternalPath(path)).toBe(path);
  });

  it.each([
    ['https://evil.example/steal', 'absolu https'],
    ['http://evil.example', 'absolu http'],
    ['javascript:alert(1)', 'javascript:'],
    ['//evil.example/steal', 'protocol-relative //'],
    ['/\\evil.example/steal', 'protocol-relative /\\'],
    ['/https://evil.example', 'schéma remonté en chemin'],
    ['dashboard', 'chemin relatif sans racine'],
    ['../admin', 'remontée de chemin'],
  ])('refuse %s (%s)', (path) => {
    expect(safeInternalPath(path)).toBeNull();
  });

  it.each([null, undefined, '', '   '])('refuse la valeur vide %p', (value) => {
    expect(safeInternalPath(value)).toBeNull();
  });

  it('tolère les espaces autour sans les garder', () => {
    expect(safeInternalPath('  /j/x  ')).toBe('/j/x');
  });
});

/**
 * Le relais n'est qu'un CONFORT (le magic link peut arriver sur un autre
 * appareil) ; il ne doit jamais devenir un vecteur. D'où le filtre à l'écriture
 * ET à la lecture, et la consommation immédiate.
 */
describe('relais post-authentification', () => {
  beforeEach(() => {
    sessionStorage.clear();
  });

  it('relaie puis restitue la destination', () => {
    rememberPostAuthPath('/j/bethel-cocody');
    expect(takeRememberedPostAuthPath()).toBe('/j/bethel-cocody');
  });

  it('CONSOUME la destination : elle ne ressuscite pas à la navigation suivante', () => {
    rememberPostAuthPath('/j/bethel-cocody');
    expect(takeRememberedPostAuthPath()).toBe('/j/bethel-cocody');
    expect(takeRememberedPostAuthPath()).toBeNull();
  });

  it('refuse une destination externe posée dans le relais', () => {
    sessionStorage.setItem('discipolat.postAuthPath', 'https://evil.example');
    expect(takeRememberedPostAuthPath()).toBeNull();
  });

  it('remember(null) efface un relais précédent au lieu de le laisser survivre', () => {
    rememberPostAuthPath('/j/x');
    rememberPostAuthPath(null);
    expect(takeRememberedPostAuthPath()).toBeNull();
  });

  it('read() ne lève pas quand sessionStorage est indisponible', () => {
    const original = Storage.prototype.getItem;
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    (Storage.prototype as any).getItem = () => { throw new Error('SecurityError'); };
    try {
      expect(takeRememberedPostAuthPath()).toBeNull();
    } finally {
      // eslint-disable-next-line @typescript-eslint/no-explicit-any
      (Storage.prototype as any).getItem = original;
    }
  });
});
