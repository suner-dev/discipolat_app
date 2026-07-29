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

    public ProblemDetail toProblemDetail() {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(this.status, this.getMessage());
        problem.setTitle(this.code);
        problem.setType(URI.create("https://api.discipolat.com/errors/" + this.code));
        if (!this.details.isEmpty()) {
            problem.setProperty("details", this.details);
        }
        return problem;
    }
}
