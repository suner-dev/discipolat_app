package com.discipolat.common.domain;

import java.util.UUID;

public class EntityNotFoundException extends RuntimeException {

    public EntityNotFoundException(String entityName, UUID id) {
        super(String.format("%s not found with id: %s", entityName, id));
    }

    public EntityNotFoundException(String entityName, String field, String value) {
        super(String.format("%s not found with %s: %s", entityName, field, value));
    }
}
