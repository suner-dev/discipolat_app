package com.discipolat.modules.statuses.domain;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/** G2.7 — Une transition non déclarée est refusée (contrôle côté backend). */
@ResponseStatus(HttpStatus.BAD_REQUEST)
public class StatusTransitionDeniedException extends RuntimeException {
    public StatusTransitionDeniedException(String message) {
        super(message);
    }
}
