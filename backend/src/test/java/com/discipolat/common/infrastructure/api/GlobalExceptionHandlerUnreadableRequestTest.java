package com.discipolat.common.infrastructure.api;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotAcceptableException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Constats H5 et H5b — une requête client fautive doit répondre 4xx, jamais 5xx.
 *
 * <p>Ces deux exceptions n'étaient pas gérées et tombaient donc dans le
 * {@code @ExceptionHandler(Exception.class)} générique : une erreur de client
 * devenait une erreur serveur. Conséquences concrètes : des 5xx dans la
 * supervision qui déclenchaient de fausses alertes, et la cause réelle masquée
 * derrière « Internal Server Error ».
 *
 * <p>Sans ce test la régression est invisible : la suite ne fait jamais passer
 * une requête réellement malformée à travers la couche MVC.
 */
class GlobalExceptionHandlerUnreadableRequestTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    private HttpMessageNotReadableException unreadable(String rawMessage) {
        HttpMessageNotReadableException cause = mock(HttpMessageNotReadableException.class);
        when(cause.getMostSpecificCause()).thenReturn(new IllegalStateException(rawMessage));
        return cause;
    }

    @Test
    @DisplayName("H5 — un corps JSON malformé répond 400 Bad Request, pas 500")
    void malformedJsonIsAClientError() {
        ProblemDetail problem = handler.handleUnreadableBody(
                unreadable("JSON parse error: unexpected character"));

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(problem.getTitle()).isEqualTo("Bad Request");
        assertThat(problem.getType().toString()).contains("bad-request");
    }

    @Test
    @DisplayName("H5 — le message brut de Jackson (noms de classes) n'est pas divulgué")
    void doesNotLeakInternalClassNames() {
        ProblemDetail problem = handler.handleUnreadableBody(
                unreadable("Cannot deserialize value of type `com.discipolat.common.domain.UserRole`"));

        // Le détail est un texte fixe : le message d'exception, qui contient le
        // nom complet des classes Java, ne doit jamais atteindre le client.
        assertThat(problem.getDetail()).isEqualTo("Malformed or missing request body");
    }

    @Test
    @DisplayName("H5b — un Accept non négociable répond 406, pas 500")
    void unacceptableMediaTypeIsAClientError() {
        // Le message que le serveur produit reellement quand l'Accept ne
        // correspond a aucune representation, et la liste des representations
        // effectivement disponibles pour la ressource.
        HttpMediaTypeNotAcceptableException cause = new HttpMediaTypeNotAcceptableException(
                List.of(MediaType.parseMediaType("application/vnd.oai.openapi;version=3.0")));

        ProblemDetail problem = handler.handleNotAcceptable(cause);

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.NOT_ACCEPTABLE.value());
        assertThat(problem.getTitle()).isEqualTo("Not Acceptable");
    }
}
