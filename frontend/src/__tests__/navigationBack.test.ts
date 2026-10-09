import { describe, it, expect } from 'vitest';
import {
  breadcrumbTrail,
  deriveParentPath,
  derivePublicParentPath,
  hasUsableHistory,
  humanizeSegment,
  isKnownRoute,
  isPublicShowcaseRoute,
  matchesRoutePattern,
  pathSegments,
  publicBreadcrumbTrail,
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

// LOT 3 §BK (T3.2/T3.4) — helpers additifs pour la vitrine publique.
// Ces tests sont STRICTEMENT additifs : aucune assertion ci-dessus n'a été
// modifiée, aucune fonction partagée n'a été touchée. Ils verrouillent le
// contrat des trois nouvelles fonctions pures (`isPublicShowcaseRoute`,
// `derivePublicParentPath`, `publicBreadcrumbTrail`) utilisées par
// `PublicBreadcrumbs.tsx` et les chrome rows de `ChurchLandingPage` /
// `PublicChurchesPage`.
describe('LOT 3 §BK (T3.2/T3.4) — vitrine publique', () => {
  describe('isPublicShowcaseRoute', () => {
    it('reconnaît les trois formes de la vitrine', () => {
      expect(isPublicShowcaseRoute('/eglises')).toBe(true);
      expect(isPublicShowcaseRoute('/churches')).toBe(true);
      expect(isPublicShowcaseRoute('/e/bethel')).toBe(true);
    });

    it('refuse formellement les routes privées ou inconnues', () => {
      // Garde-fou : un composant vitré ne doit jamais s'incruster sur
      // une route authentifiée, ni sur une route profondément nichee
      // sous /e/ (ce qui voudrait dire qu'on a ajouté une vue non
      // prévue sans étendre le helper).
      expect(isPublicShowcaseRoute('/dashboard')).toBe(false);
      expect(isPublicShowcaseRoute('/souls/12')).toBe(false);
      expect(isPublicShowcaseRoute('/e/bethel/edit')).toBe(false);
      expect(isPublicShowcaseRoute('/')).toBe(false);
    });

    it('tolère une query string (le param ?church= du picker est conservé)', () => {
      // Le parcours « Je m'enregistre pour une église déjà listée » passe
      // par /eglises?church=bethel : le helper DOIT toujours reconnaître.
      expect(isPublicShowcaseRoute('/e/bethel?church=1')).toBe(true);
      expect(isPublicShowcaseRoute('/eglises?ville=kin#haut')).toBe(true);
    });
  });

  describe('derivePublicParentPath', () => {
    it('ramène la fiche vers /eglises (jamais la racine)', () => {
      // Contrairement à deriveParentPath côté authentifié — qui refuse
      // volontairement de renvoyer '/' — ici le parent de la fiche EST
      // l'annuaire, et le parent de l'annuaire EST la racine : ce sont
      // des routes publiques, pas des vues imbriquées.
      expect(derivePublicParentPath('/e/bethel')).toBe('/eglises');
    });

    it("remonte l'annuaire vers la racine", () => {
      expect(derivePublicParentPath('/eglises')).toBe('/');
      expect(derivePublicParentPath('/churches')).toBe('/');
    });

    it("retourne null hors vitrine (le composant n'affiche alors RIEN)", () => {
      expect(derivePublicParentPath('/dashboard')).toBeNull();
      expect(derivePublicParentPath('/profile')).toBeNull();
      expect(derivePublicParentPath('/')).toBeNull();
    });

    it('ignore la query string et le fragment', () => {
      expect(derivePublicParentPath('/e/bethel?church=1')).toBe('/eglises');
      expect(derivePublicParentPath('/eglises#top')).toBe('/');
    });
  });

  describe('publicBreadcrumbTrail', () => {
    it('construit 3 maillons sur une fiche, avec currentLabel qui écrase le libellé', () => {
      const trail = publicBreadcrumbTrail('/e/bethel', 'Église Bethel');
      expect(trail.map(c => c.href)).toEqual(['/', '/eglises', '/e/bethel']);
      expect(trail.map(c => c.labelKey)).toEqual([
        'publicNav.home',
        'publicNav.directory',
        'publicNav.church',
      ]);
      // currentLabel DOIT gagner sur le libellé par défaut — c'est ce
      // qui évite d'afficher « Église » nu à la place du nom réel.
      expect(trail[2].label).toBe('Église Bethel');
      // Les deux premiers maillons n'ont pas de label forcé : le
      // composant appellera t(labelKey) au rendu.
      expect(trail[0].label).toBeUndefined();
      expect(trail[1].label).toBeUndefined();
    });

    it("construit 2 maillons sur l'annuaire (Accueil + Annuaire)", () => {
      const trail = publicBreadcrumbTrail('/eglises');
      expect(trail).toHaveLength(2);
      expect(trail[0].href).toBe('/');
      expect(trail[1].href).toBe('/eglises');
      expect(trail[1].labelKey).toBe('publicNav.directory');
    });

    it('retourne un tableau vide hors vitrine (le composant masquera le fil)', () => {
      // PublicBreadcrumbs renvoie `null` quand trail.length <= 1 ; un
      // tableau vide garantit qu'aucune structure <nav> ne s'infiltre
      // sur une page privée.
      expect(publicBreadcrumbTrail('/dashboard')).toEqual([]);
      expect(publicBreadcrumbTrail('/')).toEqual([]);
      expect(publicBreadcrumbTrail('/souls/12')).toEqual([]);
    });

    it('conserve la query string sur le maillon courant (deep link depuis le picker)', () => {
      const trail = publicBreadcrumbTrail('/e/bethel?church=1', 'Bethel');
      expect(trail[2].href).toBe('/e/bethel?church=1');
      // Les maillons ancêtres restent des chemins canoniques, sans query.
      expect(trail[0].href).toBe('/');
      expect(trail[1].href).toBe('/eglises');
    });
  });

  describe('constante PUBLIC_DIRECTORY_PATH', () => {
    it('expose /eglises comme ancêtre unique des fiches (single source of truth)', async () => {
      // Import à la volée pour ne pas révéler la constante dans les
      // autres describes du fichier (portée locale de l'assertion).
      const mod = await import('@/navigation/back');
      expect(mod.PUBLIC_DIRECTORY_PATH).toBe('/eglises');
      // Cohérence : ce chemin est bien celui que renvoie le helper.
      expect(derivePublicParentPath('/e/quelconque')).toBe(mod.PUBLIC_DIRECTORY_PATH);
    });
  });
});