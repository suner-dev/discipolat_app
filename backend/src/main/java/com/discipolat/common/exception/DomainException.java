package com.discipolat.common.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

import java.net.URI;
import java.util.Map;

public class DomainException extends RuntimeException {

    private final HttpStatus status;
    private final String code;
    private final Map<String, String> details;

    public DomainException(String message, HttpStatus status, String code) {
        this(message, status, code, Map.of());
    }

    public DomainException(String message, HttpStatus status, String code, Map<String, String> details) {
        super(message);
        this.status = status;
        this.code = code;
        this.details = details;
    }

    /**
     * Code d'erreur stable (ex. {@code INVITATION_EXPIRED}).
     *
     * <p>Accessor public : le code est la partie stable du contrat d'erreur, celle
     * sur laquelle le client (web, mobile) décide de son comportement. Sans
     * accessor, chaque contrôleur devait le reconstruire en fouillant dans le
     * {@link ProblemDetail}, donc à le recopier — et à le diverger.
     */
    public String getCode() {
        return this.code;
    }

    public ProblemDetail toProblemDetail() {        ProblemDetail problem = ProblemDetail.forStatusAndDetail(this.status, this.getMessage());
        problem.setTitle(this.code);
        problem.setType(URI.create("https://api.discipolat.com/errors/" + this.code));
        if (!this.details.isEmpty()) {
            problem.setProperty("details", this.details);
        }
        return problem;
    }
}
