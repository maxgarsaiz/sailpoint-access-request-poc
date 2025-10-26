package com.maxgarsaiz.sailpoint.domain.exception;

import lombok.Getter;

/**
 * Generic exception thrown when attempting to acquire a lock on an entity
 * that is already being processed or locked by another process.
 */
@Getter
public class EntityAlreadyLockedException extends RuntimeException {
    
    private final String entityType;
    private final Object entityId;
    private final String lockReason;
    
    /**
     * Creates an exception with a custom message
     * 
     * @param message the detail message
     */
    public EntityAlreadyLockedException(String message) {
        super(message);
        this.entityType = null;
        this.entityId = null;
        this.lockReason = null;
    }
    
    /**
     * Creates an exception for a specific entity type and ID
     * 
     * @param entityType the type of entity (e.g., "AccessRequest", "User")
     * @param entityId the ID of the entity that is locked
     */
    public EntityAlreadyLockedException(String entityType, Object entityId) {
        super(String.format("%s with id %s is already locked or being processed", entityType, entityId));
        this.entityType = entityType;
        this.entityId = entityId;
        this.lockReason = "Already being processed by another process";
    }
    
    /**
     * Creates an exception for a specific entity with a custom reason
     * 
     * @param entityType the type of entity
     * @param entityId the ID of the entity
     * @param reason the specific reason why the lock couldn't be acquired
     */
    public EntityAlreadyLockedException(String entityType, Object entityId, String reason) {
        super(String.format("%s with id %s cannot be locked: %s", entityType, entityId, reason));
        this.entityType = entityType;
        this.entityId = entityId;
        this.lockReason = reason;
    }
}
