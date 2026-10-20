package com.discipolat.modules.audit.api;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

/**
 * REPousseur DE LA SURFACE {@code /api/v1/permissions} (Phase 9) — la réponse
 * explicite aux sous-chemins que cette surface ne sert pas.
 *
 * <p><strong>Ce que cette classe est devenue, et pourquoi.</strong> Elle avait
 * été écrite comme le crépuscule de {@code PermissionController} : la surface
 * héritée lisait {@code role_permissions} dans sa forme <em>pré-V135</em>
 * ({@code role}, {@code permission}, {@code enabled}, {@code can_read},
 * {@code can_write}, {@code can_delete}, {@code scope}) alors que la migration
 * {@code V135__create_multi_tenant_core_tables.sql} a supprimé et recréé cette
 * table en simple jointure {@code (role_id, permission_id)} :
 *
 * <pre>
 *   \d role_permissions          --&gt;  role_id uuid, permission_id uuid   (2 colonnes)
 *   SELECT role, permission, enabled, can_read, can_write, can_delete, scope
 *     FROM role_permissions      --&gt;  ERROR:  column "role" does not exist  (SQLSTATE 42703)
 * </pre>
 *
 * <p>Sur l'observation du retrait a été préférée la <strong>réparation</strong>
 * (consigne projet : ne rien supprimer, réparer) : {@code PermissionController},
 * {@code PermissionGuard} et {@code audit.PermissionService} sont rétablis et
 * leur SQL est désormais écrit sur le modèle canonique {@code roles} /
 * {@code permissions} / {@code role_permissions(role_id, permission_id)}. Les
 * dix URL historiques répondent donc de nouveau, en s'appuyant sur les mêmes
 * données que les gardes d'autorisation — plus aucune de ces surfaces ne ment.
 *
 * <p><strong>Rôle restant de ce contrôleur.</strong> Il répond là où la surface
 * héritée n'a jamais rien servi (sous-chemin inconnu, verbe non pris en charge),
 * avec les pointeurs vers la surface canonique {@code /api/v1/admin/roles}. Un
 * {@code 410 Gone} serait ici un mensonge — ces chemins n'ont pas été retirés,
 * ils n'ont jamais existé : le code renvoyé est donc {@code 404}, et le corps
 * ProblemDetail reste identique dans son utilité (le client apprend quoi appeler
 * à la place). La méthode {@code ""} n'est <strong>pas</strong> cartographiée
 * ici : elle appartient au contrôleur réparé, et la mapper deux fois lèverait
 * une {@code Ambiguous mapping} au démarrage — ce test-là est asserté par
 * {@code RetiredPermissionSurfaceSunsetTest}.
 *
 * <p><strong>Garde d'autorisation inchangé</strong> : la classe porte le même
 * {@code @authz.can('AUDIT_EXECUTE', …)} que le contrôleur hérité. Un appelant non
 * autorisé reçoit donc ce qu'il recevait avant (401/403), pas davantage
 * d'information — la vocation pédagogique de la réponse ne relaxe en rien la
 * sécurité.
 */
@RestController
@RequestMapping("/api/v1/permissions")
@PreAuthorize("@authz.can('AUDIT_EXECUTE', 'TENANT', null)")
public class RetiredPermissionSurfaceController {

    /** Préfixe de la surface canonique qui remplace celle-ci. */
    private static final String CANONICAL = "/api/v1/admin/roles";

    // Le chemin « "" » (GET /api/v1/permissions) est volontairement absent : il
    // est servi par PermissionController depuis sa remise en marche, et deux
    // gestionnaires pour le même verbe + chemin empêchent l'application de démarrer.
    @RequestMapping(path = "/**",
            method = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT,
                    RequestMethod.PATCH, RequestMethod.DELETE},
            produces = MediaType.APPLICATION_PROBLEM_JSON_VALUE)
    public ResponseEntity<ProblemDetail> retired(HttpServletRequest request) {
        String path = request.getRequestURI();
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND,
                "Ce chemin n'est pas servi par la surface héritée /api/v1/permissions. "
                        + "Les dix URL historiques de cette surface répondent de nouveau (elles sont "
                        + "calculées sur les tables roles / permissions / role_permissions, les mêmes "
                        + "que lisent les gardes d'autorisation). Pour la matrice par église, la "
                        + "surface de référence est /api/v1/admin/roles.");
        problem.setTitle("Chemin inconnu sous la surface de permissions");
        problem.setType(URI.create("https://discipolat.com/api/errors/retired-permission-surface"));
        problem.setProperty("legacyPath", path);
        problem.setProperty("replacements", replacementsFor(path));
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(problem);
    }

    /**
     * Le chemin de remplacement, choisi selon l'URL appelée : c'est la seule partie
     * de ce contrôleur qui puisse être fausse sans être visible, donc elle est
     * assertée telle quelle par le test (pas seulement « un 410 »).
     */
    private List<String> replacementsFor(String path) {
        String rest = path.substring("/api/v1/permissions".length());
        if (rest.startsWith("/catalog")) {
            return List.of("GET " + CANONICAL + "/permissions");
        }
        if (rest.startsWith("/roles")) {
            return List.of(
                    "GET " + CANONICAL + "/overview",
                    "POST " + CANONICAL,
                    "PUT " + CANONICAL + "/{id}",
                    "DELETE " + CANONICAL + "/{id}",
                    "POST " + CANONICAL + "/{id}/local-copy");
        }
        if (rest.contains("/rwd") || rest.split("/").length >= 3) {
            return List.of("PUT " + CANONICAL + "/{id}/permissions  {permissionKeys:[…]}");
        }
        return List.of("GET " + CANONICAL, "GET " + CANONICAL + "/{id}/permissions");
    }
}
