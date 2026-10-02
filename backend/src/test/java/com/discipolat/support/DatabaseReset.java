package com.discipolat.support;

import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Vidage de tables pour les tests d'intégration, indifférent au dialecte.
 *
 * <p><b>Pourquoi cette classe existe :</b> les neuf classes d'intégration
 * nettoyaient leurs tables avec {@code SET REFERENTIAL_INTEGRITY FALSE},
 * commande propre à H2. Branchées contre PostgreSQL (l'ancien job CI
 * surchargeait la datasource avec {@code SPRING_DATASOURCE_URL}), elles
 * échouaient toutes en {@code BadSqlGrammarException} — un rouge sans
 * rapport avec le code de production. Le profil de test reste H2 ; ce
 * helper rend le nettoyage portable pour qu'une datasource PostgreSQL
 * (gate dédiée, debug local) ne casse plus les tests.
 *
 * <p><b>Sémantique conservée :</b> vider intégralement les tables listées
 * en ignorant les clés étrangères — en H2 on coupe l'intégrité
 * référentielle le temps du vidage, en PostgreSQL on truncate en cascade.
 * Les identifiants étant des UUID, aucune séquence à réinitialiser.
 */
public final class DatabaseReset {

    private DatabaseReset() {
    }

    /** Vide les tables données selon le dialecte de la connexion active. */
    public static void truncate(JdbcTemplate jdbc, String... tables) {
        boolean h2 = isH2(jdbc);
        if (h2) {
            jdbc.execute("SET REFERENTIAL_INTEGRITY FALSE");
        }
        for (String table : tables) {
            jdbc.execute(h2
                    ? "TRUNCATE TABLE " + table
                    : "TRUNCATE TABLE " + table + " CASCADE");
        }
        if (h2) {
            jdbc.execute("SET REFERENTIAL_INTEGRITY TRUE");
        }
    }

    private static boolean isH2(JdbcTemplate jdbc) {
        String product = jdbc.execute((ConnectionCallback<String>)
                con -> con.getMetaData().getDatabaseProductName());
        return product != null && product.toUpperCase().startsWith("H2");
    }
}
