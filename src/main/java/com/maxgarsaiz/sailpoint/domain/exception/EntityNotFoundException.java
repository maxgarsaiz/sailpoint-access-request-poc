package com.maxgarsaiz.sailpoint.domain.exception;

import lombok.Getter;

/**
 * Generic exception thrown when a domain entity is not found in the repository.
 * This exception can be used for any domain entity.
 */
@Getter
public class EntityNotFoundException extends RuntimeException {
    
    private final String entityType;
    private final Object entityId;
    
    /**
     * Creates an exception with a custom message
     * 
     * @param message the detail message
     */
    public EntityNotFoundException(String message) {
        super(message);
        this.entityType = null;
        this.entityId = null;
    }
    
    /**
     * Creates an exception for a specific entity type and ID
     * 
     * @param entityType the type of entity (e.g., "AccessRequest", "User")
     * @param entityId the ID of the entity that was not found
     */
    public EntityNotFoundException(String entityType, Object entityId) {
        super(String.format("%s not found with id: %s", entityType, entityId));
        this.entityType = entityType;
        this.entityId = entityId;
    }
    
    /**
     * Creates an exception with a custom message and cause
     * 
     * @param message the detail message
     * @param cause the cause
     */
    public EntityNotFoundException(String message, Throwable cause) {
        super(message, cause);
        this.entityType = null;
        this.entityId = null;
    }
}
