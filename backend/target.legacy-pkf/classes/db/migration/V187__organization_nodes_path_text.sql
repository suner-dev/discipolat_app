-- V187 : `organization_nodes.path` est un `ltree` que le code ne sait pas écrire
-- (constat H8).
--
-- PROBLEME MESURE. L'entite `OrganizationNode` declare
-- `@Column(name = "path", columnDefinition = "ltree")` sur un champ `String`,
-- et la couche applicative manipule ce chemin comme une chaine de codes separes
-- par des points. Aucune requete n'utilise d'operateur `ltree` (`<@`, `@>`, `~`) :
-- la hierarchie est parcourue par `LIKE CONCAT(parent.path, '%')`. Le type `ltree`
-- est donc un heritage d'un modele jamais realise, et il rend toute ecriture
-- impossible :
--
--     ERROR: column "path" is of type ltree but expression is of type character varying
--
-- Consequence : la creation de toute eglise racine echouait, donc le
-- provisionnement atomique d'un tenant (500) et l'etape CHURCH_IDENTITY du
-- wizard d'onboarding. Le module Organisation etait inoperable sur toute base
-- construite par les migrations.
--
-- CORRECTION COTE BASE, ET NON COTE ENTITE. L'entite continue de traiter `path` comme
-- une chaine : c'est la semantique reellement implementee partout (concatenation
-- `.`, prefix matching LIKE, unicite du code). Rendre l'entite compatible avec
-- `ltree` demanderait de reecrire les requetes de hierarchie sans aucun gain, l'index
-- GIST n'etant pas exploite par le code.
--
-- La conversion est sans perte (`path::text` conserve les libelles), preserve la
-- contrainte NOT NULL, et l'index est remplace par un equivalent compatible avec
-- la recherche par prefixe.

-- 1) L'index GIST utilise la classe d'operateurs ltree : il empeche la conversion
--    de type. On le supprime d'abord, on le recree en btree juste apres.
DROP INDEX IF EXISTS idx_org_node_path;

-- 2) Conversion du type. `USING path::text` est explicite et sans perte.
ALTER TABLE organization_nodes
    ALTER COLUMN path TYPE text
    USING path::text;

-- 3) Index de remplacement : `varchar_pattern_ops` sert les requetes
--    `path LIKE 'prefixe%'`, qui sont les seules utilisees
--    (`findDescendants`, `isDescendantOf`, `findByPathPrefix`).
CREATE INDEX IF NOT EXISTS idx_org_node_path
    ON organization_nodes (path varchar_pattern_ops);

-- 4) Le `columnDefinition` de l'entite doit refléter le type reel, sinon une
--    future generation de schema reintroduirait l'incoherence.
COMMENT ON COLUMN organization_nodes.path IS
    'Chemin hierarchique, codes separes par un point (ex. ROOT_CHURCH_AB12.IC_CD34). '
    'Type text, et non ltree : le code n''utilise aucun operateur ltree.';
