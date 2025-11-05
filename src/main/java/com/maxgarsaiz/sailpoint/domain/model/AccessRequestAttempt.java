package com.maxgarsaiz.sailpoint.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccessRequestAttempt {
    
    private UUID id;
    private UUID accessRequestId;  // Foreign key to AccessRequest
    private String sailpointAccessRequestId;  // ID returned by Sailpoint for this attempt
    private AttemptStatus status;
    private String errorMessage;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime completedAt;
    
    /**
     * Marks this attempt as completed.
     */
    public void markAsCompleted() {
        this.status = AttemptStatus.COMPLETED;
        this.completedAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
    
    /**
     * Marks this attempt as failed with an error message.
     */
    public void markAsFailed(String errorMessage) {
        this.status = AttemptStatus.FAILED;
        this.errorMessage = errorMessage;
        this.updatedAt = LocalDateTime.now();
    }
    
    public boolean isCompleted() {
        return status == AttemptStatus.COMPLETED;
    }
    
    public boolean isFailed() {
        return status == AttemptStatus.FAILED;
    }
    
    public boolean isInProgress() {
        return status == AttemptStatus.IN_PROGRESS;
    }

    public boolean hasProviderRequestId() {
        return sailpointAccessRequestId != null && !sailpointAccessRequestId.isBlank();
    }
}
