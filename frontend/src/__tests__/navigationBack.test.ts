import { describe, it, expect } from 'vitest';
import {
  breadcrumbTrail,
  deriveParentPath,
  hasUsableHistory,
  humanizeSegment,
  isKnownRoute,
  matchesRoutePattern,
  pathSegments,
  resolveBack,
} from '@/navigation/back';

describe('LOT 2 §BK — retour arrière & fil d’Ariane', () => {
  describe('utilitaires de chemin', () => {
    it('ignore la query string et le fragment', () => {
      expect(pathSegments('/departments?tab=teams#top')).toEqual(['departments']);
      expect(pathSegments('/')).toEqual([]);
      expect(pathSegments('')).toEqual([]);
    });

    it('les paramètres de route agissent comme jokers', () => {
      expect(matchesRoutePattern('/souls/:id', '/souls/123')).toBe(true);
      expect(matchesRoutePattern('/souls/:id', '/souls/123/notes')).toBe(false);
      expect(matchesRoutePattern('/souls', '/autre')).toBe(false);
    });

    it('reconnaît les routes de l’application', () => {
      expect(isKnownRoute('/souls')).toBe(true);
      expect(isKnownRoute('/tenant/organization')).toBe(true);
      expect(isKnownRoute('/une-route-inventée')).toBe(false);
      expect(isKnownRoute('/')).toBe(false);
    });
  });

  describe('parent déduit', () => {
    it('remonte d’un segment quand le chemin parent est une route', () => {
      expect(deriveParentPath('/tenant/organization/nodes/abc')).toBe('/tenant/organization');
      expect(deriveParentPath('/departments/12')).toBe('/departments');
    });

    it('s’arrête sur le premier ancêtre qui est une vraie route', () => {
      // /departments/12 EST une route (dossier d'un département) : on s'y
      // arrête, même en descendant plus profond.
      expect(deriveParentPath('/departments/12/members/7')).toBe('/departments/12');
      expect(deriveParentPath('/departments/12/inconnu')).toBe('/departments/12');
    });

    it('renvoie null quand aucun ancêtre n’est une route connue', () => {
      expect(deriveParentPath('/une/route/inventée')).toBeNull();
    });

    it('ne renvoie jamais la racine : un retour vers « / » n’est pas un retour', () => {
      expect(deriveParentPath('/dashboard')).toBeNull();
      expect(deriveParentPath('/')).toBeNull();
    });

    it('ne renvoie rien pour une page racine', () => {
      expect(deriveParentPath('/profile')).toBeNull();
    });
  });

  describe('résolution du retour', () => {
    it('privilégie la cible explicite fournie par l’appelant', () => {
      const back = resolveBack('/souls/123', '/souls');
      expect(back).toEqual({ target: '/souls', source: 'from' });
    });

    it('ignore une cible explicite identique à la page courante', () => {
      const back = resolveBack('/souls/123', '/souls/123');
      expect(back.source).not.toBe('from');
    });

    it('retombe sur le parent déduit quand aucune cible n’est fournie', () => {
      expect(resolveBack('/souls/123')).toEqual({ target: '/souls', source: 'parent' });
    });

    it('retombe sur l’historique quand aucun parent n’est déductible', () => {
      const back = resolveBack('/dashboard', null, true);
      expect(back.source).toBe('history');
      expect(back.target).toBe(-1);
    });

    it('ne propose RIEN quand il n’y a nulle part où revenir', () => {
      // Le bouton doit alors être masqué : un retour qui ne fait rien est pire
      // que l'absence de retour.
      expect(resolveBack('/dashboard')).toEqual({ target: null, source: 'none' });
    });
  });

  describe('mesure de l’historique réel (LOT 3 §BK)', () => {
    it('idx = 0 est un atterrissage direct : rien à popper', () => {
      expect(hasUsableHistory({ idx: 0 })).toBe(false);
    });

    it('idx > 0 offre une entrée exploitable', () => {
      expect(hasUsableHistory({ idx: 1 })).toBe(true);
      expect(hasUsableHistory({ idx: 12 })).toBe(true);
    });

    it('un état absent, vide ou non numérique ne vaut pas historique', () => {
      expect(hasUsableHistory(null)).toBe(false);
      expect(hasUsableHistory(undefined)).toBe(false);
      expect(hasUsableHistory({})).toBe(false);
      expect(hasUsableHistory({ idx: '2' })).toBe(false);
      expect(hasUsableHistory({ idx: Number.NaN })).toBe(false);
    });

    it('sans historique, resolveBack ne propose plus la cible muette -1', () => {
      // Comportement d’avant : `resolveBack('/register', null, true)` renvoyait
      // -1, un bouton qui ne fait rien sur un lien direct reçu par e-mail.
      expect(resolveBack('/register', null, hasUsableHistory({ idx: 0 })))
        .toEqual({ target: null, source: 'none' });
      expect(resolveBack('/register', null, hasUsableHistory({ idx: 3 })))
        .toEqual({ target: -1, source: 'history' });
    });
  });

  describe('fil d’Ariane', () => {
    it('énumère les ancêtres du plus général au plus précis', () => {
      expect(breadcrumbTrail('/tenant/organization/nodes/abc')).toEqual([
        '/tenant',
        '/tenant/organization',
        '/tenant/organization/nodes',
        '/tenant/organization/nodes/abc',
      ]);
    });

    it('rend un segment lisible', () => {
      expect(humanizeSegment('chef-de-famille')).toBe('chef de famille');
      expect(humanizeSegment('node_detail')).toBe('node detail');
    });
  });
});