-- V179 : Documents légaux versionnés (RGPD / preuve de consentement)
-- Table maître des CGU, politique de confidentialité, DPA et mention art. 9.
-- Chaque acceptation de consentement référence (code, version).

CREATE TABLE IF NOT EXISTS legal_documents (
    id              uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    code            varchar(50)  NOT NULL,
    version         integer      NOT NULL,
    language        varchar(5)   NOT NULL DEFAULT 'fr',
    title           varchar(255) NOT NULL,
    content         text         NOT NULL,
    published       boolean      NOT NULL DEFAULT true,
    published_at    timestamptz  NOT NULL DEFAULT now(),
    created_at      timestamptz  NOT NULL DEFAULT now(),
    updated_at      timestamptz,
    CONSTRAINT uk_legal_documents_code_version_lang UNIQUE (code, version, language)
);

CREATE INDEX IF NOT EXISTS idx_legal_documents_code ON legal_documents (code, language, published);

-- ── Documents initiaux v1 (modèles à faire valider par un juriste) ──────────

INSERT INTO legal_documents (code, version, language, title, content)
SELECT 'CGU', 1, 'fr', 'Conditions Générales d''Utilisation',
$doc$CONDITIONS GÉNÉRALES D'UTILISATION — DISCIPOLAT (v1)

1. Objet
Discipolat est une plateforme SaaS de gestion d'églises et de discipulat fournie « en l'état ». L'abonnement est souscrit par une organisation (église, réseau, département) représentée par son administrateur.

2. Compte et responsabilités
Le souscripteur garantit l'exactitude des informations fournies et la licéité des données importées. Il s'engage à obtenir les consentements nécessaires des membres dont les données sont traitées sur la plateforme.

3. Abonnement et paiement
Les plans sont facturés par mois ou par année, par carte bancaire (Stripe) ou mobile money selon les régions disponibles. L'essai gratuit commence sans moyen de paiement ; à son terme, l'accès aux fonctions payantes est suspendu jusqu'au paiement. Toute annulation prend effet en fin de période en cours.

4. Données des utilisateurs
Le souscripteur reste responsable du traitement des données saisies. Discipolat agit en qualité de sous-traitant au sens du RGPD (cf. DPA). Les données sont hébergées dans l'Union européenne ou dans un pays assurant un niveau de protection adéquat.

5. Suspension et résiliation
Un compte peut être suspendu en cas de violation des présentes, de non-paiement, ou d'exigence légale. Le souscripteur peut exporter ses données à tout moment et obtenir leur suppression conformément au RGPD.

6. Garantie et responsabilité
La plateforme est fournie sans garantie de disponibilité continue. La responsabilité de l'éditeur est limitée au montant payé au cours des douze derniers mois.

7. Propriété intellectuelle
Les logiciels, marques et contenus de Discipolat sont protégés. Est interdite toute tentative de rétro-ingénierie, reproduction ou revente non autorisée.

8. Droit applicable
Les présentes sont régies par le droit français. À défaut d'accord amiable, compétence est attribuée aux tribunaux de Paris.$doc$
WHERE NOT EXISTS (SELECT 1 FROM legal_documents WHERE code = 'CGU' AND version = 1 AND language = 'fr');

INSERT INTO legal_documents (code, version, language, title, content)
SELECT 'PRIVACY', 1, 'fr', 'Politique de Confidentialité',
$doc$POLITIQUE DE CONFIDENTIALITÉ — DISCIPOLAT (v1)

Identité du responsable de traitement
L'éditeur Discipolat détermine les finalités des traitements de compte et de facturation. Pour les données saisies par les églises (membres, prières, visites, finances), l'église est responsable de traitement et Discipolat est son sous-traitant (cf. DPA).

Données traitées
Identité et coordonnées des administrateurs ; données d'usage et de facturation ; et, pour le compte des églises : données d'identité des membres, données de vie spirituelle (prières, discipleship, témoignages), coordonnées, présences et dons.

Base légale
Exécution du contrat (compte, facturation) ; consentement explicite pour les données révélant une conviction religieuse (RGPD art. 9.2.a) ; intérêt légitime pour la sécurité et l'amélioration du service.

Durées de conservation
Comptes actifs : durée de la relation contractuelle. Après suppression de l'organisation : 12 mois (sauvegardes), puis anonymisation. Factures : 10 ans (obligation légale).

Sécurité
Chiffrement TLS en transit, chiffrement au repos des colonnes sensibles (AES-256), cloisonnement logique multi-tenant, journalisation d'audit, contrôle d'accès par rôles.

Vos droits
Accès, rectification, effacement, limitation, opposition, portabilité, retrait du consentement à tout moment. Exercez-les depuis l'application ou par email au support. Réclamation possible auprès de votre autorité de protection (ex. CNIL).

Cookies et traceurs
Application web sans cookie publicitaire. Jetons de session et stockage local strictement nécessaires au fonctionnement.

Sous-traitants
Hébergement cloud, Stripe (paiement carte), opérateurs mobile money, service email transactionnel — chacun contractué aux termes du RGPD.$doc$
WHERE NOT EXISTS (SELECT 1 FROM legal_documents WHERE code = 'PRIVACY' AND version = 1 AND language = 'fr');

INSERT INTO legal_documents (code, version, language, title, content)
SELECT 'DPA', 1, 'fr', 'Accord de Sous-traitance (DPA)',
$doc$ACCORD DE SOUS-TRAITANCE — DISCIPOLAT (v1)
au sens de l'article 28 du RGPD

1. Objet
Le présent DPA encadre le traitement des données personnelles effectué par Discipolat (« sous-traitant ») pour le compte de l'organisation cliente (« responsable de traitement »).

2. Instructions
Le sous-traitant ne traite les données que sur les instructions documentées du responsable de traitement, sauf obligation légale. L'usage de la plateforme constitue l'instruction de traitement nécessaire aux fonctionnalités souscrites.

3. Confidentialité et habilitation
Le personnel du sous-traitant est lié par la confidentialité et n'accède aux données que sur besoin d'en connaître.

4. Mesures de sécurité (art. 32)
Chiffrement AES-256 au repos, TLS 1.2+ en transit, cloisonnement multi-tenant, MFA pour l'administration, journalisation d'audit, sauvegardes chiffrées, tests de sécurité périodiques.

5. Sous-traitants ultérieurs
Liste communiquée et opposable ; notification de tout changement ; même niveau de protection exigé contractuellement.

6. Violation de données
Notification au responsable de traitement sous 72 heures après connaissance, avec nature, données concernées et mesures prises.

7. Droits des personnes
Assistance à l'exercice des droits (accès, rectification, effacement, portabilité) via les fonctions d'export et de suppression de la plateforme.

8. Fin du traitement
Suppression ou restitution des données dans les 12 mois suivant la fin de la relation, sauvegardes comprises.

9. Preuve et audits
Documentation des traitements disponible ; audit réalisable sur préavis raisonnable, une fois par an.$doc$
WHERE NOT EXISTS (SELECT 1 FROM legal_documents WHERE code = 'DPA' AND version = 1 AND language = 'fr');

INSERT INTO legal_documents (code, version, language, title, content)
SELECT 'CONSENT_ART9', 1, 'fr', 'Consentement — données religieuses (RGPD art. 9)',
$doc$CONSENTEMENT EXPLICITE — TRAITEMENT DE DONNÉES RELIGIEUSES (v1)
article 9.2.a du RGPD

Je soussigné(e), administrateur souscrivant un compte Discipolat pour une organisation religieuse :

1. Reconnaís que la plateforme traitera des données sensibles révélant des convictions religieuses (foi, prières, discipleship, fréquentation, offrandes) au sens de l'article 9 du RGPD.

2. Consentement explicite à ce que ces données soient traitées par l'organisation que je représente et par son sous-traitant Discipolat, pour les finalités listées dans la politique de confidentialité.

3. M'engage à recueillir moi-même le consentement des membres concernés avant toute saisie les concernant, et à honorer leurs retraits de consentement.

4. Peux retirer ce consentement à tout moment ; le retrait n'affecte pas la licéité des traitements antérieurs. L'anonymat des personnes est préservé par les réglages de confidentialité de la plateforme.

5. Reconnaís avoir reçu l'information prévue aux articles 13 et 14 du RGPD et disposer des voies de recours auprès de l'autorité de protection compétente.$doc$
WHERE NOT EXISTS (SELECT 1 FROM legal_documents WHERE code = 'CONSENT_ART9' AND version = 1 AND language = 'fr');
