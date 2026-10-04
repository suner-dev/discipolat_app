package com.discipolat.modules.tenants.domain;

/**
 * Nature d'une organisation — SPEC_ORGANISATION_DENOMINATION_V2 §3 / D2.
 *
 * <p>Le modèle initial assimilait un tenant à une église. La réalité client
 * est plus large : une même structure peut être une dénomination qui
 * fédère plusieurs églises, une association, une méga-association. Rendre
 * cette nature <b>explicite dans les données</b> évite de la déduire du
 * nom, et permet à l'IHM d'adapter les libellés.
 *
 * <p><b>Ne pas confondre</b> avec {@link TenantStatus} (cycle de vie
 * opérationnel : {@code PENDING_SETUP}, {@code ACTIVE}, {@code SUSPENDED},
 * {@code CANCELLED}) : le {@code kind} décrit ce que l'organisation EST,
 * le statut ce qu'elle VIT. Les deux sont orthogonaux.
 *
 * <p>Valeurs alignées sur la contrainte SQL de {@code V222}
 * ({@code tenants_kind_check}). Ajouter une valeur exige donc une migration
 * ET une modification de cette contrainte — les deux ensemble, jamais l'une
 * sans l'autre.
 */
public enum TenantKind {

    /**
     * Église isolée — le cas par défaut, et le plus fréquent.
     *
     * <p>Une église sans rattachement : {@code parentTenantId == null} et
     * {@code rootTenantId == id}. C'est ce que produit « Créer une église »
     * en libre-service.
     */
    CHURCH,

    /**
     * Dénomination : une autorité qui fédère plusieurs églises.
     *
     * <p>Le « groupe WhatsApp » du client (§1.3). Son propriétaire est
     * <b>le roi</b> : il gouverne, délègue, produit un code ou un lien par
     * église fille — et ne voit que des <b>agrégats</b> de ses enfants
     * (D7), jamais leurs membres nominatifs.
     */
    DENOMINATION,

    /**
     * Association — regroupement d'églises ou d'organisations, sans
     * connotation d'autorité extérieure.
     */
    ASSOCIATION,

    /**
     * Organisation — dénomination générique pour une structure
     * multi-activités dont les branches ne sont pas des églises.
     */
    ORGANIZATION,

    /**
     * Méga-association — structure hiérarchique profonde : une
     * dénomination nationale au-dessus de régionales, elles-mêmes
     * regroupant des églises.
     *
     * <p>Le modèle ne diffère pas techniquement de {@link DENOMINATION} :
     * c'est le <b>niveau</b> de la hiérarchie qui distingue les deux, et
     * non la mécanique. La valeur existe pour que l'IHM puisse présenter
     * une arborescence à plusieurs étages sans deviner le nombre de
     * niveaux d'après la profondeur en base.
     */
    MEGA_ASSOCIATION;

    /** Vrai si ce type peut avoir des organisations enfants (D1). */
    public boolean canHaveChildren() {
        return this == DENOMINATION || this == MEGA_ASSOCIATION || this == ASSOCIATION
                || this == ORGANIZATION;
    }

    /** Vrai si ce type est la racine d'un réseau — donc porteuse du transfert. */
    public boolean isNetworkContainer() {
        return this == DENOMINATION || this == MEGA_ASSOCIATION;
    }

    /** Vrai si ce type représente une église concrète (et non un conteneur). */
    public boolean isChurchLike() {
        return this == CHURCH;
    }
}
