package com.fluttiris.admincontrol.common.exception;

import java.util.UUID;

public class EntityNotFoundException extends RuntimeException {

    public EntityNotFoundException(String entityName, UUID id) {
        super(entityName + " introuvable (id=" + id + ")");
    }

    public EntityNotFoundException(String message) {
        super(message);
    }
}
