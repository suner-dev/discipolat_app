import { describe, it, expect } from 'vitest';
import {
  buildGroupedNav,
  containsHref,
  countNodes,
  flattenNodes,
  normalizeLabel,
  pathToHref,
  slugify,
  type NavEntry,
  type NavSection,
} from '@/navigation/grouping';
import { CircleDot } from 'lucide-react';

const icon = CircleDot;

function entry(name: string, href: string, section?: string): NavEntry {
  return { name, href, icon, subtitle: '', section };
}

function sections(...defs: Array<[string, NavEntry[]]>): NavSection[] {
  return defs.map(([title, items]) => ({ title, items }));
}

describe('LOT 2 §GR — regroupement de la navigation', () => {
  describe('normalisation & slug', () => {
    it('ignore casse, accents et espaces', () => {
      expect(normalizeLabel("Vie de l'Église ")).toBe("vie de l'eglise");
      expect(normalizeLabel('PILOTAGE')).toBe('pilotage');
      expect(normalizeLabel(null)).toBe('');
      expect(normalizeLabel(undefined)).toBe('');
    });

    it('produit un slug stable et exploitable comme clé de groupe', () => {
      expect(slugify("Vie de l'église")).toBe('vie-de-l-eglise');
      expect(slugify('Engagement & outils')).toBe('engagement-outils');
      expect(slugify('   ')).toBe('');
    });
  });

  describe('mode dégradé (aucun groupe configuré)', () => {
    it('synthétise un groupe par section : la liste plate disparaît', () => {
      const result = buildGroupedNav({
        sections: sections(
          ['Pilotage', [entry('Dashboard', '/dashboard')]],
          ['Discipolat', [entry('Âmes', '/souls'), entry('Sermons', '/sermons')]],
        ),
      });

      expect(result.nodes.map((n) => n.label)).toEqual(['Pilotage', 'Discipolat']);
      expect(result.nodes[0].collapsedByDefault).toBe(true);
      expect(result.nodes[1].items).toHaveLength(2);
    });

    it('ne perd aucune entrée', () => {
      const result = buildGroupedNav({
        sections: sections(
          ['A', [entry('a1', '/a1'), entry('a2', '/a2')]],
          ['B', [entry('b1', '/b1')]],
        ),
      });
      expect(result.totalIn).toBe(3);
      expect(result.totalOut).toBe(3);
      expect(flattenNodes(result.nodes).flatMap((n) => n.items.map((i) => i.href)).sort())
        .toEqual(['/a1', '/a2', '/b1']);
    });
  });

  describe('groupes configurés', () => {
    it('rattache une entrée à un groupe par correspondance de section', () => {
      const result = buildGroupedNav({
        sections: sections(['Pilotage', [entry('Dashboard', '/dashboard')]]),
        groups: [{ id: 'g1', key: 'pilotage', label: 'Pilotage', displayOrder: 0 }],
      });

      expect(result.nodes).toHaveLength(1);
      expect(result.nodes[0].id).toBe('g1');
      expect(result.nodes[0].items[0].href).toBe('/dashboard');
    });

    it('une affectation explicite prime sur la section', () => {
      const result = buildGroupedNav({
        sections: sections(['Pilotage', [entry('Dashboard', '/dashboard')]]),
        groups: [
          { id: 'g1', key: 'pilotage', label: 'Pilotage', displayOrder: 0 },
          { id: 'g2', key: 'favoris', label: 'Favoris', displayOrder: 1 },
        ],
        assignments: { '/dashboard': ['g2'] },
      });

      expect(result.nodes.find((n) => n.id === 'g1')?.items).toHaveLength(0);
      expect(result.nodes.find((n) => n.id === 'g2')?.items[0].href).toBe('/dashboard');
      expect(result.totalOut).toBe(1);
    });

    it('imbrique les sous-groupes sous leur parent', () => {
      const result = buildGroupedNav({
        sections: [
          { title: 'Régions', items: [entry('Zone Nord', '/regions/nord')] },
          { title: 'CAMPUS', items: [entry('Campus A', '/campus/a')] },
        ],
        groups: [
          { id: 'root', key: 'vie', label: "Vie de l'église", displayOrder: 0 },
          { id: 'reg', key: 'regions', label: 'Régions', parentGroupId: 'root', displayOrder: 0 },
          { id: 'cam', key: 'campus', label: 'CAMPUS', parentGroupId: 'root', displayOrder: 1 },
        ],
      });

      const root = result.nodes[0];
      expect(root.label).toBe("Vie de l'église");
      expect(root.children.map((c) => c.label)).toEqual(['Régions', 'CAMPUS']);
      expect(root.children[0].items[0].href).toBe('/regions/nord');
      expect(root.items).toHaveLength(0);
      expect(result.totalOut).toBe(2);
      // Le groupe le plus spécifique gagne quand libellé et section coïncident.
      expect(result.nodes[0].children[0].id).toBe('reg');
    });

    it('un sous-groupe dont le parent est absent devient racine (pas d\'orphelin invisible)', () => {
      const result = buildGroupedNav({
        sections: sections(['Régions', [entry('Nord', '/nord')]]),
        groups: [{ id: 'orphan', key: 'reg', label: 'Régions', parentGroupId: 'disparu' }],
      });

      expect(result.nodes).toHaveLength(1);
      expect(result.nodes[0].id).toBe('orphan');
      expect(result.totalOut).toBe(1);
    });

    it('un cycle de parentés ne boucle pas à l\'infini', () => {
      const result = buildGroupedNav({
        sections: sections(['X', [entry('x', '/x')]]),
        groups: [
          { id: 'a', key: 'a', label: 'A', parentGroupId: 'b' },
          { id: 'b', key: 'b', label: 'B', parentGroupId: 'a' },
        ],
      });

      expect(flattenNodes(result.nodes).length).toBeLessThanOrEqual(2);
    });
  });

  describe('garantie « rien ne disparaît »', () => {
    it('une affectation vers un groupe supprimé retombe sur le groupe de sa section', () => {
      const result = buildGroupedNav({
        sections: sections(['Administration', [entry('Menus', '/admin/menus')]]),
        groups: [{ id: 'g1', key: 'admin', label: 'Administration', displayOrder: 0 }],
        // Le serveur renvoie encore l'affectation d'un groupe supprimé.
        assignments: { '/admin/menus': ['groupe-supprime'] },
      });

      // Non perdue : elle réintègre le groupe correspondant à sa section.
      expect(result.totalOut).toBe(1);
      expect(result.nodes[0].items[0].href).toBe('/admin/menus');
      expect(result.nodes.filter((n) => n.kind === 'section')).toHaveLength(0);
    });

    it('une affectation orpheline sans groupe de section devient résiduelle', () => {
      const result = buildGroupedNav({
        sections: sections(['Zone Est', [entry('Sermons Est', '/sermons/est')]]),
        groups: [{ id: 'g1', key: 'administration', label: 'Administration' }],
        assignments: { '/sermons/est': ['groupe-supprime'] },
      });

      expect(result.totalOut).toBe(1);
      expect(result.unassigned.map((e) => e.href)).toEqual(['/sermons/est']);
      const holder = flattenNodes(result.nodes).find((n) => n.items.length > 0);
      expect(holder?.items[0].href).toBe('/sermons/est');
      expect(holder?.residual).toBe(true);
    });

    it('une section dont toutes les entrées ont été reaffectées disparaît comme section', () => {
      const result = buildGroupedNav({
        sections: sections(['Pilotage', [entry('Dashboard', '/dashboard')]]),
        groups: [{ id: 'g1', key: 'favoris', label: 'Favoris' }],
        assignments: { '/dashboard': ['g1'] },
      });

      expect(result.nodes.filter((n) => n.kind === 'section')).toHaveLength(0);
      expect(result.totalOut).toBe(1);
    });

    it('une section partiellement reaffectée ne duplique pas ses entrées', () => {
      const result = buildGroupedNav({
        sections: sections(['Pilotage', [entry('Dashboard', '/dashboard'), entry('KPI', '/kpi')]]),
        groups: [
          { id: 'g1', key: 'pilotage', label: 'Pilotage' },
          { id: 'g2', key: 'favoris', label: 'Favoris' },
        ],
        assignments: { '/dashboard': ['g2'] },
      });

      expect(result.totalOut).toBe(2);
      const hrefs = flattenNodes(result.nodes).flatMap((n) => n.items.map((i) => i.href));
      expect(hrefs.sort()).toEqual(['/dashboard', '/kpi']);
    });

    it('déduplique une href présente dans deux sections', () => {
      const result = buildGroupedNav({
        sections: sections(
          ['A', [entry('Même page', '/dupe')]],
          ['B', [entry('Même page', '/dupe')]],
        ),
      });

      expect(result.totalOut).toBe(1);
      expect(result.nodes.reduce((sum, n) => sum + n.items.length, 0)).toBe(1);
    });

    it('supporte un menu vide sans planter', () => {
      const result = buildGroupedNav({ sections: [] });
      expect(result.nodes).toEqual([]);
      expect(result.totalIn).toBe(0);
      expect(result.totalOut).toBe(0);
    });
  });

  describe('aide au rendu', () => {
    it('détecte la branche active, y compris sur un sous-chemin', () => {
      const result = buildGroupedNav({
        sections: sections(['Discipolat', [entry('Âmes', '/souls')]]),
        groups: [{ id: 'g1', key: 'discipolat', label: 'Discipolat' }],
      });

      expect(containsHref(result.nodes[0], '/souls')).toBe(true);
      expect(containsHref(result.nodes[0], '/souls/123')).toBe(true);
      expect(containsHref(result.nodes[0], '/autre')).toBe(false);
    });

    it('ne confond pas un préfixe de chaîne avec un segment', () => {
      const result = buildGroupedNav({
        sections: sections(['X', [entry('X', '/soul')]]),
      });
      expect(containsHref(result.nodes[0], '/souls')).toBe(false);
    });

    it('remonte le chemin de groupes menant à une route (fil d\'Ariane)', () => {
      const result = buildGroupedNav({
        sections: sections(['Régions', [entry('Nord', '/regions/nord')]]),
        groups: [
          { id: 'root', key: 'vie', label: "Vie de l'église" },
          { id: 'reg', key: 'regions', label: 'Régions', parentGroupId: 'root' },
        ],
      });

      expect(pathToHref(result.nodes, '/regions/nord')?.map((n) => n.label))
        .toEqual(["Vie de l'église", 'Régions']);
      expect(pathToHref(result.nodes, '/inconnu')).toBeNull();
    });

    it('compte les entrées d\'un groupe, sous-groupes inclus', () => {
      const result = buildGroupedNav({
        sections: sections(
          ['A', [entry('a1', '/a1'), entry('a2', '/a2')]],
          ['B', [entry('b1', '/b1')]],
        ),
        groups: [
          { id: 'root', key: 'root', label: 'Racine' },
          { id: 'sub', key: 'sub', label: 'Sous', parentGroupId: 'root' },
        ],
        assignments: { '/a1': ['sub'], '/b1': ['root'] },
      });

      const root = result.nodes.find((n) => n.key === 'root');
      expect(root).toBeDefined();
      // /b1 sur le groupe + /a1 dans le sous-groupe. /a2 ne correspond à aucun
      // groupe : elle reste dans une section résiduelle, jamais perdue.
      if (root) expect(countNodes(root)).toBe(2);
      expect(result.totalOut).toBe(3);
      expect(result.nodes.filter((n) => n.kind === 'section')).toHaveLength(1);
      expect(result.nodes.find((n) => n.kind === 'section')?.items[0].href).toBe('/a2');
    });

    it('un groupe configuré sans affectation reste vide et sans doublon de libellé', () => {
      const result = buildGroupedNav({
        sections: sections(['Pilotage', [entry('KPI', '/kpi')]]),
        groups: [{ id: 'g1', key: 'pilotage', label: 'Pilotage' }],
      });

      const pilotage = result.nodes.filter((n) => normalizeLabel(n.label) === 'pilotage');
      expect(pilotage).toHaveLength(1);
      expect(pilotage[0].items[0].href).toBe('/kpi');
      expect(result.totalOut).toBe(1);
    });
  });
});